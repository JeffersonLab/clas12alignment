package Particles;

import java.util.ArrayList;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;

public class ParticleEvent {


	/**
	 * List of reconstructed particles
	 */
	private ArrayList<Particle> particles;

	/**
	 * Create an empty particle event
	 */
	public ParticleEvent(){
		particles = new ArrayList<>();
	}

	/**
	 * Create an empty particle event
	 */
	public ParticleEvent(ArrayList<Particle> particles){
		this.particles = particles;
	}
	
	/**
	 * Clear all the particle list
	 */
	public void clear(){
		particles.clear();
	}
	
	/**
	 * Get all the particles of the current event
	 * 
	 * @return the list of all particles
	 */
	public ArrayList<Particle> getParticles() {
		return particles;
	}
	
	/**
	 * Get all the particles with the given pid of the current event
	 * 
	 * @param pid  particle ID to look for
	 * @return the list of all particles with the given pid
	 */
	public ArrayList<Particle> getParticles(int pid){
		ArrayList<Particle> particlesFound = new ArrayList<>();
		for (Particle currentParticle : particles){
			if (currentParticle.getPid()==pid){
				particlesFound.add(currentParticle);
			}
		}
		return particlesFound;
	}
	
	/**
	 * Get all electrons of the current event
	 * 
	 * @return a list of all the electrons
	 */
	public ArrayList<Electron> getElectrons(){
		ArrayList<Electron> particlesFound = new ArrayList<>();
		for (Particle currentParticle : particles){
			if (currentParticle instanceof Electron){
				particlesFound.add( (Electron)currentParticle );
			}
		}
		return particlesFound;
	}
	
	/**
	 * Get all protons of the current event
	 * 
	 * @return a list of all the protons
	 */
	public ArrayList<Proton> getProtons(){
		ArrayList<Proton> particlesFound = new ArrayList<>();
		for (Particle currentParticle : particles){
			if (currentParticle instanceof Proton){
				particlesFound.add( (Proton)currentParticle );
			}
		}
		return particlesFound;
	}
	
	/**
	 * Get all photons of the current event
	 * 
	 * @return a list of all the photons
	 */
	public ArrayList<Photon> getPhotons(){
		ArrayList<Photon> particlesFound = new ArrayList<>();
		for (Particle currentParticle : particles){
			if (currentParticle instanceof Photon){
				particlesFound.add( (Photon)currentParticle );
			}
		}
		return particlesFound;
	}
	
	/**
	 * Get all pions+ of the current event
	 * 
	 * @return a list of all the pions+
	 */
	public ArrayList<PionPlus> getPiPlus(){
		ArrayList<PionPlus> particlesFound = new ArrayList<>();
		for (Particle currentParticle : particles){
			if (currentParticle instanceof PionPlus){
				particlesFound.add( (PionPlus)currentParticle );
			}
		}
		return particlesFound;
	}
	
	/**
	 * Set the list of particles
	 * 
	 * @param particles  list of particles
	 */
	public void setParticles(ArrayList<Particle> particles) {
		this.particles = particles;
	}
	
	/**
	 * Add a list of particles to the current event
	 * 
	 * @param particle 
	 */
	public void addParticle(Particle... newParticles){
		for (Particle particle : newParticles){
			this.getParticles().add(particle);
		}
	}
	
	/**
	 * Get the number of particles of the current event
	 * 
	 * @return the total number of particles
	 */
	public int hasNumberOfParticles(){
		return particles.size();
	}
	
	/**
	 * Get the number of electrons of the current event
	 * 
	 * @return the number of electrons
	 */
	public int hasNumberOfElectrons(){
		int particlesFound = 0;
		for (Particle currentParticle : particles){
			if (currentParticle instanceof Electron){
				particlesFound++;
			}
		}
		return particlesFound;
	}
	
	/**
	 * Get the number of protons of the current event
	 * 
	 * @return the number of protons
	 */
	public int hasNumberOfProtons(){
		int particlesFound = 0;
		for (Particle currentParticle : particles){
			if (currentParticle instanceof Proton){
				particlesFound++;
			}
		}
		return particlesFound;
	}
	
	/**
	 * Get the number of photons of the current event
	 * 
	 * @return the number of photons
	 */
	public int hasNumberOfPhotons(){
		int particlesFound = 0;
		for (Particle currentParticle : particles){
			if (currentParticle instanceof Photon){
				particlesFound++;
			}
		}
		return particlesFound;
	}
	
	/**
	 * Get the number of pion+ of the current event
	 * 
	 * @return the number of pion+
	 */
	public int hasNumberOfPiPlus(){
		int particlesFound = 0;
		for (Particle currentParticle : particles){
			if (currentParticle instanceof PionPlus){
				particlesFound++;
			}
		}
		return particlesFound;
	}
	
	/**
	 * Read the banks of the given event and create particles according to the banks
	 * 
	 * @param event  event to analyze
	 */
	public void readBanks(DataEvent event){
		if (event.hasBank("REC::Particle") == true) {
			DataBank bankParticle = event.getBank("REC::Particle");

			for (int particleIterator = 0; particleIterator < bankParticle
					.rows(); particleIterator++) { /* For all tracks */
				Particle newParticle;
				int pid = bankParticle.getInt("pid", particleIterator);
				if (pid == Electron.pid) {
					newParticle = new Electron();
				} else if (pid == Proton.pid) {
					newParticle = new Proton();
				} else if (pid == Photon.pid) {
					newParticle = new Photon();
				} else if (pid == PionPlus.pid) {
					newParticle = new PionPlus();
				} else {
					newParticle = new Particle();
				}
				newParticle.setChi2pid(bankParticle.getFloat("chi2pid", particleIterator));
				newParticle.setCharge(bankParticle.getByte("charge", particleIterator));
				newParticle.setVertex(bankParticle.getFloat("vx", particleIterator),
						bankParticle.getFloat("vy", particleIterator), bankParticle.getFloat("vz", particleIterator));
				newParticle.setMomentum(bankParticle.getFloat("px", particleIterator),
						bankParticle.getFloat("py", particleIterator), bankParticle.getFloat("pz", particleIterator));
				newParticle.setBeta(bankParticle.getFloat("beta", particleIterator));

				this.addParticle(newParticle);
			}

		}
	}
	
	/**
	 * Read the MC banks of the given event and create particles according to the banks
	 * 
	 * @param event  event to CompareTo
	 */
	public void readMCBanks(DataEvent event){
		particles.clear();
		if (event.hasBank("MC::Particle") == true) {
			DataBank bankParticle = event.getBank("MC::Particle");

			for (int particleIterator = 0; particleIterator < bankParticle
					.rows(); particleIterator++) { /* For all tracks */
				Particle newParticle;
				int pid = bankParticle.getInt("pid", particleIterator);
				if (pid == Electron.pid) {
					newParticle = new Electron();
				} else if (pid == Proton.pid) {
					newParticle = new Proton();
				} else if (pid == Photon.pid) {
					newParticle = new Photon();
				} else if (pid == PionPlus.pid) {
					newParticle = new PionPlus();
				} else {
					newParticle = new Particle();
					newParticle.setPid(pid);
				}
				
				newParticle.setVertex(bankParticle.getFloat("vx", particleIterator),
						bankParticle.getFloat("vy", particleIterator), bankParticle.getFloat("vz", particleIterator));
				newParticle.setMomentum(bankParticle.getFloat("px", particleIterator),
						bankParticle.getFloat("py", particleIterator), bankParticle.getFloat("pz", particleIterator));
				
				this.addParticle(newParticle);
			}

		}
	}
	
}
