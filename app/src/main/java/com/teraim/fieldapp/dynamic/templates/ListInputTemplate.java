package com.teraim.fieldapp.dynamic.templates;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Filter;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Static_List;
import com.teraim.fieldapp.dynamic.workflow_realizations.filters.WF_OnlyWithoutValue_Filter;

import java.util.ArrayList;
import java.util.List;


public class ListInputTemplate extends Executor {
	private static final String TAG = "ListInputTemplate";

    private List<WF_Container> myLayouts;


    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (myContext == null) {
			Log.e("vortex","No context, exit");
			return null;
		}
		Log.d(TAG,"in onCreateView of ListInputTemplate");
		myLayouts = new ArrayList<WF_Container>();

        /* (non-Javadoc)
         * @see android.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
         */
        View v = inflater.inflate(R.layout.template_list_input_wf, container, false);
		WF_Container root = new WF_Container("root", v.findViewById(R.id.root), null);
        LinearLayout sortPanel = v.findViewById(R.id.sortPanel);

		myLayouts.add(root);
		myLayouts.add(new WF_Container("Field_List_panel_1", v.findViewById(R.id.fieldList), root));
		myLayouts.add(new WF_Container("Sort_Panel_1", sortPanel, root));
		myLayouts.add(new WF_Container("Aggregation_panel_3", v.findViewById(R.id.aggregates), root));
		myLayouts.add(new WF_Container("Filter_panel_4", v.findViewById(R.id.filterPanel), root));
		myLayouts.add(new WF_Container("Field_List_panel_2", v.findViewById(R.id.Selected), root));
		myLayouts.add(new WF_Container("Button_panel_5", v.findViewById(R.id.Button_panel_5), root));
		myContext.addContainers(getContainers());

		if (wf!=null) {
			run();
		}
		
		return v;

	}



	/* (non-Javadoc)
	 * @see android.app.Fragment#onStart()
	 */
	@Override
	public void onStart() {
		super.onStart();

		Log.d(TAG,"in onStart");
		//myContainer.removeAllViews();
		//Create blocks for template functions.
	
	}



	@Override
	protected List<WF_Container> getContainers() {
		return myLayouts;
	}

	public boolean execute(String name, String target) {
		if (name.equals("template_function_hide_edited"))
			hideEdited(target);
		return true;
	}

	private final Filter f = new WF_OnlyWithoutValue_Filter("_filter");
	private boolean toggleStateH = true;
	private void hideEdited(String target) {
		final WF_Static_List fieldList = (WF_Static_List)myContext.getFilterable(target);
		if (toggleStateH) {
			fieldList.addFilter(f);
		} else
			fieldList.removeFilter(f);
		fieldList.draw();
		toggleStateH = !toggleStateH;
	}











}