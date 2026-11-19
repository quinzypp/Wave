package dev.adapt.v3.feature.module.impl.combat;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import net.minecraft.world.item.component.AttackRange;

public final class ReachModule extends Module {
    public final NumberProperty entityInteractionRange = new NumberProperty("Entity interaction range", 3.0, 3.0, 6.0, 0.05);
    public final NumberProperty blockInteractionRange = new NumberProperty("Block interaction range", 4.5, 4.5, 6.0, 0.05);

    public ReachModule() {
        super("Reach", "Allows you to interact or attack further.", ModuleCategory.COMBAT);
        this.addProperties(this.entityInteractionRange, this.blockInteractionRange);
    }

    public static ReachModule getInstance() {
        Adapt adapt = Adapt.getInstance();
        return adapt == null ? null : adapt.getModuleManager().getByClass(ReachModule.class);
    }

    public double getEntityReach() {
        return this.entityInteractionRange.getValue();
    }

    public double getBlockReach() {
        return this.blockInteractionRange.getValue();
    }

    public static Object widenAttackRange(Object key) {
        if (key instanceof AttackRange attackrange) {
            ReachModule reachmodule = getInstance();
            if (reachmodule != null && reachmodule.isEnabled()) {
                float f = (float)reachmodule.getEntityReach();
                return attackrange.maxReach() >= f && attackrange.maxCreativeReach() >= f
                    ? attackrange
                    : new AttackRange(
                        attackrange.minReach(),
                        Math.max(attackrange.maxReach(), f),
                        attackrange.minCreativeReach(),
                        Math.max(attackrange.maxCreativeReach(), f),
                        attackrange.hitboxMargin(),
                        attackrange.mobFactor()
                    );
            } else {
                return attackrange;
            }
        } else {
            return key;
        }
    }
}
