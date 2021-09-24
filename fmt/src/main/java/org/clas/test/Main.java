package org.clas.test;

import java.util.HashMap;
import java.util.List;
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

        String file = params.get('f').get(0);
        int nEvents = params.get('n') == null ? 0 : Integer.parseInt(params.get('n').get(0));
        boolean drawPlots = params.get('v') == null ? true : false;

        // === Setup ===============================================================================
        setupGroot();
        double[] swmSetup = new double[]{-0.75, -1.0, -3.0};
        int plotRange = 5; // Plotting range.
        int fitRange  = 4; // Fitting range.

        // NOTE. Since the y axis is pointing up, what we call yaw is actually inverted from what
        //       you would expect in aviation standards.
        double[][] shArr = new double[][]{ // Alignment to be tested.
                // z     x     y    phi   yaw  pitch
                { 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}, // Global shifts
                { 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}, // Layer 1 shifts
                { 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}, // Layer 2 shifts
                { 0.00, 0.00, 0.00, 0.00, 0.00, 0.00}  // Layer 3 shifts
        };
        // Update shArr based on alignment info given by user.
        for (int i = 1; i < shArr.length; ++i) {
            if (params.get('z') != null) shArr[i][0] += Double.parseDouble(params.get('z').get(i-1));
            if (params.get('x') != null) shArr[i][1] += Double.parseDouble(params.get('x').get(i-1));
            if (params.get('y') != null) shArr[i][2] += Double.parseDouble(params.get('y').get(i-1));
            if (params.get('Z') != null) shArr[i][3] += Double.parseDouble(params.get('Z').get(i-1));
            if (params.get('X') != null) shArr[i][4] += Double.parseDouble(params.get('X').get(i-1));
            if (params.get('Y') != null) shArr[i][5] += Double.parseDouble(params.get('Y').get(i-1));
        }
        FiducialCuts fCuts = new FiducialCuts();
        TrkSwim swim = new TrkSwim(swmSetup, shArr[0][4], shArr[0][5]);

        // === Alignment ===========================================================================
        int varAlign = 0;

        if (params.get('v') == null) { // Check current best results.
            double[] zShArr = new double[]{0.00};
            ResolutionAnalysis resAnls = new ResolutionAnalysis(file, nEvents, shArr,
                    drawPlots, false);
            resAnls.shiftAnalysis(0, 0, zShArr, plotRange, fitRange, swmSetup, fCuts);
        }
        else if (params.get('v').get(0).equals("dZ")) {
            double[] zShArr = new double[]
                    {-0.50, -0.45, -0.40, -0.35, -0.30, -0.25, -0.20, -0.15, -0.10, -0.05, 0.00,
                      0.05,  0.10,  0.15,  0.20,  0.25,  0.30,  0.35,  0.40,  0.45,  0.50};
            ResolutionAnalysis resAnls = new ResolutionAnalysis(file, nEvents, shArr,
                    drawPlots, false);
            resAnls.shiftAnalysis(0, 0, zShArr, plotRange, fitRange, swmSetup, fCuts);
        }
        else if (params.get('v').get(0).equals("dXY")) {
            double[] xShArr = new double[]
                    {0.05, 0.06, 0.07, 0.08, 0.09, 0.10, 0.11, 0.12, 0.13, 0.14, 0.15};
            double[] yShArr = new double[]
                    {0.05, 0.06, 0.07, 0.08, 0.09, 0.10, 0.11, 0.12, 0.13, 0.14, 0.15};
            double orig_x = shArr[0][1];
            for (int xi = 0; xi < xShArr.length; ++xi) {
                shArr[0][1] = orig_x + xShArr[xi];
                ResolutionAnalysis resAnls = new ResolutionAnalysis(file, nEvents,
                        shArr, false, false);
                resAnls.shiftAnalysis(0, 2, yShArr, plotRange, fitRange, swmSetup, fCuts);
            }
        }
        else if (params.get('v').get(0).equals("rZ")) {
            double[] phiShArr = new double[]
                    {-2.0, -1.9, -1.8, -1.7, -1.6, -1.5, -1.4, -1.3, -1.2, -1.1,
                     -1.0, -0.9, -0.8, -0.7, -0.6, -0.5, -0.4, -0.3, -0.2, -0.1,  0.0};
            ResolutionAnalysis resAnls = new ResolutionAnalysis(file, nEvents, shArr,
                    drawPlots, false);
            resAnls.shiftAnalysis(0, 3, phiShArr, plotRange, fitRange, swmSetup, fCuts);
        }
        else if (params.get('v').get(0).equals("rXY")) {
            double[] yawShArr = new double[]
                    {-1.0, -0.5, 0.00, 0.05, 0.5, 1.0};
            double[] pitchShArr = new double[]
                    {-1.0, -0.5, 0.00, 0.05, 0.5, 1.0};
            fCuts.setYPAlign(true);
            double orig_yaw = shArr[0][4];
            for (int yi = 0; yi<yawShArr.length; ++yi) {
                shArr[0][4] = orig_yaw + yawShArr[yi];
                ResolutionAnalysis resAnls = new ResolutionAnalysis(file, nEvents,
                        shArr, false, true);
                resAnls.shiftAnalysis(0, 5, pitchShArr, plotRange, fitRange, swmSetup, fCuts);
            }
        }
        else {
            System.err.printf("<var> argument is invalid. Exiting...\n");
            System.exit(1);
        }
    }
}
