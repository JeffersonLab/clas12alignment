package org.clas.dc.alignment;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    
    private boolean tilted = true;
    private boolean global = false;
    
    public Table(boolean tilted, boolean global) {
        this.init(tilted, global);
    }

    public Table(IndexedTable table, boolean tilted, boolean global) {
        this.init(tilted, global);
        this.update(table);
    }

    private void init(boolean tilted, boolean global) {
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
        this.tilted = tilted;
        this.global = global;
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
            String axis = key.split("_")[1];
            value = this.alignment.getDoubleValue(prefix + axis, region, sector, 0);
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
           
    public GraphErrors getGraph(int sector, int icol, boolean rotation) {
        GraphErrors graph = new GraphErrors();
        Parameter[] pars = this.getParameters(sector);
        for(int i=0; i<Constants.NPARS; i++) {
            if(Constants.PARACTIVE[i] && pars[i].isRotation()==rotation) {
                int ix = i;
                if(rotation) ix -= 3;
                graph.addPoint(pars[i].value(), ix ,pars[i].error(), 0);
            }
        }
        graph.setMarkerColor(icol);
        graph.setMarkerSize(4);
        if(rotation) graph.setTitleX("Shift (deg)");
        else         graph.setTitleX("Shift (cm)");
        graph.setTitleY("Parameter");
        return graph;
    }
    
    public DataGroup getDataGroup(int icol) {
        DataGroup dg = new DataGroup(Constants.NSECTOR, 2);
        for(int i=0; i<Constants.NSECTOR; i++) {
            dg.addDataSet(this.getGraph(i+1, icol, false), i);
            dg.addDataSet(this.getGraph(i+1, icol, true),  i+Constants.NSECTOR);
        }
        return dg;
    }
    
    public Table copy() {
        Table t = new Table(this.tilted, this.global);
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
        for(int is=0; is<Constants.NSECTOR; is++) {
            Vector3D global = new Vector3D(0,0,0);
            for(int ir=0; ir<Constants.NREGION; ir++) {
                for(String n : names) {
                    if(table.hasEntry(ir+1, is+1, 0)) {
                        this.alignment.setDoubleValue(table.getDoubleValue(n, ir+1, is+1, 0), n, ir+1, is+1, 0);
                    }
                }
                Vector3D offset = new Vector3D(this.alignment.getDoubleValue("dx", ir+1, is+1, 0),
                                               this.alignment.getDoubleValue("dy", ir+1, is+1, 0),
                                               this.alignment.getDoubleValue("dz", ir+1, is+1, 0));
                offset.rotateZ(Math.toRadians(-60*is));
                if(this.tilted) offset.rotateY(Math.toRadians(-Constants.THTHILT));
                this.alignment.setDoubleValue(offset.x()-global.x(), "dx", ir+1, is+1, 0);
                this.alignment.setDoubleValue(offset.y()-global.y(), "dy", ir+1, is+1, 0);
                this.alignment.setDoubleValue(offset.z()-global.z(), "dz", ir+1, is+1, 0);            
                if(ir==0 && this.global) global.copy(offset);
            }
        }

    }
            
    public void update(int sector, Parameter[] pars) {
        for(int ir=0; ir<Constants.NREGION; ir++) {
            this.alignment.setDoubleValue(pars[ir*6+0].value(), "dx", ir+1, sector, 0);
            this.alignment.setDoubleValue(pars[ir*6+1].value(), "dy", ir+1, sector, 0);
            this.alignment.setDoubleValue(pars[ir*6+2].value(), "dz", ir+1, sector, 0);
            this.alignment.setDoubleValue(pars[ir*6+0].error(), "ex", ir+1, sector, 0);
            this.alignment.setDoubleValue(pars[ir*6+1].error(), "ey", ir+1, sector, 0);
            this.alignment.setDoubleValue(pars[ir*6+2].error(), "ez", ir+1, sector, 0);
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
                s += String.format("%4d %4d %4d   %10.4f \u00B1 %.4f %10.4f \u00B1 %.4f %10.4f \u00B1 %.4f   %10.4f \u00B1 %.4f %10.4f \u00B1 %.4f %10.4f \u00B1 %.4f\n",
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
    
    public Table toCLAS12Frame() {
        Table transformed = this.copy();
        for(int ir=0; ir<Constants.NREGION; ir++) {
            for(int is=0; is<Constants.NSECTOR; is++) {
                Vector3D offset = new Vector3D(this.alignment.getDoubleValue("dx", ir+1, is+1, 0),
                                               this.alignment.getDoubleValue("dy", ir+1, is+1, 0),
                                               this.alignment.getDoubleValue("dz", ir+1, is+1, 0));
                if(this.tilted) offset.rotateY(Math.toRadians(Constants.THTHILT));
                offset.rotateZ(Math.toRadians(60*is));
                transformed.alignment.setDoubleValue(offset.x(), "dx", ir+1, is+1, 0);
                transformed.alignment.setDoubleValue(offset.y(), "dy", ir+1, is+1, 0);
                transformed.alignment.setDoubleValue(offset.z(), "dz", ir+1, is+1, 0);
                transformed.alignment.setDoubleValue(0.0, "ex", ir+1, is+1, 0);
                transformed.alignment.setDoubleValue(0.0, "ey", ir+1, is+1, 0);
                transformed.alignment.setDoubleValue(0.0, "ez", ir+1, is+1, 0);
            }
        }
        return transformed;
    }
    
    public String toCCDBTable() {
        Table transformed = this.toCLAS12Frame();
        String s = "";
        for(int is=0; is<Constants.NSECTOR; is++) {
            Vector3D global = new Vector3D(0,0,0);
            for(int ir=0; ir<Constants.NREGION; ir++) {
                s += String.format("%4d %4d %4d   %10.4f %10.4f %10.4f   %10.4f %10.4f %10.4f\n",
                        (ir+1), (is+1), 0,
                        transformed.alignment.getDoubleValue("dx", ir+1, is+1, 0)+global.x(),
                        transformed.alignment.getDoubleValue("dy", ir+1, is+1, 0)+global.y(),
                        transformed.alignment.getDoubleValue("dz", ir+1, is+1, 0)+global.z(),
                        transformed.alignment.getDoubleValue("dtheta_x", ir+1, is+1, 0),
                        transformed.alignment.getDoubleValue("dtheta_y", ir+1, is+1, 0),
                        transformed.alignment.getDoubleValue("dtheta_z", ir+1, is+1, 0));
                if(ir==0 && this.global) 
                    global.setXYZ(transformed.alignment.getDoubleValue("dx", ir+1, is+1, 0),
                                  transformed.alignment.getDoubleValue("dy", ir+1, is+1, 0),
                                  transformed.alignment.getDoubleValue("dz", ir+1, is+1, 0));
            }
        }
        return s;
    }
    
    public void toFile(String filename) {
        try {
            FileWriter fileWriter = new FileWriter(filename);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(this.toCCDBTable());
            bufferedWriter.close();
            Logger.getLogger(Constants.LOGGERNAME).log(Level.INFO, "Alignment constants saved to file " + filename);
        } catch (IOException ex) {
            Logger.getLogger(Constants.LOGGERNAME).log(Level.SEVERE, "Error saving alignment constants to file", ex);
        }

    }
}
