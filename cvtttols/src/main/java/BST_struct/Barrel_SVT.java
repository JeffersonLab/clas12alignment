package BST_struct;

import java.util.*;
import java.util.stream.Collectors;
import org.jlab.io.base.DataBank;

import BST_struct.Hit;
import BST_struct.Cluster;
import BST_struct.Module;
import BST_geo.*;
import org.jlab.geom.prim.Point3D;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;


public class Barrel_SVT {
	
	Module[][] Modules=new Module[6][18]; //Six Layers made of at most 18 modules
	Geometry geo;
	int nb_hit;
		
	public Barrel_SVT(){
		geo= new BST_geo.Geometry();
		nb_hit=0;
		for (int lay=0; lay<6;lay++) {
			for (int sec=0; sec<18;sec++) {
				Modules[lay][sec]=new Module(lay+1,sec+1);
				Modules[lay][sec].setNormBST(geo.findBSTPlaneNormal(sec+1, lay+1));
			}
		}
	}
	
	public void clear() {
		for (int lay=0; lay<6;lay++) {
			for (int sec=0; sec<18;sec++) {
				Modules[lay][sec].clear();
			}
		}
		nb_hit=0;
	}
	
	public void DisableModule(int lay, int sec) {
		Modules[lay-1][sec-1].DisableModule();
	}
	
	public void EnableModule(int lay, int sec) {
		Modules[lay-1][sec-1].EnableModule();
	}
		
	public void DisableLayer(int lay) {
		for (int sec=0;sec<geo.getNbModule(lay);sec++) {
			Modules[lay-1][sec].DisableModule();
		}
	}
	
	public void EnableLayer(int lay) {
		for (int sec=0;sec<geo.getNbModule(lay);sec++) {
			Modules[lay-1][sec].EnableModule();
		}
	}
	
	public int getNbHits() {
		return nb_hit;
	}
	
	public Module getModule(int lay, int sec) {
		return Modules[lay-1][sec-1];
	}
	
	public Cluster RecreateCluster(int layer, int sector, float centroid) {
		Cluster clus=new Cluster(layer,sector);
		double strip=Math.floor(centroid);
		clus.setCentroid(centroid);
		double weight=centroid-strip;
		return clus;
	}
	
	public void MakeClusters() {
		for (int lay=0; lay<6;lay++) {
			for (int sec=0; sec<18;sec++) {
				Modules[lay][sec].DoClustering();
			}
		}
	}
	
	@SuppressWarnings("static-access")
	public void fillBarrel(DataBank pbank, boolean isMC) {
		clear();
		float time=0;
		for (int row=0;row<pbank.rows();row++){
			int layer= pbank.getByte("layer",row );
			int sector= pbank.getByte("sector",row );
			int strip= pbank.getShort("component",row );
			int ADC= pbank.getInt("ADC",row );
			if (!isMC) time= pbank.getFloat("time",row );
			if (layer>0) {
				double [][] endpoints=geo.getStripEndPoints(strip, (layer-1)%2);
				Point3D begin=new Point3D(geo.transformToFrame(sector, layer, endpoints[0][0], 0, endpoints[0][1], "lab", ""));
				Point3D end=new Point3D(geo.transformToFrame(sector, layer, endpoints[1][0], 0, endpoints[1][1], "lab", ""));
				double phi_begin=Math.atan2(begin.y(),begin.x());
				double phi_end=Math.atan2(end.y(),end.x());
				if (ADC>0&&strip>0) {
					nb_hit++;
					Modules[layer-1][sector-1].addHit(nb_hit, strip, (end.x()+begin.x())/2., (end.y()+begin.y())/2., Double.NaN, (phi_begin+phi_end)/2., Math.abs((phi_begin-phi_end)/2.), Double.NaN, ADC+1, time);
				}
			}
		}
		MakeClusters();
		//PrintClusters();
	}
	
	public Geometry getGeometry() {
		return geo;
	}

}
