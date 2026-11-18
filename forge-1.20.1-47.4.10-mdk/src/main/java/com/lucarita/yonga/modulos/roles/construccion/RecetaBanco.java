package com.lucarita.yonga.modulos.roles.construccion;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.lucarita.yonga.modulos.roles.ModuloRoles;
import com.lucarita.yonga.modulos.roles.RangoRol;
import com.lucarita.yonga.modulos.roles.Rol;
import com.lucarita.yonga.modulos.roles.RolCliente;
import com.lucarita.yonga.modulos.roles.RolDatos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Receta del banco de ingeniero (como las del cortapiedras: un bloque entra y sale otro).
 *
 * JSON:
 *   { "type": "yonga:banco_ingeniero", "ingredient": {"item": "minecraft:stone"},
 *     "result": "yonga:adoquin_calle", "count": 1, "rango": "todos" }
 *
 * "rango": "todos" = cualquier jugador puede hacerlo (los diseños básicos);
 *          "novato", "aprendiz"... = solo un Ingeniero de ese rango o más.
 */
public class RecetaBanco extends SingleItemRecipe {

    @Nullable
    private final RangoRol rango;   // null = todos

    public RecetaBanco(ResourceLocation id, String grupo, Ingredient ingrediente, ItemStack resultado, @Nullable RangoRol rango) {
        super(ModuloRoles.TIPO_BANCO.get(), ModuloRoles.RECETA_BANCO.get(), id, grupo, ingrediente, resultado);
        this.rango = rango;
    }

    @Override
    public boolean matches(Container contenedor, Level nivel) {
        return this.ingredient.test(contenedor.getItem(0));
    }

    /** ¿Puede este jugador hacer este diseño? (en el cliente mira su propio oficio, que le manda el servidor) */
    public boolean puedeUsar(Player jugador) {
        if (rango == null) {
            return true;
        }
        Rol rol = jugador.level().isClientSide() ? RolCliente.rol() : RolDatos.obtenerRol(jugador);
        RangoRol suRango = jugador.level().isClientSide() ? RolCliente.rango() : RolDatos.obtenerRango(jugador);
        return rol == Rol.INGENIERO && suRango.ordinal() >= rango.ordinal();
    }

    public ItemStack resultado(RegistryAccess registros) {
        return getResultItem(registros);
    }

    // Que no salga en el libro de recetas (no tiene pestaña)
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModuloRoles.BANCO_INGENIERO_ITEM.get());
    }

    public static class Serializer implements RecipeSerializer<RecetaBanco> {

        @Override
        public RecetaBanco fromJson(ResourceLocation id, JsonObject json) {
            String grupo = GsonHelper.getAsString(json, "group", "");
            Ingredient ingrediente = Ingredient.fromJson(GsonHelper.isArrayNode(json, "ingredient")
                    ? GsonHelper.getAsJsonArray(json, "ingredient") : GsonHelper.getAsJsonObject(json, "ingredient"));
            String idResultado = GsonHelper.getAsString(json, "result");
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(idResultado));
            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                throw new JsonSyntaxException("Objeto desconocido en la receta " + id + ": " + idResultado);
            }
            ItemStack resultado = new ItemStack(item, GsonHelper.getAsInt(json, "count", 1));
            String idRango = GsonHelper.getAsString(json, "rango", "todos");
            RangoRol rango = null;
            if (!idRango.equals("todos")) {
                rango = RangoRol.porId(idRango);
                if (rango == null) {
                    throw new JsonSyntaxException("Rango desconocido en la receta " + id + ": " + idRango);
                }
            }
            return new RecetaBanco(id, grupo, ingrediente, resultado, rango);
        }

        @Override
        public RecetaBanco fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            String grupo = buffer.readUtf();
            Ingredient ingrediente = Ingredient.fromNetwork(buffer);
            ItemStack resultado = buffer.readItem();
            String idRango = buffer.readUtf();
            return new RecetaBanco(id, grupo, ingrediente, resultado, idRango.isEmpty() ? null : RangoRol.porId(idRango));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, RecetaBanco receta) {
            buffer.writeUtf(receta.group);
            receta.ingredient.toNetwork(buffer);
            buffer.writeItem(receta.result);
            buffer.writeUtf(receta.rango == null ? "" : receta.rango.id());
        }
    }
}
