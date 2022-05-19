package org.clas.dc.alignment;

import java.util.List;
import org.freehep.math.minuit.FCNBase;
import org.freehep.math.minuit.FunctionMinimum;
import org.freehep.math.minuit.MnMigrad;
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
   
    private Parameter[]           pars = new Parameter[Constants.NPARS];
    private double[]        trueValues = null; // 37 'layers'
    private double[][][] currentValues = null; // 37 'layers', nTheta, nPhi
    private double[][][] currentErrors = null; // 37 'layers', nTheta, nPhi
    private double[][][][]      shifts = null; // 12 distortions, 37 'layers', nTheta, nPhi
    private double[][][][]  unitShifts = null; // 12 distortions, 37 'layers', nTheta, nPhi
    private double[][][][]  unitSerror = null; // 12 distortions, 37 'layers', nTheta, nPhi
    private int nPar;
    private int nLayer;
    private int nTheta;
    private int nPhi;
    private double chi2;        
    private int    ndf;        
    
    private int       numberOfCalls = 0;
    private long      startTime     = 0L;
    private long      endTime       = 0L;
    
    public Fitter(double[][][][] shifts, double[][][][] serror, double [][][] residuals, double[][][] errors) {
        if(shifts.length!=Constants.NPARS) 
            throw new IllegalArgumentException("Error: invalid number of parameters " + shifts.length);
        this.nPar   = shifts.length;
        this.nLayer = shifts[0].length;
        this.nTheta = shifts[0][0].length;
        this.nPhi   = shifts[0][0][0].length;
        this.unitShifts    = shifts;
        this.unitSerror    = serror;
        this.currentValues = residuals;
        this.currentErrors = errors;
        this.initParameters();
        this.setShifts(false);
        startTime = System.currentTimeMillis();        
    }
    
    private void initParameters() {
        for(int i=0; i<Constants.NPARS; i++) {
            pars[i] = new Parameter(Constants.PARNAME[i], 0, 0, Constants.PARSTEP[i], -Constants.PARMAX[i], Constants.PARMAX[i]);
        }
    }
    
    private void setShifts(boolean randomize) {
        this.shifts = new double[nPar][nLayer][nTheta][nPhi];
        for(int i=0; i<nPar; i++) {
            for(int il=0; il<nLayer; il++) {
                for(int it=0; it<nTheta; it++) {
                    for(int ip=0; ip<nPhi; ip++) {
                        this.shifts[i][il][it][ip] = this.unitShifts[i][il][it][ip];
                        if(randomize)
                            this.shifts[i][il][it][ip] += 3*Math.random()*this.unitSerror[i][il][it][ip];
                    }
                }
            }
        }       
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
        this.setNDF(ndf-Constants.NPARS);
        return chi2;
    }

    private double value(int iLayer, int iTheta, int iPhi, double[] pars) {
        double value = 0;
        for(int i=0; i<pars.length; i++) {
            value += shifts[i][iLayer][iTheta][iPhi]*pars[i];
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
                pars[i].setValue(oldPars[i].value()+oldPars[i].getStep()*(Math.random()*2-1));
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
    
    public void fit(String options, int nTry) {
        boolean randomize = true;
        if(nTry ==0) {
            nTry = 1;
            randomize = false;
        }
        double[] ave2Pars = new double[nPar];
        double[] avePars  = new double[nPar];
        double[] aveErrs  = new double[nPar];
        double[] minPars  = new double[nPar];
        double[] maxPars  = new double[nPar];
        for(int ip=0; ip<nPar; ip++) {
            minPars[ip] = Double.POSITIVE_INFINITY;
            maxPars[ip] = Double.NEGATIVE_INFINITY;
        }
        
        for(int i=0; i<nTry; i++) {

            this.setShifts(randomize);
            this.fitIteration(options);

            for(int ip=0; ip<nPar; ip++) {
                ave2Pars[ip] += pars[ip].value()*pars[ip].value();
                avePars[ip]  += pars[ip].value();
                aveErrs[ip]  += pars[ip].error()*pars[ip].error(); 
                minPars[ip] = Math.min(minPars[ip], pars[ip].value());
                maxPars[ip] = Math.max(maxPars[ip], pars[ip].value());
            }
        }
        
        for(int ip=0; ip<nPar; ip++) {
            ave2Pars[ip] /= nTry;
            avePars[ip]  /= nTry;
            aveErrs[ip]  /= nTry; 
            this.pars[ip].setValue(avePars[ip]);
            this.pars[ip].setError(Math.sqrt(ave2Pars[ip]-avePars[ip]*avePars[ip]+aveErrs[ip]));
//            this.pars[ip].setValue((minPars[ip]+maxPars[ip])/2);
//            this.pars[ip].setError((maxPars[ip]-minPars[ip])/2);
        }
        
    }
    
    private void fitIteration(String options){
        
        try{
	        	        
	        MnUserParameters upar = new MnUserParameters();
	        for(int loop = 0; loop < Constants.NPARS; loop++){
                    UserParameter par = this.pars[loop];
	            upar.add(par.name(),par.value(),par.getStep());
	            if(par.getStep()<0.0000000001){
	                upar.fix(par.name());
	            }
	            if(par.min()>-1e9&&par.max()<1e9){
	                upar.setLimits(par.name(), par.min(), par.max());
	            }
	        }
	        
	        
	        MnScan  scanner = new MnScan(this,upar);
                for(int i = 0; i < Constants.NPARS; i++){
                    List<Point> points = scanner.scan(i);
                    if(options.contains("V")) {
                        MnPlot plot = new MnPlot();
                        plot.plot(points);
                    }
                }
	        FunctionMinimum scanmin = scanner.minimize(); 
	        if(options.contains("V")==true){
                    System.err.println("******************");
                    System.err.println("*   SCAN RESULTS  *");
                    System.err.println("******************");
                    System.out.println("minimum : " + scanmin.isValid());
                    System.out.println("pars    : " + upar);
                    System.out.println(upar);
                    System.err.println("*******************************************");
                }
                if(scanmin.isValid()) upar = scanmin.userParameters();
                
                MnMigrad migrad = new MnMigrad(this, upar, 2);

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
    
    public void printChi2AndNDF() {
        System.out.println(String.format("chi2 = %.3f NDF = %d", this.getChi2(), this.getNDF()));
        System.out.println(this.getBenchmarkString());
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
