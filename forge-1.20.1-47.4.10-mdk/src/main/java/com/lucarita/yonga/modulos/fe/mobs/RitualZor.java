package com.lucarita.yonga.modulos.fe.mobs;

import com.lucarita.yonga.modulos.fe.ModuloFe;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;

import java.util.Set;

/**
 * El ritual de Zor el quemado:
 *
 *        F . F        F = farol de almas en las cuatro esquinas, sobre la piedra negra
 *        . C .        C = una CALAVERA DE ESQUELETO WITHER en el centro: al ponerla, empieza el ritual
 *        F . F
 *    sobre una plataforma de 3x3 de piedra negra (cualquiera: normal, pulida, ladrillos, cincelada, dorada...).
 *
 * Como el Pez, necesita un mundo con cielo y un sitio abierto (vuela a su alrededor y se mete bajo tierra).
 * Al completarse, el altar se consume, la tierra tiembla y Zor sale de debajo (ZorElQuemado.invocar).
 */
public final class RitualZor {

    private static final Set<Block> PIEDRA_NEGRA = Set.of(Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE,
            Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, Blocks.CHISELED_POLISHED_BLACKSTONE,
            Blocks.GILDED_BLACKSTONE);

    private RitualZor() {}

    /** Se registra en ModuloFe (bus de Forge). */
    public static void alColocar(BlockEvent.EntityPlaceEvent evento) {
        if (!evento.getPlacedBlock().is(Blocks.WITHER_SKELETON_SKULL) || !(evento.getLevel() instanceof ServerLevel nivel)
                || !(evento.getEntity() instanceof Player jugador)) {
            return;
        }
        BlockPos centro = evento.getPos();
        if (!altarCompleto(nivel, centro)) {
            return;
        }
        if (!nivel.dimensionType().hasSkyLight() || nivel.dimensionType().hasCeiling()) {
            avisar(jugador, "yonga.ritual.sin_espacio");
            return;
        }
        if (!RitualPez.abierto(nivel, centro)) {
            avisar(jugador, "yonga.ritual.sin_espacio");
            return;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                nivel.removeBlock(centro.offset(dx, 0, dz), false);
                nivel.removeBlock(centro.offset(dx, -1, dz), false);
            }
        }
        nivel.playSound(null, centro, SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 3.0f, 0.5f);
        nivel.playSound(null, centro, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 3.0f, 0.6f);
        nivel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, centro.getX() + 0.5, centro.getY() + 0.5, centro.getZ() + 0.5, 80, 1.0, 0.5, 1.0, 0.1);
        nivel.sendParticles(ParticleTypes.LARGE_SMOKE, centro.getX() + 0.5, centro.getY() + 0.5, centro.getZ() + 0.5, 40, 1.0, 0.5, 1.0, 0.05);

        ZorElQuemado zor = ModuloFe.ZOR.get().create(nivel);
        if (zor == null) {
            return;
        }
        BlockPos altar = centro.below();
        zor.moveTo(altar.getX() + 0.5, altar.getY() - 7.0, altar.getZ() + 0.5, 0.0f, -80.0f);
        zor.invocar(altar);
        nivel.addFreshEntity(zor);
    }

    private static boolean altarCompleto(Level nivel, BlockPos centro) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!PIEDRA_NEGRA.contains(nivel.getBlockState(centro.offset(dx, -1, dz)).getBlock())) {
                    return false;
                }
                BlockState encima = nivel.getBlockState(centro.offset(dx, 0, dz));
                boolean esquina = dx != 0 && dz != 0;
                if (esquina && !encima.is(Blocks.SOUL_LANTERN)) {
                    return false;
                }
                if (!esquina && (dx != 0 || dz != 0) && !encima.isAir()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void avisar(Player jugador, String clave) {
        jugador.displayClientMessage(Component.translatable(clave).withStyle(ChatFormatting.RED), true);
    }
}
