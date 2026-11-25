package com.lucarita.yonga.modulos.habitantes;

import com.lucarita.yonga.YongaMain;
import net.minecraft.world.entity.EntityType;
import com.lucarita.yonga.modulos.habitantes.red.HabitantesRed;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * MÓDULO HABITANTES: personas que sustituyen a los aldeanos (ver el documento de diseño "Yonga: los habitantes").
 *
 * Fase 5: amor y familia (familia/): parejas, bodas, hijos, adopción, celos; el anillo; /yonga sexo.
 * Fase 4: la aldea (aldea/): líder, guardias, fama, zombis e incursiones, sin comerciante errante ni gólems.
 * Fase 3: conversación con IA local (ia/).
 * Fase 2: oficios con nivel, puesto de trabajo, barra de trabajo y tienda (ver Oficio, Tienda, TiendaMenu).
 * Fase 1: el cuerpo. Entidad con época, sexo, 4 edades (bebé, niño, joven, adulto) que crecen en días reales,
 * medidas propias, aspecto por capas de textura (época medieval de prueba) y la sustitución de los aldeanos.
 */
public final class ModuloHabitantes {

    private ModuloHabitantes() {}

    public static final DeferredRegister<EntityType<?>> ENTIDADES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, YongaMain.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, YongaMain.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, YongaMain.MOD_ID);
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOQUES =
            DeferredRegister.create(ForgeRegistries.BLOCKS, YongaMain.MOD_ID);

    /** Fase 6: el tablón de encargos de la taberna. */
    public static final RegistryObject<net.minecraft.world.level.block.Block> TABLON = BLOQUES.register("tablon_encargos",
            () -> new com.lucarita.yonga.modulos.habitantes.taberna.TablonBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                            .mapColor(net.minecraft.world.level.material.MapColor.WOOD)
                            .strength(1.0f).sound(net.minecraft.world.level.block.SoundType.WOOD)
                            .noOcclusion().ignitedByLava()));
    public static final RegistryObject<Item> TABLON_ITEM = ITEMS.register("tablon_encargos",
            () -> new net.minecraft.world.item.BlockItem(TABLON.get(), new Item.Properties()));

    /** Tamaño de un adulto de altura media; cada habitante lo escala con getScale(). */
    public static final RegistryObject<EntityType<Habitante>> HABITANTE = ENTIDADES.register("habitante",
            () -> EntityType.Builder.<Habitante>of(Habitante::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f)
                    .clientTrackingRange(10)
                    .build("habitante"));

    // Huevos para pruebas: uno siempre saca hombres y otro siempre mujeres
    public static final RegistryObject<Item> HUEVO_HOMBRE = ITEMS.register("huevo_habitante_hombre",
            () -> new HuevoHabitanteItem(false, 0x8A6A48, 0x4E78B4, new Item.Properties()));
    public static final RegistryObject<Item> HUEVO_MUJER = ITEMS.register("huevo_habitante_mujer",
            () -> new HuevoHabitanteItem(true, 0x8A6A48, 0xC85A78, new Item.Properties()));

    /** Fase 5: el anillo de compromiso. Clic derecho a tu pareja con él en la mano para pedirle matrimonio. */
    public static final RegistryObject<Item> ANILLO = ITEMS.register("anillo",
            () -> new Item(new Item.Properties().stacksTo(16)));

    /** Solo creativo: clic derecho a un habitante para ver y cambiar todo lo suyo (aspecto, carácter, oficio, relación…). */
    public static final RegistryObject<Item> LIBRO_CREADOR = ITEMS.register("libro_creador",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

    /** Solo creativo: clic derecho y te cuenta todo de la aldea en la que estás (vecinos, familias, rumores…). */
    public static final RegistryObject<Item> LIBRO_ALDEA = ITEMS.register("libro_aldea",
            () -> new com.lucarita.yonga.modulos.habitantes.cronica.LibroAldeaItem(
                    new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

    public static final RegistryObject<MenuType<TiendaMenu>> MENU_TIENDA = MENUS.register("tienda_habitante",
            () -> IForgeMenuType.create(TiendaMenu::desdeRed));

    public static void registrar(IEventBus bus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, HabitantesConfig.SPEC, "yonga-habitantes.toml");
        com.lucarita.yonga.modulos.habitantes.muebles.Muebles.registrar(bus);   // muebles y adornos de las casas
        com.lucarita.yonga.modulos.habitantes.compat.Compat.registrar(bus);     // fase 9: Create, Carry On, bebés en brazos
        ENTIDADES.register(bus);
        BLOQUES.register(bus);
        ITEMS.register(bus);
        MENUS.register(bus);
        HabitantesRed.registrar();
        bus.addListener(ModuloHabitantes::alCrearAtributos);
        MinecraftForge.EVENT_BUS.addListener(SustitucionAldeanos::alEntrar);
        MinecraftForge.EVENT_BUS.addListener(SustitucionAldeanos::alTickNivel);
        MinecraftForge.EVENT_BUS.addListener(SustitucionAldeanos::alDescargarNivel);
        // Fase 4: zombis e incursiones contra los habitantes, guardias en lugar de gólems, fama, sin comerciante errante
        MinecraftForge.EVENT_BUS.addListener(com.lucarita.yonga.modulos.habitantes.aldea.Sociedad::alEntrar);
        MinecraftForge.EVENT_BUS.addListener(com.lucarita.yonga.modulos.habitantes.aldea.Sociedad::alHerir);
        MinecraftForge.EVENT_BUS.addListener(com.lucarita.yonga.modulos.habitantes.aldea.Sociedad::alMorir);
        MinecraftForge.EVENT_BUS.addListener(com.lucarita.yonga.modulos.habitantes.ia.Conversaciones::alArrancarServidor);
        // Fase 5: amor y familia
        MinecraftForge.EVENT_BUS.addListener(com.lucarita.yonga.modulos.habitantes.familia.Amor::alTickJugador);
        MinecraftForge.EVENT_BUS.addListener(com.lucarita.yonga.modulos.habitantes.familia.Amor::alRegistrarComandos);
        MinecraftForge.EVENT_BUS.addListener(com.lucarita.yonga.modulos.habitantes.familia.Encuentro::alTick);
        // Fase 6: taberna y encargos
        MinecraftForge.EVENT_BUS.addListener(com.lucarita.yonga.modulos.habitantes.taberna.Taberna::alMorir);
        MinecraftForge.EVENT_BUS.addListener(com.lucarita.yonga.modulos.habitantes.familia.Encuentro::alClicCama);
        MinecraftForge.EVENT_BUS.addListener(com.lucarita.yonga.modulos.habitantes.familia.Encuentro::alInteractuarJugador);
        MinecraftForge.EVENT_BUS.addListener(com.lucarita.yonga.modulos.habitantes.familia.Encuentro::alComprobarHora);
    }

    private static void alCrearAtributos(EntityAttributeCreationEvent evento) {
        evento.put(HABITANTE.get(), Habitante.atributos().build());
    }
}
