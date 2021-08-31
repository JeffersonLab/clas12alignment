package org.clas.analysis;

import java.util.Arrays;
import org.clas.cross.Constants;

public class FiducialCuts {

    boolean ypAlign = false; // Yaw-Pitch alignment needs less cuts than other types of alignment.

    // Physics cuts:
    private static final double maxDeltaZ  =   0.05;
    private static final double maxPzOverP =   0.4;
    private static final double minTmin    =  50;
    private static final double maxTmin    = 500;
    private static final double minEnergy  = 100;

    // Track-Cluster matching constants.
    private static final double dely    = 20; // Maximum y distance permitted.
    private static final double deltmin = 60; // Maximum delta Tmin permitted.

    // Class variables.
    int[] trsc = new int[5]; // cut trajectory points counter.
    int[] clsc = new int[5]; // cut clusters counter.

    public FiducialCuts() {}
    public void setYPAlign(boolean ypAlign) {this.ypAlign = ypAlign;}

    /** Increase the total number of trajectory points processed by 1. */
    public void increaseTrajCount() {
        trsc[0]++;
    }

    /** Increase the total number of clusters processed by 1. */
    public void increaseClusterCount() {
        clsc[0]++;
    }

    public void resetCounters() {
        Arrays.fill(trsc, 0);
        Arrays.fill(clsc, 0);
    }

    /**
     * Check if a track is too far downstream before swimming it.
     * @param trkZ track's z coordinate.
     * @param lyrZ layer's z coordinate.
     * @return true if track z is downstream the layer z
     */
    public boolean downstreamTrackCheck(double trkZ, double lyrZ) {
        if (trkZ > lyrZ) {
            trsc[1]++;
            return true;
        }
        return false;
    }

    /**
     * Check if a track needs to be cut by z, x, y, or its theta angle. Tracks that need to be cut
     * due to the delta Tmin between clusters are processed by another method due to its complexity.
     * @param z     track's z coordinate at FMT layer.
     * @param x     track's x coordinate at FMT layer.
     * @param y     track's y coordinate at FMT layer
     * @param zRef  FMT layer's z coordinate.
     * @param costh cosine of track's theta angle (pz/p).
     * @return true if the track is to be cut, false otherwise.
     */
    public boolean checkTrajCuts(double z, double x, double y, double zRef, double costh) {
        if (!ypAlign && Math.abs(z - zRef) > maxDeltaZ) {
            trsc[2]++;
            return true;
        }
        if (Constants.getInnerRadius() > (x*x + y*y) || (x*x + y*y) > Constants.getOuterRadius()) {
            trsc[3]++;
            return true;
        }
        if (!ypAlign && costh > maxPzOverP) {
            trsc[4]++;
            return true;
        }
        return false;
    }

    /**
     * Check if a cluster needs to be cut by its seed strip, size, energy, or Tmin.
     * @param strip seed strip of the cluster.
     * @param size  cluster's size.
     * @param E     cluster's total energy.
     * @param Tmin  cluster's Tmin.
     * @return true if the cluster is to be cut, false otherwise.
     */
    public boolean checkClusterCuts(int strip, int size, double E, double Tmin) {
        if (strip < Constants.getFirstStripNumber() || strip > Constants.getLastStripNumber()) {
            clsc[1]++;
            return true;
        }
        if (Tmin < minTmin || Tmin > maxTmin) {
            clsc[2]++;
            return true;
        }
        if (size == 1 && E < minEnergy) {
            clsc[3]++;
            return true;
        }
        if (!ypAlign && size >= 5) {
            clsc[4]++;
            return true;
        }
        return false;
    }

    /** Print applied cuts information. */
    public void printCutsInfo() {
        int trscsum = trsc[1] + trsc[2] + trsc[3] + trsc[4];
        int clscsum = clsc[1] + clsc[2] + clsc[3] + clsc[4];
        System.out.printf("\n");
        System.out.printf("            trajs too downstream │ %8d (%5.2f%%)   │\n",
                trsc[1], 100 * ((double) trsc[1]) / trsc[0]);
        System.out.printf("              trajs too upstream │ %8d (%5.2f%%)   │\n",
                trsc[2], 100 * ((double) trsc[2]) / trsc[0]);
        System.out.printf("      trajs in layer's bad areas │ %8d (%5.2f%%)   │\n",
                trsc[3], 100 * ((double) trsc[3]) / trsc[0]);
        System.out.printf("      trajs with theta too large │ %8d (%5.2f%%)   │\n",
                trsc[4], 100 * ((double) trsc[4]) / trsc[0]);
        System.out.printf("             TOTAL TRAJS DROPPED │ %8d / %8d │\n",
                trscsum, trsc[0]);
        System.out.printf("                               %% │ %5.2f%%              │\n",
                100 * ((double) trscsum) / trsc[0]);
        System.out.printf("─────────────────────────────────┼─────────────────────┤\n");
        System.out.printf("clusters with wrong strip number │ %8d (%5.2f%%)   │\n",
                clsc[1], 100 * ((double) clsc[1]) / clsc[0]);
        System.out.printf(" clusters with inappropiate Tmin │ %8d (%5.2f%%)   │\n",
                clsc[2], 100 * ((double) clsc[2]) / clsc[0]);
        System.out.printf("  small clusters with low energy │ %8d (%5.2f%%)   │\n",
                clsc[3], 100 * ((double) clsc[3]) / clsc[0]);
        System.out.printf("                clusters too big │ %8d (%5.2f%%)   │\n",
                clsc[4], 100 * ((double) clsc[4]) / clsc[0]);
        System.out.printf("          TOTAL CLUSTERS DROPPED │ %8d / %8d │\n",
                clscsum, clsc[0]);
        System.out.printf("                               %% │ %5.2f%%              │\n",
                100 * ((double) clscsum) / clsc[0]);
        System.out.printf("─────────────────────────────────┼─────────────────────┤\n");
    }
}
