package com.stebars.canalsunblocked.mixin;

import java.util.ArrayList;
import java.util.List;

import org.jline.utils.Log;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.Lists;
import com.stebars.canalsunblocked.Util;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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

@Mixin(FlowingFluidBlock.class)
public abstract class FlowingFluidBlockMixin extends Block {

	public FlowingFluidBlockMixin(Properties p_i48440_1_) {
		super(p_i48440_1_);
		this.fineStateCache = null;
		// TODO Auto-generated constructor stub
	}

	@Shadow public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;
	// block uses .LEVEL (0-15), FlowingFluid uses .LEVEL_FLOWING (1-8), which is called LEVEL inside FlowingFluid
	// Note that LEVEL_FLOWING is reversed, i.e. higher for lower levels of water.
	// We use the same Util.FINE_LEVEL property for both blocks and FlowingFluid
	// Fine level is 80 when it's full, 0 when it's empty
	// So, fine level on a FlowingFluid must be reversed, it must correspond to multiplier * (8 - level_flowing)
	// But on a block, we have fine level = block's level * 10, assuming it's only up to 10
	// WaterFluid etc., and WaterFluid.getAmount(), return the LEVEL_FLOWING, ie 1-8 and reversed
	// in-game, in vanilla, flowing blocks show "level" from 1-7, not reversed, ie 7 is next to source block
	// 		these are the block levels, not level_flowing

	//@Shadow public static final BooleanProperty FALLING = BlockStateProperties.FALLING;

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

	@Inject(method = "createBlockStateDefinition",
			at = @At("TAIL"))
	protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> p_206840_1_,
			CallbackInfo ci) {
		p_206840_1_.add(Util.FINE_LEVEL);
	}

	/*@Inject(method = "<init>",
			at = @At("TAIL"))
	public void FlowingFluidBlock(java.util.function.Supplier<? extends FlowingFluid> supplier,
			AbstractBlock.Properties p_i48368_1_, CallbackInfo ci) {
	    this.registerDefaultState(this.stateDefinition.any()
	    		  .setValue(LEVEL, Integer.valueOf(0))
	    		  .setValue(FINE_LEVEL, Integer.valueOf(FINE_LEVEL_MAX)));
	}*/

	@Inject(method = "<init>",
			at = @At("TAIL"))
	public void FlowingFluidBlock(FlowingFluid p_i49014_1_, AbstractBlock.Properties p_i49014_2_,
			CallbackInfo ci) {
		Log.info("Creating new block SABB with fine level 0 and level 0");
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(LEVEL, Integer.valueOf(0))
				.setValue(Util.FINE_LEVEL, Integer.valueOf(0))); // TODO necessary?
		this.fineStateCache = Lists.newArrayList();
	}

	/*@Overwrite
	public FluidState getFluidState(BlockState state) {		 
		int level = state.getValue(LEVEL);
		int fineLevel = state.getValue(Util.FINE_LEVEL);
		if (!fineStateCacheInitialized) initFineStateCache();
		// HERE is the issue! TODO it's reading source blocks as having fine level 80
		if (getFluid().isSource(getFluid().defaultFluidState()))
			return getFluid().getSource(false);
		if (fineLevel >= Util.FINE_LEVEL_MAX) {
			Log.info("SABB NB level was >= FINE_LEVEL_MAX, namely ", fineLevel, " so returning the fluidstate that's flowing with level 80");
			//return this.fineStateCache.get(Util.FINE_LEVEL_MAX);
			return getFluid().getFlowing(8, true).setValue(Util.FINE_LEVEL, Integer.valueOf(Util.FINE_LEVEL_MAX));
		}
		if (level == 0) {
			Log.info("SABBBB NB level was zero in flowingfluidblock.getFluidState");
			//return this.fineStateCache.get(0);
			return Fluids.EMPTY.defaultFluidState();
		}
		//return this.fineStateCache.get(Util.FINE_LEVEL_MAX - fineLevel);
		return getFluid().getFlowing(level, false).setValue(Util.FINE_LEVEL, fineLevel);
	}*/

	/*protected synchronized void initFineStateCache() {
		if (!fineStateCacheInitialized) {
			//this.fineStateCache.add(getFluid().getSource(false));
			this.fineStateCache.add(getFluid().getFlowing(8, true).setValue(Util.FINE_LEVEL, Integer.valueOf(Util.FINE_LEVEL_MAX)));
			for (int i = 1; i < Util.FINE_LEVEL_MAX; ++i) {
				int realFineLevel = Util.FINE_LEVEL_MAX - i;
				int realLevel = Math.max(1, realFineLevel / Util.LEVEL_MULTIPLIER);
				this.fineStateCache.add(getFluid().getFlowing(realLevel, false).setValue(Util.FINE_LEVEL, realFineLevel));
			}
			this.fineStateCache.add(getFluid().getSource(false));
			//this.fineStateCache.add(getFluid().getFlowing(8, true).setValue(Util.FINE_LEVEL, Integer.valueOf(Util.FINE_LEVEL_MAX)));
			// TODO
			fineStateCacheInitialized = true;
		}
	}*/
	
	@Overwrite
	public FluidState getFluidState(BlockState state) {
		int level = state.getValue(LEVEL);
		int fineLevel = state.getValue(Util.FINE_LEVEL);
		Log.info("Getting fluid state; level is ", level, ", fine ", fineLevel);
		//if (!fluidStateCacheInitialized) initFluidStateCache();
		if (!fineStateCacheInitialized) initFineStateCache();
		int idx = Math.min(level, 8);
		if (idx == 0) {
			return this.fineStateCache.get(0);
		}
		if (idx == 8) {
			return this.fineStateCache.get(Util.FINE_LEVEL_MAX);
		}
		// works: return getFluid().getFlowing(8 - idx, false).setValue(Util.FINE_LEVEL, 80 - fineLevel);
		return this.fineStateCache.get(80 - fineLevel);
		//return this.stateCache.get(idx);
	}

	protected synchronized void initFineStateCache() {
		if (!fineStateCacheInitialized) {
			this.fineStateCache = new ArrayList<FluidState>();
			this.fineStateCache.add(getFluid().getSource(false));
			for (int fineLevel = 1; fineLevel < Util.FINE_LEVEL_MAX; ++fineLevel) {
				//int realFineLevel = Util.FINE_LEVEL_MAX - i;
				//int realLevel = Math.max(1, realFineLevel / Util.LEVEL_MULTIPLIER);
				//this.fineStateCache.add(getFluid().getFlowing(realLevel, false).setValue(Util.FINE_LEVEL, realFineLevel));
				int level = Math.max(1, fineLevel / Util.LEVEL_MULTIPLIER);
				this.fineStateCache.add(getFluid().getFlowing(level, false).setValue(Util.FINE_LEVEL, fineLevel));
			}
			this.fineStateCache.add(getFluid().getFlowing(8, true));
			fineStateCacheInitialized = true;
		}
	}

	@Shadow private boolean fluidStateCacheInitialized;
	private boolean fineStateCacheInitialized;

	@Shadow private final List<FluidState> stateCache;
	private List<FluidState> fineStateCache;

	@Shadow public FlowingFluid getFluid() {
		return null;
	}

	/*@Shadow public abstract Fluid getFlowing();

	public FluidState getFlowing(int p_207207_1_, boolean p_207207_2_) {
		return this.getFlowing().defaultFluidState().setValue(LEVEL, Integer.valueOf(p_207207_1_)).setValue(FALLING, Boolean.valueOf(p_207207_2_));
	}*/
}