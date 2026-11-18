"""Centinela de piedra: textura de entidad (128x128, para ModeloCentinela) y la de la cara que brilla.
Cuerpo, brazos y piernas de arenisca (bandas horizontales como el bloque de Minecraft, más lisa arriba, con
grietas), un disco de oro en el pecho, y la cabeza de calabaza tallada (naranja con surcos, rabo arriba y la cara
tallada, que brilla dorada por dentro). Uso: python3 dibujar_centinela.py <carpeta_salida>"""
import sys
from PIL import Image

ARENISCA = [(218, 206, 160), (206, 192, 146), (228, 218, 178), (190, 176, 128)]
BANDA = (184, 168, 120)
GRIETA = (150, 134, 92)
ORO = [(232, 184, 56), (206, 156, 40), (250, 216, 104)]
CALABAZA = [(227, 144, 29), (206, 121, 20), (240, 165, 48)]
SURCO = (180, 96, 14)
TALLADO = (70, 34, 8)
LUZ = (255, 226, 110)
RABO = [(110, 86, 36), (90, 70, 28)]


def h(x, y, s):
    n = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def tono(paleta, x, y, s):
    r = h(x, y, s)
    k = 0 if r < 0.55 else 1 if r < 0.8 else 2 if r < 0.94 else 3
    return paleta[min(k, len(paleta) - 1)]


TEX = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
OJOS = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
P, PO = TEX.load(), OJOS.load()


def caja(u, v, dx, dy, dz, pintar):
    caras = [("arriba", u + dz, v, dx, dz), ("abajo", u + dz + dx, v, dx, dz),
             ("lado_a", u, v + dz, dz, dy), ("frente", u + dz, v + dz, dx, dy),
             ("lado_b", u + dz + dx, v + dz, dz, dy), ("detras", u + 2 * dz + dx, v + dz, dx, dy)]
    for nombre, x0, y0, w, alto in caras:
        for j in range(alto):
            for i in range(w):
                c = pintar(nombre, i, j, w, alto, x0 + i, y0 + j)
                if c is not None:
                    P[x0 + i, y0 + j] = c + (255,)


def arenisca(c, i, j, w, a, x, y, s, pecho=False):
    if c in ("arriba", "abajo"):
        return tono(ARENISCA[:3], x, y, s)              # lisa
    if pecho and c == "frente":
        cx, cy = (w - 1) / 2, 5.5
        d2 = (i - cx) ** 2 + (j - cy) ** 2
        if d2 <= 2.5:
            return ORO[2]
        if d2 <= 7:
            return ORO[0]                               # disco de oro (redondo, sin rayos)
        if d2 <= 10:
            return ORO[1]
    if j % 6 == 5 or (j % 6 == 0 and h(x, y, s + 1) > 0.6):
        return BANDA                                    # bandas de la arenisca
    if h(x // 2, y // 3, s + 2) > 0.93:
        return GRIETA                                   # grietas
    return tono(ARENISCA, x, y, s)


caja(0, 0, 18, 18, 12, lambda c, i, j, w, a, x, y: arenisca(c, i, j, w, a, x, y, 1, pecho=True))
caja(0, 30, 6, 12, 6, lambda c, i, j, w, a, x, y: arenisca(c, i, j, w, a, x, y, 2))
caja(24, 30, 7, 13, 7, lambda c, i, j, w, a, x, y: arenisca(c, i, j, w, a, x, y, 3))
caja(52, 30, 7, 14, 7, lambda c, i, j, w, a, x, y: arenisca(c, i, j, w, a, x, y, 4))


def calabaza(c, i, j, w, a, x, y):
    if c == "arriba":
        cx = cy = (w - 1) / 2
        if abs(i - cx) <= 1 and abs(j - cy) <= 1:
            return tono(RABO, x, y, 5)                  # el rabo
        return tono(CALABAZA, x, y, 6)
    if c == "frente":
        # cara tallada: dos ojos triangulares y una boca dentada (brillan por dentro)
        ojo = lambda cx: (3 <= j <= 6 and abs(i - cx) <= (j - 3))
        boca = (9 <= j <= 11 and 2 <= i <= w - 3 and not (j == 9 and i % 3 == 0) and not (j == 11 and i % 3 == 1))
        if ojo(3) or ojo(10) or boca:
            PO[x, y] = LUZ + (255,)
            return TALLADO
    if i % 3 == 0 and c != "abajo":
        return SURCO                                    # surcos de la calabaza
    return tono(CALABAZA, x, y, 7)


caja(60, 0, 14, 14, 14, calabaza)

salida = sys.argv[1] if len(sys.argv) > 1 else "."
TEX.save(f"{salida}/centinela.png")
OJOS.save(f"{salida}/centinela_ojos.png")
