package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.modulos.roles.ModuloRoles;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Flauta del Melómano. Click derecho a un mob: domarlo / quieto / sígueme (todo eso está en Companeros).
 * Click derecho al aire: toca una nota (abajo = grave, arriba = agudo). No usa el ritmo ni se gasta.
 */
public class FlautaItem extends Item {

    public FlautaItem(Properties propiedades) {
        super(propiedades.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        if (jugador instanceof ServerPlayer servidor) {
            Musica.tocarNota(servidor, ModuloRoles.SONIDO_FLAUTA_NOTA.get());
            jugador.getCooldowns().addCooldown(this, 4);
        }
        return InteractionResultHolder.consume(jugador.getItemInHand(mano));
    }
}
