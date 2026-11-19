package dev.adapt.v3.util;

import dev.adapt.v3.util.player.ChatUtility;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class Profiler {
    public static boolean enabled;
    public static final Map<String, long[]> sectionNanos = new LinkedHashMap<>();
    public static long windowStartNs;
    public static long frameCount;
    public static long totalFrameNs;
    public static long lastFrameNs;

    public static boolean toggle() {
        enabled = !enabled;
        sectionNanos.clear();
        frameCount = 0L;
        totalFrameNs = 0L;
        lastFrameNs = 0L;
        windowStartNs = System.nanoTime();
        return enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static long beginSection() {
        return enabled ? System.nanoTime() : 0L;
    }

    public static void endSection(String text, long time) {
        if (enabled && time != 0L) {
            sectionNanos.computeIfAbsent(text, text2 -> new long[1])[0] += System.nanoTime() - time;
        }
    }

    public static void onFrame() {
        if (enabled) {
            long i = System.nanoTime();
            if (lastFrameNs != 0L) {
                totalFrameNs = totalFrameNs + (i - lastFrameNs);
            }

            lastFrameNs = i;
            frameCount++;
            if (i - windowStartNs >= 2000000000L && frameCount >= 2L) {
                double d0 = frameCount;
                MutableComponent mutablecomponent = Component.literal(
                        String.format(Locale.ROOT, "frame %.3fms (%.0f fps)", totalFrameNs / d0 / 1000000.0, d0 / ((i - windowStartNs) / 1.0E9))
                    )
                    .withStyle(ChatFormatting.WHITE);

                for (Entry entry : sectionNanos.entrySet()) {
                    mutablecomponent.append(Component.literal("  " + (String)entry.getKey() + " ").withStyle(ChatFormatting.GRAY))
                        .append(
                            Component.literal(String.format(Locale.ROOT, "%.3f", ((long[])entry.getValue())[0] / d0 / 1000000.0))
                                .withStyle(ChatFormatting.AQUA)
                        );
                    ((long[])entry.getValue())[0] = 0L;
                }

                if (Minecraft.getInstance().player != null) {
                    ChatUtility.printPrefixed(mutablecomponent);
                }

                frameCount = 0L;
                totalFrameNs = 0L;
                windowStartNs = i;
            }
        }
    }
}
