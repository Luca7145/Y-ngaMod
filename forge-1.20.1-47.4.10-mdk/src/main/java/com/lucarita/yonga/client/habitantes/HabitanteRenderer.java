package com.lucarita.yonga.client.habitantes;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.habitantes.Catalogo;
import com.lucarita.yonga.modulos.habitantes.Epoca;
import com.lucarita.yonga.modulos.habitantes.Habitante;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Dibuja a un habitante por capas, todas sobre el mismo cuerpo:
 *   piel con su cara (y ropa interior) → ojos → ropa de su época → pelo y barba (en gris, teñidos con su color).
 * Esas capas planas se juntan en una sola textura (PielCompuesta), para que se vean bien también con shaders.
 * Las texturas salen de herramientas/dibujar_habitantes.py. Elige el cuerpo de hombre o de mujer en cada fotograma
 * y lo escala según la edad y la altura del habitante.
 */
public class HabitanteRenderer extends LivingEntityRenderer<Habitante, ModeloHabitante> {

    private static final String RUTA = "textures/entity/habitante/";

    /** negro, castaño oscuro, castaño, castaño claro, rubio, pelirrojo, gris, blanco (para teñir el gris del dibujo) */
    private static final int[] COLORES_PELO = {
            0x2A2420, 0x4E3424, 0x7A5032, 0xA07A50, 0xF0D290, 0xC4602A, 0x9A9A9C, 0xF4F2EE};

    private static final ResourceLocation[][][] PIEL = new ResourceLocation[2][Habitante.TONOS][Habitante.CARAS];
    private static final ResourceLocation[][] OJOS = new ResourceLocation[2][Habitante.OJOS];
    /** Las texturas de cada época (ropa, pelo, barba, complemento, marca), según se van pidiendo. */
    private static final java.util.Map<String, ResourceLocation> CACHE = new java.util.HashMap<>();

    static {
        for (int s = 0; s < 2; s++) {
            String sexo = s == 1 ? "mujer" : "hombre";
            for (int t = 0; t < Habitante.TONOS; t++) {
                for (int c = 0; c < Habitante.CARAS; c++) PIEL[s][t][c] = textura("piel_" + sexo + "_" + t + "_" + c);
            }
            for (int i = 0; i < Habitante.OJOS; i++) OJOS[s][i] = textura("ojos_" + sexo + "_" + i);
        }
    }

    private static ResourceLocation deEpoca(String carpeta, Habitante h, boolean conSexo, int n) {
        String clave = carpeta + "/" + Catalogo.dibujo(h.epoca()).id() + (conSexo ? (h.esMujer() ? "_mujer" : "_hombre") : "")
                + "_" + n;
        return CACHE.computeIfAbsent(clave, HabitanteRenderer::textura);
    }

    private final ModeloHabitante hombre, mujer;

    public HabitanteRenderer(EntityRendererProvider.Context contexto) {
        super(contexto, new ModeloHabitante(contexto.bakeLayer(ModeloHabitante.CAPA_HOMBRE), false), 0.5f);
        this.hombre = this.model;
        this.mujer = new ModeloHabitante(contexto.bakeLayer(ModeloHabitante.CAPA_MUJER), true);

        // Ojos, marcas, ropa, pelo, barba y complemento van ya juntos en la textura del cuerpo (ver PielCompuesta)
        this.addLayer(new CapaPelo3D(this, contexto.bakeLayer(CapaPelo3D.CAPA), COLORES_PELO));
        this.addLayer(new CapaBarriga.DeHabitante(this, contexto.bakeLayer(CapaBarriga.CAPA)));
        // Armadura (los guardias llevan casco y cota de malla)
        this.addLayer(new Vestido(this, new net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer<>(this,
                new net.minecraft.client.model.HumanoidModel<>(contexto.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER_INNER_ARMOR)),
                new net.minecraft.client.model.HumanoidModel<>(contexto.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER_OUTER_ARMOR)),
                contexto.getModelManager())));
        this.addLayer(new Vestido(this, new ItemInHandLayer<>(this, contexto.getItemInHandRenderer())));
    }

    private static ResourceLocation textura(String nombre) {
        return new ResourceLocation(YongaMain.MOD_ID, RUTA + nombre + ".png");
    }

    /** El peinado que se ve: los de solo adultos (tonsura, calvicie…), en los chicos, el primero de su época. */
    static int peinadoVisible(Habitante h) {
        int peinado = h.peinado();
        boolean[] adulto = Catalogo.SOLO_ADULTO[Catalogo.dibujo(h.epoca()).ordinal()][h.esMujer() ? 1 : 0];
        return !h.esAdulto() && adulto[peinado] ? 0 : peinado;
    }

    private static ResourceLocation pelo(Habitante h) {
        if (h.etapa() == 0) return null;
        return deEpoca("pelo", h, true, peinadoVisible(h));
    }

    /** La ropa de su época (las épocas aún sin dibujar se visten de medieval). El bebé, envuelto en su mantilla. */
    static ResourceLocation ropa(Habitante h) {
        if (h.enRopaInterior()) return null;            // solo la ropa interior, que va dibujada en la piel
        if (h.etapa() == 0) {
            return CACHE.computeIfAbsent("ropa/" + Catalogo.dibujo(h.epoca()).id() + "_bebe", HabitanteRenderer::textura);
        }
        return deEpoca("ropa", h, true, h.ropa());
    }

    @Override
    public void render(Habitante habitante, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
        this.model = habitante.esMujer() ? this.mujer : this.hombre;
        this.shadowRadius = 0.5f * habitante.getScale();
        boolean encima = Encima.esta(habitante.getId());
        if (encima) {
            pose.pushPose();
            Encima.aplicar(pose, habitante);
        }
        super.render(habitante, giro, parcial, pose, buffers, luz);
        if (encima) pose.popPose();
        Burbujas.dibujar(habitante, pose, buffers, luz, this.entityRenderDispatcher);
    }

    @Override
    protected void scale(Habitante habitante, PoseStack pose, float parcial) {
        float s = habitante.getScale() * 0.9375f;       // 0,9375 = lo que encoge Minecraft al jugador
        pose.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(Habitante h) {
        int pelo = COLORES_PELO[h.colorPelo()];
        return PielCompuesta.de(PIEL[h.esMujer() ? 1 : 0][h.tonoPiel()][h.cara()],
                new PielCompuesta.Capa(OJOS[h.esMujer() ? 1 : 0][h.colorOjos()], 0xFFFFFF),
                new PielCompuesta.Capa(h.marca() == 0 ? null : deEpoca("marca", h, true, h.marca()), 0xFFFFFF),
                new PielCompuesta.Capa(ropa(h), 0xFFFFFF),
                new PielCompuesta.Capa(pelo(h), pelo),
                new PielCompuesta.Capa(h.barba() == 0 ? null : deEpoca("barba", h, false, h.barba()), pelo),
                new PielCompuesta.Capa(h.complemento() == 0 || h.enRopaInterior() ? null
                        : deEpoca("complemento", h, true, h.complemento()), 0xFFFFFF));
    }

    /** Los nombres nunca se ven sobre la cabeza (salvo que el jugador le ponga uno con una etiqueta). */
    @Override
    protected boolean shouldShowName(Habitante habitante) {
        return habitante.hasCustomName() && super.shouldShowName(habitante);
    }

    /** Una capa que solo se dibuja si va vestido (armadura, lo que lleva en la mano). */
    private static class Vestido extends RenderLayer<Habitante, ModeloHabitante> {
        private final RenderLayer<Habitante, ModeloHabitante> capa;

        Vestido(RenderLayerParent<Habitante, ModeloHabitante> padre, RenderLayer<Habitante, ModeloHabitante> capa) {
            super(padre);
            this.capa = capa;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int luz, Habitante habitante, float paso,
                           float amplitud, float parcial, float edadTicks, float giroCabeza, float inclinacion) {
            if (!habitante.enRopaInterior()) {
                capa.render(pose, buffers, luz, habitante, paso, amplitud, parcial, edadTicks, giroCabeza, inclinacion);
            }
        }
    }

    /** Una capa de textura más sobre el mismo cuerpo, opcionalmente teñida. */
    private static class Capa extends RenderLayer<Habitante, ModeloHabitante> {
        private final Function<Habitante, ResourceLocation> textura;
        private final ToIntFunction<Habitante> color;

        Capa(RenderLayerParent<Habitante, ModeloHabitante> padre, Function<Habitante, ResourceLocation> textura,
             ToIntFunction<Habitante> color) {
            super(padre);
            this.textura = textura;
            this.color = color;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int luz, Habitante habitante, float paso,
                           float amplitud, float parcial, float edadTicks, float giroCabeza, float inclinacion) {
            if (habitante.isInvisible()) {
                return;
            }
            ResourceLocation tex = this.textura.apply(habitante);
            if (tex == null) {
                return;
            }
            int c = this.color.applyAsInt(habitante);
            renderColoredCutoutModel(this.getParentModel(), tex, pose, buffers, luz, habitante,
                    ((c >> 16) & 0xFF) / 255.0f, ((c >> 8) & 0xFF) / 255.0f, (c & 0xFF) / 255.0f);
        }
    }
}
