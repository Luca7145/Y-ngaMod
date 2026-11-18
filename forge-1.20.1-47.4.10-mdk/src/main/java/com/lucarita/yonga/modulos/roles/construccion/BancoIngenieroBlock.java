package com.lucarita.yonga.modulos.roles.construccion;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** El banco de ingeniero: click derecho abre su menú (BancoMenu). Lo puede usar cualquiera. */
public class BancoIngenieroBlock extends Block {

    public BancoIngenieroBlock(Properties propiedades) {
        super(propiedades);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState estado, Level nivel, BlockPos pos, Player jugador, InteractionHand mano, BlockHitResult golpe) {
        if (!nivel.isClientSide()) {
            jugador.openMenu(new SimpleMenuProvider(
                    (id, inventario, quien) -> new BancoMenu(id, inventario, ContainerLevelAccess.create(nivel, pos)),
                    Component.translatable("block.yonga.banco_ingeniero")));
        }
        return InteractionResult.sidedSuccess(nivel.isClientSide());
    }
}
