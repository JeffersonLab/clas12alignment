package org.clas.dc.alignment;

/**
 *
 * @author devita
 */
public class Constants {
    
    public static int NSECTOR = 6;
    public static int NLAYER  = 36;
    public static int NREGION = 3;
    
    // electron cuts
    public static double NPHEMIN = 2;
    public static double ECALMIN = 0.2;
    
    // histogram limits for residuals
    public static int    RESBINS = 200;
    public static double RESMIN  = -5000;
    public static double RESMAX  =  5000;
    // histogram limits for vertex plots
    public static int    VTXBINS = 500;
    public static double VTXMIN = -15.0;
    public static double VTMAX =  5.0;

    
    // global fit
    public static int NPARS = 18;
    // size of unit shift (cm for ri_xyz, deg for ri_cxyz
    public static double[] UNITSHIFT = {   0.1,    0.8,    0.2,     0.2,     0.2,     0.2,
                                           0.1,    0.8,    0.2,     0.2,     0.2,     0.2,
                                           0.1,    0.8,    0.2,     0.2,     0.2,     0.2};
    // parameter names    
    public static String[]   PARNAME = {"r1_x", "r1_y", "r1_z", "r1_cx", "r1_cy", "r1_cz",
                                        "r2_x", "r2_y", "r2_z", "r2_cx", "r2_cy", "r2_cz",
                                        "r3_x", "r3_y", "r3_z", "r3_cx", "r3_cy", "r3_cz"};
    // parameter step size: set to 0 to fix the parameter
    public static double[]   PARSTEP = {  0.2,  0.2,   0.2,     0,    0,       0,
                                          0.2,  0.2,   0.2,     0,    0,       0,
                                          0.2,  0.2,   0.2,     0,    0,       0};
    // parameter step size: set to 0 to fix the parameter
    public static double[]   PARMAX  = {  0.3,     0.3,   0.3,        0.3,  0.3,     0.3,
                                          0.3,     0.3,   0.3,        0.3,  0.3,     0.3,
                                          0.3,     0.3,   0.3,        0.3,  0.3,     0.3};
    // measurements weight   
    public static double[] MEASWEIGHT = { 1,  1,      1,      1,       1,       1,       1, 
                                              1,      1,      1,       1,       1,       1, 
                                              1,      1,      1,       1,       1,       1, 
                                              1,      1,      1,       1,       1,       1, 
                                              1,      1,      1,       1,       1,       1, 
                                              1,      1,      1,       1,       1,       1};               
    
    // target parameters
    public static double TARGETPOS    = -0.55;
    public static double TARGETLENGTH =  5;   //target length
    public static double WINDOWDIST   =  2.8; //distance between the mylar foil and the downstream window
    public static double SCALE        = 1000;
    
}
