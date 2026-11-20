package com.lucarita.yonga.modulos.roles.red;

import com.lucarita.yonga.YongaMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Canal de red del módulo de roles. Si cambias el formato de un paquete, sube VERSION. */
public final class RolesRed {

    private static final String VERSION = "8";

    public static final SimpleChannel CANAL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(YongaMain.MOD_ID, "roles"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);

    private RolesRed() {}

    public static void registrar() {
        int id = 0;
        CANAL.messageBuilder(SincronizarRolPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SincronizarRolPacket::codificar)
                .decoder(SincronizarRolPacket::decodificar)
                .consumerMainThread(SincronizarRolPacket::manejar)
                .add();
        CANAL.messageBuilder(ElegirRolPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ElegirRolPacket::codificar)
                .decoder(ElegirRolPacket::decodificar)
                .consumerMainThread(ElegirRolPacket::manejar)
                .add();
        CANAL.messageBuilder(HuecoMochilaPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HuecoMochilaPacket::codificar)
                .decoder(HuecoMochilaPacket::decodificar)
                .consumerMainThread(HuecoMochilaPacket::manejar)
                .add();
        CANAL.messageBuilder(CaraPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CaraPacket::codificar)
                .decoder(CaraPacket::decodificar)
                .consumerMainThread(CaraPacket::manejar)
                .add();
        CANAL.messageBuilder(SilenciarNotasPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SilenciarNotasPacket::codificar)
                .decoder(SilenciarNotasPacket::decodificar)
                .consumerMainThread(SilenciarNotasPacket::manejar)
                .add();
        CANAL.messageBuilder(TocarArmonicaPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TocarArmonicaPacket::codificar)
                .decoder(TocarArmonicaPacket::decodificar)
                .consumerMainThread(TocarArmonicaPacket::manejar)
                .add();
        CANAL.messageBuilder(RadiocasetePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RadiocasetePacket::codificar)
                .decoder(RadiocasetePacket::decodificar)
                .consumerMainThread(RadiocasetePacket::manejar)
                .add();
        CANAL.messageBuilder(EscanerPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EscanerPacket::codificar)
                .decoder(EscanerPacket::decodificar)
                .consumerMainThread(EscanerPacket::manejar)
                .add();
    }
}
