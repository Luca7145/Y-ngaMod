package com.lucarita.yonga.modulos.habitantes.compat;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.habitantes.Habitante;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * FASE 9: dependencias opcionales. Nada de esto necesita los otros mods para compilar ni para funcionar.
 *
 * CREATE: las aldeas industriales y contemporáneas cambian algunos bloques por los de Create si está (ver
 * ConCreateProcessor; lo usan las piezas de esas aldeas desde su lista de procesadores).
 *
 * BEBÉS EN BRAZOS (con o sin Carry On): agachado y con la mano vacía, clic derecho a un bebé para cogerlo; lo
 * llevas delante, a la altura del pecho. Para dejarlo, agachado y con la mano vacía, clic derecho al suelo (o al
 * propio bebé). A los mayores no se les coge: agachado + clic derecho sigue abriendo su tienda.
 * Con Carry On instalado, a los bebés los coge Carry On (con su animación); a los mayores no, porque esto va antes
 * que Carry On y se lo impide (romperían sus tiendas y sus rutinas).
 */
public final class Compat {

    private Compat() {}

    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    public static final DeferredRegister<StructureProcessorType<?>> PROCESADORES =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, YongaMain.MOD_ID);

    public static final RegistryObject<StructureProcessorType<ConCreateProcessor>> CON_CREATE =
            PROCESADORES.register("con_create", () -> () -> ConCreateProcessor.CODEC);

    /** (PROCESADORES se registra siempre, desde YongaMain.) */
    public static void registrar(IEventBus bus) {
        // Las dos: el cliente prueba primero "en un punto del cuerpo" y luego "a la entidad"; Carry On mira alguna de ellas
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, Compat::alInteractuarEnPunto);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, Compat::alInteractuar);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, Compat::alClicBloque);
        if (ModList.get().isLoaded("carryon")) {
            LOG.info("Yonga - Carry On encontrado: los habitantes se llevan en brazos con lo de Yonga");
        }
        if (ModList.get().isLoaded("create")) {
            LOG.info("Yonga - Create encontrado: las aldeas industriales y de hoy usarán sus bloques");
        }
    }

    /** ¿Lleva este jugador un bebé en brazos? */
    public static Habitante enBrazos(Player jugador) {
        for (Entity e : jugador.getPassengers()) {
            if (e instanceof Habitante h) return h;
        }
        return null;
    }

    private static boolean manosLibres(Player jugador, InteractionHand mano) {
        return mano == InteractionHand.MAIN_HAND && jugador.isShiftKeyDown() && jugador.getMainHandItem().isEmpty();
    }

    private static void alInteractuarEnPunto(PlayerInteractEvent.EntityInteractSpecific evento) {
        interactuar(evento, evento.getTarget());
    }

    private static void alInteractuar(PlayerInteractEvent.EntityInteract evento) {
        interactuar(evento, evento.getTarget());
    }

    /** Agachado + mano vacía + clic derecho a un habitante: coger o dejar al bebé, o la tienda de los mayores. */
    private static void interactuar(PlayerInteractEvent evento, Entity objetivo) {
        if (!(objetivo instanceof Habitante h)) return;
        Player jugador = evento.getEntity();
        if (!manosLibres(jugador, evento.getHand())) return;
        if (h.etapa() > 0) {
            if (ModList.get().isLoaded("carryon")) {
                // Que Carry On no se lo lleve: hacemos aquí lo que haría el clic (abrir su tienda o hablar)
                evento.setCanceled(true);
                evento.setCancellationResult(InteractionResult.SUCCESS);
                if (!jugador.level().isClientSide && jugador instanceof net.minecraft.server.level.ServerPlayer sp) {
                    if (h.tieneTienda()) h.abrirTienda(sp);
                    else com.lucarita.yonga.modulos.habitantes.ia.Conversaciones.abrir(sp, h);
                }
            }
            return;
        }
        // Con Carry On, a los bebés los coge Carry On (con su animación de llevar en brazos)
        if (ModList.get().isLoaded("carryon") && h.getVehicle() != jugador) return;
        evento.setCanceled(true);
        evento.setCancellationResult(InteractionResult.SUCCESS);
        if (jugador.level().isClientSide) return;
        if (h.getVehicle() == jugador) {
            dejar(jugador, h, jugador.position().add(jugador.getLookAngle().multiply(1, 0, 1).normalize()));
        } else if (enBrazos(jugador) == null && !h.isPassenger()) {
            h.startRiding(jugador, true);
            avisarPasajeros(jugador);
            jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 0.8f, 1.3f);
        }
    }

    /** Agachado + mano vacía + clic derecho al suelo con un bebé en brazos: lo dejas ahí. */
    private static void alClicBloque(PlayerInteractEvent.RightClickBlock evento) {
        Player jugador = evento.getEntity();
        if (!manosLibres(jugador, evento.getHand())) return;
        Habitante h = enBrazos(jugador);
        if (h == null) return;
        evento.setCanceled(true);
        evento.setCancellationResult(InteractionResult.SUCCESS);
        if (jugador.level().isClientSide) return;
        var sitio = evento.getPos().relative(evento.getFace());
        dejar(jugador, h, Vec3.atBottomCenterOf(sitio));
    }

    /** El servidor no le cuenta al propio jugador quién va montado en él: sin esto, no ve al bebé en sus brazos. */
    private static void avisarPasajeros(Player jugador) {
        if (jugador instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(jugador));
        }
    }

    private static void dejar(Player jugador, Habitante h, Vec3 donde) {
        h.stopRiding();
        avisarPasajeros(jugador);
        h.teleportTo(donde.x, donde.y, donde.z);
        h.getNavigation().stop();
        jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 0.8f, 1.0f);
    }
}
