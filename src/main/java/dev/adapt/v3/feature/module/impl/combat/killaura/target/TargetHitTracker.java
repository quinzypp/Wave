package dev.adapt.v3.feature.module.impl.combat.killaura.target;

import dev.adapt.v3.util.TimerUtility;

public final class TargetHitTracker {
    public final TimerUtility timer = new TimerUtility();
    public double lastDamage;

    public TargetHitTracker(double value) {
        this.update(false, value);
    }

    public void update(boolean flag, double value) {
        if (flag) {
            this.timer.reset();
        }

        this.lastDamage = value;
    }

    public long getElapsedMs() {
        return this.timer.getElapsedTime();
    }

    public double getLastDamage() {
        return this.lastDamage;
    }
}
