package BMT_struct;

import java.util.*;
import java.util.stream.Collectors;
import org.jlab.io.base.DataBank;

import BMT_struct.Hit;
import BMT_struct.Cluster;
import BMT_struct.Tile;
import BMT_geo.*;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;

public class Barrel {
	Tile[][] Tiles=new Tile[6][3]; 
	Geometry geo;
	int nb_hit;
	
	public Barrel(){
		geo= new BMT_geo.Geometry();
		nb_hit=0;
		BMT_geo.CCDBConstantsLoader.Load(new DatabaseConstantProvider(10, "default"));
		for (int lay=0; lay<6;lay++) {
			for (int sec=0; sec<3;sec++) {
				Tiles[lay][sec]=new Tile(lay+1,sec+1);
				Tiles[lay][sec].setClusteringMode(0);
			}
		}
	}
	
	public void clear() {
		for (int lay=0; lay<6;lay++) {
			for (int sec=0; sec<3;sec++) {
				Tiles[lay][sec].clear();
			}
		}
		nb_hit=0;
	}
	
	public Tile getTile(int lay, int sec) {
		return Tiles[lay][sec];
	}
	
	public void DisableTile(int lay, int sec) {
		Tiles[lay-1][sec-1].DisableTile();
	}
	
	public void EnableTile(int lay, int sec) {
		Tiles[lay-1][sec-1].EnableTile();
	}
		
	public void DisableLayer(int lay) {
		for (int sec=0;sec<3;sec++) {
			Tiles[lay-1][sec].DisableTile();
		}
	}
	
	public void EnableLayer(int lay) {
		for (int sec=0;sec<3;sec++) {
			Tiles[lay-1][sec].EnableTile();
		}
	}
	
	
	public void PrintClusters() {
		int nb_clusters;
		double rad,phi,z;
		int Edep,size;
		float t_min;
		for (int lay=0; lay<6;lay++) {
			for (int sec=0; sec<3;sec++) {
				nb_clusters=Tiles[lay][sec].getClusters().size();
				System.out.println("Tile Sector "+(sec+1)+" Layer "+(lay+1)+" with "+nb_clusters+" clusters ");
				for (int clus=0;clus<nb_clusters;clus++) {
					rad=Tiles[lay][sec].getClusters().get(clus+1).getRadius();
					phi=Math.toDegrees(Tiles[lay][sec].getClusters().get(clus+1).getPhi());
					z=Tiles[lay][sec].getClusters().get(clus+1).getZ();
					Edep=Tiles[lay][sec].getClusters().get(clus+1).getEdep();
					size=Tiles[lay][sec].getClusters().get(clus+1).getSize();
					t_min=Tiles[lay][sec].getClusters().get(clus+1).getT_min();
					System.out.println("     Cluster of size "+size+" with tmin= "+t_min+" ns at phi= "+phi+" deg and z= "+z);
				}
			}
		}
	}
	
	public void MakeClusters() {
		for (int lay=0; lay<6;lay++) {
			for (int sec=0; sec<3;sec++) {
				Tiles[lay][sec].DoClustering();
			}
		}
	}
	
	public Cluster RecreateCluster(int layer, int sector, float centroid) {
		Cluster clus=new Cluster();
		clus.setLayer(layer);
		clus.setSector(sector);
		clus.setRadius(geo.getClusterRadius(layer)); 
		double strip=Math.floor(centroid);
		clus.setCentroid(centroid);
		double weight=centroid-strip;
		if (geo.getZorC(layer)==1) { //Z-tile
			double phi=(1-weight)*geo.CRZStrip_GetPhi(sector, layer, (int) strip)+weight*geo.CRZStrip_GetPhi(sector, layer, (int)(strip+1));
			clus.setPhi(phi);
			clus.setX(geo.getClusterRadius(layer)*Math.cos(phi));
			clus.setY(geo.getClusterRadius(layer)*Math.sin(phi));
			clus.setZ(Double.NaN);
			clus.setErr(geo.CRCStrip_GetPitch(layer, (int) strip)/Math.sqrt(12));
		}
		if (geo.getZorC(layer)==0) { //C-tile
			double Zclus=(1-weight)*geo.CRCStrip_GetZ(layer, (int) strip)+weight*geo.CRCStrip_GetZ(layer, (int)(strip+1));
			clus.setX(Double.NaN);
			clus.setY(Double.NaN);
			clus.setZ(Zclus);
			clus.setErr(geo.CRCStrip_GetPitch(layer, (int) strip)/Math.sqrt(12));
		}
		return clus;
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
						
			if (geo.getZorC(layer)==1&&strip>0&&ADC>0) { 
				Tiles[layer-1][sector-1].addHit(strip, geo.getClusterRadius(layer) , geo.CRZStrip_GetPhi(sector, layer, strip), Double.NaN, ADC, time, geo.CRCStrip_GetPitch(layer, strip)/Math.sqrt(12));
				nb_hit++;
			}
			if (geo.getZorC(layer)==0&&strip>0&&ADC>0) { 
				Tiles[layer-1][sector-1].addHit(strip, geo.getClusterRadius(layer) , Double.NaN, geo.CRCStrip_GetZ(layer, strip), ADC, time, geo.CRCStrip_GetPitch(layer, strip)/Math.sqrt(12));
				nb_hit++;
			}
		}
		MakeClusters();
		//PrintClusters();
	}
	
	public int getNbHits() {
		return nb_hit;
	}
	
	public Geometry getGeometry() {
		return geo;
	}
	
}
