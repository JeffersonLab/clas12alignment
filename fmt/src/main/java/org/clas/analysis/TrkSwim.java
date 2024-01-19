package org.clas.analysis;

import org.jlab.geom.prim.Vector3D;
import org.jlab.clas.swimtools.MagFieldsEngine;
import org.jlab.clas.swimtools.Swim;
import org.jlab.clas.swimtools.Swimmer;

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
     */
    public TrkSwim(double[] swmSetup, double yaw, double pitch) {
        MagFieldsEngine mf = new MagFieldsEngine();
        mf.initializeMagneticFields();
        Swimmer.setMagneticFieldsScales(swmSetup[0], swmSetup[1], swmSetup[2]);

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
