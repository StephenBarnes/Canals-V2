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
import net.minecraft.fluid.WaterFluid;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.world.IWorldReader;

@Mixin(WaterFluid.class)
public abstract class WaterFluidMixin2 extends FlowingFluid {

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

	@Overwrite
	public BlockState createLegacyBlock(FluidState p_204527_1_) {
		Log.info("WaterFluid.createLegacyBlock called, with level(0-15) set to legacy level ", getLegacyLevel(p_204527_1_));
		int legacyLevel = getLegacyLevel(p_204527_1_);
		
		// Note this is the BlockState, not the FluidState, TODO rm comment
		return Blocks.WATER.defaultBlockState()
				.setValue(FlowingFluidBlock.LEVEL, Integer.valueOf(legacyLevel))
				.setValue(Util.FINE_LEVEL, Integer.valueOf(legacyLevel * Util.LEVEL_MULTIPLIER));
	}

	// TODO check crash here when you break the block beneath a water source block
}