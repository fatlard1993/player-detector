package justfatlard.player_detector;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class PlayerDetector extends Block implements SimpleWaterloggedBlock {
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	private static final int TICK_RATE = 10; // Check more frequently for responsive feel

	protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);

	public PlayerDetector(Properties settings) {
		super(settings);
		this.registerDefaultState(this.getStateDefinition().any()
			.setValue(WATERLOGGED, false)
			.setValue(POWERED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(WATERLOGGED, POWERED);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
		return this.defaultBlockState().setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!world.isClientSide()) {
			world.scheduleTick(pos, this, TICK_RATE);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
		boolean playerOn = isPlayerStandingOn(world, pos);
		boolean currentlyPowered = state.getValue(POWERED);

		if (playerOn != currentlyPowered) {
			world.setBlock(pos, state.setValue(POWERED, playerOn), Block.UPDATE_ALL);
			updateNeighbors(world, pos);
		}

		if (playerOn) {
			world.sendParticles(
				ParticleTypes.ELECTRIC_SPARK,
				pos.getX() + 0.5, pos.getY() + 0.15, pos.getZ() + 0.5,
				1, 0.3, 0.02, 0.3, 0.01
			);
		}

		world.scheduleTick(pos, this, TICK_RATE);
	}

	private boolean isPlayerStandingOn(Level world, BlockPos pos) {
		// Detection box just above the block (player standing on it)
		AABB detectionBox = new AABB(
			pos.getX(), pos.getY() + 0.125, pos.getZ(),
			pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0
		);
		List<Player> players = world.getEntitiesOfClass(Player.class, detectionBox, player -> !player.isSpectator());
		return !players.isEmpty();
	}

	private void updateNeighbors(Level world, BlockPos pos) {
		world.updateNeighborsAt(pos, this, null);
		for (Direction direction : Direction.values()) {
			world.updateNeighborsAt(pos.relative(direction), this, null);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
		if (state.getValue(POWERED)) {
			updateNeighbors(world, pos);
		}
		super.affectNeighborsAfterRemoval(state, world, pos, moved);
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
		return state.getValue(POWERED) ? 15 : 0;
	}

	@Override
	protected int getDirectSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
		return state.getValue(POWERED) && direction == Direction.UP ? 15 : 0;
	}

	@Override
	public BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (state.getValue(WATERLOGGED)) {
			tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
		}
		return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
	}
}
