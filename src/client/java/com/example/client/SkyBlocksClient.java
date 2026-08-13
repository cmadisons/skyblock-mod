package com.example.client;

import net.fabricmc.api.ClientModInitializer;

/**
 * The client-side half of the mod, run only on the machine that draws the game.
 *
 * Sky Blocks has nothing to do here. The world type is a data file, and the
 * island is built by the server side of the mod -- so there is no rendering or
 * input work to hook up. This entrypoint stays as the place for that if it ever
 * changes, such as a heads-up display showing your coins.
 */
public class SkyBlocksClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Intentionally empty -- see the class comment above.
	}
}
