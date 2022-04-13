package com.teraim.fieldapp.dynamic.templates;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.gesture.GestureLibrary;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.SweLocation;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.VariableCache;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.utils.ImageHandler;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class FotoTemplate extends Executor {

    private GestureLibrary gestureLib;
	//private ToggleButton gpsB;
	//private TextView gpsT,GPS_X,GPS_Y;
	private boolean fixed = false;
	private ImageButton norr;
	private ImageButton syd;
	private ImageButton ost;
	private ImageButton vast;
	private ImageButton sp;
    private ImageHandler imgHandler;
	private LocationManager lm;
	private final HashMap<String, ImageButton> buttonM = new HashMap<String,ImageButton>();
	private SweLocation cords;
    private TextView sydT,vastT,ostT,spT,norrT;

    private View v;
	
	private String selectedPictureName="";
	private ImageButton selectedPicture=null;
	private ImageView compass;

	private Variable myStatusVariable;

	
	private final ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		// Called when the action mode is created; startActionMode() was called
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			// Inflate a menu resource providing context menu items
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.foto_deletemenu, menu);
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
			
			boolean result;
			switch (item.getItemId()) {
			    
			case R.id.menu_delete:
				result = imgHandler.deleteImage(selectedPictureName);
				
				if (result) {
					Log.d("vortex","delete success!");
					selectedPicture.setImageResource(R.drawable.case_no_pic);
					}
				else
					Log.e("vortex","failed to delete "+selectedPictureName);
				mode.finish(); // Action picked, so close the CAB
				return true;
			case R.id.menu_se_stor:
				ImageButton b = new ImageButton(getActivity());
				result = imgHandler.drawButton(b, selectedPictureName, 1,false);
				if (result) {

					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
					builder.setView(b);
					builder.setPositiveButton("Stäng", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {

						}
					});
					builder.show();
				}
				return true;
				
				
			default:
				return false;
			}
		}

		// Called when the user exits the action mode
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			//mActionMode = null;				
		}
	};

	
	
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (myContext == null) {
			Log.e("vortex","No context, exit");
			return null;
		}
		gs.setDBContext(new DB_Context(null,al.createProvytaKeyMap()));
		//myContext.resetState();
		Log.d("nils","in onCreateView of foto template");
		
		v = inflater.inflate(R.layout.template_foto, container, false);	
		
		//		myLayouts.add(new WF_Container("Field_List_panel_1", (LinearLayout)v.findViewById(R.id.fieldList), root));
		//		myLayouts.add(new WF_Container("Aggregation_panel_3", (LinearLayout)v.findViewById(R.id.aggregates), root));
		//		myLayouts.add(new WF_Container("Description_panel_1", (FrameLayout)v.findViewById(R.id.Description), root));
		myContext.addContainers(getContainers());

/*
		//Gestures
		GestureOverlayView gestureOverlayView = (GestureOverlayView)v.findViewById(R.id.gesture_overlay);
		gestureOverlayView.setGestureVisible(false);
		gestureOverlayView.addOnGesturePerformedListener(this);
		gestureLib = GestureLibraries.fromRawResource(this.getActivity(), R.raw.gestures);
		if (!gestureLib.load()) {      	
			Log.i("nils", "Load gesture libraries failed.");  
		}  
*/

        Switch avstandB = v.findViewById(R.id.avstandB);
		//gpsB = (ToggleButton)v.findViewById(R.id.gpsBtn);
		//if (gpsB == null)
		//	Log.e("vortex","OUCCGHHH!!");
		//gpsT = (TextView)v.findViewById(R.id.gpsText);
		//GPS_X = (TextView)v.findViewById(R.id.GPS_X);
		//GPS_Y = (TextView)v.findViewById(R.id.GPS_Y);
		norr = v.findViewById(R.id.pic_norr);
		syd = v.findViewById(R.id.pic_soder);
		ost= v.findViewById(R.id.pic_ost);
		vast = v.findViewById(R.id.pic_vast);
		sp= v.findViewById(R.id.sp);
		sydT = v.findViewById(R.id.sydT);
		vastT = v.findViewById(R.id.vastT);
		ostT = v.findViewById(R.id.ostT);
		spT = v.findViewById(R.id.spT);
		norrT = v.findViewById(R.id.norrT);
		compass = v.findViewById(R.id.compass);
        Button oldpicsButton = v.findViewById(R.id.oldpics_button);
		
		buttonM.clear();
		//gpsT.setText("");
		
		//Init pic handler
		imgHandler = new ImageHandler(gs,this);		
		//Init geoupdate
		lm = (LocationManager)this.getActivity().getSystemService(Context.LOCATION_SERVICE);
		//Create buttons.
		
		initPic(norr,Constants.NORR);
		initPic(syd,Constants.SYD);
		initPic(ost,Constants.OST);
		initPic(vast,Constants.VAST);
		initPic(sp,Constants.SMA);

		File[] externalStorageVolumes =
				ContextCompat.getExternalFilesDirs(GlobalState.getInstance().getContext(), null);
		File primaryExternalStorage = externalStorageVolumes[0];
		String PIC_ROOT_DIR = primaryExternalStorage.getAbsolutePath() + "/pics/";
		File folder = new File(PIC_ROOT_DIR);
		folder.mkdirs();

		avstandB.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
				toggleAvstand(isChecked);
				if (isChecked && !gs.getPreferences().getB(PersistenceHelper.AVSTAND_WARNING_SHOWN)) {
					new AlertDialog.Builder(FotoTemplate.this.getActivity())
					.setTitle("OBS!")
					.setMessage("Du ska ta antingen en avståndsbild eller fyra vanliga bilder. Inte både och") 
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setCancelable(false)
					.setNeutralButton("Ok",new Dialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							gs.getPreferences().put(PersistenceHelper.AVSTAND_WARNING_SHOWN,true);
						}
					} )
					.show();
				}
					
			}
		});
		
		oldpicsButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				turnPage();
			}
		});
		
	
		VariableCache varCache = gs.getVariableCache();
        Variable n = varCache.getVariable(NamedVariables.CentrumGPSNS);
        Variable e = varCache.getVariable(NamedVariables.CentrumGPSEW);
		/*
		if (e.getValue()!=null &&n.getValue()!=null) {
			GPS_X.setText(e.getValue()+"");
			GPS_Y.setText(n.getValue()+"");
			gpsB.setChecked(true);
			fixed = true;
		} else {
			fixed = false;
			gpsB.setChecked(false);
		}
		*/
		
		
		if (gs.getPreferences().getB(PersistenceHelper.AVSTAND_IS_PRESSED)) {
			toggleAvstand(true);
			avstandB.setChecked(true);
		}
		
		
		
		myStatusVariable = varCache.getVariable(NamedVariables.STATUS_FOTO);
		
		return v;
		
	}
	

	private void toggleAvstand(boolean avstand) {
		Log.d("nils","Avstånd is "+avstand);

		int status = avstand?View.INVISIBLE:View.VISIBLE;
		boolean luck = false;
		if (avstand) {
			sydT.setText("Avståndsbild");
			luck = initPic(syd,Constants.AVST);
		} else {
			sydT.setText("Mot syd");
			luck = initPic(syd,Constants.SYD);
		}
		if (!luck)
			syd.setImageResource(R.drawable.case_no_pic);
		
		norr.setVisibility(status);
		ost.setVisibility(status);
		vast.setVisibility(status);
		sp.setVisibility(status);
		//gpsB.setVisibility(status);
		//gpsT.setVisibility(status);
		//GPS_X.setVisibility(status);
		//GPS_Y.setVisibility(status);
		vastT.setVisibility(status);
		ostT.setVisibility(status);
		spT.setVisibility(status);
		norrT.setVisibility(status);
		compass.setVisibility(status);
		gs.getPreferences().put(PersistenceHelper.AVSTAND_IS_PRESSED,avstand);
	}
	

	private boolean initPic(final ImageButton b, final String name) {
		boolean ret;
		buttonM.put(name, b);
		ret = imgHandler.drawButton(b,name,2,false);
		Log.e("vortex","Adding listener to imgb "+b+" with name "+name);
		imgHandler.addListener(b,name);
		b.setLongClickable(true);
		b.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				selectedPictureName = name;
				selectedPicture = b;
				((Activity)myContext.getContext()).startActionMode(mActionModeCallback);
				return true;
			}
		});
		return ret;
	}

	/*@Override
	public void onPause() {
		super.onPause();
		lm.removeUpdates(this);
	}

	@Override
	public void onResume() {
		super.onResume();

		//---request for location updates---
		lm.requestLocationUpdates(
				LocationManager.GPS_PROVIDER,
				0,
				1,
				this);

	}
*/



	@Override
	public void onStart() {
		super.onStart();
		if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
			startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0);
		Log.d("nils","in onStart foto");

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent myI){
		Log.d("nils","Gets to onActivityResult");

		if (requestCode == Constants.TAKE_PICTURE){
			if (resultCode == Activity.RESULT_OK) 
			{
				Log.d("Strand","picture was taken, result ok");
				//				String name = myI.getStringExtra(Strand.KEY_PIC_NAME);
				String currSaving = imgHandler.getCurrentlySaving();
				if (currSaving!=null) {
					ImageButton b = buttonM.get(currSaving);
					imgHandler.drawButton(b,currSaving,2,false);
					Log.d("Strand","Drew button!");
					
				} else
					Log.e("Strand","Did not find pic with name "+currSaving+" in onActRes in TakePic Activity");
			} else {
				Log.d("Strand","picture was NOT taken, result NOT ok");
			}

		}

	}



	@Override
	protected List<WF_Container> getContainers() {
        List<WF_Container> myLayouts = new ArrayList<WF_Container>();
		myLayouts.add(new WF_Container("root", v.findViewById(R.id.root), null));
		return myLayouts;
	}

	public boolean execute(String name, String target) {
		return true;
	}

/*

	@Override
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
		ArrayList<Prediction> predictions = gestureLib.recognize(gesture);
		for (Prediction prediction : predictions) {
			if (prediction.score > .5) {
				Log.d("nils","MATCH!!");
				if (prediction.name.equals("left")) {
					turnPage();
				} 

			}
		}		
	}
*/

	private void turnPage() {
		final FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction(); 
		OldPhotosFragment gs = new OldPhotosFragment();  			
		ft.replace(R.id.content_frame, gs);
		ft.addToBackStack(null);
		ft.commit(); 
	}


	@Override
	public void onStop() {
		File[] externalStorageVolumes =
				ContextCompat.getExternalFilesDirs(GlobalState.getInstance().getContext(), null);
		File primaryExternalStorage = externalStorageVolumes[0];
		String PIC_ROOT_DIR = primaryExternalStorage.getAbsolutePath() + "/pics/";
		Log.d("vortex","In foto stop");
		boolean hasN = Tools.doesFileExist(PIC_ROOT_DIR,imgHandler.createFileName(Constants.NORR,false));
		boolean hasS = Tools.doesFileExist(PIC_ROOT_DIR,imgHandler.createFileName(Constants.SYD,false));
		boolean hasE = Tools.doesFileExist(PIC_ROOT_DIR,imgHandler.createFileName(Constants.OST,false));
		boolean hasW = Tools.doesFileExist(PIC_ROOT_DIR,imgHandler.createFileName(Constants.VAST,false));
		boolean hasSma=Tools.doesFileExist(PIC_ROOT_DIR,imgHandler.createFileName(Constants.SMA,false));
		boolean hasFjarr = Tools.doesFileExist(PIC_ROOT_DIR,imgHandler.createFileName(Constants.AVST,false));
		boolean hasAll = hasN&&hasS&&hasE&&hasW&&hasSma;
		boolean hasOne = hasN||hasS||hasE||hasW||hasSma;
		
		if (hasOne && hasFjarr)
			myStatusVariable.setValue("2");
		else if ((hasAll&&!hasFjarr)||(hasFjarr&&!hasAll))
			myStatusVariable.setValue("3");
		else if (!hasOne && !hasFjarr)
			myStatusVariable.setValue("0");
		else if (hasOne || hasFjarr)
			myStatusVariable.setValue("1");
		Log.d("vortex","mystatus: "+myStatusVariable.getValue()+" hasAll: "+hasAll+" hasFjarr: "+hasFjarr+" hasone: "+hasOne);
		super.onStop();
	}

	
/*
	@Override
	public void onLocationChanged(Location location) {
		//If no startpunkt set, update button
		if (!fixed) {
			cords = Geomatte.convertToSweRef(location.getLatitude(),location.getLongitude());
			gpsB.setVisibility(View.VISIBLE);
			GPS_X.setText(cords.east+"");
			GPS_Y.setText(cords.north+"");
			//gpsB.setText("S�tt startpunkt\n(N: "+cords[0]+" \n�: "+cords[1]+")");
		} 
	}

	@Override
	public void onProviderDisabled(String provider) {
		gpsB.setVisibility(View.GONE);
		gpsT.setText("GPS avslagen.");
	}
	@Override
	public void onProviderEnabled(String arg0) {
		gpsT.setText("GPS p�slagen. S�ker..");
	}
	@Override
	public void onStatusChanged(String provider, int status, Bundle arg2) {
		if (status == LocationProvider.AVAILABLE) {
			gpsT.setText("GPS signal ok");
		} else if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
			gpsB.setVisibility(View.GONE);
			gpsT.setText("S�ker GPS..");


		} else if (status == LocationProvider.OUT_OF_SERVICE) {
			gpsB.setVisibility(View.GONE);
			gpsT.setText("GPS tj�nst f�rlorad");

		}


	}

	public boolean setStartPoint() {	
		if(cords!=null) {
			e.setValue(cords.east+"");
			n.setValue(cords.north+"");
			gpsT.setText("Startpunkt satt till:\nN: "+cords.north+"\n�: "+cords.east);
			fixed=true;
			myContext.registerEvent(new WF_Event_OnSave("fototemplate"));
			return true;
		} else {
			gpsT.setText("Kan inte s�tta ett v�rde! Ingen GPS signal.");
			return false;
		}
	}
*/


}