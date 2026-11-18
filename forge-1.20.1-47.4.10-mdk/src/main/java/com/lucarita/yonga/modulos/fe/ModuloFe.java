package com.lucarita.yonga.modulos.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.hechizos.Hechizo;
import com.lucarita.yonga.modulos.fe.hechizos.Hechizos;
import com.lucarita.yonga.modulos.fe.hechizos.LibroHechizo;
import com.lucarita.yonga.modulos.fe.hechizos.RecetaLibro;
import com.lucarita.yonga.modulos.fe.red.FeRed;
import com.lucarita.yonga.modulos.fe.mobs.AcechoAstado;
import com.lucarita.yonga.modulos.fe.mobs.Astado;
import com.lucarita.yonga.modulos.fe.mobs.Dardo;
import com.lucarita.yonga.modulos.fe.mobs.Duende;
import com.lucarita.yonga.modulos.fe.mobs.Centinela;
import com.lucarita.yonga.modulos.fe.mobs.EstatuaCentinela;
import com.lucarita.yonga.modulos.fe.mobs.Enano;
import com.lucarita.yonga.modulos.fe.mobs.PezDelViento;
import com.lucarita.yonga.modulos.fe.mobs.PezIlusion;
import com.lucarita.yonga.modulos.fe.mobs.RitualPez;
import com.lucarita.yonga.modulos.fe.mobs.RitualZor;
import com.lucarita.yonga.modulos.fe.mobs.ZorElQuemado;
import com.lucarita.yonga.modulos.fe.mobs.Gargola;
import com.lucarita.yonga.modulos.fe.mobs.Gnomo;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.lucarita.yonga.modulos.fe.mobs.EnanoMadre;
import com.lucarita.yonga.modulos.fe.mobs.LeonDeGuerra;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import com.lucarita.yonga.modulos.fe.mobs.Hada;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Módulo de fe: puntos, niveles (-100 a 100), rangos y maná de cada jugador, oración, orbes de fe y libros de hechizos.
 * Solo se registra si el módulo está activado (ver YongaMain).
 * La parte visual (barra, menú, botón del inventario) está en el paquete client.
 */
public final class ModuloFe {

    public static final DeferredRegister<EntityType<?>> ENTIDADES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, YongaMain.MOD_ID);

    // Orbe de fe (azul o negro) que sueltan los mobs al morir. Mismo tamaño y alcance de seguimiento que la experiencia.
    public static final RegistryObject<EntityType<OrbeFe>> ORBE_FE = ENTIDADES.register("orbe_fe",
            () -> EntityType.Builder.<OrbeFe>of(OrbeFe::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(6)
                    .updateInterval(20)
                    .build("orbe_fe"));

    // ---------- MOBS DE FE ----------
    // Hadas: seres inferiores, la luminosa (azul) y la oscura (morada). Pequeñas bolitas voladoras.
    public static final RegistryObject<EntityType<Hada>> HADA = ENTIDADES.register("hada",
            () -> EntityType.Builder.<Hada>of((tipo, nivel) -> new Hada(tipo, nivel, false), MobCategory.AMBIENT)
                    .sized(0.4f, 0.4f)
                    .clientTrackingRange(8)
                    .build("hada"));
    public static final RegistryObject<EntityType<Hada>> HADA_OSCURA = ENTIDADES.register("hada_oscura",
            () -> EntityType.Builder.<Hada>of((tipo, nivel) -> new Hada(tipo, nivel, true), MobCategory.AMBIENT)
                    .sized(0.4f, 0.4f)
                    .clientTrackingRange(8)
                    .build("hada_oscura"));

    // Enanos: seres inferiores, el luminoso (amarillo) y el oscuro (rojo). Andan lento y embisten.
    public static final RegistryObject<EntityType<Enano>> ENANO = ENTIDADES.register("enano",
            () -> EntityType.Builder.<Enano>of((tipo, nivel) -> new Enano(tipo, nivel, false), MobCategory.AMBIENT)
                    .sized(0.8f, 0.7f)
                    .clientTrackingRange(8)
                    .build("enano"));
    public static final RegistryObject<EntityType<Enano>> ENANO_OSCURO = ENTIDADES.register("enano_oscuro",
            () -> EntityType.Builder.<Enano>of((tipo, nivel) -> new Enano(tipo, nivel, true), MobCategory.AMBIENT)
                    .sized(0.8f, 0.7f)
                    .clientTrackingRange(8)
                    .build("enano_oscuro"));

    // ---------- LIBROS DE HECHIZOS ----------

    // ---------- EL ASTADO (ser oscuro superior) ----------
    // No aparece de forma normal: lo trae la fase de acecho (AcechoAstado).
    public static final RegistryObject<EntityType<Astado>> ASTADO = ENTIDADES.register("astado",
            () -> EntityType.Builder.<Astado>of(Astado::new, MobCategory.MONSTER)
                    .sized(0.9f, 2.9f)
                    .clientTrackingRange(10)
                    .build("astado"));

    // ---------- EL LEÓN DE GUERRA (ser luminoso superior): arquero y espadachín ----------
    public static final RegistryObject<EntityType<LeonDeGuerra>> LEON_ARQUERO = ENTIDADES.register("leon_arquero",
            () -> EntityType.Builder.<LeonDeGuerra>of((tipo, nivel) -> new LeonDeGuerra(tipo, nivel, true), MobCategory.CREATURE)
                    .sized(1.3f, 2.6f)
                    .clientTrackingRange(10)
                    .build("leon_arquero"));
    public static final RegistryObject<EntityType<LeonDeGuerra>> LEON_ESPADACHIN = ENTIDADES.register("leon_espadachin",
            () -> EntityType.Builder.<LeonDeGuerra>of((tipo, nivel) -> new LeonDeGuerra(tipo, nivel, false), MobCategory.CREATURE)
                    .sized(1.3f, 2.6f)
                    .clientTrackingRange(10)
                    .build("leon_espadachin"));

    // ---------- EL DUENDE (ser oscuro inferior) y su dardo ----------
    public static final RegistryObject<EntityType<Duende>> DUENDE = ENTIDADES.register("duende",
            () -> EntityType.Builder.<Duende>of(Duende::new, MobCategory.AMBIENT)
                    .sized(0.6f, 1.1f)
                    .clientTrackingRange(8)
                    .build("duende"));
    public static final RegistryObject<EntityType<Dardo>> DARDO = ENTIDADES.register("dardo",
            () -> EntityType.Builder.<Dardo>of(Dardo::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("dardo"));

    // ---------- LA ENANA MADRE (seres superiores, tipo miniboss): luminosa y oscura ----------
    public static final RegistryObject<EntityType<EnanoMadre>> ENANO_MADRE = ENTIDADES.register("enano_madre",
            () -> EntityType.Builder.<EnanoMadre>of((tipo, nivel) -> new EnanoMadre(tipo, nivel, false), MobCategory.CREATURE)
                    .sized(2.4f, 2.1f)
                    .clientTrackingRange(10)
                    .build("enano_madre"));
    public static final RegistryObject<EntityType<EnanoMadre>> ENANO_MADRE_OSCURO = ENTIDADES.register("enano_madre_oscuro",
            () -> EntityType.Builder.<EnanoMadre>of((tipo, nivel) -> new EnanoMadre(tipo, nivel, true), MobCategory.CREATURE)
                    .sized(2.4f, 2.1f)
                    .clientTrackingRange(10)
                    .build("enano_madre_oscuro"));

    // ---------- EL CENTINELA DE PIEDRA (ser luminoso superior) y su estatua trampa ----------
    public static final RegistryObject<EntityType<Centinela>> CENTINELA = ENTIDADES.register("centinela",
            () -> EntityType.Builder.<Centinela>of(Centinela::new, MobCategory.MONSTER)
                    .sized(1.4f, 2.9f)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("centinela"));

    public static final DeferredRegister<Block> BLOQUES =
            DeferredRegister.create(ForgeRegistries.BLOCKS, YongaMain.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTIDADES_BLOQUE =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, YongaMain.MOD_ID);
    public static final RegistryObject<Block> CALABAZA_CENTINELA = BLOQUES.register("calabaza_centinela",
            EstatuaCentinela.Calabaza::new);
    public static final RegistryObject<BlockEntityType<EstatuaCentinela.Vigia>> VIGIA_CENTINELA = ENTIDADES_BLOQUE.register("vigia_centinela",
            () -> BlockEntityType.Builder.of(EstatuaCentinela.Vigia::new, CALABAZA_CENTINELA.get()).build(null));

    // ---------- EL GNOMO (ser luminoso inferior): espadachín pequeño y rapidísimo ----------
    public static final RegistryObject<EntityType<Gnomo>> GNOMO = ENTIDADES.register("gnomo",
            () -> EntityType.Builder.<Gnomo>of(Gnomo::new, MobCategory.AMBIENT)
                    .sized(0.5f, 1.0f)
                    .clientTrackingRange(8)
                    .build("gnomo"));

    // ---------- LA GÁRGOLA (ser oscuro inferior): torpe de día, de noche te sube al cielo ----------
    public static final RegistryObject<EntityType<Gargola>> GARGOLA = ENTIDADES.register("gargola",
            () -> EntityType.Builder.<Gargola>of(Gargola::new, MobCategory.MONSTER)
                    .sized(0.8f, 1.0f)
                    .clientTrackingRange(10)
                    .build("gargola"));

    // ---------- EL PEZ DEL VIENTO (primer jefe) y sus ilusiones ----------
    public static final RegistryObject<EntityType<PezDelViento>> PEZ_DEL_VIENTO = ENTIDADES.register("pez_del_viento",
            () -> EntityType.Builder.<PezDelViento>of(PezDelViento::new, MobCategory.MONSTER)
                    .sized(7.0f, 5.0f)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("pez_del_viento"));
    public static final RegistryObject<EntityType<PezIlusion>> PEZ_ILUSION = ENTIDADES.register("pez_ilusion",
            () -> EntityType.Builder.<PezIlusion>of(PezIlusion::new, MobCategory.MISC)
                    .sized(7.0f, 5.0f)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("pez_ilusion"));

    // ---------- ZOR EL QUEMADO (segundo jefe): el dragón serpiente que sale de la tierra ----------
    public static final RegistryObject<EntityType<ZorElQuemado>> ZOR = ENTIDADES.register("zor",
            () -> EntityType.Builder.<ZorElQuemado>of(ZorElQuemado::new, MobCategory.MONSTER)
                    .sized(2.0f, 2.0f)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("zor"));

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, YongaMain.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECETAS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, YongaMain.MOD_ID);

    public static final RegistryObject<Item> LIBRO_GRAN_SALTO = libro("libro_gran_salto", Hechizo.GRAN_SALTO);
    public static final RegistryObject<Item> LIBRO_CASTIGO_DEL_CIELO = libro("libro_castigo_del_cielo", Hechizo.CASTIGO_DEL_CIELO);
    public static final RegistryObject<Item> LIBRO_EXPULSION = libro("libro_expulsion", Hechizo.EXPULSION);
    public static final RegistryObject<Item> LIBRO_PROYECTIL_OSCURO = libro("libro_proyectil_oscuro", Hechizo.PROYECTIL_OSCURO);

    // Huevos de los mobs de fe (colores: fondo y manchas)
    public static final RegistryObject<Item> HUEVO_HADA = ITEMS.register("huevo_hada",
            () -> new ForgeSpawnEggItem(HADA, 0x8CD9FF, 0xE6FAFF, new Item.Properties()));
    public static final RegistryObject<Item> HUEVO_HADA_OSCURA = ITEMS.register("huevo_hada_oscura",
            () -> new ForgeSpawnEggItem(HADA_OSCURA, 0x7A2FC8, 0xE0B8FF, new Item.Properties()));

    public static final RegistryObject<Item> HUEVO_ENANO = ITEMS.register("huevo_enano",
            () -> new ForgeSpawnEggItem(ENANO, 0xF0C83C, 0xFFF6B0, new Item.Properties()));
    public static final RegistryObject<Item> HUEVO_ENANO_OSCURO = ITEMS.register("huevo_enano_oscuro",
            () -> new ForgeSpawnEggItem(ENANO_OSCURO, 0xB4201E, 0xFF8A6A, new Item.Properties()));

    public static final RegistryObject<Item> HUEVO_ASTADO = ITEMS.register("huevo_astado",
            () -> new ForgeSpawnEggItem(ASTADO, 0x6E6252, 0xD6CCB2, new Item.Properties()));

    public static final RegistryObject<Item> HUEVO_LEON_ARQUERO = ITEMS.register("huevo_leon_arquero",
            () -> new ForgeSpawnEggItem(LEON_ARQUERO, 0xC69850, 0xE8B838, new Item.Properties()));
    public static final RegistryObject<Item> HUEVO_LEON_ESPADACHIN = ITEMS.register("huevo_leon_espadachin",
            () -> new ForgeSpawnEggItem(LEON_ESPADACHIN, 0xC69850, 0xEAE6DA, new Item.Properties()));

    public static final RegistryObject<Item> HUEVO_DUENDE = ITEMS.register("huevo_duende",
            () -> new ForgeSpawnEggItem(DUENDE, 0x7C40A8, 0x46A03C, new Item.Properties()));

    public static final RegistryObject<Item> HUEVO_ENANO_MADRE = ITEMS.register("huevo_enano_madre",
            () -> new ForgeSpawnEggItem(ENANO_MADRE, 0xF0C83C, 0xFFFFF5, new Item.Properties()));
    public static final RegistryObject<Item> HUEVO_ENANO_MADRE_OSCURO = ITEMS.register("huevo_enano_madre_oscuro",
            () -> new ForgeSpawnEggItem(ENANO_MADRE_OSCURO, 0xB4201E, 0x6E0A0E, new Item.Properties()));

    public static final RegistryObject<Item> HUEVO_CENTINELA = ITEMS.register("huevo_centinela",
            () -> new ForgeSpawnEggItem(CENTINELA, 0xDACB9C, 0xE3901D, new Item.Properties()));

    public static final RegistryObject<Item> HUEVO_GNOMO = ITEMS.register("huevo_gnomo",
            () -> new ForgeSpawnEggItem(GNOMO, 0xC4242A, 0x345CB0, new Item.Properties()));

    public static final RegistryObject<Item> HUEVO_GARGOLA = ITEMS.register("huevo_gargola",
            () -> new ForgeSpawnEggItem(GARGOLA, 0x80808A, 0xC81E1E, new Item.Properties()));

    public static final RegistryObject<Item> HUEVO_PEZ_DEL_VIENTO = ITEMS.register("huevo_pez_del_viento",
            () -> new ForgeSpawnEggItem(PEZ_DEL_VIENTO, 0xEEECE6, 0x9CC8FF, new Item.Properties()));
    public static final RegistryObject<Item> HUEVO_ZOR = ITEMS.register("huevo_zor",
            () -> new ForgeSpawnEggItem(ZOR, 0x1C1816, 0xFF5A14, new Item.Properties()));

    // ---------- SONIDOS (archivos en assets/yonga/sounds, nombres en assets/yonga/sounds.json) ----------
    public static final DeferredRegister<SoundEvent> SONIDOS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, YongaMain.MOD_ID);
    public static final RegistryObject<SoundEvent> SONIDO_ASTADO_RESPIRACION = sonido("astado_respiracion");
    public static final RegistryObject<SoundEvent> SONIDO_ASTADO_RAMA = sonido("astado_rama");
    public static final RegistryObject<SoundEvent> SONIDO_ASTADO_PASO = sonido("astado_paso");
    public static final RegistryObject<SoundEvent> SONIDO_ASTADO_GRUNIDO = sonido("astado_grunido");
    public static final RegistryObject<SoundEvent> SONIDO_ASTADO_CHILLIDO = sonido("astado_chillido");
    public static final RegistryObject<SoundEvent> SONIDO_ASTADO_CAZA = sonido("astado_caza");
    public static final RegistryObject<SoundEvent> SONIDO_ASTADO_MORDISCO = sonido("astado_mordisco");
    public static final RegistryObject<SoundEvent> SONIDO_ASTADO_GOLPE = sonido("astado_golpe");
    public static final RegistryObject<SoundEvent> SONIDO_ASTADO_HERIDO = sonido("astado_herido");
    public static final RegistryObject<SoundEvent> SONIDO_ASTADO_MUERTE = sonido("astado_muerte");
    public static final RegistryObject<SoundEvent> SONIDO_ASTADO_SUSTO = sonido("astado_susto");
    public static final RegistryObject<SoundEvent> SONIDO_LEON_RUGIDO = sonido("leon_rugido");
    public static final RegistryObject<SoundEvent> SONIDO_LEON_GRUNIDO = sonido("leon_grunido");
    public static final RegistryObject<SoundEvent> SONIDO_LEON_HERIDO = sonido("leon_herido");
    public static final RegistryObject<SoundEvent> SONIDO_LEON_MUERTE = sonido("leon_muerte");
    public static final RegistryObject<SoundEvent> SONIDO_LEON_TAJO = sonido("leon_tajo");
    public static final RegistryObject<SoundEvent> SONIDO_DUENDE_RISA = sonido("duende_risa");
    public static final RegistryObject<SoundEvent> SONIDO_DUENDE_SOPLIDO = sonido("duende_soplido");
    public static final RegistryObject<SoundEvent> SONIDO_DUENDE_PUF = sonido("duende_puf");
    public static final RegistryObject<SoundEvent> SONIDO_DUENDE_HERIDO = sonido("duende_herido");
    public static final RegistryObject<SoundEvent> SONIDO_DUENDE_MUERTE = sonido("duende_muerte");
    public static final RegistryObject<SoundEvent> SONIDO_CENTINELA_DESPERTAR = sonido("centinela_despertar");
    public static final RegistryObject<SoundEvent> SONIDO_CENTINELA_CARGA = sonido("centinela_carga");
    public static final RegistryObject<SoundEvent> SONIDO_CENTINELA_RAYO = sonido("centinela_rayo");
    public static final RegistryObject<SoundEvent> SONIDO_GNOMO_GRITO = sonido("gnomo_grito");
    public static final RegistryObject<SoundEvent> SONIDO_GNOMO_ESFUERZO = sonido("gnomo_esfuerzo");
    public static final RegistryObject<SoundEvent> SONIDO_GNOMO_HERIDO = sonido("gnomo_herido");
    public static final RegistryObject<SoundEvent> SONIDO_GNOMO_MUERTE = sonido("gnomo_muerte");
    public static final RegistryObject<SoundEvent> SONIDO_GARGOLA_CHILLIDO = sonido("gargola_chillido");
    public static final RegistryObject<SoundEvent> SONIDO_GARGOLA_GRUNIDO = sonido("gargola_grunido");
    public static final RegistryObject<SoundEvent> SONIDO_GARGOLA_MUERTE = sonido("gargola_muerte");
    public static final RegistryObject<SoundEvent> SONIDO_PEZ_CANTO = sonido("pez_canto");
    public static final RegistryObject<SoundEvent> SONIDO_PEZ_BRAMIDO = sonido("pez_bramido");
    public static final RegistryObject<SoundEvent> SONIDO_PEZ_RAYO = sonido("pez_rayo");
    public static final RegistryObject<SoundEvent> SONIDO_PEZ_GOLPE = sonido("pez_golpe");
    public static final RegistryObject<SoundEvent> SONIDO_PEZ_ILUSION = sonido("pez_ilusion");
    public static final RegistryObject<SoundEvent> SONIDO_PEZ_HERIDO = sonido("pez_herido");
    public static final RegistryObject<SoundEvent> SONIDO_PEZ_MUERTE = sonido("pez_muerte");
    public static final RegistryObject<SoundEvent> SONIDO_ZOR_RUGIDO = sonido("zor_rugido");
    public static final RegistryObject<SoundEvent> SONIDO_ZOR_FUEGO = sonido("zor_fuego");
    public static final RegistryObject<SoundEvent> SONIDO_ZOR_EXCAVAR = sonido("zor_excavar");
    public static final RegistryObject<SoundEvent> SONIDO_ZOR_EMERGER = sonido("zor_emerger");
    public static final RegistryObject<SoundEvent> SONIDO_ZOR_HERIDO = sonido("zor_herido");
    public static final RegistryObject<SoundEvent> SONIDO_ZOR_MUERTE = sonido("zor_muerte");

    private static RegistryObject<SoundEvent> sonido(String nombre) {
        return SONIDOS.register(nombre, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(YongaMain.MOD_ID, nombre)));
    }

    // "yonga:libro_hechizo": receta sin forma que solo puede usar quien tenga el rango de fe del hechizo
    public static final RegistryObject<RecipeSerializer<RecetaLibro>> RECETA_LIBRO =
            RECETAS.register("libro_hechizo", RecetaLibro.Serializer::new);

    private ModuloFe() {}

    private static RegistryObject<Item> libro(String nombre, Hechizo hechizo) {
        return ITEMS.register(nombre, () -> new LibroHechizo(hechizo,
                new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    }

    public static void registrar(IEventBus modEventBus) {
        ENTIDADES.register(modEventBus);
        ITEMS.register(modEventBus);
        RECETAS.register(modEventBus);
        SONIDOS.register(modEventBus);
        BLOQUES.register(modEventBus);
        ENTIDADES_BLOQUE.register(modEventBus);
        MinecraftForge.EVENT_BUS.addListener(AcechoAstado::alTickJugador);
        MinecraftForge.EVENT_BUS.addListener(Astado::alMontarODesmontar);
        MinecraftForge.EVENT_BUS.addListener(Gargola::alMontarODesmontar);
        MinecraftForge.EVENT_BUS.addListener(RitualPez::alColocar);
        MinecraftForge.EVENT_BUS.addListener(RitualZor::alColocar);
        MinecraftForge.EVENT_BUS.addListener(ZorElQuemado::alMontarODesmontar);
        MinecraftForge.EVENT_BUS.addListener(AcechoAstado::alDesconectarse);
        FeRed.registrar();
        modEventBus.addListener(ModuloFe::alCrearAtributos);
        modEventBus.addListener(ModuloFe::alRegistrarApariciones);

        // Maná: regeneración. Hechizos: sin daño de caída tras el Gran Salto, rayos que no dañan a quien los invoca
        MinecraftForge.EVENT_BUS.addListener(ManaDatos::alTick);
        MinecraftForge.EVENT_BUS.addListener(Hechizos::alCaer);
        MinecraftForge.EVENT_BUS.addListener(Hechizos::alCaerRayo);
        MinecraftForge.EVENT_BUS.addListener(Hechizos::alSalir);

        // Orbes de fe al matar mobs
        MinecraftForge.EVENT_BUS.addListener(OrbesFe::alMorir);
        MinecraftForge.EVENT_BUS.addListener(OrbesFe::alAparecer);
        MinecraftForge.EVENT_BUS.addListener(OrbesFe::alSalir);

        MinecraftForge.EVENT_BUS.addListener(ModuloFe::alRegistrarComandos);

        // Mandamos la fe al cliente cuando entra, reaparece o cambia de dimensión
        MinecraftForge.EVENT_BUS.addListener(ModuloFe::alEntrar);
        MinecraftForge.EVENT_BUS.addListener(ModuloFe::alReaparecer);
        MinecraftForge.EVENT_BUS.addListener(ModuloFe::alCambiarDimension);

        // Rezar / blasfemar
        MinecraftForge.EVENT_BUS.addListener(Oracion::alTick);
        MinecraftForge.EVENT_BUS.addListener(Oracion::alRecibirDano);
        MinecraftForge.EVENT_BUS.addListener(Oracion::alSalir);
        MinecraftForge.EVENT_BUS.addListener(Oracion::alCambiarDimension);
        MinecraftForge.EVENT_BUS.addListener(Oracion::alEmpezarAVer);
    }

    private static void alCrearAtributos(EntityAttributeCreationEvent evento) {
        evento.put(HADA.get(), Hada.atributos().build());
        evento.put(HADA_OSCURA.get(), Hada.atributos().build());
        evento.put(ENANO.get(), Enano.atributos().build());
        evento.put(ENANO_OSCURO.get(), Enano.atributos().build());
        evento.put(ASTADO.get(), Astado.atributos().build());
        evento.put(LEON_ARQUERO.get(), LeonDeGuerra.atributosArquero().build());
        evento.put(LEON_ESPADACHIN.get(), LeonDeGuerra.atributosEspadachin().build());
        evento.put(DUENDE.get(), Duende.atributos().build());
        evento.put(CENTINELA.get(), Centinela.atributos().build());
        evento.put(GNOMO.get(), Gnomo.atributos().build());
        evento.put(GARGOLA.get(), Gargola.atributos().build());
        evento.put(PEZ_DEL_VIENTO.get(), PezDelViento.atributos().build());
        evento.put(PEZ_ILUSION.get(), PezDelViento.atributos().build());
        evento.put(ZOR.get(), ZorElQuemado.atributos().build());
        evento.put(ENANO_MADRE.get(), EnanoMadre.atributosMadre().build());
        evento.put(ENANO_MADRE_OSCURO.get(), EnanoMadre.atributosMadre().build());
    }

    private static void alRegistrarApariciones(SpawnPlacementRegisterEvent evento) {
        for (RegistryObject<EntityType<Hada>> hada : java.util.List.of(HADA, HADA_OSCURA)) {
            evento.register(hada.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Hada::puedeAparecer, SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
        for (RegistryObject<EntityType<LeonDeGuerra>> leon : java.util.List.of(LEON_ARQUERO, LEON_ESPADACHIN)) {
            evento.register(leon.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    LeonDeGuerra::puedeAparecer, SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
        for (RegistryObject<EntityType<EnanoMadre>> madre : java.util.List.of(ENANO_MADRE, ENANO_MADRE_OSCURO)) {
            evento.register(madre.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    EnanoMadre::puedeAparecerMadre, SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
        evento.register(GARGOLA.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Gargola::puedeAparecer, SpawnPlacementRegisterEvent.Operation.REPLACE);
        evento.register(GNOMO.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Gnomo::puedeAparecer, SpawnPlacementRegisterEvent.Operation.REPLACE);
        evento.register(DUENDE.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Duende::puedeAparecer, SpawnPlacementRegisterEvent.Operation.REPLACE);
        for (RegistryObject<EntityType<Enano>> enano : java.util.List.of(ENANO, ENANO_OSCURO)) {
            evento.register(enano.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Enano::puedeAparecer, SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
    }

    private static void alRegistrarComandos(RegisterCommandsEvent evento) {
        FeComando.registrar(evento.getDispatcher());
    }

    private static void alEntrar(PlayerEvent.PlayerLoggedInEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador) {
            ManaDatos.asegurarIniciado(jugador);
            FeDatos.ajustarAlLimite(jugador);   // por si ya no puede estar en el nivel 101 (también sincroniza)
        }
    }

    private static void alReaparecer(PlayerEvent.PlayerRespawnEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador) {
            FeDatos.sincronizar(jugador);
        }
    }

    private static void alCambiarDimension(PlayerEvent.PlayerChangedDimensionEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador) {
            FeDatos.sincronizar(jugador);
        }
    }
}
