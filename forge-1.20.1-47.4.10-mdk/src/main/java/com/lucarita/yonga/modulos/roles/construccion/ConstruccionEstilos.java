package com.lucarita.yonga.modulos.roles.construccion;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;

/**
 * Bloques por estilos arquitectónicos (neolítico, clásico, moderno, gótico, renacentista, neoclásico,
 * barroco, rococó, egipcio y art déco).
 *
 * ¡NO LO EDITES A MANO! Lo escribe herramientas/generar_bloques.py a partir de su lista ESTILOS
 * (ahí están también sus dibujos, de qué se hacen en el banco, qué rango piden y sus nombres).
 */
final class ConstruccionEstilos {

    private ConstruccionEstilos() {}

    static void registrar() {
        RegistryObject<Block> piedra_seca = Construccion.bloque("piedra_seca", () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        Construccion.bloque("piedra_seca_losa", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        Construccion.bloque("piedra_seca_escaleras", () -> new StairBlock(() -> piedra_seca.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        RegistryObject<Block> adobe = Construccion.bloque("adobe", () -> new Block(BlockBehaviour.Properties.copy(Blocks.MUD_BRICKS)));
        Construccion.bloque("adobe_losa", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.MUD_BRICKS)));
        Construccion.bloque("adobe_escaleras", () -> new StairBlock(() -> adobe.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.MUD_BRICKS)));
        RegistryObject<Block> megalito = Construccion.bloque("megalito", () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        RegistryObject<Block> marmol = Construccion.bloque("marmol", () -> new Block(BlockBehaviour.Properties.copy(Blocks.CALCITE)));
        Construccion.bloque("marmol_losa", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.CALCITE)));
        Construccion.bloque("marmol_escaleras", () -> new StairBlock(() -> marmol.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.CALCITE)));
        RegistryObject<Block> columna_marmol = Construccion.bloque("columna_marmol", () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.CALCITE)));
        RegistryObject<Block> friso_griego = Construccion.bloque("friso_griego", () -> new Block(BlockBehaviour.Properties.copy(Blocks.CALCITE)));
        RegistryObject<Block> ladrillo_romano = Construccion.bloque("ladrillo_romano", () -> new Block(BlockBehaviour.Properties.copy(Blocks.BRICKS)));
        Construccion.bloque("ladrillo_romano_losa", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.BRICKS)));
        Construccion.bloque("ladrillo_romano_escaleras", () -> new StairBlock(() -> ladrillo_romano.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.BRICKS)));
        RegistryObject<Block> hormigon_pulido = Construccion.bloque("hormigon_pulido", () -> new Block(BlockBehaviour.Properties.copy(Blocks.LIGHT_GRAY_CONCRETE)));
        Construccion.bloque("hormigon_pulido_losa", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.LIGHT_GRAY_CONCRETE)));
        Construccion.bloque("hormigon_pulido_escaleras", () -> new StairBlock(() -> hormigon_pulido.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.LIGHT_GRAY_CONCRETE)));
        RegistryObject<Block> lamas_madera = Construccion.bloque("lamas_madera", () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)));
        RegistryObject<Block> azulejo_metro = Construccion.bloque("azulejo_metro", () -> new Block(BlockBehaviour.Properties.copy(Blocks.TERRACOTTA)));
        RegistryObject<Block> revoco_blanco = Construccion.bloque("revoco_blanco", () -> new Block(BlockBehaviour.Properties.copy(Blocks.LIGHT_GRAY_CONCRETE)));
        Construccion.bloque("revoco_blanco_losa", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.LIGHT_GRAY_CONCRETE)));
        Construccion.bloque("revoco_blanco_escaleras", () -> new StairBlock(() -> revoco_blanco.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.LIGHT_GRAY_CONCRETE)));
        RegistryObject<Block> ventanal = Construccion.bloque("ventanal", () -> new GlassBlock(BlockBehaviour.Properties.copy(Blocks.GLASS)));
        RegistryObject<Block> piedra_gotica = Construccion.bloque("piedra_gotica", () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        Construccion.bloque("piedra_gotica_losa", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        Construccion.bloque("piedra_gotica_escaleras", () -> new StairBlock(() -> piedra_gotica.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        RegistryObject<Block> traceria = Construccion.bloque("traceria", () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        RegistryObject<Block> roseton = Construccion.bloque("roseton", () -> new GlassBlock(BlockBehaviour.Properties.copy(Blocks.GLASS)));
        RegistryObject<Block> suelo_ajedrez = Construccion.bloque("suelo_ajedrez", () -> new Block(BlockBehaviour.Properties.copy(Blocks.CALCITE)));
        Construccion.bloque("suelo_ajedrez_losa", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.CALCITE)));
        Construccion.bloque("suelo_ajedrez_escaleras", () -> new StairBlock(() -> suelo_ajedrez.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.CALCITE)));
        RegistryObject<Block> almohadillado = Construccion.bloque("almohadillado", () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        Construccion.bloque("almohadillado_losa", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        Construccion.bloque("almohadillado_escaleras", () -> new StairBlock(() -> almohadillado.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        RegistryObject<Block> casetonado = Construccion.bloque("casetonado", () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        RegistryObject<Block> estuco_moldura = Construccion.bloque("estuco_moldura", () -> new Block(BlockBehaviour.Properties.copy(Blocks.CALCITE)));
        RegistryObject<Block> denticulos = Construccion.bloque("denticulos", () -> new Block(BlockBehaviour.Properties.copy(Blocks.CALCITE)));
        RegistryObject<Block> pilastra = Construccion.bloque("pilastra", () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.CALCITE)));
        RegistryObject<Block> marmol_negro = Construccion.bloque("marmol_negro", () -> new Block(BlockBehaviour.Properties.copy(Blocks.POLISHED_BLACKSTONE)));
        Construccion.bloque("marmol_negro_losa", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.POLISHED_BLACKSTONE)));
        Construccion.bloque("marmol_negro_escaleras", () -> new StairBlock(() -> marmol_negro.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.POLISHED_BLACKSTONE)));
        RegistryObject<Block> talla_dorada = Construccion.bloque("talla_dorada", () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)));
        RegistryObject<Block> damasco = Construccion.bloque("damasco", () -> new Block(BlockBehaviour.Properties.copy(Blocks.RED_WOOL)));
        RegistryObject<Block> estuco_rosa = Construccion.bloque("estuco_rosa", () -> new Block(BlockBehaviour.Properties.copy(Blocks.LIGHT_GRAY_CONCRETE)));
        RegistryObject<Block> panel_rococo = Construccion.bloque("panel_rococo", () -> new Block(BlockBehaviour.Properties.copy(Blocks.LIGHT_GRAY_CONCRETE)));
        RegistryObject<Block> jeroglificos = Construccion.bloque("jeroglificos", () -> new Block(BlockBehaviour.Properties.copy(Blocks.SANDSTONE)));
        RegistryObject<Block> abanico_deco = Construccion.bloque("abanico_deco", () -> new Block(BlockBehaviour.Properties.copy(Blocks.POLISHED_BLACKSTONE)));
        RegistryObject<Block> zigzag_deco = Construccion.bloque("zigzag_deco", () -> new Block(BlockBehaviour.Properties.copy(Blocks.POLISHED_BLACKSTONE)));
        RegistryObject<Block> marmol_verde = Construccion.bloque("marmol_verde", () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        Construccion.bloque("marmol_verde_losa", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
        Construccion.bloque("marmol_verde_escaleras", () -> new StairBlock(() -> marmol_verde.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS)));
    }
}
