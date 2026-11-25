package com.lucarita.yonga.client.habitantes;

import com.lucarita.yonga.YongaMain;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Junta en UNA textura todas las capas planas de un habitante (piel, ojos, marcas, ropa, pelo y barba teñidos,
 * complemento) y la guarda para reutilizarla.
 *
 * Antes cada capa se dibujaba encima de la anterior sobre el mismo cuerpo. Con shaders (Iris, Oculus, OptiFine) o
 * algunos mods de rendimiento, esas capas pegadas al cuerpo se pierden (la piel, translúcida, se dibuja después y
 * las tapa): por eso salían sin ojos. Con una sola textura se ven igual con cualquier cosa, y además se dibuja más
 * rápido (un cuerpo en vez de siete).
 */
final class PielCompuesta {

    private PielCompuesta() {}

    /** Una capa: su textura (null = no hay) y el color con que se tiñe (0xFFFFFF = sin teñir). */
    record Capa(@Nullable ResourceLocation textura, int color) {}

    private static final Map<String, ResourceLocation> HECHAS = new HashMap<>();
    private static int contador;

    static ResourceLocation de(ResourceLocation piel, Capa... capas) {
        StringBuilder clave = new StringBuilder(piel.toString());
        for (Capa c : capas) {
            clave.append('|').append(c.textura()).append('#').append(Integer.toHexString(c.color()));
        }
        return HECHAS.computeIfAbsent(clave.toString(), k -> componer(piel, capas));
    }

    private static ResourceLocation componer(ResourceLocation piel, Capa[] capas) {
        try {
            NativeImage base = leer(piel);
            if (base == null) return piel;
            NativeImage resultado = new NativeImage(base.getWidth(), base.getHeight(), true);
            resultado.copyFrom(base);
            base.close();
            for (Capa c : capas) {
                if (c.textura() == null) continue;
                NativeImage encima = leer(c.textura());
                if (encima == null) continue;
                pegar(resultado, encima, c.color());
                encima.close();
            }
            ResourceLocation id = new ResourceLocation(YongaMain.MOD_ID, "habitante_compuesto/" + (contador++));
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(resultado));
            return id;
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().warn("Yonga - no se pudo componer la piel de un habitante: {}", e.toString());
            return piel;
        }
    }

    @Nullable
    private static NativeImage leer(ResourceLocation textura) {
        try {
            Resource recurso = Minecraft.getInstance().getResourceManager().getResource(textura).orElse(null);
            if (recurso == null) return null;
            try (InputStream entrada = recurso.open()) {
                return NativeImage.read(entrada);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Pega "encima" sobre "base", teñido con "color", igual que lo hacía la capa recortada: lo que tiene algo de
     * opacidad (más del 10 %) tapa del todo lo de debajo, y lo demás no se pinta. Los píxeles van en ABGR.
     */
    private static void pegar(NativeImage base, NativeImage encima, int color) {
        float tr = ((color >> 16) & 0xFF) / 255f, tg = ((color >> 8) & 0xFF) / 255f, tb = (color & 0xFF) / 255f;
        int ancho = Math.min(base.getWidth(), encima.getWidth()), alto = Math.min(base.getHeight(), encima.getHeight());
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                int e = encima.getPixelRGBA(x, y);
                if (((e >>> 24) & 0xFF) < 26) continue;
                int r = Math.round((e & 0xFF) * tr), g = Math.round(((e >> 8) & 0xFF) * tg), b = Math.round(((e >> 16) & 0xFF) * tb);
                base.setPixelRGBA(x, y, (0xFF << 24) | (b << 16) | (g << 8) | r);
            }
        }
    }
}
