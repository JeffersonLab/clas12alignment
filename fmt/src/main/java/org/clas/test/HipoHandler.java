package org.clas.test;

import java.util.Map;
import java.util.List;
import javax.swing.*;
import org.jlab.groot.data.DataLine;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.data.IDataSet;
import org.jlab.groot.data.TDirectory;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.graphics.EmbeddedCanvas;
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
     * @return status boolean.
     */
    public static boolean fitRes(H1F hires, F1D f1res) {
        // Fit the data.
        double mean = hires.getDataX(hires.getMaximumBin());
        double amp = hires.getBinContent(hires.getMaximumBin());
        f1res.setParameter(0, amp);
        f1res.setParameter(1, mean);
        f1res.setParameter(2, 0.5);
        f1res.setRange(-Constants.FITRNG, Constants.FITRNG);
        DataFitter.fit(f1res, hires, "Q");
        hires.setFunction(null);

        return false;
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
     * @param dgFMT     Data group.
     * @param showPlots boolean describing if plots are to be shown or saved.
     * @return Status boolean.
     */
    public static boolean drawResPlot(DataGroup dgFMT, boolean showPlots) {
        EmbeddedCanvas canvas = new EmbeddedCanvas();
        canvas.draw(dgFMT);

        // Top plots.
        for (int pi = 0; pi < Constants.FMTLAYERS; ++pi) {
            DataLine vline = new DataLine(0, 0, 0, Double.POSITIVE_INFINITY);
            vline.setLineColor(2);
            vline.setLineWidth(2);
            canvas.cd(pi).draw(vline);
        }

        // Bottom plots.
        for (int pi = Constants.FMTLAYERS; pi < 2*Constants.FMTLAYERS; ++pi) {
            DataLine vline = new DataLine(0, 0, 0, Double.POSITIVE_INFINITY);
            vline.setLineColor(0);
            vline.setLineWidth(2);
            canvas.cd(pi).draw(vline);
        }

        return drawFrame("FMT", canvas, showPlots);
    }

    /**
     * Master method for drawing alignment plots.
     * @param v  String containing variable tested.
     * @param f  4D array containing 4 fit parameters for each FMT layer, for each shift tested.
     * @param os Original shifts.
     * @param ts List of shifts tested.
     * @param p  Boolean describing if plots are to be shown or saved.
     * @return Status int.
     */
    public static boolean drawAlignPlot(String v, double[][][][] f, double[][] os, List<Double> ts,
                                        boolean p) {
        if      (v.equals("dZ")  || v.equals("rZ"))  return draw1DAlignPlot(v, f, os, ts, p);
        else if (v.equals("dXY") || v.equals("rXY")) return draw2DAlignPlot(v, f, ts, p);
        else return true;
    }

    /** Draw a 1D alignment plot. */
    private static boolean draw1DAlignPlot(String var, double[][][][] parArr, double[][] oShArr,
                                           List<Double> tShArr, boolean showPlots) {
        // Setup.
        EmbeddedCanvas canvas = new EmbeddedCanvas();
        DataGroup dg = new DataGroup(3, 1);
        GraphErrors[] graphs = new GraphErrors[Constants.FMTLAYERS];
        int pos = var.equals("dZ") ? 2 : 5;

        for (int li = 0; li < Constants.FMTLAYERS; ++li) {
            // Create graphs.
            graphs[li] = new GraphErrors();
            if (var.equals("dZ")) graphs[li].setTitleX("shift [cm] - layer "  + (li+1));
            else                  graphs[li].setTitleX("angle [deg] - layer " + (li+1));
            graphs[li].setTitleY("#sigma [cm]");

            // Fill.
            for (int i = 0; i < tShArr.size(); ++i)
                graphs[li].addPoint(oShArr[li][pos] + tShArr.get(i), parArr[1][li][i][0], 0,
                                    parArr[2][li][i][0]);

            // Add to dataset.
            dg.addDataSet(graphs[li], li);
        }

        // Show plots.
        canvas.draw(dg);
        return drawFrame("" + var + " Alignment", canvas, showPlots);
    }

    /** Draw a 2D alignment plot. */
    private static boolean draw2DAlignPlot(String var, double[][][][] parArr, List<Double> tShArr,
                                           boolean showPlots) {
        // Setup.
        EmbeddedCanvas canvas = new EmbeddedCanvas();
        int size = tShArr.size();
        int pos;
        H2F hi = new H2F("hi", size, tShArr.get(0), tShArr.get(size-1),
                               size, tShArr.get(0), tShArr.get(size-1));
        if (var.equals("dXY")) {
            pos = 0;
            hi.setTitleX("x shift [cm]");
            hi.setTitleY("y shift [cm]");
        }
        else {
            pos = 3;
            hi.setTitleX("x angle [deg]");
            hi.setTitleY("y angle [deg]");
        }

        // Fill.
        double[] min = new double[Constants.FMTLAYERS];
        double[] max = new double[Constants.FMTLAYERS];
        for (int li = 0; li < Constants.FMTLAYERS; ++li) {
            min[li] = Double.POSITIVE_INFINITY;
            max[li] = Double.NEGATIVE_INFINITY;

            for (int i = 0; i < parArr[0][li].length; ++i) {
                for (int j = 0; j < parArr[0][li][i].length; ++j) {
                    double absMean = Math.abs(parArr[0][li][i][j]);
                    if (absMean < min[li]) min[li] = absMean;
                    if (absMean > max[li]) max[li] = absMean;
                }
            }
        }

        for (int i = 0; i < parArr[0][0].length; ++i) {
            for (int j = 0; j < parArr[0][0][i].length; ++j) {
                double normalizedMean = 0.0;
                for (int li = 0; li < Constants.FMTLAYERS; ++li) {
                    double absMean = Math.abs(parArr[0][li][i][j]);
                    normalizedMean += (absMean - min[li])/(max[li] - min[li]);
                }
                // NOTE. Weirdness here is just to solve a minor issue with 2D plots.
                double d = (tShArr.get(1) - tShArr.get(0))/2;
                double xPos = i == parArr[0][0].length-1    ? tShArr.get(i)-d : tShArr.get(i);
                double yPos = j == parArr[0][0][i].length-1 ? tShArr.get(j)-d : tShArr.get(j);
                hi.fill(xPos, yPos, normalizedMean);
            }
        }

        // Show plots.
        canvas.draw(hi);
        return drawFrame("" + var + " Alignment", canvas, showPlots);
    }

    /** Draw or save plots via a JFrame. */
    private static boolean drawFrame(String title, EmbeddedCanvas canvas, boolean showPlots) {
        if (showPlots) { // Show plots.
            JFrame frame = new JFrame(title);
            frame.setSize(1600, 1000);
            frame.add(canvas);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }

        // Save plots.
        TDirectory dir = new TDirectory();
        dir.mkdir("/histos");
        dir.cd("/histos");
        Map<String, IDataSet> objMap = canvas.getObjectMap();
        for (IDataSet val : objMap.values()) dir.addDataSet(val);
        dir.writeFile("histograms.hipo");

        return false;
    }
}
