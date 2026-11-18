package com.lucarita.yonga.modulos.fe.mobs;

import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.armas.Aturdimiento;
import com.lucarita.yonga.modulos.fe.ModuloFe;
import com.lucarita.yonga.modulos.fe.red.FeRed;
import com.lucarita.yonga.modulos.fe.red.JumpscarePacket;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

/**
 * El Astado: ser oscuro SUPERIOR (ataca a todos los que no lleguen a Blasfemo, ver MobFe).
 * Largo, famélico, con dos grandes astas. No aparece de forma normal: lo trae la fase de acecho
 * (AcechoAstado) a un jugador que anda de noche por el bosque.
 *
 * Fase de CAZA (este mob):
 *  - ERGUIDO: anda a velocidad normal y se acerca al jugador dando rodeos, sin ir directo.
 *  - En cuanto un jugador lo MIRA (o lo tiene casi encima, o le pegan), ruge, se pone a cuatro patas y va a
 *    por él muy rápido. Ya no vuelve a ponerse de pie.
 *  - Ataques: embestida (de lejos; daño alto, aturde y manda muy lejos), zarpazo (de cerca; daño alto y
 *    sangrado) y agarre (lo coge, le muerde varias veces y lo lanza por los aires; al cogido le salta un susto).
 *  - Los jugadores cerca de él quedan a oscuras (efecto Oscuridad, como con el warden).
 *  - Se va (desaparece entre humo) si se hace de día antes de cazar, o si se queda sin nadie a quien cazar
 *    (solo el que trae el acecho: el del huevo o el del comando se queda).
 */
public class Astado extends Monster implements MobFe {

    // ---------- AJUSTES ----------
    public static final double VIDA = 220.0;               // 110 corazones
    public static final double VELOCIDAD = 0.26;           // de pie va como un zombi un poco rápido
    public static final double MULT_ERGUIDO = 1.0;
    public static final double MULT_CAZA = 2.1;            // a cuatro patas: más rápido que un jugador corriendo

    // Acercarse dando rodeos (de pie)
    private static final double RADIO_MIN_RODEO = 7.0;
    private static final int TICKS_ENTRE_RODEOS = 60;
    private static final double DISTANCIA_SORPRESA = 4.0;  // si llega tan cerca sin que lo vean, caza igual
    private static final double DISTANCIA_VISTA = 48.0;
    private static final double MIRADA_COS = 0.97;          // ~14 grados alrededor de donde mira el jugador

    // Rugido al empezar la caza
    private static final int TICKS_RUGIDO = 40;

    // Embestida
    private static final double EMBESTIDA_MIN = 6.0, EMBESTIDA_MAX = 18.0;
    private static final int TICKS_PREPARAR_EMBESTIDA = 16;
    private static final int TICKS_EMBESTIDA_MAX = 26;
    private static final double VELOCIDAD_EMBESTIDA = 1.05;
    private static final float DANO_EMBESTIDA = 17.0f;      // 8,5 corazones
    private static final float EMPUJE_EMBESTIDA = 3.6f;
    private static final double EMPUJE_ARRIBA = 0.65;
    private static final int ATURDIR_TICKS = 50;            // 2,5 s
    private static final int ESPERA_EMBESTIDA = 160;        // 8 s

    // Zarpazo
    private static final double ALCANCE = 3.3;
    private static final int TICKS_ZARPAZO = 14, GOLPE_ZARPAZO = 7;
    private static final float DANO_ZARPAZO = 13.0f;         // 6,5 corazones
    private static final Sangrado.Tabla SANGRADO_ZARPAZO = new Sangrado.Tabla(0.0f, 0.35f, 0.65f, 300); // siempre sangra
    private static final int ESPERA_ZARPAZO = 30;

    // Agarre
    private static final float PROB_AGARRE = 0.35f;          // al elegir ataque de cerca
    private static final int MORDISCOS = 3, TICKS_ENTRE_MORDISCOS = 16, TICKS_AGARRE = 58;
    private static final float DANO_MORDISCO = 8.0f;         // 4 corazones cada uno: 12 en total, el que más quita
    private static final double LANZAR_ADELANTE = 2.8, LANZAR_ARRIBA = 1.7;   // lo manda lejísimos
    private static final int TICKS_LANZAR = 12;
    private static final int ESPERA_AGARRE = 300;            // 15 s

    // Oscuridad alrededor
    private static final double RADIO_OSCURIDAD = 24.0;
    private static final int OSCURIDAD_TICKS = 120;

    // Irse
    private static final int TICKS_SIN_OBJETIVO_PARA_IRSE = 600;

    // ---------- ESTADO (sincronizado para las animaciones) ----------
    public static final int NADA = 0, RUGIDO = 1, PREPARA_EMBESTIDA = 2, EMBESTIDA = 3, ZARPAZO = 4, AGARRE = 5, LANZAR = 6;
    private static final EntityDataAccessor<Boolean> CAZANDO = SynchedEntityData.defineId(Astado.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ACCION = SynchedEntityData.defineId(Astado.class, EntityDataSerializers.INT);

    private static final EntityDimensions TAMANO_ERGUIDO = EntityDimensions.scalable(0.9f, 2.9f);
    private static final EntityDimensions TAMANO_CAZA = EntityDimensions.scalable(1.3f, 1.9f);

    // Servidor
    private UUID presa;                 // el jugador al que vino a buscar
    private boolean deAcecho;           // lo trajo el acecho (se va de día / sin presa); con huevo o comando, no
    private int ticksAccion;
    private int esperaEmbestida, esperaZarpazo, esperaAgarre;
    private int ticksRodeo;
    private int ladoRodeo = 1;
    private int ticksSinObjetivo;
    private Vec3 direccionEmbestida = Vec3.ZERO;

    // Cliente (para las animaciones)
    public int inicioAccionCliente;
    public float caza0, caza;            // 0 = de pie, 1 = a cuatro patas (se pasa poco a poco)

    public Astado(EntityType<? extends Astado> tipo, Level nivel) {
        super(tipo, nivel);
        this.setMaxUpStep(1.1f);
        this.xpReward = 50;                 // el que más: el jefe de la noche
    }

    public static AttributeSupplier.Builder atributos() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, VIDA)
                .add(Attributes.MOVEMENT_SPEED, VELOCIDAD)
                .add(Attributes.ATTACK_DAMAGE, DANO_ZARPAZO)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    public boolean esOscuro() {
        return true;
    }

    @Override
    public boolean esSuperior() {
        return true;
    }

    public boolean cazando() {
        return this.entityData.get(CAZANDO);
    }

    public int accion() {
        return this.entityData.get(ACCION);
    }

    public boolean agarrando() {
        return accion() == AGARRE;
    }

    public void fijarPresa(Player jugador, boolean deAcecho) {
        this.presa = jugador.getUUID();
        this.deAcecho = deAcecho;
        this.setTarget(jugador);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CAZANDO, false);
        this.entityData.define(ACCION, NADA);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> dato) {
        super.onSyncedDataUpdated(dato);
        if (CAZANDO.equals(dato)) {
            this.refreshDimensions();
        }
        if (ACCION.equals(dato)) {
            this.inicioAccionCliente = this.tickCount;
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return cazando() ? TAMANO_CAZA : TAMANO_ERGUIDO;
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions tamano) {
        return tamano.height * 0.88f;
    }

    // ---------- IA ----------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false, this::atacaA));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            caza0 = caza;
            caza = Mth.approach(caza, cazando() ? 1.0f : 0.0f, 0.08f);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity objetivo = elegirObjetivo();

        if (esperaEmbestida > 0) esperaEmbestida--;
        if (esperaZarpazo > 0) esperaZarpazo--;
        if (esperaAgarre > 0) esperaAgarre--;

        // Oscuridad a los de alrededor
        if (this.tickCount % 40 == 0) {
            for (Player jugador : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(RADIO_OSCURIDAD))) {
                if (!jugador.isCreative() && !jugador.isSpectator()) {
                    jugador.addEffect(new MobEffectInstance(MobEffects.DARKNESS, OSCURIDAD_TICKS, 0, false, false), this);
                }
            }
        }

        // ¿Toca irse?
        if (objetivo == null) {
            if (accion() != NADA) {
                terminarAccion();   // p. ej. la víctima ha muerto en pleno agarre
            }
            if (deAcecho && ++ticksSinObjetivo > TICKS_SIN_OBJETIVO_PARA_IRSE) {
                desvanecerse();
            }
            return;
        }
        ticksSinObjetivo = 0;
        if (deAcecho && !cazando() && this.level().isDay()) {
            desvanecerse();
            return;
        }

        if (!cazando()) {
            acechar(objetivo);
        } else {
            cazar(objetivo);
        }
    }

    /** El objetivo: el que le pegó, su presa o el jugador atacable más cercano (lo pone el targetSelector). */
    private LivingEntity elegirObjetivo() {
        LivingEntity objetivo = this.getTarget();
        if (objetivo == null && presa != null && this.level() instanceof ServerLevel nivel) {
            Player jugador = nivel.getPlayerByUUID(presa);
            if (jugador != null && jugador.isAlive() && atacaA(jugador) && !jugador.isCreative() && !jugador.isSpectator()
                    && jugador.distanceToSqr(this) < 96 * 96) {
                this.setTarget(jugador);
                objetivo = jugador;
            }
        }
        if (objetivo != null && (!objetivo.isAlive() || objetivo.isSpectator()
                || (objetivo instanceof Player p && p.isCreative()) || objetivo.distanceToSqr(this) > 96 * 96)) {
            this.setTarget(null);
            objetivo = null;
        }
        return objetivo;
    }

    // ---------- DE PIE: ACERCARSE DANDO RODEOS ----------

    private void acechar(LivingEntity objetivo) {
        // ¿Lo ha visto alguien? ¿Lo tiene casi encima?
        for (Player jugador : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(DISTANCIA_VISTA))) {
            if (atacaA(jugador) && !jugador.isCreative() && !jugador.isSpectator() && meEstaMirando(jugador)) {
                this.setTarget(jugador);
                empezarCaza();
                return;
            }
        }
        if (this.distanceTo(objetivo) < DISTANCIA_SORPRESA) {
            empezarCaza();
            return;
        }

        // De vez en cuando gira la cabeza hacia él (sin ir directo)
        if (this.random.nextInt(40) == 0) {
            this.getLookControl().setLookAt(objetivo, 20.0f, 20.0f);
        }
        if (--ticksRodeo <= 0 || this.getNavigation().isDone()) {
            ticksRodeo = TICKS_ENTRE_RODEOS + this.random.nextInt(40);
            if (this.random.nextInt(4) == 0) {
                ladoRodeo = -ladoRodeo;
            }
            double angulo = Math.atan2(this.getZ() - objetivo.getZ(), this.getX() - objetivo.getX())
                    + ladoRodeo * Math.toRadians(30 + this.random.nextInt(25));
            double radio = Math.max(RADIO_MIN_RODEO, this.distanceTo(objetivo) - 5.0);
            double x = objetivo.getX() + Math.cos(angulo) * radio;
            double z = objetivo.getZ() + Math.sin(angulo) * radio;
            int y = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
            this.getNavigation().moveTo(x, y, z, MULT_ERGUIDO);
        }
    }

    /** Como el enderman: el jugador lo mira (más o menos a la cara o al cuerpo) y nada se interpone. */
    private boolean meEstaMirando(Player jugador) {
        Vec3 vista = jugador.getViewVector(1.0f).normalize();
        for (double altura : new double[]{0.85, 0.5, 0.2}) {
            Vec3 hacia = new Vec3(this.getX() - jugador.getX(),
                    this.getY() + this.getBbHeight() * altura - jugador.getEyeY(),
                    this.getZ() - jugador.getZ());
            if (hacia.length() < DISTANCIA_VISTA && vista.dot(hacia.normalize()) > MIRADA_COS) {
                return jugador.hasLineOfSight(this);
            }
        }
        return false;
    }

    private void empezarCaza() {
        if (cazando()) {
            return;
        }
        this.entityData.set(CAZANDO, true);
        this.refreshDimensions();
        this.getNavigation().stop();
        empezarAccion(RUGIDO);
        this.playSound(ModuloFe.SONIDO_ASTADO_CHILLIDO.get(), 4.0f, 1.0f);
    }

    // ---------- A CUATRO PATAS: LA CAZA ----------

    private void cazar(LivingEntity objetivo) {
        ticksAccion++;
        int accion = accion();
        this.getLookControl().setLookAt(objetivo, 40.0f, 40.0f);
        double distancia = this.distanceTo(objetivo);

        switch (accion) {
            case RUGIDO -> {
                this.getNavigation().stop();
                if (ticksAccion >= TICKS_RUGIDO) terminarAccion();
            }
            case PREPARA_EMBESTIDA -> {
                this.getNavigation().stop();
                mirarHacia(objetivo);
                if (ticksAccion >= TICKS_PREPARAR_EMBESTIDA) {
                    Vec3 hacia = objetivo.position().subtract(this.position());
                    direccionEmbestida = new Vec3(hacia.x, 0.0, hacia.z).normalize();
                    empezarAccion(EMBESTIDA);
                    this.playSound(ModuloFe.SONIDO_ASTADO_GRUNIDO.get(), 3.0f, 0.8f);
                }
            }
            case EMBESTIDA -> embestir(objetivo);
            case ZARPAZO -> {
                this.getNavigation().stop();
                mirarHacia(objetivo);
                if (ticksAccion == GOLPE_ZARPAZO && distancia < ALCANCE + 0.6 && delante(objetivo)) {
                    if (objetivo.hurt(this.damageSources().mobAttack(this), DANO_ZARPAZO)) {
                        Sangrado.intentarAplicar(objetivo, this.random, SANGRADO_ZARPAZO);
                    }
                }
                if (ticksAccion >= TICKS_ZARPAZO) terminarAccion();
            }
            case AGARRE -> agarrar(objetivo);
            case LANZAR -> {
                this.getNavigation().stop();
                if (ticksAccion >= TICKS_LANZAR) terminarAccion();
            }
            default -> elegirAtaque(objetivo, distancia);
        }
    }

    private void elegirAtaque(LivingEntity objetivo, double distancia) {
        if (this.tickCount % 10 == 0 || this.getNavigation().isDone()) {
            this.getNavigation().moveTo(objetivo, MULT_CAZA);
        }
        if (distancia < ALCANCE) {
            if (esperaAgarre == 0 && objetivo instanceof ServerPlayer && this.random.nextFloat() < PROB_AGARRE) {
                empezarAgarre(objetivo);
            } else if (esperaZarpazo == 0) {
                esperaZarpazo = ESPERA_ZARPAZO;
                empezarAccion(ZARPAZO);
                this.playSound(ModuloFe.SONIDO_ASTADO_GRUNIDO.get(), 2.0f, 1.2f);
            }
        } else if (distancia > EMBESTIDA_MIN && distancia < EMBESTIDA_MAX && esperaEmbestida == 0
                && this.onGround() && this.getSensing().hasLineOfSight(objetivo)) {
            esperaEmbestida = ESPERA_EMBESTIDA;
            empezarAccion(PREPARA_EMBESTIDA);
            this.playSound(ModuloFe.SONIDO_ASTADO_CHILLIDO.get(), 2.0f, 1.3f);
        }
    }

    private void embestir(LivingEntity objetivo) {
        this.getNavigation().stop();
        float giro = (float) (Mth.atan2(direccionEmbestida.z, direccionEmbestida.x) * Mth.RAD_TO_DEG) - 90.0f;
        this.setYRot(giro);
        this.setYBodyRot(giro);
        this.setYHeadRot(giro);
        this.setDeltaMovement(direccionEmbestida.x * VELOCIDAD_EMBESTIDA, this.getDeltaMovement().y,
                direccionEmbestida.z * VELOCIDAD_EMBESTIDA);

        if (this.getBoundingBox().inflate(0.5).intersects(objetivo.getBoundingBox())) {
            objetivo.hurt(this.damageSources().mobAttack(this), DANO_EMBESTIDA);
            double resistencia = objetivo.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            objetivo.knockback(EMPUJE_EMBESTIDA * (1.0 - resistencia), -direccionEmbestida.x, -direccionEmbestida.z);
            objetivo.setDeltaMovement(objetivo.getDeltaMovement().add(0.0, EMPUJE_ARRIBA * (1.0 - resistencia), 0.0));
            empujar(objetivo);
            aturdir(objetivo);
            this.playSound(ModuloFe.SONIDO_ASTADO_GOLPE.get(), 2.0f, 0.9f);
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.1, 1.0, 0.1));
            terminarAccion();
        } else if (this.horizontalCollision || ticksAccion >= TICKS_EMBESTIDA_MAX) {
            if (this.horizontalCollision) {
                this.playSound(ModuloFe.SONIDO_ASTADO_GOLPE.get(), 1.5f, 0.6f);
            }
            terminarAccion();
        }
    }

    private void aturdir(LivingEntity objetivo) {
        if (Modulo.ARMAS.activo()) {
            Aturdimiento.aturdir(objetivo, ATURDIR_TICKS);
        } else {
            objetivo.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ATURDIR_TICKS, 6, false, false, true));
        }
    }

    // ---------- EL AGARRE ----------

    private void empezarAgarre(LivingEntity objetivo) {
        if (!objetivo.startRiding(this, true)) {
            esperaZarpazo = ESPERA_ZARPAZO;
            empezarAccion(ZARPAZO);
            return;
        }
        esperaAgarre = ESPERA_AGARRE;
        empezarAccion(AGARRE);
        this.getNavigation().stop();
        if (objetivo instanceof ServerPlayer jugador) {
            FeRed.CANAL.send(PacketDistributor.PLAYER.with(() -> jugador), new JumpscarePacket());
        }
        this.playSound(ModuloFe.SONIDO_ASTADO_CHILLIDO.get(), 3.0f, 0.7f);
    }

    private void agarrar(LivingEntity objetivo) {
        this.getNavigation().stop();
        this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
        if (objetivo.getVehicle() != this) {
            // Se ha soltado (o lo han sacado): fin del agarre
            terminarAccion();
            return;
        }
        if (ticksAccion % TICKS_ENTRE_MORDISCOS == 0 && ticksAccion / TICKS_ENTRE_MORDISCOS <= MORDISCOS) {
            objetivo.invulnerableTime = 0;
            objetivo.hurt(this.damageSources().mobAttack(this), DANO_MORDISCO);
            this.playSound(ModuloFe.SONIDO_ASTADO_MORDISCO.get(), 1.6f, 0.8f + this.random.nextFloat() * 0.3f);
        }
        if (ticksAccion >= TICKS_AGARRE) {
            // ¡Por los aires!
            this.ejectPassengers();
            Vec3 adelante = Vec3.directionFromRotation(0.0f, this.getYRot());
            objetivo.setDeltaMovement(adelante.x * LANZAR_ADELANTE, LANZAR_ARRIBA, adelante.z * LANZAR_ADELANTE);
            empujar(objetivo);
            this.playSound(ModuloFe.SONIDO_ASTADO_GRUNIDO.get(), 3.0f, 0.7f);
            empezarAccion(LANZAR);
        }
    }

    /** La víctima va delante de la boca, a la altura de la cabeza. */
    @Override
    protected void positionRider(Entity pasajero, MoveFunction mover) {
        Vec3 adelante = Vec3.directionFromRotation(0.0f, this.yBodyRot);
        mover.accept(pasajero, this.getX() + adelante.x * 1.15, this.getY() + 0.75, this.getZ() + adelante.z * 1.15);
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.75;
    }

    @Override
    protected boolean canAddPassenger(Entity pasajero) {
        return this.getPassengers().isEmpty() && pasajero instanceof LivingEntity;
    }

    @Override
    public boolean shouldRiderSit() {
        return false;   // la víctima cuelga, no va sentada
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return null;   // la víctima no lo maneja
    }

    /**
     * No se puede bajar mientras le muerde (ni con mayús). Solo en el servidor: es quien decide cuándo se baja
     * alguien, y así el cliente nunca se queda "montado" por error. Se registra en ModuloFe.
     */
    public static void alMontarODesmontar(EntityMountEvent evento) {
        if (evento.isDismounting() && !evento.getEntityMounting().level().isClientSide()
                && evento.getEntityBeingMounted() instanceof Astado astado
                && astado.isAlive() && !astado.isRemoved() && astado.agarrando() && astado.ticksAccion < TICKS_AGARRE) {
            evento.setCanceled(true);
        }
    }

    // ---------- UTILIDADES ----------

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

    private boolean delante(LivingEntity objetivo) {
        Vec3 adelante = Vec3.directionFromRotation(0.0f, this.getYRot());
        Vec3 hacia = objetivo.position().subtract(this.position()).multiply(1, 0, 1).normalize();
        return adelante.dot(hacia) > 0.3;
    }

    /** Los jugadores no se enteran del empujón si no se lo mandamos. */
    private static void empujar(LivingEntity objetivo) {
        objetivo.hurtMarked = true;
        if (objetivo instanceof ServerPlayer jugador) {
            jugador.connection.send(new ClientboundSetEntityMotionPacket(jugador));
        }
    }

    private void desvanecerse() {
        if (this.level() instanceof ServerLevel nivel) {
            nivel.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + this.getBbHeight() * 0.5, this.getZ(),
                    40, 0.5, this.getBbHeight() * 0.4, 0.5, 0.02);
            nivel.playSound(null, this.blockPosition(), ModuloFe.SONIDO_ASTADO_RESPIRACION.get(), SoundSource.HOSTILE, 1.5f, 0.6f);
        }
        this.discard();
    }

    // Si le pegan estando de pie, deja de disimular
    @Override
    public boolean hurt(DamageSource fuente, float cantidad) {
        boolean herido = super.hurt(fuente, cantidad);
        if (herido && !this.level().isClientSide && !cazando() && fuente.getEntity() instanceof Player jugador) {
            this.setTarget(jugador);
            empezarCaza();
        }
        return herido;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance efecto) {
        return efecto.getEffect() != MobEffects.DARKNESS && super.canBeAffected(efecto);
    }

    @Override
    public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fuente) {
        return super.causeFallDamage(Math.max(0.0f, distancia - 6.0f), multiplicador * 0.5f, fuente);
    }

    @Override
    public boolean removeWhenFarAway(double distancia) {
        return deAcecho;
    }

    // ---------- SONIDOS ----------

    @Override
    protected SoundEvent getAmbientSound() {
        return cazando() ? ModuloFe.SONIDO_ASTADO_CAZA.get() : ModuloFe.SONIDO_ASTADO_RESPIRACION.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return cazando() ? 50 : 110;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource fuente) {
        return ModuloFe.SONIDO_ASTADO_HERIDO.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModuloFe.SONIDO_ASTADO_MUERTE.get();
    }

    @Override
    protected void playStepSound(BlockPos posicion, BlockState bloque) {
        this.playSound(ModuloFe.SONIDO_ASTADO_PASO.get(), cazando() ? 0.9f : 0.7f, cazando() ? 1.25f : 0.9f);
    }

    @Override
    protected float getSoundVolume() {
        return 1.6f;
    }

    // ---------- GUARDAR ----------

    @Override
    public void addAdditionalSaveData(CompoundTag datos) {
        super.addAdditionalSaveData(datos);
        datos.putBoolean("Cazando", cazando());
        datos.putBoolean("DeAcecho", deAcecho);
        if (presa != null) {
            datos.putUUID("Presa", presa);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag datos) {
        super.readAdditionalSaveData(datos);
        this.entityData.set(CAZANDO, datos.getBoolean("Cazando"));
        deAcecho = datos.getBoolean("DeAcecho");
        if (datos.hasUUID("Presa")) {
            presa = datos.getUUID("Presa");
        }
    }
}
