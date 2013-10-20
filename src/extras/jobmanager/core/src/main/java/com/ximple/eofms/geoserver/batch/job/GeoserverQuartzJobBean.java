package com.ximple.eofms.geoserver.batch.job;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.config.GeoServer;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.SchedulerException;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public abstract class GeoserverQuartzJobBean extends QuartzJobBean {
    protected static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("com.ximple.eofms.geoserver.batch");
    private static final String APPLICATION_CONTEXT_KEY = "applicationContext";
    protected static final String APPLICATION_STATE = "applicationState";
    private static final String DATA_CONFIG = "dataConfig";
    private static final String GLOBAL_CONFIG = "globalConfig";
    private static final String WMS_CONFIG = "wmsConfig";
    private static final String WCS_CONFIG = "wcsConfig";
    private static final String WFS_CONFIG = "wfsConfig";
    private static final String WMS_INSTANCE = "wms";

    protected GeoServer geoServer;

    protected abstract void executeInternal(JobExecutionContext executionContext) throws JobExecutionException;

    protected ApplicationContext getApplicationContext(JobExecutionContext context) throws JobExecutionException {
        ApplicationContext appCtx = null;
        try {
            appCtx = (ApplicationContext) context.getScheduler().getContext().get(APPLICATION_CONTEXT_KEY);
        } catch (SchedulerException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }

        if (appCtx == null) {
            LOGGER.info("ApplicationContext is null.");
            throw new JobExecutionException(
                "No application context available in scheduler context for key \"" + APPLICATION_CONTEXT_KEY + "\"");
        }
        return appCtx;
    }

    public void setGeoServer(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    /*
    protected DataConfig getDataConfig(JobExecutionContext context) throws JobExecutionException {
        return (DataConfig) getApplicationContext(context).getBean(DATA_CONFIG);
    }

    protected GlobalConfig getGlobalConfig(JobExecutionContext context) throws JobExecutionException {
        return (GlobalConfig) getApplicationContext(context).getBean(GLOBAL_CONFIG);
    }
    */
}