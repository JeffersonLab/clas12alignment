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
    public static double VTXMAX =  35.0;
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
    
    // target parameter sets:
    // - target cell exit window position, 
    // - target length, 
    // - distance between cell exit window and indulation foil, 
    // - distance beetween the scattering chamber exit window and the target center
    public static final double[] DEFAULT       = {-0.5, 5.0, 6.8, 27.3};
    public static final double[] RGAFALL2018   = {-0.5, 5.0, 2.8, 28.4};
    public static final double[] RGBSPRING2019 = {-0.5, 5.0, 6.8, 28.4};
    public static final double[] RGFSUMMER2020 = {-32, 5.0, 6.8, 27.3};
    public static final double[] RGMFALL2021   = {-0.5, 5.0, 6.8, 27.3};
    public static final double[] RGCSUMMER2022 = {-1.4,5.25, 8.3, 14.3};
    public static final double[] RGDFALL2023   = {-2.5, 5.0, 3.0, 27.1};

    // target parameters used for vertex fit initialization
    public static double TARGETPOS    = DEFAULT[0];
    public static double TARGETLENGTH = DEFAULT[1];
    public static double WINDOWDIST   = DEFAULT[2];
    public static double SCEXIT       = DEFAULT[3];
    public static double TARGETCENTER = DEFAULT[0]-DEFAULT[1]/2;
    public static double PEAKWIDTH    = 4;
    public static double SCALE        = 1000;
    
    // moller cone entrance
    public static double MOLLERZ     = 45.389;
    public static double MOLLERR     = 3.2;
    
    public static void initTargetPars(double[] pars) {
        if(pars.length>0 && pars.length<=DEFAULT.length) {
            TARGETPOS = pars[0];
            TARGETCENTER = pars[0];
            if(pars.length>1) {
                TARGETLENGTH = pars[1];
                TARGETCENTER = pars[0]-pars[1]/2;
            }
            if(pars.length>2)
                WINDOWDIST   = pars[2];
            if(pars.length>3)
                SCEXIT       = pars[3];
        }
        else {
            System.out.println("[WARNING] wrong number of target parameters. Number is " + pars.length + " instead of [1:4]");
        }
        System.out.println("[CONFIG] target parameters set to:");
        System.out.println("         - TARGETPOS    = " + TARGETPOS);
        System.out.println("         - TARGETLENGTH = " + TARGETLENGTH);
        System.out.println("         - WINDOWDIST   = " + WINDOWDIST);
        System.out.println("         - SCEXIT       = " + SCEXIT);
    }

}
