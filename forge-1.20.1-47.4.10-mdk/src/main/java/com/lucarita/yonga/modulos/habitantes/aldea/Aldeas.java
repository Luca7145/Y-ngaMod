package com.lucarita.yonga.modulos.habitantes.aldea;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Las aldeas de un mundo (una dimensión). Una aldea es la campana de su plaza: cada habitante pertenece a la campana
 * más cercana (a menos de 112 bloques). De cada aldea se guarda:
 *   · su líder (el alcalde o la alcaldesa: el adulto más veterano; si muere, se elige otro);
 *   · la FAMA de cada jugador allí (−100..100): sube ayudando (matar monstruos cerca, comerciar) y baja haciendo daño
 *     a los vecinos. Los vecinos que no le conocen empiezan opinando de él la mitad de su fama, y los guardias atacan a
 *     quien tenga −50 o menos.
 *   · los RUMORES: lo que un forastero le cuenta a un vecino de otra persona (un vecino, otro jugador o él mismo).
 *     Quien lo oye lo va contando en la plaza a los demás, y cada vecino sabe solo los que le han llegado. Duran una
 *     semana. Si un rumor dice que un jugador quiere algo a toda costa, quien lo vende y lo sabe se lo encarece.
 *   · el nombre que cada jugador ha dicho tener (para saber de quién hablan los rumores).
 * Se guarda en el mundo (data/yonga_aldeas.dat).
 */
public class Aldeas extends SavedData {

    public static final int RADIO = 112;
    public static final int FAMA_ENEMIGO = -50;

    public static final class Aldea {
        public final BlockPos campana;
        @Nullable public UUID lider;
        final Map<UUID, Integer> fama = new HashMap<>();
        final Map<UUID, String> nombres = new HashMap<>();
        /** Cuántas infidelidades se le conocen a cada jugador en la aldea (fama de infiel). */
        final Map<UUID, Integer> infieles = new HashMap<>();

        public int infidelidades(UUID jugador) {
            return infieles.getOrDefault(jugador, 0);
        }
        final List<Rumor> rumores = new ArrayList<>();

        Aldea(BlockPos campana) {
            this.campana = campana;
        }

        /** El nombre que el jugador ha dicho en la aldea que tiene, o null. */
        @Nullable
        public String nombre(UUID jugador) {
            return nombres.get(jugador);
        }

        /** ¿Habla ese rumor del jugador? (por su UUID, su nombre de usuario o el nombre que ha dicho tener) */
        public boolean esSobre(Rumor r, net.minecraft.world.entity.player.Player jugador) {
            return esSobre(r, jugador, null);
        }

        /**
         * ¿Habla ese rumor del jugador, para este vecino? Sí si el rumor es del propio jugador, si el nombre coincide
         * con su nombre de usuario o con alguno de los nombres con los que se ha presentado que ESTE vecino sabe
         * (null: cualquiera de la aldea). Sin fijarse en mayúsculas ni tildes, y basta con el primer nombre.
         */
        public boolean esSobre(Rumor r, net.minecraft.world.entity.player.Player jugador, @Nullable UUID vecino) {
            if (jugador.getUUID().equals(r.jugador)) return true;
            if (normal(r.sobre).isEmpty()) return false;
            if (coincide(r.sobre, jugador.getGameProfile().getName())) return true;
            for (Rumor n : rumores) {
                if (n.alias != null && jugador.getUUID().equals(n.jugador) && (vecino == null || n.loSabe(vecino))
                        && coincide(n.alias, r.sobre)) return true;
            }
            return false;
        }

        /** Los nombres con los que se ha presentado el jugador que sabe este vecino (null: toda la aldea). */
        public List<String> alias(UUID jugador, @Nullable UUID vecino) {
            List<String> lista = new ArrayList<>();
            for (Rumor n : rumores) {
                if (n.alias != null && jugador.equals(n.jugador) && (vecino == null || n.loSabe(vecino))) {
                    boolean repetido = false;
                    for (String x : lista) if (normal(x).equals(normal(n.alias))) repetido = true;
                    if (!repetido) lista.add(n.alias);
                }
            }
            return lista;
        }

        public int fama(UUID jugador) {
            return fama.getOrDefault(jugador, 0);
        }
    }

    /** Para comparar nombres: sin tildes, en minúsculas y con los espacios justos. */
    public static String normal(String s) {
        return java.text.Normalizer.normalize(s == null ? "" : s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT).strip().replaceAll("\\s+", " ");
    }

    /** ¿Es el mismo nombre? Igual (sin tildes ni mayúsculas) o, al menos, el mismo primer nombre. */
    public static boolean coincide(String a, String b) {
        String na = normal(a), nb = normal(b);
        if (na.isEmpty() || nb.isEmpty()) return false;
        if (na.equals(nb)) return true;
        String pa = na.split(" ")[0], pb = nb.split(" ")[0];
        return pa.length() >= 3 && pa.equals(pb);
    }

    /** Un chisme: de quién habla (nombre, y el jugador si es el propio forastero), qué dice y quién lo sabe. */
    public static final class Rumor {
        public final String sobre;
        @Nullable public final UUID jugador;
        public final String texto;
        public final long cuando;
        /** Si es un chisme de cuernos: con quién anda el jugador (para que se entere su pareja). */
        @Nullable public UUID amante;
        /** Y quién engaña a su pareja (un jugador o un habitante). */
        @Nullable public UUID infiel;
        /** Si es "el forastero dice llamarse X": ese nombre (no caduca: un nombre no se olvida). */
        @Nullable public String alias;

        public boolean vigente(long ahora) {
            return alias != null || ahora - cuando <= DURA_RUMOR;
        }
        final Set<UUID> conocen = new HashSet<>();

        Rumor(String sobre, @Nullable UUID jugador, String texto, long cuando) {
            this.sobre = sobre;
            this.jugador = jugador;
            this.texto = texto;
            this.cuando = cuando;
        }

        public boolean loSabe(UUID habitante) {
            return conocen.contains(habitante);
        }

        public int cuantosLoSaben() {
            return conocen.size();
        }
    }

    public static final long DURA_RUMOR = 7 * 24000L;
    private static final int MAX_RUMORES = 30;

    private final Map<Long, Aldea> aldeas = new HashMap<>();

    public static Aldeas de(ServerLevel nivel) {
        return nivel.getDataStorage().computeIfAbsent(Aldeas::cargar, Aldeas::new, "yonga_aldeas");
    }

    /** La campana de aldea más cercana a una posición (a menos de RADIO bloques), o null. */
    @Nullable
    public static BlockPos campanaCerca(ServerLevel nivel, BlockPos pos) {
        Optional<BlockPos> campana = nivel.getPoiManager().findClosest(
                (Holder<PoiType> tipo) -> tipo.is(PoiTypes.MEETING), pos, RADIO, PoiManager.Occupancy.ANY);
        return campana.orElse(null);
    }

    public Aldea aldea(BlockPos campana) {
        return aldeas.computeIfAbsent(campana.asLong(), k -> {
            setDirty();
            return new Aldea(campana.immutable());
        });
    }

    @Nullable
    public Aldea aldeaSiExiste(BlockPos campana) {
        return aldeas.get(campana.asLong());
    }

    public void cambiarFama(BlockPos campana, UUID jugador, int cambio) {
        Aldea a = aldea(campana);
        a.fama.put(jugador, Math.max(-100, Math.min(100, a.fama(jugador) + cambio)));
        setDirty();
    }

    /**
     * El jugador le ha dicho a un vecino cómo se llama: lo sabe ese vecino y se irá contando por la plaza. Puede
     * presentarse con varios nombres (cada uno es un chisme distinto).
     */
    public void ponerNombre(BlockPos campana, UUID jugador, String nombre, UUID vecino, long ahora) {
        String n = nombre.strip();
        if (n.isEmpty() || n.length() > 32) return;
        aldea(campana).nombres.put(jugador, n);
        Rumor r = nuevoRumor(campana, n, jugador, "El forastero dice llamarse " + n, vecino, ahora, null, null);
        if (r != null) r.alias = n;
        setDirty();
    }

    /** Un rumor nuevo que acaba de oír un vecino (si no se sabía ya algo igual). */
    public void nuevoRumor(BlockPos campana, String sobre, @Nullable UUID jugador, String texto, UUID quien, long ahora) {
        nuevoRumor(campana, sobre, jugador, texto, quien, ahora, null);
    }

    /** @param amante si es un chisme de cuernos, con quién anda el jugador (que es quien engaña) */
    @Nullable
    public Rumor nuevoRumor(BlockPos campana, String sobre, @Nullable UUID jugador, String texto, UUID quien, long ahora,
                           @Nullable UUID amante) {
        return nuevoRumor(campana, sobre, jugador, texto, quien, ahora, amante, amante == null ? null : jugador);
    }

    public void sumarInfidelidad(BlockPos campana, UUID jugador) {
        Aldea a = aldea(campana);
        a.infieles.merge(jugador, 1, Integer::sum);
        setDirty();
    }

    /** @param infiel quien engaña a su pareja; @param amante con quién */
    @Nullable
    public Rumor nuevoRumor(BlockPos campana, String sobre, @Nullable UUID jugador, String texto, UUID quien, long ahora,
                           @Nullable UUID amante, @Nullable UUID infiel) {
        String t = texto.strip();
        if (t.isEmpty()) return null;
        if (t.length() > 200) t = t.substring(0, 200);
        Aldea a = aldea(campana);
        a.rumores.removeIf(r -> !r.vigente(ahora));
        String clave = t.toLowerCase(java.util.Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "");
        for (Rumor r : a.rumores) {
            if (r.texto.toLowerCase(java.util.Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "").equals(clave)) {
                r.conocen.add(quien);
                setDirty();
                return r;
            }
        }
        Rumor r = new Rumor(sobre.strip(), jugador, t, ahora);
        r.amante = amante;
        r.infiel = infiel;
        r.conocen.add(quien);
        a.rumores.add(r);
        while (a.rumores.stream().filter(x -> x.alias == null).count() > MAX_RUMORES) {
            for (int i = 0; i < a.rumores.size(); i++) {
                if (a.rumores.get(i).alias == null) {
                    a.rumores.remove(i);
                    break;
                }
            }
        }
        setDirty();
        return r;
    }

    /** Todos los rumores vivos de la aldea, del más nuevo al más viejo. */
    public List<Rumor> rumores(BlockPos campana, long ahora) {
        Aldea a = aldeas.get(campana.asLong());
        List<Rumor> lista = new ArrayList<>();
        if (a == null) return lista;
        for (int i = a.rumores.size() - 1; i >= 0; i--) {
            if (a.rumores.get(i).vigente(ahora)) lista.add(a.rumores.get(i));
        }
        return lista;
    }

    /** Los rumores que sabe un vecino, del más nuevo al más viejo. */
    public List<Rumor> rumoresDe(BlockPos campana, UUID habitante, long ahora) {
        Aldea a = aldeas.get(campana.asLong());
        List<Rumor> lista = new ArrayList<>();
        if (a == null) return lista;
        for (int i = a.rumores.size() - 1; i >= 0; i--) {
            Rumor r = a.rumores.get(i);
            if (r.vigente(ahora) && r.loSabe(habitante)) lista.add(r);
        }
        return lista;
    }

    /** Un rumor que sabe uno y el otro no (para contárselo), o null. */
    @Nullable
    public Rumor paraContar(BlockPos campana, UUID de, UUID a, long ahora) {
        for (Rumor r : rumoresDe(campana, de, ahora)) {
            if (!r.loSabe(a)) return r;
        }
        return null;
    }

    public void contar(Rumor r, UUID a) {
        if (r.conocen.add(a)) setDirty();
    }

    public void ponerLider(BlockPos campana, @Nullable UUID lider) {
        aldea(campana).lider = lider;
        setDirty();
    }

    // ---------- guardar ----------

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag lista = new ListTag();
        for (Aldea a : aldeas.values()) {
            CompoundTag t = new CompoundTag();
            t.putLong("campana", a.campana.asLong());
            if (a.lider != null) t.putUUID("lider", a.lider);
            CompoundTag fama = new CompoundTag();
            a.fama.forEach((u, f) -> fama.putInt(u.toString(), f));
            t.put("fama", fama);
            CompoundTag nombres = new CompoundTag();
            a.nombres.forEach((u, n) -> nombres.putString(u.toString(), n));
            t.put("nombres", nombres);
            CompoundTag infieles = new CompoundTag();
            a.infieles.forEach((u, n) -> infieles.putInt(u.toString(), n));
            t.put("infieles", infieles);
            ListTag rumores = new ListTag();
            for (Rumor r : a.rumores) {
                CompoundTag rt = new CompoundTag();
                rt.putString("sobre", r.sobre);
                if (r.jugador != null) rt.putUUID("jugador", r.jugador);
                rt.putString("texto", r.texto);
                rt.putLong("cuando", r.cuando);
                if (r.amante != null) rt.putUUID("amante", r.amante);
                if (r.infiel != null) rt.putUUID("infiel", r.infiel);
                if (r.alias != null) rt.putString("alias", r.alias);
                ListTag conocen = new ListTag();
                for (UUID u : r.conocen) conocen.add(net.minecraft.nbt.NbtUtils.createUUID(u));
                rt.put("conocen", conocen);
                rumores.add(rt);
            }
            t.put("rumores", rumores);
            lista.add(t);
        }
        nbt.put("aldeas", lista);
        return nbt;
    }

    public static Aldeas cargar(CompoundTag nbt) {
        Aldeas datos = new Aldeas();
        ListTag lista = nbt.getList("aldeas", Tag.TAG_COMPOUND);
        for (int i = 0; i < lista.size(); i++) {
            CompoundTag t = lista.getCompound(i);
            Aldea a = new Aldea(BlockPos.of(t.getLong("campana")));
            if (t.hasUUID("lider")) a.lider = t.getUUID("lider");
            CompoundTag fama = t.getCompound("fama");
            for (String clave : fama.getAllKeys()) {
                try {
                    a.fama.put(UUID.fromString(clave), fama.getInt(clave));
                } catch (IllegalArgumentException ignorado) {
                    // clave rara
                }
            }
            CompoundTag nombres = t.getCompound("nombres");
            for (String clave : nombres.getAllKeys()) {
                try {
                    a.nombres.put(UUID.fromString(clave), nombres.getString(clave));
                } catch (IllegalArgumentException ignorado) {
                    // clave rara
                }
            }
            CompoundTag infieles = t.getCompound("infieles");
            for (String clave : infieles.getAllKeys()) {
                try {
                    a.infieles.put(UUID.fromString(clave), infieles.getInt(clave));
                } catch (IllegalArgumentException ignorado) {
                    // clave rara
                }
            }
            ListTag rumores = t.getList("rumores", Tag.TAG_COMPOUND);
            for (int j = 0; j < rumores.size(); j++) {
                CompoundTag rt = rumores.getCompound(j);
                Rumor r = new Rumor(rt.getString("sobre"), rt.hasUUID("jugador") ? rt.getUUID("jugador") : null,
                        rt.getString("texto"), rt.getLong("cuando"));
                if (rt.hasUUID("amante")) r.amante = rt.getUUID("amante");
                if (rt.contains("alias")) r.alias = rt.getString("alias");
                if (rt.hasUUID("infiel")) r.infiel = rt.getUUID("infiel");
                else if (r.amante != null) r.infiel = r.jugador;
                ListTag conocen = rt.getList("conocen", Tag.TAG_INT_ARRAY);
                for (int k = 0; k < conocen.size(); k++) r.conocen.add(net.minecraft.nbt.NbtUtils.loadUUID(conocen.get(k)));
                a.rumores.add(r);
            }
            datos.aldeas.put(a.campana.asLong(), a);
        }
        return datos;
    }
}
