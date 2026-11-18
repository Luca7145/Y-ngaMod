package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.ZorElQuemado;
import com.lucarita.yonga.modulos.fe.mobs.ZorParte;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Zor el quemado. La cabeza se dibuja como cualquier mob (inclinada según suba o baje) y, después, cada tramo del
 * cuerpo en la posición de su ZorParte, girado para mirar al tramo de delante: así el cuerpo entero sigue el camino de
 * la cabeza como una serpiente. Las brasas de todo el cuerpo brillan en la oscuridad.
 */
public class ZorRenderer extends MobRenderer<ZorElQuemado, ModeloZor> {

    private static final ResourceLocation TEXTURA = new ResourceLocation(YongaMain.MOD_ID, "textures/entity/zor.png");
    private static final ResourceLocation BRASAS = new ResourceLocation(YongaMain.MOD_ID, "textures/entity/zor_brasas.png");
    private static final RenderType LUZ = RenderType.eyes(BRASAS);
    private static final float CENTRO = 1.0f;     // la mitad de la altura de la cabeza

    private final ModeloZorTramo tramo;

    public ZorRenderer(EntityRendererProvider.Context contexto) {
        super(contexto, new ModeloZor(contexto.bakeLayer(ModeloZor.CAPA)), 2.0f);
        this.tramo = new ModeloZorTramo(contexto.bakeLayer(ModeloZorTramo.CAPA));
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return LUZ;
            }
        });
    }

    @Override
    protected void setupRotations(ZorElQuemado zor, PoseStack pose, float edad, float giro, float parcial) {
        pose.mulPose(Axis.YP.rotationDegrees(180.0f - giro));
        pose.translate(0.0f, CENTRO, 0.0f);
        pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(parcial, zor.xRotO, zor.getXRot())));
        pose.translate(0.0f, -CENTRO, 0.0f);
    }

    @Override
    public void render(ZorElQuemado zor, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
        super.render(zor, giro, parcial, pose, buffers, luz);

        ZorParte[] partes = zor.tramos();
        int n = partes.length;
        double ex = Mth.lerp(parcial, zor.xo, zor.getX());
        double ey = Mth.lerp(parcial, zor.yo, zor.getY());
        double ez = Mth.lerp(parcial, zor.zo, zor.getZ());
        Vec3 delante = new Vec3(ex, ey + zor.getBbHeight() * 0.5, ez);
        float edad = zor.tickCount + parcial;
        int superposicion = LivingEntityRenderer.getOverlayCoords(zor, 0.0f);

        // primero calcula dónde va cada tramo y hacia dónde mira
        Vec3[] sitio = new Vec3[n];
        float[] giros = new float[n], cabeceos = new float[n], escalas = new float[n];
        int[] luces = new int[n];
        float giroAnterior = zor.getYRot(), cabeceoAnterior = zor.getXRot();
        for (int i = 0; i < n; i++) {
            ZorParte p = partes[i];
            Vec3 c = new Vec3(Mth.lerp(parcial, p.xo, p.getX()), Mth.lerp(parcial, p.yo, p.getY()) + p.getBbHeight() * 0.5,
                    Mth.lerp(parcial, p.zo, p.getZ()));
            Vec3 d = delante.subtract(c);
            if (d.lengthSqr() > 1.0E-4) {
                giroAnterior = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0f;
                cabeceoAnterior = (float) (-Mth.atan2(d.y, d.horizontalDistance()) * Mth.RAD_TO_DEG);
            }
            sitio[i] = c;
            giros[i] = giroAnterior;
            cabeceos[i] = cabeceoAnterior;
            escalas[i] = p.getBbWidth() / ModeloZorTramo.ANCHO;
            luces[i] = LevelRenderer.getLightColor(zor.level(), BlockPos.containing(c));
            delante = c;
        }
        // las escamas y, encima, las brasas que brillan
        for (int capa = 0; capa < 2; capa++) {
            var vertices = buffers.getBuffer(capa == 0 ? RenderType.entityCutoutNoCull(TEXTURA) : LUZ);
            for (int i = 0; i < n; i++) {
                pose.pushPose();
                pose.translate(sitio[i].x - ex, sitio[i].y - ey, sitio[i].z - ez);
                pose.mulPose(Axis.YP.rotationDegrees(180.0f - giros[i]));
                pose.mulPose(Axis.XP.rotationDegrees(-cabeceos[i]));
                float s = escalas[i];
                // el largo no baja de lo necesario para que los tramos se solapen (sin huecos al estrecharse)
                pose.scale(-s, -s, Math.max(s, 1.05f));
                tramo.preparar(i, n, edad);
                if (capa == 0) {
                    tramo.render(pose, vertices, luces[i], superposicion);
                } else {
                    tramo.render(pose, vertices, 0xF000F0, OverlayTexture.NO_OVERLAY);
                }
                pose.popPose();
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ZorElQuemado zor) {
        return TEXTURA;
    }
}
