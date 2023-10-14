package com.teraim.fieldapp.dynamic.templates;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Filter;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_List;
import com.teraim.fieldapp.dynamic.workflow_realizations.filters.WF_OnlyWithValue_Filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Terje
 * Activity that runs a workflow that has a user interface.
 * Pressing Back button will return flow to parent workflow.
 */

public class TableDefaultTemplate extends Executor implements Animation.AnimationListener {



	View view;
	private LinearLayout my_root,displayPanel,tablePanel,filterPanel,filterPanel_2,filterPop,filterC1,filterC2,filterC3,filterC4;
	private View filterC1o,filterC2o,filterC3o,filterC4o;
	private FrameLayout frame;
	private Map<String,Boolean> popupVisible=null;
	private Animation popupShow;
	private Animation popupHide;
	private boolean animationRunning=false;
	private LayoutInflater inflater=null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("nils","In onCreate");


	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d("nils","I'm in the onPause method");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		Log.d("nils","I'm in the onCreateView method");
		popupVisible=new HashMap<String, Boolean>();
		if (myContext == null) {
			Log.e("vortex","No context, exit");
			return null;
		}
		this.inflater = inflater;
		View v = inflater.inflate(R.layout.template_table_default, container, false);
//		errorView = (TextView)v.findViewById(R.id.errortext);
		frame = v.findViewById(R.id.frame);
		my_root = v.findViewById(R.id.myRoot);
		displayPanel = v.findViewById(R.id.displayPanel);
		tablePanel = v.findViewById(R.id.myTable);
		filterPanel = v.findViewById(R.id.filterPanel);
		filterPanel_2 = v.findViewById(R.id.filterPanel_2);

		filterC1o = inflater.inflate(R.layout.filter_pop_inner,null);
		filterC2o = inflater.inflate(R.layout.filter_pop_inner,null);
		filterC3o = inflater.inflate(R.layout.filter_pop_inner,null);
		filterC4o = inflater.inflate(R.layout.filter_pop_inner,null);
		filterC1 = filterC1o.findViewById(R.id.inner);
		filterC2 = filterC2o.findViewById(R.id.inner);
		filterC3 = filterC3o.findViewById(R.id.inner);
		filterC4 = filterC4o.findViewById(R.id.inner);
		myContext.addContainers(getContainers());

		/*Display display = getActivity().getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		int height = size.y;
		filterPop.setY(height/2);
		*/

		if (wf!=null) {
			Log.d("cair","Executing workflow!!");
			run();

		} else
			Log.d("cair","No workflow found in oncreate default!!!!");

		popupShow = AnimationUtils.loadAnimation(getActivity(), R.anim.popup_show);
		popupShow.setAnimationListener(this);
		popupHide = AnimationUtils.loadAnimation(getActivity(), R.anim.popup_hide);
		popupHide.setAnimationListener(this);

		return v;
	}

	@Override
	protected List<WF_Container> getContainers() {
		ArrayList<WF_Container> ret = new ArrayList<WF_Container>();
		//WF_Container frameC = new WF_Container("frame", frame, null);
		WF_Container root = new WF_Container("root",my_root, null);

		//ret.add(frameC);
		ret.add(root);
		ret.add(new WF_Container("display_panel",displayPanel,root));
		ret.add(new WF_Container("table_panel",tablePanel,root));
		ret.add(new WF_Container("filter_panel",filterPanel,root));
		ret.add(new WF_Container("filter_panel_2",filterPanel_2,root));
		ret.add(new WF_Container("filter_C1",filterC1,root));
		ret.add(new WF_Container("filter_C2",filterC2,root));
		ret.add(new WF_Container("filter_C3",filterC3,root));
		ret.add(new WF_Container("filter_C4",filterC4,root));
		return ret;
	}


	@Override
	public boolean execute(String function, String target) {
		if(animationRunning)
			return false;
		Log.d("cair","Called execute with target "+target);
		if (function.equals("template_pop_up_filters") ) {

			Boolean popupVis = popupVisible.get(target);
			if (popupVis==null || !popupVis) {
				if (filterPop==null) {
					Log.d("cair","creating!");
					filterPop= (LinearLayout) inflater.inflate(R.layout.filter_menu_pop, null);

				}
				if (target.equals("filter_C1")) {
					filterPop.addView(filterC1o);
				} else if (target.equals("filter_C2")) {
					filterPop.addView(filterC2o);
				} else if (target.equals("filter_C3")) {
					filterPop.addView(filterC3o);
				} else
					filterPop.addView(filterC4o);

				if (filterPop.getParent()==null) {
					frame.addView(filterPop);
					filterPop.startAnimation(popupShow);
				}
				Log.d("cair", "animation started!");
				popupVisible.put(target,true);
			} else {
				Log.d("cair","deleting!");
				popupVisible.remove(target);

				if (popupVisible.isEmpty()) {
					filterPop.startAnimation(popupHide);
					Log.d("cair","empty! closing!");
					closePopIfUp();
				}
				else {
					View targetO;
					Log.d("cair","removing!");
					if (target.equals("filter_C1"))
						targetO=filterC1o;
					else if (target.equals("filter_C2"))
						targetO=filterC2o;
					else if (target.equals("filter_C3"))
						targetO=filterC3o;
					else
						targetO=filterC4o;
					filterPop.removeView(targetO);

				}
			}
		} else 	if (function.equals("template_function_show_only_edited"))
			showEdited(target);
	return true;
	}

	private final Filter f = new WF_OnlyWithValue_Filter("_filter");
	private boolean toggleStateH = true;

	private void showEdited(String target) {
		boolean filterWasRemoved=false;
		final WF_List fieldList = (WF_List)myContext.getFilterable(target);
		if (toggleStateH) {
			fieldList.addFilter(f);
		} else {
			fieldList.removeFilter(f);
			filterWasRemoved=true;
		}
		//need to review togglestate for columns.
		Log.d("cair","show edited...adding filter: "+toggleStateH);
		fieldList.draw();
		//Need to do uncollapse at this point in time.
//		if (filterWasRemoved)
//			((WF_Table)myContext.getFilterable(target)).unCollapse();
		toggleStateH = !toggleStateH;
	}


	@Override
	public void onStart() {
		Log.d("nils","I'm in the onStart method");
		super.onStart();


	}


	public void onAnimationStart(Animation animation) {
		if (animation.equals(popupShow)) {
			Log.d("cair","popshoe!");
			filterPop.setVisibility(View.VISIBLE);
		}
		animationRunning = true;
	}

	@Override
	public void onAnimationEnd(Animation animation) {
		animationRunning = false;
		if (animation.equals(popupHide)) {
			filterPop.removeAllViews();
			frame.removeView(filterPop);
			filterPop=null;
			popupVisible.clear();
		}
	}

	@Override
	public void onAnimationRepeat(Animation animation) {


	}

	private void closePopIfUp() {
		if (!popupVisible.isEmpty()) {
			filterPop.startAnimation(popupHide);
		}
	}
}
