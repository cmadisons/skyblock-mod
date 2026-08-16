#!/usr/bin/env python3
"""Builds every SkyBlock item, block and NPC this mod adds.

Everything the mod adds that Minecraft does not already have is described in
this one file, and everything else is made from it:

    src/main/java/com/example/Content.java     the tables the mod registers from
    assets/skyblocks/items/*.json              one item definition each
    assets/skyblocks/models/item/*.json        what each one looks like
    assets/skyblocks/blockstates/*.json        the placeable blocks
    assets/skyblocks/models/block/*.json
    assets/skyblocks/textures/**.png           gemstones, ores and the NPC token
    assets/skyblocks/lang/en_us.json           every name

Two rules keep it honest:

  * an item that looks like something already in Minecraft borrows that
    texture rather than shipping a near-identical copy of it -- Enchanted
    Coal is coal with a glint on it, exactly as it is in the real game;
  * anything with no counterpart at all -- the twelve gemstones, the Dwarven
    ores -- gets a texture drawn here, pixel by pixel, because there is
    nothing to borrow.

Every borrowed texture name is checked against the Minecraft jar before
anything is written, so a typo is a build error rather than a purple-and-black
square you only find in-game. Point VANILLA_JAR at another version and it will
tell you what moved.

Run from the project root:  python3 tools/make_content.py
"""

import glob
import json
import os
import struct
import sys
import zlib

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, "src/main/resources/assets/skyblocks")
JAVA = os.path.join(ROOT, "src/main/java/com/example")

# Used only to check that a borrowed texture really exists. Missing is fine --
# the check is skipped with a warning rather than stopping the build.
VANILLA_JAR = os.path.expanduser(
    "~/.gradle/caches/fabric-loom/26.1.2/minecraft-client.jar")


# ---------------------------------------------------------------- the tables

# Rarity is the game's own ladder. It sets the colour of the name and the line
# at the bottom of the tooltip, the same way it does on Hypixel.
RARITIES = ["COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC", "SPECIAL"]

# --- materials -------------------------------------------------------------
# (id, display name, rarity, how it's drawn, what it borrows, description)
#
# "item"  -- a flat sprite, textures/item/<ref>.png
# "block" -- looks like a block, models/block/<ref>.json
# "own"   -- a texture drawn further down this file
#
# Enchanted items are the backbone of SkyBlock's economy: 160 of a thing
# compressed into one. They are marked glinting further down by name.

ENCHANTED = [
    # --- mining ------------------------------------------------------------
    ("enchanted_cobblestone", "Enchanted Cobblestone", "UNCOMMON", "block", "cobblestone"),
    ("enchanted_stone", "Enchanted Stone", "UNCOMMON", "block", "stone"),
    ("enchanted_coal", "Enchanted Coal", "UNCOMMON", "item", "coal"),
    ("enchanted_charcoal", "Enchanted Charcoal", "UNCOMMON", "item", "charcoal"),
    ("enchanted_coal_block", "Enchanted Block of Coal", "RARE", "block", "coal_block"),
    ("enchanted_iron", "Enchanted Iron", "UNCOMMON", "item", "iron_ingot"),
    ("enchanted_iron_block", "Enchanted Iron Block", "RARE", "block", "iron_block"),
    ("enchanted_gold", "Enchanted Gold", "UNCOMMON", "item", "gold_ingot"),
    ("enchanted_gold_block", "Enchanted Gold Block", "RARE", "block", "gold_block"),
    ("enchanted_diamond", "Enchanted Diamond", "UNCOMMON", "item", "diamond"),
    ("enchanted_diamond_block", "Enchanted Diamond Block", "RARE", "block", "diamond_block"),
    ("enchanted_emerald", "Enchanted Emerald", "UNCOMMON", "item", "emerald"),
    ("enchanted_emerald_block", "Enchanted Emerald Block", "RARE", "block", "emerald_block"),
    ("enchanted_lapis_lazuli", "Enchanted Lapis Lazuli", "UNCOMMON", "item", "lapis_lazuli"),
    ("enchanted_lapis_block", "Enchanted Lapis Block", "RARE", "block", "lapis_block"),
    ("enchanted_redstone", "Enchanted Redstone", "UNCOMMON", "item", "redstone"),
    ("enchanted_redstone_block", "Enchanted Redstone Block", "RARE", "block", "redstone_block"),
    ("enchanted_quartz", "Enchanted Quartz", "UNCOMMON", "item", "quartz"),
    ("enchanted_quartz_block", "Enchanted Quartz Block", "RARE", "block", "quartz_block"),
    ("enchanted_obsidian", "Enchanted Obsidian", "UNCOMMON", "block", "obsidian"),
    ("enchanted_glowstone_dust", "Enchanted Glowstone Dust", "UNCOMMON", "item", "glowstone_dust"),
    ("enchanted_glowstone", "Enchanted Glowstone", "RARE", "block", "glowstone"),
    ("enchanted_flint", "Enchanted Flint", "UNCOMMON", "item", "flint"),
    ("enchanted_gravel", "Enchanted Gravel", "UNCOMMON", "block", "gravel"),
    ("enchanted_sand", "Enchanted Sand", "UNCOMMON", "block", "sand"),
    ("enchanted_clay_ball", "Enchanted Clay Ball", "UNCOMMON", "item", "clay_ball"),
    ("enchanted_brick", "Enchanted Brick", "UNCOMMON", "item", "brick"),
    ("enchanted_endstone", "Enchanted End Stone", "UNCOMMON", "block", "end_stone"),
    ("enchanted_netherrack", "Enchanted Netherrack", "UNCOMMON", "block", "netherrack"),
    ("enchanted_nether_bricks", "Enchanted Nether Bricks", "RARE", "block", "nether_bricks"),
    ("enchanted_ice", "Enchanted Ice", "UNCOMMON", "block", "ice"),
    ("enchanted_packed_ice", "Enchanted Packed Ice", "RARE", "block", "packed_ice"),
    ("enchanted_snow_block", "Enchanted Snow Block", "UNCOMMON", "block", "snow_block"),
    ("enchanted_prismarine_shard", "Enchanted Prismarine Shard", "UNCOMMON", "item", "prismarine_shard"),
    ("enchanted_prismarine_crystals", "Enchanted Prismarine Crystals", "UNCOMMON", "item", "prismarine_crystals"),
    ("enchanted_sponge", "Enchanted Sponge", "RARE", "block", "sponge"),

    # --- farming -----------------------------------------------------------
    ("enchanted_wheat", "Enchanted Wheat", "UNCOMMON", "item", "wheat"),
    ("enchanted_hay_block", "Enchanted Hay Bale", "RARE", "block", "hay_block"),
    ("enchanted_seeds", "Enchanted Seeds", "UNCOMMON", "item", "wheat_seeds"),
    ("enchanted_bread", "Enchanted Bread", "UNCOMMON", "item", "bread"),
    ("enchanted_carrot", "Enchanted Carrot", "UNCOMMON", "item", "carrot"),
    ("enchanted_golden_carrot", "Enchanted Golden Carrot", "RARE", "item", "golden_carrot"),
    ("enchanted_potato", "Enchanted Potato", "UNCOMMON", "item", "potato"),
    ("enchanted_baked_potato", "Enchanted Baked Potato", "RARE", "item", "baked_potato"),
    ("enchanted_poisonous_potato", "Enchanted Poisonous Potato", "RARE", "item", "poisonous_potato"),
    ("enchanted_melon", "Enchanted Melon", "UNCOMMON", "item", "melon_slice"),
    ("enchanted_melon_block", "Enchanted Melon Block", "RARE", "block", "melon"),
    ("enchanted_pumpkin", "Enchanted Pumpkin", "UNCOMMON", "block", "pumpkin"),
    ("enchanted_cactus_green", "Enchanted Cactus Green", "UNCOMMON", "item", "green_dye"),
    ("enchanted_cactus", "Enchanted Cactus", "RARE", "block", "cactus"),
    ("enchanted_sugar", "Enchanted Sugar", "UNCOMMON", "item", "sugar"),
    ("enchanted_sugar_cane", "Enchanted Sugar Cane", "RARE", "item", "sugar_cane"),
    ("enchanted_cocoa", "Enchanted Cocoa Bean", "UNCOMMON", "item", "cocoa_beans"),
    ("enchanted_cookie", "Enchanted Cookie", "RARE", "item", "cookie"),
    ("enchanted_red_mushroom", "Enchanted Red Mushroom", "UNCOMMON", "block", "red_mushroom"),
    ("enchanted_brown_mushroom", "Enchanted Brown Mushroom", "UNCOMMON", "block", "brown_mushroom"),
    ("enchanted_red_mushroom_block", "Enchanted Red Mushroom Block", "RARE", "block", "red_mushroom_block"),
    ("enchanted_brown_mushroom_block", "Enchanted Brown Mushroom Block", "RARE", "block", "brown_mushroom_block"),
    ("enchanted_mycelium", "Enchanted Mycelium", "RARE", "block", "mycelium"),
    ("enchanted_nether_wart", "Enchanted Nether Wart", "UNCOMMON", "item", "nether_wart"),
    ("enchanted_nether_stalk", "Mutant Nether Wart", "RARE", "item", "nether_wart"),
    ("enchanted_cake", "Enchanted Cake", "RARE", "block", "cake"),

    # --- foraging ----------------------------------------------------------
    ("enchanted_oak_log", "Enchanted Oak Wood", "UNCOMMON", "block", "oak_log"),
    ("enchanted_spruce_log", "Enchanted Spruce Wood", "UNCOMMON", "block", "spruce_log"),
    ("enchanted_birch_log", "Enchanted Birch Wood", "UNCOMMON", "block", "birch_log"),
    ("enchanted_dark_oak_log", "Enchanted Dark Oak Wood", "UNCOMMON", "block", "dark_oak_log"),
    ("enchanted_acacia_log", "Enchanted Acacia Wood", "UNCOMMON", "block", "acacia_log"),
    ("enchanted_jungle_log", "Enchanted Jungle Wood", "UNCOMMON", "block", "jungle_log"),

    # --- combat ------------------------------------------------------------
    ("enchanted_rotten_flesh", "Enchanted Rotten Flesh", "UNCOMMON", "item", "rotten_flesh"),
    ("enchanted_bone", "Enchanted Bone", "UNCOMMON", "item", "bone"),
    ("enchanted_string", "Enchanted String", "UNCOMMON", "item", "string"),
    ("enchanted_spider_eye", "Enchanted Spider Eye", "UNCOMMON", "item", "spider_eye"),
    ("enchanted_gunpowder", "Enchanted Gunpowder", "UNCOMMON", "item", "gunpowder"),
    ("enchanted_slime_ball", "Enchanted Slimeball", "UNCOMMON", "item", "slime_ball"),
    ("enchanted_slime_block", "Enchanted Slime Block", "RARE", "block", "slime_block"),
    ("enchanted_ender_pearl", "Enchanted Ender Pearl", "UNCOMMON", "item", "ender_pearl"),
    ("enchanted_eye_of_ender", "Enchanted Eye of Ender", "RARE", "item", "ender_eye"),
    ("enchanted_blaze_powder", "Enchanted Blaze Powder", "UNCOMMON", "item", "blaze_powder"),
    ("enchanted_blaze_rod", "Enchanted Blaze Rod", "RARE", "item", "blaze_rod"),
    ("enchanted_ghast_tear", "Enchanted Ghast Tear", "RARE", "item", "ghast_tear"),
    ("enchanted_magma_cream", "Enchanted Magma Cream", "UNCOMMON", "item", "magma_cream"),
    ("enchanted_gold_nugget", "Enchanted Gold Nugget", "UNCOMMON", "item", "gold_nugget"),

    # --- animals and fishing ------------------------------------------------
    ("enchanted_leather", "Enchanted Leather", "UNCOMMON", "item", "leather"),
    ("enchanted_raw_beef", "Enchanted Raw Beef", "UNCOMMON", "item", "beef"),
    ("enchanted_cooked_beef", "Enchanted Cooked Beef", "RARE", "item", "cooked_beef"),
    ("enchanted_pork", "Enchanted Pork", "UNCOMMON", "item", "porkchop"),
    ("enchanted_grilled_pork", "Enchanted Grilled Pork", "RARE", "item", "cooked_porkchop"),
    ("enchanted_raw_chicken", "Enchanted Raw Chicken", "UNCOMMON", "item", "chicken"),
    ("enchanted_egg", "Enchanted Egg", "UNCOMMON", "item", "egg"),
    ("enchanted_feather", "Enchanted Feather", "UNCOMMON", "item", "feather"),
    ("enchanted_mutton", "Enchanted Mutton", "UNCOMMON", "item", "mutton"),
    ("enchanted_cooked_mutton", "Enchanted Cooked Mutton", "RARE", "item", "cooked_mutton"),
    ("enchanted_rabbit", "Enchanted Raw Rabbit", "UNCOMMON", "item", "rabbit"),
    ("enchanted_rabbit_foot", "Enchanted Rabbit Foot", "RARE", "item", "rabbit_foot"),
    ("enchanted_rabbit_hide", "Enchanted Rabbit Hide", "UNCOMMON", "item", "rabbit_hide"),
    ("enchanted_raw_fish", "Enchanted Raw Fish", "UNCOMMON", "item", "cod"),
    ("enchanted_raw_salmon", "Enchanted Raw Salmon", "UNCOMMON", "item", "salmon"),
    ("enchanted_clownfish", "Enchanted Clownfish", "RARE", "item", "tropical_fish"),
    ("enchanted_pufferfish", "Enchanted Pufferfish", "RARE", "item", "pufferfish"),
    ("enchanted_ink_sac", "Enchanted Ink Sack", "UNCOMMON", "item", "ink_sac"),
    ("enchanted_water_lily", "Enchanted Lily Pad", "UNCOMMON", "block", "lily_pad"),
]

# Materials with no enchanted-glint story: drops, upgrades, and the things
# minions run on.
SPECIALS = [
    # (id, display name, rarity, kind, ref, description)
    ("super_compactor_3000", "Super Compactor 3000", "EPIC", "block", "piston",
     "Compacts a Minion's output into its enchanted form."),
    ("compactor", "Compactor", "UNCOMMON", "block", "piston",
     "Compacts a Minion's output into blocks."),
    ("dwarven_compactor", "Dwarven Compactor", "EPIC", "block", "piston",
     "Compacts Dwarven ores as they are mined."),
    ("auto_smelter", "Auto Smelter", "RARE", "item", "furnace_minecart",
     "Smelts a Minion's output as it works."),
    ("diamond_spreading", "Diamond Spreading", "RARE", "item", "diamond",
     "A Minion has a chance to produce a diamond alongside its work."),
    ("flycatcher", "Flycatcher", "RARE", "block", "cobweb",
     "Minion mobs drop their heads more often."),
    ("minion_expander", "Minion Expander", "RARE", "block", "obsidian",
     "Widens a Minion's working area by one block."),
    ("budget_hopper", "Budget Hopper", "UNCOMMON", "item", "hopper",
     "Sells a Minion's output for half price."),
    ("enchanted_hopper", "Enchanted Hopper", "RARE", "item", "hopper",
     "Sells a Minion's output at full price."),
    ("corrupt_soil", "Corrupt Soil", "RARE", "block", "soul_sand",
     "Minion mobs spawn corrupted and drop sulphur."),
    ("catalyst", "Catalyst", "EPIC", "item", "redstone",
     "Speeds a Minion up for a limited time."),
    ("super_enchanted_egg", "Super Enchanted Egg", "EPIC", "item", "egg",
     "Minion mobs have a chance to drop extra experience."),
    ("minion_fuel", "Enchanted Lava Bucket", "RARE", "item", "lava_bucket",
     "Never runs out. Minions work 25% faster."),
    ("recombobulator_3000", "Recombobulator 3000", "LEGENDARY", "item", "nether_star",
     "Raises an item's rarity by one step."),
    ("hot_potato_book", "Hot Potato Book", "RARE", "item", "book",
     "+2 Health or +2 Damage on a weapon or a piece of armour."),
    ("fuming_potato_book", "Fuming Potato Book", "EPIC", "item", "enchanted_book",
     "+4 Health or +2 Damage, past the ordinary limit."),
    ("jacobs_ticket", "Jacob's Ticket", "RARE", "item", "paper",
     "Spent at Jacob's Farming Contest."),
    ("booster_cookie", "Booster Cookie", "LEGENDARY", "item", "cookie",
     "Four days of the Cookie Buff."),
    ("god_potion", "God Potion", "LEGENDARY", "item", "potion",
     "Every potion effect in the game at once."),
    ("travel_scroll", "Travel Scroll", "RARE", "item", "paper",
     "Warps you somewhere you have already been."),
    ("summoning_eye", "Summoning Eye", "LEGENDARY", "item", "ender_eye",
     "Eight of these summon the Dragon."),
    ("griffin_feather", "Griffin Feather", "LEGENDARY", "item", "feather",
     "Dropped by a Griffin. Traded for a Griffin pet."),
    ("ancient_claw", "Ancient Claw", "RARE", "item", "quartz",
     "A Revenant Horror drop."),
    ("golden_tooth", "Golden Tooth", "EPIC", "item", "gold_nugget",
     "A Revenant Horror drop."),
    ("revenant_flesh", "Revenant Flesh", "UNCOMMON", "item", "rotten_flesh",
     "A Revenant Horror drop."),
    ("foul_flesh", "Foul Flesh", "RARE", "item", "rotten_flesh",
     "A Revenant Horror drop."),
    ("undead_catalyst", "Undead Catalyst", "EPIC", "item", "redstone",
     "Starts a Revenant Horror hunt."),
    ("tarantula_web", "Tarantula Web", "UNCOMMON", "item", "string",
     "A Tarantula Broodfather drop."),
    ("toxic_arrow_poison", "Toxic Arrow Poison", "RARE", "item", "spider_eye",
     "A Tarantula Broodfather drop."),
    ("spider_catalyst", "Spider Catalyst", "EPIC", "item", "fermented_spider_eye",
     "Starts a Tarantula Broodfather hunt."),
    ("digested_mushrooms", "Digested Mushrooms", "RARE", "block", "red_mushroom",
     "A Tarantula Broodfather drop."),
    ("fly_swatter", "Fly Swatter", "EPIC", "item", "shears",
     "Kills a spider's spawn in one hit."),
    ("wolf_tooth", "Wolf Tooth", "UNCOMMON", "item", "bone_meal",
     "A Sven Packmaster drop."),
    ("hamster_wheel", "Hamster Wheel", "RARE", "item", "minecart",
     "A Sven Packmaster drop."),
    ("red_claw_egg", "Red Claw Egg", "EPIC", "item", "egg",
     "A Sven Packmaster drop."),
    ("grizzly_bait", "Grizzly Bait", "RARE", "item", "rotten_flesh",
     "Starts a Sven Packmaster hunt."),
    ("overflux_capacitor", "Overflux Power Orb", "LEGENDARY", "item", "heart_of_the_sea",
     "Strength and Absorption to everyone standing near you."),
    ("experience_bottle_grand", "Grand Experience Bottle", "RARE", "item", "experience_bottle",
     "Worth 30,000 experience."),
    ("experience_bottle_titanic", "Titanic Experience Bottle", "EPIC", "item", "experience_bottle",
     "Worth 150,000 experience."),
    ("mithril", "Mithril", "UNCOMMON", "item", "prismarine_crystals",
     "Mined in the Dwarven Mines."),
    ("titanium", "Titanium", "RARE", "item", "iron_nugget",
     "Rare in Mithril. The Dwarves want it."),
    ("refined_mithril", "Refined Mithril", "RARE", "item", "prismarine_crystals",
     "Mithril, put through the Forge."),
    ("refined_titanium", "Refined Titanium", "EPIC", "item", "iron_ingot",
     "Titanium, put through the Forge."),
    ("refined_diamond", "Refined Diamond", "EPIC", "item", "diamond",
     "Diamond, put through the Forge."),
    ("treasurite", "Treasurite", "RARE", "item", "amethyst_shard",
     "Found in Mithril deposits."),
    ("hard_stone", "Hard Stone", "COMMON", "item", "flint",
     "The rock the Dwarven Mines are cut from."),
    ("sulphur", "Sulphur", "UNCOMMON", "item", "glowstone_dust",
     "Burned by Corrupt Soil."),
    ("bits", "Bits", "SPECIAL", "item", "amethyst_shard",
     "Earned from Fame and spent in the Community Shop."),
    ("dungeon_key", "Wither Key", "RARE", "block", "tripwire_hook",
     "Opens a Wither Door in the Catacombs."),
    ("skeleton_skull", "Skeleton Grunt Skull", "UNCOMMON", "item", "bone",
     "A Dungeon drop."),
]

# The runes, in three tiers each. A rune is a look rather than a stat -- it
# makes a weapon sparkle, smoke, drip blood or play music -- so what varies
# between them is the colour, which is exactly what the texture below uses.
RUNES = [
    ("music", "Music", (214, 120, 220)),
    ("zombie_slayer", "Zombie Slayer", (86, 148, 78)),
    ("golden", "Golden", (232, 190, 60)),
    ("bloody", "Bloody", (168, 30, 30)),
    ("ice", "Ice", (150, 214, 240)),
    ("enchant", "Enchant", (140, 110, 220)),
    ("hearts", "Hearts", (226, 70, 110)),
    ("late_summer", "Late Summer", (226, 150, 60)),
    ("rainy_day", "Rainy Day", (90, 130, 190)),
    ("lightning", "Lightning", (240, 240, 130)),
    ("spirit", "Spirit", (200, 230, 230)),
    ("end", "End", (120, 90, 180)),
    ("snake", "Snake", (110, 190, 90)),
    ("couture", "Couture", (200, 130, 190)),
    ("sparkling", "Sparkling", (240, 220, 250)),
    ("smoky", "Smoky", (110, 110, 118)),
    ("tidal", "Tidal", (60, 170, 190)),
    ("white_sand", "White Sand", (238, 232, 210)),
    ("meteor", "Meteor", (230, 120, 50)),
    ("dragon", "Dragon", (60, 50, 80)),
    ("clouds", "Clouds", (226, 236, 244)),
    ("rainbow", "Rainbow", (230, 120, 180)),
    ("grand_searing", "Grand Searing", (240, 90, 40)),
    ("jerry", "Jerry", (120, 200, 120)),
    ("pestilence", "Pestilence", (140, 170, 70)),
    ("bite", "Bite", (190, 190, 200)),
    ("blood_2", "Blood", (140, 20, 20)),
    ("hot", "Hot", (240, 140, 40)),
]
RUNE_TIERS = [("i", "I", "UNCOMMON", 0.8), ("ii", "II", "RARE", 1.0),
              ("iii", "III", "EPIC", 1.25)]

# The XP Boost potions: seven skills, three tiers each. Wisdom is a percentage
# on everything that skill earns, and the tiers are the game's own +5/+10/+20.
BOOST_SKILLS = ["Farming", "Mining", "Combat", "Foraging", "Fishing",
                "Enchanting", "Alchemy"]
BOOST_TIERS = [("i", "I", 5, "UNCOMMON"), ("ii", "II", 10, "RARE"),
               ("iii", "III", 20, "EPIC")]

# The twelve gemstones, in five qualities each. These have no counterpart in
# Minecraft at all, so their textures are drawn further down.
GEM_KINDS = [
    ("ruby", "Ruby", (206, 46, 62)),
    ("amethyst", "Amethyst", (150, 72, 200)),
    ("jade", "Jade", (60, 190, 100)),
    ("sapphire", "Sapphire", (70, 140, 230)),
    ("amber", "Amber", (232, 140, 40)),
    ("topaz", "Topaz", (238, 214, 60)),
    ("jasper", "Jasper", (226, 90, 190)),
    ("opal", "Opal", (232, 236, 244)),
    ("aquamarine", "Aquamarine", (80, 214, 214)),
    ("citrine", "Citrine", (160, 104, 48)),
    ("onyx", "Onyx", (44, 42, 52)),
    ("peridot", "Peridot", (150, 220, 60)),
]
GEM_GRADES = [
    ("rough", "Rough", "COMMON"),
    ("flawed", "Flawed", "UNCOMMON"),
    ("fine", "Fine", "RARE"),
    ("flawless", "Flawless", "EPIC"),
    ("perfect", "Perfect", "LEGENDARY"),
]

# --- weapons and tools -----------------------------------------------------
# (id, display, rarity, kind, texture, damage, speed, description)
# kind: "sword" | "bow" | "tool" | "trinket"
GEAR = [
    ("aspect_of_the_end", "Aspect of the End", "RARE", "sword", "diamond_sword", 6.0, -2.4,
     "Ability: Instant Transmission - teleport 8 blocks ahead."),
    ("aspect_of_the_dragons", "Aspect of the Dragons", "LEGENDARY", "sword", "diamond_sword", 11.0, -2.4,
     "Ability: Dragon Rage - a shockwave for 3x damage."),
    ("aspect_of_the_jerry", "Aspect of the Jerry", "COMMON", "sword", "wooden_sword", 0.0, -2.4,
     "Ability: Jerry-chine Gun - fires Jerrys."),
    ("rogue_sword", "Rogue Sword", "COMMON", "sword", "golden_sword", 2.0, -2.0,
     "Ability: Speed Boost - +100 Speed for 30 seconds."),
    ("undead_sword", "Undead Sword", "COMMON", "sword", "stone_sword", 3.0, -2.4,
     "Deals double damage to zombies."),
    ("hunter_knife", "Hunter Knife", "UNCOMMON", "sword", "iron_sword", 3.0, -1.8,
     "Deals double damage to animals."),
    ("raider_axe", "Raider Axe", "EPIC", "sword", "iron_axe", 7.0, -3.0,
     "Grows stronger with every Sea Creature you kill."),
    ("ember_rod", "Ember Rod", "RARE", "sword", "blaze_rod", 5.0, -2.4,
     "Ability: Fire Blast - a cone of flame."),
    ("frozen_scythe", "Frozen Scythe", "RARE", "sword", "diamond_hoe", 6.0, -2.4,
     "Ability: Ice Bolt - slows what it hits."),
    ("leaping_sword", "Leaping Sword", "RARE", "sword", "iron_sword", 5.0, -2.4,
     "Ability: Espresso Shot - leaps you forward."),
    ("silk_edge_sword", "Silk-Edge Sword", "EPIC", "sword", "diamond_sword", 7.0, -2.4,
     "Ability: Silk Edge - +50 Strength on a hit."),
    ("end_sword", "End Sword", "UNCOMMON", "sword", "diamond_sword", 5.0, -2.4,
     "Deals double damage to Endermen."),
    ("ornate_zombie_sword", "Ornate Zombie Sword", "EPIC", "sword", "golden_sword", 8.0, -2.4,
     "Ability: Heal - heals you for a fifth of the damage dealt."),
    ("prismarine_blade", "Prismarine Blade", "UNCOMMON", "sword", "diamond_sword", 4.0, -2.4,
     "Ability: Water Blade - stronger while wet."),
    ("zombie_sword", "Zombie Sword", "RARE", "sword", "golden_sword", 4.0, -2.4,
     "Ability: Heal - drains health from what you hit."),
    ("pigman_sword", "Pigman Sword", "LEGENDARY", "sword", "golden_sword", 10.0, -2.4,
     "Ability: Burning Souls - a burst of flame."),
    ("golem_sword", "Golem Sword", "RARE", "sword", "iron_sword", 6.0, -2.4,
     "Ability: Bone Shield - blocks the next hit."),
    ("emerald_blade", "Emerald Blade", "RARE", "sword", "diamond_sword", 4.0, -2.4,
     "Grows stronger with every coin you carry."),
    ("cleaver", "Cleaver", "UNCOMMON", "sword", "iron_axe", 5.0, -3.0,
     "Slow, but it hits everything in front of you."),
    ("flaming_sword", "Flaming Sword", "UNCOMMON", "sword", "iron_sword", 4.0, -2.4,
     "Sets what it hits alight."),
    ("sword_of_bad_health", "Sword of Bad Health", "COMMON", "sword", "wooden_sword", 8.0, -2.4,
     "Hits hard. Halves your own health."),
    ("bonemerang", "Bonemerang", "EPIC", "trinket", "bone", 0.0, 0.0,
     "Ability: Throw - comes back, hitting everything twice."),
    ("grappling_hook", "Grappling Hook", "UNCOMMON", "trinket", "fishing_rod", 0.0, 0.0,
     "Ability: Grapple - pulls you where you aim."),
    ("stonk", "Stonk", "EPIC", "tool", "wooden_pickaxe", 1.0, -2.8,
     "Ability: Instant Transmission. Mines stone very quickly."),
    ("treecapitator", "Treecapitator", "EPIC", "tool", "golden_axe", 4.0, -3.0,
     "Fells an entire tree in one swing."),
    ("jungle_axe", "Jungle Axe", "UNCOMMON", "tool", "golden_axe", 3.0, -3.0,
     "Fells a small tree in one swing."),
    ("efficient_axe", "Efficient Axe", "COMMON", "tool", "iron_axe", 3.0, -3.0,
     "Cuts wood faster than it has any right to."),
    ("pickonimbus", "Pickonimbus 2000", "LEGENDARY", "tool", "netherite_pickaxe", 3.0, -2.8,
     "Ability: Pickobulus - blasts a sphere of stone away."),
    ("titanium_drill", "Titanium Drill DR-X355", "EPIC", "tool", "diamond_pickaxe", 5.0, -2.8,
     "Runs on Drill Fuel. Cuts through the Dwarven Mines."),
    ("gemstone_gauntlet", "Gemstone Gauntlet", "LEGENDARY", "tool", "golden_pickaxe", 6.0, -2.4,
     "Made for gemstones and nothing else."),
    ("rookie_hoe", "Rookie Hoe", "COMMON", "tool", "wooden_hoe", 0.0, -3.0,
     "Counts the wheat you cut with it."),
    ("prismarine_rod", "Rod of the Sea", "EPIC", "trinket", "fishing_rod", 0.0, 0.0,
     "Ability: Water Blast - hits what you are fishing."),
    ("rod_of_champions", "Rod of Champions", "RARE", "trinket", "fishing_rod", 0.0, 0.0,
     "Sea Creatures come faster."),
    ("rod_of_legends", "Rod of Legends", "LEGENDARY", "trinket", "fishing_rod", 0.0, 0.0,
     "The best rod the Fisherman sells."),
    ("runaans_bow", "Runaan's Bow", "EPIC", "bow", "bow", 0.0, 0.0,
     "Fires three arrows at once."),
    ("hurricane_bow", "Hurricane Bow", "EPIC", "bow", "bow", 0.0, 0.0,
     "Fires five arrows, spread wide."),
    ("mosquito_bow", "Mosquito Bow", "LEGENDARY", "bow", "bow", 0.0, 0.0,
     "Damage rises the lower your health falls."),
    ("explosive_bow", "Explosive Bow", "RARE", "bow", "bow", 0.0, 0.0,
     "Arrows burst on landing."),
    ("end_stone_bow", "End Stone Bow", "RARE", "bow", "bow", 0.0, 0.0,
     "Deals double damage to Endermen."),
    ("savanna_bow", "Savanna Bow", "UNCOMMON", "bow", "bow", 0.0, 0.0,
     "Draws faster than an ordinary bow."),
    ("slime_bow", "Slime Bow", "UNCOMMON", "bow", "bow", 0.0, 0.0,
     "Arrows stick and slow."),
    ("magma_bow", "Magma Bow", "RARE", "bow", "bow", 0.0, 0.0,
     "Arrows arrive burning."),
    ("scorpion_bow", "Scorpion Foil", "EPIC", "bow", "bow", 0.0, 0.0,
     "Poisons what it hits."),
]

# --- armour ----------------------------------------------------------------
# One line per set. The mod makes four pieces out of each.
# (set id, set display, rarity, defence per piece, vanilla look, description)
ARMOUR_SETS = [
    ("leaflet", "Leaflet", "COMMON", 2, "LEATHER", "The first armour anybody wears."),
    ("miners_outfit", "Miner's Outfit", "UNCOMMON", 3, "CHAINMAIL", "Full set: +25 Defence while mining."),
    ("farm_suit", "Farm Suit", "UNCOMMON", 3, "LEATHER", "Full set: crops grow faster around you."),
    ("cheap_tuxedo", "Cheap Tuxedo", "RARE", 0, "LEATHER", "Full set: your health is fixed at 75. Damage is doubled."),
    ("fancy_tuxedo", "Fancy Tuxedo", "EPIC", 0, "LEATHER", "Full set: your health is fixed at 150. Damage is doubled."),
    ("elegant_tuxedo", "Elegant Tuxedo", "LEGENDARY", 0, "LEATHER", "Full set: your health is fixed at 250. Damage is doubled."),
    ("mushroom", "Mushroom", "UNCOMMON", 4, "LEATHER", "Full set: glows in the dark, stronger at night."),
    ("cactus", "Cactus", "UNCOMMON", 5, "LEATHER", "Full set: thorns damage to anything that hits you."),
    ("lapis", "Lapis Armor", "RARE", 6, "IRON", "Full set: +50 Mining experience."),
    ("hardened_diamond", "Hardened Diamond", "RARE", 12, "DIAMOND", "Full set: heavy, and it shows."),
    ("golem", "Golem Armor", "EPIC", 15, "IRON", "Full set: +200 Health."),
    ("ender", "Ender Armor", "EPIC", 14, "DIAMOND", "Full set: doubled in the End."),
    ("miner", "Miner Armor", "RARE", 7, "IRON", "Full set: +100 Defence underground."),
    ("squid", "Squid Armor", "RARE", 6, "LEATHER", "Full set: breathes underwater."),
    ("sponge", "Sponge Armor", "RARE", 8, "LEATHER", "Full set: +100 Health in water."),
    ("angler", "Angler Armor", "RARE", 8, "LEATHER", "Full set: +50 Sea Creature Chance."),
    ("emerald", "Emerald Armor", "RARE", 9, "DIAMOND", "Full set: stronger the more emeralds you hold."),
    ("speedster", "Speedster Armor", "RARE", 6, "GOLD", "Full set: +20 Speed."),
    ("mastiff", "Mastiff Armor", "LEGENDARY", 10, "DIAMOND", "Full set: +1000 Health, healing halved."),
    ("young_dragon", "Young Dragon Armor", "LEGENDARY", 20, "DIAMOND", "Full set: +70 Speed, no slowdown."),
    ("strong_dragon", "Strong Dragon Armor", "LEGENDARY", 22, "DIAMOND", "Full set: +75 Strength."),
    ("superior_dragon", "Superior Dragon Armor", "LEGENDARY", 25, "DIAMOND", "Full set: +5% to every stat."),
    ("unstable_dragon", "Unstable Dragon Armor", "LEGENDARY", 22, "DIAMOND", "Full set: +20% Critical Chance."),
    ("wise_dragon", "Wise Dragon Armor", "LEGENDARY", 22, "DIAMOND", "Full set: abilities cost half the mana."),
    ("old_dragon", "Old Dragon Armor", "LEGENDARY", 28, "DIAMOND", "Full set: +200 Health, +200 Defence."),
    ("protector_dragon", "Protector Dragon Armor", "LEGENDARY", 30, "DIAMOND", "Full set: Defence rises as health falls."),
    ("holy_dragon", "Holy Dragon Armor", "LEGENDARY", 22, "DIAMOND", "Full set: healing doubled."),
    ("perfect", "Perfect Armor", "LEGENDARY", 26, "DIAMOND", "Full set: forged from Refined Diamond."),
]
ARMOUR_PIECES = [
    ("helmet", "Helmet", "HELMET"),
    ("chestplate", "Chestplate", "CHESTPLATE"),
    ("leggings", "Leggings", "LEGGINGS"),
    ("boots", "Boots", "BOOTS"),
]

# --- accessories -----------------------------------------------------------
# (id, display, rarity, texture, description)
ACCESSORIES = [
    ("zombie_talisman", "Zombie Talisman", "COMMON", "rotten_flesh", "+5% damage to zombies."),
    ("skeleton_talisman", "Skeleton Talisman", "COMMON", "bone", "+5% chance to dodge a skeleton's arrow."),
    ("spider_talisman", "Spider Talisman", "COMMON", "string", "Spiders no longer target you first."),
    ("village_affinity_talisman", "Village Affinity Talisman", "UNCOMMON", "emerald", "Shop prices drop by 5%."),
    ("wood_affinity_talisman", "Wood Affinity Talisman", "RARE", "stick", "+25% speed cutting wood."),
    ("farming_talisman", "Farming Talisman", "COMMON", "wheat", "No more trampling your own crops."),
    ("feather_talisman", "Feather Talisman", "COMMON", "feather", "Fall damage cut by 5%."),
    ("haste_ring", "Haste Ring", "UNCOMMON", "redstone", "Permanent Haste I."),
    ("piggy_bank", "Piggy Bank", "UNCOMMON", "gold_nugget", "Death costs you nothing. Breaks if you die twice over."),
    ("speed_talisman", "Speed Talisman", "COMMON", "sugar", "+1 Speed."),
    ("speed_ring", "Speed Ring", "UNCOMMON", "sugar", "+3 Speed."),
    ("speed_artifact", "Speed Artifact", "RARE", "sugar", "+5 Speed."),
    ("wolf_paw", "Wolf Paw", "RARE", "bone_meal", "+3 Speed."),
    ("sea_creature_talisman", "Sea Creature Talisman", "COMMON", "cod", "+5% Sea Creature Chance."),
    ("sea_creature_ring", "Sea Creature Ring", "UNCOMMON", "salmon", "+10% Sea Creature Chance."),
    ("sea_creature_artifact", "Sea Creature Artifact", "RARE", "tropical_fish", "+15% Sea Creature Chance."),
    ("magnetic_talisman", "Magnetic Talisman", "COMMON", "iron_nugget", "Pick things up from further away."),
    ("scavenger_talisman", "Scavenger Talisman", "UNCOMMON", "gold_nugget", "Kills drop a few extra coins."),
    ("personal_compactor_4000", "Personal Compactor 4000", "EPIC", "hopper", "Compacts one item as you pick it up."),
    ("personal_compactor_5000", "Personal Compactor 5000", "EPIC", "hopper", "Compacts three items as you pick them up."),
    ("personal_compactor_6000", "Personal Compactor 6000", "LEGENDARY", "hopper", "Compacts twelve items as you pick them up."),
    ("bat_ring", "Bat Ring", "UNCOMMON", "leather", "+2 Speed and +2 Intelligence at night."),
    ("bat_artifact", "Bat Artifact", "RARE", "leather", "+5 Speed and +5 Intelligence at night."),
    ("shady_ring", "Shady Ring", "RARE", "black_dye", "Buys things nobody admits to selling."),
    ("candy_ring", "Candy Ring", "EPIC", "sugar", "+50 Health."),
    ("intimidation_talisman", "Intimidation Talisman", "COMMON", "iron_sword", "Weak mobs leave you alone."),
    ("intimidation_ring", "Intimidation Ring", "UNCOMMON", "iron_sword", "Most mobs leave you alone."),
    ("intimidation_artifact", "Intimidation Artifact", "RARE", "diamond_sword", "Nothing weak comes near you."),
    ("titanium_talisman", "Titanium Talisman", "UNCOMMON", "iron_nugget", "+2% chance of Titanium."),
    ("titanium_ring", "Titanium Ring", "RARE", "iron_ingot", "+4% chance of Titanium."),
    ("titanium_artifact", "Titanium Artifact", "EPIC", "netherite_ingot", "+6% chance of Titanium."),
    ("wither_artifact", "Wither Artifact", "EPIC", "nether_star", "+10 Strength, +10 Intelligence."),
    ("healing_talisman", "Healing Talisman", "COMMON", "ghast_tear", "Heals you slowly, always."),
    ("healing_ring", "Healing Ring", "UNCOMMON", "ghast_tear", "Heals you faster."),
    ("night_vision_charm", "Night Vision Charm", "RARE", "golden_carrot", "Permanent Night Vision."),
    ("potion_affinity_talisman", "Potion Affinity Talisman", "COMMON", "glass_bottle", "Potions last 10% longer."),
    ("potion_affinity_ring", "Potion Affinity Ring", "UNCOMMON", "glass_bottle", "Potions last 20% longer."),
    ("potion_affinity_artifact", "Potion Affinity Artifact", "RARE", "glass_bottle", "Potions last 30% longer."),
    ("farmer_orb", "Farmer Orb", "EPIC", "heart_of_the_sea", "Regrows the crops around you."),
    ("experience_artifact", "Experience Artifact", "EPIC", "experience_bottle", "+15% experience from everything."),
    ("hunter_talisman", "Hunter Talisman", "UNCOMMON", "bone", "Your pets gain experience faster."),
    ("new_year_cake_bag", "New Year Cake Bag", "EPIC", "cake", "Holds every cake you have collected."),
]

# --- the blocks ------------------------------------------------------------
# Placeable, and drawn here because Minecraft has nothing like them.
# (id, display, rarity, style, colour)
# style: "ore" (speckled through stone) | "solid" | "crystal"
BLOCKS = [
    ("mithril_ore", "Mithril", "UNCOMMON", "ore", (120, 220, 200)),
    ("titanium_ore", "Titanium", "RARE", "ore", (200, 205, 215)),
    ("hard_stone_block", "Hard Stone", "COMMON", "solid", (118, 118, 124)),
    ("sulphur_ore", "Sulphur", "UNCOMMON", "ore", (226, 214, 74)),
    ("umber_block", "Umber", "UNCOMMON", "solid", (128, 86, 52)),
    ("tungsten_block", "Tungsten", "UNCOMMON", "solid", (92, 96, 108)),
    ("glacite_block", "Glacite", "UNCOMMON", "solid", (150, 200, 230)),
    ("treasurite_ore", "Treasurite", "RARE", "ore", (188, 120, 214)),
    ("mithril_block", "Block of Mithril", "RARE", "solid", (120, 220, 200)),
    ("titanium_block", "Block of Titanium", "EPIC", "solid", (200, 205, 215)),
    ("refined_mithril_block", "Block of Refined Mithril", "EPIC", "solid", (150, 240, 220)),
    ("refined_titanium_block", "Block of Refined Titanium", "EPIC", "solid", (228, 232, 240)),
    ("hard_stone_dwarven", "Dwarven Hard Stone", "COMMON", "solid", (104, 100, 96)),
    ("low_tier_mithril", "Low Tier Mithril", "COMMON", "ore", (86, 176, 160)),
    ("mid_tier_mithril", "Mid Tier Mithril", "UNCOMMON", "ore", (120, 220, 200)),
    ("high_tier_mithril", "High Tier Mithril", "RARE", "ore", (170, 245, 230)),
    ("titanium_deposit", "Titanium Deposit", "EPIC", "ore", (215, 220, 232)),
    ("gemstone_mine_stone", "Gemstone Mine Stone", "COMMON", "solid", (96, 92, 108)),
    ("glacite_jewel_block", "Block of Glacite Jewels", "RARE", "solid", (190, 226, 244)),
    ("hardened_wood", "Hardened Wood", "UNCOMMON", "solid", (128, 96, 56)),
    ("wood_singularity_block", "Wood Singularity", "LEGENDARY", "solid", (70, 44, 24)),
    ("sulphur_block", "Block of Sulphur", "UNCOMMON", "solid", (226, 214, 74)),
    ("bedrock_of_the_universe", "Jerry's Bedrock", "SPECIAL", "solid", (58, 56, 62)),
    ("crystal_fragment_block", "Block of Crystal Fragments", "EPIC", "crystal", (150, 210, 240)),
    ("umber_hardened", "Hardened Umber", "UNCOMMON", "solid", (108, 70, 40)),
    ("tungsten_hardened", "Hardened Tungsten", "UNCOMMON", "solid", (76, 80, 92)),
    ("mycelium_dust_block", "Block of Mycelium Dust", "RARE", "solid", (120, 96, 128)),
    ("obsidian_defender_block", "Void Obsidian", "EPIC", "solid", (44, 30, 72)),
]

# One gemstone-crystal block per gem, made from the same twelve colours.
GEM_BLOCK_GRADE = ("Gemstone", "RARE")


# --- the people ------------------------------------------------------------
# (id, display name, where they stand, rarity of the token, what they say,
#  the quest they hand out -- matching Quests.ALL by name where there is one)
#
# What they say is written from what each of them does in the game rather than
# copied out of it: the job, the shop, the quest and the place are right, the
# exact wording is this mod's.
# The people and the enemies are long enough to deserve files of their own.
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from people import NPCS                                    # noqa: E402
from enemies import MOBS, COLOURS as MOB_COLOURS           # noqa: E402


# ------------------------------------------------------------------ drawing
# No image library on this machine, so PNGs are written by hand, the same way
# tools/make_minion_texture.py does it.

def png(path, pixels):
    """Write a 16x16 RGBA image. pixels is a list of 16 rows of 16 tuples."""
    raw = b""
    for row in pixels:
        raw += b"\x00"
        for r, g, b, a in row:
            raw += bytes((r, g, b, a))

    def chunk(kind, data):
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body))

    blob = (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9))
            + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as handle:
        handle.write(blob)


def shade(colour, factor):
    return tuple(min(255, max(0, int(c * factor))) for c in colour)


CLEAR = (0, 0, 0, 0)

# A gem, drawn as a cut stone: bright facet top-left, body, dark facet
# bottom-right. Bigger and brighter as the grade improves, so a row of them
# reads as a ladder at a glance.
#   . nothing · L light facet · B body · D dark facet · W sparkle
GEM_ART = {
    "rough": [
        "................",
        "................",
        "................",
        "................",
        ".....LLBB.......",
        "....LBBBBD......",
        "....BBBBBDD.....",
        "...LBBBBBBD.....",
        "...BBBBBBDD.....",
        "....BBBBDD......",
        ".....BBDD.......",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "flawed": [
        "................",
        "................",
        "................",
        "......LL........",
        ".....LLBBD......",
        "....LLBBBBD.....",
        "....LBBBBBDD....",
        "...LBBBBBBDD....",
        "...LBBBBBBDD....",
        "....BBBBBBDD....",
        ".....BBBBDD.....",
        "......BBDD......",
        "................",
        "................",
        "................",
        "................",
    ],
    "fine": [
        "................",
        "................",
        "......LLL.......",
        ".....LLBBBD.....",
        "....LLBBBBDD....",
        "...LLBBBBBBDD...",
        "...LBBBBBBBDD...",
        "..LLBBBBBBBBDD..",
        "..LBBBBBBBBBDD..",
        "...BBBBBBBBDD...",
        "...BBBBBBBDDD...",
        "....BBBBBBDD....",
        ".....BBBBDD.....",
        "......BBDD......",
        "................",
        "................",
    ],
    "flawless": [
        "................",
        "......LLL.......",
        ".....LLBBBD.....",
        "....LLBBBBBD....",
        "...LLBBBBBBDD...",
        "..LLBBBBBBBBDD..",
        "..LBBBBBBBBBDD..",
        ".LLBBBBBBBBBBDD.",
        ".LBBBBBBBBBBBDD.",
        "..BBBBBBBBBBDD..",
        "..BBBBBBBBBBDD..",
        "...BBBBBBBBDD...",
        "....BBBBBBDD....",
        ".....BBBBDD.....",
        "......BBDD......",
        "................",
    ],
    "perfect": [
        "....W...........",
        "......LLL....W..",
        ".....LWBBBD.....",
        "....LLBBBBBD....",
        "...LLBBBBBBDD...",
        "..LLBBBWBBBBDD..",
        "..LBBBBBBBBBDD..",
        ".LLBBBBBBBBBBDD.",
        ".LBBBBBBBBBBBDD.",
        "..BBBBBBBBBBDD..",
        "..BBBBBBBWBBDD..",
        "...BBBBBBBBDD...",
        "....BBBBBBDD....",
        ".W...BBBBDD.....",
        "......BBDD...W..",
        "................",
    ],
}


def gem_texture(colour, grade):
    art = GEM_ART[grade]
    lift = {"rough": 0.75, "flawed": 0.9, "fine": 1.0, "flawless": 1.1, "perfect": 1.2}[grade]
    body = shade(colour, lift)
    light = shade(colour, lift * 1.45)
    dark = shade(colour, lift * 0.6)
    table = {
        ".": CLEAR,
        "L": light + (255,),
        "B": body + (255,),
        "D": dark + (255,),
        "W": (255, 255, 255, 255),
    }
    return [[table[c] for c in row] for row in art]


# Ore and stone blocks. Stone speckle underneath so a Dwarven ore sits next to
# a vanilla one without looking out of place.
STONE_SPECKLE = [
    "22112211221122 1",
    "11221122112211 2",
    "21122112211221 1",
    "12211221122112 2",
    "22112211221122 1",
    "11221122112211 2",
    "21122112211221 1",
    "12211221122112 2",
    "22112211221122 1",
    "11221122112211 2",
    "21122112211221 1",
    "12211221122112 2",
    "22112211221122 1",
    "11221122112211 2",
    "21122112211221 1",
    "12211221122112 2",
]

ORE_BLOBS = [
    (3, 3), (4, 3), (3, 4), (4, 4), (5, 4),
    (10, 2), (11, 2), (10, 3),
    (2, 9), (3, 9), (2, 10), (3, 10),
    (9, 10), (10, 10), (11, 10), (10, 11), (9, 11),
    (12, 6), (13, 6), (12, 7),
    (6, 12), (7, 12), (6, 13),
]


def block_texture(colour, style):
    base = (122, 122, 122)
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            key = STONE_SPECKLE[y][x]
            if style == "solid":
                # A metal plate: lit at the top-left, shaded at the bottom.
                lit = 1.25 - (x + y) / 44.0
                if x in (0, 15) or y in (0, 15):
                    lit *= 0.8
                row.append(shade(colour, lit) + (255,))
            elif style == "crystal":
                lit = 1.35 - (x + y) / 40.0
                row.append(shade(colour, lit) + (255,))
            else:
                tone = {"1": 1.0, "2": 0.88, " ": 1.1}[key]
                row.append(shade(base, tone) + (255,))
        pixels.append(row)

    if style == "ore":
        for x, y in ORE_BLOBS:
            # A lit top edge on each blob, so the ore reads as embedded rather
            # than painted on.
            above = (x, y - 1) not in ORE_BLOBS
            pixels[y][x] = shade(colour, 1.3 if above else 0.95) + (255,)
    return pixels


def crystal_texture(colour):
    """A gemstone block: shards of the gem's colour growing out of stone."""
    art = [
        "................",
        "......CC........",
        ".....CCCc.......",
        "....cCCCc..CC...",
        "....cCCc..CCCc..",
        "..CC.cCc..CCCc..",
        ".CCCc.cc.cCCcc..",
        ".CCCc....cCCc...",
        "..CCc.CC..cc....",
        "...cc.CCCc......",
        "......CCCc..CC..",
        ".......cc..CCCc.",
        "...........CCCc.",
        "............cc..",
        "................",
        "................",
    ]
    base = block_texture(colour, "ore")
    for y in range(16):
        for x in range(16):
            mark = art[y][x]
            if mark == "C":
                base[y][x] = shade(colour, 1.3) + (255,)
            elif mark == "c":
                base[y][x] = shade(colour, 0.75) + (255,)
    return base


# The token you place to stand an NPC somewhere: a villager, small and plain.
NPC_ART = [
    "................",
    "......SSSS......",
    ".....SHHHHS.....",
    "....SHHHHHHS....",
    "....SHNHHNHS....",
    "....SHHHHHHS....",
    "....SHHnnHHS....",
    "....SHHnnHHS....",
    ".....SHHHHS.....",
    "......SnnS......",
    ".....CCCCCC.....",
    "....CCCCCCCC....",
    "....CCbbbbCC....",
    "....CCbbbbCC....",
    ".....CC..CC.....",
    "................",
]


def npc_texture():
    table = {
        ".": CLEAR,
        "S": (86, 62, 48, 255),      # outline
        "H": (192, 146, 112, 255),   # face
        "N": (60, 44, 36, 255),      # eyes
        "n": (128, 96, 74, 255),     # nose and brow
        "C": (110, 84, 60, 255),     # robe
        "b": (74, 56, 42, 255),      # crossed arms
    }
    return [[table[c] for c in row] for row in NPC_ART]



# The token you place to put an enemy somewhere: a spawn egg, in the two
# colours you would know the mob by.
EGG_ART = [
    "................",
    "................",
    "......LLLL......",
    ".....LBBBBL.....",
    "....LBBBBBBL....",
    "...LBBBSBBBBL...",
    "...LBSSBBBSBL...",
    "..LBBBBBSBBBBL..",
    "..LBSBBBBBBSBL..",
    "..LBBBBSBBBBBL..",
    "..LBSBBBBBSBBL..",
    "...LBBBBSBBBL...",
    "...LBBSBBBBBL...",
    "....LBBBBBBL....",
    ".....LLLLLL.....",
    "................",
]


def egg_texture(base, spot):
    table = {
        ".": CLEAR,
        "L": shade(base, 0.55) + (255,),
        "B": base + (255,),
        "S": spot + (255,),
    }
    return [[table[c] for c in row] for row in EGG_ART]



# A rune: a stone tablet with a glyph cut into it, glowing in the rune's own
# colour. Higher tiers glow brighter, which is the whole of the difference
# between them.
RUNE_ART = [
    "................",
    "................",
    "..SSSSSSSSSSSS..",
    "..SDDDDDDDDDDS..",
    "..SD........DS..",
    "..SD.GGGGGG.DS..",
    "..SD.G....G.DS..",
    "..SD.G.GG.G.DS..",
    "..SD.G.GG.G.DS..",
    "..SD.G....G.DS..",
    "..SD.GGGGGG.DS..",
    "..SD........DS..",
    "..SDDDDDDDDDDS..",
    "..SSSSSSSSSSSS..",
    "................",
    "................",
]


def rune_texture(colour, glow):
    table = {
        ".": CLEAR,
        "S": (150, 148, 154, 255),
        "D": (92, 90, 96, 255),
        "G": shade(colour, glow) + (255,),
    }
    return [[table[c] for c in row] for row in RUNE_ART]


# ------------------------------------------------------------------- writing

written = []
missing = []
vanilla_items = set()
vanilla_blocks = set()


def load_vanilla():
    if not os.path.isfile(VANILLA_JAR):
        print("!! no Minecraft jar at", VANILLA_JAR)
        print("   borrowed texture names will not be checked this run")
        return False
    import zipfile
    with zipfile.ZipFile(VANILLA_JAR) as jar:
        for entry in jar.namelist():
            if entry.startswith("assets/minecraft/textures/item/") and entry.endswith(".png"):
                vanilla_items.add(entry[len("assets/minecraft/textures/item/"):-4])
            elif entry.startswith("assets/minecraft/models/block/") and entry.endswith(".json"):
                vanilla_blocks.add(entry[len("assets/minecraft/models/block/"):-5])
    return True


def check(kind, ref, owner):
    if kind == "own" or (not vanilla_items and not vanilla_blocks):
        return
    pool = vanilla_items if kind in ("item", "handheld") else vanilla_blocks
    if ref not in pool:
        missing.append(f"{owner}: no vanilla {kind} '{ref}'")


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as handle:
        json.dump(data, handle, indent=2)
        handle.write("\n")
    written.append(path)


def item_asset(item_id, kind, ref, owner):
    """The two files that decide what an item looks like."""
    check(kind, ref, owner)
    write_json(os.path.join(ASSETS, "items", item_id + ".json"),
               {"model": {"type": "minecraft:model", "model": "skyblocks:item/" + item_id}})

    if kind == "block":
        model = {"parent": "minecraft:block/" + ref}
    elif kind == "handheld":
        model = {"parent": "minecraft:item/handheld",
                 "textures": {"layer0": "minecraft:item/" + ref}}
    elif kind == "own":
        model = {"parent": "minecraft:item/generated",
                 "textures": {"layer0": "skyblocks:item/" + ref}}
    else:
        model = {"parent": "minecraft:item/generated",
                 "textures": {"layer0": "minecraft:item/" + ref}}
    write_json(os.path.join(ASSETS, "models/item", item_id + ".json"), model)


def block_asset(block_id):
    """A cube of one texture, and the item you hold to place it."""
    write_json(os.path.join(ASSETS, "blockstates", block_id + ".json"),
               {"variants": {"": {"model": "skyblocks:block/" + block_id}}})
    write_json(os.path.join(ASSETS, "models/block", block_id + ".json"),
               {"parent": "minecraft:block/cube_all",
                "textures": {"all": "skyblocks:block/" + block_id}})
    write_json(os.path.join(ASSETS, "models/item", block_id + ".json"),
               {"parent": "skyblocks:block/" + block_id})
    write_json(os.path.join(ASSETS, "items", block_id + ".json"),
               {"model": {"type": "minecraft:model", "model": "skyblocks:block/" + block_id}})


# ------------------------------------------------------------ the Java table

def jstr(text):
    return '"' + text.replace("\\", "\\\\").replace('"', '\\"') + '"'


def chunked(name, kind, rows, out):
    """Write an array as several methods.

    One method holding five hundred entries overflows the 64KB the JVM allows
    a single method, and the compiler says so in a way that takes a while to
    understand. Splitting it is easier than finding that out twice.
    """
    parts = [rows[i:i + 80] for i in range(0, len(rows), 80)] or [[]]
    for index, part in enumerate(parts):
        out.append(f"\tprivate static {kind}[] {name}{index}() {{")
        out.append(f"\t\treturn new {kind}[] {{")
        for row in part:
            out.append("\t\t\t\t" + row + ",")
        out.append("\t\t};")
        out.append("\t}")
        out.append("")
    out.append(f"\tprivate static {kind}[] {name}() {{")
    out.append(f"\t\tjava.util.List<{kind}> all = new java.util.ArrayList<>();")
    for index in range(len(parts)):
        out.append(f"\t\tall.addAll(java.util.Arrays.asList({name}{index}()));")
    out.append(f"\t\treturn all.toArray(new {kind}[0]);")
    out.append("\t}")
    out.append("")


def main():
    load_vanilla()

    lang = {}
    mats, gear, armour, accs, blocks, npcs = [], [], [], [], [], []

    # --- enchanted and special materials -----------------------------------
    for item_id, name, rarity, kind, ref in ENCHANTED:
        item_asset(item_id, kind, ref, item_id)
        lang["item.skyblocks." + item_id] = name
        mats.append(f"new Mat({jstr(item_id)}, {jstr(name)}, {jstr(rarity)}, "
                    f"{jstr('enchanted')}, true, {jstr('')})")

    for item_id, name, rarity, kind, ref, desc in SPECIALS:
        item_asset(item_id, kind, ref, item_id)
        lang["item.skyblocks." + item_id] = name
        mats.append(f"new Mat({jstr(item_id)}, {jstr(name)}, {jstr(rarity)}, "
                    f"{jstr('material')}, false, {jstr(desc)})")

    # --- the two blocks that do something ----------------------------------
    # Assets only. These are registered by hand in Java because they have
    # behaviour -- a plain block from the table below cannot be right-clicked
    # to combine runes or to open a dungeon.
    for block_id, colour in [("rune_pedestal", (150, 148, 154)),
                             ("catacombs_entrance", (74, 68, 88))]:
        png(os.path.join(ASSETS, "textures/block", block_id + ".png"),
            block_texture(colour, "solid"))
        block_asset(block_id)

    # --- the runes ---------------------------------------------------------
    runes = []
    for rune_id, rune_name, colour in RUNES:
        for tier_id, tier, rarity, glow in RUNE_TIERS:
            item_id = rune_id + "_rune_" + tier_id
            name = rune_name + " Rune " + tier
            png(os.path.join(ASSETS, "textures/item", item_id + ".png"),
                rune_texture(colour, glow))
            item_asset(item_id, "own", item_id, item_id)
            lang["item.skyblocks." + item_id] = name
            runes.append(f"new Rune({jstr(item_id)}, {jstr(name)}, {jstr(rarity)}, "
                         f"{jstr(rune_id)}, {RUNE_TIERS.index((tier_id, tier, rarity, glow)) + 1})")

    # --- the XP Boost potions ----------------------------------------------
    boosts = []
    for skill in BOOST_SKILLS:
        for tier_id, tier, wisdom, rarity in BOOST_TIERS:
            item_id = skill.lower() + "_xp_boost_" + tier_id
            name = skill + " XP Boost " + tier
            item_asset(item_id, "item", "potion", item_id)
            lang["item.skyblocks." + item_id] = name
            boosts.append(f"new Boost({jstr(item_id)}, {jstr(name)}, {jstr(rarity)}, "
                          f"{jstr(skill)}, {wisdom})")

    # --- gemstones ---------------------------------------------------------
    for gem, gem_name, colour in GEM_KINDS:
        for grade, grade_name, rarity in GEM_GRADES:
            item_id = f"{grade}_{gem}_gem"
            name = f"{grade_name} {gem_name} Gemstone"
            png(os.path.join(ASSETS, "textures/item", item_id + ".png"),
                gem_texture(colour, grade))
            item_asset(item_id, "own", item_id, item_id)
            lang["item.skyblocks." + item_id] = name
            mats.append(f"new Mat({jstr(item_id)}, {jstr(name)}, {jstr(rarity)}, "
                        f"{jstr('gemstone')}, {'true' if grade == 'perfect' else 'false'}, "
                        f"{jstr('Cut from the gemstone mines.')})")

    # --- gear --------------------------------------------------------------
    for item_id, name, rarity, kind, ref, damage, speed, desc in GEAR:
        item_asset(item_id, "handheld" if kind != "bow" else "item", ref, item_id)
        lang["item.skyblocks." + item_id] = name
        gear.append(f"new Gear({jstr(item_id)}, {jstr(name)}, {jstr(rarity)}, "
                    f"{jstr(kind)}, {jstr(ref)}, {damage}f, {speed}f, {jstr(desc)})")

    # --- armour ------------------------------------------------------------
    for set_id, set_name, rarity, defence, look, desc in ARMOUR_SETS:
        for piece_id, piece_name, piece_type in ARMOUR_PIECES:
            item_id = f"{set_id}_{piece_id}"
            name = f"{set_name} {piece_name}"
            # Borrows the matching vanilla armour sprite, so a helmet looks
            # like a helmet without thirty new textures.
            ref = {"LEATHER": "leather", "CHAINMAIL": "chainmail", "IRON": "iron",
                   "GOLD": "golden", "DIAMOND": "diamond"}[look] + "_" + piece_id
            item_asset(item_id, "item", ref, item_id)
            lang["item.skyblocks." + item_id] = name
            armour.append(f"new Armour({jstr(item_id)}, {jstr(name)}, {jstr(rarity)}, "
                          f"{jstr(piece_type)}, {defence}, {jstr(look)}, {jstr(desc)})")

    # --- accessories -------------------------------------------------------
    for item_id, name, rarity, ref, desc in ACCESSORIES:
        item_asset(item_id, "item", ref, item_id)
        lang["item.skyblocks." + item_id] = name
        accs.append(f"new Mat({jstr(item_id)}, {jstr(name)}, {jstr(rarity)}, "
                    f"{jstr('accessory')}, false, {jstr(desc)})")

    # --- blocks ------------------------------------------------------------
    for block_id, name, rarity, style, colour in BLOCKS:
        png(os.path.join(ASSETS, "textures/block", block_id + ".png"),
            block_texture(colour, style))
        block_asset(block_id)
        lang["block.skyblocks." + block_id] = name
        hardness = 3.0 if style == "solid" else 4.0
        blocks.append(f"new Blok({jstr(block_id)}, {jstr(name)}, {jstr(rarity)}, {hardness}f)")

    for gem, gem_name, colour in GEM_KINDS:
        block_id = f"{gem}_gemstone_block"
        name = f"{gem_name} Gemstone"
        png(os.path.join(ASSETS, "textures/block", block_id + ".png"),
            crystal_texture(colour))
        block_asset(block_id)
        lang["block.skyblocks." + block_id] = name
        blocks.append(f"new Blok({jstr(block_id)}, {jstr(name)}, "
                      f"{jstr(GEM_BLOCK_GRADE[1])}, 5.0f)")

    # --- the enemies -------------------------------------------------------
    # One egg per vanilla mob, shared by every enemy wearing it: a Crypt Ghoul
    # and a Golden Ghoul are both zombies underneath, so they get the same egg
    # and are told apart by the name and level on the tooltip.
    mobs = []
    drawn = set()
    for mob_id, name, level, health, damage, where, entity in MOBS:
        if entity not in drawn:
            drawn.add(entity)
            base, spot = MOB_COLOURS.get(entity, ((140, 140, 140), (90, 90, 90)))
            png(os.path.join(ASSETS, "textures/item", "egg_" + entity + ".png"),
                egg_texture(base, spot))
        item_id = "mob_" + mob_id
        item_asset(item_id, "own", "egg_" + entity, item_id)
        lang["item.skyblocks." + item_id] = name + " [Lv" + str(level) + "]"
        mobs.append(f"new Mob({jstr(mob_id)}, {jstr(name)}, {level}, {health}.0, "
                    f"{damage}.0, {jstr(where)}, {jstr(entity)})")

    # --- the people --------------------------------------------------------
    png(os.path.join(ASSETS, "textures/item", "npc_token.png"), npc_texture())
    for npc_id, name, where, lines, quest in NPCS:
        item_id = "npc_" + npc_id
        item_asset(item_id, "own", "npc_token", item_id)
        lang["item.skyblocks." + item_id] = name
        said = ", ".join(jstr(line) for line in lines)
        npcs.append(f"new Npc({jstr(npc_id)}, {jstr(name)}, {jstr(where)}, "
                    f"new String[] {{{said}}}, {jstr(quest)})")

    # --- Content.java ------------------------------------------------------
    out = []
    out.append("package com.example;")
    out.append("")
    out.append("/**")
    out.append(" * Everything Sky Blocks adds that Minecraft does not already have.")
    out.append(" *")
    out.append(" * Generated by tools/make_content.py -- do not edit by hand. The lists")
    out.append(" * live in that script, next to the textures and models they go with, so")
    out.append(" * that adding an item is one edit in one place rather than four.")
    out.append(" *")
    out.append(" * @see SkyItems for what turns these into real registered items")
    out.append(" */")
    out.append("public final class Content {")
    out.append("\tprivate Content() {")
    out.append("\t}")
    out.append("")
    out.append("\t/** A thing you hold: materials, gemstones, accessories. */")
    out.append("\tpublic record Mat(String id, String name, String rarity, String tab,")
    out.append("\t\t\tboolean glint, String desc) {")
    out.append("\t}")
    out.append("")
    out.append("\t/** Something you swing, fire or mine with. */")
    out.append("\tpublic record Gear(String id, String name, String rarity, String kind,")
    out.append("\t\t\tString look, float damage, float speed, String desc) {")
    out.append("\t}")
    out.append("")
    out.append("\t/** One piece of an armour set. */")
    out.append("\tpublic record Armour(String id, String name, String rarity, String type,")
    out.append("\t\t\tint defence, String look, String desc) {")
    out.append("\t}")
    out.append("")
    out.append("\t/** A placeable block. */")
    out.append("\tpublic record Blok(String id, String name, String rarity, float hardness) {")
    out.append("\t}")
    out.append("")
    out.append("\t/** Somebody who lives in the world, and what they say. */")
    out.append("\tpublic record Npc(String id, String name, String where, String[] lines,")
    out.append("\t\t\tString quest) {")
    out.append("\t}")
    out.append("")
    out.append("\t/** A rune: which rune it is, and which of its three tiers. */")
    out.append("\tpublic record Rune(String id, String name, String rarity, String kind,")
    out.append("\t\t\tint tier) {")
    out.append("\t}")
    out.append("")
    out.append("\t/** An XP Boost potion: a skill, and the Wisdom it grants. */")
    out.append("\tpublic record Boost(String id, String name, String rarity, String skill,")
    out.append("\t\t\tint wisdom) {")
    out.append("\t}")
    out.append("")
    out.append("\t/** An enemy, with Hypixel's own level, health and damage. */")
    out.append("\tpublic record Mob(String id, String name, int level, double health,")
    out.append("\t\t\tdouble damage, String where, String entity) {")
    out.append("\t}")
    out.append("")
    chunked("mats", "Mat", mats, out)
    chunked("accessories", "Mat", accs, out)
    chunked("gear", "Gear", gear, out)
    chunked("armour", "Armour", armour, out)
    chunked("blocks", "Blok", blocks, out)
    chunked("npcs", "Npc", npcs, out)
    chunked("mobs", "Mob", mobs, out)
    chunked("boosts", "Boost", boosts, out)
    chunked("runes", "Rune", runes, out)
    out.append("\tpublic static final Mat[] MATERIALS = mats();")
    out.append("\tpublic static final Mat[] ACCESSORIES = accessories();")
    out.append("\tpublic static final Gear[] GEAR = gear();")
    out.append("\tpublic static final Armour[] ARMOUR = armour();")
    out.append("\tpublic static final Blok[] BLOCKS = blocks();")
    out.append("\tpublic static final Npc[] NPCS = npcs();")
    out.append("\tpublic static final Mob[] MOBS = mobs();")
    out.append("\tpublic static final Boost[] BOOSTS = boosts();")
    out.append("\tpublic static final Rune[] RUNES = runes();")
    out.append("}")

    java_path = os.path.join(JAVA, "Content.java")
    with open(java_path, "w") as handle:
        handle.write("\n".join(out) + "\n")
    written.append(java_path)

    # --- language ----------------------------------------------------------
    lang_path = os.path.join(ASSETS, "lang/en_us.json")
    existing = {}
    if os.path.isfile(lang_path):
        with open(lang_path) as handle:
            existing = json.load(handle)
    # Anything generated here wins, so renaming an item in this file renames it
    # in the game; hand-written entries for everything else are left alone.
    existing.update(lang)
    existing.update({
        "itemGroup.skyblocks.materials": "SkyBlock Materials",
        "itemGroup.skyblocks.enchanted": "SkyBlock Enchanted",
        "itemGroup.skyblocks.gear": "SkyBlock Weapons & Tools",
        "itemGroup.skyblocks.armor": "SkyBlock Armor",
        "itemGroup.skyblocks.accessories": "SkyBlock Accessories",
        "itemGroup.skyblocks.blocks": "SkyBlock Blocks",
        "itemGroup.skyblocks.npcs": "SkyBlock NPCs",
        "itemGroup.skyblocks.mobs": "SkyBlock Enemies",
        "itemGroup.skyblocks.boosts": "SkyBlock XP Boosts",
        "itemGroup.skyblocks.runes": "SkyBlock Runes",
        "block.skyblocks.rune_pedestal": "Rune Pedestal",
        "block.skyblocks.catacombs_entrance": "Catacombs Entrance",
    })
    write_json(lang_path, dict(sorted(existing.items())))

    # --- what happened -----------------------------------------------------
    total = (len(mats) + len(accs) + len(gear) + len(armour) + len(blocks)
             + len(npcs) + len(mobs) + len(boosts) + len(runes))
    print(f"materials    {len(mats):>4}")
    print(f"accessories  {len(accs):>4}")
    print(f"weapons      {len(gear):>4}")
    print(f"armour       {len(armour):>4}")
    print(f"blocks       {len(blocks):>4}")
    print(f"people       {len(npcs):>4}")
    print(f"enemies      {len(mobs):>4}")
    print(f"xp boosts    {len(boosts):>4}")
    print(f"runes        {len(runes):>4}")
    print(f"             ---- {total} things, {len(written)} files")

    if missing:
        print()
        print("BORROWED TEXTURES THAT DO NOT EXIST:")
        for line in sorted(set(missing)):
            print("  " + line)
        sys.exit(1)


if __name__ == "__main__":
    main()
