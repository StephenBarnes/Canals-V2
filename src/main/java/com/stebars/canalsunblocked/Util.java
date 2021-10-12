package com.stebars.canalsunblocked;

import net.minecraft.state.IntegerProperty;

public class Util {

	public static final int LEVEL_MULTIPLIER = 10;
	public static final int LEVEL_MAX = 8;
	public static final int FINE_LEVEL_MAX = LEVEL_MAX * LEVEL_MULTIPLIER;
	public static IntegerProperty FINE_LEVEL = IntegerProperty.create("fine_level", 0, FINE_LEVEL_MAX);
	public static final int DROP_MULTIPLIER = 5;
		// how much level decreases per update, if there's water here and no adjacent higher water; this is to prevent them iterating like 80 times, subtracting 1 each time
}
