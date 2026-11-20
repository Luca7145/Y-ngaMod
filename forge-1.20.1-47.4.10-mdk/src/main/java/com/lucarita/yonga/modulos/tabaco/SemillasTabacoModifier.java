package com.lucarita.yonga.modulos.tabaco;

import com.google.common.base.Suppliers;
import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.Modulo;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * De dónde salen las semillas de tabaco: al romper hierba o helechos (sin tijeras), de vez en cuando, como las de
 * trigo. Se registra SIEMPRE (su JSON siempre está), pero solo hace algo con el módulo de tabaco activo.
 */
public class SemillasTabacoModifier extends LootModifier {

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> MODIFICADORES =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, YongaMain.MOD_ID);

    public static final Supplier<Codec<SemillasTabacoModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(i -> codecStart(i)
                    .and(Codec.FLOAT.fieldOf("probabilidad").forGetter(m -> m.probabilidad))
                    .apply(i, SemillasTabacoModifier::new)));

    public static final RegistryObject<Codec<SemillasTabacoModifier>> SEMILLAS_TABACO = MODIFICADORES.register("semillas_tabaco", CODEC);

    private final float probabilidad;

    public SemillasTabacoModifier(LootItemCondition[] condiciones, float probabilidad) {
        super(condiciones);
        this.probabilidad = probabilidad;
    }

    public static void registrar(IEventBus bus) {
        MODIFICADORES.register(bus);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generado, LootContext contexto) {
        if (!Modulo.TABACO.activo()) return generado;
        BlockState bloque = contexto.getParamOrNull(LootContextParams.BLOCK_STATE);
        ItemStack herramienta = contexto.getParamOrNull(LootContextParams.TOOL);
        if (bloque == null || (herramienta != null && herramienta.is(Items.SHEARS))) return generado;
        boolean hierba = bloque.is(Blocks.GRASS) || bloque.is(Blocks.TALL_GRASS) || bloque.is(Blocks.FERN) || bloque.is(Blocks.LARGE_FERN);
        if (hierba && contexto.getRandom().nextFloat() < probabilidad) {
            generado.add(new ItemStack(ModuloTabaco.SEMILLAS.get()));
        }
        return generado;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
