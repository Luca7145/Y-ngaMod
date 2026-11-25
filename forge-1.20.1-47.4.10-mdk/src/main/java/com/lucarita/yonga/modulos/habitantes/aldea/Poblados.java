package com.lucarita.yonga.modulos.habitantes.aldea;

import com.lucarita.yonga.modulos.habitantes.Epoca;
import com.lucarita.yonga.modulos.habitantes.Habitante;
import com.lucarita.yonga.modulos.habitantes.HabitantesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Fase 8: cada aldea que se genera es de una ÉPOCA (según qué estructura de aldea haya salido) y todos sus
 * habitantes son de esa época. Los edificios salen ya al generarse el mundo; la primera vez que un habitante revisa su
 * aldea se arreglan los detalles (el centro de las aldeas prehistóricas) y se puebla según las camas.
 *
 * Las aldeas que ya existían antes de esta versión (con habitantes medievales) se quedan medievales.
 */
public class Poblados extends SavedData {

    /**
     * Épocas que ya tienen sus aldeas. Se generan con el mundo (ver herramientas/generar_aldeas.py): la prehistórica
     * con el trazado de Minecraft y sus casas cambiadas por chozas; las demás, como estructuras propias
     * ("yonga:aldea_<época>") con sus calles, su centro y sus casas. Todas con el mismo peso.
     */
    public static final Set<Epoca> CONSTRUIDAS = EnumSet.of(Epoca.PREHISTORICA, Epoca.ANTIGUA, Epoca.MEDIEVAL, Epoca.MODERNA, Epoca.INDUSTRIAL, Epoca.CONTEMPORANEA);

    private static final String NOMBRE = "yonga_poblados";

    /** Aldea (chunk donde empieza la estructura) -> su época y si ya se ha levantado. */
    private final Map<Long, Integer> epoca = new HashMap<>();
    private final Set<Long> hechas = new java.util.HashSet<>();
    /** Último intento por aldea (no se guarda): para no mirar cien veces por segundo. */
    private final Map<Long, Long> intento = new HashMap<>();

    public static Poblados de(ServerLevel nivel) {
        return nivel.getDataStorage().computeIfAbsent(Poblados::cargar, Poblados::new, NOMBRE);
    }

    // =====================================================================================================
    // QUÉ ALDEA Y QUÉ ÉPOCA
    // =====================================================================================================

    /** La aldea (estructura de aldea de Minecraft) que tiene una pieza en esa posición, o null. */
    @Nullable
    public static StructureStart aldeaEn(ServerLevel nivel, BlockPos pos) {
        for (Holder<Structure> s : nivel.registryAccess().registryOrThrow(Registries.STRUCTURE).getTagOrEmpty(StructureTags.VILLAGE)) {
            StructureStart inicio = nivel.structureManager().getStructureWithPieceAt(pos, s.value());
            if (inicio != null && inicio.isValid()) return inicio;
        }
        return null;
    }

    /**
     * La época de una aldea sale de qué estructura es: las nuestras se llaman "yonga:aldea_<época>"; las aldeas de
     * Minecraft que queden (de antes, o de otros mods) cuentan como prehistóricas. El reparto entre épocas lo hace
     * el propio Minecraft al elegir qué aldea genera (data/minecraft/worldgen/structure_set/villages.json, todas con el
     * mismo peso).
     */
    public static Epoca epocaDeEstructura(ServerLevel nivel, StructureStart inicio) {
        var clave = nivel.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(inicio.getStructure());
        if (clave != null && clave.getNamespace().equals("yonga") && clave.getPath().startsWith("aldea_")) {
            String id = clave.getPath().substring(6);
            for (Epoca e : Epoca.values()) if (e.id().equals(id)) return e;
        }
        return Epoca.PREHISTORICA;
    }

    /** La época de la aldea que hay en esa posición (la guardada o la de su estructura); fuera de una aldea, la medieval. */
    public static Epoca epocaEn(ServerLevel nivel, BlockPos pos) {
        StructureStart inicio = aldeaEn(nivel, pos);
        if (inicio == null) return Epoca.MEDIEVAL;
        return de(nivel).epocaDe(nivel, inicio);
    }

    /** La de un aldeano que aparece al generarse una aldea (si no se encuentra la aldea, prehistórica). */
    public static Epoca epocaAldeano(ServerLevel nivel, BlockPos pos) {
        StructureStart inicio = aldeaEn(nivel, pos);
        return inicio == null ? Epoca.PREHISTORICA : de(nivel).epocaDe(nivel, inicio);
    }

    public Epoca epocaDe(ServerLevel nivel, StructureStart inicio) {
        Integer guardada = this.epoca.get(inicio.getChunkPos().toLong());
        return guardada != null ? Epoca.de(guardada) : epocaDeEstructura(nivel, inicio);
    }

    // =====================================================================================================
    // LEVANTAR LA ALDEA DE SU ÉPOCA
    // =====================================================================================================

    /** Lo llama cada habitante al revisar su aldea: si aún no se ha levantado la de su época y está cargada, se hace. */
    public static void revisar(ServerLevel nivel, BlockPos campana) {
        Poblados datos = de(nivel);
        StructureStart inicio = aldeaEn(nivel, campana);
        if (inicio == null) return;
        long clave = inicio.getChunkPos().toLong();
        if (datos.hechas.contains(clave)) return;
        long ahora = nivel.getGameTime();
        Long antes = datos.intento.get(clave);
        if (antes != null && ahora - antes < 100) return;
        datos.intento.put(clave, ahora);

        Epoca epoca = datos.epocaDe(nivel, inicio);
        BoundingBox caja = inicio.getBoundingBox();
        // ¿Aldea de antes de esta versión? Si hay habitantes de otra época, se queda como estaba (medieval)
        if (!datos.epoca.containsKey(clave)) {
            AABB zona = new AABB(caja.minX(), caja.minY() - 8, caja.minZ(), caja.maxX() + 1, caja.maxY() + 8, caja.maxZ() + 1);
            boolean vieja = !nivel.getEntitiesOfClass(Habitante.class, zona, h -> h.epoca() != epoca).isEmpty();
            datos.epoca.put(clave, (vieja ? Epoca.MEDIEVAL : epoca).ordinal());
            datos.setDirty();
            if (vieja) {
                datos.hechas.add(clave);
                return;
            }
        }
        if (!CONSTRUIDAS.contains(epoca)) {
            datos.hechas.add(clave);
            datos.setDirty();
            return;
        }
        // Las casas ya salen de su época al generarse; aquí se arregla el centro (la campana) y lo que quede de
        // anacrónico en lo que esté cargado. Basta con que esté cargado el centro.
        if (!cargada(nivel, new BoundingBox(campana).inflatedBy(12))) return;
        datos.hechas.add(clave);
        datos.setDirty();
        if (epoca == Epoca.PREHISTORICA) PobladoPrehistorico.levantar(nivel, inicio, campana);   // centro y faroles
        poblar(nivel, inicio, epoca);
    }

    /** ¿Están cargados todos los chunks de esa caja? */
    /**
     * Si una aldea se ha quedado sin campana (se rompió al generarse, la quitó alguien…), sus habitantes no la
     * reconocen como aldea. Esto le pone una nueva en su plaza (la primera pieza de la estructura), en un sitio libre
     * con suelo firme lo más cerca posible del centro. Devuelve la campana (la que había o la nueva), o null si no se
     * está en una aldea o aún no está cargada.
     */
    @Nullable
    public static BlockPos reponerCampana(ServerLevel nivel, BlockPos pos) {
        StructureStart inicio = aldeaEn(nivel, pos);
        if (inicio == null || inicio.getPieces().isEmpty()) return null;
        BoundingBox plaza = inicio.getPieces().get(0).getBoundingBox();
        if (!cargada(nivel, plaza)) return null;
        for (BlockPos p : BlockPos.betweenClosed(plaza.minX(), plaza.minY(), plaza.minZ(), plaza.maxX(), plaza.maxY(), plaza.maxZ())) {
            if (nivel.getBlockState(p).is(Blocks.BELL)) return p.immutable();
        }
        BlockPos c = plaza.getCenter();
        for (int r = 0; r <= 7; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int x = c.getX() + dx, z = c.getZ() + dz;
                    BlockPos sitio = new BlockPos(x, nivel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                    if (nivel.getBlockState(sitio).isAir()
                            && nivel.getBlockState(sitio.below()).isFaceSturdy(nivel, sitio.below(), net.minecraft.core.Direction.UP)) {
                        nivel.setBlock(sitio, Blocks.BELL.defaultBlockState()
                                .setValue(net.minecraft.world.level.block.BellBlock.ATTACHMENT,
                                        net.minecraft.world.level.block.state.properties.BellAttachType.FLOOR), 3);
                        com.mojang.logging.LogUtils.getLogger().info("Yonga - la aldea de {} no tenía campana: puesta en {}",
                                c.toShortString(), sitio.toShortString());
                        return sitio;
                    }
                }
            }
        }
        return null;
    }

    public static boolean cargada(ServerLevel nivel, BoundingBox caja) {
        for (int cx = caja.minX() >> 4; cx <= caja.maxX() >> 4; cx++) {
            for (int cz = caja.minZ() >> 4; cz <= caja.maxZ() >> 4; cz++) {
                if (!nivel.hasChunk(cx, cz)) return false;
            }
        }
        return true;
    }

    /**
     * Si en lo cargado del pueblo hay más camas que habitantes (porque las casas nuevas no traían aldeanos), aparecen
     * habitantes de la época junto a las camas que sobran.
     */
    private static void poblar(ServerLevel nivel, StructureStart inicio, Epoca epoca) {
        BoundingBox caja = inicio.getBoundingBox();
        java.util.List<BlockPos> camas = new java.util.ArrayList<>();
        for (int cx = caja.minX() >> 4; cx <= caja.maxX() >> 4; cx++) {
            for (int cz = caja.minZ() >> 4; cz <= caja.maxZ() >> 4; cz++) {
                if (!nivel.hasChunk(cx, cz)) continue;
                for (BlockPos p : BlockPos.betweenClosed(cx << 4, Math.max(caja.minY(), nivel.getMinBuildHeight()), cz << 4,
                        (cx << 4) + 15, Math.min(caja.maxY(), nivel.getMaxBuildHeight() - 1), (cz << 4) + 15)) {
                    BlockState s = nivel.getBlockState(p);
                    if (s.getBlock() instanceof net.minecraft.world.level.block.BedBlock
                            && s.getValue(net.minecraft.world.level.block.BedBlock.PART)
                            == net.minecraft.world.level.block.state.properties.BedPart.HEAD) {
                        camas.add(p.immutable());
                    }
                }
            }
        }
        AABB zona = new AABB(caja.minX(), caja.minY() - 8, caja.minZ(), caja.maxX() + 1, caja.maxY() + 8, caja.maxZ() + 1);
        int hay = nivel.getEntitiesOfClass(Habitante.class, zona).size();
        RandomSource azar = nivel.random;
        for (int i = hay; i < camas.size(); i++) {
            BlockPos cama = camas.get(i);
            Habitante h = com.lucarita.yonga.modulos.habitantes.ModuloHabitantes.HABITANTE.get().create(nivel);
            if (h == null) return;
            h.moveTo(cama.getX() + 0.5, cama.getY() + 0.6, cama.getZ() + 0.5, azar.nextFloat() * 360.0f, 0.0f);
            h.generar(epoca, Habitante.etapaAlAzar(azar), azar);
            h.finalizeSpawn(nivel, nivel.getCurrentDifficultyAt(cama), net.minecraft.world.entity.MobSpawnType.STRUCTURE, null, null);
            nivel.addFreshEntity(h);
        }
    }

    /** ¿Ya está levantada la aldea de su época? (la taberna espera a que lo esté, para no acabar derribada) */
    public static boolean lista(ServerLevel nivel, BlockPos campana) {
        StructureStart inicio = aldeaEn(nivel, campana);
        return inicio == null || de(nivel).hechas.contains(inicio.getChunkPos().toLong());
    }

    /** La época de la aldea de una campana (la guardada; si no hay, la medieval). */
    public static Epoca epocaDeCampana(ServerLevel nivel, BlockPos campana) {
        StructureStart inicio = aldeaEn(nivel, campana);
        if (inicio == null) return Epoca.MEDIEVAL;
        Integer e = de(nivel).epoca.get(inicio.getChunkPos().toLong());
        return e == null ? Epoca.MEDIEVAL : Epoca.de(e);
    }

    // =====================================================================================================
    // UTILIDADES PARA LOS CONSTRUCTORES
    // =====================================================================================================

    /** Pone un bloque sin avisar a los vecinos (para que nada se caiga a medias mientras se derriba y se construye). */
    public static void poner(ServerLevel nivel, BlockPos p, BlockState estado) {
        nivel.setBlock(p, estado, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    /** Terreno natural (se respeta al derribar): tierra, piedra, arena, nieve, agua… */
    public static boolean natural(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.SAND)
                || s.is(Blocks.SANDSTONE) || s.is(Blocks.RED_SANDSTONE) || s.is(Blocks.GRAVEL) || s.is(Blocks.SNOW_BLOCK)
                || s.is(Blocks.SNOW) || s.is(Blocks.POWDER_SNOW) || s.is(Blocks.ICE) || s.is(Blocks.PACKED_ICE)
                || s.is(BlockTags.TERRACOTTA) || s.is(Blocks.CLAY) || s.is(Blocks.WATER) || s.is(Blocks.LAVA)
                || s.is(BlockTags.COAL_ORES) || s.is(BlockTags.IRON_ORES) || s.is(BlockTags.COPPER_ORES)
                || s.is(Blocks.MUD) || s.is(Blocks.CALCITE) || s.is(Blocks.TUFF);
    }

    // =====================================================================================================
    // GUARDAR
    // =====================================================================================================

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag lista = new ListTag();
        this.epoca.forEach((clave, e) -> {
            CompoundTag c = new CompoundTag();
            c.putLong("aldea", clave);
            c.putInt("epoca", e);
            c.putBoolean("hecha", this.hechas.contains(clave));
            lista.add(c);
        });
        nbt.put("aldeas", lista);
        return nbt;
    }

    public static Poblados cargar(CompoundTag nbt) {
        Poblados p = new Poblados();
        ListTag lista = nbt.getList("aldeas", 10);
        for (int i = 0; i < lista.size(); i++) {
            CompoundTag c = lista.getCompound(i);
            long clave = c.getLong("aldea");
            p.epoca.put(clave, c.getInt("epoca"));
            if (c.getBoolean("hecha")) p.hechas.add(clave);
        }
        return p;
    }
}
