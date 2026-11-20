package com.lucarita.yonga.modulos.tabaco;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * La cajetilla: como la mochila, pero de un solo hueco, en el que solo caben cigarros SIN ESTRENAR (ni encendidos ni
 * gastados), hasta 10 apilados. Clic derecho para abrirla.
 */
public class CajetillaItem extends Item {

    public static final int CAPACIDAD = 10;
    private static final String CLAVE = "Cigarros";

    public CajetillaItem(Properties propiedades) {
        super(propiedades.stacksTo(1));
    }

    public static int cigarros(ItemStack cajetilla) {
        return cajetilla.hasTag() ? cajetilla.getTag().getInt(CLAVE) : 0;
    }

    public static void ponerCigarros(ItemStack cajetilla, int cantidad) {
        if (cantidad <= 0) {
            if (cajetilla.hasTag()) cajetilla.getTag().remove(CLAVE);
        } else {
            cajetilla.getOrCreateTag().putInt(CLAVE, Math.min(cantidad, CAPACIDAD));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        ItemStack cajetilla = jugador.getItemInHand(mano);
        if (!nivel.isClientSide() && jugador instanceof ServerPlayer servidor) {
            NetworkHooks.openScreen(servidor,
                    new SimpleMenuProvider((id, inventario, j) -> new CajetillaMenu(id, inventario, mano), cajetilla.getHoverName()),
                    datos -> datos.writeEnum(mano));
            nivel.playSound(null, jugador.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1.4f);
        }
        return InteractionResultHolder.sidedSuccess(cajetilla, nivel.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level nivel, List<Component> lineas, TooltipFlag flag) {
        lineas.add(Component.literal(cigarros(stack) + "/" + CAPACIDAD).withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, nivel, lineas, flag);
    }
}
