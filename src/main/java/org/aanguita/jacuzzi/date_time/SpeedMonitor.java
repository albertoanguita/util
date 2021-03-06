package org.aanguita.jacuzzi.date_time;

import org.aanguita.jacuzzi.concurrency.ThreadUtil;
import org.aanguita.jacuzzi.concurrency.task_executor.ThreadExecutor;
import org.aanguita.jacuzzi.concurrency.timer.Timer;
import org.aanguita.jacuzzi.concurrency.timer.TimerAction;
import org.aanguita.jacuzzi.lists.tuple.Duple;
import org.aanguita.jacuzzi.numeric.range.LongRange;
import org.aanguita.jacuzzi.numeric.range.Range;
import org.aanguita.jacuzzi.queues.TimedQueue;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class contains logic and methods that allow measuring the speed of a process. The progress of a process is
 * measured by the quantity achieved (a long). The measure process can be set up to consider a specific amount of
 * past time (for example, average speed in the last 10 minutes).
 */
public class SpeedMonitor implements TimerAction, TimedQueue.TimedQueueInterface<Long> {

    /**
     * Time mark when measure process was initiated
     */
    protected final long initialTimeMark;

    /**
     * This variable indicates whether we are currently still within the time designated to measure (e.g. time to
     * measure is 10 seconds and the object was created 5 seconds ago). It is only useful in the average speed
     * calculation
     */
    protected boolean outOfInitialRange;

    /**
     * This list represents the progressed quantities, together with their time marks. The first element is always
     * the oldest one. By rule, no elements older than the millisToStore value are stored (if an average speed
     * measure for longer than that is asked, it is extrapolated)
     */
    protected TimedQueue<Long> progress;

    /**
     * This field stores the maxSize of the currently stored elements (the sum of their sizes). Storing this value
     * allows a faster speed calculation
     */
    protected long storedSize;

    /**
     * The registered speed monitor (if any). null if no monitor is registered. This monitor will have to be invoked
     * when the speed is out of some specified range
     */
    private SpeedMonitorAction speedMonitorAction;

    /**
     * Range of speeds to check (values outside this range will provoke notifications)
     */
    private LongRange speedMonitorRange;

    /**
     * Time allowed to pass between the detection of a speed anomaly and its corresponding notification). If 0, it is
     * directly notified without elapse of time
     */
    private int millisAllowedOutOfSpeedRange;

    /**
     * Timer for reporting above speed situations
     */
    private final Timer reportSpeedAboveTimer;

    /**
     * Timer for reporting below speed situations
     */
    private final Timer reportSpeedBelowTimer;

    /**
     * Value indicating if we just reported an above speed situation (to avoid redundant notifications)
     */
    private boolean justReportedAboveSpeed;

    /**
     * Value indicating if we just reported an below speed situation (to avoid redundant notifications)
     */
    private boolean justReportedBelowSpeed;

    private final AtomicBoolean alive;

    private final String threadExecutorClientId;


    public SpeedMonitor(long millisToStore) {
        this(millisToStore, null, null, -1);
    }

    public SpeedMonitor(long millisToStore, SpeedMonitorAction speedMonitorAction, LongRange speedMonitorRange, int millisAllowedOutOfSpeedRange) {
        this(millisToStore, speedMonitorAction, speedMonitorRange, millisAllowedOutOfSpeedRange, ThreadUtil.invokerName(1));
    }

    public SpeedMonitor(long millisToStore, SpeedMonitorAction speedMonitorAction, LongRange speedMonitorRange, int millisAllowedOutOfSpeedRange, String threadName) {
        initialTimeMark = System.currentTimeMillis();
        outOfInitialRange = false;
        progress = new TimedQueue<>(millisToStore, this, true, threadName);
        storedSize = 0;
        this.speedMonitorAction = speedMonitorAction;
        this.speedMonitorRange = speedMonitorRange;
        this.millisAllowedOutOfSpeedRange = millisAllowedOutOfSpeedRange;
        if (speedMonitorAction != null) {
            reportSpeedAboveTimer = new Timer(millisAllowedOutOfSpeedRange, this, false, threadName + "/reportSpeedAboveTimer");
            reportSpeedBelowTimer = new Timer(millisAllowedOutOfSpeedRange, this, false, threadName + "/reportSpeedBelowTimer");
        } else {
            reportSpeedAboveTimer = null;
            reportSpeedBelowTimer = null;
        }
        justReportedAboveSpeed = false;
        justReportedBelowSpeed = false;
        alive = new AtomicBoolean(true);
        threadExecutorClientId = ThreadExecutor.registerClient(this.getClass().getName() + "(" + threadName + ")");
    }

    public synchronized void setSpeedMonitorRange(LongRange newSpeedMonitorRange) {
        this.speedMonitorRange = newSpeedMonitorRange;
        checkSpeed(true);
        checkSpeed(false);
    }

    public synchronized void addProgress(long quantity) {
        progress.addElement(quantity);
        storedSize += quantity;
        // check (if necessary) that we have not surpassed the max speed
        if (speedMonitorAction != null) {
            // check only above limit, because after having added progress the speed is higher than before
            checkSpeed(true);
        }
    }

    public synchronized double getAverageSpeed() {
        Duple<Double, Long> speedAndTimeLapse = getAverageSpeedAndTimeLapse();
        return speedAndTimeLapse.element1;
    }

    public synchronized Duple<Double, Long> getAverageSpeedAndTimeLapse() {
        long currentTime = System.currentTimeMillis();
        if (outOfInitialRange) {
            return new Duple<>(1000d * storedSize / (double) progress.getMillisToStore(), progress.getMillisToStore());
        } else {
            if (currentTime > initialTimeMark + progress.getMillisToStore()) {
                outOfInitialRange = true;
                return getAverageSpeedAndTimeLapse();
            } else {
                // extrapolate
                if (currentTime == initialTimeMark) {
                    // make sure we don't get a divide by zero error
                    currentTime++;
                }
                return new Duple<>(1000d * (double) storedSize / ((double) currentTime - (double) initialTimeMark), currentTime - initialTimeMark);
            }
        }
    }

    @Override
    public synchronized Long wakeUp(Timer timer) {
        if (timer == reportSpeedAboveTimer) {
            justReportedAboveSpeed = true;
            double speed = getAverageSpeed();
            SpeedOutOfRangeTask spmTask = new SpeedOutOfRangeTask(speedMonitorAction, true, speed);
            ThreadExecutor.submit(spmTask);
            // kill the timer
            return 0L;
        } else if (timer == reportSpeedBelowTimer) {
            justReportedBelowSpeed = true;
            Double speed = getAverageSpeed();
            SpeedOutOfRangeTask spmTask = new SpeedOutOfRangeTask(speedMonitorAction, false, speed);
            ThreadExecutor.submit(spmTask);
            // kill the timer
            return 0L;
        }
        return 0L;
    }

    /**
     * Since this class uses timers to monitor some stuff, this method allows telling it that we are done using it,
     * so all timers can be deactivated. It is recommended to invoke this method, because in other case unexpected
     * reminder calls can be received after some period of time
     */
    public synchronized void stop() {
        if (alive.get()) {
            alive.set(false);
            progress.stop();
            if (reportSpeedAboveTimer != null) {
                reportSpeedAboveTimer.stop();
            }
            if (reportSpeedBelowTimer != null) {
                reportSpeedBelowTimer.stop();
            }
            ThreadExecutor.shutdownClient(threadExecutorClientId);
        }
    }

    private synchronized void checkSpeed(boolean above) {
        Double speed = getAverageSpeed();
        if (above) {
            // speed has raised: we must check if either just entered above limit, or we escaped from below limit
            if (speedMonitorRange.compareTo(speed.longValue()) == Range.ValueComparison.LEFT) {
                // above limit
                if (!justReportedAboveSpeed) {
                    if (millisAllowedOutOfSpeedRange >= 0) {
                        if (reportSpeedAboveTimer.isStopped()) {
                            //reportSpeedAboveTimer = new Timer<ComplexTimerEvent>(millisAllowedOutOfSpeedRange, this, ComplexTimerEvent.SPEED_ABOVE_LIMIT);
                            reportSpeedAboveTimer.reset();
                        }
                    } else {
                        justReportedAboveSpeed = true;
                        SpeedOutOfRangeTask spmTask = new SpeedOutOfRangeTask(speedMonitorAction, true, speed);
                        ThreadExecutor.submit(spmTask);
                    }
                }
            } else {
                justReportedAboveSpeed = false;
            }
            if (speedMonitorRange.compareTo(speed.longValue()) == Range.ValueComparison.CONTAINS || speedMonitorRange.compareTo(speed.longValue()) == Range.ValueComparison.LEFT) {
                // we are in the OK range or upper, check if we just left the below range
                if (reportSpeedBelowTimer != null && reportSpeedBelowTimer.isRunning()) {
                    reportSpeedBelowTimer.stop();
                }
            }
        } else {
            if (speedMonitorRange.compareTo(speed.longValue()) == Range.ValueComparison.RIGHT) {
                if (!justReportedBelowSpeed) {
                    if (millisAllowedOutOfSpeedRange >= 0) {
                        if (reportSpeedBelowTimer.isStopped()) {
                            reportSpeedBelowTimer.reset();
                        }
                    } else {
                        justReportedBelowSpeed = true;
                        SpeedOutOfRangeTask spmTask = new SpeedOutOfRangeTask(speedMonitorAction, false, speed);
                        ThreadExecutor.submit(spmTask);
                    }
                }
            } else {
                justReportedBelowSpeed = false;
            }
            if (speedMonitorRange.compareTo(speed.longValue()) == Range.ValueComparison.CONTAINS || speedMonitorRange.compareTo(speed.longValue()) == Range.ValueComparison.RIGHT) {
                // we are in the OK range or lower, check if we just left the above range
                if (reportSpeedAboveTimer != null && reportSpeedAboveTimer.isRunning()) {
                    reportSpeedAboveTimer.stop();
                }
            }
        }
    }

    @Override
    public synchronized void elementsRemoved(List<Long> elements) {
        for (long element : elements) {
            storedSize -= element;
        }
        if (speedMonitorAction != null) {
            checkSpeed(false);
        }
    }
}
