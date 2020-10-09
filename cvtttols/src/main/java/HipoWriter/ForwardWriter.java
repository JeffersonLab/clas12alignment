package HipoWriter;

import org.jlab.jnp.hipo.data.*;
import org.jlab.jnp.hipo.io.HipoWriter;
import org.jlab.jnp.hipo.schema.*;

import DC_struct.DriftChambers;
import Particles.*;

public class ForwardWriter {
	HipoWriter writer;
	SchemaFactory factory;
	
	
	public ForwardWriter() {
		writer=new HipoWriter();
		factory = new SchemaFactory();
		factory.addSchema(new Schema("{20612,DC::tdc}[1,sector,BYTE][2,layer,BYTE][3,component,SHORT][4,order,BYTE][5,TDC,INT]"));
		factory.addSchema(new Schema("{3,MC::Particle}[1,pid,SHORT][2,px,FLOAT][3,py,FLOAT][4,pz,FLOAT][5,vx,FLOAT][6,vy,FLOAT][7,vz,FLOAT][8,vt,FLOAT]"));
		factory.addSchema(new Schema("{20626,HitBasedTrkg::HBTracks}[1,id,SHORT][2,status,SHORT][3,sector,BYTE][4,c1_x,FLOAT][5,c1_y,FLOAT][6,c1_z,FLOAT]"
				+ "[7,c1_ux,FLOAT][8,c1_uy,FLOAT][9,c1_uz,FLOAT][10,c3_x,FLOAT][11,c3_y,FLOAT][12,c3_z,FLOAT][13,c3_ux,FLOAT][14,c3_uy,FLOAT][15,c3_uz,FLOAT]"
				+ "[16,t1_x,FLOAT][17,t1_y,FLOAT][18,t1_z,FLOAT][19,t1_px,FLOAT][20,t1_py,FLOAT][21,t1_pz,FLOAT]"
				+ "[22,Cross1_ID,SHORT][23,Cross2_ID,SHORT][24,Cross3_ID,SHORT]"
				+ "[25,Vtx0_x,FLOAT][26,Vtx0_y,FLOAT][27,Vtx0_z,FLOAT][28,p0_x,FLOAT][29,p0_y,FLOAT][30,p0_z,FLOAT][31,q,BYTE][32,pathlength,FLOAT][33,chi2,FLOAT][34,ndf,SHORT]"));
		factory.addSchema(new Schema("{11,RUN::config}[1,run,INT][2,event,INT][3,unixtime,INT][4,trigger,LONG][5,timestamp,LONG][6,type,BYTE][7,mode,BYTE][8,torus,FLOAT][9,solenoid,FLOAT]"));
				
		 writer.appendSchemaFactory(factory);
		 		 
	}
	
	public void WriteEvent(int eventnb, DriftChambers DC , ParticleEvent MCParticles) {
		HipoEvent event=writer.createEvent();
		 
		 /*event.writeGroup(this.fillCosmicRecBank(candidates));
		 event.writeGroup(this.fillCosmicTrajBank(BMT,BST,candidates));
		 event.writeGroup(this.fillBMTCrossesBank(BMT));
		 event.writeGroup(this.fillBSTCrossesBank(BST));
		 event.writeGroup(this.fillBSTHitsBank(BST));
		 event.writeGroup(this.fillBSTClusterBank(BST));
		 event.writeGroup(this.fillBMTClusterBank(BMT));*/
		event.writeGroup(this.fillDCTDCbank(DC));
		event.writeGroup(this.fillHBTrajbank(DC));
		 if (main.constant.isMC) event.writeGroup(this.fillMCBank(MCParticles));
		 event.writeGroup(this.fillRunConfig(eventnb));
		 
		 writer.writeEvent( event );
	}
	
	public void setOuputFileName(String output){
		writer.open(output);
	}
	
	public HipoGroup fillDCTDCbank(DriftChambers DC) {
		int groupsize=DC.getNbHits();
		HipoGroup bank = writer.getSchemaFactory().getSchema("DC::tdc").createGroup(groupsize);
		int index=0;
		for (int sec=1; sec<7; sec++) {
			for (int slay=1; slay<7; slay++) {
				for (int lay=1; lay<7; lay++) {
			
					for (int cl = 1; cl < DC.getSector(sec).getSuperLayer(slay).getLayer(lay).getClusterList().size()+1; cl++) {
						for (int wire = 0; wire < DC.getSector(sec).getSuperLayer(slay).getLayer(lay).getClusterList().get(cl).getListOfHits().size(); wire++) {
							
							int strip=DC.getSector(sec).getSuperLayer(slay).getLayer(lay).getClusterList().get(cl).getListOfHits().get(wire);
							bank.getNode("sector").setByte(index, (byte) sec);
							bank.getNode("layer").setByte(index, (byte) (6*(slay-1)+lay));
							bank.getNode("component").setShort(index, (short) strip);
							bank.getNode("order").setByte(index, (byte) 0);
							bank.getNode("TDC").setInt(index, DC.getSector(sec).getSuperLayer(slay).getLayer(lay).getHitList().get(strip).getTDC());
							index++;
						}
					}					
				}
			}
		}
		return bank;
	}
	
	public HipoGroup fillMCBank(ParticleEvent MCParticles) {
		
		int groupsize=MCParticles.hasNumberOfParticles();
		
		
		HipoGroup bank = writer.getSchemaFactory().getSchema("MC::Particle").createGroup(groupsize);
		int index=0;
		for (int i=0; i<groupsize; i++) {
			bank.getNode("pid").setShort(index, (short) MCParticles.getParticles().get(i).getPid());
			bank.getNode("px").setFloat(index, (float) MCParticles.getParticles().get(i).getPx());
			bank.getNode("py").setFloat(index, (float) MCParticles.getParticles().get(i).getPy());
			bank.getNode("pz").setFloat(index, (float) MCParticles.getParticles().get(i).getPz());
			bank.getNode("vx").setFloat(index, (float) MCParticles.getParticles().get(i).getVx());
			bank.getNode("vy").setFloat(index, (float) MCParticles.getParticles().get(i).getVy());
			bank.getNode("vz").setFloat(index, (float) MCParticles.getParticles().get(i).getVz());
			index++;
		}
		
		return bank;
	}
	
	
	public HipoGroup fillRunConfig(int eventnb) {
		int groupsize=1;
				
		HipoGroup bank = writer.getSchemaFactory().getSchema("RUN::config").createGroup(groupsize);
		bank.getNode("event").setInt(0, (int) eventnb);
		if (main.constant.isCosmic) bank.getNode("type").setByte(0, (byte) 1);
		if (!main.constant.isCosmic) bank.getNode("type").setByte(0, (byte) 0);
		
		return bank;
	}
	
	public HipoGroup fillHBTrajbank(DriftChambers DC) {
		int groupsize=0;
		for (int sec=1;sec<7;sec++) {
			groupsize+=DC.getSector(sec).getSectorSegments().size();
		}
		double p_straight=100;
		HipoGroup bank = writer.getSchemaFactory().getSchema("HitBasedTrkg::HBTracks").createGroup(groupsize);
		int index=0;
		for (int sec=1; sec<7; sec++) {
			for (int ray=0; ray<DC.getSector(sec).getSectorSegments().size(); ray++) {
				bank.getNode("id").setShort(index, (short) (index+1));
				bank.getNode("status").setShort(index, (short) (1));
				bank.getNode("Cross1_ID").setShort(index, (short) (-1));
				bank.getNode("Cross2_ID").setShort(index, (short) (-1));
				bank.getNode("Cross3_ID").setShort(index, (short) (-1));
				bank.getNode("sector").setByte(index, (byte) sec);
				bank.getNode("p0_x").setFloat(index, (float) (p_straight*DC.getSector(sec).getSectorSegments().get(ray).getHBtrack().getSlope().x())); 
				bank.getNode("p0_y").setFloat(index, (float) (p_straight*DC.getSector(sec).getSectorSegments().get(ray).getHBtrack().getSlope().y())); 
				bank.getNode("p0_z").setFloat(index, (float) (p_straight*DC.getSector(sec).getSectorSegments().get(ray).getHBtrack().getSlope().z())); 
				
				bank.getNode("Vtx0_x").setFloat(index, (float) DC.getSector(sec).getSectorSegments().get(ray).getHBtrack().getPoint().x()); 
				bank.getNode("Vtx0_y").setFloat(index, (float) DC.getSector(sec).getSectorSegments().get(ray).getHBtrack().getPoint().y()); 
				bank.getNode("Vtx0_z").setFloat(index, (float) DC.getSector(sec).getSectorSegments().get(ray).getHBtrack().getPoint().z()); 
				bank.getNode("q").setByte(index, (byte) 1); 
				bank.getNode("chi2").setFloat(index, (float) DC.getSector(sec).getSectorSegments().get(ray).getChi2()); 
				index++;
			}
		}
		return bank;
	}
	
	public void close() {
		writer.close();
	}
}
