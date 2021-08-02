package org.clas.dc.alignment;

import java.util.List;
import org.jlab.clas.physics.Particle;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.IDataSet;
import org.jlab.groot.data.TDirectory;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.graphics.EmbeddedCanvasTabbed;
import org.jlab.groot.group.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.jnp.hipo4.io.HipoReader;
import org.jlab.utils.benchmark.ProgressPrintout;

/**
 *
 * @author devita
 */
public class Histo {
    
    private final int nSector = 6;
    private final int nLayer  = 36;
    
    private DataGroup[][][] residuals = null; // indices are theta bin, phi bin and sector, datagroup is 6x6 and contains layers
    private DataGroup[][]   vertex    = null; // indices are theta bin, phi bin and sector, datagroup is 6x6 and contains sectors

    private double[][][][] resValues = null;
    private double[][][][] resErrors = null;
    private double[][][]   vtxValues = null;
    private double[][][]   vtxErrors = null;
    
    private Bin[] thetaBins = null;
    private Bin[] phiBins  = null;
    
    private static double tlength = 5;   //target length
    private static double wdist   = 2.8; //distance between the mylar foil and the downstream window
    
    // histogram limits for residuals
    int    resBins = 200;
    double resMin  = -5000;
    double resMax  =  5000;
    // histogram limits for vertex plots
    int    vtxBins = 100;
    double vtxMin = -12.0;
    double vtxMax = 10.0;
    
    private List<String>  inputFiles = null;
    private int           maxEvents  = -1;
    private SchemaFactory schema     = null;
    
    
    public Histo(List<String> inputfiles, Bin[] thetabins, Bin[] phibins, String optstats) {
        this.thetaBins = thetabins;
        this.phiBins   = phibins;
        this.createHistos(optstats);
        this.inputFiles = inputfiles;
    }
    
    private void createHistos(String optStats) {
        System.out.println("Creating histograms for " + nSector          + " sector, " 
                                                      + thetaBins.length + " theta bins, " 
                                                      + phiBins.length   + " phi bins");
        this.residuals = new DataGroup[nSector][thetaBins.length][phiBins.length];
        this.vertex    = new DataGroup[thetaBins.length][phiBins.length];
        this.resValues = new double[nSector][thetaBins.length][phiBins.length][nLayer];
        this.resErrors = new double[nSector][thetaBins.length][phiBins.length][nLayer];
        this.vtxValues = new double[nSector][thetaBins.length][phiBins.length];
        this.vtxErrors = new double[nSector][thetaBins.length][phiBins.length];
        
        for(int is=0; is<nSector; is++) {
            int sector = is+1;
            for(int it=0; it<thetaBins.length; it++) {
                for(int ip=0; ip<phiBins.length; ip++) {
                    this.residuals[is][it][ip] = new DataGroup(6,6);
                    for(int il=0; il<nLayer; il++) {
                        int layer = il+1;
                        H1F hi_residual = new H1F("hi_L" + layer,"Layer " + layer + " Sector " + sector, resBins, resMin, resMax);
                        hi_residual.setTitleX("Residuals (um)");
                        hi_residual.setTitleY("Counts");
                        hi_residual.setOptStat(optStats);
                        this.residuals[is][it][ip].addDataSet(hi_residual, il);
                    }
                    
                } 
            }
        }
        for(int it=0; it<thetaBins.length; it++) {
            for(int ip=0; ip<phiBins.length; ip++) {
                this.vertex[it][ip] = new DataGroup(3,2);
                for(int is=0; is<nSector; is++) {
                    int sector = is+1;
                    H1F hi_vertex = new H1F("hi_S" + sector,"Sector " + sector, vtxBins, vtxMin, vtxMax);
                    hi_vertex.setTitleX("Vertex (cm)");
                    hi_vertex.setTitleY("Counts");
                    hi_vertex.setOptStat(optStats);
                    this.vertex[it][ip].addDataSet(hi_vertex, is);
                }
            }
        }           
    }
  
    private int getElectron(Event event) {
       
        int iele = -1;
        
        Bank particleBank    = new Bank(schema.getSchema("REC::Particle"));
        Bank calorimeterBank = new Bank(schema.getSchema("REC::Calorimeter"));
        Bank cherenkovBank   = new Bank(schema.getSchema("REC::Cherenkov"));
        Bank trackBank       = new Bank(schema.getSchema("REC::Track"));
         
        if(particleBank!=null)    event.read(particleBank);
        if(calorimeterBank!=null) event.read(calorimeterBank);
        if(cherenkovBank!=null)   event.read(cherenkovBank);
        if(trackBank!=null)       event.read(trackBank);
        
        if(particleBank!= null    && particleBank.getRows()>0 && 
            calorimeterBank!= null && calorimeterBank.getRows()>0 && 
            cherenkovBank!= null   && cherenkovBank.getRows()>0 && 
            trackBank!= null       && trackBank.getRows()>0) {

            for(int loop=0; loop<particleBank.getRows(); loop++) {
                double beta = particleBank.getFloat("beta",loop);

                double nphe = 0;
                for(int i=0; i<cherenkovBank.getRows(); i++) {
                    if(cherenkovBank.getShort("pindex", i)==loop) {
                        nphe = cherenkovBank.getFloat("nphe", i);
                    }
                }

                double energy = 0;
                for (int i=0; i<calorimeterBank.getRows(); i++) {
                    if(calorimeterBank.getShort("pindex", i)==loop) {
                        energy+=calorimeterBank.getFloat("energy", i);
                    }
                }

                if(beta>0 && nphe>2 && energy>0.2) { // require at least 0.06 energy deposited in the calorimeter
                    for (int i=0; i<trackBank.getRows(); i++) {
                        if(trackBank.getShort("pindex", i)==loop) {
                            iele = trackBank.getShort("index", i);
                        }
                    }
                }
                if(iele>=0) break;
            }
        } 
                
        return iele;
    }
    private void processEvent(Event event) {

        Bank runConfig = new Bank(schema.getSchema("RUN::config"));
        Bank trackBank = new Bank(schema.getSchema("TimeBasedTrkg::TBTracks"));
        Bank hitBank  = new Bank(schema.getSchema("TimeBasedTrkg::TBHits"));
					
        if(runConfig!=null)       event.read(runConfig);
        if(trackBank!=null)       event.read(trackBank);
        if(hitBank!=null)        event.read(hitBank);
        
        if(runConfig!=null &&
           trackBank!=null && trackBank.getRows()>0 &&
           hitBank!=null  && hitBank.getRows()>0) {
            
            int iele = this.getElectron(event);
            if(iele<0) return;
            
            Particle electron = new Particle(11,
                                             trackBank.getFloat("p0_x", iele),
                                             trackBank.getFloat("p0_y", iele),
                                             trackBank.getFloat("p0_z", iele),
                                             trackBank.getFloat("Vtx0_x", iele),
                                             trackBank.getFloat("Vtx0_y", iele),
                                             trackBank.getFloat("Vtx0_z", iele));
            
            int id     = trackBank.getInt("id", iele);
            int sector = trackBank.getByte("sector", iele);
//            double pphi = Math.toDegrees(electron.phi());
            electron.vector().rotateZ(Math.toRadians(-60*(sector-1)));
            
            double theta = Math.toDegrees(electron.theta());
            double phi   = Math.toDegrees(electron.phi());
//            if(Math.abs(phi)>30) {
//                System.out.println(phi + " " + sector + " " + pphi);
//                System.out.println(electron.toString());
//            }
            for(int i=0; i<hitBank.getRows(); i++){
                
                if(hitBank.getInt("trkID", i) == id) {
                    double residual = 10000*hitBank.getFloat("fitResidual", i);
                    int superlayer  = hitBank.getInt("superlayer", i);
                    int layer       = hitBank.getInt("layer", i)+6*(superlayer-1);
                    
                    for(int it=0; it<thetaBins.length; it++) {
                        if(thetaBins[it].contains(theta)) {
                            for(int ip=0; ip<phiBins.length; ip++) {
                                if(phiBins[ip].contains(phi)) {                             
                                    this.residuals[sector-1][it][ip].getH1F("hi_L" + layer).fill(residual);
                                    this.vertex[it][ip].getH1F("hi_S" + sector).fill(electron.vz());
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    public void processFiles() {
        ProgressPrintout progress = new ProgressPrintout();

        int counter=-1;
        Event event = new Event();

        for(String inputFile : this.inputFiles){
            HipoReader reader = new HipoReader();
            reader.open(inputFile);
            schema = reader.getSchemaFactory();
            
            while (reader.hasNext()) {

                counter++;

                reader.nextEvent(event);
                this.processEvent(event);
                
                progress.updateStatus();
                if(maxEvents>0){
                    if(counter>=maxEvents) break;
                }
            }
            progress.showStatus();
            reader.close();
        } 
    }
    
 
    public void analyzeHisto() {
        for(int is=0; is<nSector; is++) {
            int s = is +1;
            for(int it=0; it<thetaBins.length; it++) {
                for(int ip=0; ip<phiBins.length; ip++) {
                    for(int l=1; l<=nLayer; l++) {
                        H1F hres = residuals[is][it][ip].getH1F("hi_L"+l);
                        System.out.print(String.format("sector=%1d theta bin=%1d phi bin=%1d layer=%2d...",s,it,ip,l));
                        Histo.fitResiduals(hres);
                        if(hres.getFunction()!=null) {
                            this.resValues[is][it][ip][l-1] = hres.getFunction().getParameter(1); 
                            this.resErrors[is][it][ip][l-1] = hres.getFunction().parameter(1).error();
                        }
                        else {
                            this.resValues[is][it][ip][l-1] = 0; 
                            this.resErrors[is][it][ip][l-1] = 0;
                        }
                    }
                    H1F hvtx = vertex[it][ip].getH1F("hi_S"+s);
                    Histo.fit3Vertex(hvtx);
                    this.vtxValues[is][it][ip] = hvtx.getFunction().getParameter(1);
                    this.vtxErrors[is][it][ip] = hvtx.getFunction().parameter(1).error();
                }
            }
        }
         
    }
    
    public double[] getResidualValues(int sector, int itheta, int iphi) {
        if(sector<1 || sector>6) 
            throw new IllegalArgumentException("Error: invalid sector="+sector);
        if(itheta<0 || itheta>=thetaBins.length) 
            throw new IllegalArgumentException("Error: invalid theta bin="+itheta);
        if(iphi<0 || iphi>phiBins.length) 
            throw new IllegalArgumentException("Error: invalid phi bin="+iphi);
        return this.resValues[sector-1][itheta][iphi];
    }
    
    public double[] getResidualErrors(int sector, int itheta, int iphi) {
        if(sector<1 || sector>6) 
            throw new IllegalArgumentException("Error: invalid sector="+sector);
        if(itheta<0 || itheta>=thetaBins.length) 
            throw new IllegalArgumentException("Error: invalid theta bin="+itheta);
        if(iphi<0 || iphi>phiBins.length) 
            throw new IllegalArgumentException("Error: invalid phi bin="+iphi);
        return this.resErrors[sector-1][itheta][iphi];
    }
       
    public double getVertexValues(int sector, int itheta, int iphi) {
        if(sector<1 || sector>6) 
            throw new IllegalArgumentException("Error: invalid sector="+sector);
        if(itheta<0 || itheta>=thetaBins.length) 
            throw new IllegalArgumentException("Error: invalid theta bin="+itheta);
        if(iphi<0 || iphi>phiBins.length) 
            throw new IllegalArgumentException("Error: invalid phi bin="+iphi);
        return this.vtxValues[sector-1][itheta][iphi];
    }
    
    public double getVertexErrors(int sector, int itheta, int iphi) {
        if(sector<1 || sector>6) 
            throw new IllegalArgumentException("Error: invalid sector="+sector);
        if(itheta<0 || itheta>=thetaBins.length) 
            throw new IllegalArgumentException("Error: invalid theta bin="+itheta);
        if(iphi<0 || iphi>phiBins.length) 
            throw new IllegalArgumentException("Error: invalid phi bin="+iphi);
        return this.vtxErrors[sector-1][itheta][iphi];
    }
    
    public EmbeddedCanvasTabbed plotHistos() {
        EmbeddedCanvasTabbed canvas = null;
        for(int is=0; is<nSector; is++) {
            int    sector = is+1;
            String title  = "Sec" + sector;
            if(canvas==null) canvas = new EmbeddedCanvasTabbed(title);
            else             canvas.addCanvas(title);
            canvas.getCanvas(title).draw(this.residuals[is][0][0]);
            for(int it=0; it<thetaBins.length; it++) {
                for(int ip=0; ip<phiBins.length; ip++) {
                    if(it==0 && ip==0) continue;
                    title = "Sec" + sector;
                    if(it!=0) title = title + " Theta:" + thetaBins[it].getRange();
                    if(ip!=0) title = title + " Phi:" + phiBins[ip].getRange();
                    canvas.addCanvas(title);
                    canvas.getCanvas(title).draw(residuals[is][it][ip]);
                }
            }
        }
        for(int it=0; it<thetaBins.length; it++) {
            for(int ip=0; ip<phiBins.length; ip++) {
//                if((it==0 && ip!=0) || (it!=0 && ip==0)) continue;
                String title = "Theta:" + thetaBins[it].getRange() + " Phi:" + phiBins[ip].getRange();
                canvas.addCanvas(title);
                canvas.getCanvas(title).draw(vertex[it][ip]);
            }
        }
        return canvas;
    }
    
    
    public static boolean fitResiduals(H1F histo) {
        double mean  = histo.getDataX(histo.getMaximumBin());
        double amp   = histo.getBinContent(histo.getMaximumBin());
        double rms   = histo.getRMS();
        double sigma = rms/2;
        double min = mean - rms;
        double max = mean + rms;
        
        if(amp>10) {
            F1D f1   = new F1D("f1res","[amp]*gaus(x,[mean],[sigma])", min, max);
            f1.setLineColor(2);
            f1.setLineWidth(2);
            f1.setOptStat("1111");
            f1.setParameter(0, amp);
            f1.setParameter(1, mean);
            f1.setParameter(2, sigma);
            f1.setParLimits(0, amp*0.2,   amp*1.2);
            f1.setParLimits(1, mean*0.8,  mean*1.2);
            f1.setParLimits(2, sigma*0.2, sigma*1.2);
            System.out.print("1st...");
            DataFitter.fit(f1, histo, "Q");
            mean  = f1.getParameter(1);
            sigma = f1.getParameter(2);
            f1.setParLimits(0, 0, 2*amp);
            f1.setParLimits(1, mean-sigma, mean+sigma);
            f1.setParLimits(2, 0, sigma*2);
            f1.setRange(mean-2.0*sigma,mean+2.0*sigma);
            DataFitter.fit(f1, histo, "Q");
            System.out.print("2nd\r");
            return true;
        }
        else {
            System.out.print("\r");
            return false;
        }
    }    

    
    public static void fit3Vertex(H1F histo) {
        int nbin = histo.getData().length;
        double dx = histo.getDataX(1)-histo.getDataX(0);
        //find downstream window
        int ibin0 = histo.getMaximumBin();
        //check if the found maximum is the first or second peak, ibin is tentative upstream window
        int ibin1 = Math.max(0, histo.getMaximumBin() - (int)(tlength/dx));
        int ibin2 = Math.min(nbin-1, histo.getMaximumBin() + (int)(tlength/dx));
        if(histo.getBinContent(ibin1)<histo.getBinContent(ibin2)) {
            ibin1 = ibin0;
            ibin0 = ibin2;
        }
        
        double mean  = histo.getDataX(ibin0);
        double amp   = histo.getBinContent(ibin0);
        double sigma = 0.5;
        double bg = histo.getBinContent((ibin1+ibin0)/2);
        
        F1D f1_vtx   = new F1D("f3vertex","[amp]*gaus(x,[mean]-[tl],[sigma])+[amp]*gaus(x,[mean],[sigma])+[amp]*gaus(x,[mean]+[wd],[sigma])/1.8+[bg]*gaus(x,[mean]-[tl]/2,[tl]*0.8)", -10, 10);
        f1_vtx.setLineColor(2);
        f1_vtx.setLineWidth(2);
        f1_vtx.setOptStat("11111111");
        f1_vtx.setParameter(0, amp/2);
        f1_vtx.setParameter(1, mean);
        f1_vtx.setParameter(2, tlength);
        f1_vtx.setParLimits(2, tlength*0.99, tlength*1.01);
        f1_vtx.setParameter(3, sigma);
        f1_vtx.setParameter(4, wdist);
        f1_vtx.setParLimits(4, wdist*0.9, wdist*1.1);
        f1_vtx.setParameter(5, bg);
//        f1_vtx.setParameter(4, sigma*3);
        f1_vtx.setRange(mean-tlength*1.5,mean+tlength);
        DataFitter.fit(f1_vtx, histo, "Q"); //No options uses error for sigma
    }
    
    public static void fitVertex(H1F histo) {
        double mean  = histo.getDataX(histo.getMaximumBin());
        double amp   = histo.getBinContent(histo.getMaximumBin());
        double sigma = 1.0;
        
        //Define peak to right of max peak
        //Look to the right (higher x values) of the previusly found peak to find the next peak)
        double binMaxAmp2 = 0;
        double maxBinXVal2 = 0;
        double maxBinNum2;
        for(int k = 0; k < histo.getAxis().getNBins() - 1; k++) {
            double binXVal2 = histo.getAxis().getBinCenter(k);
            if(binXVal2 > mean + 1.5 * sigma) {
                double tempBinContent2  = histo.getBinContent(k);
                if(tempBinContent2 > binMaxAmp2) {
                    binMaxAmp2 = tempBinContent2;
                    maxBinNum2 = k;
                    maxBinXVal2 = binXVal2;
                }
            }
        }
        
        //Define peak to left of max peak
        //Look to the left (lower x values) of the first found peak (max peak) to determine if there is another peak to left)
        double binMaxAmp3 = 0;
        double maxBinXVal3;
        double maxBinNum3;
        for(int k = 0; k < histo.getAxis().getNBins() - 1; k++) {
            double binXVal3 = histo.getAxis().getBinCenter(k);
            if(binXVal3 < mean - 1.5 * sigma) {
                double tempBinContent3  = histo.getBinContent(k);
                if(tempBinContent3 > binMaxAmp3) {
                    binMaxAmp3 = tempBinContent3;
                    maxBinNum3 = k;
                    maxBinXVal3 = binXVal3;
                }
            }
        }
       
        //The if statement checks if there is a peak (at least 2/3 of the plot's max peak) to the left of the max peak
        //If so, it fits the max peak since the downstream peak is desired
        if(binMaxAmp3 >= 0.67 * amp) {
            F1D f1_vtx   = new F1D("f1vertex","[amp]*gaus(x,[mean],[sigma])", -10, 10);
            f1_vtx.setLineColor(2);
            f1_vtx.setLineWidth(2);
            f1_vtx.setOptStat("1111");
            f1_vtx.setParameter(0, amp);
            f1_vtx.setParameter(1, mean);
            f1_vtx.setParameter(2, sigma);
            f1_vtx.setRange(mean-2.0*sigma,mean+2.0*sigma);
            DataFitter.fit(f1_vtx, histo, "Q"); //No options uses error for sigma
        }
        //The else statement runs when there is no significant peak to the left of max peak
        //So max peak is the leftmost peak (upstream peak)
        //Since the downstream peak is desired, the peak to the right of the max peak is fit
        else {
            F1D f2_vtx   = new F1D("f2vertex","[amp2]*gaus(x,[mean2],[sigma2])", -10, 10);
            f2_vtx.setLineColor(2);
            f2_vtx.setLineWidth(2);
            f2_vtx.setOptStat("1111");
            double mean2  = maxBinXVal2;
            double amp2   = binMaxAmp2;
            double sigma2 = 1.0;
            f2_vtx.setParameter(0, amp2);
            f2_vtx.setParameter(1, mean2);
            f2_vtx.setParameter(2, sigma2);
            f2_vtx.setRange(mean2-2.0*sigma2,mean2+2.0*sigma2);
            DataFitter.fit(f2_vtx, histo, "Q"); //No options uses error for sigma
        }
    }

    public void readDataGroup(String folder, TDirectory dir) {
        for(int is=0; is<nSector; is++) {
            for(int it=0; it<thetaBins.length; it++) {
                for(int ip=0; ip<phiBins.length; ip++) {
                    String subfolder = folder + "/residuals/sec" + (is+1) + "_theta" + thetaBins[it].getRange() + "_phi" + phiBins[ip].getRange();
                    int nrows = residuals[is][it][ip].getRows();
                    int ncols = residuals[is][it][ip].getColumns();
                    int nds   = nrows*ncols;
                    DataGroup newGroup = new DataGroup(ncols,nrows);
                    for(int i = 0; i < nds; i++){
                        List<IDataSet> dsList = residuals[is][it][ip].getData(i);
                        for(IDataSet ds : dsList){
                            if(dir.getObject(subfolder, ds.getName())!=null) {
                                newGroup.addDataSet(dir.getObject(subfolder, ds.getName()),i);
                            }
                        }
                    }
                    residuals[is][it][ip]=newGroup;
                }
            }
        }
        for(int it=0; it<thetaBins.length; it++) {
            for(int ip=0; ip<phiBins.length; ip++) {
                String subfolder = folder + "/vertex/theta" + thetaBins[it].getRange() + "_phi" + phiBins[ip].getRange();
                int nrows = vertex[it][ip].getRows();
                int ncols = vertex[it][ip].getColumns();
                int nds   = nrows*ncols;
                DataGroup newGroup = new DataGroup(ncols,nrows);
                for(int i = 0; i < nds; i++){
                    List<IDataSet> dsList = vertex[it][ip].getData(i);
                    for(IDataSet ds : dsList){
                        if(dir.getObject(subfolder, ds.getName())!=null) {
                            newGroup.addDataSet(dir.getObject(subfolder, ds.getName()),i);
                        }
                    }
                }
                vertex[it][ip]=newGroup;
            }
        }
    }

    public void writeDataGroup(String folder, TDirectory dir) {
        dir.mkdir("/" + folder);
        dir.cd(folder);
        dir.mkdir("residuals");
        dir.cd("residuals");
        for(int is=0; is<nSector; is++) {
            for(int it=0; it<thetaBins.length; it++) {
                for(int ip=0; ip<phiBins.length; ip++) {
                    String subfolder = "sec" + (is+1) + "_theta" + thetaBins[it].getRange() + "_phi" + phiBins[ip].getRange();
                    dir.mkdir(subfolder);
                    dir.cd(subfolder);        
                    for(int il=0; il<nLayer; il++) {
                        List<IDataSet> dsList = residuals[is][it][ip].getData(il);
                        for(IDataSet ds : dsList){
        //                    System.out.println("\t --> " + ds.getName());
                            dir.addDataSet(ds);
                        }
                    }
                    dir.cd("/" + folder + "/residuals");
                }
            }
        }
        dir.cd("/" + folder);
        dir.mkdir("vertex");
        dir.cd("vertex");
        for(int it=0; it<thetaBins.length; it++) {
            for(int ip=0; ip<phiBins.length; ip++) {
                String subfolder = "theta" + thetaBins[it].getRange() + "_phi" + phiBins[ip].getRange();
                dir.mkdir(subfolder);
                dir.cd(subfolder);        
                for(int is=0; is<nSector; is++) {
                    List<IDataSet> dsList = vertex[it][ip].getData(is);
                    for(IDataSet ds : dsList){
    //                    System.out.println("\t --> " + ds.getName());
                        dir.addDataSet(ds);
                    }
                }
                dir.cd("/" + folder + "/vertex");
            }
        } 
        dir.cd();
    }
    
}
