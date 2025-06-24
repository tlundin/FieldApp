package com.teraim.fieldapp.dynamic.types;

import java.io.Serializable;

import android.util.Log;

public class PhotoMeta implements Serializable {

	public double N=0,E=0,S=0,W=0;


	public PhotoMeta(String N,String E, String S, String W) {
		try {
			this.N = Double.parseDouble(N);
			this.W = Double.parseDouble(W);
			this.S = Double.parseDouble(S);
			this.E = Double.parseDouble(E);
		} catch (NumberFormatException e) { Log.e("vortex","non number in gis bg coordinates"); }


	}
	public PhotoMeta(double N,double E,double S,double W) {
		this.N=N;
		this.W=W;
		this.S=S;
		this.E=E;

	}
	
	public double getWidth()  {
		return E-W;
	}
	public double getHeight() {
		return N-S;
	}
}