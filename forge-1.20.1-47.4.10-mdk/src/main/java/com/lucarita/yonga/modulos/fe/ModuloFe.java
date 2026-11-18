package com.lucarita.yonga.modulos.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.hechizos.Hechizo;
import com.lucarita.yonga.modulos.fe.hechizos.Hechizos;
import com.lucarita.yonga.modulos.fe.hechizos.LibroHechizo;
import com.lucarita.yonga.modulos.fe.hechizos.RecetaLibro;
import com.lucarita.yonga.modulos.fe.red.FeRed;
import com.lucarita.yonga.modulos.fe.mobs.Enano;
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
    }

    private static void alRegistrarApariciones(SpawnPlacementRegisterEvent evento) {
        for (RegistryObject<EntityType<Hada>> hada : java.util.List.of(HADA, HADA_OSCURA)) {
            evento.register(hada.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Hada::puedeAparecer, SpawnPlacementRegisterEvent.Operation.REPLACE);
        }
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
