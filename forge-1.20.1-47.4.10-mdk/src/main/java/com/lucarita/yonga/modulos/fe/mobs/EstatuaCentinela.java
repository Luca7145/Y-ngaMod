package com.lucarita.yonga.modulos.fe.mobs;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.fe.FeDatos;
import com.lucarita.yonga.modulos.fe.ModuloFe;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

/**
 * Las estatuas del desierto: dos bloques de arenisca con una calabaza tallada encima, como un gólem a medio hacer.
 * La mayoría son solo eso, bloques. Pero una de cada PROB_DE_VERDAD tiene una calabaza "trampa" (igual a la
 * vista): si se acerca alguien a quien el centinela atacaría, la estatua explota (hace daño, pero no rompe
 * nada), desaparece y en su sitio despierta el centinela de piedra.
 *
 * La generación (el "feature") se registra siempre, porque su JSON siempre está; si el módulo de fe está
 * desactivado, solo pone estatuas normales.
 */
public final class EstatuaCentinela {

    // ---------- AJUSTES ----------
    private static final int PROB_DE_VERDAD = 12;          // 1 de cada 12 estatuas esconde un centinela
    private static final double DISTANCIA_DESPERTAR = 6.0;
    private static final float FUERZA_EXPLOSION = 2.5f;

    private EstatuaCentinela() {}

    // ---------- LA GENERACIÓN (siempre registrada) ----------

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, YongaMain.MOD_ID);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ESTATUA = FEATURES.register("estatua_desierto",
            () -> new Generar(NoneFeatureConfiguration.CODEC));

    public static void registrar(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }

    /** Pone una estatua sobre la arena (lo llama Minecraft al generar el mundo, según placed_feature/estatua_desierto). */
    public static class Generar extends Feature<NoneFeatureConfiguration> {
        public Generar(Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> contexto) {
            WorldGenLevel nivel = contexto.level();
            BlockPos base = contexto.origin();
            RandomSource azar = contexto.random();
            if (!nivel.getBlockState(base.below()).is(BlockTags.SAND)) {
                return false;
            }
            for (int i = 0; i < 3; i++) {
                if (!nivel.getBlockState(base.above(i)).isAir()) {
                    return false;
                }
            }
            Direction mira = Direction.Plane.HORIZONTAL.getRandomDirection(azar);
            boolean deVerdad = Modulo.FE.activo() && azar.nextInt(PROB_DE_VERDAD) == 0;
            BlockState cabeza = deVerdad
                    ? ModuloFe.CALABAZA_CENTINELA.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, mira)
                    : Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, mira);
            nivel.setBlock(base, Blocks.SANDSTONE.defaultBlockState(), 2);
            nivel.setBlock(base.above(), Blocks.SANDSTONE.defaultBlockState(), 2);
            nivel.setBlock(base.above(2), cabeza, 2);
            return true;
        }
    }

    // ---------- LA CALABAZA TRAMPA (solo con el módulo de fe) ----------

    /** Igual que una calabaza tallada (mismo aspecto y mismo botín), pero vigila. */
    public static class Calabaza extends HorizontalDirectionalBlock implements EntityBlock {
        public Calabaza() {
            super(BlockBehaviour.Properties.copy(Blocks.CARVED_PUMPKIN));
            this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> constructor) {
            constructor.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext contexto) {
            return this.defaultBlockState().setValue(FACING, contexto.getHorizontalDirection().getOpposite());
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos posicion, BlockState estado) {
            return new Vigia(posicion, estado);
        }

        @Nullable
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level nivel, BlockState estado, BlockEntityType<T> tipo) {
            return nivel.isClientSide || tipo != ModuloFe.VIGIA_CENTINELA.get() ? null
                    : (n, p, e, entidad) -> ((Vigia) entidad).tick((ServerLevel) n, p, e);
        }
    }

    /** El "vigía" dentro de la calabaza: cada medio segundo mira si hay alguien cerca a quien atacar. */
    public static class Vigia extends BlockEntity {
        public Vigia(BlockPos posicion, BlockState estado) {
            super(ModuloFe.VIGIA_CENTINELA.get(), posicion, estado);
        }

        void tick(ServerLevel nivel, BlockPos posicion, BlockState estado) {
            if (nivel.getGameTime() % 10 != 0) {
                return;
            }
            Player jugador = nivel.getNearestPlayer(posicion.getX() + 0.5, posicion.getY() - 1.0, posicion.getZ() + 0.5,
                    DISTANCIA_DESPERTAR, j -> j instanceof Player p && !p.isCreative() && !p.isSpectator()
                            && MobFe.ataca(false, true, FeDatos.obtenerNivel(p)));
            if (jugador != null) {
                despertar(nivel, posicion, estado.getValue(HorizontalDirectionalBlock.FACING), jugador);
            }
        }
    }

    /** ¡Bum! La estatua desaparece (sin romper nada más) y en su sitio queda el centinela. */
    private static void despertar(ServerLevel nivel, BlockPos calabaza, Direction mira, Player jugador) {
        BlockPos base = calabaza.below(2);
        nivel.removeBlock(calabaza, false);
        for (BlockPos p : new BlockPos[]{calabaza.below(), base}) {
            if (nivel.getBlockState(p).is(Blocks.SANDSTONE)) {
                nivel.removeBlock(p, false);
            }
        }
        nivel.explode(null, base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5, FUERZA_EXPLOSION, Level.ExplosionInteraction.NONE);
        nivel.playSound(null, base, ModuloFe.SONIDO_CENTINELA_DESPERTAR.get(), SoundSource.HOSTILE, 3.0f, 1.0f);
        nivel.playSound(null, base, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 2.0f, 0.6f);

        Centinela centinela = ModuloFe.CENTINELA.get().create(nivel);
        if (centinela == null) {
            return;
        }
        centinela.moveTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, mira.toYRot(), 0.0f);
        centinela.setYHeadRot(mira.toYRot());
        centinela.setYBodyRot(mira.toYRot());
        centinela.setTarget(jugador);
        centinela.despertar();
        nivel.addFreshEntity(centinela);
    }
}
