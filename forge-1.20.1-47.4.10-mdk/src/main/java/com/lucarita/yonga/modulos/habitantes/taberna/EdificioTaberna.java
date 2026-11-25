package com.lucarita.yonga.modulos.habitantes.taberna;

import com.lucarita.yonga.modulos.habitantes.ModuloHabitantes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

/**
 * La taberna (de momento, medieval): una casa de 11 x 9 con zócalo de piedra, paredes de tablones con vigas de
 * abeto, tejado a dos aguas, barra con caldero (el puesto del tabernero) y barriles, dos mesas con sillas, faroles,
 * la cama del tabernero y el TABLÓN DE ENCARGOS junto a la puerta. La puerta mira hacia la campana.
 *
 * Se busca un sitio llano y libre (sin casas, caminos, agua ni árboles) a 16-40 bloques de la campana; se allana y
 * se construye por código. Coordenadas locales: x 0..10, z 0..8 (puerta en z = 0), y 0 = suelo.
 */
public final class EdificioTaberna {

    private EdificioTaberna() {}

    public static final int ANCHO = 11, FONDO = 9;

    /** Lo que deja construido: el tablón, el caldero (puesto del tabernero) y un sitio dentro para él. */
    public record Hecho(BlockPos tablon, BlockPos caldero, BlockPos dentro) {}

    private record Sitio(BlockPos origen, Rotation giro) {}

    /** Resultado de un intento: hecho, sin cargar (no cuenta) o sin sitio. */
    public static final Hecho SIN_CARGAR = new Hecho(BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO);

    /** Busca sitio y construye. null si no hay sitio; SIN_CARGAR si falta terreno por cargar. */
    @Nullable
    public static Hecho construir(ServerLevel nivel, BlockPos campana) {
        Sitio s = buscarSitio(nivel, campana);
        if (s == null) return null;
        if (s == SITIO_SIN_CARGAR) return SIN_CARGAR;
        return segunEpoca(nivel, s, campana);
    }

    /** La taberna de la época de la aldea (de momento, la choza grande prehistórica o la casa medieval). */
    private static Hecho segunEpoca(ServerLevel nivel, Sitio s, BlockPos campana) {
        var epoca = com.lucarita.yonga.modulos.habitantes.aldea.Poblados.epocaDeCampana(nivel, campana);
        if (epoca == com.lucarita.yonga.modulos.habitantes.Epoca.PREHISTORICA) return chozaGrande(nivel, s, campana);
        if (epoca == com.lucarita.yonga.modulos.habitantes.Epoca.ANTIGUA) return levantar(nivel, s, ROMANA);
        if (epoca == com.lucarita.yonga.modulos.habitantes.Epoca.MODERNA) return levantar(nivel, s, MESON);
        if (epoca == com.lucarita.yonga.modulos.habitantes.Epoca.INDUSTRIAL) return levantar(nivel, s, CANTINA);
        if (epoca == com.lucarita.yonga.modulos.habitantes.Epoca.CONTEMPORANEA) return levantar(nivel, s, BAR);
        return levantar(nivel, s);
    }

    /**
     * Prehistoria: la "taberna" es la choza grande del guardián del fuego: el caldero al fondo, el tablón de encargos
     * junto a la puerta, su lecho y una hoguera encendida delante de la puerta.
     */
    private static Hecho chozaGrande(ServerLevel nivel, Sitio s, BlockPos campana) {
        BlockPos a = local(s, 0, 0, 0), b = local(s, ANCHO - 1, 14, FONDO - 1);
        net.minecraft.world.level.levelgen.structure.BoundingBox caja =
                net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(a, b);
        BlockPos medio = caja.getCenter();
        Direction haciaCampana = Direction.getNearest(campana.getX() - medio.getX(), 0, campana.getZ() - medio.getZ());
        if (haciaCampana.getAxis() == Direction.Axis.Y) haciaCampana = Direction.SOUTH;
        var ch = com.lucarita.yonga.modulos.habitantes.aldea.PobladoPrehistorico.choza(nivel, caja, haciaCampana, false);
        java.util.List<BlockPos> junto = new java.util.ArrayList<>(ch.junto());
        junto.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(ch.puerta())));
        java.util.Set<BlockPos> usadas = new java.util.HashSet<>();
        // El tablón, en la pared más cerca de la puerta, mirando hacia dentro
        BlockPos tablon = junto.isEmpty() ? ch.centro() : junto.get(Math.min(1, junto.size() - 1));
        Direction cara = Direction.SOUTH;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (!ch.libres().contains(tablon.relative(d)) && !nivel.getBlockState(tablon.relative(d)).isAir()) {
                cara = d.getOpposite();
                break;
            }
        }
        nivel.setBlock(tablon, ModuloHabitantes.TABLON.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, cara),
                Block.UPDATE_ALL);
        usadas.add(tablon);
        // El caldero al fondo
        BlockPos caldero = junto.isEmpty() ? ch.centro() : junto.get(junto.size() - 1);
        nivel.setBlock(caldero, Blocks.CAULDRON.defaultBlockState(), Block.UPDATE_ALL);
        usadas.add(caldero);
        // Su lecho, en otra pared
        for (int i = junto.size() - 2; i >= 0; i--) {
            BlockPos cabeza = junto.get(i);
            if (usadas.contains(cabeza) || cabeza.distSqr(caldero) < 3) continue;
            Direction dentro = null;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos pies = cabeza.relative(d);
                if (ch.libres().contains(pies) && !usadas.contains(pies) && pies.distSqr(ch.centro()) < cabeza.distSqr(ch.centro())) {
                    dentro = d;
                    break;
                }
            }
            if (dentro == null) continue;
            BlockState cama = Blocks.BROWN_BED.defaultBlockState().setValue(BedBlock.FACING, dentro.getOpposite());
            nivel.setBlock(cabeza.relative(dentro), cama.setValue(BedBlock.PART, BedPart.FOOT), Block.UPDATE_CLIENTS);
            nivel.setBlock(cabeza, cama.setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_ALL);
            break;
        }
        // La hoguera delante de la puerta, con un cerco de piedras
        BlockPos fuego = nivel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ch.fuera().relative(ch.haciaPuerta(), 2));
        nivel.setBlock(fuego.below(), Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
        nivel.setBlock(fuego, Blocks.CAMPFIRE.defaultBlockState(), Block.UPDATE_ALL);
        return new Hecho(tablon, caldero, ch.centro());
    }

    /** Construye a la fuerza aquí mismo (comando), con la puerta mirando hacia la campana. */
    public static Hecho construirAqui(ServerLevel nivel, BlockPos campana, BlockPos aqui) {
        Direction haciaCampana = Direction.getNearest(campana.getX() - aqui.getX(), 0, campana.getZ() - aqui.getZ());
        Rotation giro = giroHacia(haciaCampana);
        int[] c = girar(ANCHO / 2, FONDO / 2, giro);
        return segunEpoca(nivel, new Sitio(new BlockPos(aqui.getX() - c[0], aqui.getY() - 1, aqui.getZ() - c[1]), giro), campana);
    }

    // =====================================================================================================
    // SITIO
    // =====================================================================================================

    private static BlockPos local(Sitio s, int x, int y, int z) {
        return s.origen.offset(girar(x, z, s.giro)[0], y, girar(x, z, s.giro)[1]);
    }

    private static int[] girar(int x, int z, Rotation r) {
        return switch (r) {
            case CLOCKWISE_90 -> new int[]{-z, x};
            case CLOCKWISE_180 -> new int[]{-x, -z};
            case COUNTERCLOCKWISE_90 -> new int[]{z, -x};
            default -> new int[]{x, z};
        };
    }

    private static final Sitio SITIO_SIN_CARGAR = new Sitio(BlockPos.ZERO, Rotation.NONE);

    private static Rotation giroHacia(Direction d) {
        return switch (d) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** El mejor sitio (el que menos hay que mover), SITIO_SIN_CARGAR si algún candidato no está cargado, o null. */
    @Nullable
    private static Sitio buscarSitio(ServerLevel nivel, BlockPos campana) {
        Sitio mejor = null;
        int mejorCoste = Integer.MAX_VALUE;
        boolean faltaba = false;
        for (int distancia = 16; distancia <= 48; distancia += 4) {
            for (int i = 0; i < 16; i++) {
                double ang = i * Math.PI / 8 + distancia * 0.37;
                int cx = campana.getX() + (int) Math.round(Math.cos(ang) * distancia);
                int cz = campana.getZ() + (int) Math.round(Math.sin(ang) * distancia);
                // La puerta (norte en local) mira hacia la campana
                Rotation giro = giroHacia(Direction.getNearest(campana.getX() - cx, 0, campana.getZ() - cz));
                int[] c = girar(ANCHO / 2, FONDO / 2, giro);
                BlockPos origen = new BlockPos(cx - c[0], 0, cz - c[1]);
                long r = evaluar(nivel, origen, giro);
                if (r == Long.MIN_VALUE) { faltaba = true; continue; }
                if (r == Long.MAX_VALUE) continue;
                int base = (int) (r >> 32), coste = (int) r + distancia;   // a igualdad, mejor cerca
                if (coste < mejorCoste) {
                    mejorCoste = coste;
                    mejor = new Sitio(origen.atY(base - 1), giro);     // el suelo de la taberna, a ras de tierra
                }
            }
            if (mejor != null && mejorCoste < 40) break;        // ya hay uno bueno: no hace falta irse más lejos
        }
        if (mejor != null) return mejor;
        return faltaba ? SITIO_SIN_CARGAR : null;
    }

    private static boolean deArbol(BlockState b) {
        return b.is(BlockTags.LOGS) || b.is(BlockTags.LEAVES) || b.canBeReplaced() || b.isAir()
                || b.is(Blocks.MUSHROOM_STEM) || b.is(Blocks.BROWN_MUSHROOM_BLOCK) || b.is(Blocks.RED_MUSHROOM_BLOCK)
                || b.is(Blocks.BAMBOO) || b.is(Blocks.CACTUS) || b.is(Blocks.SUGAR_CANE) || b.is(BlockTags.FLOWERS)
                || b.is(Blocks.SWEET_BERRY_BUSH) || b.is(Blocks.MOSS_CARPET) || b.is(Blocks.SNOW) || b.is(Blocks.PUMPKIN)
                || b.is(Blocks.MELON) || b.is(Blocks.MOSS_BLOCK);
    }

    /**
     * Evalúa un sitio: Long.MIN_VALUE si falta un chunk, Long.MAX_VALUE si no vale (agua, casas, caminos, campos o
     * demasiada cuesta), o (base << 32 | coste). Los árboles y los montículos no lo impiden: la obra los quita.
     */
    private static long evaluar(ServerLevel nivel, BlockPos origen, Rotation giro) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        long suma = 0;
        int n = 0, coste = 0;
        Sitio s = new Sitio(origen, giro);
        int[] alturas = new int[(ANCHO + 2) * (FONDO + 3)];
        for (int x = -1; x <= ANCHO; x++) {
            for (int z = -2; z <= FONDO; z++) {
                BlockPos p = local(s, x, 0, z);
                if (!nivel.hasChunkAt(p)) return Long.MIN_VALUE;
                int h = nivel.getHeight(Heightmap.Types.WORLD_SURFACE, p.getX(), p.getZ());
                // Bajar por troncos, hojas, hierba y nieve hasta el suelo de verdad
                BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(p.getX(), h - 1, p.getZ());
                int tope = h - 40;
                while (m.getY() > tope && deArbol(nivel.getBlockState(m)) && nivel.getFluidState(m).isEmpty()) m.move(0, -1, 0);
                if (h - 1 - m.getY() > 2) coste += 2;                     // árbol que talar
                BlockState suelo = nivel.getBlockState(m);
                if (!nivel.getFluidState(m).isEmpty()) return Long.MAX_VALUE;   // agua o lava
                if (!natural(suelo)) return Long.MAX_VALUE;                // casas, caminos, campos…
                int g = m.getY() + 1;
                alturas[n] = g;
                min = Math.min(min, g);
                max = Math.max(max, g);
                suma += g;
                n++;
            }
        }
        if (max - min > 6) return Long.MAX_VALUE;
        int base = (int) Math.round((double) suma / n);
        for (int i = 0; i < n; i++) coste += Math.abs(alturas[i] - base) * 3;   // tierra que mover
        // Encima no puede haber nada hecho por alguien (bloques que no sean terreno ni árbol)
        for (int x = -1; x <= ANCHO; x++) {
            for (int z = -1; z <= FONDO; z++) {
                for (int y = 0; y <= 11; y++) {
                    BlockState b = nivel.getBlockState(local(s, x, 0, z).atY(base + y));
                    if (!deArbol(b) && !natural(b)) return Long.MAX_VALUE;
                }
            }
        }
        return ((long) base << 32) | (coste & 0xFFFFFFFFL);
    }

    private static boolean natural(BlockState s) {
        return s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.PODZOL)
                || s.is(Blocks.SAND) || s.is(Blocks.RED_SAND) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.STONE)
                || s.is(Blocks.GRAVEL) || s.is(Blocks.MYCELIUM) || s.is(Blocks.SANDSTONE) || s.is(Blocks.ROOTED_DIRT)
                || s.is(Blocks.MUD) || s.is(BlockTags.TERRACOTTA) || s.is(Blocks.CLAY) || s.is(Blocks.DEEPSLATE)
                || s.is(Blocks.ANDESITE) || s.is(Blocks.DIORITE) || s.is(Blocks.GRANITE) || s.is(Blocks.TUFF);
    }

    // =====================================================================================================
    // CONSTRUIR
    // =====================================================================================================

    private static void poner(ServerLevel nivel, Sitio s, int x, int y, int z, BlockState estado) {
        nivel.setBlock(local(s, x, y, z), estado.rotate(s.giro), Block.UPDATE_ALL);
    }

    private static BlockState escalera(Block b, Direction mira, Half mitad) {
        return b.defaultBlockState().setValue(StairBlock.FACING, mira).setValue(StairBlock.HALF, mitad);
    }

    /**
     * Los materiales de la taberna (misma casa, distinto estilo): la medieval de madera y piedra; la romana (caupona)
     * de estuco blanco, columnas de cuarzo, suelo de piedra pulida, tejado de teja (ladrillo) y rejas en las ventanas.
     */
    private record Paleta(Block piedra, Block ladrillo, Block paredes, Block suelo, Block viga, Block vigaTumbada,
                          Block ventana, Block puerta, Block tejas, Block losa, boolean azotea) {}

    private static final Paleta MEDIEVAL = new Paleta(Blocks.COBBLESTONE, Blocks.STONE_BRICKS, Blocks.OAK_PLANKS,
            Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG, Blocks.GLASS_PANE, Blocks.SPRUCE_DOOR,
            Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, false);
    private static final Paleta ROMANA = new Paleta(Blocks.STONE_BRICKS, Blocks.SMOOTH_SANDSTONE, Blocks.SMOOTH_QUARTZ,
            Blocks.POLISHED_ANDESITE, Blocks.QUARTZ_PILLAR, Blocks.STRIPPED_OAK_LOG, Blocks.IRON_BARS, Blocks.OAK_DOOR,
            Blocks.BRICK_STAIRS, Blocks.BRICK_SLAB, true);
    /** Siglo XVII (mesón): encalado, zócalo de piedra, vigas de roble oscuro y tejado de teja. */
    private static final Paleta MESON = new Paleta(Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE, Blocks.WHITE_CONCRETE,
            Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.GLASS_PANE, Blocks.DARK_OAK_DOOR,
            Blocks.BRICK_STAIRS, Blocks.BRICK_SLAB, false);

    /** Hacia 1890 (cantina): ladrillo visto, zócalo de piedra, tablones de abeto y tejado de pizarra. */
    private static final Paleta CANTINA = new Paleta(Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.BRICKS,
            Blocks.SPRUCE_PLANKS, Blocks.STONE_BRICKS, Blocks.STRIPPED_SPRUCE_LOG, Blocks.GLASS_PANE, Blocks.SPRUCE_DOOR,
            Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, false);
    /** Hoy (el bar del pueblo): revoco blanco, columnas de cuarzo, suelo de baldosa, ventanas grandes y azotea. */
    private static final Paleta BAR = new Paleta(Blocks.SMOOTH_STONE, Blocks.POLISHED_ANDESITE, Blocks.WHITE_CONCRETE,
            Blocks.SMOOTH_QUARTZ, Blocks.QUARTZ_PILLAR, Blocks.QUARTZ_PILLAR, Blocks.GLASS_PANE, Blocks.BIRCH_DOOR,
            Blocks.BRICK_STAIRS, Blocks.BRICK_SLAB, true);

    private static Hecho levantar(ServerLevel nivel, Sitio s) {
        return levantar(nivel, s, MEDIEVAL);
    }

    private static Hecho levantar(ServerLevel nivel, Sitio s, Paleta pal) {
        BlockState aire = Blocks.AIR.defaultBlockState();
        BlockState piedra = pal.piedra().defaultBlockState();
        BlockState ladrillo = pal.ladrillo().defaultBlockState();
        BlockState tablones = pal.paredes().defaultBlockState();
        BlockState abeto = pal.suelo().defaultBlockState();
        BlockState viga = pal.viga().defaultBlockState();
        BlockState vigaX = pal.vigaTumbada().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        BlockState vigaZ = pal.vigaTumbada().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);

        // Despejar y cimientos
        for (int x = -1; x <= ANCHO; x++) {
            for (int z = -2; z <= FONDO; z++) {
                // El suelo que había (arena en el desierto, hierba en el campo…), para dejarlo igual alrededor
                BlockState antes = nivel.getBlockState(local(s, x, 0, z));
                BlockState debajo = nivel.getBlockState(local(s, x, -1, z));
                BlockState terreno = natural(antes) ? antes : natural(debajo) ? debajo : Blocks.GRASS_BLOCK.defaultBlockState();
                for (int y = 1; y <= 11; y++) poner(nivel, s, x, y, z, aire);
                // Lo que quede de árboles por encima del tejado, fuera también (que no se quede flotando)
                for (int y = 12; y <= 40; y++) {
                    BlockState b = nivel.getBlockState(local(s, x, y, z));
                    if (b.is(BlockTags.LOGS) || b.is(BlockTags.LEAVES) || b.is(Blocks.VINE)) poner(nivel, s, x, y, z, aire);
                }
                boolean dentro = x >= 0 && x < ANCHO && z >= 0 && z < FONDO;
                // Fuera, el terreno de siempre a ras de suelo; a la entrada, camino (solo si era hierba o tierra)
                BlockState suelo = dentro ? piedra : terreno;
                boolean entrada = (z == -1 || z == -2) && x >= 3 && x <= ANCHO - 4;
                if (entrada && (terreno.is(Blocks.GRASS_BLOCK) || terreno.is(Blocks.DIRT) || terreno.is(Blocks.COARSE_DIRT))) {
                    suelo = Blocks.DIRT_PATH.defaultBlockState();
                }
                poner(nivel, s, x, 0, z, suelo);
                poner(nivel, s, x, -1, z, dentro ? piedra : terreno.is(Blocks.GRASS_BLOCK) || terreno.is(Blocks.DIRT_PATH)
                        ? Blocks.DIRT.defaultBlockState() : terreno);
                for (int y = -2; y >= -7; y--) {
                    BlockPos p = local(s, x, y, z);
                    BlockState b = nivel.getBlockState(p);
                    if (!b.isAir() && !b.canBeReplaced() && b.getFluidState().isEmpty()) break;
                    poner(nivel, s, x, y, z, dentro ? piedra : Blocks.DIRT.defaultBlockState());
                }
            }
        }
        // Suelo, zócalo, paredes y vigas
        for (int x = 0; x < ANCHO; x++) {
            for (int z = 0; z < FONDO; z++) {
                boolean borde = x == 0 || x == ANCHO - 1 || z == 0 || z == FONDO - 1;
                boolean esquina = (x == 0 || x == ANCHO - 1) && (z == 0 || z == FONDO - 1);
                if (!borde) {
                    poner(nivel, s, x, 0, z, abeto);
                    poner(nivel, s, x, 4, z, abeto);                     // techo
                    continue;
                }
                poner(nivel, s, x, 0, z, ladrillo);
                for (int y = 1; y <= 3; y++) poner(nivel, s, x, y, z, esquina || (x == 5 && y < 3 && z != 0) ? viga : tablones);
                poner(nivel, s, x, 4, z, esquina ? viga : (z == 0 || z == FONDO - 1) ? vigaX : vigaZ);
            }
        }
        // Ventanas
        BlockState cristal = pal.ventana().defaultBlockState();
        int[][] ventanas = {{0, 2, 2}, {0, 2, 6}, {10, 2, 2}, {10, 2, 6}, {2, 2, 0}, {8, 2, 0}, {3, 2, 8}, {7, 2, 8}};
        for (int[] v : ventanas) poner(nivel, s, v[0], v[1], v[2], cristal);
        // Puerta (en el centro de la fachada) con faroles fuera
        BlockState puerta = pal.puerta().defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH);
        poner(nivel, s, 5, 1, 0, puerta.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        poner(nivel, s, 5, 2, 0, puerta.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        poner(nivel, s, 4, 2, -1, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH));
        poner(nivel, s, 6, 2, -1, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH));

        if (pal.azotea()) {
            // Azotea: el techo ya es la losa; pretil de cuarzo alrededor y un farol para que no salgan monstruos
            for (int x = 0; x < ANCHO; x++) {
                for (int z = 0; z < FONDO; z++) {
                    if (x == 0 || x == ANCHO - 1 || z == 0 || z == FONDO - 1) {
                        poner(nivel, s, x, 4, z, Blocks.QUARTZ_BRICKS.defaultBlockState());
                        poner(nivel, s, x, 5, z, Blocks.QUARTZ_SLAB.defaultBlockState());
                    } else {
                        poner(nivel, s, x, 4, z, Blocks.SMOOTH_STONE.defaultBlockState());
                    }
                }
            }
            poner(nivel, s, ANCHO / 2, 5, FONDO / 2, Blocks.LANTERN.defaultBlockState());
        } else {
            // Tejado a dos aguas (de delante a atrás), con hastiales
            Block tejas = pal.tejas();
            for (int k = 0; k <= 4; k++) {
                int y = 4 + k;
                for (int x = -1; x <= ANCHO; x++) {
                    poner(nivel, s, x, y, k - 1, escalera(tejas, Direction.SOUTH, Half.BOTTOM));
                    poner(nivel, s, x, y, FONDO - k, escalera(tejas, Direction.NORTH, Half.BOTTOM));
                }
                if (k >= 1) {
                    for (int z = k; z <= FONDO - 1 - k; z++) {
                        poner(nivel, s, 0, y, z, tablones);
                        poner(nivel, s, ANCHO - 1, y, z, tablones);
                    }
                }
            }
            for (int x = -1; x <= ANCHO; x++) {
                poner(nivel, s, x, 8, 4, x == -1 || x == ANCHO ? pal.losa().defaultBlockState() : tablones);
                poner(nivel, s, x, 9, 4, pal.losa().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
            }
        }

        // Barra: mostrador en z = 5 de x 1 a 3 (hueco en x 4 para pasar), detrás caldero y barriles
        for (int x = 1; x <= 3; x++) {
            poner(nivel, s, x, 1, 5, Blocks.SPRUCE_PLANKS.defaultBlockState());
            poner(nivel, s, x, 2, 5, Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        }
        poner(nivel, s, 1, 1, 7, Blocks.CAULDRON.defaultBlockState());
        BlockState barril = Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.SOUTH);
        poner(nivel, s, 2, 1, 7, barril);
        poner(nivel, s, 3, 1, 7, barril);
        poner(nivel, s, 2, 2, 7, barril);
        poner(nivel, s, 1, 1, 6, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));

        // Mesas con sillas
        for (int z : new int[]{2, 6}) {
            poner(nivel, s, 7, 1, z, Blocks.OAK_FENCE.defaultBlockState());
            poner(nivel, s, 7, 2, z, Blocks.OAK_PRESSURE_PLATE.defaultBlockState());
            poner(nivel, s, 6, 1, z, escalera(Blocks.OAK_STAIRS, Direction.WEST, Half.BOTTOM));
            poner(nivel, s, 8, 1, z, escalera(Blocks.OAK_STAIRS, Direction.EAST, Half.BOTTOM));
        }
        // Faroles colgando del techo
        BlockState farol = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        poner(nivel, s, 7, 3, 2, farol);
        poner(nivel, s, 7, 3, 6, farol);
        poner(nivel, s, 2, 3, 4, farol);

        // La cama del tabernero, al fondo a la derecha (cabecera contra la pared)
        BlockState cama = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST);
        poner(nivel, s, 8, 1, 7, cama.setValue(BedBlock.PART, BedPart.FOOT));
        poner(nivel, s, 9, 1, 7, cama.setValue(BedBlock.PART, BedPart.HEAD));

        // El tablón de encargos, en la pared de la fachada por dentro, a la izquierda de la puerta
        BlockState tablon = ModuloHabitantes.TABLON.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH);
        poner(nivel, s, 3, 2, 1, tablon);

        // Que los cristales se unan a las paredes
        for (int[] v : ventanas) {
            BlockPos p = local(s, v[0], v[1], v[2]);
            nivel.setBlock(p, Block.updateFromNeighbourShapes(nivel.getBlockState(p), nivel, p), Block.UPDATE_ALL);
        }
        return new Hecho(local(s, 3, 2, 1), local(s, 1, 1, 7), local(s, 6, 1, 4));
    }
}
