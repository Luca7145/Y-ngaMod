package com.lucarita.yonga.core;

import com.lucarita.yonga.YongaMain;
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

    // --- ARMAS NORMALES (Usan SwordItem por defecto) ---
    // (Material, Daño Extra, Modificador de Velocidad, Propiedades)

    public static final RegistryObject<Item> ALABARDA = ITEMS.register("alabarda",
            () -> new net.minecraft.world.item.SwordItem(net.minecraft.world.item.Tiers.IRON, 5, -3.0f, new Item.Properties()));

    public static final RegistryObject<Item> FALCATA = ITEMS.register("falcata",
            () -> new net.minecraft.world.item.SwordItem(net.minecraft.world.item.Tiers.IRON, 3, -2.4f, new Item.Properties()));

    public static final RegistryObject<Item> GLADIO = ITEMS.register("gladio",
            () -> new net.minecraft.world.item.SwordItem(net.minecraft.world.item.Tiers.IRON, 3, -2.0f, new Item.Properties()));

    public static final RegistryObject<Item> MAZA = ITEMS.register("maza",
            () -> new net.minecraft.world.item.SwordItem(net.minecraft.world.item.Tiers.IRON, 6, -3.1f, new Item.Properties()));

    public static final RegistryObject<Item> PUNAL = ITEMS.register("punal",
            () -> new net.minecraft.world.item.SwordItem(net.minecraft.world.item.Tiers.IRON, 2, -1.2f, new Item.Properties()));

    // --- ARMAS CON PARRY (Usan nuestra nueva clase ArmaParryItem) ---

    public static final RegistryObject<Item> ESPADA_CABALLERESCA = ITEMS.register("espada_caballeresca",
            () -> new ArmaParryItem(net.minecraft.world.item.Tiers.IRON, 4, -2.4f, new Item.Properties()));

    public static final RegistryObject<Item> ROPERA = ITEMS.register("ropera",
            () -> new ArmaParryItem(net.minecraft.world.item.Tiers.IRON, 3, -1.8f, new Item.Properties()));

    // Este método le dice a Forge que cargue nuestra lista al arrancar
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}