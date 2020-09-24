package BMT_struct;

import BMT_struct.Hit;
import java.util.*;
import java.math.*;
import java.lang.*;
import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Vector3D;


public class Cluster {
	private float t_min;
	private float t_max;
	private Vector3D XYZ;
	private Vector3D RPhiZ;
	private double centroid; //strip info
	private double centroidResidual;
	private double centroid_phi; //info in loc frame, either phi or z.
	private double centroid_x; //info in loc frame, either phi or z.
	private double centroid_y; //info in loc frame, either phi or z.
	private double centroid_z; //info in loc frame, either phi or z.
	private double centroid_r;
	private int size;
	private int Edep;
	private ArrayList<Integer> hit_id;
	private ArrayList<BMT_struct.Hit> hit_list;
	private double Err;
	private int layer_clus;
	private int sector_clus;
	boolean InTheFit;
	private int first_tmin;
	private int second_tmin;
	private int trkID;
	private int seed;
	private int seedE;
		
	public Cluster() {
		t_min=0;
		t_max=0;
		centroid=0;
		centroid_phi=0;
		centroid_r=0;
		centroid_x=0;
		centroid_y=0;
		centroid_z=0;
		size=0;
		Edep=0;
		hit_id=new ArrayList<Integer>();
		hit_list=new ArrayList<BMT_struct.Hit>();
		Err=0.0;//mm
		InTheFit=true;
		first_tmin=0;
		second_tmin=0;
		trkID=-1;
		centroidResidual=Double.NaN;
		seed=-1;
		seedE=-1;
		}
	
	public ArrayList<Integer> getListOfHits(){
		return hit_id;
	}
	
	public void add(int id_hit, Hit aHit) {
		//By default, everything is computed in mode 0. If mode 1, everything is computed again in close method
		if (hit_id.size()==0) {
			t_min=aHit.getTime();
			t_max=aHit.getTime();
			first_tmin=hit_id.size();
			second_tmin=hit_id.size();
		}
		
		if (t_min>=aHit.getTime()) {
			t_min=aHit.getTime();
			second_tmin=first_tmin;
			first_tmin=hit_id.size();
		}
		if (t_max<aHit.getTime()) t_max=aHit.getTime();
		
		hit_id.add(id_hit);
		hit_list.add(aHit);
		
		if (aHit.getADC()>seedE) {
			seedE=aHit.getADC();
			seed=id_hit;
		}
		
		centroid_r=aHit.getRadius();
		Err=Edep*Err+aHit.getADC()*aHit.getErr();
		centroid=Edep*centroid+id_hit*aHit.getADC();
		
		if(!Double.isNaN(aHit.getPhi())) {
			centroid_x=Edep*centroid_x+centroid_r*aHit.getADC()*Math.cos(aHit.getPhi());
			centroid_y=Edep*centroid_y+centroid_r*aHit.getADC()*Math.sin(aHit.getPhi());
			Edep+=(double) aHit.getADC();
			centroid_x=centroid_x/Edep;
			centroid_y=centroid_y/Edep;
			centroid_z=Double.NaN;
			centroid_phi=Math.atan2(centroid_y, centroid_x);
			if (centroid_phi<0) centroid_phi+=2*Math.PI;
		}
		if(!Double.isNaN(aHit.getZ())) {
			centroid_x=Double.NaN;
			centroid_y=Double.NaN;
			centroid_phi=Double.NaN;
			centroid_z=Edep*centroid_z+aHit.getADC()*aHit.getZ();
			Edep+= (double) aHit.getADC();
			centroid_z=centroid_z/Edep;
		}
	
		centroid=centroid/Edep;
		
		Err=Err/Edep;
	}
	
	public int getSeed() {
		return seed;
	}
	
	public int getSeedE() {
		return seedE;
	}
	
	public double getX() {
		return centroid_x;
	}
	
	public double getY() {
		return centroid_y;
	}
	
	public double getZ() {
		return centroid_z;
	}
	
	public void setX(double xnew) {
		centroid_x=xnew;
	}
	
	public void setY(double ynew) {
		centroid_y=ynew;
	}
	
	public void setZ(double znew) {
		centroid_z=znew;
	}
	
	public double getRadius() {
		return centroid_r;
	}
	
	public double getPhi() {
		return centroid_phi;
	}
	
	public double getCentroid() {
		return centroid;
	}
	
	public float getT_min() {
		return t_min;
	}
	
	public float getT_max() {
		return t_max;
	}
	
	public float getTimeWalk() {
		return t_max-t_min;
	}
	
	public int getEdep() {
		return Edep;
	}
	
	public int getSize() {
		return hit_id.size();
	}
	
	public ArrayList<Integer> getHit_id(){
		return hit_id;
	}
	
	public int getLastEntry(){
		return hit_id.get(hit_id.size()-1);
	}
	
	public int getLayer(){
		return layer_clus;
	}
	
	public int getSector(){
		return sector_clus;
	}
	
	public void setLayer(int layer){
		layer_clus=layer;
	}
	
	public void setSector(int sector){
		sector_clus=sector;
	}
	
	public double getErr(){
		return Err;
	}
	
	public void setErr(double error) {
		Err=error;
	}
	
	public void InTheFit(boolean infit) {
		InTheFit=infit;
	}
	
	public boolean IsInFit() {
		return InTheFit;
	}
	
	public int gettrkID(){
		return trkID;
	}
	
	public void settrkID(int ID){
		trkID=ID;
	}
	
	public void close(int mode) {
		if (mode==1&&!main.constant.isMC) {
			centroid=(double) hit_id.get(first_tmin);
			if (first_tmin!=0) {
				centroid=(hit_list.get(first_tmin).getADC()*((double) hit_id.get(first_tmin))+hit_list.get(second_tmin).getADC()*((double) hit_id.get(second_tmin)))
							/(hit_list.get(second_tmin).getADC()+hit_list.get(first_tmin).getADC());
			}
			centroid_phi=0;
			centroid_r=centroid_r-BMT_geo.Constants.hStrip2Det/2.; //To get the centroid at Radius+hStrip2Det/4.
			centroid_x=0;
			centroid_y=0;
			centroid_z=0;
			if (Double.isNaN(hit_list.get(first_tmin).getPhi())){
				centroid_x=Double.NaN;
				centroid_y=Double.NaN;
				centroid_phi=Double.NaN;
				centroid_z=hit_list.get(first_tmin).getZ();
				if (first_tmin!=0) centroid_z=(hit_list.get(first_tmin).getADC()*hit_list.get(first_tmin).getZ()+hit_list.get(second_tmin).getADC()*hit_list.get(second_tmin).getZ())
						/(hit_list.get(second_tmin).getADC()+hit_list.get(first_tmin).getADC());
			}
			if (Double.isNaN(hit_list.get(first_tmin).getZ())){
				centroid_phi=hit_list.get(first_tmin).getPhi();
				centroid_z=Double.NaN;
				if (first_tmin!=0) {
					centroid_x=centroid_r*(hit_list.get(first_tmin).getADC()*Math.cos(hit_list.get(first_tmin).getPhi())+hit_list.get(second_tmin).getADC()*Math.cos(hit_list.get(second_tmin).getPhi()))
							/(hit_list.get(second_tmin).getADC()+hit_list.get(first_tmin).getADC());
					centroid_y=centroid_r*(hit_list.get(first_tmin).getADC()*Math.sin(hit_list.get(first_tmin).getPhi())+hit_list.get(second_tmin).getADC()*Math.sin(hit_list.get(second_tmin).getPhi()))
							/(hit_list.get(second_tmin).getADC()+hit_list.get(first_tmin).getADC());
					centroid_phi=Math.atan2(centroid_y, centroid_x);
					if (centroid_phi<0) centroid_phi+=2*Math.PI;
				}
			}
		}
	}
	
	public void setCentroidResidual(double res) {
		centroidResidual=res;
	}
	
	public double getCentroidResidual() {
		return centroidResidual;
	}
		
	public void setRadius(double d) {
		centroid_r=d;
	}
	
	public void setPhi(double phi) {
		centroid_phi=phi;
	}
	
	public void setCentroid(double cent) {
		centroid=cent;
	}
}
