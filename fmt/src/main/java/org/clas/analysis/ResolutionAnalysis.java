package org.clas.analysis;

import java.util.ArrayList;
import java.util.List;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.groot.group.DataGroup;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.*;
import org.jlab.groot.math.F1D;
import org.clas.cross.Cluster;
import org.clas.cross.Constants;
import org.clas.cross.TrajPoint;

/** Key class of the program, in charge of all alignment tests. */
public class ResolutionAnalysis {
    private String infile;      // Input hipo file.
    private int nEvents;        // Number of events to run.
    private double[] fmtZ;      // z position of the layers in cm (before shifting).
    private double[] fmtAngle;  // strip angle in degrees.
    private double[][] shArr;   // 2D array of shifts to be applied.
    private boolean rotXYAlign; // Special setup needed for yaw & pitch alignment.
    private FiducialCuts fCuts; // FiducialCuts class instance.
    private TrkSwim swim;       // TrkSwim class instance.

    /**
     * Class constructor.
     * @param f      Input hipo file.
     * @param n      Number of events to run. Set to 0 to run all events in file.
     * @param shArr  Array of arrays describing all the shifts applied.
     * @param fCuts  FiducialCuts class instance.
     */
    public ResolutionAnalysis(String f, int n, double[][] shArr, FiducialCuts fCuts) {
        // Sanitize input.
        if (shArr.length != Constants.FMTLAYERS || shArr[0].length != 6) {
            System.err.printf("[ERROR] shArr is malformed!\n");
            System.exit(1);
        }

        this.fCuts   = fCuts;
        this.infile  = f;
        this.nEvents = n;
        this.shArr   = new double[][]{
                {shArr[0][0], shArr[0][1], shArr[0][2], shArr[0][3], shArr[0][4], shArr[0][5]},
                {shArr[1][0], shArr[1][1], shArr[1][2], shArr[1][3], shArr[1][4], shArr[1][5]},
                {shArr[2][0], shArr[2][1], shArr[2][2], shArr[2][3], shArr[2][4], shArr[2][5]}
        };

        // Set geometry parameters by reading from database.
        DatabaseConstantProvider dbProvider = new DatabaseConstantProvider(10, "rgf_spring2020");
        String fmtTable = "/geometry/fmt/fmt_layer_noshim";
        dbProvider.loadTable(fmtTable);

        fmtZ     = new double[Constants.FMTLAYERS]; // z position of the layers in cm.
        fmtAngle = new double[Constants.FMTLAYERS]; // strip angle in deg.

        for (int li = 0; li < Constants.FMTLAYERS; li++) {
            fmtZ[li]     = dbProvider.getDouble(fmtTable+"/Z",    li)/10;
            fmtAngle[li] = dbProvider.getDouble(fmtTable+"/Angle",li);
        }
    }

    /**
     * Run perturbatory shifts analysis.
     * @param var      Variable to which shifts are applied.
     * @param tShArr   List of shifts to try.
     * @param r        Plot range.
     * @param f        Gaussian fit range.
     * @param swmSetup Setup for initializing the TrkSwim class.
     * @return status int.
     */
    public int shiftAnalysis(String var, List<Double> tShArr, int r, int f, double[] swmSetup) {
        if (var != null && var.equals("rXY")) {
            this.rotXYAlign = true;
            this.fCuts.setRotXYAlign(true);
        }

        // Setup.
        int cn1 = tShArr.size();
        int cn2 = (var == null || var.equals("dZ") || var.equals("rZ")) ? 1 : cn1;

        int[] pos = new int[]{-1, -1};
        if      (var == null)       {}
        else if (var.equals("dXY")) {pos[0] = 0; pos[1] = 1;}
        else if (var.equals("dZ" )) {pos[0] = 2;}
        else if (var.equals("rXY")) {pos[0] = 3; pos[1] = 4;}
        else if (var.equals("rZ" )) {pos[0] = 5;}

        DataGroup[][] dgFMT = Data.createResDataGroups(Constants.FMTLAYERS, cn1, cn2, r);

        // 4 Params for each layer and tested shift: mean, sigma, sigma error, and chi^2.
        double[][][][] fitParamsArr = new double[4][Constants.FMTLAYERS][cn1][cn2];

        // Run.
        double[][] origArr = new double[Constants.FMTLAYERS][6]; // No arr deep copy in java.
        for (int i = 0; i < shArr.length; ++i) {
            for (int j = 0; j < shArr[0].length; ++j) {
                origArr[i][j] = shArr[i][j];
            }
        }

        for (int ci1 = 0; ci1 < cn1; ++ci1) {
            for (int ci2 = 0; ci2 < cn2; ++ci2) {
                double[] avgRotXY = new double[]{0.0, 0.0};
                for (int lyr = 0; lyr < Constants.FMTLAYERS; ++lyr) {
                    if (pos[0] != -1) shArr[lyr][pos[0]] = origArr[lyr][pos[0]] + tShArr.get(ci1);
                    if (pos[1] != -1) shArr[lyr][pos[1]] = origArr[lyr][pos[1]] + tShArr.get(ci2);
                    avgRotXY[0] += shArr[lyr][3];
                    avgRotXY[1] += shArr[lyr][4];
                }
                this.fCuts.resetCounters();
                avgRotXY[0] /= Constants.FMTLAYERS;
                avgRotXY[1] /= Constants.FMTLAYERS;

                this.swim = new TrkSwim(swmSetup, avgRotXY[0], avgRotXY[1]);
                runAnalysis(dgFMT[ci1][ci2], f);

                // Get fit quality assessment.
                for (int li = 0; li < Constants.FMTLAYERS; ++li) {
                    F1D gss = dgFMT[ci1][ci2].getF1D("fit_l"+(li+1));
                    fitParamsArr[0][li][ci1][ci2] = gss.getParameter(1);
                    fitParamsArr[1][li][ci1][ci2] = gss.getParameter(2);
                    fitParamsArr[2][li][ci1][ci2] = gss.parameter(2).error();
                    fitParamsArr[3][li][ci1][ci2] = gss.getParameter(3);
                }

                // Print cuts data to stdout.
                System.out.printf("FMT POSITION:\n             dX    dY    dZ    rotX  rotY  rotZ");
                for (int i = 0; i < shArr.length; ++i) {
                    System.out.printf("\n  layer %1d :", i+1);
                    for (int j = 0; j < shArr[0].length; ++j)
                        System.out.printf(" %5.2f", shArr[i][j]);
                }
                System.out.printf("\n");
                this.fCuts.printCutsInfo();
            }
        }

        // Print alignment data and draw plots. TODO. Print into file instead of stdout.
        System.out.printf("\nshifts = [");
        for (int ci = 0; ci < cn1; ++ci) System.out.printf(" %5.2f,", tShArr.get(ci));
        System.out.printf("]\n");
        for (int li = 0; li < Constants.FMTLAYERS; ++li) {
            System.out.printf("# LAYER %1d\n", li+1);
            StringBuilder[] fitStr = new StringBuilder[4];
            fitStr[0] = new StringBuilder();
            fitStr[1] = new StringBuilder();
            fitStr[2] = new StringBuilder();
            fitStr[3] = new StringBuilder();
            fitStr[0].append("lyr" + (li+1) + "_mean = np.array([");
            fitStr[1].append("lyr" + (li+1) + "_sigma = np.array([");
            fitStr[2].append("lyr" + (li+1) + "_sigmaerr = np.array([");
            fitStr[3].append("lyr" + (li+1) + "_chi2 = np.array([");

            for (int fi = 0; fi < 4; ++fi) {
                for (int ci1 = 0; ci1 < cn1; ++ci1) {
                    fitStr[fi].append("\n        [");
                    for (int ci2 = 0; ci2 < cn2; ++ci2)
                        fitStr[fi].append(String.format("%9.5f, ", fitParamsArr[fi][li][ci1][ci2]));
                    fitStr[fi].append("],");
                }
                fitStr[fi].append("\n])\n");
                System.out.printf("%s", fitStr[fi].toString());
            }
        }
        if (var == null) Data.drawResPlot(dgFMT[0][0], "Residuals");

        return 0;
    }

    /**
     * Generic function for running analysis, called by all others.
     * @param dg     DataGroup where analysis data is stored.
     * @param f      Range for the gaussian fit.
     * @return status int.
     */
    private int runAnalysis(DataGroup dg, int f) {
        int ei = 0; // Event number.
        HipoDataSource reader = new HipoDataSource();
        reader.open(infile);
        System.out.printf("\nRunning analysis...\n");

        // Loop through events.
        while (reader.hasEvent()) {
            if (nEvents != 0 && ei >= nEvents) break;
            if (ei % 50000 == 0) System.out.format("Analyzed %8d events...\n", ei);
            DataEvent event = reader.getNextEvent();
            ei++;

            ArrayList<TrajPoint[]> trajPoints = TrajPoint.getTrajPoints(
                    event, this.swim, this.fCuts, fmtZ, fmtAngle, shArr, 3, true);
            ArrayList<Cluster>[] clusters = Cluster.getClusters(event, this.fCuts, true);

            if (trajPoints == null || clusters == null) continue;

            for (TrajPoint[] tpArr : trajPoints) {
                for (int layer = 0; layer < tpArr.length; ++layer) {
                    for (Cluster c : clusters[layer]) {
                        int strip    = c.get_strip();
                        double costh = tpArr[layer].get_cosTh();
                        double res   = tpArr[layer].get_y() - c.get_y();

                        if (rotXYAlign) {
                            double theta_inv = 1/Math.toDegrees(Math.acos(costh));
                            if (!Double.isFinite(theta_inv)) continue;
                            dg.getH1F("hi_l"+(layer+1)).fill(res, theta_inv);
                        }
                        else {
                            dg.getH1F("hi_l"+(layer+1)).fill(res);
                        }
                        dg.getH2F("hi_strip_l"+(layer+1)).fill(res, strip);
                    }
                }
            }
        }
        System.out.format("Analyzed %8d events... Done!\n", ei);
        reader.close();

        // Fit residual plots
        for (int li = 1; li<= Constants.FMTLAYERS; ++li) {
            Data.fitRes(dg.getH1F("hi_l"+li), dg.getF1D("fit_l"+li), f);
        }

        return 0;
    }
}
