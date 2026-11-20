package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.rey.EsqueletoRey;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Las piezas del Rey de Arenisca: una por hueso de EsqueletoRey (con la caja de ese hueso, más los adornos: la
 * corona en la cabeza y las hombreras en los brazos) y los sellos. No se mueven solas: ReyRenderer coloca cada una
 * con la matriz de su hueso y la escala x8 (1 píxel = medio bloque). Las cajas están en píxeles, con los ejes del
 * modelo de Minecraft: x = -2·x del hueso, y = -2·y, z = 2·z. Los miembros derechos van en espejo, para que los
 * asideros queden siempre por fuera. Textura 256x128 (herramientas/dibujar_rey.py).
 */
public final class ModeloRey {

    public static final ModelLayerLocation CAPA = new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "rey_arenisca"), "main");

    private static final int[][] TEXTURA = {
            {0, 36}, {0, 0}, {96, 0}, {0, 58}, {72, 58}, {100, 58}, {0, 58}, {72, 58}, {100, 58},
            {124, 58}, {160, 58}, {192, 58}, {124, 58}, {160, 58}, {192, 58}};

    public final ModelPart[] huesos = new ModelPart[EsqueletoRey.N];
    public final ModelPart selloVivo, selloRoto;

    public ModeloRey(ModelPart raiz) {
        for (int b = 0; b < EsqueletoRey.N; b++) {
            huesos[b] = raiz.getChild("hueso_" + b);
        }
        selloVivo = raiz.getChild("sello_vivo");
        selloRoto = raiz.getChild("sello_roto");
    }

    private static boolean derecho(int b) {
        return b == EsqueletoRey.BRAZO_D || b == EsqueletoRey.ANTEBRAZO_D || b == EsqueletoRey.MANO_D
                || b == EsqueletoRey.MUSLO_D || b == EsqueletoRey.ESPINILLA_D || b == EsqueletoRey.PIE_D;
    }

    public static LayerDefinition crearCapa() {
        MeshDefinition malla = new MeshDefinition();
        PartDefinition raiz = malla.getRoot();
        for (int b = 0; b < EsqueletoRey.N; b++) {
            float[] c = EsqueletoRey.CAJA[b];
            float x0 = -2 * c[3], y0 = -2 * c[4], z0 = 2 * c[2];
            float dx = 2 * (c[3] - c[0]), dy = 2 * (c[4] - c[1]), dz = 2 * (c[5] - c[2]);
            CubeListBuilder cubos = CubeListBuilder.create().texOffs(TEXTURA[b][0], TEXTURA[b][1]);
            if (derecho(b)) cubos = cubos.mirror();
            cubos = cubos.addBox(x0, y0, z0, dx, dy, dz);
            if (b == EsqueletoRey.CABEZA) {
                // la corona: un aro de oro (deja libre la coronilla, donde está el último sello) y ocho puntas
                cubos = cubos.texOffs(160, 0).addBox(-9, -20, 7, 18, 4, 2).texOffs(160, 0).addBox(-9, -20, -9, 18, 4, 2)
                        .texOffs(160, 8).addBox(7, -20, -7, 2, 4, 14).texOffs(160, 8).addBox(-9, -20, -7, 2, 4, 14);
                int[][] puntas = {{0, 7}, {0, -9}, {7, -1}, {-9, -1}, {7, 7}, {-9, 7}, {7, -9}, {-9, -9}};
                for (int[] p : puntas) {
                    cubos = cubos.texOffs(200, 8).addBox(p[0], -25, p[1], 2, 5, 2);
                }
            }
            if (b == EsqueletoRey.BRAZO_I || b == EsqueletoRey.BRAZO_D) {
                cubos = cubos.texOffs(32, 58).addBox(-5, -2, -5, 10, 7, 10);       // la hombrera
            }
            raiz.addOrReplaceChild("hueso_" + b, cubos, PartPose.ZERO);
        }
        raiz.addOrReplaceChild("sello_vivo", CubeListBuilder.create().texOffs(0, 90).addBox(-2.5f, -2.5f, -2.5f, 5, 5, 5), PartPose.ZERO);
        raiz.addOrReplaceChild("sello_roto", CubeListBuilder.create().texOffs(20, 90).addBox(-2.5f, -2.5f, -2.5f, 5, 5, 5), PartPose.ZERO);
        return LayerDefinition.create(malla, 256, 128);
    }
}
