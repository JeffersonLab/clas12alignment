package org.clas.dc.alignment;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jlab.clas.physics.Particle;
import org.jlab.geom.prim.Vector3D;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.data.IDataSet;
import org.jlab.groot.data.TDirectory;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.graphics.EmbeddedCanvasTabbed;
import org.jlab.groot.graphics.EmbeddedPad;
import org.jlab.groot.group.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.groot.math.Func1D;
import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.utils.benchmark.ProgressPrintout;

/**
 *
 * @author devita
 */
public class Histo {
    
    private String name;
    
    private final int nSector = Constants.NSECTOR;
    private final int nLayer  = Constants.NLAYER;
    private final int nSLayer = Constants.NSUPERLAYER;
    private final int nTarget = Constants.NTARGET;
    
    private DataGroup       electron  = null; 
    private DataGroup       binning   = null; 
    private DataGroup       offset    = null; 
    private DataGroup       calib     = null; 
    private DataGroup[]     wires     = null; 
    private DataGroup[][][] residuals = null; // indices are theta bin, phi bin and sector, datagroup is 6x6 and contains layers
    private DataGroup[][][] time      = null; // indices are theta bin, phi bin and sector, datagroup is 6x6 and contains layers
    private DataGroup[][]   vertex    = null; // indices are theta bin, phi bin and sector, datagroup is 6x6 and contains sectors

    private double[][][][] parValues = null;
    private double[][][][] parErrors = null;
    private double[][]     beamOffset= {{0, 0}, {0, 0}};
    
    private Bin[] thetaBins = null;
    private Bin[] phiBins  = null;
        
    private double minVtx = Constants.VTXMIN;
    private double maxVtx = Constants.VTXMAX;

    private List<String> nominalFiles = null;
    private List<String> shiftedFiles = null;
    private SchemaFactory      schema = null;
    
    private boolean tres  = false;
    private boolean shift = false;
    
    private static final Logger LOGGER = Logger.getLogger(Constants.LOGGERNAME);
    
    public Histo(String name, List<String> files, Bin[] thetabins, Bin[] phibins, double[] vertexrange, String optstats) {
        this.name      = name;
        this.thetaBins = thetabins;
        this.phiBins   = phibins;
        this.nominalFiles = files;
        this.createHistos(optstats);
    }
    
    public Histo(String name, List<String> files, Bin[] thetabins, Bin[] phibins, boolean time, double[] vertexrange, String optstats) {
        this.name      = name;
        this.thetaBins = thetabins;
        this.phiBins   = phibins;
        this.minVtx    = vertexrange[0];
        this.maxVtx    = vertexrange[1];
        this.tres      = time;
        this.nominalFiles = files;
        this.createHistos(optstats);
    }
    
    public Histo(String name, List<String> files, List<String> shifted, Bin[] thetabins, Bin[] phibins, double[] vertexrange, String optstats) {
        this.name      = name;
        this.thetaBins = thetabins;
        this.phiBins   = phibins;
        this.nominalFiles = files;
        this.shiftedFiles = shifted;
        if(shifted!=null) shift = true;
        this.createHistos(optstats);
    }
    
    public Histo(String name, boolean shift, Bin[] thetabins, Bin[] phibins, double[] vertexrange, String optstats) {
        this.name      = name;
        this.thetaBins = thetabins;
        this.phiBins   = phibins;
        this.shift     = shift;
        this.createHistos(optstats);
    }
    
    public Histo(String name, boolean shift, Bin[] thetabins, Bin[] phibins, boolean time, double[] vertexrange, String optstats) {
        this.name      = name;
        this.thetaBins = thetabins;
        this.phiBins   = phibins;
        this.shift     = shift;
        this.tres      = time;
        this.createHistos(optstats);
    }
    
    private void createHistos(String optStats) {
        LOGGER.info("[Histo] Creating histograms for " + nSector              + " sectors, " 
                                                       + (thetaBins.length-1) + " theta bins, " 
                                                       + (phiBins.length-1)   + " phi bins "
                                                       + "for variation " + name);
        this.residuals = new DataGroup[nSector][thetaBins.length][phiBins.length];
        if(tres) {
            this.wires = new DataGroup[nSector];
            this.time  = new DataGroup[nSector][thetaBins.length][phiBins.length];
        }
        this.vertex    = new DataGroup[thetaBins.length][phiBins.length];
        this.parValues = new double[nSector][thetaBins.length][phiBins.length][nLayer+nTarget];
        this.parErrors = new double[nSector][thetaBins.length][phiBins.length][nLayer+nTarget];
        
        int nbinsRes  = Constants.RESBINS;
        double minRes = Constants.RESMIN;
        double maxRes = Constants.RESMAX;
        int nbinsVtx  = Constants.VTXBINS;
        if(shift) {
            nbinsRes = Constants.DIFBINS;
            minRes = Constants.DIFMIN;
            maxRes = Constants.DIFMAX;
            nbinsVtx  = Constants.VDFBINS;
            minVtx = Constants.VDFMIN;
            maxVtx = Constants.VDFMAX;
        }
        
        this.calib = new DataGroup(nSector, nSLayer);
        for(int is=0; is<nSector; is++) {
            int sector = is+1;
            for(int it=0; it<thetaBins.length; it++) {
                for(int ip=0; ip<phiBins.length; ip++) {
                    this.residuals[is][it][ip] = new DataGroup(6,6);
                    if(tres) this.time[is][it][ip] = new DataGroup(6,6);
                    for(int il=0; il<nLayer; il++) {
                        int layer = il+1;
                        H1F hi_residual = new H1F("hi-L" + layer,"Layer " + layer + " Sector " + sector, nbinsRes, minRes, maxRes);
                        hi_residual.setTitleX("Residuals (um)");
                        hi_residual.setTitleY("Counts");
                        hi_residual.setOptStat(optStats);
                        this.residuals[is][it][ip].addDataSet(hi_residual, il);
                    }
                }
            }
            for(int isl=0; isl<nSLayer; isl++) {
                int superlayer = isl+1;
                H1F hi_time = new H1F("hi-SL" + superlayer + "_S" + sector, "SL " + superlayer + " Sector " + sector, nbinsRes, minRes, maxRes);
                hi_time.setTitleX("Residuals (um)");
                hi_time.setTitleY("Counts");
                hi_time.setOptStat(optStats);
                this.calib.addDataSet(hi_time, is+isl*nSector);
            }
        }
        if(tres) {
            for(int is=0; is<nSector; is++) {
                int sector = is+1;
                this.wires[is] = new DataGroup(nSector, nSLayer);
                for(int il=0; il<nLayer; il++) {
                    int layer = il+1;
                    H2F hi_wire = new H2F("hi-L" + layer + "_S" + sector, "L " + layer + " Sector " + sector, nbinsRes, minRes, maxRes, 112, 1, 113);
                    hi_wire.setTitleX("Residuals (um)");
                    hi_wire.setTitleY("Wire");
                    this.wires[is].addDataSet(hi_wire, il);   
                }
                for(int it=0; it<thetaBins.length; it++) {
                    for(int ip=0; ip<phiBins.length; ip++) {
                        this.time[is][it][ip] = new DataGroup(6,6);
                        for(int il=0; il<nLayer; il++) {
                            int layer = il+1;
                            H1F hi_time = new H1F("hi-L" + layer,"Layer " + layer + " Sector " + sector, nbinsRes, minRes, maxRes);
                            hi_time.setTitleX("Residuals (um)");
                            hi_time.setTitleY("Counts");
                            hi_time.setOptStat(optStats);
                            this.time[is][it][ip].addDataSet(hi_time, il);                            
                        }
                    } 
                }
            }
        }
        for(int it=0; it<thetaBins.length; it++) {
            for(int ip=0; ip<phiBins.length; ip++) {
                this.vertex[it][ip] = new DataGroup(3,2);
                for(int is=0; is<nSector; is++) {
                    int sector = is+1;
                    H1F hi_vertex = new H1F("hi-S" + sector,"Sector " + sector, nbinsVtx, minVtx, maxVtx);
                    hi_vertex.setTitleX("Vertex (cm)");
                    hi_vertex.setTitleY("Counts");
                    hi_vertex.setOptStat(optStats);
                    this.vertex[it][ip].addDataSet(hi_vertex, is);
                }
            }
        }           
        this.electron = new DataGroup(3,2);
        H1F hi_nphe  = new H1F("hi-nphe",  "HTCC NPhe",   "Counts", 100, 0., 50.);
        hi_nphe.setFillColor(4);
        H1F hi_ecal  = new H1F("hi-ecal",  "ECAL E(GeV)", "Counts", 100, 0., 4.);
        hi_ecal.setFillColor(4);
        H1F hi_vtx   = new H1F("hi-vtx",   "Vertex(cm)",  "Counts", nbinsVtx, minVtx, maxVtx);
        hi_vtx.setFillColor(4);
        H1F hi_theta = new H1F("hi-theta", "#theta(deg)", "Counts", 100, 0., 40.);
        hi_theta.setFillColor(4);
        H1F hi_phi   = new H1F("hi-phi",   "#phi(deg)",   "Counts", 100, -180, 180);
        hi_phi.setFillColor(4);
        H2F hi_thetaphi = new H2F("hi-thetaphi", "", 100, -180, 180, 100, 0, 40.);
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
            H2F hi_vtxtheta = new H2F("hi-S" + sector, "Sector " + sector, nbinsVtx, minVtx, maxVtx, 100, 0., 35);
            hi_vtxtheta.setTitleX("Vertex (cm)");
            hi_vtxtheta.setTitleY("#theta (deg)");
            F1D ftheta = new F1D("ftheta_S" + sector,"57.29*atan([r]/([z0]-x))",minVtx, maxVtx);
            ftheta.setParameter(0, Constants.MOLLERR);
            ftheta.setParameter(1, Constants.MOLLERZ);
            ftheta.setLineColor(2);
            ftheta.setLineWidth(2);
            F1D fthetaOff = new F1D("fthetaOff_S" + sector,"57.29*atan([r]/([z0]-x))",minVtx, maxVtx);
            double y0=0.715;
            fthetaOff.setParameter(0, -y0*Math.sin(Math.toRadians(is*60))+Math.sqrt(Math.pow(Constants.MOLLERR, 2)-Math.pow(y0*Math.cos(Math.toRadians(is*60)), 2)));
            fthetaOff.setParameter(1, Constants.MOLLERZ);
            fthetaOff.setLineColor(4);
            fthetaOff.setLineWidth(2);
            this.binning.addDataSet(hi_vtxtheta, is);
            this.binning.addDataSet(ftheta, is);
            this.binning.addDataSet(fthetaOff, is);
        }
        this.offset = new DataGroup(1,2);
        H2F hi_thetasc = new H2F("hi-thetasc", "", 36, -180, 180, 40, 0, 20.);
        hi_thetasc.setTitleX("#phi(deg)");
        hi_thetasc.setTitleY("#theta(deg)");
        this.offset.addDataSet(hi_thetasc, 0);
    }
  
    private class Hit {
        public int id = -1;
        public int wire  = -1;
        public int layer  = -1;
        public int superlayer = -1;
        public int sector = -1;
        public double residual;
        public double time;
        public int status = 0;
        
        public Hit(int sector, int layer, int wire, double residual, double time, int status) {
            this.sector = sector;
            this.layer  = layer;
            this.wire   = wire;
            this.superlayer = (layer-1)/6 +1;
            this.residual = residual;
            this.time = time;
            this.status = status;
        }       
        
        public boolean equals(Hit h) {
            if(this.sector == h.sector &&
               this.layer  == h.layer &&
               this.wire   == h.wire) {
                return true;
            }
            return false;
        }
    }
    
    private class Electron extends Particle {
        private int id;
        private int sector;
        
        public Electron(int pid, double px, double py, double pz, double vx, double vy, double vz, int trackId, int sector) {
            super(pid, px, py, pz, vx, vy, vz);
            this.id = trackId;
            this.sector = sector;
        }
        
        public int id() {
            return id;
        }
        
        public int sector() {
            return sector;
        }
    
        public double phiSector() {
            Vector3D dir = new Vector3D(this.px(), this.py(), this.pz());
            dir.rotateZ(-Math.PI/3*(sector-1));
            return dir.phi();
        }
    }
    
    private int getElectronIndex(Event event) {
       
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
                        break;
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
                            break;
                        }
                    }
                }
                if(iele>=0) {
                    electron.getH1F("hi-nphe").fill(nphe);
                    electron.getH1F("hi-ecal").fill(energy);
                    break;
                }
            }
        } 
                
        return iele;
    }
    
    private Electron getElectron(Event event) {
        int iele = this.getElectronIndex(event);
        if(iele<0) return null;

        Bank trackBank = new Bank(schema.getSchema("TimeBasedTrkg::TBTracks"));
					
        if(trackBank!=null) event.read(trackBank);
        
        if(trackBank!=null && trackBank.getRows()>0) {
//            Line3D track = new Line3D(new  Point3D(trackBank.getFloat("t1_x", iele),
//                                                   trackBank.getFloat("t1_y", iele),
//                                                   trackBank.getFloat("t1_z", iele)),
//                                      new Vector3D(trackBank.getFloat("t1_px", iele),
//                                                   trackBank.getFloat("t1_py", iele),
//                                                   trackBank.getFloat("t1_pz", iele)));
//            Point3D vertex = track.distance(new Line3D(0,0,0,0,0,1)).lerpPoint(0);
//            Electron elec = new Electron(11,
//                                         trackBank.getFloat("t1_px", iele),
//                                         trackBank.getFloat("t1_py", iele),
//                                         trackBank.getFloat("t1_pz", iele),
//                                         vertex.x(),//trackBank.getFloat("Vtx0_x", iele),
//                                         vertex.y(),//trackBank.getFloat("Vtx0_y", iele),
//                                         vertex.z(),//trackBank.getFloat("Vtx0_z", iele),
//                                         trackBank.getInt("id", iele),
//                                         trackBank.getByte("sector", iele));
//            System.out.println(vertex + " " + elec.vertex() + "\n");
            Electron elec = new Electron(11,
                                         trackBank.getFloat("p0_x", iele),
                                         trackBank.getFloat("p0_y", iele),
                                         trackBank.getFloat("p0_z", iele),
                                         trackBank.getFloat("Vtx0_x", iele),
                                         trackBank.getFloat("Vtx0_y", iele),
                                         trackBank.getFloat("Vtx0_z", iele),
                                         trackBank.getInt("id", iele),
                                         trackBank.getByte("sector", iele));
            return elec;
        }
        return null;
    }
    
    private void processEvent(Event event, Event shifted) {
        
        Electron electron = this.getElectron(event);
        
        if(electron!=null) {
            this.electron.getH1F("hi-vtx").fill(electron.vz());
            this.electron.getH1F("hi-theta").fill(Math.toDegrees(electron.theta()));
            this.electron.getH1F("hi-phi").fill(Math.toDegrees(electron.phi()));
            this.electron.getH2F("hi-thetaphi").fill(Math.toDegrees(electron.phi()), Math.toDegrees(electron.theta()));
                        
            this.binning.getH2F("hi-S" + electron.sector()).fill(electron.vz(), Math.toDegrees(electron.theta()));
            
            if(Math.abs(electron.vz()-(Constants.SCEXIT+Constants.TARGETCENTER))<Constants.PEAKWIDTH && Math.abs(Math.toDegrees(electron.phiSector()))<10)
                this.offset.getH2F("hi-thetasc").fill(Math.toDegrees(electron.phi()), Math.toDegrees(electron.theta()));
            
            electron.vector().rotateZ(Math.toRadians(-60*(electron.sector()-1)));
            double theta = Math.toDegrees(electron.theta());
            double phi   = Math.toDegrees(electron.phi());
            double vz    = electron.vz();
            int sector   = electron.sector();
            
            List<Hit> hits = this.getHits(event, sector, electron.id());

            if(shifted!=null) {
                Electron shiftedElectron = this.getElectron(shifted);
                if(shiftedElectron!=null) {
                    vz -= shiftedElectron.vz();
                    List<Hit> shiftedHits = this.getHits(shifted, sector, shiftedElectron.id());
                    for(Hit hit : hits) {
                        for(Hit shift : shiftedHits) {
                            if(hit.equals(shift)) {
                                hit.residual -= shift.residual;
                                break;
                            }
                        }
                    }
                }
            }
                
            for (int it = 0; it < thetaBins.length; it++) {
                if (thetaBins[it].contains(theta)) {
                    for (int ip = 0; ip < phiBins.length; ip++) {
                        if (phiBins[ip].contains(phi)) {
                            for(Hit hit : hits) {
                                this.residuals[sector - 1][it][ip].getH1F("hi-L" + hit.layer).fill(hit.residual);
                                this.calib.getH1F("hi-SL" + hit.superlayer + "_S" + hit.sector).fill(hit.time);
                            if(tres)
                                    this.wires[sector-1].getH2F("hi-L" + hit.layer + "_S" + hit.sector).fill(hit.time, hit.wire);
                                    this.time[sector - 1][it][ip].getH1F("hi-L" + hit.layer).fill(hit.time);
                            }
                            this.vertex[it][ip].getH1F("hi-S" + sector).fill(vz);
                    
                        }
                    }
                }
            }
            
        }
    }
 
    private List<Hit> getHits(Event event, int sector, int tid) {
        List<Hit> hits = new ArrayList<>();
        
        Bank hitBank = new Bank(schema.getSchema("TimeBasedTrkg::TBHits"));
        if(hitBank!=null) event.read(hitBank);
        
        if(hitBank!=null && hitBank.getRows()>0) {
            for (int i = 0; i < hitBank.getRows(); i++) {
                if ((/*tid<0 ||*/ hitBank.getInt("trkID", i) == tid) && 
                     hitBank.getInt("status", i) == 0 &&
                     hitBank.getInt("sector", i) == sector) {
                    double residual = 10000 * hitBank.getFloat("fitResidual", i);
                    double time     = 10000 * hitBank.getFloat("timeResidual", i);
                    int superlayer  = hitBank.getInt("superlayer", i);
                    int layer       = hitBank.getInt("layer", i) + 6 * (superlayer - 1);
                    int wire        = hitBank.getInt("wire", i);
                    int status      = hitBank.getInt("status", i);
                    Hit hit = new Hit(sector, layer, wire, residual, time, status);
                    hits.add(hit);
                }
            }
        }
        return hits;
    }
        
    public void processFiles(int maxEvents) {
        
        if(this.shiftedFiles!=null && this.shiftedFiles.size()!=this.nominalFiles.size())
            return;
        
        ProgressPrintout progress = new ProgressPrintout();

        int counter=-1;
        
        for(int i=0; i<this.nominalFiles.size(); i++){
            
            String nominalFile = nominalFiles.get(i);
            String shiftedFile = null;
            if(shiftedFiles!=null)
                shiftedFile = shiftedFiles.get(i);
            
            
            FileManager handler = new FileManager(nominalFile, shiftedFile);
            schema = handler.getSchema();
            
            while (handler.getNext()) {

                counter++;

                this.processEvent(handler.nominal(), handler.shifted());
                
                progress.updateStatus();
                if(maxEvents>0){
                    if(counter>=maxEvents) break;
                }
            }
            progress.showStatus();
            handler.close();
        } 
    }
    
 
    public void analyzeHisto(int fit, int vertexFit) {
        Logger.getLogger("org.freehep.math.minuit").setLevel(Level.WARNING);
        Histo.fitVertex(vertexFit,electron.getH1F("hi-vtx"));
        for(int is=0; is<nSector; is++) {
            int s = is +1;
            for(int isl=0; isl<nSLayer; isl++) {
                int sl = isl+1;
                H1F hres = calib.getH1F("hi-SL"+sl+"_S"+s);
                Histo.fitResiduals(fit, hres);
            }
            for(int it=0; it<thetaBins.length; it++) {
                for(int ip=0; ip<phiBins.length; ip++) {
                    for(int il=0; il<nLayer+nTarget; il++) {
                        this.parValues[is][it][ip][il] = 0; 
                        this.parErrors[is][it][ip][il] = 0;
                    }
                    for(int l=1; l<=nLayer; l++) {
                        H1F hres = residuals[is][it][ip].getH1F("hi-L"+l);
                        System.out.print(String.format("\tsector=%1d theta bin=%1d phi bin=%1d layer=%2d",s,it,ip,l));
                        if(Histo.fitResiduals(fit, hres)) {
                            this.parValues[is][it][ip][l] = hres.getFunction().getParameter(1); 
                            this.parErrors[is][it][ip][l] = hres.getFunction().parameter(1).error();        
                            if(!shift) this.parErrors[is][it][ip][l] = Math.max(this.parErrors[is][it][ip][l],(Constants.RESMAX-Constants.RESMIN)/Constants.RESBINS/2);
                        }
                        System.out.print("\r");
                    }
                    H1F hvtx = vertex[it][ip].getH1F("hi-S"+s);
                    double dx = hvtx.getDataX(1)-hvtx.getDataX(0);
                    if(Histo.fitVertex(vertexFit, hvtx)) {
                        this.parValues[is][it][ip][0] = hvtx.getFunction().getParameter(1)*Constants.SCALE;
                        this.parErrors[is][it][ip][0] = hvtx.getFunction().parameter(1).error()*Constants.SCALE;
                        if(!shift) {
                            this.parValues[is][it][ip][0] -= Constants.TARGETPOS*Constants.SCALE;
                            this.parErrors[is][it][ip][0] = Math.max(this.parErrors[is][it][ip][0], Constants.SCALE*dx/2);
                            int itl  = -1;
                            int isc  = -1;
                            int iscw = -1;
                            for(int i=0; i<hvtx.getFunction().getNPars(); i++) {
                                if(hvtx.getFunction().parameter(i).name().equals("tl"))  itl = i;
                                if(hvtx.getFunction().parameter(i).name().equals("sc"))  isc = i;
                                if(hvtx.getFunction().parameter(i).name().equals("scw")) iscw = i;
                            }
                            if(isc>=0 && iscw>=0 && hvtx.getFunction().getParameter(isc)>10) {
                                this.parValues[is][it][ip][nLayer+nTarget-1] = (hvtx.getFunction().getParameter(iscw)-Constants.SCEXIT)*Constants.SCALE;
                                this.parErrors[is][it][ip][nLayer+nTarget-1] =  Math.max(hvtx.getFunction().parameter(iscw).error()*Constants.SCALE, Constants.SCALE*dx);
                            }
                            if(itl>=0 && hvtx.getFunction().getParameter(0)>10) {
                                this.parValues[is][it][ip][nLayer+nTarget-2] = (hvtx.getFunction().getParameter(itl)-Constants.TARGETLENGTH)*Constants.SCALE;
                                this.parErrors[is][it][ip][nLayer+nTarget-2] =  Math.max(hvtx.getFunction().parameter(itl).error()*Constants.SCALE, Constants.SCALE*dx);
                            }
                        }
                    }
                }
            }
        }         
        if(offset.getH2F("hi-thetasc")!=null) {
            GraphErrors grthetasc = Histo.getThresholdCrossingProfile(offset.getH2F("hi-thetasc"), 0.5);
            if(grthetasc.getDataSize(0)>1) {
                GraphErrors grradius = this.fitOffset(grthetasc, Constants.MOLLERZ-(Constants.SCEXIT+Constants.TARGETCENTER));
                this.beamOffset[0][0] = grradius.getFunction().getParameter(1)*Math.cos(Math.toRadians(grradius.getFunction().getParameter(2)));
                this.beamOffset[1][0] = grradius.getFunction().getParameter(1)*Math.sin(Math.toRadians(grradius.getFunction().getParameter(2)));
                this.beamOffset[0][1] = Math.sqrt(Math.pow(grradius.getFunction().parameter(1).error()*Math.cos(Math.toRadians(grradius.getFunction().getParameter(2))),2)+
                                                  Math.pow(grradius.getFunction().getParameter(1)*Math.sin(Math.toRadians(grradius.getFunction().getParameter(2)))*Math.toRadians(grradius.getFunction().parameter(2).error()),2));
                this.beamOffset[1][1] = Math.sqrt(Math.pow(grradius.getFunction().parameter(1).error()*Math.sin(Math.toRadians(grradius.getFunction().getParameter(2))),2)+
                                                  Math.pow(grradius.getFunction().getParameter(1)*Math.cos(Math.toRadians(grradius.getFunction().getParameter(2)))*Math.toRadians(grradius.getFunction().parameter(2).error()),2));
                offset.addDataSet(grthetasc, 0);
                offset.addDataSet(grradius,  1);
            }
        }
        System.out.print("\n");
        Logger.getLogger("org.freehep.math.minuit").setLevel(Level.INFO);
        this.getFailedFitStats();
    }
    
    
    public void getFailedFitStats() {
        int nfailed = 0;
        for(int is=0; is<nSector; is++) {
            for(int it=1; it<thetaBins.length; it++) {
                for(int ip=1; ip<phiBins.length; ip++) {
                    for(int l=1; l<=nLayer; l++) {
                        if(residuals[is][it][ip].getH1F("hi-L"+l).getFunction()!=null &&
                          !residuals[is][it][ip].getH1F("hi-L"+l).getFunction().isFitValid()) {
                            nfailed++;
                            LOGGER.log(Level.WARNING, String.format("\tResidual fit for sector=%1d theta bin=%1d phi bin=%1d layer=%2d FAILED",is+1,it,ip,l));
                        }
                    }
                    if(vertex[it][ip].getH1F("hi-S"+(is+1)).getFunction()!=null &&
                      !vertex[it][ip].getH1F("hi-S"+(is+1)).getFunction().isFitValid()) {
                        nfailed++;
                        LOGGER.log(Level.WARNING, String.format("\tVertex fit for sector=%1d theta bin=%1d phi bin=%1d FAILED",is+1,it,ip));
                    }
                }
            }
        }
        LOGGER.log(Level.WARNING, String.format("\tTotal number of failed fits %d out of %d",nfailed, nSector*(thetaBins.length-1)*(phiBins.length-1)*(nLayer+1)));        
    }
    
    
    public double[][] getBeamOffset() {
        return this.beamOffset;
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
        EmbeddedCanvasTabbed canvas = new EmbeddedCanvasTabbed("electron", "binning", "offset");
        canvas.getCanvas("electron").draw(electron);
        canvas.getCanvas("electron").getPad(3).getAxisY().setLog(true);
        canvas.getCanvas("electron").getPad(5).getAxisZ().setLog(true);
        canvas.getCanvas("binning").draw(binning);
        for(EmbeddedPad pad : canvas.getCanvas("binning").getCanvasPads())
            pad.getAxisZ().setLog(true);
        canvas.getCanvas("offset").draw(offset);
        for(EmbeddedPad pad : canvas.getCanvas("offset").getCanvasPads())
            pad.getAxisZ().setLog(true);
        return canvas;
    }
    
    public EmbeddedCanvasTabbed plotHistos() {
        EmbeddedCanvasTabbed canvas = new EmbeddedCanvasTabbed("Calibration");
        canvas.getCanvas("Calibration").draw(calib);
        if(tres) {
            for(int is=0; is<nSector; is++) {
                int    sector = is+1;
                String title  = "WSec" + sector;
                canvas.addCanvas(title);
                canvas.getCanvas(title).draw(wires[is]);
            }
            for(int is=0; is<nSector; is++) {
                int    sector = is+1;
                String title  = "TSec" + sector;
                canvas.addCanvas(title);
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
        }
        for(int is=0; is<nSector; is++) {
            int    sector = is+1;
            String title  = "Sec" + sector;
            canvas.addCanvas(title);
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
                String title = "Theta:" + thetaBins[it].getRange() + " Phi:" + phiBins[ip].getRange();
                canvas.addCanvas(title);
                canvas.getCanvas(title).draw(vertex[it][ip]);
            }
        }
        return canvas;
    }
    
    
    private static boolean fitVertex(int mode, H1F histo) {
        switch (mode) {
            case 1:
                Histo.fitResiduals(2, histo);
                break;
            case 2:
                Histo.fit1Vertex(histo);
                break;
            case 3:
                Histo.fitDoublePeak(histo);
                break;
            case 4:    
                Histo.fit3Vertex(histo);
                break;
            case 5:    
                Histo.fit4Vertex(histo);
                break;
            case 6:    
                Histo.fitRGCVertex(histo);
                break;
            case 7:    
                Histo.fitRGDVertex(histo);
                break;
            default:
                if(histo.getFunction()!=null)
                    histo.getFunction().setFitValid(true);
                break;
        }
        histo.getFunction().setStatBoxFormat("%.2f");
        histo.getFunction().setStatBoxErrorFormat("%.2f");
        return histo.getFunction().isFitValid();
    }
    
    
    public static boolean fitResiduals(int fit, H1F histo) {
        double mean  = Histo.getMeanIDataSet(histo, histo.getMean()-histo.getRMS(), 
                                                    histo.getMean()+histo.getRMS());
        double amp   = histo.getBinContent(histo.getMaximumBin());
        double rms   = histo.getRMS();
        double sigma = rms/2;
        double min = mean - rms;
        double max = mean + rms;
        
        F1D f1   = new F1D("f"+histo.getName(),"[amp]*gaus(x,[mean],[sigma])", min, max);
        f1.setLineColor(2);
        f1.setLineWidth(2);
        f1.setOptStat("1111");
        f1.setParameter(0, amp);
        f1.setParameter(1, mean);
        f1.setParameter(2, sigma);
        f1.parameter(1).setError(sigma/Math.sqrt(histo.getIntegral()));
                
        if(amp<5) {
            return false;
        }
        else if(fit==1) {
            f1.setFitValid(true);
            histo.setFunction(f1);
        }
        else if(fit==2) {
            f1.setParLimits(0, amp*0.2,   amp*1.2);
            f1.setParLimits(1, mean*0.5,  mean*1.5);
            f1.setParLimits(2, sigma*0.2, sigma*2);
            DataFitter.fit(f1, histo, "Q");
            mean  = f1.getParameter(1);
            sigma = f1.getParameter(2);
            f1.setParLimits(0, 0, 2*amp);
            f1.setParLimits(1, mean-sigma, mean+sigma);
            f1.setParLimits(2, 0, sigma*2);
            f1.setRange(mean-2.0*sigma,mean+2.0*sigma);
            DataFitter.fit(f1, histo, "Q");
        }
        else if(histo.getFunction()!=null) {
            histo.getFunction().setFitValid(true);
        }
        histo.getFunction().setStatBoxFormat("%.1f");
        histo.getFunction().setStatBoxErrorFormat("%.1f");
        return histo.getFunction().isFitValid();
    }    

    /**
     * 2-Gaussian function fit function
     * Used for RG-F
     * @param histo
     * @return
     */
    public static boolean fitDoublePeak(H1F histo) {
        double mean = histo.getDataX(getMaximumBinBetween(histo, -50, 0));
        double amp   = histo.getBinContent(getMaximumBinBetween(histo, -50, 0));
        double sigma = 2;
        double min = histo.getDataX(0);
        double max = histo.getDataX(histo.getDataSize(0)-1);
        int nbin = histo.getData().length;
        double peak_separation = 2.4;    //distance in cm between two walls of upstream target window
        double second_gauss_amp_factor = 3;
        double third_gauss_mean_offset = 2.4;
        double third_gauss_amp_factor = 8;
        double third_gauss_sigma_factor = 3;
        
        //Fit a stand-alone gaussian to peak to get initial parameters for total fit function
        //This is not plotted
        F1D f1_gaus   = new F1D("f1gaus","[amp]*gaus(x,[mean],[sigma])", -50, -20);
        f1_gaus.setLineColor(3);
        f1_gaus.setLineWidth(2);
        f1_gaus.setParameter(0, amp);
        f1_gaus.setParameter(1, mean);
        f1_gaus.setParameter(2, sigma);
        f1_gaus.setOptStat("1111");
        
        //Fit a stand-alone quadtractic background to get initial parameters for total fit function
        //This is not plotted
        F1D f1_bckgr   = new F1D("f1background","[p0]+[p1]*x+[p2]*x*x", -20, 18);
        f1_bckgr.setLineColor(4);
        f1_bckgr.setLineWidth(2);
        f1_bckgr.setOptStat("1111");
        
        
        F1D fdouble_peak = new F1D("f"+histo.getName(),
                                   "[amp]*gaus(x,[mean],[sigma])+" +
                                   "[amp]/[secondGausAmpFactor]*gaus(x,[mean]-[peak_sep],[sigma])+" + 
                                   "[amp]/[thirdGaussAmpFactor]*gaus(x,[mean]+[thirdGausMeanOffset],[sigma]*[thirdGausSigmaFactor])+" +
                                   "[p0]+[p1]*x+[p2]*x*x", -50, 0);
        fdouble_peak.setLineColor(2);
        fdouble_peak.setLineWidth(2);
        fdouble_peak.setOptStat("1111");                
        
        if(amp > 5) {
            //Do background-only fit
            DataFitter.fit(f1_bckgr, histo, "Q");
            //Get parameter results from background-only fit
            double bckgr_p0 = f1_bckgr.getParameter(0);
            double bckgr_p1 = f1_bckgr.getParameter(1);
            double bckgr_p2 = f1_bckgr.getParameter(2);
            
            //Do single gaussian fit
            f1_gaus.setParLimits(0, amp*0.8,   amp*1.2);
            f1_gaus.setParLimits(1, mean*0.9,  mean*1.1);
            f1_gaus.setParLimits(2, sigma*0.2, sigma*2.5);
            DataFitter.fit(f1_gaus, histo, "Q");
            //Get parameter results from single gaussian fit
            double gaus_amp = f1_gaus.getParameter(0);
            double gaus_mean = f1_gaus.getParameter(1);
            double gaus_sigma = f1_gaus.getParameter(2);
            
            //Set initial parameters for the total fit function ("fdouble_peak")
            fdouble_peak.setParameter(0, gaus_amp);
            fdouble_peak.setParameter(1, gaus_mean);
            fdouble_peak.setParameter(2, gaus_sigma);
            fdouble_peak.setParameter(3, second_gauss_amp_factor);
            fdouble_peak.setParameter(4, peak_separation);
            fdouble_peak.setParameter(5, third_gauss_amp_factor);
            fdouble_peak.setParameter(6, third_gauss_mean_offset);
            fdouble_peak.setParameter(7, third_gauss_sigma_factor);
            fdouble_peak.setParameter(8, bckgr_p0);
            fdouble_peak.setParameter(9, bckgr_p1);
            fdouble_peak.setParameter(10, bckgr_p2);
            
            //Set parameter bounds for total fit function ("fdouble_peak")
            fdouble_peak.setParLimits(0, amp*0.5,   amp*1.5);
            fdouble_peak.setParLimits(1, mean*0.9,  mean*1.1);            
            fdouble_peak.setParLimits(2, sigma*0.2, sigma*1.5);
            fdouble_peak.setParLimits(3, second_gauss_amp_factor-2, second_gauss_amp_factor+1);
            fdouble_peak.setParLimits(4, peak_separation*0.9, peak_separation*1.1);
            fdouble_peak.setParLimits(5, third_gauss_amp_factor-2, third_gauss_amp_factor+2);
            fdouble_peak.setParLimits(6, third_gauss_mean_offset-1, third_gauss_mean_offset+2);
            fdouble_peak.setParLimits(7, third_gauss_sigma_factor-0.5, third_gauss_sigma_factor+1);
            
            DataFitter.fit(fdouble_peak, histo, "Q");

            return true;
        }
        else { 
            histo.setFunction(fdouble_peak);
            return false;
        }
    }
    
    /**
     * Single Gaussian+background vertex fit function
     * @param histo
     * @return
     */
    public static boolean fit1Vertex(H1F histo) {
        //double mean  = Histo.getMeanIDataSet(histo, histo.getMean()-histo.getRMS(), 
        //                                            histo.getMean()+histo.getRMS());
        double mean = histo.getDataX(histo.getMaximumBin());
        double amp   = histo.getBinContent(histo.getMaximumBin());
        double rms   = histo.getRMS();
        double sigma = 0.5;
        double min = histo.getDataX(0);
        double max = histo.getDataX(histo.getDataSize(0)-1);
        int nbin = histo.getData().length;
        
        F1D f1   = new F1D("f"+histo.getName(),"[amp]*gaus(x,[mean],[sigma])+[p0]+[p1]*x+[p2]*x*x", min, max);
        f1.setLineColor(2);
        f1.setLineWidth(2);
        f1.setOptStat("1111");
        
        //Fit a stand-alone gaussian to peak to get initial parameters for total fit function
        //This is not plotted
        F1D f1_gaus   = new F1D("f1gaus","[amp]*gaus(x,[mean],[sigma])", min, max);
        f1_gaus.setParameter(0, amp);
        f1_gaus.setParameter(1, mean);
        f1_gaus.setParameter(2, sigma);
            
        if(amp>5) {
            f1_gaus.setParLimits(0, amp*0.2,   amp*1.2);
            f1_gaus.setParLimits(1, mean*0.9,  mean*1.1);
            f1_gaus.setParLimits(2, sigma*0.2, sigma*2.5);
            DataFitter.fit(f1_gaus, histo, "Q");
            
            double gaus_amp = f1_gaus.getParameter(0);
            double gaus_mean = f1_gaus.getParameter(1);
            double gaus_sigma = f1_gaus.getParameter(2);
            
            //Use parameters from stand-alone gaussian fit to initilaize the gaussian+background fit function parameters
            f1.setParameter(0, gaus_amp);
            f1.setParameter(1, gaus_mean);
            f1.setParameter(2, gaus_sigma);
            
            //f1.setParLimits(0, gaus_amp*0.2,   gaus_amp*1.2);
            f1.setParLimits(1, gaus_mean*0.9,  gaus_mean*1.1);
            f1.setParLimits(2, gaus_sigma*0.2, gaus_sigma*2.5);
            DataFitter.fit(f1, histo, "Q");
            int npar = f1.getNPars();
            double red_chi2 = f1.getChiSquare() / (nbin - npar);
            
            return true;         
        }
        else {
            histo.setFunction(f1);
            return false;
        }
    }    

    /**
     * 3-peaks vertex fitting function
     * Peaks correspond to: target windows and downstream insulation foil
     * Initialized according to:
     * - chosen target length (TARGETLENGTH), 
     * - target exit window position (TARGETPOS)
     * - distance between target exit window and insulation foil (WINDOWDIST)
     * Includes a wide Gaussian to account for target residual gas
     * @param histo
     */
    public static void fit3Vertex(H1F histo) {
        int nbin = histo.getData().length;
        double dx = histo.getDataX(1)-histo.getDataX(0);
        //find downstream window
        int ibin0 = Histo.getMaximumBinBetween(histo, histo.getDataX(0), (Constants.TARGETPOS+Constants.SCEXIT)/2);
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
        
        String function = "[amp]*gaus(x,[exw]-[tl],[sigma])+"
                        + "[amp]*gaus(x,[exw],[sigma])+"
                        + "[amp]*gaus(x,[exw]+[wd],[sigma])/1.8+"
                        + "[bg]*gaus(x,[exw]-[tl]/2,[tl]*0.8)";
        F1D f1_vtx   = new F1D("f"+histo.getName(), function, -10, 10);
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
        f1_vtx.setRange(mean-Constants.TARGETLENGTH*1.5,mean+Constants.WINDOWDIST+Constants.TARGETLENGTH/2);
        DataFitter.fit(f1_vtx, histo, "Q"); //No options uses error for sigma
    }

     /**
     * 4-peaks vertex fitting function
     * Peaks correspond to: target windows, downstream insulation foil and scattering chamber exit window
     * Initialized according to:
     * - chosen target length (TARGETLENGTH), 
     * - target exit window position (TARGETPOS)
     * - distance between target exit window and insulation foil (WINDOWDIST)
     * - distance between the scattering chamber exit window and the target center (SCEXIT)
     * Includes a wide Gaussian and a Landau to account for target residual gas 
     * and the air outside the scattering chamber
     * @param histo
     */
    public static void fit4Vertex(H1F histo) {
        int nbin = histo.getData().length;
        double dx = histo.getDataX(1)-histo.getDataX(0);
        //find downstream window
        int ibin0 = Histo.getMaximumBinBetween(histo, histo.getDataX(0), (Constants.TARGETPOS+Constants.SCEXIT)/2);
        //check if the found maximum is the first or second peak, ibin is tentative upstream window
        int ibin1 = Math.max(0, ibin0 - (int)(Constants.TARGETLENGTH/dx));
        int ibin2 = Math.min(nbin-1, ibin0 + (int)(Constants.TARGETLENGTH/dx));
        if(histo.getBinContent(ibin1)<histo.getBinContent(ibin2)) {
            ibin1 = ibin0;
            ibin0 = ibin2;
        }
        int ibinsc = Histo.getMaximumBinBetween(histo, (Constants.SCEXIT-Constants.TARGETPOS)*0.9, (Constants.SCEXIT-Constants.TARGETPOS)*1.1);
        
        double mean  = histo.getDataX(ibin0);
        double amp   = histo.getBinContent(ibin0);
        double sc    = histo.getBinContent(ibinsc);
        double sigma = 0.5;
        double bg = histo.getBinContent((ibin1+ibin0)/2);
        String function = "[amp]*gaus(x,[exw]-[tl],[sigma])+"
                        + "[amp]*gaus(x,[exw],[sigma])+"
                        + "[amp]*gaus(x,[exw]+[wd],[sigma])/1.8+"
                        + "[bg]*gaus(x,[exw]-[tl]/2,[tl]*0.6)+"
                        + "[sc]*gaus(x,[exw]+[scw]-[tl]/2,[sigma])+"
                        + "[air]*landau(x,[exw]+[scw]-[tl]/2+[sigma]*2,[sigma]*4)";
        F1D f1_vtx   = new F1D("f"+histo.getName(), function, -10, 10);
        f1_vtx.setLineColor(2);
        f1_vtx.setLineWidth(2);
        f1_vtx.setOptStat("11111111111");
        f1_vtx.setParameter(0, amp/2);
        f1_vtx.setParameter(1, mean);
        f1_vtx.setParameter(2, Constants.TARGETLENGTH);
        f1_vtx.setParLimits(2, Constants.TARGETLENGTH*0.99, Constants.TARGETLENGTH*1.01);
        f1_vtx.setParameter(3, sigma);
        f1_vtx.setParameter(4, Constants.WINDOWDIST);
        f1_vtx.setParLimits(4, Constants.WINDOWDIST*0.9, Constants.WINDOWDIST*1.1);
        f1_vtx.setParameter(5, bg);
        f1_vtx.setParameter(6, sc);
        f1_vtx.setParameter(7, Constants.SCEXIT-Constants.TARGETPOS);
        f1_vtx.setParLimits(7, (Constants.SCEXIT-Constants.TARGETPOS)*0.9, (Constants.SCEXIT-Constants.TARGETPOS)*1.1);
        f1_vtx.setRange(mean-Constants.TARGETLENGTH*1.5,Constants.SCEXIT+Constants.TARGETLENGTH*0.6);
        DataFitter.fit(f1_vtx, histo, "Q"); //No options uses error for sigma
        if(f1_vtx.getParameter(6)<f1_vtx.getParameter(0)/4) f1_vtx.setParameter(6, 0);
    }

     /**
     * RG-C vertex fitting function
     * Peaks correspond to: target windows, downstream insulation foil and cryostat exit window
     * Initialized according to:
     * - chosen target length (TARGETLENGTH), 
     * - target exit window position (TARGETPOS)
     * - distance between target exit window and insulation foil (WINDOWDIST)
     * - distance between the cryostat exit window and the target center (SCEXIT)
     * Includes a wide Gaussian and a Landau to account for target residual gas 
     * and the air outside the scattering chamber
     * @param histo
     */
    public static void fitRGCVertex(H1F histo) {
        int nbin = histo.getData().length;
        double dx = histo.getDataX(1)-histo.getDataX(0);
    
        // find heat shield and scattering chamber exit window
        // assume the maximum is the scattering chamber exit window
        int ibinsc  = histo.getMaximumBin();
        int deltasc = (int) ((Constants.SCEXIT-Constants.TARGETLENGTH/2-Constants.WINDOWDIST)/dx);
        // look for the heat shield
	int ibinhs1 = ibinsc - deltasc;
	int ibinhs2 = ibinsc + deltasc;
        int ibinhs = ibinhs1;
	if(histo.getBinContent(ibinhs1)<histo.getBinContent(ibinhs2)) {
            ibinhs = ibinsc;
            ibinsc = ibinhs2;
        }

        //find downstream window relying on distance from the scattering chamber exit window
        double center = histo.getDataX(ibinsc) - Constants.SCEXIT;
	int ibin0 = Histo.getMaximumBinBetween(histo, center, center + Constants.TARGETLENGTH);
	int ibin1 = Histo.getMaximumBinBetween(histo, center - Constants.TARGETLENGTH, center);
        
        double mean  = histo.getDataX(ibin0);
        double amp   = histo.getBinContent(ibin0);
        double sc    = histo.getBinContent(ibinsc);
        double sigma = 0.8;
        double bg    = histo.getBinContent((ibin1+ibin0)/2);
	double air = 0;
        String function = "[amp]*gaus(x,[exw]-[tl],[sigma])+"
                        + "[amp]*gaus(x,[exw],[sigma])+"
                        + "[bg]*gaus(x,[exw]-[tl]/4,[tl]*0.8)+"
                        + "gaus(x,[exw]+[wd],[sigma])*[sc]*1.4+"
                        + "[sc]*gaus(x,[exw]+[scw]-[tl]/2,[sigma])+"
                        + "[air]*landau(x,[exw]+[scw]-[tl]/2,[sigma]*5)";
        F1D f1_vtx   = new F1D("f"+histo.getName(), function, mean - Constants.TARGETLENGTH*2, mean + Constants.TARGETLENGTH/2 + Constants.SCEXIT);
        f1_vtx.setLineColor(2);
        f1_vtx.setLineWidth(2);
        f1_vtx.setOptStat("11111111111");
	f1_vtx.setParameter(0, amp/2);
        f1_vtx.setParameter(1, mean);
        f1_vtx.setParameter(2, Constants.TARGETLENGTH);
	f1_vtx.setParLimits(2, Constants.TARGETLENGTH*0.99, Constants.TARGETLENGTH*1.01); 
        f1_vtx.setParameter(3, sigma);
        f1_vtx.setParameter(4, bg);
        f1_vtx.setParameter(5, Constants.WINDOWDIST);
        f1_vtx.setParLimits(5, Constants.WINDOWDIST*0.8, Constants.WINDOWDIST*1.2);
        f1_vtx.setParameter(6, sc);
        f1_vtx.setParameter(7, Constants.SCEXIT);
	f1_vtx.setParLimits(7, Constants.SCEXIT*0.9, Constants.SCEXIT*1.1);
        f1_vtx.setParameter(8, air);
        DataFitter.fit(f1_vtx, histo, "Q"); //No options uses error for sigma
        if(f1_vtx.getParameter(6)<f1_vtx.getParameter(0)/4) f1_vtx.setParameter(6, 0);
    }

     /**
     * 3-peaks vertex fitting function
     * Peaks correspond to: target windows and scattering chamber exit window
     * Initialized according to:
     * - chosen target length (TARGETLENGTH), 
     * - target exit window position (TARGETPOS)
     * - distance between target exit window and insulation foil (WINDOWDIST)
     * - distance between the scattering chamber exit window and the target center (SCEXIT)
     * Includes a wide Gaussian and a Landau to account for target residual gas 
     * and the air outside the scattering chamber
     * @param histo
     */
    public static void fitRGDVertex(H1F histo) {
        int nbin = histo.getData().length;
        double dx = histo.getDataX(1)-histo.getDataX(0);
        //find downstream window
        int ibin0 = Histo.getMaximumBinBetween(histo, histo.getDataX(0), (Constants.TARGETPOS+Constants.SCEXIT)/2);
        //check if the found maximum is the first or second peak, ibin is tentative upstream window
        int ibin1 = Math.max(0, ibin0 - (int)(Constants.TARGETLENGTH/dx));
        int ibin2 = Math.min(nbin-1, ibin0 + (int)(Constants.TARGETLENGTH/dx));
        if(histo.getBinContent(ibin1)<histo.getBinContent(ibin2)) {
            ibin1 = ibin0;
            ibin0 = ibin2;
        }
        int ibinsc = Histo.getMaximumBinBetween(histo, (Constants.SCEXIT+Constants.TARGETCENTER)*0.8, (Constants.SCEXIT+Constants.TARGETCENTER)*1.2);
        
        double mean  = histo.getDataX(ibin0);
        double amp   = histo.getBinContent(ibin0);
        double sc    = histo.getBinContent(ibinsc);
        double scw   = Constants.SCEXIT;
        if(sc>10) scw = histo.getDataX(ibinsc)-mean+Constants.TARGETLENGTH/2;
        double sigma = 0.5;
        double bg = histo.getBinContent((ibin1+ibin0)/2);
        String function = "[ampU]*gaus(x,[exw]-[tl],[sigma])+"
                        + "[ampD]*gaus(x,[exw],[sigma])+"
                        + "[bg]*gaus(x,[exw]-[tl]/2,[tl]*0.6)+"
                        + "[sc]*gaus(x,[exw]+[scw]-[tl]/2,[sigma])+"
                        + "[air]*landau(x,[exw]+[scw]-[tl]/2+[sigma]*2,[sigma]*4)";
        F1D f1_vtx   = new F1D("f"+histo.getName(), function, -10, 10);
        f1_vtx.setLineColor(2);
        f1_vtx.setLineWidth(2);
        f1_vtx.setOptStat("11111111111");
        f1_vtx.setParameter(0, amp);
        f1_vtx.setParameter(1, mean);
        f1_vtx.setParameter(2, Constants.TARGETLENGTH);
        f1_vtx.setParLimits(2, Constants.TARGETLENGTH*0.9, Constants.TARGETLENGTH*1.1);
        f1_vtx.setParameter(3, sigma);
        f1_vtx.setParameter(4, amp);        
        f1_vtx.setParameter(5, bg);
        f1_vtx.setParameter(6, sc);
        f1_vtx.setParameter(7, scw);
        f1_vtx.setParLimits(7, (Constants.SCEXIT)*0.7, (Constants.SCEXIT)*1.3);
        f1_vtx.setRange(mean-Constants.TARGETLENGTH*1.5,Constants.SCEXIT+Constants.TARGETLENGTH*0.6);
        DataFitter.fit(f1_vtx, histo, "Q"); //No options uses error for sigma
//        if(f1_vtx.getParameter(6)<f1_vtx.getParameter(0)/4) f1_vtx.setParameter(6, 0);
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
            F1D f1_vtx   = new F1D("f"+histo.getName(),"[amp]*gaus(x,[mean],[sigma])", -10, 10);
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
            F1D f2_vtx   = new F1D("f"+histo.getName(),"[amp2]*gaus(x,[mean2],[sigma2])", -10, 10);
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

    private GraphErrors fitOffset(GraphErrors grTheta, double deltaZ) {
        GraphErrors grOffset = new GraphErrors("R2");
        grOffset.setTitleX("#phi(deg)");
        grOffset.setTitleY("r(cm)");
        double phimin = 0;
        for(int i=0; i<grTheta.getDataSize(0); i++) {
            double phi     = grTheta.getDataX(i);
            double thetasc = grTheta.getDataY(i);
            double errthsc = grTheta.getDataEY(i);
            double R       = deltaZ*Math.tan((Math.toRadians(thetasc)));
            grOffset.addPoint(phi, R, 0, 0.1);
            if(R<=grOffset.getMin()) phimin=phi;
        }
        F1D f1 = new F1D("f1","sqrt([R]*[R]+[d0]*[d0]-2*[R]*[d0]*cos((x-[phi0])*" + Math.PI/180 + "))", -180, 180);
        f1.setParameter(0, (grOffset.getMax()+grOffset.getMin())/2.0);
        f1.setParameter(1, (grOffset.getMax()-grOffset.getMin())/(grOffset.getMax()+grOffset.getMin()));
        f1.setParameter(2, phimin);
        DataFitter.fit(f1, grOffset, "Q");
        grOffset.getFunction().setOptStat("11111");
        grOffset.getFunction().setLineColor(2);
        grOffset.getFunction().setLineWidth(2);
        return grOffset;
    }
    
    public static int getMaximumBinBetween(H1F histo, double min, double max) { 
        int nbin = histo.getData().length;
        double x_val_temp;
        double x_val;
        double y_max_temp;
        double y_max = 0;
        int max_bin_num = histo.getMaximumBin();
        for (int i = 0; i < nbin; i++) { 
            x_val_temp = histo.getAxis().getBinCenter(i);
            if (x_val_temp >= min && x_val_temp <= max) {
                y_max_temp = histo.getBinContent(i);
                if (y_max_temp > y_max) {
                    y_max = y_max_temp;
                    max_bin_num = i;
                }
            }
        }
        return max_bin_num;
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
   
    private static GraphErrors getThresholdCrossingProfile(H2F hi, double fraction) {
        GraphErrors graph = new GraphErrors();
        graph.setTitleX(hi.getTitleX());
        graph.setTitleY(hi.getTitleY());
        List<H1F> hslices = hi.getSlicesX();
        for(int ix=0; ix<hi.getDataSize(0); ix++) {
            H1F hix = hslices.get(ix);
            if(hix.getMax()>10) {
                graph.addPoint(hi.getDataX(ix), Histo.getThresholdCrossing(hix, fraction), 0, (hix.getDataX(1)-hix.getDataX(0))/2);
            }
        }
        return graph;
    }
    
    private static double getThresholdCrossing(H1F hi, double fraction) {
        if(fraction<0 || fraction>=1) {
            System.out.println("[ERROR] Invalid constant fraction threshold " + fraction + " for histogram " + hi.getName());
            System.exit(1);
        }
        double hiMax = hi.getMax();
        int iHalf = -1;
        for(int i=0; i<hi.getDataSize(0); i++) {
            if(hi.getDataY(i)>hiMax*fraction) {
                iHalf = i;
                break;
            }
        }
        return hi.getDataX(iHalf);
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
        electron = this.readDataGroup(folder + "/electron/electron", dir, electron);
        binning  = this.readDataGroup(folder + "/electron/binning", dir, binning);
        offset   = this.readDataGroup(folder + "/electron/offset", dir, offset);
        calib    = this.readDataGroup(folder + "/calibration/residuals", dir, calib);
        for(int is=0; is<nSector; is++) {
            for(int it=0; it<thetaBins.length; it++) {
                for(int ip=0; ip<phiBins.length; ip++) {
                    String subfolder = folder + "/residuals/sec" + (is+1) + "_theta" + thetaBins[it].getRange() + "_phi" + phiBins[ip].getRange();
                    residuals[is][it][ip]=this.readDataGroup(subfolder, dir, residuals[is][it][ip]);
                }
            }
        }
        if(tres) {
            for(int is=0; is<nSector; is++) {
                String subfolder = folder + "/time/sec" + (is+1);
                wires[is] = this.readDataGroup(subfolder, dir, wires[is]);
            }
            for(int is=0; is<nSector; is++) {
                for(int it=0; it<thetaBins.length; it++) {
                    for(int ip=0; ip<phiBins.length; ip++) {
                        String subfolder = folder + "/time/sec" + (is+1) + "_theta" + thetaBins[it].getRange() + "_phi" + phiBins[ip].getRange();
                        time[is][it][ip]=this.readDataGroup(subfolder, dir, time[is][it][ip]);
                    }
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
                    IDataSet dsread = dir.getObject(folder, ds.getName());
                    if(dsread instanceof H1F) {
                        H1F h1 = (H1F) dsread;
                        Func1D f1 = (Func1D) dir.getObject(folder, "f"+h1.getName());
                        if(f1!=null)
                            h1.setFunction(f1);
                    }
                    if(dsread instanceof H1F && ((H1F) dsread).getFunction()!=null) {
                        Func1D dsf = ((H1F) dsread).getFunction();
                        dsf.setLineColor(2);
                        dsf.setLineWidth(2);
                        String opts = "11";
                        for(int k=0; k<dsf.getNPars(); k++) opts += "1";
                        dsf.setOptStat(opts);
                    }
                    else if(dsread instanceof F1D && dg.getF1D(dsread.getName())!=null) {
                        F1D dgf = (F1D) dg.getF1D(dsread.getName());
                        F1D dsf = (F1D) dsread;
                        dsf.setLineColor(dgf.getLineColor());
                        dsf.setLineWidth(dgf.getLineWidth());
                        dsf.setOptStat(dgf.getOptStat());
                    }
                    newGroup.addDataSet(dsread,i);                        
                }
            }
        }
        return newGroup;
    }
    
    public void writeDataGroup(String root, String folder, TDirectory dir) {
        dir.cd("/" + root);
        dir.mkdir(folder);
        dir.cd(folder);
        dir.mkdir("electron");
        dir.cd("electron");
        this.writeDataGroup("electron", dir, electron);
        dir.cd("/" + root + "/" + folder + "/electron");
        this.writeDataGroup("binning", dir,  binning);
        dir.cd("/" + root + "/" + folder + "/electron");
        this.writeDataGroup("offset", dir,  offset);
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
        dir.mkdir("calibration");
        dir.cd("calibration");
        this.writeDataGroup("residuals", dir,  calib);
        if(tres) {
            dir.cd("/" + root + "/" + folder);
            dir.mkdir("time");
            dir.cd("time");
            for(int is=0; is<nSector; is++) {
                String subfolder = "sec" + (is+1);
                this.writeDataGroup(subfolder, dir,  wires[is]);
                dir.cd("/" + root + "/" + folder + "/time");
            }
            for(int is=0; is<nSector; is++) {
                for(int it=0; it<thetaBins.length; it++) {
                    for(int ip=0; ip<phiBins.length; ip++) {
                        String subfolder = "sec" + (is+1) + "_theta" + thetaBins[it].getRange() + "_phi" + phiBins[ip].getRange();
                        this.writeDataGroup(subfolder, dir, time[is][it][ip]);
                        dir.cd("/" + root + "/" + folder + "/time");
                    }
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
                if(ds instanceof H1F) {
                    H1F h1 = (H1F) ds;
                    if(h1.getFunction()!=null) {
                        Func1D f1 = h1.getFunction();
                        f1.setName("f"+h1.getName());
                        dir.addDataSet(f1);
                    }
                }
            }
        }
    }
    
}
