package com.example;

/**
 * SkyBlock's own experience table.
 *
 * This used to be a square-root curve -- level = √(xp/50) -- which was tidy,
 * cheap, and not the game. The real table is not a formula at all. It is a list
 * somebody wrote down, and its shape is the whole feel of levelling: the first
 * ten levels cost less than ten thousand XP between them, level 20 alone costs
 * two hundred thousand, and the last stretch to 60 costs more than everything
 * before it put together. A curve cannot imitate that, and a player who has
 * played SkyBlock can tell within about five minutes.
 *
 * Three tables, because the game has three:
 *
 *   the ordinary one   every skill except the three below. 1-60.
 *   Runecrafting       far smaller numbers, and it stops at 25
 *   Social             smaller again, also 25
 *   Dungeoneering      far larger, and it keeps climbing to 50 -- the last
 *                      level costs a hundred and sixteen million
 *
 * The numbers are the cost of each level, not the running total; {@link #total}
 * adds them up. Kept that way round because that is how the wiki lists them and
 * how anybody checking this against the wiki will read it.
 */
public final class SkillXp {
	private SkillXp() {
	}

	/**
	 * What each level costs, for every skill but the three special ones.
	 *
	 * Index 0 is the cost of level 1. Runs to level 60.
	 */
	private static final long[] NORMAL = {
			50, 125, 200, 300, 500, 750, 1_000, 1_500, 2_000, 3_500,
			5_000, 7_500, 10_000, 15_000, 20_000, 30_000, 50_000, 75_000, 100_000, 200_000,
			300_000, 400_000, 500_000, 600_000, 700_000, 800_000, 900_000, 1_000_000, 1_100_000, 1_200_000,
			1_300_000, 1_400_000, 1_500_000, 1_600_000, 1_700_000, 1_800_000, 1_900_000, 2_000_000,
			2_100_000, 2_200_000,
			2_300_000, 2_400_000, 2_500_000, 2_600_000, 2_750_000, 2_900_000, 3_100_000, 3_400_000,
			3_700_000, 4_000_000,
			4_300_000, 4_600_000, 4_900_000, 5_200_000, 5_500_000, 5_800_000, 6_100_000, 6_400_000,
			6_700_000, 7_000_000,
	};

	/** Runecrafting: its own much shallower table, stopping at 25. */
	private static final long[] RUNECRAFTING = {
			50, 100, 125, 160, 200, 250, 315, 400, 500, 625,
			785, 1_000, 1_250, 1_575, 1_970, 2_470, 3_090, 3_870, 4_840, 6_050,
			7_560, 9_450, 11_810, 14_760, 18_450,
	};

	/** Social: shallower still. Also stops at 25. */
	private static final long[] SOCIAL = {
			50, 100, 150, 250, 500, 750, 1_000, 1_250, 1_500, 2_000,
			2_500, 3_000, 3_750, 4_500, 6_000, 8_000, 10_000, 12_500, 15_000, 20_000,
			25_000, 30_000, 35_000, 40_000, 50_000,
	};

	/**
	 * Dungeoneering, which is the Catacombs table and is on a different scale
	 * entirely -- level 50 costs more than every other skill in the game
	 * combined. That is not a mistake in the numbers; it is what Catacombs 50
	 * means.
	 */
	private static final long[] DUNGEONEERING = {
			50, 75, 110, 160, 230, 330, 470, 670, 950, 1_340,
			1_890, 2_665, 3_760, 5_260, 7_380, 10_300, 14_400, 20_000, 27_600, 38_000,
			52_500, 71_500, 97_000, 132_000, 180_000, 243_000, 328_000, 445_000, 600_000, 800_000,
			1_065_000, 1_410_000, 1_900_000, 2_500_000, 3_300_000, 4_300_000, 5_600_000, 7_200_000,
			9_200_000, 12_000_000,
			15_000_000, 19_000_000, 24_000_000, 30_000_000, 38_000_000, 48_000_000, 60_000_000,
			75_000_000, 93_000_000, 116_250_000,
	};

	/** Which table a skill uses. */
	private static long[] tableFor(String skill) {
		if (Skills.RUNECRAFTING.equals(skill)) {
			return RUNECRAFTING;
		}
		if (Skills.SOCIAL.equals(skill)) {
			return SOCIAL;
		}
		if (Skills.DUNGEONEERING.equals(skill)) {
			return DUNGEONEERING;
		}
		return NORMAL;
	}

	/**
	 * The level this much XP buys in this skill.
	 *
	 * Walks the table adding up costs. Fifty-odd steps at worst, and it is only
	 * ever called when something needs displaying or a level-up is being
	 * checked, so there is nothing to gain by being clever about it.
	 */
	public static int levelFor(String skill, long xp) {
		long[] table = tableFor(skill);
		long spent = 0;
		for (int level = 0; level < table.length; level++) {
			spent += table[level];
			if (xp < spent) {
				return level;
			}
		}
		return table.length;
	}

	/** Total XP needed to reach a level in this skill, from nothing. */
	public static long total(String skill, int level) {
		long[] table = tableFor(skill);
		long sum = 0;
		for (int at = 0; at < Math.min(level, table.length); at++) {
			sum += table[at];
		}
		return sum;
	}

	/** XP still to go before the next level. Zero once a skill is maxed. */
	public static long toNext(String skill, long xp) {
		int level = levelFor(skill, xp);
		if (level >= tableFor(skill).length) {
			return 0;
		}
		return total(skill, level + 1) - xp;
	}

	/** How far through the current level you are, 0.0 to 1.0. */
	public static double progress(String skill, long xp) {
		int level = levelFor(skill, xp);
		long[] table = tableFor(skill);
		if (level >= table.length) {
			return 1.0;
		}
		long start = total(skill, level);
		long cost = table[level];
		return cost <= 0 ? 1.0 : Math.min(1.0, (double) (xp - start) / cost);
	}

	/** The highest level this skill's table goes to. */
	public static int ceiling(String skill) {
		return tableFor(skill).length;
	}
}
