package com.stebars.canals;

import net.minecraft.state.IntegerProperty;

public class Util {

	public static final int FINE_LEVELS = 12;
		// fine level property will be 0 to FINE_LEVELS-1
	public static final int LEVEL_MAX = 8;
	public static final int TOTAL_LEVEL_MAX = LEVEL_MAX * FINE_LEVELS - 1;
	public static final int TOTAL_LEVEL_MIN = FINE_LEVELS;
		// minimum is level 1, fine level 0
	public static IntegerProperty FLUID_FINE_LEVEL = IntegerProperty.create("canal_fluid_level", 0, FINE_LEVELS - 1);
	public static IntegerProperty BLOCK_FINE_LEVEL = IntegerProperty.create("canal_block_level", 0, FINE_LEVELS - 1);
	public static final int DROP_MULTIPLIER = 10;
		// how much level decreases per update, if there's water here and no adjacent higher water
		// this is to prevent groups of 3+ flowing water blocks from iterating ~TOTAL_LEVEL_MAX times, subtracting 1 or 2 from total level each time
	public static final int HORIZONTAL_SCAN_LENGTH = 3;
		// how wide the canal can be, excluding walls
	
	public static int getTotalLevel(int level, int fineLevel) {
		return level * FINE_LEVELS + fineLevel;
	}
	
	public static int totalToFine(int totalLevel) {
		return Math.floorMod(totalLevel, FINE_LEVELS);
	}
	
	public static int totalToLevel(int totalLevel) {
		return Math.floorDiv(totalLevel, FINE_LEVELS);
	}
}
