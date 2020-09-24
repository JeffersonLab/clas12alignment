package Alignment;

import org.freehep.math.minuit.FCNBase;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo3.Hipo3DataSource;
import Trajectory.StraightLine;
import BMT_struct.*;
import BST_struct.*;
import org.jlab.io.base.DataBank;
import org.jlab.geom.prim.Vector3D;

public class FCNChi2 implements FCNBase {

	Hipo3DataSource[] reader;
	Barrel BMT;
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
		  
		  if (layer>6) {
			  //First three parameters are Rotations
			  BMT_geo.Constants.setRx(layer-6, sector, par[0]);
			  BMT_geo.Constants.setRy(layer-6, sector, par[1]);
			  BMT_geo.Constants.setRz(layer-6, sector, par[2]);
			  //Last three parameters are rotations
			  BMT_geo.Constants.setCx(layer-6, sector, par[3]);
			  BMT_geo.Constants.setCy(layer-6, sector, par[4]);
			  BMT_geo.Constants.setCz(layer-6, sector, par[5]);
		  }
		  else {
			  BST.getGeometry().setRx(layer, sector, par[0]);
			  BST.getGeometry().setRy(layer, sector, par[1]);
			  BST.getGeometry().setRz(layer, sector, par[2]);
			  BST.getGeometry().setCx(layer, sector, par[3]);
			  BST.getGeometry().setCy(layer, sector, par[4]);
			  BST.getGeometry().setCz(layer, sector, par[5]);
			  
			  BST.getGeometry().setRx(layer+1, sector, par[0]);
			  BST.getGeometry().setRy(layer+1, sector, par[1]);
			  BST.getGeometry().setRz(layer+1, sector, par[2]);
			  BST.getGeometry().setCx(layer+1, sector, par[3]);
			  BST.getGeometry().setCy(layer+1, sector, par[4]);
			  BST.getGeometry().setCz(layer+1, sector, par[5]);
		  }
		
		  System.out.println(par[0]+" "+par[1]+" "+par[2]+" "+par[3]+" "+par[4]+" "+par[5]);
		  for (int infile=0; infile<reader.length; infile++) {	
			 
			  for (int i=0; i<reader[infile].getSize();i++) {
				  DataEvent event = reader[infile].gotoEvent(i);
				
				  if(event.hasBank("CVTRec::Cosmics")&&event.hasBank("CVTRec::Trajectory")) {
					  StraightLine ray=new StraightLine();
					  DataBank raybank=event.getBank("CVTRec::Cosmics");
					  DataBank Trajbank=event.getBank("CVTRec::Trajectory");
					  DataBank BMTClusbank=event.getBank("BMTRec::Clusters");
					  DataBank BSTClusbank=event.getBank("BSTRec::Clusters");
					  for (int nray=0;nray<raybank.rows();nray++) {
						 					  
						  //if (raybank.getShort("ndf",nray)>=1) {//if (raybank.getShort("ndf",nray)>=3) {
			    			
							  ray.setSlope_XYZ(raybank.getFloat("trkline_yx_slope", nray), 1, raybank.getFloat("trkline_yz_slope", nray));
							  ray.setPoint_XYZ(raybank.getFloat("trkline_yx_interc", nray)*10, 0, raybank.getFloat("trkline_yz_interc", nray)*10);
			    		
							  //Check the calcCentroid of the track and make sure the track went through the tile we want to align
							  for (int npt=0; npt<Trajbank.rows(); npt++) {
								   if (raybank.getShort("ID",nray)==Trajbank.getShort("ID",npt)&&
										  (layer==Trajbank.getByte("LayerTrackIntersPlane",npt)||((layer+1)==Trajbank.getByte("LayerTrackIntersPlane",npt)&&layer<7))&&
										  	sector==Trajbank.getByte("SectorTrackIntersPlane",npt)) {
									  		ClusterExpect=-20;
			    							int StudiedLayer= (int) Trajbank.getByte("LayerTrackIntersPlane",npt);
			    							ClusterExpect=Trajbank.getFloat("CalcCentroidStrip",npt);
			    										    		
			    							//Since the track is supposed to have crossed the tile, let's find the corresponding cluster
			    							if (StudiedLayer>6) {
			    								for (int clus=0; clus<BMTClusbank.rows(); clus++) {
			    									if (raybank.getShort("ID",nray)==BMTClusbank.getShort("trkID",clus)&&(StudiedLayer-6)==BMTClusbank.getByte("layer",clus)&&sector==BMTClusbank.getByte("sector",clus)){
			    										if (Math.abs(ClusterExpect-BMTClusbank.getFloat("centroid",clus))<DeltaCentroid) {
			    							
			    											BMT_struct.Cluster Clus=BMT.RecreateCluster(StudiedLayer-6,sector,BMTClusbank.getFloat("centroid",clus));
			    							
			    											val+=Math.pow(BMT.getGeometry().getResidual_line(Clus,ray.getSlope(),ray.getPoint())/Clus.getErr(),2);
			    											continue;
			    										}
			    									}
			    								}
			    							}
			    							
			    							if (StudiedLayer<=6) {
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
			  }
		  
		  System.out.println(val);
	      return val;
	   }
	
	public void SetDetectorToAlign(Barrel BMT_det, Barrel_SVT BST_det, Hipo3DataSource ReadFile[], int lay, int sec) {
		reader= ReadFile;
		BMT=BMT_det;
		BST=BST_det;
		layer=lay;
		sector=sec;
	}

}
