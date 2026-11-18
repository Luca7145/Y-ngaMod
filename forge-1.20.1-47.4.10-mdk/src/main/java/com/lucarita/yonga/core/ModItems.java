package com.lucarita.yonga.core;

import com.lucarita.yonga.YongaMain;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;
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

    // Solo sirve de icono para la pestaña creativa (no aparece dentro de ella)
    public static final RegistryObject<Item> ICONO = ITEMS.register("icono_yonga",
            () -> new Item(new Item.Properties()));

    // El lingote de lucarita también es combustible: dura lo que 8 cubos de lava (funde 800 objetos)
    public static final int COMBUSTIBLE_LUCARITA = 8 * 20000;   // en ticks (un cubo de lava = 20000)

    public static final RegistryObject<Item> LUCARITA = ITEMS.register("lucarita",
            () -> new Item(new Item.Properties()) {
                @Override
                public int getBurnTime(ItemStack objeto, @Nullable RecipeType<?> tipoReceta) {
                    return COMBUSTIBLE_LUCARITA;
                }
            });

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
