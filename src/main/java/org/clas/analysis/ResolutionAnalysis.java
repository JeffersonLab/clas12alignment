package org.clas.analysis;

import org.jlab.detector.base.DetectorType;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.groot.group.DataGroup;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.*;
import org.jlab.groot.math.F1D;

public class ResolutionAnalysis {
    // Constants:
    final int ln = 3; // Number of FMT layers.
    final int rn = 4; // Number of FMT regions.
    final int sn = 6; // Number of DC sectors.
    // FMT region separators:
    final int[] iStripArr = new int[]{-1, 319, 511, 831, 1023};

    // Class variables:
    String infile;
    boolean[] pltLArr;
    boolean debugInfo;
    boolean testRun;
    double[] fmtZ;     // z position of the layers in cm (before shifting).
    double[] fmtAngle; // strip angle in deg.
    double[][] shArr;  // 2D array of shifts to be applied.

    /**
     * Class constructor.
     * @param infile   : Input hipo file.
     * @param pltLArr  : Boolean array describing what lines should be drawn in
     *                   each plot:
     *                   * [0] : Top plots, vertical line at 0.
     *                   * [1] : Bottom plots, vertical line at 0.
     *                   * [2] : Bottom plots, horizontal lines at each cable's
     *                           endpoint.
     *                   * [3] : Bottom plots, horizonal lines separating each
     *                           "region" of the FMT.
     * @param dbgInfo  : Boolean describing if debugging info should be printed.
     * @param testRun  : Boolean describing if the run should be cut short for
     *                   expedient testing.
     * @param shiftArr : Array of arrays describing all the shifts applied:
     *                   [0]:  global [z,x,y,phi] shift.
     *                   [1]: layer 1 [z,x,y,phi] shift.
     *                   [2]: layer 2 [z,x,y,phi] shift.
     *                   [3]: layer 3 [z,x,y,phi] shift.
     */
    public ResolutionAnalysis(String infile, boolean[] pltLArr,
            boolean dbgInfo, boolean testRun, double[][] shiftArr) {

        // Sanitize input.
        if (pltLArr.length != 4) {
            System.out.printf("pltLArr should have a size of 4. Read the ");
            System.out.printf("method's description.\n");
            System.exit(1);
        }
        if (shiftArr.length != 4) {
            System.out.printf("shiftArr should have a size of 4!\n");
            System.exit(1);
        }
        if (shiftArr[0].length != 4) {
            System.out.printf("each array inside shiftArr should have a size ");
            System.out.printf("of 4!\n");
        }

        this.infile    = infile;
        this.pltLArr   = pltLArr;
        this.debugInfo = dbgInfo;
        this.testRun   = testRun;
        this.shArr     = shiftArr;

        // Set geometry parameters by reading from database.
        DatabaseConstantProvider dbProvider =
                new DatabaseConstantProvider(10, "rgf_spring2020");
        String fmtTable = "/geometry/fmt/fmt_layer_noshim";
        dbProvider.loadTable(fmtTable);

        fmtZ     = new double[ln]; // z position of the layers in cm.
        fmtAngle = new double[ln]; // strip angle in deg.

        for (int li=0; li<ln; li++) {
            fmtZ[li]     = dbProvider.getDouble(fmtTable+"/Z",li)/10;
            fmtAngle[li] = dbProvider.getDouble(fmtTable+"/Angle",li);
        }
    }

    /** Execute the runAnalysis method below with less input. */
    private int runAnalysis(int type, TrkSwim swim, FiducialCuts fcuts,
            DataGroup[] dgFMT) {
        return runAnalysis(type, -1, swim, fcuts, dgFMT, -1);
    }

    /**
     * Generic function for running analysis, called by all others.
     * @param type   : Type of analysis to be ran:
     *                   * 0 : perturbatory shifts.
     *                   * 1 : dc sector vs strip.
     *                   * 2 : dc sector vs theta.
     *                   * 3 : fmt regions plot.
     * @param opt    : Variable used in different manners by different types:
     *                   * type=0 : canvass index (ci).
     *                   * type=1 : number of DC sectors (sn).
     *                   * type=2 : number of DC sectors (sn).
     *                   * type=3 : unused.
     * @param swim   : TrkSwim class instance.
     * @param fcuts  : FiducialCuts class instance.
     * @param dgFMT  : Array of data groups where analysis data is stored.
     * @param g      : Range for the gaussian fit.
     * @return status int.
     */
    private int runAnalysis(int type, int opt, TrkSwim swim, FiducialCuts fcuts,
            DataGroup[] dgFMT, int g) {
        // Sanitize input.
        if (type < 0 || type > 3) return 1;

        // Print the shifts applied:
        if (debugInfo) {
            System.out.printf("SHIFTS APPLIED:\n");
            for (int li=1; li<=ln; ++li) {
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
            if (testRun && ei == 10000) break;
            if (ei%50000==0) System.out.format("Analyzed %8d events...\n", ei);
            DataEvent event = reader.getNextEvent();
            ei++;

            // Get relevant data banks.
            DataBank clusters = Data.getBank(event, "FMTRec::Clusters");
            DataBank traj     = Data.getBank(event, "REC::Traj");
            DataBank particle = Data.getBank(event, "REC::Particle");
            DataBank track    = Data.getBank(event, "REC::Track");
            if (clusters==null || traj==null || particle==null || track==null)
                continue;

            // Loop through trajectory points.
            for (int tri=0; tri<traj.rows(); tri++) {
                // Load trajectory variables.
                int detector = traj.getByte("detector", tri);
                int li = traj.getByte("layer", tri);
                int pi = traj.getShort("pindex", tri);
                int si = -1; // DC sector.
                double costh = -1; // track theta.

                // Use only FMT layers 1, 2, and 3.
                if (detector!=DetectorType.FMT.getDetectorId() || li<1 || li>ln)
                    continue;

                fcuts.increaseTrackCount();

                // Get DC sector of the track.
                for (int row = 0; row<track.rows(); ++row) {
                    if (track.getShort("pindex", row) == pi) {
                        si = track.getByte("sector", row)-1;
                    }
                }

                // Get FMT layer's z coordinate and phi angle.
                double zRef = fmtZ[li-1] + shArr[0][0] + shArr[li][0];
                double phiRef = fmtAngle[li-1];

                // Get particle data.
                double x  = (double) particle.getFloat("vx", pi);
                double y  = (double) particle.getFloat("vy", pi);
                double z  = (double) particle.getFloat("vz", pi);
                double px = (double) particle.getFloat("px", pi);
                double py = (double) particle.getFloat("py", pi);
                double pz = (double) particle.getFloat("pz", pi);
                int q     = (int)    particle.getByte("charge", pi);

                if (fcuts.downstreamTrackCheck(z, zRef)) continue;
                double[] V = swim.swimToPlane(x,y,z,px,py,pz,q,zRef);

                x  = V[0];
                y  = V[1];
                z  = V[2];
                px = V[3];
                py = V[4];
                pz = V[5];

                // Get the track's theta angle.
                costh = Math.acos(pz/Math.sqrt(px*px+py*py+pz*pz));

                // Apply track fiducial cuts.
                if (fcuts.checkTrackCuts(z, x, y, zRef, costh)) continue;

                // Rotate (x,y) to local coordinates.
                double xLoc = x * Math.cos(Math.toRadians(phiRef))
                        + y * Math.sin(Math.toRadians(phiRef));
                double yLoc = y * Math.cos(Math.toRadians(phiRef))
                        - x * Math.sin(Math.toRadians(phiRef));

                // Loop over the clusters and calculate residuals for every
                // track-cluster combination.
                for (int cri=0; cri<clusters.rows(); cri++) {
                    // Check that the cluster and trajectory layers match.
                    if (li!=clusters.getByte("layer", cri)) continue;
                    fcuts.increaseClusterCount();
                    int strip     = clusters.getInt("seedStrip", cri);
                    int size      = clusters.getShort("size", cri);
                    double energy = clusters.getFloat("ETot", cri);
                    double tmin   = clusters.getFloat("Tmin", cri);
                    double yclus  = clusters.getFloat("centroid", cri);

                    // Apply cluster fiducial cuts.
                    if (fcuts.checkClusterCuts(strip, size, energy, tmin))
                        continue;

                    // Update plots depending on type of analysis.
                    int plti = -1;
                    if (type == 0) plti = opt;
                    if (type == 1 || type == 2) plti = si;
                    if (type == 3) {
                        for (int ri=0; ri<=rn; ++ri) {
                            if (iStripArr[ri]+1 <= strip
                                    && strip <= iStripArr[ri+1]) {
                                dgFMT[0].getH2F(
                                        "hi_cluster_res_strip_l"+(rn*(li-1)+ri)
                                        ).fill(yLoc-yclus, strip);
                            }
                        }
                        continue;
                    }

                    dgFMT[plti].getH1F("hi_cluster_res_l"+li)
                            .fill(yLoc - yclus);
                    if (type == 0 || type == 1)
                        dgFMT[plti].getH2F("hi_cluster_res_strip_l"+li)
                                .fill(yLoc - yclus, strip);
                    if (type == 2)
                        dgFMT[plti].getH2F("hi_cluster_res_theta_l"+li)
                                .fill(yLoc - yclus, costh);
                }
            }
        }
        System.out.format("Analyzed %8d events... Done!\n", ei);
        reader.close();

        if (debugInfo) fcuts.printCutsInfo();

        // Fit residual plots
        if (type == 0) {
            for (int li=1; li<=ln; ++li) {
                Data.fitRes(dgFMT[opt].getH1F("hi_cluster_res_l"+li),
                        dgFMT[opt].getF1D("f1_res_l"+li), g);
            }
        }
        else if (type == 1 || type == 2) {
            // Fit residual plots
            for (int si=0; si<opt; ++si) {
                for (int li=1; li<=ln; ++li) {
                    Data.fitRes(dgFMT[si].getH1F("hi_cluster_res_l"+li),
                            dgFMT[si].getF1D("f1_res_l"+li), g);
                }
            }
        }

        return 0;
    }

    /**
     * Run analysis and draw a different plot for each FMT region.
     * @param r     : Residuals range in plots.
     * @param swim  : TrkSwim class instance.
     * @param fcuts : FiducialCuts class instance.
     * @return status int.
     */
    public int fmtRegionAnalysis(int r, TrkSwim swim, FiducialCuts fcuts) {
        int type = 3;
        // Set canvases' stuff.
        String title = "FMT regions";
        DataGroup[] dgFMT = Data.createFMTRegionsDataGroup(ln, rn, iStripArr, r);

        // Run.
        runAnalysis(type, swim, fcuts, dgFMT);
        if (debugInfo) fcuts.printCutsInfo();
        Data.drawPlots(dgFMT, title);

        return 0;
    }

    /**
     * Run perturbatory shifts analysis. Currently, only z shifts are
     * implemented.
     * @param lyr     : Layer to which shifts are applied (0 for global shift).
     * @param var     : Variable in layer to which shifts are applied:
     *                    * var=0 : z
     *                    * var=1 : x
     *                    * var=2 : y
     *                    * var=3 : phi
     * @param inShArr : Array of shifts to try.
     * @param r       : Plot range.
     * @param g       : Gaussian range.
     * @param swim    : TrkSwim class instance.
     * @param fcuts   : FiducialCuts class instance.
     * @return status int.
     */
    public int shiftAnalysis(int lyr, int var, double[] inShArr, int r, int g,
            TrkSwim swim, FiducialCuts fcuts) {

        int type = 0;
        // Setup.
        int cn = inShArr.length;
        String[] titleArr = new String[cn];
        for (int ci=0; ci < cn; ++ci)
            titleArr[ci] = "shift ["+lyr+","+var+"] : "+inShArr[ci];

        DataGroup[] dgFMT = Data.createResDataGroups(type, ln, cn, r);

        double[][] meanArr     = new double[ln][cn];
        double[][] meanErrArr  = new double[ln][cn];
        double[][] sigmaArr    = new double[ln][cn];
        double[][] sigmaErrArr = new double[ln][cn];

        // Run.
        double origVal = shArr[lyr][var];
        for (int ci=0; ci<cn; ++ci) {
            shArr[lyr][var] = origVal + inShArr[ci];
            runAnalysis(type, ci, swim, fcuts, dgFMT, g);

            // Get mean and sigma from fit.
            for (int li=0; li<ln; ++li) {
                F1D func = dgFMT[ci].getF1D("f1_res_l"+(li+1));
                meanArr[li][ci]     = func.getParameter(1);
                meanErrArr[li][ci]  = func.parameter(1).error();
                sigmaArr[li][ci]    = func.getParameter(2);
                sigmaErrArr[li][ci] = func.parameter(2).error();
            }
        }

        // Print alignment data and draw plots.
        for (int li=0; li<ln; ++li) {
            System.out.printf("\nLayer %1d:\n", li+1);
            for (int ci=0; ci<cn; ++ci) {
                System.out.printf("shift [%1d,%1d] : %5.2f\n", lyr, var, inShArr[ci]);
                System.out.printf("  * mean  : %9.6f +- %9.6f\n",
                        meanArr[li][ci], meanErrArr[li][ci]);
                System.out.printf("  * sigma : %9.6f +- %9.6f\n",
                        sigmaArr[li][ci], sigmaErrArr[li][ci]);

            }
        }

        Data.drawResPlots(dgFMT, cn, titleArr, pltLArr);

        return 0;
    }

    /**
     * Run DC sector strip analysis.
     * @param r    : Plot range.
     * @param g    : Gaussian range.
     * @param swim : TrkSwim class instance.
     * @param fcuts : FiducialCuts class instance.
     * @return status int.
     */
    public int dcSectorStripAnalysis(int r, int g, TrkSwim swim,
            FiducialCuts fcuts) {

        int type = 1;
        String[] titleArr = new String[sn];
        for (int si=0; si < sn; ++si) titleArr[si] = "DC sector "+(si+1);

        // Run.
        DataGroup[] dgFMT = Data.createResDataGroups(type, ln, sn, r);
        runAnalysis(type, sn, swim, fcuts, dgFMT, g);
        Data.drawResPlots(dgFMT, sn, titleArr, pltLArr);

        return 0;
    }

    /**
     * Run DC sector theta analysis.
     * @param r    : Plot range.
     * @param g    : Gaussian range.
     * @param swim : TrkSwim class instance.
     * @param fcuts : FiducialCuts class instance.
     * @return status int.
     */
    public int dcSectorThetaAnalysis(int r, int g, TrkSwim swim,
            FiducialCuts fcuts) {

        int type = 2;
        String[] titleArr = new String[sn];
        for (int si=0; si < sn; ++si) titleArr[si] = "DC sector " + (si+1);

        // Run.
        DataGroup[] dgFMT = Data.createResDataGroups(type, ln, sn, r);
        runAnalysis(type, sn, swim, fcuts, dgFMT, g);
        Data.drawResPlots(dgFMT, sn, titleArr, pltLArr);

        return 0;
    }

    /**
     * Draw a 1D plot by counting a pre-defined variable.
     * @param var : Variable to be counted:
     *                * 0 : clusters' Tmin.
     *                * 1 : clusters' energy.
     * @param r   : Range for the plot (min = 0, max = r).
     * @return status int.
     */
    public int plot1DCount(int var, int r) {
        // Sanitize input.
        if (var < 0 || var > 1) return 0;

        String title = null;
        if (var == 0) title = "Tmin count";
        if (var == 1) title = "energy count";

        DataGroup[] dgFMT = Data.create1DDataGroup(var, ln, r);

        // Run.
        int ei = 0; // Event number.
        HipoDataSource reader = new HipoDataSource();
        reader.open(infile);
        System.out.printf("\nRunning analysis...\n");

        // Loop through events.
        while (reader.hasEvent()) {
            if (ei == 10000 && testRun) break;
            if (ei%50000==0) System.out.format("Analyzed %8d events...\n", ei);
            DataEvent event = reader.getNextEvent();
            ei++;

            // Get relevant data banks.
            DataBank clusters = Data.getBank(event, "FMTRec::Clusters");
            DataBank traj     = Data.getBank(event, "REC::Traj");
            if (clusters==null || traj==null) continue;

            for (int ri=0; ri<clusters.rows(); ++ri) {
                int li        = clusters.getByte("layer", ri);
                double energy = clusters.getFloat("ETot", ri);
                double tmin   = clusters.getFloat("Tmin", ri);

                if (var==0) dgFMT[0].getH1F("hi_cluster_var"+li).fill(tmin);
                if (var==1) dgFMT[0].getH1F("hi_cluster_var"+li).fill(energy);
            }
        }
        System.out.format("Analyzed %8d events... Done!\n", ei);
        reader.close();

        Data.drawPlots(dgFMT, title);

        return 0;
    }

    /**
     * Draw a 2D plot by counting a pre-defined variable against another.
     * @param var : Variable pair to be counted:
     *                * 0 : energy / cluster size vs cluster size.
     * @param r   : Currently unused, kept for consistency.
     * @return status int.
     */
    public int plot2DCount(int var, int r) {
        // Sanitize input.
        if (var != 0) return 0;

        String title = null;
        if (var == 0) title = "energy / cluster size count";

        DataGroup[] dgFMT = Data.create2DDataGroup(var, ln, r);

        // Run.
        int ei = 0; // Event number.
        HipoDataSource reader = new HipoDataSource();
        reader.open(infile);
        System.out.printf("\nRunning analysis...\n");

        // Loop through events.
        while (reader.hasEvent()) {
            if (ei == 10000 && testRun) break;
            if (ei%50000==0) System.out.format("Analyzed %8d events...\n", ei);
            DataEvent event = reader.getNextEvent();
            ei++;

            // Get relevant data banks.
            DataBank clusters = Data.getBank(event, "FMTRec::Clusters");
            DataBank traj     = Data.getBank(event, "REC::Traj");
            if (clusters==null || traj==null) continue;

            for (int ri=0; ri<clusters.rows(); ++ri) {
                int li        = clusters.getByte("layer", ri);
                double energy = clusters.getFloat("ETot", ri);
                int size      = clusters.getShort("size", ri);

                if (var==0) dgFMT[0].getH2F("hi_cluster_var"+li)
                                    .fill(size, energy/size);
            }
        }
        System.out.format("Analyzed %8d events... Done!\n", ei);
        reader.close();

        Data.drawPlots(dgFMT, title);

        return 0;
    }
}
