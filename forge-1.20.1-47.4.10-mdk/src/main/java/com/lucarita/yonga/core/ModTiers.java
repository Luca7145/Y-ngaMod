package com.lucarita.yonga.core;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;

public class ModTiers {
    public static final Tier LUCARITA = new ForgeTier(
            3,                                              // Nivel de minado (3 = Diamante/Netherite)
            2300,                                           // Durabilidad (Usos)
            8.0f,                                           // Velocidad de picado
            4.0f,                                           // Daño base del material
            15,                                             // Encantabilidad
            BlockTags.NEEDS_DIAMOND_TOOL,                   // <-- Cambiado aquí
            () -> Ingredient.of(ModItems.LUCARITA.get())    // Material de reparación en yunque
    );
}