/*
 * author Timothy B. Hayward
 * 2018-2020
 * CLAS (12) Collaboration Service Work 
 * (supervised by Mac Mestayer)
 */

import java.io.File;

import org.jlab.io.hipo.*;
import org.jlab.io.base.DataEvent;
import org.jlab.clas.physics.*;
import org.jlab.clas12.physics.*;

import javax.swing.JFrame;
import org.jlab.groot.data.*;
import org.jlab.groot.graphics.EmbeddedCanvasTabbed;
import org.jlab.io.hipo.HipoDataBank;

import org.jlab.groot.math.F1D;
import org.jlab.groot.math.Func1D;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.base.GStyle;

import org.jlab.utils.benchmark.ProgressPrintout;

public class vertex_studies {

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
        boolean track_check = true; // will become false if a test is failed
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

        GStyle.getH1FAttributes().setOptStat("1111");
        GStyle.getAxisAttributesX().setTitleFontSize(24);
        GStyle.getAxisAttributesX().setLabelFontSize(18);
        GStyle.getAxisAttributesY().setTitleFontSize(24);
        GStyle.getAxisAttributesY().setLabelFontSize(18);
        GStyle.getAxisAttributesZ().setLabelFontSize(14);
        GStyle.getAxisAttributesX().setLabelFontName("Arial");
        GStyle.getAxisAttributesY().setLabelFontName("Arial");
        GStyle.getAxisAttributesZ().setLabelFontName("Arial");
        GStyle.getAxisAttributesX().setTitleFontName("Arial");
        GStyle.getAxisAttributesY().setTitleFontName("Arial");
        GStyle.getAxisAttributesZ().setTitleFontName("Arial");
        GStyle.setGraphicsFrameLineWidth(1);
        GStyle.getH1FAttributes().setLineWidth(2);

        int read   = 0;
        int window = 1;
        File[] hipo_list;
        String histo_name="histo.hipo";
        
        if (args.length == 0) {
            // exits program if input directory not specified 
            println("ERROR: Please enter 0/1 to read histograms from file or process file list");
            System.exit(0);
    	} 
        else if (args.length == 1) {
            // exits program if input directory not specified 
            println("ERROR: Please enter 0/1 to run in batch mode of open graphical window");
            System.exit(0);
    	} 
        else if (args.length == 2) {
            // exits program if input directory not specified 
            println("ERROR: Please enter a hipo file directory as the third argument");
            System.exit(0);
    	} 
        else {
            read   = Integer.parseInt(args[0]);
            window = Integer.parseInt(args[1]);
            if(window==0) System.setProperty("java.awt.headless", "true");
            if(read==1) {
                histo_name = args[2];
            }
            else { 
                File directory = new File(args[2]);
                hipo_list = directory.listFiles();
            }
    	}
        
    	int theta_bins, phi_bins;
    	// we will divide the residuals up into bins, these arguments are the number of bins
    	// in theta and phi respectively
    	if (args.length < 5) {
			println("Enter arguements 4-5 as the # of bins for theta and for phi respectively.");
			println("WARNING: One or both of number of bins not specified."); 
			println("Setting both to 1 bin each."); println();
			theta_bins = 1;
			phi_bins = 1;
		} else{
			// if specified, convert to int
			theta_bins = Integer.parseInt(args[3]);
			phi_bins = Integer.parseInt(args[4]);
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
        if (args.length < 7) {
            // if number of files not specified or too large, set to number of files in directory
            println("WARNING: One or both of limits not specified."); 
            println("Setting theta limits to [0,90] and phi limits to [0,60]."); println();
            theta_limits = [0,90];
            phi_limits = [0,60];
        } else {
            for (int i=0; i<theta_bins; i++) {
                theta_limits[i+1] = Integer.parseInt(args[5+i]);
            }
            for (int i=0; i<phi_bins; i++) {
                phi_limits[i+1] = Integer.parseInt(args[5+theta_bins+i]);
            }
        }

        // specify the number of files you want to iterate through in the specified directory
        int n_files; 
        if(read==0) {
            if (args.length < (7+theta_bins+phi_bins)) { 
                // if number of files not specified or too large, set to number of files in directory
                println("WARNING: Number of files not specified or number too large."); 
                println("Setting # of files to be equal to number of files in the directory.");
                println();
                n_files = hipo_list.size();
                print("WARNING: Make sure the shift directory has <= files than the nominal directory!")
                println();
            } else {
                // if specified, convert to int
                n_files = Integer.parseInt(args[6+theta_bins+phi_bins]);
            }
        }

        // histogram ranges
        // may need to be updated in the future
        int n_bins = 100;
        double min_bin = -9.999;
        double max_bin = 9.999;

        // create the histograms for each sector and angular bin
        H1F[][][] vertex_x0z = new H1F[6][theta_bins][phi_bins];
        for (int sector = 0; sector < 6; sector ++) {
            for (int i = 0; i<theta_bins; i++) {
                for (int j = 0; j<phi_bins; j++) {
                    vertex_x0z[sector][i][j] = new H1F(i + " " + j +" S"+ (sector+1),
                                                    "#theta "+i+" #phi "+j+" Sector "+(sector+1),
                                                      n_bins,min_bin,max_bin);
                    vertex_x0z[sector][i][j].setTitleX("Vtx0_z (cm)");
                    vertex_x0z[sector][i][j].setTitleY("Counts (Normalized)");                        
                }
            }
        }

        // read histograms from file
        if(read==1) {
            System.out.println("Opening file: " + histo_name);
            TDirectory dir = new TDirectory();
            dir.readFile(histo_name);
            System.out.println(dir.getDirectoryList());
            dir.cd();
            dir.pwd();
            for (int sector = 0; sector < 6; sector ++) {
                for (int i = 0; i<theta_bins; i++) {
                    for (int j = 0; j<phi_bins; j++) {
                        vertex_x0z[sector][i][j] = dir.getObject("Sector"+(sector+1), vertex_x0z[sector][i][j].getName());
                    }
                }
            }
        }
        // or process input file
        else {

            ProgressPrintout progress = new ProgressPrintout();
            int counter=-1;

            for (int current_file; current_file<n_files; current_file++) {
                println(); println(); println("Opening file "+Integer.toString(current_file+1)
                    +" out of "+n_files);
                // limit to a certain number of files defined by n_files

                HipoDataSource reader = new HipoDataSource();
                reader.open(hipo_list[current_file]); // open next hipo file

                while(reader.hasEvent()==true){ // cycle through events
                    counter++;
                    HipoDataEvent event = reader.getNextEvent(); // load next event in the hipo file
                    if (banks_test(event)) { // check that the necessary banks are present
                        HipoDataBank hitBank= (HipoDataBank) event.getBank("TimeBasedTrkg::TBHits");
                        HipoDataBank trkBank= (HipoDataBank) event.getBank("TimeBasedTrkg::TBTracks");
                        HipoDataBank trajBank=(HipoDataBank) event.getBank("TimeBasedTrkg::Trajectory");
                        HipoDataBank recBank= (HipoDataBank) event.getBank("REC::Particle");
                        HipoDataBank ccBank= (HipoDataBank) event.getBank("REC::Cherenkov");
                        HipoDataBank calBank= (HipoDataBank) event.getBank("REC::Calorimeter");

                        // check the track meets requirements defined in the track check function
                        if (track_check(recBank, ccBank, calBank)) {
                            int sector = trkBank.getInt("sector", 0);

                            // momenta of track
                            float px = trkBank.getFloat("p0_x",0);
                            float py = trkBank.getFloat("p0_y",0);
                            float pz = trkBank.getFloat("p0_z",0);

                            // convert the Cartesian coordinates above the cylindrical
                            float phi = phi_calculation(px,py);
                            double rotated_phi = phi_rotate(phi,sector);
                            float theta = Math.toDegrees(theta_calculation(px,py,pz));

                            float vtx0_z = trkBank.getFloat("Vtx0_z", 0);
                            // fill in the histograms 
                            for (int i = 0; i<theta_bins; i++) {
                                for (int j = 0; j<phi_bins; j++) {
                                    if ((theta<theta_limits[i+1]&&theta>theta_limits[i])&&
                                        (rotated_phi<phi_limits[j+1]&&
                                            rotated_phi>phi_limits[j])) {
                                        vertex_x0z[sector-1][i][j].fill(vtx0_z);
                                    }
                                }
                            }
                        }
                    }
                    progress.updateStatus();
                }
                progress.showStatus();
            }

            // normalize and save histograms
            TDirectory dir = new TDirectory();
            System.out.println("Saving histograms to file " + histo_name);
            String folder = "/";
            dir.mkdir(folder);
            for(int sector = 0; sector < 6; sector ++) {
                String subfolder = folder + "Sector" + (sector+1);
                dir.mkdir(subfolder);
                dir.cd(subfolder);
                for (int i = 0; i<theta_bins; i++) {
                    for (int j = 0; j<phi_bins; j++) {
                        vertex_x0z[sector][i][j].unit()
                        dir.addDataSet(vertex_x0z[sector][i][j]);
                    }
                }
            }
            dir.writeFile(histo_name);
        }
        
        
        if(window) {
            // z@x0 refers to the z value of the track at the distance of closest
            // approach to the x=0 plane
            JFrame frame = new JFrame("z@x0");
            frame.setSize(1200, 800);
            EmbeddedCanvasTabbed canvas = null;
            for (int sector = 0; sector < 6; sector ++) {
                String cname = "Sector " + (sector+1);
                if(canvas==null) canvas = new EmbeddedCanvasTabbed(cname);
                else             canvas.addCanvas(cname);
                for (int i = 0; i<theta_bins; i++) {
                    for (int j = 0; j<phi_bins; j++) {
                        canvas.getCanvas(cname).divide(theta_bins,phi_bins);
                        canvas.getCanvas(cname).cd(j*theta_bins+i);
                        vertex_x0z[sector][i][j].unit();
                        fitVertex(vertex_x0z[sector][i][j]);
                        canvas.getCanvas(cname).draw(vertex_x0z[sector][i][j]);
                        canvas.getCanvas(cname).getPad(j*theta_bins+i).getAxisY().setRange(0,1.00001);
                    }
                }
            }
            frame.add(canvas);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }
 
    }
    
    
    
    public static void fitVertex(H1F histo) {
        F1D f1_vtx   = new F1D("f1vertex","[amp]*gaus(x,[mean],[sigma])", -10, 10);
        f1_vtx.setLineColor(2);
        f1_vtx.setLineWidth(2);
        f1_vtx.setOptStat("1111");
        double mean  = histo.getDataX(histo.getMaximumBin());
        double amp   = histo.getBinContent(histo.getMaximumBin());
        double sigma = histo.getRMS();
        f1_vtx.setParameter(0, amp);
        f1_vtx.setParameter(1, mean);
        f1_vtx.setParameter(2, sigma);
        f1_vtx.setRange(mean-2.0*sigma,mean+2.0*sigma);
        DataFitter.fit(f1_vtx, histo, "Q"); //No options uses error for sigma
    }
}