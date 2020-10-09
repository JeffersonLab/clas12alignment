package DC_struct;

import java.util.HashMap;
import java.util.TreeMap;

import eu.mihosoft.vrl.v3d.Vector3d;

public class Layer {
	HashMap<Integer, Wire> hitmap;
	TreeMap<Integer, Wire> sorted_hitmap;
	HashMap<Integer, Cluster> clustermap;
	int layer_id;
	int sector_id;
	boolean InTheTracking;
	
	public Layer(int layer) {
		layer_id=layer;
		hitmap = new HashMap<Integer, Wire>();
		sorted_hitmap = new TreeMap<Integer, Wire>();
		clustermap = new HashMap<Integer, Cluster>();
		InTheTracking=true;
	}
	
	public void addWire(int strip, double DOCA, int tdc, Vector3d dir, Vector3d point, double res) {
		Wire aWire=new Wire(dir, point, DOCA, tdc, res);
		hitmap.put(strip, aWire);
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
	
	public int getNbLayerHit() {
		return hitmap.size();
	}
	
	public void DoClustering() {
		SortHitmap();
		int num_hit=sorted_hitmap.size();
		int last_hit=-3;
		float last_time=-1000;
		if (num_hit!=0) {
			for(HashMap.Entry<Integer,Wire> m:sorted_hitmap.entrySet()) {
		    	if (clustermap.size()!=0) last_hit=clustermap.get(clustermap.size()).getLastEntry();
		    		
		    	//We close the old cluster and create a new one
		       	if (m.getKey()-last_hit>1) {
		       		Cluster clus=new Cluster();
		    		clus.add(m.getKey(),sorted_hitmap.get(m.getKey()));
		    		clus.setLayer(layer_id);
		    		clus.setSector(sector_id);
		    		clus.InTheFit(InTheTracking);
		    		clustermap.put(clustermap.size()+1,clus);
		    	}
		       	//We add this hit to the current cluster
		    	if (m.getKey()-last_hit<=1) {
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
	
	public HashMap<Integer, Cluster> getClusterList(){
		return clustermap;
	}
	
	public HashMap<Integer, Wire> getHitList(){
		return hitmap;
	}
}
