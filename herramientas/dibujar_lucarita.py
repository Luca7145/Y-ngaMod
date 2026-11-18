"""Iconos del pico y la pala de lucarita, en estilo clásico de Minecraft (16x16).
(Los iconos de la armadura: dibujar_lucarita_iconos.py; la armadura puesta: dibujar_lucarita_armadura.py.)
Antes: armadura y herramientas en estilo clásico (16x16 y armadura de 64x32):
silueta como las del juego, sombreado plano de 4 tonos con el color del lingote, y pocos detalles
propios: vetas azul y roja como las del lingote y algún toque de oro.

Uso: python lucarita_clasica.py <carpeta_salida>"""
import sys
from PIL import Image

T = (0, 0, 0, 0)
CONT = (60, 28, 0, 255)
SOMBRA = (143, 69, 0, 255)
BASE = (209, 130, 56, 255)
BASE_O = (188, 112, 44, 255)
CLARO = (247, 149, 57, 255)
BRILLO = (252, 220, 189, 255)
ORO = (231, 197, 71, 255)
AZUL = (41, 67, 150, 255); AZUL_C = (80, 120, 215, 255)
ROJO = (165, 30, 40, 255); ROJO_C = (225, 70, 60, 255)
MAD_C = (104, 78, 47, 255); MAD = (73, 54, 33, 255); MAD_O = (40, 30, 11, 255)

def icono(mascara, detalles, mango=None):
    """mascara: 16 filas, '#' = metal. detalles: {(x, y): color}. mango: lista de (x, y) del palo.
    Como los objetos de Minecraft: contorno oscuro alrededor, borde de arriba-izquierda claro,
    de abajo-derecha en sombra y el centro del color base."""
    im = Image.new('RGBA', (16, 16), T); p = im.load()
    metal = {(x, y) for y, f in enumerate(mascara) for x, c in enumerate(f) if c == '#'}
    palo = set(mango or []) - metal
    todo = metal | palo
    for (x, y) in palo:
        p[x, y] = MAD_C if (x + y) % 4 == 0 else MAD
    for (x, y) in metal:
        arriba_izq = (x, y - 1) not in metal or (x - 1, y) not in metal
        abajo_der = (x, y + 1) not in metal or (x + 1, y) not in metal
        if arriba_izq and not abajo_der:
            c = BRILLO if (x + y) % 3 == 0 else CLARO
        elif abajo_der:
            c = SOMBRA
        elif (x, y - 2) not in metal or (x - 2, y) not in metal:
            c = CLARO
        else:
            c = BASE_O if (x * 3 + y * 5) % 7 == 0 else BASE      # algo de textura, como el metal del juego
        p[x, y] = c
    for y in range(16):
        for x in range(16):
            if (x, y) not in todo and any((x + dx, y + dy) in todo for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                p[x, y] = CONT if any((x + dx, y + dy) in metal for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))) else MAD_O
    for (x, y), c in detalles.items():
        p[x, y] = c
    return im

CASCO = ["................",
         "................",
         "................",
         "....########....",
         "...##########...",
         "..############..",
         "..############..",
         "..############..",
         "..####....####..",
         "..###......###..",
         "..###......###..",
         "................", "................", "................", "................", "................"]
CASCO_D = {(5, 5): AZUL_C, (6, 6): AZUL, (10, 5): ROJO_C, (11, 6): ROJO, (7, 7): ORO, (8, 7): ORO}

PETO = ["................",
        "..###......###..",
        ".#####....#####.",
        ".##############.",
        ".##############.",
        "..############..",
        "...##########...",
        "...##########...",
        "...##########...",
        "...##########...",
        "...##########...",
        "...##########...",
        "....########....",
        "................", "................", "................"]
PETO_D = {(7, 4): ORO, (8, 4): ORO, (7, 5): AZUL_C, (8, 5): ROJO_C, (7, 6): AZUL, (8, 6): ROJO,
          (5, 8): AZUL_C, (6, 9): AZUL, (10, 9): ROJO_C, (11, 10): ROJO}

PANTALONES = ["................",
              "...##########...",
              "...##########...",
              "...##########...",
              "...####..####...",
              "...####..####...",
              "...####..####...",
              "...####..####...",
              "...####..####...",
              "...####..####...",
              "...####..####...",
              "................", "................", "................", "................", "................"]
PANTALONES_D = {(x, 2): ORO for x in range(4, 12)}
PANTALONES_D.update({(7, 2): ROJO_C, (8, 2): AZUL_C, (5, 6): AZUL_C, (6, 7): AZUL, (10, 6): ROJO_C, (9, 7): ROJO})

BOTAS = ["................", "................", "................", "................", "................",
         "................",
         "...###....###...",
         "...###....###...",
         "...###....###...",
         "...###....###...",
         "..####....####..",
         "..####....####..",
         "................", "................", "................", "................"]
BOTAS_D = {(4, 7): ORO, (11, 7): ORO, (4, 9): AZUL_C, (5, 10): AZUL, (11, 9): ROJO_C, (12, 10): ROJO}

PICO = ["................",
        "...########.....",
        "..##########....",
        "..###....#####..",
        "..........####..",
        "...........###..",
        "...........###..",
        "............##..",
        "............##..",
        "................",
        "................", "................", "................", "................", "................", "................"]
PICO_MANGO = [(1 + i, 14 - i) for i in range(10)]
PICO_D = {(4, 2): AZUL_C, (5, 2): AZUL, (12, 5): ROJO_C, (12, 6): ROJO, (10, 4): ORO, (9, 3): ORO}

PALA = ["................",
        "...........###..",
        "..........#####.",
        ".........######.",
        ".........#####..",
        "..........###...",
        "................",
        "................", "................", "................", "................",
        "................", "................", "................", "................", "................"]
PALA_MANGO = [(1 + i, 14 - i) for i in range(9)]
PALA_D = {(12, 3): AZUL_C, (11, 4): AZUL, (13, 2): ROJO_C, (10, 5): ORO}

# ---------- Armadura puesta (64x32, como la de Minecraft) ----------
def caras(u, v, w, h, d):
    return {'arriba': (u + d, v, w, d), 'abajo': (u + d + w, v, w, d), 'derecha': (u, v + d, d, h),
            'delante': (u + d, v + d, w, h), 'izquierda': (u + d + w, v + d, d, h), 'detras': (u + 2 * d + w, v + d, w, h)}

def placa(p, r, filas=None, cols=None):
    x0, y0, w, h = r
    m = {(x, y) for x in range(w) for y in range(h) if (filas is None or y in filas) and (cols is None or x in cols)}
    for (x, y) in m:
        if (x, y - 1) not in m or (x - 1, y) not in m: c = CLARO
        elif (x, y + 1) not in m or (x + 1, y) not in m: c = SOMBRA
        else: c = BASE_O if (x * 3 + y * 5) % 7 == 0 else BASE
        p[x0 + x, y0 + y] = c

def punto(p, r, x, y, c):
    p[r[0] + x, r[1] + y] = c

def capa1():
    im = Image.new('RGBA', (64, 32), T); p = im.load()
    k = caras(0, 0, 8, 8, 8)
    for n in ('arriba', 'derecha', 'izquierda', 'detras'):
        placa(p, k[n])
    r = k['delante']                                   # la cara se ve por el hueco, como en Minecraft
    placa(p, r, filas=range(0, 3)); placa(p, r, cols=(0, 1), filas=range(0, 7)); placa(p, r, cols=(6, 7), filas=range(0, 7))
    punto(p, r, 3, 1, ORO); punto(p, r, 4, 1, ORO)
    punto(p, k['derecha'], 2, 2, AZUL_C); punto(p, k['derecha'], 3, 3, AZUL)
    punto(p, k['izquierda'], 5, 2, ROJO_C); punto(p, k['izquierda'], 4, 3, ROJO)
    punto(p, k['arriba'], 3, 3, AZUL_C); punto(p, k['arriba'], 4, 4, ROJO_C)
    punto(p, k['detras'], 2, 4, ROJO_C); punto(p, k['detras'], 5, 2, AZUL_C)
    k = caras(16, 16, 8, 12, 4)
    for n in ('arriba', 'delante', 'derecha', 'izquierda', 'detras'):
        placa(p, k[n])
    r = k['delante']
    punto(p, r, 3, 2, ORO); punto(p, r, 4, 2, ORO); punto(p, r, 3, 3, AZUL_C); punto(p, r, 4, 3, ROJO_C)
    punto(p, r, 3, 4, AZUL); punto(p, r, 4, 4, ROJO)
    punto(p, r, 1, 7, AZUL_C); punto(p, r, 2, 8, AZUL); punto(p, r, 6, 6, ROJO_C); punto(p, r, 5, 7, ROJO)
    for x in range(8): punto(p, r, x, 11, ORO if x % 2 == 0 else SOMBRA)
    r = k['detras']
    punto(p, r, 2, 3, ROJO_C); punto(p, r, 3, 4, ROJO); punto(p, r, 5, 7, AZUL_C); punto(p, r, 6, 8, AZUL)
    k = caras(40, 16, 4, 12, 4)
    for n in ('arriba', 'delante', 'derecha', 'izquierda', 'detras'):
        placa(p, k[n], filas=range(0, 5) if n != 'arriba' else None)
    for n in ('delante', 'derecha', 'izquierda', 'detras'):
        for x in range(4): punto(p, k[n], x, 4, ORO if x in (1, 2) else SOMBRA)
    punto(p, k['derecha'], 1, 1, AZUL_C); punto(p, k['izquierda'], 2, 1, ROJO_C)
    k = caras(0, 16, 4, 12, 4)
    for n in ('delante', 'derecha', 'izquierda', 'detras'):
        placa(p, k[n], filas=range(8, 12))
        for x in range(4): punto(p, k[n], x, 8, ORO if x in (1, 2) else CLARO)
    punto(p, k['derecha'], 1, 10, AZUL_C); punto(p, k['izquierda'], 2, 10, ROJO_C)
    placa(p, k['abajo'])
    return im

def capa2():
    im = Image.new('RGBA', (64, 32), T); p = im.load()
    k = caras(16, 16, 8, 12, 4)
    for n in ('delante', 'derecha', 'izquierda', 'detras'):
        placa(p, k[n], filas=range(8, 12))
        for x in range(k[n][2]): punto(p, k[n], x, 8, ORO)
    punto(p, k['delante'], 3, 8, AZUL_C); punto(p, k['delante'], 4, 8, ROJO_C)
    k = caras(0, 16, 4, 12, 4)
    for n in ('arriba', 'delante', 'derecha', 'izquierda', 'detras'):
        placa(p, k[n], filas=range(0, 9) if n != 'arriba' else None)
    punto(p, k['delante'], 1, 2, AZUL_C); punto(p, k['delante'], 2, 3, AZUL)
    punto(p, k['derecha'], 2, 5, AZUL_C); punto(p, k['izquierda'], 1, 5, ROJO_C); punto(p, k['detras'], 2, 3, ROJO_C)
    return im

if __name__ == "__main__":
    salida = sys.argv[1]
    for nombre, m, d, mango in (("pico", PICO, PICO_D, PICO_MANGO), ("pala", PALA, PALA_D, PALA_MANGO)):
        assert all(len(f) == 16 for f in m), nombre
        icono(m, d, mango).save(f"{salida}/{nombre}_lucarita.png")
    print("ok")
