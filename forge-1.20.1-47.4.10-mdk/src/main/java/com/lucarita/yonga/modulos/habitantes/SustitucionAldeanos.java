package com.lucarita.yonga.modulos.habitantes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cada aldeano de Minecraft que entra al mundo (los de las aldeas al cargar, los que nacen, los curados) se cambia
 * por un habitante en el mismo sitio. El aldeano adulto pasa a tener una edad al azar (la mitad
 * adultos, el resto jóvenes, niños y bebés) y el aldeano bebé, bebé o niño. Si tenía nombre puesto con etiqueta, lo conserva.
 *
 * El oficio se conserva cuando tiene equivalente (granjero, herrero, carnicero…), con su nivel (si sale adulto) y su
 * bloque de trabajo; los que no lo tienen (bibliotecario, cartógrafo) reciben uno al azar. Los aldeanos sin oficio
 * siguen en paro (buscarán puesto) y los "tontos" (nitwit) son holgazanes que nunca trabajan.
 *
 * El habitante se crea en el tick siguiente: añadir entidades mientras se carga un chunk puede bloquear el servidor.
 */
public final class SustitucionAldeanos {

    private SustitucionAldeanos() {}

    private record Pendiente(Vec3 pos, float giro, boolean bebe, net.minecraft.network.chat.Component nombre,
                             @Nullable Oficio oficio, int nivel, @Nullable BlockPos puesto, boolean guardia, int paro) {}

    /** Un guardia en lugar de un gólem de hierro de aldea (se crea en el tick siguiente). */
    public static void guardiaEn(ServerLevel nivel, Vec3 pos, float giro) {
        PENDIENTES.computeIfAbsent(nivel, n -> new ArrayList<>())
                .add(new Pendiente(pos, giro, false, null, null, 0, null, true, 0));
    }

    @Nullable
    private static Oficio equivalente(VillagerProfession profesion) {
        if (profesion == VillagerProfession.FARMER) return Oficio.GRANJERO;
        if (profesion == VillagerProfession.ARMORER || profesion == VillagerProfession.TOOLSMITH
                || profesion == VillagerProfession.WEAPONSMITH) return Oficio.HERRERO;
        if (profesion == VillagerProfession.BUTCHER) return Oficio.CARNICERO;
        if (profesion == VillagerProfession.FISHERMAN) return Oficio.PESCADOR;
        if (profesion == VillagerProfession.FLETCHER) return Oficio.LENADOR;
        if (profesion == VillagerProfession.MASON) return Oficio.CANTERO;
        if (profesion == VillagerProfession.SHEPHERD || profesion == VillagerProfession.LEATHERWORKER) return Oficio.TEJEDOR;
        if (profesion == VillagerProfession.CLERIC) return Oficio.CURANDERO;
        return null;
    }

    private static final Map<ServerLevel, List<Pendiente>> PENDIENTES = new HashMap<>();

    public static void alEntrar(EntityJoinLevelEvent evento) {
        if (!(evento.getLevel() instanceof ServerLevel nivel) || evento.getEntity().getType() != EntityType.VILLAGER
                || !HabitantesConfig.SUSTITUIR_ALDEANOS.get()) {
            return;
        }
        Villager aldeano = (Villager) evento.getEntity();
        evento.setCanceled(true);
        Oficio oficio = equivalente(aldeano.getVillagerData().getProfession());
        BlockPos puesto = null;
        if (oficio != null) {
            puesto = aldeano.getBrain().getMemory(MemoryModuleType.JOB_SITE)
                    .filter(g -> g.dimension() == nivel.dimension()).map(GlobalPos::pos).orElse(null);
        }
        PENDIENTES.computeIfAbsent(nivel, n -> new ArrayList<>())
                .add(new Pendiente(aldeano.position(), aldeano.getYRot(), aldeano.isBaby(), aldeano.getCustomName(),
                        oficio, aldeano.getVillagerData().getLevel(), puesto, false,
                        aldeano.getVillagerData().getProfession() == VillagerProfession.NITWIT ? 2
                                : aldeano.getVillagerData().getProfession() == VillagerProfession.NONE ? 1 : 0));
    }

    public static void alTickNivel(TickEvent.LevelTickEvent evento) {
        if (evento.phase != TickEvent.Phase.END || !(evento.level instanceof ServerLevel nivel)) {
            return;
        }
        List<Pendiente> lista = PENDIENTES.remove(nivel);
        if (lista == null) {
            return;
        }
        for (Pendiente p : lista) {
            Habitante habitante = ModuloHabitantes.HABITANTE.get().create(nivel);
            if (habitante == null) {
                continue;
            }
            habitante.moveTo(p.pos().x, p.pos().y, p.pos().z, p.giro(), 0.0f);
            habitante.setYHeadRot(p.giro());
            int etapa = p.guardia() ? 3 : p.bebe() ? nivel.random.nextInt(2) : Habitante.etapaAlAzar(nivel.random);
            // De la época que le toca a su aldea (fase 8)
            habitante.generar(com.lucarita.yonga.modulos.habitantes.aldea.Poblados.epocaAldeano(nivel,
                    net.minecraft.core.BlockPos.containing(p.pos())), etapa, nivel.random);
            if (p.oficio() != null) {
                habitante.asignarOficio(p.oficio().enEpoca(habitante.epoca()), etapa == 3 ? p.nivel() : 1, p.puesto());
            } else if (p.paro() > 0) {
                habitante.quitarOficio(p.paro() == 2);
            }
            habitante.finalizeSpawn(nivel, nivel.getCurrentDifficultyAt(habitante.blockPosition()),
                    MobSpawnType.CONVERSION, null, null);
            if (p.guardia()) {
                habitante.hacerseGuardia();
            }
            if (p.nombre() != null) {
                habitante.setCustomName(p.nombre());
            }
            nivel.addFreshEntity(habitante);
        }
    }

    public static void alDescargarNivel(net.minecraftforge.event.level.LevelEvent.Unload evento) {
        PENDIENTES.remove(evento.getLevel());
    }
}
