"""El esqueleto del Rey de Arenisca, igual que EsqueletoRey.java (para las vistas previas y para comprobar poses).
Todo en bloques. Espacio local del rey: +y arriba, +z hacia delante (adonde mira), +x a su izquierda.
Cada hueso: M = padre @ T(sitio) @ Ry(y) @ Rx(x) @ Rz(z). Rx negativo = el miembro colgante va hacia delante."""
import math

import numpy as np

CADERA, TORSO, CABEZA, BRAZO_I, ANTEBRAZO_I, MANO_I, BRAZO_D, ANTEBRAZO_D, MANO_D, \
    MUSLO_I, ESPINILLA_I, PIE_I, MUSLO_D, ESPINILLA_D, PIE_D = range(15)
N = 15
PADRE = [-1, 0, 1, 1, 3, 4, 1, 6, 7, 0, 9, 10, 0, 12, 13]
SITIO = [(0, 0, 0), (0, 4, 0), (0, 10, 0), (9.5, 9, 0), (0, -10, 0), (0, -9, 0), (-9.5, 9, 0), (0, -10, 0),
         (0, -9, 0), (4, 0, 0), (0, -9, 0), (0, -8, 0), (-4, 0, 0), (0, -9, 0), (0, -8, 0)]
CAJA = [(-7, 0, -3.5, 7, 4, 3.5), (-7.5, 0, -4, 7.5, 10, 4), (-4, 0, -4, 4, 8, 4),
        (-2, -10, -2, 2, 0, 2), (-1.75, -9, -1.75, 1.75, 0, 1.75), (-2, -5, -1, 2, 0, 1),
        (-2, -10, -2, 2, 0, 2), (-1.75, -9, -1.75, 1.75, 0, 1.75), (-2, -5, -1, 2, 0, 1),
        (-2.25, -9, -2.25, 2.25, 0, 2.25), (-2, -8, -2, 2, 0, 2), (-2, -2, -1.5, 2, 0, 4.5),
        (-2.25, -9, -2.25, 2.25, 0, 2.25), (-2, -8, -2, 2, 0, 2), (-2, -2, -1.5, 2, 0, 4.5)]
ALTURA_CADERA = 19.0
BAJADA_RODILLAS = 8.5


def rx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]])


def ry(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]])


def rz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]])


def tr(x, y, z):
    m = np.eye(4); m[:3, 3] = (x, y, z)
    return m


def suave(x):
    x = min(1.0, max(0.0, x))
    return x * x * (3 - 2 * x)


def tramo(t, a, b):
    return suave((t - a) / (b - a))


# acciones (igual que ReyDeArenisca.java)
INTRO, REPOSO, CAMINAR, PISOTON, BARRIDO, PALMADA, RAYO, COZ, SACUDIDA, RUGIDO, TEMBLOR = range(11)
TICKS_SACUDIDA, AVISO_SACUDIDA = 60.0, 20.0


def posar(accion, t, lado=1, rodillas=0.0, andar=0.0, paso=0.0, muerte=0.0, mira_yaw=0.0, mira_pitch=0.0):
    """Devuelve (rot[N][3], altura de cadera, bajada de la raíz)."""
    r = [[0.0, 0.0, 0.0] for _ in range(N)]
    s = lado
    # reposo: brazos un poco abiertos y el codo algo doblado
    r[BRAZO_I][2] += 0.08
    r[BRAZO_D][2] -= 0.08
    r[ANTEBRAZO_I][0] -= 0.15
    r[ANTEBRAZO_D][0] -= 0.15
    # andar
    a = andar * (1 - rodillas)
    sp = math.sin(paso)
    r[MUSLO_I][0] += -sp * 0.3 * a
    r[MUSLO_D][0] += sp * 0.3 * a
    r[ESPINILLA_I][0] += max(0.0, sp) * 0.45 * a
    r[ESPINILLA_D][0] += max(0.0, -sp) * 0.45 * a
    r[BRAZO_I][0] += sp * 0.2 * a
    r[BRAZO_D][0] -= sp * 0.2 * a
    bajada = abs(math.cos(paso)) * 0.4 * a

    agacharse = 0.0          # se pone de rodillas para algunos ataques
    inclinar = 0.0           # el torso hacia delante
    if accion == PISOTON:
        L = tramo(t, 0, 30) if t < 42 else 1 - tramo(t, 42, 48)
        mi, es = (MUSLO_I, ESPINILLA_I) if s > 0 else (MUSLO_D, ESPINILLA_D)
        r[mi][0] += -1.35 * L
        r[es][0] += 1.55 * L
        r[TORSO][0] -= 0.12 * L
        r[BRAZO_I][2] += 0.3 * L
        r[BRAZO_D][2] -= 0.3 * L
    elif accion == BARRIDO:
        e = tramo(t, 0, 30) if t < 48 else 1 - tramo(t, 48, 80)
        agacharse = 0.7 * e
        inclinar = 0.9 * e
        giro = 0.75 * s if t < 30 else s * (0.75 - 2.0 * tramo(t, 30, 48))
        r[TORSO][1] += giro * e
        br = BRAZO_I if s > 0 else BRAZO_D
        r[br][0] += -1.44 * e
        r[br][2] += 0.15 * s * e
        r[br + 1][0] += 0.15 * e
        r[CABEZA][0] -= 0.5 * e
    elif accion == PALMADA:
        e = tramo(t, 0, 40) if t < 120 else 1 - tramo(t, 120, 150)
        agacharse = 0.78 * e
        inclinar = 1.0 * e
        br = BRAZO_I if s > 0 else BRAZO_D
        if t < 25:
            brazo = -2.4 * tramo(t, 0, 25)
        elif t < 40:
            brazo = -2.4 + 1.4 * ((t - 25) / 15.0) ** 2
        elif t < 120:
            brazo = -1.0
        else:
            brazo = -1.0 * (1 - tramo(t, 120, 150))
        r[br][0] += brazo
        r[br + 1][0] += 0.15          # quita la curva del codo: brazo recto
        r[br + 2][0] += -1.5708 * (tramo(t, 25, 40) if t < 120 else 1 - tramo(t, 120, 150))
        r[CABEZA][0] -= 0.6 * e
    elif accion == COZ:
        pierna = -0.35 * tramo(t, 0, 20) if t < 20 else -0.35 + 1.45 * tramo(t, 20, 28) if t < 28 else 1.1 if t < 34 else 1.1 * (1 - tramo(t, 34, 50))
        rodilla = 0.7 * tramo(t, 0, 20) if t < 20 else 0.7 * (1 - tramo(t, 20, 26)) if t < 28 else 0
        mi, es = (MUSLO_I, ESPINILLA_I) if s > 0 else (MUSLO_D, ESPINILLA_D)
        atras = max(0.0, pierna)
        r[mi][0] += pierna
        r[es][0] += rodilla
        r[TORSO][0] += 0.25 * atras
        r[CABEZA][0] -= 0.2 * atras
        r[BRAZO_I][0] -= 0.3 * atras
        r[BRAZO_D][0] -= 0.3 * atras
    elif accion == TEMBLOR:
        agacharse = (0.45 * tramo(t, 0, 20) if t < 20 else 0.45 * (1 - tramo(t, 20, 28)) if t < 28 else 0 if t < 40
                     else 0.5 * tramo(t, 40, 44) if t < 44 else 0.5 * (1 - tramo(t, 44, 64)))
        brazos = -0.8 * tramo(t, 0, 20) if t < 20 else -0.8 - 1.4 * tramo(t, 20, 28) if t < 40 else -2.2 * (1 - tramo(t, 40, 50))
        r[BRAZO_I][0] += brazos
        r[BRAZO_D][0] += brazos
        r[BRAZO_I][2] += 0.2
        r[BRAZO_D][2] -= 0.2
        r[CABEZA][0] -= 0.2 * (tramo(t, 0, 20) if t < 40 else 1 - tramo(t, 40, 50))
    elif accion == SACUDIDA:
        e = 0.12 * tramo(t, 0, AVISO_SACUDIDA) if t < AVISO_SACUDIDA else math.sin(math.pi * min(1.0, max(0.0, (t - AVISO_SACUDIDA) / (TICKS_SACUDIDA - AVISO_SACUDIDA))))
        r[TORSO][2] += math.sin(t * 1.1) * 0.16 * e
        r[TORSO][1] += math.sin(t * 0.7) * 0.12 * e
        r[CADERA][2] += math.sin(t * 1.1 + 1) * 0.05 * e
        r[CABEZA][0] += math.sin(t * 1.7) * 0.15 * e
        r[BRAZO_I][0] += math.sin(t * 0.9 + 1) * 0.4 * e
        r[BRAZO_D][0] += math.sin(t * 0.9 - 1) * 0.4 * e
        r[BRAZO_I][2] += (0.3 + math.sin(t * 1.3) * 0.2) * e
        r[BRAZO_D][2] -= (0.3 + math.sin(t * 1.3 + 2) * 0.2) * e
    elif accion == RUGIDO:
        e = tramo(t, 0, 15) if t < 45 else 1 - tramo(t, 45, 60)
        r[CABEZA][0] -= 0.5 * e
        r[BRAZO_I][2] += 0.8 * e
        r[BRAZO_D][2] -= 0.8 * e
        r[TORSO][0] -= 0.15 * e
    elif accion == RAYO:
        r[TORSO][0] += 0.1
        r[BRAZO_I][0] -= 0.2
        r[BRAZO_D][0] -= 0.2

    # la cabeza mira hacia donde apunta (mira_yaw/pitch: ya relativos al rey)
    r[CABEZA][1] += max(-1.0, min(1.0, mira_yaw))
    r[CABEZA][0] += max(-0.6, min(0.7, mira_pitch))

    # muerte: se queda tieso, cae de rodillas y se vence hacia delante
    if muerte > 0:
        m = tramo(muerte, 40, 110)
        agacharse = max(agacharse, m)
        inclinar = max(inclinar, 1.2 * tramo(muerte, 70, 140))
        r[CABEZA][0] += 0.5 * m
        r[BRAZO_I][2] -= 0.05 * m
        r[BRAZO_D][2] += 0.05 * m

    k = max(rodillas, agacharse)
    altura = ALTURA_CADERA - BAJADA_RODILLAS * k - bajada
    for mu, es, pi in ((MUSLO_I, ESPINILLA_I, PIE_I), (MUSLO_D, ESPINILLA_D, PIE_D)):
        r[mu][0] *= (1 - k)
        r[es][0] = r[es][0] * (1 - k) + 1.5708 * k
        r[pi][0] += -1.4 * k
    r[TORSO][0] += 0.2 * k + inclinar
    r[CABEZA][0] -= 0.15 * k
    return r, altura


def huesos(rot, altura, yaw=0.0, dy=0.0):
    M = [None] * N
    raiz = tr(0, dy, 0) @ ry(-yaw)
    for b in range(N):
        sx, sy, sz = SITIO[b]
        if b == CADERA:
            sy = altura
        padre = raiz if PADRE[b] < 0 else M[PADRE[b]]
        M[b] = padre @ tr(sx, sy, sz) @ ry(rot[b][1]) @ rx(rot[b][0]) @ rz(rot[b][2])
    return M


def esquinas(b, M):
    x0, y0, z0, x1, y1, z1 = CAJA[b]
    pts = [M[b] @ np.array([x, y, z, 1]) for x in (x0, x1) for y in (y0, y1) for z in (z0, z1)]
    return np.array([p[:3] for p in pts])
