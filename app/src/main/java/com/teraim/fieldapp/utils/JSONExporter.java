package com.teraim.fieldapp.utils;

import android.app.Activity;
import android.content.Context;
import android.util.JsonWriter;
import android.util.Log;

import com.teraim.fieldapp.ui.ExportDialogInterface;
import com.teraim.fieldapp.utils.DbHelper.DBColumnPicker;
import com.teraim.fieldapp.utils.DbHelper.StoredVariableData;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JSONExporter extends Exporter {

	private JsonWriter writer;
    private int varC=0;

	public JSONExporter(Context ctx, ExportDialogInterface eDialog) {
		super(ctx,eDialog);
	}

	public Report writeVariables(DBColumnPicker cp) {

        StringWriter sw = new StringWriter();
		writer = new JsonWriter(sw);	

		try {
			if (cp.moveToFirst()) {
				writer.setIndent("  ");
				writer.beginObject();
				writeHeader();								
				Map<String,String> currentKeys = cp.getKeyColumnValues();
				Log.d("nils","Current keys: "+currentKeys.toString());
				writer.name("Elements");
				writer.beginArray();
				boolean more = true;
				do {
					//writer.name("NewKey");
					writer.beginObject();
					writeSubHeader(currentKeys);
					writer.name("Variables");
					writer.beginArray();					
					while (true) {
						varC++;
						writeVariable(cp.getVariable());
						more = cp.next();
						if (!more)
							break;
						Map<String,String> newKeys = cp.getKeyColumnValues();
						if (Tools.sameKeys(currentKeys, newKeys)) {
							currentKeys = newKeys;
							break;

						}
					}
					//Close variables array
					writer.endArray();
					//close NewKey.
					writer.endObject();
					if (ctx != null)
						((Activity)ctx).runOnUiThread(new Runnable() {
							@Override
							public void run() {
								eDialog.setGenerateStatus(varC+"");
							}
						});
				} while (more);				
				//Close Elements
				writer.endArray();
				//Close header
				writer.endObject();
				writer.close();
				Log.d("nils","finished writing JSON");
				Log.d("nils", sw.toString());
				return new Report(sw.toString(),varC);

			} else {
				Log.e("nils","cursor empty in writeVariables.");
				//writer.close();			
				return null;
			}





		} catch (IOException e) {
			e.printStackTrace();
			cp.close();
		} finally {
			cp.close();
		}

		return null;
	}







	
	private void write(String name,String value) throws IOException {

		String val = (value==null||value.length()==0)?"NULL":value;
		writer.name(name).value(val);
	}

	private void writeVariable(StoredVariableData variable) throws IOException {
		//Type found from extended data
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
		writer.beginObject();
		write("name",variable.name);
		write("value",variable.value);
		write("type",type);
		write("lag",variable.lagId);
		write("author",variable.creator);
		write("timestamp",variable.timeStamp);
		writer.endObject();
		} else 
			Log.d("nils","Didn't export "+variable.name);
		
			
	}
		

	private void writeSubHeader(Map<String,String> currentKeys) throws IOException {
		//subheader.
		Set<String> keys = currentKeys.keySet();
		
		for (String key:keys) {
			write(key,currentKeys.get(key));
		}
	}

	private void writeHeader() throws IOException {
		Date now = new Date();
		//File header.
		Log.d("nils","Exporting database");
		write("date",DateFormat.getInstance().format(now));
		write("time",DateFormat.getTimeInstance().format(now));
		write("programversion",Float.toString(globalPh.getF(PersistenceHelper.CURRENT_VERSION_OF_PROGRAM)));
		write("workflow bundle version",""+ph.getF(PersistenceHelper.CURRENT_VERSION_OF_WF_BUNDLE));
		write("Artlista version",""+ph.getF(PersistenceHelper.CURRENT_VERSION_OF_GROUP_CONFIG_FILE));
		write("Variable Definition version",""+ph.getF(PersistenceHelper.CURRENT_VERSION_OF_VARPATTERN_FILE));
		write("Author",""+globalPh.get(PersistenceHelper.USER_ID_KEY));
		write("Team",""+globalPh.get(PersistenceHelper.LAG_ID_KEY));
		Log.d("nils",writer.toString());
		
	}

	@Override
	public String getType() {
		return "json";
	}


}
