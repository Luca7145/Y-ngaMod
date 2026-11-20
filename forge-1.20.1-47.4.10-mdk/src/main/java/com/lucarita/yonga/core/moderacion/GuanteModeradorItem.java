package com.lucarita.yonga.core.moderacion;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * GUANTE DEL MODERADOR. Solo se saca en creativo (no tiene receta) y solo funciona en manos de un operador (permiso 2)
 * o de alguien en creativo, por si acaba en manos de quien no debe. En la mano no se ve (su modelo lo esconde en
 * primera y tercera persona); en el inventario es un guante.
 *
 * Click derecho sobre otro jugador: abre su inventario como un cofre de 5 filas (ver InventarioAjenoMenu) y se puede
 * hacer de todo: sacar, meter y mover objetos, también la armadura y la mano secundaria. Los cambios se ven al momento
 * en los dos lados.
 */
public class GuanteModeradorItem extends Item {

    public GuanteModeradorItem(Properties propiedades) {
        super(propiedades.stacksTo(1).rarity(Rarity.EPIC));
    }

    public static boolean puedeUsarlo(Player jugador) {
        return jugador.hasPermissions(2) || jugador.isCreative();
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack guante, Player moderador, LivingEntity objetivo, InteractionHand mano) {
        if (!(objetivo instanceof Player otro) || otro == moderador || !puedeUsarlo(moderador)) {
            return InteractionResult.PASS;
        }
        if (moderador instanceof ServerPlayer servidor && otro instanceof ServerPlayer jugador) {
            servidor.openMenu(new SimpleMenuProvider(
                    (id, inventario, quien) -> new InventarioAjenoMenu(id, inventario, jugador),
                    Component.translatable("yonga.guante.inventario_de", jugador.getDisplayName())));
        }
        return InteractionResult.sidedSuccess(moderador.level().isClientSide());
    }
}
