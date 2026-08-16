#!/usr/bin/env python3
"""Every enemy in SkyBlock, with the numbers the game gives them.

Read by tools/make_content.py, which turns each entry into a token you can
place to put that enemy in the world, named and levelled the way it is on
Hypixel.

    (id, name, level, health, damage, where it lives, what it looks like)

The health and damage are Hypixel's own, unscaled and unrounded -- a Soul of
the Alpha really does have 31,150 health and hit for 1,140. They are divided
down on the way into the world by Mobs.HEALTH_SCALE and Mobs.DAMAGE_SCALE,
because a vanilla player has twenty health and would simply be deleted
otherwise. Keeping the real figures here means the ratios survive: the Alpha
stays about thirty times the threat a Splitter Spider is, and one number in one
place makes the whole game harder or softer.

The last field is the vanilla mob used to show it. SkyBlock does the same thing
-- a Crypt Ghoul is a zombie underneath.
"""

MOBS = [
    # ------------------------------------------------------ hub and graveyard
    ("zombie", "Zombie", 1, 100, 20, "Graveyard", "zombie"),
    ("zombie_villager", "Zombie Villager", 1, 120, 24, "Graveyard", "zombie_villager"),
    ("crypt_ghoul", "Crypt Ghoul", 30, 2000, 350, "Hub Crypts", "zombie"),
    ("golden_ghoul", "Golden Ghoul", 60, 45000, 800, "Hub Crypts", "zombie"),
    ("wolf", "Wolf", 15, 250, 90, "Ruins", "wolf"),
    ("old_wolf", "Old Wolf", 50, 15000, 800, "Ruins", "wolf"),

    # ---------------------------------------------------------- howling cave
    ("pack_spirit", "Pack Spirit", 30, 6000, 240, "Howling Cave", "wolf"),
    ("howling_spirit", "Howling Spirit", 35, 7000, 400, "Howling Cave", "wolf"),
    ("soul_of_the_alpha", "Soul of the Alpha", 55, 31150, 1140, "Howling Cave", "wolf"),

    # ---------------------------------------------------------- spider's den
    ("splitter_spider", "Splitter Spider", 2, 180, 30, "Spider's Den", "spider"),
    ("splitter_spider_4", "Splitter Spider", 4, 220, 40, "Spider's Den", "spider"),
    ("weaver_spider", "Weaver Spider", 3, 160, 35, "Spider's Den", "spider"),
    ("dasher_spider", "Dasher Spider", 4, 170, 55, "Spider's Den", "spider"),
    ("dasher_spider_6", "Dasher Spider", 6, 210, 70, "Spider's Den", "spider"),
    ("spider_jockey", "Spider Jockey", 4, 220, 55, "Spider's Den", "spider"),
    ("jockey_skeleton", "Jockey Skeleton", 4, 250, 40, "Spider's Den", "skeleton"),
    ("den_skeleton", "Skeleton", 2, 100, 35, "Spider's Den", "skeleton"),
    ("voracious_spider", "Voracious Spider", 10, 1000, 100, "Spider's Den", "spider"),
    ("silverfish", "Silverfish", 1, 50, 20, "Spider's Den", "silverfish"),
    ("rain_slime", "Rain Slime", 8, 200, 100, "Spider's Den", "slime"),
    ("broodmother", "Broodmother", 12, 3000, 125, "Spider's Den", "spider"),

    # ---------------------------------------------------------- deep caverns
    ("sneaky_creeper", "Sneaky Creeper", 3, 120, 80, "Gunpowder Mines", "creeper"),
    ("lapis_zombie", "Lapis Zombie", 8, 200, 50, "Lapis Quarry", "zombie"),
    ("redstone_pigman", "Redstone Pigman", 10, 250, 75, "Pigmen's Den", "zombified_piglin"),
    ("emerald_slime", "Emerald Slime", 5, 80, 70, "Slimehill", "slime"),
    ("emerald_slime_10", "Emerald Slime", 10, 150, 100, "Slimehill", "slime"),
    ("emerald_slime_15", "Emerald Slime", 15, 250, 150, "Slimehill", "slime"),
    ("miner_zombie", "Miner Zombie", 15, 250, 200, "Diamond Reserve", "zombie"),
    ("miner_skeleton", "Miner Skeleton", 15, 250, 150, "Diamond Reserve", "skeleton"),
    ("miner_zombie_20", "Miner Zombie", 20, 300, 275, "Obsidian Sanctuary", "zombie"),
    ("miner_skeleton_20", "Miner Skeleton", 20, 300, 200, "Obsidian Sanctuary", "skeleton"),

    # -------------------------------------------------------- dwarven mines
    ("glacite_walker", "Glacite Walker", 45, 888, 500, "Great Ice Wall", "stray"),
    ("treasure_hoarder", "Treasure Hoarder", 70, 22000, 750, "Upper Mines", "vindicator"),
    ("goblin", "Goblin", 25, 800, 300, "Goblin Holdout", "zombie"),
    ("knifethrower", "Knifethrower", 25, 800, 300, "Goblin Holdout", "zombie"),
    ("fireslinger", "Fireslinger", 25, 200, 20, "Goblin Holdout", "zombie"),
    ("star_sentry", "Star Sentry", 50, 2000, 400, "Dwarven Mines", "iron_golem"),
    ("powder_ghast", "Powder Ghast", 1, 100, 1, "Dwarven Mines", "ghast"),
    ("ghost", "Ghost", 250, 1000000, 1000, "The Mist", "vex"),
    ("thyst", "Thyst", 20, 5000, 250, "Amethyst deposits", "endermite"),
    ("grunt", "Grunt", 50, 15000, 300, "Mithril Deposits", "zombie"),

    # ------------------------------------------------------ crystal hollows
    ("sludge", "Sludge", 5, 5000, 0, "Jungle", "slime"),
    ("sludge_10", "Sludge", 10, 10000, 700, "Jungle", "slime"),
    ("sludge_100", "Sludge", 100, 25000, 900, "Jungle", "slime"),
    ("kalhuiki_tribe_member", "Kalhuiki Tribe Member", 100, 1000000000, 800000,
     "Jungle Village", "zombie"),
    ("kalhuiki_elder", "Kalhuiki Elder", 100, 1000000000, 800000, "Jungle Village", "zombie"),
    ("kalhuiki_youngling", "Kalhuiki Youngling", 100, 1000000, 0, "Jungle Village", "zombie"),
    ("jungle_key_guardian", "Jungle Key Guardian", 100, 1000000, 100,
     "Key Guardian Temple", "zombie"),

    # --------------------------------------------------------------- the end
    ("enderman", "Enderman", 42, 4500, 500, "The End", "enderman"),
    ("enderman_45", "Enderman", 45, 6000, 600, "The End", "enderman"),
    ("enderman_50", "Enderman", 50, 9000, 700, "The End", "enderman"),
    ("endermite", "Endermite", 37, 2000, 400, "The End", "endermite"),
    ("endermite_40", "Endermite", 40, 2300, 475, "Dragon's Nest", "endermite"),
    ("nest_endermite", "Nest Endermite", 50, 4500, 1000, "The End", "endermite"),
    ("zealot", "Zealot", 55, 13000, 1250, "Dragon's Nest", "enderman"),
    ("zealot_bruiser", "Zealot Bruiser", 100, 65000, 2500, "Zealot Bruiser Hideout", "enderman"),
    ("watcher", "Watcher", 55, 9500, 500, "Dragon's Nest", "skeleton"),
    ("obsidian_defender", "Obsidian Defender", 55, 10000, 200, "Dragon's Nest", "enderman"),
    ("voidling_fanatic", "Voidling Fanatic", 85, 8000000, 13500, "Void Sepulture", "enderman"),
    ("voidling_extremist", "Voidling Extremist", 100, 750000, 3500, "Void Sepulture", "enderman"),

    # ------------------------------------------------------- the crimson isle
    ("wither_skeleton", "Wither Skeleton", 70, 600000, 3000, "Stronghold", "wither_skeleton"),
    ("wither_spectre", "Wither Spectre", 70, 700000, 3000, "Stronghold", "vex"),
    ("blaze", "Blaze", 70, 600000, 3000, "Stronghold", "blaze"),
    ("bezal", "Bezal", 80, 1000000, 3000, "Stronghold", "blaze"),
    ("mutated_blaze", "Mutated Blaze", 70, 1500000, 3500, "Stronghold", "blaze"),
    ("flare", "Flare", 90, 5000000, 3500, "Magma Chamber", "blaze"),
    ("smoldering_blaze", "Smoldering Blaze", 95, 5500000, 4500, "Smoldering Tomb", "blaze"),
    ("millennia_aged_blaze", "Millennia-Aged Blaze", 110, 30000000, 5000,
     "Smoldering Tomb", "blaze"),
    ("dive_ghast", "Dive Ghast", 90, 200000, 10000, "The Wasteland", "ghast"),
    ("ghast", "Ghast", 85, 2000000, 5000, "The Wasteland", "ghast"),
    ("hellwisp", "Hellwisp", 100, 5000000, 5000, "Matriarch's Lair", "ghast"),
    ("mushroom_bull", "Mushroom Bull", 80, 2500000, 5000, "Mystic Marsh", "mooshroom"),
    ("flaming_spider", "Flaming Spider", 80, 1000000, 2000, "Burning Desert", "spider"),
    ("magma_cube", "Magma Cube", 75, 1000000, 3000, "Crimson Fields", "magma_cube"),
    ("pack_magma_cube", "Pack Magma Cube", 90, 1500000, 4000, "Crimson Isle", "magma_cube"),
    ("magma_cube_rider", "Magma Cube Rider", 90, 3000000, 4000, "Crimson Isle", "zombified_piglin"),
    ("kada_knight", "Kada Knight", 90, 2000000, 4000, "Crimson Isle", "zombified_piglin"),
    ("matcho", "Matcho", 100, 2000000, 4000, "Blazing Volcano", "zombified_piglin"),
    ("barbarian", "Barbarian", 75, 2000000, 3500, "Barbarian Outpost", "piglin_brute"),
    ("goliath_barbarian", "Goliath Barbarian", 80, 5000000, 4000, "Barbarian Outpost", "piglin_brute"),
    ("fire_mage", "Fire Mage", 75, 2000000, 3000, "Mage Outpost", "witch"),
    ("krondor_necromancer", "Krondor Necromancer", 80, 5000000, 4000, "Mage Outpost", "witch"),
    ("vanquisher", "Vanquisher", 100, 10000000, 5000, "Crimson Isle", "wither_skeleton"),
    ("bladesoul", "Bladesoul", 200, 50000000, 4000, "Stronghold", "wither_skeleton"),
    ("mage_outlaw", "Mage Outlaw", 200, 70000000, 5000, "Courtyard", "witch"),
    ("barbarian_duke_x", "Barbarian Duke X", 200, 50000000, 4500, "The Dukedom", "piglin_brute"),
    ("ashfang", "Ashfang", 200, 50000000, 5000, "Ruins of Ashfang", "blaze"),
    ("magma_boss", "Magma Boss", 500, 200000000, 6000, "Magma Chamber", "magma_cube"),

    # ------------------------------------------------------------ mystic marsh
    ("exe", "Exe", 50, 100, 0, "Mystic Marsh", "vex"),
    ("wai", "Wai", 50, 100, 0, "Mystic Marsh", "vex"),
    ("zee", "Zee", 50, 100, 0, "Mystic Marsh", "vex"),

    # ---------------------------------------------------------------- animals
    ("cow", "Cow", 1, 50, 0, "The Barn", "cow"),
    ("pig", "Pig", 1, 50, 0, "The Barn", "pig"),
    ("chicken", "Chicken", 1, 50, 0, "The Barn", "chicken"),
    ("sheep", "Sheep", 1, 120, 0, "Shepherd's Keep", "sheep"),
    ("rabbit", "Rabbit", 1, 130, 0, "Oasis", "rabbit"),
    ("mushroom_cow", "Mushroom Cow", 1, 50, 0, "Mushroom Gorge", "mooshroom"),

    # -------------------------------------------------------------- the sea
    ("squid", "Squid", 1, 60, 8, "Fishing", "squid"),
    ("sea_walker", "Sea Walker", 4, 100, 10, "Fishing", "drowned"),
    ("sea_witch", "Sea Witch", 15, 500, 60, "Fishing", "witch"),
    ("sea_archer", "Sea Archer", 15, 450, 55, "Fishing", "skeleton"),
    ("catfish", "Catfish", 23, 800, 90, "Fishing", "drowned"),
    ("sea_leech", "Sea Leech", 30, 1200, 120, "Fishing", "drowned"),
    ("guardian_defender", "Guardian Defender", 45, 3000, 250, "Fishing", "guardian"),
    ("deep_sea_protector", "Deep Sea Protector", 60, 8000, 400, "Fishing", "elder_guardian"),
]

# What each vanilla mob's token looks like: a spawn-egg sort of thing, in the
# two colours you would recognise the mob by. Anything not listed here gets the
# grey default, which is a hint to come back and pick a colour for it.
COLOURS = {
    "zombie": ((58, 140, 90), (120, 190, 130)),
    "zombie_villager": ((58, 140, 90), (168, 132, 96)),
    "skeleton": ((198, 198, 198), (120, 120, 120)),
    "stray": ((198, 214, 224), (100, 140, 160)),
    "wither_skeleton": ((60, 60, 60), (120, 116, 108)),
    "spider": ((60, 48, 44), (150, 40, 30)),
    "cave_spider": ((30, 60, 68), (150, 40, 30)),
    "silverfish": ((110, 112, 118), (60, 62, 68)),
    "endermite": ((70, 60, 96), (140, 90, 200)),
    "enderman": ((22, 22, 26), (150, 90, 220)),
    "slime": ((110, 200, 96), (160, 230, 140)),
    "magma_cube": ((52, 24, 16), (232, 120, 30)),
    "creeper": ((70, 180, 80), (30, 100, 40)),
    "wolf": ((214, 214, 214), (120, 110, 104)),
    "witch": ((60, 46, 84), (110, 200, 110)),
    "blaze": ((240, 190, 40), (250, 240, 130)),
    "ghast": ((238, 238, 238), (180, 180, 190)),
    "vex": ((190, 200, 220), (110, 130, 170)),
    "zombified_piglin": ((222, 130, 130), (58, 140, 90)),
    "piglin_brute": ((216, 150, 120), (110, 76, 52)),
    "iron_golem": ((196, 196, 200), (150, 190, 130)),
    "vindicator": ((100, 104, 110), (60, 100, 100)),
    "guardian": ((90, 150, 150), (200, 130, 60)),
    "elder_guardian": ((190, 190, 170), (140, 130, 170)),
    "drowned": ((70, 130, 120), (120, 190, 160)),
    "squid": ((60, 80, 130), (100, 120, 180)),
    "cow": ((60, 48, 40), (200, 200, 200)),
    "mooshroom": ((160, 40, 40), (200, 200, 200)),
    "pig": ((230, 150, 160), (200, 100, 110)),
    "chicken": ((230, 230, 230), (230, 170, 50)),
    "sheep": ((230, 230, 230), (220, 190, 180)),
    "rabbit": ((160, 130, 100), (220, 200, 180)),
}
