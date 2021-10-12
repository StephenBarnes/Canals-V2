package com.stebars.canals;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;


@Mod(CanalsMod.MOD_ID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CanalsMod {
	public final static String MOD_ID = "canals";

	public CanalsMod() {
		MinecraftForge.EVENT_BUS.register(this);
	}
}
