package org.clas.dc.alignment;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import org.jlab.clas.pdg.PhysicsConstants;
import org.jlab.clas.physics.LorentzVector;
import org.jlab.clas.physics.Particle;
import org.jlab.clas.physics.PhysicsEvent;
import org.jlab.groot.base.GStyle;
import org.jlab.groot.data.DataLine;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.data.IDataSet;
import org.jlab.groot.data.TDirectory;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.graphics.EmbeddedCanvasTabbed;
import org.jlab.groot.graphics.EmbeddedPad;
import org.jlab.groot.graphics.IDataSetPlotter;
import org.jlab.groot.group.DataGroup;
import org.jlab.groot.math.F1D;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.jnp.utils.benchmark.ProgressPrintout;
import org.jlab.jnp.utils.options.OptionStore;

/**
 *
 * @author devita
 */
public class Kinematics {
    
    private double ebeam = 10.6;
    private double targetPos = -3.5;
    private double targetLength = 5;
    private double scWindow = 27+targetPos;
    private String fontName = "Arial";
    private File lundfile = null;

    private Map<String, DataGroup> histos = new LinkedHashMap<>();
    
    
    public Kinematics(double energy) {
        this.initGraphics();
        this.setEbeam(energy);
    }

    public double getEbeam() {
        return ebeam;
    }

    public void setEbeam(double ebeam) {
        this.ebeam = ebeam;
    }
    
    public double getMaxQ2() {
        double maxQ2 = 3;
        if(ebeam>6) maxQ2 = 5;
        return maxQ2;
    }
    
    public double getMaxW() {
        double maxW = ebeam*0.4+0.6;
        if(ebeam>6.5) maxW = 4.5;
        return maxW;
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
        
    public void createHistos() {
        // General
        H2F hi_q2w = new H2F("hi_q2w","hi_q2w",100, 0.6, this.getMaxW(), 100, 0.0, this.getMaxQ2()); 
        hi_q2w.setTitleX("W (GeV)");
        hi_q2w.setTitleY("Q2 (GeV2)");
        H1F hi_w = new H1F("hi_w","hi_w",500, 0.6, this.getMaxW()); 
        hi_w.setTitleX("W (GeV)");
        hi_w.setTitleY("Counts");
        H2F hi_w_phi = new H2F("hi_w_phi","hi_w_phi",100, -180.0, 180.0, 250, 0.6, this.getMaxW()); 
        hi_w_phi.setTitleX("#phi (deg)");
        hi_w_phi.setTitleY("W (GeV)");
        H2F hi_z_phi = new H2F("hi_z_phi","hi_z_phi",100, -180.0, 180.0, 100, 20, 30); 
        hi_z_phi.setTitleX("#phi (deg)");
        hi_z_phi.setTitleY("z (cm)");
        H2F hi_w_theta = new H2F("hi_w_theta","hi_w_theta",100, 5.0, 15.0, 250, 0.6, this.getMaxW()); 
        hi_w_theta.setTitleX("#theta (deg)");
        hi_w_theta.setTitleY("W (GeV)");
        H2F hi_el = new H2F("hi_el","hi_el",100, 0.5, ebeam+0.5, 100, 0.0, 35.0);
        hi_el.setTitleX("p (GeV)");
        hi_el.setTitleY("#theta (deg)");
        hi_el.setTitle("Electron");
        F1D f1_el = new F1D("f1_el", "2*(180/3.14)*atan(sqrt(0.93832*([e0]-x)/2/[e0]/x))", ebeam*0.75, ebeam*0.99);
        f1_el.setParameter(0, ebeam);
        DataGroup dg_general = new DataGroup(3,2);
        dg_general.addDataSet(hi_q2w,   0);
        dg_general.addDataSet(hi_w_theta, 1);
        dg_general.addDataSet(hi_el,    2);
        dg_general.addDataSet(f1_el,        2);  
        dg_general.addDataSet(hi_w,     3);
        dg_general.addDataSet(hi_w_phi, 4);
        dg_general.addDataSet(hi_z_phi, 5);
        histos.put("General", dg_general);
        // Elastic
        H1F hi_ela = new H1F("hi_w","hi_w",500, 0.6, 1.3); 
        hi_ela.setTitleX("W (GeV)");
        hi_ela.setTitleY("Counts");
        H2F hi_ela_phi = new H2F("hi_w_phi","hi_w_phi",100, -180.0, 180.0, 250, 0.6, 1.3); 
        hi_ela_phi.setTitleX("#phi (deg)");
        hi_ela_phi.setTitleY("W (GeV)");
        H2F hi_ela_theta = new H2F("hi_w_theta","hi_w_theta",100, 5.0, 15.0, 250, 0.6, 1.3); 
        hi_ela_theta.setTitleX("#theta (deg)");
        hi_ela_theta.setTitleY("W (GeV)");
        H2F hi_ela_p = new H2F("hi_w_p","hi_w_p",100, ebeam*0.8, ebeam, 100, 0.6, 1.3);
        hi_ela_p.setTitleX("p (GeV)");
        hi_ela_p.setTitleY("W (GeV)");
        hi_ela_p.setTitle("Electron");
        DataGroup dg_elastic = new DataGroup(2,2);
        dg_elastic.addDataSet(hi_ela, 0);
        dg_elastic.addDataSet(hi_ela_p,    1);
        dg_elastic.addDataSet(hi_ela_theta,        2);  
        dg_elastic.addDataSet(hi_ela_phi,     3);
        histos.put("Elastic", dg_elastic);
        // vertex
        String[] names= ["pi+", "pi-", "el"]; 
        DataGroup dg_vertex = new DataGroup(3, 4);
        for(int i=0; i<names.length; i++) {
            dg_vertex.addDataSet(new H1F("h1_vz_"+names[i],"vz (cm)","Counts",100,-20,30),0+i);
            dg_vertex.addDataSet(new H2F("h2_theta_"+names[i],names[i],100,-20,30, 100, 5, 45),3+i);
            dg_vertex.addDataSet(new H2F("h2_phi_"+names[i],names[i], 100,-20,30, 100, -180, 180),6+i);
            dg_vertex.addDataSet(new H2F("h2_p_"+names[i],names[i], 100,-20,30, 100, 0.5, 8),9+i);
            dg_vertex.getH1F("h1_vz_"+names[i]).setTitle(names[i]);
            dg_vertex.getH2F("h2_theta_"+names[i]).setTitleX("vz (cm)");
            dg_vertex.getH2F("h2_theta_"+names[i]).setTitleY("#theta (deg)");
            dg_vertex.getH2F("h2_phi_"+names[i]).setTitleX("vz (cm)");
            dg_vertex.getH2F("h2_phi_"+names[i]).setTitleY("#phi (deg)");
            dg_vertex.getH2F("h2_p_"+names[i]).setTitleX("vz (cm)");
            dg_vertex.getH2F("h2_p_"+names[i]).setTitleY("p (GeV)");
        }
        histos.put("Vertex", dg_vertex);
        // W
        DataGroup dg_w = new DataGroup(2,3);
        for(int sector=1; sector <= 6; sector++) {
            H1F hi_w_sec = new H1F("hi_w_" + sector, "hi_w_" + sector, 500, 0.6, this.getMaxW());  
            hi_w_sec.setTitleX("W (GeV)");
            hi_w_sec.setTitleY("Counts");
            hi_w_sec.setTitle("Sector " + sector);
            F1D f1_w_sec = new F1D("f1_w_" + sector, "[amp]*gaus(x,[mean],[sigma])", 0.8, 1.2);
            f1_w_sec.setParameter(0, 0);
            f1_w_sec.setParameter(1, 1);
            f1_w_sec.setParameter(2, 0.2);
            f1_w_sec.setLineWidth(2);
            f1_w_sec.setLineColor(2);
            f1_w_sec.setOptStat("1111");
            dg_w.addDataSet(hi_w_sec, sector-1);
//            dg_w.addDataSet(f1_w_sec, sector-1);
        }  
        histos.put("W", dg_w);
        // 2 pi
        DataGroup dg_2pi = new DataGroup(3,2);
        String[] a2pimx = ["M_X(ep#rarrow e'^#pi+^#pi-X) (GeV)","M_^X2(ep#rarrow e'p^#pi+X) (Ge^V2)","M_^X2(ep#rarrow e'p^#pi-X) (Ge^V2)"];
        for(int i=1; i<=3; i++) {
            double rmin = 0.5;
            double rmax = 2;
            if(i>1) rmin = -0.5;
            H1F hi_mmass = new H1F("mxt" + i, "", 200, rmin, rmax);     
            hi_mmass.setTitleX(a2pimx[i-1]);
            hi_mmass.setTitleY("Counts");
        //    hi_mmass.setLineColor(col);
            H1F hi_imass = new H1F("mit" + i, "", 200, 0.0, 3.0);     
            hi_imass.setTitleX("M(#pi#pi) (GeV)");
            hi_imass.setTitleY("Counts");
        //    hi_imass.setLineColor(col);
            dg_2pi.addDataSet(hi_mmass, i-1);
            dg_2pi.addDataSet(hi_imass, i+2);
        }
        histos.put("2pi", dg_2pi);
        // 1 pi
        DataGroup dg_1pi = new DataGroup(2,2);
        String[] atitle = ["M_X(ep#rarrow e'^#pi+X) (GeV)","M_^X2(ep#rarrow e'pX) (Ge^V2)"];
        for(int i=1; i<=2; i++) {
            double rmin = 0.5;
            double rmax = 2;
            if(i>1) rmin = -0.5;
            H2F hi_mw = new H2F("W" + i, "", 100, -180, 180, 100, rmin, rmax);     
            hi_mw.setTitleX("W (GeV)");
            hi_mw.setTitleY(atitle[i-1]);
        //    hi_w.setLineColor(col);
            H1F hi_mmass = new H1F("mxt" + i, "", 400, rmin, rmax);      
            hi_mmass.setTitleX(atitle[i-1]);
            hi_mmass.setTitleY("Counts");
        //    hi_mmass.setLineColor(col);
            dg_1pi.addDataSet(hi_mw,    i-1);
            dg_1pi.addDataSet(hi_mmass, i+1);
        }
        histos.put("1pi", dg_1pi);
        // Proton
        DataGroup dg_proton = new DataGroup(4,2);
        H2F hi_pr_p_dp     = new H2F("hi_p_dp", "", 100, 1, 4, 100, ebeam*0.75, ebeam*1.2);
        hi_pr_p_dp.setTitleX("p (GeV)"); hi_pr_p_dp.setTitleY("Ebeam (GeV)");
        H2F hi_pr = new H2F("hi_pr","hi_pr",100, 0, ebeam/2-0.1, 100, 20.0, 80.0);
        hi_pr.setTitleX("p (GeV)");
        hi_pr.setTitleY("#theta (deg)");
        hi_pr.setTitle("Proton");       
        F1D f1_pr = new F1D("f1_pr", "(180/3.14)*acos(([e0]*[e0]+x*x-pow(([e0]+0.93832-sqrt(x*x+0.9382*0.9382)),2))/2/[e0]/x)", ebeam*0.08, ebeam*0.4);
        f1_pr.setParameter(0, ebeam);
        H2F hi_phi = new H2F("hi_phi", "hi_phi", 200, -180, 180, 200, -180, 180);   
        hi_phi.setTitleX("El #phi (deg)");
        hi_phi.setTitleY("Pr #phi (deg)");
        //        hi_dphi.setOptStat("1110");
        H2F hi_dphi = new H2F("hi_dphi", "hi_dphi", 200, -180, 180, 200, -10, 10);   
        hi_dphi.setTitleX("Pr #phi (deg)");
        hi_dphi.setTitleY("#Delta#phi (deg)");
        H2F hi_dpr = new H2F("hi_dpr", "hi_dpr", 200, -180, 180, 200, -10, 10);   
        hi_dpr.setTitleX("Pr #phi (deg)");
        hi_dpr.setTitleY("#Delta p (GeV)");
        H2F hi_pr_phi_dphi = new H2F("hi_phi_dphi", "", 100, -180.0, 180.0, 100, 170.0, 190.0);  
        hi_pr_phi_dphi.setTitleX("#phi (deg)"); hi_pr_phi_dphi.setTitleY("#Delta#phi (deg)");
        H2F hi_pr_phi_dz = new H2F("hi_phi_dz", "", 100, -180.0, 180.0, 100, -5.0, 5.0);  
        hi_pr_phi_dz.setTitleX("#phi (deg)"); hi_pr_phi_dz.setTitleY("#Deltaz (cm)");
        H1F hi_pr_dp       = new H1F("hi_dp", "Ebeam (GeV)", "Counts", 100, ebeam*0.75, ebeam*1.2);  
        H1F hi_pr_dtheta   = new H1F("hi_dtheta", "#Delta#theta (deg)", "Counts", 100, -15.0, 15.0);  
        H1F hi_pr_dphi     = new H1F("hi_dphi", "#Delta#phi (deg)", "Counts", 100, 170.0, 190.0);  
        H1F hi_pr_dz       = new H1F("hi_dz", "#Deltaz (cm)", "Counts", 100, -5.0, 5.0);  
        dg_proton.addDataSet(hi_pr_p_dp,     0);  
        dg_proton.addDataSet(hi_pr,      1);  
        dg_proton.addDataSet(f1_pr,          1);
        dg_proton.addDataSet(hi_pr_phi_dphi, 2);
        dg_proton.addDataSet(hi_pr_phi_dz,   3);
        dg_proton.addDataSet(hi_pr_dp,       4);
        dg_proton.addDataSet(hi_pr_dtheta,   5);
        dg_proton.addDataSet(hi_pr_dphi,     6);
        dg_proton.addDataSet(hi_pr_dz,       7);
        histos.put("Proton", dg_proton);
        // Phi
        DataGroup dg_phi = new DataGroup(2,3);
        for(int sector=1; sector <= 6; sector++) {
            H1F hi_dphi_sec = new H1F("hi_dphi_" + sector, "hi_dphi_" + sector, 100, 160.0, 200.0);  
            hi_dphi_sec.setTitleX("#Delta#phi (deg)");
            hi_dphi_sec.setTitleY("Counts");
            hi_dphi_sec.setTitle("Sector " + sector);
            dg_phi.addDataSet(hi_dphi_sec, sector-1);
        }
        histos.put("Phi", dg_phi);
        // Beam
        DataGroup dg_beam = new DataGroup(2,3);
        for(int sector=1; sector <= 6; sector++) {
            H1F hi_beam_sec = new H1F("hi_beam_" + sector, "hi_beam_" + sector, 100, ebeam*0.75, ebeam*1.2);  
            hi_beam_sec.setTitleX("Beam Energy (GeV)");
            hi_beam_sec.setTitleY("Counts");
            hi_beam_sec.setTitle("Sector " + sector);
            dg_beam.addDataSet(hi_beam_sec, sector-1);
        }
        histos.put("Beam", dg_beam);
    }

    public void processEvent(DataEvent event) {

        DataBank recEvent  = null;
        DataBank recPart   = null;
        DataBank recScint  = null;
        DataBank recCal    = null;
        DataBank recTracks = null;
        DataBank recTraj   = null;

        if(event.hasBank("REC::Event"))                recEvent = event.getBank("REC::Event");
        if(event.hasBank("REC::Particle"))              recPart = event.getBank("REC::Particle");
        if(event.hasBank("REC::Scintillator"))         recScint = event.getBank("REC::Scintillator");
        if(event.hasBank("REC::Calorimeter"))            recCal = event.getBank("REC::Calorimeter");
        if(event.hasBank("REC::Track"))               recTracks = event.getBank("REC::Track");
        if(event.hasBank("REC::Traj"))                  recTraj = event.getBank("REC::Traj");

        Particle recEl = null;
        Particle recPr = null;
        Particle recPip = null;
        Particle recPim = null;
        Particle recPro = null;
        LorentzVector virtualPhoton  = null;
        LorentzVector hadronSystem   = null;
        LorentzVector virtualPhotonP = null;
        LorentzVector hadronSystemP  = null;
        Particle beam = Particle.createWithPid(11, 0,0,ebeam, 0,0,0);
        Particle target = Particle.createWithPid(2212, 0,0,0, 0,0,0);
        if(event.hasBank("REC::Particle")==true && event.hasBank("REC::Track")){
            DataBank  bank  = event.getBank("REC::Particle");
            DataBank  track = event.getBank("REC::Track");
            int rows = bank.rows();
            for(int loop = 0; loop < rows; loop++){
                int status = (int) Math.floor(Math.abs(bank.getShort("status", loop))/1000); ///
                if(loop==0 && bank.getInt("pid", loop)==11 && status==2) {
                    recEl = new Particle(
                                bank.getInt("pid", loop),
                                bank.getFloat("px", loop),
                                bank.getFloat("py", loop),
                                bank.getFloat("pz", loop),
                                bank.getFloat("vx", loop),
                                bank.getFloat("vy", loop),
                                bank.getFloat("vz", loop));
                    for(int j=0; j<track.rows(); j++) {
                        if(track.getShort("pindex", j)==loop) recEl.setProperty("sector", (double) track.getByte("sector", j));
                    }
                }
                else if(bank.getInt("charge", loop)!=0 && status==2 && Math.abs(bank.getFloat("vz", loop)+3)<30 && Math.abs(bank.getFloat("chi2pid",loop))<5) {
                    Particle part = new Particle(
                                        bank.getInt("pid", loop),
                                        bank.getFloat("px", loop),
                                        bank.getFloat("py", loop),
                                        bank.getFloat("pz", loop),
                                        bank.getFloat("vx", loop),
                                        bank.getFloat("vy", loop),
                                        bank.getFloat("vz", loop));
                    if(part.pid()==211 && recPip==null) recPip=part;
                    else if(part.pid()==-211 && recPim==null) recPim=part;
                    else if(part.pid()==2212 && recPro==null) recPro=part;
                }
                else if(bank.getInt("charge", loop)==1 && recPr==null && status==2) {
                    recPr = new Particle(
                                2212,
                                bank.getFloat("px", loop),
                                bank.getFloat("py", loop),
                                bank.getFloat("pz", loop),
                                bank.getFloat("vx", loop),
                                bank.getFloat("vy", loop),
                                bank.getFloat("vz", loop));
                    for(int j=0; j<track.rows(); j++) {
                        if(track.getShort("pindex", j)==loop) recPr.setProperty("NDF", (double) track.getShort("NDF", j));
                    }
                }
            }
            if(recEl != null) {
                virtualPhoton = new LorentzVector(0.0, 0.0, ebeam, ebeam);
                virtualPhoton.sub(recEl.vector());
                hadronSystem = new LorentzVector(0.0, 0.0, ebeam, PhysicsConstants.massProton()+ebeam);
                hadronSystem.sub(recEl.vector());
                int secEl = (int) recEl.getProperty("sector");
                double phEl = Math.toDegrees(recEl.phi());
                if(Math.toDegrees(recEl.theta())>0){
                    histos.get("General").getH2F("hi_q2w").fill(hadronSystem.mass(),-virtualPhoton.mass2());
                    histos.get("General").getH1F("hi_w").fill(hadronSystem.mass());
                    histos.get("General").getH2F("hi_z_phi").fill(Math.toDegrees(recEl.phi()), recEl.vz());
                    histos.get("General").getH2F("hi_w_phi").fill(Math.toDegrees(recEl.phi()), hadronSystem.mass());
                    histos.get("General").getH2F("hi_w_theta").fill(Math.toDegrees(recEl.theta()), hadronSystem.mass());
                    histos.get("General").getH2F("hi_el").fill(recEl.p(),Math.toDegrees(recEl.theta()));
                    histos.get("Elastic").getH1F("hi_w").fill(hadronSystem.mass());
                    histos.get("Elastic").getH2F("hi_w_phi").fill(Math.toDegrees(recEl.phi()), hadronSystem.mass());
                    histos.get("Elastic").getH2F("hi_w_theta").fill(Math.toDegrees(recEl.theta()), hadronSystem.mass());
                    histos.get("Elastic").getH2F("hi_w_p").fill(recEl.p(),hadronSystem.mass());
                    histos.get("W").getH1F("hi_w_" + secEl).fill(hadronSystem.mass());
                    histos.get("Vertex").getH1F("h1_vz_el").fill(recEl.vz());
                    histos.get("Vertex").getH2F("h2_theta_el").fill(recEl.vz(), Math.toDegrees(recEl.theta()));
                    histos.get("Vertex").getH2F("h2_phi_el").fill(recEl.vz()), Math.toDegrees(recEl.phi());
                    histos.get("Vertex").getH2F("h2_p_el").fill(recEl.vz(), recEl.p());
                }
                if(hadronSystem.mass()<1.1) {
                    if(recPr != null) {
                        double phPr = Math.toDegrees(recPr.phi()); 
                        if(phPr < phEl) phPr +=360;
                        histos.get("Proton").getH2F("hi_pr").fill(recPr.p(),Math.toDegrees(recPr.theta()));
    //                    dg_proton.getH2F("hi_phi").fill(Math.toDegrees(recEl.phi()),Math.toDegrees(recPr.phi()));
    //                    dg_proton.getH2F("hi_dphi").fill(Math.toDegrees(recPr.phi()),phPr-phEl-180);
                        histos.get("Phi").getH1F("hi_dphi_" + secEl).fill(phPr-phEl);
                        histos.get("Proton").getH1F("hi_dphi").fill(phPr-phEl);
                        histos.get("Proton").getH2F("hi_phi_dphi").fill(Math.toDegrees(recPr.phi()),phPr-phEl);
                        histos.get("Proton").getH2F("hi_phi_dz").fill(Math.toDegrees(recPr.phi()),recPr.vz()-recEl.vz());
                        histos.get("Proton").getH1F("hi_dz").fill(recPr.vz()-recEl.vz());
                        if(Math.abs(phPr-phEl-180)<10  && recPr.getProperty("NDF")>=0) {
                            histos.get("Proton").getH2F("hi_p_dp").fill(recPr.p(),-0.93832+recPr.e()+recEl.p());
                            histos.get("Proton").getH1F("hi_dp").fill(-0.93832+recPr.e()+recEl.p());
                            histos.get("Proton").getH1F("hi_dtheta").fill(Math.toDegrees(recPr.theta())-histos.get("Proton").getF1D("f1_pr").evaluate(recPr.p()));
                            histos.get("Beam").getH1F("hi_beam_" + secEl).fill((-0.93832+recPr.e()+recEl.p()));
                            if(lundfile!=null) {
                                PhysicsEvent ev = writeToLund(ebeam, recEl);
                                if(ev!=null) lundfile << ev.toLundString();
                            }
                        }
                    }
                }
                if(recPip!=null) {
                    histos.get("Vertex").getH1F("h1_vz_pi+").fill(recPip.vz());
                    histos.get("Vertex").getH2F("h2_theta_pi+").fill(recPip.vz(), Math.toDegrees(recPip.theta()));
                    histos.get("Vertex").getH2F("h2_phi_pi+").fill(recPip.vz(), Math.toDegrees(recPip.phi()));
                    histos.get("Vertex").getH2F("h2_p_pi+").fill(recPip.vz(), recPip.p());
                }
                if(recPim!=null) {
                    histos.get("Vertex").getH1F("h1_vz_pi-").fill(recPim.vz());
                    histos.get("Vertex").getH2F("h2_theta_pi-").fill(recPim.vz(), Math.toDegrees(recPim.theta()));
                    histos.get("Vertex").getH2F("h2_phi_pi-").fill(recPim.vz(), Math.toDegrees(recPim.phi()));
                    histos.get("Vertex").getH2F("h2_p_pi-").fill(recPim.vz(), recPim.p());
                }                
            }
            if(recEl!=null && recPip!=null && recPim!=null) {
                recPro = new Particle();
                recPro.copy(target);
                recPro.combine(beam, +1);
                recPro.combine(recEl, -1);
                recPro.combine(recPip,   -1);
                recPro.combine(recPim,  -1);
                Particle rho = new Particle();
                rho.copy(recPip);
                rho.combine(recPim, +1);
                histos.get("2pi").getH1F("mxt1").fill(recPro.mass());
                histos.get("2pi").getH1F("mit1").fill(rho.mass());                    
            }
            else if(recEl!=null && recPip!=null && recPro!=null && recPim==null) {
                recPim = new Particle();
                recPim.copy(target);
                recPim.combine(beam, +1);
                recPim.combine(recEl, -1);
                recPim.combine(recPip,   -1);
                recPim.combine(recPro,  -1);
                Particle rho = new Particle();
                rho.copy(recPip);
                rho.combine(recPim, +1);
                histos.get("2pi").getH1F("mxt2").fill(recPim.mass2());
                histos.get("2pi").getH1F("mit2").fill(rho.mass());                                
            }
            else if(recEl!=null && recPim!=null && recPro!=null && recPip==null) {
                recPip = new Particle();
                recPip.copy(target);
                recPip.combine(beam, +1);
                recPip.combine(recEl, -1);
                recPip.combine(recPim,   -1);
                recPip.combine(recPro,  -1);
                Particle rho = new Particle();
                rho.copy(recPip);
                rho.combine(recPim, +1);
                histos.get("2pi").getH1F("mxt3").fill(recPip.mass2());
                histos.get("2pi").getH1F("mit3").fill(rho.mass());                                
            }
            else if(recEl!=null && recPip!=null && recPim==null && recPro==null) {
                Particle neutron = new Particle();
                neutron.copy(target);
                neutron.combine(beam, +1);
                neutron.combine(recEl, -1);
                neutron.combine(recPip,   -1);
                Particle W = new Particle();
                W.copy(target);
                W.combine(beam, +1);
                W.combine(recEl, -1);
                histos.get("1pi").getH2F("W1").fill(Math.toDegrees(recEl.phi()), neutron.mass());
                histos.get("1pi").getH1F("mxt1").fill(neutron.mass());                    
            }
            else if(recEl!=null && recPro!=null && recPim==null && recPip==null) {
                Particle pizero = new Particle();
                pizero.copy(target);
                pizero.combine(beam, +1);
                pizero.combine(recEl, -1);
                pizero.combine(recPro,   -1);
                Particle W = new Particle();
                W.copy(target);
                W.combine(beam, +1);
                W.combine(recEl, -1);
                histos.get("1pi").getH2F("W2").fill(Math.toDegrees(recEl.phi()), pizero.mass2());
                histos.get("1pi").getH1F("mxt2").fill(pizero.mass2());                    
            } 
        }
                
    }

    public void analyzeHistos() {
        Logger.getLogger("org.freehep.math.minuit").setLevel(Level.WARNING);

        fitW(histos.get("General").getH1F("hi_w"), 0.8, 1.1);
        fitW(histos.get("Elastic").getH1F("hi_w")), 0.8, 1.1;
        fitW(histos.get("2pi").getH1F("mxt1"), 0.8, 1.1);
        fitW(histos.get("2pi").getH1F("mxt2"), -0.1, 0.2);
        fitW(histos.get("2pi").getH1F("mxt3"), -0.1, 0.2);
        fitW(histos.get("1pi").getH1F("mxt1"), 0.8, 1.1);
        fitW(histos.get("1pi").getH1F("mxt2"), -0.1, 0.2);
        for(int sector=1; sector <= 6; sector++) {
            fitW(histos.get("W").getH1F("hi_w_" + sector), 0.8, 1.1);
            fitGauss(histos.get("Phi").getH1F("hi_dphi_" + sector));
            fitGauss(histos.get("Beam").getH1F("hi_beam_" + sector));
        }
        fitGauss(histos.get("Proton").getH1F("hi_dp"));
        fitGauss(histos.get("Proton").getH1F("hi_dtheta"));
        fitGauss(histos.get("Proton").getH1F("hi_dphi"));
        fitGauss(histos.get("Proton").getH1F("hi_dz"));
    }

    public EmbeddedCanvasTabbed drawHistos(String optStats) {

        EmbeddedCanvasTabbed canvas = null;
        
        String[] titles = (String[]) histos.keySet().toArray();
        for(int i=0; i<titles.length; i++) {
            String title = titles[titles.length-1-i];
            if(i==0) {
                canvas = new EmbeddedCanvasTabbed(title);
            }
            else {
                canvas.addCanvas(title);
            }
            canvas.getCanvas(title).draw(histos.get(title));
            canvas.getCanvas(title).setGridX(false);
            canvas.getCanvas(title).setGridY(false);
            for(EmbeddedPad pad : canvas.getCanvas(title).getCanvasPads()) {
                pad.getAxisZ().setLog(true);
                for(IDataSetPlotter dsp : pad.getDatasetPlotters()) {
                    IDataSet ds = dsp.getDataSet();
                    if(ds instanceof H1F)
                        ((H1F) ds).setOptStat(optStats);
                }
            }
        }
        for(EmbeddedPad pad : canvas.getCanvas("Vertex").getCanvasPads()) {
            IDataSet ds = pad.getDatasetPlotters().get(0).getDataSet();
            if(ds instanceof H2F) {
               pad.getAxisZ().setLog(true);
               DataLine lineU = new DataLine(targetPos-targetLength/2,((H2F) ds).getYAxis().min(),targetPos-targetLength/2,((H2F) ds).getYAxis().max());
               DataLine lineD = new DataLine(targetPos+targetLength/2,((H2F) ds).getYAxis().min(),targetPos+targetLength/2,((H2F) ds).getYAxis().max());
               DataLine lineX = new DataLine(scWindow,((H2F) ds).getYAxis().min(),scWindow,((H2F) ds).getYAxis().max());
               pad.draw(lineU);
               pad.draw(lineD);
               pad.draw(lineX);
            }
            else if(ds instanceof H1F) {
               DataLine lineU = new DataLine(targetPos-targetLength/2,0,targetPos-targetLength/2,((H1F) ds).getMax());
               DataLine lineD = new DataLine(targetPos+targetLength/2,0,targetPos+targetLength/2,((H1F) ds).getMax());
               DataLine lineX = new DataLine(scWindow,0,scWindow,((H1F) ds).getMax());
               lineU.setLineColor(2);
               lineD.setLineColor(2);
               lineX.setLineColor(2);
               pad.draw(lineU);
               pad.draw(lineD);
               pad.draw(lineX);
            }
        }    
        return canvas;
    }
        
    public void saveHistos(String filename) {
        TDirectory dir = new TDirectory();
        for(String key : histos.keySet()) {
            String folder = "/" + key;
            dir.mkdir(folder);
            dir.cd(folder);        
            int nrows = histos.get(key).getRows();
            int ncols = histos.get(key).getColumns();
            int nds   = nrows*ncols;
            for(int i = 0; i < nds; i++){
                List<IDataSet> dsList = histos.get(key).getData(i);
                for(IDataSet ds : dsList){
    //                    System.out.println("\t --> " + ds.getName());
                    dir.addDataSet(ds);
                }
            }
        }
        dir.writeFile(filename);
    }

    public void readHistos(String filename) {
        this.createHistos();
        TDirectory dir = new TDirectory();
        dir.readFile(filename);
        System.out.println(dir.getDirectoryList());
        for(String key : histos.keySet()) {
            String folder = key + "/";
            System.out.println("Reading from: " + folder);
            DataGroup group = this.histos.get(key);
            int nrows = group.getRows();
            int ncols = group.getColumns();
            int nds   = nrows*ncols;
            DataGroup newGroup = new DataGroup(ncols,nrows);
            for(int i = 0; i < nds; i++){
                List<IDataSet> dsList = group.getData(i);
                for(IDataSet ds : dsList){
                    System.out.println("\t --> " + ds.getName());
                    newGroup.addDataSet(dir.getObject(folder, ds.getName()),i);
                }
            }            
            this.histos.replace(key, newGroup);
        }
    }
    
    private void fitW(H1F hiw, double min, double max) {

	F1D f1w = new F1D("f1_w", "[amp]*gaus(x,[mean],[sigma])", min, max);
        // get histogram maximum in the rane 0.7-1.1
        int i1=hiw.getXaxis().getBin(min);
        int i2=hiw.getXaxis().getBin(max);
        double hiMax=0;
        int    imax=i1;
        for(int i=i1; i<=i2; i++) {
            if(hiMax<hiw.getBinContent(i)) {
                imax=i;
                hiMax=hiw.getBinContent(i);
            }
        }           
        double mean = hiw.getDataX(imax); //hiw.getDataX(hiw.getMaximumBin());
        double amp  = hiMax;//hiw.getBinContent(hiw.getMaximumBin());
        double sigma = 0.05;
        f1w.setParameter(0, amp);
        f1w.setParameter(1, mean);
        f1w.setParameter(2, sigma);
        double rmax = mean + 1.0 * Math.abs(sigma);
        double rmin = mean - 2.0 * Math.abs(sigma);
        f1w.setRange(rmin, rmax);
        DataFitter.fit(f1w, hiw, "Q"); //No options uses error for sigma 
        hiw.setFunction(null);
        mean = f1w.getParameter(1);
        sigma = f1w.getParameter(2);
        rmax = mean + 1.0 * Math.abs(sigma);
        rmin = mean - 2.0 * Math.abs(sigma);
        f1w.setRange(rmin, rmax);
//        System.out.println(mean + " " + sigma + " " + rmin + " " + rmax);
        DataFitter.fit(f1w, hiw, "Q"); //No options uses error for sigma 
        amp = f1w.getParameter(0);
        mean = f1w.getParameter(1);
        sigma = f1w.getParameter(2);
        rmax = mean + 5.0 * Math.abs(sigma);
        rmin = mean - 6.0 * Math.abs(sigma);
        hiw.setFunction(null);
//        f1w = new F1D("f1_w", "[amp]*gaus(x,[mean],[sigma])+[p0]+[p1]*x+[p2]*x*x", rmin, rmax);
        f1w = new F1D("f1_w", "[amp]*gaus(x,[mean],[sigma])+[amp1]*gaus(x,[mean1],[sigma1])", rmin, rmax);
        f1w.setParameter(0, amp);
        f1w.setParameter(1, mean);
        f1w.setParameter(2, sigma);
        f1w.setParameter(3, amp*0.3);
        f1w.setParameter(4, mean+0.1);
        f1w.setParameter(5, sigma*10);
        f1w.setLineColor(2);
        f1w.setLineWidth(2);
        f1w.setOptStat("1111");
        DataFitter.fit(f1w, hiw, "Q"); //No options uses error for sigma 
    }

    public void fitGauss(H1F hi) {
        double mean = hi.getDataX(hi.getMaximumBin());
        double amp  = hi.getBinContent(hi.getMaximumBin());
        double rms  = hi.getRMS();
        double sigma = rms/2;
        String name = hi.getName().split("hi")[1];
        F1D f1 = new F1D("f1" + name,"[amp]*gaus(x,[mean],[sigma])",-0.3, 0.3);
        f1.setLineColor(2);
        f1.setLineWidth(2);
        f1.setOptStat("1111");
        f1.setParameter(0, amp);
        f1.setParameter(1, mean);
        f1.setParameter(2, sigma);
        double rmax = mean + 2.5 * Math.abs(sigma);
        double rmin = mean - 2.5 * Math.abs(sigma);
        f1.setRange(rmin, rmax);
        DataFitter.fit(f1, hi, "Q"); //No options uses error for sigma 
    }

    private PhysicsEvent writeToLund(double ebeam, Particle electron) {
        PhysicsEvent ev = new PhysicsEvent();
        double m = PhysicsConstants.massProton();
        double e = ebeam+m-electron.e();
        double p = Math.sqrt(e*e-m*m);
        double theta = Math.acos((ebeam*ebeam+p*p-Math.pow(ebeam+m-e,2))/2/ebeam/p);
        double phi = Math.PI+electron.phi();
        if(Double.isNaN(p) || Double.isNaN(theta)) return null;
        Particle part = new Particle(2212, 
                                     p*Math.sin(theta)*Math.cos(phi), 
                                     p*Math.sin(theta)*Math.sin(phi), 
                                     p*Math.cos(theta), 
                                     electron.vx(), 
                                     electron.vy(), 
                                     electron.vz());
        ev.addParticle(electron);	    
        ev.addParticle(part);	    
        ev.setBeam("e-", ebeam);    
        return ev;
    }

    
    public static void main(String[] args){
        
        OptionStore parser = new OptionStore("Kinematics");
        
        // valid options for event-base analysis
        parser.addCommand("-process", "process event files");
        parser.getOptionParser("-process").addOption("-o"        ,"",           "output histogram file name prefix");
        parser.getOptionParser("-process").addOption("-nevent"   ,"-1",         "maximum number of events to process");
        parser.getOptionParser("-process").addOption("-beam"     ,"10.6",       "beam energy in GeV");
        parser.getOptionParser("-process").addOption("-display"  ,"1",          "display histograms (0/1)");
        parser.getOptionParser("-process").addOption("-stats"    ,"",           "histogram stat option");
        
        // valid options for histogram-base analysis
        parser.addCommand("-plot", "plot histogram files");
        parser.getOptionParser("-plot").addOption("-beam"     ,"10.6",          "beam energy in GeV");
        parser.getOptionParser("-plot").addOption("-display"  ,"1",             "display histograms (0/1)");
        parser.getOptionParser("-plot").addOption("-stats"    ,"",              "set histogram stat option");
        
        parser.parse(args);
        
        boolean openWindow = false;
        String  optStats   = "";
        List<String> inputList = null;
        
        Kinematics analysis = null;
        
        if(parser.getCommand().equals("-process")) {
            int    maxEvents   = parser.getOptionParser("-process").getOption("-nevent").intValue();
            double beamEnergy  = parser.getOptionParser("-process").getOption("-beam").doubleValue();  
            String namePrefix  = parser.getOptionParser("-process").getOption("-o").stringValue();  
            String histoName   = "histo.hipo";
            if(!namePrefix.isEmpty()) {
                histoName  = namePrefix + "_" + histoName;
            }
            optStats    = parser.getOptionParser("-process").getOption("-stats").stringValue();
            openWindow  = parser.getOptionParser("-process").getOption("-display").intValue()!=0;
            if(!openWindow) System.setProperty("java.awt.headless", "true");
                        
            inputList = parser.getOptionParser("-process").getInputList();
            if(inputList.isEmpty()==true){
                parser.printUsage();
                System.out.println("\n >>>> error: no input file is specified....\n");
                System.exit(0);
            }
            
            analysis = new Kinematics(beamEnergy);
            
            ProgressPrintout progress = new ProgressPrintout();

            int counter = -1;
            analysis.createHistos();
            for(String inputFile : inputList){
                HipoDataSource reader = new HipoDataSource();
                reader.open(inputFile);

                
                while (reader.hasEvent()) {

                    counter++;

                    DataEvent event = reader.getNextEvent();
                    analysis.processEvent(event);
                    
                    progress.updateStatus();
                    if(maxEvents>0){
                        if(counter>=maxEvents) break;
                    }
                }
                progress.showStatus();
                reader.close();
            }               
            analysis.analyzeHistos();
            analysis.saveHistos(histoName);
        }
        
        if(parser.getCommand().equals("-plot")) {
            double beamEnergy  = parser.getOptionParser("-plot").getOption("-beam").doubleValue();  
            optStats   = parser.getOptionParser("-plot").getOption("-stats").stringValue();
            openWindow = parser.getOptionParser("-plot").getOption("-display").intValue()!=0;
            if(!openWindow) System.setProperty("java.awt.headless", "true");

            inputList = parser.getOptionParser("-plot").getInputList();
            if(inputList.isEmpty()==true){
                parser.printUsage();
                System.out.println("\n >>>> error: no input file is specified....\n");
                System.exit(0);
            }

            analysis = new Kinematics(beamEnergy);
            analysis.readHistos(inputList.get(0));
            analysis.analyzeHistos();
        }
        

        if(openWindow) {
            JFrame frame = new JFrame(inputList.get(0));
            frame.setSize(1200, 800);
            frame.add(analysis.drawHistos(optStats));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }     
    }
}