/*
 * This file is modified from shift_tables.groovy to display various plots of the nominal 
 * geometry sector residuals. This script simply takes the input data directory as the only input.
 * To run this script: run-groovy nominalSectorResiduals.groovy /path/to/data_directory/
 * Any plots that do not explicitly state the angle bins are done for theta: 0 to 30 and
 * phi: 0 to 60
 */

/**
 *
 * @author trevorreed
 */

import java.io.File;
import javax.swing.JTabbedPane;
import javax.swing.JLabel;
import javax.swing.JSplitPane;
import javax.swing.JPanel;

import org.jlab.io.hipo.*;
import org.jlab.io.base.DataEvent;
import org.jlab.clas.physics.*;
import org.jlab.clas12.physics.*; 
import javax.swing.JFrame;
import org.jlab.groot.data.*;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.io.hipo.HipoDataBank;
import org.jlab.groot.ui.TCanvas

import org.jlab.groot.math.F1D;
import org.jlab.groot.math.Func1D;
import org.jlab.groot.fitter.DataFitter;

public class nominalSectorResiduals {

    public static void main(String[] args) {
	File[] hipo_list_nominal;
	// file list of the nominal geometry 
	if (args.length == 0) {
            // exits program if input directory not specified 
    	    println("ERROR: Please enter a hipo file directory as the first argument");
    	    System.exit(0);
        } 
        else {
            File directory = new File(args[0]);
            hipo_list_nominal = directory.listFiles();
        }
	 
    int theta_bins, phi_bins;
    int n_files = hipo_list_nominal.size(); 
        

    // histogram limits for residuals
	// may need to be adjusted in the future
	int n_bins = 400;
    double min_bin = -6;    //Histograms examined in units of mm
	double max_bin = 6;
	
    //TCanvas testCan = new TCanvas("", 1000, 700);
	// create the histograms into which the residuals will be plotted and fit

    H1F[][] nominal_residuals = new H1F[36][6];
	for (int layer = 0; layer < 36; layer++) {
            for (int sec = 0; sec < 6; sec++) {
                // create the histograms for all 36 layers and 6 sectors
                nominal_residuals[layer][sec] = new H1F(" L" + Integer.toString(layer + 1) + " S" + Integer.toString(sec + 1),
                    n_bins, min_bin, max_bin);
                if (layer == 0 && sec == 0) {                  
                    //H1F testPlot = new H1F("Sector 1, Layer 1", n_bins, -500, 500);	        
                    //testCan.draw(nominal_residuals[0][0]);
                }
            }
	}
        
    // create the histograms into which the residuals will be plotted and fit, but now
    //by specific theta increments
    H1F[][][] nom_resids_byAngle = new H1F[36][6][4];
	for (int layer = 0; layer < 36; layer++) {
            for (int sec = 0; sec < 6; sec++) {
                for (int thetaGroup = 0; thetaGroup < 4; thetaGroup++) {
                    // create the histograms for all 36 layers, 6 sectors, and 4 theta groups
                    nom_resids_byAngle[layer][sec][thetaGroup] = new H1F(" L" + Integer.toString(layer + 1) + " S" + Integer.toString(sec + 1),
                        n_bins, min_bin, max_bin);
                }
            }
	}
	
    int n_bins2 = 400;
	double min_bin2 = -3000;
	double max_bin2 = 3000;

    //Create histograms to be filled with residuals for each layer and each sector
    H1F[][] nominal_residuals2 = new H1F[36][6];
	for (int layer = 0; layer < 36; layer++) {
            for (int sec = 0; sec < 6; sec++) {
                // create the histograms for all 36 layers and 6 sectors
                nominal_residuals2[layer][sec] = new H1F(" L" + Integer.toString(layer + 1) + " S" + Integer.toString(sec + 1),
                    n_bins2, min_bin2, max_bin2);
                //nominal_residuals2[layer][sec].getStatBox();
            }
	}
        
        //Creates the histograms to be filled with residuals for each layer, combining all 6 sectors
        H1F[] nominal_residuals3 = new H1F[36];
	for (int layer = 0; layer < 36; layer++) {
            // create the histograms for each of the 36 layers, all 6 sectors combined
            nominal_residuals3[layer] = new H1F(" L" + Integer.toString(layer + 1),
                n_bins2, min_bin2, max_bin2);           
	}
        
        //Create the JFrame where the tabbed histogram plots, (one tab for each sector, 
        //each tab showing redsiduals histos for each layer), will be printed
        //Also creates the JTabbedPane method. Each pane will fill one tab
        JFrame tabbedFrame = new JFrame("theta: 0 to 30, phi: 0 to 60");
        tabbedFrame.setSize(1200,750);
        tabbedFrame.setVisible(true);
        JTabbedPane tabbedPane = new JTabbedPane();
        
        //Creates the JFrame for showing the layer vs mean residuals by sector scatterplots 
        JFrame frame = new JFrame("Layer vs Mean Residual by Sector");
        EmbeddedCanvas c1 = new EmbeddedCanvas();
        frame.setSize(1200,750);
        c1.divide(3, 2);
        GraphErrors[] sectorResid = new GraphErrors[6];
        for (int sec = 0; sec < 6; sec++) {
            sectorResid[sec] = new GraphErrors("Sector " + Integer.toString(sec + 1));
            sectorResid[sec].setMarkerSize(2);
            //sectorResid[sec].getAxisX().setRange(-1.5, 1.5);
        }
        
        //Again, layer vs mean residual scatterplots for each sector,
        //but now using mean from gaussian fit
        JFrame frame_gaus = new JFrame("Layer vs Mean Residual (from Gaus fit) by Sector");
        frame_gaus.setSize(1200,750);
        EmbeddedCanvas gaus_can = new EmbeddedCanvas();
        gaus_can.divide(3, 2);
        GraphErrors[] sectorResid_gaus = new GraphErrors[6];
        for (int sec = 0; sec < 6; sec++) {
            sectorResid_gaus[sec] = new GraphErrors("Sector " + Integer.toString(sec + 1));
            sectorResid_gaus[sec].setMarkerSize(2);
        }
        
        //Create the JFrame where the tabbed histogram plots, (one tab for each sector, 
        //each tab showing redsiduals histos for each SUPERlayer), will be printed
        //Also creates the JTabbedPane method. Each pane will fill one tab
        JFrame tabbedFrame4 = new JFrame("Superlayer vs Residuals");
        tabbedFrame4.setSize(1200,750);
        tabbedFrame4.setVisible(true);
        JTabbedPane tabbedPane4 = new JTabbedPane();
        
        //Creates the histograms to be filled with residuals for each SUPERlayer, combining all 6 sectors
        H1F[][] nominal_residuals4 = new H1F[6][6];
	for (int sLayer = 0; sLayer < 6; sLayer++) {
            for (int sec = 0; sec < 6; sec++) {
                // create the histograms for all 6 SUPERlayers and 6 sectors
                nominal_residuals4[sLayer][sec] = new H1F(" SL" + Integer.toString(sLayer + 1) + " S" + Integer.toString(sec + 1),
                    n_bins2, min_bin2, max_bin2);
            }
	}
        
        //Create histograms to be filled with mean residuals for each layer, each sector, and each angle (theta) group
        //These will be be used to make layer vs residuals plots by sector and angle
        //Creates the JFrames for showing the layer vs residual scatterplots 
        //Two of them because splitting the plots between two canvases
        JFrame frame5 = new JFrame("Sector Residuals by Layer and Angle");
        EmbeddedCanvas c5 = new EmbeddedCanvas();
        frame5.setSize(1200,750);
        c5.divide(4, 3);
        JFrame frame6 = new JFrame("Sector Residuals by Layer and Angle");
        EmbeddedCanvas c6 = new EmbeddedCanvas();
        frame6.setSize(1200,750);
        c6.divide(4, 3);
        GraphErrors[][] sectorResid_byAngle = new GraphErrors[6][4];
        ArrayList<String> thetaMin= [0, 8, 14, 22];
        ArrayList<String> thetaMax= [8, 14, 22, 30];
        for (int sec = 0; sec < 6; sec++) {
            for (int thetaGroup = 0; thetaGroup < 4; thetaGroup++) {
                sectorResid_byAngle[sec][thetaGroup] = new GraphErrors("Sector " + Integer.toString(sec + 1) + ", " 
                    + thetaMin[thetaGroup] + "#theta" + thetaMax[thetaGroup]);
                sectorResid_byAngle[sec][thetaGroup].setMarkerSize(2);
            }
        }
        
        //Create the JFrame where the tabbed histogram plots, (one tab for each sector, 
        //each tab showing redsiduals histos for each layer), will be printed
        //Also creates the JTabbedPane method. Each pane will fill one tab
        JFrame residFrame_gaus = new JFrame("theta: 0 to 30, phi: 0 to 60");
        residFrame_gaus.setSize(1200,750);
        residFrame_gaus.setVisible(true);
        JTabbedPane residPane_gaus = new JTabbedPane();
        
         //Draw the residuals histograms for each layer and each sector (tab)
        EmbeddedCanvas[] residBySec_gaus = new EmbeddedCanvas[6];
        for (int sec = 0; sec < 6; sec++) {
            residBySec_gaus[sec] = new EmbeddedCanvas();
            residBySec_gaus[sec].divide(6, 6);
                    
            for (int layer = 0; layer < 36; layer++) {
                nominal_residuals[layer][sec].setTitle("Layer " + Integer.toString(layer + 1));
                nominal_residuals[layer][sec].setTitleX("Residuals (mm)");
                nominal_residuals[layer][sec].setTitleY("Counts");
                nominal_residuals[layer][sec].setOptStat(111);
            }
        }
        /*
        //Test Plot
        TCanvas testCan = new TCanvas("", 1000, 700);
        H1F testPlot = new H1F("Sector 1, Layer 1", n_bins, -500, 500);	
        //nom_resids_byAngle[layer][sec][thetaGroup] = new H1F(" L" + Integer.toString(layer + 1) + " S" + Integer.toString(sec + 1),
                        //n_bins, min_bin, max_bin);
        testCan.draw(testPlot);
        */
	int counts = 0;
	int num_events = 0;
        int run5297Count = 0;
        int run5298Count = 0;
        int run5295Count = 0;
        int allRunCount = 0;
        int count1 = 0;
	// iterate through the files
	for (int current_file; current_file<n_files; current_file++) {
            println(); println(); println("Opening file "+Integer.toString(current_file+1)
			+" out of "+n_files+" of the nominal geometries.");
            HipoDataSource reader = new HipoDataSource();
            // limit to a certain number of files defined by n_files
            reader.open(hipo_list_nominal[current_file]); // open next hipo file

            
            while(reader.hasEvent()==true) { // cycle through all events                
            //while(num_events < 10000) {       // Cycle through limited number of events, for testing 
                count1 += 1;
				HipoDataEvent event = reader.getNextEvent(); // load next event in the hipo file
				// println(banks_test(event));
				if (banks_test(event)) { // check that the necessary banks are present
                    num_events++;
                    // load all relevant banks
                    HipoDataBank hitBank = (HipoDataBank) event.getBank("TimeBasedTrkg::TBHits");
                    HipoDataBank trkBank = (HipoDataBank) event.getBank("TimeBasedTrkg::TBTracks");
                    HipoDataBank trajBank =(HipoDataBank) event.getBank("TimeBasedTrkg::Trajectory");
                    HipoDataBank recBank = (HipoDataBank) event.getBank("REC::Particle");
                    HipoDataBank ccBank = (HipoDataBank) event.getBank("REC::Cherenkov");
                    HipoDataBank calBank = (HipoDataBank) event.getBank("REC::Calorimeter");
                    HipoDataBank configBank = (HipoDataBank) event.getBank("RUN::config");

                    // check the track meets requirements defined in the track check function
                    //sectorCount = 0;
                    if (track_check(recBank, ccBank, calBank)) {
						int sector = trkBank.getInt("sector", 0); 
                        int run = configBank.getInt("run", 0)
                        if (run != 5297) continue;   //If this command is present, only run 5297 will be examined
                        if (run == 5297) {
                            run5297Count += 1;
                        }
                        if (run == 5298) {
                            run5298Count += 1;
                        }
                        if (run == 5295) {
                            run5295Count += 1;
                        }
                        //System.out.println(run);
                        allRunCount += 1;
                        //System.out.println(sector);
                        
                        //To run on just one sector
						//if(sector != 1) continue;                        
                        //if(sector != 2) continue;
						//if(sector != 3) continue;
						//if(sector != 4) continue;
						//if(sector != 5) continue;
						//if(sector != 6) continue;

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
                            	//Or multiply by 10, to give residual in mm
                                float residual_cm = hitBank.getFloat("fitResidual", hitBankRow);
                                float residual = residual_cm * 10;
                                //float residual = 10000*hitBank.getFloat("fitResidual", hitBankRow);
								int superlayer = hitBank.getInt("superlayer", hitBankRow);
								int layer = hitBank.getInt("layer", hitBankRow);
								//int layerNum = (superlayer - 1) * 6 + layer;    //numbered 1 - 36;
                                //System.out.println(layerNum);

                                counts++;
                                      
                                if ((theta < 30 && theta > 0) &&
										(rotated_phi < 60 && rotated_phi > 0))  {                           
                                    nominal_residuals[(superlayer-1)*6 + layer-1][sector - 1].fill(residual);
                                    nominal_residuals2[(superlayer-1)*6 + layer-1][sector - 1].fill(residual * 1000); 
                                    nominal_residuals3[(superlayer-1)*6 + layer-1].fill(residual * 1000);
                                    nominal_residuals4[superlayer - 1][sector - 1].fill(residual * 1000);
                                }
                                
                                if ((theta <= 8 && theta > 0) &&
					(rotated_phi < 60 && rotated_phi > 0))  {   
                                    nom_resids_byAngle[(superlayer-1)*6 + layer-1][sector - 1][0].fill(residual);
                                }
                                if ((theta <= 14 && theta > 8) &&
					(rotated_phi < 60 && rotated_phi > 0))  {   
                                    nom_resids_byAngle[(superlayer-1)*6 + layer-1][sector - 1][1].fill(residual);
                                }
                                if ((theta <= 22 && theta > 14) &&
					(rotated_phi < 60 && rotated_phi > 0))  {   
                                    nom_resids_byAngle[(superlayer-1)*6 + layer-1][sector - 1][2].fill(residual);
                                }
                                if ((theta <= 30 && theta > 22) &&
					(rotated_phi < 60 && rotated_phi > 0))  {   
                                    nom_resids_byAngle[(superlayer-1)*6 + layer-1][sector - 1][3].fill(residual);
                                }
                                
                            }
						}
                    }
				}
            }
	}
       
		//Calculate the mean residuals, etc. from the residuals histos, 
                //nominal_residuals, organized by sector
		double[][][] values = new double[2][36][6];
                double[][][] values_gaus = new double[2][36][6];
		int index = 0;
		

        for (int layer = 0; layer < 36; layer++) {
            for (int sec = 0; sec < 6; sec++) {
                // calculate statistics for each of the layers
                
                //Use a gaussian fit 
                double fitMean = fitResid(nominal_residuals[layer][sec]);
                
                double nominal_mean = nominal_residuals[layer][sec].getMean();
                double nominal_std = nominal_residuals[layer][sec].getRMS();
                double nominal_counts = nominal_residuals[layer][sec].getEntries();
                double nominal_error = nominal_std/Math.sqrt(nominal_counts);
                //double error = Math.sqrt(shift_error*shift_error+
                //	nominal_error*nominal_error);
                values[0][layer][sec] = nominal_mean;
                values[1][layer][sec] = nominal_std;
                    
                //sec1.addPoint(nominal_mean, layer+1, 0, 0)
                //Add data point to sector Resid histos
                sectorResid[sec].addPoint(nominal_mean, layer+1, 0, 0)
                sectorResid_gaus[sec].addPoint(fitMean, layer + 1, 0, 0);
                        
                fitResid(nominal_residuals[layer][sec]);
                residBySec_gaus[sec].cd(layer).draw(nominal_residuals[layer][sec]);
                        
                if (sec == 0) {
                    //System.out.println("layer " + Integer.toString(layer + 1) 
                            //+ ", mean = " + nominal_mean + ", counts = " + nominal_counts)
                            
                    if(layer == 0) {
                        //testPlot.fill(nominal_mean);
                        //S1L1_count += 1;
                        System.out.println("fit mean = " + fitMean + ", hist mean = " + nominal_mean);
                    }
                            
                }
            }
		}
                
        //Calculate the mean residuals, etc. from the residuals histos,
        //nom_resids_byAngle, organized by sector and angle
        double[][][][] values2 = new double[2][36][6][4];
        for (int layer = 0; layer < 36; layer++) {
            for (int sec = 0; sec < 6; sec++) {
                for (int thetaGroup = 0; thetaGroup < 4; thetaGroup++) {
                    // calculate statistics for each of the layers
                    double nominal_mean = nom_resids_byAngle[layer][sec][thetaGroup].getMean();
                    double nominal_std = nom_resids_byAngle[layer][sec][thetaGroup].getRMS();
                    double nominal_counts = nom_resids_byAngle[layer][sec][thetaGroup].getEntries();
                    double nominal_error = nominal_std/Math.sqrt(nominal_counts);
                    //double error = Math.sqrt(shift_error*shift_error+
                        //nominal_error*nominal_error);
                    values2[0][layer][sec][thetaGroup] = nominal_mean;
                    values2[1][layer][sec][thetaGroup] = nominal_std;
                    
                    //sec1.addPoint(nominal_mean, layer+1, 0, 0)
                    sectorResid_byAngle[sec][thetaGroup].addPoint(nominal_mean, layer+1, 0, 0)
                }
            }    
        }
           
        System.out.println("5297 run count = " + run5297Count);
        System.out.println("5298 run count = " + run5298Count);
        System.out.println("5295 run count = " + run5295Count);
        System.out.println("All runs count = " + allRunCount);
        System.out.println("Total event count = " + count1);
                
        //Test Plot
        //testCan.getCanvas().update();
        
        //Draw the layer vs mean resisual plots
	    for (int sec = 0; sec < 6; sec++) {
            sectorResid[sec].setTitle("Sector " + Integer.toString(sec + 1));                   
            sectorResid[sec].setTitleX("Residual (mm)");
            sectorResid[sec].setTitleY("Layer");
            c1.cd(sec).draw(sectorResid[sec]);
            //This sets the x axis limits
            c1.getPad(sec).getAxisX().setRange(-1.5, 1.5);
        }
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(c1);
        //frame.setTitle("0 < #theta < 30, 0 < #phi < 60");
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        //Draw the layer vs mean resisual (from Gaussian fits) plots
	    for (int sec = 0; sec < 6; sec++) {
            sectorResid_gaus[sec].setTitle("Sector " + Integer.toString(sec + 1));                   
            sectorResid_gaus[sec].setTitleX("Residual (mm)");
            sectorResid_gaus[sec].setTitleY("Layer");
            gaus_can.cd(sec).draw(sectorResid_gaus[sec]);
            //This sets the x axis limits
            gaus_can.getPad(sec).getAxisX().setRange(-1.5, 1.5);
        }
        frame_gaus.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame_gaus.add(gaus_can);
        frame_gaus.setLocationRelativeTo(null);
        frame_gaus.setVisible(true);
                
        //Draw the residuals histograms for each layer and each sector (tab)
        EmbeddedCanvas[] c2 = new EmbeddedCanvas[6];
        for (int sec = 0; sec < 6; sec++) {
            c2[sec] = new EmbeddedCanvas();
            c2[sec].divide(6, 6);
                    
            for (int layer = 0; layer < 36; layer++) { 
                nominal_residuals2[layer][sec].setTitle("Layer " + Integer.toString(layer + 1));
                nominal_residuals2[layer][sec].setTitleX("Residuals (#mum)");
                nominal_residuals2[layer][sec].setTitleY("Counts");
                c2[sec].cd(layer).draw(nominal_residuals2[layer][sec]);
            }
            //Add each 6 by 6 EmbeddedCanvas to the corresponding panel
            tabbedPane.add("Sector " + Integer.toString(sec + 1), c2[sec]);
        }
        tabbedFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        tabbedFrame.add(tabbedPane);
        tabbedFrame.setLocationRelativeTo(null);
        tabbedFrame.setVisible(true);
                
        //Draw the residuals histograms for each layer, with all 6 sectors combined
        JFrame frame3 = new JFrame("Residuals by Layer; All Sectors");
        frame3.setSize(1200,750);
        frame3.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        EmbeddedCanvas c3 = new EmbeddedCanvas();
        c3.divide(6, 6);
        for (int layer = 0; layer < 36; layer++) {
            nominal_residuals3[layer].setTitle("Layer " + Integer.toString(layer + 1));
            nominal_residuals3[layer].setTitleX("Residuals (#mum)");
            nominal_residuals3[layer].setTitleY("Counts");
            c3.cd(layer).draw(nominal_residuals3[layer])
        }
        frame3.add(c3);
        frame3.setLocationRelativeTo(null);
        frame3.setVisible(true);
                
        //Draw the residuals histograms for each SUPERlayer and each sector (tab)
        EmbeddedCanvas[] c4 = new EmbeddedCanvas[6];
        for (int sec = 0; sec < 6; sec++) {
            c4[sec] = new EmbeddedCanvas();
            c4[sec].divide(2, 3);
                    
            for (int sLayer = 0; sLayer < 6; sLayer++) { 
                nominal_residuals4[sLayer][sec].setTitle("Superlayer " + Integer.toString(sLayer + 1));
                nominal_residuals4[sLayer][sec].setTitleX("Residuals (#mum)");
                nominal_residuals4[sLayer][sec].setTitleY("Counts");
                c4[sec].cd(sLayer).draw(nominal_residuals4[sLayer][sec]);
            }
            //Add each embeddedCanvas (each one representing a different sector)
            tabbedPane4.add("Sector " + Integer.toString(sec + 1), c4[sec]);
        }
        tabbedFrame4.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //Add tabbedPane4 to tabbedFrame4
        tabbedFrame4.add(tabbedPane4);
        tabbedFrame4.setLocationRelativeTo(null);
        tabbedFrame4.setVisible(true);
                
        //Draw the layer vs mean resisual plots by angle and sector
        //Sectors 1, 2, and 3
		for (int sec = 0; sec < 3; sec++) {
            for (int thetaGroup = 0; thetaGroup < 4; thetaGroup++) {
                sectorResid_byAngle[sec][thetaGroup].setTitle("Sector " + Integer.toString(sec + 1) + ", " 
                    + thetaMin[thetaGroup] + "<#theta<" + thetaMax[thetaGroup]);                   
                sectorResid_byAngle[sec][thetaGroup].setTitleX("Residual (mm)");
                sectorResid_byAngle[sec][thetaGroup].setTitleY("Layer");
                c5.cd(sec*4 + thetaGroup).draw(sectorResid_byAngle[sec][thetaGroup]);
            }    
        }
        //Sectors 4, 5, and 6
        for (int sec = 3; sec < 6; sec++) {
            for (int thetaGroup = 0; thetaGroup < 4; thetaGroup++) {
                sectorResid_byAngle[sec][thetaGroup].setTitle("Sector " + Integer.toString(sec + 1) + ", " 
                    + thetaMin[thetaGroup] + "<#theta<" + thetaMax[thetaGroup]);                   
                sectorResid_byAngle[sec][thetaGroup].setTitleX("Residual (mm)");
                sectorResid_byAngle[sec][thetaGroup].setTitleY("Layer");
                c6.cd((sec-3)*4 + thetaGroup).draw(sectorResid_byAngle[sec][thetaGroup]);
            }
        }
        frame5.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame5.add(c5);
        frame5.setLocationRelativeTo(null);
        frame5.setVisible(true);
        frame6.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame6.add(c6);
        frame6.setLocationRelativeTo(null);
        frame6.setVisible(true);

        for (int sec = 0; sec < 6; sec++) {
            residPane_gaus.add("Sector " + Integer.toString(sec + 1), residBySec_gaus[sec]);
        }
        
        residFrame_gaus.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        residFrame_gaus.add(residPane_gaus);
        //tabbedFrame.getContentPane().add(tabbedPane);
        residFrame_gaus.setLocationRelativeTo(null);
        residFrame_gaus.setVisible(true);
        
        System.out.println("Done!");
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
        
    public static double fitResid(H1F histo) {
        double mean  = histo.getDataX(histo.getMaximumBin());
        double amp   = histo.getBinContent(histo.getMaximumBin());
        double sigma = 1.0;
        double min = mean - 2;
        double max = mean + 2;
        
        F1D f1   = new F1D("f1Resid","[gausAmp]*gaus(x,[gausMean],[gausSigma])", min, max);
        f1.setLineColor(2);
        f1.setLineWidth(2);
        f1.setOptStat("1111");
        f1.setParameter(0, amp);
        f1.setParameter(1, mean);
        f1.setParameter(2, sigma);
        f1.setRange(mean-2.0*sigma,mean+2.0*sigma);
        DataFitter.fit(f1, histo, "Q");
        
        double f1_mean = f1.getParameter(1);
        return f1_mean;
    }
}

