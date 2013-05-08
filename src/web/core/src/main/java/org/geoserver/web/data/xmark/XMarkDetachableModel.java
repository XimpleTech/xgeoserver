package org.geoserver.web.data.xmark;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.XMarkInfo;
import org.geoserver.web.GeoServerApplication;

public class XMarkDetachableModel extends LoadableDetachableModel {

    String id;

    public XMarkDetachableModel(XMarkInfo xmark) {
        this.id = xmark.getId();
    }

    @Override
    protected Object load() {
        return GeoServerApplication.get().getCatalog().getXMark(id);
    }
}
