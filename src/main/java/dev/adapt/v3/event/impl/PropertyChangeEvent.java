package dev.adapt.v3.event.impl;

import dev.adapt.v3.feature.property.Property;

public record PropertyChangeEvent(Property<?> property) {
}
