package com.lucarita.yonga.modulos.fe.mobs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;

/**
 * Enano (luminoso, amarillo) y enano oscuro (rojo): un bloque con patitas y bracitos que brilla.
 * Son seres inferiores. Andan lentito, pero cuando ven a alguien a quien atacar (ver MobFe) se preparan
 * un segundo y salen disparados a embestirle, como las cabras: poco daño, pero lo mandan lejos.
 * Aparecen un poco en cualquier sitio, casi siempre solos.
 */
public class Enano extends PathfinderMob implements MobFe {

    // ---- Ajustes ----
    public static final double VELOCIDAD_ANDAR = 0.17;     // un zombi va a 0.23
    public static final int TICKS_PREPARAR = 20;           // 1 segundo agachado antes de salir
    public static final int TICKS_CORRER_MAX = 30;         // si en 1,5 s no ha chocado, para
    public static final double VELOCIDAD_EMBESTIDA = 0.75; // bloques por tick durante la carrera
    public static final float DANO_EMBESTIDA = 3.0f;       // 1,5 corazones
    public static final float EMPUJE = 2.6f;               // la cabra empuja ~2,5
    public static final double EMPUJE_ARRIBA = 0.45;
    public static final int ESPERA_ENTRE_EMBESTIDAS = 80;  // 4 segundos
    public static final double DISTANCIA_MIN = 2.0;
    public static final double DISTANCIA_MAX = 14.0;

    /** Estado sincronizado para la animación: 0 = normal, 1 = preparándose, 2 = embistiendo. */
    public static final int NORMAL = 0, PREPARANDO = 1, EMBISTIENDO = 2;
    private static final EntityDataAccessor<Integer> ESTADO = SynchedEntityData.defineId(Enano.class, EntityDataSerializers.INT);

    private static final Vector3f COLOR_LUZ = new Vector3f(1.0f, 0.85f, 0.3f);
    private static final Vector3f COLOR_OSCURO = new Vector3f(1.0f, 0.2f, 0.15f);

    private final boolean oscuro;

    public Enano(EntityType<? extends Enano> tipo, Level nivel, boolean oscuro) {
        super(tipo, nivel);
        this.oscuro = oscuro;
        this.setMaxUpStep(1.0f);
    }

    public static AttributeSupplier.Builder atributos() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, VELOCIDAD_ANDAR)
                .add(Attributes.ATTACK_DAMAGE, DANO_EMBESTIDA)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    public boolean esOscuro() {
        return oscuro;
    }

    @Override
    public boolean esSuperior() {
        return false;
    }

    public int estado() {
        return this.entityData.get(ESTADO);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ESTADO, NORMAL);
    }

    // ---------- IA ----------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new Embestida());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::atacaA));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            int cada = estado() == EMBISTIENDO ? 1 : 5;
            if (this.random.nextInt(cada) == 0) {
                this.level().addParticle(new DustParticleOptions(oscuro ? COLOR_OSCURO : COLOR_LUZ, 0.8f),
                        this.getRandomX(0.6), this.getY() + this.random.nextDouble() * this.getBbHeight(),
                        this.getRandomZ(0.6), 0.0, 0.0, 0.0);
            }
            return;
        }
        // Si el jugador cambia de fe y ya no es de los que ataca, lo deja en paz (salvo que le haya pegado)
        LivingEntity objetivo = this.getTarget();
        if (objetivo instanceof Player && !atacaA(objetivo) && objetivo != this.getLastHurtByMob()) {
            this.setTarget(null);
        }
    }

    /**
     * La embestida: se para y se agacha mirando al objetivo (1 s), sale en línea recta hacia donde estaba y,
     * si choca con él, le hace un poco de daño y lo manda lejos. Si choca con una pared o no llega, para.
     */
    private class Embestida extends Goal {
        private int listoEn;         // tickCount a partir del cual puede volver a embestir
        private int ticks;
        private Vec3 direccion = Vec3.ZERO;

        Embestida() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (tickCount < listoEn) {
                return false;
            }
            LivingEntity objetivo = getTarget();
            if (objetivo == null || !objetivo.isAlive() || !onGround()) {
                return false;
            }
            double distancia = distanceTo(objetivo);
            return distancia >= DISTANCIA_MIN && distancia <= DISTANCIA_MAX && getSensing().hasLineOfSight(objetivo);
        }

        @Override
        public boolean canContinueToUse() {
            return ticks < TICKS_PREPARAR + TICKS_CORRER_MAX && getTarget() != null && getTarget().isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            ticks = 0;
            getNavigation().stop();
            entityData.set(ESTADO, PREPARANDO);
            playSound(SoundEvents.GOAT_PREPARE_RAM, 1.0f, oscuro ? 0.7f : 1.2f);
        }

        @Override
        public void stop() {
            entityData.set(ESTADO, NORMAL);
            listoEn = tickCount + ESPERA_ENTRE_EMBESTIDAS;
            ticks = TICKS_PREPARAR + TICKS_CORRER_MAX;
        }

        @Override
        public void tick() {
            LivingEntity objetivo = getTarget();
            if (objetivo == null) {
                return;
            }
            ticks++;
            if (ticks <= TICKS_PREPARAR) {
                // Preparándose: quieto, mirándole
                getLookControl().setLookAt(objetivo, 30.0f, 30.0f);
                getNavigation().stop();
                if (ticks == TICKS_PREPARAR) {
                    Vec3 hacia = objetivo.position().subtract(position());
                    direccion = new Vec3(hacia.x, 0.0, hacia.z).normalize();
                    entityData.set(ESTADO, EMBISTIENDO);
                    playSound(SoundEvents.GOAT_LONG_JUMP, 1.0f, oscuro ? 0.7f : 1.2f);
                }
                return;
            }
            // Corriendo en línea recta
            float giro = (float) (Mth.atan2(direccion.z, direccion.x) * Mth.RAD_TO_DEG) - 90.0f;
            setYRot(giro);
            setYBodyRot(giro);
            setYHeadRot(giro);
            setDeltaMovement(direccion.x * VELOCIDAD_EMBESTIDA, getDeltaMovement().y, direccion.z * VELOCIDAD_EMBESTIDA);

            if (getBoundingBox().inflate(0.3).intersects(objetivo.getBoundingBox())) {
                objetivo.hurt(damageSources().mobAttack(Enano.this), DANO_EMBESTIDA);
                double resistencia = objetivo.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
                objetivo.knockback(EMPUJE * (1.0 - resistencia), -direccion.x, -direccion.z);
                objetivo.setDeltaMovement(objetivo.getDeltaMovement().add(0.0, EMPUJE_ARRIBA * (1.0 - resistencia), 0.0));
                objetivo.hurtMarked = true;
                playSound(SoundEvents.GOAT_RAM_IMPACT, 1.0f, oscuro ? 0.7f : 1.2f);
                setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
                stop();
            } else if (horizontalCollision) {
                playSound(SoundEvents.GOAT_HORN_BREAK, 0.6f, oscuro ? 0.7f : 1.2f);
                stop();
            }
        }
    }

    // ---------- SONIDOS ----------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GOAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource fuente) {
        return SoundEvents.GOAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GOAT_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos posicion, BlockState bloque) {
        this.playSound(SoundEvents.GOAT_STEP, 0.15f, 1.0f);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (oscuro ? 0.6f : 1.3f);
    }

    // ---------- APARICIÓN ----------

    /** Sobre cualquier suelo sólido, a cualquier hora y en cualquier sitio, pero poco (1 de cada 3 intentos). */
    public static boolean puedeAparecer(EntityType<? extends Enano> tipo, LevelAccessor nivel, MobSpawnType razon,
                                        BlockPos posicion, RandomSource azar) {
        return razon != MobSpawnType.NATURAL || azar.nextInt(3) == 0;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }
}
