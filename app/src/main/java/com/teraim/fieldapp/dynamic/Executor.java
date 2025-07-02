package com.teraim.fieldapp.dynamic;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import androidx.fragment.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;

import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Looper;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.blocks.AddEntryToFieldListBlock;
import com.teraim.fieldapp.dynamic.blocks.AddFilter;
import com.teraim.fieldapp.dynamic.blocks.AddGisFilter;
import com.teraim.fieldapp.dynamic.blocks.AddGisLayerBlock;
import com.teraim.fieldapp.dynamic.blocks.AddGisPointObjects;
import com.teraim.fieldapp.dynamic.blocks.AddSumOrCountBlock;
import com.teraim.fieldapp.dynamic.blocks.AddVariableToEntryFieldBlock;
import com.teraim.fieldapp.dynamic.blocks.AddVariableToEveryListEntryBlock;
import com.teraim.fieldapp.dynamic.blocks.AddVariableToListEntry;
import com.teraim.fieldapp.dynamic.blocks.BarChartBlock;
import com.teraim.fieldapp.dynamic.blocks.Block;
import com.teraim.fieldapp.dynamic.blocks.BlockAddAggregateColumnToTable;
import com.teraim.fieldapp.dynamic.blocks.BlockAddColumnsToTable;
import com.teraim.fieldapp.dynamic.blocks.BlockAddVariableToTable;
import com.teraim.fieldapp.dynamic.blocks.BlockCreateListEntriesFromFieldList;
import com.teraim.fieldapp.dynamic.blocks.BlockCreateTable;
import com.teraim.fieldapp.dynamic.blocks.BlockCreateTableEntriesFromFieldList;
import com.teraim.fieldapp.dynamic.blocks.BlockCreateTextField;
import com.teraim.fieldapp.dynamic.blocks.BlockDeleteMatchingVariables;
import com.teraim.fieldapp.dynamic.blocks.BlockGoSub;
import com.teraim.fieldapp.dynamic.blocks.ButtonBlock;
import com.teraim.fieldapp.dynamic.blocks.ConditionalContinuationBlock;
import com.teraim.fieldapp.dynamic.blocks.ContainerDefineBlock;
import com.teraim.fieldapp.dynamic.blocks.CoupledVariableGroupBlock;
import com.teraim.fieldapp.dynamic.blocks.CreateCategoryDataSourceBlock;
import com.teraim.fieldapp.dynamic.blocks.CreateEntryFieldBlock;
import com.teraim.fieldapp.dynamic.blocks.CreateGisBlock;
import com.teraim.fieldapp.dynamic.blocks.CreateImageBlock;
import com.teraim.fieldapp.dynamic.blocks.CreateSliderEntryFieldBlock;
import com.teraim.fieldapp.dynamic.blocks.CreateSortWidgetBlock;
import com.teraim.fieldapp.dynamic.blocks.DisplayValueBlock;
import com.teraim.fieldapp.dynamic.blocks.JumpBlock;
import com.teraim.fieldapp.dynamic.blocks.MenuEntryBlock;
import com.teraim.fieldapp.dynamic.blocks.MenuHeaderBlock;
import com.teraim.fieldapp.dynamic.blocks.NoOpBlock;
import com.teraim.fieldapp.dynamic.blocks.PageDefineBlock;
import com.teraim.fieldapp.dynamic.blocks.RoundChartBlock;
import com.teraim.fieldapp.dynamic.blocks.RuleBlock;
import com.teraim.fieldapp.dynamic.blocks.SetValueBlock;
import com.teraim.fieldapp.dynamic.blocks.SetValueBlock.ExecutionBehavior;
import com.teraim.fieldapp.dynamic.blocks.StartCameraBlock;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.GisLayer;
import com.teraim.fieldapp.dynamic.types.Rule;
import com.teraim.fieldapp.dynamic.types.SweLocation;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.VariableCache;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnFlowExecuted;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Static_List;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Table;
import com.teraim.fieldapp.gis.TrackerListener;
import com.teraim.fieldapp.log.LogRepository;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.ui.MenuActivity;
import com.teraim.fieldapp.utils.Expressor.Atom;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;
import com.teraim.fieldapp.utils.Geomatte;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;
import com.teraim.fieldapp.viewmodels.GisViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.teraim.fieldapp.gis.TrackerListener.GPS_State.GPS_State_C;

/**
 * Executor - executes workflow blocks.  
 * Copyright Teraim 2015.
 * Core class in the Vortex Engine.
 * Redistribution and changes only after agreement with Teraim.
 */


public abstract class Executor extends Fragment implements AsyncResumeExecutorI {


	private static final String STOP_ID = "STOP";

	public static final String REDRAW_PAGE = "executor_redraw_page";
	private static final String REFRESH_AFTER_SUBFLOW_EXECUTION = "executor_refresh_after_subflow";

	private Long oldT = null;

	protected Workflow wf;

	//Extended context.
	protected WF_Context myContext;


	//Keep track of input in below arraylist.

	protected final Map<Rule,Boolean>executedRules = new LinkedHashMap<Rule,Boolean>();

	protected List<Rule> rules = new ArrayList<Rule>();
	private List<Workflow> wfStack;
	private Map<String,BlockCreateListEntriesFromFieldList> myListBlocks;
	private FusedLocationProviderClient fusedLocationClient;

	public WF_Context getCurrentContext() {
		return myContext;
	}


	protected abstract List<WF_Container> getContainers();
	public abstract boolean execute(String function, String target);

	protected GlobalState gs;

	protected LogRepository o;
	private BroadcastReceiver brr;
	private final Map<String,String> jump= new HashMap<String,String>();
	private Set<Variable> visiVars;

	protected VariableConfiguration al;

	protected VariableCache varCache;

	private int savedBlockPointer=-1;

	private List<Block> blocks;

	//Create pop dialog to display status.
	private ProgressDialog pDialog;
	protected boolean survivedCreate = false;
	private WF_Event_OnSave delayedOnSave=null;

	private Variable myX, myY, myAcc;
	private LocationCallback locationCallback;

	private IntentFilter ifi;
	private GisViewModel gisViewModel;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		survivedCreate = false;

		gs = GlobalState.getInstance();
		if (gs == null) {
			Log.e("vortex","globalstate null in executor, exit");
			return;
		}
		al = gs.getVariableConfiguration();
		varCache=gs.getVariableCache();
		o = gs.getLogger();


		ifi = new IntentFilter();
		ifi.addAction(REDRAW_PAGE);
		ifi.addAction(REFRESH_AFTER_SUBFLOW_EXECUTION);
		//ifi.addAction(BluetoothConnectionService.BLUETOOTH_MESSAGE_RECEIVED);
		//This receiver will forward events to the current context.
		//Bluetoothmessages are saved in the global context by the message handler.
		brr = new BroadcastReceiver() {
			@Override
			public void onReceive(Context ctx, Intent intent) {
				Log.d("vortex","GETS HERE:::::: "+this.toString()+"  P: "+Executor.this.toString());
				if (intent.getAction().equals(REDRAW_PAGE)) {
					boolean callAfterSub=intent.getBooleanExtra("RedrawAfterExecutingSub",false);
					Log.d("vortex","callAfterSUB: "+callAfterSub);
					if (!callAfterSub || callAfterSub && myContext.isCaller()) {

						Log.d("vortex","Setting DB context in broadcastreceiver");
						gs.setDBContext(myContext.getHash());
						Log.d("vortex","Redraw page received in Executor. Sending onSave event.");
						Log.d("vortex","my parent: "+Executor.this.getClass().getCanonicalName());
						myContext.registerEvent(new WF_Event_OnSave(Constants.SYNC_ID));

					} else
						Log.d("vortex"," i am not a parent");



					//Executor.this.restart();

				}
					/*
				else if (intent.getAction().equals(BluetoothConnectionService.BLUETOOTH_MESSAGE_RECEIVED)) {
					Log.d("nils","New bluetoot message received event!");
					myContext.registerEvent(new WF_Event_OnBluetoothMessageReceived());
				}
					 */

			}
		};
		gisViewModel = new ViewModelProvider(requireActivity()).get(GisViewModel.class);

		// ADD THIS OBSERVER
		// This observer waits for the download to finish
		gisViewModel.getDownloadResult().observe(this, event -> {
			// Consume the event. If it's already been handled, result will be null.
			if (event == null) {
				return;
			}
			GisViewModel.GisResult result = event.getContentIfNotHandled();
			if (result == null) {
				// This event has already been processed, so we do nothing.
				return;
			}
			Log.d("GisTrace", "Observer: Received result with masterPicName: " + result.masterPicName);
			// The rest of your logic is now protected from running twice
			if (result.isSuccess()) {
				Block currentBlock = blocks.get(result.blockIndex);
				if (currentBlock instanceof CreateGisBlock) {
					savedBlockPointer = result.blockIndex + 1;
					((CreateGisBlock) currentBlock).loadMetaAndCreateView(result, myContext, this);
				} else {
					Log.e("Executor", "Resumed from GIS download but the block was not a CreateGisBlock!");
					if (savedBlockPointer != -1) {
						this.execute(savedBlockPointer);
					}
				}
			} else {
				abortExecution(result.error.getMessage());
			}
		});


		myContext = new WF_Context(this.getActivity(),this);
		wf = getFlow();
		if (wf == null) {
			Log.e("Vortex","WF was null in Executor. Exiting...");
			return;
		} else {
			myContext.setWorkflow(wf);

			Log.d("GPS","tracker created");
			Map<String, String> gpsKeyHash = GlobalState.getInstance().getVariableConfiguration().createGpsKeyMap();
			myX = GlobalState.getInstance().getVariableCache().getVariable(gpsKeyHash, NamedVariables.MY_GPS_LAT);
			myY = GlobalState.getInstance().getVariableCache().getVariable(gpsKeyHash, NamedVariables.MY_GPS_LONG);
			myAcc = GlobalState.getInstance().getVariableCache().getVariable(gpsKeyHash, NamedVariables.MY_GPS_ACCURACY);

			locationCallback = new LocationCallback() {
				@Override
				public void onLocationResult(LocationResult locationResult) {
					if (locationResult == null) {
						return;
					}
					for (Location location : locationResult.getLocations()) {
						onLocationChanged(location);
					}
				}
			};

			fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.getContext());

			Log.d("nils","GETS TO ONCREATE EXECUTOR FOR WF "+wf.getLabel());
			survivedCreate = true;
		}






	}



	@Override
	public void onResume() {
		Log.d("vortex", "in Executor onResume " + this.toString());
		gs = GlobalState.getInstance();
		if (gs == null) {
			Log.e("vortex","globalstate null in executor, exit");
		} else {
			if (myContext != null) {
				if (myContext.hasGPSTracker()) {
					LocationRequest lr = myContext.hasHighGPS() ? createHighFrequencyLocationRequest() : createLowFrequencyLocationRequest();
					startLocationUpdates(lr, locationCallback);
				}
				resetContext();
				//make sure the correct map is updated.
				if (myContext.hasMap())
					gs.registerUpdateListener(myContext.getCurrentGis());
			}
			if (brr != null && getActivity() != null)
				LocalBroadcastManager.getInstance(getActivity()).registerReceiver(brr,
						ifi);
		}
		super.onResume();
	}

	private LocationRequest createHighFrequencyLocationRequest() {
		return new LocationRequest.Builder(
				Priority.PRIORITY_HIGH_ACCURACY,
				1000L // 1 second
		)
				.setMinUpdateIntervalMillis(500L) // Fastest update if other apps request
				.build();
	}

	private LocationRequest createLowFrequencyLocationRequest() {
		return new LocationRequest.Builder(
				Priority.PRIORITY_BALANCED_POWER_ACCURACY, // Optimizes for power, may not use GPS often
				60000L // 1 minute
		)
				.setMaxUpdateDelayMillis(5 * 60 * 1000L) // Allow batching up to 5 minutes
				.build();
	}

	private void startLocationUpdates(LocationRequest locationRequest, LocationCallback locationCallback) {
		Log.d("GPS","Start location updates "+this.hashCode());
		if (ActivityCompat.checkSelfPermission(this.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			return;
		}
		fusedLocationClient.requestLocationUpdates(locationRequest,
				locationCallback,
				Looper.getMainLooper());
		TrackerListener.GPS_State signal = GPS_State_C(TrackerListener.GPS_State.State.enabled);
		gs.updateCurrentPosition(signal,this.hashCode());
	}

	private void stopLocationUpdates() {
		Log.d("GPS","Stop location updates "+this);
		fusedLocationClient.removeLocationUpdates(locationCallback);
		TrackerListener.GPS_State signal = GPS_State_C(TrackerListener.GPS_State.State.disabled);
		gs.updateCurrentPosition(signal,this.hashCode());
	}

	private void resetContext() {
		Log.d("hash","resetting global context");
		Log.d("hash","local: "+myContext.getHash());
		Log.d("hash","global: "+gs.getVariableCache().getContext());
		if (myContext.getHash()==null) {
			myContext.setHash(DB_Context.evaluate(wf.getContext()));
		}
		gs.setDBContext(myContext.getHash());

	}


	@Override
	public void onPause()
	{

		Log.d("Vortex", "onPause() for executor "+this.toString());
		if (myContext != null) {
			if (myContext.hasGPSTracker())
				stopLocationUpdates();
		}
		if (brr!=null && getActivity()!=null)
			LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(brr);


		super.onPause();
	}



	private Workflow getFlow() {
		Workflow wf=null;
		//Find out the name of the workflow to execute.
		Bundle b = this.getArguments();
		String name;
		String statusVariable=null;
		if (b!=null) {
			name = b.getString("workflow_name");
			statusVariable = b.getString("status_variable");
		} else {
			Log.d("vortex", "BUNDLE null in executor");
			name = "Main";
		}
		Log.d("GisTrace", "getFlow: Attempting to load workflow with name: '" + name + "'");

			if (statusVariable !=null) {
				myContext.setStatusVariable(b.getString("status_variable"));
				//Add onSaveListener for the statusvariable. Change to "1" when first value saved.
				//Log.e("vortex","Added onsave listener for "+b.getString("status_variable"));
				myContext.registerEventListener(new EventListener() {
					@Override
					public void onEvent(Event e) {
						//Log.d("grog","Received onSave in statusvariable change on first save event");
						//Log.d("grogg","Context keychain is "+myContext.getKeyHash());
						String statusVar = myContext.getStatusVariable();
						Variable statusVariable =null;
						if (statusVar!=null)
							statusVariable = varCache.getVariable(myContext.getKeyHash(),statusVar);
						else
							Log.e("vortex","statusvariable is null for "+statusVar+" with hash "+myContext.getKeyHash() );
						/*
						if (statusVariable!=null && statusVariable.getValue()!=null && statusVariable.getValue().equals(Constants.STATUS_INITIAL)) {
							statusVariable.setValue(Constants.STATUS_STARTAD_MEN_INTE_KLAR);

						} else
							Log.e("vortex","value not set...because "+statusVar+" with hash "+myContext.getKeyHash() +" with value "+statusVariable.getValue());
						*/
						myContext.removeEventListener(this);
					}

					@Override
					public String getName() {
						return "Executor_StatusVar_onSave";
					}
				}, EventType.onSave);
			}
			if (name!=null && name.length()>0)
				wf = gs.getWorkflow(name);
			if (wf != null) {
				Log.d("GisTrace", "getFlow: GlobalState returned workflow with label: '" + wf.getLabel() + "' containing " + wf.getBlocks().size() + " blocks.");
			} else {
				Log.d("GisTrace", "getFlow: GlobalState returned a NULL workflow for name: '" + name + "'");
			}
			if (wf==null&&name!=null&&name.length()>0) {
				
				o.addYellowText("Workflow "+name+" NOT found!");
				return null;
			}


		return wf;
	}


	/**
	 * Execute the workflow.
	 */
	protected void run() {
		//If a stack exists, discard it.
		if (wfStack!=null) {
			if (!wfStack.isEmpty())
				wf = wfStack.get(0);
			wfStack=null;
		}
		String wfLabel = wf.getLabel();
		
		
		o.addGreenText("*******EXECUTING: "+wfLabel);
		gs.setTitle(wfLabel);
		Log.d("vortex","in Executor run()");

		myContext.resetState();
		resetContext();
		//DB_Context wfHash = DB_Context.evaluate(wf.getContext());
        /*if (myContext.getHash() ==null) {
            Log.d("hash","setting mycontext hash to "+gs.getVariableCache().getContext());
            myContext.setHash(gs.getVariableCache().getContext());
        }
        */
		//gs.setCurrentWorkflowContext(myContext);
		//gs.setDBContext(wfHash);
		wf = getFlow();
		myContext.setWorkflow(wf);
		//Need to write down all variables in wf context keyhash.
		List<String> contextVars=null;
		if (wf.getContext()!=null) {
			for (EvalExpr e:wf.getContext()) {
				if (e instanceof Atom) {
					if (((Atom)e).isVariable()) {
						Log.d("vortex","Found variable in context:"+e.toString());
						if (contextVars==null)
							contextVars = new ArrayList<String>();
						contextVars.add(e.toString());
					}
				}
			}
		}
		myContext.setContextVariables(contextVars);
		//gs.setCurrentWorkflowContext(myContext);
		gs.sendEvent(MenuActivity.REDRAW);
		visiVars = new HashSet<Variable>();
		//LinearLayout my_root = (LinearLayout) findViewById(R.id.myRoot);		
		blocks = wf.getCopyOfBlocks();
		jump.clear();
		myListBlocks = new HashMap<>();

		Log.d("hash","*******EXECUTING: "+wf.getLabel());
		Log.d("hash","myHash: "+myContext.getHash());
		execute(0);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

	}

	private void execute(int blockP) {

		boolean notDone = true;
		savedBlockPointer = -1;
		Log.d("vortex", "in execute with blockP " + blockP);
		myContext.clearExecutedBlocks();
		try {

			while (notDone) {

				if (blockP >= blocks.size()) {

					Log.d("vortex", "EXECUTION STOPPED ON BLOCK " + (blockP - 1));
					if (wfStack != null) {
						wf = wfStack.remove(wfStack.size() - 1);
						if (wf == null)
							Log.e("brexit", "WF IS NULL AFTER STACK POP IN EXECUTOR");
						else {
							if (wfStack.isEmpty())
								wfStack = null;

							blockP = wf.getBlockPointer() + 1;
							blocks = wf.getCopyOfBlocks();
							Log.d("vortex", "popped to wf " + wf.getName() + " blockP is now " + blockP);
							continue;
						}
					} else {
						notDone = false;
						Log.d("brexit", "wfStack was empty - exiting");
						break;
					}
				}
				Block b = blocks.get(blockP);
				Log.d("vortex", "In execute with block " + b.getClass().getSimpleName() + " ID: " + b.getBlockId());

				//Add block to list of executed blocks.
				try {
					myContext.addExecutedBlock(Integer.parseInt(b.getBlockId()));
				} catch (NumberFormatException e) {
					Log.e("vortex", "blockId was not Integer");
				}

				if (b instanceof PageDefineBlock) {
					PageDefineBlock bl = (PageDefineBlock) b;
					Log.d("vortex", "Found pagedefine!");
					if (bl.hasGPS()) {
						myContext.enableGPS(bl.gpsPriority());
						o.addText("GPS scanning started");
						Log.d("vortex", "GPS scanning started");
					} else {
						myContext.disableGPS();
						Log.e("abba", "GPS is turned of for " + wf.getLabel());
					}


				} else if (b instanceof ContainerDefineBlock) {
					
					o.addYellowText("ContainerDefineBlock found " + b.getBlockId());
					String id = (((ContainerDefineBlock) b).getContainerName());
					if (id != null) {
						if (myContext.getContainer(id) != null) {
							o.addText("found template container for " + id);
						} else {
							
							o.addCriticalText("Could not find container " + id + " in template!");
						}

					}
				} else if (b instanceof ButtonBlock) {
					
					o.addYellowText("ButtonBlock found " + b.getBlockId());
					ButtonBlock bl = (ButtonBlock) b;
					bl.create(myContext);
				} else if (b instanceof BlockCreateTextField) {
					
					o.addYellowText("CreatTextBlock found " + b.getBlockId());
					BlockCreateTextField bl = (BlockCreateTextField) b;
					bl.create(myContext);
				} else if (b instanceof CreateSortWidgetBlock) {
					
					o.addYellowText("CreateSortWidgetBlock found " + b.getBlockId());
					CreateSortWidgetBlock bl = (CreateSortWidgetBlock) b;
					bl.create(myContext);
				}/*
			else if (b instanceof ListFilterBlock) {
				
				o.addYellowText("ListFilterBlock found");
				ListFilterBlock bl = (ListFilterBlock)b;
				bl.create(myContext);
			}*/ else if (b instanceof CreateEntryFieldBlock) {
					
					o.addYellowText("CreateEntryFieldBlock found " + b.getBlockId());
					CreateEntryFieldBlock bl = (CreateEntryFieldBlock) b;
					Log.d("NILS", "CreateEntryFieldBlock found");
					Variable v = bl.create(myContext);
					if (v != null)
						visiVars.add(v);
				} else if (b instanceof CreateSliderEntryFieldBlock) {
					
					o.addYellowText("CreateEntryFieldBlock found " + b.getBlockId());
					CreateSliderEntryFieldBlock bl = (CreateSliderEntryFieldBlock) b;
					Log.d("NILS", "CreateSliderEntryFieldBlock found");
					Variable v = bl.create(myContext);
					if (v != null)
						visiVars.add(v);
				} else if (b instanceof AddSumOrCountBlock) {
					
					o.addYellowText("AddSumOrCountBlock found " + b.getBlockId());
					AddSumOrCountBlock bl = (AddSumOrCountBlock) b;
					bl.create(myContext);
				} else if (b instanceof DisplayValueBlock) {
					
					o.addYellowText("DisplayValueBlock found " + b.getBlockId());
					DisplayValueBlock bl = (DisplayValueBlock) b;
					bl.create(myContext);
				} else if (b instanceof CoupledVariableGroupBlock) {
					
					o.addYellowText("Slidergroupblock found " + b.getBlockId());
					CoupledVariableGroupBlock bl = (CoupledVariableGroupBlock) b;
					bl.create(myContext);
				} else if (b instanceof AddVariableToEveryListEntryBlock) {
					
					o.addYellowText("AddVariableToEveryListEntryBlock found " + b.getBlockId());
					AddVariableToEveryListEntryBlock bl = (AddVariableToEveryListEntryBlock) b;

					if (myListBlocks.get(bl.getTarget()) != null) {
						Log.d("vortex", "Addvariable: found target list");
						myListBlocks.get(bl.getTarget()).associateVariableBlock(bl);

					}


				} else if (b instanceof BlockCreateListEntriesFromFieldList) {
					
					o.addYellowText("BlockCreateListEntriesFromFieldList found " + b.getBlockId());
					//delay creation until filters applied.
					BlockCreateListEntriesFromFieldList bl = (BlockCreateListEntriesFromFieldList) b;
					bl.create(myContext);
					myListBlocks.put(bl.getListId(), bl);

					/*
					if(!bl.create(myContext)) {
						//Pause execution and wait for callback..
						savedBlockPointer = blockP+1;
						return;

					}
					*/
				} else if (b instanceof StartCameraBlock) {
					
					o.addYellowText("BlockStartCamera found " + b.getBlockId());
					StartCameraBlock bl = (StartCameraBlock) b;
					bl.create(myContext);
				} else if (b instanceof BlockCreateTable) {
					
					o.addYellowText("BlockCreateTable found " + b.getBlockId());
					BlockCreateTable bl = (BlockCreateTable) b;
					bl.create(myContext);
				} else if (b instanceof BlockCreateTableEntriesFromFieldList) {
					
					o.addYellowText("BlockCreateTableEntriesFromFieldList found " + b.getBlockId());
					BlockCreateTableEntriesFromFieldList bl = (BlockCreateTableEntriesFromFieldList) b;
					bl.create(myContext);
				} else if (b instanceof BlockAddColumnsToTable) {
					
					o.addYellowText("BlockAddColumn(s)ToTable found " + b.getBlockId());
					BlockAddColumnsToTable bl = (BlockAddColumnsToTable) b;
					bl.create(myContext);
				} else if (b instanceof BlockAddAggregateColumnToTable) {
					
					o.addYellowText("BlockAddAggregateColumnToTable found " + b.getBlockId());
					BlockAddAggregateColumnToTable bl = (BlockAddAggregateColumnToTable) b;
					bl.create(myContext);
				} else if (b instanceof BlockAddVariableToTable) {
					
					o.addYellowText("BlockAddVariableToTable(s)ToTable found " + b.getBlockId());
					BlockAddVariableToTable bl = (BlockAddVariableToTable) b;
					bl.create(myContext);
				} else if (b instanceof AddVariableToEntryFieldBlock) {
					
					o.addYellowText("AddVariableToEntryFieldBlock found " + b.getBlockId());
					AddVariableToEntryFieldBlock bl = (AddVariableToEntryFieldBlock) b;
					Variable v = bl.create(myContext);
					if (v != null)
						visiVars.add(v);

				} else if (b instanceof AddVariableToListEntry) {
					
					o.addYellowText("AddVariableToListEntry found " + b.getBlockId());
					AddVariableToListEntry bl = (AddVariableToListEntry) b;
					Variable v = bl.create(myContext);
					//TODO: REMOVE THIS??
				} else if (b instanceof AddEntryToFieldListBlock) {
					
					o.addYellowText("AddEntryToFieldListBlock found " + b.getBlockId());
					AddEntryToFieldListBlock bl = (AddEntryToFieldListBlock) b;
					bl.create(myContext);

				} else if (b instanceof NoOpBlock) {
					
					o.addYellowText("Noopblock found and skipped! " + b.getBlockId());
				} else if (b instanceof BlockGoSub) {
					
					o.addYellowText("BlockGoSub found " + b.getBlockId());
					String target = ((BlockGoSub) b).getTarget();
					Log.d("vortex", "TARGET: " + target);
					if (target != null) {
						Workflow sub = gs.getWorkflow(target);
						if (sub != null) {
							Log.d("vortex", "Executing gosub to " + target);
							if (wfStack == null)
								wfStack = new ArrayList<Workflow>();
							wfStack.add(wf);
							wf.saveBlockPointer(blockP);
							sub.setPageDefineBlock(wf.getMyPageDefineBlock());
							wf = sub;
							blockP = 0;
							blocks = sub.getCopyOfBlocks();

						}
					}

				} else if (b instanceof JumpBlock) {
					
					o.addYellowText("Jumpblock found " + b.getBlockId());
					JumpBlock bl = (JumpBlock) b;
					jump.put(bl.getBlockId(), bl.getJumpTo());
				} else if (b instanceof CreateImageBlock) {
					
					o.addYellowText("CreateImageBlock found " + b.getBlockId());
					CreateImageBlock bl = (CreateImageBlock) b;
					bl.create(myContext);
				} else if (b instanceof SetValueBlock) {
					final SetValueBlock bl = (SetValueBlock) b;
					//final List<TokenizedItem> tokens = gs.getRuleExecutor().findTokens(bl.getFormula(),null);
					if (bl.getBehavior() != ExecutionBehavior.constant && bl.getBehavior() != ExecutionBehavior.constant_value) {
						EventListener tiva = new EventListener() {
							@Override
							public void onEvent(Event e) {
								if (!e.getProvider().equals(bl.getBlockId())) {
									Variable v = varCache.getVariable(bl.getMyVariable());
									if (v != null) {
										//String	eval = bl.evaluate(gs,bl.getFormula(),tokens,v.getType()== DataType.text);
										String val = v.getValue();
										String eval = bl.getEvaluation();

										o.addText("Variable: " + v.getId() + " Current val: " + val + " New val: " + eval);
										Log.d("vortex", "Variable: " + v.getId() + " Current val: " + val + " New val: " + eval);
										if (!(eval == null && val == null)) {
											if (eval == null && val != null || val == null && eval != null || !val.equals(eval)) {
												//Remove .0 

												v.setValue(eval);
												
												o.addYellowText("Value has changed to or from null in setvalueblock OnSave for block " + bl.getBlockId());
												Log.d("Vortex", "Value has changed to or from null in setvalueblock OnSave for block " + bl.getBlockId());

												
												o.addYellowText("BEHAVIOR: " + bl.getBehavior());
												if (bl.getBehavior() == ExecutionBehavior.update_flow) {
													if (myContext.myEndIsNear()) {
														Log.d("vortex", "Disregarding event since flow reexecute already in progress.");
														return;
													}
													Log.d("nils", "Variable has sideEffects...re-executing flow");
													myContext.setMyEndIsNear();
													new Handler().postDelayed(new Runnable() {
														public void run() {
															//myContext.resetState();

															Set<Variable> previouslyVisibleVars = visiVars;
															Executor.this.run();
															for (Variable v : previouslyVisibleVars) {
																Log.d("nils", "Previously visible: " + v.getId());
																boolean found = false;
																for (Variable x : visiVars) {
																	found = x.getId().equals(v.getId());
																	if (found)
																		break;
																}

																if (!found) {
																	Log.d("nils", "Variable gone.Removing");
																	v.deleteValue();
																	//here we need a onSave event.
																	//myContext.registerEvent(new WF_Event_OnSave("Delete_visivar_setvalue"));
																}

															}
														}
													}, 0);
												}
											}
										}
									} else {
										Log.e("vortex", "variable null in SetValueBlock");
										o.addText("Setvalueblock variable " + bl.getMyVariable() + " not found or missing columns");
									}

								} else
									o.addText("Discarded onSave Event from myself in SetValueBlock " + bl.getBlockId());
							}

							@Override
							public String getName() {
								return "SETVALUE BLOCK " + bl.getBlockId();
							}
						};
						Log.d("nils", "Adding eventlistener for the setvalue block");
						myContext.registerEventListener(tiva, EventType.onSave);
					}
					//Evaluate
					Variable v = varCache.getVariable(bl.getMyVariable());
					if (v != null) {
						String eval = bl.getEvaluation();
						
						o.addYellowText("SetValueBlock " + b.getBlockId() + " [" + bl.getMyVariable() + "]");
						o.addText("Evaluation: " + eval);

						if (eval == null) {
							if (bl.getBehavior() == ExecutionBehavior.constant) {
								
								o.addText("Execution stopped on SetValueBlock " + bl.getBlockId() + ". Expression " + bl.getExpression() + "evaluates to null");
								//jump.put(bl.getBlockId(), Executor.STOP_ID);
								notDone = false;
							} else {

							}
						}

						String val = v.getValue();
						if ((val == null && eval == null) ||
								(val != null && val.equals(eval))) {
							o.addText("No change. Value: " + val + " Eval: " + eval);
						} else {
							v.setValue(eval);
							o.addText(bl.getExpression() + " Eval: [" + eval + "]");
							//Take care of any side effects before triggering redraw.
							//Try to delay onSave.
							//myContext.registerEvent(new WF_Event_OnSave(bl.getBlockId()));
							delayedOnSave = new WF_Event_OnSave(bl.getBlockId());
							Log.d("blubb", "delayed onSaveEvent in SetValue block " + bl.getBlockId());
						}
					} else {
						
						o.addCriticalText("Variable [" + bl.getMyVariable() + "] is missing in SetValueBlock " + bl.getBlockId());
					}


				} else if (b instanceof ConditionalContinuationBlock) {
					
					o.addYellowText("ConditionalContinuationBlock " + b.getBlockId());
					final ConditionalContinuationBlock bl = (ConditionalContinuationBlock) b;
					final String formula = bl.getFormula();
					//final List<TokenizedItem> vars = gs.getRuleExecutor().findTokens(formula,null);
					if (bl.isExpressionOk()) {
						EventListener tiva = new EventListener() {
							@Override
							public void onEvent(Event e) {
								//discard if redraw is coming..
								if (myContext.myEndIsNear()) {
									Log.d("vortex", "Disregarding onsave due to ongoing reexecute.");
									return;
								}
								//If evaluation different than earlier, re-render workflow.
								if (bl.evaluate()) {
									//redraw! We block all other conditional blocks from triggering.
									myContext.setMyEndIsNear();
									new Handler().postDelayed(new Runnable() {
										public void run() {
											//myContext.resetState();
											Set<Variable> previouslyVisibleVars = visiVars;
											Executor.this.run();
											for (Variable v : previouslyVisibleVars) {
												Log.d("nils", "Previously visible: " + v.getId());
												boolean found = false;
												for (Variable x : visiVars) {
													found = x.getId().equals(v.getId());
													if (found)
														break;
												}

												if (!found) {
													Log.d("nils", "Variable not found.Removing");
													v.deleteValue();
													//myContext.registerEvent(new WF_Event_OnSave("Delete_visivar_cond_cont"));

												}

											}
										}
									}, 0);

								}

							}

							@Override
							public String getName() {
								return "ConditionalContinuation BLOCK " + bl.getBlockId();
							}
						};
						Log.d("nils", "Adding eventlistener for the conditional block");
						myContext.registerEventListener(tiva, EventType.onSave);
						//trigger event.
						bl.evaluate();

						switch (bl.getCurrentEval()) {
							case ConditionalContinuationBlock.STOP:
								jump.put(bl.getBlockId(), Executor.STOP_ID);
								break;
							case ConditionalContinuationBlock.JUMP:
								jump.put(bl.getBlockId(), bl.getElseId());
								break;
							case ConditionalContinuationBlock.NEXT:
								jump.remove(bl.getBlockId());
						}

					} else {
						
						o.addCriticalText("Cannot read formula: [" + formula + "] because of previous fail during the pre-Parse step. Please reimport the workflow configuration and check your log for the root cause.");
						Log.d("nils", "Parsing of formula failed - no variables: [" + formula + "]");
					}
				} else if (b instanceof RuleBlock) {

					((RuleBlock) b).create(myContext, blocks);

				} else if (b instanceof MenuHeaderBlock) {
					((MenuHeaderBlock) b).create(myContext);
					myContext.setHasMenu();
				} else if (b instanceof MenuEntryBlock) {
					((MenuEntryBlock) b).create(myContext);
					myContext.setHasMenu();
				} else if (b instanceof RoundChartBlock) {
					((RoundChartBlock) b).create(myContext);

				} else if (b instanceof BarChartBlock) {
					((BarChartBlock) b).create(myContext);

				} else if (b instanceof CreateCategoryDataSourceBlock) {
					((CreateCategoryDataSourceBlock) b).create(myContext);

				} else if (b instanceof CreateGisBlock) {
					CreateGisBlock bl = ((CreateGisBlock) b);
					bl.reset();
					List<String> picNames = bl.getPicNames();
					// It's safer to check if the list is empty
					if (picNames == null || picNames.isEmpty()) {
						blockP++; // Skip this block if no pictures are defined
						continue;
					}

					String masterPicName = picNames.get(0);
					PersistenceHelper globalPh = gs.getGlobalPreferences();
					final String serverFileRootDir = globalPh.get(PersistenceHelper.SERVER_URL) + globalPh.get(PersistenceHelper.BUNDLE_NAME).toLowerCase() + "/extras/";
					final String cacheFolder = gs.getContext().getFilesDir() + "/" + globalPh.get(PersistenceHelper.BUNDLE_NAME).toLowerCase(Locale.ROOT) + "/cache/";
					Log.d("GisTrace", "Executor: Starting download for masterPicName: " + masterPicName);
					// Pass the current block pointer 'blockP' to the ViewModel
					gisViewModel.startGisDownload(picNames, serverFileRootDir, cacheFolder, masterPicName, blockP);

					// Set savedBlockPointer and return to pause the executor.
					// This is still needed to prevent the loop from continuing immediately.
					savedBlockPointer = blockP + 1;
					return;
				}else if (b instanceof AddGisLayerBlock) {
					((AddGisLayerBlock) b).create(myContext);

				} else if (b instanceof AddGisPointObjects) {
					((AddGisPointObjects) b).create(myContext);
				} else if (b instanceof AddGisFilter) {
					((AddGisFilter) b).create(myContext);
				} else if (b instanceof BlockDeleteMatchingVariables) {
					((BlockDeleteMatchingVariables) b).create(myContext);
				} else if (b instanceof AddFilter) {
					AddFilter bl = ((AddFilter) b);
					if (myListBlocks.get(bl.getTarget()) != null) {
						Log.d("baza", "Added filter to " + bl.getTarget());
						myListBlocks.get(bl.getTarget()).associateFilterBlock(bl);
					} else
						Log.d("baza", "failed to add filter");
				}

				String cId = b.getBlockId();
				String jNext = jump.get(cId);
				if (jNext != null) {
					if (jNext.equals(Executor.STOP_ID))
						notDone = false;
					else
						blockP = indexOf(jNext, blocks);
				} else
					blockP++;

				if (!notDone)
					Log.d("vortex", "EXECUTION STOPPED ON BLOCK " + b.getBlockId());
			}


			if (!myListBlocks.keySet().isEmpty()) {
				addLoadDialog();
				Log.d("baza", "I have lists...creating");
				Iterator<String> it = myListBlocks.keySet().iterator();
				while(it.hasNext()) {
					String lBlock = it.next();
					if (it.hasNext()) {
						myListBlocks.get(lBlock).generate(myContext,null);
					} else
						myListBlocks.get(lBlock).generate(myContext,this);
				}
			}
			else
				drawAll();
		} catch (Exception e) {
			removeLoadDialog();
			if (blocks != null) {
				Block errorBlock = blocks.get(blockP);
				if (errorBlock != null)
					Tools.printErrorToLog(o, e, "id: " + errorBlock.getBlockId());
			} else
				Tools.printErrorToLog(o, e, "index: " + Integer.toString(blockP));
		}
	}

	private void drawAll() {

		//Remove loading popup if displayed.
		removeLoadDialog();
		Container root = myContext.getContainer("root");
		if (root==null && myContext.hasContainers()) {
			
			o.addCriticalText("TEMPLATE ERROR: Cannot find the root container. \nEach template must have a root! Execution aborted.");
		} else {
			//Now all blocks are executed.
			//Draw the UI.
			
			o.addYellowText("Now Drawing components recursively");
			Log.d("vortex","Now Drawing components recursively");
			//Draw all lists first.
			for (WF_Static_List l:myContext.getLists())
				l.draw();

			if (root!=null)
				myContext.drawRecursively(root);
			//open menu if any
			if (delayedOnSave!=null) {

				Log.d("blubb","executing delayed onSave");
				myContext.registerEvent(delayedOnSave);
				delayedOnSave=null;
			}
			if (myContext.hasMenu()) {
				Log.d("vortex","Drawing menu");
				gs.getDrawerMenu().openDrawer();
			}

			//Send event that flow has executed.
			Log.d("vortex","Registering WF EXECUTION");
			myContext.registerEvent(new WF_Event_OnFlowExecuted("executor"));
			if (root==null) {
				int c = getActivity().getFragmentManager().getBackStackEntryCount();
				Log.d("blax","need to redraw previous fragment if there is one! "+c);
				if (c>0) {
					Log.d("blax","there is a fragment to redraw. Try broadcast!");
					Intent intent = new Intent();
					intent.setAction(Executor.REDRAW_PAGE);
					intent.putExtra("RedrawAfterExecutingSub",true);
					gs.sendSyncEvent(intent);
				}
			}
		}


	}

	private void addLoadDialog() {
		pDialog = ProgressDialog.show(myContext.getContext(), "",
				getResources().getString(R.string.loading_please_wait), true);
	}
	private void removeLoadDialog() {
		Log.d("wiz","REMOVELOAD");
		if (pDialog!=null) {
			pDialog.dismiss();
			pDialog=null;
		} else
			Log.d("wiz","NULL");
	}
	private int indexOf(String jNext, List<Block> blocks) {

		for(int i=0;i<blocks.size();i++) {
			String id = blocks.get(i).getBlockId();
			//			Log.d("nils","checking id: "+id);
			if(id.equals(jNext)) {
				Log.d("vortex","Jumping to block "+jNext);
				o.addText("Jumping to block "+jNext);
				return i;
			}
		}

		Log.e("nils","Jump pointer to non-existing block. Faulty ID: "+jNext);
		
		o.addCriticalText("Jump pointer to non-existing block. Faulty ID: "+jNext);
		return blocks.size();
	}

	public Set<Variable> getVariables() {
		return visiVars;
	}


	@Override
	public void continueExecution(String fromWhere) {
		if ("draw".equals(fromWhere))
			this.getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					drawAll();
				}
			});


		else if (savedBlockPointer!=-1) {
			this.execute(savedBlockPointer);
		}
		else
			Log.e("vortex","No saved block pointer...aborting execution");
	}

	@Override
	public void abortExecution(String reason) {
		Log.e("vortex","Execution aborted.");
		removeLoadDialog();
		new AlertDialog.Builder(myContext.getContext())
				.setTitle("Execution aborted")
				.setMessage(reason)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setCancelable(false)
				.setNeutralButton("Ok",new Dialog.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub

					}
				} )
				.show();
	}

	public void restart() {
		if (myContext.myEndIsNear()) {
			Log.d("vortex","skipping restart in restart!");
			return;
		}
		myContext.setMyEndIsNear();
		new Handler().postDelayed(new Runnable() {
			public void run() {
				//myContext.resetState();
				Executor.this.run();
				Log.d("vortex","workflow was restarted");
			}
		}, 0);
	}

	//Refresh all the gislayers.
	public void refreshGisObjects() {
		for (Block b: wf.getBlocks()) {
			AddGisPointObjects bl;
			if (b instanceof AddGisPointObjects) {
				bl = ((AddGisPointObjects) b);
				bl.create(myContext, true);
			}
		}
		for (GisLayer layer :myContext.getCurrentGis().getLayers()) {
			//Log.d("grogg","In refreshgisobjects");
			layer.filterLayer(myContext.getCurrentGis().getGis());
		}


	}

	public void onLocationChanged(Location location) {
		Log.d("fenris","Recieved location update at "+Tools.getCurrentTime());
		if (location!=null && myX!=null) {
			SweLocation myL = Geomatte.convertToSweRef(location.getLatitude(),location.getLongitude());

			String oldX = myX.getValue();
			String oldY = myY.getValue();


			if (oldX!=null&&oldY!=null&&oldT!=null) {
				long currT = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
				long timeDiff = currT - oldT;
				//Log.d("GPS","cuurT oldT time diff: "+currT+" "+oldT+" "+timeDiff);
				double oldXd = Double.parseDouble(oldX);
				double oldYd = Double.parseDouble(oldY);
				double distx = Math.abs(oldXd - myL.getX());
				double disty = Math.abs(oldYd - myL.getY());

				//Log.d("GPS", "Distance between mesaurements in Tracker: (x,y) " + distx + "," + disty);
				String accuracy = Float.toString(location.getAccuracy());
				String x = Double.toString(myL.getX());
				String y = Double.toString(myL.getY());


				if (distx > 15 || disty > 15 || timeDiff > 60) {
					//	Log.d("vortex","setting synced location");
					myX.setValue(x);
					myY.setValue(y);
					if (myAcc!=null)
						myAcc.setValue(accuracy);
					oldT = myX.getTimeOfInsert();
				} else {
					myX.setValueNoSync(x);
					myY.setValueNoSync(y);
					if (myAcc!=null)
						myAcc.setValueNoSync(accuracy);


				}
			} else {
				myX.setValue(myL.getX() + "");
				myY.setValue(myL.getY() + "");
				if (myAcc!=null)
					myAcc.setValue(location.getAccuracy() + "");
				oldT = myX.getTimeOfInsert();
			}

			TrackerListener.GPS_State signal = GPS_State_C(TrackerListener.GPS_State.State.newValueReceived);
			signal.accuracy=location.getAccuracy();
			signal.x=myL.getX();
			signal.y=myL.getY();
			gs.updateCurrentPosition(signal,this.hashCode());

		}
	}




}