package com.lucarita.yonga.modulos.fe.hechizos;

import com.google.gson.JsonObject;
import com.lucarita.yonga.core.JugadorFabricando;
import com.lucarita.yonga.modulos.fe.FeDatos;
import com.lucarita.yonga.modulos.fe.ModuloFe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

/**
 * Receta de libro de hechizo "yonga:libro_hechizo": receta sin forma (ingredientes en cualquier orden)
 * que solo funciona si quien fabrica tiene el rango de fe que pide el hechizo del libro, o uno más extremo.
 * Ej.: el Gran Salto lo puede fabricar un Novicio, un Sacerdote, un Iluminado... pero no un Fiel ni un Hereje.
 */
public class RecetaLibro extends ShapelessRecipe {

    public RecetaLibro(ShapelessRecipe base) {
        super(base.getId(), base.getGroup(), base.category(), base.getResultItem(RegistryAccess.EMPTY), base.getIngredients());
    }

    @Override
    public boolean matches(CraftingContainer cuadricula, Level nivel) {
        if (!super.matches(cuadricula, nivel)) {
            return false;
        }
        if (!(getResultItem(RegistryAccess.EMPTY).getItem() instanceof LibroHechizo libro)) {
            return true;
        }
        Player jugador = JugadorFabricando.buscar(cuadricula, nivel);
        return jugador != null && libro.hechizo().puedeFabricar(FeDatos.obtenerNivel(jugador));
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModuloFe.RECETA_LIBRO.get();
    }

    /** Lee y escribe la receta reutilizando el lector de recetas sin forma de Minecraft. */
    public static class Serializer implements RecipeSerializer<RecetaLibro> {

        @Override
        public RecetaLibro fromJson(ResourceLocation id, JsonObject json) {
            return new RecetaLibro(RecipeSerializer.SHAPELESS_RECIPE.fromJson(id, json));
        }

        @Override
        public RecetaLibro fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new RecetaLibro(RecipeSerializer.SHAPELESS_RECIPE.fromNetwork(id, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, RecetaLibro receta) {
            RecipeSerializer.SHAPELESS_RECIPE.toNetwork(buffer, receta);
        }
    }
}
