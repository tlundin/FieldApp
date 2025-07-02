/**
 * 
 */
package com.teraim.fieldapp.dynamic.blocks;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

import androidx.core.content.ContextCompat;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Widget;
import com.teraim.fieldapp.log.LogRepository;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;
import com.teraim.fieldapp.utils.Tools;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Terje
 *
 */
	public class CreateImageBlock extends Block implements EventListener {

	private transient ImageView img = null;
	private transient WF_Context myContext;
	private transient String dynImgName;
    private final String container;
	private final String source;
	private final String scale;
	private final boolean isVisible;
	private final List<EvalExpr> sourceE;

	public CreateImageBlock(String id, String nName, String container,
			String source, String scale, boolean isVisible) {
		this.blockId=id;
        this.container = container;
		this.sourceE=Expressor.preCompileExpression(source);
		this.source=source;
		this.scale = scale;
		this.isVisible = isVisible;
	}


	public void create(WF_Context myContext) {
		this.myContext = myContext;
		o = LogRepository.getInstance();
		WF_Container myContainer = (WF_Container)myContext.getContainer(container);
		Log.d("botox","Source name is "+source);
		if (myContainer != null && sourceE!=null) {
			dynImgName = Expressor.analyze(sourceE);
			Log.d("botox","my image name before: "+dynImgName);

			ScaleType scaleT=ScaleType.FIT_XY;
			img = new ImageView(myContext.getContext());
			if (Tools.isURL(dynImgName)) {
				new DownloadImageTask(img).execute(dynImgName);
			} else {
				//Try to parse as regexp.
				String fileName = this.figureOutFileToLoad(dynImgName);
				if (fileName==null) {
					Log.d("botox","Failed to find file using "+dynImgName+" as regexp pattern");
				} else {
					Log.d("botox","Filename now: "+fileName);
					dynImgName=fileName;
				}

				setImageFromFile(myContext,img);
			}

			if (scale!=null || scale.length()>0)
				scaleT = ScaleType.valueOf(scale.toUpperCase());
			img.setScaleType(scaleT);
			WF_Widget myWidget= new WF_Widget(blockId,img,isVisible,myContext);	
			myContainer.add(myWidget);
			myContext.registerEventListener(this, EventType.onActivityResult);
		} else {
			if (source==null || sourceE == null) {
				o.addText("");
				o.addCriticalText("Failed to add image with block id "+blockId+" - source is either null or evaluates to null: "+source);				
			}
			o.addText("");
			o.addCriticalText("Failed to add image with block id "+blockId+" - missing container "+container);
		}
		img.setClickable(true);
		img.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showImage();
			}
		});
	}

	private void showImage() {
		Dialog builder = new Dialog(myContext.getContext());
		builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
		builder.getWindow().setBackgroundDrawable(
				new ColorDrawable(android.graphics.Color.TRANSPARENT));
		builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialogInterface) {
				//nothing;
			}
		});

		ImageView imageView = new ImageView(myContext.getContext());
		if (Tools.isURL(dynImgName)) {
			new DownloadImageTask(imageView).execute(dynImgName);
		} else {
			setImageFromFile(myContext,imageView);
		}
		builder.addContentView(imageView, new RelativeLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		builder.show();
	}


	private void setImageFromFile(WF_Context myContext, ImageView img) {
		if (dynImgName==null) {
			Log.e("vortex","no dynimage name in createimageblock... exit");
		}

		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds=true;
		File[] externalStorageVolumes =
				ContextCompat.getExternalFilesDirs(GlobalState.getInstance().getContext(), null);
		File primaryExternalStorage = externalStorageVolumes[0];
		String PIC_ROOT_DIR = primaryExternalStorage.getAbsolutePath() + "/pics/";
		Bitmap bip = BitmapFactory.decodeFile(PIC_ROOT_DIR +dynImgName,options);
		float realW = options.outWidth;
		float realH = options.outHeight;
		if (realW>0) {
			new Thread(new Runnable() {
				public void run() {
					float ratio = realH/realW;
					Display display = myContext.getFragmentActivity().getWindowManager().getDefaultDisplay();
					Point size = new Point();
					display.getSize(size);
					Log.d("botox","Img size "+"realW: "+realW+" realH: "+realH+" screen size "+" x: "+size.x+" y: "+size.y);

					int x = size.x/2;
					int y = size.y/3;
					options.inSampleSize = Tools.calculateInSampleSize(options,x,y);
					Log.d("botox", "insample was "+Tools.calculateInSampleSize(options,x,y));
					options.inJustDecodeBounds = false;
					Bitmap bip = BitmapFactory.decodeFile(PIC_ROOT_DIR+dynImgName,options);
					CreateImageBlock.this.myContext.getFragmentActivity().runOnUiThread(new Runnable() {
						public void run() {
							if(Looper.myLooper() == Looper.getMainLooper())
								Log.d("botox","In UI thread");
							if (bip!=null)
								img.setImageBitmap(bip);
							else
								Log.d("botox","Could not decode image "+dynImgName);
						}
					});

				}
			}).start();


		}
		else {
			Log.d("botox","Did not find picture "+dynImgName);
		}
	}

	private String figureOutFileToLoad(String pattern) {

		String fileName = null;
		File[] externalStorageVolumes =
				ContextCompat.getExternalFilesDirs(GlobalState.getInstance().getContext(), null);
		File primaryExternalStorage = externalStorageVolumes[0];
		String PIC_ROOT_DIR = primaryExternalStorage.getAbsolutePath() + "/pics/";
		File f = new File(PIC_ROOT_DIR);
		if (f.exists() && f.isDirectory() && pattern!=null)

		{
			final Pattern p = Pattern.compile(pattern);
			File[] flists = f.listFiles(new FileFilter() {
				@Override
				public boolean accept(File file) {
					//p.matcher(file.getName()).matches();
					//Log.e("botox", "patternmatch " + p.matcher(file.getName()).matches());

					return p.matcher(file.getName()).matches();

				}
			});

			if (flists!=null && flists.length>0) {
				Log.d("botox","found file matches for pattern "+pattern);
				long max = -1; File fMax=null;
				for (File fl:flists) {
					Log.d("vortex",fl.getName()+" "+fl.lastModified());
					long lm = fl.lastModified();
					if (lm>max) {
						max = lm;
						fMax = fl;
					}
				}
				if (fMax!=null)
					return fMax.getName();
				else
					return flists[0].getName();
			}
		}

		return null;
	}










	class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
		final ImageView bmImage;

		DownloadImageTask(ImageView bmImage) {
			this.bmImage = bmImage;
		}

		protected Bitmap doInBackground(String... urls) {
			String urldisplay = urls[0];
			Bitmap mIcon11 = null;
			try {
				InputStream in = new java.net.URL(urldisplay).openStream();
				mIcon11 = BitmapFactory.decodeStream(in);
			} catch (Exception e) {
				Log.e("Error", e.getMessage());
				e.printStackTrace();
			}
			return mIcon11;
		}

		protected void onPostExecute(Bitmap result) {
			if (result!=null)
				bmImage.setImageBitmap(result);
		}
	}



	@Override
	public void onEvent(Event e) {
		Log.d("botox","Img was taken");
		String fileName = this.figureOutFileToLoad(dynImgName);
		if (fileName!=null)
			dynImgName=fileName;
		setImageFromFile(myContext,img);
	}

	@Override
	public String getName() {
		return "CREATE IMAGE BLOCK ";
	}
}
