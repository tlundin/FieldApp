package com.teraim.fieldapp.loadermodule.configurations;

import android.content.Context;
import android.util.Log;

import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.SpinnerDefinition;
import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.dynamic.types.Table.ErrCode;
import com.teraim.fieldapp.loadermodule.CSVConfigurationModule;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class VariablesConfiguration extends CSVConfigurationModule {

	
	private final SpinnerDefinition sd=new SpinnerDefinition();
	private final LoggerI o;
    private Table myTable=null;
	private List<String> cheaderL;
	private boolean scanHeader;
	private String[] groupsFileHeaderS;
	private Map<String, List<List<String>>> groups;
	private int nameIndex;
	private int groupIndex;


	public VariablesConfiguration(Context context, PersistenceHelper globalPh, PersistenceHelper ph, String serverOrFile, LoggerI debugConsole) {
		super(context, globalPh,ph, serverOrFile,VariablesConfiguration.NAME,"Variables module      ");
		this.o = debugConsole;
		o.addRow("Parsing Variables.csv file");

	}

	public static final String NAME = "Variables";
	

	@Override
	public float getFrozenVersion() {
		//Force reload of this file if Groups has been loaded.
		if (GroupsConfiguration.getSingleton()!=null) {
			return -1;
		}
		return (ph.getF(PersistenceHelper.CURRENT_VERSION_OF_VARPATTERN_FILE));
	}
	@Override
	public Table getEssence() {
		return (Table) essence;
	}
	@Override
	protected Type getEssenceType() {
		return Table.class;
	}

	@Override
	protected void setFrozenVersion(float version) {
		ph.put(PersistenceHelper.CURRENT_VERSION_OF_VARPATTERN_FILE,version);

	}

	public boolean isRequired() {
		return true;
	}

	private GroupsConfiguration gc=null;
	
	@Override
    public LoadResult prepare() throws Dependant_Configuration_Missing {
		cheaderL = new ArrayList<String>();
		scanHeader = true;
		//check if there is a groups configuration.
		if ((gc=GroupsConfiguration.getSingleton()) != null) {
			groupsFileHeaderS = gc.getGroupFileColumns();
			groups = gc.getGroups();
			nameIndex = gc.getNameIndex();
			groupIndex = gc.getGroupIndex();
		} else {
			Log.d("vortex","No groups configuration found.");
			throw new Dependant_Configuration_Missing("Groups");
		}
		return null;
	}



	@Override
	public LoadResult parse(String row, Integer currentRow) {


        int pNameIndex = 2;
        if (scanHeader && row!=null) {
			Log.d("vortex","header is: "+row);			
			String[] varPatternHeaderS = row.split(",");
			if (varPatternHeaderS==null||varPatternHeaderS.length<Constants.VAR_PATTERN_ROW_LENGTH) {
				o.addRow("");
				o.addRedText("Header corrupt in Variables.csv: "+ Arrays.toString(varPatternHeaderS));
				return new LoadResult(this,ErrorCode.ParseError,"Corrupt header");
			}
			//Remove duplicte group column and varname if group file present. 
			
			if (gc!=null) {
				boolean foundFunctionalGroupHeader=false,foundVarNameHeader=false;
				Collections.addAll(cheaderL,groupsFileHeaderS);
				Log.d("vortex","header now "+cheaderL.toString());
				
				Iterator<String> it = cheaderL.iterator();
				while(it.hasNext()) {
					String header = it.next();
					if (header.equals(VariableConfiguration.Col_Functional_Group)) {
						Log.d("vortex","found column Functional Group "); 
						foundFunctionalGroupHeader=true;
						it.remove();
						}
					else
						if (header.equals(VariableConfiguration.Col_Variable_Name)) {
							Log.d("vortex","found column VariableName");
							foundVarNameHeader=true;
							it.remove();
						}
				}
				if (!foundFunctionalGroupHeader||!foundVarNameHeader) {
					o.addRow("");
					o.addRedText("Could not find required columns "+VariableConfiguration.Col_Functional_Group+" or "+VariableConfiguration.Col_Variable_Name);
					return new LoadResult(this,ErrorCode.ParseError,"Corrupt header");
				}

			}
			List<String> vheaderL = new ArrayList<String>(trimmed(varPatternHeaderS));
			vheaderL.addAll(cheaderL);
			myTable = new Table(vheaderL,0, pNameIndex);
			scanHeader=false;

		} else {
			List<List<String>> elems;
			String[] r = Tools.split(row);
			if (r==null|| r.length<Constants.VAR_PATTERN_ROW_LENGTH) {
				o.addRow("");
				o.addRedText("Too short row or row null in Variable.csv.");
				if (r!=null) {
					o.addRow("");
					o.addRedText("Row length: "+r.length+". Expected length: "+Constants.VAR_PATTERN_ROW_LENGTH);
				} else
					o.addRow("NULL!!!");
				return new LoadResult(this,ErrorCode.ParseError,"Parse error, row: "+currentRow+1);
			} else {	
				for(int i=0;i<r.length;i++) {
					if (r[i]!=null)
						r[i] = r[i].replace("\"", "");
				}
                int pGroupIndex = 1;
                String pGroup = r[pGroupIndex];
				List<String> trr=trimmed(r);
				if (pGroup==null || pGroup.trim().length()==0) {
					//Log.d("nils","found variable "+r[pNameIndex]+" in varpattern");							
					myTable.addRow(trr);
					//o.addRow("Generated variable(1): ["+r[pNameIndex]+"]");
					//Log.d("vortex","Generated variable ["+r[pNameIndex]+"] ROW:\n"+row);
				} else {
					//Log.d("nils","found group name: "+pGroup);
					elems = groups.get(pGroup);
					String varPatternName = r[pNameIndex];
					if (elems==null) {
						//If the variable has a group,add it 
						//Log.d("nils","Group "+pGroup+" in line#"+rowC+" does not exist in config file. Will use name: "+varPatternName);								
						String name = pGroup.trim()+Constants.VariableSeparator+varPatternName.trim();
						//o.addRow("Generated variable(2): ["+name+"]");
						trr.set(pNameIndex, name);
						myTable.addRow(trr);
					} else {
						for (List<String>elem:elems) {
							//Go through all rows in group. Generate variables.
							String cFileNamePart = elem.get(nameIndex);

							if (varPatternName==null) {
								o.addRow("");
								o.addRedText("varPatternNamepart evaluates to null at line#"+(currentRow+1)+" in varpattern file");
							} else {
								String fullVarName = pGroup.trim()+Constants.VariableSeparator+(cFileNamePart!=null?cFileNamePart.trim()+Constants.VariableSeparator:"")+varPatternName.trim();
								//Remove duplicate elements from Config File row.
								//Make a copy.
								List<String>elemCopy = new ArrayList<String>(elem);
								elemCopy.remove(nameIndex);
								elemCopy.remove(groupIndex);
								List<String>varPatternL = new ArrayList<String>(trimmed(r));
								varPatternL.addAll(elemCopy);
								//Replace name column with full name.
								varPatternL.set(pNameIndex, fullVarName);
								//o.addRow("Generated variable(3): ["+fullVarName+"]");
								ErrCode err = myTable.addRow(varPatternL);
								if (err!=ErrCode.ok) {
									switch (err) {
									case keyError:
										o.addRow("");
										o.addRedText("KEY ERROR!");
										break;
									case tooFewColumns:
										o.addRow("");
										o.addRedText("TOO FEW COLUMNS!");
										return new LoadResult(this,ErrorCode.ParseError);
									case tooManyColumns:
										o.addRow("");
										o.addRedText("TOO MANY COLUMNS!");
										o.addRedText("row not inserted. Something wrong at line "+currentRow);
										break;
									}
								}
							}
						}
					}
				}
			}
		}
		return null;
	}
	private List<String> trimmed(String[] r) {
        return new ArrayList<String>(Arrays.asList(r).subList(0, Constants.VAR_PATTERN_ROW_LENGTH));
	}

	@Override
	public void setEssence() {
		essence = myTable;
	}

}
