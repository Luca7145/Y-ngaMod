"""Genera los muebles y adornos de las casas de las aldeas (uno o varios por época): sus modelos 3D, texturas,
estados, objetos, botines, nombres, recetas del banco de ingeniero y la lista Java que los registra.

Uso (desde la carpeta del repositorio):  python herramientas/generar_muebles.py

Cada mueble se dibuja con cajas (en píxeles, de 0 a 16) mirando al NORTE: su frente está en z = 0.
Los de pared tienen la espalda en z = 16 (se pegan a la pared en la que pinchas).
Texturas: "m:xxx" = minecraft:block/xxx, "y:xxx" = yonga:block/xxx, "d:xxx" = la nuestra (mueble_xxx)."""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dibujos_muebles as D

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.join(AQUI, '..', 'forge-1.20.1-47.4.10-mdk', 'src', 'main')
ASSETS = os.path.join(RAIZ, 'resources', 'assets', 'yonga')
DATA = os.path.join(RAIZ, 'resources', 'data', 'yonga')
JAVA = os.path.join(RAIZ, 'java', 'com', 'lucarita', 'yonga', 'modulos', 'habitantes', 'muebles', 'MueblesLista.java')

def tex(t):
    if t.startswith('m:'): return 'minecraft:block/' + t[2:]
    if t.startswith('y:'): return 'yonga:block/' + t[2:]
    if t.startswith('d:'): return 'yonga:block/mueble_' + t[2:]
    return t

def C(x1, y1, z1, x2, y2, z2, t, **caras):
    """Una caja. caras: north/south/east/west/up/down = textura (o (textura, [u1, v1, u2, v2])); None la quita."""
    return (x1, y1, z1, x2, y2, z2, t, caras)

def patas(x1, z1, x2, z2, alto, grosor, t):
    g = grosor
    return [C(x1, 0, z1, x1 + g, alto, z1 + g, t), C(x2 - g, 0, z1, x2, alto, z1 + g, t),
            C(x1, 0, z2 - g, x1 + g, alto, z2, t), C(x2 - g, 0, z2 - g, x2, alto, z2, t)]

# ============================ LOS MUEBLES ============================
# nombre: (época, material, luz, de pared, se atraviesa, entrada del banco, cajas, nombre en español, en inglés)
M = {}
def mueble(nombre, epoca, material, cajas, es, en, entrada, luz=0, pared=False, pasable=False, forma=None,
           asiento=-1, huecos=0, accion="", arriba=None, forma_arriba=None):
    """asiento: altura (en píxeles) a la que te sientas; huecos: 9 o 27 para guardar cosas; accion: qué hace al usarlo
    (reloj, globo, moler, hilar, coser, gramofono, tele, ordenador, lampara, nevera); arriba: cajas de la mitad de
    arriba (muebles de dos bloques de alto). Los que dan luz se encienden y apagan con clic derecho."""
    M[nombre] = dict(epoca=epoca, material=material, cajas=cajas, es=es, en=en, entrada=entrada, luz=luz,
                     pared=pared, pasable=pasable, forma=forma, asiento=asiento, huecos=huecos, accion=accion,
                     arriba=arriba, forma_arriba=forma_arriba)

# ---------- PREHISTORIA ----------
mueble("piel_suelo", "prehistorica", "tela",
       [C(0, 0, 0, 16, 0.5, 16, "d:piel", north=None, south=None, east=None, west=None)],
       "Piel en el suelo", "Fur Rug", "minecraft:leather", forma=[(0, 0, 0, 16, 1, 16)])
mueble("vasija_barro", "prehistorica", "barro",
       [C(4, 0, 4, 12, 9, 12, "m:terracotta"), C(3.9, 5, 3.9, 12.1, 6.5, 12.1, "m:brown_terracotta"),
        C(5, 9, 5, 11, 11, 11, "m:terracotta"), C(6, 11, 6, 10, 13, 10, "m:terracotta"),
        C(5, 13, 5, 11, 14, 11, "m:terracotta", up="m:black_terracotta")],
       "Vasija de barro", "Clay Pot", "minecraft:clay_ball", huecos=9)
mueble("cesto_mimbre", "prehistorica", "madera",
       [C(3, 0, 3, 13, 1, 13, "d:mimbre"), C(3, 1, 3, 13, 7, 4, "d:mimbre"), C(3, 1, 12, 13, 7, 13, "d:mimbre"),
        C(3, 1, 4, 4, 7, 12, "d:mimbre"), C(12, 1, 4, 13, 7, 12, "d:mimbre"),
        C(4, 1, 4, 12, 5.5, 12, "m:hay_block_top")],
       "Cesto de mimbre", "Wicker Basket", "minecraft:sugar_cane", forma=[(3, 0, 3, 13, 7, 13)], huecos=9)
ASTA = "m:bone_block_side"
mueble("astas_pared", "prehistorica", "madera",
       [C(6, 7, 15, 10, 13, 16, "m:oak_log"), C(6.5, 8, 12, 9.5, 11, 15, ASTA),
        C(2, 11, 13, 6.5, 12, 14, ASTA), C(2, 12, 13, 3, 15, 14, ASTA), C(4, 12, 13, 5, 14, 14, ASTA),
        C(9.5, 11, 13, 14, 12, 14, ASTA), C(13, 12, 13, 14, 15, 14, ASTA), C(11, 12, 13, 12, 14, 14, ASTA)],
       "Trofeo de astas", "Antler Trophy", "minecraft:bone", pared=True, pasable=True, forma=[(2, 7, 12, 14, 15, 16)])
LOG = dict(east="m:oak_log_top", west="m:oak_log_top")
mueble("lena_apilada", "prehistorica", "madera",
       [C(0, 0, 0.5, 16, 5, 5.5, "m:oak_log", **LOG), C(0, 0, 5.5, 16, 5, 10.5, "m:oak_log", **LOG),
        C(0, 0, 10.5, 16, 5, 15.5, "m:oak_log", **LOG), C(0, 5, 3, 16, 10, 8, "m:oak_log", **LOG),
        C(0, 5, 8, 16, 10, 13, "m:oak_log", **LOG), C(0, 10, 5.5, 16, 15, 10.5, "m:oak_log", **LOG)],
       "Pila de leña", "Firewood Stack", "#minecraft:logs", forma=[(0, 0, 0, 16, 10, 16), (0, 10, 5, 16, 15, 11)])
mueble("piedra_moler", "prehistorica", "piedra",
       [C(2, 0, 3, 14, 4, 13, "m:stone"), C(4, 4, 6, 12, 6, 8, "m:smooth_stone"),
        C(9, 4, 9, 12, 5, 12, "m:white_concrete_powder")],
       "Piedra de moler", "Grinding Stone", "minecraft:cobblestone", accion="moler")
mueble("pintura_rupestre", "prehistorica", "piedra",
       [C(0, 0, 15.9, 16, 16, 16, "d:rupestre", south=None, east=None, west=None, up=None, down=None)],
       "Pinturas rupestres", "Cave Paintings", "minecraft:red_dye", pared=True, pasable=True, forma=[(0, 0, 15, 16, 16, 16)])

# ---------- ANTIGUA ----------
TC = "m:terracotta"
mueble("anfora", "antigua", "barro",
       [C(6, 0, 6, 10, 1, 10, TC), C(5, 1, 5, 11, 3, 11, TC), C(4, 3, 4, 12, 10, 12, TC),
        C(3.9, 6, 3.9, 12.1, 8, 12.1, "m:black_terracotta"), C(5, 10, 5, 11, 12, 11, TC),
        C(6.5, 12, 6.5, 9.5, 15, 9.5, TC), C(6, 15, 6, 10, 16, 10, TC, up="m:black_terracotta"),
        C(4.5, 11, 7.5, 5.5, 14.5, 8.5, TC), C(5.5, 13.5, 7.5, 6.5, 14.5, 8.5, TC),
        C(10.5, 11, 7.5, 11.5, 14.5, 8.5, TC), C(9.5, 13.5, 7.5, 10.5, 14.5, 8.5, TC)],
       "Ánfora", "Amphora", "minecraft:terracotta", forma=[(4, 0, 4, 12, 16, 12)], huecos=9)
BR = "m:cut_copper"
mueble("brasero", "antigua", "metal",
       [C(3, 0, 3, 4, 8, 4, BR), C(12, 0, 3, 13, 8, 4, BR), C(3, 0, 12, 4, 8, 13, BR), C(12, 0, 12, 13, 8, 13, BR),
        C(2, 8, 2, 14, 11, 14, BR), C(3, 11, 3, 13, 11.5, 13, "d:brasas")],
       "Brasero", "Brazier", "minecraft:copper_ingot", luz=13, forma=[(2, 0, 2, 14, 12, 14)])
mueble("lucerna", "antigua", "barro",
       [C(5, 0, 5, 11, 2, 12, TC, up="m:brown_terracotta"), C(7, 0, 2, 9, 1.5, 5, TC),
        C(7.5, 1.5, 2.5, 8.5, 3.5, 3.5, "d:llama"), C(7.5, 2, 12, 8.5, 3, 13.5, TC)],
       "Lucerna", "Oil Lamp", "minecraft:terracotta", luz=12, forma=[(5, 0, 2, 11, 3, 13)])
mueble("lecho_romano", "antigua", "madera",
       patas(1, 2, 15, 14, 4, 2, "m:dark_oak_planks") +
       [C(0, 4, 1, 16, 6, 15, "m:dark_oak_planks"), C(0, 6, 1, 16, 8, 15, "m:red_wool"),
        C(14, 6, 1, 16, 11, 15, "m:dark_oak_planks"), C(10, 8, 3, 14, 10, 13, "m:white_wool")],
       "Lecho", "Reclining Couch", "minecraft:red_wool", forma=[(0, 0, 1, 16, 8, 15), (14, 8, 1, 16, 11, 15)], asiento=8)
mueble("mesa_marmol", "antigua", "piedra",
       [C(4, 0, 4, 12, 2, 12, "y:marmol"), C(6, 2, 6, 10, 14, 10, "y:columna_marmol"),
        C(0, 14, 0, 16, 16, 16, "y:marmol")],
       "Mesa de mármol", "Marble Table", "minecraft:calcite")
mueble("busto_marmol", "antigua", "piedra",
       [C(4, 0, 4, 12, 6, 12, "y:marmol"), C(3, 6, 5, 13, 10, 11, "y:marmol"), C(6.5, 10, 6.5, 9.5, 11, 9.5, "y:marmol"),
        C(5.5, 11, 5.5, 10.5, 16, 10.5, "y:marmol"), C(7.5, 12.5, 4.8, 8.5, 14, 5.5, "y:marmol")],
       "Busto de mármol", "Marble Bust", "minecraft:calcite", forma=[(3, 0, 4, 13, 16, 12)])

# ---------- MEDIEVAL ----------
PIN = "m:spruce_planks"; TRONCO = "m:stripped_spruce_log"
MESA_CABALLETE = [C(2, 0, 2, 4, 1, 14, TRONCO), C(12, 0, 2, 14, 1, 14, TRONCO), C(2, 1, 6, 4, 14, 10, TRONCO),
                  C(12, 1, 6, 14, 14, 10, TRONCO), C(4, 5, 7, 12, 7, 9, TRONCO), C(0, 14, 0, 16, 16, 16, PIN)]
mueble("mesa_caballete", "medieval", "madera", MESA_CABALLETE, "Mesa de caballete", "Trestle Table", "#minecraft:planks")
mueble("mesa_puesta", "medieval", "madera",
       MESA_CABALLETE + [C(2, 16, 3, 6, 17.5, 7, PIN, up="m:dark_oak_planks"), C(10, 16, 9, 13, 20, 12, TC),
                         C(5, 16, 10, 10, 18, 13, "d:hogaza"), C(11, 16, 3, 14, 16.5, 6, "m:white_wool")],
       "Mesa puesta", "Set Table", "minecraft:bread")
mueble("taburete", "medieval", "madera",
       patas(4.5, 4.5, 11.5, 11.5, 8, 1.5, TRONCO) + [C(4, 8, 4, 12, 10, 12, PIN)],
       "Taburete", "Stool", "#minecraft:planks", forma=[(4, 0, 4, 12, 10, 12)], asiento=10)
mueble("banco_madera", "medieval", "madera",
       [C(1, 0, 5, 3, 7, 11, TRONCO), C(13, 0, 5, 15, 7, 11, TRONCO), C(0, 7, 4, 16, 9, 12, PIN)],
       "Banco de madera", "Wooden Bench", "#minecraft:planks", forma=[(0, 0, 4, 16, 9, 12)], asiento=9)
HF = "d:hierro_forjado"
mueble("candelabro_hierro", "medieval", "metal",
       [C(5, 0, 5, 11, 1, 11, HF), C(7.5, 1, 7.5, 8.5, 13, 8.5, HF), C(5, 13, 5, 11, 14, 11, HF),
        C(6.5, 14, 6.5, 9.5, 19, 9.5, "m:white_wool"), C(7.5, 19, 7.5, 8.5, 21, 8.5, "d:llama")],
       "Candelero de hierro", "Iron Candlestand", "minecraft:iron_ingot", luz=13, forma=[(5, 0, 5, 11, 16, 11)])
mueble("tapiz", "medieval", "tela",
       [C(0, 15, 14, 16, 16, 15, "m:dark_oak_planks"),
        C(1, 0, 15, 15, 15, 15.5, "d:tapiz", north=("d:tapiz", [0, 0, 16, 16]), south=("d:tapiz", [0, 0, 16, 16]))],
       "Tapiz", "Tapestry", "minecraft:red_wool", pared=True, pasable=True, forma=[(0, 0, 14, 16, 16, 16)])
mueble("rueca", "medieval", "madera",
       patas(3, 4, 13, 12, 3, 1.5, TRONCO) + [C(2, 3, 3, 14, 5, 13, PIN),
        C(4, 5, 8, 12, 6, 9, TRONCO), C(2, 5, 7.5, 14, 17, 8.5, "d:rueca", north=("d:rueca", [0, 0, 16, 16]),
                                         south=("d:rueca", [0, 0, 16, 16]), east=None, west=None, up=None, down=None),
        C(7.5, 10.5, 6, 8.5, 11.5, 10, TRONCO), C(12.5, 5, 10, 13.5, 15, 11, TRONCO), C(12, 12, 9.5, 14, 15, 11.5, "m:white_wool")],
       "Rueca", "Spinning Wheel", "minecraft:string", forma=[(2, 0, 3, 14, 16, 13)], accion="hilar")

# ---------- MODERNA (siglos XVI-XVIII) ----------
OSC = "m:dark_oak_planks"
mueble("silla_tapizada", "moderna", "madera",
       patas(3, 3, 13, 13, 7, 1.5, OSC) + [C(3, 7, 3, 13, 9, 13, OSC), C(3.5, 9, 3.5, 12.5, 10, 12, "y:damasco"),
        C(3, 9, 12, 13, 20, 13.5, OSC), C(4, 10, 11.5, 12, 19, 12, "y:damasco")],
       "Silla tapizada", "Upholstered Chair", "minecraft:dark_oak_planks",
       forma=[(3, 0, 3, 13, 10, 13), (3, 10, 11.5, 13, 16, 13.5)], asiento=10)
mueble("comoda", "moderna", "madera",
       [C(0, 0, 2, 16, 14, 16, OSC, north=("d:comoda_frente", [0, 1, 16, 15])), C(0, 14, 1, 16, 16, 16, "y:marmol")],
       "Cómoda", "Chest of Drawers", "minecraft:dark_oak_planks", forma=[(0, 0, 1, 16, 16, 16)], huecos=27)
mueble("reloj_pared", "moderna", "madera",
       [C(5, 2, 14, 11, 14, 16, OSC, north=("d:reloj_pared", [5, 1, 11, 13])), C(4.5, 14, 13.5, 11.5, 15, 16, OSC)],
       "Reloj de pared", "Wall Clock", "minecraft:clock", pared=True, pasable=True, forma=[(4, 2, 13, 12, 15, 16)], accion="reloj")
mueble("globo_terraqueo", "moderna", "madera",
       [C(5, 0, 5, 11, 1, 11, OSC), C(7.5, 1, 7.5, 8.5, 4, 8.5, "m:gold_block"), C(4, 4, 4, 12, 12, 12, "d:globo"),
        C(7.5, 12, 7.5, 8.5, 13, 8.5, "m:gold_block"), C(7.6, 3, 3.5, 8.4, 13, 4, "m:gold_block"),
        C(7.6, 3, 12, 8.4, 13, 12.5, "m:gold_block")],
       "Globo terráqueo", "Globe", "minecraft:map", forma=[(4, 0, 4, 12, 13, 12)], accion="globo")
PLATA = "m:iron_block"
mueble("candelabro_plata", "moderna", "metal",
       [C(6, 0, 6, 10, 1, 10, PLATA), C(7.5, 1, 7.5, 8.5, 8, 8.5, PLATA), C(3, 7, 7.5, 13, 8, 8.5, PLATA),
        C(3, 8, 7, 5, 12, 9, "m:white_concrete"), C(7, 8, 7, 9, 13, 9, "m:white_concrete"), C(11, 8, 7, 13, 12, 9, "m:white_concrete"),
        C(3.5, 12, 7.5, 4.5, 13.5, 8.5, "d:llama"), C(7.5, 13, 7.5, 8.5, 14.5, 8.5, "d:llama"), C(11.5, 12, 7.5, 12.5, 13.5, 8.5, "d:llama")],
       "Candelabro de plata", "Silver Candelabrum", "minecraft:iron_ingot", luz=14, forma=[(3, 0, 6, 13, 14, 10)])
mueble("cuadro_paisaje", "moderna", "madera",
       [C(1, 2, 15, 15, 14, 16, "m:gold_block", north=("d:cuadro", [0, 2, 16, 14]))],
       "Cuadro de paisaje", "Landscape Painting", "minecraft:painting", pared=True, pasable=True, forma=[(1, 2, 15, 15, 14, 16)])
mueble("escritorio", "moderna", "madera",
       [C(0, 0, 1, 5, 14, 15, OSC, north=("d:comoda_frente", [0, 1, 16, 15])), C(14, 0, 1, 16, 14, 3, OSC),
        C(14, 0, 13, 16, 14, 15, OSC), C(0, 14, 0, 16, 16, 16, OSC), C(4, 16, 4, 10, 16.3, 10, "d:papel"),
        C(11, 16, 9, 13, 18, 11, "m:black_concrete"), C(12, 18, 10, 12.5, 22, 10.5, "m:white_wool")],
       "Escritorio", "Writing Desk", "minecraft:dark_oak_planks", forma=[(0, 0, 0, 16, 16, 16)], huecos=9)

# ---------- INDUSTRIAL ----------
HU = "d:hierro_fundido"
mueble("estufa_hierro", "industrial", "metal",
       patas(2, 3, 14, 14, 2, 2, HU) + [C(2, 2, 3, 14, 12, 14, HU, north="d:estufa_frente"),
        C(1, 12, 2, 15, 13, 15, HU), C(6, 13, 10, 10, 16, 14, HU)],
       "Estufa de hierro", "Cast Iron Stove", "minecraft:iron_block", luz=10, forma=[(1, 0, 2, 15, 16, 15)])
mueble("lampara_gas", "industrial", "metal",
       [C(7, 10, 12, 9, 11, 16, "m:gold_block"), C(6.5, 4, 9.5, 9.5, 5, 12.5, "m:black_concrete"),
        C(6, 5, 9, 10, 10, 13, "d:farol_cristal"), C(5.5, 10, 8.5, 10.5, 11, 13.5, "m:black_concrete")],
       "Farol de gas", "Gas Lamp", "minecraft:lantern", luz=14, pared=True, pasable=True, forma=[(5, 4, 8, 11, 11, 16)])
mueble("maquina_coser", "industrial", "metal",
       [C(1, 0, 3, 3, 14, 13, HF), C(13, 0, 3, 15, 14, 13, HF), C(3, 1, 5, 13, 2, 11, HF),
        C(0, 14, 2, 16, 16, 14, "m:oak_planks"), C(3, 16, 6, 13, 17, 10, "m:black_concrete"),
        C(10, 17, 7, 12, 22, 9, "m:black_concrete"), C(4, 21, 7, 12, 23, 9, "m:black_concrete"),
        C(4, 18, 7, 6, 21, 9, "m:black_concrete"), C(12, 19, 6.5, 13, 21, 9.5, "m:gold_block")],
       "Máquina de coser", "Sewing Machine", "minecraft:iron_ingot", forma=[(0, 0, 2, 16, 16, 14)], accion="coser")
mueble("gramofono", "industrial", "madera",
       [C(3, 0, 3, 13, 5, 13, OSC), C(4, 5, 4, 12, 5.5, 12, "m:black_concrete"), C(8, 5, 11, 9, 9, 12, "m:gold_block"),
        C(7, 8, 9, 10, 11, 12, "m:gold_block"), C(6, 10, 6, 11, 13, 11, "m:gold_block"),
        C(4, 12, 3, 13, 16, 9, "m:gold_block", north="m:black_concrete")],
       "Gramófono", "Gramophone", "minecraft:jukebox", forma=[(3, 0, 3, 13, 16, 13)], huecos=1, accion="gramofono")
mueble("perchero", "industrial", "madera",
       [C(5, 0, 5, 11, 1, 11, OSC), C(7.5, 1, 7.5, 8.5, 16, 8.5, OSC), C(5, 14, 7.5, 11, 15, 8.5, OSC),
        C(7.5, 14, 5, 8.5, 15, 11, OSC), C(5, 5, 8.5, 11, 14, 10, "m:brown_wool"),
        C(5.5, 16, 5.5, 10.5, 19, 10.5, "m:black_wool"), C(4.5, 16, 4.5, 11.5, 16.5, 11.5, "m:black_wool")],
       "Perchero", "Coat Stand", "minecraft:dark_oak_planks", forma=[(5, 0, 5, 11, 16, 11)])
RD = "d:radiador"
mueble("radiador", "industrial", "metal",
       [C(1 + 2 * i, 1, 12, 2.5 + 2 * i, 13, 15, RD) for i in range(7)] +
       [C(1, 13, 13, 15, 14, 14, RD), C(1, 0, 12, 2, 1, 15, HF), C(13, 0, 12, 14, 1, 15, HF)],
       "Radiador", "Radiator", "minecraft:iron_ingot", pared=True, forma=[(1, 0, 12, 15, 14, 16)])

# ---------- CONTEMPORÁNEA ----------
TG = "d:tela_gris"
mueble("sofa", "contemporanea", "tela",
       [C(1, 0, 3, 2, 2, 4, "m:black_concrete"), C(14, 0, 3, 15, 2, 4, "m:black_concrete"),
        C(1, 0, 14, 2, 2, 15, "m:black_concrete"), C(14, 0, 14, 15, 2, 15, "m:black_concrete"),
        C(0, 2, 2, 16, 7, 16, TG), C(0, 7, 2, 16, 9, 13, "d:cojin"), C(0, 7, 13, 16, 16, 16, TG)],
       "Sofá", "Sofa", "minecraft:gray_wool", forma=[(0, 0, 2, 16, 9, 16), (0, 9, 13, 16, 16, 16)], asiento=9)
mueble("television", "contemporanea", "metal",
       [C(0, 0, 3, 16, 6, 15, "m:white_concrete"), C(6, 6, 9, 10, 7, 12, "m:black_concrete"),
        C(7.5, 7, 10, 8.5, 8, 11, "m:black_concrete"),
        C(0.5, 8, 9.5, 15.5, 17, 10.5, "m:black_concrete", north=("d:pantalla", [0, 0, 16, 16]))],
       "Televisor", "Television", "minecraft:redstone", forma=[(0, 0, 3, 16, 16, 15)], luz=6, accion="tele")
mueble("lampara_pie", "contemporanea", "metal",
       [C(5, 0, 5, 11, 1, 11, "m:black_concrete"), C(7.5, 1, 7.5, 8.5, 16, 8.5, "m:black_concrete")],
       "Lámpara de pie", "Floor Lamp", "minecraft:glowstone", luz=14, forma=[(5, 0, 5, 11, 16, 11)], accion="lampara",
       arriba=[C(7.5, 0, 7.5, 8.5, 7, 8.5, "m:black_concrete"), C(6.5, 5, 6.5, 9.5, 6, 9.5, "m:black_concrete"),
               C(4, 6, 4, 12, 13, 12, "d:pantalla_lampara", down="d:farol_cristal")],
       forma_arriba=[(7, 0, 7, 9, 6, 9), (4, 6, 4, 12, 13, 12)])
mueble("nevera", "contemporanea", "metal",
       [C(1, 0, 1, 15, 16, 15, "m:white_concrete", north="d:nevera_frente")],
       "Nevera", "Fridge", "minecraft:iron_block", huecos=27, accion="nevera")
mueble("mesa_centro", "contemporanea", "madera",
       [C(2, 0, 3, 3, 6, 4, "m:black_concrete"), C(13, 0, 3, 14, 6, 4, "m:black_concrete"),
        C(2, 0, 12, 3, 6, 13, "m:black_concrete"), C(13, 0, 12, 14, 6, 13, "m:black_concrete"),
        C(1, 6, 2, 15, 8, 14, "m:birch_planks"), C(4, 8, 5, 9, 8.3, 9, "d:papel"),
        C(10, 8, 8, 12, 11, 10, "m:white_terracotta"), C(9.5, 11, 7.5, 12.5, 13, 10.5, "m:azalea_leaves")],
       "Mesa de centro", "Coffee Table", "minecraft:birch_planks", forma=[(1, 0, 2, 15, 8, 14)])
mueble("planta_maceta", "contemporanea", "barro",
       [C(5, 0, 5, 11, 6, 11, "m:white_terracotta"), C(5.5, 6, 5.5, 10.5, 6.1, 10.5, "m:rooted_dirt"),
        C(7.5, 6, 7.5, 8.5, 11, 8.5, "m:oak_log"), C(3, 10, 3, 13, 20, 13, "m:azalea_leaves"),
        C(4.5, 20, 4.5, 11.5, 25, 11.5, "m:azalea_leaves")],
       "Planta en maceta", "Potted Plant", "minecraft:azalea", forma=[(4, 0, 4, 12, 16, 12)])
mueble("mesa_moderna", "contemporanea", "madera",
       patas(1, 1, 15, 15, 14, 1, "m:black_concrete") + [C(0, 14, 0, 16, 16, 16, "m:white_concrete")],
       "Mesa moderna", "Modern Desk", "minecraft:white_concrete")
mueble("ordenador", "contemporanea", "metal",
       [C(6, 0, 10, 10, 0.5, 13, "m:black_concrete"), C(7.5, 0.5, 11, 8.5, 3, 12, "m:black_concrete"),
        C(1, 3, 10, 15, 12, 11, "m:black_concrete", north=("d:pantalla", [0, 0, 16, 16])),
        C(2, 0, 2, 12, 0.7, 6, "m:black_concrete", up="d:teclado"), C(13, 0, 3, 15, 0.7, 6, "m:black_concrete")],
       "Ordenador", "Computer", "minecraft:redstone", forma=[(1, 0, 2, 15, 12, 13)], luz=6, accion="ordenador")

# ======================= DATOS DE FORGE =======================
RANGO = {"prehistorica": "todos", "antigua": "novato", "medieval": "aprendiz", "moderna": "oficial",
         "industrial": "experto", "contemporanea": "maestro"}
HERRAMIENTA = {"piedra": "pickaxe", "barro": "pickaxe", "metal": "pickaxe", "madera": "axe"}
PROPIEDADES = {"madera": "madera()", "piedra": "piedra()", "barro": "barro()", "metal": "metal()", "tela": "tela()"}

def escribir(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, 'w', encoding='utf-8') as f:
        json.dump(datos, f, indent=2, ensure_ascii=False)

UV_AUTO = {
    "north": lambda x1, y1, z1, x2, y2, z2: [16 - x2, 16 - y2, 16 - x1, 16 - y1],
    "south": lambda x1, y1, z1, x2, y2, z2: [x1, 16 - y2, x2, 16 - y1],
    "east": lambda x1, y1, z1, x2, y2, z2: [16 - z2, 16 - y2, 16 - z1, 16 - y1],
    "west": lambda x1, y1, z1, x2, y2, z2: [z1, 16 - y2, z2, 16 - y1],
    "up": lambda x1, y1, z1, x2, y2, z2: [x1, z1, x2, z2],
    "down": lambda x1, y1, z1, x2, y2, z2: [x1, 16 - z2, x2, 16 - z1],
}

def ajustar(uv):
    """Las caras que salen del bloque (y > 16) se quedan dentro de la textura."""
    u1, v1, u2, v2 = uv
    if min(v1, v2) < 0:
        d = -min(v1, v2); v1 += d; v2 += d
    if max(v1, v2) > 16:
        d = max(v1, v2) - 16; v1 -= d; v2 -= d
    return [round(u1, 3), round(v1, 3), round(u2, 3), round(v2, 3)]

APAGADO = {"d:llama": "d:mecha", "d:brasas": "d:brasas_apagadas", "d:farol_cristal": "d:farol_apagado",
           "d:estufa_frente": "d:estufa_apagada", "d:pantalla": "d:pantalla_apagada"}

def _apagar(cajas):
    def ap(v):
        if isinstance(v, tuple): return (APAGADO.get(v[0], v[0]), v[1])
        return APAGADO.get(v, v) if v else v
    return [(x1, y1, z1, x2, y2, z2, ap(t), {k: ap(v) for k, v in caras.items()}) for (x1, y1, z1, x2, y2, z2, t, caras) in cajas]

def modelo(nombre, d):
    giro = {"north": 0, "east": 90, "south": 180, "west": 270}
    encendible = d["luz"] > 0
    mitades = [("lower", d["cajas"], nombre), ("upper", d["arriba"], nombre + "_arriba")] if d["arriba"] else [(None, d["cajas"], nombre)]
    variantes = {}
    for mitad, cajas, archivo in mitades:
        modelo_cajas(archivo, cajas)
        if encendible:
            modelo_cajas(archivo + "_apagado", _apagar(cajas))
        for k, v in giro.items():
            for lit in ((True, False) if encendible else (None,)):
                clave = [f"facing={k}"]
                if mitad: clave.append(f"half={mitad}")
                if lit is not None: clave.append("lit=" + str(lit).lower())
                m = "yonga:block/" + archivo + ("_apagado" if lit is False else "")
                variantes[",".join(sorted(clave))] = {"model": m, "y": v} if v else {"model": m}
    escribir(os.path.join(ASSETS, 'blockstates', nombre + '.json'), {"variants": variantes})
    datos_mueble(nombre, d)

def modelo_cajas(nombre, cajas):
    claves, texturas = {}, {}
    def clave(t):
        t = tex(t)
        if t not in claves:
            claves[t] = "t%d" % len(claves); texturas[claves[t]] = t
        return "#" + claves[t]
    elementos = []
    for (x1, y1, z1, x2, y2, z2, t, caras) in cajas:
        f = {}
        for cara, uvf in UV_AUTO.items():
            valor = caras.get(cara, t)
            if valor is None: continue
            if isinstance(valor, tuple):
                f[cara] = {"texture": clave(valor[0]), "uv": valor[1]}
            else:
                f[cara] = {"texture": clave(valor), "uv": ajustar(uvf(x1, y1, z1, x2, y2, z2))}
        elementos.append({"from": [x1, y1, z1], "to": [x2, y2, z2], "faces": f})
    texturas["particle"] = tex(cajas[0][6])
    escribir(os.path.join(ASSETS, 'models', 'block', nombre + '.json'),
             {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "ambientocclusion": False,
              "textures": texturas, "elements": elementos})

def datos_mueble(nombre, d):
    if d["arriba"]:      # el objeto de los de dos alturas: los dos bloques, bajados para que quepan
        cajas = [(x1, y1 / 2, z1, x2, y2 / 2, z2, t, c) for (x1, y1, z1, x2, y2, z2, t, c) in d["cajas"]] + \
                [(x1, 8 + y1 / 2, z1, x2, 8 + y2 / 2, z2, t, c) for (x1, y1, z1, x2, y2, z2, t, c) in d["arriba"]]
        modelo_cajas(os.path.join("..", "item", nombre), cajas)
    else:
        escribir(os.path.join(ASSETS, 'models', 'item', nombre + '.json'), {"parent": "yonga:block/" + nombre})
    escribir(os.path.join(DATA, 'loot_tables', 'blocks', nombre + '.json'),
             {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "yonga:" + nombre}],
                                                    "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    entrada = d["entrada"]
    ing = {"tag": entrada[1:]} if entrada.startswith('#') else {"item": entrada}
    escribir(os.path.join(DATA, 'recipes', 'banco', 'mueble_' + nombre + '.json'),
             {"conditions": [{"type": "yonga:modulo_activo", "modulo": "roles"}, {"type": "yonga:modulo_activo", "modulo": "habitantes"}],
              "type": "yonga:banco_ingeniero", "ingredient": ing, "result": "yonga:" + nombre, "count": 1, "rango": RANGO[d["epoca"]]})

def forma_de(d):
    if d["forma"]: return d["forma"]
    xs = [c[:6] for c in d["cajas"]]
    return [(min(c[0] for c in xs), 0, min(c[2] for c in xs), max(c[3] for c in xs), min(16, max(c[4] for c in xs)), max(c[5] for c in xs))]

def num(v):
    return ("%g" % v)

def java():
    lineas = []
    for nombre, d in M.items():
        piezas = ", ".join("{" + ", ".join(num(v) for v in f) + "}" for f in forma_de(d))
        arriba = "null"
        if d["arriba"]:
            arriba = "new double[][]{" + ", ".join("{" + ", ".join(num(v) for v in f) + "}" for f in d["forma_arriba"]) + "}"
        lineas.append(f'        Muebles.mueble("{nombre}", Muebles.{PROPIEDADES[d["material"]]}, new Muebles.Config({str(d["pared"]).lower()}, '
                      f'{str(d["pasable"]).lower()}, {d["luz"]}, {num(d["asiento"])}, {d["huecos"]}, "{d["accion"]}", '
                      f'new double[][]{{{piezas}}}, {arriba}));')
    codigo = """package com.lucarita.yonga.modulos.habitantes.muebles;

/**
 * La lista de los muebles y adornos de las casas (por épocas).
 *
 * ¡NO LO EDITES A MANO! Lo escribe herramientas/generar_muebles.py (ahí están sus dibujos, piezas y recetas).
 */
final class MueblesLista {

    private MueblesLista() {}

    static void registrar() {
""" + "\n".join(lineas) + """
    }
}
"""
    with open(JAVA, 'w', encoding='utf-8') as f:
        f.write(codigo)

def main():
    for nombre, dibujo in D.DIBUJOS.items():
        ruta = os.path.join(ASSETS, 'textures', 'block', 'mueble_' + nombre + '.png')
        os.makedirs(os.path.dirname(ruta), exist_ok=True)
        dibujo().save(ruta)
    for nombre, d in M.items():
        modelo(nombre, d)
    java()
    for idioma, campo in (("es_es", "es"), ("en_us", "en")):
        ruta = os.path.join(ASSETS, 'lang', idioma + '.json')
        datos = json.load(open(ruta, encoding='utf-8'))
        for nombre, d in M.items():
            datos["block.yonga." + nombre] = d[campo]
        escribir(ruta, datos)
    MC = os.path.join(RAIZ, 'resources', 'data', 'minecraft', 'tags', 'blocks', 'mineable')
    for herramienta in ("pickaxe", "axe"):
        ruta = os.path.join(MC, herramienta + '.json')
        datos = json.load(open(ruta, encoding='utf-8')) if os.path.exists(ruta) else {"replace": False, "values": []}
        for nombre, d in M.items():
            if HERRAMIENTA.get(d["material"]) == herramienta and "yonga:" + nombre not in datos["values"]:
                datos["values"].append("yonga:" + nombre)
        escribir(ruta, datos)
    print(len(M), "muebles")

if __name__ == "__main__":
    main()
