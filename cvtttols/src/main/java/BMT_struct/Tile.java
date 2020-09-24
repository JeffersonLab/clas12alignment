package BMT_struct;

import java.util.*;
import java.util.stream.Collectors;

import BMT_struct.Hit;
import BMT_struct.Cluster;

public class Tile {
	
	HashMap<Integer, Hit> hitmap;
	TreeMap<Integer, Hit> sorted_hitmap;
	HashMap<Integer, Cluster> clustermap;
	int layer_id;
	int sector_id;
	boolean InTheTracking;
	private double ClusteringTime;
	private int mode; //0=charge-weighted average placed in the middle of the drift space, 1=Charge-weighted average with only the first 2-strip hit and placed at a quarter of drift space
	
	public Tile() {
		layer_id=0;
		sector_id=0;
		hitmap = new HashMap<Integer, Hit>();
		sorted_hitmap = new TreeMap<Integer, Hit>();
		clustermap = new HashMap<Integer, Cluster>();
		InTheTracking=true;
		ClusteringTime=50;
		if (main.constant.isCosmic) ClusteringTime=200;
		mode=0;
	}
	
	public Tile(int layer, int sector) {
		layer_id=layer;
		sector_id=sector;
		hitmap = new HashMap<Integer, Hit>();
		sorted_hitmap = new TreeMap<Integer, Hit>();
		clustermap = new HashMap<Integer, Cluster>();
		ClusteringTime=50;
		if (main.constant.isCosmic) ClusteringTime=200;
		InTheTracking=true;
		mode=0;
	}
	
	public void addHit(int strip, double radius, double phi, double z, int adc, float time, double err) {
		Hit aHit=new Hit(radius, phi, z, adc, time, err);
		hitmap.put(strip, aHit);
	}
	
	public void SortHitmap() {
		sorted_hitmap.clear();
		sorted_hitmap.putAll(hitmap);
	}
	
	public void DisableTile() {
		InTheTracking=false;
	}
	
	public void EnableTile() {
		InTheTracking=true;
	}
	
	public void DoClustering() {
		SortHitmap();
		int num_hit=sorted_hitmap.size();
		int last_hit=-3;
		float last_time=-1000;
		if (num_hit!=0) {
			for(HashMap.Entry<Integer,Hit> m:sorted_hitmap.entrySet()) {
		    	if (clustermap.size()!=0) {
		    		last_hit=clustermap.get(clustermap.size()).getLastEntry();
		    		last_time=hitmap.get(last_hit).getTime();
		    	}	
		    	//We close the old cluster and create a new one
		       	if ((m.getKey()-last_hit>2)||Math.abs(sorted_hitmap.get(m.getKey()).getTime()-last_time)>ClusteringTime) {
		       		if (clustermap.size()!=0) clustermap.get(clustermap.size()).close(mode);
		    		Cluster clus=new Cluster();
		    		clus.add(m.getKey(),sorted_hitmap.get(m.getKey()));
		    		clus.setLayer(layer_id);
		    		clus.setSector(sector_id);
		    		clus.InTheFit(InTheTracking);
		    		clustermap.put(clustermap.size()+1,clus);
		    	}
		       	//We add this hit to the current cluster
		    	if ((m.getKey()-last_hit<=2)&&Math.abs(sorted_hitmap.get(m.getKey()).getTime()-last_time)<=ClusteringTime) {
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
	
	public void setClusteringMode(int clus_mode) {
		mode=clus_mode;
	}
	
	public int getClusteringMode() {
		return mode;
	}
	
	public HashMap<Integer, Cluster> getClusters(){
		return clustermap;
	}
	
	public HashMap<Integer, Hit> getHits(){
		return hitmap;
	}
}


