package com.lucarita.yonga.modulos.habitantes.taberna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * El tablón de encargos de la taberna: un panel de corcho con papeles clavado en la pared. Clic derecho: los
 * encargos que tiene colgados el tabernero de esa aldea.
 */
public class TablonBlock extends HorizontalDirectionalBlock {

    private static final VoxelShape NORTE = Block.box(0, 2, 14, 16, 14, 16);
    private static final VoxelShape SUR = Block.box(0, 2, 0, 16, 14, 2);
    private static final VoxelShape ESTE = Block.box(0, 2, 0, 2, 14, 16);
    private static final VoxelShape OESTE = Block.box(14, 2, 0, 16, 14, 16);

    public TablonBlock(Properties propiedades) {
        super(propiedades);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> constructor) {
        constructor.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext contexto) {
        Direction cara = contexto.getClickedFace();
        return this.defaultBlockState().setValue(FACING,
                cara.getAxis().isHorizontal() ? cara : contexto.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext contexto) {
        return switch (estado.getValue(FACING)) {
            case SOUTH -> SUR;
            case EAST -> ESTE;
            case WEST -> OESTE;
            default -> NORTE;
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState estado, Level nivel, BlockPos pos, Player jugador, InteractionHand mano,
                                 BlockHitResult golpe) {
        if (!nivel.isClientSide && jugador instanceof ServerPlayer servidor) {
            Taberna.abrirDesdeTablon(servidor, pos);
        }
        return InteractionResult.sidedSuccess(nivel.isClientSide);
    }
}
