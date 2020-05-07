package org.clas.analysis;

public class FiducialCuts {
    int[] tsc; // cut tracks counter.
    int[] csc; // cut clusters counter.

    public FiducialCuts() {
        tsc = new int[]{0, 0, 0, 0, 0, 0};
        csc = new int[]{0, 0, 0, 0, 0};
    }

    /** Increase the total number of tracks processed by 1. */
    public void increaseTrackCount() {
        tsc[0]++;
    }

    /** Increase the total number of clusters processed by 1. */
    public void increaseClusterCount() {
        csc[0]++;
    }

    /**
     * Check if a track is too far downstream before swimming it.
     * @param trkZ : track's z coordinate.
     * @param lyrZ : layer's z coordinate.
     * @return
     */
    public boolean downstreamTrackCheck(double trkZ, double lyrZ) {
        if (trkZ > lyrZ) {
            tsc[1]++;
            return true;
        }

        return false;
    }

    /**
     * Check if a track needs to be cut by z, x, y, or its theta angle. Tracks
     * that need to be cut due to the delta Tmin between clusters are processed
     * by another method due to its complexity.
     * @param z     : track's z coordinate at FMT layer.
     * @param x     : track's x coordinate at FMT layer.
     * @param y     : track's y coordinate at FMT layer
     * @param zRef  : FMT layer's z coordinate.
     * @param costh : cosine of track's theta angle (pz/p).
     * @param sc    : track error counter, used for debugging.
     * @return true if the track is to be cut, false otherwise.
     */
    public boolean checkTrackCuts(double z, double x, double y, double zRef,
            double costh) {

        if (Math.abs(z-zRef)>0.05) {
            tsc[2]++;
            return true;
        }
        if (25.0 > x*x + y*y || x*x + y*y > 225.0) {
            tsc[3]++;
            return true;
        }
        if (costh>0.4) {
            tsc[4]++;
            return true;
        }

        return false;
    }

    /**
     * Check if a cluster needs to be cut by its seed strip, size, energy, or
     * Tmin.
     * @param strip : seed strip of the cluster.
     * @param size  : cluster's size.
     * @param E     : cluster's total energy.
     * @param Tmin  : cluster's Tmin.
     * @param sc    : cluster error counter, used for debugging.
     * @return true if the cluster is to be cut, false otherwise.
     */
    public boolean checkClusterCuts(int strip, int size, double E,
            double Tmin) {

        if (strip<0 || strip>1023) {
            csc[1]++;
            return true;
        }
        if (Tmin < 50 || Tmin > 500) {
            csc[2]++;
            return true;
        }
        if (size == 1 && E < 100) {
            csc[3]++;
            return true;
        }
        if (size >= 5) {
            csc[4]++;
            return true;
        }

        return false;
    }

    /** Print applied cuts information. */
    public void printCutsInfo() {
        int tscsum = tsc[1] + tsc[2] + tsc[3] + tsc[4] + tsc[5];
        int cscsum = csc[1] + csc[2] + csc[3] + csc[4];
        System.out.printf("\n");
        System.out.printf("           tracks too downstream │ %8d (%5.2f%%)   │\n",
                tsc[1], 100*((double)tsc[1])/tsc[0]);
        System.out.printf("             tracks too upstream │ %8d (%5.2f%%)   │\n",
                tsc[2], 100*((double)tsc[2])/tsc[0]);
        System.out.printf("     tracks in layer's bad areas │ %8d (%5.2f%%)   │\n",
                tsc[3], 100*((double)tsc[3])/tsc[0]);
        System.out.printf("     tracks with theta too large │ %8d (%5.2f%%)   │\n",
                tsc[4], 100*((double)tsc[4])/tsc[0]);
        System.out.printf(" tracks' with inconsistent Tmins │ %8d (%5.2f%%)   │\n",
                tsc[5], 100*((double)tsc[5])/tsc[0]);
        System.out.printf("            TOTAL TRACKS DROPPED │ %8d / %8d │\n",
                tscsum, tsc[0]);
        System.out.printf("                               %% │ %5.2f%%              │\n",
                100*((double)tscsum)/tsc[0]);
        System.out.printf("─────────────────────────────────┼─────────────────────┤\n");
        System.out.printf("clusters with wrong strip number │ %8d (%5.2f%%)   │\n",
                csc[1], 100*((double)csc[1])/csc[0]);
        System.out.printf(" clusters with inappropiate Tmin │ %8d (%5.2f%%)   │\n",
                csc[2], 100*((double)csc[2])/csc[0]);
        System.out.printf("  small clusters with low energy │ %8d (%5.2f%%)   │\n",
                csc[3], 100*((double)csc[3])/csc[0]);
        System.out.printf("                clusters too big │ %8d (%5.2f%%)   │\n",
                csc[4], 100*((double)csc[4])/csc[0]);
        System.out.printf("          TOTAL CLUSTERS DROPPED │ %8d / %8d │\n",
                cscsum, csc[0]);
        System.out.printf("                               %% │ %5.2f%%              │\n",
                100*((double)cscsum)/csc[0]);
        System.out.printf("─────────────────────────────────┴─────────────────────┘\n");
    }
}
