package dev.adapt.v3.account;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public final class MicrosoftAuth {
    public static final String CLIENT_ID = "2fa2913a-ebf9-4bb7-b16a-c3e7e437b5bc";
    public static final String CLIENT_SECRET = "yax8Q~qfPbqyR_6Qmau3J93dVFuO5cvk4w4uQcLk";
    public static final int CALLBACK_PORT = 8247;
    public static final String REDIRECT_URI = "http://localhost:8247";
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15L)).build();
    public static HttpServer callbackServer;
    public static CompletableFuture<String> pendingCodeFuture;

    public static String buildAuthorizeUrl() {
        return "https://login.live.com/oauth20_authorize.srf?client_id=2fa2913a-ebf9-4bb7-b16a-c3e7e437b5bc&response_type=code&redirect_uri="
            + urlEncode("http://localhost:8247")
            + "&scope="
            + urlEncode("XboxLive.signin offline_access")
            + "&prompt=login";
    }

    public static synchronized CompletableFuture<String> awaitAuthCode() throws IOException {
        cancelCallback();
        CompletableFuture<String> completablefuture = new CompletableFuture<>();
        HttpServer httpserver = HttpServer.create(new InetSocketAddress("localhost", 8247), 0);
        httpserver.createContext(
            "/",
            exchange -> {
                String s = exchange.getRequestURI().getRawQuery();
                Map<String, String> map = parseQuery(s);
                String s1 = (String)map.get("code");
                boolean flag = s1 != null && !s1.isEmpty();
                String s2 = renderCallbackHtml(
                    flag ? "You're signed in" : "Sign-in failed",
                    flag
                        ? "Your account was linked to Adapt. You can close this tab and return to the game."
                        : "Microsoft didn't return a code"
                            + (map.containsKey("error_description") ? ": " + (String)map.get("error_description") : ".")
                            + " Try again from the Accounts menu."
                );
                byte[] abyte = s2.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, abyte.length);

                try (OutputStream outputstream = exchange.getResponseBody()) {
                    outputstream.write(abyte);
                }

                if (flag) {
                    completablefuture.complete(s1);
                } else {
                    completablefuture.completeExceptionally(new IOException(map.getOrDefault("error_description", "No authorization code")));
                }

                stopServer(httpserver);
            }
        );
        httpserver.setExecutor(Executors.newSingleThreadExecutor(action -> {
            Thread thread = new Thread(action, "Adapt-MS-Callback");
            thread.setDaemon(true);
            return thread;
        }));
        httpserver.start();
        callbackServer = httpserver;
        pendingCodeFuture = completablefuture;
        return completablefuture;
    }

    public static synchronized void cancelCallback() {
        if (pendingCodeFuture != null && !pendingCodeFuture.isDone()) {
            pendingCodeFuture.cancel(true);
        }

        pendingCodeFuture = null;
        if (callbackServer != null) {
            stopServer(callbackServer);
        }
    }

    public static synchronized void stopServer(HttpServer httpServer) {
        Thread.ofVirtual().start(() -> httpServer.stop(1));
        if (callbackServer == httpServer) {
            callbackServer = null;
        }
    }

    public static MicrosoftAuth.AuthResult loginWithCode(String code) throws IOException, InterruptedException {
        Map<String, String> linkedhashmap = new LinkedHashMap<>();
        linkedhashmap.put("client_id", "2fa2913a-ebf9-4bb7-b16a-c3e7e437b5bc");
        linkedhashmap.put("client_secret", "yax8Q~qfPbqyR_6Qmau3J93dVFuO5cvk4w4uQcLk");
        linkedhashmap.put("code", code);
        linkedhashmap.put("grant_type", "authorization_code");
        linkedhashmap.put("redirect_uri", "http://localhost:8247");
        return completeMicrosoftFlow(postForm("https://login.live.com/oauth20_token.srf", linkedhashmap));
    }

    public static MicrosoftAuth.AuthResult loginWithRefreshToken(String token) throws IOException, InterruptedException {
        Map<String, String> linkedhashmap = new LinkedHashMap<>();
        linkedhashmap.put("client_id", "2fa2913a-ebf9-4bb7-b16a-c3e7e437b5bc");
        linkedhashmap.put("client_secret", "yax8Q~qfPbqyR_6Qmau3J93dVFuO5cvk4w4uQcLk");
        linkedhashmap.put("refresh_token", token);
        linkedhashmap.put("grant_type", "refresh_token");
        linkedhashmap.put("redirect_uri", "http://localhost:8247");
        return completeMicrosoftFlow(postForm("https://login.live.com/oauth20_token.srf", linkedhashmap));
    }

    public static String cleanToken(String token) {
        if (token == null) {
            return "";
        }

        token = token.trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }

        return token.replace("\"", "").replaceAll("\\s+", "");
    }

    public static MicrosoftAuth.AuthResult loginWithAccessToken(String token) throws IOException, InterruptedException {
        String s = cleanToken(token);
        if (s.isEmpty()) {
            throw new IOException("Paste a token first");
        } else if (s.split("\\.").length != 3) {
            throw new IOException("That doesn't look like a Minecraft access token");
        } else {
            HttpResponse httpresponse = HTTP_CLIENT.send(
                HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                    .timeout(Duration.ofSeconds(15L))
                    .header("Authorization", "Bearer " + s)
                    .GET()
                    .build(),
                BodyHandlers.ofString()
            );
            if (httpresponse.statusCode() == 401) {
                throw new IOException("Token is invalid or has expired");
            } else if (httpresponse.statusCode() == 404) {
                throw new IOException("This account doesn't own Minecraft: Java Edition");
            } else if (httpresponse.statusCode() != 200) {
                throw new IOException("Minecraft services returned " + httpresponse.statusCode());
            } else {
                JsonObject jsonobject = JsonParser.parseString((String)httpresponse.body()).getAsJsonObject();
                if (jsonobject.has("id") && jsonobject.has("name")) {
                    return new MicrosoftAuth.AuthResult(jsonobject.get("name").getAsString(), jsonobject.get("id").getAsString(), s, null);
                } else {
                    throw new IOException("Token has no Minecraft profile");
                }
            }
        }
    }

    public static MicrosoftAuth.AuthResult completeMicrosoftFlow(JsonObject json) throws IOException, InterruptedException {
        if (!json.has("access_token")) {
            throw new IOException("Microsoft rejected the sign-in" + extractErrorSuffix(json));
        }

        String s = json.get("access_token").getAsString();
        String s1 = json.has("refresh_token") ? json.get("refresh_token").getAsString() : null;
        JsonObject jsonobject = new JsonObject();
        JsonObject jsonobject1 = new JsonObject();
        jsonobject1.addProperty("AuthMethod", "RPS");
        jsonobject1.addProperty("SiteName", "user.auth.xboxlive.com");
        jsonobject1.addProperty("RpsTicket", "d=" + s);
        jsonobject.add("Properties", jsonobject1);
        jsonobject.addProperty("RelyingParty", "http://auth.xboxlive.com");
        jsonobject.addProperty("TokenType", "JWT");
        JsonObject jsonobject2 = postJson("https://user.auth.xboxlive.com/user/authenticate", jsonobject, null);
        if (!jsonobject2.has("Token")) {
            throw new IOException("Xbox Live sign-in failed" + extractErrorSuffix(jsonobject2));
        }

        JsonObject jsonobject3 = new JsonObject();
        JsonObject jsonobject4 = new JsonObject();
        jsonobject4.addProperty("SandboxId", "RETAIL");
        JsonArray jsonarray = new JsonArray();
        jsonarray.add(jsonobject2.get("Token").getAsString());
        jsonobject4.add("UserTokens", jsonarray);
        jsonobject3.add("Properties", jsonobject4);
        jsonobject3.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        jsonobject3.addProperty("TokenType", "JWT");
        JsonObject jsonobject5 = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", jsonobject3, null);
        if (!jsonobject5.has("Token")) {
            long i = jsonobject5.has("XErr") ? jsonobject5.get("XErr").getAsLong() : 0L;
            if (i == 2148916233L) {
                throw new IOException("This Microsoft account has no Xbox profile yet");
            } else if (i == 2148916238L) {
                throw new IOException("Child account: add it to a Microsoft family first");
            } else {
                throw new IOException("Xbox authorization failed" + extractErrorSuffix(jsonobject5));
            }
        } else {
            String s2 = jsonobject5.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();
            JsonObject jsonobject6 = new JsonObject();
            jsonobject6.addProperty("identityToken", "XBL3.0 x=" + s2 + ";" + jsonobject5.get("Token").getAsString());
            JsonObject jsonobject7 = postJson("https://api.minecraftservices.com/authentication/login_with_xbox", jsonobject6, null);
            if (!jsonobject7.has("access_token")) {
                throw new IOException("Minecraft services sign-in failed" + extractErrorSuffix(jsonobject7));
            } else {
                String s3 = jsonobject7.get("access_token").getAsString();
                JsonObject jsonobject8 = getJson("https://api.minecraftservices.com/minecraft/profile", s3);
                if (jsonobject8.has("id") && jsonobject8.has("name")) {
                    return new MicrosoftAuth.AuthResult(jsonobject8.get("name").getAsString(), jsonobject8.get("id").getAsString(), s3, s1);
                } else {
                    throw new IOException("This account doesn't own Minecraft: Java Edition");
                }
            }
        }
    }

    public static JsonObject postForm(String text, Map<String, String> map) throws IOException, InterruptedException {
        String s = map.entrySet()
            .stream()
            .map(entry -> urlEncode((String)entry.getKey()) + "=" + urlEncode((String)entry.getValue()))
            .collect(Collectors.joining("&"));
        return sendJsonRequest(
            HttpRequest.newBuilder(URI.create(text))
                .timeout(Duration.ofSeconds(20L))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(BodyPublishers.ofString(s))
                .build()
        );
    }

    public static JsonObject postJson(String text, JsonObject json, String text2) throws IOException, InterruptedException {
        Builder builder = HttpRequest.newBuilder(URI.create(text))
            .timeout(Duration.ofSeconds(20L))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(BodyPublishers.ofString(json.toString()));
        if (text2 != null) {
            builder.header("Authorization", "Bearer " + text2);
        }

        return sendJsonRequest(builder.build());
    }

    public static JsonObject getJson(String text, String text2) throws IOException, InterruptedException {
        return sendJsonRequest(
            HttpRequest.newBuilder(URI.create(text))
                .timeout(Duration.ofSeconds(20L))
                .header("Authorization", "Bearer " + text2)
                .header("Accept", "application/json")
                .GET()
                .build()
        );
    }

    public static JsonObject sendJsonRequest(HttpRequest httpRequest) throws IOException, InterruptedException {
        HttpResponse httpresponse = HTTP_CLIENT.send(httpRequest, BodyHandlers.ofString());
        String s = (String)httpresponse.body();
        if (s != null && !s.isBlank()) {
            try {
                JsonElement jsonelement = JsonParser.parseString(s);
                return jsonelement.isJsonObject() ? jsonelement.getAsJsonObject() : new JsonObject();
            } catch (Exception exception) {
                JsonObject jsonobject1 = new JsonObject();
                jsonobject1.addProperty("status", httpresponse.statusCode());
                return jsonobject1;
            }
        } else {
            JsonObject jsonobject = new JsonObject();
            jsonobject.addProperty("status", httpresponse.statusCode());
            return jsonobject;
        }
    }

    public static String extractErrorSuffix(JsonObject json) {
        for (String s : new String[]{"error_description", "errorMessage", "error", "Message", "status"}) {
            if (json.has(s) && !json.get(s).isJsonNull()) {
                return " (" + json.get(s).getAsString() + ")";
            }
        }

        return "";
    }

    public static String urlEncode(String url) {
        return URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

    public static Map<String, String> parseQuery(String text) {
        Map<String, String> linkedhashmap = new LinkedHashMap<>();
        if (text == null) {
            return linkedhashmap;
        }

        for (String s : text.split("&")) {
            int i = s.indexOf(61);
            if (i > 0) {
                linkedhashmap.put(URLDecoder.decode(s.substring(0, i), StandardCharsets.UTF_8), URLDecoder.decode(s.substring(i + 1), StandardCharsets.UTF_8));
            }
        }

        return linkedhashmap;
    }

    public static String renderCallbackHtml(String text, String text2) {
        return "<!doctype html><html><head><meta charset=utf-8><title>Adapt</title><style>body{margin:0;height:100vh;display:grid;place-items:center;background:#0c0c10;color:#fff;font-family:system-ui,Segoe UI,sans-serif}.c{padding:36px 44px;border-radius:16px;background:rgba(255,255,255,.04);border:1px solid rgba(255,255,255,.1);max-width:420px;text-align:center}h1{font-size:20px;margin:0 0 10px}p{margin:0;color:rgba(255,255,255,.6);line-height:1.5}</style></head><body><div class=c><h1>"
            + escapeHtml(text)
            + "</h1><p>"
            + escapeHtml(text2)
            + "</p></div></body></html>";
    }

    public static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public record AuthResult(String name, String uuid, String accessToken, String refreshToken) {
    }
}
