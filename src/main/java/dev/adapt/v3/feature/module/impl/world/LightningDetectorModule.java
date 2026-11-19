package dev.adapt.v3.feature.module.impl.world;

import dev.adapt.v3.Adapt;
import dev.adapt.v3.event.impl.JoinWorldEvent;
import dev.adapt.v3.event.impl.PostTickEvent;
import dev.adapt.v3.event.impl.ReceivePacketEvent;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.ModuleCategory;
import dev.adapt.v3.feature.property.impl.BooleanProperty;
import dev.adapt.v3.feature.property.impl.EnumProperty;
import dev.adapt.v3.feature.property.impl.NumberProperty;
import dev.adapt.v3.notification.NotificationType;
import dev.adapt.v3.util.Displayable;
import dev.adapt.v3.util.player.ChatUtility;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class LightningDetectorModule extends Module {
    public static final double DEDUP_RADIUS = 6.0;
    public static final long DEDUP_WINDOW_MS = 2000L;
    public static final double CLEAR_DISTANCE = 20.0;
    public static final double MAX_GIZMO_DISTANCE = 48.0;
    public static final int LABEL_COLOR = -8090;
    public final EnumProperty<LightningDetectorModule.Mode> mode = new EnumProperty<>("Mode", LightningDetectorModule.Mode.BOTH);
    public final NumberProperty maxTracked = new NumberProperty("Max Tracked", 3.0, 1.0, 10.0, 1.0);
    public final BooleanProperty chat = new BooleanProperty("Chat", true);
    public final BooleanProperty notify = new BooleanProperty("Notify", true);
    public final BooleanProperty render = new BooleanProperty("Render", true);
    public final BooleanProperty tracers = (BooleanProperty)new BooleanProperty("Tracers", false).hideIf(() -> !this.render.getValue());
    public final List<LightningDetectorModule.Strike> strikes = new ArrayList<>();
    public int strikeCounter;

    public LightningDetectorModule() {
        super("Lightning Detector", "Shows where lightning strikes.", ModuleCategory.WORLD);
        this.addProperties(this.mode, this.maxTracked, this.chat, this.notify, this.render, this.tracers);
    }

    @Override
    public void onEnable() {
        this.reset();
    }

    @Override
    public void onDisable() {
        this.reset();
    }

    @Subscribe
    public void onWorldChange(JoinWorldEvent event) {
        this.reset();
    }

    public void reset() {
        this.strikes.clear();
        this.strikeCounter = 0;
    }

    @Override
    public String getSuffix() {
        return this.mode.getValue().getDisplayName();
    }

    @Subscribe
    public void onReceivePacket(ReceivePacketEvent event) {
        if (mc.player != null) {
            LightningDetectorModule.Mode lightningdetectormodule$mode = this.mode.getValue();
            if (event.getPacket() instanceof ClientboundAddEntityPacket clientboundaddentitypacket
                && lightningdetectormodule$mode != LightningDetectorModule.Mode.SOUND) {
                if (clientboundaddentitypacket.getType() == EntityTypes.LIGHTNING_BOLT) {
                    this.recordStrike(new Vec3(clientboundaddentitypacket.getX(), clientboundaddentitypacket.getY(), clientboundaddentitypacket.getZ()));
                }
            } else if (event.getPacket() instanceof ClientboundSoundPacket clientboundsoundpacket
                && lightningdetectormodule$mode != LightningDetectorModule.Mode.ENTITY) {
                if (clientboundsoundpacket.getSound().is(SoundEvents.LIGHTNING_BOLT_THUNDER.location())) {
                    this.recordStrike(new Vec3(clientboundsoundpacket.getX(), clientboundsoundpacket.getY(), clientboundsoundpacket.getZ()));
                }
            } else if (event.getPacket() instanceof ClientboundRespawnPacket) {
                this.reset();
            }
        }
    }

    public void recordStrike(Vec3 vec) {
        long i = System.currentTimeMillis();

        for (LightningDetectorModule.Strike lightningdetectormodule$strike : this.strikes) {
            if (i - lightningdetectormodule$strike.time < 2000L && lightningdetectormodule$strike.pos.distanceToSqr(vec) < 36.0) {
                return;
            }
        }

        LightningDetectorModule.Strike lightningdetectormodule$strike1 = new LightningDetectorModule.Strike(++this.strikeCounter, vec, i);
        this.strikes.add(lightningdetectormodule$strike1);

        while (this.strikes.size() > this.maxTracked.getValue().intValue()) {
            this.strikes.remove(0);
        }

        int j = (int)Math.round(Math.sqrt(this.horizontalDistSqr(vec)));
        String s = (int)Math.floor(vec.x) + ", " + (int)Math.floor(vec.y) + ", " + (int)Math.floor(vec.z);
        if (this.chat.getValue()) {
            ChatUtility.printPrefixed(
                Component.literal("Lightning #" + lightningdetectormodule$strike1.number + " ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(j + "m away at ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(s).withStyle(ChatFormatting.WHITE))
            );
        }

        if (this.notify.getValue()) {
            Adapt.getInstance()
                .getNotificationManager()
                .builder(NotificationType.WARN)
                .title("Lightning #" + lightningdetectormodule$strike1.number)
                .message(j + "m away at " + s)
                .duration(3000)
                .post();
        }
    }

    public double horizontalDistSqr(Vec3 vec) {
        double d0 = vec.x - mc.player.getX();
        double d1 = vec.z - mc.player.getZ();
        return d0 * d0 + d1 * d1;
    }

    @Subscribe
    public void onEndTick(PostTickEvent event) {
        if (mc.player != null && !this.strikes.isEmpty()) {
            this.strikes.removeIf(strike -> this.horizontalDistSqr(strike.pos) < 400.0);
            if (this.render.getValue()) {
                Vec3 vec3 = mc.player.getEyePosition();

                try {
                    for (LightningDetectorModule.Strike lightningdetectormodule$strike : this.strikes) {
                        double d0 = Math.sqrt(this.horizontalDistSqr(lightningdetectormodule$strike.pos));
                        Vec3 vec31 = lightningdetectormodule$strike.pos.subtract(vec3);
                        double d1 = vec31.length();
                        double d2 = d1 > 48.0 ? 48.0 / d1 : 1.0;
                        Vec3 vec32 = vec3.add(vec31.scale(d2));
                        double d3 = Math.max(1.0, Math.min(d1, 48.0) / 16.0);
                        Gizmos.cuboid(
                                new AABB(vec32.x - d3 / 2.0, vec32.y, vec32.z - d3 / 2.0, vec32.x + d3 / 2.0, vec32.y + d3 * 3.0, vec32.z + d3 / 2.0),
                                GizmoStyle.strokeAndFill(-520101786, 1.5F, 1090510950)
                            )
                            .setAlwaysOnTop();
                        float f = 0.32F * (float)Math.max(1.0, Math.min(d1, 48.0) / 12.0);
                        Gizmos.billboardText(
                                "#" + lightningdetectormodule$strike.number + " · " + Math.round(d0) + "m",
                                vec32.add(0.0, d3 * 3.0 + 0.6 * f / 0.32F, 0.0),
                                Style.forColorAndCentered(-8090).withScale(f)
                            )
                            .setAlwaysOnTop();
                        if (this.tracers.getValue()) {
                            Gizmos.line(vec3, vec32, -1056972698, 1.5F).setAlwaysOnTop();
                        }
                    }
                } catch (IllegalStateException illegalstateexception) {
                }
            }
        }
    }

    public enum Mode implements Displayable {
        ENTITY("Entity"),
        SOUND("Sound"),
        BOTH("Both");

        public final String displayName;

        Mode(String text) {
            this.displayName = text;
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }

    private record Strike(int number, Vec3 pos, long time) {
    }
}
