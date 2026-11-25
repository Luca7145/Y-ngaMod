package com.lucarita.yonga.client.habitantes;

import com.lucarita.yonga.modulos.habitantes.Habitante;
import com.lucarita.yonga.modulos.habitantes.red.BurbujaPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Los bocadillos de los habitantes: lo que dicen, sobre su cabeza, durante unos segundos ("…" mientras piensan).
 * Se ven desde 24 bloques; las líneas largas se parten y se enseñan como mucho 5.
 */
public final class Burbujas {

    private record Burbuja(Component texto, long hasta) {}

    private static final Map<Integer, Burbuja> BURBUJAS = new HashMap<>();
    private static final int ANCHO = 160, MAX_LINEAS = 5;

    private Burbujas() {}

    public static void recibir(BurbujaPacket paquete) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        BURBUJAS.put(paquete.idHabitante(), new Burbuja(paquete.texto(), mc.level.getGameTime() + paquete.ticks()));
    }

    public static void vaciar() {
        BURBUJAS.clear();
    }

    @Nullable
    private static Component de(Habitante habitante) {
        Burbuja b = BURBUJAS.get(habitante.getId());
        if (b == null) return null;
        if (habitante.level().getGameTime() > b.hasta()) {
            BURBUJAS.remove(habitante.getId());
            return null;
        }
        return b.texto();
    }

    /** Lo llama HabitanteRenderer después de dibujar al habitante. */
    static void dibujar(Habitante habitante, PoseStack pose, MultiBufferSource buffers, int luz,
                        net.minecraft.client.renderer.entity.EntityRenderDispatcher despachador) {
        Component texto = de(habitante);
        if (texto == null || despachador.distanceToSqr(habitante) > 24 * 24) return;
        Font fuente = Minecraft.getInstance().font;
        List<FormattedCharSequence> lineas = fuente.split(texto, ANCHO);
        if (lineas.size() > MAX_LINEAS) lineas = lineas.subList(lineas.size() - MAX_LINEAS, lineas.size());

        pose.pushPose();
        pose.translate(0.0, habitante.getBbHeight() + 0.45 + lineas.size() * 0.25, 0.0);
        pose.mulPose(despachador.cameraOrientation());
        pose.scale(-0.025f, -0.025f, 0.025f);
        Matrix4f matriz = pose.last().pose();
        for (int i = 0; i < lineas.size(); i++) {
            FormattedCharSequence linea = lineas.get(i);
            float x = -fuente.width(linea) / 2.0f;
            fuente.drawInBatch(linea, x, i * 10, 0xFF202020, false, matriz, buffers, Font.DisplayMode.NORMAL,
                    0xE6F2EEE4, luz);
        }
        pose.popPose();
    }
}
