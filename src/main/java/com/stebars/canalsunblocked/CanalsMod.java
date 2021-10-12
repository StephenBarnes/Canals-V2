package com.stebars.canalsunblocked;

import net.minecraft.block.Block;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod(CanalsMod.MOD_ID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CanalsMod {
	public final static String MOD_ID = "canalsunblocked";

	/*public static final FlowingFluid CANAL_WATER = (FlowingFluid) new CanalWater.Source()
			.setRegistryName(new ResourceLocation("minecraft", "water"));
	public static final FlowingFluid FLOWING_CANAL_WATER = (FlowingFluid) new CanalWater.Flowing()
			.setRegistryName(new ResourceLocation("minecraft", "flowing_water"));*/


	public CanalsMod() {
		MinecraftForge.EVENT_BUS.register(this);
		FlowingFluidBlock x;
	}

	/*@SubscribeEvent
	public static void onBlocksRegistry(final RegistryEvent.Register<Block> event) {
	}*/

	/*@SubscribeEvent
	public static void onItemsRegistry(final RegistryEvent.Register<Item> event) {
	}*/

	/*@SubscribeEvent
	public static void onFluidsRegistry(final RegistryEvent.Register<Fluid> event) {
		FlowingFluid f = Fluids.FLOWING_WATER;
		event.getRegistry().registerAll(CANAL_WATER, FLOWING_CANAL_WATER);
	}*/
}
