package com.lucarita.yonga.modulos.fe.mobs.rey;

import com.lucarita.yonga.modulos.fe.ModuloFe;
import com.lucarita.yonga.modulos.fe.mobs.Jefe;
import com.lucarita.yonga.modulos.fe.mobs.MobFe;
import com.lucarita.yonga.modulos.fe.red.AgarreReyPacket;
import com.lucarita.yonga.modulos.fe.red.FeRed;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * EL REY DE ARENISCA: tercer jefe, de fe luminosa (como jefe, ataca a cualquiera). Un centinela de arenisca de
 * 44 bloques de alto, con cabeza de calabaza y corona de oro. Al estilo de Shadow of the Colossus, no se le hace
 * daño a espadazos en el cuerpo: hay que ESCALARLO hasta sus tres SELLOS (los discos de oro que brillan) y
 * golpearlos. Las flechas le rebotan, como a todos los jefes.
 *
 * ESCALAR: como el centinela, se le castiga tras el RAYO: al terminarlo cae de rodillas, agotado, un rato, y solo
 * entonces puedes agarrarte (clic derecho en una zona con asideros: muescas y raíces secas). Colgado: W/S subes y
 * bajas, A/D vas de lado; de pie (hombros, coronilla, su espalda encorvada) andas adonde mires. Mayús te suelta.
 * Una vez arriba sigues agarrado aunque se levante, hasta que te eche; entonces hay que esperar al siguiente rayo.
 * Al romperle un sello se levanta de golpe y hace la SACUDIDA FUERTE: os echa a todos, sí o sí.
 *
 * PARA ECHARTE, se levanta y hace (de una en una, nunca a la vez) una de estas tres cosas:
 *  - SACUDIDA: tiembla un poco y se sacude. Si te mueves mientras se sacude, sales despedido.
 *  - ESTALLIDO: la piedra se agrieta y brilla justo donde está uno de los que lleva encima y, poco después,
 *    revienta: si sigues ahí, te hace daño y te echa. Hay que apartarse trepando.
 *  - SACUDIDA FUERTE (menos a menudo): un temblor más largo y una sacudida brutal que echa a todos, sí o sí.
 * Si te echa, la caída duele pero no te mata (como mucho 4 corazones); si te sueltas tú, la caída es normal.
 *
 * LOS SELLOS: entre los tres son toda su vida (cada uno, un tercio).
 *  1. En la parte de atrás de la espinilla derecha.
 *  2. En la espalda.
 *  3. En la coronilla, dentro de la corona: solo se abre cuando los otros dos están rotos. Es el golpe final.
 *
 * SUS ATAQUES (a quien esté en el suelo): PISOTÓN, BARRIDO (pasa la mano por el suelo), PALMADA (planta la mano),
 * RAYO (de los ojos, que se arrastra hacia ti; lo lanza a menudo: es tu oportunidad). Y si te pones a su espalda:
 * COZ hacia atrás, TEMBLOR (salta y cae con una onda de arena alrededor) y se gira mucho más rápido. Al andar,
 * cuidado con que te pise.
 *
 * VARIOS JUGADORES: los sellos aguantan más según cuántos jugadores haya cerca (Jefe.multiplicador).
 */
public class ReyDeArenisca extends Mob implements MobFe, Jefe {

    // ---------- AJUSTES ----------
    public static final float VIDA_SELLO = 80.0f;
    public static final double VIDA = VIDA_SELLO * 3;
    public static final int INTRO = 0, REPOSO = 1, CAMINAR = 2, PISOTON = 3, BARRIDO = 4, PALMADA = 5, RAYO = 6,
            COZ = 7, SACUDIDA = 8, RUGIDO = 9, TEMBLOR = 10, SACUDIDA_FUERTE = 11;
    public static final int TICKS_INTRO = 200;
    public static final float TICKS_SACUDIDA = 60, AVISO_SACUDIDA = 20, AVISO_FUERTE = 35;
    private static final int TICKS_PISOTON = 75, TICKS_BARRIDO = 80, TICKS_PALMADA = 150, TICKS_RAYO = 100,
            TICKS_COZ = 55, TICKS_TEMBLOR = 70, TICKS_RUGIDO = 60;
    private static final double VELOCIDAD = 0.13;
    private static final float GIRO = 1.2f, GIRO_ESPALDA = 3.0f;  // grados por tick (más rápido si estás detrás)
    private static final int ESPERA_DEFENSA = 90, ESPERA_DEFENSA_PRIMERA = 50;   // entre una forma de echarte y otra
    private static final int ESPERA_FUERTE = 700;                 // la sacudida fuerte, como mucho cada 35 s
    private static final int AVISO_ESTALLIDO = 40;
    private static final int TICKS_AGOTADO = 260;                 // de rodillas tras el rayo: ahí se le escala
    private static final int RAYOS_CADA = 2;                      // como mucho, cada tantos ataques lanza el rayo
    private static final double RADIO_ESTALLIDO = 4.0;
    private static final float DANO_PISOTON = 16, DANO_PISADA = 10, DANO_BARRIDO = 16, DANO_PALMADA = 22,
            DANO_ONDA_PALMADA = 12, DANO_RAYO = 7, DANO_COZ = 15, DANO_TEMBLOR = 14, DANO_ESTALLIDO = 8;
    private static final double RADIO_TEMBLOR = 15.0;
    /** Lo más que duele caer cuando te ha echado él (en medios corazones). */
    private static final float CAIDA_MAXIMA = 8.0f;
    private static final String MARCA_LANZADO = "yonga_rey_lanzado";
    private static final double VELOCIDAD_RAYO = 0.3;
    private static final DustParticleOptions ARENA = new DustParticleOptions(new Vector3f(0.86f, 0.78f, 0.55f), 2.5f);

    // ---------- ESTADO SINCRONIZADO ----------
    private static final EntityDataAccessor<Integer> ACCION = SynchedEntityData.defineId(ReyDeArenisca.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LADO = SynchedEntityData.defineId(ReyDeArenisca.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RODILLAS = SynchedEntityData.defineId(ReyDeArenisca.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SELLOS = SynchedEntityData.defineId(ReyDeArenisca.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> MIRA = SynchedEntityData.defineId(ReyDeArenisca.class, EntityDataSerializers.VECTOR3);

    private final ReyParte[] partes = new ReyParte[EsqueletoRey.N + 3];
    public final EsqueletoRey esqueleto = new EsqueletoRey();
    private AABB cajaVisible;

    private int ticksAccion;
    public int inicioAccionCliente;
    private float rodillas, rodillasO, andar, andarO, paso, pasoO;

    // servidor
    private final float[] vidaSello = {VIDA_SELLO, VIDA_SELLO, VIDA_SELLO};
    private final Map<UUID, AgarreRey.Agarre> agarres = new HashMap<>();
    private final Set<UUID> golpeados = new HashSet<>();
    private boolean golpeandoSello;
    private int rodillasRestantes, ticksSinAtacar, espera = 60, ataquesSinRayo, esperaDefensa = ESPERA_DEFENSA_PRIMERA;
    private long ultimaFuerte = -ESPERA_FUERTE;
    private float maxSello = VIDA_SELLO;                 // crece con el número de jugadores
    private int estallido = -1, estallidoSuperficie;
    private float estallidoU, estallidoV;
    private Vec3 rayoPunto = Vec3.ZERO;
    private BlockPos altar = BlockPos.ZERO;
    private final ServerBossEvent barra = new ServerBossEvent(Component.translatable("entity.yonga.rey_arenisca"),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);

    // cliente: los agarres que nos cuenta el servidor (por id de jugador)
    private final Map<Integer, AgarreRey.Agarre> agarresCliente = new HashMap<>();

    public ReyDeArenisca(EntityType<? extends ReyDeArenisca> tipo, Level nivel) {
        super(tipo, nivel);
        for (int b = 0; b < EsqueletoRey.N; b++) {
            partes[b] = new ReyParte(this, b, -1);
        }
        for (int s = 0; s < 3; s++) {
            partes[EsqueletoRey.N + s] = new ReyParte(this, -1, s);
        }
        this.setId(this.getId());
        this.setNoGravity(true);
        this.noPhysics = true;
        this.xpReward = 800;
        this.setPersistenceRequired();
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (partes == null) return;
        for (int i = 0; i < partes.length; i++) {
            if (partes[i] != null) partes[i].setId(id + i + 1);
        }
    }

    public static AttributeSupplier.Builder atributos() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, VIDA)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 100.0);
    }

    @Override
    public boolean esOscuro() {
        return false;
    }

    @Override
    public boolean esSuperior() {
        return true;
    }

    @Override
    public boolean atacaA(LivingEntity objetivo) {
        return objetivo instanceof Player p && !p.isCreative() && !p.isSpectator();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ACCION, REPOSO);
        this.entityData.define(LADO, 1);
        this.entityData.define(RODILLAS, false);
        this.entityData.define(SELLOS, 0);
        this.entityData.define(MIRA, new Vector3f());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> dato) {
        super.onSyncedDataUpdated(dato);
        if (ACCION.equals(dato)) {
            this.inicioAccionCliente = this.tickCount;
        }
    }

    public int accion() {
        return this.entityData.get(ACCION);
    }

    public int lado() {
        return this.entityData.get(LADO);
    }

    public boolean selloRoto(int i) {
        return (this.entityData.get(SELLOS) & (1 << i)) != 0;
    }

    /** Los dos primeros están siempre abiertos; el de la coronilla, solo con los otros dos rotos. */
    public boolean selloAbierto(int i) {
        return !selloRoto(i) && (i < 2 || (selloRoto(0) && selloRoto(1)));
    }

    public void invocar(BlockPos altar) {
        this.altar = altar;
        empezar(INTRO);
    }

    // ---------- EL ESQUELETO Y LAS PARTES ----------

    public float tiempoAccion(float parcial) {
        int base = this.level().isClientSide() ? this.tickCount - inicioAccionCliente : ticksAccion;
        return Math.max(0.0f, base - 1 + parcial);
    }

    public EsqueletoRey.Estado estado(float parcial) {
        double x = Mth.lerp(parcial, this.xo, this.getX());
        double y = Mth.lerp(parcial, this.yo, this.getY());
        double z = Mth.lerp(parcial, this.zo, this.getZ());
        float giro = Mth.rotLerp(parcial, this.yRotO, this.getYRot());
        float rod = Mth.lerp(parcial, rodillasO, rodillas);
        int accion = accion();
        float t = tiempoAccion(parcial);
        float muerte = this.deathTime > 0 ? this.deathTime - 1 + parcial : 0;
        // adónde mira la cabeza (relativo al rey)
        float miraGiro = 0, miraCabeceo = 0;
        Vector3f m = this.entityData.get(MIRA);
        if (m.lengthSquared() > 0.01f && muerte <= 0) {
            double hx = m.x - x, hy = m.y - (y + 37 - 8.5 * rod), hz = m.z - z;
            double a = giro * Mth.DEG_TO_RAD;
            double lx = hx * Math.cos(a) + hz * Math.sin(a);
            double lz = -hx * Math.sin(a) + hz * Math.cos(a);
            miraGiro = (float) Math.atan2(lx, lz);
            miraCabeceo = (float) -Math.atan2(hy, Math.sqrt(lx * lx + lz * lz));
        }
        float bajada = 0;
        if (accion == INTRO) {
            float p = Mth.clamp(t / TICKS_INTRO, 0, 1);
            bajada = (1 - p * p * (3 - 2 * p)) * (EsqueletoRey.ALTURA + 4);
        }
        if (accion == TEMBLOR && t > 20 && t < 40) {
            bajada = -4.5f * Mth.sin((float) Math.PI * (t - 20) / 20.0f);      // ¡salta!
        }
        if (muerte > 120) {
            bajada = (muerte - 120) / 40.0f * 8.0f;
        }
        return new EsqueletoRey.Estado(x, y, z, giro, accion, t, lado(), rod, Mth.lerp(parcial, andarO, andar),
                Mth.lerp(parcial, pasoO, paso), muerte, miraGiro, miraCabeceo, bajada);
    }

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
        rodillasO = rodillas;
        rodillas = acercar(rodillas, this.entityData.get(RODILLAS) ? 1 : 0, 1.0f / 40);
        andarO = andar;
        andar = acercar(andar, accion() == CAMINAR ? 1 : 0, 0.08f);
        pasoO = paso;
        double dx = this.getX() - this.xo, dz = this.getZ() - this.zo;
        paso += (float) Math.sqrt(dx * dx + dz * dz) * 0.31f;
        colocarPartes();
        if (!this.level().isClientSide()) {
            if (Mth.floor(paso / Math.PI) != Mth.floor(pasoO / Math.PI)) {
                pisada(Mth.floor(paso / Math.PI) % 2 == 0 ? EsqueletoRey.PIE_I : EsqueletoRey.PIE_D);
            }
            gestionarAgarres();
        }
    }

    private static float acercar(float de, float a, float paso) {
        return de + Mth.clamp(a - de, -paso, paso);
    }

    private void colocarPartes() {
        esqueleto.calcular(estado(1.0f));
        AABB caja = null;
        for (int b = 0; b < EsqueletoRey.N; b++) {
            AABB c = esqueleto.caja(b);
            partes[b].colocar(c);
            caja = caja == null ? c : caja.minmax(c);
        }
        for (int s = 0; s < 3; s++) {
            partes[EsqueletoRey.N + s].colocar(esqueleto.cajaSello(s));
        }
        cajaVisible = caja.inflate(4.0);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return cajaVisible != null ? cajaVisible : this.getBoundingBox().inflate(25.0, 50.0, 25.0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distancia) {
        return distancia < 320.0 * 320.0;
    }

    @Override
    public boolean isPickable() {
        return false;            // se le toca por sus partes
    }

    // ---------- AGARRARSE Y TREPAR ----------

    /** Clic derecho en una de sus partes: si hay un asidero a mano, te agarras. */
    public void agarrar(Player jugador) {
        // solo cuando está de rodillas (agotado tras el rayo, o al romperle el primer sello)
        if (accion() == INTRO || this.isDeadOrDying() || rodillasRestantes <= 0 || jugador.isPassenger()
                || jugador.isSpectator() || agarres.containsKey(jugador.getUUID())) {
            return;
        }
        float[] sitio = AgarreRey.masCercana(esqueleto, jugador.getEyePosition());
        if (sitio == null) {
            return;
        }
        AgarreRey.Agarre g = new AgarreRey.Agarre((int) sitio[0], sitio[1], sitio[2]);
        agarres.put(jugador.getUUID(), g);
        if (!jugador.startRiding(this, true)) {
            agarres.remove(jugador.getUUID());
            return;
        }
        enviar(jugador, g);
        this.level().playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(), SoundEvents.SAND_HIT, SoundSource.PLAYERS, 1.5f, 0.7f);
    }

    private void gestionarAgarres() {
        Iterator<Map.Entry<UUID, AgarreRey.Agarre>> it = agarres.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, AgarreRey.Agarre> e = it.next();
            AgarreRey.Agarre g = e.getValue();
            Player jugador = this.level().getPlayerByUUID(e.getKey());
            if (jugador == null || !jugador.isAlive() || jugador.getVehicle() != this) {
                it.remove();
                if (jugador != null) soltado(jugador, g);
                continue;
            }
            g.ticks++;
            boolean seMueve = Math.abs(jugador.zza) > 0.01f || Math.abs(jugador.xxa) > 0.01f;
            // ¡se está sacudiendo y te mueves!: sales despedido
            if (accion() == SACUDIDA && ticksAccion >= AVISO_SACUDIDA && seMueve) {
                expulsar(jugador, g);
                continue;
            }
            AgarreRey.mover(esqueleto, g, jugador.zza, jugador.xxa, jugador.getYRot());
            if (AgarreRey.bocaAbajo(esqueleto, g.superficie)) {
                expulsar(jugador, g);
                continue;
            }
            enviar(jugador, g);
        }
    }

    /** Te echa de encima: al soltarte sales despedido (y la caída, aunque duela, no te mata). */
    private void expulsar(Player jugador, AgarreRey.Agarre g) {
        g.lanzado = true;
        jugador.stopRiding();
    }

    /** Se ha soltado (o le ha echado): se aparta de la pared. */
    private void soltado(Player jugador, AgarreRey.Agarre g) {
        FeRed.CANAL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this),
                new AgarreReyPacket(this.getId(), jugador.getId(), -1, 0, 0));
        Vec3 n = AgarreRey.normal(esqueleto, g.superficie);
        Vec3 fuera = new Vec3(n.x, 0, n.z);
        fuera = fuera.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, jugador.getYRot()).scale(-1) : fuera.normalize();
        if (g.lanzado) {
            jugador.getPersistentData().putLong(MARCA_LANZADO, this.level().getGameTime());
            jugador.setDeltaMovement(fuera.x * 1.1, 0.5, fuera.z * 1.1);
        } else {
            jugador.setDeltaMovement(fuera.x * 0.35, 0.1, fuera.z * 0.35);
        }
        empujar(jugador);
    }

    /** Si te ha echado él, la caída duele como mucho CAIDA_MAXIMA. Se registra en ModuloFe. */
    public static void alCaer(LivingFallEvent evento) {
        if (!(evento.getEntity() instanceof Player jugador) || jugador.level().isClientSide()) return;
        CompoundTag datos = jugador.getPersistentData();
        if (!datos.contains(MARCA_LANZADO)) return;
        long cuando = datos.getLong(MARCA_LANZADO);
        datos.remove(MARCA_LANZADO);
        if (jugador.level().getGameTime() - cuando < 600) {
            evento.setDistance(Math.min(evento.getDistance(), 3.0f + CAIDA_MAXIMA));
        }
    }

    private void enviar(Player jugador, AgarreRey.Agarre g) {
        FeRed.CANAL.send(PacketDistributor.TRACKING_ENTITY.with(() -> this),
                new AgarreReyPacket(this.getId(), jugador.getId(), g.superficie, g.u, g.v));
    }

    /** Cliente: lo que cuenta el servidor de un agarre. */
    public void agarreCliente(int jugador, int superficie, float u, float v) {
        if (superficie < 0) {
            agarresCliente.remove(jugador);
            return;
        }
        AgarreRey.Agarre g = agarresCliente.computeIfAbsent(jugador, k -> new AgarreRey.Agarre(superficie, u, v));
        g.superficie = superficie;
        g.u = u;
        g.v = v;
    }

    private AgarreRey.Agarre agarreDe(Entity pasajero) {
        return this.level().isClientSide() ? agarresCliente.get(pasajero.getId()) : agarres.get(pasajero.getUUID());
    }

    @Override
    protected void positionRider(Entity pasajero, MoveFunction mover) {
        AgarreRey.Agarre g = agarreDe(pasajero);
        if (g == null) {
            mover.accept(pasajero, pasajero.getX(), pasajero.getY(), pasajero.getZ());
            return;
        }
        Vec3 p = AgarreRey.pies(esqueleto, g.superficie, g.u, g.v, pasajero.getBbHeight());
        mover.accept(pasajero, p.x, p.y, p.z);
    }

    /** Al soltarse se queda donde estaba (y cae), en vez de aparecer a los pies del rey. */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity pasajero) {
        return pasajero.position();
    }

    @Override
    protected boolean canAddPassenger(Entity pasajero) {
        return pasajero instanceof Player && this.getPassengers().size() < 8;
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

    // ---------- LOS SELLOS (solo ahí le duele) ----------

    public boolean hurtParte(ReyParte parte, DamageSource fuente, float cantidad) {
        if (this.level().isClientSide() || Jefe.esFlecha(fuente) || accion() == INTRO || this.isDeadOrDying()) {
            return false;
        }
        int sello = parte.sello;
        Entity atacante = fuente.getEntity();
        if (sello < 0 && atacante instanceof Player p && agarres.containsKey(p.getUUID())) {
            sello = selloCercano(p.getEyePosition(), 3.5);
        }
        if (sello < 0 || !selloAbierto(sello)) {
            // piedra: ni se inmuta
            Vec3 c = parte.getBoundingBox().getCenter();
            this.level().playSound(null, c.x, c.y, c.z, SoundEvents.STONE_HIT, SoundSource.HOSTILE, 1.5f, 0.5f);
            return false;
        }
        // cada sello es un tercio de su vida: le quita a la vida exactamente lo que le quita al sello
        float dano = Math.min(cantidad, vidaSello[sello]);
        if (dano <= 0) return false;
        float antes = this.getHealth();
        golpeandoSello = true;
        boolean hecho = super.hurt(fuente, dano);
        golpeandoSello = false;
        float quitado = antes - this.getHealth();
        if (hecho && quitado > 0) {
            vidaSello[sello] = Math.max(0, vidaSello[sello] - quitado);
            esperaDefensa -= 25;                 // le escuece: intentará echarte antes
            if (this.level() instanceof ServerLevel nivel) {
                Vec3 c = esqueleto.centroSello(sello);
                nivel.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 12, 0.6, 0.6, 0.6, 0.15);
                nivel.sendParticles(ARENA, c.x, c.y, c.z, 20, 0.8, 0.8, 0.8, 0.1);
            }
            if (vidaSello[sello] <= 0.01f) {
                romperSello(sello);
            }
        }
        return hecho;
    }

    private int selloCercano(Vec3 donde, double maximo) {
        int mejor = -1;
        double d = maximo;
        for (int i = 0; i < 3; i++) {
            if (selloRoto(i)) continue;
            double di = esqueleto.centroSello(i).distanceTo(donde);
            if (di < d) {
                d = di;
                mejor = i;
            }
        }
        return mejor;
    }

    private void romperSello(int i) {
        this.entityData.set(SELLOS, this.entityData.get(SELLOS) | (1 << i));
        Vec3 c = esqueleto.centroSello(i);
        if (this.level() instanceof ServerLevel nivel) {
            nivel.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 2, 0, 0, 0, 0);
            nivel.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 80, 1.0, 1.0, 1.0, 0.35);
            nivel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SANDSTONE.defaultBlockState()),
                    c.x, c.y, c.z, 60, 1.2, 1.2, 1.2, 0.2);
        }
        this.playSound(ModuloFe.SONIDO_REY_SELLO.get(), 10.0f, 1.0f);
        if (i == 2) return;                        // el último: muere (ya no le queda vida)
        // ¡le has roto un sello!: se levanta de golpe y se sacude con todas sus fuerzas para echaros a todos
        rodillasRestantes = 0;
        estallido = -1;
        golpeados.clear();
        ultimaFuerte = this.level().getGameTime();
        empezar(SACUDIDA_FUERTE);
        if (selloRoto(0) && selloRoto(1)) {
            this.playSound(ModuloFe.SONIDO_REY_RUGIDO.get(), 12.0f, 1.3f);   // se abre la corona
        }
    }

    @Override
    public boolean hurt(DamageSource fuente, float cantidad) {
        if (golpeandoSello || fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(fuente, cantidad);
        }
        return false;
    }

    // ---------- CEREBRO ----------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ticksAccion++;
        barra.setProgress(this.getHealth() / this.getMaxHealth());
        if (altar.equals(BlockPos.ZERO)) {
            altar = this.blockPosition();
        }
        int accion = accion();
        if (accion == INTRO) {
            intro();
            return;
        }
        if (this.tickCount % 100 == 0) {
            escalarPorJugadores();
        }
        if (rodillasRestantes > 0) rodillasRestantes--;
        boolean deRodillas = rodillasRestantes > 0;
        this.entityData.set(RODILLAS, deRodillas);
        defensas(deRodillas);
        Player objetivo = objetivoEnSuelo();
        if (accion != RAYO && objetivo != null) {
            mirar(objetivo.getEyePosition());
        }
        Vec3 horizontal = Vec3.ZERO;
        switch (accion) {
            case REPOSO, CAMINAR -> horizontal = decidir(objetivo, deRodillas);
            case PISOTON -> pisoton();
            case BARRIDO -> barrido();
            case PALMADA -> palmada();
            case RAYO -> rayo(objetivo);
            case COZ -> coz();
            case TEMBLOR -> temblor();
            case SACUDIDA -> {
                if (ticksAccion == 1) this.playSound(ModuloFe.SONIDO_REY_RUGIDO.get(), 10.0f, 0.7f);
                if (ticksAccion == (int) AVISO_SACUDIDA) this.playSound(ModuloFe.SONIDO_REY_GOLPE.get(), 6.0f, 1.4f);
                if (ticksAccion >= TICKS_SACUDIDA) terminarDefensa();
            }
            case SACUDIDA_FUERTE -> {
                if (ticksAccion == 1) this.playSound(ModuloFe.SONIDO_REY_RUGIDO.get(), 14.0f, 0.5f);
                if (ticksAccion == (int) AVISO_FUERTE + 5) {
                    // ¡a todos fuera!
                    this.playSound(ModuloFe.SONIDO_REY_GOLPE.get(), 10.0f, 0.8f);
                    for (Map.Entry<UUID, AgarreRey.Agarre> e : agarres.entrySet()) {
                        Player p = this.level().getPlayerByUUID(e.getKey());
                        if (p != null) expulsar(p, e.getValue());
                    }
                }
                if (ticksAccion >= TICKS_SACUDIDA) terminarDefensa();
            }
            case RUGIDO -> {
                if (ticksAccion == 1) this.playSound(ModuloFe.SONIDO_REY_RUGIDO.get(), 12.0f, 0.9f);
                if (ticksAccion >= TICKS_RUGIDO) terminarAtaque();
            }
            default -> empezar(REPOSO);
        }
        // pegado al suelo (la raíz son sus pies)
        double suelo = suelo(this.position());
        double dy = Mth.clamp(suelo - this.getY(), -0.3, 0.3);
        this.setDeltaMovement(horizontal.x, dy, horizontal.z);
    }

    /** El más cercano de los que están en el suelo (no subidos encima). */
    private Player objetivoEnSuelo() {
        Player mejor = null;
        double d = 100 * 100;
        for (Player p : this.level().players()) {
            if (!atacaA(p) || !p.isAlive() || p.getVehicle() == this) continue;
            double di = p.distanceToSqr(this);
            if (di < d) {
                d = di;
                mejor = p;
            }
        }
        this.setTarget(mejor);
        return mejor;
    }

    private void mirar(Vec3 punto) {
        Vector3f actual = this.entityData.get(MIRA);
        if (actual.distanceSquared((float) punto.x, (float) punto.y, (float) punto.z) > 0.09f) {
            this.entityData.set(MIRA, new Vector3f((float) punto.x, (float) punto.y, (float) punto.z));
        }
    }

    private Vec3 decidir(Player objetivo, boolean deRodillas) {
        ticksSinAtacar++;
        if (deRodillas || objetivo == null) {     // agotado (de rodillas) no ataca: es el momento de castigarle
            if (accion() != REPOSO) empezar(REPOSO);
            return Vec3.ZERO;
        }
        if (objetivo == null) {
            if (accion() != REPOSO) empezar(REPOSO);
            return Vec3.ZERO;
        }
        double d = Math.sqrt(horizontal2(objetivo.position()));
        float deseado = (float) (Mth.atan2(objetivo.getZ() - this.getZ(), objetivo.getX() - this.getX()) * Mth.RAD_TO_DEG) - 90.0f;
        boolean detras = Math.abs(Mth.wrapDegrees(deseado - this.getYRot())) > 100;
        float velocidadGiro = detras ? GIRO_ESPALDA : GIRO;
        float giro = Mth.approachDegrees(this.getYRot(), deseado, velocidadGiro);
        this.setYRot(giro);
        this.setYBodyRot(giro);
        this.setYHeadRot(giro);
        float diferencia = Math.abs(Mth.wrapDegrees(deseado - giro));
        if (ticksSinAtacar >= espera) {
            int ataque = -1;
            if (d < 7) {
                ataque = TEMBLOR;                                   // te has metido debajo de él
            } else if (diferencia > 100 && d < 16) {
                ataque = this.random.nextInt(5) < 3 ? COZ : TEMBLOR; // te has puesto a su espalda
            } else if (diferencia < 35) {
                // el rayo, a menudo (tras él se queda agotado: es cuando se le puede escalar)
                ataque = ataquesSinRayo >= RAYOS_CADA ? RAYO : elegirAtaque(d);
            }
            if (ataque >= 0) {
                Vec3 rel = objetivo.position().subtract(this.position());
                double a = this.getYRot() * Mth.DEG_TO_RAD;
                double lx = rel.x * Math.cos(a) + rel.z * Math.sin(a);
                this.entityData.set(LADO, lx >= 0 ? 1 : -1);
                if (ataque != RAYO) ataquesSinRayo++;
                empezar(ataque);
                return Vec3.ZERO;
            }
        }
        if (d > 18) {
            if (accion() != CAMINAR) empezar(CAMINAR);
            Vec3 frente = Vec3.directionFromRotation(0, this.getYRot());
            return frente.scale(VELOCIDAD);
        }
        if (accion() != REPOSO) empezar(REPOSO);
        return Vec3.ZERO;
    }

    private int elegirAtaque(double d) {
        int[] pesos = new int[12];
        if (d < 12) pesos[PISOTON] = 5;
        if (d >= 12 && d < 26) pesos[BARRIDO] = 4;
        if (d < 26) pesos[PALMADA] = d < 12 ? 2 : 3;
        pesos[RAYO] = d >= 18 ? 4 : 2;
        if (d < 16) pesos[TEMBLOR] = 1;
        int total = 0;
        for (int p : pesos) total += p;
        if (total == 0) return -1;
        int r = this.random.nextInt(total);
        for (int i = 0; i < pesos.length; i++) {
            if (r < pesos[i]) return i;
            r -= pesos[i];
        }
        return -1;
    }

    private void terminarAtaque() {
        golpeados.clear();
        ticksSinAtacar = 0;
        espera = 40 + this.random.nextInt(50);
        empezar(REPOSO);
    }

    private void terminarDefensa() {
        esperaDefensa = ESPERA_DEFENSA + this.random.nextInt(50);
        ticksSinAtacar = 0;
        empezar(REPOSO);
    }

    /**
     * Las formas de echarte, de una en una (nunca dos a la vez) y con un respiro entre ellas: sacudida, estallido o,
     * de vez en cuando, la sacudida fuerte. No lo hace mientras está agotado (de rodillas) ni en mitad de un ataque.
     */
    private void defensas(boolean deRodillas) {
        if (agarres.isEmpty()) {
            estallido = -1;
            esperaDefensa = ESPERA_DEFENSA_PRIMERA;
            return;
        }
        if (estallido >= 0) {
            seguirEstallido();
            return;
        }
        int accion = accion();
        if (deRodillas || accion == SACUDIDA || accion == SACUDIDA_FUERTE || (accion != REPOSO && accion != CAMINAR)) {
            return;
        }
        if (--esperaDefensa > 0) return;
        boolean fuerte = this.level().getGameTime() - ultimaFuerte > ESPERA_FUERTE;
        int r = this.random.nextInt(fuerte ? 9 : 8);
        if (r < 4) {
            empezar(SACUDIDA);
        } else if (r < 8) {
            empezarEstallido();
        } else {
            ultimaFuerte = this.level().getGameTime();
            empezar(SACUDIDA_FUERTE);
        }
    }

    // ---------- INTRO: sale de la arena ----------

    private void intro() {
        this.setInvulnerable(true);
        Vec3 base = this.position();
        if (this.level() instanceof ServerLevel nivel) {
            if (ticksAccion == 1) {
                titulo(nivel);
            }
            if (ticksAccion % 20 == 1) {
                nivel.playSound(null, altar, ModuloFe.SONIDO_REY_PASO.get(), SoundSource.HOSTILE, 10.0f, 0.5f);
            }
            for (int i = 0; i < 6; i++) {
                double a = this.random.nextDouble() * Math.PI * 2, r = this.random.nextDouble() * 12;
                nivel.sendParticles(ARENA, base.x + Math.cos(a) * r, base.y + 0.5, base.z + Math.sin(a) * r, 3, 0.5, 0.3, 0.5, 0.05);
            }
            if (ticksAccion == TICKS_INTRO - 40) {
                this.playSound(ModuloFe.SONIDO_REY_RUGIDO.get(), 14.0f, 0.8f);
            }
        }
        if (ticksAccion >= TICKS_INTRO) {
            this.setInvulnerable(false);
            this.setHealth(this.getMaxHealth());
            empezar(REPOSO);
        }
    }

    private void titulo(ServerLevel nivel) {
        for (ServerPlayer jugador : nivel.getPlayers(j -> j.distanceToSqr(this) < 120 * 120)) {
            jugador.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 20));
            jugador.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("yonga.rey.titulo")));
            jugador.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("yonga.rey.subtitulo")));
        }
    }

    // ---------- ATAQUES ----------

    /** Cada paso que da: si te pilla debajo del pie, duele. */
    private void pisada(int pie) {
        if (!(this.level() instanceof ServerLevel nivel) || this.isDeadOrDying()) return;
        Vec3 p = esqueleto.aMundo(pie, 0, -2, 1.5f);
        nivel.playSound(null, p.x, p.y, p.z, ModuloFe.SONIDO_REY_PASO.get(), SoundSource.HOSTILE, 6.0f, 0.9f + this.random.nextFloat() * 0.2f);
        polvo(nivel, p, 30, 2.5);
        for (Player jugador : jugadoresEnSuelo(partes[pie].getBoundingBox().inflate(0.8, 1.5, 0.8))) {
            if (jugador.hurt(this.damageSources().mobAttack(this), DANO_PISADA)) {
                lanzar(jugador, p, 1.0, 0.5);
            }
        }
    }

    private void pisoton() {
        int pie = lado() > 0 ? EsqueletoRey.PIE_I : EsqueletoRey.PIE_D;
        if (ticksAccion == 1) this.playSound(ModuloFe.SONIDO_REY_RUGIDO.get(), 8.0f, 0.6f);
        if (ticksAccion == 48) {
            onda(esqueleto.aMundo(pie, 0, -2, 1.5f), 9.0, DANO_PISOTON, 5.0f, 1.6);
        }
        if (ticksAccion >= TICKS_PISOTON) terminarAtaque();
    }

    private void barrido() {
        int mano = lado() > 0 ? EsqueletoRey.MANO_I : EsqueletoRey.MANO_D;
        if (ticksAccion == 1) this.playSound(ModuloFe.SONIDO_REY_RUGIDO.get(), 8.0f, 0.75f);
        if (ticksAccion == 30) this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 10.0f, 0.4f);
        if (ticksAccion >= 30 && ticksAccion <= 48 && this.level() instanceof ServerLevel nivel) {
            AABB golpe = partes[mano].getBoundingBox().minmax(partes[mano - 1].getBoundingBox()).inflate(1.0);
            Vec3 c = partes[mano].getBoundingBox().getCenter();
            polvo(nivel, new Vec3(c.x, suelo(c), c.z), 12, 2.0);
            Vec3 barre = esqueleto.direccion(EsqueletoRey.TORSO, -lado(), 0, 0);
            for (Player jugador : jugadoresEnSuelo(golpe)) {
                if (golpeados.add(jugador.getUUID()) && jugador.hurt(this.damageSources().mobAttack(this), DANO_BARRIDO)) {
                    jugador.setDeltaMovement(barre.x * 2.2, 0.8, barre.z * 2.2);
                    empujar(jugador);
                }
            }
        }
        if (ticksAccion >= TICKS_BARRIDO) terminarAtaque();
    }

    private void palmada() {
        int mano = lado() > 0 ? EsqueletoRey.MANO_I : EsqueletoRey.MANO_D;
        if (ticksAccion == 1) this.playSound(ModuloFe.SONIDO_REY_RUGIDO.get(), 8.0f, 0.8f);
        if (ticksAccion == 40) {
            AABB caja = partes[mano].getBoundingBox();
            for (Player jugador : jugadoresEnSuelo(caja.inflate(0.5, 1.0, 0.5))) {
                jugador.hurt(this.damageSources().mobAttack(this), DANO_PALMADA);
            }
            Vec3 c = caja.getCenter();
            onda(new Vec3(c.x, suelo(c), c.z), 6.0, DANO_ONDA_PALMADA, 4.0f, 1.2);
        }
        if (ticksAccion >= TICKS_PALMADA) terminarAtaque();
    }

    /** El rayo de los ojos: apunta un poco antes de ti y se arrastra hacia donde estés (a una velocidad que se esquiva). */
    private void rayo(Player objetivo) {
        if (!(this.level() instanceof ServerLevel nivel)) return;
        Vec3 ojos = esqueleto.aMundo(EsqueletoRey.CABEZA, 0, 5, 4.2f);
        if (ticksAccion < 30) {
            if (objetivo != null) mirar(objetivo.getEyePosition());
            if (ticksAccion % 3 == 0) nivel.sendParticles(ParticleTypes.END_ROD, ojos.x, ojos.y, ojos.z, 6, 1.5, 0.5, 1.5, 0.05);
            if (ticksAccion == 2) this.playSound(ModuloFe.SONIDO_CENTINELA_CARGA.get(), 10.0f, 0.6f);
        } else if (ticksAccion == 30) {
            Vec3 hacia = objetivo != null ? objetivo.position() : this.position().add(Vec3.directionFromRotation(0, getYRot()).scale(20));
            Vec3 delante = hacia.subtract(this.position()).multiply(1, 0, 1).normalize().scale(-8);
            rayoPunto = new Vec3(hacia.x + delante.x, 0, hacia.z + delante.z);
            rayoPunto = new Vec3(rayoPunto.x, suelo(rayoPunto), rayoPunto.z);
            this.playSound(ModuloFe.SONIDO_CENTINELA_RAYO.get(), 12.0f, 0.6f);
        } else if (ticksAccion < 90) {
            if (objetivo != null) {
                Vec3 hacia = new Vec3(objetivo.getX() - rayoPunto.x, 0, objetivo.getZ() - rayoPunto.z);
                if (hacia.length() > VELOCIDAD_RAYO) hacia = hacia.normalize().scale(VELOCIDAD_RAYO);
                rayoPunto = rayoPunto.add(hacia);
                rayoPunto = new Vec3(rayoPunto.x, suelo(rayoPunto), rayoPunto.z);
            }
            mirar(rayoPunto);
            for (int i = 0; i <= 24; i++) {
                Vec3 p = ojos.lerp(rayoPunto, i / 24.0);
                nivel.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.15, 0.15, 0.15, 0.0);
            }
            nivel.sendParticles(ParticleTypes.FLASH, rayoPunto.x, rayoPunto.y + 0.2, rayoPunto.z, 1, 0, 0, 0, 0);
            polvo(nivel, rayoPunto, 8, 1.2);
            for (Player jugador : jugadoresEnSuelo(new AABB(rayoPunto, rayoPunto).inflate(1.6, 3.0, 1.6))) {
                jugador.hurt(this.damageSources().mobAttack(this), DANO_RAYO);
            }
            if (ticksAccion % 20 == 0) this.playSound(ModuloFe.SONIDO_CENTINELA_RAYO.get(), 8.0f, 0.7f);
        }
        if (ticksAccion >= TICKS_RAYO) {
            ataquesSinRayo = 0;
            rodillasRestantes = Math.max(rodillasRestantes, TICKS_AGOTADO);   // agotado: cae de rodillas
            this.playSound(ModuloFe.SONIDO_REY_GOLPE.get(), 8.0f, 0.6f);
            terminarAtaque();
        }
    }

    /** Coz hacia atrás: para los que se esconden a su espalda. */
    private void coz() {
        int pie = lado() > 0 ? EsqueletoRey.PIE_I : EsqueletoRey.PIE_D;
        if (ticksAccion == 1) this.playSound(ModuloFe.SONIDO_REY_RUGIDO.get(), 8.0f, 0.9f);
        if (ticksAccion == 22) this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 10.0f, 0.3f);
        if (ticksAccion >= 22 && ticksAccion <= 34) {
            AABB golpe = partes[pie].getBoundingBox().minmax(partes[pie - 1].getBoundingBox()).inflate(1.5);
            Vec3 atras = Vec3.directionFromRotation(0, this.getYRot()).scale(-1);
            for (Player jugador : jugadoresEnSuelo(golpe)) {
                if (golpeados.add(jugador.getUUID()) && jugador.hurt(this.damageSources().mobAttack(this), DANO_COZ)) {
                    jugador.setDeltaMovement(atras.x * 2.5, 0.9, atras.z * 2.5);
                    empujar(jugador);
                }
            }
        }
        if (ticksAccion == 46) {
            onda(esqueleto.aMundo(pie, 0, -2, 1.5f), 5.0, 8.0f, 3.0f, 1.0);
        }
        if (ticksAccion >= TICKS_COZ) terminarAtaque();
    }

    /** Temblor: salta y cae a plomo. Onda de arena alrededor (también por detrás). */
    private void temblor() {
        if (ticksAccion == 1) this.playSound(ModuloFe.SONIDO_REY_RUGIDO.get(), 10.0f, 0.65f);
        if (ticksAccion == 40) {
            onda(this.position(), RADIO_TEMBLOR, DANO_TEMBLOR, 4.0f, 1.8);
        }
        if (ticksAccion >= TICKS_TEMBLOR) terminarAtaque();
    }

    /** Estallido: la piedra se agrieta y brilla donde está uno de los de encima (al azar). */
    private void empezarEstallido() {
        AgarreRey.Agarre g = agarres.values().stream().skip(this.random.nextInt(agarres.size())).findFirst().orElse(null);
        if (g == null || !(this.level() instanceof ServerLevel nivel)) return;
        estallido = 0;
        estallidoSuperficie = g.superficie;
        estallidoU = g.u;
        estallidoV = g.v;
        Vec3 p = AgarreRey.punto(esqueleto, g.superficie, g.u, g.v);
        nivel.playSound(null, p.x, p.y, p.z, SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 3.0f, 0.5f);
    }

    /** ...y tras el aviso revienta: a quien siga cerca le hace daño y le echa. */
    private void seguirEstallido() {
        if (!(this.level() instanceof ServerLevel nivel)) return;
        estallido++;
        Vec3 p = AgarreRey.punto(esqueleto, estallidoSuperficie, estallidoU, estallidoV);
        double abierto = 0.3 + estallido / (double) AVISO_ESTALLIDO;
        nivel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SANDSTONE.defaultBlockState()),
                p.x, p.y, p.z, 3, abierto, abierto, abierto, 0.02);
        nivel.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 2, abierto * 0.6, abierto * 0.6, abierto * 0.6, 0.01);
        if (estallido % 8 == 0) {
            nivel.playSound(null, p.x, p.y, p.z, SoundEvents.STONE_HIT, SoundSource.HOSTILE, 3.0f, 0.4f + estallido * 0.02f);
        }
        if (estallido >= AVISO_ESTALLIDO) {
            estallido = -1;
            esperaDefensa = ESPERA_DEFENSA + this.random.nextInt(50);
            nivel.playSound(null, p.x, p.y, p.z, ModuloFe.SONIDO_REY_GOLPE.get(), SoundSource.HOSTILE, 8.0f, 1.4f);
            nivel.sendParticles(ARENA, p.x, p.y, p.z, 60, 1.5, 1.5, 1.5, 0.3);
            nivel.sendParticles(ParticleTypes.EXPLOSION, p.x, p.y, p.z, 2, 0.5, 0.5, 0.5, 0);
            for (Map.Entry<UUID, AgarreRey.Agarre> e : agarres.entrySet()) {
                Player jugador = this.level().getPlayerByUUID(e.getKey());
                if (jugador != null && jugador.position().add(0, 1, 0).distanceTo(p) < RADIO_ESTALLIDO) {
                    jugador.hurt(this.damageSources().mobAttack(this), DANO_ESTALLIDO);
                    expulsar(jugador, e.getValue());
                }
            }
        }
    }

    /** Varios jugadores: los sellos aguantan más (conservando lo que ya se les haya quitado). */
    private void escalarPorJugadores() {
        float nuevo = VIDA_SELLO * Jefe.multiplicador(Jefe.jugadoresCerca(this, 64));
        if (Math.abs(nuevo - maxSello) < 0.01f) return;
        float total = 0;
        for (int i = 0; i < 3; i++) {
            vidaSello[i] = vidaSello[i] / maxSello * nuevo;
            total += vidaSello[i];
        }
        maxSello = nuevo;
        Jefe.ponerMultiplicador(this, nuevo / VIDA_SELLO);
        this.setHealth(total);
    }

    /** Onda de arena alrededor de un punto (el daño baja con la distancia). Los de encima solo se cansan. */
    private void onda(Vec3 centro, double radio, float danoMax, float danoMin, double fuerza) {
        if (!(this.level() instanceof ServerLevel nivel)) return;
        nivel.playSound(null, centro.x, centro.y, centro.z, ModuloFe.SONIDO_REY_GOLPE.get(), SoundSource.HOSTILE, 10.0f, 0.8f);
        for (int anillo = 1; anillo <= 4; anillo++) {
            double r = radio * anillo / 4.0;
            for (int i = 0; i < 16 * anillo; i++) {
                double a = i * Math.PI * 2 / (16 * anillo);
                nivel.sendParticles(ARENA, centro.x + Math.cos(a) * r, centro.y + 0.3, centro.z + Math.sin(a) * r, 1, 0.2, 0.1, 0.2, 0.05);
            }
        }
        polvo(nivel, centro, 60, radio * 0.4);
        nivel.sendParticles(ParticleTypes.EXPLOSION, centro.x, centro.y + 0.5, centro.z, 3, radio * 0.2, 0.2, radio * 0.2, 0);
        for (Player jugador : jugadoresEnSuelo(new AABB(centro, centro).inflate(radio, 4.0, radio))) {
            double d = Math.sqrt(jugador.distanceToSqr(centro.x, jugador.getY(), centro.z));
            if (d > radio) continue;
            float dano = (float) Mth.lerp(d / radio, danoMax, danoMin);
            if (jugador.hurt(this.damageSources().mobAttack(this), dano)) {
                lanzar(jugador, centro, fuerza * (1 - d / radio) + 0.6, 0.7);
            }
        }
    }

    private java.util.List<Player> jugadoresEnSuelo(AABB caja) {
        return this.level().getEntitiesOfClass(Player.class, caja, p -> atacaA(p) && p.getVehicle() != this);
    }

    private void lanzar(Player jugador, Vec3 desde, double fuerza, double arriba) {
        Vec3 hacia = jugador.position().subtract(desde).multiply(1, 0, 1);
        hacia = hacia.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : hacia.normalize();
        jugador.setDeltaMovement(hacia.x * fuerza, arriba, hacia.z * fuerza);
        empujar(jugador);
    }

    private static void empujar(Player jugador) {
        jugador.hurtMarked = true;
        if (jugador instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
        }
    }

    private void polvo(ServerLevel nivel, Vec3 donde, int cantidad, double ancho) {
        nivel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                donde.x, donde.y + 0.3, donde.z, cantidad, ancho, 0.3, ancho, 0.15);
        nivel.sendParticles(ARENA, donde.x, donde.y + 0.5, donde.z, cantidad / 2, ancho, 0.5, ancho, 0.05);
    }

    private double horizontal2(Vec3 p) {
        double dx = p.x - this.getX(), dz = p.z - this.getZ();
        return dx * dx + dz * dz;
    }

    private double suelo(Vec3 p) {
        return this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(p.x), Mth.floor(p.z));
    }

    private void empezar(int accion) {
        this.entityData.set(ACCION, accion);
        this.ticksAccion = 0;
    }

    /** Se mueve exactamente con la velocidad que le damos y no choca con nada (es demasiado grande). */
    @Override
    public void travel(Vec3 entrada) {
        if (this.isEffectiveAi()) {
            this.move(MoverType.SELF, this.getDeltaMovement());
        }
        this.calculateEntityAnimation(false);
    }

    // ---------- LA MUERTE: se arrodilla, se vence y se deshace en arena ----------

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        this.setDeltaMovement(Vec3.ZERO);
        if (!(this.level() instanceof ServerLevel nivel)) return;
        if (this.deathTime == 1) {
            this.playSound(ModuloFe.SONIDO_REY_MUERTE.get(), 14.0f, 1.0f);
            // los de encima bajan flotando (están a 40 bloques...)
            for (Entity e : this.getPassengers()) {
                if (e instanceof Player p) p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 240, 0));
            }
            this.ejectPassengers();
        }
        if (this.deathTime % 3 == 0) {
            ReyParte p = partes[this.random.nextInt(EsqueletoRey.N)];
            Vec3 c = p.getBoundingBox().getCenter();
            nivel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SANDSTONE.defaultBlockState()),
                    c.x, c.y, c.z, 20, 1.5, 1.5, 1.5, 0.1);
            nivel.sendParticles(ARENA, c.x, c.y, c.z, 15, 2.0, 2.0, 2.0, 0.05);
        }
        if (this.deathTime == 110) {
            nivel.playSound(null, this.getX(), this.getY(), this.getZ(), ModuloFe.SONIDO_REY_GOLPE.get(), SoundSource.HOSTILE, 14.0f, 0.5f);
        }
        if (this.deathTime >= 160 && !this.isRemoved()) {
            for (ReyParte p : partes) {
                Vec3 c = p.getBoundingBox().getCenter();
                nivel.sendParticles(ARENA, c.x, c.y, c.z, 30, 2.0, 2.0, 2.0, 0.2);
            }
            nivel.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 2, this.getZ(), 3, 1, 1, 1, 0);
            this.dropExperience();
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public boolean isAlwaysExperienceDropper() {
        return true;
    }

    // ---------- VARIOS ----------

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
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource fuente) {
        return ModuloFe.SONIDO_REY_HERIDO.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected float getSoundVolume() {
        return 8.0f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag datos) {
        super.addAdditionalSaveData(datos);
        datos.putLong("Altar", altar.asLong());
        datos.putInt("Sellos", this.entityData.get(SELLOS));
        for (int i = 0; i < 3; i++) datos.putFloat("VidaSello" + i, vidaSello[i]);
        datos.putInt("Rodillas", rodillasRestantes);
        datos.putFloat("MaxSello", maxSello);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag datos) {
        super.readAdditionalSaveData(datos);
        altar = BlockPos.of(datos.getLong("Altar"));
        this.entityData.set(SELLOS, datos.getInt("Sellos"));
        for (int i = 0; i < 3; i++) {
            if (datos.contains("VidaSello" + i)) vidaSello[i] = datos.getFloat("VidaSello" + i);
        }
        rodillasRestantes = datos.getInt("Rodillas");
        if (datos.contains("MaxSello")) maxSello = datos.getFloat("MaxSello");
    }
}
