package dev.adapt.v3.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.PreTickEvent;
import dev.adapt.v3.event.system.Listener;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.mixin.MinecraftAccessor;
import dev.adapt.v3.security.LaunchGuard;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AdaptUsers implements Listener {
    public static final Logger LOGGER = LoggerFactory.getLogger("AdaptV3/Users");
    public static final AdaptUsers INSTANCE = new AdaptUsers();
    public static final long LOOKUP_INTERVAL_MS = 10000L;
    public static final int MAX_PLAYER_UUIDS = 150;
    public final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8L))
        .version(Version.HTTP_1_1)
        .followRedirects(Redirect.NEVER)
        .build();
    public volatile Map onlinePlayers = Map.of();
    public final AtomicBoolean lookupInFlight = new AtomicBoolean();
    public long nextLookupAt;
    public static final long VERIFY_COOLDOWN_MS = 300000L;
    public final AtomicBoolean verifyInFlight = new AtomicBoolean();
    public volatile long nextVerifyAt;

    public AdaptUsers() {
        EventManager.register(this);
    }

    public static void init() {
    }

    public static String getAdaptUsername(UUID uuid) {
        return uuid == null ? null : (String)INSTANCE.onlinePlayers.get(uuid);
    }

    public static CompletableFuture<List<AdaptUsers.OnlineUser>> fetchOnlineUsers() {
        Builder builder = HttpRequest.newBuilder(URI.create(LaunchGuard.getApiBase() + "/api/client/online"))
            .timeout(Duration.ofSeconds(10L))
            .header("User-Agent", "adaptv3/3.0.4")
            .GET();

        try {
            ChatService.applyAuthHeaders(builder);
        } catch (IllegalStateException illegalstateexception) {
            return CompletableFuture.failedFuture(illegalstateexception);
        }

        return INSTANCE.httpClient.sendAsync(builder.build(), BodyHandlers.ofString()).thenApply(httpResponse -> {
            JsonObject jsonobject;
            try {
                jsonobject = JsonParser.parseString(httpResponse.body()).getAsJsonObject();
            } catch (Exception exception) {
                throw new IllegalStateException("adaptclient.com sent a bad answer.");
            }

            if (httpResponse.statusCode() != 200) {
                throw new IllegalStateException(jsonobject.has("error") ? jsonobject.get("error").getAsString() : "Couldn't get who's online.");
            }

            ArrayList arraylist = new ArrayList();
            JsonArray jsonarray = jsonobject.getAsJsonArray("users");
            if (jsonarray != null) {
                for (JsonElement jsonelement : jsonarray) {
                    JsonObject jsonobject1 = jsonelement.getAsJsonObject();
                    String s = jsonobject1.has("mc") && !jsonobject1.get("mc").isJsonNull() ? jsonobject1.get("mc").getAsString() : null;
                    arraylist.add(new AdaptUsers.OnlineUser(jsonobject1.get("name").getAsString(), s));
                }
            }

            return arraylist;
        });
    }

    @Override
    public boolean isListening() {
        return true;
    }

    @Subscribe
    public void onPreTick(PreTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.level != null && minecraft.getConnection() != null) {
            long i = System.currentTimeMillis();
            if (i >= this.nextLookupAt && ChatService.isAuthenticated() && !this.lookupInFlight.get()) {
                this.nextLookupAt = i + 10000L;
                JsonObject jsonobject = new JsonObject();
                jsonobject.addProperty("uuid", minecraft.player.getUUID().toString());
                JsonArray jsonarray = new JsonArray();

                for (PlayerInfo playerinfo : minecraft.getConnection().getOnlinePlayers()) {
                    if (jsonarray.size() >= 150) {
                        break;
                    }

                    jsonarray.add(playerinfo.getProfile().id().toString());
                }

                jsonobject.add("uuids", jsonarray);
                Builder builder = HttpRequest.newBuilder(URI.create(LaunchGuard.getApiBase() + "/api/client/players"))
                    .timeout(Duration.ofSeconds(10L))
                    .header("User-Agent", "adaptv3/3.0.4")
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(jsonobject.toString()));

                try {
                    ChatService.applyAuthHeaders(builder);
                } catch (IllegalStateException illegalstateexception) {
                    return;
                }

                this.lookupInFlight.set(true);
                this.httpClient.sendAsync(builder.build(), BodyHandlers.ofString()).whenComplete((httpResponse, error) -> {
                    this.lookupInFlight.set(false);
                    if (error == null && httpResponse.statusCode() == 200) {
                        try {
                            JsonObject jsonobject1 = JsonParser.parseString(httpResponse.body()).getAsJsonObject();
                            if (jsonobject1.has("verify")) {
                                this.verifySession(jsonobject1.get("verify").getAsString());
                            }

                            JsonObject jsonobject2 = jsonobject1.getAsJsonObject("players");
                            ConcurrentHashMap concurrenthashmap = new ConcurrentHashMap();
                            if (jsonobject2 != null) {
                                for (Entry entry : jsonobject2.entrySet()) {
                                    concurrenthashmap.put(UUID.fromString((String)entry.getKey()), ((JsonElement)entry.getValue()).getAsString());
                                }
                            }

                            this.onlinePlayers = Map.copyOf(concurrenthashmap);
                        } catch (Exception exception) {
                            LOGGER.debug("Bad Adapt user lookup answer", exception);
                        }
                    } else {
                        if (error != null) {
                            LOGGER.debug("Adapt user lookup failed", error);
                        }
                    }
                });
            }
        } else {
            this.onlinePlayers = Map.of();
        }
    }

    public void verifySession(String text) {
        long i = System.currentTimeMillis();
        if (i >= this.nextVerifyAt && text.matches("[0-9a-f]{1,40}") && this.verifyInFlight.compareAndSet(false, true)) {
            this.nextVerifyAt = i + 300000L;
            Minecraft minecraft = Minecraft.getInstance();
            User user = minecraft.getUser();
            CompletableFuture.runAsync(
                () -> {
                    try {
                        ((MinecraftAccessor)minecraft).adapt$services().sessionService().joinServer(user.getProfileId(), user.getAccessToken(), text);
                        JsonObject jsonobject = new JsonObject();
                        jsonobject.addProperty("uuid", user.getProfileId().toString());
                        jsonobject.addProperty("name", user.getName());
                        Builder builder = HttpRequest.newBuilder(URI.create(LaunchGuard.getApiBase() + "/api/client/players/verify"))
                            .timeout(Duration.ofSeconds(15L))
                            .header("User-Agent", "adaptv3/3.0.4")
                            .header("Content-Type", "application/json")
                            .POST(BodyPublishers.ofString(jsonobject.toString()));
                        ChatService.applyAuthHeaders(builder);
                        HttpResponse httpresponse = this.httpClient.send(builder.build(), BodyHandlers.ofString());
                        if (httpresponse.statusCode() == 200) {
                            this.nextVerifyAt = 0L;
                            this.nextLookupAt = 0L;
                        } else {
                            LOGGER.debug("Adapt account check refused: {}", httpresponse.body());
                        }
                    } catch (Exception exception) {
                        LOGGER.debug("Adapt account check failed: {}", exception.getMessage());
                    } finally {
                        this.verifyInFlight.set(false);
                    }
                },
                Util.nonCriticalIoPool()
            );
        }
    }

    public record OnlineUser(String name, String minecraft) {
    }
}
