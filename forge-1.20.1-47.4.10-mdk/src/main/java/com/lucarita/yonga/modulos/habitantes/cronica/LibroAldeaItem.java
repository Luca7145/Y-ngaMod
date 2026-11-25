package com.lucarita.yonga.modulos.habitantes.cronica;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Libro de la crónica (solo creativo): clic derecho y te cuenta todo de la aldea en la que estás. */
public class LibroAldeaItem extends Item {

    public LibroAldeaItem(Properties propiedades) {
        super(propiedades);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        ItemStack libro = jugador.getItemInHand(mano);
        if (!nivel.isClientSide && jugador instanceof ServerPlayer servidor) {
            if (!jugador.isCreative()) {
                jugador.displayClientMessage(Component.translatable("yonga.libro.solo_creativo"), true);
            } else {
                try {
                    Cronica.enviar(servidor);
                } catch (RuntimeException e) {
                    // Que un fallo no deje el libro en blanco sin más: se dice en el chat y queda en el registro
                    com.mojang.logging.LogUtils.getLogger().error("Yonga - el libro de la aldea ha fallado", e);
                    jugador.sendSystemMessage(Component.literal("[Yonga] El libro de la aldea ha fallado: " + e));
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(libro, nivel.isClientSide);
    }
}
