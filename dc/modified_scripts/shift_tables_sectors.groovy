/*
 * author Timothy B. Hayward
 * 2018-2020
 * CLAS (12) Collaboration Service Work 
 * (supervised by Mac Mestayer)
 * Modified by Trevor Reed
 * This script has been modified from shift_tables.groovy to display the residuals arrays of 
 * of a single geometry (intended to be for the nominal geometry, but will work for any), 
 * unlike the original script, which displays the difference in residuals
 * between two geometries (intended to be the nominal and a shifted geometry). 
 * This script, as presently constructed, will only display the residuals for one chosen sector 
 */

import java.io.File;

import org.jlab.io.hipo.*;
import org.jlab.io.base.DataEvent;
import org.jlab.clas.physics.*;
import org.jlab.clas12.physics.*; 
import javax.swing.JFrame;
import org.jlab.groot.data.*;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.io.hipo.HipoDataBank;

import org.jlab.groot.math.F1D;
import org.jlab.groot.math.Func1D;
import org.jlab.groot.fitter.DataFitter;

public class shift_tables_sectors {

	public static boolean banks_test(HipoDataEvent event){
		boolean banks_result = true; // check to see if the event has all of the banks present
		// TBHits required for residuals, TBTracks required for angular limit
		// REC::* required for track_check to ensure electron
		if (!(event.hasBank("TimeBasedTrkg::TBHits"))) {
        	banks_result = false;
    	} else if (!(event.hasBank("TimeBasedTrkg::TBTracks"))) {
    		banks_result = false;
    	} else if (!(event.hasBank("TimeBasedTrkg::Trajectory"))) {
    		banks_result = false;
    	} else if (!(event.hasBank("REC::Particle"))) {
    		banks_result = false;
    	} else if (!(event.hasBank("REC::Cherenkov"))) {
    		banks_result = false;
    	} else if (!(event.hasBank("REC::Calorimeter"))) {
    		banks_result = false;
    	}
    	return banks_result;
	}

	public static boolean track_check(HipoDataBank recBank, HipoDataBank ccBank, 
		HipoDataBank calBank) {
		// requirements of CLAS EventBuilder for an electron
		boolean track_check = true;
		if (!(recBank.getFloat("beta", 0)>0)) { // require ftof record a beta 
			track_check=false; 
		}
		for (int ccBankRow = 0; ccBankRow<ccBank.rows(); ccBankRow++) {
			if (ccBank.getInt("pindex", ccBankRow)==0) {
				if (ccBank.getFloat("nphe", ccBankRow)<2) { // require number photoelectrons > 2
					track_check=false;
				}
			}
		}
		double cal_energy = 0;
		for (int calBankRow = 0; calBankRow<calBank.rows(); calBankRow++) {
			if (calBank.getInt("pindex", calBankRow)==0) {
				cal_energy+=calBank.getFloat("energy", calBankRow);
			}
		}
		if (cal_energy<0.06) { // require at least 0.06 energy deposited in the calorimeter
			track_check=false;
		}
		return track_check;
	}

	private static double phi_calculation (float x, float y) {
		// tracks are given with Cartesian values and so must be converted to cylindrical
		double phi = Math.toDegrees(Math.atan2(x,y));
		phi = phi - 90;
		if (phi < 0) {
			phi = 360 + phi;
		}
		phi = 360 - phi;
		return phi;	
	}

	private static double phi_rotate (double phi, int sector) {
		// create a local phi value for each of the sectors
		// spanning [0, 60] with 30 being the midpoint of the sector (local x axis)
		phi = phi + 30;
		if (phi>360) {
			phi = phi - 360;
		}
		return phi-60*(sector-1);
	}

	private static double theta_calculation (float x, float y, float z) {
		// convert cartesian coordinates to polar angle
		double r = Math.pow(Math.pow(x,2)+Math.pow(y,2)+Math.pow(z,2),0.5);
		return (double) Math.acos(z/r);
	}

	public static void main(String[] args) {
		File[] hipo_list_nominal;
		// file list of the nominal geometry 
		if (args.length == 0) {
			// exits program if input directory not specified 
    	   	println("ERROR: Please enter a hipo file directory as the first argument");
    	  	System.exit(0);
    	} else {
    		File directory = new File(args[0]);
    		hipo_list_nominal = directory.listFiles();
    	}

    	int theta_bins, phi_bins;
    	// we will divide the residuals up into bins, these arguments are the number of bins
    	// in theta and phi respectively
    	if (args.length < 3) {
			println("Enter arguements 1-2 as the # of bins for theta and for phi respectively.");
			println("WARNING: One or both of number of bins not specified."); 
			println("Setting both to 1 bin each."); println();
			theta_bins = 1;
			phi_bins = 1;
		} else{
			// if specified, convert to int
			theta_bins = Integer.parseInt(args[1]);
			phi_bins = Integer.parseInt(args[2]);
		}

		def theta_limits = [0];
		def phi_limits = [0];
		// setting the upper bounds for theta and phi bins. you are specifying the upper bound of 
		// the bin, e.g. 30 60 90 would correspond to three bins, [0,30], [30,60], [60,90]
		// the phi bins are specified immediately after the theta bins, where the number of each 
		// was determined in the last step. So, for example, if you had 30 60 90 30 60 that would 
		// be theta bins: [0, 30], [30, 60], [60,90] and phi bins: [0, 30], [30, 60]
		// generally for most of my studies I used phi bins [0,30] and [30,60] to just align both
		// sides of the sector
		if (args.length < 5) {
			// if number of files not specified or too large, set to number of files in directory
			println("WARNING: One or both of limits not specified."); 
			println("Setting theta limits to [0,90] and phi limits to [0,60]."); println();
			theta_limits = [0,90];
			phi_limits = [0,60];
		} else {
			for (int i=0; i<theta_bins; i++) {
				theta_limits[i+1] = Integer.parseInt(args[3+i]);
			}
			for (int i=0; i<phi_bins; i++) {
				phi_limits[i+1] = Integer.parseInt(args[3+theta_bins+i]);
			}
		}

		// print out the bins for error handling
		// println(theta_bins);
		// println(phi_bins);
		// println(theta_limits);
		// println(phi_limits);

		// specify the number of files you want to iterate through in the specified directory
		int n_files; 
		if (args.length < (4+theta_bins+phi_bins)) { 
			// if number of files not specified or too large, set to number of files in directory
			println("WARNING: Number of files not specified or number too large."); 
			println("Setting # of files to be equal to number of files in the directory.");
			println();
			n_files = hipo_list_nominal.size();
			//print("WARNING: Make sure the shift directory has <= files than the nominal directory!")
			println();
		} else {
			// if specified, convert to int
			n_files = Integer.parseInt(args[3+theta_bins+phi_bins]);
		}

		// histogram limits for residuals
		// may need to be adjusted in the future
		int n_bins = 400;
		double min_bin = -0.6*10000;
		double max_bin = 0.6*10000;

		// create the histograms
		H1F[][][] nominal_residuals = new H1F[theta_bins][phi_bins][36];  
		for (int layer = 0; layer < 36; layer++) {
			for (int i = 0; i<theta_bins; i++) {
				for (int j = 0; j<phi_bins; j++) {
					// create the histograms for all 36 layers in each of the theta and phi bins
					nominal_residuals[i][j][layer] = new H1F(" L"+Integer.toString(layer+1),
						n_bins,min_bin,max_bin);
				}
			}
		}

		int counts = 0;
		int num_events = 0;
		// iterate through the files
		for (int current_file; current_file<n_files; current_file++) {
			println(); println(); println("Opening file "+Integer.toString(current_file+1)
				+" out of "+n_files+" of the nominal geometries.");
			HipoDataSource reader = new HipoDataSource();
			// limit to a certain number of files defined by n_files
			reader.open(hipo_list_nominal[current_file]); // open next hipo file

			while(reader.hasEvent()==true){ // cycle through events
				HipoDataEvent event = reader.getNextEvent(); // load next event in the hipo file
				// println(banks_test(event));
				if (banks_test(event)) { // check that the necessary banks are present
					num_events++;
					// load all relevant banks
					HipoDataBank hitBank= (HipoDataBank) event.getBank("TimeBasedTrkg::TBHits");
					HipoDataBank trkBank= (HipoDataBank) event.getBank("TimeBasedTrkg::TBTracks");
					HipoDataBank trajBank=(HipoDataBank) event.getBank("TimeBasedTrkg::Trajectory");
					HipoDataBank recBank= (HipoDataBank) event.getBank("REC::Particle");
					HipoDataBank ccBank= (HipoDataBank) event.getBank("REC::Cherenkov");
					HipoDataBank calBank= (HipoDataBank) event.getBank("REC::Calorimeter");

					// check the track meets requirements defined in the track check function
					if (track_check(recBank, ccBank, calBank)) {
						int sector = trkBank.getInt("sector", 0); 
						//Select which sector is to be examined
						//if(sector != 1) continue;
						//if(sector != 2) continue;
						//if(sector != 3) continue;
						//if(sector != 4) continue;
						//if(sector != 5) continue;
						if(sector != 6) continue;

						// momenta of track
						float px = trkBank.getFloat("p0_x",0);
						float py = trkBank.getFloat("p0_y",0);
						float pz = trkBank.getFloat("p0_z",0);

						// convert the Cartesian coordinates above the cylindrical
						double phi = phi_calculation(px,py);
						double rotated_phi = phi_rotate(phi,sector);
						double theta = Math.toDegrees(theta_calculation(px,py,pz));

						for(int hitBankRow=0; hitBankRow<hitBank.rows(); hitBankRow++){
							// record the residuals
							if (hitBank.getInt("trkID", hitBankRow) == 1) {
								// multiply by 10000 to convert to microns
								float residual = 
									10000*hitBank.getFloat("fitResidual", hitBankRow);
								int superlayer = hitBank.getInt("superlayer", hitBankRow);
								int layer = hitBank.getInt("layer", hitBankRow);

								// fill in the histograms 
								for (int i = 0; i<theta_bins; i++) {
									for (int j = 0; j<phi_bins; j++) {
										counts++;
										if ((theta<theta_limits[i+1]&&theta>theta_limits[i])&&
											(rotated_phi<phi_limits[j+1]&&
											rotated_phi>phi_limits[j])) {
											nominal_residuals[i][j][(superlayer-1)*
												6+layer-1].fill(residual)
										}
									}
								}
							}
						}
					}
				}
			}
		}

		
		// the point is to print out vectors for the means of the residuals for the  
		// nominal geometry
		double[][][][] values = new double[2][theta_bins][phi_bins][36];
		int index = 0;
		print("{")
		for (int i=0; i<theta_bins; i++) {
			for (int j=0; j<phi_bins; j++) {
				// create GraphErrors for each of the layers
				GraphErrors nominal_residuals_points = new GraphErrors();

				print("{");
				for (int layer = 0; layer < 36; layer++) {
					// calculate statistics for each of the layers
					double nominal_mean = nominal_residuals[i][j][layer].getMean();
					double nominal_std = nominal_residuals[i][j][layer].getRMS();
					double nominal_counts = nominal_residuals[i][j][layer].getEntries();
					double nominal_error = nominal_std/Math.sqrt(nominal_counts);

					values[0][i][j][layer] = nominal_mean;
					values[1][i][j][layer] = nominal_std;

					// print out the values of the means of the residuals
					// logical statements are all designed in order to print a suitable array to 
					// be copied into the root file for minimization
					if (layer<35) {
						print((nominal_mean).round(1)+", ");	
					} else {
						if ((i+1==theta_bins)&&(j+1==phi_bins)) {
							print((nominal_mean).round(1)+",");
							print(" }");
							index++;
						} else {
							print((nominal_mean).round(1)+",");
							println(" },");
							index++;
						}
					}
				}
			}
		}
		print("};"); println(); println(); println();
	}
}