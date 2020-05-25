package com.teraim.fieldapp.dynamic.templates;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.blocks.DisplayFieldBlock;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_DisplayValueField;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.non_generics.StatusHandler;
import com.teraim.fieldapp.non_generics.StatusHandler.Kvot;
import com.teraim.fieldapp.ui.MenuActivity;
import com.teraim.fieldapp.utils.Tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ProvytaTemplate extends Executor implements EventListener,OnGesturePerformedListener {
	private List<WF_Container> myLayouts;

    private GestureLibrary gestureLib;
	private final String[] linjeA=new String[Constants.MAX_NILS_LINJER+1];
	private Variable liv,pyv;
	private String provytaThatWasSelected=null;
	private static final String NONE_SELECTED = "-";
	private boolean clicked = false;


	private TextView provOutputValueField,linjeOutputValueField;



	private final ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {


		// Called when the action mode is created; startActionMode() was called
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Inflate a menu resource providing context menu items
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.provytadeletemenu, menu);

			return true;
		}

		// Called each time the action mode is shown. Always called after onCreateActionMode, but
		// may be called multiple times if the mode is invalidated.
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

			return false; // Return false if nothing is done
		}

		// Called when the user selects a contextual menu item
		@Override
		public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
			Map<String, String> keyChain;
			switch (item.getItemId()) {			        
			case R.id.menu_delete:
				keyChain = Tools.createKeyMap("ruta",gs.getVariableConfiguration().getCurrentRuta(),"provyta",provytaThatWasSelected);
				gs.getVariableCache().invalidateOnKey(keyChain,false);
				gs.getDb().eraseProvyta(al.getCurrentRuta(), provytaThatWasSelected);
				//Ask the user to synchronize at this point.
				gs.sendEvent(MenuActivity.SYNC_REQUIRED);
				mode.finish(); // Action picked, so close the CAB
				return true;
			default:
				return false;
			}
		}

		// Called when the user exits the action mode
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
		}
	};

	private ActionMode mActionMode;



	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (myContext == null) {
			Log.d("vortex","hasnt survived create...exiting.");
			return null;
		}
		liv = varCache.getVariable(NamedVariables.CURRENT_LINJE);
		pyv = varCache.getVariable(NamedVariables.CURRENT_PROVYTA);
		
		//myContext.resetState();
		myLayouts = new ArrayList<WF_Container>();
		Log.d("nils","in onCreateView of provyta_template");
        /* (non-Javadoc)
         * @see android.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
         */
        View v = inflater.inflate(R.layout.template_provyta_wf, container, false);
		WF_Container root = new WF_Container("root", v.findViewById(R.id.root), null);
		ListView selectedList = v.findViewById(R.id.SelectedL);
		ViewGroup aggregatePanel = (LinearLayout)v.findViewById(R.id.aggregates);
		ViewGroup fieldListPanel = (LinearLayout)v.findViewById(R.id.fieldList);

		final Spinner pySpinner = fieldListPanel.findViewById(R.id.pySpinner);
		final Spinner aboSpinner = fieldListPanel.findViewById(R.id.aboSpinner);
		final Spinner linjeSpinner = fieldListPanel.findViewById(R.id.linjeSpinner);
		myLayouts.add(root);
		myLayouts.add(new WF_Container("Field_List_panel_1", fieldListPanel , root));
		myLayouts.add(new WF_Container("Aggregation_panel_3", aggregatePanel, root));
		//		myLayouts.add(new WF_Container("Filter_panel_4", (LinearLayout)v.findViewById(R.id.filterPanel), root));
		myLayouts.add(new WF_Container("Field_List_panel_2", v.findViewById(R.id.Selected), root));
		myContext.addContainers(getContainers());


		WF_DisplayValueField rSel = new WF_DisplayValueField("whatevar", "[$Current_Ruta]",myContext, null, 
				"Vald Ruta", true,null,new DisplayFieldBlock());

		LinearLayout provytorRemainingView = (LinearLayout)inflater.inflate(R.layout.display_value_textview_horizontal, null);
		TextView h = provytorRemainingView.findViewById(R.id.header);
		h.setText("Provytor gjorda");

		provOutputValueField = provytorRemainingView.findViewById(R.id.outputValueField);

		LinearLayout linjerRemainingView = (LinearLayout)inflater.inflate(R.layout.display_value_textview_horizontal, null);
		h = linjerRemainingView.findViewById(R.id.header);
		h.setText("Linjer gjorda");		


		linjeOutputValueField = linjerRemainingView.findViewById(R.id.outputValueField);

		//WF_ClickableField_Selection aggNo = new WF_ClickableField_Selection_OnSave("Avslutade Rutor:", "De rutor ni avslutat",
		//		myContext, "AvslRutor",true);
		Button navi = fieldListPanel.findViewById(R.id.naviButton);
		//Button gron = (Button)fieldListPanel.findViewById(R.id.gronB);
		navi.setOnClickListener(new OnClickListener() {
			//TODO: CHANGE TO CORRECT GPS
			@Override
			public void onClick(View v) {
				//Get Lat Long.
				if (pyv.getValue()!=null) {
					Map<String, String> pk = al.createProvytaKeyMap();
					String lat = varCache.getVariable(pk, "CentrumGPSLat").getHistoricalValue();
					String lon = varCache.getVariable(pk, "CentrumGPSLong").getHistoricalValue();
					if (lat!=null && lon != null) {
						lat = lat.replace(",",".");
						lon = lon.replace(",",".");
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q="+lat+","+lon));
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
					} else {
						Log.e("nils","Lat or Long null for py "+pyv.getValue()+" LAT: "+lat+" LONG: "+lon);
						Toast.makeText(ProvytaTemplate.this.getActivity(), "Latitude eller Longitudvärde för provytacentrum är inte satt i historiskt data så navigering är inte möjlig.", Toast.LENGTH_LONG).show();
                    }
				}
			}
		});

		Button greenB = fieldListPanel.findViewById(R.id.greenB);
		greenB.setOnClickListener(new OnClickListener() {
			//TODO: CHANGE TO CORRECT GPS
			@Override
			public void onClick(View v) {

				Intent browse = new Intent( Intent.ACTION_VIEW , Uri.parse(Constants.SLU_URL));
				browse.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				gs.getContext().startActivity(browse);	  
			}
		});

		Button linjeInvB = fieldListPanel.findViewById(R.id.linjeButton);
		linjeInvB.setOnClickListener(new OnClickListener() {
			//TODO: CHANGE TO CORRECT GPS
			@Override
			public void onClick(View v) {
				if (Start.singleton!=null && liv.getValue()!=null && !liv.equals(NONE_SELECTED)) {
					gs.setDBContext(new DB_Context(null,al.createLinjeKeyMap()));
					Start.singleton.changePage(new LinjePortalTemplate(), "LinjePortal");
				}
				else {
					AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
					alert.setTitle("Information saknas!");
					alert.setMessage("Välj en linje först!");					
					alert.setCancelable(false);
					alert.setPositiveButton("okej!", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

						}
					});			
					alert.show();
				}
			}
		});

		Button nilsInvB  = fieldListPanel.findViewById(R.id.pyButton);
		nilsInvB.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (Start.singleton!=null &&  pyv.getValue()!=null && !clicked) {
					String pi=pySpinner.getSelectedItem().toString();
					if (pi!=null && !pi.equals(NONE_SELECTED)) {
						clicked = true;
						//refresh keyhash and status
						gs.setDBContext(new DB_Context(null,al.createProvytaKeyMap()));
						Start.singleton.changePage(new ProvytaNivaTemplate(), "ProvytaNivå");
						clicked = false;
					}
				}
			}
		});

		Button aboInvB  = fieldListPanel.findViewById(R.id.aboButton);
		aboInvB.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				if (Start.singleton!=null &&  pyv.getValue()!=null && !clicked) {
					String pi=aboSpinner.getSelectedItem().toString();
					if (pi!=null && !pi.equals(NONE_SELECTED)) {
						clicked = true;
						gs.setDBContext(new DB_Context(null,al.createProvytaKeyMap()));
						gs.sendEvent(MenuActivity.REDRAW);
						Start.singleton.changePage(new ProvytaNivaTemplate(), "ProvytaNivå");
						clicked = false;
					}
				}
			}
		});

		aggregatePanel.addView(rSel.getWidget());
		aggregatePanel.addView(provytorRemainingView);
		aggregatePanel.addView(linjerRemainingView);


		GestureOverlayView gestureOverlayView = v.findViewById(R.id.gesture_overlay);

		gestureOverlayView.setGestureVisible(false);
		gestureOverlayView.addOnGesturePerformedListener(this);
		gestureLib = GestureLibraries.fromRawResource(this.getActivity(), R.raw.gestures);
		if (!gestureLib.load()) {      	
			Log.i("nils", "Load gesture libraries failed.");  
		}  


		final List<Integer> prevProvytor = new ArrayList<Integer>();
		List<String> nilsProvytor = new ArrayList<String>();
		nilsProvytor.add(NONE_SELECTED);
		List<String> aboProvytor = new ArrayList<String>();
		aboProvytor.add(NONE_SELECTED);
		String[] opt = Tools.generateList(pyv);	 
		if (opt!=null) {
			for (String s : opt) {
				try {
					int id = Integer.parseInt(s);
					if (id <= Constants.MAX_NILS)
						nilsProvytor.add(s);
					else if (id >= Constants.MIN_ABO && id <= Constants.MAX_ABO)
						aboProvytor.add(s);
					else {
						o.addRow("");
						o.addRedText("Hittade ProvyteID som inte tillhör Nils eller Äbo: " + s);
					}
				} catch (NumberFormatException e) {
					o.addRow("");
					o.addRedText("Provyta ID not a number: " + s);
				}


			}
		}

		ArrayAdapter<String> nilsSpinnerA=new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_1,nilsProvytor);
		nilsSpinnerA.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		pySpinner.setAdapter(nilsSpinnerA);

		ArrayAdapter<String> aboSpinnerA=new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_1,aboProvytor);
		aboSpinnerA.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		aboSpinner.setAdapter(aboSpinnerA);



		linjeA[0]=NONE_SELECTED;
		for (int i=1;i<=Constants.MAX_NILS_LINJER;i++)
			linjeA[i]=i+"";


		final ArrayAdapter<String> adp2=new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_1,linjeA);
		adp2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		linjeSpinner.setAdapter(adp2);
		if (pyv.getValue()!=null)
			pySpinner.setSelection(nilsSpinnerA.getPosition(pyv.getValue()));


		if (liv.getValue()!=null)
			linjeSpinner.setSelection(adp2.getPosition(liv.getValue()));
		final ArrayAdapter<Integer> selectedListA = new ArrayAdapter<Integer>(this.getActivity(), android.R.layout.simple_list_item_1, prevProvytor);
		selectedList.setAdapter(selectedListA);	

		selectedList.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View row,
					int arg2, long arg3) {
				if (mActionMode != null) {
					return false;
				}
				TextView txt1 = row.findViewById(android.R.id.text1);
				Log.d("nils","I believe the user longclicked "+txt1.getText());
				provytaThatWasSelected = txt1.getText().toString();
				// Start the CAB using the ActionMode.Callback defined above
				mActionMode = ((Activity)myContext.getContext()).startActionMode(mActionModeCallback);

				return true;
			}
		});


		pySpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				String pi=pySpinner.getSelectedItem().toString();
				if (pi.equals(NONE_SELECTED) || pi.equals(pyv.getValue()))
					Log.d("nils","Samma provyta vald - ingen ändring");
				else {               		
					pyv.setValue(pi);
					prevProvytor.add(0, Integer.parseInt(pi));
					selectedListA.notifyDataSetChanged();
					aboSpinner.setSelection(0);
					gs.sendEvent(MenuActivity.REDRAW);                		
				}	

			}


			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}
		});

		aboSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				String pi=aboSpinner.getSelectedItem().toString();
				if (pi.equals(NONE_SELECTED) || pi.equals(pyv.getValue()))
					Log.d("nils","Samma provyta vald - ingen ändring");
				else {               		
					pyv.setValue(pi);
					prevProvytor.add(0, Integer.parseInt(pi));
					selectedListA.notifyDataSetChanged();
					pySpinner.setSelection(0);
					gs.sendEvent(MenuActivity.REDRAW);

				}	

			}


			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}
		});

		linjeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				String pi=linjeSpinner.getSelectedItem().toString();
				if (pi.equals(NONE_SELECTED) ||pi.equals(liv.getValue())) {
					Log.d("nils","Samma linje vald - ingen ändring");
					if (liv.getValue()==null && !pi.equals(NONE_SELECTED)) {
						liv.setValue(pi);
						Log.d("vortex","Setting ANYWAYS!!!");
					} else {
						Log.d("vortex","Caesars underpantzzzz on fire. "+liv.getValue());
						
					}
				}
				else {
					liv.setValue(pi);
					gs.sendEvent(MenuActivity.REDRAW); 
				}


			}


			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}
		});
		/*
	    gron.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
			 	vg = (EditText)inflater.inflate(R.layout.gron_lapp, null);
			    AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
				alert.setTitle("Grön Lapp");
				alert.setMessage("Berätta om den här provytan!");
				alert.setPositiveButton("Spara", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {				  

					}
				});
				alert.setNegativeButton("Avbryt", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

					}
				});	

				Dialog d = alert.setView(vg).create();
				d.setCancelable(false);
				d.show();
			}
		});
		 */

		myContext.registerEventListener(this, EventType.onSave);


		return v;

	}







	/* (non-Javadoc)
	 * @see android.app.Fragment#onStart()
	 */
	@Override
	public void onResume() {
		myContext.registerEvent(new WF_Event_OnSave("Template"));
		super.onResume();

	}

	private void refreshStatus() {
		StatusHandler sh = gs.getStatusHandler();
		Kvot k = sh.getStatusProvytor();
		provOutputValueField.setText(k.toString());
		k = sh.getStatusLinjer();
		linjeOutputValueField.setText(k.toString());
	}




	@Override
	public void onPause() {

		super.onPause();
	}






	@Override
	protected List<WF_Container> getContainers() {
		return myLayouts;
	}



	@Override
	public void onEvent(Event e) {
		refreshStatus();
	}

	@Override
	public String getName() {
		return "PROVYTA ";
	}


	@Override
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
		Log.d("nils","Number of gestures available: "+gestureLib.getGestureEntries().size());
		ArrayList<Prediction> predictions = gestureLib.recognize(gesture);
		Log.d("nils","Number of predictions: "+predictions.size());
		for (Prediction prediction : predictions) {
			if (prediction.score > .5) {
				Log.d("nils","MATCH!!");
				if (prediction.name.equals("left")) {
					/*final FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction(); 
	  			Fragment gs = new Fragment();  			
	  			ft.replace(R.id.content_frame, gs);
	  			ft.addToBackStack(null);
	  			ft.commit(); 
					 */
					Toast.makeText(getActivity(), "Inget innehåll", Toast.LENGTH_SHORT).show();
				} 

			}
		}		
	}



	@Override
	public boolean execute(String function, String target) {
		return true;
	}











}