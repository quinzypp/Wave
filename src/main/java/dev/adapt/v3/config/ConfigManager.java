package dev.adapt.v3.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.Blaze3D;
import dev.adapt.v3.Adapt;
import dev.adapt.v3.binding.Bind;
import dev.adapt.v3.binding.BindRegistry;
import dev.adapt.v3.binding.BindingManager;
import dev.adapt.v3.binding.type.InputType;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.impl.client.ThemeModule;
import dev.adapt.v3.feature.module.impl.combat.velocity.VelocityModule;
import dev.adapt.v3.feature.module.impl.world.WorldEditorModule;
import dev.adapt.v3.feature.property.Property;
import dev.adapt.v3.feature.property.impl.MultiBooleanProperty;
import dev.adapt.v3.hud.HudModule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;

public final class ConfigManager {
    public static final int CONFIG_VERSION = 5;
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public final Path mainConfigFile = FabricLoader.getInstance().getConfigDir().resolve("adaptv3.json");
    public final Path configsDir = FabricLoader.getInstance().getConfigDir().resolve("adaptv3").resolve("configs");
    public String activeConfig;

    public Path getMainConfigFile() {
        return this.mainConfigFile;
    }

    public Path getConfigsDir() {
        return this.configsDir;
    }

    public boolean openConfigsFolder() {
        try {
            Files.createDirectories(this.configsDir);
            Blaze3D.openUri(this.configsDir.toUri());
            return true;
        } catch (Exception exception) {
            Adapt.LOGGER.error("Failed to open the configs folder", exception);
            return false;
        }
    }

    public void save() {
        try {
            Files.createDirectories(this.mainConfigFile.getParent());
            JsonObject jsonobject = this.serialize(true);
            if (this.activeConfig != null) {
                jsonobject.addProperty("activeConfig", this.activeConfig);
            }

            Files.writeString(this.mainConfigFile, GSON.toJson(jsonobject));
        } catch (Exception exception) {
            Adapt.LOGGER.error("Failed to save AdaptV3 config", exception);
        }
    }

    public void load() {
        if (Files.exists(this.mainConfigFile)) {
            try {
                JsonObject jsonobject = JsonParser.parseString(Files.readString(this.mainConfigFile)).getAsJsonObject();
                this.deserialize(jsonobject, true);
                if (jsonobject.has("activeConfig")) {
                    this.activeConfig = jsonobject.get("activeConfig").getAsString();
                }
            } catch (Exception exception) {
                Adapt.LOGGER.error("Failed to load AdaptV3 config", exception);
            }
        }
    }

    public static boolean isValidConfigName(String name) {
        return name != null && !name.isEmpty() && name.length() <= 48 && name.matches("[A-Za-z0-9_.-]+") && !name.startsWith(".");
    }

    public Path resolveConfigPath(String name) {
        return this.configsDir.resolve(name + ".json");
    }

    public String getActiveConfig() {
        return this.activeConfig != null && this.exists(this.activeConfig) ? this.activeConfig : null;
    }

    public int countEnabledModules(String name) {
        if (!this.exists(name)) {
            return 0;
        }

        try {
            JsonObject jsonobject = JsonParser.parseString(Files.readString(this.resolveConfigPath(name))).getAsJsonObject().getAsJsonObject("modules");
            if (jsonobject == null) {
                return 0;
            }

            int i = 0;

            for (Module module : Adapt.getInstance().getModuleManager().getAll()) {
                if (module.canToggle() && !(module instanceof HudModule)) {
                    JsonElement jsonelement = jsonobject.get(module.getId());
                    if (jsonelement != null
                        && jsonelement.isJsonObject()
                        && jsonelement.getAsJsonObject().has("enabled")
                        && jsonelement.getAsJsonObject().get("enabled").getAsBoolean()) {
                        i++;
                    }
                }
            }

            return i;
        } catch (Exception exception) {
            return 0;
        }
    }

    public long getLastModified(String text) {
        try {
            return this.exists(text) ? Files.getLastModifiedTime(this.resolveConfigPath(text)).toMillis() : 0L;
        } catch (Exception exception) {
            return 0L;
        }
    }

    public boolean exists(String text) {
        return isValidConfigName(text) && Files.exists(this.resolveConfigPath(text));
    }

    public boolean saveConfig(String name) {
        if (!isValidConfigName(name)) {
            return false;
        }

        try {
            Files.createDirectories(this.configsDir);
            Files.writeString(this.resolveConfigPath(name), GSON.toJson(this.serialize(false)));
            this.activeConfig = name;
            this.save();
            return true;
        } catch (Exception exception) {
            Adapt.LOGGER.error("Failed to save config " + name, exception);
            return false;
        }
    }

    public boolean loadConfig(String name) {
        if (!this.exists(name)) {
            return false;
        }

        try {
            this.deserialize(JsonParser.parseString(Files.readString(this.resolveConfigPath(name))).getAsJsonObject(), false);
            this.activeConfig = name;
            this.save();
            return true;
        } catch (Exception exception) {
            Adapt.LOGGER.error("Failed to load config " + name, exception);
            return false;
        }
    }

    public boolean deleteConfig(String name) {
        if (!this.exists(name)) {
            return false;
        }

        try {
            Files.delete(this.resolveConfigPath(name));
            if (name.equals(this.activeConfig)) {
                this.activeConfig = null;
                this.save();
            }

            return true;
        } catch (Exception exception) {
            Adapt.LOGGER.error("Failed to delete config " + name, exception);
            return false;
        }
    }

    public List<String> listConfigs() {
        if (!Files.isDirectory(this.configsDir)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(this.configsDir)) {
            return stream.<String>map(path -> path.getFileName().toString())
                .filter(text -> text.endsWith(".json"))
                .map(text2 -> text2.substring(0, text2.length() - 5))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        } catch (Exception exception) {
            return List.of();
        }
    }

    public JsonObject serialize(boolean flag) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("version", 5);
        JsonObject jsonobject1 = new JsonObject();
        BindRegistry bindregistry = Adapt.getInstance().getBindingManager().getRegistry();

        for (Module module : Adapt.getInstance().getModuleManager().getAll()) {
            JsonObject jsonobject2 = new JsonObject();
            jsonobject2.addProperty("enabled", module.isEnabled());
            if (flag) {
                bindregistry.getBind(module).ifPresent(bind -> {
                    JsonObject jsonobject4 = new JsonObject();
                    jsonobject4.addProperty("code", bind.code());
                    jsonobject4.addProperty("type", bind.type().name());
                    jsonobject2.add("bind", jsonobject4);
                });
            }

            if (module instanceof HudModule hudmodule) {
                jsonobject2.addProperty("x", hudmodule.getAnchorX());
                jsonobject2.addProperty("y", hudmodule.getAnchorY());
            }

            JsonObject jsonobject3 = new JsonObject();

            for (Property property : module.getProperties()) {
                if (jsonobject3.has(property.getKey())) {
                    Adapt.LOGGER.warn("{} has two settings saved as \"{}\"; only the last is kept", module.getName(), property.getKey());
                }

                jsonobject3.add(property.getKey(), property.toJson());
            }

            jsonobject2.add("properties", jsonobject3);
            jsonobject1.add(module.getId(), jsonobject2);
        }

        jsonobject.add("modules", jsonobject1);
        return jsonobject;
    }

    public void deserialize(JsonObject json, boolean flag) {
        JsonObject jsonobject = json.getAsJsonObject("modules");
        if (jsonobject != null) {
            BindRegistry bindregistry = Adapt.getInstance().getBindingManager().getRegistry();
            int i = json.has("version") ? json.get("version").getAsInt() : 0;

            for (Module module : Adapt.getInstance().getModuleManager().getAll()) {
                if (i >= 4 || !(module instanceof ThemeModule)) {
                    JsonElement jsonelement = jsonobject.get(module.getId());
                    if (jsonelement == null) {
                        String s = switch (module.getId()) {
                            case "target_display" -> "target_hud";
                            case "sprint" -> "toggle_sprint";
                            default -> null;
                        };
                        if (s != null) {
                            jsonelement = jsonobject.get(s);
                        }
                    }

                    if (jsonelement == null && module instanceof WorldEditorModule worldeditormodule) {
                        JsonElement jsonelement3 = jsonobject.get("fullbright");
                        if (jsonelement3 != null && jsonelement3.isJsonObject()) {
                            JsonObject jsonobject4 = jsonelement3.getAsJsonObject();
                            if (jsonobject4.has("enabled")) {
                                module.setEnabled(jsonobject4.get("enabled").getAsBoolean());
                            }

                            JsonObject jsonobject1 = jsonobject4.getAsJsonObject("properties");
                            if (jsonobject1 != null && jsonobject1.has("strength")) {
                                worldeditormodule.brightnessStrength.fromJson(jsonobject1.get("strength"));
                            }
                        }
                    }

                    if (jsonelement != null && jsonelement.isJsonObject()) {
                        JsonObject jsonobject2 = jsonelement.getAsJsonObject();
                        JsonObject jsonobject3 = jsonobject2.getAsJsonObject("properties");
                        if (jsonobject3 != null) {
                            for (Property property : module.getProperties()) {
                                if (i >= 5 || !(module instanceof VelocityModule) || !"delay_ticks".equals(property.getKey())) {
                                    JsonElement jsonelement1 = jsonobject3.get(property.getKey());
                                    if (jsonelement1 != null) {
                                        try {
                                            property.fromJson(jsonelement1);
                                        } catch (Exception exception) {
                                        }
                                    } else if (property instanceof MultiBooleanProperty multibooleanproperty) {
                                        String s1 = multibooleanproperty.getKey();
                                        String s2 = (s1.endsWith("s") ? s1.substring(0, s1.length() - 1) : s1) + "_";

                                        for (String s3 : multibooleanproperty.getOptions()) {
                                            JsonElement jsonelement2 = jsonobject3.get(s2 + s3.toLowerCase().replace(' ', '_'));
                                            if (jsonelement2 != null && jsonelement2.isJsonPrimitive()) {
                                                multibooleanproperty.setEnabled(s3, jsonelement2.getAsBoolean());
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (flag && jsonobject2.has("bind")) {
                            JsonObject jsonobject5 = jsonobject2.getAsJsonObject("bind");
                            bindregistry.unbind(module);
                            bindregistry.bind(jsonobject5.get("code").getAsInt(), module, InputType.valueOf(jsonobject5.get("type").getAsString()));
                        }

                        if (module instanceof HudModule hudmodule && jsonobject2.has("x")) {
                            hudmodule.setAnchor(jsonobject2.get("x").getAsFloat(), jsonobject2.get("y").getAsFloat());
                        }

                        if (jsonobject2.has("enabled")) {
                            module.setEnabled(jsonobject2.get("enabled").getAsBoolean());
                        }
                    }
                }
            }
        }
    }

    public static String formatBind(Bind bind) {
        return BindingManager.formatBind(bind);
    }
}
