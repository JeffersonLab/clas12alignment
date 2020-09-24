package BMT_geo;

/**
 * 
 * @author defurne
 */

public class Lorentz {
	
	public Lorentz() {
		
	}
	
	public static double GetLorentzAngle(double xe, double xb) {
		if (xe==0||xb==0) return 0;
		double de = (BMT_geo.Constants.emax-BMT_geo.Constants.emin)/(BMT_geo.Constants.Ne-1);
		double db = (BMT_geo.Constants.bmax-BMT_geo.Constants.bmin)/(BMT_geo.Constants.Nb-1);	
		
		if (xe<BMT_geo.Constants.emin) {
		    xe=BMT_geo.Constants.emin;
		    System.err.println("Warning: E out of grid... setting it to Emin");
		  }
		  if (xe>=BMT_geo.Constants.emax) {
		    xe=BMT_geo.Constants.emax*0.99;
		    System.err.println("Warning: E out of grid... setting it to Emax");
		  }
		  if (xb>BMT_geo.Constants.bmax) {
		    xb=BMT_geo.Constants.bmax*0.99;
		    System.err.println("Warning: B field out of grid... setting it to Bmax");
		  }
		  
		  int i11 = getBin( xe, xb);
		  int i12 = getBin( xe, xb+db);
		  int i21 = getBin( xe+de, xb);
		  int i22 = getBin( xe+de, xb+db);
		 
		  double Q11 = 0; double Q12 = 0; double Q21 = 0;   double Q22 = 0;
		  double e1 = BMT_geo.Constants.emin; double e2 = BMT_geo.Constants.emax; double b1 = 0; double b2 = BMT_geo.Constants.bmax; 
		  if (i11>=0) {
		    Q11=BMT_geo.Constants.ThetaL_grid[i11]; e1 = BMT_geo.Constants.E_grid[i11];  b1 = BMT_geo.Constants.B_grid[i11];
		  }
		  if (i12>=0) Q12 = BMT_geo.Constants.ThetaL_grid[i12];
		  if (xb>=BMT_geo.Constants.bmin) Q21 = BMT_geo.Constants.ThetaL_grid[i21];
		  if (xb<BMT_geo.Constants.bmin) Q21 = 0;
		  if (i22>=0) {
		    Q22 = BMT_geo.Constants.ThetaL_grid[i22]; e2 = BMT_geo.Constants.E_grid[i22];  b2 = BMT_geo.Constants.B_grid[i22];
		  }
		 
		  double R1 = linInterp( xe, e1,e2,Q11,Q21);
		  double R2 = linInterp( xe, e1,e2,Q12,Q22);
		  
		  double P =  linInterp( xb, b1,b2,R1, R2);
		  
		  return P;
	}

	
	public static double linInterp(double x, double x1, double x2, double y1, double y2) {
		// linear interpolation
		  // return y = f(x), given x1, y1=f(x1) and x2, y2=f(x2) 
		  // y = m * ( x - x1 ) + y1
		  // m = ( y2 - y1)/(x2 - x1)
		  
		  // compute m
		  double m = (y2 - y1)/(x2 - x1);
		  
		  // return
		  return m * ( x - x1 ) + y1;
	}
	
	public static int getBin( double e, double b){
		double de = (BMT_geo.Constants.emax-BMT_geo.Constants.emin)/(BMT_geo.Constants.Ne-1);
		double db = (BMT_geo.Constants.bmax-BMT_geo.Constants.bmin)/(BMT_geo.Constants.Nb-1);
		
		int ie = (int) Math.floor( (e - BMT_geo.Constants.emin)/de );
		int ib = (int) Math.floor( (b - BMT_geo.Constants.bmin)/db );
		
			//   std::cout << ie << "  " << ib << "\n";
		  	
		  return ib + BMT_geo.Constants.Nb * ie ;
		}	
}