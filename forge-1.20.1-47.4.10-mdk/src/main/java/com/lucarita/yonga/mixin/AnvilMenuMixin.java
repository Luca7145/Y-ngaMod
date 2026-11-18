package com.lucarita.yonga.mixin;

import com.lucarita.yonga.modulos.roles.oficios.Herrero;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Yunque: cuando Minecraft calcula cuántos niveles cuesta, si quien lo usa es Herrero le hacemos el
 * descuento de su rango (ver Herrero.precioYunque). Solo en el servidor: el precio le llega a la pantalla
 * del jugador solo, y es el que se cobra.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {

    @Shadow @Final private DataSlot cost;

    // Mixin necesita este constructor para compilar, pero nunca se usa
    protected AnvilMenuMixin(MenuType<?> tipo, int id, Inventory inventario, ContainerLevelAccess acceso) {
        super(tipo, id, inventario, acceso);
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void yonga$descuentoHerrero(CallbackInfo ci) {
        if (!this.player.level().isClientSide()) {
            this.cost.set(Herrero.precioYunque(this.player, this.cost.get()));
        }
    }
}
