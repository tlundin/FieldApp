package com.teraim.fieldapp.webservices;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URLConnection;

class WebService extends AsyncTask<String, String, String>{

	//public final String SERVICE_URI = "http://teraim.com/vortexservice.php?";

	interface WsCallback {
		void doSomething(String res);
	}

	private final WsCallback myC;
	public WebService(WsCallback c) {
		super();
		myC = c;
	}

	@Override
	protected String doInBackground(String... uri) {
		Object obj = Integer.valueOf(42);
		URL url;
		URLConnection conn;
		try {
			Log.d("vortex","In webservice");
			url = new URL(uri[0]);
			conn = url.openConnection();
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setUseCaches(false);

			// send object
			try (ObjectOutputStream objOut = new ObjectOutputStream(conn.getOutputStream())) {
				objOut.writeObject(obj);
				objOut.flush();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		Object reply="cheeze";
		try {
			try (ObjectInputStream objIn = new ObjectInputStream(conn.getInputStream())) {
				reply = objIn.readObject();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return reply.toString();
		
	}

	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		myC.doSomething(result);
	}
}
