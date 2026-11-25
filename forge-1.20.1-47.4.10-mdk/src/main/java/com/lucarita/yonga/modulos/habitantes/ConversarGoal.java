package com.lucarita.yonga.modulos.habitantes;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Mientras alguien habla con él, el habitante se queda quieto mirándole. Solo pueden interrumpirlo el pánico (si le
 * hacen daño) y huir de los zombis; si le atacan o le ofenden, la conversación se corta (ver Conversaciones).
 */
public class ConversarGoal extends Goal {

    private final Habitante habitante;

    public ConversarGoal(Habitante habitante) {
        this.habitante = habitante;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return habitante.interlocutor() != null;
    }

    @Override
    public void start() {
        habitante.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Player jugador = habitante.interlocutor();
        if (jugador != null) {
            habitante.getNavigation().stop();
            habitante.getLookControl().setLookAt(jugador, 30.0f, 30.0f);
        }
    }
}
