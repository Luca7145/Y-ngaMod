package com.lucarita.yonga.client.habitantes;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.habitantes.red.EncimaPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

/** Quién está acostado encima de otro en una noche juntos: se dibuja un poco más arriba (jugadores y habitantes). */
@Mod.EventBusSubscriber(modid = YongaMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Encima {

    public static final float ALTURA = 0.1f;
    private static final Set<Integer> ENCIMA = new HashSet<>();

    private Encima() {}

    public static void recibir(EncimaPacket p) {
        if (p.encima()) ENCIMA.add(p.idEntidad());
        else ENCIMA.remove(p.idEntidad());
    }

    public static boolean esta(int id) {
        return ENCIMA.contains(id);
    }

    public static void vaciar() {
        ENCIMA.clear();
    }

    /**
     * Quien va encima: un poco más arriba y boca abajo (girado media vuelta sobre el eje del cuerpo, que acostado va
     * a lo largo de la cama). El de abajo se queda boca arriba, como duerme siempre.
     */
    public static void aplicar(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.world.entity.LivingEntity e) {
        pose.translate(0.0, ALTURA, 0.0);
        net.minecraft.core.Direction cama = e.getBedOrientation();
        if (!e.isSleeping() || cama == null) return;
        float grosor = 0.14f;                                   // el eje pasa por el medio del cuerpo tumbado
        pose.translate(0.0, grosor, 0.0);
        pose.mulPose(new org.joml.Quaternionf().rotateAxis((float) Math.PI, cama.getStepX(), 0.0f, cama.getStepZ()));
        pose.translate(0.0, -grosor, 0.0);
    }

    @SubscribeEvent
    public static void antes(RenderPlayerEvent.Pre evento) {
        if (!esta(evento.getEntity().getId())) return;
        evento.getPoseStack().pushPose();
        aplicar(evento.getPoseStack(), evento.getEntity());
    }

    @SubscribeEvent
    public static void despues(RenderPlayerEvent.Post evento) {
        if (esta(evento.getEntity().getId())) evento.getPoseStack().popPose();
    }
}
