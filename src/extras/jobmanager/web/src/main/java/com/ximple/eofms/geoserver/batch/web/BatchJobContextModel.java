package com.ximple.eofms.geoserver.batch.web;

import java.util.logging.Logger;

import com.ximple.eofms.geoserver.batch.job.BatchJobContext;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geotools.util.logging.Logging;
import org.quartz.JobKey;
import org.quartz.TriggerKey;

/**
 *
 */
public class BatchJobContextModel extends LoadableDetachableModel<BatchJobContext> {
    static Logger LOGGER = Logging.getLogger(BatchJobContextModel.class);

    private JobKey jobKey;
    private TriggerKey triggerKey;

    public BatchJobContextModel(BatchJobContext object) {
        jobKey = object.getJobKey();
        triggerKey = object.getTriggerKey();
    }

    @Override
    protected BatchJobContext load() {
        return JobManagerWebUtils.jobManager().getJobContext(jobKey, triggerKey);
    }
}
