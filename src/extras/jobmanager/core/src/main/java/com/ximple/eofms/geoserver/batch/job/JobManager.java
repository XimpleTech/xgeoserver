package com.ximple.eofms.geoserver.batch.job;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 *
 */
public class JobManager implements DisposableBean, ApplicationListener {

    static Logger LOGGER = Logging.getLogger(JobManager.class);

    private Scheduler scheduler;

    public JobManager(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public List<BatchJobContext> fetchJobContext() {
        if (scheduler == null) return null;
        LinkedList<BatchJobContext> result = new LinkedList<BatchJobContext>();
        try {
            Set<JobKey> jobset = scheduler.getJobKeys(GroupMatcher.anyJobGroup());
            for (JobKey k : jobset) {
                JobDetail jobDetail = scheduler.getJobDetail(k);
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(k);
                for (Trigger t : triggers) {
                    if (t instanceof SimpleTrigger) {
                        if (jobDetail != null) {
                            BatchJobContext jobContext = new BatchJobContext(k, t.getKey());
                            jobContext.setLastRun(t.getPreviousFireTime());
                            jobContext.setNextRun(t.getNextFireTime());

                            result.add(jobContext);
                        }
                    }
                }
            }

        } catch (SchedulerException e) {
            LOGGER.log(Level.WARNING, "BatchJobManger has exception:" + e.getMessage(), e);
            return null;
        }
        return result;
    }

    public BatchJobContext getJobContext(JobKey jobKey, TriggerKey triggerKey) {

        try {
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            Trigger trigger = scheduler.getTrigger(triggerKey);
            if ((jobDetail != null) && (trigger != null)) {
                BatchJobContext jobContext = new BatchJobContext(jobKey, triggerKey);
                jobContext.setLastRun(trigger.getPreviousFireTime());
                jobContext.setNextRun(trigger.getNextFireTime());
                return jobContext;
            }
        } catch (SchedulerException e) {
            LOGGER.log(Level.WARNING, "BatchJobManger has exception:" + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
    }

    @Override
    public void destroy() throws Exception {
    }

    public void executeJob(BatchJobContext batchJobContext) {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.log(Level.INFO, "Try to fire batch - trigger=" + batchJobContext.getTriggerName() + "group=" +
                batchJobContext.getTriggerGroupName() + "job=" + batchJobContext.getJobName());
        }
        try {
            Trigger trigger = scheduler.getTrigger(batchJobContext.getTriggerKey());
            if (trigger instanceof SimpleTrigger) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("scheduler.scheduleJob()-" + trigger.toString());
                }

                if (trigger.getKey().getName().equalsIgnoreCase("resetDataTrigger")) {
                    XGeoResetDataConfigJob.getTriggerControl().setSkipNextTime(false);
                } else if (trigger.getKey().getName().equalsIgnoreCase("simpleTrigger")) {
                    XGeosGarbageCollectorJob.getTriggerControl().setSkipNextTime(false);
                } else if (trigger.getKey().getName().equalsIgnoreCase("importDataTrigger")) {
                }
            }
        } catch (SchedulerException e) {
            LOGGER.log(Level.WARNING, "BatchJobManger.executeJob has exception:" + e.getMessage(), e);
        }
    }
}
