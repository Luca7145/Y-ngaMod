package com.lucarita.yonga.modulos.habitantes.red;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Servidor -> jugador: los encargos del tablón de una aldea (abre o refresca la pantalla).
 * estado: 0 abierto · 1 aceptado (caza, con progreso) · 2 lo cumplió otro · 3 lo cumpliste tú.
 */
public record EncargosPacket(BlockPos campana, boolean tabernero, List<Entrada> encargos) {

    public record Entrada(int id, boolean caza, ItemStack icono, Component texto, int monedas, int fama, int estado,
                          int progreso, int total, String ganador) {}

    public void codificar(FriendlyByteBuf b) {
        b.writeBlockPos(campana);
        b.writeBoolean(tabernero);
        b.writeVarInt(encargos.size());
        for (Entrada e : encargos) {
            b.writeVarInt(e.id());
            b.writeBoolean(e.caza());
            b.writeItem(e.icono());
            b.writeComponent(e.texto());
            b.writeVarInt(e.monedas());
            b.writeVarInt(e.fama());
            b.writeByte(e.estado());
            b.writeVarInt(e.progreso());
            b.writeVarInt(e.total());
            b.writeUtf(e.ganador(), 64);
        }
    }

    public static EncargosPacket decodificar(FriendlyByteBuf b) {
        BlockPos campana = b.readBlockPos();
        boolean tabernero = b.readBoolean();
        int n = Math.min(b.readVarInt(), 32);
        List<Entrada> lista = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            lista.add(new Entrada(b.readVarInt(), b.readBoolean(), b.readItem(), b.readComponent(), b.readVarInt(),
                    b.readVarInt(), b.readByte(), b.readVarInt(), b.readVarInt(), b.readUtf(64)));
        }
        return new EncargosPacket(campana, tabernero, lista);
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.lucarita.yonga.client.habitantes.EncargosScreen.abrir(this));
    }
}
