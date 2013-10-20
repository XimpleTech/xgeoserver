package com.ximple.eofms.geoserver.batch.job;

import java.util.Date;

public class BatchJobContext {
    /** serialVersionUID */
    private static final long serialVersionUID = -1L;

    private String jobName;
    private String groupName;
    private String triggerName;
    private Date nextRun;
    private Date lastRun;

    public BatchJobContext() {
    }

    public BatchJobContext(String groupName, String jobName) {
        this.groupName = groupName;
        this.jobName = jobName;
    }

    public BatchJobContext(String groupName, String jobName, String triggerName) {
        this.groupName = groupName;
        this.jobName = jobName;
        this.triggerName = triggerName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
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
        return triggerName;
    }

    public void setTriggerName(String triggerName) {
        this.triggerName = triggerName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BatchJobContext that = (BatchJobContext) o;

        if (!groupName.equals(that.groupName)) return false;
        if (!jobName.equals(that.jobName)) return false;
        if (!triggerName.equals(that.triggerName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = jobName.hashCode();
        result = 31 * result + groupName.hashCode();
        result = 31 * result + triggerName.hashCode();
        return result;
    }
}
