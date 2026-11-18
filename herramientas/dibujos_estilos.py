"""Dibujos (16x16) de los bloques por estilos arquitectónicos. Los usa generar_bloques.py."""
import math
from PIL import Image

def ruido(x, y, s=0):
    n = (x * 374761393 + y * 668265263 + s * 982451653) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 0xFFFF

def T(pal, i):
    return pal[max(0, min(len(pal) - 1, i))] + (255,)

def lienzo():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))

def grano(pal, x, y, s, base=None):
    n = ruido(x, y, s)
    b = len(pal) // 2 if base is None else base
    return T(pal, b + (n > 0.78) - (n < 0.2))

def ladrillos(pal, junta, alto, ancho, s, desplazado=True, bisel=True):
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            fila = y // alto
            d = (ancho // 2) if (desplazado and fila % 2) else 0
            lx, ly = (x + d) % ancho, y % alto
            if ly == alto - 1 or lx == ancho - 1:
                p[x, y] = junta + (255,); continue
            n = ruido(x, y, s)
            b = len(pal) // 2
            if bisel and (ly == 0 or lx == 0): b += 1
            elif bisel and (ly == alto - 2 or lx == ancho - 2): b -= 1
            p[x, y] = T(pal, b + (n > 0.8) - (n < 0.18))
    return im

# ---------------- PALETAS ----------------
PIEDRA = [(70, 66, 62), (92, 88, 82), (112, 108, 100), (128, 124, 116), (144, 140, 130), (162, 158, 148)]
BARRO = [(110, 74, 48), (132, 92, 60), (152, 110, 74), (170, 128, 88), (186, 146, 104), (200, 164, 122)]
MEGALITO = [(66, 68, 70), (84, 86, 88), (102, 104, 104), (118, 120, 118), (134, 136, 132), (150, 152, 148)]
LIQUEN = [(92, 112, 62), (116, 138, 74), (140, 160, 92)]
MARMOL = [(196, 196, 200), (212, 212, 216), (226, 226, 230), (236, 236, 238), (244, 244, 246), (252, 252, 252)]
VETA = [(140, 140, 150), (165, 165, 175)]
TERRA = [(130, 56, 30), (164, 76, 42), (190, 98, 56)]
LADRILLO_R = [(116, 48, 32), (140, 62, 40), (160, 78, 50), (178, 94, 62), (194, 112, 76)]
HORMIGON = [(150, 150, 148), (164, 164, 162), (176, 176, 174), (186, 186, 184), (196, 196, 194), (206, 206, 204)]
MADERA = [(70, 46, 28), (96, 64, 38), (122, 84, 50), (146, 104, 64), (168, 124, 80), (188, 146, 98)]
BLANCO = [(214, 216, 218), (226, 228, 230), (236, 238, 240), (244, 245, 246), (250, 250, 250), (255, 255, 255)]
GOTICO = [(58, 60, 66), (74, 76, 84), (90, 92, 100), (106, 108, 116), (122, 124, 132), (140, 142, 150)]
RENAC = [(150, 124, 88), (170, 144, 104), (188, 162, 120), (204, 180, 138), (218, 196, 154), (230, 212, 172)]
NEGRO = [(14, 14, 18), (22, 22, 28), (32, 32, 38), (44, 44, 50), (56, 56, 62)]
ORO = [(120, 84, 20), (170, 126, 36), (214, 170, 58), (240, 206, 100), (252, 236, 160)]
ROJO = [(84, 10, 20), (110, 18, 28), (136, 28, 36), (160, 40, 46), (184, 56, 58)]
ROSA = [(214, 168, 170), (226, 184, 186), (236, 198, 200), (244, 212, 212), (250, 226, 226)]
CELESTE = [(150, 186, 206), (168, 202, 220), (184, 214, 230), (200, 226, 238), (216, 236, 244)]
ARENA = [(156, 128, 82), (178, 150, 100), (198, 170, 118), (214, 188, 136), (228, 204, 154), (238, 218, 172)]
TURQUESA = [(20, 96, 100), (30, 124, 128), (46, 152, 154)]

# ---------------- NEOLÍTICO / ANTIGUO ----------------
def piedra_seca():
    """Muro de piedra seca: piedras irregulares (celdas de Voronoi) sin mortero, con huecos oscuros."""
    semillas = [(2, 2), (8, 1), (13, 3), (4, 7), (10, 6), (15, 9), (1, 12), (7, 11), (12, 13), (5, 15)]
    im = lienzo(); p = im.load()
    def celda(x, y):
        mejor = None
        for i, (sx, sy) in enumerate(semillas):
            for ox in (-16, 0, 16):
                for oy in (-16, 0, 16):
                    d = (x - sx - ox) ** 2 + ((y - sy - oy) * 1.3) ** 2
                    if mejor is None or d < mejor[0]:
                        segundo = mejor
                        mejor = (d, i)
        return mejor[1]
    mapa = {(x, y): celda(x, y) for x in range(16) for y in range(16)}
    for y in range(16):
        for x in range(16):
            c = mapa[(x, y)]
            borde = any(mapa[((x + dx) % 16, (y + dy) % 16)] != c for dx, dy in ((1, 0), (0, 1)))
            if borde:
                p[x, y] = PIEDRA[0] + (255,); continue
            arriba = mapa[(x, (y - 1) % 16)] != c or mapa[((x - 1) % 16, y)] != c
            n = ruido(x, y, 30)
            b = 2 + (c % 3 == 0) + (1 if arriba else 0)
            p[x, y] = T(PIEDRA, b + (n > 0.8) - (n < 0.2))
    return im

def adobe():
    im = ladrillos(BARRO, (96, 64, 42), 5, 8, 31)
    p = im.load()
    for y in range(16):                                  # pajitas
        for x in range(16):
            if ruido(x, y, 32) > 0.93 and p[x, y][0] > 120:
                p[x, y] = (214, 186, 120, 255)
    return im

def megalito():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x // 2, y // 2, 33) * 0.6 + ruido(x, y, 34) * 0.4
            p[x, y] = T(MEGALITO, 1 + int(n * 4.2))
    for (x, y) in ((3, 2), (4, 3), (4, 4), (5, 5), (5, 6), (6, 7), (11, 9), (11, 10), (12, 11), (12, 12), (13, 13)):
        p[x, y] = MEGALITO[0] + (255,)                    # grietas
    for y in range(16):                                   # líquenes
        for x in range(16):
            if ruido(x // 2, y // 2, 35) > 0.82 and ruido(x, y, 36) > 0.4:
                p[x, y] = T(LIQUEN, int(ruido(x, y, 37) * 3))
    return im

def vetas(p, x, y, color_grueso, color_fino, desfase=0.0):
    """Vetas de mármol continuas y que encajan al poner un bloque al lado de otro (se repiten cada 16)."""
    f = x + y * 0.5 + 2.5 * math.sin(2 * math.pi * y / 16 + desfase)
    if (f % 16) < 1.1:
        p[x, y] = color_grueso
        return True
    g = x * 0.5 - y + 2.0 * math.sin(2 * math.pi * x / 16 + desfase)
    if (g % 16) < 0.55 and ruido(x, y, 7) > 0.25:
        p[x, y] = color_fino
        return True
    return False

# ---------------- CLÁSICO (templos griegos y romanos) ----------------
def marmol():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 40)
            p[x, y] = T(MARMOL, 3 + (n > 0.8) - (n < 0.25))
            vetas(p, x, y, VETA[1] + (255,), VETA[0] + (255,))
    return im

def columna_marmol():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            t = {0: 4, 1: 3, 2: 1, 3: 3}[x % 4]
            p[x, y] = T(MARMOL, t + (ruido(x, y, 41) > 0.85))
    return im

def columna_marmol_tapa():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            t = 4 if d < 6.4 else 2
            if 6.4 <= d < 7.4: t = 0
            if d < 2: t = 2
            p[x, y] = T(MARMOL, t + (ruido(x, y, 42) > 0.85))
    return im

def friso_griego():
    """Greca (meandro griego) terracota sobre crema, con filetes arriba y abajo."""
    im = lienzo(); p = im.load()
    greca = ["................",
             "################",
             "................",
             ".#######.#######",
             ".#.....#.#.....#",
             ".#.###.#.#.###.#",
             ".#.#.#.#.#.#.#.#",
             ".#.#...#.#.#...#",
             ".#.#####.#.#####",
             ".#.......#......",
             ".#########.#####",
             "................",
             "################",
             "................",
             "................", "................"]
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 43)
            if y >= 13:
                p[x, y] = T(MARMOL, 2 + (n > 0.8)); continue
            p[x, y] = T(TERRA, 1 + (n > 0.8)) if greca[y][x] == '#' else T(ARENA, 4 + (n > 0.8) - (n < 0.2))
    return im

def ladrillo_romano():
    return ladrillos(LADRILLO_R, (196, 182, 160), 3, 16, 44)

# ---------------- MODERNO ----------------
def hormigon_pulido():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 50)
            p[x, y] = T(HORMIGON, 3 + (n > 0.85) - (n < 0.12))
            if y == 15 or x == 15:
                p[x, y] = HORMIGON[1] + (255,)            # junta del encofrado
    for (x, y) in ((3, 3), (11, 3), (3, 11), (11, 11)):     # agujeros de los tensores
        p[x, y] = HORMIGON[0] + (255,); p[x + 1, y] = (130, 130, 128, 255)
    return im

def lamas_madera():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 51)
            if x % 4 == 3:
                p[x, y] = MADERA[0] + (255,); continue
            t = (3 if x % 4 == 0 else 2) + (1 if (x // 4) % 2 else 0)
            if ruido(x, y // 3, 52) > 0.85: t -= 1      # vetas
            p[x, y] = T(MADERA, t + (n > 0.85))
    return im

def azulejo_metro():
    im = ladrillos(BLANCO, (186, 190, 194), 4, 8, 53)
    p = im.load()
    for y in range(16):
        for x in range(16):
            if y % 4 == 0 and (x + (4 if (y // 4) % 2 else 0)) % 8 in (1, 2):
                p[x, y] = (255, 255, 255, 255)              # brillo del esmalte
    return im

def revoco_blanco():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 54) * 0.7 + ruido(x // 2, y // 2, 55) * 0.3
            p[x, y] = T(BLANCO, 2 + (n > 0.72) - (n < 0.25))
    return im

def ventanal():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                p[x, y] = (30, 30, 34, 255); continue
            brillo = (x - y) in (2, 3) or (x - y) in (8,)
            p[x, y] = (235, 245, 255, 110) if brillo else (180, 210, 226, 45)
    return im

# ---------------- GÓTICO (iglesias y catedrales) ----------------
def piedra_gotica():
    return ladrillos(GOTICO, (40, 42, 48), 8, 8, 60)

def traceria():
    """Panel de piedra con dos arcos apuntados (lancetas) y un óculo encima."""
    im = lienzo(); p = im.load()
    def hueco(x, y):
        for cx in (4.5, 11.5):
            if 2.5 <= abs(x - cx) * 0 + 0 and False: pass
        for cx in (4.5, 11.5):
            ancho = 2.2
            if 8 <= y <= 14 and abs(x - cx) <= ancho:
                return True
            if 4 <= y < 8:                               # arco apuntado: se estrecha hacia arriba
                if abs(x - cx) <= ancho * (y - 3.2) / 4.8:
                    return True
        if math.hypot(x - 8, y - 3.5) <= 1.6:
            return True
        return False
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 61)
            if hueco(x, y):
                p[x, y] = T(NEGRO, 2 + (y > 10))
            else:
                b = 3
                if hueco(x + 1, y) or hueco(x, y + 1): b = 5     # bisel claro del borde del hueco
                if hueco(x - 1, y) or hueco(x, y - 1): b = 1
                if x in (0, 15) or y in (0, 15): b = 2
                p[x, y] = T(GOTICO, b + (n > 0.85))
    return im

def roseton():
    """Rosetón: ocho pétalos de cristal (azules y rojos alternos) alrededor de un centro dorado, con plomo,
    en un marco de piedra."""
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            if d > 7.6:
                p[x, y] = T(GOTICO, 3 + (ruido(x, y, 62) > 0.8)); continue
            if d > 6.8:
                p[x, y] = (40, 40, 46, 255); continue            # aro de plomo
            if d < 1.8:
                p[x, y] = (236, 196, 70, 210); continue           # centro dorado
            if d < 2.6:
                p[x, y] = (40, 40, 46, 255); continue
            a = (math.atan2(dy, dx) + math.pi) / (2 * math.pi) * 8
            if abs(a - round(a)) < 0.16:
                p[x, y] = (40, 40, 46, 255); continue             # nervios de plomo
            azul = int(a) % 2 == 0
            claro = d < 5
            r, g, b = ((90, 130, 235) if claro else (50, 80, 190)) if azul else ((230, 80, 70) if claro else (170, 40, 44))
            p[x, y] = (r, g, b, 180)
    return im

def suelo_ajedrez():
    """Suelo de iglesia: mármol blanco y negro en damero, en diagonal."""
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 63)
            if ((x + y) // 8 + (x - y + 16) // 8) % 2 == 0:
                p[x, y] = T(MARMOL, 3 + (n > 0.8) - (n < 0.2))
            else:
                p[x, y] = T(NEGRO, 2 + (n > 0.8))
    return im

# ---------------- RENACENTISTA ----------------
def almohadillado():
    """Sillares almohadillados: cada bloque abombado, con biseles marcados y junta profunda."""
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            fila = y // 8
            d = 4 if fila % 2 else 0
            lx, ly = (x + d) % 8, y % 8
            n = ruido(x, y, 70)
            if ly == 7 or lx == 7:
                p[x, y] = RENAC[0] + (255,); continue
            if ly == 0 or lx == 0: b = 5
            elif ly == 1 or lx == 1: b = 4
            elif ly == 6 or lx == 6: b = 1
            elif ly == 5 or lx == 5: b = 2
            else: b = 3
            p[x, y] = T(RENAC, b + (n > 0.88) - (n < 0.1))
    return im

def casetonado():
    """Casetón (techo artesonado): cuadrado hundido en escalones con un florón dorado en medio."""
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            n = ruido(x, y, 71)
            if d > 6.5: b = 4
            elif d > 5.5: b = 2 if (x > 7.5 or y > 7.5) else 5
            elif d > 4.5: b = 3
            elif d > 3.5: b = 1 if (x > 7.5 or y > 7.5) else 4
            else: b = 2
            p[x, y] = T(RENAC, b + (n > 0.88))
    for (x, y) in ((7, 6), (8, 6), (6, 7), (9, 7), (6, 8), (9, 8), (7, 9), (8, 9)):
        p[x, y] = ORO[2] + (255,)
    for (x, y) in ((7, 7), (8, 8)):
        p[x, y] = ORO[4] + (255,)
    for (x, y) in ((8, 7), (7, 8)):
        p[x, y] = ORO[3] + (255,)
    return im

# ---------------- NEOCLÁSICO ----------------
def estuco_moldura():
    im = revoco_blanco(); p = im.load()
    perfil = {0: 5, 1: 4, 2: 1, 3: 3, 4: 2, 5: 0}
    for y, t in perfil.items():
        for x in range(16):
            p[x, y] = T(MARMOL, t + (ruido(x, y, 72) > 0.85))
    return im

def denticulos():
    """Dentículos: la fila de "dientes" de las cornisas clásicas, bajo una moldura."""
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 73)
            if y < 4:
                t = {0: 5, 1: 4, 2: 2, 3: 0}[y]
            elif y < 11:
                lx = x % 4
                t = (4 if lx == 0 else (1 if lx == 2 else 3)) if lx != 3 else 0
                if lx == 3: p[x, y] = (150, 150, 156, 255); continue
            elif y == 11:
                t = 0
            else:
                t = {12: 5, 13: 3, 14: 2, 15: 1}[y]
            p[x, y] = T(MARMOL, t + (n > 0.88))
    return im

def pilastra():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 74)
            if x in (0, 15): t = 1
            elif x in (1, 14): t = 4
            elif 4 <= x <= 11:
                t = 2 if x == 4 else (5 if x == 11 else 3)  # panel rehundido
            else: t = 3
            p[x, y] = T(MARMOL, t + (n > 0.88))
    return im

def pilastra_tapa():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            b = 3 if 1 <= x <= 14 and 1 <= y <= 14 else 1
            p[x, y] = T(MARMOL, b + (ruido(x, y, 75) > 0.85))
    return im

# ---------------- BARROCO ----------------
def marmol_negro():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 80)
            p[x, y] = T(NEGRO, 2 + (n > 0.8) - (n < 0.2))
            vetas(p, x, y, ORO[2 + (ruido(x, y, 81) > 0.6)] + (255,), ORO[1] + (255,), 1.3)
    return im

def talla_dorada():
    """Talla barroca: volutas doradas en "S" sobre madera oscura."""
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            p[x, y] = T(MADERA[:3], 1 + (ruido(x, y, 82) > 0.8) - (ruido(x, y, 83) < 0.2))
    voluta = [(2, 3), (3, 2), (4, 2), (5, 3), (5, 4), (4, 5), (3, 5), (3, 4), (6, 5), (7, 6), (8, 7), (9, 8), (10, 9),
              (10, 10), (11, 11), (12, 11), (13, 10), (13, 9), (12, 8), (11, 9), (11, 10)]
    for i, (x, y) in enumerate(voluta):
        for (vx, vy) in ((x, y), (x, y + 8 if y + 8 < 16 else y - 8)):
            pass
        p[x, y] = ORO[3 if i % 3 == 0 else 2] + (255,)
        if y + 1 < 16: p[x, y + 1] = p[x, y + 1] if p[x, y + 1][0] > 150 else ORO[0] + (255,)
    for (x, y) in ((1, 13), (2, 12), (3, 12), (4, 13), (13, 2), (14, 3), (12, 2)):
        p[x, y] = ORO[2] + (255,)
    for x in range(16):                                   # filetes dorados arriba y abajo
        p[x, 0] = ORO[3] + (255,); p[x, 15] = ORO[1] + (255,)
    return im

def damasco():
    """Papel de damasco rojo: un motivo de flor y hojas en rojo oscuro sobre rojo."""
    im = lienzo(); p = im.load()
    motivo = ["................",
              ".......##.......",
              "......####......",
              ".....##..##.....",
              "....##.##.##....",
              "...##.####.##...",
              "....##.##.##....",
              ".....##..##.....",
              "......####......",
              ".......##.......",
              ".......##.......",
              "....###..###....",
              "...#..#..#..#...",
              "....##....##....",
              "................",
              "................"]
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 84)
            p[x, y] = T(ROJO, (1 if motivo[y][x] == '#' else 3) + (n > 0.85) - (n < 0.1))
    return im

# ---------------- ROCOCÓ ----------------
def estuco_rosa():
    """Estuco rosa pastel con rocallas (curvas en "C") blancas."""
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            p[x, y] = T(ROSA, 2 + (ruido(x, y, 90) > 0.8) - (ruido(x, y, 91) < 0.15))
    for cx, cy, r, a0, a1 in ((5, 5, 3.2, 0.2, 4.6), (11, 11, 3.2, 3.3, 7.8), (12, 4, 2.0, 1.5, 5.5), (4, 12, 2.0, -1.5, 2.5)):
        for k in range(40):
            a = a0 + (a1 - a0) * k / 39
            x, y = round(cx + r * math.cos(a)), round(cy + r * math.sin(a))
            if 0 <= x < 16 and 0 <= y < 16:
                p[x, y] = (255, 255, 255, 255)
                if 0 <= y + 1 < 16 and p[x, y + 1][1] < 240: p[x, y + 1] = ROSA[0] + (255,)
    return im

def panel_rococo():
    """Panel celeste con marco dorado de esquinas curvas."""
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            p[x, y] = T(CELESTE, 2 + (ruido(x, y, 92) > 0.85))
            # marco a 2 píxeles del borde, con las esquinas hacia dentro (curvas)
            dx, dy = min(x, 15 - x), min(y, 15 - y)
            if (dx == 2 and dy >= 4) or (dy == 2 and dx >= 4) or (dx == 3 and dy == 3):
                p[x, y] = ORO[3 if (x < 8 and y < 8) else 2] + (255,)
            if dx == 0 or dy == 0:
                p[x, y] = T(BLANCO, 3)
    for (x, y) in ((7, 7), (8, 8), (7, 8), (8, 7)):
        p[x, y] = ORO[3] + (255,)
    return im

# ---------------- EGIPCIO ----------------
def jeroglificos():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            p[x, y] = T(ARENA, 3 + (ruido(x, y, 100) > 0.82) - (ruido(x, y, 101) < 0.15))
            if x in (0, 15): p[x, y] = T(ARENA, 1)
    glifos = [  # (x, y) incisos: ankh, ojo, pájaro y ondas, en dos columnas
        (3, 1), (2, 2), (4, 2), (3, 3), (2, 4), (3, 4), (4, 4), (3, 5), (3, 6),          # ankh
        (2, 9), (3, 8), (4, 8), (5, 9), (3, 10), (4, 10), (4, 9),                        # ojo
        (2, 13), (3, 13), (4, 13), (5, 13), (3, 14),                                     # base
        (10, 1), (11, 1), (12, 2), (11, 3), (10, 3), (9, 4), (10, 5), (12, 4),           # pájaro
        (9, 8), (10, 9), (11, 8), (12, 9), (13, 8),                                      # ondas
        (9, 11), (10, 12), (11, 11), (12, 12), (13, 11),
        (10, 14), (11, 14), (12, 14)]
    for (x, y) in glifos:
        p[x, y] = ARENA[0] + (255,)
        if x + 1 < 16 and (x + 1, y) not in glifos: p[x + 1, y] = ARENA[5] + (255,)
    for y in range(16):
        p[7, y] = T(ARENA, 1); p[8, y] = T(ARENA, 4)       # separación de columnas
    return im

# ---------------- ART DÉCO ----------------
def abanico_deco():
    """Abanico de rayos dorados que nacen abajo, con arcos, sobre negro."""
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, 16.5 - y
            d = math.hypot(dx, dy); a = math.degrees(math.atan2(dy, dx))
            c = T(NEGRO, 1 + (ruido(x, y, 110) > 0.8))
            if d < 15.5:
                if int(a) % 18 < 3 or abs(d - 6) < 0.5 or abs(d - 11) < 0.5:
                    c = ORO[3] + (255,) if d < 11 else ORO[2] + (255,)
                if d < 3:
                    c = ORO[4] + (255,)
            if y == 0: c = ORO[3] + (255,)
            p[x, y] = c
    return im

def zigzag_deco():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            onda = abs((x % 8) - 4)
            banda = (y + onda) % 8
            if banda < 2: c = ORO[3] + (255,) if banda == 0 else ORO[2] + (255,)
            elif banda < 4: c = T(NEGRO, 1 + (ruido(x, y, 111) > 0.8))
            elif banda < 6: c = T(TURQUESA, 1 + (banda == 4))
            else: c = T(NEGRO, 2)
            p[x, y] = c
    return im

def marmol_verde():
    im = lienzo(); p = im.load()
    VERDE = [(16, 60, 44), (24, 80, 58), (34, 100, 72), (48, 120, 88), (66, 140, 104)]
    for y in range(16):
        for x in range(16):
            p[x, y] = T(VERDE, 2 + (ruido(x, y, 112) > 0.8) - (ruido(x, y, 113) < 0.2))
            vetas(p, x, y, (200, 230, 210, 255), (120, 170, 140, 255), 2.6)
    return im
