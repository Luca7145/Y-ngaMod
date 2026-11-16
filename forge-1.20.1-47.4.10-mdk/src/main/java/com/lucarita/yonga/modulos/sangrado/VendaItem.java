package com.lucarita.yonga.modulos.sangrado;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class VendaItem extends Item {

    // Vida que recupera al usarla: 4 puntos = 2 corazones
    private static final float CURACION = 4.0f;

    public VendaItem(Properties properties) {
        super(properties);
    }

    // Qué ocurre cuando el jugador termina de usar el ítem
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        // Solo lo ejecutamos en el servidor
        if (!level.isClientSide()) {
            // Elimina el efecto de sangrado, sin importar su nivel o duración
            entity.removeEffect(ModuloSangrado.SANGRADO.get());

            // Cura 2 corazones (cada corazón son 2 puntos de vida)
            entity.heal(CURACION);
        }

        // Consumir el ítem (restar 1 a la cantidad)
        return super.finishUsingItem(stack, level, entity);
    }

    // Opcional: Cambia la animación para que no parezca que te estás "comiendo" las vendas
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW; // Hace la animación de levantar el objeto como un arco/catalejo
    }

    // Cuánto tiempo tarda en usarse (32 es el tiempo estándar de la comida)
    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }
}