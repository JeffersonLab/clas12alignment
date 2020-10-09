package DC_struct;

import java.util.ArrayList;

public class Cluster {
	private double centroid; //strip info
	private double centroidResidual;
	private int size;
	private ArrayList<Integer> hit_id;
	private ArrayList<DC_struct.Wire> hit_list;
	private int layer_clus;
	private int LayerInSector;
	private int sector_clus;
	boolean InTheFit;
	private int trkID;
	
	public Cluster() {
		centroid=0;
		size=0;
		hit_id=new ArrayList<Integer>();
		hit_list=new ArrayList<DC_struct.Wire>();
		InTheFit=true;
		trkID=-1;
		centroidResidual=Double.NaN;
		LayerInSector=-1;
	}
	
	public ArrayList<Integer> getListOfHits(){
		return hit_id;
	}
	
	public int getLayerInSector() {
		return LayerInSector;
	}
	
	public void setLayerInSector(int NumLay) {
		if (LayerInSector==-1) LayerInSector=NumLay;
	}
	
	public void add(int id_hit, Wire aWire) {
		hit_id.add(id_hit);
		hit_list.add(aWire);
		centroid=size*centroid+id_hit;
		size=size+1;
		centroid=centroid/((double) size);
	}
	
	public int getSize() {
		return hit_id.size();
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
	
	public int getLastEntry() {
		return hit_id.get(hit_id.size()-1);
	}
	
	public double getCentroid() {
		return centroid;
	}
	
	public ArrayList<DC_struct.Wire> getWires(){
		return hit_list;
	}
	
}
