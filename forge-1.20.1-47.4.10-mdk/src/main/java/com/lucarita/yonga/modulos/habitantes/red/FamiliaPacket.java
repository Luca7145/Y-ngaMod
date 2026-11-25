package com.lucarita.yonga.modulos.habitantes.red;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Servidor -> jugador: su árbol familiar. Cada persona lleva su papel (padre, pareja, hermano, hijo, nieto), su
 * nombre, si es mujer, si vive y, para hijos y nietos, de quién son (índice de la pareja o del hijo).
 */
public record FamiliaPacket(String yo, boolean yoMujer, List<Nodo> nodos) {

    public static final int PADRE = 0, PAREJA = 1, HERMANO = 2, HIJO = 3, NIETO = 4;

    /** estado: para parejas, 1 novios · 2 casados; grupo: hijos → índice de la pareja; nietos → índice del hijo. */
    public record Nodo(int papel, String nombre, boolean mujer, boolean vivo, boolean jugador, int estado, int grupo,
                       int etapa) {}

    public void codificar(FriendlyByteBuf b) {
        b.writeUtf(yo, 64);
        b.writeBoolean(yoMujer);
        b.writeVarInt(nodos.size());
        for (Nodo n : nodos) {
            b.writeByte(n.papel());
            b.writeUtf(n.nombre(), 64);
            b.writeBoolean(n.mujer());
            b.writeBoolean(n.vivo());
            b.writeBoolean(n.jugador());
            b.writeByte(n.estado());
            b.writeVarInt(n.grupo() + 1);
            b.writeByte(n.etapa() + 1);
        }
    }

    public static FamiliaPacket decodificar(FriendlyByteBuf b) {
        String yo = b.readUtf(64);
        boolean mujer = b.readBoolean();
        int n = Math.min(b.readVarInt(), 200);
        List<Nodo> lista = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            lista.add(new Nodo(b.readByte(), b.readUtf(64), b.readBoolean(), b.readBoolean(), b.readBoolean(), b.readByte(),
                    b.readVarInt() - 1, b.readByte() - 1));
        }
        return new FamiliaPacket(yo, mujer, lista);
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.lucarita.yonga.client.habitantes.FamiliaScreen.abrir(this));
    }
}
