package com.teraim.fieldapp.loadermodule.configurations;

import android.content.Context;
import android.util.Log;

import com.teraim.fieldapp.dynamic.types.PhotoMeta;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.loadermodule.PhotoMetaI;
import com.teraim.fieldapp.loadermodule.XMLConfigurationModule;
import com.teraim.fieldapp.utils.PersistenceHelper;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Type;

public class AirPhotoMetaDataXML extends XMLConfigurationModule implements PhotoMetaI {
	private static final String TAG = "AirPhotoMetaDataXML";




	
	public AirPhotoMetaDataXML(Context context, PersistenceHelper gPh, PersistenceHelper ph,
							   String urlOrPath, String metaDataFileName,
							   String moduleName) {
		super(context,gPh, ph, urlOrPath, metaDataFileName, moduleName);
		hasSimpleVersion=false;
	}

	@Override
	protected LoadResult prepare(XmlPullParser parser) {
		return null;
	}

	@Override
	protected LoadResult parse(XmlPullParser parser)
			throws XmlPullParserException, IOException {
		Log.d(TAG,"Parsing metadata");
		   int eventType = parser.getEventType();
	      while (eventType != XmlPullParser.END_DOCUMENT) {

	            if (eventType == XmlPullParser.START_TAG) {
	                if (parser.getName().equalsIgnoreCase("nativeExtBox")) {

	                   essence = readCorners(parser);
	                   return new LoadResult(this,ErrorCode.parsed);
	                   }
	            }

	            eventType = parser.next();
	        }
          Log.d(TAG,"Did not find the meta data for image GPS coordinates!");
	      return new LoadResult(this,ErrorCode.ParseError);
	}

	private PhotoMeta readCorners(XmlPullParser parser) throws XmlPullParserException, IOException {
		double N = -1, E = -1, S = -1, W = -1;
		Log.d(TAG, "calling readCordners");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}

			Log.d(TAG, "reading corners!");
			String name = parser.getName();
			switch (name) {
				case "westBL":
					W = getCorner(parser);
					break;
				case "eastBL":
					E = getCorner(parser);
					break;
				case "northBL":
					N = getCorner(parser);
					break;
				case "southBL":
					S = getCorner(parser);
					break;
				default:
					skip(name, parser);
					break;
			}

		}

		return new PhotoMeta(N, E, S, W);
	}
	private double getCorner(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.next();
		double f =Double.parseDouble(parser.getText());
		parser.nextTag();
		return f;

	}

	@Override
	public float getFrozenVersion() {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	protected Type getEssenceType() {
		return PhotoMeta.class;
	}

	@Override
	protected void setFrozenVersion(float version) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setEssence() {
		
	}

	@Override
	public boolean isRequired() {
		return false;
	}

	@Override
	public PhotoMeta getPhotoMeta() {
		Object pm = getEssence();
		if (!(pm instanceof PhotoMeta))
			return null;
		return (PhotoMeta)pm;
	}
}
