package org.clas.cross;

import java.util.ArrayList;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.clas.analysis.Data;
import org.clas.analysis.FiducialCuts;

public class Cluster {
    // Cluster data:
    private int id;           // Cluster ID.
    private int fmtLyr;       // FMT layer.
    private int strip;        // FMT strip.
    private double y;         // y position in the layer's local coordinate system.
    private double tMin;      // Minimum time information among the cluster's hits.
    private double energy;    // Total energy of the strips in the cluster.
    private double cResidual; // Centroid residual of the cluster.
    private int tID;       // Associated track ID.

    /** Class constructor. */
    private Cluster(int _id, int _fmtLyr, int _strip, double _y, double _tMin, double _energy,
            double _cResidual, int _tID) {
        this.id        = _id;
        this.fmtLyr    = _fmtLyr;
        this.strip     = _strip;
        this.y         = _y;
        this.tMin      = _tMin;
        this.energy    = _energy;
        this.cResidual = _cResidual;
        this.tID       = _tID;
    }

    public int get_id() {return id;}
    public int get_fmtLyr() {return fmtLyr;}
    public int get_strip() {return strip;}
    public double get_y() {return y;}
    public double get_tMin() {return tMin;}
    public double get_energy() {return energy;}
    public double get_cResidual() {return cResidual;}
    public int get_trkID() {return tID;}

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
            int id        = clBank.getShort("ID", cri);
            int li        = clBank.getByte("layer", cri)-1;
            int strip     = clBank.getInt("seedStrip", cri);
            int size      = clBank.getShort("size", cri);
            double energy = clBank.getFloat("ETot", cri);
            // double tMin   = clBank.getFloat("Tmin", cri);
            double tMin   = 100;
            double y      = clBank.getFloat("centroid", cri);
            double cRes   = clBank.getFloat("centroidResidual", cri);
            int tID       = clBank.getShort("trkID", cri);

            // Apply cluster fiducial cuts.
            if (applyCuts && fcuts.checkClusterCuts(strip, size, energy, tMin)) continue;

            clusters[li].add(new Cluster(id, li, strip, y, tMin, energy, cRes, tID));
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
}
