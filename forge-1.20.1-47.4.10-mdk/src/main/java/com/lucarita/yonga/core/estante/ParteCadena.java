package com.lucarita.yonga.core.estante;

import net.minecraft.util.StringRepresentable;

/**
 * La propiedad "side_chain" del estante: cómo está unido a los de al lado cuando están activados (hasta 3 seguidos).
 * Izquierda y derecha, tal como se ven de frente.
 */
public enum ParteCadena implements StringRepresentable {
    UNCONNECTED("unconnected"),
    LEFT("left"),
    CENTER("center"),
    RIGHT("right");

    private final String nombre;

    ParteCadena(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String getSerializedName() {
        return nombre;
    }
}
