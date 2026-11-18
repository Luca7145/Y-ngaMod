"""Vista previa del modelo del león de guerra (sin abrir el juego): mismas cajas, articulaciones y poses que
ModeloLeon.java. Dibuja: espadachín en guardia, espadachín cargando el espadazo y arquero tensando.
Uso: python3 vista_leon.py <leon_de_guerra.png> <salida.png>"""
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




def pata(nombre, x, z):
    baja = Parte("baja" + nombre, (0, 7, 0), (0, 0, 0), [cubo(76, 0, -1.5, 0, -1.5, 3, 5, 3), cubo(88, 0, -2, 4, -3, 4, 1, 4)])
    return Parte("pata" + nombre, (x, 3, z), (0, 0, 0), [cubo(60, 0, -2, 0, -2, 4, 7, 4)], [baja])


def brazo(nombre, lado, extras):
    ante = Parte("ante" + nombre, (0, 6, 0), (0, 0, 0), [cubo(90, 42, -2, 0, -2, 4, 6, 4)], extras)
    return Parte("brazo" + nombre, (lado * 6.5, -11, 0), (0, 0, 0),
                 [cubo(90, 30, -2, -1, -2, 4, 7, 4), cubo(106, 0, -2.5, -2, -2.5, 5, 3, 5)], [ante])


def modelo(arquero):
    sable = Parte("sable", (0, 5, -0.5), (0, 0, 0), [cubo(0, 50, -0.5, -0.5, 0, 1, 1, 3), cubo(8, 50, -0.5, -1.5, -1, 1, 3, 1),
                                                     cubo(0, 54, -0.5, -1, -15, 1, 2, 14)],
                  [Parte("punta", (0, 0, -15), (0.3, 0, 0), [cubo(30, 54, -0.5, -1, -4, 1, 2, 4)])])
    flecha = Parte("flecha", (0, 5, 0), (0, 0, 0), [cubo(100, 60, -0.5, 0, -0.5, 1, 13, 1)])
    escudo = Parte("escudo", (0, 3, 0), (0, 0, 0), [cubo(40, 50, -5.5, -4.5, -3.5, 11, 9, 1), cubo(64, 50, -1.5, -1.5, -4.5, 3, 3, 1)])
    arco = Parte("arco", (0, 5, 0), (0, 0, 0), [cubo(70, 60, -0.5, 0, -1.5, 1, 2, 3), cubo(78, 60, -0.5, 0.5, -7.5, 1, 1, 6),
                                               cubo(78, 60, -0.5, 0.5, 1.5, 1, 1, 6), cubo(92, 60, -0.5, -0.5, -10, 1, 1, 3),
                                               cubo(92, 60, -0.5, -0.5, 7, 1, 1, 3)])
    cabeza = Parte("cabeza", (0, -13, 0), (0, 0, 0), [cubo(28, 30, -3.5, -8, -3.5, 7, 8, 7), cubo(56, 30, -5.5, -10, -2.5, 11, 12, 6), cubo(0, 92, -5, -1, -3.5, 10, 3, 7)])
    torso = Parte("torso", (0, -4, -7.5), (0, 0, 0), [cubo(0, 30, -4.5, -13, -2.5, 9, 13, 5)],
                  [cabeza, brazo("D", -1, [flecha] if arquero else [sable]), brazo("I", 1, [arco] if arquero else [escudo])])
    cola = Parte("cola", (0, -3, 10), (-0.9, 0, 0), [cubo(60, 12, -1, -1, 0, 2, 2, 7)],
                 [Parte("cola2", (0, 0, 7), (0.6, 0, 0), [cubo(78, 12, -1, -1, 0, 2, 2, 6)],
                        [Parte("borla", (0, 0, 6), (0, 0, 0), [cubo(94, 12, -1.5, -1.5, 0, 3, 3, 3)])])])
    cuerpo = Parte("cuerpo", (0, 9, 0), (0, 0, 0), [cubo(0, 0, -5, -4, -10, 10, 8, 20)],
                   [pata("DI", 3, -7.5), pata("DD", -3, -7.5), pata("TI", 3, 7.5), pata("TD", -3, 7.5), cola, torso])
    return Parte("raiz", (0, 0, 0), (0, 0, 0), [], [cuerpo])


def B(m, n):
    return m.buscar(n)


def guardia(m):
    B(m, "brazoI").rot = [-0.55, -0.25, -0.15]
    B(m, "anteI").rot = [0, 0, 1.45]
    B(m, "brazoD").rot = [-0.6, 0, 0.35]
    B(m, "anteD").rot = [-0.9, 0, 0]
    B(m, "torso").rot[1] = 0.18


def espadazo_carga(m):
    guardia(m)
    B(m, "brazoD").rot = [-2.7, 0, 0.9]
    B(m, "anteD").rot = [-0.4, 0, 0]
    B(m, "torso").rot = [-0.15, -0.65, 0]
    B(m, "brazoI").rot[0] = -0.2


def disparar(m):
    B(m, "torso").rot[1] = 0.55
    B(m, "cabeza").rot[1] = -0.5
    B(m, "brazoI").rot = [-1.55, -0.45, 0]
    B(m, "anteI").rot = [0, 0, 0]
    B(m, "brazoD").rot = [-1.55, -0.45 - 0.5 * 0.75, 0]
    B(m, "anteD").rot = [-1.1 * 0.75, 0, 0]


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



poses = [(False, guardia, 40), (False, espadazo_carga, 25), (True, disparar, 60)]
lienzo = Image.new("RGB", (1200, 460), (60, 96, 120))
ImageDraw.Draw(lienzo).rectangle([0, 410, 1200, 460], fill=(150, 130, 70))   # suelo
for i, (arquero, pose, yaw) in enumerate(poses):
    m = modelo(arquero)
    for n in ("brazoD", "brazoI"):
        B(m, n).rot[2] = 0.08 if n == "brazoD" else -0.08
    for n in ("anteD", "anteI"):
        B(m, n).rot[0] = -0.25
    pose(m)
    dibujar(poligonos(m), 1200, 460, math.radians(yaw), 9.0, 200 + i * 400, 410 - 24 * 9, lienzo)
lienzo.save(sys.argv[2])
