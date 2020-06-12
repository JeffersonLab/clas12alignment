package org.clas.test;

import org.clas.analysis.TrkSwim;
import org.clas.analysis.FiducialCuts;
import org.clas.analysis.ResolutionAnalysis;

public class Main {
    /** Print usage. */
    private static int usage() {
        System.out.printf("Usage: program infile\n");
        System.out.printf("  * infile: String with the hipo input file.\n");

        return 0;
    }

    /** Main. */
    public static void main(String[] args) {
        // Process input
        if (args.length != 1) {
            usage();
            System.exit(1);
        }

        String infile = args[0];

        // Setup
        boolean[] pltLArr = new boolean[]{true, true, false, false};
        double[] swmSetup = new double[]{-0.75, -1.0, -3.0};

        int pltRan        = 10;    // Plotting range.
        int gssRan        = 8;     // Fitting range.
        boolean dbgInfo   = true;  // Debugging info.
        boolean testRun   = false; // Shorten run for testing.

        // Shifts to be applied (best guess so far).
        double[][] shArr = new double[][]{
                // z     x     y    phi   yaw  pitch
                {-3.65,-0.02, 0.15,-0.35, 0.00, 0.00},
                { 0.20, 0.00, 0.00, 0.10, 0.00, 0.00},
                {-0.05, 0.00, 0.00,-0.20, 0.00, 0.00},
                { 0.05, 0.00, 0.00, 0.00, 0.00, 0.00}
        };
        // Shifts used for GEMC data
//        double[][] shArr = new double[][]{
//                // z     x     y    phi   yaw  pitch
//                { 0.00, 0.00, 0.00, 0.00, 0.00, 0.00},
//                { 0.00, 0.00, 0.00, 0.00, 0.00, 0.00},
//                { 0.00, 0.00, 0.00, 0.00, 0.00, 0.00},
//                { 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}
//        };

        // NOTE: Due to the fact that the y axis is pointing up, the yaw is inverted from common
        //       aviation standards!

        // Apply pitch and yaw shifts.
        FiducialCuts fCuts = new FiducialCuts();
        TrkSwim swim = new TrkSwim(swmSetup, shArr[0][4], shArr[0][5]);

        // Run
        double[] zShArr     = new double[]{0.00};
        // double[] zShArr     = new double[]{-0.05, -0.04, -0.03, -0.02, -0.01, 0.00, 0.01, 0.02, 0.03, 0.04, 0.05};
        double[] xShArr     = new double[]{-0.05, -0.04, -0.03, -0.02, -0.01, 0.00, 0.01, 0.02, 0.03, 0.04, 0.05};
        double[] yShArr     = new double[]{-0.05, -0.04, -0.03, -0.02, -0.01, 0.00, 0.01, 0.02, 0.03, 0.04, 0.05};
        double[] phiShArr   = new double[]{-0.20, -0.15, -0.10, -0.05, 0.00, 0.05, 0.10, 0.15, 0.20};
        double[] yawShArr   = new double[]{-2.0, -1.0, 0.0, 1.0, 2.0};
        double[] pitchShArr = new double[]{-2.0, -1.0, 0.0, 1.0, 2.0};

        // Z ALIGNMENT:
        ResolutionAnalysis resAnls =
                new ResolutionAnalysis(infile, pltLArr, dbgInfo, testRun, shArr);
        resAnls.shiftAnalysis(0, 0, zShArr, pltRan, gssRan, swmSetup, fCuts);

        // XY ALIGNMENT:
        // for (int xi = 0; xi<xShArr.length; ++xi) {
        //     shArr[0][1] = xShArr[xi];
        //     ResolutionAnalysis resAnls =
        //             new ResolutionAnalysis(infile, pltLArr, dbgInfo, testRun, shArr);
        //     resAnls.shiftAnalysis(0, 2, yShArr, pltRan, gssRan, swmSetup, fCuts);
        // }

        // PHI ALIGNMENT:
        // ResolutionAnalysis resAnls =
        //         new ResolutionAnalysis(infile, pltLArr, dbgInfo, testRun, shArr);
        // resAnls.shiftAnalysis(0, 3, phiShArr, pltRan, gssRan, swmSetup, fCuts);

        // YAW - PITCH ALIGNMENT:
        // TODO: WE NEED TO FIGURE OUT WHAT TO DO HERE
        // for (int yi = 0; yi<yawShArr.length; ++yi) {
        //     shArr[0][4] = yawShArr[yi];
        //     ResolutionAnalysis resAnls =
        //             new ResolutionAnalysis(infile, pltLArr, dbgInfo, testRun, shArr);
        //     resAnls.shiftAnalysis(0, 5, pitchShArr, pltRan, gssRan, swmSetup, fCuts);
        // }

        // resAnls.dcSectorStripAnalysis(pltRan, gssRan, swim, fCuts);
        // resAnls.dcSectorThetaAnalysis(pltRan, gssRan, swim, fCuts);
        // resAnls.fmtRegionAnalysis(pltRan, swim, fCuts);
        // resAnls.deltaTminAnalysis(pltRan, swim, fCuts);
        // resAnls.plot1DCount(0, swim, fCuts, 1000);
        // resAnls.plot1DCount(1, swim, fCuts, 2000);
        // resAnls.plot1DCount(2, swim, fCuts, 100);
        // resAnls.plot1DCount(3, swim, fCuts, 150);
        // resAnls.plot1DCount(4, swim, fCuts, 90);
        // resAnls.plot2DCount(0, -1);

        return;
    }
}
