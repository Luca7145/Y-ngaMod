package com.lucarita.yonga.modulos.fe.mobs;

import com.lucarita.yonga.modulos.fe.ModuloFe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Enana madre (luminosa, amarilla) y enana madre oscura (roja): un enano gigante, tipo miniboss. Ser SUPERIOR
 * (la luminosa ataca a todos los que no lleguen a Prelado; la oscura, a todos los que no lleguen a Blasfemo).
 *
 *  - EMBESTIDA desde lejos con mucho aviso: se para, ruge, tiembla y escarba (2 s). Un segundo antes de salir
 *    fija la dirección y marca el camino con polvo: da tiempo a apartarse. Sale en línea recta muy rápido.
 *    Si te da: muchísimo daño y te manda muy lejos. Si FALLA (choca o se pasa), se queda aturdida unos
 *    segundos: ahí recibe un 50% más de daño (fuera de eso, su piel dura le quita la mitad).
 *  - PISOTÓN si te pegas a ella: se alza (aviso) y cae, golpeando todo alrededor.
 *  - Al morir salen 3 enanos de su mismo bando.
 * Aparece de vez en cuando en llanuras (sin árboles que la frenen).
 */
public class EnanoMadre extends Enano {

    // ---------- AJUSTES ----------
    public static final double VIDA = 200.0;                // 100 corazones
    private static final double EMBESTIDA_MIN = 7.0, EMBESTIDA_MAX = 26.0;
    private static final int TICKS_AVISO = 40;              // 2 s de aviso
    private static final int FIJAR_DIRECCION = 20;          // ticks antes de salir en que deja de girarse
    private static final int TICKS_CARRERA_MAX = 40;
    private static final double VELOCIDAD_EMBESTIDA = 1.05;
    private static final float DANO_EMBESTIDA = 18.0f;      // 9 corazones
    private static final float EMPUJE = 3.8f;
    private static final double EMPUJE_ARRIBA = 0.8;
    private static final int ATURDIDA_SI_FALLA = 70, ATURDIDA_SI_CHOCA = 90;
    private static final int ESPERA_EMBESTIDA = 60;
    private static final float CASTIGO = 1.5f, PIEL_DURA = 0.5f;

    private static final double ALCANCE_PISOTON = 4.0, RADIO_PISOTON = 5.0;
    private static final int TICKS_PISOTON = 22, GOLPE_PISOTON = 15;
    private static final float DANO_PISOTON = 10.0f;
    private static final int ESPERA_PISOTON = 60;

    private static final int CRIAS = 3;

    private int ticksEstado;
    private int esperaEmbestida, esperaPisoton;
    private int duracionAturdida;
    private Vec3 direccion = Vec3.ZERO;
    private boolean criasSoltadas;

    public EnanoMadre(EntityType<? extends EnanoMadre> tipo, Level nivel, boolean oscuro) {
        super(tipo, nivel, oscuro);
        this.setMaxUpStep(1.5f);
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder atributosMadre() {
        return Enano.atributos()
                .add(Attributes.MAX_HEALTH, VIDA)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, DANO_EMBESTIDA)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public boolean esSuperior() {
        return true;
    }

    // ---------- IA ----------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6) {
            @Override
            public boolean canUse() {
                return getTarget() == null && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return getTarget() == null && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0f));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::atacaA));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (esperaEmbestida > 0) esperaEmbestida--;
        if (esperaPisoton > 0) esperaPisoton--;
        ticksEstado++;

        LivingEntity objetivo = this.getTarget();
        if (objetivo != null && (!objetivo.isAlive() || (objetivo instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            this.setTarget(null);
            objetivo = null;
        }
        int estado = estado();

        if (estado == ATURDIDO) {
            this.getNavigation().stop();
            if (ticksEstado % 10 == 0 && this.level() instanceof ServerLevel nivel) {
                nivel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + this.getBbHeight() + 0.2, this.getZ(),
                        8, 0.6, 0.1, 0.6, 0.05);
            }
            if (ticksEstado >= duracionAturdida) cambiar(NORMAL);
            return;
        }
        if (objetivo == null) {
            if (estado != NORMAL) cambiar(NORMAL);
            return;
        }
        double distancia = this.distanceTo(objetivo);

        switch (estado) {
            case PREPARANDO -> preparar(objetivo);
            case EMBISTIENDO -> embestir(objetivo);
            case PISOTON -> pisotear();
            default -> {
                this.getLookControl().setLookAt(objetivo, 20.0f, 20.0f);
                if (distancia < ALCANCE_PISOTON && esperaPisoton == 0) {
                    esperaPisoton = ESPERA_PISOTON;
                    cambiar(PISOTON);
                    this.playSound(SoundEvents.RAVAGER_ROAR, 1.5f, esOscuro() ? 0.7f : 1.1f);
                } else if (distancia > EMBESTIDA_MIN && distancia < EMBESTIDA_MAX && esperaEmbestida == 0
                        && this.onGround() && this.getSensing().hasLineOfSight(objetivo)) {
                    cambiar(PREPARANDO);
                    this.playSound(SoundEvents.RAVAGER_ROAR, 3.0f, esOscuro() ? 0.6f : 0.95f);
                } else if (this.tickCount % 10 == 0 || this.getNavigation().isDone()) {
                    this.getNavigation().moveTo(objetivo, 1.0);
                }
            }
        }
    }

    /** Aviso de la embestida: quieta, mirándote, escarbando. Al final deja de girarse y marca el camino. */
    private void preparar(LivingEntity objetivo) {
        this.getNavigation().stop();
        boolean fijada = ticksEstado > TICKS_AVISO - FIJAR_DIRECCION;
        if (!fijada) {
            mirarA(objetivo);
            Vec3 hacia = objetivo.position().subtract(this.position());
            direccion = new Vec3(hacia.x, 0.0, hacia.z).normalize();
        }
        if (this.level() instanceof ServerLevel nivel) {
            BlockState suelo = this.level().getBlockState(this.blockPosition().below());
            if (ticksEstado % 3 == 0) {
                nivel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, suelo), this.getX(), this.getY() + 0.1, this.getZ(),
                        6, 0.8, 0.05, 0.8, 0.1);
            }
            if (fijada && ticksEstado % 2 == 0) {
                // el camino que va a seguir
                for (int i = 2; i <= 14; i += 2) {
                    Vec3 p = this.position().add(direccion.scale(i));
                    nivel.sendParticles(ParticleTypes.CLOUD, p.x, this.getY() + 0.15, p.z, 1, 0.15, 0.0, 0.15, 0.0);
                }
            }
        }
        if (ticksEstado % 10 == 0) {
            this.playSound(SoundEvents.RAVAGER_STEP, 1.2f, 0.6f);
        }
        if (ticksEstado >= TICKS_AVISO) {
            cambiar(EMBISTIENDO);
            this.playSound(SoundEvents.RAVAGER_ROAR, 2.0f, esOscuro() ? 0.8f : 1.2f);
        }
    }

    private void embestir(LivingEntity objetivo) {
        this.getNavigation().stop();
        float giro = (float) (Mth.atan2(direccion.z, direccion.x) * Mth.RAD_TO_DEG) - 90.0f;
        this.setYRot(giro);
        this.setYBodyRot(giro);
        this.setYHeadRot(giro);
        this.setDeltaMovement(direccion.x * VELOCIDAD_EMBESTIDA, this.getDeltaMovement().y, direccion.z * VELOCIDAD_EMBESTIDA);
        if (ticksEstado % 3 == 0) {
            this.playSound(SoundEvents.RAVAGER_STEP, 1.5f, 0.8f);
        }

        if (this.getBoundingBox().inflate(0.4).intersects(objetivo.getBoundingBox())) {
            objetivo.hurt(this.damageSources().mobAttack(this), DANO_EMBESTIDA);
            double resistencia = objetivo.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            objetivo.knockback(EMPUJE * (1.0 - resistencia), -direccion.x, -direccion.z);
            objetivo.setDeltaMovement(objetivo.getDeltaMovement().add(0.0, EMPUJE_ARRIBA * (1.0 - resistencia), 0.0));
            empujar(objetivo);
            this.playSound(SoundEvents.GOAT_RAM_IMPACT, 2.0f, 0.5f);
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.1, 1.0, 0.1));
            esperaEmbestida = ESPERA_EMBESTIDA;
            cambiar(NORMAL);
        } else if (this.horizontalCollision) {
            // ¡contra la pared!
            this.playSound(SoundEvents.GENERIC_EXPLODE, 0.6f, 1.4f);
            this.playSound(SoundEvents.GOAT_HORN_BREAK, 1.5f, 0.5f);
            aturdirse(ATURDIDA_SI_CHOCA);
        } else if (ticksEstado >= TICKS_CARRERA_MAX) {
            aturdirse(ATURDIDA_SI_FALLA);
        }
    }

    private void aturdirse(int duracion) {
        this.setDeltaMovement(Vec3.ZERO);
        duracionAturdida = duracion;
        esperaEmbestida = ESPERA_EMBESTIDA;
        cambiar(ATURDIDO);
        this.playSound(SoundEvents.GOAT_SCREAMING_HURT, 1.5f, esOscuro() ? 0.5f : 0.7f);
    }

    /** Se alza y cae: golpea a todo lo atacable alrededor. */
    private void pisotear() {
        this.getNavigation().stop();
        if (ticksEstado == GOLPE_PISOTON && this.level() instanceof ServerLevel nivel) {
            for (LivingEntity vivo : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(RADIO_PISOTON, 1.0, RADIO_PISOTON))) {
                if (vivo == this || vivo instanceof MobFe || !(vivo == this.getTarget() || (vivo instanceof Player && atacaA(vivo)))) {
                    continue;
                }
                if (vivo.hurt(this.damageSources().mobAttack(this), DANO_PISOTON)) {
                    Vec3 hacia = vivo.position().subtract(this.position()).multiply(1, 0, 1).normalize();
                    vivo.knockback(2.0, -hacia.x, -hacia.z);
                    vivo.setDeltaMovement(vivo.getDeltaMovement().add(0.0, 0.5, 0.0));
                    empujar(vivo);
                }
            }
            BlockState suelo = this.level().getBlockState(this.blockPosition().below());
            nivel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, suelo), this.getX(), this.getY() + 0.1, this.getZ(),
                    60, RADIO_PISOTON * 0.5, 0.1, RADIO_PISOTON * 0.5, 0.2);
            nivel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.3, this.getZ(), 2, 1.0, 0.0, 1.0, 0.0);
            this.playSound(SoundEvents.GENERIC_EXPLODE, 1.0f, 0.6f);
        }
        if (ticksEstado >= TICKS_PISOTON) cambiar(NORMAL);
    }

    private void cambiar(int estado) {
        fijarEstado(estado);
        ticksEstado = 0;
    }

    private void mirarA(LivingEntity objetivo) {
        float giro = (float) (Mth.atan2(objetivo.getZ() - this.getZ(), objetivo.getX() - this.getX()) * Mth.RAD_TO_DEG) - 90.0f;
        this.setYRot(Mth.rotLerp(0.3f, this.getYRot(), giro));
        this.setYBodyRot(this.getYRot());
        this.setYHeadRot(this.getYRot());
    }

    private static void empujar(LivingEntity objetivo) {
        objetivo.hurtMarked = true;
        if (objetivo instanceof ServerPlayer jugador) {
            jugador.connection.send(new ClientboundSetEntityMotionPacket(jugador));
        }
    }

    // ---------- DAÑO: piel dura, salvo aturdida ----------

    @Override
    public boolean hurt(DamageSource fuente, float cantidad) {
        if (!fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            cantidad *= estado() == ATURDIDO ? CASTIGO : PIEL_DURA;
        }
        return super.hurt(fuente, cantidad);
    }

    // ---------- AL MORIR: SUS CRÍAS ----------

    @Override
    public void die(DamageSource fuente) {
        super.die(fuente);
        if (!(this.level() instanceof ServerLevel nivel) || criasSoltadas) {
            return;
        }
        criasSoltadas = true;
        EntityType<Enano> tipo = esOscuro() ? ModuloFe.ENANO_OSCURO.get() : ModuloFe.ENANO.get();
        for (int i = 0; i < CRIAS; i++) {
            Enano cria = tipo.create(nivel);
            if (cria == null) continue;
            double angulo = i * Math.PI * 2 / CRIAS;
            cria.moveTo(this.getX() + Math.cos(angulo) * 1.2, this.getY() + 0.2, this.getZ() + Math.sin(angulo) * 1.2,
                    this.random.nextFloat() * 360.0f, 0.0f);
            if (fuente.getEntity() instanceof LivingEntity asesino && cria.atacaA(asesino)) {
                cria.setTarget(asesino);
            }
            nivel.addFreshEntity(cria);
        }
        nivel.playSound(null, this.blockPosition(), SoundEvents.GOAT_SCREAMING_AMBIENT, SoundSource.HOSTILE, 1.5f, 1.4f);
    }

    // ---------- SONIDOS (más graves) ----------

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.45f;
    }

    @Override
    protected float getSoundVolume() {
        return 2.0f;
    }

    @Override
    protected void playStepSound(BlockPos posicion, BlockState bloque) {
        this.playSound(SoundEvents.RAVAGER_STEP, 0.6f, 0.7f);
    }

    // ---------- APARICIÓN ----------

    /** Sobre hierba, en la superficie, con cielo encima: llanuras. */
    public static boolean puedeAparecerMadre(EntityType<? extends EnanoMadre> tipo, LevelAccessor nivel, MobSpawnType razon,
                                             BlockPos posicion, RandomSource azar) {
        return AparicionMobsFe.esSuelo(nivel.getBlockState(posicion.below())) && nivel.canSeeSkyFromBelowWater(posicion);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }
}
