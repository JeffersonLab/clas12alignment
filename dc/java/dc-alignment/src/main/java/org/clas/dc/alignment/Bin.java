package org.clas.dc.alignment;

/**
 *
 * @author devita
 */
public class Bin {
    
    private double low;
    private double high;

    public Bin(double low, double high){
        this.low = low;
        this.high = high;
    }

    public boolean contains(double number){
        return (number >= low && number <= high);
    }

    public String getRange() {
        String range = null;
        if(low==-Double.MAX_VALUE && high==Double.MAX_VALUE) range = "all";
        else if(low==-Double.MAX_VALUE) range = "<" + high;
        else if(high==Double.MAX_VALUE) range = ">" + low;
        else range = low + "-" + high;
        return range;
    }

    public double getMean() {
        return (low+high)/2;
    }
    
    public double getMin() {
        return low;
    }
    
    public double getMax() {
        return high;
    }
    
    public double getWidth() {
        return high-low;
    }
    
    @Override
    public String toString() {
        return "\tbin: " + low + "-" + high;
    }
}
