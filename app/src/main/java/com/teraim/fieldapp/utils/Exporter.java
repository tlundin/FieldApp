package com.teraim.fieldapp.utils;

import android.content.Context;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.ui.ExportDialogInterface;
import com.teraim.fieldapp.utils.DbHelper.DBColumnPicker;


public abstract class Exporter {


    public static final int FAILED = 0;
	public static final int SUCCESS = 1;
	public static final int IN_PROGRESS = 2;

    public enum ExportReport {
		OK,
		NO_DATA,
		FILE_WRITE_ERROR,
		COLUMN_DOES_NOT_EXIST,
		EXPORTFORMAT_UNKNOWN
	}
	public static class Report {
		Report(String res, int vars) {
			this.result=res;
			this.noOfVars=vars;
			er = ExportReport.OK;
		}
		public Report(ExportReport problem) {
			er = problem;
		}
		private String result;
		public int noOfVars = 0;
		final private ExportReport er;

		public ExportReport getReport() {
			return er;
		}

		public String getData() {
			return result;
		}
	}
	
	final GlobalState gs;
	final VariableConfiguration al;
	final PersistenceHelper ph;
	final PersistenceHelper globalPh;
	protected static Exporter instance;
	final ExportDialogInterface eDialog;
	final Context ctx;

	public static Exporter getInstance(Context ctx, String type, ExportDialogInterface eDialog) {

		//Check clock
		if (Constants.FreeVersion) {
		long takenIntoUseTime = GlobalState.getInstance().getGlobalPreferences().getL(PersistenceHelper.TIME_OF_FIRST_USE);
		long currentTime = System.currentTimeMillis();
		long diff = currentTime - takenIntoUseTime;
		if (diff > Constants.MS_MONTH)
			return null;
		}

		if (type==null||type.equalsIgnoreCase("csv"))
			return new CSVExporter(ctx,eDialog);
		else
			if (type.equalsIgnoreCase("json"))
				return new JSONExporter(ctx,eDialog);
			else
				if (type.equalsIgnoreCase("geojson"))
					return new GeoJSONExporter(ctx,eDialog);
		return null;

	}
	Exporter(Context ctx, ExportDialogInterface eDialog) {
		this.gs=GlobalState.getInstance();
		al = gs.getVariableConfiguration();
		ph = gs.getPreferences();
		globalPh = gs.getGlobalPreferences();
		this.eDialog = eDialog;
		this.ctx = ctx;
		
	}

	public abstract Report writeVariables(DBColumnPicker cp);
	public abstract String getType();
	public ExportDialogInterface getDialog() { return eDialog;}
	public Context getContext() {return ctx;}
}
