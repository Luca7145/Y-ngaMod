package com.lucarita.yonga.modulos.habitantes.aldea;

import com.lucarita.yonga.modulos.habitantes.Habitante;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Lo que pasa en la aldea y se convierte en chisme: declaraciones (aceptadas o con calabazas), rupturas, peleas,
 * muertes… Lo sabe quien lo vive y, si hay alguien cerca, quien lo ve; a partir de ahí corre por la plaza.
 */
public final class Chismes {

    private Chismes() {}

    /**
     * @param protagonista el habitante al que le pasa (lo sabe él y, si hay, un testigo cercano)
     * @param jugador      el jugador implicado, si lo hay (para que el chisme "sea sobre él")
     * @param todos        si es algo sonado (una muerte): lo saben todos los que estén cerca
     */
    public static void contar(ServerLevel nivel, Habitante protagonista, String sobre, @Nullable UUID jugador,
                              String texto, boolean todos) {
        if (protagonista.aldea() == null) return;
        Aldeas datos = Aldeas.de(nivel);
        long ahora = nivel.getGameTime();
        List<Habitante> cerca = nivel.getEntitiesOfClass(Habitante.class, protagonista.getBoundingBox().inflate(todos ? 24 : 16),
                t -> t != protagonista && t.isAlive() && t.etapa() >= 1 && !t.isSleeping()
                        && protagonista.aldea().equals(t.aldea()));
        UUID primero = protagonista.isAlive() ? protagonista.getUUID()
                : cerca.isEmpty() ? null : cerca.get(0).getUUID();
        if (primero == null) return;
        Aldeas.Rumor r = datos.nuevoRumor(protagonista.aldea(), sobre, jugador, texto, primero, ahora, null, null);
        if (r == null) return;
        if (todos) {
            for (Habitante t : cerca) datos.contar(r, t.getUUID());
        } else if (!cerca.isEmpty()) {
            datos.contar(r, cerca.get(nivel.random.nextInt(cerca.size())).getUUID());
        }
    }
}
