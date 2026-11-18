"""Vista previa del modelo del gnomo (sin abrir el juego), con las poses de ModeloGnomo.java:
corriendo, en picado con la espada abajo y estirado en el tajo relámpago.
Uso: python3 vista_gnomo.py <gnomo.png> <salida.png>"""
import math
import sys

import numpy as np
from PIL import Image, ImageDraw

TEX = Image.open(sys.argv[1]).convert("RGBA")
TP = TEX.load()


def rot(xr, yr, zr):
    cx, sx = math.cos(xr), math.sin(xr)
    cy, sy = math.cos(yr), math.sin(yr)
    cz, sz = math.cos(zr), math.sin(zr)
    rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return rz @ ry @ rx


class Parte:
    def __init__(self, nombre, pivote, rot_=(0, 0, 0), cubos=(), hijos=()):
        self.nombre, self.pivote, self.rot, self.cubos, self.hijos = nombre, list(pivote), list(rot_), list(cubos), list(hijos)

    def buscar(self, nombre):
        if self.nombre == nombre:
            return self
        for h in self.hijos:
            r = h.buscar(nombre)
            if r:
                return r


def cubo(u, v, x, y, z, dx, dy, dz):
    return (u, v, x, y, z, dx, dy, dz)




def modelo():
    g3 = Parte("gorro3", (0, -4, 0.3), (-0.35, 0, 0), [cubo(40, 18, -1, -4, -1, 2, 4, 2)])
    g2 = Parte("gorro2", (0, -2, 0), (-0.2, 0, 0), [cubo(24, 18, -2, -4, -2, 4, 4, 4)], [g3])
    gorro = Parte("gorro", (0, -4.5, 0), (0, 0, 0), [cubo(0, 18, -3, -2, -3, 6, 2, 6)], [g2])
    barba = Parte("barba", (0, -1, -2.6), (0, 0, 0), [cubo(24, 8, -2, 0, -0.5, 4, 3, 1)])
    cabeza = Parte("cabeza", (0, -4, 0), (0, 0, 0), [cubo(0, 8, -2.5, -5, -2.5, 5, 5, 5), cubo(20, 8, -0.5, -3, -3.5, 1, 2, 1)], [barba, gorro])
    espada = Parte("espada", (0, 3, 0), (0, 0, 0), [cubo(16, 28, -0.5, -0.5, 0, 1, 1, 2), cubo(22, 28, -1.5, -0.5, -1, 3, 1, 1),
                                                   cubo(0, 36, -0.5, -0.5, -8, 1, 1, 7)])
    brazoD = Parte("brazoD", (-3, -3.5, 0), (0, 0, 0), [cubo(0, 28, -1, -0.5, -1, 2, 4, 2)], [espada])
    brazoI = Parte("brazoI", (3, -3.5, 0), (0, 0, 0), [cubo(0, 28, -1, -0.5, -1, 2, 4, 2)])
    cuerpo = Parte("cuerpo", (0, 21, 0), (0, 0, 0), [cubo(0, 0, -2, -4, -1.5, 4, 4, 3)], [cabeza, brazoI, brazoD])
    return Parte("raiz", (0, 0, 0), (0, 0, 0), [], [cuerpo, Parte("piernaI", (1, 21, 0), (0, 0, 0), [cubo(8, 28, -1, 0, -1, 2, 3, 2)]),
                                                     Parte("piernaD", (-1, 21, 0), (0, 0, 0), [cubo(8, 28, -1, 0, -1, 2, 3, 2)])])


B = lambda m, n: m.buscar(n)


def correr(m):
    g = 1.0
    B(m, "piernaI").rot[0] = math.cos(g) * 1.4; B(m, "piernaD").rot[0] = math.cos(g + math.pi) * 1.4
    B(m, "cuerpo").rot[0] = 0.35; B(m, "cabeza").rot[0] = -0.28
    B(m, "brazoI").rot[0] = math.cos(g + math.pi) * 1.1; B(m, "brazoD").rot = [-0.35, 0, 0.2]
    B(m, "gorro2").rot[0] = -0.45; B(m, "gorro3").rot[0] = -0.7


def picado(m):
    B(m, "brazoD").rot = [-0.35, -0.5, 0]; B(m, "brazoI").rot = [-0.35, 0.5, 0]
    B(m, "espada").rot[0] = math.pi / 2 + 0.35
    B(m, "cuerpo").pivote[1] = 17; B(m, "piernaI").pivote[1] = 17; B(m, "piernaD").pivote[1] = 17
    B(m, "cabeza").rot[0] = 0.5; B(m, "gorro2").rot[0] = -1.0; B(m, "gorro3").rot[0] = -0.9; B(m, "barba").rot[0] = -0.9


def dash(m):
    B(m, "cuerpo").rot = [0.75, -0.2, 0]
    B(m, "brazoD").rot = [-math.pi / 2 - 0.75, 0, 0]; B(m, "espada").rot[0] = math.pi / 2
    B(m, "brazoI").rot[0] = 1.2; B(m, "piernaI").rot[0] = -1.1; B(m, "piernaD").rot[0] = 1.0
    B(m, "cabeza").rot[0] = -0.6; B(m, "gorro2").rot[0] = -1.2; B(m, "gorro3").rot[0] = -1.0; B(m, "barba").rot[0] = -0.8


def caras(u, v, x, y, z, dx, dy, dz):
    x1, y1, z1, x2, y2, z2 = x, y, z, x + dx, y + dy, z + dz
    V = {7: (x1, y1, z1), 0: (x2, y1, z1), 1: (x2, y2, z1), 2: (x1, y2, z1),
         3: (x1, y1, z2), 4: (x2, y1, z2), 5: (x2, y2, z2), 6: (x1, y2, z2)}
    f6, f7, f8, f9 = u, u + dz, u + dz + dx, u + dz + dx + dx
    f10, f11, f12, f13, f14 = u + dz + dx + dz, u + dz + dx + dz + dx, v, v + dz, v + dz + dy
    return [((4, 3, 7, 0), f7, f12, f8, f13), ((1, 2, 6, 5), f8, f13, f9, f12), ((7, 3, 6, 2), f6, f13, f7, f14),
            ((0, 7, 2, 1), f7, f13, f8, f14), ((5, 4, 0, 1), f8, f13, f10, f14), ((6, 5, 4, 3), f10, f13, f11, f14)], V


def poligonos(parte, M=np.eye(4), lista=None):
    lista = [] if lista is None else lista
    T = np.eye(4); T[:3, 3] = parte.pivote
    R = np.eye(4); R[:3, :3] = rot(*parte.rot)
    M = M @ T @ R
    for c in parte.cubos:
        lista_caras, V = caras(*c)
        for idx, u1, v1, u2, v2 in lista_caras:
            P = [M @ np.array(list(V[i]) + [1]) for i in idx]
            P = [p[:3] for p in P]
            nu, nv = int(round(abs(u2 - u1))), int(round(abs(v2 - v1)))
            for iu in range(nu):
                for iv in range(nv):
                    tu = int(min(u1, u2)) + iu
                    tv = int(min(v1, v2)) + iv
                    if not (0 <= tu < TEX.width and 0 <= tv < TEX.height):
                        continue
                    color = TP[tu, tv]
                    if color[3] == 0:
                        continue

                    def punto(uu, vv):
                        s = (uu - u1) / (u2 - u1)
                        t = (vv - v1) / (v2 - v1)
                        return P[1] * (1 - s) * (1 - t) + P[0] * s * (1 - t) + P[2] * (1 - s) * t + P[3] * s * t
                    q = [punto(tu, tv), punto(tu + 1, tv), punto(tu + 1, tv + 1), punto(tu, tv + 1)]
                    n = np.cross(q[1] - q[0], q[3] - q[0])
                    lista.append((q, color, n))
    for h in parte.hijos:
        poligonos(h, M, lista)
    return lista


def dibujar(lista, ancho, alto, yaw, escala, ox, oy, lienzo):
    d = ImageDraw.Draw(lienzo)
    cy, sy = math.cos(yaw), math.sin(yaw)
    pitch = 0.25
    cp, sp = math.cos(pitch), math.sin(pitch)
    luz = np.array([0.4, -0.8, -0.45]); luz /= np.linalg.norm(luz)
    tri = []
    for q, color, n in lista:
        pts = []
        for p in q:
            x, y, z = -p[0], -p[1], p[2]          # como el juego: x e y invertidos
            x, z = x * cy - z * sy, x * sy + z * cy
            y, z = y * cp - z * sp, y * sp + z * cp
            pts.append((x, y, z))
        prof = sum(p[2] for p in pts) / 4
        nn = n / (np.linalg.norm(n) + 1e-9)
        k = 0.55 + 0.45 * abs(float(nn @ luz))
        c = tuple(int(ch * k) for ch in color[:3])
        tri.append((prof, [(ox + p[0] * escala, oy - p[1] * escala) for p in pts], c))
    for _, pts, c in sorted(tri, key=lambda t: -t[0]):
        d.polygon(pts, fill=c)



lienzo = Image.new("RGB", (1200, 360), (90, 150, 80))
ImageDraw.Draw(lienzo).rectangle([0, 320, 1200, 360], fill=(80, 60, 40))
for i, (pose, yaw) in enumerate(((correr, 55), (picado, 35), (dash, 75))):
    m = modelo(); pose(m)
    dibujar(poligonos(m), 1200, 360, math.radians(yaw), 12.0, 200 + i * 400, 320 - 24 * 12, lienzo)
lienzo.save(sys.argv[2])
