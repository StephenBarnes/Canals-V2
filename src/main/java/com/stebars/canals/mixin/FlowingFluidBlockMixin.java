package com.stebars.canals.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.stebars.canals.Util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;

@Mixin(FlowingFluidBlock.class)
public abstract class FlowingFluidBlockMixin extends Block {

	@Shadow private FlowingFluid fluid;

	public FlowingFluidBlockMixin(Properties p_i48440_1_) {
		super(p_i48440_1_);
		this.stateCache = null;
	}

	@Shadow public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;
	// block uses .LEVEL (0-15), FlowingFluid uses .LEVEL_FLOWING (1-8), which is called LEVEL inside FlowingFluid

	@Inject(method = "createBlockStateDefinition",
			at = @At("TAIL"))
	protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> p_206840_1_,
			CallbackInfo ci) {
		p_206840_1_.add(Util.BLOCK_FINE_LEVEL);
	}

	@Shadow private synchronized void initFluidStateCache() {
	}

	@Shadow public FlowingFluid getFluid() {
		return null;
	}

	@Overwrite
	public FluidState getFluidState(BlockState state) {
		int level = state.getValue(LEVEL);
		int fineLevel = state.getValue(Util.BLOCK_FINE_LEVEL);
		if (!fineStateCacheInitialized) initFineStateCache();
		int idx = Math.min(level, 8);
		if (idx == 0)
			return this.fineStateCache.get(0);
		if (idx == 8)
			return this.fineStateCache.get(Util.FINE_LEVEL_MAX);
		return this.fineStateCache.get(Util.FINE_LEVEL_MAX - fineLevel);
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
				int level = Math.max(1, fineLevel / Util.LEVEL_MULTIPLIER);
				this.fineStateCache.add(getFluid().getFlowing(level, false).setValue(Util.FLUID_FINE_LEVEL, fineLevel));
			}
			this.fineStateCache.add(getFluid().getFlowing(8, true));
			fineStateCacheInitialized = true;
		}
	}

}