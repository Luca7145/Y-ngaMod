package com.lucarita.yonga.modulos.roles;

import com.lucarita.yonga.modulos.roles.red.RolesRed;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Módulo de roles (oficios): Herrero, Explorador, Sacerdote, Ingeniero, Narco...
 * Cada jugador tiene un oficio y un rango dentro de él (Novato -> Maestro).
 * Solo se registra si el módulo está activado (ver YongaMain).
 * La parte visual (menú de oficios, botón del inventario) está en el paquete client/roles.
 */
public final class ModuloRoles {

    private ModuloRoles() {}

    public static void registrar(IEventBus modEventBus) {
        RolesRed.registrar();

        MinecraftForge.EVENT_BUS.addListener(ModuloRoles::alRegistrarComandos);
        MinecraftForge.EVENT_BUS.addListener(ModuloRoles::alEntrar);
        MinecraftForge.EVENT_BUS.addListener(ModuloRoles::alReaparecer);
        MinecraftForge.EVENT_BUS.addListener(ModuloRoles::alCambiarDimension);
    }

    private static void alRegistrarComandos(RegisterCommandsEvent evento) {
        RolComando.registrar(evento.getDispatcher());
    }

    // Al entrar: si aún no tiene oficio, le sale el menú para elegirlo
    private static void alEntrar(PlayerEvent.PlayerLoggedInEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador) {
            RolDatos.sincronizar(jugador, !RolDatos.tieneRol(jugador));
        }
    }

    private static void alReaparecer(PlayerEvent.PlayerRespawnEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador) {
            RolDatos.sincronizar(jugador, false);
        }
    }

    private static void alCambiarDimension(PlayerEvent.PlayerChangedDimensionEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador) {
            RolDatos.sincronizar(jugador, false);
        }
    }
}
