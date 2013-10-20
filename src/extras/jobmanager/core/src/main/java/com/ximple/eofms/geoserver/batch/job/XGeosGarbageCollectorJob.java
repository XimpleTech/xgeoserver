package com.ximple.eofms.geoserver.batch.job;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleTrigger;

public class XGeosGarbageCollectorJob extends GeoserverQuartzJobBean {
    private static SimpleTriggerControl simpleControl = new SimpleTriggerControl();

    private static final int MaxRestoreJvmLoops = 10;
    private static final long PauseTime = 1000;

    public static SimpleTriggerControl getTriggerControl() {
        return simpleControl;
    }

    public XGeosGarbageCollectorJob() {
        simpleControl.setAutoReset(true);
    }

    protected void executeInternal(JobExecutionContext executionContext) throws JobExecutionException {
        if (LOGGER.isLoggable(Level.FINER))
            LOGGER.finer("XGeosGarbageCollectorJob execute.");

        if (executionContext.getTrigger() instanceof SimpleTrigger) {
            if ((!simpleControl.isSkipNextTime()) && (!simpleControl.isRunning())) {
                try {
                    long memUsedPrev = Long.MAX_VALUE;
                    long memUsedNow = getMemoryUsed();
                    for (int i = 0; i < MaxRestoreJvmLoops; i++) {

                        System.runFinalization();
                        System.gc();
                        TimeUnit.MILLISECONDS.sleep(PauseTime);
                        if (memUsedPrev > memUsedNow) {
                            memUsedPrev = memUsedNow;
                            memUsedNow = getMemoryUsed();
                        } else {
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                } finally {
                    simpleControl.reset();
                }
            }
        }
    }

    public long getMemoryUsed() {
        long freeMemory = Runtime.getRuntime().freeMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();

        long memoryUsed = totalMemory - freeMemory;
        return memoryUsed;
    }
}