"""Kit de dibujo para la lucarita: máscaras, sombreado con bisel y vetas."""
import math
from PIL import Image

T = (0, 0, 0, 0)
CONT = (38, 18, 4, 255)
# Cobre (de sombra a brillo)
C = [(78, 36, 4, 255), (112, 54, 4, 255), (150, 78, 12, 255), (196, 116, 40, 255), (230, 148, 62, 255), (248, 184, 104, 255), (255, 226, 180, 255)]
# Oro
G = [(110, 76, 18, 255), (165, 120, 34, 255), (214, 170, 58, 255), (246, 214, 112, 255)]
AZ = [(24, 40, 110, 255), (46, 84, 178, 255), (96, 150, 240, 255), (180, 215, 255, 255)]
RO = [(96, 12, 18, 255), (170, 30, 36, 255), (232, 74, 64, 255), (255, 170, 150, 255)]
NEG = (16, 10, 6, 255)
MAD = [(30, 20, 12, 255), (52, 34, 20, 255), (78, 52, 30, 255)]

class Lienzo:
    def __init__(self, w=32, h=32):
        self.w, self.h = w, h
        self.im = Image.new('RGBA', (w, h), T)
        self.p = self.im.load()
        self.lleno = set()

    def dentro(self, x, y):
        return 0 <= x < self.w and 0 <= y < self.h

    def poner(self, x, y, c):
        if self.dentro(x, y):
            self.p[x, y] = c
            self.lleno.add((x, y))

    def pieza(self, mascara, paleta, luz=(-1, -1), relieve=1.0, centro=None):
        """Pinta una pieza de metal: mascara = conjunto de (x, y). Sombreado por bisel (bordes de arriba-izq
        claros, de abajo-dcha oscuros) y un degradado suave hacia la luz."""
        xs = [x for x, _ in mascara]; ys = [y for _, y in mascara]
        if not xs: return
        cx = centro[0] if centro else (min(xs) + max(xs)) / 2
        cy = centro[1] if centro else (min(ys) + max(ys)) / 2
        r = max(max(xs) - min(xs), max(ys) - min(ys)) / 2 + 1
        n = len(paleta)
        for (x, y) in mascara:
            # degradado: más claro hacia la luz
            g = ((x - cx) * luz[0] + (y - cy) * luz[1]) / (r * 1.4)
            nivel = (n - 1) / 2 + g * (n - 1) / 2 * relieve
            # bisel
            arriba = (x, y - 1) not in mascara; izq = (x - 1, y) not in mascara
            abajo = (x, y + 1) not in mascara; der = (x + 1, y) not in mascara
            if arriba or izq: nivel += 1.6
            if abajo or der: nivel -= 1.6
            i = max(0, min(n - 1, round(nivel)))
            self.poner(x, y, paleta[i])

    def contorno(self, color=CONT):
        borde = []
        for y in range(self.h):
            for x in range(self.w):
                if (x, y) not in self.lleno and any((x + dx, y + dy) in self.lleno for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    borde.append((x, y))
        for x, y in borde:
            self.p[x, y] = color

    def linea(self, puntos, color, solo_en=None):
        for (x0, y0), (x1, y1) in zip(puntos, puntos[1:]):
            n = max(abs(x1 - x0), abs(y1 - y0), 1)
            for i in range(n + 1):
                x = round(x0 + (x1 - x0) * i / n); y = round(y0 + (y1 - y0) * i / n)
                if solo_en is None or (x, y) in solo_en:
                    self.poner(x, y, color)

    def vena(self, puntos, paleta, mascara):
        """Veta de lucarita: núcleo brillante con borde oscuro, solo dentro de la pieza."""
        pts = []
        for (x0, y0), (x1, y1) in zip(puntos, puntos[1:]):
            n = max(abs(x1 - x0), abs(y1 - y0), 1)
            for i in range(n + 1):
                pts.append((round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n)))
        for (x, y) in pts:
            for dx, dy in ((0, 1), (1, 0)):
                if (x + dx, y + dy) in mascara and (x + dx, y + dy) not in pts:
                    self.poner(x + dx, y + dy, paleta[0])
        for i, (x, y) in enumerate(pts):
            if (x, y) in mascara:
                self.poner(x, y, paleta[2] if i % 3 else paleta[3])

    def gema(self, x, y):
        """Gema de lucarita 3x3: mitad azul, mitad roja, con brillo y engaste de oro."""
        for dx in range(-2, 3):
            for dy in range(-2, 3):
                if abs(dx) + abs(dy) <= 3:
                    self.poner(x + dx, y + dy, G[1] if abs(dx) + abs(dy) == 3 or max(abs(dx), abs(dy)) == 2 else G[2])
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                c = AZ[2] if dx < 0 or (dx == 0 and dy < 0) else RO[2]
                if dx == 1 and dy == 1: c = RO[1]
                if dx == -1 and dy == 1: c = AZ[1]
                self.poner(x + dx, y + dy, c)
        self.poner(x - 1, y - 1, AZ[3])

    def guardar(self, ruta, escala=10):
        self.im.save(ruta)
        g = Image.new('RGBA', (self.w * escala + 10, self.h * escala + 10), (139, 139, 139, 255))
        r = self.im.resize((self.w * escala, self.h * escala), Image.NEAREST)
        g.paste(r, (5, 5), r)
        return g

def elipse(cx, cy, rx, ry):
    return {(x, y) for x in range(int(cx - rx) - 1, int(cx + rx) + 2) for y in range(int(cy - ry) - 1, int(cy + ry) + 2)
            if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1}

def rect(x0, y0, x1, y1):
    return {(x, y) for x in range(x0, x1 + 1) for y in range(y0, y1 + 1)}

def poligono(pts):
    xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
    res = set()
    for y in range(int(min(ys)), int(max(ys)) + 1):
        for x in range(int(min(xs)), int(max(xs)) + 1):
            px, py = x + 0.5, y + 0.5
            dentro = False
            j = len(pts) - 1
            for i in range(len(pts)):
                xi, yi = pts[i]; xj, yj = pts[j]
                if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi) + xi:
                    dentro = not dentro
                j = i
            if dentro: res.add((x, y))
    return res
