package com.lucarita.yonga.core;

import com.lucarita.yonga.YongaMain;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, YongaMain.MOD_ID);

    // Registramos la Mena de Lucarita. Copiamos la dureza del diamante, le damos nivel de luz 9 y que suelte exp.
    public static final RegistryObject<Block> LUCARITA_ORE = BLOCKS.register("lucarita_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DIAMOND_ORE)
                    .strength(25.0f, 6.0f) // El primer número es la dureza (tiempo de picado), el segundo es la resistencia a explosiones
                    .lightLevel(estado -> 9), UniformInt.of(3, 7)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}