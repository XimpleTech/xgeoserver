package com.ximple.eofms.geoserver.batch.job;

import javax.servlet.ServletContext;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.xmlrules.DigesterLoader;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.TransformException;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleTrigger;
import org.springframework.web.context.ContextLoader;
import org.vfny.geoserver.global.ConfigurationException;
import org.vfny.geoserver.global.GeoserverDataDirectory;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import com.ximple.eofms.geoserver.config.XGeosDataConfig;
import com.ximple.eofms.geoserver.config.XGeosDataConfigMapping;
import com.ximple.eofms.jobs.DataReposVersionManager;
import com.ximple.eofms.util.PrintfFormat;

public class XGeoResetDataConfigJob extends GeoserverConfigJobBean {
    protected static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("com.ximple.eofms.geoserver.batch");

    private static final String SKIPCONFIGJOB = "SKIPCONFIGJOB";
    private static final String MASTERMODE = "MASTERMODE";
    private static final String EPSG = "EPSG:";
    private static final String DEFAULTNAMESPACE = "xtpc";
    private static final String XGEOSDATACONFIG_PATH = "xgeosdataconfig.xml";
    private static final String XGEOSRULES_NAME = "DefaultXGeosDataConfigRules.xml";

    // private static final int MAGIC_BLOCKSIZE = (64 * 1024 * 1024) - (32 * 1024);

    private static SimpleTriggerControl simpleControl = new SimpleTriggerControl();
    private static final String QUERY_VIEWDEFSQL = "SELECT table_name, view_definition FROM information_schema.views " +
        "WHERE table_schema = ? AND table_name LIKE ";

    private static final String CREATE_VIEWSQL = "CREATE OR REPLACE VIEW \"%s\" AS SELECT * FROM \"%s\".\"%s\"";
    private static final String EXTRAWHERE_VIEWSQL = " WHERE \"%s\".level = %s AND \"%s\".symweight = %s";

    private static final String ALTER_VIEWSQL = "ALTER TABLE \"%s\" OWNER TO ";
    // private static final String GRANT_VIEWSQL = "GRANT SELECT ON TABLE \"%s\" TO public";
    private static final int SRSID_TWD97_ZONE119 = 3825;
    private static final int SRSID_TWD97_ZONE121 = 3826;

    private static XGeosDataConfigMapping xgeosDataConfigMapping = null;

    private static Date lastUpdate;

    public static SimpleTriggerControl getTriggerControl() {
        return simpleControl;
    }

    public XGeoResetDataConfigJob() {
        simpleControl.setAutoReset(true);
    }

    protected XGeosDataConfigMapping getConfigMapping() {
        if (xgeosDataConfigMapping == null) {
            URL rulesURL = XGeosDataConfigMapping.class.getResource(XGEOSRULES_NAME);
            assert rulesURL != null;
            Digester digester = DigesterLoader.createDigester(rulesURL);
            File rootDir = GeoserverDataDirectory.getGeoserverDataDirectory();
            File xfmsConfigDir;

            try {
                xfmsConfigDir = GeoserverDataDirectory.findConfigDir(rootDir, "xgsjobs");
            } catch (ConfigurationException cfe) {
                LOGGER.warning("no xgsjobs dir found, creating new one");
                //if for some bizarre reason we don't fine the dir, make a new one.
                xfmsConfigDir = new File(rootDir, "xgsjobs");
            }

            File xfmsConfigFile = new File(xfmsConfigDir, XGEOSDATACONFIG_PATH);
            try {
                xgeosDataConfigMapping = (XGeosDataConfigMapping) digester.parse(xfmsConfigFile);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            } catch (SAXException e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        }
        return xgeosDataConfigMapping;
    }

    protected void executeInternal(JobExecutionContext executionContext) throws JobExecutionException {
        if (LOGGER.isLoggable(Level.FINER))
            LOGGER.finer("XGeoResetDataConfigJob execute.");

        if (executionContext.getTrigger() instanceof SimpleTrigger) {
            if ((!simpleControl.isSkipNextTime()) && (!simpleControl.isRunning())) {
                try {
                    simpleControl.setRunning();
                    doExecuteInternal(executionContext);
                } finally {
                    simpleControl.reset();
                }
            } else {
                if (simpleControl.isRunning())
                    LOGGER.fine("Job is Running. so skip this time.");
            }
        } else {
            boolean skipConvert = isSkipConvertJob(executionContext);
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info("isSkipConvert=" + skipConvert);
            }
            if ((!skipConvert) && (!simpleControl.isRunning())) {
                try {
                    simpleControl.setRunning();
                    doExecuteInternal(executionContext);
                } finally {
                    simpleControl.reset();
                }
            } else {
                if (simpleControl.isRunning())
                    LOGGER.info("Job is Running. so skip this time.");
            }
        }
        if (LOGGER.isLoggable(Level.FINER))
            LOGGER.finer("XGeoResetDataConfigJob execute completed.");
    }

    private boolean isSkipConvertJob(JobExecutionContext context) {
        String strSkipValue = context.getJobDetail().getJobDataMap().getString(SKIPCONFIGJOB);
        return strSkipValue == null || (!strSkipValue.equalsIgnoreCase("false") &&
            !strSkipValue.equalsIgnoreCase("no") && !strSkipValue.equalsIgnoreCase("0"));
    }

    private boolean isMasterMode(JobExecutionContext context) {
        String strSkipValue = context.getJobDetail().getJobDataMap().getString(MASTERMODE);
        return strSkipValue == null || (!strSkipValue.equalsIgnoreCase("false") &&
            !strSkipValue.equalsIgnoreCase("no") && !strSkipValue.equalsIgnoreCase("0"));
    }

    protected void doExecuteInternal(JobExecutionContext executionContext)
        throws JobExecutionException {
        if (executionContext == null)
            return;

        if (LOGGER.isLoggable(Level.INFO))
            LOGGER.info("XGeoResetDataConfigJob internal execute begin.");

        boolean masterMode = isMasterMode(executionContext);
        GeoServer geoServer = getGeosServer(executionContext);
        Catalog catalog = geoServer.getCatalog();
        CatalogBuilder catalogBuilder = new CatalogBuilder(catalog);

        // DataStoreInfo targetDataStoreInfo = catalog.getDataStoreByName("pgDMMS");

        try {
            DataStoreInfo targetDataStoreInfo = null;
            JDBCDataStore jdbcDataStore = null;
            List<DataStoreInfo> dataStoreInfos = catalog.getDataStores();
            for (DataStoreInfo storeInfo : dataStoreInfos) {
                if (storeInfo.getName().equalsIgnoreCase("pgDMMS")) {
                    Map params = storeInfo.getConnectionParameters();
                    if (params.get("dbtype").equals("postgis")) {
                        String ownerName = (String) params.get("user");
                        DataAccess access = storeInfo.getDataStore(null);
                        if (access instanceof JDBCDataStore) {
                            jdbcDataStore = (JDBCDataStore) access;
                        }
                        targetDataStoreInfo = storeInfo;
                        LOGGER.info("found pgDMMS");
                    }
                }
            }

            if (masterMode) {
                if (LOGGER.isLoggable(Level.INFO))
                    LOGGER.info("XGeoResetDataConfigJob into MASTERMODE.");
                DataAccess<? extends FeatureType, ? extends Feature> a = targetDataStoreInfo.getDataStore(null);
                catalogBuilder.setStore(targetDataStoreInfo);

                // MASTER NODE MODE
                resetGeoServerLayerConfiguration(executionContext, catalogBuilder, catalog, jdbcDataStore, targetDataStoreInfo);
                resetGeoserverDataState(executionContext, jdbcDataStore,
                    DataReposVersionManager.VSSTATUS_LINKVIEW,
                    DataReposVersionManager.VSSTATUS_CONFIG, false);

                Map<String, String> layerMapping = rebuildGeoserverLayerGroupMapping(catalog, masterMode);
                if (layerMapping != null) {
                    resetGeoServerLayerGroupConfiguration(executionContext, catalogBuilder, catalog, jdbcDataStore,
                        layerMapping, masterMode, targetDataStoreInfo);
                    resetGeoserverDataState(executionContext, jdbcDataStore,
                        DataReposVersionManager.VSSTATUS_CONFIG,
                        DataReposVersionManager.VSSTATUS_USING, true);
                } else {
                    LOGGER.info("rebuildGeoserverLayerGroupMapping is null.");
                }
                lastUpdate = Calendar.getInstance().getTime();
            } else {
                if (LOGGER.isLoggable(Level.INFO))
                    LOGGER.info("XGeoResetDataConfigJob into SALAVEMODE.");
                // SALAVE NODE MODE

                // TODO: Salave Node
                // boolean needReset = syncFeatureTypesMapping(executionContext, catalog, targetDataStoreInfo);
                boolean needReset = false;
                if (needReset) {
                    geoServer.reload();
                    lastUpdate = new Date(System.currentTimeMillis());
                }

                if (lastUpdate == null) {
                    lastUpdate = new Date(System.currentTimeMillis());
                }
            }

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }

        if (LOGGER.isLoggable(Level.INFO))
            LOGGER.info("XGeoResetDataConfigJob internal execute completed.");
    }

    /**
     * 重新建立所有重新建立所有PostGIS中的資料庫視景
     *
     * @param executionContext 批次執行的關係
     * @param dsConf           Geoserver的資料儲存連結
     * @param ownerName        資料庫視景擁有者名稱
     */
    /*
    private void resetPostgisViewMapping(JobExecutionContext executionContext, DataStoreInfo dsConf, String ownerName) {
        assert executionContext != null;
        ServletContext servletContext = ContextLoader.getCurrentWebApplicationContext().getServletContext();
        DataStore dataStore = null;
        try {
            dataStore = dsConf.getDataStore().findDataStore(servletContext);
            if (dataStore instanceof PostgisDataStore) {
                PostgisDataStore pgDataStore = (PostgisDataStore) dataStore;
                DataSource dataSource = pgDataStore.getDataSource();
                Connection connection = dataSource.getConnection();

                String currentTargetSchema = retrieveCurrentSchemaName(connection,
                    DataReposVersionManager.VSSTATUS_READY);
                if (currentTargetSchema == null) {
                    LOGGER.info("Cannot found schema that status is VSSTATUS_READY[" +
                        DataReposVersionManager.VSSTATUS_READY + "]");
                    return;
                }

                ArrayList<String> realTableNames = new ArrayList<String>();
                retrieveAllRealTableName(connection, currentTargetSchema, realTableNames);

                HashMap<String, String> viewDefs = retrieveViewDef(connection, "public", "fsc%");
                HashMap<String, String> tempViewDefs = retrieveViewDef(connection, "public", "indexshape%");
                viewDefs.putAll(tempViewDefs);
                tempViewDefs = viewDefs = retrieveViewDef(connection, "public", "lndtpc%");
                viewDefs.putAll(tempViewDefs);

                for (String tableName : realTableNames) {
                    resetPostgisDataView(connection, viewDefs, ownerName, currentTargetSchema, tableName);
                }

                resetExtraPostgisDataView(connection, ownerName, currentTargetSchema, realTableNames);

                updateCurrentRepositoryStatus(connection, currentTargetSchema,
                    DataReposVersionManager.VSSTATUS_LINKVIEW);

                String[] featureNames = dataStore.getTypeNames();
                LOGGER.info("featureNames[] size = " + featureNames.length);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        } finally {
            if (dataStore != null) dataStore.dispose();
        }
    }

    private void retrieveAllRealTableName(Connection connection, String targetSchema,
                                          ArrayList<String> realTableNames) throws SQLException {
        ResultSet rsMeta = null;
        try {
            rsMeta = connection.getMetaData().getTables("", targetSchema, "fsc%", new String[]{"TABLE"});
            while (rsMeta.next()) {
                String tableName = rsMeta.getString(3);
                realTableNames.add(tableName);
            }
            rsMeta.close();
            rsMeta = null;

            rsMeta = connection.getMetaData().getTables("", targetSchema, "index%", new String[]{"TABLE"});
            while (rsMeta.next()) {
                String tableName = rsMeta.getString(3);
                realTableNames.add(tableName);
            }
            rsMeta.close();
            rsMeta = null;

            rsMeta = connection.getMetaData().getTables("", targetSchema, "lndtpc%", new String[]{"TABLE"});
            while (rsMeta.next()) {
                String tableName = rsMeta.getString(3);
                realTableNames.add(tableName);
            }
        } finally {
            if (rsMeta != null) rsMeta.close();
        }
    }

    private void resetPostgisDataView(Connection connection, HashMap<String, String> viewDefs,
                                      String ownerName, String schemaName, String tableName) throws SQLException {
        String[] splits = tableName.split("-");
        if (splits.length > 3) {
            // feature table

            StringBuilder viewBuilder = new StringBuilder();
            viewBuilder.append(splits[0]);
            viewBuilder.append('-');
            viewBuilder.append(splits[1]);
            viewBuilder.append('-');
            viewBuilder.append(splits[2]);
            viewBuilder.append(splits[3]);
            String viewName = viewBuilder.toString();
            if (viewDefs.containsKey(viewName)) {
                String viewDef = viewDefs.get(viewName);
                int pos = viewDef.indexOf("FROM");
                String subView = viewDef.substring(pos + 4);
                // String[] viewSources = subView.split("\\.");
                String[] viewSources = subView.split("(\\.\"|\")");
                if (!viewSources[0].equalsIgnoreCase(schemaName)) {
                    createOrReplaceView(connection, schemaName, tableName, viewName, ownerName);
                }
            } else {
                createOrReplaceView(connection, schemaName, tableName, viewName, ownerName);
            }

        } else {

            splits = tableName.split("_");
            if (splits.length > 0) {
                StringBuilder viewBuilder = new StringBuilder();
                viewBuilder.append(splits[0]);
                if (splits.length > 1) viewBuilder.append(splits[1]);
                if (splits.length > 2) viewBuilder.append(splits[2]);
                String viewName = viewBuilder.toString();
                if (viewDefs.containsKey(viewName)) {
                    String viewDef = viewDefs.get(viewName);
                    int pos = viewDef.indexOf("FROM");
                    String subView = viewDef.substring(pos + 4);
                    String[] viewSources = subView.split("(\\.\"|\")");
                    if (!viewSources[0].equalsIgnoreCase(schemaName)) {
                        createOrReplaceView(connection, schemaName, tableName, viewName, ownerName);
                    }
                } else {
                    createOrReplaceView(connection, schemaName, tableName, viewName, ownerName);
                }
            }
        }
    }

    private void resetExtraPostgisDataView(Connection connection, String ownerName, String currentSchema,
                                           ArrayList<String> realTableNames) {
        try {
            // ArrayList<String> extraViewNames = new ArrayList<String>();
            XGeosDataConfigMapping configMapping = getConfigMapping();
            MultiMap configMultiMap = configMapping.getMapping();
            for (Object key : configMultiMap.keySet()) {
                List values = (List) configMultiMap.get(key);
                for (Object value : values) {
                    XGeosDataConfig xgeosConfig = (XGeosDataConfig) value;
                    short tid = xgeosConfig.getFSC();
                    short cid = xgeosConfig.getCOMP();
                    StringBuilder sbTable = new StringBuilder("fsc-");
                    sbTable.append(tid).append("-c-");
                    sbTable.append(cid);
                    int index = realTableNames.indexOf(sbTable.toString());
                    if (index == -1) {
                        LOGGER.fine("Cannot found-" + xgeosConfig.toString());
                        continue;
                    }
                    StringBuilder sbView = new StringBuilder("fsc-");
                    sbView.append(tid).append("-c");
                    sbView.append(cid).append("-l");
                    sbView.append(xgeosConfig.getLEV()).append("-w");
                    sbView.append(xgeosConfig.getWEIGHT());
                    // extraViewNames.add(sbView.toString());

                    createOrReplaceExtraView(connection, currentSchema, sbTable.toString(), sbView.toString(),
                        ownerName, xgeosConfig);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
    }
    */

    private HashMap<String, String> retrieveViewDef(Connection connection, String schemaName, String tablePattern) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(QUERY_VIEWDEFSQL + "'" + tablePattern + "'");
        stmt.setString(1, schemaName);
        // stmt.setString(2, tablePattern);
        HashMap<String, String> result = new HashMap<String, String>();
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            String tableName = rs.getString(1);
            String viewDef = rs.getString(2);
            result.put(tableName, viewDef);
        }
        rs.close();
        stmt.close();
        return result;
    }

    private void createOrReplaceView(Connection connection, String schemaName, String tableName, String viewName,
                                     String ownerName) throws SQLException {
        PrintfFormat pf = new PrintfFormat(CREATE_VIEWSQL);
        String sql = pf.sprintf(new Object[]{viewName, schemaName, tableName});
        Statement stmt = connection.createStatement();
        stmt.execute(sql);

        pf = new PrintfFormat(ALTER_VIEWSQL + ownerName);
        sql = pf.sprintf(viewName);
        stmt.execute(sql);
        stmt.close();
        connection.commit();
    }

    private void createOrReplaceExtraView(Connection connection, String schemaName, String tableName, String viewName,
                                          String ownerName, XGeosDataConfig xgeosConfig) throws SQLException {
        PrintfFormat pf = new PrintfFormat(CREATE_VIEWSQL);
        String sql = pf.sprintf(new Object[]{viewName, schemaName, tableName});

        PrintfFormat pfWhere = new PrintfFormat(EXTRAWHERE_VIEWSQL);
        sql += pfWhere.sprintf(new String[]{tableName, Short.toString(xgeosConfig.getLEV()),
            tableName, Short.toString(xgeosConfig.getWEIGHT())});

        Statement stmt = connection.createStatement();
        stmt.execute(sql);

        pf = new PrintfFormat(ALTER_VIEWSQL + ownerName);
        sql = pf.sprintf(viewName);
        stmt.execute(sql);
        stmt.close();
        connection.commit();
    }

    public static final String SEPARATOR = ":::";

    private void resetGeoServerLayerConfiguration(JobExecutionContext executionContext,
                                                  CatalogBuilder catalogBuilder, Catalog catalog,
                                                  JDBCDataStore dataStore, DataStoreInfo targetStoreInfo)
        throws IOException, JobExecutionException {
        assert executionContext != null;


        ServletContext servletContext = ContextLoader.getCurrentWebApplicationContext().getServletContext();
        if (!checkCurrentRepositoryStatus(dataStore, DataReposVersionManager.VSSTATUS_LINKVIEW)) {
            return;
        }

        // List<StyleInfo> styles = catalog.getStyles();
        List<StyleInfo> styles = catalog.getStylesByWorkspace(targetStoreInfo.getWorkspace());

        XGeosDataConfigMapping mapping = getConfigMapping();
        HashMap<String, String> defaultStyles = buildDefaultStylesMapping(mapping);
        Set<String> dynamicColorLayerNames = buildDynamicColorLayerNames(mapping, "pgOMS");
        Connection connection = null;
        try {
            // connection = dataStore.getConnection(Transaction.AUTO_COMMIT);
            String[] dsFTypeNames = dataStore.getTypeNames();
            for (String featureTypeName : dsFTypeNames) {
                // String ftKey = targetStoreInfo.getId() + SEPARATOR + featureTypeName;
                // String ftKey = dsConf.getId() + DataConfig.SEPARATOR + featureTypeName;

                catalog.getLayerGroupsByWorkspace(DEFAULTNAMESPACE);
                FeatureTypeInfo ftInfo = catalog.getFeatureTypeByDataStore(targetStoreInfo, featureTypeName);

                boolean isDynamicLayer = dynamicColorLayerNames.contains(featureTypeName);
                if (ftInfo == null) {
                    if (!createLayerFeatureTypeInfo(catalogBuilder, catalog, targetStoreInfo, dataStore,
                            styles, featureTypeName, defaultStyles, isDynamicLayer)) {
                        LOGGER.info("Create Feature Failed. [" + featureTypeName + "]");
                    }
                } else {
                    // updateFeatureTypeConfig(ftInfo, dataStore, styles, defaultStyles);
                }
            }
        } finally {
            /*
            if ((connection !=null) && (dataStore != null)) {
                dataStore.closeSafe(connection);
            }
            */
            // if (dataStore != null) dataStore.dispose();
        }
    }

    private boolean setupFeatureTypeBounds(FeatureTypeInfo featureType, SimpleFeatureSource featureSource) throws IOException {
        if (featureType.getNativeBoundingBox() == null) {
            ReferencedEnvelope bounds = getFeatureTypeNativeBounds(featureType, featureSource);
            if (bounds == null) return false;
            featureType.setNativeBoundingBox(bounds);
        }

        // setup the geographic bbox if missing and we have enough info
        featureType.setLatLonBoundingBox(getFeatureTypeLatLonBounds(featureType.getNativeBoundingBox(), featureType.getCRS()));
        return true;
    }

    private ReferencedEnvelope getFeatureTypeLatLonBounds(ReferencedEnvelope nativeBounds,
                                                          CoordinateReferenceSystem declaredCRS) throws IOException {
        if (nativeBounds != null && declaredCRS != null) {
            // make sure we use the declared CRS, not the native one, the may differ
            if (!CRS.equalsIgnoreMetadata(DefaultGeographicCRS.WGS84, declaredCRS)) {
                // transform
                try {
                    ReferencedEnvelope bounds = new ReferencedEnvelope(nativeBounds, declaredCRS);
                    return bounds.transform(DefaultGeographicCRS.WGS84, true);
                } catch (Exception e) {
                    throw (IOException) new IOException("transform error").initCause(e);
                }
            } else {
                return new ReferencedEnvelope(nativeBounds, DefaultGeographicCRS.WGS84);
            }
        }
        return null;
    }

    boolean buildEnvelopeAggregates(SimpleFeatureType featureType, StringBuffer sql) {
        boolean hasGeom = false;
        //walk through all geometry attributes and build the query
        for (Iterator a = featureType.getAttributeDescriptors().iterator(); a.hasNext();) {
            AttributeDescriptor attribute = (AttributeDescriptor) a.next();
            if (attribute instanceof GeometryDescriptor) {
                String geometryColumn = attribute.getLocalName();
                sql.append("ST_AsText(ST_Force_2D(ST_Envelope(");
                sql.append("ST_Extent(\"" + geometryColumn + "\"::geometry))))");
                sql.append(",");
                hasGeom = true;
            }
        }
        sql.setLength(sql.length() - 1);
        return hasGeom;
    }

    public Envelope decodeGeometryEnvelope(ResultSet rs, int column,
            Connection cx) throws SQLException, IOException {
        try {
            String envelope = rs.getString(column);
            if (envelope != null)
                return new WKTReader().read(envelope).getEnvelopeInternal();
            else
                // empty one
                return new Envelope();
        } catch (ParseException e) {
            throw (IOException) new IOException(
                    "Error occurred parsing the bounds WKT").initCause(e);
        }
    }

    private ReferencedEnvelope mergeEnvelope(ReferencedEnvelope base, ReferencedEnvelope merge)
                        throws TransformException, FactoryException {
        if(base == null || base.isNull()) {
            return merge;
        } else if(merge == null || merge.isNull()) {
            return base;
        } else {
            // reproject and merge
            final CoordinateReferenceSystem crsBase = base.getCoordinateReferenceSystem();
            final CoordinateReferenceSystem crsMerge = merge.getCoordinateReferenceSystem();
            if(crsBase == null) {
                merge.expandToInclude(base);
                return merge;
            } else if(crsMerge == null) {
                base.expandToInclude(base);
                return base;
            } else {
                // both not null, are they equal?
                if(!CRS.equalsIgnoreMetadata(crsBase, crsMerge)) {
                    merge = merge.transform(crsBase, true);
                }
                base.expandToInclude(merge);
                return base;
            }
        }
    }

    private ReferencedEnvelope getFeatureTypeNativeBounds(FeatureTypeInfo featureTypeInfo, SimpleFeatureSource data)
        throws IOException {
        ReferencedEnvelope bounds = null;
        // bounds
        JDBCDataStore dataStore = (JDBCDataStore) data.getDataStore();
        SimpleFeatureType ftype = data.getSchema();
        StringBuffer sb = new StringBuffer();
        sb.append("SELECT ");
        if (!buildEnvelopeAggregates(ftype, sb)) {
            return null;
        }
        sb.append(" FROM ");
        sb.append("\"" + ftype.getTypeName() + "\"");

        String sql = sb.toString();

        Connection cx = dataStore.getConnection(Transaction.AUTO_COMMIT);
        Statement st = null;
        ResultSet rs = null;
        try {
            st = cx.createStatement();
            rs = st.executeQuery(sql);

            CoordinateReferenceSystem flatCRS = CRS.getHorizontalCRS(ftype
                .getCoordinateReferenceSystem());
            final int columns = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                for (int i = 1; i <= columns; i++) {
                    final Envelope envelope = decodeGeometryEnvelope(rs, i, st
                        .getConnection());
                    if (envelope != null) {
                        if (envelope instanceof ReferencedEnvelope) {
                            bounds = mergeEnvelope(bounds, (ReferencedEnvelope) envelope);
                        } else {
                            bounds = mergeEnvelope(bounds, new ReferencedEnvelope(envelope,
                                    flatCRS));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        } catch (TransformException e) {
            throw new IOException(e.getMessage(), e);
        } catch (FactoryException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            dataStore.closeSafe(rs);
            dataStore.closeSafe(st);
        }

        // fix the native bounds if necessary, some datastores do
        // not build a proper referenced envelope
        CoordinateReferenceSystem crs = featureTypeInfo.getNativeCRS();
        if (bounds != null && bounds.getCoordinateReferenceSystem() == null && crs != null) {
            bounds = new ReferencedEnvelope(bounds, crs);
        }

        if (bounds != null) {
            // expansion factor if the bounds are empty or one dimensional
            double expandBy = 1; // 1 meter
            if (bounds.getCoordinateReferenceSystem() instanceof GeographicCRS) {
                expandBy = 0.0001;
            }
            if (bounds.getWidth() == 0 || bounds.getHeight() == 0) {
                bounds.expandBy(expandBy);
            }
        }
        return bounds;
    }

    private boolean createLayerFeatureTypeInfo(CatalogBuilder catalogBuilder, Catalog catalog,
                                               DataStoreInfo dsInfo, DataStore dataStore,
                                               List<StyleInfo> styles, String featureTypeName,
                                               HashMap<String, String> defaultStyles, boolean isDynamicLayer)
        throws IOException, JobExecutionException {

        FeatureTypeInfo featureType =
                catalogBuilder.buildFeatureType(dataStore.getFeatureSource(featureTypeName));
        featureType.setStore(null);
        featureType.setNamespace(null);

        SimpleFeatureSource featureSource = dataStore.getFeatureSource(featureTypeName);
        // SELECT ST_AsText(ST_force_2d(ST_Envelope(ST_Extent(geom)))) from "fsc-501-c1-l2-w1";
        catalogBuilder.setupBounds(featureType, featureSource);
        // if (!setupFeatureTypeBounds(featureType, featureSource)) return false;

        String featureTypeNameDyn = featureTypeName + "-dyn";
        FeatureTypeInfo dynFeatureType = null;
        if (isDynamicLayer) {
            dynFeatureType = catalogBuilder.buildFeatureType(dataStore.getFeatureSource(featureTypeName));
            dynFeatureType.setStore(null);
            dynFeatureType.setNamespace(null);
            dynFeatureType.setNativeBoundingBox(new ReferencedEnvelope(featureType.getNativeBoundingBox()));
            dynFeatureType.setLatLonBoundingBox(new ReferencedEnvelope(featureType.getLatLonBoundingBox()));
        }

        //add attributes
        CatalogFactory factory = catalog.getFactory();
        SimpleFeatureType schema = featureSource.getSchema();
        for (AttributeDescriptor ad : schema.getAttributeDescriptors()) {
            AttributeTypeInfo att = factory.createAttribute();
            att.setName(ad.getLocalName());
            att.setBinding(ad.getType().getBinding());
            featureType.getAttributes().add(att);
        }

        LayerInfo layer = catalogBuilder.buildLayer((ResourceInfo)featureType);
        layer.setName(featureTypeName);
        ResourceInfo r = layer.getResource();
        LayerInfo dynamicLayer = null;
        ResourceInfo dr = null;
        if (isDynamicLayer) {
            dynamicLayer = catalogBuilder.buildLayer((ResourceInfo)dynFeatureType);
            dynamicLayer.setName(featureTypeNameDyn);
            dr = dynamicLayer.getResource();
        }

        //initialize resource references
        r.setStore(dsInfo);
        r.setName(featureTypeName);
        r.setNamespace(
            catalog.getNamespaceByPrefix(dsInfo.getWorkspace().getName()));
        if (dr != null) {
            dr.setStore(dsInfo);
            dr.setName(featureTypeNameDyn);
            dr.setNamespace(
                catalog.getNamespaceByPrefix(dsInfo.getWorkspace().getName()));
        }

        //srs
        if (r.getSRS() == null) {
            LOGGER.info("featureType NO_CRS:" + featureTypeName);
            return false;
        }
        else {
            //changed after setting srs manually, compute the lat long bounding box
            try {
                computeLatLonBoundingBox(layer, false);
                if (dynamicLayer != null) {
                    computeLatLonBoundingBox(dynamicLayer, false);
                }
            }
            catch(Exception e) {
                LOGGER.log(Level.WARNING, "Error computing lat long bounding box", e);
                return false;
            }

            //also since this resource has no native crs set the project policy to force declared
            layer.getResource().setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
            if (dynamicLayer != null) {
                dynamicLayer.getResource().setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
            }
        }

        //bounds
        if (r.getNativeBoundingBox() == null) {
            LOGGER.info("NO BOUNDS:" + featureTypeName);
            return false;
        }


        String styleName = getDefaultFeatureTypeStyleId(styles, defaultStyles, featureType.getFeatureType());
        StyleInfo styleInfo = catalog.getStyleByName(DEFAULTNAMESPACE, styleName);
        if (styleInfo == null) {
            styleInfo = catalog.getStyleByName(styleName);
        }

        if (styleInfo == null) {
            LOGGER.warning("featureType NO_STYLE:" + featureTypeName + "-" + styleName);
            return false;
        }
        layer.setDefaultStyle(styleInfo);

        if (dynamicLayer != null) {
            StyleInfo dynStyleInfo = catalog.getStyleByName(DEFAULTNAMESPACE, styleName + "Dyn");
            if (dynStyleInfo != null) {
                dynamicLayer.setDefaultStyle(dynStyleInfo);
            } else {
                dynamicLayer.setDefaultStyle(null);
                LOGGER.warning("dynamic FeatureType NO_STYLE:" + featureTypeNameDyn + "-" + styleName);
            }
            // dynamicLayer.setName(featureTypeName + "-dyn");
        }

        r.setEnabled(true);
        catalog.add(r);

        layer.setEnabled(true);
        catalog.add(layer);

        if ((dynamicLayer != null) && (dr != null)) {
            if (dynamicLayer.getDefaultStyle() != null) {
                dr.setEnabled(true);
                catalog.add(dr);

                dynamicLayer.setEnabled(true);
                catalog.add(dynamicLayer);
            }
        }
        return true;
    }

    protected boolean updateFeatureTypeConfig(FeatureTypeInfo ftConfig, DataStore dataStore,
                                              Map styles, HashMap<String, String> defaultStyles) throws IOException {
        FeatureType featureType = dataStore.getSchema(ftConfig.getName());
        /*
        String currentStyle = ftConfig.getDefaultStyle();
        String defaultStyle = getDefaultFeatureTypeStyleId(styles, defaultStyles, featureType);
        if (!currentStyle.equals(defaultStyle)) {
            ftConfig.setDefaultStyle(defaultStyle);
            return true;
        }
        */
        return false;
    }

    protected String getDefaultFeatureTypeStyleId(List<StyleInfo> styles, HashMap<String, String> defaultStyles,
                                                  FeatureType featureType) {
        String ftName = featureType.getName().getLocalPart();
        boolean isNormalFeature = false;
        boolean isLandBased = false;
        boolean isIndex = false;
        boolean isSmallIndex = false;
        boolean isSymbol = false;
        GeometryDescriptor geomAttrType = featureType.getGeometryDescriptor();
        GeometryType geomType = geomAttrType.getType();
        if (defaultStyles.containsKey(ftName)) {
            String defaultStyleName = defaultStyles.get(ftName);
            String styleName = retrieveDefaultStyle(styles, defaultStyleName, "unknown");
            if (!styleName.equals("unknown")) {
                return styleName;
            }
        }

        if (ftName.indexOf("fsc") != -1) {
            isNormalFeature = true;
        }
        if (ftName.indexOf("indexshape") != -1) {
            isIndex = true;
        }
        if (ftName.indexOf("indexshapes") != -1) {
            isSmallIndex = true;
        }
        if (ftName.indexOf("lnd") != -1) {
            isLandBased = true;
        }
        if (featureType.getName().getLocalPart().indexOf("symbol") != -1) {
            isSymbol = true;
        }

        Class<?> bindingClass = geomType.getBinding();
        if (Point.class.equals(bindingClass)) {
            if (isSymbol) {
                return retrieveDefaultStyle(styles, "xtpc-symbol", "point");
            } else if (isIndex) {
                return retrieveDefaultStyle(styles, "xtpc-tpclidText", "point");
            } else {
                return retrieveDefaultStyle(styles, "xtpc-text", "point");
            }
        } else if (LineString.class.equals(bindingClass)) {
            if ((!isIndex) && (!isLandBased)) {
                return retrieveDefaultStyle(styles, "xtpc-polyline", "line");
            } else if (isIndex) {
                if (isSmallIndex)
                    return retrieveDefaultStyle(styles, "xtpc-indexshapes", "line");

                return retrieveDefaultStyle(styles, "xtpc-indexshape", "line");
            } else if (isLandBased) {
                return retrieveDefaultStyle(styles, "xtpc-lndcityLine", "line");
            }
        } else if (MultiPoint.class.equals(bindingClass)) {
            if (isSymbol) {
                return retrieveDefaultStyle(styles, "xtpc-symbol", "point");
            } else {
                return retrieveDefaultStyle(styles, "xtpc-text", "point");
            }
        } else if (Polygon.class.equals(bindingClass)) {
            if ((!isIndex) && (!isLandBased)) {
                if (isNormalFeature) {
                    return retrieveDefaultStyle(styles, "xtpc-symbol", "polygon");
                }
                return retrieveDefaultStyle(styles, "polygon", "polygon");
            } else if (isIndex) {
                return retrieveDefaultStyle(styles, "xtpc_indexshape", "polygon");
            } else if (isLandBased) {
                return retrieveDefaultStyle(styles, "xtpc-lndcityPolygon", "polygon");
            }
        } else if (LinearRing.class.equals(bindingClass)) {
            if (!isIndex) {
                return retrieveDefaultStyle(styles, "polygon", "polygon");
            } else {
                return retrieveDefaultStyle(styles, "xtpc-indexshape", "polygon");
            }
        } else if (MultiLineString.class.equals(bindingClass)) {
            if ((!isIndex) && (!isLandBased)) {
                return retrieveDefaultStyle(styles, "xtpc-polyline", "line");
            } else if (isLandBased) {
                return retrieveDefaultStyle(styles, "xtpc-lndcityLine", "line");
            } else {
                return retrieveDefaultStyle(styles, "xtpc-indexshape", "line");
            }
        } else if (MultiPolygon.class.equals(bindingClass)) {
            return "polygon";
        }

        return "xtpc-symbol";
    }

    /*
     * computes the lat/lon bounding box from the native bounding box and srs, optionally overriding
     * the value already set.
     */
    boolean computeLatLonBoundingBox(LayerInfo layer, boolean force) throws Exception {
        ResourceInfo r = layer.getResource();
        if (force || r.getLatLonBoundingBox() == null && r.getNativeBoundingBox() != null) {
            CoordinateReferenceSystem nativeCRS = CRS.decode(r.getSRS());
            ReferencedEnvelope nativeBbox =
                new ReferencedEnvelope(r.getNativeBoundingBox(), nativeCRS);
            r.setLatLonBoundingBox(nativeBbox.transform(CRS.decode("EPSG:4326"), true));
            return true;
        }
        return false;
    }

    private static String retrieveDefaultStyle(List<StyleInfo> styles, String styleName, String defaultStyleName) {
        for (StyleInfo style : styles) {
            if (style.getName().equals(styleName)) {
                return styleName;
            }
        }
        return defaultStyleName;
    }

    protected Map<String, String> rebuildGeoserverLayerGroupMapping(Catalog catalog,
                                                                    boolean masterMode)
        throws JobExecutionException, IOException {

        catalog.getFeatureTypes();
        List<LayerInfo> layers = catalog.getLayers();
        Map<String, String> layerMapping = new HashMap<String, String>();

        ArrayList baseMapNames = new ArrayList();
        for (LayerInfo layer:layers) {
            baseMapNames.add(layer.getName());
        }

        XGeosDataConfigMapping configMapping = getConfigMapping();
        if (configMapping.getMapping().isEmpty()) {
            LOGGER.warning("XGeosDataConfigMapping is empty! Please check XGeosDataConfig file.");
            return null;
        }

        LinkedList defaultMapNames = new LinkedList(configMapping.getMapping().keySet());
        if (defaultMapNames.isEmpty()) {
            LOGGER.warning("DefaultMapNames is empty in XGeosDataConfigMapping! Please check XGeosDataConfig file.");
        }

        for (Object key : baseMapNames) {
            String baseMapTitle = (String) key;
            if (configMapping.getMapping().containsKey(baseMapTitle)) {
                int index = defaultMapNames.indexOf(baseMapTitle);
                if (index != -1)
                    defaultMapNames.remove(index);

                List configs = (List) configMapping.getMapping().get(baseMapTitle);
                String defaultLayerNames = buildDefaultLayerNames(DEFAULTNAMESPACE, configs);
                layerMapping.put(baseMapTitle, defaultLayerNames);
            } else {
                LOGGER.warning("lv='" + baseMapTitle + "' cannot found config information in XGeosDataConfigMapping.");
            }
        }

        for (Object key : defaultMapNames) {
            List configs = (List) configMapping.getMapping().get(key);
            // String defaultLayerNames = buildDefaultLayerNames(DEFAULTNAMESPACE, configs);
            String defaultLayerNames = buildDefaultLayerNames(null, configs);
            layerMapping.put((String) key, defaultLayerNames);
        }
        return layerMapping;
    }

    protected void resetGeoServerLayerGroupConfiguration(JobExecutionContext executionContext,
                                                         CatalogBuilder catalogBuilder, Catalog catalog,
                                                         JDBCDataStore dataStore,
                                                         Map<String, String> layerMapping, boolean masterMode,
                                                         DataStoreInfo targetDataStoreInfo)
        throws JobExecutionException, IOException {

        if ((masterMode) && (!checkCurrentRepositoryStatus(dataStore, DataReposVersionManager.VSSTATUS_CONFIG))) {
            return;
        }

        TreeMap<String, LayerInfo> existLayerNames = new TreeMap<String, LayerInfo>();
        ReferencedEnvelope defaultLatLonBBox = null;
        ReferencedEnvelope defaultNativeBBox = null;

        List<LayerInfo> layers = catalog.getLayers();
        for (LayerInfo layerInfo : layers) {
            existLayerNames.put(layerInfo.getName(), layerInfo);
            if (layerInfo.getName().equals("fsc-106-c0")) {
                ResourceInfo r = layerInfo.getResource();
                defaultLatLonBBox = r.getLatLonBoundingBox();
                defaultNativeBBox = r.getNativeBoundingBox();
            }
        }

        HashMap<String, List<String>> layerGroupContext = new HashMap<String, List<String>>();
        for (String lgName : layerMapping.keySet()) {
            String layerset = layerMapping.get(lgName);
            if (lgName.startsWith("pgOMS")) {
                ArrayList<String> layerGroupContainer = new ArrayList<String>();
                String[] layerNames = null;
                if (layerset != null) {
                    layerNames = layerset.split(",");
                } else {
                    LOGGER.info("vl='" + lgName + "' is empty value.");
                    continue;
                }

                for (String ln : layerNames) {
                    String dynName = ln + "-dyn";
                    if (existLayerNames.keySet().contains(dynName)) {
                        layerGroupContainer.add(dynName);
                    }
                }

                if (layerGroupContainer.size() > 0)
                    layerGroupContext.put(lgName, layerGroupContainer);
            } else if (lgName.startsWith("pg")) {
                ArrayList<String> layerGroupContainer = new ArrayList<String>();
                String[] layerNames = null;
                if (layerset != null) {
                    layerNames = layerset.split(",");
                } else {
                    LOGGER.info("vl='" + lgName + "' is empty value.");
                    continue;
                }

                for (String ln : layerNames) {
                    if (existLayerNames.keySet().contains(ln)) {
                        layerGroupContainer.add(ln);
                    }
                }

                if (layerGroupContainer.size() > 0)
                    layerGroupContext.put(lgName, layerGroupContainer);
            }
        }

        CatalogFactory factory = catalog.getFactory();
        for (String lgName : layerGroupContext.keySet()) {
            LayerGroupInfo lg = factory.createLayerGroup();
            lg.setName(lgName);
            lg.setTitle(lgName);
            lg.setWorkspace(targetDataStoreInfo.getWorkspace());
            lg.setMode(LayerGroupInfo.Mode.SINGLE);
            if (defaultNativeBBox != null) {
                lg.setBounds(defaultNativeBBox);
            }
            List<String> layerGroupContainer = layerGroupContext.get(lgName);
            for (String aLayerName : layerGroupContainer) {
                LayerInfo lyinfo = existLayerNames.get(aLayerName);
                lg.getLayers().add(lyinfo);
            }
            catalog.add(lg);
        }
    }

    private String buildDefaultLayerNames(String namespace, List xgeosConfigs) {
        StringBuilder sbLayers = new StringBuilder();
        boolean first = true;

        for (Object value : xgeosConfigs) {
            if (!first) {
                sbLayers.append(',');
            } else {
                first = false;
            }
            XGeosDataConfig xgeosConfig = (XGeosDataConfig) value;

            StringBuilder sbView = new StringBuilder();
            if (namespace != null) {
                sbView.append(namespace);
                sbView.append(':');
            }
            sbView.append("fsc-");
            sbView.append(xgeosConfig.getFSC()).append("-c");
            sbView.append(xgeosConfig.getCOMP()).append("-l");
            sbView.append(xgeosConfig.getLEV()).append("-w");
            sbView.append(xgeosConfig.getWEIGHT());
            sbLayers.append(sbView.toString());
        }

        return sbLayers.toString();
    }

    private HashMap<String, String> buildDefaultStylesMapping(XGeosDataConfigMapping configMapping) {
        HashMap<String, String> result = new HashMap<String, String>();

        for (Object key : configMapping.getMapping().keySet()) {
            List xgeosConfigs = (List) configMapping.getMapping().get(key);
            for (Object value : xgeosConfigs) {
                XGeosDataConfig xgeosConfig = (XGeosDataConfig) value;

                StringBuilder sbView = new StringBuilder("fsc-");
                sbView.append(xgeosConfig.getFSC()).append("-c");
                sbView.append(xgeosConfig.getCOMP()).append("-l");
                sbView.append(xgeosConfig.getLEV()).append("-w");
                sbView.append(xgeosConfig.getWEIGHT());

                String viewName = sbView.toString();
                if (!result.containsKey(viewName)) {
                    result.put(viewName, xgeosConfig.getFTYPE());
                } else {
                    if (xgeosConfig.getFTYPE() != null) {
                        if (!result.get(viewName).equals(xgeosConfig.getFTYPE()))
                            LOGGER.info("Style Define Diff:" + result.get(viewName) + " - " + xgeosConfig.getFTYPE());
                    } else {
                        LOGGER.warning("xgeosConfig getFTYPE() is null - " + xgeosConfig.toString());
                    }
                }
            }
        }
        return result;
    }

    private Set<String> buildDynamicColorLayerNames(XGeosDataConfigMapping mapping, String groupName) {
        TreeSet<String> names = new TreeSet<String>();
        List rawName = (List) mapping.getMapping().get(groupName);
        for (Object value : rawName) {
            XGeosDataConfig xgeosConfig = (XGeosDataConfig) value;

            StringBuilder sbView = new StringBuilder("fsc-");
            sbView.append(xgeosConfig.getFSC()).append("-c");
            sbView.append(xgeosConfig.getCOMP()).append("-l");
            sbView.append(xgeosConfig.getLEV()).append("-w");
            sbView.append(xgeosConfig.getWEIGHT());

            String viewName = sbView.toString();
            if (!names.contains(viewName)) {
                names.add(viewName);
            }
        }
        return names;
    }

    protected void resetGeoserverDataState(JobExecutionContext executionContext, JDBCDataStore dataStore,
                                           short vsstatusBefore, short vsstatusAfter, boolean exclusive)
        throws JobExecutionException {
        ServletContext servletContext = ContextLoader.getCurrentWebApplicationContext().getServletContext();
        // DataSource dataSource = null;
        Connection connection = null;
        try {
            // dataSource = dataStore.getDataSource();
            // connection = DataSourceUtils.getConnection(dataSource);
            connection = dataStore.getConnection(Transaction.AUTO_COMMIT);

            String currentTargetSchema = retrieveCurrentSchemaName(connection, vsstatusBefore);

            if (currentTargetSchema == null) {
                LOGGER.fine("Cannot found target schema in dataStore.");
                return;
            }

            String existTargetSchema = null;
            if (exclusive)
                existTargetSchema = retrieveCurrentSchemaName(connection, vsstatusAfter);


            // GeoServer geoServer = getGeosServer(executionContext);
            // geoServer.reload();
            // geoServer.reset();
            // LOGGER.fine("resetData-load sucessful.");

            updateCurrentRepositoryStatus(connection, currentTargetSchema, vsstatusAfter);
            if ((exclusive) && (existTargetSchema != null)) {
                updateCurrentRepositoryStatus(connection, existTargetSchema,
                    DataReposVersionManager.VSSTATUS_AVAILABLE);
            }
        /*
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            throw new JobExecutionException("Update " + DataReposVersionManager.XGVERSIONTABLE_NAME +
                " has error-", e);
        */
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            throw new JobExecutionException("Update " + DataReposVersionManager.XGVERSIONTABLE_NAME +
                " has error-", e);
        } catch (IOException e) {
            throw new JobExecutionException("Update " + DataReposVersionManager.XGVERSIONTABLE_NAME +
                " has error-", e);
        } finally {
            if ((connection != null) && (dataStore != null)) {
                dataStore.closeSafe(connection);
            }
            // if (dataStore != null) dataStore.dispose();
        }
    }

    private boolean setFeatureConfBBoxFromNative(ReferencedEnvelope bboxNative, FeatureTypeInfo typeConfig, FeatureType featureType)
        throws JobExecutionException {
        if ((bboxNative == null) || (bboxNative.isNull())) return false;

        try {
            String srcSRS = EPSG + typeConfig.getSRS();
            CoordinateReferenceSystem crsDeclared = CRS.decode(srcSRS);
            CoordinateReferenceSystem original = null;

            if (featureType.getGeometryDescriptor() != null) {
                original = featureType.getGeometryDescriptor().getCoordinateReferenceSystem();
            }

            if (original == null) {
                original = crsDeclared;
            }

            CoordinateReferenceSystem crsLatLong = CRS.decode("EPSG:4326"); // latlong
            // let's show coordinates in the declared crs, not in the native one, to
            // avoid confusion (since on screen we do have the declared one, the native is
            // not visible)
            ReferencedEnvelope declaredEnvelope = bboxNative;

            if (!CRS.equalsIgnoreMetadata(original, crsDeclared)) {
                // MathTransform xform = CRS.findMathTransform(original, crsDeclared, true);
                // declaredEnvelope = JTS.transform(bboxNative, null, xform, 10); //convert data bbox to lat/long
                declaredEnvelope = bboxNative.transform(crsDeclared, true, 10);
            }

            typeConfig.setNativeBoundingBox(declaredEnvelope);

            // MathTransform xform = CRS.findMathTransform(original, crsLatLong, true);
            // Envelope xformed_envelope = JTS.transform(bboxNative, xform); //convert data bbox to lat/long
            ReferencedEnvelope bboxLatLong = bboxNative.transform(crsLatLong, true);
            typeConfig.setLatLonBoundingBox(bboxLatLong);
            return true;
        } catch (FactoryException e) {
            LOGGER.log(Level.INFO, e.getMessage(), e);
            return false;
        } catch (TransformException e) {
            LOGGER.log(Level.INFO, e.getMessage(), e);
            return false;
        }
    }

    private String retrieveCurrentSchemaName(Connection connection, short status) throws SQLException {
        StringBuilder sbSQL = new StringBuilder("SELECT vsschema, vstimestamp, vsstatus FROM ");
        sbSQL.append(DataReposVersionManager.XGVERSIONTABLE_NAME);
        sbSQL.append(" WHERE vsstatus = ");
        sbSQL.append(status);
        sbSQL.append(" ORDER BY vsid");

        String result = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();
            rs = stmt.executeQuery(sbSQL.toString());
            // get first result
            if (rs.next()) {
                result = rs.getString(1);
            }
            return result;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
    }

    private Timestamp retrieveCurrentSchemaTimestamp(Connection connection, short status) throws SQLException {
        StringBuilder sbSQL = new StringBuilder("SELECT vstimestamp, vsschema, vsstatus FROM ");
        sbSQL.append(DataReposVersionManager.XGVERSIONTABLE_NAME);
        sbSQL.append(" WHERE vsstatus = ");
        sbSQL.append(status);
        sbSQL.append(" ORDER BY vsid");

        Timestamp result = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.createStatement();
            rs = stmt.executeQuery(sbSQL.toString());
            // get first result
            if (rs.next()) {
                result = rs.getTimestamp(1);
            }
            return result;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        }
    }

    private void updateCurrentRepositoryStatus(Connection connection, String schemaName, short newStatus)
        throws SQLException {
        StringBuilder sbSQL = new StringBuilder("UPDATE ");
        sbSQL.append(DataReposVersionManager.XGVERSIONTABLE_NAME).append(' ');
        sbSQL.append(" SET vsstatus = ");
        sbSQL.append(newStatus);
        sbSQL.append(", vstimestamp = CURRENT_TIMESTAMP WHERE vsschema = '");
        sbSQL.append(schemaName).append("'");

        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate(sbSQL.toString());
        } finally {
            if (stmt != null) stmt.close();
        }
    }

    private boolean checkCurrentRepositoryStatus(JDBCDataStore dataStore, short status) {
        DataSource dataSource = null;
        Connection connection = null;

        try {
            connection = dataStore.getConnection(Transaction.AUTO_COMMIT);
            // connection = DataSourceUtils.getConnection(dataSource);
            return checkCurrentRepositoryStatus(connection, status);
        // } catch (SQLException e) {
        //     LOGGER.log(Level.WARNING, e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.log(Level.INFO, e.getMessage(), e);
        } finally {
            if ((connection != null) && (dataSource != null)) {
                // DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
        return false;
    }

    private boolean checkCurrentRepositoryStatus(Connection connection, short status) {
        try {
            return (retrieveCurrentSchemaName(connection, status) != null);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            return false;
        }
    }

    /*
    private boolean syncFeatureTypesMapping(JobExecutionContext context, DataConfig dataConfig, DataStoreConfig dataStoreConf)
        throws IOException, JobExecutionException {
        assert context != null;

        ServletContext servletContext = ContextLoader.getCurrentWebApplicationContext().getServletContext();
        DataStore dataStore = dataStoreConf.findDataStore(servletContext);
        boolean needCheckSync = false;

        try {
            if (dataStore instanceof PostgisDataStore) {
                PostgisDataStore pgDataStore = (PostgisDataStore) dataStore;
                DataSource dataSource = pgDataStore.getDataSource();
                Connection connection = dataSource.getConnection();

                // String currentTargetSchema = retrieveCurrentSchemaName(connection, vsstatusBefore);
                Timestamp last = retrieveCurrentSchemaTimestamp(connection, DataReposVersionManager.VSSTATUS_USING);
                if (last == null) return false;
                if (lastUpdate != null) {
                    needCheckSync = last.after(lastUpdate);
                } else {
                    needCheckSync = true;
                }

                if (!needCheckSync) return false;
                lastUpdate = Calendar.getInstance().getTime();

                Map styles = dataConfig.getStyles();
                XGeosDataConfigMapping mapping = getConfigMapping();
                HashMap<String, String> defaultStyles = buildDefaultStylesMapping(mapping);

                try {
                    String[] dsFTypeNames = dataStore.getTypeNames();

                    for (String featureTypeName : dsFTypeNames) {
                        String ftKey = dataStoreConf.getId() + DataConfig.SEPARATOR + featureTypeName;
                        FeatureTypeConfig ftConfig = dataConfig.getFeatureTypeConfig(ftKey);
                        if (ftConfig == null) {
                            if (createLayerFeatureTypeInfo(dataConfig, dataStoreConf, dataStore, styles,
                                featureTypeName, defaultStyles)) {
                                needCheckSync = needCheckSync | true;
                            }
                        } else {
                            needCheckSync |= updateFeatureTypeConfig(ftConfig, dataStore, styles, defaultStyles);
                        }
                    }
                } finally {
                    if (dataStore != null) dataStore.dispose();
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            return false;
        }
        return needCheckSync;
    }
    */
}