package com.teraim.fieldapp.synchronization;

import android.content.Context;

public class MobileRadioConnectionProvider extends ConnectionProvider {

	public MobileRadioConnectionProvider(Context ctx) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void write(Object o) {
		assert(o!=null);
		
		
	}

	@Override
	public void openConnection(String partner) {
		//This doesnt apply. Connection is only open when needed.
	}

	@Override
	public void closeConnection() {
		//This doesnt apply. Connection is only open when needed.

	}

	@Override
	public void abortConnection() {
		//This doesnt apply. Connection is only open when needed.

	}

	@Override
	public int getTriesRemaining() {
		return 0;
	}

	@Override
	public boolean isOpen() {
	
		return true;
	}

}
