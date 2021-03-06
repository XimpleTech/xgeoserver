/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.catalog.impl.XMarkInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.Hints;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.style.XShapeMarks;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

/**
 * WMS tests utility class to set up a mocked up catalog and geoserver environment so unit tests
 * does not depend on a fully configured geoserver instance, and also they run fast due to no data
 * directory set up required.
 * 
 * @author Gabriel Roldan
 * @version $Id$
 */
public class WMSMockData {

    public static final String TEST_NS_PREFIX = "geos";

    /**
     * Namespace used for the resources in this test suite
     */
    public static final String TEST_NAMESPACE = "http://geoserver.org";

    private CatalogImpl catalog;

    private MemoryDataStore dataStore;

    private DataStoreInfo dataStoreInfo;

    private NamespaceInfoImpl namespaceInfo;

    private WorkspaceInfoImpl workspaceInfo;

    private StyleInfoImpl defaultStyle;

    private XMarkInfoImpl defaultXMark;

    private GetMapOutputFormat mockMapProducer;

    private GeoServer mockGeoServer;

    private WMS mockWMS;

    @SuppressWarnings("deprecation")
    public void setUp() throws Exception {
        mockMapProducer = new DummyRasterMapProducer();

        catalog = new CatalogImpl();

        namespaceInfo = new NamespaceInfoImpl();
        namespaceInfo.setId("testNs");
        namespaceInfo.setPrefix(TEST_NS_PREFIX);
        namespaceInfo.setURI(TEST_NAMESPACE);
        catalog.add(namespaceInfo);

        workspaceInfo = new WorkspaceInfoImpl();
        catalog.setDefaultWorkspace(workspaceInfo);

        defaultStyle = new StyleInfoImpl(catalog) {
            /**
             * Override so it does not try to load a file from disk
             */
            @Override
            public Style getStyle() throws IOException {
                StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
                Style style = styleFactory.createStyle();
                style.setName("Default Style");
                return style;
            }
        };
        defaultStyle.setFilename("defaultStyleFileName");
        defaultStyle.setId("defaultStyleId");
        defaultStyle.setName("defaultStyleName");
        catalog.add(defaultStyle);

        defaultXMark = new XMarkInfoImpl(catalog) {
            @Override
            public XShapeMarks getXMark() throws IOException {
                XShapeMarks xmarks = new XShapeMarks();
                return xmarks;
            }
        };
        defaultXMark.setFilename("defaultXMarkFileName");
        defaultXMark.setId("defaultXMarkId");
        defaultXMark.setName("defaultXMarkName");
        catalog.add(defaultXMark);

        // coverageStoreInfo = new CoverageStoreInfoImpl(catalog);
        // coverageInfo = new CoverageInfoImpl(catalog);
        // coverageLayerInfo = new LayerInfoImpl();
        // // setUpBasicTestCoverage(coverageStoreInfo, coverageInfo, coverageLayerInfo);
        //
        // coverageInfoOldApi = new CoverageInfo(coverageLayerInfo, catalog);

        dataStoreInfo = new DataStoreInfoImpl(catalog);
        dataStoreInfo.setName("mockDataStore");
        dataStoreInfo.setEnabled(true);
        dataStoreInfo.setWorkspace(workspaceInfo);

        dataStore = new MemoryDataStore();
        ResourcePool resourcePool = new ResourcePool(catalog) {
            @Override
            public DataStore getDataStore(DataStoreInfo info) throws IOException {
                return dataStore;
            }
        };
        catalog.setResourcePool(resourcePool);

        mockGeoServer = new GeoServerImpl();
        mockGeoServer.setCatalog(catalog);

        GeoServerInfoImpl geoserverInfo = new GeoServerInfoImpl(mockGeoServer);
        geoserverInfo.setId("geoserver");
        mockGeoServer.setGlobal(geoserverInfo);

        WMSInfoImpl wmsInfo = new WMSInfoImpl();
        wmsInfo.setId("wms");
        wmsInfo.setName("WMS");
        wmsInfo.setEnabled(true);
        mockGeoServer.add(wmsInfo);

        mockWMS = new WMS(mockGeoServer);
    }

    public WMS getWMS() {
        return mockWMS;
    }

    /**
     * This dummy producer adds no functionality to DefaultRasterMapProducer, just implements a void
     * formatImageOutputStream
     * 
     * @author Gabriel Roldan
     * @version $Id$
     */
    public static class DummyRasterMapProducer extends Response implements GetMapOutputFormat {

        public static final String MIME_TYPE = "image/dummy";

        public boolean produceMapCalled;

        public String outputFormat;

        public boolean writeToCalled;

        public DummyRasterMapProducer() {
            super(WebMap.class);

        }

        /**
         * @see org.geoserver.wms.GetMapOutputFormat#getOutputFormatNames()
         */
        public Set<String> getOutputFormatNames() {
            return Collections.singleton(MIME_TYPE);
        }

        /**
         * @see org.geoserver.wms.GetMapOutputFormat#getMimeType()
         */
        public String getMimeType() {
            return MIME_TYPE;
        }

        /**
         * @see org.geoserver.wms.map.RasterMapOutputFormat#produceMap(org.geoserver.wms.WMSMapContent)
         */
        public WebMap produceMap(WMSMapContent mapContent) throws ServiceException, IOException {
            produceMapCalled = true;
            return new WebMap(mapContent) {
            };
        }

        /**
         * @see org.geoserver.ows.Response#getMimeType(java.lang.Object,
         *      org.geoserver.platform.Operation)
         */
        @Override
        public String getMimeType(Object value, Operation operation) throws ServiceException {
            return MIME_TYPE;
        }

        /**
         * @see org.geoserver.ows.Response#write(java.lang.Object, java.io.OutputStream,
         *      org.geoserver.platform.Operation)
         */
        @Override
        public void write(Object value, OutputStream output, Operation operation)
                throws IOException, ServiceException {
        }

        public MapProducerCapabilities getCapabilities(String format) {
            return new MapProducerCapabilities(true, true, true, true, MIME_TYPE);
        }

    }

    public StyleInfo getDefaultStyle() {
        return defaultStyle;
    }

    public GetMapRequest createRequest() {
        GetMapRequest request;

        request = new GetMapRequest();
        request.setFormat(DummyRasterMapProducer.MIME_TYPE);
        request.setWidth(512);
        request.setHeight(256);
        Envelope envelope = new Envelope(-180, 180, -90, 90);
        request.setBbox(envelope);
        request.setSRS("EPSG:4326");
        request.setCrs(DefaultGeographicCRS.WGS84);
        try {
            request.setStyles(Collections.singletonList(defaultStyle.getStyle()));
        } catch (IOException e) {
            throw new RuntimeException("shouldn't happen", e);
        }
        request.setRawKvp(new HashMap<String, String>());
        request.setBaseUrl("http://example.geoserver.org/geoserver");

        return request;
    }

    /**
     * Creates a vector layer with associated FeatureType in the internal MemoryDataStore with the
     * given type and two attributes: name:String and geom:geometryType
     */
    public MapLayerInfo addFeatureTypeLayer(final String name,
            Class<? extends Geometry> geometryType) throws IOException {

        final DataStore dataStore = this.dataStore;
        FeatureTypeInfoImpl featureTypeInfo = new FeatureTypeInfoImpl(catalog) {
            /**
             * Override to avoid going down to the catalog and geoserver resource loader etc
             */
            @Override
            public FeatureSource getFeatureSource(ProgressListener listener, Hints hints) {
                try {
                    return dataStore.getFeatureSource(getQualifiedName());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        featureTypeInfo.setName(name);
        featureTypeInfo.setNativeName(name);
        featureTypeInfo.setEnabled(true);

        final DefaultGeographicCRS wgs84 = DefaultGeographicCRS.WGS84;

        ReferencedEnvelope bbox = new ReferencedEnvelope(-180, 180, -90, 90, wgs84);
        featureTypeInfo.setLatLonBoundingBox(bbox);
        featureTypeInfo.setNamespace(namespaceInfo);
        featureTypeInfo.setNativeBoundingBox(bbox);
        featureTypeInfo.setNativeCRS(wgs84);
        featureTypeInfo.setSRS("EPSG:4326");
        featureTypeInfo.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
        featureTypeInfo.setStore(dataStoreInfo);
        catalog.add(featureTypeInfo);

        LayerInfo layerInfo = new LayerInfoImpl();
        layerInfo.setResource(featureTypeInfo);
        layerInfo.setName(name);
        layerInfo.setEnabled(true);
        layerInfo.setDefaultStyle(defaultStyle);
        layerInfo.setType(LayerInfo.Type.VECTOR);
        catalog.add(layerInfo);

        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.setNamespaceURI(TEST_NAMESPACE);
        ftb.setName(name);
        ftb.add("name", String.class);
        ftb.add("geom", geometryType, wgs84);
        SimpleFeatureType featureType = ftb.buildFeatureType();
        dataStore.createSchema(featureType);

        return new MapLayerInfo(layerInfo);
    }

    public SimpleFeature addFeature(final SimpleFeatureType featureType, final Object[] values)
            throws IOException, ParseException {
        SimpleFeatureStore fs;
        fs = (SimpleFeatureStore) dataStore.getFeatureSource(featureType.getName());

        SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(featureType);
        sfb.addAll(values);
        SimpleFeature feature = sfb.buildFeature(null);
        fs.addFeatures(DataUtilities.collection(feature));

        return feature;
    }

    public GeoServer getGeoServer() {
        return this.mockGeoServer;
    }

}
