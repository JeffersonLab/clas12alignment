package BMT_geo;

import java.util.Random;
import java.io.*;
import java.util.Scanner;
import org.jlab.geom.prim.Vector3D;
import BMT_struct.Cluster;

public class Geometry {

    public Geometry() {

    }

    // Comments on the Geometry of the BMT 
    //------------------------------------
    // The BMT geometry consists of 3 cylindical double layers of MicroMegas. 
    // The inner (i.e. closest to beam in double layer) layer contain longitudinal strips oriented in the Z direction. 
    // This is called the Z layer. 
    // The outer layer contain arched strips at a non-constant pitch in Z. This is called the C layer.
    // The cylinder phi profile in divided into 3 sectors, A, B, C.
    // 
    /**
     *
     * @param sector
     * @return detector index A (=0), B (=1), C (=2)
     */
    public int getDetectorIndex(int sector) {
        int DetIdx = -1;

        DetIdx = sector - 1;
        return DetIdx;
    }

    /**
     *
     * @param sector the sector in CLAS12 1...3
     * @param layer the layer 1...6
     * @param strip the strip number (starts at 1)
     * @return the angle to localize the center of strip
     */
    public double CRZStrip_GetPhi(int sector, int layer, int strip) {

        // Sector = num_detector + 1;	
        // num_detector = 0 (region A), 1 (region B), 2, (region C)
        //For CRZ, this function returns the angle to localize the  center of strip "num_strip" for the "num_detector"
        int num_detector = this.getDetectorIndex(sector); 				// index of the detector (0...2)

        int num_strip = strip - 1;     									// index of the strip (starts at 0)
        int num_region = (int) (layer + 1) / 2 - 1; 						// region index (0...2) 0=layers 1&2, 1=layers 3&4, 2=layers 5&6

        //double angle=Constants.getCRZEDGE1()[num_region][num_detector]+(Constants.getCRZXPOS()[num_region]+(Constants.getCRZWIDTH()[num_region]/2.+num_strip*(Constants.getCRZWIDTH()[num_region]+Constants.getCRZSPACING()[num_region])))/Constants.getCRZRADIUS()[num_region];
        //double angle=Constants.getCRZEDGE1()[num_region][num_detector]+(0.5+num_strip)*Constants.getCRZWIDTH()[num_region]/Constants.getCRZRADIUS()[num_region];
        //double angle = Constants.getCRZEDGE1()[num_region][num_detector] + ((double) num_strip) * Constants.getCRZWIDTH()[num_region] / Constants.getCRZRADIUS()[num_region];
        double angle = Constants.getCRZEDGE1()[num_region][num_detector] + ((double) num_strip+0.5) * Constants.getCRZWIDTH()[num_region] / Constants.getCRZRADIUS()[num_region];
        return angle; //in rad 
    }

    /**
     *
     * @param layer the layer 1...6
     * @param angle the position angle of the hit in the Z detector
     * @return the Z strip as a function of azimuthal angle
     */
    public int getZStrip(int layer, double angle) { // the angle is the Lorentz uncorrected angle
        double jitter = Math.toRadians(Constants.isInSectorJitter);
        int num_region = (int) (layer + 1) / 2 - 1; // region index (0...2) 0=layers 1&2, 1=layers 3&4, 2=layers 5&6
        int num_detector = isInDetector(layer, angle, jitter);
        if (num_detector == -1) {
            return -1;
        }

        if (angle < 0) {
            angle += 2 * Math.PI; // from 0 to 2Pi
        }
        if (num_detector == 1) {
            double angle_f = Constants.getCRCEDGE1()[num_region][1] + (Constants.getCRCXPOS()[num_region] + Constants.getCRCLENGTH()[num_region]) / Constants.getCRCRADIUS()[num_region] - 2 * Math.PI;
            if (angle >= 0 && angle <= angle_f) {
                angle += 2 * Math.PI;
            }
        }
        //double strip_calc = ( (angle-Constants.getCRZEDGE1()[num_region][num_detector])*Constants.getCRZRADIUS()[num_region]-Constants.getCRZXPOS()[num_region]-Constants.getCRZWIDTH()[num_region]/2.)/(Constants.getCRZWIDTH()[num_region]+Constants.getCRZSPACING()[num_region]);
        //double strip_calc = ((angle - Constants.getCRZEDGE1()[num_region][num_detector]) * Constants.getCRZRADIUS()[num_region]) / (Constants.getCRZWIDTH()[num_region]);
        double strip_calc = ((angle - Constants.getCRZEDGE1()[num_region][num_detector]) * Constants.getCRZRADIUS()[num_region]) / (Constants.getCRZWIDTH()[num_region])-0.5;
        strip_calc = (int) (Math.round(strip_calc * 1d) / 1d);
        int strip_num = (int) Math.floor(strip_calc);

        int value = strip_num + 1;
        //int value = strip_num;

        if (value < 1 || value > Constants.getCRZNSTRIPS()[num_region]) {
            value = -1;
        }

        return value;
    }

    /**
     *
     * @param layer the layer 1...6
     * @return the Z position of the strip center
     */
    private double CRZ_GetZStrip(int layer) {
        int num_region = (int) (layer + 1) / 2 - 1; // region index (0...2) 0=layers 1&2, 1=layers 3&4, 2=layers 5&6
        //For CRZ, this function returns the Z position of the strip center
        double zc = Constants.getCRZZMIN()[num_region] + Constants.getCRZOFFSET()[num_region] + Constants.getCRZLENGTH()[num_region] / 2.;
        return zc; //in mm
    }

    /**
     *
     * @param sector the sector in CLAS12 1...3
     * @param layer the layer 1...6
     * @return the angle to localize the beginning of the strips
     */
    private double CRC_GetBeginStrip(int sector, int layer) {
        // Sector = num_detector + 1;	
        // num_detector = 0 (region A), 1 (region B), 2, (region C)

        int num_detector = this.getDetectorIndex(sector); 			// index of the detector (0...2)
        int num_region = (int) (layer + 1) / 2 - 1; // region index (0...2) 0=layers 1&2, 1=layers 3&4, 2=layers 5&6

        //For CRC, this function returns the angle to localize the beginning of the strips
        double angle = Constants.getCRCEDGE1()[num_region][num_detector] + Constants.getCRCXPOS()[num_region] / Constants.getCRCRADIUS()[num_region];
        if (angle > 2 * Math.PI) {
            angle -= 2 * Math.PI;
        }
        return angle; //in rad
    }

    /**
     *
     * @param sector the sector in CLAS12 1...3
     * @param layer the layer 1...6
     * @return the angle to localize the end of the strips
     */
    private double CRC_GetEndStrip(int sector, int layer) {
        // Sector = num_detector + 1;	
        // num_detector = 0 (region A), 1 (region B), 2, (region C)

        int num_detector = this.getDetectorIndex(sector); 			// index of the detector (0...2)
        int num_region = (int) (layer + 1) / 2 - 1; 					// region index (0...2) 0=layers 1&2, 1=layers 3&4, 2=layers 5&6

        //For CRC, this function returns the angle to localize the end of the strips
        double angle = Constants.getCRCEDGE1()[num_region][num_detector] + (Constants.getCRCXPOS()[num_region] + Constants.getCRCLENGTH()[num_region]) / Constants.getCRCRADIUS()[num_region];
        if (angle > 2 * Math.PI) {
            angle -= 2 * Math.PI;
        }
        return angle; //in rad
    }

    /**
     *
     * @param layer the hit layer
     * @param strip the hit strip
     * @return the z position in mm for the C-detectors
     */
    public double CRCStrip_GetZ(int layer, int strip) {

        int num_strip = strip - 1;     			// index of the strip (starts at 0)
        int num_region = (int) (layer + 1) / 2 - 1; // region index (0...2) 0=layers 1&2, 1=layers 3&4, 2=layers 5&6

        //For CR6C, this function returns the Z position of the strip center
        int group = 0;
        int limit = Constants.getCRCGROUP()[num_region][group];
        //double zc = Constants.getCRCZMIN()[num_region] + Constants.getCRCOFFSET()[num_region] + Constants.getCRCWIDTH()[num_region][group] / 2.;
        double zc = Constants.getCRCZMIN()[num_region];
        
        if (num_strip > 0) {
            for (int j = 1; j < num_strip + 1; j++) {
                zc += Constants.getCRCWIDTH()[num_region][group] / 2.;
                //zc += Constants.getCRCWIDTH()[num_region][group];
                if (j >= limit) { //test if we change the width
                    group++;
                    limit += Constants.getCRCGROUP()[num_region][group];
                }
                //zc += Constants.getCRCWIDTH()[num_region][group] / 2. + Constants.getCRCSPACING()[num_region];
                zc += Constants.getCRCWIDTH()[num_region][group]/2.;
            }
        }

        return zc; //in mm
    }

    public double CRCStrip_GetPitch(int layer, int strip) {

    	int num_strip = strip - 1;     			// index of the strip (starts at 0)
        int num_region = (int) (layer + 1) / 2 - 1; // region index (0...2) 0=layers 1&2, 1=layers 3&4, 2=layers 5&6
        double pitch=0.0;
        //For CR6C, this function returns the Z position of the strip center
        if (this.getZorC(layer)==0) {
        	int group = 0;
        	int limit = Constants.getCRCGROUP()[num_region][group];

        	if (num_strip > 0) {
        		for (int j = 1; j < num_strip + 1; j++) {

        			if (j > limit) { //test if we change the width
        				group++;
        				limit += Constants.getCRCGROUP()[num_region][group];
        			}
        		}
        	}
        	pitch=Constants.getCRCWIDTH()[num_region][group];
        }
        
        if (this.getZorC(layer)==1) {
        	pitch=Constants.getCRZWIDTH()[num_region];
        }
       
        return pitch; //
    }
    
    /**
     *
     * @param layer
     * @param trk_z the track z position of intersection with the C-detector
     * @return the C-strip
     */                                                  
    public int getCStrip(int layer, double trk_z) {

        int num_region = (int) (layer + 1) / 2 - 1; // region index (0...2) 0=layers 1&2, 1=layers 3&4, 2=layers 5&6
        int strip_group = 0;
        int ClosestStrip = -1;
        // get group
        int len = Constants.getCRCGROUP()[num_region].length;
        double[] Z_lowBound = new double[len];
        double[] Z_uppBound = new double[len];
        int[] NStrips = new int[len];

        double zi = Constants.getCRCZMIN()[num_region] + Constants.getCRCOFFSET()[num_region];
        double z = trk_z - zi;

        Z_lowBound[0] = Constants.getCRCWIDTH()[num_region][0] / 2.; // the lower bound is the zMin+theOffset with half the width
        Z_uppBound[0] = Z_lowBound[0]
                + (Constants.getCRCGROUP()[num_region][0] - 1) * (Constants.getCRCWIDTH()[num_region][0] + Constants.getCRCSPACING()[num_region]);
        NStrips[0] = Constants.getCRCGROUP()[num_region][0];
        for (int i = 1; i < len; i++) {
            Z_lowBound[i] = Z_uppBound[i - 1] + Constants.getCRCWIDTH()[num_region][i - 1] / 2. + Constants.getCRCSPACING()[num_region] + Constants.getCRCWIDTH()[num_region][i] / 2.;
            Z_uppBound[i] = Z_lowBound[i] + (Constants.getCRCGROUP()[num_region][i] - 1) * (Constants.getCRCWIDTH()[num_region][i] + Constants.getCRCSPACING()[num_region]);

            NStrips[i] = NStrips[i - 1] + Constants.getCRCGROUP()[num_region][i];

            if (z >= Z_lowBound[i] && z <= Z_uppBound[i]) {
                strip_group = i;
                ClosestStrip = 1 + (int) (Math.round(((z - Z_lowBound[strip_group]) / (Constants.getCRCWIDTH()[num_region][strip_group] + Constants.getCRCSPACING()[num_region])))) + NStrips[i - 1];
                //ClosestStrip = (int) (Math.round(((z-Z_lowBound[strip_group])/(Constants.getCRCWIDTH()[num_region][strip_group] + Constants.getCRCSPACING()[num_region]))))+NStrips[i-1];

                len = i;
            }
        }
        return ClosestStrip;
    }

    /**
     *
     * @param layer
     * @param x x-coordinate of the hit in the lab frame
     * @param y y-coordinate of the hit in the lab frame
     * @return the sigma along the beam direction (longitudinal)
     */
    public double getSigmaLongit(int layer, double x, double y) { // sigma for C-detector

        int num_region = (int) (layer + 1) / 2 - 1; // region index (0...2) 0=layers 1&2, 1=layers 3&4, 2=layers 5&6
        //double sigma = Constants.SigmaDrift * Math.sqrt((Math.sqrt(x * x + y * y) - Constants.getCRCRADIUS()[num_region] + Constants.hStrip2Det) / Constants.hDrift);
        double sigma = Constants.SigmaDrift * ((Math.sqrt(x * x + y * y) - Constants.getCRZRADIUS()[num_region] + Constants.hStrip2Det) / Constants.hDrift / Math.cos(Constants.getThetaL()));
  
        return sigma;

    }

    /**
     *
     * @param layer
     * @param x x-coordinate of the hit in the lab frame
     * @param y y-coordinate of the hit in the lab frame
     * @return the sigma along in the azimuth direction taking the Lorentz angle
     * into account
     */
    public double getSigmaAzimuth(int layer, double x, double y) { // sigma for Z-detectors

        int num_region = (int) (layer + 1) / 2 - 1; // region index (0...2) 0=layers 1&2, 1=layers 3&4, 2=layers 5&6double Z0=0;
        double sigma = Constants.SigmaDrift * Math.sqrt((Math.sqrt(x * x + y * y) - Constants.getCRZRADIUS()[num_region] + Constants.hStrip2Det) / Constants.hDrift / Math.cos(Constants.getThetaL()));

        return sigma;

    }

    /**
     *
     * @param layer
     * @param x x-coordinate of the hit in the lab frame
     * @param y y-coordinate of the hit in the lab frame
     * @param z z-coordinate of the hit in the lab frame
     * @return X[] = the smeared position x = X[0], y = X[1], z = X[2] taking
     * the Lorentz angle into account
     */
    /*
	public double[] smearedPosition(int layer, double x, double y, double z) {
		
		double[] newPos = new double[3];
        Random rand = new Random();
        
		int num_region = (int) (layer+1)/2 - 1; // region index (0...2) 0=layers 1&2, 1=layers 3&4, 2=layers 5&6;

		double sigma =0;
		if(layer%2==0) if(Double.isNaN(z)) {// C layer
			sigma = getSigmaLongit(layer, x, y); //  longitudinal shower profile
			// changes z
			z = this.randomGaus(z, sigma, rand);
		}
		
		if(layer%2==1)  {// Z layer
			sigma = getSigmaAzimuth(layer, x, y); //  azimuth shower profile taking into account the Lorentz angle
			// changes phi
			double phicorr = (this.randomGaus(0, sigma, rand)/Math.cos(Constants.getThetaL())
					-(Math.sqrt(x*x+y*y)-Constants.getCRZRADIUS()[num_region]+
							Constants.hStrip2Det)*Math.tan(Constants.getThetaL()))/Constants.getCRZRADIUS()[num_region];
			double phi = Math.atan2(y, x); 
			phi+=phicorr;
			
			x = Math.sqrt(x*x+y*y)*Math.cos(phi);
			y = Math.sqrt(x*x+y*y)*Math.sin(phi);
		}
		newPos[0] = x;
		newPos[1] = y;
		newPos[2] = z;
		
		return newPos;
	}
     */
    private double randomGaus(double mean, double width, Random aRandom) {
        if (width <= 0) {
            return 0;
        }

        double smear = width * aRandom.nextGaussian();
        double randomNumber = mean + smear;

        return randomNumber;
    }
   
     public Vector3D Slope_CVTToDetFrame(int layer, int sector, Vector3D slope) {	
     	Vector3D new_slope = new Vector3D();
     	new_slope.setX(slope.x()); new_slope.setY(slope.y()); new_slope.setZ(slope.z());
     	new_slope.rotateX(BMT_geo.Constants.getRx(layer,sector));
		new_slope.rotateY(BMT_geo.Constants.getRy(layer,sector));
		new_slope.rotateZ(BMT_geo.Constants.getRz(layer,sector));
 	
     	return new_slope;
     }
     
     public Vector3D Point_CVTToDetFrame(int layer, int sector, Vector3D point) {	
      	Vector3D new_point = new Vector3D();
      	new_point.setX(point.x()); new_point.setY(point.y()); new_point.setZ(point.z());
      	new_point.rotateX(BMT_geo.Constants.getRx(layer,sector));
 		new_point.rotateY(BMT_geo.Constants.getRy(layer,sector));
 		new_point.rotateZ(BMT_geo.Constants.getRz(layer,sector));
 		new_point.setX(new_point.x()+BMT_geo.Constants.getCx(layer,sector));
		new_point.setY(new_point.y()+BMT_geo.Constants.getCy(layer,sector));
		new_point.setZ(new_point.z()+BMT_geo.Constants.getCz(layer,sector));
  	
      	return new_point;
      }
     
     public Vector3D Slope_DetToCVTFrame(int layer, int sector, Vector3D slope) {	
      	Vector3D new_slope = new Vector3D();
      	new_slope.setX(slope.x()); new_slope.setY(slope.y()); new_slope.setZ(slope.z());
      	new_slope.rotateZ(-BMT_geo.Constants.getRz(layer,sector));
 		new_slope.rotateY(-BMT_geo.Constants.getRy(layer,sector));
 		new_slope.rotateX(-BMT_geo.Constants.getRx(layer,sector));
  	
      	return new_slope;
      }
      
      public Vector3D Point_DetToCVTFrame(int layer, int sector, Vector3D point) {	
       	Vector3D new_point = new Vector3D();
       	new_point.setX(point.x()); new_point.setY(point.y()); new_point.setZ(point.z());
       	new_point.setX(new_point.x()-BMT_geo.Constants.getCx(layer,sector));
 		new_point.setY(new_point.y()-BMT_geo.Constants.getCy(layer,sector));
 		new_point.setZ(new_point.z()-BMT_geo.Constants.getCz(layer,sector));
 		new_point.rotateZ(-BMT_geo.Constants.getRz(layer,sector));
 		new_point.rotateY(-BMT_geo.Constants.getRy(layer,sector));
 		new_point.rotateX(-BMT_geo.Constants.getRx(layer,sector));
   	
       	return new_point;
       }
      
      public Vector3D Slope_LabToCVTFrame(Vector3D slope) {	
       	Vector3D new_slope = new Vector3D();
       	new_slope.setX(slope.x()); new_slope.setY(slope.y()); new_slope.setZ(slope.z());
       	new_slope.rotateX(BMT_geo.Constants.getRxCVT());
  		new_slope.rotateY(BMT_geo.Constants.getRyCVT());
  		new_slope.rotateZ(BMT_geo.Constants.getRzCVT());
   	
       	return new_slope;
       }
       
       public Vector3D Point_LabToCVTFrame(Vector3D point) {	
        	Vector3D new_point = new Vector3D();
        	new_point.setX(point.x()); new_point.setY(point.y()); new_point.setZ(point.z());
        	new_point.rotateX(BMT_geo.Constants.getRxCVT());
        	new_point.rotateY(BMT_geo.Constants.getRyCVT());
        	new_point.rotateZ(BMT_geo.Constants.getRzCVT());
        	new_point.setX(new_point.x()+BMT_geo.Constants.getCxCVT());
        	new_point.setY(new_point.y()+BMT_geo.Constants.getCyCVT());
        	new_point.setZ(new_point.z()+BMT_geo.Constants.getCzCVT());
    	
        	return new_point;
        }
       
       public Vector3D Slope_CVTToLabFrame(Vector3D slope) {	
        	Vector3D new_slope = new Vector3D();
        	new_slope.setX(slope.x()); new_slope.setY(slope.y()); new_slope.setZ(slope.z());
        	new_slope.rotateZ(-BMT_geo.Constants.getRzCVT());
        	new_slope.rotateY(-BMT_geo.Constants.getRyCVT());
        	new_slope.rotateX(-BMT_geo.Constants.getRxCVT());
    	
        	return new_slope;
        }
        
        public Vector3D Point_CVTToLabFrame(Vector3D point) {	
         	Vector3D new_point = new Vector3D();
         	new_point.setX(point.x()); new_point.setY(point.y()); new_point.setZ(point.z());
         	new_point.setX(new_point.x()-BMT_geo.Constants.getCxCVT());
         	new_point.setY(new_point.y()-BMT_geo.Constants.getCyCVT());
         	new_point.setZ(new_point.z()-BMT_geo.Constants.getCzCVT());
         	new_point.rotateZ(-BMT_geo.Constants.getRzCVT());
         	new_point.rotateY(-BMT_geo.Constants.getRyCVT());
         	new_point.rotateX(-BMT_geo.Constants.getRxCVT());
     	
         	return new_point;
         }
      
 
 
    /**
     *
     * @param sector
     * @param layer
     * @param x
     * @return a boolean indicating is the track hit is in the fiducial detector
     */
    public boolean isInFiducial(int sector, int layer, int axis, double[] x) {

        boolean isInFid = false;

        int num_region = (int) (layer + 1) / 2 - 1; // region index (0...2) 0=layers 1&2, 1=layers 3&4, 2=layers 5&6;

        double z_i = CRZ_GetZStrip(layer) - Constants.getCRZLENGTH()[num_region] / 2.; // fiducial z-profile lower limit
        double z_f = CRZ_GetZStrip(layer) + Constants.getCRZLENGTH()[num_region] / 2.; // fiducial z-profile upper limit

        double R_i = 0; // inner radius init
        double R_f = 0; // outer radius init for a C or Z detector
        if (BMT_geo.Geometry.getZorC(layer) == 1) {
            R_i = Constants.getCRZRADIUS()[num_region]; // Z layer
        }
        if (BMT_geo.Geometry.getZorC(layer) == 0) {
            R_i = Constants.getCRCRADIUS()[num_region]; // // C-dtectors 
        }
        R_f = R_i + Constants.hDrift;

        double angle_i = 0; // first angular boundary init
        double angle_f = 0; // second angular boundary for detector A, B, or C init
        double A_i = CRC_GetBeginStrip(sector, layer);
        double A_f = CRC_GetEndStrip(sector, layer);
        angle_i = A_i;
        angle_f = A_f;
        if (A_i > A_f) { // for B-detector
            angle_f = A_i;
            angle_i = A_f;
        }
        // the hit parameters
        double angle = Math.atan2(x[1], x[0]);
        if (angle > 2 * Math.PI) {
            angle -= 2 * Math.PI;
        }
        double R = Math.sqrt(x[0] * x[0] + x[1] * x[1]);
        double z = x[2];

        if ((angle_i - angle) < (angle_f - angle_i) && (R - R_i) < (R_f - R_i) && (z - z_i) < (z_f - z_i)) {
            isInFid = true;
        }

        return isInFid;
    }

    // in A (index 0), B (index 1), in C (index 2)
    public int isInDetector(int layer, double angle, double jitter) {
        int num_region = (int) (layer + 1) / 2 - 1; // region index (0...2) 0=layers 1&2, 1=layers 3&4, 2=layers 5&6
        if (angle < 0) {
            angle += 2 * Math.PI; // from 0 to 2Pi
        }
     
        double angle_i = 0; // first angular boundary init
        double angle_f = 0; // second angular boundary for detector A, B, or C init
        int num_detector = 2;
        //double jitter = Math.toRadians(Constants.isInSectorJitter);
        for (int i = 0; i < 3; i++) {

            //angle_i=Constants.getCRCEDGE1()[num_region][i]+Constants.getCRCXPOS()[num_region]/Constants.getCRCRADIUS()[num_region];
            //angle_f=Constants.getCRCEDGE1()[num_region][i]+(Constants.getCRCXPOS()[num_region]+Constants.getCRCLENGTH()[num_region])/Constants.getCRCRADIUS()[num_region];
            angle_i = Constants.getCRCEDGE1()[num_region][i];
            angle_f = Constants.getCRCEDGE2()[num_region][i];
            if ((angle >= angle_i - jitter && angle <= angle_f + jitter)) {
                num_detector = i;
            }
        }
       
        return num_detector;
    }

    public int isInSector(int layer, double angle, double jitter) {
        //double jitter = Math.toRadians(Constants.isInSectorJitter);
        int value = -1;
        int num_det = this.isInDetector(layer, angle, jitter);
        /*	if(num_det == 0)
			value = 2;
		if(num_det ==2)
			value = 3;
		if(num_det == 1)
			value = 1; */
        value = num_det + 1;

        return value;
    }

    public double LorentzAngleCorr(double phi, int layer) {

        int num_region = (int) (layer + 1) / 2 - 1; // region index (0...2) 0=layers 1&2, 1=layers 3&4, 2=layers 5&6
        //return phi +( Constants.hDrift/2*Math.tan(Constants.getThetaL()) )/Constants.getCRZRADIUS()[num_region];
        //return phi + (Constants.hDrift * Math.tan(Constants.getThetaL())) / (Constants.getCRZRADIUS()[num_region]);
        return phi + (Constants.hStrip2Det * Math.tan(Constants.getThetaL())) / (Constants.getCRZRADIUS()[num_region]);
    }
    public void SetLorentzAngle(int layer, int sector) {
     	BMT_geo.Constants.setThetaL(layer, sector); 
    }
    // Correct strip position before clustering
    public int getLorentzCorrectedZStrip(int sector, int layer, int theMeasuredZStrip) {

        double theMeasuredPhi = this.CRZStrip_GetPhi(sector, layer, theMeasuredZStrip);
        double theLorentzCorrectedAngle = this.LorentzAngleCorr(theMeasuredPhi, layer);

        return this.getZStrip(layer, theLorentzCorrectedAngle);
    }

    public static void main(String arg[]) {

        Constants.Load();
        Geometry geo = new Geometry();

        double trk_z = 0;

        int layer = 5;
        System.out.println(geo.CRCStrip_GetZ(6, 267) + " strip " + geo.getCStrip(6, -65.));
        /*
		int num_region = (int) (layer+1)/2 - 1; // region index (0...2) 0=layers 1&2, 1=layers 3&4, 2=layers 5&6
		int strip_group = 0;
		int ClosestStrip =-1;
		System.out.println((""+1*1+""+1*0+""));
		// get group
		int len = Constants.CRCGROUP[num_region].length;
		double[] Z_lowBound = new double[len];
		double[] Z_uppBound = new double[len];
		int[] NStrips = new int[len];
		
		double zi= Constants.CRCZMIN[num_region]+Constants.CRCOFFSET[num_region];
		double z = trk_z - zi;
		
		Z_lowBound[0] = Constants.CRCWIDTH[num_region][0]/2.; // the lower bound is the zMin+theOffset with half the width
		Z_uppBound[0] = Z_lowBound[0]
						   + (Constants.CRCGROUP[num_region][0]-1)*(Constants.CRCWIDTH[num_region][0]+ Constants.CRCSPACING[num_region]);
		NStrips[0] = Constants.CRCGROUP[num_region][0];
		for(int i =1; i< len; i++)
		{
			Z_lowBound[i] = Z_uppBound[i-1] + Constants.CRCWIDTH[num_region][i-1]/2. + Constants.CRCSPACING[num_region] + Constants.CRCWIDTH[num_region][i]/2.;
			Z_uppBound[i] = Z_lowBound[i] + (Constants.CRCGROUP[num_region][i]-1)*(Constants.CRCWIDTH[num_region][i] + Constants.CRCSPACING[num_region]);
			
			NStrips[i] = NStrips[i-1] + Constants.CRCGROUP[num_region][i];
			
			if(z>=Z_lowBound[i] && z<=Z_uppBound[i]) {
				strip_group = i;
				ClosestStrip = 1 + (int) (Math.round(((z-Z_lowBound[strip_group])/(Constants.CRCWIDTH[num_region][strip_group] + Constants.CRCSPACING[num_region]))))+NStrips[i-1];

				len =i;
			} 
		}
		 double[] X = geo.smearedPosition(5, 0 , Constants.CRZRADIUS[2] , 0);
		 System.out.println(0+", "+(0.3+Constants.CRZRADIUS[2])+" , "+0+"  smeared "+X[0]+", "+X[1]+" , "+X[2]);
		 System.out.println(geo.getZStrip(5, Math.atan2(Constants.CRZRADIUS[2],0 )));
		 System.out.println(geo.getZStrip(5, Math.atan2(X[1],X[0])));
		 System.out.println(Math.toDegrees( geo.CRZStrip_GetPhi(1,6, geo.getZStrip(5, Math.atan2(X[1],X[0]))) ));	
		 int theMeasuredZStrip = geo.getZStrip(5, Math.atan2(X[1],X[0])); // start reco
		 double theMeasuredPhi = geo.CRZStrip_GetPhi(1,6,theMeasuredZStrip);
		 double theLorentzCorrectedAngle = geo.LorentzAngleCorr( theMeasuredPhi, 6);
		 System.out.println(" corrected phi = "+Math.toDegrees(theLorentzCorrectedAngle));
		 int theLorentzCorrectedStrip = geo.getZStrip(5, theLorentzCorrectedAngle);
		 System.out.println(theMeasuredZStrip+" "+theLorentzCorrectedStrip); */
 /*
		 double phiC = geo.CRZStrip_GetPhi(3,6,216);
		 double x = Constants.CRCRADIUS[2]*Math.cos(phiC);
		 double y = Constants.CRCRADIUS[2]*Math.sin(phiC);
		 int theMeasuredCStrip = geo.getCStrip(6,X[2]);
		 double z = geo.CRCStrip_GetZ(6,309);
		 System.out.println(x+", "+y+", "+z);*/
        //List<double[]> Hits = geo.GEMCBMTHits(layer, sector, -199.89230321711165 , 93.78543124898611 , -164.52000000000007, .1);
        //System.out.println("There are "+Hits.size()+" hits in this cluster");
        //for(int i =0; i<Hits.size(); i++) {
        //	System.out.println(" strip "+(int)Hits.get(i)[0]+" Edep "+Hits.get(i)[1]);
        //}
    }

    public boolean isInFiducial(double x, double y, double z, int layer) {

        boolean isOK = false;

        int num_region = (int) (layer + 1) / 2 - 1;

        int axis = Geometry.getZorC(layer);

        double R = 0;
        if (axis == 0) {
            R = BMT_geo.Constants.getCRCRADIUS()[num_region];
        }
        if (axis == 1) {
            R = BMT_geo.Constants.getCRZRADIUS()[num_region];
        }

        double CRZLENGTH = BMT_geo.Constants.getCRCLENGTH()[num_region];
        double CRZZMIN = BMT_geo.Constants.getCRZZMIN()[num_region];
        double CRZOFFSET = BMT_geo.Constants.getCRZOFFSET()[num_region];

        double z_min = CRZZMIN + CRZOFFSET;
        double z_max = z_min + CRZLENGTH;

        double epsilon = 1e-1;

        if (Math.abs(x) < R + epsilon && Math.abs(y) < R + epsilon && z > z_min - epsilon && z < z_max + epsilon) {
            isOK = true;
        }
        return isOK;
    }

    public final static int getZorC(int layer) {
        int axis = 0;
        if (layer == 2 || layer == 3 || layer == 5) {
            axis = 1;
        }
        return axis;
    }
    
    public double getClusterRadius(int layer) {
    	int num_region = (int) (layer + 1) / 2 - 1;
    	double r=0;
    	if (getZorC(layer)==1) r=BMT_geo.Constants.getCRZRADIUS()[num_region]+BMT_geo.Constants.hStrip2Det;
    	if (getZorC(layer)==0) r=BMT_geo.Constants.getCRCRADIUS()[num_region]+BMT_geo.Constants.hStrip2Det;
    	//r=r+BMT_geo.Constants.hStrip2Det;
    	return r;
    }
    
    public double getResidual_line(Cluster clus, Vector3D slope_lab, Vector3D point_lab) {
		double distance=0;
		Vector3D point_inter=new Vector3D();
		Vector3D point_inter_a=new Vector3D();
		Vector3D point_inter_b=new Vector3D();
		point_inter.setXYZ(Double.NaN,Double.NaN,Double.NaN);
		
		Vector3D slope_cvt=this.Slope_LabToCVTFrame(slope_lab);
		Vector3D point_cvt=this.Point_LabToCVTFrame(point_lab);
		Vector3D slope=this.Slope_CVTToDetFrame(clus.getLayer(), clus.getSector(), slope_cvt);
		Vector3D point=this.Point_CVTToDetFrame(clus.getLayer(), clus.getSector(), point_cvt);
				
		//For C-detector, it a bit more complicated... You need to find the intersection between the cylinder and the line, which involves x and y component
		double sx=slope.x(); double sy=slope.y(); 
		double ix=point.x(); double iy=point.y();
			  
		//Find the intersection
		double a=sx*sx+sy*sy;
		double b=2*(sx*ix+sy*iy);
		double c=ix*ix+iy*iy-clus.getRadius()*clus.getRadius();
			 
		double delta=b*b-4*a*c;
		if (delta==0) {
		    double lambda=-b/2./a;
		    point_inter.setXYZ(slope.x()*lambda+point.x(),slope.y()*lambda+point.y(),slope.z()*lambda+point.z());
			if (Double.isNaN(clus.getX())&&Double.isNaN(clus.getY())&&!Double.isNaN(clus.getZ())) distance=clus.getZ()-point_inter.z();
			if (!Double.isNaN(clus.getX())&&!Double.isNaN(clus.getY())&&Double.isNaN(clus.getZ())) {
	    		double phi_clus=clus.getPhi();
				if (phi_clus>2*Math.PI) phi_clus=phi_clus-2*Math.PI;
				double phi_proj=Math.atan2(point_inter.y(), point_inter.x());
				if (phi_proj<0) phi_proj=phi_proj+2*Math.PI;
				double delta_phi=phi_clus-phi_proj;
				while (delta_phi>Math.PI) {
					delta_phi-=2*Math.PI;
				}
				while (delta_phi<-Math.PI) {
					delta_phi+=2*Math.PI;
				}
				distance=clus.getRadius()*delta_phi;
	    	}
		}
		if (delta>0) {
			double lambda_a=(-b+Math.sqrt(delta))/2./a;
		    double lambda_b=(-b-Math.sqrt(delta))/2./a;
		    point_inter_a.setXYZ(slope.x()*lambda_a+point.x(),slope.y()*lambda_a+point.y(),slope.z()*lambda_a+point.z());
		    point_inter_b.setXYZ(slope.x()*lambda_b+point.x(),slope.y()*lambda_b+point.y(),slope.z()*lambda_b+point.z());
		    point_inter=this.ClosestToSector(clus.getSector(), point_inter_a, point_inter_b);
		    	if (Double.isNaN(clus.getX())&&Double.isNaN(clus.getY())&&!Double.isNaN(clus.getZ())) distance=clus.getZ()-point_inter.z();
		    	if (!Double.isNaN(clus.getX())&&!Double.isNaN(clus.getY())&&Double.isNaN(clus.getZ())) {
		    		double phi_clus=clus.getPhi();
					if (phi_clus>2*Math.PI) phi_clus=phi_clus-2*Math.PI;
					double phi_proj=Math.atan2(point_inter.y(), point_inter.x());
					if (phi_proj<0) phi_proj=phi_proj+2*Math.PI;
					double delta_phi=phi_clus-phi_proj;
					while (delta_phi>Math.PI) {
						delta_phi-=2*Math.PI;
					}
					while (delta_phi<-Math.PI) {
						delta_phi+=2*Math.PI;
					}
					distance=clus.getRadius()*delta_phi;
		    	}
		}
		 
		  		
		return distance;
	}
    
    public Vector3D getIntercept(int layer, int sector, Vector3D slope_lab, Vector3D point_lab) {
		Vector3D point_inter=new Vector3D();
		Vector3D point_inter_a=new Vector3D();
		Vector3D point_inter_b=new Vector3D();
		point_inter.setXYZ(Double.NaN,Double.NaN,Double.NaN);
		
		Vector3D slope_cvt=this.Slope_LabToCVTFrame(slope_lab);
		Vector3D point_cvt=this.Point_LabToCVTFrame(point_lab);
		Vector3D slope=this.Slope_CVTToDetFrame(layer, sector, slope_cvt);
		Vector3D point=this.Point_CVTToDetFrame(layer, sector, point_cvt);
								
		//For C-detector, it a bit more complicated... You need to find the intersection between the cylinder and the line, which involves x and y component
		double sx=slope.x(); double sy=slope.y(); 
		double ix=point.x(); double iy=point.y();
			  
		//Find the intersection
		double a=sx*sx+sy*sy;
		double b=2*(sx*ix+sy*iy);
		double c=ix*ix+iy*iy-this.getClusterRadius(layer)*this.getClusterRadius(layer);
			 
		double delta=b*b-4*a*c;
		if (delta==0) {
		    double lambda=-b/2./a;
		    point_inter.setXYZ(slope.x()*lambda+point.x(),slope.y()*lambda+point.y(),slope.z()*lambda+point.z());
		}
		if (delta>0) {
			double lambda_a=(-b+Math.sqrt(delta))/2./a;
		    double lambda_b=(-b-Math.sqrt(delta))/2./a;
		    point_inter_a.setXYZ(slope.x()*lambda_a+point.x(),slope.y()*lambda_a+point.y(),slope.z()*lambda_a+point.z());
		    point_inter_b.setXYZ(slope.x()*lambda_b+point.x(),slope.y()*lambda_b+point.y(),slope.z()*lambda_b+point.z());
		    if (this.isinsector(point_inter_b)==sector) point_inter=point_inter_b;
		    if (this.isinsector(point_inter_a)==sector) point_inter=point_inter_a;
		}
		Vector3D point_CVT=this.Point_DetToCVTFrame(layer, sector, point_inter);  
		Vector3D point_final=this.Point_CVTToLabFrame(point_CVT);  		
		return point_final;
	}
	
		
	public int isinsector(Vector3D point) {
		int sec=0;
		double ang=Math.toDegrees(Math.atan2(point.y(), point.x()));
		if (ang<30) ang=ang+360;
		if (ang>30&&ang<150) sec=2;
		if (ang>=150&&ang<270) sec=1;
		if (ang>=270&&ang<390) sec=3;
		
		return sec;
	}
	
	public Vector3D ClosestToSector(int sector, Vector3D point_a, Vector3D point_b) {
		Vector3D pt=new Vector3D();
		
		double ang_a=Math.toDegrees(Math.atan2(point_a.y(), point_a.x()));
		double ang_b=Math.toDegrees(Math.atan2(point_b.y(), point_b.x()));
		if (ang_a<30) ang_a=ang_a+360;
		if (ang_b<30) ang_b=ang_b+360;
		double middle_sec=210-120*(sector-1);
		if (middle_sec<0) middle_sec+=360;
		double delta_phi_a=ang_a-middle_sec;
		double delta_phi_b=ang_b-middle_sec;
		while (delta_phi_a>180) {
			delta_phi_a-=360;
		}
		while (delta_phi_a<-180) {
			delta_phi_a+=360;
		}
		while (delta_phi_b>180) {
			delta_phi_b-=360;
		}
		while (delta_phi_b<-180) {
			delta_phi_b+=360;
		}
		
		if (Math.abs(delta_phi_a)>Math.abs(delta_phi_b)) pt.setXYZ(point_b.x(), point_b.y(), point_b.z());
		if (Math.abs(delta_phi_b)>Math.abs(delta_phi_a)) pt.setXYZ(point_a.x(), point_a.y(), point_a.z());
		
		return pt;
	}
	
	public void LoadMisalignmentFromFile(String FileName) throws IOException{
		File GeoTrans=new File(FileName);
		
		String separator = "\\s+";
		
		if (GeoTrans.exists()) {
			System.out.println("Opening misalignment file for MVT: "+FileName);
			String[] line=new String[8];
			int linenumber=0;
			Scanner input = new Scanner(GeoTrans);
            while (input.hasNextLine()) {
            	line = input.nextLine().trim().replaceAll(separator, " ").split(separator);
            	if (Integer.parseInt(line[0])>6) {
            	//Rx   Ry    Rz    Tx    Ty     Tz => order of columns inside the file
            		Constants.setRx(Integer.parseInt(line[0])-6,Integer.parseInt(line[1]), Double.parseDouble(line[2]));
            		Constants.setRy(Integer.parseInt(line[0])-6,Integer.parseInt(line[1]), Double.parseDouble(line[3]));
            		Constants.setRz(Integer.parseInt(line[0])-6,Integer.parseInt(line[1]), Double.parseDouble(line[4]));
            		Constants.setCx(Integer.parseInt(line[0])-6,Integer.parseInt(line[1]), Double.parseDouble(line[5]));
            		Constants.setCy(Integer.parseInt(line[0])-6,Integer.parseInt(line[1]), Double.parseDouble(line[6]));
            		Constants.setCz(Integer.parseInt(line[0])-6,Integer.parseInt(line[1]), Double.parseDouble(line[7]));
            	}
			//linenumber++;
			}
		}
		
	}
	
	public void LoadMVTSVTMisalignment(String FileName) throws IOException{
		File GeoTrans=new File(FileName);
		
		String separator = "\\s+";
		
		if (GeoTrans.exists()) {
			System.out.println("Opening misalignment file for MVT versus SVT: "+FileName);
			String[] line=new String[8];
			Scanner input = new Scanner(GeoTrans);
           	line = input.nextLine().trim().replaceAll(separator, " ").split(separator);
            //Rx   Ry    Rz    Tx    Ty     Tz => order of columns inside the file
            Constants.setRxCVT(Double.parseDouble(line[0]));
            Constants.setRyCVT(Double.parseDouble(line[1]));
            Constants.setRzCVT(Double.parseDouble(line[2]));
            Constants.setCxCVT(Double.parseDouble(line[3]));
            Constants.setCyCVT(Double.parseDouble(line[4]));
            Constants.setCzCVT(Double.parseDouble(line[5]));
         }
		
	}
	
}
