package com.lucarita.yonga.modulos.habitantes;

import com.lucarita.yonga.modulos.habitantes.familia.Familias;
import com.lucarita.yonga.modulos.habitantes.ia.Conversaciones;
import com.lucarita.yonga.modulos.habitantes.ia.Recuerdo;
import com.lucarita.yonga.modulos.habitantes.red.ConversacionPacket;
import com.lucarita.yonga.modulos.habitantes.red.HabitantesRed;
import com.lucarita.yonga.modulos.habitantes.red.RelacionPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * Botones "Sígueme" / "Deja de seguirme" y "Quédate aquí" / "Ya puedes irte" de la conversación. Tu pareja (o tus
 * hijos) te hacen caso casi siempre; los demás, según lo que te aprecien: con poca confianza casi nunca, con mucha
 * casi siempre. Dejar de seguirte o volver a lo suyo lo hacen siempre.
 */
public final class Ordenes {

    private Ordenes() {}

    /** Para los botones: 0 a lo suyo · 1 te sigue · 2 quieto. */
    public static int estado(ServerPlayer j, Habitante h) {
        if (h.sigueA(j)) return 1;
        return h.quieto() != null ? 2 : 0;
    }

    /** Probabilidad de que te haga caso. */
    private static float obedece(ServerPlayer j, Habitante h, Recuerdo r) {
        if (h.etapa() == 0) return 1.0f;                 // un bebé va con quien se lo lleve
        if (h.level() instanceof ServerLevel nivel) {
            Familias f = Familias.de(nivel.getServer());
            if (f.estado(h.getUUID(), j.getUUID()) > 0) return 0.95f;
            if (f.padres(h.getUUID()).contains(j.getUUID())) return 0.9f;
        }
        float p = (r.opinion - 10) / 60.0f;
        if (h.tieneRasgo(Habitante.DESCONFIADO) || h.tieneRasgo(Habitante.ORGULLOSO)) p -= 0.15f;
        return Math.max(0.0f, Math.min(1.0f, p));
    }

    public static void orden(ServerPlayer j, Habitante h, int tipo) {
        Recuerdo r = h.recuerdo(j);
        String accion, clave;
        switch (tipo) {
            case ConversacionPacket.SEGUIR -> {
                accion = "*Le pides que te siga*";
                boolean si = h.getRandom().nextFloat() < obedece(j, h, r);
                if (si) h.seguir(j);
                clave = si ? "sigue" : "no_sigue";
            }
            case ConversacionPacket.DEJAR -> {
                accion = "*Le dices que ya no hace falta que te siga*";
                h.dejarDeSeguir();
                clave = "deja";
            }
            case ConversacionPacket.QUEDARSE -> {
                accion = "*Le pides que se quede aquí*";
                boolean si = h.getRandom().nextFloat() < obedece(j, h, r);
                if (si) h.quedarseQuieto(h.blockPosition());
                clave = si ? "quieto" : "no_quieto";
            }
            default -> {
                accion = "*Le dices que ya puede irse*";
                h.moverseLibre();
                clave = "libre";
            }
        }
        Conversaciones.reaccion(j, h, accion, h.etapa() == 0 ? Conversaciones.fraseBebe(h)
                : Component.translatable("yonga.orden." + clave + "." + h.getRandom().nextInt(2)));
        HabitantesRed.CANAL.send(PacketDistributor.PLAYER.with(() -> j), new RelacionPacket(h.getId(),
                com.lucarita.yonga.modulos.habitantes.familia.Amor.relacionParaBotones(j, h), estado(j, h)));
    }
}
