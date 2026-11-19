package dev.adapt.v3.event.system;

import dev.adapt.v3.event.CancellableEvent;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;

public final class CallSiteListener {
    public final int negatedPriority;
    public final CallSite callSite;
    public final Listener listener;
    public final MethodHandle invoker;

    public CallSiteListener(int value, CallSite callSite, Listener listener) {
        this.negatedPriority = -value;
        this.callSite = callSite;
        this.listener = listener;
        this.invoker = callSite.dynamicInvoker();
    }

    public CallSite getCallSite() {
        return this.callSite;
    }

    public int getNegatedPriority() {
        return this.negatedPriority;
    }

    public boolean invoke(Object key) {
        if (this.listener.isListening()) {
            try {
                this.invoker.invoke((Object)this.listener, (Object)key);
            } catch (Throwable throwable) {
                throw new RuntimeException("Error invoking event", throwable);
            }

            return key instanceof CancellableEvent cancellableevent && cancellableevent.isCancelled() && cancellableevent.shouldStopPropagation();
        } else {
            return false;
        }
    }
}
