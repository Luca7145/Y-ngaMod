package com.lucarita.yonga.modulos.fe;

import com.lucarita.yonga.modulos.fe.red.FeRed;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Módulo de fe: puntos, niveles (-100 a 100) y rangos de cada jugador.
 * Solo se registra si el módulo está activado (ver YongaMain).
 * La parte visual (barra, menú, botón del inventario) está en el paquete client.
 */
public final class ModuloFe {

    private ModuloFe() {}

    public static void registrar(IEventBus modEventBus) {
        FeRed.registrar();

        MinecraftForge.EVENT_BUS.addListener(ModuloFe::alRegistrarComandos);

        // Mandamos la fe al cliente cuando entra, reaparece o cambia de dimensión
        MinecraftForge.EVENT_BUS.addListener(ModuloFe::alEntrar);
        MinecraftForge.EVENT_BUS.addListener(ModuloFe::alReaparecer);
        MinecraftForge.EVENT_BUS.addListener(ModuloFe::alCambiarDimension);

        // Rezar / blasfemar
        MinecraftForge.EVENT_BUS.addListener(Oracion::alTick);
        MinecraftForge.EVENT_BUS.addListener(Oracion::alRecibirDano);
        MinecraftForge.EVENT_BUS.addListener(Oracion::alSalir);
        MinecraftForge.EVENT_BUS.addListener(Oracion::alCambiarDimension);
        MinecraftForge.EVENT_BUS.addListener(Oracion::alEmpezarAVer);
    }

    private static void alRegistrarComandos(RegisterCommandsEvent evento) {
        FeComando.registrar(evento.getDispatcher());
    }

    private static void alEntrar(PlayerEvent.PlayerLoggedInEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador) {
            FeDatos.sincronizar(jugador);
        }
    }

    private static void alReaparecer(PlayerEvent.PlayerRespawnEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador) {
            FeDatos.sincronizar(jugador);
        }
    }

    private static void alCambiarDimension(PlayerEvent.PlayerChangedDimensionEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador) {
            FeDatos.sincronizar(jugador);
        }
    }
}
