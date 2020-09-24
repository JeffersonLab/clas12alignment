package TrackFinder;

import BMT_struct.*;
import BST_struct.*;
import java.util.*;
import TrackFinder.TrackCandidate;
import TrackFinder.Fitter;
import org.jlab.geom.prim.Vector3D;

public class TrackFinder {
	
	HashMap<Integer, TrackCandidate> Candidates;
	ArrayList<TrackCandidate> BufferLayer; //Temporary store the duplicated track to avoid infinite loop
	float time_match;
	int cand_newsec;
	Barrel BMT_det;
	Barrel_SVT BST_det;
	
	
	public TrackFinder(Barrel BMT, Barrel_SVT BST) {
		Candidates=new HashMap<Integer, TrackCandidate>();
		BufferLayer=new ArrayList<TrackCandidate>();
		time_match=40;
		if (main.constant.isCosmic) time_match=256;
		cand_newsec=0;
		BMT_det=BMT;
		BST_det=BST;
	}
	
	public HashMap<Integer, TrackCandidate> get_Candidates(){
		return Candidates;
	}
	
	public void setTimeMatch(float timing) {
		time_match=timing;
	}
	
	public void clear() {
		Candidates.clear();
	}
	
	public void BuildCandidates() {
		boolean IsAttributed=true;
		boolean noHit_yet_sector=true;
		
			//We are looking for Straight Track
			//We analyze each sector separately in beam configuration
			for (int sec=0;sec<3;sec++) {
				cand_newsec=Candidates.size(); //Avoid to mix the sectors between them
				noHit_yet_sector=true;
				
				for (int lay=5;lay>-1;lay--) {
					//if (sec==1) {
						//for (int num_cand=cand_newsec;num_cand<Candidates.size();num_cand++) Candidates.get(num_cand+1).Print();
					//}
					if (!main.constant.isCosmic||(BMT_det.getTile(lay,sec).getClusters().size()<7)) {
					//If we have already some hit in the sector, there are track candidate to check
					
						if (!noHit_yet_sector) {
							for (int clus=0;clus<BMT_det.getTile(lay,sec).getClusters().size();clus++) {
							
								//Here we always test if we have a match by time
								IsAttributed=false;
								for (int num_cand=cand_newsec;num_cand<Candidates.size();num_cand++) {
									//If we have a match in time and will add a new layer
									if (!this.IsLayerCompatible(BMT_det.getTile(lay,sec).getClusters().get(clus+1),Candidates.get(num_cand+1))) {
										TrackCandidate cand=Candidates.get(num_cand+1).DuplicateBMT();
										if (this.IsCompatible(BMT_det.getTile(lay,sec).getClusters().get(clus+1),cand)) {
											cand.addBMT(BMT_det.getTile(lay,sec).getClusters().get(clus+1));
											BufferLayer.add(cand);
											IsAttributed=true;
										}
									}
								
									if (this.IsCompatible(BMT_det.getTile(lay,sec).getClusters().get(clus+1),Candidates.get(num_cand+1))) {
										Candidates.get(num_cand+1).addBMT(BMT_det.getTile(lay,sec).getClusters().get(clus+1));
										IsAttributed=true;
									}
								
								}
							
								if (!IsAttributed) {
									TrackCandidate cand=new TrackCandidate(BMT_det,BST_det);
									cand.addBMT(BMT_det.getTile(lay,sec).getClusters().get(clus+1));
									Candidates.put(Candidates.size()+1, cand);
								}
							
							}
							for (int buf=0;buf<BufferLayer.size();buf++) {
								Candidates.put(Candidates.size()+1, BufferLayer.get(buf));
							}
							BufferLayer.clear();
						}	
				
						//We just enter the sector
						if (noHit_yet_sector) {
							//Create a new Track Candidate for each cluster of first layer
							for (int clus=0;clus<BMT_det.getTile(lay,sec).getClusters().size();clus++) {
								TrackCandidate cand=new TrackCandidate(BMT_det,BST_det);
								cand.addBMT(BMT_det.getTile(lay,sec).getClusters().get(clus+1));
								Candidates.put(Candidates.size()+1, cand);
								noHit_yet_sector=false;
							}
						
						}
					}
				}
			}
			
			//If we want to include SVT, we will try to find the strips compatible with the track candidates built with BMT
			if (main.constant.TrackerType.equals("SVT")||main.constant.TrackerType.equals("CVT")||main.constant.TrackerType.equals("MVT")) {
				for (int lay=6; lay>0;lay--) {
					
					for (int ray=0; ray<Candidates.size();ray++) {
						
							//System.out.println(ray+" "+Candidates.get(ray+1).size()+" "+Candidates.get(ray+1).BSTsize()+" "+Candidates.get(ray+1).IsFittable());
							ArrayList<Integer> sector=BST_det.getGeometry().getSectIntersect(lay, Candidates.get(ray+1).get_VectorTrack(), Candidates.get(ray+1).get_PointTrack());
							int sec_pointTrack=BST_det.getGeometry().findSectorFromAngle(lay, Candidates.get(ray+1).get_PointTrack());
							
							
							//if (sector!=-1){
							for (int hh=0;hh<sector.size();hh++) {
								//for (int sector_stud=sector-delta_sec; sector_stud<=sector+delta_sec; sector_stud++) {
									//int sec=sector_stud%BST_det.getGeometry().getNbModule(lay)+1;
									int sec= sector.get(hh);
									if (main.constant.isCosmic
											||(!main.constant.isCosmic&&(Math.abs(sec-sec_pointTrack)<2||(sec==1&&sec_pointTrack==BST_det.getGeometry().getNbModule(lay))||(sec_pointTrack==1&&sec==BST_det.getGeometry().getNbModule(lay))))) {
										
										Vector3D inter=BST_det.getGeometry().getIntersectWithRay(lay, sec, Candidates.get(ray+1).get_VectorTrack(), Candidates.get(ray+1).get_PointTrack());
									
										if (!Double.isNaN(inter.x())) {
											for (int str=0;str<BST_det.getModule(lay, sec).getClusters().size();str++) {
												double delta=Double.MAX_VALUE;
												if (!main.constant.isCosmic) delta=BST_det.getGeometry().getResidual_line(lay, sec, BST_det.getModule(lay, sec).getClusters().get(str+1).getCentroid() , inter);
												else delta=Math.sqrt((inter.y()-BST_det.getModule(lay, sec).getClusters().get(str+1).getY())*(inter.y()-BST_det.getModule(lay, sec).getClusters().get(str+1).getY())
													+(inter.x()-BST_det.getModule(lay, sec).getClusters().get(str+1).getX())*(inter.x()-BST_det.getModule(lay, sec).getClusters().get(str+1).getX()));
											
													//Going from outside to the inside
													if (Math.abs(delta)<5&&!main.constant.isCosmic) {
														if (Candidates.get(ray+1).BSTsize()!=0) {
															if (Candidates.get(ray+1).getLastBSTLayer()==lay) {
																TrackCandidate cand=Candidates.get(ray+1).DuplicateBST();
																cand.addBST(BST_det.getModule(lay, sec).getClusters().get(str+1));
																BufferLayer.add(cand);
															}
															if (Candidates.get(ray+1).getLastBSTLayer()!=lay) Candidates.get(ray+1).addBST(BST_det.getModule(lay, sec).getClusters().get(str+1));
														}
														if (Candidates.get(ray+1).BSTsize()==0) Candidates.get(ray+1).addBST(BST_det.getModule(lay, sec).getClusters().get(str+1));
													}
													
													//For cosmic, we look all the sector from a same layer.
													if (Math.abs(delta)<20&&main.constant.isCosmic) {
														if (Candidates.get(ray+1).BSTsize()!=0) {
															if (this.NotCosmicSVTCompatible(Candidates.get(ray+1),BST_det.getModule(lay, sec).getClusters().get(str+1))) {
																TrackCandidate cand=Candidates.get(ray+1).DuplicateBST();
																cand.addBST(BST_det.getModule(lay, sec).getClusters().get(str+1));
																BufferLayer.add(cand);
															}
															else Candidates.get(ray+1).addBST(BST_det.getModule(lay, sec).getClusters().get(str+1));
														}
														if (Candidates.get(ray+1).BSTsize()==0) Candidates.get(ray+1).addBST(BST_det.getModule(lay, sec).getClusters().get(str+1));
													}
													
											}
										}
										
								}
							}
					}
					for (int buf=0;buf<BufferLayer.size();buf++) {
						Candidates.put(Candidates.size()+1, BufferLayer.get(buf));
					}
					BufferLayer.clear();
				}
				
				//for (int i=0;i<Candidates.size();i++) {
					//if (Candidates.get(i+1).IsFittable()) Candidates.get(i+1).Print();
				//}
			}
		
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//For cosmic data, no need to overthink the pattern recognition
		
		if (main.constant.isCosmic&&!main.constant.efficiency) {
			
			//We look at track going between sec1 and sec3 of BMT
			int svt_opposite=0;
			ArrayList<Integer> index_fittable_sec2=new ArrayList<Integer>();
			ArrayList<Integer> index_fittable_sec1=new ArrayList<Integer>();
			ArrayList<Integer> index_fittable_sec3=new ArrayList<Integer>();
			for (int track=1;track<Candidates.size()+1;track++) {
				if (Candidates.get(track).get_Nz()>0&&Candidates.get(track).get_Nc()>1&&Candidates.get(track).GetBMTCluster(0).getSector()==2)	index_fittable_sec2.add(track);
				if (Candidates.get(track).get_Nz()>0&&Candidates.get(track).get_Nc()>1&&Candidates.get(track).GetBMTCluster(0).getSector()==1)	index_fittable_sec1.add(track);
				if (Candidates.get(track).get_Nz()>0&&Candidates.get(track).get_Nc()>1&&Candidates.get(track).GetBMTCluster(0).getSector()==3)	index_fittable_sec3.add(track);
			}
			int sector_hit=0;
			if (index_fittable_sec1.size()>0) sector_hit++;
			if (index_fittable_sec2.size()>0) sector_hit++;
			if (index_fittable_sec3.size()>0) sector_hit++;
			
			//We loop over the track candidates
			
			if (sector_hit==1&&index_fittable_sec2.size()>0) {
				for (int lay=1; lay<7;lay++) {
					if (BST_det.getModule(lay, 1).getClusters().size()==1) svt_opposite++;
				}
				if (svt_opposite>3) {
					for (int cand=0;cand<index_fittable_sec2.size();cand++) {
					 for (int lay=1; lay<7;lay++) {
						 if (BST_det.getModule(lay, 1).getClusters().size()==1) Candidates.get(index_fittable_sec2.get(cand)).addBST(BST_det.getModule(lay, 1).getClusters().get(1));
					 }
					}
				}
			}
			
			if (sector_hit==1&&index_fittable_sec1.size()>0) {
				for (int lay=1; lay<7;lay++) {
					if (lay==1||lay==2) svt_opposite+=BST_det.getModule(lay,7).getClusters().size()+BST_det.getModule(lay,8).getClusters().size();
					if (lay==3||lay==4) svt_opposite+=BST_det.getModule(lay,10).getClusters().size()+BST_det.getModule(lay,11).getClusters().size();
					if (lay==5||lay==6) svt_opposite+=BST_det.getModule(lay,13).getClusters().size();
				}
				
				if (svt_opposite>3) {
				  for (int cand=0;cand<index_fittable_sec1.size();cand++) {
				  if (BST_det.getModule(1, 7).getClusters().size()==1&&BST_det.getModule(1, 8).getClusters().size()==0) Candidates.get(index_fittable_sec1.get(cand)).addBST(BST_det.getModule(1, 7).getClusters().get(1));
				  if (BST_det.getModule(1, 7).getClusters().size()==0&&BST_det.getModule(1, 8).getClusters().size()==1) Candidates.get(index_fittable_sec1.get(cand)).addBST(BST_det.getModule(1, 8).getClusters().get(1));
				  
				  if (BST_det.getModule(2, 7).getClusters().size()==1&&BST_det.getModule(2, 8).getClusters().size()==0) Candidates.get(index_fittable_sec1.get(cand)).addBST(BST_det.getModule(2, 7).getClusters().get(1));
				  if (BST_det.getModule(2, 7).getClusters().size()==0&&BST_det.getModule(2, 8).getClusters().size()==1) Candidates.get(index_fittable_sec1.get(cand)).addBST(BST_det.getModule(2, 8).getClusters().get(1));
				  
				  if (BST_det.getModule(3, 10).getClusters().size()==1&&BST_det.getModule(3, 11).getClusters().size()==0) Candidates.get(index_fittable_sec1.get(cand)).addBST(BST_det.getModule(3, 10).getClusters().get(1));
				  if (BST_det.getModule(3, 10).getClusters().size()==0&&BST_det.getModule(3, 11).getClusters().size()==1) Candidates.get(index_fittable_sec1.get(cand)).addBST(BST_det.getModule(3, 11).getClusters().get(1));
				  
				  if (BST_det.getModule(4, 10).getClusters().size()==1&&BST_det.getModule(4, 11).getClusters().size()==0) Candidates.get(index_fittable_sec1.get(cand)).addBST(BST_det.getModule(4, 10).getClusters().get(1));
				  if (BST_det.getModule(4, 10).getClusters().size()==0&&BST_det.getModule(4, 11).getClusters().size()==1) Candidates.get(index_fittable_sec1.get(cand)).addBST(BST_det.getModule(4, 11).getClusters().get(1));
				  
				  if (BST_det.getModule(5, 13).getClusters().size()==1) Candidates.get(index_fittable_sec1.get(cand)).addBST(BST_det.getModule(5, 13).getClusters().get(1));
				  if (BST_det.getModule(6, 13).getClusters().size()==1) Candidates.get(index_fittable_sec1.get(cand)).addBST(BST_det.getModule(6, 13).getClusters().get(1));
				  }
				}
			}
			
			if (sector_hit==1&&index_fittable_sec3.size()>0) {
				for (int lay=1; lay<7;lay++) {
					if (lay==1||lay==2) svt_opposite+=BST_det.getModule(lay,4).getClusters().size()+BST_det.getModule(lay,5).getClusters().size();
					if (lay==3||lay==4) svt_opposite+=BST_det.getModule(lay,5).getClusters().size()+BST_det.getModule(lay,6).getClusters().size();
					if (lay==5||lay==6) svt_opposite+=BST_det.getModule(lay,7).getClusters().size();
				}
				
				if (svt_opposite>3) {
				  for (int cand=0;cand<index_fittable_sec3.size();cand++) {
				  if (BST_det.getModule(1, 4).getClusters().size()==1&&BST_det.getModule(1, 5).getClusters().size()==0) Candidates.get(index_fittable_sec3.get(cand)).addBST(BST_det.getModule(1, 4).getClusters().get(1));
				  if (BST_det.getModule(1, 4).getClusters().size()==0&&BST_det.getModule(1, 5).getClusters().size()==1) Candidates.get(index_fittable_sec3.get(cand)).addBST(BST_det.getModule(1, 5).getClusters().get(1));
				  
				  if (BST_det.getModule(2, 4).getClusters().size()==1&&BST_det.getModule(2, 5).getClusters().size()==0) Candidates.get(index_fittable_sec3.get(cand)).addBST(BST_det.getModule(2, 4).getClusters().get(1));
				  if (BST_det.getModule(2, 4).getClusters().size()==0&&BST_det.getModule(2, 5).getClusters().size()==1) Candidates.get(index_fittable_sec3.get(cand)).addBST(BST_det.getModule(2, 5).getClusters().get(1));
				  
				  if (BST_det.getModule(3, 5).getClusters().size()==1&&BST_det.getModule(3, 6).getClusters().size()==0) Candidates.get(index_fittable_sec3.get(cand)).addBST(BST_det.getModule(3, 5).getClusters().get(1));
				  if (BST_det.getModule(3, 5).getClusters().size()==0&&BST_det.getModule(3, 6).getClusters().size()==1) Candidates.get(index_fittable_sec3.get(cand)).addBST(BST_det.getModule(3, 6).getClusters().get(1));
				  
				  if (BST_det.getModule(4, 5).getClusters().size()==1&&BST_det.getModule(4, 6).getClusters().size()==0) Candidates.get(index_fittable_sec3.get(cand)).addBST(BST_det.getModule(4, 5).getClusters().get(1));
				  if (BST_det.getModule(4, 5).getClusters().size()==0&&BST_det.getModule(4, 6).getClusters().size()==1) Candidates.get(index_fittable_sec3.get(cand)).addBST(BST_det.getModule(4, 6).getClusters().get(1));
				  
				  if (BST_det.getModule(5, 7).getClusters().size()==1) Candidates.get(index_fittable_sec3.get(cand)).addBST(BST_det.getModule(5, 7).getClusters().get(1));
				  if (BST_det.getModule(6, 7).getClusters().size()==1) Candidates.get(index_fittable_sec3.get(cand)).addBST(BST_det.getModule(6, 7).getClusters().get(1));
				  }
				}
			}
			
			//We loop over the track candidates
			if (sector_hit>1&&index_fittable_sec2.size()<10&&index_fittable_sec1.size()<10&&index_fittable_sec3.size()<10) {
				
				//Merge sec2 track to sec1 and then sec3
				for (int track_sec2=0;track_sec2<index_fittable_sec2.size();track_sec2++) {
					for (int track_sec1=0;track_sec1<index_fittable_sec1.size();track_sec1++) {
						Candidates.put(Candidates.size()+1,Candidates.get(index_fittable_sec2.get(track_sec2)).Merge(Candidates.get(index_fittable_sec1.get(track_sec1))));
						Candidates.get(Candidates.size()).set_VectorTrack(Candidates.get(index_fittable_sec2.get(track_sec2)).get_VectorTrack());
						Candidates.get(Candidates.size()).set_PointTrack(Candidates.get(index_fittable_sec2.get(track_sec2)).get_PointTrack());
						Candidates.get(Candidates.size()).set_PhiSeed(Candidates.get(index_fittable_sec2.get(track_sec2)).getPhiSeed());
						Candidates.get(Candidates.size()).set_ThetaSeed(Candidates.get(index_fittable_sec2.get(track_sec2)).getThetaSeed());
						
					}
					for (int track_sec3=0;track_sec3<index_fittable_sec3.size();track_sec3++) {
						Candidates.put(Candidates.size()+1,Candidates.get(index_fittable_sec2.get(track_sec2)).Merge(Candidates.get(index_fittable_sec3.get(track_sec3))));
						Candidates.get(Candidates.size()).set_VectorTrack(Candidates.get(index_fittable_sec2.get(track_sec2)).get_VectorTrack());
						Candidates.get(Candidates.size()).set_PointTrack(Candidates.get(index_fittable_sec2.get(track_sec2)).get_PointTrack());
						Candidates.get(Candidates.size()).set_PhiSeed(Candidates.get(index_fittable_sec2.get(track_sec2)).getPhiSeed());
						Candidates.get(Candidates.size()).set_ThetaSeed(Candidates.get(index_fittable_sec2.get(track_sec2)).getThetaSeed());
						
					}
				}
				//Merge sec1 and sec3 Track
				for (int track_sec3=0;track_sec3<index_fittable_sec3.size();track_sec3++) {
					for (int track_sec1=0;track_sec1<index_fittable_sec1.size();track_sec1++) {
						Candidates.put(Candidates.size()+1,Candidates.get(index_fittable_sec3.get(track_sec3)).Merge(Candidates.get(index_fittable_sec1.get(track_sec1))));
						Candidates.get(Candidates.size()).set_VectorTrack(Candidates.get(index_fittable_sec3.get(track_sec3)).get_VectorTrack());
						Candidates.get(Candidates.size()).set_PointTrack(Candidates.get(index_fittable_sec3.get(track_sec3)).get_PointTrack());
						Candidates.get(Candidates.size()).set_PhiSeed(Candidates.get(index_fittable_sec3.get(track_sec3)).getPhiSeed());
						Candidates.get(Candidates.size()).set_ThetaSeed(Candidates.get(index_fittable_sec3.get(track_sec3)).getThetaSeed());
					}
				}
				
			}
		}
	}
	
	public boolean IsCompatible(BMT_struct.Cluster clus, TrackCandidate ToBuild) {
		boolean test_val=false;
		if (this.IsTimeCompatible(clus, ToBuild)) {
			if (this.IsLayerCompatible(clus, ToBuild)) {
				if (this.IsSpatialCompatible(clus, ToBuild)) {
					test_val=true;
				}
			}
		}
		return test_val;
	}
	
	public boolean AllExceptLayerCompatible(BMT_struct.Cluster clus, TrackCandidate ToBuild) {
		boolean test_val=false;
		if (this.IsTimeCompatible(clus, ToBuild)) {
		 if (this.IsSpatialCompatible(clus, ToBuild)) {
			if (!this.IsLayerCompatible(clus, ToBuild)) {
					test_val=true;
				}
			}
		}
		return test_val;
	}
	
	public boolean IsTimeCompatible(BMT_struct.Cluster clus, TrackCandidate ToBuild) {
		//Test if not on the same layer... otherwise need to duplicate track candidate
		boolean test_val=false;
		//if (clus.getSector()==2) System.out.println(clus.getT_min()+" "+ToBuild.GetTimeLastHit());
		if (Math.abs(clus.getT_min()-ToBuild.GetTimeLastHit())<time_match) test_val=true;
		return test_val;
	}
	
	public boolean IsLayerCompatible(BMT_struct.Cluster clus, TrackCandidate ToBuild) {
		//Test if not on the same layer... otherwise need to duplicate track candidate
		boolean test_val=false;
		if (clus.getLayer()!=ToBuild.GetLayerLastHit()) test_val=true;
		return test_val;
	}
	
	public boolean IsSpatialCompatible(BMT_struct.Cluster clus, TrackCandidate ToBuild) {
		//Test if not on the same layer... otherwise need to duplicate track candidate
		boolean test_val=false;
		
		if (Double.isNaN(clus.getZ())) {
			if (ToBuild.get_Nz()==0) {
				test_val=true;
			}
			if (ToBuild.get_Nz()>0) {
				Vector3D meas=new Vector3D(ToBuild.getLastX()-clus.getX(),ToBuild.getLastY()-clus.getY(),0);
				Vector3D tr_phi=new Vector3D(Math.cos(ToBuild.getPhiSeed()),Math.sin(ToBuild.getPhiSeed()),0);
				double angle_meas=meas.angle(tr_phi);
				if (angle_meas<ToBuild.getPhiTolerance()
						||Math.abs(angle_meas-Math.PI)<ToBuild.getPhiTolerance()
							||Math.abs(angle_meas-2*Math.PI)<ToBuild.getPhiTolerance()) test_val=true;
			}
		}
		if (!Double.isNaN(clus.getZ())) {
			if (ToBuild.get_Nc()==0) {	
				test_val=true;
				
			}
			if (ToBuild.get_Nc()>0) {
				double Theta_meas=Math.acos((ToBuild.getLastZ()-clus.getZ())/Math.sqrt((ToBuild.getLastZ()-clus.getZ())*(ToBuild.getLastZ()-clus.getZ())
						+(clus.getRadius()-ToBuild.getLastR())*(clus.getRadius()-ToBuild.getLastR())));
				if (Theta_meas>ToBuild.getThetaMin()&&Theta_meas<ToBuild.getThetaMax()) test_val=true;
			}
		}
		return test_val;
	}
	
	public boolean NotCosmicSVTCompatible(TrackCandidate ToBuild, BST_struct.Cluster clus) {
		boolean notCompatible=false;
		String clus_side="down";
		String last_side="down";
		if (clus.getLayer()!=ToBuild.getLastBSTLayer()) return notCompatible;
		if ((clus.getLayer()==5||clus.getLayer()==6)&&clus.getSector()>=6&&clus.getSector()<=14) clus_side="up";
		if ((clus.getLayer()==3||clus.getLayer()==4)&&clus.getSector()>=5&&clus.getSector()<=11) clus_side="up";
		if ((clus.getLayer()==1||clus.getLayer()==2)&&clus.getSector()>=4&&clus.getSector()<=8) clus_side="up";
		
		if ((ToBuild.GetBSTCluster(ToBuild.BSTsize()-1).getLayer()==5||ToBuild.GetBSTCluster(ToBuild.BSTsize()-1).getLayer()==6)&&ToBuild.GetBSTCluster(ToBuild.BSTsize()-1).getSector()>=6&&ToBuild.GetBSTCluster(ToBuild.BSTsize()-1).getSector()<=14) last_side="up";
		if ((ToBuild.GetBSTCluster(ToBuild.BSTsize()-1).getLayer()==3||ToBuild.GetBSTCluster(ToBuild.BSTsize()-1).getLayer()==4)&&ToBuild.GetBSTCluster(ToBuild.BSTsize()-1).getSector()>=5&&ToBuild.GetBSTCluster(ToBuild.BSTsize()-1).getSector()<=11) last_side="up";
		if ((ToBuild.GetBSTCluster(ToBuild.BSTsize()-1).getLayer()==1||ToBuild.GetBSTCluster(ToBuild.BSTsize()-1).getLayer()==2)&&ToBuild.GetBSTCluster(ToBuild.BSTsize()-1).getSector()>=4&&ToBuild.GetBSTCluster(ToBuild.BSTsize()-1).getSector()<=8) last_side="up";
		
		if (last_side.equals(clus_side)) notCompatible=true;
		return notCompatible;
	}
	
	public void FetchTrack() {
		Fitter myfit=new Fitter();
		int NbFittable=0;
		for (int i=0;i<Candidates.size();i++) {
			if (Candidates.get(i+1).IsFittable()) NbFittable++;
			
		}
		 if (NbFittable<5||!main.constant.isCosmic) myfit.CVTStraightTrack(BMT_det, BST_det, Candidates);
		}
	
}	
	