package com.lucarita.yonga.modulos.roles.oficios.mochila;

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
 * Mochila del Explorador. Click derecho para abrirla.
 *  - Mochila: 18 huecos (2 filas completas), hasta 100 de cada objeto por hueco.
 *  - Mochila mejorada: 27 huecos (3 filas completas), hasta 200 por hueco.
 * Lo que lleva dentro se guarda en el propio objeto, así que puedes dejarla en un cofre y no pierdes nada.
 */
public class MochilaItem extends Item {

    private final int huecos;
    private final int limite;

    public MochilaItem(int huecos, int limite, Properties propiedades) {
        super(propiedades.stacksTo(1));
        this.huecos = huecos;
        this.limite = limite;
    }

    public int huecos() {
        return huecos;
    }

    public int limite() {
        return limite;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        ItemStack mochila = jugador.getItemInHand(mano);
        if (!nivel.isClientSide() && jugador instanceof ServerPlayer servidor) {
            NetworkHooks.openScreen(servidor,
                    new SimpleMenuProvider((id, inventario, j) -> new MochilaMenu(id, inventario, mano, huecos, limite),
                            mochila.getHoverName()),
                    datos -> {
                        // Lo que necesita el cliente para montar el mismo menú
                        datos.writeEnum(mano);
                        datos.writeVarInt(huecos);
                        datos.writeVarInt(limite);
                    });
            nivel.playSound(null, jugador.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        return InteractionResultHolder.sidedSuccess(mochila, nivel.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level nivel, List<Component> lineas, TooltipFlag flag) {
        lineas.add(Component.literal(MochilaContenido.huecosOcupados(stack) + "/" + huecos)
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, nivel, lineas, flag);
    }
}
