import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mobius.software.common.dal.timers.Timer;
import com.mobius.software.common.dal.timers.WorkerPool;

public class TimerTask implements Timer {
    private static Logger logger = LogManager.getLogger(TimerTask.class);

    private WorkerPool workerPool;
    private long startTime;
    private AtomicLong timestamp;
    private AtomicLong period;
    

    public TimerTask(WorkerPool workerPool, long timeout, long period) {
        this.workerPool = workerPool;
        this.startTime = System.currentTimeMillis();
        this.timestamp = new AtomicLong(System.currentTimeMillis() + timeout);        
        this.period = new AtomicLong(period);
    }

    @Override
    public void execute() {
        logger.debug("Executing local task of type " + this.getClass().getCanonicalName());
        if (timestamp.get() < Long.MAX_VALUE) {
            if(period.get() > 0) {
                timestamp.set(System.currentTimeMillis() + period.get());
                this.workerPool.getPeriodicQueue().store(timestamp.get(), this);
            } else {
                timestamp.set(Long.MAX_VALUE);
            }
        }
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public Long getRealTimestamp() {
        return timestamp.get();
    }

    @Override
    public void stop() {
        timestamp.set(Long.MAX_VALUE);
        period.set(-1);
    }

    @Override
    public Integer getQueueIndex() {
        return Math.abs(this.hashCode()) % 1;        
    }
    
}
