package com.lucarita.yonga.modulos.fe.mobs;

import com.lucarita.yonga.modulos.fe.ModuloFe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Gnomo: ser luminoso INFERIOR (ataca del Ateo hacia abajo, ver MobFe). Un espadachín pequeño, ágil y rapidísimo.
 * No va directo a por ti: te rodea a saltitos, y de repente ataca.
 *
 *  - PICADO: se agacha, salta muy alto, en el aire gira la espada hacia abajo y cae en picado. Si te da, hace
 *    bastante daño. Si falla, el golpe contra el suelo suelta una pequeña onda que empuja un poco... y la espada
 *    se le queda clavada: tira y tira para sacarla, y ahí se le puede castigar (recibe más daño y no se mueve).
 *  - TAJO RELÁMPAGO: se echa atrás un instante y sale disparado en línea recta, cortando todo a su paso.
 * Aparece en sitios con vegetación (bosques, llanuras, taigas, junglas...), a cualquier hora.
 */
public class Gnomo extends PathfinderMob implements MobFe {

    // ---------- AJUSTES ----------
    public static final double VELOCIDAD = 0.38;            // de lo más rápido del mod
    private static final double RADIO_RODEO = 4.5;
    private static final float PROB_SALTITO = 0.07f;        // por tick, al rodearte

    // Picado
    private static final int TICKS_AGACHARSE = 7;
    private static final double SALTO_ARRIBA = 0.95, SALTO_ADELANTE = 0.55;
    private static final int TICKS_SUBIDA_MAX = 14;
    private static final double VELOCIDAD_PICADO = 1.5;
    private static final float DANO_PICADO = 9.0f;          // 4,5 corazones
    private static final double RADIO_ONDA = 3.0;
    private static final int TICKS_ATASCADO = 55;           // 2,75 s con la espada clavada
    private static final float CASTIGO = 1.5f;
    private static final int ESPERA_PICADO = 90;

    // Tajo relámpago
    private static final int TICKS_AVISO_TAJO = 8, TICKS_DASH = 5, TICKS_RECUPERAR_TAJO = 8;
    private static final double VELOCIDAD_DASH = 1.5;
    private static final float DANO_TAJO = 7.0f;            // 3,5 corazones
    private static final int ESPERA_TAJO = 50;

    private static final int ESPERA_ENTRE_ATAQUES = 25;
    private static final double ATAQUE_MIN = 2.0, ATAQUE_MAX = 7.5;

    // ---------- ESTADO (sincronizado para las animaciones) ----------
    public static final int NADA = 0, AGACHARSE = 1, SUBIDA = 2, PICADO = 3, ATASCADO = 4, AVISO_TAJO = 5, DASH = 6, RECUPERAR = 7;
    private static final EntityDataAccessor<Integer> ACCION = SynchedEntityData.defineId(Gnomo.class, EntityDataSerializers.INT);

    private int ticksAccion;
    private int espera, esperaPicado, esperaTajo;
    private int ladoRodeo = 1;
    private Vec3 direccion = Vec3.ZERO;
    private final Set<Integer> golpeados = new HashSet<>();

    public int inicioAccionCliente;

    public Gnomo(EntityType<? extends Gnomo> tipo, Level nivel) {
        super(tipo, nivel);
        this.xpReward = 8;
        this.setMaxUpStep(1.0f);
    }

    public static AttributeSupplier.Builder atributos() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 22.0)
                .add(Attributes.MOVEMENT_SPEED, VELOCIDAD)
                .add(Attributes.ATTACK_DAMAGE, DANO_TAJO)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    public boolean esOscuro() {
        return false;
    }

    @Override
    public boolean esSuperior() {
        return false;
    }

    public int accion() {
        return this.entityData.get(ACCION);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ACCION, NADA);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> dato) {
        super.onSyncedDataUpdated(dato);
        if (ACCION.equals(dato)) {
            this.inicioAccionCliente = this.tickCount;
        }
    }

    // ---------- IA ----------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.7) {
            @Override
            public boolean canUse() {
                return getTarget() == null && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return getTarget() == null && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::atacaA));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (espera > 0) espera--;
        if (esperaPicado > 0) esperaPicado--;
        if (esperaTajo > 0) esperaTajo--;
        ticksAccion++;
        int accion = accion();

        // El picado y el atasco siguen aunque se quede sin objetivo
        if (accion == PICADO) {
            caerEnPicado();
            return;
        }
        if (accion == ATASCADO) {
            atascado();
            return;
        }

        LivingEntity objetivo = this.getTarget();
        if (objetivo != null && (!objetivo.isAlive() || (objetivo instanceof Player p && (p.isCreative() || p.isSpectator()))
                || (objetivo instanceof Player && !atacaA(objetivo) && objetivo != this.getLastHurtByMob()))) {
            this.setTarget(null);
            objetivo = null;
        }
        if (objetivo == null) {
            if (accion == SUBIDA) {
                if (this.getDeltaMovement().y <= 0.05) empezar(PICADO);   // sin objetivo, cae igual
            } else if (accion != NADA) {
                terminar();
            }
            return;
        }
        double distancia = this.distanceTo(objetivo);

        switch (accion) {
            case AGACHARSE -> {
                this.getNavigation().stop();
                mirarHacia(objetivo, 0.6f);
                if (ticksAccion >= TICKS_AGACHARSE) {
                    Vec3 hacia = objetivo.position().subtract(this.position()).multiply(1, 0, 1);
                    double lejos = Math.min(1.0, hacia.length() / 5.0);
                    Vec3 dir = hacia.normalize();
                    this.setDeltaMovement(dir.x * SALTO_ADELANTE * lejos * 1.6, SALTO_ARRIBA, dir.z * SALTO_ADELANTE * lejos * 1.6);
                    this.hasImpulse = true;
                    empezar(SUBIDA);
                    this.playSound(ModuloFe.SONIDO_GNOMO_GRITO.get(), 1.0f, 1.0f + this.random.nextFloat() * 0.2f);
                }
            }
            case SUBIDA -> {
                // en el aire: se va colocando encima y, al llegar arriba, se lanza en picado
                mirarHacia(objetivo, 0.5f);
                Vec3 hacia = objetivo.position().subtract(this.position()).multiply(1, 0, 1);
                this.setDeltaMovement(this.getDeltaMovement().add(hacia.normalize().scale(0.04 * Math.min(1.0, hacia.length()))));
                if (this.getDeltaMovement().y <= 0.05 || ticksAccion >= TICKS_SUBIDA_MAX) {
                    Vec3 sobre = hacia.length() > 0.4 ? hacia.normalize().scale(Math.min(0.35, hacia.length() * 0.12)) : Vec3.ZERO;
                    this.setDeltaMovement(sobre.x, -VELOCIDAD_PICADO, sobre.z);
                    empezar(PICADO);
                }
            }
            case AVISO_TAJO -> {
                this.getNavigation().stop();
                mirarHacia(objetivo, 0.7f);
                if (ticksAccion == TICKS_AVISO_TAJO - 2 && this.level() instanceof ServerLevel nivel) {
                    nivel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 0.6, this.getZ(), 8, 0.3, 0.2, 0.3, 0.1);
                }
                if (ticksAccion >= TICKS_AVISO_TAJO) {
                    Vec3 hacia = objetivo.position().subtract(this.position());
                    direccion = new Vec3(hacia.x, 0.0, hacia.z).normalize();
                    golpeados.clear();
                    empezar(DASH);
                    this.playSound(ModuloFe.SONIDO_LEON_TAJO.get(), 1.2f, 1.7f);
                }
            }
            case DASH -> dash();
            case RECUPERAR -> {
                this.getNavigation().stop();
                if (ticksAccion >= TICKS_RECUPERAR_TAJO) terminar();
            }
            default -> rodear(objetivo, distancia);
        }
    }

    /** Te rodea a saltitos y, cuando puede, ataca. */
    private void rodear(LivingEntity objetivo, double distancia) {
        this.getLookControl().setLookAt(objetivo, 40.0f, 40.0f);
        if (espera == 0 && distancia > ATAQUE_MIN && distancia < ATAQUE_MAX && this.onGround()
                && this.getSensing().hasLineOfSight(objetivo)) {
            boolean picado = esperaPicado == 0 && (esperaTajo > 0 || this.random.nextFloat() < 0.45f);
            if (picado) {
                esperaPicado = ESPERA_PICADO;
                espera = ESPERA_ENTRE_ATAQUES;
                empezar(AGACHARSE);
                return;
            }
            if (esperaTajo == 0) {
                esperaTajo = ESPERA_TAJO;
                espera = ESPERA_ENTRE_ATAQUES;
                empezar(AVISO_TAJO);
                return;
            }
        }
        if (this.tickCount % 8 == 0 || this.getNavigation().isDone()) {
            if (this.random.nextInt(6) == 0) {
                ladoRodeo = -ladoRodeo;
            }
            double angulo = Math.atan2(this.getZ() - objetivo.getZ(), this.getX() - objetivo.getX()) + ladoRodeo * 0.7;
            double radio = distancia > RADIO_RODEO + 4 ? distancia - 3 : RADIO_RODEO;
            double x = objetivo.getX() + Math.cos(angulo) * radio;
            double z = objetivo.getZ() + Math.sin(angulo) * radio;
            int y = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
            this.getNavigation().moveTo(x, y, z, 1.25);
        }
        if (this.onGround() && this.random.nextFloat() < PROB_SALTITO) {
            this.getJumpControl().jump();      // saltitos mientras te rodea
        }
    }

    private void caerEnPicado() {
        LivingEntity objetivo = this.getTarget();
        this.setDeltaMovement(this.getDeltaMovement().x * 0.9, Math.min(this.getDeltaMovement().y, -VELOCIDAD_PICADO * 0.8),
                this.getDeltaMovement().z * 0.9);
        if (objetivo != null && this.getBoundingBox().inflate(0.3).intersects(objetivo.getBoundingBox())) {
            golpeDePicado(objetivo);
            return;
        }
        if (this.onGround() || ticksAccion > 40) {
            boolean acierta = objetivo != null && Math.abs(objetivo.getY() - this.getY()) < 2.0
                    && objetivo.position().multiply(1, 0, 1).distanceTo(this.position().multiply(1, 0, 1)) < 1.3;
            if (acierta) {
                golpeDePicado(objetivo);
            } else {
                fallarPicado();
            }
        }
    }

    private void golpeDePicado(LivingEntity objetivo) {
        objetivo.hurt(this.damageSources().mobAttack(this), DANO_PICADO);
        this.playSound(SoundEvents.TRIDENT_HIT, 1.2f, 1.3f);
        this.setDeltaMovement(0.0, 0.35, 0.0);    // rebota en él
        terminar();
    }

    /** Ha fallado: onda que empuja un poco y la espada clavada en el suelo. */
    private void fallarPicado() {
        if (this.level() instanceof ServerLevel nivel) {
            for (LivingEntity vivo : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(RADIO_ONDA, 1.0, RADIO_ONDA))) {
                if (vivo == this || vivo instanceof MobFe || this.distanceTo(vivo) > RADIO_ONDA) continue;
                Vec3 hacia = vivo.position().subtract(this.position()).multiply(1, 0, 1).normalize();
                vivo.knockback(0.9, -hacia.x, -hacia.z);
                vivo.setDeltaMovement(vivo.getDeltaMovement().add(0.0, 0.25, 0.0));
                vivo.hurtMarked = true;
                if (vivo instanceof ServerPlayer jugador) {
                    jugador.connection.send(new ClientboundSetEntityMotionPacket(jugador));
                }
            }
            BlockState suelo = this.level().getBlockState(this.blockPosition().below());
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8;
                nivel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, suelo),
                        this.getX() + Math.cos(a) * 1.5, this.getY() + 0.1, this.getZ() + Math.sin(a) * 1.5, 3, 0.2, 0.05, 0.2, 0.1);
            }
            nivel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.1, this.getZ(), 10, 1.2, 0.05, 1.2, 0.05);
        }
        this.playSound(SoundEvents.TRIDENT_HIT_GROUND, 1.5f, 0.8f);
        this.setDeltaMovement(Vec3.ZERO);
        empezar(ATASCADO);
    }

    private void atascado() {
        this.getNavigation().stop();
        this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
        if (ticksAccion % 12 == 6) {
            this.playSound(ModuloFe.SONIDO_GNOMO_ESFUERZO.get(), 0.9f, 0.9f + this.random.nextFloat() * 0.3f);
            if (this.level() instanceof ServerLevel nivel) {
                BlockState suelo = this.level().getBlockState(this.blockPosition().below());
                Vec3 delante = this.position().add(Vec3.directionFromRotation(0.0f, this.yBodyRot).scale(0.5));
                nivel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, suelo), delante.x, this.getY() + 0.1, delante.z,
                        4, 0.1, 0.05, 0.1, 0.05);
            }
        }
        if (ticksAccion >= TICKS_ATASCADO) {
            this.playSound(SoundEvents.TRIDENT_RETURN, 1.0f, 1.4f);
            terminar();
        }
    }

    /** El tajo relámpago: en línea recta, muy rápido, cortando a todo lo atacable que pille. */
    private void dash() {
        this.getNavigation().stop();
        float giro = (float) (Mth.atan2(direccion.z, direccion.x) * Mth.RAD_TO_DEG) - 90.0f;
        this.setYRot(giro);
        this.setYBodyRot(giro);
        this.setYHeadRot(giro);
        this.setDeltaMovement(direccion.x * VELOCIDAD_DASH, this.getDeltaMovement().y, direccion.z * VELOCIDAD_DASH);
        if (this.level() instanceof ServerLevel nivel) {
            nivel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 0.5, this.getZ(), 4, 0.2, 0.2, 0.2, 0.02);
            nivel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.5, this.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
        }
        for (LivingEntity vivo : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.6))) {
            if (vivo == this || vivo instanceof MobFe || !golpeados.add(vivo.getId())) continue;
            if (vivo == this.getTarget() || (vivo instanceof Player && atacaA(vivo))) {
                vivo.hurt(this.damageSources().mobAttack(this), DANO_TAJO);
                if (this.level() instanceof ServerLevel nivel) {
                    nivel.sendParticles(ParticleTypes.SWEEP_ATTACK, vivo.getX(), vivo.getY() + 0.8, vivo.getZ(), 1, 0, 0, 0, 0);
                }
            }
        }
        if (ticksAccion >= TICKS_DASH || this.horizontalCollision) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.2, 1.0, 0.2));
            empezar(RECUPERAR);
        }
    }

    private void empezar(int accion) {
        this.entityData.set(ACCION, accion);
        this.ticksAccion = 0;
    }

    private void terminar() {
        this.entityData.set(ACCION, NADA);
        this.ticksAccion = 0;
    }

    private void mirarHacia(LivingEntity objetivo, float rapidez) {
        float giro = (float) (Mth.atan2(objetivo.getZ() - this.getZ(), objetivo.getX() - this.getX()) * Mth.RAD_TO_DEG) - 90.0f;
        this.setYRot(Mth.rotLerp(rapidez, this.getYRot(), giro));
        this.setYBodyRot(this.getYRot());
    }

    // ---------- DAÑO ----------

    @Override
    public boolean hurt(DamageSource fuente, float cantidad) {
        if (accion() == ATASCADO && !fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            cantidad *= CASTIGO;   // ¡castigo!
        }
        return super.hurt(fuente, cantidad);
    }

    @Override
    public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fuente) {
        return false;
    }

    // ---------- SONIDOS ----------

    @Override
    protected SoundEvent getAmbientSound() {
        return ModuloFe.SONIDO_GNOMO_GRITO.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource fuente) {
        return ModuloFe.SONIDO_GNOMO_HERIDO.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModuloFe.SONIDO_GNOMO_MUERTE.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.8f;
    }

    // ---------- APARICIÓN ----------

    /** Sobre tierra o hierba, en la superficie, a cualquier hora. */
    public static boolean puedeAparecer(EntityType<? extends Gnomo> tipo, LevelAccessor nivel, MobSpawnType razon,
                                        BlockPos posicion, RandomSource azar) {
        int suelo = nivel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, posicion.getX(), posicion.getZ());
        return AparicionMobsFe.esSuelo(nivel.getBlockState(posicion.below())) && posicion.getY() >= suelo - 1;
    }

    @Override
    public boolean removeWhenFarAway(double distancia) {
        return true;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 2;
    }
}
