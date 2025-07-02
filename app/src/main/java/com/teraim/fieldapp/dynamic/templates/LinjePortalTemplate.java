package com.teraim.fieldapp.dynamic.templates;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import androidx.core.app.ActivityCompat;
import android.text.Editable;
import android.text.InputFilter;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout.LayoutParams;
import android.widget.TextView;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.ColumnDescriptor;
import com.teraim.fieldapp.dynamic.types.SweLocation;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.VariableCache;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnBluetoothMessageReceived;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Linje_Meter_List;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_TimeOrder_Sorter;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.synchronization.LinjeDone;
import com.teraim.fieldapp.synchronization.LinjeStarted;
import com.teraim.fieldapp.ui.Linje;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.DbHelper.Selection;
import com.teraim.fieldapp.utils.Geomatte;
import com.teraim.fieldapp.utils.InputFilterMinMax;
import com.teraim.fieldapp.utils.Tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class LinjePortalTemplate extends Executor implements LocationListener, EventListener {
	private List<WF_Container> myLayouts;
	private VariableCache varCache;
	private DbHelper db;

	private SweLocation myL = null;
	private EditText meterEd;
	private EditText meterEnEd;
	private String currentLinje;
	private String currentYear;
    private LinearLayout numTmp;
    private LinearLayout fieldListB;
    private Linje linje;
	private RelativeLayout intervallL;
	private Button startB;
    private TextView gpsView;
    private Spinner avgrSp;
	private double[] startPunkt;

    private final static String LinjePortalId = "LinjePortalTemplate";

	//private SweLocation center = new SweLocation(6564201.573, 517925.98);

	private LocationManager lm;
	private Variable linjeStatus, linjeStartEast, linjeStartNorth;

	private final int d = 25;
	private final double[][] startDistFromCenter = {{0, -d}, {0, -d}, {0, -d}, {d, 0}, {d, 0}, {d, 0}, {0, d}, {0, d}, {0, d}, {-d, 0}, {-d, 0}, {-d, 0}};
	private SweLocation center;
	private String histNorr;
	private String histOst;

	private String[] avgrValueA;

	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		Log.d("nils", "in onCreateView of LinjePortalTemplate");
		if (myContext == null) {
			Log.d("vortex", "hasnt survived create...exiting.");
			return null;
		}
		//myContext.resetState();
		//Listen to LinjeStarted and LinjeDone events.
		myContext.registerEventListener(this, EventType.onBluetoothMessageReceived);
		View v = inflater.inflate(R.layout.template_linje_portal_wf, container, false);
        WF_Container root = new WF_Container("root", v.findViewById(R.id.root), null);
        LinearLayout aggregatePanel = v.findViewById(R.id.aggregates);
        LinearLayout fieldList = v.findViewById(R.id.fieldList);
		fieldListB = fieldList.findViewById(R.id.fieldListB);
		//ListView selectedList = (ListView)v.findViewById(R.id.SelectedL);
        LinearLayout selectedPanel = v.findViewById(R.id.selected);

		lm = (LocationManager) this.getActivity().getSystemService(Context.LOCATION_SERVICE);

        Button stopB = new Button(this.getActivity());
		startB = fieldList.findViewById(R.id.startB);

		varCache = gs.getVariableCache();
		al = gs.getVariableConfiguration();
		db = gs.getDb();
		currentYear = varCache.getVariableValue(null, "Current_Year");
		currentLinje = varCache.getVariableValue(null, "Current_Linje");

		if (currentLinje == null) {
			AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
			alert.setTitle("Ingen linje angiven!");
			alert.setMessage("Den här menyn går inte att köra utan att en linje valts under ProvytaMenyn.");
			alert.setPositiveButton("Jag förstår", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			});
			alert.setIcon(android.R.drawable.ic_dialog_alert);
			alert.show();
		} else {
			Log.d("nils", "Current Linje is " + currentLinje);
			startPunkt = startDistFromCenter[Integer.parseInt(currentLinje) - 1];
			northW = startPunkt[1] < 0;
			southW = startPunkt[1] > 0;
			if (!northW && !southW) {
				eastW = startPunkt[0] > 0;
				westW = startPunkt[0] < 0;
			}

			Log.e("nils", "eastW: " + eastW + " westW: " + westW + " southW:" + southW + " northW: " + northW);
			Map<String, String> pyKeyMap = al.createLinjeKeyMap();
            Map<String, String> linjeKey = Tools.createKeyMap(VariableConfiguration.KEY_YEAR, currentYear, "ruta", varCache.getVariableValue(null, "Current_Ruta"), "linje", currentLinje);
			linjeStatus = varCache.getVariable(linjeKey, NamedVariables.STATUS_LINJE);
			Variable pyCentrumNorr = varCache.getVariable(pyKeyMap, "CentrumGPSNS");
			Variable pyCentrumOst = varCache.getVariable(pyKeyMap, "CentrumGPSEW");
			histNorr = pyCentrumNorr.getHistoricalValue();
			histOst = pyCentrumOst.getHistoricalValue();
			Log.d("nils", "pyKEyMap: " + pyKeyMap.toString());
			Log.d("nils", "Historical norr ost: " + histNorr + " " + histOst);


            String stratum = varCache.getVariableValue(al.createRutaKeyMap(), NamedVariables.STRATUM_HISTORICAL);

			Log.d("nils", "STRATUM: " + stratum);
			//			status = Active.INITIAL;
			stopB.setText("KLAR");

			startB.setText("STARTA");
			fieldListB.setVisibility(View.INVISIBLE);
			linjeStartEast = varCache.getVariable(linjeKey, "!linjestartEast");
			linjeStartNorth = varCache.getVariable(linjeKey, "!linjestartNorth");

			if (linjeStatus.getValue() != null) {
				if (linjeStatus.getValue().equals(Constants.STATUS_STARTAD_MEN_INTE_KLAR)) {
					if (linjeStartEast.getValue() != null && linjeStartNorth != null) {
						double lStartE = Double.parseDouble(linjeStartEast.getValue());
						double lStartN = Double.parseDouble(linjeStartNorth.getValue());

						setStart(lStartE, lStartN);
						Log.d("nils", "Linjestatus was STATUS_STARTAD_MEN_INTE_KLAR");
					} else {
						Log.d("nils", "Status changed back to initial, because of missing values for startE,startN");
						linjeStatus.setValue(Constants.STATUS_INITIAL);
						center = null;
					}
				} else if (linjeStatus.getValue().equals(Constants.STATUS_AVSLUTAD_EXPORT_MISSLYCKAD)) {
					Log.d("nils", "Linjestatus is Avslutad");
				}
			} else {
				Log.e("nils", "Linjestatus was null");
				linjeStatus.setValue(Constants.STATUS_INITIAL);
				center = null;
			}

			/*
			//Set statpunkt to theoretical start.
			if (status == Active.INITIAL) {
				if (histNorr !=null && histOst !=null) {
					double teoriNorr = Double.parseDouble(histNorr)+startPunkt[1];
					double teoriOst = Double.parseDouble(histOst)+startPunkt[0];
					setStart(teoriOst,teoriNorr);		
				}

			}
			 */


			LinearLayout filterPanel = v.findViewById(R.id.filterPanel);
			myLayouts = new ArrayList<WF_Container>();
			myLayouts.add(root);
			myLayouts.add(new WF_Container("Field_List_panel_1", fieldList, root));
			myLayouts.add(new WF_Container("Aggregation_panel_3", aggregatePanel, root));
			myLayouts.add(new WF_Container("Filter_panel_4", filterPanel, root));
			myLayouts.add(new WF_Container("Field_List_panel_2", selectedPanel, root));
			myContext.addContainers(getContainers());

			gpsView = aggregatePanel.findViewById(R.id.gpsView);
			gpsView.setText("Söker...");

			intervallL = (RelativeLayout) inflater.inflate(R.layout.intervall_popup, null);
			numTmp = (LinearLayout) inflater.inflate(R.layout.edit_field_numeric, null);

			avgrSp = intervallL.findViewById(R.id.avgrTyp);

			//			List<String>avgrTyper = Arrays.asList(new String[] {"�kermark","Sl�ttervall","Vatten","Otillg�nglig v�tmark","Otillg�nglig brant","Rasrisk","Tomt/Bebyggelse","On�bar biotop�","Betr�dnadsf�rbud"});			

			List<String> avgrTyperRaw = al.getListElements(al.getCompleteVariableDefinition(NamedVariables.AVGRTYP));
			String[] tmp;
			String[] avgrTyper = new String[avgrTyperRaw.size()];
			avgrValueA = new String[avgrTyperRaw.size()];
			int c = 0;
			for (String s : avgrTyperRaw) {
				s = s.replace("{", "");
				s = s.replace("}", "");
				tmp = s.split("=");
				if (tmp == null || tmp.length != 2) {
					Log.e("nils", "found corrupt element: " + s);
					o.addText("");
					o.addCriticalText("Variabeln Avgränsning:AvgrTyp saknar värden.");
					avgrValueA[c] = "null";
					avgrTyper[c] = "****";
				} else {
					avgrValueA[c] = tmp[1];
					avgrTyper[c] = tmp[0];
				}
				c++;
			}
			ArrayAdapter<String> sara = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, avgrTyper);
			avgrSp.setAdapter(sara);

            FrameLayout linjeF = filterPanel.findViewById(R.id.linje);

			linje = new Linje(getActivity(), (eastW ? "E" : (westW ? "W" : (northW ? "N" : (southW ? "S" : "?")))));

			linjeF.addView(linje);

			List<String> startAlt = (histOst != null && histNorr != null) ? Arrays.asList("Sätt startpunkt här", "Använd beräknad startpunkt", "Starten måste kartinventeras", "Hela linjen i karta") :
					Collections.singletonList("Sätt startpunkt här");

			final int startIHar = startAlt.indexOf("Sätt startpunkt här");
			final int startIKart = startAlt.indexOf("Starten måste kartinventeras");
			final int helaIKart = startAlt.indexOf("Hela linjen i karta");
			final int anvandTeoriStart = startAlt.indexOf("Använd beräknad startpunkt");
			final LinearLayout startSpinnerL = (LinearLayout) inflater.inflate(R.layout.edit_field_spinner, null);
			final Spinner startSp = startSpinnerL.findViewById(R.id.spinner);
			ArrayAdapter<String> ara = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, startAlt);
			startSp.setAdapter(ara);


			stopB.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {

					AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
					alert.setTitle("Stopp!");
					alert.setMessage("Vill du säkert avsluta och klarmarkera?");
					alert.setCancelable(false);
					alert.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							setEnded();
							//gs.sendMessage(new LinjeDone(currentLinje));
							getFragmentManager().popBackStackImmediate();
						}


					});
					alert.setNegativeButton("Nej", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {

						}
					});
					alert.show();
				}
			});

			startB.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (startSpinnerL.getParent() != null)
						((ViewGroup) startSpinnerL.getParent()).removeView(startSpinnerL);
					if (!linjeStatus.getValue().equals(Constants.STATUS_STARTAD_MEN_INTE_KLAR)) {
						if (linjeStatus.getValue().equals(Constants.STATUS_AVSLUTAD_EXPORT_MISSLYCKAD)) {
							new AlertDialog.Builder(v.getContext()).setTitle("Linjen markerad avslutad!")
									.setMessage("Vill du göra om linjen?")
									.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int which) {
											linjeStatus.setValue(Constants.STATUS_INITIAL);
											startB.performClick();
											gpsView.setText("");
										}
									})
									.setNegativeButton("Nej", new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int which) {
											// continue with delete
										}
									})
									.setCancelable(false)
									.setIcon(android.R.drawable.ic_dialog_alert)
									.show();
						}
						if (linjeStatus.getValue().equals(Constants.STATUS_INITIAL)) {
							AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
							alert.setTitle("Start");
							alert.setMessage("Hur vill du starta linjen?");
							alert.setView(startSpinnerL);
							alert.setCancelable(false);

							alert.setPositiveButton("Kör igång", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									if (startSp.getSelectedItemPosition() == startIHar && myL == null) {
										new AlertDialog.Builder(LinjePortalTemplate.this.getActivity())
												.setTitle("Din position är okänd!")
												.setMessage("Eftersom GPSen ännu inte hittat din position, så kan du inte använda det här alternativet")
												.setPositiveButton("Jag förstår!", new DialogInterface.OnClickListener() {
													public void onClick(DialogInterface dialog, int which) {
														// continue with delete
													}
												})
												.setCancelable(false)
												.setIcon(android.R.drawable.ic_dialog_alert)
												.show();
									} else {

										//should intervall be opened?
										if (startSp.getSelectedItemPosition() == startIKart ||
												startSp.getSelectedItemPosition() == helaIKart) {
											int start = 0, end = -1;

											if (startSp.getSelectedItemPosition() == helaIKart)
												end = 200;
											openInterVallPopup(start, end);
											double teoriNorr = Double.parseDouble(histNorr) + startPunkt[1];
											double teoriOst = Double.parseDouble(histOst) + startPunkt[0];
											setStart(teoriOst, teoriNorr);
										} else if (startSp.getSelectedItemPosition() == startIHar) {

											setStart(myL.east, myL.north);

										} else if (startSp.getSelectedItemPosition() == anvandTeoriStart) {
											Log.d("nils", "I should en up here.");
											double teoriNorr = Double.parseDouble(histNorr) + startPunkt[1];
											double teoriOst = Double.parseDouble(histOst) + startPunkt[0];
											setStart(teoriOst, teoriNorr);
										} else
											Log.d("nils", "startSPinner: " + startSp.getSelectedItemPosition());
										/*
										if (gs.syncIsAllowed()) {
											gs.triggerTransfer();
											//Send a LinjeStarted Message in an envelope.
											gs.sendMessage(new EnvelopedMessage(new LinjeStarted(currentLinje)));
										}
										 */

									}
								}


							});
							alert.setNegativeButton("Avbryt", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {

								}
							});
							alert.show();
						}
					}

				}
			});

			Log.d("nils", "year: " + currentYear + " Ruta: " + varCache.getVariableValue(null, "Current_Ruta") + " Linje: " + currentLinje);

			Map<String, String> keySet = Tools.createKeyMap(VariableConfiguration.KEY_YEAR, currentYear, "ruta", varCache.getVariableValue(null, "Current_Ruta"), "linje", currentLinje);

			Selection selection = db.createSelection(keySet, "!linjeobjekt");

			List<ColumnDescriptor> columns = new ArrayList<ColumnDescriptor>();
			columns.add(new ColumnDescriptor("meter", true, false, true));
			columns.add(new ColumnDescriptor("value", false, true, false));
			WF_Linje_Meter_List selectedList = new WF_Linje_Meter_List("selected_list", true, myContext, columns, selection, "!linjeobjekt", keySet, linje, avgrValueA, avgrTyper);

			selectedList.addSorter(new WF_TimeOrder_Sorter());

			selectedPanel.addView(selectedList.getWidget());

			//Trigger null event for redraw.
			selectedList.onEvent(null);

			//Variable linjeObj = al.getVariableInstance(NamedVariables.LINJEOBJEKT);
			List<String> lobjT = al.getCompleteVariableDefinition(NamedVariables.LINJEOBJEKT);
			List<String> objTypes = al.getListElements(lobjT);
			if (objTypes != null)
				Log.d("nils", "Found objTypes! " + objTypes.toString());

			//Generate buttons.
			TextView spc = new TextView(this.getActivity());
			spc.setWidth(20);

			Button b;
			for (final String linjeObjLabel : objTypes) {
				if (stratum != null && linjeObjLabel.equals(NamedVariables.RENSTIG) &&
						!stratum.equals("10"))
					continue;

				/*TODO: Add lamps to buttons.*/

				b = new Button(this.getActivity());
				//new ButtonBlock("_"+linjeObjLabel,linjeObjLabel,"Start_Workflow",linjeObjLabel,"Field_List_panel_1",NamedVariables.WF_FOTO,"action", NamedVariables.STATUS_FOTO,true,null,null,true,xContext,false);

				LayoutParams params = new LayoutParams();
				params.width = LayoutParams.MATCH_PARENT;
				params.height = LayoutParams.WRAP_CONTENT;
				b.setLayoutParams(params);
				b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
				if (linjeObjLabel.equals(NamedVariables.RENSTIG))
					b.setText("Liten fjällstig");
				else
					b.setText(linjeObjLabel);
				b.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {

						if (linjeObjLabel.equals("Avgränsning"))
							openInterVallPopup(-1, -1);
						else
							openInterVallPopup(linjeObjLabel);
					}


				});
				fieldListB.addView(b);
			}
			spc = new TextView(this.getActivity());
			spc.setWidth(20);
			fieldListB.addView(spc);
			fieldListB.addView(stopB);

			//WF_ClickableField_Selection aggNo = new WF_ClickableField_Selection_OnSave("Avslutade Rutor:", "De rutor ni avslutat",
			//		myContext, "AvslRutor",true);
			//aggregatePanel.addView(aggNo.getWidget());
		}
		mHandler = new Handler();
		startRepeatingTask();

		return v;

	}


	private void setEnded() {
		startB.setBackgroundResource(android.R.drawable.btn_default);
		startB.setText("STARTA");
		linjeStatus.setValue(Constants.STATUS_AVSLUTAD_EXPORT_MISSLYCKAD);
		fieldListB.setVisibility(View.INVISIBLE);
		myContext.registerEvent(new WF_Event_OnSave(LinjePortalId));
	}


	@Override
	public void onStart() {
		if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
			startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0);

		super.onStart();
	}


	@Override
	public void onResume() {
		myL = null;
		if (ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			// TODO: Consider calling
			//    ActivityCompat#requestPermissions
			// here to request the missing permissions, and then overriding
			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
			//                                          int[] grantResults)
			// to handle the case where the user grants the permission. See the documentation
			// for ActivityCompat#requestPermissions for more details.
			return;
		}
		lm.requestLocationUpdates(
				LocationManager.GPS_PROVIDER,
				0,
				1,
				this);



		super.onResume();
	}

	@Override
	public void onPause() {
		myL=null;
		stopRepeatingTask();
		super.onPause();
	}






	@Override
	public void onDestroy() {
		Log.d("Vortex","On destroy called");
		lm.removeUpdates(this);
		super.onDestroy();
	}


	@Override
	protected List<WF_Container> getContainers() {
		return myLayouts;
	}

	public boolean execute(String name, String target) {
		return true;
	}



	@Override
	public void onLocationChanged(Location location) {
		if (tickerRunning)
			this.stopRepeatingTask();
		myL = Geomatte.convertToSweRef(location.getLatitude(), location.getLongitude());
		String info=refreshGPSInfo();
		if (info!=null)
			gpsView.setText(info);
		else
			gpsView.setText("GPS är igång");
	}

	private void setStart(double east, double north) {
		linjeStartEast.setValue(east+"");
		linjeStartNorth.setValue(north+"");
		center = new SweLocation(east,north);
		startB.setBackgroundColor(Color.GREEN);
		//stopB.setBackgroundColor(Color.RED);
		fieldListB.setVisibility(View.VISIBLE);
		startB.setText("STARTAD");
		linjeStatus.setValue(Constants.STATUS_STARTAD_MEN_INTE_KLAR);
		myContext.registerEvent(new WF_Event_OnSave(LinjePortalId));
		//Initialize LinjeView
		String info=refreshGPSInfo();
		if (info!=null)
			gpsView.setText(info);

	}

	private boolean eastW;
    private boolean southW;
    private boolean northW;
    private boolean westW = false;
	private String refreshGPSInfo() {

		double x=0,y=0;
		String ret=null;
		if (center!=null&&myL!=null) {
			if (northW) {
				x = myL.east-center.east;
				y = myL.north-center.north;  //the more north, the lower the value.
			}
			else if (southW) {
				x = center.east-myL.east;
				y = center.north-myL.north;
			}
			else if (eastW) {
				x = myL.north-center.north;
				y = myL.east-center.east;
			}
			else if (westW) {
				x = center.north-myL.north;
				y = center.east-myL.east;
			}

			ret = "FiS: "+((int)x)+" As:"+(int)y;
			//X should now be deviation from Line. Y is distance from Start.
			linje.setUserPos((float)x,(float)y);
			linje.invalidate();
		} 
		return ret;	
	}


	@Override
	public void onProviderDisabled(String provider) {
		gpsView.setText("GPS AV");
	}



	@Override
	public void onProviderEnabled(String provider) {
		gpsView.setText("GPS PÅ");
	}



	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		if (status == LocationProvider.AVAILABLE) {
			if (gpsView.getText().equals("Söker.."))
				gpsView.setText("GPS Igång!");
		} else if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
			gpsView.setText("Söker..");


		} else if (status == LocationProvider.OUT_OF_SERVICE) {
			gpsView.setText("Söker..");
		}
	}


	enum Linjetyp {
		PUNKT,
		INTERVALL
	}

	private void openInterVallPopup(String linjeObjLabel) {	 
		meterEd = numTmp.findViewById(R.id.edit);
		meterEd.setFilters(new InputFilter[]{ new InputFilterMinMax("0", "200")});
		openInterVallPopup(Linjetyp.PUNKT,linjeObjLabel,null);
	}



	private void openInterVallPopup(int start, int end) {		
		meterEd = intervallL.findViewById(R.id.avgrStart);
		if (start!=-1)
			meterEd.setText(start+"");
		meterEd.setFilters(new InputFilter[]{ new InputFilterMinMax("0", "199")});
		meterEnEd = intervallL.findViewById(R.id.avgrSlut);
		if (end!=-1)
			meterEnEd.setText(end+"");
		meterEnEd.setFilters(new InputFilter[]{ new InputFilterMinMax("1", "200")});		
		openInterVallPopup(Linjetyp.INTERVALL,"Avgränsning",intervallL);

	}

	private Dialog complexD=null;

	private void openInterVallPopup(final Linjetyp typ,final String linjeObjLabel, ViewGroup myView) {
		complexD = null;
		boolean skipToEnd=false;

		AlertDialog.Builder alert = new AlertDialog.Builder(this.getActivity());



		alert.setMessage("Ange metertal för linjeobjekt");
		//If punkt, determine what kind of view to present. If existing objects, show selection.
		if (typ==Linjetyp.PUNKT && myView==null) {
			Set<Map<String, String>> existingLinjeObjects = getExistingObjects(linjeObjLabel);
			if (existingLinjeObjects!=null) {
				Log.d("vortex","Got this back: "+existingLinjeObjects.toString());
				Iterator<Map<String,String>> it = existingLinjeObjects.iterator();
				//Create a button array with a button for each meter.
				List<Button> buttonArray = new ArrayList<Button>();
				while (it.hasNext()) {
					Map<String,String> currentObj = it.next();
					final String meterV = currentObj.get("meter");
					Button b = new Button(this.getActivity());
					b.setText(meterV+" meter");
					b.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							if (complexD==null)
								Log.e("vortex","complexD null..should not happen");
							else
								complexD.dismiss();
							jumpToWorkFlow(meterV, null,linjeObjLabel,typ);
						}
					});
					buttonArray.add(b);
				}
				//Skapa en knapp för fallet nytt objekt.
				Button b = new Button(this.getActivity());
				b.setText("Skapa nytt objekt på annat metertal");
				b.setOnClickListener(new OnClickListener() {					
					@Override
					public void onClick(View v) {
						complexD.dismiss();
						openInterVallPopup(typ,linjeObjLabel,numTmp);
					}
				});
				buttonArray.add(b);
				myView = new LinearLayout(this.getActivity());
				((LinearLayout)myView).setOrientation(LinearLayout.VERTICAL);
				for (Button bb:buttonArray) {
					myView.addView(bb);
				}
				alert.setMessage("Välj existerande objekt eller skapa nytt:");
				skipToEnd=true;


			} else {
				Log.d("vortex","Did not find any existing objects of type "+linjeObjLabel);
				myView = numTmp;	
			}

		}

		if (myView.getParent() !=null)
			((ViewGroup)myView.getParent()).removeView(myView);

		alert.setTitle(linjeObjLabel);

		if (!skipToEnd) {

			alert.setPositiveButton("Spara", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Editable metA=null;
					Editable metS=meterEd.getText();
					boolean error = false;
					if (typ==Linjetyp.INTERVALL) {
						metA = meterEnEd.getText();

						if (metA.length()==0||metS.length()==0) {
							o.addText("");
							o.addCriticalText("Avstånd meter tom");
							error = true;
							new AlertDialog.Builder(LinjePortalTemplate.this.getActivity())
							.setTitle("FEL: Slut värde fattas")
							.setMessage("Du måste ange både start och slut på avgränsningen!") 
							.setIcon(android.R.drawable.ic_dialog_alert)
							.setCancelable(false)
							.setNeutralButton("Ok",new Dialog.OnClickListener() {				
								@Override
								public void onClick(DialogInterface dialog, int which) {

								}
							} )
							.show();
						} else if (Integer.parseInt(metS.toString())>Integer.parseInt(metA.toString())) {
							o.addText("");
							o.addCriticalText("Start längre bort än Slut");
							error = true;
							new AlertDialog.Builder(LinjePortalTemplate.this.getActivity())
							.setTitle("Fel värden")
							.setMessage("Start måste vara lägre än slut!") 
							.setIcon(android.R.drawable.ic_dialog_alert)
							.setCancelable(false)
							.setNeutralButton("Ok",new Dialog.OnClickListener() {				
								@Override
								public void onClick(DialogInterface dialog, int which) {

								}
							} )
							.show();
						}
					}
					if (metS!=null && metS.length()>0 && !error) {
						Log.d("nils","Got meters: "+meterEd.getText());

						//Create new !linjeobjekt with the meters.
						String meter = (meterEd.getText().toString());
						//peel away zeros from beginning.
						meter = meter.replaceFirst("^0+(?!$)", "");
						Log.d("nils","meter is now: "+meter);
						jumpToWorkFlow(meter, metA!=null?metA.toString():null,linjeObjLabel,typ);
					}

				}

			});
			alert.setNegativeButton("Avbryt", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

				}
			});	
		}
		complexD = alert.setView(myView).create();
		complexD.setCancelable(true);
		complexD.show();

	}

	private void jumpToWorkFlow(String start, String end, String linjeObjLabel,Linjetyp typ) {
		Variable currentMeter = varCache.getVariable(NamedVariables.CURRENT_METER);
		if (currentYear==null||varCache.getVariableValue(null,"Current_Ruta")==null||currentLinje==null||currentMeter==null) {
			o.addText("");
			o.addCriticalText("Could not start workflow "+linjeObjLabel+
					"_wf, since no value exist for one of [Current_year, Current_ruta, Current_Linje, Current_Meter]");
		} else {
			currentMeter.setValue(start);
			//check if the variable exist. If so - no deal.
			Map<String,String> key = Tools.createKeyMap(VariableConfiguration.KEY_YEAR,currentYear,"ruta",varCache.getVariableValue(null,"Current_Ruta"),"linje",currentLinje,"meter",start,"value",linjeObjLabel);
			Map<String,String> keyI = new HashMap<String,String>(key);
			keyI.remove("value");
			if (typ==Linjetyp.INTERVALL) {							
				Log.d("nils","Sätter intervall variabler");
				Variable v = varCache.getVariable(keyI,NamedVariables.AVGRANSSLUT);
				v.setValue(end);
				v= varCache.getVariable(keyI,NamedVariables.AVGRTYP);
				Log.d("nils","Setting avgrtyp to "+ avgrSp.getSelectedItem());
				v.setValue(avgrValueA[avgrSp.getSelectedItemPosition()]);
			}

//		xx	Variable v = varCache.getVariable(key, NamedVariables.LINJEOBJEKT);
			Variable v = new Variable(NamedVariables.LINJEOBJEKT,"Linjeobjekt",al.getCompleteVariableDefinition(NamedVariables.LINJEOBJEKT),key,gs,"value",null,null,null);
			//Variable v = al.getVariableInstance();

			if (v.setValue(linjeObjLabel)) {
				Log.d("nils","Stored "+linjeObjLabel+" under meter "+start);
				myContext.registerEvent(new WF_Event_OnSave("Template"));
			} else 
				Log.e("nils","Variable "+v.getId()+" Obj:"+v+" already has value "+v.getValue()+" for keychain "+key.toString());

			if (typ == Linjetyp.PUNKT) {
				if (linjeObjLabel.equals(NamedVariables.RENSTIG)) {								
					varCache.getVariable(keyI, NamedVariables.TransportledTyp).setValue("2");
				} else {
					//Start workflow here.
					Log.d("nils","Trying to start workflow "+"wf_"+linjeObjLabel);
					Workflow wf = gs.getWorkflow("wf_"+linjeObjLabel);

					if (wf!=null) {
						gs.changePage(wf, null);
						Log.d("nils","Should have started "+"wf_"+linjeObjLabel);
					} 
					else {
						o.addText("");
						o.addCriticalText("Couldn't find workflow named "+"wf_"+linjeObjLabel);
						Log.e("nils","Couldn't find workflow named"+"wf_"+linjeObjLabel);
					}
				}
			}
		}

	}


	private Set<Map<String, String>> getExistingObjects(String linjeObjLabel) {
		Map<String,String> objChain = Tools.createKeyMap(VariableConfiguration.KEY_YEAR,currentYear,"ruta",varCache.getVariableValue(null,"Current_Ruta"),"linje",currentLinje,"value",linjeObjLabel);
		return db.getKeyChainsForAllVariableInstances(NamedVariables.LINJEOBJEKT, objChain, "meter");
	}


	@Override
	public void onEvent(Event e) {
		//Py ruta changed. Force reload of page with new settings.


		if (e instanceof WF_Event_OnBluetoothMessageReceived) { 
			if ((gs.getOriginalMessage() instanceof LinjeStarted) && !isRunning()) {
				String linjeId = ((LinjeStarted)gs.getOriginalMessage()).linjeId;
				if (linjeId!=null && linjeId.equals(currentLinje)) {
					if (linjeStartEast.getValue()!=null && linjeStartNorth!=null) {
						double lStartE = Double.parseDouble(linjeStartEast.getValue());
						double lStartN = Double.parseDouble(linjeStartNorth.getValue());										
						setStart(lStartE,lStartN);
					}
				}
			}
			else if (gs.getOriginalMessage() instanceof LinjeDone && isRunning()) {
				String linjeId = ((LinjeDone)gs.getOriginalMessage()).linjeId;
				if (linjeId!=null && linjeId.equals(currentLinje)) 
					setEnded();

			}
		}
	}

	@Override
	public String getName() {
		return "LINJEPORTAL";
	}

	public boolean isRunning() {
		return linjeStatus != null && linjeStatus.getValue().equals(Constants.STATUS_STARTAD_MEN_INTE_KLAR);
	}


	private static final String ticker = "       ...Väntar på GPS...        ";


    private int curPos=0;
	private void updateStatus() {
        int tStrLen = 10;
        int end = curPos+ tStrLen;
		if (end>ticker.length())
			end = ticker.length();
		if (curPos==ticker.length()) {
			curPos=0;
			end = tStrLen;
		}
		String cString = ticker.substring(curPos, end);
		curPos++;
		gpsView.setText(cString);		
	}

	private boolean tickerRunning=false;
	private void startRepeatingTask() {
		curPos = 0;
		tickerRunning = true;
		mStatusChecker.run(); 
	}

	private void stopRepeatingTask() {
		mHandler.removeCallbacks(mStatusChecker);
		tickerRunning = false;

	}
	private Handler mHandler;
    private final Runnable mStatusChecker = new Runnable() {
		@Override 
		public void run() {
			updateStatus(); //this function can change value of mInterval.
            int mInterval = 250;
            mHandler.postDelayed(mStatusChecker, mInterval);
		}
	};

}