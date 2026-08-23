package justfatlard.player_detector;

import justfatlard.pandorical.api.BlockRegistration;
import justfatlard.pandorical.api.ItemRegistration;
import justfatlard.pandorical.api.PandoricalApi;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements ModInitializer {
	public static final String MOD_ID = "player-detector-justfatlard";
	private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Identifier PLAYER_DETECTOR_ID = Identifier.fromNamespaceAndPath(MOD_ID, "player_detector");

	public static final PlayerDetector PLAYER_DETECTOR_BLOCK = new PlayerDetector(
		BlockBehaviour.Properties.of()
			.strength(2.0f, 3.0f)
			.sound(SoundType.STONE)
			.setId(ResourceKey.create(Registries.BLOCK, PLAYER_DETECTOR_ID))
	);

	@Override
	public void onInitialize() {
		if (PandoricalApi.isAvailable()) {
			PandoricalApi.content().registerBlock(MOD_ID + ":player_detector", new BlockRegistration()
				.baseBlock("minecraft:bricks")
				.model(MOD_ID + ":block/player_detector"));
			PandoricalApi.content().registerItem(MOD_ID + ":player_detector", new ItemRegistration()
				.model(MOD_ID + ":item/player_detector"));
			PandoricalApi.content().registerModAssets(MOD_ID);
		}

		Registry.register(BuiltInRegistries.BLOCK, PLAYER_DETECTOR_ID, PLAYER_DETECTOR_BLOCK);

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, PLAYER_DETECTOR_ID);
		Item playerDetectorItem = new PlayerDetectorItem(
			PLAYER_DETECTOR_BLOCK,
			new Item.Properties().useBlockDescriptionPrefix().setId(itemKey)
		);
		Registry.register(BuiltInRegistries.ITEM, PLAYER_DETECTOR_ID, playerDetectorItem);

		ResourceKey<CreativeModeTab> tabKey = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MOD_ID, "player_detector"));
		CreativeModeTab group = FabricCreativeModeTab.builder()
			.title(Component.literal("Player Detector"))
			.icon(() -> new ItemStack(playerDetectorItem))
			.displayItems((context, entries) -> {
				entries.accept(new ItemStack(playerDetectorItem));
			})
			.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, tabKey, group);

		// Guarantees the powered-to-unpowered transition even when the leaving player was
		// the only thing keeping the detector's chunk ticking (see DetectorTracker)
		DetectorTracker.register();

		LOGGER.info("Loaded player-detector (server-side with Pandorical)");
	}
}
