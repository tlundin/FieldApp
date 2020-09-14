package com.teraim.fieldapp.dynamic.blocks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.ArrayVariable;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.GisLayer;
import com.teraim.fieldapp.dynamic.types.SweLocation;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.Variable.DataType;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.DynamicGisPoint;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GIS;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisConstants;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisMultiPointObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisObjectsMenu;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisPolygonObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.StaticGisPoint;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.WF_Gis_Map;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.DbHelper.DBColumnPicker;
import com.teraim.fieldapp.utils.DbHelper.Selection;
import com.teraim.fieldapp.utils.DbHelper.StoredVariableData;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AddGisPointObjects extends Block implements FullGisObjectConfiguration {


	private static final long serialVersionUID = 7979886099817953005L;
	private final boolean useIconOnMap;
	private final String nName;
	private final String target;
	private String coordType;
	private final String locationVariables;
	private final String imgSource;
	private final String refreshRate;

	private Bitmap icon=null;
	private final boolean isVisible;
	private final GisObjectType myType;
	private boolean loadDone=false;
	private Set<GisObject> myGisObjects;
	private float radius;
	private final String color;
	private Paint.Style fillType;
	private PolyType polyType;
	private final String onClick;
	private final String statusVariable;
	private DB_Context objectKeyHash;
	private final boolean isUser;
	private final boolean createAllowed;
	private boolean dynamic;
	private final List<EvalExpr>labelE;
	private final List<Expressor.EvalExpr> objContextE;
	private final String unevaluatedLabel;
	private String lastCheckTimeStamp;
	private String palette;
	private String creator;

	public AddGisPointObjects(String id, String nName, String label,
							  String target, String objectContext,String coordType, String locationVars,
							  String imgSource,boolean use_image_icon_on_map, String refreshRate, String radius, boolean isVisible,
							  GisObjectType type, String color, String polyType, String fillType,
							  String onClick, String statusVariable, boolean isUser, boolean createAllowed, String palette, LoggerI o) {
		super();
		this.blockId = id;
		this.nName = nName;
		this.target = target;
		this.coordType = coordType;
		this.locationVariables = locationVars;
		this.imgSource = imgSource;
		this.isVisible = isVisible;
		this.refreshRate=refreshRate;
		this.onClick = onClick;
		this.color=color;
		this.statusVariable=statusVariable;
		this.isUser=isUser;
		this.createAllowed=createAllowed;
		this.palette = palette;
		this.creator = "";
		this.useIconOnMap = use_image_icon_on_map;
		myType = type;

		if (coordType==null||coordType=="")
			this.coordType=GisConstants.SWEREF;
		else
			Log.e("vortex","LATLONG!");
		setRadius(radius);

		this.fillType=Paint.Style.FILL;
		if  (fillType !=null) {
			if (fillType.equalsIgnoreCase("STROKE"))
				this.fillType = Paint.Style.STROKE;
			else if (fillType.equalsIgnoreCase("FILL_AND_STROKE"))
				this.fillType = Paint.Style.FILL_AND_STROKE;
		}
		this.polyType=PolyType.circle;
		this.radius=10;
		if (polyType!=null) {
			try {
				this.polyType=PolyType.valueOf(polyType);
			} catch (IllegalArgumentException e) {
				if (polyType.toUpperCase().equals("SQUARE")||polyType.toUpperCase().equals("RECT")||polyType.toUpperCase().equals("RECTANGLE"))
					this.polyType=PolyType.rect;
				else if (polyType.toUpperCase().equals("TRIANGLE"))
					this.polyType=PolyType.triangle;
				else {
					o.addRow("");
					o.addRedText("Unknown polytype: ["+polyType+"]. Will default to circle");
				}
			}
		}

		if (Tools.isNumeric(radius)) {
			this.radius=Float.parseFloat(radius);

		}

		labelE = Expressor.preCompileExpression(label);
		this.unevaluatedLabel=label;
		objContextE = Expressor.preCompileExpression(objectContext);
		//Set default icons for different kind of objects.
	}

	//Assumed that all blocks will deal with "current gis".
	public void create(WF_Context myContext) {
		create(myContext, false);
	}

	//Refresh: only add new objects created after last check.

	public void create(WF_Context myContext, boolean refresh) {
		setDefaultBitmaps(myContext);
		o = GlobalState.getInstance().getLogger();
		GlobalState gs = GlobalState.getInstance();
		GIS gisB = myContext.getCurrentGis();
		if (gisB==null) {
			Log.e("vortex","gisB null!!");
			return;
		} else {
			if (createAllowed) {
				Log.d("vortex","Adding type to create menu for "+nName);
				if (palette==null ||palette.isEmpty())
					palette = GisObjectsMenu.Default;
				gisB.addGisObjectType(this,palette);
			}
			if (gisB.isZoomLevel() && !refresh) {
				Log.d("vortex","Zoom level!..use existing gis objects!");
				return;
			}
			if (refresh) {
				Log.d("vortex","This is a refreshcall!");
				if (dynamic || this.onClick==null) {
					//Dynamic objects are refreshed by themselves.
					//Objects with no workflow cannot be changed by sync. Thus, no need to refresh.
					Log.d("vortex","No need to refresh "+this.getName());
					return;
				}

			}
		}


		if (imgSource!=null&&imgSource.length()>0 ) {
			File cached = gs.getCachedFileFromUrl(imgSource);
			if (cached==null) {
				Log.d("vortex","no cached image...trying live.");
				String fullPicURL = Constants.VORTEX_ROOT_DIR+GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.BUNDLE_NAME)+"/extras/"+imgSource;
				Log.d("vortex","IMGURL: "+imgSource);
				new DownloadImageTask()
						.execute(fullPicURL);
			} else {
				try {
					icon = BitmapFactory.decodeStream(new FileInputStream(cached));
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}




		//Generate the context for these objects.
		objectKeyHash = DB_Context.evaluate(objContextE);
		//Use current year for statusvar.
		//Log.d("vortex","Curryear HASH "+currYearH.toString());
		if (objectKeyHash==null || !objectKeyHash.isOk()) {
			Log.e("vortex","keychain  null or faulty: "+objectKeyHash);
			return;
		}
		//Log.d("vortex","OBJ CONTEXTS: "+objectContextS+" OBJ KEYHASH "+objectKeyHash.toString());
		Map<String, String> currYearH = Tools.copyKeyHash(objectKeyHash.getContext());
		//TODO: FIX THIS.
		currYearH.put("år",Constants.getYear());

		if (locationVariables==null || locationVariables.isEmpty()) {
			Log.e("vortex","Missing GPS Location variable(s)!");
			o.addRow("");
			o.addRedText("Missing GPS Location variable(s). Cannot continue..aborting");
			return;
		}

		//Log.d("vortex","In onCreate for AddGisP for ["+nName+"], Obj_type: "+myType+" with keychain: "+objectKeyHash);
		//Call to the database to get the objects.

		//Either one or two location variables.
		//If One, separate X,Y by comma
		//If two, One for X & one for Y.
		//Log.d("vortex","Location variables: "+locationVariables);
		//Try split.
		String[] locationVarArray = locationVariables.split(",");
		if (locationVarArray.length>2){
			Log.e("vortex","Too many GPS Location variables! Found "+locationVarArray.length+" variables.");
			o.addRow("");
			o.addRedText("Too many GPS Location variables! Found "+locationVarArray.length+" variables. You can only have either one with format GPS_X,GPS_Y or one for X and one for Y!");
			return;
		}
		String locationVar1=locationVarArray[0].trim();
		String locationVar2=null;

		boolean twoVars = (locationVarArray.length==2);
		if (twoVars)
			locationVar2 = locationVarArray[1].trim();
		//Log.d("vortex","Twovars is "+twoVars+" gisvars are: "+(twoVars?" ["+locationVarArray[0]+","+locationVarArray[1]:locationVarArray[0])+"]");
		if(twoVars && myType.equals(GisObjectType.Multipoint)) {
			Log.e("vortex","Multivar on multipoint!");
			o.addRow("");
			o.addRedText("Multipoint can only have one Location variable with comma separated values, eg. GPSX_1,GPSY_1,...GPSX_n,GPSY_n");
			return;
		}
		VariableConfiguration al = GlobalState.getInstance().getVariableConfiguration();



		Selection coordVar1S=null,coordVar2S=null,statusVarS = null;
		DBColumnPicker pickerLocation1,pickerLocation2=null,pickerStatusVars=null;

		//save timestamp for refresh.
		String thisCheck = System.currentTimeMillis() + "";
		if (lastCheckTimeStamp == null)
			lastCheckTimeStamp = thisCheck;

		if (this.getStatusVariable()!=null) {

			statusVarS = GlobalState.getInstance().getDb().createSelection(currYearH, this.getStatusVariable().trim());
			/*
			if (refresh) {
				statusVarS.selection+=" and timestamp > ?";
				String[] s =Arrays.copyOf(statusVarS.selectionArgs, statusVarS.selectionArgs.length+1);
				s[statusVarS.selectionArgs.length] = lastCheckTimeStamp;
				statusVarS.selectionArgs = s;
			}
			*/
			pickerStatusVars = GlobalState.getInstance().getDb().getAllVariableInstances(statusVarS);
		}

		//superhack

		coordVar1S = GlobalState.getInstance().getDb().createSelection(objectKeyHash.getContext(), locationVar1);
		//If this is a refresh, dont fetch anything that has already been fetched.
		/*if (refresh) {
			coordVar1S.selection+=" and timestamp > ?";
			String[] s =Arrays.copyOf(coordVar1S.selectionArgs, coordVar1S.selectionArgs.length+1);
			s[coordVar1S.selectionArgs.length] = lastCheckTimeStamp;
			coordVar1S.selectionArgs = s;
		}
		*/
		Log.d("vortex","selection: "+coordVar1S.selection);
		Log.d("vortex","sel args: "+print(coordVar1S.selectionArgs));
		List<String> completeVariableDefinitionForL1 = al.getCompleteVariableDefinition(locationVar1);
		List<String> completeVariableDefinitionForL2 = null;

		if (completeVariableDefinitionForL1==null) {
			Log.e("vortex","Variable not found!");
			o.addRow("");
			o.addRedText("Variable "+locationVar1+" was not found. Check Variables.CSV and Groups.CSV!");
			return;
		}
		DataType t1 = al.getnumType(completeVariableDefinitionForL1);
		if (t1==DataType.array) {
			pickerLocation1 = GlobalState.getInstance().getDb().getLastVariableInstance(coordVar1S);
			Log.e("vortex","called getLast.");
		}
		else
			pickerLocation1 = GlobalState.getInstance().getDb().getAllVariableInstances(coordVar1S);

		if (twoVars) {
			completeVariableDefinitionForL2 = al.getCompleteVariableDefinition(locationVar2);
			if (completeVariableDefinitionForL2==null) {
				Log.e("vortex","Variable L2 not found!");
				o.addRow("");
				o.addRedText("Variable "+locationVar2+" was not found. Check Variables.CSV and Groups.CSV!");
				return;
			}
			DataType t2 = al.getnumType(completeVariableDefinitionForL2);
			coordVar2S = GlobalState.getInstance().getDb().createSelection(objectKeyHash.getContext(), locationVar2);

			Log.d("vortex","selection: "+coordVar2S.selection);
			Log.d("vortex","sel args: "+print(coordVar2S.selectionArgs));
			if (t2==DataType.array)
				//pickerLocation2 = GlobalState.getInstance().getDb().getAllVariableInstances(coordVar2S);
				pickerLocation2 = GlobalState.getInstance().getDb().getLastVariableInstance(coordVar2S);
			else
				pickerLocation2 = GlobalState.getInstance().getDb().getAllVariableInstances(coordVar2S);
		}
		//Static..we can generate a static GIS Point Object.
		Log.d("vortex","refreshrate is "+refreshRate);
		dynamic = false;
		if (refreshRate!=null&&refreshRate.equalsIgnoreCase("dynamic")) {
			Log.d("vortex","Setting type to dynamic for "+nName);
			dynamic = true;
		}
		Map<String, String> map1,map2;
		StoredVariableData storedVar1,storedVar2;

		final Pair<String, String> nullPair = new Pair<String, String>(null,null);

		if (pickerLocation1 !=null ) {
			myGisObjects = new HashSet<GisObject> ();
			boolean hasV1Value = pickerLocation1.moveToFirst();
			boolean hasV2Value = twoVars?pickerLocation2.moveToFirst():false;
			String v1Val = hasV1Value?pickerLocation1.getVariable().value:null;
			String v2Val = hasV2Value?pickerLocation2.getVariable().value:null;
			//No values! A dynamic variable can create new ones, so create object anyway.
            Variable v1=null,v2=null;
			if (dynamic &&(!hasV1Value ||(twoVars&&!hasV2Value))) {
				Log.e("Glapp", "no values found for gps variables.");
                v1 =  new ArrayVariable(locationVar1, "myLocationX", completeVariableDefinitionForL1, objectKeyHash.getContext(), gs, "value", v1Val, true, null);
                v2 = new ArrayVariable(locationVar1, "myLocationY", completeVariableDefinitionForL2, objectKeyHash.getContext(), gs, "value", v2Val, true, null);
                if (v1!=null && v2!=null) {
                    myGisObjects.add(new DynamicGisPoint(this, v1.getKeyChain(), v1, v2, null, null));
                }
			} else {
				if (!hasV1Value && !dynamic) {
					Log.d("vortex","No values in database for static GisPObject with name "+nName);
					o.addRow("No values in database for static GisPObject with name "+nName);
				} else {
					Map <String,Pair<String,String>> statusVarM=null;
					//Find status per UID for all geo objects. This is used to color the objects later on.
					if (pickerStatusVars!=null) {

						while (pickerStatusVars.next()) {
							String value = pickerStatusVars.getVariable().value;
							String name = pickerStatusVars.getVariable().name;
							//Log.d("vortex","STATUSVAR: "+name+" value: "+value);
							//Store status var name & value for per uuid.
							if (statusVarM == null)
								statusVarM = new HashMap<String, Pair<String, String>>();
							statusVarM.put(pickerStatusVars.getKeyColumnValues().get("uid"), new Pair<>(name, value));
						}

					} //else
					//	Log.d("gaya","PICKERSTATUSVARS NULL FOR "+statusVariable);

					Pair<String,String> statusVarP=null;
					do {
						storedVar1 = pickerLocation1.getVariable();
						//Log.d("vortex","Found "+storedVar1.value+" for "+storedVar1.name);
						map1 = pickerLocation1.getKeyColumnValues();
						//Log.d("vortex","Found columns "+map1.toString()+" for "+storedVar1.name);
						//Log.d("vortex","bitmap null? "+(icon==null));

						//If status variable has a value in database, use it.
						if (statusVarM!=null)
							statusVarP = statusVarM.get(map1.get("uid"));
						//if there is a statusvariable defined, but no value found, create a new empty variable.
						if (statusVariable !=null && statusVarP == null) {
							currYearH.put("uid",map1.get("uid"));
							statusVarP = new Pair<String, String>(statusVariable,"0");
						}
						if (statusVarP == null) {
							statusVarP = nullPair;
						}
						if (dynamic) {
							v1 =  new ArrayVariable(locationVar1, "myLocationX", completeVariableDefinitionForL1, objectKeyHash.getContext(), gs, "value", v1Val, true, null);
							//v1 = GlobalState.getInstance().getVariableCache().getCheckedVariable(pickerLocation1.getKeyColumnValues(),storedVar1.name,value,true);
						}
						if (twoVars) {
							storedVar2 = Objects.requireNonNull(pickerLocation2).getVariable();
							Log.d("Glapp","Found "+storedVar2.value+" for "+storedVar2.name);
							map2 = pickerLocation1.getKeyColumnValues();
							Log.d("Glapp","Found columns "+map2.toString()+" for "+storedVar2.name);
							if (Tools.sameKeys(map1, map2)) {
								Log.e("Glapp","key mismatch in db fetch: X key:"+map1.toString()+"\nY key: "+map2.toString());
							} else {
								if (!dynamic) {
									myGisObjects.add(new StaticGisPoint(this,map1, new SweLocation(storedVar1.value,storedVar2.value),statusVarP.first,statusVarP.second));
								}
								else {
									v2 = new ArrayVariable(locationVar1, "myLocationY", completeVariableDefinitionForL2, objectKeyHash.getContext(), gs, "value", v2Val, true, null);
									//v2 = GlobalState.getInstance().getVariableCache().getCheckedVariable(pickerLocation1.getKeyColumnValues(),storedVar2.name,value,true);
									if (v1!=null && v2!=null) {
										myGisObjects.add(new DynamicGisPoint(this, map1, v1, v2, statusVarP.first, statusVarP.second));
									}
									else {
										Log.e("Glapp","cannot create dyna 2 gis obj. One or both vars is null: "+v1+","+v2);
										continue;
									}
								}
							}
							if (!pickerLocation2.next())
								break;
						} else {
							String myTypeS = myType.toString();
							this.creator = storedVar1.creator;
							if (myTypeS.equalsIgnoreCase(GisObjectType.Point.toString())) {
								if (!dynamic) {
									//Log.d("vortex","adding "+this.getName());
									myGisObjects.add(new StaticGisPoint(this, map1, new SweLocation(storedVar1.value), statusVarP.first, statusVarP.second));
								}
								else {
									myGisObjects.add(new DynamicGisPoint(this,map1,v1,statusVarP.first,statusVarP.second));
								}
							}
							else if (myTypeS.equals(GisObjectType.Multipoint.toString())||myTypeS.equalsIgnoreCase(GisObjectType.Linestring.toString()))
								myGisObjects.add(new GisMultiPointObject(this,map1,GisObject.createListOfLocations(storedVar1.value,coordType),statusVarP.first,statusVarP.second));

							else if (myTypeS.equalsIgnoreCase(GisObjectType.Polygon.toString())) {
								//Log.d("vortex","Adding polygon");
								myGisObjects.add(new GisPolygonObject(this,map1,storedVar1.value,coordType,statusVarP.first,statusVarP.second));
							}
						}
						//Add these variables to bag

					} while (pickerLocation1.next());
					//Store timestamp for refresh calls.
					lastCheckTimeStamp = thisCheck;
					Log.d("vortex","Added "+myGisObjects.size()+" objects of type "+this.getName()==null?"null":this.getName());
				}
			}
		} else
			Log.e("vortex","picker was null");
		//Add type to layer. Add even if empty.
		if (target!=null) {//&&target.length()>0 && myGisObjects!=null && !myGisObjects.isEmpty()) {
			//Add bag to layer.
			GisLayer myLayer = myContext.getCurrentGis().getLayerFromId(target);
			if (myLayer !=null) {

				myLayer.addObjectBag(nName,myGisObjects,dynamic);

			} else {
				Log.e("vortex","Could not find layer ["+target+"] for type "+nName);
				o.addRow("");
				o.addRedText("Could not find layer ["+target+"]. This means that type "+nName+" was not added");
			}
		}
		loadDone=true;

	}


	public boolean useIconOnMap() {
		return useIconOnMap;
	}

	private void setDefaultBitmaps(WF_Context myContext) {
		if (icon==null) {

			if (myType == GisObjectType.Polygon) {

				icon = BitmapFactory.decodeResource(myContext.getContext().getResources(), R.drawable.poly);


			}

		}
	}

	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {


		protected Bitmap doInBackground(String... urls) {
			String urldisplay = urls[0];
			Bitmap mIcon11 = null;
			try {
				Log.d("vortex","Trying to load bitmap");
				InputStream in = new java.net.URL(urldisplay).openStream();
				mIcon11 = BitmapFactory.decodeStream(in);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return mIcon11;
		}

		protected void onPostExecute(final Bitmap result) {
			if(!loadDone)
				Log.e("vortex","In load bitmap postexecute, but load failed or not done");
			icon=result;
			/*
			if(!loadDone&&tries-->0) {
				Log.e("vortex","not done loading objects!!");
				final Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						onPostExecute(result);
					}
				}, 1000);
			}
			else {
				Log.e("vortex","setting bitmaps or giving up");
				icon=result;
				if(gisB!=null)
					gisB.getGis().invalidate();
			}*/
		}
	}



	public  List<EvalExpr> getLabelExpression() {
		return labelE;

	}

	public boolean isVisible() {
		return isVisible;
	}

	public float getRadius() {
		return radius;
	}

	public String getColor() {
		return color;
	}

	public Bitmap getIcon() {
		return icon;
	}

	@Override
	public GisObjectType getGisPolyType() {
		return myType;
	}


	private void setRadius(String radius) {
		if (!Tools.isNumeric(radius))
			return;
		this.radius = Float.parseFloat(radius);
	}

	private String print(String[] selectionArgs) {
		StringBuilder res= new StringBuilder();
		for (String s:selectionArgs)
			res.append(s).append(",");
		res = new StringBuilder(res.substring(0, res.length() - 1));
		return res.toString();
	}





	@Override
	public Style getStyle() {
		return fillType;
	}

	@Override
	public PolyType getShape() {
		return polyType;
	}

	@Override
	public String getClickFlow() {
		return onClick;
	}



	public String getStatusVariable() {
		return statusVariable;
	}

	public boolean isUser() {
		return isUser;
	}

	@Override
	public String getName() {
		return nName;
	}

	@Override
	public DB_Context getObjectKeyHash() {
		return objectKeyHash;
	}

	/**
	 * getLabel() returns the label before any processing.
	 */
	@Override
	public String getRawLabel() {

		return unevaluatedLabel;
	}



	public String getCreator() {
		return creator;
	}








}



