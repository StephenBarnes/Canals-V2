package com.stebars.canals;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

/* Keeping here, though I don't have config options for now. Also re-enable in main file.
public class OptionsHolder {
	public static class Common {	

		public ConfigValue<Boolean> cropAgeObserver;
		public ConfigValue<Boolean> distanceObserver;

		public Common(ForgeConfigSpec.Builder builder) {
			cropAgeObserver = builder.comment("If true, add crop age observer.")
					.define("cropAgeObserver", true);
			distanceObserver = builder.comment("If true, add distance observer.")
					.define("distanceObserver", true);
		}
	}

	public static final Common COMMON;
	public static final ForgeConfigSpec COMMON_SPEC;

	static { //constructor
		Pair<Common, ForgeConfigSpec> commonSpecPair = new ForgeConfigSpec.Builder().configure(Common::new);
		COMMON = commonSpecPair.getLeft();
		COMMON_SPEC = commonSpecPair.getRight();
	}
}*/