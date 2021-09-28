package org.clas.analysis;

import java.util.ArrayList;
import java.util.List;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.groot.group.DataGroup;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.*;
import org.jlab.groot.math.F1D;
import org.clas.data.Cluster;
import org.clas.data.TrajPoint;
import org.clas.test.Constants;
import org.clas.test.HipoHandler;

/** Key class of the program, in charge of all alignment tests. */
public class ResolutionAnalysis {
    private String infile;      // Input hipo file.
    private int nEvents;        // Number of events to run.
    private double[] fmtZ;      // z position of the layers in cm (before shifting).
    private double[] fmtAngle;  // strip angle in degrees.
    private double[][] shArr;   // 2D array of shifts to be applied.
    private double[][] origShArr; // Copy of shArr.
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
        if (shArr.length != Constants.FMTLAYERS || shArr[0].length != Constants.NVARS) {
            System.err.printf("[ERROR] shArr is malformed!\n");
            System.exit(1);
        }

        this.fCuts     = fCuts;
        this.infile    = f;
        this.nEvents   = n;
        this.shArr     = new double[Constants.FMTLAYERS][Constants.NVARS];
        this.origShArr = new double[Constants.FMTLAYERS][Constants.NVARS];
        for (int li = 0; li < Constants.FMTLAYERS; ++li) {
            for (int vi = 0; vi < Constants.NVARS; ++vi) {
                this.shArr[li][vi]     = shArr[li][vi];
                this.origShArr[li][vi] = shArr[li][vi];
            }
        }

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

    /** Setup and initialize TrkSwim class. */
    private int setupSwim(double[] swmSetup) {
        double[] avgRotXY = new double[]{0.0, 0.0};
        for (int lyr = 0; lyr < Constants.FMTLAYERS; ++lyr) {
            avgRotXY[0] += this.shArr[lyr][3];
            avgRotXY[1] += this.shArr[lyr][4];
        }
        avgRotXY[0] /= Constants.FMTLAYERS;
        avgRotXY[1] /= Constants.FMTLAYERS;

        this.swim = new TrkSwim(swmSetup, avgRotXY[0], avgRotXY[1]);

        return 0;
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

        DataGroup[][] dgFMT = HipoHandler.createResDataGroups(Constants.FMTLAYERS, cn1, cn2, r);

        // 4 Params for each layer and tested shift: mean, sigma, sigma error, and chi^2.
        double[][][][] fitParamsArr = new double[4][Constants.FMTLAYERS][cn1][cn2];

        if (!this.rotXYAlign) setupSwim(swmSetup);

        // Run.
        HipoDataSource reader = new HipoDataSource();
        reader.open(infile);

        for (int ci1 = 0; ci1 < cn1; ++ci1) {
            for (int ci2 = 0; ci2 < cn2; ++ci2) {
                // Setup.
                for (int li = 0; li < Constants.FMTLAYERS; ++li) {
                    if (pos[0]!=-1) this.shArr[li][pos[0]] =
                            this.origShArr[li][pos[0]]+tShArr.get(ci1);
                    if (pos[1]!=-1) this.shArr[li][pos[1]] =
                            this.origShArr[li][pos[1]]+tShArr.get(ci2);
                }
                this.fCuts.resetCounters();
                if (this.rotXYAlign) setupSwim(swmSetup);

                // Print run data.
                System.out.printf("\nRUN %3d/%3d:\n", ci1*cn2+ci2+1, cn1*cn2);
                System.out.printf("             dX    dY    dZ    rotX  rotY  rotZ");
                for (int li = 0; li < Constants.FMTLAYERS; ++li) {
                    System.out.printf("\n  layer %1d :", li+1);
                    for (int vi = 0; vi < this.shArr[0].length; ++vi)
                        System.out.printf(" %5.2f", this.shArr[li][vi]);
                }
                System.out.printf("\n");

                // Execute run.
                runAnalysis(reader, dgFMT[ci1][ci2], f);

                // Get fit quality assessment.
                for (int li = 0; li < Constants.FMTLAYERS; ++li) {
                    F1D gss = dgFMT[ci1][ci2].getF1D("fit_l"+(li+1));
                    fitParamsArr[0][li][ci1][ci2] = gss.getParameter(1);
                    fitParamsArr[1][li][ci1][ci2] = gss.getParameter(2);
                    fitParamsArr[2][li][ci1][ci2] = gss.parameter(2).error();
                    fitParamsArr[3][li][ci1][ci2] = gss.getParameter(3);
                }

                // Print cuts data to stdout.
                this.fCuts.printCutsInfo(); // NOTE. Change to printDetailedCutsInfo for details.
            }
        }
        reader.close();

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
                // TODO. Figure out where the wrong reference to the actual fit parameters is.
            }
        }
        if (var == null) HipoHandler.drawResPlot(dgFMT[0][0], "Residuals");

        return 0;
    }

    /**
     * Generic function for running analysis, called by all others.
     * @param reader HipoDataSource to stream events from source hipo file.
     * @param dg     DataGroup where analysis data is stored.
     * @param f      Range for the gaussian fit.
     * @return status int.
     */
    private int runAnalysis(HipoDataSource reader, DataGroup dg, int f) {
        int ei = 0; // Event number.
        reader.gotoEvent(0);

        // Loop through events.
        while (reader.hasEvent()) {
            if (nEvents != 0 && ei >= nEvents) break;
            DataEvent event = reader.getNextEvent();
            ei++;

            ArrayList<TrajPoint[]> trajPoints = TrajPoint.getTrajPoints(
                    event, this.swim, this.fCuts, this.fmtZ, this.fmtAngle, this.shArr, 3, true);
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

        // Fit residual plots
        for (int li = 1; li<= Constants.FMTLAYERS; ++li) {
            HipoHandler.fitRes(dg.getH1F("hi_l"+li), dg.getF1D("fit_l"+li), f);
        }

        return 0;
    }
}
