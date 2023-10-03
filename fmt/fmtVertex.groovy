import org.jlab.groot.base.GStyle;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.data.IDataSet;
import org.jlab.groot.data.TDirectory;
import org.jlab.groot.math.F1D;
import org.jlab.groot.math.Dimension1D;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.fitter.ParallelSliceFitter;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.ui.TCanvas;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.data.DataLine;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.io.hipo.HipoDataSource;
import org.jlab.detector.base.DetectorType;
import org.jlab.detector.base.GeometryFactory;
import org.jlab.clas.pdg.PhysicsConstants;
import org.jlab.clas.physics.Particle;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;
import org.jlab.groot.group.DataGroup;
import org.jlab.groot.graphics.EmbeddedCanvasTabbed;
import javax.swing.JFrame;
import org.jlab.geom.base.Detector;
import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Transformation3D;
import org.jlab.utils.benchmark.ProgressPrintout;
import org.jlab.utils.options.OptionParser;
import org.jlab.logging.DefaultLogger;


public class FMTVertex {
    
    private String variation = null
    private String fitType = null;
    
    private Detector fmtDetector = null;
    
    private int fmtNLayers = 3;
    private int fmtNStrips;
    private double fmtZmin;
    private double fmtPitch;
    private double fmtRmin;
    private double fmtRmax;
    private double fmtError;
    private double targetZ;
    private double targetL;
    
    private Map<String,DataGroup> dataGroups = new LinkedHashMap<>();
    
    
    public FMTVertex(String variation, String fit, String optStats) {
        this.variation = variation;
        this.fitType = fit;
        this.loadGeometry(10, variation);
        this.setStyle(optStats);
        this.createHistos()
    }
    
    private void loadGeometry(int run, String variation) {
        // set geometry parameters reading from database
        // load tables
        DatabaseConstantProvider dbProvider = new DatabaseConstantProvider(run, variation);

        //fmt        
        fmtDetector = GeometryFactory.getDetector(DetectorType.FMT,run, variation);
        fmtNStrips  = fmtDetector.getSector(0).getSuperlayer(0).getLayer(0).getNumComponents();
        fmtZmin     = fmtDetector.getSector(0).getSuperlayer(0).getLayer(0).getComponent(0).getMidpoint().z();
        fmtPitch    = fmtDetector.getSector(0).getSuperlayer(0).getLayer(0).getComponent(0).getWidth();
        fmtRmin     = fmtDetector.getSector(0).getSuperlayer(0).getLayer(0).getRmin();
        fmtRmax     = fmtDetector.getSector(0).getSuperlayer(0).getLayer(0).getRmax();
        fmtError    = fmtPitch/Math.sqrt(12)*0.6;
        
        // target
        dbProvider.loadTable("/geometry/target");
        targetZ =  dbProvider.getDouble("/geometry/target/position",0);
        targetL =  dbProvider.getDouble("/geometry/target/length",0);
    }
    
    public Point3D toLocal(int layer, Point3D p) {
        Point3D local = new Point3D(p);
        Transformation3D transform = fmtDetector.getSector(0).getSuperlayer(0).getLayer(layer-1).getTransformation().inverse();
        transform.apply(local);
        return local;
    }


    private void createHistos() {
        double z1 = targetZ-targetL*2-10;
        double z2 = Math.max(targetZ+35, targetZ+targetL+20);
        // create histos
        DataGroup dgVtx = new DataGroup(3,2);
        H1F hi_vz_dc = new H1F("hi_vz_dc", "z (cm)", "Counts", 500, z1, z2);
        hi_vz_dc.setFillColor(43);
        H1F hi_vz_dcfmt = new H1F("hi_vz_dcfmt", "z (cm)", "Counts", 500, z1, z2);
        hi_vz_dcfmt.setFillColor(43);
        H1F hi_vz_fmt = new H1F("hi_vz_fmt", "z (cm)", "Counts", 500, z1, z2);
        hi_vz_fmt.setFillColor(44);
        H2F hi_vz_theta_dc = new H2F("hi_vz_theta_dc", 200, z1, z2, 100, 0, 40);
        hi_vz_theta_dc.setTitleX("DC vz (cm)");
        hi_vz_theta_dc.setTitleY("#theta (deg)");
        H2F hi_vz_theta_fmt = new H2F("hi_vz_theta_fmt", 200, z1, z2, 100, 0, 40);
        hi_vz_theta_fmt.setTitleX("FMT vz (cm)");
        hi_vz_theta_fmt.setTitleY("#theta (deg)");
        F1D ftheta = new F1D("ftheta","57.29*atan([r]/([z0]-x))",z1, 20.5);
        ftheta.setParameter(0, fmtRmin);
        ftheta.setParameter(1, fmtZmin);
        ftheta.setLineColor(2);
        ftheta.setLineWidth(2);
        F1D ftheta2 = new F1D("ftheta2","57.29*atan([r]/([z0]-x))",z1, 3);
        ftheta2.setParameter(0, fmtRmax);
        ftheta2.setParameter(1, fmtZmin);
        ftheta2.setLineColor(2);
        ftheta2.setLineWidth(2);
        H2F hi_vz_phi_dc = new H2F("hi_vz_phi_dc", 200, z1, z2, 100, -180, 180);
        hi_vz_phi_dc.setTitleX("DC vz (cm)");
        hi_vz_phi_dc.setTitleY("#phi (deg)");
        H2F hi_vz_phi_fmt = new H2F("hi_vz_phi_fmt", 200, z1, z2, 100, -180, 180);
        hi_vz_phi_fmt.setTitleX("FMT vz (cm)");
        hi_vz_phi_fmt.setTitleY("#phi (deg)");
        dgVtx.addDataSet(hi_vz_dcfmt,     0);
        dgVtx.addDataSet(hi_vz_fmt,       0);
        dgVtx.addDataSet(hi_vz_theta_fmt, 1);
        dgVtx.addDataSet(ftheta,          1);
        dgVtx.addDataSet(ftheta2,         1);
        dgVtx.addDataSet(hi_vz_phi_fmt,   2);
        dgVtx.addDataSet(hi_vz_dc,        3);
        dgVtx.addDataSet(hi_vz_fmt,       3);
        dgVtx.addDataSet(hi_vz_theta_dc,  4);
        dgVtx.addDataSet(ftheta,          4);
        dgVtx.addDataSet(ftheta2,         4);
        dgVtx.addDataSet(hi_vz_phi_dc,    5);
        dataGroups.put("Vertex", dgVtx);

        DataGroup dgFMT = new DataGroup(3,2);
        for (int sector = 1; sector <= 6; sector++) {
            hi_vz_fmt = new H1F("hi_vz_fmt_sec" + sector, "z (cm) - sector " + sector, "Counts", 500, z1, z2);
            hi_vz_fmt.setFillColor(44);
            hi_vz_dc = new H1F("hi_vz_dc_sec" + sector, "z (cm) - sector " + sector, "Counts", 500, z1, z2);
            hi_vz_dc.setFillColor(43);
            dgFMT.addDataSet(hi_vz_dc, sector - 1);
            dgFMT.addDataSet(hi_vz_fmt, sector - 1);
        }
        dataGroups.put("VertexSector", dgFMT);
        dataGroups.put("VertexZoomed", dgFMT);

        DataGroup dgTrack = new DataGroup(2,1);
        H1F hi_chi2  = new H1F("hi_chi2", "#chi^2", "Counts", 100, 0, 5);
        hi_chi2.setFillColor(44);
        hi_chi2.setOptStat("1110");
        H1F hi_ndf  = new H1F("hi_ndf", "NDF", "Counts", 6, -0.5, 5.5);
        hi_ndf.setFillColor(44);
        hi_ndf.setOptStat("1110");
        dgTrack.addDataSet(hi_chi2, 0);
        dgTrack.addDataSet(hi_ndf,  1);
        dataGroups.put("Track", dgTrack);

        DataGroup dgTraj = new DataGroup(4,2);
        for (int layer = 0; layer<=3; layer++) {
            H2F hi_traj = new H2F("hi_traj" + layer, 200, -20, 20, 200, -20, 20);
            hi_traj.setTitleX("x (cm)");
            hi_traj.setTitleY("y (cm)");
            H2F hi_ftraj = new H2F("hi_ftraj" + layer, 200, -20, 20, 200, -20, 20);
            hi_ftraj.setTitleX("x (cm)");
            hi_ftraj.setTitleY("y (cm)");
            H2F hi_rtraj = new H2F("hi_rtraj" + layer, 200, -20, 20, 200, -20, 20);
            hi_rtraj.setTitleX("x (cm)");
            hi_rtraj.setTitleY("y (cm)");
            dgTraj.addDataSet(hi_traj, layer);
            dgTraj.addDataSet(hi_ftraj,layer);
            dgTraj.addDataSet(hi_rtraj,layer+4);
        }
        dataGroups.put("Traj", dgTraj);

        DataGroup dgRes = new DataGroup(3,4);
        for (int layer = 1; layer<=3; layer++) {
            H1F hi_res_fmt = new H1F("hi_res_fmt_lay" + layer, "Res(cm) - layer " + layer, "Counts", 200, -0.3, 0.3);
            hi_res_fmt.setFillColor(4);
            H1F hi_pull_fmt = new H1F("hi_pull_fmt_lay" + layer, "Pull - layer " + layer, "Counts", 200, -10, 10);
            hi_pull_fmt.setFillColor(4);
            H1F hi_res_dc  = new H1F("hi_res_dc_lay" + layer, "Res(cm) - layer " + layer, "Counts", 200, -3, 3);
            hi_res_dc.setFillColor(3);
            H1F hi_doca_dc  = new H1F("hi_doca_dc_lay" + layer, "Doca(cm) - layer " + layer, "Counts", 200, -3, 3);
            hi_doca_dc.setFillColor(3);
            dgRes.addDataSet(hi_res_fmt,  layer-1);
            dgRes.addDataSet(hi_pull_fmt, layer+2);
            dgRes.addDataSet(hi_res_dc,   layer+5);
            dgRes.addDataSet(hi_doca_dc,  layer+8);
        }
        dataGroups.put("Residuals", dgRes);

        DataGroup dgRes2D = new DataGroup(3,4);
        for (int layer = 1; layer<=3; layer++) {
            H2F hi_resX_fmt = new H2F("hi_res2dX_fmt_lay" + layer, 200, -0.3, 0.3, 100, -20, 20);
            hi_resX_fmt.setTitleX("Res(cm) - layer " + layer);
            hi_resX_fmt.setTitleY("x(cm)");
            H2F hi_resY_fmt = new H2F("hi_res2dY_fmt_lay" + layer, 200, -0.3, 0.3, 100, -20, 20);
            hi_resY_fmt.setTitleX("Res(cm) - layer " + layer);
            hi_resY_fmt.setTitleY("y(cm)");
            H2F hi_res_dc = new H2F("hi_res2d_dc_lay" + layer, 200, -3, 3, 100, -20, 20);
            hi_res_dc.setTitleX("Res(cm) - layer " + layer);
            hi_res_dc.setTitleY("x(cm)");
            H2F hi_doca_dc = new H2F("hi_doca2d_dc_lay" + layer, 200, -3, 3, 100, -20, 20);
            hi_doca_dc.setTitleX("Doca(cm) - layer " + layer);
            hi_doca_dc.setTitleY("x(cm)");
            dgRes2D.addDataSet(hi_resX_fmt, layer-1);
            dgRes2D.addDataSet(hi_resY_fmt, layer+2);
            dgRes2D.addDataSet(hi_res_dc,   layer+5);
            dgRes2D.addDataSet(hi_doca_dc,  layer+8);
        }
        dataGroups.put("Res2D", dgRes2D);

        DataGroup dgCluster = new DataGroup(3,3);
        for (int layer = 1; layer<=3; layer++) {
            H1F hi_size  = new H1F("hi_size_lay" + layer, "Size - layer " + layer, "Counts", 10, 0, 10);
            hi_size.setFillColor(4);
            H1F hi_size_track  = new H1F("hi_size_track_lay" + layer, "Size - layer " + layer, "Counts", 10, 0, 10);
            hi_size_track.setFillColor(3);
            H1F hi_time  = new H1F("hi_time_lay" + layer, "Time - layer " + layer, "Counts", 240, 0, 240);
            hi_time.setFillColor(4);
            H1F hi_time_track  = new H1F("hi_time_track_lay" + layer, "Time - layer " + layer, "Counts", 240, 0, 240);
            hi_time_track.setFillColor(3);
            H1F hi_energy  = new H1F("hi_energy_lay" + layer, "Energy - layer " + layer, "Counts", 100, 0, 20000);
            hi_energy.setFillColor(4);
            H1F hi_energy_track  = new H1F("hi_energy_track_lay" + layer, "Energy - layer " + layer, "Counts", 100, 0, 20000);
            hi_energy_track.setFillColor(3);
            dgCluster.addDataSet(hi_size,         layer-1);
            dgCluster.addDataSet(hi_size_track,   layer-1);
            dgCluster.addDataSet(hi_time,         layer+2);
            dgCluster.addDataSet(hi_time_track,   layer+2);
            dgCluster.addDataSet(hi_energy,       layer+5);
            dgCluster.addDataSet(hi_energy_track, layer+5);
        }
        dataGroups.put("Clusters", dgCluster);

      DataGroup dgHit = new DataGroup(3,3);
        for (int layer = 1; layer<=3; layer++) {
            H2F hi_strip_res = new H2F("hi_strip_res_lay" + layer, 100, -0.3, 0.3, 1025, 0, 1025);
            hi_strip_res.setTitleX("Res(cm) - layer " + layer);
            hi_strip_res.setTitleY("Strip - layer " + layer);
            H1F hi_time  = new H1F("hi_time_lay" + layer, "Time - layer " + layer, "Counts", 240, 0, 240);
            hi_time.setFillColor(4);
            H1F hi_time_track  = new H1F("hi_time_track_lay" + layer, "Time - layer " + layer, "Counts", 240, 0, 240);
            hi_time_track.setFillColor(3);
            H1F hi_energy  = new H1F("hi_energy_lay" + layer, "Energy - layer " + layer, "Counts", 100, 0, 6000);
            hi_energy.setFillColor(4);
            H1F hi_energy_track  = new H1F("hi_energy_track_lay" + layer, "Energy - layer " + layer, "Counts", 100, 0, 6000);
            hi_energy_track.setFillColor(3);
            dgHit.addDataSet(hi_strip_res,    layer-1);
            dgHit.addDataSet(hi_time,         layer+2);
            dgHit.addDataSet(hi_time_track,   layer+2);
            dgHit.addDataSet(hi_energy,       layer+5);
            dgHit.addDataSet(hi_energy_track, layer+5);
        }
        dataGroups.put("Hits", dgHit);
            
        DataGroup dgStrips = new DataGroup(3,3);
        for (int layer = 1; layer<=3; layer++) {
            H1F hi_strip  = new H1F("hi_strip_lay" + layer, "Strip - layer " + layer, "Counts", 1025, 0, 1025);
            hi_strip.setFillColor(4);
            H1F hi_strip_track  = new H1F("hi_strip_track_lay" + layer, "Strip - layer " + layer, "Counts", 1025, 0, 1025);
            hi_strip_track.setFillColor(3);
            H1F hi_strip_eff  = new H1F("hi_strip_eff_lay" + layer, "Strip - layer " + layer, "Counts", 1025, 0, 1025);
            hi_strip_eff.setFillColor(3);
            dgStrips.addDataSet(hi_strip,       layer-1);
            dgStrips.addDataSet(hi_strip_track, layer+2);
            dgStrips.addDataSet(hi_strip_eff,   layer+5);
        }
        dataGroups.put("Strips", dgStrips);
    }

    private void setStyle(String optStats) {
        GStyle.getAxisAttributesX().setTitleFontSize(24);
        GStyle.getAxisAttributesX().setLabelFontSize(18);
        GStyle.getAxisAttributesY().setTitleFontSize(24);
        GStyle.getAxisAttributesY().setLabelFontSize(18);
        GStyle.getAxisAttributesZ().setLabelFontSize(14);
        GStyle.getAxisAttributesX().setLabelFontName("Arial");
        GStyle.getAxisAttributesY().setLabelFontName("Arial");
        GStyle.getAxisAttributesZ().setLabelFontName("Arial");
        GStyle.getAxisAttributesX().setTitleFontName("Arial");
        GStyle.getAxisAttributesY().setTitleFontName("Arial");
        GStyle.getAxisAttributesZ().setTitleFontName("Arial");
        GStyle.setGraphicsFrameLineWidth(1);
        GStyle.getH1FAttributes().setLineWidth(1);
        GStyle.getH1FAttributes().setOptStat(optStats);
    }

    public void processEvent(DataEvent event) {
        // get relrevant data banks
        DataBank recRun = null;
        DataBank recPart = null;
        DataBank recTrack = null;
        DataBank recTraj = null;
        DataBank fmtHits = null;
        DataBank fmtClusters = null;
        DataBank fmtTracks= null;
        if(event.hasBank("RUN::config"))      recRun = event.getBank("RUN::config");
        if(event.hasBank("REC::Particle"))    recPart = event.getBank("REC::Particle");
        if(event.hasBank("REC::Track"))       recTrack = event.getBank("REC::Track");
        if(event.hasBank("REC::Traj"))        recTraj = event.getBank("REC::Traj");
        if(event.hasBank("FMT::Hits"))     fmtHits = event.getBank("FMT::Hits");
        if(event.hasBank("FMT::Clusters")) fmtClusters = event.getBank("FMT::Clusters");
        if(event.hasBank("FMT::Tracks"))   fmtTracks = event.getBank("FMT::Tracks");

        // ignore events that don't have necessary banks
        if (recPart== null || recTrack== null || fmtTracks==null) return;

        // loop through tracks points
        for (int loop = 0; loop < recTrack.rows(); loop++) {
            int index      = recTrack.getShort("index", loop);
            int pindex     = recTrack.getShort("pindex", loop);
            int sector     = recTrack.getByte("sector", loop);
            double chi2    = recTrack.getFloat("chi2", loop);
            int    ndf     = recTrack.getInt("NDF", loop);
            int charge     = recPart.getByte("charge",pindex);
            int pid        = recPart.getInt("pid",pindex);
            int status     = recPart.getShort("status",pindex);
            double vz      = recPart.getFloat("vz",pindex);
            double chi2pid = recPart.getFloat("chi2pid",pindex);
            status = (int) (Math.abs(status)/1000);
            
            // use only FMT layers 1,2,3 (ignore 4,5,6 that are not installed in RG-F)
            if (status==2 && Math.abs(chi2pid)<5 && vz<fmtZmin && pid==11 && chi2/ndf<15
                && recTrack.rows()>=1) {
                Particle dcPart = new Particle(pid,
                    recPart.getFloat("px", pindex),
                    recPart.getFloat("py", pindex),
                    recPart.getFloat("pz", pindex),
                    recPart.getFloat("vx", pindex),
                    recPart.getFloat("vy", pindex),
                    recPart.getFloat("vz", pindex));
                if(Math.sqrt(dcPart.vx()*dcPart.vx()+dcPart.vy()*dcPart.vy())>2) continue;
                dataGroups.get("Vertex").getH1F("hi_vz_dc").fill(dcPart.vz());
                dataGroups.get("Vertex").getH2F("hi_vz_theta_dc").fill(dcPart.vz(), Math.toDegrees(dcPart.theta()));
                dataGroups.get("Vertex").getH2F("hi_vz_phi_dc").fill(dcPart.vz(), Math.toDegrees(dcPart.phi()));

                Particle fmtPart = new Particle(pid,
                    fmtTracks.getFloat("p0_x", index),
                    fmtTracks.getFloat("p0_y", index),
                    fmtTracks.getFloat("p0_z", index),
                    fmtTracks.getFloat("Vtx0_x", index),
                    fmtTracks.getFloat("Vtx0_y", index),
                    fmtTracks.getFloat("Vtx0_z", index));
                double fchi2 = fmtTracks.getFloat("chi2", index);
                int nmeas    = fmtTracks.getInt("NDF", index);

                boolean traj = false;
                Point3D[] trajs = new Point3D[3]; 
                for(int j=0; j<recTraj.rows(); j++) {
                    if(recTraj.getByte("detector",j)==DetectorType.FMT.getDetectorId() && recTraj.getShort("index",j)==index) {
                        int layer = recTraj.getByte("layer",j);
                        if(layer<=3) {
                            trajs[layer-1] = new Point3D(recTraj.getFloat("x",j),recTraj.getFloat("y",j),recTraj.getFloat("z",j));
                            dataGroups.get("Traj").getH2F("hi_traj"+layer).fill(trajs[layer-1].x(),trajs[layer-1].y());
                            if(layer==1) {
                                dataGroups.get("Traj").getH2F("hi_traj"+0).fill(trajs[0].x(),trajs[0].y());
                            }
                        }   
                    }
                }
                if(nmeas>=2 && fmtTracks.rows()>=1) {
                    dataGroups.get("Track").getH1F("hi_chi2").fill(fchi2);
                    dataGroups.get("Track").getH1F("hi_ndf").fill(nmeas);
                    for(int i=0; i<3; i++) {
                        int layer = i+1;
                        if(trajs[layer-1]!=null) {
                            if(layer==1) dataGroups.get("Traj").getH2F("hi_ftraj"+0).fill(trajs[0].x(),trajs[0].y());
                            Point3D dcLocal = this.toLocal(layer, trajs[i]);
                            double fmtTraj  = Double.POSITIVE_INFINITY;
                            double dcDoca   = Double.POSITIVE_INFINITY;
                            for(int j=0; j<fmtClusters.rows(); j++) {
                                if(fmtClusters.getByte("layer",j)==layer) {
                                    double centroid = fmtClusters.getFloat("centroid",j);
                                    double residual = fmtClusters.getFloat("residual",j);
                                    double doca     = fmtClusters.getFloat("doca",j);
                                    int trackIndex  = fmtClusters.getShort("trackIndex",j);
                                    if(trackIndex==index) {
                                        dcDoca  = centroid - doca;
                                        fmtTraj = centroid - residual; 
                                        dataGroups.get("Traj").getH2F("hi_ftraj" + layer).fill(trajs[layer-1].x(),trajs[layer-1].y());
                                        break;
                                    }
                                }
                            }
                            for(int j=0; j<fmtClusters.rows(); j++) {
                                if(fmtClusters.getByte("layer",j)==layer) {
                                    int    size     = fmtClusters.getInt("size",j);
                                    int    strip    = fmtClusters.getShort("seedStrip",j);
                                    double centroid = fmtClusters.getFloat("centroid",j);
                                    double error    = fmtClusters.getFloat("centroidError",j);
                                    double energy   = fmtClusters.getFloat("energy",j);
                                    double time     = fmtClusters.getFloat("time",j);
                                    dataGroups.get("Strips").getH1F("hi_strip_lay" + layer).fill(strip);
                                    dataGroups.get("Clusters").getH1F("hi_size_lay" + layer).fill(size);
                                    dataGroups.get("Clusters").getH1F("hi_energy_lay" + layer).fill(energy);
                                    dataGroups.get("Clusters").getH1F("hi_time_lay" + layer).fill(time);
                                    if(fmtClusters.getShort("trackIndex",j)==index) {
                                        dataGroups.get("Strips").getH1F("hi_strip_track_lay" + layer).fill(strip);
                                        dataGroups.get("Hits").getH2F("hi_strip_res_lay" + layer).fill(centroid-fmtTraj,strip);
                                        dataGroups.get("Clusters").getH1F("hi_size_track_lay" + layer).fill(size);
                                        dataGroups.get("Clusters").getH1F("hi_energy_track_lay" + layer).fill(energy);
                                        dataGroups.get("Clusters").getH1F("hi_time_track_lay" + layer).fill(time);
                                    }
                                    //if(Math.abs(time-124)>50) continue;
                                    dataGroups.get("Residuals").getH1F("hi_res_dc_lay"+layer).fill(centroid-dcLocal.y());
                                    dataGroups.get("Residuals").getH1F("hi_doca_dc_lay"+layer).fill(centroid-dcDoca);
                                    dataGroups.get("Residuals").getH1F("hi_res_fmt_lay"+layer).fill(centroid-fmtTraj);
                                    dataGroups.get("Residuals").getH1F("hi_pull_fmt_lay"+layer).fill((centroid-fmtTraj)/error);
                                    dataGroups.get("Res2D").getH2F("hi_res2d_dc_lay"+layer).fill(centroid-dcLocal.y(),dcLocal.x());
                                    dataGroups.get("Res2D").getH2F("hi_doca2d_dc_lay"+layer).fill(centroid-dcDoca,dcLocal.x());
                                    dataGroups.get("Res2D").getH2F("hi_res2dX_fmt_lay"+layer).fill(centroid-fmtTraj,trajs[layer-1].x());
                                    dataGroups.get("Res2D").getH2F("hi_res2dY_fmt_lay"+layer).fill(centroid-fmtTraj,trajs[layer-1].y());
                                }
                            }
                            for(int j=0; j<fmtHits.rows(); j++) {
                                if(fmtHits.getByte("layer",j)==layer) {
                                    int    strip    = fmtHits.getShort("strip",j);
                                    double energy   = fmtHits.getFloat("energy",j);
                                    double time     = fmtHits.getFloat("time",j);
                                    dataGroups.get("Hits").getH1F("hi_energy_lay" + layer).fill(energy);
                                    dataGroups.get("Hits").getH1F("hi_time_lay" + layer).fill(time);
                                    if(fmtHits.getShort("trackIndex",j)==index) {
                                        dataGroups.get("Hits").getH1F("hi_energy_track_lay" + layer).fill(energy);
                                        dataGroups.get("Hits").getH1F("hi_time_track_lay" + layer).fill(time);
                                    }
                                }
                            }
                        }   
                    }   
                    dataGroups.get("Vertex").getH2F("hi_vz_phi_fmt").fill(fmtPart.vz(), Math.toDegrees(fmtPart.phi()));
                    dataGroups.get("Vertex").getH2F("hi_vz_theta_fmt").fill(fmtPart.vz(), Math.toDegrees(fmtPart.theta()));
                    dataGroups.get("Vertex").getH1F("hi_vz_fmt").fill(fmtPart.vz());
                    dataGroups.get("Vertex").getH1F("hi_vz_dcfmt").fill(dcPart.vz());
                    dataGroups.get("VertexSector").getH1F("hi_vz_fmt_sec"+sector).fill(fmtPart.vz());
                    dataGroups.get("VertexSector").getH1F("hi_vz_dc_sec"+sector).fill(dcPart.vz());
                }
            }
        }	
    }

        
    private void analyze() {
        for(int layer=1; layer<=3; layer++) {
            fitResiduals(dataGroups.get("Residuals").getH1F("hi_res_fmt_lay"+layer));
            fitResiduals(dataGroups.get("Residuals").getH1F("hi_pull_fmt_lay"+layer));
            fitResiduals(dataGroups.get("Residuals").getH1F("hi_res_dc_lay"+layer));
            fitTime(dataGroups.get("Hits").getH1F("hi_time_track_lay"+layer));
        }
        for(int sector=1; sector<=6; sector++) {            
            fitVertex(dataGroups.get("VertexSector").getH1F("hi_vz_fmt_sec"+sector), 4, fitType);
            fitVertex(dataGroups.get("VertexSector").getH1F("hi_vz_dc_sec"+sector), 3, fitType);
        }
        for(int layer=0; layer<=3; layer++) {
            H2F h1 = dataGroups.get("Traj").getH2F("hi_ftraj"+layer);
            H2F h2 = dataGroups.get("Traj").getH2F("hi_traj"+layer);
            H2F h3 = dataGroups.get("Traj").getH2F("hi_rtraj"+layer);
            for(int i=0; i<h1.getDataBufferSize(); i++) {
                double b1 = h1.getDataBufferBin(i);
                double b2 = h2.getDataBufferBin(i);
                double b3 = 0;
                if(b2>0) b3 = b1/b2; // b1/b3
                h3.setDataBufferBin(i, (float) b3);
                if(b1>b2)
                System.out.println(b1 + " " + b2);
            }
            if(layer>=1) {
                dataGroups.get("Strips").getH1F("hi_strip_eff_lay"+layer).add(dataGroups.get("Strips").getH1F("hi_strip_track_lay"+layer));
                dataGroups.get("Strips").getH1F("hi_strip_eff_lay"+layer).divide(dataGroups.get("Strips").getH1F("hi_strip_lay"+layer));
            }
        }   
    }
    
    public EmbeddedCanvasTabbed plotHistos() {

        this.analyze();
        
        EmbeddedCanvasTabbed fmtCanvas = null;
        for(String key : dataGroups.keySet()) {
            if(fmtCanvas==null) fmtCanvas = new EmbeddedCanvasTabbed(key);
            else fmtCanvas.addCanvas(key);
            fmtCanvas.getCanvas(key).draw(dataGroups.get(key));
            fmtCanvas.getCanvas(key).setGridX(false);
            fmtCanvas.getCanvas(key).setGridY(false);
            fmtCanvas.getCanvas(key).setAxisFontSize(18);
            fmtCanvas.getCanvas(key).setAxisTitleSize(24);
        }
        for(int i=0; i<6; i++) {
            Dimension1D range = dataGroups.get("VertexZoomed").getH1F("hi_vz_fmt_sec"+(i+1)).getFunction().getRange();
            fmtCanvas.getCanvas("VertexZoomed").getPad(i).getAxisX().setRange(range.getMin()-2, range.getMax()+2);
        }   
        fmtCanvas.getCanvas("Track").getPad(0).getAxisY().setLog(true);        
        for(int i=0; i<4; i++) {
            fmtCanvas.getCanvas("Traj").getPad(i).getAxisZ().setLog(true);
            fmtCanvas.getCanvas("Traj").getPad(i+4).getAxisZ().setRange(0.8,1.0);
        }   
        for(int i=0; i<3; i++) {
            fmtCanvas.getCanvas("Clusters").getPad(i+3).getAxisY().setLog(true);
            fmtCanvas.getCanvas("Clusters").getPad(i+6).getAxisY().setLog(true);
            fmtCanvas.getCanvas("Hits").getPad(i).getAxisZ().setLog(true);
            fmtCanvas.getCanvas("Hits").getPad(i+3).getAxisY().setLog(true);
            fmtCanvas.getCanvas("Hits").getPad(i+6).getAxisY().setLog(true);
        }   
        return fmtCanvas;
    }
    
    private void fitResiduals(H1F hi) {
        double mean = hi.getDataX(hi.getMaximumBin());
        double amp  = hi.getBinContent(hi.getMaximumBin());
        double rms  = hi.getRMS();
        double sigma = rms/2;
        String name = hi.getName().split("hi")[1];
        F1D f1 = new F1D("f1" + name,"[amp]*gaus(x,[mean],[sigma])+[p0]",-0.3, 0.3);
        f1.setLineColor(2);
        f1.setLineWidth(2);
        f1.setOptStat("1111");
        f1.setParameter(0, amp);
        f1.setParameter(1, mean);
        f1.setParameter(2, sigma);
        double rmax = mean + 4.0 * Math.abs(sigma);
        double rmin = mean - 4.0 * Math.abs(sigma);
        f1.setRange(rmin, rmax);
        DataFitter.fit(f1, hi, "Q"); //No options uses error for sigma 
    }

    private void fitTime(H1F hi) {
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
        double rmax = mean + 3.0 * Math.abs(sigma);
        double rmin = mean - 3.0 * Math.abs(sigma);
        f1.setRange(rmin, rmax);
        DataFitter.fit(f1, hi, "Q"); //No options uses error for sigma 
    }

    private void fitVertex(H1F hi, int icol, String fitType) {
        String name = hi.getName().split("hi")[1];
        double mean  = hi.getDataX(hi.getMaximumBin());
        double amp   = hi.getBinContent(hi.getMaximumBin());
        double sigma = 0.5;
        F1D f1 = null;
        if(fitType.equals("rgm_cryo")) {
            f1 = new F1D("f1"+name,"[amp]*gaus(x,[mean],[sigma])+[p0]+[p1]*x",4, 8);
            mean  = 6;
            int bmean = (mean - hi.getDataX(0))/(hi.getDataX(1)-hi.getDataX(0));
            amp   = hi.getBinContent(bmean);
            sigma = 0.2;
        }
        else if(fitType.equals("rgm_carbon")) {
            f1 = new F1D("f1"+name,"[amp0]*gaus(x,[mean],[sigma])+[amp1]*gaus(x,[mean]+1.245,[sigma])+[amp2]*gaus(x,[mean]+2.490,[sigma])+[amp3]*gaus(x,[mean]+3.735,[sigma])+[amp0]/10*gaus(x,[mean]+2.49,[sigma1])",-8, 3);
            mean = -4.20;
            sigma = 0.15;
            f1.setParameter(3, amp);
            f1.setParameter(4, 1.24);
            f1.setParameter(5, amp);
            f1.setParameter(6, sigma*6);
        }        
        else {
            if(fitType.equals("rgf_spring2020"))
            f1 = new F1D("f1"+name,"[amp]*gaus(x,[mean],[sigma])+[amp]/3*gaus(x,[mean]-2.4,[sigma])+[amp]/8*gaus(x,[mean]-1.2,[sigma]*3)+[p0]+[p1]*x",-36, -28);
            else
            f1 = new F1D("f1"+name,"[amp]*gaus(x,[mean],[sigma])+[amp]/6*gaus(x,[mean]-1.5,[sigma])+[amp]/8*gaus(x,[mean]-1.2,[sigma]*3)+[p0]+[p1]*x",-36, -28);
        }
        f1.setOptStat("1101");
        f1.setLineWidth(2);
        f1.setLineColor(icol);

        f1.setParameter(0, amp);
        f1.setParameter(1, mean);
        f1.setParameter(2, sigma);
        DataFitter.fit(f1, hi, "Q"); //No options uses error for sigma      
    }


    private void fitVertexRGM(H1F hi, int icol) {
        String name = hi.getName().split("hi")[1];
        F1D f1 = new F1D("f1"+name,"[amp]*gaus(x,[mean],[sigma])+[p0]+[p1]*x",4, 8);
        f1.setOptStat("1111");
        f1.setLineWidth(2);
        f1.setLineColor(icol);
        double mean  = 6;
        int    bmean = (mean - hi.getDataX(0))/(hi.getDataX(1)-hi.getDataX(0));
        double amp   = hi.getBinContent(bmean);
        double sigma = 0.2;
        f1.setParameter(0, amp);
        f1.setParameter(1, mean);
        f1.setParameter(2, sigma);
        DataFitter.fit(f1, hi, "Q"); //No options uses error for sigma      
    }

    public void readHistos(String fileName) {
        // TXT table summary FILE //
        System.out.println("Opening file: " + fileName);
        TDirectory dir = new TDirectory();
        dir.readFile(fileName);
        System.out.println(dir.getDirectoryList());
        dir.cd();
        dir.pwd();
        for(String key : dataGroups.keySet()) 
        this.readDataGroup(dir, key);
    }

    public void saveHistos(String fileName) {
        this.analyze();
        TDirectory dir = new TDirectory();
        for(String key : dataGroups.keySet()) 
        this.writeDataGroup(dir, key);
        System.out.println("Saving histograms to file " + fileName);
        dir.writeFile(fileName);
    } 

    public void readDataGroup(TDirectory dir, String key) {
        String folder = key;
        int nrows = dataGroups.get(key).getRows();
        int ncols = dataGroups.get(key).getColumns();
        int nds   = nrows*ncols;
        boolean replace = true;
        DataGroup newGroup = new DataGroup(ncols,nrows);
        for(int i = 0; i < nds; i++){
            List<IDataSet> dsList = dataGroups.get(key).getData(i);
            for(IDataSet ds : dsList){
                if(dir.getObject(folder, ds.getName())!=null) {
                    newGroup.addDataSet(dir.getObject(folder, ds.getName()),i);
                }
                else
                replace = false;
            }
        }
        dataGroups.replace(key, newGroup);
    }

    public void writeDataGroup(TDirectory dir, String key) {
        String folder = "/" + key;
        dir.mkdir(folder);
        dir.cd(folder);        
        int nrows = dataGroups.get(key).getRows();
        int ncols = dataGroups.get(key).getColumns();
        int nds   = nrows*ncols;
        for(int i = 0; i < nds; i++){
            List<IDataSet> dsList = dataGroups.get(key).getData(i);
            for(IDataSet ds : dsList){
                dir.addDataSet(ds);
            }
        }
    }
}

OptionParser parser = new OptionParser("fmtVertex");
parser.setRequiresInputList(false);
// valid options for event-base analysis
parser.addOption("-o"     ,"fmt.hipo",     "output file name ");
parser.addOption("-n"     ,"-1",           "maximum number of events to process");
parser.addOption("-histo" ,"0",            "read histogram file (0/1)");
parser.addOption("-plot"  ,"1",            "display histograms (0/1)");
parser.addOption("-stats" ,"",             "histogram stat option");
parser.addOption("-var"   ,"rgm_fall2021", "database variation");
parser.addOption("-fit"   ,"rgm_cryo",     "vertex fit type");

parser.parse(args);

int     maxEvents    = parser.getOption("-n").intValue();
String  outFile      = parser.getOption("-o").stringValue();        
boolean readHistos   = (parser.getOption("-histo").intValue()!=0);            
boolean openWindow   = (parser.getOption("-plot").intValue()!=0);
String  optStats     = parser.getOption("-stats").stringValue();        
String  variation    = parser.getOption("-var").stringValue();        
String  fit          = parser.getOption("-fit").stringValue();        

if(!openWindow) System.setProperty("java.awt.headless", "true");
//DefaultLogger.debug();

List<String> inputList = parser.getInputList();
if(inputList.isEmpty()==true){
    parser.printUsage();
    System.out.println("\n >>>> error: no input file is specified....\n");
    System.exit(0);
}

FMTVertex analysis = new FMTVertex(variation, fit, optStats);
if(readHistos) {
    for(int i=0; i<inputList.size(); i++){
        analysis.readHistos(inputList.get(i));
    }  
}
else{
    
    ProgressPrintout progress = new ProgressPrintout();
    
    int counter=-1;
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
    analysis.saveHistos(outFile);
}

if(openWindow) {
    JFrame frame = new JFrame("FMT");
    EmbeddedCanvasTabbed canvas = analysis.plotHistos();
    frame.setSize(1400, 850);
    frame.add(canvas);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
}      

