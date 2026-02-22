package com.teraim.fieldapp.dynamic.workflow_realizations.gis

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
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
import com.teraim.fieldapp.R
import com.teraim.fieldapp.dynamic.workflow_abstracts.Drawable
import com.teraim.fieldapp.utils.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
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
        /** Debug tag for map needle flow: adb logcat MapNeedle:D *:S */
        private const val MAP_NEEDLE_DEBUG = "MapNeedle"
        private const val CONTENT_FILE = "content.txt"
        /** Zoom level at which object labels become visible (e.g. ~1/3 of Sweden visible at zoom 6). */
        private const val LABEL_VISIBLE_ZOOM_LEVEL = 6.0
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

    private val layerState = mutableMapOf<String, LayerState>()
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
        addLayerOrBelowTeam(
            fillLayer(fillLayerId, sourceId) {
                filter(
                    any(
                        eq(geometryType(), literal("Polygon")),
                        eq(geometryType(), literal("MultiPolygon"))
                    )
                )
                fillColor(pystatusColorExpression(fillColorInt))
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
                    circleColor(pystatusColorExpression(fillColorInt))
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
                    iconColor(pystatusColorExpression(fillColorInt))
                    iconOpacity(fillOpacityVal.toDouble())
                    if (useOutline) {
                        iconHaloColor(lineColorInt)
                        iconHaloWidth(lineWidthVal.toDouble())
                    }
                    iconAllowOverlap(true)
                    visibility(visibility)
                    // Add text label directly to symbol layer if showLabels is true (visible when zoomed in)
                    if (spec.showLabels) {
                        textField(concat(get("TYPKOD"), literal(" "), get("OBJECTID")))
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
                    textField(concat(get("TYPKOD"), literal(" "), get("OBJECTID")))
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
        Log.d(TAG, "Added Mapbox layer: $name (polyType=${spec.polyType}, fillColor=${spec.fillColor}, showLabels=${spec.showLabels})")
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
                    val properties = feature.queriedFeature.feature.properties()
                    if (properties != null) {
                        showFeaturePropertiesDialog(properties)
                    }
                }
            }
            true
        }
    }
    
    private fun showFeaturePropertiesDialog(properties: JsonObject) {
        val context = mapView.context ?: return
        val gson = GsonBuilder().setPrettyPrinting().create()
        val formattedJson = gson.toJson(properties)
        
        AlertDialog.Builder(context)
            .setTitle("Feature Properties")
            .setMessage(formattedJson)
            .setPositiveButton("OK", null)
            .show()
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
            Log.d(MAP_NEEDLE_DEBUG, "[MapboxMapHolder.updateTeamLayer] SKIP batchId=null same list reference (avoid overwriting fresh draw)")
            return
        }
        lastTeamMembers = members
        Log.d(MAP_NEEDLE_DEBUG, "[MapboxMapHolder.updateTeamLayer] ENTRY list size=${members.size} batchId=$batchId (same list from MapTemplate: member.iconBitmap = gop.getIcon() from that run)")
        val map = mapboxMap
        if (map == null) {
            Log.w(TAG, "updateTeamLayer: mapboxMap is null, cannot add team layer (will retry when style loads)")
            return
        }
        map.getStyle { style ->
            if (style.styleLayerExists(TEAM_LAYER_ID)) style.removeStyleLayer(TEAM_LAYER_ID)
            if (style.styleSourceExists(TEAM_SOURCE_ID)) style.removeStyleSource(TEAM_SOURCE_ID)
            if (members.isEmpty()) {
                Log.d(TAG, "Team layer cleared (no members)")
                return@getStyle
            }
            // Draw "me" (current user) last so their icon is always on top when multiple markers overlap (e.g. duplicate or same location)
            val sortedMembers = members.sortedBy { if (it.name?.contains("(me)") == true) 1 else 0 }
            val meLastIndex = sortedMembers.indexOfFirst { it.name?.contains("(me)") == true }.takeIf { it >= 0 }
            Log.d(MAP_NEEDLE_DEBUG, "[MapboxMapHolder.updateTeamLayer] sorted so (me) is last: (me) at sortedIndex=$meLastIndex of ${sortedMembers.size}")
            val features = mutableListOf<Feature>()
            // Use stable icon id per member (team_<id>) so list order changes (e.g. Set iteration) don't overwrite one member's needle with another's
            for ((index, member) in sortedMembers.withIndex()) {
                val safeId = member.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val iconId = "team_$safeId"
                val bitmap = member.iconBitmap
                // Remove existing image so updated needle (e.g. after user change in settings) is shown
                if (style.hasStyleImage(iconId)) style.removeStyleImage(iconId)
                val drawableSource: String
                if (bitmap != null) {
                    style.addImage(iconId, bitmap, false)
                    drawableSource = "member.iconBitmap (= gop.getIcon() from MapTemplate)"
                } else {
                    // Use default needle icon so the map always shows a needle shape (not person_active)
                    var fallbackResName = "ic_needle_symbol"
                    val drawable = ContextCompat.getDrawable(mapView.context, R.drawable.ic_needle_symbol)
                        ?: ContextCompat.getDrawable(mapView.context, R.drawable.person_away).also { fallbackResName = "person_away" }
                    drawableSource = "fallback $fallbackResName"
                    val fallback = drawable?.let { Tools.drawableToBitmap(it) }
                    if (fallback != null) {
                        style.addImage(iconId, fallback, false)
                    } else {
                        Log.w(TAG, "Could not create fallback bitmap for team member $index (${member.name})")
                    }
                }
                Log.d(MAP_NEEDLE_DEBUG, "[MapboxMapHolder.updateTeamLayer] index=$index name=${member.name} iconBitmap=${if (bitmap != null) "non-null" else "null"} drawableSource=$drawableSource (same bitmap as MapTemplate put in from gop.getIcon())")
                val point = Point.fromLngLat(member.lng, member.lat)
                if (index < 3) {
                    Log.d(TAG, "Team feature $index: ${member.name} at lat=${member.lat}, lng=${member.lng}")
                }
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
            Log.d(TAG, "Team layer updated with ${members.size} members (layer $TEAM_LAYER_ID visible=$teamLayerVisible)")
        }
    }
}
