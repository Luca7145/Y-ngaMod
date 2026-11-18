"""Zor el quemado: textura de entidad (256x256, para ModeloZor y ModeloZorTramo) y la capa de brasas que brillan.
Escamas de carbón con grietas de brasa, vientre de placas rojizas, cuernos y garras de hueso tostado, ojos de fuego,
la garganta encendida y el penacho de llamas de la cola.
Uso: python3 dibujar_zor.py <carpeta_salida>"""
import math
import os
import sys

from PIL import Image

CARBON = [(36, 31, 31), (29, 25, 26), (46, 39, 37), (23, 20, 21)]
BORDE = (58, 48, 44)
VIENTRE = [(118, 42, 28), (134, 54, 34), (98, 34, 24)]
JUNTA = (60, 20, 16)
HUESO = [(214, 202, 180), (192, 178, 156), (166, 150, 130)]
HUESO_TOSTADO = (92, 74, 60)
BRASA = [(255, 128, 32), (255, 178, 64), (236, 78, 22)]
BOCA = (70, 10, 8)
DIENTE = (232, 224, 204)

TEX = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
LUZ = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
P, PL = TEX.load(), LUZ.load()


def h(x, y, s):
    n = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def tono(paleta, x, y, s):
    r = h(x, y, s)
    k = 0 if r < 0.5 else 1 if r < 0.78 else 2 if r < 0.93 else 3
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
                if isinstance(c, tuple) and len(c) == 2 and isinstance(c[1], bool):
                    c, brilla = c
                P[x0 + i, y0 + j] = c + (255,)
                if brilla:
                    PL[x0 + i, y0 + j] = c + (255,)


def grieta(x, y, s, densidad=0.10):
    """Grietas onduladas (líneas de brasa entre las escamas)."""
    a = math.sin(x * 0.42 + 2.6 * math.sin(y * 0.23 + s)) + math.sin(y * 0.57 + 2.1 * math.sin(x * 0.19 + s * 2))
    return abs(a) < densidad


def escama(i, j, x, y, s, grietas=0.10):
    if grietas and grieta(x, y, s, grietas):
        return (tono(BRASA, x, y, s), True)
    fila = j // 3
    if j % 3 == 0 or (i + (fila % 2) * 2) % 4 == 0:
        return BORDE if h(x, y, s + 1) > 0.25 else CARBON[0]
    return tono(CARBON, x, y, s)


def vientre(i, j, x, y, s, a_lo_largo):
    k = j if a_lo_largo else i
    if k % 4 == 0:
        return JUNTA
    if h(x, y, s + 3) > 0.96:
        return (tono(BRASA, x, y, s), True)
    return tono(VIENTRE, x, y, s)


def hueso(i, j, w, a, x, y, s, punta_en_alto=True):
    return tono(HUESO, x, y, s) if h(x, y, s) > 0.1 else HUESO_TOSTADO


# ---------------- LA CABEZA ----------------
def craneo(c, i, j, w, a, x, y):
    # ojos de fuego: en los laterales, arriba y hacia delante, y en las esquinas del frente
    if c == "lado_a" and 2 <= j <= 4 and w - 6 <= i <= w - 2:
        return ((255, 210, 80) if j == 3 and i in (w - 4, w - 3) else BRASA[2], True)
    if c == "lado_b" and 2 <= j <= 4 and 1 <= i <= 5:
        return ((255, 210, 80) if j == 3 and i in (2, 3) else BRASA[2], True)
    if c in ("lado_a", "lado_b") and j == 1 and ((c == "lado_a" and w - 8 <= i <= w - 1) or (c == "lado_b" and 0 <= i <= 7)):
        return CARBON[3]                                   # ceja
    if c == "frente" and 1 <= j <= 3 and (i <= 2 or i >= w - 3):
        return (BRASA[0], True)
    if c == "abajo":
        return vientre(i, j, x, y, 4, True)
    if c in ("lado_a", "lado_b") and j >= a - 3:
        return vientre(i, j, x, y, 5, False)
    return escama(i, j, x, y, 6, 0.12 if c == "arriba" else 0.08)


def hocico(c, i, j, w, a, x, y):
    if c == "frente":
        if j == 2 and i in (3, 4, w - 5, w - 4):
            return (BRASA[1], True)                        # ollares encendidos
        if j >= a - 2:
            return DIENTE if i % 2 == 0 else BOCA          # dientes de arriba
    if c in ("lado_a", "lado_b") and j >= a - 2:
        return DIENTE if (i % 3 == 0) else (BOCA, False)
    if c == "abajo":
        return (BOCA if h(x, y, 7) > 0.15 else BRASA[2], True)   # el paladar, al rojo
    return escama(i, j, x, y, 8, 0.1)


def mandibula(c, i, j, w, a, x, y):
    if c == "arriba":
        if i <= 1 or i >= w - 2:
            return DIENTE if j % 2 == 0 else BOCA          # dientes de abajo
        return ((255, 96, 24) if 3 <= i <= w - 4 and h(x, y, 9) > 0.35 else (120, 18, 10), True)  # la garganta encendida
    if c == "frente" and j == 0:
        return DIENTE if i % 2 == 0 else BOCA
    if c in ("lado_a", "lado_b") and j == 0:
        return DIENTE if i % 3 == 0 else BOCA
    if c == "abajo":
        return vientre(i, j, x, y, 10, True)
    return escama(i, j, x, y, 11, 0.08)


caja(0, 0, 20, 14, 24, craneo)
caja(0, 40, 14, 8, 14, hocico)
caja(60, 40, 13, 4, 18, mandibula)


def cuerno(c, i, j, w, a, x, y):
    # hueso que se tuesta hacia la punta (z alto = punta) y acaba en brasa
    lejos = (j / max(1, a - 1)) if c in ("arriba", "abajo") else (i / max(1, w - 1))
    if c in ("frente", "detras"):
        lejos = 0.0 if c == "frente" else 1.0
    if lejos > 0.92:
        return (BRASA[0], True)
    if lejos > 0.65:
        return HUESO_TOSTADO if h(x, y, 12) > 0.4 else HUESO[2]
    return tono(HUESO, x, y, 13) if (x + y) % 5 else HUESO[2]


caja(0, 64, 3, 3, 22, cuerno)
caja(52, 64, 3, 3, 22, cuerno)


def barba(c, i, j, w, a, x, y):
    lejos = (j / max(1, a - 1)) if c in ("arriba", "abajo") else (i / max(1, w - 1))
    if c == "detras" or lejos > 0.8:
        return (BRASA[1] if lejos > 0.9 or c == "detras" else BRASA[2], True)
    return CARBON[2] if (x + y) % 3 else BORDE


caja(104, 64, 1, 1, 18, barba)
caja(144, 64, 1, 1, 18, barba)


def puas(c, i, j, w, a, x, y):
    if c in ("lado_a", "lado_b", "frente", "detras") and j <= 1:
        return (BRASA[0], True)                            # el filo, al rojo
    return HUESO_TOSTADO if h(x, y, 14) > 0.45 else CARBON[2]


caja(0, 92, 2, 10, 12, puas)
caja(28, 92, 2, 7, 10, puas)

# ---------------- EL CUERPO (los tramos) ----------------


def tramo(c, i, j, w, a, x, y):
    if c == "abajo":
        return vientre(i, j, x, y, 20, True)
    if c in ("lado_a", "lado_b"):
        if j >= a - 5:
            return vientre(i, j, x, y, 21, True)           # el vientre sube un poco por los lados
        if j == a - 6:
            return JUNTA
    if c == "arriba" and (w // 2 - 2) <= i <= (w // 2 + 1):
        return (BRASA[2], True) if j % 4 == 0 else CARBON[3]   # la cresta del lomo, con brasas
    if c in ("frente", "detras"):
        # el corte del tramo (casi no se ve: queda dentro del de al lado)
        return CARBON[3] if not grieta(x, y, 22, 0.25) else (BRASA[2], True)
    return escama(i, j, x, y, 23, 0.11)


caja(0, 128, 24, 20, 32, tramo)


def pua(c, i, j, w, a, x, y):
    if j <= 1 and c not in ("arriba", "abajo"):
        return (BRASA[0], True)
    return HUESO_TOSTADO if h(x, y, 24) > 0.5 else CARBON[2]


caja(120, 128, 2, 7, 10, pua)


def pata(c, i, j, w, a, x, y):
    if c in ("lado_a", "lado_b", "frente", "detras") and j >= a - 2:
        return BORDE
    return escama(i, j, x, y, 25, 0.12)


caja(150, 128, 5, 12, 5, pata)
caja(172, 128, 5, 12, 5, pata)


def garra(c, i, j, w, a, x, y):
    if c == "frente" or (c == "arriba" and j <= 1):
        return tono(HUESO, x, y, 26) if i % 2 == 0 else HUESO_TOSTADO   # las uñas
    return escama(i, j, x, y, 27, 0.0)


caja(196, 128, 7, 2, 7, garra)


def llamas(c, i, j, w, a, x, y):
    # el penacho de la cola: llamas de amarillo (dentro) a rojo (las puntas), todo brilla
    if c in ("arriba", "abajo"):
        lejos = j / max(1, a - 1)
    elif c in ("lado_a", "lado_b"):
        lejos = (i / max(1, w - 1)) if c == "lado_b" else 1 - i / max(1, w - 1)
    else:
        lejos = 0.2 if c == "frente" else 0.9
    lengua = 0.25 * math.sin(j * 0.9 + i * 0.5) + 0.25 * h(x, y, 28)
    v = lejos + lengua
    if v < 0.45:
        col = (255, 226, 120)
    elif v < 0.75:
        col = BRASA[1]
    elif v < 1.0:
        col = BRASA[0]
    else:
        col = BRASA[2]
    return (col, True)


caja(0, 184, 12, 18, 14, llamas)

salida = sys.argv[1] if len(sys.argv) > 1 else "."
TEX.save(os.path.join(salida, "zor.png"))
LUZ.save(os.path.join(salida, "zor_brasas.png"))
