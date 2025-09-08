package archives.tater.offhandhotbar.mixin.client;

import archives.tater.offhandhotbar.OffhandHotbar;
import archives.tater.offhandhotbar.OffhandHotbarConfig;
import archives.tater.offhandhotbar.OffhandHotbarConfig.DisplayMode;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static archives.tater.offhandhotbar.OffhandHotbar.getOffhandHotbarSlot;
import static net.minecraft.util.math.MathHelper.HALF_PI;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
	@Shadow
    protected abstract @Nullable PlayerEntity getCameraPlayer();

	@Inject(
			method = "renderMainHud",
			at = @At(value = "HEAD")
	)
	private void shiftHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		if (!OffhandHotbarConfig.displayMode.isStacked()) return;
		context.getMatrices().pushMatrix();
		context.getMatrices().translate(0, OffhandHotbar.HOTBAR_Y_OFFSET);
	}

	@Inject(
			method = "renderMainHud",
			at = @At("TAIL")
	)
	private void unshiftHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		if (!OffhandHotbarConfig.displayMode.isStacked()) return;
		context.getMatrices().popMatrix();
	}

	@Unique
	private void offhandhotbar$hotbarRotate(DrawContext context, boolean leftSide) {
		context.getMatrices().translate(
				leftSide
						? context.getScaledWindowHeight()
						: context.getScaledWindowHeight() + context.getScaledWindowWidth() - OffhandHotbar.HOTBAR_HEIGHT,
				-(context.getScaledWindowWidth() - context.getScaledWindowHeight()) / 2f
		);
		context.getMatrices().rotate(HALF_PI);
	}

	@WrapMethod(
			method = "renderHotbar"
	)
	private void shiftHotbar(DrawContext context, RenderTickCounter tickCounter, Operation<Void> original, @Share("offhand") LocalBooleanRef offhand) {
		var cameraPlayer = getCameraPlayer();
		var mainArm = cameraPlayer == null ? Arm.RIGHT : cameraPlayer.getMainArm();
		var mx = mainArm == Arm.RIGHT ? 1 : -1;
		var matrices = context.getMatrices();

		matrices.pushMatrix();
		switch (OffhandHotbarConfig.displayMode) {
			case SIDE_BY_SIDE -> matrices.translate(mx * OffhandHotbar.HOTBAR_X_OFFSET, 0);
			case STACKED -> {
				matrices.pushMatrix();
				matrices.translate(0, -OffhandHotbar.HOTBAR_Y_OFFSET);
			}
			case VERTICAL_SWAPPED -> {
				matrices.pushMatrix();
				offhandhotbar$hotbarRotate(context, mainArm == Arm.LEFT);
			}
		}
		offhand.set(false);
		original.call(context, tickCounter);
		switch (OffhandHotbarConfig.displayMode) {
			case SIDE_BY_SIDE -> matrices.translate(-mx * OffhandHotbar.HOTBAR_X_OFFSET * 2, 0);
			case STACKED, VERTICAL_SWAPPED -> matrices.popMatrix();
			case STACKED_SWAPPED -> matrices.translate(0, -OffhandHotbar.HOTBAR_Y_OFFSET);
			case VERTICAL -> {
				offhandhotbar$hotbarRotate(context, mainArm == Arm.RIGHT);
			}
        }
		offhand.set(true);
		original.call(context, tickCounter);
		matrices.popMatrix();
	}

	@WrapOperation(
			method = "renderHotbar",
			slice = @Slice(
					from = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/InGameHud;HOTBAR_SELECTION_TEXTURE:Lnet/minecraft/util/Identifier;")
			),
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V", ordinal = 0)
	)
	private void fixSelectionBottomBorder(DrawContext instance, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, Operation<Void> original) {
		original.call(instance, pipeline, sprite, x, y, width, height);
		instance.drawGuiTexture(pipeline, sprite, 24, 23, 0, 0, x, y + height, width, 1);
	}

	@WrapOperation(
			method = "renderHotbar",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHotbarItem(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;I)V")
	)
	private void rotateHotbarItem(InGameHud instance, DrawContext context, int x, int y, RenderTickCounter tickCounter, PlayerEntity player, ItemStack stack, int seed, Operation<Void> original, @Share("offhand") LocalBooleanRef offhand) {
		if (OffhandHotbarConfig.displayMode != (offhand.get() ? DisplayMode.VERTICAL : DisplayMode.VERTICAL_SWAPPED)) {
			original.call(instance, context, x, y, tickCounter, player, stack, seed);
			return;
		}
		context.getMatrices().pushMatrix();
		context.getMatrices().rotateAbout(-HALF_PI, x + 8, y + 8);
		original.call(instance, context, x, y, tickCounter, player, stack, seed);
		context.getMatrices().popMatrix();
	}

    @SuppressWarnings({"LocalMayBeArgsOnly"}) // Incorrect
    @WrapOperation(
            method = "renderHotbar",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getStack(I)Lnet/minecraft/item/ItemStack;")
    )
    private ItemStack modifyDisplayedItem(PlayerInventory instance, int index, Operation<ItemStack> original, @Local ItemStack offhandStack, @Local PlayerEntity player, @Share("offhand") LocalBooleanRef offhand) {
        if (OffhandHotbar.focusSwapped)
            if (offhand.get()) {
                if (index == OffhandHotbar.selectedOffhandSlot)
                    return original.call(instance, player.getInventory().getSelectedSlot());
            } else if (index == player.getInventory().getSelectedSlot())
                return offhandStack;
        if (!offhand.get())
            return original.call(instance, index);
        if (OffhandHotbar.swapped && index == OffhandHotbar.selectedOffhandSlot)
            return offhandStack;
        return original.call(instance, getOffhandHotbarSlot(index));
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
			at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getSelectedSlot()I")
	)
	private int useOffhandSlot(int original, @Share("offhand") LocalBooleanRef offhand) {
		return offhand.get() ? OffhandHotbar.selectedOffhandSlot : original;
	}

	@Inject(
			method = "renderHotbar",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z", ordinal = 1),
			cancellable = true
	)
	// Prevents other mixins that inject at TAIL from rendering twice
	private void earlyReturn(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci, @Share("offhand") LocalBooleanRef offhand) {
		if (offhand.get() ^ OffhandHotbarConfig.displayMode.isSwapped())
			ci.cancel();
	}
}