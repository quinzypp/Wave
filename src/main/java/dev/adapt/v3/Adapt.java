package dev.adapt.v3;

import dev.adapt.v3.account.AccountManager;
import dev.adapt.v3.binding.BindingManager;
import dev.adapt.v3.command.CommandManager;
import dev.adapt.v3.config.ConfigManager;
import dev.adapt.v3.event.EventManager;
import dev.adapt.v3.event.impl.JoinWorldEvent;
import dev.adapt.v3.event.impl.KeyEvent;
import dev.adapt.v3.event.impl.PostTickEvent;
import dev.adapt.v3.event.system.Listener;
import dev.adapt.v3.event.system.Subscribe;
import dev.adapt.v3.feature.helper.movement.MovementHelper;
import dev.adapt.v3.feature.helper.rotation.MovementFixHelper;
import dev.adapt.v3.feature.helper.rotation.RotationHelper;
import dev.adapt.v3.feature.helper.target.TargetHelper;
import dev.adapt.v3.feature.module.Module;
import dev.adapt.v3.feature.module.impl.client.NotificationsModule;
import dev.adapt.v3.feature.module.impl.client.ThemeModule;
import dev.adapt.v3.feature.module.impl.combat.AutoPotionModule;
import dev.adapt.v3.feature.module.impl.combat.AutoProjectileModule;
import dev.adapt.v3.feature.module.impl.combat.BacktrackModule;
import dev.adapt.v3.feature.module.impl.combat.BlockModule;
import dev.adapt.v3.feature.module.impl.combat.CriticalsModule;
import dev.adapt.v3.feature.module.impl.combat.PiercingModule;
import dev.adapt.v3.feature.module.impl.combat.ReachModule;
import dev.adapt.v3.feature.module.impl.combat.keepsprint.KeepSprintModule;
import dev.adapt.v3.feature.module.impl.combat.killaura.KillAuraModule;
import dev.adapt.v3.feature.module.impl.combat.velocity.VelocityModule;
import dev.adapt.v3.feature.module.impl.hud.ArmorStatusHud;
import dev.adapt.v3.feature.module.impl.hud.ModuleListHud;
import dev.adapt.v3.feature.module.impl.hud.SpeedometerHud;
import dev.adapt.v3.feature.module.impl.hud.TabGuiHud;
import dev.adapt.v3.feature.module.impl.hud.TargetDisplayHud;
import dev.adapt.v3.feature.module.impl.hud.WatermarkHud;
import dev.adapt.v3.feature.module.impl.movement.AntiVoidModule;
import dev.adapt.v3.feature.module.impl.movement.LongJumpModule;
import dev.adapt.v3.feature.module.impl.movement.MovementFixModule;
import dev.adapt.v3.feature.module.impl.movement.PhaseModule;
import dev.adapt.v3.feature.module.impl.movement.StepModule;
import dev.adapt.v3.feature.module.impl.movement.TargetStrafeModule;
import dev.adapt.v3.feature.module.impl.movement.flight.FlightModule;
import dev.adapt.v3.feature.module.impl.movement.invmove.InvMoveModule;
import dev.adapt.v3.feature.module.impl.movement.noslow.NoSlowModule;
import dev.adapt.v3.feature.module.impl.movement.speed.SpeedModule;
import dev.adapt.v3.feature.module.impl.utility.ChestLooterModule;
import dev.adapt.v3.feature.module.impl.utility.InvManagerModule;
import dev.adapt.v3.feature.module.impl.utility.SprintModule;
import dev.adapt.v3.feature.module.impl.utility.disabler.DisablerModule;
import dev.adapt.v3.feature.module.impl.utility.nofall.NoFallModule;
import dev.adapt.v3.feature.module.impl.visual.AnimationsModule;
import dev.adapt.v3.feature.module.impl.visual.EspModule;
import dev.adapt.v3.feature.module.impl.visual.NametagsModule;
import dev.adapt.v3.feature.module.impl.visual.NoSwingSoundModule;
import dev.adapt.v3.feature.module.impl.visual.PerspectiveModule;
import dev.adapt.v3.feature.module.impl.visual.ZoomModule;
import dev.adapt.v3.feature.module.impl.world.LightningDetectorModule;
import dev.adapt.v3.feature.module.impl.world.WorldEditorModule;
import dev.adapt.v3.feature.module.impl.world.scaffold.ScaffoldModule;
import dev.adapt.v3.feature.module.registry.ModuleRegistry;
import dev.adapt.v3.hud.HudManager;
import dev.adapt.v3.notification.NotificationManager;
// VENDOR-DISABLED: import dev.adapt.v3.security.LaunchGuard;
import dev.adapt.v3.ui.screen.DashboardScreen;
import dev.adapt.v3.ui.screen.HudEditorScreen;
import dev.adapt.v3.util.RandomUtility;
import dev.adapt.v3.util.SpeedTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Adapt implements Listener {
    public static final String NAME = "AdaptV3";
    public static final String VERSION = "3.0.4";
    public static final Logger LOGGER = LoggerFactory.getLogger("AdaptV3");
    public static Adapt instance;
    public final NotificationManager notificationManager = new NotificationManager();
    public final BindingManager bindingManager = new BindingManager();
    public final SpeedTracker speedTracker = new SpeedTracker();
    public final ModuleRegistry moduleManager;
    public final HudManager hudManager = new HudManager();
    public final ConfigManager configManager = new ConfigManager();
    public final CommandManager commandManager = new CommandManager();
    public AccountManager accountManager;
    public static final int DASHBOARD_KEY = 229;
    public boolean openDashboardPending;

    public Adapt() {
        instance = this;
        this.moduleManager = ModuleRegistry.of(
            new WatermarkHud(),
            new TabGuiHud(),
            new TargetDisplayHud(),
            new SpeedometerHud(),
            new ArmorStatusHud(),
            new ModuleListHud(),
            new KillAuraModule(),
            new BlockModule(),
            new VelocityModule(),
            new MovementFixModule(),
            new SpeedModule(),
            new TargetStrafeModule(),
            new NoSlowModule(),
            new InvMoveModule(),
            new FlightModule(),
            new PhaseModule(),
            new AntiVoidModule(),
            new LongJumpModule(),
            new CriticalsModule(),
            new KeepSprintModule(),
            new ReachModule(),
            new PiercingModule(),
            new StepModule(),
            new AutoPotionModule(),
            new AutoProjectileModule(),
            new BacktrackModule(),
            new InvManagerModule(),
            new ChestLooterModule(),
            new DisablerModule(),
            new NoFallModule(),
            new ScaffoldModule(),
            new LightningDetectorModule(),
            new ZoomModule(),
            new WorldEditorModule(),
            new AnimationsModule(),
            new NametagsModule(),
            new EspModule(),
            new PerspectiveModule(),
            new NoSwingSoundModule(),
            new SprintModule(),
            new ThemeModule(),
            new NotificationsModule()
        );
        EventManager.register(this);
        RotationHelper.init();
        MovementFixHelper.init();
        MovementHelper.init();
        TargetHelper.getInstance();
    }

    @Subscribe
    public void onJoinWorld(JoinWorldEvent event) {
        RandomUtility.updateTickFactor();
    }

    public static void init() {
        new Adapt();
        instance.configManager.load();
        LOGGER.info("{} {} initialised with {} modules", "AdaptV3", "3.0.4", instance.moduleManager.getAll().size());
    }

    public static Adapt getInstance() {
        return instance;
    }

    public NotificationManager getNotificationManager() {
        return this.notificationManager;
    }

    public BindingManager getBindingManager() {
        return this.bindingManager;
    }

    public ModuleRegistry getModuleManager() {
        return this.moduleManager;
    }

    public HudManager getHudManager() {
        return this.hudManager;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public CommandManager getCommandManager() {
        return this.commandManager;
    }

    public SpeedTracker getSpeedTracker() {
        return this.speedTracker;
    }

    public AccountManager getAccountManager() {
        if (this.accountManager == null) {
            this.accountManager = new AccountManager();
        }

        return this.accountManager;
    }

    public void notifyModuleToggle(Module module) {
        NotificationsModule notificationsmodule = this.moduleManager.getByClass(NotificationsModule.class);
        if (notificationsmodule != null && notificationsmodule.isEnabled() && notificationsmodule.moduleToggles.getValue()) {
            this.notificationManager
                .builder(module.getNotificationType())
                .title(module.getName())
                .message(module.isEnabled() ? "Enabled" : "Disabled")
                .duration(1400)
                .post();
        }
    }

    public static Minecraft mc() {
        return Minecraft.getInstance();
    }

    public void openDashboard() {
        // VENDOR-DISABLED: if (!LaunchGuard.isAuthorised()) { LaunchGuard.notifySessionEnded(); return; }
        mc().gui.setScreen(new DashboardScreen(mc().gui.screen()));
    }

    public void openHudEditor() {
        // VENDOR-DISABLED: if (!LaunchGuard.isAuthorised()) { LaunchGuard.notifySessionEnded(); return; }
        Screen screen = mc().gui.screen();
        mc().gui.setScreen(new HudEditorScreen(screen));
    }

    @Subscribe
    public void onKey(KeyEvent event) {
        if (event.getCode() == 229 && mc().gui.screen() == null && mc().level != null) {
            this.openDashboardPending = true;
        }
    }

    @Subscribe
    public void onPostTick(PostTickEvent event) {
        if (this.openDashboardPending) {
            this.openDashboardPending = false;
            if (mc().gui.screen() == null) {
                this.openDashboard();
            }
        }
    }
}
