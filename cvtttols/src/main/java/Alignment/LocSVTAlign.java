package Alignment;

import org.freehep.math.minuit.FCNBase;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo3.Hipo3DataSource;
import Trajectory.StraightLine;
import BMT_struct.*;
import BST_struct.*;
import org.jlab.io.base.DataBank;
import org.jlab.geom.prim.Vector3D;

public class LocSVTAlign implements FCNBase {

	Hipo3DataSource[] reader;
	Barrel_SVT BST;
	int layer=-1;
	int sector=-1;
	
	public double valueOf(double[] par)
	   {
		
		  double val=0;	  
		  int count=0;
		  //Look if track is supposed to 
		  boolean ThroughTile=false;
		  float ClusterExpect=-20;
		  
		  //Check is cluster is compatible with expected intercept with track
		  float DeltaCentroid=10;
		  
		  BST.getGeometry().setLocTx(layer, sector, par[0]);
		 		
		  System.out.println(par[0]);
		  for (int infile=0; infile<reader.length; infile++) {	
			 
			  for (int i=0; i<reader[infile].getSize();i++) {
				  DataEvent event = reader[infile].gotoEvent(i);
				
				  if(event.hasBank("CVTRec::Cosmics")&&event.hasBank("CVTRec::Trajectory")) {
					  StraightLine ray=new StraightLine();
					  DataBank raybank=event.getBank("CVTRec::Cosmics");
					  DataBank Trajbank=event.getBank("CVTRec::Trajectory");
					  DataBank BSTClusbank=event.getBank("BSTRec::Clusters");
					  for (int nray=0;nray<raybank.rows();nray++) {
								    			
							  ray.setSlope_XYZ(raybank.getFloat("trkline_yx_slope", nray), 1, raybank.getFloat("trkline_yz_slope", nray));
							  ray.setPoint_XYZ(raybank.getFloat("trkline_yx_interc", nray)*10, 0, raybank.getFloat("trkline_yz_interc", nray)*10);
			    		
							  //Check the calcCentroid of the track and make sure the track went through the tile we want to align
							  for (int npt=0; npt<Trajbank.rows(); npt++) {
								   if (raybank.getShort("ID",nray)==Trajbank.getShort("ID",npt)&&
										  (layer==Trajbank.getByte("LayerTrackIntersPlane",npt))&&//||((layer+1)==Trajbank.getByte("LayerTrackIntersPlane",npt)&&layer<7))&&
										  	sector==Trajbank.getByte("SectorTrackIntersPlane",npt)) {
									  		ClusterExpect=-20;
			    							int StudiedLayer= (int) Trajbank.getByte("LayerTrackIntersPlane",npt);
			    							ClusterExpect=Trajbank.getFloat("CalcCentroidStrip",npt);
			    										    		
			    							for (int clus=0; clus<BSTClusbank.rows(); clus++) {
			    									
			    								if (raybank.getShort("ID",nray)==BSTClusbank.getShort("trkID",clus)&&StudiedLayer==BSTClusbank.getByte("layer",clus)&&sector==BSTClusbank.getByte("sector",clus)){
			    									if (Math.abs(ClusterExpect-BSTClusbank.getFloat("centroid",clus))<DeltaCentroid) {
			    										BST_struct.Cluster Clus=BST.RecreateCluster(StudiedLayer,sector,BSTClusbank.getFloat("centroid",clus));
			    										Vector3D inter=BST.getGeometry().getIntersectWithRay(Clus.getLayer(), Clus.getSector(), ray.getSlope(), ray.getPoint());
			    										if (!Double.isNaN(inter.x())) val+=Math.pow(BST.getGeometry().getResidual_line(Clus.getLayer(),Clus.getSector(),Clus.getCentroid(),inter)
			    												/BST.getGeometry().getSingleStripResolution(Clus.getLayer(), (int)Clus.getCentroid(), inter.z()),2);
			    										continue;
			    												
			    									}	
			    								}
			    							}
							  }
						  }
					  }
				  }
			  }
		  } 
		  System.out.println(val);
	      return val;
	   }
	
	public void SetLocToAlign(Barrel_SVT BST_det, Hipo3DataSource ReadFile[], int lay, int sec) {
		reader= ReadFile;
		BST=BST_det;
		layer=lay;
		sector=sec;
	}

}
