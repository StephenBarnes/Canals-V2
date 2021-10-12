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
import net.minecraft.fluid.WaterFluid;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;

@Mixin(FlowingFluidBlock.class)
public abstract class FlowingFluidBlockMixin2 extends Block {

	public FlowingFluidBlockMixin2(Properties p_i48440_1_) {
		super(p_i48440_1_);
		this.stateCache = null;
		// TODO Auto-generated constructor stub
	}

	@Shadow public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;
	// block uses .LEVEL (0-15), FlowingFluid uses .LEVEL_FLOWING (1-8), which is called LEVEL inside FlowingFluid



	/*@Inject(method = "<init>",
			at = @At("TAIL"))
	public void FlowingFluidBlock(FlowingFluid p_i49014_1_, AbstractBlock.Properties p_i49014_2_,
			CallbackInfo ci) {
		Log.info("Creating new block with level(0-15) 0");
	}*/

	/*@Overwrite
	public FluidState getFluidState(BlockState p_204507_1_) {
		int i = p_204507_1_.getValue(LEVEL);
		if (!fluidStateCacheInitialized) initFluidStateCache();
		FluidState cached = this.stateCache.get(Math.min(i, 8));
		Log.info("block.getFluidState called, level is ", i, " so returning a fluidState with level(0-15) ", cached.getValue(LEVEL));
		return cached;
	}*/

	/*@Inject(method = "createBlockStateDefinition",
			at = @At("TAIL"))
	protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> p_206840_1_,
			CallbackInfo ci) {
		p_206840_1_.add(Util.FINE_LEVEL);
	}

	@Inject(method = "<init>",
			at = @At("TAIL"))
	public void FlowingFluidBlock(FlowingFluid p_i49014_1_, AbstractBlock.Properties p_i49014_2_,
			CallbackInfo ci) {
		Log.info("Creating new block SABB with fine level 0 and level 0");
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(LEVEL, Integer.valueOf(0))
				.setValue(Util.FINE_LEVEL, Integer.valueOf(0))); // TODO necessary?
	}*/
	
	@Inject(method = "createBlockStateDefinition",
			at = @At("TAIL"))
	protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> p_206840_1_,
			CallbackInfo ci) {
		p_206840_1_.add(Util.FINE_LEVEL);
	}

	@Shadow private synchronized void initFluidStateCache() {
	}

	@Shadow public FlowingFluid getFluid() {
		return null;
	}

	/*@Overwrite
	public FluidState getFluidState(BlockState state) {
		Log.info("Getting fluid state");
		int level = state.getValue(LEVEL);
		if (!fluidStateCacheInitialized) initFluidStateCache();
		return this.stateCache.get(Math.min(level, 8));
	}*/ /*
	Yes, vanilla fetches fluid state very frequently, maybe 1x/tick, even if it's just 1 source and 1 flowing.
	Vanilla also fetches very frequently for 1 source block, if you're looking at it, or roughly 1-2x/second (ie much slower) for 1 source block you're not looking at.
	Same for my modified getFluidState, as of right now
		*/
	
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
	

	@Shadow private boolean fluidStateCacheInitialized;
	private boolean fineStateCacheInitialized;

	@Shadow private final List<FluidState> stateCache;
	private List<FluidState> fineStateCache;

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

}