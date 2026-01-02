package com.teraim.fieldapp.dynamic.templates

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.*
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.any
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.color
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.concat
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.eq
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.geometryType
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.get
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.literal
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.match
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.teraim.fieldapp.BuildConfig
import com.teraim.fieldapp.R
import com.teraim.fieldapp.dynamic.Executor
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs



class GoogleGisTemplate : Executor() {

    // --- Constants ---
    private val TAG = "GoogleGisTemplate"
    private val GEOJSON_CONTENT_LIST_URL = "http://52.19.13.203/vortex/gis_objects/content.txt"
    private val GEOJSON_BASE_URL = "http://52.19.13.203/vortex/gis_objects/gis_objects_wgs84/"
    private val INITIAL_ZOOM = 8.0
    private val TRAKT_ZOOM_LEVEL = 11.0
    private val CIRCLE_ICON_ID = "circle-icon"
    private val SQUARE_ICON_ID = "square-icon"
    private val TRAKTER_LAYER_NAME = "trakter"

    // --- Color Palette for Layers ---
    private val layerColors = listOf(
        Color.parseColor("#F0F8FF"), // AliceBlue
        Color.parseColor("#FAEBD7"), // AntiqueWhite
        Color.parseColor("#AFEEEE"), // PaleTurquoise
        Color.parseColor("#FFE4C4"), // Bisque
        Color.parseColor("#E6E6FA"), // Lavender
        Color.parseColor("#FFF0F5"), // LavenderBlush
        Color.parseColor("#FFFACD"), // LemonChiffon
        Color.parseColor("#ADD8E6"), // LightBlue
        Color.parseColor("#F08080"), // LightCoral
        Color.parseColor("#E0FFFF"), // LightCyan
        Color.parseColor("#FAFAD2"), // LightGoldenrodYellow
        Color.parseColor("#90EE90"), // LightGreen
        Color.parseColor("#FFB6C1"), // LightPink
        Color.parseColor("#FFA07A")  // LightSalmon
    )

    // --- UI and Map Components ---
    private var mapView: MapView? = null
    private lateinit var mapboxMap: MapboxMap
    private lateinit var rootContainer: ViewGroup
    private lateinit var layerToggleFab: FloatingActionButton

    // --- State Management & Listeners ---
    private lateinit var networkExecutor: ExecutorService
    private lateinit var mainHandler: Handler
    private val layerVisibility = ConcurrentHashMap<String, Boolean>()
    private val allLayerIds = ConcurrentHashMap.newKeySet<String>()


    override fun getContainers(): MutableList<WF_Container> {
        val ret = ArrayList<WF_Container>()
        if (::rootContainer.isInitialized) {
            ret.add(WF_Container("root", rootContainer, null))
        }
        return ret
    }

    override fun execute(function: String?, target: String?): Boolean {
        Log.d(TAG, "Execute called with function: $function, target: $target")
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called.")

        networkExecutor = Executors.newFixedThreadPool(5)
        mainHandler = Handler(Looper.getMainLooper())

        if (wf != null) {
            Log.d(TAG, "Workflow exists, calling run().")
            run()
        } else {
            Log.d(TAG, "Workflow is null in onCreate.")
        }
        val token = BuildConfig.MAPBOX_ACCESS_TOKEN
        MapboxOptions.accessToken = token
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootContainer = view.findViewById(R.id.myRoot)
        mapView = view.findViewById(R.id.mapView)
        layerToggleFab = view.findViewById(R.id.fab_layer_toggle)

        layerToggleFab.visibility = View.GONE
        layerToggleFab.setOnClickListener { showLayerSelectionDialog() }

        mapView?.let {
            mapboxMap = it.getMapboxMap()
            mapboxMap.loadStyleUri(Style.SATELLITE_STREETS) { style -> onMapReady(style) }
        }
    }

    private fun onMapReady(style: Style) {
        Log.d(TAG, "Mapbox Style loaded. Initializing map components.")
        addImagesToStyle(style)
        setInitialCameraPosition()
        setupMapClickListener()
        loadGeoJsonLayers()
    }

    private fun addImagesToStyle(style: Style) {
        val circleBitmap = drawableToBitmap(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_circle_symbol))
        val squareBitmap = drawableToBitmap(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_square_symbol))
        if (circleBitmap != null && squareBitmap != null) {
            style.addImage(CIRCLE_ICON_ID, circleBitmap, true)
            style.addImage(SQUARE_ICON_ID, squareBitmap, true)
            Log.d(TAG, "Added custom icons to style with SDF enabled")
        } else {
            Log.e(TAG, "Could not create bitmaps for icons")
        }
    }

    private fun setInitialCameraPosition() {
        val initialPoint: Point = Point.fromLngLat(18.0686, 59.3293) // Default: Stockholm
        mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(initialPoint)
                .zoom(INITIAL_ZOOM)
                .build()
        )
    }

    private fun setupMapClickListener() {
        mapboxMap.addOnMapClickListener { point ->
            val screenCoordinate = mapboxMap.pixelForCoordinate(point)
            val queryGeometry = RenderedQueryGeometry(screenCoordinate)
            val queryOptions = RenderedQueryOptions(allLayerIds.toList(), null)

            mapboxMap.queryRenderedFeatures(queryGeometry, queryOptions) { result ->
                result.value?.firstOrNull()?.let { feature ->
                    val sourceId = feature.queriedFeature.source
                    val properties = feature.queriedFeature.feature.properties()
                    val geometry = feature.queriedFeature.feature.geometry()

                    if (sourceId != null && properties != null && geometry != null) {
                        if (sourceId.contains(TRAKTER_LAYER_NAME)) {
                            // If a trakter square is clicked, zoom to it and show all layers.
                            val padding = EdgeInsets(100.0, 100.0, 100.0, 100.0)
                            val cameraOptions = mapboxMap.cameraForGeometry(geometry, padding)
                            mapView?.camera?.easeTo(cameraOptions)
                            showAllLayers()
                        } else {
                            // For any other object, show the properties dialog.
                            showFeaturePropertiesDialog(properties)
                        }
                    }
                }
            }
            true
        }
    }

    private fun showAllLayers() {
        layerVisibility.keys.forEach { layerName ->
            if (layerVisibility[layerName] == false) {
                layerVisibility[layerName] = true
                toggleLayerVisibility(layerName, true)
            }
        }
    }

    private fun loadGeoJsonLayers() {
        Log.d(TAG, "Starting background task to fetch filename list from: $GEOJSON_CONTENT_LIST_URL")
        networkExecutor.execute {
            try {
                val url = URL(GEOJSON_CONTENT_LIST_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                        val geoJsonFileNames = reader.readLines()
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }

                        if (geoJsonFileNames.isNotEmpty()) {
                            Log.d(TAG, "Found ${geoJsonFileNames.size} GeoJSON layers to load: $geoJsonFileNames")
                            mainHandler.post { layerToggleFab.visibility = View.VISIBLE }
                            geoJsonFileNames.forEach { fileName ->
                                layerVisibility[fileName] = (fileName == TRAKTER_LAYER_NAME)
                                val geoJsonUrl = "$GEOJSON_BASE_URL$fileName.json"
                                downloadAndAddGeoJsonLayer(geoJsonUrl, fileName)
                            }
                        } else {
                            Log.w(TAG, "Content list was empty. No layers to load.")
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to fetch filename list. HTTP Response: ${conn.responseCode}")
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching or processing filename list.", e)
            }
        }
    }

    private fun downloadAndAddGeoJsonLayer(geoJsonUrl: String, layerName: String) {
        networkExecutor.execute {
            try {
                val url = URL(geoJsonUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                var geoJsonData: String? = null

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader(InputStreamReader(conn.inputStream)).use {
                        geoJsonData = it.readText()
                    }
                    Log.d(TAG, "Successfully downloaded $layerName.json.")
                } else {
                    Log.e(TAG, "Failed to download $layerName.json. HTTP Response: ${conn.responseCode}")
                }
                conn.disconnect()

                geoJsonData?.let { data ->
                    if (layerName.equals(TRAKTER_LAYER_NAME, ignoreCase = true)) {
                        val processedGeoJson = transformTrakterPointsToPolygons(data)
                        mainHandler.post { addLayerToMap(processedGeoJson, layerName) }
                    } else {
                        mainHandler.post { addLayerToMap(data, layerName) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading GeoJSON from $geoJsonUrl", e)
            }
        }
    }

    private fun addLayerToMap(geoJsonData: String, layerName: String) {
        mapboxMap.getStyle { style ->
            try {
                val featureCollection = FeatureCollection.fromJson(geoJsonData)
                if (featureCollection.features().isNullOrEmpty()) {
                    Log.w(TAG, "GeoJSON file $layerName contains no features. Skipping.")
                    return@getStyle
                }

                val layerColor = if (layerName.equals(TRAKTER_LAYER_NAME, ignoreCase = true)) {
                    Color.WHITE
                } else {
                    val index = abs(layerName.hashCode()) % layerColors.size
                    layerColors[index]
                }

                val sourceId = "source-$layerName"
                val fillLayerId = "fill-$layerName"
                val symbolLayerId = "symbol-$layerName"
                val outlineLayerId = "outline-$layerName"

                allLayerIds.add(fillLayerId)
                allLayerIds.add(symbolLayerId)
                allLayerIds.add(outlineLayerId)

                if (style.styleSourceExists(sourceId)) style.removeStyleSource(sourceId)
                if (style.styleLayerExists(fillLayerId)) style.removeStyleLayer(fillLayerId)
                if (style.styleLayerExists(outlineLayerId)) style.removeStyleLayer(outlineLayerId)
                if (style.styleLayerExists(symbolLayerId)) style.removeStyleLayer(symbolLayerId)

                style.addSource(
                    geoJsonSource(sourceId) {
                        featureCollection(featureCollection)
                    }
                )

                val initialVisibility = if (layerVisibility[layerName] == true) Visibility.VISIBLE else Visibility.NONE

                style.addLayer(
                    fillLayer(fillLayerId, sourceId) {
                        filter(
                            any(
                                eq(geometryType(), literal("Polygon")),
                                eq(geometryType(), literal("MultiPolygon"))
                            )
                        )
                        fillColor(layerColor)
                        if (layerName.equals(TRAKTER_LAYER_NAME, ignoreCase = true)) {
                            fillOpacity(0.0)
                        } else {
                            fillOpacity(0.4)
                        }
                        visibility(initialVisibility)
                    }
                )

                style.addLayer(
                    lineLayer(outlineLayerId, sourceId) {
                        filter(
                            any(
                                eq(geometryType(), literal("Polygon")),
                                eq(geometryType(), literal("MultiPolygon"))
                            )
                        )
                        lineColor(Color.WHITE)
                        if (layerName.equals(TRAKTER_LAYER_NAME, ignoreCase = true)) {
                            lineWidth(3.0)
                        } else {
                            lineWidth(1.5)
                            lineDasharray(listOf(0.5, 2.0))
                        }
                        visibility(initialVisibility)
                    }
                )

                style.addLayer(
                    symbolLayer(symbolLayerId, sourceId) {
                        filter(
                            any(
                                eq(geometryType(), literal("Point")),
                                eq(geometryType(), literal("MultiPoint"))
                            )
                        )
                        iconImage(
                            match(get("PROVYTETYP"),
                                literal("REKTANGEL"), literal(SQUARE_ICON_ID),
                                literal(CIRCLE_ICON_ID)
                            )
                        )
                        iconColor(
                            match(get("GISTYP"),
                                literal("akerkant"), color(Color.YELLOW),
                                literal("basiskberghall"), color(Color.CYAN),
                                literal("hallmarkstorr"), color(Color.GREEN),
                                color(layerColor)
                            )
                        )
                        textField(
                            concat(
                                get("TYPKOD"),
                                literal(" - "),
                                get("OBJECTID")
                            )
                        )
                        textColor(Color.WHITE)
                        textHaloColor(Color.BLACK)
                        textHaloWidth(1.0)
                        textAnchor(TextAnchor.TOP)
                        textOffset(listOf(0.0, 1.5))
                        iconAllowOverlap(true)
                        textAllowOverlap(true)
                        iconIgnorePlacement(true)
                        textIgnorePlacement(true)
                        visibility(initialVisibility)
                    }
                )

                Log.d(TAG, "Added GeoJSON source and layers for: $layerName")
                zoomToFeatureCollection(featureCollection)

            } catch (e: Exception) {
                Log.e(TAG, "Error parsing or adding GeoJSON layer: $layerName", e)
            }
        }
    }

    private fun transformTrakterPointsToPolygons(geoJsonData: String): String {
        val originalFeatureCollection = FeatureCollection.fromJson(geoJsonData)
        val newFeatures = mutableListOf<Feature>()

        originalFeatureCollection.features()?.forEach { feature ->
            val geometry = feature.geometry()
            if (geometry is Point) {
                // Create a 10km square polygon from the point.
                val polygon = createSquarePolygon(geometry, 10000.0)
                val newFeature = Feature.fromGeometry(polygon, feature.properties())
                newFeatures.add(newFeature)
            }
        }

        val newFeatureCollection = FeatureCollection.fromFeatures(newFeatures)
        return newFeatureCollection.toJson()
    }

    private fun createSquarePolygon(center: Point, sideLengthMeters: Double): Polygon {
        val halfSide = sideLengthMeters / 2.0
        val lon = center.longitude()
        val lat = center.latitude()

        val earthRadius = 6378137.0

        val latOffset = (halfSide / earthRadius) * (180 / Math.PI)
        val lonOffset = (halfSide / (earthRadius * Math.cos(Math.PI * lat / 180))) * (180 / Math.PI)

        val north = lat + latOffset
        val south = lat - latOffset
        val east = lon + lonOffset
        val west = lon - lonOffset

        val points = listOf(
            Point.fromLngLat(west, north),
            Point.fromLngLat(east, north),
            Point.fromLngLat(east, south),
            Point.fromLngLat(west, south),
            Point.fromLngLat(west, north)
        )

        return Polygon.fromLngLats(listOf(points))
    }



    private fun zoomToFeatureCollection(featureCollection: FeatureCollection) {
        val geometries = featureCollection.features()?.mapNotNull { it.geometry() }
        if (geometries.isNullOrEmpty()) {
            Log.w(TAG, "No geometries found in FeatureCollection to zoom to.")
            return
        }
        val geometryCollection = GeometryCollection.fromGeometries(geometries)
        val padding = EdgeInsets(100.0, 100.0, 100.0, 100.0)
        val cameraOptions = mapboxMap.cameraForGeometry(geometryCollection, padding)
        mapView?.camera?.easeTo(cameraOptions)
        Log.d(TAG, "Zoomed camera to bounds of the new layer.")
    }

    private fun showLayerSelectionDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_layer_selection, null)
        val checkboxContainer = dialogView.findViewById<LinearLayout>(R.id.layer_checkbox_container)

        if (layerVisibility.isEmpty()) {
            Log.w(TAG, "Layer selection dialog opened, but no layers have been loaded.")
            return
        }

        layerVisibility.keys.sorted().forEach { layerName ->
            val checkBox = CheckBox(context).apply {
                text = layerName
                isChecked = layerVisibility[layerName] ?: true
                setOnCheckedChangeListener { _, isChecked ->
                    layerVisibility[layerName] = isChecked
                    toggleLayerVisibility(layerName, isChecked)
                }
            }
            checkboxContainer.addView(checkBox)
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun showFeaturePropertiesDialog(properties: JsonObject) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val formattedJson = gson.toJson(properties)

        AlertDialog.Builder(requireContext())
            .setTitle("Feature Properties")
            .setMessage(formattedJson)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun toggleLayerVisibility(layerName: String, isVisible: Boolean) {
        mapboxMap.getStyle { style ->
            val visibility = if (isVisible) Visibility.VISIBLE else Visibility.NONE
            val fillLayerId = "fill-$layerName"
            val symbolLayerId = "symbol-$layerName"
            val outlineLayerId = "outline-$layerName"

            style.getLayer(fillLayerId)?.let {
                it.visibility(visibility)
                Log.d(TAG, "Set visibility of $fillLayerId to $visibility")
            }
            style.getLayer(outlineLayerId)?.let {
                it.visibility(visibility)
                Log.d(TAG, "Set visibility of $outlineLayerId to $visibility")
            }
            style.getLayer(symbolLayerId)?.let {
                it.visibility(visibility)
                Log.d(TAG, "Set visibility of $symbolLayerId to $visibility")
            }
        }
    }

    private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
        if (drawable == null) return null
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 50
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 50

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    override fun onDestroyView() {
        super.onDestroyView()
        networkExecutor.shutdownNow()
        mapView?.onDestroy()
        mapView = null
        Log.d(TAG, "onDestroyView called, MapView destroyed and Executor shutdown.")
    }
}