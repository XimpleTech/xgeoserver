package org.geoserver.web.data.xmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.XMarkInfo;
import org.geoserver.web.GeoServerApplication;

public class XMarksModel extends LoadableDetachableModel {
    @Override
    protected Object load() {
        List<XMarkInfo> styles = new ArrayList<XMarkInfo>(GeoServerApplication.get().getCatalog().getXMarks());
        Collections.sort(styles, new XMarkNameComparator());
        return styles;
    }
}
