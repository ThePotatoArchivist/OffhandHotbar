package archives.tater.offhandhotbar.mixin.client;

import archives.tater.offhandhotbar.OffhandHotbar;
import archives.tater.offhandhotbar.OffhandHotbarConfig;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Mouse;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Shadow @Final
    private MinecraftClient client;

    @Inject(
            method = "onMouseScroll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"),
            cancellable = true
    )
    private void scrollInventory(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (client.currentScreen != null) return;
        if (!OffhandHotbar.SCROLL_INVENTORY_KEY.isPressed()) return;
        if (vertical == 0) return;

        OffhandHotbar.scrollInventory(client, vertical > 0);

        ci.cancel();
    }

    @WrapOperation(
            method = "onMouseScroll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;setSelectedSlot(I)V")
    )
    private void scrollOffhand(PlayerInventory instance, int value, Operation<Void> original) {
        if (OffhandHotbarConfig.scrollControls == Hand.OFF_HAND ^ OffhandHotbar.CONTROL_OPPOSITE_KEY.isPressed()) {
            OffhandHotbar.selectedOffhandSlot = value;
            OffhandHotbar.updateOffhandSlots(MinecraftClient.getInstance());
        } else
            original.call(instance, value);
    }

    @ModifyExpressionValue(
            method = "onMouseScroll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getSelectedSlot()I")
    )
    private int scrollOffhand(int original) {
        return (OffhandHotbarConfig.scrollControls == Hand.OFF_HAND ^ OffhandHotbar.CONTROL_OPPOSITE_KEY.isPressed())
                ? OffhandHotbar.selectedOffhandSlot : original;
    }
}
