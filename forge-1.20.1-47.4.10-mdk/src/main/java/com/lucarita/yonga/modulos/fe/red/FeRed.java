package com.lucarita.yonga.modulos.fe.red;

import com.lucarita.yonga.YongaMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Canal de red del módulo de fe: por aquí el servidor le cuenta a cada jugador cuánta fe tiene.
 * Si algún día cambias el formato de un paquete, sube VERSION para que no se conecten versiones distintas.
 */
public final class FeRed {

    private static final String VERSION = "4";

    public static final SimpleChannel CANAL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(YongaMain.MOD_ID, "fe"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);

    private FeRed() {}

    public static void registrar() {
        int id = 0;
        CANAL.messageBuilder(SincronizarFePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SincronizarFePacket::codificar)
                .decoder(SincronizarFePacket::decodificar)
                .consumerMainThread(SincronizarFePacket::manejar)
                .add();
        CANAL.messageBuilder(EstadoOracionPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EstadoOracionPacket::codificar)
                .decoder(EstadoOracionPacket::decodificar)
                .consumerMainThread(EstadoOracionPacket::manejar)
                .add();
        CANAL.messageBuilder(PedirOracionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PedirOracionPacket::codificar)
                .decoder(PedirOracionPacket::decodificar)
                .consumerMainThread(PedirOracionPacket::manejar)
                .add();
        CANAL.messageBuilder(JumpscarePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(JumpscarePacket::codificar)
                .decoder(JumpscarePacket::decodificar)
                .consumerMainThread(JumpscarePacket::manejar)
                .add();
    }
}
