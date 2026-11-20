package com.lucarita.yonga.modulos.roles.oficios;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * TRAMPA DE PINCHOS (Ingeniero, Oficial). Un bloque que se pone en el suelo. Con señal de redstone saca los pinchos
 * (y al salir, pincha a lo que haya encima); sin señal los recoge. Con los pinchos fuera, lo que esté encima recibe
 * 3 de daño cada medio segundo (como mucho) y se mueve más despacio. A los objetos sueltos no les hace nada.
 */
public class TrampaPinchosBlock extends Block {

    public static final BooleanProperty FUERA = BooleanProperty.create("fuera");

    private static final float DANO_AL_SALIR = 5.0f;
    private static final float DANO = 3.0f;

    public TrampaPinchosBlock(Properties propiedades) {
        super(propiedades);
        registerDefaultState(stateDefinition.any().setValue(FUERA, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> definicion) {
        definicion.add(FUERA);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext contexto) {
        return defaultBlockState().setValue(FUERA, contexto.getLevel().hasNeighborSignal(contexto.getClickedPos()));
    }

    @Override
    public void neighborChanged(BlockState estado, Level nivel, BlockPos pos, Block vecino, BlockPos posVecino, boolean moviendo) {
        if (nivel.isClientSide()) return;
        boolean senal = nivel.hasNeighborSignal(pos);
        if (senal == estado.getValue(FUERA)) return;
        nivel.setBlock(pos, estado.setValue(FUERA, senal), 3);
        nivel.playSound(null, pos, senal ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.6f,
                senal ? 1.6f : 1.4f);
        if (senal) {
            for (LivingEntity victima : nivel.getEntitiesOfClass(LivingEntity.class, encima(pos))) {
                victima.hurt(nivel.damageSources().cactus(), DANO_AL_SALIR);
            }
        }
    }

    @Override
    public void stepOn(Level nivel, BlockPos pos, BlockState estado, Entity entidad) {
        if (estado.getValue(FUERA) && entidad instanceof LivingEntity victima) {
            victima.makeStuckInBlock(estado, new net.minecraft.world.phys.Vec3(0.6, 1.0, 0.6));
            if (!nivel.isClientSide() && victima.tickCount % 10 == 0) {
                victima.hurt(nivel.damageSources().cactus(), DANO);
            }
        }
        super.stepOn(nivel, pos, estado, entidad);
    }

    // Con los pinchos fuera, de vez en cuando brillan un poco
    @Override
    public void animateTick(BlockState estado, Level nivel, BlockPos pos, RandomSource azar) {
        if (estado.getValue(FUERA) && azar.nextInt(12) == 0) {
            nivel.addParticle(net.minecraft.core.particles.ParticleTypes.CRIT, pos.getX() + azar.nextDouble(), pos.getY() + 1.3,
                    pos.getZ() + azar.nextDouble(), 0, 0.02, 0);
        }
    }

    private static AABB encima(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
    }
}
