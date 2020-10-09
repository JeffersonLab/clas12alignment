package Analyzer;

import org.jlab.groot.data.*;
import TrackFinder.*;
import org.jlab.groot.ui.*;

import DC_struct.Segment;

import java.util.*;
import Trajectory.StraightLine;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.math.F1D;
import javax.swing.JFrame;

public class BeamAna {
	
	H1F[] vz=new H1F[6];
	H1F[] vy=new H1F[6];
	H1F[] vx=new H1F[6];
	
	H2F xy_beam;
	H1F xz_beam;
	H1F yz_beam;
	
	F1D funcres_x;
	F1D funcres_y;
	F1D funcres_zd;
	F1D funcres_zu;
	F1D funcres_z_sc;
	
	public BeamAna(String type) {
		if (type.equals("CVT")) {
			for (int sec=0;sec<6;sec++) {
				vz[sec]=new H1F("Z-vertex","Z-vertex",120,-55,65);
				vy[sec]=new H1F("Y-vertex","Y-vertex",150,-25,25);
				vx[sec]=new H1F("X-vertex","X-vertex",150,-25,25);
			}
		}
		
		if (type.equals("DC")) {
			for (int sec=0;sec<6;sec++) {
				vz[sec]=new H1F("Z-vertex","Z-vertex",120,-100,200);
				vy[sec]=new H1F("Y-vertex","Y-vertex",150,-25,25);
				vx[sec]=new H1F("X-vertex","X-vertex",150,-25,25);
			}
		}
		
		funcres_x=new F1D("vx", "[amp]*gaus(x,[mean],[sigma])",-25,25);
		funcres_y=new F1D("vy", "[amp]*gaus(x,[mean],[sigma])",-25,25);
		funcres_zu=new F1D("vz_up", "[amp]*gaus(x,[mean],[sigma])",-30,-25);
		funcres_zu.setParameter(0, 2000.);
		funcres_zu.setParameter(1, -25.);
		funcres_zu.setParameter(2, 1.);
		funcres_zd=new F1D("vz_down", "[amp]*gaus(x,[mean],[sigma])",22.5,27.5);
		funcres_zd.setParameter(0, 2000.);
		funcres_zd.setParameter(1, 25.);
		funcres_zd.setParameter(2, 1.);
		funcres_z_sc=new F1D("v_sc", "[amp]*gaus(x,[mean],[sigma])",30,80);
		funcres_z_sc.setParameter(0, 400);
		funcres_z_sc.setParameter(1, 50);
		funcres_z_sc.setParameter(2, 1.);
		
		xz_beam=new H1F("xz_beam angle (degrees)","xz_beam angle (degrees)",50,-2.5,2.5);
		yz_beam=new H1F("yz_beam angle (degrees)","yz_beam angle (degrees)",50,-2.5,2.5);
		
		xy_beam=new H2F("Beam position in z=0",50,-5,5,50,-5,5);
		
		
	}
	
	public void CVTAnalyze(StraightLine Beam, HashMap<Integer, ArrayList<TrackCandidate> > Events) {

		xy_beam.fill(Beam.getPoint().x(), Beam.getPoint().y());
		xz_beam.fill(Math.toDegrees(Math.atan(Beam.getSlope().x())));
		yz_beam.fill(Math.toDegrees(Math.atan(Beam.getSlope().y())));
		
		for (int i=0; i<Events.size();i++) {
			 for (int j=0; j<Events.get(i+1).size();j++) {
				if (Events.get(i+1).get(j).IsFromTarget()) {
					vz[0].fill(Events.get(i+1).get(j).getVertex().z());
					vy[0].fill(Events.get(i+1).get(j).getVertex().y());
					vx[0].fill(Events.get(i+1).get(j).getVertex().x());
					
				}
			 }
		}
	}
	
	public void draw() {
		 TCanvas BeamViewer = new TCanvas("Beam Viewer", 1100, 700);
		 BeamViewer.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		 BeamViewer.divide(3, 1);
		 BeamViewer.cd(0);
		 BeamViewer.draw(xy_beam);
		 BeamViewer.cd(1);
		 BeamViewer.draw(xz_beam);
		 BeamViewer.cd(2);
		 BeamViewer.draw(yz_beam);
		 
		 TCanvas TargetViewer = new TCanvas("Target Viewer", 1100, 700);
		 TargetViewer.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		 TargetViewer.divide(1, 3);
		 TargetViewer.cd(0);
		 TargetViewer.draw(vx[0]);
		 	funcres_x.setParameter(0,vx[0].getMax());
			funcres_x.setParameter(1, vx[0].getMean());
			funcres_x.setParameter(2, vx[0].getRMS());
			DataFitter.fit(funcres_x, vx[0], "Q");
			funcres_x.setOptStat(1100);
			funcres_x.setLineColor(2);
			funcres_x.setLineWidth(2);
			TargetViewer.draw(funcres_x,"same");
		 TargetViewer.cd(1);
		 TargetViewer.draw(vy[0]);
		 	funcres_y.setParameter(0,vy[0].getMax());
			funcres_y.setParameter(1, vy[0].getMean());
			funcres_y.setParameter(2, vy[0].getRMS());
			DataFitter.fit(funcres_y, vy[0], "Q");
			funcres_y.setOptStat(1100);
			funcres_y.setLineColor(2);
			funcres_y.setLineWidth(2);
			TargetViewer.draw(funcres_y,"same");
		 TargetViewer.cd(2);
		 TargetViewer.draw(vz[0]);
		 for (int i=0;i<120;i++) {
			 System.out.println((-54.5+i)+" "+vz[0].getBinContent(i));
		 }
		 /*DataFitter.fit(funcres_zu, vz[0], "Q");
			funcres_zu.setOptStat(1100);
			funcres_zu.setLineColor(2);
			funcres_zu.setLineWidth(2);
			TargetViewer.draw(funcres_zu,"same");
			DataFitter.fit(funcres_zd, vz[0], "Q");
			funcres_zd.setOptStat(1100);
			funcres_zd.setLineColor(2);
			funcres_zd.setLineWidth(2);
			TargetViewer.draw(funcres_zd,"same");
			DataFitter.fit(funcres_z_sc, vz[0], "Q");
			funcres_z_sc.setOptStat(1100);
			funcres_z_sc.setLineColor(2);
			funcres_z_sc.setLineWidth(2);
			TargetViewer.draw(funcres_z_sc,"same");*/
	}

	public void FDAnalyze(StraightLine beam, ArrayList<Segment> FDEvents, int sector) {
		
		for (int j=0; j<FDEvents.size();j++) {
			vz[sector-1].fill(FDEvents.get(j).getVertex().z());
			vy[sector-1].fill(FDEvents.get(j).getVertex().y());
			vx[sector-1].fill(FDEvents.get(j).getVertex().x());
			
		}
		
	}

}
