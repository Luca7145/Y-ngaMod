package com.lucarita.yonga.modulos.habitantes.ia;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lucarita.yonga.modulos.habitantes.Habitante;
import com.lucarita.yonga.modulos.habitantes.HabitantesConfig;
import com.lucarita.yonga.modulos.habitantes.red.AbrirConversacionPacket;
import com.lucarita.yonga.modulos.habitantes.red.BurbujaPacket;
import com.lucarita.yonga.modulos.habitantes.red.ConversacionPacket;
import com.lucarita.yonga.modulos.habitantes.red.HabitantesRed;
import com.lucarita.yonga.modulos.habitantes.red.LineaConversacionPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Las conversaciones con los habitantes (fase 3).
 *
 * El jugador escribe en la pantalla de conversación. Nada sale en el chat: lo que dice el habitante se ve en un bocadillo
 * sobre su cabeza (lo ven todos los que estén cerca) y en la pantalla de quien le habla. El servidor
 * pregunta a la IA local (Ollama) haciéndose pasar por el habitante: quién es, su época, su oficio, su carácter, lo que
 * opina del jugador y lo último que se han dicho. La IA devuelve JSON con:
 *   · respuesta   lo que dice (sale en un bocadillo sobre su cabeza y en la pantalla de conversación);
 *   · opinion     cuánto cambia lo que piensa del jugador (−10..10);
 *   · dice_nombre si ha dicho su nombre (se apunta, pero el nombre nunca se muestra: hay que recordarlo);
 *   · notas       lo importante que sabe del jugador, reescrito cada vez (memoria larga, más allá de las frases);
 *   · accion      ninguna / regatear / regalo / echar, con "producto" (0 = todo, 1..9 = hueco de la tienda).
 *                 REGATEO: la IA solo dice lo que ha pedido el jugador ("cantidad" por "precio") y su "postura"
 *                 (aceptar / contraoferta / rechazar / ofendido), SIN números en la respuesta. Los números los pone
 *                 el servidor (ver Habitante.regatear) y se dicen al final de la respuesta, así siempre coincide lo
 *                 dicho con lo aplicado. Límite según la opinión: nada si es < −20; si no, 10 % + opinión/4
 *                 (entre 5 y 35 %). Si se ofende, le sube un 20 % los precios hasta mañana.
 *                   regalo: opinión ≥ 50, uno al día;
 *                   echar: corta la conversación, se aleja y no vuelve a hablarte en 5 minutos.
 * El regateo es hablando: "¿me dejas la espada más barata?" y la IA decide. Hay que ganárselo.
 * Si un jugador le pega, la opinión cae 25 y la conversación se corta.
 *
 * Si la IA está apagada o falla, contestan con frases sencillas según lo que opinen del jugador.
 * Los bebés solo balbucean.
 */
public final class Conversaciones {

    private static final Logger LOG = LogUtils.getLogger();

    private Conversaciones() {}

    /** Rasgos de carácter, en la lengua del prompt. Cada habitante tiene dos. */
    public static final String[] RASGOS = {
            "amable", "gruñón", "tímido", "charlatán", "desconfiado", "alegre",
            "avaricioso", "generoso", "orgulloso", "curioso", "supersticioso", "bromista"};

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final int MAX_LETRAS = 500;
    private static final int OPINION_REGALO = 40;
    private static final long ENFADO = 6000L;              // 5 minutos sin hablarte
    /** Lo que dura una conversación sin decir nada antes de que se vaya (5 minutos). */
    private static final int CONVERSACION = 6000;
    private static final Map<UUID, Long> ULTIMO_MENSAJE = new HashMap<>();
    private static long ultimoAvisoFallo;

    // =====================================================================================================
    // ENTRADA
    // =====================================================================================================

    public static void alPaquete(ServerPlayer jugador, ConversacionPacket paquete) {
        Entity entidad = jugador.level().getEntity(paquete.idHabitante());
        if (!(entidad instanceof Habitante habitante) || !habitante.isAlive() || habitante.distanceToSqr(jugador) > 8 * 8) {
            return;
        }
        switch (paquete.tipo()) {
            case ConversacionPacket.ABRIR -> abrir(jugador, habitante);
            case ConversacionPacket.HABLAR -> hablar(jugador, habitante, paquete.texto());
            case ConversacionPacket.TIENDA -> habitante.abrirTienda(jugador);
            case ConversacionPacket.CERRAR -> {
                if (habitante.interlocutor() == jugador) habitante.terminarConversacion();
            }
            case ConversacionPacket.REGALAR -> com.lucarita.yonga.modulos.habitantes.Gustos.regalar(jugador, habitante);
            case ConversacionPacket.DECLARARSE -> com.lucarita.yonga.modulos.habitantes.familia.Amor.declararse(jugador, habitante);
            case ConversacionPacket.NOCHE -> com.lucarita.yonga.modulos.habitantes.familia.Amor.nocheJuntos(jugador, habitante);
            case ConversacionPacket.ROMPER -> com.lucarita.yonga.modulos.habitantes.familia.Amor.romperBoton(jugador, habitante);
            case ConversacionPacket.SEGUIR, ConversacionPacket.DEJAR, ConversacionPacket.QUEDARSE, ConversacionPacket.LIBRE ->
                    com.lucarita.yonga.modulos.habitantes.Ordenes.orden(jugador, habitante, paquete.tipo());
            case ConversacionPacket.ENCARGOS -> {
                if ((habitante.oficio() != null && habitante.oficio().esTabernero())) {
                    com.lucarita.yonga.modulos.habitantes.taberna.Taberna.abrirDesdeTabernero(jugador, habitante);
                }
            }
            default -> { }
        }
    }

    /** Abre la pantalla de conversación con lo último que se dijeron. */
    public static void abrir(ServerPlayer jugador, Habitante habitante) {
        if (habitante.isSleeping()) {
            burbuja(habitante, Component.literal("Zzz…"), 60);
            return;
        }
        Recuerdo rr = habitante.recuerdo(jugador);
        if (rr.enfadadoHasta > habitante.level().getGameTime()) {
            burbuja(habitante, rr.reproche.isEmpty() ? frase("enfadado", habitante)
                    : Component.translatable("yonga.amor.reproche." + habitante.getRandom().nextInt(2), rr.reproche), 80);
            return;
        }
        habitante.conversarCon(jugador, CONVERSACION);
        Component nombre = habitante.nombrePara(jugador);
        List<Component> lineas = new ArrayList<>();
        for (String frase : habitante.recuerdo(jugador).frases) {
            String texto = frase.substring(2);
            lineas.add(frase.startsWith("j:") ? lineaJugador(texto) : lineaHabitante(nombre, Component.literal(texto)));
        }
        HabitantesRed.CANAL.send(PacketDistributor.PLAYER.with(() -> jugador),
                new AbrirConversacionPacket(habitante.getId(), nombre, habitante.tieneTienda(), lineas,
                        com.lucarita.yonga.modulos.habitantes.familia.Amor.relacionParaBotones(jugador, habitante),
                        (habitante.oficio() != null && habitante.oficio().esTabernero()),
                        com.lucarita.yonga.modulos.habitantes.Ordenes.estado(jugador, habitante)));
        // Se ha enterado de que le engañas: te lo echa en cara nada más empezar a hablar
        if (!rr.reproche.isEmpty()) {
            Component reproche = Component.translatable("yonga.amor.reproche." + habitante.getRandom().nextInt(2), rr.reproche);
            rr.anotar(false, reproche.getString());
            decir(habitante, jugador, reproche, null);
            rr.reproche = "";
        }
    }

    public static void hablar(ServerPlayer jugador, Habitante habitante, String texto) {
        texto = texto.replace('\n', ' ').strip();
        if (texto.isEmpty()) return;
        if (texto.length() > MAX_LETRAS) texto = texto.substring(0, MAX_LETRAS);

        long ahora = habitante.level().getGameTime();
        Long ultimo = ULTIMO_MENSAJE.get(jugador.getUUID());
        if (ultimo != null && ahora - ultimo < 20) return;          // un mensaje por segundo como mucho
        ULTIMO_MENSAJE.put(jugador.getUUID(), ahora);

        if (habitante.pensando) {
            linea(jugador, habitante, Component.translatable("yonga.habla.espera").withStyle(ChatFormatting.GRAY));
            return;
        }
        habitante.conversarCon(jugador, CONVERSACION);

        Recuerdo recuerdo = habitante.recuerdo(jugador);
        if (habitante.etapa() == 0) {
            // Un bebé no entiende nada: diga lo que diga, balbucea (pero los botones sí los obedece)
            decir(habitante, jugador, frase("bebe", habitante), null);
            return;
        }
        if (recuerdo.enfadadoHasta > ahora) {
            decir(habitante, jugador, frase("enfadado", habitante), null);
            return;
        }
        if (!HabitantesConfig.IA_ACTIVADA.get()) {
            responderSinIA(jugador, habitante, recuerdo);
            return;
        }

        // Las notas (memoria larga) solo se reescriben cada 4 frases suyas: es lo más largo de generar
        // Las notas (memoria larga) se reescriben cada 3 frases suyas, y siempre que las frases más viejas estén a punto
        // de olvidarse, para que nada se pierda
        int n = recuerdo.frases.size();
        boolean conNotas = n % 6 == 4 || n >= recuerdo.maxFrases() - 2 || recuerdo.notas.isBlank() && n >= 2;
        JsonObject peticion = Prompt.peticion(jugador, habitante, recuerdo, texto, conNotas, true);
        habitante.pensando = true;
        burbuja(habitante, Component.literal("…"), 1200);
        MinecraftServer servidor = jugador.getServer();
        UUID quien = jugador.getUUID();
        String dicho = texto;
        // Mientras se escribe, lo que lleva dicho va apareciendo en el bocadillo (salvo al regatear: ahí los
        // números los pone el servidor al final)
        Avance avance = new Avance();
        JsonObject repuesto = peticion.deepCopy();
        repuesto.remove("think");
        pedir(peticion, parcial -> {
            String visto = extraerRespuesta(parcial);
            if (visto == null || parcial.contains("\"regatear\"") || servidor == null) return;
            long reloj = System.currentTimeMillis();
            if (visto.length() - avance.letras >= 8 && reloj - avance.cuando >= 250) {
                avance.letras = visto.length();
                avance.cuando = reloj;
                servidor.execute(() -> burbuja(habitante, Component.literal(visto + "…"), 1200));
            }
        }).exceptionallyCompose(primerError -> {
            // Un fallo puntual (o un Ollama que no acepta "think"): se reintenta una vez sin "think"
            LOG.debug("Yonga: la IA ha fallado, se reintenta: {}", primerError.toString());
            return pedir(repuesto, parcial -> { });
        }).whenComplete((contenido, error) -> {
            if (servidor != null) {
                servidor.execute(() -> alResponder(servidor, quien, habitante, dicho, contenido, error, 0, null));
            }
        });
    }

    private static final class Avance {
        int letras;
        long cuando;
    }

    /** El texto de "respuesta" de un JSON a medio escribir (o null si aún no ha empezado). */
    static String extraerRespuesta(String json) {
        int clave = json.indexOf("\"respuesta\"");
        if (clave < 0) return null;
        int comillas = json.indexOf('"', json.indexOf(':', clave) + 1);
        if (comillas < 0) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = comillas + 1; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '"') break;
            if (ch == '\\') {
                if (++i >= json.length()) break;
                char esc = json.charAt(i);
                switch (esc) {
                    case 'n', 't', 'r' -> sb.append(' ');
                    case 'u' -> {
                        if (i + 4 < json.length()) {
                            try {
                                sb.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                            } catch (NumberFormatException ignorado) {
                                // a medias: se ignora
                            }
                        }
                        i += 4;
                    }
                    default -> sb.append(esc);
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString().replaceAll("\\s+", " ").strip();
    }

    // =====================================================================================================
    // RESPUESTA
    // =====================================================================================================

    /**
     * @param intento    0 la primera respuesta; 1 si se ha pedido otra (callado o repetido); 2 la de un regateo ya decidido
     * @param cierreFijo en un regateo ya decidido por el servidor, la frase con el trato (se añade a lo que diga)
     */
    private static void alResponder(MinecraftServer servidor, UUID quien, Habitante habitante, String dicho,
                                    String contenido, Throwable error, int intento, Component cierreFijo) {
        habitante.pensando = false;
        ServerPlayer jugador = servidor.getPlayerList().getPlayer(quien);
        if (habitante.isRemoved() || jugador == null) {
            return;
        }
        Recuerdo recuerdo = habitante.recuerdo(jugador);
        if ((error != null || contenido == null || contenido.isBlank()) && cierreFijo != null) {
            contenido = "{}";            // el regateo ya está hecho: aunque la IA falle, se dice el trato
            error = null;
        }
        if (error != null || contenido == null || contenido.isBlank()) {
            fallo(jugador, habitante, recuerdo, error == null ? "respuesta vacía" : motivo(error));
            return;
        }

        String respuesta;
        int cambio = 0;
        boolean diceNombre = false;
        String accion = "ninguna";
        int producto = 0, cantidad = 0, precio = 0;
        String postura = "rechazar";
        String notas = null;
        String nombreForastero = "", rumorSobre = "", rumor = "";
        try {
            JsonObject json = JsonParser.parseString(contenido).getAsJsonObject();
            respuesta = texto(json.get("respuesta"));
            if (json.has("opinion")) cambio = json.get("opinion").getAsInt();
            if (json.has("dice_nombre")) diceNombre = json.get("dice_nombre").getAsBoolean();
            if (json.has("accion")) accion = texto(json.get("accion"));
            if (json.has("producto")) producto = json.get("producto").getAsInt();
            if (json.has("postura")) postura = texto(json.get("postura"));
            if (json.has("cantidad")) cantidad = json.get("cantidad").getAsInt();
            if (json.has("precio")) precio = json.get("precio").getAsInt();
            if (json.has("notas")) notas = texto(json.get("notas"));
            if (json.has("nombre_forastero")) nombreForastero = texto(json.get("nombre_forastero"));
            if (json.has("rumor_sobre")) rumorSobre = texto(json.get("rumor_sobre"));
            if (json.has("rumor")) rumor = texto(json.get("rumor"));
        } catch (RuntimeException e) {
            // JSON roto (p. ej. cortado por el límite de palabras): se rescata lo que dijo, si se ve
            String rescatado = extraerRespuesta(contenido);
            respuesta = rescatado != null ? rescatado : contenido.contains("{") ? "" : contenido;
            LOG.debug("Yonga: respuesta de la IA sin JSON válido: {}", contenido);
        }
        respuesta = limpiar(respuesta);

        // Preguntar un precio no es regatear: si lo que dijo el jugador no suena a regateo, no se trata como tal
        if ("regatear".equals(accion) && !pareceRegateo(dicho)) {
            accion = "ninguna";
        }

        // ¿Se ha quedado callado? (los modelos a veces "se niegan" ante insultos fuertes): se le pide que conteste en su
        // papel, una vez; si vuelve a callarse, contesta enfadado él solo (más abajo)
        if (intento == 0 && respuesta.isEmpty() && !"regatear".equals(accion)) {
            habitante.pensando = true;
            JsonObject otra = Prompt.peticion(jugador, habitante, recuerdo, dicho, false, false,
                    "No te quedes callado: contesta en tu papel a lo que te ha dicho, aunque sea con enfado o insultándole.", 0.2);
            pedir(otra, parcial -> { }).whenComplete((nuevo, err) ->
                    servidor.execute(() -> alResponder(servidor, quien, habitante, dicho, nuevo, err, 1, null)));
            return;
        }

        // ¿Se está repitiendo? Se le pide otra respuesta (una vez), más creativa y diciéndole qué ha repetido
        if (intento == 0 && !respuesta.isEmpty() && repetida(recuerdo, respuesta)) {
            habitante.pensando = true;
            JsonObject otra = Prompt.peticion(jugador, habitante, recuerdo, dicho, false, false,
                    "Ibas a repetir «" + respuesta + "»: di otra cosa distinta.", 0.35);
            pedir(otra, parcial -> { }).whenComplete((nuevo, err) ->
                    servidor.execute(() -> alResponder(servidor, quien, habitante, dicho, nuevo, err, 1, null)));
            return;
        }

        // Regateo con trato (acepta o contraoferta): primero decide el servidor si es posible y cuánto, y LUEGO la IA
        // contesta sabiendo el resultado, para que no diga "acepto" y luego "no puedo"
        if (cierreFijo == null && intento < 2 && "regatear".equals(accion)) {
            Component resultado = regateo(jugador, habitante, recuerdo, producto, cantidad, precio, postura);
            if (resultado != null) {
                habitante.pensando = true;
                String aviso = "Ya está decidido cómo acaba este regateo: «" + resultado.getString() + "». "
                        + "Reacciona a lo que te ha pedido de acuerdo con eso (si no puedes bajarlo, no digas que aceptas), "
                        + "sin decir ningún número: yo añadiré el trato exacto al final.";
                JsonObject otra = Prompt.peticion(jugador, habitante, recuerdo, dicho, false, false, aviso, 0.0);
                pedir(otra, parcial -> { }).whenComplete((nuevo, err) ->
                        servidor.execute(() -> alResponder(servidor, quien, habitante, dicho, nuevo, err, 2, resultado)));
                return;
            }
        }

        recuerdo.anotar(true, dicho);
        recuerdo.anotar(false, respuesta);
        recuerdo.cambiarOpinion(habitante.ajustarOpinion(Math.max(-10, Math.min(10, cambio))));
        if (diceNombre || respuesta.contains(habitante.nombre())) {
            recuerdo.sabeNombre = true;
        }
        if (notas != null && !notas.isBlank()) {
            String n = notas.replaceAll("\\s+", " ").strip();
            recuerdo.notas = n.length() > recuerdo.maxNotas() ? n.substring(0, recuerdo.maxNotas()) : n;
        }
        // El regateo lo cierra el servidor (ya decidido arriba): añade el trato real (o su negativa) a lo que dice
        Component cierre = cierreFijo;
        boolean trato = cierreFijo != null;
        apuntarRumor(jugador, habitante, nombreForastero, rumorSobre, rumor, dicho);
        if (trato && cierre != null && respuesta.matches(".*\\d.*")) {
            respuesta = "";          // la IA ha dicho números en un trato: se quedan solo los del servidor
        }
        if (respuesta.isEmpty() && cierre == null) {
            // Ni a la segunda: lo más probable es que le hayan dicho algo muy ofensivo. Contesta enfadado y le cae peor.
            respuesta = frase("enfadado", habitante).getString();
            recuerdo.cambiarOpinion(-5);
        }
        recuerdo.frases.pollLast();                      // se sustituye lo anotado por lo que de verdad dice
        Component frase = Component.literal(respuesta);
        if (cierre != null) {
            frase = respuesta.isEmpty() ? cierre : Component.literal(respuesta + " ").append(cierre);
        }
        String texto = frase.getString();
        recuerdo.anotar(false, texto);
        decir(habitante, jugador, frase, texto);
        cumplir(jugador, habitante, recuerdo, accion, producto);
    }

    /** Cierra un regateo con números del servidor. Devuelve la frase que lo dice (o null si no hay nada que decir). */
    private static Component regateo(ServerPlayer jugador, Habitante habitante, Recuerdo recuerdo, int producto,
                                     int cantidad, int precio, String postura) {
        if (!habitante.tieneTienda()) return null;
        int maximo = maxRebaja(habitante, recuerdo.opinion);
        long dia = habitante.level().getDayTime() / 24000L;
        if (recuerdo.diaAbusos != dia) {
            recuerdo.diaAbusos = dia;
            recuerdo.abusos = 0;
        }
        // ¿Pide más de lo que estaría dispuesto a rebajarle? Entonces es una apuesta: puede que cuele, que le deje en su
        // límite, que le diga que ni hablar o que se ofenda y se lo suba. Cuanto más pide (y más veces), peor.
        if (producto > 0 && cantidad > 0 && precio > 0 && !"ofendido".equals(postura)) {
            double pedida = habitante.rebajaPedida(jugador, producto, cantidad, precio) * 100.0;
            if (!Double.isNaN(pedida) && pedida > Math.max(1, maximo)) {
                return apuesta(jugador, habitante, recuerdo, producto, cantidad, precio, maximo, pedida);
            }
        }
        switch (postura) {
            case "ofendido" -> {
                habitante.cambiarPrecioPara(jugador, producto, 0.2f);
                return Component.translatable("yonga.trato.ofendido");
            }
            case "aceptar", "contraoferta" -> {
                if (producto <= 0) {
                    if (maximo <= 0) return Component.translatable("yonga.trato.no");
                    int pct = "aceptar".equals(postura) ? maximo : Math.max(5, maximo / 2);
                    habitante.cambiarPrecioPara(jugador, 0, -pct / 100.0f);
                    return Component.translatable("yonga.trato.todo", pct);
                }
                Habitante.Pacto pacto = habitante.regatear(jugador, producto, cantidad, precio, maximo, "aceptar".equals(postura));
                if (pacto == null) return Component.translatable("yonga.trato.no");
                return Component.translatable(pacto.loPedido() ? "yonga.trato.hecho" : "yonga.trato.contra", pacto.trato());
            }
            default -> {
                return null;             // rechaza: con lo que diga la IA basta
            }
        }
    }

    /** El jugador pide más rebaja de la que el vendedor daría: se tiran los dados (ver regateo). */
    private static Component apuesta(ServerPlayer jugador, Habitante h, Recuerdo recuerdo, int producto, int cantidad,
                                     int precio, int maximo, double pedida) {
        double exceso = pedida / Math.max(2, maximo) - 1.0;          // 0 = justo en su límite; 3 = pide 4 veces más
        boolean blando = h.tieneRasgo(Habitante.GENEROSO) || h.tieneRasgo(Habitante.AMABLE);
        boolean duro = h.tieneRasgo(Habitante.AVARICIOSO) || h.tieneRasgo(Habitante.ORGULLOSO) || h.tieneRasgo(Habitante.GRUNON);
        int veces = recuerdo.abusos++;
        double gana = 0.30 / (1.0 + 3.0 * exceso) * (blando ? 1.5 : 1.0) * (duro ? 0.5 : 1.0) / (1.0 + veces);
        double ofende = Math.min(0.75, (0.15 * exceso + 0.15 * veces) * (duro ? 1.5 : 1.0)
                * (blando || h.tieneRasgo(Habitante.TIMIDO) ? 0.5 : 1.0));
        double rechaza = Math.min(0.4, 0.25 * Math.min(1.0, exceso));
        double dado = h.getRandom().nextDouble();
        if (dado < gana) {
            Habitante.Pacto pacto = h.regatear(jugador, producto, cantidad, precio, maximo, true, true);
            if (pacto != null) {
                return Component.translatable(pacto.loPedido() ? "yonga.trato.hecho" : "yonga.trato.contra", pacto.trato());
            }
        }
        if (dado < gana + ofende) {
            float subida = (float) Math.min(0.4, 0.1 + 0.05 * exceso + 0.05 * veces);
            h.cambiarPrecioPara(jugador, producto, subida);
            recuerdo.cambiarOpinion(-(2 + (int) Math.min(6, Math.round(2 * exceso))));
            return Component.translatable("yonga.trato.abuso", h.describirTrato(jugador, producto));
        }
        if (dado < gana + ofende + rechaza || maximo <= 0) {
            recuerdo.cambiarOpinion(-1);
            return Component.translatable("yonga.trato.nada");
        }
        Habitante.Pacto pacto = h.regatear(jugador, producto, cantidad, precio, maximo, true);
        if (pacto == null) return Component.translatable("yonga.trato.no");
        return Component.translatable(pacto.loPedido() ? "yonga.trato.hecho" : "yonga.trato.contra", pacto.trato());
    }

    /**
     * Lo que la IA ha sacado de la charla para la aldea: el nombre que dice tener el forastero y, si le ha contado algo
     * de alguien (de un vecino, de otra persona o de sí mismo), el rumor, que este vecino irá contando por la plaza.
     */
    private static final java.util.regex.Pattern DESEO = java.util.regex.Pattern.compile(
            "pagar[ií]a|matar[ií]a|dar[ií]a lo que|lo que fuera|lo que sea|se muere por|muriendo por|loc[oa] por|"
                    + "desesperad|a toda costa|necesita much|would pay|would kill|anything for|desperate",
            java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern NOMBRE = java.util.regex.Pattern.compile("(?<![.!?¿¡]\\s)(?<=\\s)([A-ZÁÉÍÓÚÑ][a-záéíóúñ]{2,})");

    private static void apuntarRumor(ServerPlayer jugador, Habitante h, String nombre, String sobre, String rumor,
                                     String dicho) {
        if (h.aldea() == null || !(h.level() instanceof net.minecraft.server.level.ServerLevel nivel)) return;
        var datos = com.lucarita.yonga.modulos.habitantes.aldea.Aldeas.de(nivel);
        if (nombre != null && !nombre.isBlank()) {
            datos.ponerNombre(h.aldea(), jugador.getUUID(), nombre, h.getUUID(), nivel.getGameTime());
        }
        // Por si la IA no lo apunta: "X pagaría lo que fuera por…" es un chisme seguro
        if ((rumor == null || rumor.isBlank()) && dicho != null && DESEO.matcher(dicho).find()) {
            java.util.regex.Matcher m = NOMBRE.matcher(dicho);
            String quien = m.find() ? m.group(1) : "alguien";
            sobre = quien;
            String limpio = dicho.strip().replaceAll("[\\s]+", " ");
            rumor = (quien.equals("alguien") ? "Hay alguien que" : quien) + " anda loco por algo: " + "«" + limpio + "»";
        }
        if (rumor == null || rumor.isBlank() || rumor.length() < 8) return;
        String s = sobre == null ? "" : sobre.strip();
        String bajo = s.toLowerCase(java.util.Locale.ROOT);
        boolean delForastero = bajo.isEmpty() || bajo.equals("forastero") || bajo.equals("el forastero")
                || bajo.equals("la forastera") || bajo.equals(jugador.getGameProfile().getName().toLowerCase(java.util.Locale.ROOT));
        datos.nuevoRumor(h.aldea(), delForastero ? jugador.getGameProfile().getName() : s,
                delForastero ? jugador.getUUID() : null, rumor, h.getUUID(), nivel.getGameTime());
    }

    /** Corta la conversación en seco (le han atacado o le han echado): termina la conversación y se aleja. */
    public static void cortar(ServerPlayer jugador, Habitante habitante) {
        cortarSinIrse(jugador, habitante);
        habitante.alejarseDe(jugador);
    }

    /** Corta la conversación sin alejarse (porque va a pegarle). */
    public static void cortarSinIrse(ServerPlayer jugador, Habitante habitante) {
        habitante.terminarConversacion();
        HabitantesRed.CANAL.send(PacketDistributor.PLAYER.with(() -> jugador), new LineaConversacionPacket(habitante.getId(),
                Component.translatable("yonga.habla.corta", habitante.nombrePara(jugador))
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true));
    }

    /** Las acciones que la IA quiere hacer, con los límites que pone la opinión. */
    private static void cumplir(ServerPlayer jugador, Habitante habitante, Recuerdo recuerdo, String accion, int producto) {
        switch (accion) {
            case "regalo" -> {
                long dia = habitante.level().getDayTime() / 24000L;
                if (recuerdo.opinion >= OPINION_REGALO && recuerdo.diaRegalo != dia && habitante.tieneTienda()) {
                    ItemStack regalo = habitante.regalarA(jugador, producto);
                    if (!regalo.isEmpty()) {
                        recuerdo.diaRegalo = dia;
                        aviso(jugador, habitante, Component.translatable("yonga.habla.regalo",
                                habitante.nombrePara(jugador), regalo.getCount(), regalo.getHoverName()));
                    }
                }
            }
            case "seguir" -> {
                int umbral = (habitante.etapa() == 1 ? 45 : 25) + (habitante.tieneRasgo(Habitante.DESCONFIADO) ? 20 : 0);
                if (habitante.etapa() == 0 || recuerdo.opinion < umbral || habitante.sigueA(jugador)) return;
                habitante.seguir(jugador);
                aviso(jugador, habitante, Component.translatable("yonga.habla.sigue", habitante.nombrePara(jugador)));
            }
            case "noviazgo", "romper", "intimidad", "casarse", "adoptar" ->
                    com.lucarita.yonga.modulos.habitantes.familia.Amor.accion(jugador, habitante, recuerdo, accion);
            case "quedarse" -> {
                if (habitante.etapa() == 0 || recuerdo.opinion < 15) return;
                habitante.quedarseQuieto(habitante.blockPosition());
            }
            case "dejar_de_seguir" -> {
                if (!habitante.sigueA(jugador)) return;
                habitante.dejarDeSeguir();
                aviso(jugador, habitante, Component.translatable("yonga.habla.deja", habitante.nombrePara(jugador)));
            }
            case "pegar", "atacar" -> {
                boolean unGolpe = "pegar".equals(accion);
                if (!habitante.puedePelear() || recuerdo.opinion > (unGolpe ? -30 : -55)) {
                    // No es de los que pegan (o no está tan enfadado): se limita a echarle si ya le cae mal
                    if (recuerdo.opinion < 0) {
                        recuerdo.enfadadoHasta = habitante.level().getGameTime() + ENFADO;
                        cortar(jugador, habitante);
                    }
                    return;
                }
                recuerdo.enfadadoHasta = habitante.level().getGameTime() + ENFADO;
                aviso(jugador, habitante, Component.translatable(unGolpe ? "yonga.habla.pega" : "yonga.habla.ataca",
                        habitante.nombrePara(jugador)).withStyle(ChatFormatting.RED));
                cortarSinIrse(jugador, habitante);
                habitante.empezarPelea(jugador, unGolpe);
            }
            case "echar" -> {
                if (recuerdo.opinion >= 0) return;          // solo echa a quien le cae mal
                recuerdo.enfadadoHasta = habitante.level().getGameTime() + ENFADO;
                cortar(jugador, habitante);
            }
            default -> { }
        }
    }

    /** ¿Habla de comprar, vender o precios? Solo entonces se le recuerda su lista de venta (si no, habla de otras cosas). */
    static boolean hablaDeTienda(String texto) {
        if (pareceRegateo(texto)) return true;
        String t = texto.toLowerCase();
        for (String clave : new String[]{"vend", "compr", "precio", "cuest", "cuánto", "cuanto", "moneda", "oro", "tienda",
                "ofrec", "caro", "trato", "pagar", "pago", "tienes", "comerci", "negocio", "buy", "sell", "price", "cost",
                "how much", "shop", "trade"}) {
            if (t.contains(clave)) return true;
        }
        return false;
    }

    /** ¿Pregunta por encargos, misiones, trabajos o tareas? (al tabernero) */
    static boolean hablaDeEncargos(String texto) {
        String t = texto.toLowerCase();
        for (String clave : new String[]{"encarg", "misi", "tarea", "trabaj", "faena", "recompensa", "tabl", "quest",
                "job", "task", "board", "ayuda", "necesit", "hacer algo"}) {
            if (t.contains(clave)) return true;
        }
        return false;
    }

    /** ¿Suena a regateo? (pide rebaja, más barato, un descuento, o propone números) */
    private static boolean pareceRegateo(String texto) {
        String t = texto.toLowerCase();
        if (t.matches(".*\\d.*")) return true;
        for (String clave : new String[]{"rebaj", "barat", "descuent", "menos", "regate", "oferta", "déjame", "dejame",
                "déjamelo", "dejamelo", "me lo dejas", "me la dejas", "mejor precio", "más por", "mas por", "por menos",
                "cheaper", "discount", "deal", "lower", "less"}) {
            if (t.contains(clave)) return true;
        }
        return false;
    }

    /** ¿Es igual (o casi) a alguna de sus últimas 4 frases? Compara las palabras, sin mayúsculas ni signos. */
    private static boolean repetida(Recuerdo recuerdo, String respuesta) {
        java.util.Set<String> nuevas = palabras(respuesta);
        if (nuevas.isEmpty()) return false;
        int vistas = 0;
        java.util.Iterator<String> it = recuerdo.frases.descendingIterator();
        while (it.hasNext() && vistas < 4) {
            String f = it.next();
            if (!f.startsWith("h:")) continue;
            vistas++;
            java.util.Set<String> viejas = palabras(f.substring(2));
            java.util.Set<String> comunes = new java.util.HashSet<>(nuevas);
            comunes.retainAll(viejas);
            java.util.Set<String> todas = new java.util.HashSet<>(nuevas);
            todas.addAll(viejas);
            if (!todas.isEmpty() && (double) comunes.size() / todas.size() >= 0.7) return true;
        }
        return false;
    }

    private static java.util.Set<String> palabras(String texto) {
        java.util.Set<String> set = new java.util.HashSet<>();
        for (String p : texto.toLowerCase().split("[^\\p{L}\\p{N}]+")) {
            if (!p.isEmpty()) set.add(p);
        }
        return set;
    }

    /**
     * Lo máximo que rebaja según lo duro que sea regateando y lo que opine del jugador: nada por debajo de −20; si no,
     * su flexibilidad (2..40 %) + opinión/4, entre 2 y 50 %.
     */
    public static int maxRebaja(Habitante h, int opinion) {
        return opinion < -20 ? 0 : Math.max(2, Math.min(50, h.flexibilidad() + opinion / 4));
    }

    /**
     * La IA ha fallado: se apunta el motivo en el registro (y en la pantalla de quien habla, en gris, para saber qué
     * pasa) y contesta con una frase sencilla.
     */
    private static void fallo(ServerPlayer jugador, Habitante habitante, Recuerdo recuerdo, String motivo) {
        long ahora = System.currentTimeMillis();
        if (ahora - ultimoAvisoFallo > 30_000) {
            ultimoAvisoFallo = ahora;
            LOG.warn("Yonga: la IA de los habitantes ha fallado ({}). Ollama en {} con el modelo {}.", motivo,
                    HabitantesConfig.IA_URL.get(), HabitantesConfig.IA_MODELO.get());
        }
        // Si es por el límite del plan (y ya se ha esperado todo lo posible), nada de avisos: contesta sin más
        if (!motivo.startsWith("límite del plan")) {
            linea(jugador, habitante, Component.translatable("yonga.habla.fallo", motivo)
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        responderSinIA(jugador, habitante, recuerdo);
    }

    private static String motivo(Throwable error) {
        Throwable causa = error;
        while ((causa instanceof CompletionException || causa instanceof java.util.concurrent.ExecutionException)
                && causa.getCause() != null) {
            causa = causa.getCause();
        }
        if (causa instanceof java.net.http.HttpTimeoutException) return "tarda demasiado";
        if (causa instanceof java.net.ConnectException) return "Ollama no está encendido";
        String m = causa.getMessage();
        if (m == null) m = causa.getClass().getSimpleName();
        return m.length() > 120 ? m.substring(0, 120) + "…" : m;
    }

    /** Sin IA: una frase sencilla según lo que opine del jugador. */
    private static void responderSinIA(ServerPlayer jugador, Habitante habitante, Recuerdo recuerdo) {
        String tipo = habitante.etapa() == 1 ? "nino"
                : recuerdo.opinion < -30 ? "hostil" : recuerdo.opinion > 30 ? "amable" : "neutral";
        decir(habitante, jugador, frase(tipo, habitante), null);
    }

    // =====================================================================================================
    // DECIR
    // =====================================================================================================

    /** Bocadillo sobre la cabeza (lo ven todos los cercanos) y línea en la pantalla de quien le habla. Nada en el chat. */
    private static void decir(Habitante habitante, ServerPlayer aQuien, Component texto, String crudo) {
        int largo = crudo == null ? 40 : crudo.length();
        burbuja(habitante, texto, Math.min(400, 80 + largo * 3));
        linea(aQuien, habitante, lineaHabitante(habitante.nombrePara(aQuien), texto));
    }

    /**
     * Lo que pasa con los botones (regalar, declararse…): la acción del jugador en cursiva, la respuesta del habitante
     * (bocadillo y línea) y las dos cosas quedan en su memoria, para que la IA sepa luego lo que pasó.
     */
    /** Un balbuceo de bebé. */
    public static Component fraseBebe(Habitante bebe) {
        return frase("bebe", bebe);
    }

    public static void reaccion(ServerPlayer jugador, Habitante habitante, String accion, Component respuesta) {
        Recuerdo r = habitante.recuerdo(jugador);
        linea(jugador, habitante, lineaJugador(accion));
        r.anotar(true, accion);
        r.anotar(false, respuesta.getString());
        decir(habitante, jugador, respuesta, null);
    }

    /** Aviso (regalo, etc.) en gris, solo en la pantalla de conversación. */
    public static void aviso(ServerPlayer jugador, Habitante habitante, Component texto) {
        linea(jugador, habitante, texto.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    private static void linea(ServerPlayer jugador, Habitante habitante, Component linea) {
        HabitantesRed.CANAL.send(PacketDistributor.PLAYER.with(() -> jugador),
                new LineaConversacionPacket(habitante.getId(), linea, false));
    }

    private static void burbuja(Habitante habitante, Component texto, int ticks) {
        HabitantesRed.CANAL.send(PacketDistributor.TRACKING_ENTITY.with(() -> habitante),
                new BurbujaPacket(habitante.getId(), texto, ticks));
    }

    public static Component lineaHabitante(Component nombre, Component texto) {
        return Component.literal("<").append(nombre).append("> ").append(texto);
    }

    public static Component lineaJugador(String texto) {
        return Component.translatable("yonga.habla.tu", texto).withStyle(ChatFormatting.GRAY);
    }

    /** Frase sencilla traducible: yonga.frase.<tipo>.0..3 */
    private static Component frase(String tipo, Habitante habitante) {
        return Component.translatable("yonga.frase." + tipo + "." + habitante.getRandom().nextInt(4));
    }

    // =====================================================================================================
    // OLLAMA
    // =====================================================================================================

    /**
     * POST /api/chat en streaming: cada trozo que llega se pasa a "parcial" (el texto acumulado) y al final se
     * devuelve todo (el JSON que pedimos).
     */
    private static CompletableFuture<String> pedir(JsonObject cuerpo, java.util.function.Consumer<String> parcial) {
        if (online()) {
            return pedirOnline(cuerpo, parcial);
        }
        HttpRequest peticion;
        try {
            peticion = HttpRequest.newBuilder(URI.create(urlBase() + "/api/chat"))
                    .timeout(Duration.ofSeconds(HabitantesConfig.IA_ESPERA.get()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo.toString(), StandardCharsets.UTF_8))
                    .build();
        } catch (IllegalArgumentException e) {
            return CompletableFuture.failedFuture(e);
        }
        return HTTP.sendAsync(peticion, HttpResponse.BodyHandlers.ofLines()).thenCompose(res -> {
            if (res.statusCode() / 100 != 2) {
                String cuerpoError = String.join("\n", res.body().toList());
                // Modelos que no saben "pensar" rechazan el campo think: se repite sin él
                if (res.statusCode() == 400 && cuerpo.has("think") && cuerpoError.contains("think")) {
                    JsonObject sin = cuerpo.deepCopy();
                    sin.remove("think");
                    return pedir(sin, parcial);
                }
                throw new CompletionException(new IOException("HTTP " + res.statusCode() + ": " + cuerpoError));
            }
            StringBuilder todo = new StringBuilder();
            res.body().forEach(linea -> {
                if (linea.isBlank()) return;
                JsonObject trozo = JsonParser.parseString(linea).getAsJsonObject();
                if (trozo.has("error")) {
                    throw new CompletionException(new IOException(trozo.get("error").getAsString()));
                }
                if (trozo.has("message")) {
                    JsonObject mensaje = trozo.getAsJsonObject("message");
                    if (mensaje.has("content")) {
                        todo.append(mensaje.get("content").getAsString());
                        parcial.accept(todo.toString());
                    }
                }
                // Se quedó sin palabras antes de contestar (pensando demasiado): se avisa con un motivo claro
                if (trozo.has("done_reason") && "length".equals(trozo.get("done_reason").getAsString())
                        && todo.toString().isBlank()) {
                    throw new CompletionException(new IOException("se ha quedado sin palabras pensando (prueba sinPensar = true)"));
                }
            });
            return CompletableFuture.completedFuture(todo.toString());
        });
    }

    /** ¿Se usa una API online (compatible con OpenAI) en lugar de Ollama? */
    public static boolean online() {
        return "online".equalsIgnoreCase(HabitantesConfig.IA_PROVEEDOR.get().strip());
    }

    /**
     * API online compatible con OpenAI (Groq, Gemini, DeepSeek, OpenRouter…): POST {url}/chat/completions en streaming
     * (eventos "data: {...}"). Se traduce la petición de Ollama: mismos mensajes y temperatura; "max_tokens" en lugar de
     * "num_predict"; y se pide JSON con response_format = json_object (lo admiten todos; el esquema exacto va en las
     * instrucciones). La clave va en la cabecera Authorization.
     */
    private static CompletableFuture<String> pedirOnline(JsonObject ollama, java.util.function.Consumer<String> parcial) {
        String clave = HabitantesConfig.ONLINE_CLAVE.get().strip();
        if (clave.isEmpty()) {
            return CompletableFuture.failedFuture(new IOException("falta la clave de la API en yonga-habitantes.toml"));
        }
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("model", HabitantesConfig.ONLINE_MODELO.get().strip());
        cuerpo.add("messages", ollama.get("messages"));
        cuerpo.addProperty("stream", true);
        JsonObject opciones = ollama.getAsJsonObject("options");
        if (opciones != null) {
            if (opciones.has("temperature")) cuerpo.add("temperature", opciones.get("temperature"));
            if (opciones.has("top_p")) cuerpo.add("top_p", opciones.get("top_p"));
            if (opciones.has("num_predict")) cuerpo.addProperty("max_tokens", Math.min(2048, opciones.get("num_predict").getAsInt()));
        }
        String modelo = HabitantesConfig.ONLINE_MODELO.get().toLowerCase(java.util.Locale.ROOT);
        if (modelo.contains("gpt-oss")) {
            // Estos modelos "piensan" antes de contestar y ese pensamiento gasta max_tokens: que piensen poco y con margen
            cuerpo.addProperty("reasoning_effort", "low");
            int max = cuerpo.has("max_tokens") ? cuerpo.get("max_tokens").getAsInt() : 400;
            cuerpo.addProperty("max_tokens", max + 1024);
        }
        cuerpo.addProperty("frequency_penalty", 0.4);       // como el repeat_penalty de Ollama: que no se repita
        // Groq obliga a cumplir el esquema exacto ("strict") con los gpt-oss; con otros modelos o proveedores se pide sin
        // formato y se rescata el JSON del texto (el esquema ya va explicado en las instrucciones)
        if (modelo.contains("gpt-oss") && ollama.has("format") && ollama.get("format").isJsonObject()) {
            JsonObject esquema = ollama.getAsJsonObject("format").deepCopy();
            esquema.addProperty("additionalProperties", false);
            JsonObject js = new JsonObject();
            js.addProperty("name", "respuesta");
            js.addProperty("strict", true);
            js.add("schema", esquema);
            JsonObject formato = new JsonObject();
            formato.addProperty("type", "json_schema");
            formato.add("json_schema", js);
            cuerpo.add("response_format", formato);
            cuerpo.addProperty("stream", false);           // Groq no admite streaming con formato estricto
        }
        long hasta = System.currentTimeMillis() + HabitantesConfig.IA_LIMITE.get() * 1000L;
        return enviarEsperando(cuerpo, clave, parcial, hasta).exceptionallyCompose(error -> {
            Throwable causa = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
            String msg = String.valueOf(causa.getMessage());
            // Si el modelo no ha cumplido el formato (o el proveedor no lo admite), se repite sin formato obligatorio
            if (cuerpo.has("response_format") && msg.startsWith("HTTP 400")) {
                JsonObject sin = cuerpo.deepCopy();
                sin.remove("response_format");
                sin.addProperty("stream", true);
                return enviarEsperando(sin, clave, parcial, hasta);
            }
            return CompletableFuture.failedFuture(causa);
        });
    }

    /** El proveedor ha dicho "demasiados mensajes, espera": cuánto (en milisegundos). */
    private static final class Limite extends IOException {
        final long espera;

        Limite(long espera, String detalle) {
            super("límite del plan: " + detalle);
            this.espera = espera;
        }
    }

    /**
     * Envía y, si el proveedor dice que se ha llegado al límite de mensajes (429) o está saturado (502/503), espera lo
     * que pida y lo vuelve a intentar, sin decir nada al jugador: el habitante solo parece que tarda en contestar.
     * Así hasta "hasta" (ia.limite segundos desde la pregunta); solo si se pasa de ahí falla de verdad.
     */
    private static CompletableFuture<String> enviarEsperando(JsonObject cuerpo, String clave,
                                                             java.util.function.Consumer<String> parcial, long hasta) {
        return enviarOnline(cuerpo, clave, parcial).exceptionallyCompose(error -> {
            Throwable causa = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
            if (!(causa instanceof Limite limite)) return CompletableFuture.failedFuture(causa);
            long espera = Math.max(1000L, Math.min(30_000L, limite.espera)) + 300L;
            if (System.currentTimeMillis() + espera > hasta) return CompletableFuture.failedFuture(causa);
            return CompletableFuture.supplyAsync(() -> null,
                            CompletableFuture.delayedExecutor(espera, java.util.concurrent.TimeUnit.MILLISECONDS))
                    .thenCompose(nada -> enviarEsperando(cuerpo, clave, parcial, hasta));
        });
    }

    /** Lo que pide esperar el proveedor: cabecera Retry-After o el texto "try again in 7.5s" / "1m2s" / "450ms". */
    private static long esperaPedida(HttpResponse<?> res, String error) {
        var cabecera = res.headers().firstValue("retry-after");
        if (cabecera.isPresent()) {
            try {
                return (long) (Double.parseDouble(cabecera.get().strip()) * 1000);
            } catch (NumberFormatException ignorado) {
                // era una fecha: se usa el texto
            }
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("try again in ([0-9hms.]+)").matcher(error);
        if (m.find()) {
            double ms = 0;
            java.util.regex.Matcher t = java.util.regex.Pattern.compile("([\\d.]+)(ms|h|m|s)").matcher(m.group(1));
            while (t.find()) {
                double v;
                try {
                    v = Double.parseDouble(t.group(1));
                } catch (NumberFormatException e) {
                    continue;
                }
                ms += switch (t.group(2)) {
                    case "ms" -> v;
                    case "h" -> v * 3_600_000.0;
                    case "m" -> v * 60_000.0;
                    default -> v * 1000.0;
                };
            }
            if (ms > 0) return (long) ms;
        }
        return 5000L;
    }

    private static CompletableFuture<String> enviarOnline(JsonObject cuerpo, String clave, java.util.function.Consumer<String> parcial) {
        HttpRequest peticion;
        try {
            peticion = HttpRequest.newBuilder(URI.create(HabitantesConfig.ONLINE_URL.get().strip().replaceAll("/+$", "")
                            + "/chat/completions"))
                    .timeout(Duration.ofSeconds(HabitantesConfig.IA_ESPERA.get()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + clave)
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo.toString(), StandardCharsets.UTF_8))
                    .build();
        } catch (IllegalArgumentException e) {
            return CompletableFuture.failedFuture(e);
        }
        return HTTP.sendAsync(peticion, HttpResponse.BodyHandlers.ofLines()).thenApply(res -> {
            if (res.statusCode() / 100 != 2) {
                String error = String.join(" ", res.body().toList());
                try {
                    String rescate = textoFallido(JsonParser.parseString(error).getAsJsonObject());
                    if (rescate != null) {
                        int a = rescate.indexOf('{'), b = rescate.lastIndexOf('}');
                        return a >= 0 && b > a ? rescate.substring(a, b + 1) : rescate;
                    }
                } catch (RuntimeException ignorado) {
                    // no era JSON: se informa del error tal cual
                }
                if (res.statusCode() == 429 || res.statusCode() == 502 || res.statusCode() == 503) {
                    throw new CompletionException(new Limite(esperaPedida(res, error),
                            error.length() > 200 ? error.substring(0, 200) : error));
                }
                String motivo = switch (res.statusCode()) {
                    case 401, 403 -> "la clave de la API no vale";
                    case 404 -> "ese modelo no existe en el proveedor";
                    case 429 -> "se ha llegado al límite de mensajes del plan (espera un poco)";
                    default -> "HTTP " + res.statusCode();
                };
                throw new CompletionException(new IOException(motivo + ": " + (error.length() > 200 ? error.substring(0, 200) : error)));
            }
            StringBuilder todo = new StringBuilder();
            StringBuilder entero = new StringBuilder();        // por si contesta sin streaming (un único JSON)
            res.body().forEach(linea -> {
                if (!linea.startsWith("data:")) {
                    entero.append(linea).append('\n');
                    return;
                }
                String datos = linea.substring(5).strip();
                if (datos.isEmpty() || datos.equals("[DONE]")) return;
                JsonObject trozo = JsonParser.parseString(datos).getAsJsonObject();
                if (trozo.has("error")) {
                    String rescate = textoFallido(trozo);
                    if (rescate != null) { todo.setLength(0); todo.append(rescate); return; }
                    throw new CompletionException(new IOException(trozo.get("error").toString()));
                }
                JsonArray opciones2 = trozo.getAsJsonArray("choices");
                if (opciones2 == null || opciones2.isEmpty()) return;
                JsonObject delta = opciones2.get(0).getAsJsonObject().getAsJsonObject("delta");
                if (delta != null && delta.has("content") && !delta.get("content").isJsonNull()) {
                    todo.append(delta.get("content").getAsString());
                    parcial.accept(todo.toString());
                }
            });
            if (todo.isEmpty() && !entero.toString().isBlank()) {
                JsonObject json = JsonParser.parseString(entero.toString()).getAsJsonObject();
                if (json.has("error")) {
                    String rescate = textoFallido(json);
                    if (rescate == null) throw new CompletionException(new IOException(json.get("error").toString()));
                    todo.append(rescate);
                } else {
                    JsonArray opciones2 = json.getAsJsonArray("choices");
                    if (opciones2 != null && !opciones2.isEmpty()) {
                        JsonObject mensaje = opciones2.get(0).getAsJsonObject().getAsJsonObject("message");
                        if (mensaje != null && mensaje.has("content") && !mensaje.get("content").isJsonNull()) {
                            todo.append(mensaje.get("content").getAsString());
                            parcial.accept(todo.toString());
                        }
                    }
                }
            }
            String texto = todo.toString();
            int a = texto.indexOf('{'), b = texto.lastIndexOf('}');
            return a >= 0 && b > a ? texto.substring(a, b + 1) : texto;      // sin ```json ni texto alrededor
        });
    }

    /**
     * Groq devuelve error "json_validate_failed" cuando el modelo no ha cumplido el formato, pero adjunta lo que escribió
     * en "failed_generation": casi siempre se puede aprovechar (se rescata la respuesta del texto).
     */
    private static String textoFallido(JsonObject json) {
        JsonElement e = json.get("error");
        if (e == null || !e.isJsonObject()) return null;
        JsonElement fallo = e.getAsJsonObject().get("failed_generation");
        return fallo != null && fallo.isJsonPrimitive() && !fallo.getAsString().isBlank() ? fallo.getAsString() : null;
    }

    private static String urlBase() {
        return HabitantesConfig.IA_URL.get().replaceAll("/+$", "");
    }

    /**
     * Al arrancar el servidor se carga el modelo en la tarjeta gráfica (petición vacía a /api/generate), para que la
     * primera conversación no tenga que esperar a que se cargue.
     */
    public static void alArrancarServidor(net.minecraftforge.event.server.ServerStartedEvent evento) {
        if (!HabitantesConfig.IA_ACTIVADA.get() || online()) return;
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("model", HabitantesConfig.IA_MODELO.get());
        cuerpo.addProperty("keep_alive", "60m");
        try {
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(urlBase() + "/api/generate"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo.toString(), StandardCharsets.UTF_8))
                    .build();
            HTTP.sendAsync(peticion, HttpResponse.BodyHandlers.discarding()).whenComplete((r, e) -> {
                if (e != null) {
                    LOG.warn("Yonga: no se ha podido cargar el modelo de IA de los habitantes ({})", e.getMessage());
                } else {
                    LOG.info("Yonga: modelo de IA de los habitantes cargado ({})", HabitantesConfig.IA_MODELO.get());
                }
            });
        } catch (IllegalArgumentException e) {
            LOG.warn("Yonga: dirección de Ollama no válida: {}", HabitantesConfig.IA_URL.get());
        }
    }

    private static String texto(JsonElement e) {
        return e == null || e.isJsonNull() ? "" : e.getAsString();
    }

    /** Una sola línea, sin comillas sueltas ni acotaciones de rol, y no demasiado larga. */
    private static String limpiar(String s) {
        s = s.replaceAll("\\s+", " ").strip();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length() - 1).strip();
        if (s.length() > 320) {
            int corte = s.lastIndexOf('.', 320);
            s = corte > 80 ? s.substring(0, corte + 1) : s.substring(0, 320) + "…";
        }
        return s;
    }
}
