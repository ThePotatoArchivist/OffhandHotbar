package archives.tater.offhandhotbar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public class OffhandHotbar implements ClientModInitializer {
	public static int selectedOffhandSlot = 0;
	private static int lastOffhandSlot = selectedOffhandSlot;
	public static boolean swapped = false;

	public static final float HOTBAR_WIDTH = 91;
	public static final float HOTBAR_GAP = 4;
	public static final float HOTBAR_OFFSET = HOTBAR_WIDTH + HOTBAR_GAP / 2;

	public static int getOffhandHotbarSlot(int selectedSlot) {
		return selectedSlot + 27;
	}

	public static void cycle(MinecraftClient client, int slot1, int slot2, int slot3) {
		var interactionManager = client.interactionManager;
		if (interactionManager == null) return;
		var player = client.player;
		if (player == null) return;
		cycle(interactionManager, player, slot1, slot2, slot3);
	}

	public static void cycle(ClientPlayerInteractionManager interactionManager, ClientPlayerEntity player, int slot1, int slot2, int slot3) {
		interactionManager.clickSlot(player.playerScreenHandler.syncId, slot1, 0, SlotActionType.PICKUP, player);
		interactionManager.clickSlot(player.playerScreenHandler.syncId, slot2, 0, SlotActionType.PICKUP, player);
		interactionManager.clickSlot(player.playerScreenHandler.syncId, slot3, 0, SlotActionType.PICKUP, player);
		interactionManager.clickSlot(player.playerScreenHandler.syncId, slot1, 0, SlotActionType.PICKUP, player);
	}

	public static void swap(MinecraftClient client, int slot1, int slot2) {
		var interactionManager = client.interactionManager;
		if (interactionManager == null) return;
		var player = client.player;
		if (player == null) return;
		swap(interactionManager, player, slot1, slot2);
	}

	public static void swap(ClientPlayerInteractionManager interactionManager, ClientPlayerEntity player, int slot1, int slot2) {
		interactionManager.clickSlot(player.playerScreenHandler.syncId, slot1, 0, SlotActionType.PICKUP, player);
		interactionManager.clickSlot(player.playerScreenHandler.syncId, slot2, 0, SlotActionType.PICKUP, player);
		interactionManager.clickSlot(player.playerScreenHandler.syncId, slot1, 0, SlotActionType.PICKUP, player);
	}

	public static void onOpenScreen(MinecraftClient client) {
		swap(client, PlayerScreenHandler.INVENTORY_START + 18 + selectedOffhandSlot, PlayerScreenHandler.OFFHAND_ID);
		swapped = false;
	}

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (selectedOffhandSlot != lastOffhandSlot) {
				cycle(client,
						PlayerScreenHandler.INVENTORY_START + 18 + selectedOffhandSlot,
						PlayerScreenHandler.OFFHAND_ID,
						PlayerScreenHandler.INVENTORY_START + 18 + lastOffhandSlot);
				lastOffhandSlot = selectedOffhandSlot;
			} else if (!swapped && client.player != null && (client.player.currentScreenHandler == null || client.player.currentScreenHandler instanceof PlayerScreenHandler)) {
				// TODO: this gets called before currentScreeHandler is set for some reason??? Threading possibly?
				swap(client, PlayerScreenHandler.INVENTORY_START + 18 + selectedOffhandSlot, PlayerScreenHandler.OFFHAND_ID);
				swapped = true;
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			swap(client, PlayerScreenHandler.INVENTORY_START + 18 + selectedOffhandSlot, PlayerScreenHandler.OFFHAND_ID);
			swapped = false;
		});
	}
}