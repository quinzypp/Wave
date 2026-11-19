package dev.adapt.v3.util;

import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.PostTickEvent;
import dev.adapt.v3.event.system.Listener;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.util.player.MoveUtility;
import net.minecraft.client.Minecraft;

public final class SpeedTracker implements Listener {
    public final float[] history = new float[80];
    public int writeIndex;
    public float currentBps;

    public SpeedTracker() {
        EventManager.register(this);
    }

    @Subscribe
    public void onPostTick(PostTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            this.currentBps = (float)MoveUtility.getBlocksPerSecond();
            this.history[this.writeIndex] = this.currentBps;
            this.writeIndex = (this.writeIndex + 1) % this.history.length;
        }
    }

    public float getCurrentBps() {
        return this.currentBps;
    }

    public float[] getOrderedHistory() {
        float[] afloat = new float[this.history.length];

        for (int i = 0; i < afloat.length; i++) {
            afloat[i] = this.history[(this.writeIndex + i) % this.history.length];
        }

        return afloat;
    }
}
