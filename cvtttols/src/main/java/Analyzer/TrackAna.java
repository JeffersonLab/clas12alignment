package Analyzer;

import javax.swing.JFrame;

import org.jlab.groot.data.*;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.math.F1D;
import TrackFinder.*;
import org.jlab.groot.ui.TCanvas;
import Analyzer.ClusterAna;

public class TrackAna {
	ClusterAna Clusty;
	H1F Theta_track;
	H1F Phi_track;
	H1F Chi2_track;
	H1F[][] Z_residual=new H1F[3][3];
	H1F[][] C_residual=new H1F[3][3];
	
	public TrackAna() {
		Clusty=new ClusterAna();
		Theta_track=new H1F("Theta angle of track","Theta angle for track",90,0,180);
		Phi_track=new H1F("Phi angle of track","Phi angle for track",90,-180,180);
		Chi2_track=new H1F("Chi2 of track","Chi2 angle for track",90,0,500);
		for (int lay=0;lay<3;lay++) {
			for (int sec=0;sec<3;sec++) {
				Z_residual[lay][sec]=new H1F("Residuals for Z-tile L"+(lay+1)+" S"+(sec+1)+" in mm","Residuals for Z-tile L"+(lay+1)+" S"+(sec+1)+" in mm",100,-0.5,0.5);
				C_residual[lay][sec]=new H1F("Residuals for C-tile L"+(lay+1)+" S"+(sec+1)+" in mm","Residuals for C-tile L"+(lay+1)+" S"+(sec+1)+" in mm",100,-0.5,0.5);
			}
		}
	}
	
	public void analyze(TrackCandidate cand) {
		
		if (cand.get_FitStatus()) {
			Theta_track.fill(Math.toDegrees(Math.acos(cand.get_VectorTrack().z())));
			Phi_track.fill(Math.toDegrees(Math.atan2(cand.get_VectorTrack().y(),cand.get_VectorTrack().x())));
			//if (cand.get_Nc()==3&&cand.get_Nz()==3) Chi2_track.fill(cand.get_chi2());
			Chi2_track.fill(cand.get_chi2());
			for (int clus=0; clus<cand.size();clus++) {
				
				if (cand.IsGoodCandidate()) {
					
					Clusty.analyze(cand);
					
				if (cand.get_Nz()>=2&&(cand.GetBMTCluster(clus).getLayer()==2||cand.GetBMTCluster(clus).getLayer()==3||cand.GetBMTCluster(clus).getLayer()==5)) {
					Z_residual[(cand.GetBMTCluster(clus).getLayer()-1)/2][cand.GetBMTCluster(clus).getSector()-1].fill(cand.getResidual(clus));
					}
					
				if (cand.get_Nc()>=3&&(cand.GetBMTCluster(clus).getLayer()==1||cand.GetBMTCluster(clus).getLayer()==4||cand.GetBMTCluster(clus).getLayer()==6)) {
					C_residual[(cand.GetBMTCluster(clus).getLayer()-1)/2][cand.GetBMTCluster(clus).getSector()-1].fill(cand.getResidual(clus));
				
					}
				}
			}
		}
	}
	
	public void draw() {
		 TCanvas theta = new TCanvas("theta", 1100, 700);
		 theta.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		 theta.draw(Theta_track);
		 TCanvas phi = new TCanvas("phi", 1100, 700);
		 phi.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		 phi.draw(Phi_track);
		 //phi.draw(Chi2_track);
		 TCanvas z_res = new TCanvas("Z layers", 1100, 700);
		 z_res.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		 z_res.divide(3, 3);
		 TCanvas c_res = new TCanvas("C_layers", 1100, 700);
		 c_res.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		 c_res.divide(3, 3);
		 for (int lay=0;lay<3;lay++) {
				for (int sec=0;sec<3;sec++) {
					c_res.cd(3*lay+sec);
					c_res.draw(C_residual[lay][sec]);
					if (C_residual[lay][sec].getEntries()>20) {
						F1D funcres;
						if (main.constant.isMC) funcres=new F1D("resolution", "[amp]*gaus(x,[mean],[sigma])",-0.5,0.5);
						else funcres=new F1D("resolution", "[amp]*gaus(x,[mean],[sigma])",-0.5,0.5);
						funcres.setParameter(0, 100);
						funcres.setParameter(1, C_residual[lay][sec].getMean());
						funcres.setParameter(2, C_residual[lay][sec].getRMS());
						DataFitter.fit(funcres, C_residual[lay][sec], "Q");
						funcres.setOptStat(1100);
						funcres.setLineColor(2);
						funcres.setLineWidth(2);
						c_res.draw(funcres,"same");
					}
					
					z_res.cd(3*lay+sec);
					z_res.draw(Z_residual[lay][sec]);
					if (Z_residual[lay][sec].getEntries()>20) {
						F1D funcres;
						if (main.constant.isMC) funcres=new F1D("resolution", "[amp]*gaus(x,[mean],[sigma])",-0.25,0.25);
						else funcres=new F1D("resolution", "[amp]*gaus(x,[mean],[sigma])",-0.25,0.25);
						funcres.setParameter(0, 100);
						funcres.setParameter(1, Z_residual[lay][sec].getMean());
						funcres.setParameter(2, Z_residual[lay][sec].getRMS());
						DataFitter.fit(funcres, Z_residual[lay][sec], "Q");
						funcres.setOptStat(1100);
						funcres.setLineColor(2);
						funcres.setLineWidth(2);
						z_res.draw(funcres,"same");
					}
				}
		 }
		 
		 //Clusty.draw();
	}
}
