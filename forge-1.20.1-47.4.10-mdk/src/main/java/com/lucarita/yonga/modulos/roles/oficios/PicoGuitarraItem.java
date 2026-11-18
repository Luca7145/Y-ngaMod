package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.core.GolpeCargado;
import com.lucarita.yonga.core.ModTiers;
import com.lucarita.yonga.modulos.roles.ModuloRoles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pico-guitarra del Melómano: un pico de lucarita que además es una guitarra eléctrica.
 *
 *  - Click izquierdo: pica como un pico de lucarita y golpea (suena una nota al golpear).
 *  - Click derecho: toca una nota (abajo = grave, arriba = agudo, 2 octavas), como los otros instrumentos.
 *  - Ritmo (ver Musica): las notas, los golpes cargados y además CADA BLOQUE QUE PICAS con él (si es un
 *    bloque que el pico puede sacar) llenan la barra. Picando, la barra aguanta 3 s sin vaciarse.
 *  - Con la barra llena, click derecho = SOLO DE GUITARRA: pica de golpe todos los bloques que tienes
 *    alrededor a 1 bloque de distancia (los 8 de alrededor, a la altura de los pies, del pecho y de encima
 *    de la cabeza, y los 8 del suelo de alrededor) con dureza 3,5 o menos. El bloque justo debajo de tus
 *    pies no se rompe, para que no te caigas. No rompe cofres, hornos ni nada que guarde cosas, ni bloques
 *    que se rompen al instante (antorchas, flores...). Respeta las protecciones de los servidores.
 *    Cada bloque gasta durabilidad como si lo picases a mano.
 *  - Durabilidad: como el bajo y el keytar (2200). Solo se gasta al golpear y al romper bloques. Tocar no la gasta.
 */
public class PicoGuitarraItem extends PickaxeItem {

    // ---- Ajustes ----
    private static final int DANO_EXTRA = 1;               // daño en el juego: 1 + 4 (lucarita) + 1 = 6 (como un pico de netherita)
    private static final float VELOCIDAD = -2.8f;          // velocidad de ataque como cualquier pico (1,2)
    private static final int DURABILIDAD = 2200;
    private static final int RITMO_POR_BLOQUE = 10;
    private static final float DUREZA_MAXIMA = 3.5f;

    public PicoGuitarraItem(Properties propiedades) {
        super(ModTiers.LUCARITA, DANO_EXTRA, VELOCIDAD, propiedades.durability(DURABILIDAD));
    }

    // ---------- CLICK DERECHO: nota o solo ----------

    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        ItemStack pico = jugador.getItemInHand(mano);
        if (jugador instanceof ServerPlayer servidor) {
            if (Musica.ritmoLleno(servidor)) {
                soloDeGuitarra(servidor);
                Musica.vaciarRitmo(servidor);
            } else {
                Musica.tocarNota(servidor, ModuloRoles.SONIDO_GUITARRA_NOTA.get());
                Musica.sumarRitmo(servidor, Musica.RITMO_POR_NOTA);
            }
        }
        // "consume" = no hace el gesto de golpear al tocar
        return InteractionResultHolder.consume(pico);
    }

    // ---------- PICAR: recarga el ritmo ----------

    @Override
    public boolean mineBlock(ItemStack pico, Level nivel, BlockState estado, BlockPos pos, LivingEntity minero) {
        if (!Excavacion.enCurso() && minero instanceof ServerPlayer jugador
                && estado.getDestroySpeed(nivel, pos) > 0 && pico.isCorrectToolForDrops(estado)) {
            Musica.tocarNota(jugador, ModuloRoles.SONIDO_GUITARRA_NOTA.get());
            Musica.sumarRitmo(jugador, RITMO_POR_BLOQUE, Musica.PAUSA_PICANDO);
        }
        return super.mineBlock(pico, nivel, estado, pos, minero);
    }

    // ---------- GOLPEAR ----------

    @Override
    public boolean hurtEnemy(ItemStack pico, LivingEntity objetivo, LivingEntity atacante) {
        if (atacante instanceof ServerPlayer jugador) {
            Musica.tocarNota(jugador, ModuloRoles.SONIDO_GUITARRA_NOTA.get());
            if (GolpeCargado.fue(jugador)) {
                Musica.sumarRitmo(jugador, Musica.RITMO_POR_GOLPE);
            }
        }
        // 1 de durabilidad por golpe (como una espada), no 2 como un pico normal
        pico.hurtAndBreak(1, atacante, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    // ---------- SOLO DE GUITARRA ----------

    private void soloDeGuitarra(ServerPlayer jugador) {
        jugador.serverLevel().playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(),
                ModuloRoles.SONIDO_GUITARRA_ACORDE.get(), SoundSource.RECORDS, 1.2f, 1.0f);
        Excavacion.excavar(jugador, this, 1, DUREZA_MAXIMA, estado -> true);
    }
}
