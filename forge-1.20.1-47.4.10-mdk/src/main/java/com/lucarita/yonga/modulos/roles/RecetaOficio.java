package com.lucarita.yonga.modulos.roles;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.lucarita.yonga.core.JugadorFabricando;
import com.lucarita.yonga.modulos.roles.oficios.mochila.MochilaItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * Receta de oficio "yonga:receta_oficio": una receta con forma normal que SOLO puede usar quien tenga
 * cierto oficio con, al menos, cierto rango. Sirve para los objetos de cualquier oficio.
 *
 * En el JSON, además de lo normal de una receta con forma:
 *   "oficio": "explorador",
 *   "rango": "oficial"        (opcional; si no se pone, basta con ser Novato)
 */
public class RecetaOficio extends ShapedRecipe {

    private final Rol oficio;
    private final RangoRol rangoMinimo;

    public RecetaOficio(ShapedRecipe base, Rol oficio, RangoRol rangoMinimo) {
        super(base.getId(), base.getGroup(), base.category(), base.getWidth(), base.getHeight(),
                base.getIngredients(), base.getResultItem(RegistryAccess.EMPTY));
        this.oficio = oficio;
        this.rangoMinimo = rangoMinimo;
    }

    @Override
    public boolean matches(CraftingContainer cuadricula, Level nivel) {
        if (!super.matches(cuadricula, nivel)) {
            return false;
        }
        Player jugador = JugadorFabricando.buscar(cuadricula, nivel);
        return jugador != null
                && RolDatos.obtenerRol(jugador) == oficio
                && RolDatos.obtenerRango(jugador).ordinal() >= rangoMinimo.ordinal();
    }

    /**
     * Mejorar un objeto conserva lo que tenía:
     *  - una mochila (para hacer la mejorada): lo que llevaba dentro pasa a la nueva.
     *  - una herramienta o pieza de armadura (p. ej. de netherita a lucarita): encantamientos, nombre y
     *    desgaste pasan a la nueva.
     */
    @Override
    public ItemStack assemble(CraftingContainer cuadricula, RegistryAccess registros) {
        ItemStack resultado = super.assemble(cuadricula, registros);
        for (int i = 0; i < cuadricula.getContainerSize(); i++) {
            ItemStack ingrediente = cuadricula.getItem(i);
            if (!ingrediente.hasTag()) {
                continue;
            }
            boolean mochila = resultado.getItem() instanceof MochilaItem && ingrediente.getItem() instanceof MochilaItem;
            boolean mejora = resultado.isDamageableItem() && ingrediente.isDamageableItem();
            if (mochila || mejora) {
                resultado.setTag(ingrediente.getTag().copy());
                break;
            }
        }
        return resultado;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModuloRoles.RECETA_OFICIO.get();
    }

    public static class Serializer implements RecipeSerializer<RecetaOficio> {

        @Override
        public RecetaOficio fromJson(ResourceLocation id, JsonObject json) {
            ShapedRecipe base = RecipeSerializer.SHAPED_RECIPE.fromJson(id, json);
            String idOficio = GsonHelper.getAsString(json, "oficio");
            String idRango = GsonHelper.getAsString(json, "rango", RangoRol.NOVATO.id());
            Rol oficio = Rol.porId(idOficio);
            RangoRol rango = RangoRol.porId(idRango);
            if (oficio == null) {
                throw new JsonSyntaxException("Oficio desconocido en la receta " + id + ": " + idOficio);
            }
            if (rango == null) {
                throw new JsonSyntaxException("Rango desconocido en la receta " + id + ": " + idRango);
            }
            return new RecetaOficio(base, oficio, rango);
        }

        @Override
        public RecetaOficio fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            ShapedRecipe base = RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buffer);
            Rol oficio = Rol.porId(buffer.readUtf());
            RangoRol rango = RangoRol.porId(buffer.readUtf());
            return new RecetaOficio(base, oficio, rango == null ? RangoRol.NOVATO : rango);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, RecetaOficio receta) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buffer, receta);
            buffer.writeUtf(receta.oficio.id());
            buffer.writeUtf(receta.rangoMinimo.id());
        }
    }
}
