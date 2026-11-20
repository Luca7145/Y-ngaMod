package com.lucarita.yonga.modulos.roles.oficios;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import org.jetbrains.annotations.Nullable;

/**
 * SACO DE DORMIR (Explorador, desde Aprendiz). Se pone y se duerme en él como en una cama (de noche o con tormenta,
 * sin monstruos cerca, y pasa la noche), pero:
 *  - NO guarda el punto de aparición: puedes dormir de camino sin perder tu cama de casa.
 *  - Es más cómodo: si duermes la noche entera te levantas descansado (regeneración 30 s y algo de saturación).
 *  - Es bajito y no rebota. En el Nether y el End no se puede usar (pero tampoco explota).
 */
public class SacoDormirBlock extends BedBlock {

    private static final VoxelShape FORMA = Block.box(0, 0, 0, 16, 7, 16);

    public SacoDormirBlock(Properties propiedades) {
        super(DyeColor.GREEN, propiedades);
    }

    @Override
    public RenderShape getRenderShape(BlockState estado) {
        return RenderShape.MODEL;               // modelo normal, no el de la cama de vanilla
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
        return null;
    }

    @Override
    public VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext contexto) {
        return FORMA;
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter nivel, Entity entidad) {
        entidad.setDeltaMovement(entidad.getDeltaMovement().multiply(1.0, 0.0, 1.0));     // no rebota
    }

    @Override
    public InteractionResult use(BlockState estado, Level nivel, BlockPos pos, Player jugador, InteractionHand mano, BlockHitResult golpe) {
        if (!nivel.dimensionType().bedWorks()) {
            if (!nivel.isClientSide()) {
                jugador.displayClientMessage(Component.translatable("yonga.saco.aqui_no").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.sidedSuccess(nivel.isClientSide());
        }
        return super.use(estado, nivel, pos, jugador, mano, golpe);
    }

    // ---------- EVENTOS (se registran en ModuloRoles) ----------

    /** Dormir en el saco no cambia el punto de aparición. */
    public static void alGuardarAparicion(PlayerSetSpawnEvent evento) {
        BlockPos pos = evento.getNewSpawn();
        if (pos == null || evento.isForced() || !(evento.getEntity().level() instanceof ServerLevel nivel)) return;
        ServerLevel destino = nivel.getServer().getLevel(evento.getSpawnLevel());
        if (destino != null && destino.getBlockState(pos).getBlock() instanceof SacoDormirBlock) {
            evento.setCanceled(true);
        }
    }

    /** Tras dormir la noche entera en el saco, te levantas descansado. */
    public static void alDespertar(PlayerWakeUpEvent evento) {
        Player jugador = evento.getEntity();
        if (evento.wakeImmediately() || jugador.level().isClientSide()) return;
        boolean enSaco = jugador.getSleepingPos().map(p -> jugador.level().getBlockState(p).getBlock() instanceof SacoDormirBlock).orElse(false);
        if (enSaco && jugador.level().isDay()) {
            jugador.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0));
            jugador.addEffect(new MobEffectInstance(MobEffects.SATURATION, 2, 0));
        }
    }
}
