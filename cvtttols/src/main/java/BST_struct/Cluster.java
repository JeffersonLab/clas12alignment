package BST_struct;

import BST_struct.Hit;
import java.util.*;
import java.math.*;
import java.lang.*;
import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Vector3D;

public class Cluster {
	
	private int size;
	private int Edep;
	private int Eseed;
	private int stripseed;
	private double centroid;
	private double centroidResidual;
	private double phi_mid;
	private double z_mid;
	private double x_mid;
	private double y_mid;
	private double err_phi_mid;
	private double err_z_mid;
	private int layer;
	private int sector;
	private ArrayList<Integer> hit_id;
	private int trkID;
	boolean InTheFit;
	
	public Cluster(int lay, int sec) {
		size=0;
		Edep=0;
		centroid=0;
		centroidResidual=Double.NaN;
		phi_mid=0;
		z_mid=0;
		y_mid=0;
		x_mid=0;
		err_phi_mid=0;
		err_z_mid=0;
		hit_id= new ArrayList();
		layer=lay;
		sector=sec;
		trkID=-1;
		Eseed=-1;
		stripseed=-1;
		InTheFit=true;
	}
	
	public void add(int id_hit, Hit aHit) {
		hit_id.add(id_hit);
		if (Eseed<aHit.getADC()) {
			Eseed=aHit.getADC();
			stripseed=id_hit;
		}
		centroid=Edep*centroid+aHit.getADC()*id_hit;
		size++;
		phi_mid=Edep*phi_mid+aHit.getPhi()*aHit.getADC();
		z_mid=Edep*z_mid+aHit.getZ()*aHit.getADC();
		y_mid=Edep*y_mid+aHit.getY()*aHit.getADC();
		x_mid=Edep*x_mid+aHit.getX()*aHit.getADC();
		err_phi_mid=Edep*err_phi_mid+aHit.getErrPhi()*aHit.getADC();
		err_z_mid=Edep*err_z_mid+aHit.getErrZ()*aHit.getADC();
		Edep+=aHit.getADC();
		if (Edep!=0) {
			phi_mid=phi_mid/Edep;
			z_mid=z_mid/Edep;
			y_mid=y_mid/Edep;
			x_mid=x_mid/Edep;
			err_phi_mid=err_phi_mid/Edep;
			err_z_mid=err_z_mid/Edep;
			centroid=centroid/Edep;
		}
	}

	public int getLastEntry() {
		return hit_id.get(hit_id.size()-1);
	}
	
	public double getPhi() {
		return phi_mid;
	}
	
	public double getErrPhi() {
		return err_phi_mid;
	}
	
	public double getZ() {
		return z_mid;
	}
	
	public double getY() {
		return y_mid;
	}
	
	public double getX() {
		return x_mid;
	}
	
	public double getErrZ() {
		return err_z_mid;
	}
	
	public double getCentroid() {
		return centroid;
	}
	
	public double getCentroidResidual() {
		return centroidResidual;
	}
	
	public void setCentroidResidual(double residue) {
		centroidResidual=residue;
	}
	
	public int getLayer() {
		return layer;
	}
	
	public int getSector() {
		return sector;
	}
	
	public void setX(double xnew) {
		x_mid=xnew;
	}
	
	public void setY(double ynew) {
		y_mid=ynew;
	}
	
	public void setZ(double znew) {
		z_mid=znew;
	}
	
	public ArrayList<Integer> getListOfHits(){
		return hit_id;
	}
	
	public int gettrkID(){
		return trkID;
	}
	
	public void settrkID(int ID){
		trkID=ID;
	}
	
	public int getEtot(){
		return Edep;
	}
	
	public int getSeedE(){
		return Eseed;
	}
	
	public int getSeed(){
		return stripseed;
	}
	
	public void InTheFit(boolean infit) {
		InTheFit=infit;
	}
	
	public boolean IsInFit() {
		return InTheFit;
	}
	
	public void setCentroid(double cent) {
		centroid=cent;
	}
	
}
