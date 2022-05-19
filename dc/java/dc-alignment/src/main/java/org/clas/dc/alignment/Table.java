package org.clas.dc.alignment;

import org.jlab.geom.prim.Vector3D;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.group.DataGroup;
import org.jlab.utils.groups.IndexedTable;

/**
 *
 * @author devita
 */
public class Table {
    
    private final String[] names = {"dx", "ex", "dy", "ey", "dz", "ez", "dtheta_x", "etheta_x", "dtheta_y", "etheta_y", "dtheta_z", "etheta_z"}; 
    private IndexedTable alignment;
    
    public Table() {
        this.init();
    }

    public Table(IndexedTable table) {
        this.init();
        this.update(table);
    }

    public final void init() {
        String[] nameTypes = new String[names.length];
        for(int i=0; i<names.length; i++) 
            nameTypes[i] = names[i]+"/F";
        this.alignment = new IndexedTable(3, nameTypes);
        this.alignment.setIndexName(0, "region");
        this.alignment.setIndexName(1, "sector");
        this.alignment.setIndexName(2, "component");
        for(int ir=0; ir<Constants.NREGION; ir++) {
            for(int is=0; is<Constants.NSECTOR; is++) {
                this.alignment.addEntry(ir+1, is+1, 0);
                for(String n : names) {
                    this.alignment.setDoubleValue(0.0, n, ir+1, is+1, 0);
                }
            }
        }
    }
        
    public double getShiftSize(String key, int sector) {
        return this.getElement(key, "d", sector);
    }
    
    public double getShiftError(String key, int sector) {
        return this.getElement(key, "e", sector);
    }
    
    public double getElement(String key, String prefix, int sector) {
        double value = 0;
        int region = Integer.parseInt(key.split("_")[0].substring(1));
        if(key.contains("_c")) {  // rotation
            String axis = key.split("_c")[1];
            value = this.alignment.getDoubleValue(prefix + "theta_" + axis, region, sector, 0);
        }
        else { // offset
            Vector3D offset = new Vector3D(this.alignment.getDoubleValue(prefix + "x", region, sector, 0),
                                           this.alignment.getDoubleValue(prefix + "y", region, sector, 0),
                                           this.alignment.getDoubleValue(prefix + "z", region, sector, 0));
            offset.rotateZ(Math.toRadians(-60*(sector-1)));
            String axis = key.split("_")[1];
            switch (axis) {
                case "x":
                    value = offset.x();
                    break;
                case "y":
                    value = offset.y();
                    break;
                case "z":
                    value = offset.z();
                    break;
                default:
                    break;
            }
        }
        return value;
    }
    
    public Parameter[] getParameters(int sector) {
        Parameter[] pars = new Parameter[Constants.NPARS];
        for(int i=0; i<Constants.NPARS; i++) {
            pars[i]  = new Parameter(Constants.PARNAME[i], 
                                     this.getShiftSize(Constants.PARNAME[i], sector),
                                     this.getShiftError(Constants.PARNAME[i], sector),
                                     Constants.PARSTEP[i],
                                    -Constants.PARMAX[i],
                                     Constants.PARMAX[i]);
        }
        return pars;
    }
        
    public double[] getValues(int sector) {
        double[] pars = new double[Constants.NPARS];
        for(int i=0; i<Constants.NPARS; i++) {
            pars[i]  = this.getShiftSize(Constants.PARNAME[i], sector);
        }
        return pars;
    }
        
    public double[] getErrors(int sector) {
        double[] pars = new double[Constants.NPARS];
        for(int i=0; i<Constants.NPARS; i++) {
            pars[i]  = this.getShiftError(Constants.PARNAME[i], sector);
        }
        return pars;
    }
           
    public GraphErrors getGraph(int sector, int icol) {
        GraphErrors graph = new GraphErrors();
        for(int i=0; i<Constants.NPARS; i++) {
            if(Constants.PARSTEP[i]>0)
                graph.addPoint(this.getShiftSize(Constants.PARNAME[i], sector), i, 
                               this.getShiftError(Constants.PARNAME[i], sector), 0);
        }
        graph.setMarkerColor(icol);
        graph.setMarkerSize(4);
        graph.setTitleX("Shift (cm/deg)");
        graph.setTitleY("Parameter");
        return graph;
    }
    
    public DataGroup getDataGroup(int icol) {
        DataGroup dg = new DataGroup(Constants.NSECTOR, 1);
        for(int i=0; i<Constants.NSECTOR; i++) {
            dg.addDataSet(this.getGraph(i+1, icol), i);
        }
        return dg;
    }
    
    public Table copy() {
        Table t = new Table();
        for(int ir=0; ir<Constants.NREGION; ir++) {
            for(int is=0; is<Constants.NSECTOR; is++) {
                for(String n : names) {
                    t.alignment.setDoubleValue(this.alignment.getDoubleValue(n, ir+1, is+1, 0), n, ir+1, is+1, 0);
                }
            }
        }
        return t;
    }    
    
    
    public Table add(Table table) {
        Table summed = this.copy();
        for(int ir=0; ir<Constants.NREGION; ir++) {
            for(int is=0; is<Constants.NSECTOR; is++) {
                for(String n : names) {
                    if(table.alignment.hasEntry(ir+1, is+1, 0)) {
                        summed.alignment.setDoubleValue(summed.alignment.getDoubleValue(n, ir+1, is+1, 0)
                                                        +table.alignment.getDoubleValue(n, ir+1, is+1, 0)
                                                       , n, ir+1, is+1, 0);
                    }
                }
            }
        }
        return summed;
    }

    public Table subtract(Table table) {
        Table summed = this.copy();
        for(int ir=0; ir<Constants.NREGION; ir++) {
            for(int is=0; is<Constants.NSECTOR; is++) {
                for(String n : names) {
                    if(table.alignment.hasEntry(ir+1, is+1, 0)) {
                        summed.alignment.setDoubleValue(summed.alignment.getDoubleValue(n, ir+1, is+1, 0)
                                                        -table.alignment.getDoubleValue(n, ir+1, is+1, 0)
                                                       , n, ir+1, is+1, 0);
                    }
                }
            }
        }
        return summed;
    }

    public final void update(IndexedTable table) {
        for(int ir=0; ir<Constants.NREGION; ir++) {
            for(int is=0; is<Constants.NSECTOR; is++) {
                for(String n : names) {
                    if(table.hasEntry(ir+1, is+1, 0)) {
                        this.alignment.setDoubleValue(table.getDoubleValue(n, ir+1, is+1, 0), n, ir+1, is+1, 0);
                    }
                }
            }
        }

    }
            
    public void update(int sector, Parameter[] pars) {
        for(int ir=0; ir<Constants.NREGION; ir++) {
            Vector3D offset = new Vector3D(pars[ir*6+0].value(),pars[ir*6+1].value(),pars[ir*6+2].value());
            Vector3D errors = new Vector3D(pars[ir*6+0].error(),pars[ir*6+1].error(),pars[ir*6+2].error());
            offset.rotateZ(Math.toRadians(60*(sector-1)));
            this.alignment.setDoubleValue(offset.x(), "dx", ir+1, sector, 0);
            this.alignment.setDoubleValue(offset.y(), "dy", ir+1, sector, 0);
            this.alignment.setDoubleValue(offset.z(), "dz", ir+1, sector, 0);
            this.alignment.setDoubleValue(errors.x(), "ex", ir+1, sector, 0);
            this.alignment.setDoubleValue(errors.y(), "ey", ir+1, sector, 0);
            this.alignment.setDoubleValue(errors.z(), "ez", ir+1, sector, 0);
            this.alignment.setDoubleValue(pars[ir*6+3].value(), "dtheta_x", ir+1, sector, 0);
            this.alignment.setDoubleValue(pars[ir*6+4].value(), "dtheta_y", ir+1, sector, 0);
            this.alignment.setDoubleValue(pars[ir*6+5].value(), "dtheta_z", ir+1, sector, 0);
            this.alignment.setDoubleValue(pars[ir*6+3].error(), "etheta_x", ir+1, sector, 0);
            this.alignment.setDoubleValue(pars[ir*6+4].error(), "etheta_y", ir+1, sector, 0);
            this.alignment.setDoubleValue(pars[ir*6+5].error(), "etheta_z", ir+1, sector, 0);
        }
    }
            
    public void reset() {
        for(int ir=0; ir<Constants.NREGION; ir++) {
            for(int is=0; is<Constants.NSECTOR; is++) {
                for(String n : names) {
                    this.alignment.setDoubleValue(0.0, n, ir+1, is+1, 0);
                }
            }
        }
    }
    
    @Override
    public String toString() {
        String s = "";
        for(int ir=0; ir<Constants.NREGION; ir++) {
            for(int is=0; is<Constants.NSECTOR; is++) {
                s += String.format("%4d %4d %4d   %10.4f/%.4f %10.4f/%.4f %10.4f/%.4f   %10.4f/%.4f %10.4f/%.4f %10.4f/%.4f\n",
                        (ir+1), (is+1), 0,
                        this.alignment.getDoubleValue("dx", ir+1, is+1, 0),
                        this.alignment.getDoubleValue("ex", ir+1, is+1, 0),
                        this.alignment.getDoubleValue("dy", ir+1, is+1, 0),
                        this.alignment.getDoubleValue("ey", ir+1, is+1, 0),
                        this.alignment.getDoubleValue("dz", ir+1, is+1, 0),
                        this.alignment.getDoubleValue("ez", ir+1, is+1, 0),
                        this.alignment.getDoubleValue("dtheta_x", ir+1, is+1, 0),
                        this.alignment.getDoubleValue("etheta_x", ir+1, is+1, 0),
                        this.alignment.getDoubleValue("dtheta_y", ir+1, is+1, 0),
                        this.alignment.getDoubleValue("etheta_y", ir+1, is+1, 0),
                        this.alignment.getDoubleValue("dtheta_z", ir+1, is+1, 0),
                        this.alignment.getDoubleValue("etheta_z", ir+1, is+1, 0));
            }
        }
        return s;
    }
    
    public String toTextTable() {
        String s = "";
        for(int ir=0; ir<Constants.NREGION; ir++) {
            for(int is=0; is<Constants.NSECTOR; is++) {
                s += String.format("%4d %4d %4d   %10.4f %10.4f %10.4f   %10.4f %10.4f %10.4f\n",
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
