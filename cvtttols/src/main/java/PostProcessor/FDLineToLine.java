package PostProcessor;

import org.freehep.math.minuit.FCNBase;

import DC_struct.Segment;
import TrackFinder.TrackCandidate;
import Trajectory.StraightLine;
import PostProcessor.BeamFinder;
import java.util.*;

public class FDLineToLine implements FCNBase{
	
	ArrayList<Segment> FDTrack;

	public FDLineToLine(ArrayList<Segment> FTrack) {
		FDTrack=FTrack;
	}
	
	public double valueOf(double[] par)
	   {
		 double val=0;
		 StraightLine line=new StraightLine();
		
		 line.setPoint_XYZ(par[2], par[3], 0);
		 line.setSlope_XYZ(par[0],par[1],1);
		 
		 StraightLine Beam=new StraightLine();
		 Beam.setPoint_XYZ(0, 0, 0);
		 Beam.setSlope_XYZ(0,0,1);
		 
		 StraightLine track=new StraightLine();
		 
		 for (int i=0; i<FDTrack.size();i++) {
				double track_dist=Beam.getDistanceToLine(FDTrack.get(i).getHBtrack());
				double dist=line.getDistanceToLine(FDTrack.get(i).getHBtrack());
				if (track_dist<50) {
					val+=Math.pow(dist,2); 
				}
		 }
		 
		 return val;
	   }
	
}
