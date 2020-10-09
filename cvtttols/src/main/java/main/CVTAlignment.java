package main;

import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo3.Hipo3DataSource;

import java.io.*;
import java.util.ArrayList;

import BMT_struct.Barrel;
import BST_struct.Barrel_SVT;
import Alignment.Aligner;


public class CVTAlignment {
	
	static Barrel BMT;
	static Barrel_SVT BST;
		
	public CVTAlignment() {
		BST=new Barrel_SVT();
		BMT=new Barrel();
	}
	
	public static void main(String[] args) throws IOException {
		
		if (args.length<4) {
			System.out.println("Execution line is as follows, in this specific order:\n");
			System.out.println("java -jar Alignator.jar LAYER SECTOR -i INPUT1 -i INPUT2 (-svt ALIGNMENTFILESVT -mvt ALIGNMENTFILEMVT -cvt ALIGNMENTFILECVT -loc LOCAL)");
			System.out.println("INPUT1: File on which alignment code will run. It should be a file produced with Tracker.jar, with the requirement to exclude the detector to be aligned from reconstruction.");
			System.out.println("LAYER: Layer of the detector to be aligned");
			System.out.println("SECTOR: Sector of the detector to be aligned... If Layer=all and sector=all, alignment is all MVT wrt all SVT");
			System.out.println("optional: ALIGNMENTFILE: Path and name of the file in which alignment results must be written");
			System.out.println("optional: LOCAL: Alignment within local frame of SVT sensors (ONLY for SVT)");
			System.exit(0);
		}
		
		ArrayList<String> Inputsfile=new ArrayList<String>();
		String ConstantFileMVT=""; //File containing internal misalignments of MVT
		String ConstantFileSVT=""; //File containing internal misalignments of SVT
		String ConstantFileCVT=""; //File contaning MVT wrt SVT misalignments
		Boolean LocalAlign=false;
		for (int i=2; i<args.length; i++) {
			if (args[i].equals("-i")) Inputsfile.add(args[i+1]);
			if (args[i].equals("-svt")) ConstantFileSVT=args[i+1];
			if (args[i].equals("-mvt")) ConstantFileMVT=args[i+1];
			if (args[i].equals("-cvt")) ConstantFileCVT=args[i+1];
			if (args[i].equals("-loc")) LocalAlign=Boolean.parseBoolean(args[i+1]);
		}
		
		Hipo3DataSource[] reader=new Hipo3DataSource[Inputsfile.size()];
		for (int infile=0; infile<Inputsfile.size(); infile++) {
			reader[infile]=new Hipo3DataSource();
			reader[infile].open(Inputsfile.get(infile));
		}
				
		CVTAlignment CVTAli=new CVTAlignment();
		
		BMT.getGeometry().LoadMVTSVTMisalignment(ConstantFileCVT);
		BMT.getGeometry().LoadMisalignmentFromFile(ConstantFileMVT);
		BST.getGeometry().LoadMisalignmentFromFile(ConstantFileSVT);
		
		Aligner Alignment=new Aligner();
		
		/**********************************************************************************************************************************************************************************************/
		//We align a specific tile or module
		if (!args[0].equals("all")&&!args[1].equals("all")) {
		
			int layer=Integer.parseInt(args[0]);
			int sector=Integer.parseInt(args[1]);
		
			if (LocalAlign) Alignment.DoSVTLocAlignment(BST, reader, layer, sector);
			else Alignment.DoAlignment(BMT, BST, reader, layer, sector);
			
			if (layer>6) System.out.println(BMT_geo.Constants.getRx(layer-6, sector)+" "+BMT_geo.Constants.getRy(layer-6, sector)+" "+BMT_geo.Constants.getRz(layer-6, sector)+" "+
				BMT_geo.Constants.getCx(layer-6, sector)+" "+BMT_geo.Constants.getCy(layer-6, sector)+" "+BMT_geo.Constants.getCz(layer-6, sector));
			else System.out.println(BST.getGeometry().getRx(layer, sector)+" "+BST.getGeometry().getRy(layer, sector)+" "+BST.getGeometry().getRz(layer, sector)+" "+
				BST.getGeometry().getCx(layer, sector)+" "+BST.getGeometry().getCy(layer, sector)+" "+BST.getGeometry().getCz(layer, sector)+" "+BST.getGeometry().getLocTx(layer, sector));
		
			//Need to write down the file
			File AlignCst;
			if (layer<7) AlignCst=new File(ConstantFileSVT);
			else AlignCst=new File(ConstantFileMVT);
			try {
				if (!AlignCst.exists()) AlignCst.createNewFile();
				FileWriter Writer=new FileWriter(AlignCst, true);
				try {
					//If BMT constants, write BMT constants
					if (layer>6) Writer.write(layer+" "+sector+" "+BMT_geo.Constants.getRx(layer-6, sector)+" "+BMT_geo.Constants.getRy(layer-6, sector)+" "+BMT_geo.Constants.getRz(layer-6, sector)+" "+
							BMT_geo.Constants.getCx(layer-6, sector)+" "+BMT_geo.Constants.getCy(layer-6, sector)+" "+BMT_geo.Constants.getCz(layer-6, sector)+"\n");
					//If BST constants, write BST constants
					else {
						Writer.write(layer+" "+sector+" "+BST.getGeometry().getRx(layer, sector)+" "+BST.getGeometry().getRy(layer, sector)+" "+BST.getGeometry().getRz(layer, sector)+" "+
							BST.getGeometry().getCx(layer, sector)+" "+BST.getGeometry().getCy(layer, sector)+" "+BST.getGeometry().getCz(layer, sector)+" "+BST.getGeometry().getLocTx(layer, sector)+"\n");
						if (!LocalAlign) Writer.write((layer+1)+" "+sector+" "+BST.getGeometry().getRx(layer, sector)+" "+BST.getGeometry().getRy(layer, sector)+" "+BST.getGeometry().getRz(layer, sector)+" "+
								BST.getGeometry().getCx(layer, sector)+" "+BST.getGeometry().getCy(layer, sector)+" "+BST.getGeometry().getCz(layer, sector)+" "+BST.getGeometry().getLocTx(layer+1, sector)+"\n");
					}
				} finally {
					
					Writer.close();
				}
			} catch (Exception e) {
				System.out.println("Impossible to write results in file");
			}
		}
		
		/**********************************************************************************************************************************************************************************************/
		//We want to align MVT wrt to SVT
		else {
			Alignment.DoMVTSVTAlignment(BMT, reader);
			File AlignCst=new File(ConstantFileCVT);
			try {
				if (!AlignCst.exists()) AlignCst.createNewFile();
				FileWriter Writer=new FileWriter(AlignCst, true);
				try {
					Writer.write(BMT_geo.Constants.getRxCVT()+" "+BMT_geo.Constants.getRyCVT()+" "+BMT_geo.Constants.getRzCVT()+" "+BMT_geo.Constants.getCxCVT()+" "+BMT_geo.Constants.getCyCVT()+" "+BMT_geo.Constants.getCzCVT()+"\n");
				} finally {
					Writer.close();
				}
			} catch (Exception e) {
				System.out.println("Impossible to write results in file");
			}
		}
	}
			 
	
	
}