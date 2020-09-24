package Trajectory;

import org.freehep.math.minuit.FCNBase;
import BMT_struct.Cluster;
import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Vector3D;


public class StraightLine {
	Vector3D slope;
	Vector3D point;
	double Phi; //atan2(sy,sx);
	double Theta; //ACos(s_perp/s_tot)
		
	public StraightLine() {
		slope=new Vector3D();
		point=new Vector3D();
		Phi=0;
		Theta=0;
		slope.setXYZ(0, 0, 1);
		point.setXYZ(0, 0, 0);
	}
		
	public void setPoint_XYZ(double dx, double dy, double dz) {
		point.setXYZ(dx, dy, dz);
	}
	
	public void setPoint_X(double dx) {
		point.setX(dx);
	}
	
	public void setPoint_Y(double dy) {
		point.setY(dy);
	}
	
	public void setPoint_Z(double dz) {
		point.setZ(dz);
	}
		
	public Vector3D getPointOnTrack(double lambda) {
		Vector3D NewPoint=new Vector3D();
		NewPoint.setXYZ(slope.x()*lambda+point.x(),slope.y()*lambda+point.y() , slope.z()*lambda+point.z());
		return NewPoint;
	}
	
	public Vector3D getPoint() {
		return point;
	}
	
	public Vector3D getSlope() {
		return slope;
	}
	
	public void setSlope_XYZ(double sx, double sy, double sz) {
		slope.setX(sx);slope.setY(sy);slope.setZ(sz);
		Phi=Math.atan2(slope.y(), slope.x());
		Theta=Math.acos(slope.z()/slope.mag());
	}
	
	public double getDistanceToLine(StraightLine line_bis) {
		double dist=0;
		double s1x=this.getSlope().x();double s1y=this.getSlope().y();double s1z=this.getSlope().z();
		double i1x=this.getPoint().x();double i1y=this.getPoint().y();double i1z=this.getPoint().z();
		
		double s2x=line_bis.getSlope().x();double s2y=line_bis.getSlope().y();double s2z=line_bis.getSlope().z();
		double i2x=line_bis.getPoint().x();double i2y=line_bis.getPoint().y();double i2z=line_bis.getPoint().z();
		
		double a=-(s1x*s1x+s1y*s1y+s1z*s1z);
		double b=(s1x*s2x+s2y*s1y+s2z*s1z);
		double d=(s2x*s2x+s2y*s2y+s2z*s2z);
		double c=-b;
		double det=(a*d-b*c);

		if (det==0) {
			double lambda=(i2x-i1x+i2y-i1y+i2z-i1z)/(s1x+s1y+s1z);
			Vector3D point_a=this.getPointOnTrack(lambda);
			dist=Math.sqrt((point_a.x()-i2x)*(point_a.x()-i2x)+(point_a.y()-i2y)*(point_a.y()-i2y)+(point_a.z()-i2z)*(point_a.z()-i2z));
		}
		if (det!=0) {
			double bx=s1x*(i1x-i2x)+s1y*(i1y-i2y)+s1z*(i1z-i2z);
			double by=s2x*(i1x-i2x)+s2y*(i1y-i2y)+s2z*(i1z-i2z);
			
			double lambda=(bx*d-b*by)/det;
			double mu=(-bx*c+a*by)/det;
			
			Vector3D point_a=this.getPointOnTrack(lambda);
			Vector3D point_b=line_bis.getPointOnTrack(mu);
			dist=Math.sqrt((point_b.x()-point_a.x())*(point_b.x()-point_a.x())
					+(point_b.y()-point_a.y())*(point_b.y()-point_a.y())
					+(point_b.z()-point_a.z())*(point_b.z()-point_a.z()));
			
		}
		
		return dist;
	}
	
	public Vector3D getClosestPointToLine(StraightLine line_bis) {
		double dist=0;
		Vector3D point_a=new Vector3D();
		double s1x=this.getSlope().x();double s1y=this.getSlope().y();double s1z=this.getSlope().z();
		double i1x=this.getPoint().x();double i1y=this.getPoint().y();double i1z=this.getPoint().z();
		
		double s2x=line_bis.getSlope().x();double s2y=line_bis.getSlope().y();double s2z=line_bis.getSlope().z();
		double i2x=line_bis.getPoint().x();double i2y=line_bis.getPoint().y();double i2z=line_bis.getPoint().z();
		
		double a=-(s1x*s1x+s1y*s1y+s1z*s1z);
		double b=(s1x*s2x+s2y*s1y+s2z*s1z);
		double d=(s2x*s2x+s2y*s2y+s2z*s2z);
		double c=-b;
		double det=(a*d-b*c);

		if (det==0) point_a.setXYZ(i1x, i1y, i1z);
		if (det!=0) {
			double bx=s1x*(i1x-i2x)+s1y*(i1y-i2y)+s1z*(i1z-i2z);
			double by=s2x*(i1x-i2x)+s2y*(i1y-i2y)+s2z*(i1z-i2z);
			
			double lambda=(bx*d-b*by)/det;
			double mu=(-bx*c+a*by)/det;
			
			point_a=this.getPointOnTrack(lambda);
			
		}
		return point_a;
	}
	
	public StraightLine InTileFrame(int layer, int sector) {
		StraightLine new_line=new StraightLine();
		Vector3D new_slope=this.getSlope();
		Vector3D new_point=this.getPoint();
		new_slope.rotateX(BMT_geo.Constants.getRx(layer,sector));
		new_slope.rotateY(BMT_geo.Constants.getRy(layer,sector));
		new_slope.rotateZ(BMT_geo.Constants.getRz(layer,sector));
		new_point.rotateX(BMT_geo.Constants.getRx(layer,sector));
		new_point.rotateY(BMT_geo.Constants.getRy(layer,sector));
		new_point.rotateZ(BMT_geo.Constants.getRz(layer,sector));
		new_point.setX(new_point.x()+BMT_geo.Constants.getCx(layer,sector));
		new_point.setY(new_point.y()+BMT_geo.Constants.getCy(layer,sector));
		new_point.setZ(new_point.z()+BMT_geo.Constants.getCz(layer,sector));
		
		new_line.setSlope_XYZ(new_slope.x(), new_slope.y(), new_slope.z());
		new_line.setPoint_XYZ(new_point.x(), new_point.y(), new_point.z());
		
		return new_line;
			
	}
	
	public Vector3D IntersectWithPlaneY() {
		
		if (slope.y()==0||Double.isNaN(slope.y())) {
			Vector3D intersect=new Vector3D();
			intersect.setXYZ(Double.NaN, Double.NaN, Double.NaN);
			return intersect;
		}
		
		double lambda_y=-point.y()/slope.y();
		Vector3D intersect=this.getPointOnTrack(lambda_y);
		return intersect;
	}
	
public Vector3D IntersectWithPlaneZ(double z0) {
		
		if (slope.z()==0||Double.isNaN(slope.z())) {
			Vector3D intersect=new Vector3D();
			intersect.setXYZ(Double.NaN, Double.NaN, Double.NaN);
			return intersect;
		}
		
		double lambda_z=(z0-point.z())/slope.z();
		Vector3D intersect=this.getPointOnTrack(lambda_z);
		return intersect;
	}
	
	public void Print() {
		System.out.println("/////////////////");
		System.out.println(slope);
		System.out.println(point);
	}
	
}
