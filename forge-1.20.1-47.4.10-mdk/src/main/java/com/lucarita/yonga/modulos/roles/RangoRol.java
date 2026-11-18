package com.lucarita.yonga.modulos.roles;

import net.minecraft.network.chat.Component;

/**
 * Rangos dentro de un oficio, como los de los aldeanos.
 *
 * El rango sale de la experiencia de oficio que lleva el jugador. Cada salto cuesta unas 2,5-3 veces el
 * anterior (100, 300, 800, 2000): llegar a Maestro es largo. Cómo se gana la experiencia: ExperienciaOficio.
 */
public enum RangoRol {
    NOVATO("novato", 0),
    APRENDIZ("aprendiz", 100),
    OFICIAL("oficial", 400),
    EXPERTO("experto", 1200),
    MAESTRO("maestro", 3200);

    private final String id;
    private final int experienciaNecesaria;

    RangoRol(String id, int experienciaNecesaria) {
        this.id = id;
        this.experienciaNecesaria = experienciaNecesaria;
    }

    public String id() {
        return id;
    }

    /** Experiencia de oficio total necesaria para tener este rango. */
    public int experienciaNecesaria() {
        return experienciaNecesaria;
    }

    public Component nombre() {
        return Component.translatable("yonga.rol.rango." + id);
    }

    /** El rango siguiente, o null si ya es Maestro. */
    public RangoRol siguiente() {
        int i = ordinal() + 1;
        return i < values().length ? values()[i] : null;
    }

    public static RangoRol deExperiencia(int experiencia) {
        RangoRol resultado = NOVATO;
        for (RangoRol rango : values()) {
            if (experiencia >= rango.experienciaNecesaria) {
                resultado = rango;
            }
        }
        return resultado;
    }

    public static RangoRol porId(String id) {
        for (RangoRol rango : values()) {
            if (rango.id.equals(id)) {
                return rango;
            }
        }
        return null;
    }

    /** De 0.0 a 1.0: cuánto lleva hacia el siguiente rango. Maestro = 1. */
    public static float progreso(int experiencia) {
        RangoRol actual = deExperiencia(experiencia);
        RangoRol siguiente = actual.siguiente();
        if (siguiente == null) {
            return 1.0f;
        }
        float hecho = experiencia - actual.experienciaNecesaria;
        float total = siguiente.experienciaNecesaria - actual.experienciaNecesaria;
        return Math.min(1.0f, Math.max(0.0f, hecho / total));
    }
}
