package com.teraim.fieldapp.utils;

import android.app.AlertDialog;
import androidx.fragment.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.non_generics.Constants;

import java.io.File;

public class ImageHandler {

	private final Fragment fragment;
    private final VariableConfiguration al;

	public ImageHandler(GlobalState gs, Fragment _fragment) {
		fragment = _fragment;
        this.al = gs.getVariableConfiguration();
	}


	public String createFileName(String name,boolean isHistorical) {

		String nameWithNum = name;
		if (name.equals("SYD"))
			nameWithNum = "3SYD_";
		else if (name.equals("NORR"))
			nameWithNum = "1NORR";
		else if (name.equals("OST"))
			nameWithNum = "2OST_";
		else if (name.equals("VAST"))
			nameWithNum = "4VAST";
		else if (name.equals("SMA"))
			nameWithNum = "5SMA_";
		else if (name.equals("AVST"))
			nameWithNum = "6AVST";
		String rutID = al.getCurrentRuta();
		String pyID = al.getCurrentProvyta();
		if (rutID!=null&&pyID!=null) {
			int paddingSize=4-rutID.length();
			for (int i=0;i<paddingSize;i++)
				rutID = "0"+rutID;
			Log.d("nils"," PADDINGSIZE: "+paddingSize+" rutaWITHZ: "+rutID);
			paddingSize=4-pyID.length();
			for (int i=0;i<paddingSize;i++)
				pyID = "0"+pyID;		
			Log.d("nils"," PADDINGSIZE: "+paddingSize+" pyWITHZ: "+pyID);
			return "R"+rutID+"_"+pyID+"_"+nameWithNum+"_"+(isHistorical?(Constants.getHistoricalPictureYear()):Constants.getYear())+".JPG";
		}
		return null;
	}

	

	public boolean drawButton(ImageButton b, String name, int divisor,boolean historical) {
		
		//Try to load pic from disk, if any.
		//To avoid memory issues, we need to figure out how big bitmap to allocate, approximately
		//Picture is in landscape & should be approx half the screen width, and 1/5th of the height.
		//First get the ration between h and w of the pic.
		final BitmapFactory.Options options = new BitmapFactory.Options();

		final String fileName = createFileName(name,historical);
		if (fileName == null) {			
			return false;
		}
		options.inJustDecodeBounds=true;
		File[] externalStorageVolumes =
				ContextCompat.getExternalFilesDirs(GlobalState.getInstance().getContext(), null);
		File primaryExternalStorage = externalStorageVolumes[0];
		String PIC_ROOT_DIR = primaryExternalStorage.getAbsolutePath() + "/pics/";
		String OLD_PIC_ROOT_DIR = primaryExternalStorage.getAbsolutePath() + "/old_pics/";
		Bitmap bip = BitmapFactory.decodeFile((historical?OLD_PIC_ROOT_DIR:PIC_ROOT_DIR)+fileName,options);

		//there is a picture..
		int realW = options.outWidth;
		int realH = options.outHeight;


		//check if file exists
		if (realW>0) {
			double ratio = realH/realW;
			//Height should not be higher than width.
			//Find out screen size.
			Display display = fragment.getActivity().getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			int sWidth = size.x;

			//Target width should be about half the screen width.

			double tWidth = sWidth/divisor;
			//height is then the ratio times this..
			int tHeight = (int) (tWidth*ratio);

			//use target values to calculate the correct inSampleSize
			options.inSampleSize = Tools.calculateInSampleSize(options, (int)tWidth, tHeight);

			Log.d("nils"," Calculated insamplesize "+options.inSampleSize);
			//now create real bitmap using insampleSize

			options.inJustDecodeBounds = false;
			Log.d("nils","Filename: "+fileName);
			bip = BitmapFactory.decodeFile((historical?OLD_PIC_ROOT_DIR:PIC_ROOT_DIR)+fileName,options);
			if (bip!=null) {
				b.setImageBitmap(bip);
				return true;
			} else {
				Log.d("bils","Picture was null after decode");
				return false;
			}

		}
		else {
			Log.d("nils","Did not find picture "+fileName);
			//need to set the width equal to the height...
			return false;
		}
	}
	
	public boolean deleteImage(final String name) {
		File[] externalStorageVolumes =
				ContextCompat.getExternalFilesDirs(GlobalState.getInstance().getContext(), null);
		File primaryExternalStorage = externalStorageVolumes[0];
		String PIC_ROOT_DIR = primaryExternalStorage.getAbsolutePath() + "/pics/";
		String fileName = createFileName(name,false);
		File file = new File(PIC_ROOT_DIR, fileName);
		return file.delete();
	}

	public void addListener(ImageButton b, final String name) {

		b.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				Log.d("nils","in the listener for image button");
				String fileName = createFileName(name,false);

				//String fileName = "R00al.getCurrentRuta()
				//Toast.makeText(fragment.getActivity(),
				//		"pic" + name + " selected",
				//		Toast.LENGTH_SHORT).show();
				currSaving=name;
				File[] externalStorageVolumes =
						ContextCompat.getExternalFilesDirs(GlobalState.getInstance().getContext(), null);
				File primaryExternalStorage = externalStorageVolumes[0];
				String PIC_ROOT_DIR = primaryExternalStorage.getAbsolutePath() + "/pics/";
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				File photoFile = new File(PIC_ROOT_DIR, fileName);
				if (intent.resolveActivity(fragment.getActivity().getPackageManager()) != null) {
					// Create the File where the photo should go
					photoFile = new File(PIC_ROOT_DIR,Constants.TEMP_BARCODE_IMG_NAME);

					// Continue only if the File was successfully created
					if (photoFile != null) {
						Uri photoURI = FileProvider.getUriForFile(fragment.getContext(),
								"com.teraim.fieldapp.fileprovider",
								photoFile);
						intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
						fragment.getActivity().startActivityForResult(intent, Constants.TAKE_PICTURE);
					}
				}

			}


		});


	}
	



	

	private String currSaving=null;

	public String getCurrentlySaving() {
		return currSaving;
	}

	private void displayErrorMsg() {
		new AlertDialog.Builder(fragment.getActivity())
		.setTitle("Ingen ruta/provyta vald")
		.setMessage("För att spara och visa bilder måste först ruta och provyta väljas")
		.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) { 
				// continue with delete
			}
		})

		.setIcon(android.R.drawable.ic_dialog_alert)
		.show();
	}

}
