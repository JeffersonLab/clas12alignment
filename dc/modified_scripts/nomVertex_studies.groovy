/*
 * This script has been modified from vertex_studies.groovy to show the z vertex
 * histograms of the nominal geometry for all angles theta: 0 to 30 and phi: 0 to 60
 * combined. Running this script produces the histograms for all 6 sectors (separately)
 */

/**
 *
 * @author trevorreed
 */

import java.io.File;

import org.jlab.io.hipo.*;
import org.jlab.io.base.DataEvent;
import org.jlab.clas.physics.*;
import org.jlab.clas12.physics.*;

import javax.swing.JFrame;
import java.awt.Graphics;
import org.jlab.groot.data.*;
import org.jlab.groot.graphics.EmbeddedCanvasTabbed;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.io.hipo.HipoDataBank;

import org.jlab.groot.math.F1D;
import org.jlab.groot.math.Func1D;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.base.GStyle;
import org.jlab.utils.benchmark.ProgressPrintout;

class nomVertex_studies {
	
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
            println("ERROR: Please enter a hipo file directory");
            System.exit(0);
    	} 
        else {
            File directory = new File(args[0]);
            hipo_list = directory.listFiles();
    	}
        
        // Set the number of files to iterate through as the number of files in the specified directory
        int n_files = hipo_list.size();

        // histogram ranges
        int n_bins = 100;
        double min_bin = -150;
        double max_bin = 150;


        // create the histograms for each sector and angular bin
        H1F[] vertex_x0z = new H1F[6];
        for (int sector = 0; sector < 6; sector ++) {
            vertex_x0z[sector] = new H1F(" Sector "+(sector+1),
                    n_bins,min_bin,max_bin);                                      
        }
        

        int run5297Count = 0;
        int usedRunCount = 0;
            ProgressPrintout progress = new ProgressPrintout();
            int counter=-1;

            for (int current_file; current_file < n_files; current_file++) {
                println(); println(); println("Opening file "+Integer.toString(current_file+1)
                    +" out of "+n_files);
                // limit to a certain number of files defined by n_files

                HipoDataSource reader = new HipoDataSource();
                reader.open(hipo_list[current_file]); // open next hipo file

                while(reader.hasEvent()==true){ // cycle through events
                //while(counter < 100000) {
                    counter++;
                    HipoDataEvent event = reader.getNextEvent(); // load next event in the hipo file
                    if (banks_test(event)) { // check that the necessary banks are present
                        HipoDataBank hitBank= (HipoDataBank) event.getBank("TimeBasedTrkg::TBHits");
                        HipoDataBank trkBank= (HipoDataBank) event.getBank("TimeBasedTrkg::TBTracks");
                        HipoDataBank trajBank=(HipoDataBank) event.getBank("TimeBasedTrkg::Trajectory");
                        HipoDataBank recBank= (HipoDataBank) event.getBank("REC::Particle");
                        HipoDataBank ccBank= (HipoDataBank) event.getBank("REC::Cherenkov");
                        HipoDataBank calBank= (HipoDataBank) event.getBank("REC::Calorimeter");
                        HipoDataBank configBank = (HipoDataBank) event.getBank("RUN::config");

                        // check the track meets requirements defined in the track check function
                        if (track_check(recBank, ccBank, calBank)) {
                            int sector = trkBank.getInt("sector", 0);
                            int run = configBank.getInt("run", 0);
                            if (run == 5297) {
                                run5297Count += 1;
                            }
                            if (run != 5297) continue;
                            usedRunCount += 1;
                            // momenta of track
                            float px = trkBank.getFloat("p0_x",0);
                            float py = trkBank.getFloat("p0_y",0);
                            float pz = trkBank.getFloat("p0_z",0);

                            // convert the Cartesian coordinates above to cylindrical
                            float phi = phi_calculation(px,py);
                            double rotated_phi = phi_rotate(phi,sector);
                            float theta = Math.toDegrees(theta_calculation(px,py,pz));

                            float vtx0_z = trkBank.getFloat("Vtx0_z", 0);
                            // fill in the histograms 
                            if ((theta < 30 && theta > 0) &&
                                    (rotated_phi < 60 && rotated_phi > 0)) {
                                //Multiply z vertex by 10 to convert to mm
                                vertex_x0z[sector-1].fill(vtx0_z * 10);                                                                   
                            }
                        }
                    }
                    progress.updateStatus();
                }
                progress.showStatus();
            }
            
            System.out.println("5297 run count = " + run5297Count);
            System.out.println("Total events before filter = " + counter);
            System.out.println("Total events used = " + usedRunCount);
            
        
            // z@x0 refers to the z value of the track at the distance of closest
            // approach to the x=0 plane
            JFrame frame = new JFrame("z@x0");
            frame.setSize(1200, 800);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            EmbeddedCanvas c1 = new EmbeddedCanvas();
            c1.divide(3, 2);
            for (int sector = 0; sector < 6; sector ++) {
                String cname = "Sector " + (sector+1);
                vertex_x0z[sector].unit();
                vertex_x0z[sector].setTitle("Sector " + Integer.toString(sector + 1));
                vertex_x0z[sector].setTitleY("Counts (Normalized)");
                vertex_x0z[sector].setTitleX("z Vertex (mm)");
                c1.cd(sector).draw(vertex_x0z[sector])
                //c1.cd(sector).getAxisY().setRange(0, 1.00001);                  
            }
            frame.add(c1);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
 
    }
    
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
}

