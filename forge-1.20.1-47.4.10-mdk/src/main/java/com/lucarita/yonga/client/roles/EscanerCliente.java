package com.lucarita.yonga.client.roles;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.roles.oficios.EscanerItem;
import com.lucarita.yonga.modulos.roles.red.EscanerPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.List;

/**
 * El sonar del escáner en el cliente: guarda las manchas del último barrido y dibuja la tablet que se ve en la mano
 * (como el mapa, ver mixin/ItemInHandRendererMixin).
 *
 * Se dibuja en las medidas del mapa: la pantalla va de 0 a 128 "píxeles" (centro en 64, 64; 60 de radio = 32
 * bloques) y el marco de la tablet de -8 a 136. El sonar gira contigo (arriba = hacia donde miras) y marca el norte
 * con una N. Cada zona con mineral es un punto rojo, sin más (ni altura ni cantidad). La línea del barrido da la vuelta
 * cada 3 segundos y los puntos se "descubren" uno a uno justo cuando la línea pasa por encima: entonces se encienden
 * y se van apagando hasta la siguiente pasada. Los que ya no están en el último barrido no vuelven a encenderse y se
 * apagan solos. Arriba a la izquierda, el mineral que busca; abajo, la batería que le queda.
 */
public final class EscanerCliente {

    private static final ResourceLocation TABLET = new ResourceLocation(YongaMain.MOD_ID, "textures/misc/escaner_tablet.png");
    private static final ResourceLocation BARRIDO = new ResourceLocation(YongaMain.MOD_ID, "textures/misc/escaner_barrido.png");
    private static final ResourceLocation PUNTO = new ResourceLocation(YongaMain.MOD_ID, "textures/misc/escaner_punto.png");
    private static final ResourceLocation BLANCO = new ResourceLocation(YongaMain.MOD_ID, "textures/misc/escaner_blanco.png");

    private static final float CENTRO = 64f, RADIO = 60f;
    private static final float VUELTA = 60f;                 // ticks que tarda la línea en dar la vuelta
    private static final long CADUCA = 200;                  // ticks sin noticias del servidor: se borra todo
    private static final int LUZ = LightTexture.FULL_BRIGHT; // la pantalla brilla aunque esté oscuro

    /** Un punto del sonar: dónde está, cuándo lo encontró la línea por última vez, y si sigue en el último barrido. */
    private static final class Punto {
        final int x, z;
        float encontrado = Float.NaN;      // tiempo (en ticks) de la última vez que pasó la línea; NaN = aún no
        boolean vigente = true;

        Punto(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }

    private static final List<Punto> PUNTOS = new java.util.ArrayList<>();
    private static long recibido = -1000;
    private static float barridoAnterior = Float.NaN;      // dónde estaba la línea en el fotograma anterior

    private EscanerCliente() {}

    public static void recibir(List<EscanerPacket.Mancha> nuevas) {
        Minecraft mc = Minecraft.getInstance();
        recibido = mc.level != null ? mc.level.getGameTime() : 0;
        // Los que siguen (misma posición) conservan su brillo; los nuevos esperan a que pase la línea
        for (Punto p : PUNTOS) p.vigente = false;
        for (EscanerPacket.Mancha m : nuevas) {
            Punto existente = null;
            for (Punto p : PUNTOS) {
                if (p.x == m.x() && p.z == m.z()) existente = p;
            }
            if (existente != null) existente.vigente = true;
            else PUNTOS.add(new Punto(m.x(), m.z()));
        }
        if (nuevas.isEmpty()) PUNTOS.clear();
    }

    /** La tablet, en las coordenadas del papel del mapa (x a la derecha, y hacia abajo, de -7 a 135). */
    public static void dibujarTablet(PoseStack pose, MultiBufferSource buffers, int luz, ItemStack escaner) {
        Minecraft mc = Minecraft.getInstance();
        Player jugador = mc.player;
        Matrix4f m = pose.last().pose();
        cuadro(buffers.getBuffer(RenderType.text(TABLET)), m, -8, -8, 136, 136, 0, 0xFFFFFFFF, luz);
        if (jugador == null || mc.level == null) return;

        // La batería, abajo en el marco
        float parte = (float) EscanerItem.energia(escaner) / EscanerItem.MAX_ENERGIA;
        VertexConsumer blanco = buffers.getBuffer(RenderType.text(BLANCO));
        cuadro(blanco, m, 8, 129.5f, 120, 133.5f, -0.1f, 0xFF1A1A1A, LUZ);
        if (parte > 0) cuadro(blanco, m, 9, 130.5f, 9 + 110 * parte, 132.5f, -0.2f, 0xFFD02020, LUZ);

        boolean vivo = EscanerItem.encendido(escaner) && EscanerItem.energia(escaner) > 0;
        if (!vivo) return;

        float tiempo = mc.level.getGameTime() + mc.getFrameTime();
        float barrido = (tiempo % VUELTA) / VUELTA * Mth.TWO_PI;

        // La línea del barrido (una cuña con estela que gira)
        pose.pushPose();
        pose.translate(CENTRO, CENTRO, -0.1f);
        pose.mulPose(Axis.ZP.rotation(barrido));
        cuadro(buffers.getBuffer(RenderType.text(BARRIDO)), pose.last().pose(), -RADIO, -RADIO, RADIO, RADIO, 0, 0xFFFFFFFF, LUZ);
        pose.popPose();

        float giro = jugador.getViewYRot(mc.getFrameTime()) * Mth.DEG_TO_RAD;
        double delanteX = -Mth.sin(giro), delanteZ = Mth.cos(giro);
        double derechaX = -Mth.cos(giro), derechaZ = -Mth.sin(giro);

        // La N del norte
        double nx = -derechaZ, ny = delanteZ;
        Font letra = mc.font;
        pose.pushPose();
        pose.translate(CENTRO + (float) nx * (RADIO - 7), CENTRO + (float) ny * (RADIO - 7), -0.3f);
        pose.scale(0.9f, 0.9f, 1);
        letra.drawInBatch("N", -letra.width("N") / 2f, -4, 0xFF8FD8A0, false, pose.last().pose(), buffers,
                Font.DisplayMode.NORMAL, 0, LUZ);
        pose.popPose();

        // Los puntos rojos: cada uno se enciende cuando la línea pasa por encima y se va apagando en una vuelta
        float antes = barridoAnterior;
        barridoAnterior = barrido;
        float avance = Float.isNaN(antes) ? 0 : Mth.positiveModulo(barrido - antes, Mth.TWO_PI);
        if (avance > 1.0f) avance = 0;                               // ha habido un salto (otra pantalla, pausa...): no cuenta
        if (mc.level.getGameTime() - recibido >= CADUCA) PUNTOS.clear();
        VertexConsumer punto = buffers.getBuffer(RenderType.text(PUNTO));
        double escala = RADIO / EscanerItem.RADIO;
        java.util.Iterator<Punto> it = PUNTOS.iterator();
        while (it.hasNext()) {
            Punto p = it.next();
            double rx = p.x + 0.5 - jugador.getX(), rz = p.z + 0.5 - jugador.getZ();
            float sx = (float) ((rx * derechaX + rz * derechaZ) * escala);
            float sy = (float) (-(rx * delanteX + rz * delanteZ) * escala);
            float angulo = (float) Math.atan2(sx, -sy);
            // ¿Ha pasado la línea por su ángulo entre el fotograma anterior y este?
            if (p.vigente && avance > 0 && Mth.positiveModulo(angulo - antes, Mth.TWO_PI) <= avance) {
                p.encontrado = tiempo;
            }
            if (Float.isNaN(p.encontrado)) continue;                 // aún no lo ha encontrado la línea
            float edad = (tiempo - p.encontrado) / VUELTA;           // 0 = recién encontrado, 1 = una vuelta después
            if (edad >= 1.0f) {
                if (!p.vigente) it.remove();                         // ya no está y se ha apagado del todo
                continue;
            }
            if (sx * sx + sy * sy > (RADIO - 3) * (RADIO - 3)) continue;
            int alfa = (int) (255 * (1.0f - 0.85f * edad));
            float tam = 3.0f + 1.2f * Math.max(0, 1 - edad * 6);      // al encontrarlo, un instante más grande
            cuadro(punto, m, CENTRO + sx - tam, CENTRO + sy - tam, CENTRO + sx + tam, CENTRO + sy + tam, -0.4f,
                    (alfa << 24) | 0xFFFFFF, LUZ);
        }

        // El mineral que busca, pequeño, arriba a la izquierda de la pantalla
        ItemStack muestra = EscanerItem.muestra(escaner);
        if (!muestra.isEmpty()) {
            pose.pushPose();
            pose.translate(12, 12, -0.5f);
            pose.scale(14, -14, 0.01f);
            mc.getItemRenderer().renderStatic(muestra, ItemDisplayContext.GUI, LUZ, OverlayTexture.NO_OVERLAY, pose, buffers,
                    mc.level, 0);
            pose.popPose();
        }
    }

    private static void cuadro(VertexConsumer v, Matrix4f m, float x0, float y0, float x1, float y1, float z, int argb, int luz) {
        int a = argb >>> 24, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        v.vertex(m, x0, y1, z).color(r, g, b, a).uv(0, 1).uv2(luz).endVertex();
        v.vertex(m, x1, y1, z).color(r, g, b, a).uv(1, 1).uv2(luz).endVertex();
        v.vertex(m, x1, y0, z).color(r, g, b, a).uv(1, 0).uv2(luz).endVertex();
        v.vertex(m, x0, y0, z).color(r, g, b, a).uv(0, 0).uv2(luz).endVertex();
    }
}
