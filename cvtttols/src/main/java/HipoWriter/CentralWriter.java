package HipoWriter;

import org.jlab.jnp.hipo4.data.*;
import org.jlab.jnp.hipo4.io.HipoWriter;

import BST_struct.*;
import Particles.*;
import BMT_struct.*;
import TrackFinder.*;
import java.util.*;
import org.jlab.geom.prim.Vector3D;
import org.jlab.utils.system.ClasUtilsFile;

public class CentralWriter {
	HipoWriter writer = new HipoWriter();;
	
	
	public CentralWriter() {
                
                String dir = ClasUtilsFile.getResourceDir("CLAS12DIR", "etc/bankdefs/hipo4");
                writer.setCompressionType(2);
                writer.getSchemaFactory().initFromDirectory(dir);
            
//		factory.addSchema(new Schema("{20125,BMTRec::Crosses}[1,ID,SHORT][2,sector,BYTE][3,region,BYTE][4,x,FLOAT][5,y,FLOAT][6,z,FLOAT]"
//				+ "[7,err_x,FLOAT][8,err_y,FLOAT][9,err_z,FLOAT][10,ux,FLOAT][11,uy,FLOAT][12,uz,FLOAT][13,Cluster1_ID,SHORT][14,Cluster2_ID,SHORT][15,trkID,SHORT]"));
//		
//		factory.addSchema(new Schema("{20225,BSTRec::Crosses}[1,ID,SHORT][2,sector,BYTE][3,region,BYTE][4,x,FLOAT][5,y,FLOAT][6,z,FLOAT]"
//				+ "[7,err_x,FLOAT][8,err_y,FLOAT][9,err_z,FLOAT][10,ux,FLOAT][11,uy,FLOAT][12,uz,FLOAT][13,Cluster1_ID,SHORT][14,Cluster2_ID,SHORT][15,trkID,SHORT]"));
//		
//		factory.addSchema(new Schema("{20221,BSTRec::Hits}[1,ID,SHORT][2,sector,BYTE][3,layer,BYTE][4,strip,INT][5,fitResidual,FLOAT][6,trkingStat,INT]"
//				+ "[7,clusterID,SHORT][8,trkID,SHORT]"));
//		
//		factory.addSchema(new Schema("{20222,BSTRec::Clusters}[1,ID,SHORT][2,sector,BYTE][3,layer,BYTE][4,size,SHORT][5,Etot,FLOAT][6,seedE,FLOAT][7,seedStrip,INT][8,centroid,FLOAT]"
//				+ "[9,centroidResidual,FLOAT][10,seedResidual,FLOAT][11,Hit1_ID,SHORT][12,Hit2_ID,SHORT][13,Hit3_ID,SHORT][14,Hit4_ID,SHORT][15,Hit5_ID,SHORT][16,trkID,SHORT]"));
//		
//		factory.addSchema(new Schema("{20122,BMTRec::Clusters}[1,ID,SHORT][2,sector,BYTE][3,layer,BYTE][4,size,SHORT][5,Etot,FLOAT][6,seedE,FLOAT][7,seedStrip,INT][8,centroid,FLOAT]"
//				+ "[9,centroidResidual,FLOAT][10,seedResidual,FLOAT][11,Hit1_ID,SHORT][12,Hit2_ID,SHORT][13,Hit3_ID,SHORT][14,Hit4_ID,SHORT][15,Hit5_ID,SHORT][16,trkID,SHORT][17,Tmin,FLOAT][18,Tmax,FLOAT]"));
//		
//		factory.addSchema(new Schema("{3,MC::Particle}[1,pid,SHORT][2,px,FLOAT][3,py,FLOAT][4,pz,FLOAT][5,vx,FLOAT][6,vy,FLOAT][7,vz,FLOAT][8,vt,FLOAT]"));
//		
//		factory.addSchema(new Schema("{20528,CVTRec::Cosmics}[1,ID,SHORT][2,trkline_yx_slope,FLOAT][3,trkline_yx_interc,FLOAT][4,trkline_yz_slope,FLOAT][5,trkline_yz_interc,FLOAT][6,theta,FLOAT][7,phi,FLOAT]"
//				+ "[8,chi2,FLOAT][9,ndf,SHORT][10,Cross1_ID,SHORT][11,Cross2_ID,SHORT][12,Cross3_ID,SHORT][13,Cross4_ID,SHORT][14,Cross5_ID,SHORT][14,Cross5_ID,SHORT][15,Cross6_ID,SHORT]"
//				+ "[16,Cross7_ID,SHORT][17,Cross8_ID,SHORT][18,Cross9_ID,SHORT][19,Cross10_ID,SHORT][20,Cross11_ID,SHORT][21,Cross12_ID,SHORT][22,Cross13_ID,SHORT][23,Cross14_ID,SHORT]"
//				+ "[24,Cross15_ID,SHORT][25,Cross16_ID,SHORT][26,Cross17_ID,SHORT][27,Cross18_ID,SHORT][28,NbBSTHits,SHORT][29,NbBMTHits,SHORT]"));
//		
//		factory.addSchema(new Schema("{20529,CVTRec::Trajectory}[1,ID,SHORT][2,LayerTrackIntersPlane,BYTE][3,SectorTrackIntersPlane,BYTE][4,XtrackIntersPlane,FLOAT][5,YtrackIntersPlane,FLOAT][6,ZtrackIntersPlane,FLOAT]"
//				+ "[7,PhitrackIntersPlane,FLOAT][8,ThetatrackIntersPlane,FLOAT][9,trkToMPlnAngl,FLOAT],[10,CalcCentroidStrip,FLOAT]"));
//		
//		factory.addSchema(new Schema("{11,RUN::config}[1,run,INT][2,event,INT][3,unixtime,INT][4,trigger,LONG][5,timestamp,LONG][6,type,BYTE][7,mode,BYTE][8,torus,FLOAT][9,solenoid,FLOAT]"));
//		
//		factory.addSchema(new Schema("{20211,BST::adc}[1,sector,BYTE][2,layer,BYTE][3,component,SHORT][4,order,BYTE][5,ADC,INT][6,time,FLOAT][7,ped,SHORT][8,timestamp,LONG]"));
//		factory.addSchema(new Schema("{20111,BMT::adc}[1,sector,BYTE][2,layer,BYTE][3,component,SHORT][4,order,BYTE][5,ADC,INT][6,time,FLOAT][7,ped,SHORT][8,integral,INT][9,timestamp,LONG]"));
//		 writer.appendSchemaFactory(factory);
		 		 
	}
	
	public void WriteEvent(int eventnb, Barrel BMT ,Barrel_SVT BST ,ArrayList<TrackCandidate> candidates, ParticleEvent MCParticles) {
		Event event= new Event();
		 
		 event.write(this.fillCosmicRecBank(candidates));
		 event.write(this.fillCosmicTrajBank(BMT,BST,candidates));
		 event.write(this.fillBMTCrossesBank(BMT));
		 event.write(this.fillBSTCrossesBank(BST));
		 event.write(this.fillBSTHitsBank(BST));
		 event.write(this.fillBSTClusterBank(BST));
		 event.write(this.fillBMTClusterBank(BMT));
		 if (main.constant.isMC) event.write(this.fillMCBank(MCParticles));
		 event.write(this.fillRunConfig(eventnb));
		 event.write(this.fillBSTADCbank(BST));
		 event.write(this.fillBMTADCbank(BMT));
		 writer.addEvent(event );
	}
	
	public void setOuputFileName(String output){
		writer.open(output);
	}
	
	public Bank fillBSTADCbank(Barrel_SVT BST) {
		int groupsize=BST.getNbHits();
		Bank bank = new Bank(writer.getSchemaFactory().getSchema("BST::adc"),groupsize);
		int index=0;
		for (int lay=1; lay<7; lay++) {
			for (int sec=1; sec<19; sec++) {
				for (int j = 0; j < BST.getModule(lay,sec).getClusters().size(); j++) {
					for (int str=0;str<BST.getModule(lay,sec).getClusters().get(j+1).getListOfHits().size();str++) {
						int strip=BST.getModule(lay,sec).getClusters().get(j+1).getListOfHits().get(str);
						bank.putByte("sector", index, (byte) sec);
                                                bank.putByte("layer", index, (byte) lay);
						bank.putShort("component", index, (short) strip);
						bank.putInt("ADC", index, BST.getModule(lay,sec).getHits().get(strip).getADC());
						bank.putFloat("time", index, BST.getModule(lay,sec).getHits().get(strip).getTime());
						index++;
					}
				}					
			}
		}
		
		return bank;
	}
	
	public Bank fillBMTADCbank(Barrel BMT) {
		int groupsize=BMT.getNbHits();
		Bank bank = new Bank(writer.getSchemaFactory().getSchema("BMT::adc"),groupsize);
		
		int index=0;
		for (int lay=0; lay<6; lay++) {
			for (int sec=0; sec<3; sec++) {
				for (int j = 0; j < BMT.getTile(lay,sec).getClusters().size(); j++) {
					for (int str=0;str<BMT.getTile(lay,sec).getClusters().get(j+1).getListOfHits().size();str++) {
						int strip=BMT.getTile(lay,sec).getClusters().get(j+1).getListOfHits().get(str);
						bank.putByte("sector", index, (byte) (sec+1));
						bank.putByte("layer", index, (byte) (lay+1));
						bank.putShort("component", index, (short) strip);
						bank.putInt("ADC", index, BMT.getTile(lay,sec).getHits().get(strip).getADC());
						bank.putFloat("time", index, BMT.getTile(lay,sec).getHits().get(strip).getTime());
						index++;
					}
				}					
			}
		}
		
		return bank;
	}

	public Bank fillBMTCrossesBank(Barrel BMT) {
		int groupsize=0;
		for (int lay=0; lay<6; lay++) {
			for (int sec=0; sec<3; sec++) {
			groupsize+=BMT.getTile(lay,sec).getClusters().size();
			}
		}
		
		Bank bank = new Bank(writer.getSchemaFactory().getSchema("BMTRec::Crosses"),groupsize);
		int index=0;
		
		for (int lay=0; lay<6; lay++) {
			for (int sec=0; sec<3; sec++) {
				for (int j = 0; j < BMT.getTile(lay,sec).getClusters().size(); j++) {
					bank.putShort("ID", index, (short) index);
					bank.putByte("sector", index, (byte) (sec+1));
					bank.putByte("region", index, (byte) ((lay+1)/2));
					bank.putFloat("x", index, (float) (BMT.getTile(lay,sec).getClusters().get(j+1).getX()/10.));
					bank.putFloat("y", index, (float) (BMT.getTile(lay,sec).getClusters().get(j+1).getY()/10.));
					bank.putFloat("z", index, (float) (BMT.getTile(lay,sec).getClusters().get(j+1).getZ()/10.));
					if (!Double.isNaN(BMT.getTile(lay,sec).getClusters().get(j+1).getX())) {
						bank.putFloat("err_x", index, (float) Math.abs(BMT.getTile(lay,sec).getClusters().get(j+1).getErr()*Math.sin(BMT.getTile(lay,sec).getClusters().get(j+1).getPhi())));
						bank.putFloat("err_y", index, (float) Math.abs(BMT.getTile(lay,sec).getClusters().get(j+1).getErr()*Math.cos(BMT.getTile(lay,sec).getClusters().get(j+1).getPhi())));
					}
					else {
						bank.putFloat("err_x", index, Float.NaN);
						bank.putFloat("err_y", index, Float.NaN);
					}
					if (!Double.isNaN(BMT.getTile(lay,sec).getClusters().get(j+1).getZ())) bank.putFloat("err_z", index, (float) BMT.getTile(lay,sec).getClusters().get(j+1).getErr());
					else bank.putFloat("err_z", index, Float.NaN);
					index++;
				}
			}
		}
        return bank;
	}
	
	public Bank fillBSTCrossesBank(Barrel_SVT BST) {
		int groupsize=0;
		for (int lay=1; lay<7; lay++) {
			for (int sec=1; sec<19; sec++) {
			groupsize+=BST.getModule(lay,sec).getClusters().size();
			}
		}
		
		Bank bank = new Bank(writer.getSchemaFactory().getSchema("BSTRec::Crosses"),groupsize);
		int index=0;
		
		for (int lay=1; lay<7; lay++) {
			for (int sec=1; sec<19; sec++) {
				for (int j = 0; j < BST.getModule(lay,sec).getClusters().size(); j++) {
					bank.putShort("ID", index, (short) index);
					bank.putByte("sector", index, (byte) sec);
					bank.putByte("region", index, (byte) ((lay+1)/2));
					bank.putFloat("x", index, (float) (BST.getModule(lay,sec).getClusters().get(j+1).getX()/10.));
					bank.putFloat("y", index, (float) (BST.getModule(lay,sec).getClusters().get(j+1).getY()/10.));
					bank.putFloat("z", index, (float) (BST.getModule(lay,sec).getClusters().get(j+1).getZ()/10.));
					bank.putFloat("err_x", index, (float) Math.abs(BST.getModule(lay,sec).getClusters().get(j+1).getErrPhi()*Math.sin(BST.getModule(lay,sec).getClusters().get(j+1).getPhi())));
					bank.putFloat("err_y", index, (float) Math.abs(BST.getModule(lay,sec).getClusters().get(j+1).getErrPhi()*Math.cos(BST.getModule(lay,sec).getClusters().get(j+1).getPhi())));
					bank.putFloat("err_z", index, (float) (BST.getModule(lay,sec).getClusters().get(j+1).getErrZ()/10.));
					index++;
				}
			}
		}
        return bank;
	}
	
	public Bank fillBSTHitsBank(Barrel_SVT BST) {
		int groupsize=BST.getNbHits();
				
		Bank bank = new Bank(writer.getSchemaFactory().getSchema("BSTRec::Hits"),groupsize);
		
		int index=0;
		int index_clus=0;
				
		for (int lay=1; lay<7; lay++) {
			for (int sec=1; sec<19; sec++) {
				for (int j = 0; j < BST.getModule(lay,sec).getClusters().size(); j++) {
					for (int str=0;str<BST.getModule(lay,sec).getClusters().get(j+1).getListOfHits().size();str++) {
						int strip=BST.getModule(lay,sec).getClusters().get(j+1).getListOfHits().get(str);
						bank.putShort("clusterID", index, (short) index_clus);
						bank.putShort("trkID", index, (short) BST.getModule(lay,sec).getClusters().get(j+1).gettrkID());
						bank.putByte("sector", index, (byte) sec);
						bank.putByte("layer", index, (byte) lay);
						bank.putInt("strip", index, strip);
						bank.putShort("ID", index, (short) BST.getModule(lay,sec).getHits().get(strip).getHit_ID());
						bank.putFloat("fitResidual", index, (float) BST.getModule(lay,sec).getHits().get(strip).getResidual());
						index++;
					}
					index_clus++;
				}
			}
		}
        return bank;
	}
	
	public Bank fillBSTClusterBank(Barrel_SVT BST) {
		int groupsize=0;
		for (int lay=1; lay<7; lay++) {
			for (int sec=1; sec<19; sec++) {
			groupsize+=BST.getModule(lay,sec).getClusters().size();
			}
		}
		
		Bank bank = new Bank(writer.getSchemaFactory().getSchema("BSTRec::Clusters"),groupsize);
		int index=0;
						
		for (int lay=1; lay<7; lay++) {
			for (int sec=1; sec<19; sec++) {
				for (int j = 0; j < BST.getModule(lay,sec).getClusters().size(); j++) {
					bank.putShort("ID", index, (short) index);
					bank.putShort("trkID", index, (short) BST.getModule(lay,sec).getClusters().get(j+1).gettrkID());
					bank.putByte("sector", index, (byte) sec);
					bank.putByte("layer", index, (byte) lay);
					bank.putFloat("centroid", index, (float) BST.getModule(lay,sec).getClusters().get(j+1).getCentroid());
					bank.putShort("size", index, (short) BST.getModule(lay,sec).getClusters().get(j+1).getListOfHits().size());
					bank.putFloat("ETot", index, (float) BST.getModule(lay,sec).getClusters().get(j+1).getEtot());
					bank.putInt("seedStrip", index, (int) BST.getModule(lay,sec).getClusters().get(j+1).getSeed());
					bank.putFloat("seedE", index, (float) BST.getModule(lay,sec).getClusters().get(j+1).getSeedE());
					bank.putFloat("centroidResidual", index, (float) BST.getModule(lay,sec).getClusters().get(j+1).getCentroidResidual());
					index++;
				}
			}
		}
        return bank;
	}
	
	public Bank fillBMTClusterBank(Barrel BMT) {
		int groupsize=0;
		for (int lay=0; lay<6; lay++) {
			for (int sec=0; sec<3; sec++) {
			groupsize+=BMT.getTile(lay,sec).getClusters().size();
			}
		}
		
		Bank bank = new Bank(writer.getSchemaFactory().getSchema("BMTRec::Clusters"),groupsize);
		int index=0;
						
		for (int lay=0; lay<6; lay++) {
			for (int sec=0; sec<3; sec++) {
				for (int j = 0; j < BMT.getTile(lay,sec).getClusters().size(); j++) {
					bank.putShort("ID", index, (short) index);
					bank.putShort("trkID", index, (short) BMT.getTile(lay,sec).getClusters().get(j+1).gettrkID());
					bank.putByte("sector", index, (byte) (sec+1));
					bank.putByte("layer", index, (byte) (lay+1));
					bank.putFloat("centroid", index, (float) BMT.getTile(lay,sec).getClusters().get(j+1).getCentroid());
					bank.putShort("size", index, (short) BMT.getTile(lay,sec).getClusters().get(j+1).getSize());
					bank.putFloat("ETot", index, (float) BMT.getTile(lay,sec).getClusters().get(j+1).getEdep());
					bank.putInt("seedStrip", index, (int) BMT.getTile(lay,sec).getClusters().get(j+1).getSeed());
					bank.putFloat("seedE", index, (float) BMT.getTile(lay,sec).getClusters().get(j+1).getSeedE());
					bank.putFloat("Tmin", index, (float) BMT.getTile(lay,sec).getClusters().get(j+1).getT_min());
					bank.putFloat("Tmax", index, (float) BMT.getTile(lay,sec).getClusters().get(j+1).getT_max());
					bank.putFloat("centroidResidual", index, (float) BMT.getTile(lay,sec).getClusters().get(j+1).getCentroidResidual());
					index++;
				}
			}
		}
        return bank;
	}
	
	public Bank fillMCBank(ParticleEvent MCParticles) {
		
		int groupsize=MCParticles.hasNumberOfParticles();
		
		
		Bank bank = new Bank(writer.getSchemaFactory().getSchema("MC::Particle"),groupsize);
		int index=0;
		for (int i=0; i<groupsize; i++) {
			bank.putInt("pid", index,  MCParticles.getParticles().get(i).getPid());
			bank.putFloat("px", index, (float) MCParticles.getParticles().get(i).getPx());
			bank.putFloat("py", index, (float) MCParticles.getParticles().get(i).getPy());
			bank.putFloat("pz", index, (float) MCParticles.getParticles().get(i).getPz());
			bank.putFloat("vx", index, (float) MCParticles.getParticles().get(i).getVx());
			bank.putFloat("vy", index, (float) MCParticles.getParticles().get(i).getVy());
			bank.putFloat("vz", index, (float) MCParticles.getParticles().get(i).getVz());
			index++;
		}
		
		return bank;
	}
	
	public Bank fillCosmicRecBank(ArrayList<TrackCandidate> candidates) {
		int groupsize=candidates.size();
				
		Bank bank = new Bank(writer.getSchemaFactory().getSchema("CVTRec::Cosmics"),groupsize);
		
		int index=0;
		for (int i=0; i<groupsize; i++) {
			Vector3D inter=candidates.get(i).getLine().IntersectWithPlaneY();
			bank.putShort("ID", index, (short) (index+1));
			bank.putFloat("trkline_yx_slope", index, (float) (candidates.get(i).get_VectorTrack().x()/candidates.get(i).get_VectorTrack().y()));
			bank.putFloat("trkline_yx_interc", index, (float) (inter.x()/10.));
			bank.putFloat("trkline_yz_slope", index, (float) (candidates.get(i).get_VectorTrack().z()/candidates.get(i).get_VectorTrack().y()));
			bank.putFloat("trkline_yz_interc", index, (float) (inter.z()/10.));
			bank.putFloat("theta", index, (float) Math.toDegrees((candidates.get(i).getTheta())));
			bank.putFloat("phi", index, (float) Math.toDegrees((candidates.get(i).getPhi())));
			bank.putFloat("chi2", index, (float) (candidates.get(i).get_chi2()));
			bank.putShort("NbBSTHits", index, (short) (candidates.get(i).get_Nsvt()));
			bank.putShort("NbBMTHits", index, (short) (candidates.get(i).get_Nc()+candidates.get(i).get_Nz()));
			int ndf=0;
			if (main.constant.TrackerType.equals("SVT")) ndf=candidates.get(i).get_Nsvt()-4;
			if (main.constant.TrackerType.equals("MVT")) ndf=candidates.get(i).get_Nc()+candidates.get(i).get_Nz()-4;
			if (main.constant.TrackerType.equals("CVT")) ndf=candidates.get(i).get_Nc()+candidates.get(i).get_Nz()+candidates.get(i).get_Nsvt()-4;
			bank.putShort("ndf", index, (short) ndf);
			index++;
		}
		
		return bank;
	}
	
	public Bank fillRunConfig(int eventnb) {
		int groupsize=1;
				
		Bank bank = new Bank(writer.getSchemaFactory().getSchema("RUN::config"));
		bank.putInt("event", 0, (int) eventnb);
		if (main.constant.isCosmic) bank.putByte("type", 0, (byte) 1);
		if (!main.constant.isCosmic) bank.putByte("type", 0, (byte) 0);
		
		return bank;
	}
	
	
	//This method is filling cosmic traj bank... And Update crosses if they are linked to a trajectory!!!!
	public Bank fillCosmicTrajBank(Barrel BMT, Barrel_SVT BST, ArrayList<TrackCandidate> candidates) {
		int groupsize=12*candidates.size();
		if (main.constant.isCosmic) groupsize=groupsize*2+10;
		
		Bank bank = new Bank(writer.getSchemaFactory().getSchema("CVTRec::Trajectory"),groupsize);
		
		int index=0;
		for (int track=0; track<candidates.size();track++) {
			//Intercept with SVT modules
			for (int lay=0; lay<6;lay++) {
				for (int sector=1; sector<BST.getGeometry().getNbModule(lay+1)+1;sector++) {
					Vector3D inter=new Vector3D(BST.getGeometry().getIntersectWithRay(lay+1, sector, candidates.get(track).get_VectorTrack(), candidates.get(track).get_PointTrack()));
					if (!Double.isNaN(inter.x())&&sector==BST.getGeometry().findSectorFromAngle(lay+1, inter)
							&&(main.constant.isCosmic||(BST.getGeometry().findSectorFromAngle(lay+1, candidates.get(track).get_PointTrack())==sector))) {
						//int sector=BST.getGeometry().findSectorFromAngle(lay+1, inter);
						
						Vector3D normSVT=BST.getGeometry().findBSTPlaneNormal(sector, lay+1);
						Vector3D PhiSVT=new Vector3D();
						PhiSVT.setX(candidates.get(track).get_VectorTrack().x());PhiSVT.setY(candidates.get(track).get_VectorTrack().y());PhiSVT.setZ(0);
						Vector3D eTheta=new Vector3D();
						eTheta.setX(-Math.sin(BST.getGeometry().findBSTPlaneAngle(sector, lay+1)));	eTheta.setY(Math.cos(BST.getGeometry().findBSTPlaneAngle(sector, lay+1))); eTheta.setZ(0);		
												
						Vector3D ThetaSVT=new Vector3D();
						ThetaSVT.setX(candidates.get(track).get_VectorTrack().x());ThetaSVT.setY(candidates.get(track).get_VectorTrack().y());ThetaSVT.setZ(candidates.get(track).get_VectorTrack().z());
						Vector3D ProjThetaSVT=new Vector3D();
						ProjThetaSVT.setX(candidates.get(track).get_VectorTrack().x());ProjThetaSVT.setY(candidates.get(track).get_VectorTrack().y());ProjThetaSVT.setZ(candidates.get(track).get_VectorTrack().z());
						ThetaSVT.sub(ProjThetaSVT.projection(eTheta));
						
						bank.putShort("id", index, (short) (track+1));
						bank.putByte("layer", index, (byte) (lay+1));
						bank.putByte("sector", index, (byte) (sector));
						bank.putFloat("x", index, (float) (inter.x()/10.));
						bank.putFloat("y", index, (float) (inter.y()/10.));
						bank.putFloat("z", index, (float) (inter.z()/10.));
						bank.putFloat("phi", index, (float) Math.toDegrees(PhiSVT.angle(normSVT)));
						bank.putFloat("theta", index, (float) Math.toDegrees(ThetaSVT.angle(normSVT)));
						bank.putFloat("langle", index, (float) Math.toDegrees(candidates.get(track).get_VectorTrack().angle(normSVT)));
						bank.putFloat("centroid", index, (float) BST.getGeometry().calcNearestStrip(inter.x(), inter.y(), inter.z(), lay+1, sector));
						index++;
					
						int clus_id=-1;
						for (int clus_track=0;clus_track<candidates.get(track).BSTsize();clus_track++) {
							if (candidates.get(track).GetBSTCluster(clus_track).getLayer()==(lay+1)&&candidates.get(track).GetBSTCluster(clus_track).getSector()==sector) 
								clus_id=candidates.get(track).GetBSTCluster(clus_track).getLastEntry();
						}
					
						if (clus_id!=-1) { //&&(main.constant.TrackerType.equals("SVT")||main.constant.TrackerType.equals("CVT"))) {
							//Update the cluster X,Y,Z info with track info
							for (int clus=0;clus<BST.getModule(lay+1, sector).getClusters().size();clus++) {
								if (BST.getModule(lay+1, sector).getClusters().get(clus+1).getLastEntry()==clus_id) {
									BST.getModule(lay+1, sector).getClusters().get(clus+1).setX(inter.x());
									BST.getModule(lay+1, sector).getClusters().get(clus+1).setY(inter.y());
									BST.getModule(lay+1, sector).getClusters().get(clus+1).setZ(inter.z());
									BST.getModule(lay+1, sector).getClusters().get(clus+1).settrkID(track+1);
									BST.getModule(lay+1, sector).getClusters().get(clus+1).setCentroidResidual(BST.getGeometry().getResidual_line(lay+1,sector,BST.getModule(lay+1, sector).getClusters().get(clus+1).getCentroid(),inter));
								}
							}
						}
					}
				}
			}
			
			//Intercept with BMT tiles and add info on missing coordinates of the clusters.
			int sector=BMT.getGeometry().isinsector(candidates.get(track).get_PointTrack());
			for (int lay=0; lay<6;lay++) {
				for (int sec=1; sec<4;sec++) {
					
					Vector3D inter=new Vector3D(BMT.getGeometry().getIntercept(lay+1, sec, candidates.get(track).get_VectorTrack(), candidates.get(track).get_PointTrack()));
					
					if (!Double.isNaN(inter.x())&&(main.constant.isCosmic||sec==sector)) {
						bank.putShort("id", index, (short) (track+1));
						bank.putByte("layer", index, (byte) (lay+7));
						bank.putByte("sector", index, (byte) sec);
						bank.putFloat("x", index, (float) (inter.x()/10.));
						bank.putFloat("y", index, (float) (inter.y()/10.));
						bank.putFloat("z", index, (float) (inter.z()/10.));
						
						if (BMT.getGeometry().getZorC(lay+1)==0) bank.putFloat("centroid", index, (float) BMT.getGeometry().getCStrip(lay+1, inter.z()));
						if (BMT.getGeometry().getZorC(lay+1)==1) bank.putFloat("centroid", index, (float) BMT.getGeometry().getZStrip(lay+1, Math.atan2(inter.y(), inter.x())));
						
						int clus_id=-1;
						for (int clus_track=0;clus_track<candidates.get(track).size();clus_track++) {
							if (candidates.get(track).GetBMTCluster(clus_track).getLayer()==(lay+1)&&candidates.get(track).GetBMTCluster(clus_track).getSector()==sec) {
								clus_id=candidates.get(track).GetBMTCluster(clus_track).getLastEntry();
								if (clus_id!=-1) { //&&(main.constant.TrackerType.equals("MVT")||main.constant.TrackerType.equals("CVT"))) {
									//Update the cluster X,Y,Z info with track info
									for (int clus=0;clus<BMT.getTile(lay, sec-1).getClusters().size();clus++) {
										if (BMT.getTile(lay, sec-1).getClusters().get(clus+1).getLastEntry()==clus_id) {
											if (BMT.getGeometry().getZorC(lay+1)==0) {
												BMT.getTile(lay, sec-1).getClusters().get(clus+1).setX(inter.x());
												BMT.getTile(lay, sec-1).getClusters().get(clus+1).setY(inter.y());
											}
											if (BMT.getGeometry().getZorC(lay+1)==1) BMT.getTile(lay, sec-1).getClusters().get(clus+1).setZ(inter.z());
											BMT.getTile(lay, sec-1).getClusters().get(clus+1).settrkID(track+1);
											//BMT.getTile(lay, sec-1).getClusters().get(clus+1).setCentroidResidual(BMT.getGeometry().getResidual_line(BMT.getTile(lay, sec-1).getClusters().get(clus+1),candidates.get(track).get_VectorTrack(),candidates.get(track).get_PointTrack()));
											BMT.getTile(lay, sec-1).getClusters().get(clus+1).setCentroidResidual(candidates.get(track).getResidual(clus_track));
										}
									}
								}
							}
						}
					
						
					
						inter.setZ(0);// er is the vector normal to the tile... use inter to compute the angle between track and tile normal.
						bank.putFloat("langle", index, (float) Math.toDegrees(candidates.get(track).get_VectorTrack().angle(inter)));
						Vector3D PhiBMT=new Vector3D();
						PhiBMT.setXYZ(candidates.get(track).get_VectorTrack().x(),candidates.get(track).get_VectorTrack().y() , 0);
						Vector3D eTheta=new Vector3D();
						eTheta.setXYZ(-inter.y(),inter.x(),0);
						Vector3D ThetaBMT=new Vector3D();
						ThetaBMT.setXYZ(candidates.get(track).get_VectorTrack().x(),candidates.get(track).get_VectorTrack().y() , candidates.get(track).get_VectorTrack().z());
						Vector3D ProjThetaBMT=new Vector3D();
						ProjThetaBMT=ThetaBMT.projection(eTheta);
						ThetaBMT.sub(ProjThetaBMT);
						bank.putFloat("phi", index, (float) Math.toDegrees(PhiBMT.angle(inter)));
						bank.putFloat("theta", index, (float) Math.toDegrees(ThetaBMT.angle(inter)));
						index++;
					}
				}
			}
		}
				
		return bank;
	}
	
	public void close() {
		writer.close();
	}
}
