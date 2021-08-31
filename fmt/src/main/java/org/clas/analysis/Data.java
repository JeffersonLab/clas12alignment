package org.clas.analysis;

import javax.swing.*;
import org.clas.cross.Constants;
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
     *
     * @param event hipo event from which the bank is to be taken.
     * @param name  data bank name.
     * @return reference to the data bank.
     */
    public static DataBank getBank(DataEvent event, String name) {
        return (event.hasBank(name)) ? event.getBank(name) : null;
    }

    /**
     * Fit residuals plot using two gaussians.
     *
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
     * Create a 1D data group of a specified variable.
     *
     * @param var Index defining which variable is to be plotted.
     *              * 0 : cluster's Tmin.
     *              * 1 : cluster's total energy.
     *              * 2 : track's z position.
     *              * 3 : delta tmin between clusters in a cross.
     *              * 4 : track's theta angle.
     * @param ln  Number of FMT layers.
     * @param r   Range of the variable (0,r).
     * @return the created data group.
     */
    public static DataGroup[] create1DDataGroup(int var, int ln, int r) {
        DataGroup[] dgFMT = new DataGroup[1];

        if (var == 0 || var == 1) {
            dgFMT[0] = new DataGroup(3, 1);
            for (int li = 0; li < ln; ++li) {
                H1F h1f = new H1F("clusters" + li, 200, 0, r);
                if (var == 0) h1f.setTitleX("Tmin (ns)");
                if (var == 1) h1f.setTitleX("Energy (MeV)");
                h1f.setTitleY("Cluster count");
                h1f.setFillColor(4);

                dgFMT[0].addDataSet(h1f, li);
            }
        }

        else if (var == 2 || var == 4) {
            dgFMT[0] = new DataGroup(1, 1);
            H1F hi_track_var = new H1F("tracks", 800, -r, r);
            if (var == 2) hi_track_var.setTitleX("vertex z (cm)");
            if (var == 4) hi_track_var.setTitleX("track theta (deg)");
            hi_track_var.setTitleY("track count");
            hi_track_var.setFillColor(4);

            dgFMT[0].addDataSet(hi_track_var, 0);
        }

        else if (var == 3) {
            dgFMT[0] = new DataGroup(2, 1);
            for (int i = 0; i < 2; ++i) {
                H1F hi_dtmin_var = new H1F("delta_tmin" + i, 2 * r, -r, r);
                hi_dtmin_var.setTitleX("dTmin (ns)");
                hi_dtmin_var.setTitleY("Counts");
                hi_dtmin_var.setFillColor(4);
                dgFMT[0].addDataSet(hi_dtmin_var, i);

                F1D f1_res = new F1D("f" + i, "[amp]*gaus(x,[mean],[sigma])+[p0]+[p1]*x+[p2]*x*x",
                        -r, r);
                f1_res.setParameter(0, 0);
                f1_res.setParameter(1, 0);
                f1_res.setParameter(2, 1.0);
                f1_res.setParameter(3, 0);
                f1_res.setParameter(4, 0);
                f1_res.setParameter(5, 0);
                f1_res.setLineWidth(2);
                f1_res.setLineColor(2);
                f1_res.setOptStat("1111");
                dgFMT[0].addDataSet(f1_res, i);
            }
        }

        else if (var == 5) {
            dgFMT[0] = new DataGroup(2, 1);
            // We're only interested in the target's first window.
            for (int i = 0; i < 2; ++i) {
                H1F hi_track_var = null;

                if (i == 0) hi_track_var = new H1F("DCTB tracks", 801, -50, 50);
                if (i == 1) hi_track_var = new H1F("FMT tracks",  801, -50, 50);

                hi_track_var.setTitleX("vertex z (cm)");
                hi_track_var.setTitleY("track count");
                hi_track_var.setLineWidth(1);
                hi_track_var.setFillColor(4);

                dgFMT[0].addDataSet(hi_track_var, i);

                // Prepare fit.
                F1D f1_2peaks = new F1D("twinpeaks"+i, "[amp1]*gaus(x,[mean],[sigma])+[amp2]*gaus(x,[mean]-2.4,[sigma])+[p0]+[p1]*x+[p2]*x*x",
                        -r, r);

                f1_2peaks.setLineWidth(2);
                f1_2peaks.setLineColor(2);
                f1_2peaks.setOptStat("1111");
                dgFMT[0].addDataSet(f1_2peaks, i);
            }
        }

        else if (var == 6) {
            // Setup plot.
            dgFMT[0] = new DataGroup(1, 1);
            H1F hi_track_var = null;
            hi_track_var = new H1F("FMT tracks", 801, -50, 50);

            hi_track_var.setTitleX("vertex z (cm)");
            hi_track_var.setTitleY("track count");
            hi_track_var.setFillColor(4);

            dgFMT[0].addDataSet(hi_track_var, 0);

            // Prepare fit.
            F1D f1 = new F1D("upstream fit",   "[amp1]*gaus(x,[mean],[sigma])+[amp2]*gaus(x,[mean]-2.4,[sigma])+[p0]+[p1]*x+[p2]*x*x", -r, r); // found peak distance
            F1D f2 = new F1D("downstream fit", "[amp]*gaus(x,[mean],[sigma])+[p0]+[p1]*x+[p2]*x*x", -r, r);

            f1.setLineWidth(2);         f2.setLineWidth(2);
            f1.setLineColor(2);         f2.setLineColor(2);
            f1.setOptStat("1111");      f2.setOptStat("1111");
            dgFMT[0].addDataSet(f1, 0); dgFMT[0].addDataSet(f2, 0);
        }

        else if (var == 7) {
            dgFMT[0] = new DataGroup(1, 1);
            H1F plt = new H1F("track_status", 110, 0, 110);
            plt.setTitleX("Track Status");
            plt.setTitleY("Counts");
            plt.setFillColor(4);
            dgFMT[0].addDataSet(plt, 0);
        }

        else if (var == 8) {
            dgFMT[0] = new DataGroup(1, 1);
            H1F plt = new H1F("chi2", 200, 0, 0.2);
            plt.setTitleX("Chi2");
            plt.setTitleY("Counts");
            plt.setFillColor(4);
            dgFMT[0].addDataSet(plt, 0);
        }

        else if (var == 9) {
            // Setup plot.
            dgFMT[0] = new DataGroup(1, 1);
            H1F hi_track_var = null;
            hi_track_var = new H1F("DC tracks", 801, -50, 50);

            hi_track_var.setTitleX("vertex z (cm)");
            hi_track_var.setTitleY("track count");
            hi_track_var.setFillColor(4);

            dgFMT[0].addDataSet(hi_track_var, 0);

            // Prepare fits.
            F1D f1 = new F1D("upstream fit",   "[amp1]*gaus(x,[mean],[sigma])+[amp2]*gaus(x,[mean]-2.4,[sigma])+[p0]+[p1]*x+[p2]*x*x", -r, r); // found peak distance.
            F1D f2 = new F1D("downstream fit", "[amp]*gaus(x,[mean],[sigma])+[p0]+[p1]*x+[p2]*x*x", -r, r);

            f1.setLineWidth(2);         f2.setLineWidth(2);
            f1.setLineColor(2);         f2.setLineColor(2);
            f1.setOptStat("1111");      f2.setOptStat("1111");
            dgFMT[0].addDataSet(f1, 0); dgFMT[0].addDataSet(f2,0);
        }


        else System.out.printf("[Data] var should be between 0 and 9! Something went wrong...\n");

        return dgFMT;
    }

    /**
     * Create a 2D data group of a specified variable combination.
     *
     * @param var Index defining which pair of variables is to be plotted:
     *            * 0 : energy vs cluster size.
     * @param ln  Number of FMT layers.
     * @param r   Unused, but kept for consistency.
     * @return the created data group.
     */
    public static DataGroup[] create2DDataGroup(int var, int ln, int r) {
        DataGroup[] dgFMT = new DataGroup[1];

        dgFMT[0] = new DataGroup(ln, 1);
        if (var == 0) {
            for (int li = 1; li <= ln; ++li) {
                H2F hi_cluster_var = new H2F("hi_cluster_var" + li, "", 40, 0, 40, 100, 0, 5000);
                hi_cluster_var.setTitleX("Cluster size");
                hi_cluster_var.setTitleY("Energy / Cluster size (MeV)");
                dgFMT[0].addDataSet(hi_cluster_var, li - 1);
            }
        }
        else if (var == 2) {
            H2F plt = new H2F("theta_vs_vz", "", 201, -50, 50, 201, 0, 60); // Full target
            plt.setTitleX("vz (cm)");
            plt.setTitleY("#theta (deg)");
            dgFMT[0].addDataSet(plt, 0);
        }
        else if (var == 3) {
            for (int i = 0; i < ln; ++i) {
                String name = "";
                if (i == 0) name = "positive";
                if (i == 1) name = "negative";
                H2F plt = new H2F("phi_vs_vz_" + name, "", 201, -50, 50, 360, -180, 180); // Full target
                // H2F plt = new H2F("phi_vs_vz_" + name, "", 100, -37, -27, 360, -180, 180); // Upstream window
                plt.setTitleX("vz " + name);
                plt.setTitleY("phi");
                dgFMT[0].addDataSet(plt, i);
            }
        }
        else System.out.printf("[Data] var should be 0 or 3! Something went wrong...\n");

        return dgFMT;
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
     * Create 2D data groups separated by the FMT regions.
     *
     * @param r Residuals range.
     * @return the created data group arrays.
     */
    public static DataGroup[] createFMTRegionsDataGroup(int r) {
        DataGroup[] dgFMT = new DataGroup[1];

        dgFMT[0] = new DataGroup(4, 3);
        for (int li = 0; li < Constants.getNumberOfFMTLayers(); ++li) {
            for (int ri = 0; ri < Constants.getNumberOfFMTRegions(); ++ri) {
                H2F hi_cluster_res_strip = new H2F(
                        "hi_cluster_res_strip_l" + (Constants.getNumberOfFMTRegions() * li + ri),
                        200, -r, r,
                        Constants.getFMTRegionSeparators(ri+1)-Constants.getFMTRegionSeparators(ri),
                        Constants.getFMTRegionSeparators(ri)+1,Constants.getFMTRegionSeparators(ri+1)
                );
                hi_cluster_res_strip.setTitleX("Residual (cm) - L " + (li + 1) + ", R " + (ri + 1));
                hi_cluster_res_strip.setTitleY("Strips");

                dgFMT[0].addDataSet(hi_cluster_res_strip, Constants.getNumberOfFMTRegions()*li+ri);
            }
        }

        return dgFMT;
    }

    /**
     * Render plots for a set of data groups.
     *
     * @param dgFMT Array of data groups.
     * @param title Title to be given to the canvas.
     * @return status int.
     */
    public static int drawPlots(DataGroup[] dgFMT, String title) {
        drawZPlots(dgFMT, title, null);
        return 0;
    }

    /**
     * Fit highest peak in vertex plot.
     *
     * @param hires 1-dimensional residuals distribution.
     * @param f1res function to use for fitting.
     * @param min   minimum for the gaussian fit.
     * @param max   maximum for the gaussian fit.
     * @return status int.
     */
    public static int fitZ(H1F hires, F1D f1res, double min, double max) {
        double mean = hires.getDataX(hires.getMaximumBin());
        double amp = hires.getBinContent(hires.getMaximumBin());

        f1res.setParameter(0, 2*amp);  // amp1
        f1res.setParameter(1, mean); // mean
        f1res.setParameter(2, 0.2);  // sigma

        f1res.setRange(min, max);
        DataFitter.fit(f1res, hires, "Q"); // No options use error for sigma.
        hires.setFunction(null);

        return 0;
    }

    /**
     * Fit upstream window with two gaussians.
     *
     * @param hires 1-dimensional residuals distribution.
     * @param f1res function to use for fitting.
     * @param min   minimum for the fit.
     * @param max   maximum for the fit.
     * @return status int.
     */
    public static int fitUpstream(H1F hires, F1D f1res, double min, double max) {
        double amp = hires.getBinContent(hires.getMaximumBin());

        f1res.setParameter(0, 2*amp);  // amp1
        f1res.setParameter(1, -32.085); // mean
       f1res.setParameter(2, 1);  // sigma

        f1res.setRange(min, max);
        DataFitter.fit(f1res, hires, "Q"); // No options use error for sigma.
        hires.setFunction(null);

        return 0;
    }

    /**
     * Fit downstream window with a gaussian.
     *
     * @param hires 1-dimensional residuals distribution.
     * @param f1res function to use for fitting.
     * @param min   minimum for the fit.
     * @param max   maximum for the fit.
     * @return status int.
     */
    public static int fitDownstream(H1F hires, F1D f1res, double min, double max) {
        double amp = hires.getBinContent(hires.getMaximumBin());

        f1res.setParameter(0, 2*amp);  // amp1
        f1res.setParameter(1, 23.221); // mean
        f1res.setParameter(2, 0.2);  // sigma

        f1res.setRange(min, max);
        DataFitter.fit(f1res, hires, "Q"); // No options use error for sigma.
        hires.setFunction(null);

        return 0;
    }

    /**
     * Render plots for tracks' z position.
     *
     * @param dgFMT Array of data groups.
     * @param title Title to be given to the canvas.
     * @param fmtZ  FMT layers' z position.
     * @return status int.
     */
    public static int drawZPlots(DataGroup[] dgFMT, String title, double[] fmtZ) {
        EmbeddedCanvasTabbed fmtCanvas = new EmbeddedCanvasTabbed(title);

        fmtCanvas.getCanvas(title).draw(dgFMT[0]);
        fmtCanvas.getCanvas(title).setGridX(false);
        fmtCanvas.getCanvas(title).setGridY(false);
        fmtCanvas.getCanvas(title).setAxisFontSize(18);
        fmtCanvas.getCanvas(title).setAxisTitleSize(24);

        JFrame frame = new JFrame("FMT");
        frame.setSize(1600, 1000);
        frame.add(fmtCanvas);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        return 0;
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
