package org.geoserver.web.data.xmark;

import java.util.Comparator;

import org.geoserver.catalog.XMarkInfo;

public class XMarkNameComparator implements Comparator<XMarkInfo> {
    @Override
    public int compare(XMarkInfo xMarkInfo, XMarkInfo xMarkInfo2) {
        return xMarkInfo.getName().compareToIgnoreCase(xMarkInfo2.getName());
    }
}
