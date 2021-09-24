package org.clas.test;

import java.util.HashMap;
import java.util.Map;

import org.jlab.groot.base.GStyle;

import org.clas.analysis.TrkSwim;
import org.clas.analysis.FiducialCuts;
import org.clas.analysis.ResolutionAnalysis;

public class Main {
    public static boolean setupGroot() {
        GStyle.getAxisAttributesX().setTitleFontSize(24);
        GStyle.getAxisAttributesX().setLabelFontSize(18);
        GStyle.getAxisAttributesY().setTitleFontSize(24);
        GStyle.getAxisAttributesY().setLabelFontSize(18);
        GStyle.getAxisAttributesZ().setLabelFontSize(14);
        GStyle.getAxisAttributesX().setLabelFontName("Arial");
        GStyle.getAxisAttributesY().setLabelFontName("Arial");
        GStyle.getAxisAttributesZ().setLabelFontName("Arial");
        GStyle.getAxisAttributesX().setTitleFontName("Arial");
        GStyle.getAxisAttributesY().setTitleFontName("Arial");
        GStyle.getAxisAttributesZ().setTitleFontName("Arial");
        GStyle.setGraphicsFrameLineWidth(1);
        GStyle.getH1FAttributes().setLineWidth(2);
        GStyle.getH1FAttributes().setOptStat("1111");

        return false;
    }

    /** Main. */
    public static void main(String[] args) {
        // === Process input =======================================================================
        Map<Character, List<String>> params = new HashMap<>();
        if (IOHandler.parseArgs(args, params)) System.exit(1);

        // String file = params.get('f');
        // int nEvents = params.get('n') == null ? 0 : Integer.parseInt(params.get('n'));
        // boolean drawPlots = params.get('v') == null
        //         || (params.get('v').equals("dZ") && params.get('v').equals("rZ")) ? true : false;
        //
        // // === Setup ===============================================================================
        // setupGroot();
        // double[] swmSetup = new double[]{-0.75, -1.0, -3.0};
        // int plotRange = 5; // Plotting range.
        // int fitRange  = 4; // Fitting range.
        //
        // // NOTE. Dunno if we'll keep this tbh.
        // boolean analysisType = false; // false for alignment, true for plotting.
        //
        // // Shifts to be applied.
        // // NOTE. Since the y axis is pointing up, what we call yaw is actually inverted from what
        // //       you would expect in aviation standards.
        // double[][] shArr = new double[][]{ // Alignment to be tested.
        //         // z     x     y    phi   yaw  pitch
        //         { 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}, // Global shifts
        //         { 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}, // Layer 1 shifts
        //         { 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}, // Layer 2 shifts
        //         { 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}  // Layer 3 shifts
        // };
        // FiducialCuts fCuts = new FiducialCuts();
        // TrkSwim swim = new TrkSwim(swmSetup, shArr[0][4], shArr[0][5]);
        //
        // // === Alignment ===========================================================================
        // if (!analysisType) {
        //     int varAlign = 0;
        //
        //     if (params.get('v') == null) { // Check current best results.
        //         double[] zShArr = new double[]{0.00};
        //         ResolutionAnalysis resAnls = new ResolutionAnalysis(file, nEvents, shArr,
        //                 drawPlots, false);
        //         resAnls.shiftAnalysis(0, 0, zShArr, plotRange, fitRange, swmSetup, fCuts);
        //     }
        //     else if (params.get('v').equals("dZ")) {
        //         double[] zShArr = new double[]
        //                 {-0.50, -0.45, -0.40, -0.35, -0.30, -0.25, -0.20, -0.15, -0.10, -0.05, 0.00,
        //                   0.05,  0.10,  0.15,  0.20,  0.25,  0.30,  0.35,  0.40,  0.45,  0.50};
        //         ResolutionAnalysis resAnls = new ResolutionAnalysis(file, nEvents, shArr,
        //                 drawPlots, false);
        //         resAnls.shiftAnalysis(0, 0, zShArr, plotRange, fitRange, swmSetup, fCuts);
        //     }
        //     else if (params.get('v').equals("dXY")) {
        //         double[] xShArr = new double[]
        //                 {0.05, 0.06, 0.07, 0.08, 0.09, 0.10, 0.11, 0.12, 0.13, 0.14, 0.15};
        //         double[] yShArr = new double[]
        //                 {0.05, 0.06, 0.07, 0.08, 0.09, 0.10, 0.11, 0.12, 0.13, 0.14, 0.15};
        //         double orig_x = shArr[0][1];
        //         for (int xi = 0; xi < xShArr.length; ++xi) {
        //             shArr[0][1] = orig_x + xShArr[xi];
        //             ResolutionAnalysis resAnls = new ResolutionAnalysis(file, nEvents,
        //                     shArr, false, false);
        //             resAnls.shiftAnalysis(0, 2, yShArr, plotRange, fitRange, swmSetup, fCuts);
        //         }
        //     }
        //     else if (params.get('v').equals("rZ")) {
        //         double[] phiShArr = new double[]
        //                 {-2.0, -1.9, -1.8, -1.7, -1.6, -1.5, -1.4, -1.3, -1.2, -1.1,
        //                  -1.0, -0.9, -0.8, -0.7, -0.6, -0.5, -0.4, -0.3, -0.2, -0.1,  0.0};
        //         ResolutionAnalysis resAnls = new ResolutionAnalysis(file, nEvents, shArr,
        //                 drawPlots, false);
        //         resAnls.shiftAnalysis(0, 3, phiShArr, plotRange, fitRange, swmSetup, fCuts);
        //     }
        //     else if (params.get('v').equals("rXY")) {
        //         double[] yawShArr = new double[]
        //                 {-1.0, -0.5, 0.00, 0.05, 0.5, 1.0};
        //         double[] pitchShArr = new double[]
        //                 {-1.0, -0.5, 0.00, 0.05, 0.5, 1.0};
        //         fCuts.setYPAlign(true);
        //         double orig_yaw = shArr[0][4];
        //         for (int yi = 0; yi<yawShArr.length; ++yi) {
        //             shArr[0][4] = orig_yaw + yawShArr[yi];
        //             ResolutionAnalysis resAnls = new ResolutionAnalysis(file, nEvents,
        //                     shArr, false, true);
        //             resAnls.shiftAnalysis(0, 5, pitchShArr, plotRange, fitRange, swmSetup, fCuts);
        //         }
        //     }
        //     else {
        //         System.err.printf("<var> argument is invalid. Exiting...\n");
        //         System.exit(1);
        //     }
        // }
        // // === Variable plots ======================================================================
        // // TODO: Clean this boi up, most of these aren't needed for alignment!
        // else {
        //     // Config: plotType defines the type of alignment that's to be done.
        //     // * plotType ==  0 : Plot data in each FMT strip separating data by DC's sectors.
        //     // * plotType ==  1 : Plot the Tracks' theta separating data by DC's sectors.
        //     // * plotType ==  2 : Plot data in each FMT strip separating by FMT's "regions".
        //     // * plotType ==  3 : Plot the delta Tmin between clusters inside crosses.
        //     // * plotType ==  4 : Plot the clusters' Tmin.
        //     // * plotType ==  5 : Plot the clusters' energy.
        //     // * plotType ==  6 : Plot the tracks' z vertex coordinate.
        //     // * plotType ==  7 : Plot the delta Tmin between clusters inside crosses.
        //     // * plotType ==  8 : Plot the tracks' theta coordinate.
        //     // * plotType ==  9 : 2D plot the clusters' energy divided by their number of strips.
        //     // * plotType == 10 : Plot the DCTB vs FMT tracks' vertex z.
        //     // * plotType == 11 : 2D plot the tracks' vz vs their polar angle (theta).
        //     // * plotType == 12 : Plot FMT tracks' vertex z.
        //     // * plotType == 13 : 2D plot the tracks' vz vs their azimuthal (phi) angle.
        //     // * plotType == 14 : Plot the status of each FMT track.
        //     // * plotType == 15 : Plot the chi^2 of the FMT tracks.
        //     // * plotType == 16 : Plot DC tracks' vertex z.
        //     int plotType = 16;
        //
        //     ResolutionAnalysis resAnls = new ResolutionAnalysis(file, nEvents,
        //             shArr, drawPlots, false);
        //
        //     if      (plotType ==  0) resAnls.dcSectorStripAnalysis(plotRange, fitRange, swim, fCuts);
        //     else if (plotType ==  1) resAnls.dcSectorThetaAnalysis(plotRange, fitRange, swim, fCuts);
        //     else if (plotType ==  2) resAnls.fmtRegionAnalysis(plotRange, swim, fCuts);
        //     else if (plotType ==  3) resAnls.deltaTminAnalysis(plotRange, swim, fCuts);
        //     else if (plotType ==  4) resAnls.plot1DCount(0, swim, fCuts, 1000);
        //     else if (plotType ==  5) resAnls.plot1DCount(1, swim, fCuts, 2000);
        //     else if (plotType ==  6) resAnls.plot1DCount(2, swim, fCuts, 50);
        //     else if (plotType ==  7) resAnls.plot1DCount(3, swim, fCuts, 150);
        //     else if (plotType ==  8) resAnls.plot1DCount(4, swim, fCuts, 90);
        //     else if (plotType ==  9) resAnls.plot2DCount(0, -1);
        //     else if (plotType == 10) resAnls.plot1DCount(5, swim, fCuts, 0);
        //     else if (plotType == 11) resAnls.plot2DCount(2, 50);
        //     else if (plotType == 12) resAnls.plot1DCount(6, swim, fCuts, 0);
        //     else if (plotType == 13) resAnls.plot2DCount(3, 50);
        //     else if (plotType == 14) resAnls.plot1DCount(7, swim, fCuts, 0);
        //     else if (plotType == 15) resAnls.plot1DCount(8, swim, fCuts, 0);
        //     else if (plotType == 16) resAnls.plot1DCount(9, swim, fCuts, 0);
        //     else {
        //         System.err.printf("plotType should be between 0 and 16!\n");
        //         System.exit(1);
        //     }
        // }
    }
}
