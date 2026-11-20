package com.lucarita.yonga.modulos.fe.mobs;

import com.lucarita.yonga.modulos.fe.ModuloFe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * EL PEZ DEL VIENTO: primer jefe. Un cachalote blanco celestial, enorme (~10 bloques), con alas de plumas, un halo
 * dorado y constelaciones que brillan en la piel, que nada por el aire. Ser de fe luminosa, pero como
 * jefe ataca a cualquiera, tenga la fe que tenga. Se invoca con un ritual (RitualPez).
 *
 * INTRO: el altar estalla en luz, cae un rayo, sube un pilar de luz, aparece el título y la ballena baja en
 * espiral desde lo alto cantando mientras se llena su barra. Durante la intro no se le puede hacer daño.
 *
 * COMBATE: te rodea desde el cielo como un buitre y, sin avisar de cuál, hace uno de sus tres ataques:
 *  - BARRIDO: se aleja, baja a ras de suelo y te embiste en línea recta antes de volver a subir.
 *  - RAYO: se para en el aire, abre la boca (se ve cómo se carga la luz) y lanza un rayo que te va siguiendo.
 *  - PICADO: sube muy alto, se lanza de cabeza y revienta contra el suelo con una gran onda. Se queda tumbado y
 *    aturdido: es el momento de castigarle (recibe el doble de daño).
 *
 * FASES: a 2/3 de vida se divide en dos (él y una ILUSIÓN); a 1/3, en cuatro (él y tres ilusiones). Las
 * ilusiones (PezIlusion) hacen exactamente lo mismo desde otro lado, pero no tienen colisión ni hacen daño:
 * solo confunden. Al primer golpe desaparecen; al rato él saca las que le falten.
 */
public class PezDelViento extends Mob implements MobFe, Jefe {

    // ---------- AJUSTES ----------
    public static final double VIDA = 500.0;                     // 250 corazones
    private static final int TICKS_INTRO = 200;
    private static final double ALTURA_INTRO = 70.0;

    private static final double RADIO_RODEO = 22.0, ALTURA_RODEO = 16.0;
    private static final double VELOCIDAD_NADO = 0.55;
    private static final int RODEO_MIN = 80, RODEO_MAX = 150;

    private static final double LEJOS_BARRIDO = 30.0, VELOCIDAD_BARRIDO = 1.25;
    private static final int TICKS_BARRIDO_MAX = 60;
    private static final float DANO_BARRIDO = 14.0f;

    private static final int AVISO_RAYO = 35, TICKS_RAYO = 50, CADA_DANO_RAYO = 8;
    private static final float DANO_RAYO = 5.0f;
    private static final double ALCANCE_RAYO = 48.0;
    private static final double BARRIDO_RAYO = 0.16;             // bloques por tick que se mueve la punta del rayo
                                                                 // sobre el suelo (andando vas a ~0.21): se esquiva
    private static final int RAYO_SIN_DANO = 8;                  // los primeros ticks del rayo aún no hacen daño
    private static final double ANCHO_RAYO = 0.3;

    private static final double ALTURA_PICADO = 30.0;
    private static final int TICKS_SUBIR_PICADO = 45, FIJAR_PICADO = 12;
    private static final double VELOCIDAD_PICADO = 1.8;
    private static final double RADIO_ONDA = 12.0;
    private static final float DANO_ONDA = 20.0f;
    private static final int TICKS_ATURDIDO = 110;
    private static final float CASTIGO = 2.0f;

    private static final int ESPERA_PICADO = 900;                // al menos 45 s entre picado y picado
    private static final int PESO_BARRIDO = 4, PESO_RAYO = 4, PESO_PICADO = 2;
    private static final int ESPERA_ILUSIONES = 600;             // 30 s para sacar las que le falten
    private static final int TICKS_DIVIDIRSE = 40;

    // ---------- ESTADO (sincronizado para las animaciones) ----------
    public static final int INTRO = 0, RODEAR = 1, ALEJARSE = 2, BARRIDO = 3, SUBIR = 4, APUNTAR = 5, RAYO = 6,
            ELEVARSE = 7, PICADO = 8, ATURDIDO = 9, DIVIDIRSE = 10;
    private static final EntityDataAccessor<Integer> ACCION = SynchedEntityData.defineId(PezDelViento.class, EntityDataSerializers.INT);

    protected int ticksAccion;
    private int duracionRodeo;
    private double anguloRodeo;
    private int ultimoAtaque = -1;
    private long ultimoPicado = -100000;
    protected Vec3 objetivoVuelo = Vec3.ZERO;
    private Vec3 direccion = Vec3.ZERO;
    protected Vec3 haciaRayo = Vec3.ZERO;
    private BlockPos altar = BlockPos.ZERO;
    private int fase = 1;
    private final List<UUID> ilusiones = new ArrayList<>();
    private int esperaIlusiones;
    private final ServerBossEvent barra = new ServerBossEvent(Component.translatable("entity.yonga.pez_del_viento"),
            BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10);

    // Cliente (animación)
    public int inicioAccionCliente;
    public float alabeo0, alabeo;           // inclinación de lado al girar

    public PezDelViento(EntityType<? extends PezDelViento> tipo, Level nivel) {
        super(tipo, nivel);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.xpReward = 500;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder atributos() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, VIDA)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 96.0)
                .add(Attributes.FLYING_SPEED, VELOCIDAD_NADO);
    }

    @Override
    public boolean esOscuro() {
        return false;
    }

    @Override
    public boolean esSuperior() {
        return true;
    }

    /** Es un jefe: ataca a cualquiera (no en creativo ni espectador). */
    @Override
    public boolean atacaA(LivingEntity objetivo) {
        return objetivo instanceof Player p && !p.isCreative() && !p.isSpectator();
    }

    public int accion() {
        return this.entityData.get(ACCION);
    }

    public int ticksAccion() {
        return ticksAccion;
    }

    public boolean esIlusion() {
        return false;
    }

    /** Lo llama el ritual: dónde está el altar (la ballena aparece muy por encima y baja en espiral). */
    public void invocar(BlockPos altar) {
        this.altar = altar;
        empezar(INTRO);
        this.setHealth(1.0f);   // la barra se va llenando durante la intro
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ACCION, RODEAR);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> dato) {
        super.onSyncedDataUpdated(dato);
        if (ACCION.equals(dato)) {
            this.inicioAccionCliente = this.tickCount;
        }
    }

    // ---------- TICK ----------

    @Override
    public void tick() {
        this.noPhysics = accion() != ATURDIDO;
        super.tick();
        if (this.level().isClientSide) {
            // se ladea al girar, como un animal grande nadando
            alabeo0 = alabeo;
            float giro = Mth.wrapDegrees(this.getYRot() - this.yRotO);
            alabeo = Mth.lerp(0.1f, alabeo, Mth.clamp(-giro * 6.0f, -35.0f, 35.0f));
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ticksAccion++;
        if (altar.equals(BlockPos.ZERO)) {
            // con huevo o /summon: su "altar" es el suelo bajo donde apareció
            altar = BlockPos.containing(this.getX(), suelo(this.position()), this.getZ());
        }
        barra.setProgress(this.getHealth() / this.getMaxHealth());
        if (esperaIlusiones > 0) esperaIlusiones--;

        Player objetivo = elegirObjetivo();
        int accion = accion();
        if (accion == INTRO) {
            intro();
            return;
        }
        if (this.tickCount % 100 == 0) {
            Jefe.escalarVida(this, 64);          // más vida con más jugadores (conserva el porcentaje)
        }
        comprobarFase();
        efectos();

        Vec3 centro = objetivo != null ? objetivo.position() : Vec3.atBottomCenterOf(altar);
        switch (accion) {
            case RODEAR -> rodear(centro, objetivo);
            case ALEJARSE -> alejarse(centro);
            case BARRIDO -> barrer(centro);
            case SUBIR -> subir(centro);
            case APUNTAR, RAYO -> rayo(objetivo, centro);
            case ELEVARSE -> elevarse(centro);
            case PICADO -> picado();
            case ATURDIDO -> aturdido();
            case DIVIDIRSE -> dividirse(centro);
            default -> empezar(RODEAR);
        }
        orientarSegunVelocidad();
    }

    protected Player elegirObjetivo() {
        LivingEntity actual = this.getTarget();
        if (actual instanceof Player p && p.isAlive() && atacaA(p) && p.distanceToSqr(this) < 120 * 120) {
            return p;
        }
        Player cercano = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), 96.0, e -> e instanceof Player p && atacaA(p));
        this.setTarget(cercano);
        return cercano;
    }

    // ---------- LA INTRO ----------

    private void intro() {
        this.setInvulnerable(true);
        Vec3 base = Vec3.atBottomCenterOf(altar);
        float p = ticksAccion / (float) TICKS_INTRO;
        // la barra se llena
        this.setHealth(Math.max(1.0f, this.getMaxHealth() * Math.min(1.0f, p * 1.1f)));
        if (this.level() instanceof ServerLevel nivel) {
            if (ticksAccion == 1) {
                // ¡el altar estalla en luz!
                LightningBolt rayo = EntityType.LIGHTNING_BOLT.create(nivel);
                if (rayo != null) {
                    rayo.moveTo(base);
                    rayo.setVisualOnly(true);
                    nivel.addFreshEntity(rayo);
                }
                nivel.sendParticles(ParticleTypes.FLASH, base.x, base.y + 1, base.z, 3, 0.5, 0.5, 0.5, 0);
                nivel.sendParticles(ParticleTypes.END_ROD, base.x, base.y + 1, base.z, 120, 1.5, 1.5, 1.5, 0.35);
                titulo(nivel);
            }
            if (ticksAccion < 80 && ticksAccion % 2 == 0) {
                // el pilar de luz hacia el cielo
                for (int y = 0; y < 70; y += 2) {
                    nivel.sendParticles(ParticleTypes.END_ROD, base.x, base.y + y, base.z, 1, 0.15, 0.3, 0.15, 0.0);
                }
                nivel.sendParticles(ParticleTypes.CLOUD, base.x, base.y + 0.5, base.z, 6, 3.0, 0.1, 3.0, 0.05);
            }
            if (ticksAccion == 30 || ticksAccion == 140) {
                nivel.playSound(null, base.x, base.y + 30, base.z, ModuloFe.SONIDO_PEZ_CANTO.get(), SoundSource.HOSTILE, 12.0f, 1.0f);
            }
            if (ticksAccion == 2) {
                nivel.playSound(null, base.x, base.y, base.z, SoundEvents.ELYTRA_FLYING, SoundSource.HOSTILE, 6.0f, 0.5f);
            }
        }
        // baja en espiral: de muy alto y lejos a la altura del rodeo
        double angulo = p * Math.PI * 3.0;
        double radio = Mth.lerp(p, 40.0, RADIO_RODEO);
        double altura = Mth.lerp(p * p, ALTURA_INTRO, ALTURA_RODEO);
        Vec3 punto = base.add(Math.cos(angulo) * radio, altura, Math.sin(angulo) * radio);
        nadarHacia(punto, 1.2, 0.2);
        orientarSegunVelocidad();
        if (ticksAccion >= TICKS_INTRO) {
            this.setInvulnerable(false);
            this.setHealth(this.getMaxHealth());
            this.playSound(ModuloFe.SONIDO_PEZ_BRAMIDO.get(), 10.0f, 0.9f);
            empezarRodeo();
        }
    }

    private void titulo(ServerLevel nivel) {
        for (ServerPlayer jugador : nivel.getPlayers(j -> j.distanceToSqr(Vec3.atCenterOf(altar)) < 96 * 96)) {
            jugador.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            jugador.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("yonga.pez.titulo")));
            jugador.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("yonga.pez.subtitulo")));
        }
    }

    // ---------- RODEAR Y ELEGIR ATAQUE (al azar, sin repetir) ----------

    private void empezarRodeo() {
        empezar(RODEAR);
        Jefe.objetivoAlAzar(this, 64);           // con varios jugadores, reparte los ataques entre todos
        duracionRodeo = RODEO_MIN + this.random.nextInt(RODEO_MAX - RODEO_MIN);
    }

    private void rodear(Vec3 centro, Player objetivo) {
        anguloRodeo += 0.025;
        double suelo = suelo(centro);
        Vec3 punto = new Vec3(centro.x + Math.cos(anguloRodeo) * RADIO_RODEO, Math.max(centro.y, suelo) + ALTURA_RODEO
                + Math.sin(this.tickCount * 0.03) * 3.0, centro.z + Math.sin(anguloRodeo) * RADIO_RODEO);
        nadarHacia(punto, VELOCIDAD_NADO, 0.05);
        if (this.tickCount % 220 == 0) {
            this.playSound(ModuloFe.SONIDO_PEZ_CANTO.get(), 8.0f, 0.9f + this.random.nextFloat() * 0.2f);
        }
        if (objetivo != null && ticksAccion >= duracionRodeo) {
            // al azar (con pesos), sin repetir el anterior y sin picado si hace poco del último
            boolean puedePicar = this.level().getGameTime() - ultimoPicado > ESPERA_PICADO;
            int ataque;
            do {
                int r = this.random.nextInt(PESO_BARRIDO + PESO_RAYO + (puedePicar ? PESO_PICADO : 0));
                ataque = r < PESO_BARRIDO ? 0 : r < PESO_BARRIDO + PESO_RAYO ? 1 : 2;
            } while (ataque == ultimoAtaque);
            if (ataque == 2) ultimoPicado = this.level().getGameTime();
            ultimoAtaque = ataque;
            switch (ataque) {
                case 0 -> {
                    // se aleja por donde va y baja a ras de suelo
                    Vec3 fuera = this.position().subtract(centro).multiply(1, 0, 1).normalize().scale(LEJOS_BARRIDO);
                    objetivoVuelo = centro.add(fuera.x, 0, fuera.z);
                    objetivoVuelo = new Vec3(objetivoVuelo.x, suelo(objetivoVuelo) + 2.5, objetivoVuelo.z);
                    empezar(ALEJARSE);
                }
                case 1 -> empezar(APUNTAR);
                default -> empezar(ELEVARSE);
            }
        }
    }

    // ---------- BARRIDO ----------

    private void alejarse(Vec3 centro) {
        nadarHacia(objetivoVuelo, 0.9, 0.08);
        if (this.position().distanceTo(objetivoVuelo) < 3.0 || ticksAccion > 120) {
            Vec3 hacia = centro.subtract(this.position()).multiply(1, 0, 1);
            direccion = hacia.normalize();
            this.playSound(ModuloFe.SONIDO_PEZ_BRAMIDO.get(), 8.0f, 1.1f);
            empezar(BARRIDO);
        }
    }

    private void barrer(Vec3 centro) {
        double altura = suelo(this.position()) + 2.0;
        Vec3 v = new Vec3(direccion.x * VELOCIDAD_BARRIDO, (altura - this.getY()) * 0.2, direccion.z * VELOCIDAD_BARRIDO);
        this.setDeltaMovement(this.getDeltaMovement().lerp(v, 0.35));
        golpearAlPasar(DANO_BARRIDO, 2.5, 0.6);
        if (ticksAccion >= TICKS_BARRIDO_MAX) {
            empezar(SUBIR);
        }
    }

    private void subir(Vec3 centro) {
        Vec3 arriba = this.position().add(this.getDeltaMovement().multiply(10, 0, 10)).add(0, 0, 0);
        nadarHacia(new Vec3(arriba.x, Math.max(centro.y, suelo(centro)) + ALTURA_RODEO, arriba.z), 0.8, 0.06);
        if (this.getY() >= Math.max(centro.y, suelo(centro)) + ALTURA_RODEO - 2 || ticksAccion > 80) {
            anguloRodeo = Math.atan2(this.getZ() - centro.z, this.getX() - centro.x);
            empezarRodeo();
        }
    }

    // ---------- RAYO ----------

    private void rayo(Player objetivo, Vec3 centro) {
        // se queda flotando, encarado, y abre la boca
        Vec3 deriva = this.getDeltaMovement().scale(0.9);
        this.setDeltaMovement(deriva.x, deriva.y + Math.sin(this.tickCount * 0.1) * 0.005, deriva.z);
        Vec3 boca = boca();
        if (objetivo == null) {
            empezarRodeo();
            return;
        }
        Vec3 haciaObjetivo = objetivo.getEyePosition().subtract(boca).normalize();
        if (accion() == APUNTAR) {
            haciaRayo = haciaObjetivo;
            mirarHacia(haciaObjetivo, 6.0f);
            if (ticksAccion == 1) this.playSound(ModuloFe.SONIDO_CENTINELA_CARGA.get(), 6.0f, 0.6f);
            if (ticksAccion >= AVISO_RAYO) {
                empezar(RAYO);
                this.playSound(ModuloFe.SONIDO_PEZ_RAYO.get(), 8.0f, 1.0f);
            }
            return;
        }
        // el rayo sigue al objetivo, pero despacio: su punta se arrastra por el suelo más lenta que tú andando
        double distancia = Math.max(4.0, objetivo.getEyePosition().distanceTo(boca));
        haciaRayo = girarHacia(haciaRayo, haciaObjetivo, (float) Math.toDegrees(BARRIDO_RAYO / distancia));
        mirarHacia(haciaRayo, 6.0f);
        if (ticksAccion > RAYO_SIN_DANO && ticksAccion % CADA_DANO_RAYO == 0) {
            Vec3 fin = finDelRayo(boca, haciaRayo);
            for (Player jugador : this.level().getEntitiesOfClass(Player.class, new AABB(boca, fin).inflate(1.0))) {
                if (atacaA(jugador) && jugador.getBoundingBox().inflate(ANCHO_RAYO).clip(boca, fin).isPresent()) {
                    jugador.invulnerableTime = 0;
                    jugador.hurt(this.damageSources().indirectMagic(this, this), DANO_RAYO);
                    jugador.setSecondsOnFire(4);
                }
            }
        }
        if (ticksAccion >= TICKS_RAYO) {
            empezarRodeo();
        }
    }

    protected Vec3 finDelRayo(Vec3 boca, Vec3 dir) {
        Vec3 fin = boca.add(dir.scale(ALCANCE_RAYO));
        var choque = this.level().clip(new net.minecraft.world.level.ClipContext(boca, fin,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        return choque.getLocation();
    }

    /** Dónde tiene la boca (delante y algo abajo). */
    public Vec3 boca() {
        Vec3 adelante = Vec3.directionFromRotation(this.getXRot(), this.getYRot());
        return this.position().add(0, this.getBbHeight() * 0.4, 0).add(adelante.scale(6.5));
    }

    // ---------- PICADO ----------

    private void elevarse(Vec3 centro) {
        double alto = Math.max(centro.y, suelo(centro)) + ALTURA_PICADO;
        Vec3 encima = new Vec3(centro.x + Math.cos(anguloRodeo) * 8, alto, centro.z + Math.sin(anguloRodeo) * 8);
        nadarHacia(encima, 0.8, 0.07);
        if (ticksAccion == TICKS_SUBIR_PICADO - FIJAR_PICADO) {
            objetivoVuelo = new Vec3(centro.x, suelo(centro), centro.z);   // fija dónde va a caer
            this.playSound(ModuloFe.SONIDO_PEZ_BRAMIDO.get(), 10.0f, 0.7f);
        }
        if (ticksAccion >= TICKS_SUBIR_PICADO) {
            direccion = objetivoVuelo.subtract(this.position()).normalize();
            empezar(PICADO);
        }
    }

    private void picado() {
        this.setDeltaMovement(this.getDeltaMovement().lerp(direccion.scale(VELOCIDAD_PICADO), 0.3));
        golpearAlPasar(DANO_BARRIDO, 2.0, 0.5);
        if (this.getY() <= suelo(this.position()) + 0.6 || ticksAccion > 80) {
            this.setPos(this.getX(), suelo(this.position()), this.getZ());
            this.setDeltaMovement(Vec3.ZERO);
            golpeContraElSuelo();
            empezar(ATURDIDO);
        }
    }

    /** ¡PUM! Gran onda alrededor (el daño baja con la distancia). Las ilusiones solo hacen la parte visual. */
    protected void golpeContraElSuelo() {
        if (!(this.level() instanceof ServerLevel nivel)) return;
        this.playSound(SoundEvents.GENERIC_EXPLODE, 6.0f, 0.5f);
        this.playSound(ModuloFe.SONIDO_PEZ_GOLPE.get(), 8.0f, 1.0f);
        BlockState suelo = this.level().getBlockState(BlockPos.containing(this.position()).below());
        for (int anillo = 1; anillo <= 4; anillo++) {
            double r = RADIO_ONDA * anillo / 4.0;
            for (int i = 0; i < 24 * anillo; i++) {
                double a = i * Math.PI * 2 / (24 * anillo);
                nivel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, suelo), this.getX() + Math.cos(a) * r,
                        this.getY() + 0.2, this.getZ() + Math.sin(a) * r, 2, 0.2, 0.1, 0.2, 0.15);
            }
        }
        nivel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 0.5, this.getZ(), 1, 0, 0, 0, 0);
        nivel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.5, this.getZ(), 60, RADIO_ONDA * 0.4, 0.3, RADIO_ONDA * 0.4, 0.2);
        if (esIlusion()) return;
        for (Player jugador : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(RADIO_ONDA, 3.0, RADIO_ONDA))) {
            double d = jugador.position().distanceTo(this.position());
            if (!atacaA(jugador) || d > RADIO_ONDA) continue;
            float dano = (float) Mth.lerp(d / RADIO_ONDA, DANO_ONDA, 6.0);
            jugador.hurt(this.damageSources().mobAttack(this), dano);
            Vec3 hacia = jugador.position().subtract(this.position()).multiply(1, 0, 1).normalize();
            jugador.knockback(3.0 * (1 - d / RADIO_ONDA) + 0.8, -hacia.x, -hacia.z);
            jugador.setDeltaMovement(jugador.getDeltaMovement().add(0, 0.6, 0));
            empujar(jugador);
        }
    }

    private void aturdido() {
        this.setDeltaMovement(Vec3.ZERO);
        if (ticksAccion % 15 == 0 && this.level() instanceof ServerLevel nivel) {
            nivel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + this.getBbHeight() + 0.5, this.getZ(), 10, 1.5, 0.2, 1.5, 0.05);
        }
        if (ticksAccion == TICKS_ATURDIDO - 20) {
            this.playSound(ModuloFe.SONIDO_PEZ_BRAMIDO.get(), 8.0f, 1.2f);
        }
        if (ticksAccion >= TICKS_ATURDIDO) {
            empezar(SUBIR);
        }
    }

    // ---------- FASES E ILUSIONES ----------

    private void comprobarFase() {
        float vida = this.getHealth() / this.getMaxHealth();
        int nueva = vida > 2.0f / 3.0f ? 1 : vida > 1.0f / 3.0f ? 2 : 3;
        if (nueva > fase) {
            fase = nueva;
            esperaIlusiones = 0;
            barra.setColor(fase == 2 ? BossEvent.BossBarColor.BLUE : BossEvent.BossBarColor.PURPLE);
            if (accion() != PICADO && accion() != ATURDIDO) {
                empezar(DIVIDIRSE);
            }
        }
        limpiarIlusiones();
        int quiero = fase == 2 ? 1 : fase == 3 ? 3 : 0;
        if (ilusiones.size() < quiero && esperaIlusiones == 0 && accion() == RODEAR) {
            empezar(DIVIDIRSE);
        }
    }

    private void limpiarIlusiones() {
        if (!(this.level() instanceof ServerLevel nivel)) return;
        ilusiones.removeIf(id -> !(nivel.getEntity(id) instanceof PezIlusion i) || !i.isAlive());
    }

    /** Brilla, brama y se divide: aparecen las ilusiones que le falten. */
    private void dividirse(Vec3 centro) {
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
        if (ticksAccion == 1) {
            this.playSound(ModuloFe.SONIDO_PEZ_CANTO.get(), 10.0f, 1.3f);
        }
        if (this.level() instanceof ServerLevel nivel && ticksAccion % 3 == 0) {
            nivel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.5, this.getZ(), 15, 3.0, 1.5, 3.0, 0.1);
        }
        if (ticksAccion >= TICKS_DIVIDIRSE && this.level() instanceof ServerLevel nivel) {
            limpiarIlusiones();
            int quiero = fase == 2 ? 1 : fase == 3 ? 3 : 0;
            int total = quiero + 1;
            List<Integer> libres = new ArrayList<>();
            for (int k = 1; k < total; k++) libres.add(k);
            for (UUID id : ilusiones) {
                // las que ya había se recolocan para el nuevo reparto (si no, la de la fase 2, a media vuelta,
                // quedaba justo encima de una de las nuevas)
                if (nivel.getEntity(id) instanceof PezIlusion i) {
                    i.repartir(total);
                    libres.remove((Integer) i.indice());
                }
            }
            for (int k : libres) {
                PezIlusion ilusion = ModuloFe.PEZ_ILUSION.get().create(nivel);
                if (ilusion == null) continue;
                ilusion.copiarDe(this, k, total);
                nivel.addFreshEntity(ilusion);
                ilusiones.add(ilusion.getUUID());
                nivel.sendParticles(ParticleTypes.FLASH, ilusion.getX(), ilusion.getY() + 1.5, ilusion.getZ(), 1, 0, 0, 0, 0);
            }
            this.playSound(ModuloFe.SONIDO_PEZ_ILUSION.get(), 8.0f, 1.0f);
            esperaIlusiones = ESPERA_ILUSIONES;
            empezarRodeo();
        }
    }

    /** Lo llama una ilusión al deshacerse: tardará en volver a sacarla. */
    public void ilusionDeshecha() {
        esperaIlusiones = ESPERA_ILUSIONES;
    }

    /** Efectos visuales que hacen igual él y sus ilusiones (las ilusiones también "disparan", pero sin dañar). */
    protected void efectos() {
        if (!(this.level() instanceof ServerLevel nivel)) return;
        int accion = accion();
        Vec3 boca = boca();
        if (accion == APUNTAR && ticksAccion % 2 == 0) {
            // la luz se concentra en la boca
            for (int i = 0; i < 6; i++) {
                Vec3 desde = boca.add((this.random.nextDouble() - 0.5) * 6, (this.random.nextDouble() - 0.5) * 6, (this.random.nextDouble() - 0.5) * 6);
                Vec3 v = boca.subtract(desde).scale(0.12);
                nivel.sendParticles(ParticleTypes.END_ROD, desde.x, desde.y, desde.z, 0, v.x, v.y, v.z, 1.0);
            }
        }
        if (accion == RAYO) {
            Vec3 dir = haciaRayo.lengthSqr() > 0 ? haciaRayo : Vec3.directionFromRotation(this.getXRot(), this.getYRot());
            Vec3 fin = finDelRayo(boca, dir);
            double largo = fin.distanceTo(boca);
            for (double i = 0; i < largo; i += 0.6) {
                Vec3 p = boca.add(dir.scale(i));
                nivel.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.12, 0.12, 0.12, 0.01);
            }
            nivel.sendParticles(ParticleTypes.FLASH, fin.x, fin.y, fin.z, 1, 0, 0, 0, 0);
        }
        if (accion != ATURDIDO && this.tickCount % 3 == 0) {
            // estela de luz de la cola y de las puntas de las alas
            Vec3 atras = Vec3.directionFromRotation(this.getXRot(), this.getYRot()).scale(-9.0);
            Vec3 lado = Vec3.directionFromRotation(0, this.getYRot() + 90).scale(9.0);
            Vec3 c = this.position().add(0, this.getBbHeight() * 0.5, 0);
            nivel.sendParticles(ParticleTypes.END_ROD, c.x + atras.x, c.y + atras.y, c.z + atras.z, 2, 0.6, 0.3, 0.6, 0.0);
            nivel.sendParticles(ParticleTypes.END_ROD, c.x + lado.x, c.y, c.z + lado.z, 1, 0.3, 0.2, 0.3, 0.0);
            nivel.sendParticles(ParticleTypes.END_ROD, c.x - lado.x, c.y, c.z - lado.z, 1, 0.3, 0.2, 0.3, 0.0);
        }
        if ((accion == BARRIDO || accion == PICADO) && ticksAccion % 2 == 0) {
            nivel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 2.5, this.getZ(), 6, 2.5, 1.5, 2.5, 0.02);
        }
    }

    // ---------- MOVIMIENTO ----------

    /**
     * Se mueve exactamente con la velocidad que le damos, sin el frenado del aire de Minecraft: así controlamos
     * el nado (aceleraciones suaves y curvas amplias) y queda fluido.
     */
    @Override
    public void travel(Vec3 entrada) {
        if (this.isEffectiveAi()) {
            this.move(MoverType.SELF, this.getDeltaMovement());
        }
        this.calculateEntityAnimation(false);
    }

    protected void nadarHacia(Vec3 punto, double velocidadMax, double aceleracion) {
        Vec3 hacia = punto.subtract(this.position());
        Vec3 deseada = hacia.lengthSqr() < 1.0E-4 ? Vec3.ZERO : hacia.normalize().scale(Math.min(velocidadMax, hacia.length() * 0.2));
        Vec3 v = this.getDeltaMovement();
        this.setDeltaMovement(v.add(deseada.subtract(v).scale(aceleracion)));
    }

    /** Mira hacia donde nada (suave), como un pez. */
    protected void orientarSegunVelocidad() {
        int accion = accion();
        if (accion == APUNTAR || accion == RAYO || accion == ATURDIDO) return;
        Vec3 v = this.getDeltaMovement();
        if (v.lengthSqr() < 0.004) return;
        float giro = (float) (Mth.atan2(v.z, v.x) * Mth.RAD_TO_DEG) - 90.0f;
        float cabeceo = (float) (-Mth.atan2(v.y, v.horizontalDistance()) * Mth.RAD_TO_DEG);
        this.setYRot(Mth.rotLerp(0.12f, this.getYRot(), giro));
        this.setXRot(Mth.lerp(0.12f, this.getXRot(), Mth.clamp(cabeceo, -60.0f, 75.0f)));
        this.setYBodyRot(this.getYRot());
        this.setYHeadRot(this.getYRot());
    }

    private void mirarHacia(Vec3 dir, float maximo) {
        float giro = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90.0f;
        float cabeceo = (float) (-Mth.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)) * Mth.RAD_TO_DEG);
        this.setYRot(Mth.approachDegrees(this.getYRot(), giro, maximo));
        this.setXRot(Mth.approach(this.getXRot(), Mth.clamp(cabeceo, -60.0f, 75.0f), maximo));
        this.setYBodyRot(this.getYRot());
        this.setYHeadRot(this.getYRot());
    }

    private static Vec3 girarHacia(Vec3 desde, Vec3 hasta, float maxGrados) {
        double angulo = Math.acos(Mth.clamp(desde.dot(hasta), -1.0, 1.0));
        double max = Math.toRadians(maxGrados);
        if (angulo <= max) return hasta;
        return desde.lerp(hasta, max / angulo).normalize();
    }

    protected double suelo(Vec3 p) {
        return this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(p.x), Mth.floor(p.z));
    }

    private void golpearAlPasar(float dano, double empuje, double arriba) {
        for (Player jugador : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(1.0))) {
            if (!atacaA(jugador) || jugador.invulnerableTime > 0) continue;
            if (jugador.hurt(this.damageSources().mobAttack(this), dano)) {
                Vec3 v = this.getDeltaMovement().multiply(1, 0, 1).normalize();
                jugador.knockback(empuje, -v.x, -v.z);
                jugador.setDeltaMovement(jugador.getDeltaMovement().add(0, arriba, 0));
                empujar(jugador);
            }
        }
    }

    private static void empujar(LivingEntity objetivo) {
        objetivo.hurtMarked = true;
        if (objetivo instanceof ServerPlayer jugador) {
            jugador.connection.send(new ClientboundSetEntityMotionPacket(jugador));
        }
    }

    protected void empezar(int accion) {
        this.entityData.set(ACCION, accion);
        this.ticksAccion = 0;
    }

    // ---------- DAÑO ----------

    @Override
    public boolean hurt(DamageSource fuente, float cantidad) {
        if (Jefe.esFlecha(fuente)) {
            return false;                                   // las flechas le rebotan (como a todos los jefes)
        }
        if (accion() == INTRO && !fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (fuente.is(DamageTypeTags.IS_FALL) || fuente.is(DamageTypeTags.IS_DROWNING)) {
            return false;
        }
        if (accion() == ATURDIDO && !fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            cantidad *= CASTIGO;   // ¡castigo!
        }
        return super.hurt(fuente, cantidad);
    }

    /** Es mucho más largo que su caja: que no desaparezca de la vista antes de tiempo. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(8.0);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fuente) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distancia) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    // ---------- LA MUERTE: sube despacio girando y se deshace en luz ----------

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        this.setDeltaMovement(0.0, 0.05, 0.0);
        this.setYRot(this.getYRot() + 2.0f);
        if (this.level() instanceof ServerLevel nivel) {
            if (this.deathTime % 3 == 0) {
                nivel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.5, this.getZ(), 10, 3.0, 1.5, 3.0, 0.15);
            }
            if (this.deathTime == 1) {
                this.playSound(ModuloFe.SONIDO_PEZ_MUERTE.get(), 12.0f, 1.0f);
                for (UUID id : ilusiones) {
                    if (nivel.getEntity(id) instanceof PezIlusion i) i.deshacer();
                }
            }
            if (this.deathTime >= 100 && !this.isRemoved()) {
                nivel.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 1.5, this.getZ(), 3, 1, 1, 1, 0);
                nivel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.5, this.getZ(), 200, 3.0, 2.0, 3.0, 0.4);
                this.dropExperience();   // la muerte la llevamos nosotros: hay que soltar la experiencia a mano
                this.remove(RemovalReason.KILLED);
            }
        }
    }

    /** Un jefe siempre suelta su experiencia (la muerte dura unos segundos y se pasaría el plazo de "lo mató un jugador"). */
    @Override
    public boolean isAlwaysExperienceDropper() {
        return true;
    }

    // ---------- LA BARRA DE JEFE ----------

    @Override
    public void startSeenByPlayer(ServerPlayer jugador) {
        super.startSeenByPlayer(jugador);
        if (!esIlusion()) barra.addPlayer(jugador);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer jugador) {
        super.stopSeenByPlayer(jugador);
        barra.removePlayer(jugador);
    }

    // ---------- SONIDOS ----------

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return null;   // canta en rodear() (muy de vez en cuando)
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource fuente) {
        return ModuloFe.SONIDO_PEZ_HERIDO.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return null;   // lo suena tickDeath
    }

    @Override
    protected float getSoundVolume() {
        return 6.0f;
    }

    // ---------- GUARDAR ----------

    @Override
    public void addAdditionalSaveData(CompoundTag datos) {
        super.addAdditionalSaveData(datos);
        datos.putLong("Altar", altar.asLong());
        datos.putInt("Fase", fase);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag datos) {
        super.readAdditionalSaveData(datos);
        altar = BlockPos.of(datos.getLong("Altar"));
        fase = Math.max(1, datos.getInt("Fase"));
        if (this.hasCustomName()) {
            barra.setName(this.getDisplayName());
        }
    }
}
