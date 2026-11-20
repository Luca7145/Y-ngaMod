package com.lucarita.yonga.modulos.roles.red;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.RecordItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

/**
 * Servidor -> clientes: qué hace el radiocasete del jugador con este id (empezar un disco, pausa, seguir, parar).
 * Lo maneja client/roles/RadiocaseteCliente.
 */
public record RadiocasetePacket(int idEntidad, int accion, Item disco, int desde) {

    public static final int TOCAR = 0;
    public static final int PAUSAR = 1;
    public static final int CONTINUAR = 2;
    public static final int PARAR = 3;

    /** Empezar el disco por el tick "desde" (0 = desde el principio). */
    public static RadiocasetePacket tocar(int idEntidad, RecordItem disco, int desde) {
        return new RadiocasetePacket(idEntidad, TOCAR, disco, desde);
    }

    public static RadiocasetePacket accion(int idEntidad, int accion) {
        return new RadiocasetePacket(idEntidad, accion, Items.AIR, 0);
    }

    public void codificar(FriendlyByteBuf buffer) {
        buffer.writeVarInt(idEntidad);
        buffer.writeByte(accion);
        buffer.writeRegistryId(ForgeRegistries.ITEMS, disco);
        buffer.writeVarInt(desde);
    }

    public static RadiocasetePacket decodificar(FriendlyByteBuf buffer) {
        int id = buffer.readVarInt();
        int accion = buffer.readByte();
        Item disco = buffer.readRegistryId();
        return new RadiocasetePacket(id, accion, disco, buffer.readVarInt());
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.lucarita.yonga.client.roles.RadiocaseteCliente.recibir(idEntidad, accion, disco, desde));
    }
}
