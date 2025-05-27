package archives.tater.offhandhotbar;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import static net.minecraft.util.Util.createTranslationKey;

public class OffhandHotbar implements ModInitializer, ClientModInitializer {
	public static final String MOD_ID = "offhandhotbar";

	public static int selectedOffhandSlot = 0;
	private static int lastOffhandSlot = selectedOffhandSlot;
	public static boolean attemptedSwap = false;
	public static boolean swapped = false;

	public static final int OFFHAND_SWAP_ID = 40;
	public static final int SLOTS_OFFSET = 18;

	public static final int HOTBAR_WIDTH = 91;
	public static final int HOTBAR_GAP = 4;
	public static final int HOTBAR_X_OFFSET = HOTBAR_WIDTH + HOTBAR_GAP / 2;
	public static final int HOTBAR_HEIGHT = 22;
	public static final int HOTBAR_Y_OFFSET = -(HOTBAR_HEIGHT + HOTBAR_GAP);

	public static final KeyBinding CONTROL_OPPOSITE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			createTranslationKey("key", Identifier.of(MOD_ID, "control_opposite")),
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_LEFT_ALT,
			createTranslationKey("category", Identifier.of(MOD_ID, "offhandhotbar"))
	));

	public static int getOffhandHotbarSlot(int selectedSlot) {
		return PlayerScreenHandler.INVENTORY_START + SLOTS_OFFSET + selectedSlot;
	}

	public static void offhandCycle(MinecraftClient client, int slot1, int slot2) {
		var interactionManager = client.interactionManager;
		if (interactionManager == null) return;
		var player = client.player;
		if (player == null) return;
		offhandCycle(interactionManager, player, slot1, slot2);
	}

	public static void offhandCycle(ClientPlayerInteractionManager interactionManager, ClientPlayerEntity player, int slot1, int slot2) {
		interactionManager.clickSlot(player.playerScreenHandler.syncId, slot1, OFFHAND_SWAP_ID, SlotActionType.SWAP, player);
		interactionManager.clickSlot(player.playerScreenHandler.syncId, slot2, OFFHAND_SWAP_ID, SlotActionType.SWAP, player);
	}

	public static void swapOffhand(MinecraftClient client, int slot) {
		var interactionManager = client.interactionManager;
		if (interactionManager == null) return;
		var player = client.player;
		if (player == null) return;
		swapOffhand(interactionManager, player, slot);
	}

	public static void swapOffhand(ClientPlayerInteractionManager interactionManager, ClientPlayerEntity player, int slot) {
		interactionManager.clickSlot(player.playerScreenHandler.syncId, slot, OFFHAND_SWAP_ID, SlotActionType.SWAP, player);
	}

	public static void updateOffhandSlots(MinecraftClient client) {
        if (selectedOffhandSlot == lastOffhandSlot) return;
        offhandCycle(client,
                getOffhandHotbarSlot(lastOffhandSlot),
                getOffhandHotbarSlot(selectedOffhandSlot));
        lastOffhandSlot = selectedOffhandSlot;
    }

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			updateOffhandSlots(client);

            if (client.player == null) return;

            if (!(client.currentScreen instanceof HandledScreen<?>) || client.player.currentScreenHandler == null) {
                if (!swapped) {
                    swapOffhand(client, getOffhandHotbarSlot(selectedOffhandSlot));
                    swapped = true;
                }
            } else {
                if (swapped) {
                    swapOffhand(client, getOffhandHotbarSlot(selectedOffhandSlot));
                    swapped = false;
                }
            }
        });
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			swapOffhand(client, getOffhandHotbarSlot(selectedOffhandSlot));
			swapped = false;
		});
	}

	@Override
	public void onInitialize() {
		MidnightConfig.init(MOD_ID, OffhandHotbarConfig.class);
	}
}