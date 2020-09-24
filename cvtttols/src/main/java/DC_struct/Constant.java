package DC_struct;

import org.jlab.detector.calib.utils.DatabaseConstantProvider;

public class Constant {
	public org.jlab.rec.dc.timetodistance.TableLoader CCDBLoader;
	public org.jlab.rec.dc.Constants DCConstants;
	
	//For time jitter correction
	double period;
	int phase;
	int cycles;
	int run;
	
	//For time to dist
	double[][] v0;
	double[][] deltanm;
	double[][] tmax;
	double[][] distbeta;
	double[][] delta_bfield_coefficient;
	double[][] b1; double[][] b2; double[][] b3; double[][] b4; 
	double[][] delta_T0;
		
	@SuppressWarnings("static-access")
	public Constant(){
		DCConstants=new org.jlab.rec.dc.Constants();
		CCDBLoader=new org.jlab.rec.dc.timetodistance.TableLoader();
		period=0;
		phase=0;
		cycles=0;
		v0=new double[DCConstants.NSECT][DCConstants.NSLAY];
		deltanm=new double[DCConstants.NSECT][DCConstants.NSLAY];
		tmax=new double[DCConstants.NSECT][DCConstants.NSLAY];
		distbeta=new double[DCConstants.NSECT][DCConstants.NSLAY];
		delta_bfield_coefficient=new double[DCConstants.NSECT][DCConstants.NSLAY];
		delta_T0=new double[DCConstants.NSECT][DCConstants.NSLAY];
		
		b1=new double[DCConstants.NSECT][DCConstants.NSLAY];
		b2=new double[DCConstants.NSECT][DCConstants.NSLAY];
		b3=new double[DCConstants.NSECT][DCConstants.NSLAY];
		b4=new double[DCConstants.NSECT][DCConstants.NSLAY];
		run=-1; //Means MC
	}
	
	@SuppressWarnings("static-access")
	public void Load(int run, String variation) {
		if (run!=-1) {
			CCDBLoader.FillT0Tables(run, variation);
			DatabaseConstantProvider ToTruth=new DatabaseConstantProvider(run, "variation");
			ToTruth.loadTable("/calibration/dc/time_to_distance/time2dist");
			ToTruth.loadTable("/calibration/dc/time_jitter");
		
			period=ToTruth.getDouble("/calibration/dc/time_jitter/period",0);
			phase=ToTruth.getInteger("/calibration/dc/time_jitter/phase",0);
			cycles=ToTruth.getInteger("/calibration/dc/time_jitter/cycles",0);
		
			for (int sector=0; sector<DCConstants.NSECT; sector++) {
				for (int SL=0; SL<DCConstants.NSLAY; SL++) {
					v0[sector][SL]=ToTruth.getDouble("/calibration/dc/time_to_distance/time2dist/v0",sector*DCConstants.NSLAY+SL);
					deltanm[sector][SL]=ToTruth.getDouble("/calibration/dc/time_to_distance/time2dist/deltanm",sector*DCConstants.NSLAY+SL);
					tmax[sector][SL]=ToTruth.getDouble("/calibration/dc/time_to_distance/time2dist/tmax",sector*DCConstants.NSLAY+SL);
					distbeta[sector][SL]=ToTruth.getDouble("/calibration/dc/time_to_distance/time2dist/distbeta",sector*DCConstants.NSLAY+SL);
					delta_bfield_coefficient[sector][SL]=ToTruth.getDouble("/calibration/dc/time_to_distance/time2dist/delta_bfield_coefficient",sector*DCConstants.NSLAY+SL);
					delta_T0[sector][SL]=ToTruth.getDouble("/calibration/dc/time_to_distance/time2dist/delta_T0",sector*DCConstants.NSLAY+SL);
				
					b1[sector][SL]=ToTruth.getDouble("/calibration/dc/time_to_distance/time2dist/b1",sector*DCConstants.NSLAY+SL);
					b2[sector][SL]=ToTruth.getDouble("/calibration/dc/time_to_distance/time2dist/b2",sector*DCConstants.NSLAY+SL);
					b3[sector][SL]=ToTruth.getDouble("/calibration/dc/time_to_distance/time2dist/b3",sector*DCConstants.NSLAY+SL);
					b4[sector][SL]=ToTruth.getDouble("/calibration/dc/time_to_distance/time2dist/b4",sector*DCConstants.NSLAY+SL);
				}
			}
		}
	}
	
	public double getTriggerPhase(long timestamp) {
		double phase=0;
		if (cycles>0) phase=period*((timestamp+phase)%cycles);
		return phase;
	}
	
	public double getDistance(int sector, int SL, int layer, int wire, int tdc, long timestamp) {
		double dist=(double) tdc*0.0041;
		if (run!=-1) {
			@SuppressWarnings("static-access")
			double time_corr=(double) tdc +this.get_T0(sector, SL, layer, wire, DCConstants.getT0(), DCConstants.getT0Err())[0]-this.getTriggerPhase(timestamp);
			if (time_corr<0&&time_corr>-55) time_corr=0;
			else time_corr=1e6;
			dist=v0[sector-1][SL-1]*time_corr;
		}
		return dist;//cm
	}
	
	private double[] get_T0(int sector, int superlayer, int layer, int wire, double[][][][] T0, double[][][][] T0ERR) {
		double[] T0Corr = new double[2];

		int cable = this.getCableID1to6(layer, wire);
		int slot = this.getSlotID1to7(wire);

		double t0 = T0[sector - 1][superlayer - 1][slot - 1][cable - 1];      //nSec*nSL*nSlots*nCables
		double t0E = T0ERR[sector - 1][superlayer - 1][slot - 1][cable - 1];

		T0Corr[0] = t0;
		T0Corr[1] = t0E;

		return T0Corr;
	}

	private int getSlotID1to7(int wire1to112) {
		return ((wire1to112 - 1) / 16) + 1;
	}

	private int getCableID1to6(int layer1to6, int wire1to112) {
		/*96 channels are grouped into 6 groups of 16 channels and each group 
	joins with a connector & a corresponding cable (with IDs 1,2,3,4,& 6)*/
		int wire1to16 = ((wire1to112 - 1) % 16 + 1);
		return this.CableID[layer1to6 - 1][wire1to16 - 1];
	}

	//Map of Cable ID (1, .., 6) in terms of Layer number (1, ..., 6) and localWire# (1, ..., 16)
	private final int[][] CableID = {
			//[nLayer][nLocWire] => nLocWire=16, 7 groups of 16 wires in each layer
			{1, 1, 1, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 6, 6, 6}, //Layer 1
			{1, 1, 1, 2, 2, 2, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6}, //Layer 2
			{1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 5, 5, 5, 6, 6, 6}, //Layer 3
			{1, 1, 1, 2, 2, 2, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6}, //Layer 4
			{1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 5, 5, 5, 6, 6, 6}, //Layer 5
			{1, 1, 1, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 6, 6, 6}, //Layer 6
			//===> 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
			// (Local wire ID: 0 for 1st, 16th, 32th, 48th, 64th, 80th, 96th wires)
	};
	
}
