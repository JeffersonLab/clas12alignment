package org.clas.dc.alignment;


import eu.mihosoft.vrl.v3d.Vector3d;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.detector.geant4.v2.DCGeant4Factory;
import org.jlab.geom.base.ConstantProvider;
import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Vector3D;
import org.jlab.groot.base.GStyle;
import org.jlab.groot.data.Directory;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.TDirectory;
import org.jlab.groot.graphics.EmbeddedCanvasTabbed;
import org.jlab.groot.graphics.EmbeddedPad;
import org.jlab.groot.group.DataGroup;
import org.jlab.jnp.utils.options.OptionStore;

/**
 * DC alignment code, implementing the procedure developed by T. Hayward
 * See original scripts and documentation in original_scripts_and_docs
 * 
 * @author thayward
 * @author devita
 * @author reedtg
 * 
 */
public class Alignment {


    private Map<String,Histo> histos           = new LinkedHashMap();
    private String[]          inputs           = new String[Constants.NPARS+1];
    private Bin[]             thetaBins        = null;
    private Bin[]             phiBins          = null;
    private ConstantsManager  manager          = new ConstantsManager();
    private Table             compareAlignment = null;
    private Table             initAlignment    = new Table();
    private DCGeant4Factory   dcDetector       = null;
        
    private boolean           subtractedShifts = true;
    private boolean           sectorShifts      = false;
    private boolean           initFitPar       = false;
    private boolean           fitVerbosity     = false;
    private int               fitIteration     = 1;
    
    private int               markerSize = 4;
    private int[]             markerColor = {2,3,4,5,7,9};
    private int[]             markerStyle = {2, 3, 1, 4};
    private String            fontName = "Arial";
    
    ByteArrayOutputStream pipeOut = new ByteArrayOutputStream();
    private static PrintStream outStream = System.out;
    private static PrintStream errStream = System.err;
    
    public Alignment() {
        this.initInputs();
    }
    
    private void initConstants(int run, String initVariation, String compareVariation) {
        ConstantProvider provider  = GeometryFactory.getConstants(DetectorType.DC, 11, "default");
        dcDetector = new DCGeant4Factory(provider, DCGeant4Factory.MINISTAGGERON, false);
        initAlignment    = this.getTable(run, initVariation);
        compareAlignment = this.getTable(run, compareVariation);
    }
    
    public void setFitOptions(boolean sector, int iteration, boolean verbosity) {
        this.sectorShifts = sector;
        this.fitIteration = iteration;
        this.fitVerbosity = verbosity;
        this.printConfig(sectorShifts, "sectorShifts", "");
        this.printConfig(fitIteration, "fitIteration", "");
        this.printConfig(fitVerbosity, "fitVerbosity", "");
    }
    
    private void setShiftsMode(boolean shifts) {
        this.subtractedShifts = shifts;
        this.printConfig(shifts, "subtractedShifts", "");
    }

    private void initGraphics() {
        GStyle.getAxisAttributesX().setTitleFontSize(18);
        GStyle.getAxisAttributesX().setLabelFontSize(14);
        GStyle.getAxisAttributesY().setTitleFontSize(18);
        GStyle.getAxisAttributesY().setLabelFontSize(14);
        GStyle.getAxisAttributesZ().setLabelFontSize(14);
        GStyle.getAxisAttributesX().setLabelFontName(this.fontName);
        GStyle.getAxisAttributesY().setLabelFontName(this.fontName);
        GStyle.getAxisAttributesZ().setLabelFontName(this.fontName);
        GStyle.getAxisAttributesX().setTitleFontName(this.fontName);
        GStyle.getAxisAttributesY().setTitleFontName(this.fontName);
        GStyle.getAxisAttributesZ().setTitleFontName(this.fontName);
        GStyle.setGraphicsFrameLineWidth(1);
        GStyle.getH1FAttributes().setLineWidth(2);
    }
    
    private void initInputs() {
        inputs[0] = "nominal";
        for(int i=0; i<Constants.NPARS; i++) {
            inputs[i+1] = Constants.PARNAME[i];
        }
    }
    
    public Table getTable(int run, String variation) {
        Table alignment = null;
        
        String table = "/geometry/dc/alignment";
        List<String> tables = new ArrayList<>();
        tables.add(table);
        manager.init(tables);

        if(!variation.isEmpty()) {
            manager.reset();
            manager.setVariation(variation);
            alignment = new Table(manager.getConstants(run, table));
        }
        return alignment;
    }
    
    private void printConfig(Object o, String name, String message) {
        String s = "[CONFIG] " + name + " set to " + o.toString();
        if(!message.isEmpty()) s += ": " + message;
        System.out.println(s);
    }
    
    public void addHistoSet(String name, Histo histo) {
        this.histos.put(name, histo);
    }
    
    public void analyzeHistos(int resFit, int vertexFit) {
        this.printConfig(resFit, "resFit", "");
        this.printConfig(vertexFit, "vertexFit", "");
        for(String key : histos.keySet()) {
            System.out.println("\nAnalyzing histos for variation " + key);
            histos.get(key).analyzeHisto(resFit, vertexFit);
            for(int i=0; i<Constants.NPARS; i++)
                if(key.equals(Constants.PARNAME[i])) Constants.PARACTIVE[i] = true;
        }
    }
    
    private double getShift(String key, int sector, int layer, int itheta, int iphi) {
        double shift=0;
        if(histos.containsKey(key)) {
            if(subtractedShifts)
                shift = -histos.get(key).getParValues(sector, itheta, iphi)[layer];
            else
                shift = histos.get("nominal").getParValues(sector, itheta, iphi)[layer]
                       -histos.get(key).getParValues(sector, itheta, iphi)[layer];                
        }
        return shift;
    }
       
    private double getShiftError(String key, int sector, int layer, int itheta, int iphi) {
        double error=0;
        if(histos.containsKey(key)) {
            if(subtractedShifts)
                error = histos.get(key).getParErrors(sector, itheta, iphi)[layer];
            else
                error = Math.sqrt(Math.pow(histos.get("nominal").getParErrors(sector, itheta, iphi)[layer], 2)
                                 +Math.pow(histos.get(key).getParErrors(sector, itheta, iphi)[layer], 2));
        }
        return error;
    }
   
    private double getShift(String key, int layer, int itheta, int iphi) {
        double shift=0;
        if(histos.containsKey(key)) {
            for(int is=0; is<Constants.NSECTOR; is++) {
                int sector = is+1;
                shift += this.getShift(key, sector, layer, itheta, iphi)/Constants.NSECTOR;
            }
        }
        return shift;
    }

    private double getShiftError(String key, int layer, int itheta, int iphi) {
        double shift=0;
        if(histos.containsKey(key)) {
            for(int is=0; is<Constants.NSECTOR; is++) {
                int sector = is+1;
                shift += Math.pow(this.getShiftError(key, sector, layer, itheta, iphi),2)/Constants.NSECTOR;
            }
        }
        return Math.sqrt(shift);
    }

    private double[] getShifts(String key, int sector, int itheta, int iphi) {
        double[] shifts = new double[Constants.NLAYER+1];
        for(int layer=0; layer<=Constants.NLAYER; layer++) {
           shifts[layer] = this.getShift(key, sector, layer, itheta, iphi);
        }
        return shifts;
    }
    
    private double[] getShiftsError(String key, int sector, int itheta, int iphi) {
        double[] errors = new double[Constants.NLAYER+1];
        for(int layer=0; layer<=Constants.NLAYER; layer++) {
           errors[layer] = this.getShiftError(key, sector, layer, itheta, iphi);
        }
        return errors;
    }
    
    private double[] getShifts(String key, int itheta, int iphi) {
        double[] shifts = new double[Constants.NLAYER+1];
        for(int layer=0; layer<=Constants.NLAYER; layer++) {
           shifts[layer] = this.getShift(key, layer, itheta, iphi);
        }
        return shifts;
    }
    
    private double[] getShiftsError(String key, int itheta, int iphi) {
        double[] errors = new double[Constants.NLAYER+1];
        for(int layer=0; layer<=Constants.NLAYER; layer++) {
           errors[layer] = this.getShiftError(key, layer, itheta, iphi);
        }
        return errors;
    }
    
    private double getFittedResidual(Table alignment, int sector, int layer, int itheta, int iphi) {
        double value=0;
        if(alignment!=null) {
            for(int ikey=0; ikey<inputs.length; ikey++) {
                String key = inputs[ikey];
                if(!key.equals("nominal") && this.histos.containsKey(key)) {
                    value += this.getShift(key, sector, layer, itheta, iphi)*
                             alignment.getShiftSize(key, sector)/Constants.UNITSHIFT[ikey-1];
                }
            }
        }
        return value;
    }

    private double getFittedResidualError(Table alignment, int sector, int layer, int itheta, int iphi) {
        double value=0;
        if(alignment!=null) {
            for(int ikey=1; ikey<inputs.length; ikey++) {
                String key = inputs[ikey];
                if(!key.equals("nominal") && this.histos.containsKey(key)) {
                    value += Math.pow(this.getShiftError(key, sector, layer, itheta, iphi)*alignment.getShiftSize(key, sector)/Constants.UNITSHIFT[ikey-1],2);
                    value += Math.pow(this.getShift(key, layer, itheta, iphi)*this.getShiftSizeError(key, sector)/Constants.UNITSHIFT[ikey-1],2);
                }
            }
        }
        return Math.sqrt(value);
    }

    private double[] getFittedResidual(Table alignment, int sector, int itheta, int iphi) {
        double[] shift = new double[Constants.NLAYER+1];
        for(int layer=0; layer<=Constants.NLAYER; layer++) {
           shift[layer] = this.getFittedResidual(alignment, sector, layer, itheta, iphi);
        }
        return shift;
    }
    
    private double[] getFittedResidualError(Table alignment, int sector, int itheta, int iphi) {
        double[] shift = new double[Constants.NLAYER+1];
        for(int layer=0; layer<=Constants.NLAYER; layer++) {
           shift[layer] = this.getFittedResidualError(alignment,sector, layer, itheta, iphi);
        }
        return shift;
    }
    
    private double getShiftSizeError(String key, int sector) {
        return 0;
    }
    
    public EmbeddedCanvasTabbed analyzeFits() {
        EmbeddedCanvasTabbed canvas = new EmbeddedCanvasTabbed("nominal");
        System.out.println("\nPlotting nominal geometry residuals");
        canvas.getCanvas("nominal").draw(this.getResidualGraphs(null));
        canvas.getCanvas().setFont(fontName);
        for(EmbeddedPad pad : canvas.getCanvas("nominal").getCanvasPads())
            pad.getAxisX().setRange(-2000, 2000);
        
        canvas.addCanvas("nominal vs. theta");
        canvas.getCanvas("nominal vs. theta").draw(this.getAngularGraph(null));
        canvas.getCanvas().setFont(fontName);
        for(EmbeddedPad pad : canvas.getCanvas("nominal vs. theta").getCanvasPads())
            pad.getAxisX().setRange(-2000, 2000);
        
        System.out.println("\nPlotting corrected geometry residuals");        
        canvas.addCanvas("CCDB corrected");
        canvas.getCanvas("CCDB corrected").draw(this.getResidualGraphs(compareAlignment.subtract(initAlignment)));
        canvas.getCanvas().setFont(fontName);
        for(EmbeddedPad pad : canvas.getCanvas("CCDB corrected").getCanvasPads())
            pad.getAxisX().setRange(-2000, 2000);
        
        canvas.addCanvas("CCDB corrected vs. theta");
        canvas.getCanvas("CCDB corrected vs. theta").draw(this.getAngularGraph(compareAlignment.subtract(initAlignment)));
        canvas.getCanvas().setFont(fontName);
        for(EmbeddedPad pad : canvas.getCanvas("CCDB corrected vs. theta").getCanvasPads())
            pad.getAxisX().setRange(-2000, 2000);
        
        // shifts
        System.out.println("\nPlotting shifted geometry residuals");
        canvas.addCanvas("shift magnitude");
        canvas.getCanvas("shift magnitude").draw(this.getShiftsHisto(1));
        for(String key : histos.keySet()) {
            if(!key.equals("nominal")) {
                canvas.addCanvas(key);
                canvas.getCanvas(key).draw(this.getShiftsGraph(key));
                canvas.getCanvas().setFont(fontName);
                for(EmbeddedPad pad : canvas.getCanvas(key).getCanvasPads())
                    pad.getAxisX().setRange(-1000, 1000);
            }
        }
        if(compareAlignment!=null) {
            System.out.println("\nFitting residuals");
            System.out.println("\nInitial alignment parameters\n" + this.initAlignment.toString());
            System.out.println("\nInitial alignment parameters\n" + this.initAlignment.toTextTable());
            Table fittedAlignment = new Table();
            if(this.setActiveParameters()>0) {
                for(int is=0; is<Constants.NSECTOR; is++) {
                    int sector = is+1;
                    Parameter[] par = this.fit(sector);
                    fittedAlignment.update(sector, par);
                }
                Table finalAlignment = fittedAlignment.copy().add(initAlignment);
                System.out.println("\nFitted alignment parameters\n" +fittedAlignment.toString());
                System.out.println("\nFinal alignment parameters\n" +finalAlignment.toTextTable());
                System.out.println("\nTo be compared to\n" +this.compareAlignment.toTextTable());
                
                canvas.addCanvas("corrected (with new parameters)");
                canvas.getCanvas("corrected (with new parameters)").draw(this.getResidualGraphs(fittedAlignment));
                canvas.getCanvas().setFont(fontName);
                for(EmbeddedPad pad : canvas.getCanvas("corrected (with new parameters)").getCanvasPads())
                    pad.getAxisX().setRange(-2000, 2000);

                canvas.addCanvas("corrected (with new parameters) vs. theta");
                canvas.getCanvas("corrected (with new parameters) vs. theta").draw(this.getAngularGraph(fittedAlignment));
                canvas.getCanvas().setFont(fontName);
                for(EmbeddedPad pad : canvas.getCanvas("corrected (with new parameters) vs. theta").getCanvasPads())
                    pad.getAxisX().setRange(-2000, 2000); 
                DataGroup comAlignPars = this.compareAlignment.getDataGroup(1);
                DataGroup preAlignPars = this.initAlignment.getDataGroup(3);
                DataGroup newAlignPars = finalAlignment.getDataGroup(2);
                canvas.addCanvas("before/after");
                canvas.getCanvas("before/after").draw(this.getSectorHistograms(null, 1));
                canvas.getCanvas("before/after").draw(this.getSectorHistograms(compareAlignment.subtract(initAlignment), 3));
                canvas.getCanvas("before/after").draw(this.getSectorHistograms(fittedAlignment, 2));
                canvas.addCanvas("misalignments");
                canvas.getCanvas("misalignments").draw(comAlignPars);
                canvas.getCanvas("misalignments").draw(newAlignPars);
                for(int i=0; i<canvas.getCanvas("misalignments").getCanvasPads().size(); i++) {
                    EmbeddedPad pad = canvas.getCanvas("misalignments").getCanvasPads().get(i);
                    if(i<Constants.NSECTOR) 
                        pad.getAxisX().setRange(-0.5, 0.5);
                    else
                        pad.getAxisX().setRange(-0.199, 0.201);
                }
                canvas.addCanvas("internal-only");
                Table compareInternal = compareAlignment.subtract(this.getGlobalOffsets(compareAlignment));
                Table finalInternal   = finalAlignment.subtract(this.getGlobalOffsets(finalAlignment));
                canvas.getCanvas("internal-only").draw(compareInternal.getDataGroup(1));
                canvas.getCanvas("internal-only").draw(finalInternal.getDataGroup(2));
                for(int i=0; i<canvas.getCanvas("internal-only").getCanvasPads().size(); i++) {
                    EmbeddedPad pad = canvas.getCanvas("internal-only").getCanvasPads().get(i);
                    if(i<Constants.NSECTOR) 
                        pad.getAxisX().setRange(-0.5, 0.5);
                    else
                        pad.getAxisX().setRange(-0.199, 0.201);
                }
            }
        }
        return canvas;
    }
    
    
    private Parameter[] fit(int sector) {
        String options = "";
        if(fitVerbosity) options = "V";
        double[][][][] shifts = new double[Constants.NPARS][Constants.NLAYER+1][thetaBins.length-1][phiBins.length-1];
        double[][][]   values = new double[Constants.NLAYER+1][thetaBins.length-1][phiBins.length-1];
        double[][][]   errors = new double[Constants.NLAYER+1][thetaBins.length-1][phiBins.length-1];
        for(int i=0; i<Constants.NPARS+1; i++) {
            for(int il=0; il<=Constants.NLAYER; il++) {
                for(int it=1; it<thetaBins.length; it ++) {
                    for(int ip=1; ip<phiBins.length; ip++) {
                        if(i==0) {
                            values[il][it-1][ip-1] = histos.get("nominal").getParValues(sector, it, ip)[il];
                            errors[il][it-1][ip-1] = histos.get("nominal").getParErrors(sector, it, ip)[il];
                        }
                        else {
                            if(sectorShifts) {
                                shifts[i-1][il][it-1][ip-1] = this.getShift(Constants.PARNAME[i-1], sector, il, it, ip)/Constants.UNITSHIFT[i-1];
                            }
                            else {
                                shifts[i-1][il][it-1][ip-1] = this.getShift(Constants.PARNAME[i-1], il, it, ip)/Constants.UNITSHIFT[i-1];
                            }
                        }
                    }
                }
            }
        }
        Fitter residualFitter = new Fitter(shifts, values, errors);
        // check chi2 of "compare" misalignments
        residualFitter.setPars(compareAlignment.getParameters(sector));
        System.out.println(String.format("\nSector %d", sector));
        residualFitter.printChi2AndNDF();
        // reinit
        residualFitter.zeroPars();
        double chi2 = Double.POSITIVE_INFINITY;
        Parameter[] fittedPars = residualFitter.getParCopy();
        String benchmark = "";
        for(int i=0; i<fitIteration; i++) {
            System.out.print("\riteration "+i + "\t" + benchmark);
//            residualFitter.randomizePars(fittedPars);
//            residualFitter.printChi2AndNDF();
            residualFitter.fit(options);
//            residualFitter.printPars();
            if(residualFitter.getChi2()< chi2 && residualFitter.getStatus()) {
                chi2 = residualFitter.getChi2();
                fittedPars = residualFitter.getParCopy();
                benchmark = residualFitter.getBenchmarkString();
            }
        }
        System.out.println();
        residualFitter.setPars(fittedPars);
        residualFitter.printChi2AndNDF();
        residualFitter.printPars();
        return fittedPars;
    }

    
    private DataGroup getResidualGraphs(Table alignment) {
        double[] layers = new double[Constants.NLAYER+1];
        double[] zeros  = new double[Constants.NLAYER+1];
        for(int il=0; il<=Constants.NLAYER; il++) layers[il]=il;

        DataGroup residuals = new DataGroup(2,3);
        for(int it=1; it<thetaBins.length; it ++) {
            for(int ip=1; ip<phiBins.length; ip++) {
                for(int is=0; is<Constants.NSECTOR; is++ ) {
                    int sector = is+1;
                    double[] shiftRes = new double[Constants.NLAYER+1];
                    double[] errorRes = new double[Constants.NLAYER+1];
                    for (int il = 0; il <= Constants.NLAYER; il++) {
                        shiftRes[il] = histos.get("nominal").getParValues(sector, it, ip)[il]
                                     - this.getFittedResidual(alignment, sector, it, ip)[il];
                        errorRes[il] = Math.sqrt(Math.pow(histos.get("nominal").getParErrors(sector, it, ip)[il], 2)
                                              +0*Math.pow(this.getFittedResidualError(alignment, sector, it, ip)[il], 2));
                    }
                    GraphErrors gr_fit = new GraphErrors("gr_fit_S" + sector + "_theta " + it + "_phi" + ip, 
                                                         shiftRes, layers, errorRes, zeros);
                    gr_fit.setTitle("Sector " + sector);
                    gr_fit.setTitleX("Residual (um)");
                    gr_fit.setTitleY("Layer");
                    gr_fit.setMarkerColor(this.markerColor[(it-1)%6]);
                    gr_fit.setMarkerStyle(this.markerStyle[(ip-1)%4]);
                    gr_fit.setMarkerSize(this.markerSize);
                    residuals.addDataSet(gr_fit, is);
                }               
            }
        }
        return residuals;
    }  
    
    private DataGroup getShiftsGraph(String key) {
        double[] layers = new double[Constants.NLAYER+1];
        double[] zeros  = new double[Constants.NLAYER+1];
        for(int il=0; il<=Constants.NLAYER; il++) layers[il]=il;
        
        DataGroup shifts = new DataGroup(thetaBins.length,phiBins.length);
        for(int it=0; it<thetaBins.length; it ++) {
            for(int ip=0; ip<phiBins.length; ip++) {
                for(int is=0; is<Constants.NSECTOR; is++ ) {
                    int sector = is+1;
                    // residuals 
                    GraphErrors gr_res = new GraphErrors("gr_res_S" + sector  + "_theta " + it + "_phi" + ip, 
                                                         this.getShifts(key, sector, it, ip), layers, 
                                                         this.getShiftsError(key, sector, it, ip), zeros);
                    gr_res.setTitle("Theta:"+ thetaBins[it].getRange() + " Phi:" + phiBins[ip].getRange());
                    gr_res.setTitleX("Residual (um)");
                    gr_res.setTitleY("Layer");
                    gr_res.setMarkerColor(this.markerColor[is]);
                    gr_res.setMarkerSize(this.markerSize);
                    shifts.addDataSet(gr_res, ip*thetaBins.length+it);
                }
                GraphErrors gr_res = new GraphErrors("gr_res" + "_theta " + it + "_phi" + ip, this.getShifts(key, it, ip), layers, zeros, zeros);
                gr_res.setTitle("Theta:"+ thetaBins[it].getRange() + " Phi:" + phiBins[ip].getRange());
                gr_res.setTitleX("Residual (um)");
                gr_res.setTitleY("Layer");
                gr_res.setMarkerColor(1);
                gr_res.setMarkerSize(this.markerSize);
                shifts.addDataSet(gr_res, ip*thetaBins.length+it);
            }
        }
        return shifts;
    }
    
    private DataGroup getShiftsHisto(int sector) {
        
        int nbin = (inputs.length-1)*thetaBins.length*phiBins.length;
        
        DataGroup shifts = new DataGroup(1, 2);
        for(int it=0; it<thetaBins.length; it ++) {
            for(int ip=0; ip<phiBins.length; ip++) {
                int icol = 1;
                if(it>0) icol = this.markerColor[(it-1)%6];
                icol += 20*(ip%phiBins.length);
                H1F hi_res = new H1F("hi_res" + "_theta " + it + "_phi" + ip, "Shift", "#Deltaresidual (um)", nbin, 0, inputs.length-1);
                H1F hi_vtx = new H1F("hi_vtx" + "_theta " + it + "_phi" + ip, "Shift", "#Deltavertex (cm)",   nbin, 0, inputs.length-1);
                hi_res.setLineColor(icol);
                hi_res.setFillColor(icol);
                hi_res.setLineWidth(0);
                hi_vtx.setLineColor(icol);
                hi_vtx.setFillColor(icol);
                hi_vtx.setLineWidth(0);
                shifts.addDataSet(hi_res, 0);
                shifts.addDataSet(hi_vtx, 1);
                for(int ikey=1; ikey<inputs.length; ikey++) {
                    String key = inputs[ikey];
                    double ibin = (ikey-1)*thetaBins.length*phiBins.length + it*phiBins.length + ip + 0.5;
                    ibin /= thetaBins.length*phiBins.length;
                    if(histos.containsKey(key)) {
                        double aveResShift = 0;
                        double aveVtxShift = Math.abs(this.getShift(key, sector, 0, it, ip))/Constants.SCALE;
                        for(int layer=1; layer<=Constants.NLAYER; layer++) {
                            aveResShift += Math.abs(this.getShift(key, sector, layer, it, ip))/Constants.NLAYER;
                        }
                        hi_res.fill(ibin, aveResShift);
                        hi_vtx.fill(ibin, aveVtxShift);
                    }
                }
            }
        }
        return shifts;
    }

    
    private DataGroup getAngularGraph(Table alignment) {
        double[] zeros  = new double[thetaBins.length-1];

        DataGroup residuals = new DataGroup(6,1);
        for(int is=0; is<Constants.NSECTOR; is++ ) {
            int sector = is+1;
            for(int ip=1; ip<phiBins.length; ip++) {
                for (int il = 0; il <= Constants.NLAYER; il++) {
                    double[] shiftRes = new double[thetaBins.length-1];
                    double[] errorRes = new double[thetaBins.length-1];
                    double[] angles   = new double[thetaBins.length-1];          
                    for(int it=1; it<thetaBins.length; it++) {
                        shiftRes[it-1] = histos.get("nominal").getParValues(sector, it, ip)[il]
                                       - this.getFittedResidual(alignment, sector, it, ip)[il];
                        errorRes[it-1] = Math.sqrt(Math.pow(histos.get("nominal").getParErrors(sector, it, ip)[il], 2)
                                               + 0*Math.pow(this.getFittedResidualError(alignment, sector, it, ip)[il], 2));
//                        angles[it-1]   = thetaBins[it].getMean()+thetaBins[it].getWidth()*(il-Constants.NLAYER/2)/Constants.NLAYER/1.2;
                        angles[it-1]   = it+0.9*(il-Constants.NLAYER/2)/Constants.NLAYER;
                    }
                    GraphErrors gr_fit = new GraphErrors("gr_fit_S" + sector + "_layer " + il + "_phi" + ip, 
                                                         shiftRes, angles, errorRes, zeros);
                    gr_fit.setTitle("Sector " + sector);
                    gr_fit.setTitleX("Residual (um)");
                    gr_fit.setTitleY("#theta bin/layer");
                    if(il==0) gr_fit.setMarkerColor(1);
                    else      gr_fit.setMarkerColor(this.markerColor[(il-1)/6]);
                    gr_fit.setMarkerStyle(this.markerStyle[ip-1]);
                    gr_fit.setMarkerSize(this.markerSize);
                    residuals.addDataSet(gr_fit, is);                    
                }               
            }
        }
        return residuals;        
    }
    
    private DataGroup getSectorHistograms(Table alignment, int icol) {

        DataGroup residuals = new DataGroup(3,2);
        for(int is=0; is<Constants.NSECTOR; is++ ) {
            int sector = is+1;
            H1F hi_res = new H1F("hi_res_S" + sector, "Residual (um)", "Counts", 100, -300, 300);
            hi_res.setTitle("Sector " + sector);
            hi_res.setLineColor(icol);
            residuals.addDataSet(hi_res, is);
            for(int it=1; it<thetaBins.length; it++) {
                for(int ip=1; ip<phiBins.length; ip++) {
                    for (int il = 0; il <= Constants.NLAYER; il++) {
                        double shift = histos.get("nominal").getParValues(sector, it, ip)[il]
                                     - this.getFittedResidual(alignment, sector, it, ip)[il];
                        hi_res.fill(shift);
                    }
                }        
            }
        }
        return residuals;        
    }

    private Bin[] getBins(String binsString) {
        Bin[] binArray = null;
        if(binsString.contains(":")) {
            String[] bins = binsString.split(":");
            binArray = new Bin[bins.length];
            binArray[0] = new Bin(-Double.MAX_VALUE,Double.MAX_VALUE);
            for(int i=1; i<bins.length; i++) {
                double low  = Double.parseDouble(bins[i-1].trim());
                double high = Double.parseDouble(bins[i].trim());
                binArray[i] = new Bin(low,high);
                System.out.println(binArray[i].toString());
            }
        }
        else {
            binArray = new Bin[1];
            binArray[0] = new Bin(-Double.MAX_VALUE,Double.MAX_VALUE); 
        }
        return binArray;
    }
    
    private String getBinString(Bin[] bins) {
        String binString = "";
        if(bins.length>1) {
            binString += bins[1].getMin();
            for(int i=1; i<bins.length; i++)
                binString += ":" + bins[i].getMax();
        }
        return binString;
    }
    
    private int setActiveParameters() {
        int nActive =0;
        for(int i=0; i<Constants.NPARS; i++) {
            if(!this.histos.containsKey(Constants.PARNAME[i]))
                Constants.PARSTEP[i] = 0;
            else
                nActive++;
        }
        return nActive;
    }

    private void setAngularBins(String thetabins, String phibins) {
        System.out.println("[CONFIG] Setting theta bins to:");
        thetaBins = this.getBins(thetabins);
        System.out.println("[CONFIG] Setting phi bins to:");
        phiBins   = this.getBins(phibins);
    }

    public Bin[] getThetaBins() {
        return thetaBins;
    }

    public Bin[] getPhiBins() {
        return phiBins;
    }   
    
    public JTabbedPane getCanvases() {
        this.initGraphics();
        JTabbedPane panel = new JTabbedPane();
        panel.add("analysis", this.analyzeFits());
        panel.add("electron", this.histos.get("nominal").getElectronPlots());
        for(String key : histos.keySet()) {
            panel.add(key, histos.get(key).plotHistos());
        }
        return panel;
    }
    
    public static List<String> getFileNames(String input) {
        List<String> inputfiles = new ArrayList<>();
        File file = new File(input);
        if(file.isDirectory()) {
//            System.out.println("is a directory");
          for(String filename : file.list()) {
              if(filename.toLowerCase().endsWith(".hipo"))
                  inputfiles.add(input + "/" + filename);
          }
        }
        else {
           inputfiles.add(input);
        }
        return inputfiles;
    }
    
    public String[] getInputs() {
        return inputs;
    }
    
    public Table getGlobalOffsets(Table table) {
            
        Point3D target = new Point3D(0,0,0);
        Point3D[] idealRegion   = new Point3D[3];
        Vector3D[] toIdealRegion = new Vector3D[3];
        for(int ir=0; ir<Constants.NREGION; ir++) {
            Vector3d p = dcDetector.getRegionMidpoint(ir);
            idealRegion[ir] = new Point3D(p.x*0, p.y, p.z);
            toIdealRegion[ir] = target.vectorTo(idealRegion[ir]);
        }
        
        Table global = table.copy();
        for(int is=0; is<Constants.NSECTOR; is++) {
            int sector = is+1;
            Parameter[] pars = table.getParameters(sector);
            Point3D[] shiftedRegion = new Point3D[3];
            Vector3D[] toShiftedRegion = new Vector3D[3];
            for(int ir=0; ir<Constants.NREGION; ir++) {
                shiftedRegion[ir] = new Point3D(pars[ir*6+0].value()+idealRegion[ir].x(), 
                                                pars[ir*6+1].value()+idealRegion[ir].y(), 
                                                pars[ir*6+2].value()+idealRegion[ir].z());
                toShiftedRegion[ir] = target.vectorTo(shiftedRegion[ir]);               
            }
            // apply scale factor based on region 1 position
            int refRegion = 1;
            Vector3D[] refRegionScaled = new Vector3D[3];
            for(int ir=0; ir<Constants.NREGION; ir++) {
                refRegionScaled[ir] = toShiftedRegion[refRegion-1].multiply(toShiftedRegion[ir].mag()/toShiftedRegion[refRegion-1].mag());
//                System.out.println(toIdealRegion[ir]);
//                System.out.println(refRegionScaled[ir]);
//                System.out.println(toShiftedRegion[ir]);
                pars[ir*6 + 0].setValue(refRegionScaled[ir].x()-toIdealRegion[ir].x());
                pars[ir*6 + 1].setValue(refRegionScaled[ir].y()-toIdealRegion[ir].y());
                pars[ir*6 + 2].setValue(refRegionScaled[ir].z()-toIdealRegion[ir].z());
                for(int ic=0; ic<3; ic++) {
                    pars[ir*6 + ic].setError(0);
                    pars[ir*6 + ic+3].setValue(0);
                    pars[ir*6 + ic+3].setError(0);
                }
            }
            global.update(sector, pars);
        }       
        return global;
    }
    
    public Parameter[] removeGlobalComponent(Parameter[] pars) {
            
        Point3D target = new Point3D(0,0,0);
        Point3D[] idealRegion   = new Point3D[3];
        Vector3D[] toIdealRegion = new Vector3D[3];
        for(int ir=0; ir<Constants.NREGION; ir++) {
            Vector3d p = dcDetector.getRegionMidpoint(ir);
            idealRegion[ir] = new Point3D(p.x*0, p.y, p.z);
            toIdealRegion[ir] = target.vectorTo(idealRegion[ir]);
        }
        
        Point3D[] shiftedRegion = new Point3D[3];
        Vector3D[] toShiftedRegion = new Vector3D[3];
        for(int ir=0; ir<Constants.NREGION; ir++) {
            shiftedRegion[ir] = new Point3D(pars[ir*6+0].value()+idealRegion[ir].x(), 
                                            pars[ir*6+1].value()+idealRegion[ir].y(), 
                                            pars[ir*6+2].value()+idealRegion[ir].z());
            toShiftedRegion[ir] = target.vectorTo(shiftedRegion[ir]);               
        }
        
        // apply scale factor based on region 1 position
        Parameter[] relativePars = new Parameter[Constants.NPARS];
        for(int i=0; i<Constants.NPARS; i++) relativePars[i] = pars[i].copy();
        int refRegion = 1;
        Vector3D[] refRegionScaled = new Vector3D[3];
        for(int ir=0; ir<Constants.NREGION; ir++) {
            refRegionScaled[ir] = toShiftedRegion[refRegion-1].multiply(toShiftedRegion[ir].mag()/toShiftedRegion[refRegion-1].mag());
            relativePars[ir+6 + 0].setValue(pars[ir*6 + 0].value() - (refRegionScaled[ir].x()-toIdealRegion[ir].x()));
            relativePars[ir+6 + 1].setValue(pars[ir*6 + 1].value() - (refRegionScaled[ir].y()-toIdealRegion[ir].y()));
            relativePars[ir+6 + 2].setValue(pars[ir*6 + 2].value() - (refRegionScaled[ir].z()-toIdealRegion[ir].z()));
        }              
        return relativePars;
    }


    public void processFiles(int maxEvents) {
        for(String key : histos.keySet()) {
            if(histos.containsKey(key)) histos.get(key).processFiles(maxEvents);
        }
    }

    public void readHistos(String fileName, String optStats) {
        System.out.println("Opening file: " + fileName);
        PrintStream pipeStream = new PrintStream(pipeOut);
        System.setOut(pipeStream);
        System.setErr(pipeStream);
        TDirectory dir = new TDirectory();
        dir.readFile(fileName);
        System.out.println(dir.getDirectoryList());
        String folder = dir.getDirectoryList().get(0);
        String[] bins = folder.split("_");
        this.setAngularBins(bins[1], bins[2]);
        dir.cd("/" + folder);
        dir.ls();
        for(Object entry : dir.getDir().getDirectoryMap().entrySet()) {
            Map.Entry<String,Directory> object = (Map.Entry<String,Directory>) entry;
            String key = object.getKey();
            boolean shift = !key.equals("nominal") && subtractedShifts;
            this.addHistoSet(key, new Histo(shift, thetaBins, phiBins, optStats));
            histos.get(key).readDataGroup(folder+"/"+key, dir);
        }
        System.setOut(outStream);
        System.setErr(errStream);
        this.setAngularBins(bins[1],bins[2]); // just to get the printout
    }

    public void saveHistos(String fileName) {
        System.out.println("\nSaving histograms to file " + fileName);
        PrintStream pipeStream = new PrintStream(pipeOut);
        System.setOut(pipeStream);
        System.setErr(pipeStream);
        TDirectory dir = new TDirectory();
        String folder = "angles_" + this.getBinString(thetaBins) + "_" + this.getBinString(phiBins);
        dir.mkdir("/" + folder);
        dir.cd(folder);
        for(String key : histos.keySet()) {
            histos.get(key).writeDataGroup(folder, key, dir);
        }
        dir.writeFile(fileName);
        System.setOut(outStream);
        System.setErr(errStream);
    }    

    public static void main(String[] args){
        
        Alignment align = new Alignment();
        String[] inputs = align.getInputs();
            

        OptionStore parser = new OptionStore("dc-alignment");
        
        // valid options for event-base analysis
        parser.addCommand("-process", "process event files");
        parser.getOptionParser("-process").addOption("-o"        ,"",              "output histogram file name prefix");
        parser.getOptionParser("-process").addOption("-nevent"   ,"-1",            "maximum number of events to process");
        parser.getOptionParser("-process").addRequired("-" + inputs[0],            "nominal geometry hipo file or directory");
        for(int i=1; i<inputs.length; i++) parser.getOptionParser("-process").addOption("-" + inputs[i], "", inputs[i] + " hipo file or directory");
        parser.getOptionParser("-process").addOption("-display"  ,"1",             "display histograms (0/1)");
        parser.getOptionParser("-process").addOption("-stats"    ,"",              "histogram stat option");
        parser.getOptionParser("-process").addOption("-theta"    , "5:10:20",      "theta bin limits, e.g. \"5:10:20:30\"");
        parser.getOptionParser("-process").addOption("-phi"      , "-30:0:30",     "phi bin limits, e.g. \"-30:-10:0:10:30\"");
        parser.getOptionParser("-process").addOption("-shifts"   , "0",            "use event-by-event subtraction for unit shifts (1=on, 0=off)");
        parser.getOptionParser("-process").addOption("-time"     , "0",            "make time residual histograms (1=true, 0=false)");
        parser.getOptionParser("-process").addOption("-residuals", "2",            "fit residuals (2) or use mean (1)");
        parser.getOptionParser("-process").addOption("-vertex"   , "4",            "fit vertex plots with 3 gaussians (4), 2 gaussians (3), 1 gaussian plus background (2) or only 1 gaussian (1)");
        parser.getOptionParser("-process").addOption("-sector"   , "1",            "sector-dependent derivatives (1) or average (0)");
        parser.getOptionParser("-process").addOption("-compare"  , "default",      "database variation for constant comparison");
        parser.getOptionParser("-process").addOption("-init"     , "default",      "init global fit from previous constants from the selected variation");
        parser.getOptionParser("-process").addOption("-iter"     , "1",            "number of global fit iterations");
        parser.getOptionParser("-process").addOption("-verbose"  , "0",            "global fit verbosity (1/0 = on/off)");
        
        // valid options for histogram-base analysis
        parser.addCommand("-analyze", "analyze histogram files");
        parser.getOptionParser("-analyze").addRequired("-input"  ,                 "input histogram file");
        parser.getOptionParser("-analyze").addOption("-display"  ,"1",             "display histograms (0/1)");
        parser.getOptionParser("-analyze").addOption("-stats"    ,"",              "set histogram stat option");
        parser.getOptionParser("-analyze").addOption("-shifts"   , "0",            "use event-by-event subtraction for unit shifts (1=on, 0=off)");
        parser.getOptionParser("-analyze").addOption("-residuals", "2",            "fit residuals (2) or use mean (1)");
        parser.getOptionParser("-analyze").addOption("-vertex"   , "4",            "fit vertex plots with 3 gaussians (4), 2 gaussians (3), 1 gaussian plus background (2) or only 1 gaussian (1)");
        parser.getOptionParser("-analyze").addOption("-sector"   , "1",            "sector-dependent derivatives (1) or average (0)");
        parser.getOptionParser("-analyze").addOption("-compare"  , "default",      "database variation for constant comparison");
        parser.getOptionParser("-analyze").addOption("-init"     , "default",      "init global fit from previous constants from the selected variation");
        parser.getOptionParser("-analyze").addOption("-iter"     , "1",            "number of global fit iterations");
        parser.getOptionParser("-analyze").addOption("-verbose"  , "0",            "global fit verbosity (1/0 = on/off)");
        
        // valid options for final minuit-fit
        parser.addCommand("-fit", "perform misalignment fit");
        parser.getOptionParser("-fit").addRequired("-input"  ,                 "input histogram file");
        parser.getOptionParser("-fit").addOption("-display"  ,"1",             "display histograms (0/1)");
        parser.getOptionParser("-fit").addOption("-stats"    ,"",              "set histogram stat option");
        parser.getOptionParser("-fit").addOption("-shifts"   , "0",            "use event-by-event subtraction for unit shifts (1=on, 0=off)");
        parser.getOptionParser("-fit").addOption("-sector"   , "1",            "sector-dependent derivatives (1) or average (0)");
        parser.getOptionParser("-fit").addOption("-compare"  , "default",      "database variation for constant comparison");
        parser.getOptionParser("-fit").addOption("-init"     , "default",      "init global fit from previous constants from the selected variation");
        parser.getOptionParser("-fit").addOption("-iter"     , "1",            "number of global fit iterations");
        parser.getOptionParser("-fit").addOption("-verbose"  , "0",            "global fit verbosity (1/0 = on/off)");
        
        parser.parse(args);
        
        boolean openWindow = false;
        
        
        if(parser.getCommand().equals("-process")) {
            int    maxEvents   = parser.getOptionParser("-process").getOption("-nevent").intValue();
            String namePrefix  = parser.getOptionParser("-process").getOption("-o").stringValue();  
            String histoName   = "histo.hipo";
            if(!namePrefix.isEmpty()) {
                histoName  = namePrefix + "_" + histoName;
            }
            String nominal     = parser.getOptionParser("-process").getOption("-nominal").stringValue();
            String thetaBins   = parser.getOptionParser("-process").getOption("-theta").stringValue();
            String phiBins     = parser.getOptionParser("-process").getOption("-phi").stringValue();
            String optStats    = parser.getOptionParser("-process").getOption("-stats").stringValue();
            boolean time       = parser.getOptionParser("-process").getOption("-time").intValue()!=0;
            int     residuals  = parser.getOptionParser("-process").getOption("-residuals").intValue();
            int     vertex     = parser.getOptionParser("-process").getOption("-vertex").intValue();
            boolean sector     = parser.getOptionParser("-process").getOption("-sector").intValue()!=0;
            boolean shifts     = parser.getOptionParser("-process").getOption("-shifts").intValue()!=0;
            String  compareVar = parser.getOptionParser("-process").getOption("-compare").stringValue();
            String  initVar    = parser.getOptionParser("-process").getOption("-init").stringValue();
            int     iter       = parser.getOptionParser("-process").getOption("-iter").intValue();
            boolean verbose    = parser.getOptionParser("-process").getOption("-verbose").intValue()!=0;
            openWindow         = parser.getOptionParser("-process").getOption("-display").intValue()!=0;
            if(!openWindow) System.setProperty("java.awt.headless", "true");
            
            align.setShiftsMode(shifts);
            align.setAngularBins(thetaBins, phiBins);
            align.setFitOptions(sector, iter, verbose);
            align.initConstants(11, initVar, compareVar);
            
            align.addHistoSet(inputs[0], new Histo(Alignment.getFileNames(nominal),align.getThetaBins(),align.getPhiBins(), time, optStats));
            for(int i=1; i<inputs.length; i++) {
                String input = parser.getOptionParser("-process").getOption("-" + inputs[i]).stringValue();
                if(!input.isEmpty()) { 
                    if(shifts)
                        align.addHistoSet(inputs[i], new Histo(Alignment.getFileNames(input), 
                                                               Alignment.getFileNames(nominal), 
                                                               align.getThetaBins(),align.getPhiBins(),optStats));
                    else 
                        align.addHistoSet(inputs[i], new Histo(Alignment.getFileNames(input), 
                                                               align.getThetaBins(),align.getPhiBins(),optStats));
                }
            }
            align.processFiles(maxEvents);
            align.analyzeHistos(residuals, vertex);
            align.saveHistos(histoName);
        }
        
        if(parser.getCommand().equals("-analyze")) {
            String  optStats   = parser.getOptionParser("-analyze").getOption("-stats").stringValue();
            int     residuals  = parser.getOptionParser("-analyze").getOption("-residuals").intValue();
            int     vertex     = parser.getOptionParser("-analyze").getOption("-vertex").intValue();
            boolean sector     = parser.getOptionParser("-analyze").getOption("-sector").intValue()!=0;
            boolean shifts     = parser.getOptionParser("-analyze").getOption("-shifts").intValue()!=0;
            String  compareVar = parser.getOptionParser("-analyze").getOption("-compare").stringValue();
            String  initVar    = parser.getOptionParser("-analyze").getOption("-init").stringValue();
            int     iter       = parser.getOptionParser("-analyze").getOption("-iter").intValue();
            boolean verbose    = parser.getOptionParser("-analyze").getOption("-verbose").intValue()!=0;
            openWindow         = parser.getOptionParser("-analyze").getOption("-display").intValue()!=0;
            if(!openWindow) System.setProperty("java.awt.headless", "true");

            String histoName   = parser.getOptionParser("-analyze").getOption("-input").stringValue();

            align.setShiftsMode(shifts);
            align.setFitOptions(sector, iter, verbose);
            align.initConstants(11, initVar, compareVar);
            align.readHistos(histoName, optStats);
            align.analyzeHistos(residuals, vertex);
        }
        
        if(parser.getCommand().equals("-fit")) {
            String  optStats   = parser.getOptionParser("-fit").getOption("-stats").stringValue();
            boolean shifts     = parser.getOptionParser("-fit").getOption("-shifts").intValue()!=0;
            boolean sector     = parser.getOptionParser("-fit").getOption("-sector").intValue()!=0;
            String  compareVar = parser.getOptionParser("-fit").getOption("-compare").stringValue();
            String  initVar    = parser.getOptionParser("-fit").getOption("-init").stringValue();
            int     iter       = parser.getOptionParser("-fit").getOption("-iter").intValue();
            boolean verbose    = parser.getOptionParser("-fit").getOption("-verbose").intValue()!=0;
            openWindow         = parser.getOptionParser("-fit").getOption("-display").intValue()!=0;
            if(!openWindow) System.setProperty("java.awt.headless", "true");

            String histoName   = parser.getOptionParser("-fit").getOption("-input").stringValue();

            align.setShiftsMode(shifts);
            align.setFitOptions(sector, iter, verbose);
            align.initConstants(11, initVar, compareVar);
            align.readHistos(histoName, optStats);
            align.analyzeHistos(0, 0);
        }

        if(openWindow) {
            JFrame frame = new JFrame("DC Alignment");
            frame.setSize(1200, 800);
            frame.add(align.getCanvases());
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }     
    }
}
