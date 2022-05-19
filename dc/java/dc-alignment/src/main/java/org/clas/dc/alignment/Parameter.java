package org.clas.dc.alignment;

import org.jlab.groot.math.UserParameter;

/**
 *
 * @author devita
 */
public class Parameter extends UserParameter {
    
    private int region;
    private int axis;
    private static String[] axisNames = {"x", "y", "z"};
    private boolean rotation;
    
    
    public Parameter(int region, int axis, boolean rotation, double stepsize) {
        super(Parameter.getName(region, axis, rotation), 0);
        this.setStep(stepsize);
    }
    
    public Parameter(String name, double value, double error, double stepsize) {
        super(name, value);
        this.setError(error);
        this.setStep(stepsize);
        this.initFromName(name);
    }
    
    public Parameter(String name, double value, double error, double stepsize, double min, double max) {
        super(name, value);
        this.setError(error);
        this.setStep(stepsize);
        this.setLimits(min, max);
        this.initFromName(name);
    }
    
    public int getAxis() {
        return axis;
    }
    
    public int getAxis(String name) {
        if(!name.matches("[xyz]")) {
            throw new IllegalArgumentException("Error: unknown axis name");
        }
        for(int i=0; i<3; i++) {
            if(axisNames[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }
    
    public String getAxisName() {
        return axisNames[axis];
    }

    public static String getName(int region, int axis, boolean rotation) {
        return Parameter.getName(region, axisNames[axis], rotation);
    }
    
    public static String getName(int region, String axis, boolean rotation) {
        if(rotation)
            return "r" + region + "_c" + axis;
        else 
            return "r" + region + "_" + axis;
    }

    public int getRegion() {
        return region;
    }

    public boolean isRotation() {
        return rotation;
    }
    
    public final void initFromName(String name) {
        if(name.matches("r[123]_[xyz]")) {
            boolean rotation = false;
            this.region = Integer.parseInt(name.split("_")[0].split("r")[1]);
            this.axis   = this.getAxis(name.split("_")[1]);
        }
        else if(name.matches("r[123]_c[xyz]")) {
            boolean rotation = true;
            this.region = Integer.parseInt(name.split("_")[0].split("r")[1]);
            this.axis   = this.getAxis(name.split("_")[1].split("c")[1]);
        }
        else {
            throw new IllegalArgumentException("Error: unknown parameter name");            
        }
    }    
    
    public Parameter copy() {
        Parameter p = new Parameter(this.name(), this.value(), this.error(), this.getStep(), this.min(), this.max());
        p.initFromName(p.name());
        return p;
    }

}
