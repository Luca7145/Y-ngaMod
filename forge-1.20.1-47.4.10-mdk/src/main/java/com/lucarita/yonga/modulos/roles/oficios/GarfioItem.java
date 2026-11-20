package com.lucarita.yonga.modulos.roles.oficios;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * GARFIO (Explorador, Maestro). Click derecho: lanza el gancho (ver GarfioEntity), que se clava en el primer bloque que
 * toca y tira de ti hasta él. Otra vez click derecho o mayúsculas: lo sueltas.
 *
 * Mientras el gancho está fuera, el garfio de la mano se dibuja sin él (CustomModelData 1; el gancho suelto es el 2,
 * ver models/item/garfio.json).
 */
public class GarfioItem extends Item {

    private static final int DURABILIDAD = 300;
    private static final int ESPERA = 8;                     // ticks entre un lanzamiento y otro
    private static final String CLAVE_MODELO = "CustomModelData";

    public GarfioItem(Properties propiedades) {
        super(propiedades.durability(DURABILIDAD));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        ItemStack objeto = jugador.getItemInHand(mano);
        if (!nivel.isClientSide() && jugador instanceof ServerPlayer servidor) {
            if (GarfioEntity.lanzado(jugador.getUUID())) {
                GarfioEntity.soltar(jugador.getUUID());
                marcarLanzado(objeto, false);
            } else {
                GarfioEntity.lanzar(servidor);
                marcarLanzado(objeto, true);
                objeto.hurtAndBreak(1, jugador, j -> j.broadcastBreakEvent(mano));
                jugador.getCooldowns().addCooldown(this, ESPERA);
            }
        }
        return InteractionResultHolder.sidedSuccess(objeto, nivel.isClientSide());
    }

    /** El gancho solo, para dibujarlo en vuelo. */
    public static ItemStack soloGancho(ItemStack garfio) {
        ItemStack copia = garfio.copy();
        copia.getOrCreateTag().putInt(CLAVE_MODELO, 2);
        return copia;
    }

    private static void marcarLanzado(ItemStack objeto, boolean lanzado) {
        if (lanzado) {
            objeto.getOrCreateTag().putInt(CLAVE_MODELO, 1);
        } else if (objeto.hasTag()) {
            objeto.getTag().remove(CLAVE_MODELO);
            if (objeto.getTag().isEmpty()) {
                objeto.setTag(null);
            }
        }
    }

    // Si el gancho se ha soltado solo (o se ha guardado el garfio con él fuera), vuelve a dibujarse entero
    @Override
    public void inventoryTick(ItemStack objeto, Level nivel, Entity entidad, int hueco, boolean enMano) {
        if (!nivel.isClientSide() && entidad.tickCount % 5 == 0 && objeto.hasTag() && objeto.getTag().contains(CLAVE_MODELO)
                && !GarfioEntity.lanzado(entidad.getUUID())) {
            marcarLanzado(objeto, false);
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack antes, ItemStack despues, boolean cambioDeHueco) {
        return cambioDeHueco || !ItemStack.isSameItem(antes, despues);
    }

    @Override
    public void appendHoverText(ItemStack objeto, @Nullable Level nivel, List<Component> texto, TooltipFlag flag) {
        texto.add(Component.translatable("yonga.garfio.uso").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
