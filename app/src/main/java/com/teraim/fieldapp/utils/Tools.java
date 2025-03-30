package com.teraim.fieldapp.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.Numerable.Type;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.VariableCache;
import com.teraim.fieldapp.loadermodule.ConfigurationModule;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.loadermodule.ModuleLoader;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.DbHelper.Selection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StreamCorruptedException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

public class Tools {

	public static void sendMail (Activity ctx,String fileName,String email)
	{
		Log.d("vortex","full name is "+fileName);
		File[] externalStorageVolumes =
				ContextCompat.getExternalFilesDirs(GlobalState.getInstance().getContext(), null);
		File primaryExternalStorage = externalStorageVolumes[0];
		String exportFolder = primaryExternalStorage.getAbsolutePath() + "/export/";
		File exportFile = new File(exportFolder+fileName);
		Uri exportFileURI = FileProvider.getUriForFile(
				ctx,
				"com.teraim.fieldapp.fileprovider", exportFile);
		Intent intent = new Intent (Intent.ACTION_SEND);
		intent.setType("plain/text");
		intent.putExtra (Intent.EXTRA_EMAIL, new String[] {email});
		intent.putExtra(Intent.EXTRA_STREAM, exportFileURI);
		intent.putExtra (Intent.EXTRA_SUBJECT, "Export file "+Constants.getSweDate());
		intent.putExtra (Intent.EXTRA_TEXT, "Log file attached."); // do this so some email clients don't complain about empty body.
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		ctx.startActivity (intent);
	}
	/*
	public static void sendMail(Activity ctx,String filename,String email) {
		File[] externalStorageVolumes =
				ContextCompat.getExternalFilesDirs(ctx, null);
		File primaryExternalStorage = externalStorageVolumes[0];
		String fullName = primaryExternalStorage.getAbsolutePath() + "/export/"+filename;
		Intent intent = new Intent (Intent.ACTION_SEND);
		intent.setType ("plain/text");
		intent.putExtra (Intent.EXTRA_EMAIL, new String[] {email});
		intent.putExtra (Intent.EXTRA_SUBJECT, "Export file "+Constants.getSweDate());
		intent.putExtra (Intent.EXTRA_STREAM, Uri.parse ("file://" + fullName));
		intent.putExtra (Intent.EXTRA_TEXT, "Export data attached."); // do this so some email clients don't complain about empty body.
		ctx.startActivity (intent);
	}
*/
    public static int eraseFolder(String s) {
		Log.d("franzon","Erasing cache folder");
		File dir = new File(s);
		if (dir.isDirectory())
		{
			String[] children = dir.list();
			if (children!=null && children.length>0) {
                for (String aChildren : children) {
                    new File(dir, aChildren).delete();
                }
				Log.d("vortex", "erased " + children.length + " files from cache");
				return children.length;
			}
		}
		return 0;
	}

	public static String printIfNotNull(String s, String parameter) {
		if (parameter==null)
			return "";
		return s+parameter;
	}

	public static String convertToKeyPairs(Map<String, String> keyChain) {
		if (keyChain==null||keyChain.isEmpty())
			return null;
		StringBuilder result = new StringBuilder();
		int i=1;
		for (String key:keyChain.keySet()) {
			result.append(key).append("=").append(keyChain.get(key)).append(i < keyChain.keySet().size() ? "," : "");
			i++;
		}
		return result.toString();
	}

    public static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return sdf.format(new Date());
    }

    public static String getTimeStampDetails(long timestamp, boolean minimal) {
		//Divide by 1000
		timestamp=System.currentTimeMillis()-timestamp;
		long inSecs = timestamp/1000;
		long inHours = Math.round(inSecs/3600);
		long days = inHours / 24;
		long hours = inHours % 24;
		long remain = inSecs - inHours*3600;
		long mins = Math.round(remain/60);
		long secs = remain - mins*60;
		if (minimal) {
			if (days > 0)
				return days + "d";
			if (hours > 0)
				return hours + "h";
			if (mins > 0)
				return mins + "m";
			return secs + "s";
		} else
			return days+"d "+hours+"h "+mins+"m "+secs+"s";

	}

	public static String setColorFromTime(long timestamp) {
		long age = System.currentTimeMillis() - timestamp;
		return isOverAnHourOld(age) ? isOverAnHourColor :
				isOverHalfAnHourOld(age) ? isOverHalfAnHourColor :
						isOverAQuarterOld(age) ? isOverAQuarterColor :
								isFreshColor;
	}

	private static final long AnHour = 3600 * 1000;
	private static final long HalfAnHour = AnHour/2;
	private static final long QuarterOfAnHour = AnHour/4;

	private static boolean isOverAnHourOld(long latestUpdate) { return latestUpdate > AnHour; }
	private static boolean isOverHalfAnHourOld(long latestUpdate) {
		return latestUpdate > HalfAnHour;
	}
	private static boolean isOverAQuarterOld(long latestUpdate) {
		return latestUpdate > QuarterOfAnHour;
	}

	private final static String isFreshColor = "#7CFC00";
	private final static String isOverAnHourColor = "#D3D3D3";
	private final static String isOverHalfAnHourColor = "#050FF5";
	private final static String isOverAQuarterColor = "#FFFA0A";

	public enum Unit {
		percentage,
		dm,
		m,
		cm,
		cm2,
		meter,
		cl,
		ml,
		m2,
		dl2,
		cl2,
		m3,
		dm3,
		deg,
		mm,
		st,
		m2_ha,
		antal,
		antal_ha,
		år,
		nd

	}


	public static boolean writeToFile(String filename,String text) {
		PrintWriter out;
		try {
			out = new PrintWriter(filename);
			out.println(text);
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static void witeObjectToFile(Object object, String filename) throws IOException {

		Log.d("nils","Writing frozen object to file "+filename);
		ObjectOutputStream objectOut = null;
		try {
			FileOutputStream fileOut = new FileOutputStream(filename);
			objectOut = new ObjectOutputStream(fileOut);
			objectOut.writeObject(object);
			fileOut.getFD().sync();


		} finally {
			if (objectOut != null) {
				try {
					objectOut.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 *
	 * @param fileName abc
	 * @return abc
	 * @throws IOException
	 * @throws StreamCorruptedException
	 * @throws ClassNotFoundException
	 *
	 *
	 */

	public static Object readObjectFromFile(String fileName) {
		ObjectInputStream objectIn = null;
		Object object = null;

		try {
			FileInputStream fileIn = new FileInputStream(fileName);
			objectIn = new ObjectInputStream(fileIn);
			object = objectIn.readObject();

		} catch (Exception e) {
			Log.d("vortex","thaw failed");
			object=null;
		}  finally {
			if (objectIn != null) {
				try {
					objectIn.close();
				} catch (IOException e) {
					// do nowt
				}
			}
		}

		return object;
	}


	public static void readObjectFromFileAsync(String filename, final ConfigurationModule caller, final ModuleLoader callBack) {


		class FileLoader extends AsyncTask<String, Void, Object> {

			//fileName to load
			protected Object doInBackground(String... params) {
				String fileName = params[0];

				ObjectInputStream objectIn = null;
				Object object = null;

				try {
					File file = new File(fileName);
					FileInputStream fileIn = new FileInputStream(file);
					int fileLength = (int)file.length();
					Integer totalBlocks = fileLength/4096;
					byte[] buf = new byte[4096];
					int read = 0,blockC=0;
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
					while((read = fileIn.read(buf))>0) {
						callBack.onUpdate(blockC++,totalBlocks);
						outputStream.write(buf,0,read);
					}
					//create object.
					ByteArrayInputStream bis = new ByteArrayInputStream(outputStream.toByteArray());
					objectIn = new ObjectInputStream(bis);
					object = objectIn.readObject();

				} catch (Exception e) {
                    Log.d("vortex","thaw failed");
                    e.printStackTrace();
                   return null;
				}  finally {
					if (objectIn != null) {
						try {
							objectIn.close();
						} catch (IOException e) {
							Log.d("vortex","thaw failed - ");
							e.printStackTrace();
							return null;
						}
					}
				}
				Log.d("vortex","returning thawed object");
				return object;


			}




			protected void onPostExecute(Object result) {

				Log.d("vortex", "Object thawed ");
                caller.setEssence(result);
				caller.setThawActive(false);
                LoadResult loadResult = new LoadResult(caller,result==null?
                        LoadResult.ErrorCode.thawFailed:LoadResult.ErrorCode.thawed);
				callBack.onFileLoaded(loadResult);
			}
		}
		new FileLoader().execute(filename);
	}





	//This cannot be part of Variable, since Variable is an interface.

	public static Type convertToType(String text) {
		Type[] types = Type.values();
		//Special cases
		if (text.equals("number"))
			return Type.NUMERIC;
		for (int i =0;i<types.length;i++) {
			if (text.equalsIgnoreCase(types[i].name()))
				return types[i];

		}
		return null;
	}

	public static Unit convertToUnit(String unit) {
		if (unit == null) {
			Log.d("unit","translates to undefined");
			return Unit.nd;
		}
		Unit[] units = Unit.values();
		if (unit.equals("%"))
			return Unit.percentage;
		for (int i =0;i<units.length;i++) {
			if (unit.equalsIgnoreCase(units[i].name()))
				return units[i];
		}
		return Unit.nd;
	}

	private static void createFoldersIfMissing(File file) {
		final File parent_directory = file.getParentFile();

		if (null != parent_directory)
		{
			parent_directory.mkdirs();
		}
	}

	private static final Map<Rect,Integer> inSampleMemory  = new HashMap<Rect,Integer>();

	public static Bitmap getScaledImageRegion(Context ctx,String fileName, Rect r) {
		BitmapRegionDecoder decoder = null;
		int inSampleSize=1;

		try {



			Integer memorized = inSampleMemory.get(r);
			final BitmapFactory.Options options = new BitmapFactory.Options();
			decoder = BitmapRegionDecoder.newInstance(fileName, true);

			if (memorized!=null) {
				Log.d("vortex","trying memorized insample: "+memorized);
				inSampleSize=memorized;
			} else {
				WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
				Display display = wm.getDefaultDisplay();
				Point size = new Point();
				display.getSize(size);
				inSampleSize = Math.round((float) r.width() / (float) size.x);
				Log.d("vortex","r width: "+r.width()+" screen width: "+size.x);
				Log.d("vortex","trying insampleSize "+inSampleSize);
			}



			options.inSampleSize = inSampleSize;
			Bitmap piece = decoder.decodeRegion(r,options);
			//If i get here, it worked.
			inSampleMemory.put(r,inSampleSize);
			return piece;
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		} catch (OutOfMemoryError ex) {
			Log.e("vortex","out of memory! trying smaller image");
			System.gc();
			if (decoder!=null)
				decoder.recycle();
			inSampleMemory.put(r,inSampleSize+2);
			Log.d("vortex","trying insampleSize "+(inSampleSize+2));
			return getScaledImageRegion(ctx,fileName,r);
		}

	}


	//Scales an image region to a size that can be displayed.
	public static Bitmap getScaledImageRegionOld(Context ctx,String fileName, Rect r) {
		BitmapRegionDecoder decoder = null;


		try {

			decoder = BitmapRegionDecoder.newInstance(fileName, true);
			final BitmapFactory.Options options = new BitmapFactory.Options();
			decoder.decodeRegion(r,options);
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
		Log.d("vortex","w h "+decoder.getWidth()+","+decoder.getHeight());
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds=true;
		Bitmap piece;
		decoder.decodeRegion(r,options);
		int realW = options.outWidth;
		int realH = options.outHeight;
		Log.d("vortex","Wp Wh: "+realW+","+realH);
		DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();
		//height is then the ratio times this..
		int tHeight = (int) ((double) metrics.widthPixels *.66f);
		//use target values to calculate the correct inSampleSize
		options.inSampleSize = Tools.calculateInSampleSize(options, (int) (double) metrics.widthPixels, tHeight);
		Log.d("nils"," Calculated insamplesize "+options.inSampleSize);
		//now create real bitmap using insampleSize
		options.inJustDecodeBounds = false;
		Log.d("vortex","stime: "+System.currentTimeMillis()/1000);
		piece = decoder.decodeRegion(r,options);
		Log.d("vortex","ptime: "+System.currentTimeMillis()/1000);
		Log.d("vortex","piece w: b: "+piece.getWidth()+","+piece.getRowBytes());
		return piece;
	}
	public static Bitmap getScaledImage(Context ctx,String fileName) {
		//Try to load pic from disk, if any.
		//To avoid memory issues, we need to figure out how big bitmap to allocate, approximately
		//Picture is in landscape & should be approx half the screen width, and 1/5th of the height.
		//First get the ration between h and w of the pic.
		final BitmapFactory.Options options = new BitmapFactory.Options();
		if (fileName == null) {
			return null;
		}
		options.inJustDecodeBounds=true;
		BitmapFactory.decodeFile(fileName,options);

		//there is a picture..
		int realW = options.outWidth;
		int realH = options.outHeight;


		//check if file exists
		if (realW>0) {
			double ratio = realH/realW;
			//Height should not be higher than width.
			Log.d("nils", "realW realH"+realW+" "+realH);

			//Find out screen size.

			DisplayMetrics metrics = ctx.getResources().getDisplayMetrics();

			//Target width should be about half the screen width.

			//height is then the ratio times this..
			int tHeight = (int) ((double) metrics.widthPixels *ratio);

			//use target values to calculate the correct inSampleSize
			options.inSampleSize = Tools.calculateInSampleSize(options, (int) (double) metrics.widthPixels, tHeight);

			Log.d("nils"," Calculated insamplesize "+options.inSampleSize);
			//now create real bitmap using insampleSize

			options.inJustDecodeBounds = false;
			Log.d("nils","Filename: "+fileName);
			return BitmapFactory.decodeFile(fileName,options);


		}
		else {
			Log.e("Vortex","Did not find picture "+fileName);
			//need to set the width equal to the height...
			return null;
		}
	}

	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
														 int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res, resId, options);
	}


	public static int calculateInSampleSize(
			BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			// Calculate ratios of height and width to requested height and width
			final int heightRatio = Math.round((float) height / (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);

			// Choose the smallest ratio as inSampleSize value, this will guarantee
			// a final image with both dimensions larger than or equal to the
			// requested height and width.
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}

		return inSampleSize;
	}




	/*********************************************************
	 *
	 * File Data Parsers.
	 */



	public static Map<String,String> copyKeyHash(Map<String,String> orig) {
		if (orig==null)
			return null;
		Map<String,String> res = new HashMap<String,String>();
		for (String key:orig.keySet()) {
			res.put(key, orig.get(key));
		}
		return res;
	}


	public static Map<String,String> cutKeyMap(String columns, Map<String,String> fullHash) {
		if (columns.isEmpty())
			return null;
		//Log.d("cutkey","cols: "+columns+"fhash: "+((fullHash==null)?"null":fullHash.toString()));
		Map<String,String> ret = new HashMap<String,String>();
		if (fullHash == null) {
			Log.e("vortex","Hash null - so returning empty in cutkeymap. Columns: "+columns);

		} else {
			String[] keys = columns.split("\\|");
			for (String key:keys) {
				String value = fullHash.get(key);
				if(value!=null)
					ret.put(key, value);
			}
		}
		return ret;
	}


	public static Map<String,String> createKeyMap(String ...parameters) {

		if ((parameters.length & 1) != 0 ) {
			Log.e("fargo","createKeyMap needs an even number of arguments: "+ Arrays.toString(parameters));
			return null;
		}
		String colName;
		String colValue;
		Map<String,String> ret = new HashMap<String,String>();
		for (int i=0;i<parameters.length;i+=2) {
			colName = parameters[i];
			colValue = parameters[i+1];
			if (colName != null && colValue!=null && !colValue.equals("null"))
				ret.put(colName,colValue);

		}
		return ret;
	}

	public static String getPrintedUnit(Unit unit) {
		if (unit == Unit.percentage)
			return "%";
		if (unit == Unit.nd || unit == null)
			return "";
		else
			return unit.name();
	}

	public static boolean doesFileExist(String folder, String fileName) {
		Log.d("vortex","Looking for file "+fileName+ "in folder "+folder);
		File f = new File(folder,fileName);
		return (f.exists() && !f.isDirectory());
	}
/*
	public static boolean isNetworkAvailable(Context ctx) {
		ConnectivityManager connectivityManager 
		= (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
	}
*/

	public static boolean isNumeric(Object num)
	{
		//Log.d("vortex","isnumeric "+num);
		if (num == null)
			return false;
		if (num instanceof Double || num instanceof Float || num instanceof Integer)
			return true;
		if (num instanceof String) {
			String str = (String)num;
			if (str==null||str.length()==0)
				return false;
			int i=0;
			//Log.d("vortex","isnumeric? str "+str);
			for (char c : str.toCharArray())
			{

				if (!Character.isDigit(c)&& c!='.' && c!='E' && c!='-') {
					return false;
				}
				i++;
			}
			//Log.d("vortex","isnumeric yes");
			return true;
		} else {
			Log.d("fenris","isNumeric returns false...not a string: "+num.getClass()+" "+num);
			return false;
		}
	}

	public static String printSelectionArgs(String[] selectionArgs) {
		if (selectionArgs == null)
			return "NULL";
		String ret = "";
		for (int i = 0; i < selectionArgs.length; i++)
			ret += ("[" + i + "]: " + selectionArgs[i] + " ");
		return ret;
	}


	public static boolean isVersionNumber(String str)
	{
		if (str==null||str.length()==0)
			return false;
		for (char c : str.toCharArray())
		{
			if (!Character.isDigit(c) && c!='.')
				return false;
		}
		return true;
	}
	//Create a map of references to variables. 



	public static Bitmap drawableToBitmap (Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable)drawable).getBitmap();
		}

		Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}

	public static String generateUUID(){
        final String uuid = UUID.randomUUID().toString().replace("-", "");
        Log.d("fenris","uuid = " + uuid);
        return uuid;
    }
	public static String[] generateList(Variable variable) {
		String[] opt=null;
		GlobalState gs = GlobalState.getInstance();
		VariableConfiguration al = gs.getVariableConfiguration();
		VariableCache vc = gs.getVariableCache();

		LoggerI o = gs.getLogger();
		List<String>listValues = al.getListElements(variable.getBackingDataSet());
		Log.d("nils","Found dynamic list definition for variable "+variable.getId()+": "+listValues);

		if (listValues!=null&&listValues.size()>0) {
			String [] columnSelector = listValues.get(0).split("=");
			String[] column=null;
			boolean error = false;
			if (columnSelector[0].equalsIgnoreCase("@col")) {
				Log.d("nils","found column selector");
				//Column to select.
				String dbColName = gs.getDb().getDatabaseColumnName(columnSelector[1]);
				if (dbColName!=null) {
					Log.d("nils","Real Column name for "+columnSelector[1]+" is "+dbColName);
					column = new String[1];
					column[0]=dbColName;
				} else {
					Log.d("nils","Column referenced in List definition for variable "+variable.getLabel()+" not found: "+columnSelector[1]);
					o.addRow("");
					o.addRedText("Column referenced in List definition for variable "+variable.getLabel()+" not found: "+columnSelector[1]);
					error = true;
				}
				if (!error) {
					//Any other columns part of key?
					Map<String,String>keySet = new HashMap<String,String>();
					if (listValues.size()>1) {
						//yes..include these in search
						Log.d("nils","found additional keys...");
						String[] keyPair;
						for (int i=1;i<listValues.size();i++) {
							keyPair = listValues.get(i).split("=");
							if (keyPair!=null && keyPair.length==2) {
								String valx=vc.getVariableValue(null,keyPair[1]);
								if (valx!=null)
									keySet.put(keyPair[0], valx);
								else {
									Log.e("nils","The variable "+keyPair[1]+" used for dynamic list "+variable.getLabel()+" is not returning a value");
									o.addRow("");
									o.addRedText("The variable "+keyPair[1]+" used for dynamic list "+variable.getLabel()+" is not returning a value");
								}
							} else {
								Log.d("nils","Keypair error: "+ Arrays.toString(keyPair));
								o.addRow("");
								o.addRedText("Keypair referenced in List definition for variable "+variable.getLabel()+" cannot be read: "+ Arrays.toString(keyPair));
							}
						}

					} else
						Log.d("nils","no additional keys..only column");
					Selection s = gs.getDb().createCoulmnSelection(keySet);
					List<String[]> values = gs.getDb().getUniqueValues(column, s);
					if (values !=null) {
						Log.d("nils","Got "+values.size()+" results");
						//Remove duplicates and sort.
						SortedSet<String> ss = new TreeSet<String>(new Comparator<String>(){
							public int compare(String a, String b){
								return Integer.parseInt(a)-Integer.parseInt(b);
							}}
						);
						String S;
						for (int i = 0; i<values.size();i++) {
							S = values.get(i)[0];
							if (Tools.isNumeric(S))
								ss.add(S);
							else
								Log.e("vortex","NonNumeric value found: ["+S+"]");
						}
						opt = new String[ss.size()];
						int i = 0;
						Iterator<String> it = ss.iterator();
						while (it.hasNext()) {
							opt[i++]=it.next();
						}
					}
				} else {
					Log.e("vortex","CONFIG ERROR");
					opt = new String[]{"Config Error...please check your list definitions for variable " + variable.getLabel()};
				}

			} else
				Log.e("nils","List "+variable.getId()+" has too few parameters: "+listValues.toString());
		} else
			Log.e("nils","List "+variable.getId()+" has strange parameters: "+listValues.toString());
		return opt;
	}











	/*
	public static SpinnerDefinition thawSpinners(Context myC) {		
		SpinnerDefinition sd=null;
		Log.d("nils","NO NETWORK. Loading file spinner def");
		sd = (SpinnerDefinition)Tools.readObjectFromFile(myC,Constants.CONFIG_FILES_DIR+Constants.WF_FROZEN_SPINNER_ID);		
		if (sd==null) 
			Log.d("vortex","No frozen Spinner definition");
		else
			Log.d("nils","Thawspinners called. Returned "+sd.size()+" spinners");
		return sd;

	}
	 */
	//Check if two keys are equal
	public static boolean sameKeys(Map<String, String> m1,
								   Map<String, String> m2) {
		if (m1.size() != m2.size())
			return true;
		for (String key: m1.keySet()) {
			//Log.d("nils","Key:"+key+" m1: "+(m1==null?"null":m1.toString())+" m2: "+(m2==null?"null":m2.toString()));
			if (m1.get(key)==null&&m2.get(key)==null)
				continue;
			if ((m1.get(key)==null || m2.get(key)==null)||!m1.get(key).equals(m2.get(key)))
				return true;
		}
		//Log.d("nils","keys equal..no header");
		return false;
	}

	public static String[] split(String input) {
		List<String> result = new ArrayList<String>();
		int start = 0;
		boolean inQuotes = false;
		for (int current = 0; current < input.length(); current++) {
			if (input.charAt(current) == '\"') inQuotes = !inQuotes; // toggle state
			boolean atLastChar = (current == input.length() - 1);
			if(atLastChar) {
				if (input.charAt(current) == ',') {
					if (start==current)
						result.add("");
					else
						result.add(input.substring(start,current));
					result.add("");
				} else {
					//Log.d("nils","Last char: "+input.charAt(current));
					result.add(input.substring(start));
				}
			}
			else if (input.charAt(current) == ',' && !inQuotes) {
				String toAdd = input.substring(start, current);
				//Log.d("Adding",toAdd);

				result.add(toAdd);
				start = current + 1;
			}
		}
		if (result.size()==0)
			return new String[]{input};
		else
			return result.toArray(new String[0]);

	}

	public static String removeStartingZeroes(String value) {
		if (value == null || value.length()<=1 || !value.startsWith("0"))
			return value;
		return removeStartingZeroes(value.substring(1));
	}


	public static void restart(Activity context) {
		Log.d("vortex","restarting...");
		if (GlobalState.getInstance()!=null)
			GlobalState.destroy();
		android.app.FragmentManager fm = context.getFragmentManager();
		for(int i = 0; i < fm.getBackStackEntryCount(); ++i) {
			fm.popBackStack();
		}
		Intent intent = new Intent(context, Start.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
		context.finish();
	}

	public static Map<String,String> findKeyDifferences(Map<String, String> k1,
														Map<String, String> k2) {
		Map<String,String> res = new HashMap<String,String>();
		Map<String, String> longer,shorter;
		//if (shorter!=null && longer!=null){
		//	Log.d("vortex","Longer: "+longer.toString());
		//	Log.d("vortex","shorter: "+shorter.toString());
		//}

		if (k1!=null &&!k1.isEmpty()) {
			//If k2 is null, the diff is everything.
			if (k2==null||k2.isEmpty())
				return copyKeyHash(k1);
			if (k1.size()>k2.size()) {
				longer = k1;
				shorter = k2;
			}
			if (k1.size()<k2.size()) {
				longer = k2;
				shorter = k1;
			}
			//same.
			else
				return null;
		} else {
			//both are null
			if (k2==null||k2.isEmpty())
				return null;
				//else k2 is the difference.
			else
				return copyKeyHash(k2);
		}
		Set<String> sK = shorter.keySet();
		for (String key:longer.keySet()) {
			if (!sK.contains(key)) {
				//Log.d("vortex","KEY DIFFERENT:"+key);
				res.put(key, longer.get(key));
			}
		}
		if (res.isEmpty())
			return null;
		return res;
	}



	public static boolean isURL(String source) {
		if (source==null)
			return false;
		return source.startsWith("http") && !source.startsWith("www");
	}

	/*
	public static String parseString(String varString) {
		return parseString(varString, GlobalState.getInstance().getCurrentKeyHash());
	}
	 */

	/*
	public static String parseString(String varString, Map<String,String> keyHash) {

		if (varString == null||varString.isEmpty())
			return "";
		if (!varString.contains("["))
			return varString;
		if (keyHash!=null)
			Log.d("vortex","Keyhash: "+keyHash.toString());
		int cIndex=0;
		String res="";
		do {
			int hakeS = varString.indexOf('[', cIndex)+1;
			if (hakeS==0)
				break;
			int hakeE = varString.indexOf(']', hakeS);
			if (hakeE==-1) {
				Log.e("vortex","missing end bracket in parseString: "+varString);
				GlobalState.getInstance().getLogger().addRow("");
				GlobalState.getInstance().getLogger().addRedText("missing end bracket in parseString: "+varString);
				break;
			}
			//Log.e("vortex","PARSESTRING: "+varString.substring(hakeS, hakeE));
			String interPS = interpret(varString.substring(hakeS, hakeE),keyHash);
			String preS = varString.substring(cIndex, hakeS-1);
			res = res+preS+interPS;
			//Log.d("vortex","Res is now "+res);
			cIndex=hakeE+1;
		} while(cIndex<varString.length());
		String postS = varString.substring(cIndex,varString.length());
		Log.d("vortex","Parse String returns "+res+postS);
		return res+postS;
	}
	 */
	/*
	private static String interpret(String varString, Map<String,String> keyHash) {
		final RuleExecutor re = GlobalState.getInstance().getRuleExecutor();
		List<TokenizedItem> tokenized = re.findTokens(varString, null, keyHash);
		SubstiResult x=null;
		if (tokenized!=null)
			x = re.substituteForValue(tokenized, varString, true);
		if (x!=null) {
			String res = x.result;
			return res;
		}
		else
			return varString;
	}
	 */

	public static void onLoadCacheImage(String serverFileRootDir, final String fileName, final String cacheFolder, WebLoaderCb cb) {
		final String fullPicURL = serverFileRootDir+fileName;
		new DownloadTask(cb).execute(fileName,fullPicURL,cacheFolder);

	}
	public static void preCacheImage(String serverFileRootDir, final String fileName, final String cacheFolder, final LoggerI logger) {

		onLoadCacheImage (serverFileRootDir,fileName,cacheFolder,new WebLoaderCb(){
			@Override
			public void loaded(Boolean result) {
				if (logger!=null) {

					if (result) {
						logger.addRow("Succesfully cached " + fileName);
						Log.d("vortex","Cached "+fileName);
					}
					else {
						logger.addRow("");
						logger.addRedText("Failed to cache "+fileName);
						Log.e("vortex","Failed to cache "+fileName);
					}
				}
			}
			@Override
			public void progress(int bytesRead) {

			}
		});



	}

	public interface WebLoaderCb {

		//called when done.
		void loaded(Boolean result);
		//called every time 1kb has been read.
		void progress(int bytesRead);
	}


	private static class DownloadTask extends AsyncTask<String, Void, Boolean> {
		final WebLoaderCb cb;

		DownloadTask(WebLoaderCb cb) {
			this.cb=cb;
		}

		protected Boolean doInBackground(String... fileNameAndUrl) {
			final String protoH = "https://";
			if (fileNameAndUrl==null||fileNameAndUrl.length<3) {
				Log.e("vortex","filename or url name corrupt in downloadtask");
				return false;
			}
			String fileName = fileNameAndUrl[0].trim();
			String url = fileNameAndUrl[1].trim();
			String folder = fileNameAndUrl[2];
			if (!url.startsWith(protoH))
				url=protoH+url;
			//If url is used as name, remove protocol.
			if (fileName.startsWith(protoH))
				fileName = fileName.replace(protoH, "");
			if (fileName.contains("|")) {
				Log.e("vortex","Illegal filename, cannot cache: "+fileName);
				return false;
			}

			fileName = fileName.replace("/", "|");
			fileName = folder+fileName;
			File file = new File(fileName);

			//File already cached
			if(file.exists()) {
				Log.d("vortex","NO cache - file exists");
				return true;
			}

			Tools.createFoldersIfMissing(new File(fileName));
			boolean success = false;
			BufferedInputStream in = null;
			BufferedOutputStream fout = null;

			try {
				Log.d("vortex","urlString: "+url);
				URL _url = new URL(url);
				URLConnection ucon = _url.openConnection();
				ucon.setConnectTimeout(5000);
				in = new BufferedInputStream(ucon.getInputStream());
				fout = new BufferedOutputStream(new FileOutputStream(fileName));

				final byte data[] = new byte[4096];
				int count;
				while ((count = in.read(data, 0, 4096)) != -1) {
					fout.write(data, 0, count);
					cb.progress(count);
				}
				success = true;
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			} finally {

				if (in != null && fout != null) {
					try {
						in.close();
						fout.close();
						if (!success) {
							Log.d("vortex","caching failed...deleting temp file");
							file.delete();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
			}
			return true;
		}

		protected void onProgressUpdate(Integer... progress) {
			//This method runs on the UI thread, it receives progress updates
			//from the background thread and publishes them to the status bar
			cb.progress(progress[0]);
		}

		protected void onPostExecute(Boolean result) {

			cb.loaded(result);
		}
	}


	public static void saveUrl(final String filename, final String urlString, WebLoaderCb cb)
			throws IOException {
		BufferedInputStream in = null;
		FileOutputStream fout = null;
		try {
			Log.d("vortex","urlString: "+urlString);
			in = new BufferedInputStream(new URL(urlString).openStream());
			fout = new FileOutputStream(filename);

			final byte data[] = new byte[1024];
			int count;
			while ((count = in.read(data, 0, 1024)) != -1) {
				fout.write(data, 0, count);

				cb.progress(count);
			}
		} finally {
			if (in != null) {
				in.close();
			}
			if (fout != null) {
				fout.close();
			}
		}
	}

	public static File getCachedFile(String fileName, String folder) {

		fileName = folder+fileName;
		File f = new File(fileName);
		Log.d("vortex", "getCached: " + fileName);
		if (f.exists()) {

			return f;
		}
		else
			return null;
	}

	public static void printErrorToLog(LoggerI o, Exception e, String blockP) {
		o.addRow("");
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		if (blockP != null) {
			o.addRedText("Error executing BLOCK with "+blockP);
			o.addRow("");
		}
		if (e!=null) {
			e.printStackTrace(pw);
			e.printStackTrace();
		}
		o.addRedText(sw.toString());

	}

	public static boolean existingTimestampIsMoreRecent(String existingTime, String newTime) {
		if (newTime==null || existingTime == null) {
			Log.e("vortex","A timestamp was null in more recent!");
			return true;
		}
		try {
			long existingTimeStamp = Long.parseLong(existingTime);
			long newTimeStamp = Long.parseLong(newTime);
			return (existingTimeStamp>=newTimeStamp);
		} catch (NumberFormatException e) {
			Log.e("vortex","A timestamp was not a number in more recent!");
			return true;
		}

	}

	public static Object bytesToObject(byte[] inB) {
		ByteArrayInputStream bis = new ByteArrayInputStream(inB);
		ObjectInput in = null;
		Object o =null;
		try {
			in = new ObjectInputStream(bis);
			o = in.readObject();

		} catch (IOException e) {

			e.printStackTrace();
			return null;
		} catch (ClassNotFoundException e) {

			e.printStackTrace();
			return null;
		} finally {
			try {
				bis.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		if (o==null)
			Log.d("vortex","returning null in bytestoobject for "+ Arrays.toString(inB) +" with l "+inB.length+" inBS "+ Arrays.toString(inB));
		return o;
	}

	public static String fixedLengthString(String string, int length) {
		if (string==null)
			return null;
		if (string.length()>length) {
			string = string.substring(0, length - 2);
			string +="..";
		}
		return String.format("%1$-" + length + "s", string);
		//return String.format("%1$"+length+ "s", string);
	}

	public static String server(String serverUrl) {
		assert serverUrl != null;
		if (!serverUrl.matches("^(https?)://.*$"))
			if (serverUrl.length() < 4)
				serverUrl = Constants.DEFAULT_SERVER_URI;
			else {
				serverUrl = "https://" + serverUrl;


			}
		if (!serverUrl.endsWith("/"))
			serverUrl += "/";
		return serverUrl;
	}

	public static int getColorResource(Context ctx, String colorName) {
		return Tools.getColorResource(ctx,colorName,R.color.black);
	}

	public static int getColorResource(Context ctx, String colorName, int defaultColor) {

		if(colorName !=null) {
			if (colorName.startsWith("#"))
				return Color.parseColor(colorName);
			else if (colorName.equalsIgnoreCase("black"))
			    return Color.BLACK;
            else if (colorName.equalsIgnoreCase("white"))
                return Color.WHITE;
            else if (colorName.equalsIgnoreCase("green"))
                return Color.GREEN;
            else if (colorName.equalsIgnoreCase("red"))
                return Color.RED;
            else if (colorName.equalsIgnoreCase("blue"))
                return Color.BLUE;
            else if (colorName.equalsIgnoreCase("Lightgray"))
            	return Color.parseColor("#D3D3D3");
			try {
				int resourceId = ctx.getResources().getIdentifier(colorName.toLowerCase(), "color", ctx.getPackageName());
				return ctx.getColor(resourceId);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		Log.e("plax","Color "+colorName+" not known...returning default");
		return ctx.getColor(defaultColor);
	}

	public static String getMajorVersion(String urlString) {
		String ss = null;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(new URL(urlString).openStream())));
			String _a = reader.readLine();
			if (_a != null) {
				String vs = reader.readLine();
				Log.d("VEXXOR",vs);
				int _av = vs.indexOf("app_version");
				int _s = vs.indexOf('\"',_av)+1;
				int _e = vs.indexOf('\"',_s);
				Log.d("VEXXOR","_av "+_av+"_s "+_s+"_e "+_e);
				ss = vs.substring(_s,_e);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ss;
	}


}

