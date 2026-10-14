package com.lucarita.yonga.core;

import com.lucarita.yonga.YongaMain;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    // Creamos el registro para las pestañas creativas
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, YongaMain.MOD_ID);

    // Creamos nuestra pestaña personalizada
    public static final RegistryObject<CreativeModeTab> YONGA_TAB = CREATIVE_MODE_TABS.register("yonga_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.MONEDA.get())) // El icono de la pestaña será tu moneda
                    .title(Component.translatable("creativetab.yonga_tab")) // El nombre (lo traduciremos en el JSON)
                    .displayItems((parametros, output) -> {
                        // AQUÍ AÑADIMOS LOS ÍTEMS A LA PESTAÑA
                        output.accept(ModItems.MONEDA.get());
                        output.accept(ModItems.LUCARITA.get());
                        output.accept(ModItems.RAW_LUCARITA.get());
                        output.accept(ModItems.LUCARITA_ORE_ITEM.get());
                        output.accept(ModItems.FALCATA.get());
                        output.accept(ModItems.ALABARDA.get());
                        output.accept(ModItems.GLADIO.get());
                        output.accept(ModItems.MAZA.get());
                        output.accept(ModItems.PUNAL.get());
                        output.accept(ModItems.ESPADA_CABALLERESCA.get());
                        output.accept(ModItems.ROPERA.get());

                        // Cuando crees más ítems en el futuro, solo tendrás que añadir otra línea como esta:
                        // output.accept(ModItems.NUEVO_ITEM.get());
                    })
                    .build());

    // Método para conectar con el YongaMain
    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}