package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.core.ModTiers;
import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.fe.ManaDatos;
import com.lucarita.yonga.modulos.fe.TipoMana;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * BÁCULO (Sacerdote, desde Oficial). Sin maná pega poco (5, velocidad 1,2). Cada golpe cargado gasta 30 de maná,
 * del tipo que tengas (luminoso u oscuro), y pega 6 más: 11 por golpe, lo mismo que la alabarda o la maza del Herrero
 * pero algo más rápido, a cambio de depender del maná (con 100 de maná, 3 golpes fuertes; con 500, 16). El maná se
 * recupera solo (1 por segundo).
 *
 * No hace barrido. Si no tienes maná (o no tienes fe, o el módulo de fe está apagado) es un palo.
 */
public class BaculoItem extends SwordItem {

    private static final int DANO_EXTRA = 0;               // daño en el juego: 1 + 4 (lucarita) + 0 = 5
    private static final float VELOCIDAD = -2.8f;          // velocidad en el juego: 4 - 2,8 = 1,2
    private static final int DURABILIDAD = 2100;
    public static final float DANO_FE = 6.0f;              // lo que suma un golpe con maná (11 en total)
    public static final double COSTE = 30.0;
    private static final float CARGA_MINIMA = 0.9f;

    // Jugadores que acaban de dar un golpe cargado con el báculo (y en qué tick)
    private static final Map<UUID, Long> CARGADOS = new HashMap<>();

    public BaculoItem(Properties propiedades) {
        super(ModTiers.LUCARITA, DANO_EXTRA, VELOCIDAD, propiedades.durability(DURABILIDAD));
    }

    @Override
    public boolean canPerformAction(ItemStack objeto, ToolAction accion) {
        return accion != ToolActions.SWORD_SWEEP && super.canPerformAction(objeto, accion);
    }

    // ---------- EVENTOS (se registran en ModuloRoles) ----------

    /** Antes del golpe: Minecraft reinicia la carga del ataque antes de hacer el daño, así que la miramos aquí. */
    public static void alAtacar(AttackEntityEvent evento) {
        Player jugador = evento.getEntity();
        if (!jugador.level().isClientSide() && jugador.getMainHandItem().getItem() instanceof BaculoItem
                && jugador.getAttackStrengthScale(0.5f) >= CARGA_MINIMA) {
            CARGADOS.put(jugador.getUUID(), jugador.level().getGameTime());
        }
    }

    /** El golpe: si estaba cargado y hay maná, lo gasta y pega más. */
    public static void alRecibirDano(LivingHurtEvent evento) {
        DamageSource fuente = evento.getSource();
        if (!(fuente.getEntity() instanceof ServerPlayer jugador) || fuente.getDirectEntity() != jugador) {
            return;
        }
        Long cuando = CARGADOS.remove(jugador.getUUID());
        if (cuando == null || cuando != jugador.level().getGameTime() || !Modulo.FE.activo()
                || !(jugador.getMainHandItem().getItem() instanceof BaculoItem)) {
            return;
        }
        TipoMana tipo = ManaDatos.tipo(jugador);
        if (tipo == TipoMana.NINGUNO || !ManaDatos.gastar(jugador, COSTE)) {
            return;
        }
        evento.setAmount(evento.getAmount() + DANO_FE);

        LivingEntity objetivo = evento.getEntity();
        if (jugador.level() instanceof ServerLevel nivel) {
            boolean oscuro = tipo == TipoMana.OSCURO;
            nivel.sendParticles(oscuro ? ParticleTypes.SOUL : ParticleTypes.END_ROD,
                    objetivo.getX(), objetivo.getY() + objetivo.getBbHeight() * 0.6, objetivo.getZ(),
                    10, 0.3, 0.4, 0.3, 0.05);
            nivel.playSound(null, objetivo.getX(), objetivo.getY(), objetivo.getZ(),
                    oscuro ? SoundEvents.SCULK_SHRIEKER_SHRIEK : SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, oscuro ? 0.35f : 1.2f, oscuro ? 1.6f : 0.8f);
        }
    }
}
