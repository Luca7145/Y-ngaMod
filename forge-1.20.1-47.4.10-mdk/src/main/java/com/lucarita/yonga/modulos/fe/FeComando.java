package com.lucarita.yonga.modulos.fe;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;

/**
 * Comando /fe (necesita permisos de operador, nivel 2):
 *   /fe ver [jugador]                    -> muestra puntos, nivel y rango
 *   /fe sumar <jugadores> <puntos>       -> suma puntos de fe
 *   /fe restar <jugadores> <puntos>      -> resta puntos de fe
 *   /fe fijar <jugadores> <puntos>       -> pone los puntos a un valor exacto (puede ser negativo)
 *   /fe nivel <jugadores> <nivel>        -> pone al jugador justo al principio de un nivel (-100 a 100; -101/101 solo el Sacerdote Maestro)
 *   /fe mana <jugadores> <cantidad>      -> pone el maná (o maná oscuro) a esa cantidad (como mucho, el máximo del rango)
 */
public final class FeComando {

    private FeComando() {}

    public static void registrar(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fe")
                .requires(fuente -> fuente.hasPermission(2))

                .then(Commands.literal("ver")
                        .executes(ctx -> ver(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .executes(ctx -> ver(ctx.getSource(), EntityArgument.getPlayers(ctx, "jugadores")))))

                .then(Commands.literal("sumar")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .then(Commands.argument("puntos", DoubleArgumentType.doubleArg(0))
                                        .executes(ctx -> {
                                            double puntos = DoubleArgumentType.getDouble(ctx, "puntos");
                                            return cambiar(ctx.getSource(), EntityArgument.getPlayers(ctx, "jugadores"),
                                                    jugador -> FeDatos.obtenerPuntos(jugador) + puntos);
                                        }))))

                .then(Commands.literal("restar")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .then(Commands.argument("puntos", DoubleArgumentType.doubleArg(0))
                                        .executes(ctx -> {
                                            double puntos = DoubleArgumentType.getDouble(ctx, "puntos");
                                            return cambiar(ctx.getSource(), EntityArgument.getPlayers(ctx, "jugadores"),
                                                    jugador -> FeDatos.obtenerPuntos(jugador) - puntos);
                                        }))))

                .then(Commands.literal("fijar")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .then(Commands.argument("puntos", DoubleArgumentType.doubleArg())
                                        .executes(ctx -> {
                                            double puntos = DoubleArgumentType.getDouble(ctx, "puntos");
                                            return cambiar(ctx.getSource(), EntityArgument.getPlayers(ctx, "jugadores"),
                                                    jugador -> puntos);
                                        }))))

                .then(Commands.literal("nivel")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .then(Commands.argument("nivel", IntegerArgumentType.integer(-FeNiveles.NIVEL_SUPREMO, FeNiveles.NIVEL_SUPREMO))
                                        .executes(ctx -> {
                                            int nivel = IntegerArgumentType.getInteger(ctx, "nivel");
                                            double puntos = Math.signum(nivel) * FeNiveles.puntosParaNivel(Math.abs(nivel));
                                            return cambiar(ctx.getSource(), EntityArgument.getPlayers(ctx, "jugadores"),
                                                    jugador -> puntos);
                                        }))))

                .then(Commands.literal("mana")
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .then(Commands.argument("cantidad", DoubleArgumentType.doubleArg(0))
                                        .executes(ctx -> {
                                            double cantidad = DoubleArgumentType.getDouble(ctx, "cantidad");
                                            Collection<ServerPlayer> jugadores = EntityArgument.getPlayers(ctx, "jugadores");
                                            for (ServerPlayer jugador : jugadores) {
                                                ManaDatos.fijar(jugador, cantidad);
                                                ctx.getSource().sendSuccess(() -> Component.translatable("yonga.fe.comando.mana",
                                                        jugador.getDisplayName(),
                                                        ManaDatos.tipo(jugador).nombre(),
                                                        formatear(ManaDatos.obtener(jugador)),
                                                        ManaDatos.maximo(jugador)), true);
                                            }
                                            return jugadores.size();
                                        }))))
        );
    }

    private static int cambiar(CommandSourceStack fuente, Collection<ServerPlayer> jugadores, ToDoubleFunction<ServerPlayer> nuevosPuntos) {
        for (ServerPlayer jugador : jugadores) {
            FeDatos.fijarPuntos(jugador, nuevosPuntos.applyAsDouble(jugador));
        }
        return ver(fuente, jugadores);
    }

    private static int ver(CommandSourceStack fuente, Collection<ServerPlayer> jugadores) {
        for (ServerPlayer jugador : jugadores) {
            double puntos = FeDatos.obtenerPuntos(jugador);
            int nivel = FeNiveles.nivel(puntos);
            fuente.sendSuccess(() -> Component.translatable("yonga.fe.comando.estado",
                    jugador.getDisplayName(),
                    formatear(puntos),
                    nivel,
                    RangoFe.deNivel(nivel).nombre()), true);
        }
        return jugadores.size();
    }

    public static String formatear(double numero) {
        return String.format(Locale.ROOT, "%.1f", numero);
    }
}
