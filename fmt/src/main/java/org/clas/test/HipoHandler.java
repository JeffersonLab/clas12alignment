package org.clas.test;

import java.util.List;
import javax.swing.*;
import org.jlab.groot.data.DataLine;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.graphics.EmbeddedCanvasTabbed;
import org.jlab.groot.group.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;

/** Class in charge of handling hipo banks and groot. */
public class HipoHandler {
    /**
     * Get data bank.
     * @param event hipo event from which the bank is to be taken.
     * @param name  data bank name.
     * @return Reference to the data bank.
     */
    public static DataBank getBank(DataEvent event, String name) {
        return (event.hasBank(name)) ? event.getBank(name) : null;
    }

    /**
     * Fit residuals plot using a function given by f1res. Assumed to be a gaussian.
     * @param hires 1-dimensional residuals distribution.
     * @param f1res functions to use for fitting.
     * @return status int.
     */
    public static int fitRes(H1F hires, F1D f1res) {
        // Fit the data.
        double mean = hires.getDataX(hires.getMaximumBin());
        double amp = hires.getBinContent(hires.getMaximumBin());
        f1res.setParameter(0, amp);
        f1res.setParameter(1, mean);
        f1res.setParameter(2, 0.5);
        f1res.setRange(-Constants.FITRNG, Constants.FITRNG);
        DataFitter.fit(f1res, hires, "Q");
        hires.setFunction(null);

        return 0;
    }

    /**
     * Create a set of data groups for residuals analysis.
     * @param tn1 Number of shifts to be attempted.
     * @param tn2 Second number of shifts to be attempted for dXY and rXY alignment.
     * @return Array of data groups.
     */
    public static DataGroup[][] createResDataGroups(int tn1, int tn2) {
        DataGroup[][] dgFMT = new DataGroup[tn1][tn2];

        for (int ti1 = 0; ti1 < tn1; ++ti1) {
            for (int ti2 = 0; ti2 < tn2; ++ti2) {
                dgFMT[ti1][ti2] = new DataGroup(Constants.FMTLAYERS, 3);
                for (int li = 1; li <= Constants.FMTLAYERS; ++li) {
                    H1F hi1D = new H1F("hi_l" + li, "", Constants.PLOTRES,
                            -Constants.PLOTRNG, Constants.PLOTRNG);
                    hi1D.setTitleX("Residual (cm) - Layer " + li);
                    hi1D.setFillColor(4);
                    dgFMT[ti1][ti2].addDataSet(hi1D, li - 1);

                    String resfit = "[amp]*gaus(x,[mean],[sigma])+[p0]+[p1]*x+[p2]*x*x";
                    F1D fit = new F1D("fit_l" + li, resfit, -Constants.PLOTRNG, Constants.PLOTRNG);
                    fit.setParameter(0, 0);
                    fit.setParameter(1, 0);
                    fit.setParameter(2, 1.0);
                    fit.setParameter(3, 0);
                    fit.setParameter(4, 0);
                    fit.setParameter(5, 0);
                    fit.setLineWidth(2);
                    fit.setLineColor(2);
                    fit.setOptStat("1111");
                    dgFMT[ti1][ti2].addDataSet(fit, li - 1);

                    H2F hi2D = new H2F("hi_strip2D_l" + li,
                            Constants.PLOTRES, -Constants.PLOTRNG, Constants.PLOTRNG,
                            Constants.FMTNSTRIPS, 0, Constants.FMTNSTRIPS);
                    hi2D.setTitleX("Residual (cm) - Layer " + li);
                    hi2D.setTitleY("Strip - Layer " + li);
                    dgFMT[ti1][ti2].addDataSet(hi2D, Constants.FMTLAYERS + li - 1);

                    H1F hi1Dstrip = new H1F("hi_strip1D_l" + li, Constants.FMTNSTRIPS, 0,
                            Constants.FMTNSTRIPS);
                    hi1Dstrip.setTitleX("Strip - Layer" + li);
                    hi1Dstrip.setFillColor(43);
                    hi1Dstrip.setLineWidth(1);
                    dgFMT[ti1][ti2].addDataSet(hi1Dstrip, 2*Constants.FMTLAYERS + li - 1);
                }
            }
        }

        return dgFMT;
    }

    /**
     * Render plots for a data group related to residuals analysis.
     * @param dgFMT Data group.
     * @return Status int.
     */
    public static int drawResPlot(DataGroup dgFMT) {
        String title = "Residuals";
        EmbeddedCanvasTabbed fmtCanvas = new EmbeddedCanvasTabbed(title);
        fmtCanvas.getCanvas(title).draw(dgFMT);

        // Top plots.
        for (int pi = 0; pi < Constants.FMTLAYERS; ++pi) {
            DataLine vline = new DataLine(0, 0, 0, Double.POSITIVE_INFINITY);
            vline.setLineColor(2);
            vline.setLineWidth(2);
            fmtCanvas.getCanvas(title).cd(pi).draw(vline);
        }

        // Bottom plots.
        for (int pi = Constants.FMTLAYERS; pi < 2*Constants.FMTLAYERS; ++pi) {
            DataLine vline = new DataLine(0, 0, 0, Double.POSITIVE_INFINITY);
            vline.setLineColor(0);
            vline.setLineWidth(2);
            fmtCanvas.getCanvas(title).cd(pi).draw(vline);
        }

        JFrame frame = new JFrame("FMT");
        frame.setSize(1600, 1000);
        frame.add(fmtCanvas);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        return 0;
    }

    /**
     * Master method for drawing alignment plots.
     * @param var          String containing variable tested.
     * @param fitParamsArr 4D array containing 4 fit parameters for each FMT layer, for each shift
     *                     tested.
     * @param shArr        List of shifts tested.
     * @return Status int.
     */
    public static int drawAlignPlot(String var, double[][][][] parArr, List<Double> shArr) {
        if      (var.equals("dZ")  || var.equals("rZ"))  return draw1DAlignPlot(var, parArr, shArr);
        else if (var.equals("dXY") || var.equals("rXY")) return draw2DAlignPlot(var, parArr, shArr);
        else return 1;
    }

    /*
    fitParamsArr:
    [0]          : variable - 0 mean, 1 sigma, 2 sigmaerr, 3 chi2.
    [0][0]       : FMT layer - 0, 1 or 2.
    [0][0][0]    : z alignment tested.
    [0][0][0][0] : nothing (for 1D alignment).
    */
    private static int draw1DAlignPlot(String var, double[][][][] parArr, List<Double> shArr) {
        // Setup.
        EmbeddedCanvas canvas = new EmbeddedCanvas();
        DataGroup dg = new DataGroup(3, 1);
        GraphErrors[] graphs = new GraphErrors[Constants.FMTLAYERS];

        for (int li = 0; li < Constants.FMTLAYERS; ++li) {
            // Create graphs.
            graphs[li] = new GraphErrors();
            graphs[li].setTitleX("shift [cm] - layer " + (li+1));
            graphs[li].setTitleY("#sigma [cm]");

            // Fill.
            for (int i = 0; i < shArr.size(); ++i)
                graphs[li].addPoint(shArr.get(i), parArr[1][li][i][0], 0, parArr[2][li][i][0]);

            // Add to dataset.
            dg.addDataSet(graphs[li], li);
        }
        canvas.draw(dg);

        // Show plots.
        String title = "" + var + " Alignment";
        JFrame frame = new JFrame(title);
        frame.setSize(1600, 1000);
        frame.add(canvas);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        return 0;
    }

    private static int draw2DAlignPlot(String var, double[][][][] parArr, List<Double> shArr) {
        return 1;
    }
}
