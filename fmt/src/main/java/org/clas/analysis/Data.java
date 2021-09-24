package org.clas.analysis;

import javax.swing.*;
import org.jlab.groot.data.DataLine;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.graphics.EmbeddedCanvasTabbed;
import org.jlab.groot.group.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;

public class Data {

    /**
     * Get data bank.
     * @param event hipo event from which the bank is to be taken.
     * @param name  data bank name.
     * @return reference to the data bank.
     */
    public static DataBank getBank(DataEvent event, String name) {
        return (event.hasBank(name)) ? event.getBank(name) : null;
    }

    /**
     * Fit residuals plot using a gaussian.
     * @param hires 1-dimensional residuals distribution.
     * @param f1res functions to use for fitting.
     * @param r     radius for the gaussian fit (-r,r).
     * @return status int.
     */
    public static int fitRes(H1F hires, F1D f1res, double r) {
        // Fit the data.
        double mean = hires.getDataX(hires.getMaximumBin());
        double amp = hires.getBinContent(hires.getMaximumBin());
        f1res.setParameter(0, amp);
        f1res.setParameter(1, mean);
        f1res.setParameter(2, 0.5);
        f1res.setRange(-r, r);
        DataFitter.fit(f1res, hires, "Q"); // No options use error for sigma.
        hires.setFunction(null);

        return 0;
    }

    /**
     * Create a set of data groups for residuals analysis.
     *
     * @param type Type of plot, explained in ResolutionAnalysis.runAnalysis.
     * @param ln   Number of FMT layers.
     * @param zn   Number of z shifts to try.
     * @param r    Plot range.
     * @return array of data groups.
     */
    public static DataGroup[] createResDataGroups(int type, int ln, int zn, int r) {
        DataGroup[] dgFMT = new DataGroup[zn];

        for (int zi = 0; zi < zn; ++zi) {
            dgFMT[zi] = new DataGroup(3, 2);
            for (int li = 1; li <= ln; ++li) {
                H1F hi_cluster_res = new H1F("hi_cluster_res_l" + li, "", 200, -r, r);
                hi_cluster_res.setTitleX("Residual (cm) - Layer " + li);
                hi_cluster_res.setFillColor(4);
                dgFMT[zi].addDataSet(hi_cluster_res, li - 1);

                F1D f1_res = new F1D("f1_res_l" + li,
                        "[amp]*gaus(x,[mean],[sigma])+[p0]+[p1]*x+[p2]*x*x", -r, r);
                f1_res.setParameter(0, 0);
                f1_res.setParameter(1, 0);
                f1_res.setParameter(2, 1.0);
                f1_res.setParameter(3, 0);
                f1_res.setParameter(4, 0);
                f1_res.setParameter(5, 0);
                f1_res.setLineWidth(2);
                f1_res.setLineColor(2);
                f1_res.setOptStat("1111");
                dgFMT[zi].addDataSet(f1_res, li - 1);

                if (type == 0 || type == 1) {
                    H2F hi_cluster_res_strip = new H2F("hi_cluster_res_strip_l" + li,
                            200, -r, r, 200, 0, 1024);
                    hi_cluster_res_strip.setTitleX("Residual (cm) - Layer " + li);
                    hi_cluster_res_strip.setTitleY("Strip - Layer " + li);
                    dgFMT[zi].addDataSet(hi_cluster_res_strip, li - 1 + ln);
                }
                else if (type == 2) {
                    H2F hi_cluster_res_theta = new H2F("hi_cluster_res_theta_l" + li,
                            200, -r, r, 200, 0, 1);
                    hi_cluster_res_theta.setTitleX("Residual - Layer " + li);
                    hi_cluster_res_theta.setTitleY("Theta - Layer " + li);
                    dgFMT[zi].addDataSet(hi_cluster_res_theta, li - 1 + ln);
                }
                else if (type == 4) {
                    H2F hi_cluster_res_dtmin = new H2F("hi_cluster_res_dtmin_l" + li,
                            200, -r, r, 200, 0, 200);
                    hi_cluster_res_dtmin.setTitleX("Residual - Layer " + li);
                    hi_cluster_res_dtmin.setTitleY("dTmin - Layer " + li);
                    dgFMT[zi].addDataSet(hi_cluster_res_dtmin, li - 1 + ln);
                }
                else if (type != 3)
                    System.out.printf("[Data] type variable should be between 0 and 4!\n");
            }
        }

        return dgFMT;
    }

    /**
     * Render plots for a set of data groups, related to residuals analysis.
     *
     * @param dgFMT    Array containing the data groups.
     * @param cn       Number of canvases to be drawn.
     * @param titleArr Array of titles for the plots.
     * @return status int.
     */
    public static int drawResPlots(DataGroup[] dgFMT, int cn, String[] titleArr) {
        EmbeddedCanvasTabbed fmtCanvas = new EmbeddedCanvasTabbed(titleArr);
        for (int ci = 0; ci < cn; ++ci) {
            fmtCanvas.getCanvas(titleArr[ci]).draw(dgFMT[ci]);
            fmtCanvas.getCanvas(titleArr[ci]).setGridX(false);
            fmtCanvas.getCanvas(titleArr[ci]).setGridY(false);
            fmtCanvas.getCanvas(titleArr[ci]).setAxisFontSize(18);
            fmtCanvas.getCanvas(titleArr[ci]).setAxisTitleSize(24);

            // Top plots
            for (int pi = 0; pi < 3; ++pi) {
                DataLine vline = new DataLine(0, 0, 0, Double.POSITIVE_INFINITY);
                vline.setLineColor(2);
                vline.setLineWidth(2);
                fmtCanvas.getCanvas(titleArr[ci]).cd(pi).draw(vline);
            }

            // Bottom plots
            for (int pi = 3; pi < 6; ++pi) {
                DataLine vline = new DataLine(0, 0, 0, Double.POSITIVE_INFINITY);
                vline.setLineColor(0);
                vline.setLineWidth(2);
                fmtCanvas.getCanvas(titleArr[ci]).cd(pi).draw(vline);
            }
        }

        JFrame frame = new JFrame("FMT");
        frame.setSize(1600, 1000);
        frame.add(fmtCanvas);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        return 0;
    }
}
