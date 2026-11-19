package dev.adapt.v3.util.animation;

public final class Animation {
    public final Easing easing;
    public long duration;
    public long startTime;
    public float startValue;
    public float endValue;
    public float value;
    public boolean finished;

    public Animation(Easing easing, long time) {
        this.easing = easing;
        this.startTime = System.currentTimeMillis();
        this.duration = time;
    }

    public void run(float value) {
        long i = System.currentTimeMillis();
        if (this.endValue != value) {
            this.endValue = value;
            this.reset();
        } else {
            this.finished = i - this.duration > this.startTime || this.value == value;
            if (this.finished) {
                this.value = value;
                return;
            }
        }

        float f = this.easing.getFunction().apply(Math.min(1.0F, this.getProgress()));
        if (this.duration == 0L) {
            this.value = value;
        } else {
            this.value = this.startValue + (value - this.startValue) * f;
        }

        if (Float.isNaN(this.value) || !Float.isFinite(this.value)) {
            this.value = value;
        }
    }

    public float getProgress() {
        return (float)(System.currentTimeMillis() - this.startTime) / (float)this.duration;
    }

    public void reset() {
        this.startTime = System.currentTimeMillis();
        this.startValue = this.value;
        this.finished = false;
    }

    public float getValue() {
        return this.value;
    }

    public void setValue(float value) {
        this.value = value;
        this.startValue = value;
        this.endValue = value;
    }

    public boolean isFinished() {
        return this.finished;
    }
}
