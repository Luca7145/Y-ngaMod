package com.lucarita.yonga.modulos.fe;

import net.minecraft.network.chat.Component;

/** Los dos tipos de maná: el azul (rangos positivos) y el oscuro (rangos negativos). */
public enum TipoMana {
    NINGUNO("ninguno"),
    LUZ("luz"),        // maná, azul
    OSCURO("oscuro");  // maná oscuro, negro

    private final String id;

    TipoMana(String id) {
        this.id = id;
    }

    public Component nombre() {
        return Component.translatable("yonga.fe.mana." + id);
    }
}
