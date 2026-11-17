package com.lucarita.yonga.modulos.roles.cara;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Un medallón: se pone en el hueco de cara y da sus efectos mientras se lleva (ver Cara). */
public class MedallonItem extends Item {

    private final Medallon medallon;

    public MedallonItem(Medallon medallon, Properties propiedades) {
        super(propiedades.stacksTo(1));
        this.medallon = medallon;
    }

    public Medallon medallon() {
        return medallon;
    }

    // Brillo de objeto encantado
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
