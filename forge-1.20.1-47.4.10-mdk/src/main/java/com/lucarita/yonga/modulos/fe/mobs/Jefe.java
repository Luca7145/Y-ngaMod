package com.lucarita.yonga.modulos.fe.mobs;

import com.lucarita.yonga.modulos.armas.LanzaEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Marca a los jefes (Pez del viento, Zor el quemado, el Rey de Arenisca...). Atacan a cualquiera, y al morir no dan
 * ni quitan fe (sería muchísima): su premio es la experiencia.
 *
 * Todos los jefes REPELEN LAS FLECHAS (excepto las ilusiones del Pez, que se deshacen con cualquier cosa): si una
 * flecha no les hace daño, el juego la hace rebotar sola. Los tridentes y la lanza sí les dan.
 *
 * VARIOS JUGADORES: la vida de los jefes crece con los jugadores que haya cerca (cada uno más, +60 %, hasta 6), y
 * reparten sus ataques entre todos (al empezar cada ronda eligen a uno al azar).
 */
public interface Jefe {

    UUID VIDA_POR_JUGADORES = UUID.fromString("5e0c3c52-8a2f-4d6e-9c41-7b1f2a9d3e10");
    float EXTRA_POR_JUGADOR = 0.6f;
    int MAXIMO_JUGADORES = 6;

    /** ¿Es el golpe de una flecha (normal, espectral, con efectos... o la de cualquier otro mod)? */
    static boolean esFlecha(DamageSource fuente) {
        return fuente.getDirectEntity() instanceof AbstractArrow flecha
                && !(flecha instanceof ThrownTrident) && !(flecha instanceof LanzaEntity);
    }

    /** Los jugadores que pelean (no en creativo ni espectador) a menos de "radio". */
    static List<Player> jugadores(LivingEntity jefe, double radio) {
        List<Player> lista = new ArrayList<>();
        for (Player p : jefe.level().players()) {
            if (p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(jefe) < radio * radio) {
                lista.add(p);
            }
        }
        return lista;
    }

    static int jugadoresCerca(LivingEntity jefe, double radio) {
        return jugadores(jefe, radio).size();
    }

    static float multiplicador(int jugadores) {
        return 1.0f + EXTRA_POR_JUGADOR * (Math.min(Math.max(jugadores, 1), MAXIMO_JUGADORES) - 1);
    }

    /** Pone el multiplicador de vida máxima (sin tocar la vida actual). */
    static void ponerMultiplicador(LivingEntity jefe, float multiplicador) {
        AttributeInstance vida = jefe.getAttribute(Attributes.MAX_HEALTH);
        if (vida == null) return;
        double extra = multiplicador - 1.0;
        AttributeModifier actual = vida.getModifier(VIDA_POR_JUGADORES);
        if (actual != null && Math.abs(actual.getAmount() - extra) < 1.0E-4) return;
        if (actual != null) vida.removeModifier(VIDA_POR_JUGADORES);
        if (extra > 1.0E-4) {
            vida.addPermanentModifier(new AttributeModifier(VIDA_POR_JUGADORES, "yonga_jefe_jugadores", extra,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    /** Ajusta la vida máxima a los jugadores que haya cerca, conservando el porcentaje de vida que le quede. */
    static void escalarVida(LivingEntity jefe, double radio) {
        float antes = jefe.getMaxHealth();
        float parte = antes > 0 ? jefe.getHealth() / antes : 1.0f;
        ponerMultiplicador(jefe, multiplicador(jugadoresCerca(jefe, radio)));
        if (Math.abs(jefe.getMaxHealth() - antes) > 0.01f) {
            jefe.setHealth(parte * jefe.getMaxHealth());
        }
    }

    /** Elige como objetivo a uno de los jugadores cercanos al azar (así reparte los ataques entre todos). */
    static void objetivoAlAzar(Mob jefe, double radio) {
        List<Player> lista = jugadores(jefe, radio);
        if (!lista.isEmpty()) {
            jefe.setTarget(lista.get(jefe.getRandom().nextInt(lista.size())));
        }
    }
}
