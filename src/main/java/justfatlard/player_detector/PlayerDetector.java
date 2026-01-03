package justfatlard.player_detector;

import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

public class PlayerDetector extends Block implements Waterloggable, PolymerTexturedBlock {
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	public static final BooleanProperty POWERED = Properties.POWERED;

	private static final int TICK_RATE = 10; // Check more frequently for responsive feel

	protected static final VoxelShape SHAPE = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);

	private BlockState polymerBlockState;

	public PlayerDetector(Settings settings) {
		super(settings);
		this.setDefaultState(this.stateManager.getDefaultState()
			.with(WATERLOGGED, false)
			.with(POWERED, false));
	}

	public void setPolymerBlockState(BlockState state) {
		this.polymerBlockState = state;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(WATERLOGGED, POWERED);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
		return this.polymerBlockState != null ? this.polymerBlockState : state;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
		return this.getDefaultState().with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}

	@Override
	protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		if (!world.isClient()) {
			world.scheduleBlockTick(pos, this, TICK_RATE);
		}
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		boolean playerOn = isPlayerStandingOn(world, pos);
		boolean currentlyPowered = state.get(POWERED);

		if (playerOn != currentlyPowered) {
			world.setBlockState(pos, state.with(POWERED, playerOn), Block.NOTIFY_ALL);
			updateNeighbors(world, pos);
		}

		world.scheduleBlockTick(pos, this, TICK_RATE);
	}

	private boolean isPlayerStandingOn(World world, BlockPos pos) {
		// Detection box just above the block (player standing on it)
		Box detectionBox = new Box(
			pos.getX(), pos.getY() + 0.125, pos.getZ(),
			pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0
		);
		List<PlayerEntity> players = world.getEntitiesByClass(PlayerEntity.class, detectionBox, player -> !player.isSpectator());
		return !players.isEmpty();
	}

	private void updateNeighbors(World world, BlockPos pos) {
		world.updateNeighborsAlways(pos, this, null);
		for (Direction direction : Direction.values()) {
			world.updateNeighborsAlways(pos.offset(direction), this, null);
		}
	}

	@Override
	protected boolean emitsRedstonePower(BlockState state) {
		return true;
	}

	@Override
	protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
		return state.get(POWERED) ? 15 : 0;
	}

	@Override
	protected int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
		return state.get(POWERED) && direction == Direction.UP ? 15 : 0;
	}

	@Override
	public BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
		if (state.get(WATERLOGGED)) {
			tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		}
		return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
	}
}
