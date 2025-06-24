package com.teraim.fieldapp.non_generics;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.loadermodule.ConfigurationModule;
import com.teraim.fieldapp.loadermodule.configurations.GISListConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.GisObjectConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.GroupsConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.SpinnerConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.VariablesConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.WorkFlowBundleConfiguration;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.ui.AsyncLoadDoneCb;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.PersistenceHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class Constants {


    public static final String VORTEX_VERSION = "10.0";

    public final static String DEFAULT_APP = "Vortex";
    public static final String DEFAULT_SERVER_URI = "https://www.teraim.com/";

    //public static final String EXPORT_SERVER = "https://rlo.slu.se/api/v1/fieldpad";

    public static final String DEFAULT_EXPORT_SERVER = "https://synkserver.net/";
    public static final String TIMESTAMP_SYNC_RECEIVE = "timestamp_receive";
    public static final String TIMESTAMP_SYNC_SEND = "timestamp_send";
    public static final String TIMESTAMP_LATEST_SUCCESFUL_SYNC = "timestamp_latest_sync" ;
    public static final String TIMESTAMP_CURRENT_SEQUENCE_NUMBER = "timestamp_seq_no";

    //String constants

    //Remember to always add system root path before any app specific path!

    public final static String HISTORICAL_TOKEN_IN_XML = "*HISTORICAL*";

    public static final String HISTORICAL_TOKEN_IN_DATABASE = "H";

    public static final String TEMP_BARCODE_IMG_NAME = "tmpbar" ;
    public static final String DEFAULT_IMG_FORMAT = "jgw";
    //Update interval in seconds for location updates.
    public static final int LOCATION_UPDATE_INTERVAL = 10;
    public static final String RELOAD_DB_MODULES = "reload_database_modules";

    public static String UNDEFINED = "undefined";

    public static final String Color_Pressed="#4682B4";

    public static final String SYNC_ID = "SYNX";

    private static final UUID BLUE_UID = null;


    //Static methods
    public static String compassToPicName(int compass) {
        return (compass==0?"vast":(compass==1?"norr":(compass==2?"syd":(compass==3?"ost":null))));
    }


    //Static constants
    public static final long MS_MONTH = 2_629_746_000L;
    public static final String SLU_URL = "https://arbetsplats.slu.se/sites/srh/Landskapsanalys/Faltportal/default.aspx";
    public static final String STATUS_HIGH_PRIORITY = "-1";
    public static final String STATUS_INITIAL = "0";
    public static final String STATUS_STARTAD_MEN_INTE_KLAR = "1";
    public static final String STATUS_STARTAD_MED_FEL = "2";
    public static final String STATUS_AVSLUTAD_EXPORT_MISSLYCKAD = "3";
    public static final String STATUS_AVSLUTAD_EXPORTERAD = "4";
    public static final String STATUS_KLAR_I_DB = "100";
    public static final int MAX_NILS_LINJER = 12;
    public static final String NORR = "NORR";
    public static final String SYD = "SYD";
    public static final String OST = "OST";
    public static final String VAST = "VAST";
    public static final String AVST = "AVST";
    public static final String SMA = "SMA";
    public static final String NOT_NULL = "*NN*";

    //Name of the special variable group used for status variables
    public static final String STATUS_VARIABLES_GROUP_NAME = "STATUS";

    public static final String NO_DEFAULT_VALUE = "*NULL*";
    public static UUID getmyUUID() {
		/*
		String myC = getDeviceColor();
		if (myC.equals(nocolor()))
			return null;
		else if (myC.equals(red()))
			return RED_UID;
		else
		 */
        return BLUE_UID;
    }

    public static final int MIN_ABO = 50;

    public static final int MAX_ABO = 99;


    public static boolean isAbo(int pyID) {
        return pyID>=Constants.MIN_ABO && pyID<=Constants.MAX_ABO;
    }

    //Static Time providers.


    public static String getTimeStamp() {
        return getYear()+getMonth()+getDayOfMonth()+"_"+getHour()+"_"+getMinute();
    }

    public static String getYear() {
        return Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
    }

    public static String getWeekNumber() {
        return Integer.toString(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
    }

    public static String getMonth() {
        return Integer.toString(Calendar.getInstance().get(Calendar.MONTH)+1);
    }

    public static String getDayOfMonth() {
        return Integer.toString(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
    }

    public static String getHour() {
        return Integer.toString(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
    }

    public static String getMinute() {
        return Integer.toString(Calendar.getInstance().get(Calendar.MINUTE));
    }
    public static String getSecond() {
        return Integer.toString(Calendar.getInstance().get(Calendar.SECOND));
    }

    @SuppressLint("SimpleDateFormat")
    public static String getSweDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date());
    }

    public static final String GLOBAL_PREFS = "GlobalPrefs";

    public final static int VAR_PATTERN_ROW_LENGTH = 11;


    public static final String VariableSeparator = ":";

    public final static int TAKE_PICTURE = 133;
    public final static int QR_SCAN_REQUEST = 1111;


    public static final String GIS_LIST_FILE_NAME = "content.txt";


    public static final String GIS_CONFIG_WEB_FOLDER = "gis_objects";


    public static final String BLUETOOTH_NAME = "vortex";

    public static final boolean FreeVersion = false;

    public static final String BACKUP_FILE_NAME = "backup";

    //Backup if data older than 24h
    public static final long BACKUP_FREQUENCY = 86_400_000;

    public static final String SyncDataURI = "https://synkserver.net/synkserv/SynkServ";
    public static final String SynkStatusURI =      "https://synkserver.net";

    public static List<ConfigurationModule> getCurrentlyKnownModules(Context context, PersistenceHelper globalPh,PersistenceHelper ph,String server, String bundle, LoggerI debugConsole) {
        List<ConfigurationModule> ret = new ArrayList<>();
        //Workflow xml. Named same as bundle.
        final String pathOrURL = server + bundle.toLowerCase() + "/";
        Log.d("vortex","Parthorurl is now"+pathOrURL);
        String cachePath = context.getFilesDir()+"/"+bundle.toLowerCase(Locale.ROOT) + "/cache/";
        ret.add(new WorkFlowBundleConfiguration(context,cachePath,globalPh,ph,pathOrURL,bundle,debugConsole));
        ret.add(new SpinnerConfiguration(context,globalPh,ph,pathOrURL,debugConsole));
        ret.add(new GroupsConfiguration(context,globalPh,ph,pathOrURL,bundle,debugConsole));
        //VariableConfiguration depends on the Groups Configuration.
        ret.add(new VariablesConfiguration(context,globalPh,ph,pathOrURL,debugConsole));
        ret.add(new GISListConfiguration(context,globalPh,ph,pathOrURL+Constants.GIS_CONFIG_WEB_FOLDER +"/"));
        return ret;
    }

    public static void getDBImportModules(Context context,
            final PersistenceHelper globalPh, final PersistenceHelper ph, final String server,
            final String bundle, final LoggerI debugConsole,final DbHelper db, final Table t, final AsyncLoadDoneCb asyncLoadDoneCb) {
        final List<ConfigurationModule> ret = new ArrayList<>();
        //Workflow xml. Named same as bundle.
        //ret.add(new GisPolygonConfiguration(globalPh,ph,VORTEX_ROOT_DIR+bundle+AIR_PHOTO_FILE_DIR,debugConsole,db));
        //ret.add(new ImportDataConfiguration(context,globalPh,ph,server,bundle,debugConsole,db,t));

        final String fileFolder = context.getFilesDir()+"/"+bundle+"/gisdata/";
        final String serverFolder = server+"/"+bundle.toLowerCase()+"/"+Constants.GIS_CONFIG_WEB_FOLDER +"/";
        //dont load if no update or no connection

        new DownloadFileTask(new WebLoaderCb() {

            @Override
            public void loaded(List<String> fileNames) {
                if (fileNames != null)
                    Log.d("vortex", "loadresult is " + fileNames.toString());
                getAllConfigurationFileNamesFromWebOrFile(context,fileNames, serverFolder, fileFolder, asyncLoadDoneCb, globalPh, ph, debugConsole, db, ret, t);
            }
        })
                .execute(serverFolder + Constants.GIS_LIST_FILE_NAME);
    } 		//Try server.



    private static void getAllConfigurationFileNamesFromWebOrFile(Context context,List<String> fileNames,
                                                                  String serverFolder, String fileFolder, AsyncLoadDoneCb asyncLoadDoneCb, PersistenceHelper globalPh,PersistenceHelper ph, LoggerI debugConsole,DbHelper db, List<ConfigurationModule> modules, Table t) {

        boolean loadFromWeb=false;
        //look for contents.txt file on net.
        if (fileNames!=null) {
            Log.d("vortex","found GIS files list.");
            loadFromWeb = true;
        } else
            fileNames = getAllConfigurationFileNames(fileFolder);
        if (fileNames!=null && !fileNames.isEmpty()) {
            for (String file:fileNames) {
                modules.add(new GisObjectConfiguration(context,globalPh,ph,serverFolder,file,debugConsole,db,t));
            }
        } else
            Log.d("vortex","found no GIS configuration files.");


        asyncLoadDoneCb.onLoadSuccesful(modules);
    }




    private static List<String> getAllConfigurationFileNames(String folderName) {
        List<String> ret = new ArrayList<>();
        File folder = new File(folderName);
        //folder.mkdir();
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles!=null) {
            for (File f:listOfFiles) {
                Log.d("vortex","scanning "+f.getName());
                if (f.isFile() && f.getName().endsWith(".json"))
                    ret.add(f.getName().substring(0, f.getName().length()-".json".length()));
            }
        }
        return ret;
    }

    //Historical picture year is currently five years minus current year.
    public static int getHistoricalPictureYear() {
        return Calendar.getInstance().get(Calendar.YEAR)-5;
    }




    private interface WebLoaderCb {

        void loaded(List<String> result);
    }

    private static class DownloadFileTask extends AsyncTask<String, Void, List<String>> {
        final WebLoaderCb cb;

        DownloadFileTask(WebLoaderCb cb) {
            this.cb=cb;
        }

        protected List<String> doInBackground(String... url) {
            String inputLine;
            URL website=null;
            BufferedReader in;
            List<String> fileNames = null;
            try {
                website = new URL(url[0]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            try {
                URLConnection ucon = website.openConnection();
                ucon.setConnectTimeout(5000);
                in = new BufferedReader(new InputStreamReader(ucon.getInputStream()));

                while ((inputLine = in.readLine()) != null) {
                    if (fileNames==null)
                        fileNames = new ArrayList<>();
                    fileNames.add(inputLine);
                }
                in.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return fileNames;
        }

        protected void onPostExecute(List<String> fileNames) {

            cb.loaded(fileNames);
        }
    }






}
