package com.lucarita.yonga.modulos.fe.mobs;

import com.lucarita.yonga.modulos.fe.ModuloFe;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.level.BlockEvent;

import java.util.Set;

/**
 * El ritual del Pez del viento. Como el del Wither, se hace con bloques y se completa con el último:
 *
 *        V . V        V = vara del End (de pie) en las cuatro esquinas, sobre el cuarzo
 *        . C .        C = un CONDUCTO en el centro (el corazón del mar): al ponerlo, empieza el ritual
 *        V . V
 *    sobre una plataforma de 3x3 bloques de cuarzo (cualquiera: normal, cincelado, pilar, liso o ladrillos).
 *
 * Solo funciona en un mundo con cielo (no en el Nether ni en el End) y en un sitio ABIERTO: nada encima del altar
 * y, alrededor (hasta 24 bloques), casi todo el terreno a poca altura respecto al altar (sin montañas, ni árboles
 * altos, ni edificios que le estorben al nadar por el aire). Si no, avisa y no pasa nada.
 * Al completarse, el altar se consume y empieza la intro (PezDelViento.invocar).
 */
public final class RitualPez {

    private static final Set<Block> CUARZO = Set.of(Blocks.QUARTZ_BLOCK, Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_PILLAR,
            Blocks.SMOOTH_QUARTZ, Blocks.QUARTZ_BRICKS);
    private static final int[] RADIOS = {8, 16, 24};
    private static final int MUESTRAS = 16;                 // puntos por anillo
    private static final int ALTURA_PERMITIDA = 6;          // bloques por encima del altar que se toleran (arbustos...)
    private static final float ABIERTO_MINIMO = 0.85f;      // proporción de puntos que tienen que estar despejados

    private RitualPez() {}

    /** Se registra en ModuloFe (bus de Forge). */
    public static void alColocar(BlockEvent.EntityPlaceEvent evento) {
        if (!evento.getPlacedBlock().is(Blocks.CONDUIT) || !(evento.getLevel() instanceof ServerLevel nivel)
                || !(evento.getEntity() instanceof Player jugador)) {
            return;
        }
        BlockPos centro = evento.getPos();
        if (!altarCompleto(nivel, centro)) {
            return;
        }
        if (!nivel.dimensionType().hasSkyLight() || nivel.dimensionType().hasCeiling()) {
            avisar(jugador, "yonga.pez.ritual_sin_cielo");
            return;
        }
        if (!abierto(nivel, centro)) {
            avisar(jugador, "yonga.pez.ritual_cerrado");
            return;
        }
        // ¡Se consume el altar!
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                nivel.removeBlock(centro.offset(dx, 0, dz), false);
                nivel.removeBlock(centro.offset(dx, -1, dz), false);
            }
        }
        nivel.playSound(null, centro, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 4.0f, 0.6f);
        nivel.playSound(null, centro, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 4.0f, 0.8f);
        nivel.sendParticles(ParticleTypes.END_ROD, centro.getX() + 0.5, centro.getY() + 0.5, centro.getZ() + 0.5, 60, 1.0, 0.5, 1.0, 0.2);

        PezDelViento pez = ModuloFe.PEZ_DEL_VIENTO.get().create(nivel);
        if (pez == null) {
            return;
        }
        BlockPos altar = centro.below();
        pez.moveTo(altar.getX() + 40.5, altar.getY() + 70.0, altar.getZ() + 0.5, 90.0f, 0.0f);
        pez.invocar(altar);
        nivel.addFreshEntity(pez);
    }

    /** ¿Está el conducto en el centro de la plataforma de cuarzo, con las cuatro varas del End en las esquinas? */
    private static boolean altarCompleto(Level nivel, BlockPos centro) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!CUARZO.contains(nivel.getBlockState(centro.offset(dx, -1, dz)).getBlock())) {
                    return false;
                }
                BlockState encima = nivel.getBlockState(centro.offset(dx, 0, dz));
                boolean esquina = dx != 0 && dz != 0;
                if (esquina && !(encima.is(Blocks.END_ROD) && encima.getValue(EndRodBlock.FACING) == Direction.UP)) {
                    return false;
                }
                if (!esquina && (dx != 0 || dz != 0) && !encima.isAir()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Nada encima del altar y, alrededor, casi todo a poca altura: hay sitio para que nade por el aire. */
    public static boolean abierto(ServerLevel nivel, BlockPos centro) {
        int base = centro.getY() - 1;
        if (nivel.getHeight(Heightmap.Types.MOTION_BLOCKING, centro.getX(), centro.getZ()) > centro.getY() + 1) {
            return false;   // hay techo encima
        }
        int bien = 0, total = 0;
        for (int radio : RADIOS) {
            for (int i = 0; i < MUESTRAS; i++) {
                double a = i * Math.PI * 2 / MUESTRAS;
                int x = centro.getX() + (int) Math.round(Math.cos(a) * radio);
                int z = centro.getZ() + (int) Math.round(Math.sin(a) * radio);
                int alto = nivel.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                total++;
                if (alto <= base + ALTURA_PERMITIDA) {
                    bien++;
                }
            }
        }
        return bien >= total * ABIERTO_MINIMO;
    }

    private static void avisar(Player jugador, String clave) {
        jugador.displayClientMessage(Component.translatable(clave).withStyle(ChatFormatting.AQUA), true);
    }
}
