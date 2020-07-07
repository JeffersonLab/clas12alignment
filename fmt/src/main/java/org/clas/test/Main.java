package org.clas.test;

import org.clas.analysis.TrkSwim;
import org.clas.analysis.FiducialCuts;
import org.clas.analysis.ResolutionAnalysis;

public class Main {
    /** Print usage. */
    private static void printUsageAndExit() {
        System.out.println("Usage: program infile");
        System.out.println("  * infile: String with the hipo input file.");
        System.exit(1);
    }

    /** Main. */
    public static void main(String[] args) {
        // === Process input =======================================================================
        String infile = args[0];
        if (args.length != 1 || !infile.endsWith(".hipo")) {
            printUsageAndExit();
        }

        // === Setup ===============================================================================
        boolean[] pltLArr = new boolean[]{true, true, false, false};
        double[] swmSetup = new double[]{-0.75, -1.0, -3.0};

        int pltRan           = 10;    // Plotting range.
        int gssRan           = 8;     // Fitting range.
        boolean dbgInfo      = true;  // Show debugging info.
        int nEvents          = 0;     // Number of events. Set to 0 to run all events in input file.
        boolean dataType     = false; // false for detector data, true for gemc simulation data.
        boolean analysisType = false; // false for alignment, true for plotting variables.
        boolean makeCrosses  = false; // Boolean describing if we should do crossmaking.
        boolean drawPlots    = true;  // Boolean describing if plots are to be drawn. Due to hasty
                                      // implementation, can't be set to true for xy and pitch & yaw
                                      // alignment.

        // Shifts to be applied (best guess so far). Should be set to current best guess.
        // NOTE: Since the y axis is pointing up, the yaw direction is inverted from common aviation
        // standards.
        double[][] shArr;
        if (!dataType) { // detector data.
            shArr = new double[][]{
                    // z     x     y    phi   yaw  pitch
                    {-3.65,-0.02, 0.15,-0.35,-0.10, 0.15}, // Global shifts
                    { 0.20, 0.00, 0.00, 0.10, 0.00, 0.00}, // Layer 1 shifts
                    {-0.05, 0.00, 0.00,-0.20, 0.00, 0.00}, // Layer 2 shifts
                    { 0.05, 0.00, 0.00, 0.00, 0.00, 0.00}  // Layer 3 shifts
            };
        }
        else { // gemc data.
            shArr = new double[][]{
                    // z     x     y    phi   yaw  pitch
                    { 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}, // Global shifts
                    { 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}, // Layer 1 shifts
                    { 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}, // Layer 2 shifts
                    { 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}  // Layer 3 shifts
            };
        }
        FiducialCuts fCuts = new FiducialCuts(makeCrosses);
        TrkSwim swim = new TrkSwim(swmSetup, shArr[0][4], shArr[0][5]);

        // === Alignment ===========================================================================
        if (!analysisType) {
            // Config: varAlign defines the type of alignment that's to be done.
            // * varAlign = 0 : no alignment, only check current best results.
            // * varAlign = 1 : z alignment.
            // * varAlign = 2 : x & y alignment.
            // * varAlign = 3 : phi (roll) alignment.
            // * varALign = 4 : yaw & pitch alignment.
            int varAlign = 0;

            if (varAlign == 0) { // Check current best results.
                double[] zShArr = new double[]{0.00};
                ResolutionAnalysis resAnls = new ResolutionAnalysis(infile, pltLArr, dbgInfo,
                        nEvents, shArr, makeCrosses, drawPlots, false);
                resAnls.shiftAnalysis(0, 0, zShArr, pltRan, gssRan, swmSetup, fCuts);
            }

            else if (varAlign == 1) { // z alignment.
                // Setup:
                double[] zShArr = new double[]
                        {-0.05, -0.04, -0.03, -0.02, -0.01, 0.00, 0.01, 0.02, 0.03, 0.04, 0.05};
                // Run:
                ResolutionAnalysis resAnls = new ResolutionAnalysis(infile, pltLArr, dbgInfo,
                        nEvents, shArr, makeCrosses, drawPlots, false);
                resAnls.shiftAnalysis(0, 0, zShArr, pltRan, gssRan, swmSetup, fCuts);
            }

            else if (varAlign == 2) { // x & y alignment.
                // Setup:
                double[] xShArr = new double[]
                        {-0.05, -0.04, -0.03, -0.02, -0.01, 0.00, 0.01, 0.02, 0.03, 0.04, 0.05};
                double[] yShArr = new double[]
                        {-0.05, -0.04, -0.03, -0.02, -0.01, 0.00, 0.01, 0.02, 0.03, 0.04, 0.05};
                // Run:
                double orig_x = shArr[0][1];
                for (int xi = 0; xi < xShArr.length; ++xi) {
                    shArr[0][1] = orig_x + xShArr[xi];
                    ResolutionAnalysis resAnls = new ResolutionAnalysis(infile, pltLArr, dbgInfo,
                        nEvents, shArr, makeCrosses, false, false);
                    resAnls.shiftAnalysis(0, 2, yShArr, pltRan, gssRan, swmSetup, fCuts);
                }
            }

            else if (varAlign == 3) { // phi (roll) alignment.
                // Setup:
                double[] phiShArr = new double[]
                        {-0.20, -0.15, -0.10, -0.05, 0.00, 0.05, 0.10, 0.15, 0.20};
                // Run:
                ResolutionAnalysis resAnls = new ResolutionAnalysis(infile, pltLArr, dbgInfo,
                        nEvents, shArr, makeCrosses, drawPlots, false);
                resAnls.shiftAnalysis(0, 3, phiShArr, pltRan, gssRan, swmSetup, fCuts);
            }

            else if (varAlign == 4) { // pitch & yaw alignment.
                // Setup:
                double[] yawShArr = new double[]
                        {-0.25, -0.20, -0.15, -0.10, -0.05, 0.00, 0.05, 0.10, 0.15, 0.20, 0.25};
                double[] pitchShArr = new double[]
                        {-0.25, -0.20, -0.15, -0.10, -0.05, 0.00, 0.05, 0.10, 0.15, 0.20, 0.25};

                // Run:
                fCuts.setYPAlign(true);
                double orig_yaw = shArr[0][4];
                for (int yi = 0; yi<yawShArr.length; ++yi) {
                    shArr[0][4] = orig_yaw + yawShArr[yi];
                    ResolutionAnalysis resAnls = new ResolutionAnalysis(infile, pltLArr, dbgInfo,
                        nEvents, shArr, makeCrosses, false, true);
                    resAnls.shiftAnalysis(0, 5, pitchShArr, pltRan, gssRan, swmSetup, fCuts);
                }
            }

            else System.out.printf("varAlign should be between 0 and 4!\n");
        }

        // === Variable plots ======================================================================
        else {
            // Config: plotType defines the type of alignment that's to be done.
            // * plotType == 0 : Plot data in each FMT strip separating data by DC's sectors.
            // * plotType == 1 : Plot the Tracks' theta separating data by DC's sectors.
            // * plotType == 2 : Plot data in each FMT strip separating by FMT's "regions".
            // * plotType == 3 : Plot the delta Tmin between clusters inside crosses.
            // * plotType == 4 : Plot the clusters' Tmin.
            // * plotType == 5 : Plot the clusters' energy.
            // * plotType == 6 : Plot the tracks' z vertex coordinate.
            // * plotType == 7 : Plot the delta Tmin between clusters inside crosses.
            // * plotType == 8 : Plot the tracks' theta coordinate.
            // * plotType == 9 : 2D plot the clusters' energy divided by their number of strips.
            int plotType = 0;

            ResolutionAnalysis resAnls = new ResolutionAnalysis(infile, pltLArr, dbgInfo, nEvents,
                    shArr, makeCrosses, drawPlots, false);

            if      (plotType == 0) resAnls.dcSectorStripAnalysis(pltRan, gssRan, swim, fCuts);
            else if (plotType == 1) resAnls.dcSectorThetaAnalysis(pltRan, gssRan, swim, fCuts);
            else if (plotType == 2) resAnls.fmtRegionAnalysis(pltRan, swim, fCuts);
            else if (plotType == 3) resAnls.deltaTminAnalysis(pltRan, swim, fCuts);
            else if (plotType == 4) resAnls.plot1DCount(0, swim, fCuts, 1000);
            else if (plotType == 5) resAnls.plot1DCount(1, swim, fCuts, 2000);
            else if (plotType == 6) resAnls.plot1DCount(2, swim, fCuts, 100);
            else if (plotType == 7) resAnls.plot1DCount(3, swim, fCuts, 150);
            else if (plotType == 8) resAnls.plot1DCount(4, swim, fCuts, 90);
            else if (plotType == 9) resAnls.plot2DCount(0, -1);
            else System.out.printf("plotType should be between 0 and 9!\n");
        }
    }
}
