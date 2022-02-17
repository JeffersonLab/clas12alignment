package org.clas.dc.alignment;

import org.jlab.geom.prim.Vector3D;
import org.jlab.utils.groups.IndexedTable;

/**
 *
 * @author devita
 */
public class Table {
    
    private IndexedTable alignment;
    
    public Table(IndexedTable table) {
        this.alignment = table;
    }

    public IndexedTable getTable() {
        return alignment;
    }
        
    public double getShiftSize(String key, int sector) {
        double shift = 0;
        int region = Integer.parseInt(key.split("_")[0].substring(1));
        if(key.contains("_c")) {  // rotation
            String axis = key.split("_c")[1];
            shift = this.alignment.getDoubleValue("dtheta_" + axis, region, sector, 0);
        }
        else { // offset
            Vector3D offset = new Vector3D(this.alignment.getDoubleValue("dx", region, sector, 0),
                                           this.alignment.getDoubleValue("dy", region, sector, 0),
                                           this.alignment.getDoubleValue("dz", region, sector, 0));
            offset.rotateZ(Math.toRadians(-60*(sector-1)));
            String axis = key.split("_")[1];
            switch (axis) {
                case "x":
                    shift = offset.x();
                    break;
                case "y":
                    shift = offset.y();
                    break;
                case "z":
                    shift = offset.z();
                    break;
                default:
                    break;
            }
        }
        return shift;
    }
    
    public Parameter[] getParameters(int sector) {
        Parameter[] pars = new Parameter[18];
        for(int i=0; i<Constants.NPARS; i++) {
            pars[i]  = new Parameter(Constants.PARNAME[i],  this.getShiftSize(Constants.PARNAME[i], sector), Constants.PARSTEP[i]);
        }
        return pars;
    }
        
        
    public void update(int sector, Parameter[] pars) {
        for(int ir=0; ir<Constants.NREGION; ir++) {
            Vector3D offset = new Vector3D(pars[ir*6+0].value(),pars[ir*6+1].value(),pars[ir*6+2].value());
            offset.rotateZ(Math.toRadians(60*(sector-1)));
            this.alignment.setDoubleValue(offset.x(), "dx", ir+1, sector, 0);
            this.alignment.setDoubleValue(offset.y(), "dy", ir+1, sector, 0);
            this.alignment.setDoubleValue(offset.z(), "dz", ir+1, sector, 0);
            this.alignment.setDoubleValue(pars[ir*6+3].value(), "dtheta_x", ir+1, sector, 0);
            this.alignment.setDoubleValue(pars[ir*6+4].value(), "dtheta_y", ir+1, sector, 0);
            this.alignment.setDoubleValue(pars[ir*6+5].value(), "dtheta_z", ir+1, sector, 0);
        }
    }
            
    public void reset() {
        for(int is=0; is<Constants.NSECTOR; is++) {
            for(int ir=0; ir<Constants.NREGION; ir++) {
                this.alignment.setDoubleValue(0.0, "dx", ir+1, is+1, 0);
                this.alignment.setDoubleValue(0.0, "dy", ir+1, is+1, 0);
                this.alignment.setDoubleValue(0.0, "dz", ir+1, is+1, 0);
                this.alignment.setDoubleValue(0.0, "dtheta_x", ir+1, is+1, 0);
                this.alignment.setDoubleValue(0.0, "dtheta_y", ir+1, is+1, 0);
                this.alignment.setDoubleValue(0.0, "dtheta_z", ir+1, is+1, 0);
            }
        }
    }
    
    public String toString() {
        String s = "";
        for(int ir=0; ir<Constants.NREGION; ir++) {
            for(int is=0; is<Constants.NSECTOR; is++) {
                s += String.format("%6d %6d %6d %14.4f %14.4f %14.4f  %14.4f %14.4f %14.4f\n",
                        (ir+1), (is+1), 0,
                        this.alignment.getDoubleValue("dx", ir+1, is+1, 0),
                        this.alignment.getDoubleValue("dy", ir+1, is+1, 0),
                        this.alignment.getDoubleValue("dz", ir+1, is+1, 0),
                        this.alignment.getDoubleValue("dtheta_x", ir+1, is+1, 0),
                        this.alignment.getDoubleValue("dtheta_y", ir+1, is+1, 0),
                        this.alignment.getDoubleValue("dtheta_z", ir+1, is+1, 0));
            }
        }
        return s;
    }
    
}
