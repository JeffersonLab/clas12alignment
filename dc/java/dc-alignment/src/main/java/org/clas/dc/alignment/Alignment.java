package org.clas.dc.alignment;


import eu.mihosoft.vrl.v3d.Vector3d;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
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
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.graphics.EmbeddedCanvasTabbed;
import org.jlab.groot.graphics.EmbeddedPad;
import org.jlab.groot.graphics.IDataSetPlotter;
import org.jlab.groot.group.DataGroup;
import org.jlab.jnp.utils.options.OptionStore;
import org.jlab.logging.DefaultLogger;

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


    private Map<String,Histo> histos            = new LinkedHashMap();
    private String[]          inputs            = new String[Constants.NPARS+1];
    private Bin[]             thetaBins         = null;
    private Bin[]             phiBins           = null;
    private double[]          vertexRange       = null;
    private ConstantsManager  manager           = new ConstantsManager();
    private String            compareVariation  = null;
    private String            previousVariation = null;
    private String            initVariation     = null;
    private Table             compareAlignment  = null;
    private Table             previousAlignment = null;
    private Table             initAlignment     = null;
    private DCGeant4Factory   dcDetector        = null;
        
    private boolean           subtractedShifts  = true;
    private boolean           sectorShifts      = false;
    private int               fitIteration      = 1;
    private boolean           tscFrame          = true;
    private boolean           globalTranslation = false;
    
    private int               markerSize = 4;
    private int[]             markerColor = {2,3,4,5,7,9};
    private int[]             markerStyle = {2, 3, 1, 4};
    private String            fontName = "Arial";
    
    private static final Logger LOGGER = Logger.getLogger(Constants.LOGGERNAME);
    private static Level LEVEL = Level.CONFIG;
    
    public Alignment() {
        this.initInputs();
    }
    
    private void initConstants(int run, String initVariation, String previousVariation, String compareVariation) {
        ConstantProvider provider  = GeometryFactory.getConstants(DetectorType.DC, 11, "default");
        dcDetector = new DCGeant4Factory(provider, DCGeant4Factory.MINISTAGGERON, false);
        for(int isl=0; isl<Constants.NSUPERLAYER; isl++) {
            Constants.WPDIST[isl] = dcDetector.getWireMidpoint(isl, 0,0).distance(dcDetector.getWireMidpoint(isl, 0, 1))/2; 
        }
        this.compareVariation  = compareVariation;
        this.previousVariation = previousVariation;
        if(initVariation.isBlank())
            this.initVariation = this.previousVariation;
        else
            this.initVariation = initVariation;
        initAlignment     = this.getTable(run, this.initVariation);
        previousAlignment = this.getTable(run, this.previousVariation);
        compareAlignment  = this.getTable(run, this.compareVariation);
    }
    
    private void initLogger(Level level) {
        DefaultLogger.debug();
        LEVEL = level;
        try{
            SimpleFormatter formatter = new SimpleFormatter() {
                private static final String format = " %3$s %n";

                @Override
                public synchronized String format(LogRecord lr) {
                    return String.format("%s\n", lr.getMessage());
                }
            };  
            //Creating fileHandler
            Handler fileHandler    = new FileHandler(Constants.LOGGERNAME + ".log");
             
            //Assigning handlers to LOGGER object
            LOGGER.addHandler(fileHandler);
             
            //Setting logger format
            fileHandler.setFormatter(formatter);  

            //Setting levels to handlers and LOGGER
            LOGGER.setLevel(level);
        } 
        catch(IOException exception){
            LOGGER.log(Level.SEVERE, "Error occur in configuring Logging file", exception);
        }
        LOGGER.config("[CONFIG] Completed logger configuration, level set to " + LOGGER.getLevel().getName());
    }
    
    private void setLoggerLevel(Level level) {
        LOGGER.setLevel(level);
        for(Handler handler : LOGGER.getHandlers()) handler.setLevel(level);
    }
    
    public void setFitOptions(boolean sector, int iteration, boolean tsc, boolean global) {
        this.sectorShifts      = sector;
        this.fitIteration      = iteration;
        this.tscFrame          = tsc;
        this.globalTranslation = global;
        this.printConfig(sectorShifts,      "sectorShifts", "");
        this.printConfig(fitIteration,      "fitIteration", "");
        this.printConfig(tscFrame,          "tscFrame", "");
        this.printConfig(globalTranslation, "globalTranslation", "");
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
    
    private void initMeasurementWeights(double w) {
        Constants.MEASWEIGHTS = new double[Constants.NSECTOR][thetaBins.length][phiBins.length][Constants.NLAYER+Constants.NTARGET];
        for(int is=0; is<Constants.NSECTOR; is++) {
            for(int il=0; il<Constants.NLAYER+Constants.NTARGET; il++) {
                for(int it=1; it<thetaBins.length; it ++) {
                    for(int ip=1; ip<phiBins.length; ip++) {
                        Constants.MEASWEIGHTS[is][it][ip][il] = w;
                    }
                }
            }
        }
    }
    
    private void initVertexPar(int vertex, String pars) {
        if(!pars.isEmpty()) {
            double[] parValues = new double[pars.split(":").length];
            for(int i=0; i<parValues.length; i++) {
                parValues[i] = Double.parseDouble(pars.split(":")[i]);
            }
            Constants.initTargetPars(parValues);
        }
        else {
            if(Math.abs(vertex)==3)
                Constants.initTargetPars(Constants.RGFSUMMER2020);
            else if(Math.abs(vertex)==5)
                Constants.initTargetPars(Constants.RGMFALL2021);
            else if(Math.abs(vertex)==6)
                Constants.initTargetPars(Constants.RGCSUMMER2022);
            else if(Math.abs(vertex)==7)
                Constants.initTargetPars(Constants.RGDFALL2023);
        }
        if(vertex<0) vertex=0;
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
            alignment = new Table(manager.getConstants(run, table), tscFrame, globalTranslation);
        }
        return alignment;
    }
    
    private void printConfig(Object o, String name, String message) {
        String s = "[CONFIG] " + name + " set to " + o.toString();
        if(!message.isEmpty()) s += ": " + message;
        LOGGER.config(s);
    }
    
    public void addHistoSet(String name, Histo histo) {
        this.histos.put(name, histo);
    }
    
    public void analyzeHistos(int resFit, int vertexFit, String vertexPar, boolean test) {
        this.printConfig(resFit, "resFit", "");
        this.printConfig(vertexFit, "vertexFit", "");
        this.initMeasurementWeights(1.0);
        this.initVertexPar(vertexFit, vertexPar);
        for(String key : histos.keySet()) {
            if(test && !key.equals("nominal")) continue;
            LOGGER.log(LEVEL,"\nAnalyzing histos for variation " + key);
            histos.get(key).analyzeHisto(resFit, vertexFit);
            for(int i=0; i<Constants.NPARS; i++)
                if(key.equals(Constants.PARNAME[i])) Constants.PARACTIVE[i] = true;
        }
        this.printExclusionStats();
    }
    
    private void printExclusionStats() {
        LOGGER.log(Level.WARNING, "\nEcluded measurements because of failed fits:");
        for (int is = 0; is < Constants.NSECTOR; is++) {
            String si = "";
            int nexclude = 0;
            for (int it = 1; it < thetaBins.length; it++) {
                for (int ip = 1; ip < phiBins.length; ip++) {
                    for (int il = 0; il < Constants.NLAYER + Constants.NTARGET; il++) {
                        if (Constants.MEASWEIGHTS[is][it][ip][il] == 0) {
                            nexclude++;
                            si += String.format("\n\t\ttheta bin=%d phi bin=%d layer=%d", it, ip, il);
                        }
                    }
                }
            }
            si = "\tSector " + (is+1) + String.format(": %d/%d", nexclude, (thetaBins.length-1)*(phiBins.length-1)*(Constants.NLAYER+Constants.NTARGET)) + si;
            LOGGER.log(Level.WARNING, si);
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
        double[] shifts = new double[Constants.NLAYER+Constants.NTARGET];
        for(int layer=0; layer<Constants.NLAYER+Constants.NTARGET; layer++) {
           shifts[layer] = this.getShift(key, sector, layer, itheta, iphi);
        }
        return shifts;
    }
    
    private double[] getShiftsError(String key, int sector, int itheta, int iphi) {
        double[] errors = new double[Constants.NLAYER+Constants.NTARGET];
        for(int layer=0; layer<Constants.NLAYER+Constants.NTARGET; layer++) {
           errors[layer] = this.getShiftError(key, sector, layer, itheta, iphi);
        }
        return errors;
    }
    
    private double[] getShifts(String key, int itheta, int iphi) {
        double[] shifts = new double[Constants.NLAYER+Constants.NTARGET];
        for(int layer=0; layer<Constants.NLAYER+Constants.NTARGET; layer++) {
           shifts[layer] = this.getShift(key, layer, itheta, iphi);
        }
        return shifts;
    }
    
    private double[] getShiftsError(String key, int itheta, int iphi) {
        double[] errors = new double[Constants.NLAYER+Constants.NTARGET];
        for(int layer=0; layer<Constants.NLAYER+Constants.NTARGET; layer++) {
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
        double[] shift = new double[Constants.NLAYER+Constants.NTARGET];
        for(int layer=0; layer<Constants.NLAYER+Constants.NTARGET; layer++) {
           shift[layer] = this.getFittedResidual(alignment, sector, layer, itheta, iphi);
        }
        return shift;
    }
    
    private double[] getFittedResidualError(Table alignment, int sector, int itheta, int iphi) {
        double[] shift = new double[Constants.NLAYER+Constants.NTARGET];
        for(int layer=0; layer<Constants.NLAYER+Constants.NTARGET; layer++) {
           shift[layer] = this.getFittedResidualError(alignment,sector, layer, itheta, iphi);
        }
        return shift;
    }
    
    private double getShiftSizeError(String key, int sector) {
        return 0;
    }
    
    public EmbeddedCanvasTabbed analyzeFits() {
        // check beam offset
        this.getBeamOffset();
        
        EmbeddedCanvasTabbed canvas = new EmbeddedCanvasTabbed("nominal");
        LOGGER.log(LEVEL,"\nPlotting nominal geometry residuals");
        canvas.getCanvas("nominal").draw(this.getResidualGraphs(null));
        canvas.getCanvas().setFont(fontName);
        for(EmbeddedPad pad : canvas.getCanvas("nominal").getCanvasPads())
            pad.getAxisX().setRange(-2000, 2000);
        
        canvas.addCanvas("nominal vs. theta");
        canvas.getCanvas("nominal vs. theta").draw(this.getAngularGraph("fit",null));
        canvas.getCanvas().setFont(fontName);
        for(EmbeddedPad pad : canvas.getCanvas("nominal vs. theta").getCanvasPads())
            pad.getAxisX().setRange(-2000, 2000);
        
        canvas.addCanvas("time residuals vs. theta");
        canvas.getCanvas("time residuals vs. theta").draw(this.getAngularGraph("time",null));
        canvas.getCanvas().setFont(fontName);
        for(EmbeddedPad pad : canvas.getCanvas("time residuals vs. theta").getCanvasPads())
            pad.getAxisX().setRange(-2000, 2000);
        
        canvas.addCanvas("LR residuals vs. theta");
        canvas.getCanvas("LR residuals vs. theta").draw(this.getAngularGraph("LR",null));
        canvas.getCanvas().setFont(fontName);
        for(EmbeddedPad pad : canvas.getCanvas("LR residuals vs. theta").getCanvasPads())
            pad.getAxisX().setRange(-2000, 2000);
        
        canvas.addCanvas("vertex");
        canvas.getCanvas("vertex").draw(this.getVertexGraph("fit",null));
        canvas.getCanvas().setFont(fontName);
        for(EmbeddedPad pad : canvas.getCanvas("vertex").getCanvasPads())
            pad.getAxisY().setRange(-2, 2);
        
        canvas.addCanvas("residual mean and sigma");
        canvas.getCanvas("residual mean and sigma").draw(this.getSectorHistograms("fit",null, 1));
        canvas.getCanvas("residual mean and sigma").draw(this.getSectorHistograms("time",null, 4));
        this.canvasAutoScale(canvas.getCanvas("residual mean and sigma"));
        canvas.getCanvas().setFont(fontName);
        canvas.addCanvas("residuals by region");
        canvas.getCanvas("residuals by region").draw(this.getRegionHistograms("fit",null, 1));
        canvas.getCanvas("residuals by region").draw(this.getRegionHistograms("time",null, 4));
        this.canvasAutoScale(canvas.getCanvas("residuals by region"));
        canvas.getCanvas().setFont(fontName);
                
        if(compareAlignment!=null && this.histos.size()>1) {
            LOGGER.log(LEVEL,"\nPlotting corrected geometry residuals");        
            canvas.addCanvas("CCDB corrected");
            canvas.getCanvas("CCDB corrected").draw(this.getResidualGraphs(compareAlignment.subtract(initAlignment)));
            canvas.getCanvas().setFont(fontName);
            for(EmbeddedPad pad : canvas.getCanvas("CCDB corrected").getCanvasPads())
                pad.getAxisX().setRange(-2000, 2000);

            canvas.addCanvas("CCDB corrected vs. theta");
            canvas.getCanvas("CCDB corrected vs. theta").draw(this.getAngularGraph("fit",compareAlignment.subtract(initAlignment)));
            canvas.getCanvas().setFont(fontName);
            for(EmbeddedPad pad : canvas.getCanvas("CCDB corrected vs. theta").getCanvasPads())
                pad.getAxisX().setRange(-2000, 2000);

            // shifts
            LOGGER.log(LEVEL,"\nPlotting shifted geometry residuals");
            canvas.addCanvas("shift magnitude");
            canvas.getCanvas("shift magnitude").draw(this.getShiftsHisto(1));
            this.canvasAutoScale(canvas.getCanvas("shift magnitude"));
            for(String key : histos.keySet()) {
                if(!key.equals("nominal")) {
                    canvas.addCanvas(key);
                    canvas.getCanvas(key).draw(this.getShiftsGraph(key));
                    canvas.getCanvas().setFont(fontName);
                    for(EmbeddedPad pad : canvas.getCanvas(key).getCanvasPads())
                        pad.getAxisX().setRange(-1000, 1000);
                }
            }
            LOGGER.log(LEVEL,"\nFitting residuals");
            LOGGER.log(LEVEL,"\nInitial alignment parameters (variation: " + this.previousVariation + ") in the DC tilted sector frame\n" + this.previousAlignment.toString());
            LOGGER.log(LEVEL,"\nInitial alignment parameters (variation: " + this.previousVariation + ") in CCDB format\n" + this.previousAlignment.toCCDBTable());
            Table fittedAlignment = new Table(tscFrame, globalTranslation);
            if(this.setActiveParameters()>0) {
                for(int is=0; is<Constants.NSECTOR; is++) {
                    int sector = is+1;
                    Parameter[] par = this.fit(sector);
                    fittedAlignment.update(sector, par);
                }
                Table finalAlignment = fittedAlignment.copy().add(previousAlignment);
                LOGGER.log(LEVEL,"\nFitted alignment parameters in the DC tilted sector frame\n" +fittedAlignment.toString());
                LOGGER.log(LEVEL,"\nFinal alignment parameters in CCDB format (sum of this and previous iteration costants)\n" +finalAlignment.toCCDBTable());
                LOGGER.log(LEVEL,"\nCompare to " +this.compareVariation + " variation constants\n" +this.compareAlignment.toCCDBTable());
                finalAlignment.toFile("dc-alignment.txt");
                
                canvas.addCanvas("corrected (with new parameters)");
                canvas.getCanvas("corrected (with new parameters)").draw(this.getResidualGraphs(fittedAlignment));
                canvas.getCanvas().setFont(fontName);
                for(EmbeddedPad pad : canvas.getCanvas("corrected (with new parameters)").getCanvasPads())
                    pad.getAxisX().setRange(-2000, 2000);

                canvas.addCanvas("corrected (with new parameters) vs. theta");
                canvas.getCanvas("corrected (with new parameters) vs. theta").draw(this.getAngularGraph("fit",fittedAlignment));
                canvas.getCanvas().setFont(fontName);
                for(EmbeddedPad pad : canvas.getCanvas("corrected (with new parameters) vs. theta").getCanvasPads())
                    pad.getAxisX().setRange(-2000, 2000); 
                DataGroup comAlignPars = this.compareAlignment.getDataGroup(1);
                DataGroup preAlignPars = this.previousAlignment.getDataGroup(3);
                DataGroup newAlignPars = finalAlignment.getDataGroup(2);
                canvas.addCanvas("before/after");
                canvas.getCanvas("before/after").draw(this.getSectorHistograms("fit",null, 1));
                canvas.getCanvas("before/after").draw(this.getSectorHistograms("fit",compareAlignment.subtract(previousAlignment), 3));
                canvas.getCanvas("before/after").draw(this.getSectorHistograms("fit",fittedAlignment, 2));
                this.canvasAutoScale(canvas.getCanvas("before/after"));
                canvas.getCanvas().setFont(fontName);
                canvas.addCanvas("before/after by region");
                canvas.getCanvas("before/after by region").draw(this.getRegionHistograms("fit",null, 1));
                canvas.getCanvas("before/after by region").draw(this.getRegionHistograms("fit",compareAlignment.subtract(previousAlignment), 3));
                canvas.getCanvas("before/after by region").draw(this.getRegionHistograms("fit",fittedAlignment, 2));
                this.canvasAutoScale(canvas.getCanvas("before/after by region"));
                canvas.getCanvas().setFont(fontName);
                canvas.addCanvas("misalignments");
                canvas.getCanvas("misalignments").draw(comAlignPars);
                canvas.getCanvas("misalignments").draw(newAlignPars);
                canvas.getCanvas().setFont(fontName);
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
                canvas.getCanvas().setFont(fontName);
                for(int i=0; i<canvas.getCanvas("internal-only").getCanvasPads().size(); i++) {
                    EmbeddedPad pad = canvas.getCanvas("internal-only").getCanvasPads().get(i);
                    if(i<Constants.NSECTOR) 
                        pad.getAxisX().setRange(-0.5, 0.5);
                    else
                        pad.getAxisX().setRange(-0.199, 0.201);
                }
                canvas.addCanvas("clas12 frame");
                Table compareGlobal = compareAlignment.toCLAS12Frame();
                Table finalGlobal   = finalAlignment.toCLAS12Frame();
                canvas.getCanvas("clas12 frame").draw(compareGlobal.getDataGroup(1));
                canvas.getCanvas("clas12 frame").draw(finalGlobal.getDataGroup(2));
                canvas.getCanvas().setFont(fontName);
                for(int i=0; i<canvas.getCanvas("clas12 frame").getCanvasPads().size(); i++) {
                    EmbeddedPad pad = canvas.getCanvas("clas12 frame").getCanvasPads().get(i);
                    if(i<Constants.NSECTOR) 
                        pad.getAxisX().setRange(-0.5, 0.5);
                    else
                        pad.getAxisX().setRange(-0.199, 0.201);
                }
            }
        }
        return canvas;
    }
    
    private void canvasAutoScale(EmbeddedCanvas canvas) {
        for(EmbeddedPad pad : canvas.getCanvasPads()){
            double yMax = -1;
            for(IDataSetPlotter ds : pad.getDatasetPlotters()) {
                if(ds.getDataSet() instanceof H1F) {
                    yMax = Math.max(yMax, ds.getDataSet().getMax());
                }
            }
            if(yMax>0) pad.getAxisY().setRange(0, yMax*1.2);
        }
    }
    
    private Parameter[] fit(int sector) {
        String options = "";
        double[][][][] shifts = new double[Constants.NPARS][Constants.NLAYER+Constants.NTARGET][thetaBins.length-1][phiBins.length-1];
        double[][][]   values = new double[Constants.NLAYER+Constants.NTARGET][thetaBins.length-1][phiBins.length-1];
        double[][][]   errors = new double[Constants.NLAYER+Constants.NTARGET][thetaBins.length-1][phiBins.length-1];
        for(int i=0; i<Constants.NPARS+1; i++) {
            for(int il=0; il<Constants.NLAYER+Constants.NTARGET; il++) {
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
        residualFitter.setPars(compareAlignment.subtract(previousAlignment).getParameters(sector));
        LOGGER.log(LEVEL,String.format("\nSector %d", sector));
        LOGGER.log(LEVEL,"Chi2 and benchmark with constants from variation " + this.compareVariation + ":");
        residualFitter.printChi2AndNDF();
        // reinit
        residualFitter.setPars(initAlignment.subtract(previousAlignment).getParameters(sector));
        LOGGER.log(LEVEL,String.format("\nSector %d", sector));
        LOGGER.log(LEVEL,"Chi2 and benchmark with initial constants from variation " + this.initVariation + ":");
        residualFitter.printChi2AndNDF();
        LOGGER.log(LEVEL,"Current minuit results:");
        double chi2 = Double.POSITIVE_INFINITY;
        Parameter[] fittedPars = residualFitter.getParCopy();
        String benchmark = "";
        for(int i=0; i<fitIteration; i++) {
            LOGGER.log(LEVEL,"iteration "+i + "\t" + benchmark);
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
        LOGGER.log(LEVEL,"");
        residualFitter.setPars(fittedPars);
        residualFitter.printChi2AndNDF();
        residualFitter.printPars();
        return fittedPars;
    }

    private void getBeamOffset() {
        double[][] offset = this.histos.get("nominal").getBeamOffset();
        LOGGER.log(LEVEL,String.format("\nBeam offset from scattering-chamber exit window analysis: x=(%.3f \u00B1 %.3f), y=(%.3f \u00B1 %.3f)", 
                                                                                   offset[0][0], offset[0][1], offset[1][0], offset[1][1]));
    }
    
    private DataGroup getResidualGraphs(Table alignment) {

        DataGroup residuals = new DataGroup(2,3);
        for(int it=1; it<thetaBins.length; it ++) {
            for(int ip=1; ip<phiBins.length; ip++) {
                for(int is=0; is<Constants.NSECTOR; is++ ) {
                    int sector = is+1;
                    double[] shiftRes = new double[Constants.NLAYER+Constants.NTARGET];
                    double[] errorRes = new double[Constants.NLAYER+Constants.NTARGET];
                    GraphErrors gr_fit = new GraphErrors("gr_fit_S" + sector + "_theta " + it + "_phi" + ip); 
                    for (int il = 0; il < Constants.NLAYER+Constants.NTARGET; il++) {
                        shiftRes[il] = histos.get("nominal").getParValues(sector, it, ip)[il]
                                     - this.getFittedResidual(alignment, sector, it, ip)[il];
                        errorRes[il] = Math.sqrt(Math.pow(histos.get("nominal").getParErrors(sector, it, ip)[il], 2)
                                              +0*Math.pow(this.getFittedResidualError(alignment, sector, it, ip)[il], 2));
                        if(Constants.MEASWEIGHTS[is][it][ip][il]>0)
                            gr_fit.addPoint(shiftRes[il], il, errorRes[il], 0);
                    }
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
        double[] layers = new double[Constants.NLAYER+Constants.NTARGET];
        double[] zeros  = new double[Constants.NLAYER+Constants.NTARGET];
        
        DataGroup shifts = new DataGroup(thetaBins.length,phiBins.length);
        for(int it=0; it<thetaBins.length; it ++) {
            for(int ip=0; ip<phiBins.length; ip++) {
                for(int is=0; is<Constants.NSECTOR; is++ ) {
                    int sector = is+1;
                    // residuals 
                    GraphErrors gr_res = new GraphErrors("gr_res_S" + sector  + "_theta " + it + "_phi" + ip);
                    for(int il=0; il<Constants.NLAYER+Constants.NTARGET; il++) {
                        if(Constants.MEASWEIGHTS[is][it][ip][il]>0)
                            gr_res.addPoint(this.getShifts(key, sector, it, ip)[il], il, this.getShiftsError(key, sector, it, ip)[il], 0);
                    }
                    gr_res.setTitle("Theta:"+ thetaBins[it].getRange() + " Phi:" + phiBins[ip].getRange());
                    gr_res.setTitleX("Residual (um)");
                    gr_res.setTitleY("Layer");
                    gr_res.setMarkerColor(this.markerColor[is]);
                    gr_res.setMarkerSize(this.markerSize);
                    if(gr_res.getDataSize(0)>0)
                        shifts.addDataSet(gr_res, ip*thetaBins.length+it);
                }
                GraphErrors gr_res = new GraphErrors("gr_res" + "_theta " + it + "_phi" + ip);
                for(int il=0; il<Constants.NLAYER+Constants.NTARGET; il++) {
                    double weight = 1;    
                    for(int is=0; is<Constants.NSECTOR; is++ ) {
                        if(Constants.MEASWEIGHTS[is][it][ip][il]==0) weight=0;
                    }
                    if(weight>0)
                        gr_res.addPoint(this.getShifts(key, it, ip)[il], il, this.getShiftsError(key, it, ip)[il], 0);
                }
                gr_res.setTitle("Theta:"+ thetaBins[it].getRange() + " Phi:" + phiBins[ip].getRange());
                gr_res.setTitleX("Residual (um)");
                gr_res.setTitleY("Layer");
                gr_res.setMarkerColor(1);
                gr_res.setMarkerSize(this.markerSize);
                if(gr_res.getDataSize(0)>0)
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
                H1F hi_res = new H1F("hi-res" + "_theta " + it + "_phi" + ip, "Shift", "#Deltaresidual (um)", nbin, 0, inputs.length-1);
                H1F hi_vtx = new H1F("hi-vtx" + "_theta " + it + "_phi" + ip, "Shift", "#Deltavertex (cm)",   nbin, 0, inputs.length-1);
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

    
    private DataGroup getAngularGraph(String parameter, Table alignment) {

        DataGroup residuals = new DataGroup(6,1);
        for(int is=0; is<Constants.NSECTOR; is++ ) {
            int sector = is+1;
            for(int ip=1; ip<phiBins.length; ip++) {
                for (int il = 0; il < Constants.NLAYER+Constants.NTARGET; il++) {
                    GraphErrors gr_fit = new GraphErrors("gr_fit_S" + sector + "_layer " + il + "_phi" + ip);
                    for(int it=1; it<thetaBins.length; it++) {
                        double angles   = it+0.9*(il-Constants.NLAYER/2)/Constants.NLAYER;
                        double shiftRes = histos.get("nominal").getParValues(parameter, sector, it, ip)[il];
                        double errorRes = 0.0;
                        if(!(parameter.equals("time") || parameter.equals("LR"))) {
                            shiftRes -= this.getFittedResidual(alignment, sector, it, ip)[il];
                            errorRes = Math.sqrt(Math.pow(histos.get("nominal").getParErrors(parameter,sector, it, ip)[il], 2)
                                                   + 0*Math.pow(this.getFittedResidualError(alignment, sector, it, ip)[il], 2));
                        }
                        if(Constants.MEASWEIGHTS[is][it][ip][il]>0 || parameter.equals("time") || parameter.equals("LR"))
                            gr_fit.addPoint(shiftRes, angles, errorRes, 0.0);
                        }
                    gr_fit.setTitle("Sector " + sector);
                    gr_fit.setTitleX("Residual (um)");
                    gr_fit.setTitleY("#theta bin/layer");
                    if(il==0 || il>=Constants.NLAYER+Constants.NTARGET-2) gr_fit.setMarkerColor(1);
                    else      gr_fit.setMarkerColor(this.markerColor[(il-1)/6]);
                    gr_fit.setMarkerStyle(this.markerStyle[ip-1]);
                    gr_fit.setMarkerSize(this.markerSize);
                    if(gr_fit.getDataSize(0)>0) residuals.addDataSet(gr_fit, is);                    
                }               
            }
        }
        return residuals;        
    }

    private DataGroup getVertexGraph(String parameter, Table alignment) {

        Map<Integer,List<GraphErrors>> graphs = new LinkedHashMap<>();
        for (int i = 0; i < Constants.NTARGET; i++) {
            int il = i==0 ? i : Constants.NLAYER+i;
            for(int it=1; it<thetaBins.length; it++) {
                GraphErrors gr_fit = new GraphErrors("gr_fit_layer " + il + "_theta" + it);
                for(int is=0; is<Constants.NSECTOR; is++ ) {
                    int sector = is+1;
                    for(int ip=1; ip<phiBins.length; ip++) {
                        double phi = is*60 + phiBins[ip].getMean();
                        if(phi<0) phi += 360;
                        double shiftRes = histos.get("nominal").getParValues(parameter, sector, it, ip)[il];
                        double errorRes = 0.0;
                        if(!(parameter.equals("time") || parameter.equals("LR"))) {
                            shiftRes -= this.getFittedResidual(alignment, sector, it, ip)[il];
                            errorRes = Math.sqrt(Math.pow(histos.get("nominal").getParErrors(parameter,sector, it, ip)[il], 2)
                                                   + 0*Math.pow(this.getFittedResidualError(alignment, sector, it, ip)[il], 2));
                        }
                        if(Constants.MEASWEIGHTS[is][it][ip][il]>0 || parameter.equals("time") || parameter.equals("LR"))
                            gr_fit.addPoint(phi, shiftRes/Constants.SCALE, 0.0, errorRes/Constants.SCALE);
                    }
                }               
                gr_fit.setTitle("Layer " + (il+1));
                gr_fit.setTitleX("#Deltaz (cm)");
                gr_fit.setTitleY("#phi (deg)");
                gr_fit.setMarkerColor(this.markerColor[it-1]);
                gr_fit.setMarkerSize(this.markerSize);
                if(gr_fit.getDataSize(0)>0) {
                    if(!graphs.containsKey(i))
                        graphs.put(i, new ArrayList<>());
                    graphs.get(i).add(gr_fit);
                }                    
            }
        }
        DataGroup dg = new DataGroup(1,graphs.size());
        for(int key : graphs.keySet()) {
            for(GraphErrors gr : graphs.get(key))
                dg.addDataSet(gr, key);
        }
        return dg;        
    }

    private DataGroup getSectorHistograms(String parameter, Table alignment, int icol) {

        DataGroup residuals = new DataGroup(6,2);
        for(int is=0; is<Constants.NSECTOR; is++ ) {
            int sector = is+1;
            H1F hi_res = new H1F("hi-res_S" + sector, "Residual mean (um)", "Counts", 100, -300, 300);
            H1F hi_sig = new H1F("hi-sig_S" + sector, "Residual sigma (um)", "Counts", 100, 0, 1000);
            hi_res.setTitle("Sector " + sector);
            hi_res.setLineColor(icol);
            hi_res.setOptStat("1101");
            hi_sig.setTitle("Sector " + sector);
            hi_sig.setLineColor(icol);
            hi_sig.setOptStat("1101");
            residuals.addDataSet(hi_res, is);
            residuals.addDataSet(hi_sig, is+6);
            for(int it=1; it<thetaBins.length; it++) {
                for(int ip=1; ip<phiBins.length; ip++) {
                    for (int il = 0; il < Constants.NLAYER+Constants.NTARGET; il++) {
                        double shift = histos.get("nominal").getParValues(parameter, sector, it, ip)[il];
                        if(!(parameter.equals("time") || parameter.equals("LR"))) {
                            shift -= this.getFittedResidual(alignment, sector, it, ip)[il];
                        }
                        double sigma = histos.get("nominal").getParSigmas(parameter, sector, it, ip)[il];
                        hi_res.fill(shift);
                        hi_sig.fill(sigma);
                    }
                }        
            }
        }
        return residuals;        
    }

    private DataGroup getRegionHistograms(String parameter, Table alignment, int icol) {

        DataGroup residuals = new DataGroup(Constants.NSECTOR,Constants.NREGION);
        for(int is=0; is<Constants.NSECTOR; is++ ) {
            int sector = is+1;
            for(int ir=0; ir<Constants.NREGION; ir++) {
                int region = ir+1;
                H1F hi_res = new H1F("hi-res_S" + sector + "_R" + region, "Residual mean (um)", "Counts", 100, -300, 300);
                hi_res.setTitle("Sector " + sector);
                hi_res.setLineColor(icol);
                hi_res.setOptStat("1101");
                residuals.addDataSet(hi_res, ir*Constants.NSECTOR + is);
                for(int it=1; it<thetaBins.length; it++) {
                    for(int ip=1; ip<phiBins.length; ip++) {
                        for (int il = 0; il < Constants.NLAYER/Constants.NREGION; il++) {
                            int ilayer = il+ir*Constants.NLAYER/Constants.NREGION;
                            double shift = histos.get("nominal").getParValues(parameter, sector, it, ip)[ilayer];
                            if(!(parameter.equals("time") || parameter.equals("LR"))) {
                                shift -= this.getFittedResidual(alignment, sector, it, ip)[ilayer];
                            }
                            hi_res.fill(shift);
                        }
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
        thetaBins = this.getBins(thetabins);
        LOGGER.config("[CONFIG] Setting theta bins to:");
        for(int i=1; i<thetaBins.length; i++) LOGGER.config(thetaBins[i].toString());
        phiBins   = this.getBins(phibins);
        LOGGER.config("[CONFIG] Setting phi bins to:");
        for(int i=1; i<phiBins.length; i++) LOGGER.config(phiBins[i].toString());
    }

    private void setTrackCuts(String range, double ecalMin) {
        String[] limits = range.split(":");
        this.vertexRange = new double[2];
        LOGGER.config("[CONFIG] Setting vertex range to:");
        if(limits.length==2) {
            this.vertexRange[0] = Double.parseDouble(limits[0]);
            this.vertexRange[1] = Double.parseDouble(limits[1]);            
        }
        else {
            vertexRange[0] = Constants.VTXMIN;
            vertexRange[1] = Constants.VTXMAX;
        }
        LOGGER.config("\t" + vertexRange[0]+" < vz < "+vertexRange[1] + " cm");
        LOGGER.config("[CONFIG] Setting ECAL energy cut to:");
        Constants.ECALMIN = ecalMin;
        LOGGER.config("\tenergy > " + Constants.ECALMIN + " GeV");        
    }
    
    private void setHitCuts(boolean doca, boolean alpha) {
        LOGGER.config("[CONFIG] Setting doca cuts to:");
        if(!doca) {
            for(int isl=0; isl<Constants.NSUPERLAYER; isl++) {
                Constants.DOCAMIN[isl] = 0.0;
                Constants.DOCAMAX[isl] = Double.MAX_VALUE;
                LOGGER.config(String.format("\tSuperlayer %d: 0 - inf cm", (isl+1)));
            }
        }
        else {
            for(int isl=0; isl<Constants.NSUPERLAYER; isl++) {
                Constants.DOCAMIN[isl] = 0.1;
                Constants.DOCAMAX[isl] = Constants.WPDIST[isl]*(0.9-0.02*isl);
                LOGGER.config(String.format("\tSuperlayer %d: %.2f - %.2f cm", (isl+1), Constants.DOCAMIN[isl], Constants.DOCAMAX[isl]));
            }
        }
        LOGGER.config("[CONFIG] Setting local angle cuts to:");
        if(!alpha) {
            Constants.ALPHACUT = Double.MAX_VALUE;
        }
        LOGGER.config("\t|trackTheta - 25 deg - alpha| < " + Constants.ALPHACUT + " deg");
    }
    
    public Bin[] getThetaBins() {
        return thetaBins;
    }

    public Bin[] getPhiBins() {
        return phiBins;
    }   
    
    public double[] getVertexRange() {
        return vertexRange;
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
            
        // apply global translation based on region 1 position
        int refRegion = 1;
        Table global = table.copy();
        for(int is=0; is<Constants.NSECTOR; is++) {
            int sector = is+1;
            Parameter[] pars = table.getParameters(sector);
            
            Point3D refPoint = new Point3D(pars[(refRegion-1)*6+0].value(),
                                           pars[(refRegion-1)*6+1].value(),
                                           pars[(refRegion-1)*6+2].value());
            for(int ir=0; ir<Constants.NREGION; ir++) {
                if(!this.globalTranslation || ir==0) {
                    pars[ir*6 + 0].setValue(refPoint.x());
                    pars[ir*6 + 1].setValue(refPoint.y());
                    pars[ir*6 + 2].setValue(refPoint.z());
                }
                else {
                    pars[ir*6 + 0].setValue(0);
                    pars[ir*6 + 1].setValue(0);
                    pars[ir*6 + 2].setValue(0);                    
                }
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
    
        public Table getGlobalOffsetsOld(Table table) {
            
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
        LOGGER.log(LEVEL,"Opening file: " + fileName);
        TDirectory dir = new TDirectory();
        dir.readFile(fileName);
        String folder = dir.getDirectoryList().get(0);
        String[] bins = folder.split("_");
        this.setAngularBins(bins[1], bins[2]);
        dir.cd("/" + folder);
        for(Object entry : dir.getDir().getDirectoryMap().entrySet()) {
            dir.cd("/" + folder);
            Map.Entry<String,Directory> object = (Map.Entry<String,Directory>) entry;
            String key = object.getKey();
            boolean shift = !key.equals("nominal") && subtractedShifts;
            Map<String,Directory> dgs = dir.getDirectoryByPath(key).getDirectoryMap();
            boolean time = dgs.containsKey("time");
            this.addHistoSet(key, new Histo(key, shift, thetaBins, phiBins, time, vertexRange, optStats));
            histos.get(key).readDataGroup(folder+"/"+key, dir);
        }
    }

    public void saveHistos(String fileName) {
        LOGGER.log(LEVEL,"\nSaving histograms to file " + fileName);
        TDirectory dir = new TDirectory();
        String folder = "angles_" + this.getBinString(thetaBins) + "_" + this.getBinString(phiBins);
        dir.mkdir("/" + folder);
        dir.cd(folder);
        for(String key : histos.keySet()) {
            histos.get(key).writeDataGroup(folder, key, dir);
        }
        dir.writeFile(fileName);
    }    

    public static void main(String[] args){
        
        Alignment align = new Alignment();
        align.initLogger(Level.CONFIG);
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
        parser.getOptionParser("-process").addOption("-residuals", "2",            "fit residuals with double gaussian (2), single gaussian (1), or use mean (0)");
        parser.getOptionParser("-process").addOption("-vertfit"  , "5",            "fit vertex plots with:\n" +
                                                                                   "\t\t- RG-E layout (10), dual target, \n" +
                                                                                   "\t\t- RG-A Spring18 layout (9)\n" +
                                                                                   "\t\t- RG-K layout (8), new cryotarget, \n" +
                                                                                   "\t\t- RG-D layout (7), new cryotarget, \n" +
                                                                                   "\t\t- RG-C layout (6),\n" +
                                                                                   "\t\t- 4 gaussians (5),\n" +
                                                                                   "\t\t- 3 gaussians (4),\n" +
                                                                                   "\t\t- 2 gaussians (3),\n" +
                                                                                   "\t\t- 1 gaussian plus background (2),\n" +
                                                                                   "\t\t- or only 1 gaussian (1)");
        parser.getOptionParser("-process").addOption("-vertpar"  , "",             "comma-separated vertex function parameters, default values are for Spring19 cryotarget with:\n" +
                                                                                   "\t\t- -0.5: target cell exit window position,\n" +
                                                                                   "\t\t-  5.0: target length,\n" +
                                                                                   "\t\t-  6.8: distance between the cell exit window and the insulation foil,\n" +
                                                                                   "\t\t- 27.3: distance between the scattering chamber exit window and the target center,\n" +
                                                                                   "\t\t leave empty to use defaults; units are cm");
        parser.getOptionParser("-process").addOption("-vertrange", "-20:35",       "comma-separated vertex histogram limits, e.g. -20:35; units are cm");
        parser.getOptionParser("-process").addOption("-ecal"     , "0.5",          "cut on ECAL energy (GeV) for track selection ");
        parser.getOptionParser("-process").addOption("-doca"     , "1",            "cut on doca (1) or not (0)");
        parser.getOptionParser("-process").addOption("-alpha"    , "1",            "cut on alpha, i.e. local angle, (1) or not (0)");
        parser.getOptionParser("-process").addOption("-sector"   , "1",            "sector-dependent derivatives (1) or average (0)");
        parser.getOptionParser("-process").addOption("-compare"  , "nominal",      "database variation for constant comparison");
        parser.getOptionParser("-process").addOption("-previous" , "nominal",      "database variation with previous iteration constants");
        parser.getOptionParser("-process").addOption("-init"     , "",             "init global fit from previous constants from the selected variation");
        parser.getOptionParser("-process").addOption("-iter"     , "1",            "number of global fit iterations");
        parser.getOptionParser("-process").addOption("-frame"    , "0",            "translations defined in the CLAS12 tilted sector frame (0) or sector frame (1)");
        parser.getOptionParser("-process").addOption("-global"   , "0",            "r1 translations defined as relative (0) or global (1) translations");
        parser.getOptionParser("-process").addOption("-verbose"  , "0",            "global fit verbosity (1/0 = on/off)");
        parser.getOptionParser("-process").addOption("-test"     , "0",            "analyze nominal geometry only for fit testing (1/0 = on/off)");

        // valid options for histogram-base analysis
        parser.addCommand("-analyze", "analyze histogram files");
        parser.getOptionParser("-analyze").setRequiresInputList(true);
        parser.getOptionParser("-analyze").addOption("-o"        ,"",              "output histogram file name prefix");
        parser.getOptionParser("-analyze").addOption("-display"  ,"1",             "display histograms (0/1)");
        parser.getOptionParser("-analyze").addOption("-stats"    ,"",              "set histogram stat option");
        parser.getOptionParser("-analyze").addOption("-shifts"   , "0",            "use event-by-event subtraction for unit shifts (1=on, 0=off)");
        parser.getOptionParser("-analyze").addOption("-residuals", "2",            "fit residuals (2), use mean (1), or use existing fit available (0)");
        parser.getOptionParser("-analyze").addOption("-vertfit"   , "5",           "fit vertex plots with:\n" +
                                                                                   "\t\t- RG-E layout (10), dual target, \n" +
                                                                                   "\t\t- RG-A Spring18 layout (9)\n" +
                                                                                   "\t\t- RG-K layout (8), new cryotarget, \n" +
                                                                                   "\t\t- RG-D layout (7), new cryotarget,\n" +
                                                                                   "\t\t- RG-C layout (6),\n" +
                                                                                   "\t\t- 4 gaussians (5),\n" +
                                                                                   "\t\t- 3 gaussians (4),\n" +
                                                                                   "\t\t- 2 gaussians (3),\n" +
                                                                                   "\t\t- 1 gaussian plus background (2),\n" +
                                                                                   "\t\t- or only 1 gaussian (1)");
        parser.getOptionParser("-analyze").addOption("-vertpar"  , "",             "comma-separated vertex function parameters, default values are for Spring19 cryotarget with:\n" +
                                                                                   "\t\t- -0.5: target cell exit window position,\n" +
                                                                                   "\t\t-  5.0: target length,\n" +
                                                                                   "\t\t-  6.8: distance between the cell exit window and the insulation foil,\n" +
                                                                                   "\t\t- 27.3: distance between the scattering chamber exit window and the target center,\n" +
                                                                                   "\t\tleave empty to use defaults; units are cm");
        parser.getOptionParser("-analyze").addOption("-sector"   , "1",            "sector-dependent derivatives (1) or average (0)");
        parser.getOptionParser("-analyze").addOption("-compare"  , "nominal",      "database variation for constant comparison");
        parser.getOptionParser("-analyze").addOption("-previous" , "nominal",      "database variation with previous iteration constants");
        parser.getOptionParser("-analyze").addOption("-init"     , "",             "init global fit from previous constants from the selected variation");
        parser.getOptionParser("-analyze").addOption("-iter"     , "1",            "number of global fit iterations");
        parser.getOptionParser("-analyze").addOption("-frame"    , "0",            "translations defined in the CLAS12 tilted sector frame (0) or sector frame (1)");
        parser.getOptionParser("-analyze").addOption("-global"   , "0",            "r1 translations defined as relative (0) or global (1) translations");
        parser.getOptionParser("-analyze").addOption("-verbose"  , "0",            "global fit verbosity (1/0 = on/off)");
        parser.getOptionParser("-analyze").addOption("-test"     , "0",            "analyze nominal geometry only for fit testing (1/0 = on/off)");
        
        // valid options for final minuit-fit
        parser.addCommand("-fit", "perform misalignment fit");
        parser.getOptionParser("-fit").setRequiresInputList(true);
        parser.getOptionParser("-fit").addOption("-display"  ,"1",             "display histograms (0/1)");
        parser.getOptionParser("-fit").addOption("-stats"    ,"",              "set histogram stat option");
        parser.getOptionParser("-fit").addOption("-shifts"   , "0",            "use event-by-event subtraction for unit shifts (1=on, 0=off)");
        parser.getOptionParser("-fit").addOption("-sector"   , "1",            "sector-dependent derivatives (1) or average (0)");
        parser.getOptionParser("-fit").addOption("-vertfit"  , "5",            "fit vertex plots with:\n" +
                                                                               "\t\t- RG-E layout (10), dual target, \n" +
                                                                               "\t\t- RG-A Spring18 layout (9)\n" +
                                                                               "\t\t- RG-K layout (8), new cryotarget, \n" +
                                                                               "\t\t- RG-D layout (7), new cryotarget, \n" +
                                                                               "\t\t- RG-C layout (6),\n" +
                                                                               "\t\t- 4 gaussians (5),\n" +
                                                                               "\t\t- 3 gaussians (4),\n" +
                                                                               "\t\t- 2 gaussians (3),\n" +
                                                                               "\t\t- 1 gaussian plus background (2),\n" +
                                                                               "\t\t- or only 1 gaussian (1)");
        parser.getOptionParser("-fit").addOption("-vertpar"  , "",             "comma-separated vertex function parameters, default values are for Spring19 cryotarget with:\n" +
                                                                               "\t\t- -0.5: target cell exit window position,\n" +
                                                                               "\t\t-  5.0: target length,\n" +
                                                                               "\t\t-  6.8: distance between the cell exit window and the insulation foil,\n" +
                                                                               "\t\t- 27.3: distance between the scattering chamber exit window and the target center,\n" +
                                                                               "\t\t leave empty to use defaults; units are cm");
        parser.getOptionParser("-fit").addOption("-compare"  , "nominal",      "database variation for constant comparison");
        parser.getOptionParser("-fit").addOption("-previous" , "nominal",      "database variation with previous iteration constants");
        parser.getOptionParser("-fit").addOption("-init"     , "",             "init global fit from previous constants from the selected variation");
        parser.getOptionParser("-fit").addOption("-iter"     , "1",            "number of global fit iterations");
        parser.getOptionParser("-fit").addOption("-frame"    , "0",            "translations defined in the CLAS12 tilted sector frame (0) or sector frame (1)");
        parser.getOptionParser("-fit").addOption("-global"   , "0",            "r1 translations defined as relative (0) or global (1) translations");
        parser.getOptionParser("-fit").addOption("-verbose"  , "0",            "global fit verbosity (1/0 = on/off)");
        
        parser.parse(args);
        
        boolean openWindow = false;
        String frameTitle = "DC Alignment";
        
        if(parser.getCommand().equals("-process")) {
            int    maxEvents   = parser.getOptionParser("-process").getOption("-nevent").intValue();
            String namePrefix  = parser.getOptionParser("-process").getOption("-o").stringValue();  
            String histoName   = "histo.hipo";
            if(!namePrefix.isEmpty()) {
                histoName  = namePrefix + "_" + histoName;
            }
            String   nominal      = parser.getOptionParser("-process").getOption("-nominal").stringValue();
            String   thetaBins    = parser.getOptionParser("-process").getOption("-theta").stringValue();
            String   phiBins      = parser.getOptionParser("-process").getOption("-phi").stringValue();
            String   optStats     = parser.getOptionParser("-process").getOption("-stats").stringValue();
            boolean  time         = parser.getOptionParser("-process").getOption("-time").intValue()!=0;
            int      residuals    = parser.getOptionParser("-process").getOption("-residuals").intValue();
            int      vertexFit    = parser.getOptionParser("-process").getOption("-vertfit").intValue();
            String   vertexPar    = parser.getOptionParser("-process").getOption("-vertpar").stringValue();   
            String   vertexRange  = parser.getOptionParser("-process").getOption("-vertrange").stringValue();
            double   ecalCut      = parser.getOptionParser("-process").getOption("-ecal").doubleValue();
            boolean  docaCut      = parser.getOptionParser("-process").getOption("-doca").intValue()!=0;
            boolean  alphaCut     = parser.getOptionParser("-process").getOption("-alpha").intValue()!=0;
            boolean  sector       = parser.getOptionParser("-process").getOption("-sector").intValue()!=0;
            boolean  shifts       = parser.getOptionParser("-process").getOption("-shifts").intValue()!=0;
            String   compareVar   = parser.getOptionParser("-process").getOption("-compare").stringValue();
            String   initVar      = parser.getOptionParser("-process").getOption("-init").stringValue();
            String   previousVar  = parser.getOptionParser("-process").getOption("-previous").stringValue();
            int      iter         = parser.getOptionParser("-process").getOption("-iter").intValue();
            boolean  tscFrame     = parser.getOptionParser("-process").getOption("-frame").intValue()==0;
            boolean  r1Global     = parser.getOptionParser("-process").getOption("-global").intValue()!=0;
            boolean  verbose      = parser.getOptionParser("-process").getOption("-verbose").intValue()!=0;
            boolean  testFit      = parser.getOptionParser("-process").getOption("-test").intValue()!=0;
            openWindow            = parser.getOptionParser("-process").getOption("-display").intValue()!=0;
            frameTitle = frameTitle + " - " + nominal;
            if(!openWindow) System.setProperty("java.awt.headless", "true");
            if(verbose)     align.setLoggerLevel(Level.FINE);
            
            align.setShiftsMode(shifts);
            align.setAngularBins(thetaBins, phiBins);
            align.initConstants(11, initVar, previousVar, compareVar);
            align.setTrackCuts(vertexRange, ecalCut);
            align.setHitCuts(docaCut, alphaCut);
            align.setFitOptions(sector, iter, tscFrame, r1Global);
            
            align.addHistoSet(inputs[0], new Histo(inputs[0], Alignment.getFileNames(nominal),align.getThetaBins(),align.getPhiBins(), time, align.getVertexRange(), optStats));
            for(int i=1; i<inputs.length; i++) {
                String input = parser.getOptionParser("-process").getOption("-" + inputs[i]).stringValue();
                if(!input.isEmpty()) { 
                    if(shifts)
                        align.addHistoSet(inputs[i], new Histo(inputs[i],
                                                               Alignment.getFileNames(input), 
                                                               Alignment.getFileNames(nominal), 
                                                               align.getThetaBins(),align.getPhiBins(),align.getVertexRange(),optStats));
                    else 
                        align.addHistoSet(inputs[i], new Histo(inputs[i],
                                                               Alignment.getFileNames(input), 
                                                               align.getThetaBins(),align.getPhiBins(),align.getVertexRange(),optStats));
                }
            }
            align.processFiles(maxEvents);
            align.analyzeHistos(residuals, vertexFit, vertexPar, testFit);
            align.saveHistos(histoName);
        }
        
        if(parser.getCommand().equals("-analyze")) {
            String namePrefix  = parser.getOptionParser("-analyze").getOption("-o").stringValue();  
            String histoName   = "histo.hipo";
            if(!namePrefix.isEmpty()) {
                histoName  = namePrefix + "_" + histoName;
            }
            String  optStats    = parser.getOptionParser("-analyze").getOption("-stats").stringValue();
            int     residuals   = parser.getOptionParser("-analyze").getOption("-residuals").intValue();
            int     vertexFit   = parser.getOptionParser("-analyze").getOption("-vertfit").intValue();
            String  vertexPar   = parser.getOptionParser("-analyze").getOption("-vertpar").stringValue();            
            boolean sector      = parser.getOptionParser("-analyze").getOption("-sector").intValue()!=0;
            boolean shifts      = parser.getOptionParser("-analyze").getOption("-shifts").intValue()!=0;
            String  compareVar  = parser.getOptionParser("-analyze").getOption("-compare").stringValue();
            String  initVar     = parser.getOptionParser("-analyze").getOption("-init").stringValue();
            String  previousVar = parser.getOptionParser("-analyze").getOption("-previous").stringValue();
            int     iter        = parser.getOptionParser("-analyze").getOption("-iter").intValue();
            boolean  tscFrame   = parser.getOptionParser("-analyze").getOption("-frame").intValue()==0;
            boolean  r1Global   = parser.getOptionParser("-analyze").getOption("-global").intValue()!=0;
            boolean verbose     = parser.getOptionParser("-analyze").getOption("-verbose").intValue()!=0;
            boolean testFit     = parser.getOptionParser("-analyze").getOption("-test").intValue()!=0;
            openWindow          = parser.getOptionParser("-analyze").getOption("-display").intValue()!=0;
            if(!openWindow) System.setProperty("java.awt.headless", "true");
            if(verbose)     align.setLoggerLevel(Level.FINE);

            String inputHisto   = parser.getOptionParser("-analyze").getInputList().get(0);
            frameTitle = frameTitle + " - " + inputHisto;

            align.setShiftsMode(shifts);
            align.setFitOptions(sector, iter, tscFrame, r1Global);
            align.initConstants(11, initVar, previousVar, compareVar);
            align.readHistos(inputHisto, optStats);
            align.analyzeHistos(residuals, vertexFit, vertexPar, testFit);
            align.saveHistos(histoName);
        }
        
        if(parser.getCommand().equals("-fit")) {
            String  optStats    = parser.getOptionParser("-fit").getOption("-stats").stringValue();
            boolean shifts      = parser.getOptionParser("-fit").getOption("-shifts").intValue()!=0;
            boolean sector      = parser.getOptionParser("-fit").getOption("-sector").intValue()!=0;
            int     vertexFit   = parser.getOptionParser("-fit").getOption("-vertfit").intValue();
            String  vertexPar   = parser.getOptionParser("-fit").getOption("-vertpar").stringValue();            
            String  compareVar  = parser.getOptionParser("-fit").getOption("-compare").stringValue();
            String  initVar     = parser.getOptionParser("-fit").getOption("-init").stringValue();
            String  previousVar = parser.getOptionParser("-fit").getOption("-previous").stringValue();
            int     iter        = parser.getOptionParser("-fit").getOption("-iter").intValue();
            boolean tscFrame    = parser.getOptionParser("-fit").getOption("-frame").intValue()==0;
            boolean r1Global    = parser.getOptionParser("-fit").getOption("-global").intValue()!=0;
            boolean verbose     = parser.getOptionParser("-fit").getOption("-verbose").intValue()!=0;
            openWindow          = parser.getOptionParser("-fit").getOption("-display").intValue()!=0;
            if(!openWindow) System.setProperty("java.awt.headless", "true");
            if(verbose)     align.setLoggerLevel(Level.FINE);

            String histoName   = parser.getOptionParser("-fit").getInputList().get(0);
            frameTitle = frameTitle + " - " + histoName;

            align.setShiftsMode(shifts);
            align.setFitOptions(sector, iter, tscFrame, r1Global);
            align.initConstants(11, initVar, previousVar, compareVar);
            align.readHistos(histoName, optStats);
            align.analyzeHistos(0, -vertexFit, vertexPar, false);
        }

        if(openWindow) {
            JFrame frame = new JFrame(frameTitle);
            frame.setSize(1200, 800);
            frame.add(align.getCanvases());
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }     
    }
}
