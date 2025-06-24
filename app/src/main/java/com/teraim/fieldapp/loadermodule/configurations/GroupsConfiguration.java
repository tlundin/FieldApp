package com.teraim.fieldapp.loadermodule.configurations;

import android.content.Context;
import android.util.Log;

import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.loadermodule.CSVConfigurationModule;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupsConfiguration extends CSVConfigurationModule {

	private final LoggerI o;
	private boolean scanHeader;
	private String[] groupsFileHeaderS;
	private Map <String, List<List<String>>> groups;
	private int groupIndex = -1;
	private int nameIndex = -1;
	private static GroupsConfiguration singleton=null;

	public GroupsConfiguration(Context context, PersistenceHelper globalPh, PersistenceHelper ph, String serverOrFile, String bundle, LoggerI debugConsole) {
		super(context,globalPh,ph, serverOrFile, "Groups", "Group module            ");
		o = debugConsole;
		singleton = null;
		o.addRow("Parsing Groups.csv file");
	}

	public static GroupsConfiguration getSingleton() {
		return singleton;
	}

	@Override
    public LoadResult prepare() {
		scanHeader=true;
		groups=new HashMap<String,List<List<String>>>();
		groupIndex = -1;
		nameIndex = -1;
		Log.d("vortex","in prepare for groups");
		singleton = this;
		return null;
	}

	


	@Override
	public LoadResult parse(String row, Integer currentRow) {
		//Log.d("vortex","group parsing "+row);
		//if no header, abort.
		if (scanHeader && row == null) {
			o.addRow("");
			o.addRedText("Header missing. Load cannot proceed");
			return new LoadResult(this,ErrorCode.ParseError);
		}
		//Scan header.
		if (scanHeader && row!=null) {
			Log.d("vortex","Header for groups is "+row);

			groupsFileHeaderS = row.split(",");
			o.addRow("Header for Groups file: "+row);
			o.addRow("Has: "+groupsFileHeaderS.length+" elements");
			scanHeader = false;
			//Go through varpattern. Generate rows for the master table.
			//...but first - find the key columns in Artlista.


			//Find the Variable key row.
			for (int i = 0; i<groupsFileHeaderS.length;i++) {
				if (groupsFileHeaderS[i].trim().equals(VariableConfiguration.Col_Functional_Group))
					groupIndex = i;
				else if  (groupsFileHeaderS[i].trim().equals(VariableConfiguration.Col_Variable_Name))
					nameIndex = i;
			}

			if (nameIndex ==-1 || groupIndex == -1) {
				o.addRow("");
				o.addRedText("Header missing either name or functional group column. Load cannot proceed");
				o.addRow("Header:");
				o.addRow(row);
				return new LoadResult(this,ErrorCode.ParseError);
			}
		} else {
			//Split config file into parts according to functional group.
			String[] r = Tools.split(row);
			if (r!=null && r.length>groupIndex) {
				for(int i=0;i<r.length;i++) {
					if (r[i]!=null)
						r[i] = r[i].replace("\"", "");
				}						
				String group = r[groupIndex];
				//Add group if not already found
				List<List<String>> elem = groups.get(group); 
				if (elem==null) {
					elem = new ArrayList<List<String>>();
					groups.put(group,elem);
				}
				elem.add(Arrays.asList(r));
			} else {
				o.addRow("");
				o.addRedText("Impossible to split row #"+currentRow);
				o.addRow("ROW that I cannot parse:");
				o.addRow(row);
				return new LoadResult(this,ErrorCode.ParseError);
			}
		}
		return null;
	}



	@Override
	public float getFrozenVersion() {
		return (ph.getF(PersistenceHelper.CURRENT_VERSION_OF_GROUP_CONFIG_FILE));
	}

	@Override
	protected Type getEssenceType() {
		return GroupsConfiguration.class;
	}

	@Override
	protected void setFrozenVersion(float version) {
		ph.put(PersistenceHelper.CURRENT_VERSION_OF_GROUP_CONFIG_FILE,version);
	}

	public boolean isRequired() {
		return false;
	}

	
	public String[] getGroupFileColumns() {		
		return groupsFileHeaderS;
	}

	public int getNameIndex() {
		return nameIndex;
	}
	
	public Map <String, List<List<String>>> getGroups() {
		return groups;
	}

	public int getGroupIndex() {
		// TODO Auto-generated method stub
		return groupIndex;
	}

	@Override
	public void setEssence() {
		essence=groups;
	}
}
