package org.clas.dc.alignment;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import org.jlab.detector.calib.utils.ConstantsManager;
import org.jlab.groot.base.GStyle;
import org.jlab.groot.data.Directory;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.IDataSet;
import org.jlab.groot.data.TDirectory;
import org.jlab.groot.graphics.EmbeddedCanvasTabbed;
import org.jlab.groot.graphics.EmbeddedPad;
import org.jlab.groot.group.DataGroup;
import org.jlab.jnp.utils.options.OptionStore;

/**
 *
 * @author devita
 */
public class Alignment {


    private Map<String,Histo> histos = new LinkedHashMap();
    private String[]          inputs = new String[19];
    private Bin[]             thetaBins = null;
    private Bin[]             phiBins   = null;
    private ConstantsManager  manager = new ConstantsManager();
    private Table             alignment = null;
        
    private boolean           residualFit  = false;
    private boolean           sectorDeriv  = false;
    private boolean           initFitPar   = false;
    private boolean           fitVerbosity = false;
    private int               fitIteration = 1;
    
    private int               markerSize = 4;
    private int[]             markerColor = {2,3,4,5,7,9};
    private int[]             markerStyle = {2,3};
    private String            fontName = "Arial";
    
    ByteArrayOutputStream pipeOut = new ByteArrayOutputStream();
    private static PrintStream outStream = System.out;
    private static PrintStream errStream = System.err;
    
    public Alignment() {
        this.initInputs();
    }
    
    private void initConstants(int run, String variation) {
        if(!variation.isEmpty()) {
            String table = "/geometry/dc/alignment";
            List<String> tables = new ArrayList<>();
            tables.add(table);
            manager.init(tables);
            manager.setVariation(variation);
            alignment = new Table(manager.getConstants(run, table));
        }
    }
    
    public void setFitOptions(boolean resFit, boolean sectorDer, boolean init, int iteration, boolean verbosity) {
        this.residualFit = resFit;
        this.sectorDeriv = sectorDer;
        this.initFitPar  = init;
        this.fitIteration = iteration;
        this.fitVerbosity = verbosity;
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
        String[] inputs = new String[19];
        String[] coord = {"x", "y", "z"};
        inputs[0] = "nominal";
        for(int ir=0; ir<3; ir++) {
            int r = ir+1;
            for(int ic=0; ic<3; ic++) {
                inputs[ir*6+ic+1] = "r" + r + "_"  + coord[ic];
                inputs[ir*6+ic+4] = "r" + r + "_c" + coord[ic];
            }
        }
        this.inputs = inputs;
    }
    
    public void addHistoSet(String name, Histo histo) {
        this.histos.put(name, histo);
    }
    
    public void analyzeHistos() {
        for(String key : histos.keySet()) {
            System.out.println("\nAnalyzing histos for variation " + key);
            histos.get(key).analyzeHisto(this.residualFit);
        }
    }
    
    private double getShift(String key, int sector, int layer, int itheta, int iphi) {
        double shift=0;
        if(histos.containsKey(key)) {
            shift = histos.get("nominal").getParValues(sector, itheta, iphi)[layer]-
                    histos.get(key).getParValues(sector, itheta, iphi)[layer];
        }
        return shift;
    }
       
    private double getShiftError(String key, int sector, int layer, int itheta, int iphi) {
        double error=0;
        if(histos.containsKey(key)) {
            error = Math.sqrt(Math.pow(histos.get(key).getParErrors(sector, itheta, iphi)[layer],2)+
                              Math.pow(histos.get("nominal").getParErrors(sector, itheta, iphi)[layer],2));
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
    
    private double getFittedResidual(int sector, int layer, int itheta, int iphi) {
        double value=0;
        if(alignment!=null) {
            for(String key : histos.keySet()) {
                if(!key.equals("nominal")) 
                    value += this.getShift(key, sector, layer, itheta, iphi)*
                             alignment.getShiftSize(key, sector)/Constants.UNITSHIFT;
            }
        }
        return value;
    }

    private double getFittedResidualError(int sector, int layer, int itheta, int iphi) {
        double value=0;
        if(alignment!=null) {
            for(String key : histos.keySet()) {
                if(!key.equals("nominal")) {
                    value += Math.pow(this.getShiftError(key, sector, layer, itheta, iphi)*alignment.getShiftSize(key, sector)/Constants.UNITSHIFT,2);
                    value += Math.pow(this.getShift(key, layer, itheta, iphi)*this.getShiftSizeError(key, sector)/Constants.UNITSHIFT,2);
                }
            }
        }
        return Math.sqrt(value);
    }

    private double[] getFittedResidual(int sector, int itheta, int iphi) {
        double[] shift = new double[Constants.NLAYER+1];
        for(int layer=0; layer<=Constants.NLAYER; layer++) {
           shift[layer] = this.getFittedResidual(sector, layer, itheta, iphi);
        }
        return shift;
    }
    
    private double[] getFittedResidualError(int sector, int itheta, int iphi) {
        double[] shift = new double[Constants.NLAYER+1];
        for(int layer=0; layer<=Constants.NLAYER; layer++) {
           shift[layer] = this.getFittedResidualError(sector, layer, itheta, iphi);
        }
        return shift;
    }
    
    private double getShiftSizeError(String key, int sector) {
        return 0;
    }
    
    public EmbeddedCanvasTabbed analyzeFits() {
        EmbeddedCanvasTabbed canvas = new EmbeddedCanvasTabbed("nominal residuals");
        System.out.println("\nPlotting nominal geometry residuals");
        canvas.getCanvas("nominal residuals").draw(this.getResidualGraphs(false));
        canvas.getCanvas().setFont(fontName);
        for(EmbeddedPad pad : canvas.getCanvas("nominal residuals").getCanvasPads())
            pad.getAxisX().setRange(-2000, 2000);
        
        System.out.println("\nPlotting corrected geometry residuals");        
        canvas.addCanvas("corrected residuals");
        canvas.getCanvas("corrected residuals").draw(this.getResidualGraphs(true));
        canvas.getCanvas().setFont(fontName);
        for(EmbeddedPad pad : canvas.getCanvas("corrected residuals").getCanvasPads())
            pad.getAxisX().setRange(-2000, 2000);
        
        // shifts
        System.out.println("\nPlotting shifted geometry residuals");
        for(String key : histos.keySet()) {
            if(!key.equals("nominal")) {
                canvas.addCanvas(key);
                canvas.getCanvas(key).draw(this.getShiftsGraph(key));
                canvas.getCanvas().setFont(fontName);
                for(EmbeddedPad pad : canvas.getCanvas(key).getCanvasPads())
                    pad.getAxisX().setRange(-1500, 1500);
            }
        }
        if(alignment!=null) {
            System.out.println("\nFitting residuals");
            System.out.println("\nInitial alignment parameters\n" + this.alignment.toString());
            for(int is=0; is<Constants.NSECTOR; is++) {
                int sector = is+1;
                Parameter[] par = this.fit(sector);
                alignment.update(sector, par);
            }
            System.out.println("\nFinal alignment parameters\n" +this.alignment.toString());

            canvas.addCanvas("fitted residuals");
            canvas.getCanvas("fitted residuals").draw(this.getResidualGraphs(true));
            canvas.getCanvas().setFont(fontName);
            for(EmbeddedPad pad : canvas.getCanvas("fitted residuals").getCanvasPads())
                pad.getAxisX().setRange(-2000, 2000);
        }
        return canvas;
    }
    
    private DataGroup getResidualGraphs(boolean correct) {
        double[] layers = new double[Constants.NLAYER+1];
        double[] zeros  = new double[Constants.NLAYER+1];
        for(int il=0; il<=Constants.NLAYER; il++) layers[il]=il;

        int scale=0;
        if(correct) scale = 1;
        DataGroup residuals = new DataGroup(3,2);
        for(int it=1; it<thetaBins.length-1; it ++) {
            for(int ip=1; ip<phiBins.length; ip++) {
                if(it+ip==1) continue;
                for(int is=0; is<Constants.NSECTOR; is++ ) {
                    int sector = is+1;
                    if(alignment!=null) {
                        double[] shiftRes = new double[Constants.NLAYER+1];
                        double[] errorRes = new double[Constants.NLAYER+1];
                        for (int il = 0; il <= Constants.NLAYER; il++) {
                            shiftRes[il] = histos.get("nominal").getParValues(sector, it, ip)[il]
                                         - scale*this.getFittedResidual(sector, it, ip)[il];
                            errorRes[il] = Math.sqrt(Math.pow(histos.get("nominal").getParErrors(sector, it, ip)[il], 2)
                                              +scale*0*Math.pow(this.getFittedResidualError(sector, it, ip)[il], 2));
                        }
                        GraphErrors gr_fit = new GraphErrors("gr_fit_S" + sector + "_theta " + it + "_phi" + ip, 
                                                             shiftRes, layers, errorRes, zeros);
                        gr_fit.setTitle("Sector " + sector);
                        gr_fit.setTitleX("Residual (um)");
                        gr_fit.setTitleY("Layer");
                        gr_fit.setMarkerColor(this.markerColor[(it-1)%6]);
                        gr_fit.setMarkerStyle(this.markerStyle[ip-1]);
                        gr_fit.setMarkerSize(this.markerSize);
                        residuals.addDataSet(gr_fit, is);
                    }
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
    
    private Parameter[] fit(int sector) {
        String options = "";
        if(fitVerbosity) options = "V";
        double[][][][] shifts = new double[Constants.NPARS][Constants.NLAYER+1][thetaBins.length-1][phiBins.length-1];
        double[][][]   values = new double[Constants.NLAYER+1][thetaBins.length-1][phiBins.length-1];
        double[][][]   errors = new double[Constants.NLAYER+1][thetaBins.length-1][phiBins.length-1];
        for(int i=0; i<Constants.NPARS+1; i++) {
            for(int il=0; il<=Constants.NLAYER; il++) {
                for(int it=1; it<thetaBins.length-1; it ++) {
                    for(int ip=1; ip<phiBins.length; ip++) {
                        if(i==0) {
                            values[il][it-1][ip-1] = histos.get("nominal").getParValues(sector, it, ip)[il];
                            errors[il][it-1][ip-1] = histos.get("nominal").getParErrors(sector, it, ip)[il];
                            if(il==0) values[il][it-1][ip-1] -= Constants.TARGETPOS;
                        }
                        else {
                            if(sectorDeriv)
                                shifts[i-1][il][it-1][ip-1] = this.getShift(Constants.PARNAME[i-1], sector, il, it, ip)/Constants.UNITSHIFT;
                            else
                                shifts[i-1][il][it-1][ip-1] = this.getShift(Constants.PARNAME[i-1], il, it, ip)/Constants.UNITSHIFT;
                        }
                    }
                }
            }
        }
        Fitter residualFitter = new Fitter(shifts, values, errors);
        if(initFitPar) residualFitter.setPars(alignment.getParameters(sector));
        for(int i=0; i<fitIteration; i++) {
            residualFitter.fit(options);
        }
        System.out.println("Sector " + sector + ": chi2=" + residualFitter.getChi2() + " NDF=" + residualFitter.getNDF());
        return residualFitter.getPars();
    }

    public EmbeddedCanvasTabbed plotResults() {
        System.out.println("\nPlotting results");
        EmbeddedCanvasTabbed canvas = new EmbeddedCanvasTabbed("theta dependence: nominal");
        canvas.getCanvas("theta dependence: nominal").draw(this.getAngularGraph(false));
        canvas.getCanvas().setFont(fontName);
        for(EmbeddedPad pad : canvas.getCanvas("theta dependence: nominal").getCanvasPads())
            pad.getAxisX().setRange(-2000, 2000);
        canvas.addCanvas("theta dependence: fitted");
        canvas.getCanvas("theta dependence: fitted").draw(this.getAngularGraph(true));
        canvas.getCanvas().setFont(fontName);
        for(EmbeddedPad pad : canvas.getCanvas("theta dependence: fitted").getCanvasPads())
            pad.getAxisX().setRange(-2000, 2000);
        return canvas;
    }

    private DataGroup getAngularGraph(boolean correct) {
        double[] zeros  = new double[thetaBins.length-1];

        int scale=0;
        if(correct) scale = 1;
        DataGroup residuals = new DataGroup(3,2);
        for(int is=0; is<Constants.NSECTOR; is++ ) {
            int sector = is+1;
            for(int ip=1; ip<phiBins.length; ip++) {
                for (int il = 0; il <= Constants.NLAYER; il++) {
                    double[] shiftRes = new double[thetaBins.length-1];
                    double[] errorRes = new double[thetaBins.length-1];
                    double[] angles   = new double[thetaBins.length-1];          
                    for(int it=1; it<thetaBins.length; it++) {
                        shiftRes[it-1] = histos.get("nominal").getParValues(sector, it, ip)[il]
                                       - scale*this.getFittedResidual(sector, it, ip)[il];
                        errorRes[it-1] = Math.sqrt(Math.pow(histos.get("nominal").getParErrors(sector, it, ip)[il], 2)
                                       + scale*0*Math.pow(this.getFittedResidualError(sector, it, ip)[il], 2));
                        angles[it-1]   = thetaBins[it].getMean()+thetaBins[it].getWidth()*(il-Constants.NLAYER/2)/Constants.NLAYER/2;
                    }
                    GraphErrors gr_fit = new GraphErrors("gr_fit_S" + sector + "_layer " + il + "_phi" + ip, 
                                                         shiftRes, angles, errorRes, zeros);
                    gr_fit.setTitle("Sector " + sector);
                    gr_fit.setTitleX("Residual (um)");
                    gr_fit.setTitleY("Layer");
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

    private void setAngularBins(String thetabins, String phibins) {
        thetaBins = this.getBins(thetabins);
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
        panel.add("results", this.plotResults());
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
    
    public void processFiles() {
        for(String key : histos.keySet()) {
            histos.get(key).processFiles();
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
            this.addHistoSet(key, new Histo(null, thetaBins, phiBins, optStats));
            histos.get(key).readDataGroup(folder+"/"+key, dir);
        }
        System.setOut(outStream);
        System.setErr(errStream);
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
            

        OptionStore parser = new OptionStore("DC Alignment");
        
        // valid options for event-base analysis
        parser.addCommand("-process", "process event files");
        parser.getOptionParser("-process").addOption("-o"        ,"",              "output file name prefix");
        parser.getOptionParser("-process").addOption("-nevent"   ,"-1",            "maximum number of events to process");
        parser.getOptionParser("-process").addRequired("-" + inputs[0],            "nominal geometry files");
        for(int i=1; i<inputs.length; i++) parser.getOptionParser("-process").addOption("-" + inputs[i],"");
        parser.getOptionParser("-process").addOption("-display"  ,"1",             "display histograms (0/1)");
        parser.getOptionParser("-process").addOption("-stats"    ,"",              "set histogram stat option");
        parser.getOptionParser("-process").addOption("-theta"    , "",             "theta bin limits, e.g. \"5:10:20:30\"");
        parser.getOptionParser("-process").addOption("-phi"      , "",             "phi bin limits, e.g. \"-30:-15:0:15:30\"");
        parser.getOptionParser("-process").addOption("-variation", "",             "database variation for constant test");
        parser.getOptionParser("-process").addOption("-fit"      , "1",            "fit residuals (1) or use mean (0)");
        parser.getOptionParser("-process").addOption("-sector"   , "0",            "sector-dependent derivatives (1) or average (0)");
        parser.getOptionParser("-process").addOption("-init"     , "0",            "init global fit from previous constants (1) or from zero shifts (0)");
        parser.getOptionParser("-process").addOption("-iter"     , "1",            "number of global fit iterations");
        parser.getOptionParser("-process").addOption("-verbose"  , "0",            "global fit verbosity (1/0 = on/off)");
        
        // valid options for histogram-base analysis
        parser.addCommand("-analyze", "analyze histogram files");
        parser.getOptionParser("-analyze").addRequired("-input"  ,                 "input histogram file");
        parser.getOptionParser("-analyze").addOption("-display"  ,"1",             "display histograms (0/1)");
        parser.getOptionParser("-analyze").addOption("-stats"    ,"",              "set histogram stat option");
        parser.getOptionParser("-analyze").addOption("-variation", "",             "database variation for constant test");
        parser.getOptionParser("-analyze").addOption("-fit"      , "1",            "fit residuals (1) or use mean (0)");
        parser.getOptionParser("-analyze").addOption("-sector"   , "0",            "sector-dependent derivatives (1) or average (0)");
        parser.getOptionParser("-analyze").addOption("-init"     , "0",            "init global fit from previous constants (1) or from zero shifts (0)");
        parser.getOptionParser("-analyze").addOption("-iter"     , "1",            "number of global fit iterations");
        parser.getOptionParser("-analyze").addOption("-verbose"  , "0",            "global fit verbosity (1/0 = on/off)");
        
        parser.parse(args);
        
        boolean openWindow = false;
        
        
        if(parser.getCommand().equals("-process")) {
            int    maxEvents   = parser.getOptionParser("-process").getOption("-nevent").intValue();
            String namePrefix  = parser.getOptionParser("-process").getOption("-o").stringValue();  
            String histoName   = "histo.hipo";
            if(!namePrefix.isEmpty()) {
                histoName  = namePrefix + "_" + histoName;
            }
            String thetaBins   = parser.getOptionParser("-process").getOption("-theta").stringValue();
            String phiBins     = parser.getOptionParser("-process").getOption("-phi").stringValue();
            String optStats    = parser.getOptionParser("-process").getOption("-stats").stringValue();
            String variation   = parser.getOptionParser("-process").getOption("-variation").stringValue();
            boolean fit        = parser.getOptionParser("-process").getOption("-fit").intValue()!=0;
            boolean sector     = parser.getOptionParser("-process").getOption("-sector").intValue()!=0;
            boolean init       = parser.getOptionParser("-process").getOption("-init").intValue()!=0;
            int     iter       = parser.getOptionParser("-process").getOption("-iter").intValue();
            boolean verbose    = parser.getOptionParser("-process").getOption("-verbose").intValue()!=0;
            openWindow         = parser.getOptionParser("-process").getOption("-display").intValue()!=0;
            if(!openWindow) System.setProperty("java.awt.headless", "true");

            align.setAngularBins(thetaBins, phiBins);
            align.setFitOptions(fit, sector, init, iter, verbose);
            align.initConstants(11, variation);
            for(int i=0; i<inputs.length; i++) {
                String input = parser.getOptionParser("-process").getOption("-" + inputs[i]).stringValue();
                if(!input.isEmpty()) {                    
                    align.addHistoSet(inputs[i], new Histo(Alignment.getFileNames(input),align.getThetaBins(),align.getPhiBins(),optStats));
                }
            }
            align.processFiles();
            align.analyzeHistos();
            align.saveHistos(histoName);
        }
        
        if(parser.getCommand().equals("-analyze")) {
            String optStats    = parser.getOptionParser("-analyze").getOption("-stats").stringValue();
            String variation   = parser.getOptionParser("-analyze").getOption("-variation").stringValue();
            boolean fit        = parser.getOptionParser("-analyze").getOption("-fit").intValue()!=0;
            boolean sector     = parser.getOptionParser("-analyze").getOption("-sector").intValue()!=0;
            boolean init       = parser.getOptionParser("-analyze").getOption("-init").intValue()!=0;
            int     iter       = parser.getOptionParser("-analyze").getOption("-iter").intValue();
            boolean verbose    = parser.getOptionParser("-analyze").getOption("-verbose").intValue()!=0;
            openWindow         = parser.getOptionParser("-analyze").getOption("-display").intValue()!=0;
            if(!openWindow) System.setProperty("java.awt.headless", "true");

            String histoName   = parser.getOptionParser("-analyze").getOption("-input").stringValue();
            align.setFitOptions(fit, sector, init, iter, verbose);
            align.initConstants(11, variation);
            align.readHistos(histoName, optStats);
            align.analyzeHistos();
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
