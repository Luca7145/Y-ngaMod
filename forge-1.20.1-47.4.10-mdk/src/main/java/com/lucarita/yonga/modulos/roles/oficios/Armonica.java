package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.modulos.roles.ModuloRoles;
import com.lucarita.yonga.modulos.roles.cara.Cara;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Armónica del Melómano: va en el hueco de la cara y se ve en la boca del personaje. Deja las manos libres,
 * así que se puede tocar a la vez que cualquier otro instrumento (con el tambor levantado, tensando la lira...).
 *
 *  - Tecla H (se cambia en Controles, apartado "Yönga"): toca una nota (abajo = grave, arriba = agudo) y suma
 *    ritmo, el mismo ritmo que los demás instrumentos.
 *  - BLUES: con la armónica puesta, cada vez que te hacen daño se queja (suena un quejido) y el dolor se
 *    convierte en ritmo (+15). Como mucho una vez por segundo (para que el fuego no la llene sola).
 *  - LAMENTO: con la barra llena, la tecla H toca un lamento: los mobs hostiles (y los neutrales enfadados)
 *    a 10 bloques o menos se asustan y huyen de ti durante 6 s, sin poder atacarte. No afecta a jefes
 *    (Wither, dragón...) ni a jugadores. Cada lamento gasta 1 de durabilidad de la armónica (100 en total;
 *    se repara con lucarita).
 */
public final class Armonica {

    // ---- Ajustes ----
    private static final int RITMO_POR_QUEJIDO = 15;
    private static final long PAUSA_ENTRE_QUEJIDOS = 20;      // 1 s
    private static final long PAUSA_ENTRE_NOTAS = 4;          // que mantener la tecla no dispare 20 notas por segundo
    private static final double RADIO_LAMENTO = 10.0;
    private static final int DURACION_MIEDO = 20 * 6;         // 6 s
    private static final double VELOCIDAD_HUIDA = 1.4;
    private static final int DESGASTE_LAMENTO = 1;            // de 100 (ArmonicaItem.DURABILIDAD): 100 lamentos

    private static final String CLAVE_MIEDO = "yonga_miedo";  // en el mob: { hasta, x, z } (de quién huye)

    private static final Map<UUID, Long> ULTIMA_NOTA = new HashMap<>();
    private static final Map<UUID, Long> ULTIMO_QUEJIDO = new HashMap<>();

    private Armonica() {}

    public static boolean laLleva(ServerPlayer jugador) {
        return Cara.obtener(jugador).is(ModuloRoles.ARMONICA.get());
    }

    // ---------- TECLA H ----------

    public static void tocar(ServerPlayer jugador) {
        if (!laLleva(jugador)) {
            return;
        }
        long ahora = jugador.level().getGameTime();
        Long ultima = ULTIMA_NOTA.get(jugador.getUUID());
        if (ultima != null && ahora - ultima < PAUSA_ENTRE_NOTAS) {
            return;
        }
        ULTIMA_NOTA.put(jugador.getUUID(), ahora);

        if (Musica.ritmoLleno(jugador)) {
            lamento(jugador);
            Musica.vaciarRitmo(jugador);
            // Cada lamento gasta 1 de durabilidad (64 en total). Si se rompe, desaparece de la cara.
            Cara.obtener(jugador).hurtAndBreak(1, jugador,
                    j -> j.level().playSound(null, j.getX(), j.getY(), j.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 1.0f));
            gastar(jugador);
        } else {
            Musica.tocarNota(jugador, ModuloRoles.SONIDO_ARMONICA_NOTA.get());
            Musica.sumarRitmo(jugador, Musica.RITMO_POR_NOTA);
        }
    }

    // ---------- BLUES: el daño se convierte en ritmo ----------

    public static void alRecibirDano(LivingDamageEvent evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jugador) || evento.getAmount() <= 0 || !laLleva(jugador)) {
            return;
        }
        long ahora = jugador.level().getGameTime();
        Long ultimo = ULTIMO_QUEJIDO.get(jugador.getUUID());
        if (ultimo != null && ahora - ultimo < PAUSA_ENTRE_QUEJIDOS) {
            return;
        }
        ULTIMO_QUEJIDO.put(jugador.getUUID(), ahora);
        if (!Musica.silenciado(jugador)) {
            jugador.serverLevel().playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(),
                    ModuloRoles.SONIDO_ARMONICA_QUEJIDO.get(), SoundSource.RECORDS, 1.0f,
                    0.9f + jugador.getRandom().nextFloat() * 0.2f);
        }
        Musica.sumarRitmo(jugador, RITMO_POR_QUEJIDO);
    }

    // ---------- LAMENTO: los mobs huyen ----------

    private static void lamento(ServerPlayer jugador) {
        ServerLevel nivel = jugador.serverLevel();
        nivel.playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(),
                ModuloRoles.SONIDO_ARMONICA_LAMENTO.get(), SoundSource.RECORDS, 1.5f, 1.0f);

        long hasta = nivel.getGameTime() + DURACION_MIEDO;
        List<PathfinderMob> asustados = nivel.getEntitiesOfClass(PathfinderMob.class,
                new AABB(jugador.blockPosition()).inflate(RADIO_LAMENTO),
                m -> m.isAlive() && m.distanceTo(jugador) <= RADIO_LAMENTO && seAsusta(m, jugador));
        for (PathfinderMob mob : asustados) {
            CompoundTag miedo = new CompoundTag();
            miedo.putLong("hasta", hasta);
            miedo.putDouble("x", jugador.getX());
            miedo.putDouble("z", jugador.getZ());
            mob.getPersistentData().put(CLAVE_MIEDO, miedo);
            mob.setTarget(null);
            huir(mob, miedo);
        }

        // Ondas de notas que se alejan
        for (int i = 0; i < 20; i++) {
            double angulo = Math.PI * 2 * i / 20;
            for (double r = 2; r <= RADIO_LAMENTO; r += 4) {
                nivel.sendParticles(ParticleTypes.NOTE, jugador.getX() + Math.cos(angulo) * r, jugador.getY() + 1.2,
                        jugador.getZ() + Math.sin(angulo) * r, 0, 0.6, 0, 0, 1.0);   // notas azules
            }
        }
    }

    // La armónica está en el hueco de cara: se gasta ahí mismo y, si se rompe, el hueco se queda vacío
    private static void gastar(ServerPlayer jugador) {
        if (jugador.getAbilities().instabuild) {
            return;   // en creativo no se gasta
        }
        ItemStack armonica = Cara.obtener(jugador);
        armonica.hurtAndBreak(DESGASTE_LAMENTO, jugador, j -> j.level().playSound(null, j.getX(), j.getY(), j.getZ(),
                SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 1.0f));
    }

    private static boolean seAsusta(PathfinderMob mob, ServerPlayer jugador) {
        if (mob.getType().is(Tags.EntityTypes.BOSSES)) {
            return false;
        }
        return mob instanceof Enemy || (mob instanceof NeutralMob neutral && neutral.isAngryAt(jugador)) || mob.getTarget() == jugador;
    }

    private static void huir(PathfinderMob mob, CompoundTag miedo) {
        Vec3 deQuien = new Vec3(miedo.getDouble("x"), mob.getY(), miedo.getDouble("z"));
        Vec3 lejos = DefaultRandomPos.getPosAway(mob, 16, 7, deQuien);
        if (lejos != null) {
            mob.getNavigation().moveTo(lejos.x, lejos.y, lejos.z, VELOCIDAD_HUIDA);
        }
    }

    /** Cada tick de cada mob asustado: que siga huyendo y no ataque. */
    public static void alTickMob(LivingEvent.LivingTickEvent evento) {
        if (!(evento.getEntity() instanceof PathfinderMob mob) || mob.level().isClientSide()
                || !mob.getPersistentData().contains(CLAVE_MIEDO)) {
            return;
        }
        CompoundTag miedo = mob.getPersistentData().getCompound(CLAVE_MIEDO);
        if (mob.level().getGameTime() > miedo.getLong("hasta")) {
            mob.getPersistentData().remove(CLAVE_MIEDO);
            return;
        }
        mob.setTarget(null);
        if (mob.tickCount % 10 == 0 || mob.getNavigation().isDone()) {
            huir(mob, miedo);
        }
        if (mob.tickCount % 15 == 0 && mob.level() instanceof ServerLevel nivel) {
            nivel.sendParticles(ParticleTypes.NOTE, mob.getX(), mob.getEyeY() + 0.5, mob.getZ(), 0, 0.6, 0, 0, 1.0);
        }
    }

    /** Un mob asustado no puede elegir a nadie como objetivo. */
    public static void alCambiarObjetivo(LivingChangeTargetEvent evento) {
        if (evento.getEntity() instanceof Mob mob && evento.getNewTarget() != null
                && mob.getPersistentData().contains(CLAVE_MIEDO)
                && mob.level().getGameTime() <= mob.getPersistentData().getCompound(CLAVE_MIEDO).getLong("hasta")) {
            evento.setCanceled(true);
        }
    }

    public static void alSalir(PlayerEvent.PlayerLoggedOutEvent evento) {
        ULTIMA_NOTA.remove(evento.getEntity().getUUID());
        ULTIMO_QUEJIDO.remove(evento.getEntity().getUUID());
    }
}
