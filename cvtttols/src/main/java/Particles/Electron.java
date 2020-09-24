package Particles;

public class Electron extends Particle {

	/**
	 * Particle mass
	 */
	public static double mass = 0.000511;
	
	/**
	 * Particle ID in LUND convention (11 electron, 22 gamma, 2212 proton, ...)
	 */
	public static int pid = 11;

	/**
	 * Create a new proton
	 */
	public Electron(){
		super();
	}
	
	@Override
	public double getMass(){
		return Electron.mass;
	}
	
	@Override
	public int getPid() {
		return Electron.pid;
	}
	
}
