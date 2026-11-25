"""Texturas de los habitantes (época medieval de prueba) en el estilo clásico de Minecraft: skins de 64x64 como las
de Steve y Alex, en un estilo sencillo y con personalidad (nada realista): ojos grandes de 2x2 con pestañas en las
mujeres (estilo "anime" pequeño: ojos grandes abajo, flequillo largo de mechones con brillo y sombra), boca diminuta; cejas
gruesas sobre cada ojo y nariz con luz y sombra en los hombres. Piel y telas con una textura suave. Se pueden retocar con
cualquier editor de skins. Capas, una encima de otra:

  piel_<sexo>_<tono>_<cara>.png  cuerpo con ropa interior y la cara                             6 tonos x 6 caras x 2
  ojos_<sexo>_<color>.png        los ojos                                                       6 colores x 2
  ropa/<epoca>_<sexo>_<n>.png    la ropa: 3 cortes x 4 colores por sexo (n = corte*4 + color)   + el bebé
  pelo_<sexo>_<estilo>.png       pelo y cejas en GRIS: el juego lo tiñe con el color de pelo    8 peinados x 2
  barba_<n>.png                  barba en gris, también teñida (solo hombres adultos)          5 barbas
  pelo3d.png                     piezas de pelo en 3D (coleta, moño, trenzas, flequillo, melena), en gris

Cara: (col, fila) de 0 a 7 dentro del frente de la cabeza (x 8..15, y 8..15).

Uso: python3 dibujar_habitantes.py <carpeta src/main/resources>"""
import os
import sys

from PIL import Image

TRANSPARENTE = (0, 0, 0, 0)


# ================================================================= formato de skin

def caras(u, v, w, h, d):
    return {
        "arriba": (u + d, v, u + d + w, v + d),
        "abajo": (u + d + w, v, u + d + 2 * w, v + d),
        "derecha": (u, v + d, u + d, v + d + h),
        "frente": (u + d, v + d, u + d + w, v + d + h),
        "izquierda": (u + d + w, v + d, u + 2 * d + w, v + d + h),
        "detras": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }


def cajas(brazo):
    return {
        "cabeza": (0, 0, 8, 8, 8), "cabeza_capa": (32, 0, 8, 8, 8),
        "cuerpo": (16, 16, 8, 12, 4), "cuerpo_capa": (16, 32, 8, 12, 4),
        "brazo_d": (40, 16, brazo, 12, 4), "brazo_d_capa": (40, 32, brazo, 12, 4),
        "brazo_i": (32, 48, brazo, 12, 4), "brazo_i_capa": (48, 48, brazo, 12, 4),
        "pierna_d": (0, 16, 4, 12, 4), "pierna_d_capa": (0, 32, 4, 12, 4),
        "pierna_i": (16, 48, 4, 12, 4), "pierna_i_capa": (0, 48, 4, 12, 4),
    }


def mezclar(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def oscuro(color, f=0.8):
    return tuple(max(0, min(255, int(c * f))) for c in color[:3])


def ruido(x, y, semilla=0):
    """Número pseudoaleatorio estable por píxel (0..1)."""
    n = (x * 374761393 + y * 668265263 + semilla * 982451653) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return (n ^ (n >> 16)) / 0xFFFFFFFF


class Lienzo:
    def __init__(self, brazo=4, semilla=0):
        self.img = Image.new("RGBA", (64, 64), TRANSPARENTE)
        self.cajas = cajas(brazo)
        self.semilla = semilla

    def punto(self, x, y, color):
        if 0 <= x < 64 and 0 <= y < 64:
            self.img.putpixel((x, y), tuple(color[:3]) + (255,))

    def borrar(self, x, y):
        if 0 <= x < 64 and 0 <= y < 64:
            self.img.putpixel((x, y), TRANSPARENTE)

    def leer(self, x, y):
        return self.img.getpixel((x, y))

    def textura(self, x, y, color, fuerza):
        """Tono de un material con textura suave: la mayoría de píxeles igual, algunos algo más claros u oscuros."""
        if not fuerza:
            return color
        r = ruido(x, y, self.semilla)
        if r < 0.18:
            return oscuro(color, 1.0 - fuerza)
        if r > 0.86:
            return oscuro(color, 1.0 + fuerza * 0.7)
        return color

    def rect(self, r, color, fuerza=0.0):
        x0, y0, x1, y1 = r
        for y in range(y0, y1):
            for x in range(x0, x1):
                self.punto(x, y, self.textura(x, y, color, fuerza))

    def cara(self, nombre, cual):
        u, v, w, h, d = self.cajas[nombre]
        return caras(u, v, w, h, d)[cual]

    def caja(self, nombre, color, filas=None, fuerza=0.05, solo=None):
        """Pinta las caras de una caja. filas = (desde, hasta) en altura; arriba/abajo solo si tocan el borde."""
        u, v, w, h, d = self.cajas[nombre]
        for cara, (x0, y0, x1, y1) in caras(u, v, w, h, d).items():
            if solo and cara not in solo:
                continue
            if cara in ("arriba", "abajo"):
                if filas is None or (cara == "arriba" and filas[0] == 0) or (cara == "abajo" and filas[1] == h):
                    self.rect((x0, y0, x1, y1), color, fuerza)
                continue
            a, b = filas if filas else (0, h)
            self.rect((x0, y0 + a, x1, y0 + b), color, fuerza)

    def fila(self, nombre, fila, color, solo=("derecha", "frente", "izquierda", "detras"), fuerza=0.04):
        self.caja(nombre, color, filas=(fila, fila + 1), solo=solo, fuerza=fuerza)

    def guardar(self, ruta):
        os.makedirs(os.path.dirname(ruta), exist_ok=True)
        self.img.save(ruta)


EXTREMIDADES = ("brazo_d", "brazo_i", "pierna_d", "pierna_i")
BRAZOS = ("brazo_d", "brazo_i")
PIERNAS = ("pierna_d", "pierna_i")
LADOS = ("derecha", "frente", "izquierda", "detras")
CARA_X, CARA_Y = 8, 8


def cf(c, col, fila, color):
    c.punto(CARA_X + col, CARA_Y + fila, color)


# ================================================================= piel y cara (estilo "cute")

TONOS = [(250, 222, 200), (238, 200, 168), (222, 178, 140), (198, 146, 108), (160, 108, 72), (110, 72, 48)]
INTERIOR = (234, 228, 210)
LABIOS = (214, 110, 122)
RUBOR = (240, 140, 140)
PECAS = (170, 110, 76)
CARAS = 6


def piel(sexo, tono, variante):
    """Cara de 8x8 sencilla y con personalidad: sin realismo. Ojos (en su capa) en las filas 3-5, rubor y boca abajo.
    Variantes: 0 normal · 1 rubor fuerte (hombres: cicatriz) · 2 pecas · 3 lunar · 4 sonrisa · 5 mofletes rojos."""
    mujer = sexo == "mujer"
    c = Lienzo(3 if mujer else 4, semilla=tono * 31 + variante * 7 + (1 if mujer else 0))
    base = TONOS[tono]
    for nombre in ("cabeza", "cuerpo") + EXTREMIDADES:
        c.caja(nombre, base, fuerza=0.03)
    c.rect(c.cara("cabeza", "abajo"), oscuro(base, 0.86))
    for brazo in BRAZOS:
        c.fila(brazo, 11, oscuro(base, 0.94))

    sombra = oscuro(base, 0.88)
    luz = oscuro(base, 1.06)
    for fila in range(1, 5):                                      # sombra bajo el pelo, a los lados
        cf(c, 0, fila, sombra)
        cf(c, 7, fila, sombra)
    if mujer:
        # Ojos grandes abajo (filas 5-6, en su capa); debajo, en la última fila: rubor y una boca diminuta
        for col in range(8):                                      # la piel algo más clara y rosada en la cara
            for fila in range(3, 8):
                cf(c, col, fila, mezclar(base, (255, 236, 236), 0.12))
        rubor = mezclar(base, RUBOR, 0.55 if variante in (1, 5) else 0.36)
        cf(c, 1, 7, rubor)
        cf(c, 6, 7, rubor)
        if variante in (1, 5):
            cf(c, 2, 7, mezclar(base, RUBOR, 0.3))
            cf(c, 5, 7, mezclar(base, RUBOR, 0.3))
        if variante in (2, 3, 4):                                  # las demás, sin boca: todo en los ojos
            cf(c, 3, 7, mezclar(base, LABIOS, 0.55))
            cf(c, 4, 7, mezclar(base, LABIOS, 0.7))
    else:
        cf(c, 3, 4, luz)                                          # nariz: luz y sombra
        cf(c, 3, 5, luz)
        cf(c, 4, 5, oscuro(base, 0.86))
        for col in (3, 4):                                        # boca
            cf(c, col, 6, oscuro(base, 0.68))
        cf(c, 2, 6, oscuro(base, 0.82))
        cf(c, 5, 6, oscuro(base, 0.82))
        if variante == 5:
            for col in (0, 7):
                cf(c, col, 5, mezclar(base, RUBOR, 0.3))
    if variante == 1 and not mujer:
        cf(c, 6, 2, mezclar(base, (250, 232, 224), 0.6))          # cicatriz
        cf(c, 6, 5, mezclar(base, (250, 232, 224), 0.6))
    if variante == 2:
        pecas = ((1, 6), (2, 5), (5, 5), (6, 6)) if not mujer else ((2, 7), (0, 7), (5, 7), (7, 7))
        for col, fila in pecas:
            cf(c, col, fila, mezclar(base, PECAS, 0.4 if mujer else 0.45))
    if variante == 3:
        if mujer:
            cf(c, 5 if tono % 2 else 2, 7, oscuro(base, 0.6))   # lunar
        else:
            cf(c, 6 if tono % 2 else 1, 7, oscuro(base, 0.55))
    if variante == 4:                                             # sonrisa
        if mujer:
            cf(c, 2, 7, mezclar(base, LABIOS, 0.35))
            cf(c, 5, 7, mezclar(base, LABIOS, 0.35))
            cf(c, 3, 7, mezclar(base, LABIOS, 0.8))
            cf(c, 4, 7, mezclar(base, LABIOS, 0.8))
        else:
            cf(c, 2, 5, oscuro(base, 0.72))
            cf(c, 5, 5, oscuro(base, 0.72))

    # ropa interior: calzón en caderas y muslos; las mujeres, camisola hasta debajo del pecho
    c.caja("cuerpo", INTERIOR, filas=(10, 12), fuerza=0.03)
    for pierna in PIERNAS:
        c.caja(pierna, INTERIOR, filas=(0, 4), fuerza=0.03)
    if mujer:
        c.caja("cuerpo", INTERIOR, filas=(0, 6), fuerza=0.03)
    return c


# ================================================================= ojos

OJOS = [(110, 72, 42), (70, 46, 30), (74, 120, 206), (84, 156, 96), (150, 118, 64), (110, 120, 140)]
BLANCO = (244, 244, 240)
PESTANAS = (44, 30, 30)


def ojos(sexo, color):
    """Ojos grandes de 2x2 (blanco y tres tonos de iris) con pestañas encima en las mujeres; los hombres, de una
    fila, con el iris oscuro."""
    c = Lienzo()
    iris = OJOS[color]
    claro = mezclar(iris, (255, 255, 255), 0.45)
    hondo = oscuro(iris, 0.55)
    if sexo == "mujer":
        # Pestañas en la fila 4; debajo, ojo de 2x2: la columna de fuera, blanca entera (la de arriba algo en
        # sombra bajo la pestaña); la de dentro, el iris (oscuro arriba, más claro abajo)
        for fuera, dentro in ((1, 2), (6, 5)):
            cf(c, fuera, 4, PESTANAS)
            cf(c, dentro, 4, PESTANAS)
            cf(c, fuera, 5, oscuro(BLANCO, 0.9))
            cf(c, fuera, 6, BLANCO)
            cf(c, dentro, 5, hondo)
            cf(c, dentro, 6, iris)
    else:
        for fuera, dentro in ((1, 2), (6, 5)):
            cf(c, fuera, 4, BLANCO)
            cf(c, dentro, 4, oscuro(iris, 0.75))
    return c


# ================================================================= pelo (gris, se tiñe)

G_BRILLO, G_LUZ, G, G_SOMBRA, G_OSCURO, G_HONDO = (255, 255, 255), (236, 236, 236), (204, 204, 204), \
    (166, 166, 166), (128, 128, 128), (94, 94, 94)


def tipo_mechon(x, semilla=0):
    """Cada columna es un mechón: claro, medio, en sombra o medio (con algo de azar, estable)."""
    return int(ruido(x, 7, 11 + semilla) * 4)


def tono_mechon(x, y, desde, hasta, semilla=0):
    """Pelo con mechones verticales de cuatro tonos, un brillo arriba (el "anillo" de luz), sombra hacia las puntas
    y algún pelito suelto más claro u oscuro."""
    t = tipo_mechon(x, semilla)
    largo = max(1, hasta - desde)
    rel = (y - desde) / largo
    tono = [G_LUZ, G, G_SOMBRA, G][t]
    if rel < 0.25 and t != 2:
        tono = G_BRILLO if t == 0 else G_LUZ
    elif rel > 0.7:
        tono = G_SOMBRA if t != 2 else G_OSCURO
    if y == hasta - 1 and largo > 2:
        tono = G_OSCURO if t == 2 else G_SOMBRA
    r = ruido(x, y, 29 + semilla)
    if r < 0.1:
        tono = oscuro(tono, 0.88)
    elif r > 0.93:
        tono = oscuro(tono, 1.08)
    return tono


def mechones(c, r, luz_arriba=True):
    x0, y0, x1, y1 = r
    for y in range(y0, y1):
        for x in range(x0, x1):
            c.punto(x, y, tono_mechon(x, y, y0 if luz_arriba else y0 - 5, y1, c.semilla))


def flequillo(c, largos, sombra=True):
    """Flequillo sobre la frente: largos[col] = filas que cubre en esa columna. Cada columna es un mechón con su
    tono, brillo arriba y punta oscura; debajo de cada punta, una sombra suave sobre la piel."""
    for col, largo in enumerate(largos):
        t = tipo_mechon(col + 3, c.semilla)
        for fila in range(largo):
            if fila == largo - 1 and largo > 1:
                tono = G_OSCURO if t == 2 else G_SOMBRA
            elif fila == largo - 2 and largo > 3:
                tono = G_SOMBRA if t != 0 else G
            elif fila == 1:
                tono = G_BRILLO if t == 0 else (G_LUZ if t != 2 else G)
            else:
                tono = [G_LUZ, G, G_SOMBRA, G][t]
            cf(c, col, fila, tono)
        if sombra and 0 < largo < 8:
            # Sombra que proyecta la punta sobre la frente (medio transparente: se tiñe del color del pelo)
            x, y = CARA_X + col, CARA_Y + largo
            if c.leer(x, y)[3] == 0 and tipo_mechon(col + 3, c.semilla) != 0:
                c.punto(x, y, G_HONDO)


CAPA_X, CAPA_Y = 40, 8          # frente de la capa exterior de la cabeza


def flequillo_profundo(c, largos):
    """Flequillo en dos capas, como las skins con volumen: en la cabeza, el pelo de dentro, más oscuro; en la capa
    exterior (medio píxel por delante), los mechones claros con huecos entre ellos. Al moverse se ve la profundidad."""
    for col, largo in enumerate(largos):
        for fila in range(largo):
            t = tipo_mechon(col + 3, c.semilla)
            tono = G_OSCURO if fila >= largo - 1 else (G_SOMBRA if t != 0 else G)
            cf(c, col, fila, tono)
        if 0 < largo < 8:
            c.punto(CARA_X + col, CARA_Y + largo, G_HONDO)       # sombra sobre la frente
    for col, largo in enumerate(largos):
        t = tipo_mechon(col + 3, c.semilla)
        if t == 2 and 0 < col < 7:
            continue                                               # hueco: se ve el pelo oscuro de dentro
        hasta = largo if t == 0 or col in (0, 7) else max(1, largo - 1)
        for fila in range(hasta):
            if fila == 1:
                tono = G_BRILLO if t == 0 else G_LUZ
            elif fila == hasta - 1 and hasta > 1:
                tono = G_SOMBRA
            else:
                tono = G_LUZ if t == 0 else G
            c.punto(CAPA_X + col, CAPA_Y + fila, tono)


def volumen(c, huecos=0.3):
    """Pelo en la capa exterior por arriba y por los lados, con huecos: da volumen y textura."""
    for cual in ("arriba", "derecha", "izquierda", "detras"):
        x0, y0, x1, y1 = c.cara("cabeza_capa", cual)
        bx0, by0, _, _ = c.cara("cabeza", cual)
        for y in range(y0, y1):
            for x in range(x0, x1):
                debajo = c.leer(bx0 + (x - x0), by0 + (y - y0))
                hueco = huecos * (0.35 if cual == "arriba" else 1.0)
                if debajo[3] == 0 or ruido(x, y, 77 + c.semilla) < hueco:
                    continue
                c.punto(x, y, oscuro(debajo[:3], 1.12) if ruido(x, y, 5) > 0.5 else debajo[:3])


def cejas(c, mujer, fila):
    if mujer:
        return                                                    # las tapa el flequillo
    else:                                                         # gruesas, sobre cada ojo
        for col in (1, 2, 5, 6):
            cf(c, col, fila, G_OSCURO)


def cabeza(c, lados, nuca):
    """Pelo arriba, a los lados (hasta 'lados' filas) y en la nuca (hasta 'nuca' filas)."""
    mechones(c, c.cara("cabeza", "arriba"))
    for lado in ("derecha", "izquierda"):
        x0, y0, x1, _ = c.cara("cabeza", lado)
        mechones(c, (x0, y0, x1, y0 + lados), False)
    x0, y0, x1, _ = c.cara("cabeza", "detras")
    mechones(c, (x0, y0, x1, y0 + nuca), False)


def espalda(c, filas, lados=0):
    x0, y0, _, _ = c.cara("cuerpo_capa", "detras")
    mechones(c, (x0, y0, x0 + 8, y0 + filas), False)
    if lados:
        for lado in ("derecha", "izquierda"):
            lx0, ly0, lx1, _ = c.cara("cuerpo_capa", lado)
            mechones(c, (lx0, ly0, lx1, ly0 + lados), False)


# Peinados. Varios llevan además piezas en 3D (ver PELO_3D y client/habitantes/ModeloPelo3D):
#   mujer: 0 larga con flequillo (+melena) · 1 larga con raya al lado (+melena) · 2 trenza (+coleta trenzada)
#          3 moño (+moño) · 4 coleta (+coleta) · 5 media melena con flequillo (+flequillo) · 6 dos trenzas (+trenzas)
#          7 corta con flequillo con volumen (+flequillo)
#   hombre: 0 corto con flequillo · 1 rapado · 2 melena (+melena) · 3 tonsura · 4 raya al lado
#           5 tazón (+flequillo) · 6 rizado (+flequillo) · 7 flequillo despeinado (+flequillo)

def pelo(sexo, estilo):
    mujer = sexo == "mujer"
    c = Lienzo(3 if mujer else 4, semilla=100 + estilo + (50 if mujer else 0))
    if mujer:
        cejas(c, True, 2)
        fleq = flequillo_profundo
        if estilo == 0:
            cabeza(c, 8, 8)
            fleq(c, [8, 4, 4, 3, 4, 3, 4, 8])
            espalda(c, 6, lados=2)
        elif estilo == 1:
            cabeza(c, 8, 8)
            fleq(c, [8, 4, 4, 3, 2, 2, 3, 7])
            espalda(c, 5, lados=1)
        elif estilo == 2:
            cabeza(c, 6, 8)
            fleq(c, [6, 3, 4, 2, 3, 4, 3, 6])
        elif estilo == 3:
            cabeza(c, 5, 8)
            fleq(c, [5, 3, 2, 2, 1, 2, 3, 5])
        elif estilo == 4:
            cabeza(c, 6, 8)
            fleq(c, [6, 4, 3, 2, 3, 3, 4, 6])
        elif estilo == 5:
            cabeza(c, 7, 8)
            fleq(c, [8, 4, 4, 4, 4, 4, 4, 8])
        elif estilo == 6:
            cabeza(c, 6, 8)
            fleq(c, [7, 3, 4, 3, 3, 4, 3, 7])
        else:
            cabeza(c, 5, 7)
            fleq(c, [7, 4, 3, 4, 3, 4, 3, 7])
        volumen(c)
    else:
        cejas(c, False, 3)
        if estilo == 0:
            cabeza(c, 3, 7)
            flequillo(c, [2, 2, 1, 2, 2, 1, 2, 2])
        elif estilo == 1:
            for cual, filas in (("arriba", 8), ("derecha", 4), ("izquierda", 4), ("detras", 7)):
                x0, y0, x1, _ = c.cara("cabeza", cual)
                for y in range(y0, y0 + filas):
                    for x in range(x0, x1):
                        c.punto(x, y, G_SOMBRA if ruido(x, y, 3) < 0.7 else G_OSCURO)
            flequillo(c, [1] * 8)
        elif estilo == 2:
            cabeza(c, 8, 8)
            flequillo(c, [4, 2, 2, 1, 2, 2, 2, 4])
            espalda(c, 2)
        elif estilo == 3:
            cabeza(c, 5, 7)
            x0, y0, x1, y1 = c.cara("cabeza", "arriba")
            for y in range(y0 + 2, y1 - 2):
                for x in range(x0 + 2, x1 - 2):
                    c.borrar(x, y)
            flequillo(c, [1] * 8)
        elif estilo == 4:
            cabeza(c, 3, 7)
            flequillo(c, [3, 3, 2, 2, 1, 1, 1, 2])
        elif estilo == 5:
            cabeza(c, 5, 7)
            flequillo(c, [3] * 8)
        elif estilo == 6:
            cabeza(c, 5, 7)
            flequillo(c, [2, 3, 2, 3, 2, 3, 2, 3])
            for cual in ("arriba", "derecha", "izquierda", "detras"):
                x0, y0, x1, y1 = c.cara("cabeza", cual)
                for y in range(y0, y1):
                    for x in range(x0, x1):
                        if c.leer(x, y)[3] and (x + y) % 2 == 0:
                            c.punto(x, y, G_LUZ)
        else:
            cabeza(c, 4, 7)
            flequillo(c, [3, 2, 3, 1, 3, 2, 3, 2])
        if estilo not in (1, 3):
            volumen(c, 0.45)
    return c


def barba(n):
    """1 de tres días · 2 completa · 3 bigote · 4 perilla · 5 larga (0 = sin barba, sin archivo)."""
    c = Lienzo(semilla=400 + n)
    if n == 1:
        for col in range(8):
            if col not in (3, 4):
                cf(c, col, 7, G_SOMBRA)
        cf(c, 0, 6, G_SOMBRA)
        cf(c, 7, 6, G_SOMBRA)
    elif n in (2, 5):
        for col in range(8):
            cf(c, col, 7, G if col % 3 else G_SOMBRA)
            if col in (0, 1, 6, 7):
                cf(c, col, 6, G)
                cf(c, col, 5, G_SOMBRA)
        for lado in ("derecha", "izquierda"):
            x0, y0, x1, _ = c.cara("cabeza", lado)
            mechones(c, (x0, y0 + 5, x1, y0 + 8), False)
        mechones(c, c.cara("cabeza", "abajo"), False)
        if n == 5:
            x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
            mechones(c, (x0 + 2, y0, x0 + 6, y0 + 3), False)
    elif n == 3:
        for col in (2, 3, 4, 5):
            cf(c, col, 5, G if col in (2, 5) else G_SOMBRA)
        cf(c, 2, 6, G)
        cf(c, 5, 6, G)
    elif n == 4:
        for col in (3, 4):
            cf(c, col, 7, G)
        cf(c, 2, 7, G_SOMBRA)
        cf(c, 5, 7, G_SOMBRA)
    return c


# ================================================================= pelo en 3D (piezas sobre la cabeza)

COLETA, MONO, TRENZAS, FLEQUILLO, MELENA, TRENZA_ATRAS = 1, 2, 4, 8, 16, 32

def pelo3d():
    """Textura de las piezas 3D de pelo (64x32, gris, se tiñe). Cajas (u, v, ancho, alto, fondo), como en
    client/habitantes/ModeloPelo3D: coleta (0,0,2,8,2) · moño (8,0,4,3,4) · trenza (24,0,2,8,2) ·
    flequillo (0,12,8,3,2) · melena (0,18,8,9,2)."""
    img = Image.new("RGBA", (64, 32), TRANSPARENTE)
    c = Lienzo()
    c.img = img

    def caja(u, v, w, h, d, trenzada=False):
        for cara, (x0, y0, x1, y1) in caras(u, v, w, h, d).items():
            for y in range(y0, y1):
                for x in range(x0, x1):
                    if trenzada and cara not in ("arriba", "abajo"):
                        tono = G_SOMBRA if (y + x) % 3 == 0 else (G_LUZ if (y + x) % 3 == 1 else G)
                    else:
                        tono = tono_mechon(x, y, y0, y1)
                    img.putpixel((x, y), tono + (255,))
    caja(0, 0, 2, 8, 2)
    caja(8, 0, 4, 3, 4)
    caja(24, 0, 2, 8, 2, trenzada=True)
    caja(0, 12, 8, 3, 2)
    caja(0, 18, 8, 9, 2)
    return img


# ================================================================= ropa medieval

CUERO = (96, 64, 40)
LINO = (230, 222, 202)
ORO = (210, 170, 78)
COLORES = {
    "hombre": [
        [(120, 86, 56), (98, 112, 60), (78, 96, 128), (138, 60, 52)],      # túnica con cinturón
        [(84, 70, 112), (60, 92, 82), (150, 124, 92), (112, 52, 48)],      # túnica larga bordada
        [(70, 70, 76), (110, 40, 40), (52, 74, 110), (96, 82, 46)],        # jubón con camisa
    ],
    "mujer": [
        [(120, 86, 56), (98, 112, 60), (78, 96, 128), (138, 60, 52)],      # vestido con delantal
        [(150, 54, 60), (58, 88, 120), (90, 110, 66), (110, 80, 120)],     # corpiño con blusa
        [(66, 96, 118), (132, 70, 92), (150, 130, 96), (74, 100, 74)],     # vestido largo con cinturón y bordado
    ],
}
CAPAS_PIERNAS = ("pierna_d_capa", "pierna_i_capa")
TELA = 0.07                         # textura de las telas


def zapatos(c, altos=False):
    desde = 7 if altos else 10
    for pierna in PIERNAS:
        c.caja(pierna, CUERO, filas=(desde, 12), fuerza=0.06)
        c.fila(pierna, desde, oscuro(CUERO, 0.8))


def punos(c, color, fila):
    for brazo in BRAZOS:
        c.fila(brazo, fila, color)


def medieval(sexo, n):
    mujer = sexo == "mujer"
    corte, color = divmod(n, 4)
    tela = COLORES[sexo][corte][color]
    c = Lienzo(3 if mujer else 4, semilla=200 + n + (20 if mujer else 0))
    fx, fy, _, _ = c.cara("cuerpo", "frente")

    if not mujer and corte == 0:        # túnica con cinturón y cuello en pico, calzas oscuras
        c.caja("cuerpo", tela, fuerza=TELA)
        for brazo in BRAZOS:
            c.caja(brazo, tela, filas=(0, 10), fuerza=TELA)
        punos(c, oscuro(tela, 0.78), 9)
        for pierna in PIERNAS:
            c.caja(pierna, oscuro(tela, 0.6), fuerza=TELA)
        for capa in CAPAS_PIERNAS:
            c.caja(capa, tela, filas=(0, 4), solo=LADOS, fuerza=TELA)
            c.fila(capa, 3, oscuro(tela, 0.82))
        c.fila("cuerpo", 9, CUERO, solo=None)
        c.punto(fx + 4, fy + 9, ORO)
        for fila in range(2):
            c.punto(fx + 3, fy + fila, oscuro(tela, 0.7))
            c.punto(fx + 4, fy + fila, oscuro(tela, 0.7))
        zapatos(c)
    elif not mujer and corte == 1:      # túnica larga con cuello redondo y bordado dorado
        c.caja("cuerpo", tela, fuerza=TELA)
        for brazo in BRAZOS:
            c.caja(brazo, tela, filas=(0, 11), fuerza=TELA)
        punos(c, ORO, 10)
        for pierna in PIERNAS:
            c.caja(pierna, oscuro(tela, 0.55), fuerza=TELA)
        for capa in CAPAS_PIERNAS:
            c.caja(capa, tela, filas=(0, 7), solo=LADOS, fuerza=TELA)
            c.fila(capa, 6, ORO)
        c.fila("cuerpo", 0, ORO)
        for col in range(2, 6):
            c.punto(fx + col, fy + 1, ORO)
        c.fila("cuerpo", 8, oscuro(tela, 0.72))
        zapatos(c)
    elif not mujer:                     # jubón abotonado, camisa blanca, calzas y botas altas
        c.caja("cuerpo", tela, fuerza=TELA)
        for brazo in BRAZOS:
            c.caja(brazo, LINO, filas=(0, 11), fuerza=0.05)
        punos(c, oscuro(LINO, 0.86), 10)
        for fila in range(0, 11):
            c.punto(fx + 3, fy + fila, oscuro(tela, 0.75))
        for fila in range(1, 11, 2):
            c.punto(fx + 4, fy + fila, ORO)
        c.punto(fx + 3, fy, LINO)
        c.punto(fx + 4, fy, LINO)
        c.fila("cuerpo", 11, oscuro(tela, 0.72))
        for pierna in PIERNAS:
            c.caja(pierna, mezclar(tela, (200, 190, 170), 0.35), fuerza=TELA)
        zapatos(c, altos=True)
    elif corte == 0:                    # vestido con delantal y escote cuadrado
        c.caja("cuerpo", tela, fuerza=TELA)
        for brazo in BRAZOS:
            c.caja(brazo, tela, filas=(0, 11), fuerza=TELA)
        punos(c, LINO, 10)
        for pierna in PIERNAS:
            c.caja(pierna, tela, fuerza=TELA)
        for capa in CAPAS_PIERNAS:
            c.caja(capa, oscuro(tela, 0.92), filas=(0, 11), solo=LADOS, fuerza=TELA)
        c.rect((fx + 2, fy, fx + 6, fy + 1), LINO)
        c.rect((fx + 1, fy + 7, fx + 7, fy + 12), LINO, 0.04)
        for capa in CAPAS_PIERNAS:
            px0, py0, px1, _ = c.cara(capa, "frente")
            c.rect((px0, py0, px1, py0 + 8), LINO, 0.04)
        c.fila("cuerpo", 6, oscuro(tela, 0.72))
        zapatos(c)
    elif corte == 1:                    # corpiño con cordones sobre blusa blanca, falda larga
        c.caja("cuerpo", tela, fuerza=TELA)
        c.caja("cuerpo", oscuro(tela, 0.7), filas=(2, 8), solo=LADOS, fuerza=TELA)
        c.rect((fx + 1, fy, fx + 7, fy + 2), LINO)
        for fila in range(2, 8):
            c.punto(fx + 3 + (fila % 2), fy + fila, LINO)
        for brazo in BRAZOS:
            c.caja(brazo, LINO, filas=(0, 11), fuerza=0.05)
        punos(c, oscuro(LINO, 0.86), 10)
        for pierna in PIERNAS:
            c.caja(pierna, tela, fuerza=TELA)
        for capa in CAPAS_PIERNAS:
            c.caja(capa, oscuro(tela, 0.9), filas=(0, 11), solo=LADOS, fuerza=TELA)
            c.fila(capa, 10, oscuro(tela, 0.72))
        zapatos(c)
    else:                               # vestido largo con cinturón y bordado dorado
        c.caja("cuerpo", tela, fuerza=TELA)
        for brazo in BRAZOS:
            c.caja(brazo, tela, filas=(0, 11), fuerza=TELA)
        punos(c, ORO, 10)
        c.fila("cuerpo", 0, ORO)
        for col in range(2, 6):
            c.punto(fx + col, fy + 1, oscuro(tela, 0.8))
        c.fila("cuerpo", 7, CUERO, solo=None)
        c.punto(fx + 4, fy + 7, ORO)
        c.punto(fx + 4, fy + 8, CUERO)
        c.punto(fx + 4, fy + 9, CUERO)
        for pierna in PIERNAS:
            c.caja(pierna, tela, fuerza=TELA)
        for capa in CAPAS_PIERNAS:
            c.caja(capa, mezclar(tela, (255, 255, 255), 0.06), filas=(0, 11), solo=LADOS, fuerza=TELA)
            c.fila(capa, 10, ORO)
        zapatos(c)
    return c


def bebe():
    """El bebé va envuelto en una mantilla de lino."""
    c = Lienzo(semilla=300)
    for nombre in ("cuerpo",) + EXTREMIDADES:
        c.caja(nombre, LINO, fuerza=0.06)
    c.fila("cuerpo", 6, (196, 180, 150), solo=None)
    return c


EPOCAS = ["prehistorica", "antigua", "medieval", "moderna", "industrial", "contemporanea"]
JAVA = os.path.join("java", "com", "lucarita", "yonga", "modulos", "habitantes", "Catalogo.java")


def cargar_epocas():
    """Los módulos de las épocas ya dibujadas (epoca_<id>.py)."""
    import importlib
    hechas = {}
    for e in EPOCAS:
        try:
            hechas[e] = importlib.import_module("epoca_" + e)
        except ModuleNotFoundError:
            pass
    return hechas


def java_str(lista):
    return "{" + ", ".join('"' + x.replace('"', '\\"') + '"' for x in lista) + "}"


def escribir_catalogo(ruta, hechas):
    """Catalogo.java: nombres (para el libro del creador), piezas 3D y peinados solo de adultos de cada época. Las
    épocas sin dibujar usan los de la medieval."""
    base = hechas["medieval"]
    filas = {k: [] for k in ("ROPA", "PEINADO", "PIEZAS", "SOLO_ADULTO", "BARBA", "COMPLEMENTO", "MARCA")}
    for e in EPOCAS:
        m = hechas.get(e, base)
        ropa, pein, piezas, adulto, comp, marca = [], [], [], [], [], []
        for sexo in ("hombre", "mujer"):
            ropa.append(java_str([m.nombre_ropa(sexo, n) for n in range(20)]))
            pein.append(java_str([p[0] for p in m.PEINADOS[sexo]]))
            piezas.append("{" + ", ".join(str(p[5]) for p in m.PEINADOS[sexo]) + "}")
            adulto.append("{" + ", ".join("true" if p[6] else "false" for p in m.PEINADOS[sexo]) + "}")
            comp.append(java_str(["ninguno"] + m.COMPLEMENTOS[sexo]))
            marca.append(java_str(["ninguno"] + m.NOMBRES_MARCA[sexo]))
        filas["ROPA"].append("{" + ", ".join(ropa) + "}")
        filas["PEINADO"].append("{" + ", ".join(pein) + "}")
        filas["PIEZAS"].append("{" + ", ".join(piezas) + "}")
        filas["SOLO_ADULTO"].append("{" + ", ".join(adulto) + "}")
        filas["BARBA"].append(java_str(["ninguna"] + m.BARBAS))
        filas["COMPLEMENTO"].append("{" + ", ".join(comp) + "}")
        filas["MARCA"].append("{" + ", ".join(marca) + "}")

    def bloque(nombre, tipo, comentario):
        return ("    /** " + comentario + " */\n    public static final " + tipo + " " + nombre + " = {\n            "
                + ",\n            ".join(filas[nombre]) + "};\n")

    texto = ("package com.lucarita.yonga.modulos.habitantes;\n\n"
             "import net.minecraft.util.RandomSource;\n\n"
             "/**\n * GENERADO por herramientas/dibujar_habitantes.py (no tocar a mano): lo que hay de cada cosa en cada época\n"
             " * (índices como Epoca.ordinal()). Por época y sexo: 20 ropas, 20 peinados, 20 complementos y 20 rasgos de cara;\n"
             " * por época, 20 barbas. Barba, complemento y marca empiezan por \"ninguno\" (0). Las épocas aún sin dibujar\n"
             " * se ven (y se llaman) como la medieval.\n */\n"
             "public final class Catalogo {\n\n    private Catalogo() {}\n\n"
             "    public static final boolean[] DIBUJADA = {" + ", ".join("true" if e in hechas else "false" for e in EPOCAS)
             + "};\n\n"
             + bloque("ROPA", "String[][][]", "[época][0 hombre · 1 mujer][0..19]")
             + bloque("PEINADO", "String[][][]", "[época][sexo][0..19]")
             + bloque("PIEZAS", "int[][][]", "Piezas 3D de cada peinado (ver CapaPelo3D): coleta 1, moño 2, trenzas 4, flequillo 8, melena 16, trenza atrás 32")
             + bloque("SOLO_ADULTO", "boolean[][][]", "Peinados que solo llevan los adultos (tonsura, calvicie…)")
             + bloque("BARBA", "String[][]", "[época][0..20]")
             + bloque("COMPLEMENTO", "String[][][]", "[época][sexo][0..20]")
             + bloque("MARCA", "String[][][]", "Rasgos de cara: [época][sexo][0..20]")
             + "\n    /** La época cuyas texturas se usan (las que aún no están dibujadas, la medieval). */\n"
             "    public static Epoca dibujo(Epoca e) {\n        return DIBUJADA[e.ordinal()] ? e : Epoca.MEDIEVAL;\n    }\n\n"
             "    /** Un peinado al azar de su época (los de solo adultos, menos a menudo). */\n"
             "    public static int peinadoAlAzar(Epoca e, boolean mujer, RandomSource azar) {\n"
             "        boolean[] adulto = SOLO_ADULTO[dibujo(e).ordinal()][mujer ? 1 : 0];\n"
             "        int n = azar.nextInt(adulto.length);\n"
             "        return adulto[n] && azar.nextBoolean() ? azar.nextInt(adulto.length) : n;\n    }\n}\n")
    with open(ruta, "w", encoding="utf-8") as f:
        f.write(texto)


def main():
    principal = sys.argv[1] if len(sys.argv) > 1 else "src/main/resources"
    raiz = os.path.join(principal, "assets", "yonga", "textures", "entity", "habitante")
    hechas = cargar_epocas()
    # Borra las texturas viejas de habitantes
    for carpeta in [raiz] + [os.path.join(raiz, x) for x in ("ropa", "pelo", "barba", "complemento", "marca")]:
        if os.path.isdir(carpeta):
            for f in os.listdir(carpeta):
                if f.endswith(".png"):
                    os.remove(os.path.join(carpeta, f))
    for sexo in ("hombre", "mujer"):
        for tono in range(len(TONOS)):
            for variante in range(CARAS):
                piel(sexo, tono, variante).guardar(os.path.join(raiz, f"piel_{sexo}_{tono}_{variante}.png"))
        for color in range(len(OJOS)):
            ojos(sexo, color).guardar(os.path.join(raiz, f"ojos_{sexo}_{color}.png"))
    for e, m in hechas.items():
        for sexo in ("hombre", "mujer"):
            for n in range(20):
                m.ropa(sexo, n).guardar(os.path.join(raiz, "ropa", f"{e}_{sexo}_{n}.png"))
                m.pelo(sexo, n).guardar(os.path.join(raiz, "pelo", f"{e}_{sexo}_{n}.png"))
            for n in range(1, 21):
                m.complemento(sexo, n).guardar(os.path.join(raiz, "complemento", f"{e}_{sexo}_{n}.png"))
                m.marca(sexo, n).guardar(os.path.join(raiz, "marca", f"{e}_{sexo}_{n}.png"))
        for n in range(1, 21):
            m.barba(n).guardar(os.path.join(raiz, "barba", f"{e}_{n}.png"))
        bebe().guardar(os.path.join(raiz, "ropa", f"{e}_bebe.png"))
    pelo3d().save(os.path.join(raiz, "pelo3d.png"))
    escribir_catalogo(os.path.join(principal, "..", JAVA), hechas)
    print("Texturas de habitantes en", raiz, "· épocas:", ", ".join(hechas))


if __name__ == "__main__":
    main()
