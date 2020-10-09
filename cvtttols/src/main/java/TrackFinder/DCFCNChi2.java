package TrackFinder;

import org.freehep.math.minuit.FCNBase;
import TrackFinder.TrackCandidate;
import Trajectory.StraightLine;
import TrackFinder.Fitter;
import BMT_struct.*;
import BST_struct.*;
import DC_struct.Segment;

import org.jlab.geom.prim.Vector3D;

public class DCFCNChi2 implements FCNBase {

	Segment ToFit;
	
	public double valueOf(double[] par)
	   {
			//Radius of the middle layer which should be crossed by the track in anycase	
		  StraightLine line=new StraightLine();
		  line.setSlope_XYZ(Math.cos(par[0])*Math.sin(par[1]),Math.sin(par[0])*Math.sin(par[1]),Math.cos(par[1]));
		  		  		  
		  line.setPoint_XYZ(par[2], par[3], par[4]);
		  
	      double val=ToFit.ComputeChi2(line);
	     
	      return val;
	   }
	
	public void SetTrackCandidate(Segment seg) {
		ToFit=seg;
	}

}
