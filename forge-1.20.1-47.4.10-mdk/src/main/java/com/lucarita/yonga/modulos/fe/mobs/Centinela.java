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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Centinela de piedra: ser luminoso SUPERIOR (ataca a todos los que no lleguen a Prelado, ver MobFe).
 * Un gólem de arenisca con una calabaza tallada por cabeza. Duerme como estatua en el desierto
 * (EstatuaCentinela) y despierta cuando alguien se acerca.
 *
 *  - RAYO DE LUZ: los ojos se encienden y apunta (aviso, se ve la línea del rayo). Medio segundo antes deja de
 *    seguirte y dispara: daño alto y ceguera. Se puede esquivar apartándose a tiempo.
 *    Después del rayo se queda AGOTADO 3,5 s (echando humo, quieto, sin girarse ni pisotear): es el momento de
 *    rodearlo y castigarle la ESPALDA, que entonces recibe un 50% más de daño. Por delante sigue sin hacerle nada.
 *  - PISOTÓN si te pegas a él (como el de la enana madre).
 *  - DURO COMO LA PIEDRA: por delante no le hace nada ningún golpe ni flecha (rebota como en un escudo).
 *    Su punto débil es la ESPALDA: por detrás recibe todo el daño. Gira el cuerpo muy despacio, así que se le
 *    puede rodear.
 *  - Al morir se queda inmóvil unos segundos, se agrieta, parpadea y EXPLOTA (sin romper bloques).
 */
public class Centinela extends PathfinderMob implements MobFe {

    // ---------- AJUSTES ----------
    public static final double VIDA = 180.0;                // 90 corazones
    private static final float GIRO_MAXIMO = 4.0f;          // grados por tick que gira el cuerpo (lento)
    private static final double ESPALDA = -0.35;            // producto escalar: por debajo de esto, le dan por detrás

    private static final int TICKS_DESPERTAR = 40;

    private static final double ALCANCE_RAYO = 24.0, MINIMO_RAYO = 5.0;
    private static final int TICKS_APUNTAR = 30, FIJAR_ANTES = 10, TICKS_RAYO = 42;
    private static final float DANO_RAYO = 14.0f;           // 7 corazones
    private static final double ANCHO_RAYO = 0.7;
    private static final int CEGUERA_TICKS = 80;
    private static final int ESPERA_RAYO = 70;

    private static final double ALCANCE_PISOTON = 3.5, RADIO_PISOTON = 5.0;
    private static final int TICKS_PISOTON = 26, GOLPE_PISOTON = 16;
    private static final float DANO_PISOTON = 12.0f;        // 6 corazones
    private static final int ESPERA_PISOTON = 60;

    // Tras el rayo se queda agotado: ni se mueve, ni se gira, ni pisotea. Sigue siendo de piedra por delante:
    // solo la espalda, y mientras está agotado la espalda recibe más daño.
    private static final int TICKS_AGOTADO = 70;            // 3,5 s
    private static final float CASTIGO = 1.5f;
    private static final int ESPERA_PISOTON_TRAS_AGOTADO = 30;

    public static final int TICKS_MORIR = 70;               // 3,5 s quieto antes de explotar
    private static final float FUERZA_EXPLOSION = 3.0f;

    // ---------- ESTADO (sincronizado para las animaciones) ----------
    public static final int NADA = 0, DESPERTAR = 1, APUNTAR = 2, PISOTON = 3, AGOTADO = 4;
    private static final EntityDataAccessor<Integer> ACCION = SynchedEntityData.defineId(Centinela.class, EntityDataSerializers.INT);

    private int ticksAccion;
    private int esperaRayo, esperaPisoton;
    private Vec3 apunte = Vec3.ZERO;

    public int inicioAccionCliente;

    public Centinela(EntityType<? extends Centinela> tipo, Level nivel) {
        super(tipo, nivel);
        this.setMaxUpStep(1.0f);
        this.xpReward = 40;
        // Gira despacio también al andar
        this.moveControl = new MoveControl(this) {
            @Override
            protected float rotlerp(float desde, float hasta, float maximo) {
                return super.rotlerp(desde, hasta, GIRO_MAXIMO);
            }
        };
    }

    public static AttributeSupplier.Builder atributos() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, VIDA)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, DANO_PISOTON)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public boolean esOscuro() {
        return false;
    }

    @Override
    public boolean esSuperior() {
        return true;
    }

    public int accion() {
        return this.entityData.get(ACCION);
    }

    /** Lo llama la estatua al despertarlo. */
    public void despertar() {
        empezar(DESPERTAR);
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

    // El cuerpo no sigue a la cabeza: lo giramos nosotros, despacio
    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
            }
        };
    }

    @Override
    public int getMaxHeadYRot() {
        return 25;
    }

    // ---------- IA ----------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false, this::atacaA));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.yBodyRot = this.getYRot();
        this.yBodyRotO = this.yRotO;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (esperaRayo > 0) esperaRayo--;
        if (esperaPisoton > 0) esperaPisoton--;
        ticksAccion++;
        int accion = accion();

        if (accion == DESPERTAR) {
            this.getNavigation().stop();
            if (ticksAccion % 4 == 0 && this.level() instanceof ServerLevel nivel) {
                nivel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SANDSTONE.defaultBlockState()),
                        this.getX(), this.getY() + 1.5, this.getZ(), 10, 0.6, 1.0, 0.6, 0.1);
            }
            if (ticksAccion >= TICKS_DESPERTAR) terminar();
            return;
        }

        LivingEntity objetivo = this.getTarget();
        if (objetivo != null && (!objetivo.isAlive() || (objetivo instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            this.setTarget(null);
            objetivo = null;
        }
        if (objetivo == null) {
            if (accion != NADA) terminar();
            this.getNavigation().stop();
            return;
        }
        double distancia = this.distanceTo(objetivo);

        switch (accion) {
            case APUNTAR -> rayo(objetivo);
            case PISOTON -> pisotear();
            case AGOTADO -> {
                this.getNavigation().stop();
                if (ticksAccion % 4 == 0 && this.level() instanceof ServerLevel nivel) {
                    Vec3 cara = this.getEyePosition().add(Vec3.directionFromRotation(0.0f, this.yBodyRot).scale(0.5));
                    nivel.sendParticles(ParticleTypes.LARGE_SMOKE, cara.x, cara.y, cara.z, 2, 0.2, 0.1, 0.2, 0.01);
                }
                if (ticksAccion >= TICKS_AGOTADO) {
                    terminar();
                    esperaPisoton = Math.max(esperaPisoton, ESPERA_PISOTON_TRAS_AGOTADO);
                }
            }
            default -> {
                girarHacia(objetivo);
                this.getLookControl().setLookAt(objetivo, 10.0f, 20.0f);
                if (distancia < ALCANCE_PISOTON && esperaPisoton == 0) {
                    esperaPisoton = ESPERA_PISOTON;
                    empezar(PISOTON);
                    this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 2.0f, 0.5f);
                } else if (distancia > MINIMO_RAYO && distancia < ALCANCE_RAYO && esperaRayo == 0
                        && this.getSensing().hasLineOfSight(objetivo)) {
                    empezar(APUNTAR);
                    this.playSound(ModuloFe.SONIDO_CENTINELA_CARGA.get(), 2.0f, 1.0f);
                } else if (this.tickCount % 10 == 0 || this.getNavigation().isDone()) {
                    this.getNavigation().moveTo(objetivo, 1.0);
                }
            }
        }
    }

    /** Apunta con los ojos (se ve la línea), deja de seguirte al final y dispara. */
    private void rayo(LivingEntity objetivo) {
        this.getNavigation().stop();
        Vec3 ojos = this.getEyePosition();
        if (ticksAccion <= TICKS_APUNTAR - FIJAR_ANTES) {
            girarHacia(objetivo);
            this.getLookControl().setLookAt(objetivo, 10.0f, 20.0f);
            apunte = objetivo.getEyePosition().subtract(ojos).normalize();
        }
        ServerLevel nivel = this.level() instanceof ServerLevel n ? n : null;
        if (nivel != null && ticksAccion < TICKS_APUNTAR && ticksAccion % 2 == 0) {
            // la línea de aviso: fina al principio, más densa al final
            int paso = ticksAccion > TICKS_APUNTAR - FIJAR_ANTES ? 1 : 3;
            for (int i = 2; i < ALCANCE_RAYO; i += paso) {
                Vec3 p = ojos.add(apunte.scale(i));
                nivel.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }
        if (ticksAccion == TICKS_APUNTAR) {
            disparar(ojos, nivel);
        }
        if (ticksAccion >= TICKS_RAYO) {
            empezar(AGOTADO);
            esperaRayo = ESPERA_RAYO;
        }
    }

    private void disparar(Vec3 ojos, ServerLevel nivel) {
        Vec3 fin = ojos.add(apunte.scale(ALCANCE_RAYO));
        // el rayo se para en la primera pared
        var choque = this.level().clip(new net.minecraft.world.level.ClipContext(ojos, fin,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        fin = choque.getLocation();
        double largo = fin.distanceTo(ojos);
        AABB zona = new AABB(ojos, fin).inflate(ANCHO_RAYO);
        for (LivingEntity vivo : this.level().getEntitiesOfClass(LivingEntity.class, zona)) {
            if (vivo == this || vivo instanceof MobFe) continue;
            if (!(vivo == this.getTarget() || (vivo instanceof Player && atacaA(vivo)))) continue;
            if (vivo.getBoundingBox().inflate(ANCHO_RAYO).clip(ojos, fin).isPresent()
                    && vivo.hurt(this.damageSources().indirectMagic(this, this), DANO_RAYO)) {
                vivo.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, CEGUERA_TICKS, 0), this);
                vivo.setSecondsOnFire(3);
            }
        }
        if (nivel != null) {
            for (double i = 1; i < largo; i += 0.4) {
                Vec3 p = ojos.add(apunte.scale(i));
                nivel.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.02);
                if (((int) (i * 2.5)) % 3 == 0) {
                    nivel.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.05);
                }
            }
            nivel.sendParticles(ParticleTypes.FLASH, fin.x, fin.y, fin.z, 1, 0, 0, 0, 0);
        }
        this.playSound(ModuloFe.SONIDO_CENTINELA_RAYO.get(), 3.0f, 1.0f);
    }

    /** Se alza y cae: golpea a todo lo atacable alrededor. */
    private void pisotear() {
        this.getNavigation().stop();
        if (ticksAccion == GOLPE_PISOTON && this.level() instanceof ServerLevel nivel) {
            for (LivingEntity vivo : this.level().getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(RADIO_PISOTON, 1.0, RADIO_PISOTON))) {
                if (vivo == this || vivo instanceof MobFe || !(vivo == this.getTarget() || (vivo instanceof Player && atacaA(vivo)))) {
                    continue;
                }
                if (vivo.hurt(this.damageSources().mobAttack(this), DANO_PISOTON)) {
                    Vec3 hacia = vivo.position().subtract(this.position()).multiply(1, 0, 1).normalize();
                    vivo.knockback(2.2, -hacia.x, -hacia.z);
                    vivo.setDeltaMovement(vivo.getDeltaMovement().add(0.0, 0.5, 0.0));
                    vivo.hurtMarked = true;
                    if (vivo instanceof ServerPlayer jugador) {
                        jugador.connection.send(new ClientboundSetEntityMotionPacket(jugador));
                    }
                }
            }
            BlockState suelo = this.level().getBlockState(this.blockPosition().below());
            nivel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, suelo), this.getX(), this.getY() + 0.1, this.getZ(),
                    60, RADIO_PISOTON * 0.5, 0.1, RADIO_PISOTON * 0.5, 0.2);
            nivel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.3, this.getZ(), 2, 1.0, 0.0, 1.0, 0.0);
            this.playSound(SoundEvents.GENERIC_EXPLODE, 1.0f, 0.6f);
        }
        if (ticksAccion >= TICKS_PISOTON) terminar();
    }

    /** El cuerpo gira hacia el objetivo, pero despacio (así se le puede rodear). */
    private void girarHacia(LivingEntity objetivo) {
        float giro = (float) (Mth.atan2(objetivo.getZ() - this.getZ(), objetivo.getX() - this.getX()) * Mth.RAD_TO_DEG) - 90.0f;
        this.setYRot(Mth.approachDegrees(this.getYRot(), giro, GIRO_MAXIMO));
    }

    private void empezar(int accion) {
        this.entityData.set(ACCION, accion);
        this.ticksAccion = 0;
    }

    private void terminar() {
        this.entityData.set(ACCION, NADA);
        this.ticksAccion = 0;
    }

    // ---------- DEFENSA: piedra por delante, la espalda es su punto débil ----------

    @Override
    public boolean hurt(DamageSource fuente, float cantidad) {
        if (this.level().isClientSide || fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(fuente, cantidad);
        }
        if (accion() == DESPERTAR || this.isDeadOrDying()) {
            return false;
        }
        Vec3 desde = fuente.getSourcePosition();
        if (desde == null && fuente.getEntity() != null) {
            desde = fuente.getEntity().position();
        }
        if (desde != null) {
            Vec3 adelante = Vec3.directionFromRotation(0.0f, this.yBodyRot);
            Vec3 hacia = desde.subtract(this.position()).multiply(1, 0, 1).normalize();
            if (adelante.dot(hacia) > ESPALDA) {
                // por delante o de lado: rebota
                this.playSound(SoundEvents.SHIELD_BLOCK, 1.0f, 0.6f);
                this.playSound(SoundEvents.STONE_HIT, 1.2f, 0.7f);
                if (fuente.getEntity() instanceof LivingEntity atacante && this.getTarget() == null
                        && !(atacante instanceof Player p && p.isCreative())) {
                    this.setTarget(atacante);
                }
                return false;
            }
            // ¡por la espalda! (y más aún si está agotado tras el rayo)
            this.playSound(SoundEvents.STONE_BREAK, 1.5f, 0.6f);
            if (accion() == AGOTADO) {
                cantidad *= CASTIGO;
            }
        }
        return super.hurt(fuente, cantidad);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    // ---------- LA MUERTE: quieto, se agrieta, parpadea y explota ----------

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
        if (this.level() instanceof ServerLevel nivel) {
            if (this.deathTime % 5 == 0) {
                nivel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SANDSTONE.defaultBlockState()),
                        this.getX(), this.getY() + 1.4, this.getZ(), 4 + this.deathTime / 6, 0.6, 1.0, 0.6, 0.05);
                this.playSound(SoundEvents.STONE_HIT, 1.0f, 0.5f + this.deathTime / (float) TICKS_MORIR);
            }
            if (this.deathTime >= TICKS_MORIR && !this.isRemoved()) {
                nivel.explode(this, this.getX(), this.getY() + 1.2, this.getZ(), FUERZA_EXPLOSION, Level.ExplosionInteraction.NONE);
                nivel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SANDSTONE.defaultBlockState()),
                        this.getX(), this.getY() + 1.4, this.getZ(), 120, 0.9, 1.3, 0.9, 0.3);
                this.remove(RemovalReason.KILLED);
            }
        }
    }

    // ---------- SONIDOS ----------

    @Override
    protected SoundEvent getAmbientSound() {
        return null;   // una estatua no hace ruido
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource fuente) {
        return SoundEvents.STONE_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModuloFe.SONIDO_CENTINELA_DESPERTAR.get();
    }

    @Override
    protected void playStepSound(BlockPos posicion, BlockState bloque) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.2f, 0.6f);
    }

    @Override
    public boolean removeWhenFarAway(double distancia) {
        return false;   // es un guardián: se queda donde despertó
    }
}
