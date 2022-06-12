package com.teraim.fieldapp.utils;

import android.content.SharedPreferences;

import java.util.ArrayList;

public class PersistenceHelper {
	public static final String UNDEFINED = "";
	public static final String USER_ID_KEY = "user_id";
	public static final String LAG_ID_KEY = "lag_id";
	public static final String EXPORT_EMAIL_KEY = "export_email_key";
	public static final String MITTPUNKT_KEY = "mittpunkt";
	public static final String DEVICE_COLOR_KEY_NEW = "device_type";
	public static final String SHOW_AUTHOR_KEY = "show_author";
	public static final String CONFIG_LOCATION = "config_name";
	public static final String BUNDLE_NAME = "bundle_name";
	public static final String BACKUP_LOCATION = "backup_location";
	public static final String SERVER_URL = "server_location";
	public static final String EXPORT_SERVER_URL = "export_server_location";
	public static final String FOLDER = "file_picker";
	public static final String CURRENT_VERSION_OF_APP = "current_version_of_app_f";
	public static final String CURRENT_VERSION_OF_WF_BUNDLE = "current_version_wf_f";
	public static final String CURRENT_VERSION_OF_GROUP_CONFIG_FILE = "current_version_config_f";
	public static final String CURRENT_VERSION_OF_PROGRAM = "prog_version";
	public static final String CURRENT_VERSION_OF_HISTORY_FILE = "current_version_hist_f";
	public static final String CURRENT_VERSION_OF_SPINNERS = "current_version_spinners_f";
	public static final String CURRENT_VERSION_OF_GIS_BLOCKS = "current_version_gis_blocks_f";
	public static final String CURRENT_VERSION_OF_GIS_OBJECT_BLOCKS = "current_version_gis_object_blocks_f";
	public static final String CURRENT_VERSION_OF_VARPATTERN_FILE = "current_version_varpattern_f";

	public static final String FIRST_TIME_KEY = "firzzt";
	public static final String GIS_CREATE_FIRST_TIME_KEY = "fzzzt_gis";
	public static final String DEVELOPER_SWITCH = "dev_switch";
	public static final String VERSION_CONTROL = "version_control";
	public static final String HistoricalRutorList = "historical_rutor";
	public static final String AVSTAND_IS_PRESSED = "avstands_matning_pressed";
	public static final String NO_OF_PROVYTOR = "antalprovytor";
	public static final String NO_OF_RUTOR = "antalrutor";
	public static final String CHANGE_BUNDLE = "change_bundle";
	public static final String HIST_LOAD_COUNTER = "hist_counter";
	public static final String AVSTAND_WARNING_SHOWN = "Avstand_warning_was_shown";
	public static final String GLOBAL_AUTO_INC_COUNTER = "auto_increment_counter";
	public static final String TIME_OF_FIRST_USE = "time_of_first_use";
	public static final String LAYER_VISIBILITY = "Layer_Is_Visible_";
	public static final String LAYER_BOLDNESS = "Layer_Is_Bold_";
	public static final String ALL_MODULES_FROZEN = "All_modules_frozen_";
	public static final String NEW_APP_VERSION = "new_app_version";
	public static final String TIME_OF_LAST_BACKUP = "time_of_last_backup";
	public static final String BACKUP_AUTOMATICALLY = "auto_backup";
	public static final String SYNC_METHOD = "sync_method";
	public static final String TIME_OF_LAST_SYNC = "kakkadua";
	public static final String SYNC_ON_FIRST_TIME_KEY = "sync_first_use";
	public static final String TIMESTAMP_LAST_SYNC_FROM_ME = "time_last_sync_from_me";
	public static final String TIME_OF_LAST_SYNC_FROM_TEAM = "time_last_sync_from_team";
	public static final String TIME_OF_LAST_SYNC_INET = "last_time_I_got_something_from_server";
	public static final String PARTNER_NAME = "my_partner";
	public static final String LOG_LEVEL = "log_levels";
	public static final String PotentiallyTimeStampToUseIfInsertDoesNotFail="potential_timestamp";
	public static final String USERUUID_KEY = "myuuid";

	private final SharedPreferences sp;
	ArrayList<String> delta = new ArrayList<String>();

	public PersistenceHelper(SharedPreferences sp) {
		this.sp = sp;
	}

	public String get(String key,String undefined) {
		return sp.getString(key,undefined);
	}

	public String get(String key) {
		return sp.getString(key,UNDEFINED);
	}

	public void put(String key, String value) { sp.edit().putString(key,value).commit();

	}
	public void put(String key, boolean value) {
		sp.edit().putBoolean(key,value).apply();
	}
	public void put(String key, int value) {
		sp.edit().putInt(key,value).apply();
	}
	public void put(String key, float value) {
		sp.edit().putFloat(key,value).apply();
	}
	public void put(String key, long value) { sp.edit().putLong(key,value).apply();
	}
	public boolean getB(String key) {
		return sp.getBoolean(key, false);
	}

	public int getI(String key) {
		return sp.getInt(key, -1);
	}

	public float getF(String key) {
		return sp.getFloat(key, -1);
	}

	public long getL(String key) {
		return sp.getLong(key, -1);
	}


}