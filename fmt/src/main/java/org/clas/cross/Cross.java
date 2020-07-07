package org.clas.cross;

import java.util.ArrayList;
import org.clas.analysis.FiducialCuts;

public class Cross {
    // Cross' data. trajPoints is a set of 3 trajectory points, and clusters is a set of 3 clusters.
    private TrajPoint[] trajPoints;
    private Cluster[] clusters;

    /** Get trajectory point by index. */
    public TrajPoint gett(int ti) {
        if (ti < 0 || ti > size()) return null;
        return trajPoints[ti];
    }

    /** Get cluster by index. */
    public Cluster getc(int ci) {
        if (ci < 0 || ci > size()) return null;
        return clusters[ci];
    }

    /** Get residual between trajectory point and cluster by index.*/
    public double getr(int ri) {
        if (ri < 0 || ri > size()) return Double.POSITIVE_INFINITY;
        return (gett(ri).get_y() - getc(ri).get_y());
    }

    /** Return the size of the cross. Currently can only be 3. */
    public int size() {
        int clusCnt = 3;
        for (int ci = 0; ci < 3; ++ci) if (clusters[ci] == null) clusCnt--;
        return clusCnt;
    }

    /** Class constructor. */
    public Cross(Cluster c0, Cluster c1, Cluster c2, TrajPoint t0, TrajPoint t1, TrajPoint t2) {
        clusters = new Cluster[]{c0, c1, c2};
        trajPoints = new TrajPoint[]{t0, t1, t2};
    }

    /**
     * Make crosses with three clusters and trajectory points.
     * @param trajPoints ArrayList of arrays of 3 trajectory points, one per FMT layer.
     * @param clusters   Array of size 3 of ArrayLists of clusters.
     * @param fcuts      FiducialCuts class instance.
     * @return ArrayList of crosses.
     */
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
            for (TrajPoint trjP : trjPArr) {
                int li = trjP.get_fmtLyr();
                for (int ci = cclusters[li].size()-1; ci >= 0; --ci) {
                    if (fcuts.checkCrossDeltaY(trjP.get_y(), cclusters[li].get(ci).get_y()))
                        cclusters[trjP.get_fmtLyr()].remove(ci);
                }
            }

            // Create crosses where the Tmin difference makes sense.
            // NOTE: Hardcoded for 3 FMT layers. This should change for reconstruction, where a par-
            //       ticle can
            double bar = Double.POSITIVE_INFINITY;         // Best average residual.
            Cluster[] bc = new Cluster[] {null,null,null}; // Best clusters.
            for (Cluster c0 : cclusters[0]) {
                for (Cluster c1 : cclusters[1]) {
                    for (Cluster c2 : cclusters[2]) {
                        if (fcuts.checkCrossDeltaTmin(c0.get_tMin(), c1.get_tMin(), c2.get_tMin()))
                            continue;
                        if (bar > Math.abs(c0.get_y() - trjPArr[0].get_y())
                                + Math.abs(c1.get_y() - trjPArr[1].get_y())
                                + Math.abs(c2.get_y() - trjPArr[2].get_y())) {
                            bc[0] = c0;
                            bc[1] = c1;
                            bc[2] = c2;
                        }
                    }
                }
            }
            if (bc[0]!=null && bc[1]!=null && bc[2]!=null)
                crosses.add(new Cross(bc[0], bc[1], bc[2], trjPArr[0], trjPArr[1], trjPArr[2]));
        }

        return crosses;
    }
}
