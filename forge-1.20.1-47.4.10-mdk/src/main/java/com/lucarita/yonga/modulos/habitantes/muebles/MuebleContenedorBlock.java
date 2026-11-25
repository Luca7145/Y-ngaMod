package com.lucarita.yonga.modulos.habitantes.muebles;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Un mueble en el que se guardan cosas (vasija, ánfora, cesto, escritorio, cómoda, nevera) o el gramófono. */
public class MuebleContenedorBlock extends MuebleBlock implements EntityBlock {

    public MuebleContenedorBlock(Properties propiedades, Muebles.Config config) {
        super(propiedades, config);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
        return new MuebleBlockEntity(pos, estado);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level nivel, BlockState estado, BlockEntityType<T> tipo) {
        if (nivel.isClientSide || !config.accion().equals("nevera") || tipo != Muebles.ENTIDAD.get()) return null;
        return (n, p, e, be) -> MuebleBlockEntity.enfriar(n, p, (MuebleBlockEntity) be);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState estado, Level nivel, BlockPos pos, Player jugador, InteractionHand mano, BlockHitResult golpe) {
        if (config.accion().equals("gramofono")) {
            return super.use(estado, nivel, pos, jugador, mano, golpe);
        }
        if (nivel.isClientSide) return InteractionResult.SUCCESS;
        if (nivel.getBlockEntity(pos) instanceof MuebleBlockEntity be) {
            jugador.openMenu(be);
            SoundEvent sonido = switch (config.accion()) {
                case "nevera" -> SoundEvents.IRON_TRAPDOOR_OPEN;
                default -> SoundEvents.BARREL_OPEN;
            };
            nivel.playSound(null, pos, sonido, SoundSource.BLOCKS, 0.6f, 1.0f);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState estado, Level nivel, BlockPos pos, BlockState nuevo, boolean movido) {
        if (!estado.is(nuevo.getBlock())) {
            if (nivel.getBlockEntity(pos) instanceof MuebleBlockEntity be) {
                if (config.accion().equals("gramofono")) be.parar();
                Containers.dropContents(nivel, pos, be);
            }
        }
        super.onRemove(estado, nivel, pos, nuevo, movido);
    }
}
