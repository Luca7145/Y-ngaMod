package com.lucarita.yonga.modulos.fe.mobs;

import com.lucarita.yonga.modulos.fe.ModuloFe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Duende: ser oscuro INFERIOR (ataca del Ateo hacia arriba, ver MobFe). Pequeño, morado, rápido y muy nervioso.
 * Se mantiene a distancia y sopla dardos con su cerbatana. Hay tres, según la máscara que lleve:
 *  0 = veneno (máscara verde), 1 = náusea severa (máscara amarilla), 2 = lentitud (máscara azul).
 * Cada 5 segundos se vuelve invisible durante 3 (y al volver a verse hace una floritura burlona).
 * Aparece en los bosques a cualquier hora.
 */
public class Duende extends PathfinderMob implements MobFe {

    // ---------- AJUSTES ----------
    public static final double VELOCIDAD = 0.36;            // rápido
    private static final double DISTANCIA_CERCA = 6.0, DISTANCIA_LEJOS = 13.0, ALCANCE = 18.0;
    private static final int TICKS_APUNTAR = 12, ESPERA_DISPARO = 35;
    private static final int VISIBLE = 100, INVISIBLE = 60;  // 5 s a la vista, 3 s invisible
    private static final int TICKS_FLORITURA = 12;

    // ---------- ESTADO (sincronizado) ----------
    public static final int NADA = 0, SOPLAR = 1, FLORITURA = 2;
    private static final EntityDataAccessor<Integer> TIPO = SynchedEntityData.defineId(Duende.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACCION = SynchedEntityData.defineId(Duende.class, EntityDataSerializers.INT);

    private int ticksAccion;
    private int espera;
    private int ladoEsquiva = 1;
    private int ticksLado;
    private int fase;                       // reloj del ciclo de invisibilidad

    public int inicioAccionCliente;

    public Duende(EntityType<? extends Duende> tipo, Level nivel) {
        super(tipo, nivel);
        this.xpReward = 6;
        this.fase = this.random.nextInt(VISIBLE);   // que no se vuelvan invisibles todos a la vez
    }

    public static AttributeSupplier.Builder atributos() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0)
                .add(Attributes.MOVEMENT_SPEED, VELOCIDAD)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    public boolean esOscuro() {
        return true;
    }

    @Override
    public boolean esSuperior() {
        return false;
    }

    public int tipo() {
        return this.entityData.get(TIPO);
    }

    public int accion() {
        return this.entityData.get(ACCION);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TIPO, 0);
        this.entityData.define(ACCION, NADA);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> dato) {
        super.onSyncedDataUpdated(dato);
        if (ACCION.equals(dato)) {
            this.inicioAccionCliente = this.tickCount;
        }
    }

    /** Al aparecer elige su máscara al azar. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor nivel, DifficultyInstance dificultad, MobSpawnType razon,
                                        @Nullable SpawnGroupData datos, @Nullable CompoundTag nbt) {
        this.entityData.set(TIPO, this.random.nextInt(3));
        return super.finalizeSpawn(nivel, dificultad, razon, datos, nbt);
    }

    // ---------- IA ----------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8) {
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
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide && !this.isInvisible() && this.random.nextInt(6) == 0) {
            this.level().addParticle(new DustParticleOptions(new Vector3f(0.6f, 0.3f, 0.9f), 0.6f),
                    this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 0, 0, 0);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        cicloInvisible();
        if (espera > 0) espera--;

        LivingEntity objetivo = this.getTarget();
        if (objetivo != null && (!objetivo.isAlive() || (objetivo instanceof Player p && (p.isCreative() || p.isSpectator()))
                || (objetivo instanceof Player && !atacaA(objetivo) && objetivo != this.getLastHurtByMob()))) {
            this.setTarget(null);
            objetivo = null;
        }
        ticksAccion++;
        int accion = accion();
        if (accion == FLORITURA && ticksAccion >= TICKS_FLORITURA) {
            terminarAccion();
        }
        if (objetivo == null) {
            if (accion == SOPLAR) terminarAccion();
            return;
        }

        double distancia = this.distanceTo(objetivo);
        this.getLookControl().setLookAt(objetivo, 40.0f, 40.0f);
        if (accion == SOPLAR) {
            mirarHacia(objetivo);
            if (ticksAccion == TICKS_APUNTAR) {
                soplar(objetivo);
            }
            if (ticksAccion >= TICKS_APUNTAR + 6) {
                terminarAccion();
                espera = ESPERA_DISPARO;
            }
            moverse(objetivo, distancia, true);
            return;
        }
        if (espera == 0 && accion == NADA && distancia < ALCANCE && this.getSensing().hasLineOfSight(objetivo)) {
            empezarAccion(SOPLAR);
        }
        moverse(objetivo, distancia, false);
    }

    /** 5 s a la vista, 3 s invisible, una y otra vez. */
    private void cicloInvisible() {
        fase = (fase + 1) % (VISIBLE + INVISIBLE);
        boolean debeEstarInvisible = fase >= VISIBLE;
        if (debeEstarInvisible != this.isInvisible()) {
            this.setInvisible(debeEstarInvisible);
            if (this.level() instanceof ServerLevel nivel) {
                nivel.sendParticles(new DustParticleOptions(new Vector3f(0.55f, 0.2f, 0.85f), 1.2f),
                        this.getX(), this.getY() + 0.5, this.getZ(), 18, 0.3, 0.4, 0.3, 0.02);
                nivel.sendParticles(ParticleTypes.WITCH, this.getX(), this.getY() + 0.6, this.getZ(), 6, 0.2, 0.3, 0.2, 0.05);
            }
            this.playSound(ModuloFe.SONIDO_DUENDE_PUF.get(), 0.8f, debeEstarInvisible ? 1.3f : 1.0f);
            if (!debeEstarInvisible && accion() == NADA) {
                empezarAccion(FLORITURA);
                this.playSound(ModuloFe.SONIDO_DUENDE_RISA.get(), 1.0f, 1.0f + this.random.nextFloat() * 0.2f);
            }
        }
    }

    /** Lejos: se acerca. Cerca: huye. En medio: va de lado a lado. */
    private void moverse(LivingEntity objetivo, double distancia, boolean apuntando) {
        double velocidad = apuntando ? 0.6 : 1.3;
        if (distancia > DISTANCIA_LEJOS) {
            if (this.tickCount % 8 == 0 || this.getNavigation().isDone()) {
                this.getNavigation().moveTo(objetivo, velocidad);
            }
        } else if (distancia < DISTANCIA_CERCA) {
            Vec3 lejos = this.position().subtract(objetivo.position()).multiply(1, 0, 1).normalize().scale(7.0);
            Vec3 destino = this.position().add(lejos);
            if (this.tickCount % 8 == 0 || this.getNavigation().isDone()) {
                int y = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(destino.x), Mth.floor(destino.z));
                this.getNavigation().moveTo(destino.x, y, destino.z, velocidad * 1.2);
            }
        } else {
            this.getNavigation().stop();
            if (--ticksLado <= 0) {
                ticksLado = 15 + this.random.nextInt(30);
                ladoEsquiva = -ladoEsquiva;
            }
            this.getMoveControl().strafe(0.0f, ladoEsquiva * (apuntando ? 0.3f : 0.7f));
            mirarHacia(objetivo);
        }
    }

    private void soplar(LivingEntity objetivo) {
        Dardo dardo = new Dardo(this.level(), this, tipo());
        double dx = objetivo.getX() - this.getX();
        double dy = objetivo.getY(0.4) - dardo.getY();
        double dz = objetivo.getZ() - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        dardo.shoot(dx, dy + horizontal * 0.12, dz, 1.6f, 3.0f);
        this.level().addFreshEntity(dardo);
        this.playSound(ModuloFe.SONIDO_DUENDE_SOPLIDO.get(), 1.0f, 0.9f + this.random.nextFloat() * 0.3f);
    }

    private void empezarAccion(int accion) {
        this.entityData.set(ACCION, accion);
        this.ticksAccion = 0;
    }

    private void terminarAccion() {
        this.entityData.set(ACCION, NADA);
        this.ticksAccion = 0;
    }

    private void mirarHacia(LivingEntity objetivo) {
        float giro = (float) (Mth.atan2(objetivo.getZ() - this.getZ(), objetivo.getX() - this.getX()) * Mth.RAD_TO_DEG) - 90.0f;
        this.setYRot(Mth.rotLerp(0.5f, this.getYRot(), giro));
        this.setYBodyRot(this.getYRot());
    }

    // ---------- SONIDOS ----------

    @Override
    protected SoundEvent getAmbientSound() {
        return ModuloFe.SONIDO_DUENDE_RISA.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource fuente) {
        return ModuloFe.SONIDO_DUENDE_HERIDO.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModuloFe.SONIDO_DUENDE_MUERTE.get();
    }

    @Override
    protected void playStepSound(BlockPos posicion, BlockState bloque) {
        // pasos ligeros: casi no se le oye
        super.playStepSound(posicion, bloque);
    }

    @Override
    protected float getSoundVolume() {
        return 0.8f;
    }

    // ---------- APARICIÓN ----------

    /** Sobre tierra o hierba, en la superficie del bosque, a cualquier hora. */
    public static boolean puedeAparecer(EntityType<? extends Duende> tipo, LevelAccessor nivel, MobSpawnType razon,
                                        BlockPos posicion, RandomSource azar) {
        int suelo = nivel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, posicion.getX(), posicion.getZ());
        return nivel.getBlockState(posicion.below()).is(BlockTags.DIRT) && posicion.getY() >= suelo - 1;
    }

    @Override
    public boolean removeWhenFarAway(double distancia) {
        return true;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 2;
    }

    // ---------- GUARDAR ----------

    @Override
    public void addAdditionalSaveData(CompoundTag datos) {
        super.addAdditionalSaveData(datos);
        datos.putInt("Mascara", tipo());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag datos) {
        super.readAdditionalSaveData(datos);
        this.entityData.set(TIPO, Mth.clamp(datos.getInt("Mascara"), 0, 2));
    }
}
