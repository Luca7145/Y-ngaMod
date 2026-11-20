package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.modulos.fe.mobs.rey.ReyDeArenisca;
import com.lucarita.yonga.modulos.fe.red.AgarreReyPacket;
import net.minecraft.client.Minecraft;

/** Cliente del Rey de Arenisca: recibe dónde está agarrado cada jugador (para colocarlo sobre el rey). */
public final class ClienteRey {

    private ClienteRey() {}

    public static void recibir(AgarreReyPacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getEntity(p.rey()) instanceof ReyDeArenisca rey) {
            rey.agarreCliente(p.jugador(), p.superficie(), p.u(), p.v());
        }
    }
}
