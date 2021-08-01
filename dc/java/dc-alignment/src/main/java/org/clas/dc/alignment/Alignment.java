package org.clas.dc.alignment;


import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import org.jlab.groot.data.GraphErrors;
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

    private int               nSector = 6;
    private int               nLayer  = 36;
    private Map<String,Histo> histos = new LinkedHashMap();
    private String[]          inputs = new String[19];
    private Bin[]             thetaBins = null;
    private Bin[]             phiBins  = null;
    
    public Alignment() {
        this.initsInputs();
    }
    
    private void initsInputs() {
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
            System.out.println("Analyzing histos for variation " + key);
            histos.get(key).analyzeHisto();
        }
    }
    
    public EmbeddedCanvasTabbed analyzeFits() {
        EmbeddedCanvasTabbed canvas = new EmbeddedCanvasTabbed("Nominal Residuals");
        double[] layers = new double[nLayer];
        double[] zeros  = new double[nLayer];
        for(int il=0; il<nLayer; il++) layers[il]=il+1; 
        //
        System.out.println("Analyzing nominal geometry residuals");
        DataGroup nomResiduals = new DataGroup(3,2);
        Histo nomHisto = histos.get("nominal");
        for(int it=0; it<thetaBins.length; it ++) {
            for(int ip=0; ip<phiBins.length; ip++) {
                if(it+ip==1) continue;
                for(int is=0; is<nSector; is++ ) {
                    int sector = is+1;
                    GraphErrors gr_res = new GraphErrors("gr_res_S" + sector + "_theta " + it + "_phi" + ip, 
                                                         nomHisto.getResidualValues(sector,it,ip), 
                                                         layers, 
                                                         nomHisto.getResidualErrors(sector,it,ip), 
                                                         zeros);
                    gr_res.setTitle("Sector " + sector);
                    gr_res.setTitleX("Residual (um)");
                    gr_res.setTitleY("Layer");
                    gr_res.setMarkerColor((ip-1)*thetaBins.length+it+1);
                    nomResiduals.addDataSet(gr_res, is);
                }               
            }
        }
        canvas.getCanvas("Nominal Residuals").draw(nomResiduals);
        for(EmbeddedPad pad : canvas.getCanvas("Nominal Residuals").getCanvasPads())
            pad.getAxisX().setRange(-2000, 2000);
        // shifts
        System.out.println("Analyzing shifted geometry residuals");
        for(String key : histos.keySet()) {
            if(!key.equals("nominal")) {
                Histo shiftedHisto = histos.get(key);
                DataGroup shiftedResiduals = new DataGroup(thetaBins.length,phiBins.length);
                DataGroup shiftedVertex    = new DataGroup(thetaBins.length,phiBins.length);
                for(int it=0; it<thetaBins.length; it ++) {
                    for(int ip=0; ip<phiBins.length; ip++) {
                        double[] shiftVtx = new double[nSector];
                        double[] errorVtx = new double[nSector];
                        for(int is=0; is<nSector; is++ ) {
                            int sector = is+1;
                            // residuals 
                            double[] shiftRes = new double[nLayer];
                            double[] errorRes = new double[nLayer];
                            for(int il=0; il<nLayer; il++) {
                                shiftRes[il] = shiftedHisto.getResidualValues(sector, it, ip)[il]-
                                               nomHisto.getResidualValues(sector, it, ip)[il];
                                errorRes[il] = Math.sqrt(Math.pow(shiftedHisto.getResidualErrors(sector, it, ip)[il],2)+
                                                         Math.pow(nomHisto.getResidualErrors(sector, it, ip)[il],2));
                            }
                            GraphErrors gr_res = new GraphErrors("gr_res_S" + sector  + "_theta " + it + "_phi" + ip, shiftRes, layers, errorRes, zeros);
                            gr_res.setTitle("Theta:"+ thetaBins[it].getRange() + " Phi:" + phiBins[ip].getRange());
                            gr_res.setTitleX("Residual (um)");
                            gr_res.setTitleY("Layer");
                            gr_res.setMarkerColor(sector);
                            shiftedResiduals.addDataSet(gr_res, ip*thetaBins.length+it);
                            // vertex
                            shiftVtx[is] = shiftedHisto.getVertexValues(sector, it, ip)-
                                           nomHisto.getVertexValues(sector, it, ip);
                            errorVtx[is] = Math.sqrt(Math.pow(shiftedHisto.getVertexErrors(sector, it, ip),2)+
                                                     Math.pow(nomHisto.getVertexErrors(sector, it, ip),2));
                        }
                        GraphErrors gr_vtx = new GraphErrors("gr_vtx_theta " + it + "_phi" + ip, shiftVtx, layers, errorVtx, zeros);
                        gr_vtx.setTitle("Theta:"+ thetaBins[it].getRange() + " Phi:" + phiBins[ip].getRange());
                        gr_vtx.setTitleX("Vertex (cm)");
                        gr_vtx.setTitleY("Sector");
                        gr_vtx.setMarkerColor(2);
                        shiftedVertex.addDataSet(gr_vtx, ip*thetaBins.length+it);
                    }
                }
                canvas.addCanvas(key + " Residuals");
                canvas.getCanvas(key + " Residuals").draw(shiftedResiduals);
                for(EmbeddedPad pad : canvas.getCanvas(key + " Residuals").getCanvasPads())
                    pad.getAxisX().setRange(-2000, 2000);
                canvas.addCanvas(key + " Vertex");
                canvas.getCanvas(key + " Vertex").draw(shiftedVertex);
            }
        }
        this.printResults();
        return canvas;
    }
    
    public void printResults() {
        for(String key: histos.keySet()) {
            if(key.equals("nominal")) {
                for(int is=0; is<nSector; is++) {
                    int sector = is+1;
                    System.out.print("double sector_" + sector + "_array[" + ((thetaBins.length-1)*(phiBins.length-1)) + "][37] = {");
                    for(int it=1; it<thetaBins.length; it ++) {
                        for(int ip=1; ip<phiBins.length; ip++) {
                            System.out.print("{");
                            for(int il=0; il<nLayer; il++) {
                                System.out.print(String.format("%.1f, ", histos.get(key).getResidualValues(sector, it, ip)[il]));
                            }
                            System.out.print(String.format("%.1f}", histos.get(key).getVertexValues(sector, it, ip)*1E4));
                            if(!(it==thetaBins.length-1 && ip==phiBins.length-1)) System.out.println(",");
                        }
                    }
                    System.out.println("};\n");
                }
            }
            else {
                System.out.print("double " + key + "_list[" + ((thetaBins.length-1)*(phiBins.length-1)) + "][37] = {");
                for(int it=1; it<thetaBins.length; it ++) {
                    for(int ip=1; ip<phiBins.length; ip++) {
                        System.out.print("{");
                        for(int il=0; il<nLayer; il++) {
                            double shift=0;
                            for(int is=0; is<nSector; is++) {
                                int sector = is+1;
                                shift += (histos.get(key).getResidualValues(sector, it, ip)[il]-histos.get("nominal").getResidualValues(sector, it, ip)[il])/nSector;
                            }
                            System.out.print(String.format("%.1f, ", shift));
                        }
                        double shift=0;
                        for(int is=0; is<nSector; is++) {
                            int sector = is+1;
                            shift += (histos.get(key).getVertexValues(sector, it, ip)-histos.get("nominal").getVertexValues(sector, it, ip))*1E4/nSector;
                        }
                        System.out.print(String.format("%.1f}", shift));
                        if(!(it==thetaBins.length-1 && ip==phiBins.length-1)) System.out.println(",");
                    }
                }
                System.out.println("};\n");
            }
        }
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
        JTabbedPane panel = new JTabbedPane();
        for(String key : histos.keySet()) {
            panel.add(key, histos.get(key).plotHistos());
        }
        panel.add("Analysis", this.analyzeFits());
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
    
    public void readHistos(String fileName, Bin[] thetaBins, Bin[] phiBins, String optStats) {
        System.out.println("Opening file: " + fileName);
        TDirectory dir = new TDirectory();
        dir.readFile(fileName);
        System.out.println(dir.getDirectoryList());
        dir.cd();
        for(String key : dir.getDirectoryList()) {
            this.addHistoSet(key, new Histo(null, thetaBins, phiBins, optStats));
            histos.get(key).readDataGroup(key, dir);
        }
    }

    public void saveHistos(String fileName) {
        TDirectory dir = new TDirectory();
        for(String key : histos.keySet()) {
            histos.get(key).writeDataGroup(key, dir);
        }
        System.out.println("Saving histograms to file " + fileName);
        dir.writeFile(fileName);
    }    

    public static void main(String[] args){
        
        Alignment align = new Alignment();
        String[] inputs = align.getInputs();
            

        OptionStore parser = new OptionStore("DC Alignment");
        
        // valid options for event-base analysis
        parser.addCommand("-process", "process event files");
        parser.getOptionParser("-process").addOption("-o"       ,"",   "output file name prefix");
        parser.getOptionParser("-process").addOption("-nevent"  ,"-1", "maximum number of events to process");
        for(int i=0; i<inputs.length; i++) parser.getOptionParser("-process").addOption("-" + inputs[i],"");
        parser.getOptionParser("-process").addOption("-display" ,"1",  "display histograms (0/1)");
        parser.getOptionParser("-process").addOption("-stats"   ,"",   "set histogram stat option");
        parser.getOptionParser("-process").addOption("-theta"   , "",  "theta bin limits, e.g. \"5:10:20:30\"");
        parser.getOptionParser("-process").addOption("-phi"     , "",  "phi bin limits, e.g. \"-30:-15:0:15:30\"");
        
        // valid options for histogram-base analysis
        parser.addCommand("-analyze", "analyze histogram files");
        parser.getOptionParser("-analyze").addRequired("-input" ,      "input histogram file");
        parser.getOptionParser("-analyze").addOption("-display" ,"1",  "display histograms (0/1)");
        parser.getOptionParser("-analyze").addOption("-stats"   ,"",   "set histogram stat option");
        parser.getOptionParser("-analyze").addOption("-theta"   , "",  "theta bin limits, e.g. \"5:10:20:30\"");
        parser.getOptionParser("-analyze").addOption("-phi"     , "",  "phi bin limits, e.g. \"-30:-15:0:15:30\"");
        
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
            openWindow         = parser.getOptionParser("-process").getOption("-display").intValue()!=0;
            
            align.setAngularBins(thetaBins, phiBins);
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
            String thetaBins   = parser.getOptionParser("-analyze").getOption("-theta").stringValue();
            String phiBins     = parser.getOptionParser("-analyze").getOption("-phi").stringValue();
            String optStats    = parser.getOptionParser("-analyze").getOption("-stats").stringValue();
            openWindow         = parser.getOptionParser("-analyze").getOption("-display").intValue()!=0;
            
            String histoName   = parser.getOptionParser("-analyze").getOption("-input").stringValue();
            align.setAngularBins(thetaBins, phiBins);
            align.readHistos(histoName,align.getThetaBins(),align.getPhiBins(),optStats);
            align.analyzeHistos();
        }

        if(!openWindow) System.setProperty("java.awt.headless", "true");
        else {
            JFrame frame = new JFrame("DC Alignment");
            frame.setSize(1200, 800);
            frame.add(align.getCanvases());
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }     
    }
}
