package com.stebars.canalsunblocked.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
import net.minecraft.block.ILiquidContainer;
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
		Log.info("called getFlowing without fineLevel, level ", level, " falling ", falling);
		return this.getFlowing().defaultFluidState()
				.setValue(LEVEL, Integer.valueOf(level))
				.setValue(FALLING, Boolean.valueOf(falling))
				.setValue(Util.FINE_LEVEL, Integer.valueOf(Util.LEVEL_MULTIPLIER * level));
		// TODO rewrite simpler, make it call function below
	}

	public FluidState getFlowing(boolean falling, int fineLevel) {
		if (fineLevel <= 0) {
			Log.info("called getFlowing with fineLevel <= 0, this should never happen, TODO remove those calls");
			return Fluids.EMPTY.defaultFluidState();
		}
		int level = Math.max(1, fineLevel / Util.LEVEL_MULTIPLIER);
		return getFlowing(level, fineLevel, falling);
	}

	public FluidState getFlowing(int level, int fineLevel, boolean falling) {
		Log.info("called getFlowing, level ", level, ", falling ", falling, ", fineLevel ", fineLevel);
		return this.getFlowing().defaultFluidState()
				.setValue(LEVEL, Integer.valueOf(level))
				.setValue(FALLING, Boolean.valueOf(falling))
				.setValue(Util.FINE_LEVEL, Integer.valueOf(fineLevel));
	}

	@Shadow public abstract Fluid getFlowing();


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
				BlockState blockstate = makeLegacyBlock(fluidstate);
				world.setBlock(pos, blockstate, 2);
				world.getLiquidTicks().scheduleTick(pos, fluidstate.getType(), i);
				world.updateNeighborsAt(pos, blockstate.getBlock());
			}
		}

		this.spread(world, pos, originalState);
	}

	@Overwrite
	protected void spreadTo(IWorld world, BlockPos pos, BlockState blockState, Direction p_205574_4_, FluidState fluidState) {
		if (blockState.getBlock() instanceof ILiquidContainer) {
			((ILiquidContainer)blockState.getBlock()).placeLiquid(world, pos, blockState, fluidState);
		} else {
			if (!blockState.isAir()) {
				this.beforeDestroyingBlock(world, pos, blockState);
			}

			world.setBlock(pos, makeLegacyBlock(fluidState), 3);
		}
	}
	
	private BlockState makeLegacyBlock(FluidState fluidState) {
		if (fluidState.hasProperty(Util.FINE_LEVEL)) {
			Log.info("makeLegacyBlock: found a fine level on fluidstate ", fluidState);
			BlockState result = fluidState.createLegacyBlock()
					.setValue(Util.FINE_LEVEL, Util.FINE_LEVEL_MAX - fluidState.getValue(Util.FINE_LEVEL));
					//.setValue(BlockStateProperties.LEVEL, Math.min(8, fluidState.getValue(LEVEL)));
			Log.info("...so, returning blockstate with level = ", result.getValue(BlockStateProperties.LEVEL), " and fineLevel = ", result.getValue(Util.FINE_LEVEL));
			//result = fluidState.createLegacyBlock().setValue(Util.FINE_LEVEL, 13).setValue(BlockStateProperties.LEVEL, 5);
			return result;
		}
		else {
			Log.info("makeLegacyBlock: no fine level on fluidstate ", fluidState);
			return fluidState.createLegacyBlock();
		}
	}
	
	@Shadow protected abstract void beforeDestroyingBlock(IWorld p_205580_1_, BlockPos p_205580_2_, BlockState p_205580_3_);

	@Shadow public abstract Fluid getSource();

	@Shadow public FluidState getSource(boolean p_207204_1_) {
		return this.getSource().defaultFluidState().setValue(FALLING, Boolean.valueOf(p_207204_1_));
	}
	
	@Shadow protected void spread(IWorld p_205575_1_, BlockPos p_205575_2_, FluidState p_205575_3_) {
	}




	/*@Overwrite
	public float getOwnHeight(FluidState p_223407_1_) {
		Log.info("fluidState.getOwnHeight called, amount is ", (float)p_223407_1_.getAmount(), " so returning height ", (float)p_223407_1_.getAmount() / 9.0F);
		return (float)p_223407_1_.getAmount() / 9.0F;
	}*/

	@Overwrite
	public float getOwnHeight(FluidState fluidState) {
		int fineLevel = fluidState.getValue(Util.FINE_LEVEL);
		int level;
		if (!fluidState.hasProperty(LEVEL)) { // for some reason fluidState.isSource() always returns false here
			Log.info("it's a source, no level, returning 0.9");
			fineLevel = Util.FINE_LEVEL_MAX; // For source blocks
		} else Log.info("it's NOT a source, has level ", fluidState.getValue(LEVEL));
		Log.info("fluidState.getOwnHeight called, fine level is ", fineLevel, " so returning height ", fineLevel / (((float) Util.FINE_LEVEL_MAX) + 1F));
		/*Log.info("state ", p_223407_1_, " has height ",
				(float)fineLevel, " / ", (((float) Util.FINE_LEVEL_MAX) + 5F), " = ",
				(float)fineLevel / (((float) Util.FINE_LEVEL_MAX) + 5F));*/
		return fineLevel / (((float) Util.FINE_LEVEL_MAX) + 1F);
		// Note, needs to be tuned depending on LEVEL_MULTIPLIER, else water will seem to flow uphill from source blocks next to solid blocks.
		// See constant 8/9 = 0.8888889F in vanilla code.
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

	// Overwrite this so we continue spreading at level 1, fine level 19
	@Overwrite
	private void spreadToSides(IWorld p_207937_1_, BlockPos p_207937_2_, FluidState p_207937_3_, BlockState p_207937_4_) {
		int newFineLevel = p_207937_3_.getValue(Util.FINE_LEVEL) - this.getDropOff(p_207937_1_);
		if (p_207937_3_.getValue(FALLING)) {
			newFineLevel = Util.FINE_LEVEL_MAX - 1;
		}

		if (newFineLevel > 0 || p_207937_3_.isSource()) { // the only change here is the `|| fluidState.isSource()`
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

		for(Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos blockpos = pos.relative(direction);
			BlockState blockstate = world.getBlockState(blockpos);
			FluidState fluidstate = blockstate.getFluidState();
			if (fluidstate.getType().isSame(this) && this.canPassThroughWall(direction, world, pos, state, blockpos, blockstate)) {
				if (fluidstate.isSource() && net.minecraftforge.event.ForgeEventFactory.canCreateFluidSource(world, blockpos, blockstate, this.canConvertToSource())) {
					++adjacentSources;
				}

				int fineLevelThere = fluidstate.getValue(Util.FINE_LEVEL);
				if (fluidstate.isSource()) fineLevelThere = Util.FINE_LEVEL_MAX;
				Log.info("for getNewLiquid at ", pos, " we found fine level of ", fineLevelThere, " at ", blockpos);
				maxAmount = Math.max(maxAmount, fineLevelThere);
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
			Log.info(pos, " - has fluid above, so setting to a flowing block with height 8");
			return this.getFlowing(8, true);
		} else {
			int k = maxAmount - this.getDropOff(world);
			Log.info(pos, " - has fluid adjacent with max amount ", maxAmount, " so minus dropoff ", this.getDropOff(world), "gives new fluid block with fineLevel ", k);
			return k <= 0 ? Fluids.EMPTY.defaultFluidState() : this.getFlowing(false, k);
		}
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