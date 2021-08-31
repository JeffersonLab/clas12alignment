package org.clas.cross;

/** Class containing constants relevant to FMT and DC geometry. */
public class Constants {
    // Geometry.
    public static final double FMTINNERRADIUS      = 25;
    public static final double FMTOUTERRADIUS      = 225;
    public static final int    FMTLAYERS           = 3;
    public static final int    FMTREGIONS          = 4;
    public static final int    DCSECTORS           = 6;
    public static final int[]  FMTREGIONSEPARATORS = new int[]{-1, 319, 511, 831, 1023};

    // Physics cuts.
    public static final double MAXDZ     = 0.05; // Max z distance between cluster and traj point.
    public static final double MAXPZ     = 0.4;  // Max Pz/P.
    public static final double MINTMIN   = 50;   // Min Tmin for a cluster.
    public static final double MAXTMIN   = 500;  // Max Tmin for a cluster.
    public static final double MINENERGY = 100;  // Min energy for a cluster.

    public Constants() {}
}
