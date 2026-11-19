package dev.adapt.v3.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DeviceId {
    public static String cachedId;

    public static synchronized String get() {
        if (cachedId != null) {
            return cachedId;
        }

        String s = null;

        try {
            String s1 = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (s1.contains("win")) {
                s = extractGroup(
                    execCommand("reg", "query", "HKLM\\SOFTWARE\\Microsoft\\Cryptography", "/v", "MachineGuid"), "MachineGuid\\s+REG_SZ\\s+([0-9a-fA-F-]+)"
                );
            } else if (Files.exists(Path.of("/etc/machine-id"))) {
                s = Files.readString(Path.of("/etc/machine-id")).trim();
            } else if (s1.contains("mac")) {
                s = extractGroup(execCommand("ioreg", "-rd1", "-c", "IOPlatformExpertDevice"), "\"IOPlatformUUID\" = \"([^\"]+)\"");
            }
        } catch (Exception exception1) {
            s = null;
        }

        if (s == null || s.isBlank()) {
            s = readLauncherDeviceId();
        }

        if (s == null) {
            s = "unknown";
        }

        try {
            byte[] abyte = MessageDigest.getInstance("SHA-256").digest(("adapt-device-v1:" + s.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));
            cachedId = HexFormat.of().formatHex(abyte);
        } catch (Exception exception) {
            cachedId = "0".repeat(64);
        }

        return cachedId;
    }

    public static String readLauncherDeviceId() {
        String s = System.getenv("APPDATA");
        Path path = s != null ? Path.of(s, "Adapt Launcher", "device-id") : Path.of(System.getProperty("user.home"), ".config", "Adapt Launcher", "device-id");

        try {
            return Files.readString(path).trim();
        } catch (IOException ioexception) {
            return null;
        }
    }

    public static String execCommand(String... strings) throws Exception {
        Process process = new ProcessBuilder(strings).redirectErrorStream(true).start();
        String s = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        process.waitFor(5L, TimeUnit.SECONDS);
        return s;
    }

    public static String extractGroup(String text, String text2) {
        Matcher matcher = Pattern.compile(text2).matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }
}
