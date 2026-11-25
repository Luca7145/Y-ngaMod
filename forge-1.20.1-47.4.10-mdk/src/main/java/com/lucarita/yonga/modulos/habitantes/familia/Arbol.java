package com.lucarita.yonga.modulos.habitantes.familia;

import com.lucarita.yonga.modulos.habitantes.Habitante;
import com.lucarita.yonga.modulos.habitantes.red.FamiliaPacket;
import com.lucarita.yonga.modulos.habitantes.red.HabitantesRed;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * El árbol familiar de un jugador (tecla J o /yonga familia): sus padres, hermanos, parejas (novios, casados y los
 * que murieron), hijos (con quién) y nietos. Sale de Familias, así que incluye a los que no están cargados o han muerto.
 */
public final class Arbol {

    private Arbol() {}

    public static void enviar(ServerPlayer j) {
        Familias f = Familias.de(j.getServer());
        f.apuntar(j.getUUID(), j.getGameProfile().getName(), f.persona(j.getUUID()).mujer, true);
        UUID yo = j.getUUID();
        List<FamiliaPacket.Nodo> nodos = new ArrayList<>();
        for (UUID u : f.padres(yo)) nodos.add(nodo(j, f, u, FamiliaPacket.PADRE, 0, -1));
        for (UUID u : f.hermanos(yo)) nodos.add(nodo(j, f, u, FamiliaPacket.HERMANO, 0, -1));
        // Parejas: las vivas con su estado y los difuntos con los que estuvo casado
        List<UUID> parejas = new ArrayList<>();
        Familias.Persona p = f.persona(yo);
        for (var e : p.parejas.entrySet()) {
            parejas.add(e.getKey());
            nodos.add(nodo(j, f, e.getKey(), FamiliaPacket.PAREJA, e.getValue().estado, -1));
        }
        // Hijos (agrupados por el otro progenitor) y nietos
        List<UUID> hijos = f.hijos(yo);
        for (UUID h : hijos) {
            int grupo = -1;
            for (UUID otro : f.padres(h)) {
                if (otro.equals(yo)) continue;
                grupo = parejas.indexOf(otro);
                if (grupo < 0) {                         // el otro progenitor ya no es pareja: se añade igual
                    parejas.add(otro);
                    nodos.add(nodo(j, f, otro, FamiliaPacket.PAREJA, 0, -1));
                    grupo = parejas.size() - 1;
                }
            }
            nodos.add(nodo(j, f, h, FamiliaPacket.HIJO, 0, grupo));
        }
        for (int i = 0; i < hijos.size(); i++) {
            for (UUID n : f.hijos(hijos.get(i))) nodos.add(nodo(j, f, n, FamiliaPacket.NIETO, 0, i));
        }
        HabitantesRed.CANAL.send(PacketDistributor.PLAYER.with(() -> j),
                new FamiliaPacket(j.getGameProfile().getName(), p.esMujer(), nodos));
    }

    private static FamiliaPacket.Nodo nodo(ServerPlayer j, Familias f, UUID u, int papel, int estado, int grupo) {
        Familias.Persona p = f.persona(u);
        int etapa = -1;
        for (ServerLevel nivel : j.getServer().getAllLevels()) {
            Entity e = nivel.getEntity(u);
            if (e instanceof Habitante h) {
                etapa = h.etapa();
                break;
            }
        }
        String nombre = p.nombre.isEmpty() ? "?" : p.nombre;
        return new FamiliaPacket.Nodo(papel, nombre, p.esMujer(), p.vivo, p.jugador, estado, grupo, etapa);
    }
}
