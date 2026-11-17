package com.lucarita.yonga.modulos.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.hechizos.Hechizo;
import com.lucarita.yonga.modulos.fe.hechizos.Hechizos;
import com.lucarita.yonga.modulos.fe.hechizos.LibroHechizo;
import com.lucarita.yonga.modulos.fe.hechizos.RecetaLibro;
import com.lucarita.yonga.modulos.fe.red.FeRed;
import net.minecraft.world.entity.EntityType;
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

    // ---------- LIBROS DE HECHIZOS ----------

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, YongaMain.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECETAS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, YongaMain.MOD_ID);

    public static final RegistryObject<Item> LIBRO_GRAN_SALTO = libro("libro_gran_salto", Hechizo.GRAN_SALTO);
    public static final RegistryObject<Item> LIBRO_CASTIGO_DEL_CIELO = libro("libro_castigo_del_cielo", Hechizo.CASTIGO_DEL_CIELO);
    public static final RegistryObject<Item> LIBRO_EXPULSION = libro("libro_expulsion", Hechizo.EXPULSION);
    public static final RegistryObject<Item> LIBRO_PROYECTIL_OSCURO = libro("libro_proyectil_oscuro", Hechizo.PROYECTIL_OSCURO);

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
