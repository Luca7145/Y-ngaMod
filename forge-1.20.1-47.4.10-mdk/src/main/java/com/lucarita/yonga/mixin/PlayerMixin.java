package com.lucarita.yonga.mixin;

import com.lucarita.yonga.modulos.roles.cara.AccesoCara;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Le da a cada jugador un hueco de cara, lo guarda con la partida y lo suelta al morir (como la armadura).
 */
@Mixin(Player.class)
public abstract class PlayerMixin implements AccesoCara {

    @Unique
    private static final String YONGA$CLAVE = "YongaCara";

    @Unique
    private final SimpleContainer yonga$hueco = new SimpleContainer(1);

    @Override
    public SimpleContainer yonga$cara() {
        return yonga$hueco;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void yonga$guardarCara(CompoundTag datos, CallbackInfo ci) {
        ItemStack objeto = yonga$hueco.getItem(0);
        if (!objeto.isEmpty()) {
            datos.put(YONGA$CLAVE, objeto.save(new CompoundTag()));
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void yonga$leerCara(CompoundTag datos, CallbackInfo ci) {
        yonga$hueco.setItem(0, datos.contains(YONGA$CLAVE) ? ItemStack.of(datos.getCompound(YONGA$CLAVE)) : ItemStack.EMPTY);
    }

    // Al morir se suelta, igual que el resto del equipo (salvo con la regla keepInventory)
    @Inject(method = "dropEquipment", at = @At("TAIL"))
    private void yonga$soltarCara(CallbackInfo ci) {
        Player jugador = (Player) (Object) this;
        if (jugador.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        ItemStack objeto = yonga$hueco.removeItemNoUpdate(0);
        if (!objeto.isEmpty()) {
            jugador.drop(objeto, true, false);
        }
    }
}
