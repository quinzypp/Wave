package dev.adapt.v3.mixin;

import com.mojang.authlib.minecraft.UserApiService;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.server.Services;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Mutable
    @Accessor("user")
    void adapt$setUser(User user);

    @Mutable
    @Accessor("userApiService")
    void adapt$setUserApiService(UserApiService userApiService);

    @Mutable
    @Accessor("userPropertiesFuture")
    void adapt$setUserPropertiesFuture(CompletableFuture future);

    @Mutable
    @Accessor("profileFuture")
    void adapt$setProfileFuture(CompletableFuture future);

    @Mutable
    @Accessor("profileKeyPairManager")
    void adapt$setProfileKeyPairManager(ProfileKeyPairManager profileKeyPairManager);

    @Accessor("reportingContext")
    void adapt$setReportingContext(ReportingContext reportingContext);

    @Accessor("services")
    Services adapt$services();
}
