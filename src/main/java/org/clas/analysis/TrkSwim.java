package org.clas.analysis;

import org.jlab.geom.prim.Vector3D;
import org.jlab.clas.swimtools.MagFieldsEngine;
import org.jlab.clas.swimtools.Swim;
import org.jlab.clas.swimtools.Swimmer;

public class TrkSwim {
    Swim swim;

    /**
     * Setup the Swim class.
     * @param swmSetup  : Swimm setup array:
     *                    - [0] : solenoid magnet scale.
     *                    - [1] : torus magnet scale.
     *                    - [2] : torus magnet shift.
     * @return Swim instance.
     */
    public TrkSwim(double[] swmSetup) {
        if (swmSetup.length != 3) {
            System.out.printf("swmSetup should have a size of 3. Read the ");
            System.out.printf("method's description.\n");
        }

        MagFieldsEngine mf = new MagFieldsEngine();
        mf.initializeMagneticFields();
        Swimmer.setMagneticFieldsScales(swmSetup[0], swmSetup[1], swmSetup[2]);

        swim = new Swim();
    }

    /**
     * Translate trajectory using swimmer.
     * @param swimmer        : Swim instance.
     * @param x,y,z,px,py,pz : Kinematics to setup swimmer.
     * @param q              : Particle's charge.
     * @param zTarget        : z plane where to swim.
     * @return Array with data after swimming to z.
     */
    public double[] swimToPlane(double x, double y, double z,
            double px, double py, double pz, int q, double zTarget) {
        swim.SetSwimParameters(x,y,z,px,py,pz,q);
        return swim.SwimToPlaneBoundary(zTarget, new Vector3D(0,0,1), 1);
    }
}



