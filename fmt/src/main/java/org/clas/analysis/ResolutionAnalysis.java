package org.clas.analysis;

import java.util.ArrayList;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.groot.group.DataGroup;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.*;
import org.jlab.groot.math.F1D;
import org.clas.cross.Cluster;
import org.clas.cross.Constants;
import org.clas.cross.TrajPoint;

/** Key class of the program, in charge of alignment. */
public class ResolutionAnalysis {
    // Class variables:
    private String infile;
    private int nEvents;       // Number of events to run.
    private double[] fmtZ;     // z position of the layers in cm (before shifting).
    private double[] fmtAngle; // strip angle in degrees.
    private double[][] shArr;  // 2D array of shifts to be applied.
    private boolean drawPlots; // Boolean describing if plots are to be drawn.
    private boolean ypAlign;   // Special setup needed for yaw & pitch alignment.

    /**
     * Class constructor.
     * @param infile      Input hipo file.
     * @param nEvents     Number of events to run. Set to 0 to run all events in file.
     * @param shArr       Array of arrays describing all the shifts applied:
     *                      * [0] :  global [dZ,dX,dY,rotZ,rotX,rotY] shift.
     *                      * [1] : layer 1 [dZ,dX,dY,rotZ,rotX,rotY] shift.
     *                      * [2] : layer 2 [dZ,dX,dY,rotZ,rotX,rotY] shift.
     *                      * [3] : layer 3 [dZ,dX,dY,rotZ,rotX,rotY] shift.
     * @param drawPlots   Boolean describing if plots are to be drawn.
     * @param ypAlign     Special setup needed for yaw & pitch alignment.
     */
    public ResolutionAnalysis(String infile, int nEvents, double[][] shArr, boolean drawPlots,
            boolean ypAlign) {
        this.drawPlots = drawPlots;
        this.ypAlign   = ypAlign;

        // Sanitize input.
        if (shArr.length != 4 || shArr[0].length != 6) {
            System.err.printf("[ERROR] shArr is malformed!\n");
            System.exit(1);
        }

        this.infile  = infile;
        this.nEvents = nEvents;
        this.shArr   = new double[][]{
            {shArr[0][0], shArr[0][1], shArr[0][2], shArr[0][3], shArr[0][4], shArr[0][5]},
            {shArr[1][0], shArr[1][1], shArr[1][2], shArr[1][3], shArr[1][4], shArr[1][5]},
            {shArr[2][0], shArr[2][1], shArr[2][2], shArr[2][3], shArr[2][4], shArr[2][5]},
            {shArr[3][0], shArr[3][1], shArr[3][2], shArr[3][3], shArr[3][4], shArr[3][5]}
        };

        // Set geometry parameters by reading from database.
        DatabaseConstantProvider dbProvider = new DatabaseConstantProvider(10, "rgf_spring2020");
        String fmtTable = "/geometry/fmt/fmt_layer_noshim";
        dbProvider.loadTable(fmtTable);

        fmtZ     = new double[Constants.FMTLAYERS]; // z position of the layers in cm.
        fmtAngle = new double[Constants.FMTLAYERS]; // strip angle in deg.

        for (int li = 0; li < Constants.FMTLAYERS; li++) {
            fmtZ[li]     = dbProvider.getDouble(fmtTable+"/Z",li)/10;
            fmtAngle[li] = dbProvider.getDouble(fmtTable+"/Angle",li);
        }
    }

    /** Execute the runAnalysis method below with less input. */
    private int runAnalysis(int func, TrkSwim swim, FiducialCuts fcuts, DataGroup[] dgFMT) {
        return runAnalysis(func, -1, swim, fcuts, dgFMT, -1);
    }

    /**
     * Generic function for running analysis, called by all others.
     * @param func   Type of analysis to be ran:
     *                 * 0 : perturbatory shifts.
     *                 * 1 : dc sector vs strip.
     *                 * 2 : dc sector vs theta.
     *                 * 3 : fmt regions plot.
     * @param opt    Variable used in different manners by different types:
     *                 * func == 0 : canvass index (ci).
     *                 * func == 1 : number of DC sectors (sn).
     *                 * func == 2 : number of DC sectors (sn).
     *                 * func == 3 : unused.
     * @param swim   TrkSwim class instance.
     * @param fcuts  FiducialCuts class instance.
     * @param dgFMT  Array of data groups where analysis data is stored.
     * @param g      Range for the gaussian fit.
     * @return status int.
     */
    private int runAnalysis(int func, int opt, TrkSwim swim, FiducialCuts fcuts, DataGroup[] dgFMT,
            int g) {
        // Sanitize input.
        if (func<0 || func>4) return 1;

        // Print the shifts applied.
        System.out.printf("SHIFTS APPLIED:\n");
        for (int li = 1; li<= Constants.FMTLAYERS; ++li) {
            System.out.printf("[");
            for (int vi=0; vi<shArr[li].length; ++vi) {
                System.out.printf("%6.2f,", shArr[0][vi] + shArr[li][vi]);
                if (vi == shArr[li].length-1) System.out.printf("\b");
            }
            System.out.printf("]\n");
        }

        int ei = 0; // Event number.
        HipoDataSource reader = new HipoDataSource();
        reader.open(infile);
        System.out.printf("\nRunning analysis...\n");

        // Loop through events.
        while (reader.hasEvent()) {
            if (nEvents != 0 && ei >= nEvents) break;
            if (ei%50000==0) System.out.format("Analyzed %8d events...\n", ei);
            DataEvent event = reader.getNextEvent();
            ei++;

            ArrayList<TrajPoint[]> trajPoints = TrajPoint.getTrajPoints(event, swim,
                    fcuts, fmtZ, fmtAngle, shArr, 3, true);
            ArrayList<Cluster>[] clusters = Cluster.getClusters(event, fcuts, true);

            if (trajPoints==null || clusters==null) continue;

            for (TrajPoint[] tpArr : trajPoints) {
                for (int layer=0; layer<tpArr.length; ++layer) {
                    for (Cluster c : clusters[layer]) {
                        int strip    = c.get_strip();
                        double costh = tpArr[layer].get_cosTh();
                        double res   = tpArr[layer].get_y() - c.get_y();

                        if (ypAlign) {
                            double theta_inv = 1/Math.toDegrees(Math.acos(costh));
                            if (!Double.isFinite(theta_inv)) continue;
                            dgFMT[opt].getH1F("hi_cluster_res_l"+(layer+1)).fill(res, theta_inv);
                        }
                        else {
                            dgFMT[opt].getH1F("hi_cluster_res_l"+(layer+1)).fill(res);
                        }
                        dgFMT[opt].getH2F("hi_cluster_res_strip_l"+(layer+1)).fill(res, strip);
                    }
                }
            }
        }
        System.out.format("Analyzed %8d events... Done!\n", ei);
        reader.close();

        fcuts.printCutsInfo(); // Show the lost tracks due to the cuts.

        // Fit residual plots
        if (func == 0) {
            for (int li = 1; li<= Constants.FMTLAYERS; ++li) {
                Data.fitRes(dgFMT[opt].getH1F("hi_cluster_res_l"+li),
                        dgFMT[opt].getF1D("f1_res_l"+li), g);
            }
        }
        else if (func == 1 || func == 2) {
            for (int si=0; si<opt; ++si) {
                for (int li = 1; li<= Constants.FMTLAYERS; ++li) {
                    Data.fitRes(dgFMT[si].getH1F("hi_cluster_res_l"+li),
                            dgFMT[si].getF1D("f1_res_l"+li), g);
                }
            }
        }

        return 0;
    }

    /**
     * Run perturbatory shifts analysis. Currently, only z shifts are implemented.
     * @param lyr      Layer to which shifts are applied (0 for global shift).
     * @param var      Variable in layer to which shifts are applied:
     *                   * var == 0 : z
     *                   * var == 1 : x
     *                   * var == 2 : y
     *                   * var == 3 : phi
     * @param inShArr  Array of shifts to try.
     * @param r        Plot range.
     * @param g        Gaussian range.
     * @param swmSetup Setup for initializing the TrkSwim class.
     * @param fcuts    FiducialCuts class instance.
     * @return status int.
     */
    public int shiftAnalysis(int lyr, int var, double[] inShArr, int r, int g, double[] swmSetup,
            FiducialCuts fcuts) {

        int func = 0;
        // Setup.
        int cn = inShArr.length;
        String[] titleArr = new String[cn];
        for (int ci=0; ci < cn; ++ci) titleArr[ci] = "shift ["+lyr+","+var+"] : " + inShArr[ci];

        DataGroup[] dgFMT = Data.createResDataGroups(func, Constants.FMTLAYERS, cn, r);

        double[][] meanArr     = new double[Constants.FMTLAYERS][cn];
        double[][] sigmaArr    = new double[Constants.FMTLAYERS][cn];
        double[][] sigmaErrArr = new double[Constants.FMTLAYERS][cn];
        double[][] chiSqArr    = new double[Constants.FMTLAYERS][cn];

        // Run.
        double origVal = shArr[lyr][var];
        for (int ci=0; ci<cn; ++ci) {
            shArr[lyr][var] = origVal + inShArr[ci];
            fcuts.resetCounters();
            TrkSwim swim = new TrkSwim(swmSetup, shArr[0][4], shArr[0][5]);
            runAnalysis(func, ci, swim, fcuts, dgFMT, g);

            // Get mean and sigma from fit.
            for (int li = 0; li< Constants.FMTLAYERS; ++li) {
                F1D gss = dgFMT[ci].getF1D("f1_res_l"+(li+1));
                meanArr[li][ci]     = gss.getParameter(1);
                sigmaArr[li][ci]    = gss.getParameter(2);
                sigmaErrArr[li][ci] = gss.parameter(2).error();
                chiSqArr[li][ci]    = gss.getParameter(3);
            }
        }

        // Print alignment data and draw plots.
        if (var==0) System.out.printf("\nz_");
        if (var==1) System.out.printf("\nx_");
        if (var==2) System.out.printf("\ny_");
        if (var==3) System.out.printf("\nphi_");
        if (var==4) System.out.printf("\nyaw_");
        if (var==5) System.out.printf("\npitch_");
        System.out.printf("shift   = [");
        for (int ci=0; ci<cn; ++ci) System.out.printf("%9.5f, ", inShArr[ci]);
        System.out.printf("\b\b]\n");
        for (int li = 0; li< Constants.FMTLAYERS; ++li) {
            System.out.printf("# layer %1d:\n", li+1);
            System.out.printf("mean      = [");
            for (int ci=0; ci<cn; ++ci) System.out.printf("%9.5f, ", meanArr[li][ci]);
            System.out.printf("\b\b]\nsigma     = [");
            for (int ci=0; ci<cn; ++ci) System.out.printf("%9.5f, ", sigmaArr[li][ci]);
            System.out.printf("\b\b]\nsigma_err = [");
            for (int ci=0; ci<cn; ++ci) System.out.printf("%9.5f, ", sigmaErrArr[li][ci]);
            System.out.printf("\b\b]\nchi_sq    = [");
            for (int ci=0; ci<cn; ++ci) System.out.printf("%9.2f, ", chiSqArr[li][ci]);
            System.out.printf("\b\b]\n");
        }
        if (drawPlots) Data.drawResPlots(dgFMT, cn, titleArr);

        return 0;
    }

    /**
     * Run DC sector strip analysis.
     * @param r     Plot range.
     * @param g     Gaussian range.
     * @param swim  TrkSwim class instance.
     * @param fcuts FiducialCuts class instance.
     * @return status int.
     */
    public int dcSectorStripAnalysis(int r, int g, TrkSwim swim, FiducialCuts fcuts) {
        int func = 1;
        String[] titleArr = new String[Constants.DCSECTORS];
        for (int si = 0; si < Constants.DCSECTORS; ++si)
            titleArr[si] = "DC sector " + (si + 1);

        // Run.
        DataGroup[] dgFMT = Data.createResDataGroups(
                func, Constants.FMTLAYERS, Constants.DCSECTORS, r
        );
        runAnalysis(func, Constants.DCSECTORS, swim, fcuts, dgFMT, g);
        if (drawPlots)
            Data.drawResPlots(dgFMT, Constants.DCSECTORS, titleArr);

        return 0;
    }

    /**
     * Run DC sector theta analysis.
     * @param r     Plot range.
     * @param g     Gaussian range.
     * @param swim  TrkSwim class instance.
     * @param fcuts FiducialCuts class instance.
     * @return status int.
     */
    public int dcSectorThetaAnalysis(int r, int g, TrkSwim swim, FiducialCuts fcuts) {
        int func = 2;
        String[] titleArr = new String[Constants.DCSECTORS];
        for (int si = 0; si < Constants.DCSECTORS; ++si)
            titleArr[si] = "DC sector " + (si+1);

        // Run.
        DataGroup[] dgFMT = Data.createResDataGroups(
                func, Constants.FMTLAYERS, Constants.DCSECTORS, r
        );
        runAnalysis(func, Constants.DCSECTORS, swim, fcuts, dgFMT, g);
        if (drawPlots)
            Data.drawResPlots(dgFMT, Constants.DCSECTORS, titleArr);

        return 0;
    }

    /**
     * Run analysis and draw a different plot for each FMT region.
     * @param r     Residuals range in plots.
     * @param swim  TrkSwim class instance.
     * @param fcuts FiducialCuts class instance.
     * @return status int.
     */
    public int fmtRegionAnalysis(int r, TrkSwim swim, FiducialCuts fcuts) {
        int func = 3;
        // Set canvases' stuff.
        String title = "FMT regions";
        DataGroup[] dgFMT = Data.createFMTRegionsDataGroup(r);

        // Run.
        runAnalysis(func, swim, fcuts, dgFMT);
        fcuts.printCutsInfo();
        if (drawPlots) Data.drawPlots(dgFMT, title);

        return 0;
    }

    public int deltaTminAnalysis(int r, TrkSwim swim, FiducialCuts fcuts) {
        int func = 4;
        String title = "delta Tmin";
        DataGroup[] dgFMT = Data.createResDataGroups(func, Constants.FMTLAYERS, 1, r);

        // Run.
        runAnalysis(func, swim, fcuts, dgFMT);
        if (drawPlots) Data.drawPlots(dgFMT, title);

        return 0;
    }

    /**
     * Draw a 1D plot by counting a pre-defined variable.
     * @param var Variable to be counted:
     *              * 0 : clusters' Tmin.
     *              * 1 : clusters' energy.
     *              * 2 : tracks' z.
     *              * 3 : tmin between clusters.
     *              * 4 : theta of each track.
     *              * 5 : tracks' z (special case).
     * @param r   Range for the plot.
     * @return status int.
     */
    public int plot1DCount(int var, TrkSwim swim, FiducialCuts fcuts, int r) {
        // Sanitize input.
        if (var < 0 || var > 9) return 0;

        String title = null;
        if (var == 0)                                     title = "Tmin count";
        if (var == 1)                                     title = "energy count";
        if (var == 2 || var == 5 || var == 6 || var == 9) title = "vertex z";
        if (var == 3)                                     title = "delta Tmin";
        if (var == 4)                                     title = "track theta";
        if (var == 7)                                     title = "track status";
        if (var == 8)                                     title = "track chi^2";

        DataGroup[] dgFMT = Data.create1DDataGroup(var, Constants.FMTLAYERS, r);

        int totalCount = 0;
        int correctCount = 0;


        // Run.
        int ei = 0; // Event number.
        HipoDataSource reader = new HipoDataSource();
        reader.open(infile);
        System.out.printf("\nPlotting...\n");

        // Loop through events.
        while (reader.hasEvent()) {
            if (nEvents != 0 && ei >= nEvents) break;
            if (ei % 50000 == 0) System.out.format("Analyzed %8d events...\n", ei);
            DataEvent event = reader.getNextEvent();
            ei++;

            if (var == 0 || var == 1) {
                ArrayList<Cluster>[] clusters = Cluster.getClusters(event, fcuts, false);
                if (clusters == null) continue;
                for (ArrayList<Cluster> clist : clusters) {
                    if (clist == null) continue;
                    for (Cluster c : clist) {
                        if (c == null) continue;
                        if (var == 0)
                            dgFMT[0].getH1F("clusters" + (c.get_fmtLyr())).fill(c.get_tMin());
                        if (var == 1)
                            dgFMT[0].getH1F("clusters" + (c.get_fmtLyr())).fill(c.get_energy());
                    }
                }
            }
            else if (var == 2) {
                // Plot z from particle bank.
                DataBank traj = Data.getBank(event, "REC::Traj");
                DataBank part = Data.getBank(event, "REC::Particle");
                if (traj == null || part == null) continue;
                for (int tri = 0; tri < traj.rows(); ++tri) {
                    int detector = traj.getByte("detector", tri);
                    int li = traj.getByte("layer", tri);
                    int pi = traj.getShort("pindex", tri);
                    int status = (int) part.getShort("status", pi)/1000;
                    // Check that FMT was used.
                    if (status == 2) continue;

                    // Get particle data.
                    double z  = (double) part.getFloat("vz", pi);
                    dgFMT[0].getH1F("tracks").fill(z);
                }
            }
            else if (var == 3) {
                System.err.printf("Method dropped.\n");
                System.exit(1);
            }
            else if (var == 4) {
                ArrayList<TrajPoint[]> trajPoints =
                        TrajPoint.getTrajPoints(event, swim, fcuts, fmtZ, fmtAngle, shArr, 3,false);
                if (trajPoints == null) continue;
                for (TrajPoint[] trjparr : trajPoints) {
                    for (TrajPoint trjp : trjparr) {
                        dgFMT[0].getH1F("tracks").fill(Math.toDegrees(trjp.get_cosTh()));
                    }
                }
            }
            else if (var == 5) {
                // Plot z from DC and FMT track banks.
                DataBank dcTracks  = Data.getBank(event, "TimeBasedTrkg::TBTracks");
                DataBank fmtTracks = Data.getBank(event, "FMTRec::Tracks");
                DataBank recTracks = Data.getBank(event, "REC::Track");
                DataBank particles = Data.getBank(event, "REC::Particle");
                if (dcTracks == null || fmtTracks == null || recTracks == null || particles == null)
                    continue;

                // Filter tracks by number of tracks --- use only events with one track.
                if (fmtTracks.rows() <= 1) continue;

                for (int dc_tri = 0; dc_tri < dcTracks.rows(); ++dc_tri) {
                    for (int fmt_tri = 0; fmt_tri < fmtTracks.rows(); ++fmt_tri) {
                        // Match DC and FMT tracks.
                        if (dcTracks.getShort("id", dc_tri) != fmtTracks.getShort("id", fmt_tri))
                            continue;

                        // Filter by DC sector.
//                        if ((int) fmtTracks.getByte("sector", fmt_tri) != 1) continue;

                        // Filter by number of FMT measurements.
                        if (fmtTracks.getShort("status", fmt_tri) != 3) continue;

                        // Filter by PID (Only electrons).
                        int pid = -1;
                        for (int rec_tri = 0; rec_tri < recTracks.rows(); ++rec_tri) {
                            if (fmt_tri != recTracks.getShort("index", rec_tri)) continue;
                            int pindex = (int) recTracks.getShort("pindex", rec_tri);
                            pid = particles.getInt("pid", pindex);
                            break;
                        }
                        if (pid != -211) continue;

                        dgFMT[0].getH1F("DCTB tracks").fill((double) dcTracks
                                .getFloat("Vtx0_z", dc_tri));
                        dgFMT[0].getH1F("FMT tracks") .fill((double) fmtTracks
                                .getFloat("Vtx0_z", fmt_tri));
                    }
                }
            }
            else if (var == 6) {
                // Plot z from FMT track bank.
                DataBank fmtTracks = Data.getBank(event, "FMT::Tracks");
                DataBank recTracks = Data.getBank(event, "REC::Track");
                DataBank particles = Data.getBank(event, "REC::Particle");
                if (fmtTracks == null || recTracks == null || particles == null) continue;

                for (int tri = 0; tri < fmtTracks.rows(); ++tri) {
                    // Filter by DC sector.
                    if ((int) fmtTracks.getByte("sector", tri) != 1) continue;

                    // Filter tracks by their number of measurements.
                    if ((int) fmtTracks.getByte("NDF", tri) != 3) continue;

                    // Filter tracks by pid.
                    int pid = -1;
                    for (int rtri = 0; rtri < recTracks.rows(); ++rtri) {
                        if (tri != recTracks.getShort("index", rtri)) continue;
                        int pindex = (int) recTracks.getShort("pindex", rtri);
                        pid = particles.getInt("pid", pindex);
                        break;
                    }
                    if (pid !=   11) continue; // e-

                    dgFMT[0].getH1F("FMT tracks").fill((double) fmtTracks.getFloat("Vtx0_z", tri));
                }
            }
            else if (var == 7) {
                // Plot the status from the FVT tracks.
                DataBank fmtTracks = Data.getBank(event, "FMT::Tracks");
                if (fmtTracks == null) continue;

                for (int tri = 0; tri < fmtTracks.rows(); ++tri) {
                    int status = (int) fmtTracks.getShort("status", tri);
                    dgFMT[0].getH1F("track_status").fill(status);
                }
            }
            else if (var == 8) {
                // Plot the status from the FVT tracks.
                DataBank fmtTracks = Data.getBank(event, "FMTRec::Tracks");
                if (fmtTracks == null) continue;

                for (int tri = 0; tri < fmtTracks.rows(); ++tri) {
                    if ((int) fmtTracks.getShort("status", tri) > 50) continue;
                    if ((int) fmtTracks.getShort("status", tri) == 0) continue;
                    double chi2 = (double) fmtTracks.getFloat("chi2", tri);
                    dgFMT[0].getH1F("chi2").fill(chi2);
                }
            }
            else if (var == 9) {
                // Plot z from DC track bank.
                DataBank dcTracks  = Data.getBank(event, "TimeBasedTrkg::TBTracks");
                DataBank recTracks = Data.getBank(event, "REC::Track");
                DataBank particles = Data.getBank(event, "REC::Particle");
                if (dcTracks == null || recTracks == null || particles == null) continue;

                for (int tri = 0; tri < dcTracks.rows(); ++tri) {
                    // Filter by DC sector.
                    if ((int) dcTracks.getByte("sector", tri) != 1) continue;

                    // Filter tracks by pid.
                    int pid = -1;
                    for (int rtri = 0; rtri < recTracks.rows(); ++rtri) {
                        if (tri != recTracks.getShort("index", rtri)) continue;
                        int pindex = (int) recTracks.getShort("pindex", rtri);
                        pid = particles.getInt("pid", pindex);
                        break;
                    }
                    if (pid != 11) continue;
//                    if (pid != 211 && pid != -211) continue;

                    dgFMT[0].getH1F("DC tracks").fill((double) dcTracks.getFloat("Vtx0_z", tri));
                }
            }

            else {
                System.err.printf("[ResolutionAnalysis] var should be between 0 and 9!\n");
                System.exit(1);
            }
        }

        System.out.format("Analyzed %8d events... Done!\n", ei);
        reader.close();

        if ((var==0 || var==1 || var==4 || var==7 || var==8) && drawPlots)
            Data.drawPlots(dgFMT, title);
        if (var == 2) {
            // Apply z shifts and draw plots.
            for (int li = 0; li< Constants.FMTLAYERS; ++li)
                fmtZ[li] += shArr[0][0] + shArr[li+1][0];
            if (drawPlots) Data.drawZPlots(dgFMT, title, fmtZ);
        }
        if (var == 3) {
            // Fit gaussian and draw plots.
            for (int i = 0; i < 2; ++i)
                Data.fitRes(dgFMT[0].getH1F("delta_tmin" + i), dgFMT[0].getF1D("f" + i), 40);
            if (drawPlots) Data.drawPlots(dgFMT, title);
        }
        if (var == 5 && drawPlots) {
            // Fit gaussians and draw plots.
            Data.fitZ(dgFMT[0].getH1F("DCTB tracks"), dgFMT[0].getF1D("twinpeaks0"), -36, -30);
            Data.fitZ(dgFMT[0].getH1F( "FMT tracks"), dgFMT[0].getF1D("twinpeaks1"), -36, -30);
            Data.drawPlots(dgFMT, title);
        }
        if (var == 6 && drawPlots) {
            // Fit gaussian and draw plots.
            Data.fitUpstream(dgFMT[0].getH1F("FMT tracks"),dgFMT[0].getF1D("upstream fit"),-36,-30);
            Data.drawPlots(dgFMT, title);
        }
        if (var == 9 && drawPlots) {
            // Fit gaussian and draw plots.
            Data.fitUpstream(dgFMT[0].getH1F("DC tracks"), dgFMT[0].getF1D("upstream fit"),
                    -36, -30);
            Data.fitDownstream(dgFMT[0].getH1F("DC tracks"), dgFMT[0].getF1D("downstream fit"),
                    20, 26);
            Data.drawPlots(dgFMT, title);
        }

        return 0;
    }

    /**
     * Draw a 2D plot by counting a pre-defined variable against another.
     * @param var Variable pair to be counted:
     *              * 0 : energy / cluster size vs cluster size.
     *              * 1 : residual vs delta Tmin.
     * @param r   Currently unused, kept for consistency.
     * @return status int.
     */
    public int plot2DCount(int var, int r) {
        // Sanitize input.
        if (var < 0 || var > 3) return 0;

        String title = null;
        if (var == 0) title = "energy / cluster size count";
        if (var == 1) title = "residual vs delta Tmin";
        if (var == 2) title = "vertex z vs track theta";
        if (var == 3) title = "vertex z vs track phi";

        DataGroup[] dgFMT = null;
        if (var == 0 || var == 1)
            dgFMT = Data.create2DDataGroup(var, Constants.FMTLAYERS, r);
        if (var == 2)
            dgFMT = Data.create2DDataGroup(var, 1, r);
        if (var == 3)
            dgFMT = Data.create2DDataGroup(var, 2, r);

        // Run.
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

            if (var == 0) {
                // Get relevant data banks.
                DataBank clusters = Data.getBank(event, "FMTRec::Clusters");
                DataBank traj     = Data.getBank(event, "REC::Traj");
                if (clusters == null || traj == null) continue;

                for (int ri=0; ri<clusters.rows(); ++ri) {
                    int li        = clusters.getByte("layer", ri);
                    double energy = clusters.getFloat("ETot", ri);
                    int size      = clusters.getShort("size", ri);

                    if (var == 0) dgFMT[0].getH2F("hi_cluster_var"+li).fill(size, energy/size);
                }
            }

            if (var == 2) {
                // DataBank tracks  = Data.getBank(event, "TimeBasedTrkg::TBTracks");
                DataBank tracks = Data.getBank(event, "FMT::Tracks");

                if (tracks == null) continue;

                for (int tri = 0; tri < tracks.rows(); ++tri) {
                    if (tracks.getByte("status", tri) == 0) continue;
                    double vz = (double) tracks.getFloat("Vtx0_z", tri);

                    double px = (double) tracks.getFloat("p0_x", tri);
                    double py = (double) tracks.getFloat("p0_y", tri);
                    double pz = (double) tracks.getFloat("p0_z", tri);
                    double th = Math.toDegrees(Math.acos(pz/Math.sqrt(px*px+py*py+pz*pz)));

                    dgFMT[0].getH2F("theta_vs_vz").fill(vz, th);
                }
            }

            if (var == 3) {
                // TODO: Do the same for TimeBasedTrkg::TBTracks to compare

                // DataBank tracks = Data.getBank(event, "TimeBasedTrkg::TBTracks");
                DataBank tracks = Data.getBank(event, "FMTRec::Tracks");
                if (tracks == null) continue;
                for (int ti = 0; ti < tracks.rows(); ++ti) {
                    if (tracks.getShort("status", ti) == 0) continue;
                    double q  = (int) tracks.getByte("q", ti);
                    double vz = (double) tracks.getFloat("Vtx0_z", ti);
                    double px = (double) tracks.getFloat("p0_x", ti);
                    double py = (double) tracks.getFloat("p0_y", ti);
                    double phi = Math.toDegrees(Math.atan2(py, px));

                    if (q > 0) dgFMT[0].getH2F("phi_vs_vz_positive").fill(vz, phi);
                    if (q < 0) dgFMT[0].getH2F("phi_vs_vz_negative").fill(vz, phi);
                }
            }
        }
        System.out.format("Analyzed %8d events... Done!\n", ei);
        reader.close();

        if (drawPlots) Data.drawPlots(dgFMT, title);

        return 0;
    }
}
