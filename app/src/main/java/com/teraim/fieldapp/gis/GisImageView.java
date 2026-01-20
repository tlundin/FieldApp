package com.teraim.fieldapp.gis;

import static com.teraim.fieldapp.GlobalState.getInstance;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.FillType;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.GisLayer;
import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.PhotoMeta;
import com.teraim.fieldapp.dynamic.types.SweLocation;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.Workflow;
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
import com.teraim.fieldapp.log.LogRepository;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.ui.MapNeedlePreference;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Geomatte;
import com.teraim.fieldapp.utils.Tools;
import com.teraim.fieldapp.viewmodels.TeamStatusViewModel;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;


public class GisImageView extends GestureImageView implements TrackerListener {
	private static final String TAG = "GisImageView";


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

	// Use selectedUserNeedle instead of personActive
	private Bitmap selectedUserNeedle; // This will hold the user's selected icon
	private Bitmap personAway; // Keep personAway as is for now

	private static final int LabelOffset = 5;

	//Long click or short click?
	private boolean clickWasShort=false;

	//The user Gis Point Object
	private GisPointObject userGop;

	//The bag and layer that contains the object currently clicked.
	private Set<GisObject> touchedBag;
	private GisLayer touchedLayer;
	private GisObject touchedGop = null;


	private boolean candMenuVisible=false, initialized = false;
	private Set<GisObject> teamMembers = null;
	private TeamStatusViewModel teamStatusViewModel=null;

	// Flag to track if team data has been initialized (i.e., its layer bag has been added)
	private boolean teamLayerBagInitialized = false;
	private Observer<Set<GisPointObject>> teamMembersObserver;

	public GisImageView(Context context) {
		super(context);
		init(context);
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
		calendar.setTime(new Date());
		Paint grCursorPaint = new Paint();
		grCursorPaint.setColor(Color.GRAY);
		grCursorPaint.setStyle(Style.FILL);
		Paint blCursorPaint = new Paint();
		blCursorPaint.setColor(Color.BLUE);
		blCursorPaint.setStyle(Style.FILL);
		Paint rCursorPaint = new Paint();
		rCursorPaint.setColor(Color.RED);
		rCursorPaint.setStyle(Style.FILL);
		Paint wCursorPaint = new Paint();
		wCursorPaint.setColor(Color.WHITE);
		wCursorPaint.setStyle(Style.FILL);
		bCursorPaint = new Paint();
		bCursorPaint.setColor(Color.BLACK);
		bCursorPaint.setStyle(Style.FILL);
		Paint markerPaint = new Paint();
		markerPaint.setColor(Color.YELLOW);
		markerPaint.setStyle(Style.FILL);
		txtPaint = new Paint();
		txtPaint.setTextSize(8);
		txtPaint.setColor(Color.WHITE);
		txtPaint.setStyle(Style.FILL_AND_STROKE);
		txtPaint.setTextAlign(Paint.Align.CENTER);
		Paint selectedPaint = new Paint();
		selectedPaint.setTextSize(8);
		selectedPaint.setColor(Color.BLACK);

		selectedPaint.setTextAlign(Paint.Align.CENTER);
		Paint btnTxt = new Paint();
		btnTxt.setTextSize(8);
		btnTxt.setColor(Color.WHITE);

		btnTxt.setTextAlign(Paint.Align.CENTER);
		vtnTxt = new Paint();
		vtnTxt.setTextSize(8);
		vtnTxt.setColor(Color.WHITE);
		txtPaint.setStyle(Style.FILL_AND_STROKE);
		vtnTxt.setTextAlign(Paint.Align.CENTER);
		Paint borderPaint = new Paint();
		borderPaint.setColor(Color.WHITE);
		borderPaint.setStyle(Style.STROKE);
		borderPaint.setStrokeWidth(3);
		polyPaint = new Paint();
		polyPaint.setColor(Color.WHITE);
		polyPaint.setStyle(Style.STROKE);
		polyPaint.setStrokeWidth( 2.0f * getResources().getDisplayMetrics().density );
		polyPaint.setPathEffect( new DashPathEffect( new float[] {20,5,},0 ) );
		fgPaintSel = new Paint();
		fgPaintSel.setColor(Color.YELLOW);
		fgPaintSel.setStyle(Style.STROKE);
		fgPaintSel.setStrokeWidth(2);

		paintSimple = new Paint();
		paintSimple.setAntiAlias(true);
		paintSimple.setDither(true);
		paintSimple.setColor(Color.argb(248, 255, 255, 255));
		paintSimple.setStrokeWidth( 2.0f * getResources().getDisplayMetrics().density );
		paintSimple.setStyle(Style.STROKE);
		paintSimple.setStrokeJoin(Paint.Join.ROUND);
		paintSimple.setStrokeCap(Paint.Cap.ROUND);

		paintBlur = new Paint();
		paintBlur.set(paintSimple);
		paintBlur.setColor(Color.argb(245, 74, 138, 255));
		paintBlur.setStrokeWidth(5f * getResources().getDisplayMetrics().density);
		paintBlur.setMaskFilter(new BlurMaskFilter(10, BlurMaskFilter.Blur.NORMAL));

		if (getInstance()!=null)
			getInstance().registerListener(this,TrackerListener.Type.MAP);

		// REPLACEMENT START: Load user-selected map needle
		loadUserMapNeedle(); // Call a new method to load the user's selected needle
		personAway = BitmapFactory.decodeResource(getResources(), R.drawable.person_away); // Person away is still the old one
		// REPLACEMENT END

		teamStatusViewModel = new ViewModelProvider(getInstance().getActivity()).get(TeamStatusViewModel.class);
	}

	// New method to load the user's selected map needle
	private void loadUserMapNeedle() {
		try {
			int selectedNeedleIndex = GlobalState.getInstance().getGlobalPreferences().getI("map_needle_set");
			Log.d(TAG, "Selected map needle index: " + selectedNeedleIndex);
			if (selectedNeedleIndex == -1)
				selectedNeedleIndex = 0;
			// Retrieve the saved idex
			// Assume `MapNeedlePreference.cropAllNeedlesFromSet` requires the original full image set resource IDs
			// You'll need to retrieve these from R.array.map_needle_image_sets
			TypedArray ta = getContext().getResources().obtainTypedArray(R.array.map_needle_image_sets);
			List<Bitmap> allIcons = new ArrayList<>();
			for (int i = 0; i < ta.length(); i++) {
				int resourceId = ta.getResourceId(i, 0);
				if (resourceId != 0) {
					// Use the static helper method from MapNeedlePreference
					allIcons.addAll(MapNeedlePreference.cropAllNeedlesFromSet(getContext(), resourceId)); // Re-crop all icons
				}
			}
			ta.recycle(); // Important: Recycle TypedArray

			if (allIcons != null && allIcons.size() > selectedNeedleIndex) {
				selectedUserNeedle = allIcons.get(selectedNeedleIndex); // Set the selected user needle
			} else {
				Log.e("GisImageView", "Could not load selected user map needle. Index out of bounds or no icons loaded.");
				selectedUserNeedle = BitmapFactory.decodeResource(getResources(), R.drawable.person_active); // Fallback to default
			}
		} catch (Exception e) {
			Log.e("GisImageView", "Error loading user selected map needle: " + e.getMessage(), e);
			selectedUserNeedle = BitmapFactory.decodeResource(getResources(), R.drawable.person_active); // Fallback to default
		}
	}


	public void setViewModelStoreOwner(@NonNull LifecycleOwner owner) {
		// Initialize observers if not already (or ensure single registration)
		if (teamMembersObserver == null) {
			teamMembersObserver = new Observer<Set<GisPointObject>>() {
				@Override
				public void onChanged(Set<GisPointObject> teamMembers) {
					Log.d(TAG, "Team member GisObjects updated. Count: " + teamMembers.size());

					if (myMap != null) {
						GisLayer teamLayer = null;
						for (GisLayer layer : myMap.getLayers()) {
							if (layer.getId().equals("Team")) {
								teamLayer = layer;
								break;
							}
						}

						if (teamLayer != null) {
							teamLayer.clearObjectBag("Team");
							teamLayer.addObjectBag("Team", new HashSet<>(teamMembers), true);
							teamLayer.filterLayer(GisImageView.this);
						} else {
							//Log.e("GisImageView", "Team layer not found in myMap. Cannot update team objects.");
						}
					}
					postInvalidate(); // Simple redraw after data changes.
				}
			};
		}
		// Register observer with the provided LifecycleOwner
		teamStatusViewModel.teamMemberGisObjects.observe(owner, teamMembersObserver);

	}

	// NEW: Method to explicitly remove observers
	public void removeViewModelObservers() {
		if (teamStatusViewModel != null) {
			if (teamMembersObserver != null) {
				teamStatusViewModel.teamMemberGisObjects.removeObserver(teamMembersObserver);
				Log.d(TAG, "Removed teamMembersObserver from LiveData.");
			}
		}
		// Optional: clear references to allow GC
		teamMembersObserver = null;
	}


	private double pXR;
	private double pYR;
	private WF_Gis_Map myMap;
	private boolean allowZoom;
	private final IntBuffer intBuffer = new IntBuffer();
	private final PathBuffer pathBuffer = new PathBuffer();

	public void removeViewModelObservers(LifecycleOwner owner) {
		Log.d(TAG,"removing observer");
		teamStatusViewModel.teamMemberGisObjects.removeObservers(owner);
	}


	private static class PathBuffer {
		private final Path[] mBuffer = new Path[500];
		private int c=0;

		PathBuffer() {
			for (int i = 0; i<mBuffer.length;i++)
				mBuffer[i]= new Path();
		}

		Path getPath() {
			if (c<mBuffer.length)
				return mBuffer[c++];
			Log.d(TAG,"Ran out of path objects: "+(c++));
			return new Path();
		}

		void reset() {
			c=0;
		}
	}

	private static class IntBuffer {

		private final int[][] mBuffer = new int[1500][2];
		private int c=0;

		int[] getIntBuf() {
			if (c<mBuffer.length)
				return mBuffer[c++];
			Log.d(TAG,"Ran out of int arrays..");
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
	 *
	 */
	public void initialize(FragmentActivity fragment, WF_Gis_Map wf_Gis_Map, PhotoMeta pm,boolean allowZoom) {

		if (pm== null) {
			Log.e("vortex","Photometadata was null for "+wf_Gis_Map.getName());
		}
		rectBuffer.clear();
		mapLocationForClick=null;
		this.photoMetaData=pm;
		pXR = this.getImageWidth()/pm.getWidth();
		pYR = this.getImageHeight()/pm.getWidth(); // pYR should be based on height. Fix: pm.getHeight()
		// Fix: pYR = this.getImageHeight()/pm.getHeight();

		myMap = wf_Gis_Map;

		imgHReal = pm.N-pm.S;
		imgWReal = pm.E-pm.W;
		Map<String, String> gps_key_map = getInstance().getVariableConfiguration().createGpsKeyMap();
		myX = getInstance().getVariableCache().getVariable(gps_key_map, NamedVariables.MY_GPS_LAT);
		myY = getInstance().getVariableCache().getVariable(gps_key_map, NamedVariables.MY_GPS_LONG);
		this.allowZoom = allowZoom;


		setOnClickListener(v -> {
			Log.d(TAG,"Gets short! Clickable is "+GisImageView.this.isClickable());
			if (clickXY!=null)
				return;
			if (!GisImageView.this.isClickable()) {
				Log.d(TAG,"click outside popup?");
				if (myMap.wasShowingPopup()) {
					Log.d(TAG,"YES!");
					GisImageView.this.setClickable(true);
				}
				return;
			}

			calculateMapLocationForClick(polyVertexX,polyVertexY);


			if (gisTypeToCreate!=null) {
				if (newGisObj ==null) {
					Set<GisObject> bag;
					for (GisLayer l:myMap.getLayers()) {

						bag = l.getBagOfType(gisTypeToCreate.getName());
						if (bag!= null) {
							Log.d(TAG,"found a bag in layer "+ l.getLabel()+" of type "+gisTypeToCreate.getName());
							newGisObj = createNewGisObject(gisTypeToCreate,bag);
							if (gisTypeToCreate.getGisPolyType()==GisObjectType.Point)
								myMap.setVisibleCreate(true,newGisObj.getLabel());
						}
					}
				}
				if (newGisObj !=null) {

					if (newGisObj instanceof GisPathObject)
						newGisObj.getCoordinates().add(mapLocationForClick);

				} else
					Log.e("vortex","New GisObj is null!");
			} else {
				Log.e("vortex", "gistype null!");
				myMap.showCandidates(null);
			}
			clickWasShort = true;
			invalidate();

		});



		setOnLongClickListener(v -> {
			Log.d(TAG,"Gets long! clickable is "+GisImageView.this.isClickable());
			if (clickXY!=null || !GisImageView.this.isClickable())
				return false;
			calculateMapLocationForClick(polyVertexX,polyVertexY);
			clickWasShort = false;
			invalidate();
			return true;
		});

		initializeAndSiftGisObjects();
		initialized = true;
	}

	public boolean isInitialized() {
		return initialized;
	}

	public void editSelectedGop() {
		if (touchedGop!=null) {
			newGisObj = touchedGop;
			currentCreateBag = touchedBag;
			cancelGisObjectCreation();
			startGisObjectCreation(touchedGop.getFullConfiguration());
			myMap.setVisibleCreate(true, newGisObj.getLabel());

		}

	}


	/**
	 *
	 * Filters gis objects in all layers that are visible in this view.
	 * This should be called once on initialization and whenever map view parameters (zoom, pan) change.
	 */
	public void initializeAndSiftGisObjects() {
		if (myMap==null) {
			Log.e("vortex","myMap null in initializeAndSiftGisObjects");
			return;
		}
		for (GisLayer layer :myMap.getLayers()) {
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
			Log.d(TAG,"fixed xy"+fixedX+","+fixedY);
		}
	}

	private Location mapLocationForClick=null;
	private float[] clickXY;
	private double imgHReal;
	private double imgWReal;

	private GisObject newGisObj;

	private Set<GisObject> currentCreateBag;

	private float[] translateToReal(float mx,float my) {
		float fixScale = scale * scaleAdjust;
		Log.d(TAG,"fixscale: "+fixScale);
		mx = (mx-x)/fixScale;
		my = (my-y)/fixScale;
		return new float[]{mx,my};
	}


	private Location translateRealCoordinatestoMap(float[] xy) {
		Log.d(TAG,"IMGW: "+this.getImageWidth());
		float rX = (float)this.getImageWidth()/2+ xy[0];
		float rY = (float)this.getImageHeight()/2+ xy[1];
		pXR = photoMetaData.getWidth()/this.getImageWidth();
		pYR = photoMetaData.getHeight()/this.getImageHeight(); // Corrected
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
		if (l == null)
			return false;
		boolean isInside = false;
		double mapDistX = l.getX()-photoMetaData.W;
		double mapDistY = l.getY()-photoMetaData.S;
		if ((mapDistX <=imgWReal && mapDistX>=0) && (mapDistY <=imgHReal && mapDistY>=0)) {
			isInside = true;
		}

		pXR = this.getImageWidth()/photoMetaData.getWidth();
		pYR = this.getImageHeight()/photoMetaData.getHeight(); // Corrected

		double pixDX = mapDistX*pXR;
		double pixDY = mapDistY*pYR;
		float rX = ((float)pixDX)-(float)this.getImageWidth()/2;
		float rY = (float)this.getImageHeight()/2-((float)pixDY);

		xy[0]=(int)rX;
		xy[1]=(int)rY;

		return isInside;

	}

	private Location calculateMapLocationForClick(float x, float y) {
		clickXY=translateToReal(x,y);
		mapLocationForClick = translateRealCoordinatestoMap(clickXY);
		Log.d(TAG,"click at "+x+","+y);
		Log.d(TAG,"click at "+mapLocationForClick.getX()+","+mapLocationForClick.getY());
		return mapLocationForClick;
	}

	private GisObject createNewGisObject(
			FullGisObjectConfiguration gisTypeToCreate, Set<GisObject> bag) {
		Map<String, String> keyHash = Tools.copyKeyHash(gisTypeToCreate.getObjectKeyHash().getContext());
		GisObject ret=null;

		if (keyHash!=null) {
			String uid = UUID.randomUUID().toString();
			Log.d(TAG,"HACK: Adding uid: "+uid);
			keyHash.put("uid", uid);
			Variable gistyp = getInstance().getVariableCache().getVariable(keyHash,NamedVariables.GIS_TYPE);
			if (gistyp!=null ) {
				gistyp.setValue(gisTypeToCreate.getObjectKeyHash().getContext().get("gistyp"));
				Log.d(TAG, "keyhash for new obj is: " + keyHash + " and gistyp: " + gistyp.getValue());
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
					myDots = new ArrayList<>();
					ret = new GisMultiPointObject(gisTypeToCreate, keyHash,myDots,gisTypeToCreate.getStatusVariable(),null);
					break;
				case Polygon:
					ret = new GisPolygonObject(gisTypeToCreate, keyHash,
							"",GisConstants.SWEREF,gisTypeToCreate.getStatusVariable(),null);
					break;

			}
			Log.d(TAG,"Adding "+ret.toString()+" to bag "+bag.toString());
			if (ret !=null) {
				ret.markAsUseful();
				bag.add(ret);

				currentCreateBag = bag;
			}
		} else
			Log.e("vortex","Cannot create, object keyhash is null!!!");
		return ret;
	}

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

					String keyPairs = Tools.convertToKeyPairs(newGisObj.getKeyHash());
					int rowsAffected = getInstance().getDb().erase(keyPairs,null);
					if (rowsAffected>0) {
						Log.d(TAG,"aff: "+rowsAffected);
						getInstance().getLogger().addText(" erased " + rowsAffected + " entries for GIS object [" + newGisObj.getLabel() + "]");
						getInstance().getDb().insertEraseAuditEntry(keyPairs,null);
					}
					cancelGisObjectCreation();
					Log.d(TAG,"Go BACK: NewGisObj removed");
				} else
					invalidate();
			}

		}
	}

	public void createOk() {
		if (currentCreateBag!=null) {
			Log.d(TAG,"inserting new GIS object.");
			getInstance().getDb().insertGisObject(newGisObj);

			gisTypeToCreate=null;
			touchedBag = currentCreateBag;
			currentCreateBag=null;
			touchedGop = newGisObj;
			if (touchedGop instanceof GisPathObject)
				touchedGop.clearCache();
			newGisObj=null;
			myMap.setVisibleAvstRikt(true,touchedGop);

			this.redraw();
		} else
			Log.e("vortex","cannot create...createbag null");
	}

	private final List<GisObject> candidates = new ArrayList<>();
	private final static Random rnd = new Random();
	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		canvas.save();
		try {
			float adjustedScale = scale * scaleAdjust;
			myMap.setZoomButtonVisible(allowZoom && adjustedScale > 2.0f && this.getDrawable() != null);

			canvas.translate(x, y);
			if(adjustedScale != 1.0f) {
				canvas.scale(adjustedScale, adjustedScale);
			}
			candidates.clear();

			//for (int layerCount= myMap.getLayers().size()-1; layerCount>=0;layerCount--) {
			for (int layerCount= 0; layerCount< myMap.getLayers().size();layerCount++) {

				GisLayer layerO = myMap.getLayers().get(layerCount);

				if (!layerO.isVisible()) {
					continue;
				}

				pXR = this.getImageWidth()/photoMetaData.getWidth();
				pYR = this.getImageHeight()/photoMetaData.getHeight(); // Corrected
				Map<String, Set<GisObject>> bags = layerO.getGisBags();
				Map<String, Set<GisFilter>> filterMap = layerO.getFilters();
				boolean isTeamLayer = layerO.getId().equals("Team");


				if (bags!=null && !bags.isEmpty()) {

					for (String key:bags.keySet()) {

						Set<GisObject> bagOfObjects = bags.get(key);
						if (bagOfObjects != null) {
							for (GisObject go : bagOfObjects) {
								if (!go.isUseful() || (go.equals(touchedGop)) || isExcludedByStandardFilter(go.getStatusVariableValue())) {
									continue;
								}
								if (go instanceof GisPointObject) {
									GisPointObject gop = (GisPointObject) go;

									// Debugging: Log GisPointObject details before translating/drawing
									if (gop.getLocation() == null) {
										Log.e("GisImageView", "ERROR: GisPointObject " + gop.getLabel() + " (UUID: " + gop.getKeyHash().get("uuid") + ") has a null Location. Skipping drawing.");
										// You could also mark it for destruction here if a null location means it's invalid
										// gop.markForDestruction();
										continue; // Skip drawing this object if its location is null
									}
									Bitmap bitmap = gop.getIcon(); // Get icon from GisObject.
									String color = gop.getColor();
									String borderColor = gop.getBorderColor();
									if (gop.isDynamic()) {
										int[] xy = intBuffer.getIntBuf();
										boolean inside = translateMapToRealCoordinates(gop.getLocation(), xy);
										if (!inside) {
											if (gop.equals(userGop)) {
												myMap.showCenterButton(false);
												userGop = null;
											}
											continue;
										} else {
											if (gop.isUser()) {
												// Use the selectedUserNeedle for the user's icon
												bitmap = selectedUserNeedle; // Assign the custom selected user needle
												userGop = gop;
												myMap.showCenterButton(true);
												gop.setTranslatedLocation(xy);
											}
										}
									}
									float radius = gop.getRadius();
									Style style = gop.getStyle();
									PolyType polyType = gop.getShape();

									String statusColor = gop.getStatusColor();
									if (statusColor != null)
										color = statusColor;
									Set<GisFilter> filters = filterMap != null ? filterMap.get(key) : null;
									if (filters != null && !filters.isEmpty()) {
										for (GisFilter filter : filters) {
											if (filter.isActive()) {
												if (!gop.hasCachedFilterResult(filter)) {
													Boolean result = Expressor.analyzeBooleanExpression(filter.getExpression(), gop.getKeyHash(), null);
													gop.setCachedFilterResult(filter, result);
												}
												if (gop.getCachedFilterResult(filter)) {
													bitmap = filter.getBitmap();
													radius = filter.getRadius();
													style = filter.getStyle();
													polyType = filter.getShape();
												}
											} else
												Log.d(TAG, "Filter turned off!");
										}
									}
									int[] xy = gop.getTranslatedLocation();

									if (xy == null && gop.getCoordinates() != null && !gop.getCoordinates().isEmpty()) {
										Log.e("GisImageView", "Getzzz for "+gop.getLabel());
										xy = intBuffer.getIntBuf();
										translateMapToRealCoordinates(gop.getCoordinates().get(0), xy);
										gop.setTranslatedLocation(xy);
										xy = gop.getTranslatedLocation();
									}

									// Pass the specific bitmap to drawPoint
									drawPoint(canvas, bitmap, radius, gop.getFullConfiguration().getLineWidth(), color, borderColor,style, polyType, xy, adjustedScale, gop.getFullConfiguration().useIconOnMap(), layerO.isBold()); // Use the `bitmap` variable

									if (layerO.showLabels()) {
										if (bitmap!=null && gop.getFullConfiguration().useIconOnMap())
											drawGopLabel(canvas, xy, go.getLabel(), bCursorPaint, txtPaint,20);
										else
											drawGopLabel(canvas, xy, go.getLabel(), bCursorPaint, txtPaint);
									}


								} else if (go instanceof GisPathObject) {
									if (go instanceof GisMultiPointObject) {
										if (((GisMultiPointObject) go).isLineString()) {
											Log.d(TAG, "This is a multipoint. Path is useless.");
										}
									}

									drawGop(canvas, layerO, go, false);
								}

								if (!candMenuVisible && touchedGop == null && gisTypeToCreate == null && mapLocationForClick != null && go.isTouchedByClick(mapLocationForClick, pXR, pYR) && !go.equals(userGop)) {
									boolean s = candidates.add(go);
									if (!s) {
										Log.d(TAG, go.getLabel() + " exists: " + candidates.contains(go) + " loc " + go.getCoordinates() + " gid: " + go.getKeyHash());
										for (GisObject g : candidates)
											Log.d(TAG, g.getLabel() + " loc " + g.getCoordinates() + " gid: " + g.getKeyHash());

									} else
										Log.d(TAG, "cand added: " + go.getLabel());

								}
							}
						}
					}
				}
			}

			if (candidates.size()>1) {
				if (!candMenuVisible) {
					candidates.sort((lhs, rhs) -> (int) (lhs.getDistanceToClick() - rhs.getDistanceToClick()));
					List<GisObject> candies = new ArrayList<>(candidates);
					candMenuVisible = true;
					touchedBag=null;
					myMap.showCandidates(candies);
				}

			} else if (!candidates.isEmpty()) {
				touchedGop=candidates.iterator().next();
				riktLinjeStart=null;
				touchedBag=null;
			}

			if (touchedGop!=null) {
				if (touchedBag==null || touchedLayer == null) {
					for (GisLayer layer : myMap.getLayers()) {
						touchedBag = layer.getBagContainingGo(touchedGop);
						if (touchedBag != null) {
							Log.d(TAG, "setting touchedlayer for "+touchedGop.getKeyHash());
							touchedLayer = layer;
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
						double mX = Double.parseDouble(myX.getValue());
						double mY = Double.parseDouble(myY.getValue());
						riktLinjeStart = intBuffer.getIntBuf();riktLinjeEnd = intBuffer.getIntBuf();
						translateMapToRealCoordinates(new SweLocation(mX,mY),riktLinjeStart);
						translateMapToRealCoordinates(touchedGop.getLocation(),riktLinjeEnd);
					}
					if (touchedLayer == null) {
						LogRepository o = getInstance().getLogger();
						o.addText("");
						o.addCriticalText("The selected gis object with name "+touchedGop.getFullConfiguration().getName()+" is not attached to any layer");
						Log.d(TAG,"TOUCHEDLAYER WAS NULL. TouchedGop was: "+touchedGop.getKeyHash());
					}


					drawGop(canvas,touchedLayer,touchedGop,true);
					if (riktLinjeStart!=null) {
						canvas.drawLine(riktLinjeStart[0], riktLinjeStart[1], riktLinjeEnd[0], riktLinjeEnd[1], fgPaintSel);
					}
				} else {
					Log.e("vortex", "The touched object does not belong to a bag.");
					touchedGop = null;
				}
			}
		} catch(Exception e) {
			if (getInstance()!=null) {
				LogRepository o = getInstance().getLogger();
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				o.addCriticalText(sw.toString());
				e.printStackTrace();
			}

		}

		if (newGisObj!=null) {
			double lengthOfPath = 0;
			myMap.setVisibleCreate(true,newGisObj.getLabel());
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
		mapLocationForClick=null;
		clickXY=null;
		intBuffer.reset();
		pathBuffer.reset();
	}

	private final static String isFreshColor = "#7CFC00";
	private final static String isOverAnHourColor = "#D3D3D3";
	private final static String isOverHalfAnHourColor = "#050FF5";
	private final static String isOverAQuarterColor = "#FFFA0A";


	private boolean isExcludedByStandardFilter(String status) {
		if (status==null)
			return false;
		return !myMap.isNotExcluded(status);
	}

	public void selectGop(GisObject go) {
		touchedGop=go;
		candMenuVisible=false;
	}

	private void drawGop(Canvas canvas, GisLayer layerO, GisObject go, boolean selected) {

		boolean beingDrawn = false;
		int[] xy;

		if (newGisObj != null) {
			beingDrawn = go.equals(newGisObj);
		}
		GisPointObject gop;
		boolean isBold = (layerO==null?false:layerO.isBold());
		if (go instanceof GisPointObject) {
			gop = (GisPointObject) go;
			if (gop.getTranslatedLocation() != null) {
				// The actual drawing of the point's shape and color.
				// If useIconOnMap is true, drawPoint will use the bitmap passed to it.
				// If not, it will draw a circle/rect/triangle with radius/color.
				drawPoint(canvas, gop.getIcon(), gop.getRadius(),gop.getFullConfiguration().getLineWidth(), "red",null, Style.FILL, gop.getShape(), gop.getTranslatedLocation(), 1,false,isBold);
			} else
				Log.e("vortex", "NOT calling drawpoint since translatedlocation was null");

		} else if (go instanceof GisPathObject) {
			boolean singlePath = false,isPolygon = (go instanceof GisPolygonObject);

			GisPathObject gpo = (GisPathObject) go;

			if (beingDrawn) {
				Path p = createPathFromCoordinates(gpo.getCoordinates(),false);
				if (p != null)
					canvas.drawPath(p, polyPaint);
				xy = intBuffer.getIntBuf();
				translateMapToRealCoordinates(gpo.getCoordinates().get(gpo.getCoordinates().size()-1),xy);
				drawPoint(canvas, null,2,go.getFullConfiguration().getLineWidth(), "white", null,Style.STROKE, PolyType.circle, xy,1,false,isBold);
			} else {
				if (go instanceof GisPolygonObject) {
					if (gpo.getPaths() == null) {
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

					Path p;
					if (gpo.getPaths() != null) {
						p = gpo.getPaths().get(0);
					} else {
						p = createPathFromCoordinates(gpo.getCoordinates(),isPolygon);
						gpo.addPath(p);
					}
					if (p!=null)
						drawPath(p,selected,canvas,go,isBold);
				} else {
					List<Path> paths = gpo.getPaths();
					if (paths != null) {
						for (Path p : paths) {
							drawPath(p,selected,canvas,go,isBold);
						}
					}
				}
			}
		}

		if (!selected&&layerO!=null && layerO.showLabels() && !beingDrawn) {
			xy = intBuffer.getIntBuf();
			translateMapToRealCoordinates(go.getLocation(),xy);
			drawGopLabel(canvas,xy,go.getLabel(), bCursorPaint,vtnTxt);
		}


	}

	private void drawPath(Path p, boolean selected, Canvas canvas, GisObject go, boolean isBold) {
		if (selected) {
			Log.d(TAG,"isbold"+isBold);
			canvas.drawPath(p, paintBlur);
			canvas.drawPath(p, paintSimple);
		} else {
			float linew = go.getFullConfiguration().getLineWidth();
			canvas.drawPath(p,
					createPaint(
							go.getStatusColor(),
							Style.STROKE,
							(int) ((isBold?linew+2:linew) * getResources().getDisplayMetrics().density) ));
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
	 */
	private final Map<int[],Rect> rectBuffer = new HashMap<>();
	private void drawGopLabel(Canvas canvas, int[] xy, String mLabel, Paint bgPaint, Paint txtPaint) {
		drawGopLabel(canvas, xy, mLabel, bgPaint, txtPaint,GisImageView.LabelOffset);
	}
	private void drawGopLabel(Canvas canvas, int[] xy, String mLabel, Paint bgPaint, Paint txtPaint, int offset) {
		Rect bounds = rectBuffer.get(xy);
		if (bounds== null) {
			bounds = new Rect();
			rectBuffer.put(xy,bounds);
		}
		mLabel=mLabel==null?"":mLabel;
		txtPaint.getTextBounds(mLabel, 0, mLabel.length(), bounds);
		int textH = bounds.height()/2;
		bounds.offset(xy[0] -bounds.width()/2, xy[1] - (bounds.height()/2+(int) (float) offset));
		bounds.set(bounds.left-2,bounds.top-2,bounds.right+2,bounds.bottom+2);
		canvas.drawRect(bounds, bgPaint);
		canvas.drawText(mLabel, bounds.centerX(), bounds.centerY()+textH,txtPaint);

	}

	private void drawPoint(Canvas canvas, Bitmap bitmap, float radius, float linew, String color, String border_color,Style style, PolyType type, int[] xy, float adjustedScale, boolean useIconOnMap, boolean isBold) {

		Rect r;
		// If a bitmap is provided AND useIconOnMap is true for this GisObject, use the bitmap
		if (useIconOnMap && bitmap!=null ) {
			final int DRAW_WIDTH_DP = 16;
			final int DRAW_HEIGHT_DP = 24;
			float density = getResources().getDisplayMetrics().density;
			int drawPxWidth = (int)(DRAW_WIDTH_DP * density);
			int drawPxHeight = (int)(DRAW_HEIGHT_DP * density);
			r = new Rect();
			// Calculate left and right to center horizontally on xy[0]
			int left = xy[0] - drawPxWidth / 2;
			int right = xy[0] + drawPxWidth / 2;
			// Calculate top and bottom so that the bottom of the bitmap is at xy[1]
			int bottom = xy[1];
			int top = xy[1] - drawPxHeight; // Shift up by the full height of the bitmap
			r.set(left, top, right, bottom);
			canvas.drawBitmap(bitmap, null, r, null);
		} else { // Fallback to drawing a shape (circle, rect, triangle)
			boolean hasBorder = (border_color!=null);
			int translBw=0;
			Paint borderPaint=null;
			if (hasBorder) {
				translBw = (int) (linew * getResources().getDisplayMetrics().density);
				borderPaint = createPaint(border_color, Style.STROKE, (int) (linew * getResources().getDisplayMetrics().density));
			}
			if (type == PolyType.circle) {
				canvas.drawCircle(xy[0], xy[1], radius, createPaint(color, style, linew, isBold));
				if (hasBorder)
					canvas.drawCircle(xy[0], xy[1], radius+translBw, borderPaint);
			}
			else if (type == PolyType.rect) {
				int diam = (int) (radius / 2);
				int left = xy[0] - diam;
				int right = xy[0] + diam;
				int top = xy[1] - diam;
				int bottom = xy[1] + diam;
				canvas.drawRect(left, top, right, bottom, createPaint(color, style, linew, isBold));
				if (hasBorder)
					canvas.drawRect(left-translBw, top-translBw, right+translBw, bottom+translBw, borderPaint);

			} else if (type == PolyType.triangle) {
				drawTriangle(canvas, radius, xy[0], xy[1], createPaint(color, style, linew, isBold));
				if (hasBorder)
					drawTriangle(canvas,radius+translBw,xy[0], xy[1], borderPaint);
			}
		}
	}


	public void drawTriangle(Canvas canvas, float radius, int x, int y, Paint paint) {
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
		riktLinjeStart=null;
		riktLinjeEnd=null;
		invalidate();
	}


	private int[] riktLinjeStart,riktLinjeEnd;

	private final static int TimeOut = 3;

	private void displayDistanceAndDirection() {
		final int interval = TimeOut*1000;

		if (handler==null) {
			handler = new Handler();
			Runnable runnable = new Runnable(){
				public void run() {
					displayDistanceAndDirectionL(null);
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
			return;
		}

		if (myX==null||myY==null|| getInstance()==null) {
			myMap.setAvstTxt("Config");
			myMap.setRiktTxt("fault!");
			handler=null;
			return;
		}

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
			myMap.setAvstTxt("Lost signal");
			myMap.setRiktTxt(timeDiff+" s");

		}

		String mXs = myX.getValue();
		String mYs = myY.getValue();
		double mX = Double.parseDouble(mXs);
		double mY = Double.parseDouble(mYs);
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
		int currentDistance = (int) Geomatte.sweDist(mY, mX, gY, gX);
		int rikt = (int)(Geomatte.getRikt2(mY, mX, gY, gX)*57.2957795);
		myMap.setAvstTxt(currentDistance >9999?(currentDistance /1000+"km"):(currentDistance +"m"));
		myMap.setRiktTxt(rikt+Deg);

	}


	private final Map<String,Paint> paintCache = new HashMap<>();


	public Paint createPaint(String color, Paint.Style style, float line_width, boolean isBold) {
		return createPaint(color,style, (int) ((isBold?line_width+2:line_width) * getResources().getDisplayMetrics().density) );
	}

	private Paint createPaint(String color, Paint.Style style, int strokeWidth) {
		String key = style==null?color+strokeWidth:color+strokeWidth+style.name();
		Paint p = paintCache.get(key);
		if (p!=null) {
			return p;
		}
		p = new Paint();
		p.setColor(color!=null?Color.parseColor(color):Color.YELLOW);
		p.setStyle(style!=null?style:Style.FILL);
		p.setStrokeWidth(strokeWidth);
		paintCache.put(key, p);
		return p;
	}

	private FullGisObjectConfiguration gisTypeToCreate;

	public void runSelectedWf() {
		invalidate();
		if (touchedGop!=null)
			runSelectedWf(touchedGop);
		unSelectGop();

	}
	private void runSelectedWf(GisObject gop) {

		getInstance().setDBContext(new DB_Context(null,gop.getKeyHash()));
		Log.d(TAG,"Setting current keyhash to "+gop.getKeyHash());
		String target = gop.getWorkflow();
		Workflow wf = getInstance().getWorkflow(target);
		if (wf ==null) {
			Log.e("vortex","missing click target workflow");
			new AlertDialog.Builder(ctx)
					.setTitle("Missing workflow")
					.setMessage("No workflow associated with the GIS object or workflow not found: ["+target+"]. Check your XML.")
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setCancelable(false)
					.setNeutralButton("Ok", (dialog, which) -> {

					})
					.show();
		} else {
			GlobalState.getInstance().changePage(wf,gop.getStatusVariableId());

		}
	}


	public void centerOnUser() {
		centerOn(userGop);
	}

	public void centerOnCurrentDot() {
		centerOn(newGisObj);
	}


	private void centerOn(GisObject gisObject) {

		if (gisObject!=null) {
			int[] xy = intBuffer.getIntBuf();
			boolean inside = translateMapToRealCoordinates(gisObject.getLocation(),xy);
			if (inside) {
				setPosition(fixedX-xy[0]*scaleAdjust,fixedY-xy[1]*scaleAdjust);
				this.invalidate();
			}
		}
	}


	public void hideImage() {
		this.setStaticMeasure(this.getImageWidth(),this.getImageHeight());
		setImageDrawable(null);
	}

	public GisObject getGopBeingCreated() {
		return newGisObj;
	}

	public void cancelGisObjectCreation() {
		if (gisTypeToCreate!=null) {
			gisTypeToCreate=null;
			if (newGisObj!=null) {
				Variable gistyp = getInstance().getVariableCache().getVariable(newGisObj.getKeyHash(),NamedVariables.GIS_TYPE);
				if (gistyp!=null) {
					gistyp.deleteValue();
					Log.d(TAG, "Removed gistype value for new gisobject");
				}
				currentCreateBag.remove(newGisObj);
				currentCreateBag =null;
				newGisObj=null;
			}

		}
		invalidate();
	}


	public void startGisObjectCreation(FullGisObjectConfiguration fop) {
		Toast.makeText(ctx,"Click on map to register a coordinate",Toast.LENGTH_LONG).show();
		this.unSelectGop();
		gisTypeToCreate=fop;
	}

	public void deleteSelectedGop() {
		if (touchedGop!=null) {
			String keyPairs = Tools.convertToKeyPairs(touchedGop.getKeyHash());
			int rowsAffected = getInstance().getDb().erase(keyPairs,null);
			if (rowsAffected>0) {
				Log.d(TAG,"aff: "+rowsAffected);
				getInstance().getLogger().addText(" erased " + rowsAffected + " entries for GIS object [" + touchedGop.getLabel() + "]");
				getInstance().getDb().insertEraseAuditEntry(keyPairs,null);
			}
			touchedBag.remove(touchedGop);
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
					.setNeutralButton("Ok", (dialog, which) -> {

					})
					.show();
		} else
			Log.e("vortex","Touchedgop null in describeSelectedGop");
	}

	private long mostRecentGPSValueTimeStamp=-1;
	@Override
	public void gpsStateChanged(GPS_State gps_state) {
		if (gps_state.state==GPS_State.State.newValueReceived||gps_state.state==GPS_State.State.ping) {
			mostRecentGPSValueTimeStamp = System.currentTimeMillis();
			displayDistanceAndDirectionL(gps_state);
			teamStatusViewModel.gpsStateChanged(gps_state);
		}
		this.postInvalidate();

	}



	public List<Location> getRectGeoCoordinates() {

		List<Location> ret = new ArrayList<> ();

		Location topCorner = calculateMapLocationForClick(0,0);
		Location bottomCorner = calculateMapLocationForClick(this.displayWidth,this.displayHeight);

		ret.add(topCorner);
		ret.add(bottomCorner);

		return ret;

	}

	public Rect getCurrentViewSize(float fileImageWidth,float fileImageHeight) {

		final float Xs = (float)this.getScaledWidth()/2-x;
		final float Ys = (float)this.getScaledHeight()/2-y;
		final float Xe = Xs+this.displayWidth;
		final float Ye = Ys+this.displayHeight;


		final float scaleFx = fileImageWidth/this.getScaledWidth();
		final float scaleFy = fileImageHeight/this.getScaledHeight();


		final int left  = (int)(Xs * scaleFx);
		final int top  = (int)(Ys * scaleFy);

		final int right = (int)(Xe * scaleFx);
		final int bottom =(int)(Ye * scaleFy);

		Rect r = new Rect(left,top,right,bottom);

		Log.d(TAG,"top bottom left right "+top+","+bottom+","+left+","+right);
		return r;
	}

}