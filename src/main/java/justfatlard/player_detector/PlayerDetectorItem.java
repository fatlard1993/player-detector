package justfatlard.player_detector;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import xyz.nucleoid.packettweaker.PacketContext;

public class PlayerDetectorItem extends BlockItem implements PolymerItem {
	private final Identifier modelId;

	public PlayerDetectorItem(Block block, Item.Settings settings) {
		super(block, settings);
		this.modelId = Identifier.of(Main.MOD_ID, "player_detector");
	}

	@Override
	public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
		return Items.PAPER;
	}

	@Override
	public Identifier getPolymerItemModel(ItemStack itemStack, PacketContext context) {
		return this.modelId;
	}
}
