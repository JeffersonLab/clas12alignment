package Alignment;

import BMT_struct.*;
import BST_struct.*;
import java.util.*;
import Alignment.*;
import org.freehep.math.minuit.*;
import org.jlab.geom.prim.Vector3D;
import org.jlab.io.hipo3.Hipo3DataSource;
import Trajectory.*;

public class Aligner {
		
	public Aligner() {
		
	}
			
	public void DoAlignment(Barrel BMT, Barrel_SVT BST, Hipo3DataSource[] reader, int layer, int sector) {
			//Use minimizer
									
				//Create parameters
				MnUserParameters upar = new MnUserParameters();
				double Rx=0; double Ry=0; double Rz=0; double Cx=0; double Cy=0; double Cz=0;
				
				if (layer>6) {
					Rx=BMT_geo.Constants.getRx(layer-6, sector);
					Ry=BMT_geo.Constants.getRy(layer-6, sector);
					Rz=BMT_geo.Constants.getRz(layer-6, sector);
					Cx=BMT_geo.Constants.getCx(layer-6, sector);
					Cy=BMT_geo.Constants.getCy(layer-6, sector);
					Cz=BMT_geo.Constants.getCz(layer-6, sector);
				}
				else {
					Rx=BST.getGeometry().getRx(layer, sector);
					Ry=BST.getGeometry().getRy(layer, sector);
					Rz=BST.getGeometry().getRz(layer, sector);
					Cx=BST.getGeometry().getCx(layer, sector);
					Cy=BST.getGeometry().getCy(layer, sector);
					Cz=BST.getGeometry().getCz(layer, sector);
				}
				
				double DeltaRot=Math.toRadians(3);
				double DeltaTrans=4.0;
			
				double RotErr=2*DeltaRot;
				double TransErr=2*DeltaTrans;
				double Rxmin=Rx-DeltaRot; double Rymin=Ry-DeltaRot; double Rzmin=Rz-DeltaRot; double Cxmin=Cx-DeltaTrans; double Cymin=Cy-DeltaTrans; double Czmin=Cz-DeltaTrans;
				double Rxmax=Rx+DeltaRot; double Rymax=Ry+DeltaRot; double Rzmax=Rz+DeltaRot; double Cxmax=Cx+DeltaTrans; double Cymax=Cy+DeltaTrans; double Czmax=Cz+DeltaTrans;
				
				
				upar.add("Rx", Rx, RotErr, Rxmin, Rxmax);
				upar.add("Ry", Ry, RotErr, Rymin, Rymax);
				upar.add("Rz", Rz, RotErr, Rzmin, Rzmax);
				upar.add("Cx", Cx, TransErr, Cxmin, Cxmax);
				upar.add("Cy", Cy, TransErr, Cymin, Cymax);
				upar.add("Cz", Cz, TransErr, Czmin, Czmax);
				if (layer<=6) upar.fix("Cz");
				if (layer>6) {
					if (BMT.getGeometry().getZorC(layer-6)==1) upar.fix("Cz");
					if (BMT.getGeometry().getZorC(layer-6)==0) upar.fix("Rz");
				}
							  	    
			    //Create function to minimize
			    FCNChi2 Align=new FCNChi2();
			    
			    //Give clusters to Chi2 to compute distance
			    Align.SetDetectorToAlign(BMT,BST,reader,layer,sector);
			    
			 
			    //Create Minuit (parameters and function to minimize)
			    MnMigrad migrad = new MnMigrad(Align, upar);
			 			    
			    //Haven t checked if it is necessarry... might duplicate Straight to parameters for minimum
			    FunctionMinimum min = migrad.minimize();
			    
			    if (min.isValid()) {
			    	double[] res=migrad.params(); //res[0] and res[1] are phi and theta for vec, res[2] is phi for intersection point on cylinder and  res[3] is z_inter
			    	double[] err_res=new double[migrad.covariance().nrow()];
			    	for (int i=0;i<migrad.covariance().nrow();i++) {
			    		err_res[i]=Math.sqrt(migrad.covariance().get(i, i));
			    	}
			    	if (layer>6) {
			    		BMT_geo.Constants.setRx(layer-6, sector, res[0]);
			    		BMT_geo.Constants.setRy(layer-6, sector, res[1]);
			    		BMT_geo.Constants.setRz(layer-6, sector, res[2]);
			    		BMT_geo.Constants.setCx(layer-6, sector, res[3]);
			    		BMT_geo.Constants.setCy(layer-6, sector, res[4]);
			    		BMT_geo.Constants.setCz(layer-6, sector, res[5]);
			    	}
			    	else{
			    		BST.getGeometry().setRx(layer, sector, res[0]);
			    		BST.getGeometry().setRy(layer, sector, res[1]);
			    		BST.getGeometry().setRz(layer, sector, res[2]);
			    		BST.getGeometry().setCx(layer, sector, res[3]);
			    		BST.getGeometry().setCy(layer, sector, res[4]);
			    		BST.getGeometry().setCz(layer, sector, res[5]);
			    		
			    		BST.getGeometry().setRx(layer+1, sector, res[0]);
			    		BST.getGeometry().setRy(layer+1, sector, res[1]);
			    		BST.getGeometry().setRz(layer+1, sector, res[2]);
			    		BST.getGeometry().setCx(layer+1, sector, res[3]);
			    		BST.getGeometry().setCy(layer+1, sector, res[4]);
			    		BST.getGeometry().setCz(layer+1, sector, res[5]);
			    	}
			   	}
			    			    
			
	}
	
	//For aligning MVT versus SVT all at once
	public void DoMVTSVTAlignment(Barrel BMT, Hipo3DataSource[] reader) {
		//Use minimizer
								
			//Create parameters
			MnUserParameters upar = new MnUserParameters();
			double Rx=0; double Ry=0; double Rz=0; double Cx=0; double Cy=0; double Cz=0;
						
			Rx=BMT_geo.Constants.getRxCVT();
			Ry=BMT_geo.Constants.getRyCVT();
			Rz=BMT_geo.Constants.getRzCVT();
			Cx=BMT_geo.Constants.getCxCVT();
			Cy=BMT_geo.Constants.getCyCVT();
			Cz=BMT_geo.Constants.getCzCVT();
			
			double DeltaRot=Math.toRadians(5);
			double DeltaTrans=3;
			if (Cx!=0||Cy!=0||Cz!=0||Rx!=0||Ry!=0||Rz!=0) {
				DeltaRot=Math.toRadians(1.5);
				DeltaTrans=1;
			}
			double RotErr=2*DeltaRot;
			double TransErr=2*DeltaTrans;
			double Rxmin=Rx-DeltaRot; double Rymin=Ry-DeltaRot; double Rzmin=Rz-DeltaRot; double Cxmin=Cx-DeltaTrans; double Cymin=Cy-DeltaTrans; double Czmin=Cz-DeltaTrans;
			double Rxmax=Rx+DeltaRot; double Rymax=Ry+DeltaRot; double Rzmax=Rz+DeltaRot; double Cxmax=Cx+DeltaTrans; double Cymax=Cy+DeltaTrans; double Czmax=Cz+DeltaTrans;
			
			
			upar.add("Rx", Rx, RotErr, Rxmin, Rxmax);
			upar.add("Ry", Ry, RotErr, Rymin, Rymax);
			upar.add("Rz", Rz, RotErr, Rzmin, Rzmax);
			upar.add("Cx", Cx, TransErr, Cxmin, Cxmax);
			upar.add("Cy", Cy, TransErr, Cymin, Cymax);
			upar.add("Cz", Cz, TransErr, Czmin, Czmax);
			
			//Create function to minimize
		    MVTvsSVT Align=new MVTvsSVT();
		    
		    //Give clusters to Chi2 to compute distance
		    Align.SetDetectorToAlign(BMT,reader);
		    
		 
		    //Create Minuit (parameters and function to minimize)
		    MnMigrad migrad = new MnMigrad(Align, upar);
		 			    
		    //Haven t checked if it is necessarry... might duplicate Straight to parameters for minimum
		    FunctionMinimum min = migrad.minimize();
		    if (min.isValid()) {
		    	double[] res=migrad.params(); //res[0] and res[1] are phi and theta for vec, res[2] is phi for intersection point on cylinder and  res[3] is z_inter
		    	double[] err_res=new double[migrad.covariance().nrow()];
		    	for (int i=0;i<migrad.covariance().nrow();i++) {
		    		err_res[i]=Math.sqrt(migrad.covariance().get(i, i));
		    	}
		    	BMT_geo.Constants.setRxCVT(res[0]);
	    		BMT_geo.Constants.setRyCVT(res[1]);
	    		BMT_geo.Constants.setRzCVT(res[2]);
	    		BMT_geo.Constants.setCxCVT(res[3]);
	    		BMT_geo.Constants.setCyCVT(res[4]);
	    		BMT_geo.Constants.setCzCVT(res[5]);
		    }
	}
	
	public void DoSVTLocAlignment(Barrel_SVT BST, Hipo3DataSource[] reader, int layer, int sector) {
		//Use minimizer
								
			//Create parameters
			MnUserParameters upar = new MnUserParameters();
			double LocTx_1=0;
						
			LocTx_1=BST.getGeometry().getLocTx(layer, sector);
			
			double DeltaTrans=0.5;
			if (LocTx_1!=0) DeltaTrans=0.25;
		
			double TransErr=2*DeltaTrans;
			double LocTx1min=LocTx_1-DeltaTrans; double LocTx1max=LocTx_1+DeltaTrans; 
			//double LocTx2min=LocTx_2-DeltaTrans; double LocTx2max=LocTx_2+DeltaTrans; 
			
			
			upar.add("LocTx1", LocTx_1, TransErr, LocTx1min, LocTx1max);
			//upar.add("LocTx2", LocTx_2, TransErr, LocTx2min, LocTx2max);
									  	    
		    //Create function to minimize
			LocSVTAlign Align=new LocSVTAlign();
		    
		    //Give clusters to Chi2 to compute distance
		    Align.SetLocToAlign(BST,reader,layer,sector);
		    
		 
		    //Create Minuit (parameters and function to minimize)
		    MnMigrad migrad = new MnMigrad(Align, upar);
		 			    
		    //Haven t checked if it is necessarry... might duplicate Straight to parameters for minimum
		    FunctionMinimum min = migrad.minimize();
		    
		    if (min.isValid()) {
		    	double[] res=migrad.params(); //res[0] and res[1] are phi and theta for vec, res[2] is phi for intersection point on cylinder and  res[3] is z_inter
		    	double[] err_res=new double[migrad.covariance().nrow()];
		    	for (int i=0;i<migrad.covariance().nrow();i++) {
		    		err_res[i]=Math.sqrt(migrad.covariance().get(i, i));
		    	}
		    	
		    	BST.getGeometry().setLocTx(layer, sector, res[0]);
		    			    	
		   	}
		    
		    
		
	}

}
