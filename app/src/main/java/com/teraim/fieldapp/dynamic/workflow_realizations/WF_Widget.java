package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.util.Log;
import android.view.View;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Drawable;

public class WF_Widget extends WF_Thing implements Drawable {
	private static final String TAG = "WF_Widget";


	private final View myView;
	private boolean isVisible;
	final VariableConfiguration al;


	public WF_Widget(String id,View v,boolean isVisible,WF_Context myContext) {
		super(id);
		al = GlobalState.getInstance().getVariableConfiguration();
		myView = v;
		if (!isVisible)
			hide();
		this.isVisible = isVisible;
		myContext.addDrawable(id, this);

	}


	@Override
	public View getWidget() {
		return myView;
	}


	@Override 
	public boolean isVisible() {
		return isVisible;
	}
	
	@Override
	public void show() {
		Log.d(TAG,"Showing view ");
		myView.setVisibility(View.VISIBLE);
		isVisible = true;
	}


	@Override
	public void hide() {
		Log.d(TAG,"Hiding view ");
		myView.setVisibility(View.GONE);
		isVisible = false;
	}


	public void postDraw() {

	}
}