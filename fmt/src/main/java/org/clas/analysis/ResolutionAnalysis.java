package org.clas.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.groot.group.DataGroup;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.*;
import org.jlab.groot.math.F1D;
import org.jlab.groot.data.TDirectory;
import org.clas.data.Cluster;
import org.clas.data.TrajPoint;
import org.clas.test.Constants;
import org.clas.test.HipoHandler;

/** Key class of the program, in charge of all alignment tests. */
public class ResolutionAnalysis {
    private String infile;        // Input hipo file.
    private int nEvents;          // Number of events to run.
    private int cutsInfo;         // Int describing how much info on the cuts should be printed.
    private boolean sPlts;        // Boolean describing if plots are to be shown or saved.
    private double fmtDrift;      // thickness of the drift gap
    private double[] fmtZ;        // z position of the layers in cm (before shifting).
    private double[] fmtAngle;    // strip angle in degrees.
    private double[][] shArr;     // 2D array of shifts to be applied.
    private double[][] origShArr; // Copy of shArr, not supposed to change.
    private boolean rotXYAlign;   // Special setup needed for yaw & pitch alignment.
    private FiducialCuts fCuts;   // FiducialCuts class instance.
    private TrkSwim swim;         // TrkSwim class instance.

    /**
     * Class constructor.
     * @param f         Input hipo file.
     * @param n         Number of events to run. Set to 0 to run all events in file.
     * @param c         Amount of information on the applied cuts that should be printed.
     * @param shArr     Array of arrays describing all the shifts applied.
     * @param fCuts     FiducialCuts class instance.
     * @param var       CCDB variation to be used.
     * @param showPlots boolean describing if plots are to be shown or saved.
     */
    public ResolutionAnalysis(String f, int n, int c, double[][] shArr, FiducialCuts fCuts,
            String var, boolean showPlots) {
        // Sanitize input.
        if (shArr.length != Constants.FMTLAYERS || shArr[0].length != Constants.NVARS) {
            System.err.printf("[ERROR] shArr is malformed!\n");
            System.exit(1);
        }

        this.infile    = f;
        this.nEvents   = n;
        this.cutsInfo  = c;
        this.fCuts     = fCuts;
        this.sPlts     = showPlots;
        this.origShArr = new double[Constants.FMTLAYERS][Constants.NVARS];
        this.shArr     = new double[Constants.FMTLAYERS][Constants.NVARS];
        for (int li = 0; li < Constants.FMTLAYERS; ++li) {
            for (int vi = 0; vi < Constants.NVARS; ++vi) {
                this.origShArr[li][vi] = shArr[li][vi];
                this.shArr[li][vi]     = shArr[li][vi];
            }
        }

        // Set geometry parameters by reading from database.
        // NOTE. We purposefully don't grab data from the FMT alignment table to avoid confusion.
        fmtZ     = new double[Constants.FMTLAYERS]; // z position of the layers in cm.
        fmtAngle = new double[Constants.FMTLAYERS]; // strip angle in deg.

        DatabaseConstantProvider dbProvider = new DatabaseConstantProvider(10, var);
        dbProvider.loadTable(Constants.FMTTABLELOC);
        dbProvider.loadTable("/geometry/fmt/fmt_global");
        fmtDrift = dbProvider.getDouble(Constants.FMTTABLEGLO+"/hDrift",0)/10;
        for (int li = 0; li < Constants.FMTLAYERS; li++) {
            fmtZ[li]     = dbProvider.getDouble(Constants.FMTTABLELOC+"/Z",    li)/10
                         + fmtDrift/2;
            fmtAngle[li] = dbProvider.getDouble(Constants.FMTTABLELOC+"/Angle",li);
        }
    }

    /** Setup and initialize TrkSwim class. */
    private boolean setupSwim(double[] swmSetup) {
        double[] avgRotXY = new double[]{0.0, 0.0};
        for (int lyr = 0; lyr < Constants.FMTLAYERS; ++lyr) {
            avgRotXY[0] += this.shArr[lyr][3];
            avgRotXY[1] += this.shArr[lyr][4];
        }
        avgRotXY[0] /= Constants.FMTLAYERS;
        avgRotXY[1] /= Constants.FMTLAYERS;
        this.swim = new TrkSwim(swmSetup, avgRotXY[0], avgRotXY[1]);
        return false;
    }

    /**
     * Run perturbatory shifts analysis.
     * @param var      Variable to which shifts are applied.
     * @param tShArr   List of shifts to try.
     * @param swmSetup Setup for initializing the TrkSwim class.
     * @return Status boolean.
     */
    public boolean shiftAnalysis(String var, List<Double> tShArr, double[] swmSetup) {
        if (var != null && var.equals("rXY")) {
            this.rotXYAlign = true;
            this.fCuts.setRotXYAlign(true);
        }

        // Setup.
        int cn1 = tShArr.size();
        int cn2 = (var == null || var.equals("dZ") || var.equals("rZ")) ? 1 : cn1;

        int[] pos = new int[]{-1, -1};
        if      (var == null)       {} // Not a mistake! --- just making sure var exists.
        else if (var.equals("dXY")) {pos[0] = 0; pos[1] = 1;}
        else if (var.equals("dZ" )) {pos[0] = 2;}
        else if (var.equals("rXY")) {pos[0] = 3; pos[1] = 4;}
        else if (var.equals("rZ" )) {pos[0] = 5;}

        DataGroup[][] dgFMT = HipoHandler.createResDataGroups(cn1, cn2);
        TDirectory dir = new TDirectory(); // Directory where plots are to be saved.

        // Four params for each layer and tested shift: mean, sigma, sigma error, and chi^2.
        double[][][][] fitParamsArr = new double[4][Constants.FMTLAYERS][cn1][cn2];
        if (!this.rotXYAlign) setupSwim(swmSetup);

        // Run.
        HipoDataSource reader = new HipoDataSource();
        reader.open(infile);

        for (int ci1 = 0; ci1 < cn1; ++ci1) {
            for (int ci2 = 0; ci2 < cn2; ++ci2) {
                // Run setup.
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
                if (runAnalysis(reader, dgFMT[ci1][ci2])) return true;

                // Get fit quality assessment.
                for (int li = 0; li < Constants.FMTLAYERS; ++li) {
                    F1D gss = dgFMT[ci1][ci2].getF1D("fit_l"+(li+1));
                    fitParamsArr[0][li][ci1][ci2] = gss.getParameter(1);
                    fitParamsArr[1][li][ci1][ci2] = gss.getParameter(2);
                    fitParamsArr[2][li][ci1][ci2] = gss.parameter(2).error();
                    fitParamsArr[3][li][ci1][ci2] = gss.getParameter(3);
                }

                // Print cuts data to stdout.
                if      (this.cutsInfo == 1) this.fCuts.printCutsInfo();
                else if (this.cutsInfo == 2) this.fCuts.printDetailedCutsInfo();

                // Draw and save residuals plots.
                HipoHandler.drawResPlot(dir, "Residuals " + (ci1*cn2+ci2+1) + "-" + (cn1*cn2),
                                        dgFMT[ci1][ci2], var == null && this.sPlts);
            }
        }
        reader.close();

        // Draw, save, and (maybe) show alignment plots.
        if (var != null)
            HipoHandler.drawAlignPlot(dir, var, fitParamsArr, this.origShArr, tShArr, this.sPlts);

        File fname = new File("histograms.hipo");
        fname.delete(); // Delete file in case it already exists.
        dir.writeFile("histograms.hipo"); // Write file.

        return false;
    }

    /**
     * Generic function for running analysis, called by all others.
     * @param reader HipoDataSource to stream events from source hipo file.
     * @param dg     DataGroup where analysis data is stored.
     * @return Status boolean.
     */
    private boolean runAnalysis(HipoDataSource reader, DataGroup dg) {
        int ei = 0;          // Event number.
        reader.gotoEvent(0); // Reset reader counter.

        // Loop through events.
        while (reader.hasEvent()) {
            if (nEvents != 0 && ei >= nEvents) break;
            DataEvent event = reader.getNextEvent();
            ei++;

            ArrayList<TrajPoint[]> trajPoints = TrajPoint.getTrajPoints(
                    event, this.swim, this.fCuts, this.fmtZ, this.fmtAngle, this.shArr, Constants.FMTLAYERS, true);
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
                        dg.getH2F("hi_strip2D_l"+(layer+1)).fill(res, strip);
                        dg.getH1F("hi_strip1D_l"+(layer+1)).fill(strip);
                    }
                }
            }
        }

        // Fit residual plots
        for (int li = 1; li<= Constants.FMTLAYERS; ++li)
            if (HipoHandler.fitRes(dg.getH1F("hi_l"+li), dg.getF1D("fit_l"+li))) return true;

        return false;
    }
}
