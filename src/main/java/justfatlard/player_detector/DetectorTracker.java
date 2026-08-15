package justfatlard.player_detector;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Player-driven detector updates.
 *
 * The block's own scheduled tick only runs while its chunk is simulation-ticking, which is
 * usually true only because the detected player is nearby. When that player leaves in a way
 * that also removes the chunk's ticket (teleport far away, disconnect, dimension change,
 * death), the chunk stops ticking before the next scheduled check fires and the detector is
 * saved stuck in the powered state.
 *
 * This tracker diffs, once per server tick, which detector each player is standing on. Any
 * transition (stepped on, stepped off, teleported, died, disconnected) refreshes the affected
 * detector on that same tick, while the chunk the player just left is still loaded. Writing a
 * block state only needs the chunk loaded, not ticking, so the power-down always lands.
 */
public final class DetectorTracker {
	private static final Map<UUID, GlobalPos> STANDING_ON = new HashMap<>();

	private DetectorTracker() {}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(DetectorTracker::onEndTick);
	}

	private static void onEndTick(MinecraftServer server) {
		Map<UUID, GlobalPos> current = new HashMap<>();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			GlobalPos pos = detectorUnder(player);
			if (pos != null) current.put(player.getUUID(), pos);
		}

		// Players that left a detector: moved off, teleported, died, or disconnected
		for (Map.Entry<UUID, GlobalPos> entry : STANDING_ON.entrySet()) {
			if (!entry.getValue().equals(current.get(entry.getKey()))) {
				refresh(server, entry.getValue());
			}
		}

		// Players that arrived on a detector
		for (Map.Entry<UUID, GlobalPos> entry : current.entrySet()) {
			if (!entry.getValue().equals(STANDING_ON.get(entry.getKey()))) {
				refresh(server, entry.getValue());
			}
		}

		STANDING_ON.clear();
		STANDING_ON.putAll(current);
	}

	private static GlobalPos detectorUnder(ServerPlayer player) {
		ServerLevel world = player.level();
		BlockPos feet = player.blockPosition();
		for (BlockPos candidate : new BlockPos[]{feet, feet.below()}) {
			BlockState state = world.getBlockState(candidate);
			if (state.getBlock() == Main.PLAYER_DETECTOR_BLOCK
					&& Main.PLAYER_DETECTOR_BLOCK.isStandingOn(player, candidate)) {
				return GlobalPos.of(world.dimension(), candidate);
			}
		}
		return null;
	}

	private static void refresh(MinecraftServer server, GlobalPos pos) {
		ServerLevel world = server.getLevel(pos.dimension());
		if (world != null) {
			Main.PLAYER_DETECTOR_BLOCK.refresh(world, pos.pos());
		}
	}
}
