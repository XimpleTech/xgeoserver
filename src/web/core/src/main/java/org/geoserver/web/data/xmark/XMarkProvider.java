package org.geoserver.web.data.xmark;

import java.util.Arrays;
import java.util.List;

import org.geoserver.catalog.XMarkInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class XMarkProvider extends GeoServerDataProvider<XMarkInfo> {
    public static Property<XMarkInfo> NAME =
        new BeanProperty<XMarkInfo>( "name", "name" );

    public static Property<XMarkInfo> WORKSPACE =
            new BeanProperty<XMarkInfo>( "workspace", "workspace.name" );

    static List PROPERTIES = Arrays.asList(NAME, WORKSPACE);

    public XMarkProvider() {
        setSort(NAME.getName(), true);
    }

    @Override
    protected List<Property<XMarkInfo>> getProperties() {
        return PROPERTIES;
    }

    @Override
    protected List<XMarkInfo> getItems() {
        return getCatalog().getXMarks();
    }
}
