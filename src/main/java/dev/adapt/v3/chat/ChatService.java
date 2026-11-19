package dev.adapt.v3.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.adapt.v3.security.DeviceId;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChatService {
    public static final Logger LOGGER = LoggerFactory.getLogger("AdaptV3");
    public static final ChatService INSTANCE = new ChatService();
    public static final long POLL_INTERVAL_MS = 2500L;
    public static final int MAX_MESSAGES = 150;
    public static final int MAX_MESSAGE_LENGTH = 500;
    public final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8L))
        .version(Version.HTTP_1_1)
        .followRedirects(Redirect.NEVER)
        .build();
    public final Map<ChatService.Channel, ChatService.State> channelStates = new EnumMap<>(ChatService.Channel.class);
    public final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(action -> {
        Thread thread = new Thread(action, "Adapt chat");
        thread.setDaemon(true);
        return thread;
    });
    public ScheduledFuture pollFuture;
    public volatile ChatService.Channel activeChannel = ChatService.Channel.IRC;
    public volatile int onlineCount = -1;
    public volatile long selfUserId = -1L;
    public volatile String statusError;
    public int pollTick;
    public volatile boolean staff;
    public static final String DEV_TOKEN_HINT = "Dev run: save your dev token as adapt-dev-token.txt in the run folder (Staff panel on the website).";

    public static ChatService getInstance() {
        return INSTANCE;
    }

    public ChatService() {
        for (ChatService.Channel chatservice$channel : ChatService.Channel.values()) {
            this.channelStates.put(chatservice$channel, new ChatService.State());
        }
    }

    public List<ChatService.Message> getMessages(ChatService.Channel channel) {
        return this.channelStates.get(channel).messages;
    }

    public boolean isLoaded(ChatService.Channel channel) {
        return this.channelStates.get(channel).loaded;
    }

    public int getUnreadCount(ChatService.Channel channel) {
        return this.channelStates.get(channel).unreadCount;
    }

    public ChatService.Channel getActiveChannel() {
        return this.activeChannel;
    }

    public int getOnlineCount() {
        return this.onlineCount;
    }

    public long getSelfUserId() {
        return this.selfUserId;
    }

    public boolean isStaff() {
        return this.staff;
    }

    public String getErrorMessage() {
        if (LaunchGuard.getTicket() == null && !LaunchGuard.isDevRun()) {
            return "Chat needs Adapt opened from the launcher.";
        } else {
            return LaunchGuard.getTicket() == null && getDevToken() == null
                ? "Dev run: save your dev token as adapt-dev-token.txt in the run folder (Staff panel on the website)."
                : this.statusError;
        }
    }

    public static String getDevToken() {
        return LaunchGuard.isDevRun() ? LaunchGuard.readDevToken() : null;
    }

    public synchronized void setPolling(boolean polling) {
        if (polling && this.pollFuture == null) {
            this.pollFuture = this.executor.scheduleWithFixedDelay(this::poll, 0L, 2500L, TimeUnit.MILLISECONDS);
        } else if (!polling && this.pollFuture != null) {
            this.pollFuture.cancel(false);
            this.pollFuture = null;
        }
    }

    public void setActiveChannel(ChatService.Channel activeChannel) {
        if (this.activeChannel != activeChannel) {
            this.activeChannel = activeChannel;
            this.channelStates.get(activeChannel).unreadCount = 0;
            this.executor.execute(() -> this.pollChannel(activeChannel));
        }
    }

    public CompletableFuture<String> sendMessage(ChatService.Channel channel, String message, Long time) {
        String s = message.strip();
        if (s.isEmpty()) {
            return CompletableFuture.completedFuture("Type a message first.");
        } else {
            return s.length() > 500 ? CompletableFuture.completedFuture("Messages can be up to 500 characters.") : CompletableFuture.supplyAsync(() -> {
                try {
                    JsonObject jsonobject = new JsonObject();
                    jsonobject.addProperty("channel", channel.id);
                    jsonobject.addProperty("body", s);
                    if (time != null) {
                        jsonobject.addProperty("replyTo", time);
                    }

                    HttpResponse httpresponse = this.sendRequest("POST", "/api/client/chat", jsonobject.toString());
                    JsonObject jsonobject1 = parseJson((String)httpresponse.body());
                    if (httpresponse.statusCode() == 200 && jsonobject1 != null && jsonobject1.has("message")) {
                        this.appendMessages(channel, List.of(parseMessage(jsonobject1.getAsJsonObject("message"))));
                        return null;
                    } else {
                        return extractError(jsonobject1, "Couldn't send that.");
                    }
                } catch (IllegalStateException illegalstateexception) {
                    return illegalstateexception.getMessage();
                } catch (Exception exception) {
                    return "Couldn't reach adaptclient.com.";
                }
            }, this.executor);
        }
    }

    public CompletableFuture<String> editMessage(ChatService.Channel channel, long id, String message) {
        String s = message.strip();
        if (s.isEmpty()) {
            return CompletableFuture.completedFuture("A message can't be empty. Delete it instead.");
        } else {
            return s.length() > 500 ? CompletableFuture.completedFuture("Messages can be up to 500 characters.") : CompletableFuture.supplyAsync(() -> {
                try {
                    JsonObject jsonobject = new JsonObject();
                    jsonobject.addProperty("id", id);
                    jsonobject.addProperty("body", s);
                    HttpResponse httpresponse = this.sendRequest("POST", "/api/client/chat/edit", jsonobject.toString());
                    JsonObject jsonobject1 = parseJson((String)httpresponse.body());
                    if (httpresponse.statusCode() == 200 && jsonobject1 != null && jsonobject1.has("message")) {
                        this.applyEdits(channel, List.of(parseMessage(jsonobject1.getAsJsonObject("message"))));
                        return null;
                    } else {
                        return extractError(jsonobject1, "Couldn't save that.");
                    }
                } catch (IllegalStateException illegalstateexception) {
                    return illegalstateexception.getMessage();
                } catch (Exception exception) {
                    return "Couldn't reach adaptclient.com.";
                }
            }, this.executor);
        }
    }

    public CompletableFuture<String> deleteMessage(ChatService.Channel channel, long id) {
        synchronized (this) {
            ChatService.State chatservice$state = this.channelStates.get(channel);
            chatservice$state.messages = chatservice$state.messages.stream().filter(message -> message.id() != id).toList();
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject jsonobject = new JsonObject();
                jsonobject.addProperty("id", id);
                HttpResponse httpresponse = this.sendRequest("POST", "/api/client/chat/delete", jsonobject.toString());
                JsonObject jsonobject1 = parseJson((String)httpresponse.body());
                if (httpresponse.statusCode() == 200) {
                    return null;
                }

                this.channelStates.get(channel).pollCount = 7;
                return extractError(jsonobject1, "Couldn't delete that.");
            } catch (IllegalStateException illegalstateexception) {
                return illegalstateexception.getMessage();
            } catch (Exception exception) {
                return "Couldn't reach adaptclient.com.";
            }
        }, this.executor);
    }

    public void poll() {
        try {
            ChatService.Channel chatservice$channel = this.activeChannel;
            this.pollChannel(chatservice$channel);
            if (++this.pollTick % 4 == 0) {
                for (ChatService.Channel chatservice$channel1 : ChatService.Channel.values()) {
                    if (chatservice$channel1 != chatservice$channel) {
                        this.pollChannel(chatservice$channel1);
                    }
                }
            }
        } catch (Throwable throwable) {
            LOGGER.debug("Chat poll failed", throwable);
        }
    }

    public void pollChannel(ChatService.Channel channel) {
        if (LaunchGuard.getTicket() != null || getDevToken() != null) {
            ChatService.State chatservice$state = this.channelStates.get(channel);
            boolean flag = !chatservice$state.loaded || ++chatservice$state.pollCount % 8 == 0;
            String s = "/api/client/chat?channel="
                + channel.id
                + (flag ? "" : "&after=" + chatservice$state.lastMessageId + "&since=" + chatservice$state.sinceTimestamp);

            try {
                HttpResponse httpresponse = this.sendRequest("GET", s, null);
                JsonObject jsonobject = parseJson((String)httpresponse.body());
                if (httpresponse.statusCode() != 200 || jsonobject == null) {
                    this.statusError = extractError(jsonobject, "Chat is unavailable right now.");
                    return;
                }

                this.statusError = null;
                if (jsonobject.has("online")) {
                    this.onlineCount = jsonobject.get("online").getAsInt();
                }

                if (jsonobject.has("me")) {
                    this.selfUserId = jsonobject.get("me").getAsLong();
                }

                if (jsonobject.has("staff")) {
                    this.staff = jsonobject.get("staff").getAsBoolean();
                }

                if (jsonobject.has("now")) {
                    chatservice$state.sinceTimestamp = jsonobject.get("now").getAsLong();
                }

                List<ChatService.Message> list = parseMessages(jsonobject.getAsJsonArray("messages"));
                if (flag) {
                    long i = chatservice$state.lastMessageId;
                    chatservice$state.messages = List.copyOf(trimMessages(list));
                    chatservice$state.lastMessageId = list.isEmpty() ? 0L : ((ChatService.Message)list.get(list.size() - 1)).id();
                    if (chatservice$state.loaded && channel != this.activeChannel) {
                        chatservice$state.unreadCount = chatservice$state.unreadCount
                            + (int)list.stream().filter(message -> message.id() > i && message.userId() != this.selfUserId).count();
                    }

                    chatservice$state.loaded = true;
                } else {
                    if (channel != this.activeChannel) {
                        chatservice$state.unreadCount = chatservice$state.unreadCount
                            + (int)list.stream().filter(message2 -> message2.userId() != this.selfUserId).count();
                    }

                    this.appendMessages(channel, list);
                    this.applyEdits(channel, parseMessages(jsonobject.getAsJsonArray("edited")));
                }
            } catch (IllegalStateException illegalstateexception) {
                this.statusError = illegalstateexception.getMessage();
            } catch (Exception exception) {
                this.statusError = "Couldn't reach adaptclient.com.";
            }
        }
    }

    public synchronized void appendMessages(ChatService.Channel channel, List<ChatService.Message> messages) {
        if (!messages.isEmpty()) {
            ChatService.State chatservice$state = this.channelStates.get(channel);
            List<ChatService.Message> arraylist = new ArrayList<>(chatservice$state.messages);

            for (ChatService.Message chatservice$message : messages) {
                if (arraylist.stream().noneMatch(message -> message.id() == chatservice$message.id())) {
                    arraylist.add(chatservice$message);
                }
            }

            arraylist.sort((a, b) -> Long.compare(a.id(), b.id()));
            chatservice$state.messages = List.copyOf(trimMessages(arraylist));
            chatservice$state.lastMessageId = Math.max(chatservice$state.lastMessageId, ((ChatService.Message)arraylist.get(arraylist.size() - 1)).id());
        }
    }

    public synchronized void applyEdits(ChatService.Channel channel, List<ChatService.Message> messages) {
        if (!messages.isEmpty()) {
            ChatService.State chatservice$state = this.channelStates.get(channel);
            Map<Long, ChatService.Message> hashmap = new HashMap<>();

            for (ChatService.Message chatservice$message : messages) {
                hashmap.put(chatservice$message.id(), chatservice$message);
            }

            chatservice$state.messages = chatservice$state.messages
                .stream()
                .map(message -> (ChatService.Message)hashmap.getOrDefault(message.id(), message))
                .toList();
        }
    }

    public static List<ChatService.Message> trimMessages(List<ChatService.Message> messages) {
        return messages.size() <= 150 ? messages : messages.subList(messages.size() - 150, messages.size());
    }

    public HttpResponse sendRequest(String message, String message2, String message3) throws Exception {
        Builder builder = HttpRequest.newBuilder(URI.create(LaunchGuard.getApiBase() + message2))
            .timeout(Duration.ofSeconds(10L))
            .header("User-Agent", "adaptv3/3.0.4");
        applyAuthHeaders(builder);
        if (message3 != null) {
            builder.header("Content-Type", "application/json").method(message, BodyPublishers.ofString(message3));
        } else {
            builder.method(message, BodyPublishers.noBody());
        }

        return this.httpClient.send(builder.build(), BodyHandlers.ofString());
    }

    public static boolean isAuthenticated() {
        return LaunchGuard.getTicket() != null || getDevToken() != null;
    }

    public static void applyAuthHeaders(Builder builder) {
        String s = LaunchGuard.getTicket();
        if (s != null) {
            builder.header("Authorization", "Adapt " + s).header("X-Adapt-Device", DeviceId.get());
        } else {
            String s1 = getDevToken();
            if (s1 == null) {
                throw new IllegalStateException(
                    LaunchGuard.isDevRun()
                        ? "Dev run: save your dev token as adapt-dev-token.txt in the run folder (Staff panel on the website)."
                        : "Chat needs Adapt opened from the launcher."
                );
            }

            builder.header("Authorization", "AdaptDev " + s1);
        }
    }

    public static String extractError(JsonObject json, String message) {
        return json != null && json.has("error") ? json.get("error").getAsString() : message;
    }

    public static JsonObject parseJson(String text) {
        try {
            return JsonParser.parseString(text).getAsJsonObject();
        } catch (Exception exception) {
            return null;
        }
    }

    public static List<ChatService.Message> parseMessages(JsonArray array) {
        ArrayList arraylist = new ArrayList();
        if (array == null) {
            return arraylist;
        }

        for (JsonElement jsonelement : array) {
            try {
                arraylist.add(parseMessage(jsonelement.getAsJsonObject()));
            } catch (Exception exception) {
            }
        }

        return arraylist;
    }

    public static ChatService.Message parseMessage(JsonObject json) {
        JsonElement jsonelement = json.get("replyTo");
        boolean flag = jsonelement != null && jsonelement.isJsonObject();
        String s = null;
        String s1 = null;
        long i = -1L;
        if (flag) {
            JsonObject jsonobject = jsonelement.getAsJsonObject();
            if (jsonobject.has("userId") && !jsonobject.get("userId").isJsonNull()) {
                i = jsonobject.get("userId").getAsLong();
            }

            if (jsonobject.has("username") && !jsonobject.get("username").isJsonNull()) {
                s = jsonobject.get("username").getAsString();
            }

            if (jsonobject.has("body") && !jsonobject.get("body").isJsonNull()) {
                s1 = jsonobject.get("body").getAsString();
            }
        }

        return new ChatService.Message(
            json.get("id").getAsLong(),
            json.get("userId").getAsLong(),
            json.get("username").getAsString(),
            json.get("body").getAsString(),
            json.get("createdAt").getAsLong(),
            json.has("editedAt") && !json.get("editedAt").isJsonNull(),
            json.has("source") && "client".equals(json.get("source").getAsString()),
            flag,
            i,
            s,
            s1
        );
    }

    public enum Channel {
        IRC("irc", "IRC"),
        WEB("web", "Website");

        public final String id;
        public final String label;

        Channel(String text, String text2) {
            this.id = text;
            this.label = text2;
        }
    }

    public record Message(
        long id,
        long userId,
        String username,
        String body,
        long createdAt,
        boolean edited,
        boolean fromGame,
        boolean isReply,
        long replyUserId,
        String replyUser,
        String replyBody
    ) {
    }

    private static final class State {
        public volatile List<ChatService.Message> messages = List.of();
        public volatile boolean loaded;
        public volatile int unreadCount;
        public long lastMessageId;
        public long sinceTimestamp;
        public int pollCount;

        public State() {
        }
    }
}
