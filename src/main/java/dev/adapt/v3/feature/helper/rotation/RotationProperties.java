package dev.adapt.v3.feature.helper.rotation;

import dev.adapt.v3.feature.helper.rotation.model.ModelType;
import dev.adapt.v3.feature.helper.rotation.model.RotationModel;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.property.Property;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import java.util.function.BooleanSupplier;

public final class RotationProperties {
    public final EnumProperty model;
    public final NumberProperty maxAngle;
    public BooleanSupplier hideCondition = () -> false;

    public RotationProperties(Module module, RotationModel rotationModel, Property<?>... properties) {
        this.model = (EnumProperty)new EnumProperty<>("Rotation model", rotationModel.getType()).hideIf(() -> this.hideCondition.getAsBoolean());
        this.maxAngle = (NumberProperty)new NumberProperty("Max angle", 90.0, 5.0, 360.0, 5.0)
            .hideIf(() -> this.hideCondition.getAsBoolean() || this.model.getValue() == ModelType.INSTANT);
        module.addProperties(this.model, this.maxAngle);
        module.addProperties(properties);
    }

    public RotationProperties hideIf(BooleanSupplier condition) {
        this.hideCondition = condition;
        return this;
    }

    public int getMaxAngle() {
        return this.maxAngle.getValue().intValue();
    }

    public ModelType getModelType() {
        return (ModelType)this.model.getValue();
    }

    public RotationModel createModel() {
        return ((ModelType)this.model.getValue()).create(this);
    }
}
