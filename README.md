# 🏝️ Sky Blocks

Turns a Minecraft world into Sky Blocks — you start on a tiny island floating in
an empty void, with a tree, a chest, and nothing else.

**Minecraft 26.1.2 · Fabric**

## Playing it

1. **Create New World**
2. Click **World Type** until it reads **Sky Blocks**
3. Create

Make sure the game mode is **Survival** — this mod does nothing in Creative.

## What you get

A **5×5 grass island** with an **oak tree**, and — across a gap of empty void —
a second island holding a **cobblestone minion** and a **portal to the Hub**.
There is no bridge, so getting to them means digging up your own island and
bridging across. The same start as Hypixel's private island.

There is **no starting chest** on your island. No lava, no ice, no seeds. Just
the tree.

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

## Multiplayer

Everybody gets an island of their own. Islands sit in slots along a row heading
east — slot 0 at `x=0`, slot 1 at `x=1000`, and so on — and a player is handed
the next free slot the first time they join. The slot is saved on them, so they
come back to the same island forever, and to the same one after dying.

A row rather than a grid because the Hub sits at `z=+1000` and the Arena at
`z=-1000`; growing along z would eventually drop an island on top of them.

Portals work on anyone's island, not just your own, so stepping into a friend's
portal takes you to the Hub rather than doing nothing. The way home always goes
to *your* island.

### Running a server

A server has no Create World screen, so the world type has to be named in
`server.properties` instead:

```properties
level-type=skyblocks:sky_blocks
```

Without that line the server generates an ordinary world, the emptiness check
finds ground, and the mod correctly does nothing at all — which looks exactly
like it being broken.

## It won't touch a world you already have

An island only appears if that stretch of world is genuinely empty — the mod
checks every block from bedrock to sky down the column it would build in before
placing anything. Join a normal world with this installed and nothing happens at
all. Once an island exists, that check stops matching, so it never builds twice.

The Nether and the End are left normal, so portals work once you get that far.

## How it's put together

Two halves, and only one of them is code:

| | |
|---|---|
| `data/skyblocks/worldgen/world_preset/sky_blocks.json` | the empty world — a flat generator with no layers at all |
| `data/minecraft/tags/worldgen/world_preset/normal.json` | adds it to the world type list on the Create World screen |
| `SkyBlocksMod.java` | detects an empty world on join and places the island |
| `Islands.java` | who owns which island, and where it goes |
| `Island.java` | the platform, the tree, the minion and the portal across the gap |
| `Portals.java` | the doorways, and watching for somebody standing in one |
| `Hub.java` · `Arena.java` | the shared plaza and the room monsters spawn in |
| `Structures.java` | places a Hub built by hand instead of one written in Java |

### Building the Hub by hand

Scenery is the one thing code is bad at — every lamp is a line of Java you can't
see without launching the game. So the Hub can come from a structure file
instead. Build it in a Creative world, save it with a Structure Block named
`skyblocks:hub`, and copy the result from that world's `generated/` folder to
`src/main/resources/data/skyblocks/structure/hub.nbt`.

Ship no file and the plaza in `Hub.java` is used instead, so this is entirely
optional.

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
