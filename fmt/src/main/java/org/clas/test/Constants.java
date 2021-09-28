package org.clas.test;

/** Class containing constants relevant geometry, cuts, and the program in general. */
public class Constants {
    // Generic.
    public static final int    NVARS   = 6;   // Number of variables to be aligned.
    public static final int    NPLOTS  = 6;   // Number of plots to be drawn. Should be a factor of 2.
    public static final int    PLOTRES = 200; // Resolution of 1D and 2D plots.

    // Geometry.
    public static final double FMTINNERRADIUS      = 25;
    public static final double FMTOUTERRADIUS      = 225;
    public static final int    FMTLAYERS           = 3;
    public static final int    FMTREGIONS          = 4;
    public static final int    FMTNSTRIPS          = 1024;
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
