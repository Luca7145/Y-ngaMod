package com.lucarita.yonga.modulos.fe;

import net.minecraft.network.chat.Component;

/**
 * Rangos de fe según el nivel. Los números son niveles, no puntos.
 * En cada límite, el nivel pertenece al rango más extremo (7 ya es Fiel, -10 ya es Apóstata).
 * Para cambiar un rango, toca sus números y su texto en los archivos de idioma.
 *
 * El último número es el maná máximo del rango. Los rangos positivos tienen maná (azul) y los negativos
 * maná oscuro (negro). El Ateo no tiene. Nunca se tienen los dos a la vez.
 */
public enum RangoFe {
    //                                      desde, hasta, maná
    // Positivos -> maná
    ATEO("ateo", 0, 6, 0),
    FIEL("fiel", 7, 19, 100),
    NOVICIO("novicio", 20, 39, 100),
    SACERDOTE("sacerdote", 40, 59, 200),
    PRELADO("prelado", 60, 79, 200),
    SUMO_SACERDOTE("sumo_sacerdote", 80, 99, 300),
    ILUMINADO("iluminado", 100, 100, 500),

    // Negativos -> maná oscuro
    SECULAR("secular", -9, -1, 100),
    APOSTATA("apostata", -29, -10, 100),
    HEREJE("hereje", -49, -30, 200),
    BLASFEMO("blasfemo", -69, -50, 200),
    IRREDIMIBLE("irredimible", -99, -70, 300),
    SIERVO_DE_LAS_SOMBRAS("siervo_de_las_sombras", -100, -100, 500);

    private final String id;
    private final int desde;
    private final int hasta;
    private final int manaMaximo;

    RangoFe(String id, int desde, int hasta, int manaMaximo) {
        this.id = id;
        this.desde = desde;
        this.hasta = hasta;
        this.manaMaximo = manaMaximo;
    }

    public String id() {
        return id;
    }

    public Component nombre() {
        return Component.translatable("yonga.fe.rango." + id);
    }

    /** Nivel más bajo del rango. */
    public int desde() {
        return desde;
    }

    /** Nivel más alto del rango. */
    public int hasta() {
        return hasta;
    }

    /** Maná (o maná oscuro) máximo de este rango. 0 = sin barra. */
    public int manaMaximo() {
        return manaMaximo;
    }

    /** Qué tipo de maná tiene este rango. */
    public TipoMana tipoMana() {
        if (manaMaximo <= 0) {
            return TipoMana.NINGUNO;
        }
        return desde > 0 ? TipoMana.LUZ : TipoMana.OSCURO;
    }

    public static RangoFe deNivel(int nivel) {
        for (RangoFe rango : values()) {
            if (nivel >= rango.desde && nivel <= rango.hasta) {
                return rango;
            }
        }
        return ATEO;
    }

    public static RangoFe dePuntos(double puntos) {
        return deNivel(FeNiveles.nivel(puntos));
    }
}
