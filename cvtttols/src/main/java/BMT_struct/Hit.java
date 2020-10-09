package BMT_struct;

public class Hit {
	int adc_strip;
	float time_strip;
	double radius_strip;
	double phi_strip;
	double err_strip;
	double z_strip;
	
	public Hit() {
		this.set(Float.NaN,Float.NaN,Float.NaN,0,0,0);
	}
	
	public Hit(double radius, double phi, double z,int adc, float time, double err) {
		this.set(radius, phi, z, adc, time, err);
	}
	
	public final void set(double radius, double phi, double z,int adc, float time, double err) {
		radius_strip=radius;
		phi_strip=phi;
		z_strip=z;
		adc_strip=adc;
		time_strip=time;
		err_strip=err;
	}
		
	public int getADC() {
		return adc_strip;
	}
	
	public float getTime() {
		return time_strip;
	}
	
	public double getPhi() {
		return phi_strip;
	}
	
	public double getRadius() {
		return radius_strip;
	}
	
	public double getZ() {
		return z_strip;
	}
	
	public double getErr() {
		return err_strip;
	}
	
	public void print() {
		System.out.println("ADC= "+adc_strip+" and time= "+time_strip+" ns");
	}

}
