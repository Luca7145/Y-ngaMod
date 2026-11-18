"""Vista previa del modelo del Astado (sin abrir el juego): monta las mismas cajas, articulaciones y poses que
ModeloAstado.java (de pie y a cuatro patas, quieto), las pinta con la textura y las dibuja en perspectiva.
Sirve para revisar proporciones y posturas. Uso: python3 vista_astado.py <astado.png> <salida.png>"""
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


def astas(lado):
    return Parte("asta", (lado * 2, -5.5, -1), (-0.3, 0, lado * 0.6),
                 [cubo(102, 8, -1, -4, -1, 2, 4, 2), cubo(78, 8, -0.5, -14, -0.5, 1, 10, 1)], [
        Parte("pa", (0, -5, 0), (0, 0, lado * 0.85), [cubo(84, 8, -0.5, -7, -0.5, 1, 7, 1)]),
        Parte("pb", (0, -13.5, 0), (-0.35, 0, -lado * 0.5), [cubo(90, 8, -0.5, -8, -0.5, 1, 8, 1)]),
        Parte("pm", (0, -10, 0), (0.2, 0, lado * 0.9), [cubo(84, 8, -0.5, -7, -0.5, 1, 7, 1)]),
        Parte("pc", (0, -3, 0), (-1.0, 0, -lado * 0.3), [cubo(96, 8, -0.5, -5, -0.5, 1, 5, 1)])])


def brazo(nombre, lado):
    mano = Parte("mano" + nombre, (0, 12, 0), (0, 0, 0), [cubo(24, 20, -1.5, 0, -1.5, 3, 2, 3),
                                                       cubo(38, 20, -1.5, 2, -1.5, 1, 4, 1), cubo(38, 20, 0.5, 2, -1.5, 1, 4, 1),
                                                       cubo(38, 20, -0.5, 2, 0.5, 1, 4, 1)])
    ante = Parte("ante" + nombre, (0, 12.5, 0), (0, 0, 0), [cubo(14, 20, -1, 0, -1, 2, 12, 2)], [mano])
    return Parte("brazo" + nombre, (lado * 5, -12, 0), (0, 0, -0.1 * lado), [cubo(0, 20, -1.5, -1, -1.5, 3, 14, 3)], [ante])


def pierna(nombre, lado):
    pie = Parte("pie" + nombre, (0, 11, 0), (0, 0, 0), [cubo(68, 20, -1.5, 0, -3, 3, 1, 4)])
    esp = Parte("esp" + nombre, (0, 12, 0), (0, 0, 0), [cubo(58, 20, -1, 0, -1, 2, 11, 2)], [pie])
    return Parte("pierna" + nombre, (lado * 2.5, 0, 0), (0, 0, 0), [cubo(44, 20, -1.5, 0, -1.5, 3, 12, 3)], [esp])


def modelo():
    vert = [cubo(70, 8, -0.5, -12.5 + i * 2.6, 2.5, 1, 1, 2) for i in range(5)]
    cabeza = Parte("cabeza", (0, -4, -0.5), (0, 0, 0), [cubo(28, 0, -3, -6, -4, 6, 6, 7), cubo(80, 0, -2, -3, -8, 4, 3, 4)],
                   [Parte("mandibula", (0, 0, -3), (0, 0, 0), [cubo(98, 0, -2, 0, -5, 4, 1, 5)]), astas(1), astas(-1)])
    cuello = Parte("cuello", (0, -13, -0.5), (-0.25, 0, 0), [cubo(56, 8, -1.5, -4, -1.5, 3, 4, 3)], [cabeza])
    torso = Parte("torso", (0, -2, 0), (0.32, 0, 0), [cubo(0, 0, -4, -13, -2.5, 8, 13, 5)] + vert,
                  [cuello, brazo("I", 1), brazo("D", -1)])
    cadera = Parte("cadera", (0, 0, 0), (0, 0, 0), [cubo(56, 0, -3.5, -2, -2, 7, 4, 4)], [torso])
    return Parte("raiz", (0, 0, 0), (0, 0, 0), [], [cadera, pierna("I", 1), pierna("D", -1)])


def posar(m, f):
    """Lo mismo que setupAnim (sin andar ni acciones), con f = 0 de pie y 1 a cuatro patas."""
    L = lambda a, b: a + (b - a) * f
    g = 1 - f
    c = m.buscar("cadera"); c.pivote[1] = L(0, 6); c.pivote[2] = 5 * f
    for n in ("piernaI", "piernaD"):
        p = m.buscar(n); p.pivote[1] = c.pivote[1] + 1; p.pivote[2] = c.pivote[2]
    inc = L(0.32, 1.45)
    m.buscar("torso").rot[0] = inc
    cu = m.buscar("cuello"); cu.rot[0] = L(-0.25, -0.75)
    m.buscar("cabeza").rot[0] = -inc - cu.rot[0]
    for n, lado in (("I", 1), ("D", -1)):
        b = m.buscar("brazo" + n)
        b.rot[0] = L(-inc + 0.12, -inc - 0.05)
        b.rot[2] = lado * (-0.1 - 0.12 * g)
        m.buscar("ante" + n).rot[0] = L(-0.15, -1.0)
        m.buscar("mano" + n).rot[0] = L(0.1, 1.0)
        pi = m.buscar("pierna" + n); pi.rot[0] = L(0, -0.85)
        e = m.buscar("esp" + n); e.rot[0] = L(0.05, 1.45)
        m.buscar("pie" + n).rot[0] = -pi.rot[0] - e.rot[0]
    m.buscar("mandibula").rot[0] = 0.12 + 0.15 * f + 0.2


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


vistas = []
for f in (0.0, 1.0):
    m = modelo()
    posar(m, f)
    vistas.append(poligonos(m))
lienzo = Image.new("RGB", (900, 620), (38, 44, 38))
ImageDraw.Draw(lienzo).rectangle([0, 570, 900, 620], fill=(52, 40, 30))   # suelo
for i, lista in enumerate(vistas):
    dibujar(lista, 900, 520, math.radians(35 if i == 0 else 60), 8.0, 210 + i * 460, 570 - 24 * 8, lienzo)
lienzo.save(sys.argv[2])
