package com.teraim.fieldapp.loadermodule.configurations;

import android.content.Context;
import android.util.Log;

import com.teraim.fieldapp.dynamic.types.SpinnerDefinition;
import com.teraim.fieldapp.dynamic.types.SpinnerDefinition.SpinnerElement;
import com.teraim.fieldapp.loadermodule.CSVConfigurationModule;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.log.LogRepository;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SpinnerConfiguration extends CSVConfigurationModule {
	
	public final static String NAME = "Spinners";
	private final static int noOfRequiredColumns=5;			
	private final SpinnerDefinition sd=new SpinnerDefinition();
	private final LogRepository o;
	private int c=0;

	public SpinnerConfiguration(Context context,  PersistenceHelper globalPh, PersistenceHelper ph, String serverOrFile, LogRepository debugConsole) {
		super(context, globalPh, ph, serverOrFile, SpinnerConfiguration.NAME,"Spinner module         ");
		this.o = debugConsole;
		o.addGreenText("Parsing spinner module");
	}

	@Override
	public float getFrozenVersion() {
		return (ph.getF(PersistenceHelper.CURRENT_VERSION_OF_SPINNERS));
	}

	@Override
	protected Type getEssenceType() {
		return SpinnerDefinition.class;
	}

	@Override
	protected void setFrozenVersion(float version) {
		ph.put(PersistenceHelper.CURRENT_VERSION_OF_SPINNERS,version);
		
	}

	public boolean isRequired() {
		return false;
	}

	private String curId=null;
	private List<SpinnerElement>sl = null;


	@Override
	public LoadResult parse(String row, Integer currentRow) {
			if (currentRow==1) {
				Log.d("vortex","skip header: "+row);
				return null;
			}
			//Split into lines.			
			String[]  r = Tools.split(row);
			if (r.length<noOfRequiredColumns) {
				o.addCriticalText("Too short row in spinnerdef file. Row #"+currentRow+" has "+r.length+" columns but should have "+noOfRequiredColumns+" columns");
			for (int i=0;i<r.length;i++) {
				o.addText("R"+i+":"+r[i]);
			}
			String errMsg = "Spinnerdef file corrupt. Check Log for details";
			return new LoadResult(this,ErrorCode.ParseError,errMsg);
			} else {
				String id = r[0];
				if (!id.equals(curId)) {
					if (c!=0) 
						o.addText("List had "+c+" members");
					c=0;			
					o.addText("Adding new spinner list with ID "+curId);
					Log.d("vortex","Added new spinner element. ID "+curId);
					sl = new ArrayList<SpinnerElement>();
					sd.add(id, sl);
					curId = id;
		
				}
				//Log.d("vortex","Added new spinner element. ID "+curId);
				sl.add(sd.new SpinnerElement(r[1],r[2],r[3],r[4]));
				c++;
			}
			//good!
			return null;
			//mCallBack.onUpdate(pHelper.getCurrentRow(),this.getNumberOfLinesInRawData());
		}


	
	@Override
    public LoadResult prepare() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setEssence() {
		essence = sd;
	}
		
		
		
	}

	

	

