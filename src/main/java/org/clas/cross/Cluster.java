package org.clas.cross;

import java.util.ArrayList;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.clas.analysis.Data;
import org.clas.analysis.FiducialCuts;
import org.jlab.io.hipo.HipoDataSource;

public class Cluster {
    // Cluster data:
    private int fmtLyr;    // FMT layer.
    private int strip;     // FMT strip.
    private double y;      // y position in the layer's local coordinate system.
    private double tMin;   // Minimum time information among the cluster's hits.
    private double energy; // Total energy of the strips in the cluster.

    private Cluster(int _fmtLyr, int _strip, double _y, double _tMin, double _energy) {
        this.fmtLyr   = _fmtLyr;
        this.strip    = _strip;
        this.y        = _y;
        this.tMin     = _tMin;
        this.energy   = _energy;
    }

    public int get_fmtLyr() {return fmtLyr;}
    public int get_strip() {return strip;}
    public double get_y() {return y;}
    public double get_tMin() {return tMin;}
    public double get_energy() {return energy;}

    /**
     * Get clusters from event bank.
     * @param event     Event in question.
     * @param fcuts     FiducialCuts class instance.
     * @param applyCuts Boolean to decide if fiducial cuts should be applied or not.
     * @return an arraylist of clusters for each FMT layer.
     */
    public static ArrayList<Cluster>[] getClusters(DataEvent event, FiducialCuts fcuts,
            boolean applyCuts) {
        // Get data bank.
        DataBank clBank  = Data.getBank(event, "FMTRec::Clusters");
        if (clBank==null) return null;

        // NOTE: Here it's assumed that there are 3 FMT layers. Needs to be fixed if working with
        //       the full detector.
        ArrayList[] clusters = new ArrayList[]{
                new ArrayList<Cluster>(), new ArrayList<Cluster>(), new ArrayList<Cluster>()};

        for (int cri=0; cri<clBank.rows(); cri++) {
            fcuts.increaseClusterCount();
            int li        = clBank.getByte("layer", cri)-1;
            int strip     = clBank.getInt("seedStrip", cri);
            int size      = clBank.getShort("size", cri);
            double energy = clBank.getFloat("ETot", cri);
            double tMin   = clBank.getFloat("Tmin", cri);
            double y      = clBank.getFloat("centroid", cri);

            // Apply cluster fiducial cuts.
            if (applyCuts && fcuts.checkClusterCuts(strip, size, energy, tMin)) continue;

            clusters[li].add(new Cluster(li, strip, y, tMin, energy));
        }

        return clusters;
    }

    /** Print cluster's info for debugging. */
    public void printInfo() {
        System.out.printf("Cluster info:\n");
        System.out.printf("  FMT layer    : %d\n", get_fmtLyr());
        System.out.printf("  seed strip   : %d\n", get_strip());
        System.out.printf("  y            : %.2f\n", get_y());
        System.out.printf("  t_min        : %.2f\n", get_tMin());
        System.out.printf("  total energy : %.2f\n", get_energy());
    }


    /** Class tester. */
    public static void main(String[] args) {
        boolean debug = true;
        FiducialCuts fCuts = new FiducialCuts();

        int ei = 0; // Event number.
        HipoDataSource reader = new HipoDataSource();
        reader.open(args[0]);
        if (!debug) System.out.printf("\nReading clusters...\n");

        // Loop through events.
        int[] clusCnt = new int[]{0, 0, 0, 0};
        while (reader.hasEvent()) {
            if (debug && ei==5) break;
            if (!debug && ei%50000==0) System.out.printf("Ran %8d events...\n", ei);
            ei++;
            DataEvent event = reader.getNextEvent();
            clusCnt[3]++;

            ArrayList<Cluster>[] clusters = getClusters(event, fCuts, true);

            if (clusters == null) continue;
            clusCnt[0] += clusters[0].size();
            clusCnt[1] += clusters[1].size();
            clusCnt[2] += clusters[2].size();

            if (!debug) continue;
            System.out.printf("clusters arr:\n");
            int li = 0;
            for (ArrayList<Cluster> clusArr : clusters) {
                System.out.printf("  layer %d:\n", ++li);
                for (Cluster clus : clusArr) {
                    System.out.printf("    [%d %8.2f %8.2f]\n",
                            clus.get_fmtLyr(), clus.get_y(), clus.get_tMin());
                }
                li %= 3;
            }
        }
        if (!debug) System.out.printf("Ran %8d events... Done!\n", ei);

        for (int li=0; li<3; ++li)
            System.out.printf("%9d clusters found in layer %d\n", clusCnt[li], li+1);
        System.out.printf("in a total of %d events.\n", clusCnt[3]);

        return;
    }
}
