package dev.adapt.v3.event;

public class CancellableEvent {
    public boolean cancelled;

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public boolean shouldStopPropagation() {
        return true;
    }
}
