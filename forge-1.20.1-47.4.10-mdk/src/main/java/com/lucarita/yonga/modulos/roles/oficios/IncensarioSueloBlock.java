package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.modulos.fe.mobs.Jefe;
import com.lucarita.yonga.modulos.fe.mobs.MobFe;
import com.lucarita.yonga.modulos.roles.ModuloRoles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * INCENSARIO de suelo (Sacerdote, desde Aprendiz). Dos versiones:
 *  - Incensario (luminoso): aleja a los mobs de fe OSCURA inferiores.
 *  - Incensario oscuro: aleja a los mobs de fe LUMINOSA inferiores.
 *
 * Se enciende con mechero (pedernal) o con una carga ígnea, y se apaga con la mano vacía. Mientras arde:
 *  - Los mobs del lado contrario que estén dentro del radio salen huyendo hacia fuera y se olvidan de su objetivo.
 *  - Los de fuera tampoco persiguen a nadie que esté dentro.
 *  - No aparecen mobs de ese lado dentro del radio.
 * A los superiores y a los jefes no les hace nada.
 */
public class IncensarioSueloBlock extends BaseEntityBlock {

    public static final BooleanProperty ENCENDIDO = BlockStateProperties.LIT;

    public static final double RADIO = 16.0;                // en horizontal
    public static final double ALTO = 10.0;                 // arriba y abajo

    private static final VoxelShape FORMA = Shapes.or(
            Block.box(4, 0, 4, 12, 2, 12),                  // pie
            Block.box(7, 2, 7, 9, 7, 9),                    // columna
            Block.box(4, 7, 4, 12, 11, 12),                 // cuenco
            Block.box(6, 11, 6, 10, 13, 10));               // tapa

    // Incensarios cargados en cada mundo (para saber rápido si un mob aparece cerca de uno). Solo en el servidor.
    private static final Map<Level, Set<BlockPos>> CARGADOS = Collections.synchronizedMap(new WeakHashMap<>());

    private final boolean oscuro;

    public IncensarioSueloBlock(boolean oscuro, Properties propiedades) {
        super(propiedades);
        this.oscuro = oscuro;
        registerDefaultState(this.stateDefinition.any().setValue(ENCENDIDO, false));
    }

    /** true = incensario oscuro (aleja a los luminosos); false = el normal (aleja a los oscuros). */
    public boolean oscuro() {
        return oscuro;
    }

    /** ¿Este incensario aleja a este mob? */
    public boolean aleja(Entity entidad) {
        return entidad instanceof MobFe fe && !(entidad instanceof Jefe) && !fe.esSuperior() && fe.esOscuro() != oscuro;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> definicion) {
        definicion.add(ENCENDIDO);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext contexto) {
        return defaultBlockState();
    }

    @Override
    public RenderShape getRenderShape(BlockState estado) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext contexto) {
        return FORMA;
    }

    @Override
    public boolean canSurvive(BlockState estado, LevelReader nivel, BlockPos pos) {
        return Block.canSupportCenter(nivel, pos.below(), net.minecraft.core.Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState estado, net.minecraft.core.Direction lado, BlockState vecino,
                                  net.minecraft.world.level.LevelAccessor nivel, BlockPos pos, BlockPos posVecino) {
        if (lado == net.minecraft.core.Direction.DOWN && !estado.canSurvive(nivel, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(estado, lado, vecino, nivel, pos, posVecino);
    }

    @Override
    public InteractionResult use(BlockState estado, Level nivel, BlockPos pos, Player jugador, InteractionHand mano, BlockHitResult golpe) {
        ItemStack objeto = jugador.getItemInHand(mano);
        boolean encendido = estado.getValue(ENCENDIDO);
        if (!encendido && (objeto.getItem() instanceof FlintAndSteelItem || objeto.getItem() instanceof FireChargeItem)) {
            if (!nivel.isClientSide()) {
                nivel.setBlock(pos, estado.setValue(ENCENDIDO, true), 3);
                nivel.gameEvent(jugador, GameEvent.BLOCK_CHANGE, pos);
                if (objeto.getItem() instanceof FlintAndSteelItem) {
                    nivel.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    objeto.hurtAndBreak(1, jugador, j -> j.broadcastBreakEvent(mano));
                } else {
                    nivel.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    if (!jugador.getAbilities().instabuild) objeto.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(nivel.isClientSide());
        }
        if (encendido && objeto.isEmpty() && mano == InteractionHand.MAIN_HAND) {
            if (!nivel.isClientSide()) {
                nivel.setBlock(pos, estado.setValue(ENCENDIDO, false), 3);
                nivel.gameEvent(jugador, GameEvent.BLOCK_CHANGE, pos);
                nivel.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 1.4f);
            }
            return InteractionResult.sidedSuccess(nivel.isClientSide());
        }
        return InteractionResult.PASS;
    }

    // Humo y chispas mientras arde
    @Override
    public void animateTick(BlockState estado, Level nivel, BlockPos pos, RandomSource azar) {
        if (!estado.getValue(ENCENDIDO)) return;
        double x = pos.getX() + 0.5, y = pos.getY() + 0.85, z = pos.getZ() + 0.5;
        if (azar.nextInt(2) == 0) {
            nivel.addParticle(oscuro ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    x + (azar.nextDouble() - 0.5) * 0.2, y, z + (azar.nextDouble() - 0.5) * 0.2, 0, 0.03, 0);
        }
        if (azar.nextInt(4) == 0) {
            nivel.addParticle(oscuro ? ParticleTypes.SOUL : ParticleTypes.END_ROD,
                    x + (azar.nextDouble() - 0.5) * 0.4, y + 0.1, z + (azar.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
        }
        if (azar.nextInt(30) == 0) {
            nivel.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.4f, oscuro ? 0.6f : 1.2f, false);
        }
    }

    // ---------- ENTIDAD DE BLOQUE ----------

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState estado) {
        return new Entidad(pos, estado);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level nivel, BlockState estado, BlockEntityType<T> tipo) {
        return nivel.isClientSide() ? null : createTickerHelper(tipo, ModuloRoles.ENTIDAD_INCENSARIO_SUELO.get(), Entidad::tickServidor);
    }

    public static class Entidad extends BlockEntity {

        public Entidad(BlockPos pos, BlockState estado) {
            super(ModuloRoles.ENTIDAD_INCENSARIO_SUELO.get(), pos, estado);
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (this.level != null && !this.level.isClientSide()) {
                CARGADOS.computeIfAbsent(this.level, n -> ConcurrentHashMap.newKeySet()).add(this.worldPosition.immutable());
            }
        }

        @Override
        public void setRemoved() {
            super.setRemoved();
            quitar();
        }

        @Override
        public void onChunkUnloaded() {
            super.onChunkUnloaded();
            quitar();
        }

        private void quitar() {
            if (this.level != null && !this.level.isClientSide()) {
                Set<BlockPos> lista = CARGADOS.get(this.level);
                if (lista != null) lista.remove(this.worldPosition);
            }
        }

        static void tickServidor(Level nivel, BlockPos pos, BlockState estado, Entidad entidad) {
            if (!estado.getValue(ENCENDIDO) || !(estado.getBlock() instanceof IncensarioSueloBlock bloque)
                    || (nivel.getGameTime() + pos.asLong()) % 10 != 0) {
                return;
            }
            Vec3 centro = Vec3.atBottomCenterOf(pos);
            AABB zona = zona(pos);
            // Los que persiguen a alguien de dentro, aunque estén fuera, se olvidan de él
            for (Mob mob : nivel.getEntitiesOfClass(Mob.class, zona.inflate(24, 8, 24), bloque::aleja)) {
                LivingEntity objetivo = mob.getTarget();
                if (objetivo != null && dentro(pos, objetivo.position())) {
                    mob.setTarget(null);
                }
                if (dentro(pos, mob.position())) {
                    echar(mob, centro);
                }
            }
        }

        /** Saca al mob hacia fuera del radio: le da un empujón y, si anda, le manda caminar lejos. */
        private static void echar(Mob mob, Vec3 centro) {
            Vec3 fuera = new Vec3(mob.getX() - centro.x, 0, mob.getZ() - centro.z);
            if (fuera.lengthSqr() < 1.0E-3) {
                fuera = new Vec3(mob.getRandom().nextDouble() - 0.5, 0, mob.getRandom().nextDouble() - 0.5);
            }
            fuera = fuera.normalize();
            mob.setDeltaMovement(mob.getDeltaMovement().add(fuera.x * 0.25, mob.onGround() ? 0.1 : 0.0, fuera.z * 0.25));
            mob.hurtMarked = true;
            if (mob instanceof PathfinderMob andante) {
                Vec3 destino = centro.add(fuera.scale(RADIO + 6));
                Path camino = andante.getNavigation().createPath(destino.x, mob.getY(), destino.z, 1);
                if (camino != null) {
                    andante.getNavigation().moveTo(camino, 1.3);
                }
            }
            if (mob.level() instanceof ServerLevel servidor && mob.getRandom().nextInt(3) == 0) {
                servidor.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + mob.getBbHeight() * 0.6, mob.getZ(),
                        3, 0.2, 0.2, 0.2, 0.01);
            }
        }
    }

    private static AABB zona(BlockPos pos) {
        return new AABB(pos).inflate(RADIO, ALTO, RADIO);
    }

    private static boolean dentro(BlockPos pos, Vec3 punto) {
        double dx = punto.x - (pos.getX() + 0.5), dz = punto.z - (pos.getZ() + 0.5), dy = punto.y - pos.getY();
        return dx * dx + dz * dz <= RADIO * RADIO && Math.abs(dy) <= ALTO;
    }

    // ---------- EVENTOS (se registran en ModuloRoles) ----------

    /** Dentro del radio de un incensario encendido no aparecen mobs del lado que aleja. */
    public static void alAparecer(MobSpawnEvent.FinalizeSpawn evento) {
        Mob mob = evento.getEntity();
        if (!(mob instanceof MobFe fe) || fe.esSuperior() || mob instanceof Jefe || !(evento.getLevel() instanceof ServerLevel)) {
            return;
        }
        // Solo las apariciones normales: huevos, comandos e invocaciones siguen funcionando
        MobSpawnType razon = evento.getSpawnType();
        if (razon != MobSpawnType.NATURAL && razon != MobSpawnType.CHUNK_GENERATION
                && razon != MobSpawnType.STRUCTURE && razon != MobSpawnType.PATROL) {
            return;
        }
        Level nivel = mob.level();
        Set<BlockPos> lista = CARGADOS.get(nivel);
        if (lista == null || lista.isEmpty()) return;
        Vec3 donde = new Vec3(evento.getX(), evento.getY(), evento.getZ());
        for (BlockPos pos : lista) {
            if (!dentro(pos, donde)) continue;
            BlockState estado = nivel.getBlockState(pos);
            if (estado.getBlock() instanceof IncensarioSueloBlock bloque && estado.getValue(ENCENDIDO) && bloque.aleja(mob)) {
                evento.setSpawnCancelled(true);
                return;
            }
        }
    }
}
