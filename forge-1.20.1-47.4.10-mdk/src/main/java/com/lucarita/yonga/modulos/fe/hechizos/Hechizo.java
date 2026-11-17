package com.lucarita.yonga.modulos.fe.hechizos;

import com.lucarita.yonga.modulos.fe.RangoFe;
import com.lucarita.yonga.modulos.fe.TipoMana;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Los hechizos que se lanzan con los libros.
 *
 * Cada uno tiene: el tipo de maná que gasta (los de luz no funcionan con maná oscuro y al revés),
 * cuánto gasta, su enfriamiento (en ticks, 20 = 1 segundo), el rango de fe desde el que se puede
 * fabricar su libro, y qué hace (ver Hechizos).
 *
 * Para añadir uno nuevo: una línea aquí, su método en Hechizos, su libro en ModuloFe, su receta y sus textos.
 */
public enum Hechizo {
    //                                 maná,        coste, enfriamiento, fabricable desde
    GRAN_SALTO("gran_salto",           TipoMana.LUZ,    30, 10 * 20, RangoFe.NOVICIO,  Hechizos::granSalto),
    CASTIGO_DEL_CIELO("castigo_del_cielo", TipoMana.LUZ, 100, 30 * 20, RangoFe.PRELADO, Hechizos::castigoDelCielo),
    EXPULSION("expulsion",             TipoMana.OSCURO, 30, 10 * 20, RangoFe.APOSTATA, Hechizos::expulsion),
    PROYECTIL_OSCURO("proyectil_oscuro", TipoMana.OSCURO, 100, 30 * 20, RangoFe.BLASFEMO, Hechizos::proyectilOscuro);

    /** Lo que hace el hechizo. Devuelve false si no se ha podido lanzar (y entonces no gasta maná). */
    @FunctionalInterface
    public interface Efecto {
        boolean lanzar(ServerPlayer jugador);
    }

    private final String id;
    private final TipoMana tipo;
    private final int coste;
    private final int enfriamiento;
    private final RangoFe rangoMinimo;
    private final Efecto efecto;

    Hechizo(String id, TipoMana tipo, int coste, int enfriamiento, RangoFe rangoMinimo, Efecto efecto) {
        this.id = id;
        this.tipo = tipo;
        this.coste = coste;
        this.enfriamiento = enfriamiento;
        this.rangoMinimo = rangoMinimo;
        this.efecto = efecto;
    }

    public String id() { return id; }
    public TipoMana tipo() { return tipo; }
    public int coste() { return coste; }
    public int enfriamiento() { return enfriamiento; }
    public RangoFe rangoMinimo() { return rangoMinimo; }
    public Efecto efecto() { return efecto; }

    public Component nombre() {
        return Component.translatable("yonga.hechizo." + id);
    }

    /**
     * ¿Puede alguien con este nivel de fe fabricar el libro? "Desde" un rango significa ese rango o uno más extremo:
     * para los de luz, nivel igual o mayor que el principio del rango; para los oscuros, igual o menor.
     */
    public boolean puedeFabricar(int nivelFe) {
        if (tipo == TipoMana.OSCURO) {
            return nivelFe <= rangoMinimo.hasta();
        }
        return nivelFe >= rangoMinimo.desde();
    }
}
