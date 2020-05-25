package com.teraim.fieldapp.dynamic.templates;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.non_generics.StatusHandler;
import com.teraim.fieldapp.non_generics.StatusHandler.Kvot;
import com.teraim.fieldapp.ui.ExportDialog;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.DbHelper.Selection;
import com.teraim.fieldapp.utils.Exporter;
import com.teraim.fieldapp.utils.Exporter.ExportReport;
import com.teraim.fieldapp.utils.Exporter.Report;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;



public class SimpleRutaTemplate extends Executor implements OnGesturePerformedListener {
	private List<WF_Container> myLayouts;


    private GestureLibrary gestureLib;
	private List<Integer> rutor;

    private Variable rutaKlar;

	private TextView rutOutputValueField;
	private final static int MIN_UNSYNCED = 5;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (myContext==null) {
			Log.d("vortex","hasnt survived create...exiting.");
			return null;
		}
		//create keyhash for current ruta. 
		al = gs.getVariableConfiguration();
		gs.setDBContext(new DB_Context(null,al.createRutaKeyMap()));
		//myContext.resetState();
		myLayouts = new ArrayList<WF_Container>();
		Log.d("nils","in onCreateView of ruta_template");
        /* (non-Javadoc)
         * @see android.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
         */
        View v = inflater.inflate(R.layout.template_ruta_wf, container, false);
		WF_Container root = new WF_Container("root", v.findViewById(R.id.root), null);
		ListView fieldList = v.findViewById(R.id.fieldListL);
		ListView selectedList = v.findViewById(R.id.SelectedL);
		LinearLayout aggregatePanel = v.findViewById(R.id.aggregates);
		myLayouts.add(root);
		myLayouts.add(new WF_Container("Field_List_panel_1", v.findViewById(R.id.fieldList), root));
		myLayouts.add(new WF_Container("Aggregation_panel_3", aggregatePanel, root));
		myLayouts.add(new WF_Container("Filter_panel_4", v.findViewById(R.id.filterPanel), root));
		myLayouts.add(new WF_Container("Field_List_panel_2", v.findViewById(R.id.Selected), root));
		myContext.addContainers(getContainers());


		LinearLayout rutorRemainingView = (LinearLayout)inflater.inflate(R.layout.display_value_textview_horizontal, null);
		TextView h = rutorRemainingView.findViewById(R.id.header);
		h.setText("Rutor gjorda");

		rutOutputValueField = rutorRemainingView.findViewById(R.id.outputValueField);


		aggregatePanel.addView(rutorRemainingView);


		Button exportB = new Button(getActivity());		
		exportB.setText("Exportera vald ruta");

		exportB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				final String currentRuta = gs.getVariableCache().getVariable(NamedVariables.CURRENT_RUTA).getValue();
				final String lagID = gs.getGlobalPreferences().get(PersistenceHelper.LAG_ID_KEY);
				if (currentRuta == null) {
					new AlertDialog.Builder(getActivity())
					.setTitle("Ingen ruta vald")
					.setMessage("Det verkar inte som om du ännu valt någon ruta.")
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) { 
							// continue with delete
						}
					})			    
					.setIcon(android.R.drawable.ic_dialog_alert)
					.show();
				} else if (lagID==null || lagID.equals(PersistenceHelper.UNDEFINED) || lagID.length()==0) {
					new AlertDialog.Builder(getActivity())
					.setTitle("Inget LAG ID!")
					.setMessage("Du måste först ange dit lag/team för att exportera.")
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) { 
							// continue with delete
						}
					})			    
					.setIcon(android.R.drawable.ic_dialog_alert)
					.show();
				}
				else{
					int noOfSyncE = gs.getDb().getNumberOfUnsyncedEntries();
					new AlertDialog.Builder(getActivity())
					.setTitle("Export av Ruta "+currentRuta)
					.setMessage((noOfSyncE<MIN_UNSYNCED)?"Du ska till och exportera ruta "+currentRuta+".\n"+					
							"Vill du klarmarkera? ":"VARNING!! DU HAR "+noOfSyncE+" OSYNKADE INMATNINGAR!! Vill du verkligen klarmarkera?")
							.setPositiveButton("Ja - den är klar!", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) { 							
									rutaKlar = varCache.getVariable(al.createRutaKeyMap(), NamedVariables.RUTA_KLAR_AV_ANVANDARE);
									rutaKlar.setValue("1");
									export(true,currentRuta);
									varCache.getVariable(NamedVariables.CURRENT_RUTA).setValue(null);
								}			
							})
							.setNeutralButton("Nej - men exportera ändå", new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									export(false,currentRuta);
								}
							})
							.setNegativeButton("Avbryt! Ingen export", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) { 
									//do nothing.
								}
							}
									)		
									.setCancelable(false)
									.setIcon(android.R.drawable.ic_dialog_alert)
									.show();


				}


			}
		});

		aggregatePanel.addView(exportB);
		//CreateEntryFieldBlock x = new CreateEntryFieldBlock("typSpinner","RutaSorteringsTyp", "Aggregation_panel_3",true,"DDD",false,Constants.NO_DEFAULT_VALUE,null,true);
		//x.create(myContext);

		GestureOverlayView gestureOverlayView = v.findViewById(R.id.gesture_overlay);

		gestureOverlayView.setGestureVisible(false);
		gestureOverlayView.addOnGesturePerformedListener(this);
		gestureLib = GestureLibraries.fromRawResource(this.getActivity(), R.raw.gestures);
		if (!gestureLib.load()) {      	
			Log.i("nils", "Load gesture libraries failed.");  
		}  


		final List<Integer> prevRutor = new ArrayList<Integer>();

		DbHelper db = gs.getDb();
		if (rutor == null) {
			rutor = new ArrayList<Integer>();
			HashSet<Integer> temp = new HashSet<Integer>();
			List<String[]> values = db.getUniqueValues(new String[] {db.getDatabaseColumnName("ruta")}, new Selection());
			for (String[] val:values)
				temp.add(Integer.parseInt(val[0]));
			rutor.addAll(temp);
			Collections.sort(rutor);		
		}


        ArrayAdapter<Integer> fieldListAdapter = new ArrayAdapter<Integer>(this.getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, rutor);
		final ArrayAdapter selectedListA = new ArrayAdapter<>(this.getActivity(),android.R.layout.simple_list_item_1, android.R.id.text1, prevRutor);
		fieldList.setAdapter(fieldListAdapter);
		selectedList.setAdapter(selectedListA);
		fieldList.setOnItemClickListener((arg0, view, position, arg3) -> {


			final Variable currentRuta = varCache.getGlobalVariable(NamedVariables.CURRENT_RUTA);
			final Integer rl = rutor.get(position);
			final String pi = rl.toString();
			final Map<String,String>rKeyChain = Tools.createKeyMap("år",Constants.getYear(),"ruta",pi);
			rutaKlar = varCache.getVariable(rKeyChain, NamedVariables.RUTA_KLAR_AV_ANVANDARE);
			String rutaKS = rutaKlar.getValue();
			final boolean rutaK = rutaKS!=null&&rutaKS.equals("1");
			String msg = null;
			if (rutaK)
				msg = "Vill du verkligen öppna ruta "+pi+"? Den är markerad klar!!!";
			else
				msg = "Vill du "+(currentRuta.getValue()!=null&&currentRuta.getValue().equals(pi)?"fortsätta":"börja")+" insamling på ruta "+pi+"?";
			new AlertDialog.Builder(SimpleRutaTemplate.this.getActivity())
			.setTitle("Starta insamling")
			.setMessage(msg)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setCancelable(false)
			.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which)  {
					if (rutaK)
						rutaKlar.setValue("0");
					currentRuta.setValue(pi);
					//Nullify currentprovyta and currentlinje
					varCache.getGlobalVariable(NamedVariables.CURRENT_PROVYTA).deleteValue();
					varCache.getGlobalVariable(NamedVariables.CURRENT_LINJE).deleteValue();
					varCache.getGlobalVariable(NamedVariables.CURRENT_DELYTA).deleteValue();
					varCache.getGlobalVariable(NamedVariables.CURRENT_SMAPROVYTA).deleteValue();
					//kill the current variable cache.
					//						gs.getVariableCache().invalidateAll();
					Variable stratum = varCache.getVariable(rKeyChain,NamedVariables.STRATUM);
					Variable hStratum = varCache.getVariable(rKeyChain,NamedVariables.STRATUM_HISTORICAL);
					String strH = stratum.getHistoricalValue();
					if (strH==null) {
						o.addRow("");
						o.addRedText("Stratum missing for ruta "+pi+" Will default to 1");
						strH="1";
					}
					if (hStratum!=null) {
						hStratum.setValue(strH);
						Log.d("nils","HISTORICAL STRATUM SET TO "+hStratum.getValue());
					} else
						Log.e("vortex","HISTO_STRATUM NULL!!");
					//copy
					prevRutor.add(0,rl);
					selectedListA.notifyDataSetChanged();
					Log.d("vortex","in simpleruta, after refreshkey, before menu redraw. PI: "+pi+" CurrentR_: "+currentRuta.getValue()+"\nkeyhash: "+gs.getVariableCache().getContext().toString());

					Log.d("vortex","in simpleruta, after refreshkey, after menu redraw!!");
					Start.singleton.changePage(new ProvytaTemplate(), "Provyta");
				}

			})
			.setNegativeButton(android.R.string.no,new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {}} )
				.show();



		});

		myContext.getContainer("Aggregation_panel_3").draw();

		return v;

	}




	@Override
	public void onResume() {


		StatusHandler sh = gs.getStatusHandler();
		sh.setStatusRuta();
		Kvot k = sh.getStatusRutor();
		rutOutputValueField.setText(k.toString());

		super.onResume();
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
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
		Log.d("nils","Number of gestures available: "+gestureLib.getGestureEntries().size());
		ArrayList<Prediction> predictions = gestureLib.recognize(gesture);
		Log.d("nils","Number of predictions: "+predictions.size());
		for (Prediction prediction : predictions) {
			if (prediction.score > .5) {
				if (prediction.name.equals("left")) {
					final FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction(); 
					Fragment gs = new Fragment();  			
					ft.replace(R.id.content_frame, gs);
					ft.addToBackStack(null);
					ft.commit(); 
				} 
			}
		}		
	}


	@Override
	public boolean execute(String function, String target) {
		return true;
	}

	//Export current Ruta
	private void export(boolean isKlar, String currentRuta) {
		String rutaSorteringsTyp = varCache.getVariableValue(al.createRutaKeyMap(), "RutaSorteringsTyp");

		//Build up filename
		@SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HH_mm");
		Date date = new Date();
		String dS = (dateFormat.format(date));
		String typPrefix = rutaSorteringsTyp==null||rutaSorteringsTyp.equals("Normal")?"N_":
			(rutaSorteringsTyp.equals("Kontroll")?"K_":(rutaSorteringsTyp.equals("Flaggskepp")?"F_":"N_"));
		String lagID = gs.getGlobalPreferences().get(PersistenceHelper.LAG_ID_KEY);

		String exportFileName = typPrefix+lagID+"_ruta_"+currentRuta+"_"+dS+(isKlar?"_KLAR":"");
		Report jRep = gs.getDb().export(al.createRutaKeyMap(),Exporter.getInstance(this.getActivity(),"JSON", new ExportDialog()),exportFileName);
		String msg, btnText;
		if (jRep.getReport() == ExportReport.OK) {
			msg = "Export OK.\nFilnamn: "+exportFileName+"\n"+jRep.noOfVars+" variabler exporterade";
			btnText = "Ok";
		} else {
			msg = "Exporten fungerade inte. Orsak: "+jRep.getReport().name();
			btnText = "Ok";
		}
		new AlertDialog.Builder(getActivity())
		.setTitle("Export klar")
		.setMessage(msg)
		.setPositiveButton(btnText, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) { 
				// continue with delete
			}
		})			    
		.setIcon(android.R.drawable.ic_dialog_alert)
		.show();
	}











}