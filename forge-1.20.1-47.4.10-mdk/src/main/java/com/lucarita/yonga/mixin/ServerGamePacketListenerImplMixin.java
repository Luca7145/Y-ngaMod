package com.lucarita.yonga.mixin;

import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.roles.cara.AccesoCara;
import com.lucarita.yonga.modulos.roles.cara.Cara;
import com.lucarita.yonga.modulos.roles.cara.HuecoCara;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * En creativo, el cliente manda cada hueco del inventario que cambia y el servidor solo acepta los normales (1 a 45).
 * Sin esto, lo que se pusiera en la cara en creativo no llegaría al servidor. Aquí aceptamos también el hueco de la
 * cara (solo objetos de cara, o vaciarlo).
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleSetCreativeModeSlot",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V"),
            cancellable = true)
    private void yonga$huecoCaraEnCreativo(ServerboundSetCreativeModeSlotPacket paquete, CallbackInfo ci) {
        if (!Modulo.ROLES.activo() || !this.player.isCreative()) return;
        int indice = paquete.getSlotNum();
        if (indice < 0 || indice >= this.player.inventoryMenu.slots.size()) return;
        Slot hueco = this.player.inventoryMenu.slots.get(indice);
        if (!(hueco instanceof HuecoCara)) return;
        ItemStack objeto = paquete.getItem();
        if (objeto.isEmpty() || objeto.is(Cara.OBJETOS_DE_CARA)) {
            ((AccesoCara) this.player).yonga$cara().setItem(0, objeto.copyWithCount(Math.min(1, objeto.getCount())));
        }
        this.player.inventoryMenu.broadcastChanges();
        ci.cancel();
    }
}
