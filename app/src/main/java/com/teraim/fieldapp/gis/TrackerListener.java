package com.teraim.fieldapp.gis;

public interface TrackerListener {

	class GPS_State {

		public static GPS_State GPS_State_C(State s) {
			GPS_State g = new GPS_State();
			g.state=s;
			g.time=System.currentTimeMillis();
			return g;
		}

		private GPS_State() {
			x=-1;
			y=-1;
			lat=-1;
			lng=-1;
		}

		public enum State {

			disabled,
			enabled,
			newValueReceived,
			ping
		}



		public float accuracy;
		/** SWEREF99 easting (or -1 if not set). */
		public double x;
		/** SWEREF99 northing (or -1 if not set). */
		public double y;
		/** WGS84 latitude (or -1 if not set). */
		public double lat;
		/** WGS84 longitude (or -1 if not set). */
		public double lng;
		public State state;
		public long time;
	}

	enum Type {
		MENU,
		MAP,
		USER
	}

	void gpsStateChanged(GPS_State newState);
}
