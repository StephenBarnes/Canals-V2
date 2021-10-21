package com.stebars.canals;

import net.minecraft.block.Block;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import virtuoel.statement.api.StateRefresher;


@Mod(CanalsMod.MOD_ID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CanalsMod {
	public final static String MOD_ID = "canals";

	public CanalsMod() {
		MinecraftForge.EVENT_BUS.register(this);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::injectFineLevel);
		StateRefresher.INSTANCE.reorderBlockStates();
	}

	public void injectFineLevel(FMLCommonSetupEvent e) {
		for (Block block : Registry.BLOCK) {
			if (block instanceof FlowingFluidBlock && !((FlowingFluidBlock) block).getFluid().isSource(block.defaultBlockState().getFluidState()))
				StateRefresher.INSTANCE.addBlockProperty(block, Util.BLOCK_FINE_LEVEL, 11);
		}
	}
}
