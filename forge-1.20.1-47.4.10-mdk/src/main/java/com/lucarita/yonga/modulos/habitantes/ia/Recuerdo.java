package com.lucarita.yonga.modulos.habitantes.ia;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Lo que un habitante recuerda de UN jugador: qué opina de él (−100 odio … 100 aprecio), si le ha dicho su nombre,
 * si está enfadado con él, las últimas frases que se han dicho (para que la IA siga el hilo) y sus NOTAS: lo
 * importante que sabe de él, resumido por la propia IA en cada respuesta, que dura aunque se olviden las frases.
 */
public class Recuerdo {

    /** Largo de las notas: más para la gente con la que tiene confianza (y, en la fase 5, su familia). */
    public static final int MAX_NOTAS = 1500, MAX_NOTAS_CERCANOS = 3000;

    public int opinion;
    public boolean sabeNombre;
    /** gameTime hasta el que no quiere hablar con él. */
    public long enfadadoHasta;
    /** Día (dayTime / 24000) del último regalo: uno al día como mucho. */
    public long diaRegalo = -1;
    public String notas = "";
    /** Regateos abusivos de hoy (no se guarda): cuantos más, más fácil que se ofenda. */
    public int abusos;
    /** Se ha enterado de que le engañas: con quién (se lo echará en cara la próxima vez que habléis). Se guarda. */
    public String reproche = "";
    /** Regalos que le ha hecho hoy (no se guarda): cuantos más, menos ilusión hace cada uno. */
    public int regalosHoy;
    public long diaRegalos = -1;
    public long diaAbusos = -1;
    /** Frases: "j:" del jugador, "h:" del habitante. */
    public final Deque<String> frases = new ArrayDeque<>();

    public void anotar(boolean delJugador, String texto) {
        frases.addLast((delJugador ? "j:" : "h:") + texto);
        int maximo = maxFrases();
        while (frases.size() > maximo) {
            frases.removeFirst();
        }
    }

    /** Frases que recuerda al pie de la letra: el doble con quien aprecia mucho (opinión ≥ 60). */
    public int maxFrases() {
        int base = com.lucarita.yonga.modulos.habitantes.HabitantesConfig.IA_FRASES.get();
        return opinion >= 60 ? base * 2 : base;
    }

    public int maxNotas() {
        return opinion >= 60 ? MAX_NOTAS_CERCANOS : MAX_NOTAS;
    }

    public void cambiarOpinion(int cambio) {
        opinion = Math.max(-100, Math.min(100, opinion + cambio));
    }

    public CompoundTag guardar() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("opinion", opinion);
        nbt.putBoolean("sabeNombre", sabeNombre);
        nbt.putLong("enfadadoHasta", enfadadoHasta);
        nbt.putLong("diaRegalo", diaRegalo);
        nbt.putString("notas", notas);
        if (!reproche.isEmpty()) nbt.putString("reproche", reproche);
        ListTag lista = new ListTag();
        for (String f : frases) lista.add(StringTag.valueOf(f));
        nbt.put("frases", lista);
        return nbt;
    }

    public static Recuerdo cargar(CompoundTag nbt) {
        Recuerdo r = new Recuerdo();
        r.opinion = nbt.getInt("opinion");
        r.sabeNombre = nbt.getBoolean("sabeNombre");
        r.enfadadoHasta = nbt.getLong("enfadadoHasta");
        r.diaRegalo = nbt.contains("diaRegalo") ? nbt.getLong("diaRegalo") : -1;
        r.notas = nbt.getString("notas");
        r.reproche = nbt.getString("reproche");
        ListTag lista = nbt.getList("frases", Tag.TAG_STRING);
        for (int i = 0; i < lista.size(); i++) r.frases.addLast(lista.getString(i));
        return r;
    }
}
