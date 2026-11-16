package com.lucarita.yonga.modulos.fe;

import net.minecraft.network.chat.Component;

/**
 * Rangos de fe según el nivel. Los números son niveles, no puntos.
 * En cada límite, el nivel pertenece al rango más extremo (7 ya es Fiel, -10 ya es Apóstata).
 * Para cambiar un rango, toca sus dos números (desde, hasta) y su texto en los archivos de idioma.
 */
public enum RangoFe {
    // Positivos
    ATEO("ateo", 0, 6),
    FIEL("fiel", 7, 19),
    NOVICIO("novicio", 20, 39),
    SACERDOTE("sacerdote", 40, 59),
    PRELADO("prelado", 60, 79),
    SUMO_SACERDOTE("sumo_sacerdote", 80, 99),
    ILUMINADO("iluminado", 100, 100),

    // Negativos
    SECULAR("secular", -9, -1),
    APOSTATA("apostata", -29, -10),
    HEREJE("hereje", -49, -30),
    BLASFEMO("blasfemo", -69, -50),
    IRREDIMIBLE("irredimible", -99, -70),
    SIERVO_DE_LAS_SOMBRAS("siervo_de_las_sombras", -100, -100);

    private final String id;
    private final int desde;
    private final int hasta;

    RangoFe(String id, int desde, int hasta) {
        this.id = id;
        this.desde = desde;
        this.hasta = hasta;
    }

    public String id() {
        return id;
    }

    public Component nombre() {
        return Component.translatable("yonga.fe.rango." + id);
    }

    public static RangoFe deNivel(int nivel) {
        for (RangoFe rango : values()) {
            if (nivel >= rango.desde && nivel <= rango.hasta) {
                return rango;
            }
        }
        return ATEO;
    }
}
