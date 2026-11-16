package com.lucarita.yonga.modulos.armas;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

/**
 * Aturdir (lo usa la maza): el objetivo no puede moverse, saltar ni atacar durante un momento.
 * Después queda inmune un rato para que no se pueda encadenar un aturdimiento tras otro.
 */
public final class Aturdimiento {

    public static final int DURACION = 30;    // 1.5 segundos
    public static final int INMUNIDAD = 80;   // 4 segundos sin poder volver a ser aturdido (cuenta desde que acaba)

    private static final String CLAVE_INMUNE_HASTA = "yonga_inmune_aturdir_hasta";

    private Aturdimiento() {}

    /** El efecto "Aturdido": quita el 100% de la velocidad de movimiento. Lo de no atacar/saltar va en los eventos de abajo. */
    public static class Efecto extends MobEffect {
        public Efecto() {
            super(MobEffectCategory.HARMFUL, 0xC8B560);
        }
    }

    public static void aturdir(LivingEntity objetivo) {
        long ahora = objetivo.level().getGameTime();
        if (ahora < objetivo.getPersistentData().getLong(CLAVE_INMUNE_HASTA)) {
            return;   // todavía inmune
        }
        objetivo.getPersistentData().putLong(CLAVE_INMUNE_HASTA, ahora + DURACION + INMUNIDAD);

        objetivo.addEffect(new MobEffectInstance(ModuloArmas.ATURDIDO.get(), DURACION, 0, false, false, true));
        if (objetivo instanceof Mob mob) {
            mob.getNavigation().stop();
        }

        // Sonido de golpe metálico y estrellitas
        objetivo.level().playSound(null, objetivo.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4f, 1.6f);
        if (objetivo.level() instanceof ServerLevel nivel) {
            nivel.sendParticles(ParticleTypes.CRIT, objetivo.getX(), objetivo.getEyeY() + 0.3, objetivo.getZ(),
                    8, 0.3, 0.1, 0.3, 0.05);
        }
    }

    public static boolean estaAturdido(LivingEntity entidad) {
        return ModuloArmas.ATURDIDO.isPresent() && entidad.hasEffect(ModuloArmas.ATURDIDO.get());
    }

    // ---------- EVENTOS (se registran en ModuloArmas) ----------

    // Un aturdido no hace daño (sirve para mobs y jugadores)
    public static void alAtacarAlguien(LivingAttackEvent evento) {
        if (evento.getSource().getEntity() instanceof LivingEntity atacante && estaAturdido(atacante)) {
            evento.setCanceled(true);
        }
    }

    // Un jugador aturdido no puede ni empezar un ataque
    public static void alAtacarJugador(AttackEntityEvent evento) {
        Player jugador = evento.getEntity();
        if (estaAturdido(jugador)) {
            evento.setCanceled(true);
        }
    }

    // Ni saltar
    public static void alSaltar(LivingEvent.LivingJumpEvent evento) {
        LivingEntity entidad = evento.getEntity();
        if (estaAturdido(entidad)) {
            Vec3 movimiento = entidad.getDeltaMovement();
            entidad.setDeltaMovement(movimiento.x, 0.0, movimiento.z);
        }
    }
}
