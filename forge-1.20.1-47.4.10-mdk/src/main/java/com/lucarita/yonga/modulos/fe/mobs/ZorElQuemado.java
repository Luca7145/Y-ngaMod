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
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityMountEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * ZOR EL QUEMADO: segundo jefe, de fe oscura (como jefe, ataca a cualquiera). Un dragón larguísimo, como una
 * serpiente (unos 27 bloques): la cabeza es esta entidad y el cuerpo, 14 tramos (ZorParte) que siguen el rastro
 * exacto de la cabeza, así que serpentea, se mete bajo tierra y sale de ella en columna. Se le puede golpear en
 * cualquier tramo (en el cuerpo recibe algo menos que en la cabeza). Se invoca con un ritual (RitualZor).
 *
 * Vuela alrededor de su presa serpenteando y, al azar, hace uno de sus ataques:
 *  - LLAMARADA: se aleja, baja y pasa por encima de ti echando fuego: deja el suelo ardiendo a su paso.
 *  - ENTERRARSE: cae en picado y se mete bajo tierra. Sale polvo y humo justo donde estás tú; tienes unos instantes
 *    para apartarte antes de que salga hacia arriba (a velocidad moderada). Mientras sale, su cuerpo queda a tiro.
 *  - DENTELLADA (menos a menudo): baja a por ti, te agarra con la boca, sube y te suelta desde lo alto. Si le
 *    pegas mientras te lleva, te suelta enseguida.
 * FASE 2 (a media vida): al enterrarse te engaña: el polvo te sigue, se detiene como si fuera a salir ahí... y
 * vuelve a moverse hasta donde estás para salir de verdad. Y escupe BOLAS DE FUEGO como un ghast (más fuertes y
 * rápidas): si le devuelves una de un golpe, le hace mucho daño.
 */
public class ZorElQuemado extends Mob implements MobFe, Jefe {

    // ---------- AJUSTES ----------
    public static final double VIDA = 180.0;                     // 90 corazones (solo se le puede pegar a ratos)
    public static final int TRAMOS = 14;
    public static final double ESPACIO = 1.85;                   // bloques entre tramo y tramo
    private static final float DANO_EN_EL_CUERPO = 0.75f;

    private static final int TICKS_INTRO = 140;
    private static final double RADIO_RODEO = 18.0, ALTURA_RODEO = 14.0;
    private static final double VELOCIDAD = 0.75;
    private static final int RODEO_MIN = 70, RODEO_MAX = 130;

    // Llamarada
    private static final double LEJOS_FUEGO = 24.0, ALTURA_FUEGO = 7.0, VELOCIDAD_FUEGO = 0.85;
    private static final int TICKS_FUEGO = 50;
    private static final float DANO_FUEGO = 9.0f;

    // Enterrarse
    private static final double PROFUNDIDAD = 7.0, VELOCIDAD_BAJO_TIERRA = 0.6, VELOCIDAD_EMERGER = 0.75;
    private static final int TICKS_SEGUIR = 20, TICKS_QUIETO = 28;             // el aviso: te sigue y luego se fija
    private static final int ENGANO_PARADO = 24, ENGANO_MOVERSE = 22, ENGANO_ULTIMO = 12;   // fase 2
    private static final double VELOCIDAD_POLVO = 0.45;
    private static final double RADIO_EMERGER = 2.8;
    private static final float DANO_EMERGER = 16.0f;
    private static final double ALTURA_TRAS_EMERGER = 20.0;

    // Dentellada
    private static final int TICKS_BUSCAR = 70, TICKS_LLEVAR = 30;
    private static final double SUBIDA_LLEVAR = 0.55;
    private static final float DANO_DENTELLADA = 8.0f;
    private static final int ESPERA_DENTELLADA = 500;            // al menos 25 s entre dentellada y dentellada

    // Bolas de fuego (fase 2): como las del ghast, pero más fuertes y más rápidas
    private static final int[] DISPAROS = {20, 36, 52};
    private static final int TICKS_BOLAS = 72;
    private static final int POTENCIA_BOLA = 2;                  // la del ghast es 1
    private static final double RAPIDEZ_BOLA = 1.6;
    private static final float DANO_BOLA_DEVUELTA = 15.0f;       // si se la devuelves de un golpe

    // Pesos de los ataques (al azar, sin repetir el anterior)
    private static final int PESO_FUEGO = 4, PESO_ENTERRARSE = 4, PESO_DENTELLADA = 2, PESO_BOLAS = 3;

    // ---------- ESTADO (sincronizado) ----------
    public static final int INTRO = 0, RODEAR = 1, IR_A_FUEGO = 2, FUEGO = 3, ZAMBULLIRSE = 4, BAJO_TIERRA = 5,
            AVISO = 6, EMERGER = 7, BUSCAR = 8, LLEVAR = 9, RUGIDO = 10, BOLAS = 11;
    private static final EntityDataAccessor<Integer> ACCION = SynchedEntityData.defineId(ZorElQuemado.class, EntityDataSerializers.INT);

    private final ZorParte[] partes = new ZorParte[TRAMOS];
    private final List<Vec3> rastro = new ArrayList<>();       // posiciones del centro de la cabeza (lo último primero)
    private AABB cajaVisible;

    private int ticksAccion;
    private int duracionRodeo;
    private double anguloRodeo;
    private int ultimoAtaque = -1;
    private long ultimaDentellada = -ESPERA_DENTELLADA;
    private Vec3 objetivoVuelo = Vec3.ZERO;
    private Vec3 direccion = Vec3.ZERO;
    private Vec3 polvo = Vec3.ZERO;          // dónde va a salir (lo que ve el jugador)
    private boolean engano;
    private int fase = 1;
    private BlockPos altar = BlockPos.ZERO;
    private final ServerBossEvent barra = new ServerBossEvent(Component.translatable("entity.yonga.zor"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    public int inicioAccionCliente;

    public ZorElQuemado(EntityType<? extends ZorElQuemado> tipo, Level nivel) {
        super(tipo, nivel);
        for (int i = 0; i < TRAMOS; i++) {
            float ancho = i < 3 ? 2.0f : (float) Mth.lerp((i - 3) / (double) (TRAMOS - 4), 2.0, 0.9);
            partes[i] = new ZorParte(this, i, ancho);
        }
        this.setId(this.getId());   // los tramos, con las ids justo detrás de la cabeza
        this.setNoGravity(true);
        this.noPhysics = true;
        this.xpReward = 600;
        this.setPersistenceRequired();
    }

    /** Las ids de los tramos van justo detrás de la de la cabeza (así el cliente y el servidor coinciden). */
    @Override
    public void setId(int id) {
        super.setId(id);
        if (partes == null) return;
        for (int i = 0; i < TRAMOS; i++) {
            if (partes[i] != null) partes[i].setId(id + i + 1);
        }
    }

    public static AttributeSupplier.Builder atributos() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, VIDA)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 96.0);
    }

    @Override
    public boolean esOscuro() {
        return true;
    }

    @Override
    public boolean esSuperior() {
        return true;
    }

    /** Jefe: ataca a cualquiera. */
    @Override
    public boolean atacaA(LivingEntity objetivo) {
        return objetivo instanceof Player p && !p.isCreative() && !p.isSpectator();
    }

    public int accion() {
        return this.entityData.get(ACCION);
    }

    public ZorParte[] tramos() {
        return partes;
    }

    public void invocar(BlockPos altar) {
        this.altar = altar;
        empezar(INTRO);
        this.setHealth(1.0f);
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

    // ---------- EL CUERPO: tramos sobre el rastro de la cabeza ----------

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return partes;
    }

    @Override
    public void tick() {
        super.tick();
        colocarTramos();
    }

    /** Apunta la posición de la cabeza y pone cada tramo a su distancia por el camino que ha seguido. */
    private void colocarTramos() {
        Vec3 cabeza = this.position().add(0, this.getBbHeight() * 0.5, 0);
        if (rastro.isEmpty()) {
            // al aparecer: el cuerpo estirado detrás de la cabeza
            Vec3 atras = Vec3.directionFromRotation(0, this.getYRot()).scale(-1);
            for (int i = 60; i >= 0; i--) rastro.add(cabeza.add(atras.scale(i * 0.5)));
        }
        if (rastro.get(rastro.size() - 1).distanceToSqr(cabeza) > 0.01) {
            rastro.add(cabeza);
        }
        double necesario = (TRAMOS + 2) * ESPACIO;
        // recorta el rastro que ya no hace falta
        double total = 0;
        int corte = 0;
        for (int i = rastro.size() - 1; i > 0; i--) {
            total += rastro.get(i).distanceTo(rastro.get(i - 1));
            if (total > necesario) {
                corte = i - 1;
                break;
            }
        }
        if (corte > 0) rastro.subList(0, corte).clear();

        int tramo = 0;
        double recorrido = 0;
        double siguiente = ESPACIO * 1.1;
        Vec3 anterior = cabeza;
        AABB caja = this.getBoundingBox();
        for (int i = rastro.size() - 2; i >= 0 && tramo < TRAMOS; i--) {
            Vec3 p = rastro.get(i);
            double d = p.distanceTo(anterior);
            while (tramo < TRAMOS && recorrido + d >= siguiente) {
                double f = d <= 0 ? 0 : (siguiente - recorrido) / d;
                Vec3 sitio = anterior.lerp(p, f);
                ponerTramo(partes[tramo], sitio);
                caja = caja.minmax(partes[tramo].getBoundingBox());
                tramo++;
                siguiente += ESPACIO;
            }
            recorrido += d;
            anterior = p;
        }
        while (tramo < TRAMOS) {
            ponerTramo(partes[tramo], anterior);
            tramo++;
        }
        cajaVisible = caja.inflate(2.0);
    }

    private static void ponerTramo(ZorParte parte, Vec3 centro) {
        parte.xo = parte.getX();
        parte.yo = parte.getY();
        parte.zo = parte.getZ();
        parte.xOld = parte.getX();
        parte.yOld = parte.getY();
        parte.zOld = parte.getZ();
        parte.setPos(centro.x, centro.y - parte.getBbHeight() * 0.5, centro.z);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return cajaVisible != null ? cajaVisible : this.getBoundingBox().inflate(30.0);
    }

    /** Golpes en el cuerpo: los recibe Zor (algo menos que en la cabeza). */
    public boolean hurtParte(ZorParte parte, DamageSource fuente, float cantidad) {
        return this.hurt(fuente, cantidad * DANO_EN_EL_CUERPO);
    }

    // ---------- CEREBRO ----------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ticksAccion++;
        if (altar.equals(BlockPos.ZERO)) {
            altar = BlockPos.containing(this.getX(), suelo(this.position()), this.getZ());
        }
        barra.setProgress(this.getHealth() / this.getMaxHealth());
        Player objetivo = elegirObjetivo();
        int accion = accion();
        if (accion == INTRO) {
            intro();
            return;
        }
        if (fase == 1 && this.getHealth() < this.getMaxHealth() * 0.5f) {
            fase = 2;
            barra.setColor(BossEvent.BossBarColor.YELLOW);
            if (accion == RODEAR) {
                empezar(RUGIDO);
                accion = RUGIDO;
            }
        }
        efectos();
        Vec3 centro = objetivo != null ? objetivo.position() : Vec3.atBottomCenterOf(altar);
        switch (accion) {
            case RODEAR -> rodear(centro, objetivo);
            case IR_A_FUEGO -> irAFuego(centro);
            case FUEGO -> llamarada();
            case ZAMBULLIRSE -> zambullirse();
            case BAJO_TIERRA -> bajoTierra(objetivo, centro);
            case AVISO -> aviso(objetivo, centro);
            case EMERGER -> emerger();
            case BUSCAR -> buscar(objetivo, centro);
            case LLEVAR -> llevar();
            case RUGIDO -> rugido();
            case BOLAS -> bolas(centro, objetivo);
            default -> empezarRodeo();
        }
        orientar();
    }

    private Player elegirObjetivo() {
        LivingEntity actual = this.getTarget();
        if (actual instanceof Player p && p.isAlive() && atacaA(p) && p.distanceToSqr(this) < 120 * 120) {
            return p;
        }
        Player cercano = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), 96.0, e -> e instanceof Player p && atacaA(p));
        this.setTarget(cercano);
        return cercano;
    }

    // ---------- INTRO: sale de la tierra bajo el altar, en espiral ----------

    private void intro() {
        this.setInvulnerable(true);
        float p = ticksAccion / (float) TICKS_INTRO;
        this.setHealth(Math.max(1.0f, this.getMaxHealth() * Math.min(1.0f, p * 1.15f)));
        Vec3 base = Vec3.atBottomCenterOf(altar);
        if (this.level() instanceof ServerLevel nivel) {
            if (ticksAccion == 1) {
                nivel.playSound(null, altar, ModuloFe.SONIDO_ZOR_EXCAVAR.get(), SoundSource.HOSTILE, 8.0f, 0.7f);
                titulo(nivel);
            }
            if (ticksAccion < 40) {
                // la tierra tiembla y echa humo
                nivel.sendParticles(ParticleTypes.LARGE_SMOKE, base.x, base.y + 0.5, base.z, 6, 2.0, 0.2, 2.0, 0.03);
                polvoDeSuelo(nivel, base, 8);
            }
            if (ticksAccion == 40) {
                nivel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, base.x, base.y + 1, base.z, 2, 1.0, 0.5, 1.0, 0);
                nivel.sendParticles(ParticleTypes.FLAME, base.x, base.y + 1, base.z, 120, 2.0, 1.0, 2.0, 0.3);
                nivel.sendParticles(ParticleTypes.LAVA, base.x, base.y + 1, base.z, 40, 1.5, 0.5, 1.5, 0.2);
                nivel.playSound(null, altar, ModuloFe.SONIDO_ZOR_EMERGER.get(), SoundSource.HOSTILE, 10.0f, 0.8f);
                nivel.playSound(null, altar, ModuloFe.SONIDO_ZOR_RUGIDO.get(), SoundSource.HOSTILE, 12.0f, 0.8f);
            }
        }
        if (ticksAccion < 40) {
            this.setPos(base.x, base.y - PROFUNDIDAD, base.z);   // aún bajo tierra
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        // sale disparado hacia arriba girando en espiral
        float q = (ticksAccion - 40) / (float) (TICKS_INTRO - 40);
        double angulo = q * Math.PI * 4.0;
        double radio = q * RADIO_RODEO;
        Vec3 punto = base.add(Math.cos(angulo) * radio, 4 + q * (ALTURA_RODEO + 14), Math.sin(angulo) * radio);
        volarHacia(punto, 1.3, 0.25);
        if (ticksAccion >= TICKS_INTRO) {
            this.setInvulnerable(false);
            this.setHealth(this.getMaxHealth());
            empezarRodeo();
        }
    }

    private void titulo(ServerLevel nivel) {
        for (ServerPlayer jugador : nivel.getPlayers(j -> j.distanceToSqr(Vec3.atCenterOf(altar)) < 96 * 96)) {
            jugador.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            jugador.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("yonga.zor.titulo")));
            jugador.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("yonga.zor.subtitulo")));
        }
    }

    // ---------- RODEAR Y ELEGIR ATAQUE ----------

    private void empezarRodeo() {
        empezar(RODEAR);
        duracionRodeo = RODEO_MIN + this.random.nextInt(RODEO_MAX - RODEO_MIN);
    }

    private void rodear(Vec3 centro, Player objetivo) {
        anguloRodeo += 0.035;
        double alto = Math.max(centro.y, suelo(centro)) + ALTURA_RODEO;
        // serpentea: sube y baja y se abre y se cierra mientras da vueltas
        double radio = RADIO_RODEO + Math.sin(this.tickCount * 0.07) * 4.0;
        Vec3 punto = new Vec3(centro.x + Math.cos(anguloRodeo) * radio, alto + Math.sin(this.tickCount * 0.11) * 3.0,
                centro.z + Math.sin(anguloRodeo) * radio);
        volarHacia(punto, VELOCIDAD, 0.08);
        if (this.tickCount % 200 == 0) {
            this.playSound(ModuloFe.SONIDO_ZOR_RUGIDO.get(), 8.0f, 0.9f + this.random.nextFloat() * 0.2f);
        }
        if (objetivo != null && ticksAccion >= duracionRodeo) {
            int ataque = elegirAtaque();
            ultimoAtaque = ataque;
            switch (ataque) {
                case 0 -> {
                    Vec3 fuera = this.position().subtract(centro).multiply(1, 0, 1).normalize().scale(LEJOS_FUEGO);
                    Vec3 inicio = centro.add(fuera.x, 0, fuera.z);
                    objetivoVuelo = new Vec3(inicio.x, suelo(inicio) + ALTURA_FUEGO, inicio.z);
                    empezar(IR_A_FUEGO);
                }
                case 1 -> {
                    // se zambulle a cierta distancia de ti
                    Vec3 lado = this.position().subtract(centro).multiply(1, 0, 1).normalize().scale(12.0);
                    Vec3 entrada = centro.add(lado.x, 0, lado.z);
                    objetivoVuelo = new Vec3(entrada.x, suelo(entrada) - PROFUNDIDAD, entrada.z);
                    engano = fase >= 2 && this.random.nextFloat() < 0.75f;
                    this.playSound(ModuloFe.SONIDO_ZOR_RUGIDO.get(), 10.0f, 0.7f);
                    empezar(ZAMBULLIRSE);
                }
                case 2 -> {
                    ultimaDentellada = this.level().getGameTime();
                    this.playSound(ModuloFe.SONIDO_ZOR_RUGIDO.get(), 8.0f, 1.2f);
                    empezar(BUSCAR);
                }
                default -> {
                    this.playSound(ModuloFe.SONIDO_ZOR_RUGIDO.get(), 8.0f, 1.0f);
                    empezar(BOLAS);
                }
            }
        }
    }

    /** 0 llamarada, 1 enterrarse, 2 dentellada (no muy seguido), 3 bolas de fuego (solo en la fase 2). */
    private int elegirAtaque() {
        boolean dentellada = this.level().getGameTime() - ultimaDentellada > ESPERA_DENTELLADA;
        int[] pesos = {PESO_FUEGO, PESO_ENTERRARSE, dentellada ? PESO_DENTELLADA : 0, fase >= 2 ? PESO_BOLAS : 0};
        if (ultimoAtaque >= 0) pesos[ultimoAtaque] = 0;
        int total = 0;
        for (int p : pesos) total += p;
        if (total <= 0) return 0;
        int r = this.random.nextInt(total);
        for (int i = 0; i < pesos.length; i++) {
            if (r < pesos[i]) return i;
            r -= pesos[i];
        }
        return 0;
    }

    // ---------- BOLAS DE FUEGO (fase 2) ----------

    /** Se queda a distancia, mirándote, y escupe tres bolas de fuego (que puedes devolverle de un golpe). */
    private void bolas(Vec3 centro, Player objetivo) {
        anguloRodeo += 0.012;
        double alto = Math.max(centro.y, suelo(centro)) + ALTURA_RODEO - 2.0;
        Vec3 punto = new Vec3(centro.x + Math.cos(anguloRodeo) * (RADIO_RODEO + 4), alto, centro.z + Math.sin(anguloRodeo) * (RADIO_RODEO + 4));
        volarHacia(punto, 0.35, 0.1);
        if (objetivo == null) {
            empezarRodeo();
            return;
        }
        Vec3 boca = boca();
        if (this.level() instanceof ServerLevel nivel && ticksAccion % 2 == 0) {
            nivel.sendParticles(ParticleTypes.FLAME, boca.x, boca.y, boca.z, 4, 0.3, 0.3, 0.3, 0.02);
        }
        for (int disparo : DISPAROS) {
            if (ticksAccion == disparo - 8) {
                this.playSound(SoundEvents.GHAST_WARN, 4.0f, 0.6f);
            }
            if (ticksAccion == disparo) {
                Vec3 hacia = objetivo.getEyePosition().subtract(boca);
                net.minecraft.world.entity.projectile.LargeFireball bola = new net.minecraft.world.entity.projectile.LargeFireball(
                        this.level(), this, hacia.x, hacia.y, hacia.z, POTENCIA_BOLA);
                bola.setPos(boca.x, boca.y, boca.z);
                bola.xPower *= RAPIDEZ_BOLA;
                bola.yPower *= RAPIDEZ_BOLA;
                bola.zPower *= RAPIDEZ_BOLA;
                this.level().addFreshEntity(bola);
                this.playSound(SoundEvents.GHAST_SHOOT, 6.0f, 0.5f);
                this.playSound(ModuloFe.SONIDO_ZOR_FUEGO.get(), 4.0f, 1.4f);
            }
        }
        if (ticksAccion >= TICKS_BOLAS) {
            empezarRodeo();
        }
    }

    private void rugido() {
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
        if (ticksAccion == 1) {
            this.playSound(ModuloFe.SONIDO_ZOR_RUGIDO.get(), 12.0f, 0.6f);
        }
        if (this.level() instanceof ServerLevel nivel && ticksAccion % 2 == 0) {
            Vec3 c = this.position().add(0, 1, 0);
            nivel.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 20, 2.0, 2.0, 2.0, 0.2);
        }
        if (ticksAccion >= 40) empezarRodeo();
    }

    // ---------- LLAMARADA ----------

    private void irAFuego(Vec3 centro) {
        volarHacia(objetivoVuelo, 1.0, 0.1);
        if (this.position().distanceTo(objetivoVuelo) < 4.0 || ticksAccion > 140) {
            direccion = centro.subtract(this.position()).multiply(1, 0, 1).normalize();
            this.playSound(ModuloFe.SONIDO_ZOR_FUEGO.get(), 8.0f, 1.0f);
            empezar(FUEGO);
        }
    }

    private void llamarada() {
        double alto = suelo(this.position()) + ALTURA_FUEGO;
        Vec3 v = new Vec3(direccion.x * VELOCIDAD_FUEGO, (alto - this.getY()) * 0.15, direccion.z * VELOCIDAD_FUEGO);
        this.setDeltaMovement(this.getDeltaMovement().lerp(v, 0.3));
        if (ticksAccion % 20 == 0) {
            this.playSound(ModuloFe.SONIDO_ZOR_FUEGO.get(), 8.0f, 0.9f + this.random.nextFloat() * 0.2f);
        }
        if (!(this.level() instanceof ServerLevel nivel)) return;
        // el chorro de fuego, de la boca al suelo
        Vec3 boca = boca();
        Vec3 suelo = new Vec3(boca.x + direccion.x * 3, suelo(boca.add(direccion.scale(3))), boca.z + direccion.z * 3);
        for (int i = 0; i < 10; i++) {
            Vec3 p = boca.lerp(suelo, i / 10.0);
            nivel.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 3, 0.4, 0.4, 0.4, 0.05);
        }
        nivel.sendParticles(ParticleTypes.LARGE_SMOKE, suelo.x, suelo.y + 0.5, suelo.z, 3, 1.0, 0.3, 1.0, 0.02);
        // el suelo arde (si las reglas del mundo dejan que los mobs rompan cosas)
        if (ticksAccion % 2 == 0 && ForgeEventFactory.getMobGriefingEvent(nivel, this)) {
            for (int k = 0; k < 3; k++) {
                BlockPos sitio = BlockPos.containing(suelo.x + (this.random.nextDouble() - 0.5) * 4, suelo.y, suelo.z + (this.random.nextDouble() - 0.5) * 4);
                if (nivel.getBlockState(sitio).isAir() && BaseFireBlock.canBePlacedAt(nivel, sitio, net.minecraft.core.Direction.UP)) {
                    nivel.setBlockAndUpdate(sitio, BaseFireBlock.getState(nivel, sitio));
                }
            }
        }
        for (Player jugador : nivel.getEntitiesOfClass(Player.class, new AABB(suelo, suelo).inflate(2.5, 3.0, 2.5))) {
            if (atacaA(jugador) && jugador.hurt(this.damageSources().mobAttack(this), DANO_FUEGO)) {
                jugador.setSecondsOnFire(5);
            }
        }
        if (ticksAccion >= TICKS_FUEGO) {
            objetivoVuelo = this.position().add(this.getDeltaMovement().scale(15)).add(0, ALTURA_RODEO, 0);
            empezarRodeo();
        }
    }

    // ---------- ENTERRARSE Y SALIR ----------

    private void zambullirse() {
        volarHacia(objetivoVuelo, 1.4, 0.18);
        double suelo = suelo(this.position());
        if (this.level() instanceof ServerLevel nivel && Math.abs(this.getY() + this.getBbHeight() * 0.5 - suelo) < 1.5) {
            polvoDeSuelo(nivel, this.position(), 10);
            if (ticksAccion % 4 == 0) this.playSound(ModuloFe.SONIDO_ZOR_EXCAVAR.get(), 6.0f, 1.0f);
        }
        if (this.getY() < suelo - PROFUNDIDAD + 1.5 || ticksAccion > 120) {
            empezar(BAJO_TIERRA);
        }
    }

    /** Bajo tierra: va a por ti hasta estar debajo; entonces empieza el aviso (polvo y humo donde vas a estar). */
    private void bajoTierra(Player objetivo, Vec3 centro) {
        Vec3 debajo = new Vec3(centro.x, suelo(centro) - PROFUNDIDAD, centro.z);
        volarHacia(debajo, VELOCIDAD_BAJO_TIERRA, 0.15);
        if (ticksAccion % 10 == 0) this.playSound(ModuloFe.SONIDO_ZOR_EXCAVAR.get(), 3.0f, 0.6f);
        if (this.position().multiply(1, 0, 1).distanceTo(debajo.multiply(1, 0, 1)) < 3.0 || ticksAccion > 80) {
            polvo = new Vec3(centro.x, suelo(centro), centro.z);
            empezar(AVISO);
        }
    }

    /**
     * El aviso: el polvo te sigue un momento y se queda quieto (tienes tiempo de apartarte). En la fase 2 te engaña:
     * se queda quieto, pero luego vuelve a moverse hasta donde estés y sale ahí.
     */
    private void aviso(Player objetivo, Vec3 centro) {
        int t = ticksAccion;
        boolean seMueve;
        int total;
        if (!engano) {
            seMueve = t < TICKS_SEGUIR;
            total = TICKS_SEGUIR + TICKS_QUIETO;
        } else {
            int fin1 = TICKS_SEGUIR, fin2 = fin1 + ENGANO_PARADO, fin3 = fin2 + ENGANO_MOVERSE;
            seMueve = t < fin1 || (t >= fin2 && t < fin3);
            total = fin3 + ENGANO_ULTIMO;
        }
        if (seMueve && objetivo != null) {
            Vec3 hacia = new Vec3(centro.x - polvo.x, 0, centro.z - polvo.z);
            double v = engano && t >= TICKS_SEGUIR ? VELOCIDAD_POLVO * 1.6 : VELOCIDAD_POLVO;
            if (hacia.length() > v) hacia = hacia.normalize().scale(v);
            polvo = polvo.add(hacia);
            polvo = new Vec3(polvo.x, suelo(polvo), polvo.z);
        }
        // la cabeza espera debajo del polvo
        this.setDeltaMovement(new Vec3(polvo.x - this.getX(), (polvo.y - PROFUNDIDAD) - this.getY(), polvo.z - this.getZ()).scale(0.3));
        if (this.level() instanceof ServerLevel nivel) {
            polvoDeSuelo(nivel, polvo, seMueve ? 6 : 10);
            nivel.sendParticles(ParticleTypes.LARGE_SMOKE, polvo.x, polvo.y + 0.3, polvo.z, seMueve ? 2 : 4, 0.8, 0.1, 0.8, 0.02);
            if (t % 8 == 0) this.playSound(ModuloFe.SONIDO_ZOR_EXCAVAR.get(), 4.0f, seMueve ? 1.0f : 0.7f);
        }
        if (t >= total) {
            direccion = new Vec3(0, 1, 0);
            this.setPos(polvo.x, polvo.y - PROFUNDIDAD, polvo.z);
            empezar(EMERGER);
            this.playSound(ModuloFe.SONIDO_ZOR_EMERGER.get(), 10.0f, 1.0f);
        }
    }

    /** ¡Sale hacia arriba! A velocidad moderada: mientras sale, su cuerpo pasa por el agujero y se le puede pegar. */
    private void emerger() {
        this.setDeltaMovement(new Vec3((polvo.x - this.getX()) * 0.2, VELOCIDAD_EMERGER, (polvo.z - this.getZ()) * 0.2));
        double suelo = polvo.y;
        double cabeza = this.getY() + this.getBbHeight() * 0.5;
        if (this.level() instanceof ServerLevel nivel) {
            if (cabeza > suelo - 1 && cabeza < suelo + 3) {
                nivel.sendParticles(ParticleTypes.FLAME, polvo.x, suelo + 0.5, polvo.z, 20, 1.5, 0.5, 1.5, 0.15);
                polvoDeSuelo(nivel, polvo, 20);
                for (Player jugador : nivel.getEntitiesOfClass(Player.class, new AABB(polvo, polvo).inflate(RADIO_EMERGER, 3.0, RADIO_EMERGER))) {
                    if (atacaA(jugador) && jugador.hurt(this.damageSources().mobAttack(this), DANO_EMERGER)) {
                        jugador.setDeltaMovement(jugador.getDeltaMovement().add(0, 1.1, 0));
                        jugador.setSecondsOnFire(4);
                        empujar(jugador);
                    }
                }
            }
            // mientras el cuerpo sigue saliendo por el agujero
            if (ticksAccion % 3 == 0) {
                nivel.sendParticles(ParticleTypes.LARGE_SMOKE, polvo.x, suelo + 0.5, polvo.z, 4, 1.2, 0.2, 1.2, 0.03);
            }
        }
        if (this.getY() > suelo + ALTURA_TRAS_EMERGER) {
            empezarRodeo();
        }
    }

    // ---------- DENTELLADA ----------

    private void buscar(Player objetivo, Vec3 centro) {
        if (objetivo == null) {
            empezarRodeo();
            return;
        }
        volarHacia(objetivo.position().add(0, objetivo.getBbHeight() * 0.5, 0), 1.2, 0.2);
        if (this.getBoundingBox().inflate(0.6).intersects(objetivo.getBoundingBox()) && objetivo.startRiding(this, true)) {
            objetivo.hurt(this.damageSources().mobAttack(this), DANO_DENTELLADA);
            this.playSound(ModuloFe.SONIDO_ASTADO_MORDISCO.get(), 4.0f, 0.6f);
            empezar(LLEVAR);
        } else if (ticksAccion > TICKS_BUSCAR) {
            empezarRodeo();
        }
    }

    private void llevar() {
        Vec3 adelante = Vec3.directionFromRotation(0, this.getYRot());
        this.setDeltaMovement(adelante.x * 0.4, SUBIDA_LLEVAR, adelante.z * 0.4);
        if (ticksAccion >= TICKS_LLEVAR || this.getFirstPassenger() == null) {
            soltar();
        }
    }

    private void soltar() {
        empezarRodeo();                 // primero cambia de acción: así el evento de desmontar ya no lo impide
        this.ejectPassengers();
        this.playSound(ModuloFe.SONIDO_ZOR_RUGIDO.get(), 8.0f, 1.3f);
    }

    @Override
    protected void positionRider(Entity pasajero, MoveFunction mover) {
        Vec3 boca = boca();
        mover.accept(pasajero, boca.x, boca.y - pasajero.getBbHeight() * 0.6, boca.z);
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

    @Override
    public boolean canRiderInteract() {
        return true;
    }

    public static void alMontarODesmontar(EntityMountEvent evento) {
        if (evento.isDismounting() && !evento.getEntityMounting().level().isClientSide()
                && evento.getEntityBeingMounted() instanceof ZorElQuemado zor && zor.isAlive() && !zor.isRemoved()
                && zor.accion() == LLEVAR && zor.ticksAccion < TICKS_LLEVAR) {
            evento.setCanceled(true);
        }
    }

    // ---------- EFECTOS Y UTILIDADES ----------

    /** Ascuas que caen del cuerpo y humo de las fauces. */
    private void efectos() {
        if (!(this.level() instanceof ServerLevel nivel) || this.tickCount % 4 != 0) return;
        Vec3 boca = boca();
        nivel.sendParticles(ParticleTypes.SMOKE, boca.x, boca.y, boca.z, 2, 0.2, 0.2, 0.2, 0.01);
        ZorParte p = partes[this.random.nextInt(TRAMOS)];
        if (p.getY() > suelo(p.position()) + 0.5) {
            nivel.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + p.getBbHeight() * 0.5, p.getZ(), 1, 0.3, 0.3, 0.3, 0.01);
            nivel.sendParticles(ParticleTypes.ASH, p.getX(), p.getY() + p.getBbHeight() * 0.5, p.getZ(), 3, 0.5, 0.5, 0.5, 0.0);
        }
    }

    private static void polvoDeSuelo(ServerLevel nivel, Vec3 donde, int cantidad) {
        BlockPos debajo = BlockPos.containing(donde.x, donde.y - 0.5, donde.z);
        BlockState suelo = nivel.getBlockState(debajo);
        if (suelo.isAir()) suelo = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
        nivel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, suelo), donde.x, donde.y + 0.2, donde.z,
                cantidad, 1.0, 0.1, 1.0, 0.15);
    }

    public Vec3 boca() {
        Vec3 adelante = Vec3.directionFromRotation(this.getXRot(), this.getYRot());
        return this.position().add(0, this.getBbHeight() * 0.5, 0).add(adelante.scale(2.2));
    }

    private void volarHacia(Vec3 punto, double velocidadMax, double aceleracion) {
        Vec3 hacia = punto.subtract(this.position());
        Vec3 deseada = hacia.lengthSqr() < 1.0E-4 ? Vec3.ZERO : hacia.normalize().scale(Math.min(velocidadMax, hacia.length() * 0.25));
        Vec3 v = this.getDeltaMovement();
        this.setDeltaMovement(v.add(deseada.subtract(v).scale(aceleracion)));
    }

    private void orientar() {
        Vec3 v = this.getDeltaMovement();
        if (accion() == BOLAS && this.getTarget() != null) {
            v = this.getTarget().getEyePosition().subtract(this.position().add(0, this.getBbHeight() * 0.5, 0)).normalize();
        }
        if (v.lengthSqr() < 0.003) return;
        float giro = (float) (Mth.atan2(v.z, v.x) * Mth.RAD_TO_DEG) - 90.0f;
        float cabeceo = (float) (-Mth.atan2(v.y, v.horizontalDistance()) * Mth.RAD_TO_DEG);
        this.setYRot(Mth.rotLerp(0.25f, this.getYRot(), giro));
        this.setXRot(Mth.lerp(0.25f, this.getXRot(), Mth.clamp(cabeceo, -85.0f, 85.0f)));
        this.setYBodyRot(this.getYRot());
        this.setYHeadRot(this.getYRot());
    }

    private double suelo(Vec3 p) {
        return this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(p.x), Mth.floor(p.z));
    }

    private static void empujar(LivingEntity objetivo) {
        objetivo.hurtMarked = true;
        if (objetivo instanceof ServerPlayer jugador) {
            jugador.connection.send(new ClientboundSetEntityMotionPacket(jugador));
        }
    }

    private void empezar(int accion) {
        this.entityData.set(ACCION, accion);
        this.ticksAccion = 0;
    }

    /** Se mueve exactamente con la velocidad que le damos (sin frenado del aire) y atraviesa la tierra. */
    @Override
    public void travel(Vec3 entrada) {
        if (this.isEffectiveAi()) {
            this.move(MoverType.SELF, this.getDeltaMovement());
        }
        this.calculateEntityAnimation(false);
    }

    // ---------- DAÑO ----------

    @Override
    public boolean hurt(DamageSource fuente, float cantidad) {
        if (accion() == INTRO && !fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (fuente.getEntity() == this) {
            return false;                                   // sus propias bolas de fuego no le hacen nada
        }
        boolean hecho;
        if (fuente.is(DamageTypes.FIREBALL) && fuente.getEntity() instanceof Player jugador) {
            // ¡le has devuelto una bola de fuego de un golpe! (no le quema, pero el impacto le duele mucho)
            hecho = super.hurt(this.damageSources().playerAttack(jugador), DANO_BOLA_DEVUELTA);
        } else if (fuente.is(DamageTypes.IN_WALL) || fuente.is(DamageTypeTags.IS_FALL) || fuente.is(DamageTypeTags.IS_FIRE)
                || fuente.is(DamageTypeTags.IS_DROWNING)) {
            return false;
        } else {
            hecho = super.hurt(fuente, cantidad);
        }
        // si te lleva en la boca y le pegas, te suelta (así no te deja caer desde tan alto)
        if (hecho && accion() == LLEVAR && !this.level().isClientSide()) {
            soltar();
        }
        return hecho;
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
    public boolean removeWhenFarAway(double distancia) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    // ---------- LA MUERTE: se derrumba envuelto en llamas y se deshace en ceniza ----------

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        this.setDeltaMovement(0.0, -0.08, 0.0);
        this.move(MoverType.SELF, this.getDeltaMovement());
        colocarTramos();
        if (this.level() instanceof ServerLevel nivel) {
            if (this.deathTime == 1) {
                this.playSound(ModuloFe.SONIDO_ZOR_MUERTE.get(), 12.0f, 1.0f);
            }
            ZorParte p = partes[this.deathTime % TRAMOS];
            nivel.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 12, 0.8, 0.8, 0.8, 0.05);
            nivel.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 6, 0.8, 0.8, 0.8, 0.02);
            if (this.deathTime % 10 == 0) {
                nivel.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
                this.playSound(SoundEvents.GENERIC_EXPLODE, 3.0f, 0.7f);
            }
            if (this.deathTime >= 120 && !this.isRemoved()) {
                for (ZorParte parte : partes) {
                    nivel.sendParticles(ParticleTypes.ASH, parte.getX(), parte.getY() + 1, parte.getZ(), 40, 1.0, 1.0, 1.0, 0.1);
                }
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
        barra.addPlayer(jugador);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer jugador) {
        super.stopSeenByPlayer(jugador);
        barra.removePlayer(jugador);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource fuente) {
        return ModuloFe.SONIDO_ZOR_HERIDO.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected float getSoundVolume() {
        return 6.0f;
    }

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
    }
}
