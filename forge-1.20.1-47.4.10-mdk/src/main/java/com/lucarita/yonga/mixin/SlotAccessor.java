package com.lucarita.yonga.mixin;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Para mover un hueco de sitio en una pantalla (el de la cara en el inventario del creativo). */
@Mixin(Slot.class)
public interface SlotAccessor {
    @Mutable
    @Accessor("x")
    void yonga$ponerX(int x);

    @Mutable
    @Accessor("y")
    void yonga$ponerY(int y);
}
