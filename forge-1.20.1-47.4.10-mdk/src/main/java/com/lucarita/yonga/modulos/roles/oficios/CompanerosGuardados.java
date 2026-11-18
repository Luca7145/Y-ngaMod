package com.lucarita.yonga.modulos.roles.oficios;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Qué compañero (mob domado con la flauta) tiene cada jugador, y dónde se le vio por última vez
 * (dimensión y chunk). Se guarda con el mundo (data/yonga_companeros.dat), así funciona aunque el dueño
 * esté desconectado cuando su compañero desaparece.
 *
 * El "dónde" sirve para saber si un compañero que no encontramos sigue existiendo: si su chunk está
 * cargado y no está ahí, es que ya no existe (ver Companeros.sigueVivo).
 */
public class CompanerosGuardados extends SavedData {

    private static final String NOMBRE = "yonga_companeros";

    public record Companero(UUID mob, String dimension, int chunkX, int chunkZ, boolean conPosicion) {}

    private final Map<UUID, Companero> companeroDe = new HashMap<>();   // jugador -> compañero

    public static CompanerosGuardados de(MinecraftServer servidor) {
        return servidor.overworld().getDataStorage().computeIfAbsent(CompanerosGuardados::cargar, CompanerosGuardados::new, NOMBRE);
    }

    public Companero companero(UUID jugador) {
        return companeroDe.get(jugador);
    }

    /** Apunta (o actualiza) el compañero del jugador y dónde está ahora. */
    public void fijar(UUID jugador, Entity mob) {
        Companero nuevo = new Companero(mob.getUUID(), mob.level().dimension().location().toString(),
                mob.chunkPosition().x, mob.chunkPosition().z, true);
        if (!nuevo.equals(companeroDe.get(jugador))) {
            companeroDe.put(jugador, nuevo);
            setDirty();
        }
    }

    /** Lo quita solo si el compañero apuntado es ese mob. Devuelve true si lo ha quitado. */
    public boolean quitar(UUID jugador, UUID mob) {
        Companero actual = companeroDe.get(jugador);
        if (actual != null && actual.mob().equals(mob)) {
            companeroDe.remove(jugador);
            setDirty();
            return true;
        }
        return false;
    }

    /** Lo quita sin más (cuando sabemos que ya no existe). */
    public void quitar(UUID jugador) {
        if (companeroDe.remove(jugador) != null) {
            setDirty();
        }
    }

    private static CompanerosGuardados cargar(CompoundTag datos) {
        CompanerosGuardados guardados = new CompanerosGuardados();
        for (String jugador : datos.getAllKeys()) {
            UUID id = UUID.fromString(jugador);
            if (datos.hasUUID(jugador)) {
                // Formato antiguo (solo el mob, sin posición)
                guardados.companeroDe.put(id, new Companero(datos.getUUID(jugador), "", 0, 0, false));
            } else {
                CompoundTag c = datos.getCompound(jugador);
                guardados.companeroDe.put(id, new Companero(c.getUUID("mob"), c.getString("dimension"),
                        c.getInt("x"), c.getInt("z"), true));
            }
        }
        return guardados;
    }

    @Override
    public CompoundTag save(CompoundTag datos) {
        companeroDe.forEach((jugador, c) -> {
            CompoundTag compuesto = new CompoundTag();
            compuesto.putUUID("mob", c.mob());
            compuesto.putString("dimension", c.dimension());
            compuesto.putInt("x", c.chunkX());
            compuesto.putInt("z", c.chunkZ());
            datos.put(jugador.toString(), compuesto);
        });
        return datos;
    }
}
