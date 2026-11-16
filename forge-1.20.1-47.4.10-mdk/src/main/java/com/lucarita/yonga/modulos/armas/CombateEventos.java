package com.lucarita.yonga.modulos.armas;

import com.lucarita.yonga.core.GolpeCargado;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Parry y contraataque de las armas.
 */
public final class CombateEventos {

    // Enfriamiento tras bloquear (20 ticks = 1 segundo)
    private static final int ENFRIAMIENTO_PARRY = 20;
    private static final int ENFRIAMIENTO_PARRY_PROYECTIL = 40;

    // Contraataque: ventana de tiempo tras un parry y multiplicador de daño
    private static final int VENTANA_CONTRAATAQUE = 20;
    private static final float MULTIPLICADOR_CONTRAATAQUE = 1.5f;

    // Jugador -> momento (en ticks del mundo) hasta el que puede contraatacar
    private static final Map<UUID, Long> CONTRAATAQUE_HASTA = new HashMap<>();

    private CombateEventos() {}

    // ---------- PARRY ----------

    /** Usamos LivingAttackEvent para bloquear el 100% del daño antes de que ocurra. */
    public static void alSerAtacado(LivingAttackEvent evento) {
        if (!(evento.getEntity() instanceof Player jugador) || !jugador.isUsingItem()) {
            return;
        }

        // El ítem con el que está haciendo guardia (vale en cualquier mano)
        ItemStack enUso = jugador.getUseItem();
        if (!(enUso.getItem() instanceof ArmaYonga arma) || !arma.tieneParry()) {
            return;
        }

        // ¿Qué le golpea? Si no hay nada que golpee (caída, lava, fuego, sangrado...) no se puede bloquear
        Entity golpeDirecto = evento.getSource().getDirectEntity();
        int enfriamiento;
        if (golpeDirecto instanceof LivingEntity) {
            enfriamiento = ENFRIAMIENTO_PARRY;
        } else if (golpeDirecto instanceof Projectile && arma.rasgos().parry == ArmaYonga.TipoParry.TAMBIEN_PROYECTILES) {
            enfriamiento = ENFRIAMIENTO_PARRY_PROYECTIL;
        } else {
            return;
        }

        // Bloqueo: 0 daño, sin retroceso. La flecha rebota.
        evento.setCanceled(true);
        jugador.stopUsingItem();
        jugador.getCooldowns().addCooldown(arma, enfriamiento);
        jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);

        if (arma.rasgos().contraataque) {
            CONTRAATAQUE_HASTA.put(jugador.getUUID(), jugador.level().getGameTime() + VENTANA_CONTRAATAQUE);
        }
    }

    // ---------- CONTRAATAQUE ----------

    /** Si el jugador acaba de hacer parry con un arma de contraataque, su siguiente golpe cargado hace +50%. */
    public static void alHacerDano(LivingHurtEvent evento) {
        if (!(evento.getSource().getEntity() instanceof Player jugador)
                || evento.getSource().getDirectEntity() != jugador) {   // solo cuerpo a cuerpo
            return;
        }
        Long hasta = CONTRAATAQUE_HASTA.get(jugador.getUUID());
        if (hasta == null) {
            return;
        }
        CONTRAATAQUE_HASTA.remove(jugador.getUUID());   // se gasta con el primer golpe, acierte o no las condiciones

        boolean conArmaDeContraataque = jugador.getMainHandItem().getItem() instanceof ArmaYonga arma && arma.rasgos().contraataque;
        if (jugador.level().getGameTime() > hasta || !conArmaDeContraataque || !GolpeCargado.fue(jugador)) {
            return;
        }

        evento.setAmount(evento.getAmount() * MULTIPLICADOR_CONTRAATAQUE);
        LivingEntity objetivo = evento.getEntity();
        jugador.level().playSound(null, objetivo.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.2f);
        if (jugador.level() instanceof ServerLevel nivel) {
            nivel.sendParticles(ParticleTypes.ENCHANTED_HIT, objetivo.getX(), objetivo.getY(0.5), objetivo.getZ(),
                    12, 0.3, 0.4, 0.3, 0.2);
        }
    }

    public static void alSalir(PlayerEvent.PlayerLoggedOutEvent evento) {
        CONTRAATAQUE_HASTA.remove(evento.getEntity().getUUID());
    }
}
