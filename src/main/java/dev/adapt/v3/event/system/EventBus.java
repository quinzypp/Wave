package dev.adapt.v3.event.system;

import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EventBus {
    public final Map<Class<?>, List<CallSiteListener>> listeners = new HashMap<>();
    public static final Lookup LOOKUP = MethodHandles.lookup();

    public void subscribe(Object key) {
        Listener listener = (Listener)key;

        for (Method method : key.getClass().getDeclaredMethods()) {
            Subscribe subscribe = method.getDeclaredAnnotation(Subscribe.class);
            if (subscribe != null && !Modifier.isStatic(method.getModifiers())) {
                Class oclass = method.getParameterTypes()[0];
                MethodType methodtype = MethodType.methodType(void.class, oclass);

                try {
                    Lookup lookup = MethodHandles.privateLookupIn(key.getClass(), LOOKUP);
                    MethodHandle methodhandle = lookup.findVirtual(key.getClass(), method.getName(), methodtype);
                    ConstantCallSite constantcallsite = new ConstantCallSite(methodhandle);
                    this.listeners
                        .computeIfAbsent(oclass, type -> new ArrayList<>())
                        .add(new CallSiteListener(subscribe.priority(), constantcallsite, listener));
                } catch (IllegalAccessException | NoSuchMethodException illegalaccessexception) {
                    throw new RuntimeException("Error subscribing event: " + method.getName(), illegalaccessexception);
                }
            }
        }

        for (List list : this.listeners.values()) {
            list.sort(Comparator.comparingInt(CallSiteListener::getNegatedPriority));
        }
    }

    public void post(Object key) {
        List list = this.listeners.get(key.getClass());
        if (list != null) {
            int i = 0;

            while (i < list.size() && !((CallSiteListener)list.get(i)).invoke(key)) {
                i++;
            }
        }
    }
}
