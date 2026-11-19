package dev.adapt.v3.feature.helper.swing;

import dev.adapt.v3.util.TimerUtility;
import net.minecraft.client.Minecraft;

public final class SwingHelper {
    public static final TimerUtility TIMER = new TimerUtility();

    public static void resetTimer() {
        TIMER.reset();
    }

    public static boolean canAttack(CpsProperties cpsProperties, boolean flag) {
        Minecraft minecraft = Minecraft.getInstance();
        if (cpsProperties.isModern() && minecraft.player != null) {
            return isAttackCooldownReady();
        }

        if (TIMER.hasTimeElapsed(cpsProperties.getNextDelayMs())) {
            if (flag) {
                cpsProperties.rollNextDelay();
                resetTimer();
            }

            return true;
        } else {
            return false;
        }
    }

    public static boolean isAttackCooldownReady() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.player.getAttackStrengthScale(0.0F) >= 1.0F;
    }

    public static boolean tryAttack(CpsProperties cpsProperties) {
        return canAttack(cpsProperties, true);
    }
}
