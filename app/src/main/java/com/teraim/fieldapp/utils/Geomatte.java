package com.teraim.fieldapp.utils;

import android.util.Log;

import com.teraim.fieldapp.dynamic.types.LatLong;
import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.SweLocation;

import java.util.List;



public class Geomatte {
	private static final String TAG = "Geomatte";

	private final static double d2r = (Math.PI / 180.0);
	//calculate haversine distance for linear distance


	public static double dist(double lat1, double long1, double lat2, double long2)
	{
		double dlong = (long2 - long1) * d2r;
		double dlat = (lat2 - lat1) * d2r;
		double a = Math.pow(Math.sin(dlat/2.0), 2) + Math.cos(lat1*d2r) * 
				Math.cos(lat2*d2r) * Math.pow(Math.sin(dlong/2.0), 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return 6367 * c;
	}

	private static double sqr(double x) { return x * x; }
	private static double dist2(Location v, Location w,double pxr,double pyr) { 
		
		
		return sqr((v.getX() - w.getX())*pxr) + sqr((v.getY() - w.getY())*pyr); 
		
	}
	private static double distToSegmentSquared(Location p, Location v, Location w,double pxr,double pyr) {
		double l2 = dist2(v, w,pxr,pyr);
		if (l2 == 0) return dist2(p, v,pxr,pyr);
		double t = ((p.getX() - v.getX()) * (w.getX() - v.getX()) + (p.getY() - v.getY()) * (w.getY() - v.getY())) / l2;
		if (t < 0) return dist2(p, v,pxr,pyr);
		if (t > 1) return dist2(p, w,pxr,pyr);
		return dist2(p, new SweLocation(v.getX() + t * (w.getX() - v.getX()),
				v.getY() + t * (w.getY() - v.getY())),pxr,pyr);
	}
	public static double pointToLineDistance3(Location A, Location B, Location P,double pxr,double pyr) { return Math.sqrt(distToSegmentSquared(P, A, B,pxr,pyr)); }



	public static double sweDist(double myY,double myX,double destY, double destX) {	
		//Log.d(TAG,"diffX: diffY: "+(myX-destX)+" "+(myY-destY));
		//Log.d(TAG,"Values  x1 y1 x2 y2: "+myX+" "+myY+" "+destX+" "+destY);
        //Log.d(TAG,"res: "+res);
		return Math.sqrt(Math.pow((myX-destX),2)+Math.pow(myY-destY, 2));

	}
	
	private static double sweDist(Location location, Location location2) {
		//Log.d(TAG,"swedist: x1,y1  - x2,y2"+location.getX()+","+location.getY()+" - "+location2.getX()+","+location2.getY());
		return sweDist(location.getX(),location.getY(),location2.getX(),location2.getY());
	}
	
	public static double lengthOfPath(List<Location> myDots) {
		if (myDots==null || myDots.size()<2) {
			Log.d(TAG,"too few coordinates in lengthofpath...returning 0");
			return 0;
		}
		double length = 0;
		
		for (int i = 0 ; i < myDots.size()-1; i++) {
			//Log.d(TAG,"mydots: "+myDots.get(i)+","+myDots.get(i+1));
			length += sweDist(myDots.get(i),myDots.get(i+1));
		}
		return length;
	}

	public static double getArea(List<Location> myDots) {
		double T=0; 
		int p,n;
		for (int i=0;i<myDots.size();i++) {
			p = i==0?myDots.size()-1:i-1;
			n = i==(myDots.size()-1)?0:i+1;
			T+= myDots.get(i).getX()*(myDots.get(n).getY()-myDots.get(p).getY());
		}
		return Math.abs(T/2);
	}
	public static double getCircumference(List<Location> myDots) {
			return lengthOfPath(myDots);
	}

	

	public static double getRikt2(double userY, double userX, double destY, double destX) {
		double alfa=-100;
		double PI = Math.PI;
		double dy = destY-userY;
		double dx = destX-userX;
		double a2 = Math.atan2(dy,dx);
		//Log.d(TAG,"ATAN2 (r) (g)"+a2+" "+57.2957795*a2);
		if (a2>-PI&&a2<=PI/2)
			alfa = (PI/2-a2);
		else
			alfa = (PI/2-a2)+2*PI;
		//Log.d(TAG,"ALFA I RADIER: Grader: "+alfa+" "+ 57.2957795*alfa);
		return alfa;


	}


	static double getRikt(double dest, double centerY, double centerX, double destY, double destX) {
		//dest is Opposite side length.
		//We still 
		double b = destX-centerX;
		//double a = Math.abs(destY-centerY); **not needed.

        double  beta = Math.acos(b/ dest);
		Log.d(TAG,"b,c,beta: "+b+" "+ dest +" "+beta);
		//Gamma is the top angle in a 90 deg. triangle.
		double gamma = Math.PI/2-beta; // 90 grader - beta i radianer = 90*pi/180 = 1*pi/2.
		//alfa is PI+gamma if destx - x is negative.
		double alfa =  Math.PI+gamma;
		Log.d(TAG,"gamma: "+gamma);

		//Alfa should also be equal to Atan2(y,x).
		double alfa2 = Math.atan2(destY, destX);
		Log.d(TAG,"ALFA: "+alfa+" ALFA (tan2): "+alfa2);
		return alfa;


	}






	public static LatLong convertToLatLong(double northing, double easting) {


		double axis; // Semi-major axis of the ellipsoid.
		double flattening; // Flattening of the ellipsoid.
		double central_meridian; // Central meridian for the projection.
		double scale; // Scale on central meridian.
		double false_northing; // Offset for origo.
		double false_easting; // Offset for origo.

		//Sweref 99_tm.
		axis = 6378137.0; // GRS 80.
		flattening = 1.0 / 298.257222101; // GRS 80.
		central_meridian = Double.MIN_VALUE;
		scale = 1.0;
		false_northing = 0.0;
		false_easting = 150000.0;
		central_meridian = 15.00;
		scale = 0.9996;
		false_northing = 0.0;
		false_easting = 500000.0;





		double[] lat_lon = new double[2];

		// Prepare ellipsoid-based stuff.
		double e2 = flattening * (2.0 - flattening);
		double n = flattening / (2.0 - flattening);
		double a_roof = axis / (1.0 + n) * (1.0 + n * n / 4.0 + n * n * n * n / 64.0);
		double delta1 = n / 2.0 - 2.0 * n * n / 3.0 + 37.0 * n * n * n / 96.0 - n * n * n * n / 360.0;
		double delta2 = n * n / 48.0 + n * n * n / 15.0 - 437.0 * n * n * n * n / 1440.0;
		double delta3 = 17.0 * n * n * n / 480.0 - 37 * n * n * n * n / 840.0;
		double delta4 = 4397.0 * n * n * n * n / 161280.0;

		double Astar = e2 + e2 * e2 + e2 * e2 * e2 + e2 * e2 * e2 * e2;
		double Bstar = -(7.0 * e2 * e2 + 17.0 * e2 * e2 * e2 + 30.0 * e2 * e2 * e2 * e2) / 6.0;
		double Cstar = (224.0 * e2 * e2 * e2 + 889.0 * e2 * e2 * e2 * e2) / 120.0;
		double Dstar = -(4279.0 * e2 * e2 * e2 * e2) / 1260.0;

		// Convert.
		double deg_to_rad = Math.PI / 180;
		double lambda_zero = central_meridian * deg_to_rad;
		double xi = (easting - false_northing) / (scale * a_roof);
		double eta = (northing - false_easting) / (scale * a_roof);
		double xi_prim = xi -
				delta1 * Math.sin(2.0 * xi) * math_cosh(2.0 * eta) -
				delta2 * Math.sin(4.0 * xi) * math_cosh(4.0 * eta) -
				delta3 * Math.sin(6.0 * xi) * math_cosh(6.0 * eta) -
				delta4 * Math.sin(8.0 * xi) * math_cosh(8.0 * eta);
		double eta_prim = eta -
				delta1 * Math.cos(2.0 * xi) * math_sinh(2.0 * eta) -
				delta2 * Math.cos(4.0 * xi) * math_sinh(4.0 * eta) -
				delta3 * Math.cos(6.0 * xi) * math_sinh(6.0 * eta) -
				delta4 * Math.cos(8.0 * xi) * math_sinh(8.0 * eta);
		double phi_star = Math.asin(Math.sin(xi_prim) / math_cosh(eta_prim));
		double delta_lambda = Math.atan(math_sinh(eta_prim) / Math.cos(xi_prim));
		double lon_radian = lambda_zero + delta_lambda;
		double lat_radian = phi_star + Math.sin(phi_star) * Math.cos(phi_star) *
				(Astar +
						Bstar * Math.pow(Math.sin(phi_star), 2) +
						Cstar * Math.pow(Math.sin(phi_star), 4) +
						Dstar * Math.pow(Math.sin(phi_star), 6));
		lat_lon[0] = lat_radian * 180.0 / Math.PI;
		lat_lon[1] = lon_radian * 180.0 / Math.PI;
		return new LatLong(lat_lon[0],lat_lon[1]);
		
	}

	public static SweLocation convertToSweRef(double lat, double lon) {

		double a = 6378137.0000;
		double f = 1.0/298.257222101;
		double e2 = f*(2.0-f);
		double cent_m = 15.0;
		double k0 = 0.9996;
		double FN = 0;
		double FE = 500000;
		double n = f/(2.0-f);
		double ap = a/(1.0+n)*(1.0+(Math.pow(n,2)/4.0)+(Math.pow(n,4)/64.0));

		double latr = lat*Math.PI/180.0;
		double lambda = lon*Math.PI/180.0;
		double lambda0 = cent_m*Math.PI/180.0;
		double deltalambda = lambda-lambda0;
        double B = (1.0/6.0)*(5.0*Math.pow(e2, 2)-Math.pow(e2, 3));
		double C = (1.0/120.0)*(104.0*Math.pow(e2, 3)-45.0*Math.pow(e2, 4));
		double D = (1.0/1260.0)*(1237.0*Math.pow(e2, 4));
		double latStar = latr-Math.sin(latr)*Math.cos(latr)*(e2 +B*Math.pow(Math.sin(latr),2)+
				C*Math.pow(Math.sin(latr), 4)+D*Math.pow(Math.sin(latr),6));
		double oui = Math.atan(Math.tan(latStar)/Math.cos(deltalambda));
		double nui = math_atanh(Math.cos(latStar)*Math.sin(deltalambda));

		double b1 = n/2.0-(2.0*n*n)/3.0+(5.0/16.0)*Math.pow(n,3)+(41.0/180.0)*Math.pow(n, 4);
		double b2 = (13.0/48.0)*n*n-(3.0/5.0)*Math.pow(n,3)+(557.0/1440.0)*Math.pow(n, 4);
		double b3 = (61.0/240.0)*Math.pow(n, 3)-(103.0/140.0)*Math.pow(n, 4);
		double b4 = (49561.0/161280.0)*Math.pow(n, 4);


		double y = k0*ap*(oui+
				b1*Math.sin(2.0*oui)*Math.cosh(2.0*nui)+
				b2*Math.sin(4.0*oui)*Math.cosh(4.0*nui)+
				b3*Math.sin(6.0*oui)*Math.cosh(6.0*nui)+
				b4*Math.sin(8.0*oui)*Math.cosh(8.0*nui))+FN;
		double x = k0*ap*(nui+
				b1*Math.cos(2.0*oui)*Math.sinh(2.0*nui)+
				b2*Math.cos(4.0*oui)*Math.sinh(4.0*nui)+
				b3*Math.cos(6.0*oui)*Math.sinh(6.0*nui)+
				b4*Math.cos(8.0*oui)*Math.sinh(8.0*nui))+FE;
		y = Math.round(y * 1000.0) / 1000.0;
		x = Math.round(x * 1000.0) / 1000.0;

		//Log.d(TAG," lat long (x,y): "+x+" "+y);
		return new SweLocation(x,y);
	}

	public static Location subtract(Location l1,
			Location l2) {
		return new SweLocation(l1.getX()-l2.getX(),l1.getY()-l2.getY());
	}

	private static double math_sinh(double value) {
		return 0.5 * (Math.exp(value) - Math.exp(-value));
	}

	private static double math_cosh(double value) {
		return 0.5 * (Math.exp(value) + Math.exp(-value));
	}

	private static double math_atanh(double value) {
		return 0.5 * Math.log((1.0 + value) / (1.0 - value));
	}



	public static double pointToLineDistance4(double x, double y, double x1, double y1, double x2, double y2) {

		double A = x - x1;
		double B = y - y1;
		double C = x2 - x1;
		double D = y2 - y1;

		double dot = A * C + B * D;
		double len_sq = C * C + D * D;
		double param = -1;
		if (len_sq != 0) //in case of 0 length line
			param = dot / len_sq;

		double xx, yy;

		if (param < 0) {
			xx = x1;
			yy = y1;
		}
		else if (param > 1) {
			xx = x2;
			yy = y2;
		}
		else {
			xx = x1 + param * C;
			yy = y1 + param * D;
		}

		double dx = x - xx;
		double dy = y - yy;
		return Math.sqrt(dx * dx + dy * dy);
	}


}