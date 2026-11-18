"""Dibujo (32x32) del taladro del Ingeniero: apagado y encendido (con combustible).
Uso: python dibujar_taladro.py <carpeta_salida>   (necesita kit.py en la misma carpeta)"""
import sys, math, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from kit import *

ACERO = [(36, 38, 44, 255), (58, 62, 70, 255), (84, 89, 98, 255), (116, 122, 132, 255), (152, 158, 168, 255), (196, 201, 208, 255), (236, 238, 242, 255)]
PUNTA = [(70, 74, 82, 255), (110, 116, 126, 255), (150, 156, 166, 255), (190, 196, 204, 255), (225, 229, 234, 255), (250, 251, 253, 255)]
COBRE = C          # el cobre de la lucarita (de kit)
NEGRO = (20, 20, 24, 255)
GOMA = [(28, 26, 26, 255), (44, 40, 40, 255), (62, 56, 54, 255)]
LUZ = [(120, 40, 0, 255), (220, 100, 20, 255), (255, 170, 60, 255), (255, 230, 150, 255)]

S = (13.0, 19.0)                    # centro del motor
def uv(x, y):
    dx, dy = x + 0.5 - S[0], y + 0.5 - S[1]
    return (dx - dy) / math.sqrt(2), (dx + dy) / math.sqrt(2)   # u: hacia arriba-derecha (la broca), v: de lado

def zona(f):
    return {(x, y) for x in range(32) for y in range(32) if f(*uv(x, y))}

def dibujar(encendido):
    L = Lienzo()
    empunadura = zona(lambda u, v: -11 <= u <= -4 and abs(v) <= 1.6)
    gatillo = zona(lambda u, v: -6 <= u <= -4.5 and 1.6 < v <= 3.2)
    motor = zona(lambda u, v: -4.5 <= u <= 5 and (u / 5.2) ** 2 + (v / 4.3) ** 2 <= 1.6 and abs(v) <= 4.3)
    portabrocas = zona(lambda u, v: 5 < u <= 7.5 and abs(v) <= 2.6)
    broca = zona(lambda u, v: 7.5 < u <= 18 and abs(v) <= 2.2 * (1 - (u - 7.5) / 10.8) + 0.3)
    tubo = zona(lambda u, v: -3 <= u <= 1 and -5.6 <= v < -4.3)

    L.pieza(empunadura, GOMA, relieve=1.0)
    for (x, y) in empunadura:                                    # estrías de la goma
        u, v = uv(x, y)
        if int(u) % 2 == 0 and abs(v) < 1.2:
            L.poner(x, y, GOMA[0])
    L.pieza(gatillo, ACERO[1:5], relieve=1.0)
    L.pieza(motor, ACERO, relieve=1.1, centro=(12, 17))
    L.pieza(tubo, COBRE[1:6], relieve=1.0)
    # franjas de cobre (lucarita) en el motor
    for (x, y) in motor:
        u, v = uv(x, y)
        if abs(u - 3.6) < 0.6 or abs(u + 3.2) < 0.6:
            L.poner(x, y, COBRE[4] if v < 0 else COBRE[2])
    # ventanita del motor: apagada o con el fuego dentro
    ventana = zona(lambda u, v: -1.8 <= u <= 1.8 and -1.6 <= v <= 1.6)
    for (x, y) in ventana:
        u, v = uv(x, y)
        if encendido:
            c = LUZ[3] if abs(u) < 0.6 and abs(v) < 0.6 else (LUZ[2] if abs(u) + abs(v) < 2 else LUZ[1])
        else:
            c = NEGRO if abs(u) + abs(v) < 2 else ACERO[0]
        L.poner(x, y, c)
    for (x, y) in ventana:                                      # marco de la ventana
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) in motor and (x + dx, y + dy) not in ventana:
                L.poner(x + dx, y + dy, COBRE[1])
    L.pieza(portabrocas, G, relieve=1.0)
    # broca con la espiral
    L.pieza(broca, PUNTA, relieve=1.2, centro=(22, 8))
    for (x, y) in broca:
        u, v = uv(x, y)
        if ((u * 1.3 + v) % 2.6) < 0.9:
            L.poner(x, y, ACERO[1] if v > 0 else PUNTA[0])
    L.contorno(NEGRO)
    if encendido:                                               # humito del tubo (sin contorno)
        for (x, y), c in (((9, 11), (150, 150, 150, 210)), ((8, 10), (175, 175, 175, 170)), ((8, 9), (200, 200, 200, 130)), ((9, 8), (220, 220, 220, 90))):
            L.p[x, y] = c
    return L

salida = sys.argv[1] if len(sys.argv) > 1 else '.'
g1 = dibujar(False).guardar(f'{salida}/taladro.png')
g2 = dibujar(True).guardar(f'{salida}/taladro_cargado.png')
from PIL import Image
g = Image.new('RGBA', (g1.width * 2, g1.height)); g.paste(g1, (0, 0)); g.paste(g2, (g1.width, 0)); g.save(f'{salida}/taladro_grande.png')
