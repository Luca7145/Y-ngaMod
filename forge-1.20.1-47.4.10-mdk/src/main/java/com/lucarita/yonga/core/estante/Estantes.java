package com.lucarita.yonga.core.estante;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.core.ModBlocks;
import com.lucarita.yonga.core.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Los estantes de la 1.21.9 traídos a la 1.20.1 (núcleo: siempre están). Uno por cada madera que existe en esta
 * versión (el roble pálido no existe en la 1.20.1). Se fabrican con 6 troncos descortezados de la misma madera
 * (fila de arriba y fila de abajo) y dan 6 estantes.
 */
public final class Estantes {

    private Estantes() {}

    public static final DeferredRegister<BlockEntityType<?>> ENTIDADES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, YongaMain.MOD_ID);
    public static final DeferredRegister<SoundEvent> SONIDOS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, YongaMain.MOD_ID);

    /** Madera: nombre (como en vanilla) y el tronco descortezado del que se copian el sonido y el color. */
    private record Madera(String nombre, Block descortezado, boolean inflamable) {}

    private static final Madera[] MADERAS = {
            new Madera("oak", Blocks.STRIPPED_OAK_LOG, true),
            new Madera("spruce", Blocks.STRIPPED_SPRUCE_LOG, true),
            new Madera("birch", Blocks.STRIPPED_BIRCH_LOG, true),
            new Madera("jungle", Blocks.STRIPPED_JUNGLE_LOG, true),
            new Madera("acacia", Blocks.STRIPPED_ACACIA_LOG, true),
            new Madera("dark_oak", Blocks.STRIPPED_DARK_OAK_LOG, true),
            new Madera("mangrove", Blocks.STRIPPED_MANGROVE_LOG, true),
            new Madera("cherry", Blocks.STRIPPED_CHERRY_LOG, true),
            new Madera("bamboo", Blocks.STRIPPED_BAMBOO_BLOCK, true),
            new Madera("crimson", Blocks.STRIPPED_CRIMSON_STEM, false),
            new Madera("warped", Blocks.STRIPPED_WARPED_STEM, false),
    };

    public static final List<RegistryObject<Block>> BLOQUES = new ArrayList<>();

    static {
        for (Madera m : MADERAS) {
            String id = m.nombre() + "_shelf";
            RegistryObject<Block> bloque = ModBlocks.BLOCKS.register(id, () -> new EstanteBlock(propiedades(m), m.inflamable()));
            ModItems.ITEMS.register(id, () -> new BlockItem(bloque.get(), new Item.Properties()));
            BLOQUES.add(bloque);
        }
    }

    /**
     * Las propiedades, con el color y el sonido del tronco descortezado. (No se pueden copiar sin más: el color de los
     * troncos depende de su eje, una propiedad que el estante no tiene, y eso rompía el juego al arrancar.)
     */
    private static BlockBehaviour.Properties propiedades(Madera m) {
        Block tronco = m.descortezado();
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of()
                .mapColor(tronco.defaultMapColor())
                .sound(tronco.defaultBlockState().getSoundType())
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.0f, 3.0f)
                .noOcclusion();
        return m.inflamable() ? p.ignitedByLava() : p;
    }

    public static final RegistryObject<BlockEntityType<EstanteBlockEntity>> ENTIDAD = ENTIDADES.register("shelf",
            () -> BlockEntityType.Builder.of(EstanteBlockEntity::new, BLOQUES.stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));

    // Los sonidos (como los de vanilla: block.shelf.*)
    public static final RegistryObject<SoundEvent> SONIDO_ACTIVAR = sonido("block.shelf.activate");
    public static final RegistryObject<SoundEvent> SONIDO_DESACTIVAR = sonido("block.shelf.deactivate");
    public static final RegistryObject<SoundEvent> SONIDO_CAMBIAR_TODO = sonido("block.shelf.multi_swap");
    public static final RegistryObject<SoundEvent> SONIDO_PONER = sonido("block.shelf.place_item");
    public static final RegistryObject<SoundEvent> SONIDO_CAMBIAR = sonido("block.shelf.single_swap");
    public static final RegistryObject<SoundEvent> SONIDO_COGER = sonido("block.shelf.take_item");

    private static RegistryObject<SoundEvent> sonido(String nombre) {
        return SONIDOS.register(nombre, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(YongaMain.MOD_ID, nombre)));
    }

    /** Se llama desde YongaMain (antes de registrar ModBlocks y ModItems, para que los estantes entren en ellos). */
    public static void registrar(IEventBus bus) {
        ENTIDADES.register(bus);
        SONIDOS.register(bus);
    }
}
