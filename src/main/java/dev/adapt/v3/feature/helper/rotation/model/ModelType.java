package dev.adapt.v3.feature.helper.rotation.model;

import dev.adapt.v3.feature.helper.rotation.RotationProperties;
import dev.adapt.v3.feature.helper.rotation.model.impl.InstantRotationModel;
import dev.adapt.v3.feature.helper.rotation.model.impl.LinearRotationModel;
import dev.adapt.v3.util.Displayable;
import java.util.function.Function;

public enum ModelType implements Displayable {
    INSTANT("Instant", rotationProperties -> InstantRotationModel.INSTANCE),
    LINEAR("Linear", rotationProperties -> new LinearRotationModel(rotationProperties.getMaxAngle()));

    public final String name;
    public final Function<RotationProperties, RotationModel> factory;

    ModelType(String text, Function<RotationProperties, RotationModel> mapper) {
        this.name = text;
        this.factory = mapper;
    }

    @Override
    public String getDisplayName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public RotationModel create(RotationProperties rotationProperties) {
        return this.factory.apply(rotationProperties);
    }
}
