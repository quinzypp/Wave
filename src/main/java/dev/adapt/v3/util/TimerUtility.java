package dev.adapt.v3.util;

public final class TimerUtility {
    public long lastMs;

    public TimerUtility(long time) {
        this.lastMs = time;
    }

    public TimerUtility() {
        this.reset();
    }

    public void reset() {
        this.lastMs = System.currentTimeMillis();
    }

    public boolean hasTimeElapsed(long delayMs, boolean flag) {
        if (this.getElapsedTime() > delayMs) {
            if (flag) {
                this.reset();
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean hasTimeElapsed(long delayMs) {
        return this.hasTimeElapsed(delayMs, false);
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - this.lastMs;
    }

    public void setLastMs(long lastMs) {
        this.lastMs = lastMs;
    }
}
