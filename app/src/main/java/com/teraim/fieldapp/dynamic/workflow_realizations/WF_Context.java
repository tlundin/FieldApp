package com.teraim.fieldapp.dynamic.workflow_realizations;

/**
 * 
 * Copyright Teraim 2015.
 * Core class in the Vortex Engine.
 * Redistribution and changes only after agreement with Teraim.
 */


import android.content.Context;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.EventBroker;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.blocks.CoupledVariableGroupBlock;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.DataSource;
import com.teraim.fieldapp.dynamic.types.Rule;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Drawable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Filterable;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.WF_Gis_Map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WF_Context {

	private final FragmentActivity ctx;
	private final List<WF_Static_List> lists= new ArrayList<>();
	private final Map<String,Drawable> drawables;
	private List<WF_Container> containers;
	private transient Executor myTemplate;
	private final EventBroker eventBroker;
	private final Set<Rule> rules=new HashSet<Rule>();
	private final Set<Integer> executedBlocks = new HashSet<Integer>();
    private String statusVariable=null;
	private final List<Filterable> filterables;
	private WF_Gis_Map currentGis;
	private final List<WF_Gis_Map> gisses;
	private boolean hasGPSTracker = false;
	private DB_Context myHash;
	private Workflow myWorkflow;
	private boolean hasSatNav;
	private int mapLayer=0;
	private List<String> contextVariables = null;
	private boolean myEndIsNear=false;
	private boolean hasMenu = false;
	private final Map<String,List<WF_ClickableField_Slider>> sliderGroupM=new HashMap<String, List<WF_ClickableField_Slider>>();
	private final Map<String,CoupledVariableGroupBlock> mySliderGroups = new HashMap<String, CoupledVariableGroupBlock>();
	private final Map<String,DataSource> chartGroupM = new HashMap<String, DataSource>();
	private String gpsPriority="low";


	public WF_Context(FragmentActivity ctx, Executor e) {
		this.ctx=ctx;
		myTemplate = e;
		eventBroker = new EventBroker(ctx);
		this.drawables=new HashMap<String,Drawable>();
		this.filterables=new ArrayList<Filterable>();
		this.gisses=new ArrayList<WF_Gis_Map>();
	}
	public Context getContext() {
		return ctx;
	}
	
	public Workflow getWorkflow() {
		return myWorkflow;
	}
	
	public void setWorkflow(Workflow flow) {
		myWorkflow = flow;
	}

	public void addExecutedBlock(int blockId) {
		executedBlocks.add(blockId);
	}
	
	public void clearExecutedBlocks() {
		executedBlocks.clear();
	}
	
	public FragmentActivity getFragmentActivity() {
		return ctx;
	}

	public List<WF_Static_List> getLists() {
		return lists;
	}
	


	public  WF_Static_List getList(String id) {
		for (WF_Static_List wfl:lists) {
			String myId = wfl.getId();				
			if(myId!=null && myId.equalsIgnoreCase(id))
				return wfl;
		}
		return null;
	}	

	public void setHasMenu() {
		hasMenu=true;
	}
	
	public Filterable getFilterable(String id) {
		Log.d("nils","Getfilterable called with id "+id);
		if (id==null)
			return null;
		for (Filterable wfl:filterables) {
			Log.d("nils","filterable: "+wfl.getId());
			String myId = wfl.getId();				
			if(myId!=null && myId.equalsIgnoreCase(id))
				return wfl;
		}
		return null;
	}
	
	public void addContainers(List<WF_Container> containers) {
		this.containers = containers; 
	}
	//TODO: If required introduce nonfilterable lists and tables. For now it is assumed that all implements filterable. 
	public void addList(WF_Static_List l) {
		Log.d("zorg","Added list "+l.getId());
		Log.d("zorg","existing:");
		for (WF_Static_List lo:lists) {
			Log.d("zorg",lo.getId());
		}
		lists.add(l);
		filterables.add(l);
	}
	
	public void addDrawable(String key,Drawable d) {
		drawables.put(key,d);
	}

	public Drawable getDrawable(String name) {
		return drawables.get(name);
	}
	public Collection<Drawable> getDrawables() {
		return drawables.values();
	}

	public Container getContainer(String id) {
		if (containers == null)
			return null;
		//Log.d("nils","GetContainer. looking for container "+id);
		if (id==null || id.length()==0) {
			Log.e("nils","Container null!!");		
			return null;
		}
		for (WF_Container c:containers) {
			String myId =c.getId();				
			if(myId!=null && myId.equalsIgnoreCase(id))
				return c;
		}
		Log.e("nils","Failed to find container "+id);
		return null;
	}

	public boolean hasContainers() {
		return containers!=null && !containers.isEmpty();
	}

	public void resetState() {
		emptyContainers();
		lists.clear();
		filterables.clear();
		drawables.clear();
		sliderGroupM.clear();
		mySliderGroups.clear();
		chartGroupM.clear();
		eventBroker.removeAllListeners();
		rules.clear();
		currentGis = null;
		gisses.clear();
		hasGPSTracker=false;
		contextVariables=null;
		myEndIsNear=false;
		isCaller = false;
		if (hasMenu) {
			hasMenu = false;
			GlobalState.getInstance().getDrawerMenu().clear();
		}

	}

	private void emptyContainers() {
		if (containers!=null)
			for (Container c:containers) 
				c.removeAll();
	}


	public void emptyContainer(Container c) {
		c.removeAll();
	}


	//draws all containers traversing the tree.
	public void drawRecursively(Container c) {
		if (c==null) {
			Log.e("nils","This container is null");
			return;
		}

		c.draw();
		List<Container> cs = getChildren(c);
		for(Container child:cs)
			drawRecursively(child);
		
	}

	private List<Container> getChildren(Container key) {
		List<Container>ret = new ArrayList<Container>();
		if (key!=null) {
			Container parent;
			for(Container c:containers) {
				parent = c.getParent();
				if (parent == null) {
					//Log.e("nils","Parent is null in getChildren");
					continue;
				}
				if (parent.equals(key))
					ret.add(c);
			}
		}
		return ret;
	}

	public Executor getTemplate() {
		return myTemplate;
	}

	public void registerEventListener(EventListener el,
									  EventType et) {

		eventBroker.registerEventListener(et, el);
	}
	
	public void removeEventListener(EventListener eventListener) {
		eventBroker.removeEventListener(eventListener);
	}

	public List<EventListener> getEventListeners(EventType et) {
		return eventBroker.getEventListeners(et);
	}

	public void onEvent(Event ev) {
		eventBroker.onEvent(ev);
	}
	public void registerEvent(Event event) {
		eventBroker.onEvent(event);
	}

	
	/*
	public void onCreateView() {

		if (containers!=null) {
			resetState();
			containers.clear();
		}
	}
	*/

	public void addRule(Rule r) {
		rules.add(r);
	}

	/*public Set<Rule> getRules() {
		return rules;
	}
	*/
	public Set<Rule> getRulesThatApply() {
		Set<Rule> ret=new HashSet<Rule>();
		boolean add;
		for (Rule r:rules) {
			add=(r.getMyTargetBlockId()==-1)||(executedBlocks.contains(r.getMyTargetBlockId()));
			if (add)
				ret.add(r);
		}
		return ret;
	}

	public void setStatusVariable(String statusVar) {
		statusVariable = statusVar;
	}

	public String getStatusVariable() {
		return statusVariable;
	}
	public void addGis(String id, WF_Gis_Map wf_Gis_Map) {
		currentGis = wf_Gis_Map;
		gisses.add(wf_Gis_Map);
		mapLayer++;
	}
	
	public WF_Gis_Map getCurrentGis() {
		return currentGis;
	}
	
	public boolean hasMap() { return currentGis!=null; }
	public boolean hasGPSTracker() {
		return hasGPSTracker;
	}
	public boolean hasHighGPS() {
		return gpsPriority.equals("high");
	}
	public void enableGPS(String gpsPriority) {
		hasGPSTracker = true;
		if (gpsPriority!=null)
			this.gpsPriority = gpsPriority;
	}

	public void disableGPS() {
		hasGPSTracker = false;
	}

	public void setHash(DB_Context myHash) {
		this.myHash=myHash;
	}
	public DB_Context getHash() {
		return myHash;
	}
	public Map<String, String> getKeyHash() {
		if (myHash!=null)
			return myHash.getContext();
		else {
			Log.e("vortex", "myhash was null i getKeyHash, context!!");
			return null;
		}
	}
	
	public boolean hasSatNav() {
		return hasSatNav;
	}
	public void enableSatNav() {
		hasSatNav=true;
	}
	public void reload() {
		Log.d("hash","reloading mycontext to "+DB_Context.evaluate(getWorkflow().getContext()));
		setHash(DB_Context.evaluate(getWorkflow().getContext()));
		GlobalState.getInstance().setDBContext(getHash());
		myTemplate.restart();
	}
	
	public void refreshGisObjects(WF_Context my_context) {
		myTemplate.refreshGisObjects(my_context);
	}
	
	public void setContextVariables(List<String> contextVars) {
		contextVariables = contextVars;
	}
	
	public boolean isContextVariable(String cVar) {
		Log.d("boo","contextvar? "+cVar+" cVars "+contextVariables);
		if (contextVariables == null)
			return false;
		return (contextVariables.contains(cVar));
	}
	public void setMyEndIsNear() {
		Log.e("Vortex","MY END IS COMING!");
		myEndIsNear=true;
	}
	public boolean myEndIsNear() {
		return myEndIsNear;
	}
	
	
	public int getMapLayer() {
		return mapLayer;
	}
	public void upOneMapLevel() {
		mapLayer--;
	}

	public void printD() {
		for (String d:drawables.keySet()) {
			Log.d("Bortex","key: "+d);
		}
	}


	public List<WF_ClickableField_Slider> getSliderGroupMembers(String groupName) {
		if (sliderGroupM!=null)
			return sliderGroupM.get(groupName);
		return null;
	}

	public CoupledVariableGroupBlock getSliderGroup(String groupName) {
		return mySliderGroups.get(groupName);
	}

	public void addSliderGroup(CoupledVariableGroupBlock sliderGroup) {
		mySliderGroups.put(sliderGroup.getName(),sliderGroup);
	}

	public void addSliderToGroup(String groupName, WF_ClickableField_Slider wf_clickableField_slider) {
		List<WF_ClickableField_Slider> list = getSliderGroupMembers(groupName);
		if (list==null)
			list = new ArrayList<>();
		list.add(wf_clickableField_slider);
		sliderGroupM.put(groupName,list);
	}


	public void addChartDataSource(String myChart, DataSource dataSource) {
		chartGroupM.put(myChart,dataSource);
	}

	public DataSource getChartDataSource(String chartName) {
		return chartGroupM.get(chartName);
	}

	public boolean hasMenu() {
		return hasMenu;
	}

	private boolean isCaller = false;
    public void setCaller() {
		isCaller=true;
    }
    public boolean isCaller() {
		return isCaller;
	}

    public boolean isWorkFlowEnded() {
		return myEndIsNear;
    }
}
