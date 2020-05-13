package org.clas.cross;

import java.util.ArrayList;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.clas.analysis.Data;
import org.clas.analysis.FiducialCuts;
import org.clas.analysis.TrkSwim;
import org.jlab.io.hipo.HipoDataSource;

public class Cross {
    private TrajPoint[] trajPoints;
    private Cluster[] clusters;
    private double[] residuals;

    public TrajPoint get_t0() {return trajPoints[0];}
    public TrajPoint get_t1() {return trajPoints[1];}
    public TrajPoint get_t2() {return trajPoints[2];}
    public TrajPoint gett(int ti) {
        if (ti<0 || ti>size()) return null;
        return trajPoints[ti];
    }
    public Cluster get_c0() {return clusters[0];}
    public Cluster get_c1() {return clusters[1];}
    public Cluster get_c2() {return clusters[2];}
    public Cluster getc(int ci) {
        if (ci<0 || ci>size()) return null;
        return clusters[ci];
    }
    public double get_r0() {return residuals[0];}
    public double get_r1() {return residuals[0];}
    public double get_r2() {return residuals[0];}
    public double getr(int ri) {
        if (ri<0 || ri>size()) return -1;
        return residuals[ri];
    }
    public int size() {
        int clusCnt = 3;
        for (int ci=0; ci<3; ++ci) if (clusters[ci]==null) clusCnt--;
        return clusCnt;
    }

    public Cross(Cluster c0, Cluster c1, Cluster c2, TrajPoint t0, TrajPoint t1, TrajPoint t2) {
        clusters = new Cluster[]{c0, c1, c2};
        trajPoints = new TrajPoint[]{t0, t1, t2};
        residuals = new double[]{c0.get_y()-t0.get_y(),c1.get_y()-t1.get_y(),c2.get_y()-t2.get_y()};
    }

    public static ArrayList<Cross> makeCrosses(ArrayList<TrajPoint[]> trajPoints,
            ArrayList<Cluster>[] clusters, FiducialCuts fcuts) {

        ArrayList<Cross> crosses = new ArrayList<Cross>();

        for (TrajPoint[] trjPArr : trajPoints) {
            // Copy arraylists of clusters.
            ArrayList<Cluster>[] cclusters = new ArrayList[]{
                    new ArrayList<Cluster>(clusters[0]),
                    new ArrayList<Cluster>(clusters[1]),
                    new ArrayList<Cluster>(clusters[2])
            };

            // Count how much crosses would be generated if no cut was applied, which is all
            // possible combinations between the available clusters for each FMT layer.
            fcuts.increaseCrossCount(cclusters[0].size()*cclusters[1].size()*cclusters[2].size());

            // Filter out clusters too far from their respective trajectory points.
            // NOTE: Currently only cutting clusters farther than 3cm in the y coordinate.
            //       Ideally we wanna do this for the x coordinate too but it's not straightforward.
            for (TrajPoint trjP : trjPArr) {
                int li = trjP.get_fmtLyr();
                for (int ci = cclusters[li].size()-1; ci >= 0; --ci) {
                    if (fcuts.checkCrossDeltaY(trjP.get_y(), cclusters[li].get(ci).get_y()))
                        cclusters[trjP.get_fmtLyr()].remove(ci);
                }
            }

            // Create crosses where the Tmin difference makes sense.
            // NOTE: Hardcoded for 3 FMT layers.
            for (Cluster c0 : cclusters[0]) {
                for (Cluster c1 : cclusters[1]) {
                    for (Cluster c2 : cclusters[2]) {
                        if (fcuts.checkCrossDeltaTmin(c0.get_tMin(), c1.get_tMin(), c2.get_tMin()))
                            continue;

                        crosses.add(new Cross(c0, c1, c2, trjPArr[0], trjPArr[1], trjPArr[2]));
                    }
                }
            }
        }

        return crosses;
    }

    /** Class tester. */
    public static void main(String[] args) {
        boolean debug = false;

        Constants    constants = new Constants();
        TrkSwim      trkSwim   = new TrkSwim(new double[]{-0.75, -1.0, -3.0});
        FiducialCuts fcuts     = new FiducialCuts();

        // Set geometry parameters by reading from database.
        DatabaseConstantProvider dbProvider = new DatabaseConstantProvider(10, "rgf_spring2020");
        String fmtTable = "/geometry/fmt/fmt_layer_noshim";
        dbProvider.loadTable(fmtTable);

        double[] fmtZ     = new double[constants.ln]; // z position of the layers in cm.
        double[] fmtAngle = new double[constants.ln]; // strip angle in deg.

        for (int li=0; li<constants.ln; li++) {
            fmtZ[li]     = dbProvider.getDouble(fmtTable+"/Z",li)/10;
            fmtAngle[li] = dbProvider.getDouble(fmtTable+"/Angle",li);
        }

        double[][] shArr = new double[][]{
                {-3.65, 0, 0, 0},
                { 0.20, 0, 0, 0},
                { 0.00, 0, 0, 0},
                { 0.05, 0, 0, 0}
        };

        int ei = 0; // Event number.
        HipoDataSource reader = new HipoDataSource();
        reader.open(args[0]);
        System.out.printf("\nRunning...\n");

        // Loop through events.
        int[] crossCnt = new int[]{0, 0};
        while (reader.hasEvent()) {
            if (debug && ei==25) break;
            if (ei%50000==0) System.out.printf("Ran %8d events...\n", ei);
            ei++;
            DataEvent event = reader.getNextEvent();
            crossCnt[1]++;

            ArrayList<TrajPoint[]> trajPoints = TrajPoint.getTrajPoints(event, constants, trkSwim,
                    fcuts, fmtZ, fmtAngle, shArr, 3, true);
            ArrayList<Cluster>[] clusters = Cluster.getClusters(event, fcuts, true);

            if (trajPoints==null || clusters==null) continue;

            ArrayList<Cross> crosses = makeCrosses(trajPoints, clusters, fcuts);
            crossCnt[0] += crosses.size();

            if (!debug) continue;
            if (crosses.size()!=0) System.out.printf("event #%d Crosses:\n", ei);
            for (Cross cross : crosses) {
                for (int ci=0; ci<cross.size(); ++ci) {
                    System.out.printf("cluster %d: %8.2f, residual: %8.2f\n",
                            cross.getc(ci).get_fmtLyr(), cross.getc(ci).get_y(), cross.getr(ci));
                }
            }
        }
        System.out.printf("Ran %8d events... Done!\n", ei);
        System.out.printf("%d crosses found in %d events.\n", crossCnt[0], crossCnt[1]);

        return;
    }
}
