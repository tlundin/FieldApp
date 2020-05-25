package com.teraim.fieldapp.dynamic.templates;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;

import java.util.ArrayList;
import java.util.List;

public class PageWithAggregationTemplate extends Executor {

	private ScrollView mScrollView;
	private int[] scrollPosition;

	@Override
	protected List<WF_Container> getContainers() {
        List<WF_Container> myLayouts = new ArrayList<WF_Container>();
		WF_Container root = new WF_Container("root", v.findViewById(R.id.root), null);
		myLayouts.add(root);
		myLayouts.add(new WF_Container("Field_panel_1", v.findViewById(R.id.fieldList), root));
		myLayouts.add(new WF_Container("Aggregation_panel_3", v.findViewById(R.id.aggregates), root));
		myLayouts.add(new WF_Container("Description_panel_2", v.findViewById(R.id.Description), root));
		myLayouts.add(new WF_Container("Button_panel_5", v.findViewById(R.id.Description), root));
		
		return myLayouts;
	}

	@Override
	public boolean execute(String function, String target) {
		return true;
	}

    ViewGroup myContainer = null;
	private View v;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (!survivedCreate) {
			Log.d("vortex","hasnt survived create...exiting.");
			return null;
		}
		Log.d("nils","in onCreateView of Template PAGE with AGGregation");
		v = inflater.inflate(R.layout.template_page_with_agg_wf, container, false);	
		
		//myContext.onCreateView();
		myContext.addContainers(getContainers());
		if (wf!=null) {
			run();
		}

		mScrollView = v.findViewById(R.id.scrollView3);
		
		return v;
	}


	@Override
	public void onResume() {
		if(scrollPosition != null) {
			mScrollView.post(new Runnable() {
				public void run() {
					mScrollView.scrollTo(scrollPosition[0], scrollPosition[1]);
				}
			});
		}
		Log.d("nils","in onResume of Template PAGE with AGGregation");
		super.onResume();
	}

	@Override
	public void onPause() {
		if (mScrollView!=null)
			scrollPosition= new int[]{ mScrollView.getScrollX(), mScrollView.getScrollY()};
		super.onPause();
	}


}
