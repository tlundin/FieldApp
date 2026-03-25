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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
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
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.ViewAnnotationAnchorConfig
import com.mapbox.maps.ViewAnnotationOptions
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.any
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.color
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.concat
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.eq
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.geometryType
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.coalesce
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
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.extension.style.sources.updateGeoJSONSourceFeatures
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.bindgen.Value
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import com.teraim.fieldapp.GlobalState
import com.teraim.fieldapp.R
import com.teraim.fieldapp.dynamic.types.DB_Context
import com.teraim.fieldapp.dynamic.types.Workflow
import com.teraim.fieldapp.dynamic.workflow_abstracts.Drawable
import com.teraim.fieldapp.non_generics.Constants
import com.teraim.fieldapp.utils.Expressor
import com.teraim.fieldapp.utils.Geomatte
import com.teraim.fieldapp.utils.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.HashMap
import java.util.Locale
import kotlin.math.abs

/**
 * Mapbox-backed map drawable. Registered as the "map" when using MapTemplate with network.
 * Layers are loaded from server: gis_objects/<type>.json (must be WGS84)
 * Layer types are determined by block_add_gis_layer blocks in the workflow XML (gistype attribute).
 */
class MapboxMapHolder(
    private val mapView: View,
    private val gisObjectsBaseUrl: String
) : Drawable {

    interface OnMapNotePlacementListener {
        fun onMapNotePlaced(point: Point, gistyp: String, label: String)
        fun onMapNotePlacementCancelled()
    }

    companion object {
        private const val TAG = "MapboxMapHolder"
        /** Zoom level at which object labels become visible (e.g. ~1/3 of Sweden visible at zoom 6). */
        private const val LABEL_VISIBLE_ZOOM_LEVEL = 6.0
        /** Zoom level for ~20 km visible horizontally/vertically (centering on trakt). */
        private const val CENTER_ON_ZOOM_LEVEL = 10.5
        private const val TEAM_SOURCE_ID = "source-team"
        private const val TEAM_LAYER_ID = "layer-team"
        private const val TEAM_HALO_LAYER_ID = "layer-team-halo"
        private const val TEAM_INNER_LAYER_ID = "layer-team-inner"
        private const val TEAM_ME_SOURCE_ID = "source-team-me"
        private const val TEAM_ME_HALO_LAYER_ID = "layer-team-me-halo"
        private const val TEAM_ME_INNER_LAYER_ID = "layer-team-me-inner"
        private const val TEAM_OTHERS_SOURCE_ID = "source-team-others"
        private const val TEAM_OTHERS_HALO_LAYER_ID = "layer-team-others-halo"
        private const val TEAM_OTHERS_INNER_LAYER_ID = "layer-team-others-inner"
        private const val NAVIGATE_LINE_SOURCE_ID = "source-navigate-line"
        private const val NAVIGATE_LINE_LAYER_ID = "layer-navigate-line"
        /** GeoJSON layer built from DB: variabler rows with GISTYP=map_note and GPSCOORD. */
        private const val MAP_NOTE_SOURCE_ID = "source-map-notes"
        private const val MAP_NOTE_LAYER_ID = "layer-map-notes"
        const val MAP_NOTE_LAYER_NAME = "map_notes"
        private const val WIGGLE_MIN_MS = 5_000L
        private const val WIGGLE_MAX_MS = 15_000L
        private const val FRESH_POSITION_MS = 5 * 60 * 1000L  // 5 minutes
        private const val WIGGLE_DURATION_MS = 150L
        private const val WIGGLE_ANGLE = 8.0
        private const val ME_PULSE_INTERVAL_MS = 5_000L
        private const val ME_PULSE_AURA_START = 10.0
        private const val ME_PULSE_AURA_EXPANDED = 28.0
        private const val ME_PULSE_AURA_OPACITY = 0.65
        private const val ME_PULSE_EXPAND_DELAY_MS = 80L
        private const val ME_PULSE_FADE_DELAY_MS = 350L
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
                loadMapNoteLayerFromDb()
                setupMapClickListener()
            }
        }

    /** "detailed" = two-circle glow for team; "normal" = needle icons. */
    var gisMode: String = "normal"
        set(value) {
            val newVal = if (value != null && value.equals("detailed", ignoreCase = true)) "detailed" else "normal"
            field = newVal
            Log.d(TAG, "MapboxMapHolder: gisMode set to $newVal (input was: $value)")
        }

    /** Workflow to run when "center on" is pressed (e.g. wf_Karta_Provytor). Null if not configured. */
    var onCenterClickWorkflow: String? = null

    /** Container for the Trakter info card. Set by MapTemplate so we use the correct view. */
    var trakterCardContainer: ViewGroup? = null
        set(value) {
            field = value
            value?.let { container ->
                container.post {
                    BottomSheetBehavior.from(container).state = BottomSheetBehavior.STATE_HIDDEN
                }
            }
        }

    private var trakterBottomSheetCallback: BottomSheetBehavior.BottomSheetCallback? = null

    private val layerState = mutableMapOf<String, LayerState>()
    /** Stored layer specs for refresh (re-fetch GeoJSON from server). */
    private val layerSpecs = mutableMapOf<String, PendingLayer>()
    private val pendingLayers = mutableListOf<PendingLayer>()
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var visible = true

    /** When set, draw dotted line from user to target and show distance. Updated when team layer refreshes. */
    @Volatile
    private var navigateTarget: Pair<Double, Double>? = null

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
        val lineDasharray: String? = null,
        val circleRadius: Float?,
        val polyType: String?,
        val objContext: String? = null,
        val onClick: String? = null,
        val gistype: String? = null,
        /** Fixed prefix for labels instead of GeoJSON TYPKOD; concatenated with OBJECTID. */
        val iconLabel: String? = null,
        /** above, below, left, right — placement relative to point or polygon centroid. */
        val iconLabelPosition: String? = null
    )

    /** Per-layer obj_context and on_click for feature click handling (e.g. TRAKTER dialog). */
    private val layerClickConfig = mutableMapOf<String, Pair<String?, String?>>()

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
        lineDasharray: String? = null,
        circleRadius: Float? = null,
        polyType: String? = null,
        objContext: String? = null,
        onClick: String? = null,
        gistype: String? = null,
        iconLabel: String? = null,
        iconLabelPosition: String? = null
    ) {
        if (layerState.containsKey(name)) {
            Log.d(TAG, "Layer $name already added")
            return
        }
        if (objContext != null || onClick != null) {
            layerClickConfig[name] = objContext to onClick
        }
        val pending = PendingLayer(
            name, label, isVisible, hasWidget, showLabels, isBold,
            fillColor, fillOpacity, lineColor, lineWidth, lineDasharray, circleRadius, polyType,
            objContext, onClick, gistype, iconLabel, iconLabelPosition
        )
        val map = mapboxMap
        if (map == null) {
            synchronized(pendingLayers) { pendingLayers.add(pending) }
            Log.d(TAG, "Queued layer $name (map not ready)")
            return
        }
        scope.launch {
            addLayerInternal(map, pending)
        }
    }

    private fun processPendingLayers() {
        val map = mapboxMap ?: return
        val toProcess = synchronized(pendingLayers) {
            pendingLayers.toList().also { pendingLayers.clear() }
        }
        scope.launch {
            for (spec in toProcess) {
                addLayerInternal(map, spec)
            }
        }
    }

    /** Layer name to GeoJSON type: e.g. akerkant_layer -> akerkant (fallback when gistype not set) */
    private fun layerNameToType(layerName: String): String =
        if (layerName.endsWith("_layer")) layerName.removeSuffix("_layer") else layerName

    private suspend fun addLayerInternal(map: MapboxMap, spec: PendingLayer, cacheBust: Boolean = false) {
        val geoJson = fetchGeoJson(spec.name, spec.gistype, cacheBust)
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
     * Fetch GeoJSON for a layer. Uses gistype when provided (explicit mapping to GeoJSON file),
     * otherwise falls back to layerNameToType(name) for backward compatibility.
     * @param cacheBust if true, appends ?t=timestamp to bypass HTTP cache (use for refresh).
     */
    private suspend fun fetchGeoJson(layerName: String, gistype: String?, cacheBust: Boolean = false): String? = withContext(Dispatchers.IO) {
        val type = gistype?.takeIf { it.isNotBlank() } ?: layerNameToType(layerName)
        val url = if (cacheBust) "$gisObjectsBaseUrl$type.json?t=${System.currentTimeMillis()}" else "$gisObjectsBaseUrl$type.json"
        fetchGeoJsonFromUrl(url, cacheBust)
    }

    private suspend fun fetchGeoJsonFromUrl(urlString: String, noCache: Boolean = false): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            if (noCache) {
                conn.setUseCaches(false)
                conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                conn.setRequestProperty("Pragma", "no-cache")
            }
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

    /** PYSTATUS color mapping: 0 = default, 2 = Error #ff0000, 3 = Ready #fca005, 4 = Ready+exported #008500, 100 = Ready+inserted #00FFFF */
    private val PYSTATUS_COLORS = mapOf(
        2 to Color.parseColor("#ff0000"),
        3 to Color.parseColor("#fca005"),
        4 to Color.parseColor("#008500"),
        100 to Color.parseColor("#00FFFF")
    )

    /**
     * Expression: color by PYSTATUS (0 or missing = default, 2 = Error, 3 = Ready, 4 = Ready+exported, 100 = Ready+inserted).
     * PYSTATUS is treated as string (status indicator). Uses to-string to normalize number/string from GeoJSON,
     * then match on string literals. Works for both circle layers and symbol layers (squares, triangles).
     */
    private fun pystatusColorExpression(defaultColorInt: Int): Expression {
        val defaultHex = "#%06X".format(0xFFFFFF and defaultColorInt)
        // match: input, label1, output1, label2, output2, ..., fallback (fallback must be last)
        val raw = """
            ["match", ["to-string", ["coalesce", ["get", "PYSTATUS"], ""]],
             "2", "#ff0000",
             "3", "#fca005",
             "4", "#008500",
             "100", "#00FFFF",
             "$defaultHex"]
        """.trimIndent().replace("\n", " ")
        return Expression.fromRaw(raw)
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
        if (layerType.equals("trakter", ignoreCase = true)) trakterStatusColorExpression()
        else pystatusColorExpression(defaultColorInt)

    private enum class IconLabelPositionKind { ABOVE, BELOW, LEFT, RIGHT }

    private fun parseIconLabelPosition(raw: String?): IconLabelPositionKind? {
        if (raw.isNullOrBlank()) return null
        return when (raw.trim().lowercase(Locale.US)) {
            "above", "top" -> IconLabelPositionKind.ABOVE
            "below", "bottom" -> IconLabelPositionKind.BELOW
            "left" -> IconLabelPositionKind.LEFT
            "right" -> IconLabelPositionKind.RIGHT
            else -> {
                Log.w(TAG, "Unknown icon_label_position '$raw', using default placement")
                null
            }
        }
    }

    /**
     * Mapbox text-anchor and text-offset (ems) for feature labels.
     * @param polygonCentroid true for the polygon-only label layer when points use a separate symbol layer.
     */
    private fun labelAnchorAndOffset(spec: PendingLayer, polygonCentroid: Boolean): Pair<TextAnchor, List<Double>> {
        val pos = parseIconLabelPosition(spec.iconLabelPosition)
        if (pos == null) {
            return if (polygonCentroid) {
                TextAnchor.CENTER to listOf(0.0, 0.0)
            } else {
                TextAnchor.BOTTOM to listOf(0.0, 2.0)
            }
        }
        return when (pos) {
            IconLabelPositionKind.ABOVE -> TextAnchor.BOTTOM to listOf(0.0, 2.0)
            IconLabelPositionKind.BELOW -> TextAnchor.TOP to listOf(0.0, 0.8)
            IconLabelPositionKind.LEFT -> TextAnchor.RIGHT to listOf(-2.2, 0.0)
            IconLabelPositionKind.RIGHT -> TextAnchor.LEFT to listOf(2.2, 0.0)
        }
    }

    /** Label text: trakter uses TRAKT; else TYPKOD + OBJECTID, or icon_label + OBJECTID when [PendingLayer.iconLabel] is set. */
    private fun labelFieldExpression(layerType: String, spec: PendingLayer): Expression {
        if (layerType.equals("trakter", ignoreCase = true)) {
            return concat(get("TRAKT"), literal(""))
        }
        val prefix = spec.iconLabel?.trim()?.takeIf { it.isNotEmpty() }
        return if (prefix != null) {
            concat(literal(prefix), get("OBJECTID"))
        } else {
            concat(get("TYPKOD"), literal(" "), get("OBJECTID"))
        }
    }

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
            // SDF=true required for iconColor to work (data-driven symbol coloring)
            style.addImage(iconId, bitmap, true)
            Log.d(TAG, "Added style image: $iconId (SDF)")
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
        val layerType = spec.gistype?.takeIf { it.isNotBlank() } ?: layerNameToType(name)
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
        val dashArray = spec.lineDasharray?.trim()?.split(",")?.mapNotNull { it.trim().toDoubleOrNull() }
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
                dashArray?.let { lineDasharray(it) }
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
                        val (tAnchor, tOff) = labelAnchorAndOffset(spec, polygonCentroid = false)
                        textField(labelFieldExpression(layerType, spec))
                        textColor(Color.BLACK)
                        textHaloColor(Color.WHITE)
                        textHaloWidth(1.0)
                        textSize(12.0)
                        textAnchor(tAnchor)
                        textOffset(tOff)
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
            val (polyLabelAnchor, polyLabelOffset) = labelAnchorAndOffset(
                spec,
                polygonCentroid = pointShapeIconId != null
            )
            addLayerOrBelowTeam(
                symbolLayer(labelLayerId, sourceId) {
                    filter(labelFilter)
                    textField(labelFieldExpression(layerType, spec))
                    textColor(Color.BLACK)
                    textHaloColor(Color.WHITE)
                    textHaloWidth(1.0)
                    textSize(12.0)
                    textAnchor(polyLabelAnchor)
                    textOffset(polyLabelOffset)
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

    /** Loads the map_note GeoJSON layer from the database and adds it to the style (with other layers, below team). */
    private fun loadMapNoteLayerFromDb() {
        val map = mapboxMap ?: return
        scope.launch {
            val geoJson = withContext(Dispatchers.IO) {
                GlobalState.getInstance().db.getMapNotePointsGeoJson()
            }
            map.getStyle { style ->
                if (style.styleSourceExists(MAP_NOTE_SOURCE_ID)) return@getStyle
                addMapNoteLayerToStyle(style, geoJson)
            }
        }
    }

    private fun addMapNoteLayerToStyle(style: Style, geoJson: String) {
        val featureCollection = FeatureCollection.fromJson(geoJson)
        val features = featureCollection.features() ?: emptyList()
        if (features.isEmpty()) {
            Log.d(TAG, "Map note layer: no map_note points in DB")
        } else {
            Log.d(TAG, "Map note layer: adding ${features.size} map_note points from DB")
        }
        if (style.styleSourceExists(MAP_NOTE_SOURCE_ID)) style.removeStyleSource(MAP_NOTE_SOURCE_ID)
        if (style.styleLayerExists(MAP_NOTE_LAYER_ID)) style.removeStyleLayer(MAP_NOTE_LAYER_ID)
        // Ensure note icons are available in the style (one icon id per gistyp)
        ensureMapNoteIconsInStyle(style)
        style.addSource(
            geoJsonSource(MAP_NOTE_SOURCE_ID) {
                featureCollection(featureCollection)
            }
        )
        fun addLayerOrBelowTeam(layer: com.mapbox.maps.extension.style.layers.Layer) {
            if (style.styleLayerExists(TEAM_LAYER_ID)) style.addLayerBelow(layer, TEAM_LAYER_ID) else style.addLayer(layer)
        }
        addLayerOrBelowTeam(
            symbolLayer(MAP_NOTE_LAYER_ID, MAP_NOTE_SOURCE_ID) {
                filter(
                    any(
                        eq(geometryType(), literal("Point")),
                        eq(geometryType(), literal("MultiPoint"))
                    )
                )
                // Choose icon by gistyp property in GeoJSON (GISTYP)
                iconImage(
                    match(
                        get("GISTYP"),
                        literal("map_note"), literal("map_note_icon"),
                        literal("map_parking"), literal("map_parking_icon"),
                        literal("map_poi"), literal("map_poi_icon"),
                        // default
                        literal("map_note_icon")
                    )
                )
                iconSize(1.0)
                iconAllowOverlap(true)
                visibility(Visibility.VISIBLE)
            }
        )
        layerState[MAP_NOTE_LAYER_NAME] = LayerState(
            MAP_NOTE_SOURCE_ID, "", "", MAP_NOTE_LAYER_ID, null, true
        )
    }

    /**
     * Ensure that the three map-note icons (note, parking, poi) are added to the style.
     * Uses the same drawable→bitmap path as other GIS icons to avoid null bitmaps.
     */
    private fun ensureMapNoteIconsInStyle(style: Style) {
        val ctx = mapView.context ?: run {
            Log.w(TAG, "ensureMapNoteIconsInStyle: mapView.context is null, skipping icon load")
            return
        }
        fun addIfMissing(id: String, resId: Int) {
            if (style.hasStyleImage(id)) return
            val drawable = ContextCompat.getDrawable(ctx, resId)
            val bitmap: Bitmap? = if (drawable != null) Tools.drawableToBitmap(drawable) else null
            if (bitmap != null) {
                // Regular bitmap icons (non-SDF); color is baked into the asset.
                style.addImage(id, bitmap, false)
                Log.d(TAG, "ensureMapNoteIconsInStyle: added style image $id")
            } else {
                Log.w(TAG, "ensureMapNoteIconsInStyle: could not create bitmap for $id (resId=$resId)")
            }
        }
        addIfMissing("map_note_icon", R.drawable.ic_map_note)
        addIfMissing("map_parking_icon", R.drawable.ic_map_parking)
        addIfMissing("map_poi_icon", R.drawable.ic_map_poi)
    }

    /** Re-fetches map_note points from the DB and updates the map note layer (e.g. after adding a new note). */
    fun refreshMapNoteLayer() {
        val map = mapboxMap ?: return
        scope.launch {
            val geoJson = withContext(Dispatchers.IO) {
                GlobalState.getInstance().db.getMapNotePointsGeoJson()
            }
            map.getStyle { style ->
                val source = style.getSource(MAP_NOTE_SOURCE_ID) as? GeoJsonSource
                if (source != null) {
                    val fc = FeatureCollection.fromJson(geoJson)
                    source.featureCollection(fc, "refresh-map-notes-${System.currentTimeMillis()}")
                    Log.d(TAG, "refreshMapNoteLayer: updated source with ${fc.features()?.size ?: 0} features")
                } else {
                    addMapNoteLayerToStyle(style, geoJson)
                }
            }
        }
    }

    /**
     * Re-fetches all GeoJSON layers from the server and updates the map.
     * Updates source data in place (keeps layers) so Mapbox re-renders with new data.
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
            val specs = layerSpecs.values.toList()
            for (spec in specs) {
                val geoJson = fetchGeoJson(spec.name, spec.gistype, cacheBust = true)
                if (geoJson == null) {
                    Log.e(TAG, "refreshLayers: failed to fetch ${spec.name}")
                    continue
                }
                val featureCollection = try {
                    FeatureCollection.fromJson(geoJson)
                } catch (e: Exception) {
                    Log.e(TAG, "refreshLayers: failed to parse GeoJSON for ${spec.name}", e)
                    continue
                }
                if (featureCollection.features().isNullOrEmpty()) {
                    Log.w(TAG, "refreshLayers: ${spec.name} has no features")
                    continue
                }
                val sourceId = "source-${spec.name}"
                withContext(Dispatchers.Main) {
                    map.getStyle { style ->
                        val source = style.getSource(sourceId) as? GeoJsonSource
                        if (source != null) {
                            source.featureCollection(featureCollection, "refresh-${System.currentTimeMillis()}")
                            Log.d(TAG, "refreshLayers: updated source $sourceId with ${featureCollection.features()!!.size} features")
                        } else {
                            Log.w(TAG, "refreshLayers: source $sourceId not found, adding layer")
                            addLayerToStyle(style, spec, geoJson)
                        }
                    }
                }
            }
            withContext(Dispatchers.Main) {
                Log.d(TAG, "refreshLayers: updated ${specs.size} layers")
                onComplete?.invoke()
            }
        }
    }

    fun setLayerVisibility(layerName: String, visible: Boolean) {
        if (layerName == TEAM_LAYER_DISPLAY_NAME) {
            teamLayerVisible = visible
            val visibility = if (visible) Visibility.VISIBLE else Visibility.NONE
            mapboxMap?.getStyle { style ->
                style.getLayer(TEAM_LAYER_ID)?.visibility(visibility)
                style.getLayer(TEAM_HALO_LAYER_ID)?.visibility(visibility)
                style.getLayer(TEAM_INNER_LAYER_ID)?.visibility(visibility)
                style.getLayer(TEAM_ME_HALO_LAYER_ID)?.visibility(visibility)
                style.getLayer(TEAM_ME_INNER_LAYER_ID)?.visibility(visibility)
                style.getLayer(TEAM_OTHERS_HALO_LAYER_ID)?.visibility(visibility)
                style.getLayer(TEAM_OTHERS_INNER_LAYER_ID)?.visibility(visibility)
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
            if (addObjectPlacementMode) {
                Handler(Looper.getMainLooper()).post { updatePlacementPoint(point) }
                return@addOnMapClickListener true
            }
            val screenCoordinate = map.pixelForCoordinate(point)
            val queryGeometry = RenderedQueryGeometry(screenCoordinate)
            val gisLayerIds = layerState.values.flatMap { state ->
                listOfNotNull(
                    state.fillLayerId,
                    state.outlineLayerId,
                    state.pointLayerId,
                    state.labelLayerId
                )
            }
            val teamLayerIds = listOf(TEAM_LAYER_ID, TEAM_ME_INNER_LAYER_ID, TEAM_OTHERS_INNER_LAYER_ID)
            val allLayerIds = gisLayerIds + teamLayerIds
            val queryOptions = RenderedQueryOptions(
                if (allLayerIds.isEmpty()) null else allLayerIds,
                null
            )
            Log.d(TAG, "Map tap: querying layers (count=${allLayerIds.size})")
            map.queryRenderedFeatures(queryGeometry, queryOptions) { result ->
                val features = result.value
                if (features.isNullOrEmpty()) {
                    Log.d(TAG, "queryRenderedFeatures: no features at tap (layerIds=${allLayerIds.size})")
                } else {
                    val queriedFeature = features.firstOrNull()
                    if (queriedFeature != null) {
                        val mapFeature = queriedFeature.queriedFeature.feature
                        val properties = mapFeature.properties()
                        val layerId = queriedFeature.layers.firstOrNull()
                        val layerName = layerId?.let { layerIdToLayerName(it) }
                        val isTeamLayer = layerId == TEAM_LAYER_ID || layerId == TEAM_ME_INNER_LAYER_ID || layerId == TEAM_OTHERS_INNER_LAYER_ID
                        Log.d(TAG, "queryRenderedFeatures: hit layer=$layerName isTeam=$isTeamLayer gistyp=${properties?.get("GISTYP")?.asString}")
                        if (properties != null) {
                            Handler(Looper.getMainLooper()).post {
                                if (isTeamLayer) {
                                    val geoPoint = featureCenter(mapFeature)?.let { (lat, lng) -> Point.fromLngLat(lng, lat) } ?: point
                                    showTeamMemberBubble(properties, geoPoint)
                                } else {
                                    showFeaturePropertiesDialog(mapFeature, properties, layerName)
                                }
                            }
                        }
                    }
                }
            }
            true
        }
    }

    private fun formatLastSeen(timestampMs: Long): String {
        if (timestampMs <= 0) return mapView.context.getString(R.string.team_member_active_now)
        val age = System.currentTimeMillis() - timestampMs
        return when {
            age < 2 * 60 * 1000 -> mapView.context.getString(R.string.team_member_active_now)
            else -> mapView.context.getString(R.string.team_member_seen_ago, Tools.getTimeStampDetails(timestampMs, true))
        }
    }

    /** Shows the team member info bubble as a view annotation at the needle's map coordinates so it stays aligned on resize/zoom. */
    private fun showTeamMemberBubble(properties: JsonObject, geoPoint: Point) {
        val context = mapView.context ?: return
        val mbMapView = mapView as? MapView ?: return
        val annotationManager = mbMapView.viewAnnotationManager
        val name = safePropString(properties, "name") ?: "—"
        val members = lastTeamMembers ?: emptyList()
        val member = members.firstOrNull { it.name == name }
        val timestampMs = member?.timestampMs ?: 0L
        val lastSeenText = formatLastSeen(timestampMs)
        val displayName = name.removeSuffix(" (me)").replace(Regex("\\[[^\\]]*\\]$"), "").trim().ifEmpty { name }
        teamMemberBubbleDismissRunnable?.let { mePulseHandler.removeCallbacks(it) }
        teamMemberBubbleView?.let { annotationManager.removeViewAnnotation(it); teamMemberBubbleView = null }
        val bubbleView = LayoutInflater.from(context).inflate(R.layout.popup_team_member_bubble, null)
        bubbleView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        bubbleView.findViewById<android.widget.TextView>(R.id.team_member_bubble_name).text = displayName
        bubbleView.findViewById<android.widget.TextView>(R.id.team_member_bubble_last_seen).text = lastSeenText
        val needleHeightPx = context.resources.getDimensionPixelSize(R.dimen.team_member_icon_top_offset)
        val options = ViewAnnotationOptions.Builder()
            .geometry(geoPoint)
            .allowOverlap(true)
            .variableAnchors(listOf(
                ViewAnnotationAnchorConfig.Builder()
                    .anchor(ViewAnnotationAnchor.BOTTOM)
                    .offsetY(needleHeightPx.toDouble())
                    .build()
            ))
            .build()
        annotationManager.addViewAnnotation(bubbleView, options)
        teamMemberBubbleView = bubbleView
        teamMemberBubbleDismissRunnable = Runnable {
            teamMemberBubbleView?.let { view ->
                (mapView as? MapView)?.viewAnnotationManager?.removeViewAnnotation(view)
            }
            teamMemberBubbleView = null
            teamMemberBubbleDismissRunnable = null
        }
        mePulseHandler.postDelayed(teamMemberBubbleDismissRunnable!!, TEAM_BUBBLE_DISMISS_MS)
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

    /** Extract layer name from Mapbox layer ID (e.g. "fill-akerkant_layer" -> "akerkant_layer"). */
    private fun layerIdToLayerName(layerId: String): String {
        for (prefix in listOf("fill-", "outline-", "point-", "label-")) {
            if (layerId.startsWith(prefix)) return layerId.removePrefix(prefix)
        }
        return layerId
    }

    /** Extract string from JsonElement. Numbers are converted without decimals (123.0 -> "123") for TRAKT/OBJECTID. */
    private fun jsonElementToString(el: JsonElement): String {
        if (el.isJsonNull) return "—"
        return try {
            when {
                el.isJsonPrimitive -> {
                    val p = el.asJsonPrimitive
                    when {
                        p.isNumber -> {
                            val n = p.asNumber
                            if (n.toLong().toDouble() == n.toDouble()) n.toLong().toString()
                            else n.toString()
                        }
                        p.isString -> p.asString
                        else -> p.toString()
                    }
                }
                else -> el.toString()
            }.ifEmpty { "—" }
        } catch (_: Exception) { "—" }
    }

    private fun safePropString(properties: JsonObject, key: String): String {
        val el = properties.get(key) ?: return "—"
        return jsonElementToString(el)
    }

    /** Build keyHash from feature properties for obj_context evaluation (lowercase keys for Expressor). */
    private fun propertiesToKeyHash(properties: JsonObject): HashMap<String, String> {
        val map = HashMap<String, String>()
        properties.keySet().forEach { key ->
            val el = properties.get(key) ?: return@forEach
            if (!el.isJsonNull) {
                val value = jsonElementToString(el)
                if (value != "—" && value.isNotEmpty()) {
                    map[key.lowercase()] = value
                    map[key] = value  // also keep original case for compatibility
                }
            }
        }
        return map
    }

    private fun showFeaturePropertiesDialog(feature: Feature, properties: JsonObject, layerName: String?) {
        val context = mapView.context ?: return
        val gistyp = properties.get("GISTYP")?.asString
        if (gistyp == "trakter") {
            showTrakterInfoDialog(context, feature, properties)
        } else {
            val (objContext, onClick) = layerName?.let { layerClickConfig[it] } ?: (null to null)
            if (objContext != null && !objContext.isBlank() && onClick != null && onClick.isNotBlank()) {
                showGisObjectStartDialog(context, feature, properties, objContext, onClick)
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
    }

    /** Card with Start button for non-TRAKT GIS objects. Sets DB_Context from obj_context and changes page to on_click. */
    private fun showGisObjectStartDialog(
        context: android.content.Context,
        feature: Feature,
        properties: JsonObject,
        objContext: String,
        onClick: String
    ) {
        var container = trakterCardContainer
        if (container == null) {
            container = (mapView.parent as? ViewGroup)?.findViewById(R.id.trakter_card_container)
        }
        if (container == null) {
            container = mapView.rootView.findViewById(R.id.trakter_card_container)
        }
        if (container == null) {
            Log.w(TAG, "trakter_card_container not found, falling back to AlertDialog")
            showGisObjectStartAlertDialog(context, feature, properties, objContext, onClick)
            return
        }
        container.removeAllViews()
        val card = LayoutInflater.from(context).inflate(R.layout.card_gis_object_start, container, false)
        val label = buildString {
            val typkod = safePropString(properties, "TYPKOD")
            val objectId = safePropString(properties, "OBJECTID")
            if (typkod != "—" || objectId != "—") append("$typkod $objectId".trim())
            else append(context.getString(R.string.gis_object))
        }
        val pystatusVal = try {
            properties.get("PYSTATUS")?.takeIf { !it.isJsonNull }?.let { el ->
                if (el.isJsonPrimitive && el.asJsonPrimitive.isNumber) el.asInt else null
            }
        } catch (_: Exception) { null }
        val (_, pystatusColor) = pystatusToLabelAndColor(pystatusVal)
        val titleView = card.findViewById<android.widget.TextView>(R.id.gis_object_title)
        val infoView = card.findViewById<android.widget.TextView>(R.id.gis_object_info)
        val statusIndicator = card.findViewById<View>(R.id.gis_object_status_indicator)
        titleView.text = label
        val indicatorDrawable = android.graphics.drawable.GradientDrawable().apply {
            setColor(pystatusColor)
            setStroke(1, Color.GRAY)
            cornerRadius = 2 * context.resources.displayMetrics.density
        }
        statusIndicator.background = indicatorDrawable
        infoView.text = propertiesToInfoString(properties)
        fun dismissCard() {
            val behavior = BottomSheetBehavior.from(container)
            behavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
        // Close button removed from card layout; bottom sheet can still be dismissed via drag.
        card.findViewById<View>(R.id.btn_navigate)?.setOnClickListener {
            val center = featureCenter(feature)
            if (center != null) {
                startNavigateTo(center.first, center.second)
                dismissCard()
            }
        }
        card.findViewById<View>(R.id.btn_start).setOnClickListener {
            runStartWorkflow(feature, properties, objContext, onClick)
            dismissCard()
        }
        container.addView(card)
        val behavior = BottomSheetBehavior.from(container)
        behavior.isHideable = true
        trakterBottomSheetCallback?.let { behavior.removeBottomSheetCallback(it) }
        trakterBottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    container.removeAllViews()
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }
        behavior.addBottomSheetCallback(trakterBottomSheetCallback!!)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    /** Fallback when trakter_card_container is not in the view hierarchy. */
    private fun showGisObjectStartAlertDialog(
        context: android.content.Context,
        feature: Feature,
        properties: JsonObject,
        objContext: String,
        onClick: String
    ) {
        val card = LayoutInflater.from(context).inflate(R.layout.card_gis_object_start, null)
        val label = buildString {
            val typkod = safePropString(properties, "TYPKOD")
            val objectId = safePropString(properties, "OBJECTID")
            if (typkod != "—" || objectId != "—") append("$typkod $objectId".trim())
            else append(context.getString(R.string.gis_object))
        }
        val pystatusVal = try {
            properties.get("PYSTATUS")?.takeIf { !it.isJsonNull }?.let { el ->
                if (el.isJsonPrimitive && el.asJsonPrimitive.isNumber) el.asInt else null
            }
        } catch (_: Exception) { null }
        val (_, pystatusColor) = pystatusToLabelAndColor(pystatusVal)
        val titleView = card.findViewById<android.widget.TextView>(R.id.gis_object_title)
        val infoView = card.findViewById<android.widget.TextView>(R.id.gis_object_info)
        val statusIndicator = card.findViewById<View>(R.id.gis_object_status_indicator)
        titleView.text = label
        val indicatorDrawable = android.graphics.drawable.GradientDrawable().apply {
            setColor(pystatusColor)
            setStroke(1, Color.GRAY)
            cornerRadius = 2 * context.resources.displayMetrics.density
        }
        statusIndicator.background = indicatorDrawable
        infoView.text = propertiesToInfoString(properties)
        val dialog = AlertDialog.Builder(context).setView(card).create()
        // Close button removed from card layout; dialog can be dismissed via outside/back.
        card.findViewById<View>(R.id.btn_navigate)?.setOnClickListener {
            val center = featureCenter(feature)
            if (center != null) {
                startNavigateTo(center.first, center.second)
                dialog.dismiss()
            }
        }
        card.findViewById<View>(R.id.btn_start).setOnClickListener {
            runStartWorkflow(feature, properties, objContext, onClick)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun runStartWorkflow(feature: Feature, properties: JsonObject, objContext: String, onClick: String) {
        try {
            val keyHash = propertiesToKeyHash(properties)
            GlobalState.getInstance().setDBContext(DB_Context(null, keyHash))
            val objContextE = Expressor.preCompileExpression(objContext)
            val dbContext = DB_Context.evaluate(objContextE)
            if (dbContext.isOk) {
                val ctx = dbContext.context ?: HashMap()
                val merged = HashMap(ctx)
                merged["år"] = Constants.getYear()
                safePropString(properties, "FIXEDGID").takeIf { it != "—" }?.removeSurrounding("{", "}")?.takeIf { it.isNotBlank() }?.let { merged["uid"] = it }
                GlobalState.getInstance().setDBContext(DB_Context(null, merged))
            } else {
                Log.w(TAG, "obj_context evaluation failed: " + dbContext.toString())
                return
            }
            val wf = GlobalState.getInstance().getWorkflow(onClick)
            if (wf != null) {
                val center = featureCenter(feature)
                if (center != null) {
                    val zoom = mapboxMap?.cameraState?.zoom
                    GlobalState.getInstance().setPendingMapCenter(center.first, center.second, zoom)
                }
                GlobalState.getInstance().changePage(wf, null)
            } else {
                Log.w(TAG, "Workflow not found: $onClick")
            }
        } catch (e: Exception) {
            Log.e(TAG, "runStartWorkflow error", e)
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

    private fun pystatusToLabelAndColor(value: Int?): Pair<String, Int> {
        val defaultColor = Color.parseColor("#cccccc")
        val (labelRes, color) = when {
            value == null || value == 0 -> R.string.pystatus_default to defaultColor
            value == 2 -> R.string.pystatus_error to PYSTATUS_COLORS[2]!!
            value == 3 -> R.string.pystatus_ready to PYSTATUS_COLORS[3]!!
            value == 4 -> R.string.pystatus_exported to PYSTATUS_COLORS[4]!!
            value == 100 -> R.string.pystatus_inserted to PYSTATUS_COLORS[100]!!
            else -> R.string.pystatus_default to defaultColor
        }
        return mapView.context.getString(labelRes) to color
    }

    private fun propertiesToInfoString(properties: JsonObject): String = buildString {
        for ((key, value) in properties.entrySet()) {
            if (key == "geometry" || key == "geometry_name") continue
            val strVal = value?.takeIf { !it.isJsonNull }?.let { jsonElementToString(it) } ?: "—"
            val displayVal = if (key == "PYSTATUS") {
                val pystatusVal = try {
                    value?.takeIf { !it.isJsonNull }?.let { el ->
                        if (el.isJsonPrimitive && el.asJsonPrimitive.isNumber) el.asInt else null
                    }
                } catch (_: Exception) { null }
                val (label, _) = pystatusToLabelAndColor(pystatusVal)
                "$strVal ($label)"
            } else strVal
            append("$key: "); appendLine(displayVal)
        }
    }

    private fun showTrakterInfoDialog(context: android.content.Context, feature: Feature, properties: JsonObject) {
        var container = trakterCardContainer
        if (container == null) {
            container = (mapView.parent as? ViewGroup)?.findViewById(R.id.trakter_card_container)
        }
        if (container == null) {
            container = mapView.rootView.findViewById(R.id.trakter_card_container)
        }
        if (container == null) {
            Log.w(TAG, "trakter_card_container not found, falling back to AlertDialog with card layout")
            showTrakterInfoAlertDialog(context, feature, properties)
            return
        }
        container.removeAllViews()
        val card = LayoutInflater.from(context).inflate(R.layout.card_trakter_info, container, false)
        val trakt = safePropString(properties, "TRAKT")
        val traktStatusVal = try {
            properties.get("TRAKTSTATUS")?.takeIf { !it.isJsonNull }?.let { el ->
                if (el.isJsonPrimitive && el.asJsonPrimitive.isNumber) el.asInt else null
            }
        } catch (_: Exception) { null }
        val (traktStatusLabel, traktStatusColor) = traktStatusToLabelAndColor(traktStatusVal)
        val objectId = safePropString(properties, "OBJECTID")
        val typkod = safePropString(properties, "TYPKOD")
        val column1 = safePropString(properties, "COLUMN1")
        val titleView = card.findViewById<android.widget.TextView>(R.id.trakter_title)
        val infoView = card.findViewById<android.widget.TextView>(R.id.trakter_info)
        val statusIndicator = card.findViewById<View>(R.id.trakter_status_indicator)
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
        fun dismissCard() {
            val behavior = BottomSheetBehavior.from(container)
            behavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
        // Close button removed from card layout; bottom sheet can still be dismissed via drag.
        card.findViewById<View>(R.id.btn_center_on).setOnClickListener {
            Log.i(TAG, "Center-on button clicked")
            try {
                val center = featureCenter(feature)
                Log.i(TAG, "Center-on: featureCenter=$center")
                if (center != null) {
                    val (lat, lng) = center
                    val point = Point.fromLngLat(lng, lat)
                    val cameraOptions = CameraOptions.Builder()
                        .center(point)
                        .zoom(CENTER_ON_ZOOM_LEVEL)
                        .build()
                    (mapView as? MapView)?.camera?.easeTo(cameraOptions)
                    dismissCard()
                    performCenterOnTrakterWorkflow(trakt, properties, lat, lng)
                } else {
                    Log.w(TAG, "Center-on: featureCenter was null")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Center-on error", e)
            }
        }
        card.findViewById<View>(R.id.btn_navigate).setOnClickListener {
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
            dismissCard()
        }
        container.addView(card)
        val behavior = BottomSheetBehavior.from(container)
        behavior.isHideable = true
        trakterBottomSheetCallback?.let { behavior.removeBottomSheetCallback(it) }
        trakterBottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    container.removeAllViews()
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }
        behavior.addBottomSheetCallback(trakterBottomSheetCallback!!)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun performCenterOnTrakterWorkflow(trakt: String, properties: JsonObject, lat: Double, lng: Double) {
        val (objContext, onClick) = layerClickConfig["trakter"] ?: (null to null)
        val keyHash = HashMap<String, String>()
        keyHash["trakt"] = trakt
        GlobalState.getInstance().setDBContext(DB_Context(null, keyHash))
        val dbContext = if (objContext != null && objContext.isNotBlank()) {
            val objContextE = Expressor.preCompileExpression(objContext)
            val evaluated = DB_Context.evaluate(objContextE)
            if (evaluated.isOk) {
                val ctx = evaluated.context
                val merged = if (ctx != null) HashMap(ctx) else HashMap<String, String>()
                merged["gistyp"] = "Trakter"
                merged["år"] = Constants.getYear()
                safePropString(properties, "FIXEDGID").takeIf { it != "—" }?.removeSurrounding("{", "}")?.takeIf { it.isNotBlank() }?.let { merged["uid"] = it }
                DB_Context(null, merged)
            } else evaluated
        } else {
            keyHash["gistyp"] = "Trakter"
            keyHash["år"] = Constants.getYear()
            safePropString(properties, "FIXEDGID").takeIf { it != "—" }?.removeSurrounding("{", "}")?.takeIf { it.isNotBlank() }?.let { keyHash["uid"] = it }
            DB_Context(null, HashMap(keyHash))
        }
        if (dbContext.isOk) {
            GlobalState.getInstance().setDBContext(dbContext)
        } else {
            Log.w(TAG, "Center-on: obj_context evaluation failed: " + dbContext.toString())
        }
        GlobalState.getInstance().setPendingMapCenter(lat, lng)
        val wfName = onClick ?: onCenterClickWorkflow
        Log.i(TAG, "Center-on: workflow=$wfName (layer onClick=$onClick, map onCenterClick=$onCenterClickWorkflow)")
        if (!wfName.isNullOrBlank()) {
            val wf = GlobalState.getInstance().getWorkflow(wfName)
            Log.i(TAG, "Center-on: workflow lookup result=${if (wf != null) "found" else "null"}")
            if (wf != null) {
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.i(TAG, "Center-on: calling changePage to $wfName")
                    GlobalState.getInstance().changePage(wf, "STATUS:status_trakt")
                }, 350)
            } else {
                Log.w(TAG, "Center-on workflow not found: $wfName")
            }
        } else {
            Log.w(TAG, "Center-on workflow not set (on_click empty in block_add_gis_layer and block_add_gis_map_view)")
        }
    }

    /** Fallback when trakter_card_container is not in the view hierarchy: show Material card in AlertDialog. */
    private fun showTrakterInfoAlertDialog(context: android.content.Context, feature: Feature, properties: JsonObject) {
        val card = LayoutInflater.from(context).inflate(R.layout.card_trakter_info, null)
        val trakt = safePropString(properties, "TRAKT")
        val traktStatusVal = try {
            properties.get("TRAKTSTATUS")?.takeIf { !it.isJsonNull }?.let { el ->
                if (el.isJsonPrimitive && el.asJsonPrimitive.isNumber) el.asInt else null
            }
        } catch (_: Exception) { null }
        val (traktStatusLabel, traktStatusColor) = traktStatusToLabelAndColor(traktStatusVal)
        val objectId = safePropString(properties, "OBJECTID")
        val typkod = safePropString(properties, "TYPKOD")
        val column1 = safePropString(properties, "COLUMN1")
        val titleView = card.findViewById<android.widget.TextView>(R.id.trakter_title)
        val infoView = card.findViewById<android.widget.TextView>(R.id.trakter_info)
        val statusIndicator = card.findViewById<View>(R.id.trakter_status_indicator)
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
        val dialog = AlertDialog.Builder(context).setView(card).create()
        // Close button removed from card layout; dialog can be dismissed via outside/back.
        card.findViewById<View>(R.id.btn_center_on).setOnClickListener {
            val center = featureCenter(feature)
            if (center != null) {
                val (lat, lng) = center
                val point = Point.fromLngLat(lng, lat)
                (mapView as? MapView)?.camera?.easeTo(CameraOptions.Builder().center(point).zoom(CENTER_ON_ZOOM_LEVEL).build())
                dialog.dismiss()
                performCenterOnTrakterWorkflow(trakt, properties, lat, lng)
            }
        }
        card.findViewById<View>(R.id.btn_navigate).setOnClickListener {
            val center = featureCenter(feature)
            if (center != null) {
                val (lat, lng) = center
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: android.content.ActivityNotFoundException) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            dialog.dismiss()
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

    private fun isMe(member: TeamMemberMapPoint): Boolean {
        val myUuid = GlobalState.getInstance().userUUID ?: return false
        val memberUuid = member.uuid ?: return false
        return memberUuid.trim().equals(myUuid.trim(), ignoreCase = true)
    }
    /** Team layer visibility (for layer list toggle). Defaults to true; MapTemplate may override based on GisMapView. */
    private var teamLayerVisible: Boolean = true

    /** Allows MapTemplate / GisMapView config to set initial visibility of the team layer. */
    fun setInitialTeamLayerVisible(visible: Boolean) {
        teamLayerVisible = visible
    }
    private val mePulseHandler = Handler(Looper.getMainLooper())
    private var mePulseRunnable: Runnable? = null
    private var mePulseExpandRunnable: Runnable? = null
    private var mePulseHideRunnable: Runnable? = null
    private var wiggleRunnable: Runnable? = null
    private var wiggleResetRunnable: Runnable? = null
    /** True once we've scheduled a wiggle for the current team layer; reset when layer is torn down. */
    private var wiggleScheduled = false
    private var teamMemberBubbleView: View? = null
    private var teamMemberBubbleDismissRunnable: Runnable? = null
    private val TEAM_BUBBLE_DISMISS_MS = 10_000L

    /** Map note placement mode: user selected an object type and is choosing location on map. */
    private var addObjectPlacementMode = false
    private var addObjectGistyp: String? = null
    private var addObjectLabel: String? = null
    private var placementPoint: Point? = null
    private var placementPreviewView: View? = null
    private var placementBarView: View? = null
    private var onMapNotePlacementListener: OnMapNotePlacementListener? = null

    fun setOnMapNotePlacementListener(listener: OnMapNotePlacementListener?) {
        onMapNotePlacementListener = listener
    }

    /** Start "add object" flow: show placement UI; user taps map to set position, then OK or Cancel. */
    fun startAddObjectMode(gistyp: String, label: String) {
        if (addObjectPlacementMode) return
        addObjectPlacementMode = true
        addObjectGistyp = gistyp
        addObjectLabel = label
        placementPoint = null
        showPlacementBar()
        Log.d(TAG, "Add object mode started: gistyp=$gistyp label=$label")
    }

    private fun showPlacementBar() {
        val context = mapView.context ?: return
        val parent = mapView.parent as? ViewGroup ?: return
        val bar = LayoutInflater.from(context).inflate(R.layout.map_note_placement_bar, parent, false)
        bar.findViewById<Button>(R.id.map_note_placement_ok).setOnClickListener {
            if (placementPoint != null && addObjectGistyp != null && addObjectLabel != null) {
                onMapNotePlacementListener?.onMapNotePlaced(placementPoint!!, addObjectGistyp!!, addObjectLabel!!)
            }
            cancelPlacementMode()
        }
        bar.findViewById<Button>(R.id.map_note_placement_cancel).setOnClickListener { cancelPlacementMode() }
        bar.findViewById<Button>(R.id.map_note_placement_ok).isEnabled = false
        parent.addView(bar)
        placementBarView = bar
    }

    private fun updatePlacementPoint(point: Point) {
        placementPoint = point
        val mbMapView = mapView as? MapView ?: return
        val annotationManager = mbMapView.viewAnnotationManager
        placementPreviewView?.let { annotationManager.removeViewAnnotation(it); placementPreviewView = null }
        val context = mapView.context ?: return
        val preview = ImageView(context).apply {
            setImageResource(R.drawable.ic_needle_symbol)
            layoutParams = ViewGroup.LayoutParams(
                context.resources.getDimensionPixelSize(R.dimen.team_member_icon_top_offset) * 2,
                context.resources.getDimensionPixelSize(R.dimen.team_member_icon_top_offset) * 3
            )
        }
        val options = ViewAnnotationOptions.Builder()
            .geometry(point)
            .allowOverlap(true)
            .variableAnchors(listOf(
                ViewAnnotationAnchorConfig.Builder()
                    .anchor(ViewAnnotationAnchor.CENTER)
                    .build()
            ))
            .build()
        annotationManager.addViewAnnotation(preview, options)
        placementPreviewView = preview
        placementBarView?.findViewById<Button>(R.id.map_note_placement_ok)?.isEnabled = true
    }

    private fun cancelPlacementMode() {
        addObjectPlacementMode = false
        addObjectGistyp = null
        addObjectLabel = null
        placementPoint = null
        placementPreviewView?.let { (mapView as? MapView)?.viewAnnotationManager?.removeViewAnnotation(it) }
        placementPreviewView = null
        placementBarView?.let { (it.parent as? ViewGroup)?.removeView(it) }
        placementBarView = null
        onMapNotePlacementListener?.onMapNotePlacementCancelled()
        Log.d(TAG, "Add object mode cancelled")
    }

    private fun startMePulse() {
        stopMePulse()
        mePulseRunnable = object : Runnable {
            override fun run() {
                val map = mapboxMap ?: return
                map.getStyle { style ->
                    if (style.styleLayerExists(TEAM_ME_HALO_LAYER_ID)) {
                        style.setStyleLayerProperty(TEAM_ME_HALO_LAYER_ID, "circle-opacity", Value(ME_PULSE_AURA_OPACITY))
                        style.setStyleLayerProperty(TEAM_ME_HALO_LAYER_ID, "circle-radius", Value(ME_PULSE_AURA_START))
                    }
                }
                mePulseExpandRunnable = Runnable {
                    mapboxMap?.getStyle { s ->
                        if (s.styleLayerExists(TEAM_ME_HALO_LAYER_ID)) {
                            s.setStyleLayerProperty(TEAM_ME_HALO_LAYER_ID, "circle-radius", Value(ME_PULSE_AURA_EXPANDED))
                        }
                    }
                    mePulseExpandRunnable = null
                }
                mePulseHideRunnable = Runnable {
                    mapboxMap?.getStyle { s ->
                        if (s.styleLayerExists(TEAM_ME_HALO_LAYER_ID)) {
                            s.setStyleLayerProperty(TEAM_ME_HALO_LAYER_ID, "circle-opacity", Value(0.0))
                        }
                    }
                    mePulseHideRunnable = null
                }
                mePulseHandler.postDelayed(mePulseExpandRunnable!!, ME_PULSE_EXPAND_DELAY_MS)
                mePulseHandler.postDelayed(mePulseHideRunnable!!, ME_PULSE_FADE_DELAY_MS)
                mePulseHandler.postDelayed(this, ME_PULSE_INTERVAL_MS)
            }
        }
        mePulseHandler.postDelayed(mePulseRunnable!!, ME_PULSE_INTERVAL_MS)
    }

    private fun stopMePulse() {
        mePulseRunnable?.let { mePulseHandler.removeCallbacks(it) }
        mePulseRunnable = null
        mePulseExpandRunnable?.let { mePulseHandler.removeCallbacks(it) }
        mePulseExpandRunnable = null
        mePulseHideRunnable?.let { mePulseHandler.removeCallbacks(it) }
        mePulseHideRunnable = null
    }

    /** Call when holder is no longer needed (e.g. MapTemplate.onDestroyView) to stop pulse animation. */
    fun release() {
        stopMePulse()
        stopWiggle()
        stopNavigate()
    }

    private fun scheduleWiggle() {
        if (wiggleScheduled) return
        wiggleScheduled = true
        wiggleRunnable?.let { mePulseHandler.removeCallbacks(it) }
        val delay = (WIGGLE_MIN_MS..WIGGLE_MAX_MS).random()
        Log.d(TAG, "wiggle: scheduled in ${delay}ms")
        wiggleRunnable = Runnable {
            performWiggle()
            wiggleRunnable = null
            wiggleScheduled = false
            scheduleWiggle()
        }
        mePulseHandler.postDelayed(wiggleRunnable!!, delay)
    }

    private fun stopWiggle() {
        wiggleRunnable?.let { mePulseHandler.removeCallbacks(it) }
        wiggleRunnable = null
        wiggleResetRunnable?.let { mePulseHandler.removeCallbacks(it) }
        wiggleResetRunnable = null
        wiggleScheduled = false
    }

    private fun performWiggle() {
        val members = lastTeamMembers ?: run {
            Log.d(TAG, "wiggle: no members, skipping")
            return
        }
        if (gisMode != "normal") {
            Log.d(TAG, "wiggle: gisMode=$gisMode (need 'normal' for needles), skipping")
            return
        }
        val now = System.currentTimeMillis()
        val fresh = members.filter { it.timestampMs > 0 && (now - it.timestampMs) < FRESH_POSITION_MS }
        val meMember = members.firstOrNull { isMe(it) }
        val meAgeMs = meMember?.let { if (it.timestampMs > 0) now - it.timestampMs else -1L } ?: -1L
        val meFresh = meMember != null && meMember.timestampMs > 0 && meAgeMs in 0 until FRESH_POSITION_MS
        Log.d(TAG, "wiggle: members=${members.size} fresh=${fresh.size} gisMode=$gisMode | me: ts=${meMember?.timestampMs ?: 0} age=${if (meAgeMs >= 0) "${meAgeMs / 1000}s" else "n/a"} fresh=$meFresh (need ts>0 and age<5min)")
        if (fresh.isEmpty()) {
            Log.d(TAG, "wiggle: no fresh members (all >5min or ts=0), skipping")
            return
        }
        val map = mapboxMap ?: return
        map.getStyle { style ->
            if (!style.styleSourceExists(TEAM_SOURCE_ID)) return@getStyle
            val source = style.getSource(TEAM_SOURCE_ID) as? GeoJsonSource ?: return@getStyle
            val freshIds = fresh.map { it.id.replace(Regex("[^a-zA-Z0-9_-]"), "_") }.toSet()
            val features = members.map { member ->
                val safeId = member.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val iconId = "team_$safeId"
                val point = Point.fromLngLat(member.lng, member.lat)
                val isMe = isMe(member)
                val wiggle = freshIds.contains(safeId)
                val props = JsonObject().apply {
                    addProperty("name", member.name ?: "")
                    addProperty("isMe", if (isMe) 1 else 0)
                    addProperty("icon", iconId)
                    addProperty("iconRotate", if (wiggle) WIGGLE_ANGLE else 0.0)
                }
                Feature.fromGeometry(point, props, "team_$safeId")
            }
            source.updateGeoJSONSourceFeatures(features)
            val wigglingIds = fresh.map { it.id }.joinToString()
            Log.d(TAG, "wiggle: applied to $wigglingIds (me in list: ${fresh.any { isMe(it) }})")
        }
        wiggleResetRunnable?.let { mePulseHandler.removeCallbacks(it) }
        wiggleResetRunnable = Runnable {
            performWiggleReset()
            wiggleResetRunnable = null
        }
        mePulseHandler.postDelayed(wiggleResetRunnable!!, WIGGLE_DURATION_MS)
    }

    private fun performWiggleReset() {
        val members = lastTeamMembers ?: return
        if (gisMode != "normal") return
        val map = mapboxMap ?: return
        map.getStyle { style ->
            if (!style.styleSourceExists(TEAM_SOURCE_ID)) return@getStyle
            val source = style.getSource(TEAM_SOURCE_ID) as? GeoJsonSource ?: return@getStyle
            val features = members.map { member ->
                val safeId = member.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val iconId = "team_$safeId"
                val point = Point.fromLngLat(member.lng, member.lat)
                val isMe = isMe(member)
                val props = JsonObject().apply {
                    addProperty("name", member.name ?: "")
                    addProperty("isMe", if (isMe) 1 else 0)
                    addProperty("icon", iconId)
                    addProperty("iconRotate", 0.0)
                }
                Feature.fromGeometry(point, props, "team_$safeId")
            }
            source.updateGeoJSONSourceFeatures(features)
        }
    }

    /**
     * Start navigate mode: draw dotted line from user to target and show distance.
     * Line and distance updates when updateTeamLayer is called with "me" position.
     */
    fun startNavigateTo(targetLat: Double, targetLng: Double) {
        navigateTarget = targetLat to targetLng
        val map = mapboxMap ?: return
        map.getStyle { style ->
            val userLat = lastTeamMembers?.firstOrNull { isMe(it) }?.lat
            val userLng = lastTeamMembers?.firstOrNull { isMe(it) }?.lng
            if (userLat != null && userLng != null) {
                updateNavigateLineInternal(style, userLat, userLng, targetLat, targetLng)
            } else {
                showNavigateDistance(-1.0)  // "—" until GPS available
            }
        }
        showNavigateDistanceOverlay(true)
    }

    /** Stop navigate mode: remove line and hide distance overlay. */
    fun stopNavigate() {
        navigateTarget = null
        val map = mapboxMap ?: return
        map.getStyle { style ->
            if (style.styleLayerExists(NAVIGATE_LINE_LAYER_ID)) style.removeStyleLayer(NAVIGATE_LINE_LAYER_ID)
            if (style.styleSourceExists(NAVIGATE_LINE_SOURCE_ID)) style.removeStyleSource(NAVIGATE_LINE_SOURCE_ID)
        }
        showNavigateDistanceOverlay(false)
    }

    private fun showNavigateDistanceOverlay(show: Boolean) {
        Handler(Looper.getMainLooper()).post {
            val root = mapView.rootView
            val container = root.findViewById<View>(R.id.navigate_distance_container)
            container?.visibility = if (show) View.VISIBLE else View.GONE
            if (show) {
                container?.findViewById<View>(R.id.navigate_distance_close)?.setOnClickListener {
                    stopNavigate()
                }
            }
        }
    }

    private fun showNavigateDistance(distanceM: Double) {
        Handler(Looper.getMainLooper()).post {
            val root = mapView.rootView
            val textView = root.findViewById<android.widget.TextView>(R.id.navigate_distance_text)
            if (textView != null) {
                textView.text = when {
                    distanceM < 0 -> "—"
                    distanceM < 1000 -> "${distanceM.toInt()} m"
                    else -> String.format("%.1f km", distanceM / 1000)
                }
            }
        }
    }

    private fun updateNavigateLineInternal(
        style: Style,
        userLat: Double,
        userLng: Double,
        targetLat: Double,
        targetLng: Double
    ) {
        val line = com.mapbox.geojson.LineString.fromLngLats(
            listOf(Point.fromLngLat(userLng, userLat), Point.fromLngLat(targetLng, targetLat))
        )
        val feature = Feature.fromGeometry(line)
        if (style.styleSourceExists(NAVIGATE_LINE_SOURCE_ID)) {
            (style.getSource(NAVIGATE_LINE_SOURCE_ID) as? GeoJsonSource)?.feature(feature)
        } else {
            style.addSource(geoJsonSource(NAVIGATE_LINE_SOURCE_ID) { feature(feature) })
            style.addLayer(
                lineLayer(NAVIGATE_LINE_LAYER_ID, NAVIGATE_LINE_SOURCE_ID) {
                    lineColor(Color.WHITE)
                    lineWidth(4.0)
                    lineDasharray(listOf(1.0, 2.0))
                }
            )
        }
        val distanceKm = Geomatte.dist(userLat, userLng, targetLat, targetLng)
        showNavigateDistance(distanceKm * 1000)
    }

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
            val layersExist = gisMode == "detailed" && style.styleLayerExists(TEAM_ME_HALO_LAYER_ID)
            if (layersExist && members.isNotEmpty()) {
                val sortedMembers = members.sortedBy { if (isMe(it)) 1 else 0 }
                val meFeatures = mutableListOf<Feature>()
                val othersFeatures = mutableListOf<Feature>()
                for ((idx, member) in sortedMembers.withIndex()) {
                    val isMe = isMe(member)
                    val point = Point.fromLngLat(member.lng, member.lat)
                    val props = JsonObject().apply {
                        addProperty("name", member.name ?: "")
                        addProperty("isMe", if (isMe) 1 else 0)
                    }
                    val f = Feature.fromGeometry(point, props, if (isMe) "me" else "others-$idx")
                    if (isMe) meFeatures.add(f) else othersFeatures.add(f)
                }
                // Update source data in place to avoid "Source already exists" (remove/add is racy)
                (style.getSource(TEAM_ME_SOURCE_ID) as? GeoJsonSource)?.updateGeoJSONSourceFeatures(meFeatures)
                (style.getSource(TEAM_OTHERS_SOURCE_ID) as? GeoJsonSource)?.updateGeoJSONSourceFeatures(othersFeatures)
                val target = navigateTarget
                if (target != null) {
                    val me = members.firstOrNull { isMe(it) }
                    if (me != null) {
                        updateNavigateLineInternal(style, me.lat, me.lng, target.first, target.second)
                    }
                }
                return@getStyle
            }
            stopMePulse()
            if (style.styleLayerExists(TEAM_LAYER_ID)) style.removeStyleLayer(TEAM_LAYER_ID)
            if (style.styleLayerExists(TEAM_HALO_LAYER_ID)) style.removeStyleLayer(TEAM_HALO_LAYER_ID)
            if (style.styleLayerExists(TEAM_INNER_LAYER_ID)) style.removeStyleLayer(TEAM_INNER_LAYER_ID)
            if (style.styleLayerExists(TEAM_ME_HALO_LAYER_ID)) style.removeStyleLayer(TEAM_ME_HALO_LAYER_ID)
            if (style.styleLayerExists(TEAM_ME_INNER_LAYER_ID)) style.removeStyleLayer(TEAM_ME_INNER_LAYER_ID)
            if (style.styleLayerExists(TEAM_OTHERS_HALO_LAYER_ID)) style.removeStyleLayer(TEAM_OTHERS_HALO_LAYER_ID)
            if (style.styleLayerExists(TEAM_OTHERS_INNER_LAYER_ID)) style.removeStyleLayer(TEAM_OTHERS_INNER_LAYER_ID)
            if (style.styleSourceExists(TEAM_SOURCE_ID)) style.removeStyleSource(TEAM_SOURCE_ID)
            if (style.styleSourceExists(TEAM_ME_SOURCE_ID)) style.removeStyleSource(TEAM_ME_SOURCE_ID)
            if (style.styleSourceExists(TEAM_OTHERS_SOURCE_ID)) style.removeStyleSource(TEAM_OTHERS_SOURCE_ID)
            if (members.isEmpty()) {
                return@getStyle
            }
            // Draw "me" (current user) last so their icon is always on top when multiple markers overlap (e.g. duplicate or same location)
            val sortedMembers = members.sortedBy { if (isMe(it)) 1 else 0 }
            val features = mutableListOf<Feature>()
            val meFeatures = mutableListOf<Feature>()
            val othersFeatures = mutableListOf<Feature>()
            val useDetailed = gisMode == "detailed"
            for ((index, member) in sortedMembers.withIndex()) {
                val isMe = isMe(member)
                val point = Point.fromLngLat(member.lng, member.lat)
                val props = JsonObject().apply {
                    addProperty("name", member.name ?: "")
                    addProperty("isMe", if (isMe) 1 else 0)
                }
                if (useDetailed) {
                    val f = Feature.fromGeometry(point, props, if (isMe) "me" else "others-$index")
                    features.add(f)
                    if (isMe) meFeatures.add(f) else othersFeatures.add(f)
                } else {
                    // Normal mode: symbol layer with needle icons
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
                    props.addProperty("icon", iconId)
                    props.addProperty("iconRotate", 0.0)
                    features.add(Feature.fromGeometry(point, props, "team_$safeId"))
                }
            }
            if (useDetailed) {
                Log.d(TAG, "updateTeamLayer: adding circle layers (detailed mode)")
                val othersColor = color(Color.parseColor("#4285F4"))
                val meColor = color(Color.WHITE)
                val baseRadius = step(zoom(), literal(6.0), literal(8.0) to literal(10.0), literal(12.0) to literal(16.0), literal(14.0) to literal(24.0))
                val innerRadius = step(zoom(), literal(3.0), literal(8.0) to literal(5.0), literal(12.0) to literal(8.0), literal(14.0) to literal(10.0))
                if (othersFeatures.isNotEmpty()) {
                    val othersCollection = FeatureCollection.fromFeatures(othersFeatures)
                    style.addSource(geoJsonSource(TEAM_OTHERS_SOURCE_ID) { featureCollection(othersCollection) })
                    style.addLayer(
                        circleLayer(TEAM_OTHERS_HALO_LAYER_ID, TEAM_OTHERS_SOURCE_ID) {
                            circleRadius(baseRadius)
                            circleColor(othersColor)
                            circleOpacity(0.25)
                            circleStrokeWidth(0.0)
                        }
                    )
                    style.addLayer(
                        circleLayer(TEAM_OTHERS_INNER_LAYER_ID, TEAM_OTHERS_SOURCE_ID) {
                            circleRadius(innerRadius)
                            circleColor(othersColor)
                            circleOpacity(1.0)
                            circleStrokeWidth(0.0)
                        }
                    )
                    style.getLayer(TEAM_OTHERS_HALO_LAYER_ID)?.visibility(if (teamLayerVisible) Visibility.VISIBLE else Visibility.NONE)
                    style.getLayer(TEAM_OTHERS_INNER_LAYER_ID)?.visibility(if (teamLayerVisible) Visibility.VISIBLE else Visibility.NONE)
                }
                if (meFeatures.isNotEmpty()) {
                    val meCollection = FeatureCollection.fromFeatures(meFeatures)
                    style.addSource(geoJsonSource(TEAM_ME_SOURCE_ID) { featureCollection(meCollection) })
                    style.addLayer(
                        circleLayer(TEAM_ME_HALO_LAYER_ID, TEAM_ME_SOURCE_ID) {
                            circleRadius(literal(ME_PULSE_AURA_START))
                            circleColor(meColor)
                            circleOpacity(0.0)
                            circleStrokeWidth(0.0)
                        }
                    )
                    style.addLayer(
                        circleLayer(TEAM_ME_INNER_LAYER_ID, TEAM_ME_SOURCE_ID) {
                            circleRadius(innerRadius)
                            circleColor(meColor)
                            circleOpacity(1.0)
                            circleStrokeWidth(0.0)
                        }
                    )
                    style.getLayer(TEAM_ME_HALO_LAYER_ID)?.visibility(if (teamLayerVisible) Visibility.VISIBLE else Visibility.NONE)
                    style.getLayer(TEAM_ME_INNER_LAYER_ID)?.visibility(if (teamLayerVisible) Visibility.VISIBLE else Visibility.NONE)
                    startMePulse()
                }
            } else {
                val collection = FeatureCollection.fromFeatures(features)
                style.addSource(geoJsonSource(TEAM_SOURCE_ID) { featureCollection(collection) })
                style.addLayer(
                    symbolLayer(TEAM_LAYER_ID, TEAM_SOURCE_ID) {
                        iconImage(get("icon"))
                        iconRotate(coalesce(get("iconRotate"), literal(0.0)))
                        iconSize(step(zoom(), literal(0.2), literal(4.0) to literal(0.2), literal(8.0) to literal(0.5), literal(12.0) to literal(1.0), literal(14.0) to literal(1.2)))
                        iconAnchor(IconAnchor.BOTTOM)
                        iconAllowOverlap(true)
                        iconIgnorePlacement(true)
                    }
                )
                style.getLayer(TEAM_LAYER_ID)?.visibility(if (teamLayerVisible) Visibility.VISIBLE else Visibility.NONE)
                scheduleWiggle()
            }
            val target = navigateTarget
            if (target != null) {
                val me = members.firstOrNull { isMe(it) }
                if (me != null) {
                    updateNavigateLineInternal(style, me.lat, me.lng, target.first, target.second)
                }
            }
        }
    }
}
