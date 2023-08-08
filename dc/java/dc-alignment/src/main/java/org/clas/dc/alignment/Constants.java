package org.clas.dc.alignment;

/**
 *
 * @author devita
 */
public class Constants {
    
    public static final String LOGGERNAME = "dc-alignment";

    public static int NSECTOR = 6;
    public static int NLAYER  = 36;
    public static int NREGION = 3;
    public static double THTHILT = 25;
    public static int NTARGET = 2;
    
    // electron cuts
    public static double NPHEMIN = 2;
    public static double ECALMIN = 0.5;
    
    // histogram limits for residuals
    public static int    RESBINS = 200;
    public static double RESMIN  = -5000;
    public static double RESMAX  =  5000;
    // histogram limits for residual shift
    public static int    DIFBINS = 200;
    public static double DIFMIN  = -1000;
    public static double DIFMAX  =  1000;
    // histogram limits for vertex plots
    public static int    VTXBINS = 500;
    public static double VTXMIN = -20.0;
    public static double VTXMAX =  15.0;
    // histogram limits for vertex difference plots
    public static int    VDFBINS = 200;
    public static double VDFMIN = -5.0;
    public static double VDFMAX =  5.0;

    
    // global fit
    public static int NPARS = 18;
    
    // size of unit shift (cm for ri_xyz, deg for ri_cxyz
//    public static double[] UNITSHIFT = {   0.1,    0.8,    0.2,     0.2,     0.2,     0.4,
//                                           0.05,   0.4,    0.1,     0.2,     0.1,     0.2,
//                                           0.1,    0.8,    0.2,     0.2,     0.1,     0.4};
    public static double[] UNITSHIFT = {   0.1,    0.8,    0.2,     0.2,     0.2,     0.2,
                                           0.1,    0.8,    0.2,     0.2,     0.2,     0.2,
                                           0.1,    0.8,    0.2,     0.2,     0.2,     0.2};
    // parameter names    
    public static String[]   PARNAME = {"r1_x", "r1_y", "r1_z", "r1_cx", "r1_cy", "r1_cz",
                                        "r2_x", "r2_y", "r2_z", "r2_cx", "r2_cy", "r2_cz",
                                        "r3_x", "r3_y", "r3_z", "r3_cx", "r3_cy", "r3_cz"};

    // parameter step size: set to 0 to fix the parameter
    public static double[]   PARSTEP = {  0.2,  0.2,   0.2,     0,    0.2,       0.2,
                                          0.2,  0.2,   0.2,     0,    0.2,       0.2,
                                          0.2,  0.2,   0.2,     0,    0.2,       0.2};
   
    // parameter max value
    public static double[]   PARMAX  = {  1.5,     1.5,   1.5,        0.5,  0.5,     0.5,
                                          1.5,     1.5,   1.5,        0.5,  0.5,     0.5,
                                          1.5,     1.5,   1.5,        0.5,  0.5,     0.5};
 
    // parameter status
    public static boolean[] PARACTIVE  = new boolean[NPARS];
    
    
    // measurements weight   
    public static double[] MEASWEIGHT = { 1,  1,      1,      1,       1,       1,       1, 
                                              1,      1,      1,       1,       1,       1, 
                                              1,      1,      1,       1,       1,       1, 
                                              1,      1,      1,       1,       1,       1, 
                                              1,      1,      1,       1,       1,       1, 
                                              1,      1,      1,       1,       1,       1,   1};               
    
    // target parameters
    public static double TARGETPOS    = -0.5;
    public static double TARGETLENGTH =  5;    //target length
    public static double WINDOWDIST   =  6.8;  //6.8;//2.8; //distance between the mylar foil and the downstream window
    public static double SCEXIT       = 28.4;  //scattering chamber exit window, old value from PDF - 2 mm for the window bow
    public static double SCALE        = 1000;
    
}
