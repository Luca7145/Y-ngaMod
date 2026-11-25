package com.lucarita.yonga.modulos.habitantes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Los oficios de los habitantes, propios de cada ÉPOCA (fase 7). Cada uno tiene:
 *   · su época y su ROL: el papel que cumple en la aldea, el mismo en todas las épocas (el "granjero" medieval es el
 *     "recolector" prehistórico; el "tabernero", el "guardián del fuego"). Sirve para pasar de una época a otra;
 *   · su PUESTO: los bloques donde trabaja (los mismos que los aldeanos, para aprovechar las aldeas);
 *   · lo que PRODUCE al llenar su barra de trabajo, por calidades del 1 al 5.
 * No compran nada a los jugadores: las monedas se ganan con los encargos de la taberna (fase 6).
 *
 * NIVEL (1 novato … 5 maestro): solo los adultos suben, comerciando; los jóvenes se quedan en el 1.
 * Cada nivel hace que la barra se llene antes y que las calidades altas salgan más a menudo (ver elegir()).
 *
 * Nunca hay objetos de rol en estas listas: no se pueden comerciar.
 * Los objetos se nombran por su id ("minecraft:pan" o "yonga:cigarro"); si no existe (otro módulo apagado), se salta.
 * Pociones: "minecraft:potion#minecraft:healing".
 */
public enum Oficio {

    GRANJERO("granjero", 20, "minecraft:iron_hoe", Set.of(Blocks.COMPOSTER),
            List.of(p("wheat", 12, 1, 1), p("carrot", 10, 1, 1), p("potato", 10, 1, 1),
                    p("beetroot", 10, 1, 2), p("pumpkin", 3, 1, 2), p("melon_slice", 12, 1, 2),
                    p("apple", 4, 1, 3), p("sweet_berries", 12, 1, 3),
                    p("golden_carrot", 3, 3, 4),
                    p("golden_apple", 1, 8, 5))),

    HERRERO("herrero", 10, "minecraft:iron_pickaxe",
            Set.of(Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL, Blocks.SMITHING_TABLE, Blocks.BLAST_FURNACE, Blocks.GRINDSTONE),
            List.of(p("iron_shovel", 1, 2, 1), p("iron_hoe", 1, 2, 1), p("shears", 1, 2, 1),
                    p("iron_axe", 1, 4, 2), p("iron_sword", 1, 4, 2), p("iron_pickaxe", 1, 5, 2), p("shield", 1, 3, 2),
                    p("chainmail_chestplate", 1, 6, 3), p("iron_helmet", 1, 5, 3), p("iron_boots", 1, 4, 3),
                    p("iron_leggings", 1, 7, 3), p("iron_chestplate", 1, 8, 3),
                    p("diamond_sword", 1, 14, 4), p("diamond_axe", 1, 14, 4), p("diamond_pickaxe", 1, 16, 4),
                    p("diamond_helmet", 1, 16, 5), p("diamond_chestplate", 1, 26, 5))),

    PANADERO("panadero", 10, "minecraft:bread", Set.of(Blocks.FURNACE),
            List.of(p("bread", 4, 1, 1), p("cookie", 10, 1, 2), p("pumpkin_pie", 3, 1, 3),
                    p("cake", 1, 3, 4), p("honey_bottle", 2, 2, 4))),

    CARNICERO("carnicero", 8, "minecraft:iron_axe", Set.of(Blocks.SMOKER),
            List.of(p("cooked_chicken", 4, 1, 1), p("cooked_rabbit", 4, 1, 1),
                    p("cooked_porkchop", 4, 1, 2), p("cooked_mutton", 4, 1, 2),
                    p("cooked_beef", 4, 2, 3), p("rabbit_stew", 1, 1, 3),
                    p("leather", 6, 2, 4))),

    PESCADOR("pescador", 8, "minecraft:fishing_rod", Set.of(Blocks.BARREL),
            List.of(p("cod", 8, 1, 1), p("cooked_cod", 6, 1, 1), p("cooked_salmon", 6, 1, 2), p("ink_sac", 6, 1, 2),
                    p("fishing_rod", 1, 2, 3), p("tropical_fish", 2, 2, 3),
                    p("nautilus_shell", 1, 6, 5))),

    LENADOR("lenador", 10, "minecraft:iron_axe", Set.of(Blocks.FLETCHING_TABLE),
            List.of(p("oak_log", 16, 1, 1), p("spruce_log", 16, 1, 1), p("birch_log", 16, 1, 1), p("stick", 32, 1, 1),
                    p("charcoal", 8, 1, 2), p("arrow", 16, 1, 2),
                    p("bow", 1, 2, 3), p("dark_oak_log", 16, 2, 3),
                    p("crossbow", 1, 3, 4),
                    p("spectral_arrow", 8, 3, 5))),

    CANTERO("cantero", 8, "minecraft:iron_pickaxe", Set.of(Blocks.STONECUTTER),
            List.of(p("stone_bricks", 16, 1, 1), p("bricks", 16, 1, 1),
                    p("polished_andesite", 16, 1, 2), p("polished_diorite", 16, 1, 2), p("polished_granite", 16, 1, 2),
                    p("chiseled_stone_bricks", 8, 1, 3), p("smooth_stone", 16, 1, 3),
                    p("quartz_block", 4, 2, 4),
                    p("quartz_pillar", 4, 3, 5))),

    TEJEDOR("tejedor", 8, "minecraft:shears", Set.of(Blocks.LOOM),
            List.of(p("white_wool", 8, 1, 1), p("white_carpet", 12, 1, 1),
                    p("brown_wool", 8, 1, 2), p("red_wool", 8, 1, 2), p("leather_chestplate", 1, 3, 2),
                    p("blue_wool", 8, 1, 3), p("white_bed", 1, 2, 3), p("painting", 2, 2, 3),
                    p("white_banner", 1, 3, 4),
                    p("saddle", 1, 6, 5))),

    CURANDERO("curandero", 6, null, Set.of(Blocks.BREWING_STAND),
            List.of(p("honey_bottle", 2, 2, 1), p("redstone", 4, 1, 1),
                    p("potion#minecraft:healing", 1, 3, 2), p("glowstone_dust", 4, 2, 2),
                    p("potion#minecraft:regeneration", 1, 5, 3), p("splash_potion#minecraft:healing", 1, 5, 3),
                    p("potion#minecraft:strong_healing", 1, 8, 4),
                    p("golden_apple", 1, 8, 5))),

    TABERNERO("tabernero", 4, null, Set.of(Blocks.CAULDRON),
            List.of(p("mushroom_stew", 1, 1, 1), p("beetroot_soup", 1, 1, 1), p("bread", 4, 1, 1),
                    p("honey_bottle", 2, 2, 2),
                    p("yonga:cigarro", 2, 2, 3), p("rabbit_stew", 1, 2, 3),
                    p("yonga:puro", 1, 4, 4), p("cake", 1, 3, 4),
                    p("golden_apple", 1, 9, 5))),

    // ==================================================================================================== PREHISTORIA
    // Sin metal ni vidrio (salvo lo "mágico" del chamán): piedra, hueso, piel, barro y lo que da el campo. Más barato.

    RECOLECTOR("recolector", Epoca.PREHISTORICA, "granjero", 20, "minecraft:wooden_hoe", Set.of(Blocks.COMPOSTER),
            List.of(p("sweet_berries", 12, 1, 1), p("apple", 4, 1, 1), p("brown_mushroom", 8, 1, 1),
                    p("red_mushroom", 6, 1, 2), p("carrot", 8, 1, 2), p("potato", 8, 1, 2), p("beetroot", 8, 1, 2),
                    p("melon_slice", 10, 1, 2),
                    p("glow_berries", 8, 1, 3), p("honeycomb", 3, 2, 3), p("pumpkin", 2, 1, 3),
                    p("torchflower_seeds", 1, 5, 4), p("pitcher_pod", 1, 5, 4),
                    p("sniffer_egg", 1, 14, 5))),

    CAZADOR("cazador", Epoca.PREHISTORICA, "carnicero", 10, "minecraft:bow", Set.of(Blocks.SMOKER),
            List.of(p("feather", 8, 1, 1), p("bone", 6, 1, 1), p("rabbit", 3, 1, 1), p("chicken", 3, 1, 1),
                    p("cooked_rabbit", 3, 1, 1),
                    p("mutton", 3, 1, 2), p("porkchop", 3, 1, 2), p("cooked_beef", 3, 2, 2), p("leather", 4, 2, 2),
                    p("rabbit_hide", 4, 1, 2),
                    p("cooked_porkchop", 4, 2, 3),
                    p("rabbit_foot", 1, 3, 4),
                    p("goat_horn", 1, 8, 5))),

    TALLADOR("tallador", Epoca.PREHISTORICA, "herrero", 10, "minecraft:stone_pickaxe",
            Set.of(Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL, Blocks.SMITHING_TABLE, Blocks.BLAST_FURNACE, Blocks.GRINDSTONE),
            List.of(p("flint", 8, 1, 1), p("stone_shovel", 1, 1, 1), p("stone_hoe", 1, 1, 1),
                    p("stone_axe", 1, 1, 2), p("stone_pickaxe", 1, 2, 2), p("stone_sword", 1, 2, 2), p("arrow", 12, 1, 2),
                    p("flint_and_steel", 1, 3, 3), p("bow", 1, 3, 3),
                    p("obsidian", 2, 4, 4),
                    p("trident", 1, 16, 5))),

    COCINERO("cocinero", Epoca.PREHISTORICA, "panadero", 10, null, Set.of(Blocks.FURNACE),
            List.of(p("baked_potato", 4, 1, 1), p("bread", 3, 1, 1), p("mushroom_stew", 1, 1, 1),
                    p("cooked_chicken", 3, 1, 2), p("beetroot_soup", 1, 1, 2), p("dried_kelp", 12, 1, 2),
                    p("cooked_mutton", 3, 2, 3), p("rabbit_stew", 1, 2, 3),
                    p("cooked_beef", 6, 3, 4),
                    p("rabbit_stew", 3, 4, 5))),

    ARPONERO("arponero", Epoca.PREHISTORICA, "pescador", 8, "minecraft:fishing_rod", Set.of(Blocks.BARREL),
            List.of(p("cod", 8, 1, 1), p("salmon", 6, 1, 1), p("kelp", 16, 1, 1),
                    p("cooked_cod", 4, 1, 2), p("cooked_salmon", 4, 1, 2), p("ink_sac", 4, 1, 2),
                    p("tropical_fish", 2, 2, 3), p("pufferfish", 1, 2, 3),
                    p("prismarine_shard", 2, 3, 4),
                    p("nautilus_shell", 1, 5, 5), p("scute", 1, 6, 5))),

    ARQUERO("arquero", Epoca.PREHISTORICA, "lenador", 10, "minecraft:stone_axe", Set.of(Blocks.FLETCHING_TABLE),
            List.of(p("stick", 16, 1, 1), p("oak_log", 12, 1, 1), p("birch_log", 12, 1, 1), p("spruce_log", 12, 1, 1),
                    p("arrow", 16, 1, 2), p("charcoal", 6, 1, 2),
                    p("bow", 1, 2, 3), p("dark_oak_log", 12, 2, 3),
                    p("tipped_arrow#minecraft:poison", 8, 3, 4),
                    p("tipped_arrow#minecraft:strong_poison", 8, 5, 5))),

    ALFARERO("alfarero", Epoca.PREHISTORICA, "cantero", 8, null, Set.of(Blocks.STONECUTTER),
            List.of(p("clay_ball", 8, 1, 1), p("brick", 8, 1, 1),
                    p("terracotta", 8, 1, 2), p("flower_pot", 2, 1, 2), p("bricks", 8, 1, 2),
                    p("decorated_pot", 1, 2, 3), p("orange_terracotta", 8, 1, 3), p("white_terracotta", 8, 1, 3),
                    p("brown_terracotta", 8, 1, 3),
                    p("archer_pottery_sherd", 1, 4, 4), p("prize_pottery_sherd", 1, 4, 4),
                    p("howl_pottery_sherd", 1, 6, 5), p("heart_pottery_sherd", 1, 6, 5))),

    CURTIDOR("curtidor", Epoca.PREHISTORICA, "tejedor", 8, "minecraft:shears", Set.of(Blocks.LOOM),
            List.of(p("leather", 3, 1, 1), p("rabbit_hide", 4, 1, 1), p("white_wool", 6, 1, 1),
                    p("leather_boots", 1, 2, 2), p("leather_helmet", 1, 2, 2), p("brown_wool", 8, 1, 2), p("string", 8, 1, 2),
                    p("leather_leggings", 1, 3, 3), p("leather_chestplate", 1, 3, 3),
                    p("white_bed", 1, 3, 4),
                    p("painting", 1, 5, 5))),

    CHAMAN("chaman", Epoca.PREHISTORICA, "curandero", 6, null, Set.of(Blocks.BREWING_STAND),
            List.of(p("honeycomb", 2, 2, 1), p("glow_berries", 4, 1, 1), p("brown_mushroom", 4, 1, 1),
                    p("suspicious_stew", 1, 2, 2), p("potion#minecraft:healing", 1, 3, 2),
                    p("glowstone_dust", 4, 2, 3), p("fermented_spider_eye", 2, 2, 3), p("potion#minecraft:regeneration", 1, 5, 3),
                    p("potion#minecraft:night_vision", 1, 4, 4),
                    p("ender_eye", 1, 8, 5))),

    GUARDIAN_FUEGO("guardian_fuego", Epoca.PREHISTORICA, "tabernero", 4, null, Set.of(Blocks.CAULDRON),
            List.of(p("mushroom_stew", 1, 1, 1), p("beetroot_soup", 1, 1, 1), p("baked_potato", 4, 1, 1),
                    p("cooked_mutton", 3, 1, 2), p("honeycomb", 2, 2, 2),
                    p("rabbit_stew", 1, 2, 3), p("cooked_beef", 4, 2, 3),
                    p("suspicious_stew", 1, 3, 4), p("glow_berries", 6, 2, 4),
                    p("golden_apple", 1, 8, 5))),

    // ==================================================================================================== ANTIGÜEDAD
    // Hispania romana: hierro y bronce, mármol, vino y aceite (que no hay: miel y fruta), la púrpura de lujo.

    AGRICULTOR("agricultor", Epoca.ANTIGUA, "granjero", 20, "minecraft:iron_hoe", Set.of(Blocks.COMPOSTER),
            List.of(p("wheat", 12, 1, 1), p("carrot", 10, 1, 1), p("beetroot", 10, 1, 1),
                    p("apple", 6, 1, 2), p("sweet_berries", 12, 1, 2), p("melon_slice", 12, 1, 2), p("pumpkin", 3, 1, 2),
                    p("honeycomb", 3, 2, 3), p("egg", 8, 1, 3),
                    p("golden_carrot", 3, 3, 4),
                    p("golden_apple", 1, 8, 5))),

    FORJADOR("forjador", Epoca.ANTIGUA, "herrero", 10, "minecraft:iron_pickaxe",
            Set.of(Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL, Blocks.SMITHING_TABLE, Blocks.BLAST_FURNACE, Blocks.GRINDSTONE),
            List.of(p("iron_shovel", 1, 2, 1), p("iron_hoe", 1, 2, 1), p("shears", 1, 2, 1), p("iron_nugget", 9, 1, 1),
                    p("iron_axe", 1, 4, 2), p("iron_sword", 1, 4, 2), p("iron_pickaxe", 1, 5, 2), p("shield", 1, 3, 2),
                    p("chainmail_chestplate", 1, 6, 3), p("chainmail_helmet", 1, 4, 3), p("iron_helmet", 1, 5, 3),
                    p("chainmail_leggings", 1, 6, 3),
                    p("iron_chestplate", 1, 8, 4), p("golden_helmet", 1, 8, 4),
                    p("golden_chestplate", 1, 14, 5), p("trident", 1, 18, 5))),

    MOLINERO("molinero", Epoca.ANTIGUA, "panadero", 10, "minecraft:bread", Set.of(Blocks.FURNACE),
            List.of(p("bread", 4, 1, 1), p("wheat", 8, 1, 1), p("cookie", 10, 1, 2), p("honey_bottle", 2, 2, 2),
                    p("pumpkin_pie", 3, 1, 3), p("sugar", 6, 1, 3),
                    p("cake", 1, 3, 4))),

    MATARIFE("matarife", Epoca.ANTIGUA, "carnicero", 8, "minecraft:iron_axe", Set.of(Blocks.SMOKER),
            List.of(p("cooked_chicken", 4, 1, 1), p("cooked_rabbit", 4, 1, 1), p("cooked_porkchop", 4, 1, 1),
                    p("cooked_mutton", 4, 1, 2), p("egg", 8, 1, 2),
                    p("cooked_beef", 4, 2, 3), p("rabbit_stew", 1, 1, 3),
                    p("leather", 6, 2, 4))),

    PESCADERO("pescadero", Epoca.ANTIGUA, "pescador", 8, "minecraft:fishing_rod", Set.of(Blocks.BARREL),
            List.of(p("cod", 8, 1, 1), p("cooked_cod", 6, 1, 1), p("salmon", 6, 1, 1),
                    p("cooked_salmon", 6, 1, 2), p("ink_sac", 6, 1, 2),
                    p("tropical_fish", 2, 2, 3), p("pufferfish", 1, 2, 3),
                    p("prismarine_shard", 3, 3, 4),
                    p("nautilus_shell", 1, 6, 5))),

    CARPINTERO("carpintero", Epoca.ANTIGUA, "lenador", 10, "minecraft:iron_axe", Set.of(Blocks.FLETCHING_TABLE),
            List.of(p("oak_planks", 16, 1, 1), p("stick", 32, 1, 1), p("oak_log", 16, 1, 1), p("birch_log", 16, 1, 1),
                    p("arrow", 16, 1, 2), p("oak_fence", 8, 1, 2), p("charcoal", 8, 1, 2),
                    p("bow", 1, 2, 3), p("chest", 1, 2, 3), p("ladder", 8, 1, 3),
                    p("oak_boat", 1, 3, 4),
                    p("crossbow", 1, 5, 5))),

    MARMOLISTA("marmolista", Epoca.ANTIGUA, "cantero", 8, "minecraft:iron_pickaxe", Set.of(Blocks.STONECUTTER),
            List.of(p("stone_bricks", 16, 1, 1), p("bricks", 16, 1, 1), p("smooth_stone", 16, 1, 1),
                    p("polished_diorite", 16, 1, 2), p("polished_andesite", 16, 1, 2), p("terracotta", 16, 1, 2),
                    p("quartz_block", 4, 2, 3), p("smooth_quartz", 4, 2, 3),
                    p("quartz_pillar", 4, 3, 4), p("quartz_stairs", 4, 3, 4),
                    p("chiseled_quartz_block", 2, 4, 5))),

    TINTORERO("tintorero", Epoca.ANTIGUA, "tejedor", 8, "minecraft:shears", Set.of(Blocks.LOOM),
            List.of(p("white_wool", 8, 1, 1), p("white_carpet", 12, 1, 1),
                    p("red_wool", 8, 1, 2), p("yellow_wool", 8, 1, 2), p("brown_wool", 8, 1, 2), p("leather_chestplate", 1, 3, 2),
                    p("blue_wool", 8, 2, 3), p("white_bed", 1, 2, 3), p("painting", 2, 2, 3),
                    p("red_banner", 1, 3, 4), p("magenta_wool", 8, 3, 4),
                    p("purple_wool", 8, 6, 5))),

    MEDICO("medico", Epoca.ANTIGUA, "curandero", 6, null, Set.of(Blocks.BREWING_STAND),
            List.of(p("honey_bottle", 2, 2, 1), p("glistering_melon_slice", 1, 2, 1),
                    p("potion#minecraft:healing", 1, 3, 2), p("glowstone_dust", 4, 2, 2),
                    p("potion#minecraft:regeneration", 1, 5, 3), p("splash_potion#minecraft:healing", 1, 5, 3),
                    p("potion#minecraft:strong_healing", 1, 8, 4), p("potion#minecraft:fire_resistance", 1, 6, 4),
                    p("golden_apple", 1, 8, 5))),

    POSADERO("posadero", Epoca.ANTIGUA, "tabernero", 4, null, Set.of(Blocks.CAULDRON),
            List.of(p("bread", 4, 1, 1), p("mushroom_stew", 1, 1, 1), p("beetroot_soup", 1, 1, 1),
                    p("honey_bottle", 2, 2, 2), p("cooked_porkchop", 3, 1, 2),
                    p("rabbit_stew", 1, 2, 3), p("glistering_melon_slice", 2, 3, 3),
                    p("cake", 1, 3, 4), p("sweet_berries", 16, 2, 4),
                    p("golden_apple", 1, 9, 5))),

    // ==================================================================================================== EDAD MODERNA
    // La España del siglo XVII: lo que llega de las Indias (cacao, patata, calabaza), los tercios, los galeones.

    LABRADOR("labrador", Epoca.MODERNA, "granjero", 20, "minecraft:iron_hoe", Set.of(Blocks.COMPOSTER),
            List.of(p("wheat", 12, 1, 1), p("potato", 10, 1, 1), p("carrot", 10, 1, 1),
                    p("beetroot", 10, 1, 2), p("pumpkin", 3, 1, 2), p("melon_slice", 12, 1, 2), p("egg", 8, 1, 2),
                    p("apple", 6, 1, 3), p("sugar_cane", 8, 1, 3), p("cocoa_beans", 6, 2, 3),
                    p("golden_carrot", 3, 3, 4),
                    p("golden_apple", 1, 8, 5))),

    ARMERO("armero", Epoca.MODERNA, "herrero", 10, "minecraft:iron_pickaxe",
            Set.of(Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL, Blocks.SMITHING_TABLE, Blocks.BLAST_FURNACE, Blocks.GRINDSTONE),
            List.of(p("iron_shovel", 1, 2, 1), p("iron_hoe", 1, 2, 1), p("shears", 1, 2, 1), p("bucket", 1, 2, 1),
                    p("iron_axe", 1, 4, 2), p("iron_sword", 1, 4, 2), p("iron_pickaxe", 1, 5, 2), p("lantern", 2, 2, 2),
                    p("iron_helmet", 1, 5, 3), p("iron_boots", 1, 4, 3), p("iron_chestplate", 1, 8, 3), p("crossbow", 1, 4, 3),
                    p("diamond_sword", 1, 14, 4), p("diamond_pickaxe", 1, 16, 4),
                    p("diamond_chestplate", 1, 26, 5))),

    CONFITERO("confitero", Epoca.MODERNA, "panadero", 10, "minecraft:bread", Set.of(Blocks.FURNACE),
            List.of(p("bread", 4, 1, 1), p("cookie", 10, 1, 1), p("sugar", 8, 1, 2), p("pumpkin_pie", 3, 1, 2),
                    p("cocoa_beans", 6, 2, 3), p("honey_bottle", 2, 2, 3),
                    p("cake", 1, 3, 4))),

    CHACINERO("chacinero", Epoca.MODERNA, "carnicero", 8, "minecraft:iron_axe", Set.of(Blocks.SMOKER),
            List.of(p("cooked_porkchop", 4, 1, 1), p("cooked_chicken", 4, 1, 1), p("porkchop", 6, 1, 1),
                    p("cooked_mutton", 4, 1, 2), p("cooked_rabbit", 4, 1, 2),
                    p("cooked_beef", 4, 2, 3), p("rabbit_stew", 1, 1, 3),
                    p("leather", 6, 2, 4))),

    MARINERO("marinero", Epoca.MODERNA, "pescador", 8, "minecraft:fishing_rod", Set.of(Blocks.BARREL),
            List.of(p("cod", 8, 1, 1), p("cooked_cod", 6, 1, 1), p("cooked_salmon", 6, 1, 2), p("ink_sac", 6, 1, 2),
                    p("fishing_rod", 1, 2, 2), p("tropical_fish", 2, 2, 3), p("map", 1, 3, 3),
                    p("compass", 1, 5, 4), p("spyglass", 1, 6, 4),
                    p("nautilus_shell", 1, 6, 5), p("heart_of_the_sea", 1, 24, 5))),

    EBANISTA("ebanista", Epoca.MODERNA, "lenador", 10, "minecraft:iron_axe", Set.of(Blocks.FLETCHING_TABLE),
            List.of(p("oak_planks", 16, 1, 1), p("spruce_planks", 16, 1, 1), p("stick", 32, 1, 1),
                    p("chest", 1, 2, 2), p("barrel", 1, 2, 2), p("oak_door", 1, 1, 2), p("arrow", 16, 1, 2),
                    p("bookshelf", 1, 3, 3), p("dark_oak_planks", 16, 2, 3), p("oak_boat", 1, 2, 3),
                    p("jukebox", 1, 6, 4), p("crossbow", 1, 4, 4),
                    p("note_block", 2, 4, 5))),

    ALARIFE("alarife", Epoca.MODERNA, "cantero", 8, "minecraft:iron_pickaxe", Set.of(Blocks.STONECUTTER),
            List.of(p("bricks", 16, 1, 1), p("stone_bricks", 16, 1, 1), p("cobblestone", 32, 1, 1),
                    p("polished_granite", 16, 1, 2), p("terracotta", 16, 1, 2), p("smooth_stone", 16, 1, 2),
                    p("white_glazed_terracotta", 8, 2, 3), p("blue_glazed_terracotta", 8, 2, 3),
                    p("quartz_block", 4, 2, 4), p("chiseled_stone_bricks", 8, 2, 4),
                    p("quartz_pillar", 4, 3, 5))),

    SASTRE("sastre", Epoca.MODERNA, "tejedor", 8, "minecraft:shears", Set.of(Blocks.LOOM),
            List.of(p("white_wool", 8, 1, 1), p("black_wool", 8, 1, 1), p("string", 8, 1, 1),
                    p("red_wool", 8, 1, 2), p("leather_chestplate", 1, 3, 2), p("leather_boots", 1, 2, 2),
                    p("white_bed", 1, 2, 3), p("painting", 2, 2, 3), p("leather_helmet", 1, 2, 3),
                    p("red_banner", 1, 3, 4), p("item_frame", 2, 2, 4),
                    p("saddle", 1, 6, 5))),

    BOTICARIO("boticario", Epoca.MODERNA, "curandero", 6, null, Set.of(Blocks.BREWING_STAND),
            List.of(p("honey_bottle", 2, 2, 1), p("sugar", 4, 1, 1), p("glass_bottle", 4, 1, 1),
                    p("potion#minecraft:healing", 1, 3, 2), p("glowstone_dust", 4, 2, 2), p("glistering_melon_slice", 2, 2, 2),
                    p("potion#minecraft:regeneration", 1, 5, 3), p("splash_potion#minecraft:healing", 1, 5, 3),
                    p("potion#minecraft:strong_healing", 1, 8, 4), p("potion#minecraft:swiftness", 1, 5, 4),
                    p("golden_apple", 1, 8, 5))),

    MESONERO("mesonero", Epoca.MODERNA, "tabernero", 4, null, Set.of(Blocks.CAULDRON),
            List.of(p("bread", 4, 1, 1), p("mushroom_stew", 1, 1, 1), p("baked_potato", 4, 1, 1),
                    p("cooked_porkchop", 3, 1, 2), p("honey_bottle", 2, 2, 2),
                    p("yonga:cigarro", 2, 2, 3), p("rabbit_stew", 1, 2, 3),
                    p("yonga:puro", 1, 4, 4), p("cake", 1, 3, 4),
                    p("golden_apple", 1, 9, 5))),

    // ==================================================================================================== ÉPOCA INDUSTRIAL
    // Hacia 1890: fábricas, ferrocarril, minas, latas de conserva, farmacias; el hierro barato y las primeras máquinas.

    HORTELANO("hortelano", Epoca.INDUSTRIAL, "granjero", 20, "minecraft:iron_hoe", Set.of(Blocks.COMPOSTER),
            List.of(p("wheat", 16, 1, 1), p("potato", 12, 1, 1), p("carrot", 12, 1, 1), p("beetroot", 12, 1, 1),
                    p("pumpkin", 4, 1, 2), p("melon_slice", 16, 1, 2), p("egg", 12, 1, 2), p("sugar_cane", 12, 1, 2),
                    p("apple", 8, 1, 3), p("cocoa_beans", 8, 2, 3), p("bone_meal", 12, 1, 3),
                    p("golden_carrot", 4, 3, 4),
                    p("golden_apple", 1, 8, 5))),

    FUNDIDOR("fundidor", Epoca.INDUSTRIAL, "herrero", 10, "minecraft:iron_pickaxe",
            Set.of(Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL, Blocks.SMITHING_TABLE, Blocks.BLAST_FURNACE, Blocks.GRINDSTONE),
            List.of(p("iron_ingot", 4, 2, 1), p("iron_bars", 16, 1, 1), p("chain", 8, 1, 1), p("bucket", 1, 1, 1),
                    p("rail", 16, 2, 2), p("lantern", 4, 2, 2), p("iron_pickaxe", 1, 4, 2), p("cauldron", 1, 2, 2),
                    p("minecart", 1, 4, 3), p("hopper", 1, 4, 3), p("iron_door", 1, 2, 3), p("clock", 1, 4, 3),
                    p("powered_rail", 6, 6, 4), p("piston", 2, 4, 4), p("anvil", 1, 8, 4),
                    p("diamond_pickaxe", 1, 14, 5), p("diamond_chestplate", 1, 24, 5))),

    TAHONERO("tahonero", Epoca.INDUSTRIAL, "panadero", 10, "minecraft:bread", Set.of(Blocks.FURNACE),
            List.of(p("bread", 6, 1, 1), p("cookie", 12, 1, 1), p("sugar", 8, 1, 2), p("pumpkin_pie", 3, 1, 2),
                    p("cocoa_beans", 6, 2, 3), p("honey_bottle", 2, 2, 3),
                    p("cake", 1, 3, 4))),

    TABLAJERO("tablajero", Epoca.INDUSTRIAL, "carnicero", 8, "minecraft:iron_axe", Set.of(Blocks.SMOKER),
            List.of(p("cooked_porkchop", 4, 1, 1), p("cooked_chicken", 4, 1, 1), p("cooked_beef", 4, 1, 1),
                    p("cooked_mutton", 4, 1, 2), p("cooked_rabbit", 4, 1, 2), p("porkchop", 8, 1, 2),
                    p("rabbit_stew", 1, 1, 3), p("leather", 6, 2, 3),
                    p("cooked_beef", 12, 3, 4))),

    CONSERVERO("conservero", Epoca.INDUSTRIAL, "pescador", 8, "minecraft:fishing_rod", Set.of(Blocks.BARREL),
            List.of(p("cooked_cod", 8, 1, 1), p("cooked_salmon", 8, 1, 1), p("dried_kelp", 16, 1, 1),
                    p("cod", 12, 1, 2), p("ink_sac", 6, 1, 2), p("glass_bottle", 4, 1, 2),
                    p("tropical_fish", 2, 2, 3), p("pufferfish", 1, 2, 3), p("glow_ink_sac", 3, 3, 3),
                    p("prismarine_shard", 4, 3, 4),
                    p("nautilus_shell", 1, 6, 5))),

    ASERRADOR("aserrador", Epoca.INDUSTRIAL, "lenador", 10, "minecraft:iron_axe", Set.of(Blocks.FLETCHING_TABLE),
            List.of(p("oak_planks", 32, 1, 1), p("spruce_planks", 32, 1, 1), p("birch_planks", 32, 1, 1),
                    p("stick", 32, 1, 1), p("oak_log", 16, 1, 2), p("charcoal", 12, 1, 2), p("ladder", 16, 1, 2),
                    p("chest", 2, 2, 3), p("barrel", 2, 2, 3), p("oak_trapdoor", 4, 1, 3), p("bookshelf", 1, 3, 3),
                    p("note_block", 2, 3, 4), p("jukebox", 1, 6, 4),
                    p("loom", 1, 4, 5))),

    ALBANIL("albanil", Epoca.INDUSTRIAL, "cantero", 8, "minecraft:iron_pickaxe", Set.of(Blocks.STONECUTTER),
            List.of(p("bricks", 32, 1, 1), p("stone_bricks", 32, 1, 1), p("gravel", 32, 1, 1), p("sand", 32, 1, 1),
                    p("glass", 16, 1, 2), p("glass_pane", 16, 1, 2), p("smooth_stone", 16, 1, 2),
                    p("gray_concrete", 16, 2, 3), p("white_concrete", 16, 2, 3), p("terracotta", 16, 1, 3),
                    p("polished_deepslate", 16, 2, 4), p("quartz_block", 8, 3, 4),
                    p("tinted_glass", 4, 4, 5))),

    HILANDERO("hilandero", Epoca.INDUSTRIAL, "tejedor", 8, "minecraft:shears", Set.of(Blocks.LOOM),
            List.of(p("white_wool", 16, 1, 1), p("string", 16, 1, 1), p("white_carpet", 16, 1, 1),
                    p("red_wool", 16, 1, 2), p("blue_wool", 16, 1, 2), p("black_wool", 16, 1, 2), p("gray_carpet", 16, 1, 2),
                    p("white_bed", 1, 2, 3), p("painting", 2, 2, 3), p("leather_chestplate", 1, 2, 3),
                    p("white_banner", 2, 3, 4), p("item_frame", 4, 2, 4),
                    p("saddle", 1, 5, 5))),

    FARMACEUTICO("farmaceutico", Epoca.INDUSTRIAL, "curandero", 6, null, Set.of(Blocks.BREWING_STAND),
            List.of(p("glass_bottle", 6, 1, 1), p("sugar", 6, 1, 1), p("honey_bottle", 2, 1, 1),
                    p("potion#minecraft:healing", 1, 2, 2), p("glowstone_dust", 6, 2, 2), p("glistering_melon_slice", 2, 2, 2),
                    p("potion#minecraft:regeneration", 1, 4, 3), p("splash_potion#minecraft:healing", 1, 4, 3),
                    p("potion#minecraft:strong_healing", 1, 6, 4), p("potion#minecraft:night_vision", 1, 5, 4),
                    p("potion#minecraft:strong_regeneration", 1, 9, 5), p("golden_apple", 1, 8, 5))),

    CANTINERO("cantinero", Epoca.INDUSTRIAL, "tabernero", 4, null, Set.of(Blocks.CAULDRON),
            List.of(p("bread", 4, 1, 1), p("mushroom_stew", 1, 1, 1), p("baked_potato", 6, 1, 1),
                    p("cooked_porkchop", 3, 1, 2), p("honey_bottle", 2, 2, 2), p("cookie", 8, 1, 2),
                    p("yonga:cigarro", 3, 2, 3), p("rabbit_stew", 1, 2, 3),
                    p("yonga:puro", 1, 3, 4), p("cake", 1, 3, 4),
                    p("golden_apple", 1, 9, 5))),

    // ==================================================================================================== HOY
    // Un pueblo de hoy: de todo y en cantidad, con lo más moderno (redstone, máquinas) en lo caro.

    ECOLOGICO("ecologico", Epoca.CONTEMPORANEA, "granjero", 20, "minecraft:diamond_hoe", Set.of(Blocks.COMPOSTER),
            List.of(p("wheat", 16, 1, 1), p("potato", 16, 1, 1), p("carrot", 16, 1, 1), p("beetroot", 16, 1, 1),
                    p("apple", 8, 1, 2), p("melon_slice", 16, 1, 2), p("egg", 12, 1, 2), p("sweet_berries", 16, 1, 2),
                    p("honeycomb", 4, 2, 3), p("cocoa_beans", 8, 2, 3), p("bone_meal", 16, 1, 3),
                    p("golden_carrot", 6, 3, 4), p("glow_berries", 12, 2, 4),
                    p("golden_apple", 1, 7, 5))),

    MECANICO("mecanico", Epoca.CONTEMPORANEA, "herrero", 10, "minecraft:iron_pickaxe",
            Set.of(Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL, Blocks.SMITHING_TABLE, Blocks.BLAST_FURNACE, Blocks.GRINDSTONE),
            List.of(p("iron_ingot", 6, 2, 1), p("redstone", 16, 1, 1), p("bucket", 1, 1, 1), p("shears", 1, 1, 1),
                    p("rail", 16, 2, 2), p("minecart", 1, 3, 2), p("lever", 4, 1, 2), p("redstone_torch", 8, 1, 2),
                    p("piston", 2, 3, 3), p("repeater", 4, 2, 3), p("comparator", 2, 3, 3), p("hopper", 1, 3, 3),
                    p("observer", 2, 4, 4), p("daylight_detector", 1, 3, 4), p("diamond_pickaxe", 1, 12, 4),
                    p("netherite_ingot", 1, 30, 5))),

    PASTELERO("pastelero", Epoca.CONTEMPORANEA, "panadero", 10, "minecraft:bread", Set.of(Blocks.FURNACE),
            List.of(p("bread", 6, 1, 1), p("cookie", 16, 1, 1), p("pumpkin_pie", 3, 1, 2), p("sugar", 12, 1, 2),
                    p("honey_bottle", 3, 2, 3), p("cocoa_beans", 8, 2, 3),
                    p("cake", 1, 2, 4))),

    CHARCUTERO("charcutero", Epoca.CONTEMPORANEA, "carnicero", 8, "minecraft:iron_axe", Set.of(Blocks.SMOKER),
            List.of(p("cooked_porkchop", 6, 1, 1), p("cooked_chicken", 6, 1, 1), p("cooked_beef", 4, 1, 1),
                    p("cooked_mutton", 6, 1, 2), p("cooked_rabbit", 6, 1, 2),
                    p("rabbit_stew", 2, 1, 3), p("leather", 8, 2, 3),
                    p("cooked_beef", 16, 3, 4))),

    MARISQUERO("marisquero", Epoca.CONTEMPORANEA, "pescador", 8, "minecraft:fishing_rod", Set.of(Blocks.BARREL),
            List.of(p("cooked_cod", 8, 1, 1), p("cooked_salmon", 8, 1, 1), p("dried_kelp", 16, 1, 1),
                    p("tropical_fish", 3, 1, 2), p("ink_sac", 6, 1, 2), p("glow_ink_sac", 4, 2, 2),
                    p("pufferfish", 2, 2, 3), p("sea_pickle", 4, 2, 3), p("turtle_helmet", 1, 6, 4),
                    p("prismarine_crystals", 4, 3, 4),
                    p("nautilus_shell", 1, 5, 5), p("heart_of_the_sea", 1, 20, 5))),

    MADERERO("maderero", Epoca.CONTEMPORANEA, "lenador", 10, "minecraft:diamond_axe", Set.of(Blocks.FLETCHING_TABLE),
            List.of(p("oak_planks", 32, 1, 1), p("spruce_planks", 32, 1, 1), p("birch_planks", 32, 1, 1),
                    p("cherry_planks", 16, 1, 2), p("bamboo_planks", 16, 1, 2), p("acacia_planks", 16, 1, 2),
                    p("dark_oak_planks", 16, 1, 3), p("mangrove_planks", 16, 2, 3), p("bookshelf", 2, 3, 3),
                    p("jukebox", 1, 4, 4), p("note_block", 4, 3, 4),
                    p("chiseled_bookshelf", 2, 5, 5))),

    CONSTRUCTOR("constructor", Epoca.CONTEMPORANEA, "cantero", 8, "minecraft:diamond_pickaxe", Set.of(Blocks.STONECUTTER),
            List.of(p("white_concrete", 32, 1, 1), p("gray_concrete", 32, 1, 1), p("glass", 32, 1, 1),
                    p("bricks", 32, 1, 2), p("smooth_stone", 32, 1, 2), p("polished_andesite", 32, 1, 2),
                    p("black_concrete", 16, 1, 3), p("quartz_block", 16, 2, 3), p("iron_bars", 16, 1, 3),
                    p("polished_blackstone_bricks", 16, 2, 4), p("sea_lantern", 4, 3, 4),
                    p("tinted_glass", 8, 3, 5))),

    MODISTA("modista", Epoca.CONTEMPORANEA, "tejedor", 8, "minecraft:shears", Set.of(Blocks.LOOM),
            List.of(p("white_wool", 16, 1, 1), p("black_wool", 16, 1, 1), p("string", 16, 1, 1),
                    p("pink_wool", 16, 1, 2), p("light_blue_wool", 16, 1, 2), p("leather_chestplate", 1, 2, 2),
                    p("leather_boots", 1, 1, 2), p("white_bed", 1, 2, 3), p("red_carpet", 16, 1, 3),
                    p("painting", 2, 2, 3), p("white_banner", 2, 2, 4), p("glow_item_frame", 4, 3, 4),
                    p("saddle", 1, 4, 5))),

    ENFERMERO("enfermero", Epoca.CONTEMPORANEA, "curandero", 6, null, Set.of(Blocks.BREWING_STAND),
            List.of(p("glass_bottle", 6, 1, 1), p("honey_bottle", 2, 1, 1), p("milk_bucket", 1, 1, 1),
                    p("potion#minecraft:healing", 1, 2, 2), p("splash_potion#minecraft:healing", 1, 3, 2),
                    p("potion#minecraft:regeneration", 1, 3, 3), p("potion#minecraft:fire_resistance", 1, 4, 3),
                    p("potion#minecraft:strong_healing", 1, 5, 4), p("potion#minecraft:water_breathing", 1, 4, 4),
                    p("potion#minecraft:strong_regeneration", 1, 8, 5), p("golden_apple", 1, 7, 5))),

    CAMARERO("camarero", Epoca.CONTEMPORANEA, "tabernero", 4, null, Set.of(Blocks.CAULDRON),
            List.of(p("bread", 4, 1, 1), p("baked_potato", 6, 1, 1), p("cookie", 8, 1, 1),
                    p("cooked_porkchop", 3, 1, 2), p("mushroom_stew", 1, 1, 2), p("honey_bottle", 2, 2, 2),
                    p("rabbit_stew", 1, 2, 3), p("pumpkin_pie", 2, 2, 3),
                    p("cake", 1, 2, 4), p("yonga:cigarro", 3, 2, 4),
                    p("golden_apple", 1, 8, 5)));

    /** Un lote que produce: cantidad de objetos, su precio en monedas y su calidad (1..5). */
    public record Producto(String id, int cantidad, int precio, int calidad) {
        public ItemStack crear() {
            return Oficio.crear(id, cantidad);
        }
    }

    public static final String[] NIVELES = {"novato", "aprendiz", "oficial", "experto", "maestro"};
    /** Experiencia total para llegar a cada nivel (como los aldeanos). */
    public static final int[] XP_NIVEL = {0, 10, 70, 150, 250};
    /** Peso de cada calidad a nivel 1; cada nivel multiplica las altas (ver elegir). */
    private static final int[] PESO_CALIDAD = {60, 25, 10, 4, 1};

    private final String id;
    private final Epoca epoca;
    private final String rol;
    private final int frecuencia;
    @Nullable private final String herramienta;
    private final Set<Block> puestos;
    private final List<Producto> productos;

    /** Los medievales: su rol es su propio id. */
    Oficio(String id, int frecuencia, @Nullable String herramienta, Set<Block> puestos, List<Producto> productos) {
        this(id, Epoca.MEDIEVAL, id, frecuencia, herramienta, puestos, productos);
    }

    Oficio(String id, Epoca epoca, String rol, int frecuencia, @Nullable String herramienta, Set<Block> puestos,
           List<Producto> productos) {
        this.id = id;
        this.epoca = epoca;
        this.rol = rol;
        this.frecuencia = frecuencia;
        this.herramienta = herramienta;
        this.puestos = puestos;
        this.productos = productos;
    }

    private static Producto p(String id, int cantidad, int precio, int calidad) {
        return new Producto(id, cantidad, precio, calidad);
    }

    public String id() {
        return id;
    }

    public Epoca epoca() {
        return epoca;
    }

    /** El papel en la aldea (el id del oficio medieval equivalente): "granjero", "tabernero"… */
    public String rol() {
        return rol;
    }

    public boolean esTabernero() {
        return rol.equals("tabernero");
    }

    /** Lo que es más parecido a una época (las que aún no tienen oficios propios usan los medievales). */
    public static Epoca epocaDeOficios(Epoca e) {
        for (Oficio o : values()) if (o.epoca == e) return e;
        return Epoca.MEDIEVAL;
    }

    /** El oficio con este rol en otra época (o este mismo, si allí no hay). */
    public Oficio enEpoca(Epoca e) {
        Epoca destino = epocaDeOficios(e);
        if (destino == this.epoca) return this;
        for (Oficio o : values()) if (o.epoca == destino && o.rol.equals(this.rol)) return o;
        return this;
    }

    /** El tabernero de cada época (el que lleva la taberna y los encargos). */
    public static Oficio tabernero(Epoca e) {
        return TABERNERO.enEpoca(e);
    }

    public boolean esPuesto(BlockState estado) {
        return puestos.contains(estado.getBlock());
    }

    public ItemStack herramienta() {
        return herramienta == null ? ItemStack.EMPTY : crear(herramienta, 1);
    }

    /**
     * Elige qué produce. Peso de cada calidad c a nivel n: PESO_CALIDAD[c] x (1 + 0,6·(n−1))^(c−1).
     * A nivel 1 sale casi siempre lo básico; a nivel 5 las calidades 3-5 salen más de la mitad de las veces.
     */
    public Producto elegir(int nivel, RandomSource azar) {
        double factor = 1.0 + 0.6 * (nivel - 1);
        double total = 0;
        double[] pesos = new double[productos.size()];
        for (int i = 0; i < productos.size(); i++) {
            Producto p = productos.get(i);
            int c = Math.max(1, Math.min(5, p.calidad()));
            pesos[i] = existe(p.id()) ? PESO_CALIDAD[c - 1] * Math.pow(factor, c - 1) : 0;
            total += pesos[i];
        }
        double r = azar.nextDouble() * total;
        for (int i = 0; i < pesos.length; i++) {
            r -= pesos[i];
            if (r < 0) return productos.get(i);
        }
        return productos.get(0);
    }

    /** Un oficio de su época al azar (según lo frecuente que es cada uno). */
    public static Oficio alAzar(Epoca epoca, RandomSource azar) {
        Epoca e = epocaDeOficios(epoca);
        int total = 0;
        for (Oficio o : values()) if (o.epoca == e) total += o.frecuencia;
        int r = azar.nextInt(total);
        for (Oficio o : values()) {
            if (o.epoca != e) continue;
            r -= o.frecuencia;
            if (r < 0) return o;
        }
        return GRANJERO.enEpoca(e);
    }

    @Nullable
    public static Oficio porId(String id) {
        for (Oficio o : values()) {
            if (o.id.equals(id)) return o;
        }
        return null;
    }

    // ---------- objetos por id ----------

    private static boolean existe(String id) {
        return !crear(id, 1).isEmpty();
    }

    static ItemStack crear(String id, int cantidad) {
        String objeto = id, pocion = null;
        int almohadilla = id.indexOf('#');
        if (almohadilla >= 0) {
            objeto = id.substring(0, almohadilla);
            pocion = id.substring(almohadilla + 1);
        }
        ResourceLocation rl = objeto.contains(":") ? new ResourceLocation(objeto) : new ResourceLocation("minecraft", objeto);
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item, cantidad);
        if (pocion != null) {
            Potion p = ForgeRegistries.POTIONS.getValue(new ResourceLocation(pocion));
            if (p != null) PotionUtils.setPotion(stack, p);
        }
        return stack;
    }
}
