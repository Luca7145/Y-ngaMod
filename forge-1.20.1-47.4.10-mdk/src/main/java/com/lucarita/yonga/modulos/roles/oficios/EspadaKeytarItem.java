package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.core.ModTiers;
import com.lucarita.yonga.modulos.roles.ModuloRoles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Espada-keytar del Melómano: el arma rápida y a distancia (el bajo-hacha es la pesada y de área).
 *
 *  - Click izquierdo: golpea como una espada rápida y suena una nota.
 *  - Click derecho: toca una nota de sintetizador (abajo = grave, arriba = agudo, 2 octavas) y la nota
 *    sale disparada hacia donde miras, haciendo daño a lo primero que toque.
 *  - Durabilidad: la misma que el bajo-hacha (2200) y se gasta igual: 1 por golpe y 1 por bloque roto.
 *    Tocar y disparar no la gastan.
 */
public class EspadaKeytarItem extends SwordItem {

    // ---- Ajustes ----
    private static final int DANO_EXTRA = 3;                // daño en el juego: 1 + 4 (lucarita) + 3 = 8
    private static final float VELOCIDAD = -2.2f;           // velocidad en el juego: 4 - 2,2 = 1,8
    public static final int DURABILIDAD = 2200;             // como el bajo-hacha, un poco más que la netherita

    private static final int SEMITONOS = 24;                // 2 octavas
    private static final float VELOCIDAD_NOTA = 1.6f;       // bloques por tick del proyectil
    private static final int ENFRIAMIENTO_DISPARO = 8;      // ticks entre disparos (2,5 por segundo)

    public EspadaKeytarItem(Properties propiedades) {
        super(ModTiers.LUCARITA, DANO_EXTRA, VELOCIDAD, propiedades.durability(DURABILIDAD));
    }

    // Click derecho: tocar y disparar la nota
    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        ItemStack keytar = jugador.getItemInHand(mano);
        if (!nivel.isClientSide() && jugador instanceof ServerPlayer servidor) {
            float color = sonarNota(servidor);
            NotaProyectil nota = new NotaProyectil(nivel, servidor, color);
            nota.shootFromRotation(servidor, servidor.getXRot(), servidor.getYRot(), 0.0f, VELOCIDAD_NOTA, 0.0f);
            nivel.addFreshEntity(nota);
            servidor.getCooldowns().addCooldown(this, ENFRIAMIENTO_DISPARO);
        }
        return InteractionResultHolder.consume(keytar);
    }

    // Golpe con la espada: suena una nota (la durabilidad la gasta la espada normal, 1 por golpe)
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity objetivo, LivingEntity atacante) {
        if (atacante instanceof ServerPlayer jugador) {
            sonarNota(jugador);
        }
        return super.hurtEnemy(stack, objetivo, atacante);
    }

    // Romper bloques gasta 1, como el bajo-hacha (las espadas de Minecraft gastan 2)
    @Override
    public boolean mineBlock(ItemStack stack, Level nivel, BlockState bloque, BlockPos posicion, LivingEntity quien) {
        if (!nivel.isClientSide() && bloque.getDestroySpeed(nivel, posicion) != 0.0f) {
            stack.hurtAndBreak(1, quien, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }
        return true;
    }

    /** Suena una nota de sintetizador según hacia dónde mira el jugador. Devuelve el color de la nota (0 a 1). */
    private static float sonarNota(ServerPlayer jugador) {
        ServerLevel nivel = jugador.serverLevel();
        // getXRot() va de -90 (arriba) a 90 (abajo)
        int semitono = Mth.clamp(Math.round((90.0f - jugador.getXRot()) / 180.0f * SEMITONOS), 0, SEMITONOS);
        float tono = (float) Math.pow(2.0, (semitono - SEMITONOS / 2) / 12.0);   // 0,5 (grave) a 2,0 (agudo)
        nivel.playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(),
                ModuloRoles.SONIDO_KEYTAR_NOTA.get(), SoundSource.RECORDS, 1.0f, tono);
        float color = semitono / (float) SEMITONOS;
        nivel.sendParticles(ParticleTypes.NOTE, jugador.getX(), jugador.getEyeY() + 0.6, jugador.getZ(), 0, color, 0.0, 0.0, 1.0);
        return color;
    }
}
