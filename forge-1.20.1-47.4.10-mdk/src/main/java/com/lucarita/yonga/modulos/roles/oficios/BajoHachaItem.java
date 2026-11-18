package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.core.GolpeCargado;
import com.lucarita.yonga.core.ModTiers;
import com.lucarita.yonga.modulos.roles.ModuloRoles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Bajo-hacha del Melómano: un hacha de lucarita que además es un bajo.
 *
 *  - Click izquierdo: golpea como un hacha pesada (rompe escudos, como cualquier hacha) y suena una nota.
 *  - Durabilidad: solo se gasta al golpear (1 por golpe, como una espada) y al romper bloques. Tocar no la gasta.
 *  - Click derecho: toca una nota. El tono depende de hacia dónde miras: abajo = grave, arriba = agudo
 *    (2 octavas). Manteniendo pulsado se repite la nota.
 *  - Ritmo (ver Musica): cada nota y cada golpe cargado llenan la barra. Con la barra llena,
 *    la siguiente nota es un ACORDE DE POTENCIA: una onda alrededor que empuja y daña a TODOS los mobs
 *    (monstruos, animales, aldeanos...) y jugadores cercanos. A los jugadores solo les afecta si se les
 *    puede hacer daño (respeta el PvP del servidor y los equipos). Los soportes de armadura no cuentan.
 */
public class BajoHachaItem extends AxeItem {

    // ---- Ajustes ----
    private static final float DANO_EXTRA = 6.0f;          // daño en el juego: 1 + 4 (lucarita) + 6 = 11
    private static final float VELOCIDAD = -3.0f;          // velocidad en el juego: 4 - 3 = 1.0
    public static final int DURABILIDAD = 2200;            // un poco más que la netherita (2031)

    private static final double RADIO_ACORDE = 4.0;
    private static final double EMPUJE_ACORDE = 1.5;
    private static final float DANO_ACORDE = 6.0f;          // 3 corazones

    public BajoHachaItem(Properties propiedades) {
        super(ModTiers.LUCARITA, DANO_EXTRA, VELOCIDAD, propiedades.durability(DURABILIDAD));
    }

    // Click derecho apuntando a un bloque: no descorteza troncos (como haría un hacha), así que sigue tocando
    @Override
    public InteractionResult useOn(UseOnContext contexto) {
        return InteractionResult.PASS;
    }

    // Click derecho: tocar
    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        ItemStack bajo = jugador.getItemInHand(mano);
        if (!nivel.isClientSide() && jugador instanceof ServerPlayer servidor) {
            tocar(servidor);
        }
        // "consume" = no hace el gesto de golpear al tocar
        return InteractionResultHolder.consume(bajo);
    }

    // Golpe con el hacha: suena una nota (como al tocar) y, si es un golpe cargado, suma ritmo
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity objetivo, LivingEntity atacante) {
        if (atacante instanceof ServerPlayer jugador) {
            Musica.tocarNota(jugador, ModuloRoles.SONIDO_BAJO_NOTA.get());
            if (GolpeCargado.fue(jugador)) {
                Musica.sumarRitmo(jugador, Musica.RITMO_POR_GOLPE);
            }
        }
        // Gasta 1 de durabilidad por golpe, como una espada (las hachas de Minecraft gastan 2)
        stack.hurtAndBreak(1, atacante, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    // ---------- TOCAR ----------

    private static void tocar(ServerPlayer jugador) {
        // ¿Barra llena? Entonces esta nota es el acorde de potencia
        if (Musica.ritmoLleno(jugador)) {
            acordeDePotencia(jugador);
            Musica.vaciarRitmo(jugador);
            return;
        }
        Musica.tocarNota(jugador, ModuloRoles.SONIDO_BAJO_NOTA.get());
        Musica.sumarRitmo(jugador, Musica.RITMO_POR_NOTA);
    }

    // Onda sonora: empuja y daña a todos los mobs y jugadores cercanos
    private static void acordeDePotencia(ServerPlayer jugador) {
        ServerLevel nivel = jugador.serverLevel();
        List<LivingEntity> objetivos = nivel.getEntitiesOfClass(LivingEntity.class,
                new AABB(jugador.blockPosition()).inflate(RADIO_ACORDE + 1),
                e -> e != jugador && e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand)
                        && e.distanceTo(jugador) <= RADIO_ACORDE
                        && (!(e instanceof Player otro) || jugador.canHarmPlayer(otro)));
        for (LivingEntity objetivo : objetivos) {
            objetivo.hurt(jugador.damageSources().playerAttack(jugador), DANO_ACORDE);
            objetivo.knockback(EMPUJE_ACORDE, jugador.getX() - objetivo.getX(), jugador.getZ() - objetivo.getZ());
            objetivo.hurtMarked = true;   // necesario para que a los jugadores les llegue el empujón
        }
        nivel.playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(),
                ModuloRoles.SONIDO_BAJO_ACORDE.get(), SoundSource.RECORDS, 1.5f, 1.0f);
        // Onda visual: un anillo de notas y un estallido sónico en el centro
        nivel.sendParticles(ParticleTypes.SONIC_BOOM, jugador.getX(), jugador.getY() + 1.0, jugador.getZ(), 1, 0, 0, 0, 0);
        for (int i = 0; i < 16; i++) {
            double angulo = Math.PI * 2 * i / 16;
            nivel.sendParticles(ParticleTypes.NOTE,
                    jugador.getX() + Math.cos(angulo) * RADIO_ACORDE * 0.6, jugador.getY() + 1.0,
                    jugador.getZ() + Math.sin(angulo) * RADIO_ACORDE * 0.6,
                    0, i / 16.0, 0.0, 0.0, 1.0);
        }
    }

}
