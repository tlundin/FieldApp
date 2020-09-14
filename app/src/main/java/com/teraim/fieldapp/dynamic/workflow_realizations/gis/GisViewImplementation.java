package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import com.google.android.libraries.maps.GoogleMap;
import com.teraim.fieldapp.gis.GisImageView;

public class GisViewImplementation  {
    final GoogleMap gm;
    final GisImageView giv;
    public GisImageView getImageGis() {
        return giv;
    }
    public GoogleMap getGoogleMap() {
        return gm;
    }
    public GisViewImplementation(GoogleMap gm) {
        this.gm=gm;
        this.giv=null;
    }
    public GisViewImplementation(GisImageView giv) {
        this.giv=giv;
        this.gm=null;
    }

    public boolean isImageGis() {
        return gm == null;
    }
}
