package com.lucarita.yonga.modulos;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.lucarita.yonga.YongaMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

/**
 * Condición para archivos JSON (recetas, etc.): solo se cargan si el módulo está activo.
 *
 * Uso en una receta:
 *   "conditions": [ { "type": "yonga:modulo_activo", "modulo": "armas" } ]
 */
public record ModuloActivoCondition(Modulo modulo) implements ICondition {

    public static final ResourceLocation ID = new ResourceLocation(YongaMain.MOD_ID, "modulo_activo");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return modulo.activo();
    }

    public static class Serializer implements IConditionSerializer<ModuloActivoCondition> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void write(JsonObject json, ModuloActivoCondition condicion) {
            json.addProperty("modulo", condicion.modulo().id());
        }

        @Override
        public ModuloActivoCondition read(JsonObject json) {
            String id = GsonHelper.getAsString(json, "modulo");
            Modulo modulo = Modulo.porId(id);
            if (modulo == null) {
                throw new JsonSyntaxException("Modulo de Yonga desconocido: " + id);
            }
            return new ModuloActivoCondition(modulo);
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    }
}
