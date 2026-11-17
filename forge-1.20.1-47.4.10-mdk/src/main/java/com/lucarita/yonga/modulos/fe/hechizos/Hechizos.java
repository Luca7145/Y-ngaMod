package com.lucarita.yonga.modulos.fe.hechizos;

import com.lucarita.yonga.modulos.roles.oficios.Sacerdote;
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
 *
 * Versiones II: solo para el oficio Sacerdote, a partir del rango que diga Sacerdote.java
 * (Oficial: Gran Salto II y Expulsión II; Maestro: Castigo del Cielo II y Proyectil Oscuro II).
 */
public final class Hechizos {

    // ---- Ajustes: Gran Salto ----
    private static final double SALTO_VERTICAL = 1.5;            // ~12 bloques de altura
    private static final double SALTO_VERTICAL_II = 1.9;         // ~18 bloques de altura
    private static final double SALTO_HORIZONTAL = 0.6;          // empujón hacia donde mira
    private static final int SALTO_PROTECCION_TICKS = 15 * 20;   // tiempo máximo protegido de la caída
    private static final double ATERRIZAJE_II_RADIO = 3.0;       // onda al aterrizar (solo II)
    private static final double ATERRIZAJE_II_FUERZA = 0.8;      // bastante más flojo que la Expulsión
    private static final float ATERRIZAJE_II_DANO = 2.0f;        // 1 corazón

    // ---- Ajustes: Castigo del Cielo ----
    private static final double CASTIGO_RADIO = 3.0;
    private static final double CASTIGO_RADIO_II = 6.0;

    // ---- Ajustes: Expulsión ----
    private static final double EXPULSION_RADIO = 3.0;
    private static final double EXPULSION_FUERZA = 2.0;          // como un golpe con Empuje alto
    private static final double EXPULSION_FUERZA_II = 4.0;       // el doble de lejos
    private static final float EXPULSION_DANO_II = 10.5f;        // espadazo y medio de diamante (7 x 1.5)

    // ---- Ajustes: Proyectil Oscuro ----
    private static final int PROYECTIL_POTENCIA = 1;             // la explosión del ghast
    private static final int PROYECTIL_POTENCIA_II = 3;          // mucho más grande (la dinamita es 4)

    // Jugadores protegidos de la caída tras un Gran Salto: hasta cuándo y si al aterrizar hace la onda (II)
    private record Salto(long hasta, boolean mejorado) {}
    private static final Map<UUID, Salto> SALTOS = new HashMap<>();

    private Hechizos() {}

    // ---------- GRAN SALTO ----------

    public static boolean granSalto(ServerPlayer jugador) {
        boolean mejorado = Sacerdote.hechizoMejorado(jugador, Hechizo.GRAN_SALTO);

        Vec3 mirada = jugador.getLookAngle();
        Vec3 horizontal = new Vec3(mirada.x, 0, mirada.z);
        if (horizontal.lengthSqr() > 1.0E-4) {
            horizontal = horizontal.normalize().scale(SALTO_HORIZONTAL);
        }
        jugador.setDeltaMovement(horizontal.x, mejorado ? SALTO_VERTICAL_II : SALTO_VERTICAL, horizontal.z);
        jugador.fallDistance = 0;
        // Hay que avisar al cliente del jugador del nuevo movimiento (si no, no se mueve)
        jugador.connection.send(new ClientboundSetEntityMotionPacket(jugador));

        SALTOS.put(jugador.getUUID(), new Salto(jugador.level().getGameTime() + SALTO_PROTECCION_TICKS, mejorado));

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
        Salto salto = SALTOS.get(jugador.getUUID());
        if (salto == null) {
            return;
        }
        if (jugador.level().getGameTime() > salto.hasta()) {
            SALTOS.remove(jugador.getUUID());
            return;
        }
        if (evento.getDistance() > 0.5f) {
            evento.setCanceled(true);
            SALTOS.remove(jugador.getUUID());
            if (salto.mejorado()) {
                ondaAterrizaje(jugador);
            }
        }
    }

    // Gran Salto II: al aterrizar, una expulsión flojita con un poco de daño
    private static void ondaAterrizaje(ServerPlayer jugador) {
        for (Entity objetivo : cercanos(jugador, ATERRIZAJE_II_RADIO)) {
            if (objetivo instanceof LivingEntity vivo) {
                vivo.hurt(jugador.damageSources().playerAttack(jugador), ATERRIZAJE_II_DANO);
            }
            empujar(jugador, objetivo, ATERRIZAJE_II_FUERZA);
        }
        ServerLevel nivel = jugador.serverLevel();
        nivel.sendParticles(ParticleTypes.EXPLOSION, jugador.getX(), jugador.getY() + 0.2, jugador.getZ(), 3, 1.0, 0.1, 1.0, 0.0);
        nivel.sendParticles(ParticleTypes.CLOUD, jugador.getX(), jugador.getY() + 0.1, jugador.getZ(), 30, 1.5, 0.05, 1.5, 0.1);
        nivel.playSound(null, jugador.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 1.5f);
    }

    // ---------- CASTIGO DEL CIELO ----------

    public static boolean castigoDelCielo(ServerPlayer jugador) {
        double radio = Sacerdote.hechizoMejorado(jugador, Hechizo.CASTIGO_DEL_CIELO) ? CASTIGO_RADIO_II : CASTIGO_RADIO;
        List<LivingEntity> objetivos = jugador.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(jugador.blockPosition()).inflate(radio + 1),
                e -> e != jugador && e.isAlive() && !e.isSpectator() && e.distanceTo(jugador) <= radio);
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
        boolean mejorado = Sacerdote.hechizoMejorado(jugador, Hechizo.EXPULSION);
        List<Entity> objetivos = cercanos(jugador, EXPULSION_RADIO);
        if (objetivos.isEmpty()) {
            jugador.displayClientMessage(Component.translatable("yonga.hechizo.sin_objetivos"), true);
            return false;
        }
        for (Entity objetivo : objetivos) {
            // Expulsión II además hace daño (primero el daño, luego el empujón para que no lo anule)
            if (mejorado && objetivo instanceof LivingEntity vivo) {
                vivo.hurt(jugador.damageSources().playerAttack(jugador), EXPULSION_DANO_II);
            }
            empujar(jugador, objetivo, mejorado ? EXPULSION_FUERZA_II : EXPULSION_FUERZA);
        }
        ServerLevel nivel = jugador.serverLevel();
        nivel.sendParticles(ParticleTypes.SQUID_INK, jugador.getX(), jugador.getY() + 1.0, jugador.getZ(),
                mejorado ? 80 : 40, 1.5, 0.5, 1.5, 0.1);
        nivel.playSound(null, jugador.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, mejorado ? 0.4f : 0.6f);
        return true;
    }

    // ---------- PROYECTIL OSCURO ----------

    public static boolean proyectilOscuro(ServerPlayer jugador) {
        int potencia = Sacerdote.hechizoMejorado(jugador, Hechizo.PROYECTIL_OSCURO) ? PROYECTIL_POTENCIA_II : PROYECTIL_POTENCIA;
        Vec3 mirada = jugador.getLookAngle();
        LargeFireball bola = new LargeFireball(jugador.level(), jugador, mirada.x, mirada.y, mirada.z, potencia);
        // Sale un poco por delante de los ojos para no chocar con el propio jugador
        bola.setPos(jugador.getX() + mirada.x * 1.5, jugador.getEyeY() - 0.2 + mirada.y * 1.5, jugador.getZ() + mirada.z * 1.5);
        jugador.level().addFreshEntity(bola);
        jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.0f, 0.7f);
        return true;
    }

    // ---------- AYUDAS ----------

    // Todas las entidades (vivas o no) a cierta distancia del jugador, menos él mismo y los espectadores
    private static List<Entity> cercanos(ServerPlayer jugador, double radio) {
        return jugador.level().getEntities(jugador,
                new AABB(jugador.blockPosition()).inflate(radio + 1),
                e -> e.isAlive() && !e.isSpectator() && e.distanceTo(jugador) <= radio);
    }

    // Empuja una entidad lejos del jugador
    private static void empujar(ServerPlayer jugador, Entity objetivo, double fuerza) {
        double dx = jugador.getX() - objetivo.getX();
        double dz = jugador.getZ() - objetivo.getZ();
        if (dx * dx + dz * dz < 1.0E-4) {   // justo encima: lo empujamos hacia donde mira el jugador
            dx = -jugador.getLookAngle().x;
            dz = -jugador.getLookAngle().z;
        }
        if (objetivo instanceof LivingEntity vivo) {
            // Mismo empuje que un golpe (respeta la resistencia al empuje, como un golpe normal)
            vivo.knockback(fuerza, dx, dz);
        } else {
            // Objetos, flechas, barcas...: los lanzamos igual
            Vec3 empuje = new Vec3(-dx, 0, -dz).normalize().scale(fuerza * 0.6);
            objetivo.setDeltaMovement(objetivo.getDeltaMovement().add(empuje.x, 0.3, empuje.z));
        }
        objetivo.hurtMarked = true;   // envía el nuevo movimiento a los clientes (necesario para jugadores)
    }

    // ---------- LIMPIEZA ----------

    public static void alSalir(PlayerEvent.PlayerLoggedOutEvent evento) {
        SALTOS.remove(evento.getEntity().getUUID());
    }
}
