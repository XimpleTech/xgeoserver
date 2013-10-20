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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.xmlrules.DigesterLoader;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.feature.FeatureSourceUtils;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.type.FeatureType;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleTrigger;
import org.springframework.web.context.ContextLoader;
import org.vfny.geoserver.global.ConfigurationException;
import org.vfny.geoserver.global.GeoserverDataDirectory;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Envelope;
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
    private static final String DEFAULTNAMESPACE = "tpc";
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
                LOGGER.warning("no xmark dir found, creating new one");
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

    }

    /*
    protected void doExecuteInternal(JobExecutionContext executionContext)
        throws JobExecutionException {
        if (executionContext == null)
            return;

        if (LOGGER.isLoggable(Level.INFO))
            LOGGER.info("XGeoResetDataConfigJob internal execute begin.");

        boolean masterMode = isMasterMode(executionContext);
        DataConfig dataConfig = getDataConfig(executionContext);
        DataStoreConfig targetDataStoreConf = null;
        DataStoreInfo targetDataStoreInfo = getCatalog().getDataStoreByName("pgDMMS");
        // if (targetDataStoreInfo.)
        List idList = dataConfig.getDataStoreIds();
        for (Object anIdList : idList) {
            String dsId = (String) anIdList;
            if (dsId.equalsIgnoreCase("pgDMMS")) {
                DataStoreConfig dsConf = dataConfig.getDataStore(dsId);
                Map params = dsConf.getConnectionParams();
                if (params.get("dbtype").equals("postgis")) {
                    String ownerName = (String) params.get("user");
                    targetDataStoreConf = dsConf;
                    if (masterMode)
                        resetPostgisViewMapping(executionContext, targetDataStoreConf, ownerName);
                }
                break;
            }
        }

        try {
            if (masterMode) {
                if (LOGGER.isLoggable(Level.INFO))
                    LOGGER.info("XGeoResetDataConfigJob into MASTERMODE.");
                // MASTER NODE MODE
                resetFeatureTypesMapping(executionContext, dataConfig, targetDataStoreConf);
                resetGeoserverDataConfig(executionContext, targetDataStoreConf,
                    DataReposVersionManager.VSSTATUS_LINKVIEW,
                    DataReposVersionManager.VSSTATUS_CONFIG, false);

                resetGeoserverWMSConfig(executionContext, targetDataStoreConf, masterMode);
                resetWMSVirtualLayerMapping(executionContext, targetDataStoreConf, masterMode);
                resetGeoserverDataConfig(executionContext, targetDataStoreConf,
                    DataReposVersionManager.VSSTATUS_CONFIG,
                    DataReposVersionManager.VSSTATUS_USING, true);
                lastUpdate = Calendar.getInstance().getTime();
            } else {
                if (LOGGER.isLoggable(Level.INFO))
                    LOGGER.info("XGeoResetDataConfigJob into SALAVEMODE.");
                // SALAVE NODE MODE
                boolean needReset = syncFeatureTypesMapping(executionContext, dataConfig, targetDataStoreConf);
                if (needReset) {
                    updateGeoserver(executionContext);
                    updateValidation(executionContext);
                    if (LOGGER.isLoggable(Level.FINE))
                        LOGGER.fine("resetData-update sucessful.");

                    saveGeoserver(executionContext);
                    saveValidation(executionContext);
                    if (LOGGER.isLoggable(Level.FINE))
                        LOGGER.fine("resetData-save sucessful.");

                    loadGeoserver(executionContext);
                    loadValidation(executionContext);
                    if (LOGGER.isLoggable(Level.INFO))
                        LOGGER.info("resetData-load sucessful.");

                    resetGeoserverWMSConfig(executionContext, targetDataStoreConf, masterMode);
                    resetWMSVirtualLayerMapping(executionContext, targetDataStoreConf, masterMode);

                    updateGeoserver(executionContext);
                    updateValidation(executionContext);
                    if (LOGGER.isLoggable(Level.FINE))
                        LOGGER.fine("resetData-update sucessful.");

                    saveGeoserver(executionContext);
                    saveValidation(executionContext);
                    if (LOGGER.isLoggable(Level.FINE))
                        LOGGER.fine("resetData-save sucessful.");

                    loadGeoserver(executionContext);
                    loadValidation(executionContext);
                    if (LOGGER.isLoggable(Level.INFO))
                        LOGGER.info("resetGeoserverWMSConfig and resetWMSVirtualLayerMapping sucessful.");

                    lastUpdate = new Date(System.currentTimeMillis());
                }
                if (lastUpdate == null) {
                    lastUpdate = new Date(System.currentTimeMillis());
                }
            }

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }

        if (LOGGER.isLoggable(Level.INFO))
            LOGGER.info("XGeoResetDataConfigJob internal execute completed.");
    }
    */

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

    /*
    private void resetFeatureTypesMapping(JobExecutionContext executionContext,
                                          DataInfo dataConfig, DataStoreConfigInfo dsConf)
        throws IOException, JobExecutionException {
        assert executionContext != null;

        ServletContext servletContext = ContextLoader.getCurrentWebApplicationContext().getServletContext();
        DataStore dataStore = dsConf.findDataStore(servletContext);

        if (!checkCurrentRepositoryStatus(dataStore, DataReposVersionManager.VSSTATUS_LINKVIEW)) {
            return;
        }

        Map styles = dataConfig.getStyles();
        XGeosDataConfigMapping mapping = getConfigMapping();
        HashMap<String, String> defaultStyles = buildDefaultStylesMapping(mapping);

        try {
            String[] dsFTypeNames = dataStore.getTypeNames();

            for (String featureTypeName : dsFTypeNames) {
                String ftKey = dsConf.getId() + DataConfig.SEPARATOR + featureTypeName;
                FeatureTypeConfig ftConfig = dataConfig.getFeatureTypeConfig(ftKey);
                if (ftConfig == null) {
                    if (!createFeatureTypeConfig(dataConfig, dsConf, dataStore, styles, featureTypeName, defaultStyles)) {
                        LOGGER.info("Create Feature Failed. [" + featureTypeName + "]");
                    }
                } else {
                    updateFeatureTypeConfig(ftConfig, dataStore, styles, defaultStyles);
                }
            }
        } finally {
            if (dataStore != null) dataStore.dispose();
        }
    }

    private boolean createFeatureTypeConfig(DataConfig dataConfig, DataStoreConfig dsConf, DataStore dataStore,
                                            Map styles, String featureTypeName, HashMap<String, String> defaultStyles)
        throws IOException, JobExecutionException {
        FeatureTypeConfig ftConfig;
        FeatureType featureType = dataStore.getSchema(featureTypeName);
        ftConfig = new FeatureTypeConfig(dsConf.getId(), featureType, false);

        if (featureType.getDefaultGeometry() == null) {
            LOGGER.warning("featureType=" + featureType.getTypeName() + " has not DefaultGeometry");
            return false;
        }

        CoordinateReferenceSystem crs = featureType.getDefaultGeometry().getCoordinateSystem();
        if (crs != null) {
            Set idents = crs.getIdentifiers();

            for (Object ident : idents) {
                Identifier id = (Identifier) ident;

                if (id.toString().indexOf("EPSG:") != -1) {
                    //we have an EPSG #, so lets use it!
                    String str_num = id.toString().substring(id.toString().indexOf(':') + 1);
                    int num = Integer.parseInt(str_num);
                    ftConfig.setSRS(num);

                    break; // take the first EPSG
                }
            }
        } else {
            ftConfig.setSRS(SRSID_TWD97_ZONE121);
        }

        Envelope latLongBox = new Envelope();
        Envelope nativeBox = new Envelope();

        FeatureSource fs = dataStore.getFeatureSource(featureType.getTypeName());
        Envelope bboxNative = FeatureSourceUtils.getBoundingBoxEnvelope(fs);
        if (!setFeatureConfBBoxFromNative(bboxNative, ftConfig, featureType)) {
            if (!latLongBox.isNull()) {
                ftConfig.setLatLongBBox(latLongBox);
                ftConfig.setNativeBBox(nativeBox);
            } else {
                LOGGER.info("Cannot setLatLongBBox on " + ftConfig.toString());
            }
        }

        ftConfig.setDefaultStyle(getDefaultFeatureTypeStyleId(styles, defaultStyles, featureType));

        if (ftConfig.getAlias() != null && !"".equals(ftConfig.getAlias()))
            dataConfig.addFeatureType(ftConfig.getDataStoreId() + ":" + ftConfig.getAlias(), ftConfig);
        else
            dataConfig.addFeatureType(ftConfig.getDataStoreId() + ":" + ftConfig.getName(), ftConfig);
        return true;
    }

    protected boolean updateFeatureTypeConfig(FeatureTypeConfig ftConfig, DataStore dataStore,
                                              Map styles, HashMap<String, String> defaultStyles) throws IOException {
        FeatureType featureType = dataStore.getSchema(ftConfig.getName());
        String currentStyle = ftConfig.getDefaultStyle();
        String defaultStyle = getDefaultFeatureTypeStyleId(styles, defaultStyles, featureType);
        if (!currentStyle.equals(defaultStyle)) {
            ftConfig.setDefaultStyle(defaultStyle);
            return true;
        }
        return false;
    }

    protected String getDefaultFeatureTypeStyleId(Map styles, HashMap<String, String> defaultStyles, FeatureType featureType) {
        String ftName = featureType.getTypeName();
        boolean isNormalFeature = false;
        boolean isLandBased = false;
        boolean isIndex = false;
        boolean isSmallIndex = false;
        boolean isSymbol = false;
        GeometryAttributeType geomAttrType = featureType.getDefaultGeometry();
        Class geomType = geomAttrType.getBinding();
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
        if (featureType.find("symbol") != -1) {
            isSymbol = true;
        }

        if (Point.class.equals(geomType)) {
            if (isSymbol) {
                return retrieveDefaultStyle(styles, "pgTPC_Symbol", "point");
            } else if (isIndex) {
                return retrieveDefaultStyle(styles, "pgTPC_TpclidText", "point");
            } else {
                return retrieveDefaultStyle(styles, "pgTPC_Text", "point");
            }
        } else if (LineString.class.equals(geomType)) {
            if ((!isIndex) && (!isLandBased)) {
                return retrieveDefaultStyle(styles, "pgTPC_Conductor", "line");
            } else if (isIndex) {
                if (isSmallIndex)
                    return retrieveDefaultStyle(styles, "pgTPC_INDEXSHAPES", "line");

                return retrieveDefaultStyle(styles, "pgTPC_INDEXSHAPE", "line");
            } else if (isLandBased) {
                return retrieveDefaultStyle(styles, "pgTPC_LndcityLine", "line");
            }
        } else if (MultiPoint.class.equals(geomType)) {
            if (isSymbol) {
                return retrieveDefaultStyle(styles, "pgTPC_Symbol", "point");
            } else {
                return retrieveDefaultStyle(styles, "pgTPC_Text", "point");
            }
        } else if (Polygon.class.equals(geomType)) {
            if ((!isIndex) && (!isLandBased)) {
                return retrieveDefaultStyle(styles, "polygon", "polygon");
            } else if (isIndex) {
                return retrieveDefaultStyle(styles, "pgTPC_INDEXSHAPE", "polygon");
            } else if (isLandBased) {
                return retrieveDefaultStyle(styles, "pgTPC_LndcityPolygon", "polygon");
            }
        } else if (LinearRing.class.equals(geomType)) {
            if (!isIndex) {
                return retrieveDefaultStyle(styles, "polygon", "polygon");
            } else {
                return retrieveDefaultStyle(styles, "pgTPC_INDEXSHAPE", "polygon");
            }
        } else if (MultiLineString.class.equals(geomType)) {
            if ((!isIndex) && (!isLandBased)) {
                return retrieveDefaultStyle(styles, "pgTPC_Conductor", "line");
            } else if (isLandBased) {
                return retrieveDefaultStyle(styles, "pgTPC_LndcityLine", "line");
            } else {
                return retrieveDefaultStyle(styles, "pgTPC_INDEXSHAPE", "line");
            }
        } else if (MultiPolygon.class.equals(geomType)) {
            return "polygon";
        }

        return "pgTPC_Symbol";
    }
    */

    private static String retrieveDefaultStyle(Map styles, String styleName, String defaultStyleName) {
        if (styles.containsKey(styleName)) {
            return styleName;
        } else
            return defaultStyleName;
    }

    /*
    protected void resetGeoserverWMSConfig(JobExecutionContext executionContext, DataStoreConfig dataStoreConf,
                                           boolean masterMode)
        throws JobExecutionException, IOException {
        ServletContext servletContext = ContextLoader.getCurrentWebApplicationContext().getServletContext();
        DataStore dataStore = dataStoreConf.findDataStore(servletContext);
        if ((masterMode) && (!checkCurrentRepositoryStatus(dataStore, DataReposVersionManager.VSSTATUS_CONFIG))) {
            return;
        }

        WMSConfig wmsConfig = getWMSConfig(executionContext);
        Map baseMapLayers = wmsConfig.getBaseMapLayers();
        ArrayList baseMapNames = new ArrayList(baseMapLayers.keySet());

        XGeosDataConfigMapping configMapping = getConfigMapping();
        if (configMapping.getMapping().isEmpty()) {
            LOGGER.warning("XGeosDataConfigMapping is empty! Pleace check XGeosDataConfig file.");
            return;
        }

        LinkedList defaultMapNames = new LinkedList(configMapping.getMapping().keySet());
        if (defaultMapNames.isEmpty()) {
            LOGGER.warning("DefaultMapNames is emptyin XGeosDataConfigMapping! Pleace check XGeosDataConfig file.");
        }

        for (Object key : baseMapNames) {
            String baseMapTitle = (String) key;
            if (configMapping.getMapping().containsKey(baseMapTitle)) {
                int index = defaultMapNames.indexOf(baseMapTitle);
                if (index != -1)
                    defaultMapNames.remove(index);

                List configs = (List) configMapping.getMapping().get(baseMapTitle);
                String defaultLayerNames = buildDefaultWMSLayerNames(DEFAULTNAMESPACE, configs);
                wmsConfig.getBaseMapLayers().put(baseMapTitle, defaultLayerNames);
            } else {
                LOGGER.warning("lv='" + baseMapTitle + "' cannot found config information in XGeosDataConfigMapping.");
            }
        }

        for (Object key : defaultMapNames) {
            List configs = (List) configMapping.getMapping().get(key);
            String defaultLayerNames = buildDefaultWMSLayerNames(DEFAULTNAMESPACE, configs);
            wmsConfig.getBaseMapLayers().put(key, defaultLayerNames);
        }
    }

    protected void resetWMSVirtualLayerMapping(JobExecutionContext executionContext, DataStoreConfig dataStoreConf,
                                               boolean masterMode)
        throws JobExecutionException, IOException {
        ServletContext servletContext = ContextLoader.getCurrentWebApplicationContext().getServletContext();

        DataStore dataStore = dataStoreConf.findDataStore(servletContext);
        if ((masterMode) && (!checkCurrentRepositoryStatus(dataStore, DataReposVersionManager.VSSTATUS_CONFIG))) {
            return;
        }

        Data catalog = (Data) servletContext.getAttribute(Data.WEB_CONTAINER_KEY);

        WMSConfig wmsConfig = getWMSConfig(executionContext);
        Map baseMapLayers = wmsConfig.getBaseMapLayers();
        Map baseMapEnvelopes = wmsConfig.getBaseMapEnvelopes();
        ArrayList baseMapNames = new ArrayList(baseMapLayers.keySet());

        for (Object key : baseMapNames) {
            String baseMapTitle = (String) key;
            if (baseMapTitle.startsWith("pg")) {
                GeneralEnvelope envelope = (GeneralEnvelope) baseMapEnvelopes.get(baseMapTitle);

                GeneralEnvelope selectedEnvelope = null;
                String baseLayersValue = (String) wmsConfig.getBaseMapLayers().get(baseMapTitle);
                String[] layerNames = null;
                if (baseLayersValue != null) {
                    layerNames = baseLayersValue.split(",");
                } else {
                    LOGGER.info("vl='" + baseMapTitle + "' is empty value.");
                    continue;
                }

                ArrayList<String> newLayerNames = new ArrayList<String>();
                for (int i = 0; i < layerNames.length; i++) {
                    String layerName = layerNames[i].trim();

                    Integer layerType = catalog.getLayerType(layerName);
                    if (layerType != null) {
                        newLayerNames.add(layerName);
                        if (layerType.intValue() == MapLayerInfo.TYPE_VECTOR) {
                            FeatureTypeInfo ftype = catalog.getFeatureTypeInfo(layerName);
                            ftype = ((ftype != null) ? ftype
                                : catalog.getFeatureTypeInfo(layerName
                                .substring(layerName.indexOf(":") + 1, layerName.length())));

                            if (selectedEnvelope == null) {
                                ReferencedEnvelope ftEnvelope = null;

                                try {
                                    if (ftype.getBoundingBox() instanceof ReferencedEnvelope
                                        && !ftype.getBoundingBox().isNull()) {
                                        ftEnvelope = ftype.getBoundingBox();
                                    } else {
                                        // TODO Add Action Errors
                                        return;
                                    }
                                } catch (IOException e) {
                                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                                    return;
                                }

                                selectedEnvelope = new GeneralEnvelope(new double[]{
                                    ftEnvelope.getMinX(), ftEnvelope.getMinY()
                                },
                                    new double[]{ftEnvelope.getMaxX(), ftEnvelope.getMaxY()});
                                selectedEnvelope.setCoordinateReferenceSystem(ftEnvelope
                                    .getCoordinateReferenceSystem());
                            } else {
                                final CoordinateReferenceSystem dstCRS = selectedEnvelope
                                    .getCoordinateReferenceSystem();

                                ReferencedEnvelope ftEnvelope = null;

                                try {
                                    if (ftype.getBoundingBox() instanceof ReferencedEnvelope) {
                                        ftEnvelope = (ReferencedEnvelope) ftype.getBoundingBox();
                                        ftEnvelope.transform(dstCRS, true);
                                    } else {
                                        // TODO Add Action Errors
                                        return;
                                    }
                                } catch (TransformException e) {
                                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                                    return;
                                } catch (FactoryException e) {
                                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                                    return;
                                } catch (IOException e) {
                                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                                    return;
                                }

                                ReferencedEnvelope newEnvelope = new ReferencedEnvelope(dstCRS);
                                newEnvelope.init(selectedEnvelope.getLowerCorner().getOrdinate(0),
                                    selectedEnvelope.getUpperCorner().getOrdinate(0),
                                    selectedEnvelope.getLowerCorner().getOrdinate(1),
                                    selectedEnvelope.getUpperCorner().getOrdinate(1));

                                newEnvelope.expandToInclude(ftEnvelope);

                                selectedEnvelope = new GeneralEnvelope(new double[]{
                                    newEnvelope.getMinX(), newEnvelope.getMinY()
                                },
                                    new double[]{newEnvelope.getMaxX(), newEnvelope.getMaxY()});
                                selectedEnvelope.setCoordinateReferenceSystem(dstCRS);
                            }
                        }
                    } else {
                        LOGGER.warning("Cannot found layer " + layerName + " in " + baseMapTitle);
                    }
                }

                if (layerNames.length != newLayerNames.size()) {
                    StringBuilder layerBuilder = new StringBuilder();
                    boolean bFirst = true;
                    for (String newlayerName : newLayerNames) {
                        if (!bFirst) {
                            layerBuilder.append(',');
                        } else bFirst = false;
                        layerBuilder.append(newlayerName);
                    }
                    baseMapLayers.put(baseMapTitle, layerBuilder.toString());
                }

                if (selectedEnvelope != null) {
                    if (envelope != null) {
                        envelope.setCoordinateReferenceSystem(selectedEnvelope
                            .getCoordinateReferenceSystem());
                        envelope.setEnvelope(selectedEnvelope);
                        baseMapEnvelopes.put(baseMapTitle, envelope);
                    } else {
                        baseMapEnvelopes.put(baseMapTitle, selectedEnvelope);
                    }
                }
            }
        }
    }
    */

    private String buildDefaultWMSLayerNames(String namespace, List xgeosConfigs) {
        StringBuilder sbLayers = new StringBuilder();
        boolean first = true;

        for (Object value : xgeosConfigs) {
            if (!first) {
                sbLayers.append(',');
            } else {
                first = false;
            }
            XGeosDataConfig xgeosConfig = (XGeosDataConfig) value;

            StringBuilder sbView = new StringBuilder(namespace);
            sbView.append(":fsc-");
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

    /*
    protected void resetGeoserverDataConfig(JobExecutionContext executionContext, DataStoreConfig dataConfig,
                                            short vsstatusBefore, short vsstatusAfter, boolean exclusive)
        throws JobExecutionException {
        ServletContext servletContext = ContextLoader.getCurrentWebApplicationContext().getServletContext();
        DataStore dataStore = null;
        try {
            dataStore = dataConfig.findDataStore(servletContext);
            if (dataStore instanceof PostgisDataStore) {
                PostgisDataStore pgDataStore = (PostgisDataStore) dataStore;
                DataSource dataSource = pgDataStore.getDataSource();
                Connection connection = dataSource.getConnection();

                String currentTargetSchema = retrieveCurrentSchemaName(connection, vsstatusBefore);

                if (currentTargetSchema == null) {
                    LOGGER.fine("Cannot found target schema in dataStore.");
                    return;
                }

                String existTargetSchema = null;
                if (exclusive)
                    existTargetSchema = retrieveCurrentSchemaName(connection, vsstatusAfter);

                updateGeoserver(executionContext);
                updateValidation(executionContext);
                LOGGER.fine("resetData-update sucessful.");

                saveGeoserver(executionContext);
                saveValidation(executionContext);
                LOGGER.fine("resetData-save sucessful.");

                loadGeoserver(executionContext);
                loadValidation(executionContext);
                LOGGER.fine("resetData-load sucessful.");

                updateCurrentRepositoryStatus(connection, currentTargetSchema, vsstatusAfter);
                if ((exclusive) && (existTargetSchema != null)) {
                    updateCurrentRepositoryStatus(connection, existTargetSchema,
                        DataReposVersionManager.VSSTATUS_AVAILABLE);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            throw new JobExecutionException("Update " + DataReposVersionManager.XGVERSIONTABLE_NAME +
                " has error-", e);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            throw new JobExecutionException("Update " + DataReposVersionManager.XGVERSIONTABLE_NAME +
                " has error-", e);
        } finally {
            if (dataStore != null) dataStore.dispose();
        }
    }

    private boolean setFeatureConfBBoxFromNative(Envelope bboxNative, FeatureTypeConfig typeConfig, FeatureType featureType)
        throws JobExecutionException {
        if ((bboxNative == null) || (bboxNative.isNull())) return false;

        try {
            String srcSRS = EPSG + typeConfig.getSRS();
            CoordinateReferenceSystem crsDeclared = CRS.decode(srcSRS);
            CoordinateReferenceSystem original = null;

            if (featureType.getDefaultGeometry() != null) {
                original = featureType.getDefaultGeometry().getCoordinateSystem();
            }

            if (original == null) {
                original = crsDeclared;
            }

            CoordinateReferenceSystem crsLatLong = CRS.decode("EPSG:4326"); // latlong
            // let's show coordinates in the declared crs, not in the native one, to
            // avoid confusion (since on screen we do have the declared one, the native is
            // not visible)
            Envelope declaredEnvelope = bboxNative;

            if (!CRS.equalsIgnoreMetadata(original, crsDeclared)) {
                MathTransform xform = CRS.findMathTransform(original, crsDeclared, true);
                declaredEnvelope = JTS.transform(bboxNative, null, xform, 10); //convert data bbox to lat/long
            }

            typeConfig.setNativeBBox(declaredEnvelope);

            MathTransform xform = CRS.findMathTransform(original, crsLatLong, true);
            Envelope xformed_envelope = JTS.transform(bboxNative, xform); //convert data bbox to lat/long
            typeConfig.setLatLongBBox(xformed_envelope);
            return true;
        } catch (FactoryException e) {
            LOGGER.log(Level.INFO, e.getMessage(), e);
            return false;
        } catch (TransformException e) {
            LOGGER.log(Level.INFO, e.getMessage(), e);
            return false;
        }
    }
    */

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
    /*
    private boolean checkCurrentRepositoryStatus(DataStore dataStore, short status) {
        try {
            if (dataStore instanceof PostgisDataStore) {
                PostgisDataStore pgDataStore = (PostgisDataStore) dataStore;
                DataSource dataSource = pgDataStore.getDataSource();
                Connection connection = dataSource.getConnection();
                return checkCurrentRepositoryStatus(connection, status);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
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
    */

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
                            if (createFeatureTypeConfig(dataConfig, dataStoreConf, dataStore, styles,
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