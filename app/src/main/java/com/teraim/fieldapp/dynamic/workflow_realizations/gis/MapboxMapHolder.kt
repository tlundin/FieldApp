package com.teraim.fieldapp.dynamic.workflow_realizations.gis

import android.graphics.Color
import android.util.Log
import android.view.View
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.any
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.eq
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.geometryType
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.literal
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.teraim.fieldapp.dynamic.workflow_abstracts.Drawable
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
 * Layers are loaded from server: gis_objects/<layerName>.json
 */
class MapboxMapHolder(
    private val mapView: View,
    private val gisObjectsBaseUrl: String
) : Drawable {

    companion object {
        private const val TAG = "MapboxMapHolder"
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
            if (value != null) processPendingLayers()
        }

    private val layerState = mutableMapOf<String, LayerState>()
    private val pendingLayers = mutableListOf<PendingLayer>()
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var visible = true

    data class LayerState(
        val sourceId: String,
        val fillLayerId: String,
        val outlineLayerId: String,
        val pointLayerId: String,
        var visible: Boolean
    )

    private data class PendingLayer(
        val name: String,
        val label: String,
        val isVisible: Boolean,
        val hasWidget: Boolean,
        val showLabels: Boolean,
        val isBold: Boolean
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
        isBold: Boolean
    ) {
        if (layerState.containsKey(name)) {
            Log.d(TAG, "Layer $name already added")
            return
        }
        val pending = PendingLayer(name, label, isVisible, hasWidget, showLabels, isBold)
        val map = mapboxMap
        if (map == null) {
            synchronized(pendingLayers) { pendingLayers.add(pending) }
            Log.d(TAG, "Queued layer $name (map not ready)")
            return
        }
        addLayerInternal(map, pending)
    }

    private fun processPendingLayers() {
        val map = mapboxMap ?: return
        val toProcess = synchronized(pendingLayers) {
            pendingLayers.toList().also { pendingLayers.clear() }
        }
        toProcess.forEach { addLayerInternal(map, it) }
    }

    private fun addLayerInternal(map: MapboxMap, spec: PendingLayer) {
        scope.launch {
            val geoJson = fetchGeoJson(spec.name)
            if (geoJson == null) {
                Log.e(TAG, "Failed to load GeoJSON for layer ${spec.name}")
                return@launch
            }
            withContext(Dispatchers.Main) {
                map.getStyle { style ->
                    addLayerToStyle(style, spec, geoJson)
                }
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

    private fun addLayerToStyle(style: Style, spec: PendingLayer, geoJson: String) {
        val name = spec.name
        val featureCollection = FeatureCollection.fromJson(geoJson)
        if (featureCollection.features().isNullOrEmpty()) {
            Log.w(TAG, "GeoJSON $name has no features")
            return
        }
        val layerColor = LAYER_COLORS[abs(name.hashCode()) % LAYER_COLORS.size]
        val sourceId = "source-$name"
        val fillLayerId = "fill-$name"
        val outlineLayerId = "outline-$name"
        val pointLayerId = "point-$name"
        val visibility = if (spec.isVisible) Visibility.VISIBLE else Visibility.NONE

        if (style.styleSourceExists(sourceId)) style.removeStyleSource(sourceId)
        if (style.styleLayerExists(fillLayerId)) style.removeStyleLayer(fillLayerId)
        if (style.styleLayerExists(outlineLayerId)) style.removeStyleLayer(outlineLayerId)
        if (style.styleLayerExists(pointLayerId)) style.removeStyleLayer(pointLayerId)

        style.addSource(
            geoJsonSource(sourceId) {
                featureCollection(featureCollection)
            }
        )
        style.addLayer(
            fillLayer(fillLayerId, sourceId) {
                filter(
                    any(
                        eq(geometryType(), literal("Polygon")),
                        eq(geometryType(), literal("MultiPolygon"))
                    )
                )
                fillColor(layerColor)
                fillOpacity(0.4)
                visibility(visibility)
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
                lineWidth(1.5)
                visibility(visibility)
            }
        )
        style.addLayer(
            circleLayer(pointLayerId, sourceId) {
                filter(
                    any(
                        eq(geometryType(), literal("Point")),
                        eq(geometryType(), literal("MultiPoint"))
                    )
                )
                circleRadius(6.0)
                circleColor(layerColor)
                circleStrokeColor(Color.WHITE)
                circleStrokeWidth(1.0)
                visibility(visibility)
            }
        )
        layerState[name] = LayerState(sourceId, fillLayerId, outlineLayerId, pointLayerId, spec.isVisible)
        Log.d(TAG, "Added Mapbox layer: $name")
    }

    fun setLayerVisibility(layerName: String, visible: Boolean) {
        val state = layerState[layerName] ?: return
        state.visible = visible
        val visibility = if (visible) Visibility.VISIBLE else Visibility.NONE
        mapboxMap?.getStyle { style ->
            style.getLayer(state.fillLayerId)?.visibility(visibility)
            style.getLayer(state.outlineLayerId)?.visibility(visibility)
            style.getLayer(state.pointLayerId)?.visibility(visibility)
        }
    }

    fun getLayerNames(): List<String> = layerState.keys.sorted()
    fun isLayerVisible(layerName: String): Boolean = layerState[layerName]?.visible ?: false
}
