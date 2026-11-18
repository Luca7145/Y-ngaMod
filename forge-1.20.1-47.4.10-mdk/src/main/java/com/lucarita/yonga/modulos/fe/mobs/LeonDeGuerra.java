package com.lucarita.yonga.modulos.fe.mobs;

import com.lucarita.yonga.modulos.fe.ModuloFe;
import com.lucarita.yonga.modulos.sangrado.Sangrado;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * León de guerra: ser luminoso SUPERIOR (ataca a todos los que no lleguen a Prelado, ver MobFe).
 * Un centauro con cuerpo de león y torso de guerrero con melena. Es rápido. Dos variantes:
 *
 * Los dos tienen la misma vida que el Astado (110 corazones).
 *
 * ARQUERO: se mantiene a distancia prudente (se aleja si te acercas, se acerca si te vas, y si no, se mueve
 * de lado), dispara flechas que dan Lentitud y, si le tienes encima, te da un zarpazo con la pata (sangrado).
 *
 * ESPADACHÍN (tipo miniboss, con ataques que se avisan y se aprenden):
 *  - En guardia se cubre con el escudo: NO se le puede hacer daño (ni cuerpo a cuerpo ni con flechas).
 *  - ESPADAZO: levanta el sable por detrás (aviso largo) y barre todo lo que tiene delante. Quita mucho.
 *  - ZARPAZO: se alza sobre las patas de atrás (aviso corto) y golpea con la pata delantera. Da sangrado.
 *  - Si cualquiera de los dos FALLA, se queda tambaleándose unos segundos con la guardia baja: ahí sí se le
 *    puede pegar, y además recibe más daño (castigo).
 *  - ESCUDAZO: si le pegas al escudo una y otra vez de cerca, te aparta de un golpe de escudo.
 * Los dos rugen al ver a su presa por primera vez.
 */
public class LeonDeGuerra extends PathfinderMob implements MobFe {

    // ---------- AJUSTES ----------
    public static final double VELOCIDAD = 0.33;            // rápido: un lobo va a 0.3

    // Arquero
    private static final double DISTANCIA_CERCA = 9.0, DISTANCIA_LEJOS = 18.0, ALCANCE_ARCO = 26.0;
    private static final int TICKS_TENSAR = 20, ESPERA_DISPARO = 30;
    private static final double DANO_FLECHA = 4.0;          // ~7 de daño con la velocidad de disparo
    private static final int LENTITUD_TICKS = 100, LENTITUD_NIVEL = 1;   // Lentitud II, 5 s
    private static final float DANO_ZARPAZO_ARQUERO = 10.0f;

    // Espadachín
    private static final double ALCANCE_ATAQUE = 4.0;
    private static final int AVISO_ESPADAZO = 16, AVISO_ZARPAZO = 10;
    private static final float DANO_ESPADAZO = 19.0f;       // 9,5 corazones
    private static final double RADIO_ESPADAZO = 4.8;
    private static final double ARCO_ESPADAZO = -0.1;       // producto escalar mínimo: ~190 grados por delante
    private static final float DANO_ZARPAZO = 13.0f;        // 6,5 corazones
    private static final double ALCANCE_ZARPAZO = 3.6;
    private static final int RECUPERAR_SI_ACIERTA = 10;
    private static final int TAMBALEO = 50;                 // 2,5 s vulnerable si falla
    private static final float CASTIGO = 1.5f;              // daño extra mientras se tambalea
    private static final int ESPERA_ENTRE_ATAQUES = 25;
    private static final int GOLPES_PARA_ESCUDAZO = 3;      // golpes al escudo seguidos (en 3 s)
    private static final float DANO_ESCUDAZO = 5.0f;
    private static final int TICKS_ESCUDAZO = 12;

    private static final int TICKS_RUGIDO = 30;
    private static final Sangrado.Tabla SANGRADO_GARRA = new Sangrado.Tabla(0.0f, 0.4f, 0.6f, 300);   // siempre sangra

    // ---------- ESTADO (sincronizado para las animaciones) ----------
    public static final int NADA = 0, RUGIDO = 1, DISPARAR = 2, ESPADAZO = 3, ZARPAZO = 4, ESCUDAZO = 5, TAMBALEO_ACCION = 6;
    private static final EntityDataAccessor<Integer> ACCION = SynchedEntityData.defineId(LeonDeGuerra.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> EN_GUARDIA = SynchedEntityData.defineId(LeonDeGuerra.class, EntityDataSerializers.BOOLEAN);

    private final boolean arquero;

    // Servidor
    private int ticksAccion;
    private int espera;
    private int esperaZarpazo;
    private int duracionAccion;
    private boolean haRugido;
    private int golpesAlEscudo;
    private long ultimoGolpeAlEscudo;
    private int ladoEsquiva = 1;
    private int ticksLado;

    // Cliente
    public int inicioAccionCliente;

    public LeonDeGuerra(EntityType<? extends LeonDeGuerra> tipo, Level nivel, boolean arquero) {
        super(tipo, nivel);
        this.arquero = arquero;
        this.setMaxUpStep(1.0f);
        this.xpReward = arquero ? 30 : 40;   // los dos tienen la vida del Astado
    }

    public static AttributeSupplier.Builder atributosArquero() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, Astado.VIDA)             // la misma vida que el Astado
                .add(Attributes.MOVEMENT_SPEED, VELOCIDAD)
                .add(Attributes.ATTACK_DAMAGE, DANO_ZARPAZO_ARQUERO)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    public static AttributeSupplier.Builder atributosEspadachin() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, Astado.VIDA)             // la misma vida que el Astado
                .add(Attributes.MOVEMENT_SPEED, VELOCIDAD)
                .add(Attributes.ATTACK_DAMAGE, DANO_ESPADAZO)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ARMOR_TOUGHNESS, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    public boolean esArquero() {
        return arquero;
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

    public boolean enGuardia() {
        return this.entityData.get(EN_GUARDIA);
    }

    /** ¿Se le puede hacer daño ahora? El arquero siempre; el espadachín solo tambaleándose. */
    public boolean vulnerable() {
        return arquero || accion() == TAMBALEO_ACCION;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ACCION, NADA);
        this.entityData.define(EN_GUARDIA, false);
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
        // Pasear solo cuando no está cazando (si caza, lo mueve customServerAiStep)
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
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10.0f));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::atacaA));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (espera > 0) espera--;
        if (esperaZarpazo > 0) esperaZarpazo--;

        LivingEntity objetivo = this.getTarget();
        if (objetivo != null && (!objetivo.isAlive() || (objetivo instanceof Player p && (p.isCreative() || p.isSpectator()))
                || (objetivo instanceof Player && !atacaA(objetivo) && objetivo != this.getLastHurtByMob()))) {
            this.setTarget(null);
            objetivo = null;
        }
        this.entityData.set(EN_GUARDIA, !arquero && objetivo != null && accion() != TAMBALEO_ACCION);

        if (objetivo == null) {
            if (accion() != NADA) terminarAccion();
            haRugido = false;   // volverá a rugir cuando encuentre otra presa
            return;
        }
        ticksAccion++;
        if (!haRugido) {
            haRugido = true;
            empezarAccion(RUGIDO, TICKS_RUGIDO);
            this.playSound(ModuloFe.SONIDO_LEON_RUGIDO.get(), 3.0f, arquero ? 1.1f : 0.9f);
        }

        if (accion() == RUGIDO) {
            this.getNavigation().stop();
            this.getLookControl().setLookAt(objetivo, 30.0f, 30.0f);
            if (ticksAccion >= duracionAccion) terminarAccion();
            return;
        }
        if (arquero) {
            pensarArquero(objetivo);
        } else {
            pensarEspadachin(objetivo);
        }
    }

    // ---------- ARQUERO ----------

    private void pensarArquero(LivingEntity objetivo) {
        double distancia = this.distanceTo(objetivo);
        this.getLookControl().setLookAt(objetivo, 30.0f, 30.0f);
        int accion = accion();

        if (accion == ZARPAZO) {
            this.getNavigation().stop();
            mirarHacia(objetivo);
            if (ticksAccion == 9 && distancia < ALCANCE_ZARPAZO && delante(objetivo, 0.4)) {
                if (objetivo.hurt(this.damageSources().mobAttack(this), DANO_ZARPAZO_ARQUERO)) {
                    Sangrado.intentarAplicar(objetivo, this.random, SANGRADO_GARRA);
                }
                this.playSound(ModuloFe.SONIDO_LEON_TAJO.get(), 1.0f, 1.3f);
            }
            if (ticksAccion >= 16) terminarAccion();
            return;
        }
        if (accion == DISPARAR) {
            mirarHacia(objetivo);
            if (ticksAccion == TICKS_TENSAR) {
                disparar(objetivo);
            }
            if (ticksAccion >= TICKS_TENSAR + 4) {
                terminarAccion();
                espera = ESPERA_DISPARO;
            }
            moverseComoArquero(objetivo, distancia, true);
            return;
        }

        // Te tiene encima: zarpazo
        if (distancia < ALCANCE_ZARPAZO - 0.6 && esperaZarpazo == 0) {
            esperaZarpazo = 50;
            empezarAccion(ZARPAZO, 16);
            this.playSound(ModuloFe.SONIDO_LEON_GRUNIDO.get(), 1.5f, 1.2f);
            return;
        }
        if (espera == 0 && distancia < ALCANCE_ARCO && this.getSensing().hasLineOfSight(objetivo)) {
            empezarAccion(DISPARAR, TICKS_TENSAR + 4);
            this.playSound(SoundEvents.CROSSBOW_LOADING_MIDDLE, 1.0f, 0.8f);
        }
        moverseComoArquero(objetivo, distancia, false);
    }

    /** Lejos de más: se acerca. Cerca de más: se aleja. En medio: se mueve de lado. */
    private void moverseComoArquero(LivingEntity objetivo, double distancia, boolean tensando) {
        double velocidad = tensando ? 0.7 : 1.3;
        if (distancia > DISTANCIA_LEJOS) {
            this.getNavigation().moveTo(objetivo, velocidad);
        } else if (distancia < DISTANCIA_CERCA) {
            Vec3 lejos = this.position().subtract(objetivo.position()).multiply(1, 0, 1).normalize().scale(8.0);
            Vec3 destino = this.position().add(lejos);
            if (this.tickCount % 10 == 0 || this.getNavigation().isDone()) {
                this.getNavigation().moveTo(destino.x, destino.y, destino.z, velocidad * 1.15);
            }
        } else {
            this.getNavigation().stop();
            if (--ticksLado <= 0) {
                ticksLado = 30 + this.random.nextInt(40);
                ladoEsquiva = -ladoEsquiva;
            }
            this.getMoveControl().strafe(0.0f, ladoEsquiva * (tensando ? 0.35f : 0.6f));
            mirarHacia(objetivo);
        }
    }

    private void disparar(LivingEntity objetivo) {
        Arrow flecha = new Arrow(this.level(), this);
        flecha.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, LENTITUD_TICKS, LENTITUD_NIVEL));
        flecha.setBaseDamage(DANO_FLECHA);
        flecha.pickup = AbstractArrow.Pickup.DISALLOWED;
        double dx = objetivo.getX() - this.getX();
        double dy = objetivo.getY(0.3333) - flecha.getY();
        double dz = objetivo.getZ() - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        flecha.shoot(dx, dy + horizontal * 0.2, dz, 1.8f, 2.0f);
        this.level().addFreshEntity(flecha);
        this.playSound(SoundEvents.ARROW_SHOOT, 1.0f, 1.0f / (this.random.nextFloat() * 0.4f + 0.8f));
    }

    // ---------- ESPADACHÍN ----------

    private void pensarEspadachin(LivingEntity objetivo) {
        double distancia = this.distanceTo(objetivo);
        int accion = accion();
        this.getLookControl().setLookAt(objetivo, 30.0f, 30.0f);

        switch (accion) {
            case ESPADAZO -> {
                this.getNavigation().stop();
                if (ticksAccion <= AVISO_ESPADAZO) {
                    mirarHacia(objetivo);
                    if (ticksAccion % 4 == 0 && this.level() instanceof ServerLevel nivel) {
                        // destello en el sable: aviso
                        nivel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 3.0, this.getZ(), 2, 0.3, 0.2, 0.3, 0.01);
                    }
                }
                if (ticksAccion == AVISO_ESPADAZO + 1) {
                    boolean acierta = espadazo();
                    if (acierta) {
                        duracionAccion = ticksAccion + RECUPERAR_SI_ACIERTA;
                    } else {
                        tambalearse();
                        return;
                    }
                }
                if (ticksAccion >= duracionAccion) terminarAtaque();
            }
            case ZARPAZO -> {
                this.getNavigation().stop();
                if (ticksAccion <= AVISO_ZARPAZO) {
                    mirarHacia(objetivo);
                }
                if (ticksAccion == AVISO_ZARPAZO + 1) {
                    // se echa un poco hacia delante con el golpe
                    Vec3 adelante = Vec3.directionFromRotation(0.0f, this.getYRot());
                    this.setDeltaMovement(this.getDeltaMovement().add(adelante.x * 0.45, 0.0, adelante.z * 0.45));
                    boolean acierta = distancia < ALCANCE_ZARPAZO && delante(objetivo, 0.4)
                            && objetivo.hurt(this.damageSources().mobAttack(this), DANO_ZARPAZO);
                    this.playSound(ModuloFe.SONIDO_LEON_TAJO.get(), 1.4f, 1.1f);
                    if (acierta) {
                        Sangrado.intentarAplicar(objetivo, this.random, SANGRADO_GARRA);
                        duracionAccion = ticksAccion + RECUPERAR_SI_ACIERTA;
                    } else {
                        tambalearse();
                        return;
                    }
                }
                if (ticksAccion >= duracionAccion) terminarAtaque();
            }
            case ESCUDAZO -> {
                this.getNavigation().stop();
                mirarHacia(objetivo);
                if (ticksAccion == 5 && distancia < 3.2) {
                    objetivo.hurt(this.damageSources().mobAttack(this), DANO_ESCUDAZO);
                    Vec3 hacia = objetivo.position().subtract(this.position()).multiply(1, 0, 1).normalize();
                    objetivo.knockback(1.8, -hacia.x, -hacia.z);
                    empujar(objetivo);
                    this.playSound(SoundEvents.SHIELD_BLOCK, 1.5f, 0.6f);
                }
                if (ticksAccion >= TICKS_ESCUDAZO) terminarAtaque();
            }
            case TAMBALEO_ACCION -> {
                this.getNavigation().stop();
                if (ticksAccion % 10 == 0 && this.level() instanceof ServerLevel nivel) {
                    nivel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 2.6, this.getZ(), 6, 0.4, 0.1, 0.4, 0.05);
                }
                if (ticksAccion >= TAMBALEO) {
                    terminarAtaque();
                }
            }
            default -> {
                // En guardia: se acerca y elige ataque
                if (distancia > ALCANCE_ATAQUE - 0.6) {
                    if (this.tickCount % 8 == 0 || this.getNavigation().isDone()) {
                        this.getNavigation().moveTo(objetivo, 1.15);
                    }
                } else {
                    this.getNavigation().stop();
                    mirarHacia(objetivo);
                }
                if (golpesAlEscudo >= GOLPES_PARA_ESCUDAZO && distancia < 3.0) {
                    golpesAlEscudo = 0;
                    empezarAccion(ESCUDAZO, TICKS_ESCUDAZO);
                } else if (espera == 0 && distancia < ALCANCE_ATAQUE) {
                    if (this.random.nextFloat() < 0.55f) {
                        empezarAccion(ESPADAZO, AVISO_ESPADAZO + 1 + RECUPERAR_SI_ACIERTA);
                        this.playSound(ModuloFe.SONIDO_LEON_GRUNIDO.get(), 2.0f, 0.8f);
                    } else {
                        empezarAccion(ZARPAZO, AVISO_ZARPAZO + 1 + RECUPERAR_SI_ACIERTA);
                        this.playSound(ModuloFe.SONIDO_LEON_GRUNIDO.get(), 2.0f, 1.3f);
                    }
                }
            }
        }
    }

    /** El barrido: todo lo atacable que tenga delante, en un arco muy amplio. ¿Ha dado a alguien? */
    private boolean espadazo() {
        boolean acierta = false;
        Vec3 adelante = Vec3.directionFromRotation(0.0f, this.getYRot());
        for (LivingEntity vivo : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(RADIO_ESPADAZO))) {
            if (vivo == this || vivo instanceof MobFe || this.distanceTo(vivo) > RADIO_ESPADAZO) {
                continue;
            }
            Vec3 hacia = vivo.position().subtract(this.position()).multiply(1, 0, 1).normalize();
            boolean esEnemigo = vivo == this.getTarget() || (vivo instanceof Player && atacaA(vivo));
            if (esEnemigo && adelante.dot(hacia) > ARCO_ESPADAZO && vivo.hurt(this.damageSources().mobAttack(this), DANO_ESPADAZO)) {
                vivo.knockback(0.8, -hacia.x, -hacia.z);
                empujar(vivo);
                acierta = true;
            }
        }
        if (this.level() instanceof ServerLevel nivel) {
            for (int i = -3; i <= 3; i++) {
                Vec3 punto = Vec3.directionFromRotation(0.0f, this.getYRot() + i * 28).scale(2.6).add(this.position());
                nivel.sendParticles(ParticleTypes.SWEEP_ATTACK, punto.x, this.getY() + 1.4, punto.z, 1, 0, 0, 0, 0);
            }
        }
        this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.7f);
        this.playSound(ModuloFe.SONIDO_LEON_TAJO.get(), 1.5f, 0.85f);
        return acierta;
    }

    private void tambalearse() {
        empezarAccion(TAMBALEO_ACCION, TAMBALEO);
        this.entityData.set(EN_GUARDIA, false);
        this.playSound(ModuloFe.SONIDO_LEON_HERIDO.get(), 1.2f, 0.7f);
    }

    private void terminarAtaque() {
        terminarAccion();
        espera = ESPERA_ENTRE_ATAQUES;
    }

    // ---------- EL ESCUDO ----------

    @Override
    public boolean hurt(DamageSource fuente, float cantidad) {
        if (!this.level().isClientSide && !vulnerable() && enGuardia() && paraConElEscudo(fuente)) {
            this.playSound(SoundEvents.SHIELD_BLOCK, 1.0f, 0.8f + this.random.nextFloat() * 0.4f);
            long ahora = this.level().getGameTime();
            golpesAlEscudo = ahora - ultimoGolpeAlEscudo < 60 ? golpesAlEscudo + 1 : 1;
            ultimoGolpeAlEscudo = ahora;
            if (fuente.getEntity() instanceof LivingEntity atacante && this.getTarget() == null
                    && !(atacante instanceof Player p && p.isCreative())) {
                this.setTarget(atacante);
            }
            return false;
        }
        if (!arquero && accion() == TAMBALEO_ACCION) {
            cantidad *= CASTIGO;
        }
        return super.hurt(fuente, cantidad);
    }

    /** El escudo para golpes y flechas (no el fuego, la caída, el vacío, /kill...). */
    private boolean paraConElEscudo(DamageSource fuente) {
        return fuente.getEntity() != null && !fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && !fuente.is(DamageTypeTags.IS_FIRE) && !fuente.is(DamageTypeTags.IS_EXPLOSION);
    }

    // ---------- UTILIDADES ----------

    private void empezarAccion(int accion, int duracion) {
        this.entityData.set(ACCION, accion);
        this.ticksAccion = 0;
        this.duracionAccion = duracion;
    }

    private void terminarAccion() {
        this.entityData.set(ACCION, NADA);
        this.ticksAccion = 0;
    }

    private void mirarHacia(LivingEntity objetivo) {
        float giro = (float) (Mth.atan2(objetivo.getZ() - this.getZ(), objetivo.getX() - this.getX()) * Mth.RAD_TO_DEG) - 90.0f;
        this.setYRot(Mth.rotLerp(0.4f, this.getYRot(), giro));
        this.setYBodyRot(this.getYRot());
    }

    private boolean delante(LivingEntity objetivo, double minimo) {
        Vec3 adelante = Vec3.directionFromRotation(0.0f, this.getYRot());
        Vec3 hacia = objetivo.position().subtract(this.position()).multiply(1, 0, 1).normalize();
        return adelante.dot(hacia) > minimo;
    }

    private static void empujar(LivingEntity objetivo) {
        objetivo.hurtMarked = true;
        if (objetivo instanceof ServerPlayer jugador) {
            jugador.connection.send(new ClientboundSetEntityMotionPacket(jugador));
        }
    }

    // ---------- SONIDOS ----------

    @Override
    protected SoundEvent getAmbientSound() {
        return ModuloFe.SONIDO_LEON_GRUNIDO.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource fuente) {
        return ModuloFe.SONIDO_LEON_HERIDO.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModuloFe.SONIDO_LEON_MUERTE.get();
    }

    @Override
    protected void playStepSound(BlockPos posicion, BlockState bloque) {
        this.playSound(SoundEvents.WOLF_STEP, 0.4f, 0.6f);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (arquero ? 1.1f : 0.92f);
    }

    // ---------- APARICIÓN ----------

    /** En la superficie (sobre hierba, tierra o arena, con cielo encima), a cualquier hora. */
    public static boolean puedeAparecer(EntityType<? extends LeonDeGuerra> tipo, LevelAccessor nivel, MobSpawnType razon,
                                        BlockPos posicion, RandomSource azar) {
        BlockState debajo = nivel.getBlockState(posicion.below());
        return (debajo.is(BlockTags.DIRT) || debajo.is(BlockTags.SAND)) && nivel.canSeeSkyFromBelowWater(posicion);
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
        datos.putBoolean("HaRugido", haRugido);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag datos) {
        super.readAdditionalSaveData(datos);
        haRugido = datos.getBoolean("HaRugido");
    }
}
