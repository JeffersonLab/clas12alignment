package org.clas.dc.alignment;

import org.jlab.jnp.hipo4.data.Bank;
import org.jlab.jnp.hipo4.data.Event;
import org.jlab.jnp.hipo4.data.SchemaFactory;
import org.jlab.jnp.hipo4.io.HipoReader;

/**
 *
 * @author devita
 */
public class FileHandler {
    
    private HipoReader readNominal = null;
    private HipoReader readShifted = null;
    
    private SchemaFactory schema = new SchemaFactory();
    
    private int eventNumber = -1;
    private Event nominal = new Event();
    private Event shifted = new Event();
    
    public FileHandler(String nominal) {
        this.readNominal = new HipoReader();
        this.readNominal.open(nominal);
        this.schema = this.readNominal.getSchemaFactory();
    }
    
    public FileHandler(String nominal, String shifted) {
        this(nominal);
        if(shifted!=null) {
            this.readShifted = new HipoReader();
            this.readShifted.open(shifted);
        }
    }
    
    public void close() {
        this.readNominal.close();
        if(readShifted!=null) this.readShifted.close();
    }
    
    public Event nominal() {
        return nominal;
    }
        
    public Event shifted() {
        if(readShifted!=null)
            return shifted;
        else
            return null;
    }
    
    private boolean getEvent(HipoReader reader, Event event) {
        if(reader.hasNext()){
            reader.nextEvent(event);
            return true;
        }
        return false;
    }
    
    public boolean getNext() {
        if(this.readNominal!=null && this.readShifted==null) {
            return this.getEvent();
        }
        else if(this.readNominal!=null && this.readShifted!=null) {
            return this.getEvent(nominal, shifted);
        }
        else
            return false;
    }
    
    public boolean getEvent() {
        if(this.getEvent(readNominal, nominal)) {
            this.eventNumber = this.getEventNumber(nominal);
            return true;
        }
        return false;
    }  
    
    public boolean getEvent(Event nominal, Event shifted) {
        if(readNominal.hasNext() && readShifted.hasNext()){
            this.getEvent(readNominal, nominal);
            eventNumber = this.getEventNumber(nominal);
            
            if(eventNumber>0) {
                int shiftedNumber = -1;
                while(shiftedNumber<eventNumber && this.getEvent(readShifted, shifted)) {
                    shiftedNumber = this.getEventNumber(shifted);
                }
                if(shiftedNumber==eventNumber) return true;
            }
            else
                return true;
        }
        return false;
    }
    
    private int getEventNumber(Event event) {
        
        int eventNumber = -1;
        
        Bank runConfig = new Bank(schema.getSchema("RUN::config"));
        
        if(runConfig!=null) {
            event.read(runConfig);            
            if(runConfig.getRows()>0) eventNumber = runConfig.getInt("event", 0);
        }
        return eventNumber;
    }
    
    public int getEventNumber() {
        return this.eventNumber;
    }
    
    public SchemaFactory getSchema() {
        return this.schema;
    }
}
