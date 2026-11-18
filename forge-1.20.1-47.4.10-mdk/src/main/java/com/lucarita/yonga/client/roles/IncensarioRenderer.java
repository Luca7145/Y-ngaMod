package com.lucarita.yonga.client.roles;

import com.lucarita.yonga.modulos.roles.ModuloRoles;
import com.lucarita.yonga.modulos.roles.oficios.BotafumeiroItem;
import com.lucarita.yonga.modulos.roles.oficios.IncensarioEntity;
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
 * Dibuja el incensario lanzado (el modelo "solo incensario" del botafumeiro, mirando siempre a la cámara
 * y girando) y la cadena que lo une a la mano del jugador.
 *
 * La cadena son dos tiras cruzadas (para que se vea desde cualquier lado) de eslabones gris oscuro y gris
 * claro alternados, que cuelga un poco por el peso.
 */
public class IncensarioRenderer extends EntityRenderer<IncensarioEntity> {

    private static final int ESLABONES_POR_BLOQUE = 4;
    private static final float GROSOR = 0.035f;
    private static final float COLGADO = 0.25f;             // cuánto cuelga la cadena por el medio

    private final ItemRenderer objetos;
    private final ItemStack incensario;

    public IncensarioRenderer(EntityRendererProvider.Context contexto) {
        super(contexto);
        this.objetos = contexto.getItemRenderer();
        this.incensario = new ItemStack(ModuloRoles.BOTAFUMEIRO.get());
        BotafumeiroItem.marcarSoloIncensario(this.incensario);
    }

    @Override
    public void render(IncensarioEntity entidad, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
        // El incensario
        pose.pushPose();
        pose.translate(0, 0.15, 0);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.ZP.rotationDegrees((entidad.tickCount + parcial) * 25f));
        pose.scale(0.9f, 0.9f, 0.9f);
        objetos.renderStatic(incensario, ItemDisplayContext.GROUND, luz, OverlayTexture.NO_OVERLAY, pose, buffers, entidad.level(), entidad.getId());
        pose.popPose();

        // La cadena
        if (entidad.getOwner() instanceof Player jugador) {
            Vec3 mano = posicionMano(jugador, parcial);
            Vec3 aqui = new Vec3(Mth.lerp(parcial, entidad.xo, entidad.getX()),
                    Mth.lerp(parcial, entidad.yo, entidad.getY()) + 0.15,
                    Mth.lerp(parcial, entidad.zo, entidad.getZ()));
            dibujarCadena(pose, buffers.getBuffer(RenderType.leash()), mano.subtract(aqui).add(0, 0.15, 0), luz);
        }
        super.render(entidad, giro, parcial, pose, buffers, luz);
    }

    /** Dónde está la mano del jugador (lo mismo que hace Minecraft con el sedal de la caña de pescar). */
    private Vec3 posicionMano(Player jugador, float parcial) {
        int lado = jugador.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        if (!(jugador.getMainHandItem().getItem() instanceof BotafumeiroItem)) {
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

    private static void dibujarCadena(PoseStack pose, VertexConsumer vertices, Vec3 hasta, int luz) {
        Matrix4f matriz = pose.last().pose();
        double largo = hasta.length();
        int partes = Math.max(4, (int) (largo * ESLABONES_POR_BLOQUE));
        // Dos direcciones perpendiculares a la cadena, para las dos tiras cruzadas
        Vec3 direccion = largo < 1.0E-4 ? new Vec3(0, 1, 0) : hasta.normalize();
        Vec3 ancho1 = direccion.cross(new Vec3(0, 1, 0));
        if (ancho1.lengthSqr() < 1.0E-4) {
            ancho1 = new Vec3(1, 0, 0);
        }
        ancho1 = ancho1.normalize().scale(GROSOR);
        Vec3 ancho2 = direccion.cross(ancho1).normalize().scale(GROSOR);
        // La segunda tira va al revés (de la mano al incensario): así las dos se unen en el mismo punto y no
        // sale un triángulo raro entre ellas (lo mismo hace Minecraft con las riendas)
        for (int tira = 0; tira < 2; tira++) {
            Vec3 ancho = tira == 0 ? ancho1 : ancho2;
            for (int k = 0; k <= partes; k++) {
                int i = tira == 0 ? k : partes - k;
                float t = (float) i / partes;
                Vec3 p = hasta.scale(t).add(0, -COLGADO * Mth.sin(t * (float) Math.PI) * Math.min(1, largo / 4), 0);
                float gris = (i % 2 == 0) ? 0.22f : 0.48f;   // eslabones oscuros y claros
                vertices.vertex(matriz, (float) (p.x + ancho.x), (float) (p.y + ancho.y), (float) (p.z + ancho.z))
                        .color(gris, gris, gris * 1.05f, 1.0f).uv2(luz).endVertex();
                vertices.vertex(matriz, (float) (p.x - ancho.x), (float) (p.y - ancho.y), (float) (p.z - ancho.z))
                        .color(gris, gris, gris * 1.05f, 1.0f).uv2(luz).endVertex();
            }
        }
    }

    // Que se dibuje aunque el incensario quede fuera de la pantalla (para ver la cadena)
    @Override
    public boolean shouldRender(IncensarioEntity entidad, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(IncensarioEntity entidad) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
