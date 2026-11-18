package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.core.GolpeCargado;
import com.lucarita.yonga.core.ModTiers;
import com.lucarita.yonga.modulos.roles.ModuloRoles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Banjo-pala del Melómano: una pala de lucarita que además es un banjo.
 *
 *  - Click izquierdo: cava como una pala de lucarita y golpea (suena una nota al golpear).
 *  - Click derecho: toca una nota (abajo = grave, arriba = agudo, 2 octavas). No hace caminos de tierra
 *    (como el bajo, que no descorteza troncos), para poder tocar apuntando al suelo.
 *  - Ritmo (ver Musica): las notas, los golpes cargados y CADA BLOQUE QUE CAVAS con él (tierra, arena,
 *    grava...) llenan la barra. Cavando, la barra aguanta 3 s sin vaciarse.
 *  - Con la barra llena, click derecho = PUNTEO DE BANJO: cava de golpe todo lo blando que tienes alrededor
 *    a 2 bloques de distancia: solo bloques de pala débiles (tierra, hierba, arena, grava, arcilla, barro,
 *    nieve, arena de almas, cemento en polvo...). El bloque justo debajo de tus pies no se rompe.
 *    Cada bloque gasta durabilidad como si lo cavases a mano.
 *  - Durabilidad: como los demás instrumentos (2200). Solo se gasta al golpear y al romper bloques.
 */
public class BanjoPalaItem extends ShovelItem {

    // ---- Ajustes ----
    private static final float DANO_EXTRA = 1.5f;          // daño en el juego: 1 + 4 (lucarita) + 1,5 = 6,5 (como una pala de netherita)
    private static final float VELOCIDAD = -3.0f;          // velocidad de ataque como cualquier pala (1,0)
    private static final int DURABILIDAD = 2200;
    private static final int RITMO_POR_BLOQUE = 10;
    private static final int RADIO_PUNTEO = 2;
    private static final float DUREZA_MAXIMA = 1.0f;       // "débil": tierra 0,5, grava 0,6, arcilla 0,6...

    public BanjoPalaItem(Properties propiedades) {
        super(ModTiers.LUCARITA, DANO_EXTRA, VELOCIDAD, propiedades.durability(DURABILIDAD));
    }

    // Click derecho apuntando a un bloque: no hace caminos, así que sigue tocando
    @Override
    public InteractionResult useOn(UseOnContext contexto) {
        return InteractionResult.PASS;
    }

    // ---------- CLICK DERECHO: nota o punteo ----------

    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        ItemStack banjo = jugador.getItemInHand(mano);
        if (jugador instanceof ServerPlayer servidor) {
            if (Musica.ritmoLleno(servidor)) {
                punteo(servidor);
                Musica.vaciarRitmo(servidor);
            } else {
                Musica.tocarNota(servidor, ModuloRoles.SONIDO_BANJO_NOTA.get());
                Musica.sumarRitmo(servidor, Musica.RITMO_POR_NOTA);
            }
        }
        // "consume" = no hace el gesto de golpear al tocar
        return InteractionResultHolder.consume(banjo);
    }

    // ---------- CAVAR: recarga el ritmo ----------

    @Override
    public boolean mineBlock(ItemStack banjo, Level nivel, BlockState estado, BlockPos pos, LivingEntity minero) {
        if (!Excavacion.enCurso() && minero instanceof ServerPlayer jugador
                && estado.getDestroySpeed(nivel, pos) > 0 && banjo.isCorrectToolForDrops(estado)) {
            Musica.tocarNota(jugador, ModuloRoles.SONIDO_BANJO_NOTA.get());
            Musica.sumarRitmo(jugador, RITMO_POR_BLOQUE, Musica.PAUSA_PICANDO);
        }
        return super.mineBlock(banjo, nivel, estado, pos, minero);
    }

    // ---------- GOLPEAR ----------

    @Override
    public boolean hurtEnemy(ItemStack banjo, LivingEntity objetivo, LivingEntity atacante) {
        if (atacante instanceof ServerPlayer jugador) {
            Musica.tocarNota(jugador, ModuloRoles.SONIDO_BANJO_NOTA.get());
            if (GolpeCargado.fue(jugador)) {
                Musica.sumarRitmo(jugador, Musica.RITMO_POR_GOLPE);
            }
        }
        // 1 de durabilidad por golpe (como una espada), no 2 como una pala normal
        banjo.hurtAndBreak(1, atacante, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    // ---------- PUNTEO DE BANJO ----------

    private void punteo(ServerPlayer jugador) {
        jugador.serverLevel().playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(),
                ModuloRoles.SONIDO_BANJO_PUNTEO.get(), SoundSource.RECORDS, 1.2f, 1.0f);
        Excavacion.excavar(jugador, this, RADIO_PUNTEO, DUREZA_MAXIMA,
                estado -> estado.is(BlockTags.MINEABLE_WITH_SHOVEL));
    }
}
