package com.teraim.fieldapp.dynamic.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.Connectivity;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

public class EmptyTemplate extends Executor {


	private ViewGroup my_root;
	private TextView versionTxt;

	@Override
	public boolean execute(String function, String target) {
		return true;
	}

	@Override
	public void onStart() {
		Log.d("vortex","in onstart for empty fragment");
		super.onStart();
	}

	View view;
	ImageView bg,logo;
	TextView appName, appVersion;
	Button load_configuration_button;
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		Log.d("gipp","on create view EmptyTemplate wf is "+wf.getLabel());
		view = inflater.inflate(R.layout.template_empty, container, false);
		my_root = view.findViewById(R.id.myRoot);

		if (myContext != null )
			myContext.addContainers(getContainers());
		
        load_configuration_button = my_root.findViewById(R.id.buttonLoadConfig);
        appName = view.findViewById(R.id.textViewAppValue);
		appVersion = view.findViewById(R.id.textViewVersionValue);
		versionTxt = view.findViewById(R.id.textViewAppVersion);
		bg = view.findViewById(R.id.bgImg);
		logo = view.findViewById(R.id.logo);


		if (wf!=null) {
			Log.d("gipp", "gets here, wf is " + wf.getLabel());
			run();
		}
		PersistenceHelper globalPh = GlobalState.getInstance().getGlobalPreferences();
		PersistenceHelper ph = GlobalState.getInstance().getPreferences();
		String serverURL = globalPh.get(PersistenceHelper.SERVER_URL);
		String bundleName = globalPh.get(PersistenceHelper.BUNDLE_NAME);
		float currentVersionF = ph.getF(PersistenceHelper.CURRENT_VERSION_OF_APP);
		String currentVersion = String.valueOf(currentVersionF);
		String appBaseUrl = serverURL+bundleName.toLowerCase(Locale.ROOT)+"/";
		String appRootFolderPath = getContext().getFilesDir()+"/"+bundleName.toLowerCase(Locale.ROOT)+"/";

		Tools.onLoadCacheImage(appBaseUrl,"bg_image.jpg", appRootFolderPath+"cache/", new Tools.WebLoaderCb() {
			@Override
			public void loaded(Boolean result) {
				if (result) {
					Bitmap bm = BitmapFactory.decodeFile(appRootFolderPath+"cache/bg_image.jpg", new BitmapFactory.Options());
					if (bm!=null)
						bg.setImageBitmap(bm);
				}
			}

			@Override
			public void progress(int bytesRead) {
			}
		});


		Tools.onLoadCacheImage(appBaseUrl,"logo.png", appRootFolderPath+"cache/", new Tools.WebLoaderCb() {
			@Override
			public void loaded(Boolean result) {
				if (result) {
					Bitmap bm = BitmapFactory.decodeFile(appRootFolderPath+"cache/logo.png", new BitmapFactory.Options());
					if (bm!=null)
						logo.setImageBitmap(bm);
				}
			}
			@Override
			public void progress(int bytesRead) {
			}
		});
		appName.setText(bundleName);
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		PersistenceHelper globalPh = GlobalState.getInstance().getGlobalPreferences();
		PersistenceHelper ph = GlobalState.getInstance().getPreferences();
		float currentVersionF = ph.getF(PersistenceHelper.CURRENT_VERSION_OF_APP);
		String currentVersion = String.valueOf(currentVersionF);

		load_configuration_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

					if (!Connectivity.isConnected(getContext())) {
						new AlertDialog.Builder(Start.singleton)
								.setTitle("No connection - cannot reload configuration")
								.setMessage("Please try again when you are connected")
								.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {

									}
								})
								.setCancelable(true)
								.setIcon(android.R.drawable.ic_dialog_alert)
								.show();
					} else {
						new AlertDialog.Builder(Start.singleton)
								.setTitle(getResources().getString(R.string.ladda_styrfiler))
								.setMessage(getResources().getString(R.string.ladda_descr))
								.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										PersistenceHelper ph = GlobalState.getInstance().getPreferences();
										ph.put(PersistenceHelper.ALL_MODULES_FROZEN + "moduleLoader", null);
										ph.put(PersistenceHelper.ALL_MODULES_FROZEN + "dbLoader", null);
										ph.put(PersistenceHelper.CURRENT_VERSION_OF_WF_BUNDLE, null);
										Tools.restart(Start.singleton);
									}
								})
								.setNegativeButton("Cancel", null)
								.setCancelable(false)
								.setIcon(android.R.drawable.ic_dialog_alert)
								.show();
					}
				}
		});
		appVersion.setText(currentVersion);
		versionTxt.setText(Constants.VORTEX_VERSION);
	}

	protected List<WF_Container> getContainers() {
		ArrayList<WF_Container> ret = new ArrayList<WF_Container>();
		ret.add(new WF_Container("root",my_root,null));
		return ret;
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d("gipp","Emptytemplate on resume!");

		//v.setBackgroundColor(Color.BLUE);
	}

	@Override
	public void onStop() {
		Log.d("gipp","Emptytemplate on stop!");
		super.onStop();
	}

	@Override
	public void onDestroy() {
		Log.d("gipp","on destroy!");
		super.onDestroy();
	}
}
