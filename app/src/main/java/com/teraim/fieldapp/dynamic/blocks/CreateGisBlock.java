package com.teraim.fieldapp.dynamic.blocks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.lifecycle.ViewModelProvider;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.AsyncResumeExecutorI;
import com.teraim.fieldapp.dynamic.types.GisLayer;
import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.MapGisLayer;
import com.teraim.fieldapp.dynamic.types.PhotoMeta;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisConstants;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.WF_Gis_Map;
import com.teraim.fieldapp.loadermodule.ConfigurationModule;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.loadermodule.ModuleLoaderCb;
import com.teraim.fieldapp.loadermodule.PhotoMetaI;
import com.teraim.fieldapp.loadermodule.configurations.AirPhotoMetaDataIni;
import com.teraim.fieldapp.loadermodule.configurations.AirPhotoMetaDataJgw;
import com.teraim.fieldapp.loadermodule.configurations.AirPhotoMetaDataXML;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.ui.ModuleLoaderViewModel;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;
import com.teraim.fieldapp.utils.Tools.Unit;
import com.teraim.fieldapp.utils.Tools.WebLoaderCb;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class CreateGisBlock extends Block {


	private transient GlobalState gs;
	private transient Cutout cutOut = null;
	private transient WF_Context myContext;
	private transient LoggerI o;
	private transient WF_Gis_Map gis = null;
	private transient List<MapGisLayer> mapLayers;
	private transient AsyncResumeExecutorI cb;
	private transient PhotoMeta photoMetaData;
	private transient List<GisLayer> myLayers = null;
	private transient boolean aborted = false;
	private transient Rect r = null;
	private transient String cachedImgFilePath="";
	private static final int MAX_NUMBER_OF_PICS = 100;
	private final String name,source,containerId,N,E,S,W;
	private boolean isVisible = false;
	private final boolean hasSatNav;
    private final boolean showTeam;
	private final List<EvalExpr> sourceE;
	public boolean hasCarNavigation() {
		return hasSatNav;
	}
	public boolean isTeamVisible() { return showTeam;}

	public CreateGisBlock(String id,String name,
						  String containerId,boolean isVisible,String source,String N,String E, String S,String W, boolean hasSatNav,boolean showTeam) {
		super();

		this.name = name;
		this.containerId=containerId;
		this.isVisible=isVisible;
		this.blockId=id;
		this.sourceE=Expressor.preCompileExpression(source);
		this.source=source;
		this.N=N;
		this.E=E;
		this.S=S;
		this.W=W;
		this.hasSatNav=hasSatNav;
		this.showTeam=showTeam;

	}
    /**
	 *
	 * @param myContext
	 * @param cb
	 * @return true if loaded. False if executor should pause.
	 */

	public boolean create(WF_Context myContext,final AsyncResumeExecutorI cb) {
		mapLayers  = new ArrayList<MapGisLayer>();
		aborted = false;
		Log.d("vortex","in create for createGisBlock!");
		Context ctx = myContext.getContext();
		gs = GlobalState.getInstance();
		o = gs.getLogger();
		this.cb=cb;
		this.myContext = myContext;
		PersistenceHelper ph = gs.getPreferences();
		PersistenceHelper globalPh = gs.getGlobalPreferences();

		final String serverFileRootDir = globalPh.get(PersistenceHelper.SERVER_URL)+globalPh.get(PersistenceHelper.BUNDLE_NAME).toLowerCase()+"/extras/";
		final String cacheFolder = GlobalState.getInstance().getContext().getFilesDir()+"/"+globalPh.get(PersistenceHelper.BUNDLE_NAME).toLowerCase(Locale.ROOT)+"/cache/";


		if (sourceE==null ) {
			Log.e("vortex","Image url evaluates to null! GisImageView will not load");
			o.addRow("");
			o.addRedText("GisImageView failed to load. No picture defined or failure to parse: "+source);
			//continue execution immediately.
			return true;
		}

		final String pics = Expressor.analyze(sourceE);
		Log.d("vortex","ParseString result: "+pics);
		//Load asynchronously. Put up a loadbar.
		//Need one async load per image.
		String[] picNames=null;
		if (pics!=null && pics.contains(",")) {
			picNames = pics.split(",");
			Log.d("vortex","found "+picNames.length+" pics");

		} else {
			picNames = new String[1];
			picNames[0]=pics;
		}
//		boolean allLoaded=false;
		final String masterPicName = picNames[0];

		for (int i=0;i<picNames.length;i++) {
			final boolean isLast = (i == picNames.length-1);
			final int I = i;
			final String picName = picNames[i];
			Tools.onLoadCacheImage(serverFileRootDir, picName, cacheFolder, new WebLoaderCb() {
				@Override
				public void progress(int bytesRead) {

				}
				@Override
				public void loaded(Boolean result) {
					if (!aborted) {
						if (result) {
							Log.d("vortex", "picture " + picName + " now in cache.");
							MapGisLayer mapLayer;
							if (picName.equals(masterPicName)) {
								mapLayer = new MapGisLayer(gis, GisConstants.DefaultTag, picName);

							} else
								mapLayer = new MapGisLayer(gis, "bg" + (I), picName);
							Log.d("vortex", "Added layer: " + mapLayer.getLabel());
							mapLayers.add(mapLayer);
						} else {
							Log.e("vortex", "Picture not found. "+serverFileRootDir+picName);
							String image_failed_to_load = ctx.getResources().getString(R.string.image_failed_to_load);
							if (gs.getGlobalPreferences().getB(PersistenceHelper.DEVELOPER_SWITCH)) {
								o.addRow("");
								o.addRedText(image_failed_to_load + serverFileRootDir + picName);
							}
							if (picName.equals(masterPicName)) {
								aborted = true;
								cb.abortExecution(image_failed_to_load + serverFileRootDir + picName);
							}

						}

						if (isLast)
							loadImageMetaData(masterPicName, serverFileRootDir, cacheFolder);

					}

				}
			});
		}
		return false;
	}



	private void createAfterLoad(PhotoMeta photoMeta, final String cacheFolder, final String fileName) {
		this.photoMetaData=photoMeta;
		cachedImgFilePath = cacheFolder+fileName;
		final Container myContainer = myContext.getContainer(containerId);

		if (myContainer!=null && photoMetaData!=null) {
			LayoutInflater li = LayoutInflater.from(myContext.getContext());
			final FrameLayout mapView = (FrameLayout)li.inflate(R.layout.image_gis_layout, null);
			final View avstRL = mapView.findViewById(R.id.avstRL);
			boolean found = false;
			GisLayer masterLayer = null;
			Log.d("banjo","cache path "+cachedImgFilePath+" mapl: "+mapLayers);
			for (MapGisLayer layer:mapLayers) {
				Log.d("banjo","Layer: "+layer.getImageName());
				if (layer.isVisible()) {
					cachedImgFilePath = cacheFolder + layer.getImageName();
					found = true;
					Log.d("banjo","layer with name "+layer.getLabel()+" now visible" );
					break;
				}
				if (layer.getLabel().equals(GisConstants.DefaultTag)) {
					Log.d("banjo","masterlayer found.");
					masterLayer = layer;
				}
			}
			Log.d("mask","cachepath2 "+cachedImgFilePath);
			if (!found) {
				if (masterLayer!=null) {
					masterLayer.setVisible(true);
					Log.d("gurk", "masterlayer is now visible.");
				} else
					Log.e("vortex", "no layer with label Def found");
			}

			if (cutOut==null) {

				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeFile(cachedImgFilePath, options);
                int imageHeight = options.outHeight;
                int imageWidth = options.outWidth;
				Log.d("vortex","image rect h w is "+ imageHeight +","+ imageWidth);
				r = new Rect(0,0, imageWidth, imageHeight);
			} else {
				Log.d("vortex","This is a cutout! "+cachedImgFilePath);
				r = cutOut.r;
				Location topC = cutOut.geoR.get(0);
				Location botC = cutOut.geoR.get(1);
				photoMetaData = new PhotoMeta(topC.getY(),botC.getX(),botC.getY(),topC.getX());
				cutOut=null;
				mapLayers.clear();
			}

			new Handler().postDelayed(new Runnable() {
				public void run() {

					Bitmap bmp = Tools.getScaledImageRegion(myContext.getContext(),cachedImgFilePath,r);
					if (bmp!=null) {

						gis = new WF_Gis_Map(CreateGisBlock.this,r,blockId, mapView, isVisible, bmp,myContext,photoMetaData,avstRL,myLayers,r.width(),r.height());

						//need to throw away the reference to myLayers.
						myLayers=null;
						myContainer.add(gis);
						myContext.addGis(gis.getId(),gis);
						myContext.registerEventListener(gis, EventType.onSave);
						myContext.registerEventListener(gis, EventType.onFlowExecuted);
						myContext.addDrawable(name,gis);
						//listen for updates from the server.
						gs.registerUpdateListener(gis);

						avstRL.setVisibility(View.INVISIBLE);


						for (GisLayer gl:mapLayers) {
							gis.addLayer(gl);
						}


						cb.continueExecution("gis");
					} else {
						Log.e("vortex","Failed to create map image. Will exit");
						cb.abortExecution("Failed to create Gis page. The map image ["+cachedImgFilePath+"] could not be found" );
					}
				}
			}, 0);

		} else {
			o.addRow("");
			if (photoMetaData ==null) {
				Log.e("vortex","Photemetadata null! Cannot add GisImageView!");
				o.addRedText("Adding GisImageView to "+containerId+" failed. Photometadata missing (the boundaries of the image on the map)");
				cb.abortExecution("Adding GisImageView to "+containerId+" failed. Photometadata missing (the boundaries of the image on the map)");
			}
			else {
				Log.e("vortex","Container null! Cannot add GisImageView!");
				o.addRedText("Adding GisImageView to "+containerId+" failed. Container cannot be found in template");
				cb.abortExecution("Missing container for GisImageView: "+containerId);
			}
		}


	}


	private void loadImageMetaData(String picName,String serverFileRootDir,String cacheFolder) {

		if (N==null||E==null||S==null||W==null||N.length()==0) {
			//Parse and cache the metafile.
			loadImageMetaFromFile(picName, serverFileRootDir,cacheFolder);
		}
		else {
			Log.e("vortex","Found tags for photo meta");
			createAfterLoad(new PhotoMeta(N,E,S,W),cacheFolder,picName);
		}
	}

	//User parser to parse and cache xml.

	private void loadImageMetaFromFile(final String fileName,String serverFileRootDir,final String cacheFolder) {

		//cut the .jpg type ending.
		String[]tmp = fileName.split("\\.");

		if (tmp!=null && tmp.length!=0) {
			final String metaFileName = tmp[0];
			Log.d("vortex","metafilename: "+metaFileName);
			Log.d("jgw","imgmetaformatisfile: "+gs.getImgMetaFormat());
			final ConfigurationModule  meta;
			String imgFormat = gs.getImgMetaFormat();
			switch (imgFormat) {
				default:
					/*falls through*/
				case "xml":
					meta = new AirPhotoMetaDataXML(gs.getContext(),gs.getGlobalPreferences(),
							gs.getPreferences(),
							serverFileRootDir,metaFileName,"");
					break;
				case "ini":
					meta = new AirPhotoMetaDataIni(gs.getContext(),gs.getGlobalPreferences(),
							gs.getPreferences(),
							serverFileRootDir,metaFileName,"");
					break;
				case "jgw":
					meta = new AirPhotoMetaDataJgw(gs.getContext(),gs.getGlobalPreferences(),
							GlobalState.getInstance().getPreferences(),
							serverFileRootDir,metaFileName,"");
					break;

			}

			// Check if the metadata is already cached and loaded
			if (meta.thawSynchronously().errCode == ErrorCode.thawed) {
				Log.d("vortex","Found frozen metadata. Will use it");
				PhotoMeta pm = ((PhotoMetaI)meta).getPhotoMeta();
				// It's already loaded, so we can call createAfterLoad directly.
				// This part of the logic is likely on the main thread already.
				createAfterLoad(pm, cacheFolder, fileName);
			} else {
				Log.d("vortex","No frozen metadata. Delegating download to ViewModel.");

				// DELEGATE the background work to the ViewModel
				// The ViewModel returns a LiveData object that we can observe.
				ModuleLoaderViewModel viewModel = new ViewModelProvider(myContext.getFragmentActivity()).get(ModuleLoaderViewModel.class);

				viewModel.loadPhotoMetadata(meta, cacheFolder, fileName)
						.observe(myContext.getFragmentActivity(), gisResult -> {
							// THIS BLOCK IS GUARANTEED TO RUN ON THE MAIN THREAD

							if (gisResult != null && gisResult.isSuccess()) {
								// The background task was successful. Now we can safely update the UI.
								Log.d("vortex","ViewModel finished loading. Calling createAfterLoad on main thread.");
								createAfterLoad(gisResult.photoMeta, gisResult.cacheFolder, gisResult.fileName);
							} else {
								// The background task failed.
								String errorMessage = "Could not load GIS image meta file [" + metaFileName + "." + gs.getImgMetaFormat() + "].";
								if (gisResult != null && gisResult.error != null) {
									errorMessage = gisResult.error.getMessage();
								}
								o.addRow("");
								o.addRedText(errorMessage);
								Log.e("vortex", "Failed to parse image meta. " + errorMessage);
								cb.abortExecution(errorMessage);
							}
						});
			}
		}
	}


	private class Cutout {
		Rect r;
		List<Location> geoR;
	}


	//Reloads current flow with a new viewport.
	public void setCutOut(Rect r, List<Location> geoR, List<GisLayer> myLayers) {
		cutOut = new Cutout();
		cutOut.r = r;
		cutOut.geoR = geoR;
		this.myLayers = myLayers;
	}

public static class GisResult {
	public final PhotoMeta photoMeta;
	public final String cacheFolder;
	public final String fileName;
	public final Exception error; // Will be null on success

	// Constructor for success
	public GisResult(PhotoMeta photoMeta, String cacheFolder, String fileName) {
		this.photoMeta = photoMeta;
		this.cacheFolder = cacheFolder;
		this.fileName = fileName;
		this.error = null;
	}

	// Constructor for failure
	public GisResult(Exception error) {
		this.photoMeta = null;
		this.cacheFolder = null;
		this.fileName = null;
		this.error = error;
	}

	public boolean isSuccess() {
		return error == null;
	}
}

}


