package com.lucarita.yonga.modulos.habitantes.compat;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Las casas de las aldeas industriales y contemporáneas usan bloques de Create si está instalado (y los de siempre
 * si no). Las plantillas llevan bloques de Minecraft que hacen de "marca" y que, sin Create, se ven bien igualmente:
 *  - basalto pulido tumbado → eje de Create (la transmisión de la fábrica);
 *  - cobre cortado encerado → carcasa de cobre (las cajas de engranajes);
 *  - paneles de cristal → paneles de cristal enmarcado;
 *  - bloque de hierro (la máquina del aire de las azoteas) → bloque de hierro industrial.
 * Si algún bloque de Create no existe en la versión instalada, se deja el de Minecraft.
 */
public class ConCreateProcessor extends StructureProcessor {

    public static final Codec<ConCreateProcessor> CODEC =
            Codec.STRING.fieldOf("epoca").xmap(ConCreateProcessor::new, p -> p.epoca).codec();

    private static final Map<String, Map<String, String>> CAMBIOS = Map.of(
            "industrial", Map.of(
                    "minecraft:polished_basalt", "create:shaft",
                    "minecraft:waxed_cut_copper", "create:copper_casing",
                    "minecraft:glass_pane", "create:framed_glass_pane"),
            "contemporanea", Map.of(
                    "minecraft:glass_pane", "create:framed_glass_pane",
                    "minecraft:iron_block", "create:industrial_iron_block"));

    private final String epoca;

    public ConCreateProcessor(String epoca) {
        this.epoca = epoca;
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader nivel, BlockPos origen, BlockPos pivote,
                                                             StructureTemplate.StructureBlockInfo original,
                                                             StructureTemplate.StructureBlockInfo actual,
                                                             StructurePlaceSettings ajustes) {
        if (!ModList.get().isLoaded("create")) return actual;
        Map<String, String> cambios = CAMBIOS.get(epoca);
        if (cambios == null) return actual;
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(actual.state().getBlock());
        String nuevo = id == null ? null : cambios.get(id.toString());
        if (nuevo == null) return actual;
        Block bloque = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(nuevo));
        if (bloque == null || bloque == Blocks.AIR) return actual;
        return new StructureTemplate.StructureBlockInfo(actual.pos(), copiar(actual.state(), bloque.defaultBlockState()), actual.nbt());
    }

    /** Pasa al bloque nuevo las propiedades que tengan el mismo nombre (eje, uniones de los paneles…). */
    private static BlockState copiar(BlockState viejo, BlockState nuevo) {
        for (Property<?> p : viejo.getProperties()) {
            Property<?> q = nuevo.getBlock().getStateDefinition().getProperty(p.getName());
            if (q != null) nuevo = poner(nuevo, q, valor(viejo, p));
        }
        return nuevo;
    }

    private static <T extends Comparable<T>> BlockState poner(BlockState estado, Property<T> propiedad, String valor) {
        return propiedad.getValue(valor).map(v -> estado.setValue(propiedad, v)).orElse(estado);
    }

    private static <T extends Comparable<T>> String valor(BlockState estado, Property<T> propiedad) {
        return propiedad.getName(estado.getValue(propiedad));
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return Compat.CON_CREATE.get();
    }
}
