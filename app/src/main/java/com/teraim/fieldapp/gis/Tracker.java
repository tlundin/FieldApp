package com.teraim.fieldapp.gis;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import androidx.core.app.ActivityCompat;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.SweLocation;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.gis.TrackerListener.GPS_State;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.utils.Geomatte;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.teraim.fieldapp.gis.TrackerListener.GPS_State.GPS_State_C;


public class Tracker extends Service implements LocationListener {

	//Keep track of time between synchronised saves
	private Long oldT = null;
	// flag for GPS status
	boolean isGPSEnabled = false;

    private Location location; // location

    // The minimum distance to change Updates in meters
	private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 1 meters

	// The minimum time between updates in milliseconds
	private static final long MIN_TIME_BW_UPDATES = 1000 * 5; // 1 minute


	// Declaring a Location Manager
	private LocationManager locationManager;

	private final Variable myX, myY, myAcc;

    public Tracker() {
        Map<String, String> gpsKeyHash = GlobalState.getInstance().getVariableConfiguration().createGpsKeyMap();
		myX = GlobalState.getInstance().getVariableCache().getVariable(gpsKeyHash, NamedVariables.MY_GPS_LAT);
		myY = GlobalState.getInstance().getVariableCache().getVariable(gpsKeyHash, NamedVariables.MY_GPS_LONG);
		myAcc = GlobalState.getInstance().getVariableCache().getVariable(gpsKeyHash, NamedVariables.MY_GPS_ACCURACY);


	}


	public enum ErrorCode {
		GPS_VARS_MISSING,
		GPS_NOT_ON,
		UNSTABLE,
		GPS_NOT_ENABLED,
		GPS_OK,
		MISSING_PERMISSION
    }

	public ErrorCode startScan(Context ctx) {
		//do we have variables?

		if (myX == null || myY == null)
			return ErrorCode.GPS_VARS_MISSING;
		//does Globalstate exist?
		GlobalState gs = GlobalState.getInstance();
		if (gs == null)
			return ErrorCode.UNSTABLE;

		try {
			locationManager = (LocationManager) ctx
					.getSystemService(LOCATION_SERVICE);

			// getting GPS status
			isGPSEnabled = locationManager
					.isProviderEnabled(LocationManager.GPS_PROVIDER);

			// getting network status
            // flag for network status
            boolean isNetworkEnabled = false;
			//locationManager
			//		.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

			if (!isGPSEnabled && !isNetworkEnabled) {
				return ErrorCode.GPS_NOT_ENABLED;
			} else {
                boolean canGetLocation = true;
				// First get location from Network Provider
				if (isNetworkEnabled) {
					if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
							ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
						// TODO: Consider calling
						//    ActivityCompat#requestPermissions
						// here to request the missing permissions, and then overriding
						//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
						//                                          int[] grantResults)
						// to handle the case where the user grants the permission. See the documentation
						// for ActivityCompat#requestPermissions for more details.
						return ErrorCode.MISSING_PERMISSION;
					}
					locationManager.requestLocationUpdates(
							LocationManager.NETWORK_PROVIDER,
							MIN_TIME_BW_UPDATES,
							MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
					Log.d("Network", "Network");
					if (locationManager != null) {
						if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
								ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
							// TODO: Consider calling
							//    ActivityCompat#requestPermissions
							// here to request the missing permissions, and then overriding
							//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
							//                                          int[] grantResults)
							// to handle the case where the user grants the permission. See the documentation
							// for ActivityCompat#requestPermissions for more details.
							return ErrorCode.MISSING_PERMISSION;
						}
						location = locationManager
								.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
						if (location != null) {
                            // latitude
                            double latitude = location.getLatitude();
                            // longitude
                            double longitude = location.getLongitude();
						}
					}
				}
				// if GPS Enabled get lat/long using GPS Services
				if (isGPSEnabled) {
					if (location == null) {

						if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
								ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
							// TODO: Consider calling
							//    ActivityCompat#requestPermissions
							// here to request the missing permissions, and then overriding
							//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
							//                                          int[] grantResults)
							// to handle the case where the user grants the permission. See the documentation
							// for ActivityCompat#requestPermissions for more details.
							return ErrorCode.MISSING_PERMISSION;
						}
						locationManager.requestLocationUpdates(
								LocationManager.GPS_PROVIDER,
								MIN_TIME_BW_UPDATES,
								MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
						Log.d("GPS Enabled", "GPS Enabled");
								/*	                        if (locationManager != null) {
	                            location = locationManager
	                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
	                            if (location != null) {
	                                latitude = location.getLatitude();
	                                longitude = location.getLongitude();
	                            }
	                        } */
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return ErrorCode.GPS_OK;
	}


	@Override
	public void onLocationChanged(Location location) {
		try {
			if (GlobalState.getInstance()==null || !GlobalState.getInstance().getDb().db.isOpen()) {
				Log.e("vortex","No db anymore in Tracker. I will kill myself.");
				this.stopUsingGPS();
				this.stopSelf();
				return;
			}
			Log.d("jgw","got new coords: "+location.getLatitude()+","+location.getLongitude());
			if (location!=null && myX!=null) {

				//Log.d("vortex","setting sweref location");
				SweLocation myL = Geomatte.convertToSweRef(location.getLatitude(),location.getLongitude());

				String oldX = myX.getValue();
				String oldY = myY.getValue();


				if (oldX!=null&&oldY!=null&&oldT!=null) {
					long currT = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
					long timeDiff = currT - oldT;
					//Log.d("vortex","cuurT oldT time diff: "+currT+" "+oldT+" "+timeDiff);
					double oldXd = Double.parseDouble(oldX);
					double oldYd = Double.parseDouble(oldY);
					double distx = Math.abs(oldXd - myL.getX());
					double disty = Math.abs(oldYd - myL.getY());

					//Log.d("vortex", "Distance between mesaurements in Tracker: (x,y) " + distx + "," + disty);
					String accuracy = Float.toString(location.getAccuracy());
					String x = Double.toString(myL.getX());
					String y = Double.toString(myL.getY());


					if (oldT==null || distx > 15 || disty > 15 || timeDiff > 60) {
					//	Log.d("vortex","setting synced location");
						myX.setValue(x);
						myY.setValue(y);
						if (myAcc!=null)
							myAcc.setValue(accuracy);
						oldT = myX.getTimeOfInsert();
					} else {
						myX.setValueNoSync(x);
						myY.setValueNoSync(y);
						if (myAcc!=null)
							myAcc.setValueNoSync(accuracy);


					}
				} else {
					myX.setValue(myL.getX() + "");
					myY.setValue(myL.getY() + "");
					if (myAcc!=null)
						myAcc.setValue(location.getAccuracy() + "");
					oldT = myX.getTimeOfInsert();
				}

				GPS_State signal = GPS_State_C(GPS_State.State.newValueReceived);
				signal.accuracy=location.getAccuracy();
				signal.x=myL.getX();
				signal.y=myL.getY();
				sendMessage(signal);

			}
		}catch(Exception e) {
			e.printStackTrace();
			if (GlobalState.getInstance()!=null && GlobalState.getInstance().getLogger()!=null) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				LoggerI o = GlobalState.getInstance().getLogger();
				o.addRedText(sw.toString());
				o.addRow("");
				this.stopUsingGPS();
				this.stopSelf();
				return;
			}
		}
	}



	private void sendMessage(GPS_State newState) {
		if (menu!=null)
    		menu.gpsStateChanged(newState);
		if (user!=null)
			user.gpsStateChanged(newState);
		if (map!=null)
			map.gpsStateChanged(newState);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}



	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {

		switch(status)
		{
			case LocationProvider.OUT_OF_SERVICE:
				System.out.println("GPS out of service");
				break;
			case LocationProvider.AVAILABLE:
				sendMessage(GPS_State_C(GPS_State.State.ping));
				//System.out.println("GPS Available");
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				System.out.println("Temp unavailable!!");
				break;
			default:
				System.out.println("GPS __??? "+status);
				break;
		}
                /*
                 * GPS_EVENT_FIRST_FIX Event is called when GPS is locked            
                 
                    Location gpslocation = locationManager
                            .getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    if(gpslocation != null)
                    {       
                    System.out.println("GPS Info:"+gpslocation.getLatitude()+":"+gpslocation.getLongitude());

                    
                     * Removing the GPS status listener once GPS is locked  
                     
                        //locationManager.removeGpsStatusListener(mGPSStatusListener);                
                    }               

                break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
            	System.out.println("TAG - GPS_EVENT_SATELLITE_STATUS");
                break; 
                
       }
       */
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.d("vortex","Provider enabled in gps listener");
		sendMessage(GPS_State_C(GPS_State.State.enabled));
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.d("vortex","Provider disabled in gps listener");
		sendMessage(GPS_State_C(GPS_State.State.disabled));
	}
	public void stopUsingGPS(){
		if(locationManager != null){
			locationManager.removeUpdates(Tracker.this);
		}
	}

	private TrackerListener map,menu,user;

	public void registerListener(TrackerListener tl, TrackerListener.Type type) {
		switch (type) {
			case MAP:
				map = tl;
				break;
			case MENU:
				menu = tl;
				break;
			case USER:
				user = tl;
				break;
		}
	}


}
