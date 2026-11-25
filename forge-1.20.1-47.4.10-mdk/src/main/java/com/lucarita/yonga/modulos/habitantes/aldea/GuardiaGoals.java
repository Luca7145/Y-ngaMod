package com.lucarita.yonga.modulos.habitantes.aldea;

import com.lucarita.yonga.modulos.habitantes.Habitante;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;

/**
 * Lo que hace distinto a un guardia (y lo que NO hace un vecino normal):
 *   · Pánico y huida de zombis / saqueadores: solo los vecinos; los guardias se quedan y pelean.
 *   · Atacar: solo los guardias, a monstruos (no a los creepers), saqueadores, a quien les ataque y a los jugadores con
 *     mala fama en su aldea (−50 o menos). Unos van con espada (cuerpo a cuerpo) y otros con arco (a distancia: se
 *     quedan a unos 12 bloques y disparan flechas). Si uno de espada no consigue acercarse a su objetivo (metido en el
 *     agua, en un sitio sin camino…), lo deja estar un rato y busca otro; los de espada no eligen monstruos que estén
 *     nadando.
 */
public final class GuardiaGoals {

    private GuardiaGoals() {}

    /** Pánico al recibir daño o arder… salvo los guardias. */
    public static class Panico extends PanicGoal {
        private final Habitante h;

        public Panico(Habitante h, double velocidad) {
            super(h, velocidad);
            this.h = h;
        }

        @Override
        public boolean canUse() {
            return !h.esGuardia() && super.canUse();
        }
    }

    /** Huir de algo (zombis, saqueadores)… salvo los guardias. */
    public static class Huir<T extends LivingEntity> extends AvoidEntityGoal<T> {
        private final Habitante h;

        public Huir(Habitante h, Class<T> clase, float distancia, double lento, double rapido) {
            super(h, clase, distancia, lento, rapido);
            this.h = h;
        }

        @Override
        public boolean canUse() {
            return !h.esGuardia() && super.canUse();
        }
    }

    /** Pelear cuerpo a cuerpo con su objetivo: los guardias de espada. Si no consigue acercarse, lo deja. */
    public static class Atacar extends MeleeAttackGoal {
        private final Habitante h;
        private double mejorDistancia;
        private int sinAvanzar;

        public Atacar(Habitante h) {
            super(h, 0.8, true);
            this.h = h;
        }

        @Override
        public boolean canUse() {
            return h.esGuardia() && !h.esArquero() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return h.esGuardia() && !h.esArquero() && super.canContinueToUse();
        }

        @Override
        public void start() {
            super.start();
            this.mejorDistancia = Double.MAX_VALUE;
            this.sinAvanzar = 0;
        }

        @Override
        public void tick() {
            super.tick();
            LivingEntity objetivo = h.getTarget();
            if (objetivo == null) return;
            double d = h.distanceToSqr(objetivo);
            if (d < this.mejorDistancia - 0.5) {
                this.mejorDistancia = d;
                this.sinAvanzar = 0;
            } else if (d > 3.0 * 3.0 && ++this.sinAvanzar > 80) {
                // 4 segundos sin acercarse: no hay forma de llegar. Lo deja y no lo vuelve a elegir en 10 s.
                h.ignorar(objetivo, 200);
                h.setTarget(null);
            }
        }
    }

    /** Disparar flechas a su objetivo desde lejos: los guardias de arco. */
    public static class Disparar extends net.minecraft.world.entity.ai.goal.RangedAttackGoal {
        private final Habitante h;

        public Disparar(Habitante h) {
            super(h, 0.7, 25, 14.0f);
            this.h = h;
        }

        @Override
        public boolean canUse() {
            return h.esGuardia() && h.esArquero() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return h.esGuardia() && h.esArquero() && super.canContinueToUse();
        }
    }

    /** Devolver el ataque a quien le pegue: solo los guardias (y avisa a los otros guardias cercanos). */
    public static class Defenderse extends HurtByTargetGoal {
        private final Habitante h;

        public Defenderse(Habitante h) {
            super(h);
            this.h = h;
            this.setAlertOthers(Habitante.class);
        }

        @Override
        public boolean canUse() {
            return h.esGuardia() && super.canUse();
        }

        @Override
        protected void alertOther(net.minecraft.world.entity.Mob otro, LivingEntity atacante) {
            if (otro instanceof Habitante vecino && vecino.esGuardia()) {
                super.alertOther(otro, atacante);
            }
        }
    }

    /** Buscar a quién atacar en la aldea: monstruos, saqueadores y jugadores con muy mala fama. */
    public static class Vigilar extends NearestAttackableTargetGoal<LivingEntity> {
        private final Habitante h;

        public Vigilar(Habitante h) {
            super(h, LivingEntity.class, 10, true, false, objetivo -> esAmenaza(h, objetivo));
            this.h = h;
        }

        @Override
        public boolean canUse() {
            return h.esGuardia() && super.canUse();
        }

        private static boolean esAmenaza(Habitante h, LivingEntity e) {
            if (e instanceof Creeper || h.ignora(e)) return false;
            if (!h.esArquero() && e.isInWater() && !e.onGround()) return false;     // los de espada no llegan
            if (e instanceof Raider || e instanceof Enemy) return true;
            if (e instanceof Player jugador && !jugador.isCreative() && !jugador.isSpectator()
                    && h.aldea() != null && h.level() instanceof ServerLevel nivel) {
                Aldeas.Aldea aldea = Aldeas.de(nivel).aldeaSiExiste(h.aldea());
                return aldea != null && aldea.fama(jugador.getUUID()) <= Aldeas.FAMA_ENEMIGO;
            }
            return false;
        }
    }
}
