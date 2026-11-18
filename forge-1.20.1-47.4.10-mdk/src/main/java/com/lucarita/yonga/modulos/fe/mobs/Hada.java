package com.lucarita.yonga.modulos.fe.mobs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Vector3f;

/**
 * Hada (luminosa) y hada oscura: bolitas que brillan y vuelan por los bosques a cualquier hora.
 * Son seres inferiores. Al atacar no hacen daño: dan Levitación (y la caída ya hace el resto).
 * Las dos funcionan igual; solo cambia el bando (a quién atacan, ver MobFe) y el color.
 */
public class Hada extends PathfinderMob implements MobFe, FlyingAnimal {

    // ---- Ajustes ----
    public static final int LEVITACION_TICKS = 200;      // 10 segundos de Levitación I por toque
    public static final int TIEMPO_ENTRE_TOQUES = 60;    // 3 segundos entre toque y toque (cada toque vuelve a poner los 10 s)

    private static final Vector3f COLOR_LUZ = new Vector3f(0.55f, 0.85f, 1.0f);
    private static final Vector3f COLOR_OSCURO = new Vector3f(0.70f, 0.30f, 1.0f);

    private final boolean oscura;

    public Hada(EntityType<? extends Hada> tipo, Level nivel, boolean oscura) {
        super(tipo, nivel);
        this.oscura = oscura;
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder atributos() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    public boolean esOscuro() {
        return oscura;
    }

    @Override
    public boolean esSuperior() {
        return false;
    }

    // ---------- IA ----------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true) {
            @Override
            protected int getAttackInterval() {
                return this.adjustedTickDelay(TIEMPO_ENTRE_TOQUES);
            }
        });
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::atacaA));
    }

    @Override
    protected PathNavigation createNavigation(Level nivel) {
        FlyingPathNavigation navegacion = new FlyingPathNavigation(this, nivel);
        navegacion.setCanOpenDoors(false);
        navegacion.setCanFloat(true);
        navegacion.setCanPassDoors(true);
        return navegacion;
    }

    /** El toque: sin daño, solo Levitación. */
    @Override
    public boolean doHurtTarget(Entity objetivo) {
        if (!(objetivo instanceof LivingEntity vivo)) {
            return false;
        }
        vivo.addEffect(new MobEffectInstance(MobEffects.LEVITATION, LEVITACION_TICKS, 0), this);
        this.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, oscura ? 0.6f : 1.4f);
        this.setLastHurtMob(objetivo);
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            // Estela de brillos
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(new DustParticleOptions(oscura ? COLOR_OSCURO : COLOR_LUZ, 0.7f),
                        this.getRandomX(0.6), this.getY() + this.getBbHeight() * 0.5 + (this.random.nextDouble() - 0.5) * 0.4,
                        this.getRandomZ(0.6), 0.0, 0.0, 0.0);
            }
            return;
        }
        // Si el jugador cambia de fe y ya no es de los que ataca, lo deja en paz
        // (salvo que le haya pegado: entonces se defiende igual)
        LivingEntity objetivo = this.getTarget();
        if (objetivo instanceof Player && !atacaA(objetivo) && objetivo != this.getLastHurtByMob()) {
            this.setTarget(null);
        }
    }

    // ---------- VUELO ----------

    @Override
    public boolean isFlying() {
        return !this.onGround();
    }

    @Override
    public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fuente) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean enSuelo, BlockState bloque, BlockPos posicion) {
    }

    @Override
    protected void playStepSound(BlockPos posicion, BlockState bloque) {
    }

    // ---------- SONIDOS ----------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource fuente) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (oscura ? 0.75f : 1.25f);
    }

    // ---------- APARICIÓN ----------

    /**
     * Aparecen sobre tierra, hierba u hojas, a cualquier hora (la luz no importa), y solo en la superficie:
     * no en cuevas (tiene que estar a la altura del suelo o encima de los árboles).
     */
    public static boolean puedeAparecer(EntityType<? extends Hada> tipo, LevelAccessor nivel, MobSpawnType razon,
                                        BlockPos posicion, RandomSource azar) {
        BlockState debajo = nivel.getBlockState(posicion.below());
        int suelo = nivel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, posicion.getX(), posicion.getZ());
        return (debajo.is(BlockTags.DIRT) || debajo.is(BlockTags.LEAVES))
                && nivel.getBlockState(posicion).isAir()
                && posicion.getY() >= suelo;
    }

    @Override
    public boolean removeWhenFarAway(double distancia) {
        return true;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 3;
    }
}
