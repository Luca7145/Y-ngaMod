"""El Rey de Arenisca: textura de entidad (256x128, para ModeloRey) y la capa que brilla (cara, runas y sellos).
Un centinela gigantesco: cuerpo de arenisca con bandas, grietas y ribetes de oro, cabeza de calabaza tallada con
corona de oro, y los ASIDEROS (muescas oscuras y raíces secas) en las zonas por donde se le puede escalar: la
parte de atrás de piernas, cadera, espalda y cabeza, el exterior de los brazos y el dorso de las manos.
Ojo: en el modelo, la cara "frente" es la ESPALDA del rey y "detras" su cara de delante.
Uso: python3 dibujar_rey.py <carpeta_salida>"""
import os
import sys

from PIL import Image

ARENISCA = [(218, 206, 160), (206, 192, 146), (228, 218, 178), (190, 176, 128)]
BANDA = (184, 168, 120)
GRIETA = (150, 134, 92)
ORO = [(232, 184, 56), (206, 156, 40), (250, 216, 104)]
ORO_OSCURO = (150, 104, 24)
CALABAZA = [(227, 144, 29), (206, 121, 20), (240, 165, 48)]
SURCO = (180, 96, 14)
TALLADO = (70, 34, 8)
LUZ = (255, 226, 110)
ASIDERO = (112, 84, 46)
ASIDERO_HONDO = (74, 52, 26)
RAIZ = [(132, 104, 60), (108, 82, 44)]
RUNA = (255, 214, 96)

TEX = Image.new("RGBA", (256, 128), (0, 0, 0, 0))
BRILLO = Image.new("RGBA", (256, 128), (0, 0, 0, 0))
P, PB = TEX.load(), BRILLO.load()


def h(x, y, s):
    n = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def tono(paleta, x, y, s):
    r = h(x, y, s)
    k = 0 if r < 0.55 else 1 if r < 0.8 else 2 if r < 0.94 else 3
    return paleta[min(k, len(paleta) - 1)]


def caja(u, v, dx, dy, dz, pintar):
    caras = [("arriba", u + dz, v, dx, dz), ("abajo", u + dz + dx, v, dx, dz),
             ("lado_a", u, v + dz, dz, dy), ("frente", u + dz, v + dz, dx, dy),
             ("lado_b", u + dz + dx, v + dz, dz, dy), ("detras", u + 2 * dz + dx, v + dz, dx, dy)]
    for nombre, x0, y0, w, alto in caras:
        for j in range(alto):
            for i in range(w):
                c = pintar(nombre, i, j, w, alto, x0 + i, y0 + j)
                if c is None:
                    continue
                brilla = False
                if len(c) == 2 and isinstance(c[1], bool):
                    c, brilla = c
                P[x0 + i, y0 + j] = c + (255,)
                if brilla:
                    PB[x0 + i, y0 + j] = c + (255,)


def piedra(i, j, w, a, x, y, s, bandas=4):
    """Arenisca como la del bloque: bandas horizontales y alguna grieta."""
    if bandas and j % bandas == bandas - 1:
        return BANDA if h(x, y, s) > 0.2 else GRIETA
    if h(x // 2, y, s + 1) > 0.965:
        return GRIETA
    return tono(ARENISCA, x, y, s)


def asideros(i, j, w, a, x, y, s):
    """Muescas talladas y raíces secas: aquí se puede agarrar uno (en el juego, clic derecho)."""
    fila = j // 5
    col = (i + (fila % 2) * 3) % 6
    if j % 5 == 2 and col in (1, 2, 3):
        return ASIDERO_HONDO
    if j % 5 in (1, 3) and col in (1, 2, 3):
        return ASIDERO
    if h(i // 2, y, s + 5) > 0.9 and i % 6 == 5:
        return tono(RAIZ, x, y, s)                        # raíces secas que cuelgan
    return None


def runa(i, j, w, a, x, y, s):
    """Runas finas que brillan (es un ser luminoso)."""
    if h(x // 2, y // 3, s + 9) > 0.93 and (i + j) % 2 == 0:
        return (RUNA, True)
    return None


def cuerpo(agarrable_en, ribete_arriba=False, ribete_abajo=False, s=1, bandas=4):
    def pintar(c, i, j, w, a, x, y):
        if ribete_arriba and c in ("lado_a", "lado_b", "frente", "detras") and j <= 1:
            return tono(ORO, x, y, s) if j == 0 else ORO_OSCURO
        if ribete_abajo and c in ("lado_a", "lado_b", "frente", "detras") and j >= a - 2:
            return tono(ORO, x, y, s) if j == a - 1 else ORO_OSCURO
        if c in agarrable_en:
            q = asideros(i, j, w, a, x, y, s)
            if q:
                return q
        q = runa(i, j, w, a, x, y, s)
        if q and c not in agarrable_en:
            return q
        return piedra(i, j, w, a, x, y, s, bandas)
    return pintar


# torso: la espalda ("frente") y los hombros ("arriba") se escalan; disco de oro en el pecho ("detras")
def torso(c, i, j, w, a, x, y):
    if c == "detras":
        cx, cy = w / 2 - 0.5, 7
        d = ((i - cx) ** 2 + (j - cy) ** 2) ** 0.5
        if d < 3.2:
            return (LUZ if d < 1.5 else tono(ORO, x, y, 3), d < 1.5)
        if d < 4.2:
            return ORO_OSCURO
    return cuerpo({"frente", "arriba"}, ribete_abajo=True, s=2)(c, i, j, w, a, x, y)


caja(0, 0, 30, 20, 16, torso)
# cadera: el cinturón de oro, y por detrás se escala
caja(0, 36, 28, 8, 14, cuerpo({"frente"}, ribete_arriba=True, ribete_abajo=True, s=3, bandas=0))


# la cabeza: calabaza tallada; la cara (delante = "detras") brilla; la nuca y los lados se escalan
def cabeza(c, i, j, w, a, x, y):
    if c == "detras":
        ojo = (3 <= i <= 5 or 10 <= i <= 12) and 4 <= j <= 6
        nariz = 7 <= i <= 8 and j == 8
        boca = 3 <= i <= 12 and 10 <= j <= 12 and not (j == 10 and i % 3 == 0) and not (j == 12 and i % 3 == 1)
        if ojo or nariz or boca:
            return (LUZ if (i + j) % 3 else (255, 196, 70), True)
    if c in ("frente", "lado_a", "lado_b"):
        q = asideros(i, j, w, a, x, y, 4)
        if q and j >= 2:
            return q
    if c == "arriba":
        cx, cy = w / 2 - 0.5, a / 2 - 0.5
        if abs(i - cx) < 1.1 and abs(j - cy) < 1.1:
            return tono([(110, 86, 36), (90, 70, 28)], x, y, 4)        # el rabo (cubierto por el sello)
    if i % 4 == 0 and c not in ("arriba", "abajo"):
        return SURCO
    return tono(CALABAZA, x, y, 4)


caja(96, 0, 16, 16, 16, cabeza)


def oro(c, i, j, w, a, x, y):
    if (i + j) % 7 == 0:
        return (tono(ORO, x, y, 5), True)
    return tono(ORO, x, y, 5) if j > 0 else ORO[2]


caja(160, 0, 18, 4, 2, oro)          # corona: delante y detrás
caja(160, 8, 2, 4, 14, oro)          # corona: lados
caja(200, 8, 2, 5, 2, lambda c, i, j, w, a, x, y: (ORO[2], True) if j == 0 else tono(ORO, x, y, 6))   # puntas

# brazos y piernas
caja(0, 58, 8, 20, 8, cuerpo({"lado_a", "frente"}, s=7))         # brazo: se escala por fuera y por detrás
caja(32, 58, 10, 7, 10, cuerpo(set(), ribete_abajo=True, s=8, bandas=3))   # hombrera
caja(72, 58, 7, 18, 7, cuerpo({"lado_a", "frente"}, ribete_arriba=True, s=9))   # antebrazo (brazalete de oro)
caja(100, 58, 8, 10, 4, cuerpo({"detras"}, s=10, bandas=3))      # mano: el dorso
caja(124, 58, 9, 18, 9, cuerpo({"frente"}, s=11))                # muslo: por detrás
caja(160, 58, 8, 16, 8, cuerpo({"frente"}, ribete_abajo=True, s=12))      # espinilla: por detrás
caja(192, 58, 8, 4, 12, cuerpo(set(), s=13, bandas=0))           # pie


# los sellos: oro con un ojo de luz (vivo) / piedra rota y apagada (roto)
def sello_vivo(c, i, j, w, a, x, y):
    if 1 <= i <= 3 and 1 <= j <= 3:
        return ((255, 250, 200) if i == 2 and j == 2 else LUZ, True)
    return (tono(ORO, x, y, 14), True)


def sello_roto(c, i, j, w, a, x, y):
    if (i + j) % 3 == 0:
        return (60, 50, 40)
    return GRIETA if h(x, y, 15) > 0.5 else (120, 104, 70)


caja(0, 90, 5, 5, 5, sello_vivo)
caja(20, 90, 5, 5, 5, sello_roto)

salida = sys.argv[1] if len(sys.argv) > 1 else "."
TEX.save(os.path.join(salida, "rey_arenisca.png"))
BRILLO.save(os.path.join(salida, "rey_arenisca_brillo.png"))
