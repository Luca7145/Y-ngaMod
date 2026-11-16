package com.lucarita.yonga.modulos.fe;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

/**
 * Orbes de fe al matar mobs:
 *  - Monstruos (zombis, esqueletos, slimes, ghasts...): orbes AZULES que suman fe.
 *  - El resto (animales, aldeanos, gólems, peces, murciélagos...): orbes NEGROS que restan fe.
 * En total valen tantos puntos de fe como vida máxima tenía el mob (vaca 10, zombi 20, gólem de hierro 100...).
 *
 * Como con la experiencia, solo los suelta si lo ha matado un jugador.
 */
public final class OrbesFe {

    // Puntos máximos por orbe: un mob de 20 de vida suelta 2 orbes de 10
    private static final float VALOR_MAXIMO_POR_ORBE = 10.0f;

    private OrbesFe() {}

    public static void alMorir(LivingDeathEvent evento) {
        LivingEntity muerto = evento.getEntity();
        if (!(muerto.level() instanceof ServerLevel nivel) || muerto instanceof Player || muerto instanceof ArmorStand) {
            return;
        }
        boolean loMatoUnJugador = evento.getSource().getEntity() instanceof Player
                || muerto.getKillCredit() instanceof Player;
        if (!loMatoUnJugador) {
            return;
        }

        float signo = muerto instanceof Enemy ? 1.0f : -1.0f;
        float restante = muerto.getMaxHealth();

        // Lo repartimos en varios orbes, como la experiencia
        while (restante > 0.001f) {
            float trozo = Math.min(restante, VALOR_MAXIMO_POR_ORBE);
            restante -= trozo;
            nivel.addFreshEntity(OrbeFe.crear(nivel, muerto.getX(), muerto.getY() + 0.5, muerto.getZ(), signo * trozo));
        }
    }
}
