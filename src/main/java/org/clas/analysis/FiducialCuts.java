package org.clas.analysis;

public class FiducialCuts {
    // track-cluster matching constants:
    private static final double dely    = 20; // Maximum y distance permitted.
    private static final double deltmin = 60; // Maximum delta Tmin permitted.

    // class variables:
    int[] trsc; // cut trajectory points counter.
    int[] clsc; // cut clusters counter.
    int[] crsc; // cut crosses counter.

    public FiducialCuts() {
        trsc = new int[]{0, 0, 0, 0, 0};
        clsc = new int[]{0, 0, 0, 0, 0};
        crsc = new int[]{0, 0, 0};
    }

    /** Increase the total number of trajectory points processed by 1. */
    public void increaseTrajCount() {
        trsc[0]++;
    }

    /** Increase the total number of clusters processed by 1. */
    public void increaseClusterCount() {
        clsc[0]++;
    }

    /** Increase the total number of crosses processed by 1. */
    public void increaseCrossCount(int amnt) {
        crsc[0] += amnt;
    }

    public void resetCounters() {
        for (int tri=0; tri<trsc.length; ++tri) trsc[tri]=0;
        for (int cli=0; cli<clsc.length; ++cli) clsc[cli]=0;
        for (int cri=0; cri<crsc.length; ++cri) crsc[cri]=0;
    }

    /**
     * Check if a track is too far downstream before swimming it.
     * @param trkZ track's z coordinate.
     * @param lyrZ layer's z coordinate.
     * @return
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
     * @param sc    track error counter, used for debugging.
     * @return true if the track is to be cut, false otherwise.
     */
    public boolean checkTrajCuts(double z, double x, double y, double zRef, double costh) {
        if (Math.abs(z-zRef)>0.05) {
            trsc[2]++;
            return true;
        }
        if (25.0 > x*x + y*y || x*x + y*y > 225.0) {
            trsc[3]++;
            return true;
        }
        if (costh>0.4) {
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
     * @param sc    cluster error counter, used for debugging.
     * @return true if the cluster is to be cut, false otherwise.
     */
    public boolean checkClusterCuts(int strip, int size, double E, double Tmin) {
        if (strip<0 || strip>1023) {
            clsc[1]++;
            return true;
        }
        if (Tmin < 50 || Tmin > 500) {
            clsc[2]++;
            return true;
        }
        if (size == 1 && E < 100) {
            clsc[3]++;
            return true;
        }
        if (size >= 5) {
            clsc[4]++;
            return true;
        }

        return false;
    }

    /**
     * Check if a cross needs to be cut due to a cluster being too far from the trajectory points in
     * its same FMT layer in the y coordinate. Currently unused.
     * @param traj_y trajectory point y position.
     * @param clus_y cluster y position.
     * @return true if the cross is to be cut, false otherwise.
     */
    public boolean checkCrossDeltaY(double traj_y, double clus_y) {
        if (Math.abs(traj_y-clus_y)>=dely) {
            crsc[1]++;
            return true;
        }
        return false;
    }

    /**
     * Check if a cross needs to be cut due to two clusters' Tmin being too far.
     * @param c0tmin cluster 0 Tmin.
     * @param c1tmin cluster 1 Tmin.
     * @param c2tmin cluster 2 Tmin.
     * @return true if the cross is to be cut, false otherwise.
     */
    public boolean checkCrossDeltaTmin(double c0tmin, double c1tmin, double c2tmin) {
        if (Math.abs(c0tmin - c1tmin) >= deltmin || Math.abs(c1tmin - c2tmin) >= deltmin) {
            crsc[2]++;
            return true;
        }
        return false;
    }

    /** Print applied cuts information. */
    public void printCutsInfo() {
        int trscsum = trsc[1] + trsc[2] + trsc[3] + trsc[4];
        int clscsum = clsc[1] + clsc[2] + clsc[3] + clsc[4];
        int crscsum = crsc[1] + crsc[2];
        System.out.printf("\n");
        System.out.printf("            trajs too downstream │ %8d (%5.2f%%)   │\n",
                trsc[1], 100*((double)trsc[1])/trsc[0]);
        System.out.printf("              trajs too upstream │ %8d (%5.2f%%)   │\n",
                trsc[2], 100*((double)trsc[2])/trsc[0]);
        System.out.printf("      trajs in layer's bad areas │ %8d (%5.2f%%)   │\n",
                trsc[3], 100*((double)trsc[3])/trsc[0]);
        System.out.printf("      trajs with theta too large │ %8d (%5.2f%%)   │\n",
                trsc[4], 100*((double)trsc[4])/trsc[0]);
        System.out.printf("             TOTAL TRAJS DROPPED │ %8d / %8d │\n",
                trscsum, trsc[0]);
        System.out.printf("                               %% │ %5.2f%%              │\n",
                100*((double)trscsum)/trsc[0]);
        System.out.printf("─────────────────────────────────┼─────────────────────┤\n");
        System.out.printf("clusters with wrong strip number │ %8d (%5.2f%%)   │\n",
                clsc[1], 100*((double)clsc[1])/clsc[0]);
        System.out.printf(" clusters with inappropiate Tmin │ %8d (%5.2f%%)   │\n",
                clsc[2], 100*((double)clsc[2])/clsc[0]);
        System.out.printf("  small clusters with low energy │ %8d (%5.2f%%)   │\n",
                clsc[3], 100*((double)clsc[3])/clsc[0]);
        System.out.printf("                clusters too big │ %8d (%5.2f%%)   │\n",
                clsc[4], 100*((double)clsc[4])/clsc[0]);
        System.out.printf("          TOTAL CLUSTERS DROPPED │ %8d / %8d │\n",
                clscsum, clsc[0]);
        System.out.printf("                               %% │ %5.2f%%              │\n",
                100*((double)clscsum)/clsc[0]);
        System.out.printf("─────────────────────────────────┼─────────────────────┤\n");
        System.out.printf(" cluster y too distant to traj y | %8d (%5.2f%%)   |\n",
                crsc[2], 100*((double)crsc[1])/crsc[0]);
        System.out.printf("   clusters with large tmin diff | %8d (%5.2f%%)   |\n",
                crsc[2], 100*((double)crsc[2])/crsc[0]);
        System.out.printf("           TOTAL CROSSES DROPPED | %8d / %8d |\n",
                crscsum, crsc[0]);
        System.out.printf("                               %% | %5.2f%%              │\n",
                100*((double)crscsum)/crsc[0]);
        System.out.printf("─────────────────────────────────┴─────────────────────┘\n");
    }
}
