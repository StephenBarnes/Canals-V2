package com.stebars.canalsunblocked.mixin;

import org.jline.utils.Log;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.Lists;
import com.stebars.canalsunblocked.Util;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.WaterFluid;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin2 extends Fluid {

	/*
	 * So, state rn:
	 * Flowing on polished stone only works if it flows from elsewhere onto that block. Probably because that causes it to actually get fineLevel 80
	 * However, falling creating fineLevel 80 makes it effectively a source block -- need to fix, check how vanilla does it
	 * It's calling getFlowing() and getFluidState() maybe every tick, so need to figure out what's calling it and fix that
	 * State updates don't happen, eg changing from polished stone to dirt while water is on top won't make the level drop
	 * Buckets don't work
	 * "getFluidState returning level 3 and fine level 58" -- should never happen
	 * Block that falls down from a fluid should not be treated as a source block -- if block above is removed, it should empty
	 */

	@Shadow public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_FLOWING; // 1-8, not reversed
	@Shadow public static final BooleanProperty FALLING = BlockStateProperties.FALLING;

	/*@Inject(at = @At("HEAD"), method = "canConvertToSource", cancellable = true)
	protected void canConvertToSource(CallbackInfoReturnable<Boolean> info) {
		info.setReturnValue(false);
	}*/ // TODO remove

	/*public int getTickDelay(IWorldReader p_205569_1_) {
		return 1;
	}*/

	/*@Inject(at = @At("RETURN"), method = "createFluidStateDefinition", cancellable = true)
	protected void createFluidStateDefinition(StateContainer.Builder<Fluid, FluidState> p_207184_1_) {
		p_207184_1_.add(FINE_LEVEL);
	}*/
	

	@Overwrite
	protected void createFluidStateDefinition(StateContainer.Builder<Fluid, FluidState> p_207184_1_) {
		p_207184_1_.add(FALLING);
		p_207184_1_.add(Util.FINE_LEVEL);
	}
	// We should be able to only add fine level to non-source blocks; but that would require modifying WaterFluid.Flowing, and LavaFluid.Flowing, and all modded fluids too
	// So instead we just add fine level to source blocks as well, but don't use the field


	@Overwrite
	public FluidState getFlowing(int level, boolean falling) {
		Log.info("called getFlowing, level ", level, " falling ", falling);
		return this.getFlowing().defaultFluidState()
				.setValue(LEVEL, Integer.valueOf(level)) // this is 1-8 and reversed
				.setValue(FALLING, Boolean.valueOf(falling))
				.setValue(Util.FINE_LEVEL, Integer.valueOf(Util.LEVEL_MULTIPLIER * level));
	}


	@Overwrite

	protected FluidState getNewLiquid(IWorldReader world, BlockPos pos, BlockState state) {
		int maxAmount = 0;
		int adjacentSources = 0;

		for(Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos blockpos = pos.relative(direction);
			BlockState blockstate = world.getBlockState(blockpos);
			FluidState fluidstate = blockstate.getFluidState();
			if (fluidstate.getType().isSame(this) && this.canPassThroughWall(direction, world, pos, state, blockpos, blockstate)) {
				if (fluidstate.isSource() && net.minecraftforge.event.ForgeEventFactory.canCreateFluidSource(world, blockpos, blockstate, this.canConvertToSource())) {
					++adjacentSources;
				}

				maxAmount = Math.max(maxAmount, fluidstate.getAmount());
			}
		}
		Log.info("getNewLiquid at pos ", pos, " got maxAmount ", maxAmount, " and adjacentSources ", adjacentSources);

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
		if (!fluidstate2.isEmpty() && fluidstate2.getType().isSame(this) && this.canPassThroughWall(Direction.UP, world, pos, state, blockpos1, blockstate2)) {
			return this.getFlowing(8, true);
		} else {
			int k = maxAmount - this.getDropOff(world);
			return k <= 0 ? Fluids.EMPTY.defaultFluidState() : this.getFlowing(k, false);
		}
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

	@Shadow public abstract Fluid getFlowing();

	@Shadow public abstract boolean canConvertToSource();

	@Shadow protected int getSpreadDelay(World p_215667_1_, BlockPos p_215667_2_, FluidState p_215667_3_, FluidState p_215667_4_) {
		return 0;
	}

	@Overwrite
	public void tick(World world, BlockPos pos, FluidState originalState) {
		Log.info("called FlowingFluid.tick, pos ", pos); // " fluidState has amount() of ", originalState.getAmount());
		if (!originalState.isSource()) {
			Log.info("for tick: it's not source, running extra");
			FluidState fluidstate = this.getNewLiquid(world, pos, world.getBlockState(pos));
			int i = this.getSpreadDelay(world, pos, originalState, fluidstate);
			if (fluidstate.isEmpty()) {
				Log.info("for tick: new state is empty");
				originalState = fluidstate;
				world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			} else if (!fluidstate.equals(originalState)) {
				Log.info("for tick: fluidstate has changed");
				originalState = fluidstate;
				BlockState blockstate = fluidstate.createLegacyBlock();
				world.setBlock(pos, blockstate, 2);
				world.getLiquidTicks().scheduleTick(pos, fluidstate.getType(), i);
				world.updateNeighborsAt(pos, blockstate.getBlock());
			}
		}

		this.spread(world, pos, originalState);
	}

	@Shadow protected void spread(IWorld p_205575_1_, BlockPos p_205575_2_, FluidState p_205575_3_) {}

	@Shadow public abstract Fluid getSource();

	@Shadow public FluidState getSource(boolean p_207204_1_) {
		return this.getSource().defaultFluidState().setValue(FALLING, Boolean.valueOf(p_207204_1_));
	}




	/*@Overwrite
	public float getOwnHeight(FluidState p_223407_1_) {
		Log.info("fluidState.getOwnHeight called, amount is ", (float)p_223407_1_.getAmount(), " so returning height ", (float)p_223407_1_.getAmount() / 9.0F);
		return (float)p_223407_1_.getAmount() / 9.0F;
	}*/
	
	@Overwrite
	public float getOwnHeight(FluidState p_223407_1_) {
		int fineLevel = p_223407_1_.getValue(Util.FINE_LEVEL);
		if (fineLevel == 0) fineLevel = Util.FINE_LEVEL_MAX; // For source blocks
		Log.info("fluidState.getOwnHeight called, amount is ", (float)p_223407_1_.getAmount(), " so returning height ", fineLevel / (((float) Util.FINE_LEVEL_MAX) + 5F));
		/*Log.info("state ", p_223407_1_, " has height ",
				(float)fineLevel, " / ", (((float) Util.FINE_LEVEL_MAX) + 5F), " = ",
				(float)fineLevel / (((float) Util.FINE_LEVEL_MAX) + 5F));*/
		return fineLevel / (((float) Util.FINE_LEVEL_MAX) + 5F);
	}

	/*@Overwrite
	public VoxelShape getShape(FluidState p_215664_1_, IBlockReader p_215664_2_, BlockPos p_215664_3_) {
		return p_215664_1_.getAmount() == 9 && hasSameAbove(p_215664_1_, p_215664_2_, p_215664_3_) ? VoxelShapes.block() : this.shapes.computeIfAbsent(p_215664_1_, (p_215668_2_) -> {
			return VoxelShapes.box(0.0D, 0.0D, 0.0D, 1.0D, (double)p_215668_2_.getHeight(p_215664_2_, p_215664_3_), 1.0D);
		});
	}*/

	@Overwrite
	protected static int getLegacyLevel(FluidState p_207205_0_) {
		int val = p_207205_0_.isSource() ? 0 : 8 - Math.min(p_207205_0_.getAmount(), 8) + (p_207205_0_.getValue(FALLING) ? 8 : 0);
		Log.info("called getLegacyLevel, returning ", val);
		return val;
	}



	/* Notes
	 * For a source block, .getAmount() returns 8
	 * We call .getFlowing() with the same "level" that's displayed on F3, ie it's 7 next to source blocks
	 * It does indeed call WaterFluid.createLegacyBlock on every block creation, both source and flowing
	 * 		this is called with legacy level 0, so the block has its level(0-15) set to 0 for source block, 1 for adjacent, etc.
	 * The fluidState.getOwnHeight is still indeed 8 and 7, ie it doesn't use the legacy values
	 * TODO make it use cache, but for every fine level, not just every level
	 */
}