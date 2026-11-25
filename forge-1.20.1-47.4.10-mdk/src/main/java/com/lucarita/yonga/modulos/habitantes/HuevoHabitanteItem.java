package com.lucarita.yonga.modulos.habitantes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.common.ForgeSpawnEggItem;

/**
 * Huevo de habitante de un sexo fijo (para pruebas): el habitante sale con edad, aspecto y oficio al azar, pero
 * siempre hombre o siempre mujer. Pone una marca en los datos de la entidad que Habitante lee al aparecer.
 */
public class HuevoHabitanteItem extends ForgeSpawnEggItem {

    public static final String MARCA_SEXO = "YongaMujer";

    private final boolean mujer;

    public HuevoHabitanteItem(boolean mujer, int fondo, int manchas, Properties propiedades) {
        super(ModuloHabitantes.HABITANTE, fondo, manchas, propiedades);
        this.mujer = mujer;
    }

    @Override
    public InteractionResult useOn(UseOnContext contexto) {
        ItemStack huevo = contexto.getItemInHand();
        boolean tenia = huevo.getTag() != null && huevo.getTag().contains("EntityTag");
        CompoundTag datos = huevo.getOrCreateTagElement("EntityTag");
        datos.putBoolean(MARCA_SEXO, this.mujer);
        InteractionResult resultado = super.useOn(contexto);
        // Se quita la marca para que el huevo siga siendo igual que los demás (y se apile con ellos)
        if (!tenia && !huevo.isEmpty()) {
            huevo.removeTagKey("EntityTag");
            if (huevo.getTag() != null && huevo.getTag().isEmpty()) huevo.setTag(null);
        } else if (!huevo.isEmpty()) {
            datos.remove(MARCA_SEXO);
        }
        return resultado;
    }
}
