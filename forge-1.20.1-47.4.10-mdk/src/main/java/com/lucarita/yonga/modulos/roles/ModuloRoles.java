package com.lucarita.yonga.modulos.roles;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.roles.cara.Cara;
import com.lucarita.yonga.modulos.roles.cara.Medallon;
import com.lucarita.yonga.modulos.roles.cara.MedallonItem;
import com.lucarita.yonga.modulos.roles.oficios.BajoHachaItem;
import com.lucarita.yonga.modulos.roles.oficios.Explorador;
import com.lucarita.yonga.modulos.roles.oficios.ParapenteItem;
import com.lucarita.yonga.modulos.roles.oficios.mochila.MochilaItem;
import com.lucarita.yonga.modulos.roles.oficios.mochila.MochilaMenu;
import net.minecraft.world.inventory.MenuType;
import com.lucarita.yonga.modulos.roles.red.RolesRed;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Módulo de roles (oficios): Herrero, Explorador, Sacerdote, Ingeniero, Melómano...
 * Cada jugador tiene un oficio y un rango dentro de él (Novato -> Maestro).
 * Solo se registra si el módulo está activado (ver YongaMain).
 * La parte visual (menú de oficios, botón del inventario) está en el paquete client/roles.
 */
public final class ModuloRoles {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, YongaMain.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECETAS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, YongaMain.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, YongaMain.MOD_ID);
    public static final DeferredRegister<SoundEvent> SONIDOS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, YongaMain.MOD_ID);

    // ---------- OBJETOS DE OFICIO ----------

    // Explorador (se fabrica desde Oficial, ver data/yonga/recipes/parapente.json)
    public static final RegistryObject<Item> PARAPENTE = ITEMS.register("parapente",
            () -> new ParapenteItem(new Item.Properties()));

    // Explorador: mochila (Aprendiz) y mochila mejorada (Experto). (huecos, máximo por hueco)
    public static final RegistryObject<Item> MOCHILA = ITEMS.register("mochila",
            () -> new MochilaItem(16, 100, new Item.Properties()));
    public static final RegistryObject<Item> MOCHILA_MEJORADA = ITEMS.register("mochila_mejorada",
            () -> new MochilaItem(26, 200, new Item.Properties()));

    // Explorador: antifaz (Novato). Va en el hueco de cara; los mobs te detectan a la mitad de distancia.
    public static final RegistryObject<Item> ANTIFAZ = ITEMS.register("antifaz",
            () -> new Item(new Item.Properties().stacksTo(1)));

    // Explorador: medallones, se llevan en el hueco de cara y se ven en el pecho
    public static final RegistryObject<Item> MEDALLON_AGUA = ITEMS.register("medallon_agua",
            () -> new MedallonItem(Medallon.AGUA, new Item.Properties()));       // Novato
    public static final RegistryObject<Item> MEDALLON_TIERRA = ITEMS.register("medallon_tierra",
            () -> new MedallonItem(Medallon.TIERRA, new Item.Properties()));     // Aprendiz
    public static final RegistryObject<Item> MEDALLON_FUEGO = ITEMS.register("medallon_fuego",
            () -> new MedallonItem(Medallon.FUEGO, new Item.Properties()));      // Oficial
    public static final RegistryObject<Item> MEDALLON_AIRE = ITEMS.register("medallon_aire",
            () -> new MedallonItem(Medallon.AIRE, new Item.Properties()));       // Experto

    // Melómano: bajo-hacha
    public static final RegistryObject<Item> BAJO_HACHA = ITEMS.register("bajo_hacha",
            () -> new BajoHachaItem(new Item.Properties()));

    // ---------- SONIDOS (archivos en assets/yonga/sounds, nombres en assets/yonga/sounds.json) ----------

    public static final RegistryObject<SoundEvent> SONIDO_BAJO_NOTA = sonido("bajo_nota");
    public static final RegistryObject<SoundEvent> SONIDO_BAJO_ACORDE = sonido("bajo_acorde");

    private static RegistryObject<SoundEvent> sonido(String nombre) {
        return SONIDOS.register(nombre, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(YongaMain.MOD_ID, nombre)));
    }

    // ---------- MENÚS ----------

    public static final RegistryObject<MenuType<MochilaMenu>> MENU_MOCHILA = MENUS.register("mochila",
            () -> IForgeMenuType.create(MochilaMenu::desdeRed));

    // ---------- RECETAS ----------

    // "yonga:receta_oficio": receta con forma que solo puede usar quien tenga cierto oficio y rango
    public static final RegistryObject<RecipeSerializer<RecetaOficio>> RECETA_OFICIO =
            RECETAS.register("receta_oficio", RecetaOficio.Serializer::new);

    private ModuloRoles() {}

    public static void registrar(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        RECETAS.register(modEventBus);
        MENUS.register(modEventBus);
        SONIDOS.register(modEventBus);
        RolesRed.registrar();

        MinecraftForge.EVENT_BUS.addListener(ModuloRoles::alRegistrarComandos);

        // Hueco de cara (antifaz)
        MinecraftForge.EVENT_BUS.addListener(Cara::alCalcularVisibilidad);
        MinecraftForge.EVENT_BUS.addListener(Cara::alClonar);
        MinecraftForge.EVENT_BUS.addListener(Cara::alTick);
        MinecraftForge.EVENT_BUS.addListener(Cara::alEmpezarAVer);
        MinecraftForge.EVENT_BUS.addListener(Cara::alSalir);

        // Habilidades de oficio
        MinecraftForge.EVENT_BUS.addListener(Explorador::alRecibirDano);
        MinecraftForge.EVENT_BUS.addListener(BajoHachaItem::alSalir);
        MinecraftForge.EVENT_BUS.addListener(ModuloRoles::alEntrar);
        MinecraftForge.EVENT_BUS.addListener(ModuloRoles::alReaparecer);
        MinecraftForge.EVENT_BUS.addListener(ModuloRoles::alCambiarDimension);
    }

    private static void alRegistrarComandos(RegisterCommandsEvent evento) {
        RolComando.registrar(evento.getDispatcher());
    }

    // Al entrar: si aún no tiene oficio, le sale el menú para elegirlo
    private static void alEntrar(PlayerEvent.PlayerLoggedInEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador) {
            RolDatos.sincronizar(jugador, !RolDatos.tieneRol(jugador));
        }
    }

    private static void alReaparecer(PlayerEvent.PlayerRespawnEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador) {
            RolDatos.sincronizar(jugador, false);
        }
    }

    private static void alCambiarDimension(PlayerEvent.PlayerChangedDimensionEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador) {
            RolDatos.sincronizar(jugador, false);
        }
    }
}
