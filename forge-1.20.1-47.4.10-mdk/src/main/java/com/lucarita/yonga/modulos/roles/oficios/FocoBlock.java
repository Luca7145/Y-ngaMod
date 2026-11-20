package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.modulos.roles.ModuloRoles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * FOCO (Ingeniero, Aprendiz). Se pone mirando hacia donde apuntas (también arriba o abajo) e ilumina un cono largo
 * en esa dirección: hasta 28 bloques, cada vez más ancho. Para la luz pone bloques de luz invisibles (los de Minecraft)
 * en el aire del cono, solo donde le llega la luz desde el foco (las paredes hacen sombra), y los quita al apagarlo o
 * romperlo. Cada 2 segundos repasa el cono por si algo ha cambiado. Click derecho con la mano vacía: encender/apagar.
 */
public class FocoBlock extends BaseEntityBlock {

    public static final DirectionProperty MIRA = BlockStateProperties.FACING;
    public static final BooleanProperty ENCENDIDO = BlockStateProperties.LIT;

    private static final int ALCANCE = 28;
    private static final int PASO = 4;                     // cada cuántos bloques del eje pone luz
    private static final int NIVEL_LUZ = 15;

    public FocoBlock(Properties propiedades) {
        super(propiedades);
        registerDefaultState(stateDefinition.any().setValue(MIRA, Direction.NORTH).setValue(ENCENDIDO, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> definicion) {
        definicion.add(MIRA, ENCENDIDO);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext contexto) {
        return defaultBlockState().setValue(MIRA, contexto.getNearestLookingDirection());
    }

    @Override
    public BlockState rotate(BlockState estado, Rotation giro) {
        return estado.setValue(MIRA, giro.rotate(estado.getValue(MIRA)));
    }

    @Override
    public BlockState mirror(BlockState estado, Mirror espejo) {
        return estado.rotate(espejo.getRotation(estado.getValue(MIRA)));
    }

    @Override
    public RenderShape getRenderShape(BlockState estado) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState estado, Level nivel, BlockPos pos, Player jugador, InteractionHand mano, BlockHitResult golpe) {
        if (!jugador.getItemInHand(mano).isEmpty() || mano != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!nivel.isClientSide()) {
            boolean ahora = !estado.getValue(ENCENDIDO);
            nivel.setBlock(pos, estado.setValue(ENCENDIDO, ahora), 3);
            nivel.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5f, ahora ? 1.4f : 1.0f);
            if (nivel.getBlockEntity(pos) instanceof Entidad entidad) {
                entidad.repasar(nivel, pos, nivel.getBlockState(pos));
            }
        }
        return InteractionResult.sidedSuccess(nivel.isClientSide());
    }

    @Override
    public void onRemove(BlockState estado, Level nivel, BlockPos pos, BlockState nuevo, boolean moviendo) {
        if (!estado.is(nuevo.getBlock()) && nivel.getBlockEntity(pos) instanceof Entidad entidad) {
            entidad.quitarLuces(nivel);
        }
        super.onRemove(estado, nivel, pos, nuevo, moviendo);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
        return new Entidad(pos, estado);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level nivel, BlockState estado, BlockEntityType<T> tipo) {
        return nivel.isClientSide() ? null : createTickerHelper(tipo, ModuloRoles.ENTIDAD_FOCO.get(),
                (n, p, e, entidad) -> {
                    if ((n.getGameTime() + p.asLong()) % 40 == 0) entidad.repasar(n, p, e);
                });
    }

    // ---------- ENTIDAD DE BLOQUE: las luces que ha puesto ----------

    public static class Entidad extends BlockEntity {

        private final Set<BlockPos> luces = new HashSet<>();

        public Entidad(BlockPos pos, BlockState estado) {
            super(ModuloRoles.ENTIDAD_FOCO.get(), pos, estado);
        }

        /** Calcula dónde tiene que haber luz y pone o quita las que hagan falta. */
        void repasar(Level nivel, BlockPos pos, BlockState estado) {
            Set<BlockPos> nuevas = new HashSet<>();
            if (estado.getBlock() instanceof FocoBlock && estado.getValue(ENCENDIDO)) {
                for (BlockPos punto : cono(pos, estado.getValue(MIRA))) {
                    if (seVe(nivel, pos, punto) && (luces.contains(punto) || nivel.getBlockState(punto).isAir())) {
                        nuevas.add(punto);
                    }
                }
            }
            for (BlockPos vieja : luces) {
                if (!nuevas.contains(vieja) && nivel.getBlockState(vieja).is(Blocks.LIGHT)) {
                    nivel.setBlock(vieja, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            BlockState luz = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, NIVEL_LUZ);
            for (BlockPos punto : nuevas) {
                if (nivel.getBlockState(punto).isAir()) {
                    nivel.setBlock(punto, luz, 3);
                }
            }
            if (!nuevas.equals(luces)) {
                luces.clear();
                luces.addAll(nuevas);
                setChanged();
            }
        }

        void quitarLuces(Level nivel) {
            for (BlockPos punto : luces) {
                if (nivel.getBlockState(punto).is(Blocks.LIGHT)) {
                    nivel.setBlock(punto, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            luces.clear();
        }

        /** Los puntos del cono: en el eje cada 4 bloques y, según se aleja, también a los lados (cada vez más abierto). */
        private static List<BlockPos> cono(BlockPos origen, Direction mira) {
            List<BlockPos> puntos = new ArrayList<>();
            Direction[] lados = perpendiculares(mira);
            for (int paso = 2; paso <= ALCANCE; paso += PASO) {
                BlockPos centro = origen.relative(mira, paso);
                puntos.add(centro);
                int abertura = Math.round(paso * 0.22f);
                if (abertura >= 2) {
                    for (Direction lado : lados) {
                        puntos.add(centro.relative(lado, abertura));
                    }
                }
            }
            return puntos;
        }

        private static Direction[] perpendiculares(Direction mira) {
            List<Direction> lista = new ArrayList<>();
            for (Direction d : Direction.values()) {
                if (d.getAxis() != mira.getAxis()) lista.add(d);
            }
            return lista.toArray(new Direction[0]);
        }

        /** ¿Llega la luz del foco hasta ese punto? La paran los bloques opacos (el cristal o las hojas, no). */
        private static boolean seVe(Level nivel, BlockPos foco, BlockPos punto) {
            Vec3 desde = Vec3.atCenterOf(foco);
            Vec3 hacia = Vec3.atCenterOf(punto).subtract(desde);
            int pasos = (int) Math.ceil(hacia.length() * 3);
            BlockPos.MutableBlockPos mirando = new BlockPos.MutableBlockPos();
            for (int i = 1; i < pasos; i++) {
                Vec3 p = desde.add(hacia.scale((double) i / pasos));
                mirando.set(p.x, p.y, p.z);
                if (mirando.equals(foco) || mirando.equals(punto)) continue;
                if (nivel.getBlockState(mirando).canOcclude()) return false;
            }
            return true;
        }

        @Override
        protected void saveAdditional(CompoundTag nbt) {
            super.saveAdditional(nbt);
            ListTag lista = new ListTag();
            for (BlockPos punto : luces) lista.add(LongTag.valueOf(punto.asLong()));
            nbt.put("Luces", lista);
        }

        @Override
        public void load(CompoundTag nbt) {
            super.load(nbt);
            luces.clear();
            for (Tag t : nbt.getList("Luces", Tag.TAG_LONG)) {
                luces.add(BlockPos.of(((LongTag) t).getAsLong()));
            }
        }
    }
}
