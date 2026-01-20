//package com.teraim.fieldapp.loadermodule;
//
//import android.os.AsyncTask;
//import android.util.JsonReader;
//import android.util.Log;
//import android.util.Xml;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//
//import com.teraim.fieldapp.FileLoadedCb;
//import com.teraim.fieldapp.GlobalState;
//import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
//import com.teraim.fieldapp.loadermodule.configurations.CI_ConfigurationModule;
//import com.teraim.fieldapp.loadermodule.configurations.Dependant_Configuration_Missing;
//import com.teraim.fieldapp.log.LoggerI;
//import com.teraim.fieldapp.utils.Tools;
//
//import org.json.JSONException;
//import org.xmlpull.v1.XmlPullParser;
//import org.xmlpull.v1.XmlPullParserException;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.StringReader;
//import java.lang.ref.WeakReference;
//
////Loader might have progressbar.
//
//abstract class Loader {
//
//
//	private final ModuleLoaderCb cb;
//	Loader(ModuleLoaderCb cb) {
//		this.cb=cb;
//	}
//
//
//	static float getVersion(String h1,String h2) {
//
//		Log.d(TAG,"Get version with h1:"+h1+" h2: "+h2);
//
//		if (h2!=null) {
//			int p = h2.indexOf("app_version");
//			if (p>0) {
//				p = h2.indexOf("version",p+11);
//				String vNo = h2.substring(p+9, h2.indexOf('\"', p+9));
//				Log.d(TAG,"Version line: "+vNo);
//				if (Tools.isVersionNumber(vNo))
//					return Float.parseFloat(vNo);
//			}
//		} else {
//			if (h1 !=null) {
//				String[] header = h1.split(",");
//				if (header.length >= 2) {
//					String potVer = header[1].trim();
//					if (Tools.isVersionNumber(potVer))
//						return Float.parseFloat(potVer);
//				}
//			}
//		}
//		Log.d(TAG,"No version found for simple lookup.");
//		Log.d(TAG,"Header row1: "+h1);
//		Log.d(TAG,"Header row2: "+h2);
//		return -1;
//
//	}
//
//	private int rowC = 1;
//	LoadResult read(ConfigurationModule m, float newVersion, BufferedReader reader, StringBuilder sb) throws IOException {
//		String line;
//		float frozenVersion = m.getFrozenVersion();
//		Log.d(TAG,"module: "+m.getLabel()+" versionC: "+versionControl+" newVersion: "+newVersion+" frozenV: "+frozenVersion);
//		if (newVersion != -1) {
//			m.setNewVersion(newVersion);
//			if (versionControl) {
//				// frozenVersion = m.getFrozenVersion();
//				Log.d(TAG,"newversion equals: "+newVersion+" frozenVersion equals "+frozenVersion);
//
//				if (frozenVersion!=-1) {
//					if (frozenVersion==newVersion) {
//						//We can set the version number safely.
//						Log.d(TAG,"frozenversion equals new: "+frozenVersion);
//						return new LoadResult(m,ErrorCode.sameold);
//					} else
//					if (frozenVersion > newVersion)
//						return new LoadResult(m,ErrorCode.existingVersionIsMoreCurrent,newVersion+"");
//
//				}
//			}
//		}
//		Log.d(TAG,"READING FILE!");
//		while ((line = reader.readLine()) != null)
//		{
//			sb.append(line);
//			sb.append("\n");
//			if(isCancelled()) {
//				reader.close();
//				return new LoadResult(m,ErrorCode.Aborted);
//			}
//			if ((rowC++%20)==0)
//				this.publishProgress(0,rowC);
//		}
//		m.setRawData(sb.toString(), rowC);
//		return new LoadResult(m,ErrorCode.loaded);
//	}
//
//	LoadResult parse(ConfigurationModule m) throws IOException, XmlPullParserException, JSONException, Dependant_Configuration_Missing {
//		switch(m.type) {
//			case csv:
//				/*fallthrough*/
//			case ini:
//				/*fallthrough*/
//			case jgw:
//				return parseCSV((CI_ConfigurationModule)m);
//			case json:
//				return parseJSON((JSONConfigurationModule)m);
//			case xml:
//				/*fallthrough*/
//			default:
//				return parseXML((XMLConfigurationModule)m);
//
//		}
//
//
//
//
//	}
//
//	private LoadResult parseCSV(CI_ConfigurationModule m) throws IOException, Dependant_Configuration_Missing {
//		String[] myRows;
//		int noOfRows=rowC;
//		LoadResult res = null;
//		rowC=1;
//		LoadResult lr = m.prepare();
//		//exit if prepare returns.
//		if (lr!=null)
//			return lr;
//
//		if (m.getRawData()!=null)
//			myRows = m.getRawData().split("\\n");
//		else
//			return new LoadResult(m,ErrorCode.noData);
//		for (String row:myRows) {
//			LoadResult loadR = m.parse(row, rowC);
//			if (loadR !=null) {
//				res = loadR;
//				break;
//			}
//			if ((rowC++%20)==0)
//				this.publishProgress(rowC,noOfRows);
//
//		}
//		if (res==null)
//			m.finalizeMe();
//		if (res==null)
//			res = new LoadResult(m,ErrorCode.parsed);
//		this.publishProgress(rowC,noOfRows);
//		return res;
//	}
//
//
//	private LoadResult parseXML(XMLConfigurationModule m) throws XmlPullParserException, IOException {
//		XmlPullParser parser = Xml.newPullParser();
//		parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
//		parser.setInput(new StringReader(m.getRawData()));
//		LoadResult lr = m.prepare(parser);
//		//exit if prepare returns.
//		if (lr!=null)
//			return lr;
//		rowC=0;
//		while((lr=m.parse(parser))==null) {
//			if ((rowC++%20)==0)
//				this.publishProgress(rowC);
//		}
//		return lr;
//	}
//
//
//	private LoadResult parseJSON(JSONConfigurationModule m) throws IOException, JSONException {
//		JsonReader parser = new JsonReader(new StringReader(m.getRawData()));
//		LoadResult lr = m.prepare(parser);
//		if (lr!=null)
//			return lr;
//		rowC=0;
//
//		while((lr=m.parse(parser))==null) {
//			if ((rowC++%20)==0)
//				this.publishProgress(rowC);
//
//		}
//
//		return lr;
//
//	}
//
//
//
//	LoadResult freeze(ConfigurationModule m) throws IOException {
//		Log.d(TAG,"Freeze called for "+m.getLabel());
//		//Multiple steps or only one to freeze?
//		if (m.freezeSteps>0) {
//			rowC=0;
//			while (rowC<m.freezeSteps) {
//				m.freeze(rowC);
//				if ((rowC++%100)==0)
//					this.publishProgress(rowC,m.freezeSteps);
//
//			}
//		} else
//			m.freeze(-1);
//
//
//		if (m.newVersion!=-1) {
//			m.setFrozenVersion(m.newVersion);
//			Log.d(TAG,"Frozen version set to "+m.newVersion);
//		} else
//			Log.d(TAG,"NewVersion number was -1 in setfrozenversion");
//
//		if (m.getEssence()!=null||m.isDatabaseModule)
//			return new LoadResult(m,ErrorCode.frozen);
//		else {
//			//Log.d(TAG,"in freez: Essence is "+m.getEssence());
//			return new LoadResult(m,ErrorCode.noData);
//		}
//
//	}
//
//}
