package Particles;

import org.jlab.clas.physics.Vector3;

public class LorentzVector {

	/**
	 * Momentum vector component
	 */
	private Vector3 vector;
	
	/**
	 * Energy/time component
	 */
	private double energy;

	/**
	 * Create Lorentz-vector (0,0,0,0)
	 */
	public LorentzVector(){
		this.vector = new Vector3(0, 0, 0);
		this.energy = 0;
	}
	
	/**
	 * Create Lorentz-vector as a copy of a given Lorentz-vector
	 * 
	 * @param vect  Lorentz-vector to copy
	 */
	public LorentzVector(LorentzVector vect){
		this.vector = new Vector3(vect.px(), vect.py(), vect.pz());
		this.energy = vect.e();
	}
	/**
	 * Create Lorentz-vector with given momentum-vector and energy/time
	 * 
	 * @param momentum  momentum-vector
	 * @param energy  energy
	 */
	public LorentzVector(Vector3 momentum, double energy) {
		this.vector = momentum;
		this.energy = energy;
	}
	
	/**
	 * Copy the given Lorentz-vector
	 * 
	 * @param vect  Lorentz-vector to copy
	 */
	public void copy(LorentzVector vect) {
		this.vector.setXYZ(vect.px(), vect.py(), vect.pz());
		this.energy = vect.e();
	}

	/**
	 * Set Lorentz-vector components.
	 * 
	 * @param px  x-component
	 * @param py  y-component
	 * @param pz  z-component
	 * @param energy  energy/time-component
	 */
	public void setPxPyPzE(double px, double py, double pz, double energy) {
		this.vector.setXYZ(px, py, pz);
		this.energy = energy;
	}

	/**
	 * Set Lorentz-vector components. Computes energy given the mass.
	 * 
	 * @param px  momentum x-component
	 * @param py  momentum y-component
	 * @param pz  momentum z-component
	 * @param mass  mass
	 */
	public void setPxPyPzM(double px, double py, double pz, double mass) {
		this.vector.setXYZ(px, py, pz);
		this.energy = Math.sqrt(mass * mass + this.vector.mag2());
	}
	
	/**
	 * Set Lorentz-vector components.
	 * 
	 * @param x  x-component
	 * @param y  y-component
	 * @param z  z-component
	 * @param t  time/energy-component
	 */
	public void setXYZT(double x, double y, double z, double t) {
		this.setPxPyPzE(x, y, z, t);
	}
	
	/**
	 * Set Lorentz-vector components. Computes energy given the mass.
	 * 
	 * @param momentum  momentum-vector
	 * @param mass  mass
	 */
	public void setVectM(Vector3 momentum, double mass) {
		vector = momentum;
		energy = Math.sqrt(mass * mass + vector.mag2());
	}


	/**
	 * Get momentum x-component
	 * 
	 * @return momentum x-component
	 */
	public double px() {
		return this.vector.x();
	}

	/**
	 * Get momentum y-component 
	 * 
	 * @return momentum y-component 
	 */
	public double py() {
		return this.vector.y();
	}

	/**
	 * Get momentum z-component 
	 * 
	 * @return momentum z-component 
	 */
	public double pz() {
		return this.vector.z();
	}

	/**
	 * Get Lorentz-vector momentum
	 * 
	 * @return Lorentz-vector momentum
	 */
	public double p() {
		return this.vector.mag();
	}
	
	/**
	 * Get Lorentz-vector energy
	 * 
	 * @return Lorentz-vector energy
	 */
	public double e() {
		return this.energy;
	}

	/**
	 * Get momentum-vector theta
	 * 
	 * @return momentum-vector theta
	 */
	public double theta() {
		return this.vector.theta();
	}

	/**
	 * Get momentum-vector phi
	 * 
	 * @return momentum-vector phi
	 */
	public double phi() {
		return this.vector.phi();
	}

	/**
	 * Get Lorentz-vector mass
	 * 
	 * @return Lorentz-vector mass
	 */
	public double m() {
		double m2 = this.m2();
		if (m2 < 0)
			return -Math.sqrt(-m2);
		return Math.sqrt(m2);
	}
	
	/**
	 * Get Lorentz-vector mass
	 * 
	 * @return Lorentz-vector mass
	 */
	public double mass() {
		return this.m();
	}
	
	/**
	 * Get the Lorentz-vector squared magnitude
	 * 
	 * @return Lorentz-vector squared magnitude
	 */
	public double m2() {
		return (this.e() * this.e() - this.vector.mag2());
	}
	
	/**
	 * Get the Lorentz-vector squared magnitude
	 * 
	 * @return Lorentz-vector squared magnitude
	 */
	public double mass2() {
		return this.m2();
	}
	
	/**
	 * Get momentum-vector
	 * 
	 * @return momentum-vector
	 */
	public Vector3 vect() {
		return this.vector;
	}

	/**
	 * Rotate around X axis
	 * 
	 * @param angle  angle to rotate (radian)
	 */
	public void rotateX(double angle) {
		this.vector.rotateX(angle);
	}

	/**
	 * Rotate around Y axis
	 * 
	 * @param angle  angle to rotate (radian)
	 */
	public void rotateY(double angle) {
		this.vector.rotateY(angle);
	}

	/**
	 * Rotate around Z axis
	 * 
	 * @param angle  angle to rotate (radian)
	 */
	public void rotateZ(double angle) {
		this.vector.rotateZ(angle);
	}
	
	/**
	 * Get a vector of momentum divided by energy
	 * 
	 * @return vector made with momentum divided by energy
	 */
	public Vector3 boostVector() {
		if (this.e() == 0)
			return new Vector3(0., 0., 0.);
		return new Vector3(this.px() / this.e(), this.py() / this.e(), this.pz() / this.e());
	}

	/**
	 * Lorentz boost the current Lorentz-vector
	 * 
	 * @param bx  boost x-coordinate
	 * @param by  boost y-coordinate
	 * @param bz  boost z-coordinate
	 */
	public void boost(double bx, double by, double bz) {
		double b2 = bx * bx + by * by + bz * bz;
		double gamma = 1.0 / Math.sqrt(1.0 - b2);
		// System.out.println("GAMMA = " + gamma + " b2 = " + b2);
		double bp = bx * px() + by * py() + bz * pz();
		double gamma2 = b2 > 0 ? (gamma - 1.0) / b2 : 0.0;

		this.vector.setXYZ(px() + gamma2 * bp * bx + gamma * bx * e(), py() + gamma2 * bp * by + gamma * by * e(),
				pz() + gamma2 * bp * bz + gamma * bz * e());
		this.energy = gamma * (e() + bp);
	}

	/**
	 * Lorentz boost the current Lorentz-vector
	 * 
	 * @param vect  boost-vector
	 */
	public void boost(Vector3 vect) {
		boost(vect.x(), vect.y(), vect.z());
	}

	/**
	 * Add a Lorentz-vectors to the current Lorentz-vector
	 * 
	 * @param vects  Lorentz-vectors to add
	 */
	public void add(LorentzVector... vects) {
		for (LorentzVector vect : vects) {
			this.vector.add(vect.vect());
			this.energy = this.e() + vect.e();
		}
	}

	/**
	 * Substract Lorentz-vectors to the current Lorentz-vector
	 * 
	 * @param vectors  Lorentz-vectors to substract
	 */
	public void sub(LorentzVector... vectors) {
		for (LorentzVector vect : vectors) {
			this.vector.sub(vect.vect());
			this.energy = this.e() - vect.e();
		}
	}
	
	/**
	 * Multiply the current Lorentz-vector by a factor
	 * 
	 * @param factor  multiplicative factor
	 */
	public void fact(double factor) {
		this.setPxPyPzE(factor * this.px(), factor * this.py(), factor * this.pz(), factor * this.e());
	}
	
	/**
	 * Get the opposite Lorentz-vector
	 * 
	 * @deprecated
	 */
	public void invert() {
		this.vector.setXYZ(-this.vector.x(), -this.vector.y(), -this.vector.z());
		this.energy = -this.energy;
	}
	
	/**
	 * Create a new Lorentz-vector as the sum of the current Lorentz-vector and a list of Lorentz-vectors
	 * 
	 * @param vects  Lorentz-vectors to sum
	 * @return the summed Lorentz-vector
	 */
	public LorentzVector sum(LorentzVector... vects) {
		LorentzVector newVector = new LorentzVector(this);
		newVector.add(vects);
		return newVector;
	}

	/**
	 * Create a new Lorentz-vector as the substraction of the current Lorentz-vector by a list of Lorentz-vectors
	 * 
	 * @param vects  Lorentz-vectors to substract
	 * @return the substracted Lorentz-vector
	 */
	public LorentzVector substract(LorentzVector... vects) {
		LorentzVector newVector = new LorentzVector(this);
		newVector.sub(vects);
		return newVector;
	}

	/**
	 * Create a new Lorentz-vector as the multiplication of the current Lorentz-vector and a factor
	 * 
	 * @param factor  value to multiply
	 * @return the multiplied Lorentz-vector
	 */
	public LorentzVector multiply(double factor) {
		LorentzVector newVector = new LorentzVector(this);
		this.fact(factor);
		return newVector;
	}
	
	/**
	 * Compute the dot product of the current Lorentz-vector and the given one
	 * 
	 * @param vect  Lorentz-vector to multiply
	 * @return the dot product
	 */
	public double dot(LorentzVector vect){
		double dotProduct = this.e()*vect.e() - this.vect().dot(vect.vect());
		return dotProduct;
	}

	/**
	 * Returns a string containing the current Lorentz-Vector components
	 */
	public String toString() {
		String toString = null;
		toString = "[" + this.px() + ", " + this.py() + ", " + this.pz() + ", " + this.e() + "]";
		return toString;
	}
	
	/**
	 * Print Lorentz-vector informations
	 * 
	 * @deprecated
	 */
	public void print() {
		System.out.format("L Vect : %12.6f %12.6f %12.6f %12.6f %12.6f\n", this.px(), this.py(), this.pz(), this.p(), this.mass());
	}

}
