package com.lucarita.yonga.modulos.fe.hechizos;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que hace cada hechizo (se ejecuta en el servidor).
 * Si un hechizo no se puede lanzar (p. ej. no hay nadie cerca) devuelve false y no gasta maná ni pone enfriamiento.
 */
public final class Hechizos {

    // ---- Ajustes ----
    private static final double RADIO = 3.0;                    // bloques alrededor del jugador (castigo y expulsión)

    private static final double SALTO_VERTICAL = 1.5;           // ~12 bloques de altura
    private static final double SALTO_HORIZONTAL = 0.6;         // empujón hacia donde mira
    private static final int SALTO_PROTECCION_TICKS = 15 * 20;  // tiempo máximo protegido de la caída

    private static final double EXPULSION_FUERZA = 2.0;         // como un golpe con Empuje alto
    private static final int PROYECTIL_POTENCIA = 1;            // potencia de explosión, la misma que la del ghast

    // Jugadores protegidos de la caída tras un Gran Salto -> hasta qué tick del mundo
    private static final Map<UUID, Long> SIN_DANO_CAIDA = new HashMap<>();

    private Hechizos() {}

    // ---------- GRAN SALTO ----------

    public static boolean granSalto(ServerPlayer jugador) {
        Vec3 mirada = jugador.getLookAngle();
        Vec3 horizontal = new Vec3(mirada.x, 0, mirada.z);
        if (horizontal.lengthSqr() > 1.0E-4) {
            horizontal = horizontal.normalize().scale(SALTO_HORIZONTAL);
        }
        jugador.setDeltaMovement(horizontal.x, SALTO_VERTICAL, horizontal.z);
        jugador.fallDistance = 0;
        // Hay que avisar al cliente del jugador del nuevo movimiento (si no, no se mueve)
        jugador.connection.send(new ClientboundSetEntityMotionPacket(jugador));

        SIN_DANO_CAIDA.put(jugador.getUUID(), jugador.level().getGameTime() + SALTO_PROTECCION_TICKS);

        ServerLevel nivel = jugador.serverLevel();
        nivel.sendParticles(ParticleTypes.CLOUD, jugador.getX(), jugador.getY() + 0.1, jugador.getZ(), 20, 0.4, 0.05, 0.4, 0.05);
        nivel.playSound(null, jugador.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 0.8f);
        return true;
    }

    /** Al caer tras un Gran Salto no se recibe daño. La protección se gasta en el primer aterrizaje de verdad. */
    public static void alCaer(LivingFallEvent evento) {
        // Solo en el servidor, que es quien hace el daño. Ojo: en un mundo de un jugador el cliente y el servidor
        // comparten esta lista, y el cliente detecta el aterrizaje antes; si lo dejáramos, gastaría la protección
        // y el servidor haría el daño igualmente.
        if (!(evento.getEntity() instanceof ServerPlayer jugador)) {
            return;
        }
        Long hasta = SIN_DANO_CAIDA.get(jugador.getUUID());
        if (hasta == null) {
            return;
        }
        if (jugador.level().getGameTime() > hasta) {
            SIN_DANO_CAIDA.remove(jugador.getUUID());
            return;
        }
        if (evento.getDistance() > 0.5f) {
            evento.setCanceled(true);
            SIN_DANO_CAIDA.remove(jugador.getUUID());
        }
    }

    // ---------- CASTIGO DEL CIELO ----------

    public static boolean castigoDelCielo(ServerPlayer jugador) {
        List<LivingEntity> objetivos = jugador.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(jugador.blockPosition()).inflate(RADIO + 1),
                e -> e != jugador && e.isAlive() && !e.isSpectator() && e.distanceTo(jugador) <= RADIO);
        if (objetivos.isEmpty()) {
            jugador.displayClientMessage(Component.translatable("yonga.hechizo.sin_objetivos"), true);
            return false;
        }
        ServerLevel nivel = jugador.serverLevel();
        for (LivingEntity objetivo : objetivos) {
            LightningBolt rayo = EntityType.LIGHTNING_BOLT.create(nivel);
            if (rayo != null) {
                rayo.moveTo(objetivo.getX(), objetivo.getY(), objetivo.getZ());
                rayo.setCause(jugador);   // así sabemos que es suyo y no le hace daño a él
                nivel.addFreshEntity(rayo);
            }
        }
        return true;
    }

    /** Los rayos del Castigo del cielo no le hacen nada a quien los invoca. */
    public static void alCaerRayo(EntityStruckByLightningEvent evento) {
        if (evento.getLightning().getCause() == evento.getEntity()) {
            evento.setCanceled(true);
        }
    }

    // ---------- EXPULSIÓN ----------

    public static boolean expulsion(ServerPlayer jugador) {
        List<Entity> objetivos = jugador.level().getEntities(jugador,
                new AABB(jugador.blockPosition()).inflate(RADIO + 1),
                e -> e.isAlive() && !e.isSpectator() && e.distanceTo(jugador) <= RADIO);
        if (objetivos.isEmpty()) {
            jugador.displayClientMessage(Component.translatable("yonga.hechizo.sin_objetivos"), true);
            return false;
        }
        for (Entity objetivo : objetivos) {
            double dx = jugador.getX() - objetivo.getX();
            double dz = jugador.getZ() - objetivo.getZ();
            if (dx * dx + dz * dz < 1.0E-4) {   // justo encima: lo empujamos hacia donde mira el jugador
                dx = -jugador.getLookAngle().x;
                dz = -jugador.getLookAngle().z;
            }
            if (objetivo instanceof LivingEntity vivo) {
                // Mismo empuje que un golpe, pero sin daño (respeta la resistencia al empuje, como un golpe normal)
                vivo.knockback(EXPULSION_FUERZA, dx, dz);
            } else {
                // Objetos, flechas, barcas...: los lanzamos igual
                Vec3 empuje = new Vec3(-dx, 0, -dz).normalize().scale(EXPULSION_FUERZA * 0.6);
                objetivo.setDeltaMovement(objetivo.getDeltaMovement().add(empuje.x, 0.3, empuje.z));
            }
            objetivo.hurtMarked = true;   // envía el nuevo movimiento a los clientes (necesario para jugadores)
        }
        ServerLevel nivel = jugador.serverLevel();
        nivel.sendParticles(ParticleTypes.SQUID_INK, jugador.getX(), jugador.getY() + 1.0, jugador.getZ(), 40, 1.5, 0.5, 1.5, 0.1);
        nivel.playSound(null, jugador.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 0.6f);
        return true;
    }

    // ---------- PROYECTIL OSCURO ----------

    public static boolean proyectilOscuro(ServerPlayer jugador) {
        Vec3 mirada = jugador.getLookAngle();
        LargeFireball bola = new LargeFireball(jugador.level(), jugador, mirada.x, mirada.y, mirada.z, PROYECTIL_POTENCIA);
        // Sale un poco por delante de los ojos para no chocar con el propio jugador
        bola.setPos(jugador.getX() + mirada.x * 1.5, jugador.getEyeY() - 0.2 + mirada.y * 1.5, jugador.getZ() + mirada.z * 1.5);
        jugador.level().addFreshEntity(bola);
        jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.0f, 0.7f);
        return true;
    }

    // ---------- LIMPIEZA ----------

    public static void alSalir(PlayerEvent.PlayerLoggedOutEvent evento) {
        SIN_DANO_CAIDA.remove(evento.getEntity().getUUID());
    }
}
