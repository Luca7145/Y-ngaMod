"""Pez del viento: textura de entidad (256x128, para ModeloPez) y la de los ojos que brillan.
Cachalote blanco celestial: piel blanca cremosa con arrugas horizontales en los costados, cicatrices claras (de viejas
peleas), la cabeza con el orificio para respirar, la boca rosada por dentro con la hilera de dientes de marfil en la
mandíbula, la aleta de la cola con la muesca central, dos alas de plumas con el borde de oro, el halo dorado, y
constelaciones (estrellas unidas por líneas) y runas doradas por los costados, que brillan en la oscuridad. Uso: python3 dibujar_pez.py <carpeta_salida>"""
import sys
from PIL import Image

BLANCO = [(238, 236, 230), (226, 224, 216), (246, 245, 240), (214, 212, 204)]
ARRUGA = (204, 202, 194)
CICATRIZ = (252, 252, 250)
SOMBRA = (196, 194, 186)
BOCA = [(222, 150, 156), (206, 130, 138)]
DIENTE = (250, 242, 214)
OJO = (40, 44, 60)


def h(x, y, s):
    n = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def tono(paleta, x, y, s):
    r = h(x, y, s)
    k = 0 if r < 0.55 else 1 if r < 0.8 else 2 if r < 0.93 else 3
    return paleta[min(k, len(paleta) - 1)]


TEX = Image.new("RGBA", (256, 128), (0, 0, 0, 0))
OJOS = Image.new("RGBA", (256, 128), (0, 0, 0, 0))
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


def piel(c, i, j, w, a, x, y, s):
    if c == "abajo":
        return tono(BLANCO[1:], x, y, s)
    # cicatrices: rayas finas en diagonal, sueltas
    if h(x // 7, y // 5, s + 1) > 0.82 and (x + y) % 7 == 0:
        return CICATRIZ
    # arrugas horizontales en los costados (como un cachalote de verdad)
    if c in ("lado_a", "lado_b") and j % 3 == 2 and h(x // 4, y, s + 2) > 0.3:
        return ARRUGA
    if j >= a - 2 and c in ("lado_a", "lado_b", "frente", "detras"):
        return SOMBRA
    return tono(BLANCO, x, y, s)


def cabeza(c, i, j, w, a, x, y):
    if c == "arriba" and 3 <= i <= 4 and 4 <= j <= 6:
        return (150, 148, 142)                          # orificio para respirar
    if c in ("lado_a", "lado_b") and 15 <= j <= 16 and w // 2 - 1 <= i <= w // 2:
        PO[x, y] = (210, 236, 255, 255)
        return OJO                                      # el ojo
    if c == "abajo":
        return tono(BOCA, x, y, 3) if 6 <= i <= 13 else tono(BLANCO[1:], x, y, 3)   # el paladar, donde encaja la mandíbula
    return piel(c, i, j, w, a, x, y, 4)


def mandibula(c, i, j, w, a, x, y):
    if c == "arriba":
        if i in (0, w - 1):
            return DIENTE if j % 2 == 0 else (200, 120, 128)   # hilera de dientes
        return tono(BOCA, x, y, 5)
    return piel(c, i, j, w, a, x, y, 6)


caja(0, 0, 20, 22, 36, cabeza)
caja(112, 0, 22, 20, 26, lambda c, i, j, w, a, x, y: piel(c, i, j, w, a, x, y, 7))
caja(0, 58, 8, 4, 30, mandibula)
caja(76, 58, 18, 16, 22, lambda c, i, j, w, a, x, y: piel(c, i, j, w, a, x, y, 8))
caja(156, 58, 10, 10, 18, lambda c, i, j, w, a, x, y: piel(c, i, j, w, a, x, y, 9))


def aleta_cola(c, i, j, w, a, x, y):
    if c in ("arriba", "abajo"):
        # muesca en el centro del borde de atrás y borde un poco más oscuro
        if j >= a - 2 and abs(i - (w - 1) / 2) <= 1.5:
            return None if j == a - 1 else SOMBRA
        if j == a - 1:
            return SOMBRA
    return tono(BLANCO, x, y, 10)


caja(0, 96, 36, 2, 12, aleta_cola)
ORO = [(236, 190, 70), (250, 222, 120)]
CIELO = (196, 224, 255)


def ala(c, i, j, w, a, x, y, punta=False):
    """Ala de plumas: blanca, con plumas marcadas, borde dorado delante y el borde de atrás deshilachado."""
    if c in ("arriba", "abajo"):
        largo = w
        if j == 0:
            PO[x, y] = ORO[1] + (255,)
            return ORO[0]                               # borde de delante, dorado (brilla)
        # borde de atrás: plumas sueltas (huecos)
        if j >= a - 3 and (i % 3 == 2 or (j == a - 1 and i % 3 != 0)):
            return None
        if punta and i > largo - 4 and j > a - 1 - (largo - i) * 2:
            return None                                 # la punta se afila
        if i % 3 == 0 and j > 2:
            return SOMBRA                               # el cañón de cada pluma
        return tono(BLANCO[:3], x, y, 12)
    return ORO[0]


caja(96, 96, 34, 1, 16, ala)
caja(0, 112, 20, 1, 12, lambda c, i, j, w, a, x, y: ala(c, i, j, w, a, x, y, True))


def halo(c, i, j, w, a, x, y):
    PO[x, y] = ORO[1] + (255,)
    return ORO[(i + j) % 2]


caja(196, 96, 5, 1, 2, halo)


# ---------- LUZ: constelaciones y runas en la piel (capa que brilla) ----------
def constelaciones(u, v, dx, dy, dz, s):
    """Estrellas sueltas y unas líneas que las unen, en los costados; y una línea de runas doradas por el medio."""
    for (x0, ancho) in ((u, dz), (u + dz + dx, dz)):          # los dos costados
        y0 = v + dz
        estrellas = []
        for k in range(max(3, ancho // 6)):
            ex = x0 + int(h(k, s, 1) * (ancho - 2)) + 1
            ey = y0 + int(h(k, s, 2) * (dy - 4)) + 1
            estrellas.append((ex, ey))
            PO[ex, ey] = CIELO + (255,)
            P[ex, ey] = (250, 252, 255, 255)
        for a_, b_ in zip(estrellas, estrellas[1:]):
            if abs(a_[0] - b_[0]) + abs(a_[1] - b_[1]) > 14:
                continue
            pasos = max(abs(a_[0] - b_[0]), abs(a_[1] - b_[1]))
            for t_ in range(1, pasos):
                lx = round(a_[0] + (b_[0] - a_[0]) * t_ / pasos)
                ly = round(a_[1] + (b_[1] - a_[1]) * t_ / pasos)
                if (lx + ly) % 2 == 0:
                    PO[lx, ly] = (120, 160, 220, 255)       # línea tenue
        runa_y = y0 + dy // 2
        for xx in range(x0 + 1, x0 + ancho - 1):
            if xx % 4 != 3:
                PO[xx, runa_y] = ORO[1] + (255,)            # runas doradas
                P[xx, runa_y] = ORO[1] + (255,)


constelaciones(0, 0, 20, 22, 36, 31)
constelaciones(112, 0, 22, 20, 26, 32)
constelaciones(76, 58, 18, 16, 22, 33)
constelaciones(156, 58, 10, 10, 18, 34)

salida = sys.argv[1] if len(sys.argv) > 1 else "."
TEX.save(f"{salida}/pez_del_viento.png")
OJOS.save(f"{salida}/pez_del_viento_ojos.png")
