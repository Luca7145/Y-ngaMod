package com.lucarita.yonga.modulos.fe.mobs.rey;

import com.lucarita.yonga.modulos.fe.ModuloFe;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.level.BlockEvent;

import java.util.Set;

/**
 * El ritual del Rey de Arenisca (como los del Pez y Zor):
 *
 *        O . O        O = un BLOQUE DE ORO en cada esquina, sobre la arenisca
 *        . C .        C = una CALABAZA TALLADA (o una lámpara de calabaza) en el centro: al ponerla, empieza
 *        O . O
 *    sobre una plataforma de 3x3 de arenisca (cualquiera: normal, cincelada, cortada, lisa, roja...).
 *
 * Da igual el bioma, pero el sitio tiene que ser GRANDE: a cielo abierto, con altura de sobra para sus 44 bloques
 * y un llano de unos 45 bloques de ancho alrededor (sin montañas ni barrancos; los árboles dan igual). Si no, solo
 * dice que no se puede (que hace falta más espacio) y no se gasta nada.
 */
public final class RitualRey {

    private static final Set<Block> ARENISCA = Set.of(Blocks.SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.CUT_SANDSTONE,
            Blocks.SMOOTH_SANDSTONE, Blocks.RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE, Blocks.CUT_RED_SANDSTONE,
            Blocks.SMOOTH_RED_SANDSTONE);
    public static final int RADIO = 22;
    private static final int DESNIVEL = 6;
    private static final double PARTE_DESIGUAL = 0.15;
    /** Lo alto que tiene que caber (sus 44 bloques y un poco más). */
    private static final int ALTURA_NECESARIA = 50;

    private RitualRey() {}

    /** Se registra en ModuloFe (bus de Forge). */
    public static void alColocar(BlockEvent.EntityPlaceEvent evento) {
        BlockState puesto = evento.getPlacedBlock();
        if (!(puesto.is(Blocks.CARVED_PUMPKIN) || puesto.is(Blocks.JACK_O_LANTERN)) || !(evento.getLevel() instanceof ServerLevel nivel)
                || !(evento.getEntity() instanceof Player jugador)) {
            return;
        }
        BlockPos centro = evento.getPos();
        if (!altarCompleto(nivel, centro)) {
            return;
        }
        String falla = comprobarSitio(nivel, centro);
        if (falla != null) {
            jugador.displayClientMessage(Component.translatable("yonga.ritual.sin_espacio").withStyle(ChatFormatting.RED), true);
            return;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                nivel.removeBlock(centro.offset(dx, 0, dz), false);
                nivel.removeBlock(centro.offset(dx, -1, dz), false);
            }
        }
        nivel.playSound(null, centro, SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 3.0f, 0.7f);
        nivel.playSound(null, centro, ModuloFe.SONIDO_REY_RUGIDO.get(), SoundSource.HOSTILE, 8.0f, 0.6f);
        nivel.sendParticles(ParticleTypes.END_ROD, centro.getX() + 0.5, centro.getY() + 0.5, centro.getZ() + 0.5, 80, 1.0, 0.5, 1.0, 0.15);
        nivel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                centro.getX() + 0.5, centro.getY() + 0.5, centro.getZ() + 0.5, 80, 1.5, 0.5, 1.5, 0.2);

        ReyDeArenisca rey = ModuloFe.REY_ARENISCA.get().create(nivel);
        if (rey == null) {
            return;
        }
        BlockPos altar = centro.below();
        // mira hacia quien lo ha invocado
        float giro = (float) (Math.atan2(jugador.getZ() - (altar.getZ() + 0.5), jugador.getX() - (altar.getX() + 0.5)) * 180 / Math.PI) - 90.0f;
        rey.moveTo(altar.getX() + 0.5, altar.getY(), altar.getZ() + 0.5, giro, 0.0f);
        rey.setYBodyRot(giro);
        rey.setYHeadRot(giro);
        rey.invocar(altar);
        nivel.addFreshEntity(rey);
    }

    private static boolean altarCompleto(Level nivel, BlockPos centro) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!ARENISCA.contains(nivel.getBlockState(centro.offset(dx, -1, dz)).getBlock())) {
                    return false;
                }
                BlockState encima = nivel.getBlockState(centro.offset(dx, 0, dz));
                boolean esquina = dx != 0 && dz != 0;
                if (esquina && !encima.is(Blocks.GOLD_BLOCK)) {
                    return false;
                }
                if (!esquina && (dx != 0 || dz != 0) && !encima.isAir()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Null si el sitio vale; si no, la clave del mensaje que explica qué falla. */
    private static String comprobarSitio(ServerLevel nivel, BlockPos centro) {
        if (!nivel.dimensionType().hasSkyLight() || nivel.dimensionType().hasCeiling() || !nivel.canSeeSky(centro.above())) {
            return "yonga.rey.ritual_sin_cielo";
        }
        if (centro.getY() + ALTURA_NECESARIA > nivel.getMaxBuildHeight()) {
            return "yonga.rey.ritual_alto";
        }
        int y0 = centro.getY() - 1;
        int total = 0, desiguales = 0;
        for (int dx = -RADIO; dx <= RADIO; dx += 3) {
            for (int dz = -RADIO; dz <= RADIO; dz += 3) {
                if (dx * dx + dz * dz > RADIO * RADIO) continue;
                total++;
                int y = sueloSinArboles(nivel, centro.getX() + dx, centro.getZ() + dz);
                if (Math.abs(y - y0) > DESNIVEL) desiguales++;
            }
        }
        if (desiguales > total * PARTE_DESIGUAL) {
            return "yonga.rey.ritual_pequeno";
        }
        return null;
    }

    /** La altura del suelo, sin contar troncos ni hojas (los árboles no le estorban). */
    private static int sueloSinArboles(ServerLevel nivel, int x, int z) {
        int y = nivel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y - 1, z);
        int minimo = nivel.getMinBuildHeight();
        while (p.getY() > minimo) {
            BlockState b = nivel.getBlockState(p);
            if (!(b.is(BlockTags.LOGS) || b.is(BlockTags.LEAVES) || b.isAir())) break;
            p.move(0, -1, 0);
        }
        return p.getY() + 1;
    }
}
