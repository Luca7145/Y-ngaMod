package com.lucarita.yonga.modulos.habitantes.red;

import com.lucarita.yonga.YongaMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Canal de red del módulo de habitantes. Si cambias el formato de un paquete, sube VERSION. */
public final class HabitantesRed {

    private static final String VERSION = "13";

    public static final SimpleChannel CANAL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(YongaMain.MOD_ID, "habitantes"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);

    private HabitantesRed() {}

    public static void registrar() {
        int id = 0;
        CANAL.messageBuilder(TiendaEstadoPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TiendaEstadoPacket::codificar)
                .decoder(TiendaEstadoPacket::decodificar)
                .consumerMainThread(TiendaEstadoPacket::manejar)
                .add();
        CANAL.messageBuilder(TiendaAccionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TiendaAccionPacket::codificar)
                .decoder(TiendaAccionPacket::decodificar)
                .consumerMainThread(TiendaAccionPacket::manejar)
                .add();
        CANAL.messageBuilder(ConversacionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ConversacionPacket::codificar)
                .decoder(ConversacionPacket::decodificar)
                .consumerMainThread(ConversacionPacket::manejar)
                .add();
        CANAL.messageBuilder(AbrirConversacionPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AbrirConversacionPacket::codificar)
                .decoder(AbrirConversacionPacket::decodificar)
                .consumerMainThread(AbrirConversacionPacket::manejar)
                .add();
        CANAL.messageBuilder(LineaConversacionPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LineaConversacionPacket::codificar)
                .decoder(LineaConversacionPacket::decodificar)
                .consumerMainThread(LineaConversacionPacket::manejar)
                .add();
        CANAL.messageBuilder(BurbujaPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BurbujaPacket::codificar)
                .decoder(BurbujaPacket::decodificar)
                .consumerMainThread(BurbujaPacket::manejar)
                .add();
        CANAL.messageBuilder(FundidoPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FundidoPacket::codificar)
                .decoder(FundidoPacket::decodificar)
                .consumerMainThread(FundidoPacket::manejar)
                .add();
        CANAL.messageBuilder(BarrigaPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BarrigaPacket::codificar)
                .decoder(BarrigaPacket::decodificar)
                .consumerMainThread(BarrigaPacket::manejar)
                .add();
        CANAL.messageBuilder(PedirSexoPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PedirSexoPacket::codificar)
                .decoder(PedirSexoPacket::decodificar)
                .consumerMainThread(PedirSexoPacket::manejar)
                .add();
        CANAL.messageBuilder(SexoPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SexoPacket::codificar)
                .decoder(SexoPacket::decodificar)
                .consumerMainThread(SexoPacket::manejar)
                .add();
        CANAL.messageBuilder(EditorPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EditorPacket::codificar)
                .decoder(EditorPacket::decodificar)
                .consumerMainThread(EditorPacket::manejar)
                .add();
        CANAL.messageBuilder(GuardarEditorPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GuardarEditorPacket::codificar)
                .decoder(GuardarEditorPacket::decodificar)
                .consumerMainThread(GuardarEditorPacket::manejar)
                .add();
        CANAL.messageBuilder(RelacionPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RelacionPacket::codificar)
                .decoder(RelacionPacket::decodificar)
                .consumerMainThread(RelacionPacket::manejar)
                .add();
        CANAL.messageBuilder(EncimaPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EncimaPacket::codificar)
                .decoder(EncimaPacket::decodificar)
                .consumerMainThread(EncimaPacket::manejar)
                .add();
        CANAL.messageBuilder(EncargosPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EncargosPacket::codificar)
                .decoder(EncargosPacket::decodificar)
                .consumerMainThread(EncargosPacket::manejar)
                .add();
        CANAL.messageBuilder(EncargoAccionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(EncargoAccionPacket::codificar)
                .decoder(EncargoAccionPacket::decodificar)
                .consumerMainThread(EncargoAccionPacket::manejar)
                .add();
        CANAL.messageBuilder(FamiliaPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FamiliaPacket::codificar)
                .decoder(FamiliaPacket::decodificar)
                .consumerMainThread(FamiliaPacket::manejar)
                .add();
        CANAL.messageBuilder(PedirFamiliaPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PedirFamiliaPacket::codificar)
                .decoder(PedirFamiliaPacket::decodificar)
                .consumerMainThread(PedirFamiliaPacket::manejar)
                .add();
        CANAL.messageBuilder(NombrarBebePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(NombrarBebePacket::codificar)
                .decoder(NombrarBebePacket::decodificar)
                .consumerMainThread(NombrarBebePacket::manejar)
                .add();
        CANAL.messageBuilder(NombreBebePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(NombreBebePacket::codificar)
                .decoder(NombreBebePacket::decodificar)
                .consumerMainThread(NombreBebePacket::manejar)
                .add();
        CANAL.messageBuilder(CronicaPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CronicaPacket::codificar)
                .decoder(CronicaPacket::decodificar)
                .consumerMainThread(CronicaPacket::manejar)
                .add();
    }
}
