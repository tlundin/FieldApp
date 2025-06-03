package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.types.VariableCache;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Filter;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Filterable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Sortable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Sorter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class WF_List<Listable> extends WF_Widget implements Sortable,Filterable {

	private final List<Listable> list = new  ArrayList<>(); //Instantiated in constructor
	private final List<Filter> myFilters=new ArrayList<>();
	private final List<Sorter> mySorters=new ArrayList<>();
	private List<Listable> filteredList;

	final WF_Context myContext;
	final GlobalState gs;
	VariableCache varCache;
	//Keep track if list has changed and needs to be recalculated.
	private boolean redraw = false;
	private final ViewGroup myW;

	//Table needs to send only part of its layout to list
	 WF_List(String id,boolean isVisible,WF_Context ctx, View v) {
		super(id,v,isVisible,ctx);
		myContext = ctx;
		gs = GlobalState.getInstance();
		myW = (TableLayout)v.findViewById(R.id.table);
		redraw = true;

	}
	//TODO: MERGE THESE
	//How about using the Container's panel?? TODO
	 WF_List(String id,boolean isVisible,WF_Context ctx) {
		super(id,new LinearLayout(ctx.getContext()),isVisible,ctx);
		myW = (LinearLayout)this.getWidget();
		LinearLayout ll = (LinearLayout)myW;
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		myContext = ctx;
		gs = GlobalState.getInstance();
		varCache = gs.getVariableCache();

	}

	@Override
	public void addSorter(Sorter s) {
		mySorters.add(s);
		redraw=true;
	}
	@Override
	public void removeSorter(Sorter s) {
		mySorters.remove(s);
		redraw=true;
	}
	@Override
	public void addFilter(Filter f) {
		myFilters.add(f);
		redraw=true;
	}

	@Override
	public void removeFilter(Filter f) {
		myFilters.remove(f);
		redraw=true;
	}

	 void reSortAndFilter() {
		filteredList = list;
		if (myFilters != null) {
			List<Listable> listx = new ArrayList<>(list);
			for (Filter f : myFilters) {
				f.filter(listx);
			}
			filteredList = listx;
		}
		//Log.d("nils","before sorter: "+System.currentTimeMillis());
		if (mySorters != null) {
			for (Sorter s : mySorters) {
				filteredList = (List<Listable>) s.sort(filteredList);
			}
		}
		//Log.d("nils","After sorter: "+System.currentTimeMillis());

	}

	//public List<Listable> getList() {
	//	return list;
	//}

	void add(Listable l) {
		list.add(l);
		redraw=true;
	}

	public List<Listable> get() {
		return list;
	}

	void clear() {
		list.clear();
	}

	void resetOnEvent() {
		redraw=true;
	}

	private int intC=0;
	private boolean drawActive = false;
	public void draw() {
		//Log.d("draw","DRAW CALLED "+ (++intC)+" times in list"+this.getId());
		Log.d("nils","DrawActive "+drawActive);
		if (!drawActive) {
			drawActive = true;
			Log.d("zorgo","in redraw..."+(intC++)+" for list "+getId()+" wf_list: "+WF_List.this+" handler: "+this);

			//If list requires recalc, do it.
			if (redraw) {
				this.reSortAndFilter();
				prepareDraw();
				redraw=false;
				Log.d("baza","redraw list "+this.getId()+" filtered: "+filteredList.size()+" all: "+list.size());
				for (Listable l:filteredList) {
					//l.refreshInputFields();
					l.refresh();
					//Everything is WF_Widgets, so this is safe!
					myW.addView(((WF_Widget)l).getWidget());
					//Log.d("vortex","Drawing: "+l.getLabel());
				}
				//Log.d("nils","Settingdrawactive to false");

			} else
				Log.d("vortex","no redraw required");
			//Log.d("nils","Settingdrawactive to true from list"+this.getId());

			//final ProgressDialog progress = new ProgressDialog(myContext.getContext());

			//new Handler().postDelayed(new Runnable() {
			//	public void run() {


			drawActive = false;


			//	}
			//}, 0);



		} else
			Log.d("nils","DISCARDED DRAW CALL");



	}

	//Return true if incremental.

	boolean prepareIncrementalDraw(Listable l) {
		if (l==null || l.getKey()==null) {
			if (l!=null) {
				o.addRow("");
				o.addRedText("Empty EntryField detected with Label "+l.getLabel()+". This is normally due to a duplicate variable. Please check your configuration");
			}
			return false;
		}

		//for (Listable li:list)
		//Log.d("vortex","li label li key: "+li.getLabel()+" "+li.getKey());
		Iterator<Listable> it = list.iterator();
		while (it.hasNext()) {
			Listable li = it.next();
			if (li.getKey()==null) {
				o.addRow("");
				o.addRedText("Empty EntryField detected with Label "+li.getLabel()+". This is normally due to a duplicate variable. Please check your configuration");
				it.remove();
				continue;
			}
			if (li.getKey().equals(l.getKey())) {
				li.refresh();
				boolean isRemoved=false;
				for (Filter fi : myFilters)
					isRemoved = fi.isRemovedByFilter(li);
				Log.e("zuzz","isremoved "+isRemoved);
				if (isRemoved && filteredList.contains(li)) {
					filteredList.remove(li);
					Log.e("zuzz","removed "+li);
					myW.removeView(((WF_Widget)li).getWidget());
				}
				boolean added = false;
				boolean inFiltered = filteredList.contains(li);
				if (!isRemoved && !inFiltered) {
					filteredList.add(li);
					Log.e("zuzz","added true!"+li.getKey());
					added=true;
				} else {
					Log.e("zuzz", "added false!" + li.getKey() + ", " + inFiltered + ", " + isRemoved);
					Log.d("zuzz","INF: "+filteredList.toString());
				}
				int index=filteredList.size()-1;
				if (mySorters != null) {
					for (Sorter s : mySorters) {
						filteredList = (List<Listable>) s.sort(filteredList);
					}
					index = filteredList.indexOf(li);

				}
				if (added) {
					Log.e("zuzz","added "+li);
					myW.addView(((WF_Widget) li).getWidget(), index);

				}
				redraw = false;
				return true;
			}
		}

		Log.d("vortex","couldnt find "+l+" in prepareInc0");

		return false;
	}


	void prepareDraw() {
		Log.d("zorg","DISCARDED ALL");
		myW.removeAllViews();
	}
/*
	public static void listReady() {

		if (GlobalState.getInstance()!=null) {
			GlobalState.getInstance().getCurrentWorkflowContext().getTemplate().continueExecution();
		} else
			Log.e("vortex","GS null in Wf_list");
	}
*/
}
