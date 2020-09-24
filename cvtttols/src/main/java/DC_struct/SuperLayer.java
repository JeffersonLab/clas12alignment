package DC_struct;

import java.util.ArrayList;
import java.util.HashMap;

public class SuperLayer {
	//0 is upstream SL, and 1 downstream
	public int region;
	public int SuperLayer;
	public Layer[] StackLayer= new Layer[6];
	ArrayList<Segment> segmap;
	public double DeltaCluster; //Authorized to associate to layer from one to the next if DeltaCluster<1.5
	public double CellSize;
	ArrayList<Segment> BufferLayer;
	public int sector;
	
	public SuperLayer(int Sector, int SL) {
		region=(SL-1)/2+1;
		SuperLayer=SL;
		segmap = new ArrayList<Segment>();
		BufferLayer = new ArrayList<Segment>();
		
		for (int lay=0; lay<6;lay++) {
			StackLayer[lay]=new Layer(lay+1);
			
		}
		DeltaCluster=1.5;
		sector=Sector;
		if (SL==1||SL==2) CellSize=1.3;//cm
		if (SL==3||SL==4) CellSize=2.;//cm
		if (SL==5||SL==6) CellSize=3.;//cm
	}
	
	public Layer getLayer(int lay) {
		return StackLayer[lay-1];
	}
	
	public int getSuperLayerNbHits() {
		int nHit=0;
		for (int i=0; i<6;i++) {
			nHit+=StackLayer[i].getNbLayerHit();
		}
		return nHit;
	}
	
	public double getCellSize() {
		return CellSize;
	}
	
	public void MakeSegment() {
		boolean IsAttributed=true;
		boolean noHit_yet_SL=true;
			
		for (int lay=6; lay>0;lay--) {
			this.getLayer(lay).DoClustering();
			if (this.getLayer(lay).getClusterList().size()<20) {
				//If we have already some hit in the sector, there are track candidate to check
				//cand_newlay=segmap.size();
				if (!noHit_yet_SL) {
					for (int clus=0;clus<this.getLayer(lay).getClusterList().size();clus++) {
						//Here we always test if we have a match by time
						IsAttributed=false;
						for (int num_seg=0;num_seg<segmap.size();num_seg++) {
							//If we have a match in time and will add a new layer
							if (this.IsCompatible(this.getLayer(lay).getClusterList().get(clus+1),segmap.get(num_seg))&&
									!this.IsLayerCompatible(this.getLayer(lay).getClusterList().get(clus+1),segmap.get(num_seg))) {
								Segment seg=segmap.get(num_seg).Duplicate();
								//if (this.IsCompatible(get(clus+1),cand)) { /// Commented because it might be useful to deal with time info
									seg.addCluster(this.getLayer(lay).getClusterList().get(clus+1));
									BufferLayer.add(seg);
									IsAttributed=true;
								//}
							}
					
							if (this.IsCompatible(this.getLayer(lay).getClusterList().get(clus+1),segmap.get(num_seg))&&
									this.IsLayerCompatible(this.getLayer(lay).getClusterList().get(clus+1),segmap.get(num_seg))) {
								segmap.get(num_seg).addCluster(this.getLayer(lay).getClusterList().get(clus+1));
								IsAttributed=true;
							}
					
						}
				
						if (!IsAttributed) {
							Segment seg=new Segment(sector, SuperLayer);
							seg.addCluster(this.getLayer(lay).getClusterList().get(clus+1));
							segmap.add(seg);
						}
				
					}
					for (int buf=0;buf<BufferLayer.size();buf++) {
						segmap.add(BufferLayer.get(buf));
					}
					BufferLayer.clear();
				}	
	
				//We just enter the sector
				if (noHit_yet_SL) {
					//Create a new Track Candidate for each cluster of first layer
					for (int clus=0;clus<this.getLayer(lay).getClusterList().size();clus++) {
						Segment cand=new Segment(sector, SuperLayer);
						cand.addCluster(this.getLayer(lay).getClusterList().get(clus+1));
						segmap.add(cand);
						noHit_yet_SL=false;
					}
			
				}
			}
		
	
		}
		
	}
	
	public boolean IsCompatible(DC_struct.Cluster clus,Segment seg) {
		boolean Compatible=true;
		if (Math.abs(clus.getCentroid()-seg.getLastCentroid())>DeltaCluster*Math.abs(clus.getLayer()-seg.getLayerLastEntry())) Compatible=false;
		return Compatible;
	}
	
	public boolean IsLayerCompatible(DC_struct.Cluster clus,Segment seg) {
		boolean Compatible=true;
		
		if (Math.abs(clus.getLayer()-seg.getLayerLastEntry())==0) Compatible=false;
		//if (Math.abs(clus.getLayer()-seg.getLayerLastEntry())>3) Compatible=false;
		return Compatible;
	}
	
	public ArrayList<Segment> getSegments(){
		return segmap;
	}
	
	public void clear() {
		for (int lay=0; lay<6;lay++) {
			StackLayer[lay].clear();
		}
		segmap.clear();
	}
	
	public boolean hasGoodSegments() {
		boolean good=false;
		for (int el=0;el<segmap.size();el++) {
			if (segmap.get(el).isGoodSLSegment()) good=true;
		}
		return good;
	}

}
