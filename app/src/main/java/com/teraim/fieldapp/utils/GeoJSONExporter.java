package com.teraim.fieldapp.utils;

import android.app.Activity;
import android.content.Context;
import android.util.JsonWriter;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisConstants;
import com.teraim.fieldapp.log.LogRepository;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.ui.ExportDialogInterface;
import com.teraim.fieldapp.utils.DbHelper.DBColumnPicker;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class GeoJSONExporter extends Exporter {

    private JsonWriter writer;
	private final List<String> coordLess = new ArrayList<>();
	private final Map<String,String> rutMap = new HashMap<>();
	private final Map<String,String> authorMap = new HashMap<>();
	private int varC=0,exx=0;


	GeoJSONExporter(Context ctx, ExportDialogInterface eDialog) {
		super(ctx,eDialog);

	}

	@Override
	public Report writeVariables(DBColumnPicker cp) {

		LogRepository o = LogRepository.getInstance();
        StringWriter sw = new StringWriter();
		writer = new JsonWriter(sw);
		try {
			if (cp!=null && cp.moveToFirst()) {
				writer.setIndent("  ");
				//Begin main obj
				writer.beginObject();
				Log.d("nils","Writing header");
				write("name","Export");
				write("type","FeatureCollection");
				writer.name("crs");
				writer.beginObject();
				write("type","name");
				writer.name("properties");
				writer.beginObject();
				write("name","EPSG:3006");
				writer.endObject();
				//end header
				writer.endObject();
				writer.name("features");
				writer.beginArray();

				Map<String,String> currentHash=null;

				//gisobjects: A map between UID and variable key-value pairs.
				HashMap<String, Map<String, Map<String, String>>> gisObjects=null;


				Map<String, String> gisObjM;
				do {
					String uid=null,sub=null;
					currentHash = cp.getKeyColumnValues();
					if (currentHash==null) {
						o.addText("");
						o.addCriticalText("Missing keyHash!");
						Log.e("vortex","Missing keyHash!");
						continue;
					}
					uid = currentHash.get("uid");
					rutMap.put(uid,currentHash.get(NamedVariables.AreaTerm));

					sub = currentHash.get("sub");
					Log.d("SUB","Current HASH "+currentHash.toString());


					//Log.d("botox","CURRENT_HASH: "+currentHash);
					if (uid==null) {
						Log.e("vortex","missing uid!!!");
						Log.e("vortex","keyhash: "+currentHash);
					}
					else {
						if (gisObjects==null)
							gisObjects = new HashMap<String,Map<String,Map<String,String>>>();
						//Find maps per sub.
						Map<String, Map<String, String>> gisObjH = gisObjects.get(uid);
						if (gisObjH==null) {
							//No subs at all? Create new sub.
							gisObjH = new HashMap<String, Map<String, String>>();
							gisObjects.put(uid,gisObjH);
						}
						//Find correct "sub" for this uid. if sub doesnt exist, create.
						gisObjM = gisObjH.get(sub);
						if (gisObjM==null) {
							gisObjM = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
							gisObjH.put(sub, gisObjM);
							if (sub == null)
								gisObjM.put("GISTYP", currentHash.get(GisConstants.TYPE_COLUMN));
							//Log.d("vortex","keyhash: "+currentHash.toString());
						}

						//Hack for multiple sub1 variables.
						List<String> row;
						if (cp.getVariable()!=null) {
							String name = cp.getVariable().name;
							//Try to find in variable config.
							row = gs.getVariableConfiguration().getCompleteVariableDefinition(name);
							if (row!=null) {
								name =  gs.getVariableConfiguration().getVarName(row);
							}
							authorMap.put(uid,cp.getVariable().creator);

							if (name!=null) {
								//Check if this variable is supposed to be exported.
								boolean isLocal = gs.getVariableConfiguration().isLocal(gs.getVariableConfiguration().getCompleteVariableDefinition(name));
								if (isLocal) {
									//Log.d("vortex","skipping "+name+" since it is marked local");
									//o.addText("skipping "+name+" since it is marked local");

								} else {
									gisObjM.put(name, cp.getVariable().value);
									gisObjM.put("ts_" + name, cp.getVariable().timeStamp);
									varC++;
								}
							}
							else {
								o.addText("");
								o.addCriticalText("Variable name was null!");
							}
						} else {
							o.addText("");
							o.addCriticalText("Variable was null!");
						}
					}
					if (ctx != null)
						((Activity)ctx).runOnUiThread(new Runnable() {
							@Override
							public void run() {
								eDialog.setGenerateStatus("Objects: #"+varC);
							}
						});

				} while (cp.next());
				Log.d("vortex","now inserting into json.");
				//For each fixedGid (uid)...
				if (gisObjects!=null) {
					final int sz = gisObjects.keySet().size();
					int curr = 0;
					for (final String keyUID:gisObjects.keySet()) {
						//Log.d("vortex", "sub sets under " + keyUID);
						final int cf = curr++;
						if (ctx != null)
							((Activity)ctx).runOnUiThread(new Runnable() {
								@Override
								public void run() {
									eDialog.setGenerateStatus("Writing: ("+(cf+1)+"/"+sz+")");
								}
							});
						Map<String, Map<String, String>> gisObjH = gisObjects.get(keyUID);

						//First do the default.
						gisObjM = gisObjH.get(null);
						if (gisObjM==null) {
							Log.e("vortex", "NULL gisobjM.Keys: "+gisObjH.keySet());
							gisObjM = gisObjH.get(gisObjH.keySet().iterator().next());
							//String coor = gisObjM.remove(GisConstants.GPS_Coord_Var_Name);
							//Log.d("vortex","COOR is "+coor);

							continue;
						}
						String coordinates = getCoordinates(keyUID, gisObjM.remove(GisConstants.GPS_Coord_Var_Name));

						String[] polygons = null;
						if (coordinates == null) {
							//Try to find the object from historical. If not possible, sound alert.
							//o.addText("");
							//o.addCriticalText("Object " + keyUID + " is missing GPS Coordinates!!!");
							Log.e("fortex", "Object " + keyUID + " is missing GPS Coordinates!!!");

							//for (String mKey : gisObjM.keySet()) {
							//	Log.d("fortex","variable: " + mKey + ", value: " + gisObjM.get(mKey));
							//}



							//coordinates = "0,0";

							//check for coordinates from global id.
							String uidFromGlobal =   GlobalState.getInstance().getDb().findUIDFromGlobalId(keyUID);

							if (uidFromGlobal !=null) {
								//remove braces.
								//uidFromGlobal = uidFromGlobal.replaceAll("[{}]", " ");
								//Log.d("vortex","trying to find coords from globalId");
								coordinates = getCoordinates(uidFromGlobal, gisObjM.remove(GisConstants.GPS_Coord_Var_Name));

							}
							if (coordinates==null) {
								if (uidFromGlobal !=null) {
									o.addText("");
									o.addCriticalText("Failed to find replacement coord with " + uidFromGlobal + " for [" + keyUID + "]");
								} else
									Log.d("vortex","uidfromglobal failed for "+keyUID);
								coordLess.add(keyUID);
								continue;
							} else {
								o.addText("");
								o.addCriticalText("Found replacement coords with "+uidFromGlobal+" ["+keyUID+"]");
								String objectId = GlobalState.getInstance().getDb().findVarFromUID(uidFromGlobal,GisConstants.ObjectID);
								String geoT= GlobalState.getInstance().getDb().findVarFromUID(uidFromGlobal,GisConstants.Geo_Type);
								if (objectId!=null) {
									o.addText("Found object ID...");
									gisObjM.put(GisConstants.ObjectID,objectId);
								}
								if (geoT!=null) {
									o.addText("Found geotype...: "+geoT);
									gisObjM.put(GisConstants.Geo_Type,geoT);
								}
							}

						}
						polygons = coordinates.split("\\|");
						String geoType= gisObjM.get(GisConstants.Geo_Type);
						Log.d("gorgon","geotype: "+geoType);
							//Try to figure out geotype from number of coordinates
							if (geoType==null) {
								if (polygons.length == 1) {
									String[] cs = polygons[0].split(",");
									if (cs != null) {
										if (cs.length == 2)
											geoType = "Point";
										else if (cs.length > 2)
											geoType = "Polygon";
									}
								} else if (polygons.length >= 2) {
									geoType = "Polygon";

								}
							}
						if (geoType==null){
							Log.d("brex","WRONG: "+polygons[0]);
							o.addText("");
							o.addCriticalText("Failed to assign geotype for " + keyUID+" with polygons length = "+polygons.length+" and coordinates "+coordinates);
							continue;
						}
						boolean isPoly= "Polygon".equalsIgnoreCase(geoType);
						boolean isLineString = "Linestring".equalsIgnoreCase(geoType);
						if (isLineString)
							geoType="LineString";
						if (isPoly) {
							String firstPoly = polygons[0];
							String[] coordio = firstPoly.split(",");
							if (coordio.length<6){
								exx++;
								Log.e("bengo","polygon has "+coordio.length+" count:"+exx);
								continue;
							}
						}
						//Beg of line.
						writer.beginObject();
						write("type", "Feature");
						writer.name("geometry");
						writer.beginObject();
						write("type", geoType);
						writer.name("coordinates");
						Log.d("brex","RUTA: "+rutMap.get(keyUID));
						Log.d("brex","GID: "+keyUID);

						if (isPoly||isLineString)
							writer.beginArray();

						for (String polygon : polygons) {

							String[] coords = polygon.split(",");
							if (isPoly)
								writer.beginArray();
							Log.d("vortex", "is poly? "+isPoly);
							Log.d("vortex", "geotype is  "+geoType);
							Log.d("vortex", "Length is "+coords.length);
							try {
								for (int i = 0; i < coords.length; i += 2) {
									Log.d("vortex", "coord [" + i + "] :" + coords[i]+" [" + (i+1) + "] :" + coords[i+1]);
									writer.beginArray();
									printCoord(writer, coords[i]);
									printCoord(writer, coords[i + 1]);
									writer.endArray();
								}
								//Close poly if not done.
								if (isPoly && !lastCoordEqualToFirst(coords)) {
									Log.d("vortex","Closing poly..");
									writer.beginArray();
									printCoord(writer, coords[0]);
									printCoord(writer, coords[1]);
									writer.endArray();
								}



							} catch (IllegalStateException e) {
								Log.e("brex","Illegalstate!!");
								Log.e("brex","Full Poly is "+polygon);
							}
							if (isPoly)
								writer.endArray();
						}
						if (isPoly||isLineString)
							writer.endArray();
						//End geometry.
						writer.endObject();
						writer.name("properties");
						writer.beginObject();
						//Add the UUID
						write(GisConstants.FixedGid, "{"+keyUID+"}");
						String ruta = rutMap.get(keyUID);
						String author = authorMap.get(keyUID);
						if (ruta!=null)
							write(NamedVariables.AreaTerm.toUpperCase(),ruta);
						write("author", author);
						//write("timestamp",cp.getVariable().timeStamp);
						//write("author",cp.getKeyColumnValues().get("author"));
						for (String mKey : gisObjM.keySet()) {
							write(mKey, gisObjM.get(mKey));
							//Log.d("volde", "var, value: " + mKey + "," + gisObjM.get(mKey));
						}
						//Check if there are other sub than default.
						if (gisObjH != null && !gisObjH.isEmpty()) {
							writer.name("sub");
							writer.beginArray();
							for (String key : gisObjH.keySet()) {
								if (key == null) {
									//Log.d("volde", "skipping null key");
									continue;
								}
								//Log.d("volde", "found some extra under " + key);
								gisObjM = gisObjH.get(key);
								if (gisObjM != null) {
									writer.beginObject();
									write("sub".toUpperCase(Locale.ROOT), key);
									for (String mKey : gisObjM.keySet()) {
										write(mKey, gisObjM.get(mKey));
										//Log.d("volde", "var, value: " + mKey + "," + gisObjM.get(mKey));
									}
									writer.endObject();
								}

							}
							writer.endArray();
						}
						writer.endObject();

						//eol
						writer.endObject();

					}

				} else {
					o.addText("");
					o.addCriticalText("GisObjects was null!");
					return new Report(ExportReport.NO_DATA);
				}
				//End of array.
				writer.endArray();
				//End of all.
				writer.endObject();

				Log.d("nils","finished writing JSON");
				//Log.d("nils", sw.toString());
				if (!coordLess.isEmpty()&&globalPh.getB(PersistenceHelper.DEVELOPER_SWITCH)) {
					o.addText("");
					o.addCriticalText("No coordinates found for these objects:");
					for (String keyUID:coordLess) {
						o.addCriticalText(keyUID);
					}
				}
				return new Report(sw.toString(),varC);
			}else
				Log.e("vortex","EMPTY!!!");
		} catch (Exception e) {
			Tools.printErrorToLog(LogRepository.getInstance(), e, null);
			cp.close();
		} finally {
			cp.close();
		}

		return null;	}

	private boolean lastCoordEqualToFirst(String[] coords) {
		return (coords==null || coords.length==0 || (coords[0].equals(coords.length-2) &&
				coords[1].equals(coords.length-1))) ;


	}

	private String getCoordinates(String uid, String thisYear) {
		//If there is a coord for this year, return it.
		if (thisYear!=null)
			return thisYear;
		//otherwise, search historical for uid.
		return
				GlobalState.getInstance().getDb().findVarFromUID(uid,GisConstants.GPS_Coord_Var_Name);
	}

	private void printCoord(JsonWriter writer, String coord) {
		try {
			if (coord == null || "null".equalsIgnoreCase(coord)) {
				Log.e("vortex", "coordinate was null in db. ");

				writer.nullValue();

			} else {
				try {
					writer.value(Float.parseFloat(coord));
				} catch (NumberFormatException e) {
					writer.nullValue();
				}
            }
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getType() {
		return "json";
	}

	private void write(String name,String value) throws IOException {
		String val = (value==null||value.length()==0)?"NULL":value;
		writer.name(name).value(val);
	}



}
