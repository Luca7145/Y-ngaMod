package com.lucarita.yonga.core;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.armas.ModuloArmas;
import com.lucarita.yonga.modulos.fe.ModuloFe;
import com.lucarita.yonga.modulos.roles.ModuloRoles;
import com.lucarita.yonga.modulos.sangrado.ModuloSangrado;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, YongaMain.MOD_ID);

    public static final RegistryObject<CreativeModeTab> YONGA_TAB = CREATIVE_MODE_TABS.register("yonga_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.MONEDA.get()))
                    .title(Component.translatable("creativetab.yonga_tab"))
                    .displayItems((parametros, output) -> {
                        // Núcleo (siempre)
                        ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));

                        // Cada módulo solo añade sus cosas si está activo.
                        // Las armas se añaden solas: cualquier arma nueva que registres en ModuloArmas aparece aquí.
                        if (Modulo.ARMAS.activo()) {
                            ModuloArmas.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        }

                        if (Modulo.ROLES.activo()) {
                            ModuloRoles.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        }

                        if (Modulo.FE.activo()) {
                            ModuloFe.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        }

                        if (Modulo.SANGRADO.activo()) {
                            ModuloSangrado.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                            output.accept(EnchantedBookItem.createForEnchantment(
                                    new EnchantmentInstance(ModuloSangrado.CORTANTE.get(), 1)));
                        }
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
