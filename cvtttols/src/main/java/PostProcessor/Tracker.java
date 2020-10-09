package PostProcessor;

import TrackFinder.*;
import Trajectory.*;
import java.util.*;
import Analyzer.*;
import DC_struct.DriftChambers;
import DC_struct.Segment;
import PostProcessor.*;
import Analyzer.*;

public class Tracker {
	
	private int[] ntarget;
	private int nBeamFinder;
	private VertexFinder Vexter;
	private BeamAna BPMer;
		
	private HashMap<Integer, ArrayList<TrackCandidate> > Events;
	private HashMap<Integer, ArrayList<Segment> > FDEvents;
	
	public Tracker(String Type) {
		ntarget=new int[6];
		for (int sec=0;sec<6;sec++) ntarget[sec]=0;
		nBeamFinder=30;
		Events=new HashMap<Integer, ArrayList<TrackCandidate> >();
		FDEvents=new HashMap<Integer, ArrayList<Segment> >();
		Vexter=new VertexFinder();
		BPMer=new BeamAna(Type);	
	}
	
	
	
	public void addCVTEvent(int event, HashMap<Integer,TrackCandidate> candidates) {
		ArrayList<TrackCandidate> TrackList=new ArrayList<TrackCandidate>();
		
		TrackList=CentralDuplicateRemoval(candidates);
		
		//Load the events and store them until enough statistics to find beam
		for (int i=0;i<TrackList.size();i++) {
				if (TrackList.get(i).IsFromTarget()) ntarget[0]++;
			}
			
		Events.put(Events.size()+1,TrackList);
		
		if (ntarget[0]>nBeamFinder) {
			BeamFinder Beamer=new BeamFinder();
			StraightLine Beam=Beamer.FindBeam(Events);
			Vexter.FindCVTVertices(Beam,Events);
			BPMer.CVTAnalyze(Beam, Events);
			Events.clear();
			ntarget[0]=0;
		}
		
	}
	
	public ArrayList<TrackCandidate> CentralDuplicateRemoval(HashMap<Integer,TrackCandidate> cand){
		ArrayList<TrackCandidate> Temp_good=new ArrayList<TrackCandidate>();
		ArrayList<TrackCandidate> good=new ArrayList<TrackCandidate>();
		
		//Keep first only good tracks
		for (int i=0;i<cand.size();i++) {
			//System.out.println(cand.get(i+1).get_chi2()+" "+cand.get(i+1).get_Nz()+" "+cand.get(i+1).get_Nc());
			if (cand.get(i+1).IsGoodCandidate()) {
				Temp_good.add(cand.get(i+1));
			}
		}
		
		ArrayList<Integer> Toremove=new ArrayList<Integer> ();
				
		
		for (int i=0;i<Temp_good.size();i++) {
			good.add(Temp_good.get(i));
			Toremove.add(i);
			
			for (int j=i+1;j<Temp_good.size();j++) {
				//if (good.get(good.size()-1).IsSimilar(Temp_good.get(j))&&good.get(good.size()-1).get_chi2()/(2*good.get(good.size()-1).size()+2*good.get(good.size()-1).BSTsize()-4)>Temp_good.get(j).get_chi2()/(2*Temp_good.get(j).size()+2*Temp_good.get(j).BSTsize()-4)) {
				if (good.get(good.size()-1).IsSimilar(Temp_good.get(j))) {
					if ((good.get(good.size()-1).size()+good.get(good.size()-1).BSTsize())<(Temp_good.get(j).size()+Temp_good.get(j).BSTsize())
						||((good.get(good.size()-1).size()+good.get(good.size()-1).BSTsize())==(Temp_good.get(j).size()+Temp_good.get(j).BSTsize())&&good.get(good.size()-1).get_chi2()>Temp_good.get(j).get_chi2())) {
						good.remove(good.size()-1);
						good.add(Temp_good.get(j));
					}
					
					Toremove.add(j);
				}
				
			}
			int removed=0;
			
			for (int index=0; index<Toremove.size(); index++) {
				Temp_good.remove(Toremove.get(index)-removed); //Remove all tracks similar with the one in good
				removed++;//Correct for the change of the size of the arraylist 
			}
			Toremove.clear();
		}
		
		return good;
	}
	
	public void draw() {
		BPMer.draw();
	}
	
	public void addForwardEvent(int event, DriftChambers DC) {
				
		this.ForwardDuplicateRemoval(DC);
		for (int sec=1;sec<7;sec++) {
			for (int tr=0; tr<DC.getSector(sec).getSectorSegments().size();tr++) {
				if (FDEvents.containsKey(sec)) {
					FDEvents.get(sec).add(DC.getSector(sec).getSectorSegments().get(tr));
					if (DC.getSector(sec).getSectorSegments().get(tr).ThroughFMT()) ntarget[sec-1]++; 
				}
				else {
					ArrayList<Segment> temp=new ArrayList<Segment>();
					temp.add(DC.getSector(sec).getSectorSegments().get(tr));
					if (DC.getSector(sec).getSectorSegments().get(tr).ThroughFMT()) ntarget[sec-1]++; 
					FDEvents.put(sec, temp);
				}
			}
		}
		
		for (int sec=1;sec<7;sec++) {
			if (ntarget[sec-1]>nBeamFinder) {
				BeamFinder Beamer=new BeamFinder();
				StraightLine Beam=Beamer.FindFDBeam(FDEvents.get(sec));
				Vexter.FindFDVertices(Beam,FDEvents.get(sec));
				BPMer.FDAnalyze(Beam, FDEvents.get(sec), sec);
				FDEvents.get(sec).clear();
				ntarget[sec-1]=0;
			}
		}
		
		
	}
	
	public void ForwardDuplicateRemoval(DriftChambers DC) {
		//First we clean all tracks with no convergence
				
		for (int sec=1;sec<7;sec++) {
			for (int tr=DC.getSector(sec).getSectorSegments().size()-1; tr>=0;tr--) {
				if (!DC.getSector(sec).getSectorSegments().get(tr).getFitStatus()||DC.getSector(sec).getSectorSegments().get(tr).getSize()<30||DC.getSector(sec).getSectorSegments().get(tr).getChi2()==Double.POSITIVE_INFINITY) {
					
					DC.getSector(sec).getSectorSegments().remove(tr);
					
				}
			}
		}
		
		// Remove Duplicated segment with the following criteria
		// -1) Keep the track with more hits -2) with best chi2 for same number of hits
		ArrayList<Segment> goodSegment = new ArrayList<Segment>();
		boolean Similar=false;
		
		for (int sec=1;sec<7;sec++) {
			for (int tr=DC.getSector(sec).getSectorSegments().size()-1; tr>=0;tr--) {
				
				// If we have already good track, we might want to check that there is no duplicate
				if (goodSegment.size()!=0) {
					for (int gtr=goodSegment.size()-1;gtr>-1;gtr--) {
						if (DC.getSector(sec).getSectorSegments().get(tr).IsSimilar(goodSegment.get(gtr))) {
							if (goodSegment.get(gtr).IsWorseThan(DC.getSector(sec).getSectorSegments().get(tr))) {
								goodSegment.remove(gtr);
								goodSegment.add(DC.getSector(sec).getSectorSegments().get(tr));
							}
							continue;
						}
					}
				}
				else goodSegment.add(DC.getSector(sec).getSectorSegments().get(tr));
							
			}
			DC.getSector(sec).getSectorSegments().clear();
			for (int gtr=0;gtr<goodSegment.size();gtr++) {
				DC.getSector(sec).getSectorSegments().add(goodSegment.get(gtr));
			}
			goodSegment.clear();
		}
			
	}

}
