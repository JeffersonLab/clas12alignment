package org.clas.dc.alignment;

import org.jlab.geom.prim.Vector3D;

/**
 * Utility class to create the lines of text to be inserted in the GEMC gcard
 * for implementing shifts
 * 
 * @author devita
 */
public class Utilities {

    public static void sameShift() {
        
        Vector3D[] shifts = new Vector3D[3];
        
        shifts[0] = new Vector3D(0.0328, 0.0175, 0.0211);
        shifts[1] = new Vector3D(0.1333, 0.0465, -0.0807);
        shifts[2] = new Vector3D(0.0615, -0.0367, 0.0209);
        
        double[][][] align = new double[3][6][6];
        
        for(int is =0; is<6; is++) {
              int sector = is+1;
              for(int ir=0; ir<3; ir++) {
                    int region = ir+1;
                    
                    Vector3D v = new Vector3D();
                    v.copy(shifts[ir]);
                    v.rotateZ(is*Math.toRadians(60));
                    
                    align[ir][is][0] = v.x();
                    align[ir][is][1] = v.y();
                    align[ir][is][2] = v.z();
                    align[ir][is][3] = 0;
                    align[ir][is][4] = 0;
                    align[ir][is][5] = 0;
                    
                    String gcard = String.format("      <detector name=\"region%d_s%d\">\n", region, sector) +
                                   String.format("		  <position x=\"%.4f*cm\"  y=\"%.4f*cm\"  z=\"%.4f*cm\"  />\n", v.x(), v.y(), v.z()) +
                                                 "	  	  <rotation x=\"0*deg\"    y=\"0*deg\"  z=\"0.0*deg\"  />\n" +
                                                 "	</detector>";
                    System.out.println(gcard);
              }
        }    
        for(int ir=0; ir<3; ir++) {
            for(int is =0; is<6; is++) {
                System.out.println(String.format("%d %d 0 %.4f %.4f %.4f %.4f %.4f %.4f", ir+1, is+1, align[ir][is][0], align[ir][is][1], align[ir][is][2], align[ir][is][3], align[ir][is][4], align[ir][is][5]));
            }
        }
    }
    
    
    public static void main(String[] args){

        sameShift();
    }
}
