package org.aanguita.jacuzzi.concurrency;

import org.aanguita.jacuzzi.id.AlphaNumFactory;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * This class provides simple stop/let go functionality to threads. SimpleSemaphore has two methods for opening or
 * closing access. Access threads invoke a method that is non-blocking (returns immediately) if access is open, and
 * is blocking if access is closed (until a controller thread opens access again)
 * <p>
 * The simple semaphore does not maintain the number of available permits or acquired stops. It just acts as an
 * open/close barrier
 */
public class Barrier {

    private static class OnTimeout implements Consumer<String> {

        private final Thread threadToInterrupt;

        private OnTimeout(Thread threadToInterrupt) {
            this.threadToInterrupt = threadToInterrupt;
        }

        @Override
        public void accept(String alertName) {
            threadToInterrupt.interrupt();
        }
    }

    /**
     * All {@link Barrier ) instances use the same time alert
     */
    private static final String TIMED_ALERT_ID = "SIMPLE_SEMAPHORE_TIME_ALERT";

    /**
     * Semaphore used to control the execution flow
     */
    private Semaphore semaphore;

    /**
     * Flag for controlling when the element is paused
     */
    private boolean paused;

    public Barrier() {
        this(false);
    }

    public Barrier(boolean fairness) {
        semaphore = new Semaphore(1, fairness);
        paused = false;
    }

    /**
     * Pauses the element. Further invocations to the access method will be blocked until someone resumes the element.
     * The element can be paused more times, with no effect.
     */
    public void pause() {
        // if the lock isn't currently acquired, then it must be acquired. Otherwise, leave it paused (so this
        // invocation never blocks)
        synchronized (this) {
            if (!paused) {
                semaphore.acquireUninterruptibly();
                paused = true;
            }
        }
    }

    /**
     * Resumes the element. If the element was paused, all accessions will be allowed again. Further invocations to
     * this method will have no effect. This element can be resumed by any thread, even if it did not pause the
     * element previously
     */
    public void resume() {
        // if the lock is currently acquired, then release it. Otherwise leave it unlocked (so this
        // invocation never blocks)
        synchronized (this) {
            if (paused) {
                semaphore.release();
                paused = false;
            }
        }
    }

    /**
     * This method makes the invoking thread access the pausable element. If the element is currently paused, the
     * thread will be blocked until some other thread resumes the element. If the element is not paused, this method
     * will return immediately.
     * <p/>
     * If fairness is used, upon resume, blocked accesses will be executed in order of arrival
     */
    public void access() {
        semaphore.acquireUninterruptibly();
        semaphore.release();
    }

    /**
     * This method makes the invoking thread access the pausable element. If the element is currently paused, the
     * thread will be blocked until some other thread resumes the element, or the timeout fires. If the element is not paused, this method
     * will return immediately.
     * <p/>
     * If fairness is used, upon resume, blocked accesses will be executed in order of arrival
     *
     * @param timeout: the time in millis to wait before a timeout exception kicks. If 0 or negative, the timeout
     *                 exception is thrown directly
     * @throws TimeoutException if the pausable element cannot be accessed before the given timeout passes, or if timeout is equals or less than zero
     */
    public void access(long timeout) throws TimeoutException {
        if (timeout > 0) {
            try {
                String alertName = this.getClass().getName() + "-" + AlphaNumFactory.getStaticId();
                TimeAlert.getInstance(TIMED_ALERT_ID).addAlert(alertName, timeout, new OnTimeout(Thread.currentThread()));
                semaphore.acquire(1);
                TimeAlert.getInstance(TIMED_ALERT_ID).removeAlert(alertName);
                semaphore.release();
            } catch (InterruptedException e) {
                // timeout was fired.
                throw new TimeoutException();
            }
        } else {
            throw new TimeoutException();
        }
    }

    @Override
    public String toString() {
        return "SimpleSemaphore open=" + !paused;
    }
}