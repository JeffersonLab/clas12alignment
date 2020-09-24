package DC_struct;

import java.util.ArrayList;

import org.jlab.geom.prim.Vector3D;

import TrackFinder.Fitter;
import Trajectory.StraightLine;

public class Segment {
	
	private ArrayList<DC_struct.Cluster> clusterlist;
	private int nbSuperLayer;
	private int nbLayer;
	private int SuperLayer;
	private int region;
	private int sector;
	private StraightLine HBtrack;
	private double chi2;
	private boolean fitstatus;
	private Vector3D vertex;
	
	public Segment(int Sector, int Super_Layer) {
		clusterlist=new ArrayList<DC_struct.Cluster>();
		nbSuperLayer=0;
		nbLayer=0;
		SuperLayer=Super_Layer;
		region=(SuperLayer+1)/2;
		HBtrack=new StraightLine();
		chi2=Double.POSITIVE_INFINITY;
		sector=Sector;
		fitstatus=false;
		vertex=new Vector3D(Double.NaN,Double.NaN,Double.NaN);
	}
	
	public int getRegion() {
		return region;
	}
	
	public void addCluster(Cluster clus) {
		clusterlist.add(clus);
		clusterlist.get(clusterlist.size()-1).setLayerInSector(clus.getLayer()+6*(SuperLayer-1));
		nbLayer++;
	}
	
	public double getLastCentroid() {
		return clusterlist.get(clusterlist.size()-1).getCentroid();
	}
	
	public double getFirstCentroid() {
		return clusterlist.get(0).getCentroid();
	}
	
	public int getSize() {
		return clusterlist.size();
	}
	
	public int getLayerLastEntry() {
		return clusterlist.get(clusterlist.size()-1).getLayer();
	}
	
	public ArrayList<DC_struct.Cluster> getClusters(){
		return clusterlist;
	}
	
	public Segment Duplicate() {
		 Segment Dupli=new Segment(this.getSector(),this.getSuperLayer());
		
		for (int cl=0;cl<this.getClusters().size()-1; cl++) {
			Dupli.addCluster(this.getClusters().get(cl));
		}
		return Dupli;
	}
	
	public Segment Merge(Segment ToMerge) {
		Segment Merged=new Segment(ToMerge.getSector(),ToMerge.getSuperLayer());
		Merged.setHBtrack(this.getHBtrack());
		for (int cl=0;cl<this.getSize();cl++) {
			Merged.addCluster(this.getClusters().get(cl));
		}
		for (int cl=0;cl<ToMerge.getSize();cl++) {
			Merged.addCluster(ToMerge.getClusters().get(cl));
		}
		Merged.setNbSuperLayer(this.getNbSuperLayer()+ToMerge.getNbSuperLayer());
		if (ToMerge.getSuperLayer()==5||ToMerge.getSuperLayer()==3) {
			Fitter Tracklet=new Fitter();
			if (ToMerge.getSuperLayer()==5) Tracklet.DCStraightTrack_init(Merged);
			if (ToMerge.getSuperLayer()==3) Tracklet.DCStraightTrack(Merged);
		}
		return Merged;
	}
	
	public int getNbLayer() {
		return nbLayer;
	}
	
	public int getNbSuperLayer() {
		return nbSuperLayer;
	}
	
	
	public void setNbSuperLayer(int nSL) {
		nbSuperLayer=nSL;
	}
	public int getSuperLayer() {
		return SuperLayer;
	}
	
	public boolean isGoodSLSegment() {
		boolean good=false;
		if (this.nbLayer>=3&&(this.getClusters().get(0).getLayer()==6||this.getClusters().get(0).getLayer()==5)&&(this.getClusters().get(this.getSize()-1).getLayer()==1||this.getClusters().get(this.getSize()-1).getLayer()==2)) good=true;
		return good;
	}
	
	public boolean isGoodSectorSegment() {
		boolean good=false;
		if (this.nbSuperLayer==6) good=true;
		return good;
	}
	
	public void PrintSegment() {
		for (int clus=0;clus<clusterlist.size();clus++) {
			System.out.println(clusterlist.get(clus).getLayerInSector()+" "+clusterlist.get(clus).getCentroid()+" "+clusterlist.get(clus).getWires().get(0).getWirePoint().x+" "+clusterlist.get(clus).getWires().get(0).getWirePoint().y
					+" "+clusterlist.get(clus).getWires().get(0).getWirePoint().z);
		}
	}

	public double ComputeChi2(StraightLine line) {
		double chi2=0;
		for (int clus=0;clus<this.getSize();clus++) {
			if (this.getClusters().get(clus).getSize()<3) {
				for (int wire=0;wire<this.getClusters().get(clus).getSize();wire++) {
					chi2+=Math.pow((this.getClusters().get(clus).getWires().get(wire).getWire().getDistanceToLine(line)-this.getClusters().get(clus).getWires().get(wire).getDOCA())/this.getClusters().get(clus).getWires().get(wire).getResolution(),2);
				}
			}
		}
		return chi2;
	}
	
	public void setHBtrack(StraightLine HB) {
		HBtrack=HB;
	}
	
	public StraightLine getHBtrack() {
		return HBtrack;
	}
	
	public void setChi2(double chisq) {
		if (chisq==Double.POSITIVE_INFINITY||chisq==Double.NEGATIVE_INFINITY) System.out.println("OhOh");
		chi2=chisq;
	}
	
	public double getChi2() {
		return chi2;
	}

	public boolean IsSimilar(Segment segment) {
		boolean Similar=false;
		if (this.getSector()!= segment.getSector()) return Similar;
		else {
			int SharedHit=0;
			for (int cl=0; cl<this.getClusters().size(); cl++)  {
				for (int ncl=0; ncl<segment.getClusters().size(); ncl++)  {
					if (this.getClusters().get(cl).getCentroid()==segment.getClusters().get(ncl).getCentroid()&&this.getClusters().get(cl).getLayer()==segment.getClusters().get(ncl).getLayer()) SharedHit++;
				}
				if (SharedHit>=2) {
					Similar=true;
					return Similar;
				}
			}
		}
		return Similar;
	}
	
	public int getSector() {
		return sector;
	}
	
	public boolean getFitStatus() {
		return fitstatus;
	}
	
	public void setFitStatus(boolean status) {
		fitstatus=status;
	}

	public boolean IsWorseThan(Segment segment) {
		if (this.getClusters().size()<segment.getClusters().size()) return true;
		else {
			if (this.getChi2()>segment.getChi2()&&this.getClusters().size()==segment.getClusters().size()) return true;
			else return false;
		}
				
	}

	public void setVertex(Vector3D vx0) {
		vertex.setXYZ(vx0.x(), vx0.y(), vx0.z());
		
	}
	
	public Vector3D getVertex() {
		return vertex;
		
	}
	
	public boolean ThroughFMT() {
		boolean check=false;
		Vector3D inter=this.getHBtrack().IntersectWithPlaneZ(30);
		if (Math.sqrt(inter.x()*inter.x()+inter.y()*inter.y())<40) check=true;
		return check;
	}
	
}
