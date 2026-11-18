package com.lucarita.yonga.modulos.roles.construccion;

import com.lucarita.yonga.YongaMain;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Bloques de construcción del Ingeniero (se hacen en el banco de ingeniero, ver RecetaBanco).
 *
 * Familias (y quién las puede hacer en el banco):
 *  - Adoquinado (cualquiera):  adoquín de calle, adoquín en espiga, baldosa de acera
 *  - Sillería (Ingeniero Novato): sillares, sillar con moldura, columna
 *  - Industrial (Ingeniero Aprendiz): planchas remachadas, chapa estriada, rejilla, bloque de aviso
 *  - Mosaico (Ingeniero Oficial): baldosas en damero, mosaico de rombos, azulejo
 *  - Cristalería (Ingeniero Experto): vidriera emplomada, cristal esmerilado, claraboya
 *  - Lucarita (Ingeniero Maestro): lucarita pulida, ladrillos de lucarita, lámpara de lucarita (da luz)
 * Los que tienen sentido llevan también losa y escaleras.
 *
 * Además, los bloques por estilos arquitectónicos están en ConstruccionEstilos (los genera el programa).
 *
 * Para añadir un bloque de los de aquí: una línea aquí, su textura, su receta del banco y su nombre en los
 * idiomas (los modelos, estados y botines los genera herramientas/generar_bloques.py). Para uno por estilo,
 * basta con añadirlo a la lista ESTILOS de ese programa.
 */
public final class Construccion {

    public static final DeferredRegister<Block> BLOQUES = DeferredRegister.create(ForgeRegistries.BLOCKS, YongaMain.MOD_ID);
    public static final DeferredRegister<Item> OBJETOS = DeferredRegister.create(ForgeRegistries.ITEMS, YongaMain.MOD_ID);

    /** Todos los objetos de bloques de construcción, en orden (para la pestaña de creativo). */
    public static final List<RegistryObject<Item>> PARA_CREATIVO = new ArrayList<>();

    // ---------- EL BANCO ----------
    public static final RegistryObject<Block> BANCO_INGENIERO = bloque("banco_ingeniero",
            () -> new BancoIngenieroBlock(BlockBehaviour.Properties.copy(Blocks.STONECUTTER)));

    // ---------- ADOQUINADO ----------
    public static final RegistryObject<Block> ADOQUIN_CALLE = bloque("adoquin_calle", () -> new Block(piedra()));
    public static final RegistryObject<Block> ADOQUIN_CALLE_LOSA = bloque("adoquin_calle_losa", () -> new SlabBlock(piedra()));
    public static final RegistryObject<Block> ADOQUIN_CALLE_ESCALERAS = bloque("adoquin_calle_escaleras",
            () -> new StairBlock(() -> Construccion.ADOQUIN_CALLE.get().defaultBlockState(), piedra()));
    public static final RegistryObject<Block> ADOQUIN_ESPIGA = bloque("adoquin_espiga", () -> new Block(piedra()));
    public static final RegistryObject<Block> ADOQUIN_ESPIGA_LOSA = bloque("adoquin_espiga_losa", () -> new SlabBlock(piedra()));
    public static final RegistryObject<Block> ADOQUIN_ESPIGA_ESCALERAS = bloque("adoquin_espiga_escaleras",
            () -> new StairBlock(() -> Construccion.ADOQUIN_ESPIGA.get().defaultBlockState(), piedra()));
    public static final RegistryObject<Block> BALDOSA_ACERA = bloque("baldosa_acera", () -> new Block(piedra()));

    // ---------- SILLERÍA ----------
    public static final RegistryObject<Block> SILLARES = bloque("sillares", () -> new Block(piedra()));
    public static final RegistryObject<Block> SILLARES_LOSA = bloque("sillares_losa", () -> new SlabBlock(piedra()));
    public static final RegistryObject<Block> SILLARES_ESCALERAS = bloque("sillares_escaleras",
            () -> new StairBlock(() -> Construccion.SILLARES.get().defaultBlockState(), piedra()));
    public static final RegistryObject<Block> SILLAR_MOLDURA = bloque("sillar_moldura", () -> new Block(piedra()));
    public static final RegistryObject<Block> COLUMNA = bloque("columna", () -> new RotatedPillarBlock(piedra()));

    // ---------- INDUSTRIAL ----------
    public static final RegistryObject<Block> PLANCHAS_REMACHADAS = bloque("planchas_remachadas", () -> new Block(metal()));
    public static final RegistryObject<Block> PLANCHAS_REMACHADAS_LOSA = bloque("planchas_remachadas_losa", () -> new SlabBlock(metal()));
    public static final RegistryObject<Block> PLANCHAS_REMACHADAS_ESCALERAS = bloque("planchas_remachadas_escaleras",
            () -> new StairBlock(() -> Construccion.PLANCHAS_REMACHADAS.get().defaultBlockState(), metal()));
    public static final RegistryObject<Block> CHAPA_ESTRIADA = bloque("chapa_estriada", () -> new Block(metal()));
    public static final RegistryObject<Block> CHAPA_ESTRIADA_LOSA = bloque("chapa_estriada_losa", () -> new SlabBlock(metal()));
    public static final RegistryObject<Block> CHAPA_ESTRIADA_ESCALERAS = bloque("chapa_estriada_escaleras",
            () -> new StairBlock(() -> Construccion.CHAPA_ESTRIADA.get().defaultBlockState(), metal()));
    public static final RegistryObject<Block> REJILLA = bloque("rejilla",
            () -> new Block(metal().noOcclusion().isViewBlocking((e, n, p) -> false).isSuffocating((e, n, p) -> false)));
    public static final RegistryObject<Block> AVISO = bloque("aviso", () -> new Block(metal()));

    // ---------- MOSAICO ----------
    public static final RegistryObject<Block> DAMERO = bloque("damero", () -> new Block(ceramica()));
    public static final RegistryObject<Block> DAMERO_LOSA = bloque("damero_losa", () -> new SlabBlock(ceramica()));
    public static final RegistryObject<Block> DAMERO_ESCALERAS = bloque("damero_escaleras",
            () -> new StairBlock(() -> Construccion.DAMERO.get().defaultBlockState(), ceramica()));
    public static final RegistryObject<Block> MOSAICO_ROMBOS = bloque("mosaico_rombos", () -> new Block(ceramica()));
    public static final RegistryObject<Block> AZULEJO = bloque("azulejo", () -> new Block(ceramica()));

    // ---------- CRISTALERÍA ----------
    public static final RegistryObject<Block> VIDRIERA = bloque("vidriera", () -> new GlassBlock(cristal()));
    public static final RegistryObject<Block> CRISTAL_ESMERILADO = bloque("cristal_esmerilado", () -> new GlassBlock(cristal()));
    public static final RegistryObject<Block> CLARABOYA = bloque("claraboya", () -> new GlassBlock(cristal()));

    // ---------- LUCARITA ----------
    public static final RegistryObject<Block> LUCARITA_PULIDA = bloque("lucarita_pulida", () -> new Block(lucarita()));
    public static final RegistryObject<Block> LADRILLOS_LUCARITA = bloque("ladrillos_lucarita", () -> new Block(lucarita()));
    public static final RegistryObject<Block> LADRILLOS_LUCARITA_LOSA = bloque("ladrillos_lucarita_losa", () -> new SlabBlock(lucarita()));
    public static final RegistryObject<Block> LADRILLOS_LUCARITA_ESCALERAS = bloque("ladrillos_lucarita_escaleras",
            () -> new StairBlock(() -> Construccion.LADRILLOS_LUCARITA.get().defaultBlockState(), lucarita()));
    public static final RegistryObject<Block> LAMPARA_LUCARITA = bloque("lampara_lucarita",
            () -> new Block(lucarita().lightLevel(estado -> 15)));

    private Construccion() {}

    private static BlockBehaviour.Properties ceramica() {
        return BlockBehaviour.Properties.copy(Blocks.TERRACOTTA);
    }

    private static BlockBehaviour.Properties cristal() {
        return BlockBehaviour.Properties.copy(Blocks.GLASS);
    }

    private static BlockBehaviour.Properties lucarita() {
        return BlockBehaviour.Properties.copy(Blocks.COPPER_BLOCK);
    }

    private static BlockBehaviour.Properties piedra() {
        return BlockBehaviour.Properties.copy(Blocks.STONE_BRICKS);
    }

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK);
    }

    static RegistryObject<Block> bloque(String nombre, Supplier<Block> fabrica) {
        RegistryObject<Block> bloque = BLOQUES.register(nombre, fabrica);
        PARA_CREATIVO.add(OBJETOS.register(nombre, () -> new BlockItem(bloque.get(), new Item.Properties())));
        return bloque;
    }

    public static void registrar(IEventBus bus) {
        ConstruccionEstilos.registrar();   // los bloques por estilos (los escribe herramientas/generar_bloques.py)
        BLOQUES.register(bus);
        OBJETOS.register(bus);
    }
}
