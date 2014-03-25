package com.ximple.eofms.geoserver.batch.job;

public final class SimpleTriggerControl {
    /*
    private static SimpleTriggerControl _instance;

    public static SimpleTriggerControl getInstance()
    {
        if (_instance == null) _instance = new SimpleTriggerControl();
        return _instance;
    }
    */

    private boolean skipNextTime = true;
    private boolean autoReset = true;
    private boolean defaultValue = true;
    private boolean running = false;

    public SimpleTriggerControl() {
    }

    public synchronized boolean isSkipNextTime() {
        return skipNextTime;
    }

    public synchronized void setSkipNextTime(boolean skipNextTime) {
        this.skipNextTime = skipNextTime;
    }

    public synchronized boolean isAutoReset() {
        return autoReset;
    }

    public synchronized void setAutoReset(boolean autoReset) {
        this.autoReset = autoReset;
    }

    public synchronized boolean isDefaultValue() {
        return defaultValue;
    }

    public synchronized void setDefaultValue(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    public synchronized void reset() {
        if (autoReset) {
            running = false;
            skipNextTime = defaultValue;
        }
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public synchronized void setRunning() {
        running = true;
    }
}