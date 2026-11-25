package com.lucarita.yonga.modulos.habitantes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Lo que un habitante tiene a la venta: 9 huecos, cada uno con un lote (p. ej. 12 de trigo), su precio en monedas y
 * cuántos lotes iguales le quedan (hasta 8). Lo que produce al llenar su barra de trabajo va reponiendo los huecos.
 */
public class Tienda {

    public static final int HUECOS = 9;
    public static final int MAX_LOTES = 8;

    public static final class Oferta {
        public final ItemStack lote;
        public final int precio;
        public int lotes;

        Oferta(ItemStack lote, int precio, int lotes) {
            this.lote = lote;
            this.precio = precio;
            this.lotes = lotes;
        }
    }

    private final Oferta[] ofertas = new Oferta[HUECOS];

    @Nullable
    public Oferta oferta(int i) {
        return i >= 0 && i < HUECOS ? ofertas[i] : null;
    }

    /** Añade un lote: junto a los iguales si caben, si no en un hueco libre. false = tienda llena. */
    public boolean anadir(ItemStack lote, int precio) {
        for (Oferta o : ofertas) {
            if (o != null && o.lotes < MAX_LOTES && o.precio == precio && o.lote.getCount() == lote.getCount()
                    && ItemStack.isSameItemSameTags(o.lote, lote)) {
                o.lotes++;
                return true;
            }
        }
        for (int i = 0; i < HUECOS; i++) {
            if (ofertas[i] == null) {
                ofertas[i] = new Oferta(lote.copy(), precio, 1);
                return true;
            }
        }
        return false;
    }

    public boolean llena() {
        for (Oferta o : ofertas) {
            if (o == null || o.lotes < MAX_LOTES) return false;
        }
        return true;
    }

    /** Quita un lote del hueco i (y vacía el hueco si era el último). */
    public void quitarLote(int i) {
        Oferta o = oferta(i);
        if (o != null && --o.lotes <= 0) {
            ofertas[i] = null;
        }
    }

    // ---------- guardar ----------

    public CompoundTag guardar() {
        CompoundTag nbt = new CompoundTag();
        ListTag lista = new ListTag();
        for (int i = 0; i < HUECOS; i++) {
            Oferta o = ofertas[i];
            if (o == null) continue;
            CompoundTag t = new CompoundTag();
            t.putByte("hueco", (byte) i);
            t.put("lote", o.lote.save(new CompoundTag()));
            t.putInt("precio", o.precio);
            t.putInt("lotes", o.lotes);
            lista.add(t);
        }
        nbt.put("ofertas", lista);
        return nbt;
    }

    public void cargar(CompoundTag nbt) {
        java.util.Arrays.fill(ofertas, null);
        ListTag lista = nbt.getList("ofertas", Tag.TAG_COMPOUND);
        for (int k = 0; k < lista.size(); k++) {
            CompoundTag t = lista.getCompound(k);
            int i = t.getByte("hueco");
            ItemStack lote = ItemStack.of(t.getCompound("lote"));
            if (i >= 0 && i < HUECOS && !lote.isEmpty()) {
                ofertas[i] = new Oferta(lote, t.getInt("precio"), Math.max(1, t.getInt("lotes")));
            }
        }
    }
}
