package com.ximple.eofms.geoserver.batch.web;

import com.ximple.eofms.geoserver.batch.job.JobManager;
import org.apache.wicket.Application;
import org.geoserver.web.GeoServerApplication;

/**
 *
 */
public class JobManagerWebUtils {
    static JobManager jobManager() {
        return GeoServerApplication.get().getBeanOfType(JobManager.class);
    }

    static boolean isDevMode() {
        return Application.DEVELOPMENT.equalsIgnoreCase(GeoServerApplication.get().getConfigurationType());
    }
}
