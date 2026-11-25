package com.lucarita.yonga.modulos.habitantes.muebles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Un mueble o adorno: mira hacia quien lo pone (su frente) y tiene la forma de sus piezas.
 * La forma se da mirando al norte (en píxeles: x1, y1, z1, x2, y2, z2 por pieza) y se gira sola.
 * Los de pared (tapiz, cuadro, astas…) tienen la espalda en z = 16: al ponerlos en una pared, se pegan a ella.
 * Los que dan luz tienen "lit" (se encienden y apagan); los de dos alturas, "half" (como las puertas).
 *
 * Acciones (clic derecho): asiento → te sientas; reloj → la hora; globo → dónde estás; moler → piedra de moler;
 * hilar → rueca; coser → máquina de coser; gramofono → pone y quita discos; tele / ordenador / lampara → se encienden.
 */
public class MuebleBlock extends HorizontalDirectionalBlock {

    public static final BooleanProperty ENCENDIDO = BlockStateProperties.LIT;
    public static final EnumProperty<DoubleBlockHalf> MITAD = BlockStateProperties.DOUBLE_BLOCK_HALF;

    /** El constructor de Block crea los estados antes de que este tenga sus campos: se le pasa la config por aquí. */
    private static final ThreadLocal<Muebles.Config> PREPARANDO = new ThreadLocal<>();

    protected final Muebles.Config config;
    private final Map<Direction, VoxelShape> formas = new EnumMap<>(Direction.class);
    private final Map<Direction, VoxelShape> formasArriba = new EnumMap<>(Direction.class);

    public MuebleBlock(Properties propiedades, Muebles.Config config) {
        super(preparar(propiedades, config));
        PREPARANDO.remove();
        this.config = config;
        BlockState estado = stateDefinition.any().setValue(FACING, Direction.NORTH);
        if (config.encendible()) estado = estado.setValue(ENCENDIDO, true);
        if (config.alto()) estado = estado.setValue(MITAD, DoubleBlockHalf.LOWER);
        registerDefaultState(estado);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            formas.put(d, girar(config.forma(), d));
            if (config.alto()) formasArriba.put(d, girar(config.formaArriba(), d));
        }
    }

    private static Properties preparar(Properties propiedades, Muebles.Config config) {
        PREPARANDO.set(config);
        return propiedades;
    }

    private static VoxelShape girar(double[][] piezas, Direction d) {
        VoxelShape forma = Shapes.empty();
        int vueltas = switch (d) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
        for (double[] p : piezas) {
            double x1 = p[0], z1 = p[2], x2 = p[3], z2 = p[5];
            for (int i = 0; i < vueltas; i++) {          // un cuarto de vuelta, como las agujas del reloj desde arriba
                double nx1 = 16 - z2, nx2 = 16 - z1, nz1 = x1, nz2 = x2;
                x1 = nx1; x2 = nx2; z1 = nz1; z2 = nz2;
            }
            forma = Shapes.or(forma, Block.box(x1, p[1], z1, x2, p[4], z2));
        }
        return forma.optimize();
    }

    public Muebles.Config config() {
        return config;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        Muebles.Config c = PREPARANDO.get();
        if (c != null && c.encendible()) builder.add(ENCENDIDO);
        if (c != null && c.alto()) builder.add(MITAD);
    }

    // ------------------------------------------------------------------ colocar y quitar

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext contexto) {
        if (config.alto() && !contexto.getLevel().getBlockState(contexto.getClickedPos().above()).canBeReplaced(contexto)) {
            return null;
        }
        Direction cara = contexto.getClickedFace();
        if (config.pared() && cara.getAxis().isHorizontal()) {
            return defaultBlockState().setValue(FACING, cara);   // pegado a la pared que has pinchado
        }
        return defaultBlockState().setValue(FACING, contexto.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level nivel, BlockPos pos, BlockState estado, @Nullable LivingEntity quien, ItemStack objeto) {
        if (config.alto()) {
            nivel.setBlock(pos.above(), estado.setValue(MITAD, DoubleBlockHalf.UPPER), 3);
        }
    }

    @Override
    public BlockState updateShape(BlockState estado, Direction lado, BlockState vecino, LevelAccessor nivel, BlockPos pos, BlockPos posVecino) {
        if (config.alto()) {
            boolean abajo = estado.getValue(MITAD) == DoubleBlockHalf.LOWER;
            if (lado == (abajo ? Direction.UP : Direction.DOWN)
                    && !(vecino.is(this) && vecino.getValue(MITAD) != estado.getValue(MITAD))) {
                return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();   // se ha roto la otra mitad
            }
        }
        return super.updateShape(estado, lado, vecino, nivel, pos, posVecino);
    }

    /** Para que las casas giradas de las aldeas giren también sus muebles. */
    @Override
    @SuppressWarnings("deprecation")
    public BlockState rotate(BlockState estado, net.minecraft.world.level.block.Rotation giro) {
        return estado.setValue(FACING, giro.rotate(estado.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState estado, net.minecraft.world.level.block.Mirror espejo) {
        return estado.rotate(espejo.getRotation(estado.getValue(FACING)));
    }

    @Override
    public boolean canSurvive(BlockState estado, LevelReader nivel, BlockPos pos) {
        return true;
    }

    @Override
    public VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext contexto) {
        if (config.alto() && estado.getValue(MITAD) == DoubleBlockHalf.UPPER) {
            return formasArriba.get(estado.getValue(FACING));
        }
        return formas.get(estado.getValue(FACING));
    }

    @Override
    public boolean propagatesSkylightDown(BlockState estado, BlockGetter nivel, BlockPos pos) {
        return true;
    }

    // ------------------------------------------------------------------ usarlo

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState estado, Level nivel, BlockPos pos, Player jugador, InteractionHand mano, BlockHitResult golpe) {
        ItemStack enMano = jugador.getItemInHand(mano);
        String accion = config.accion();

        if (config.asiento() >= 0 && !jugador.isShiftKeyDown() && !jugador.isPassenger()) {
            if (nivel.isClientSide) return InteractionResult.SUCCESS;
            return Asiento.sentar(nivel, pos, estado, config.asiento(), jugador) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }

        switch (accion) {
            case "reloj" -> {
                if (!nivel.isClientSide) {
                    long t = nivel.getDayTime() % 24000L;
                    int horas = (int) ((t / 1000 + 6) % 24), minutos = (int) ((t % 1000) * 60 / 1000);
                    jugador.displayClientMessage(Component.translatable("yonga.mueble.reloj",
                            String.format("%02d:%02d", horas, minutos)), true);
                    nivel.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4f, 1.6f);
                }
                return InteractionResult.sidedSuccess(nivel.isClientSide);
            }
            case "globo" -> {
                if (!nivel.isClientSide) {
                    Holder<Biome> bioma = nivel.getBiome(jugador.blockPosition());
                    Component nombre = bioma.unwrapKey().map(k -> (Component) Component.translatable(
                            "biome." + k.location().getNamespace() + "." + k.location().getPath())).orElse(Component.literal("?"));
                    jugador.displayClientMessage(Component.translatable("yonga.mueble.globo", nombre,
                            jugador.blockPosition().getX(), jugador.blockPosition().getZ()), true);
                    nivel.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8f, 1.2f);
                }
                return InteractionResult.sidedSuccess(nivel.isClientSide);
            }
            case "moler" -> {
                ItemStack sale = moler(enMano);
                return transformar(nivel, pos, jugador, enMano, sale, SoundEvents.GRINDSTONE_USE, "yonga.mueble.moler.nada");
            }
            case "hilar" -> {
                ItemStack sale = enMano.is(ItemTags.WOOL) ? new ItemStack(Items.STRING, 4) : ItemStack.EMPTY;
                return transformar(nivel, pos, jugador, enMano, sale, SoundEvents.UI_LOOM_TAKE_RESULT, "yonga.mueble.hilar.nada");
            }
            case "coser" -> {
                return coser(nivel, pos, jugador, enMano);
            }
            case "gramofono" -> {
                return gramofono(nivel, pos, jugador, enMano);
            }
            default -> { }
        }

        if (config.encendible()) {
            if (!nivel.isClientSide) {
                boolean ahora = !estado.getValue(ENCENDIDO);
                nivel.setBlock(pos, estado.setValue(ENCENDIDO, ahora), 3);
                if (config.alto()) {
                    BlockPos otra = estado.getValue(MITAD) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
                    BlockState otroEstado = nivel.getBlockState(otra);
                    if (otroEstado.is(this)) nivel.setBlock(otra, otroEstado.setValue(ENCENDIDO, ahora), 3);
                }
                boolean electrico = accion.equals("tele") || accion.equals("ordenador") || accion.equals("lampara");
                SoundEvent sonido = electrico ? (ahora ? SoundEvents.STONE_BUTTON_CLICK_ON : SoundEvents.STONE_BUTTON_CLICK_OFF)
                        : (ahora ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.CANDLE_EXTINGUISH);
                nivel.playSound(null, pos, sonido, SoundSource.BLOCKS, 0.8f, 1.0f);
                if (ahora && accion.equals("tele")) {
                    int n = nivel.random.nextInt(8);
                    jugador.displayClientMessage(Component.translatable("yonga.mueble.tele." + n), true);
                } else if (ahora && accion.equals("ordenador")) {
                    long dia = nivel.getDayTime() / 24000L + 1;
                    long t = nivel.getDayTime() % 24000L;
                    String hora = String.format("%02d:%02d", (int) ((t / 1000 + 6) % 24), (int) ((t % 1000) * 60 / 1000));
                    String tiempo = nivel.isThundering() ? "tormenta" : nivel.isRaining() ? "lluvia" : "despejado";
                    jugador.displayClientMessage(Component.translatable("yonga.mueble.ordenador", dia, hora,
                            Component.translatable("yonga.mueble.tiempo." + tiempo)), true);
                }
            }
            return InteractionResult.sidedSuccess(nivel.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /** Lo que sale al moler (o vacío si eso no se muele). */
    private static ItemStack moler(ItemStack entra) {
        if (entra.is(Items.BONE)) return new ItemStack(Items.BONE_MEAL, 4);
        if (entra.is(Items.WHEAT)) return new ItemStack(Items.BREAD);
        if (entra.is(Items.SUGAR_CANE)) return new ItemStack(Items.SUGAR, 2);
        if (entra.is(Items.COBBLESTONE)) return new ItemStack(Items.GRAVEL);
        if (entra.is(Items.GRAVEL)) return new ItemStack(Items.FLINT);
        if (entra.is(Items.COCOA_BEANS)) return new ItemStack(Items.BROWN_DYE, 2);
        return ItemStack.EMPTY;
    }

    /** Gasta uno de lo que llevas en la mano y te da "sale" (si no hay nada que hacer, te dice qué se puede). */
    private static InteractionResult transformar(Level nivel, BlockPos pos, Player jugador, ItemStack enMano, ItemStack sale,
                                                 SoundEvent sonido, String nada) {
        if (nivel.isClientSide) return InteractionResult.SUCCESS;
        if (sale.isEmpty()) {
            jugador.displayClientMessage(Component.translatable(nada), true);
            return InteractionResult.CONSUME;
        }
        if (!jugador.getAbilities().instabuild) enMano.shrink(1);
        if (!jugador.getInventory().add(sale)) jugador.drop(sale, false);
        nivel.playSound(null, pos, sonido, SoundSource.BLOCKS, 1.0f, 1.0f);
        return InteractionResult.CONSUME;
    }

    /** Remienda del todo una prenda de cuero gastando un hilo del inventario. */
    private static InteractionResult coser(Level nivel, BlockPos pos, Player jugador, ItemStack enMano) {
        if (nivel.isClientSide) return InteractionResult.SUCCESS;
        if (!(enMano.getItem() instanceof DyeableLeatherItem) || !enMano.isDamaged()) {
            jugador.displayClientMessage(Component.translatable("yonga.mueble.coser.nada"), true);
            return InteractionResult.CONSUME;
        }
        if (!jugador.getAbilities().instabuild) {
            int hilo = jugador.getInventory().findSlotMatchingItem(new ItemStack(Items.STRING));
            if (hilo < 0) {
                jugador.displayClientMessage(Component.translatable("yonga.mueble.coser.hilo"), true);
                return InteractionResult.CONSUME;
            }
            jugador.getInventory().removeItem(hilo, 1);
        }
        enMano.setDamageValue(0);
        nivel.playSound(null, pos, SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.BLOCKS, 1.0f, 1.3f);
        jugador.displayClientMessage(Component.translatable("yonga.mueble.coser.hecho"), true);
        return InteractionResult.CONSUME;
    }

    private static InteractionResult gramofono(Level nivel, BlockPos pos, Player jugador, ItemStack enMano) {
        if (!(nivel.getBlockEntity(pos) instanceof MuebleBlockEntity be)) return InteractionResult.PASS;
        if (nivel.isClientSide) return InteractionResult.SUCCESS;
        ItemStack disco = be.getItem(0);
        if (!disco.isEmpty()) {                         // quitar el disco
            be.parar();
            be.setItem(0, ItemStack.EMPTY);
            if (!jugador.getInventory().add(disco)) jugador.drop(disco, false);
            return InteractionResult.CONSUME;
        }
        if (enMano.getItem() instanceof net.minecraft.world.item.RecordItem) {
            ItemStack uno = enMano.copyWithCount(1);
            if (!jugador.getAbilities().instabuild) enMano.shrink(1);
            be.setItem(0, uno);
            be.tocar();
            return InteractionResult.CONSUME;
        }
        jugador.displayClientMessage(Component.translatable("yonga.mueble.gramofono.nada"), true);
        return InteractionResult.CONSUME;
    }
}
