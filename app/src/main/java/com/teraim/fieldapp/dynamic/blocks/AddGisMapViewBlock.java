package com.teraim.fieldapp.dynamic.blocks;

import com.teraim.fieldapp.dynamic.types.GisMapView;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;

/**
 * Block that adds a GIS map view from block_add_gis_map_view.
 * Holds a GisMapView configuration; create() can instantiate the view in the container.
 */
public class AddGisMapViewBlock extends Block {

	private final GisMapView gisMapView;

	public AddGisMapViewBlock(String id, GisMapView gisMapView) {
		super();
		this.blockId = id;
		this.gisMapView = gisMapView;
	}

	public GisMapView getGisMapView() {
		return gisMapView;
	}

	public void create(WF_Context myContext) {
		// TODO: create map view in container from gisMapView config
	}
}
