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
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Linje_Meter_List;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_TimeOrder_Sorter;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.ui.Linje;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.DbHelper.Selection;
import com.teraim.fieldapp.utils.Geomatte;
import com.teraim.fieldapp.utils.InputFilterMinMax;
import com.teraim.fieldapp.utils.Tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class LineMapTemplate extends Executor implements LocationListener, EventListener {
    private List<WF_Container> myLayouts;
    private VariableCache varCache;
    private DbHelper db;

    private SweLocation myL = null, linjeStart = null, linjeSlut = null;
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

    private final static String LinjePortalId = "LinjePortalTemplate";

    //private SweLocation center = new SweLocation(6564201.573, 517925.98);

    private LocationManager lm;
    private Variable linjeStatus, linjeStartEast, linjeStartNorth;
    private String[] avgrValueA;

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("nils", "in onCreateView of LineMapTemplate");
        if (myContext == null) {
            Log.d("vortex", "hasnt survived create...exiting.");
            return null;
        }
        run();
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

        //HACK. Set current variables to trakt and uuid from key.
        Variable currentLineV = varCache.getVariable("Current_Linje");
        Variable currentRutaV = varCache.getVariable("Current_Ruta");

        currentLinje = myContext.getKeyHash().get("uid");
        String currentRuta = myContext.getKeyHash().get("trakt");
        currentYear = "2018";
        //Find out the line start coordinates (east, north)
        double east, north;
        Variable gpsCoordV = gs.getVariableCache().getVariable("GPSCoord");
        if (gpsCoordV.getValue() != null) {
            String[] tmp = gpsCoordV.getValue().split(",");
            int i = 0;
            double[] coords = new double[tmp.length];
            for (String coord : tmp) {
                //coords contains the coordinates now.
                coords[i++] = Double.parseDouble(coord);

            }
        }

        currentRutaV.setValue(currentRuta);
        currentLineV.setValue(currentLinje);


        Log.d("nils", "Current Linje is " + currentLinje);


        Map<String, String> linjeKey = Tools.createKeyMap(VariableConfiguration.KEY_YEAR, currentYear, "trakt", currentRuta, "linje", currentLinje);

        linjeStatus = varCache.getVariable(myContext.getKeyHash(), NamedVariables.STATUS_TRANSEKT);


        if (linjeStatus.getValue() != null) {
            if (linjeStatus.equals(Constants.STATUS_INITIAL)) {
                stopB.setText("KLAR");
                startB.setText("STARTA");

            } else if (linjeStatus.getValue().equals(Constants.STATUS_STARTAD_MEN_INTE_KLAR)) {
                startDataCollection();
            } else if (linjeStatus.getValue().equals(Constants.STATUS_AVSLUTAD_EXPORT_MISSLYCKAD)) {
                setEnded();
            }
        } else {
            Log.e("nils", "Linjestatus was null");
            linjeStatus.setValue(Constants.STATUS_INITIAL);
            startB.setText("STARTA");
            fieldListB.setVisibility(View.INVISIBLE);
        }


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
                o.addRow("");
                o.addRedText("Variabeln Avgränsning:AvgrTyp saknar värden.");
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

        linje = new Linje(getActivity(), ("N"));

        linjeF.addView(linje);

        stopB.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
                alert.setTitle("Klar");
                alert.setMessage("Vill du säkert avsluta och klarmarkera?");
                alert.setCancelable(false);
                alert.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        setEnded();
                        myContext.registerEvent(new WF_Event_OnSave(LinjePortalId));
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
                Log.d("vortex", "gets click. Linjestatus: " + linjeStatus.getValue());
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
                        if (myL == null) {
                            new AlertDialog.Builder(LineMapTemplate.this.getActivity())
                                    .setTitle(getResources().getString(R.string.ladda_styrfiler))
                                    .setMessage("Eftersom GPSen ännu inte hittat din position, så kan du inte gå linjen.")
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // continue with delete
                                        }
                                    })
                                    .setCancelable(false)
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                        } else {


                            startDataCollection();
                        }

                    }
                }

            }
        });

        Log.d("nils", "year: " + currentYear + " Ruta: " + varCache.getVariableValue(null, "Current_Ruta") + " Linje: " + currentLinje);

        Map<String, String> keySet = Tools.createKeyMap(VariableConfiguration.KEY_YEAR, currentYear, "trakt", varCache.getVariableValue(null, "Current_Ruta"), "linje", currentLinje);

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


            /*TODO: Add lamps to buttons.*/

            b = new Button(this.getActivity());
            //new ButtonBlock("_"+linjeObjLabel,linjeObjLabel,"Start_Workflow",linjeObjLabel,"Field_List_panel_1",NamedVariables.WF_FOTO,"action", NamedVariables.STATUS_FOTO,true,null,null,true,xContext,false);

            LayoutParams params = new LayoutParams();
            params.width = LayoutParams.MATCH_PARENT;
            params.height = LayoutParams.WRAP_CONTENT;
            b.setLayoutParams(params);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);

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

        mHandler = new Handler();
        startRepeatingTask();

        return v;

    }


    private void setEnded() {
        startB.setBackgroundResource(android.R.drawable.btn_default);
        startB.setText("Gör om");
        linjeStatus.setValue(Constants.STATUS_AVSLUTAD_EXPORT_MISSLYCKAD);
        fieldListB.setVisibility(View.INVISIBLE);

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

	private void startDataCollection() {

		startB.setBackgroundColor(Color.GREEN);
		fieldListB.setVisibility(View.VISIBLE);
		startB.setText("STARTAD");
		linjeStatus.setValue(Constants.STATUS_STARTAD_MEN_INTE_KLAR);
		myContext.registerEvent(new WF_Event_OnSave(LinjePortalId));
		//Initialize LinjeView
		String info=refreshGPSInfo();
		if (info!=null)
			gpsView.setText(info);

	}


	//check difference between
	private String refreshGPSInfo() {

		double x=0,y=0;
		String ret=null;

		if (myL!=null) {
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
		if (typ== Linjetyp.PUNKT && myView==null) {
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
					if (typ== Linjetyp.INTERVALL) {
						metA = meterEnEd.getText();

						if (metA.length()==0||metS.length()==0) {
							o.addRow("");
							o.addRedText("Avstånd meter tom");
							error = true;
							new AlertDialog.Builder(LineMapTemplate.this.getActivity())
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
							o.addRow("");
							o.addRedText("Start längre bort än Slut");
							error = true;
							new AlertDialog.Builder(LineMapTemplate.this.getActivity())
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
			o.addRow("");
			o.addRedText("Could not start workflow "+linjeObjLabel+
					"_wf, since no value exist for one of [Current_year, Current_ruta, Current_Linje, Current_Meter]");
		} else {
			currentMeter.setValue(start);
			//check if the variable exist. If so - no deal.
			Map<String,String> key = Tools.createKeyMap(VariableConfiguration.KEY_YEAR,currentYear,"trakt",varCache.getVariableValue(null,"Current_Ruta"),"linje",currentLinje,"meter",start,"value",linjeObjLabel);
			Map<String,String> keyI = new HashMap<String,String>(key);
			keyI.remove("value");
			if (typ== Linjetyp.INTERVALL) {
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
						o.addRow("");
						o.addRedText("Couldn't find workflow named "+"wf_"+linjeObjLabel);
						Log.e("nils","Couldn't find workflow named"+"wf_"+linjeObjLabel);
					}
				}
			}
		}

	}


	private Set<Map<String, String>> getExistingObjects(String linjeObjLabel) {
		Map<String,String> objChain = Tools.createKeyMap(VariableConfiguration.KEY_YEAR,currentYear,"trakt",varCache.getVariableValue(null,"Current_Ruta"),"linje",currentLinje,"value",linjeObjLabel);
		return db.getKeyChainsForAllVariableInstances(NamedVariables.LINJEOBJEKT, objChain, "meter");
	}


	@Override
	public void onEvent(Event e) {
		//Py ruta changed. Force reload of page with new settings.



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