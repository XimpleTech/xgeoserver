package com.ximple.eofms.geoserver.batch.job;

import java.io.File;

import org.geoserver.catalog.Catalog;
import org.vfny.geoserver.global.ConfigurationException;
import org.vfny.geoserver.global.GeoserverDataDirectory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;

public abstract class GeoserverConfigJobBean extends GeoserverQuartzJobBean {
    private static final String PLUG_INS = "plugIns";
    private static final String VALIDATION = "validation";

    /*
    protected void updateGeoserver(JobExecutionContext context) throws JobExecutionException {
        try {
            WCSDTO wcsDTO = getWCSConfig(context).toDTO();
            WMSDTO wmsDTO = getWMSConfig(context).toDTO();
            WFSDTO wfsDTO = getWFSConfig(context).toDTO();
            GeoServerDTO geoserverDTO = getGlobalConfig(context).toDTO();
            DataDTO dataDTO = getDataConfig(context).toDTO();
            //we're updating...increment the updateSequence
            final int gsUs = geoserverDTO.getUpdateSequence();
            geoserverDTO.setUpdateSequence(gsUs + 1);

            //load each service global bean from the modified config DTO
            getWCS(context).load(wcsDTO);
            getWFS(context).load(wfsDTO);
            getWMS(context).load(wmsDTO);

            //also, don't forget to update the main global config with the changes to the updatesequence
            getGlobalConfig(context).update(geoserverDTO);

            //load the main geoserver bean from the modified config DTO
            getWCS(context).getGeoServer().load(geoserverDTO);
            //load the data bean from the modified config DTO
            getWFS(context).getData().load(dataDTO);
        } catch (ConfigurationException e) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, e.getMessage(), e);
            }

            throw new JobExecutionException(e);
        }

        // We need to stay on the same page!
        getApplicationState(context).notifyToGeoServer();
    }

    protected void updateValidation(JobExecutionContext context) throws JobExecutionException {
        try {
            Map plugins = new HashMap();
            Map testSuites = new HashMap();

            if (getValidationConfig(context).toDTO(plugins, testSuites)) {
                //sorry, no time to really test this, but I got a null pointer
                //exception with the demo build target. ch
                if (getWFS(context).getValidation() != null) {
                    getWFS(context).getValidation().load(testSuites, plugins);
                }
            } else {
                throw new ConfigurationException(
                    "ValidationConfig experienced an error exporting Data Transpher Objects.");
            }
        } catch (ConfigurationException e) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, e.getMessage(), e);
            }
            throw new JobExecutionException(e);
        }

        // We need to stay on the same page!
        getApplicationState(context).notifyToGeoServer();
    }
    */
    /*
    protected void saveGeoserver(JobExecutionContext context) throws JobExecutionException {
        File rootDir = GeoserverDataDirectory.getGeoserverDataDirectory();

        try {
            XMLConfigWriter.store((WCSDTO) getWCS(context).toDTO(),
                (WMSDTO) getWMS(context).toDTO(), (WFSDTO) getWFS(context).toDTO(),
                (GeoServerDTO) getWFS(context).getGeoServer().toDTO(),
                (DataDTO) getWFS(context).getData().toDTO(), rootDir);
        } catch (ConfigurationException e) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, e.getMessage(), e);
            }
            throw new JobExecutionException(e);
        }
    }

    protected void saveValidation(JobExecutionContext context) throws JobExecutionException {
        File rootDir = GeoserverDataDirectory.getGeoserverDataDirectory();

        File dataDir;

        dataDir = rootDir;

        File plugInDir;
        File validationDir;

        try {
            plugInDir = XMLConfigWriter.WriterUtils.initWriteFile(new File(dataDir, PLUG_INS), true);
            validationDir = XMLConfigWriter.WriterUtils.initWriteFile(new File(dataDir, VALIDATION), true);
        } catch (ConfigurationException e) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, e.getMessage(), e);
            }
            throw new JobExecutionException(e);
        }

        //Map plugIns = (Map) getWFS(context).getValidation().toPlugInDTO();
        //Map testSuites = (Map) getWFS(context).getValidation().toTestSuiteDTO();

        getApplicationState(context).notifiySaveXML();
    }
    */

    /*
    protected void loadGeoserver(JobExecutionContext context) throws JobExecutionException {
        WMSDTO wmsDTO = null;
        WFSDTO wfsDTO = null;
        WCSDTO wcsDTO = null;
        GeoServerDTO geoserverDTO = null;
        DataDTO dataDTO = null;

        //DJB: changed for geoserver_data_dir
        // File rootDir = new File(sc.getRealPath("/"));
        File rootDir = GeoserverDataDirectory.getGeoserverDataDirectory();

        XMLConfigReader configReader;

        try {
            configReader = new XMLConfigReader(rootDir, null);
        } catch (ConfigurationException configException) {
            configException.printStackTrace();
            return;
        }

        if (configReader.isInitialized()) {
            // These are on separate lines so we can tell with the
            // stack trace/debugger where things go wrong
            wmsDTO = configReader.getWms();
            wfsDTO = configReader.getWfs();
            wcsDTO = configReader.getWcs();
            geoserverDTO = configReader.getGeoServer();
            dataDTO = configReader.getData();
        } else {
            System.err.println("Config Reader not initialized for LoadXMLAction.execute().");
            return;
        }

        // Update GeoServer
        try {
            getWCS(context).load(wcsDTO);
            getWFS(context).load(wfsDTO);
            getWMS(context).load(wmsDTO);
            getWCS(context).getGeoServer().load(geoserverDTO);
            getWCS(context).getData().load(dataDTO);
            getWFS(context).getGeoServer().load(geoserverDTO);
            getWFS(context).getData().load(dataDTO);
        } catch (ConfigurationException configException) {
            configException.printStackTrace();
            return;
        }

        // Update Config
        getGlobalConfig(context).update(geoserverDTO);
        getDataConfig(context).update(dataDTO);
        getWCSConfig(context).update(wcsDTO);
        getWFSConfig(context).update(wfsDTO);
        getWMSConfig(context).update(wmsDTO);

        getApplicationState(context).notifyLoadXML();
    }

    protected void loadValidation(JobExecutionContext context) throws JobExecutionException {
        WFS wfs = getWFS(context);

        if (wfs == null) {
            // lazy creation on load?
            loadGeoserver(context);
        }

        //CH- fixed for data dir, looks like this got missed first time around.
        File rootDir = GeoserverDataDirectory.getGeoserverDataDirectory();

        try {
            File plugInDir = findConfigDir(rootDir, PLUG_INS);
            File validationDir = findConfigDir(rootDir, VALIDATION);
            Map plugIns = XMLReader.loadPlugIns(plugInDir);
            Map testSuites = XMLReader.loadValidations(validationDir, plugIns);
            ValidationConfig vc = new ValidationConfig(plugIns, testSuites);

            // sc.setAttribute(ValidationConfig.CONFIG_KEY, vc);
            ApplicationContext appCtx = getApplicationContext(context);
            if (appCtx instanceof WebApplicationContext) {
                WebApplicationContext webCtx = (WebApplicationContext) appCtx;
                ServletContext srvCtx = webCtx.getServletContext();
                srvCtx.setAttribute(ValidationConfig.CONFIG_KEY, vc);
            }
        } catch (Exception e) {
            // LOG error
            e.printStackTrace();

            return;
        }
    }
    */

    protected File findConfigDir(File rootDir, String name) throws Exception {
        return GeoserverDataDirectory.findConfigDir(rootDir, name);
    }

    /*
    protected Catalog getCatalog() {
        if (getGeoServer() == null) return null;
        return geoServer.getCatalog();
    }
    */
}