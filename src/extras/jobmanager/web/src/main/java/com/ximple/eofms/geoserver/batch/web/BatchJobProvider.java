package com.ximple.eofms.geoserver.batch.web;

import java.util.Arrays;
import java.util.List;

import com.ximple.eofms.geoserver.batch.job.BatchJobContext;
import org.apache.wicket.model.IModel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

/**
 *
 */
public class BatchJobProvider extends GeoServerDataProvider<BatchJobContext> {
    public static Property<BatchJobContext> JOBNAME = new BeanProperty("jobName", "jobKey.name");
    public static Property<BatchJobContext> JOBGROUP = new BeanProperty("jobGroup", "jobKey.group");
    public static Property<BatchJobContext> TRIGGERNAME = new BeanProperty("triggerName", "triggerKey.name");
    public static Property<BatchJobContext> TRIGGERGROUP = new BeanProperty("triggerGroup", "triggerKey.group");

    @Override
    protected List<Property<BatchJobContext>> getProperties() {
        return Arrays.asList(JOBNAME, JOBGROUP, TRIGGERNAME, TRIGGERGROUP);
    }

    @Override
    protected List<BatchJobContext> getItems() {
        return JobManagerWebUtils.jobManager().fetchJobContext();
    }

    @Override
    protected IModel newModel(Object object) {
        return new BatchJobContextModel((BatchJobContext) object);
    }
}
