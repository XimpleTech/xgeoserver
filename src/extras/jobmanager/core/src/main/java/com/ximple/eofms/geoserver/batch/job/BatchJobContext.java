package com.ximple.eofms.geoserver.batch.job;

import java.io.Serializable;
import java.util.Date;

import org.quartz.JobKey;
import org.quartz.TriggerKey;

public class BatchJobContext implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 1002869733533108325L;

    private JobKey jobKey;
    private TriggerKey triggerKey;
    private Date nextRun;
    private Date lastRun;

    public BatchJobContext() {
    }

    public BatchJobContext(JobKey jobKey, TriggerKey triggerKey) {
        this.jobKey = jobKey;
        this.triggerKey = triggerKey;
    }

    public BatchJobContext(String groupName, String jobName, String triggerGroupName, String triggerName) {
        this.jobKey = new JobKey(jobName, groupName);
        this.triggerKey = new TriggerKey(triggerName, triggerGroupName);
    }

    public String getGroupName() {
        return jobKey.getGroup();
    }

    public String getJobName() {
        return jobKey.getName();
    }

    public JobKey getJobKey() {
        return jobKey;
    }

    public void setJobKey(JobKey jobKey) {
        this.jobKey = jobKey;
    }

    public Date getLastRun() {
        return lastRun;
    }

    public void setLastRun(Date lastRun) {
        this.lastRun = lastRun;
    }

    public Date getNextRun() {
        return nextRun;
    }

    public void setNextRun(Date nextRun) {
        this.nextRun = nextRun;
    }

    public String getTriggerName() {
        return triggerKey.getName();
    }

    public String getTriggerGroupName() {
        return triggerKey.getGroup();
    }

    public void setTriggerKey(TriggerKey triggerKey) {
        this.triggerKey = triggerKey;
    }

    public TriggerKey getTriggerKey() {
        return triggerKey;
    }
}
