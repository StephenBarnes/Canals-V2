package com.stebars.canals;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

public class CanalBlock extends Block {
	// Not implementing IWaterloggable, that doesn't let flowing water pass through anyway, only lets it be placed in source blocks

	// References:
	// FlowingFluidBlock for fluid handling
	// NetherPortalBlock for horizontal axes -- no other block uses it

	public static final int WATER_MAX_LEVEL = 8; // It's defined as 0-15, but in practice it's 7 right next to a source block
	public static final int CANAL_LEVEL_MULTIPLIER = 10;
	public static final int CANAL_MAX_LEVEL = WATER_MAX_LEVEL * CANAL_LEVEL_MULTIPLIER;

	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;
	public static final IntegerProperty CANAL_LEVEL = IntegerProperty.create("canal_level", 0, CANAL_MAX_LEVEL);

	// I'll make this canal block only transport water, then subclass for a type that only transports lava
	// Lava canals can be built from different materials, eg nether brick walls and obsidian floor
	private final FlowingFluid FLUID_SOURCE = Fluids.WATER;
	private final FlowingFluid FLUID_FLOWING = Fluids.FLOWING_WATER;

	protected static final VoxelShape X_AXIS_AABB =
			VoxelShapes.or(VoxelShapes.box(0, 2D/16, 14D/16, 1, 1, 1),
					VoxelShapes.box(0, 0, 0, 1, 2D/16, 1),
					VoxelShapes.box(0, 2D/16, 0, 1, 1, 2D/16D));
	protected static final VoxelShape Z_AXIS_AABB =
			VoxelShapes.or(VoxelShapes.box(14D/16, 2D/16, 0, 1, 1, 1),
					VoxelShapes.box(0, 0, 0, 1, 2D/16, 1),
					VoxelShapes.box(0, 2D/16, 0, 2D/16, 1, 1));


	public CanalBlock() {
		super(AbstractBlock.Properties.of(Material.STONE).requiresCorrectToolForDrops().strength(2.0F, 6.0F));
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(AXIS, Direction.Axis.X)
				.setValue(LEVEL, Integer.valueOf(0))
				.setValue(CANAL_LEVEL, Integer.valueOf(0)));
	}

	protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> p_206840_1_) {
		p_206840_1_.add(AXIS, LEVEL, CANAL_LEVEL);
	}

	public boolean propagatesSkylightDown(BlockState p_200123_1_, IBlockReader p_200123_2_, BlockPos p_200123_3_) {
		return false;
	}

	public BlockState getStateForPlacement(BlockItemUseContext context) {
		FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
		boolean startWaterlogged = fluidstate.getType() == Fluids.WATER;
		// TODO a state update here somehow, bc else it doesn't pick up adjacent water after being placed
		return super.getStateForPlacement(context)
				.setValue(AXIS, directionToAxis(context.getHorizontalDirection()))
				.setValue(LEVEL, Integer.valueOf(startWaterlogged ? WATER_MAX_LEVEL : 0))
				.setValue(CANAL_LEVEL, Integer.valueOf(startWaterlogged ? CANAL_MAX_LEVEL : 0));
	}

	public Direction.Axis directionToAxis(Direction dir) {
		switch (dir) {
		case EAST:
		case WEST:
			return Direction.Axis.X;
		default:
			return Direction.Axis.Z;
		}
	}

	public VoxelShape getShape(BlockState p_220053_1_, IBlockReader p_220053_2_, BlockPos p_220053_3_, ISelectionContext p_220053_4_) {
		// Since this inherits from AbstractBlock, this will also be response to getCollisionShape, getSupportShape, getVisualShape, etc.
		switch((Direction.Axis)p_220053_1_.getValue(AXIS)) {
		case Z:
			return Z_AXIS_AABB;
		case X:
		default:
			return X_AXIS_AABB;
		}
	}

	public BlockState rotate(BlockState state, Rotation rotation) {
		switch(rotation) {
		case COUNTERCLOCKWISE_90:
		case CLOCKWISE_90:
			switch((Direction.Axis)state.getValue(AXIS)) {
			case Z:
				return state.setValue(AXIS, Direction.Axis.X);
			case X:
				return state.setValue(AXIS, Direction.Axis.Z);
			default:
				return state;
			}
		default:
			return state;
		}
	}

	// Note that farmland blocks determine whether they're close to water by checking the .getFluidState() of nearby blocks
	public FluidState getFluidState(BlockState state) {
		int level = state.getValue(LEVEL);
		if (level == 0)
			return Fluids.EMPTY.defaultFluidState();
		return FLUID_SOURCE.getFlowing(level, false);
	}

	public boolean isPathfindable(BlockState p_196266_1_, IBlockReader p_196266_2_, BlockPos p_196266_3_, PathType p_196266_4_) {
		return true; // TODO make false for lava canal, if level > 0
	}

	public void neighborChanged(BlockState state, World world, BlockPos pos1, Block block, BlockPos pos2, boolean flag) {
		// state is state of this block, pos1 is position of this block; Block is the block it was BEFORE change, pos2 is the neighbor's position, flag is false

		// Get the 2 positions connected to this canal
		Direction dir = getPositiveDirection(state);
		BlockPos posA = pos1.relative(dir);
		BlockPos posB = pos1.relative(dir, -1);
		BlockPos posAbove = pos1.above();

		// Check if change is along the axis of this block; if not, ignore
		if (!pos2.equals(posA) && !pos2.equals(posB) && !pos2.equals(posAbove))
			return;

		updateState(state, world, pos1, posA, posB, posAbove);
	}
	
	public boolean updateState(BlockState state, World world, BlockPos pos) {
		Direction dir = getPositiveDirection(state);
		return updateState(state, world, pos, pos.relative(dir), pos.relative(dir, -1), pos.above());
	}

	public boolean updateState(BlockState state, World world, BlockPos pos1, BlockPos posA, BlockPos posB, BlockPos posAbove) {
		int newLevel, newCLevel;
		BlockState stateA = world.getBlockState(posA);
		FluidState fluidA = world.getFluidState(posA);
		Block blockA = stateA.getBlock();
		BlockState stateB = world.getBlockState(posB);
		FluidState fluidB = world.getFluidState(posB);
		Block blockB = stateB.getBlock();
		
		FluidState fluidAbove = world.getFluidState(posAbove);
		Fluid fluidTypeAbove = fluidAbove.getType();
		if (!fluidAbove.isEmpty() &&
				(fluidTypeAbove == FLUID_SOURCE || fluidTypeAbove == FLUID_FLOWING)) {
			newLevel = WATER_MAX_LEVEL;
			newCLevel = CANAL_MAX_LEVEL;
		} else {
			int cLevelA = getCanalLevel(blockA, stateA, fluidA);
			int cLevelB = getCanalLevel(blockB, stateB, fluidB);

			int higherCLevel = cLevelA > cLevelB ? cLevelA : cLevelB;
			newCLevel = (higherCLevel == 0) ? 0 : higherCLevel - 1;
			newLevel = newCLevel / CANAL_LEVEL_MULTIPLIER;
		}

		int level = state.getValue(LEVEL);
		int canalLevel = state.getValue(CANAL_LEVEL);

		if (canalLevel != newCLevel || newLevel != level) {
			state = state.setValue(CANAL_LEVEL, newCLevel)
					.setValue(LEVEL, newLevel);
			world.setBlock(pos1, state, 3); // 3 = 1 (causes block update) + 2 (send change to clients)
			
			//if (newCLevel != 0)
			//	world.getLiquidTicks().scheduleTick(pos1, state.getFluidState().getType(), CONTAINED_FLUID.getTickDelay(world));
			// NB code above causes canal blocks to disappear, TODO

			if (blockA.isAir(stateA, world, posA))
				world.getLiquidTicks().scheduleTick(posA, fluidA.getType(), FLUID_SOURCE.getTickDelay(world));
			if (blockB.isAir(stateB, world, posA))
				world.getLiquidTicks().scheduleTick(posA, fluidB.getType(), FLUID_SOURCE.getTickDelay(world));
			return true;
		}

		// Even if water level didn't change, schedule ticks at adjacent spots, e.g. removing a dirt block along a canal
		/*if (blockA.isAir(stateA, world, posA))
			world.getLiquidTicks().scheduleTick(posA, fluidA.getType(), FLUID_SOURCE.getTickDelay(world));
		if (blockB.isAir(stateB, world, posA))
			world.getLiquidTicks().scheduleTick(posA, fluidB.getType(), FLUID_SOURCE.getTickDelay(world));*/
		// TODO code above doesn't seem to have any effect, water still stays in canals until a canal block is destroyed
		
		return false;
	}

	public int getCanalLevel(Block block, BlockState state, FluidState fluid) {
		if (block instanceof CanalBlock)
			return state.getValue(CANAL_LEVEL);
		if (fluid.isEmpty())
			return 0;
		return fluid.getAmount() * CANAL_LEVEL_MULTIPLIER; // fluid.getAmount just returns the LEVEL property
	}

	public boolean isConnectedTo(BlockState state, BlockPos pos1, BlockPos pos2) {
		Direction dir = getPositiveDirection(state.getValue(AXIS));
		return (pos1.relative(dir) == pos2) || (pos1.relative(dir, -1) == pos2);
	}

	public Direction getPositiveDirection(Direction.Axis axis) {
		switch (axis) {
		case X:
			return Direction.EAST;
		case Z:
			return Direction.SOUTH;
		case Y:
		default:
			return Direction.UP;
		}
	}

	public Direction getPositiveDirection(BlockState state) {
		return getPositiveDirection(state.getValue(AXIS));
	}

	public BlockPos otherDirectionTo(BlockPos pos1, BlockPos pos2) {
		// With pos1 as center, return position on the other side from pos2
		Vector3i diff = pos1.subtract(pos2);
		return pos1.offset(-diff.getX(), -diff.getY(), -diff.getZ());
	}

	// Tick function from FlowingFluid
	/*
	public void tickContainedFluid(World world, BlockPos pos, FluidState fluidState) {
		if (!fluidState.isSource()) {
			FluidState newFluidState = CONTAINED_FLUID.getNewLiquid(world, pos, world.getBlockState(pos));
			int i = CONTAINED_FLOWING_FLUID.getSpreadDelay(world, pos, fluidState, newFluidState);
			if (newFluidState.isEmpty()) {
				fluidState = newFluidState;
				world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			} else if (!newFluidState.equals(fluidState)) {
				fluidState = newFluidState;
				BlockState blockstate = newFluidState.createLegacyBlock();
				world.setBlock(pos, blockstate, 2);
				world.getLiquidTicks().scheduleTick(pos, newFluidState.getType(), i);
				world.updateNeighborsAt(pos, blockstate.getBlock());
			}
		}

		this.spread(world, pos, fluidState);
	}*/
	

	@Override
	public void onPlace(BlockState state, World world, BlockPos pos, BlockState p_220082_4_, boolean p_220082_5_) {
		// Update state to get initial value
		updateState(state, world, pos);
	}

}
