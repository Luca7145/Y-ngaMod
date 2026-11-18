package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.core.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * La armónica como objeto: va en el hueco de cara (lo que hace está en Armonica).
 * Se gasta 1 de durabilidad por cada lamento; tocar notas y el blues no la gastan. Se repara con lucarita.
 */
public class ArmonicaItem extends Item {

    public static final int DURABILIDAD = 64;   // 64 lamentos

    public ArmonicaItem(Properties propiedades) {
        super(propiedades.durability(DURABILIDAD));
    }

    @Override
    public boolean isValidRepairItem(ItemStack armonica, ItemStack material) {
        return material.is(ModItems.LUCARITA.get());
    }
}
