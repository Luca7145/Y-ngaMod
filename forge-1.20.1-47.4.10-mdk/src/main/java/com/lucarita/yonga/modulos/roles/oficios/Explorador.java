package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.modulos.roles.RangoRol;
import com.lucarita.yonga.modulos.roles.Rol;
import com.lucarita.yonga.modulos.roles.RolDatos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.EnumMap;
import java.util.Map;

/**
 * Habilidades del oficio Explorador (sus objetos —parapente, mochilas, antifaz, medallones— van en sus recetas).
 *
 *  - Caída de plumas: recibe menos daño de caída, como con el encantamiento. Desde Aprendiz.
 *    Cada nivel quita un 12% del daño de caída (igual que el encantamiento de Minecraft), y se suma
 *    a lo que ya quiten las botas encantadas.
 */
public final class Explorador {

    // Nivel de "Caída de plumas" según el rango
    private static final Map<RangoRol, Integer> NIVEL_CAIDA_DE_PLUMAS = new EnumMap<>(Map.of(
            RangoRol.NOVATO, 0,
            RangoRol.APRENDIZ, 1,
            RangoRol.OFICIAL, 2,
            RangoRol.EXPERTO, 3,
            RangoRol.MAESTRO, 4
    ));
    private static final float REDUCCION_POR_NIVEL = 0.12f;

    private Explorador() {}

    public static int nivelCaidaDePlumas(Rol rol, RangoRol rango) {
        return rol == Rol.EXPLORADOR ? NIVEL_CAIDA_DE_PLUMAS.getOrDefault(rango, 0) : 0;
    }

    // ---------- EVENTOS (se registran en ModuloRoles) ----------

    public static void alRecibirDano(LivingHurtEvent evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jugador) || !evento.getSource().is(DamageTypeTags.IS_FALL)) {
            return;
        }
        int nivel = nivelCaidaDePlumas(RolDatos.obtenerRol(jugador), RolDatos.obtenerRango(jugador));
        if (nivel > 0) {
            evento.setAmount(evento.getAmount() * (1.0f - REDUCCION_POR_NIVEL * nivel));
        }
    }
}
