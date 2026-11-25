package com.lucarita.yonga.modulos.habitantes;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * config/yonga-habitantes.toml (común: la usa el servidor).
 * El crecimiento se mide con el reloj del servidor (gameTime): dormir o /time no lo aceleran,
 * y con el servidor apagado no avanza.
 */
public final class HabitantesConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue DIAS_BEBE;
    public static final ForgeConfigSpec.DoubleValue DIAS_NINO;
    public static final ForgeConfigSpec.DoubleValue DIAS_JOVEN;
    public static final ForgeConfigSpec.DoubleValue DIAS_EMBARAZO;
    public static final ForgeConfigSpec.DoubleValue PROB_EMBARAZO;
    public static final ForgeConfigSpec.BooleanValue SUSTITUIR_ALDEANOS;
    public static final ForgeConfigSpec.BooleanValue ROMANCE;
    public static final ForgeConfigSpec.BooleanValue QUITAR_COMERCIANTE;
    public static final ForgeConfigSpec.BooleanValue GUARDIAS_POR_GOLEMS;
    public static final ForgeConfigSpec.BooleanValue TABERNAS;


    public static final ForgeConfigSpec.BooleanValue IA_ACTIVADA;
    public static final ForgeConfigSpec.ConfigValue<String> IA_PROVEEDOR;
    public static final ForgeConfigSpec.ConfigValue<String> ONLINE_URL;
    public static final ForgeConfigSpec.ConfigValue<String> ONLINE_CLAVE;
    public static final ForgeConfigSpec.ConfigValue<String> ONLINE_MODELO;
    public static final ForgeConfigSpec.ConfigValue<String> IA_URL;
    public static final ForgeConfigSpec.ConfigValue<String> IA_MODELO;
    public static final ForgeConfigSpec.BooleanValue IA_SIN_PENSAR;
    public static final ForgeConfigSpec.DoubleValue IA_TEMPERATURA;
    public static final ForgeConfigSpec.IntValue IA_ESPERA;
    public static final ForgeConfigSpec.IntValue IA_LIMITE;
    public static final ForgeConfigSpec.IntValue IA_CONTEXTO;
    public static final ForgeConfigSpec.IntValue IA_FRASES;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Cuanto dura cada etapa, en dias reales de servidor encendido (1 dia = 24 horas = 1.728.000 ticks).")
                .push("crecimiento");
        DIAS_BEBE = b.comment("De bebe a nino").defineInRange("bebe", 1.0, 0.01, 365.0);
        DIAS_NINO = b.comment("De nino a joven").defineInRange("nino", 2.5, 0.01, 365.0);
        DIAS_JOVEN = b.comment("De joven a adulto").defineInRange("joven", 5.0, 0.01, 365.0);
        DIAS_EMBARAZO = b.comment("Lo que dura un embarazo (habitantes y jugadoras), en dias reales de servidor encendido.",
                        "0.0139 = un dia de Minecraft (20 minutos); 1.0 = un dia real; 0.0035 = unos 5 minutos (para probar)")
                .defineInRange("embarazo", 0.0139, 0.001, 30.0);
        PROB_EMBARAZO = b.comment("Probabilidad de quedarse embarazada en cada noche juntos (hombre y mujer). 1.0 = siempre")
                .defineInRange("probabilidad_embarazo", 0.9, 0.0, 1.0);
        b.pop();

        b.push("mundo");
        SUSTITUIR_ALDEANOS = b.comment("Los aldeanos de Minecraft se convierten en habitantes al aparecer")
                .define("sustituirAldeanos", true);
        QUITAR_COMERCIANTE = b.comment("Quitar el comerciante errante (y sus llamas)").define("quitarComercianteErrante", true);
        TABERNAS = b.comment("Construir una taberna (con tabernero y tablón de encargos) junto a cada aldea")
                .define("generarTabernas", true);
        GUARDIAS_POR_GOLEMS = b.comment("Los golems de hierro de las aldeas se cambian por guardias (los que hacen los jugadores no)")
                .define("guardiasPorGolems", true);
        ROMANCE = b.comment("Romance, matrimonio e hijos con habitantes (fase 5). Desactivalo para quitarlo todo.")
                .define("romance", true);
        b.pop();

        b.comment("Conversacion con IA local (Ollama: https://ollama.com). Instala Ollama en el PC del servidor y",
                "descarga el modelo con: ollama pull qwen3:4b   (rapido; si te sobra memoria de video: qwen3:8b, mas listo)",
                "Para ver si cabe entero en la grafica: 'ollama ps' tiene que decir 100% GPU (si sale CPU, va muy lento).",
                "Si la IA no responde, los habitantes contestan con frases sencillas.").push("ia");
        IA_ACTIVADA = b.comment("Usar la IA para las conversaciones").define("activada", true);
        IA_PROVEEDOR = b.comment("De donde sale la IA: \"ollama\" (en tu PC, ver abajo) u \"online\" (una API en internet",
                "compatible con OpenAI: Groq, Gemini, DeepSeek, OpenRouter...; ver la seccion [ia.online])").define("proveedor", "ollama");
        IA_URL = b.comment("Direccion de Ollama").define("url", "http://127.0.0.1:11434");
        IA_MODELO = b.comment("Modelo de Ollama").define("modelo", "qwen3:4b");
        IA_SIN_PENSAR = b.comment("Pedir al modelo que no 'piense' antes de responder (mucho mas rapido en qwen3)")
                .define("sinPensar", true);
        IA_TEMPERATURA = b.comment("Creatividad de las respuestas (0 = siempre igual, 1,5 = muy loco)")
                .defineInRange("temperatura", 0.8, 0.0, 1.5);
        IA_ESPERA = b.comment("Segundos maximos esperando una respuesta").defineInRange("espera", 60, 5, 600);
        IA_LIMITE = b.comment("Si el proveedor online dice que se ha llegado al limite de mensajes, el habitante espera",
                        "en silencio (como si pensara) y se reintenta solo; esto es lo maximo que espera, en segundos")
                .defineInRange("espera_limite", 120, 0, 600);
        IA_CONTEXTO = b.comment("Contexto del modelo en tokens (lo que 'cabe' en su cabeza a la vez). Mas = mas memoria de video.",
                "8192 basta para conversaciones largas; si la grafica va justa, 4096. qwen3 admite hasta 32768.",
                "Si la conversacion no cabe, se le mandan solo las frases mas recientes que quepan (lo demas, en sus notas).").defineInRange("contexto", 8192, 2048, 32768);
        IA_FRASES = b.comment("Frases que recuerda literalmente de cada jugador (de los dos; el doble con quien aprecia mucho).",
                "Lo anterior lo resume en sus notas.")
                .defineInRange("frasesRecordadas", 40, 4, 200);
        b.comment("IA online (proveedor = \"online\"). Por defecto Groq, que tiene un plan gratuito sin tarjeta:",
                "  1. Registrate en https://console.groq.com  2. API Keys > Create API Key  3. Pega la clave aqui.",
                "Otros: Gemini  url = \"https://generativelanguage.googleapis.com/v1beta/openai\"  modelo = \"gemini-3.1-flash-lite\"",
                "       DeepSeek url = \"https://api.deepseek.com\"  modelo = \"deepseek-chat\"",
                "La clave es tuya: no la compartas (ni este archivo con ella dentro).").push("online");
        ONLINE_URL = b.comment("Direccion base de la API (sin /chat/completions)").define("url", "https://api.groq.com/openai/v1");
        ONLINE_CLAVE = b.comment("Tu clave de la API").define("clave", "");
        ONLINE_MODELO = b.comment("Modelo. En Groq mira los disponibles en https://console.groq.com/docs/models",
                "(en Groq gratis: \"openai/gpt-oss-120b\" o, mas rapido y simple, \"openai/gpt-oss-20b\")")
                .define("modelo", "openai/gpt-oss-120b");
        b.pop();
        b.pop();

        SPEC = b.build();
    }

    private HabitantesConfig() {}

    public static final long TICKS_DIA = 24L * 60 * 60 * 20;

    public static long ticksBebe() {
        return (long) (DIAS_BEBE.get() * TICKS_DIA);
    }

    public static long ticksNino() {
        return (long) (DIAS_NINO.get() * TICKS_DIA);
    }

    public static long ticksJoven() {
        return (long) (DIAS_JOVEN.get() * TICKS_DIA);
    }
}
