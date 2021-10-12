package com.stebars.canalsunblocked.mixin;

import org.jline.utils.Log;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.stebars.canalsunblocked.Util;

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
import net.minecraft.state.Property;
import net.minecraft.state.StateContainer;
import net.minecraft.state.StateHolder;
import net.minecraft.world.IWorldReader;

@Mixin(FluidState.class)
public abstract class FluidStateMixin2 extends StateHolder<Fluid, FluidState> {

//@Implements(@Interface(iface = FluidState.class, prefix = "fluidState$"))
//public abstract class FluidStateMixin2 implements FluidState {

	/*private static final int LEVEL_MULTIPLIER = 10;
	private static final int LEVEL_MAX = 8;
	private static final int FINE_LEVEL_MAX = LEVEL_MAX * LEVEL_MULTIPLIER;
	private static final IntegerProperty FINE_LEVEL = IntegerProperty.create("fine_level", 1, FINE_LEVEL_MAX);*/

	/*public abstract class Flowing extends WaterFluidMixin {

		@Inject(at = @At("RETURN"), method = "createFluidStateDefinition", cancellable = true)
		protected void createFluidStateDefinition(StateContainer.Builder<Fluid, FluidState> p_207184_1_) {
			p_207184_1_.add(FINE_LEVEL);
		}

		@Overwrite
		public int getTickDelay(IWorldReader p_205569_1_) {
			return 1;
		}
	}*/

	protected FluidStateMixin2(Fluid p_i231879_1_, ImmutableMap<Property<?>, Comparable<?>> p_i231879_2_,
			MapCodec<FluidState> p_i231879_3_) {
		super(p_i231879_1_, p_i231879_2_, p_i231879_3_);
		// TODO Auto-generated constructor stub
	}

	/*@Inject(method = "createLegacyBlock",
			at = @At("TAIL"))
	public BlockState fluidState$createLegacyBlock(CallbackInfoReturnable ci) {
		return ((BlockState) ci.getReturnValue()).setValue(Util.FINE_LEVEL, this.getValue(Util.FINE_LEVEL));
	}*/

	@Shadow
	public Fluid getType() {
		return this.owner;
	}


	// TODO check crash here when you break the block beneath a water source block
}