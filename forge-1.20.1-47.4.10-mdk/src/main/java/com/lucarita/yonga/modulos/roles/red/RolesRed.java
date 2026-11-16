package com.lucarita.yonga.modulos.roles.red;

import com.lucarita.yonga.YongaMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Canal de red del módulo de roles. Si cambias el formato de un paquete, sube VERSION. */
public final class RolesRed {

    private static final String VERSION = "1";

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
    }
}
