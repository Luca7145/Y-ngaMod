"""León de guerra: textura de entidad (128x128, para ModeloLeon) y la de los ojos que brillan.

Materiales: pelaje de león dorado (más claro en la tripa y el pecho), melena castaña anaranjada,
piel morena en la cara y los brazos, coraza blanca con remates de oro y un sol en el pecho, hombreras y
brazales de oro, sable de acero con guarda de oro, escudo blanco con borde y sol de oro, arco de madera con
puntas de oro. Ojos dorados (brillan). Uso: python3 dibujar_leon.py <carpeta_salida>"""
import math
import sys
from PIL import Image

PELO = [(198, 152, 80), (182, 136, 66), (212, 168, 96), (164, 120, 58)]
TRIPA = [(226, 200, 152), (214, 186, 136), (236, 214, 170)]
MELENA = [(150, 84, 30), (126, 66, 22), (176, 104, 40), (102, 52, 18)]
PIEL = [(196, 146, 104), (182, 134, 94), (208, 160, 118)]
BLANCO = [(234, 230, 218), (218, 212, 196), (244, 242, 234)]
ORO = [(232, 184, 56), (206, 156, 40), (250, 216, 104), (176, 124, 30)]
ACERO = [(216, 222, 228), (190, 198, 206), (236, 240, 244)]
CUERO = [(96, 60, 32), (80, 48, 26)]
MADERA = [(124, 82, 42), (104, 68, 34), (140, 96, 50)]
GARRA = (54, 40, 30)
OSCURO = (40, 26, 18)


def h(x, y, s):
    n = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def tono(paleta, x, y, s):
    r = h(x, y, s)
    k = 0 if r < 0.5 else 1 if r < 0.75 else 2 if r < 0.92 else 3
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


# ---------- EL LEÓN ----------
def cuerpo(c, i, j, w, a, x, y):
    if c == "abajo" or (c in ("lado_a", "lado_b") and j >= a - 2):
        return tono(TRIPA, x, y, 1)
    if c == "frente":                                   # el pecho, donde nace el torso
        return tono(TRIPA if j > 3 else PELO, x, y, 2)
    if c == "arriba" and 2 <= i <= w - 3 and h(x, y, 3) > 0.7:
        return PELO[3]                                  # línea del lomo
    return tono(PELO, x, y, 4)


caja(0, 0, 10, 8, 20, cuerpo)
caja(60, 0, 4, 7, 4, lambda c, i, j, w, a, x, y: tono(PELO, x, y, 5))
caja(76, 0, 3, 5, 3, lambda c, i, j, w, a, x, y: tono(PELO, x, y, 6) if j < 4 else tono(TRIPA, x, y, 6))
caja(88, 0, 4, 1, 4, lambda c, i, j, w, a, x, y:
     GARRA if (c == "frente" and i % 2 == 0) or (c == "arriba" and j == 0 and i % 2 == 0) else tono(TRIPA, x, y, 7))
caja(60, 12, 2, 2, 7, lambda c, i, j, w, a, x, y: tono(PELO, x, y, 8))
caja(78, 12, 2, 2, 6, lambda c, i, j, w, a, x, y: tono(PELO, x, y, 9))
caja(94, 12, 3, 3, 3, lambda c, i, j, w, a, x, y: tono(MELENA, x, y, 10))     # borla de la cola

# ---------- EL GUERRERO ----------
def torso(c, i, j, w, a, x, y):
    if c == "arriba":
        return tono(PIEL, x, y, 11) if 2 <= i <= w - 3 else tono(ORO, x, y, 11)
    if j == 0:
        return tono(PIEL, x, y, 12) if c == "frente" and 3 <= i <= 5 else tono(ORO, x, y, 12)   # cuello
    if j >= a - 2:
        return tono(ORO, x, y, 13) if j == a - 2 else tono(CUERO, x, y, 13)                    # cinturón
    if c == "frente":
        # medallón en rombo con una gema en el centro (simétrico)
        cx, cy = (w - 1) // 2, 5
        d = abs(i - cx) + abs(j - cy)
        if d == 0:
            return (250, 250, 255)                      # gema
        if d == 1:
            return ORO[2]
        if d == 2:
            return ORO[3]                               # borde del rombo
        if i in (0, w - 1):
            return tono(ORO, x, y, 14)
    if c == "detras" and i in (0, w - 1, (w - 1) // 2):
        return tono(ORO, x, y, 15)
    return tono(BLANCO, x, y, 16)


caja(0, 30, 9, 13, 5, torso)


def cabeza(c, i, j, w, a, x, y):
    if c == "frente":
        if j <= 1:
            return tono(MELENA, x, y, 17)               # flequillo
        if j == 3 and i in (1, 2, 4, 5):
            if i in (2, 4):
                PO[x, y] = (255, 214, 90, 255)
                return (255, 214, 90)                   # ojos dorados
            return (240, 236, 226)
        if j == 2 and i in (1, 2, 4, 5):
            return MELENA[3]                            # cejas
        if j == 5 and i == 3:
            return (150, 100, 70)                       # nariz ancha, de león
        if j == 6 and 2 <= i <= 4:
            return (120, 70, 50)                        # boca
        if j == 7:
            return tono(MELENA, x, y, 18)               # barba
        return tono(PIEL, x, y, 19)
    if c in ("lado_a", "lado_b") and j >= 2:
        return tono(PIEL, x, y, 20) if j < 6 and i < 3 else tono(MELENA, x, y, 20)
    return tono(MELENA, x, y, 21)


caja(28, 30, 7, 8, 7, cabeza)


def melena(c, i, j, w, a, x, y):
    # mechones: franjas verticales de tonos y puntas más oscuras abajo
    base = MELENA[(i + (1 if h(x, 0, 22) > 0.5 else 0)) % 3]
    if j >= a - 2 and h(x, y, 23) > 0.4:
        return MELENA[3]
    if h(x, y, 24) > 0.85:
        return MELENA[2]
    return base


caja(56, 30, 11, 12, 6, melena)
caja(0, 92, 10, 3, 7, melena)      # collarín de melena bajo la cabeza


def brazo_alto(c, i, j, w, a, x, y):
    return tono(PIEL, x, y, 25)


caja(90, 30, 4, 7, 4, brazo_alto)
caja(90, 42, 4, 6, 4, lambda c, i, j, w, a, x, y:
     (tono(ORO, x, y, 26) if j in (1, 2, 3) else tono(PIEL, x, y, 26)) if c not in ("arriba", "abajo") else tono(PIEL, x, y, 27))
caja(106, 0, 5, 3, 5, lambda c, i, j, w, a, x, y:
     ORO[3] if (c in ("frente", "lado_a", "lado_b", "detras") and j == a - 1) else tono(ORO, x, y, 28))   # hombreras

# ---------- ARMAS ----------
caja(0, 50, 1, 1, 3, lambda c, i, j, w, a, x, y: tono(CUERO, x, y, 29))
caja(8, 50, 1, 3, 1, lambda c, i, j, w, a, x, y: tono(ORO, x, y, 30))
caja(0, 54, 1, 2, 14, lambda c, i, j, w, a, x, y:
     ACERO[2] if (c in ("lado_a", "lado_b") and j == a - 1) or c == "abajo" else tono(ACERO, x, y, 31))
caja(30, 54, 1, 2, 4, lambda c, i, j, w, a, x, y: ACERO[2] if c == "abajo" else tono(ACERO, x, y, 32))


def escudo(c, i, j, w, a, x, y):
    if c == "frente":
        if i in (0, w - 1) or j in (0, a - 1):
            return tono(ORO, x, y, 33)                  # borde
        # sol simétrico: disco en el centro y rayos rectos en cruz y en diagonal
        cx, cy = (w - 1) // 2, (a - 1) // 2
        dx, dy = i - cx, j - cy
        if dx * dx + dy * dy <= 2:
            return ORO[2]
        if max(abs(dx), abs(dy)) <= 3 and (dx == 0 or dy == 0 or abs(dx) == abs(dy)):
            return ORO[0]
        return tono(BLANCO, x, y, 34)
    if c == "detras":
        return tono(MADERA, x, y, 35)
    return tono(ORO, x, y, 36)


caja(40, 50, 11, 9, 1, escudo)
caja(64, 50, 3, 3, 1, lambda c, i, j, w, a, x, y: ORO[2] if (i == 1 and j == 1) else tono(ORO, x, y, 37))   # umbo
caja(70, 60, 1, 2, 3, lambda c, i, j, w, a, x, y: tono(CUERO, x, y, 38))
caja(78, 60, 1, 1, 6, lambda c, i, j, w, a, x, y: tono(MADERA, x, y, 39))
caja(92, 60, 1, 1, 3, lambda c, i, j, w, a, x, y: tono(ORO, x, y, 40))
caja(0, 72, 1, 0, 19, lambda c, i, j, w, a, x, y: (236, 232, 220))           # cuerda
caja(100, 60, 1, 13, 1, lambda c, i, j, w, a, x, y:
     ACERO[1] if j >= 11 else (BLANCO[0] if j <= 2 else tono(MADERA, x, y, 41)))   # flecha: plumas arriba, punta abajo

salida = sys.argv[1] if len(sys.argv) > 1 else "."
TEX.save(f"{salida}/leon_de_guerra.png")
OJOS.save(f"{salida}/leon_de_guerra_ojos.png")
TEX.resize((512, 512), Image.NEAREST).save(f"{salida}/leon_textura_grande.png")
