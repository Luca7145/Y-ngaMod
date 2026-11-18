package com.lucarita.yonga.modulos.fe.mobs;

import com.lucarita.yonga.modulos.fe.FeDatos;
import com.lucarita.yonga.modulos.fe.RangoFe;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Mob de fe: un ser oscuro o luminoso, inferior o superior. Lo implementan todos los mobs de la parte de fe.
 *
 * A quién atacan (según el nivel de fe del jugador):
 *  - Oscuros inferiores:   del Ateo hacia arriba, Ateo incluido (nivel >= 0). A los negativos no les hacen nada.
 *  - Oscuros superiores:   además, a todos los que no lleguen a Blasfemo (nivel >= -49).
 *  - Luminosos inferiores: del Ateo hacia abajo, Ateo incluido (nivel <= 6).
 *  - Luminosos superiores: además, a todos los que no lleguen a Prelado (nivel <= 59).
 *
 * Al morir a manos de un jugador sueltan más fe que un mob normal: los oscuros suman y los luminosos restan
 * (ver OrbesFe).
 */
public interface MobFe {

    /** true = ser oscuro, false = ser luminoso. */
    boolean esOscuro();

    /** true = superior, false = inferior. */
    boolean esSuperior();

    /** ¿Este mob ataca a este jugador? (los creativos y espectadores ya los descarta Minecraft) */
    default boolean atacaA(LivingEntity objetivo) {
        return objetivo instanceof Player jugador && ataca(esOscuro(), esSuperior(), FeDatos.obtenerNivel(jugador));
    }

    static boolean ataca(boolean oscuro, boolean superior, int nivel) {
        if (oscuro) {
            return superior ? nivel > RangoFe.BLASFEMO.hasta() : nivel >= RangoFe.ATEO.desde();
        }
        return superior ? nivel < RangoFe.PRELADO.desde() : nivel <= RangoFe.ATEO.hasta();
    }
}
