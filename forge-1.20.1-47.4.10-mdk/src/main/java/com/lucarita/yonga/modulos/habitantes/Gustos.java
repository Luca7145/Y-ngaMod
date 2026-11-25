package com.lucarita.yonga.modulos.habitantes;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Lo que le gusta y lo que no a cada habitante, para los regalos. Cada uno tiene dos cosas que le encantan y una que
 * no soporta (de su UUID, y algo de su carácter: a los avariciosos les pirran las joyas, a los curiosos los
 * cachivaches, a los orgullosos las armas; a los niños, los dulces). La basura no le gusta a nadie y lo macabro
 * espanta a los supersticiosos. Lo demás se agradece sin más. La IA sabe sus gustos y puede contarlos.
 */
public final class Gustos {

    private Gustos() {}

    public enum Cosa {
        FLORES("las flores", s -> s.is(ItemTags.FLOWERS)),
        DULCES("los dulces", s -> s.is(Items.CAKE) || s.is(Items.COOKIE) || s.is(Items.PUMPKIN_PIE) || s.is(Items.HONEY_BOTTLE)
                || s.is(Items.SWEET_BERRIES) || s.is(Items.GLOW_BERRIES) || s.is(Items.GOLDEN_APPLE) || s.is(Items.SUGAR)),
        COMIDA("la buena comida", s -> s.isEdible() && !basura(s)),
        JOYAS("las joyas y el oro", s -> s.is(Items.DIAMOND) || s.is(Items.EMERALD) || s.is(Items.GOLD_INGOT)
                || s.is(Items.GOLD_NUGGET) || s.is(Items.AMETHYST_SHARD) || s.is(Items.LAPIS_LAZULI) || s.is(Items.GOLD_BLOCK)
                || s.is(Items.DIAMOND_BLOCK) || s.is(Items.EMERALD_BLOCK) || s.is(Items.RAW_GOLD) || s.is(Items.QUARTZ)),
        LIBROS("los libros", s -> s.is(Items.BOOK) || s.is(Items.WRITABLE_BOOK) || s.is(Items.WRITTEN_BOOK)
                || s.is(Items.ENCHANTED_BOOK) || s.is(Items.PAPER) || s.is(Items.MAP)),
        MUSICA("la música", s -> s.is(ItemTags.MUSIC_DISCS) || s.is(Items.NOTE_BLOCK) || s.is(Items.JUKEBOX) || s.is(Items.GOAT_HORN)),
        CACHIVACHES("los cachivaches curiosos", s -> s.is(Items.COMPASS) || s.is(Items.CLOCK) || s.is(Items.SPYGLASS)
                || s.is(Items.RECOVERY_COMPASS) || s.is(Items.FILLED_MAP) || s.is(Items.NAUTILUS_SHELL) || s.is(Items.PRISMARINE_SHARD)
                || s.is(Items.HEART_OF_THE_SEA) || s.is(Items.ECHO_SHARD)),
        ARMAS("las armas y armaduras", s -> s.getItem() instanceof SwordItem || s.getItem() instanceof BowItem
                || s.getItem() instanceof CrossbowItem || s.getItem() instanceof TridentItem || s.getItem() instanceof ArmorItem
                || s.is(Items.SHIELD)),
        HERRAMIENTAS("las buenas herramientas", s -> s.getItem() instanceof DiggerItem || s.is(Items.SHEARS)
                || s.is(Items.FISHING_ROD) || s.is(Items.FLINT_AND_STEEL)),
        NATURALEZA("las plantas y el campo", s -> s.is(ItemTags.SAPLINGS) || s.is(Items.WHEAT_SEEDS) || s.is(Items.WHEAT)
                || s.is(Items.HONEYCOMB) || s.is(Items.FEATHER) || s.is(Items.EGG) || s.is(ItemTags.LEAVES)
                || s.is(Items.MOSS_BLOCK) || s.is(Items.LILY_PAD) || s.is(Items.VINE)),
        TELAS("las telas bonitas", s -> s.is(ItemTags.WOOL) || s.is(ItemTags.WOOL_CARPETS) || s.is(Items.LEATHER)
                || s.is(ItemTags.BANNERS));

        public final String nombre;
        private final Predicate<ItemStack> es;

        Cosa(String nombre, Predicate<ItemStack> es) {
            this.nombre = nombre;
            this.es = es;
        }

        public boolean es(ItemStack s) {
            return this.es.test(s);
        }
    }

    /** Basura: a nadie le gusta que le regalen esto. */
    public static boolean basura(ItemStack s) {
        return s.is(Items.ROTTEN_FLESH) || s.is(Items.SPIDER_EYE) || s.is(Items.POISONOUS_POTATO) || s.is(Items.BONE)
                || s.is(Items.DIRT) || s.is(Items.GRAVEL) || s.is(Items.COBBLESTONE) || s.is(Items.STICK)
                || s.is(Items.PUFFERFISH) || s.is(Items.SAND) || s.is(Items.DEAD_BUSH)
                || s.is(Items.SLIME_BALL) || s.is(Items.GUNPOWDER) || s.is(Items.NETHERRACK);
    }

    /** Lo macabro: espanta a los supersticiosos (y no le hace gracia a casi nadie). */
    public static boolean macabro(ItemStack s) {
        return s.is(Items.SKELETON_SKULL) || s.is(Items.WITHER_SKELETON_SKULL) || s.is(Items.ZOMBIE_HEAD)
                || s.is(Items.CREEPER_HEAD) || s.is(Items.PLAYER_HEAD) || s.is(Items.PIGLIN_HEAD) || s.is(Items.ENDER_PEARL)
                || s.is(Items.ENDER_EYE) || s.is(Items.PHANTOM_MEMBRANE) || s.is(Items.GHAST_TEAR)
                || s.is(Items.FERMENTED_SPIDER_EYE) || s.is(Items.WITHER_ROSE);
    }

    private static long semilla(Habitante h) {
        return h.getUUID().getMostSignificantBits() ^ (h.getUUID().getLeastSignificantBits() * 0x9E3779B97F4A7C15L);
    }

    /** Las dos cosas que más le gustan. */
    public static List<Cosa> leEncantan(Habitante h) {
        Cosa[] todas = Cosa.values();
        long s = semilla(h);
        List<Cosa> lista = new ArrayList<>();
        if (h.etapa() <= 1) lista.add(Cosa.DULCES);
        if (h.tieneRasgo(Habitante.AVARICIOSO)) lista.add(Cosa.JOYAS);
        if (h.tieneRasgo(Habitante.CURIOSO) && !lista.contains(Cosa.CACHIVACHES)) lista.add(Cosa.CACHIVACHES);
        if (h.tieneRasgo(Habitante.ORGULLOSO) && h.etapa() >= 2) lista.add(Cosa.ARMAS);
        int i = 0;
        while (lista.size() < 2) {
            Cosa c = todas[(int) Math.floorMod(s >>> (i * 5), (long) todas.length)];
            if (!lista.contains(c)) lista.add(c);
            if (++i > 12) {
                for (Cosa x : todas) if (lista.size() < 2 && !lista.contains(x)) lista.add(x);
            }
        }
        return lista.subList(0, 2);
    }

    /** Lo que no soporta (aparte de la basura). */
    public static Cosa noSoporta(Habitante h) {
        Cosa[] todas = Cosa.values();
        List<Cosa> gustan = leEncantan(h);
        long s = semilla(h) >>> 23;
        for (int i = 0; i < todas.length; i++) {
            Cosa c = todas[(int) Math.floorMod(s + i, (long) todas.length)];
            if (!gustan.contains(c) && c != Cosa.COMIDA) return c;
        }
        return Cosa.ARMAS;
    }

    /** 2 le encanta · 1 le gusta · 0 lo agradece · −1 no le gusta · −2 lo odia. */
    public static int valorar(Habitante h, ItemStack s) {
        if (s.isEmpty()) return 0;
        if (macabro(s)) return h.tieneRasgo(Habitante.SUPERSTICIOSO) ? -2 : -1;
        if (basura(s)) return -2;
        for (Cosa c : leEncantan(h)) if (c.es(s)) return 2;
        if (noSoporta(h).es(s)) return -1;
        Oficio o = h.oficio();
        if (o != null && !o.herramienta().isEmpty() && s.is(o.herramienta().getItem())) return 1;
        if (s.getRarity() != net.minecraft.world.item.Rarity.COMMON || s.isEnchanted()) return 1;
        return 0;
    }

    /**
     * Botón "Regalar": le das lo que llevas en la mano (uno). Según sus gustos le encanta, le gusta, lo agradece, no le
     * gusta o lo odia, y su opinión de ti sube o baja. Varios regalos el mismo día hacen cada vez menos ilusión.
     * Los objetos de oficio (rol) no se regalan.
     */
    public static void regalar(net.minecraft.server.level.ServerPlayer j, Habitante h) {
        ItemStack mano = j.getMainHandItem();
        if (mano.isEmpty()) {
            com.lucarita.yonga.modulos.habitantes.ia.Conversaciones.aviso(j, h,
                    net.minecraft.network.chat.Component.translatable("yonga.regalo.vacio"));
            return;
        }
        if (com.lucarita.yonga.modulos.roles.ModuloRoles.ITEMS.getEntries().stream().anyMatch(r -> mano.is(r.get()))) {
            com.lucarita.yonga.modulos.habitantes.ia.Conversaciones.aviso(j, h,
                    net.minecraft.network.chat.Component.translatable("yonga.regalo.rol"));
            return;
        }
        int valor = valorar(h, mano);
        String nombre = mano.getHoverName().getString();
        if (!j.getAbilities().instabuild) mano.shrink(1);
        var r = h.recuerdo(j);
        long dia = h.level().getDayTime() / 24000L;
        if (r.diaRegalos != dia) {
            r.diaRegalos = dia;
            r.regalosHoy = 0;
        }
        int[] cambios = {-10, -4, 3, 7, 12};
        int cambio = cambios[valor + 2];
        if (cambio > 0) cambio = h.ajustarOpinion(Math.max(1, Math.round(cambio / (1.0f + r.regalosHoy))));
        r.regalosHoy++;
        r.cambiarOpinion(cambio);
        String clave = valor >= 0 && r.regalosHoy > 3 ? "harto" : String.valueOf(valor + 2);
        com.lucarita.yonga.modulos.habitantes.ia.Conversaciones.reaccion(j, h, "*Le regalas: " + nombre + "*",
                net.minecraft.network.chat.Component.translatable("yonga.regalo.r." + clave + "." + h.getRandom().nextInt(3)));
        if (valor == 2 && h.level() instanceof net.minecraft.server.level.ServerLevel nivel) {
            nivel.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, h.getX(), h.getY() + h.getBbHeight() + 0.3,
                    h.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
        } else if (valor < 0 && h.level() instanceof net.minecraft.server.level.ServerLevel nivel) {
            nivel.sendParticles(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER, h.getX(), h.getY() + h.getBbHeight() + 0.3,
                    h.getZ(), 2, 0.3, 0.2, 0.3, 0.0);
        }
    }

    /** Para la IA: "te encantan las flores y los libros; no soportas las armas y armaduras". */
    public static String describir(Habitante h) {
        List<Cosa> g = leEncantan(h);
        return "Te encantan " + g.get(0).nombre + " y " + g.get(1).nombre + "; no soportas " + noSoporta(h).nombre
                + (h.tieneRasgo(Habitante.SUPERSTICIOSO) ? ", y lo macabro (calaveras, ojos de araña…) te da pavor" : "")
                + ". Si te preguntan qué te gusta, puedes decirlo.";
    }
}
