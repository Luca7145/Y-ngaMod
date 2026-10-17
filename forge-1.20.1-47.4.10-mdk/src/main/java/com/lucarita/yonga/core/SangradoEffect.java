package com.lucarita.yonga.core;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SangradoEffect extends MobEffect {

    public SangradoEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return new ArrayList<>(); // Evita que la leche lo cure
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // LÓGICA DE GOTEO PERSONALIZADA
        if (!entity.level().isClientSide()) {
            // Suelta una gota solo el 15% de los ticks, reduciendo drásticamente la cantidad
            if (entity.getRandom().nextFloat() < 0.15f) {
                ServerLevel level = (ServerLevel) entity.level();
                level.sendParticles(ModParticles.SANGRE_GOTEO.get(),
                        entity.getX(), entity.getY() + (entity.getBbHeight() / 1.5), entity.getZ(),
                        1, 0.15, 0.3, 0.15, 0.0);
            }
        }

        // LÓGICA DE DAÑO (Cada 4 segundos)
        if (entity.tickCount % 80 == 0) {
            float daño = (amplifier == 0) ? 1.0f : 2.0f;
            entity.hurt(entity.damageSources().magic(), daño);
        }

        super.applyEffectTick(entity, amplifier);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}