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
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.LavaFluid;
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
public abstract class FlowingFluidMixin extends Fluid {

	/*
	 * So, state rn:
	 * Flowing on polished stone only works if it flows from elsewhere onto that block. Probably because that causes it to actually get fineLevel 80
	 * However, falling creating fineLevel 80 makes it effectively a source block -- need to fix, check how vanilla does it
	 * Buckets don't work
	 * "getFluidState returning level 3 and fine level 58" -- should never happen
	 * 
	 * It's not flowing past 19 -- so, I'm checking for level=1 somewhere, instead of level<=0
	 * Stuff is being called too often; it's calling getFlowing too often, from pos 2 of block.getFluidState
	 */

	@Shadow public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_FLOWING; // 1-8, reversed
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
		p_207184_1_.add(FALLING).add(Util.FINE_LEVEL);
	}

	@Overwrite
	public FluidState getFlowing(int level, boolean falling) {
		//Log.info("called getFlowing, level ", level, " falling ", falling);
		Log.info("WARNING: called getFlowing without setting fineLevel");
		return this.getFlowing().defaultFluidState()
				.setValue(LEVEL, Integer.valueOf(level)) // this is 1-8 and reversed
				.setValue(FALLING, Boolean.valueOf(falling))
				.setValue(Util.FINE_LEVEL, Integer.valueOf(Util.LEVEL_MULTIPLIER * level));
	}

	// New method
	public FluidState getFlowing(int level, int fineLevel, boolean falling) {
		Log.info("called getFlowing with fine level ", fineLevel, ", level ", level, " falling ", falling);
		return this.getFlowing().defaultFluidState()
				.setValue(LEVEL, Integer.valueOf(level))
				.setValue(FALLING, Boolean.valueOf(falling))
				.setValue(Util.FINE_LEVEL, Integer.valueOf(fineLevel));
	}

	@Overwrite
	protected FluidState getNewLiquid(IWorldReader world, BlockPos pos, BlockState state) {
		Log.info("getNewLiquid called at pos ", pos);
		FluidState fluidStateHere = state.getFluidState();
		if (getFluid().isSource(fluidStateHere)) {
			Log.info("getNewLiquid called on a source block, at ", pos, ", this shouldn't happen I think");
			return fluidStateHere;
		} else {
			Log.info("getNewLiquid called on non-source block at ", pos);
		}
		if (!fluidStateHere.isEmpty()) {
			Log.info(pos, " - fluid state has amount ", fluidStateHere.getAmount(), " and level ", fluidStateHere.getValue(LEVEL));
			/*if (fluidStateHere.getAmount() == 8 && fluidStateHere.getValue(Util.FINE_LEVEL) != 80)
				return fluidStateHere.setValue(Util.FINE_LEVEL, 80); // hack
			if (fluidStateHere.isSource() || fluidStateHere.getAmount() == 8)
				return fluidStateHere;
			if (fluidStateHere.getValue(Util.FINE_LEVEL) == 80) {
				Log.info(pos, " - somehow not a source, but with fine level 80");
				return fluidStateHere;
			}*/
		}

		int maxLevel = 0; // Stores highest fluid level found in 4 surrounding spots
		int maxFineLevel = 0; // Added, stores highest fine level in 4 surrounding spots
		int j = 0;

		// Iterate over the 4 directions it could flow
		// For each direction, TODO
		for(Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos blockpos = pos.relative(direction);
			BlockState blockstate = world.getBlockState(blockpos);
			FluidState fluidstate = blockstate.getFluidState();
			if (fluidstate.getType().isSame(this) && this.canPassThroughWall(direction, world, pos, state, blockpos, blockstate)) {
				if (fluidstate.isSource() && net.minecraftforge.event.ForgeEventFactory.canCreateFluidSource(world, blockpos, blockstate, this.canConvertToSource())) {
					++j;
				}

				maxLevel = Math.max(maxLevel, fluidstate.getAmount());
				int fineLevelS = getFineLevelAt(fluidstate);
				Log.info("At pos ", blockpos, " , detected fine level of: ", fineLevelS);
				maxFineLevel = Math.max(maxFineLevel, getFineLevelAt(fluidstate));
			}
		}

		if (j >= 2) {
			BlockState blockstate1 = world.getBlockState(pos.below());
			Log.info("getNewLiquid calling getFluidState, pos 2");
			FluidState fluidstate1 = blockstate1.getFluidState();
			if (blockstate1.getMaterial().isSolid() || this.isSourceBlockOfThisType(fluidstate1)) {
				return this.getSource(false);
			}
		}

		BlockPos blockpos1 = pos.above();
		BlockState blockstate2 = world.getBlockState(blockpos1);
		Log.info("getNewLiquid calling getFluidState, pos 3");
		FluidState fluidstate2 = blockstate2.getFluidState();
		if (!fluidstate2.isEmpty() && fluidstate2.getType().isSame(this) && this.canPassThroughWall(Direction.UP, world, pos, state, blockpos1, blockstate2)) {
			Log.info("getFlowing(8, true) because it's falling onto here");
			return this.getFlowing(8, true);
		} else {
			// If there's a canal block below, we subtract from fine level instead of normal level
			Block blockBelow = world.getBlockState(pos.below()).getBlock();
			if (blockBelow == Blocks.POLISHED_ANDESITE) {
				Log.info("!!! ", pos, " - it's polished andesite");
				int newFineLevel;
				if ((fluidStateHere.hasProperty(Util.FINE_LEVEL) && (maxFineLevel <= fluidStateHere.getValue(Util.FINE_LEVEL))))
					newFineLevel = maxFineLevel - this.getDropOff(world) * Util.DROP_MULTIPLIER; // water level drops faster, to prevent iterating MULTIPLIER * MAX_LEVEL times, decreasing by 1 each time
				else
					newFineLevel = maxFineLevel - this.getDropOff(world);

				int newLevel = newFineLevel <= 0 ? 0 : Math.max(1, newFineLevel / Util.LEVEL_MULTIPLIER);
				Log.info(pos, " - max surrounding fine level ", maxFineLevel, " so new fine level ", newFineLevel, " and new level ", newLevel);
				return newLevel <= 0 ? Fluids.EMPTY.defaultFluidState() : this.getFlowing(newLevel, newFineLevel, false);
			} else {
				Log.info(pos, " - not polished andesite");
				int newLevel = maxLevel - this.getDropOff(world);
				int newFineLevel = newLevel * Util.LEVEL_MULTIPLIER; // TODO remove
				Log.info(pos, " - max surrounding level ", maxLevel, " so new level ", newLevel);
				return newLevel <= 0 ? Fluids.EMPTY.defaultFluidState() : this.getFlowing(newLevel, newFineLevel, false);
				// TODO pretty sure these are the lines that are calling getFlowing every tick
			}
		}
	}
	// TODO somehow broke picking water up with a bucket, need to fix

	public int getFineLevelAt(FluidState state) {
		if (state.isSource()) {
			Log.info("asked for fine level at source block, so returning ", Util.FINE_LEVEL_MAX);
			//return Util.FINE_LEVEL_MAX;
			return 0;
		}
		if (state.isEmpty())
			return 0;
		try {
			return state.getValue(Util.FINE_LEVEL);
		} catch (Exception e) {
			// Will be necessary for modded fluids I think
			Log.info("could not get fine level, returning amount * level multiplier: ", e);
			return state.getAmount() * Util.LEVEL_MULTIPLIER;
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

	// Necessary to overwrite this because it does a "fluidstate.equals(fluidstate2)" check which depends on the cache, I think
	@Overwrite
	public void tick(World world, BlockPos pos, FluidState fluidState) {
		if ((fluidState.getType() instanceof WaterFluid.Source) || fluidState.getType() instanceof LavaFluid.Source) {
			Log.info("It's instance of water(orLava)fluid.source, so just spread+return; has .isSource() == ", fluidState.isSource());
			this.spread(world, pos, fluidState);
			return;
		}
		if (!fluidState.isSource()) { // TODO the amount==8 thing is a hack, should not be necessary, but for some reason isSource returns false for water source blocks
			Log.info("Pos ", pos, " IS NOT SOURCE, ticking"); // NB TODO: here is the error, it's updating source blocks
			FluidState newFluidState = this.getNewLiquid(world, pos, world.getBlockState(pos));
			if (!newFluidState.isEmpty())
				Log.info("GetNewLiquid returned a fluidstate with fine level ", newFluidState.getValue(Util.FINE_LEVEL));
			int i = this.getSpreadDelay(world, pos, fluidState, newFluidState);
			if (newFluidState.isEmpty()) {
				fluidState = newFluidState;
				world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			} else {
				boolean eitherIsSource = newFluidState.isSource() || fluidState.isSource();
				boolean bothSources = newFluidState.isSource() && fluidState.isSource();
				boolean nonSourceDifferences = (!eitherIsSource && (
						(getFineLevelAt(newFluidState) != getFineLevelAt(fluidState)) || (newFluidState.getValue(LEVEL) != fluidState.getValue(LEVEL))));
				if (nonSourceDifferences || (eitherIsSource && !bothSources)) {
					fluidState = newFluidState;
					BlockState blockstate = newFluidState.createLegacyBlock();
					if (!newFluidState.isSource()) {
						blockstate.setValue(Util.FINE_LEVEL, newFluidState.getValue(Util.FINE_LEVEL)); // TODO is this necessary?
						Log.info("tick calling getFluidState");
						blockstate.getFluidState().setValue(Util.FINE_LEVEL, newFluidState.getValue(Util.FINE_LEVEL));
					}
					world.setBlock(pos, blockstate, 2);
					world.getLiquidTicks().scheduleTick(pos, newFluidState.getType(), i);
					world.updateNeighborsAt(pos, blockstate.getBlock());
				}
			}
		} else {
			Log.info("SABB not ticking source block at ", pos);
			Log.info(".isSource() returned: ", fluidState.isSource(), " and level is ", fluidState.getValue(LEVEL), " and falling is ", fluidState.getValue(FALLING));
		}
		this.spread(world, pos, fluidState);
	}


	@Shadow protected void spread(IWorld p_205575_1_, BlockPos p_205575_2_, FluidState p_205575_3_) {}


	/*@Overwrite
	public FluidState getSource(boolean p_207204_1_) {
		return this.getSource().defaultFluidState().setValue(FALLING, Boolean.valueOf(p_207204_1_))
				.setValue(Util.FINE_LEVEL, Integer.valueOf(Util.FINE_LEVEL_MAX));
	}*/ // Source doesn't have FINE_LEVEL, just like it doesn't have LEVEL, so we shouldn't need to set it here
	@Shadow public FluidState getSource(boolean bool) {
		return this.getSource().defaultFluidState().setValue(Util.FINE_LEVEL, 0).setValue(FALLING, Boolean.valueOf(bool));
			// TODO setting fine level to 0 here is probably unnecessary, remove it
	}

	@Shadow public abstract Fluid getSource();


	/*@Overwrite
	public float getHeight(FluidState p_215662_1_, IBlockReader p_215662_2_, BlockPos p_215662_3_) {
		return hasSameAbove(p_215662_1_, p_215662_2_, p_215662_3_) ? 1.0F : p_215662_1_.getOwnHeight();
	}*/

	@Overwrite
	public float getOwnHeight(FluidState p_223407_1_) {
		int fineLevel = p_223407_1_.getValue(Util.FINE_LEVEL);
		if (fineLevel == 0) fineLevel = Util.FINE_LEVEL_MAX; // TODO this should not be necessary! ugh
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

	// This is necessary so that it doesn't stop spreading at fineLevel 19, bc level is 1 so it thinks it can't spread further
	@Overwrite
	private void spreadToSides(IWorld p_207937_1_, BlockPos p_207937_2_, FluidState p_207937_3_, BlockState p_207937_4_) {
		int dropOff = this.getDropOff(p_207937_1_);
		int nextLevel = p_207937_3_.getAmount() - dropOff;
		int nextFineLevel = p_207937_3_.getValue(Util.FINE_LEVEL) - dropOff;
		if (p_207937_3_.getValue(FALLING)) {
			nextLevel = 7;
		}

		if (nextFineLevel > 0) {
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
	protected void spreadTo(IWorld p_205574_1_, BlockPos p_205574_2_, BlockState p_205574_3_, Direction p_205574_4_, FluidState p_205574_5_) {
	}

	@Shadow
	protected boolean canSpreadTo(IBlockReader p_205570_1_, BlockPos p_205570_2_, BlockState p_205570_3_, Direction p_205570_4_, BlockPos p_205570_5_, BlockState p_205570_6_, FluidState p_205570_7_, Fluid p_205570_8_) {
		return false;
	}

	@Shadow
	protected Map<Direction, FluidState> getSpread(IWorldReader p_205572_1_, BlockPos p_205572_2_, BlockState p_205572_3_) {
		return new HashMap<Direction, FluidState>();
	}

}