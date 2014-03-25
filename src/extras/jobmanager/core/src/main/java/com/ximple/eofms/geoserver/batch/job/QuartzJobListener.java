package com.ximple.eofms.geoserver.batch.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

/**
 *
 */
public class QuartzJobListener implements JobListener {
    @Override
    public String getName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
