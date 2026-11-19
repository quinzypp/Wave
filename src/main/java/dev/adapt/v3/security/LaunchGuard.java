package dev.adapt.v3.security;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.adapt.v3.Adapt;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.notification.NotificationType;
import dev.adapt.v3.ui.screen.DashboardScreen;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LaunchGuard {
    public static final Logger LOGGER = LoggerFactory.getLogger("AdaptV3");
    public static final long HEARTBEAT_INTERVAL_SECONDS = 240L;
    public static final long EXPIRY_GRACE_MS = 600000L;
    public static final Set<String> TERMINAL_ERROR_CODES = Set.of("invalid_ticket", "ticket_expired", "revoked", "banned", "no_license", "bad_request");
    public static final String DEFAULT_API_BASE = "https://adaptclient.com";
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10L))
        .version(Version.HTTP_1_1)
        .followRedirects(Redirect.NEVER)
        .build();
    public static volatile boolean authorised;
    public static volatile String statusReason = "Open Adapt from the Adapt Launcher.";
    public static volatile String ticket;
    public static volatile long expiresAt;
    public static volatile String accountName;
    public static ScheduledExecutorService heartbeatExecutor;
    public static volatile boolean devRun;
    public static String devTokenError;
    public static PublicKey publicKey;

    public static boolean isAuthorised() {
        return authorised;
    }

    public static String getStatusReason() {
        return statusReason;
    }

    public static String getTicket() {
        return authorised ? ticket : null;
    }

    public static boolean isDevRun() {
        return devRun;
    }

    public static String getAccountName() {
        return authorised ? accountName : null;
    }

    public static void init() {
        String s = System.getenv("ADAPT_TICKET");
        if ((s == null || s.isBlank()) && isUnpackagedDevEnvironment()) {
            authorised = true;
            devRun = true;
            statusReason = "Development run";
            LOGGER.warn("Development run from the IDE: launch check skipped (release jars always need the Adapt Launcher)");
            resolveDevNameAsync();
        } else {
            String s1 = s;
            if (s1 == null || s1.isBlank()) {
                s1 = requestDevTicket();
                if (s1 == null) {
                    markUnauthorised(devTokenError != null ? devTokenError : "Open Adapt from the Adapt Launcher.");
                    return;
                }
            }

            authoriseWithTicket(s1.trim());
        }
    }

    public static void authoriseWithTicket(String text) {
        JsonObject jsonobject = verifyTicket(text);
        if (jsonobject == null) {
            markUnauthorised("This launch wasn't authorised. Open Adapt from the Adapt Launcher.");
        } else {
            long i = jsonobject.get("exp").getAsLong();
            if (System.currentTimeMillis() > i + 600000L) {
                markUnauthorised("This launch has expired. Open Adapt from the Adapt Launcher (and check your PC's clock is right).");
            } else {
                ticket = text;
                expiresAt = i;
                accountName = jsonobject.has("name") ? jsonobject.get("name").getAsString() : null;
                authorised = true;
                LOGGER.info("Launch authorised{}", accountName != null ? " for " + accountName : "");
                heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(action -> {
                    Thread thread = new Thread(action, "Adapt session");
                    thread.setDaemon(true);
                    return thread;
                });
                heartbeatExecutor.schedule(LaunchGuard::sendHeartbeat, 2L, TimeUnit.SECONDS);
                heartbeatExecutor.scheduleAtFixedRate(LaunchGuard::sendHeartbeat, 240L, 240L, TimeUnit.SECONDS);
            }
        }
    }

    public static String requestDevTicket() {
        String s = readDevToken();
        if (s == null) {
            return null;
        }

        try {
            JsonObject jsonobject = new JsonObject();
            jsonobject.addProperty("device", DeviceId.get());
            HttpResponse httpresponse = HTTP_CLIENT.send(
                HttpRequest.newBuilder(URI.create(getApiBase() + "/api/client/dev-ticket"))
                    .timeout(Duration.ofSeconds(15L))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "AdaptDev " + s)
                    .header("User-Agent", "adaptv3/3.0.4")
                    .POST(BodyPublishers.ofString(jsonobject.toString()))
                    .build(),
                BodyHandlers.ofString()
            );
            JsonObject jsonobject1 = parseJsonObject((String)httpresponse.body());
            if (httpresponse.statusCode() == 200 && jsonobject1 != null && jsonobject1.has("ticket")) {
                LOGGER.warn("Got a ticket with the staff dev token");
                return jsonobject1.get("ticket").getAsString();
            }

            String s1 = jsonobject1 != null && jsonobject1.has("error") ? jsonobject1.get("error").getAsString() : "HTTP " + httpresponse.statusCode();
            LOGGER.warn("Dev token launch refused: {}", s1);
            devTokenError = "Dev token refused: " + s1;
        } catch (Exception exception) {
            LOGGER.warn("Dev token launch failed: {}", exception.getMessage());
        }

        return null;
    }

    public static void resolveDevNameAsync() {
        if (readDevToken() != null) {
            Thread thread = new Thread(() -> {
                String s = requestDevTicket();
                JsonObject jsonobject = s == null ? null : decodeTicketPayload(s);
                if (jsonobject != null && jsonobject.has("name") && !jsonobject.get("name").isJsonNull()) {
                    accountName = jsonobject.get("name").getAsString();
                }
            }, "Adapt dev name");
            thread.setDaemon(true);
            thread.start();
        }
    }

    public static JsonObject decodeTicketPayload(String code) {
        try {
            String[] astring = code.split("\\.");
            return astring.length == 2 ? parseJsonObject(new String(Base64.getUrlDecoder().decode(astring[0]), StandardCharsets.UTF_8)) : null;
        } catch (Exception exception) {
            return null;
        }
    }

    public static String readDevToken() {
        String s = System.getenv("ADAPT_DEV_TOKEN");
        if (s != null && !s.isBlank()) {
            return s.trim();
        }

        try {
            Path path = FabricLoader.getInstance().getGameDir().resolve("adapt-dev-token.txt");
            if (!Files.isRegularFile(path)) {
                return null;
            }

            String s1 = Files.readString(path).trim();
            return s1.isEmpty() ? null : s1;
        } catch (Throwable throwable) {
            return null;
        }
    }

    public static boolean isUnpackagedDevEnvironment() {
        try {
            if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
                return false;
            } else {
                CodeSource codesource = LaunchGuard.class.getProtectionDomain().getCodeSource();
                if (codesource != null && codesource.getLocation() != null) {
                    Path path = Path.of(codesource.getLocation().toURI());
                    return Files.isDirectory(path);
                } else {
                    return false;
                }
            }
        } catch (Throwable throwable) {
            return false;
        }
    }

    public static void sendHeartbeat() {
        if (authorised) {
            try {
                JsonObject jsonobject = new JsonObject();
                jsonobject.addProperty("ticket", ticket);
                jsonobject.addProperty("device", DeviceId.get());
                HttpResponse httpresponse = HTTP_CLIENT.send(
                    HttpRequest.newBuilder(URI.create(getApiBase() + "/api/client/heartbeat"))
                        .timeout(Duration.ofSeconds(15L))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "adaptv3/3.0.4")
                        .POST(BodyPublishers.ofString(jsonobject.toString()))
                        .build(),
                    BodyHandlers.ofString()
                );
                JsonObject jsonobject1 = parseJsonObject((String)httpresponse.body());
                if (httpresponse.statusCode() == 200 && jsonobject1 != null && jsonobject1.has("ticket")) {
                    String s2 = jsonobject1.get("ticket").getAsString();
                    JsonObject jsonobject2 = verifyTicket(s2);
                    if (jsonobject2 != null) {
                        ticket = s2;
                        expiresAt = jsonobject2.get("exp").getAsLong();
                        return;
                    }

                    LOGGER.warn("Session check returned an unsigned ticket");
                } else {
                    String s = jsonobject1 != null && jsonobject1.has("code") ? jsonobject1.get("code").getAsString() : "";
                    if (TERMINAL_ERROR_CODES.contains(s)) {
                        String s1 = jsonobject1.has("error") ? jsonobject1.get("error").getAsString() : "Your session ended.";

                        revokeSession(switch (s) {
                            case "banned" -> "This account is banned.";
                            case "no_license" -> "Your license has expired. Renew it on adaptclient.com.";
                            case "revoked" -> "You were signed out. Sign in again in the Adapt Launcher.";
                            default -> s1;
                        });
                        return;
                    }
                }
            } catch (Exception exception) {
                LOGGER.warn("Session check failed: {}", exception.getMessage());
            }

            if (System.currentTimeMillis() > expiresAt + 600000L) {
                revokeSession("Lost contact with adaptclient.com. Restart Adapt from the launcher when you're back online.");
            }
        }
    }

    public static void markUnauthorised(String text) {
        authorised = false;
        statusReason = text;
        LOGGER.warn("Adapt is off: {}", text);
    }

    public static void revokeSession(String text) {
        if (authorised) {
            markUnauthorised(text);
            if (heartbeatExecutor != null) {
                heartbeatExecutor.shutdown();
            }

            Minecraft.getInstance().execute(() -> {
                Adapt adapt = Adapt.getInstance();
                if (adapt != null) {
                    for (Module module : adapt.getModuleManager().getAll()) {
                        if (module.canToggle() && module.isEnabled()) {
                            module.setEnabled(false);
                        }
                    }

                    if (Minecraft.getInstance().gui.screen() instanceof DashboardScreen) {
                        Minecraft.getInstance().gui.setScreen(null);
                    }

                    notifySessionEnded();
                }
            });
        }
    }

    public static void notifySessionEnded() {
        Adapt adapt = Adapt.getInstance();
        if (adapt != null) {
            adapt.getNotificationManager().builder(NotificationType.ERROR).title("Adapt is off").message(statusReason).duration(6000).post();
        }
    }

    public static JsonObject verifyTicket(String text) {
        try {
            String[] astring = text.split("\\.");
            if (astring.length != 2) {
                return null;
            }

            byte[] abyte = ("adapt-ticket-v1\n" + astring[0]).getBytes(StandardCharsets.UTF_8);
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(getPublicKey());
            signature.update(abyte);
            if (!signature.verify(Base64.getUrlDecoder().decode(astring[1]))) {
                return null;
            }

            JsonObject jsonobject = parseJsonObject(new String(Base64.getUrlDecoder().decode(astring[0]), StandardCharsets.UTF_8));
            return jsonobject != null
                    && jsonobject.has("v")
                    && jsonobject.get("v").getAsInt() == 1
                    && jsonobject.has("exp")
                    && jsonobject.has("sid")
                    && jsonobject.has("sub")
                ? jsonobject
                : null;
        } catch (Exception exception) {
            return null;
        }
    }

    public static PublicKey getPublicKey() throws Exception {
        if (publicKey != null) {
            return publicKey;
        }

        Properties properties = new Properties();

        try (InputStream inputstream = LaunchGuard.class.getResourceAsStream("/adapt-security.properties")) {
            if (inputstream == null) {
                throw new IllegalStateException("missing key");
            }

            properties.load(inputstream);
        }

        byte[] abyte1 = Base64.getDecoder().decode(properties.getProperty("publicKey", "").trim());
        if (abyte1.length != 32) {
            throw new IllegalStateException("bad key");
        }

        byte[] abyte = new byte[44];
        System.arraycopy(HexFormat.of().parseHex("302a300506032b6570032100"), 0, abyte, 0, 12);
        System.arraycopy(abyte1, 0, abyte, 12, 32);
        publicKey = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(abyte));
        return publicKey;
    }

    public static JsonObject parseJsonObject(String text) {
        try {
            return JsonParser.parseString(text).getAsJsonObject();
        } catch (Exception exception) {
            return null;
        }
    }

    public static String getApiBase() {
        String s = System.getenv("ADAPT_API_BASE");
        return s == null || !s.startsWith("https://") && !s.startsWith("http://localhost") && !s.startsWith("http://127.0.0.1")
            ? "https://adaptclient.com"
            : s.replaceAll("/+$", "");
    }
}
