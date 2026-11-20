"""Vista previa del Rey de Arenisca (sin abrir el juego): las cajas de ModeloRey.java colocadas con el mismo
esqueleto que el juego (esqueleto_rey.py = EsqueletoRey.java), en varias poses.
Uso: python3 vista_rey.py <rey_arenisca.png> <salida.png>"""
import math
import sys

import numpy as np
from PIL import Image, ImageDraw

import esqueleto_rey as E

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





def caja_modelo(b):
    x0, y0, z0, x1, y1, z1 = E.CAJA[b]
    return (-2 * x1, -2 * y1, 2 * z0, 2 * (x1 - x0), 2 * (y1 - y0), 2 * (z1 - z0))


TEXOFFS = {E.CADERA: (0, 36), E.TORSO: (0, 0), E.CABEZA: (96, 0), E.BRAZO_I: (0, 58), E.BRAZO_D: (0, 58),
           E.ANTEBRAZO_I: (72, 58), E.ANTEBRAZO_D: (72, 58), E.MANO_I: (100, 58), E.MANO_D: (100, 58),
           E.MUSLO_I: (124, 58), E.MUSLO_D: (124, 58), E.ESPINILLA_I: (160, 58), E.ESPINILLA_D: (160, 58),
           E.PIE_I: (192, 58), E.PIE_D: (192, 58)}
SELLOS = [(E.ESPINILLA_D, (0, -4, -2.2)), (E.TORSO, (0, 6, -4.2)), (E.CABEZA, (0, 8.2, 0))]


def cubos_hueso(b):
    u, v = TEXOFFS[b]
    cs = [cubo(u, v, *caja_modelo(b))]
    if b == E.CABEZA:
        cs += [cubo(160, 0, -9, -20, 7, 18, 4, 2), cubo(160, 0, -9, -20, -9, 18, 4, 2),
               cubo(160, 8, 7, -20, -7, 2, 4, 14), cubo(160, 8, -9, -20, -7, 2, 4, 14)]
        for px, pz in ((0, 7), (0, -9), (7, -1), (-9, -1), (7, 7), (-9, 7), (7, -9), (-9, -9)):
            cs.append(cubo(200, 8, px, -25, pz, 2, 5, 2))
    if b in (E.BRAZO_I, E.BRAZO_D):
        cs.append(cubo(32, 58, -5, -2, -5, 10, 7, 10))
    return cs


def figura(accion, t, lado=1, rodillas=0.0, sellos_rotos=()):
    rot, alt = E.posar(accion, t, lado=lado, rodillas=rodillas)
    M = E.huesos(rot, alt)
    F = np.diag([-1.0, -1.0, 1.0, 1.0])
    S = np.diag([-0.5, -0.5, 0.5, 1.0])
    lista = []
    for b in range(E.N):
        poligonos(Parte("h", (0, 0, 0), (0, 0, 0), cubos_hueso(b)), F @ M[b] @ S, lista)
    for k, (b, c) in enumerate(SELLOS):
        T = np.eye(4); T[:3, 3] = c
        uv = (20, 90) if k in sellos_rotos else (0, 90)
        poligonos(Parte("s", (0, 0, 0), (0, 0, 0), [cubo(uv[0], uv[1], -2.5, -2.5, -2.5, 5, 5, 5)]), F @ M[b] @ T @ S, lista)
    return lista


ESC = 7
lienzo = Image.new("RGB", (1800, 760), (86, 96, 120))
d = ImageDraw.Draw(lienzo)
d.line([(0, 700), (1800, 700)], fill=(200, 184, 130), width=2)
poses = [("de pie (delante)", E.REPOSO, 0, 1, 0.0, math.radians(200)),
         ("de pie (espalda)", E.REPOSO, 0, 1, 0.0, math.radians(20)),
         ("de rodillas", E.REPOSO, 0, 1, 1.0, math.radians(140)),
         ("palmada", E.PALMADA, 70, 1, 0.0, math.radians(120)),
         ("barrido", E.BARRIDO, 39, 1, 0.0, math.radians(160)),
         ("coz atrás", E.COZ, 31, 1, 0.0, math.radians(110))]
for n, (nombre, acc, t, lado, rod, yaw) in enumerate(poses):
    dibujar(figura(acc, t, lado, rod), 1800, 760, yaw, ESC, 150 + n * 300, 700, lienzo)
    d.text((100 + n * 300, 720), nombre, fill=(255, 255, 255))
cerca = Image.new("RGB", (600, 600), (86, 96, 120))
dibujar(figura(E.REPOSO, 0), 600, 600, math.radians(180), 26, 300, 1080, cerca)
lienzo.paste(cerca.resize((420, 420)), (1380, 0))
lienzo.save(sys.argv[2])
