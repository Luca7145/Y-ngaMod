package com.lucarita.yonga.modulos.habitantes.familia;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * El árbol familiar de todo el mundo (habitantes y jugadores), guardado en data/yonga_familias.dat del Overworld:
 *   · quiénes son los padres de cada uno (de sangre o adoptivos) → con eso se saben hermanos, abuelos, tíos, primos;
 *   · las parejas: novios (1) o casados (2), desde cuándo; un habitante tiene como mucho una, un jugador puede liarse
 *     con varios… y pagarlo (ver Amor: celos);
 *   · los embarazos: cuándo nace el bebé y quién es el otro progenitor;
 *   · el nombre y el sexo de cada uno (para hablar de ellos aunque no estén cargados) y si sigue vivo;
 *   · la afinidad entre habitantes solteros que charlan a menudo (de ahí salen las parejas de la aldea).
 * Nunca hay romance entre parientes: ver parientes().
 */
public class Familias extends SavedData {

    public static final int NOVIOS = 1, CASADOS = 2;
    /** Sexo de un jugador: aún sin decir, hombre o mujer (/yonga sexo). */
    public static final int SIN_DECIR = 0, HOMBRE = 1, MUJER = 2;

    public static final class Relacion {
        public int estado;
        public long desde;

        Relacion(int estado, long desde) {
            this.estado = estado;
            this.desde = desde;
        }
    }

    public static final class Persona {
        public boolean jugador;
        public String nombre = "";
        /** Habitantes: su sexo. Jugadores: ver sexo. */
        public boolean mujer;
        public int sexo = SIN_DECIR;
        public boolean vivo = true;
        public final List<UUID> padres = new ArrayList<>();
        public final Map<UUID, Relacion> parejas = new LinkedHashMap<>();
        /** gameTime en que nace su bebé (0 = no espera ninguno) y el otro progenitor. */
        public long parto;
        /** gameTime en que se quedó embarazada (para saber lo avanzado que va). */
        public long concepcion;
        @Nullable public UUID otroProgenitor;
        /** Rumores del embarazo ya soltados: 1 = ella lo sabe, 2 = ya se le nota. */
        public int rumorEmbarazo;
        /** El último rumor de cuernos que ya ha visto (gameTime), para no enfadarse dos veces por lo mismo. */
        public long celosVistos;
        /** Con quién tiene una aventura a escondidas (si la tiene). */
        @Nullable public UUID amante;
        /** gameTime de su última noche con alguien (para el tiempo de espera). No se guarda. */
        public long ultimaNoche = -1_000_000L;

        /** Lo avanzado del embarazo, de 0 (nada) a 1 (a punto de nacer). */
        public float embarazo(long ahora) {
            if (parto <= 0 || parto <= concepcion) return 0.0f;
            return Math.max(0.0f, Math.min(1.0f, (float) (ahora - concepcion) / (float) (parto - concepcion)));
        }

        public boolean esMujer() {
            return jugador ? sexo == MUJER : mujer;
        }

        public boolean sexoConocido() {
            return !jugador || sexo != SIN_DECIR;
        }
    }

    private final Map<UUID, Persona> personas = new HashMap<>();
    private final Map<String, Integer> afinidad = new HashMap<>();

    public static Familias de(MinecraftServer servidor) {
        return servidor.overworld().getDataStorage().computeIfAbsent(Familias::cargar, Familias::new, "yonga_familias");
    }

    public Persona persona(UUID id) {
        return personas.computeIfAbsent(id, k -> {
            setDirty();
            return new Persona();
        });
    }

    @Nullable
    public Persona personaSiExiste(UUID id) {
        return personas.get(id);
    }

    public void apuntar(UUID id, String nombre, boolean mujer, boolean jugador) {
        Persona p = persona(id);
        if (!nombre.equals(p.nombre) || p.mujer != mujer || p.jugador != jugador || !p.vivo) {
            p.nombre = nombre;
            p.mujer = mujer;
            p.jugador = jugador;
            p.vivo = true;
            setDirty();
        }
    }

    public void ponerSexo(UUID jugador, int sexo) {
        persona(jugador).sexo = sexo;
        setDirty();
    }

    public void morir(UUID id) {
        Persona p = personas.get(id);
        if (p != null && p.vivo) {
            p.vivo = false;
            setDirty();
        }
    }

    public boolean vivo(UUID id) {
        Persona p = personas.get(id);
        return p == null || p.vivo;
    }

    public String nombre(UUID id) {
        Persona p = personas.get(id);
        return p == null ? "" : p.nombre;
    }

    // ---------- parentesco ----------

    public List<UUID> padres(UUID id) {
        Persona p = personas.get(id);
        return p == null ? List.of() : p.padres;
    }

    public void ponerPadres(UUID hijo, List<UUID> padres) {
        Persona p = persona(hijo);
        p.padres.clear();
        for (UUID u : padres) if (u != null && !p.padres.contains(u) && p.padres.size() < 2) p.padres.add(u);
        setDirty();
    }

    public List<UUID> hijos(UUID id) {
        List<UUID> lista = new ArrayList<>();
        personas.forEach((u, p) -> {
            if (p.padres.contains(id)) lista.add(u);
        });
        return lista;
    }

    public List<UUID> hermanos(UUID id) {
        Set<UUID> lista = new HashSet<>();
        for (UUID padre : padres(id)) lista.addAll(hijos(padre));
        lista.remove(id);
        return new ArrayList<>(lista);
    }

    /** Él y sus antepasados hasta los abuelos. */
    private Set<UUID> raiz(UUID id) {
        Set<UUID> s = new HashSet<>();
        s.add(id);
        for (UUID padre : padres(id)) {
            s.add(padre);
            s.addAll(padres(padre));
        }
        return s;
    }

    /**
     * ¿Son familia? Padres e hijos, abuelos y nietos, hermanos, tíos y sobrinos, primos: comparten alguien entre
     * ellos mismos y sus padres y abuelos.
     */
    public boolean parientes(UUID a, UUID b) {
        if (a.equals(b)) return true;
        Set<UUID> ra = raiz(a);
        for (UUID u : raiz(b)) if (ra.contains(u)) return true;
        return false;
    }

    // ---------- parejas ----------

    /** Estado de la pareja a-b: 0 nada, NOVIOS o CASADOS. */
    public int estado(UUID a, UUID b) {
        Persona p = personas.get(a);
        if (p == null) return 0;
        Relacion r = p.parejas.get(b);
        return r == null ? 0 : r.estado;
    }

    @Nullable
    public Relacion relacion(UUID a, UUID b) {
        Persona p = personas.get(a);
        return p == null ? null : p.parejas.get(b);
    }

    /** Las parejas vivas de alguien (un habitante, como mucho una). */
    public List<UUID> parejas(UUID id) {
        Persona p = personas.get(id);
        List<UUID> lista = new ArrayList<>();
        if (p == null) return lista;
        for (UUID u : p.parejas.keySet()) if (vivo(u)) lista.add(u);
        return lista;
    }

    @Nullable
    public UUID pareja(UUID id) {
        List<UUID> lista = parejas(id);
        return lista.isEmpty() ? null : lista.get(0);
    }

    /** La pareja que se le murió (si la hubo y no tiene otra). */
    @Nullable
    public UUID difunto(UUID id) {
        Persona p = personas.get(id);
        if (p == null) return null;
        for (var e : p.parejas.entrySet()) if (!vivo(e.getKey()) && e.getValue().estado == CASADOS) return e.getKey();
        return null;
    }

    public void emparejar(UUID a, UUID b, int estado, long ahora) {
        persona(a).parejas.put(b, new Relacion(estado, ahora));
        persona(b).parejas.put(a, new Relacion(estado, ahora));
        // Un viudo que vuelve a emparejarse deja atrás al difunto
        persona(a).parejas.keySet().removeIf(u -> !u.equals(b) && !vivo(u));
        persona(b).parejas.keySet().removeIf(u -> !u.equals(a) && !vivo(u));
        setDirty();
    }

    public void romper(UUID a, UUID b) {
        Persona pa = personas.get(a), pb = personas.get(b);
        if (pa != null) pa.parejas.remove(b);
        if (pb != null) pb.parejas.remove(a);
        setDirty();
    }

    // ---------- afinidad entre habitantes ----------

    private static String clave(UUID a, UUID b) {
        return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
    }

    public int sumarAfinidad(UUID a, UUID b, int cuanto) {
        int v = afinidad.getOrDefault(clave(a, b), 0) + cuanto;
        afinidad.put(clave(a, b), v);
        if (afinidad.size() > 4000) afinidad.clear();          // por si acaso: es solo para empezar parejas
        setDirty();
        return v;
    }

    public void olvidarAfinidad(UUID a, UUID b) {
        if (afinidad.remove(clave(a, b)) != null) setDirty();
    }

    public void marcar() {
        setDirty();
    }

    // ---------- guardar ----------

    @Override
    public CompoundTag save(CompoundTag nbt) {
        CompoundTag todas = new CompoundTag();
        personas.forEach((u, p) -> {
            CompoundTag t = new CompoundTag();
            t.putBoolean("jugador", p.jugador);
            t.putString("nombre", p.nombre);
            t.putBoolean("mujer", p.mujer);
            t.putInt("sexo", p.sexo);
            t.putBoolean("vivo", p.vivo);
            ListTag padres = new ListTag();
            for (UUID x : p.padres) padres.add(NbtUtils.createUUID(x));
            t.put("padres", padres);
            CompoundTag parejas = new CompoundTag();
            p.parejas.forEach((x, r) -> {
                CompoundTag rt = new CompoundTag();
                rt.putInt("estado", r.estado);
                rt.putLong("desde", r.desde);
                parejas.put(x.toString(), rt);
            });
            t.put("parejas", parejas);
            t.putLong("parto", p.parto);
            t.putInt("rumor_embarazo", p.rumorEmbarazo);
            t.putLong("concepcion", p.concepcion);
            if (p.otroProgenitor != null) t.putUUID("otro", p.otroProgenitor);
            t.putLong("celos", p.celosVistos);
            if (p.amante != null) t.putUUID("amante", p.amante);
            todas.put(u.toString(), t);
        });
        nbt.put("personas", todas);
        CompoundTag af = new CompoundTag();
        afinidad.forEach(af::putInt);
        nbt.put("afinidad", af);
        return nbt;
    }

    public static Familias cargar(CompoundTag nbt) {
        Familias f = new Familias();
        CompoundTag todas = nbt.getCompound("personas");
        for (String clave : todas.getAllKeys()) {
            try {
                CompoundTag t = todas.getCompound(clave);
                Persona p = new Persona();
                p.jugador = t.getBoolean("jugador");
                p.nombre = t.getString("nombre");
                p.mujer = t.getBoolean("mujer");
                p.sexo = t.getInt("sexo");
                p.vivo = !t.contains("vivo") || t.getBoolean("vivo");
                ListTag padres = t.getList("padres", Tag.TAG_INT_ARRAY);
                for (int i = 0; i < padres.size(); i++) p.padres.add(NbtUtils.loadUUID(padres.get(i)));
                CompoundTag parejas = t.getCompound("parejas");
                for (String x : parejas.getAllKeys()) {
                    CompoundTag rt = parejas.getCompound(x);
                    p.parejas.put(UUID.fromString(x), new Relacion(rt.getInt("estado"), rt.getLong("desde")));
                }
                p.parto = t.getLong("parto");
                p.rumorEmbarazo = t.getInt("rumor_embarazo");
                p.concepcion = t.getLong("concepcion");
                p.otroProgenitor = t.hasUUID("otro") ? t.getUUID("otro") : null;
                p.celosVistos = t.getLong("celos");
                p.amante = t.hasUUID("amante") ? t.getUUID("amante") : null;
                f.personas.put(UUID.fromString(clave), p);
            } catch (IllegalArgumentException ignorado) {
                // clave rara
            }
        }
        CompoundTag af = nbt.getCompound("afinidad");
        for (String k : af.getAllKeys()) f.afinidad.put(k, af.getInt(k));
        return f;
    }
}
