/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.clas.test;

import org.clas.analysis.TrkSwim;
import org.clas.analysis.FiducialCuts;
import org.clas.analysis.ResolutionAnalysis;

public class Main {
        private static int usage() {
        System.out.printf("Usage: program infile\n");
        System.out.printf("  * infile: String with the hipo input file.\n");

        return 0;
    }

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
                {-3.65, 0, 0, 0},
                { 0.20, 0, 0, 0},
                { 0.00, 0, 0, 0},
                { 0.05, 0, 0, 0}
        };



        TrkSwim trkSwim = new TrkSwim(swmSetup);
        FiducialCuts fCuts = new FiducialCuts();
        ResolutionAnalysis resAnls =
                new ResolutionAnalysis(infile, pltLArr, dbgInfo, testRun, shArr);

        // Run
        double[] inShArr = new double[]{0}; // Shifts to be tested.
        // resAnls.shiftAnalysis(0, 0, inShArr, pltRan, gssRan, trkSwim, fCuts);
        // resAnls.dcSectorStripAnalysis(pltRan, gssRan, trkSwim, fCuts);
        // resAnls.dcSectorThetaAnalysis(pltRan, gssRan, trkSwim, fCuts);
        // resAnls.fmtRegionAnalysis(pltRan, trkSwim, fCuts);
        // resAnls.plot1DCount(0, 1000);
        // resAnls.plot1DCount(1, 2000);
        resAnls.plot1DCount(2, 100);
        // resAnls.plot2DCount(0, -1);

        return;
    }
}
