"""Gárgola: textura de entidad (64x64, para ModeloGargola) y la de los ojos rojos que brillan.
Piedra gris con vetas, grietas y algo de musgo, alas de piedra oscura con nervios, dientes y garras claras.
Uso: python3 dibujar_gargola.py <carpeta_salida>"""
import sys
from PIL import Image

PIEDRA = [(128, 128, 132), (112, 112, 118), (146, 146, 150), (98, 98, 104)]
OSCURA = [(78, 78, 86), (66, 66, 74), (90, 90, 98)]
NERVIO = (118, 118, 126)
GRIETA = (60, 60, 66)
MUSGO = [(70, 102, 52), (86, 120, 60)]
DIENTE = (222, 220, 206)


def h(x, y, s):
    n = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def tono(paleta, x, y, s):
    r = h(x, y, s)
    k = 0 if r < 0.5 else 1 if r < 0.78 else 2 if r < 0.93 else 3
    return paleta[min(k, len(paleta) - 1)]


TEX = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
OJOS = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
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


def piedra(x, y, s, musgo=0.0):
    if musgo and h(x // 2, y // 2, s + 9) > 1.0 - musgo:
        return tono(MUSGO, x, y, s)
    if h(x, y // 2, s + 5) > 0.95:
        return GRIETA
    return tono(PIEDRA, x, y, s)


caja(0, 0, 6, 7, 5, lambda c, i, j, w, a, x, y: piedra(x, y, 1, 0.15 if c == "arriba" or c == "detras" else 0.0))


def cabeza(c, i, j, w, a, x, y):
    if c == "frente":
        if j == 1 and i in (1, 3):
            PO[x, y] = (255, 40, 30, 255)
            return (40, 0, 0)                           # ojos
        if j == 0 and i in (0, 1, 3, 4):
            return PIEDRA[3]                            # ceño
    return piedra(x, y, 2, 0.1 if c == "arriba" else 0.0)


caja(0, 12, 5, 4, 5, cabeza)
caja(20, 12, 3, 2, 2, lambda c, i, j, w, a, x, y:
     (DIENTE if i % 2 == 0 else GRIETA) if (c in ("frente", "lado_a", "lado_b") and j == a - 1) else
     (GRIETA if (c == "frente" and j == 0 and i in (0, 2)) else piedra(x, y, 3)))
caja(30, 12, 1, 2, 1, lambda c, i, j, w, a, x, y: tono(OSCURA, x, y, 4) if j > 0 else (200, 196, 180))
caja(34, 12, 3, 1, 3, lambda c, i, j, w, a, x, y:
     (DIENTE if (i + j) % 2 == 0 else (90, 20, 20)) if c == "arriba" else piedra(x, y, 5))
caja(0, 21, 2, 6, 2, lambda c, i, j, w, a, x, y: DIENTE if j == a - 1 and c != "arriba" else piedra(x, y, 6))   # garras
caja(8, 21, 1, 1, 6, lambda c, i, j, w, a, x, y: piedra(x, y, 7))
caja(8, 28, 1, 1, 5, lambda c, i, j, w, a, x, y: piedra(x, y, 8))
caja(22, 28, 3, 1, 2, lambda c, i, j, w, a, x, y: tono(OSCURA, x, y, 9))
caja(30, 21, 2, 4, 2, lambda c, i, j, w, a, x, y: piedra(x, y, 10, 0.2))
caja(38, 21, 2, 4, 2, lambda c, i, j, w, a, x, y: piedra(x, y, 11))
caja(46, 21, 2, 1, 3, lambda c, i, j, w, a, x, y: DIENTE if c == "frente" and i == 0 else tono(OSCURA, x, y, 12))
# alas: huesos y membranas (planos)
caja(0, 36, 7, 1, 1, lambda c, i, j, w, a, x, y: tono(OSCURA, x, y, 13))
caja(16, 36, 6, 1, 1, lambda c, i, j, w, a, x, y: tono(OSCURA, x, y, 14))


def membrana(ancho, alto, x0, y0, s):
    for cara in (0, 1):
        for j in range(alto):
            for i in range(ancho):
                ii = i if cara == 0 else ancho - 1 - i
                # el borde de abajo hace ondas, como un ala de murciélago
                corte = alto - 1 - (1 if (ii % 3 == 1) else 0) - (ii * alto // (ancho * 3))
                if j > corte:
                    continue
                c = NERVIO if (ii % 3 == 0) else tono(OSCURA, x0 + i, y0 + j, s)
                P[x0 + cara * ancho + i, y0 + j] = c + (255,)


membrana(7, 6, 0, 40, 15)
membrana(6, 5, 16, 40, 16)

salida = sys.argv[1] if len(sys.argv) > 1 else "."
TEX.save(f"{salida}/gargola.png")
OJOS.save(f"{salida}/gargola_ojos.png")
