package com.lucarita.yonga.modulos.habitantes;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Pelearse con un jugador que le ha ofendido de verdad (lo decide la conversación) o que le ha pegado (los gruñones y
 * los orgullosos devuelven los golpes): va a por él a puñetazos. Con "un golpe" le da una vez y se va; si no, sigue
 * hasta darle 4 golpes o pasar un minuto. Malherido, lo deja y huye. Es lo más importante que hace: va por encima
 * de todo lo demás.
 */
public class PeleaGoal extends Goal {

    private final Habitante habitante;
    private int espera;
    private int recarga;

    public PeleaGoal(Habitante habitante) {
        this.habitante = habitante;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return habitante.enemigo() != null;
    }

    @Override
    public void start() {
        this.espera = 0;
        this.recarga = 10;
    }

    @Override
    public void stop() {
        habitante.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Player enemigo = habitante.enemigo();
        if (enemigo == null) return;
        habitante.getLookControl().setLookAt(enemigo, 30.0f, 30.0f);
        double distancia = habitante.distanceToSqr(enemigo);
        double alcance = habitante.getBbWidth() * 2.0 * habitante.getBbWidth() * 2.0 + enemigo.getBbWidth() + 1.2;
        if (--espera <= 0) {
            espera = 8;
            habitante.getNavigation().moveTo(enemigo, 0.85);
        }
        if (--recarga <= 0 && distancia <= alcance && habitante.hasLineOfSight(enemigo)) {
            recarga = 20;
            habitante.swing(InteractionHand.MAIN_HAND);
            if (habitante.doHurtTarget(enemigo)) {
                habitante.golpeDado();
            }
        }
    }
}
