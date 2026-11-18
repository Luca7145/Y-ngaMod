"""Gnomo: textura de entidad (64x64, para ModeloGnomo). Gorro rojo con una banda dorada y una estrella,
túnica azul con cinturón, barba blanca, nariz sonrosada, botas marrones y una espada corta con guarda de oro.
Uso: python3 dibujar_gnomo.py <carpeta_salida>"""
import sys
from PIL import Image

ROJO = [(196, 36, 42), (172, 28, 34), (214, 58, 60)]
ORO = [(232, 184, 56), (250, 216, 104)]
AZUL = [(52, 92, 176), (42, 76, 152), (70, 112, 196)]
PIEL = [(236, 188, 160), (222, 172, 144)]
NARIZ = (226, 128, 120)
BARBA = [(238, 238, 232), (220, 220, 214), (250, 250, 246)]
BOTA = [(96, 62, 34), (80, 50, 28)]
CINTURON = (120, 80, 40)
ACERO = [(210, 216, 222), (186, 194, 202), (236, 240, 244)]
CUERO = (90, 56, 30)


def h(x, y, s):
    n = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def tono(paleta, x, y, s):
    r = h(x, y, s)
    k = 0 if r < 0.6 else 1 if r < 0.85 else 2
    return paleta[min(k, len(paleta) - 1)]


TEX = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
P = TEX.load()


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


# cuerpo (0,0) 4x4x3: túnica azul con cinturón y botones
caja(0, 0, 4, 4, 3, lambda c, i, j, w, a, x, y:
     CINTURON if j == 2 and c not in ("arriba", "abajo") else (ORO[0] if c == "frente" and i == 1 and j in (0, 3) else tono(AZUL, x, y, 1)))


# cabeza (0,8) 5x5x5: cara con ojos, pelo blanco por los lados y detrás
def cabeza(c, i, j, w, a, x, y):
    if c == "frente":
        if j == 2 and i in (1, 3):
            return (30, 40, 90)                         # ojos
        if j == 1 and i in (1, 3):
            return BARBA[1]                             # cejas blancas
        if j >= 3:
            return tono(BARBA, x, y, 2)                 # bigote/barba
        return tono(PIEL, x, y, 3)
    if c == "arriba":
        return tono(ROJO, x, y, 4)
    return tono(BARBA, x, y, 5) if j >= 1 else tono(ROJO, x, y, 5)


caja(0, 8, 5, 5, 5, cabeza)
caja(20, 8, 1, 2, 1, lambda c, i, j, w, a, x, y: NARIZ)
caja(24, 8, 4, 3, 1, lambda c, i, j, w, a, x, y: tono(BARBA, x, y, 6) if not (j == 2 and i in (0, 3)) else BARBA[1])
# gorro: ala con banda dorada y estrella delante, cono rojo
caja(0, 18, 6, 2, 6, lambda c, i, j, w, a, x, y:
     (ORO[1] if (c == "frente" and i in (2, 3)) else ORO[0]) if (j == 1 and c not in ("arriba", "abajo")) else tono(ROJO, x, y, 7))
caja(24, 18, 4, 4, 4, lambda c, i, j, w, a, x, y: tono(ROJO, x, y, 8))
caja(40, 18, 2, 4, 2, lambda c, i, j, w, a, x, y: ORO[1] if (c == "arriba") else tono(ROJO, x, y, 9))
# brazo (0,28) 2x4x2: manga azul, mano
caja(0, 28, 2, 4, 2, lambda c, i, j, w, a, x, y: tono(PIEL, x, y, 10) if j == a - 1 else tono(AZUL, x, y, 10))
# pierna (8,28) 2x3x2: pantalón azul oscuro y bota
caja(8, 28, 2, 3, 2, lambda c, i, j, w, a, x, y: tono(BOTA, x, y, 11) if j >= 1 or c == "abajo" else AZUL[1])
# espada
caja(16, 28, 1, 1, 2, lambda c, i, j, w, a, x, y: CUERO)
caja(22, 28, 3, 1, 1, lambda c, i, j, w, a, x, y: ORO[0])
caja(0, 36, 1, 1, 7, lambda c, i, j, w, a, x, y: ACERO[2] if c in ("arriba",) else tono(ACERO, x, y, 12))

salida = sys.argv[1] if len(sys.argv) > 1 else "."
TEX.save(f"{salida}/gnomo.png")
