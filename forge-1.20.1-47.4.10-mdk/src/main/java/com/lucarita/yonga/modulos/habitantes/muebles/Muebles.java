package com.lucarita.yonga.modulos.habitantes.muebles;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.habitantes.ModuloHabitantes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Muebles y adornos de las casas, por épocas (pieles y vasijas de la prehistoria, ánforas y braseros de la
 * antigüedad, mesas de caballete y tapices medievales, cómodas y relojes de la edad moderna, estufas y gramófonos
 * de la industrial, sofás y televisores de hoy…). Salen en las casas de las aldeas y el Ingeniero los hace en su banco.
 *
 * Qué se puede hacer con ellos (clic derecho):
 *  - sentarse en sillas, sofás, bancos, taburetes y el lecho (agachado no te sientas);
 *  - guardar cosas en vasijas, ánforas, cestos, el escritorio, la cómoda y la nevera (que además hace hielo con
 *    los cubos de agua que le metas);
 *  - encender y apagar lo que da luz, la tele y el ordenador;
 *  - poner discos en el gramófono; mirar la hora en el reloj y dónde estás en el globo; moler en la piedra de moler,
 *    hilar lana en la rueca y remendar ropa de cuero con la máquina de coser.
 *
 * La lista está en MueblesLista (la escribe herramientas/generar_muebles.py).
 */
public final class Muebles {

    private Muebles() {}

    /**
     * Cómo es un mueble. forma: sus cajas (en píxeles, mirando al norte); formaArriba: la de la mitad de arriba si
     * es de dos bloques de alto (si no, null); asiento: altura del asiento en píxeles (-1 = no es asiento);
     * huecos: casillas para guardar (0 = ninguna); accion: qué hace al usarlo (ver MuebleBlock).
     */
    public record Config(boolean pared, boolean pasable, int luz, double asiento, int huecos, String accion,
                         double[][] forma, @Nullable double[][] formaArriba) {
        boolean encendible() { return luz > 0; }
        boolean alto() { return formaArriba != null; }
    }

    public static final Map<String, RegistryObject<Block>> TODOS = new LinkedHashMap<>();

    public static final DeferredRegister<BlockEntityType<?>> ENTIDADES_BLOQUE =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, YongaMain.MOD_ID);

    /** Lo que guarda cosas (y el disco del gramófono). */
    public static final RegistryObject<BlockEntityType<MuebleBlockEntity>> ENTIDAD = ENTIDADES_BLOQUE.register("mueble",
            () -> BlockEntityType.Builder.of(MuebleBlockEntity::new, TODOS.values().stream().map(RegistryObject::get)
                    .filter(b -> b instanceof MuebleContenedorBlock).toArray(Block[]::new)).build(null));

    /** La entidad invisible en la que vas montado cuando te sientas. */
    public static final RegistryObject<EntityType<Asiento>> ASIENTO = ModuloHabitantes.ENTIDADES.register("asiento",
            () -> EntityType.Builder.<Asiento>of(Asiento::new, MobCategory.MISC).sized(0.001f, 0.001f)
                    .noSummon().clientTrackingRange(10).build("asiento"));

    /** Se llama desde ModuloHabitantes antes de registrar sus listas. */
    public static void registrar(IEventBus bus) {
        MueblesLista.registrar();
        ENTIDADES_BLOQUE.register(bus);
    }

    static void mueble(String nombre, BlockBehaviour.Properties propiedades, Config config) {
        BlockBehaviour.Properties p = propiedades.noOcclusion();
        if (config.luz() > 0) {
            int luz = config.luz();
            p = p.lightLevel(estado -> estado.hasProperty(MuebleBlock.ENCENDIDO) && !estado.getValue(MuebleBlock.ENCENDIDO) ? 0 : luz);
        }
        if (config.pasable()) {
            p = p.noCollission();
        }
        final BlockBehaviour.Properties fin = p;
        RegistryObject<Block> bloque = ModuloHabitantes.BLOQUES.register(nombre,
                () -> config.huecos() > 0 ? new MuebleContenedorBlock(fin, config) : new MuebleBlock(fin, config));
        ModuloHabitantes.ITEMS.register(nombre, () -> new BlockItem(bloque.get(), new Item.Properties()));
        TODOS.put(nombre, bloque);
    }

    static BlockBehaviour.Properties madera() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD).ignitedByLava();
    }

    static BlockBehaviour.Properties piedra() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(1.5f, 6f).sound(SoundType.STONE).requiresCorrectToolForDrops();
    }

    static BlockBehaviour.Properties barro() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(0.8f).sound(SoundType.DECORATED_POT);
    }

    static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2f, 6f).sound(SoundType.METAL);
    }

    static BlockBehaviour.Properties tela() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.6f).sound(SoundType.WOOL).ignitedByLava();
    }
}
