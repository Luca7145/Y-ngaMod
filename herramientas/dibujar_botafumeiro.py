"""Bocetos (32x32) del botafumeiro, estilo barroco y oscuro (sirve al lado luminoso y al oscuro):
metal ennegrecido, filigrana de oro viejo y brasas que asoman por las celosías.
Tres texturas: entero (mango + cadena + incensario colgando), solo el mango, solo el incensario.

Uso: python dibujar_botafumeiro.py <carpeta_salida>"""
import sys
from PIL import Image
N = 32
CONT = (14, 12, 16, 255)
M1 = (34, 31, 38, 255); M2 = (52, 48, 58, 255); M3 = (78, 72, 86, 255); M4 = (110, 102, 118, 255)   # metal ennegrecido
O1 = (92, 64, 26, 255); O2 = (140, 102, 40, 255); O3 = (184, 142, 62, 255); O4 = (222, 186, 100, 255)  # oro viejo
B1 = (120, 30, 12, 255); B2 = (200, 72, 20, 255); B3 = (255, 150, 50, 255)                            # brasas
EB = (26, 18, 14, 255); EB2 = (46, 32, 24, 255)                                                       # ébano
VINO = (70, 16, 30, 255)

def contorno(im, hay):
    p = im.load()
    for y in range(N):
        for x in range(N):
            if (x, y) not in hay and any((x + dx, y + dy) in hay for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                p[x, y] = CONT

def poner(p, hay, x, y, col):
    if 0 <= x < N and 0 <= y < N:
        p[x, y] = col; hay.add((x, y))

def mango(p, hay, x0=1, y0=30, largo=10):
    """Mango de ébano en diagonal con anillas de oro, puño forrado de vino y pomo dorado."""
    for t in range(largo):
        x, y = x0 + t, y0 - t
        for dx, dy in ((0, 0), (1, 0), (0, -1), (1, -1)):
            col = EB2 if (dx, dy) == (1, -1) else EB
            if 2 <= t <= 5:
                col = VINO if (dx + dy) != 0 else (45, 10, 20, 255)
            if t in (1, 6, largo - 1):
                col = O4 if (dx, dy) == (1, -1) else (O3 if (dx + dy) != 0 else O1)
            poner(p, hay, x + dx, y + dy, col)
    # pomo
    for dx, dy, c in ((-1, 1, O2), (0, 1, O3), (-1, 0, O3), (0, 0, O2)):
        poner(p, hay, x0 + dx, y0 + dy, c)
    return (x0 + largo, y0 - largo)   # dónde empieza la cadena

def cadena(p, hay, desde, hasta):
    (x0, y0), (x1, y1) = desde, hasta
    n = max(abs(x1 - x0), abs(y1 - y0), 1)
    for i in range(n + 1):
        x = round(x0 + (x1 - x0) * i / n); y = round(y0 + (y1 - y0) * i / n)
        poner(p, hay, x, y, M4 if i % 2 == 0 else M2)

# Incensario: (fila, media anchura, tipo). Arriba la anilla de la cadena, luego remate, cúpula con
# celosía, ala dorada, cuerpo panzudo con filigrana y ventana de brasas, y pie dorado.
FILAS = [
    (0, 1, 'anilla'), (1, 1, 'anilla_baja'),
    (2, 0, 'remate'), (3, 1, 'remate'),
    (4, 2, 'cupula'), (5, 3, 'celosia'), (6, 4, 'cupula'), (7, 4, 'celosia'),
    (8, 5, 'ala'), (9, 6, 'ala_baja'),
    (10, 5, 'cuerpo'), (11, 6, 'ventana'), (12, 6, 'cuerpo'), (13, 5, 'ventana'), (14, 5, 'cuerpo'),
    (15, 4, 'banda'), (16, 2, 'cuello'), (17, 3, 'pie'), (18, 4, 'pie_bajo')]

def incensario(p, hay, cx, cy):
    for fila, ancho, tipo in FILAS:
        y = cy + fila
        for dx in range(-ancho, ancho + 1):
            luz = dx < 0
            borde = abs(dx) == ancho
            if tipo == 'anilla':
                if abs(dx) != 1: continue
                col = O3
            elif tipo == 'anilla_baja':
                col = O2
            elif tipo == 'remate':
                col = O4 if dx == 0 and fila == 2 else (O3 if luz else O2)
            elif tipo == 'cupula':
                col = M4 if luz and not borde else (M3 if not borde else M2)
                if dx == 0: col = O2                                  # nervio dorado
            elif tipo == 'celosia':
                if borde: col = M2
                elif dx % 2 == 0: col = B2 if abs(dx) <= 1 else B1  # brasas por los agujeros
                else: col = O2
            elif tipo == 'ala':
                col = O4 if luz else O3
                if borde: col = O2
            elif tipo == 'ala_baja':
                col = O1 if not borde else CONT
            elif tipo == 'cuerpo':
                col = M3 if luz else M2
                if abs(dx) == 3: col = O2                             # filigrana (volutas)
                if borde: col = M1
            elif tipo == 'ventana':
                col = M3 if luz else M2
                if abs(dx) == 3 or abs(dx) == 2 and fila == 13: col = O3   # filigrana
                if dx == 0: col = B3 if fila == 11 else B2               # ventana de brasas
                if abs(dx) == 1 and fila == 11: col = B1
                if borde: col = M1
            elif tipo == 'banda':
                col = O3 if luz else O2
                if dx % 2 == 0 and not borde: col = O1                   # cordón
            elif tipo == 'cuello':
                col = M2
            elif tipo == 'pie':
                col = O3 if luz else O2
            else:  # pie_bajo
                col = O2 if luz else O1
            poner(p, hay, cx + dx, y, col)

def guardar(im, ruta, reflejar=False):
    # Los dibujos que se llevan en la mano tienen además una versión "_mano", reflejada sobre la diagonal
    # del mango (abajo-izquierda a arriba-derecha): el agarre queda igual, pero la cadena y el incensario
    # caen hacia el otro lado. El inventario usa la normal y la mano la reflejada.
    im.save(ruta)
    if reflejar:
        im.transpose(Image.Transpose.TRANSVERSE).save(ruta.replace(".png", "_mano.png"))
    return im

carpeta = sys.argv[1]
ims = []
# Entero: mango abajo-izquierda, cadena hacia la derecha, incensario colgando abajo-derecha
im = Image.new('RGBA', (N, N), (0, 0, 0, 0)); p = im.load(); hay = set()
punta = mango(p, hay)
incensario(p, hay, 24, 12)
cadena(p, hay, punta, (23, 12))
contorno(im, hay); ims.append(guardar(im, f"{carpeta}/botafumeiro.png", True))
# Solo el mango, con un trozo de cadena colgando
im = Image.new('RGBA', (N, N), (0, 0, 0, 0)); p = im.load(); hay = set()
punta = mango(p, hay)
cadena(p, hay, punta, (punta[0] + 3, punta[1] + 6))
contorno(im, hay); ims.append(guardar(im, f"{carpeta}/botafumeiro_mango.png", True))
# Solo el incensario, centrado
im = Image.new('RGBA', (N, N), (0, 0, 0, 0)); p = im.load(); hay = set()
incensario(p, hay, 15, 6)
contorno(im, hay); ims.append(guardar(im, f"{carpeta}/botafumeiro_incensario.png"))
g = Image.new('RGBA', (3 * 170, 170), (139, 139, 139, 255))
for i, im in enumerate(ims):
    r = im.resize((160, 160), Image.NEAREST); g.paste(r, (5 + i * 170, 5), r)
g.save(f"{carpeta}/botafumeiro_grande.png")
