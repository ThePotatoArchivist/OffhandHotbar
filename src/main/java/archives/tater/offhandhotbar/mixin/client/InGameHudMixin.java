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
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
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

	@ModifyArg(
			method = "renderHotbar",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getStack(I)Lnet/minecraft/item/ItemStack;")
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
			at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getSelectedSlot()I")
	)
	private int useOffhandSlot(int original, @Share("offhand") LocalBooleanRef offhand) {
		return offhand.get() ? OffhandHotbar.selectedOffhandSlot : original;
	}
}