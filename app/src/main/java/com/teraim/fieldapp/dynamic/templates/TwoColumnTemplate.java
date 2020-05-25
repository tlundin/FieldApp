package com.teraim.fieldapp.dynamic.templates;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Terje
 * Activity that runs a workflow that has a user interface.
 * Pressing Back button will return flow to parent workflow.
 */

public class TwoColumnTemplate extends Executor {



	View view;
	private ListView lv; 
	//private ValidatorListAdapter mAdapter;
	private LinearLayout my_root,fieldPanelLeft,fieldPanelRight;
	//private TextView errorView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("nils","In onCreate");

		
	}
	
	
	
	/* (non-Javadoc)
	 * @see android.app.Fragment#onPause()
	 */
	@Override
	public void onPause() {
		super.onPause();
		Log.d("nils","I'm in the onPause method");
	}



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d("vortex","I'm in the onCreateView method for twocolumntemplate");
		if (myContext == null) {
			Log.e("vortex","No context, exit");
			return null;
		}
		View v = inflater.inflate(R.layout.template_two_columns, container, false);
//		errorView = (TextView)v.findViewById(R.id.errortext);
		my_root = v.findViewById(R.id.myRoot);
		fieldPanelLeft = v.findViewById(R.id.fieldPanelLeft);
		fieldPanelRight = v.findViewById(R.id.fieldPanelRight);
//		my_pie = (LinearLayout)v.findViewById(R.id.pieRoot);
		myContext.addContainers(getContainers());

		if (wf!=null) {
			Log.d("vortex","Executing workflow!!");
			run();
		} else
			Log.d("vortex","No workflow found in oncreate two columns!!!!");
			
		
		return v;
	}
	
	@Override
	protected List<WF_Container> getContainers() {
		ArrayList<WF_Container> ret = new ArrayList<WF_Container>();
		WF_Container root = new WF_Container("root",my_root,null);
		ret.add(root);
		ret.add(new WF_Container("Field_panel_1_left",fieldPanelLeft,root));
		ret.add(new WF_Container("Field_panel_1_right",fieldPanelRight,root));
		return ret;
	}
	@Override
	public boolean execute(String function, String target) {
		return true;
	}
	
	

	@Override
	public void onStart() {
		Log.d("nils","I'm in the onStart method");
		super.onStart();


	}



}
