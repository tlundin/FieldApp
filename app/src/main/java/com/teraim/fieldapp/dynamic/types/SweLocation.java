package com.teraim.fieldapp.dynamic.types;

import android.util.Log;

public class SweLocation implements Location {

	public  double north;
	public  double east;
	
	public SweLocation(double X,double Y) {
		this.north=Y;
		this.east=X;
	}

	//Y=N-S, X=E-W
	public SweLocation(String X, String Y) {
		if (Y==null||X==null) {
			north=-1;
			east=-1;
			Log.e("vortex","null value in sweloc constructor! "+X+" "+Y);
			return;
		}
		this.east=Double.parseDouble(X);
		this.north=Double.parseDouble(Y);
	}
	
	public SweLocation(String XandY) {
		if (XandY == null) {
			north=-1;
			east=-1;
			Log.e("vortex","null value in sweloc constructor! ");
			return;
		}
		String []xy = XandY.split(",");
		this.east=Double.parseDouble(xy[0]);
		this.north=Double.parseDouble(xy[1]);
	}

	public double getX() {
		return east;
	}
	public double getY() {
		return north;
	}
	
	@Override
	public String toString() {

			return getX()+","+getY();

	}


}
