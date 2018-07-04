package ch.psi.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeoutException;

/**
 * Implement monotonic time counting and timeout checking.
 */
public class Chrono {

    long start;
    long timestamp;
    long nanosOffset;

    public Chrono() {
        start = System.nanoTime();              //Monotonic, used for measureing ellapsed time
        timestamp = System.currentTimeMillis();
    }

    public Chrono(long initialTimestamp) {
        start = System.nanoTime() - ((System.currentTimeMillis() - initialTimestamp) * 1000000);
        timestamp = initialTimestamp;
    }

    public Chrono(long initialTimestamp, long nanosOffset) {
        start = System.nanoTime() - ((System.currentTimeMillis() - initialTimestamp) * 1000000 - nanosOffset);
        timestamp = initialTimestamp;
        this.nanosOffset = nanosOffset;
    }

    public int getEllapsed() {
        return Math.max((int) ((System.nanoTime() - start) / 1000000), 0);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getNanosOffset() {
        return nanosOffset;
    }

    public long getTimestampNanos() {
        return (timestamp * 1000000) + nanosOffset;
    }

    public String getEllapsedStr(String mask) {
        return getEllapsedStr(getEllapsed(), mask);
    }

    public boolean isTimeout(int timeout) {
        return (getEllapsed() > timeout);
    }

    /**
     * If timeout negative, then wait forever. 
     */
    public void checkTimeout(int timeout) throws TimeoutException {
        if (timeout >=0){
            if (isTimeout(timeout)) {
                throw new TimeoutException();
            }
        }
    }
    
    public void checkTimeout(int timeout, String errorMessage) throws TimeoutException {
        if (timeout >=0){
            if (isTimeout(timeout)) {
                throw new TimeoutException(errorMessage);
            }
        }
    }    


    public boolean waitTimeout(int timeout) throws InterruptedException {
        int sleep = timeout - getEllapsed();
        if (sleep < 0) {
            return false;
        }
        if (sleep > 0) {
            Thread.sleep(sleep);
        }
        return true;
    }

    public void waitCondition(Condition condition, int timeout) throws TimeoutException, InterruptedException {
        waitCondition(condition, timeout, 1);
    }

    /**
     * Negative timeout: wait forever 
     */
    public void waitCondition(Condition condition, int timeout, int sleepInterval) throws TimeoutException, InterruptedException {
        while (!condition.evaluate()) {
            checkTimeout(timeout);
            Thread.sleep(sleepInterval);
        }
    }

    /**
     * Negative timeout: wait forever 
     */
    public void waitCondition(Object lock, Condition condition, int timeout) throws TimeoutException, InterruptedException {
        int wait = Math.max(timeout, 0);
        while (!condition.evaluate()) {
            synchronized (lock) {
                lock.wait(wait);
            }
            if (timeout>=0){
                if (wait > 0) {
                    wait = timeout - getEllapsed();
                    if (wait <= 0) {
                        throw new TimeoutException();
                    }
                }
            }
        }
    }

    public static String getEllapsedStr(Integer millis, String mask) {
        if ((millis == null) || (millis <= 0)) {
            return "";
        }
        millis = millis - java.util.TimeZone.getDefault().getOffset(millis);
        return new SimpleDateFormat("HH:mm:ss").format(millis);
    }

    public static String getTimeStr(Long millis, String mask) {
        if ((millis == null) || (millis <= 0)) {
            return "";
        }
        Date time = new Date(millis);
        SimpleDateFormat date_format = new SimpleDateFormat(mask);
        return date_format.format(time);
    }
}
