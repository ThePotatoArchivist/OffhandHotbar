package archives.tater.offhandhotbar.mixin.client.combatamenities;

import archives.tater.offhandhotbar.OffhandHotbarConfig;
import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InGameHud.class, priority = 1500)
@Debug(export = true)
public abstract class HudRendererMixinMixin {
    @Shadow protected abstract @Nullable PlayerEntity getCameraPlayer();

    @Unique
    private boolean offhand = false;

    @Inject(
            method = "renderHotbar",
            at = @At("HEAD")
    )
    private void saveOffhand(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci, @Share(value = "offhand", namespace = "archives.tater.offhandhotbar.mixin.client.InGameHudMixin") LocalBooleanRef offhand) {
        this.offhand = offhand.get();
    }

    @TargetHandler(
            mixin = "net.hollowed.combatamenities.mixin.slots.rendering.HudRendererMixin",
            name = "renderHotbar"
    )
    @Inject(
            method = "@MixinSquared:Handler",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cancelSlotsRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci0, CallbackInfo ci) {
        if (offhand ^ (OffhandHotbarConfig.displayMode.isSwapped()))
            ci.cancel();
    }
}
