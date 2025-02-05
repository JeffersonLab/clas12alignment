package org.clas.test;

/** Class containing constants relevant geometry, cuts, and the program in general. */
public class Constants {
    // Generic.
    public static final String DEFVARIATION = "rga_spring2018"; // Default ccdb variation.
    public static final String FMTTABLEGLO  = "/geometry/fmt/fmt_global";
    public static final String FMTTABLELOC  = "/geometry/fmt/fmt_layer_noshim";
    public static final int    NVARS   = 6;   // Number of variables to be aligned.
    public static final int    PLOTRES = 200; // Resolution of Residuals plots.
    public static final int    PLOTRNG = 10;   // Range for residuals plots in cm.
    public static final int    FITRNG  = 4;   // Range for residuals fit in cm.

    // Geometry.
    public static final double FMTINNERRADIUS      = 25;   // Squared inner radius of each FMT layer.
    public static final double FMTOUTERRADIUS      = 225;  // Squared outer radius of each FMT layer.
    public static final int    FMTLAYERS           = 6;    // Number of FMT layers.
    public static final int    FMTREGIONS          = 4;    // Number of FMT regions.
    public static final int    FMTNSTRIPS          = 1024; // Number of FMT strips.
    public static final int[]  FMTREGIONSEPARATORS = new int[]{-1, 319, 511, 831, 1023};
    public static final int    DCSECTORS           = 6;    // Number of DC sectors.

    // Default swim parameters.
    public static final double SOLMAGSCALE = -1.00; // Default solenoid magnet scale.
    public static final double TORMAGSCALE =  1.00; // Default torus magnet scale.
    public static final double SOLMAGSHIFT = -3.00; // Default solenoid magnet shift.
    public static final String TORUSMAP    = "Full_torus_r251_phi181_z251_25Jan2021.dat";    // Default torus map
    public static final String SOLENOIDMAP = "Symm_solenoid_r601_phi1_z1201_13June2018.dat"; // Default solenoid map 

    // Physics cuts.
    public static final double MAXDZ     = 0.05; // Max z distance between cluster and traj point.
    public static final double MAXPZ     = 0.4;  // Max Pz/P.
    public static final double MINTMIN   = 50;   // Min Tmin for a cluster.
    public static final double MAXTMIN   = 500;  // Max Tmin for a cluster.
    public static final double MINENERGY = 0;  // Min energy for a cluster.

    public Constants() {}
}
