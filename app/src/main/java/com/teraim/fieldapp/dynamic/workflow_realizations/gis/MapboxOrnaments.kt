package com.teraim.fieldapp.dynamic.workflow_realizations.gis

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Gravity
import android.view.View
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.compass.compass
import com.teraim.fieldapp.R

/**
 * Mapbox default compass is top-right with tiny margins, which overlaps the overflow menu.
 * On map screens the toolbar is transparent and the map draws behind it, so [marginTop] is
 * simply the measured toolbar height (not screen coordinates — those race with padding changes
 * on rotation and can double-count the action bar).
 *
 * Call from [MapTemplate.onResume] after [Start.setToolbarTransparent] true.
 */
fun MapView.updateFieldAppCompassPlacement() {
    val toolbar = context.findHostActivity()?.findViewById<View>(R.id.toolbar) ?: return
    val apply = {
        val height = toolbar.height
        if (height > 0) {
            applyCompassMargins(height.toFloat())
        }
    }
    if (toolbar.height > 0) {
        apply()
    } else {
        toolbar.post { apply() }
    }
}

private fun MapView.applyCompassMargins(marginTopPx: Float) {
    val density = resources.displayMetrics.density
    compass.updateSettings {
        position = Gravity.TOP or Gravity.END
        marginTop = marginTopPx
        marginRight = 16f * density
        marginLeft = 0f
        marginBottom = 0f
        clickable = true
    }
}

private fun Context.findHostActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
