package justfatlard.player_detector;

import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class Main implements ModInitializer {
	public static final String MOD_ID = "player-detector-justfatlard";

	public static final Identifier PLAYER_DETECTOR_ID = Identifier.of(MOD_ID, "player_detector");

	public static final PlayerDetector PLAYER_DETECTOR_BLOCK = new PlayerDetector(
		AbstractBlock.Settings.create()
			.strength(2.0f, 3.0f)
			.sounds(BlockSoundGroup.STONE)
			.registryKey(RegistryKey.of(RegistryKeys.BLOCK, PLAYER_DETECTOR_ID))
	);

	@Override
	public void onInitialize() {
		PolymerResourcePackUtils.addModAssets(MOD_ID);
		PolymerResourcePackUtils.markAsRequired();

		// Register block
		Registry.register(Registries.BLOCK, PLAYER_DETECTOR_ID, PLAYER_DETECTOR_BLOCK);

		// Register item
		RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, PLAYER_DETECTOR_ID);
		Item playerDetectorItem = new PlayerDetectorItem(
			PLAYER_DETECTOR_BLOCK,
			new Item.Settings().useBlockPrefixedTranslationKey().registryKey(itemKey)
		);
		Registry.register(Registries.ITEM, PLAYER_DETECTOR_ID, playerDetectorItem);

		// Setup Polymer model for server-side rendering (flat tripwire for thin blocks)
		Identifier modelId = Identifier.of(MOD_ID, "block/player_detector");
		BlockState polymerState = PolymerBlockResourceUtils.requestBlock(
			BlockModelType.TRIPWIRE_BLOCK_FLAT,
			PolymerBlockModel.of(modelId)
		);

		if (polymerState != null) {
			PLAYER_DETECTOR_BLOCK.setPolymerBlockState(polymerState);
		} else {
			System.err.println("[player-detector] Failed to request polymer model - no slots available");
		}

		// Create item group
		ItemGroup group = PolymerItemGroupUtils.builder()
			.displayName(Text.literal("Player Detector"))
			.icon(() -> new ItemStack(playerDetectorItem))
			.entries((context, entries) -> {
				entries.add(new ItemStack(playerDetectorItem));
			})
			.build();
		PolymerItemGroupUtils.registerPolymerItemGroup(Identifier.of(MOD_ID, "player_detector"), group);

		System.out.println("[player-detector] Loaded player-detector (server-side with Polymer)");
	}
}
