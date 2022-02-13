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
    
    
    
    // global fit
    public static int NPARS = 18;
    // size of unit shift (cm for ri_xyz, deg for ri_cxyz
    public static double   UNITSHIFT =  0.2;
    // parameter names    
    public static String[]   PARNAME = {"r1_x", "r1_y", "r1_z", "r1_cx", "r1_cy", "r1_cz",
                                        "r2_x", "r2_y", "r2_z", "r2_cx", "r2_cy", "r2_cz",
                                        "r3_x", "r3_y", "r3_z", "r3_cx", "r3_cy", "r3_cz"};
    // parameter weight   
    public static double[] PARWEIGHT = {     1,      1,      1,       1,       1,       1, 
                                             1,      1,      1,       1,       1,       1, 
                                             1,      1,      1,       1,       1,       1};
    // parameter step size: set to 0 to fix the parameter
    public static double[]   PARSTEP = {  0.01,      0,   0.01,       0,    0.01,       0,
                                          0.01,      0,   0.01,       0,    0.01,       0,
                                          0.01,      0,   0.01,       0,    0.01,       0};
               
    
    // target parameters
    public static double TARGETPOS    = -0.5;
    public static double TARGETLENGTH =  5;   //target length
    public static double WINDOWDIST   =  2.8; //distance between the mylar foil and the downstream window

}
