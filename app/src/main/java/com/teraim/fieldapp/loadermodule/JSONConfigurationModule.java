package com.teraim.fieldapp.loadermodule;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonToken;

import com.teraim.fieldapp.utils.PersistenceHelper;

import org.json.JSONException;

import java.io.IOException;

public abstract class JSONConfigurationModule extends ConfigurationModule {

	protected JSONConfigurationModule(Context context, PersistenceHelper gPh, PersistenceHelper ph,
									   String urlOrPath, String fileName, String moduleName) {
		super(context,gPh,ph, FileFormat.json, urlOrPath, fileName, moduleName);
	}

	protected abstract LoadResult prepare(JsonReader reader) throws IOException, JSONException;
	public abstract LoadResult parse(JsonReader reader) throws IOException, JSONException;
	
	protected String getAttribute(JsonReader reader) throws IOException {
		String ret=null;
		if (reader.peek() != JsonToken.NULL) {
			if (reader.peek() == JsonToken.STRING) {
				ret = reader.nextString();
				if (ret.isEmpty())
					ret = null;
			} else if (reader.peek() == JsonToken.NUMBER) {
				/*
				try {
					//ret = Integer.toString((int)reader.nextDouble());
				} catch (NumberFormatException e) {
					Log.e("vortex","Numberformat exception here: "+reader);
					return null;
				}
				*/
				ret = reader.nextString();
			}
			else if (reader.peek() == JsonToken.BEGIN_OBJECT) {
				reader.beginObject();
				while (reader.peek() != JsonToken.END_OBJECT) {
					String name = reader.nextName();
					String attr = getAttribute(reader);
					ret = name+":"+attr;
				}
				reader.endObject();
			}
		}
		else { 
			ret = null;
			reader.nextNull();
		}
		//Log.d("vortex","Value: "+ret);
		return ret;

	}
}
