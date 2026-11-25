package com.lucarita.yonga.modulos.habitantes.aldea;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * La aldea PREHISTÓRICA: sobre el trazado de una aldea de siempre (sus calles de tierra se quedan, que ya son senderos),
 * cada casa se derriba y en su sitio se levanta una CHOZA redonda u ovalada del tamaño del solar:
 *   · paredes de barro (adobe) con postes de madera y un zuncho de troncos arriba;
 *   · tejado cónico de paja (balas de heno) escalonado;
 *   · suelo de barro, la puerta hacia la calle, y dentro: lechos de pieles (camas marrones), el puesto de trabajo que
 *     tenía la casa, un hogar apagado en el centro y una antorcha.
 * Los huertos, corrales y establos se quedan (ya hay agricultura y ganado). El centro del pueblo se convierte en un
 * claro con la campana sobre un tótem y un fuego rodeado de piedras. Faroles y cristales desaparecen.
 */
public final class PobladoPrehistorico {

    private PobladoPrehistorico() {}

    /** Lo que ha quedado de una choza, para amueblarla. */
    public record Choza(BlockPos puerta, BlockPos fuera, BlockPos centro, Direction haciaPuerta, int suelo,
                        List<BlockPos> libres, List<BlockPos> junto) {}

    private static final BlockState AIRE = Blocks.AIR.defaultBlockState();
    private static final BlockState BARRO = Blocks.PACKED_MUD.defaultBlockState();
    private static final BlockState ADOBE = Blocks.MUD_BRICKS.defaultBlockState();
    private static final BlockState POSTE = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
    private static final BlockState PAJA = Blocks.HAY_BLOCK.defaultBlockState();

    // =====================================================================================================
    // LA ALDEA ENTERA
    // =====================================================================================================

    static void levantar(ServerLevel nivel, StructureStart inicio, BlockPos campana) {
        RandomSource azar = RandomSource.create(inicio.getChunkPos().toLong() ^ 0x50524548L);
        List<BoundingBox> rehechas = new ArrayList<>();
        for (StructurePiece pieza : inicio.getPieces()) {
            if (!(pieza instanceof PoolElementStructurePiece pe)) continue;
            String nombre = pe.getElement().toString();
            BoundingBox caja = pieza.getBoundingBox();
            if (!Poblados.cargada(nivel, caja) || nombre.contains("yonga:")) continue;   // las nuestras ya están bien
            if (caja.isInside(campana) || nombre.contains("town_centers")) {
                centro(nivel, caja, campana, azar);
                rehechas.add(caja);
            } else if (nombre.contains("houses/") && !nombre.contains("farm") && !nombre.contains("animal_pen")
                    && !nombre.contains("stable") && !nombre.contains("accessory")) {
                choza(nivel, caja, null, true);
                rehechas.add(caja);
            } else {
                anacronismos(nivel, caja, campana);
            }
        }
        // Los que se hayan quedado dentro de una pared salen a la calle
        for (BoundingBox caja : rehechas) {
            AABB zona = AABB.of(caja).inflate(1);
            for (LivingEntity e : nivel.getEntitiesOfClass(LivingEntity.class, zona)) {
                BlockPos p = e.blockPosition();
                if (!nivel.getBlockState(p).getCollisionShape(nivel, p).isEmpty()
                        || !nivel.getBlockState(p.above()).getCollisionShape(nivel, p.above()).isEmpty()) {
                    BlockPos libre = nivel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            new BlockPos(caja.maxX() + 2, 0, caja.getCenter().getZ()));
                    e.teleportTo(libre.getX() + 0.5, libre.getY(), libre.getZ() + 0.5);
                }
            }
        }
    }

    // =====================================================================================================
    // CHOZA
    // =====================================================================================================

    private static boolean esPuesto(BlockState s) {
        return s.is(Blocks.COMPOSTER) || s.is(Blocks.SMOKER) || s.is(Blocks.BLAST_FURNACE) || s.is(Blocks.BARREL)
                || s.is(Blocks.FLETCHING_TABLE) || s.is(Blocks.STONECUTTER) || s.is(Blocks.LOOM) || s.is(Blocks.BREWING_STAND)
                || s.is(Blocks.SMITHING_TABLE) || s.is(Blocks.GRINDSTONE) || s.is(BlockTags.CAULDRONS) || s.is(BlockTags.ANVIL);
    }

    /** El suelo de fuera según el sitio: arena en el desierto, nieve donde nieva, hierba en lo demás. */
    private static BlockState sueloFuera(ServerLevel nivel, BlockPos p) {
        Holder<Biome> bioma = nivel.getBiome(p);
        if (bioma.is(Biomes.DESERT)) return Blocks.SAND.defaultBlockState();
        if (bioma.value().coldEnoughToSnow(p)) return Blocks.SNOW_BLOCK.defaultBlockState();
        return Blocks.GRASS_BLOCK.defaultBlockState();
    }

    private static double elipse(double x, double z, double cx, double cz, double rx, double rz) {
        if (rx <= 0 || rz <= 0) return 99;
        double a = (x - cx) / rx, b = (z - cz) / rz;
        return a * a + b * b;
    }

    /**
     * Derriba lo que haya en el solar (salvo el terreno) y levanta una choza que ocupa casi todo. Si conservar, la
     * choza se queda con el puesto de trabajo y las camas que tenía la casa.
     *
     * @param puerta hacia dónde mira la puerta (null: hacia el lado con más camino de tierra)
     */
    public static Choza choza(ServerLevel nivel, BoundingBox caja, @Nullable Direction puerta, boolean conservar) {
        int suelo = caja.minY();
        // Lo que tenía la casa
        BlockState puesto = null;
        int camas = 0;
        if (conservar) {
            for (BlockPos p : BlockPos.betweenClosed(caja.minX(), caja.minY(), caja.minZ(), caja.maxX(), caja.maxY(), caja.maxZ())) {
                BlockState s = nivel.getBlockState(p);
                if (puesto == null && esPuesto(s)) puesto = s.getBlock().defaultBlockState();
                if (s.getBlock() instanceof BedBlock && s.getValue(BedBlock.PART) == BedPart.HEAD) camas++;
            }
        }
        // Derribar: fuera todo lo que no es terreno
        for (BlockPos p : BlockPos.betweenClosed(caja.minX(), caja.minY(), caja.minZ(), caja.maxX(), caja.maxY(), caja.maxZ())) {
            BlockState s = nivel.getBlockState(p);
            if (!s.isAir() && (!Poblados.natural(s) || !s.getFluidState().isEmpty() && p.getY() > caja.minY())) {
                Poblados.poner(nivel, p.immutable(), AIRE);
            }
        }
        // Tapar con tierra lo que era el suelo de la casa
        BlockState fuera = sueloFuera(nivel, caja.getCenter());
        for (int x = caja.minX(); x <= caja.maxX(); x++) {
            for (int z = caja.minZ(); z <= caja.maxZ(); z++) {
                BlockPos p = new BlockPos(x, suelo, z);
                if (nivel.getBlockState(p).isAir() && !nivel.getBlockState(p.below()).isAir()) {
                    Poblados.poner(nivel, p, fuera.is(Blocks.SNOW_BLOCK) ? Blocks.DIRT.defaultBlockState() : fuera);
                }
            }
        }

        // La planta: una elipse dentro del solar (un bloque de margen)
        int ancho = caja.getXSpan(), fondo = caja.getZSpan();
        double cx = caja.minX() + (ancho - 1) / 2.0, cz = caja.minZ() + (fondo - 1) / 2.0;
        double rx = Math.max(2.0, ancho / 2.0 - 1.0), rz = Math.max(2.0, fondo / 2.0 - 1.0);
        Set<Long> dentro = new HashSet<>(), pared = new HashSet<>();
        for (int x = caja.minX() - 1; x <= caja.maxX() + 1; x++) {
            for (int z = caja.minZ() - 1; z <= caja.maxZ() + 1; z++) {
                if (elipse(x, z, cx, cz, rx + 0.3, rz + 0.3) <= 1.0) dentro.add(BlockPos.asLong(x, 0, z));
            }
        }
        for (long c : dentro) {
            int x = BlockPos.getX(c), z = BlockPos.getZ(c);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (!dentro.contains(BlockPos.asLong(x + d.getStepX(), 0, z + d.getStepZ()))) {
                    pared.add(c);
                    break;
                }
            }
        }

        // ¿Hacia dónde la puerta? Hacia el lado con más camino de tierra alrededor
        if (puerta == null) {
            int mejor = -1;
            puerta = Direction.SOUTH;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                int n = 0;
                for (int i = -3; i <= Math.max(ancho, fondo) + 3; i++) {
                    int x = d == Direction.EAST ? caja.maxX() + 1 : d == Direction.WEST ? caja.minX() - 1 : caja.minX() + i;
                    int z = d == Direction.SOUTH ? caja.maxZ() + 1 : d == Direction.NORTH ? caja.minZ() - 1 : caja.minZ() + i;
                    for (int y = suelo - 2; y <= suelo + 1; y++) {
                        if (nivel.getBlockState(new BlockPos(x, y, z)).is(Blocks.DIRT_PATH)) n++;
                    }
                }
                if (n > mejor) {
                    mejor = n;
                    puerta = d;
                }
            }
        }

        // Suelo, hueco interior y paredes
        for (long c : dentro) {
            int x = BlockPos.getX(c), z = BlockPos.getZ(c);
            Poblados.poner(nivel, new BlockPos(x, suelo, z), BARRO);
            for (int y = suelo + 1; y <= suelo + 4; y++) Poblados.poner(nivel, new BlockPos(x, y, z), AIRE);
        }
        for (long c : pared) {
            int x = BlockPos.getX(c), z = BlockPos.getZ(c);
            boolean poste = Math.floorMod(x * 3 + z * 5, 4) == 0;
            for (int y = suelo + 1; y <= suelo + 2; y++) Poblados.poner(nivel, new BlockPos(x, y, z), poste ? POSTE : ADOBE);
            Poblados.poner(nivel, new BlockPos(x, suelo + 3, z), POSTE.setValue(RotatedPillarBlock.AXIS,
                    rx >= rz ? Direction.Axis.X : Direction.Axis.Z));
        }
        // Tejado cónico de paja, escalonado y con un poco de alero
        for (int k = 0; ; k++) {
            double ax = rx + 1.0 - k * 0.9, az = rz + 1.0 - k * 0.9;
            int y = suelo + 4 + k;
            if (ax < 0.8 || az < 0.8) {
                Poblados.poner(nivel, BlockPos.containing(cx, y, cz), PAJA);
                break;
            }
            for (int x = (int) Math.floor(cx - ax); x <= (int) Math.ceil(cx + ax); x++) {
                for (int z = (int) Math.floor(cz - az); z <= (int) Math.ceil(cz + az); z++) {
                    if (elipse(x, z, cx, cz, ax, az) <= 1.0 && elipse(x, z, cx, cz, ax - 1.6, az - 1.6) > 1.0) {
                        Poblados.poner(nivel, new BlockPos(x, y, z), PAJA);
                    }
                }
            }
        }

        // La puerta: el trozo de pared más en medio del lado elegido
        int px = (int) Math.round(cx), pz = (int) Math.round(cz);
        BlockPos hueco = null;
        int mejor = Integer.MIN_VALUE;
        for (long c : pared) {
            int x = BlockPos.getX(c), z = BlockPos.getZ(c);
            int avance = x * puerta.getStepX() + z * puerta.getStepZ();
            int lado = Math.abs(puerta.getAxis() == Direction.Axis.X ? z - pz : x - px);
            if (lado > 0) continue;
            if (avance > mejor) {
                mejor = avance;
                hueco = new BlockPos(x, suelo, z);
            }
        }
        if (hueco == null) hueco = new BlockPos(px, suelo, pz).relative(puerta, (int) Math.round(puerta.getAxis() == Direction.Axis.X ? rx : rz));
        for (int y = 1; y <= 2; y++) Poblados.poner(nivel, hueco.above(y), AIRE);
        // Un caminito de tierra de la puerta a la calle
        BlockPos fueraP = hueco.relative(puerta);
        for (int i = 0; i < 6; i++) {
            BlockPos q = hueco.relative(puerta, i + 1);
            if (dentro.contains(BlockPos.asLong(q.getX(), 0, q.getZ()))) continue;
            if (!caja.isInside(q) && i > 1) break;
            BlockState debajo = nivel.getBlockState(q);
            if (debajo.is(BlockTags.DIRT) || debajo.is(Blocks.GRASS_BLOCK)) {         // en la arena, sin camino
                Poblados.poner(nivel, q, Blocks.DIRT_PATH.defaultBlockState());
            }
            for (int y = 1; y <= 2; y++) {
                if (!nivel.getBlockState(q.above(y)).is(Blocks.HAY_BLOCK)) Poblados.poner(nivel, q.above(y), AIRE);
            }
        }

        // Lo de dentro: casillas libres y las que dan a la pared
        BlockPos puertaDentro = hueco.relative(puerta.getOpposite()).above();
        List<BlockPos> libres = new ArrayList<>(), junto = new ArrayList<>();
        for (long c : dentro) {
            if (pared.contains(c)) continue;
            BlockPos p = new BlockPos(BlockPos.getX(c), suelo + 1, BlockPos.getZ(c));
            if (p.equals(puertaDentro)) continue;
            libres.add(p);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (pared.contains(BlockPos.asLong(p.getX() + d.getStepX(), 0, p.getZ() + d.getStepZ()))) {
                    junto.add(p);
                    break;
                }
            }
        }
        BlockPos centro = BlockPos.containing(cx, suelo + 1, cz);
        Choza ch = new Choza(hueco.above(), fueraP.above(), centro, puerta, suelo, libres, junto);
        if (conservar) amueblar(nivel, ch, puesto, Math.max(1, camas));
        return ch;
    }

    /** Lo de dentro de una casa: hogar, puesto de trabajo (al fondo), lechos de pieles y una antorcha. */
    private static void amueblar(ServerLevel nivel, Choza ch, @Nullable BlockState puesto, int camas) {
        Set<BlockPos> usadas = new HashSet<>();
        // Hogar apagado en el centro (si cabe sin taparlo todo)
        if (ch.libres().size() >= 12 && ch.libres().contains(ch.centro())) {
            Poblados.poner(nivel, ch.centro(), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
            usadas.add(ch.centro());
            for (Direction d : Direction.Plane.HORIZONTAL) usadas.add(ch.centro().relative(d));   // paso alrededor
        }
        // El puesto de trabajo, contra la pared del fondo (lo más lejos de la puerta)
        List<BlockPos> fondo = new ArrayList<>(ch.junto());
        fondo.sort(Comparator.comparingDouble(p -> -p.distSqr(ch.puerta())));
        if (puesto != null) {
            for (BlockPos p : fondo) {
                if (usadas.contains(p)) continue;
                Poblados.poner(nivel, p, puesto);
                usadas.add(p);
                break;
            }
        }
        // Lechos: la cabecera contra la pared y los pies hacia el centro
        int puestas = 0;
        for (BlockPos cabeza : fondo) {
            if (puestas >= camas) break;
            if (usadas.contains(cabeza)) continue;
            Direction haciaDentro = null;
            double mejor = cabeza.distSqr(ch.centro());
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos pies = cabeza.relative(d);
                if (ch.libres().contains(pies) && !usadas.contains(pies) && pies.distSqr(ch.centro()) < mejor) {
                    mejor = pies.distSqr(ch.centro());
                    haciaDentro = d;
                }
            }
            if (haciaDentro == null) continue;
            BlockPos pies = cabeza.relative(haciaDentro);
            BlockState cama = Blocks.BROWN_BED.defaultBlockState().setValue(BedBlock.FACING, haciaDentro.getOpposite());
            Poblados.poner(nivel, pies, cama.setValue(BedBlock.PART, BedPart.FOOT));
            Poblados.poner(nivel, cabeza, cama.setValue(BedBlock.PART, BedPart.HEAD));
            usadas.add(pies);
            usadas.add(cabeza);
            for (Direction d : Direction.Plane.HORIZONTAL) {                     // que no se pegue otra cama
                usadas.add(cabeza.relative(d));
            }
            puestas++;
        }
        // Una antorcha junto a la pared, cerca de la puerta
        List<BlockPos> cerca = new ArrayList<>(ch.junto());
        cerca.sort(Comparator.comparingDouble(p -> p.distSqr(ch.puerta())));
        for (BlockPos p : cerca) {
            if (usadas.contains(p) || p.distSqr(ch.puerta()) < 2) continue;
            Poblados.poner(nivel, p, Blocks.TORCH.defaultBlockState());
            break;
        }
    }

    // =====================================================================================================
    // CENTRO DEL POBLADO
    // =====================================================================================================

    private static void centro(ServerLevel nivel, BoundingBox caja, BlockPos campana, RandomSource azar) {
        for (BlockPos p : BlockPos.betweenClosed(caja.minX(), caja.minY(), caja.minZ(), caja.maxX(), caja.maxY(), caja.maxZ())) {
            if (p.equals(campana)) continue;
            BlockState s = nivel.getBlockState(p);
            // Fuera también el agua de las fuentes: sin su pilón lo inundaría todo
            if (!s.isAir() && (!Poblados.natural(s) || !s.getFluidState().isEmpty())) Poblados.poner(nivel, p.immutable(), AIRE);
        }
        BlockState fuera = sueloFuera(nivel, caja.getCenter());
        for (int x = caja.minX(); x <= caja.maxX(); x++) {
            for (int z = caja.minZ(); z <= caja.maxZ(); z++) {
                BlockPos p = new BlockPos(x, caja.minY(), z);
                if (nivel.getBlockState(p).isAir() && !nivel.getBlockState(p.below()).isAir()) {
                    Poblados.poner(nivel, p, fuera.is(Blocks.SNOW_BLOCK) ? Blocks.DIRT.defaultBlockState() : fuera);
                }
            }
        }
        // La campana, de pie sobre un tótem de troncos que llega al suelo
        BlockState campanaEstado = nivel.getBlockState(campana);
        if (campanaEstado.getBlock() instanceof BellBlock) {
            Poblados.poner(nivel, campana, campanaEstado.setValue(BellBlock.ATTACHMENT, BellAttachType.FLOOR));
            BlockPos p = campana.below();
            while (p.getY() > nivel.getMinBuildHeight() && (nivel.getBlockState(p).isAir() || !Poblados.natural(nivel.getBlockState(p)))) {
                Poblados.poner(nivel, p, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
                p = p.below();
            }
        }
        // El fuego del poblado: una hoguera rodeada de piedras, a unos pasos de la campana, con troncos para sentarse
        BlockPos fuego = suelo(nivel, campana.offset(4, 0, 1));
        List<BlockPos> piedras = new ArrayList<>();
        if (fuego != null) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = fuego.offset(dx, 0, dz);
                    if (dx == 0 && dz == 0) {
                        Poblados.poner(nivel, p.below(), Blocks.COBBLESTONE.defaultBlockState());
                        Poblados.poner(nivel, p, Blocks.CAMPFIRE.defaultBlockState());
                    } else {
                        Poblados.poner(nivel, p.below(), Blocks.COBBLESTONE.defaultBlockState());
                        Poblados.poner(nivel, p, Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState());
                        piedras.add(p);
                    }
                }
            }
            // Que el cerco de piedras se una (al ponerlo sin avisar a los vecinos se quedaba en postes sueltos)
            for (BlockPos p : piedras) {
                nivel.setBlock(p, net.minecraft.world.level.block.Block.updateFromNeighbourShapes(nivel.getBlockState(p), nivel, p),
                        net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            }
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos asiento = suelo(nivel, fuego.relative(d, 3));
                if (asiento == null) continue;
                Direction lado = d.getClockWise();
                Poblados.poner(nivel, asiento, Blocks.STRIPPED_OAK_LOG.defaultBlockState()
                        .setValue(RotatedPillarBlock.AXIS, lado.getAxis()));
            }
        }
        // Antorchas en palos alrededor
        int[][] palos = {{-4, -4}, {4, -4}, {-4, 4}, {6, 5}};
        for (int[] o : palos) {
            BlockPos p = suelo(nivel, campana.offset(o[0], 0, o[1]));
            if (p == null) continue;
            Poblados.poner(nivel, p, Blocks.OAK_FENCE.defaultBlockState());
            Poblados.poner(nivel, p.above(), Blocks.TORCH.defaultBlockState());
        }
        // Unas balas de paja y un cráneo en un poste, por darle vida
        BlockPos paja = suelo(nivel, campana.offset(-3, 0, 2));
        if (paja != null) {
            Poblados.poner(nivel, paja, PAJA);
            if (azar.nextBoolean()) Poblados.poner(nivel, paja.above(), PAJA);
        }
    }

    /** La primera casilla libre sobre el suelo en esa columna (o null si es agua). */
    @Nullable
    private static BlockPos suelo(ServerLevel nivel, BlockPos cerca) {
        BlockPos p = nivel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cerca);
        if (!nivel.getFluidState(p.below()).isEmpty()) return null;
        return p;
    }

    // =====================================================================================================
    // LO DEMÁS (calles, adornos, huertos)
    // =====================================================================================================

    private static void anacronismos(ServerLevel nivel, BoundingBox caja, BlockPos campana) {
        for (BlockPos p : BlockPos.betweenClosed(caja.minX(), caja.minY(), caja.minZ(), caja.maxX(), caja.maxY(), caja.maxZ())) {
            if (p.equals(campana)) continue;
            BlockState s = nivel.getBlockState(p);
            if (s.is(Blocks.LANTERN) || s.is(Blocks.SOUL_LANTERN)) {
                Poblados.poner(nivel, p.immutable(), s.getValue(net.minecraft.world.level.block.LanternBlock.HANGING)
                        ? AIRE : Blocks.TORCH.defaultBlockState());
            } else if (s.is(Blocks.GLASS) || s.is(Blocks.GLASS_PANE) || s.is(Blocks.IRON_BARS) || s.is(BlockTags.IMPERMEABLE)
                    || s.is(Blocks.BELL)) {
                Poblados.poner(nivel, p.immutable(), AIRE);
            } else if (s.is(BlockTags.WOODEN_DOORS) || s.is(BlockTags.TRAPDOORS) || s.is(BlockTags.WOOL_CARPETS)) {
                Poblados.poner(nivel, p.immutable(), AIRE);
            }
        }
    }
}
