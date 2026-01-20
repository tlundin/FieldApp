package com.teraim.fieldapp.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.teraim.fieldapp.ui.ExportDialogInterface;
import com.teraim.fieldapp.utils.DbHelper.DBColumnPicker;
import com.teraim.fieldapp.utils.DbHelper.StoredVariableData;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

public class CSVExporter extends Exporter {
	private static final String TAG = "CSVExporter";


    private int varC=0;


	public CSVExporter(Context ctx, ExportDialogInterface eDialog) {
		super(ctx,eDialog);
	}



	@Override
	public Report writeVariables(DBColumnPicker cp) {
        StringWriter sw = new StringWriter();

		try {
			if (cp.moveToFirst()) {
				Map<String,String> currentKeys = cp.getKeyColumnValues();
				//eDialog.setExportProgress("Current keys: "+currentKeys.toString());

				Log.d(TAG,"Exporting csv");


				StringBuilder header= new StringBuilder();
				for (String key: currentKeys.keySet()) {
					header.append(key).append(",");
				}
				header.append("name,");
				header.append("value,");
				header.append("type,");
				header.append("team,");
				header.append("author,");
				header.append("timestamp");


				Log.d(TAG, header.toString());
				sw.write(header.toString());
				sw.append(System.getProperty("line.separator"));
				String row,value,var;
				
				do {
					row = "";					
					currentKeys = cp.getKeyColumnValues();
					var = writeVariable(cp.getVariable());
					if (var.length()>0) {
						for (String key:currentKeys.keySet()) {
							value = currentKeys.get(key);
							if (value==null)
								value = "";
							row+=value+",";
						}
						row += var;

						Log.d(TAG,row);
						sw.write(row);
						sw.append(System.getProperty("line.separator"));
						varC++;
					}
					if (ctx != null)
						((Activity)ctx).runOnUiThread(new Runnable() {
							@Override
							public void run() {
								eDialog.setGenerateStatus(varC+"");
							}
						});
				} while (cp.next());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new Report(sw.toString(),varC);//new Report(result,noOfVars);
	}


	private String writeVariable(StoredVariableData variable) {
		//Type found from extended data
		String ret = "";
		String type;
		List<String> row = al.getCompleteVariableDefinition(variable.name);
		boolean isExported = true;
		if (row==null)
			type ="";
		else {
			type = al.getnumType(row).name();
			isExported = !al.isLocal(row);
		}
		if (isExported) {
			ret = variable.name+","+variable.value+","+type+","+variable.lagId+","+variable.creator+","+variable.timeStamp;
		} else 
			Log.d(TAG,"Didn't export "+variable.name);

		return ret;
	}
	





	@Override
	public String getType() {
		return "csv";
	}
}
