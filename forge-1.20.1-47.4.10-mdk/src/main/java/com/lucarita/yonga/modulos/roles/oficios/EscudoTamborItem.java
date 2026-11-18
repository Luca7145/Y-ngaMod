package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.core.ModItems;
import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.armas.Aturdimiento;
import com.lucarita.yonga.modulos.roles.ModuloRoles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Escudo-tambor del Melómano: se usa como un escudo (click derecho = levantar el tambor y bloquear).
 *
 *  - Tiene su PROPIO ritmo (no el de los otros instrumentos): una barra de 3 golpes que dura 1 minuto
 *    desde el último golpe.
 *  - Cada golpe que paras con el tambor levantado suena (¡pum!) y suma 1.
 *  - Al llegar a 3, el tambor suelta solo un REDOBLE: una onda como la del bajo (empuja y daña a todos los
 *    mobs y a los jugadores que se puedan dañar) que además los deja ATURDIDOS 4 segundos (no se mueven,
 *    no saltan, no atacan). Luego la barra vuelve a 0.
 *  - Con las notas silenciadas (tecla M) los golpes no suenan, pero el redoble sí.
 *  - Durabilidad: como los demás instrumentos (2200). Se gasta al parar golpes, como un escudo. Se repara con lucarita.
 */
public class EscudoTamborItem extends ShieldItem {

    // ---- Ajustes ----
    private static final int DURABILIDAD = 2200;
    public static final int RITMO_MAXIMO = 3;
    private static final long DURACION_RITMO = 20 * 60;    // 1 minuto (en ticks) desde el último golpe
    private static final double RADIO = 4.0;
    private static final double EMPUJE = 1.5;
    private static final float DANO = 6.0f;                // 3 corazones
    private static final int ATURDIMIENTO = 20 * 4;        // 4 segundos

    private record Ritmo(int valor, long ultimo) {}
    private static final Map<UUID, Ritmo> RITMOS = new HashMap<>();

    public EscudoTamborItem(Properties propiedades) {
        super(propiedades.durability(DURABILIDAD));
    }

    @Override
    public boolean isValidRepairItem(ItemStack tambor, ItemStack material) {
        return material.is(ModItems.LUCARITA.get());
    }

    // ---------- AL PARAR UN GOLPE ----------

    public static void alBloquear(ShieldBlockEvent evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jugador)
                || !(jugador.getUseItem().getItem() instanceof EscudoTamborItem)
                || evento.getDamageSource().getEntity() == null) {
            return;
        }
        long ahora = jugador.level().getGameTime();
        Ritmo anterior = RITMOS.get(jugador.getUUID());
        int valor = (anterior == null || ahora - anterior.ultimo() > DURACION_RITMO) ? 0 : anterior.valor();
        valor++;

        // ¡Pum! Cada golpe un poco más agudo (1, 2, 3)
        if (!Musica.silenciado(jugador)) {
            jugador.serverLevel().playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(),
                    ModuloRoles.SONIDO_TAMBOR_GOLPE.get(), SoundSource.RECORDS, 1.0f, (float) Math.pow(2, (valor - 1) / 6.0));
        }
        jugador.serverLevel().sendParticles(ParticleTypes.NOTE, jugador.getX(), jugador.getEyeY() + 0.6, jugador.getZ(),
                0, valor / (double) RITMO_MAXIMO, 0, 0, 1.0);

        if (valor >= RITMO_MAXIMO) {
            redoble(jugador);
            RITMOS.remove(jugador.getUUID());
            mostrarRitmo(jugador, 0);
        } else {
            RITMOS.put(jugador.getUUID(), new Ritmo(valor, ahora));
            mostrarRitmo(jugador, valor);
        }
    }

    // ---------- REDOBLE ----------

    private static void redoble(ServerPlayer jugador) {
        ServerLevel nivel = jugador.serverLevel();
        List<LivingEntity> objetivos = nivel.getEntitiesOfClass(LivingEntity.class,
                new AABB(jugador.blockPosition()).inflate(RADIO + 1),
                e -> e != jugador && e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand)
                        && e.distanceTo(jugador) <= RADIO
                        && (!(e instanceof Player otro) || jugador.canHarmPlayer(otro)));
        for (LivingEntity objetivo : objetivos) {
            objetivo.hurt(jugador.damageSources().playerAttack(jugador), DANO);
            objetivo.knockback(EMPUJE, jugador.getX() - objetivo.getX(), jugador.getZ() - objetivo.getZ());
            objetivo.hurtMarked = true;   // necesario para que a los jugadores les llegue el empujón
            aturdir(objetivo);
        }
        nivel.playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(),
                ModuloRoles.SONIDO_TAMBOR_REDOBLE.get(), SoundSource.RECORDS, 1.5f, 1.0f);
        nivel.sendParticles(ParticleTypes.SONIC_BOOM, jugador.getX(), jugador.getY() + 1.0, jugador.getZ(), 1, 0, 0, 0, 0);
        for (int i = 0; i < 16; i++) {
            double angulo = Math.PI * 2 * i / 16;
            nivel.sendParticles(ParticleTypes.NOTE,
                    jugador.getX() + Math.cos(angulo) * RADIO * 0.6, jugador.getY() + 1.0,
                    jugador.getZ() + Math.sin(angulo) * RADIO * 0.6,
                    0, i / 16.0, 0.0, 0.0, 1.0);
        }
    }

    // El aturdimiento de verdad es el del módulo de armas (el de la maza). Si ese módulo está apagado,
    // usamos lentitud y debilidad al máximo, que se parece bastante.
    private static void aturdir(LivingEntity objetivo) {
        if (Modulo.ARMAS.activo()) {
            Aturdimiento.aturdir(objetivo, ATURDIMIENTO);
            return;
        }
        objetivo.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ATURDIMIENTO, 9, false, false, true));
        objetivo.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ATURDIMIENTO, 9, false, false, true));
        if (objetivo instanceof Mob mob) {
            mob.getNavigation().stop();
        }
    }

    // Barra del tambor en la barra de acción: Tambor ●●○
    private static void mostrarRitmo(ServerPlayer jugador, int valor) {
        MutableComponent barra = Component.translatable("yonga.tambor.barra").withStyle(ChatFormatting.GOLD);
        barra.append(Component.literal(" " + "●".repeat(valor)).withStyle(ChatFormatting.LIGHT_PURPLE));
        barra.append(Component.literal("○".repeat(RITMO_MAXIMO - valor)).withStyle(ChatFormatting.DARK_GRAY));
        jugador.displayClientMessage(barra, true);
    }

    public static void alSalir(PlayerEvent.PlayerLoggedOutEvent evento) {
        RITMOS.remove(evento.getEntity().getUUID());
    }
}
