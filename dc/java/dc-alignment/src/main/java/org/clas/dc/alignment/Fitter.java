package org.clas.dc.alignment;

import java.util.List;
import java.util.logging.Logger;
import org.freehep.math.minuit.FCNBase;
import org.freehep.math.minuit.FunctionMinimum;
import org.freehep.math.minuit.MinosError;
import org.freehep.math.minuit.MnMigrad;
import org.freehep.math.minuit.MnMinos;
import org.freehep.math.minuit.MnPlot;
import org.freehep.math.minuit.MnScan;
import org.freehep.math.minuit.MnUserParameters;
import org.freehep.math.minuit.Point;
import org.jlab.groot.math.UserParameter;

/**
 *
 * @author devita
 */
public class Fitter  implements FCNBase {
   
    private static double EPS = 0.000001;
    private Parameter[]           pars = new Parameter[Constants.NPARS];
    private double[]        trueValues = null; // 37 'layers'
    private double[][][] currentValues = null; // 37 'layers', nTheta, nPhi
    private double[][][] currentErrors = null; // 37 'layers', nTheta, nPhi
    private double[][][][]  unitShifts = null; // 12 distortions, 37 'layers', nTheta, nPhi
    private int nPar;
    private int nLayer;
    private int nTheta;
    private int nPhi;
    private double chi2;        
    private int    ndf;        
    
    private int       numberOfCalls = 0;
    private long      startTime     = 0L;
    private long      endTime       = 0L;
    private boolean   status;
    
    private static final Logger LOGGER = Logger.getLogger(Constants.LOGGERNAME);
    
    public Fitter(double[][][][] shifts, double [][][] residuals, double[][][] errors) {
        if(shifts.length!=Constants.NPARS) 
            throw new IllegalArgumentException("Error: invalid number of parameters " + shifts.length);
        this.nPar   = shifts.length;
        this.nLayer = shifts[0].length;
        this.nTheta = shifts[0][0].length;
        this.nPhi   = shifts[0][0][0].length;
        this.unitShifts    = shifts;
        this.currentValues = residuals;
        this.currentErrors = errors;
        this.initParameters();
        startTime = System.currentTimeMillis();        
        endTime = System.currentTimeMillis();        
    }
    
    private void initParameters() {
        for(int i=0; i<Constants.NPARS; i++) {
            pars[i] = new Parameter(Constants.PARNAME[i], 0, 0, Constants.PARSTEP[i], -Constants.PARMAX[i], Constants.PARMAX[i]);
        }
        this.getChi2(this.getParArray());
    }
 
    
    @Override
    public double valueOf(double[] pars) {
        double chi2 = getChi2(pars);
        numberOfCalls++;
        endTime = System.currentTimeMillis();
        /*
        if(numberOfCalls%10==0){
            System.out.println("********************************************************");
            System.out.println( " Number of calls =  " + numberOfCalls + " CHI 2 " + chi2);
            function.show();
        }*/
        //function.show();
        //System.err.println("\n************ CHI 2 = " + chi2);
        return chi2;        
    }
    
    
    public double getChi2(double[] pars){
        
        double chi2 = 0.0;
        int    ndf  = 0;
        for(int im=0; im<currentValues.length; im++) {
            for(int it=0; it<currentValues[0].length; it++) {
                for(int ip=0; ip<currentValues[0][0].length; ip++) {
                    
                    double norm = currentErrors[im][it][ip]*currentErrors[im][it][ip];
                    if(norm>1E-12) {
                        chi2 += Math.pow(this.value(im, it, ip, pars)-currentValues[im][it][ip],2)*Constants.MEASWEIGHT[im]/norm;
                        ndf++;
                    }
                }
            }
        }
        this.setChi2(chi2);
        this.setNDF(ndf-pars.length);
        return chi2;
    }

    private double value(int iLayer, int iTheta, int iPhi, double[] pars) {
        double value = 0;
        for(int i=0; i<pars.length; i++) {
            value += unitShifts[i][iLayer][iTheta][iPhi]*pars[i];
        }
        return value;
    }

    public double getChi2() {
        return chi2;
    }

    public void setChi2(double chi2) {
        this.chi2 = chi2;
    }

    public int getNDF() {
        return ndf;
    }

    
    public void setNDF(int ndf) {
        this.ndf = ndf;
    }

    public boolean getStatus() {
        return status;
    }

    public Parameter[] getPars() {
        return pars;
    }

    public Parameter[] getParCopy() {
        Parameter[] parCopy = new Parameter[this.pars.length];
        for(int i=0; i<pars.length; i++) {
            parCopy[i] = pars[i].copy();
        }
        return parCopy;
    }

    public double[] getParArray() {
        double[] parArray = new double[pars.length];
        for(int i=0; i<pars.length; i++)
            parArray[i] = pars[i].value();
        return parArray;
    }
    
    public void randomizePars(Parameter[] oldPars) {
        if(oldPars == null) return;
        for(int i=0; i<pars.length; i++) {
            if(pars[i].getStep()>0) {
                pars[i].setValue(oldPars[i].getStep()*(Math.random()*2-1));
            }
        }
        this.getChi2(this.getParArray());
    }
    
    public void randomizePars() {
        for(int i=0; i<pars.length; i++) {
            if(pars[i].getStep()>0) {
                pars[i].setValue(pars[i].value()+pars[i].error()*3*(Math.random()*2-1));
                pars[i].setStep(pars[i].getStep()/2);
            }
        }
        this.getChi2(this.getParArray());
    }
    
    public void setPars(Parameter[] pars) {
        this.pars = pars;
        double[] parArray = new double[pars.length];
        for(int i=0; i<pars.length; i++)
            parArray[i] = pars[i].value();
        this.getChi2(parArray);
    }
    
    public void zeroPars() {
        for(int i=0; i<pars.length; i++) {
            pars[i].setValue(0);
        }
        this.getChi2(this.getParArray());
    }

    public void fit(String options, int nTry) {
                
        if(nTry <= 0) {
            nTry = 1;
        }
        
        for(int i=0; i<nTry; i++) {
            if(i>0) this.randomizePars();
            this.fit(options);
        }
        
    }
    
    public void fit(String options){
        
        
        try{

            MnUserParameters upar = new MnUserParameters();
            
            if(options.contains("G")) {
                for(int loop = 0; loop < pars.length; loop++){
                    UserParameter par = this.pars[loop%6];
	            upar.add(par.name(),par.value(),par.getStep());
	            if(par.getStep()<EPS || (loop%6)>=3){
	                upar.fix(par.name());
	            }
	            if(par.min()>-1e9&&par.max()<1e9){
	                upar.setLimits(par.name(), par.min(), par.max());
	            }
	        }
            }
            else {
	        for(int loop = 0; loop < pars.length; loop++){
                    UserParameter par = this.pars[loop];
	            upar.add(par.name(),par.value(),par.getStep());
	            if(par.getStep()<EPS){
	                upar.fix(par.name());
	            }
	            if(par.min()>-1e9&&par.max()<1e9){
	                upar.setLimits(par.name(), par.min(), par.max());
	            }
	        }
            }
	        
	        MnScan  scanner = new MnScan(this,upar);
                for(int i = 0; i < pars.length; i++){
                    if(pars[i].getStep()>EPS) {
                        List<Point> points = scanner.scan(i);
                        if(options.contains("V") && pars[i].getStep()>EPS) {
                            MnPlot plot = new MnPlot();
                            plot.plot(points);
                        }
                    }
                }
	        FunctionMinimum scanmin = scanner.minimize(); 
                LOGGER.fine("******************");
                LOGGER.fine("*   SCAN RESULTS  *");
                LOGGER.fine("******************");
                LOGGER.fine("minimum : " + scanmin.isValid());
                LOGGER.fine("pars    : " + upar);
                LOGGER.fine(upar.toString());
                LOGGER.fine("*******************************************");

                if(scanmin.isValid()) upar = scanmin.userParameters();
                
                MnMigrad migrad = new MnMigrad(this, upar, 2);
                migrad.checkAnalyticalDerivatives();

                FunctionMinimum min = migrad.minimize();
	        
	        MnUserParameters userpar = min.userParameters();
                
	        status = min.isValid();

                MnMinos minos = new MnMinos(this, min, 2);
                if(status) {                 
                    for(int i = 0; i < pars.length; i++){
                        pars[i].setValue(userpar.value(pars[i].name()));
                        pars[i].setError(userpar.error(pars[i].name()));
                        if(pars[i].getStep()>EPS) {
                            MinosError err = minos.minos(i);
                            if(err.isValid()) {
                                pars[i].setRange(err.lower(), err.upper());
                            }
                        }
                    }
                }
	        LOGGER.fine(upar.toString());
	        LOGGER.fine("******************");
	        LOGGER.fine("*   FIT RESULTS  *");
	        LOGGER.fine("******************");
	            
	        LOGGER.fine(min.toString());
	        

        }catch(Exception e){
	       e.printStackTrace();
        }
    }

    
    public String getBenchmarkString(){
        StringBuilder str = new StringBuilder();
        double time = (double) (endTime-startTime);
        str.append(String.format("[fit-benchmark] Time = %.3f , Iterations = %d, Status = %b, Chi2/NDF = %.3f/%d",
                time/1000.0,
                this.numberOfCalls,
                this.status,
                this.chi2,
                this.ndf));
        return str.toString();
    }   
    
    public void printChi2AndNDF() {
        LOGGER.info(String.format("chi2 = %.3f NDF = %d", this.getChi2(), this.getNDF()));
        LOGGER.info(this.getBenchmarkString());
    }
    
    public void printPars() {
        for(int i=0; i<pars.length; i++) {
            LOGGER.info(String.format("   %6s: %7.4f \u00B1 %.4f (%7.4f - %.4f)", pars[i].name(), pars[i].value(), pars[i].error(), pars[i].lower(), pars[i].upper()));
        }
        LOGGER.info("");
    }
    
    public void printResiduals() {
        LOGGER.info("Layer\tResiduals+/-Errors...");
        for(int im=0; im<currentValues.length; im++) {
            LOGGER.info(im + "");
            for(int it=0; it<currentValues[0].length; it++) {
                for(int ip=0; ip<currentValues[0][0].length; ip++) {
                    LOGGER.info(String.format("\t%7.2f \u00B1 %7.2f", currentValues[im][it][ip], currentErrors[im][it][ip]));
                }
            }
            LOGGER.info("");
        }
    }
    
    public void printShifts(int key) {
        LOGGER.info("Layer\tShifts for distortion " + key);
        for(int im=0; im<unitShifts[key].length; im++) {
            LOGGER.info(im + "");
            for(int it=0; it<unitShifts[key][0].length; it++) {
                for(int ip=0; ip<unitShifts[key][0][0].length; ip++) {
                    LOGGER.info(String.format("\t%7.2f", unitShifts[key][im][it][ip], unitShifts[key][im][it][ip]));
                }
            }
            LOGGER.info("");
        }
    }
}
