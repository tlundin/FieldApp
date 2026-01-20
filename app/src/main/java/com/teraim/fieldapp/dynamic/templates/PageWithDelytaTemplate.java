package com.teraim.fieldapp.dynamic.templates;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.DelyteManager;
import com.teraim.fieldapp.ui.ProvytaView;

import java.util.ArrayList;
import java.util.List;

public class PageWithDelytaTemplate extends Executor {
	private static final String TAG = "PageWithDelytaTemplate";


	@Override
	protected List<WF_Container> getContainers() {
        List<WF_Container> myLayouts = new ArrayList<WF_Container>();
		WF_Container root = new WF_Container("root", v.findViewById(R.id.root), null);
		myLayouts.add(root);
		myLayouts.add(new WF_Container("Field_panel_1", v.findViewById(R.id.fieldList), root));
		myLayouts.add(new WF_Container("Aggregation_panel_3", v.findViewById(R.id.aggregates), root));
		myLayouts.add(new WF_Container("Description_panel_2", v.findViewById(R.id.Description), root));
		
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
			Log.d(TAG,"hasnt survived create...exiting.");
			return null;
		}
		Log.d(TAG,"in onCreateView of Template PAGE with DELYTA");
		v = inflater.inflate(R.layout.template_page_with_agg_wf, container, false);	
		
		//myContext.onCreateView();
		myContext.addContainers(getContainers());
		
		ViewGroup provytaViewPanel = (LinearLayout)v.findViewById(R.id.Description);
		DelyteManager dym = DelyteManager.getInstance();

		//Marker man = new Marker(BitmapFactory.decodeResource(getResources(),R.drawable.icon_man));

		ProvytaView pyv = new ProvytaView(this.getActivity(), null,Constants.isAbo(dym.getPyID()));


		if (wf!=null) {
			run();
		}	
		
		provytaViewPanel.addView(pyv);
		pyv.showDelytor(dym.getDelytor(),true);
		return v;
		
	}
}
