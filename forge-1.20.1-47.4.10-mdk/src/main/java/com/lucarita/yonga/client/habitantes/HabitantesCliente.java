package com.lucarita.yonga.client.habitantes;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.client.fe.FeTeclas;
import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.habitantes.Habitante;
import com.lucarita.yonga.modulos.habitantes.red.ConversacionPacket;
import com.lucarita.yonga.modulos.habitantes.red.HabitantesRed;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Tecla de hablar (por defecto G, mirando a un habitante a menos de 6 bloques) y limpieza al salir del mundo. */
@Mod.EventBusSubscriber(modid = YongaMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class HabitantesCliente {

    public static final KeyMapping HABLAR = new KeyMapping("key.yonga.hablar", GLFW.GLFW_KEY_G, FeTeclas.CATEGORIA);
    public static final KeyMapping FAMILIA = new KeyMapping("key.yonga.familia", GLFW.GLFW_KEY_J, FeTeclas.CATEGORIA);

    private HabitantesCliente() {}

    @SubscribeEvent
    public static void alTick(TickEvent.ClientTickEvent evento) {
        if (evento.phase != TickEvent.Phase.END || !Modulo.HABITANTES.activo()) return;
        Minecraft mc = Minecraft.getInstance();
        SexoScreen.quizaAbrir();
        NombrarBebeScreen.quizaAbrir();
        if (mc.player == null || mc.screen != null) return;
        while (FAMILIA.consumeClick()) {
            HabitantesRed.CANAL.sendToServer(new com.lucarita.yonga.modulos.habitantes.red.PedirFamiliaPacket());
        }
        while (HABLAR.consumeClick()) {
            if (mc.crosshairPickEntity instanceof Habitante habitante && habitante.distanceToSqr(mc.player) <= 6 * 6) {
                HabitantesRed.CANAL.sendToServer(new ConversacionPacket(habitante.getId(), ConversacionPacket.ABRIR, ""));
            }
        }
    }

    @SubscribeEvent
    public static void alSalir(ClientPlayerNetworkEvent.LoggingOut evento) {
        Burbujas.vaciar();
        CapaBarriga.vaciar();
        Encima.vaciar();
        SexoScreen.olvidar();
        NombrarBebeScreen.olvidar();
    }
}
