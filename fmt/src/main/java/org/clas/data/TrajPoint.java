package org.clas.data;

import java.util.ArrayList;
import org.jlab.detector.base.DetectorType;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.clas.analysis.FiducialCuts;
import org.clas.analysis.TrkSwim;
import org.clas.test.Constants;
import org.clas.test.HipoHandler;

/** Class in charge of the Trajectory Point objects and importing them from their hipo bank. */
public class TrajPoint {
    private int pi;       // Particle index.
    private int id;       // Track index.
    private int fmtLyr;   // FMT layer.
    private int dcSec;    // DC sector.
    private double z;     // z position.
    private double x;     // x position in the layer's local coordinate system.
    private double y;     // y position in the layer's local coordinate system.
    private double cosTh; // cosine of the trajectory's theta angle.

    /** Constructor. */
    private TrajPoint(int _pi, int _id, int _fmtLyr, int _dcSec, double _z, double _x, double _y,
            double _cosTh) {
        this.pi     = _pi;
        this.id     = _id;
        this.fmtLyr = _fmtLyr;
        this.dcSec  = _dcSec;
        this.z      = _z;
        this.x      = _x;
        this.y      = _y;
        this.cosTh  = _cosTh;
    }

    public int get_pi() {return pi;}
    public int get_id() {return id;}
    public int get_fmtLyr() {return fmtLyr;}
    public int get_dcSec() {return dcSec;}
    public double get_z() {return z;}
    public double get_x() {return x;}
    public double get_y() {return y;}
    public double get_cosTh() {return cosTh;}

    /**
     * Get trajectory points from event bank.
     * @param event        Source data event.
     * @param swim         Instance of the TrkSwim class.
     * @param fcuts        Instance of the FiducialCuts class.
     * @param fmtZ         Array of each FMT layer's z position.
     * @param fmtAngle     Array of each FMT layer's theta angle.
     * @param shArr        Array describing the shifts to be applied.
     * @param minTrjPoints Parameter defining the minimum number of trajectory points to define a
     *                     group. Can be 1, 2, or 3.
     * @param applyCuts    Boolean defining if fiducial cuts should be applied or not.
     * @return ArrayList of an array of trajectory points, each on a different layer.
     */
    public static ArrayList<TrajPoint[]> getTrajPoints(DataEvent event, TrkSwim swim,
            FiducialCuts fcuts, double[] fmtZ, double[] fmtAngle, double[][] shArr,
            int minTrjPoints, boolean applyCuts) {
        // Sanitize input.
        if (minTrjPoints < 1 || minTrjPoints > Constants.FMTLAYERS) {
            System.err.printf("minTrjPoints should be at least 1 and at most Constants.FMTLAYERS!\n");
            return null;
        }

        // Get data banks.
        DataBank trjBank = HipoHandler.getBank(event, "REC::Traj");
        DataBank ptcBank = HipoHandler.getBank(event, "REC::Particle");
        DataBank trkBank = HipoHandler.getBank(event, "REC::Track");

        if (trjBank==null || ptcBank==null || trkBank==null) return null;

        ArrayList<TrajPoint[]> trajPoints = new ArrayList<TrajPoint[]>();

        // Loop through trajectory points.
        for (int trji=0; trji<trjBank.rows(); ++trji) {
            // Load trajectory variables.
            int detector = trjBank.getByte("detector", trji);
            int id  = trjBank.getShort("index", trji);
            int lyr = trjBank.getByte("layer", trji)-1;
            int pi  = trjBank.getShort("pindex", trji);
            int si  = -1;       // DC sector.
            double costh = -1; // track theta.

            // Use only FMT detector and valid FMT layers.
            if (detector!=DetectorType.FMT.getDetectorId() || lyr<0 || lyr>Constants.FMTLAYERS-1)
                continue;

            // Bank integrity is assumed from this point onward.
            if (lyr == 0) {
                trajPoints.add(new TrajPoint[Constants.FMTLAYERS]);
                for (int li = 0; li < Constants.FMTLAYERS; ++li)
                    trajPoints.get(trajPoints.size()-1)[li] = null;
            }

            if (trajPoints.size() == 0) return null;
            fcuts.increaseTrajCount();

            // Get DC sector of the track.
            for (int trki = 0; trki<trkBank.rows(); ++trki) {
                if (trkBank.getShort("pindex", trki) == pi) si = trkBank.getByte("sector", trki)-1;
            }

            // Get FMT layer's z coordinate and strips angle.
            double zRef   = fmtZ[lyr]     + shArr[lyr][2]; // Apply z shift.
            double phiRef = fmtAngle[lyr] - shArr[lyr][5]; // Apply phi shift.

            // Get particle's kinematics.
            double x  = (double) ptcBank.getFloat("vx", pi);
            double y  = (double) ptcBank.getFloat("vy", pi);
            double z  = (double) ptcBank.getFloat("vz", pi);
            double px = (double) ptcBank.getFloat("px", pi);
            double py = (double) ptcBank.getFloat("py", pi);
            double pz = (double) ptcBank.getFloat("pz", pi);
            int q     = (int)    ptcBank.getByte("charge", pi);

            if (applyCuts && fcuts.downstreamTrackCheck(z, zRef)) continue;
            double[] V = swim.swimToPlane(x,y,z,px,py,pz,q,zRef);

            x  = V[0] - shArr[lyr][0]; // Apply x shift.
            y  = V[1] - shArr[lyr][1]; // Apply y shift.
            z  = V[2];
            px = V[3];
            py = V[4];
            pz = V[5];

            // Get the track's theta angle.
            costh = Math.acos(pz/Math.sqrt(px*px+py*py+pz*pz));

            // Apply track fiducial cuts.
            if (applyCuts && fcuts.checkTrajCuts(x, y, z, zRef, costh)) continue;

            // Rotate (x,y) to FMT's local coordinate system.
            double xLoc = x*Math.cos(Math.toRadians(phiRef)) + y*Math.sin(Math.toRadians(phiRef));
            double yLoc = y*Math.cos(Math.toRadians(phiRef)) - x*Math.sin(Math.toRadians(phiRef));

            trajPoints.get(trajPoints.size()-1)[lyr] =
                    new TrajPoint(pi, id, lyr, si, z, xLoc, yLoc, costh);
        }

        // Clean trios.
        for (int arri = trajPoints.size()-1; arri >= 0; --arri) {
            int count = 0;
            for (int li = 0; li < Constants.FMTLAYERS; ++li)
                if (trajPoints.get(arri)[li] != null) count++;
            if (count >= minTrjPoints) continue;
            trajPoints.remove(arri);
        }

        return trajPoints;
    }
}
