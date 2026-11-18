package com.lucarita.yonga.modulos.fe.mobs;

import com.lucarita.yonga.modulos.fe.ModuloFe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityMountEvent;

/**
 * Gárgola: ser oscuro INFERIOR (ataca del Ateo hacia arriba, ver MobFe). No muy grande, de piedra, con alas.
 *
 *  - DE DÍA va por el suelo: lenta y torpe, fácil de matar. Ataca con zarpazos y mordiscos.
 *  - DE NOCHE vuela. Sin presa, busca un saliente (el alero de un tejado, una cornisa) y se cuelga boca abajo.
 *    Con presa, la rodea en círculos desde el cielo, como un buitre. Al rato baja en picado, la AGARRA y sube
 *    en vertical: no la suelta hasta llegar al techo del mundo... salvo que la maten mientras la lleva.
 * Aparece de noche en montañas y en zonas con pueblos (llanuras, bosques, taigas), donde se cuelga de los tejados.
 */
public class Gargola extends Monster implements MobFe {

    // ---------- AJUSTES ----------
    private static final double VELOCIDAD_SUELO = 0.17;     // lenta y torpe
    private static final float DANO_ZARPAZO = 4.0f, DANO_MORDISCO = 5.0f;
    private static final int TICKS_ATAQUE = 14, GOLPE = 6, ESPERA_ATAQUE = 20;

    private static final double RADIO_CIRCULO = 8.0, ALTURA_CIRCULO = 10.0;
    private static final double VELOCIDAD_VUELO = 0.45, VELOCIDAD_PICADO = 0.95;
    private static final int RODEAR_MIN = 100, RODEAR_MAX = 180;     // 5 a 9 s rodeando antes de bajar
    private static final int PICADO_MAX = 60;
    private static final double VELOCIDAD_SUBIDA = 0.6;              // bloques por tick con la presa
    private static final double BUSCAR_SALIENTE = 10.0;

    // ---------- ESTADO (sincronizado) ----------
    public static final int NADA = 0, ZARPAZO = 1, MORDISCO = 2, RODEAR = 3, PICADO = 4, LLEVAR = 5;
    private static final EntityDataAccessor<Integer> ACCION = SynchedEntityData.defineId(Gargola.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> VOLANDO = SynchedEntityData.defineId(Gargola.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> COLGADA = SynchedEntityData.defineId(Gargola.class, EntityDataSerializers.BOOLEAN);

    private int ticksAccion;
    private int espera;
    private int duracionRodeo;
    private double anguloRodeo;
    private BlockPos saliente;

    public int inicioAccionCliente;

    public Gargola(EntityType<? extends Gargola> tipo, Level nivel) {
        super(tipo, nivel);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder atributos() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, VELOCIDAD_SUELO)
                .add(Attributes.ATTACK_DAMAGE, DANO_MORDISCO)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public boolean esOscuro() {
        return true;
    }

    @Override
    public boolean esSuperior() {
        return false;
    }

    public int accion() {
        return this.entityData.get(ACCION);
    }

    public boolean volando() {
        return this.entityData.get(VOLANDO);
    }

    public boolean colgada() {
        return this.entityData.get(COLGADA);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ACCION, NADA);
        this.entityData.define(VOLANDO, false);
        this.entityData.define(COLGADA, false);
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
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return getTarget() == null && !volando() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return getTarget() == null && !volando() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false, this::atacaA));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (espera > 0) espera--;
        ticksAccion++;
        boolean noche = this.level().isNight();

        // Si lleva a alguien, sube pase lo que pase
        if (accion() == LLEVAR) {
            llevar();
            return;
        }

        LivingEntity objetivo = this.getTarget();
        if (objetivo != null && (!objetivo.isAlive() || (objetivo instanceof Player p && (p.isCreative() || p.isSpectator()))
                || (objetivo instanceof Player && !atacaA(objetivo) && objetivo != this.getLastHurtByMob()))) {
            this.setTarget(null);
            objetivo = null;
        }

        if (noche) {
            noche(objetivo);
        } else {
            dia(objetivo);
        }
    }

    // ---------- DE DÍA: por el suelo, torpe ----------

    private void dia(LivingEntity objetivo) {
        if (colgada()) this.entityData.set(COLGADA, false);
        ponerseAVolar(false);
        int accion = accion();
        if (accion == RODEAR || accion == PICADO) terminar();
        if (objetivo == null) {
            if (accion != NADA) terminar();
            return;
        }
        double distancia = this.distanceTo(objetivo);
        this.getLookControl().setLookAt(objetivo, 30.0f, 30.0f);
        if (accion == ZARPAZO || accion == MORDISCO) {
            this.getNavigation().stop();
            if (ticksAccion == GOLPE && distancia < 2.4) {
                objetivo.hurt(this.damageSources().mobAttack(this), accion == ZARPAZO ? DANO_ZARPAZO : DANO_MORDISCO);
                this.playSound(accion == ZARPAZO ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.FOX_BITE, 0.8f, 0.6f);
            }
            if (ticksAccion >= TICKS_ATAQUE) {
                terminar();
                espera = ESPERA_ATAQUE;
            }
            return;
        }
        if (distancia < 2.0 && espera == 0) {
            empezar(this.random.nextBoolean() ? ZARPAZO : MORDISCO);
        } else if (this.tickCount % 10 == 0 || this.getNavigation().isDone()) {
            this.getNavigation().moveTo(objetivo, 1.0);
        }
    }

    // ---------- DE NOCHE: vuela ----------

    private void noche(LivingEntity objetivo) {
        if (objetivo == null) {
            if (accion() != NADA) terminar();
            colgarse();
            return;
        }
        if (colgada()) {
            this.entityData.set(COLGADA, false);   // ¡despierta!
            this.playSound(ModuloFe.SONIDO_GARGOLA_CHILLIDO.get(), 1.5f, 1.0f);
        }
        ponerseAVolar(true);
        this.getNavigation().stop();
        int accion = accion();
        if (accion != RODEAR && accion != PICADO) {
            empezar(RODEAR);
            duracionRodeo = RODEAR_MIN + this.random.nextInt(RODEAR_MAX - RODEAR_MIN);
            anguloRodeo = Math.atan2(this.getZ() - objetivo.getZ(), this.getX() - objetivo.getX());
            accion = RODEAR;
        }
        if (accion == RODEAR) {
            // en círculos sobre su presa, como un buitre
            anguloRodeo += 0.045;
            Vec3 punto = objetivo.position().add(Math.cos(anguloRodeo) * RADIO_CIRCULO, ALTURA_CIRCULO + Math.sin(this.tickCount * 0.05) * 1.5,
                    Math.sin(anguloRodeo) * RADIO_CIRCULO);
            volarHacia(punto, VELOCIDAD_VUELO, 0.08);
            if (ticksAccion % 40 == 0) {
                this.playSound(ModuloFe.SONIDO_GARGOLA_CHILLIDO.get(), 1.0f, 0.8f + this.random.nextFloat() * 0.3f);
            }
            if (ticksAccion >= duracionRodeo) {
                empezar(PICADO);
                this.playSound(SoundEvents.PHANTOM_SWOOP, 2.0f, 0.7f);
            }
        } else {
            // ¡en picado!
            volarHacia(objetivo.position().add(0, objetivo.getBbHeight() * 0.6, 0), VELOCIDAD_PICADO, 0.25);
            if (objetivo instanceof Player && this.getBoundingBox().inflate(0.4).intersects(objetivo.getBoundingBox())) {
                if (objetivo.startRiding(this, true)) {
                    empezar(LLEVAR);
                    this.playSound(ModuloFe.SONIDO_GARGOLA_CHILLIDO.get(), 2.0f, 1.2f);
                    objetivo.hurt(this.damageSources().mobAttack(this), DANO_ZARPAZO);
                }
            } else if (ticksAccion >= PICADO_MAX || this.horizontalCollision) {
                empezar(RODEAR);
                duracionRodeo = RODEAR_MIN / 2 + this.random.nextInt(RODEAR_MIN);
            }
        }
    }

    /** Sube en vertical con su presa hasta el techo del mundo y allí la suelta. */
    private void llevar() {
        ponerseAVolar(true);
        Entity presa = this.getFirstPassenger();
        if (presa == null) {
            empezar(RODEAR);
            return;
        }
        this.setDeltaMovement(this.getDeltaMovement().x * 0.5, VELOCIDAD_SUBIDA, this.getDeltaMovement().z * 0.5);
        if (ticksAccion % 6 == 0) {
            this.playSound(SoundEvents.PHANTOM_FLAP, 1.5f, 0.6f);
        }
        if (this.getY() >= this.level().getMaxBuildHeight() - 2 || (this.verticalCollision && ticksAccion > 10)) {
            this.ejectPassengers();
            this.playSound(ModuloFe.SONIDO_GARGOLA_CHILLIDO.get(), 2.0f, 1.4f);
            empezar(RODEAR);
            duracionRodeo = RODEAR_MAX;
        }
    }

    /** Sin presa por la noche: busca un saliente cerca (bloque con aire debajo) y se cuelga boca abajo. */
    private void colgarse() {
        if (colgada()) {
            this.setDeltaMovement(Vec3.ZERO);
            if (saliente == null || !puedeColgarse(saliente)) {
                this.entityData.set(COLGADA, false);
                saliente = null;
            }
            return;
        }
        ponerseAVolar(true);
        if (saliente == null || !puedeColgarse(saliente)) {
            saliente = null;
            if (this.tickCount % 20 == 0) {
                for (int i = 0; i < 20 && saliente == null; i++) {
                    BlockPos p = BlockPos.containing(this.getX() + (this.random.nextDouble() - 0.5) * 2 * BUSCAR_SALIENTE,
                            this.getY() + this.random.nextInt(8), this.getZ() + (this.random.nextDouble() - 0.5) * 2 * BUSCAR_SALIENTE);
                    if (puedeColgarse(p)) saliente = p;
                }
            }
            // mientras busca, revolotea por la zona
            if (saliente == null) {
                Vec3 paseo = this.position().add(Math.cos(this.tickCount * 0.03) * 3, Math.sin(this.tickCount * 0.02), Math.sin(this.tickCount * 0.03) * 3);
                volarHacia(paseo, 0.2, 0.05);
            }
            return;
        }
        Vec3 sitio = new Vec3(saliente.getX() + 0.5, saliente.getY() + 1.0 - this.getBbHeight(), saliente.getZ() + 0.5);
        volarHacia(sitio, 0.3, 0.1);
        if (this.position().distanceTo(sitio) < 0.4) {
            this.setPos(sitio.x, sitio.y, sitio.z);
            this.setDeltaMovement(Vec3.ZERO);
            this.entityData.set(COLGADA, true);
        }
    }

    /** Hay sitio para colgarse en p si el bloque de encima es sólido por debajo y hay aire para el cuerpo. */
    private boolean puedeColgarse(BlockPos p) {
        BlockPos arriba = p.above();
        return this.level().getBlockState(arriba).isFaceSturdy(this.level(), arriba, Direction.DOWN)
                && this.level().getBlockState(p).isAir() && this.level().getBlockState(p.below()).isAir();
    }

    private void volarHacia(Vec3 punto, double velocidadMax, double aceleracion) {
        Vec3 hacia = punto.subtract(this.position());
        Vec3 deseada = hacia.lengthSqr() < 1.0E-4 ? Vec3.ZERO : hacia.normalize().scale(Math.min(velocidadMax, hacia.length() * 0.35));
        Vec3 v = this.getDeltaMovement();
        v = v.add(deseada.subtract(v).scale(aceleracion));
        this.setDeltaMovement(v);
        if (v.horizontalDistanceSqr() > 0.002) {
            float giro = (float) (Mth.atan2(v.z, v.x) * Mth.RAD_TO_DEG) - 90.0f;
            this.setYRot(Mth.rotLerp(0.3f, this.getYRot(), giro));
            this.setYBodyRot(this.getYRot());
        }
    }

    private void ponerseAVolar(boolean volar) {
        if (volando() != volar) {
            this.entityData.set(VOLANDO, volar);
        }
        this.setNoGravity(volar);
    }

    private void empezar(int accion) {
        this.entityData.set(ACCION, accion);
        this.ticksAccion = 0;
    }

    private void terminar() {
        this.entityData.set(ACCION, NADA);
        this.ticksAccion = 0;
    }

    // ---------- LA PRESA: colgando debajo, sin poder soltarse ----------

    @Override
    protected void positionRider(Entity pasajero, MoveFunction mover) {
        mover.accept(pasajero, this.getX(), this.getY() - pasajero.getBbHeight() + 0.3, this.getZ());
    }

    @Override
    protected boolean canAddPassenger(Entity pasajero) {
        return this.getPassengers().isEmpty() && pasajero instanceof LivingEntity;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    /** Para que la presa pueda pegarle mientras la lleva (si no, Minecraft no deja golpear a tu "montura"). */
    @Override
    public boolean canRiderInteract() {
        return true;
    }

    /** No se puede bajar mientras sube (ni con mayús). Solo en el servidor. Se registra en ModuloFe. */
    public static void alMontarODesmontar(EntityMountEvent evento) {
        if (evento.isDismounting() && !evento.getEntityMounting().level().isClientSide()
                && evento.getEntityBeingMounted() instanceof Gargola gargola
                && gargola.isAlive() && !gargola.isRemoved() && gargola.accion() == LLEVAR
                && gargola.getY() < gargola.level().getMaxBuildHeight() - 2 && !gargola.verticalCollision) {
            evento.setCanceled(true);
        }
    }

    // ---------- VARIOS ----------

    @Override
    public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fuente) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean enSuelo, BlockState bloque, BlockPos posicion) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return colgada() ? null : ModuloFe.SONIDO_GARGOLA_GRUNIDO.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource fuente) {
        return ModuloFe.SONIDO_GARGOLA_CHILLIDO.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModuloFe.SONIDO_GARGOLA_MUERTE.get();
    }

    @Override
    protected void playStepSound(BlockPos posicion, BlockState bloque) {
        this.playSound(SoundEvents.STONE_STEP, 0.6f, 0.7f);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // aleteos
        if (this.level().isClientSide && volando() && !colgada() && this.tickCount % 12 == 0) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.PHANTOM_FLAP,
                    this.getSoundSource(), 0.6f, 0.8f + this.random.nextFloat() * 0.2f, false);
        }
    }

    /** De noche, en la superficie (con cielo encima), sin luz: montañas y alrededor de los pueblos. */
    public static boolean puedeAparecer(EntityType<? extends Gargola> tipo, ServerLevelAccessor nivel, MobSpawnType razon,
                                        BlockPos posicion, RandomSource azar) {
        return nivel.getDifficulty() != Difficulty.PEACEFUL && Monster.isDarkEnoughToSpawn(nivel, posicion, azar)
                && nivel.canSeeSkyFromBelowWater(posicion) && checkMobSpawnRules(tipo, nivel, razon, posicion, azar);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag datos) {
        super.addAdditionalSaveData(datos);
        datos.putBoolean("Volando", volando());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag datos) {
        super.readAdditionalSaveData(datos);
        ponerseAVolar(datos.getBoolean("Volando"));
    }
}
