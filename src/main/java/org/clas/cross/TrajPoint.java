package org.clas.cross;

import java.util.ArrayList;
import org.jlab.detector.base.DetectorType;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.clas.analysis.Data;
import org.clas.analysis.FiducialCuts;
import org.clas.analysis.TrkSwim;

public class TrajPoint {
    // Trajectory point data:
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
     * @return ArrayList of a trio of trajectory points, each on a different layer.
     */
    public static ArrayList<TrajPoint[]> getTrajPoints(DataEvent event, TrkSwim swim,
            FiducialCuts fcuts, double[] fmtZ, double[] fmtAngle, double[][] shArr,
            int minTrjPoints, boolean applyCuts) {
        // Sanitize input.
        if (minTrjPoints < 1 || minTrjPoints > 3) {
            System.err.printf("minTrjPoints should be at least 1 and at most 3!\n");
            return null;
        }

        // Get data banks.
        DataBank trjBank = Data.getBank(event, "REC::Traj");
        DataBank ptcBank = Data.getBank(event, "REC::Particle");
        DataBank trkBank = Data.getBank(event, "REC::Track");

        if (trjBank==null || ptcBank==null || trkBank==null) return null;

        ArrayList<TrajPoint[]> trajPoints = new ArrayList<TrajPoint[]>();

        // Loop through trajectory points.
        for (int trji=0; trji<trjBank.rows(); trji++) {
            // Load trajectory variables.
            int detector = trjBank.getByte("detector", trji);
            int id = trjBank.getShort("index", trji);
            int li = trjBank.getByte("layer", trji)-1;
            int pi = trjBank.getShort("pindex", trji);
            int si = -1; // DC sector.
            double costh = -1; // track theta.

            // Use only FMT layers 1, 2, and 3.
            if (detector!=DetectorType.FMT.getDetectorId() || li<0 || li> Constants.getNumberOfFMTLayers() -1)
                continue;

            // Bank integrity is being assumed in this line.
            if (li == 0) trajPoints.add(new TrajPoint[]{null, null, null});

            fcuts.increaseTrajCount();

            // Get DC sector of the track.
            for (int trki = 0; trki<trkBank.rows(); ++trki) {
                if (trkBank.getShort("pindex", trki) == pi) si = trkBank.getByte("sector", trki)-1;
            }

            // Get FMT layer's z coordinate and strips angle.
            double zRef = fmtZ[li] + shArr[0][0] + shArr[li+1][0];
            double phiRef = fmtAngle[li] - (shArr[0][3]+shArr[li+1][3]); // Apply phi shift.

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

            x  = V[0] - (shArr[0][1]+shArr[li+1][1]); // Apply global x shift.
            y  = V[1] - (shArr[0][2]+shArr[li+1][2]); // Apply global y shift.
            z  = V[2];
            px = V[3];
            py = V[4];
            pz = V[5];

            // Get the track's theta angle.
            costh = Math.acos(pz/Math.sqrt(px*px+py*py+pz*pz));

            // Apply track fiducial cuts.
            if (applyCuts && fcuts.checkTrajCuts(z, x, y, zRef, costh)) continue;

            // Rotate (x,y) to FMT's local coordinate system.
            double xLoc = x * Math.cos(Math.toRadians(phiRef))
                    + y * Math.sin(Math.toRadians(phiRef));
            double yLoc = y * Math.cos(Math.toRadians(phiRef))
                    - x * Math.sin(Math.toRadians(phiRef));

            trajPoints.get(trajPoints.size()-1)[li] =
                    new TrajPoint(pi, id, li, si, z, xLoc, yLoc, costh);
        }

        // Clean trios.
        for (int arri=trajPoints.size()-1; arri>=0; --arri) {
            if (minTrjPoints==1) {
                if (trajPoints.get(arri)[0]!=null) continue;
                if (trajPoints.get(arri)[1]!=null) continue;
                if (trajPoints.get(arri)[2]!=null) continue;
            }
            else if (minTrjPoints==2) {
                if (trajPoints.get(arri)[0]!=null && trajPoints.get(arri)[1]!=null) continue;
                if (trajPoints.get(arri)[1]!=null && trajPoints.get(arri)[2]!=null) continue;
                if (trajPoints.get(arri)[2]!=null && trajPoints.get(arri)[0]!=null) continue;
            }
            else if (minTrjPoints==3) {
                if (trajPoints.get(arri)[0]!=null
                        && trajPoints.get(arri)[1]!=null
                        && trajPoints.get(arri)[2]!=null)
                    continue;
            }

            trajPoints.remove(arri);
        }

        return trajPoints;
    }
}
