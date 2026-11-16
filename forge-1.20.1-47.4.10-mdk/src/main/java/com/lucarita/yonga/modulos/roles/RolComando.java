package com.lucarita.yonga.modulos.roles;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Comando /rol (necesita permisos de operador, nivel 2):
 *   /rol ver [jugadores]                        -> muestra oficio, rango y experiencia
 *   /rol fijar <jugadores> <rol>                -> cambia el oficio (empieza como Novato)
 *   /rol quitar <jugadores>                     -> deja sin oficio y vuelve a salir el menú de elegir
 *   /rol rango <jugadores> <rango>              -> pone el rango (novato, aprendiz, oficial, experto, maestro)
 *   /rol experiencia <jugadores> <cantidad>     -> suma experiencia de oficio (negativo para restar)
 */
public final class RolComando {

    private static final DynamicCommandExceptionType ROL_DESCONOCIDO =
            new DynamicCommandExceptionType(id -> Component.translatable("yonga.rol.comando.rol_desconocido", id));
    private static final DynamicCommandExceptionType RANGO_DESCONOCIDO =
            new DynamicCommandExceptionType(id -> Component.translatable("yonga.rol.comando.rango_desconocido", id));

    private RolComando() {}

    public static void registrar(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rol")
                .requires(fuente -> fuente.hasPermission(2))

                .then(Commands.literal("ver")
                        .executes(ctx -> ver(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .executes(ctx -> ver(ctx.getSource(), EntityArgument.getPlayers(ctx, "jugadores")))))

                .then(Commands.literal("fijar")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .then(Commands.argument("rol", StringArgumentType.word())
                                        .suggests((ctx, sugerencias) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(Rol.values()).map(Rol::id), sugerencias))
                                        .executes(ctx -> {
                                            Rol rol = leerRol(ctx);
                                            return aplicar(ctx, jugador -> RolDatos.cambiarRol(jugador, rol));
                                        }))))

                .then(Commands.literal("quitar")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .executes(ctx -> aplicar(ctx, RolDatos::quitarRol))))

                .then(Commands.literal("rango")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .then(Commands.argument("rango", StringArgumentType.word())
                                        .suggests((ctx, sugerencias) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(RangoRol.values()).map(RangoRol::id), sugerencias))
                                        .executes(ctx -> {
                                            RangoRol rango = leerRango(ctx);
                                            return aplicar(ctx, jugador -> RolDatos.fijarRango(jugador, rango));
                                        }))))

                .then(Commands.literal("experiencia")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .then(Commands.argument("cantidad", IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            int cantidad = IntegerArgumentType.getInteger(ctx, "cantidad");
                                            return aplicar(ctx, jugador -> RolDatos.sumarExperiencia(jugador, cantidad));
                                        }))))
        );
    }

    private static Rol leerRol(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "rol");
        Rol rol = Rol.porId(id);
        if (rol == null) {
            throw ROL_DESCONOCIDO.create(id);
        }
        return rol;
    }

    private static RangoRol leerRango(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "rango");
        RangoRol rango = RangoRol.porId(id);
        if (rango == null) {
            throw RANGO_DESCONOCIDO.create(id);
        }
        return rango;
    }

    private static int aplicar(CommandContext<CommandSourceStack> ctx, Consumer<ServerPlayer> accion) throws CommandSyntaxException {
        Collection<ServerPlayer> jugadores = EntityArgument.getPlayers(ctx, "jugadores");
        jugadores.forEach(accion);
        return ver(ctx.getSource(), jugadores);
    }

    private static int ver(CommandSourceStack fuente, Collection<ServerPlayer> jugadores) {
        for (ServerPlayer jugador : jugadores) {
            Rol rol = RolDatos.obtenerRol(jugador);
            if (rol == null) {
                fuente.sendSuccess(() -> Component.translatable("yonga.rol.comando.sin_rol", jugador.getDisplayName()), true);
            } else {
                fuente.sendSuccess(() -> Component.translatable("yonga.rol.comando.estado",
                        jugador.getDisplayName(), rol.nombre(),
                        RolDatos.obtenerRango(jugador).nombre(), RolDatos.obtenerExperiencia(jugador)), true);
            }
        }
        return jugadores.size();
    }
}
