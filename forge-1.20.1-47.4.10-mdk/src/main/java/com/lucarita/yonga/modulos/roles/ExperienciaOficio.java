package com.lucarita.yonga.modulos.roles;

import com.lucarita.yonga.modulos.fe.Oracion;
import com.lucarita.yonga.core.GolpeCargado;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.GrindstoneEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Map;
import java.util.UUID;

/**
 * Cómo gana experiencia de oficio cada rol. Todas las cifras están aquí arriba para equilibrarlas fácil.
 *
 * Los rangos piden (ver RangoRol): Aprendiz 100, Oficial 400, Experto 1200, Maestro 3200.
 * Cada salto cuesta unas 2,5-3 veces el anterior. Con un ritmo de ~100 de experiencia por hora de juego
 * dedicada al oficio: Aprendiz ~1 h, Oficial ~4 h, Experto ~12 h, Maestro ~32 h (algo menos rezando).
 *
 *  - Explorador: 1 por cada 100 bloques recorridos (a pie, nadando, a caballo, en barca, volando...).
 *    Se mide en línea recta cada 5 s, así que dar vueltas en el sitio o ir y volver en un pasillo apenas
 *    cuenta, y como mucho cuentan 50 bloques cada 5 s (los teletransportes no cuentan).
 *  - Sacerdote: 1 por cada 20 s rezando o blasfemando (necesita el módulo de fe). Maestro: ~18 h rezando.
 *  - Melómano: golpes a tiempo. 1 por cada 10 golpes cuerpo a cuerpo a mobs con el ataque cargado del
 *    todo (con cualquier arma o herramienta, no hacen falta instrumentos). Aporrear no cuenta.
 *  - Fantasma: 1 por cada minuto conectado, sin hacer nada.
 *  - Herrero (todo lo que sea trabajar el metal):
 *      · Fundir: 1 por cada 10 objetos que saques de un horno, alto horno o ahumador (solo lo que sacas
 *        tú, no lo que sacan las tolvas).
 *      · Fabricar un arma, armadura o herramienta de hierro o mejor (también oro, diamante, lucarita,
 *        escudos...): 2 por cada una. Las de madera, piedra o cuero no cuentan (son demasiado baratas).
 *      · Usar el yunque con un arma, armadura o herramienta (reparar, encantar con libros, juntar): 4.
 *        Cambiar solo el nombre no cuenta.
 *      · Usar la piedra de afilar con un arma, armadura o herramienta de hierro o mejor: 2.
 *  - Ingeniero: 1 por cada 20 bloques que rompas con un hacha, un pico o una pala (cualquiera, también
 *    los instrumentos-herramienta). No cuentan los bloques que se rompen al instante (hierba alta, flores,
 *    antorchas...) ni los que acaba de poner un jugador (para que poner y quitar no sirva).
 */
public final class ExperienciaOficio {

    // ---- Ajustes ----
    private static final long TICKS_ENTRE_MUESTRAS = 100;      // Explorador: cada 5 s
    private static final double DISTANCIA_MAXIMA_MUESTRA = 50;  // más que esto en 5 s = teletransporte
    private static final double BLOQUES_POR_PUNTO = 100;

    private static final int TICKS_REZANDO_POR_PUNTO = 20 * 20;

    private static final int GOLPES_POR_PUNTO = 10;

    private static final int FUNDIDOS_POR_PUNTO = 10;
    private static final int PUNTOS_FABRICAR = 2;
    private static final int PUNTOS_YUNQUE = 4;
    private static final int PUNTOS_PIEDRA_AFILAR = 2;

    private static final int BLOQUES_POR_PUNTO_INGENIERO = 20;
    private static final int RECORDAR_PUESTOS = 10000;          // últimos bloques puestos por jugadores

    private static final int TICKS_FANTASMA_POR_PUNTO = 20 * 60;

    // ---- Lo que va acumulando cada jugador (se pierde al desconectarse, no pasa nada) ----
    private record Muestra(Vec3 posicion, String dimension) {}
    private static final Map<UUID, Muestra> ULTIMA_MUESTRA = new HashMap<>();
    private static final Map<UUID, Double> BLOQUES = new HashMap<>();
    private static final Map<UUID, Integer> TICKS_REZANDO = new HashMap<>();
    private static final Map<UUID, Integer> GOLPES = new HashMap<>();
    private static final Map<UUID, Integer> FUNDIDOS = new HashMap<>();
    private static final Map<UUID, Integer> ROTOS = new HashMap<>();
    // Bloques que han puesto jugadores hace poco (dimensión + posición). Se olvidan los más antiguos.
    private static final Set<String> PUESTOS = Collections.newSetFromMap(new LinkedHashMap<>(16, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> masAntiguo) {
            return size() > RECORDAR_PUESTOS;
        }
    });

    private ExperienciaOficio() {}

    public static void alTick(TickEvent.PlayerTickEvent evento) {
        if (evento.phase != TickEvent.Phase.END || !(evento.player instanceof ServerPlayer jugador)) {
            return;
        }
        Rol rol = RolDatos.obtenerRol(jugador);
        if (rol == null) {
            return;
        }
        switch (rol) {
            case EXPLORADOR -> explorador(jugador);
            case SACERDOTE -> sacerdote(jugador);
            case FANTASMA -> {
                if (jugador.tickCount % TICKS_FANTASMA_POR_PUNTO == 0) {
                    RolDatos.sumarExperiencia(jugador, 1);
                }
            }
            default -> {}
        }
    }

    private static void explorador(ServerPlayer jugador) {
        if (jugador.tickCount % TICKS_ENTRE_MUESTRAS != 0) {
            return;
        }
        UUID id = jugador.getUUID();
        Muestra ahora = new Muestra(jugador.position(), jugador.level().dimension().location().toString());
        Muestra antes = ULTIMA_MUESTRA.put(id, ahora);
        if (antes == null || !antes.dimension().equals(ahora.dimension())) {
            return;
        }
        double dx = ahora.posicion().x - antes.posicion().x;
        double dz = ahora.posicion().z - antes.posicion().z;
        double distancia = Math.sqrt(dx * dx + dz * dz);
        if (distancia > DISTANCIA_MAXIMA_MUESTRA) {
            return;   // teletransporte, perla de ender...
        }
        double total = BLOQUES.getOrDefault(id, 0.0) + distancia;
        int puntos = (int) (total / BLOQUES_POR_PUNTO);
        if (puntos > 0) {
            RolDatos.sumarExperiencia(jugador, puntos);
            total -= puntos * BLOQUES_POR_PUNTO;
        }
        BLOQUES.put(id, total);
    }

    private static void sacerdote(ServerPlayer jugador) {
        if (Oracion.direccion(jugador) == Oracion.NADA) {
            return;
        }
        int ticks = TICKS_REZANDO.getOrDefault(jugador.getUUID(), 0) + 1;
        if (ticks >= TICKS_REZANDO_POR_PUNTO) {
            RolDatos.sumarExperiencia(jugador, 1);
            ticks = 0;
        }
        TICKS_REZANDO.put(jugador.getUUID(), ticks);
    }

    // ---------- MELÓMANO: golpes a tiempo ----------

    /**
     * Cada golpe cuerpo a cuerpo a un mob con el ataque cargado del todo (pegar a tiempo, no aporrear),
     * con cualquier arma o herramienta, y que le haga daño de verdad.
     */
    public static void alHacerDano(LivingDamageEvent evento) {
        if (!(evento.getSource().getDirectEntity() instanceof ServerPlayer jugador)
                || RolDatos.obtenerRol(jugador) != Rol.MELOMANO
                || !(evento.getEntity() instanceof Mob) || evento.getAmount() <= 0
                || !GolpeCargado.fue(jugador)) {
            return;
        }
        int golpes = GOLPES.getOrDefault(jugador.getUUID(), 0) + 1;
        if (golpes >= GOLPES_POR_PUNTO) {
            RolDatos.sumarExperiencia(jugador, 1);
            golpes = 0;
        }
        GOLPES.put(jugador.getUUID(), golpes);
    }

    // ---------- HERRERO: fundir ----------

    public static void alFundir(PlayerEvent.ItemSmeltedEvent evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jugador) || RolDatos.obtenerRol(jugador) != Rol.HERRERO) {
            return;
        }
        contar(jugador, FUNDIDOS, evento.getSmelting().getCount(), FUNDIDOS_POR_PUNTO);
    }

    public static void alFabricar(PlayerEvent.ItemCraftedEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador && RolDatos.obtenerRol(jugador) == Rol.HERRERO
                && esEquipoBueno(evento.getCrafting())) {
            RolDatos.sumarExperiencia(jugador, PUNTOS_FABRICAR * Math.max(1, evento.getCrafting().getCount()));
        }
    }

    public static void alUsarYunque(AnvilRepairEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador && RolDatos.obtenerRol(jugador) == Rol.HERRERO
                && evento.getLeft().isDamageableItem() && !evento.getRight().isEmpty()) {
            RolDatos.sumarExperiencia(jugador, PUNTOS_YUNQUE);
        }
    }

    /**
     * La piedra de afilar no dice quién la usa: buscamos al jugador que la tiene abierta con esos mismos
     * objetos puestos.
     */
    public static void alUsarPiedraAfilar(GrindstoneEvent.OnTakeItem evento) {
        ItemStack arriba = evento.getTopItem(), abajo = evento.getBottomItem();
        if (!esEquipoBueno(arriba) && !esEquipoBueno(abajo)) {
            return;
        }
        var servidor = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (servidor == null) {
            return;
        }
        for (ServerPlayer jugador : servidor.getPlayerList().getPlayers()) {
            if (jugador.containerMenu instanceof GrindstoneMenu menu
                    && (menu.getSlot(0).getItem() == arriba || menu.getSlot(1).getItem() == abajo)) {
                if (RolDatos.obtenerRol(jugador) == Rol.HERRERO) {
                    RolDatos.sumarExperiencia(jugador, PUNTOS_PIEDRA_AFILAR);
                }
                return;
            }
        }
    }

    /** Arma, armadura o herramienta de hierro o mejor (las de madera, piedra o cuero no). */
    public static boolean esEquipoBueno(ItemStack objeto) {
        if (objeto.isEmpty() || !objeto.isDamageableItem()) {
            return false;
        }
        Item item = objeto.getItem();
        if (item instanceof TieredItem conNivel) {
            return conNivel.getTier().getLevel() >= 2 || conNivel.getTier() == Tiers.GOLD;
        }
        if (item instanceof ArmorItem armadura) {
            return armadura.getMaterial() != ArmorMaterials.LEATHER && armadura.getMaterial() != ArmorMaterials.TURTLE;
        }
        return item instanceof ShieldItem || item instanceof CrossbowItem;
    }

    // ---------- INGENIERO: romper bloques con herramientas ----------

    public static void alPonerBloque(BlockEvent.EntityPlaceEvent evento) {
        if (evento.getEntity() instanceof Player && !evento.getLevel().isClientSide()) {
            PUESTOS.add(clave(evento.getLevel(), evento.getPos()));
        }
    }

    public static void alRomperBloque(BlockEvent.BreakEvent evento) {
        boolean recienPuesto = PUESTOS.remove(clave(evento.getLevel(), evento.getPos()));
        if (!(evento.getPlayer() instanceof ServerPlayer jugador) || jugador.isCreative() || recienPuesto
                || RolDatos.obtenerRol(jugador) != Rol.INGENIERO
                || evento.getState().getDestroySpeed(evento.getLevel(), evento.getPos()) <= 0) {
            return;
        }
        ItemStack herramienta = jugador.getMainHandItem();
        // Por tipo (vale para las de cualquier mod, también los instrumentos) o por etiqueta
        boolean esHerramienta = herramienta.getItem() instanceof AxeItem || herramienta.getItem() instanceof PickaxeItem
                || herramienta.getItem() instanceof ShovelItem
                || herramienta.is(ItemTags.AXES) || herramienta.is(ItemTags.PICKAXES) || herramienta.is(ItemTags.SHOVELS);
        if (esHerramienta) {
            contar(jugador, ROTOS, 1, BLOQUES_POR_PUNTO_INGENIERO);
        }
    }

    private static String clave(LevelAccessor nivel, BlockPos pos) {
        String dimension = nivel instanceof Level l ? l.dimension().location().toString() : "?";
        return dimension + "|" + pos.asLong();
    }

    /** Suma "cantidad" al contador del jugador y da 1 punto por cada "porPunto". */
    private static void contar(ServerPlayer jugador, Map<UUID, Integer> contador, int cantidad, int porPunto) {
        int total = contador.getOrDefault(jugador.getUUID(), 0) + cantidad;
        int puntos = total / porPunto;
        if (puntos > 0) {
            RolDatos.sumarExperiencia(jugador, puntos);
        }
        contador.put(jugador.getUUID(), total % porPunto);
    }

    public static void alSalir(PlayerEvent.PlayerLoggedOutEvent evento) {
        UUID id = evento.getEntity().getUUID();
        ULTIMA_MUESTRA.remove(id);
        BLOQUES.remove(id);
        TICKS_REZANDO.remove(id);
        GOLPES.remove(id);
        FUNDIDOS.remove(id);
        ROTOS.remove(id);
    }
}
