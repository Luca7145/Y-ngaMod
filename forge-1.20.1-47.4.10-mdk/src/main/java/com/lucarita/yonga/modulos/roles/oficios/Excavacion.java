package com.lucarita.yonga.modulos.roles.oficios;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/**
 * El "solo" de los instrumentos-herramienta (pico-guitarra, banjo-pala): rompe de golpe los bloques que
 * tiene el jugador alrededor, hasta {@code radio} bloques de distancia, por los lados, por encima de la
 * cabeza y en el suelo de alrededor.
 *
 *  - Nunca rompe lo que hay justo debajo del jugador (para que no se caiga) ni donde está él.
 *  - Nunca rompe bloques que guardan cosas (cofres, hornos, carteles...), aire ni bloques que se rompen
 *    al instante (antorchas, flores...).
 *  - Rompe como si lo picase el jugador a mano: respeta las protecciones del servidor, suelta lo que tenga
 *    que soltar y gasta durabilidad de la herramienta por cada bloque.
 */
public final class Excavacion {

    // Mientras dura un solo, los bloques que rompe no suman ritmo (si no, se recargaría solo)
    private static boolean enCurso = false;

    private Excavacion() {}

    public static boolean enCurso() {
        return enCurso;
    }

    /**
     * Rompe los bloques de alrededor con dureza {@code durezaMaxima} o menos que además cumplan {@code sePuede}.
     * Para si la herramienta se rompe.
     */
    public static void excavar(ServerPlayer jugador, Item herramienta, int radio, float durezaMaxima, Predicate<BlockState> sePuede) {
        ServerLevel nivel = jugador.serverLevel();
        BlockPos pies = jugador.blockPosition();
        enCurso = true;
        try {
            for (int dy = -radio; dy <= 1 + radio; dy++) {
                for (int dx = -radio; dx <= radio; dx++) {
                    for (int dz = -radio; dz <= radio; dz++) {
                        if (dx == 0 && dz == 0 && dy <= 1) {
                            continue;   // el suelo bajo tus pies y donde estás tú
                        }
                        if (jugador.getMainHandItem().getItem() != herramienta) {
                            return;     // se ha roto la herramienta
                        }
                        BlockPos pos = pies.offset(dx, dy, dz);
                        BlockState estado = nivel.getBlockState(pos);
                        if (estado.isAir() || nivel.getBlockEntity(pos) != null) {
                            continue;
                        }
                        float dureza = estado.getDestroySpeed(nivel, pos);   // agua y lava: 100
                        if (dureza <= 0 || dureza > durezaMaxima || !sePuede.test(estado)) {
                            continue;
                        }
                        if (jugador.gameMode.destroyBlock(pos)) {
                            nivel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, estado),
                                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.05);
                        }
                    }
                }
            }
        } finally {
            enCurso = false;
            // Onda y anillo de notas
            nivel.sendParticles(ParticleTypes.SONIC_BOOM, jugador.getX(), jugador.getY() + 1.0, jugador.getZ(), 1, 0, 0, 0, 0);
            for (int i = 0; i < 12; i++) {
                double angulo = Math.PI * 2 * i / 12;
                nivel.sendParticles(ParticleTypes.NOTE, jugador.getX() + Math.cos(angulo) * (radio + 0.5), jugador.getY() + 1.0,
                        jugador.getZ() + Math.sin(angulo) * (radio + 0.5), 0, i / 12.0, 0, 0, 1.0);
            }
        }
    }
}
