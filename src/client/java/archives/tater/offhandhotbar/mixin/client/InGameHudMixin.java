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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

import static archives.tater.offhandhotbar.OffhandHotbar.getOffhandHotbarSlot;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
	@Shadow
    protected abstract @Nullable PlayerEntity getCameraPlayer();

	@Shadow public abstract void tick(boolean paused);

	@Shadow protected abstract void tick();

	@Inject(
			method = "renderMainHud",
			at = @At(value = "HEAD")
	)
	private void shiftHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		if (!OffhandHotbarConfig.displayMode.isStacked()) return;
		context.getMatrices().push();
		context.getMatrices().translate(0, OffhandHotbar.HOTBAR_Y_OFFSET, 0);
	}

	@Inject(
			method = "renderMainHud",
			at = @At("TAIL")
	)
	private void unshiftHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		if (!OffhandHotbarConfig.displayMode.isStacked()) return;
		context.getMatrices().pop();
	}

	@ModifyVariable(
			method = "renderExperienceLevel",
			ordinal = 2,
			at = @At(value = "STORE")
	)
	private int shiftXpLevel(int value) {
		return OffhandHotbarConfig.displayMode.isStacked() ? value + OffhandHotbar.HOTBAR_Y_OFFSET : value;
	}

	@Unique
	private void offhandhotbar$hotbarRotate(DrawContext context, boolean leftSide) {
		context.getMatrices().translate(
				leftSide
						? context.getScaledWindowHeight()
						: context.getScaledWindowHeight() + context.getScaledWindowWidth() - OffhandHotbar.HOTBAR_HEIGHT,
				-(context.getScaledWindowWidth() - context.getScaledWindowHeight()) / 2f,
				0
		);
		context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90));
	}

	@WrapMethod(
			method = "renderHotbar"
	)
	private void shiftHotbar(DrawContext context, RenderTickCounter tickCounter, Operation<Void> original, @Share("offhand") LocalBooleanRef offhand) {
		var cameraPlayer = getCameraPlayer();
		var mainArm = cameraPlayer == null ? Arm.RIGHT : cameraPlayer.getMainArm();
		var mx = mainArm == Arm.RIGHT ? 1 : -1;
		var matrices = context.getMatrices();

		matrices.push();
		switch (OffhandHotbarConfig.displayMode) {
			case SIDE_BY_SIDE -> matrices.translate(mx * OffhandHotbar.HOTBAR_X_OFFSET, 0, 0);
			case STACKED -> {
				matrices.push();
				matrices.translate(0, -OffhandHotbar.HOTBAR_Y_OFFSET, 0);
			}
			case VERTICAL_SWAPPED -> {
				matrices.push();
				offhandhotbar$hotbarRotate(context, mainArm == Arm.LEFT);
			}
		}
		offhand.set(false);
		original.call(context, tickCounter);
		switch (OffhandHotbarConfig.displayMode) {
			case SIDE_BY_SIDE -> matrices.translate(-mx * OffhandHotbar.HOTBAR_X_OFFSET * 2, 0, 0);
			case STACKED, VERTICAL_SWAPPED -> matrices.pop();
			case STACKED_SWAPPED -> matrices.translate(0, -OffhandHotbar.HOTBAR_Y_OFFSET, 0);
			case VERTICAL -> {
				offhandhotbar$hotbarRotate(context, mainArm == Arm.RIGHT);
			}
        }
		offhand.set(true);
		original.call(context, tickCounter);
		matrices.pop();
	}

	@WrapOperation(
			method = "renderHotbar",
			slice = @Slice(
					from = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/InGameHud;HOTBAR_SELECTION_TEXTURE:Lnet/minecraft/util/Identifier;")
			),
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIII)V", ordinal = 0)
	)
	private void fixSelectionBottomBorder(DrawContext instance, Function<Identifier, RenderLayer> renderLayers, Identifier sprite, int x, int y, int width, int height, Operation<Void> original) {
		original.call(instance, renderLayers, sprite, x, y, width, height);
		instance.drawGuiTexture(renderLayers, sprite, 24, 23, 0, 0, x, y + height, width, 1);
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
		context.getMatrices().push();
		context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90), x + 8, y + 8, 0);
		original.call(instance, context, x, y, tickCounter, player, stack, seed);
		context.getMatrices().pop();
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