package dev.adapt.v3.feature.property;

public interface Listener<T> {
    void onChange(T first, T second);
}
