package Analyzer;

import org.jlab.groot.data.*;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.math.F1D;

import TrackFinder.*;
import org.jlab.groot.ui.TCanvas;
import DC_struct.*;

import java.util.HashMap;

import javax.swing.JFrame;

import org.jlab.geom.prim.Vector3D;

public class DCAna {
	H2F[][] T0_vs_wire=new H2F[6][6];
	
	public DCAna() {
		for (int sec=0; sec<6;sec++) {
			for (int lay=0; lay<6;lay++) {
				T0_vs_wire[sec][lay]=new H2F("T0 for S"+(sec+1)+" L"+(lay+1),"T0 for S"+(sec+1)+" L"+(lay+1),100,0, 500, 112,0,112);
			}
		}
	}
	
	public void FillT0(DriftChambers DC) {
		for (int sec=1;sec<7;sec++) {
			for (int slay=1;slay<7;slay++) {
				for (int lay=1;lay<7;lay++) {
					for (HashMap.Entry<Integer,Wire> m : DC.getSector(sec).getSuperLayer(slay).getLayer(lay).getHitList().entrySet()) {
						T0_vs_wire[sec-1][lay-1].fill(DC.getSector(sec).getSuperLayer(slay).getLayer(lay).getHitList().get(m.getKey()).getTDC(),m.getKey());
					}
				}
			}
		}
	}
	
	public void draw() {
		TCanvas[] DCT0 = new TCanvas[6];
		for (int sec=0;sec<6;sec++) {
			DCT0[sec]=new TCanvas("Sector "+(sec+1),1100, 700);
			DCT0[sec].setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			DCT0[sec].divide(1, 6);
			for (int lay=0;lay<6;lay++) {
				DCT0[sec].cd(lay);
				DCT0[sec].draw(T0_vs_wire[sec][lay]);
			}
		}
	}
}
