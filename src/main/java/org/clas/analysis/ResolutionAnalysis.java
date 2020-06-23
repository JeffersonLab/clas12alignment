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
import org.clas.cross.Cross;
import org.clas.cross.TrajPoint;

public class ResolutionAnalysis {
    // Class variables:
    private String infile;
    private boolean[] pltLArr;
    private boolean debugInfo;
    private boolean testRun;
    private double[] fmtZ;       // z position of the layers in cm (before shifting).
    private double[] fmtAngle;   // strip angle in deg degrees.
    private double[][] shArr;    // 2D array of shifts to be applied.
    private boolean makeCrosses; // Boolean describing if we should do crossmaking.
    private boolean drawPlots;   // Boolean describing if plots are to be drawn.
    private boolean ypAlign;     // Special setup needed for yaw & pitch alignment.

    /**
     * Class constructor.
     * @param infile      Input hipo file.
     * @param pltLArr     Boolean array describing what lines should be drawn in each plot:
     *                      * [0] : Top plots, vertical line at 0.
     *                      * [1] : Bottom plots, vertical line at 0.
     *                      * [2] : Bottom plots, horizontal lines at each cable's endpoint.
     *                      * [3] : Bottom plots, horizonal lines separating each FMT "region".
     * @param dbgInfo     Boolean describing if debugging info should be printed.
     * @param testRun     Boolean describing if the run should be cut short for expedient testing.
     * @param shArr       Array of arrays describing all the shifts applied:
     *                      * [0] :  global [z,x,y,phi] shift.
     *                      * [1] : layer 1 [z,x,y,phi] shift.
     *                      * [2] : layer 2 [z,x,y,phi] shift.
     *                      * [3] : layer 3 [z,x,y,phi] shift.
     * @param makeCrosses Boolean describing if we should do crossmaking.
     * @param drawPlots   Boolean describing if plots are to be drawn.
     * @param ypAlign     Special setup needed for yaw & pitch alignment.
     */
    public ResolutionAnalysis(String infile, boolean[] pltLArr, boolean dbgInfo, boolean testRun,
            double[][] shArr, boolean makeCrosses, boolean drawPlots, boolean ypAlign) {
        this.makeCrosses = makeCrosses;
        this.drawPlots   = drawPlots;
        this.ypAlign     = ypAlign;

        // Sanitize input.
        if (pltLArr.length != 4) {
            System.out.printf("pltLArr should have a size of 4. Read the method's description.\n");
            System.exit(1);
        }
        if (shArr.length != 4) {
            System.out.printf("shArr should have a size of 4!\n");
            System.exit(1);
        }
        if (shArr[0].length != 6) {
            System.out.printf("Each array inside shArr should have a size of 6!\n");
            System.exit(1);
        }

        this.infile    = infile;
        this.pltLArr   = pltLArr;
        this.debugInfo = dbgInfo;
        this.testRun   = testRun;
        this.shArr     = new double[][]{
            {shArr[0][0], shArr[0][1], shArr[0][2], shArr[0][3], shArr[0][4], shArr[0][5]},
            {shArr[1][0], shArr[1][1], shArr[1][2], shArr[1][3], shArr[1][4], shArr[1][5]},
            {shArr[2][0], shArr[2][1], shArr[2][2], shArr[2][3], shArr[2][4], shArr[2][5]},
            {shArr[3][0], shArr[3][1], shArr[3][2], shArr[3][3], shArr[3][4], shArr[3][5]}
        };

        // Set geometry parameters by reading from database.
        DatabaseConstantProvider dbProvider = new DatabaseConstantProvider(10, "rgf_spring2020");
        String fmtTable = "/geometry/fmt/fmt_layer_noshim";
        dbProvider.loadTable(fmtTable);

        fmtZ     = new double[Constants.getNumberOfFMTLayers()]; // z position of the layers in cm.
        fmtAngle = new double[Constants.getNumberOfFMTLayers()]; // strip angle in deg.

        for (int li = 0; li< Constants.getNumberOfFMTLayers(); li++) {
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
        if (debugInfo) {
            System.out.printf("SHIFTS APPLIED:\n");
            for (int li = 1; li<= Constants.getNumberOfFMTLayers(); ++li) {
                System.out.printf("[");
                for (int vi=0; vi<shArr[li].length; ++vi) {
                    System.out.printf("%6.2f,", shArr[0][vi] + shArr[li][vi]);
                    if (vi == shArr[li].length-1) System.out.printf("\b");
                }
                System.out.printf("]\n");
            }
        }

        int ei = 0; // Event number.
        HipoDataSource reader = new HipoDataSource();
        reader.open(infile);
        System.out.printf("\nRunning analysis...\n");

        // Loop through events.
        while (reader.hasEvent()) {
            if (testRun && ei == 50) break;
            if (ei%50000==0) System.out.format("Analyzed %8d events...\n", ei);
            DataEvent event = reader.getNextEvent();
            ei++;

            ArrayList<TrajPoint[]> trajPoints = TrajPoint.getTrajPoints(event, swim,
                    fcuts, fmtZ, fmtAngle, shArr, 3, true);
            ArrayList<Cluster>[] clusters = Cluster.getClusters(event, fcuts, true);
            if (trajPoints==null || clusters==null) continue;

            if (makeCrosses) {
                ArrayList<Cross> crosses = Cross.makeCrosses(trajPoints, clusters, fcuts);

                // Loop through crosses.
                for (Cross cross : crosses) {
                    // Loop through trajectory points, clusters, and residuals in the cross.
                    for (int ci=0; ci<cross.size(); ++ci) {
                        // Get necessary data
                        int li       = cross.getc(ci).get_fmtLyr();
                        int si       = cross.gett(ci).get_dcSec();
                        int strip    = cross.getc(ci).get_strip();
                        double costh = cross.gett(ci).get_cosTh();

                        // Setup plots
                        int plti = -1;
                        if (func == 0) plti = opt;
                        if (func == 1 || func == 2) plti = si;
                        if (func == 4) plti = 0;

                        // Plot per FMT-region residuals.
                        if (func==3) {
                            for (int ri = 0; ri<= Constants.getNumberOfFMTRegions(); ++ri) {
                                if (Constants.getFMTRegionSeparators(ri) + 1 <= strip
                                        && strip<= Constants.getFMTRegionSeparators(ri + 1)) {
                                    dgFMT[0].getH2F("hi_cluster_res_strip_l" +
                                            (Constants.getNumberOfFMTRegions() * li + ri))
                                            .fill(cross.getr(ci), strip);
                                }
                            }
                            continue;
                        }

                        // Plot other types of analysis.
                        dgFMT[plti].getH1F("hi_cluster_res_l"+(li+1)).fill(cross.getr(ci));
                        if (func==0 || func==1)
                            dgFMT[plti].getH2F("hi_cluster_res_strip_l"+(li+1))
                                    .fill(cross.getr(ci), strip);
                        if (func==2)
                            dgFMT[plti].getH2F("hi_cluster_res_theta_l"+(li+1))
                                    .fill(cross.getr(ci), costh);
                        if (func==4) {
                            dgFMT[plti].getH2F("hi_cluster_res_dtmin_l"+(li+1))
                                    .fill(cross.getr(ci),
                                    Math.abs(cross.getc(li).get_tMin() -
                                            cross.getc((li+1)%3).get_tMin()));
                        }
                    }
                }
            }
            else {
                // Loop through trajectory points
                for (TrajPoint[] tpArr : trajPoints) {
                    for (int li=0; li<tpArr.length; ++li) {
                        for (Cluster c : clusters[li]) {
                            int si       = tpArr[li].get_dcSec();
                            int strip    = c.get_strip();
                            double costh = tpArr[li].get_cosTh();
                            double res   = tpArr[li].get_y() - c.get_y();

                            // Setup plots
                            int plti = -1;
                            if (func == 0) plti = opt;
                            if (func == 1 || func == 2) plti = si;
                            if (func == 4) plti = 0;

                            // Plot per FMT-region residuals
                            if (func == 3) {
                                for (int ri = 0; ri<= Constants.getNumberOfFMTRegions(); ++ri) {
                                    if (Constants.getFMTRegionSeparators(ri)+1<=strip
                                            && strip<= Constants.getFMTRegionSeparators(ri + 1)) {
                                        dgFMT[0].getH2F("hi_cluster_res_strip_l" +
                                                (Constants.getNumberOfFMTRegions() * li + ri))
                                                .fill(res, strip);
                                    }
                                }
                                continue;
                            }

                            // Plot other types of analysis
                            double theta_inv = 1/Math.toDegrees(Math.acos(costh));
                            if (ypAlign) {
                                if (!Double.isFinite(theta_inv)) continue;
                                dgFMT[plti].getH1F("hi_cluster_res_l"+(li+1)).fill(res, theta_inv);
                            }
                            else {
                                dgFMT[plti].getH1F("hi_cluster_res_l"+(li+1)).fill(res);
                            }
                            if (func==0 || func==1)
                                dgFMT[plti].getH2F("hi_cluster_res_strip_l"+(li+1))
                                        .fill(res, strip);
                            if (func==2)
                                dgFMT[plti].getH2F("hi_cluster_res_theta_l"+(li+1))
                                        .fill(res, costh);
                            if (func==4) {
                                System.out.printf("Set makeCrosses to true to get this plot!\n");
                                System.exit(1);
                            }
                        }
                    }
                }
            }
        }
        System.out.format("Analyzed %8d events... Done!\n", ei);
        reader.close();

        if (debugInfo) fcuts.printCutsInfo();

        // Fit residual plots
        if (func == 0) {
            for (int li = 1; li<= Constants.getNumberOfFMTLayers(); ++li) {
                Data.fitRes(dgFMT[opt].getH1F("hi_cluster_res_l"+li),
                        dgFMT[opt].getF1D("f1_res_l"+li), g);
            }
        }
        else if (func == 1 || func == 2) {
            for (int si=0; si<opt; ++si) {
                for (int li = 1; li<= Constants.getNumberOfFMTLayers(); ++li) {
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

        DataGroup[] dgFMT = Data.createResDataGroups(func, Constants.getNumberOfFMTLayers(), cn, r);

        double[][] meanArr     = new double[Constants.getNumberOfFMTLayers()][cn];
        double[][] sigmaArr    = new double[Constants.getNumberOfFMTLayers()][cn];
        double[][] sigmaErrArr = new double[Constants.getNumberOfFMTLayers()][cn];
        double[][] chiSqArr    = new double[Constants.getNumberOfFMTLayers()][cn];

        // Run.
        double origVal = shArr[lyr][var];
        for (int ci=0; ci<cn; ++ci) {
            shArr[lyr][var] = origVal + inShArr[ci];
            fcuts.resetCounters();
            TrkSwim swim = new TrkSwim(swmSetup, shArr[0][4], shArr[0][5]);
            runAnalysis(func, ci, swim, fcuts, dgFMT, g);

            // Get mean and sigma from fit.
            for (int li = 0; li< Constants.getNumberOfFMTLayers(); ++li) {
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
        System.out.printf("shift = [");
        for (int ci=0; ci<cn; ++ci) System.out.printf("%9.5f, ", inShArr[ci]);
        System.out.printf("\b\b]\n");
        for (int li = 0; li< Constants.getNumberOfFMTLayers(); ++li) {
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
        if (drawPlots) Data.drawResPlots(dgFMT, cn, titleArr, pltLArr);

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
        String[] titleArr = new String[Constants.getNumberOfDCSectors()];
        for (int si = 0; si < Constants.getNumberOfDCSectors(); ++si)
            titleArr[si] = "DC sector " + (si + 1);

        // Run.
        DataGroup[] dgFMT = Data.createResDataGroups(
                func, Constants.getNumberOfFMTLayers(), Constants.getNumberOfDCSectors(), r
        );
        runAnalysis(func, Constants.getNumberOfDCSectors(), swim, fcuts, dgFMT, g);
        if (drawPlots)
            Data.drawResPlots(dgFMT, Constants.getNumberOfDCSectors(), titleArr, pltLArr);

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
        String[] titleArr = new String[Constants.getNumberOfDCSectors()];
        for (int si = 0; si < Constants.getNumberOfDCSectors(); ++si)
            titleArr[si] = "DC sector " + (si+1);

        // Run.
        DataGroup[] dgFMT = Data.createResDataGroups(
                func, Constants.getNumberOfFMTLayers(), Constants.getNumberOfDCSectors(), r
        );
        runAnalysis(func, Constants.getNumberOfDCSectors(), swim, fcuts, dgFMT, g);
        if (drawPlots)
            Data.drawResPlots(dgFMT, Constants.getNumberOfDCSectors(), titleArr, pltLArr);

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
        if (debugInfo) fcuts.printCutsInfo();
        if (drawPlots) Data.drawPlots(dgFMT, title);

        return 0;
    }

    public int deltaTminAnalysis(int r, TrkSwim swim, FiducialCuts fcuts) {
        int func = 4;
        String title = "delta Tmin";
        DataGroup[] dgFMT = Data.createResDataGroups(func, Constants.getNumberOfFMTLayers(), 1, r);

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
     * @param r   Range for the plot.
     * @return status int.
     */
    public int plot1DCount(int var, TrkSwim swim, FiducialCuts fcuts, int r) {
        // Sanitize input.
        if (var < 0 || var > 5) return 0;

        String title = null;
        if (var == 0) title = "Tmin count";
        if (var == 1) title = "energy count";
        if (var == 2) title = "track z";
        if (var == 3) title = "delta Tmin";
        if (var == 4) title = "track theta";

        DataGroup[] dgFMT = Data.create1DDataGroup(var, Constants.getNumberOfFMTLayers(), r);

        // Run.
        int ei = 0; // Event number.
        HipoDataSource reader = new HipoDataSource();
        reader.open(infile);
        System.out.printf("\nRunning analysis...\n");

        // Loop through events.
        while (reader.hasEvent()) {
            if (ei == 5 && testRun) break;
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
                DataBank traj = Data.getBank(event, "REC::Traj");
                DataBank part = Data.getBank(event, "REC::Particle");
                if (traj == null || part == null) continue;
                for (int tri = 0; tri < traj.rows(); ++tri) {
                    int detector = traj.getByte("detector", tri);
                    int li = traj.getByte("layer", tri);
                    int pi = traj.getShort("pindex", tri);
                    // Use only FMT layers 1, 2, and 3.
                    if (detector!=DetectorType.FMT.getDetectorId() || li < 1 ||
                            li > Constants.getNumberOfFMTLayers())
                        continue;

                    // Get particle data.
                    double z  = (double) part.getFloat("vz", pi);
                    dgFMT[0].getH1F("tracks").fill(z);
                }
            }
            else if (var == 3) {
                ArrayList<TrajPoint[]> trajPoints =
                        TrajPoint.getTrajPoints(event, swim, fcuts, fmtZ, fmtAngle, shArr, 3, true);
                ArrayList<Cluster>[] clusters = Cluster.getClusters(event, fcuts, true);
                if (trajPoints == null || clusters == null) continue;

                ArrayList<Cross> crosses = Cross.makeCrosses(trajPoints, clusters, fcuts);
                if (crosses == null) continue;

                for (Cross c : crosses) {
                    dgFMT[0].getH1F("delta_tmin0")
                            .fill(c.getc(1).get_tMin()-c.getc(0).get_tMin());
                    dgFMT[0].getH1F("delta_tmin1")
                            .fill(c.getc(2).get_tMin()-c.getc(1).get_tMin());
                }
            }
            else if (var == 4) {
                ArrayList<TrajPoint[]> trajPoints =
                        TrajPoint.getTrajPoints(event, swim, fcuts, fmtZ, fmtAngle, shArr, 3, false);
                if (trajPoints == null) continue;
                for (TrajPoint[] trjparr : trajPoints) {
                    for (TrajPoint trjp : trjparr) {
                        dgFMT[0].getH1F("tracks").fill(Math.toDegrees(trjp.get_cosTh()));
                    }
                }
            }
            else System.out.printf("[ResolutionAnalysis] var should be between 0 and 4!\n");

        }
        System.out.format("Analyzed %8d events... Done!\n", ei);
        reader.close();

        if ((var == 0 || var == 1 || var == 4) && drawPlots) Data.drawPlots(dgFMT, title);
        if (var == 2) {
            // Apply z shifts and draw plots.
            for (int li = 0; li< Constants.getNumberOfFMTLayers(); ++li)
                fmtZ[li] += shArr[0][0] + shArr[li+1][0];
            if (drawPlots) Data.drawZPlots(dgFMT, title, fmtZ);
        }
        if (var == 3) {
            // Fit gaussian and draw plots.
            for (int i = 0; i < 2; ++i)
                Data.fitRes(dgFMT[0].getH1F("delta_tmin" + i), dgFMT[0].getF1D("f" + i), 40);
            if (drawPlots) Data.drawPlots(dgFMT, title);
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
        if (var < 0 || var > 1) return 0;

        String title = null;
        if (var == 0) title = "energy / cluster size count";
        if (var == 1) title = "residual vs delta Tmin";

        DataGroup[] dgFMT = Data.create2DDataGroup(var, Constants.getNumberOfFMTLayers(), r);

        // Run.
        int ei = 0; // Event number.
        HipoDataSource reader = new HipoDataSource();
        reader.open(infile);
        System.out.printf("\nRunning analysis...\n");

        // Loop through events.
        while (reader.hasEvent()) {
            if (ei == 10000 && testRun) break;
            if (ei % 50000 == 0) System.out.format("Analyzed %8d events...\n", ei);
            DataEvent event = reader.getNextEvent();
            ei++;

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
        System.out.format("Analyzed %8d events... Done!\n", ei);
        reader.close();

        if (drawPlots) Data.drawPlots(dgFMT, title);

        return 0;
    }
}
