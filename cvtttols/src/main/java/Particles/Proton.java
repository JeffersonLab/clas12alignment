package Particles;

public class Proton extends Particle {

	/**
	 * Particle mass
	 */
	private static double mass = 0.938272;
	
	/**
	 * Particle ID in LUND convention (11 electron, 22 gamma, 2212 proton, ...)
	 */
	public static int pid = 2212;

	/**
	 * Create a new proton
	 */
	public Proton(){
		super();
	}
	
	@Override
	public double getMass(){
		return Proton.mass;
	}
	
	@Override
	public int getPid() {
		return Proton.pid;
	}
	
}
