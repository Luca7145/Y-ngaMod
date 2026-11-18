"""Armadura de lucarita puesta (64x32, como las de Minecraft).

Técnica: cada pieza son varias PLACAS con rendijas oscuras entre ellas; cada placa con su borde de
arriba claro y el de abajo en sombra, y el interior con grano de metal (no un color plano). Encima,
remaches, ribetes de oro y alguna veta azul o roja como las del lingote.

Uso: python capas.py <carpeta_salida>"""
import sys
from PIL import Image

T = (0, 0, 0, 0)
GRIETA = (66, 30, 2, 255)                    # rendija entre placas
C = [(99, 49, 1, 255), (128, 62, 6, 255), (156, 84, 22, 255), (186, 108, 40, 255),
     (209, 130, 56, 255), (232, 146, 62, 255), (247, 168, 84, 255), (252, 214, 170, 255)]
ORO = [(130, 92, 20, 255), (184, 142, 44, 255), (231, 197, 71, 255), (250, 230, 140, 255)]
AZ = [(32, 52, 128, 255), (60, 96, 196, 255), (120, 170, 245, 255)]
RO = [(120, 18, 26, 255), (190, 40, 44, 255), (240, 110, 96, 255)]

def ruido(x, y, semilla=0):
    """Número pseudoaleatorio fijo (0..1) por píxel: el "grano" del metal."""
    n = (x * 374761393 + y * 668265263 + semilla * 982451653) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 0xFFFF

class Capa:
    def __init__(self):
        self.im = Image.new('RGBA', (64, 32), T)
        self.p = self.im.load()

    def caras(self, u, v, w, h, d):
        return {'arriba': (u + d, v, w, d), 'abajo': (u + d + w, v, w, d), 'derecha': (u, v + d, d, h),
                'delante': (u + d, v + d, w, h), 'izquierda': (u + d + w, v + d, d, h), 'detras': (u + 2 * d + w, v + d, w, h)}

    def placa(self, cara, x0, y0, x1, y1, tono=0, semilla=0, oro=False):
        """Placa rectangular (coordenadas dentro de la cara, inclusivas). tono: +1 más clara, -1 más oscura."""
        fx, fy = cara[0], cara[1]
        pal = ORO if oro else C
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                r = ruido(x + fx, y + fy, semilla)
                if oro:
                    i = (3 if r > 0.75 else 2) if y == y0 else (0 if y == y1 and y1 > y0 else (1 if r > 0.35 else 2))
                else:
                    if y == y0:                       i = 6 if r > 0.8 else 5     # borde de arriba: algo de brillo
                    elif x == x0 and x1 > x0:         i = 4 if r > 0.5 else 3     # borde izquierdo
                    elif y == y1 and y1 > y0:         i = 0 if r > 0.5 else 1     # borde de abajo: sombra
                    elif x == x1 and x1 > x0:         i = 1 if r > 0.5 else 2     # borde derecho
                    else:                             i = 2 + (r > 0.3) + (r > 0.75) + (r > 0.95)  # grano
                    i = max(0, min(7, i + tono))
                self.p[fx + x, fy + y] = pal[i]

    def rendija(self, cara, puntos):
        for x, y in puntos:
            self.p[cara[0] + x, cara[1] + y] = GRIETA

    def remache(self, cara, x, y):
        self.p[cara[0] + x, cara[1] + y] = ORO[3]

    def veta(self, cara, puntos, pal):
        for i, (x, y) in enumerate(puntos):
            self.p[cara[0] + x, cara[1] + y] = pal[2] if i == 0 else (pal[1] if i % 2 else pal[0])

    def pon(self, cara, x, y, c):
        self.p[cara[0] + x, cara[1] + y] = c

# ======================= CAPA 1: casco, peto y botas =======================
L = Capa()

# ---------- CASCO (8x8x8) ----------
k = L.caras(0, 0, 8, 8, 8)
# arriba: dos placas a los lados de una cresta central
a = k['arriba']
L.placa(a, 0, 0, 2, 7, semilla=1); L.placa(a, 5, 0, 7, 7, semilla=2)
L.placa(a, 3, 0, 4, 7, tono=1, semilla=3)                         # cresta
L.veta(a, [(3, 1), (3, 2), (4, 3)], AZ); L.veta(a, [(4, 5), (3, 6)], RO)
# delante: frente, nasal y mejillas; la cara se ve por los huecos
d = k['delante']
L.placa(d, 0, 0, 7, 2, semilla=4)
L.placa(d, 0, 0, 7, 0, oro=True)                                  # ribete de oro en la frente
L.placa(d, 3, 3, 4, 4, tono=-1, semilla=5)                        # nasal
L.placa(d, 0, 3, 1, 7, semilla=6); L.placa(d, 6, 3, 7, 7, semilla=7)
L.rendija(d, [(x, 2) for x in range(2, 6)])
L.remache(d, 1, 1); L.remache(d, 6, 1)
L.pon(d, 3, 1, AZ[1]); L.pon(d, 4, 1, RO[1])                      # gema de la frente
# lados: placa alta, rendija, placa baja (guarda de la mejilla)
for nombre, semilla, pal, veta in (('derecha', 8, AZ, [(1, 1), (2, 2), (2, 3)]), ('izquierda', 9, RO, [(6, 1), (5, 2), (5, 3)])):
    c = k[nombre]
    L.placa(c, 0, 0, 7, 3, semilla=semilla); L.placa(c, 0, 5, 7, 7, tono=-1, semilla=semilla + 10)
    L.rendija(c, [(x, 4) for x in range(8)])
    L.placa(c, 0, 4, 7, 4, oro=True)
    L.veta(c, veta, pal)
    L.remache(c, 1 if nombre == 'izquierda' else 6, 6)
# detrás: dos placas y cubrenuca
c = k['detras']
L.placa(c, 0, 0, 7, 3, semilla=11); L.placa(c, 0, 4, 7, 7, tono=-1, semilla=12)
L.rendija(c, [(x, 4) for x in range(8)])
L.placa(c, 0, 7, 7, 7, oro=True)
L.veta(c, [(2, 1), (3, 2)], RO); L.veta(c, [(5, 5), (6, 6)], AZ)

# ---------- PETO (cuerpo 8x12x4) ----------
k = L.caras(16, 16, 8, 12, 4)
d = k['delante']
L.placa(d, 0, 0, 7, 1, tono=-1, semilla=20)                       # cuello
L.placa(d, 2, 0, 5, 0, oro=True)
L.placa(d, 0, 2, 3, 6, semilla=21); L.placa(d, 4, 2, 7, 6, semilla=22)   # pectorales
L.rendija(d, [(x, 1) for x in range(8)])
L.placa(d, 0, 7, 7, 8, semilla=23); L.placa(d, 0, 9, 7, 9, tono=-1, semilla=24)  # abdomen
L.rendija(d, [(x, 7) for x in (0, 1, 6, 7)])
L.placa(d, 0, 10, 7, 11, oro=True)                                # cinturón
L.pon(d, 3, 10, AZ[1]); L.pon(d, 4, 10, RO[1])                    # hebilla
L.pon(d, 3, 3, AZ[2]); L.pon(d, 4, 3, RO[2]); L.pon(d, 3, 4, AZ[0]); L.pon(d, 4, 4, RO[0])   # gema del pecho
# (el pecho solo lleva la gema)
for nombre, semilla in (('derecha', 25), ('izquierda', 26)):
    c = k[nombre]
    L.placa(c, 0, 0, 3, 6, semilla=semilla); L.placa(c, 0, 7, 3, 9, tono=-1, semilla=semilla + 5)
    L.placa(c, 0, 10, 3, 11, oro=True)
c = k['detras']
L.placa(c, 0, 0, 3, 8, semilla=27); L.placa(c, 4, 0, 7, 8, semilla=28)
L.placa(c, 0, 9, 7, 9, tono=-1, semilla=29); L.placa(c, 0, 10, 7, 11, oro=True)
L.veta(c, [(1, 2), (2, 3), (2, 4)], RO); L.veta(c, [(6, 5), (5, 6)], AZ)
L.placa(k['arriba'], 0, 0, 7, 3, semilla=30)

# Brazos (4x12x4): hombrera de dos launas, brazo y brazal
k = L.caras(40, 16, 4, 12, 4)
for nombre, semilla in (('delante', 31), ('derecha', 32), ('izquierda', 33), ('detras', 34)):
    c = k[nombre]
    L.placa(c, 0, 0, 3, 2, tono=1, semilla=semilla)
    L.placa(c, 0, 3, 3, 4, semilla=semilla + 1)
    L.placa(c, 0, 4, 3, 4, oro=True)
    L.placa(c, 0, 8, 3, 11, tono=-1, semilla=semilla + 2)        # brazal
    L.placa(c, 0, 8, 3, 8, oro=True)
L.remache(k['derecha'], 1, 1); L.remache(k['izquierda'], 2, 1)
L.veta(k['delante'], [(1, 9), (2, 10)], AZ)
L.placa(k['arriba'], 0, 0, 3, 3, tono=1, semilla=35)

# Botas (pierna 4x12x4, solo abajo)
k = L.caras(0, 16, 4, 12, 4)
for nombre, semilla in (('delante', 40), ('derecha', 41), ('izquierda', 42), ('detras', 43)):
    c = k[nombre]
    L.placa(c, 0, 7, 3, 7, oro=True)                              # vuelta
    L.placa(c, 0, 8, 3, 9, semilla=semilla)
    L.placa(c, 0, 10, 3, 11, tono=-1, semilla=semilla + 5)
L.placa(k['delante'], 0, 10, 3, 11, tono=1, semilla=46)           # puntera más clara
L.pon(k['delante'], 1, 10, ORO[3]); L.pon(k['delante'], 2, 10, ORO[2])
L.veta(k['derecha'], [(1, 8), (2, 9)], AZ); L.veta(k['izquierda'], [(2, 8), (1, 9)], RO)
L.placa(k['abajo'], 0, 0, 3, 3, tono=-3, semilla=47)
L.im.save(f"{sys.argv[1]}/lucarita_layer_1.png")

# ======================= CAPA 2: pantalones =======================
L = Capa()
k = L.caras(16, 16, 8, 12, 4)
for nombre, semilla in (('delante', 50), ('derecha', 51), ('izquierda', 52), ('detras', 53)):
    c = k[nombre]
    w = c[2]
    L.placa(c, 0, 7, w - 1, 8, oro=True)                          # cinturón
    L.placa(c, 0, 9, w - 1, 11, semilla=semilla)                  # faldar
L.pon(k['delante'], 3, 7, AZ[2]); L.pon(k['delante'], 4, 7, RO[2])
L.rendija(k['delante'], [(3, y) for y in (10, 11)] + [(4, y) for y in (10, 11)])
k = L.caras(0, 16, 4, 12, 4)
for nombre, semilla in (('delante', 54), ('derecha', 55), ('izquierda', 56), ('detras', 57)):
    c = k[nombre]
    L.placa(c, 0, 0, 3, 4, semilla=semilla)                       # muslo
    L.placa(c, 0, 5, 3, 6, tono=1, semilla=semilla + 5)           # rodilla
    L.placa(c, 0, 7, 3, 9, tono=-1, semilla=semilla + 9)          # espinilla (debajo van las botas)
L.remache(k['delante'], 1, 5); L.pon(k['delante'], 2, 5, ORO[2])
L.veta(k['delante'], [(1, 1), (2, 2), (2, 3)], AZ)
L.veta(k['derecha'], [(1, 1), (2, 2)], AZ); L.veta(k['izquierda'], [(2, 1), (1, 2)], RO)
L.veta(k['detras'], [(2, 2), (1, 3)], RO)
L.placa(k['arriba'], 0, 0, 3, 3, semilla=58)
L.im.save(f"{sys.argv[1]}/lucarita_layer_2.png")
print("ok")
