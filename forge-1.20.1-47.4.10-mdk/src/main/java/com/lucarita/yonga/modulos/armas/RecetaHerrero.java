package com.lucarita.yonga.modulos.armas;

import com.google.gson.JsonObject;
import com.lucarita.yonga.core.JugadorFabricando;
import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.roles.Rol;
import com.lucarita.yonga.modulos.roles.RolDatos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * Receta de arma "yonga:arma_herrero": igual que una receta con forma normal, pero SOLO funciona si quien
 * fabrica es un Herrero con, al menos, el rango que pide el arma (ArmaYonga.rangoHerrero()).
 * Un Maestro puede fabricar todas; un Novato, solo el puñal. Nadie más puede fabricarlas.
 *
 * Si el módulo de roles está desactivado, funciona como una receta normal (cualquiera puede).
 *
 * Quién está fabricando lo averigua JugadorFabricando.
 */
public class RecetaHerrero extends ShapedRecipe {

    public RecetaHerrero(ShapedRecipe base) {
        super(base.getId(), base.getGroup(), base.category(), base.getWidth(), base.getHeight(),
                base.getIngredients(), base.getResultItem(RegistryAccess.EMPTY));
    }

    @Override
    public boolean matches(CraftingContainer cuadricula, Level nivel) {
        // Primero: ¿están los ingredientes bien colocados?
        if (!super.matches(cuadricula, nivel)) {
            return false;
        }
        if (!Modulo.ROLES.activo()) {
            return true;
        }
        // Segundo: ¿quien fabrica es un Herrero con rango suficiente?
        Player jugador = JugadorFabricando.buscar(cuadricula, nivel);
        return jugador != null && puedeFabricar(jugador);
    }

    private boolean puedeFabricar(Player jugador) {
        if (!(getResultItem(RegistryAccess.EMPTY).getItem() instanceof ArmaYonga arma)) {
            return true;
        }
        return RolDatos.obtenerRol(jugador) == Rol.HERRERO
                && RolDatos.obtenerRango(jugador).ordinal() >= arma.rangoHerrero().ordinal();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModuloArmas.RECETA_HERRERO.get();
    }

    /** Lee y escribe la receta reutilizando el lector de recetas con forma de Minecraft. */
    public static class Serializer implements RecipeSerializer<RecetaHerrero> {

        @Override
        public RecetaHerrero fromJson(ResourceLocation id, JsonObject json) {
            return new RecetaHerrero(RecipeSerializer.SHAPED_RECIPE.fromJson(id, json));
        }

        @Override
        public RecetaHerrero fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new RecetaHerrero(RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, RecetaHerrero receta) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buffer, receta);
        }
    }
}
