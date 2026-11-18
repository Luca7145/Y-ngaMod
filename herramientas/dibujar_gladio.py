"""Gladio (25x25), con la paleta de las otras armas: hoja corta y ancha de doble filo con punta triangular,
canal central, guarda redondeada, empuñadura con anillas y pomo de bola."""
import sys, math
from PIL import Image
N = 25
CONT = (24, 24, 24, 255)
OSC = (68, 68, 68, 255); MED = (150, 150, 150, 255); CLA = (193, 193, 193, 255); MUYCLA = (216, 216, 216, 255); BLANCO = (255, 255, 255, 255)
MAD = (54, 37, 16, 255); MAD_C = (92, 64, 30, 255)
AZUL = (44, 78, 150, 255); AZUL_C = (72, 112, 190, 255)
LATON = (150, 118, 50, 255); LATON_C = (196, 160, 80, 255)

S = (3.2, 21.3)     # pomo
def uv(x, y):
    dx, dy = x + 0.5 - S[0], y + 0.5 - S[1]
    return (dx - dy) / math.sqrt(2), (dx + dy) / math.sqrt(2)

im = Image.new('RGBA', (N, N), (0, 0, 0, 0)); p = im.load(); lleno = set()
def poner(x, y, c):
    p[x, y] = c; lleno.add((x, y))

for y in range(N):
    for x in range(N):
        u, v = uv(x, y)
        a = abs(v)
        # pomo de bola
        if math.hypot(u, v) <= 1.9:
            poner(x, y, LATON_C if v < 0 else LATON); continue
        # empuñadura con anillas
        if 1.5 < u <= 6.5 and a <= 1.1:
            poner(x, y, (AZUL_C if v < 0 else AZUL) if int(u * 1.4) % 2 else (MAD_C if v < 0 else MAD)); continue
        # guarda redondeada (ancha y corta)
        if 6.5 < u <= 8.3 and a <= 3.4 - 0.5 * abs(u - 7.4):
            poner(x, y, LATON_C if u < 7.3 else LATON); continue
        # hoja: ancha y recta, punta triangular al final
        if 8.3 < u <= 27.0:
            ancho = 2.3 if u <= 21.5 else 2.3 * (27.0 - u) / 5.5
            if a <= ancho + 0.15:
                if abs(v) < 0.5 and u < 20.5: c = MED                # canal central
                elif v < -0.5: c = BLANCO if (a > ancho - 0.6) else MUYCLA   # filo con luz
                elif v > 0.5: c = OSC if (a > ancho - 0.6) else CLA         # filo en sombra
                else: c = MUYCLA
                poner(x, y, c)
for y in range(N):
    for x in range(N):
        if (x, y) not in lleno and any((x + dx, y + dy) in lleno for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            p[x, y] = CONT
salida = sys.argv[1]
im.save(f"{salida}/gladio.png")
g = Image.new('RGBA', (N * 10 + 10, N * 10 + 10), (139, 139, 139, 255))
r = im.resize((N * 10, N * 10), Image.NEAREST); g.paste(r, (5, 5), r); g.save(f"{salida}/gladio_grande.png")
