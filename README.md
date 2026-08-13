# 🏝️ Sky Blocks

Turns a Minecraft world into Sky Blocks — you start on a tiny island floating in
an empty void, with a tree, a chest, and nothing else.

**Minecraft 26.1.2 · Fabric**

## Playing it

1. **Create New World**
2. Click **World Type** until it reads **Sky Blocks**
3. Create

You land on a 5×5 dirt platform with an oak tree and a chest.

## What you get

A **5×5 grass island** with an **oak tree**, a walkway, and a **portal ten
blocks away** that takes you to the **Hub** — the same start as Hypixel's
private island.

There is **no starting chest**. No lava, no ice, no seeds. Just the tree.

## The Hub

Step into the portal and you arrive at a stone plaza with lamps, four market
stalls, and a portal back to your island. It's built the first time somebody
goes through, so a world where you never use the portal stays empty.

## Survival only

In Creative or Hardcore this mod does nothing at all — no island, no portals,
no commands. Creative would defeat the point of a game about having nothing.

## Commands

| | |
|---|---|
| `/skyblock island` | build a fresh island where you're standing |

## It won't touch a world you already have

The island only appears if the world is genuinely empty — the mod checks every
block from bedrock to sky at spawn before placing anything. Join a normal world
with this installed and nothing happens at all. Once an island exists, that
check stops matching, so it never builds twice.

The Nether and the End are left normal, so portals work once you get that far.

## How it's put together

Two halves, and only one of them is code:

| | |
|---|---|
| `data/skyblocks/worldgen/world_preset/sky_blocks.json` | the empty world — a flat generator with no layers at all |
| `data/minecraft/tags/worldgen/world_preset/normal.json` | adds it to the world type list on the Create World screen |
| `SkyBlocksMod.java` | detects an empty world on join and places the island |
| `Island.java` | the platform, the tree and the chest |

## Building it yourself

This machine has no system JDK, so Gradle uses the one bundled with the
Minecraft launcher:

```bash
export JAVA_HOME="$HOME/Library/Application Support/minecraft/runtime/java-runtime-epsilon/mac-os/java-runtime-epsilon/jre.bundle/Contents/Home"
./gradlew build
```

The jar lands in `build/libs/`.

Made by [@cmadisons](https://github.com/cmadisons) ·
part of the [Starbr0 Arcade](https://cmadisons.github.io/arcade/)
