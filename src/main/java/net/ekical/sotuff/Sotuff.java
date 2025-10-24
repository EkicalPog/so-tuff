package net.ekical.sotuff;

import net.ekical.sotuff.network.NetworkHandler;
import net.ekical.sotuff.server.FreezeServerControl;
import net.ekical.sotuff.utils.GeneratedSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import static net.ekical.sotuff.SoTuffConstants.MOD_ID;

public final class Sotuff implements ModInitializer {

	@Override
	public void onInitialize() {
		GeneratedSounds.registerDefaults();
		NetworkHandler.registerPayloadTypes();
		NetworkHandler.registerServerReceivers();
		NetworkHandler.registerServerEventTriggers();
		ServerTickEvents.END_SERVER_TICK.register(server -> FreezeServerControl.isActive());
	}
}
