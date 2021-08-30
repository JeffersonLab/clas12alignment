package org.clas.test;

import org.jlab.groot.base.GStyle;

import org.clas.analysis.TrkSwim;
import org.clas.analysis.FiducialCuts;
import org.clas.analysis.ResolutionAnalysis;

public class Main {
    /** Print usage. */
    private static void usage() {
        System.out.println("Usage: program infile");
        System.out.println("  * infile: String with the hipo input file.");
        System.exit(1);
    }

    /** Main. */
    public static void main(String[] args) {
        // === Process input ===================================================
        String infile = args[0];
        if (args.length != 1 || !infile.endsWith(".hipo")) {
            usage();
        }

        GStyle.getH1FAttributes().setOptStat("1111111");
        GStyle.getH2FAttributes().setOptStat("1111111");

        // === Setup ===========================================================
        boolean[] pltLArr = new boolean[]{true, true, false, false};
        double[] swmSetup = new double[]{-0.75, -1.0, -3.0};

        // TODO: These should come from the program args!!
        int pltRan           = 5;     // Plotting range.
        int gssRan           = 4;     // Fitting range.
        boolean analysisType = false; // false for alignment, true for plotting.
        boolean drawPlots    = true;  // set to true to get plots.
        int nEvents          = 100000; // Number of events to run.

        // Shifts to be applied (best guess so far).
        // NOTE: Since the y axis is pointing up, what we call yaw is actually
        // inverted from what you would expect in aviation standards.
        double[][] shArr = new double[][]{ // Alignment to be tested.
                // z     x     y    phi   yaw  pitch
                { 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}, // Global shifts
                { 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}, // Layer 1 shifts
                { 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}, // Layer 2 shifts
                { 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}  // Layer 3 shifts
        };
        FiducialCuts fCuts = new FiducialCuts();
        TrkSwim swim = new TrkSwim(swmSetup, shArr[0][4], shArr[0][5]);

        // === Alignment =======================================================
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
                ResolutionAnalysis resAnls = new ResolutionAnalysis(infile,
                        pltLArr, nEvents, shArr, drawPlots, false);
                resAnls.shiftAnalysis(0, 0, zShArr, pltRan, gssRan, swmSetup,
                        fCuts);
            }
            else if (varAlign == 1) { // z alignment.
                double[] zShArr = new double[]
                        {-0.05, -0.04, -0.03, -0.02, -0.01, 0.00, 0.01, 0.02, 0.03, 0.04, 0.05};
                ResolutionAnalysis resAnls = new ResolutionAnalysis(infile, pltLArr,
                        nEvents, shArr, drawPlots, false);
                resAnls.shiftAnalysis(0, 0, zShArr, pltRan, gssRan, swmSetup, fCuts);
            }
            else if (varAlign == 2) { // x & y alignment.
                double[] xShArr = new double[]
                        {-0.05, -0.04, -0.03, -0.02, -0.01, 0.00, 0.01, 0.02, 0.03, 0.04, 0.05};
                double[] yShArr = new double[]
                        {-0.05, -0.04, -0.03, -0.02, -0.01, 0.00, 0.01, 0.02, 0.03, 0.04, 0.05};
                double orig_x = shArr[0][1];
                for (int xi = 0; xi < xShArr.length; ++xi) {
                    shArr[0][1] = orig_x + xShArr[xi];
                    ResolutionAnalysis resAnls = new ResolutionAnalysis(infile, pltLArr,
                        nEvents, shArr, false, false);
                    resAnls.shiftAnalysis(0, 2, yShArr, pltRan, gssRan, swmSetup, fCuts);
                }
            }
            else if (varAlign == 3) { // phi (roll) alignment.
                double[] phiShArr = new double[]
                        {-0.20, -0.15, -0.10, -0.05, 0.00, 0.05, 0.10, 0.15, 0.20};
                ResolutionAnalysis resAnls = new ResolutionAnalysis(infile, pltLArr,
                        nEvents, shArr, drawPlots, false);
                resAnls.shiftAnalysis(0, 3, phiShArr, pltRan, gssRan, swmSetup, fCuts);
            }
            else if (varAlign == 4) { // pitch & yaw alignment.
                double[] yawShArr = new double[]
                        {-0.25, -0.20, -0.15, -0.10, -0.05, 0.00, 0.05, 0.10, 0.15, 0.20, 0.25};
                double[] pitchShArr = new double[]
                        {-0.25, -0.20, -0.15, -0.10, -0.05, 0.00, 0.05, 0.10, 0.15, 0.20, 0.25};
                fCuts.setYPAlign(true);
                double orig_yaw = shArr[0][4];
                for (int yi = 0; yi<yawShArr.length; ++yi) {
                    shArr[0][4] = orig_yaw + yawShArr[yi];
                    ResolutionAnalysis resAnls = new ResolutionAnalysis(infile, pltLArr,
                        nEvents, shArr, false, true);
                    resAnls.shiftAnalysis(0, 5, pitchShArr, pltRan, gssRan, swmSetup, fCuts);
                }
            }
            else {
                System.out.printf("varAlign should be between 0 and 4!\n");
                System.exit(1);
            }
        }
        // === Variable plots ==================================================
        // TODO: Clean this boi up, most of these aren't needed for alignment!
        else {
            // Config: plotType defines the type of alignment that's to be done.
            // * plotType ==  0 : Plot data in each FMT strip separating data by DC's sectors.
            // * plotType ==  1 : Plot the Tracks' theta separating data by DC's sectors.
            // * plotType ==  2 : Plot data in each FMT strip separating by FMT's "regions".
            // * plotType ==  3 : Plot the delta Tmin between clusters inside crosses.
            // * plotType ==  4 : Plot the clusters' Tmin.
            // * plotType ==  5 : Plot the clusters' energy.
            // * plotType ==  6 : Plot the tracks' z vertex coordinate.
            // * plotType ==  7 : Plot the delta Tmin between clusters inside crosses.
            // * plotType ==  8 : Plot the tracks' theta coordinate.
            // * plotType ==  9 : 2D plot the clusters' energy divided by their number of strips.
            // * plotType == 10 : Plot the DCTB vs FMT tracks' vertex z.
            // * plotType == 11 : 2D plot the tracks' vz vs their polar angle (theta).
            // * plotType == 12 : Plot FMT tracks' vertex z.
            // * plotType == 13 : 2D plot the tracks' vz vs their azimuthal (phi) angle.
            // * plotType == 14 : Plot the status of each FMT track.
            // * plotType == 15 : Plot the chi^2 of the FMT tracks.
            // * plotType == 16 : Plot DC tracks' vertex z.
            int plotType = 16;

            ResolutionAnalysis resAnls = new ResolutionAnalysis(infile, pltLArr, nEvents,
                    shArr, drawPlots, false);

            if      (plotType ==  0) resAnls.dcSectorStripAnalysis(pltRan, gssRan, swim, fCuts);
            else if (plotType ==  1) resAnls.dcSectorThetaAnalysis(pltRan, gssRan, swim, fCuts);
            else if (plotType ==  2) resAnls.fmtRegionAnalysis(pltRan, swim, fCuts);
            else if (plotType ==  3) resAnls.deltaTminAnalysis(pltRan, swim, fCuts);
            else if (plotType ==  4) resAnls.plot1DCount(0, swim, fCuts, 1000);
            else if (plotType ==  5) resAnls.plot1DCount(1, swim, fCuts, 2000);
            else if (plotType ==  6) resAnls.plot1DCount(2, swim, fCuts, 50);
            else if (plotType ==  7) resAnls.plot1DCount(3, swim, fCuts, 150);
            else if (plotType ==  8) resAnls.plot1DCount(4, swim, fCuts, 90);
            else if (plotType ==  9) resAnls.plot2DCount(0, -1);
            else if (plotType == 10) resAnls.plot1DCount(5, swim, fCuts, 0);
            else if (plotType == 11) resAnls.plot2DCount(2, 50);
            else if (plotType == 12) resAnls.plot1DCount(6, swim, fCuts, 0);
            else if (plotType == 13) resAnls.plot2DCount(3, 50);
            else if (plotType == 14) resAnls.plot1DCount(7, swim, fCuts, 0);
            else if (plotType == 15) resAnls.plot1DCount(8, swim, fCuts, 0);
            else if (plotType == 16) resAnls.plot1DCount(9, swim, fCuts, 0);
            else System.out.printf("plotType should be between 0 and 16!\n");
        }
    }
}
