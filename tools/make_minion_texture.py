#!/usr/bin/env python3
"""Draws the Cobblestone Minion block texture.

No image library on this machine, so the PNG is written by hand. The look is a
grey stone machine with a lit face, so it reads as a working thing rather than
just another cobblestone block.

Run from the project root:  python3 tools/make_minion_texture.py
"""

import os
import struct
import zlib

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   "..", "src/main/resources/assets/skyblocks/textures/block")

# One texture per axe tier, tinted by the axe's own material so you can tell a
# fast minion from a slow one at a glance.
TIERS = {
    "wooden":    (74, 78, 86),
    "stone":     (108, 108, 108),
    "golden":    (188, 152, 48),
    "iron":      (196, 196, 200),
    "diamond":   (68, 200, 196),
    "netherite": (58, 48, 52),
}

STONE = (128, 128, 128)
STONE_DK = (96, 96, 96)
STONE_LT = (160, 160, 160)
PLATE = (74, 78, 86)
EYE = (96, 220, 255)
EYE_DK = (40, 140, 190)

# S stone · d dark · l light · P plate · E eye · e dim eye
ART = [
    "llllllllllllllll",
    "lSSSSSSSSSSSSSSd",
    "lSSdSSSSSSdSSSSd",
    "lSPPPPPPPPPPPPSd",
    "lSPEEPPPPPPEEPSd",
    "lSPEEPPPPPPEEPSd",
    "lSPPPPPPPPPPPPSd",
    "lSPPPPeeeePPPPSd",
    "lSPPPPeeeePPPPSd",
    "lSPPPPPPPPPPPPSd",
    "lSSPPPPPPPPPPSSd",
    "lSSSdSSSSSSSSSSd",
    "lSSSSSSSSdSSSSSd",
    "lSSSSSSSSSSSSSSd",
    "dSSSSSSSSSSSSSSd",
    "dddddddddddddddd",
]

PALETTE = {"S": STONE, "d": STONE_DK, "l": STONE_LT, "P": PLATE, "E": EYE, "e": EYE_DK}


def one(plate):
    palette = dict(PALETTE, P=plate)
    return [[palette[ch] + (255,) for ch in row] for row in ART]


def write(name, grid):
    raw = b"".join(b"\x00" + b"".join(struct.pack("BBBB", *px) for px in row) for row in grid)

    def chunk(tag, body):
        blob = tag + body
        return struct.pack(">I", len(body)) + blob + struct.pack(">I", zlib.crc32(blob))

    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, name + ".png"), "wb") as fh:
        fh.write(b"\x89PNG\r\n\x1a\n"
                 + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
                 + chunk(b"IDAT", zlib.compress(raw, 9))
                 + chunk(b"IEND", b""))

def main():
    for tier, plate in TIERS.items():
        write("%s_cobblestone_minion" % tier, one(plate))
    print("wrote %d minion textures (16x16)" % len(TIERS))


if __name__ == "__main__":
    main()
