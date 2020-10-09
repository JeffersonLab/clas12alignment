package Analyzer;

import org.jlab.groot.data.*;
import TrackFinder.*;
import org.jlab.groot.ui.TCanvas;
import Particles.*;

public class MCAna {
	H1F Theta_res;
	H1F Phi_res;
	H1F Phipt_res;
	H1F Thetapt_res;
	
		public MCAna() {
			Theta_res=new H1F("Theta angle of track","Theta angle for track",240,-3e-2,3e-2);
			Phi_res=new H1F("Phi angle of track","Phi angle for track",240,-3e-2,3e-2);
		}
		
		public void analyze(ParticleEvent MCParticle, TrackCandidate cand) {
			//Try to find the particles and look if the track is reconstructed
			if (cand.IsGoodCandidate()) {
				for (int i=0; i< MCParticle.getParticles().size();i++) {
					double phi_part=Math.atan2(MCParticle.getParticles().get(i).getPy(),MCParticle.getParticles().get(i).getPx());
					double theta_part=Math.acos(MCParticle.getParticles().get(i).getPz()/MCParticle.getParticles().get(i).getMomentum().mag());
					Phi_res.fill(phi_part-Math.atan2(cand.get_VectorTrack().y(),cand.get_VectorTrack().x()));
					Theta_res.fill(theta_part-Math.acos(cand.get_VectorTrack().z()));
				}
			}
			
//			if (Math.acos(MCParticle.getParticles().get(0).getPz()/MCParticle.getParticles().get(0).getMomentum().mag())>Math.toRadians(40)&&Math.acos(MCParticle.getParticles().get(0).getPz()/MCParticle.getParticles().get(0).getMomentum().mag())<Math.toRadians(60)) {
//				System.out.println(cand.get_Nc()+" "+Math.acos(MCParticle.getParticles().get(0).getPz()/MCParticle.getParticles().get(0).getMomentum().mag()));
//			}
			
		}
		
		public void draw() {
			if (Theta_res.getEntries()!=0&&Phi_res.getEntries()!=0) {
				TCanvas theta_res = new TCanvas("theta", 1100, 700);
				theta_res.draw(Theta_res);
				TCanvas phi_res = new TCanvas("phi", 1100, 700);
				phi_res.draw(Phi_res);
			}
		}
}
