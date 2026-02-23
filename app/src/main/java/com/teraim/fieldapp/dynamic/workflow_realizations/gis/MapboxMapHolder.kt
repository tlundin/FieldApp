package com.teraim.fieldapp.dynamic.workflow_realizations.gis

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import com.mapbox.geojson.MultiPolygon
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.any
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.color
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.concat
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.eq
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.geometryType
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.get
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.literal
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.match
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.step
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.zoom
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import android.widget.ImageButton
import com.teraim.fieldapp.GlobalState
import com.teraim.fieldapp.R
import com.teraim.fieldapp.dynamic.types.DB_Context
import com.teraim.fieldapp.dynamic.types.Workflow
import com.teraim.fieldapp.dynamic.workflow_abstracts.Drawable
import com.teraim.fieldapp.non_generics.Constants
import com.teraim.fieldapp.utils.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.HashMap
import kotlin.math.abs

/**
 * Mapbox-backed map drawable. Registered as the "map" when using MapTemplate with network.
 * Layers are loaded from server: gis_objects/wgs/<type>.json
 * Only layer types listed in gis_objects/content.txt are loaded.
 */
class MapboxMapHolder(
    private val mapView: View,
    private val gisObjectsBaseUrl: String
) : Drawable {

    companion object {
        private const val TAG = "MapboxMapHolder"
        private const val CONTENT_FILE = "content.txt"
        /** Zoom level at which object labels become visible (e.g. ~1/3 of Sweden visible at zoom 6). */
        private const val LABEL_VISIBLE_ZOOM_LEVEL = 6.0
        /** Zoom level for ~20 km visible horizontally/vertically (centering on trakt). */
        private const val CENTER_ON_ZOOM_LEVEL = 10.5
        private const val TEAM_SOURCE_ID = "source-team"
        private const val TEAM_LAYER_ID = "layer-team"
        /** Display name for layer list (FAB layer dialog). */
        const val TEAM_LAYER_DISPLAY_NAME = "Team"
        /** Point shape: use circle layer. Other shapes use symbol layer with icon. */
        private const val ICON_ID_SQUARE = "mapbox-point-square"
        private const val ICON_ID_TRIANGLE = "mapbox-point-triangle"
        private const val ICON_ID_NEEDLE = "mapbox-point-needle"
        private val LAYER_COLORS = intArrayOf(
            Color.parseColor("#F0F8FF"),
            Color.parseColor("#FAEBD7"),
            Color.parseColor("#AFEEEE"),
            Color.parseColor("#FFE4C4"),
            Color.parseColor("#E6E6FA"),
            Color.parseColor("#FFF0F5"),
            Color.parseColor("#FFFACD"),
            Color.parseColor("#ADD8E6"),
            Color.parseColor("#F08080"),
            Color.parseColor("#E0FFFF"),
            Color.parseColor("#FAFAD2"),
            Color.parseColor("#90EE90"),
            Color.parseColor("#FFB6C1"),
            Color.parseColor("#FFA07A")
        )
    }

    @Volatile
    var mapboxMap: MapboxMap? = null
        set(value) {
            field = value
            if (value != null) {
                processPendingLayers()
                setupMapClickListener()
            }
        }

    /** Workflow to run when "center on" is pressed (e.g. wf_Karta_Provytor). Null if not configured. */
    var onCenterClickWorkflow: String? = null

    private val layerState = mutableMapOf<String, LayerState>()
    /** Stored layer specs for refresh (re-fetch GeoJSON from server). */
    private val layerSpecs = mutableMapOf<String, PendingLayer>()
    private val pendingLayers = mutableListOf<PendingLayer>()
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var visible = true
    /** Set of GeoJSON type names from gis_objects/content.txt; null if not yet loaded or fetch failed. */
    @Volatile
    private var allowedLayerTypes: Set<String>? = null

    data class LayerState(
        val sourceId: String,
        val fillLayerId: String,
        val outlineLayerId: String,
        val pointLayerId: String,
        val labelLayerId: String?,
        var visible: Boolean
    )

    private data class PendingLayer(
        val name: String,
        val label: String,
        val isVisible: Boolean,
        val hasWidget: Boolean,
        val showLabels: Boolean,
        val isBold: Boolean,
        val fillColor: String?,
        val fillOpacity: Float?,
        val lineColor: String?,
        val lineWidth: Float?,
        val circleRadius: Float?,
        val polyType: String?
    )

    override fun getWidget(): View = mapView
    override fun show() { visible = true; mapView.visibility = View.VISIBLE }
    override fun hide() { visible = false; mapView.visibility = View.GONE }
    override fun isVisible(): Boolean = visible

    fun addLayer(
        name: String,
        label: String,
        isVisible: Boolean,
        hasWidget: Boolean,
        showLabels: Boolean,
        isBold: Boolean,
        fillColor: String? = null,
        fillOpacity: Float? = null,
        lineColor: String? = null,
        lineWidth: Float? = null,
        circleRadius: Float? = null,
        polyType: String? = null
    ) {
        if (layerState.containsKey(name)) {
            Log.d(TAG, "Layer $name already added")
            return
        }
        val pending = PendingLayer(
            name, label, isVisible, hasWidget, showLabels, isBold,
            fillColor, fillOpacity, lineColor, lineWidth, circleRadius, polyType
        )
        val map = mapboxMap
        if (map == null) {
            synchronized(pendingLayers) { pendingLayers.add(pending) }
            Log.d(TAG, "Queued layer $name (map not ready)")
            return
        }
        scope.launch {
            if (allowedLayerTypes == null) allowedLayerTypes = fetchContentList()
            val type = layerNameToType(name)
            if (allowedLayerTypes != null && !allowedLayerTypes!!.contains(type)) {
                Log.d(TAG, "Skipping layer $name (type $type not in content.txt)")
                return@launch
            }
            addLayerInternal(map, pending)
        }
    }

    private fun processPendingLayers() {
        val map = mapboxMap ?: return
        val toProcess = synchronized(pendingLayers) {
            pendingLayers.toList().also { pendingLayers.clear() }
        }
        scope.launch {
            if (allowedLayerTypes == null) {
                allowedLayerTypes = fetchContentList()
                allowedLayerTypes?.let { Log.d(TAG, "Loaded ${it.size} layer types from content.txt: $it") }
                    ?: Log.w(TAG, "Could not load content.txt; will attempt all workflow layers")
            }
            val allowed = allowedLayerTypes
            for (spec in toProcess) {
                val type = layerNameToType(spec.name)
                if (allowed != null && !allowed.contains(type)) {
                    Log.d(TAG, "Skipping layer ${spec.name} (type $type not in content.txt)")
                    continue
                }
                addLayerInternal(map, spec)
            }
        }
    }

    /** Layer name to GeoJSON type: e.g. akerkant_layer -> akerkant */
    private fun layerNameToType(layerName: String): String =
        if (layerName.endsWith("_layer")) layerName.removeSuffix("_layer") else layerName

    /** Fetch gis_objects/content.txt and return set of type names (one per line). */
    private suspend fun fetchContentList(): Set<String>? = withContext(Dispatchers.IO) {
        val contentUrl = gisObjectsBaseUrl.removeSuffix("wgs/") + CONTENT_FILE
        try {
            val url = URL(contentUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            try {
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    conn.inputStream.bufferedReader().use { reader ->
                        reader.readLines()
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .toSet()
                    }
                } else {
                    Log.w(TAG, "content.txt HTTP ${conn.responseCode} for $contentUrl")
                    null
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching content.txt from $contentUrl", e)
            null
        }
    }

    private suspend fun addLayerInternal(map: MapboxMap, spec: PendingLayer) {
        val geoJson = fetchGeoJson(spec.name)
        if (geoJson == null) {
            Log.e(TAG, "Failed to load GeoJSON for layer ${spec.name}")
            return
        }
        withContext(Dispatchers.Main) {
            map.getStyle { style ->
                addLayerToStyle(style, spec, geoJson)
            }
        }
    }

    /**
     * Fetch GeoJSON for a layer. Server files are always <type>.json; workflow layer names
     * may have a "_layer" suffix (e.g. akerkant_layer → akerkant.json).
     */
    private suspend fun fetchGeoJson(layerName: String): String? = withContext(Dispatchers.IO) {
        val type = if (layerName.endsWith("_layer")) layerName.removeSuffix("_layer") else layerName
        fetchGeoJsonFromUrl("$gisObjectsBaseUrl$type.json")
    }

    private suspend fun fetchGeoJsonFromUrl(urlString: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            try {
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    Log.w(TAG, "HTTP ${conn.responseCode} for $urlString")
                    null
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching $urlString", e)
            null
        }
    }

    private fun parseColorOrNull(hex: String?): Int? {
        if (hex == null || hex.isBlank()) return null
        return try {
            Color.parseColor(hex.trim().let { if (it.startsWith("#")) it else "#$it" })
        } catch (e: Exception) {
            Log.w(TAG, "Invalid color '$hex', using default")
            null
        }
    }

    /**
     * Expression: color by PYSTATUS (0 or missing = default, 2 = red, 3 = yellow, 4 = green, 100 = cyan).
     */
    private fun pystatusColorExpression(defaultColorInt: Int): Expression {
        return match(
            get("PYSTATUS"),
            literal(2), color(Color.RED),
            literal(3), color(Color.YELLOW),
            literal(4), color(Color.GREEN),
            literal(100), color(Color.CYAN),
            color(defaultColorInt)
        )
    }

    /** TRAKTSTATUS color mapping for trakter layer: -1 purple, 0 white, 1-30 orange, 31-70 lime, 71-99 green, 100 cyan. */
    private val TRAKTSTATUS_COLORS = mapOf(
        -1 to Color.parseColor("#9900ff"),
        0 to Color.parseColor("#ffffff"),
        1 to Color.parseColor("#ff9600"),
        31 to Color.parseColor("#a6fc00"),
        71 to Color.parseColor("#008500"),
        100 to Color.parseColor("#00FFFF")
    )

    /**
     * Expression: color by TRAKTSTATUS for trakter layer.
     * -1: #9900ff, 0: #ffffff, 1-30: #ff9600, 31-70: #a6fc00, 71-99: #008500, 100: #00FFFF
     */
    private fun trakterStatusColorExpression(): Expression {
        return step(
            get("TRAKTSTATUS"),
            color(TRAKTSTATUS_COLORS[0]!!),
            literal(-1.0) to color(TRAKTSTATUS_COLORS[-1]!!),
            literal(0.0) to color(TRAKTSTATUS_COLORS[0]!!),
            literal(1.0) to color(TRAKTSTATUS_COLORS[1]!!),
            literal(31.0) to color(TRAKTSTATUS_COLORS[31]!!),
            literal(71.0) to color(TRAKTSTATUS_COLORS[71]!!),
            literal(100.0) to color(TRAKTSTATUS_COLORS[100]!!)
        )
    }

    /** Returns status color expression for the layer; trakter uses TRAKTSTATUS, others use PYSTATUS. */
    private fun statusColorExpression(layerType: String, defaultColorInt: Int): Expression =
        if (layerType == "trakter") trakterStatusColorExpression()
        else pystatusColorExpression(defaultColorInt)

    /** Returns label field expression: TRAKT only for trakter, TYPKOD + OBJECTID for others. */
    private fun labelFieldExpression(layerType: String): Expression =
        if (layerType == "trakter") concat(get("TRAKT"), literal(""))
        else concat(get("TYPKOD"), literal(" "), get("OBJECTID"))

    /**
     * Normalize poly_type from XML to a point shape. Returns icon id for symbol layer, or null for circle layer.
     */
    private fun pointShapeToIconId(polyType: String?): String? {
        if (polyType.isNullOrBlank()) return null
        return when (polyType.trim().lowercase()) {
            "circle" -> null
            "rect", "square", "rectangle" -> ICON_ID_SQUARE
            "triangle" -> ICON_ID_TRIANGLE
            "needle" -> ICON_ID_NEEDLE
            else -> null
        }
    }

    private fun ensurePointIconInStyle(style: Style, iconId: String) {
        if (style.hasStyleImage(iconId)) return
        val ctx = mapView.context
        val drawable: android.graphics.drawable.Drawable? = when (iconId) {
            ICON_ID_SQUARE -> ContextCompat.getDrawable(ctx, R.drawable.ic_square_symbol)
            ICON_ID_TRIANGLE -> ContextCompat.getDrawable(ctx, R.drawable.ic_triangle_symbol)
            ICON_ID_NEEDLE -> ContextCompat.getDrawable(ctx, R.drawable.ic_needle_symbol)
            else -> null
        }
        val bitmap: Bitmap? = if (drawable != null) Tools.drawableToBitmap(drawable) else null
        if (bitmap != null) {
            style.addImage(iconId, bitmap, false)
            Log.d(TAG, "Added style image: $iconId")
        } else {
            Log.w(TAG, "Could not add style image for $iconId")
        }
    }

    private fun addLayerToStyle(style: Style, spec: PendingLayer, geoJson: String) {
        val name = spec.name
        val featureCollection = FeatureCollection.fromJson(geoJson)
        if (featureCollection.features().isNullOrEmpty()) {
            Log.w(TAG, "GeoJSON $name has no features")
            return
        }
        val defaultLayerColor = LAYER_COLORS[abs(name.hashCode()) % LAYER_COLORS.size]
        val fillColorInt = parseColorOrNull(spec.fillColor) ?: defaultLayerColor
        val lineColorInt = parseColorOrNull(spec.lineColor) ?: Color.WHITE
        val fillOpacityVal = spec.fillOpacity?.coerceIn(0f, 1f) ?: 0.4f
        // Mapbox lineWidth is in logical pixels (1 = 1 pixel). Use value directly.
        val lineWidthVal = (spec.lineWidth ?: 1f).coerceAtLeast(0f)
        val circleRadiusVal = (spec.circleRadius ?: 6f).coerceAtLeast(0f)

        val sourceId = "source-$name"
        val fillLayerId = "fill-$name"
        val outlineLayerId = "outline-$name"
        val pointLayerId = "point-$name"
        val labelLayerId = if (spec.showLabels) "label-$name" else null
        val visibility = if (spec.isVisible) Visibility.VISIBLE else Visibility.NONE

        if (style.styleSourceExists(sourceId)) style.removeStyleSource(sourceId)
        if (style.styleLayerExists(fillLayerId)) style.removeStyleLayer(fillLayerId)
        if (style.styleLayerExists(outlineLayerId)) style.removeStyleLayer(outlineLayerId)
        if (style.styleLayerExists(pointLayerId)) style.removeStyleLayer(pointLayerId)
        labelLayerId?.let { if (style.styleLayerExists(it)) style.removeStyleLayer(it) }

        style.addSource(
            geoJsonSource(sourceId) {
                featureCollection(featureCollection)
            }
        )
        // Add GIS layers below team layer when it exists so team needles stay on top (no re-apply needed)
        fun addLayerOrBelowTeam(layer: com.mapbox.maps.extension.style.layers.Layer) {
            if (style.styleLayerExists(TEAM_LAYER_ID)) style.addLayerBelow(layer, TEAM_LAYER_ID) else style.addLayer(layer)
        }
        val layerType = layerNameToType(name)
        val statusColorExpr = statusColorExpression(layerType, fillColorInt)
        addLayerOrBelowTeam(
            fillLayer(fillLayerId, sourceId) {
                filter(
                    any(
                        eq(geometryType(), literal("Polygon")),
                        eq(geometryType(), literal("MultiPolygon"))
                    )
                )
                fillColor(statusColorExpr)
                fillOpacity(fillOpacityVal.toDouble())
                visibility(visibility)
            }
        )
        addLayerOrBelowTeam(
            lineLayer(outlineLayerId, sourceId) {
                filter(
                    any(
                        eq(geometryType(), literal("Polygon")),
                        eq(geometryType(), literal("MultiPolygon"))
                    )
                )
                lineColor(lineColorInt)
                lineWidth(lineWidthVal.toDouble())
                visibility(visibility)
            }
        )
        val pointShapeIconId = pointShapeToIconId(spec.polyType)
        Log.d(TAG, "Layer $name: polyType='${spec.polyType}', pointShapeIconId=$pointShapeIconId")
        if (pointShapeIconId == null) {
            addLayerOrBelowTeam(
                circleLayer(pointLayerId, sourceId) {
                    filter(
                        any(
                            eq(geometryType(), literal("Point")),
                            eq(geometryType(), literal("MultiPoint"))
                        )
                    )
                    circleRadius(circleRadiusVal.toDouble())
                    circleColor(statusColorExpr)
                    circleStrokeColor(lineColorInt)
                    circleStrokeWidth(lineWidthVal.toDouble())
                    visibility(visibility)
                }
            )
        } else {
            ensurePointIconInStyle(style, pointShapeIconId)
            val iconSize = (circleRadiusVal / 6f).coerceIn(0.5f, 3f).toDouble()
            val useOutline = lineWidthVal > 0f
            addLayerOrBelowTeam(
                symbolLayer(pointLayerId, sourceId) {
                    filter(
                        any(
                            eq(geometryType(), literal("Point")),
                            eq(geometryType(), literal("MultiPoint"))
                        )
                    )
                    iconImage(pointShapeIconId)
                    iconSize(iconSize)
                    iconColor(statusColorExpr)
                    iconOpacity(fillOpacityVal.toDouble())
                    if (useOutline) {
                        iconHaloColor(lineColorInt)
                        iconHaloWidth(lineWidthVal.toDouble())
                    }
                    iconAllowOverlap(true)
                    visibility(visibility)
                    // Add text label directly to symbol layer if showLabels is true (visible when zoomed in)
                    if (spec.showLabels) {
                        textField(labelFieldExpression(layerType))
                        textColor(Color.BLACK)
                        textHaloColor(Color.WHITE)
                        textHaloWidth(1.0)
                        textSize(12.0)
                        textAnchor(TextAnchor.BOTTOM)
                        textOffset(listOf(0.0, 2.0)) // Positive Y moves text up above the point
                        textAllowOverlap(true)
                        textIgnorePlacement(false)
                        textOpacity(step(zoom(), literal(0.0), literal(LABEL_VISIBLE_ZOOM_LEVEL) to literal(1.0)))
                    }
                }
            )
        }

        // Add unified label layer for circles and polygons if showLabels is true
        // Note: Symbol-based points already have textField in their symbolLayer, so we only label circles and polygons here
        if (spec.showLabels && labelLayerId != null) {
            val labelFilter = if (pointShapeIconId == null) {
                // Include circles (points) and polygons
                any(
                    eq(geometryType(), literal("Point")),
                    eq(geometryType(), literal("MultiPoint")),
                    eq(geometryType(), literal("Polygon")),
                    eq(geometryType(), literal("MultiPolygon"))
                )
            } else {
                // Only include polygons (symbol-based points already have labels)
                any(
                    eq(geometryType(), literal("Polygon")),
                    eq(geometryType(), literal("MultiPolygon"))
                )
            }
            addLayerOrBelowTeam(
                symbolLayer(labelLayerId, sourceId) {
                    filter(labelFilter)
                    textField(labelFieldExpression(layerType))
                    textColor(Color.BLACK)
                    textHaloColor(Color.WHITE)
                    textHaloWidth(1.0)
                    textSize(12.0)
                    // Use BOTTOM anchor with positive offset for circles (above point), CENTER for polygons
                    textAnchor(if (pointShapeIconId == null) TextAnchor.BOTTOM else TextAnchor.CENTER)
                    if (pointShapeIconId == null) {
                        textOffset(listOf(0.0, 2.0)) // Positive Y moves text up above the circle
                    }
                    textAllowOverlap(true)
                    textIgnorePlacement(false)
                    visibility(visibility)
                    textOpacity(step(zoom(), literal(0.0), literal(LABEL_VISIBLE_ZOOM_LEVEL) to literal(1.0)))
                }
            )
        }

        layerState[name] = LayerState(sourceId, fillLayerId, outlineLayerId, pointLayerId, labelLayerId, spec.isVisible)
        layerSpecs[name] = spec
        Log.d(TAG, "Added Mapbox layer: $name (polyType=${spec.polyType}, fillColor=${spec.fillColor}, showLabels=${spec.showLabels})")
    }

    /**
     * Re-fetches all GeoJSON layers from the server and updates the map.
     * Call when server has updated GeoJSON files (e.g. after serverPendingUpdate is true).
     */
    fun refreshLayers(onComplete: (() -> Unit)? = null) {
        val map = mapboxMap ?: run {
            Log.w(TAG, "refreshLayers: map not ready")
            onComplete?.invoke()
            return
        }
        if (layerSpecs.isEmpty()) {
            Log.d(TAG, "refreshLayers: no layers to refresh")
            onComplete?.invoke()
            return
        }
        scope.launch {
            val toRemove = layerState.toMap()
            map.getStyle { style ->
                for ((_, state) in toRemove) {
                    if (style.styleSourceExists(state.sourceId)) style.removeStyleSource(state.sourceId)
                    if (style.styleLayerExists(state.fillLayerId)) style.removeStyleLayer(state.fillLayerId)
                    if (style.styleLayerExists(state.outlineLayerId)) style.removeStyleLayer(state.outlineLayerId)
                    if (style.styleLayerExists(state.pointLayerId)) style.removeStyleLayer(state.pointLayerId)
                    state.labelLayerId?.let { if (style.styleLayerExists(it)) style.removeStyleLayer(it) }
                }
                layerState.clear()
                allowedLayerTypes = null
                val specs = layerSpecs.values.toList()
                scope.launch {
                    for (spec in specs) {
                        addLayerInternal(map, spec)
                    }
                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "refreshLayers: re-added ${specs.size} layers")
                        onComplete?.invoke()
                    }
                }
            }
        }
    }

    fun setLayerVisibility(layerName: String, visible: Boolean) {
        if (layerName == TEAM_LAYER_DISPLAY_NAME) {
            teamLayerVisible = visible
            val visibility = if (visible) Visibility.VISIBLE else Visibility.NONE
            mapboxMap?.getStyle { style ->
                style.getLayer(TEAM_LAYER_ID)?.visibility(visibility)
            }
            return
        }
        val state = layerState[layerName] ?: return
        state.visible = visible
        val visibility = if (visible) Visibility.VISIBLE else Visibility.NONE
        mapboxMap?.getStyle { style ->
            style.getLayer(state.fillLayerId)?.visibility(visibility)
            style.getLayer(state.outlineLayerId)?.visibility(visibility)
            style.getLayer(state.pointLayerId)?.visibility(visibility)
            state.labelLayerId?.let { style.getLayer(it)?.visibility(visibility) }
        }
    }
    
    private fun setupMapClickListener() {
        val map = mapboxMap ?: return
        map.addOnMapClickListener { point ->
            val screenCoordinate = map.pixelForCoordinate(point)
            val queryGeometry = RenderedQueryGeometry(screenCoordinate)
            val allLayerIds = layerState.values.flatMap { state ->
                listOfNotNull(
                    state.fillLayerId,
                    state.outlineLayerId,
                    state.pointLayerId,
                    state.labelLayerId
                )
            }
            val queryOptions = RenderedQueryOptions(allLayerIds, null)
            
            map.queryRenderedFeatures(queryGeometry, queryOptions) { result ->
                result.value?.firstOrNull()?.let { feature ->
                    val mapFeature = feature.queriedFeature.feature
                    val properties = mapFeature.properties()
                    if (properties != null) {
                        showFeaturePropertiesDialog(mapFeature, properties)
                    }
                }
            }
            true
        }
    }
    
    /** Returns the center point (lat, lng) of a feature's geometry, or null if not computable. */
    private fun featureCenter(feature: Feature): Pair<Double, Double>? {
        val geom = feature.geometry() ?: return null
        return when (geom) {
            is Point -> geom.latitude() to geom.longitude()
            is Polygon -> {
                val outer = geom.coordinates().firstOrNull() ?: return null
                val n = outer.size
                if (n == 0) return null
                val lat = outer.map { it.latitude() }.average()
                val lng = outer.map { it.longitude() }.average()
                lat to lng
            }
            is MultiPolygon -> {
                val polys = geom.coordinates()
                val first = polys.firstOrNull()?.firstOrNull() ?: return null
                val lat = first.map { it.latitude() }.average()
                val lng = first.map { it.longitude() }.average()
                lat to lng
            }
            else -> null
        }
    }

    private fun safePropString(properties: JsonObject, key: String): String {
        val el = properties.get(key) ?: return "—"
        if (el.isJsonNull) return "—"
        return try {
            el.toString().trim('"').ifEmpty { "—" }
        } catch (_: Exception) { "—" }
    }

    private fun showFeaturePropertiesDialog(feature: Feature, properties: JsonObject) {
        val context = mapView.context ?: return
        val gistyp = properties.get("GISTYP")?.asString
        if (gistyp == "trakter") {
            showTrakterInfoDialog(context, feature, properties)
        } else {
            val gson = GsonBuilder().setPrettyPrinting().create()
            val formattedJson = gson.toJson(properties)
            AlertDialog.Builder(context)
                .setTitle("Feature Properties")
                .setMessage(formattedJson)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun traktStatusToLabelAndColor(value: Int?): Pair<String, Int> {
        val (labelRes, color) = when {
            value == null || value < -1 -> R.string.trakter_status_none to TRAKTSTATUS_COLORS[0]!!
            value == -1 -> R.string.trakter_status_high to TRAKTSTATUS_COLORS[-1]!!
            value == 0 -> R.string.trakter_status_none to TRAKTSTATUS_COLORS[0]!!
            value in 1..30 -> R.string.trakter_status_started to TRAKTSTATUS_COLORS[1]!!
            value in 31..70 -> R.string.trakter_status_partial to TRAKTSTATUS_COLORS[31]!!
            value in 71..99 -> R.string.trakter_status_much to TRAKTSTATUS_COLORS[71]!!
            value >= 100 -> R.string.trakter_status_complete to TRAKTSTATUS_COLORS[100]!!
            else -> R.string.trakter_status_none to TRAKTSTATUS_COLORS[0]!!
        }
        return mapView.context.getString(labelRes) to color
    }

    private fun showTrakterInfoDialog(context: android.content.Context, feature: Feature, properties: JsonObject) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_trakter_info, null)
        val trakt = safePropString(properties, "TRAKT")
        val traktStatusVal = try {
            properties.get("TRAKTSTATUS")?.takeIf { !it.isJsonNull }?.let { el ->
                if (el.isJsonPrimitive && el.asJsonPrimitive.isNumber) el.asInt else null
            }
        } catch (_: Exception) { null }
        val (traktStatusLabel, traktStatusColor) = traktStatusToLabelAndColor(traktStatusVal)
        val objectId = safePropString(properties, "OBJECTID")
        val typkod = properties.get("TYPKOD")?.asString ?: "—"
        val column1 = safePropString(properties, "COLUMN1")
        val titleView = view.findViewById<android.widget.TextView>(R.id.trakter_title)
        val infoView = view.findViewById<android.widget.TextView>(R.id.trakter_info)
        val statusIndicator = view.findViewById<View>(R.id.trakter_status_indicator)
        titleView.text = context.getString(R.string.trakter_info_title, trakt)
        val indicatorDrawable = android.graphics.drawable.GradientDrawable().apply {
            setColor(traktStatusColor)
            setStroke(1, Color.GRAY)
            cornerRadius = 2 * context.resources.displayMetrics.density
        }
        statusIndicator.background = indicatorDrawable
        infoView.text = buildString {
            append("TRAKT: "); appendLine(trakt)
            append("TRAKTSTATUS: "); appendLine(traktStatusLabel)
            append("TYPKOD: "); appendLine(typkod)
            append("OBJECTID: "); appendLine(objectId)
            append("COLUMN1: "); append(column1)
        }
        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .create()
        view.findViewById<ImageButton>(R.id.btn_center_on).setOnClickListener {
            android.util.Log.i(TAG, "Center-on button clicked")
            try {
            val center = featureCenter(feature)
            android.util.Log.i(TAG, "Center-on: featureCenter=$center")
            if (center != null) {
                val (lat, lng) = center
                val point = Point.fromLngLat(lng, lat)
                val cameraOptions = CameraOptions.Builder()
                    .center(point)
                    .zoom(CENTER_ON_ZOOM_LEVEL)
                    .build()
                (mapView as? MapView)?.camera?.easeTo(cameraOptions)
                dialog.dismiss()
                // 2) Set DB context with gistyp=Trakter, trakt=<TRAKT>
                val keyHash = HashMap<String, String>()
                keyHash["gistyp"] = "Trakter"
                keyHash["trakt"] = trakt
                keyHash["år"] = Constants.getYear()
                GlobalState.getInstance().setDBContext(DB_Context(null, keyHash))
                // 4) Transfer center to target map
                GlobalState.getInstance().setPendingMapCenter(lat, lng)
                // 3) Execute workflow from on_click - post to next frame so dialog dismiss completes
                val wfName = onCenterClickWorkflow
                android.util.Log.i(TAG, "Center-on: onCenterClickWorkflow=$wfName")
                if (!wfName.isNullOrBlank()) {
                    val wf = GlobalState.getInstance().getWorkflow(wfName)
                    android.util.Log.i(TAG, "Center-on: workflow lookup result=${if (wf != null) "found" else "null"}")
                    if (wf != null) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            android.util.Log.i(TAG, "Center-on: calling changePage to $wfName")
                            GlobalState.getInstance().changePage(wf, "STATUS:status_trakt")
                        }, 350)
                    } else {
                        Log.w(TAG, "Center-on workflow not found: $wfName")
                    }
                } else {
                    Log.w(TAG, "Center-on workflow not set (on_click empty in block_add_gis_map_view)")
                }
            } else {
                Log.w(TAG, "Center-on: featureCenter was null")
            }
            } catch (e: Exception) {
                Log.e(TAG, "Center-on error", e)
            }
        }
        view.findViewById<ImageButton>(R.id.btn_navigate).setOnClickListener {
            val center = featureCenter(feature)
            if (center != null) {
                val (lat, lng) = center
                val uri = Uri.parse("google.navigation:q=$lat,$lng")
                val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                } catch (_: android.content.ActivityNotFoundException) {
                    val geoUri = Uri.parse("geo:$lat,$lng")
                    context.startActivity(Intent(Intent.ACTION_VIEW, geoUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }
        dialog.show()
    }

    fun getLayerNames(): List<String> {
        val names = layerState.keys.sorted().toMutableList()
        if (lastTeamMembers != null && lastTeamMembers!!.isNotEmpty()) {
            names.add(TEAM_LAYER_DISPLAY_NAME)
        }
        return names
    }
    fun isLayerVisible(layerName: String): Boolean {
        if (layerName == TEAM_LAYER_DISPLAY_NAME) return teamLayerVisible
        return layerState[layerName]?.visible ?: false
    }

    /** Last team members passed to updateTeamLayer; used for layer list (getLayerNames). */
    private var lastTeamMembers: List<TeamMemberMapPoint>? = null
    /** Team layer visibility (for layer list toggle). */
    private var teamLayerVisible: Boolean = true

    /**
     * Updates the team member layer with the given list of positions.
     * Each member is shown with their needle icon at (lat, lng).
     * Call from main thread when TeamStatusViewModel.teamMemberGisObjects emits.
     */
    fun updateTeamLayer(members: List<TeamMemberMapPoint>, batchId: String? = null) {
        // Skip re-apply when called with cached list (batchId=null) and we already have this exact list:
        // otherwise the queued getStyle callback can run after a fresher update and overwrite correct icons (e.g. me with index 11 → index 0).
        if (batchId == null && lastTeamMembers === members) {
            return
        }
        lastTeamMembers = members
        val map = mapboxMap
        if (map == null) {
            Log.w(TAG, "updateTeamLayer: mapboxMap is null, cannot add team layer (will retry when style loads)")
            return
        }
        map.getStyle { style ->
            if (style.styleLayerExists(TEAM_LAYER_ID)) style.removeStyleLayer(TEAM_LAYER_ID)
            if (style.styleSourceExists(TEAM_SOURCE_ID)) style.removeStyleSource(TEAM_SOURCE_ID)
            if (members.isEmpty()) {
                return@getStyle
            }
            // Draw "me" (current user) last so their icon is always on top when multiple markers overlap (e.g. duplicate or same location)
            val sortedMembers = members.sortedBy { if (it.name?.contains("(me)") == true) 1 else 0 }
            val features = mutableListOf<Feature>()
            // Use stable icon id per member (team_<id>) so list order changes (e.g. Set iteration) don't overwrite one member's needle with another's
            for ((index, member) in sortedMembers.withIndex()) {
                val safeId = member.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val iconId = "team_$safeId"
                val bitmap = member.iconBitmap
                if (style.hasStyleImage(iconId)) style.removeStyleImage(iconId)
                if (bitmap != null) {
                    style.addImage(iconId, bitmap, false)
                } else {
                    val drawable = ContextCompat.getDrawable(mapView.context, R.drawable.ic_needle_symbol)
                        ?: ContextCompat.getDrawable(mapView.context, R.drawable.person_away)
                    val fallback = drawable?.let { Tools.drawableToBitmap(it) }
                    if (fallback != null) {
                        style.addImage(iconId, fallback, false)
                    } else {
                        Log.w(TAG, "Could not create fallback bitmap for team member $index (${member.name})")
                    }
                }
                val point = Point.fromLngLat(member.lng, member.lat)
                val props = JsonObject().apply {
                    addProperty("icon", iconId)
                    addProperty("name", member.name ?: "")
                }
                features.add(Feature.fromGeometry(point, props))
            }
            val collection = FeatureCollection.fromFeatures(features)
            style.addSource(geoJsonSource(TEAM_SOURCE_ID) { featureCollection(collection) })
            style.addLayer(
                symbolLayer(TEAM_LAYER_ID, TEAM_SOURCE_ID) {
                    iconImage(get("icon"))
                    // Scale icon with zoom so it stays small when zoomed out (e.g. all Sweden) and correct when zoomed in
                    iconSize(step(zoom(), literal(0.2), literal(4.0) to literal(0.2), literal(8.0) to literal(0.5), literal(12.0) to literal(1.0), literal(14.0) to literal(1.2)))
                    iconAnchor(IconAnchor.BOTTOM)
                    iconAllowOverlap(true)
                    iconIgnorePlacement(true)
                }
            )
            style.getLayer(TEAM_LAYER_ID)?.visibility(if (teamLayerVisible) Visibility.VISIBLE else Visibility.NONE)
        }
    }
}
