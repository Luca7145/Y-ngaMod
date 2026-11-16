package com.lucarita.yonga.core;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ¿El último golpe de un jugador fue cargado del todo?
 *
 * Las habilidades de las armas (sangrado, aturdir, contraataque...) solo funcionan con golpes cargados,
 * para que hacer clic sin parar no sirva para provocarlas una y otra vez.
 *
 * Problema: cuando Minecraft llama a hurtEnemy la carga del ataque ya se ha reiniciado a 0.
 * Solución: la apuntamos aquí al principio del ataque (AttackEntityEvent) y la consultamos después.
 */
public final class GolpeCargado {

    // Lo mismo que usa Minecraft para decidir si un golpe es "fuerte" (barrido, crítico...)
    private static final float CARGA_MINIMA = 0.9f;

    private static final Map<UUID, Boolean> ULTIMO_GOLPE = new HashMap<>();

    private GolpeCargado() {}

    /** true si el atacante dio un golpe cargado. Los mobs siempre cuentan como cargados. */
    public static boolean fue(LivingEntity atacante) {
        if (atacante instanceof Player jugador) {
            return ULTIMO_GOLPE.getOrDefault(jugador.getUUID(), false);
        }
        return true;
    }

    // ---------- EVENTOS (se registran en YongaMain) ----------

    public static void alAtacar(AttackEntityEvent evento) {
        Player jugador = evento.getEntity();
        if (!jugador.level().isClientSide()) {
            ULTIMO_GOLPE.put(jugador.getUUID(), jugador.getAttackStrengthScale(0.5f) > CARGA_MINIMA);
        }
    }

    public static void alSalir(PlayerEvent.PlayerLoggedOutEvent evento) {
        ULTIMO_GOLPE.remove(evento.getEntity().getUUID());
    }
}
