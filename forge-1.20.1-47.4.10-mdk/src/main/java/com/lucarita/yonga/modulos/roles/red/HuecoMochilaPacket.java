package com.lucarita.yonga.modulos.roles.red;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Servidor -> cliente: "en el hueco X del menú abierto hay este objeto, con esta cantidad".
 * Solo se usa para montones de más de 127 (mochila mejorada), que Minecraft no sabe enviar.
 */
public record HuecoMochilaPacket(int idMenu, int hueco, ItemStack objeto, int cantidad) {

    public void codificar(FriendlyByteBuf buffer) {
        buffer.writeVarInt(idMenu);
        buffer.writeVarInt(hueco);
        buffer.writeItem(objeto);
        buffer.writeVarInt(cantidad);
    }

    public static HuecoMochilaPacket decodificar(FriendlyByteBuf buffer) {
        return new HuecoMochilaPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readItem(), buffer.readVarInt());
    }

    // Se ejecuta en el cliente (la parte que toca la pantalla está en una clase solo de cliente)
    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        if (FMLEnvironment.dist.isClient()) {
            com.lucarita.yonga.client.roles.MochilaCliente.actualizarHueco(idMenu, hueco, objeto.copyWithCount(cantidad));
        }
    }
}
