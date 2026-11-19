package dev.adapt.v3.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.services.MinecraftServicesDiscoveryService;
import com.mojang.authlib.services.ProfileResult;
import com.mojang.util.UndashedUuid;
import dev.adapt.v3.Adapt;
import dev.adapt.v3.mixin.MinecraftAccessor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerSkin;

public final class AccountManager {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public final Path accountsFile = FabricLoader.getInstance().getConfigDir().resolve("adaptv3-accounts.json");
    public final List<AccountManager.Account> accounts = new ArrayList<>();
    public final ExecutorService executor = Executors.newSingleThreadExecutor(action -> {
        Thread thread = new Thread(action, "Adapt-Accounts");
        thread.setDaemon(true);
        return thread;
    });
    public User initialUser;
    public boolean autoLogin;
    public String lastAccount;
    public boolean autoLoginAttempted;
    public volatile int loginStage;
    public final Map<UUID, Identifier> skinCache = new ConcurrentHashMap<>();
    public final Set<UUID> pendingSkinLookups = ConcurrentHashMap.newKeySet();
    public final Map<UUID, Long> skinRetryAfter = new ConcurrentHashMap<>();
    public final Map<UUID, Integer> skinFailures = new ConcurrentHashMap<>();

    public AccountManager() {
        this.load();
    }

    public boolean isAutoLogin() {
        return this.autoLogin;
    }

    public void setAutoLogin(boolean autoLogin) {
        this.autoLogin = autoLogin;
        this.save();
    }

    public Identifier getSkinTexture(UUID uuid) {
        Identifier identifier = this.skinCache.get(uuid);
        if (identifier != null) {
            return identifier;
        }

        Minecraft minecraft = Minecraft.getInstance();
        long i = System.currentTimeMillis();
        if (i >= this.skinRetryAfter.getOrDefault(uuid, 0L) && this.pendingSkinLookups.add(uuid)) {
            CompletableFuture.<ProfileResult>supplyAsync(
                    () -> ((MinecraftAccessor)minecraft).adapt$services().sessionService().fetchProfile(uuid, true), Util.nonCriticalIoPool()
                )
                .thenComposeAsync(
                    profileResult -> profileResult == null ? CompletableFuture.completedFuture(Optional.empty()) : minecraft.getSkinManager().get(profileResult.profile()), minecraft
                )
                .whenComplete((optional, error) -> {
                    if (error == null && optional != null && optional.isPresent()) {
                        this.skinCache.put(uuid, ((PlayerSkin)optional.get()).body().texturePath());
                        this.skinFailures.remove(uuid);
                    } else {
                        int j = this.skinFailures.merge(uuid, 1, Integer::sum);
                        this.skinRetryAfter.put(uuid, System.currentTimeMillis() + Math.min(60000L, 3000L << Math.min(5, j - 1)));
                        if (error != null) {
                            Adapt.LOGGER.debug("Skin lookup for {} failed (attempt {})", uuid, j, error);
                        }
                    }

                    this.pendingSkinLookups.remove(uuid);
                });
        }

        return DefaultPlayerSkin.get(uuid).body().texturePath();
    }

    public static UUID parseUuid(AccountManager.Account account) {
        try {
            return UndashedUuid.fromStringLenient(account.uuid);
        } catch (Exception exception) {
            return new UUID(0L, 0L);
        }
    }

    public Optional<AccountManager.Account> getActiveAccount() {
        for (AccountManager.Account accountmanager$account : this.accounts) {
            if (this.isActiveAccount(accountmanager$account)) {
                return Optional.of(accountmanager$account);
            }
        }

        return Optional.empty();
    }

    public void tryAutoLogin(Consumer<AccountManager.Account> consumer) {
        if (!this.autoLoginAttempted) {
            this.autoLoginAttempted = true;
            this.getInitialUser();
            if (this.autoLogin && this.lastAccount != null) {
                for (AccountManager.Account accountmanager$account : this.accounts) {
                    if (accountmanager$account.uuid.equalsIgnoreCase(this.lastAccount) && !this.isActiveAccount(accountmanager$account)) {
                        this.login(accountmanager$account).thenAccept(consumer).exceptionally(error -> {
                            Adapt.LOGGER.warn("Auto sign-in failed for {}", accountmanager$account.name, error);
                            return null;
                        });
                        return;
                    }
                }
            }
        }
    }

    public List<AccountManager.Account> getAccounts() {
        return Collections.unmodifiableList(this.accounts);
    }

    public User getInitialUser() {
        if (this.initialUser == null) {
            this.initialUser = Minecraft.getInstance().getUser();
        }

        return this.initialUser;
    }

    public boolean isActiveAccount(AccountManager.Account account) {
        return matchesUuid(Minecraft.getInstance().getUser().getProfileId(), account.uuid);
    }

    public boolean isInitialSession() {
        User user = Minecraft.getInstance().getUser();
        return user == this.getInitialUser() || user.getProfileId().equals(this.getInitialUser().getProfileId());
    }

    public static boolean matchesUuid(UUID uuid, String text) {
        try {
            return uuid.equals(UndashedUuid.fromStringLenient(text));
        } catch (Exception exception) {
            return false;
        }
    }

    public CompletableFuture<AccountManager.Account> loginWithMicrosoft(Consumer<String> consumer) {
        this.getInitialUser();
        CompletableFuture<AccountManager.Account> completablefuture = new CompletableFuture<>();

        CompletableFuture<String> completablefuture1;
        try {
            completablefuture1 = MicrosoftAuth.awaitAuthCode();
        } catch (Exception exception) {
            completablefuture.completeExceptionally(new IllegalStateException("Couldn't open the sign-in callback (port 8247 busy?)", exception));
            return completablefuture;
        }

        this.loginStage = 0;
        consumer.accept(MicrosoftAuth.buildAuthorizeUrl());
        completablefuture1.thenAcceptAsync(text -> {
            this.loginStage = 1;

            try {
                MicrosoftAuth.AuthResult microsoftauth$authresult = MicrosoftAuth.loginWithCode(text);
                Minecraft.getInstance().execute(() -> {
                    AccountManager.Account accountmanager$account = this.upsertAccount(microsoftauth$authresult, AccountManager.Type.MICROSOFT);
                    this.applyAuthResult(microsoftauth$authresult);
                    accountmanager$account.lastUsed = System.currentTimeMillis();
                    this.save();
                    completablefuture.complete(accountmanager$account);
                });
            } catch (Exception exception1) {
                completablefuture.completeExceptionally(exception1);
            }
        }, this.executor).exceptionally(error -> {
            completablefuture.completeExceptionally(error);
            return null;
        });
        return completablefuture;
    }

    public void cancelMicrosoftLogin() {
        MicrosoftAuth.cancelCallback();
    }

    public CompletableFuture<AccountManager.Account> loginWithToken(String token) {
        this.getInitialUser();
        CompletableFuture<AccountManager.Account> completablefuture = new CompletableFuture<>();
        this.executor.execute(() -> {
            try {
                MicrosoftAuth.AuthResult microsoftauth$authresult = MicrosoftAuth.loginWithAccessToken(token);
                Minecraft.getInstance().execute(() -> {
                    AccountManager.Account accountmanager$account = this.upsertAccount(microsoftauth$authresult, AccountManager.Type.TOKEN);
                    this.applyAuthResult(microsoftauth$authresult);
                    accountmanager$account.lastUsed = System.currentTimeMillis();
                    this.save();
                    completablefuture.complete(accountmanager$account);
                });
            } catch (Exception exception) {
                completablefuture.completeExceptionally(exception);
            }
        });
        return completablefuture;
    }

    public CompletableFuture<AccountManager.Account> login(AccountManager.Account account) {
        this.getInitialUser();
        CompletableFuture<AccountManager.Account> completablefuture = new CompletableFuture<>();
        this.executor
            .execute(
                () -> {
                    try {
                        MicrosoftAuth.AuthResult microsoftauth$authresult = account.type == AccountManager.Type.TOKEN
                            ? MicrosoftAuth.loginWithAccessToken(account.accessToken)
                            : MicrosoftAuth.loginWithRefreshToken(account.refreshToken);
                        Minecraft.getInstance().execute(() -> {
                            account.name = microsoftauth$authresult.name();
                            account.uuid = microsoftauth$authresult.uuid();
                            if (microsoftauth$authresult.refreshToken() != null) {
                                account.refreshToken = microsoftauth$authresult.refreshToken();
                            }

                            account.lastUsed = System.currentTimeMillis();
                            this.applyAuthResult(microsoftauth$authresult);
                            this.save();
                            completablefuture.complete(account);
                        });
                    } catch (Exception exception) {
                        completablefuture.completeExceptionally(exception);
                    }
                }
            );
        return completablefuture;
    }

    public void restoreInitialSession() {
        User user = this.getInitialUser();
        this.lastAccount = null;
        this.applySession(user);
        this.save();
    }

    public void removeAccount(AccountManager.Account account) {
        this.accounts.remove(account);
        this.save();
    }

    public void moveAccount(AccountManager.Account account, int value) {
        int i = this.accounts.indexOf(account);
        int j = Math.max(0, Math.min(this.accounts.size() - 1, value));
        if (i >= 0 && i != j) {
            this.accounts.remove(i);
            this.accounts.add(j, account);
            this.save();
        }
    }

    public AccountManager.Account upsertAccount(MicrosoftAuth.AuthResult authResult, AccountManager.Type type) {
        AccountManager.Account accountmanager$account = null;

        for (AccountManager.Account accountmanager$account1 : this.accounts) {
            if (accountmanager$account1.uuid.equalsIgnoreCase(authResult.uuid())) {
                accountmanager$account = accountmanager$account1;
            }
        }

        if (accountmanager$account == null) {
            accountmanager$account = new AccountManager.Account(authResult.name(), authResult.uuid(), type);
            accountmanager$account.newlyAdded = true;
            this.accounts.add(accountmanager$account);
        } else {
            accountmanager$account.newlyAdded = false;
        }

        accountmanager$account.name = authResult.name();
        if (type == AccountManager.Type.MICROSOFT) {
            accountmanager$account.type = AccountManager.Type.MICROSOFT;
            accountmanager$account.refreshToken = authResult.refreshToken();
            accountmanager$account.accessToken = null;
        } else if (accountmanager$account.type == AccountManager.Type.TOKEN) {
            accountmanager$account.accessToken = authResult.accessToken();
        }

        return accountmanager$account;
    }

    public void applyAuthResult(MicrosoftAuth.AuthResult authResult) {
        UUID uuid = UndashedUuid.fromStringLenient(authResult.uuid());
        this.lastAccount = authResult.uuid();
        this.applySession(new User(authResult.name(), uuid, authResult.accessToken(), Optional.empty(), Optional.empty()));
    }

    public void applySession(User user) {
        Minecraft minecraft = Minecraft.getInstance();
        MinecraftAccessor minecraftaccessor = (MinecraftAccessor)minecraft;

        UserApiService userapiservice;
        try {
            userapiservice = MinecraftServicesDiscoveryService.create(minecraft.getProxy(), true).createUserApiService(user.getAccessToken());
        } catch (Exception exception) {
            Adapt.LOGGER.warn("Couldn't create user API service for {}; chat signing disabled", user.getName(), exception);
            userapiservice = UserApiService.OFFLINE;
        }

        UserApiService userapiservice1 = userapiservice;
        minecraftaccessor.adapt$setUser(user);
        minecraftaccessor.adapt$setUserApiService(userapiservice1);
        minecraftaccessor.adapt$setUserPropertiesFuture(CompletableFuture.supplyAsync(() -> {
            try {
                return userapiservice1.fetchProperties();
            } catch (Exception exception1) {
                return UserApiService.OFFLINE_PROPERTIES;
            }
        }, Util.nonCriticalIoPool()));
        minecraftaccessor.adapt$setProfileFuture(
            CompletableFuture.supplyAsync(
                () -> minecraftaccessor.adapt$services().sessionService().fetchProfile(user.getProfileId(), true), Util.nonCriticalIoPool()
            )
        );
        minecraftaccessor.adapt$setProfileKeyPairManager(ProfileKeyPairManager.create(userapiservice1, user, minecraft.gameDirectory.toPath()));
        minecraftaccessor.adapt$setReportingContext(ReportingContext.create(ReportEnvironment.local(), userapiservice1));
        Adapt.LOGGER.info("Switched session to {}", user.getName());
    }

    public void load() {
        if (Files.exists(this.accountsFile)) {
            try {
                JsonObject jsonobject = JsonParser.parseString(Files.readString(this.accountsFile)).getAsJsonObject();
                if (jsonobject.has("autoLogin")) {
                    this.autoLogin = jsonobject.get("autoLogin").getAsBoolean();
                }

                if (jsonobject.has("lastAccount") && !jsonobject.get("lastAccount").isJsonNull()) {
                    this.lastAccount = jsonobject.get("lastAccount").getAsString();
                }

                JsonArray jsonarray = jsonobject.getAsJsonArray("accounts");
                if (jsonarray == null) {
                    return;
                }

                for (JsonElement jsonelement : jsonarray) {
                    JsonObject jsonobject1 = jsonelement.getAsJsonObject();
                    if (jsonobject1.has("uuid") && jsonobject1.has("name")) {
                        AccountManager.Type accountmanager$type = AccountManager.Type.MICROSOFT;
                        if (jsonobject1.has("type")) {
                            try {
                                accountmanager$type = AccountManager.Type.valueOf(jsonobject1.get("type").getAsString());
                            } catch (Exception exception) {
                            }
                        }

                        AccountManager.Account accountmanager$account = new AccountManager.Account(
                            jsonobject1.get("name").getAsString(), jsonobject1.get("uuid").getAsString(), accountmanager$type
                        );
                        if (jsonobject1.has("refreshToken")) {
                            accountmanager$account.refreshToken = jsonobject1.get("refreshToken").getAsString();
                        }

                        if (jsonobject1.has("accessToken")) {
                            accountmanager$account.accessToken = jsonobject1.get("accessToken").getAsString();
                        }

                        if (accountmanager$type == AccountManager.Type.MICROSOFT
                            ? accountmanager$account.refreshToken != null
                            : accountmanager$account.accessToken != null) {
                            if (jsonobject1.has("added")) {
                                accountmanager$account.added = jsonobject1.get("added").getAsLong();
                            }

                            if (jsonobject1.has("lastUsed")) {
                                accountmanager$account.lastUsed = jsonobject1.get("lastUsed").getAsLong();
                            }

                            this.accounts.add(accountmanager$account);
                        }
                    }
                }
            } catch (Exception exception1) {
                Adapt.LOGGER.error("Failed to load accounts", exception1);
            }
        }
    }

    public void save() {
        try {
            JsonObject jsonobject = new JsonObject();
            jsonobject.addProperty("autoLogin", this.autoLogin);
            if (this.lastAccount != null) {
                jsonobject.addProperty("lastAccount", this.lastAccount);
            }

            JsonArray jsonarray = new JsonArray();

            for (AccountManager.Account accountmanager$account : this.accounts) {
                JsonObject jsonobject1 = new JsonObject();
                jsonobject1.addProperty("name", accountmanager$account.name);
                jsonobject1.addProperty("uuid", accountmanager$account.uuid);
                jsonobject1.addProperty("type", accountmanager$account.type.name());
                if (accountmanager$account.refreshToken != null) {
                    jsonobject1.addProperty("refreshToken", accountmanager$account.refreshToken);
                }

                if (accountmanager$account.accessToken != null) {
                    jsonobject1.addProperty("accessToken", accountmanager$account.accessToken);
                }

                jsonobject1.addProperty("added", accountmanager$account.added);
                jsonobject1.addProperty("lastUsed", accountmanager$account.lastUsed);
                jsonarray.add(jsonobject1);
            }

            jsonobject.add("accounts", jsonarray);
            Files.createDirectories(this.accountsFile.getParent());
            Files.writeString(this.accountsFile, GSON.toJson(jsonobject));
        } catch (Exception exception) {
            Adapt.LOGGER.error("Failed to save accounts", exception);
        }
    }

    public static final class Account {
        public String name;
        public String uuid;
        public AccountManager.Type type;
        public String refreshToken;
        public String accessToken;
        public long added;
        public long lastUsed;
        public transient boolean newlyAdded;

        public Account(String text, String text2, AccountManager.Type type) {
            this.name = text;
            this.uuid = text2;
            this.type = type;
            this.added = System.currentTimeMillis();
        }
    }

    public enum Type {
        MICROSOFT("Microsoft"),
        TOKEN("Token");

        public final String label;

        Type(String text) {
            this.label = text;
        }
    }
}
