package archives.tater.offhandhotbar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OffhandHotbar implements ClientModInitializer {
	public static int selectedOffhandSlot = 0;
	private static int lastOffhandSlot = selectedOffhandSlot;
	public static boolean swapped = false;

	public static final int OFFHAND_SWAP_ID = 40;
	public static final int SLOTS_OFFSET = 18;

	public static final float HOTBAR_WIDTH = 91;
	public static final float HOTBAR_GAP = 4;
	public static final float HOTBAR_OFFSET = HOTBAR_WIDTH + HOTBAR_GAP / 2;

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
		interactionManager.clickSlot(player.currentScreenHandler.syncId, slot, OFFHAND_SWAP_ID, SlotActionType.SWAP, player);
	}

	public static @Nullable List<Slot> getScreenSlots(MinecraftClient client) {
		if (client.currentScreen == null)
			return null;

		if (client.player == null) return null;
		if (client.player.currentScreenHandler == null) return null;
		if (client.player.currentScreenHandler instanceof PlayerScreenHandler) return null;
		return client.player.currentScreenHandler.slots;
	}

	public static int findOffhandSlot(List<Slot> slots, int slotIndex, ClientPlayerEntity player) {
		for (var slot : slots)
			if (slot.inventory == player.getInventory() && slot.getIndex() == slotIndex)
				return slot.id;
		return -1;
	}

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (selectedOffhandSlot != lastOffhandSlot) {
				offhandCycle(client,
						getOffhandHotbarSlot(lastOffhandSlot),
						getOffhandHotbarSlot(selectedOffhandSlot));
				lastOffhandSlot = selectedOffhandSlot;
			}
            if (client.player == null) return;

			var slots = getScreenSlots(client);

            if (slots == null) {
                if (!swapped) {
                    swapOffhand(client, getOffhandHotbarSlot(selectedOffhandSlot));
                    swapped = true;
                }
            } else {
                if (swapped) {
                    swapOffhand(client, findOffhandSlot(slots, getOffhandHotbarSlot(selectedOffhandSlot), client.player));
                    swapped = false;
                }
            }
        });
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			swapOffhand(client, getOffhandHotbarSlot(selectedOffhandSlot));
			swapped = false;
		});
	}
}