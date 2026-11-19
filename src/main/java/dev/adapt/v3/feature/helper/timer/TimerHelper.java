package dev.adapt.v3.feature.helper.timer;

public final class TimerHelper {
    public static final TimerHelper INSTANCE = new TimerHelper();
    public float timerSpeed = 1.0F;

    public static TimerHelper getInstance() {
        return INSTANCE;
    }

    public float getTimerSpeed() {
        return this.timerSpeed;
    }

    public void setTimerSpeed(float timerSpeed) {
        this.timerSpeed = timerSpeed > 0.0F ? timerSpeed : 1.0F;
    }

    public void reset() {
        this.timerSpeed = 1.0F;
    }
}
