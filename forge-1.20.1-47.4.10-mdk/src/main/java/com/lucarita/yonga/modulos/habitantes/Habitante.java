package com.lucarita.yonga.modulos.habitantes;

import com.lucarita.yonga.core.ModItems;
import com.lucarita.yonga.modulos.habitantes.aldea.Aldeas;
import com.lucarita.yonga.modulos.habitantes.ia.Conversaciones;
import com.lucarita.yonga.modulos.habitantes.ia.Recuerdo;
import com.lucarita.yonga.modulos.habitantes.red.HabitantesRed;
import com.lucarita.yonga.modulos.habitantes.red.TiendaAccionPacket;
import com.lucarita.yonga.modulos.habitantes.red.TiendaEstadoPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * Un habitante: una persona con época, sexo, edad, medidas y aspecto propios. Sustituye al aldeano.
 *
 * EDAD: se guarda el momento en que nació (gameTime del servidor) y de ahí sale el "crecimiento", un número de 0 a 3:
 *   0..1 bebé · 1..2 niño · 2..3 joven · 3 adulto.  Las duraciones vienen de HabitantesConfig (1 / 2,5 / 5 días reales).
 * El servidor lo recalcula cada segundo y lo manda al cliente, que solo lo usa para dibujar.
 *
 * TAMAÑO: escala(crecimiento) x altura propia (0,94..1,08), así que un adulto mide entre 1,69 y 1,94 bloques.
 *
 * ASPECTO (un entero con varios campos): tono de piel, color de ojos, peinado, color de pelo, ropa y pecho.
 * El cliente lo convierte en capas de textura (ver client/habitantes/HabitanteRenderer).
 *
 * OFICIO (fase 2, ver Oficio): de día va a su puesto y llena su barra de trabajo; al llenarla produce un lote para
 * su tienda (9 huecos), donde se compra con monedas de oro; el regateo se hace hablando con él. No compran nada a los
 * jugadores: las monedas salen de los encargos de la taberna (fase 6). Comerciar le da experiencia y sube de nivel (1..5),
 * pero solo a los adultos: los jóvenes se quedan en el 1. Más nivel = barra más rápida y lotes mejores.
 *
 * CONVERSACIÓN (fase 3, ver ia/Conversaciones): clic derecho (o la tecla de hablar mirándolo) abre la conversación;
 * mayúsculas + clic derecho abre directamente la tienda. Cada habitante tiene dos rasgos de carácter y recuerda, de cada
 * jugador, lo que opina de él, si le ha dicho su nombre y lo último que se han dicho.
 */
public class Habitante extends PathfinderMob implements net.minecraft.world.entity.monster.RangedAttackMob {

    // ---------- datos que ve el cliente ----------
    private static final EntityDataAccessor<Integer> EPOCA = SynchedEntityData.defineId(Habitante.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MUJER = SynchedEntityData.defineId(Habitante.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> CRECIMIENTO = SynchedEntityData.defineId(Habitante.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ALTURA = SynchedEntityData.defineId(Habitante.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> COMPLEXION = SynchedEntityData.defineId(Habitante.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> ASPECTO = SynchedEntityData.defineId(Habitante.class, EntityDataSerializers.INT);
    /** En ropa interior (sin ropa, armadura ni objetos a la vista): un momento antes del fundido a negro. */
    private static final EntityDataAccessor<Integer> ASPECTO2 = SynchedEntityData.defineId(Habitante.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> INTERIOR = SynchedEntityData.defineId(Habitante.class, EntityDataSerializers.BOOLEAN);
    /** Lo avanzado de su embarazo (0 nada .. 1 a punto), para dibujar la barriga. */
    private static final EntityDataAccessor<Float> BARRIGA = SynchedEntityData.defineId(Habitante.class, EntityDataSerializers.FLOAT);

    // ---------- campos del aspecto (bits dentro de ASPECTO) ----------
    /**
     * Fase 7: por cada época y sexo, 20 ropas, 20 peinados, 20 barbas, 20 complementos y 20 rasgos de cara (marcas,
     * maquillaje, pinturas…); barba, complemento y marca tienen además el 0 = nada. La piel, los ojos, el color de pelo
     * y las expresiones (CARAS) son de todas las épocas.
     */
    public static final int TONOS = 6, CARAS = 6, OJOS = 6, PEINADOS = 20, COLORES_PELO = 8, ROPAS = 20, PECHOS = 8,
            CAIDAS = 4, BARBAS = 21, COMPLEMENTOS = 21, MARCAS = 21;
    /** En ASPECTO2: ropa, peinado, barba, complemento y marca, 5 bits cada uno. */
    private static final int A_ROPA = 0, A_PEINADO = 5, A_BARBA = 10, A_COMPLEMENTO = 15, A_MARCA = 20;
    private static final int B_PIEL = 0, B_OJOS = 3, B_PEINADO = 6, B_COLOR = 9, B_ROPA = 13, B_PECHO = 17, B_CAIDA = 20,
            B_CARA = 22, B_BARBA = 25;

    // ---------- solo en el servidor ----------
    /** gameTime del servidor en que nació. Long.MIN_VALUE = aún sin generar. */
    private long nacimiento = Long.MIN_VALUE;
    private String nombre = "";
    private int etapaAplicada = -1;

    // ---------- oficio (solo servidor) ----------
    @Nullable private Oficio oficio;
    private int nivel = 1;
    private int xp = 0;
    @Nullable private BlockPos puesto;
    private float barra;
    private final Tienda tienda = new Tienda();
    @Nullable private Player cliente;
    private boolean trabajando;
    /**
     * Rebajas (o subidas) de precio pactadas hablando, por jugador: de un objeto concreto o de todo (producto vacío),
     * hasta cuándo duran (un día de Minecraft) y, o bien un trato exacto (cantidad y precio, p. ej. 12 por 1 moneda),
     * o bien un modificador del precio (−0,35..+0,2) cuando es de todo.
     */
    private final Map<UUID, List<Rebaja>> rebajas = new HashMap<>();

    private record Rebaja(ItemStack producto, float modificador, int cantidad, int precio, long hasta) {
        boolean exacta() {
            return cantidad > 0 && precio > 0;
        }
    }

    // ---------- conversación (solo servidor) ----------
    /** Dos rasgos de carácter (índices de Conversaciones.RASGOS), o -1 si aún no tiene. */
    private int rasgo1 = -1, rasgo2 = -1;
    private final Map<UUID, Recuerdo> recuerdos = new HashMap<>();
    /** Esperando respuesta de la IA. */
    public boolean pensando;
    @Nullable private Player interlocutor;
    private long conversandoHasta;

    /** Ticks para llenar la barra a nivel 1 (3 minutos); cada nivel la acelera un 35 %. */
    private static final double TICKS_PIEZA = 3600.0;

    public Habitante(EntityType<? extends Habitante> tipo, Level nivel) {
        super(tipo, nivel);
        if (this.getNavigation() instanceof GroundPathNavigation navegacion) {
            navegacion.setCanOpenDoors(true);
        }
        this.setPersistenceRequired();
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }

    public static AttributeSupplier.Builder atributos() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, 2.5)          // un puñetazo (solo si se pelea)
                .add(Attributes.ATTACK_KNOCKBACK, 0.4);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(EPOCA, Epoca.MEDIEVAL.ordinal());
        this.entityData.define(MUJER, false);
        this.entityData.define(CRECIMIENTO, 3.0f);
        this.entityData.define(ALTURA, 1.0f);
        this.entityData.define(COMPLEXION, 1.0f);
        this.entityData.define(ASPECTO, 0);
        this.entityData.define(INTERIOR, false);
        this.entityData.define(ASPECTO2, 0);
        this.entityData.define(BARRIGA, 0.0f);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, new PeleaGoal(this));
        this.goalSelector.addGoal(0, new EscenaGoal(this));
        this.goalSelector.addGoal(1, new com.lucarita.yonga.modulos.habitantes.aldea.GuardiaGoals.Atacar(this));
        this.goalSelector.addGoal(1, new com.lucarita.yonga.modulos.habitantes.aldea.GuardiaGoals.Disparar(this));
        this.goalSelector.addGoal(1, new com.lucarita.yonga.modulos.habitantes.aldea.GuardiaGoals.Panico(this, 0.75));
        this.goalSelector.addGoal(1, new com.lucarita.yonga.modulos.habitantes.aldea.GuardiaGoals.Huir<>(this, Zombie.class, 8.0f, 0.6, 0.75));
        this.goalSelector.addGoal(1, new com.lucarita.yonga.modulos.habitantes.aldea.GuardiaGoals.Huir<>(this,
                net.minecraft.world.entity.raid.Raider.class, 12.0f, 0.6, 0.8));
        this.goalSelector.addGoal(2, new ConversarGoal(this));
        this.goalSelector.addGoal(2, new QuietoGoal(this));
        // Abrir puertas: los bebés no (se quedan dentro, o fuera, hasta que alguien les abra)
        this.goalSelector.addGoal(3, new OpenDoorGoal(this, true) {
            @Override
            public boolean canUse() {
                return Habitante.this.etapa() > 0 && super.canUse();
            }
        });
        this.goalSelector.addGoal(3, new SeguirGoal(this));
        this.goalSelector.addGoal(3, new DormirGoal(this));
        this.goalSelector.addGoal(4, new TrabajarGoal(this));
        this.goalSelector.addGoal(4, new CharlaGoal(this));
        this.goalSelector.addGoal(4, new PasearGoal(this));
        this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this, 0.55));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5));
        // Solo los guardias eligen objetivos (ver GuardiaGoals)
        this.targetSelector.addGoal(1, new com.lucarita.yonga.modulos.habitantes.aldea.GuardiaGoals.Defenderse(this));
        this.targetSelector.addGoal(2, new com.lucarita.yonga.modulos.habitantes.aldea.GuardiaGoals.Vigilar(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    // =====================================================================================================
    // NACER
    // =====================================================================================================

    /**
     * Le da época, sexo, medidas y aspecto al azar, y una edad dentro de la etapa pedida.
     * @param etapa 0 bebé, 1 niño, 2 joven, 3 adulto
     */
    public void generar(Epoca epoca, int etapa, RandomSource azar) {
        boolean mujer = azar.nextBoolean();
        this.entityData.set(EPOCA, epoca.ordinal());
        this.entityData.set(MUJER, mujer);
        // Medidas: las mujeres algo más bajas de media
        float altura = mujer ? 0.94f + azar.nextFloat() * 0.10f : 0.97f + azar.nextFloat() * 0.11f;
        this.entityData.set(ALTURA, altura);
        // Complexión (ancho de hombros y caderas): las mujeres, más finas
        this.entityData.set(COMPLEXION, mujer ? 0.88f + azar.nextFloat() * 0.14f : 0.96f + azar.nextFloat() * 0.18f);

        int aspecto = 0;
        // muy clara, clara, clara-media, media, morena, oscura (Castilla medieval: sobre todo claras)
        aspecto |= ponderado(azar, 18, 28, 24, 16, 9, 5) << B_PIEL;
        aspecto |= ponderado(azar, 30, 30, 12, 10, 12, 6) << B_OJOS;
        // (peinado, ropa, barba, complemento y marca van en ASPECTO2: ver más abajo)
        // negro, castaño oscuro, castaño, castaño claro, rubio, pelirrojo, gris, blanco
        aspecto |= ponderado(azar, 24, 26, 20, 10, 9, 6, 3, 2) << B_COLOR;
        aspecto |= azar.nextInt(CARAS) << B_CARA;                 // nada, rubor/cicatriz, pecas, lunar, sonrisa...
        aspecto |= azar.nextInt(PECHOS) << B_PECHO;               // tamaño: de muy pequeño a muy grande
        aspecto |= ponderado(azar, 45, 32, 17, 6) << B_CAIDA;     // de recto a caído (los caídos, pocos)
        this.entityData.set(ASPECTO, aspecto);
        // De su época: 20 ropas, 20 peinados; barba (casi la mitad de los hombres), complemento y alguna marca
        int barba = azar.nextInt(100) < 45 ? 0 : 1 + azar.nextInt(BARBAS - 1);
        int complemento = azar.nextInt(100) < 45 ? 0 : 1 + azar.nextInt(COMPLEMENTOS - 1);
        int marca = azar.nextInt(100) < 60 ? 0 : 1 + azar.nextInt(MARCAS - 1);
        this.entityData.set(ASPECTO2, componerAspecto2(azar.nextInt(ROPAS), Catalogo.peinadoAlAzar(epoca, mujer, azar),
                barba, complemento, marca));

        this.nombre = Nombres.elegir(epoca, mujer, azar);
        elegirRasgos(azar);
        // Oficio desde que nace (lo ejerce a partir de joven). Algunos adultos ya llevan tiempo y son aprendices.
        // Uno de cada cuatro no tiene oficio: busca trabajo (se queda con un puesto libre que encuentre) o, si es un
        // holgazán (uno de cada tres de ellos), no trabaja nunca.
        if (azar.nextInt(4) == 0) {
            quitarOficio(azar.nextInt(3) == 0);
        } else {
            asignarOficio(Oficio.alAzar(epoca, azar), etapa == 3 && azar.nextFloat() < 0.3f ? 2 : 1, null);
        }
        long ahora = this.level().getGameTime();
        this.nacimiento = ahora - edadDentroDeEtapa(Mth.clamp(etapa, 0, 3), azar.nextFloat());
        actualizarCrecimiento();
    }

    /** Una edad al azar como la de un pueblo cualquiera: 10 % bebés, 20 % niños, 20 % jóvenes y 50 % adultos. */
    public static int etapaAlAzar(RandomSource azar) {
        return ponderado(azar, 10, 20, 20, 50);
    }

    private void elegirRasgos(RandomSource azar) {
        this.rasgo1 = azar.nextInt(Conversaciones.RASGOS.length);
        do {
            this.rasgo2 = azar.nextInt(Conversaciones.RASGOS.length);
        } while (this.rasgo2 == this.rasgo1);
    }

    /** Ticks de edad para estar en esa etapa con ese progreso (0..1). Los adultos, hasta 10 días de más. */
    private static long edadDentroDeEtapa(int etapa, float progreso) {
        long b = HabitantesConfig.ticksBebe(), n = HabitantesConfig.ticksNino(), j = HabitantesConfig.ticksJoven();
        return switch (etapa) {
            case 0 -> (long) (b * progreso);
            case 1 -> b + (long) (n * progreso);
            case 2 -> b + n + (long) (j * progreso);
            default -> b + n + j + (long) (HabitantesConfig.TICKS_DIA * 10 * progreso);
        };
    }

    private static int ponderado(RandomSource azar, int... pesos) {
        int total = 0;
        for (int p : pesos) total += p;
        int r = azar.nextInt(total);
        for (int i = 0; i < pesos.length; i++) {
            r -= pesos[i];
            if (r < 0) return i;
        }
        return 0;
    }

    /** Le deja sin oficio (en paro). Los holgazanes no buscan trabajo nunca. */
    public void quitarOficio(boolean holgazan) {
        this.oficio = null;
        this.puesto = null;
        this.holgazan = holgazan;
    }

    public boolean sinOficio() {
        return this.oficio == null && !this.guardia;
    }

    public boolean esHolgazan() {
        return this.holgazan;
    }

    /**
     * Un parado (joven o adulto, que no sea holgazán) mira si hay un puesto de trabajo libre cerca (una mesa de
     * cantero, un horno, un barril…) y, si lo hay, se queda con ese oficio, como los aldeanos de siempre.
     */
    private void buscarTrabajo() {
        if (!sinOficio() || this.holgazan || etapa() < 2) return;
        BlockPos centro = this.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dz = -16; dz <= 16; dz++) {
                pos.set(centro.getX() + dx, centro.getY(), centro.getZ() + dz);
                if (!this.level().hasChunkAt(pos)) continue;
                for (int dy = -4; dy <= 4; dy++) {
                    pos.setY(centro.getY() + dy);
                    BlockState estado = this.level().getBlockState(pos);
                    for (Oficio o : Oficio.values()) {
                        if (o.epoca() != Oficio.epocaDeOficios(epoca())) continue;      // los de su época
                        if (o.esPuesto(estado) && !ocupado(pos)) {
                            asignarOficio(o, 1, pos.immutable());
                            decirEnAlto(Component.translatable("yonga.charla.trabajo." + this.random.nextInt(3)));
                            return;
                        }
                    }
                }
            }
        }
    }

    /** Le pone un oficio, un nivel (solo cuenta si es adulto) y, si se sabe, su puesto; y le llena algo la tienda. */
    public void asignarOficio(Oficio oficio, int nivel, @Nullable BlockPos puesto) {
        this.oficio = oficio;
        this.holgazan = false;
        this.nivel = Mth.clamp(nivel, 1, 5);
        this.xp = Oficio.XP_NIVEL[this.nivel - 1];
        this.puesto = puesto;
        this.barra = 0;
        this.tienda.cargar(new CompoundTag());
        for (int i = 0; i < 3; i++) {
            producir();
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor nivel, DifficultyInstance dificultad, MobSpawnType razon,
                                        @Nullable SpawnGroupData datos, @Nullable CompoundTag nbt) {
        if (this.nacimiento == Long.MIN_VALUE) {
            // De la época de la aldea donde aparece (fuera de las aldeas, medieval)
            generar(com.lucarita.yonga.modulos.habitantes.aldea.Poblados.epocaEn(nivel.getLevel(), this.blockPosition()),
                    etapaAlAzar(nivel.getRandom()), nivel.getRandom());
        }
        return super.finalizeSpawn(nivel, dificultad, razon, datos, nbt);
    }

    // =====================================================================================================
    // CRECER
    // =====================================================================================================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            tickCliente();
        } else {
            if (this.nacimiento == Long.MIN_VALUE) {          // por si llegó sin pasar por finalizeSpawn (comandos)
                generar(this.level() instanceof ServerLevel sn
                        ? com.lucarita.yonga.modulos.habitantes.aldea.Poblados.epocaEn(sn, this.blockPosition())
                        : Epoca.MEDIEVAL, etapaAlAzar(this.random), this.random);
            } else if (this.tickCount % 20 == 0) {
                actualizarCrecimiento();
            }
            tickOficio();
            if (this.interiorHasta != 0 && this.level().getGameTime() > this.interiorHasta) {
                this.interiorHasta = 0;
                this.entityData.set(INTERIOR, false);
            }
            if (Math.floorMod(this.tickCount + this.getId() * 3, 200) == 0 && this.level() instanceof ServerLevel nivel) {
                com.lucarita.yonga.modulos.habitantes.familia.Amor.revisar(nivel, this);
            }
        }
    }

    // =====================================================================================================
    // FAMILIA (ver familia/Amor)
    // =====================================================================================================

    private long interiorHasta;
    /** Puestos con el libro del creador (−1 = lo que salga de su UUID). Se guardan. */
    private int orientacionFija = -1, flexFija = -1;

    public float barriga() {
        return this.entityData.get(BARRIGA);
    }

    public void ponerBarriga(float b) {
        if (Math.abs(b - barriga()) > 0.001f) this.entityData.set(BARRIGA, b);
    }

    public boolean enRopaInterior() {
        return this.entityData.get(INTERIOR);
    }

    /** Se queda en ropa interior unos ticks (justo antes del fundido a negro; nunca más allá). */
    public void ropaInteriorDurante(int ticks) {
        this.entityData.set(INTERIOR, true);
        this.interiorHasta = this.level().getGameTime() + ticks;
        this.getNavigation().stop();
    }

    /** Se vuelve a vestir (al levantarse de la cama del encuentro). */
    public void vestirse() {
        this.entityData.set(INTERIOR, false);
        this.interiorHasta = 0;
    }

    /** 0 le atraen las personas del otro sexo (la mayoría), 1 las de su mismo sexo, 2 ambos. Sale de su UUID. */
    public int orientacion() {
        if (this.orientacionFija >= 0) return this.orientacionFija;
        int n = (int) Math.floorMod(this.getUUID().getLeastSignificantBits() >>> 11, 100L);
        return n < 85 ? 0 : n < 95 ? 1 : 2;
    }

    /**
     * Lo fiel que es a su pareja (0..100, de su UUID y su carácter): los amables, generosos y tímidos, más; los
     * avariciosos, bromistas, charlatanes y orgullosos, menos. Por debajo de 35 puede caer en la tentación.
     */
    public int fidelidad() {
        int f = (int) Math.floorMod(this.getUUID().getMostSignificantBits() >>> 17, 101L);
        if (tieneRasgo(AMABLE)) f += 15;
        if (tieneRasgo(GENEROSO)) f += 10;
        if (tieneRasgo(TIMIDO)) f += 10;
        if (tieneRasgo(AVARICIOSO)) f -= 10;
        if (tieneRasgo(BROMISTA)) f -= 10;
        if (tieneRasgo(CHARLATAN)) f -= 5;
        if (tieneRasgo(ORGULLOSO)) f -= 5;
        return Mth.clamp(f, 0, 100);
    }

    /**
     * Lo "facilón" que es para pasar la noche con alguien (0..100, de su UUID y su carácter): los alegres,
     * charlatanes y bromistas, más; los tímidos, desconfiados y orgullosos, menos.
     */
    public int facilidad() {
        int f = (int) Math.floorMod(this.getUUID().getLeastSignificantBits() >>> 29, 101L);
        if (tieneRasgo(ALEGRE)) f += 10;
        if (tieneRasgo(CHARLATAN)) f += 8;
        if (tieneRasgo(BROMISTA)) f += 8;
        if (tieneRasgo(TIMIDO)) f -= 15;
        if (tieneRasgo(DESCONFIADO)) f -= 12;
        if (tieneRasgo(ORGULLOSO)) f -= 6;
        return Mth.clamp(f, 0, 100);
    }

    public boolean infielDeNatural() {
        return fidelidad() < 35;
    }

    /** ¿Le puede atraer alguien de ese sexo? (si no se sabe el sexo del otro, sí) */
    public boolean leAtrae(boolean otraMujer, boolean sexoConocido) {
        if (!sexoConocido) return true;
        return switch (orientacion()) {
            case 0 -> otraMujer != esMujer();
            case 1 -> otraMujer == esMujer();
            default -> true;
        };
    }

    /** El bebé se parece a sus padres: piel, ojos, color de pelo y cara de uno u otro (o, a veces, de algún abuelo). */
    public void heredar(@Nullable Habitante a, @Nullable Habitante b) {
        if (a == null && b == null) return;
        int aspecto = this.entityData.get(ASPECTO);
        int[][] campos = {{B_PIEL, 3}, {B_OJOS, 3}, {B_COLOR, 4}, {B_CARA, 3}};
        for (int[] c : campos) {
            Habitante de = a == null ? b : b == null ? a : this.random.nextBoolean() ? a : b;
            if (this.random.nextInt(8) == 0) continue;              // un rasgo "del abuelo": se queda el suyo
            int mascara = ((1 << c[1]) - 1) << c[0];
            aspecto = (aspecto & ~mascara) | (de.entityData.get(ASPECTO) & mascara);
        }
        this.entityData.set(ASPECTO, aspecto);
    }

    // =====================================================================================================
    // TRABAJAR
    // =====================================================================================================

    private void tickOficio() {
        if (this.rasgo1 < 0) {
            elegirRasgos(this.random);
        }
        if (this.interlocutor != null && (this.cliente != null || !this.interlocutor.isAlive()
                || this.level().getGameTime() > this.conversandoHasta || this.interlocutor.distanceToSqr(this) > 12 * 12)) {
            this.interlocutor = null;
        }
        if (this.cliente != null) {
            if (!this.cliente.isAlive() || this.cliente.distanceToSqr(this) > 100) {
                this.cliente = null;
            } else {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(this.cliente, 30.0f, 30.0f);
            }
        }
        if (etapa() >= 2 && this.oficio != null) {
            if (Math.floorMod(this.tickCount + this.getId(), 100) == 0) {
                revisarPuesto();
            }
        }
        if (Math.floorMod(this.tickCount + this.getId() * 7, 200) == 0) {
            revisarAldea();
            revisarCama();
            if (Rutina.trabajo(this.level())) buscarTrabajo();
        }
        if (etapa() >= 2 && this.oficio != null) {
            if (this.trabajando) {
                this.barra += (float) ritmo();
                if (this.barra >= 1.0f) {
                    this.barra = producir() ? 0.0f : 1.0f;     // con la tienda llena, la barra espera llena
                }
            }
        }
        this.trabajando = false;
    }

    /** Nivel que cuenta: los jóvenes trabajan siempre como novatos. */
    public int nivelEfectivo() {
        return esAdulto() ? this.nivel : 1;
    }

    /** Lo que se llena la barra por tick: más rápido con nivel; los jóvenes, un 25 % más lentos. */
    private double ritmo() {
        double ticks = TICKS_PIEZA / (1.0 + 0.35 * (nivelEfectivo() - 1));
        if (!esAdulto()) ticks /= 0.75;
        return 1.0 / ticks;
    }

    /** Produce un lote y lo pone a la venta. false si la tienda está llena. */
    private boolean producir() {
        if (this.oficio == null) return true;
        Oficio.Producto producto = this.oficio.elegir(nivelEfectivo(), this.random);
        ItemStack lote = producto.crear();
        return lote.isEmpty() || this.tienda.anadir(lote, producto.precio());
    }

    public boolean puedeTrabajar() {
        if (etapa() < 2 || this.oficio == null || this.puesto == null || this.cliente != null || this.interlocutor != null
                || this.seguido != null || this.enemigo != null) return false;
        return Rutina.trabajo(this.level()) && !(this.barra >= 1.0f && this.tienda.llena())
                && barriga() < 0.85f;                                   // al final del embarazo descansa
    }

    public void marcarTrabajando() {
        this.trabajando = true;
    }

    /** Comprueba que su puesto sigue ahí; si no tiene, busca uno libre a menos de 16 bloques. */
    private void revisarPuesto() {
        if (this.puesto != null && (!this.oficio.esPuesto(this.level().getBlockState(this.puesto))
                || this.puesto.distSqr(this.blockPosition()) > 48 * 48)) {
            this.puesto = null;
        }
        if (this.puesto != null) return;
        BlockPos centro = this.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dz = -16; dz <= 16; dz++) {
                pos.set(centro.getX() + dx, centro.getY(), centro.getZ() + dz);
                if (!this.level().hasChunkAt(pos)) continue;
                for (int dy = -4; dy <= 4; dy++) {
                    pos.setY(centro.getY() + dy);
                    if (this.oficio.esPuesto(this.level().getBlockState(pos)) && !ocupado(pos)) {
                        this.puesto = pos.immutable();
                        return;
                    }
                }
            }
        }
    }

    private boolean ocupado(BlockPos pos) {
        return !this.level().getEntitiesOfClass(Habitante.class, new AABB(pos).inflate(64),
                h -> h != this && pos.equals(h.puesto)).isEmpty();
    }

    // =====================================================================================================
    // LA TIENDA
    // =====================================================================================================

    /**
     * Clic derecho: abre la conversación (a un bebé solo se le saca un balbuceo).
     * Mayúsculas + clic derecho a alguien con oficio: directamente su tienda.
     */
    @Override
    protected InteractionResult mobInteract(Player jugador, InteractionHand mano) {
        if (mano != InteractionHand.MAIN_HAND || jugador.getItemInHand(mano).getItem() instanceof net.minecraft.world.item.SpawnEggItem) {
            return super.mobInteract(jugador, mano);
        }
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer servidor = (ServerPlayer) jugador;
        if (jugador.getItemInHand(mano).is(ModuloHabitantes.LIBRO_CREADOR.get())) {
            if (jugador.isCreative()) {
                HabitantesRed.CANAL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> servidor),
                        new com.lucarita.yonga.modulos.habitantes.red.EditorPacket(this.getId(), datosEditor(servidor)));
            } else {
                aviso(jugador, "yonga.libro.solo_creativo");
            }
        } else if (jugador.getItemInHand(mano).is(ModuloHabitantes.ANILLO.get())) {
            com.lucarita.yonga.modulos.habitantes.familia.Amor.pedirMano(servidor, this);
        } else if (jugador.isShiftKeyDown() && etapa() >= 2 && this.oficio != null) {
            abrirTienda(servidor);
        } else {
            Conversaciones.abrir(servidor, this);
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Fase 9: un bebé en brazos va montado en el jugador, pero no en su cabeza: delante, a la altura del pecho,
     * mirando hacia donde mira él (ver compat/Compat).
     */
    @Override
    public void rideTick() {
        super.rideTick();
        if (this.getVehicle() instanceof Player jugador) {
            if (this.isSleeping()) this.stopSleeping();
            net.minecraft.world.phys.Vec3 delante = net.minecraft.world.phys.Vec3.directionFromRotation(0, jugador.getYRot()).scale(0.42);
            this.setPos(jugador.getX() + delante.x, jugador.getY() + jugador.getBbHeight() * 0.42, jugador.getZ() + delante.z);
            this.setYRot(jugador.getYRot());
            this.yBodyRot = jugador.yBodyRot;
            this.yHeadRot = jugador.getYHeadRot();
            this.fallDistance = 0;
        }
    }

    /** ¿Tiene tienda que enseñar? */
    public boolean tieneTienda() {
        return etapa() >= 2 && this.oficio != null;
    }

    public void abrirTienda(ServerPlayer servidor) {
        if (!tieneTienda()) {
            return;
        }
        if (this.cliente != null && this.cliente != servidor) {
            aviso(servidor, "yonga.tienda.ocupado");
            return;
        }
        NetworkHooks.openScreen(servidor,
                new SimpleMenuProvider((id, inventario, p) -> new TiendaMenu(id, inventario, this),
                        nombreOficio()),
                datos -> datos.writeVarInt(this.getId()));
        if (servidor.containerMenu instanceof TiendaMenu menu) {
            menu.enviarEstado(servidor);
        }
    }

    // ---------- conversación ----------

    public Recuerdo recuerdo(Player jugador) {
        return this.recuerdos.computeIfAbsent(jugador.getUUID(), u -> {
            // A quien no conoce, le juzga por la fama que tiene en su aldea (la mitad)
            Recuerdo r = new Recuerdo();
            if (this.aldea != null && this.level() instanceof ServerLevel nivel) {
                Aldeas.Aldea a = Aldeas.de(nivel).aldeaSiExiste(this.aldea);
                if (a != null) r.opinion = a.fama(u) / 2;
            }
            r.opinion += primeraImpresion();
            return r;
        });
    }

    // =====================================================================================================
    // ALDEA: LÍDER Y GUARDIAS
    // =====================================================================================================

    /** La campana de su aldea, o null si vive solo. */
    @Nullable private BlockPos aldea;
    private boolean guardia;
    /** Sin oficio y sin ganas de buscarlo (como los aldeanos "tontos" de siempre). Se guarda. */
    private boolean holgazan;
    /** Si es el líder de su aldea (se recalcula cada poco en el servidor). */
    private boolean lider;

    @Nullable
    public BlockPos aldea() {
        return this.aldea;
    }

    public boolean esGuardia() {
        return this.guardia;
    }

    public boolean esLider() {
        return this.lider;
    }

    /** Cada ~10 s: busca su aldea (la campana más cercana), elige líder si no hay y, si faltan guardias, se alista. */
    private void revisarAldea() {
        if (!(this.level() instanceof ServerLevel nivel)) return;
        this.aldea = Aldeas.campanaCerca(nivel, this.blockPosition());
        if (this.aldea == null) {
            // ¿Está en una aldea que se ha quedado sin campana? Se le pone una
            this.aldea = com.lucarita.yonga.modulos.habitantes.aldea.Poblados.reponerCampana(nivel, this.blockPosition());
        }
        if (this.aldea == null) {
            this.lider = false;
            if (this.guardia) this.clearRestriction();
            return;
        }
        if (this.guardia) {
            this.restrictTo(this.aldea, 32);
        }
        Aldeas datos = Aldeas.de(nivel);
        Aldeas.Aldea a = datos.aldea(this.aldea);
        // Fase 8: la primera vez, la aldea se rehace con los edificios de su época (luego ya no hace nada)
        com.lucarita.yonga.modulos.habitantes.aldea.Poblados.revisar(nivel, this.aldea);
        com.lucarita.yonga.modulos.habitantes.taberna.Taberna.revisar(nivel, this.aldea);
        if (a.lider == null && esAdulto() && !this.guardia) {
            datos.ponerLider(this.aldea, this.getUUID());
        }
        this.lider = this.getUUID().equals(a.lider);
        if (this.lider && this.oficio != null) quitarOficio(false);      // el alcalde no tiene otro oficio

        // Guardias: uno por cada 6 adultos (al menos uno si hay 3 adultos). Se alista algún adulto que no sea el líder.
        if (!this.lider && !this.guardia && esAdulto() && this.random.nextInt(3) == 0) {
            java.util.List<Habitante> vecinos = nivel.getEntitiesOfClass(Habitante.class,
                    new AABB(this.aldea).inflate(Aldeas.RADIO), h -> this.aldea.equals(h.aldea));
            long adultos = vecinos.stream().filter(Habitante::esAdulto).count();
            long guardias = vecinos.stream().filter(Habitante::esGuardia).count();
            long hacen = adultos >= 6 ? Math.max(1, adultos / 6) : adultos >= 3 ? 1 : 0;
            if (guardias < hacen) {
                hacerseGuardia();
            }
        }
    }

    /** Guardias con arco (uno de cada tres): disparan desde lejos. */
    public boolean esArquero() {
        return this.getMainHandItem().getItem() instanceof net.minecraft.world.item.BowItem;
    }

    /** Objetivos que un guardia ha dejado por imposibles, hasta cuándo (no se guardan). */
    private final Map<Integer, Long> ignorados = new HashMap<>();

    public void ignorar(net.minecraft.world.entity.Entity e, int ticks) {
        this.ignorados.put(e.getId(), this.level().getGameTime() + ticks);
    }

    public boolean ignora(net.minecraft.world.entity.Entity e) {
        Long hasta = this.ignorados.get(e.getId());
        if (hasta == null) return false;
        if (this.level().getGameTime() > hasta) {
            this.ignorados.remove(e.getId());
            return false;
        }
        return true;
    }

    /** Dispara una flecha (guardias con arco), como un esqueleto. */
    @Override
    public void performRangedAttack(net.minecraft.world.entity.LivingEntity objetivo, float fuerza) {
        net.minecraft.world.entity.projectile.AbstractArrow flecha = net.minecraft.world.entity.projectile.ProjectileUtil
                .getMobArrow(this, new ItemStack(net.minecraft.world.item.Items.ARROW), fuerza);
        double dx = objetivo.getX() - this.getX();
        double dy = objetivo.getY(1.0 / 3.0) - flecha.getY();
        double dz = objetivo.getZ() - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        flecha.shoot(dx, dy + horizontal * 0.2, dz, 1.6f, 6.0f);
        flecha.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.DISALLOWED;
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0f, 1.0f / (this.random.nextFloat() * 0.4f + 0.8f));
        this.swing(InteractionHand.MAIN_HAND);
        this.level().addFreshEntity(flecha);
    }

    /** Deja de ser guardia: suelta las armas y la armadura y vuelve a ser un vecino más (sin oficio). */
    public void dejarGuardia() {
        if (!this.guardia) return;
        this.guardia = false;
        for (EquipmentSlot hueco : EquipmentSlot.values()) this.setItemSlot(hueco, ItemStack.EMPTY);
        this.clearRestriction();
        this.etapaAplicada = -1;
    }

    // =====================================================================================================
    // LIBRO DEL CREADOR (solo creativo): ver todo y cambiarlo
    // =====================================================================================================

    /** Junta piel, ojos, color de pelo, cara (expresión), pecho y caída en el número del aspecto. */
    public static int componerAspecto(int piel, int ojos, int colorPelo, int cara, int pecho, int caida) {
        return (Mth.clamp(piel, 0, TONOS - 1) << B_PIEL) | (Mth.clamp(ojos, 0, OJOS - 1) << B_OJOS)
                | (Mth.clamp(colorPelo, 0, COLORES_PELO - 1) << B_COLOR) | (Mth.clamp(cara, 0, CARAS - 1) << B_CARA)
                | (Mth.clamp(pecho, 0, PECHOS - 1) << B_PECHO) | (Mth.clamp(caida, 0, CAIDAS - 1) << B_CAIDA);
    }

    /** Pone a la vez todo lo que se ve (también en el cliente, para la vista previa del libro). */
    public void ponerAspecto(boolean mujer, int aspecto, int aspecto2, float altura, float complexion, int epoca,
                             float crecimiento, float barriga) {
        this.entityData.set(MUJER, mujer);
        this.entityData.set(ASPECTO, aspecto);
        this.entityData.set(ASPECTO2, aspecto2);
        this.entityData.set(ALTURA, Mth.clamp(altura, 0.8f, 1.2f));
        this.entityData.set(COMPLEXION, Mth.clamp(complexion, 0.8f, 1.25f));
        this.entityData.set(EPOCA, Mth.clamp(epoca, 0, Epoca.values().length - 1));
        this.entityData.set(CRECIMIENTO, Mth.clamp(crecimiento, 0.0f, 3.0f));
        this.entityData.set(BARRIGA, barriga);
        this.refreshDimensions();
    }

    /** Todo lo que se puede tocar con el libro, tal como está ahora (con lo de este jugador). */
    public CompoundTag datosEditor(ServerPlayer j) {
        CompoundTag t = new CompoundTag();
        t.putBoolean("mujer", esMujer());
        t.putInt("piel", tonoPiel());
        t.putInt("ojos", colorOjos());
        t.putInt("peinado", peinado());
        t.putInt("colorPelo", colorPelo());
        t.putInt("ropa", ropa());
        t.putInt("cara", cara());
        t.putInt("barba", barbaGuardada());
        t.putInt("complemento", campo2(A_COMPLEMENTO) % COMPLEMENTOS);
        t.putInt("marca", campo2(A_MARCA) % MARCAS);
        t.putInt("pecho", pecho());
        t.putInt("caida", caidaPecho());
        t.putFloat("altura", altura());
        t.putFloat("complexion", complexion());
        t.putString("nombre", this.nombre);
        t.putFloat("edad", crecimiento());
        t.putInt("epoca", epoca().ordinal());
        t.putInt("rasgo1", Math.max(0, this.rasgo1));
        t.putInt("rasgo2", Math.max(0, this.rasgo2));
        t.putInt("orientacion", orientacion());
        t.putInt("flex", flexibilidad());
        t.putInt("oficio", this.oficio == null ? -1 : this.oficio.ordinal());
        t.putInt("nivel", this.nivel);
        t.putBoolean("holgazan", this.holgazan);
        t.putBoolean("guardia", this.guardia);
        Recuerdo r = recuerdo(j);
        t.putInt("opinion", r.opinion);
        t.putBoolean("sabeNombre", r.sabeNombre);
        t.putBoolean("enfadado", r.enfadadoHasta > this.level().getGameTime());
        t.putString("notas", r.notas);
        t.putBoolean("olvidar", false);
        var f = com.lucarita.yonga.modulos.habitantes.familia.Familias.de(j.getServer());
        t.putInt("pareja", f.estado(this.getUUID(), j.getUUID()));
        String imp = com.lucarita.yonga.modulos.habitantes.familia.Amor.impedimento(f, this, j);
        t.putBoolean("puedePareja", imp == null || imp.equals("pareja") || imp.equals("no_atrae"));
        var p = f.personaSiExiste(this.getUUID());
        t.putFloat("embarazo", p == null ? 0.0f : p.embarazo(this.level().getGameTime()));
        return t;
    }

    /** Aplica lo que se ha cambiado en el libro (ya comprobado que el jugador está en creativo). */
    public void aplicarEditor(ServerPlayer j, CompoundTag t) {
        if (!(this.level() instanceof ServerLevel nivel)) return;
        long ahora = nivel.getGameTime();
        boolean mujer = t.getBoolean("mujer");
        int aspecto = componerAspecto(t.getInt("piel"), t.getInt("ojos"), t.getInt("colorPelo"), t.getInt("cara"),
                t.getInt("pecho"), t.getInt("caida"));
        int aspecto2 = componerAspecto2(t.getInt("ropa"), t.getInt("peinado"), t.getInt("barba"),
                t.getInt("complemento"), t.getInt("marca"));
        float edad = Mth.clamp(t.getFloat("edad"), 0.0f, 3.0f);
        // La edad: se mueve el nacimiento para que el crecimiento sea justo ese
        long b = HabitantesConfig.ticksBebe(), n = HabitantesConfig.ticksNino(), jv = HabitantesConfig.ticksJoven();
        long anos = edad < 1 ? (long) (edad * b) : edad < 2 ? b + (long) ((edad - 1) * n)
                : edad < 3 ? b + n + (long) ((edad - 2) * jv) : b + n + jv + 1;
        this.nacimiento = ahora - anos;
        String nombreNuevo = t.getString("nombre").strip();
        if (!nombreNuevo.isEmpty() && nombreNuevo.length() <= 32) this.nombre = nombreNuevo;
        ponerAspecto(mujer, aspecto, aspecto2, t.getFloat("altura"), t.getFloat("complexion"), t.getInt("epoca"), edad, barriga());
        this.etapaAplicada = -1;
        actualizarCrecimiento();

        int r1 = Mth.clamp(t.getInt("rasgo1"), 0, Conversaciones.RASGOS.length - 1);
        int r2 = Mth.clamp(t.getInt("rasgo2"), 0, Conversaciones.RASGOS.length - 1);
        this.rasgo1 = r1;
        this.rasgo2 = r2 == r1 ? (r1 + 1) % Conversaciones.RASGOS.length : r2;
        this.orientacionFija = Mth.clamp(t.getInt("orientacion"), 0, 2);
        this.flexFija = Mth.clamp(t.getInt("flex"), 2, 40);

        // Trabajo
        if (t.getBoolean("guardia")) {
            if (!this.guardia) hacerseGuardia();
        } else {
            dejarGuardia();
            int o = t.getInt("oficio");
            int nivelNuevo = Mth.clamp(t.getInt("nivel"), 1, 5);
            if (o < 0 || o >= Oficio.values().length) {
                quitarOficio(t.getBoolean("holgazan"));
            } else if (this.oficio == Oficio.values()[o].enEpoca(epoca())) {
                this.nivel = nivelNuevo;
            } else {
                // Siempre el de su época (si se elige uno de otra, el que hace lo mismo en la suya)
                asignarOficio(Oficio.values()[o].enEpoca(epoca()), nivelNuevo, null);
            }
        }

        // Con este jugador
        Recuerdo r = recuerdo(j);
        r.opinion = Mth.clamp(t.getInt("opinion"), -100, 100);
        r.sabeNombre = t.getBoolean("sabeNombre");
        if (!t.getBoolean("enfadado")) r.enfadadoHasta = 0;
        else if (r.enfadadoHasta <= ahora) r.enfadadoHasta = ahora + 12000;
        String notas = t.getString("notas");
        r.notas = notas.length() > Recuerdo.MAX_NOTAS_CERCANOS ? notas.substring(0, Recuerdo.MAX_NOTAS_CERCANOS) : notas;
        if (t.getBoolean("olvidar")) r.frases.clear();

        // Familia: pareja con este jugador y embarazo (nunca con niños ni con la familia)
        var f = com.lucarita.yonga.modulos.habitantes.familia.Familias.de(j.getServer());
        f.apuntar(this.getUUID(), this.nombre, mujer, false);
        int pareja = Mth.clamp(t.getInt("pareja"), 0, 2);
        int antes = f.estado(this.getUUID(), j.getUUID());
        if (pareja == 0 && antes != 0) {
            f.romper(this.getUUID(), j.getUUID());
        } else if (pareja != 0 && pareja != antes) {
            String imp = com.lucarita.yonga.modulos.habitantes.familia.Amor.impedimento(f, this, j);
            if (imp == null || imp.equals("pareja") || imp.equals("no_atrae")) {
                UUID otra = f.pareja(this.getUUID());
                if (otra != null && !otra.equals(j.getUUID())) f.romper(this.getUUID(), otra);
                f.apuntar(j.getUUID(), j.getGameProfile().getName(), f.persona(j.getUUID()).mujer, true);
                f.emparejar(this.getUUID(), j.getUUID(), pareja, ahora);
            }
        }
        var p = f.persona(this.getUUID());
        float emb = mujer && esAdulto() ? Mth.clamp(t.getFloat("embarazo"), 0.0f, 0.99f) : 0.0f;
        if (emb <= 0.0f) {
            p.parto = 0;
            p.concepcion = 0;
        } else {
            long dura = com.lucarita.yonga.modulos.habitantes.familia.Amor.duracionEmbarazo();
            p.concepcion = ahora - (long) (emb * dura);
            p.parto = p.concepcion + dura;
            if (p.otroProgenitor == null) p.otroProgenitor = f.pareja(this.getUUID());
        }
        f.marcar();
        ponerBarriga(emb);
    }

    /** Deja su oficio y se hace guardia: espada o arco (uno de cada tres), casco de hierro y cota de malla. */
    public void hacerseGuardia() {
        this.guardia = true;
        if (this.level() instanceof ServerLevel nivel) soltarCama(nivel);      // los guardias no duermen
        this.oficio = null;
        this.puesto = null;
        this.tienda.cargar(new CompoundTag());
        boolean piedra = epoca() == Epoca.PREHISTORICA;      // en la prehistoria: hacha de piedra y pieles
        boolean roma = epoca() == Epoca.ANTIGUA;              // en Roma: gladio (espada de hierro) y cota de malla
        boolean tercio = epoca() == Epoca.MODERNA;            // en el XVII: espada, morrión y coleto de cuero
        boolean sable = epoca() == Epoca.INDUSTRIAL          // hacia 1890 y hoy: sable o porra y chaqueta, sin casco
                || epoca() == Epoca.CONTEMPORANEA;
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(this.random.nextInt(3) == 0
                ? net.minecraft.world.item.Items.BOW
                : piedra ? net.minecraft.world.item.Items.STONE_AXE : net.minecraft.world.item.Items.IRON_SWORD));
        this.setItemSlot(EquipmentSlot.HEAD, piedra || sable ? ItemStack.EMPTY : new ItemStack(roma
                ? net.minecraft.world.item.Items.CHAINMAIL_HELMET : net.minecraft.world.item.Items.IRON_HELMET));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(piedra || tercio || sable ? net.minecraft.world.item.Items.LEATHER_CHESTPLATE
                : net.minecraft.world.item.Items.CHAINMAIL_CHESTPLATE));
        for (EquipmentSlot hueco : EquipmentSlot.values()) {
            this.setDropChance(hueco, 0.0f);
        }
        AttributeInstance puno = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (puno != null) puno.setBaseValue(1.0);          // con la espada: 1 + 5
        this.etapaAplicada = -1;                            // recalcula la vida (30)
        if (this.aldea != null) this.restrictTo(this.aldea, 32);
    }

    // ---------- casa ----------

    /** Su cama (reservada en el registro de puntos de interés, como los aldeanos), o null. */
    @Nullable private BlockPos cama;

    @Nullable
    public BlockPos cama() {
        return this.cama;
    }

    /** Comprueba que su cama sigue ahí; si no tiene, reserva una libre cerca. Los guardias no duermen. */
    private void revisarCama() {
        if (!(this.level() instanceof ServerLevel nivel) || this.esGuardia() || this.isSleeping()) return;
        if (this.cama != null && !(nivel.getBlockState(this.cama).getBlock() instanceof net.minecraft.world.level.block.BedBlock)) {
            soltarCama(nivel);
        }
        if (this.cama == null) {
            this.cama = nivel.getPoiManager().take(
                    t -> t.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME),
                    (t, pos) -> true, this.blockPosition(), 48).orElse(null);
        }
    }

    private void soltarCama(ServerLevel nivel) {
        if (this.cama != null) {
            // Si la cama ya no existe (la han roto), su punto de interés tampoco: liberarlo haría fallar al juego
            var poi = nivel.getPoiManager();
            if (poi.existsAtPosition(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME, this.cama)) {
                try {
                    poi.release(this.cama);
                } catch (IllegalStateException ignorado) {
                    // ya estaba libre o desapareció justo ahora
                }
            }
            this.cama = null;
        }
    }

    /** Dice algo en alto (bocadillo sobre su cabeza, sin IA): charlas de plaza. */
    public void decirEnAlto(Component frase) {
        com.lucarita.yonga.modulos.habitantes.red.HabitantesRed.CANAL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY.with(() -> this),
                new com.lucarita.yonga.modulos.habitantes.red.BurbujaPacket(this.getId(), frase, 80));
    }

    @Override
    public void die(DamageSource fuente) {
        if (this.level() instanceof ServerLevel nivel) {
            soltarCama(nivel);
            com.lucarita.yonga.modulos.habitantes.familia.Familias.de(nivel.getServer()).morir(this.getUUID());
        }
        if (this.lider && this.aldea != null && this.level() instanceof ServerLevel nivel) {
            Aldeas.de(nivel).ponerLider(this.aldea, null);        // la aldea elegirá otro
        }
        super.die(fuente);
    }

    /** Lo que recuerda de un jugador, si le conoce (null si no). */
    @Nullable
    public Recuerdo recuerdoSiExiste(java.util.UUID jugador) {
        return this.recuerdos.get(jugador);
    }

    public int rasgo1() {
        return Math.max(0, this.rasgo1);
    }

    public int rasgo2() {
        return Math.max(0, this.rasgo2);
    }

    /** Se queda quieto mirando a quien le habla (ConversarGoal) hasta que se cierre la conversación o pasen los ticks. */
    public void conversarCon(Player jugador, int ticks) {
        this.interlocutor = jugador;
        this.conversandoHasta = this.level().getGameTime() + ticks;
        this.getNavigation().stop();
    }

    public void terminarConversacion() {
        this.interlocutor = null;
    }

    /** Se aleja unos bloques de alguien (al echarle o cuando le atacan). */
    public void alejarseDe(Player jugador) {
        net.minecraft.world.phys.Vec3 destino = net.minecraft.world.entity.ai.util.DefaultRandomPos.getPosAway(this, 10, 5, jugador.position());
        if (destino != null) {
            this.getNavigation().moveTo(destino.x, destino.y, destino.z, 0.65);
        }
    }

    /** Si un jugador le pega: le cae mucho peor y, si estaban hablando, corta la conversación. */
    @Override
    public boolean hurt(DamageSource fuente, float cantidad) {
        boolean herido = super.hurt(fuente, cantidad);
        if (herido && !this.level().isClientSide && fuente.getEntity() instanceof ServerPlayer atacante) {
            recuerdo(atacante).cambiarOpinion(-25);
            if (atacante.getUUID().equals(this.seguido)) {
                this.seguido = null;
            }
            boolean conversando = this.interlocutor == atacante;
            if (this.enemigo == null && !this.guardia && esPeleon()) {
                // Los gruñones y los orgullosos devuelven los golpes y van a por él
                if (conversando) Conversaciones.cortarSinIrse(atacante, this);
                empezarPelea(atacante, false);
            } else if (conversando) {
                Conversaciones.cortar(atacante, this);
            }
        }
        if (herido && this.enemigo != null && this.getHealth() < this.getMaxHealth() * 0.35f) {
            terminarPelea();                      // malherido: deja la pelea y huye (pánico)
        }
        return herido;
    }

    // =====================================================================================================
    // SEGUIR Y PELEAR
    // =====================================================================================================

    /** A quién sigue (un jugador amigo), o null. Se guarda. */
    @Nullable private java.util.UUID seguido;
    /** Con quién se pelea, o null. No se guarda: al cargar el mundo se le ha pasado. */
    @Nullable private Player enemigo;
    /** true = le pega una vez y se va; false = va a por él hasta darle unos cuantos golpes. */
    private boolean soloUnGolpe;
    private int golpesDados;
    private long peleaHasta;

    public static final int AMABLE = 0, GRUNON = 1, TIMIDO = 2, DESCONFIADO = 4, AVARICIOSO = 6, ORGULLOSO = 8;

    public static final int CHARLATAN = 3, ALEGRE = 5, GENEROSO = 7, CURIOSO = 9, SUPERSTICIOSO = 10, BROMISTA = 11;

    /** Lo que le cae de entrada un desconocido, según su carácter: la gente abierta empieza fiándose algo. */
    public int primeraImpresion() {
        int p = 0;
        if (tieneRasgo(AMABLE)) p += 12;
        if (tieneRasgo(ALEGRE)) p += 10;
        if (tieneRasgo(GENEROSO)) p += 8;
        if (tieneRasgo(CHARLATAN)) p += 6;
        if (tieneRasgo(CURIOSO)) p += 4;
        if (tieneRasgo(DESCONFIADO)) p -= 10;
        if (tieneRasgo(GRUNON)) p -= 6;
        return p;
    }

    /**
     * Cuánto cuenta lo que le dicen, según su carácter: a los abiertos una buena charla les gana enseguida; a los
     * desconfiados y gruñones les cuesta mucho más. Lo malo cuenta igual para todos.
     */
    public int ajustarOpinion(int cambio) {
        if (cambio <= 0) return cambio;
        double f = 1.0;
        if (tieneRasgo(AMABLE) || tieneRasgo(ALEGRE) || tieneRasgo(GENEROSO) || tieneRasgo(CHARLATAN)) f += 0.5;
        if (tieneRasgo(DESCONFIADO)) f -= 0.45;
        if (tieneRasgo(GRUNON) || tieneRasgo(ORGULLOSO)) f -= 0.2;
        return Math.max(1, (int) Math.round(cambio * f));
    }

    public boolean tieneRasgo(int rasgo) {
        return this.rasgo1 == rasgo || this.rasgo2 == rasgo;
    }

    /** ¿Puede llegar a pegar? Los tímidos nunca, ni los niños ni los bebés. */
    public boolean puedePelear() {
        return etapa() >= 2 && !tieneRasgo(TIMIDO) && barriga() < 0.5f;     // embarazada de más de medio, no
    }

    /** ¿Devuelve los golpes por sí mismo? Los gruñones y los orgullosos. */
    public boolean esPeleon() {
        return puedePelear() && (tieneRasgo(GRUNON) || tieneRasgo(ORGULLOSO));
    }

    public void seguir(Player jugador) {
        this.seguido = jugador.getUUID();
        this.quieto = null;
        this.sigueFamilia = false;
    }

    /**
     * Un recién nacido va detrás de su madre (habitante o jugadora) mientras es bebé, salvo de noche o si ella duerme,
     * y hasta que alguien le diga que le siga, que se quede quieto o que se vaya. Se guarda.
     */
    private boolean sigueFamilia;

    public void seguirPorDefecto(java.util.UUID madre) {
        this.seguido = madre;
        this.quieto = null;
        this.sigueFamilia = true;
    }

    public boolean sigueAFamilia() {
        return this.sigueFamilia;
    }

    public void ponerNombre(String nombre) {
        this.nombre = nombre;
    }

    /** Se queda quieto en un sitio (hasta que le digan que ya puede irse). Se guarda. */
    @Nullable
    private BlockPos quieto;

    public void quedarseQuieto(BlockPos donde) {
        this.quieto = donde.immutable();
        this.seguido = null;
        this.sigueFamilia = false;
        this.getNavigation().stop();
    }

    public void moverseLibre() {
        this.quieto = null;
        if (this.sigueFamilia) {
            this.seguido = null;
            this.sigueFamilia = false;
        }
    }

    @Nullable
    public BlockPos quieto() {
        return this.quieto;
    }

    public void dejarDeSeguir() {
        this.seguido = null;
        this.sigueFamilia = false;
    }

    /** A quién sigue ahora (un jugador, o la madre si es un bebé), o null. */
    @Nullable
    public net.minecraft.world.entity.LivingEntity seguido() {
        if (this.seguido == null) return null;
        net.minecraft.world.entity.Entity e = this.level() instanceof ServerLevel nivel ? nivel.getEntity(this.seguido) : this.level().getPlayerByUUID(this.seguido);
        if (!(e instanceof net.minecraft.world.entity.LivingEntity v) || !v.isAlive()) return null;
        if (this.sigueFamilia && (v.isSleeping() || this.level().isNight())) return null;     // de noche, a dormir
        return v;
    }

    public boolean sigueA(Player jugador) {
        return jugador.getUUID().equals(this.seguido);
    }

    public void empezarPelea(Player jugador, boolean unGolpe) {
        this.enemigo = jugador;
        this.soloUnGolpe = unGolpe;
        this.golpesDados = 0;
        this.peleaHasta = this.level().getGameTime() + (unGolpe ? 400 : 1200);
        this.seguido = null;
        this.interlocutor = null;
    }

    public void terminarPelea() {
        Player era = this.enemigo;
        this.enemigo = null;
        if (era != null) {
            alejarseDe(era);
        }
    }

    @Nullable
    public Player enemigo() {
        if (this.enemigo != null && (!this.enemigo.isAlive() || this.enemigo.isSpectator()
                || this.level().getGameTime() > this.peleaHasta || this.enemigo.level() != this.level())) {
            terminarPelea();
        }
        return this.enemigo;
    }

    /** Ha conseguido darle: cuenta el golpe y, si ya está, se va. */
    public void golpeDado() {
        this.golpesDados++;
        if (this.soloUnGolpe || this.golpesDados >= 4) {
            terminarPelea();
        }
    }

    @Nullable
    public Player interlocutor() {
        return this.interlocutor;
    }

    /**
     * Cómo lo ve un jugador: SIEMPRE por lo que se ve ("La herrera", "Un niño"…), nunca por su nombre, aunque se lo
     * haya dicho: el nombre hay que recordarlo.
     */
    public Component nombrePara(Player jugador) {
        return descripcion();
    }

    public Component descripcion() {
        String sexo = esMujer() ? "f" : "m";
        if (etapa() >= 2 && this.lider) return Component.translatable("yonga.quien.lider." + sexo);
        if (etapa() >= 2 && this.guardia) return Component.translatable("yonga.quien.guardia." + sexo);
        return switch (etapa()) {
            case 0 -> Component.translatable("yonga.quien.bebe." + sexo);
            case 1 -> Component.translatable("yonga.quien.nino." + sexo);
            default -> this.oficio != null ? Component.translatable("yonga.quien." + this.oficio.id() + "." + sexo)
                    : Component.translatable("yonga.quien.aldeano." + sexo);
        };
    }

    /** El nombre del oficio según su sexo ("Herrero" / "Herrera"). */
    public Component nombreOficio() {
        return this.oficio == null ? Component.empty()
                : Component.translatable("yonga.oficio." + this.oficio.id() + (esMujer() ? ".f" : ""));
    }

    /**
     * Cambia el precio para un jugador hasta mañana (lo pacta la conversación).
     * @param producto 0 = todo lo que vende; 1..9 = ese hueco de la tienda (como en resumenTienda)
     * @param modificador −0,35 (rebaja del 35 %) .. +0,2 (le sube un 20 %)
     * @return lo rebajado (vacío = todo), o null si ese hueco está vacío
     */
    @Nullable
    public ItemStack cambiarPrecioPara(Player jugador, int producto, float modificador) {
        ItemStack que = ItemStack.EMPTY;
        if (producto >= 1) {
            Tienda.Oferta o = this.tienda.oferta(producto - 1);
            if (o == null) return null;
            que = o.lote.copy();
        }
        List<Rebaja> lista = this.rebajas.computeIfAbsent(jugador.getUUID(), u -> new ArrayList<>());
        ItemStack buscado = que;
        lista.removeIf(r -> buscado.isEmpty() ? r.producto().isEmpty() : ItemStack.isSameItemSameTags(r.producto(), buscado));
        lista.add(new Rebaja(que, modificador, 0, 0, this.level().getGameTime() + 24000L));
        return que;
    }

    /**
     * Regateo de un hueco (1..9). El servidor pone los números, no la IA:
     *   aceptar      → lo que pidió el jugador, o el mejor trato permitido si pidió demasiado;
     *   contraoferta → a medio camino (la mitad de la rebaja máxima), sin dar más de lo que pidió.
     * Nunca da más que la rebaja máxima (maxPct, según la opinión). Queda pactado hasta mañana.
     * @return el trato que queda y si es exactamente lo que pidió; null si no se puede mejorar el precio
     */
    @Nullable
    public Pacto regatear(Player jugador, int producto, int cantidadPedida, int precioPedido, int maxPct, boolean aceptar) {
        return regatear(jugador, producto, cantidadPedida, precioPedido, maxPct, aceptar, false);
    }

    /** @param sinLimite le da exactamente lo que pidió aunque pase de su rebaja máxima (ha tenido suerte) */
    @Nullable
    public Pacto regatear(Player jugador, int producto, int cantidadPedida, int precioPedido, int maxPct, boolean aceptar,
                          boolean sinLimite) {
        Tienda.Oferta o = this.tienda.oferta(producto - 1);
        if (o == null || (maxPct <= 0 && !sinLimite)) return null;
        float recargo = recargoRumor(jugador, o);              // se regatea desde su precio (inflado o no)
        Trato actual = tratoConModificador(o, recargo);
        Trato limite = tratoConModificador(o, recargo - maxPct / 100.0f);
        Trato pedido = cantidadPedida > 0 && precioPedido > 0
                ? new Trato(Mth.clamp(cantidadPedida, 1, o.lote.getMaxStackSize()), Math.max(1, precioPedido), 0) : null;
        Trato objetivo = aceptar ? (pedido != null ? pedido : limite)
                : tratoConModificador(o, recargo - Math.max(3, maxPct / 2) / 100.0f);
        if (!sinLimite && unidad(objetivo) < unidad(limite)) objetivo = limite;     // nunca más allá del límite
        if (pedido != null && unidad(objetivo) < unidad(pedido)) objetivo = pedido;   // ni más de lo que pidió
        if (unidad(objetivo) >= unidad(actual)) return null;                         // no es una rebaja
        ItemStack que = o.lote.copy();
        List<Rebaja> lista = this.rebajas.computeIfAbsent(jugador.getUUID(), u -> new ArrayList<>());
        lista.removeIf(r -> !r.producto().isEmpty() && ItemStack.isSameItemSameTags(r.producto(), que));
        lista.add(new Rebaja(que, 0.0f, objetivo.cantidad(), objetivo.precio(), this.level().getGameTime() + 24000L));
        boolean loPedido = pedido != null && objetivo.cantidad() == pedido.cantidad() && objetivo.precio() == pedido.precio();
        return new Pacto(describirTrato(jugador, producto), loPedido);
    }

    public record Pacto(Component trato, boolean loPedido) {}

    /**
     * Cuánto pide rebajar el jugador sobre lo que le cuesta ahora (0.2 = un 20 % más barato por unidad), o NaN si no
     * se entiende (hueco vacío o números raros).
     */
    public double rebajaPedida(Player jugador, int producto, int cantidad, int precio) {
        Tienda.Oferta o = this.tienda.oferta(producto - 1);
        if (o == null || cantidad <= 0 || precio <= 0) return Double.NaN;
        Trato ahora = tratoPara(jugador, o);
        return 1.0 - ((double) precio / Math.min(cantidad, o.lote.getMaxStackSize())) / unidad(ahora);
    }

    /**
     * Lo duro que es regateando: el % que rebajaría a alguien que ni le va ni le viene (antes de lo que opine de él).
     * Cada uno el suyo (de su UUID): unos casi no se mueven y otros son de manga ancha; los avariciosos y orgullosos,
     * más duros; los generosos y amables, más blandos.
     */
    public int flexibilidad() {
        if (this.flexFija >= 0) return this.flexFija;
        long semilla = this.getUUID().getMostSignificantBits() * 31 + this.getUUID().getLeastSignificantBits();
        int f = 4 + (int) Math.floorMod(semilla >>> 7, 27L);          // 4..30
        if (tieneRasgo(GENEROSO)) f += 8;
        if (tieneRasgo(AMABLE)) f += 4;
        if (tieneRasgo(AVARICIOSO)) f -= 9;
        if (tieneRasgo(ORGULLOSO)) f -= 4;
        return Mth.clamp(f, 2, 40);
    }

    /**
     * Si este vendedor ha oído que el jugador quiere a toda costa algo que él vende, se lo encarece (los avariciosos,
     * mucho; los generosos y amables, poco). 0 si no.
     */
    public float recargoRumor(Player jugador, Tienda.Oferta o) {
        if (this.aldea == null || !(this.level() instanceof ServerLevel nivel)) return 0.0f;
        Aldeas datos = Aldeas.de(nivel);
        Aldeas.Aldea a = datos.aldeaSiExiste(this.aldea);
        if (a == null) return 0.0f;
        for (Aldeas.Rumor r : datos.rumoresDe(this.aldea, this.getUUID(), nivel.getGameTime())) {
            if (a.esSobre(r, jugador, this.getUUID()) && menciona(r.texto, o.lote.getHoverName().getString())) {
                return tieneRasgo(AVARICIOSO) ? 0.8f : tieneRasgo(GENEROSO) || tieneRasgo(AMABLE) ? 0.15f : 0.4f;
            }
        }
        return 0.0f;
    }

    /** ¿Habla el texto de ese objeto? Palabra a palabra, aceptando plurales ("diamante" en "diamantes"). */
    private static boolean menciona(String texto, String objeto) {
        String t = texto.toLowerCase(java.util.Locale.ROOT);
        boolean alguna = false;
        for (String w : objeto.toLowerCase(java.util.Locale.ROOT).split("[^\\p{L}]+")) {
            if (w.length() < 3 || w.equals("del") || w.equals("los") || w.equals("las") || w.equals("the")) continue;
            alguna = true;
            if (!t.contains(w.length() > 4 ? w.substring(0, w.length() - 1) : w)) return false;
        }
        return alguna;
    }

    /**
     * Le cuenta a un vecino (en alto, en la plaza) un rumor que él sabe y el otro no. Los charlatanes casi siempre;
     * los tímidos, poco. true si lo ha contado.
     */
    public boolean cotillearCon(Habitante otro) {
        if (this.aldea == null || !this.aldea.equals(otro.aldea) || otro.etapa() < 1 || etapa() < 1
                || !(this.level() instanceof ServerLevel nivel)) return false;
        float ganas = tieneRasgo(CHARLATAN) ? 0.9f : tieneRasgo(TIMIDO) ? 0.2f : 0.6f;
        if (this.random.nextFloat() > ganas) return false;
        Aldeas datos = Aldeas.de(nivel);
        Aldeas.Rumor r = datos.paraContar(this.aldea, this.getUUID(), otro.getUUID(), nivel.getGameTime());
        if (r == null) return false;
        datos.contar(r, otro.getUUID());
        decirEnAlto(Component.translatable("yonga.charla.rumor", r.texto));
        return true;
    }

    private static double unidad(Trato t) {
        return (double) t.precio() / t.cantidad();
    }

    /** Le regala un lote de su tienda: el del hueco pedido (1..9) o, si no, el primero que tenga. */
    public ItemStack regalarA(ServerPlayer jugador, int producto) {
        int elegido = -1;
        if (producto >= 1 && this.tienda.oferta(producto - 1) != null) {
            elegido = producto - 1;
        } else {
            for (int i = 0; i < Tienda.HUECOS && elegido < 0; i++) {
                if (this.tienda.oferta(i) != null) elegido = i;
            }
        }
        if (elegido < 0) return ItemStack.EMPTY;
        ItemStack regalo = this.tienda.oferta(elegido).lote.copy();
        this.tienda.quitarLote(elegido);
        ItemHandlerHelper.giveItemToPlayer(jugador, regalo.copy());
        return regalo;
    }

    /** Lo que vende, numerado (1..9, el hueco), con el precio que tiene ahora para ese jugador. */
    public String resumenTienda(Player jugador) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Tienda.HUECOS; i++) {
            Tienda.Oferta o = this.tienda.oferta(i);
            if (o == null) continue;
            Trato t = tratoPara(jugador, o);
            sb.append("\n  ").append(i + 1).append(") ").append(t.cantidad()).append(" x ")
                    .append(o.lote.getHoverName().getString()).append(" por ").append(t.precio())
                    .append(t.precio() == 1 ? " moneda" : " monedas");
            sb.append(" (te quedan ").append(o.lotes).append(o.lotes == 1 ? " lote" : " lotes").append(")");
            if (t.signo() != 0) {
                sb.append(" (lo normal: ").append(o.lote.getCount()).append(" por ").append(o.precio);
                boolean pactado = rebajaExacta(jugador, o) != null || modificador(jugador, o) != null;
                if (pactado) sb.append("; esto lo pactaste con él hasta mañana");
                if (recargoRumor(jugador, o) > 0) sb.append("; se lo has subido porque has oído que lo quiere a toda costa");
                sb.append(")");
            }

        }
        return sb.toString();
    }

    public int nivel() {
        return nivelEfectivo();
    }

    @Nullable
    public Player cliente() {
        return this.cliente;
    }

    public void atender(@Nullable Player jugador) {
        this.cliente = jugador;
        if (jugador != null) {
            this.getNavigation().stop();
        }
    }

    public TiendaEstadoPacket estadoTienda(ServerPlayer jugador) {
        List<TiendaEstadoPacket.Linea> ofertas = new ArrayList<>();
        for (int i = 0; i < Tienda.HUECOS; i++) {
            Tienda.Oferta o = this.tienda.oferta(i);
            if (o == null) {
                ofertas.add(new TiendaEstadoPacket.Linea(ItemStack.EMPTY, 0, 0, 0));
                continue;
            }
            Trato t = tratoPara(jugador, o);
            ItemStack lote = o.lote.copy();
            lote.setCount(t.cantidad());
            ofertas.add(new TiendaEstadoPacket.Linea(lote, t.precio(), t.signo(), o.lotes));
        }
        int n = nivelEfectivo();
        return new TiendaEstadoPacket(this.getId(), this.oficio == null ? 0 : this.oficio.ordinal(), n, this.xp,
                Oficio.XP_NIVEL[n - 1], n < 5 ? Oficio.XP_NIVEL[n] : Oficio.XP_NIVEL[4], esAdulto() && n < 5,
                Math.min(1.0f, this.barra), ofertas);
    }

    public void accionTienda(ServerPlayer jugador, int accion, int indice, boolean todo) {
        if (this.oficio == null) return;
        switch (accion) {
            case TiendaAccionPacket.COMPRAR -> comprar(jugador, indice, todo);
            default -> { }
        }
    }

    private void comprar(ServerPlayer jugador, int i, boolean todo) {
        int hechas = 0;
        int maximo = todo ? Tienda.MAX_LOTES : 1;
        while (hechas < maximo) {
            Tienda.Oferta o = this.tienda.oferta(i);
            if (o == null) break;
            Trato trato = tratoPara(jugador, o);
            int precio = trato.precio();
            if (contar(jugador, MONEDA) < precio) {
                if (hechas == 0) aviso(jugador, "yonga.tienda.sin_monedas");
                break;
            }
            quitar(jugador, MONEDA, precio);
            ItemStack lote = o.lote.copy();
            lote.setCount(trato.cantidad());
            ItemHandlerHelper.giveItemToPlayer(jugador, lote);
            if (this.aldea != null && this.level() instanceof ServerLevel nivel) {
                Aldeas.de(nivel).cambiarFama(this.aldea, jugador.getUUID(), 1);     // comerciar da buena fama
            }
            this.tienda.quitarLote(i);
            ganarXp(precio);
            hechas++;
        }
        if (hechas > 0) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.6f, 1.0f);
        }
    }

    /**
     * Lo que paga un jugador por un hueco: cuántos objetos se lleva y por cuántas monedas, con lo pactado hablando
     * (de ese objeto si lo hay; si no, de todo). Si el precio no puede bajar más (p. ej. 1 moneda), el trato se
     * cumple dando más cantidad: 16 ladrillos por 1 moneda con un 20 % de rebaja pasan a ser 20 por 1.
     * @param signo 1 mejor de lo normal, −1 peor, 0 normal
     */
    public record Trato(int cantidad, int precio, int signo) {}

    private Trato tratoPara(Player jugador, Tienda.Oferta o) {
        Rebaja exacta = rebajaExacta(jugador, o);
        if (exacta != null) {
            return new Trato(exacta.cantidad(), exacta.precio(), signo(o, exacta.cantidad(), exacta.precio()));
        }
        Float mod = modificador(jugador, o);
        float recargo = recargoRumor(jugador, o);
        if (mod == null && recargo == 0.0f) return new Trato(o.lote.getCount(), o.precio, 0);
        return tratoConModificador(o, (mod == null ? 0.0f : mod) + recargo);
    }

    private static Trato tratoConModificador(Tienda.Oferta o, float mod) {
        int cantidad = o.lote.getCount();
        if (Math.abs(mod) < 0.001f) {
            return new Trato(cantidad, o.precio, 0);
        }
        float ideal = o.precio * (1.0f + mod);
        int precio = Math.max(1, Math.round(ideal));
        int cant = Mth.clamp(Math.round(cantidad * precio / ideal), 1, o.lote.getMaxStackSize());
        if (precio == o.precio && cant == cantidad) {
            // Ni el precio ni la cantidad recogen el trato al redondear (lotes pequeños o baratos): se mueve una
            // unidad, que es lo mínimo que se puede regatear (4 panes por 1 moneda → 5 por 1)
            if (mod < 0) {
                if (cantidad < o.lote.getMaxStackSize()) cant = cantidad + 1;
                else precio = Math.max(1, o.precio - 1);
            } else {
                if (cantidad > 1) cant = cantidad - 1;
                else precio = o.precio + 1;
            }
        }
        return new Trato(cant, precio, signo(o, cant, precio));
    }

    private static int signo(Tienda.Oferta o, int cantidad, int precio) {
        float nuevo = (float) precio / cantidad, normal = (float) o.precio / o.lote.getCount();
        return nuevo < normal - 1.0E-4f ? 1 : nuevo > normal + 1.0E-4f ? -1 : 0;
    }

    @Nullable
    private Rebaja rebajaExacta(Player jugador, Tienda.Oferta o) {
        List<Rebaja> lista = this.rebajas.get(jugador.getUUID());
        if (lista == null) return null;
        long ahora = this.level().getGameTime();
        lista.removeIf(r -> ahora > r.hasta());
        for (Rebaja r : lista) {
            if (r.exacta() && ItemStack.isSameItemSameTags(r.producto(), o.lote)) return r;
        }
        return null;
    }

    @Nullable
    private Float modificador(Player jugador, Tienda.Oferta o) {
        List<Rebaja> lista = this.rebajas.get(jugador.getUUID());
        if (lista == null) return null;
        long ahora = this.level().getGameTime();
        lista.removeIf(r -> ahora > r.hasta());
        Float general = null, concreta = null;
        for (Rebaja r : lista) {
            if (r.exacta()) continue;
            if (r.producto().isEmpty()) general = r.modificador();
            else if (ItemStack.isSameItemSameTags(r.producto(), o.lote)) concreta = r.modificador();
        }
        return concreta != null ? concreta : general;
    }

    /** "20 Ladrillos por 1 moneda": el trato actual de un hueco (1..9) para un jugador, o null si está vacío. */
    @Nullable
    public Component describirTrato(Player jugador, int producto) {
        Tienda.Oferta o = this.tienda.oferta(producto - 1);
        if (o == null) return null;
        Trato t = tratoPara(jugador, o);
        return Component.translatable(t.precio() == 1 ? "yonga.tienda.trato_una" : "yonga.tienda.trato",
                t.cantidad(), o.lote.getHoverName(), t.precio());
    }

    /** Experiencia por comerciar (1 + las monedas que se mueven). Solo suben de nivel los adultos. */
    private void ganarXp(int monedas) {
        if (!esAdulto() || this.nivel >= 5) return;
        this.xp += 1 + monedas;
        boolean subio = false;
        while (this.nivel < 5 && this.xp >= Oficio.XP_NIVEL[this.nivel]) {
            this.nivel++;
            subio = true;
        }
        if (subio && this.level() instanceof ServerLevel servidor) {
            servidor.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + getBbHeight() + 0.3, getZ(), 12, 0.4, 0.3, 0.4, 0.0);
            this.level().playSound(null, this.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.5f, 1.2f);
        }
    }

    // ---------- monedas y objetos del jugador ----------

    private static final Predicate<ItemStack> MONEDA = s -> s.is(ModItems.MONEDA.get());

    private static int contar(Player jugador, Predicate<ItemStack> que) {
        return jugador.getInventory().clearOrCountMatchingItems(que, 0, jugador.inventoryMenu.getCraftSlots());
    }

    private static void quitar(Player jugador, Predicate<ItemStack> que, int cuantos) {
        jugador.getInventory().clearOrCountMatchingItems(que, cuantos, jugador.inventoryMenu.getCraftSlots());
    }

    private static void aviso(Player jugador, String clave) {
        jugador.displayClientMessage(Component.translatable(clave), true);
    }

    private void actualizarCrecimiento() {
        long edad = Math.max(0, this.level().getGameTime() - this.nacimiento);
        long b = HabitantesConfig.ticksBebe(), n = HabitantesConfig.ticksNino(), j = HabitantesConfig.ticksJoven();
        float crecimiento;
        if (edad < b) crecimiento = (float) edad / b;
        else if (edad < b + n) crecimiento = 1 + (float) (edad - b) / n;
        else if (edad < b + n + j) crecimiento = 2 + (float) (edad - b - n) / j;
        else crecimiento = 3;
        // Solo se manda si cambia lo bastante como para notarse (cada ~0,1 % de etapa)
        if (Math.abs(crecimiento - this.crecimiento()) > 0.001f || crecimiento == 3 && this.crecimiento() != 3) {
            this.entityData.set(CRECIMIENTO, crecimiento);
        }
        int etapa = etapa();
        if (this.sigueFamilia && etapa > 0) dejarDeSeguir();          // ya no es un bebé: va a su aire
        // Un bebé no sabe abrir puertas: el camino no puede pasar por ellas
        if (this.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav) {
            nav.setCanOpenDoors(etapa > 0);
        }
        if (etapa != this.etapaAplicada) {
            this.etapaAplicada = etapa;
            aplicarEtapa(etapa);
        }
    }

    /** Vida y velocidad de cada etapa: el bebé apenas gatea, el niño corretea. */
    private void aplicarEtapa(int etapa) {
        double[] vida = {6, 12, 18, this.guardia ? 30 : 20};
        double[] velocidad = {0.15, 0.55, 0.5, 0.5};
        AttributeInstance maxVida = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxVida != null) {
            boolean llena = this.getHealth() >= maxVida.getValue();
            maxVida.setBaseValue(vida[etapa]);
            if (llena || this.getHealth() > this.getMaxHealth()) {
                this.setHealth(this.getMaxHealth());
            }
        }
        AttributeInstance rapidez = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (rapidez != null) {
            rapidez.setBaseValue(velocidad[etapa]);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> dato) {
        super.onSyncedDataUpdated(dato);
        if (CRECIMIENTO.equals(dato) || ALTURA.equals(dato)) {
            this.refreshDimensions();
        }
    }

    /** Tamaño respecto a un adulto de altura media (LivingEntity lo aplica a la caja de choque). */
    @Override
    public float getScale() {
        return escala(crecimiento()) * altura();
    }

    /**
     * Escala del cuerpo según el crecimiento: bebé 0,42 → 0,5, niño 0,5 → 0,72, joven 0,72 → 1, adulto 1.
     * El bebé tiene además sus propias proporciones (cabezón, cuerpo y miembros cortos: ver ModeloHabitante).
     */
    public static float escala(float crecimiento) {
        if (crecimiento < 1) return Mth.lerp(crecimiento, 0.42f, 0.5f);
        if (crecimiento < 2) return Mth.lerp(crecimiento - 1, 0.5f, 0.72f);
        if (crecimiento < 3) return Mth.lerp(crecimiento - 2, 0.72f, 1.0f);
        return 1.0f;
    }

    /** Cabeza respecto al cuerpo: los pequeños son cabezones. */
    public static float escalaCabeza(float crecimiento) {
        if (crecimiento < 1) return 1.0f;            // el bebé ya va proporcionado en su modelo
        if (crecimiento < 2) return Mth.lerp(crecimiento - 1, 1.4f, 1.15f);
        if (crecimiento < 3) return Mth.lerp(crecimiento - 2, 1.15f, 1.0f);
        return 1.0f;
    }

    /** El bebé va sentado o a gatas: su caja es baja y cuadrada. */
    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        if (crecimiento() < 1.0f) {
            return net.minecraft.world.entity.EntityDimensions.scalable(0.5f, 0.55f);
        }
        // Los altos se dibujan más grandes, pero su caja no pasa de 1,9 bloques: si no, no caben por las puertas
        var d = super.getDimensions(pose);
        return d.height > 1.9f || d.width > 0.62f
                ? net.minecraft.world.entity.EntityDimensions.scalable(Math.min(d.width, 0.62f), Math.min(d.height, 1.9f)) : d;
    }

    // ---------- solo en el cliente: cuánto está gateando (0 sentado, 1 a gatas), suavizado ----------
    public float gateo, gateoAntes;

    private void tickCliente() {
        this.gateoAntes = this.gateo;
        double dx = this.getX() - this.xo, dz = this.getZ() - this.zo;
        boolean moviendose = dx * dx + dz * dz > 1.0E-5;
        float objetivo = moviendose ? 1.0f : 0.0f;
        this.gateo += Mth.clamp(objetivo - this.gateo, -0.12f, 0.12f);
    }

    @Override
    public float getVoicePitch() {
        float base = (this.random.nextFloat() - this.random.nextFloat()) * 0.2f + 1.0f;
        return base + (3 - crecimiento()) * 0.15f + (esMujer() ? 0.15f : 0.0f);
    }

    // =====================================================================================================
    // LEER
    // =====================================================================================================

    public Epoca epoca() {
        return Epoca.de(this.entityData.get(EPOCA));
    }

    public boolean esMujer() {
        return this.entityData.get(MUJER);
    }

    public float crecimiento() {
        return this.entityData.get(CRECIMIENTO);
    }

    /** 0 bebé, 1 niño, 2 joven, 3 adulto. */
    public int etapa() {
        return Math.min(3, (int) crecimiento());
    }

    public boolean esAdulto() {
        return etapa() == 3;
    }

    public float altura() {
        return this.entityData.get(ALTURA);
    }

    public float complexion() {
        return this.entityData.get(COMPLEXION);
    }

    private int campo(int desde, int bits) {
        return (this.entityData.get(ASPECTO) >> desde) & ((1 << bits) - 1);
    }

    public int tonoPiel() {
        return campo(B_PIEL, 3) % TONOS;
    }

    public int cara() {
        return campo(B_CARA, 3) % CARAS;
    }

    private int campo2(int desde) {
        return (this.entityData.get(ASPECTO2) >> desde) & 31;
    }

    public static int componerAspecto2(int ropa, int peinado, int barba, int complemento, int marca) {
        return (Mth.clamp(ropa, 0, ROPAS - 1) << A_ROPA) | (Mth.clamp(peinado, 0, PEINADOS - 1) << A_PEINADO)
                | (Mth.clamp(barba, 0, BARBAS - 1) << A_BARBA) | (Mth.clamp(complemento, 0, COMPLEMENTOS - 1) << A_COMPLEMENTO)
                | (Mth.clamp(marca, 0, MARCAS - 1) << A_MARCA);
    }

    public int aspecto2() {
        return this.entityData.get(ASPECTO2);
    }

    /** 0 sin barba; 1..20 las barbas de su época. Solo cuenta en hombres adultos. */
    public int barba() {
        return esMujer() || !esAdulto() ? 0 : barbaGuardada();
    }

    public int barbaGuardada() {
        return campo2(A_BARBA) % BARBAS;
    }

    /** 0 sin complemento; 1..20 los de su época (gorros, tocados, joyas…). Los bebés, nada. */
    public int complemento() {
        return etapa() == 0 ? 0 : campo2(A_COMPLEMENTO) % COMPLEMENTOS;
    }

    /** 0 sin marca; 1..20 los rasgos de cara de su época (pecas, cicatrices, maquillaje, pinturas…). */
    public int marca() {
        return etapa() == 0 ? 0 : campo2(A_MARCA) % MARCAS;
    }

    public int colorOjos() {
        return campo(B_OJOS, 3) % OJOS;
    }

    public int peinado() {
        return campo2(A_PEINADO) % PEINADOS;
    }

    public int colorPelo() {
        return campo(B_COLOR, 4) % COLORES_PELO;
    }

    public int ropa() {
        return campo2(A_ROPA) % ROPAS;
    }

    /** Tamaño del pecho, 0 (muy pequeño) .. 7 (muy grande). */
    public int pecho() {
        return campo(B_PECHO, 3) % PECHOS;
    }

    /** Caída del pecho, 0 (recto) .. 3 (caído). */
    public int caidaPecho() {
        return campo(B_CAIDA, 2) % CAIDAS;
    }

    /** Solo en el servidor. Nunca se muestra encima de la cabeza. */
    public String nombre() {
        return this.nombre;
    }

    public long nacimiento() {
        return this.nacimiento;
    }

    @Nullable
    public Oficio oficio() {
        return this.oficio;
    }

    @Nullable
    public BlockPos puesto() {
        return this.puesto;
    }

    // =====================================================================================================
    // GUARDAR
    // =====================================================================================================

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        CompoundTag h = new CompoundTag();
        h.putInt("epoca", this.entityData.get(EPOCA));
        h.putBoolean("mujer", esMujer());
        h.putFloat("altura", altura());
        h.putFloat("complexion", complexion());
        h.putInt("aspecto", this.entityData.get(ASPECTO));
        h.putInt("aspecto2", this.entityData.get(ASPECTO2));
        h.putString("nombre", this.nombre);
        if (this.nacimiento != Long.MIN_VALUE) {
            h.putLong("nacimiento", this.nacimiento);
        }
        if (this.oficio != null) {
            CompoundTag o = new CompoundTag();
            o.putString("id", this.oficio.id());
            o.putInt("nivel", this.nivel);
            o.putInt("xp", this.xp);
            o.putFloat("barra", this.barra);
            if (this.puesto != null) o.put("puesto", NbtUtils.writeBlockPos(this.puesto));
            o.put("tienda", this.tienda.guardar());
            h.put("oficio", o);
        }
        h.putInt("rasgo1", this.rasgo1);
        h.putInt("orientacionFija", this.orientacionFija);
        h.putInt("flexFija", this.flexFija);
        h.putBoolean("guardia", this.guardia);
        h.putBoolean("holgazan", this.holgazan);
        if (this.cama != null) h.putLong("cama", this.cama.asLong());
        if (this.aldea != null) h.putLong("aldea", this.aldea.asLong());
        if (this.seguido != null) h.putUUID("sigue", this.seguido);
        h.putBoolean("sigueFamilia", this.sigueFamilia);
        if (this.quieto != null) h.putLong("quieto", this.quieto.asLong());
        h.putInt("rasgo2", this.rasgo2);
        CompoundTag mem = new CompoundTag();
        this.recuerdos.forEach((uuid, r) -> mem.put(uuid.toString(), r.guardar()));
        h.put("recuerdos", mem);
        nbt.put("Habitante", h);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("Habitante")) {
            leerHabitante(nbt.getCompound("Habitante"));
        }
        // Los huevos de hombre y de mujer llevan esta marca: cambia el sexo (y el nombre, a uno del sexo nuevo)
        if (nbt.contains(HuevoHabitanteItem.MARCA_SEXO)) {
            boolean mujer = nbt.getBoolean(HuevoHabitanteItem.MARCA_SEXO);
            if (mujer != esMujer()) {
                this.entityData.set(MUJER, mujer);
                this.nombre = Nombres.elegir(epoca(), mujer, this.random);
            }
        }
    }

    private void leerHabitante(CompoundTag h) {
        if (h.isEmpty()) {
            return;
        }
        this.entityData.set(EPOCA, h.getInt("epoca"));
        this.entityData.set(MUJER, h.getBoolean("mujer"));
        this.entityData.set(ALTURA, h.contains("altura") ? h.getFloat("altura") : 1.0f);
        this.entityData.set(COMPLEXION, h.contains("complexion") ? h.getFloat("complexion") : 1.0f);
        this.entityData.set(ASPECTO, h.getInt("aspecto"));
        if (h.contains("aspecto2")) {
            this.entityData.set(ASPECTO2, h.getInt("aspecto2"));
        } else {
            // De antes de la fase 7: ropa, peinado y barba venían en ASPECTO
            int viejo = h.getInt("aspecto");
            this.entityData.set(ASPECTO2, componerAspecto2((viejo >> B_ROPA) & 15, (viejo >> B_PEINADO) & 7,
                    (viejo >> B_BARBA) & 7, 0, 0));
        }
        this.nombre = h.getString("nombre");
        this.rasgo1 = h.contains("rasgo1") ? h.getInt("rasgo1") : -1;
        this.orientacionFija = h.contains("orientacionFija") ? h.getInt("orientacionFija") : -1;
        this.flexFija = h.contains("flexFija") ? h.getInt("flexFija") : -1;
        this.guardia = h.getBoolean("guardia");
        this.holgazan = h.getBoolean("holgazan");
        this.cama = h.contains("cama") ? BlockPos.of(h.getLong("cama")) : null;
        this.aldea = h.contains("aldea") ? BlockPos.of(h.getLong("aldea")) : null;
        this.seguido = h.hasUUID("sigue") ? h.getUUID("sigue") : null;
        this.sigueFamilia = h.getBoolean("sigueFamilia");
        this.quieto = h.contains("quieto") ? BlockPos.of(h.getLong("quieto")) : null;
        this.rasgo2 = h.contains("rasgo2") ? h.getInt("rasgo2") : -1;
        this.recuerdos.clear();
        CompoundTag mem = h.getCompound("recuerdos");
        for (String clave : mem.getAllKeys()) {
            try {
                this.recuerdos.put(UUID.fromString(clave), Recuerdo.cargar(mem.getCompound(clave)));
            } catch (IllegalArgumentException ignorado) {
                // clave rara: se ignora
            }
        }
        if (h.contains("oficio")) {
            CompoundTag o = h.getCompound("oficio");
            this.oficio = Oficio.porId(o.getString("id"));
            this.nivel = Mth.clamp(o.getInt("nivel"), 1, 5);
            this.xp = o.getInt("xp");
            this.barra = o.getFloat("barra");
            this.puesto = o.contains("puesto") ? NbtUtils.readBlockPos(o.getCompound("puesto")) : null;
            this.tienda.cargar(o.getCompound("tienda"));
        }
        if (h.contains("nacimiento")) {
            this.nacimiento = h.getLong("nacimiento");
            actualizarCrecimiento();
        }
    }

    // =====================================================================================================
    // VARIOS
    // =====================================================================================================

    @Override
    public boolean removeWhenFarAway(double distancia) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource fuente) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }
}
