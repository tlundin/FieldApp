package com.teraim.fieldapp.dynamic.types;


import com.google.android.libraries.maps.model.LatLng;

public class LatLong implements Location {

	private final double	latitude;
	private final double longitude;

	public LatLng ll() {
		return new LatLng(latitude,longitude);
	}

		
	public LatLong(double latitude, double longitude) {
		this.latitude=latitude;
		this.longitude=longitude;
	}
	public LatLong(String latitude, String longitude) {
		this.latitude=Double.parseDouble(latitude);
		this.longitude=Double.parseDouble(longitude);
	}

	@Override
	public double getX() {
		
		return latitude;
	}


	@Override
	public double getY() {
		
		return longitude;
	}
	
	@Override
	public String toString() {
		return getX()+","+getY();
	}
}
