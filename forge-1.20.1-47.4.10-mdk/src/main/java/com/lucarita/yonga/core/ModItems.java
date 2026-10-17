package com.lucarita.yonga.core;

import com.lucarita.yonga.YongaMain;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    // Creamos la lista (Registro) donde guardaremos todos nuestros ítems
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, YongaMain.MOD_ID);

    // --- AQUÍ REGISTRAMOS LA MONEDA ---
    public static final RegistryObject<Item> MONEDA = ITEMS.register("moneda",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LUCARITA = ITEMS.register("lucarita",
            () -> new Item(new Item.Properties()));

    // El material crudo
    public static final RegistryObject<Item> RAW_LUCARITA = ITEMS.register("raw_lucarita",
            () -> new Item(new Item.Properties()));

    // El ítem que representa al bloque en tu inventario
    public static final RegistryObject<Item> LUCARITA_ORE_ITEM = ITEMS.register("lucarita_ore",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.LUCARITA_ORE.get(), new Item.Properties()));

    // --- ARMAS NORMALES (Usan SwordItem por defecto) ---
    // (Material, Daño Extra, Modificador de Velocidad, Propiedades)

    public static final RegistryObject<Item> ALABARDA = ITEMS.register("alabarda",
            () -> new ArmaSangradoItem(ModTiers.LUCARITA, 7, -3.3f, new Item.Properties()));

    public static final RegistryObject<Item> FALCATA = ITEMS.register("falcata",
            () -> new ArmaSangradoItem(ModTiers.LUCARITA, 4, -2.6f, new Item.Properties()));

    public static final RegistryObject<Item> GLADIO = ITEMS.register("gladio",
            () -> new net.minecraft.world.item.SwordItem(ModTiers.LUCARITA, 3, -2.1f, new Item.Properties()));

    public static final RegistryObject<Item> MAZA = ITEMS.register("maza",
            () -> new net.minecraft.world.item.SwordItem(ModTiers.LUCARITA, 5, -3.1f, new Item.Properties()));

    public static final RegistryObject<Item> PUNAL = ITEMS.register("punal",
            () -> new ArmaSangradoItem(ModTiers.LUCARITA, 1, -1.5f, new Item.Properties()));

    // --- ARMAS CON PARRY (Usan nuestra nueva clase ArmaParryItem) ---

    public static final RegistryObject<Item> ESPADA_CABALLERESCA = ITEMS.register("espada_caballeresca",
            () -> new ArmaParryItem(ModTiers.LUCARITA, 4, -2.4f, new Item.Properties()));

    public static final RegistryObject<Item> ROPERA = ITEMS.register("ropera",
            () -> new ArmaParryItem(ModTiers.LUCARITA, 2, -2.0f, new Item.Properties()));

    public static final RegistryObject<Item> VENDAS = ITEMS.register("vendas",
            () -> new VendaItem(new Item.Properties()
                    .food(new FoodProperties.Builder().alwaysEat().build())));

    // Este método le dice a Forge que cargue nuestra lista al arrancar
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}