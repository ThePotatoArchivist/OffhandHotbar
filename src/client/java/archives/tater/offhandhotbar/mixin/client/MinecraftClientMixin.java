package archives.tater.offhandhotbar.mixin.client;

import archives.tater.offhandhotbar.OffhandHotbar;
import archives.tater.offhandhotbar.OffhandHotbarConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @WrapOperation(
            method = "handleInputEvents",
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerInventory;selectedSlot:I")
    )
    private void setOffhandSlot(PlayerInventory instance, int value, Operation<Void> original) {
        if (OffhandHotbarConfig.keyboardControls == Hand.OFF_HAND ^ OffhandHotbar.CONTROL_OPPOSITE_KEY.isPressed())
            OffhandHotbar.selectedOffhandSlot = value;
        else
            original.call(instance, value);
    }
}
