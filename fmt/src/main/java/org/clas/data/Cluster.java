package org.clas.data;

import java.util.ArrayList;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.clas.analysis.FiducialCuts;
import org.clas.test.Constants;
import org.clas.test.HipoHandler;

/** Class in charge of the Cluster objects and importing clusters from the corresponding bank. */
public class Cluster {
    private int id;        // Cluster ID.
    private int fmtLyr;    // FMT layer.
    private int strip;     // FMT strip.
    private double y;      // y position in the layer's local coordinate system.
    private double tMin;   // Minimum time information among the cluster's hits.
    private double energy; // Total energy of the strips in the cluster.
    private int tID;       // Associated track ID.

    /** Class constructor. */
    private Cluster(int _id, int _fmtLyr, int _strip, double _y, double _tMin, double _energy,
            int _tID) {
        this.id        = _id;
        this.fmtLyr    = _fmtLyr;
        this.strip     = _strip;
        this.y         = _y;
        this.tMin      = _tMin;
        this.energy    = _energy;
        this.tID       = _tID;
    }

    public int get_id() {return id;}
    public int get_fmtLyr() {return fmtLyr;}
    public int get_strip() {return strip;}
    public double get_y() {return y;}
    public double get_tMin() {return tMin;}
    public double get_energy() {return energy;}
    public int get_trkID() {return tID;}

    /**
     * Get clusters from event bank.
     * @param event     Event in question.
     * @param fcuts     FiducialCuts class instance.
     * @param applyCuts Boolean to decide if fiducial cuts should be applied or not.
     * @return An arraylist of clusters for each FMT layer.
     */
    public static ArrayList<Cluster>[] getClusters(DataEvent event, FiducialCuts fcuts,
            boolean applyCuts) {
        // Get data bank.
        DataBank clBank = HipoHandler.getBank(event, "FMT::Clusters");
        if (clBank == null) return null;

        ArrayList[] clusters = new ArrayList[Constants.FMTLAYERS];
        for (int li = 0; li < Constants.FMTLAYERS; ++li) clusters[li] = new ArrayList<Cluster>();

        for (int cri = 0; cri < clBank.rows(); ++cri) {
            fcuts.increaseClusterCount();
            int id        = clBank.getShort("index",      cri);
            int li        = clBank.getByte( "layer",      cri) - 1;
            int strip     = clBank.getInt(  "seedStrip",  cri);
            int size      = clBank.getShort("size",       cri);
            double energy = clBank.getFloat("energy",     cri);
            // NOTE. tMin is used in a cut, but currently the measurement has issues so it's ignored
            //       for the time being.
            double tMin   = 100; // clBank.getFloat("Tmin", cri);
            double y      = clBank.getFloat("centroid",   cri);
            int tID       = clBank.getShort("trackIndex", cri);

            // Apply cluster fiducial cuts.
            if (applyCuts && fcuts.checkClusterCuts(strip, size, energy, tMin)) continue;

            clusters[li].add(new Cluster(id, li, strip, y, tMin, energy, tID));
        }

        return clusters;
    }
}
