package org.clas.analysis;

import cnuphys.magfield.MagneticFieldInitializationException;
import cnuphys.magfield.MagneticFields;
import java.io.FileNotFoundException;
import org.clas.test.Constants;
import org.jlab.geom.prim.Vector3D;
import org.jlab.clas.swimtools.Swim;
import org.jlab.utils.CLASResources;

/** Class in charge of all the swimming through the magnetic field. */
public class TrkSwim {
    Swim swim;
    Vector3D dir;

    /**
     * Setup the Swim class.
     * @param swmSetup Swim setup array:
     *                   * [0] : solenoid magnet scale.
     *                   * [1] : torus magnet scale.
     *                   * [2] : solenoid magnet shift.
     * @param yaw
     * @param pitch
     */
    public TrkSwim(double[] swmSetup, double yaw, double pitch) {
        

        String mapDir = CLASResources.getResourcePath("etc")+"/data/magfield";
        try {
            MagneticFields.getInstance().initializeMagneticFields(mapDir, Constants.TORUSMAP, Constants.SOLENOIDMAP);
        }
        catch (MagneticFieldInitializationException | FileNotFoundException e) {
            e.printStackTrace();
        }
        MagneticFields.getInstance().getSolenoid().setScaleFactor(swmSetup[0]);
        MagneticFields.getInstance().getTorus().setScaleFactor(swmSetup[1]);
        MagneticFields.getInstance().getSolenoid().setShiftZ(swmSetup[2]);
        
        // Obtain the plane angle from the yaw and the pitch.
        double x = -Math.sin(Math.toRadians(yaw));
        double y = Math.sin(Math.toRadians(pitch));
        double z = Math.cos(Math.toRadians(yaw))*Math.cos(Math.toRadians(pitch));
        this.dir = new Vector3D(x,y,z);
        this.swim = new Swim();
    }

    /**
     * Translate trajectory using swimmer.
     * @param x,y,z,px,py,pz Kinematics to setup swimmer.
     * @param q              Particle's charge.
     * @param zTarget        z plane where to swim.
     * @return Array with data after swimming to z.
     */
    public double[] swimToPlane(
        double x, double y, double z, double px, double py, double pz, int q,
        double zTarget
    ) {
        swim.SetSwimParameters(x, y, z, px, py, pz, q);
        return swim.SwimToPlaneBoundary(zTarget, this.dir, 1);
    }
}
