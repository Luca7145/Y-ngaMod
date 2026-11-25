package com.lucarita.yonga.client.habitantes;

import com.lucarita.yonga.YongaMain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Fundido a negro de la pantalla: entra en 1 s, se queda negro y sale en 1,5 s. Encima de todo el HUD. */
@Mod.EventBusSubscriber(modid = YongaMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Fundido {

    private static long inicio = -1;
    private static int duracion;

    private Fundido() {}

    public static void empezar(int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mc.screen instanceof ConversacionScreen) mc.setScreen(null);     // que se vea el fundido
        inicio = mc.level.getGameTime();
        duracion = Math.max(60, ticks);
    }

    @SubscribeEvent
    public static void alDibujar(RenderGuiEvent.Post evento) {
        dibujar(evento.getGuiGraphics(), evento.getPartialTick());
    }

    /** También encima de las pantallas (la de "salir de la cama"). */
    @SubscribeEvent
    public static void alDibujarPantalla(net.minecraftforge.client.event.ScreenEvent.Render.Post evento) {
        dibujar(evento.getGuiGraphics(), evento.getPartialTick());
    }

    private static void dibujar(GuiGraphics g, float parcial) {
        Minecraft mc = Minecraft.getInstance();
        if (inicio < 0 || mc.level == null) return;
        float t = (mc.level.getGameTime() - inicio) + parcial;
        if (t > duracion) {
            inicio = -1;
            return;
        }
        float entrada = 20.0f, salida = 30.0f;
        float alfa = t < entrada ? t / entrada : t > duracion - salida ? (duracion - t) / salida : 1.0f;
        int a = (int) (Math.max(0.0f, Math.min(1.0f, alfa)) * 255.0f);
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), a << 24);
        g.pose().popPose();
    }
}
