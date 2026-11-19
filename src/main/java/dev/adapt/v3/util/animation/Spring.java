package dev.adapt.v3.util.animation;

public final class Spring {
    public float value;
    public float velocity;
    public float target;
    public float stiffness;
    public float damping;
    public long lastNanos = -1L;

    public Spring(float value, float value2, float value3) {
        this.value = value;
        this.target = value;
        this.stiffness = value2;
        this.damping = value3;
    }

    public static Spring smooth(float value) {
        return new Spring(value, 220.0F, 22.0F);
    }

    public static Spring snappy(float value) {
        return new Spring(value, 900.0F, 60.0F);
    }

    public static Spring bouncy(float value) {
        return new Spring(value, 260.0F, 14.0F);
    }

    public static Spring gentle(float value) {
        return new Spring(value, 90.0F, 16.0F);
    }

    public Spring target(float value) {
        this.target = value;
        return this;
    }

    public float update() {
        long i = System.nanoTime();
        if (this.lastNanos < 0L) {
            this.lastNanos = i;
            return this.value;
        }

        float f = (float)(i - this.lastNanos) / 1.0E9F;
        this.lastNanos = i;
        if (f > 0.05F) {
            f = 0.05F;
        }

        int j = Math.max(1, (int)Math.ceil(f / 0.004F));
        float f1 = f / j;

        for (int k = 0; k < j; k++) {
            float f2 = -this.stiffness * (this.value - this.target) - this.damping * this.velocity;
            this.velocity += f2 * f1;
            this.value = this.value + this.velocity * f1;
        }

        if (Math.abs(this.value - this.target) < 5.0E-4F && Math.abs(this.velocity) < 5.0E-4F) {
            this.value = this.target;
            this.velocity = 0.0F;
        }

        return this.value;
    }

    public float get() {
        return this.value;
    }

    public float getTarget() {
        return this.target;
    }

    public void snap(float value) {
        this.value = value;
        this.target = value;
        this.velocity = 0.0F;
    }

    public void impulse(float value) {
        this.velocity += value;
    }

    public boolean isSettled() {
        return this.value == this.target && this.velocity == 0.0F;
    }
}
