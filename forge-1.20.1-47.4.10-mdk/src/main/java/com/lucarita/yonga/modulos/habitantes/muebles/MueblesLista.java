package com.lucarita.yonga.modulos.habitantes.muebles;

/**
 * La lista de los muebles y adornos de las casas (por épocas).
 *
 * ¡NO LO EDITES A MANO! Lo escribe herramientas/generar_muebles.py (ahí están sus dibujos, piezas y recetas).
 */
final class MueblesLista {

    private MueblesLista() {}

    static void registrar() {
        Muebles.mueble("piel_suelo", Muebles.tela(), new Muebles.Config(false, false, 0, -1, 0, "", new double[][]{{0, 0, 0, 16, 1, 16}}, null));
        Muebles.mueble("vasija_barro", Muebles.barro(), new Muebles.Config(false, false, 0, -1, 9, "", new double[][]{{3.9, 0, 3.9, 12.1, 14, 12.1}}, null));
        Muebles.mueble("cesto_mimbre", Muebles.madera(), new Muebles.Config(false, false, 0, -1, 9, "", new double[][]{{3, 0, 3, 13, 7, 13}}, null));
        Muebles.mueble("astas_pared", Muebles.madera(), new Muebles.Config(true, true, 0, -1, 0, "", new double[][]{{2, 7, 12, 14, 15, 16}}, null));
        Muebles.mueble("lena_apilada", Muebles.madera(), new Muebles.Config(false, false, 0, -1, 0, "", new double[][]{{0, 0, 0, 16, 10, 16}, {0, 10, 5, 16, 15, 11}}, null));
        Muebles.mueble("piedra_moler", Muebles.piedra(), new Muebles.Config(false, false, 0, -1, 0, "moler", new double[][]{{2, 0, 3, 14, 6, 13}}, null));
        Muebles.mueble("pintura_rupestre", Muebles.piedra(), new Muebles.Config(true, true, 0, -1, 0, "", new double[][]{{0, 0, 15, 16, 16, 16}}, null));
        Muebles.mueble("anfora", Muebles.barro(), new Muebles.Config(false, false, 0, -1, 9, "", new double[][]{{4, 0, 4, 12, 16, 12}}, null));
        Muebles.mueble("brasero", Muebles.metal(), new Muebles.Config(false, false, 13, -1, 0, "", new double[][]{{2, 0, 2, 14, 12, 14}}, null));
        Muebles.mueble("lucerna", Muebles.barro(), new Muebles.Config(false, false, 12, -1, 0, "", new double[][]{{5, 0, 2, 11, 3, 13}}, null));
        Muebles.mueble("lecho_romano", Muebles.madera(), new Muebles.Config(false, false, 0, 8, 0, "", new double[][]{{0, 0, 1, 16, 8, 15}, {14, 8, 1, 16, 11, 15}}, null));
        Muebles.mueble("mesa_marmol", Muebles.piedra(), new Muebles.Config(false, false, 0, -1, 0, "", new double[][]{{0, 0, 0, 16, 16, 16}}, null));
        Muebles.mueble("busto_marmol", Muebles.piedra(), new Muebles.Config(false, false, 0, -1, 0, "", new double[][]{{3, 0, 4, 13, 16, 12}}, null));
        Muebles.mueble("mesa_caballete", Muebles.madera(), new Muebles.Config(false, false, 0, -1, 0, "", new double[][]{{0, 0, 0, 16, 16, 16}}, null));
        Muebles.mueble("mesa_puesta", Muebles.madera(), new Muebles.Config(false, false, 0, -1, 0, "", new double[][]{{0, 0, 0, 16, 16, 16}}, null));
        Muebles.mueble("taburete", Muebles.madera(), new Muebles.Config(false, false, 0, 10, 0, "", new double[][]{{4, 0, 4, 12, 10, 12}}, null));
        Muebles.mueble("banco_madera", Muebles.madera(), new Muebles.Config(false, false, 0, 9, 0, "", new double[][]{{0, 0, 4, 16, 9, 12}}, null));
        Muebles.mueble("candelabro_hierro", Muebles.metal(), new Muebles.Config(false, false, 13, -1, 0, "", new double[][]{{5, 0, 5, 11, 16, 11}}, null));
        Muebles.mueble("tapiz", Muebles.tela(), new Muebles.Config(true, true, 0, -1, 0, "", new double[][]{{0, 0, 14, 16, 16, 16}}, null));
        Muebles.mueble("rueca", Muebles.madera(), new Muebles.Config(false, false, 0, -1, 0, "hilar", new double[][]{{2, 0, 3, 14, 16, 13}}, null));
        Muebles.mueble("silla_tapizada", Muebles.madera(), new Muebles.Config(false, false, 0, 10, 0, "", new double[][]{{3, 0, 3, 13, 10, 13}, {3, 10, 11.5, 13, 16, 13.5}}, null));
        Muebles.mueble("comoda", Muebles.madera(), new Muebles.Config(false, false, 0, -1, 27, "", new double[][]{{0, 0, 1, 16, 16, 16}}, null));
        Muebles.mueble("reloj_pared", Muebles.madera(), new Muebles.Config(true, true, 0, -1, 0, "reloj", new double[][]{{4, 2, 13, 12, 15, 16}}, null));
        Muebles.mueble("globo_terraqueo", Muebles.madera(), new Muebles.Config(false, false, 0, -1, 0, "globo", new double[][]{{4, 0, 4, 12, 13, 12}}, null));
        Muebles.mueble("candelabro_plata", Muebles.metal(), new Muebles.Config(false, false, 14, -1, 0, "", new double[][]{{3, 0, 6, 13, 14, 10}}, null));
        Muebles.mueble("cuadro_paisaje", Muebles.madera(), new Muebles.Config(true, true, 0, -1, 0, "", new double[][]{{1, 2, 15, 15, 14, 16}}, null));
        Muebles.mueble("escritorio", Muebles.madera(), new Muebles.Config(false, false, 0, -1, 9, "", new double[][]{{0, 0, 0, 16, 16, 16}}, null));
        Muebles.mueble("estufa_hierro", Muebles.metal(), new Muebles.Config(false, false, 10, -1, 0, "", new double[][]{{1, 0, 2, 15, 16, 15}}, null));
        Muebles.mueble("lampara_gas", Muebles.metal(), new Muebles.Config(true, true, 14, -1, 0, "", new double[][]{{5, 4, 8, 11, 11, 16}}, null));
        Muebles.mueble("maquina_coser", Muebles.metal(), new Muebles.Config(false, false, 0, -1, 0, "coser", new double[][]{{0, 0, 2, 16, 16, 14}}, null));
        Muebles.mueble("gramofono", Muebles.madera(), new Muebles.Config(false, false, 0, -1, 1, "gramofono", new double[][]{{3, 0, 3, 13, 16, 13}}, null));
        Muebles.mueble("perchero", Muebles.madera(), new Muebles.Config(false, false, 0, -1, 0, "", new double[][]{{5, 0, 5, 11, 16, 11}}, null));
        Muebles.mueble("radiador", Muebles.metal(), new Muebles.Config(true, false, 0, -1, 0, "", new double[][]{{1, 0, 12, 15, 14, 16}}, null));
        Muebles.mueble("sofa", Muebles.tela(), new Muebles.Config(false, false, 0, 9, 0, "", new double[][]{{0, 0, 2, 16, 9, 16}, {0, 9, 13, 16, 16, 16}}, null));
        Muebles.mueble("television", Muebles.metal(), new Muebles.Config(false, false, 6, -1, 0, "tele", new double[][]{{0, 0, 3, 16, 16, 15}}, null));
        Muebles.mueble("lampara_pie", Muebles.metal(), new Muebles.Config(false, false, 14, -1, 0, "lampara", new double[][]{{5, 0, 5, 11, 16, 11}}, new double[][]{{7, 0, 7, 9, 6, 9}, {4, 6, 4, 12, 13, 12}}));
        Muebles.mueble("nevera", Muebles.metal(), new Muebles.Config(false, false, 0, -1, 27, "nevera", new double[][]{{1, 0, 1, 15, 16, 15}}, null));
        Muebles.mueble("mesa_centro", Muebles.madera(), new Muebles.Config(false, false, 0, -1, 0, "", new double[][]{{1, 0, 2, 15, 8, 14}}, null));
        Muebles.mueble("planta_maceta", Muebles.barro(), new Muebles.Config(false, false, 0, -1, 0, "", new double[][]{{4, 0, 4, 12, 16, 12}}, null));
        Muebles.mueble("mesa_moderna", Muebles.madera(), new Muebles.Config(false, false, 0, -1, 0, "", new double[][]{{0, 0, 0, 16, 16, 16}}, null));
        Muebles.mueble("ordenador", Muebles.metal(), new Muebles.Config(false, false, 6, -1, 0, "ordenador", new double[][]{{1, 0, 2, 15, 12, 13}}, null));
    }
}
