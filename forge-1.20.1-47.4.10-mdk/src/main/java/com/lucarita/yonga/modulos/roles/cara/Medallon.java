package com.lucarita.yonga.modulos.roles.cara;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/**
 * Los medallones (amuletos) y los efectos que dan mientras se llevan puestos en el hueco de cara.
 * Para añadir uno: una línea aquí, su objeto en ModuloRoles, su receta, sus texturas
 * (item/medallon_<id>.png y entity/medallon_<id>.png) y añadirlo a la etiqueta yonga:cara.
 */
public enum Medallon {
    AGUA("agua", MobEffects.CONDUIT_POWER, MobEffects.DOLPHINS_GRACE),          // canalización + gracia del delfín
    TIERRA("tierra", MobEffects.DIG_SPEED, MobEffects.DAMAGE_RESISTANCE),       // prisa minera + resistencia
    AIRE("aire", MobEffects.NIGHT_VISION, MobEffects.MOVEMENT_SPEED),           // visión nocturna + velocidad
    FUEGO("fuego", MobEffects.DAMAGE_BOOST, MobEffects.FIRE_RESISTANCE);        // fuerza + resistencia al fuego

    private final String id;
    private final List<MobEffect> efectos;

    Medallon(String id, MobEffect... efectos) {
        this.id = id;
        this.efectos = List.of(efectos);
    }

    public String id() {
        return id;
    }

    public List<MobEffect> efectos() {
        return efectos;
    }
}
