package com.lucarita.yonga.client.roles;

import com.lucarita.yonga.modulos.roles.ModuloRoles;
import com.lucarita.yonga.modulos.roles.oficios.GarfioEntity;
import com.lucarita.yonga.modulos.roles.oficios.GarfioItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Dibuja el gancho del garfio (el modelo "solo gancho", apuntando hacia donde va) y la cuerda que lo une a la mano.
 * La cuerda son dos tiras cruzadas de color cuerda, que cuelgan un poco mientras el gancho vuela y van tensas
 * cuando está clavado.
 */
public class GarfioRenderer extends EntityRenderer<GarfioEntity> {

    private static final float GROSOR = 0.025f;
    private static final float COLGADO = 0.6f;

    private final ItemRenderer objetos;
    private final ItemStack gancho;

    public GarfioRenderer(EntityRendererProvider.Context contexto) {
        super(contexto);
        this.objetos = contexto.getItemRenderer();
        this.gancho = GarfioItem.soloGancho(new ItemStack(ModuloRoles.GARFIO.get()));
    }

    @Override
    public void render(GarfioEntity entidad, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
        Vec3 aqui = new Vec3(Mth.lerp(parcial, entidad.xo, entidad.getX()),
                Mth.lerp(parcial, entidad.yo, entidad.getY()),
                Mth.lerp(parcial, entidad.zo, entidad.getZ()));
        Vec3 mano = entidad.getOwner() instanceof Player jugador ? posicionMano(jugador, parcial) : null;

        // El gancho, con las puntas hacia fuera (lejos de la mano)
        pose.pushPose();
        if (mano != null) {
            Vec3 fuera = aqui.subtract(mano);
            if (fuera.lengthSqr() > 1.0E-4) {
                fuera = fuera.normalize();
                float guinada = (float) Math.atan2(fuera.x, fuera.z);
                float cabeceo = (float) Math.asin(Mth.clamp(fuera.y, -1, 1));
                pose.mulPose(Axis.YP.rotation(guinada));
                pose.mulPose(Axis.XP.rotation(-cabeceo + (float) Math.PI / 2));   // el dibujo apunta hacia arriba
            }
        }
        pose.scale(0.8f, 0.8f, 0.8f);
        objetos.renderStatic(gancho, ItemDisplayContext.FIXED, luz, OverlayTexture.NO_OVERLAY, pose, buffers, entidad.level(), entidad.getId());
        pose.popPose();

        // La cuerda
        if (mano != null) {
            dibujarCuerda(pose, buffers.getBuffer(RenderType.leash()), mano.subtract(aqui), luz, entidad.enganchado() ? 0f : COLGADO);
        }
        super.render(entidad, giro, parcial, pose, buffers, luz);
    }

    /** Dónde está la mano del jugador (lo mismo que hace Minecraft con el sedal de la caña de pescar). */
    private Vec3 posicionMano(Player jugador, float parcial) {
        int lado = jugador.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        if (!(jugador.getMainHandItem().getItem() instanceof GarfioItem)) {
            lado = -lado;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.getCameraType().isFirstPerson() && jugador == mc.player) {
            double fov = 960.0 / mc.options.fov().get();
            float balanceo = Mth.sin(Mth.sqrt(jugador.getAttackAnim(parcial)) * (float) Math.PI);
            Vec3 punto = this.entityRenderDispatcher.camera.getNearPlane().getPointOnPlane(lado * 0.525f, -0.1f)
                    .scale(fov).yRot(balanceo * 0.5f).xRot(-balanceo * 0.7f);
            return jugador.getEyePosition(parcial).add(punto);
        }
        float cuerpo = Mth.lerp(parcial, jugador.yBodyRotO, jugador.yBodyRot) * Mth.DEG_TO_RAD;
        double seno = Mth.sin(cuerpo), coseno = Mth.cos(cuerpo);
        double ladoX = lado * 0.35;
        return new Vec3(
                Mth.lerp(parcial, jugador.xo, jugador.getX()) - coseno * ladoX - seno * 0.8,
                jugador.yo + jugador.getEyeHeight() + (jugador.getY() - jugador.yo) * parcial - 0.45,
                Mth.lerp(parcial, jugador.zo, jugador.getZ()) - seno * ladoX + coseno * 0.8);
    }

    private static void dibujarCuerda(PoseStack pose, VertexConsumer vertices, Vec3 hasta, int luz, float colgado) {
        Matrix4f matriz = pose.last().pose();
        double largo = hasta.length();
        int partes = Math.max(4, (int) (largo * 3));
        Vec3 direccion = largo < 1.0E-4 ? new Vec3(0, 1, 0) : hasta.normalize();
        Vec3 ancho1 = direccion.cross(new Vec3(0, 1, 0));
        if (ancho1.lengthSqr() < 1.0E-4) {
            ancho1 = new Vec3(1, 0, 0);
        }
        ancho1 = ancho1.normalize().scale(GROSOR);
        Vec3 ancho2 = direccion.cross(ancho1).normalize().scale(GROSOR);
        for (int tira = 0; tira < 2; tira++) {
            Vec3 ancho = tira == 0 ? ancho1 : ancho2;
            for (int k = 0; k <= partes; k++) {
                int i = tira == 0 ? k : partes - k;
                float t = (float) i / partes;
                Vec3 p = hasta.scale(t).add(0, -colgado * Mth.sin(t * (float) Math.PI) * Math.min(1, largo / 6), 0);
                float tono = (i % 2 == 0) ? 1.0f : 0.85f;        // trenzado
                float r = 0.62f * tono, g = 0.49f * tono, b = 0.32f * tono;
                vertices.vertex(matriz, (float) (p.x + ancho.x), (float) (p.y + ancho.y), (float) (p.z + ancho.z))
                        .color(r, g, b, 1.0f).uv2(luz).endVertex();
                vertices.vertex(matriz, (float) (p.x - ancho.x), (float) (p.y - ancho.y), (float) (p.z - ancho.z))
                        .color(r, g, b, 1.0f).uv2(luz).endVertex();
            }
        }
    }

    // Que se dibuje aunque el gancho quede fuera de la pantalla (para ver la cuerda)
    @Override
    public boolean shouldRender(GarfioEntity entidad, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(GarfioEntity entidad) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
