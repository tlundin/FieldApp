package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;

import java.util.ArrayList;
import java.util.List;

public class WF_Container extends WF_Thing implements Container {
	private static final String TAG = "WF_Container";


	private final ViewGroup me;
	private final Container parent;
	private List<WF_Widget> myItems;

	public WF_Container(String id, ViewGroup container, Container parent) {
		super(id);
		this.parent=parent;
		me = container;
		if (container == null)
			Log.e("nils","UUUU Create container called with null");
		myItems = new ArrayList<WF_Widget>();
		
	}

	@Override
	public Container getParent() {
		return parent;
	}
	
	public ViewGroup getViewGroup() {
		return me;
	}

	@Override
	public Container getRoot() {
		Container parent = this;
		Container child = null;
		while (parent!=null) {
			child = parent;
			parent = parent.getParent();			
		}
		return child;
	}



	@Override
	public void draw() {

		Log.d(TAG,"in WF_Container draw with ID: "+this.getId()+". I have  "+myItems.size()+" widgets.");
		View v;

		for(WF_Widget d:myItems) {
			Log.d(TAG,"Drawing "+d.getId());
			v = d.getWidget();
			//If the widget is the container, don't draw. The same if the comp is already attached.
			if (v.equals(me)||(v.getParent()!=null && v.getParent().equals(me))) {
				Log.d(TAG,"Parent of this object is me. Skip draw!!!");
				continue;
			}
			me.addView(v);

			d.postDraw();
			
		} 

	}

	@Override
	public void add(WF_Widget d) {
		myItems.add(d);
	}

	@Override
	public void remove(WF_Widget d) {
		
		myItems.remove(d);
	}

	@Override
	public List<WF_Widget> getWidgets() {
		return myItems;
	}

	@Override
	public void removeAll() {
		Log.d(TAG,"cleaning up container "+getId());
		if (myItems!=null) {
			for (WF_Widget w:myItems) 
				me.removeView(w.getWidget());
			myItems = new ArrayList<WF_Widget>();
			//me.removeAllViews();
		}

	}



}
