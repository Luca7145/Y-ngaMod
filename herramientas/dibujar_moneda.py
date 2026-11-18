"""Moneda (16x16): oro con canto estriado, un aro grabado y en el centro una L en relieve entre dos
ramas de laurel.
Uso: python dibujar_moneda.py <carpeta_salida>"""
import sys, math
from PIL import Image

ORO = [(92, 58, 8), (138, 92, 16), (184, 132, 30), (220, 170, 50), (244, 204, 86), (255, 234, 150), (255, 250, 214)]
CONT = (64, 38, 4, 255)
AZ = [(32, 56, 140, 255), (70, 110, 210, 255), (150, 190, 250, 255)]
RO = [(130, 20, 26, 255), (210, 56, 50, 255), (250, 140, 120, 255)]

def T(i):
    return ORO[max(0, min(6, i))] + (255,)

im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); p = im.load()
C = 7.5
for y in range(16):
    for x in range(16):
        d = math.hypot(x - C, y - C)
        luz = ((C - x) + (C - y)) / 10.6          # +1 arriba a la izquierda, -1 abajo a la derecha
        if d > 6.9:
            continue
        if d > 6.0:                                 # canto estriado
            a = math.atan2(y - C, x - C)
            estria = int((a + math.pi) / (2 * math.pi) * 20) % 2
            p[x, y] = T(3 + round(luz * 2) + estria)
        elif d > 5.1:                               # aro grabado (hundido: sombra arriba, luz abajo)
            p[x, y] = T(2 - round(luz * 1.5))
        else:                                       # campo de la moneda
            p[x, y] = T(4 + round(luz * 1.4))
# Laurel: dos ramas en arco alrededor de la gema (hojitas en oro oscuro)
for (x, y) in ((4, 5), (3, 7), (4, 9), (5, 10), (11, 5), (12, 7), (11, 9), (10, 11)):
    p[x, y] = T(1)                                  # hojas (hundidas)
    p[x + (1 if x < 8 else -1), y] = T(5)           # su brillo
# La L en relieve, en el centro (acuñada: cara clara, brillo arriba a la izquierda y sombra abajo a la derecha)
letra = [(6, 4), (6, 5), (6, 6), (6, 7), (6, 8), (6, 9), (7, 4), (7, 5), (7, 6), (7, 7), (7, 8), (7, 9),
         (8, 8), (9, 8), (10, 8), (8, 9), (9, 9), (10, 9)]
for (x, y) in letra:
    p[x, y] = T(6) if (x - 1, y) not in letra or (x, y - 1) not in letra else T(5)
for (x, y) in letra:
    for dx, dy in ((1, 0), (0, 1), (1, 1)):
        if (x + dx, y + dy) not in letra and 0 <= x + dx < 16 and 0 <= y + dy < 16:
            p[x + dx, y + dy] = T(1)
borde = [(x, y) for y in range(16) for x in range(16)
         if p[x, y][3] == 0 and any(0 <= x + dx < 16 and 0 <= y + dy < 16 and p[x + dx, y + dy][3] > 0 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))]
for x, y in borde:
    p[x, y] = CONT
im.save(f"{sys.argv[1]}/moneda.png")
im.resize((160, 160), Image.NEAREST).save(f"{sys.argv[1]}/moneda_grande.png")
