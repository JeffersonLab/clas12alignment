package Analyzer;

import java.util.HashMap;
import BST_struct.*;
import TrackFinder.*;
import org.jlab.io.base.DataBank;
import Particles.*;

public class Analyzer {
	TrackAna Trackmeter;
	BSTAna Simeter;
	MCAna MCmeter;
	
	public Analyzer() {
		Trackmeter=new TrackAna();
		Simeter=new BSTAna();
		MCmeter= new MCAna();
	}
	
	public void analyze(Barrel_SVT BST , HashMap<Integer,TrackCandidate> candidates, ParticleEvent MCParticle) {
		for (int i=0;i<candidates.size();i++) {
			if (candidates.get(i+1).IsGoodCandidate()) {
				Trackmeter.analyze(candidates.get(i+1));
				Simeter.analyze(BST, candidates.get(i+1));
				MCmeter.analyze(MCParticle,candidates.get(i+1));
			}
		}
	}
	
	public void draw() {
		Trackmeter.draw();
		Simeter.draw();
		MCmeter.draw();
	}
	
}
