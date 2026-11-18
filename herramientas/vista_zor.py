"""Vista previa de Zor el quemado (sin abrir el juego), con las cajas de ModeloZor.java y ModeloZorTramo.java:
la cabeza y el cuerpo serpenteando (cada tramo mira al de delante, como en ZorRenderer).
Uso: python3 vista_zor.py <zor.png> <salida.png>"""
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




def cabeza(boca):
    hijos = [Parte("mandibula", (0, 3, -6), (boca, 0, 0), [cubo(60, 40, -6.5, 0, -18, 13, 4, 18)]),
             Parte("cI", (6, -7, 4), (0.45, 0.25, 0), [cubo(0, 64, -1.5, -1.5, 0, 3, 3, 22)]),
             Parte("cD", (-6, -7, 4), (0.45, -0.25, 0), [cubo(52, 64, -1.5, -1.5, 0, 3, 3, 22)]),
             Parte("bI", (6.5, 1, -20), (0.2, 0.9, 0), [cubo(104, 64, -0.5, -0.5, 0, 1, 1, 18)]),
             Parte("bD", (-6.5, 1, -20), (0.2, -0.9, 0), [cubo(144, 64, -0.5, -0.5, 0, 1, 1, 18)]),
             Parte("melena", (0, -7, 2), (0.5, 0, 0), [cubo(0, 92, -1, -10, 0, 2, 10, 12), cubo(28, 92, -7, -7, 2, 2, 7, 10),
                                                       cubo(28, 92, 5, -7, 2, 2, 7, 10)])]
    return Parte("cabeza", (0, 0, 0), (0, 0, 0), [cubo(0, 0, -10, -8, -10, 20, 14, 24), cubo(0, 40, -7, -5, -24, 14, 8, 14)], hijos)


def tramo(i, n):
    hijos = [Parte("pua", (0, -10, 0), (-0.35, 0, 0), [cubo(120, 128, -1, -7, -5, 2, 7, 10)])] if i < n - 1 else []
    if i in (1, 7):
        for lado in (1, -1):
            hijos.append(Parte("pata", (11 * lado, 6, -6), (0.6, 0, -0.5 * lado), [cubo(150 if lado > 0 else 172, 128, -2.5, 0, -2.5, 5, 12, 5)],
                               [Parte("garra", (0, 12, 0), (0, 0, 0), [cubo(196, 128, -3.5, 0, -5, 7, 2, 7)])]))
    if i == n - 1:
        hijos.append(Parte("cola", (0, 0, 10), (0, 0, 0), [cubo(0, 184, -6, -9, 0, 12, 18, 14)]))
    return Parte("tramo", (0, 0, 0), (0, 0, 0), [cubo(0, 128, -12, -10, -16, 24, 20, 32)], hijos)


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




def serpiente(n=14, espacio=29.6):
    """Coloca la cabeza y los tramos sobre una curva (en píxeles de modelo), como el renderer."""
    def curva(s):
        return np.array([28 * math.sin(s * 0.018), -10 * math.sin(s * 0.011), s])
    lista = []
    M = np.eye(4)
    poligonos(cabeza(0.55), M, lista)
    delante = curva(0)
    anchos = [2.0 if i < 3 else 2.0 + (0.9 - 2.0) * (i - 3) / (n - 4) for i in range(n)]
    for i in range(n):
        p = curva(espacio * (i + 1.1))
        d = delante - p
        yaw = math.atan2(-d[0], -d[2])
        pitch = math.atan2(d[1], math.hypot(d[0], d[2]))
        s = anchos[i] / 1.5
        T = np.eye(4); T[:3, 3] = p
        R = np.eye(4); R[:3, :3] = rot(0, yaw, 0) @ rot(pitch, 0, 0)
        S = np.diag([s, s, max(s, 1.05), 1.0])
        poligonos(tramo(i, n), T @ R @ S, lista)
        delante = p
    return lista


lienzo = Image.new("RGB", (1600, 700), (70, 64, 70))
lista = serpiente()
dibujar(lista, 1600, 700, math.radians(115), 1.55, 700, 300, lienzo)
cerca = Image.new("RGB", (700, 500), (70, 64, 70))
dibujar(poligonos(cabeza(0.6)), 700, 500, math.radians(35), 9, 350, 260, cerca)
lienzo.paste(cerca.resize((560, 400)), (1040, 300))
lienzo.save(sys.argv[2])
