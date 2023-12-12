import java.util.concurrent.atomic.AtomicLong;

import com.mobius.software.common.dal.timers.Timer;

public class TimerTask implements Timer {

    private long startTime;
    private AtomicLong timestamp;
    private AtomicLong period;
    

    public TimerTask(long timeout, long period) {
        this.startTime = System.currentTimeMillis();
        this.timestamp = new AtomicLong(System.currentTimeMillis() + timeout);        
        this.period = new AtomicLong(period);
    }

    @Override
    public void execute() {
        
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
    
}
