package com.teraim.fieldapp.loadermodule;

import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.utils.PersistenceHelper;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public abstract class XMLConfigurationModule extends ConfigurationModule {

	
	protected XMLConfigurationModule(PersistenceHelper gPh, PersistenceHelper ph,
                                     Source source, String urlOrPath, String fileName, String moduleName) {
		super(gPh,ph, Type.xml, source, urlOrPath, fileName, moduleName);
	}
	protected abstract LoadResult prepare(XmlPullParser parser) throws XmlPullParserException, IOException;
	protected abstract LoadResult parse(XmlPullParser parser) throws XmlPullParserException, IOException;
	
	//Skips entry...return one level up in recursion if end reached.
	protected void skip(String name,XmlPullParser parser, LoggerI o) throws XmlPullParserException, IOException {
		if (parser.getEventType() != XmlPullParser.START_TAG) {
			if (o!=null) { 
				o.addRow("");
				o.addRedText("IllegalStateException while trying to read START_TAG");
			}
			throw new IllegalStateException();

		}
		if (o!=null) {
			o.addRow("");
			if ("workflow".equals(name)) {
				o.addRedText("Closing tag for workflow missing. Aborting");
				throw new XmlPullParserException("Workflow closing tag missing");
			} else {		
			o.addYellowText("Skipped TAG: ["+name+"]");
			}
		}
		int depth = 1;
		while (depth != 0) {
			switch (parser.next()) {
			case XmlPullParser.END_TAG:
				depth--;
				break;
			case XmlPullParser.START_TAG:
				depth++;
				break;
			}
		}
	}
	
	// Read string from tag.
	protected static String readText(String tag,XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null,tag);
		String text = readText(parser);
		parser.require(XmlPullParser.END_TAG, null,tag);
		if (text==null || text.isEmpty())
			return null;
		else
			return text;
	}

	protected static String[] readArray(String tag,XmlPullParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null,tag);
		String temp = readText(parser);
		String[] res = null;
		if (temp!=null) 
			res = temp.split(",");			 

		parser.require(XmlPullParser.END_TAG, null,tag);
		return res;
	}

	// Extract string values.
	protected static String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
		String result = "";
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		}
		return result;
	}
	// Extract string values.
	protected static Float readFloat(XmlPullParser parser) throws IOException, XmlPullParserException {
		String  result=null;
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		}
		return Float.parseFloat(result);
	}
	protected void skip(String name,XmlPullParser parser) throws XmlPullParserException, IOException {
		skip(name,parser,null);
	}

}
