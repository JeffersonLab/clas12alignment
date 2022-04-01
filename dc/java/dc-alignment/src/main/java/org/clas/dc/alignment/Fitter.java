package org.clas.dc.alignment;

import org.freehep.math.minuit.FCNBase;
import org.freehep.math.minuit.FunctionMinimum;
import org.freehep.math.minuit.MnMigrad;
import org.freehep.math.minuit.MnScan;
import org.freehep.math.minuit.MnUserParameters;
import org.jlab.groot.math.UserParameter;

/**
 *
 * @author devita
 */
public class Fitter  implements FCNBase {
   
    private Parameter[]           pars = new Parameter[Constants.NPARS];
    private double[]        trueValues = null; // 37 'layers'
    private double[][][] currentValues = null; // 37 'layers', nTheta, nPhi
    private double[][][] currentErrors = null; // 37 'layers', nTheta, nPhi
    private double[][][][]  unitShifts = null; // 12 distortions, 37 'layers', nTheta, nPhi
    private double chi2;        
    private int    ndf;        
    
    private int       numberOfCalls = 0;
    private long      startTime     = 0L;
    private long      endTime       = 0L;
    
    public Fitter(double[][][][] shifts, double [][][] residuals, double[][][] errors) {
        if(shifts.length!=Constants.NPARS) 
            throw new IllegalArgumentException("Error: invalid number of parameters " + shifts.length);
        this.unitShifts    = shifts;
        this.currentValues = residuals;
        this.currentErrors = errors;
        this.initParameters();
        startTime = System.currentTimeMillis();        
    }
    
    private void initParameters() {
        for(int i=0; i<Constants.NPARS; i++) {
            pars[i] = new Parameter(Constants.PARNAME[i], 0, Constants.PARSTEP[i]);
        }
    }
    
    @Override
    public double valueOf(double[] pars) {
        double chi2 = 0.0;
        chi2 = getChi2(pars);
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
        this.setNDF(ndf-Constants.NPARS);
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

    public Parameter[] getPars() {
        return pars;
    }

    public void setPars(Parameter[] pars) {
        this.pars = pars;
    }
    
    public void fit(String options){
        
        if(options.contains("V")) {
            this.printResiduals();
            this.printShifts(0);
        }
        try{
	        	        
	        MnUserParameters upar = new MnUserParameters();
	        for(int loop = 0; loop < Constants.NPARS; loop++){
                    UserParameter par = this.pars[loop];
	            upar.add(par.name(),par.value(),0.0001);
	            if(par.getStep()<0.0000000001){
	                upar.fix(par.name());
	            }
	            if(par.min()>-1e9&&par.max()<1e9){
	                upar.setLimits(par.name(), par.min(), par.max());
	            }
	        }
	        
	        
	        MnScan  scanner = new MnScan(this,upar);
	        FunctionMinimum scanmin = scanner.minimize(); 
	        if(options.contains("V")==true){
                    System.err.println("******************");
                    System.err.println("*   SCAN RESULTS  *");
                    System.err.println("******************");
                    System.out.println("minimum : " + scanmin);
                    System.out.println("pars    : " + upar);
                    System.out.println(upar);
                    System.err.println("*******************************************");
                }
                MnMigrad migrad = new MnMigrad(this, upar);
	        
	        FunctionMinimum min = migrad.minimize();
	        
	        MnUserParameters userpar = min.userParameters();
	        
	        for(int loop = 0; loop < Constants.NPARS; loop++){
	            UserParameter par = this.pars[loop];
	            par.setValue(userpar.value(par.name()));
	            par.setError(userpar.error(par.name()));
	        }
	        
	        if(options.contains("V")==true){
	            System.out.println(upar);
	            System.err.println("******************");
	            System.err.println("*   FIT RESULTS  *");
	            System.err.println("******************");
	            
	            System.err.println(min);
	        }
	        
	        System.out.println(this.getBenchmarkString());
        }catch(Exception e){
	       e.printStackTrace();
        }
    }

    
    public String getBenchmarkString(){
        StringBuilder str = new StringBuilder();
        double time = (double) (endTime-startTime);
        str.append(String.format("[fit-benchmark] Time = %.3f , Iterations = %d"
                , time/1000.0,
                this.numberOfCalls));
        return str.toString();
    }   
    
    
    public void printPars() {
        for(int i=0; i<pars.length; i++) {
            if(pars[i].getStep()>0) System.out.print(String.format("   %s: %.4f +/- %.4f", pars[i].name(), pars[i].value(), pars[i].error()));
        }
        System.out.println();
    }
    
    public void printResiduals() {
        System.out.println("Layer\tResiduals+/-Errors...");
        for(int im=0; im<currentValues.length; im++) {
            System.out.print(im);
            for(int it=0; it<currentValues[0].length; it++) {
                for(int ip=0; ip<currentValues[0][0].length; ip++) {
                    System.out.print(String.format("\t%6.2f+/-%6.2f", currentValues[im][it][ip], currentErrors[im][it][ip]));
                }
            }
            System.out.println();
        }
    }
    
    public void printShifts(int key) {
        System.out.println("Layer\tShifts for distortion " + key);
        for(int im=0; im<unitShifts[key].length; im++) {
            System.out.print(im);
            for(int it=0; it<unitShifts[key][0].length; it++) {
                for(int ip=0; ip<unitShifts[key][0][0].length; ip++) {
                    System.out.print(String.format("\t%6.2f", unitShifts[key][im][it][ip], unitShifts[key][im][it][ip]));
                }
            }
            System.out.println();
        }
    }
}
