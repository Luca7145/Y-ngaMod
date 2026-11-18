package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.core.GolpeCargado;
import com.lucarita.yonga.core.ModTiers;
import com.lucarita.yonga.modulos.roles.ModuloRoles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Espada-keytar del Melómano. Funciona como el bajo-hacha, pero su ataque especial es A DISTANCIA:
 *
 *  - Click izquierdo: golpea como una espada y suena una nota.
 *  - Click derecho: toca una nota de sintetizador (abajo = grave, arriba = agudo, 2 octavas).
 *  - Ritmo (ver Musica): cada nota y cada golpe cargado llenan la barra. Con la barra llena, la siguiente
 *    nota lanza una ONDA SONORA hacia donde miras, que atraviesa y daña a todos los mobs y jugadores
 *    que pille por el camino (el bajo-hacha, en cambio, hace su onda alrededor).
 *  - Durabilidad: la misma que el bajo-hacha (2200) y se gasta igual: 1 por golpe y 1 por bloque roto.
 *    Tocar no la gasta.
 */
public class EspadaKeytarItem extends SwordItem {

    // ---- Ajustes ----
    private static final int DANO_EXTRA = 4;                // daño en el juego: 1 + 4 (lucarita) + 4 = 9
    private static final float VELOCIDAD = -2.2f;           // velocidad en el juego: 4 - 2,2 = 1,8
    public static final int DURABILIDAD = 2200;             // como el bajo-hacha, un poco más que la netherita

    private static final float VELOCIDAD_ONDA = 1.8f;       // bloques por tick

    public EspadaKeytarItem(Properties propiedades) {
        super(ModTiers.LUCARITA, DANO_EXTRA, VELOCIDAD, propiedades.durability(DURABILIDAD));
    }

    // Click derecho: tocar (o lanzar la onda si la barra de ritmo está llena)
    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        ItemStack keytar = jugador.getItemInHand(mano);
        if (!nivel.isClientSide() && jugador instanceof ServerPlayer servidor) {
            if (Musica.ritmoLleno(servidor)) {
                lanzarOnda(servidor);
                Musica.vaciarRitmo(servidor);
            } else {
                Musica.tocarNota(servidor, ModuloRoles.SONIDO_KEYTAR_NOTA.get());
                Musica.sumarRitmo(servidor, Musica.RITMO_POR_NOTA);
            }
        }
        // "consume" = no hace el gesto de golpear al tocar
        return InteractionResultHolder.consume(keytar);
    }

    private static void lanzarOnda(ServerPlayer jugador) {
        Level nivel = jugador.level();
        NotaProyectil onda = new NotaProyectil(nivel, jugador);
        onda.shootFromRotation(jugador, jugador.getXRot(), jugador.getYRot(), 0.0f, VELOCIDAD_ONDA, 0.0f);
        nivel.addFreshEntity(onda);
        nivel.playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(),
                ModuloRoles.SONIDO_KEYTAR_NOTA.get(), SoundSource.RECORDS, 1.5f, 0.5f);
        nivel.playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    // Golpe con la espada: suena una nota y, si es un golpe cargado, suma ritmo (la durabilidad la gasta la espada: 1)
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity objetivo, LivingEntity atacante) {
        if (atacante instanceof ServerPlayer jugador) {
            Musica.tocarNota(jugador, ModuloRoles.SONIDO_KEYTAR_NOTA.get());
            if (GolpeCargado.fue(jugador)) {
                Musica.sumarRitmo(jugador, Musica.RITMO_POR_GOLPE);
            }
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
}
