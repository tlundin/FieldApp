package com.teraim.fieldapp.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.log.LogRepository;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.ui.ExportDialog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.TimeUnit;

public class BackupManager {
	private static final String TAG = "BackupManager";


	private static BackupManager singleton;
	private final GlobalState gs;
	private final Context ctx;

	public BackupManager(GlobalState gs) {
		this.gs = gs;
		this.ctx=gs.getContext();
	}


	public static BackupManager getInstance(GlobalState gs) {
		if (singleton == null)
			singleton = new BackupManager(gs);
		return singleton;
	}


	/*
	public File getBackupStorageDir() {
		gs = GlobalState.getInstance();
		// Get the directory for the user's public pictures directory.
		PersistenceHelper globalPh = gs.getGlobalPreferences();
		String path = null;
		if (globalPh !=null)
			path = globalPh.get(PersistenceHelper.BACKUP_LOCATION);

		if (path==null||path.length()==0) {
			Log.d(TAG,"Path was null for backupdir. will try default path");
			path = Constants.DEFAULT_EXT_BACKUP_DIR;
		}

		File backupFolder = new File(path);
		if (!backupFolder.exists()) {
			System.out.println("creating directory: " + Constants.DEFAULT_EXT_BACKUP_DIR);
			boolean result = false;

			try{
				backupFolder.mkdir();
				result = true;
			} catch(SecurityException se){
				//handle it
			}        
			if(result) {    
				System.out.println("DIR created");  
				return backupFolder;
			}
		}


		return null;
	}
	 */
	public boolean backupDatabase() {
		return backupDatabase(Constants.BACKUP_FILE_NAME+"_"+Constants.getSweDate());
	}

	/**
	 * 
	 * @param backupFileName --optional and should normally not be used.
	 * @return
	 */
	public boolean backupDatabase(String backupFileName) {
		String appName = GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.BUNDLE_NAME);
		LogRepository logger = LogRepository.getInstance();
		Log.d(TAG,"starting backup for "+ctx.getDatabasePath(appName));
		logger.addText("Starting backup for "+ctx.getDatabasePath(appName));
		File dbFile = ctx.getDatabasePath(appName);
		try {
			File dir = createOrFindBackupStorageDir();
			
			if (dir == null) {
				Log.e("vortex","failed to create export folder!!");
				return false;
				
			}
			String outFileName = dir+"/"+backupFileName;

			Log.d(TAG,"Output folder: "+outFileName);
			// Transfer bytes from the inputfile to the outputfile
			byte[] buffer = new byte[1024];
			int length;
			try (FileInputStream fis = new FileInputStream(dbFile);
				 OutputStream output = new FileOutputStream(outFileName)) {
				while ((length = fis.read(buffer))>0){
					output.write(buffer, 0, length);
				}
				output.flush();
			}

		} catch (FileNotFoundException e) {
			Log.e("vortex","backup failed");
			logger.addCriticalText("Backup failed");
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			Log.e("vortex","backup failed");
			logger.addCriticalText("Backup failed");
			e.printStackTrace();
			return false;
		}
		Log.d(TAG,"backup done");
		gs.getPreferences().put(PersistenceHelper.TIME_OF_LAST_BACKUP,System.currentTimeMillis());
		logger.addText("Backup done");
		return true;
	}
	
	
	private File createOrFindBackupStorageDir() {
		File[] externalStorageVolumes =
				ContextCompat.getExternalFilesDirs(gs.getContext(), null);
		File primaryExternalStorage = externalStorageVolumes[0];
		//String state = Environment.getExternalStorageState();
		//Log.d(TAG,"ext state: "+state);
		//Log.d(TAG, Environment.getExternalStorageDirectory().toString());


		//File sdCard = Environment.getExternalStorageDirectory();
		//File dir = new File (sdCard.getAbsolutePath() + "/vortex");
		String backupFolder = gs.getGlobalPreferences().get(PersistenceHelper.BACKUP_LOCATION);
		if (backupFolder==null || backupFolder.isEmpty()) {
			backupFolder = primaryExternalStorage+"/"+gs.getGlobalPreferences().get(PersistenceHelper.BUNDLE_NAME)+"/backup";
			Log.e("vortex","no backup folder configured...reverting to default:\n"+backupFolder);
			//backupFolder = Constants.DEFAULT_EXT_BACKUP_DIR;
		}
		File dir = new File (backupFolder);

		dir.mkdirs();
		LogRepository logger = LogRepository.getInstance();
		logger.addText("Backup to folder:"+backupFolder);
		return dir;
	}


	public String backupExportDataWithProgress(String exportFileName, String data, final ExportDialog dialog, Activity act) {

		File dir = createOrFindBackupStorageDir();

		if (exportFileName==null) {
			Log.e("vortex","no filename!!");
			return "no filename";
		}
		if (data==null) {
			Log.e("vortex","no data sent to backup!");
			return "no data";
		}

		if (dir == null) {
			Log.e("vortex","failed to create export folder!!");
			return "failed to create folder";
		}
		String ret = "OK";
		File file = new File(dir, exportFileName);
		int offset=0;
		int count=512;
		try (BufferedWriter outWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)), 1024)) {
			while(data.length()>offset) {
				// write part of
				// string
				outWriter.write(data, offset, Math.min(count,data.length()-offset));
				final int off = offset;
				act.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						dialog.setBackupStatus(off+"");
					}

				});
				offset += count;

			}
		}catch(Exception e){
			ret = e.getMessage();
			System.out.println("Could not write backup file! Filename: "+exportFileName);

			e.printStackTrace();
			return ret;
		}
		Log.d(TAG,"file successfully written to backup: "+exportFileName);

		return ret;


	}


	public String backupExportData(String exportFileName, String data) {

		File dir = createOrFindBackupStorageDir();
		
		if (exportFileName==null) {
			Log.e("vortex","no filename!!");
			return "no filename";
		}
		if (data==null) {
			Log.e("vortex","no data sent to backup!");
			return "no data";
		}
		
		if (dir == null) {
			Log.e("vortex","failed to create export folder!!");
			return "failed to create folder";
		}
		String ret = "OK";
		File file = new File(dir, exportFileName);
		try (BufferedWriter outWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)),1024)) {
			// write the whole string
			outWriter.write(data);
		}catch(Exception e){
			ret = e.getMessage();
			System.out.println("Could not write backup file! Filename: "+exportFileName);

			e.printStackTrace();
			return ret;
		}
		Log.d(TAG,"file successfully written to backup: "+exportFileName);

		return ret;


	}




	/* Checks if external storage is available for read and write */
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }


	public boolean timeToBackup() {

		if (gs.getGlobalPreferences().getB(PersistenceHelper.BACKUP_AUTOMATICALLY)) {
			long timeOfLast = gs.getPreferences().getL(PersistenceHelper.TIME_OF_LAST_BACKUP);
			Log.d(TAG, "time of last backup: " + timeOfLast);
			if (System.currentTimeMillis() - timeOfLast > Constants.BACKUP_FREQUENCY) {
				Log.d(TAG, "Time to backup!");
				return true;
			}  else {
				Log.d(TAG,"No backup required");
				gs.getLogger().addGreenText("Database backup not required");
			}
		} else {
			Log.d(TAG,"Autobackup is off");
			gs.getLogger().addGreenText("Database auto-backup turned off");
		}
		return false;
	}

	public void backUp() {
			if (!this.backupDatabase()) {
				gs.getLogger().addCriticalText("Backup of data failed! Please make sure you have configured the backup folder correctly under the config menu.");
            } else {
				gs.getLogger().addGreenText("Your database has been backed up");
            }



	}

	public boolean restoreDatabase() {
		return restoreDatabase(Constants.BACKUP_FILE_NAME);
	}
	private boolean restoreDatabase(String backupFileName) {
		String appName = GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.BUNDLE_NAME);
		File[] externalStorageVolumes =
				ContextCompat.getExternalFilesDirs(gs.getContext(), null);
		File primaryExternalStorage = externalStorageVolumes[0];
		File dbFile = ctx.getDatabasePath(appName);
		Log.d(TAG,"starting restore for "+dbFile);
		
		try {
			
			File dir = createOrFindBackupStorageDir();
			
			if (dir == null) {
				Log.e("vortex","failed to find backup folder named ["+ primaryExternalStorage+gs.getGlobalPreferences().get(PersistenceHelper.BUNDLE_NAME)+"/backup"+"]");
				return false;
				
			}

			String backupFile = dir+"/"+backupFileName;

			Log.d(TAG,"Restore file: "+backupFile);
			// Transfer bytes from the inputfile to the outputfile
			byte[] buffer = new byte[1024];
			int length;

			//Close the existing database.
			GlobalState.getInstance().getDb().closeDatabaseBeforeExit();

			try (FileInputStream fis = new FileInputStream(backupFile);
				 OutputStream output = new FileOutputStream(dbFile)) {
				while ((length = fis.read(buffer))>0){
					output.write(buffer, 0, length);
				}
				output.flush();
			}
		} catch (FileNotFoundException e) {
			Log.e("vortex","restore failed");
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			Log.e("vortex","restore failed");
			e.printStackTrace();
			return false;
		}
		Log.d(TAG,"restore done");
		return true;
	}

	public String daysSinceLastBackup() {
		long timeOfLast = gs.getPreferences().getL(PersistenceHelper.TIME_OF_LAST_BACKUP);
		if (timeOfLast <= 0) {
			return "0";
		}
		long diffMs = System.currentTimeMillis() - timeOfLast;
		if (diffMs <= 0) {
			return "0";
		}
		long days = TimeUnit.MILLISECONDS.toDays(diffMs);
		return Long.toString(days);
	}
}
