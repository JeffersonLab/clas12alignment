package main;

import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataSource;
import java.util.*;
import org.jlab.groot.*;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.ui.TCanvas;
import org.jlab.io.base.DataBank;


public class Efficiency {
	public static int[] NumStrip= new int[6];
	public static double PhiMin; //Fiducail range for C-tile
	public static double PhiMax; //Fiducail range for C-tile
	public static double DeltaPhi; //For convenience
	public static double[] Zmin= new double[3]; //Fiducail range for Z-tile
	public static double[] Zmax= new double[3]; //Fiducail range for Z-tile
	
	
	public static int DeltaStripEff;
	
	public Efficiency() {
		NumStrip[0]=896; NumStrip[1]=640; NumStrip[2]=640; NumStrip[3]=1024; NumStrip[4]=768; NumStrip[5]=1152;
		PhiMin=Math.toRadians(10);
		PhiMax=Math.toRadians(95);
		DeltaPhi=Math.toRadians(35);
		Zmin[0]=-115.77;Zmin[1]=-136.69;Zmin[2]=-157.6;
		Zmax[0]=216.93;Zmax[1]=244.25;Zmax[2]=247.25;
		DeltaStripEff=20;
		
	}

	public static void main(String[] args) {
		
		Efficiency eff=new Efficiency();
				
		double[][] GotHit=new double[6][3];
		double[][] RefTrack=new double[6][3];
		
		double[] HV_strip=new double[args.length-1];
		double[] CR4C_sec1=new double[args.length-1];
		double[] CR4C_sec2=new double[args.length-1];
		double[] CR4C_sec3=new double[args.length-1];
		double[] CR5C_sec1=new double[args.length-1];
		double[] CR5C_sec2=new double[args.length-1];
		double[] CR5C_sec3=new double[args.length-1];
		double[] CR6C_sec1=new double[args.length-1];
		double[] CR6C_sec2=new double[args.length-1];
		double[] CR6C_sec3=new double[args.length-1];
		
		double[] CR4Z_sec1=new double[args.length-1];
		double[] CR4Z_sec2=new double[args.length-1];
		double[] CR4Z_sec3=new double[args.length-1];
		double[] CR5Z_sec1=new double[args.length-1];
		double[] CR5Z_sec2=new double[args.length-1];
		double[] CR5Z_sec3=new double[args.length-1];
		double[] CR6Z_sec1=new double[args.length-1];
		double[] CR6Z_sec2=new double[args.length-1];
		double[] CR6Z_sec3=new double[args.length-1];
		
		double[] errCR4C_sec1=new double[args.length-1];
		double[] errCR4C_sec2=new double[args.length-1];
		double[] errCR4C_sec3=new double[args.length-1];
		double[] errCR5C_sec1=new double[args.length-1];
		double[] errCR5C_sec2=new double[args.length-1];
		double[] errCR5C_sec3=new double[args.length-1];
		double[] errCR6C_sec1=new double[args.length-1];
		double[] errCR6C_sec2=new double[args.length-1];
		double[] errCR6C_sec3=new double[args.length-1];
		
		double[] errCR4Z_sec1=new double[args.length-1];
		double[] errCR4Z_sec2=new double[args.length-1];
		double[] errCR4Z_sec3=new double[args.length-1];
		double[] errCR5Z_sec1=new double[args.length-1];
		double[] errCR5Z_sec2=new double[args.length-1];
		double[] errCR5Z_sec3=new double[args.length-1];
		double[] errCR6Z_sec1=new double[args.length-1];
		double[] errCR6Z_sec2=new double[args.length-1];
		double[] errCR6Z_sec3=new double[args.length-1];
		
		for (int num_file=0;num_file<args.length-1;num_file++) {
		
			for (int i=0;i<6;i++) {
				for (int j=0;j<3;j++) {
					GotHit[i][j]=0;
					RefTrack[i][j]=0;
				}
			}
		
			String fileName=args[0]+args[num_file+1]+".hipo";
			HV_strip[num_file]=Double.parseDouble(args[num_file+1]);
		
			HipoDataSource reader = new HipoDataSource();
			reader.open(fileName);
		
			while(reader.hasEvent()) {
					
				DataEvent event = reader.getNextEvent();
				//Look if a track has been found
				if(event.hasBank("CVTRec::Cosmics")&&event.hasBank("CVTRec::Trajectory")) {
					DataBank Cbank=event.getBank("CVTRec::Cosmics");
					DataBank Bbank=event.getBank("BMTRec::Clusters");
					DataBank Tbank=event.getBank("CVTRec::Trajectory");
					for (int Crow=0;Crow<Cbank.rows();Crow++){
						if (Cbank.getShort("NbBSTHits",Crow )>8&&Cbank.getShort("NbBMTHits",Crow )>3) {
							// Let's find the sector of the good track
							boolean[] LayerHit= {false,false,false,false,false,false};
							int sector=-1;
							int counter=0;
							while (sector==-1) {
								if (Bbank.getShort("trkID",counter)==Cbank.getShort("ID",Crow)) sector=Bbank.getByte("sector",counter);
								counter++;
							}
						
							//Now let us check the efficiency in the opposite sector giving the track 
							for (int Trow=0;Trow<Tbank.rows();Trow++) {
								//The intercept must belong to the track (might have several track)
								if (Cbank.getShort("ID",Crow)==Tbank.getShort("ID",Trow)) {
									//Need to not look at intercept in same sector than the track was found
									if (Tbank.getByte("LayerTrackIntersPlane",Trow)>=7&&sector!=Tbank.getByte("SectorTrackIntersPlane",Trow)) {
									
										if (eff.IsInFiducial(Tbank.getByte("LayerTrackIntersPlane",Trow)-6, Tbank.getByte("SectorTrackIntersPlane",Trow), Tbank.getFloat("XtrackIntersPlane",Trow), Tbank.getFloat("YtrackIntersPlane",Trow), Tbank.getFloat("ZtrackIntersPlane",Trow) ,Tbank.getFloat("CalcCentroidStrip",Trow))) {
											// The track must intercept a layer in the opposite sector and not close to the edge (Need to add Phi condition for C and Z condition for Z-tile)
											// Need now to check if a cluster match
											// We are looking for a good hit
											RefTrack[Tbank.getByte("LayerTrackIntersPlane",Trow)-7][Tbank.getByte("SectorTrackIntersPlane",Trow)-1]++;
										
											for (int Brow=0;Brow<Bbank.rows();Brow++) {
												if (Math.abs(Bbank.getFloat("centroid",Brow)-Tbank.getFloat("CalcCentroidStrip",Trow))<DeltaStripEff&&
														(Tbank.getByte("LayerTrackIntersPlane",Trow)-6)==Bbank.getByte("layer",Brow)&&
														Tbank.getByte("SectorTrackIntersPlane",Trow)==Bbank.getByte("sector",Brow)&&
														!LayerHit[Tbank.getByte("LayerTrackIntersPlane",Trow)-7]) {
														//On a trouve un bon hit dans la tuile, donc on marque la tuile pour pas compter deux fois si multiple cluster
														LayerHit[Tbank.getByte("LayerTrackIntersPlane",Trow)-7]=true;
														GotHit[Bbank.getByte("layer",Brow)-1][Bbank.getByte("sector",Brow)-1]++;
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
		
		//Printout the results
		//for (int i=0;i<6;i++) {
			//for (int j=0;j<3;j++) {
				//System.out.println("Layer "+(i+1)+" sector "+(j+1)+" Efficiency "+(GotHit[i][j]/RefTrack[i][j])+" with "+RefTrack[i][j]+" reference tracks");
				//}
			//}
			CR4C_sec1[num_file]=GotHit[0][0]/RefTrack[0][0];CR4C_sec2[num_file]=GotHit[0][1]/RefTrack[0][1];CR4C_sec3[num_file]=GotHit[0][2]/RefTrack[0][2];
			CR4Z_sec1[num_file]=GotHit[1][0]/RefTrack[1][0];CR4Z_sec2[num_file]=GotHit[1][1]/RefTrack[1][1];CR4Z_sec3[num_file]=GotHit[1][2]/RefTrack[1][2];
			
			CR5C_sec1[num_file]=GotHit[3][0]/RefTrack[3][0];CR5C_sec2[num_file]=GotHit[3][1]/RefTrack[3][1];CR5C_sec3[num_file]=GotHit[3][2]/RefTrack[3][2];
			CR5Z_sec1[num_file]=GotHit[2][0]/RefTrack[2][0];CR5Z_sec2[num_file]=GotHit[2][1]/RefTrack[2][1];CR5Z_sec3[num_file]=GotHit[2][2]/RefTrack[2][2];
			
			CR6C_sec1[num_file]=GotHit[5][0]/RefTrack[5][0];CR6C_sec2[num_file]=GotHit[5][1]/RefTrack[5][1];CR6C_sec3[num_file]=GotHit[5][2]/RefTrack[5][2];
			CR6Z_sec1[num_file]=GotHit[4][0]/RefTrack[4][0];CR6Z_sec2[num_file]=GotHit[4][1]/RefTrack[4][1];CR6Z_sec3[num_file]=GotHit[4][2]/RefTrack[4][2];
			
			errCR4C_sec1[num_file]=Math.sqrt((CR4C_sec1[num_file]*(1-CR4C_sec1[num_file]))/RefTrack[0][0]); errCR4C_sec2[num_file]=Math.sqrt((CR4C_sec2[num_file]*(1-CR4C_sec2[num_file]))/RefTrack[0][1]); errCR4C_sec3[num_file]=Math.sqrt((CR4C_sec3[num_file]*(1-CR4C_sec3[num_file]))/RefTrack[0][2]);
			errCR5C_sec1[num_file]=Math.sqrt((CR5C_sec1[num_file]*(1-CR5C_sec1[num_file]))/RefTrack[3][0]); errCR5C_sec2[num_file]=Math.sqrt((CR5C_sec2[num_file]*(1-CR5C_sec2[num_file]))/RefTrack[3][1]); errCR5C_sec3[num_file]=Math.sqrt((CR5C_sec3[num_file]*(1-CR5C_sec3[num_file]))/RefTrack[3][2]);
			errCR6C_sec1[num_file]=Math.sqrt((CR6C_sec1[num_file]*(1-CR6C_sec1[num_file]))/RefTrack[5][0]); errCR6C_sec2[num_file]=Math.sqrt((CR6C_sec2[num_file]*(1-CR6C_sec2[num_file]))/RefTrack[5][1]); errCR6C_sec3[num_file]=Math.sqrt((CR6C_sec3[num_file]*(1-CR6C_sec3[num_file]))/RefTrack[5][2]);

			errCR4Z_sec1[num_file]=Math.sqrt((CR4Z_sec1[num_file]*(1-CR4Z_sec1[num_file]))/RefTrack[1][0]); errCR4Z_sec2[num_file]=Math.sqrt((CR4Z_sec2[num_file]*(1-CR4Z_sec2[num_file]))/RefTrack[1][1]); errCR4Z_sec3[num_file]=Math.sqrt((CR4Z_sec3[num_file]*(1-CR4Z_sec3[num_file]))/RefTrack[1][2]);
			errCR5Z_sec1[num_file]=Math.sqrt((CR5Z_sec1[num_file]*(1-CR5Z_sec1[num_file]))/RefTrack[2][0]); errCR5Z_sec2[num_file]=Math.sqrt((CR5Z_sec2[num_file]*(1-CR5Z_sec2[num_file]))/RefTrack[2][1]); errCR5Z_sec3[num_file]=Math.sqrt((CR5Z_sec3[num_file]*(1-CR5Z_sec3[num_file]))/RefTrack[2][2]);
			errCR6Z_sec1[num_file]=Math.sqrt((CR6Z_sec1[num_file]*(1-CR6Z_sec1[num_file]))/RefTrack[4][0]); errCR6Z_sec2[num_file]=Math.sqrt((CR6Z_sec2[num_file]*(1-CR6Z_sec2[num_file]))/RefTrack[4][1]); errCR6Z_sec3[num_file]=Math.sqrt((CR6Z_sec3[num_file]*(1-CR6Z_sec3[num_file]))/RefTrack[4][2]);
		}
		
		GraphErrors Eff4C1=new GraphErrors();GraphErrors Eff4C2=new GraphErrors();GraphErrors Eff4C3=new GraphErrors();
		Eff4C1.setMarkerStyle(0);Eff4C1.setMarkerColor(3);Eff4C2.setMarkerColor(1);Eff4C2.setMarkerStyle(1);Eff4C3.setMarkerColor(2);Eff4C3.setMarkerStyle(2);
		Eff4C1.setTitle("CR4C-Layer1 Efficiency"); Eff4C1.setTitleX("HV strip (V)");
		
		GraphErrors Eff5C1=new GraphErrors();GraphErrors Eff5C2=new GraphErrors();GraphErrors Eff5C3=new GraphErrors();
		Eff5C1.setMarkerStyle(0);Eff5C1.setMarkerColor(3);Eff5C2.setMarkerColor(1);Eff5C2.setMarkerStyle(1);Eff5C3.setMarkerColor(2);Eff5C3.setMarkerStyle(2);
		Eff5C1.setTitle("CR5C-Layer4 Efficiency"); Eff5C1.setTitleX("HV strip (V)");
		
		GraphErrors Eff6C1=new GraphErrors();GraphErrors Eff6C2=new GraphErrors();GraphErrors Eff6C3=new GraphErrors();
		Eff6C1.setMarkerStyle(0);Eff6C1.setMarkerColor(3);Eff6C2.setMarkerColor(1);Eff6C2.setMarkerStyle(1);Eff6C3.setMarkerColor(2);Eff6C3.setMarkerStyle(2);
		Eff6C1.setTitle("CR6C-Layer6 Efficiency"); Eff6C1.setTitleX("HV strip (V)");
		
		GraphErrors Eff4Z1=new GraphErrors();GraphErrors Eff4Z2=new GraphErrors();GraphErrors Eff4Z3=new GraphErrors();
		Eff4Z1.setMarkerStyle(0);Eff4Z1.setMarkerColor(3);Eff4Z2.setMarkerColor(1);Eff4Z2.setMarkerStyle(1);Eff4Z3.setMarkerColor(2);Eff4Z3.setMarkerStyle(2);
		Eff4Z1.setTitle("CR4Z-Layer2 Efficiency"); Eff4Z1.setTitleX("HV strip (V)");
		
		GraphErrors Eff5Z1=new GraphErrors();GraphErrors Eff5Z2=new GraphErrors();GraphErrors Eff5Z3=new GraphErrors();
		Eff5Z1.setMarkerStyle(0);Eff5Z1.setMarkerColor(3);Eff5Z2.setMarkerColor(1);Eff5Z2.setMarkerStyle(1);Eff5Z3.setMarkerColor(2);Eff5Z3.setMarkerStyle(2);
		Eff5Z1.setTitle("CR5Z-Layer3 Efficiency");Eff5Z1.setTitleX("HV strip (V)");
		
		GraphErrors Eff6Z1=new GraphErrors();GraphErrors Eff6Z2=new GraphErrors();GraphErrors Eff6Z3=new GraphErrors();
		Eff6Z1.setMarkerStyle(0);Eff6Z1.setMarkerColor(3);Eff6Z2.setMarkerColor(1);Eff6Z2.setMarkerStyle(1);Eff6Z3.setMarkerColor(2);Eff6Z3.setMarkerStyle(2);
		Eff6Z1.setTitle("CR6Z-Layer5 Efficiency");Eff6Z1.setTitleX("HV strip (V)");
		
		for (int i=0;i<args.length-1;i++) {
			Eff4C1.addPoint(HV_strip[i]-1, CR4C_sec1[i], 0, errCR4C_sec1[i]);
			Eff4C2.addPoint(HV_strip[i], CR4C_sec2[i], 0, errCR4C_sec2[i]);
			Eff4C3.addPoint(HV_strip[i]+1, CR4C_sec3[i], 0, errCR4C_sec3[i]);
			
			Eff5C1.addPoint(HV_strip[i]-1, CR5C_sec1[i], 0, errCR5C_sec1[i]);
			Eff5C2.addPoint(HV_strip[i], CR5C_sec2[i], 0, errCR5C_sec2[i]);
			Eff5C3.addPoint(HV_strip[i]+1, CR5C_sec3[i], 0, errCR5C_sec3[i]);
			
			Eff6C1.addPoint(HV_strip[i]-1, CR6C_sec1[i], 0, errCR6C_sec1[i]);
			Eff6C2.addPoint(HV_strip[i], CR6C_sec2[i], 0, errCR6C_sec2[i]);
			Eff6C3.addPoint(HV_strip[i]+1, CR6C_sec3[i], 0, errCR6C_sec3[i]);
			
			Eff4Z1.addPoint(HV_strip[i]-1, CR4Z_sec1[i], 0, errCR4Z_sec1[i]);
			Eff4Z2.addPoint(HV_strip[i], CR4Z_sec2[i], 0, errCR4Z_sec2[i]);
			Eff4Z3.addPoint(HV_strip[i]+1, CR4Z_sec3[i], 0, errCR4Z_sec3[i]);
			
			Eff5Z1.addPoint(HV_strip[i]-1, CR5Z_sec1[i], 0, errCR5Z_sec1[i]);
			Eff5Z2.addPoint(HV_strip[i], CR5Z_sec2[i], 0, errCR5Z_sec2[i]);
			Eff5Z3.addPoint(HV_strip[i]+1, CR5Z_sec3[i], 0, errCR5Z_sec3[i]);
			
			Eff6Z1.addPoint(HV_strip[i]-1, CR6Z_sec1[i], 0, errCR6Z_sec1[i]);
			Eff6Z2.addPoint(HV_strip[i], CR6Z_sec2[i], 0, errCR6Z_sec2[i]);
			Eff6Z3.addPoint(HV_strip[i]+1, CR6Z_sec3[i], 0, errCR6Z_sec3[i]);
			
		}
		
		TCanvas Micromegas = new TCanvas("Efficiency BMT", 1600, 1200);
		 Micromegas.divide(3, 2);
		 
		 Micromegas.cd(0);
		 Micromegas.draw(Eff4C1);Micromegas.draw(Eff4C2,"same");Micromegas.draw(Eff4C3,"same");
		 Micromegas.cd(1);
		 Micromegas.draw(Eff5C1);Micromegas.draw(Eff5C2,"same");Micromegas.draw(Eff5C3,"same");
		 Micromegas.cd(2);
		 Micromegas.draw(Eff6C1);Micromegas.draw(Eff6C2,"same");Micromegas.draw(Eff6C3,"same");
		 
		 Micromegas.cd(3);
		 Micromegas.draw(Eff4Z1);Micromegas.draw(Eff4Z2,"same");Micromegas.draw(Eff4Z3,"same");
		 Micromegas.cd(4);
		 Micromegas.draw(Eff5Z1);Micromegas.draw(Eff5Z2,"same");Micromegas.draw(Eff5Z3,"same");
		 Micromegas.cd(5);
		 Micromegas.draw(Eff6Z1);Micromegas.draw(Eff6Z2,"same");Micromegas.draw(Eff6Z3,"same");
	}
	
	public boolean IsInFiducial(int layer, int sector, float x, float y, float z, float CalcCentroid) {
		boolean IsIn=false;
		int region=(layer-1)/2;
		double phiinter=Math.atan2(y, x);
		if (phiinter<0) phiinter+=2*Math.PI;
		
		phiinter=phiinter-DeltaPhi;
		if (sector==1) phiinter=phiinter-2*Math.PI/3.; 
		if (sector==3) phiinter=phiinter-4*Math.PI/3.;
		
		if (CalcCentroid>DeltaStripEff&&CalcCentroid<NumStrip[layer-1]-DeltaStripEff) {
			
		//For C tile, we cut on phi
		  if (layer==1||layer==4||layer==6) {
			if (phiinter>PhiMin&&phiinter<PhiMax) IsIn=true;
		  }
		  
		  //For Z-tile, we cut on z
		  if (layer==2||layer==3||layer==5) {
				if (z>Zmin[region]&&z<Zmax[region]) IsIn=true;
		  }
		}
		
		return IsIn;
	}
	
}
