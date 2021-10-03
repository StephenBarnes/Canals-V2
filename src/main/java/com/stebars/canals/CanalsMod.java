package com.stebars.canals;

import net.minecraft.block.Block;
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
	public final static String MOD_ID = "canals";

	public static final Block CANAL_BLOCK = new CanalBlock()
			.setRegistryName(new ResourceLocation(MOD_ID, "canal"));
	public static final Item CANAL_ITEM = new BlockItem(CANAL_BLOCK,
			new Item.Properties().tab(ItemGroup.TAB_BUILDING_BLOCKS)).setRegistryName(new ResourceLocation(MOD_ID, "canal"));
	//public static final FlowingFluid WATER_IN_CANAL = (FlowingFluid) new WaterInCanal.Source().setRegistryName(new ResourceLocation(MOD_ID, "water_in_canal"));
	//public static final FlowingFluid WATER_IN_CANAL_FLOWING = (FlowingFluid) new WaterInCanal.Flowing().setRegistryName(new ResourceLocation(MOD_ID, "flowing_water_in_canal"));


	public CanalsMod() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public static void onBlocksRegistry(final RegistryEvent.Register<Block> event) {
		event.getRegistry().registerAll(CANAL_BLOCK);
	}

	@SubscribeEvent
	public static void onItemsRegistry(final RegistryEvent.Register<Item> event) {
		event.getRegistry().registerAll(CANAL_ITEM);
	}

	/*@SubscribeEvent
	public static void onFluidsRegistry(final RegistryEvent.Register<Fluid> event) {
		event.getRegistry().registerAll(WATER_IN_CANAL);
		event.getRegistry().registerAll(WATER_IN_CANAL_FLOWING);
	}*/
}
