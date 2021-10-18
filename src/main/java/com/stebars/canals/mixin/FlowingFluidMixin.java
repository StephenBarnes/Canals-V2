package com.stebars.canals.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.stebars.canals.CanalsMod;
import com.stebars.canals.Util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ILiquidContainer;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin extends Fluid {

	@Shadow public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_FLOWING; // 1-8, not reversed
	@Shadow public static final BooleanProperty FALLING = BlockStateProperties.FALLING;

	private static final ResourceLocation tagCanalBase = new ResourceLocation(CanalsMod.MOD_ID, "canal_base");
	private static final ResourceLocation tagCanalWall = new ResourceLocation(CanalsMod.MOD_ID, "canal_wall");

	@Inject(method = "createFluidStateDefinition",
			at = @At("TAIL"))
	protected void createFluidStateDefinition(StateContainer.Builder<Fluid, FluidState> builder,
			CallbackInfo ci) {
		builder.add(Util.FLUID_FINE_LEVEL);
	}
	// We should be able to only add fine level to non-source blocks; but that would require modifying WaterFluid.Flowing, and LavaFluid.Flowing, and all modded fluids too
	// So instead we just add fine level to source blocks as well, but don't use the field


	@Shadow public abstract Fluid getFlowing();

	@Overwrite
	public FluidState getFlowing(int level, boolean falling) {
		return this.getFlowing().defaultFluidState()
				.setValue(LEVEL, Integer.valueOf(level))
				.setValue(FALLING, Boolean.valueOf(falling))
				.setValue(Util.FLUID_FINE_LEVEL, Integer.valueOf(Util.FINE_LEVELS - 1));
	}

	public FluidState getFlowingFine(boolean falling, int totalLevel) {
		if (totalLevel < Util.TOTAL_LEVEL_MIN)
			return Fluids.EMPTY.defaultFluidState();
		//if (totalLevel > Util.TOTAL_LEVEL_MAX) totalLevel = Util.TOTAL_LEVEL_MAX;
		int level = Util.totalToLevel(totalLevel);
		int fineLevel = Util.totalToFine(totalLevel);
		return getFlowing(level, fineLevel, falling);
	}

	public FluidState getFlowing(int level, int fineLevel, boolean falling) {
		return this.getFlowing().defaultFluidState()
				.setValue(LEVEL, Integer.valueOf(level))
				.setValue(FALLING, Boolean.valueOf(falling))
				.setValue(Util.FLUID_FINE_LEVEL, Integer.valueOf(fineLevel));
	}


	@Shadow protected abstract int getDropOff(IWorldReader p_205576_1_);

	@Shadow
	private boolean canPassThroughWall(Direction p_212751_1_, IBlockReader p_212751_2_, BlockPos p_212751_3_, BlockState p_212751_4_, BlockPos p_212751_5_, BlockState p_212751_6_) {
		return true;
	}

	@Shadow
	private boolean isSourceBlockOfThisType(FluidState p_211758_1_) {
		return p_211758_1_.getType().isSame(this) && p_211758_1_.isSource();
	}

	@Shadow public abstract boolean canConvertToSource();

	@Shadow protected int getSpreadDelay(World p_215667_1_, BlockPos p_215667_2_, FluidState p_215667_3_, FluidState p_215667_4_) {
		return 0;
	}

	@Overwrite
	public void tick(World world, BlockPos pos, FluidState originalState) {
		if (!originalState.isSource()) {
			FluidState fluidstate = this.getNewLiquid(world, pos, world.getBlockState(pos));
			int i = this.getSpreadDelay(world, pos, originalState, fluidstate);
			if (fluidstate.isEmpty()) {
				originalState = fluidstate;
				world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			} else if (!fluidstate.equals(originalState)) {
				originalState = fluidstate;
				BlockState blockstate = makeLegacyBlock(fluidstate);
				world.setBlock(pos, blockstate, 2);
				world.getLiquidTicks().scheduleTick(pos, fluidstate.getType(), i);
				world.updateNeighborsAt(pos, blockstate.getBlock());
			}
		}
		this.spread(world, pos, originalState);
	}

	@SuppressWarnings("deprecation")
	@Overwrite
	protected void spreadTo(IWorld world, BlockPos pos, BlockState blockState, Direction p_205574_4_, FluidState fluidState) {
		if (blockState.getBlock() instanceof ILiquidContainer)
			((ILiquidContainer)blockState.getBlock()).placeLiquid(world, pos, blockState, fluidState);
		else {
			if (!blockState.isAir())
				this.beforeDestroyingBlock(world, pos, blockState);
			world.setBlock(pos, makeLegacyBlock(fluidState), 3);
		}
	}

	private BlockState makeLegacyBlock(FluidState fluidState) {
		if (fluidState.hasProperty(Util.FLUID_FINE_LEVEL))
			return fluidState.createLegacyBlock()
					.setValue(Util.BLOCK_FINE_LEVEL, Util.FINE_LEVELS - fluidState.getValue(Util.FLUID_FINE_LEVEL) - 1);
		else
			return fluidState.createLegacyBlock();
	}

	@Shadow protected abstract void beforeDestroyingBlock(IWorld p_205580_1_, BlockPos p_205580_2_, BlockState p_205580_3_);

	@Shadow public abstract Fluid getSource();

	@Shadow public FluidState getSource(boolean p_207204_1_) {
		return this.getSource().defaultFluidState().setValue(FALLING, Boolean.valueOf(p_207204_1_));
	}

	@Shadow protected void spread(IWorld p_205575_1_, BlockPos p_205575_2_, FluidState p_205575_3_) {
	}


	@Overwrite
	public float getOwnHeight(FluidState fluidState) {
		int totalLevel = Util.getTotalLevel(fluidState.getAmount(), fluidState.getValue(Util.FLUID_FINE_LEVEL));
		if (!fluidState.hasProperty(LEVEL)) // For source blocks - for some reason fluidState.isSource() always returns false here
			totalLevel = Util.TOTAL_LEVEL_MAX + 10;
		totalLevel -= Util.TOTAL_LEVEL_MIN;
		return 0.89F * (totalLevel / ((float) Util.TOTAL_LEVEL_MAX + 15 - Util.TOTAL_LEVEL_MIN));
		// Note, needs to be tuned depending on FINE_LEVELS, else water will seem to flow uphill from source blocks next to solid blocks.
	}

	// Overwrite this so we continue spreading at level 1, fine level 15
	@Overwrite
	private void spreadToSides(IWorld p_207937_1_, BlockPos p_207937_2_, FluidState fluidState, BlockState p_207937_4_) {
		int totalLevel = Util.getTotalLevel(fluidState.getAmount(), fluidState.getValue(Util.FLUID_FINE_LEVEL));
		int newTotalLevel;
		if (fluidState.getValue(FALLING))
			newTotalLevel = Util.TOTAL_LEVEL_MAX - 1; // TODO remove the -1?
		else
			newTotalLevel = totalLevel - this.getDropOff(p_207937_1_);

		if (newTotalLevel >= Util.TOTAL_LEVEL_MIN || fluidState.isSource()) {
			Map<Direction, FluidState> map = this.getSpread(p_207937_1_, p_207937_2_, p_207937_4_);

			for(Entry<Direction, FluidState> entry : map.entrySet()) {
				Direction direction = entry.getKey();
				FluidState fluidstate = entry.getValue();
				BlockPos blockpos = p_207937_2_.relative(direction);
				BlockState blockstate = p_207937_1_.getBlockState(blockpos);
				if (this.canSpreadTo(p_207937_1_, p_207937_2_, p_207937_4_, direction, blockpos, blockstate, p_207937_1_.getFluidState(blockpos), fluidstate.getType())) {
					this.spreadTo(p_207937_1_, blockpos, blockstate, direction, fluidstate);
				}
			}
		}
	}

	@Shadow
	protected boolean canSpreadTo(IBlockReader p_205570_1_, BlockPos p_205570_2_, BlockState p_205570_3_, Direction p_205570_4_, BlockPos p_205570_5_, BlockState p_205570_6_, FluidState p_205570_7_, Fluid p_205570_8_) {
		return false;
	}

	@Shadow
	protected Map<Direction, FluidState> getSpread(IWorldReader p_205572_1_, BlockPos p_205572_2_, BlockState p_205572_3_) {
		return new HashMap<Direction, FluidState>();
	}

	@Overwrite
	protected FluidState getNewLiquid(IWorldReader world, BlockPos pos, BlockState state) {
		int maxAmount = 0;
		int adjacentSources = 0;
		boolean inCanal = posInCanal(world, pos);

		for(Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos blockpos = pos.relative(direction);
			BlockState blockstate = world.getBlockState(blockpos);
			FluidState fluidstate = blockstate.getFluidState();
			if (fluidstate.getType().isSame(this) && this.canPassThroughWall(direction, world, pos, state, blockpos, blockstate)) {
				if (fluidstate.isSource() && net.minecraftforge.event.ForgeEventFactory.canCreateFluidSource(world, blockpos, blockstate, this.canConvertToSource()))
					++adjacentSources;

				if (inCanal) {
					int totalLevelThere = Util.getTotalLevel(fluidstate.getAmount(), fluidstate.getValue(Util.FLUID_FINE_LEVEL));
					if (fluidstate.isSource()) totalLevelThere = Util.TOTAL_LEVEL_MAX;
					maxAmount = Math.max(maxAmount, totalLevelThere);
				} else {
					maxAmount = Math.max(maxAmount, fluidstate.getAmount());
				}
			}
		}

		if (adjacentSources >= 2) {
			BlockState blockstate1 = world.getBlockState(pos.below());
			FluidState fluidstate1 = blockstate1.getFluidState();
			if (blockstate1.getMaterial().isSolid() || this.isSourceBlockOfThisType(fluidstate1)) {
				return this.getSource(false);
			}
		}

		BlockPos blockpos1 = pos.above();
		BlockState blockstate2 = world.getBlockState(blockpos1);
		FluidState fluidstate2 = blockstate2.getFluidState();
		if (!fluidstate2.isEmpty() && fluidstate2.getType().isSame(this) && this.canPassThroughWall(Direction.UP, world, pos, state, blockpos1, blockstate2))
			return this.getFlowing(8, true);
		else {
			if (inCanal) {
				FluidState currentFluidState = state.getFluidState();
				int currentTotalLevel = currentFluidState.isEmpty() ? 0 :
					Util.getTotalLevel(currentFluidState.getAmount(), currentFluidState.getValue(Util.FLUID_FINE_LEVEL));
				int newTotalLevel = (currentTotalLevel > maxAmount) ?
						(currentTotalLevel - this.getDropOff(world) * Util.DROP_MULTIPLIER) // drop faster if it's higher than all adjacent fluid levels
						: maxAmount - this.getDropOff(world);
				return newTotalLevel < Util.TOTAL_LEVEL_MIN ? Fluids.EMPTY.defaultFluidState() : this.getFlowingFine(false, newTotalLevel);
			} else {
				int newLevel = maxAmount - this.getDropOff(world);
				return newLevel <= 0 ? Fluids.EMPTY.defaultFluidState() : this.getFlowing(newLevel, false);
			}
		}
	}

	@SuppressWarnings("deprecation")
	protected boolean posInCanal(IWorldReader world, BlockPos pos) {
		if (!validCanalBaseBlock(world.getBlockState(pos.below()).getBlock()))
			return false;
		int validSideCount = 0;
		for(Direction direction : Direction.Plane.HORIZONTAL) {
			for (int i = 1; i <= Util.HORIZONTAL_SCAN_LENGTH; i++) {
				BlockPos sidePos = pos.relative(direction, i);
				BlockState sideState = world.getBlockState(sidePos);
				Block sideBlock = sideState.getBlock();
				if (validCanalSideBlock(sideBlock)) {
					validSideCount++;
					break;
				} else if (!sideBlock.isAir(sideState, world, sidePos) && sideBlock.getFluidState(sideState).isEmpty())
					return false;
			}
		}
		return validSideCount >= 2;
	}

	protected boolean validCanalBaseBlock(Block block) {
		return block.getTags().contains(tagCanalBase);
	}

	protected boolean validCanalSideBlock(Block block) {
		return block.getTags().contains(tagCanalWall);
	}

	// TODO use @Inject etc. instead of @Overwrite
}