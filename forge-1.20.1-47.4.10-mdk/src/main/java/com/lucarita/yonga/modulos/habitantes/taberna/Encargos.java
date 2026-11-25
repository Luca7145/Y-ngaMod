package com.lucarita.yonga.modulos.habitantes.taberna;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Los encargos de la taberna de cada aldea (data/yonga_encargos.dat de cada dimensión): el tablón de la taberna, los
 * encargos que tiene colgados el tabernero y quién los ha cumplido. Solo el primero que cumple un encargo se lleva
 * la recompensa.
 */
public class Encargos extends SavedData {

    /** Un encargo: traer objetos o cazar monstruos, con su recompensa. */
    public static final class Encargo {
        public int id;
        public boolean caza;
        /** Id del objeto (traer) o del tipo de monstruo (cazar). */
        public String objetivo = "";
        public int cantidad;
        public int monedas;
        public int fama;
        public int aprecio;
        public long caduca;
        @Nullable public UUID ganador;
        public String nombreGanador = "";
        public long cumplido;
        /** Cazas: quién lo ha aceptado y cuántos lleva. */
        public final Map<UUID, Integer> progreso = new LinkedHashMap<>();

        public boolean abierto() {
            return ganador == null;
        }

        CompoundTag guardar() {
            CompoundTag t = new CompoundTag();
            t.putInt("id", id);
            t.putBoolean("caza", caza);
            t.putString("objetivo", objetivo);
            t.putInt("cantidad", cantidad);
            t.putInt("monedas", monedas);
            t.putInt("fama", fama);
            t.putInt("aprecio", aprecio);
            t.putLong("caduca", caduca);
            if (ganador != null) t.putUUID("ganador", ganador);
            t.putString("nombreGanador", nombreGanador);
            t.putLong("cumplido", cumplido);
            CompoundTag p = new CompoundTag();
            progreso.forEach((u, n) -> p.putInt(u.toString(), n));
            t.put("progreso", p);
            return t;
        }

        static Encargo cargar(CompoundTag t) {
            Encargo e = new Encargo();
            e.id = t.getInt("id");
            e.caza = t.getBoolean("caza");
            e.objetivo = t.getString("objetivo");
            e.cantidad = t.getInt("cantidad");
            e.monedas = t.getInt("monedas");
            e.fama = t.getInt("fama");
            e.aprecio = t.getInt("aprecio");
            e.caduca = t.getLong("caduca");
            e.ganador = t.hasUUID("ganador") ? t.getUUID("ganador") : null;
            e.nombreGanador = t.getString("nombreGanador");
            e.cumplido = t.getLong("cumplido");
            CompoundTag p = t.getCompound("progreso");
            for (String k : p.getAllKeys()) {
                try {
                    e.progreso.put(UUID.fromString(k), p.getInt(k));
                } catch (IllegalArgumentException ignorado) {
                    // clave rara
                }
            }
            return e;
        }
    }

    /** Lo de la taberna de una aldea. */
    public static final class Datos {
        @Nullable public BlockPos tablon;
        /** El caldero de la barra (puesto del tabernero) y un sitio dentro de la taberna. */
        @Nullable public BlockPos caldero, dentro;
        public int intentos;
        public long ultimoIntento;
        public long ultimoDia = -1;
        public int siguienteId = 1;
        public final List<Encargo> encargos = new ArrayList<>();

        @Nullable
        public Encargo encargo(int id) {
            for (Encargo e : encargos) if (e.id == id) return e;
            return null;
        }
    }

    private final Map<Long, Datos> tabernas = new HashMap<>();

    public static Encargos de(ServerLevel nivel) {
        return nivel.getDataStorage().computeIfAbsent(Encargos::cargar, Encargos::new, "yonga_encargos");
    }

    public Datos taberna(BlockPos campana) {
        return tabernas.computeIfAbsent(campana.asLong(), k -> {
            setDirty();
            return new Datos();
        });
    }

    public Iterable<Map.Entry<Long, Datos>> todas() {
        return tabernas.entrySet();
    }

    public void marcar() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag lista = new ListTag();
        tabernas.forEach((clave, t) -> {
            CompoundTag c = new CompoundTag();
            c.putLong("campana", clave);
            if (t.tablon != null) c.put("tablon", NbtUtils.writeBlockPos(t.tablon));
            if (t.caldero != null) c.put("caldero", NbtUtils.writeBlockPos(t.caldero));
            if (t.dentro != null) c.put("dentro", NbtUtils.writeBlockPos(t.dentro));
            c.putInt("intentos", t.intentos);
            c.putLong("ultimoDia", t.ultimoDia);
            c.putInt("siguienteId", t.siguienteId);
            ListTag es = new ListTag();
            for (Encargo e : t.encargos) es.add(e.guardar());
            c.put("encargos", es);
            lista.add(c);
        });
        nbt.put("tabernas", lista);
        return nbt;
    }

    public static Encargos cargar(CompoundTag nbt) {
        Encargos datos = new Encargos();
        ListTag lista = nbt.getList("tabernas", Tag.TAG_COMPOUND);
        for (int i = 0; i < lista.size(); i++) {
            CompoundTag c = lista.getCompound(i);
            Datos t = new Datos();
            if (c.contains("tablon")) t.tablon = NbtUtils.readBlockPos(c.getCompound("tablon"));
            if (c.contains("caldero")) t.caldero = NbtUtils.readBlockPos(c.getCompound("caldero"));
            if (c.contains("dentro")) t.dentro = NbtUtils.readBlockPos(c.getCompound("dentro"));
            t.intentos = c.getInt("intentos");
            t.ultimoDia = c.getLong("ultimoDia");
            t.siguienteId = Math.max(1, c.getInt("siguienteId"));
            ListTag es = c.getList("encargos", Tag.TAG_COMPOUND);
            for (int j = 0; j < es.size(); j++) t.encargos.add(Encargo.cargar(es.getCompound(j)));
            datos.tabernas.put(c.getLong("campana"), t);
        }
        return datos;
    }
}
