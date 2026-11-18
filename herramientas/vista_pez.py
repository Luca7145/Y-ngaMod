"""Vista previa del modelo del Pez del viento (sin abrir el juego), con las cajas de ModeloPez.java:
nadando (la ola del cuerpo) y con la boca abierta para el rayo.
Uso: python3 vista_pez.py <pez_del_viento.png> <salida.png>"""
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




def modelo(boca):
    aleta = Parte("aletaCola", (0, 0, 18), (0.25, 0, 0), [cubo(0, 96, -18, -1, 0, 36, 2, 12)])
    cola = Parte("cola", (0, -1, 22), (0.15, 0, 0), [cubo(156, 58, -5, -5, 0, 10, 10, 18)], [aleta])
    cuerpo2 = Parte("cuerpo2", (0, 0, 13), (0.07, 0, 0), [cubo(76, 58, -9, -8, 0, 18, 16, 22)], [cola])
    mand = Parte("mandibula", (0, 10, -6), (boca, 0, 0), [cubo(0, 58, -4, 0, -30, 8, 4, 30)])
    halo = Parte("halo", (0, -22, -16), (0, 0.2, 0), [], [Parte("h" + str(k), (0, 0, 0), (0, k * math.pi / 4, 0),
                                                                [cubo(196, 96, -2.5, -0.5, 9, 5, 1, 2)]) for k in range(8)])
    cabeza = Parte("cabeza", (0, 0, -13), (0, 0, 0), [cubo(0, 0, -10, -12, -36, 20, 22, 36)], [mand, halo])
    aI = Parte("aI", (11, 4, -8), (0, 0.3, -0.25), [cubo(96, 96, 0, -0.5, -8, 34, 1, 16)],
               [Parte("pI", (34, 0, 2), (0, 0, 0.15), [cubo(0, 112, 0, -0.5, -6, 20, 1, 12)])])
    aD = Parte("aD", (-11, 4, -8), (0, -0.3, 0.25), [cubo(96, 96, -34, -0.5, -8, 34, 1, 16)],
               [Parte("pD", (-34, 0, 2), (0, 0, -0.15), [cubo(0, 112, -20, -0.5, -6, 20, 1, 12)])])
    cuerpo = Parte("cuerpo", (0, -4, 0), (0, 0, 0), [cubo(112, 0, -11, -10, -13, 22, 20, 26)], [cabeza, aI, aD, cuerpo2])
    return Parte("raiz", (0, 0, 0), (0, 0, 0), [], [cuerpo])


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



lienzo = Image.new("RGB", (1400, 560), (40, 60, 110))
for i, (boca, yaw) in enumerate(((0.03, 60), (0.6, 30))):
    m = modelo(boca)
    dibujar(poligonos(m), 1400, 560, math.radians(yaw), 3.2, 360 + i * 700, 330, lienzo)
lienzo.save(sys.argv[2])
