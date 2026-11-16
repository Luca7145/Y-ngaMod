package com.lucarita.yonga.modulos.armas;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.core.ModTiers;
import com.lucarita.yonga.modulos.armas.ArmaYonga.Rasgos;
import com.lucarita.yonga.modulos.roles.RangoRol;
import com.lucarita.yonga.modulos.sangrado.Sangrado;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Módulo de armas: todas las armas de lucarita, el parry, el contraataque y el aturdimiento.
 * Solo se registra si el módulo está activado (ver YongaMain).
 *
 * Daño que verás en el juego = 1 (jugador) + 4 (lucarita) + daño extra.
 * Velocidad que verás en el juego = 4 + modificador de velocidad.
 * Como referencia, una espada de netherita: 8 de daño, 1.6 de velocidad.
 */
public final class ModuloArmas {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, YongaMain.MOD_ID);
    public static final DeferredRegister<MobEffect> EFECTOS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, YongaMain.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECETAS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, YongaMain.MOD_ID);

    // ---------- ARMAS (en orden de rango de herrero) ----------
    //                                     daño extra, velocidad, durabilidad

    // Novato. Daño 6, velocidad 2.2. Rápido y flojo, sangrado débil.
    public static final RegistryObject<Item> PUNAL = arma("punal", 1, -1.8f, 1500,
            Rasgos.nuevo().herrero(RangoRol.NOVATO).sangrado(Sangrado.PUNAL));

    // Aprendiz. Daño 8, velocidad 1.8. Sin habilidades: sólido y fiable.
    public static final RegistryObject<Item> GLADIO = arma("gladio", 3, -2.2f, 1900,
            Rasgos.nuevo().herrero(RangoRol.APRENDIZ));

    // Oficial. Daño 11, velocidad 0.9. +1 bloque de alcance, sangrado medio.
    public static final RegistryObject<Item> ALABARDA = arma("alabarda", 6, -3.1f, 2100,
            Rasgos.nuevo().herrero(RangoRol.OFICIAL).alcance(1.0).sangrado(Sangrado.ALABARDA));

    // Experto. Daño 10, velocidad 1.4. La especialista en sangrado.
    public static final RegistryObject<Item> FALCATA = arma("falcata", 5, -2.6f, 2300,
            Rasgos.nuevo().herrero(RangoRol.EXPERTO).sangrado(Sangrado.FALCATA));

    // Experto. Daño 12, velocidad 0.9. 20% de aturdir, rompe escudos, sin barrido.
    public static final RegistryObject<Item> MAZA = arma("maza", 7, -3.1f, 2500,
            Rasgos.nuevo().herrero(RangoRol.EXPERTO).aturdir(0.20f).rompeEscudos().sinBarrido());

    // Maestro. Daño 8, velocidad 2.0. Parry + contraataque (+50% tras bloquear).
    public static final RegistryObject<Item> ROPERA = arma("ropera", 3, -2.0f, 2600,
            Rasgos.nuevo().herrero(RangoRol.MAESTRO).parry().contraataque());

    // Maestro. Daño 11, velocidad 1.6. Parry que también para flechas.
    public static final RegistryObject<Item> ESPADA_CABALLERESCA = arma("espada_caballeresca", 6, -2.4f, 2800,
            Rasgos.nuevo().herrero(RangoRol.MAESTRO).parryTambienProyectiles());

    // ---------- EFECTOS ----------

    // Aturdido: -100% de velocidad de movimiento (lo de no atacar ni saltar va en Aturdimiento)
    public static final RegistryObject<MobEffect> ATURDIDO = EFECTOS.register("aturdido",
            () -> new Aturdimiento.Efecto().addAttributeModifier(Attributes.MOVEMENT_SPEED,
                    "5d0c1f3a-6b7e-4c2d-8e9f-a1b2c3d4e5f6", -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));

    // ---------- RECETAS ----------

    // "yonga:arma_herrero": receta con forma que solo puede usar un Herrero con el rango del arma
    public static final RegistryObject<RecipeSerializer<RecetaHerrero>> RECETA_HERRERO =
            RECETAS.register("arma_herrero", RecetaHerrero.Serializer::new);

    private ModuloArmas() {}

    private static RegistryObject<Item> arma(String nombre, int dañoExtra, float velocidad, int durabilidad, Rasgos rasgos) {
        return ITEMS.register(nombre, () -> new ArmaYonga(ModTiers.LUCARITA, dañoExtra, velocidad, rasgos,
                new Item.Properties().durability(durabilidad)));
    }

    public static void registrar(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        EFECTOS.register(modEventBus);
        RECETAS.register(modEventBus);

        MinecraftForge.EVENT_BUS.addListener(CombateEventos::alSerAtacado);
        MinecraftForge.EVENT_BUS.addListener(CombateEventos::alHacerDano);
        MinecraftForge.EVENT_BUS.addListener(CombateEventos::alSalir);
        MinecraftForge.EVENT_BUS.addListener(Aturdimiento::alAtacarAlguien);
        MinecraftForge.EVENT_BUS.addListener(Aturdimiento::alAtacarJugador);
        MinecraftForge.EVENT_BUS.addListener(Aturdimiento::alSaltar);
    }
}
