package Analyzer;

import org.jlab.groot.data.*;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.math.F1D;

import TrackFinder.*;
import org.jlab.groot.ui.TCanvas;
import BST_struct.*;

import javax.swing.JFrame;

import org.jlab.geom.prim.Vector3D;


public class BSTAna {
	H1F[][] SVT_residual=new H1F[6][18];
	H2F[][] residual_vs_z=new H2F[6][18];
	H1F SVT_LayerHit;
		
	public BSTAna() {
		for (int lay=0; lay<6;lay++) {
			for (int sec=0; sec<18;sec++) {
				if (main.constant.isMC) {
					SVT_residual[lay][sec]=new H1F("Residuals for L"+(lay+1)+" S"+(sec+1)+" in mm","Residuals for L"+(lay+1)+" S"+(sec+1)+" in mm",100,-0.25,0.25);
				}
				if (!main.constant.isMC) {
					SVT_residual[lay][sec]=new H1F("Residuals for L"+(lay+1)+" S"+(sec+1)+" in mm","Residuals for L"+(lay+1)+" S"+(sec+1)+" in mm",100,-0.25,0.25);
				}
				residual_vs_z[lay][sec]=new H2F("Residuals for L"+(lay+1)+" S"+(sec+1)+" in mm","Residuals for L"+(lay+1)+" S"+(sec+1)+" in mm",14,-100, 180, 11,-0.1,0.1);
			}
		}
		SVT_LayerHit=new H1F("Total number of hit per track candidate","Total number of hit per track candidate",12,0,12);
		
	}
	
	public void analyze(Barrel_SVT BST, TrackCandidate cand) {
		
		if (cand.IsGoodCandidate()&&cand.get_FitStatus()) {
//			for (int lay=1;lay<7;lay++) {
//					Vector3D inter=new Vector3D(BST.getGeometry().getIntersectWithRay(lay, cand.get_VectorTrack(), cand.get_PointTrack()));
//					if (!Double.isNaN(inter.x())) {
//					int sec=BST.getGeometry().findSectorFromAngle(lay, inter);
//					double strip=BST.getGeometry().calcNearestStrip(inter.x(), inter.y(), inter.z(), lay, BST.getGeometry().findSectorFromAngle(lay, inter));
//					double ClosestStrip=-20;
//					for (int clus=0;clus<BST.getModule(lay, sec).getClusters().size();clus++) {
//						//System.out.println(BST.getModule(lay, sec).getClusters().size()+" ");
//						if (Math.abs(BST.getModule(lay, sec).getClusters().get(clus+1).getCentroid()-strip)<Math.abs(ClosestStrip-strip)) ClosestStrip=BST.getModule(lay, sec).getClusters().get(clus+1).getCentroid();
//					}
//					double residual=BST.getGeometry().getResidual_line(lay, BST.getGeometry().findSectorFromAngle(lay, inter), ClosestStrip, inter);
//					SVT_residual[lay-1][sec-1].fill(residual);
//					residual_vs_z[lay-1][sec-1].fill(inter.z(),residual);
//				}
//				//System.out.println(BST.getGeometry().getResidual(1, BST.getGeometry().findSectorFromAngle(1, inter), 63, inter));
//			}
			
			for (int clus=0; clus<cand.BSTsize();clus++) {
				int lay=cand.GetBSTCluster(clus).getLayer();
				Vector3D inter=new Vector3D(BST.getGeometry().getIntersectWithRay(cand.GetBSTCluster(clus).getLayer(), cand.GetBSTCluster(clus).getSector(), cand.get_VectorTrack(), cand.get_PointTrack()));
				if (!Double.isNaN(inter.x())) {
				int sec=BST.getGeometry().findSectorFromAngle(lay, inter);
				double strip=BST.getGeometry().calcNearestStrip(inter.x(), inter.y(), inter.z(), lay, BST.getGeometry().findSectorFromAngle(lay, inter));
				double ClosestStrip=-20;
				if (cand.GetBSTCluster(clus).getCentroid()-(int)cand.GetBSTCluster(clus).getCentroid()>0.5) ClosestStrip=cand.GetBSTCluster(clus).getCentroid()+1.;
				else ClosestStrip=cand.GetBSTCluster(clus).getCentroid();
				double residual=BST.getGeometry().getResidual_line(lay, BST.getGeometry().findSectorFromAngle(lay, inter), ClosestStrip, inter);
					SVT_residual[lay-1][sec-1].fill(residual);
					residual_vs_z[lay-1][sec-1].fill(inter.z(),residual);
			}
			}
			SVT_LayerHit.fill(cand.BSTsize());
		}
	}
	
	public void draw() {
		 TCanvas SVTHit = new TCanvas("SVT Layer Hit", 1100, 700);
		 SVTHit.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		 SVTHit.draw(SVT_LayerHit);
		 TCanvas[] residual = new TCanvas[6];
		 for (int lay=0;lay<6;lay++) {
		 residual[lay]= new TCanvas("Residual for layer "+(lay+1), 1100, 700);
		 residual[lay].setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		 residual[lay].divide(4, 5);
		 for (int sec=0;sec<18;sec++) {
			 F1D funcres;
			 if (main.constant.isMC) funcres=new F1D("resolution", "[amp]*gaus(x,[mean],[sigma])",-0.10,0.10);
			 else funcres=new F1D("resolution", "[amp]*gaus(x,[mean],[sigma])",-0.10,0.10);
			
				residual[lay].cd(sec);
				//residual[lay].draw(residual_vs_z[lay][sec]);
				residual[lay].draw(SVT_residual[lay][sec]);
				if (SVT_residual[lay][sec].getEntries()>20) {
					 funcres.setParameter(0, SVT_residual[lay][sec].getMax());
					 funcres.setParameter(1, SVT_residual[lay][sec].getMean());
					 funcres.setParameter(2, SVT_residual[lay][sec].getRMS());
					DataFitter.fit(funcres, SVT_residual[lay][sec], "Q");
					funcres.setOptStat(1100);
					funcres.setLineColor(2);
					funcres.setLineWidth(2);
					residual[lay].draw(funcres,"same");
				}
				//residual[lay].draw(funcres);
				//residual[lay].draw(residual_vs_z[lay][sec]);
					
		 	}
		 }
	}

}
