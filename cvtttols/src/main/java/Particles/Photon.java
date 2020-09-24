package Particles;

public class Photon extends Particle {

	/**
	 * Particle mass
	 */
	public static double mass = 0;
	
	/**
	 * Particle ID in LUND convention (11 electron, 22 gamma, 2212 proton, ...)
	 */
	public static int pid = 22;

	/**
	 * Create a new proton
	 */
	public Photon(){
		super();
	}
	
	@Override
	public double getMass(){
		return Photon.mass;
	}
	
	@Override
	public int getPid() {
		return Photon.pid;
	}
	
}
