package com.lucarita.yonga.mixin;

import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.roles.cara.AccesoCara;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * El hueco de la cara en la pestaña de inventario del creativo. Minecraft copia ahí todos los huecos del inventario,
 * pero el nuestro (al no conocerlo) lo dejaba encima de la barra rápida, tapado. Lo ponemos al otro lado de la
 * armadura, igual de separado que el hueco del escudo, y le dibujamos su marco.
 */
@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {

    private static final int CARA_X = 127, CARA_Y = 20;

    @Shadow
    private static CreativeModeTab selectedTab;

    @Inject(method = "selectTab", at = @At("TAIL"))
    private void yonga$colocarHuecoCara(CreativeModeTab pestana, CallbackInfo ci) {
        Slot cara = yonga$huecoCara();
        if (cara != null && pestana.getType() == CreativeModeTab.Type.INVENTORY) {
            ((SlotAccessor) cara).yonga$ponerX(CARA_X);
            ((SlotAccessor) cara).yonga$ponerY(CARA_Y);
        }
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void yonga$marcoHuecoCara(GuiGraphics g, float parcial, int ratonX, int ratonY, CallbackInfo ci) {
        if (selectedTab == null || selectedTab.getType() != CreativeModeTab.Type.INVENTORY || yonga$huecoCara() == null) {
            return;
        }
        AbstractContainerScreen<?> pantalla = (AbstractContainerScreen<?>) (Object) this;
        int x = pantalla.getGuiLeft() + CARA_X - 1, y = pantalla.getGuiTop() + CARA_Y - 1;
        g.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        g.fill(x, y, x + 17, y + 1, 0xFF373737);
        g.fill(x, y, x + 1, y + 17, 0xFF373737);
        g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
    }

    /** El hueco de la pantalla que es la cara del jugador (null si no lo hay: otra pestaña o roles apagado). */
    private Slot yonga$huecoCara() {
        Minecraft mc = Minecraft.getInstance();
        if (!Modulo.ROLES.activo() || mc.player == null) return null;
        Container cara = ((AccesoCara) mc.player).yonga$cara();
        for (Slot hueco : ((AbstractContainerScreen<?>) (Object) this).getMenu().slots) {
            if (hueco.container == cara) return hueco;
        }
        return null;
    }
}
