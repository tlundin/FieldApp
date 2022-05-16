package com.teraim.fieldapp.gis;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.FillType;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.GisLayer;
import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.PhotoMeta;
import com.teraim.fieldapp.dynamic.types.SweLocation;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.GisObjectType;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.PolyType;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisConstants;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisFilter;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisMultiPointObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisPathObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisPointObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisPolygonObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.StaticGisPoint;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.WF_Gis_Map;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Geomatte;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.teraim.fieldapp.GlobalState.*;

public class GisImageView extends GestureImageView implements TrackerListener {

	private final static String Deg = "\u00b0";

	//Various paints.
	private Paint txtPaint;
	private Paint polyPaint;
    private Paint vtnTxt;
    private Paint fgPaintSel;
    private Paint bCursorPaint;
    private Paint paintBlur;
	private Paint paintSimple;

	private Handler handler;
	private Context ctx;
	private final Calendar calendar = Calendar.getInstance();

	//Photometadata for the current view.
	private PhotoMeta photoMetaData;
	private Variable myX,myY;



	private static final int LabelOffset = 5;

	//Long click or short click?
	private boolean clickWasShort=false;

	//To avoid creating a new rect in onDraw.
	private Rect rectReuse = new Rect();

	//The user Gis Point Object
	private GisPointObject userGop;

	//The bag and layer that contains the object currently clicked.
	private Set<GisObject> touchedBag;
	private GisLayer touchedLayer;
	private GisObject touchedGop = null;


    private boolean candMenuVisible=false, initialized = false;

	public GisImageView(Context context) {
		super(context);
		init(context);
		//Initialize will become true after the call to "initialize" has ended.
		initialized = false;
	}

	public GisImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public GisImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}


	private CurrStatVar currentStatusVariable;

	private void init(Context ctx) {
		this.setClickable(true);
		this.ctx=ctx;
		//used for cursor blink.
		calendar.setTime(new Date());
        Paint grCursorPaint = new Paint();
		grCursorPaint.setColor(Color.GRAY);
		grCursorPaint.setStyle(Paint.Style.FILL);
        Paint blCursorPaint = new Paint();
		blCursorPaint.setColor(Color.BLUE);
		blCursorPaint.setStyle(Paint.Style.FILL);
        Paint rCursorPaint = new Paint();
		rCursorPaint.setColor(Color.RED);
		rCursorPaint.setStyle(Paint.Style.FILL);
        Paint wCursorPaint = new Paint();
		wCursorPaint.setColor(Color.WHITE);
		wCursorPaint.setStyle(Paint.Style.FILL);
		bCursorPaint = new Paint();
		bCursorPaint.setColor(Color.BLACK);
		bCursorPaint.setStyle(Paint.Style.FILL);
        Paint markerPaint = new Paint();
		markerPaint.setColor(Color.YELLOW);
		markerPaint.setStyle(Paint.Style.FILL);
		txtPaint = new Paint();
		txtPaint.setTextSize(8);
		txtPaint.setColor(Color.WHITE);
		txtPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		txtPaint.setTextAlign(Paint.Align.CENTER);
        Paint selectedPaint = new Paint();
		selectedPaint.setTextSize(8);
		selectedPaint.setColor(Color.BLACK);
		txtPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		selectedPaint.setTextAlign(Paint.Align.CENTER);
        Paint btnTxt = new Paint();
		btnTxt.setTextSize(8);
		btnTxt.setColor(Color.WHITE);
		txtPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		btnTxt.setTextAlign(Paint.Align.CENTER);
		vtnTxt = new Paint();
		vtnTxt.setTextSize(8);
		vtnTxt.setColor(Color.WHITE);
		txtPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		vtnTxt.setTextAlign(Paint.Align.CENTER);
        Paint borderPaint = new Paint();
		borderPaint.setColor(Color.WHITE);
		borderPaint.setStyle(Paint.Style.STROKE);
		borderPaint.setStrokeWidth(3);
		polyPaint = new Paint();
		polyPaint.setColor(Color.WHITE);
		polyPaint.setStyle(Paint.Style.STROKE);
		polyPaint.setStrokeWidth( 2.0f * getResources().getDisplayMetrics().density );
		polyPaint.setPathEffect( new DashPathEffect( new float[] {20,5,},0 ) );
		fgPaintSel = new Paint();
		fgPaintSel.setColor(Color.YELLOW);
		fgPaintSel.setStyle(Paint.Style.STROKE);
		fgPaintSel.setStrokeWidth(2);

		paintSimple = new Paint();
		paintSimple.setAntiAlias(true);
		paintSimple.setDither(true);
		paintSimple.setColor(Color.argb(248, 255, 255, 255));
		paintSimple.setStrokeWidth( 2.0f * getResources().getDisplayMetrics().density );
		paintSimple.setStyle(Paint.Style.STROKE);
		paintSimple.setStrokeJoin(Paint.Join.ROUND);
		paintSimple.setStrokeCap(Paint.Cap.ROUND);

		paintBlur = new Paint();
		paintBlur.set(paintSimple);
		paintBlur.setColor(Color.argb(245, 74, 138, 255));
		paintBlur.setStrokeWidth(5f * getResources().getDisplayMetrics().density);
		paintBlur.setMaskFilter(new BlurMaskFilter(10, BlurMaskFilter.Blur.NORMAL));


		if (getInstance()!=null)
			getInstance().registerListener(this,Type.MAP);


	}
	private double pXR;
    private double pYR;
	private WF_Gis_Map myMap;
	private boolean allowZoom;
	private final IntBuffer intBuffer = new IntBuffer();
	private final PathBuffer pathBuffer = new PathBuffer();


	private class PathBuffer {
		private final Path[] mBuffer = new Path[500];
		private int c=0;

		PathBuffer() {
			for (int i = 0; i<mBuffer.length;i++)
				mBuffer[i]= new Path();
		}

		Path getPath() {
			if (c<mBuffer.length)
				return mBuffer[c++];
			Log.d("vortex","Ran out of path objects: "+(c++));
			return new Path();
		}

		void reset() {
			c=0;
		}
	}

	private class IntBuffer {

		private final int[][] mBuffer = new int[1500][2];
		private int c=0;

		int[] getIntBuf() {
			if (c<mBuffer.length)
				return mBuffer[c++];
			Log.d("vortex","Ran out of int arrays..");
			return new int[2];
		}

		void reset() {
			c=0;
		}
	}

	/**
	 *
	 * @param wf_Gis_Map 	The map object
	 * @param pm			The geo coordinates for the image corners
	 * @param allowZoom			If extreme zoom is enabled
	 * @return
	 */
	public void initialize(WF_Gis_Map wf_Gis_Map, PhotoMeta pm,boolean allowZoom) {

		if (pm== null) {
			Log.e("vortex","Photometadata was null for "+wf_Gis_Map.getName());
		}
		rectBuffer.clear();
		mapLocationForClick=null;
		this.photoMetaData=pm;
		pXR = this.getImageWidth()/pm.getWidth();
		pYR = this.getImageHeight()/pm.getHeight();

		//Filter away all objects not visible and create cached values for all gisobjects on this map and zoom level.
		myMap = wf_Gis_Map;



		imgHReal = pm.N-pm.S;
		imgWReal = pm.E-pm.W;
		Map<String, String> gps_key_map = getInstance().getVariableConfiguration().createGpsKeyMap();
		myX = getInstance().getVariableCache().getVariable(gps_key_map, NamedVariables.MY_GPS_LAT);
		myY = getInstance().getVariableCache().getVariable(gps_key_map, NamedVariables.MY_GPS_LONG);
		this.allowZoom = allowZoom;


		setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d("vortex","Gets short! Clickable is "+GisImageView.this.isClickable());
				if (clickXY!=null)
					return;
				if (!GisImageView.this.isClickable()) {
					Log.d("vortex","click outside popup?");
					if (myMap.wasShowingPopup()) {
						Log.d("vortex","YES!");
						GisImageView.this.setClickable(true);
					}
					return;
				}

				calculateMapLocationForClick(polyVertexX,polyVertexY);


				if (gisTypeToCreate!=null) {
					//GisObject newP = StaticGisPoint(gisTypeToCreate, Map<String, String> keyChain,Location myLocation, Variable statusVar)

					if (newGisObj ==null) {
						Set<GisObject> bag;
						for (GisLayer l:myMap.getLayers()) {

							bag = l.getBagOfType(gisTypeToCreate.getName());
							if (bag!= null) {
								Log.d("vortex","found correct bag!");
								newGisObj = createNewGisObject(gisTypeToCreate,bag);
								if (gisTypeToCreate.getGisPolyType()==GisObjectType.Point)
									myMap.setVisibleCreate(true,newGisObj.getLabel());
							}
						}
					}
					if (newGisObj !=null) {

						//Add new point.
						if (newGisObj instanceof GisPathObject)
							newGisObj.getCoordinates().add(mapLocationForClick);

					} else
						Log.e("vortex","New GisObj is null!");
				} else {
					Log.e("vortex", "gistype null!");
					//close candidates window if showing.
					myMap.showCandidates(null);
				}
				clickWasShort = true;
				invalidate();

			}
		});



		setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				Log.d("vortex","Gets long! clickable is "+GisImageView.this.isClickable());
				if (clickXY!=null || !GisImageView.this.isClickable())
					return false;
				calculateMapLocationForClick(polyVertexX,polyVertexY);
				clickWasShort = false;
				invalidate();
				return true;
			}


		});

		//Remove any gis objects outside current viewport.
		initializeAndSiftGisObjects();
		initialized = true;
	}

	public boolean isInitialized() {
		return initialized;
	}

	public void editSelectedGop() {
		//Cancel any ongoing creation
		if (touchedGop!=null) {
			newGisObj = touchedGop;
			currentCreateBag = touchedBag;
			cancelGisObjectCreation();
			//start new.
			startGisObjectCreation(touchedGop.getFullConfiguration());
			myMap.setVisibleCreate(true, newGisObj.getLabel());

		}

	}


	/**
	 *
	 * @return copy only the gis objects in the layers that are visible in this view.
	 */
	public void initializeAndSiftGisObjects() {
		if (myMap==null) {
			Log.e("vortex","myMap null in initializeAndSiftGisObjects");
			return;
		}
		for (GisLayer layer :myMap.getLayers()) {

			if (layer.getId().equals("Team")) {
				Set<GisObject>teamMembers = findMyTeam();
				if (teamMembers==null || teamMembers.isEmpty())
					Log.e("bortex","no team members found");
				else {
					Log.d("bortex", "found " + teamMembers.size()+" team members");
					layer.addObjectBag("Team", teamMembers, false, this);
				}

			}
			layer.filterLayer(this);
		}
	}

	public CurrStatVar getCurrentStatusVariable() {
		return currentStatusVariable;
	}

	private float fixedX=-1;
	private float fixedY;

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (fixedX==-1) {
			fixedY=y;
			fixedX=x;
			Log.d("vortex","fixed xy"+fixedX+","+fixedY);
		}
	}


	PhotoMeta gisImage;
	//difference in % between ruta and image size.

	private Location mapLocationForClick=null;
	private float[] clickXY;
	private double imgHReal;
	private double imgWReal;

	private GisObject newGisObj;

	private Set<GisObject> currentCreateBag;

	private float[] translateToReal(float mx,float my) {
		float fixScale = scale * scaleAdjust;
		Log.d("vortex","fixscale: "+fixScale);
		mx = (mx-x)/fixScale;
		my = (my-y)/fixScale;
		return new float[]{mx,my};
	}


	private Location translateRealCoordinatestoMap(float[] xy) {
		Log.d("betex","IMGW: "+this.getImageWidth());
		float rX = this.getImageWidth()/2+ xy[0];
		float rY = this.getImageHeight()/2+ xy[1];
		pXR = photoMetaData.getWidth()/this.getImageWidth();
		pYR = photoMetaData.getHeight()/this.getImageHeight();
		double mapDistX = rX*pXR;
		double mapDistY = rY*pYR;
		return new SweLocation(mapDistX + photoMetaData.W,photoMetaData.N-mapDistY);

	}

	/**
	 *
	 * @param l	- the location to examine
	 * @param xy - the translated x and y into screen coordinates.
	 * @return true if the coordinate is inside the current map.
	 */
	public boolean translateMapToRealCoordinates(Location l, int[] xy) {
		//Unknown location, surely outside.
		if (l == null) {
			xy=null;
			return false;
		}
		//Assume it is inside
		boolean isInside = false;
		double mapDistX = l.getX()-photoMetaData.W;
		double mapDistY = l.getY()-photoMetaData.S;
		if ((mapDistX <=imgWReal && mapDistX>=0) && (mapDistY <=imgHReal && mapDistY>=0)) {
			//Log.d("jgw", " distX: " + mapDistX + " distY: "+mapDistY+" [imgW: "+imgWReal+" imgH: "+imgHReal+"]");
			isInside = true;
		}
//		else {
//			if(mapDistX>imgWReal||mapDistX<0)
//				Log.e("jgw","Distance X in meter: "+mapDistX+" [outside!]");
//			if(mapDistY>imgHReal||mapDistY<0)
//				Log.e("jgw","Distance Y in meter: "+mapDistY+" [outside!]");
//			Log.d("jgw","w h of gis image. w h of image ("+photoMetaData.getWidth()+","+photoMetaData.getHeight()+") ("+this.getScaledWidth()+","+this.getScaledHeight()+")");
//			Log.d("jgw","photo (X) "+photoMetaData.W+"-"+photoMetaData.E);
//			Log.d("jgw","photo (Y) "+photoMetaData.S+"-"+photoMetaData.N);
//			Log.d("jgw","object X,Y: "+l.getX()+","+l.getY());
		//No, it is outside.
//			isInside = false;
//		}


		pXR = this.getImageWidth()/photoMetaData.getWidth();
		pYR = this.getImageHeight()/photoMetaData.getHeight();

		//		Log.d("vortex","px, py"+pXR+","+pYR);
		double pixDX = mapDistX*pXR;
		double pixDY = mapDistY*pYR;
		//Log.d("vortex","distance on map (in pixel no scale): x,y "+pixDX+","+pixDY);
		float rX = ((float)pixDX)-this.getImageWidth()/2;
		float rY = this.getImageHeight()/2-((float)pixDY);

		//		Log.d("vortex","X: Y:"+x+","+y);
		//		Log.d("vortex","fixScale: "+fixScale);
		//		Log.d("vortex","after calc(x), calc(y) "+rX+","+rY);
		//		Log.d("vortex","fX: fY:"+fixedX+","+fixedY);
		//		Log.d("vortex","after fcalc(x), fcalc(y) "+this.fCalcX(rX)+","+fCalcY(rY));
		xy[0]=(int)rX;
		xy[1]=(int)rY;

		return isInside;

	}

	private Location calculateMapLocationForClick(float x, float y) {
		//Figure out geo coords from pic coords.
		clickXY=translateToReal(x,y);
		mapLocationForClick = translateRealCoordinatestoMap(clickXY);
		Log.d("vortex","click at "+x+","+y);
		Log.d("vortex","click at "+mapLocationForClick.getX()+","+mapLocationForClick.getY());
		return mapLocationForClick;
	}

	private GisObject createNewGisObject(
			FullGisObjectConfiguration gisTypeToCreate, Set<GisObject> bag) {
		//create object or part of object.
		Map<String, String> keyHash = Tools.copyKeyHash(gisTypeToCreate.getObjectKeyHash().getContext());
		GisObject ret=null;

		//break if no keyhash.
		if (keyHash!=null) {
			String uid = UUID.randomUUID().toString();
			Log.d("vortex","HACK: Adding uid: "+uid);
			keyHash.put("uid", uid);
			Variable gistyp = getInstance().getVariableCache().getVariable(keyHash,NamedVariables.GIS_TYPE);
			if (gistyp!=null ) {
				gistyp.setValue(gisTypeToCreate.getObjectKeyHash().getContext().get("gistyp"));
				Log.d("vortex", "keyhash for new obj is: " + keyHash.toString() + " and gistyp: " + gistyp.getValue());
			}
			List<Location> myDots;

			switch (gisTypeToCreate.getGisPolyType()) {

				case Point:
					ret = new StaticGisPoint(gisTypeToCreate,keyHash,mapLocationForClick, gisTypeToCreate.getStatusVariable(),null);
					int[] xy = intBuffer.getIntBuf();
					translateMapToRealCoordinates(mapLocationForClick,xy);
					((StaticGisPoint)ret).setTranslatedLocation(xy);
					break;

				case Linestring:
					myDots = new ArrayList<Location>();
					ret = new GisMultiPointObject(gisTypeToCreate, keyHash,myDots,gisTypeToCreate.getStatusVariable(),null);
					break;
				case Polygon:
					ret = new GisPolygonObject(gisTypeToCreate, keyHash,
							"",GisConstants.SWEREF,gisTypeToCreate.getStatusVariable(),null);
					break;

			}
			//Log.d("vortex","Adding "+ret.toString()+" to bag "+bag.toString());
			ret.markAsUseful();
			bag.add(ret);

			//save layer and object for undo.
			currentCreateBag = bag;

		} else
			Log.e("vortex","Cannot create, object keyhash is null!!!");
		return ret;
	}

	//returns true if there is no more backing up possible.
	public void goBack() {
		if (newGisObj!=null) {
			if (newGisObj instanceof StaticGisPoint) {
				cancelGisObjectCreation();


			} else if (newGisObj instanceof GisPathObject) {
				List<Location> myDots = newGisObj.getCoordinates();
				if (myDots!=null && !myDots.isEmpty()) {
					myDots.remove(myDots.size() - 1);
					newGisObj.clearCache();
				}

				if (myDots==null ||myDots.isEmpty()) {

					//If this object exists in the db, it should be deleted if last dot is removed.
					String keyPairs = Tools.convertToKeyPairs(newGisObj.getKeyHash());
					int rowsAffected = getInstance().getDb().erase(keyPairs,null);
					if (rowsAffected>0) {
						Log.d("claxon","aff: "+rowsAffected);
						getInstance().getLogger().addRow(" erased " + rowsAffected + " entries for GIS object [" + newGisObj.getLabel() + "]");
						//Create sync entry for all variables with matching keys (no pattern)
						getInstance().getDb().insertEraseAuditEntry(keyPairs,null);
					}
					//cancel the creation
					cancelGisObjectCreation();
					Log.d("vortex","Go BACK: NewGisObj removed");
				} else
					//invalidate done inside cancelGisObjectCreation for other case.
					invalidate();
			}

		}
	}

	public void createOk() {
		//if this is a path, close it.
//		if (newGisObj instanceof GisPolygonObject) {
//			Path p = createPathFromCoordinates(newGisObj.getCoordinates(), true);
//			((GisPolygonObject) newGisObj).addPath(p);
//		}
		//	((GisPathObject)newGisObj).getPaths().get(0).close();
		if (currentCreateBag!=null) {
			Log.d("vortex","inserting new GIS object.");
			getInstance().getDb().insertGisObject(newGisObj);

			gisTypeToCreate=null;
			touchedBag = currentCreateBag;
			currentCreateBag=null;
			touchedGop = newGisObj;
			//throw away the current path and force redraw.
			if (touchedGop instanceof GisPathObject)
				touchedGop.clearCache();
			newGisObj=null;
			//Log.d("vortex","Here touched is TG TB"+touchedGop.toString()+","+touchedBag.toString());
			myMap.setVisibleAvstRikt(true,touchedGop);

			this.redraw();
		} else
			Log.e("vortex","cannot create...createbag null");
	}

	Drawable d1;

	private final List<GisObject> candidates = new ArrayList<GisObject>();

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		canvas.save();
		//scale and adjust.
		try {
			float adjustedScale = scale * scaleAdjust;
			if (allowZoom && adjustedScale>2.0f && this.getDrawable()!=null)
				myMap.setZoomButtonVisible(true);
			else
				myMap.setZoomButtonVisible(false);

			canvas.translate(x, y);
			if(adjustedScale != 1.0f) {
				canvas.scale(adjustedScale, adjustedScale);
			}
			//Log.d("ergo","clearing candidates list");
			candidates.clear();

			for (int layerCount= myMap.getLayers().size()-1; layerCount>=0;layerCount--) {
				GisLayer layerO = myMap.getLayers().get(layerCount);
				String layerId = layerO.getId();

				//Log.d("bortex","Drawing layer "+layerId);
				//get all objects that should be drawn on this layer.
				//Treat maplayers
				if (!layerO.isVisible()) {
					//Log.d("vortex","layer not visible...skipping "+layerId+" Obj: "+layerO.toString());
					continue;
				}

				//only allow clicks if something is not already touched.
				pXR = this.getImageWidth()/photoMetaData.getWidth();
				pYR = this.getImageHeight()/photoMetaData.getHeight();
				Map<String, Set<GisObject>> bags = layerO.getGisBags();
				Map<String, Set<GisFilter>> filterMap = layerO.getFilters();

				if (bags!=null && !bags.isEmpty()) {

					for (String key:bags.keySet()) {
						Set<GisFilter> filters = filterMap!=null?filterMap.get(key):null;
						Set<GisObject> bagOfObjects = bags.get(key);
						Iterator<GisObject> iterator = bagOfObjects.iterator();
						//Log.d("vortex","bag "+key+" has "+bagOfObjects.size()+" members");
						while (iterator.hasNext()) {
							GisObject go = iterator.next();
							//Log.d("bortex","Checking "+go.getLabel()+" id: "+go.getId()+ "type: "+go.getClass().getCanonicalName());
							//If not inside map, or if touched, skip.
							if (!go.isUseful() || (go.equals(touchedGop)) || isExcludedByStandardFilter(go.getStatusVariableValue())) {
								//Log.d("bortex",go.getLabel()+" is thown. useful?" + go.isUseful());
								continue;
							}
							if (go instanceof GisPointObject) {
								GisPointObject gop = (GisPointObject)go;
								//Log.d("baha",gop.getId());
								if (gop.isDynamic()) {
									//Log.d("Glapp","found dynamic object");
									int[] xy = intBuffer.getIntBuf();
                                    boolean inside = translateMapToRealCoordinates(gop.getLocation(),xy);
									if (!inside) {
										//Log.d("Glapp","outside "+gop.getLocation().getX()+" "+gop.getLocation().getY());
										if (gop.equals(userGop)) {
											myMap.showCenterButton(false);
											userGop=null;
										}
										//This object should not be drawn.
										continue;
									} else {

										//Log.d("inside","inside. Label :"+gop.getLabel());
										if (gop.isUser()) {
											userGop = gop;
											myMap.showCenterButton(true);
										}
										gop.setTranslatedLocation(xy);
									}


								}
								Bitmap bitmap = gop.getIcon();
								float radius = gop.getRadius();
								String color = gop.getColor();
								Style style = gop.getStyle();
								PolyType polyType=gop.getShape();

								//Log.d("bortex", "LBL: "+gop.getLabel()+" STAT: "+statusValue+" POLLY "+polyType.name());

								String statusColor = colorShiftOnStatus(gop.getStatusVariableValue());
								if (statusColor!=null)
									color = statusColor;

								if (filters!=null&&!filters.isEmpty()) {

									//Log.d("vortex","has filter!");

									for (GisFilter filter:filters) {
										if (filter.isActive()) {
											//Log.d("vortex","Filter active!");

											if (!gop.hasCachedFilterResult(filter)) {
												Boolean result = Expressor.analyzeBooleanExpression(filter.getExpression(),gop.getKeyHash(),null);
												gop.setCachedFilterResult(filter,result);
											}
											if ( gop.getCachedFilterResult(filter)) {
												Log.d("vortex","FILTER MATCH FOR FILTER: "+filter.getLabel());
												bitmap = filter.getBitmap();
												radius = filter.getRadius();
												//color = filter.getColor();
												style = filter.getStyle();
												polyType = filter.getShape();
											}
										} else
											Log.d("vortex","Filter turned off!");
									}
								}
								int[] xy = gop.getTranslatedLocation();
								if (xy == null && gop.getCoordinates()!=null && !gop.getCoordinates().isEmpty()) {
									xy = intBuffer.getIntBuf();
									translateMapToRealCoordinates(gop.getCoordinates().get(0),xy);
									gop.setTranslatedLocation(xy);
									xy = gop.getTranslatedLocation();
								}
								if (xy!=null) {
									//Log.d("vortex","drawing "+gop.getLabel());
									drawPoint(canvas,bitmap,radius,color,style,polyType,xy,adjustedScale,gop.getFullConfiguration().useIconOnMap());
									if (layerO!=null && layerO.showLabels()) {
										drawGopLabel(canvas,xy,go.getLabel(),LabelOffset,bCursorPaint,txtPaint);
									}
								}
								/*


								 */

							} else if (go instanceof GisPathObject) {
								if (go instanceof GisMultiPointObject) {
									if (((GisMultiPointObject) go).isLineString()) {
										Log.d("vortex","This is a multipoint. Path is useless.");
									}
								}

								drawGop(canvas,layerO,go,false);
							}

							//Check if an object has been clicked in this layer.
							if (!candMenuVisible && touchedGop==null && gisTypeToCreate == null && mapLocationForClick!=null && go.isTouchedByClick(mapLocationForClick,pXR,pYR) && !go.equals(userGop)) {


								boolean s = candidates.add(go);
								if (!s) {

									Log.d("pex", go.getLabel()+" exists: "+candidates.contains(go)+" loc "+go.getCoordinates()+" gid: "+go.getKeyHash());
									for (GisObject g:candidates)
										Log.d("pex",g.getLabel()+" loc "+g.getCoordinates()+" gid: "+g.getKeyHash());

								}
								else
									Log.d("pex","cand added: "+go.getLabel());

							}
						}
					}
				}
			}

			//Special rendering of touched gop.
			if (candidates.size()>1) {
				//Candidates are sorted in a set. Return first.
				String candidatesS="";

				if (!candMenuVisible) {
					Collections.sort(candidates,
					new Comparator<GisObject>() {
						@Override
						public int compare(GisObject lhs, GisObject rhs) {
							return (int)(lhs.getDistanceToClick()-rhs.getDistanceToClick());
						}
					});
					List<GisObject> candies = new ArrayList<GisObject>();
					//Log.d("vortex","candidate: "+go.getLabel()+" id: "+go.getId());
					candies.addAll(candidates);
					candMenuVisible = true;
					touchedBag=null;
					myMap.showCandidates(candies);
				}


				//If only one candidate, select it.
			} else if (!candidates.isEmpty()) {
				touchedGop=candidates.iterator().next();
				riktLinjeStart=null;
				touchedBag=null;
			}

			if (touchedGop!=null) {
				//Log.d("vortex", "Gop selected now has Keychain: " + touchedGop.getKeyHash());
				//Find the layer and bag touched.
				if (touchedBag==null) {
					for (GisLayer layer : myMap.getLayers()) {
						touchedBag = layer.getBagContainingGo(touchedGop);
						if (touchedBag != null) {
							Log.d("vortex", "setting touchedlayer");
							touchedLayer = layer;
							//if longclick, open the actionbar menu.
							if (!clickWasShort)
								myMap.startActionModeCb();
							else {
								myMap.setVisibleAvstRikt(true, touchedGop);
								displayDistanceAndDirection();
							}

							break;
						}
					}

				}
				if (touchedBag != null) {



					if (clickWasShort && (riktLinjeStart == null || riktLinjeEnd == null) && mostRecentGPSValueTimeStamp!=-1 && myX!=null&&myY!=null&&myX.getValue()!=null && myY.getValue()!=null) {
						//Create a line from user to object.
						double mX = Double.parseDouble(myX.getValue());
						double mY = Double.parseDouble(myY.getValue());
						riktLinjeStart = intBuffer.getIntBuf();riktLinjeEnd = intBuffer.getIntBuf();
						translateMapToRealCoordinates(new SweLocation(mX,mY),riktLinjeStart);
						translateMapToRealCoordinates(touchedGop.getLocation(),riktLinjeEnd);
					}
					//if (touchedLayer == null)
					//	Log.d("vortex","TOUCHEDLAYER WAS NULL. TouchedBag was: "+touchedBag);

					drawGop(canvas,touchedLayer,touchedGop,true);
					//Check if directional line should be drawn.
					if (riktLinjeStart!=null) {
						//Log.d("vortex","drawing a million times?");
						canvas.drawLine(riktLinjeStart[0], riktLinjeStart[1], riktLinjeEnd[0], riktLinjeEnd[1], fgPaintSel);//fgPaintSel
					}
				} else {
					Log.e("vortex", "The touched object does not belong to a bag.");
					touchedGop = null;
				}
			}
		} catch(Exception e) {
			if (getInstance()!=null) {
				LoggerI o = getInstance().getLogger();
				if (o!=null) {
					o.addRow("");

					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					o.addRedText(sw.toString());
					e.printStackTrace();
				}
			}
		}

		if (newGisObj!=null) {
			double lengthOfPath = 0;
			myMap.setVisibleCreate(true,newGisObj.getLabel());
			//Show ok button only after at least 2 points defined.
			boolean showOk = true;
			if (newGisObj instanceof GisPathObject) {
				List<Location> myDots = newGisObj.getCoordinates();
				showOk = (myDots!=null && myDots.size()>1);
				lengthOfPath = Geomatte.lengthOfPath(myDots);

			}
			myMap.setVisibleCreateOk(showOk);
			myMap.showLength(lengthOfPath);

		} else
			myMap.setVisibleCreate(false,"");


		canvas.restore();
		//Reset any click done.
		mapLocationForClick=null;
		clickXY=null;
		//Reuse same int arrays and paths next call.
		intBuffer.reset();
		pathBuffer.reset();
	}

	private final static String isFreshColor = "#7CFC00";
	private final static String isOverAnHourColor = "#D3D3D3";
	private final static String isOverHalfAnHourColor = "#050FF5";
	private final static String isOverAQuarterColor = "#FFFA0A";



	public Set<GisObject> findMyTeam() {
		//Log.d("bortex","In findmyteam");

		Set<GisObject> ret = null;

		final String team = getInstance().getGlobalPreferences().get(PersistenceHelper.LAG_ID_KEY);
		final String user = getInstance().getGlobalPreferences().get(PersistenceHelper.USER_ID_KEY);
		if(team ==null || team.length()==0) {
			Log.d("vortex","no team but team is set to show. alarm!");
			return null;
		}


		int cc=0;
		Map<String,DbHelper.LocationAndTimeStamp> myTeam = getInstance().getDb().getTeamMembers(team,user);
		if (myTeam==null)
			return null;

		for (String name:myTeam.keySet()) {

			Log.d("bortex","Adding Team member "+name);
			if (name==null || name.isEmpty()) {
				Log.e("vortex","skipping nameless team member");
				continue;
			}
			final DbHelper.LocationAndTimeStamp l = myTeam.get(name);

			//create a key for the workflow.
			final Map<String,String> tmp = new HashMap<>();
			tmp.put(DbHelper.YEAR,Constants.getYear());
			tmp.put("team",team);
			tmp.put("inventerare",name);
			tmp.put("timestamp",Tools.getTimeStampDetails(l.getMostRecentTimeStamp(),false)+"");
			tmp.put("location",l.location+"");


			//Log.d("bortex","Location "+l);

			GisPointObject member = new StaticGisPoint(new FullGisObjectConfiguration() {
				@Override
				public float getRadius() {
					return 4;
				}

				@Override
				public String getColor() {
					return l.isOverAnHourOld()? isOverAnHourColor :
							l.isOverHalfAnHourOld()? isOverHalfAnHourColor :
									l.isOverAQuarterOld()? isOverAQuarterColor :
											isFreshColor;
				}

				@Override
				public GisObjectType getGisPolyType() {
					return GisObjectType.Point;
				}

				@Override
				public Bitmap getIcon() {
					return null;//BitmapFactory.decodeResource(getResources(), R.drawable.boy);
				}

				@Override
				public Style getStyle() {
					return Style.FILL_AND_STROKE;
				}

				@Override
				public PolyType getShape() {
					return PolyType.circle;
				}

				@Override
				public String getClickFlow() {
					return "wf_teammember";
				}

				@Override
				public DB_Context getObjectKeyHash() {
					return new DB_Context("år=[getCurrentYear()], lag = [getTeamName()], author ",tmp );
				}

				@Override
				public String getStatusVariable() {
					return null;
				}

				@Override
				public boolean isUser() {
					return false;
				}

				@Override
				public String getName() {
					return name;
				}

				@Override
				public String getRawLabel() {
					return name;
				}

				@Override
				public String getCreator() {
					return "";
				}

				@Override
				public boolean useIconOnMap() {
					return true;
				}

				@Override
				public boolean isVisible() {
					return true;
				}

				@Override
				public List<Expressor.EvalExpr> getLabelExpression() {
					return Expressor.preCompileExpression(name);
				}
			}, tmp, l.location, null, null);
			member.setLabel(name+"["+Tools.getTimeStampDetails(l.getMostRecentTimeStamp(),true)+"]");
			GisLayer.markIfUseful(member,this);

			if (ret ==null)
				ret = new HashSet<>();
			ret.add(member);
		}
		return ret;
	}

	private boolean isExcludedByStandardFilter(String status) {
		if (status==null)
			return false;
		//Log.d("vortex","status is: "+status+" excluded? "+!myMap.isNotExcluded(status));

		return !myMap.isNotExcluded(status);
	}

	public void selectGop(GisObject go) {
		touchedGop=go;
		candMenuVisible=false;
	}
	/*
	private void startShowLabelTimer() {
		final int interval = 1000; // 1 Second
		Handler handler = new Handler();
		Runnable runnable = new Runnable(){
			public void run() {
				showLabelForAWhile=false;
				postInvalidate();
			}
		};

		handler.postDelayed(runnable, interval);
	}
	 */
	private static String colorShiftOnStatus(String statusValue) {
		String color=null;
		if (statusValue!=null ) {

			if (statusValue.equals("1")) {
				color = "yellow";
			}
			else if (statusValue.equals("2")) {
				color = "red";
			}
			else if	(statusValue.equals("3")) {
				color = "green";
			}
		}
		return color;
	}

	private void drawGop(Canvas canvas, GisLayer layerO, GisObject go, boolean selected) {

		boolean beingDrawn = false;
		int[] xy;

		if (newGisObj != null) {
			beingDrawn = go.equals(newGisObj);
			//Log.d("blommor","beingdrawn "+beingDrawn+" for "+go.getLabel());
		}
		//will only be called from here if selected.
		GisPointObject gop = null;
		//Only gets her for Gispoint, if it is selected.
		if (go instanceof GisPointObject) {
			gop = (GisPointObject) go;
			if (gop.getTranslatedLocation() != null) {
				//Log.d("vortex","Calling drawpoint in dispatchdraw for "+go.getLabel());
				drawPoint(canvas, null, gop.getRadius(), "red", Style.FILL, gop.getShape(), gop.getTranslatedLocation(), 1,false);
			} else
				Log.e("vortex", "NOT calling drawpoint since translatedlocation was null");

		} else if (go instanceof GisPathObject) {
			boolean singlePath = false,isPolygon = (go instanceof GisPolygonObject);

			GisPathObject gpo = (GisPathObject) go;
			//Add glow effect if it is currently selected.

			if (beingDrawn) {
				Path p = createPathFromCoordinates(gpo.getCoordinates(),false);
				if (p != null)
					canvas.drawPath(p, polyPaint);
				xy = intBuffer.getIntBuf();
				translateMapToRealCoordinates(gpo.getCoordinates().get(gpo.getCoordinates().size()-1),xy);
				drawPoint(canvas, null,2, "white", Style.STROKE, PolyType.circle, xy,1,false);
			} else {
				if (go instanceof GisPolygonObject) {
					//check if buffered paths already exists.
					if (gpo.getPaths() == null) {
						//no...create.

						for (List<Location> ll : ((GisPolygonObject) gpo).getPolygons().values()) {
							Path p = createPathFromCoordinates(ll,true);
							if (p != null) {
								gpo.addPath(p);
							}
						}
						if (gpo.getPaths() != null && gpo.getPaths().size() == 1)
							singlePath = true;
					}
				} else
					singlePath = true;

				if (singlePath) {

					Path p = null;
					if (gpo.getPaths() != null) {
						p = gpo.getPaths().get(0);
					} else {
						p = createPathFromCoordinates(gpo.getCoordinates(),isPolygon);
						gpo.addPath(p);
					}
					if (p!=null)
						drawPath(p,selected,canvas,go);
				} else {
					List<Path> paths = gpo.getPaths();
					if (paths != null) {
						for (Path p : paths) {
							drawPath(p,selected,canvas,go);
						}
					}
				}
			}
		}
		//Check if label should be drawn.

		if (!selected&&layerO!=null && layerO.showLabels() && !beingDrawn) {
			xy = intBuffer.getIntBuf();
			translateMapToRealCoordinates(go.getLocation(),xy);
			//Log.d("vortex","Calling drawlabel in drawgop for "+go.getLabel()+". I am a path? "+(go instanceof GisPathObject)+" Iam point? "+(go instanceof GisPointObject));
			drawGopLabel(canvas,xy,go.getLabel(),LabelOffset,bCursorPaint,vtnTxt);
		}


	}

	private void drawPath(Path p, boolean selected, Canvas canvas, GisObject go) {
		if (selected) {
			//Log.d("bel","get status returns "+go.getStatusVariableValue());
			canvas.drawPath(p, paintBlur);
			canvas.drawPath(p, paintSimple);
		} else {
			String color = colorShiftOnStatus(go.getStatusVariableValue());
			if (color == null)
				color = go.getColor();
			// strokeWidth: 0 changed to display-aware
			canvas.drawPath(p,
					createPaint(
							color,
							Paint.Style.STROKE,
							(int) (2.0f * getResources().getDisplayMetrics().density) ));
		}
	}


	private Path createPathFromCoordinates(List<Location> ll, boolean isClosed) {
		int[] xy=intBuffer.getIntBuf();
		if (ll ==null)
			return null;
		boolean first = true;
		Path p = new Path();

		for (int i=0;i<ll.size();i++) {
			Location l=ll.get(i);
			translateMapToRealCoordinates(l,xy);
			if (xy==null)
				continue;
			if (first) {
				p.moveTo(xy[0],xy[1]);
				first =false;
			} else
				p.lineTo(xy[0],xy[1]);
		}
		if (isClosed)
			p.close();

		return p;
	}





	/**
	 * Draws a Label above the object location at the distance given by offSet
	 * @param canvas
	 * @param xy
	 * @param mLabel
	 * @param offSet
	 * @param bgPaint
	 * @param txtPaint
	 */
	private final Map<int[],Rect> rectBuffer = new HashMap<int[],Rect>();

	private void drawGopLabel(Canvas canvas, int[] xy, String mLabel, float offSet, Paint bgPaint, Paint txtPaint) {
		Rect bounds = rectBuffer.get(xy);
		if (bounds== null) {
			bounds = new Rect();
			rectBuffer.put(xy,bounds);
		}
		mLabel=mLabel==null?"":mLabel; //prevent null exception if label is null.
		txtPaint.getTextBounds(mLabel, 0, mLabel.length(), bounds);
		int textH = bounds.height()/2;
		bounds.offset(xy[0] -bounds.width()/2, xy[1] -(bounds.height()/2+(int)offSet));
		bounds.set(bounds.left-2,bounds.top-2,bounds.right+2,bounds.bottom+2);
		//txtPaint.setTextAlign(Paint.Align.CENTER);
		canvas.drawRect(bounds, bgPaint);
		canvas.drawText(mLabel, bounds.centerX(), bounds.centerY()+textH,txtPaint);

	}

	private void drawPoint(Canvas canvas, Bitmap bitmap, float radius, String color, Style style, PolyType type, int[] xy, float adjustedScale, boolean useIconOnMap) {

		Rect r;
		//Log.d("bortex","in drawpoint type "+type.name()+" bitmap: "+bitmap);
		//Log.d("arriba","useI"+useIconOnMap+" bm: "+(bitmap!=null));
		if (useIconOnMap && bitmap!=null ) {

			r = new Rect();
			//Log.d("vortex","bitmap! "+gop.getLabel());
			r.set(xy[0]-16, xy[1]-16, xy[0]+16, xy[1]+16);
			canvas.drawBitmap(bitmap, null, r, null);
		} //circular?

		else if (type == PolyType.circle) {
			//Log.d("bortex","x,y,r"+xy[0]+","+xy[1]+","+radius);
			canvas.drawCircle(xy[0], xy[1],radius, createPaint(color,style));
		}
		//no...square.
		else if (type == PolyType.rect) {
			//Log.d("vortex","rect!");
			int diam = (int)(radius/2);
			canvas.drawRect(xy[0]-diam, xy[1]-diam, xy[0]+diam, xy[1]+diam, createPaint(color,style));
		}
		else if (type == PolyType.triangle) {
			drawTriangle(canvas,color,style,radius,xy[0], xy[1]);
		}
	}

	//0 = distance, 1=riktning.


	public void drawTriangle(Canvas canvas, String color, Style style,
							 float radius, int x, int y) {
		Paint paint = this.createPaint(color, style);
		Path path = pathBuffer.getPath();
		path.reset();
		path.setFillType(FillType.EVEN_ODD);

		path.moveTo(x,y-radius);
		path.lineTo(x+radius, y+radius);
		path.lineTo(x-radius, y+radius);
		path.close();

		canvas.drawPath(path, paint);
	}

	public void unSelectGop() {
		touchedLayer=null;
		touchedBag=null;
		touchedGop=null;
		myMap.setVisibleAvstRikt(false,null);
		//Remove directional line.
		riktLinjeStart=null;
		riktLinjeEnd=null;
		invalidate();
	}



	//save the position where the user pressed start.
	private int[] riktLinjeStart,riktLinjeEnd;

    private final static int TimeOut = 3;

	private void displayDistanceAndDirection() {
		final int interval = TimeOut*1000;

		if (handler==null) {
			handler = new Handler();
			Runnable runnable = new Runnable(){
				public void run() {
					displayDistanceAndDirectionL(null);
					//Log.d("vortex","displaydistcalled from disp timer");
					if (handler!=null)
						handler.postDelayed(this, interval);
				}
			};

			handler.postDelayed(runnable, 3000);

		}

	}

	private void displayDistanceAndDirectionL(GPS_State s) {

		if (touchedGop==null) {
			handler=null;
			//Log.e("vortex","Touched GOP null in dispDistAndDir. Will exit");
			return;
		}

		//Check preconditions for GPS to work
		//Log.d("wolf","In display distance an direction");
		if (myX==null||myY==null|| getInstance()==null) {
			myMap.setAvstTxt("Config");
			myMap.setRiktTxt("fault!");
			handler=null;
			return;
		}


	//	if (!getInstance().getTracker().isGPSEnabled) {
	//		myMap.setAvstTxt("GPS OFF");
	//		myMap.setRiktTxt("");
	//		return;
			//myMap.setRiktTxt(spinAnim());
	//	}
		//Start a redrawtimer if not already started that redraws this window independent of the redraw cycle of the gops.

		//Check  timediff. Returns null in case no value exists.

		long timeDiff;
		long ct;
		if (mostRecentGPSValueTimeStamp!=-1) {
			ct = System.currentTimeMillis();
			timeDiff = (ct-mostRecentGPSValueTimeStamp)/1000;

		} else {

			myMap.setAvstTxt("GPS");
			myMap.setRiktTxt("searching");
			return;
		}

		boolean old = timeDiff>TimeOut;
		if (old) {
			//Log.d("vortex","Time of insert: "+mostRecentGPSValueTimeStamp);
			//Log.d("vortex","Current time: "+ct);
			//Log.d("vortex","TimeDiff: "+timeDiff);
			myMap.setAvstTxt("Lost signal");
			myMap.setRiktTxt(timeDiff+" s");

		}

		String mXs = myX.getValue();
		String mYs = myY.getValue();
		double mX = Double.parseDouble(mXs);
		double mY = Double.parseDouble(mYs);
		//COMPARE DIFF - REPLACE WITH SIGNAL VALUE IF PRESENT AND DIFFERENT
		if(s !=null && s.x != -1) {
			if (mX != s.x || mY != s.y) {
				mX = s.x;
				mY = s.y;
				myX.setOnlyCached(Double.toString(mX));
				myY.setOnlyCached(Double.toString(mY));
			}
		}

		double gX = touchedGop.getLocation().getX();
		double gY = touchedGop.getLocation().getY();
		Integer currentDistance = (int) Geomatte.sweDist(mY, mX, gY, gX);
		int rikt = (int)(Geomatte.getRikt2(mY, mX, gY, gX)*57.2957795);
		myMap.setAvstTxt(currentDistance >9999?(currentDistance /1000+"km"):(currentDistance +"m"));
		myMap.setRiktTxt(rikt+Deg);



	}




	private final Map<String,Paint> paintCache = new HashMap<String,Paint>();

	public Paint createPaint(String color, Paint.Style style) {
		return createPaint(color,style, (int) (2.0f * getResources().getDisplayMetrics().density) );
	}

	private Paint createPaint(String color, Paint.Style style, int strokeWidth) {
		String key = style==null?color:color+style.name();
		Paint p = paintCache.get(key);
		if (p!=null) {
			//Log.d("vortex","returns cached paint for "+key);
			return p;
		}
		//If no cached object, create.
		p = new Paint();
		p.setColor(color!=null?Color.parseColor(color):Color.YELLOW);
		p.setStyle(style!=null?style:Paint.Style.FILL);
		p.setStrokeWidth(strokeWidth);
		paintCache.put(key, p);
		return p;
	}








	private boolean isStarted=false;


	private FullGisObjectConfiguration gisTypeToCreate;



	public void runSelectedWf() {
		//update image to close polygon.
		invalidate();
		if (touchedGop!=null)
			runSelectedWf(touchedGop);
		unSelectGop();

	}
	private void runSelectedWf(GisObject gop) {

		getInstance().setDBContext(new DB_Context(null,gop.getKeyHash()));
		Log.d("vortex","Setting current keyhash to "+gop.getKeyHash());
		String target = gop.getWorkflow();
		Workflow wf = getInstance().getWorkflow(target);
		if (wf ==null) {
			Log.e("vortex","missing click target workflow");
			new AlertDialog.Builder(ctx)
					.setTitle("Missing workflow")
					.setMessage("No workflow associated with the GIS object or workflow not found: ["+target+"]. Check your XML.")
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setCancelable(false)
					.setNeutralButton("Ok",new Dialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {

						}
					} )
					.show();
		} else {
			if (gop.getStatusVariableId()!=null) {
				Map<String, String> keyHash = gop.getKeyHash();
				if (keyHash!=null)
					keyHash.put(VariableConfiguration.KEY_YEAR,Constants.getYear());
				Log.d("grogg","wfclick keyhash is "+keyHash+" for "+gop.getLabel());
				Variable statusVariable = getInstance().getVariableCache().getVariable(keyHash,gop.getStatusVariableId());
				if (statusVariable!=null) {
					String valS = statusVariable.getValue();
					if (valS == null || valS.equals("0")) {
						statusVariable.setValue("1");
						gop.setStatusVariableValue("1");
						myMap.registerEvent(new WF_Event_OnSave("Gis"));
					}
					//keep track
					currentStatusVariable = new CurrStatVar();
					currentStatusVariable.v = statusVariable;
					currentStatusVariable.id = gop.getKeyHash().get("uid");

				} else {
					Log.e("grogg", "StatusVariable definition error");
					LoggerI o = getInstance().getLogger();
					o.addRow("");
					o.addRedText("StatusVariable definition missing for: "+gop.getStatusVariableId());
				}

			} else
				Log.e("grogg",gop.getStatusVariableId()+" is null");

			Start.singleton.changePage(wf,gop.getStatusVariableId());

		}
	}


	public void centerOnUser() {
		centerOn(userGop);
	}/* else {
			new AlertDialog.Builder(ctx)
			.setTitle("Context problem")
			.setMessage("You are either outside map or have no valid GPS location.")
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setCancelable(true)
			.setNeutralButton("Ok",new Dialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {

				}
			} )
			.show();
		}*/

	public void centerOnCurrentDot() {
		centerOn(newGisObj);
	}


	private void centerOn(GisObject gisObject) {

		if (gisObject!=null) {
			int[] xy = intBuffer.getIntBuf();
			boolean inside = translateMapToRealCoordinates(gisObject.getLocation(),xy);
			if (inside) {
//					Log.d("vortex","X Y USER "+xy[0]+","+xy[1]);
				//Log.d("vortex","RX RY USER "+rxy[0]+","+rxy[1]);
//					Log.d("vortex","X Y SCALE: SCALEADJ:"+x+","+y+","+scale+","+scaleAdjust);
				setPosition(fixedX-xy[0]*scaleAdjust,fixedY-xy[1]*scaleAdjust);
				this.invalidate();
			}
		}
	}


	public void hideImage() {
		//use exisitng size if any so that clicks on empty canvas get correctly calculated coordinates.
		this.setStaticMeasure(this.getImageWidth(),this.getImageHeight());
		setImageDrawable(null);
	}

	public GisObject getGopBeingCreated() {
		return newGisObj;
	}



	enum CreateState {
		initial,
		inBetween,
		readyToGo
	}

	public void cancelGisObjectCreation() {
		if (gisTypeToCreate!=null) {
			gisTypeToCreate=null;
			if (newGisObj!=null) {
				Variable gistyp = getInstance().getVariableCache().getVariable(newGisObj.getKeyHash(),NamedVariables.GIS_TYPE);
				if (gistyp!=null) {
					gistyp.deleteValue();
					Log.d("bertox", "Removed gistype value for new gisobject");
				}
				currentCreateBag.remove(newGisObj);
				currentCreateBag =null;
				newGisObj=null;
			}

		}
		invalidate();
	}


	public void startGisObjectCreation(FullGisObjectConfiguration fop) {
		//unselect if selected
		Toast.makeText(ctx,"Click on map to register a coordinate",Toast.LENGTH_LONG).show();
		this.unSelectGop();
		gisTypeToCreate=fop;
	}

	public void deleteSelectedGop() {
		if (touchedGop!=null) {
			//GlobalState.getInstance().getDb().deleteAllVariablesUsingKey(touchedGop.getKeyHash());

			String keyPairs = Tools.convertToKeyPairs(touchedGop.getKeyHash());
			int rowsAffected = getInstance().getDb().erase(keyPairs,null);
			if (rowsAffected>0) {
				Log.d("claxon","aff: "+rowsAffected);
				getInstance().getLogger().addRow(" erased " + rowsAffected + " entries for GIS object [" + touchedGop.getLabel() + "]");
				//Create sync entry for all variables with matching keys (no pattern)
				getInstance().getDb().insertEraseAuditEntry(keyPairs,null);
			}
			//GlobalState.getInstance().getVariableCache().deleteAll(touchedGop.getKeyHash());
			touchedBag.remove(touchedGop);
			//Dont need to keep track of the bag anymore.
			touchedGop=null;
			invalidate();
		} else
			Log.e("vortex","Touchedgop null in deleteSelectedGop");
	}

	public GisObject getSelectedGop() {
		return touchedGop;
	}

	public void describeSelectedGop() {
		if (touchedGop != null && touchedGop.getKeyHash() != null) {
			String hash = touchedGop.getKeyHash().toString();
			new AlertDialog.Builder(ctx)
					.setTitle("GIS OBJECT DESCRIPTION")
					.setMessage("Type: " + touchedGop.getId() + "\nLabel: " + touchedGop.getLabel() +
							"\nSweref: " + touchedGop.getLocation().getX() + "," + touchedGop.getLocation().getY() +
							"\nAttached workflow: " + touchedGop.getWorkflow() +
							"\nKeyHash: " + hash +
							"\nPolygon type: " + touchedGop.getGisPolyType().name() +
							"\nCreator: " + touchedGop.getFullConfiguration().getCreator()
					)
					.setIcon(android.R.drawable.ic_menu_info_details)
					.setCancelable(true)
					.setNeutralButton("Ok", new Dialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {

						}
					})
					.show();
		} else
			Log.e("vortex","Touchedgop null in describeSelectedGop");
	}

	private long mostRecentGPSValueTimeStamp=-1;
	@Override
	public void gpsStateChanged(GPS_State gps_state) {
		//Log.d("vortex","Got GPS STATECHANGE");
		if (gps_state.state==GPS_State.State.newValueReceived||gps_state.state==GPS_State.State.ping) {
			mostRecentGPSValueTimeStamp = System.currentTimeMillis();
			displayDistanceAndDirectionL(gps_state);
		}
		this.postInvalidate();

	}



	public List<Location> getRectGeoCoordinates() {

		//Left top right bottom!
		List<Location> ret = new ArrayList<Location> ();

		Location topCorner = calculateMapLocationForClick(0,0);
		Location bottomCorner = calculateMapLocationForClick(this.displayWidth,this.displayHeight);

		ret.add(topCorner);
		ret.add(bottomCorner);

		return ret;

	}

	public Rect getCurrentViewSize(float fileImageWidth,float fileImageHeight) {
		//float Scales = scale * scaleAdjust;


		//int top = (int)(rX-this.getImageWidth()/2);

		final float Xs = (this.getScaledWidth()/2)-x;
		final float Ys = (this.getScaledHeight()/2)-y;
		final float Xe = Xs+this.displayWidth;
		final float Ye = Ys+this.displayHeight;


		final float scaleFx = fileImageWidth/this.getScaledWidth();
		final float scaleFy = fileImageHeight/this.getScaledHeight();


		final int left  = (int)(Xs * scaleFx);
		final int top  = (int)(Ys * scaleFy);

		final int right = (int)(Xe * scaleFx);
		final int bottom =(int)(Ye * scaleFy);

		Rect r = new Rect(left,top,right,bottom);

		Log.d("vortex","top bottom left right "+top+","+bottom+","+left+","+right);
		return r;
	}

}
