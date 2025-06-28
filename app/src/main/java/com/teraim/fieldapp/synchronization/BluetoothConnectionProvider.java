package com.teraim.fieldapp.synchronization;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.exceptions.BluetoothDevicesNotPaired;
import com.teraim.fieldapp.log.LogRepository;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.synchronization.ConnectionListener.ConnectionEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.util.Iterator;
import java.util.Set;

/**
 * 
 * @author Terje
 *
 * A ConnectionProvider for Bluetooth. 
 */


public class BluetoothConnectionProvider extends ConnectionProvider {

	//Receive Broadcast messages from the connection threads.
	private BroadcastReceiver brr=null;

	//Try connecting five times. 
	private final int AttemptsBeforeGivingUp = 5;

	//Convenience
	private final GlobalState gs;
	private final LogRepository o;
	private final Context ctx;

	//Threads
	private ClientConnectThread mClient=null;
	private ServerThread mServer=null;
	private ConnectedThread mConnected_T = null;

	//Adapter.
	private final BluetoothAdapter mBluetoothAdapter;

	private InternalState internalState = InternalState.closed;

	private String mPartner;


	//Keeps track of state
	private enum InternalState {
		closed,
		closing,
		opening,
		open
	}




	public BluetoothConnectionProvider(Context ctx) {
		this.ctx=ctx;
		gs = GlobalState.getInstance();	
		o = gs.getLogger();

		mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
		mBluetoothAdapter.setName(Constants.BLUETOOTH_NAME);
		Log.d("vortex","My bluetooth name is now: "+mBluetoothAdapter.getName());
		//showNotification();
		brr = new BroadcastReceiver() {
			@Override
			public void onReceive(final Context ctx, Intent intent) {
				final String action = intent.getAction();
				//If message fails, try to ping until sister replies.

				if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
					final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
							BluetoothAdapter.ERROR);
					switch (state) {
					case BluetoothAdapter.STATE_OFF:
						Log.d("BT","Bluetooth off");
						closeConnection();
						break;
					case BluetoothAdapter.STATE_TURNING_OFF:
						Log.d("BT","Bluetooth turning off");
						internalState  = InternalState.closing;
						break;
					case BluetoothAdapter.STATE_ON:
						Log.d("BT","Bluetooth on...starting server");
						if (internalState  == InternalState.opening)
							//Try now!
							openConnection(mPartner);
						break;
					case BluetoothAdapter.STATE_TURNING_ON:
						Log.d("BT","Bluetooth turning on");
						break;
					}
				}

			}};


			Log.d("vortex","Bluetooth on create");
			o.addText("BlueTooth starting ");

	}

	@Override
	public void openConnection(String partner) {

		mPartner = partner;

		Log.d("vortex","Bluetooth: Open Connection");
		if (mConnected_T !=null && mConnected_T.isAlive()) {
			Log.e("vortex","connection is running, so will disregard call to openconnection");
		} else 
		{


			if (mBluetoothAdapter==null) {
				Log.e("NILS","bt adaptor null in bluetoothremotedevice service CREATE");

			} 
			//If we need to turn on bluetooth, wait for broadcast.
			else if (!mBluetoothAdapter.isEnabled())	{
				Log.d("vortex","Enabling bluetooth");
				mBluetoothAdapter.enable();

				IntentFilter ifi = new IntentFilter();
				ifi.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

				internalState  = InternalState.opening;
				LocalBroadcastManager.getInstance(ctx).registerReceiver(brr,
						ifi);
				//ctx.registerReceiver(brr, ifi);

			}
			else {
				//Bluetooth is running. Try.
				Log.d("Vortex","Bluetooth: starting client and server");

				//Now the fun starts.
				try {
					mClient = startClient(partner);
					if (mClient == null) {
						o.addText("");
						o.addCriticalText("Bluetooth Connection failed: Partner named "+partner+" not found");
						broadcastEvent(ConnectionEvent.connectionFailedNamedPartnerMissing);
					} else
						mServer = startServer();
				} catch (BluetoothDevicesNotPaired e) {
					broadcastEvent(ConnectionEvent.connectionFailedNoPartner);
					o.addText("");
					o.addCriticalText("Bluetooth Connection failed: No paired device");
					e.printStackTrace();
				}

			}
		}
	}





	@Override
	public void closeConnection() {
		//Stop multiple calls..it will cause exception
		if (internalState  != InternalState.closed) {

			if(BluetoothAdapter.getDefaultAdapter().isEnabled())
				BluetoothAdapter.getDefaultAdapter().disable();
			//drop reference to this object.
			Log.d("vortex","Bluetooth: Close Connection");
			if (mClient!=null)
				mClient.cancel();
			if (mServer!=null)
				mServer.cancel();
			if (mConnected_T !=null)
				mConnected_T.cancel();
			mConnected_T=null;
			try {
				LocalBroadcastManager.getInstance(ctx).unregisterReceiver(brr);
			} catch (IllegalArgumentException e) {
				Log.d("nils","unregisterReceiver - dropping exception");
			}
			internalState  = InternalState.closed;
			broadcastEvent(ConnectionEvent.connectionClosedGracefully);

		}
	}


	@Override
	public void abortConnection() {
		closeConnection();
	}


	@Override
	public void write(Object message) {
		if (isOpen()) {
			Log.d("vortex","internalstate is open. Passing on message");
			mConnected_T.write(message);
		} else
			Log.d("vortex","cannot send. connection not open!");
	}




	/**Start a Client thread for communication with a named device.
	 * If the device is not named, take first found.		
	 * 
	 * @throws BluetoothDevicesNotPaired
	 */

	private ClientConnectThread startClient(String partnerName) throws BluetoothDevicesNotPaired {

		ClientConnectThread client=null;

		//check if there is a bonded device.
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		BluetoothDevice partner=null;

		if (pairedDevices.isEmpty()) {
			Log.e("NILS","Didn't find any paired device.");
			throw new BluetoothDevicesNotPaired();
		}
		else 
		{
			Iterator<BluetoothDevice> partners = pairedDevices.iterator();

			//If no name is given, return first found.
			if (partnerName == null || pairedDevices.size()==1) {
				partner = partners.next();
			} else {

				boolean found = false;
				while (partners.hasNext() && !found) {
					BluetoothDevice potentialPartner = partners.next();
					if (potentialPartner.getName().equals(partnerName)) { 
						partner = potentialPartner;
						found = true;
					} else
						Log.d("vortex","No match, partner name: "+potentialPartner);
				}
			}
			if (partner == null && pairedDevices.size()>0) {
				Log.d("vortex","Could not find partner named "+partnerName+" .Will try first device in list");
				partner=pairedDevices.iterator().next();
			}
			if (partner!=null) {
				Log.d("Vortex","Bluetooth: Starting client connect to partner: "+partner.getName());
				client = new ClientConnectThread(partner);
				client.start();
			} else {
				Log.d("vortex","Bluetooth connection failed. Could not find partner named "+partnerName);
				return null;
			}
		}

		return client;
	}


	//Create server thread
	private ServerThread startServer() {

		Log.d("NILS","Trying to start server.");
		ServerThread server = new ServerThread();
		server.start();

		return server;

	}


	private class ServerThread extends Thread {
		private final BluetoothServerSocket mmServerSocket;

		ServerThread() {
			// Use a temporary object that is later assigned to mmServerSocket,
			// because mmServerSocket is final
			BluetoothServerSocket tmp = null;
			try {
				// MY_UUID is the app's UUID string, also used by the client code
				tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("NILS", Constants.getmyUUID());
			} catch (IOException e) {
				Log.e("vortex","exception in server thread startup");
				broadcastEvent(ConnectionEvent.connectionFailed);
			}
			mmServerSocket = tmp;

		}

		public void run() {
			BluetoothSocket socket = null;
			// Keep listening until exception occurs or a socket is returned
			Log.d("NILS","Server is started");
			while (true) {
				try {
					socket = mmServerSocket.accept();
					Log.d("NILS","SERVER got a SOCKET");

				} catch (IOException e) {
					Log.e("NILS","Exception in ServerThread");
					e.printStackTrace();
					break;
				}
				// If a connection was accepted
				if (socket != null) {
					// Do work to manage the connection (in a separate thread)
					Log.d("NILS","calling managesocket");
					startConnectedThread(socket);
					try {
						mmServerSocket.close();
					} catch (IOException e) {
						e.printStackTrace();

					}
					break;
				}
			}
		}



		/** Will cancel the listening socket, and cause the thread to finish */
        void cancel() {
			try {
				mmServerSocket.close();
			} catch (IOException e) { }
		}
	}



	/**
	 * 
	 * @author Terje
	 * Try connecting. If success, a socket is created that can be used to communicate.
	 * 
	 */
	private class ClientConnectThread extends Thread {

		private static final long CONNECTION_ATTEMPT_DELAY = 5000;
		private final BluetoothSocket mmSocket;
		int 	attempts = 		AttemptsBeforeGivingUp;
		boolean success	 =		false;

		ClientConnectThread(BluetoothDevice device) {
			// Use a temporary object that is later assigned to mmSocket,
			// because mmSocket is final
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket to connect with the given BluetoothDevice
			try {
				// MY_UUID is the app's UUID string, also used by the server code
				tmp = device.createRfcommSocketToServiceRecord(Constants.getmyUUID());
			} catch (IOException e) { 
				e.printStackTrace();
				//ups!
				broadcastEvent(ConnectionEvent.connectionFailed);
			}
			mmSocket = tmp;

		}

		int attemptsRemaining() {
			return attempts;
		}

		public void run() {
			// Cancel discovery because it will slow down the connection
			mBluetoothAdapter.cancelDiscovery();


			while (attempts>0 &&!success && mConnected_T==null ) {

				attempts--;
				success = true;

				try {
					// Connect the device through the socket. This will block
					// until it succeeds or throws an exception
					Log.d("Vortex","Trying to connect to sister device. Attempts left: "+attempts);
					mmSocket.connect();
				} catch (IOException connectException) {
					// Unable to connect; close the socket and get out
					Log.d("Vortex","Failed to connect");
					Log.d("Vortex","Msg: "+connectException.getMessage());
					//Tell the world.

					if (mConnected_T==null) {
						success=false;
						broadcastEvent(ConnectionEvent.connectionAttemptFailed);		
						try {
							Thread.sleep(CONNECTION_ATTEMPT_DELAY);
						} catch (InterruptedException e) {
							Log.e("vortex","Interrupted sleep in clientconnectthread!");
							attempts=0;
						}
					} 
				}

			}
			if (mConnected_T!=null) {
				Log.d("vortex","socket already aquired...i can exit");
			} else {
				if (success) {
					// Do work to manage the connection (in a separate thread)
					startConnectedThread(mmSocket);
				} else {
					cancel();
					o.addText("");
					Log.d("vortex","Gave up, no more attempts");
					o.addCriticalText("Bluetooth connection failed. Make sure that both devices have turned on Bluetooth and that they are within reach of eachother!");
				}
			}
		}



		/** Will cancel an in-progress connection, and close the socket */
        void cancel() {
			attempts=0;
			try {
				mmSocket.close();
			} catch (IOException e) {Log.e("vortex","upzzz");}
			broadcastEvent(ConnectionEvent.connectionBroken);
		}

	}	

	private void startConnectedThread(BluetoothSocket mmSocket) {
		Log.d("Vortex","connected to sister device");		
		if (mConnected_T== null) {
			mConnected_T = new ConnectedThread(gs,mmSocket);
			mConnected_T.start();
			Log.d("Vortex","Connected!");
			internalState = InternalState.open;
			broadcastEvent(ConnectionEvent.connectionGained);


		} else
			Log.d("vortex","socket already received!");
	}

	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		//private final InputStream mmInStream;
		//private final OutputStream mmOutStream;
		private final ObjectOutputStream obj_out;
		private final ObjectInputStream obj_in;
		private final GlobalState gs;

		ConnectedThread(GlobalState gs, BluetoothSocket socket) {
			this.gs=gs;
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			ObjectOutputStream tmp_obj_out=null;
			ObjectInputStream tmp_obj_in=null;


			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
				//stream for sending objects.
				tmp_obj_out = new
						ObjectOutputStream (tmpOut);
				tmp_obj_in = 
						new ObjectInputStream (tmpIn);

			} catch (IOException e) { }

			obj_out = tmp_obj_out;
			obj_in = tmp_obj_in;




		}

		public void run() {
			// Keep listening to the InputStream until an exception occurs
			Object message=null;

			while (true) {
				// Read from the InputStream
				try {						
					message = obj_in.readObject();
					//broadcast to all listeners.
					broadcastData(message);
				} catch (ClassNotFoundException e) {
					if (message != null) {
						Log.e("NILS","CLASS NOT FOUND IN Stream for "+message.toString());
					}
					e.printStackTrace();
				} catch (Exception e) {
					Log.e("NILS","got strange exception in Inputstream");

					if (e instanceof StreamCorruptedException) {							
						broadcastEvent(ConnectionEvent.restartRequired);
						o.addText("");
						o.addCriticalText("Bluetooth: Stream corrupted. It is recommended that you restart your devices before attempting a new connection.");
					} else if (e instanceof IOException) {
						broadcastEvent(ConnectionEvent.connectionBroken);
					}
					else {
						e.printStackTrace();
						broadcastEvent(ConnectionEvent.connectionBroken);
					}
					break;
				}
			}
		}

		// Write object with ObjectOutputStream
        void write(Object message) {
			try {

				obj_out.writeObject(message);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}



		/* Call this from the main activity to shutdown the connection */

		void cancel() {
			if (mConnected_T==null ) {
				Log.d("vortex","I was already closed...");
				return;
			}
			try {
				obj_out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				obj_in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (mmSocket.isConnected()) {
				Log.d("nils","ISCONNECTED CALLING SOCKET CLOSE!!!");
				//Is this always called anyway??
				try {
					mmSocket.close();
				} catch (IOException e) { e.printStackTrace();}
			}


		}
	}

	@Override
	public int getTriesRemaining() {
		if (mClient!=null)
			return mClient.attemptsRemaining();
		return -1;
	}

	@Override
	public boolean isOpen() {
		return internalState.equals(InternalState.open);
	}

}
