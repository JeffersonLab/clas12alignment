package org.clas.dc.alignment;

import java.util.List;
import org.jlab.clas.physics.Particle;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.data.IDataSet;
import org.jlab.groot.data.TDirectory;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.graphics.EmbeddedCanvasTabbed;
import org.jlab.groot.graphics.EmbeddedPad;
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
    
    private DataGroup       electron  = null; 
    private DataGroup       binning   = null; 
    private DataGroup[][][] residuals = null; // indices are theta bin, phi bin and sector, datagroup is 6x6 and contains layers
    private DataGroup[][][] time      = null; // indices are theta bin, phi bin and sector, datagroup is 6x6 and contains layers
    private DataGroup[][]   vertex    = null; // indices are theta bin, phi bin and sector, datagroup is 6x6 and contains sectors

    private double[][][][] parValues = null;
    private double[][][][] parErrors = null;
    
    private Bin[] thetaBins = null;
    private Bin[] phiBins  = null;
    
    // histogram limits for residuals
    int    resBins = 200;
    double resMin  = -5000;
    double resMax  =  5000;
    // histogram limits for vertex plots
    int    vtxBins = 200;
    double vtxMin = -50.0;
    double vtxMax =  50.0;
    
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
        System.out.println("Creating histograms for " + nSector              + " sectors, " 
                                                      + (thetaBins.length-1) + " theta bins, " 
                                                      + (phiBins.length-1)   + " phi bins");
        this.residuals = new DataGroup[nSector][thetaBins.length][phiBins.length];
        this.time      = new DataGroup[nSector][thetaBins.length][phiBins.length];
        this.vertex    = new DataGroup[thetaBins.length][phiBins.length];
        this.parValues = new double[nSector][thetaBins.length][phiBins.length][nLayer+1];
        this.parErrors = new double[nSector][thetaBins.length][phiBins.length][nLayer+1];
        
        for(int is=0; is<nSector; is++) {
            int sector = is+1;
            for(int it=0; it<thetaBins.length; it++) {
                for(int ip=0; ip<phiBins.length; ip++) {
                    this.residuals[is][it][ip] = new DataGroup(6,6);
                    this.time[is][it][ip] = new DataGroup(6,6);
                    for(int il=0; il<nLayer; il++) {
                        int layer = il+1;
                        H1F hi_residual = new H1F("hi_L" + layer,"Layer " + layer + " Sector " + sector, resBins, resMin, resMax);
                        hi_residual.setTitleX("Residuals (um)");
                        hi_residual.setTitleY("Counts");
                        hi_residual.setOptStat(optStats);
                        this.residuals[is][it][ip].addDataSet(hi_residual, il);
                        H1F hi_time = new H1F("hi_L" + layer,"Layer " + layer + " Sector " + sector, resBins, resMin, resMax);
                        hi_time.setTitleX("Residuals (um)");
                        hi_time.setTitleY("Counts");
                        hi_time.setOptStat(optStats);
                        this.time[is][it][ip].addDataSet(hi_time, il);
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
        this.electron = new DataGroup(3,2);
        H1F hi_nphe  = new H1F("hi_nphe",  "HTCC NPhe",   "Counts", 100, 0., 50.);
        hi_nphe.setFillColor(4);
        H1F hi_ecal  = new H1F("hi_ecal",  "ECAL E(GeV)", "Counts", 100, 0., 4.);
        hi_ecal.setFillColor(4);
        H1F hi_vtx   = new H1F("hi_vtx",   "Vertex(cm)",  "Counts", vtxBins, vtxMin, vtxMax);
        hi_vtx.setFillColor(4);
        H1F hi_theta = new H1F("hi_theta", "#theta(deg)", "Counts", 100, 0., 40.);
        hi_theta.setFillColor(4);
        H1F hi_phi   = new H1F("hi_phi",   "#phi(deg)",   "Counts", 100, -180, 180);
        hi_phi.setFillColor(4);
        H2F hi_thetaphi = new H2F("hi_thetaphi", "", 100, -180, 180, 100, 0, 40.);
        hi_thetaphi.setTitleX("#phi(deg)");
        hi_thetaphi.setTitleY( "#theta(deg)");
        this.electron.addDataSet(hi_nphe,     0);
        this.electron.addDataSet(hi_ecal,     1);
        this.electron.addDataSet(hi_vtx,      2);
        this.electron.addDataSet(hi_theta,    3);
        this.electron.addDataSet(hi_phi,      4);
        this.electron.addDataSet(hi_thetaphi, 5);
        this.binning = new DataGroup(3,2);
        for(int is=0; is<nSector; is++) {
            int sector = is+1;
            H2F hi_vtxtheta = new H2F("hi_S" + sector, "Sector " + sector, vtxBins, vtxMin, vtxMax, 100, 0., 35);
            hi_vtxtheta.setTitleX("Vertex (cm)");
            hi_vtxtheta.setTitleY("#theta (deg)");
            this.binning.addDataSet(hi_vtxtheta, is);
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
                double vtx  = particleBank.getFloat("vz",loop);
                
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

                if(beta>0 && nphe>Constants.NPHEMIN && energy>Constants.ECALMIN) { 
                    for (int i=0; i<trackBank.getRows(); i++) {
                        if(trackBank.getShort("pindex", i)==loop) {
                            iele = trackBank.getShort("index", i);
                        }
                    }
                }
                if(iele>=0) {
                    electron.getH1F("hi_nphe").fill(nphe);
                    electron.getH1F("hi_ecal").fill(energy);
                    break;
                }
            }
        } 
                
        return iele;
    }
    private void processEvent(Event event) {

        Bank runConfig = new Bank(schema.getSchema("RUN::config"));
        Bank trackBank = new Bank(schema.getSchema("TimeBasedTrkg::TBTracks"));
        Bank hitBank   = new Bank(schema.getSchema("TimeBasedTrkg::TBHits"));
					
        if(runConfig!=null)      event.read(runConfig);
        if(trackBank!=null)      event.read(trackBank);
        if(hitBank!=null)        event.read(hitBank);
        
        if(runConfig!=null &&
           trackBank!=null && trackBank.getRows()>0 &&
           hitBank!=null   && hitBank.getRows()>0) {
            
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
            
            this.electron.getH1F("hi_vtx").fill(electron.vz());
            this.electron.getH1F("hi_theta").fill(Math.toDegrees(electron.theta()));
            this.electron.getH1F("hi_phi").fill(Math.toDegrees(electron.phi()));
            this.electron.getH2F("hi_thetaphi").fill(Math.toDegrees(electron.phi()), Math.toDegrees(electron.theta()));
                        
            this.binning.getH2F("hi_S" + sector).fill(electron.vz(), Math.toDegrees(electron.theta()));
            
            electron.vector().rotateZ(Math.toRadians(-60*(sector-1)));
            double theta = Math.toDegrees(electron.theta());
            double phi   = Math.toDegrees(electron.phi());

            for (int it = 0; it < thetaBins.length; it++) {
                if (thetaBins[it].contains(theta)) {
                    for (int ip = 0; ip < phiBins.length; ip++) {
                        if (phiBins[ip].contains(phi)) {
                            for (int i = 0; i < hitBank.getRows(); i++) {
                                if (hitBank.getInt("trkID", i) == id) {
                                    double residual = 10000 * hitBank.getFloat("fitResidual", i);
                                    double time = 10000 * hitBank.getFloat("timeResidual", i);
                                    int superlayer = hitBank.getInt("superlayer", i);
                                    int layer = hitBank.getInt("layer", i) + 6 * (superlayer - 1);

                                    this.residuals[sector - 1][it][ip].getH1F("hi_L" + layer).fill(residual);
                                    this.time[sector - 1][it][ip].getH1F("hi_L" + layer).fill(time);
                                }
                            }
                            this.vertex[it][ip].getH1F("hi_S" + sector).fill(electron.vz());
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
    
 
    public void analyzeHisto(boolean fit, int vertexFit) {
        for(int is=0; is<nSector; is++) {
            int s = is +1;
            for(int it=0; it<thetaBins.length; it++) {
                for(int ip=0; ip<phiBins.length; ip++) {
                    for(int l=1; l<=nLayer; l++) {
                        H1F hres = residuals[is][it][ip].getH1F("hi_L"+l);
                        System.out.print(String.format("\tsector=%1d theta bin=%1d phi bin=%1d layer=%2d",s,it,ip,l));
                        if(hres.getIntegral()==0) {
                            this.parValues[is][it][ip][l] = 0; 
                            this.parErrors[is][it][ip][l] = 0;
                        }
                        else {
                            if(fit) {
                                boolean fitStatus = Histo.fitResiduals(hres);
                                if(fitStatus) {
                                    this.parValues[is][it][ip][l] = hres.getFunction().getParameter(1); 
                                    this.parErrors[is][it][ip][l] = hres.getFunction().parameter(1).error();
                                }
                                else {
                                    double integral = Histo.getIntegralIDataSet(hres, hres.getFunction().getMin(), hres.getFunction().getMax());
                                    this.parValues[is][it][ip][l] = hres.getFunction().getParameter(1); 
                                    this.parErrors[is][it][ip][l] = hres.getFunction().getParameter(2)/Math.sqrt(integral);
                                }
                            }
                            else {
                                this.parValues[is][it][ip][l] = hres.getMean(); 
                                this.parErrors[is][it][ip][l] = hres.getRMS()/Math.sqrt(hres.getIntegral());
                            }
                            this.parErrors[is][it][ip][l] = Math.max(this.parErrors[is][it][ip][l],(this.resMax-this.resMin)/this.resBins);
                        }
                        System.out.print("\r");
                    }
                    H1F hvtx = vertex[it][ip].getH1F("hi_S"+s);
                    this.fitVertex(vertexFit, hvtx);
                    this.parValues[is][it][ip][0] = hvtx.getFunction().getParameter(1)*Constants.SCALE-Constants.TARGETPOS*Constants.SCALE;
                    this.parErrors[is][it][ip][0] = hvtx.getFunction().parameter(1).error()*Constants.SCALE;
                }
            }
        }
         
    }
    
    public double[] getParValues(int sector, int itheta, int iphi) {
        if(sector<1 || sector>6) 
            throw new IllegalArgumentException("Error: invalid sector="+sector);
        if(itheta<0 || itheta>=thetaBins.length) 
            throw new IllegalArgumentException("Error: invalid theta bin="+itheta);
        if(iphi<0 || iphi>phiBins.length) 
            throw new IllegalArgumentException("Error: invalid phi bin="+iphi);
        return this.parValues[sector-1][itheta][iphi];
    }
    
    public double[] getParErrors(int sector, int itheta, int iphi) {
        if(sector<1 || sector>6) 
            throw new IllegalArgumentException("Error: invalid sector="+sector);
        if(itheta<0 || itheta>=thetaBins.length) 
            throw new IllegalArgumentException("Error: invalid theta bin="+itheta);
        if(iphi<0 || iphi>phiBins.length) 
            throw new IllegalArgumentException("Error: invalid phi bin="+iphi);
        return this.parErrors[sector-1][itheta][iphi];
    }
    
    public EmbeddedCanvasTabbed getElectronPlots() {
        EmbeddedCanvasTabbed canvas = new EmbeddedCanvasTabbed("electron", "binning");
        canvas.getCanvas("electron").draw(electron);
        canvas.getCanvas("electron").getPad(3).getAxisY().setLog(true);
        canvas.getCanvas("electron").getPad(5).getAxisZ().setLog(true);
        canvas.getCanvas("binning").draw(binning);
        for(EmbeddedPad pad : canvas.getCanvas("binning").getCanvasPads())
            pad.getAxisZ().setLog(true);
        return canvas;
    }
    
    public EmbeddedCanvasTabbed plotHistos() {
        EmbeddedCanvasTabbed canvas = null;
        for(int is=0; is<nSector; is++) {
            int    sector = is+1;
            String title  = "TSec" + sector;
            if(canvas==null) canvas = new EmbeddedCanvasTabbed(title);
            else             canvas.addCanvas(title);
            canvas.getCanvas(title).draw(this.time[is][0][0]);
            for(int it=0; it<thetaBins.length; it++) {
                for(int ip=0; ip<phiBins.length; ip++) {
                    if(it==0 && ip==0) continue;
                    title = "TSec" + sector;
                    if(it!=0) title = title + " Theta:" + thetaBins[it].getRange();
                    if(ip!=0) title = title + " Phi:" + phiBins[ip].getRange();
                    canvas.addCanvas(title);
                    canvas.getCanvas(title).draw(time[is][it][ip]);
                }
            }
        }
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
    
    
    private void fitVertex(int mode, H1F histo) {
        if(mode == 1)
            Histo.fitResiduals(histo);
        else if(mode == 2)
            Histo.fit1Vertex(histo);
        else
            Histo.fit3Vertex(histo);
    }
    
    
    public static boolean fitResiduals(H1F histo) {
        double mean  = Histo.getMeanIDataSet(histo, histo.getMean()-histo.getRMS(), 
                                                    histo.getMean()+histo.getRMS());
        double amp   = histo.getBinContent(histo.getMaximumBin());
        double rms   = histo.getRMS();
        double sigma = rms/2;
        double min = mean - rms;
        double max = mean + rms;
        
        F1D f1   = new F1D("f1res","[amp]*gaus(x,[mean],[sigma])", min, max);
        f1.setLineColor(2);
        f1.setLineWidth(2);
        f1.setOptStat("1111");
        f1.setParameter(0, amp);
        f1.setParameter(1, mean);
        f1.setParameter(2, sigma);
            
        if(amp>5) {
            f1.setParLimits(0, amp*0.2,   amp*1.2);
            f1.setParLimits(1, mean*0.5,  mean*1.5);
            f1.setParLimits(2, sigma*0.2, sigma*2);
//            System.out.print("1st...");
            DataFitter.fit(f1, histo, "Q");
//            mean  = f1.getParameter(1);
//            sigma = f1.getParameter(2);
//            f1.setParLimits(0, 0, 2*amp);
//            f1.setParLimits(1, mean-sigma, mean+sigma);
//            f1.setParLimits(2, 0, sigma*2);
//            f1.setRange(mean-2.0*sigma,mean+2.0*sigma);
//            DataFitter.fit(f1, histo, "Q");
//            System.out.print("2nd");
            return true;
        }
        else {
            histo.setFunction(f1);
            return false;
        }
    }    

    
    public static boolean fit1Vertex(H1F histo) {
        double mean  = Histo.getMeanIDataSet(histo, histo.getMean()-histo.getRMS(), 
                                                    histo.getMean()+histo.getRMS());
        double amp   = histo.getBinContent(histo.getMaximumBin());
        double rms   = histo.getRMS();
        double sigma = 0.5;
        double min = histo.getDataX(0);
        double max = histo.getDataX(histo.getDataSize(0)-1);
        
        F1D f1   = new F1D("f1res","[amp]*gaus(x,[mean],[sigma])+[p0]+[p1]*x+[p2]*x*x", min, max);
        f1.setLineColor(2);
        f1.setLineWidth(2);
        f1.setOptStat("1111");
        f1.setParameter(0, amp);
        f1.setParameter(1, mean);
        f1.setParameter(2, sigma);
            
        if(amp>5) {
            f1.setParLimits(0, amp*0.2,   amp*1.2);
            f1.setParLimits(1, mean*0.5,  mean*1.5);
            f1.setParLimits(2, sigma*0.2, sigma*2);
            DataFitter.fit(f1, histo, "Q");
            return true;
        }
        else {
            histo.setFunction(f1);
            return false;
        }
    }    

    
    public static void fit3Vertex(H1F histo) {
        int nbin = histo.getData().length;
        double dx = histo.getDataX(1)-histo.getDataX(0);
        //find downstream window
        int ibin0 = histo.getMaximumBin();
        //check if the found maximum is the first or second peak, ibin is tentative upstream window
        int ibin1 = Math.max(0, histo.getMaximumBin() - (int)(Constants.TARGETLENGTH/dx));
        int ibin2 = Math.min(nbin-1, histo.getMaximumBin() + (int)(Constants.TARGETLENGTH/dx));
        if(histo.getBinContent(ibin1)<histo.getBinContent(ibin2)) {
            ibin1 = ibin0;
            ibin0 = ibin2;
        }
        
        double mean  = histo.getDataX(ibin0);
        double amp   = histo.getBinContent(ibin0);
        double sigma = 0.5;
        double bg = histo.getBinContent((ibin1+ibin0)/2);
        
        F1D f1_vtx   = new F1D("f3vertex","[amp]*gaus(x,[mean]-[tl],[sigma])+[amp]*gaus(x,[mean],[sigma])+[amp]*gaus(x,[mean]+[wd],[sigma])/1.8+[bg]*gaus(x,[mean]-[tl]/2,[tl]*0.8)", -10, 10);
	//F1D f1_vtx   = new F1D("f3vertex","[amp]*gaus(x,[mean]-[tl],[sigma])+[amp]*gaus(x,[mean],[sigma])+[amp]*gaus(x,[mean]+[wd],[sigma])/1.8+[bg]*gaus(x,[mean]/2,[tl]*0.8)", -10, 10);
        f1_vtx.setLineColor(2);
        f1_vtx.setLineWidth(2);
        f1_vtx.setOptStat("11111111");
        f1_vtx.setParameter(0, amp/2);
        f1_vtx.setParameter(1, mean);
        f1_vtx.setParameter(2, Constants.TARGETLENGTH);
        f1_vtx.setParLimits(2, Constants.TARGETLENGTH*0.99, Constants.TARGETLENGTH*1.01);
        f1_vtx.setParameter(3, sigma);
        f1_vtx.setParameter(4, Constants.WINDOWDIST);
        f1_vtx.setParLimits(4, Constants.WINDOWDIST*0.9, Constants.WINDOWDIST*1.1);
        f1_vtx.setParameter(5, bg);
//        f1_vtx.setParameter(4, sigma*3);
        f1_vtx.setRange(mean-Constants.TARGETLENGTH*1.5,mean+Constants.TARGETLENGTH);
        DataFitter.fit(f1_vtx, histo, "Q"); //No options uses error for sigma
    }

    //This was a previous version of fitting the z vertex peaks
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

    private static double getIntegralIDataSet(IDataSet data, double min, double max) {
            double nEntries = 0;
            for (int i = 0; i < data.getDataSize(0); i++) {
                    double x = data.getDataX(i);
                    double y = data.getDataY(i);
                    if (x > min && x < max && y != 0) {
                            nEntries += y;
                    }
            }
            return (double) nEntries;
    }

    private static double getMeanIDataSet(IDataSet data, double min, double max) {
            int nsamples = 0;
            double sum = 0;
            double nEntries = 0;
            for (int i = 0; i < data.getDataSize(0); i++) {
                    double x = data.getDataX(i);
                    double y = data.getDataY(i);
                    if (x > min && x < max && y != 0) {
                            nsamples++;
                            sum += x * y;
                            nEntries += y;
                    }
            }
            return sum / (double) nEntries;
    }

    private static double getRMSIDataSet(IDataSet data, double min, double max) {
            int nsamples = 0;
            double mean = getMeanIDataSet(data, min, max);
            double sum = 0;
            double nEntries = 0;

            for (int i = 0; i < data.getDataSize(0); i++) {
                    double x = data.getDataX(i);
                    double y = data.getDataY(i);
                    if (x > min && x < max && y != 0) {
                            nsamples++;
                            sum += Math.pow(x - mean, 2) * y;
                            nEntries += y;
                    }
            }
            return Math.sqrt(sum / (double) nEntries);
    }
        
    public void readDataGroup(String folder, TDirectory dir) {
        electron = this.readDataGroup(folder + "/electron", dir, electron);
        for(int is=0; is<nSector; is++) {
            for(int it=0; it<thetaBins.length; it++) {
                for(int ip=0; ip<phiBins.length; ip++) {
                    String subfolder = folder + "/residuals/sec" + (is+1) + "_theta" + thetaBins[it].getRange() + "_phi" + phiBins[ip].getRange();
                    residuals[is][it][ip]=this.readDataGroup(subfolder, dir, residuals[is][it][ip]);
                }
            }
        }
        for(int is=0; is<nSector; is++) {
            for(int it=0; it<thetaBins.length; it++) {
                for(int ip=0; ip<phiBins.length; ip++) {
                    String subfolder = folder + "/time/sec" + (is+1) + "_theta" + thetaBins[it].getRange() + "_phi" + phiBins[ip].getRange();
                    time[is][it][ip]=this.readDataGroup(subfolder, dir, time[is][it][ip]);
                }
            }
        }
        for(int it=0; it<thetaBins.length; it++) {
            for(int ip=0; ip<phiBins.length; ip++) {
                String subfolder = folder + "/vertex/theta" + thetaBins[it].getRange() + "_phi" + phiBins[ip].getRange();
                vertex[it][ip]=this.readDataGroup(subfolder, dir, vertex[it][ip]);
            }
        }
    }

    private DataGroup readDataGroup(String folder, TDirectory dir, DataGroup dg) {
        int nrows = dg.getRows();
        int ncols = dg.getColumns();
        int nds   = nrows*ncols;
        DataGroup newGroup = new DataGroup(ncols,nrows);
        for(int i = 0; i < nds; i++){
            List<IDataSet> dsList = dg.getData(i);
            for(IDataSet ds : dsList){
                if(dir.getObject(folder, ds.getName())!=null) {
                    newGroup.addDataSet(dir.getObject(folder, ds.getName()),i);
                }
            }
        }
        return newGroup;
    }
    
    public void writeDataGroup(String root, String folder, TDirectory dir) {
        dir.cd("/" + root);
        dir.mkdir(folder);
        dir.cd(folder);
        this.writeDataGroup("electron", dir,electron);
        dir.cd("/" + root + "/" + folder);
        dir.mkdir("residuals");
        dir.cd("residuals");
        for(int is=0; is<nSector; is++) {
            for(int it=0; it<thetaBins.length; it++) {
                for(int ip=0; ip<phiBins.length; ip++) {
                    String subfolder = "sec" + (is+1) + "_theta" + thetaBins[it].getRange() + "_phi" + phiBins[ip].getRange();
                    this.writeDataGroup(subfolder, dir, residuals[is][it][ip]);
                    dir.cd("/" + root + "/" + folder + "/residuals");
                }
            }
        }
        dir.cd("/" + root + "/" + folder);
        dir.mkdir("time");
        dir.cd("time");
        for(int is=0; is<nSector; is++) {
            for(int it=0; it<thetaBins.length; it++) {
                for(int ip=0; ip<phiBins.length; ip++) {
                    String subfolder = "sec" + (is+1) + "_theta" + thetaBins[it].getRange() + "_phi" + phiBins[ip].getRange();
                    this.writeDataGroup(subfolder, dir, time[is][it][ip]);
                    dir.cd("/" + root + "/" + folder + "/time");
                }
            }
        }
        dir.cd("/" + root + "/" + folder);
        dir.mkdir("vertex");
        dir.cd("vertex");
        for(int it=0; it<thetaBins.length; it++) {
            for(int ip=0; ip<phiBins.length; ip++) {
                String subfolder = "theta" + thetaBins[it].getRange() + "_phi" + phiBins[ip].getRange();
                this.writeDataGroup(subfolder, dir, vertex[it][ip]);
                dir.cd("/" + root + "/" + folder + "/vertex");
            }
        } 
        dir.cd();
    }
    
    private void writeDataGroup(String folder, TDirectory dir, DataGroup dg) {
        int nrows = dg.getRows();
        int ncols = dg.getColumns();
        int nds   = nrows*ncols;
        dir.mkdir(folder);
        dir.cd(folder);        
        for(int i=0; i<nds; i++) {
            List<IDataSet> dsList = dg.getData(i);
            for(IDataSet ds : dsList){
//                    System.out.println("\t --> " + ds.getName());
                dir.addDataSet(ds);
            }
        }
    }
    
}
