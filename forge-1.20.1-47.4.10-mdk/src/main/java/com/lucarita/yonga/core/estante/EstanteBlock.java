package com.lucarita.yonga.core.estante;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * EL ESTANTE (como el de la 1.21.9 de vanilla). Guarda y enseña hasta 3 montones de objetos.
 *  - Clic derecho en una de sus tres ranuras (por delante): cambia lo que llevas en la mano por lo que hay en esa
 *    ranura (meter, sacar o intercambiar; con la mano vacía, lo coges).
 *  - ACTIVADO con redstone (cambia su textura): sus tres ranuras van juntas y se intercambian de golpe con las 3
 *    ranuras de más a la derecha de tu barra rápida. Hasta 3 estantes activados y seguidos (mirando al mismo lado) se
 *    unen: 2 estantes = las 6 de la derecha, 3 = la barra entera. Da igual en cuál de ellos hagas clic.
 *  - Comparador por detrás: 1 si hay algo a la izquierda, 2 en el centro y 4 a la derecha (sumados, hasta 7).
 *  - Las tolvas meten y sacan objetos. Al romperlo suelta lo que tenga. Se puede anegar.
 * Estados: facing, powered, side_chain (unconnected / left / center / right), waterlogged.
 */
public class EstanteBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<ParteCadena> SIDE_CHAIN = EnumProperty.create("side_chain", ParteCadena.class);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final int MAXIMO_CADENA = 3;

    /** La forma (mirando al norte): la tabla del fondo y los dos listones de arriba y abajo. */
    private static final Map<Direction, VoxelShape> FORMAS = new EnumMap<>(Direction.class);

    static {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            FORMAS.put(d, Shapes.or(caja(d, 0, 12, 11, 16, 16, 13), caja(d, 0, 0, 13, 16, 16, 16), caja(d, 0, 0, 11, 16, 4, 13)));
        }
    }

    /** Una caja definida mirando al norte, girada hacia "d". */
    private static VoxelShape caja(Direction d, double x0, double y0, double z0, double x1, double y1, double z1) {
        return switch (d) {
            case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
            case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
            case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
            default -> Block.box(x0, y0, z0, x1, y1, z1);
        };
    }

    private final boolean inflamable;

    public EstanteBlock(Properties propiedades, boolean inflamable) {
        super(propiedades);
        this.inflamable = inflamable;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false)
                .setValue(SIDE_CHAIN, ParteCadena.UNCONNECTED).setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> constructor) {
        constructor.add(FACING, POWERED, SIDE_CHAIN, WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext contexto) {
        return FORMAS.get(estado.getValue(FACING));
    }

    @Override
    public RenderShape getRenderShape(BlockState estado) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState estado) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext contexto) {
        boolean agua = contexto.getLevel().getFluidState(contexto.getClickedPos()).getType() == Fluids.WATER;
        return this.defaultBlockState()
                .setValue(FACING, contexto.getHorizontalDirection().getOpposite())
                .setValue(POWERED, contexto.getLevel().hasNeighborSignal(contexto.getClickedPos()))
                .setValue(WATERLOGGED, agua);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
        return new EstanteBlockEntity(pos, estado);
    }

    // ---------- USAR ----------

    @Override
    public InteractionResult use(BlockState estado, Level nivel, BlockPos pos, Player jugador, InteractionHand mano, BlockHitResult golpe) {
        if (mano != InteractionHand.MAIN_HAND || golpe.getDirection() != estado.getValue(FACING)
                || !(nivel.getBlockEntity(pos) instanceof EstanteBlockEntity estante)) {
            return InteractionResult.PASS;
        }
        if (estado.getValue(POWERED)) {
            boolean hecho = intercambiarTodo(nivel, pos, estado, jugador.getInventory(), nivel.isClientSide());
            return hecho ? InteractionResult.sidedSuccess(nivel.isClientSide()) : InteractionResult.PASS;
        }
        int ranura = ranura(golpe, pos, estado.getValue(FACING));
        ItemStack enMano = jugador.getItemInHand(mano);
        ItemStack enRanura = estante.getItem(ranura);
        if (enMano.isEmpty() && enRanura.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!nivel.isClientSide()) {
            estante.setItem(ranura, enMano);
            jugador.setItemInHand(mano, enRanura);
            SoundEvent sonido = enRanura.isEmpty() ? Estantes.SONIDO_PONER.get()
                    : enMano.isEmpty() ? Estantes.SONIDO_COGER.get() : Estantes.SONIDO_CAMBIAR.get();
            sonar(nivel, pos, sonido);
            nivel.gameEvent(jugador, GameEvent.BLOCK_CHANGE, pos);
        }
        return InteractionResult.sidedSuccess(nivel.isClientSide());
    }

    /** La ranura (0 izquierda, 1 centro, 2 derecha, vistas de frente) en la que has hecho clic. */
    private static int ranura(BlockHitResult golpe, BlockPos pos, Direction frente) {
        Vec3 rel = golpe.getLocation().subtract(Vec3.atCenterOf(pos));
        Direction izquierda = frente.getClockWise();          // la izquierda de quien lo mira de frente
        double haciaIzquierda = rel.x * izquierda.getStepX() + rel.z * izquierda.getStepZ();
        double desdeIzquierda = 0.5 - haciaIzquierda;          // 0 en el borde izquierdo, 1 en el derecho
        return Mth.clamp(Mth.floor(desdeIzquierda * EstanteBlockEntity.RANURAS), 0, EstanteBlockEntity.RANURAS - 1);
    }

    /**
     * Activado: intercambia todas las ranuras de la cadena (de izquierda a derecha) con las ranuras de más a la
     * derecha de la barra rápida (1 estante = las 3 últimas, 2 = las 6 últimas, 3 = las 9).
     */
    private boolean intercambiarTodo(Level nivel, BlockPos pos, BlockState estado, Inventory inventario, boolean soloComprobar) {
        List<BlockPos> cadena = cadena(nivel, pos, estado);
        int primera = 9 - EstanteBlockEntity.RANURAS * cadena.size();
        boolean algo = false;
        for (int i = 0; i < cadena.size(); i++) {
            if (!(nivel.getBlockEntity(cadena.get(i)) instanceof EstanteBlockEntity estante)) continue;
            for (int r = 0; r < EstanteBlockEntity.RANURAS; r++) {
                int hueco = primera + EstanteBlockEntity.RANURAS * i + r;
                if (hueco < 0 || hueco >= Inventory.getSelectionSize()) continue;
                ItemStack deBarra = inventario.getItem(hueco);
                ItemStack deEstante = estante.getItem(r);
                if (deBarra.isEmpty() && deEstante.isEmpty()) continue;
                algo = true;
                if (!soloComprobar) {
                    inventario.setItem(hueco, deEstante);
                    estante.setItem(r, deBarra);
                }
            }
        }
        if (algo && !soloComprobar) {
            sonar(nivel, pos, Estantes.SONIDO_CAMBIAR_TODO.get());
            nivel.gameEvent(inventario.player, GameEvent.BLOCK_CHANGE, pos);
        }
        return algo;
    }

    /** Los estantes de la cadena de este, de izquierda a derecha (si no está unido, solo él). */
    private static List<BlockPos> cadena(Level nivel, BlockPos pos, BlockState estado) {
        List<BlockPos> lista = new ArrayList<>();
        Direction frente = estado.getValue(FACING);
        Direction izquierda = frente.getClockWise(), derecha = izquierda.getOpposite();
        // hacia la izquierda mientras este no sea el primero de su cadena
        BlockPos p = pos;
        for (int i = 0; i < MAXIMO_CADENA - 1; i++) {
            ParteCadena aqui = parte(nivel, p);
            if (aqui != ParteCadena.CENTER && aqui != ParteCadena.RIGHT) break;
            BlockPos q = p.relative(izquierda);
            if (!unido(nivel, q, frente)) break;
            p = q;
        }
        // y de ahí hacia la derecha hasta el último
        lista.add(p);
        while (lista.size() < MAXIMO_CADENA) {
            ParteCadena aqui = parte(nivel, p);
            if (aqui != ParteCadena.LEFT && aqui != ParteCadena.CENTER) break;
            BlockPos q = p.relative(derecha);
            if (!unido(nivel, q, frente)) break;
            lista.add(q);
            p = q;
        }
        return lista;
    }

    private static ParteCadena parte(Level nivel, BlockPos p) {
        BlockState s = nivel.getBlockState(p);
        return s.getBlock() instanceof EstanteBlock ? s.getValue(SIDE_CHAIN) : ParteCadena.UNCONNECTED;
    }

    /** ¿Hay en "p" un estante activado mirando hacia "frente"? */
    private static boolean unido(Level nivel, BlockPos p, Direction frente) {
        BlockState s = nivel.getBlockState(p);
        return s.getBlock() instanceof EstanteBlock && s.getValue(POWERED) && s.getValue(FACING) == frente;
    }

    // ---------- REDSTONE Y CADENAS ----------

    @Override
    public void neighborChanged(BlockState estado, Level nivel, BlockPos pos, Block bloque, BlockPos desde, boolean movido) {
        if (nivel.isClientSide()) return;
        boolean activado = nivel.hasNeighborSignal(pos);
        if (activado != estado.getValue(POWERED)) {
            nivel.setBlock(pos, estado.setValue(POWERED, activado).setValue(SIDE_CHAIN, ParteCadena.UNCONNECTED), Block.UPDATE_CLIENTS);
            sonar(nivel, pos, activado ? Estantes.SONIDO_ACTIVAR.get() : Estantes.SONIDO_DESACTIVAR.get());
            rehacerCadenas(nivel, pos);
        }
    }

    @Override
    public void onPlace(BlockState estado, Level nivel, BlockPos pos, BlockState antes, boolean movido) {
        if (!antes.is(estado.getBlock()) && !nivel.isClientSide()) {
            rehacerCadenas(nivel, pos);
        }
    }

    @Override
    public void onRemove(BlockState estado, Level nivel, BlockPos pos, BlockState despues, boolean movido) {
        if (!estado.is(despues.getBlock())) {
            if (nivel.getBlockEntity(pos) instanceof EstanteBlockEntity estante) {
                Containers.dropContents(nivel, pos, estante);
                nivel.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(estado, nivel, pos, despues, movido);
            if (!nivel.isClientSide()) {
                Direction izquierda = estado.getValue(FACING).getClockWise();
                rehacerCadenas(nivel, pos.relative(izquierda));
                rehacerCadenas(nivel, pos.relative(izquierda.getOpposite()));
            }
            return;
        }
        super.onRemove(estado, nivel, pos, despues, movido);
    }

    /**
     * Vuelve a unir la fila de estantes activados en la que está "pos": de izquierda a derecha, en grupos de hasta 3
     * (izquierda, centro, derecha). Uno solo queda "unconnected"; los desactivados, siempre "unconnected".
     */
    private static void rehacerCadenas(Level nivel, BlockPos pos) {
        BlockState estado = nivel.getBlockState(pos);
        if (!(estado.getBlock() instanceof EstanteBlock)) return;
        Direction frente = estado.getValue(FACING);
        Direction izquierda = frente.getClockWise(), derecha = izquierda.getOpposite();
        if (!estado.getValue(POWERED)) {
            poner(nivel, pos, ParteCadena.UNCONNECTED);
            if (unido(nivel, pos.relative(izquierda), frente)) rehacerCadenas(nivel, pos.relative(izquierda));
            if (unido(nivel, pos.relative(derecha), frente)) rehacerCadenas(nivel, pos.relative(derecha));
            return;
        }
        BlockPos inicio = pos;
        for (int i = 0; i < 64 && unido(nivel, inicio.relative(izquierda), frente); i++) {
            inicio = inicio.relative(izquierda);
        }
        List<BlockPos> fila = new ArrayList<>();
        for (BlockPos p = inicio; fila.size() < 128 && unido(nivel, p, frente); p = p.relative(derecha)) {
            fila.add(p);
        }
        for (int g = 0; g < fila.size(); g += MAXIMO_CADENA) {
            int tamano = Math.min(MAXIMO_CADENA, fila.size() - g);
            for (int k = 0; k < tamano; k++) {
                ParteCadena p = tamano == 1 ? ParteCadena.UNCONNECTED
                        : k == 0 ? ParteCadena.LEFT : k == tamano - 1 ? ParteCadena.RIGHT : ParteCadena.CENTER;
                poner(nivel, fila.get(g + k), p);
            }
        }
    }

    private static void poner(Level nivel, BlockPos pos, ParteCadena parte) {
        BlockState s = nivel.getBlockState(pos);
        if (s.getBlock() instanceof EstanteBlock && s.getValue(SIDE_CHAIN) != parte) {
            nivel.setBlock(pos, s.setValue(SIDE_CHAIN, parte), Block.UPDATE_CLIENTS);
        }
    }

    private static void sonar(Level nivel, BlockPos pos, SoundEvent sonido) {
        nivel.playSound(null, pos, sonido, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    // ---------- COMPARADOR ----------

    @Override
    public boolean hasAnalogOutputSignal(BlockState estado) {
        return true;
    }

    /** Solo para un comparador puesto detrás del estante (como en vanilla). */
    @Override
    public int getAnalogOutputSignal(BlockState estado, Level nivel, BlockPos pos) {
        Direction atras = estado.getValue(FACING).getOpposite();
        BlockState detras = nivel.getBlockState(pos.relative(atras));
        if (!(detras.getBlock() instanceof ComparatorBlock) || detras.getValue(ComparatorBlock.FACING) != estado.getValue(FACING)) {
            return 0;
        }
        return nivel.getBlockEntity(pos) instanceof EstanteBlockEntity estante ? estante.senal() : 0;
    }

    // ---------- AGUA, GIROS, VARIOS ----------

    @Override
    public FluidState getFluidState(BlockState estado) {
        return estado.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(estado);
    }

    @Override
    public BlockState updateShape(BlockState estado, Direction direccion, BlockState vecino, LevelAccessor nivel, BlockPos pos, BlockPos posVecino) {
        if (estado.getValue(WATERLOGGED)) {
            nivel.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(nivel));
        }
        return super.updateShape(estado, direccion, vecino, nivel, pos, posVecino);
    }

    @Override
    public BlockState rotate(BlockState estado, Rotation giro) {
        return estado.setValue(FACING, giro.rotate(estado.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState estado, Mirror espejo) {
        return estado.rotate(espejo.getRotation(estado.getValue(FACING)));
    }

    @Override
    public boolean isPathfindable(BlockState estado, BlockGetter nivel, BlockPos pos, PathComputationType tipo) {
        return false;
    }

    @Override
    public boolean isFlammable(BlockState estado, BlockGetter nivel, BlockPos pos, Direction cara) {
        return inflamable;
    }

    @Override
    public int getFlammability(BlockState estado, BlockGetter nivel, BlockPos pos, Direction cara) {
        return inflamable ? 20 : 0;
    }

    @Override
    public int getFireSpreadSpeed(BlockState estado, BlockGetter nivel, BlockPos pos, Direction cara) {
        return inflamable ? 30 : 0;
    }
}
