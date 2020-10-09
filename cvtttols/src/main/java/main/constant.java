package main;

import Trajectory.StraightLine;

public class constant {
	
	public static double solenoid_scale=0;
	public static boolean isLoaded=false;
	public static boolean isMC=false;
	public static boolean efficiency=false;
	public static boolean isCosmic=false;
	public static String TrackerType;
	public static StraightLine IdealBeam;
	public static int max_event=Integer.MAX_VALUE;
	public static boolean drawing=false;
	
	public static double getSolenoidscale() {
		return solenoid_scale;
	}
	
	public static void setSolenoidscale(double scale) {
		solenoid_scale=scale;
		IdealBeam=new StraightLine();
		IdealBeam.setPoint_XYZ(0, 0, 0);
		IdealBeam.setSlope_XYZ(0, 0, 1);
	}
	
	public static boolean IsMC() {
		return isMC;
	}
	
	public static void setMC(boolean is) {
		isMC=is;
	}
	
	public static void setLoaded(boolean is) {
		isLoaded=is;
	}
	
	public static boolean IsCosmic() {
		return isCosmic;
	}
	
	public static void setCosmic(boolean is) {
		isCosmic=is;
	}
	
	//public static void IncludeSVT(boolean is) {
		//WithSVT=is;
	//}
	
	//public static boolean IsWithSVT() {
		//return WithSVT;
	//}
	
	public static void setTrackerType(String Type) {
		if (!Type.equals("SVT")&&!Type.equals("MVT")&&!Type.equals("CVT")) {
			System.err.println("Invalid Tracker Type entered :");
			System.err.println(Type);
			System.err.println("Must be SVT, MVT or CVT");
			System.err.println("More info by typing execution line with no argument");
			System.exit(0);
		}
		TrackerType=Type;
	}
	
}
