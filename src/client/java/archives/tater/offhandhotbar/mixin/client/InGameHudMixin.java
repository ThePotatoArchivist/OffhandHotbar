package archives.tater.offhandhotbar.mixin.client;

import archives.tater.offhandhotbar.OffhandHotbar;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static archives.tater.offhandhotbar.OffhandHotbar.getOffhandHotbarSlot;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
	@Shadow
    protected abstract @Nullable PlayerEntity getCameraPlayer();

	@WrapMethod(
			method = "renderHotbar"
	)
	private void shiftHotbar(DrawContext context, RenderTickCounter tickCounter, Operation<Void> original, @Share("offhand") LocalBooleanRef offhand) {
		var cameraPlayer = getCameraPlayer();
		var mx = cameraPlayer == null ? 0 : cameraPlayer.getMainArm() == Arm.RIGHT ? 1 : -1;
		context.getMatrices().push();
		context.getMatrices().translate(mx * OffhandHotbar.HOTBAR_OFFSET, 0, 0);
		offhand.set(false);
		original.call(context, tickCounter);
		context.getMatrices().translate(-mx * OffhandHotbar.HOTBAR_OFFSET * 2, 0, 0);
		offhand.set(true);
		original.call(context, tickCounter);
		context.getMatrices().pop();
	}

	@ModifyArg(
			method = "renderHotbar",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/util/collection/DefaultedList;get(I)Ljava/lang/Object;")
	)
	private int useRow3Items(int index, @Share("offhand") LocalBooleanRef offhand) {
		return offhand.get() ? getOffhandHotbarSlot(index) : index;
	}

	@ModifyArg(
			method = "renderHotbar",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHotbarItem(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;I)V", ordinal = 0),
			index = 5
	)
	private ItemStack useOffhandItem(ItemStack stack, @Share("offhand") LocalBooleanRef offhand, @Local(ordinal = 4) int index, @Local ItemStack offhandStack) {
		return offhand.get() && OffhandHotbar.swapped && index == OffhandHotbar.selectedOffhandSlot ? offhandStack : stack;
	}

	@ModifyExpressionValue(
			method = "renderHotbar",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z")
	)
	private boolean hideVanillaOffhand(boolean original) {
		return true;
	}

	@ModifyExpressionValue(
			method = "renderHotbar",
			at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerInventory;selectedSlot:I")
	)
	private int useOffhandSlot(int original, @Share("offhand") LocalBooleanRef offhand) {
		return offhand.get() ? OffhandHotbar.selectedOffhandSlot : original;
	}
}