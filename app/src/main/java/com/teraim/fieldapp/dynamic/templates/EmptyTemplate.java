package com.teraim.fieldapp.dynamic.templates;

import java.util.List;

import android.os.Bundle;
import android.util.Log;

import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;

public class EmptyTemplate extends Executor {


	@Override
	protected List<WF_Container> getContainers() {
		return null;
	}

	@Override
	public boolean execute(String function, String target) {
		return true;
	}

	@Override
	public void onStart() {
		Log.d("vortex","in onstart for empty fragment");
		super.onStart();
	}

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        if (wf!=null)
			run();


		
		
	}

	@Override
	public void onStop() {
		Log.d("blax","Emptytemplate on stop!");
		super.onStop();
	}

	@Override
	public void onDestroy() {
		Log.d("blax","on destroy!");
		super.onDestroy();
	}
}
