package com.lucarita.yonga.modulos.habitantes.taberna;

import com.lucarita.yonga.core.ModItems;
import com.lucarita.yonga.modulos.habitantes.Epoca;
import com.lucarita.yonga.modulos.habitantes.Habitante;
import com.lucarita.yonga.modulos.habitantes.HabitantesConfig;
import com.lucarita.yonga.modulos.habitantes.ModuloHabitantes;
import com.lucarita.yonga.modulos.habitantes.Oficio;
import com.lucarita.yonga.modulos.habitantes.aldea.Aldeas;
import com.lucarita.yonga.modulos.habitantes.red.EncargosPacket;
import com.lucarita.yonga.modulos.habitantes.red.HabitantesRed;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FASE 6: la taberna y sus encargos.
 *   · Cada aldea tiene su taberna: se construye sola la primera vez que se ve la aldea (EdificioTaberna), con un
 *     tabernero dentro (si la aldea no tenía) y el TABLÓN DE ENCARGOS junto a la puerta.
 *   · Cada mañana el tabernero cuelga encargos nuevos hasta tener 4: TRAER objetos (se entregan en el tablón) o
 *     CAZAR monstruos (se aceptan en el tablón y cuentan las muertes de quien los aceptó, estés donde estés).
 *     Los que nadie cumple caducan a los 3 días. Si el tabernero no está (muerto o lejos), no hay encargos nuevos.
 *   · Recompensa, SOLO para el primero que lo cumple: monedas (así se ganan las monedas: los habitantes no compran
 *     nada a los jugadores), fama en la aldea y aprecio del tabernero.
 * Se abre con clic derecho en el tablón o con el botón "Encargos" al hablar con el tabernero.
 */
public final class Taberna {

    private Taberna() {}

    private record Tipo(String id, int min, int max, float precio) {}

    /** Cantidades pensadas para que cada encargo valga una moneda (los encargos dan como mucho una). */
    private static final Tipo[] TRAER = {
            new Tipo("minecraft:iron_ingot", 16, 24, 0), new Tipo("minecraft:gold_ingot", 8, 12, 0),
            new Tipo("minecraft:coal", 48, 64, 0), new Tipo("minecraft:wheat", 48, 64, 0),
            new Tipo("minecraft:bread", 24, 32, 0), new Tipo("minecraft:cooked_beef", 16, 24, 0),
            new Tipo("minecraft:cod", 16, 24, 0), new Tipo("minecraft:salmon", 12, 20, 0),
            new Tipo("minecraft:oak_log", 48, 64, 0), new Tipo("minecraft:spruce_log", 48, 64, 0),
            new Tipo("minecraft:cobblestone", 96, 128, 0), new Tipo("minecraft:leather", 16, 24, 0),
            new Tipo("minecraft:white_wool", 24, 32, 0), new Tipo("minecraft:potato", 48, 64, 0),
            new Tipo("minecraft:carrot", 48, 64, 0), new Tipo("minecraft:sugar_cane", 48, 64, 0),
            new Tipo("minecraft:string", 24, 32, 0), new Tipo("minecraft:feather", 24, 32, 0),
            new Tipo("minecraft:honey_bottle", 6, 10, 0), new Tipo("minecraft:pumpkin", 12, 20, 0),
            new Tipo("minecraft:glass", 32, 48, 0), new Tipo("minecraft:torch", 64, 96, 0),
            new Tipo("minecraft:egg", 16, 32, 0), new Tipo("minecraft:apple", 12, 20, 0),
            new Tipo("minecraft:sweet_berries", 48, 64, 0), new Tipo("minecraft:copper_ingot", 32, 48, 0)};

    private static final Tipo[] CAZAR = {
            new Tipo("minecraft:zombie", 8, 12, 0), new Tipo("minecraft:skeleton", 6, 10, 0),
            new Tipo("minecraft:spider", 6, 10, 0), new Tipo("minecraft:creeper", 4, 6, 0),
            new Tipo("minecraft:witch", 1, 2, 0), new Tipo("minecraft:pillager", 4, 6, 0),
            new Tipo("minecraft:drowned", 5, 8, 0), new Tipo("minecraft:slime", 8, 12, 0),
            new Tipo("minecraft:enderman", 2, 3, 0), new Tipo("minecraft:husk", 5, 8, 0),
            new Tipo("minecraft:stray", 5, 8, 0), new Tipo("minecraft:cave_spider", 6, 10, 0)};

    private static final int ABIERTOS = 4;
    private static final long CADUCA = 3 * 24000L;
    public static final int ACEPTAR = 0, ENTREGAR = 1;

    // =====================================================================================================
    // TABERNA Y TABERNERO
    // =====================================================================================================

    /** Cada ~10 s por habitante de la aldea: si no hay taberna, la construye; cada mañana, encargos nuevos. */
    public static void revisar(ServerLevel nivel, BlockPos campana) {
        if (!HabitantesConfig.TABERNAS.get()) return;
        Encargos datos = Encargos.de(nivel);
        Encargos.Datos t = datos.taberna(campana);
        long ahora = nivel.getGameTime();
        if (t.tablon == null) {
            if (!com.lucarita.yonga.modulos.habitantes.aldea.Poblados.lista(nivel, campana)) return;
            // Sin tope de intentos: si no hay sitio se vuelve a mirar cada medio minuto (los árboles se talan, el
            // terreno se carga…). Si falta terreno por cargar no cuenta como intento.
            if (ahora - t.ultimoIntento < (t.intentos > 20 ? 1200 : 300)) return;
            if (!nivel.isAreaLoaded(campana, 3)) return;
            EdificioTaberna.Hecho hecho = EdificioTaberna.construir(nivel, campana);
            if (hecho == EdificioTaberna.SIN_CARGAR) {
                t.ultimoIntento = ahora - 200;           // se vuelve a mirar en 5 s, sin gastar intento
                return;
            }
            t.ultimoIntento = ahora;
            if (hecho == null) {
                t.intentos++;
                datos.marcar();
                return;
            }
            t.intentos = 0;
            t.tablon = hecho.tablon();
            t.caldero = hecho.caldero();
            t.dentro = hecho.dentro();
            datos.marcar();
            if (tabernero(nivel, campana) == null) ponerTabernero(nivel, campana, hecho);
            return;
        }
        long dia = nivel.getDayTime() / 24000L;
        if (dia != t.ultimoDia) {
            t.ultimoDia = dia;
            // ¿La taberna se ha quedado sin tabernero? Un parado del pueblo se queda con ella; si no hay, llega uno
            if (tabernero(nivel, campana) == null && t.caldero != null && t.dentro != null) {
                Habitante parado = null;
                for (Habitante v : nivel.getEntitiesOfClass(Habitante.class, new AABB(campana).inflate(Aldeas.RADIO),
                        v -> v.isAlive() && v.esAdulto() && v.sinOficio() && !v.esHolgazan() && !v.esLider())) {
                    parado = v;
                    break;
                }
                if (parado != null) parado.asignarOficio(Oficio.tabernero(parado.epoca()), 1, t.caldero);
                else ponerTabernero(nivel, campana, new EdificioTaberna.Hecho(t.tablon, t.caldero, t.dentro));
            }
            renovar(nivel, campana, t, ahora);
            datos.marcar();
        }
    }

    /** /yonga taberna: levanta la taberna de la aldea más cercana justo donde está el jugador. */
    public static int forzar(net.minecraft.server.level.ServerPlayer j) {
        ServerLevel nivel = j.serverLevel();
        BlockPos campana = Aldeas.campanaCerca(nivel, j.blockPosition());
        if (campana == null) {
            j.sendSystemMessage(net.minecraft.network.chat.Component.translatable("yonga.taberna.sin_aldea"));
            return 0;
        }
        Encargos datos = Encargos.de(nivel);
        Encargos.Datos t = datos.taberna(campana);
        if (t.tablon != null) {
            j.sendSystemMessage(net.minecraft.network.chat.Component.translatable("yonga.taberna.ya_hay",
                    t.tablon.getX(), t.tablon.getY(), t.tablon.getZ()));
            return 0;
        }
        BlockPos aqui = j.blockPosition();
        // El suelo bajo los pies del jugador
        while (aqui.getY() > nivel.getMinBuildHeight() && nivel.getBlockState(aqui.below()).canBeReplaced()) aqui = aqui.below();
        EdificioTaberna.Hecho hecho = EdificioTaberna.construirAqui(nivel, campana, aqui);
        t.intentos = 0;
        t.tablon = hecho.tablon();
        t.caldero = hecho.caldero();
        t.dentro = hecho.dentro();
        datos.marcar();
        if (tabernero(nivel, campana) == null) ponerTabernero(nivel, campana, hecho);
        j.sendSystemMessage(net.minecraft.network.chat.Component.translatable("yonga.taberna.hecha"));
        return 1;
    }

    @Nullable
    public static Habitante tabernero(ServerLevel nivel, BlockPos campana) {
        List<Habitante> lista = nivel.getEntitiesOfClass(Habitante.class, new AABB(campana).inflate(Aldeas.RADIO),
                h -> h.isAlive() && h.oficio() != null && h.oficio().esTabernero() && !h.esLider());
        return lista.isEmpty() ? null : lista.get(0);
    }

    private static void ponerTabernero(ServerLevel nivel, BlockPos campana, EdificioTaberna.Hecho hecho) {
        Habitante h = ModuloHabitantes.HABITANTE.get().create(nivel);
        if (h == null) return;
        BlockPos p = hecho.dentro();
        h.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, nivel.random.nextFloat() * 360.0f, 0.0f);
        Epoca epoca = Epoca.MEDIEVAL;
        for (Habitante v : nivel.getEntitiesOfClass(Habitante.class, new AABB(campana).inflate(Aldeas.RADIO))) {
            epoca = v.epoca();
            break;
        }
        h.generar(epoca, 3, nivel.random);
        h.asignarOficio(Oficio.tabernero(h.epoca()), 2, hecho.caldero());
        h.finalizeSpawn(nivel, nivel.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null, null);
        nivel.addFreshEntity(h);
    }

    // =====================================================================================================
    // ENCARGOS
    // =====================================================================================================

    private static void renovar(ServerLevel nivel, BlockPos campana, Encargos.Datos t, long ahora) {
        // Los cumplidos se quitan; los que nadie ha cumplido siguen colgados. Se rellena hasta 4.
        t.encargos.removeIf(e -> !e.abierto());
        if (tabernero(nivel, campana) == null) return;            // sin tabernero no hay encargos nuevos
        reponerTablon(nivel, t);
        int abiertos = 0;
        for (Encargos.Encargo e : t.encargos) if (e.abierto()) abiertos++;
        while (abiertos < ABIERTOS) {
            Encargos.Encargo e = nuevo(nivel, t, ahora);
            if (e == null) break;
            t.encargos.add(e);
            abiertos++;
        }
    }

    @Nullable
    private static Encargos.Encargo nuevo(ServerLevel nivel, Encargos.Datos t, long ahora) {
        for (int intento = 0; intento < 10; intento++) {
            boolean caza = nivel.random.nextFloat() < 0.4f;
            Tipo tipo = (caza ? CAZAR : TRAER)[nivel.random.nextInt((caza ? CAZAR : TRAER).length)];
            ResourceLocation id = new ResourceLocation(tipo.id());
            if (caza ? !ForgeRegistries.ENTITY_TYPES.containsKey(id) : !ForgeRegistries.ITEMS.containsKey(id)) continue;
            boolean repetido = false;
            for (Encargos.Encargo x : t.encargos) if (x.abierto() && x.objetivo.equals(tipo.id())) repetido = true;
            if (repetido) continue;
            Encargos.Encargo e = new Encargos.Encargo();
            e.id = t.siguienteId++;
            e.caza = caza;
            e.objetivo = tipo.id();
            e.cantidad = tipo.min() + nivel.random.nextInt(tipo.max() - tipo.min() + 1);
            e.monedas = 1;                                        // un encargo, una moneda
            e.fama = 3;
            e.aprecio = 6;
            e.caduca = ahora + CADUCA;
            return e;
        }
        return null;
    }

    // ---------- abrir la lista ----------

    /**
     * Clic derecho en un tablón: solo el de la taberna tiene encargos (los cuelga allí el tabernero). Un tablón que te
     * hayas llevado a otro sitio está vacío: los encargos no se actualizan solos.
     */
    public static void abrirDesdeTablon(ServerPlayer j, BlockPos tablon) {
        ServerLevel nivel = j.serverLevel();
        BlockPos campana = Aldeas.campanaCerca(nivel, tablon);
        Encargos.Datos t = campana == null ? null : Encargos.de(nivel).taberna(campana);
        if (t == null || !tablon.equals(t.tablon)) {
            j.displayClientMessage(Component.translatable("yonga.encargos.solo_tabernero"), true);
            return;
        }
        revisar(nivel, campana);
        enviar(j, nivel, campana);
    }

    /** Si se han llevado el tablón de la taberna, el tabernero cuelga otro por la mañana (si el hueco sigue libre). */
    private static void reponerTablon(ServerLevel nivel, Encargos.Datos t) {
        if (t.tablon == null || !nivel.hasChunkAt(t.tablon)) return;
        var estado = nivel.getBlockState(t.tablon);
        if (estado.is(ModuloHabitantes.TABLON.get()) || !estado.isAir()) return;
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            // Mira hacia el lado contrario de la pared en la que se apoya
            if (nivel.getBlockState(t.tablon.relative(d.getOpposite())).isSolid()) {
                nivel.setBlock(t.tablon, ModuloHabitantes.TABLON.get().defaultBlockState()
                        .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, d), 3);
                return;
            }
        }
    }

    public static void abrirDesdeTabernero(ServerPlayer j, Habitante h) {
        if (h.aldea() == null || !(h.level() instanceof ServerLevel nivel)) {
            j.displayClientMessage(Component.translatable("yonga.encargos.sin_aldea"), true);
            return;
        }
        revisar(nivel, h.aldea());
        enviar(j, nivel, h.aldea());
    }

    private static void enviar(ServerPlayer j, ServerLevel nivel, BlockPos campana) {
        Encargos.Datos t = Encargos.de(nivel).taberna(campana);
        List<EncargosPacket.Entrada> lista = new ArrayList<>();
        for (Encargos.Encargo e : t.encargos) {
            int estado = !e.abierto() ? (j.getUUID().equals(e.ganador) ? 3 : 2) : e.progreso.containsKey(j.getUUID()) ? 1 : 0;
            lista.add(new EncargosPacket.Entrada(e.id, e.caza, icono(e), descripcion(e), e.monedas, e.fama, estado,
                    e.progreso.getOrDefault(j.getUUID(), 0), e.cantidad, e.nombreGanador));
        }
        HabitantesRed.CANAL.send(PacketDistributor.PLAYER.with(() -> j),
                new EncargosPacket(campana, tabernero(nivel, campana) != null, lista));
    }

    private static ItemStack icono(Encargos.Encargo e) {
        ResourceLocation id = new ResourceLocation(e.objetivo);
        if (!e.caza) {
            Item item = ForgeRegistries.ITEMS.getValue(id);
            return item == null ? new ItemStack(Items.PAPER) : new ItemStack(item);
        }
        EntityType<?> tipo = ForgeRegistries.ENTITY_TYPES.getValue(id);
        SpawnEggItem huevo = tipo == null ? null : SpawnEggItem.byId(tipo);
        return huevo == null ? new ItemStack(Items.IRON_SWORD) : new ItemStack(huevo);
    }

    public static Component descripcion(Encargos.Encargo e) {
        ResourceLocation id = new ResourceLocation(e.objetivo);
        if (!e.caza) {
            Item item = ForgeRegistries.ITEMS.getValue(id);
            return Component.translatable("yonga.encargos.traer", e.cantidad,
                    item == null ? Component.literal(e.objetivo) : item.getDescription());
        }
        EntityType<?> tipo = ForgeRegistries.ENTITY_TYPES.getValue(id);
        return Component.translatable("yonga.encargos.cazar", e.cantidad,
                tipo == null ? Component.literal(e.objetivo) : tipo.getDescription());
    }

    // ---------- aceptar / entregar ----------

    /** Hay que estar junto al tablón de la taberna o junto al tabernero. */
    private static boolean cerca(ServerPlayer j, ServerLevel nivel, BlockPos campana, Encargos.Datos t) {
        if (t.tablon != null && j.blockPosition().closerThan(t.tablon, 8)
                && nivel.getBlockState(t.tablon).is(ModuloHabitantes.TABLON.get())) return true;
        Habitante h = tabernero(nivel, campana);
        return h != null && h.distanceToSqr(j) < 8 * 8;
    }

    public static void accion(ServerPlayer j, BlockPos campana, int id, int que) {
        ServerLevel nivel = j.serverLevel();
        Encargos datos = Encargos.de(nivel);
        Encargos.Datos t = datos.taberna(campana);
        Encargos.Encargo e = t.encargo(id);
        if (e == null || !cerca(j, nivel, campana, t)) return;
        if (!e.abierto()) {
            j.displayClientMessage(Component.translatable("yonga.encargos.ya_cumplido", e.nombreGanador), true);
        } else if (que == ACEPTAR && e.caza) {
            e.progreso.putIfAbsent(j.getUUID(), 0);
            datos.marcar();
            j.displayClientMessage(Component.translatable("yonga.encargos.aceptado"), true);
        } else if (que == ENTREGAR && !e.caza) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(e.objetivo));
            if (item == null) return;
            int tiene = j.getInventory().countItem(item);
            if (tiene < e.cantidad) {
                j.displayClientMessage(Component.translatable("yonga.encargos.faltan", e.cantidad - tiene), true);
            } else {
                j.getInventory().clearOrCountMatchingItems(s -> s.is(item), e.cantidad, j.inventoryMenu.getCraftSlots());
                completar(nivel, campana, e, j);
                datos.marcar();
            }
        }
        enviar(j, nivel, campana);
    }

    /** El primero que lo cumple se lo lleva todo: monedas, fama en la aldea y aprecio del tabernero. */
    private static void completar(ServerLevel nivel, BlockPos campana, Encargos.Encargo e, ServerPlayer j) {
        e.ganador = j.getUUID();
        e.nombreGanador = j.getGameProfile().getName();
        e.cumplido = nivel.getGameTime();
        int quedan = e.monedas;
        while (quedan > 0) {
            ItemStack monedas = new ItemStack(ModItems.MONEDA.get(), Math.min(quedan, 64));
            quedan -= monedas.getCount();
            if (!j.getInventory().add(monedas)) j.drop(monedas, false);
        }
        Aldeas.de(nivel).cambiarFama(campana, j.getUUID(), e.fama);
        Habitante h = tabernero(nivel, campana);
        if (h != null) h.recuerdo(j).cambiarOpinion(e.aprecio);
        j.sendSystemMessage(Component.translatable("yonga.encargos.cumplido", descripcion(e), e.monedas));
        for (UUID otro : e.progreso.keySet()) {
            if (otro.equals(j.getUUID())) continue;
            ServerPlayer p = nivel.getServer().getPlayerList().getPlayer(otro);
            if (p != null) p.sendSystemMessage(Component.translatable("yonga.encargos.otro_gano", e.nombreGanador, descripcion(e)));
        }
    }

    /** Las cazas: cada monstruo que mata quien aceptó el encargo cuenta, esté donde esté. */
    public static void alMorir(LivingDeathEvent evento) {
        if (!(evento.getSource().getEntity() instanceof ServerPlayer j) || !(j.level() instanceof ServerLevel nivel)) return;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(evento.getEntity().getType());
        if (id == null) return;
        String tipo = id.toString();
        Encargos datos = Encargos.de(nivel);
        for (Map.Entry<Long, Encargos.Datos> par : datos.todas()) {
            for (Encargos.Encargo e : par.getValue().encargos) {
                if (!e.abierto() || !e.caza || !e.objetivo.equals(tipo) || !e.progreso.containsKey(j.getUUID())) continue;
                int n = e.progreso.get(j.getUUID()) + 1;
                e.progreso.put(j.getUUID(), n);
                datos.marcar();
                if (n >= e.cantidad) completar(nivel, BlockPos.of(par.getKey()), e, j);
                else j.displayClientMessage(Component.translatable("yonga.encargos.progreso", descripcion(e), n, e.cantidad), true);
            }
        }
    }

    /** Para la IA del tabernero: lo que tiene colgado en el tablón. */
    public static String resumen(ServerLevel nivel, BlockPos campana) {
        Encargos.Datos t = Encargos.de(nivel).taberna(campana);
        StringBuilder sb = new StringBuilder();
        for (Encargos.Encargo e : t.encargos) {
            if (!e.abierto()) continue;
            sb.append("\n  - ").append(descripcion(e).getString()).append(" (").append(e.monedas).append(" monedas)");
        }
        return sb.toString();
    }
}
