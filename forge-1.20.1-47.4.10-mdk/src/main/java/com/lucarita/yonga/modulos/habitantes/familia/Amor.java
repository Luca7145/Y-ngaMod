package com.lucarita.yonga.modulos.habitantes.familia;

import com.lucarita.yonga.modulos.habitantes.Epoca;
import com.lucarita.yonga.modulos.habitantes.Habitante;
import com.lucarita.yonga.modulos.habitantes.HabitantesConfig;
import com.lucarita.yonga.modulos.habitantes.ModuloHabitantes;
import com.lucarita.yonga.modulos.habitantes.aldea.Aldeas;
import com.lucarita.yonga.modulos.habitantes.ia.Conversaciones;
import com.lucarita.yonga.modulos.habitantes.ia.Recuerdo;
import com.lucarita.yonga.modulos.habitantes.red.BarrigaPacket;
import com.lucarita.yonga.modulos.habitantes.red.FundidoPacket;
import com.lucarita.yonga.modulos.habitantes.red.PedirSexoPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.lucarita.yonga.modulos.habitantes.red.HabitantesRed;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * FASE 5: amor y familia.
 *
 * ENTRE HABITANTES: los adultos solteros que charlan a menudo en la plaza cogen afinidad; si se atraen (cada uno tiene
 * su orientación) y no son parientes, se hacen novios y, unos días después, se casan. Los matrimonios de hombre y
 * mujer tienen hijos (si hay camas libres en la aldea, hasta 4); los del mismo sexo adoptan a un niño de la aldea sin
 * familia. Los bebés se parecen a sus padres.
 *
 * CON EL JUGADOR (solo adultos, nunca parientes, solo si le atrae): todo se decide hablando con la IA.
 *   · noviazgo → si le aprecia lo bastante, acepta salir con él;
 *   · intimidad → solo con su pareja: se queda en ropa interior y la pantalla funde a negro (nunca se ve nada más).
 *     Si son de distinto sexo, puede haber embarazo; el bebé nace en un día;
 *   · casarse → hace falta el ANILLO (clic derecho con él en la mano: le pide matrimonio);
 *   · adoptar → casados, a un niño de la aldea sin familia;
 *   · romper.
 * CELOS: si el jugador tiene pareja y se lía con otro, alguien lo ve y corre el rumor; cuando le llega a su pareja,
 * se enfada mucho y puede dejarle.
 *
 * Todo se apaga con romance = false en la configuración.
 */
public final class Amor {

    private Amor() {}

    public static final long DIA = 24000L;
    private static final int AFINIDAD_NOVIOS = 6;

    public static boolean activo() {
        return HabitantesConfig.ROMANCE.get();
    }

    // =====================================================================================================
    // ¿PUEDE HABER ALGO?
    // =====================================================================================================

    /** Por qué no puede haber romance entre ese habitante y el jugador, o null si puede. */
    @Nullable
    public static String impedimento(Familias f, Habitante h, ServerPlayer j) {
        if (!activo()) return "desactivado";
        if (h.etapa() < 3) return "menor";
        if (f.parientes(h.getUUID(), j.getUUID())) return "familia";
        Familias.Persona pj = f.persona(j.getUUID());
        if (!h.leAtrae(pj.esMujer(), pj.sexoConocido())) return "no_atrae";
        UUID pareja = f.pareja(h.getUUID());
        if (pareja != null && !pareja.equals(j.getUUID())) return "pareja";
        return null;
    }

    // =====================================================================================================
    // ENTRE HABITANTES
    // =====================================================================================================

    /** Dos vecinos charlan en la plaza: de ahí salen las parejas y las bodas. */
    public static void charlan(Habitante a, Habitante b) {
        if (!activo() || a.etapa() < 3 || b.etapa() < 3 || !(a.level() instanceof ServerLevel nivel)) return;
        Familias f = Familias.de(nivel.getServer());
        if (f.parientes(a.getUUID(), b.getUUID())) return;
        long ahora = nivel.getGameTime();
        int estado = f.estado(a.getUUID(), b.getUUID());
        if (estado == Familias.NOVIOS) {
            Familias.Relacion r = f.relacion(a.getUUID(), b.getUUID());
            if (r != null && ahora - r.desde > 3 * DIA && nivel.random.nextInt(3) == 0) boda(nivel, f, a, b);
            return;
        }
        if (estado != 0 || f.pareja(a.getUUID()) != null || f.pareja(b.getUUID()) != null) return;
        if (!a.leAtrae(b.esMujer(), true) || !b.leAtrae(a.esMujer(), true)) return;
        int v = f.sumarAfinidad(a.getUUID(), b.getUUID(), 1 + (a.tieneRasgo(Habitante.CHARLATAN) ? 1 : 0));
        if (v >= AFINIDAD_NOVIOS) {
            f.emparejar(a.getUUID(), b.getUUID(), Familias.NOVIOS, ahora);
            com.lucarita.yonga.modulos.habitantes.aldea.Chismes.contar(nivel, a, a.nombre(), null,
                    a.nombre() + " y " + b.nombre() + " salen juntos", false);
            f.olvidarAfinidad(a.getUUID(), b.getUUID());
            a.decirEnAlto(Component.translatable("yonga.amor.declara." + nivel.random.nextInt(3)));
            b.decirEnAlto(Component.translatable("yonga.amor.acepta"));
            corazones(nivel, a);
            corazones(nivel, b);
        }
    }

    private static void boda(ServerLevel nivel, Familias f, Habitante a, Habitante b) {
        f.emparejar(a.getUUID(), b.getUUID(), Familias.CASADOS, nivel.getGameTime());
        a.decirEnAlto(Component.translatable("yonga.amor.boda"));
        corazones(nivel, a);
        corazones(nivel, b);
        if (a.aldea() != null) {
            Aldeas.de(nivel).nuevoRumor(a.aldea(), a.nombre(), null,
                    a.nombre() + " y " + b.nombre() + " se han casado", a.getUUID(), nivel.getGameTime());
        }
    }

    /** Con su pareja cerca, la prefiere para charlar en la plaza (la mitad de las veces). */
    @Nullable
    public static Habitante parejaEntre(Habitante h, List<Habitante> cerca) {
        if (!activo() || !(h.level() instanceof ServerLevel nivel) || h.getRandom().nextBoolean()) return null;
        UUID pareja = Familias.de(nivel.getServer()).pareja(h.getUUID());
        if (pareja == null) return null;
        for (Habitante o : cerca) if (o.getUUID().equals(pareja)) return o;
        return null;
    }

    /**
     * Cada 10 segundos por habitante: lo apunta en el árbol, y si toca, nace su bebé, se queda embarazada, adoptan
     * o se entera de unos cuernos.
     */
    public static void revisar(ServerLevel nivel, Habitante h) {
        Familias f = Familias.de(nivel.getServer());
        f.apuntar(h.getUUID(), h.nombre(), h.esMujer(), false);
        if (!activo()) return;
        Familias.Persona p = f.persona(h.getUUID());
        long ahora = nivel.getGameTime();
        if (p.parto > 0 && ahora >= p.parto) {
            UUID otro = p.otroProgenitor;
            p.parto = 0;
            p.concepcion = 0;
            p.otroProgenitor = null;
            p.rumorEmbarazo = 0;
            f.marcar();
            Habitante bebe = nacer(nivel, h.blockPosition(), h.epoca(), h.getUUID(), otro);
            if (bebe != null) rumorParto(nivel, f, h.blockPosition(), h.nombre(), null, bebe);
        }
        // La barriga crece con el embarazo; al final pesa y va más despacio
        float barriga = h.esMujer() ? p.embarazo(ahora) : 0.0f;
        h.ponerBarriga(barriga);
        rumoresEmbarazo(nivel, f, h, p, barriga);
        if (barriga > 0.66f) {
            h.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 260, 0, false, false));
        }
        UUID pareja = f.pareja(h.getUUID());
        if (pareja == null) return;
        Familias.Persona pp = f.personaSiExiste(pareja);
        celos(nivel, f, h, p, pareja);
        if (f.pareja(h.getUUID()) == null) return;                  // le ha dejado
        quizaAventura(nivel, f, h, p, pareja, pp);
        if (pp != null && pp.jugador) return;
        if (f.estado(h.getUUID(), pareja) != Familias.CASADOS || h.getUUID().compareTo(pareja) > 0) return;
        if (!(nivel.getEntity(pareja) instanceof Habitante otro) || otro.distanceToSqr(h) > 64 * 64) return;
        int hijos = f.hijos(h.getUUID()).size();
        if (h.esMujer() != otro.esMujer()) {
            Habitante madre = h.esMujer() ? h : otro;
            Familias.Persona pm = f.persona(madre.getUUID());
            if (pm.parto == 0 && hijos < 4 && nivel.random.nextInt(240) == 0 && hayCamaLibre(nivel, madre)) {
                embarazar(f, pm, (madre == h ? otro : h).getUUID(), ahora);
                madre.decirEnAlto(Component.translatable("yonga.amor.embarazo"));
            }
        } else if (hijos < 2 && nivel.random.nextInt(600) == 0) {
            Habitante nino = huerfano(nivel, f, h);
            if (nino != null) {
                f.ponerPadres(nino.getUUID(), List.of(h.getUUID(), otro.getUUID()));
                h.decirEnAlto(Component.translatable("yonga.amor.adopta"));
                corazones(nivel, nino);
            }
        }
    }

    private static boolean hayCamaLibre(ServerLevel nivel, Habitante h) {
        BlockPos centro = h.aldea() != null ? h.aldea() : h.blockPosition();
        return nivel.getPoiManager().getCountInRange(t -> t.is(PoiTypes.HOME), centro, 64, PoiManager.Occupancy.HAS_SPACE) > 0;
    }

    /** Un niño de la aldea (bebé o crío) sin padres conocidos, el más cercano. */
    @Nullable
    private static Habitante huerfano(ServerLevel nivel, Familias f, Habitante adulto) {
        BlockPos centro = adulto.aldea() != null ? adulto.aldea() : adulto.blockPosition();
        List<Habitante> ninos = nivel.getEntitiesOfClass(Habitante.class, new AABB(centro).inflate(Aldeas.RADIO),
                n -> n.isAlive() && n.etapa() <= 1 && f.padres(n.getUUID()).isEmpty());
        ninos.sort(Comparator.comparingDouble(n -> n.distanceToSqr(adulto)));
        return ninos.isEmpty() ? null : ninos.get(0);
    }

    /** Nace un bebé junto a su madre (o a quien lo haya tenido), parecido a sus padres. */
    @Nullable
    public static Habitante nacer(ServerLevel nivel, BlockPos donde, Epoca epoca, UUID progenitor1, @Nullable UUID progenitor2) {
        Habitante bebe = ModuloHabitantes.HABITANTE.get().create(nivel);
        if (bebe == null) return null;
        bebe.moveTo(donde.getX() + 0.5, donde.getY(), donde.getZ() + 0.5, nivel.random.nextFloat() * 360.0f, 0.0f);
        bebe.generar(epoca, 0, nivel.random);
        Habitante a = nivel.getEntity(progenitor1) instanceof Habitante x ? x : null;
        Habitante b = progenitor2 != null && nivel.getEntity(progenitor2) instanceof Habitante y ? y : null;
        bebe.heredar(a, b);
        bebe.finalizeSpawn(nivel, nivel.getCurrentDifficultyAt(donde), MobSpawnType.BREEDING, null, null);
        nivel.addFreshEntity(bebe);
        Familias f = Familias.de(nivel.getServer());
        f.apuntar(bebe.getUUID(), bebe.nombre(), bebe.esMujer(), false);
        List<UUID> padres = new ArrayList<>();
        padres.add(progenitor1);
        if (progenitor2 != null) padres.add(progenitor2);
        f.ponerPadres(bebe.getUUID(), padres);
        corazones(nivel, bebe);
        // Recién nacido, va siempre detrás de su madre (hasta que alguien le diga otra cosa)
        bebe.seguirPorDefecto(progenitor1);
        // Le pone nombre un padre jugador: si lo son los dos, el que no lo ha llevado en la barriga
        boolean madreJugadora = nivel.getServer().getPlayerList().getPlayer(progenitor1) != null;
        boolean otroJugador = progenitor2 != null && nivel.getServer().getPlayerList().getPlayer(progenitor2) != null;
        UUID nombra = otroJugador ? progenitor2 : madreJugadora ? progenitor1 : null;
        if (nombra != null) POR_NOMBRAR.put(bebe.getUUID(), nombra);
        // Si uno de los padres es un jugador: el bebé le quiere y él sabe cómo se llama
        for (UUID u : padres) {
            ServerPlayer jugador = nivel.getServer().getPlayerList().getPlayer(u);
            if (jugador != null) {
                Recuerdo r = bebe.recuerdo(jugador);
                r.opinion = 80;
                r.sabeNombre = true;
                r.notas = jugador.getGameProfile().getName() + " es " + (f.persona(u).esMujer() ? "mi madre." : "mi padre.");
                if (u.equals(nombra)) {
                    HabitantesRed.CANAL.send(PacketDistributor.PLAYER.with(() -> jugador),
                            new com.lucarita.yonga.modulos.habitantes.red.NombrarBebePacket(bebe.getId(), bebe.esMujer(), bebe.nombre()));
                } else if (nombra == null) {
                    jugador.sendSystemMessage(Component.translatable(bebe.esMujer() ? "yonga.amor.nace.hija" : "yonga.amor.nace.hijo",
                            bebe.nombre()));
                }
            }
        }
        return bebe;
    }

    /** Bebés recién nacidos que esperan a que un padre jugador les ponga nombre: bebé -> quién se lo pone. */
    private static final java.util.Map<UUID, UUID> POR_NOMBRAR = new java.util.HashMap<>();

    /** El padre o la madre que tocaba ha elegido el nombre del bebé. */
    public static void nombrarBebe(ServerPlayer j, int idBebe, String nombre) {
        if (!(j.serverLevel().getEntity(idBebe) instanceof Habitante bebe)) return;
        if (!j.getUUID().equals(POR_NOMBRAR.get(bebe.getUUID()))) return;
        POR_NOMBRAR.remove(bebe.getUUID());
        String limpio = nombre.replaceAll("§.", "").replaceAll("[\\p{Cntrl}]", "").strip();
        if (limpio.length() > 32) limpio = limpio.substring(0, 32);
        if (!limpio.isEmpty()) bebe.ponerNombre(limpio);
        Familias f = Familias.de(j.getServer());
        f.apuntar(bebe.getUUID(), bebe.nombre(), bebe.esMujer(), false);
        for (UUID u : f.padres(bebe.getUUID())) {
            ServerPlayer padre = j.getServer().getPlayerList().getPlayer(u);
            if (padre != null) {
                padre.sendSystemMessage(Component.translatable(bebe.esMujer() ? "yonga.amor.nace.hija" : "yonga.amor.nace.hijo",
                        bebe.nombre()));
            }
        }
    }

    // =====================================================================================================
    // CON EL JUGADOR
    // =====================================================================================================

    /** Lo que la IA ha decidido (noviazgo, romper, intimidad, casarse, adoptar), con los límites del servidor. */
    public static void accion(ServerPlayer j, Habitante h, Recuerdo r, String accion) {
        if (!activo() || !(h.level() instanceof ServerLevel nivel)) return;
        Familias f = Familias.de(nivel.getServer());
        f.apuntar(j.getUUID(), j.getGameProfile().getName(), f.persona(j.getUUID()).mujer, true);
        UUID yo = h.getUUID(), el = j.getUUID();
        int estado = f.estado(yo, el);
        long ahora = nivel.getGameTime();
        switch (accion) {
            case "noviazgo" -> {
                if (estado != 0 || impedimento(f, h, j) != null) return;
                if (r.opinion < (h.tieneRasgo(Habitante.DESCONFIADO) ? 65 : 50)) return;
                boolean infiel = !f.parejas(el).isEmpty();
                f.emparejar(yo, el, Familias.NOVIOS, ahora);
                corazones(nivel, h);
                Conversaciones.aviso(j, h, Component.translatable("yonga.amor.novios", h.nombrePara(j)));
                if (infiel) cuernos(nivel, f, j, h);
            }
            case "romper" -> {
                if (estado == 0) return;
                f.romper(yo, el);
                r.cambiarOpinion(-20);
                Conversaciones.aviso(j, h, Component.translatable("yonga.amor.ruptura", h.nombrePara(j)));
            }
            case "intimidad" -> {
                if (estado == 0 || impedimento(f, h, j) != null || r.opinion < Math.max(5, 25 + (50 - h.facilidad()) / 2)) return;
                intimidad(nivel, f, j, h, r);
            }
            case "casarse" -> {
                if (estado != Familias.NOVIOS || r.opinion < 70) return;
                for (UUID otra : f.parejas(el)) if (!otra.equals(yo) && f.estado(el, otra) == Familias.CASADOS) return;
                if (!quitarAnillo(j)) return;
                f.emparejar(yo, el, Familias.CASADOS, ahora);
                corazones(nivel, h);
                Conversaciones.aviso(j, h, Component.translatable("yonga.amor.boda.jugador", h.nombrePara(j)));
                if (h.aldea() != null) {
                    Aldeas.de(nivel).nuevoRumor(h.aldea(), nombreDe(nivel, j), el,
                            h.nombre() + " se ha casado con " + nombreDe(nivel, j), yo, ahora);
                }
                if (f.parejas(el).size() > 1) cuernos(nivel, f, j, h);
            }
            case "adoptar" -> {
                if (estado != Familias.CASADOS) return;
                Habitante nino = huerfano(nivel, f, h);
                if (nino == null) {
                    Conversaciones.aviso(j, h, Component.translatable("yonga.amor.sin_huerfanos"));
                    return;
                }
                f.ponerPadres(nino.getUUID(), List.of(yo, el));
                Recuerdo rn = nino.recuerdo(j);
                rn.cambiarOpinion(50);
                rn.sabeNombre = true;
                corazones(nivel, nino);
                Conversaciones.aviso(j, h, Component.translatable("yonga.amor.adopcion", nino.nombre()));
            }
            default -> { }
        }
    }

    private static boolean quitarAnillo(ServerPlayer j) {
        if (j.getAbilities().instabuild) return true;
        var inv = j.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModuloHabitantes.ANILLO.get())) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    /**
     * Intimidad: se queda en ropa interior un momento y la pantalla del jugador funde a negro. Nada más se ve.
     * Si son de distinto sexo, puede quedar embarazada (ella, sea el habitante o la jugadora).
     */
    /** Tiempo de espera entre noches juntos: un día de juego (de cada habitante; al jugador no le limita). */
    public static final long ESPERA_NOCHE = DIA;
    /** Probabilidad de embarazo en cada noche (si son de distinto sexo). */

    public static boolean cansado(Familias f, UUID quien, long ahora) {
        return ahora - f.persona(quien).ultimaNoche < ESPERA_NOCHE;
    }

    /** Intimidad desde la IA: igual que el botón, pero sin respuesta (la IA ya ha contestado). */
    private static void intimidad(ServerLevel nivel, Familias f, ServerPlayer j, Habitante h, Recuerdo r) {
        if (Encuentro.ocupado(j.getUUID()) || Encuentro.ocupado(h.getUUID())) return;
        if (cansado(f, h.getUUID(), nivel.getGameTime())) return;
        BlockPos cama = Encuentro.camaCerca(nivel, h.blockPosition());
        if (cama == null) return;
        Encuentro.conHabitante(nivel, j, h, cama);
    }

    /**
     * Después de la noche: tiempo de espera para los dos, el habitante te quiere un poco más, si había otra pareja
     * corre el rumor y, si son de distinto sexo, lo más probable es que ella se quede embarazada.
     */
    public static void despues(ServerLevel nivel, UUID a, UUID b) {
        Familias f = Familias.de(nivel.getServer());
        long ahora = nivel.getGameTime();
        Familias.Persona pa = f.persona(a), pb = f.persona(b);
        // El tiempo de espera es de cada habitante (el jugador puede estar con otro); entre dos jugadores, de la pareja
        if (!pa.jugador) pa.ultimaNoche = ahora;
        if (!pb.jugador) pb.ultimaNoche = ahora;
        if (pa.jugador && pb.jugador) Encuentro.apuntarPareja(a, b, ahora);
        Entity ea = nivel.getEntity(a), eb = nivel.getEntity(b);
        // Corazones al levantarse, encima de los dos
        for (Entity e : new Entity[]{ea, eb}) {
            if (e == null) continue;
            corazones(nivel, e);
            nivel.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 8, 0.6, 0.5, 0.6, 0.0);
        }
        for (Entity[] par : new Entity[][]{{ea, eb}, {eb, ea}}) {
            if (par[0] instanceof Habitante h && par[1] instanceof ServerPlayer j) {
                h.recuerdo(j).cambiarOpinion(5);
                boolean otra = false;
                for (UUID u : f.parejas(j.getUUID())) if (!u.equals(h.getUUID())) otra = true;
                if (otra) cuernos(nivel, f, j, h);
                // Y si el habitante tenía pareja (y no eras tú), es él quien engaña
                UUID suya = f.pareja(h.getUUID());
                if (suya != null && !suya.equals(j.getUUID())) {
                    f.persona(h.getUUID()).amante = j.getUUID();
                    f.marcar();
                    cuernosHabitante(nivel, h, j.getUUID(), nombreDe(nivel, j), j.getUUID());
                }
            }
        }
        if (!pa.sexoConocido() || !pb.sexoConocido() || pa.esMujer() == pb.esMujer()) return;
        if (nivel.random.nextDouble() >= HabitantesConfig.PROB_EMBARAZO.get()) return;
        Familias.Persona madre = pa.esMujer() ? pa : pb;
        UUID otro = pa.esMujer() ? b : a;
        if (madre.parto != 0) return;
        embarazar(f, madre, otro, ahora);
        // El padre jugador se entera (ella se lo dice al levantarse): si no, no había forma de saberlo hasta la barriga
        if (!madre.jugador && f.persona(otro).jugador) {
            ServerPlayer padre = nivel.getServer().getPlayerList().getPlayer(otro);
            if (padre != null) padre.sendSystemMessage(Component.translatable("yonga.amor.la_has_dejado", madre.nombre));
        }
        if (madre.jugador) {
            UUID idMadre = madre == pa ? a : b;
            ServerPlayer jm = nivel.getServer().getPlayerList().getPlayer(idMadre);
            if (jm != null) jm.sendSystemMessage(Component.translatable("yonga.amor.embarazada"));
        }
    }

    /**
     * El jugador tiene pareja y se ha liado con otro: si algún vecino lo ve, empieza a correr el rumor (si nadie lo
     * ve, la mitad de las veces queda en secreto… salvo para el amante). Además crece su fama de infiel en la aldea.
     * Si el rumor llega a su pareja, habrá celos (ver celos()).
     */
    private static void cuernos(ServerLevel nivel, Familias f, ServerPlayer j, Habitante amante) {
        if (amante.aldea() == null) return;
        Habitante testigo = testigo(nivel, amante);
        if (testigo == null && nivel.random.nextBoolean()) return;
        Aldeas datos = Aldeas.de(nivel);
        datos.nuevoRumor(amante.aldea(), nombreDe(nivel, j), j.getUUID(),
                nombreDe(nivel, j) + " anda con " + amante.nombre() + " a escondidas, aunque tiene pareja",
                (testigo == null ? amante : testigo).getUUID(), nivel.getGameTime(), amante.getUUID(), j.getUUID());
        datos.sumarInfidelidad(amante.aldea(), j.getUUID());
    }

    /** Un habitante engaña a su pareja (con otro vecino o con un jugador): rumor si alguien lo ve. */
    private static void cuernosHabitante(ServerLevel nivel, Habitante infiel, UUID amante, String nombreAmante,
                                         @Nullable UUID jugadorImplicado) {
        if (infiel.aldea() == null) return;
        Habitante testigo = testigo(nivel, infiel);
        if (testigo == null && nivel.random.nextInt(3) != 0) return;
        Aldeas.de(nivel).nuevoRumor(infiel.aldea(), infiel.nombre(), jugadorImplicado,
                infiel.nombre() + " engaña a su pareja con " + nombreAmante,
                (testigo == null ? infiel : testigo).getUUID(), nivel.getGameTime(), amante, infiel.getUUID());
    }

    /** Alguien de la aldea que estaba cerca y lo ha visto (o null). */
    @Nullable
    private static Habitante testigo(ServerLevel nivel, Habitante cerca) {
        List<Habitante> testigos = nivel.getEntitiesOfClass(Habitante.class, cerca.getBoundingBox().inflate(24),
                t -> t != cerca && t.etapa() >= 1 && !t.isSleeping() && cerca.aldea().equals(t.aldea()));
        return testigos.isEmpty() ? null : testigos.get(nivel.random.nextInt(testigos.size()));
    }

    /** Cuántas veces le consta a este habitante que el jugador ha engañado a sus parejas (rumores y fama). */
    public static int infidelidadesSabidas(ServerLevel nivel, Habitante h, UUID jugador) {
        if (h.aldea() == null) return 0;
        Aldeas datos = Aldeas.de(nivel);
        int n = 0;
        for (Aldeas.Rumor r : datos.rumoresDe(h.aldea(), h.getUUID(), nivel.getGameTime())) {
            if (jugador.equals(r.infiel)) n++;
        }
        Aldeas.Aldea a = datos.aldeaSiExiste(h.aldea());
        int fama = a == null ? 0 : a.infidelidades(jugador);
        return Math.max(n, fama >= 2 ? fama : 0);           // a partir de dos, lo sabe todo el pueblo
    }

    /** Lo que le cuesta de más fiarse en el amor de alguien con fama de infiel. */
    private static int penalizacion(ServerLevel nivel, Habitante h, UUID jugador) {
        return 15 * Math.min(3, infidelidadesSabidas(nivel, h, jugador));
    }

    /**
     * Su pareja le engaña y el rumor le ha llegado. Si la pareja es un jugador: se enfada mucho, te lo echará en cara
     * la próxima vez que habléis y, si ya le caías mal, te deja. Si es otro vecino: lo más probable es que le deje.
     */
    private static void celos(ServerLevel nivel, Familias f, Habitante h, Familias.Persona p, UUID pareja) {
        if (h.aldea() == null) return;
        for (Aldeas.Rumor r : Aldeas.de(nivel).rumoresDe(h.aldea(), h.getUUID(), nivel.getGameTime())) {
            if (r.amante == null || !pareja.equals(r.infiel) || r.amante.equals(h.getUUID()) || r.cuando <= p.celosVistos) {
                continue;
            }
            p.celosVistos = r.cuando;
            f.marcar();
            String conQuien = f.nombre(r.amante).isEmpty() ? "otra persona" : f.nombre(r.amante);
            Familias.Persona pp = f.personaSiExiste(pareja);
            if (pp != null && pp.jugador) {
                Recuerdo rec = h.recuerdoSiExiste(pareja);
                if (rec == null) continue;
                rec.cambiarOpinion(-40);
                rec.enfadadoHasta = nivel.getGameTime() + DIA / 2;
                rec.reproche = conQuien;
                rec.notas = (rec.notas + " Me he enterado de que me engaña con " + conQuien + ".").strip();
                h.decirEnAlto(Component.translatable("yonga.amor.celos." + nivel.random.nextInt(3)));
                if (rec.opinion < 30) {
                    f.romper(h.getUUID(), pareja);
                    h.decirEnAlto(Component.translatable("yonga.amor.deja"));
                    com.lucarita.yonga.modulos.habitantes.aldea.Chismes.contar(nivel, h, h.nombre(), pareja,
                            h.nombre() + " ha dejado a " + (f.nombre(pareja).isEmpty() ? "su pareja" : f.nombre(pareja))
                                    + " por ponerle los cuernos", false);
                }
            } else {
                h.decirEnAlto(Component.translatable("yonga.amor.celos." + nivel.random.nextInt(3)));
                if (nivel.random.nextFloat() < 0.65f) {
                    f.romper(h.getUUID(), pareja);
                    f.persona(pareja).amante = null;
                    h.decirEnAlto(Component.translatable("yonga.amor.deja"));
                    com.lucarita.yonga.modulos.habitantes.aldea.Chismes.contar(nivel, h, h.nombre(), null,
                            h.nombre() + " ha dejado a " + f.nombre(pareja) + " por ponerle los cuernos", false);
                }
            }
            return;
        }
    }

    /**
     * Los poco fieles a veces tienen una aventura con otro vecino. Más probable si su pareja es un jugador que le
     * trata mal (poco aprecio). Con suerte nadie lo ve… pero si lo ven, corre el rumor.
     */
    private static void quizaAventura(ServerLevel nivel, Familias f, Habitante h, Familias.Persona p, UUID pareja,
                                      @Nullable Familias.Persona pp) {
        if (h.etapa() < 3 || !h.infielDeNatural() || h.aldea() == null) return;
        int dado = 2400;
        if (pp != null && pp.jugador) {
            Recuerdo r = h.recuerdoSiExiste(pareja);
            int op = r == null ? 0 : r.opinion;
            dado = op < 20 ? 600 : op < 50 ? 1200 : 3600;
        }
        if (nivel.random.nextInt(dado) != 0) return;
        List<Habitante> candidatos = nivel.getEntitiesOfClass(Habitante.class, h.getBoundingBox().inflate(32),
                o -> o != h && o.etapa() >= 3 && !o.getUUID().equals(pareja) && h.aldea().equals(o.aldea())
                        && !f.parientes(h.getUUID(), o.getUUID()) && h.leAtrae(o.esMujer(), true) && o.leAtrae(h.esMujer(), true));
        if (candidatos.isEmpty()) return;
        Habitante amante = candidatos.get(nivel.random.nextInt(candidatos.size()));
        p.amante = amante.getUUID();
        f.marcar();
        cuernosHabitante(nivel, h, amante.getUUID(), amante.nombre(), null);
    }

    // =====================================================================================================
    // BOTONES DE LA CONVERSACIÓN (sin depender de la IA)
    // =====================================================================================================

    /** −1 sin botones de amor (niño, familia o romance apagado); 0 aún nada; 1 novios; 2 casados. */
    public static int relacionParaBotones(ServerPlayer j, Habitante h) {
        if (!activo() || !(h.level() instanceof ServerLevel nivel)) return -1;
        Familias f = Familias.de(nivel.getServer());
        String imp = impedimento(f, h, j);
        if ("menor".equals(imp) || "familia".equals(imp) || "desactivado".equals(imp)) return -1;
        return f.estado(h.getUUID(), j.getUUID());
    }

    private static void avisarRelacion(ServerPlayer j, Habitante h) {
        HabitantesRed.CANAL.send(PacketDistributor.PLAYER.with(() -> j),
                new com.lucarita.yonga.modulos.habitantes.red.RelacionPacket(h.getId(), relacionParaBotones(j, h),
                        com.lucarita.yonga.modulos.habitantes.Ordenes.estado(j, h)));
    }

    private static Component respuesta(Habitante h, String clave) {
        return Component.translatable("yonga.amor.r." + clave + "." + h.getRandom().nextInt(2));
    }

    /** Botón "Declararte": le dices lo que sientes y le pides salir. Acepta si le gustas y te aprecia. */
    public static void declararse(ServerPlayer j, Habitante h) {
        if (!activo() || !(h.level() instanceof ServerLevel nivel)) return;
        Familias f = Familias.de(nivel.getServer());
        Recuerdo r = h.recuerdo(j);
        String accion = "*Le confiesas lo que sientes y le pides salir*";
        String imp = impedimento(f, h, j);
        int estado = f.estado(h.getUUID(), j.getUUID());
        int umbral = (h.tieneRasgo(Habitante.DESCONFIADO) ? 65 : 50) + penalizacion(nivel, h, j.getUUID());
        // Con pareja, pero poco fiel y loco por ti: deja a su pareja
        boolean dejaPorTi = "pareja".equals(imp) && h.infielDeNatural() && r.opinion >= Math.max(85, umbral + 30);
        String clave;
        if (estado > 0) clave = "ya_pareja";
        else if (dejaPorTi) clave = "deja_por_ti";
        else if (imp != null) clave = imp;
        else if (r.opinion < 0) {
            clave = "ni_hablar";
            r.cambiarOpinion(-3);
        } else if (r.opinion < umbral) clave = penalizacion(nivel, h, j.getUUID()) > 0 ? "infiel" : "poco";
        else clave = "si";
        Conversaciones.reaccion(j, h, accion, respuesta(h, clave));
        if (dejaPorTi) {
            UUID antes = f.pareja(h.getUUID());
            if (antes != null) {
                f.romper(h.getUUID(), antes);
                if (h.aldea() != null) {
                    Aldeas.de(nivel).nuevoRumor(h.aldea(), h.nombre(), j.getUUID(),
                            h.nombre() + " ha dejado a " + f.nombre(antes) + " por " + nombreDe(nivel, j),
                            h.getUUID(), nivel.getGameTime());
                }
            }
            clave = "si";
        }
        String quien = nombreDe(nivel, j);
        if (clave.equals("si")) {
            accion(j, h, r, "noviazgo");
            avisarRelacion(j, h);
            com.lucarita.yonga.modulos.habitantes.aldea.Chismes.contar(nivel, h, quien, j.getUUID(),
                    quien + " y " + h.nombre() + " ahora son pareja", false);
        } else if (!clave.equals("ya_pareja")) {
            com.lucarita.yonga.modulos.habitantes.aldea.Chismes.contar(nivel, h, quien, j.getUUID(),
                    quien + " se le declaró a " + h.nombre() + " y le dio calabazas", false);
        }
    }

    /** Botón "Romper" (con confirmación en el cliente): lo dejáis. Le duele, y la aldea se entera. */
    public static void romperBoton(ServerPlayer j, Habitante h) {
        if (!activo() || !(h.level() instanceof ServerLevel nivel)) return;
        Familias f = Familias.de(nivel.getServer());
        int estado = f.estado(h.getUUID(), j.getUUID());
        if (estado == 0) return;
        Recuerdo r = h.recuerdo(j);
        Conversaciones.reaccion(j, h, "*Le dices que lo vuestro se ha acabado*",
                respuesta(h, estado == Familias.CASADOS ? "divorcio" : "dejada"));
        f.romper(h.getUUID(), j.getUUID());
        r.cambiarOpinion(estado == Familias.CASADOS ? -35 : -20);
        if (h.aldea() != null) {
            Aldeas.de(nivel).nuevoRumor(h.aldea(), nombreDe(nivel, j), j.getUUID(),
                    nombreDe(nivel, j) + " ha dejado a " + h.nombre(), h.getUUID(), nivel.getGameTime());
        }
        Conversaciones.aviso(j, h, Component.translatable("yonga.amor.ruptura", h.nombrePara(j)));
        avisarRelacion(j, h);
    }

    /**
     * Botón "Pasar la noche juntos": con tu pareja si te quiere lo bastante; sin ser pareja, si te aprecia mucho (y
     * le gustas). Alguien con pareja solo acepta si es poco fiel y le gustas mucho. La fama de infiel lo complica.
     */
    public static void nocheJuntos(ServerPlayer j, Habitante h) {
        if (!activo() || !(h.level() instanceof ServerLevel nivel)) return;
        Familias f = Familias.de(nivel.getServer());
        Recuerdo r = h.recuerdo(j);
        String accion = "*Le propones pasar la noche juntos*";
        int estado = f.estado(h.getUUID(), j.getUUID());
        String imp = impedimento(f, h, j);
        boolean tentacion = "pareja".equals(imp) && h.infielDeNatural();
        if (imp != null && !tentacion) {
            r.cambiarOpinion(-3);
            Conversaciones.reaccion(j, h, accion, respuesta(h, "pareja".equals(imp) || "no_atrae".equals(imp) ? imp : "deprisa"));
            return;
        }
        // Opinión que hace falta: con pareja, poca; sin serlo, algo más; con su propia pareja de por medio, más aún.
        // Lo "facilón" que sea la sube o la baja hasta 25 puntos.
        int ajuste = (50 - h.facilidad()) / 2;
        int umbral = estado > 0 ? Math.max(5, 25 + ajuste)
                : Math.max(10, (tentacion ? 55 : 40) + ajuste + penalizacion(nivel, h, j.getUUID()));
        if (r.opinion < umbral) {
            boolean ofende = estado == 0 && r.opinion < umbral - 25;
            if (ofende) {
                r.cambiarOpinion(-3);
                String quien = nombreDe(nivel, j);
                com.lucarita.yonga.modulos.habitantes.aldea.Chismes.contar(nivel, h, quien, j.getUUID(),
                        quien + " intentó llevarse a la cama a " + h.nombre() + " y no le hizo ni caso", false);
            }
            Conversaciones.reaccion(j, h, accion, respuesta(h, ofende ? "deprisa" : "despacio"));
            return;
        }
        if (Encuentro.ocupado(h.getUUID()) || Encuentro.ocupado(j.getUUID()) || cansado(f, h.getUUID(), nivel.getGameTime())) {
            Conversaciones.reaccion(j, h, accion, respuesta(h, "cansado"));
            return;
        }
        BlockPos cama = Encuentro.camaCerca(nivel, h.blockPosition());
        if (cama == null) {
            Conversaciones.reaccion(j, h, accion, respuesta(h, "sin_cama"));
            return;
        }
        Conversaciones.reaccion(j, h, accion, respuesta(h, tentacion ? "a_escondidas" : "noche"));
        Encuentro.conHabitante(nivel, j, h, cama);
    }

    /** Clic derecho con el anillo: le pides matrimonio. Acepta si sois novios y te quiere mucho. */
    public static void pedirMano(ServerPlayer j, Habitante h) {
        if (!activo() || h.etapa() < 1 || !(h.level() instanceof ServerLevel nivel)) return;
        Conversaciones.abrir(j, h);
        if (h.interlocutor() != j) return;           // dormido, enfadado…
        Familias f = Familias.de(nivel.getServer());
        Recuerdo r = h.recuerdo(j);
        String accion = "*Le enseñas un anillo y le pides matrimonio*";
        int estado = f.estado(h.getUUID(), j.getUUID());
        boolean casadoConOtro = false;
        for (UUID otra : f.parejas(j.getUUID())) {
            if (!otra.equals(h.getUUID()) && f.estado(j.getUUID(), otra) == Familias.CASADOS) casadoConOtro = true;
        }
        String clave = impedimento(f, h, j) != null && estado == 0 ? "anillo_no"
                : estado == Familias.CASADOS ? "ya_casados"
                : estado == 0 ? "sin_salir"
                : casadoConOtro ? "casado_otro"
                : r.opinion < 70 ? "pronto" : "boda";
        Conversaciones.reaccion(j, h, accion, respuesta(h, clave));
        if (clave.equals("boda")) {
            accion(j, h, r, "casarse");
            avisarRelacion(j, h);
        }
    }

    /** El nombre que la aldea conoce del jugador (el que dijo tener) o su nombre de usuario. */
    public static String nombreDe(ServerLevel nivel, ServerPlayer j) {
        for (var e : nivel.getEntitiesOfClass(Habitante.class, j.getBoundingBox().inflate(64), x -> x.aldea() != null)) {
            Aldeas.Aldea a = Aldeas.de(nivel).aldeaSiExiste(e.aldea());
            if (a != null && a.nombre(j.getUUID()) != null) return a.nombre(j.getUUID());
            break;
        }
        return j.getGameProfile().getName();
    }

    private static void corazones(ServerLevel nivel, Entity e) {
        nivel.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 6, 0.35, 0.25, 0.35, 0.0);
    }

    // =====================================================================================================
    // JUGADORES: embarazo y sexo (/yonga sexo)
    // =====================================================================================================

    /** Lo que dura un embarazo, en ticks (configurable, en días reales como el crecimiento). */
    public static long duracionEmbarazo() {
        return Math.max(200L, (long) (HabitantesConfig.DIAS_EMBARAZO.get() * 1_728_000.0));
    }

    private static void embarazar(Familias f, Familias.Persona madre, UUID otro, long ahora) {
        madre.concepcion = ahora;
        madre.parto = ahora + duracionEmbarazo();
        madre.otroProgenitor = otro;
        madre.rumorEmbarazo = 0;
        f.marcar();
    }

    // =====================================================================================================
    // RUMORES DEL EMBARAZO
    // =====================================================================================================

    /** A partir de aquí la barriga ya se ve y lo sabe todo el que la mire. */
    private static final float SE_NOTA = 0.4f;

    /**
     * Habitante embarazada: al principio solo lo sabe ella (y sabe de quién es; si lo cuenta, puede destapar una
     * aventura); cuando se le nota, lo ve todo el que esté cerca.
     */
    private static void rumoresEmbarazo(ServerLevel nivel, Familias f, Habitante h, Familias.Persona p, float barriga) {
        if (barriga <= 0.0f || h.aldea() == null) return;
        Aldeas datos = Aldeas.de(nivel);
        long ahora = nivel.getGameTime();
        if (p.rumorEmbarazo < 1) {
            p.rumorEmbarazo = 1;
            f.marcar();
            String padre = p.otroProgenitor != null ? f.nombre(p.otroProgenitor) : "";
            UUID jugadorPadre = p.otroProgenitor != null && f.persona(p.otroProgenitor).jugador ? p.otroProgenitor : null;
            datos.nuevoRumor(h.aldea(), h.nombre(), jugadorPadre,
                    h.nombre() + " está embarazada" + (padre.isEmpty() ? "" : " de " + padre), h.getUUID(), ahora);
        }
        if (p.rumorEmbarazo < 2 && barriga >= SE_NOTA) {
            p.rumorEmbarazo = 2;
            f.marcar();
            UUID pareja = f.pareja(h.getUUID());
            boolean deOtro = pareja != null && p.otroProgenitor != null && !pareja.equals(p.otroProgenitor);
            com.lucarita.yonga.modulos.habitantes.aldea.Chismes.contar(nivel, h, h.nombre(), null,
                    "A " + h.nombre() + " ya se le nota la barriga: está embarazada"
                            + (pareja != null ? (deOtro ? ", y hay quien dice que el bebé no es de " + f.nombre(pareja)
                            : " de " + f.nombre(pareja)) : ", y nadie sabe de quién"), true);
        }
    }

    /** Jugadora embarazada: en cuanto se le nota y la ve algún habitante, la aldea se entera. */
    private static void rumoresEmbarazo(ServerLevel nivel, Familias f, ServerPlayer j, Familias.Persona p, float barriga) {
        if (p.rumorEmbarazo >= 2 || barriga < SE_NOTA) return;
        Habitante testigo = testigoEn(nivel, j.blockPosition());
        if (testigo == null || !testigo.hasLineOfSight(j)) return;
        p.rumorEmbarazo = 2;
        f.marcar();
        String quien = nombreDe(nivel, j);
        com.lucarita.yonga.modulos.habitantes.aldea.Chismes.contar(nivel, testigo, quien, j.getUUID(),
                quien + " está embarazada, se le nota la barriga", true);
    }

    /** Ha nacido un bebé: lo sabe todo el que esté cerca. */
    private static void rumorParto(ServerLevel nivel, Familias f, BlockPos donde, String madre, @Nullable UUID jugador,
                                   Habitante bebe) {
        Habitante testigo = testigoEn(nivel, donde);
        if (testigo == null) return;
        com.lucarita.yonga.modulos.habitantes.aldea.Chismes.contar(nivel, testigo, madre, jugador,
                madre + " ha dado a luz " + (bebe.esMujer() ? "a una niña, " : "a un niño, ") + bebe.nombre(), true);
    }

    /** El habitante adulto de una aldea más cercano (a 16 bloques como mucho), o null. */
    @Nullable
    private static Habitante testigoEn(ServerLevel nivel, BlockPos pos) {
        Habitante mejor = null;
        double d = Double.MAX_VALUE;
        for (Habitante v : nivel.getEntitiesOfClass(Habitante.class, new AABB(pos).inflate(16),
                v -> v.isAlive() && v.etapa() >= 1 && v.aldea() != null && !v.isSleeping())) {
            double dv = v.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            if (dv < d) {
                d = dv;
                mejor = v;
            }
        }
        return mejor;
    }

    /** Jugadoras a las que ya se ha mandado la barriga (para mandarles 0 cuando dan a luz). */
    private static final java.util.Set<UUID> CON_BARRIGA = new java.util.HashSet<>();

    /**
     * Cada 10 s por jugadora embarazada: la barriga (a ella y a quien la vea), los efectos y, si ya toca, el parto.
     *   · primer tercio: náuseas de vez en cuando y más hambre;
     *   · segundo: más hambre;
     *   · último: lentitud (pesa) y algo de cansancio al picar.
     */
    public static void alTickJugador(TickEvent.PlayerTickEvent evento) {
        if (evento.phase != TickEvent.Phase.END || !(evento.player instanceof ServerPlayer j)) return;
        // Nada más entrar: que elija su sexo (la pantalla sale en cuanto se cierre la de elegir oficio)
        if (j.tickCount == 10 && activo()
                && Familias.de(j.serverLevel().getServer()).persona(j.getUUID()).sexo == Familias.SIN_DECIR) {
            HabitantesRed.CANAL.send(PacketDistributor.PLAYER.with(() -> j), new PedirSexoPacket());
        }
        if (j.tickCount % 200 != 0) return;
        ServerLevel nivel = j.serverLevel();
        Familias f = Familias.de(nivel.getServer());
        Familias.Persona p = f.personaSiExiste(j.getUUID());
        long ahora = nivel.getGameTime();
        float barriga = p == null || !activo() ? 0.0f : p.embarazo(ahora);
        if (barriga > 0.0f || CON_BARRIGA.remove(j.getUUID())) {
            HabitantesRed.CANAL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> j), new BarrigaPacket(j.getId(), barriga));
            if (barriga > 0.0f) CON_BARRIGA.add(j.getUUID());
        }
        if (barriga <= 0.0f) return;
        rumoresEmbarazo(nivel, f, j, p, barriga);
        j.causeFoodExhaustion(barriga < 0.66f ? 0.6f : 0.3f);
        if (barriga < 0.33f && nivel.random.nextInt(12) == 0) {
            j.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0, false, false));
        }
        if (barriga > 0.66f) {
            j.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 260, 0, false, false, true));
            j.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 260, 0, false, false, true));
        }
        if (ahora < p.parto) return;
        UUID otro = p.otroProgenitor;
        p.parto = 0;
        p.concepcion = 0;
        p.otroProgenitor = null;
        f.marcar();
        Epoca epoca = otro != null && nivel.getEntity(otro) instanceof Habitante h ? h.epoca() : Epoca.MEDIEVAL;
        p.rumorEmbarazo = 0;
        Habitante bebe = nacer(nivel, j.blockPosition(), epoca, j.getUUID(), otro);
        if (bebe != null) rumorParto(nivel, f, j.blockPosition(), nombreDe(nivel, j), j.getUUID(), bebe);
    }

    /** La jugadora ha elegido su sexo en la pantalla del principio. */
    public static void elegirSexo(ServerPlayer j, int sexo) {
        if (sexo != Familias.HOMBRE && sexo != Familias.MUJER) return;
        Familias f = Familias.de(j.getServer());
        f.apuntar(j.getUUID(), j.getGameProfile().getName(), sexo == Familias.MUJER, true);
        f.ponerSexo(j.getUUID(), sexo);
        j.displayClientMessage(Component.translatable(sexo == Familias.MUJER ? "yonga.amor.sexo.mujer" : "yonga.amor.sexo.hombre"), true);
    }

    public static void alRegistrarComandos(RegisterCommandsEvent evento) {
        CommandDispatcher<CommandSourceStack> d = evento.getDispatcher();
        d.register(Commands.literal("yonga").then(Commands.literal("sexo")
                .then(Commands.literal("hombre").executes(c -> ponerSexo(c.getSource(), Familias.HOMBRE)))
                .then(Commands.literal("mujer").executes(c -> ponerSexo(c.getSource(), Familias.MUJER))))
                .then(Commands.literal("familia").executes(c -> {
                    Arbol.enviar(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("habitante").requires(f -> f.hasPermission(2))
                        .then(Commands.argument("epoca", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                        java.util.Arrays.stream(Epoca.values()).map(Epoca::id), b))
                                .executes(c -> aparecer(c.getSource(),
                                        com.mojang.brigadier.arguments.StringArgumentType.getString(c, "epoca"), 1))
                                .then(Commands.argument("cuantos", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 20))
                                        .executes(c -> aparecer(c.getSource(),
                                                com.mojang.brigadier.arguments.StringArgumentType.getString(c, "epoca"),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "cuantos"))))))
                .then(Commands.literal("taberna").requires(f -> f.hasPermission(2))
                        .executes(c -> com.lucarita.yonga.modulos.habitantes.taberna.Taberna.forzar(c.getSource().getPlayerOrException()))));
    }

    /** /yonga habitante <época> [cuántos]: aparecen habitantes de esa época (para probar cada una). */
    private static int aparecer(CommandSourceStack fuente, String id, int cuantos) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer j = fuente.getPlayerOrException();
        Epoca epoca = null;
        for (Epoca e : Epoca.values()) if (e.id().equals(id)) epoca = e;
        if (epoca == null) {
            fuente.sendFailure(Component.literal("Época desconocida: " + id));
            return 0;
        }
        ServerLevel nivel = j.serverLevel();
        for (int i = 0; i < cuantos; i++) {
            Habitante h = ModuloHabitantes.HABITANTE.get().create(nivel);
            if (h == null) return 0;
            h.moveTo(j.getX() + nivel.random.nextGaussian(), j.getY(), j.getZ() + nivel.random.nextGaussian(),
                    nivel.random.nextFloat() * 360.0f, 0.0f);
            h.generar(epoca, 3, nivel.random);
            h.finalizeSpawn(nivel, nivel.getCurrentDifficultyAt(j.blockPosition()), MobSpawnType.COMMAND, null, null);
            nivel.addFreshEntity(h);
        }
        return cuantos;
    }

    private static int ponerSexo(CommandSourceStack fuente, int sexo) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer j = fuente.getPlayerOrException();
        Familias f = Familias.de(fuente.getServer());
        f.apuntar(j.getUUID(), j.getGameProfile().getName(), sexo == Familias.MUJER, true);
        f.ponerSexo(j.getUUID(), sexo);
        fuente.sendSuccess(() -> Component.translatable(sexo == Familias.MUJER ? "yonga.amor.sexo.mujer" : "yonga.amor.sexo.hombre"), false);
        return 1;
    }
}
