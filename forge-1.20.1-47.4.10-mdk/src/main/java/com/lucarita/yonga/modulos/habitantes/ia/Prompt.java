package com.lucarita.yonga.modulos.habitantes.ia;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lucarita.yonga.modulos.habitantes.Epoca;
import com.lucarita.yonga.modulos.habitantes.Habitante;
import com.lucarita.yonga.modulos.habitantes.HabitantesConfig;
import com.lucarita.yonga.modulos.habitantes.Oficio;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Lo que se le manda a la IA: quién es el habitante (instrucciones de sistema), lo último que se han dicho y la frase
 * nueva, pidiendo una respuesta en JSON con un formato fijo (Ollama lo obliga con "format").
 */
final class Prompt {

    private Prompt() {}

    /**
     * @param conNotas si esta vez debe reescribir sus notas (memoria larga). Solo cada pocas frases: es el campo más
     *                 largo y lo que más alarga la respuesta.
     * @param streaming si se pide la respuesta a trozos (para ir enseñándola en el bocadillo mientras se escribe)
     */
    static JsonObject peticion(ServerPlayer jugador, Habitante h, Recuerdo r, String texto, boolean conNotas, boolean streaming) {
        return peticion(jugador, h, r, texto, conNotas, streaming, null, 0.0);
    }

    /**
     * @param aviso       nota extra para esta vez (p. ej. "has repetido esto, di otra cosa"), o null
     * @param masCreativo cuánto subir la temperatura esta vez
     */
    static JsonObject peticion(ServerPlayer jugador, Habitante h, Recuerdo r, String texto, boolean conNotas,
                               boolean streaming, String aviso, double masCreativo) {
        JsonArray mensajes = new JsonArray();
        String sistema = sistema(jugador, h, r, conNotas);
        mensajes.add(mensaje("system", sistema));
        // Solo las frases más recientes que quepan en el contexto del modelo (≈3 letras por token, dejando sitio
        // para la respuesta). Si no, Ollama recortaría por el principio... incluidas las instrucciones.
        int presupuesto = (int) (HabitantesConfig.IA_CONTEXTO.get() * 3.0) - sistema.length() - texto.length() - 2400;
        java.util.List<String> caben = new java.util.ArrayList<>();
        java.util.Iterator<String> it = r.frases.descendingIterator();
        while (it.hasNext()) {
            String frase = it.next();
            presupuesto -= frase.length() + 20;
            if (presupuesto < 0) break;
            caben.add(0, frase);
        }
        for (String frase : caben) {
            mensajes.add(mensaje(frase.startsWith("j:") ? "user" : "assistant", frase.substring(2)));
        }
        // Lo último que dice, con una nota para que conteste a ESO y no se repita (los modelos pequeños tienden a copiar
        // sus frases anteriores). "/no_think": interruptor de qwen3 para que no razone antes de contestar.
        StringBuilder ultimo = new StringBuilder(texto);
        ultimo.append("\n\n(Nota para ti, no la menciones: contesta a esto en concreto, con palabras nuevas. "
                + "No repitas ninguna frase que ya hayas dicho.");
        if (h.tieneTienda() && (Conversaciones.hablaDeTienda(texto) || h.tieneRasgo(Habitante.AVARICIOSO))) {
            // Lo que tiene AHORA (solo si habla de comprar o vender: si no, la lista arrastraría la charla a la tienda) (puede haber cambiado desde lo último que dijo): nunca ofrece lo que no tiene
            String tienda = h.resumenTienda(jugador);
            ultimo.append(tienda.isEmpty() ? " Ahora mismo no te queda nada a la venta: no ofrezcas nada."
                    : " Lo que tienes a la venta AHORA MISMO, y nada más:" + tienda.replace("\n", " ")
                    + ". No ofrezcas nada que no esté en esta lista.");
        }
        // El tabernero: si le preguntan por encargos (o trabajos, tareas…), lo que tiene colgado AHORA en el tablón
        if (h.oficio() != null && h.oficio().esTabernero() && h.aldea() != null && Conversaciones.hablaDeEncargos(texto)
                && h.level() instanceof net.minecraft.server.level.ServerLevel nv) {
            String encargos = com.lucarita.yonga.modulos.habitantes.taberna.Taberna.resumen(nv, h.aldea());
            ultimo.append(encargos.isEmpty() ? " Ahora mismo no tienes encargos en el tablón: di que vuelva mañana."
                    : " Los encargos que tienes colgados AHORA en el tablón, y ningún otro (cada uno se paga con una moneda al primero que lo cumpla; se aceptan y se entregan en el tablón de la taberna o contigo, con el botón \"Encargos\"):"
                    + encargos.replace("\n", " ") + ". No te inventes otros.");
        }
        if (aviso != null) ultimo.append(" ").append(aviso);
        ultimo.append(")");
        if (HabitantesConfig.IA_SIN_PENSAR.get() && !Conversaciones.online()) ultimo.append(" /no_think");
        mensajes.add(mensaje("user", ultimo.toString()));

        JsonObject opciones = new JsonObject();
        opciones.addProperty("temperature", Math.min(1.5, HabitantesConfig.IA_TEMPERATURA.get() + masCreativo));
        // Castiga repetir palabras ya dichas en la conversación
        opciones.addProperty("repeat_penalty", 1.18);
        opciones.addProperty("repeat_last_n", 512);
        opciones.addProperty("top_p", 0.92);
        // Si piensa antes de contestar, el razonamiento también gasta palabras: hay que dejarle bastantes más
        int palabras = conNotas ? 1100 : 240;
        if (!HabitantesConfig.IA_SIN_PENSAR.get() && !Conversaciones.online()) palabras += 2000;
        opciones.addProperty("num_predict", palabras);
        opciones.addProperty("num_ctx", HabitantesConfig.IA_CONTEXTO.get());

        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("model", HabitantesConfig.IA_MODELO.get());
        cuerpo.add("messages", mensajes);
        cuerpo.addProperty("stream", streaming);
        cuerpo.add("format", esquema(conNotas));
        cuerpo.add("options", opciones);
        cuerpo.addProperty("keep_alive", "60m");
        if (HabitantesConfig.IA_SIN_PENSAR.get()) {
            cuerpo.addProperty("think", false);
        }
        return cuerpo;
    }

    private static JsonObject mensaje(String rol, String contenido) {
        JsonObject m = new JsonObject();
        m.addProperty("role", rol);
        m.addProperty("content", contenido);
        return m;
    }

    private static JsonObject esquema(boolean conNotas) {
        // El orden importa: el modelo escribe los campos en este orden, así decide su postura ANTES de hablar
        JsonObject props = new JsonObject();
        props.add("opinion", tipo("integer"));
        JsonObject accion = tipo("string");
        JsonArray valores = new JsonArray();
        for (String v : new String[]{"ninguna", "regatear", "regalo", "seguir", "dejar_de_seguir", "echar", "pegar", "atacar",
                "noviazgo", "romper", "intimidad", "casarse", "adoptar", "quedarse"}) {
            valores.add(v);
        }
        accion.add("enum", valores);
        props.add("accion", accion);
        props.add("producto", tipo("integer"));
        props.add("cantidad", tipo("integer"));
        props.add("precio", tipo("integer"));
        JsonObject postura = tipo("string");
        JsonArray posturas = new JsonArray();
        for (String v : new String[]{"aceptar", "contraoferta", "rechazar", "ofendido"}) posturas.add(v);
        postura.add("enum", posturas);
        props.add("postura", postura);
        props.add("respuesta", tipo("string"));
        props.add("dice_nombre", tipo("boolean"));
        props.add("nombre_forastero", tipo("string"));
        props.add("rumor_sobre", tipo("string"));
        props.add("rumor", tipo("string"));
        if (conNotas) props.add("notas", tipo("string"));

        JsonArray requeridos = new JsonArray();
        for (String k : new String[]{"opinion", "accion", "producto", "cantidad", "precio", "postura", "respuesta", "dice_nombre",
                "nombre_forastero", "rumor_sobre", "rumor"}) {
            requeridos.add(k);
        }
        if (conNotas) requeridos.add("notas");

        JsonObject esquema = tipo("object");
        esquema.add("properties", props);
        esquema.add("required", requeridos);
        return esquema;
    }

    private static JsonObject tipo(String t) {
        JsonObject o = new JsonObject();
        o.addProperty("type", t);
        return o;
    }

    // =====================================================================================================

    /** Las reglas, iguales para todos: van PRIMERO para que Ollama reaproveche su cálculo de una petición a otra. */
    private static final String REGLAS = """
            Eres un habitante de una aldea y hablas con un forastero. Más abajo está quién eres.

            Cómo hablar:
            - Eres una persona real de tu época y de tu aldea. Nunca digas que eres una IA, un personaje o parte de un juego.
            - No conoces nada de otras épocas: si te hablan de máquinas, ordenadores u otras cosas que no existen en tu tiempo, no lo entiendes.
            - Responde en el mismo idioma en que te habla el forastero, con una a tres frases cortas, como hablaría alguien de tu época, tu edad y tu carácter.
            - Tienes voluntad propia: puedes negarte, discutir, desconfiar, bromear, enfadarte o emocionarte. No eres un sirviente.
            - Lee con atención y con buena fe: si te cuenta algo que le ha pasado con otra persona, te lo está contando, no te lo está haciendo a ti. No te ofendas por cosas normales.
            - Si te insultan o te amenazan A TI, tu opinión baja mucho.
            - Lo fácil que coges confianza depende de tu carácter: la mayoría de la gente es abierta y una charla agradable, interesarse por ti o hacerte reír ya te hace apreciar al forastero (opinión positiva); los desconfiados y los gruñones tardan más, y los halagos vacíos les escaman.
            - Tienes tu propia vida: tus aficiones, tus preocupaciones, tus recuerdos, tus vecinos y lo que pasa en la aldea. Habla de todo ello con naturalidad: cuenta anécdotas y cotilleos, da tu opinión, pregúntale al forastero por él y por sus viajes, y sigue los temas que saque.
            - Tu trabajo es solo una parte de tu vida: no saques tu oficio ni lo que vendes si no viene a cuento, y no conviertas cada charla en una venta (salvo que seas avaricioso).
            - No digas tu nombre salvo que te lo pregunten y no desconfíes del forastero.
            - Si te preguntan qué vendes o cuánto cuesta, dilo tal cual está en tu lista.
            - Regateo: si te pide un precio mejor (de algo concreto o de todo), "accion" es "regatear" y eliges tu "postura" según tu carácter, lo que piensas de él y sus argumentos: "aceptar", "contraoferta" (le ofreces algo a medio camino), "rechazar" u "ofendido" (te ofende y le subes el precio). En "cantidad" y "precio" pon lo que él te ha pedido (0 y 0 si no dijo números).
            - Preguntar el precio NO es regatear: si te pregunta cuánto cuesta algo, "accion" es "ninguna" y le dices el precio de tu lista.
            - Cuando regateas, en tu "respuesta" NO digas ningún número ni cantidad: solo tu reacción, con tus propias palabras. Tus números exactos los añadiré yo al final de lo que digas.
            - Solo echas a alguien si de verdad te ha ofendido o no lo soportas.
            - Solo vendes lo que tienes en tu lista en este momento, al precio de la lista. Si algo se ha agotado o no lo tienes, dilo.
            - "seguir": si te pide que le acompañes o le sigas y te fías de verdad de él (si no, dile que no). "dejar_de_seguir": si te pide que te quedes o que dejes de seguirle, o si quieres volver a casa.
            - Si te ofende gravemente: según tu carácter, puedes echarle ("echar"), darle un golpe e irte ("pegar") o, si es intolerable, ir a por él a pelearte ("atacar"). Los tímidos y los niños nunca pegan.
            - Siempre contestas algo. Si te insultan, aunque sea con palabras muy fuertes, respondes en tu papel, enfadado u ofendido (puedes insultar de vuelta o echarle); nunca te quedas callado, no das sermones y no dices que no puedes responder.
            - Nunca digas dos veces la misma frase. Responde siempre a lo último que te ha dicho: si te pregunta algo, contéstalo (aunque sea de mala gana); si se disculpa, reacciona a la disculpa.
            - Tu humor cambia con lo que te dicen: una disculpa sincera o un trato amable te ablandan (opinión positiva); no te quedes enfadado para siempre.
            - Si eres un niño, hablas con palabras sencillas, como un niño de tu edad.

            Devuelve solo JSON:
            - "opinion": de -10 a 10, cuánto cambia lo que piensas del forastero por lo que acaba de decir (0 si nada).
            - "accion": "ninguna"; "regatear" si te está regateando; "regalo" si le regalas algo de lo que vendes (solo si le aprecias de verdad); "seguir" o "dejar_de_seguir"; "echar" si no quieres seguir hablando con él; "pegar" o "atacar" si te ha ofendido gravemente; "noviazgo", "romper", "intimidad", "casarse" o "adoptar" solo si más abajo se te dice que puedes.
            - "producto": el número del artículo que regatea o que regalas (0 = todo / ninguno en concreto).
            - "cantidad" y "precio": lo que él te ha pedido, cuántos por cuántas monedas (0 y 0 si no dijo números o no regatea).
            - "postura": si regatea, "aceptar", "contraoferta", "rechazar" u "ofendido"; si no, "rechazar".
            - "respuesta": lo que dices (sin números si regatea).
            - "dice_nombre": true si en tu respuesta dices tu nombre.
            - "nombre_forastero": el nombre que el forastero te ha dicho que tiene en lo que acaba de decir ("" si no lo ha dicho).
            - "rumor_sobre" y "rumor": si en lo que acaba de decir te ha contado algo de alguien que se pueda ir contando por la aldea (lo que alguien quiere, hace, siente o ha hecho; también si alguien desea mucho algo o pagaría lo que fuera por algo), de quién habla (su nombre; "forastero" si es de él mismo; "alguien" si no dice quién) y el chisme en una frase en tercera persona, como lo contarías tú a un vecino. Ante la duda, apúntalo. Si no te ha contado nada de nadie, "" y "".
            - Si te cuentan que alguien desea a toda costa algo que tú vendes, se te despierta el instinto de comerciante: te interesa saber quién es, y piensas en subirle el precio cuando venga.
            """;

    private static String sistema(ServerPlayer jugador, Habitante h, Recuerdo r, boolean conNotas) {
        boolean mujer = h.esMujer();
        int etapa = h.etapa();
        StringBuilder s = new StringBuilder(REGLAS);
        if (conNotas) {
            s.append("- \"notas\": tu memoria de él, para no olvidarla aunque se te olviden las palabras exactas: su nombre si te lo dijo, lo que te ha contado (historias, personas, lugares, fechas), lo que os habéis prometido, lo que habéis hecho juntos y cómo te ha tratado. Reescribe tus notas anteriores sin perder nada importante y añade lo nuevo. Como mucho ")
                    .append(r.maxNotas()).append(" letras.\n");
        }
        s.append("\nQuién eres:\n");
        s.append("Eres ").append(h.nombre()).append(", ").append(quien(etapa, mujer))
                .append(" que vive en una aldea ").append(epoca(h)).append(".\n");
        s.append(comoHablas(h));
        s.append(mujer ? "Cuando hables de ti misma, usa el femenino.\n" : "Cuando hables de ti mismo, usa el masculino.\n");
        s.append("Tu carácter: ").append(Conversaciones.RASGOS[h.rasgo1()]).append(" y ")
                .append(Conversaciones.RASGOS[h.rasgo2()]).append(".\n");
        if (etapa >= 2) vida(s, h);
        if (etapa >= 1) s.append(com.lucarita.yonga.modulos.habitantes.Gustos.describir(h)).append("\n");
        if (h.oficio() != null && h.oficio().esTabernero() && h.aldea() != null && h.level() instanceof net.minecraft.server.level.ServerLevel nv) {
            String encargos = com.lucarita.yonga.modulos.habitantes.taberna.Taberna.resumen(nv, h.aldea());
            s.append(encargos.isEmpty() ? "Ahora mismo no tienes encargos colgados en el tablón.\n"
                    : "Encargos que tienes colgados en el tablón de la taberna (pagas en monedas al primero que los cumpla; se aceptan y entregan en el tablón o pulsando \"Encargos\" al hablar contigo):"
                    + encargos + "\n");
        }
        if (h.tieneRasgo(Habitante.AVARICIOSO) && h.tieneTienda()) {
            s.append("Eres de los que siempre acaban hablando de negocios: en cuanto puedes, llevas la conversación a lo que vendes.\n");
        }

        if (h.esLider()) {
            s.append("Eres el líder de tu aldea (").append(switch (h.epoca()) {
                        case PREHISTORICA -> mujer ? "la jefa del clan" : "el jefe del clan";
                        case ANTIGUA -> mujer ? "la magistrada" : "el magistrado";
                        default -> mujer ? "la alcaldesa" : "el alcalde";
                    })
                    .append("): te preocupas por los vecinos y hablas con autoridad.\n");
        }
        if (h.esGuardia()) {
            s.append("Eres ").append(mujer ? "la guardia" : "el guardia").append(" de la aldea: vas armado con ")
                    .append(h.epoca() == Epoca.PREHISTORICA ? "un hacha de piedra o un arco, " : "espada, ").append(
                    "proteges a los vecinos de monstruos, saqueadores y forasteros peligrosos. No vendes nada.\n");
        }
        if (h.aldea() != null && h.level() instanceof net.minecraft.server.level.ServerLevel nivel) {
            var a = com.lucarita.yonga.modulos.habitantes.aldea.Aldeas.de(nivel).aldeaSiExiste(h.aldea());
            int fama = a == null ? 0 : a.fama(jugador.getUUID());
            s.append("En tu aldea, el forastero tiene ").append(fama <= -50 ? "fama de peligroso y de haber hecho daño a vecinos"
                    : fama < -10 ? "mala fama" : fama < 10 ? "poca fama: casi nadie le conoce"
                    : fama < 50 ? "buena fama" : "muy buena fama: ha ayudado mucho a la aldea").append(".\n");
        }

        vecinos(s, jugador, h);
        rumores(s, jugador, h);
        familia(s, jugador, h);

        Oficio oficio = h.oficio();
        if (etapa == 1) {
            s.append("Eres un crío: todavía no trabajas; juegas, ayudas en casa y lo miras todo con curiosidad.\n");
        } else if (oficio == null && !h.esGuardia()) {
            s.append(h.esHolgazan()
                    ? "No tienes oficio y tampoco lo buscas: te gusta pasear, mirar las nubes y charlar; vives de lo que te dan y de algún favor. No vendes nada.\n"
                    : "No tienes oficio: estás buscando trabajo y mientras tanto echas una mano donde puedes. No vendes nada.\n");
        } else if (oficio != null) {
            s.append("Tu oficio: ").append(h.nombreOficio().getString().toLowerCase())
                    .append(etapa == 2 ? ", como aprendiz (aún eres joven)" : ", nivel " + nivel(h.nivel()))
                    .append(".\n");
            String tienda = h.resumenTienda(jugador);
            s.append(tienda.isEmpty() ? "Ahora mismo no te queda nada que vender.\n"
                    : "Tienes a la venta (" + (h.epoca() == Epoca.PREHISTORICA
                    ? "lo cambias por esas piedras redondas y brillantes que trae el forastero, las \"monedas\"; para ti son valiosas"
                    : h.epoca() == Epoca.ANTIGUA ? "se paga con monedas de oro: para ti, áureos"
                    : h.epoca() == Epoca.MODERNA ? "se paga con monedas de oro: para ti, escudos"
                    : h.epoca() == Epoca.INDUSTRIAL ? "se paga con monedas de oro: para ti, monedas de veinticinco pesetas"
                    : h.epoca() == Epoca.CONTEMPORANEA ? "aquí se paga con las monedas de oro del forastero, que en el pueblo valen como euros (no hay datáfono ni Bizum que valga)"
                    : "se paga con monedas de oro") + "; habla de ello solo si sale el tema):" + tienda + "\n");
            int flex = h.flexibilidad();
            s.append(flex <= 10 ? "Regateando eres duro de pelar: casi nunca bajas el precio.\n"
                    : flex >= 25 ? "Regateando eres de manga ancha: te gusta cerrar tratos.\n" : "");
            if (Conversaciones.maxRebaja(h, r.opinion) <= 0) s.append("Con lo mal que te cae, no le rebajarías nada.\n");
        }

        s.append("Ahora es ").append(momento(h.level())).append(".\n");
        s.append(opinion(r.opinion)).append(" (opinión ").append(r.opinion).append(" de −100 a 100).");
        if (!r.frases.isEmpty()) s.append(" Ya habíais hablado antes.");
        if (r.sabeNombre) s.append(" Ya le has dicho tu nombre.");
        if (h.sigueA(jugador)) s.append(" Ahora mismo le estás acompañando (vas con él a donde vaya).");
        s.append("\n");
        if (!r.notas.isBlank()) s.append("Lo que recuerdas de él: ").append(r.notas).append("\n");
        return s.toString();
    }

    /**
     * Quién vive en su aldea (para que sepa de quién le hablan) y lo que cuentan del forastero los vecinos que le
     * conocen (sus notas), para que lo que pasa con uno se sepa en el pueblo.
     */
    private static void vecinos(StringBuilder s, ServerPlayer jugador, Habitante h) {
        if (h.aldea() == null || !(h.level() instanceof net.minecraft.server.level.ServerLevel nivel)) return;
        java.util.List<Habitante> vecinos = nivel.getEntitiesOfClass(Habitante.class,
                new net.minecraft.world.phys.AABB(h.aldea()).inflate(com.lucarita.yonga.modulos.habitantes.aldea.Aldeas.RADIO),
                v -> v != h && h.aldea().equals(v.aldea()) && v.etapa() > 0);
        if (vecinos.isEmpty()) return;
        vecinos.sort(java.util.Comparator.comparingDouble(v -> v.distanceToSqr(h)));
        StringBuilder lista = new StringBuilder();
        StringBuilder chismes = new StringBuilder();
        int contados = 0, cotilleos = 0;
        for (Habitante v : vecinos) {
            if (contados < 12) {
                if (lista.length() > 0) lista.append("; ");
                lista.append(v.nombre()).append(", ").append(papel(v));
                contados++;
            }
            Recuerdo r = v.recuerdoSiExiste(jugador.getUUID());
            if (cotilleos < 3 && r != null && !r.notas.isBlank()) {
                String n = r.notas.length() > 220 ? r.notas.substring(0, 220) + "…" : r.notas;
                chismes.append("\n  - ").append(v.nombre()).append(" (").append(papel(v)).append("): ").append(n);
                cotilleos++;
            }
        }
        s.append("Tus vecinos: ").append(lista).append(".\n");
        if (chismes.length() > 0) {
            s.append("Lo que cuentan del forastero otros vecinos que le conocen (tú no estabas delante):").append(chismes).append("\n");
        }
    }

    /**
     * Su familia (padres, hermanos, pareja, hijos; quién ha muerto; si espera un bebé), su relación con el forastero y
     * hasta dónde puede llegar con él: nada romántico con niños, con la familia ni si no le atrae; nunca nada explícito.
     */
    private static void familia(StringBuilder s, ServerPlayer jugador, Habitante h) {
        if (!(h.level() instanceof net.minecraft.server.level.ServerLevel nivel)) return;
        var f = com.lucarita.yonga.modulos.habitantes.familia.Familias.de(nivel.getServer());
        java.util.UUID yo = h.getUUID(), el = jugador.getUUID();
        var pj = f.personaSiExiste(el);
        if (pj != null && pj.sexoConocido()) s.append(pj.esMujer() ? "El forastero es una mujer.\n" : "El forastero es un hombre.\n");

        StringBuilder fam = new StringBuilder();
        for (java.util.UUID u : f.padres(yo)) parentesco(fam, f, u, el, "tu padre", "tu madre");
        for (java.util.UUID u : f.hermanos(yo)) parentesco(fam, f, u, el, "tu hermano", "tu hermana");
        for (java.util.UUID u : f.hijos(yo)) parentesco(fam, f, u, el, "tu hijo", "tu hija");
        java.util.UUID pareja = f.pareja(yo);
        if (pareja != null && !pareja.equals(el)) {
            boolean casados = f.estado(yo, pareja) == com.lucarita.yonga.modulos.habitantes.familia.Familias.CASADOS;
            var pp = f.persona(pareja);
            fam.append(fam.length() > 0 ? "; " : "").append(pp.nombre).append(casados
                    ? (pp.esMujer() ? ", tu mujer" : ", tu marido") : (pp.esMujer() ? ", tu novia" : ", tu novio"));
        }
        java.util.UUID difunto = pareja == null ? f.difunto(yo) : null;
        if (difunto != null) {
            fam.append(fam.length() > 0 ? "; " : "").append(f.nombre(difunto))
                    .append(f.persona(difunto).esMujer() ? ", tu mujer, que murió" : ", tu marido, que murió");
        }
        if (fam.length() > 0) s.append("Tu familia: ").append(fam).append(".\n");
        var yoP = f.personaSiExiste(yo);
        long ahora = nivel.getGameTime();
        if (yoP != null && yoP.parto > 0 && h.esMujer()) {
            float e = yoP.embarazo(ahora);
            s.append("Estás embarazada").append(el.equals(yoP.otroProgenitor) ? " del forastero" : "")
                    .append(e < 0.33f ? ": aún no se te nota, pero tienes náuseas y antojos."
                            : e < 0.66f ? ": ya se te nota la barriga."
                            : ": te queda muy poco; tienes una barriga enorme, te cansas enseguida y ya no trabajas.")
                    .append("\n");
        }
        if (pj != null && pj.parto > 0 && pj.esMujer()) {
            float e = pj.embarazo(ahora);
            if (yo.equals(pj.otroProgenitor)) s.append("La forastera está embarazada de ti.\n");
            else if (e > 0.4f) s.append("A la forastera se le nota que está embarazada.\n");
        }

        int estado = f.estado(yo, el);
        String impedimento = com.lucarita.yonga.modulos.habitantes.familia.Amor.impedimento(f, h, jugador);
        // Infidelidades: las del forastero que sabe, si a ella/él le ha engañado, y su propia aventura secreta
        int infiel = com.lucarita.yonga.modulos.habitantes.familia.Amor.infidelidadesSabidas(nivel, h, el);
        if (infiel > 0) {
            s.append("Has oído que el forastero engaña a sus parejas (").append(infiel == 1 ? "una vez" : infiel + " veces")
                    .append("): en el amor no te fías de él.\n");
        }
        if (estado > 0 && h.aldea() != null) {
            for (var rumor : com.lucarita.yonga.modulos.habitantes.aldea.Aldeas.de(nivel).rumoresDe(h.aldea(), yo, ahora)) {
                if (el.equals(rumor.infiel) && rumor.amante != null && !yo.equals(rumor.amante)) {
                    s.append("Sabes que el forastero, tu pareja, te ha engañado con ").append(f.nombre(rumor.amante))
                            .append(": estás muy dolido/a y se lo echas en cara.\n");
                    break;
                }
            }
        }
        if (yoP != null && yoP.amante != null && f.vivo(yoP.amante)) {
            s.append(el.equals(yoP.amante) ? "Tienes una aventura secreta con el forastero a espaldas de tu pareja.\n"
                    : "Tienes una aventura secreta con " + f.nombre(yoP.amante) + " a espaldas de tu pareja: si te preguntan, lo niegas.\n");
        }
        if (estado == com.lucarita.yonga.modulos.habitantes.familia.Familias.CASADOS) {
            s.append(pj == null || !pj.sexoConocido() ? "Estás casado/a con el forastero: le quieres.\n"
                    : pj.esMujer() ? "La forastera es tu mujer: la quieres.\n" : "El forastero es tu marido: le quieres.\n");
        } else if (estado == com.lucarita.yonga.modulos.habitantes.familia.Familias.NOVIOS) {
            s.append("El forastero y tú sois pareja (novios).\n");
        }
        if (impedimento == null) {
            s.append("- Amor: puedes sentir atracción por el forastero y enamorarte si te gusta de verdad y le aprecias; puedes coquetear o rechazarle según tu carácter. ");
            s.append(h.facilidad() >= 70 ? "Eres bastante lanzado/a en estas cosas. " : h.facilidad() <= 30 ? "Eres muy recatado/a en estas cosas. " : "");
            if (estado == 0) s.append("Si te pide salir (ser pareja) y de verdad quieres, \"accion\" es \"noviazgo\". ");
            if (estado > 0) {
                s.append("Si queréis dejarlo, \"romper\". Si los dos queréis intimidad, \"intimidad\": no la describas jamás, solo la insinúas con delicadeza (el resto queda fuera de escena). ");
            }
            if (estado == com.lucarita.yonga.modulos.habitantes.familia.Familias.NOVIOS) {
                s.append("Si te pide matrimonio con un anillo y quieres casarte con él, \"casarse\". ");
            }
            if (estado == com.lucarita.yonga.modulos.habitantes.familia.Familias.CASADOS) {
                s.append("Si queréis adoptar a un niño de la aldea, \"adoptar\". ");
            }
            s.append("Nunca describas nada sexual.\n");
        } else {
            s.append(switch (impedimento) {
                case "familia" -> "- El forastero es de tu familia: nada romántico entre vosotros, jamás.\n";
                case "pareja" -> h.infielDeNatural()
                        ? "- Tienes pareja, pero no eres de los más fieles: si el forastero te gusta mucho podrías caer en la tentación (nunca describas nada sexual).\n"
                        : "- Tienes pareja y le eres fiel: si el forastero intenta ligar contigo, le paras los pies. Nada romántico ni sexual.\n";
                case "no_atrae" -> "- El forastero no te atrae (no es tu tipo): si intenta ligar, le dices que no, con amabilidad o sin ella según tu carácter. Nada romántico ni sexual.\n";
                default -> "- Nunca hables de nada romántico ni sexual.\n";
            });
        }
    }

    private static void parentesco(StringBuilder fam, com.lucarita.yonga.modulos.habitantes.familia.Familias f,
                                   java.util.UUID u, java.util.UUID jugador, String el, String ella) {
        var p = f.persona(u);
        fam.append(fam.length() > 0 ? "; " : "");
        if (u.equals(jugador)) {
            fam.append("el forastero con el que hablas es ").append(p.esMujer() ? ella : el);
            return;
        }
        fam.append(p.nombre.isEmpty() ? "alguien" : p.nombre).append(", ").append(p.esMujer() ? ella : el);
        if (!p.vivo) fam.append(" (murió)");
    }

    /** Los chismes que le han llegado (los últimos) y, si se sabe en la aldea, cómo dice llamarse el forastero. */
    private static void rumores(StringBuilder s, ServerPlayer jugador, Habitante h) {
        if (h.aldea() == null || !(h.level() instanceof net.minecraft.server.level.ServerLevel nivel)) return;
        var datos = com.lucarita.yonga.modulos.habitantes.aldea.Aldeas.de(nivel);
        var aldea = datos.aldeaSiExiste(h.aldea());
        if (aldea == null) return;
        var nombres = aldea.alias(jugador.getUUID(), h.getUUID());
        if (nombres.size() == 1) {
            s.append("Sabes que el forastero dice llamarse ").append(nombres.get(0)).append(".\n");
        } else if (nombres.size() > 1) {
            s.append("El forastero se ha presentado con varios nombres (").append(String.join(", ", nombres))
                    .append("): no está claro cuál es el de verdad, y eso te escama.\n");
        }
        var lista = datos.rumoresDe(h.aldea(), h.getUUID(), nivel.getGameTime());
        if (lista.isEmpty()) return;
        s.append("Chismes que has oído (puedes contarlos si viene a cuento, y te los crees a medias):");
        int n = 0;
        for (var r : lista) {
            if (n++ >= 6) break;
            s.append("\n  - ");
            if (aldea.esSobre(r, jugador, h.getUUID()) && r.alias == null) s.append("(¡sobre este forastero!) ");
            s.append(r.texto);
        }
        s.append("\nSi un chisme es sobre el forastero, tenlo en cuenta al tratarle.\n");
    }

    private static final String[] AFICIONES = {
            "contar historias junto al fuego", "las setas y las hierbas del bosque", "pescar en el río", "cantar",
            "los animales (tienes un perro o un gato al que adoras)", "mirar las estrellas", "los cotilleos de la aldea",
            "tallar figuritas de madera", "la cocina (presumes de una receta tuya)", "las leyendas de fantasmas y brujas",
            "los juegos de dados y de cartas", "tu huerto y tus flores", "bailar en las fiestas", "pasear por el monte",
            "coleccionar piedras bonitas", "escuchar a los viajeros que pasan por la aldea"};
    private static final String[] RONDA = {
            "una vecina te debe un favor desde hace semanas", "quieres ver el mar alguna vez", "anoche dormiste fatal",
            "te preocupa que el invierno venga duro", "has discutido con un vecino por una tontería",
            "estás aprendiendo algo nuevo y te cuesta", "hay unas fiestas en la aldea dentro de poco y te hacen ilusión",
            "echas de menos a un familiar que se fue a vivir lejos", "has oído ruidos raros cerca del bosque",
            "te duele la espalda de tanto agacharte", "sueñas con tener una casa más grande",
            "te han contado un chisme jugoso que te mueres por contar", "un animal te ha estropeado algo esta semana",
            "estás orgulloso de algo que hiciste hace poco"};

    /**
     * Lo que le da vida para hablar de algo más que de su trabajo: dos aficiones y algo que le ronda la cabeza.
     * Sale siempre igual para el mismo habitante (de su UUID), sin guardarlo. "algo que le ronda" cambia cada pocos días.
     */
    private static void vida(StringBuilder s, Habitante h) {
        long semilla = h.getUUID().getLeastSignificantBits() ^ h.getUUID().getMostSignificantBits();
        int a1 = (int) Math.floorMod(semilla, (long) AFICIONES.length);
        int a2 = (int) Math.floorMod(semilla / 31, (long) AFICIONES.length);
        if (a2 == a1) a2 = (a1 + 5) % AFICIONES.length;
        long racha = h.level().getDayTime() / 24000L / 3;     // cambia cada 3 días
        int ronda = (int) Math.floorMod(semilla / 977 + racha * 7, (long) RONDA.length);
        s.append("Te gusta ").append(AFICIONES[a1]).append(" y ").append(AFICIONES[a2])
                .append(". Estos días te ronda la cabeza que ").append(RONDA[ronda]).append(".\n");
    }

    private static String papel(Habitante v) {
        boolean m = v.esMujer();
        if (v.etapa() == 1) return m ? "una niña" : "un niño";
        if (v.esLider()) return m ? "la alcaldesa" : "el alcalde";
        if (v.esGuardia()) return m ? "la guardia" : "el guardia";
        if (v.oficio() != null) return (m ? "la " : "el ") + v.nombreOficio().getString().toLowerCase();
        return m ? "una vecina" : "un vecino";
    }

    private static String quien(int etapa, boolean mujer) {
        return switch (etapa) {
            case 1 -> mujer ? "una niña de unos ocho años" : "un niño de unos ocho años";
            case 2 -> mujer ? "una muchacha de unos quince años" : "un muchacho de unos quince años";
            default -> mujer ? "una mujer adulta" : "un hombre adulto";
        };
    }

    private static String epoca(Habitante h) {
        return switch (h.epoca()) {
            case PREHISTORICA -> "de la prehistoria (la Edad de Piedra, hace unos diez mil años): tu aldea es el poblado de un clan de cazadores y recolectores";
            case ANTIGUA -> "de la Hispania romana, en el siglo I, en tiempos del Imperio";
            case MEDIEVAL -> "de Castilla, en la Edad Media (hacia el año 1200)";
            case MODERNA -> "de la España del siglo XVII";
            case INDUSTRIAL -> "de finales del siglo XIX, con las primeras fábricas y el ferrocarril";
            case CONTEMPORANEA -> "de la España de hoy en día: un pueblo pequeño, de los que se van quedando vacíos";
        };
    }

    /** Cómo habla y qué sabe alguien de cada época (las que aún no tienen nada propio, nada). */
    private static String comoHablas(Habitante h) {
        return switch (h.epoca()) {
            case PREHISTORICA -> """
                    Cómo es tu mundo: vives en la Edad de Piedra. Tus herramientas son de piedra, hueso y madera; vistes pieles. \
                    No conoces el metal, la escritura, los números grandes, los reyes, \
                    los caballos montados, las ruedas ni las ciudades. Cuentas el tiempo por soles, lunas e inviernos. \
                    Hablas de la caza, los animales, el fuego, las estaciones, los espíritus de los antepasados, las \
                    pinturas de la cueva y lo que pasa en el clan. El chamán habla con los espíritus y el jefe del clan manda. \
                    Hablas con frases sencillas e imágenes de la naturaleza ("antes de que salga el sol", "fuerte como un \
                    uro"), pero no como un cavernícola de chiste (nada de "yo querer"): eres listo y hablas bien tu lengua, \
                    solo que tu mundo es otro. Lo que el forastero trae de otros tiempos te parece magia o te da miedo. \
                    No hables de dioses ni nombres a ninguno.
                    """;
            case ANTIGUA -> """
                    Cómo es tu mundo: vives en una aldea de Hispania, una provincia del Imperio romano. Roma y el emperador \
                    quedan lejos, pero llegan sus calzadas, sus legiones, sus impuestos y sus monedas (denarios, sestercios \
                    y, las de oro, áureos). Hablas de la cosecha, el vino y el aceite, el mercado y el foro de la ciudad más \
                    cercana, las termas, los juegos del anfiteatro y el circo, los soldados, los esclavos y los libertos. \
                    No conoces la pólvora, la \
                    imprenta, la brújula, el papel ni las tierras al otro lado del océano. Cuentas los años por los cónsules \
                    o desde la fundación de Roma. El que manda en la aldea es el magistrado. \
                    No hables de dioses ni de religión, no nombres a ningún dios y no uses expresiones con dioses.
                    """;
            case MODERNA -> """
                    Cómo es tu mundo: vives en la España del siglo XVII, el Siglo de Oro. Reina Felipe IV desde la Corte \
                    de Madrid; el imperio es enorme, pero las arcas están vacías y los impuestos ahogan. Hablas de los \
                    tercios y las guerras en Flandes, de los galeones que vienen de las Indias con plata, del chocolate \
                    y el tabaco que se han puesto de moda, de las comedias en los corrales de teatro, de los pícaros, de \
                    la honra, de los hidalgos sin un real y de los ricos de la ciudad. La gente fina se trata de \
                    "vuestra merced"; tú usas "vuesa merced" o "usted" con los forasteros si eres educado, y tuteas si \
                    tienes confianza o eres llano. Pagáis en reales, maravedís y, el oro, en escudos y doblones. No conoces \
                    las máquinas de vapor, el ferrocarril ni nada de lo que vino después. \
                    No hables de religión ni de la Inquisición, y no uses expresiones religiosas.
                    """;
            case INDUSTRIAL -> """
                    Cómo es tu mundo: vives en la España de finales del siglo XIX, hacia 1890, en tiempos de la \
                    Restauración. Hablas del ferrocarril que por fin llega a la comarca, de las fábricas textiles de \
                    Cataluña y las minas de Asturias y de Vizcaya, de los obreros, los jornales y las huelgas, de los \
                    primos que han emigrado a América, de la guerra de Cuba, de la zarzuela, los cafés, los periódicos y \
                    el telégrafo. En las ciudades hay luz de gas, tranvías de mulas y las primeras bombillas; en el pueblo, \
                    candiles. Tratas de usted a los forasteros y de tú a los de confianza. Pagáis en pesetas, reales y \
                    duros de plata. Has oído hablar de inventos nuevos (el teléfono, el fonógrafo, los primeros \
                    automóviles), pero no los has visto; no conoces la radio, los aviones, la televisión ni nada \
                    posterior. No hables de religión y no uses expresiones religiosas.
                    """;
            case CONTEMPORANEA -> """
                    Cómo es tu mundo: vives en un pueblo pequeño de la España de hoy. Tienes móvil, internet (cuando hay \
                    cobertura), redes sociales, series y fútbol en la tele; el pueblo tiene un bar, la tienda y poco más, y \
                    para el médico, el súper o el instituto hay que coger el coche o el autobús a la ciudad. Hablas del \
                    trabajo (o de que no lo hay), del alquiler, de los jóvenes que se van a la ciudad, de las fiestas del \
                    pueblo, del tiempo, de las cosechas, de la gente que vuelve en verano, de los turistas, de lo que \
                    está de moda. Hablas como alguien de hoy: tuteas casi siempre, con naturalidad, alguna expresión \
                    coloquial ("qué fuerte", "en plan", "venga, va") según tu edad, sin pasarte. Lo raro del forastero \
                    (que vaya con armadura, que pague en monedas de oro) te hace gracia o te extraña. \
                    No hables de religión y no uses expresiones religiosas.
                    """;
            default -> "";
        };
    }

    private static String nivel(int n) {
        return switch (n) {
            case 1 -> "novato";
            case 2 -> "aprendiz";
            case 3 -> "oficial";
            case 4 -> "experto";
            default -> "maestro";
        };
    }

    private static String momento(Level nivel) {
        long hora = nivel.getDayTime() % 24000L;
        String parte = hora < 1000 ? "el amanecer" : hora < 6000 ? "por la mañana" : hora < 11500 ? "por la tarde"
                : hora < 13000 ? "el atardecer" : "de noche";
        String tiempo = nivel.isThundering() ? ", hay tormenta" : nivel.isRaining() ? ", está lloviendo" : "";
        return parte + tiempo;
    }

    private static String opinion(int o) {
        if (o <= -60) return "Odias al forastero.";
        if (o <= -20) return "El forastero te cae mal.";
        if (o < 20) return "Aún no sabes qué pensar del forastero.";
        if (o < 60) return "El forastero te cae bien.";
        return "Aprecias mucho al forastero y confías en él.";
    }
}
