package com.lucarita.yonga.modulos.tabaco;

import com.lucarita.yonga.YongaMain;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * MÓDULO TABACO: la planta de tabaco y lo que se hace con ella, más o menos como en la vida real.
 *
 *   semillas (de la hierba, a veces) → PLANTA DE TABACO (cultivo) → hojas de tabaco frescas
 *   hoja fresca → CURAR al calor (horno, ahumador o fogata) → hoja de tabaco curada
 *     · 3 hojas curadas enrolladas (en columna) → PURO
 *     · hoja curada → 2 de tabaco picado;  tabaco picado + papel → CIGARRO
 *   3 papeles + tinte rojo → CAJETILLA (guarda hasta 10 cigarros sin estrenar)
 *
 * Se encienden con el mechero en la otra mano. Ver FumableItem (estados, humo, tirarlo encendido) y Tabaco (el
 * boost, el wither y las náuseas, y la nicotina que se acumula).
 */
public final class ModuloTabaco {

    private ModuloTabaco() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, YongaMain.MOD_ID);
    public static final DeferredRegister<Block> BLOQUES = DeferredRegister.create(ForgeRegistries.BLOCKS, YongaMain.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, YongaMain.MOD_ID);

    // ---------- EQUILIBRIO ENTRE CIGARRO Y PURO ----------
    //                                          nombre, dura (ticks), cada calada, nicotina, boost, wither, náuseas
    /** Cigarro: barato (media hoja y un papel), 3 minutos, ~12 caladas; boost de 45 s de velocidad y prisa. */
    public static final FumableItem.Tipo CIGARRO_TIPO = new FumableItem.Tipo("cigarro", 3600, 300, 1.0f / 12, 900, 160, 240);
    /** Puro: caro (3 hojas), 9 minutos, ~22 caladas; boost de 90 s de fuerza, resistencia y velocidad; engancha más. */
    public static final FumableItem.Tipo PURO_TIPO = new FumableItem.Tipo("puro", 10800, 480, 2.5f / 22, 1800, 220, 300);
    // Los dos, además, dan +1,5 bloques de alcance mientras dura el boost (ver Tabaco).
    // Nicotina: un cigarro entero suma 1 punto (1/12 por calada); un puro entero, 2,5 (engancha más).

    // ---------- LA PLANTA ----------
    public static final RegistryObject<Block> PLANTA_TABACO = BLOQUES.register("planta_tabaco",
            () -> new PlantaTabacoBlock(BlockBehaviour.Properties.copy(Blocks.WHEAT)));

    // ---------- OBJETOS ----------
    public static final RegistryObject<Item> SEMILLAS = ITEMS.register("semillas_tabaco",
            () -> new ItemNameBlockItem(PLANTA_TABACO.get(), new Item.Properties()));
    public static final RegistryObject<Item> HOJA = ITEMS.register("hoja_tabaco", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HOJA_CURADA = ITEMS.register("hoja_tabaco_curada", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TABACO_PICADO = ITEMS.register("tabaco_picado", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CIGARRO = ITEMS.register("cigarro", () -> new FumableItem(CIGARRO_TIPO, new Item.Properties()));
    public static final RegistryObject<Item> PURO = ITEMS.register("puro", () -> new FumableItem(PURO_TIPO, new Item.Properties()));
    public static final RegistryObject<Item> CAJETILLA = ITEMS.register("cajetilla", () -> new CajetillaItem(new Item.Properties()));

    // ---------- MENÚ DE LA CAJETILLA ----------
    public static final RegistryObject<MenuType<CajetillaMenu>> MENU_CAJETILLA = MENUS.register("cajetilla",
            () -> IForgeMenuType.create(CajetillaMenu::desdeRed));

    public static void registrar(IEventBus bus) {
        ITEMS.register(bus);
        BLOQUES.register(bus);
        MENUS.register(bus);
        bus.addListener(ModuloTabaco::alPreparar);
        MinecraftForge.EVENT_BUS.addListener(Tabaco::alTick);
        MinecraftForge.EVENT_BUS.addListener(Tabaco::alClicBloque);
        MinecraftForge.EVENT_BUS.addListener(Tabaco::alClonar);
    }

    /** Las semillas y las hojas se pueden echar al compostador. */
    private static void alPreparar(FMLCommonSetupEvent evento) {
        evento.enqueueWork(() -> {
            ComposterBlock.COMPOSTABLES.put(SEMILLAS.get(), 0.3f);
            ComposterBlock.COMPOSTABLES.put(HOJA.get(), 0.65f);
        });
    }
}
