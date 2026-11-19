package dev.adapt.v3.notification;

import dev.adapt.v3.util.TimerUtility;
import dev.adapt.v3.util.animation.Spring;
import java.util.Objects;

public final class Notification {
    public final TimerUtility timer = new TimerUtility();
    public final NotificationType type;
    public final String title;
    public final String message;
    public int durationMs;
    public final Spring slideSpring = Spring.bouncy(0.0F);
    public final Spring ySpring = Spring.smooth(-1.0F);

    public Notification(NotificationType notificationType, String text, String text2, int value) {
        this.type = notificationType;
        this.title = text;
        this.message = text2;
        this.durationMs = value;
    }

    public NotificationType getType() {
        return this.type;
    }

    public String getTitle() {
        return this.title;
    }

    public String getMessage() {
        return this.message;
    }

    public int getDurationMs() {
        return this.durationMs;
    }

    public boolean matches(Notification notification) {
        return this.type == notification.type && Objects.equals(this.title, notification.title) && Objects.equals(this.message, notification.message);
    }

    public void refresh(int value) {
        this.durationMs = value;
        this.timer.reset();
    }

    public boolean isExpired() {
        return this.timer.hasTimeElapsed(this.durationMs, false);
    }

    public long getElapsedMs() {
        return this.timer.getElapsedTime();
    }

    public float getProgress() {
        return Math.min(1.0F, (float)this.timer.getElapsedTime() / this.durationMs);
    }
}
