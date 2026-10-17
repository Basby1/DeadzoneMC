package de.deadzonemc.welt.gebaeude;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Random;

/** Baustoffe eines Gebäudes. */
public record Fassade(String name, Block wand, Block alt, Block ecke, Block boden, Block dachTreppe, Block dachVoll,
                      Block dachStufe, Block sockel, Block tuer, Block zaun, Block fensterladen) {

    public static final Fassade ZIEGEL = new Fassade("ziegel", Blocks.BRICKS, Blocks.PACKED_MUD,
            Blocks.STRIPPED_DARK_OAK_LOG, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_PLANKS,
            Blocks.DARK_OAK_SLAB, Blocks.COBBLESTONE, Blocks.DARK_OAK_DOOR, Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_TRAPDOOR);
    public static final Fassade PUTZ_WEISS = new Fassade("putz_weiss", Blocks.WHITE_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA,
            Blocks.POLISHED_ANDESITE, Blocks.OAK_PLANKS, Blocks.BRICK_STAIRS, Blocks.BRICKS,
            Blocks.BRICK_SLAB, Blocks.STONE_BRICKS, Blocks.SPRUCE_DOOR, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_TRAPDOOR);
    public static final Fassade PUTZ_GELB = new Fassade("putz_gelb", Blocks.SMOOTH_SANDSTONE, Blocks.SANDSTONE,
            Blocks.STRIPPED_OAK_LOG, Blocks.BIRCH_PLANKS, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES,
            Blocks.DEEPSLATE_TILE_SLAB, Blocks.STONE_BRICKS, Blocks.OAK_DOOR, Blocks.OAK_FENCE, Blocks.OAK_TRAPDOOR);
    public static final Fassade STEIN = new Fassade("stein", Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS,
            Blocks.POLISHED_ANDESITE, Blocks.DARK_OAK_PLANKS, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES,
            Blocks.DEEPSLATE_TILE_SLAB, Blocks.COBBLESTONE, Blocks.SPRUCE_DOOR, Blocks.COBBLESTONE_WALL, Blocks.SPRUCE_TRAPDOOR);
    public static final Fassade HOLZ = new Fassade("holz", Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS,
            Blocks.STRIPPED_SPRUCE_LOG, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_PLANKS,
            Blocks.SPRUCE_SLAB, Blocks.COBBLESTONE, Blocks.SPRUCE_DOOR, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_TRAPDOOR);
    public static final Fassade KLINKER = new Fassade("klinker", Blocks.MUD_BRICKS, Blocks.PACKED_MUD,
            Blocks.STRIPPED_OAK_LOG, Blocks.OAK_PLANKS, Blocks.MUD_BRICK_STAIRS, Blocks.MUD_BRICKS,
            Blocks.MUD_BRICK_SLAB, Blocks.BRICKS, Blocks.OAK_DOOR, Blocks.OAK_FENCE, Blocks.OAK_TRAPDOOR);
    public static final Fassade BETON = new Fassade("beton", Blocks.LIGHT_GRAY_CONCRETE, Blocks.ANDESITE,
            Blocks.STONE_BRICKS, Blocks.BIRCH_PLANKS, Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICKS,
            Blocks.STONE_BRICK_SLAB, Blocks.STONE_BRICKS, Blocks.BIRCH_DOOR, Blocks.COBBLESTONE_WALL, Blocks.IRON_TRAPDOOR);
    public static final Fassade FACHWERK = new Fassade("fachwerk", Blocks.WHITE_TERRACOTTA, Blocks.CALCITE,
            Blocks.DARK_OAK_LOG, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_PLANKS,
            Blocks.DARK_OAK_SLAB, Blocks.COBBLESTONE, Blocks.DARK_OAK_DOOR, Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_TRAPDOOR);
    public static final Fassade ROT = new Fassade("rot", Blocks.RED_TERRACOTTA, Blocks.TERRACOTTA,
            Blocks.STRIPPED_DARK_OAK_LOG, Blocks.SPRUCE_PLANKS, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES,
            Blocks.DEEPSLATE_TILE_SLAB, Blocks.STONE_BRICKS, Blocks.SPRUCE_DOOR, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_TRAPDOOR);

    public static final Fassade[] WOHNHAUS = {ZIEGEL, PUTZ_WEISS, PUTZ_GELB, STEIN, HOLZ, KLINKER, ROT};

    public static Fassade wohnhaus(Random r) {
        return WOHNHAUS[r.nextInt(WOHNHAUS.length)];
    }
}
