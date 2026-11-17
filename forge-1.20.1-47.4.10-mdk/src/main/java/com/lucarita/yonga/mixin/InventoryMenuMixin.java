package com.lucarita.yonga.mixin;

import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.roles.cara.AccesoCara;
import com.lucarita.yonga.modulos.roles.cara.HuecoCara;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Añade el hueco de cara al inventario del jugador (al final, detrás del hueco del escudo).
 * Solo si el módulo de roles está activado.
 */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {

    // Mixin necesita este constructor para compilar, pero nunca se usa
    protected InventoryMenuMixin(MenuType<?> tipo, int id) {
        super(tipo, id);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void yonga$anadirHuecoCara(Inventory inventario, boolean activo, Player dueno, CallbackInfo ci) {
        if (Modulo.ROLES.activo()) {
            this.addSlot(new HuecoCara(((AccesoCara) dueno).yonga$cara()));
        }
    }
}
