package com.lucarita.yonga.mixin;

import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.roles.cara.AccesoCara;
import com.lucarita.yonga.modulos.roles.cara.HuecoCara;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import com.lucarita.yonga.modulos.roles.cara.Cara;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Añade el hueco de cara al inventario del jugador (al final, detrás del hueco del escudo).
 * Solo si el módulo de roles está activado. Con mayúsculas+click, los objetos de cara van a su hueco (como la
 * armadura y el escudo van a los suyos).
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

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void yonga$alHuecoCara(Player jugador, int indice, CallbackInfoReturnable<ItemStack> cir) {
        if (!Modulo.ROLES.activo() || indice < 9 || indice > 45) return;      // solo desde el inventario, la barra o el escudo
        int hueco = -1;
        for (int i = 0; i < this.slots.size(); i++) {
            if (this.slots.get(i) instanceof HuecoCara) hueco = i;
        }
        if (hueco < 0) return;
        Slot origen = this.slots.get(indice);
        ItemStack objeto = origen.getItem();
        if (objeto.isEmpty() || !objeto.is(Cara.OBJETOS_DE_CARA) || this.slots.get(hueco).hasItem()) return;
        ItemStack copia = objeto.copy();
        if (this.moveItemStackTo(objeto, hueco, hueco + 1, false)) {
            if (objeto.isEmpty()) origen.setByPlayer(ItemStack.EMPTY);
            else origen.setChanged();
            cir.setReturnValue(copia);
        }
    }
}
