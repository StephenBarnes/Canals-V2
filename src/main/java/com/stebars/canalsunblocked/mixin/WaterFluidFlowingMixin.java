package com.stebars.canalsunblocked.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.Lists;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.WaterFluid;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.world.IWorldReader;

@Mixin(WaterFluid.Flowing.class)
public abstract class WaterFluidFlowingMixin extends WaterFluid {

	/*private static final int LEVEL_MULTIPLIER = 10;
	private static final int LEVEL_MAX = 8;
	private static final int FINE_LEVEL_MAX = LEVEL_MAX * LEVEL_MULTIPLIER;*/
	//private static final IntegerProperty FINE_LEVEL = IntegerProperty.create("fine_level2", 1, 10); // TODO change to fine_level, eliminate the unnecessary one

	/*@Inject(at = @At("RETURN"), method = "createFluidStateDefinition", cancellable = true)
	protected void createFluidStateDefinition(StateContainer.Builder<Fluid, FluidState> p_207184_1_,
			CallbackInfo ci) {
		p_207184_1_.add(FlowingFluidMixin.FINE_LEVEL);
	}*/

	// This is defined in WaterFluid, not WaterFluid.Flowing, so no overwrite necessary, just define
	//public int getTickDelay(IWorldReader p_205569_1_) {
	//	return 1;
	//}
}