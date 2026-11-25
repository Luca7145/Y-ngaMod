package com.lucarita.yonga.modulos.habitantes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * La rutina de día de quien no trabaja: los parados y los holgazanes (jóvenes y adultos sin oficio) y los niños.
 * En horas de trabajo van de un sitio a otro de la aldea, se paran un rato, saludan a quien se cruzan y le dicen
 * algo (bocadillo, sin IA). Los parados, de paso, miran si hay algún puesto libre (ver Habitante.buscarTrabajo);
 * los holgazanes van más despacio y se paran más; los niños corretean y juegan.
 * Por la tarde se juntan con los demás en la plaza (CharlaGoal) y por la noche duermen, como todos.
 */
public class PasearGoal extends Goal {

    private final Habitante habitante;
    private Vec3 destino;
    private int parado;
    private int espera;

    public PasearGoal(Habitante habitante) {
        this.habitante = habitante;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean toca() {
        int etapa = habitante.etapa();
        boolean libre = etapa == 1 || (etapa >= 2 && habitante.sinOficio());
        return libre && !habitante.esGuardia() && habitante.enemigo() == null && habitante.seguido() == null
                && habitante.interlocutor() == null && habitante.cliente() == null && Rutina.trabajo(habitante.level());
    }

    @Override
    public boolean canUse() {
        return toca() && habitante.getRandom().nextInt(20) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return toca();
    }

    @Override
    public void start() {
        this.destino = null;
        this.parado = 0;
        this.espera = 0;
    }

    @Override
    public void stop() {
        habitante.getNavigation().stop();
    }

    private double velocidad() {
        if (habitante.etapa() == 1) return 0.7;
        return habitante.esHolgazan() ? 0.4 : 0.5;
    }

    /** Un sitio al azar de la aldea (cerca de la campana), o cerca de donde está si no tiene aldea. */
    private Vec3 sitioNuevo() {
        BlockPos centro = habitante.aldea() != null ? habitante.aldea() : habitante.blockPosition();
        int radio = habitante.etapa() == 1 ? 16 : 28;
        var azar = habitante.getRandom();
        return Vec3.atBottomCenterOf(centro).add(azar.nextInt(radio * 2 + 1) - radio, 0, azar.nextInt(radio * 2 + 1) - radio);
    }

    @Override
    public void tick() {
        var azar = habitante.getRandom();
        if (destino == null) {
            destino = sitioNuevo();
            espera = 0;
        }
        if (parado > 0) {
            parado--;
            // Mientras está parado, mira a quien tenga cerca y a veces le dice algo
            List<Habitante> cerca = habitante.level().getEntitiesOfClass(Habitante.class,
                    new AABB(habitante.blockPosition()).inflate(4), v -> v != habitante && v.etapa() >= 1);
            if (!cerca.isEmpty()) {
                Habitante otro = cerca.get(0);
                habitante.getLookControl().setLookAt(otro, 30.0f, 30.0f);
                if (azar.nextInt(400) == 0 && !habitante.cotillearCon(otro)) {
                    String tipo = habitante.etapa() == 1 ? "nino" : habitante.esHolgazan() ? "holgazan" : "paro";
                    habitante.decirEnAlto(Component.translatable("yonga.charla." + tipo + "." + azar.nextInt(5)));
                }
            }
            if (parado == 0) destino = null;
            return;
        }
        if (habitante.distanceToSqr(destino) < 2.0 * 2.0 || habitante.getNavigation().isDone() && espera > 0) {
            habitante.getNavigation().stop();
            // Se queda un rato: los holgazanes, más; los niños, poco (enseguida salen corriendo a otro sitio)
            int base = habitante.etapa() == 1 ? 60 : habitante.esHolgazan() ? 400 : 200;
            parado = base + azar.nextInt(base);
            return;
        }
        if (--espera <= 0) {
            espera = 40;
            if (!habitante.getNavigation().moveTo(destino.x, destino.y, destino.z, velocidad())) {
                destino = sitioNuevo();      // no se puede llegar: a otro sitio
            }
        }
    }
}
