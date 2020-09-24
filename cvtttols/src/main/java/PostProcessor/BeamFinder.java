package PostProcessor;

import Trajectory.*;
import TrackFinder.*;
import Trajectory.*;
import java.util.*;
import PostProcessor.*;
import org.freehep.math.minuit.*;

import DC_struct.Segment;

public class BeamFinder {
	
	public BeamFinder() {
		
	}
	
	public StraightLine FindBeam(HashMap<Integer, ArrayList<TrackCandidate> > Events) {
		StraightLine beam=new StraightLine();
		
		//Create parameters
		MnUserParameters upar = new MnUserParameters();
	  	upar.add("tx", 0, 1,-0.5,0.5); //angle beween x,z
	  	upar.add("ty",0, 1,-0.5,0.5); //angle beween y,z
	    upar.add("x", 0, 50,-25,25);
	    upar.add("y", 0, 50,-25,25);
	    
	    //Create function to minimize
	    LineToLine BeamCarac=new LineToLine(Events);
	    
	    //Create Minuit (parameters and function to minimize)
	    MnMigrad migrad = new MnMigrad(BeamCarac, upar);
	    
	    //Haven t checked if it is necessarry... might duplicate Straight to parameters for minimum
	    FunctionMinimum min = migrad.minimize();
	    
	    //If fit is valid, then compute the residuals
	    if (min.isValid()) {
	    	double[] res=migrad.params();
	    	beam.setPoint_XYZ(res[2], res[3], 0);
	    	beam.setSlope_XYZ(res[0],res[1],1.0);
	    }
		
		return beam;
	}
	
	public StraightLine FindFDBeam(ArrayList<Segment> FDEvents) {
		StraightLine beam=new StraightLine();
		
		//Create parameters
		MnUserParameters upar = new MnUserParameters();
		upar.add("tx", 0, 0.5,-0.25,0.25); //angle beween x,z
		upar.add("ty",0, 0.5,-0.25,0.25); //angle beween y,z
		upar.add("x", 0, 50,-25,25);
		upar.add("y", 0, 50,-25,25);
		
		//Create function to minimize
		FDLineToLine BeamCarac=new FDLineToLine(FDEvents);
	    
		//Create Minuit (parameters and function to minimize)
		MnMigrad migrad = new MnMigrad(BeamCarac, upar);
	    
		//Haven t checked if it is necessarry... might duplicate Straight to parameters for minimum
		FunctionMinimum min = migrad.minimize();
	    
		//If fit is valid, then compute the residuals
		if (min.isValid()) {
			double[] res=migrad.params();
			beam.setPoint_XYZ(res[2], res[3], 0);
			beam.setSlope_XYZ(res[0],res[1],1.0);
			beam.Print();
		}
		else{
			beam.setPoint_XYZ(Double.NaN, Double.NaN, Double.NaN);
			beam.setSlope_XYZ(Double.NaN, Double.NaN, Double.NaN);
		}
				
		return beam;
	}

}
