package com.lucarita.yonga.modulos.habitantes.cronica;

import com.lucarita.yonga.modulos.habitantes.Gustos;
import com.lucarita.yonga.modulos.habitantes.Habitante;
import com.lucarita.yonga.modulos.habitantes.aldea.Aldeas;
import com.lucarita.yonga.modulos.habitantes.familia.Amor;
import com.lucarita.yonga.modulos.habitantes.familia.Familias;
import com.lucarita.yonga.modulos.habitantes.ia.Conversaciones;
import com.lucarita.yonga.modulos.habitantes.ia.Recuerdo;
import com.lucarita.yonga.modulos.habitantes.red.CronicaPacket;
import com.lucarita.yonga.modulos.habitantes.red.HabitantesRed;
import com.lucarita.yonga.modulos.habitantes.taberna.Encargos;
import com.lucarita.yonga.modulos.habitantes.taberna.Taberna;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * El libro de la crónica (solo creativo): todo lo de la aldea en la que estás. Resumen, tu fama, las parejas, cada
 * vecino (oficio, carácter, gustos, familia, pareja y aventuras, lo que opina de ti) y los rumores que corren.
 * Cuenta a los vecinos que están cargados (cerca).
 */
public final class Cronica {

    private Cronica() {}

    private static final String[] ETAPAS = {"bebé", "niño/a", "joven", "adulto/a"};
    private static final String[] ORIENTACION = {"le atrae el otro sexo", "le atrae su mismo sexo", "le atraen ambos"};

    public static void enviar(ServerPlayer j) {
        ServerLevel nivel = j.serverLevel();
        BlockPos cerca = Aldeas.campanaCerca(nivel, j.blockPosition());
        final BlockPos campana = cerca != null ? cerca
                : com.lucarita.yonga.modulos.habitantes.aldea.Poblados.reponerCampana(nivel, j.blockPosition());
        List<Component> l = new ArrayList<>();
        if (campana == null) {
            l.add(Component.translatable("yonga.cronica.sin_aldea").withStyle(ChatFormatting.GRAY));
            mandar(j, l);
            return;
        }
        long ahora = nivel.getGameTime();
        Aldeas datos = Aldeas.de(nivel);
        Aldeas.Aldea aldea = datos.aldea(campana);
        Familias f = Familias.de(nivel.getServer());
        List<Habitante> vecinos = nivel.getEntitiesOfClass(Habitante.class, new AABB(campana).inflate(Aldeas.RADIO),
                h -> h.isAlive() && campana.equals(h.aldea()));
        vecinos.sort(Comparator.comparingInt((Habitante h) -> -h.etapa())
                .thenComparing(Habitante::nombre, Comparator.nullsLast(Comparator.naturalOrder())));   // los bebés sin nombre, al final

        // ---- Resumen
        titulo(l, "Aldea de la campana (" + campana.getX() + ", " + campana.getY() + ", " + campana.getZ() + ")");
        int[] porEtapa = new int[4];
        int guardias = 0, parados = 0;
        Habitante lider = null;
        for (Habitante h : vecinos) {
            porEtapa[Math.min(3, h.etapa())]++;
            if (h.esGuardia()) guardias++;
            if (h.esLider()) lider = h;
            if (h.etapa() >= 2 && h.sinOficio() && !h.esLider()) parados++;
        }
        linea(l, "Vecinos: " + vecinos.size() + " (" + porEtapa[3] + " adultos, " + porEtapa[2] + " jóvenes, "
                + porEtapa[1] + " niños, " + porEtapa[0] + " bebés)");
        linea(l, "Alcalde: " + (lider == null ? "nadie" : lider.nombre()) + " · Guardias: " + guardias + " · Sin oficio: " + parados);
        Encargos.Datos taberna = Encargos.de(nivel).taberna(campana);
        Habitante tabernero = Taberna.tabernero(nivel, campana);
        int abiertos = 0;
        for (Encargos.Encargo e : taberna.encargos) if (e.abierto()) abiertos++;
        linea(l, "Taberna: " + (taberna.tablon == null ? "no tiene" : "sí, tablón en " + taberna.tablon.toShortString())
                + " · Tabernero: " + (tabernero == null ? "ninguno" : tabernero.nombre()) + " · Encargos abiertos: " + abiertos);

        // ---- Tú
        titulo(l, "Tú en esta aldea");
        linea(l, "Fama: " + aldea.fama(j.getUUID()) + " · Infidelidades que se te conocen: " + aldea.infidelidades(j.getUUID()));
        List<String> dichos = aldea.alias(j.getUUID(), null);
        linea(l, (dichos.size() > 1 ? "Nombres con los que te has presentado: " : "Nombre que has dicho: ")
                + (dichos.isEmpty() ? "ninguno" : String.join(", ", dichos)));

        // ---- Parejas
        titulo(l, "Parejas");
        Set<String> vistas = new HashSet<>();
        int parejas = 0;
        for (Habitante h : vecinos) {
            UUID p = f.pareja(h.getUUID());
            if (p == null) continue;
            String clave = h.getUUID().compareTo(p) < 0 ? h.getUUID() + "" + p : p + "" + h.getUUID();
            if (!vistas.add(clave)) continue;
            parejas++;
            int estado = f.estado(h.getUUID(), p);
            String otro = p.equals(j.getUUID()) ? "tú" : nombre(f, p);
            linea(l, "  " + h.nombre() + " ♥ " + otro + " (" + (estado == Familias.CASADOS ? "casados" : "novios") + ")");
        }
        if (parejas == 0) linea(l, "  Ninguna.");

        // ---- Vecinos
        titulo(l, "Vecinos");
        for (Habitante h : vecinos) vecino(l, j, f, h, ahora, nivel);

        // ---- Rumores
        titulo(l, "Rumores");
        List<Aldeas.Rumor> rumores = datos.rumores(campana, ahora);
        if (rumores.isEmpty()) linea(l, "  No corre ninguno.");
        for (Aldeas.Rumor r : rumores) {
            l.add(Component.literal("  · " + r.texto).withStyle(r.infiel != null ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.WHITE)
                    .append(Component.literal("  (lo saben " + r.cuantosLoSaben() + ")").withStyle(ChatFormatting.DARK_GRAY)));
        }
        mandar(j, l);
    }

    private static void vecino(List<Component> l, ServerPlayer j, Familias f, Habitante h, long ahora, ServerLevel nivel) {
        MutableComponent cab = Component.literal((h.esMujer() ? "♀ " : "♂ ") + h.nombre())
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                .append(Component.literal("  " + ETAPAS[Math.min(3, h.etapa())] + " · ").withStyle(ChatFormatting.GRAY))
                .append(papel(h).copy().withStyle(ChatFormatting.GRAY));
        l.add(Component.empty());
        l.add(cab);
        if (h.etapa() >= 1) {
            linea(l, "  Carácter: " + Conversaciones.RASGOS[h.rasgo1()] + " y " + Conversaciones.RASGOS[h.rasgo2()]
                    + " · " + (h.infielDeNatural() ? "poco fiel" : "fiel") + " · "
                    + (h.facilidad() >= 70 ? "lanzado/a" : h.facilidad() <= 30 ? "recatado/a" : "ni fu ni fa")
                    + " · regatea hasta un " + h.flexibilidad() + " %");
            List<Gustos.Cosa> gustan = Gustos.leEncantan(h);
            linea(l, "  Gustos: le encantan " + gustan.get(0).nombre + " y " + gustan.get(1).nombre
                    + "; no soporta " + Gustos.noSoporta(h).nombre);
        }
        if (h.etapa() >= 3) linea(l, "  Amor: " + ORIENTACION[h.orientacion()]);
        // Familia
        List<String> fam = new ArrayList<>();
        for (UUID u : f.padres(h.getUUID())) fam.add((f.persona(u).esMujer() ? "madre " : "padre ") + nombre(f, u, j));
        List<UUID> hijos = f.hijos(h.getUUID());
        if (!hijos.isEmpty()) {
            List<String> n = new ArrayList<>();
            for (UUID u : hijos) n.add(nombre(f, u, j));
            fam.add("hijos: " + String.join(", ", n));
        }
        List<UUID> hermanos = f.hermanos(h.getUUID());
        if (!hermanos.isEmpty()) {
            List<String> n = new ArrayList<>();
            for (UUID u : hermanos) n.add(nombre(f, u, j));
            fam.add("hermanos: " + String.join(", ", n));
        }
        UUID pareja = f.pareja(h.getUUID());
        if (pareja != null) {
            fam.add((f.estado(h.getUUID(), pareja) == Familias.CASADOS ? "casado/a con " : "sale con ") + nombre(f, pareja, j));
        } else if (f.difunto(h.getUUID()) != null) {
            fam.add("viudo/a de " + nombre(f, f.difunto(h.getUUID()), j));
        }
        if (!fam.isEmpty()) linea(l, "  Familia: " + String.join(" · ", fam));
        Familias.Persona p = f.personaSiExiste(h.getUUID());
        if (p != null && p.amante != null && f.vivo(p.amante)) {
            l.add(Component.literal("  Aventura secreta con " + nombre(f, p.amante, j)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (p != null && p.parto > 0) linea(l, "  Embarazada (" + Math.round(p.embarazo(ahora) * 100) + " %)");
        // Contigo
        Recuerdo r = h.recuerdoSiExiste(j.getUUID());
        if (r != null) {
            linea(l, "  Contigo: opinión " + r.opinion + (r.sabeNombre ? " · sabe tu nombre" : "")
                    + (r.enfadadoHasta > ahora ? " · enfadado/a" : "")
                    + (Amor.infidelidadesSabidas(nivel, h, j.getUUID()) > 0 ? " · sabe que eres infiel" : ""));
        } else {
            linea(l, "  Contigo: aún no os conocéis (le caerías " + h.primeraImpresion() + " de entrada)");
        }
    }

    private static Component papel(Habitante h) {
        if (h.esLider()) return Component.literal("alcalde");
        if (h.esGuardia()) return Component.literal("guardia");
        if (h.etapa() < 2) return Component.literal("sin oficio (aún)");
        if (h.sinOficio()) return Component.literal(h.esHolgazan() ? "holgazán" : "busca trabajo");
        return Component.empty().append(h.nombreOficio()).append(" (nivel " + h.nivel() + ")");
    }

    private static String nombre(Familias f, UUID u) {
        String n = f.nombre(u);
        return (n.isEmpty() ? "?" : n) + (f.vivo(u) ? "" : " †");
    }

    private static String nombre(Familias f, UUID u, ServerPlayer j) {
        return u.equals(j.getUUID()) ? "tú" : nombre(f, u);
    }

    private static void titulo(List<Component> l, String t) {
        if (!l.isEmpty()) l.add(Component.empty());
        l.add(Component.literal(t).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    private static void linea(List<Component> l, String t) {
        l.add(Component.literal(t));
    }

    private static void mandar(ServerPlayer j, List<Component> l) {
        HabitantesRed.CANAL.send(PacketDistributor.PLAYER.with(() -> j), new CronicaPacket(l));
    }
}
