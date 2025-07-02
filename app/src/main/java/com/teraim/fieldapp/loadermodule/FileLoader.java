//package com.teraim.fieldapp.loadermodule;
//
//import android.widget.ProgressBar;
//import android.widget.TextView;
//
//import com.teraim.fieldapp.FileLoadedCb;
//import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
//import com.teraim.fieldapp.loadermodule.configurations.Dependant_Configuration_Missing;
//
//import org.json.JSONException;
//import org.xmlpull.v1.XmlPullParserException;
//
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
//
//class FileLoader extends Loader {
//
//	public FileLoader(ProgressBar pb, TextView tv, FileLoadedCb cb,
//			String versionControlS) {
//		super(pb, tv, cb, versionControlS);
//	}
//
//	@Override
//	protected LoadResult doInBackground(ConfigurationModule... params) {
//
//		ConfigurationModule module = params[0];
//
//		BufferedReader reader=null;
//		try {
//			reader = new BufferedReader(new FileReader(module.getFullPath()));
//		float version=-1;
//		StringBuilder sb = new StringBuilder();
//		if (module.hasSimpleVersion) {
//			String header = reader.readLine();
//			version = versionControl?getVersion(header,null):-1;
//		}
//
//		LoadResult lr = read(module,version,reader,sb);
//		//setresult runs a parser before returning. Parser is depending on module type.
//		if (lr!=null && lr.errCode==ErrorCode.loaded) {
//			lr = parse(module);
//			if (lr.errCode == ErrorCode.parsed) {
//				lr = freeze(module);
//			}
//		}
//		return lr;
//
//		}  catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return new LoadResult(module,ErrorCode.IOError);
//		} catch (XmlPullParserException e) {
//			e.printStackTrace();
//			return new LoadResult(module,ErrorCode.ParseError,"XML PULL PARSER EXCEPTION");
//		} catch (JSONException e) {
//			e.printStackTrace();
//			return new LoadResult(module,ErrorCode.ParseError,"JSON PARSER EXCEPTION");
//		} catch (Dependant_Configuration_Missing e) {
//			return new LoadResult(module, ErrorCode.reloadDependant,e.getDependendant());
//		}
//		finally {
//			try {if (reader!=null)reader.close();}catch (Exception e){}
//        }
//	}
//
//}
