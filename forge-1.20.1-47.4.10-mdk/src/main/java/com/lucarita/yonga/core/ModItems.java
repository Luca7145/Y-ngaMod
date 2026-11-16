package com.lucarita.yonga.core;

import com.lucarita.yonga.YongaMain;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Ítems del núcleo: siempre están, actives los módulos que actives.
 * Los ítems de cada módulo están en su propio paquete (modulos/armas, modulos/sangrado...).
 */
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, YongaMain.MOD_ID);

    public static final RegistryObject<Item> MONEDA = ITEMS.register("moneda",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LUCARITA = ITEMS.register("lucarita",
            () -> new Item(new Item.Properties()));

    // El material crudo
    public static final RegistryObject<Item> RAW_LUCARITA = ITEMS.register("raw_lucarita",
            () -> new Item(new Item.Properties()));

    // El ítem que representa al bloque en tu inventario
    public static final RegistryObject<Item> LUCARITA_ORE_ITEM = ITEMS.register("lucarita_ore",
            () -> new BlockItem(ModBlocks.LUCARITA_ORE.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
