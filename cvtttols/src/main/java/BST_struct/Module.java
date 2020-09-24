package BST_struct;

import java.util.*;
import java.util.stream.Collectors;

import BST_struct.Hit;
import BST_struct.Cluster;
import org.jlab.geom.prim.Vector3D;

public class Module {

	HashMap<Integer, Hit> hitmap;
	TreeMap<Integer, Hit> sorted_hitmap;
	HashMap<Integer, Cluster> clustermap;
	int layer_id;
	int sector_id;
	boolean InTheTracking;
	Vector3D norm;
	
	public Module() {
		layer_id=0;
		sector_id=0;
		hitmap = new HashMap<Integer, Hit>();
		sorted_hitmap = new TreeMap<Integer, Hit>();
		clustermap = new HashMap<Integer, Cluster>();
		norm=new Vector3D();
		InTheTracking=true;
	}
	
	public Module(int layer, int sector) {
		layer_id=layer;
		sector_id=sector;
		hitmap = new HashMap<Integer, Hit>();
		sorted_hitmap = new TreeMap<Integer, Hit>();
		clustermap = new HashMap<Integer, Cluster>();
		norm=new Vector3D();
		InTheTracking=true;
	}
	
	public void addHit(int id, int strip, double x, double y, double z, double phi, double err_phi, double err_z, int adc, float time) {
		Hit aHit=new Hit(id, x, y, z, phi, err_phi, err_z, adc, time);
		hitmap.put(strip, aHit);
	}
	
	public void SortHitmap() {
		sorted_hitmap.clear();
		sorted_hitmap.putAll(hitmap);
	}
	
	public void DisableModule() {
		InTheTracking=false;
	}
	
	public void EnableModule() {
		InTheTracking=true;
	}
	
	public void DoClustering() {
		SortHitmap();
		int num_hit=sorted_hitmap.size();
		int last_hit=-5;
		float last_time=-1000;
		if (num_hit!=0) {
			for(HashMap.Entry<Integer,Hit> m:sorted_hitmap.entrySet()) {
		    	if (clustermap.size()!=0) {
		    		last_hit=clustermap.get(clustermap.size()).getLastEntry();
		    		last_time=hitmap.get(last_hit).getTime();
		    	}	
		       	if (m.getKey()-last_hit>2) {
		    		Cluster clus=new Cluster(layer_id,sector_id);
		    		clus.InTheFit(InTheTracking);
		    		clus.add(m.getKey(),sorted_hitmap.get(m.getKey()));
		    		clustermap.put(clustermap.size()+1,clus);
		    	}
		    	if (m.getKey()-last_hit<=2) {
		    		clustermap.get(clustermap.size()).add(m.getKey(),sorted_hitmap.get(m.getKey()));
		    	}
			}
		}
	}
	
	public void clear() {
		sorted_hitmap.clear();
		hitmap.clear();
		clustermap.clear();
	}
	
	public void setNormBST(Vector3D vec) {
		norm=vec;
	}
	
	public Vector3D getNormBST() {
		return norm;
	}
	
	public HashMap<Integer, Cluster> getClusters(){
		return clustermap;
	}
	
	public HashMap<Integer, Hit> getHits(){
		return hitmap;
	}
}
