package com.lucarita.yonga.modulos.roles.construccion;

import com.lucarita.yonga.modulos.roles.ModuloRoles;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Menú del banco de ingeniero. Funciona como el del cortapiedras: pones un bloque a la izquierda, eliges
 * un diseño de la lista y lo coges a la derecha. Solo salen los diseños que puede hacer quien lo usa
 * (ver RecetaBanco.puedeUsar).
 */
public class BancoMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess acceso;
    private final DataSlot elegida = DataSlot.standalone();
    private final Level nivel;
    private final Player jugador;
    private List<RecetaBanco> recetas = new ArrayList<>();
    private ItemStack entradaAnterior = ItemStack.EMPTY;
    private long ultimoSonido;
    final Slot huecoEntrada;
    final Slot huecoResultado;
    private Runnable alCambiar = () -> {};

    public final Container entrada = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            BancoMenu.this.slotsChanged(this);
            BancoMenu.this.alCambiar.run();
        }
    };
    private final ResultContainer resultado = new ResultContainer();

    public BancoMenu(int id, Inventory inventario, ContainerLevelAccess acceso) {
        super(ModuloRoles.MENU_BANCO.get(), id);
        this.acceso = acceso;
        this.nivel = inventario.player.level();
        this.jugador = inventario.player;
        this.huecoEntrada = addSlot(new Slot(entrada, 0, 20, 33));
        this.huecoResultado = addSlot(new Slot(resultado, 1, 143, 33) {
            @Override
            public boolean mayPlace(ItemStack objeto) {
                return false;
            }

            @Override
            public void onTake(Player quien, ItemStack objeto) {
                objeto.onCraftedBy(quien.level(), quien, objeto.getCount());
                ItemStack usado = huecoEntrada.remove(1);
                if (!usado.isEmpty()) {
                    prepararResultado();
                }
                acceso.execute((nivelBanco, pos) -> {
                    long ahora = nivelBanco.getGameTime();
                    if (ultimoSonido != ahora) {
                        nivelBanco.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0f, 1.0f);
                        ultimoSonido = ahora;
                    }
                });
                super.onTake(quien, objeto);
            }
        });
        for (int fila = 0; fila < 3; fila++) {
            for (int columna = 0; columna < 9; columna++) {
                addSlot(new Slot(inventario, columna + fila * 9 + 9, 8 + columna * 18, 84 + fila * 18));
            }
        }
        for (int columna = 0; columna < 9; columna++) {
            addSlot(new Slot(inventario, columna, 8 + columna * 18, 142));
        }
        addDataSlot(elegida);
    }

    public BancoMenu(int id, Inventory inventario) {
        this(id, inventario, ContainerLevelAccess.NULL);
    }

    public int elegida() { return elegida.get(); }
    public List<RecetaBanco> recetas() { return recetas; }
    public int cuantas() { return recetas.size(); }
    public boolean hayEntrada() { return huecoEntrada.hasItem() && !recetas.isEmpty(); }
    public void alCambiar(Runnable accion) { this.alCambiar = accion; }

    @Override
    public boolean stillValid(Player quien) {
        return stillValid(acceso, quien, ModuloRoles.BANCO_INGENIERO.get());
    }

    @Override
    public boolean clickMenuButton(Player quien, int indice) {
        if (indice >= 0 && indice < recetas.size()) {
            elegida.set(indice);
            prepararResultado();
            return true;
        }
        return false;
    }

    @Override
    public void slotsChanged(Container contenedor) {
        ItemStack ahora = huecoEntrada.getItem();
        if (!ahora.is(entradaAnterior.getItem())) {
            entradaAnterior = ahora.copy();
            prepararLista(contenedor, ahora);
        }
    }

    private void prepararLista(Container contenedor, ItemStack objeto) {
        recetas = new ArrayList<>();
        elegida.set(-1);
        huecoResultado.set(ItemStack.EMPTY);
        if (!objeto.isEmpty()) {
            for (RecetaBanco receta : nivel.getRecipeManager().getRecipesFor(ModuloRoles.TIPO_BANCO.get(), contenedor, nivel)) {
                if (receta.puedeUsar(jugador)) {
                    recetas.add(receta);
                }
            }
            // El mismo orden en el cliente y en el servidor
            recetas.sort(Comparator.comparing(r -> r.getId().toString()));
        }
    }

    private void prepararResultado() {
        int i = elegida.get();
        if (!recetas.isEmpty() && i >= 0 && i < recetas.size()) {
            RecetaBanco receta = recetas.get(i);
            resultado.setRecipeUsed(receta);
            huecoResultado.set(receta.assemble(entrada, nivel.registryAccess()));
        } else {
            huecoResultado.set(ItemStack.EMPTY);
        }
        broadcastChanges();
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack objeto, Slot hueco) {
        return hueco.container != resultado && super.canTakeItemForPickAll(objeto, hueco);
    }

    @Override
    public ItemStack quickMoveStack(Player quien, int indice) {
        ItemStack copia = ItemStack.EMPTY;
        Slot hueco = slots.get(indice);
        if (hueco.hasItem()) {
            ItemStack objeto = hueco.getItem();
            copia = objeto.copy();
            if (indice == 1) {
                objeto.getItem().onCraftedBy(objeto, quien.level(), quien);
                if (!moveItemStackTo(objeto, 2, 38, true)) return ItemStack.EMPTY;
                hueco.onQuickCraft(objeto, copia);
            } else if (indice == 0) {
                if (!moveItemStackTo(objeto, 2, 38, false)) return ItemStack.EMPTY;
            } else if (nivel.getRecipeManager().getRecipeFor(ModuloRoles.TIPO_BANCO.get(), new SimpleContainer(objeto), nivel).isPresent()) {
                if (!moveItemStackTo(objeto, 0, 1, false)) return ItemStack.EMPTY;
            } else if (indice < 29) {
                if (!moveItemStackTo(objeto, 29, 38, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(objeto, 2, 29, false)) {
                return ItemStack.EMPTY;
            }
            if (objeto.isEmpty()) hueco.setByPlayer(ItemStack.EMPTY);
            hueco.setChanged();
            if (objeto.getCount() == copia.getCount()) return ItemStack.EMPTY;
            hueco.onTake(quien, objeto);
            broadcastChanges();
        }
        return copia;
    }

    @Override
    public void removed(Player quien) {
        super.removed(quien);
        resultado.removeItemNoUpdate(1);
        acceso.execute((n, p) -> clearContainer(quien, entrada));
    }
}
