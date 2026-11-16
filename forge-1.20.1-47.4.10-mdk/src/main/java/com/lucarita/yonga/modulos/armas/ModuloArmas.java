package com.lucarita.yonga.modulos.armas;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.core.ModTiers;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Módulo de armas: todas las armas de lucarita y la mecánica de parry.
 * Solo se registra si el módulo está activado (ver YongaMain).
 */
public final class ModuloArmas {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, YongaMain.MOD_ID);

    // (Material, Daño extra, Velocidad de ataque, ¿parry?, ¿sangrado?, Propiedades)

    public static final RegistryObject<Item> FALCATA = ITEMS.register("falcata",
            () -> new ArmaYonga(ModTiers.LUCARITA, 4, -2.6f, false, true, new Item.Properties()));

    public static final RegistryObject<Item> ALABARDA = ITEMS.register("alabarda",
            () -> new ArmaYonga(ModTiers.LUCARITA, 7, -3.3f, false, true, new Item.Properties()));

    public static final RegistryObject<Item> GLADIO = ITEMS.register("gladio",
            () -> new ArmaYonga(ModTiers.LUCARITA, 3, -2.1f, false, false, new Item.Properties()));

    public static final RegistryObject<Item> MAZA = ITEMS.register("maza",
            () -> new ArmaYonga(ModTiers.LUCARITA, 5, -3.1f, false, false, new Item.Properties()));

    public static final RegistryObject<Item> PUNAL = ITEMS.register("punal",
            () -> new ArmaYonga(ModTiers.LUCARITA, 1, -1.5f, false, true, new Item.Properties()));

    public static final RegistryObject<Item> ESPADA_CABALLERESCA = ITEMS.register("espada_caballeresca",
            () -> new ArmaYonga(ModTiers.LUCARITA, 4, -2.4f, true, false, new Item.Properties()));

    public static final RegistryObject<Item> ROPERA = ITEMS.register("ropera",
            () -> new ArmaYonga(ModTiers.LUCARITA, 2, -2.0f, true, false, new Item.Properties()));

    private ModuloArmas() {}

    public static void registrar(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        // Un solo escuchador de parry para todas las armas
        MinecraftForge.EVENT_BUS.addListener(ArmaYonga::alSerAtacado);
    }
}
