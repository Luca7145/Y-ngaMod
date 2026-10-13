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

    // Este método le dice a Forge que cargue nuestra lista al arrancar
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}