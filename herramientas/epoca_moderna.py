"""Época MODERNA (la España del siglo XVII, el Siglo de Oro): 20 ropas, 20 peinados, 20 barbas, 20 complementos y 20
rasgos de cara por sexo. Jubones, golas y golillas, ropillas, calzones y medias, coletos de los tercios, sayas con
guardainfante, basquiñas, mantillas, sombreros chambergos con pluma. Mismo formato que epoca_medieval."""
from dibujar_habitantes import (Lienzo, cf, oscuro, mezclar, ruido, mechones, flequillo, flequillo_profundo, volumen,
                                cabeza, espalda, BRAZOS, PIERNAS, LADOS, G_BRILLO, G_LUZ, G, G_SOMBRA,
                                G_OSCURO, G_HONDO, COLETA, MONO, TRENZAS, FLEQUILLO, MELENA, TRENZA_ATRAS,
                                CAPAS_PIERNAS, CUERO, LINO, ORO, TELA, punos, zapatos, CAPA_X, CAPA_Y)
from epoca_medieval import _barba_base, _bigote, _perilla, _pecho, _gorro, _banda, _hombros, _flor, _collar

ID = "moderna"

NEGRO = (40, 36, 40)
ENCAJE = (240, 236, 226)
ANTE = (184, 150, 100)            # el cuero claro de los coletos
PLATA = (196, 200, 206)
PERLA = (240, 236, 228)


def gola(c, ancha=True):
    """Gola (cuello de lechuguilla): rizado blanco alrededor del cuello, en la capa exterior del cuerpo y bajo la
    barbilla. La golilla es la versión plana, de una fila."""
    for cual in LADOS:
        x0, y0, x1, _ = c.cara("cuerpo_capa", cual)
        for x in range(x0, x1):
            c.punto(x, y0, ENCAJE if x % 2 else oscuro(ENCAJE, 0.84))
            if ancha:
                c.punto(x, y0 + 1, oscuro(ENCAJE, 0.9) if x % 2 else ENCAJE)
    x0, y0, x1, y1 = c.cara("cuerpo_capa", "arriba")
    for y in range(y0, y1):
        for x in range(x0, x1):
            c.punto(x, y, ENCAJE if (x + y) % 2 else oscuro(ENCAJE, 0.86))


def medias(c, color, desde=5):
    """Medias de color desde la rodilla y zapatos de hebilla."""
    for pierna in PIERNAS:
        c.caja(pierna, color, filas=(desde, 11), fuerza=0.04)
        c.caja(pierna, NEGRO, filas=(11, 12), fuerza=0.04)
        x0, y0, _, _ = c.cara(pierna, "frente")
        c.punto(x0 + 1, y0 + 11, PLATA)
        c.punto(x0 + 2, y0 + 11, PLATA)


def botas(c, color=CUERO, desde=6):
    for pierna in PIERNAS:
        c.caja(pierna, color, filas=(desde, 12), fuerza=0.06)
    for capa in CAPAS_PIERNAS:          # la vuelta de la bota, ancha
        c.caja(capa, oscuro(color, 1.1), filas=(desde, desde + 2), solo=LADOS, fuerza=0.06)


def acuchillado(c, nombre, color, forro, filas=(0, 12)):
    """Tela acuchillada: rajas verticales que dejan ver el forro de otro color."""
    c.caja(nombre, color, filas=filas, fuerza=0.05)
    for lado in LADOS:
        x0, y0, x1, _ = c.cara(nombre, lado)
        for x in range(x0, x1):
            if (x - x0) % 3 == 1:
                for y in range(y0 + filas[0] + 1, y0 + filas[1] - 1):
                    c.punto(x, y, forro)


# ================================================================= ROPA: 5 cortes x 4 colores

COLORES = {
    "hombre": [
        [NEGRO, (70, 70, 76), (70, 52, 40), (36, 44, 70)],                 # traje de hidalgo
        [(150, 40, 44), (54, 96, 70), (50, 70, 130), (180, 140, 60)],      # jubón acuchillado
        [(120, 100, 76), (96, 90, 74), (140, 120, 90), (90, 80, 70)],      # labrador
        [(150, 40, 44), (176, 150, 60), (60, 80, 140), (220, 220, 214)],   # soldado de los tercios (color de la banda)
        [(110, 40, 50), (50, 74, 110), (80, 90, 60), (130, 100, 70)],      # casaca larga
    ],
    "mujer": [
        [(150, 40, 60), (60, 90, 130), (200, 170, 100), (90, 110, 80)],    # saya con guardainfante
        [(150, 40, 44), (54, 96, 70), (50, 70, 130), (170, 120, 60)],      # basquiña negra y jubón de color
        [(120, 100, 76), (110, 96, 80), (96, 104, 84), (140, 116, 92)],    # criada con delantal
        [(220, 200, 190), (190, 150, 170), (150, 170, 190), (210, 190, 140)],  # dama de corte
        [(180, 40, 40), (60, 110, 60), (190, 140, 40), (60, 70, 130)],     # labradora con refajo
    ],
}
NOMBRES_ROPA = {
    "hombre": ["traje de hidalgo", "jubón acuchillado", "traje de labrador", "soldado de los tercios", "casaca larga"],
    "mujer": ["saya con guardainfante", "basquiña con jubón", "traje de criada", "vestido de dama", "traje de labradora"],
}
NOMBRES_COLOR = {
    "hombre": [["negro", "gris", "pardo", "azul noche"], ["rojo", "verde", "azul", "ocre"],
               ["pardo", "gris", "arena", "oscuro"], ["banda roja", "banda amarilla", "banda azul", "banda blanca"],
               ["granate", "azul", "verde", "marrón"]],
    "mujer": [["carmesí", "azul", "dorada", "verde"], ["roja", "verde", "azul", "ocre"],
              ["pardo", "terroso", "verde gris", "arena"], ["marfil", "rosa", "celeste", "dorado"],
              ["rojo", "verde", "amarillo", "azul"]],
}


def ropa(sexo, n):
    mujer = sexo == "mujer"
    corte, color = divmod(n, 4)
    tela = COLORES[sexo][corte][color]
    c = Lienzo(3 if mujer else 4, semilla=3200 + n + (40 if mujer else 0))
    fx, fy, _, _ = c.cara("cuerpo", "frente")

    if not mujer and corte == 0:        # traje oscuro de hidalgo: ropilla, golilla, calzones y medias negras
        c.caja("cuerpo", tela, fuerza=TELA)
        for brazo in BRAZOS:
            c.caja(brazo, tela, fuerza=TELA)
            c.fila(brazo, 11, ENCAJE)
        for fila in range(1, 10, 2):
            c.punto(fx + 4, fy + fila, PLATA)
        c.fila("cuerpo", 9, oscuro(tela, 0.7), solo=None)
        for capa in ("brazo_d_capa", "brazo_i_capa"):       # brahones: hombreras abullonadas
            c.caja(capa, oscuro(tela, 1.3), filas=(0, 2), fuerza=0.05)
        for pierna in PIERNAS:
            c.caja(pierna, tela, filas=(0, 5), fuerza=TELA)
        gola(c, ancha=False)
        medias(c, NEGRO if tela != NEGRO else (60, 60, 66))
    elif not mujer and corte == 1:      # jubón acuchillado con gola, calzones bombachos y medias de color
        forro = mezclar(tela, (250, 240, 210), 0.55)
        acuchillado(c, "cuerpo", tela, forro)
        for brazo in BRAZOS:
            acuchillado(c, brazo, tela, forro, (0, 11))
            c.fila(brazo, 11, ENCAJE)
        for fila in range(1, 10, 2):
            c.punto(fx + 4, fy + fila, ORO)
        for pierna in PIERNAS:
            acuchillado(c, pierna, tela, forro, (0, 5))
        for capa in CAPAS_PIERNAS:      # calzones abultados
            c.caja(capa, tela, filas=(0, 5), solo=LADOS, fuerza=0.05)
            c.fila(capa, 4, oscuro(tela, 0.75))
        gola(c)
        medias(c, mezclar(tela, (255, 255, 255), 0.45))
    elif not mujer and corte == 2:      # labrador: camisa, coleto de cuero sin mangas, calzón y polainas
        c.caja("cuerpo", LINO, fuerza=0.06)
        for brazo in BRAZOS:
            c.caja(brazo, LINO, filas=(0, 11), fuerza=0.06)
            c.fila(brazo, 10, oscuro(LINO, 0.85))
        c.caja("cuerpo", tela, filas=(0, 10), solo=("derecha", "izquierda", "detras"), fuerza=0.1)
        c.rect((fx, fy, fx + 3, fy + 10), tela, 0.1)
        c.rect((fx + 5, fy, fx + 8, fy + 10), tela, 0.1)
        c.fila("cuerpo", 9, (150, 40, 40), solo=None)       # faja
        c.fila("cuerpo", 10, (150, 40, 40), solo=None)
        for pierna in PIERNAS:
            c.caja(pierna, oscuro(tela, 0.75), filas=(0, 6), fuerza=0.08)
            c.caja(pierna, (190, 180, 160), filas=(6, 10), fuerza=0.05)
        for pierna in PIERNAS:
            c.caja(pierna, CUERO, filas=(10, 12), fuerza=0.06)
            c.fila(pierna, 8, CUERO)
    elif not mujer and corte == 3:      # soldado: coleto de ante, banda de color cruzada, calzones y botas
        c.caja("cuerpo", ANTE, fuerza=0.08)
        for brazo in BRAZOS:
            c.caja(brazo, (150, 40, 44) if tela != (150, 40, 44) else (60, 60, 70), filas=(0, 11), fuerza=TELA)
            c.fila(brazo, 11, ENCAJE)
        for i in range(10):             # la banda, del hombro a la cadera, con lazo
            col = i * 7 // 9
            c.punto(fx + col, fy + i, tela)
            if col + 1 < 8:
                c.punto(fx + col + 1, fy + i, oscuro(tela, 0.85))
        c.rect((fx + 6, fy + 9, fx + 8, fy + 11), tela)
        c.fila("cuerpo", 9, oscuro(CUERO, 0.8), solo=("derecha", "izquierda", "detras"))
        for pierna in PIERNAS:
            c.caja(pierna, (90, 80, 70), filas=(0, 6), fuerza=TELA)
        botas(c, (70, 54, 40), 5)
        gola(c, ancha=False)
    elif not mujer:                     # casaca larga abotonada hasta la rodilla, con vueltas en las mangas
        c.caja("cuerpo", tela, fuerza=TELA)
        for brazo in BRAZOS:
            c.caja(brazo, tela, filas=(0, 11), fuerza=TELA)
            c.caja(brazo, oscuro(tela, 0.75), filas=(8, 11), fuerza=TELA)
            c.fila(brazo, 11, ENCAJE)
        for fila in range(0, 12):
            c.punto(fx + 3, fy + fila, oscuro(tela, 0.7))
            if fila % 2:
                c.punto(fx + 4, fy + fila, ORO)
        for capa in CAPAS_PIERNAS:
            c.caja(capa, tela, filas=(0, 6), solo=LADOS, fuerza=TELA)
        for pierna in PIERNAS:
            c.caja(pierna, oscuro(tela, 0.6), filas=(0, 5), fuerza=TELA)
        x0, y0, _, _ = c.cara("cuerpo", "frente")
        c.punto(x0 + 3, y0, ENCAJE)     # corbata de encaje
        c.punto(x0 + 4, y0, ENCAJE)
        c.punto(x0 + 3, y0 + 1, ENCAJE)
        c.punto(x0 + 4, y0 + 1, oscuro(ENCAJE, 0.85))
        medias(c, (220, 214, 204))
    elif corte == 0:                    # saya ancha (guardainfante) con jubón ceñido y cuello de encaje
        c.caja("cuerpo", tela, fuerza=0.05)
        for brazo in BRAZOS:
            c.caja(brazo, tela, fuerza=0.05)
            c.fila(brazo, 11, ENCAJE)
            c.fila(brazo, 2, ORO)
        c.fila("cuerpo", 7, ORO, solo=None)
        for fila in range(1, 7, 2):
            c.punto(fx + 3, fy + fila, ORO)
            c.punto(fx + 4, fy + fila, ORO)
        for pierna in PIERNAS:
            c.caja(pierna, tela, fuerza=0.05)
        for capa in CAPAS_PIERNAS:      # la saya, ahuecada y con franjas de galón
            c.caja(capa, mezclar(tela, (255, 255, 255), 0.06), filas=(0, 12), solo=LADOS, fuerza=0.05)
            for fila in (5, 8, 11):
                c.fila(capa, fila, ORO)
        x0, y0, x1, _ = c.cara("cuerpo_capa", "frente")
        c.rect((x0, y0, x1, y0 + 1), ENCAJE)
        for cual in ("derecha", "izquierda", "detras"):
            x0, y0, x1, _ = c.cara("cuerpo_capa", cual)
            c.rect((x0, y0, x1, y0 + 1), ENCAJE)
        zapatos(c)
    elif corte == 1:                    # basquiña negra (falda) con jubón de color y mangas con puños
        c.caja("cuerpo", tela, fuerza=0.05)
        for brazo in BRAZOS:
            c.caja(brazo, tela, fuerza=0.05)
            c.fila(brazo, 11, ENCAJE)
        c.rect((fx + 2, fy, fx + 6, fy + 1), ENCAJE)
        for fila in range(1, 7):
            c.punto(fx + 4, fy + fila, oscuro(tela, 0.7))
        c.fila("cuerpo", 7, NEGRO, solo=None)
        c.caja("cuerpo", NEGRO, filas=(8, 12), fuerza=0.05)
        for pierna in PIERNAS:
            c.caja(pierna, NEGRO, fuerza=0.05)
        for capa in CAPAS_PIERNAS:
            c.caja(capa, (54, 50, 56), filas=(0, 12), solo=LADOS, fuerza=0.05)
        zapatos(c)
    elif corte == 2:                    # criada: corpiño, mangas remangadas, delantal y pañoleta
        c.caja("cuerpo", tela, fuerza=0.09)
        for brazo in BRAZOS:
            c.caja(brazo, LINO, filas=(0, 6), fuerza=0.05)
            c.fila(brazo, 5, oscuro(LINO, 0.84))
        panuelo = mezclar(tela, (240, 230, 210), 0.65)
        for fila in range(4):
            for col in range(8):
                if abs(col - 3.5) >= fila + 0.5:
                    c.punto(fx + col, fy + fila, panuelo)
        for pierna in PIERNAS:
            c.caja(pierna, oscuro(tela, 0.9), fuerza=0.09)
        for capa in CAPAS_PIERNAS:
            c.caja(capa, oscuro(tela, 0.86), filas=(0, 11), solo=LADOS, fuerza=0.09)
        delantal = (238, 232, 218)
        c.rect((fx + 1, fy + 7, fx + 7, fy + 12), delantal, 0.04)
        for capa in CAPAS_PIERNAS:
            px0, py0, px1, _ = c.cara(capa, "frente")
            c.rect((px0, py0, px1, py0 + 9), delantal, 0.04)
        c.fila("cuerpo", 7, oscuro(tela, 0.7), solo=("derecha", "izquierda", "detras"))
        zapatos(c)
    elif corte == 3:                    # dama de corte: vestido claro, gorguera, mangas abullonadas y perlas
        c.caja("cuerpo", tela, fuerza=0.04)
        for brazo in BRAZOS:
            c.caja(brazo, tela, fuerza=0.04)
            c.fila(brazo, 11, ENCAJE)
        for capa in ("brazo_d_capa", "brazo_i_capa"):
            c.caja(capa, mezclar(tela, (255, 255, 255), 0.3), filas=(0, 4), fuerza=0.04)
            c.fila(capa, 3, PERLA)
        for fila in range(1, 9):
            c.punto(fx + 3 + (fila % 2), fy + fila, PERLA)
        c.fila("cuerpo", 8, ORO, solo=None)
        for pierna in PIERNAS:
            c.caja(pierna, tela, fuerza=0.04)
        for capa in CAPAS_PIERNAS:
            c.caja(capa, oscuro(tela, 0.95), filas=(0, 12), solo=LADOS, fuerza=0.04)
            c.fila(capa, 11, ORO)
        gola(c)
        zapatos(c)
    else:                               # labradora: jubón negro, refajo de color con franjas, delantal corto
        c.caja("cuerpo", NEGRO, filas=(0, 8), fuerza=0.06)
        for brazo in BRAZOS:
            c.caja(brazo, LINO, filas=(0, 7), fuerza=0.05)
        c.rect((fx + 2, fy, fx + 6, fy + 2), LINO)
        c.caja("cuerpo", tela, filas=(8, 12), fuerza=0.06)
        for pierna in PIERNAS:
            c.caja(pierna, tela, filas=(0, 9), fuerza=0.06)
            c.caja(pierna, (230, 224, 214), filas=(9, 11), fuerza=0.03)
        for capa in CAPAS_PIERNAS:
            c.caja(capa, tela, filas=(0, 10), solo=LADOS, fuerza=0.06)
            c.fila(capa, 7, NEGRO)
            c.fila(capa, 9, NEGRO)
        c.rect((fx + 2, fy + 8, fx + 6, fy + 12), (60, 60, 64), 0.04)
        zapatos(c)
    return c


def nombre_ropa(sexo, n):
    corte, color = divmod(n, 4)
    return NOMBRES_ROPA[sexo][corte] + " " + NOMBRES_COLOR[sexo][corte][color]


# ================================================================= PEINADOS
# Especiales: "rizado", "ondas" (ondas a los lados), "ahuecado" (pelo hinchado a los lados, como en los retratos de
# Velázquez), "lazo" (lazo detrás), "rapado", "calvo", "entradas".
PEINADOS = {
    "mujer": [
        ("ahuecado con lazo", 7, 8, [6, 3, 2, 1, 1, 2, 3, 6], (0, 0), 0, False, "ahuecado"),
        ("rizos a los lados", 8, 8, [7, 3, 2, 1, 1, 2, 3, 7], (3, 1), MELENA, False, "rizado"),
        ("moño con lazo", 5, 8, [5, 3, 2, 2, 1, 2, 3, 5], (0, 0), MONO, False, "lazo"),
        ("crenchas con moño bajo", 6, 8, [6, 3, 2, 1, 1, 2, 3, 6], (0, 0), TRENZA_ATRAS, False, "ondas"),
        ("trenzas recogidas", 6, 8, [6, 3, 2, 2, 2, 2, 3, 6], (0, 0), MONO, False, None),
        ("flequillo rizado", 7, 8, [7, 4, 4, 4, 4, 4, 4, 7], (0, 0), FLEQUILLO, False, "rizado"),
        ("larga con raya", 8, 8, [8, 3, 2, 1, 1, 2, 3, 8], (7, 2), MELENA, False, None),
        ("coleta con cinta", 6, 8, [6, 4, 3, 2, 3, 3, 4, 6], (0, 0), COLETA, False, "lazo"),
        ("dos trenzas", 6, 8, [7, 3, 4, 3, 3, 4, 3, 7], (0, 0), TRENZAS, False, None),
        ("tirabuzones", 8, 8, [8, 4, 3, 3, 3, 3, 4, 8], (5, 2), MELENA, False, "tirabuzones"),
        ("moño alto", 4, 8, [4, 3, 2, 2, 2, 2, 3, 4], (0, 0), MONO, False, None),
        ("media melena ondulada", 7, 8, [7, 3, 2, 2, 1, 2, 3, 7], (0, 0), 0, False, "ondas"),
        ("larga con flequillo", 8, 8, [8, 4, 4, 3, 4, 3, 4, 8], (6, 2), FLEQUILLO | MELENA, False, None),
        ("trenza larga", 6, 8, [6, 3, 4, 2, 3, 4, 3, 6], (0, 0), TRENZA_ATRAS, False, None),
        ("ahuecado sin lazo", 7, 8, [6, 4, 3, 2, 2, 3, 4, 6], (0, 0), 0, False, "ahuecado"),
        ("recogido de criada", 5, 8, [5, 3, 3, 2, 2, 3, 3, 5], (0, 0), MONO, False, None),
        ("rizada suelta", 8, 8, [8, 4, 3, 4, 3, 4, 3, 8], (6, 2), MELENA, False, "rizado"),
        ("corta con flequillo", 5, 7, [7, 4, 3, 4, 3, 4, 3, 7], (0, 0), FLEQUILLO, False, None),
        ("larguísima", 8, 8, [8, 4, 3, 3, 3, 3, 4, 8], (12, 4), MELENA, False, None),
        ("tirabuzones con lazo", 8, 8, [7, 3, 3, 2, 2, 3, 3, 7], (4, 2), MELENA, False, "tirabuzones_lazo"),
    ],
    "hombre": [
        ("melena de caballero", 8, 8, [4, 2, 2, 1, 2, 2, 2, 4], (2, 0), MELENA, False, "ondas"),
        ("corto con raya", 3, 7, [3, 3, 2, 2, 1, 1, 1, 2], (0, 0), 0, False, None),
        ("guedejas rizadas", 8, 8, [4, 3, 2, 3, 2, 3, 2, 4], (3, 1), MELENA, False, "rizado"),
        ("peluca de rizos", 8, 8, [3, 2, 2, 2, 2, 2, 2, 3], (5, 2), MELENA, False, "tirabuzones"),
        ("coleta con lazo", 5, 8, [2, 2, 1, 1, 1, 1, 2, 2], (0, 0), COLETA, False, "lazo"),
        ("corto", 3, 7, [2, 2, 1, 2, 2, 1, 2, 2], (0, 0), 0, False, None),
        ("con entradas", 4, 8, [0, 1, 0, 0, 0, 0, 1, 0], (1, 0), MELENA, True, "entradas"),
        ("rapado", 4, 7, [1] * 8, (0, 0), 0, False, "rapado"),
        ("melena con raya al medio", 8, 8, [5, 2, 1, 0, 0, 1, 2, 5], (3, 0), MELENA, False, None),
        ("rizado corto", 3, 6, [2, 2, 1, 2, 2, 1, 2, 2], (0, 0), 0, False, "rizado"),
        ("calvo con melena", 6, 8, [0] * 8, (2, 0), 0, True, "calvo"),
        ("flequillo corto", 3, 7, [3, 3, 3, 2, 2, 3, 3, 3], (0, 0), FLEQUILLO, False, None),
        ("melena ondulada", 7, 8, [3, 2, 1, 1, 1, 1, 2, 3], (2, 0), MELENA, False, "ondas"),
        ("peinado hacia atrás", 4, 8, [1, 1, 0, 0, 0, 0, 1, 1], (1, 0), 0, False, None),
        ("despeinado de pícaro", 4, 7, [3, 2, 3, 1, 3, 2, 3, 2], (0, 0), FLEQUILLO, False, None),
        ("melena larga", 8, 8, [4, 3, 2, 2, 2, 2, 3, 4], (5, 1), MELENA, False, None),
        ("coleta de marinero", 5, 8, [2, 2, 1, 1, 1, 1, 2, 2], (0, 0), TRENZA_ATRAS, False, None),
        ("tazón", 5, 7, [3] * 8, (0, 0), FLEQUILLO, False, None),
        ("canoso con entradas", 3, 7, [0, 1, 0, 0, 0, 0, 1, 0], (0, 0), 0, True, "entradas"),
        ("peluca larga", 8, 8, [4, 3, 3, 2, 2, 3, 3, 4], (8, 3), MELENA, False, "tirabuzones"),
    ],
}


def _ondas(c):
    for cual in ("derecha", "izquierda", "detras"):
        x0, y0, x1, y1 = c.cara("cabeza", cual)
        for y in range(y0, y1):
            for x in range(x0, x1):
                if c.leer(x, y)[3]:
                    fase = (y + (x - x0) // 2) % 3
                    c.punto(x, y, G_LUZ if fase == 0 else (G if fase == 1 else G_SOMBRA))


def _tirabuzones(c):
    """Rizos largos en columna: espirales de luz y sombra que bajan por los lados y la espalda."""
    for cual in ("derecha", "izquierda", "detras"):
        x0, y0, x1, y1 = c.cara("cabeza", cual)
        for y in range(y0, y1):
            for x in range(x0, x1):
                if c.leer(x, y)[3]:
                    c.punto(x, y, G_LUZ if (y + (x - x0)) % 2 == 0 else G_SOMBRA)
    x0, y0, x1, y1 = c.cara("cuerpo_capa", "detras")
    for y in range(y0, y1):
        for x in range(x0, x1):
            if c.leer(x, y)[3]:
                c.punto(x, y, G_LUZ if (y + (x - x0)) % 2 == 0 else G_SOMBRA)


def _ahuecado(c):
    """Pelo hinchado a los lados (en la capa exterior, de arriba abajo)."""
    for cual in ("derecha", "izquierda"):
        x0, y0, x1, _ = c.cara("cabeza_capa", cual)
        for y in range(y0, y0 + 7):
            for x in range(x0, x1):
                c.punto(x, y, G_LUZ if (x + y) % 3 == 0 else (G if y < y0 + 5 else G_SOMBRA))


def _lazo(c):
    """Lazo detrás de la cabeza, pintado en gris claro: se tiñe del color del pelo, como una cinta a juego."""
    x0, y0, _, _ = c.cara("cabeza_capa", "detras")
    for x, y in ((2, 3), (3, 4), (4, 4), (5, 3), (2, 5), (5, 5)):
        c.punto(x0 + x, y0 + y, G_BRILLO)
    c.punto(x0 + 3, y0 + 4, G_LUZ)


def pelo(sexo, n):
    mujer = sexo == "mujer"
    nombre, lados, nuca, fleq, (esp, esp_lados), _, _, especial = PEINADOS[sexo][n]
    c = Lienzo(3 if mujer else 4, semilla=3100 + n * 3 + (50 if mujer else 0))
    if not mujer:
        for col in (1, 2, 5, 6):
            cf(c, col, 3, G_OSCURO)
    if especial == "rapado":
        for cual, filas in (("arriba", 8), ("derecha", 4), ("izquierda", 4), ("detras", 7)):
            x0, y0, x1, _ = c.cara("cabeza", cual)
            for y in range(y0, y0 + filas):
                for x in range(x0, x1):
                    c.punto(x, y, G_SOMBRA if ruido(x, y, 3) < 0.7 else G_OSCURO)
    elif especial == "calvo":
        for lado in ("derecha", "izquierda"):
            x0, y0, x1, _ = c.cara("cabeza", lado)
            mechones(c, (x0, y0 + 2, x1, y0 + lados), False)
        x0, y0, x1, _ = c.cara("cabeza", "detras")
        mechones(c, (x0, y0 + 2, x1, y0 + nuca), False)
    else:
        cabeza(c, lados, nuca)
        if especial == "entradas":
            x0, y0, x1, y1 = c.cara("cabeza", "arriba")
            for x in (x0, x0 + 1, x1 - 2, x1 - 1):
                for y in range(y1 - 3, y1):
                    c.borrar(x, y)
    if especial not in ("rapado", "calvo"):
        (flequillo_profundo if mujer else flequillo)(c, fleq)
    elif especial == "rapado":
        flequillo(c, fleq)
    if esp:
        espalda(c, esp, lados=esp_lados)
    if especial == "rizado":
        for cual in ("arriba", "derecha", "izquierda", "detras"):
            x0, y0, x1, y1 = c.cara("cabeza", cual)
            for y in range(y0, y1):
                for x in range(x0, x1):
                    if c.leer(x, y)[3] and (x + y) % 2 == 0:
                        c.punto(x, y, G_LUZ if (x // 2 + y) % 3 else G_SOMBRA)
    elif especial == "ondas":
        _ondas(c)
    elif especial in ("tirabuzones", "tirabuzones_lazo"):
        _tirabuzones(c)
    if especial not in ("rapado", "calvo"):
        volumen(c, 0.3 if mujer else 0.45)
    if especial == "ahuecado":
        _ahuecado(c)
        if nombre.endswith("lazo"):
            _lazo(c)
    elif especial in ("lazo", "tirabuzones_lazo"):
        _lazo(c)
    return c


# ================================================================= BARBAS (1..20; 0 = ninguna)

BARBAS = [
    "bigote engomado", "bigote y perilla", "barba de candado", "barba en punta", "bigote fino", "perilla",
    "bigotera de puntas", "de tres días", "barba corta", "barba completa", "barba larga de anciano", "patillas",
    "bigote caído", "barba de marinero", "perilla larga", "bigote grueso", "barba rala", "de collar",
    "barba de ermitaño", "bigote de mosquetero",
]


def _puntas(c):
    """Bigote con las puntas hacia arriba."""
    for col in (2, 3, 4, 5):
        cf(c, col, 5, G_SOMBRA)
    cf(c, 1, 5, G)
    cf(c, 6, 5, G)
    cf(c, 0, 4, G_LUZ)
    cf(c, 7, 4, G_LUZ)


def barba(n):
    c = Lienzo(semilla=3400 + n)
    nombre = BARBAS[n - 1]
    if nombre == "bigote engomado":
        _puntas(c)
    elif nombre == "bigote y perilla":
        _puntas(c)
        _perilla(c)
    elif nombre == "barba de candado":
        _bigote(c, "grueso")
        for col in (2, 3, 4, 5):
            cf(c, col, 7, G)
        for col in (1, 6):
            cf(c, col, 6, G_SOMBRA)
    elif nombre == "barba en punta":
        _barba_base(c, 2)
        _bigote(c, "fino")
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.punto(x0 + 3, y0, G)
        c.punto(x0 + 4, y0, G_SOMBRA)
        c.punto(x0 + 3, y0 + 1, G_SOMBRA)
    elif nombre == "bigote fino":
        _bigote(c, "fino")
    elif nombre == "perilla":
        _perilla(c)
    elif nombre == "bigotera de puntas":
        _puntas(c)
        cf(c, 0, 3, G_LUZ)
        cf(c, 7, 3, G_LUZ)
        _perilla(c, larga=True)
    elif nombre == "de tres días":
        for col in range(8):
            if col not in (3, 4):
                cf(c, col, 7, G_SOMBRA)
        cf(c, 0, 6, G_SOMBRA)
        cf(c, 7, 6, G_SOMBRA)
    elif nombre == "barba corta":
        _barba_base(c, 2, G_SOMBRA)
        _bigote(c, "fino")
    elif nombre == "barba completa":
        _barba_base(c)
        _bigote(c, "grueso")
    elif nombre == "barba larga de anciano":
        _barba_base(c, 4)
        _bigote(c, "caido")
        _pecho(c, 5, 6)
    elif nombre == "patillas":
        for col in (0, 7):
            for fila in range(3, 7):
                cf(c, col, fila, G_SOMBRA)
        for lado in ("derecha", "izquierda"):
            x0, y0, x1, _ = c.cara("cabeza", lado)
            mechones(c, (x0, y0 + 3, x0 + 3, y0 + 7), False)
    elif nombre == "bigote caído":
        _bigote(c, "caido")
    elif nombre == "barba de marinero":
        for col in range(8):
            cf(c, col, 7, G_SOMBRA)
        for col in (0, 7):
            cf(c, col, 6, G_SOMBRA)
            cf(c, col, 5, G_SOMBRA)
        mechones(c, c.cara("cabeza", "abajo"), False)
        _bigote(c, "grueso")
    elif nombre == "perilla larga":
        _bigote(c, "fino")
        _perilla(c, larga=True)
    elif nombre == "bigote grueso":
        _bigote(c, "grueso")
    elif nombre == "barba rala":
        for col in range(8):
            for fila in (5, 6, 7):
                if ruido(col, fila, 9) < (0.5 if fila == 7 else 0.25) and not (fila < 7 and 2 <= col <= 5):
                    cf(c, col, fila, G_SOMBRA)
    elif nombre == "de collar":
        for col in range(8):
            cf(c, col, 7, G_SOMBRA)
        for col in (0, 7):
            cf(c, col, 6, G_SOMBRA)
        mechones(c, c.cara("cabeza", "abajo"), False)
    elif nombre == "barba de ermitaño":
        _barba_base(c, 4)
        _bigote(c, "grueso")
        _pecho(c, 7, 6)
    else:                                                         # bigote de mosquetero: puntas y perilla fina
        _puntas(c)
        for col in (3, 4):
            cf(c, col, 7, G_SOMBRA)
    return c


# ================================================================= COMPLEMENTOS (1..20; 0 = ninguno). No se tiñen.

FIELTRO = (50, 44, 44)


def _sombrero(c, color, pluma=None, ala=True):
    """Sombrero de ala ancha (chambergo): copa en la capa de la cabeza y el ala como un borde oscuro."""
    _gorro(c, color, 2, 1, detalle=(1, oscuro(color, 0.6)), fuerza=0.06)
    if pluma:
        x0, y0, _, _ = c.cara("cabeza_capa", "izquierda")
        for i in range(5):
            c.punto(x0 + 3 + i, y0 + (1 if i < 2 else 0), pluma if i % 2 == 0 else mezclar(pluma, (255, 255, 255), 0.4))
        x0, y0, x1, y1 = c.cara("cabeza_capa", "arriba")
        for x in range(x0 + 4, x1):
            c.punto(x, y0 + 2, pluma)


COMPLEMENTOS = {
    "mujer": ["mantilla negra", "mantilla blanca", "lazo en el pelo", "abanico al cinto", "collar de perlas",
              "pendientes de perla", "cuello de encaje", "chal", "toca", "cofia", "flores en el pelo", "joyel",
              "guantes", "pañuelo a la cabeza", "sombrero de paja", "bolsa de mano", "pulseras de oro",
              "cinta de terciopelo", "pendientes de oro", "diadema de perlas"],
    "hombre": ["chambergo con pluma", "sombrero de ala ancha", "gola grande", "capa española", "espada ropera",
               "tahalí", "guantes", "montera", "faja roja", "bolsa al cinto", "cadena de oro", "quevedos",
               "pendiente de oro", "pañuelo al cuello", "botas con espuelas", "pipa de barro", "sombrero de tres picos",
               "capa corta", "cinto de cuero", "parche en el ojo"],
}


def complemento(sexo, n):
    mujer = sexo == "mujer"
    c = Lienzo(3 if mujer else 4, semilla=3600 + n + (40 if mujer else 0))
    nombre = COMPLEMENTOS[sexo][n - 1]
    fx, fy, _, _ = c.cara("cabeza_capa", "frente")
    if nombre in ("mantilla negra", "mantilla blanca"):
        color = NEGRO if nombre == "mantilla negra" else ENCAJE
        _gorro(c, color, 8, 1, fuerza=0.04)
        for cual in ("derecha", "izquierda", "detras"):          # encaje calado: huecos
            x0, y0, x1, _ = c.cara("cabeza_capa", cual)
            for y in range(y0 + 2, y0 + 8):
                for x in range(x0, x1):
                    if (x + y) % 3 == 0:
                        c.borrar(x, y)
        c.caja("cuerpo_capa", color, filas=(0, 5), solo=("detras", "derecha", "izquierda"), fuerza=0.04)
    elif nombre == "lazo en el pelo":
        x0, y0, _, _ = c.cara("cabeza_capa", "izquierda")
        for x, y in ((4, 1), (5, 2), (6, 1), (4, 3), (6, 3)):
            c.punto(x0 + x, y0 + y, (200, 50, 70))
        c.punto(x0 + 5, y0 + 2, (150, 30, 50))
    elif nombre == "abanico al cinto":
        c.fila("cuerpo_capa", 7, NEGRO, solo=None)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for i, color in enumerate(((200, 60, 80), (240, 220, 200), (200, 60, 80))):
            c.punto(x0 + 5 + i, y0 + 8, color)
        c.punto(x0 + 6, y0 + 9, (120, 80, 50))
    elif nombre == "collar de perlas":
        _collar(c, PERLA)
    elif nombre == "diadema de perlas":
        _banda(c, 1, PERLA)
    elif nombre in ("pendientes de perla", "pendientes de oro", "pendiente de oro"):
        color = PERLA if "perla" in nombre else ORO
        for cual in (("derecha", "izquierda") if mujer else ("izquierda",)):
            x0, y0, _, _ = c.cara("cabeza_capa", cual)
            c.punto(x0 + 4, y0 + 6, ORO)
            if mujer:
                c.punto(x0 + 4, y0 + 7, color)
    elif nombre == "cuello de encaje":
        for cual in LADOS:
            x0, y0, x1, _ = c.cara("cuerpo_capa", cual)
            for x in range(x0, x1):
                c.punto(x, y0, ENCAJE)
                if x % 2 == 0:
                    c.punto(x, y0 + 1, ENCAJE)
    elif nombre == "gola grande":
        gola(c, ancha=True)
        x0, y0, x1, _ = c.cara("cabeza_capa", "frente")       # sube por debajo de la barbilla
        for x in range(x0, x1):
            c.punto(x, y0 + 7, ENCAJE if x % 2 else oscuro(ENCAJE, 0.85))
    elif nombre == "chal":
        _hombros(c, (130, 40, 60), 4, 3)
    elif nombre == "toca":
        _gorro(c, ENCAJE, 6, 1)
        c.rect((fx, fy + 7, fx + 8, fy + 8), ENCAJE)
    elif nombre == "cofia":
        _gorro(c, ENCAJE, 4, 1)
    elif nombre == "flores en el pelo":
        _flor(c, fx + 6, fy + 1, (220, 50, 60), (250, 230, 120))
        x0, y0, _, _ = c.cara("cabeza_capa", "izquierda")
        _flor(c, x0 + 5, y0 + 2, (240, 240, 240), (250, 230, 120))
    elif nombre == "joyel":
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.rect((x0 + 3, y0 + 2, x0 + 5, y0 + 4), ORO)
        c.punto(x0 + 3, y0 + 2, (160, 30, 50))
        c.punto(x0 + 4, y0 + 3, PERLA)
    elif nombre == "guantes":
        color = (236, 230, 220) if mujer else (110, 80, 50)
        for capa in ("brazo_d_capa", "brazo_i_capa"):
            c.caja(capa, color, filas=(8, 12), fuerza=0.05)
    elif nombre == "pañuelo a la cabeza":
        _gorro(c, (170, 50, 50), 4, 2)
        x0, y0, _, _ = c.cara("cabeza_capa", "frente")
        for col in (1, 4, 6):
            c.punto(x0 + col, y0, (240, 220, 120))
    elif nombre == "sombrero de paja":
        _gorro(c, (214, 186, 110), 2, 1, detalle=(1, (60, 80, 140)), fuerza=0.12)
    elif nombre == "bolsa de mano":
        x0, y0, _, _ = c.cara("brazo_i_capa", "frente")
        c.rect((x0, y0 + 10, x0 + 3, y0 + 12), (120, 40, 60))
    elif nombre == "pulseras de oro":
        for capa in ("brazo_d_capa", "brazo_i_capa"):
            c.fila(capa, 10, ORO)
    elif nombre == "cinta de terciopelo":
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for cual in LADOS:
            x0, y0, x1, _ = c.cara("cuerpo_capa", cual)
            c.rect((x0, y0, x1, y0 + 1), NEGRO)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.punto(x0 + 3, y0 + 1, ORO)
    elif nombre == "chambergo con pluma":
        _sombrero(c, FIELTRO, (230, 230, 230))
    elif nombre == "sombrero de ala ancha":
        _sombrero(c, (90, 70, 50))
    elif nombre == "sombrero de tres picos":
        _sombrero(c, NEGRO)
        x0, y0, _, _ = c.cara("cabeza_capa", "frente")
        c.punto(x0 + 3, y0, ORO)
        c.punto(x0 + 4, y0, ORO)
    elif nombre == "montera":
        _gorro(c, NEGRO, 3, 2, detalle=(2, (120, 30, 40)))
        for cual in ("derecha", "izquierda"):
            x0, y0, x1, _ = c.cara("cabeza_capa", cual)
            c.rect((x0 + 2, y0 + 3, x0 + 5, y0 + 5), NEGRO)
    elif nombre in ("capa española", "capa corta"):
        color = NEGRO if nombre == "capa española" else (110, 40, 40)
        _hombros(c, color, 3, 3)
        c.caja("cuerpo_capa", color, filas=(0, 12 if nombre == "capa española" else 7), solo=("detras", "derecha"),
               fuerza=0.05)
        c.caja("brazo_d_capa", color, filas=(0, 10 if nombre == "capa española" else 5), fuerza=0.05)
    elif nombre == "espada ropera":
        c.fila("cuerpo_capa", 9, CUERO, solo=None)
        x0, y0, _, _ = c.cara("cuerpo_capa", "izquierda")
        c.rect((x0 + 1, y0 + 8, x0 + 3, y0 + 10), PLATA)              # la cazoleta
        for fila in range(10, 12):
            c.punto(x0 + 2, y0 + fila, NEGRO)
        x0, y0, _, _ = c.cara("pierna_i_capa", "izquierda")
        for fila in range(0, 8):
            c.punto(x0 + 2, y0 + fila, NEGRO)
    elif nombre == "tahalí":
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for i in range(11):
            c.punto(x0 + i * 7 // 10, y0 + i, (120, 40, 40))
            c.punto(x0 + min(7, i * 7 // 10 + 1), y0 + i, ORO if i % 3 == 0 else (120, 40, 40))
    elif nombre == "faja roja":
        c.caja("cuerpo_capa", (170, 40, 40), filas=(8, 10), fuerza=0.05)
        x0, y0, _, _ = c.cara("cuerpo_capa", "izquierda")
        c.rect((x0 + 1, y0 + 10, x0 + 3, y0 + 12), (170, 40, 40))
    elif nombre == "bolsa al cinto":
        c.fila("cuerpo_capa", 8, CUERO, solo=None)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.rect((x0 + 5, y0 + 9, x0 + 7, y0 + 11), oscuro(CUERO, 0.8))
    elif nombre == "cadena de oro":
        _collar(c, ORO, ORO, filas=2)
    elif nombre == "quevedos":
        for col in (1, 2, 5, 6):
            c.punto(fx + col, fy + 4, (230, 240, 245))
        c.punto(fx + 0, fy + 4, NEGRO)
        c.punto(fx + 3, fy + 4, NEGRO)
        c.punto(fx + 4, fy + 4, NEGRO)
        c.punto(fx + 7, fy + 4, NEGRO)
    elif nombre == "pañuelo al cuello":
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for cual in LADOS:
            x0, y0, x1, _ = c.cara("cuerpo_capa", cual)
            c.rect((x0, y0, x1, y0 + 1), (220, 200, 150))
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.rect((x0 + 3, y0 + 1, x0 + 5, y0 + 3), (220, 200, 150))
    elif nombre == "botas con espuelas":
        for capa in CAPAS_PIERNAS:
            c.caja(capa, (70, 54, 40), filas=(5, 12), solo=LADOS, fuerza=0.06)
            c.fila(capa, 5, (90, 70, 52))
            x0, y0, _, _ = c.cara(capa, "detras")
            c.punto(x0 + 1, y0 + 11, PLATA)
            c.punto(x0 + 2, y0 + 11, PLATA)
    elif nombre == "pipa de barro":
        c.punto(fx + 5, fy + 6, (230, 224, 214))
        c.punto(fx + 6, fy + 7, (230, 224, 214))
        c.punto(fx + 7, fy + 7, (200, 190, 180))
    elif nombre == "cinto de cuero":
        c.caja("cuerpo_capa", CUERO, filas=(8, 9), fuerza=0.05)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.punto(x0 + 3, y0 + 8, PLATA)
        c.punto(x0 + 4, y0 + 8, PLATA)
    elif nombre == "parche en el ojo":
        for x in range(8):
            c.punto(fx + x, fy + 2, (40, 34, 30))
        c.rect((fx + 5, fy + 3, fx + 7, fy + 5), (30, 26, 24))
        for cual in ("derecha", "izquierda", "detras"):
            x0, y0, x1, _ = c.cara("cabeza_capa", cual)
            c.rect((x0, y0 + 2, x1, y0 + 3), (40, 34, 30))
    return c


# ================================================================= RASGOS DE CARA (1..20; 0 = ninguno)

MARCAS = ["arrebol en las mejillas", "lunar postizo", "polvo blanco", "pecas", "cicatriz de duelo",
          "cicatriz en la ceja", "marcas de viruela", "nariz roja", "ojeras", "quemado por el sol", "verruga",
          "arrugas", "ojo morado", "labios pintados", "lunar junto a la boca", "colorete fuerte", "tierra en la cara",
          "herida reciente", "cejas finas", "mejillas sonrosadas"]

CICATRIZ, LUNAR, OJERA, ROJO_C = (236, 196, 186), (50, 34, 30), (120, 92, 110), (214, 92, 92)


def marca(sexo, n):
    mujer = sexo == "mujer"
    c = Lienzo(3 if mujer else 4, semilla=3700 + n)
    nombre = MARCAS[n - 1]
    ojo = 5 if mujer else 4
    mejilla = 7 if mujer else 5
    if nombre == "arrebol en las mejillas":
        for x in ((1, 2, 5, 6) if mujer else (0, 1, 6, 7)):
            cf(c, x, mejilla, (230, 80, 90))
        for x in ((1, 6) if mujer else (1, 6)):
            cf(c, x, mejilla - 1 if not mujer else mejilla, (236, 110, 110))
    elif nombre == "lunar postizo":
        cf(c, 6, ojo + 1 if mujer else mejilla, LUNAR)
    elif nombre == "polvo blanco":
        for x in range(8):
            for y in range(1, 8):
                if not (y in (ojo, ojo + (1 if mujer else 0)) and x in (1, 2, 5, 6)) and ruido(x, y, 5) < 0.5:
                    cf(c, x, y, (246, 238, 232))
    elif nombre == "pecas":
        for x, y in ([(1, 7), (2, 7), (5, 7), (6, 7)] if mujer else [(1, 5), (2, 6), (5, 6), (6, 5)]):
            cf(c, x, y, (156, 98, 66))
    elif nombre == "cicatriz de duelo":
        for y in (mejilla - 2, mejilla - 1, mejilla):
            cf(c, 6, y, CICATRIZ)
        cf(c, 7, mejilla - 3, CICATRIZ)
    elif nombre == "cicatriz en la ceja":
        for y in range(ojo - 2, ojo):
            cf(c, 5, y, CICATRIZ)
    elif nombre == "marcas de viruela":
        for x, y in ((1, mejilla), (6, mejilla), (2, 2), (5, 3), (7, mejilla - 1), (0, mejilla - 1)):
            cf(c, x, y, (176, 140, 120))
    elif nombre == "nariz roja":
        cf(c, 3, ojo + (2 if mujer else 1), ROJO_C)
        cf(c, 4, ojo + (2 if mujer else 1), ROJO_C)
    elif nombre == "ojeras":
        for x in (1, 2, 5, 6):
            cf(c, x, ojo + (2 if mujer else 1), OJERA)
    elif nombre == "quemado por el sol":
        for x in range(1, 7):
            cf(c, x, ojo + (2 if mujer else 1), (226, 120, 100) if x in (3, 4) else (220, 140, 120))
    elif nombre == "verruga":
        cf(c, 2, ojo + 2, (140, 110, 90))
    elif nombre == "arrugas":
        for x in (3, 4):
            cf(c, x, 2, oscuro((200, 160, 130), 0.75))
        cf(c, 0, ojo + 1, oscuro((200, 160, 130), 0.75))
        cf(c, 7, ojo + 1, oscuro((200, 160, 130), 0.75))
    elif nombre == "ojo morado":
        for x, y in ((0, ojo), (0, ojo + 1), (1, ojo - 1), (2, ojo - 1), (1, ojo + (2 if mujer else 1))):
            cf(c, x, y, (110, 70, 120))
    elif nombre == "labios pintados":
        if mujer:
            cf(c, 3, 7, (190, 30, 50))
            cf(c, 4, 7, (190, 30, 50))
        else:
            cf(c, 3, 6, (170, 60, 70))
            cf(c, 4, 6, (170, 60, 70))
    elif nombre == "lunar junto a la boca":
        cf(c, 5, 7 if mujer else 6, LUNAR)
    elif nombre == "colorete fuerte":
        for x in range(8):
            if x not in (3, 4):
                cf(c, x, mejilla, (226, 70, 80))
    elif nombre == "tierra en la cara":
        for x, y in ((0, mejilla), (1, mejilla), (6, mejilla - 1), (7, 3), (2, 2)):
            cf(c, x, y, (122, 94, 64))
    elif nombre == "herida reciente":
        for x, y in ((5, 2), (6, 3), (7, 4)):
            cf(c, x, y, (180, 40, 40))
    elif nombre == "cejas finas":
        for x in (1, 2, 5, 6):
            cf(c, x, 3 if mujer else 2, (80, 56, 44))
    else:                                                         # mejillas sonrosadas
        for x in ((0, 1, 6, 7)):
            cf(c, x, mejilla, mezclar((240, 180, 170), (230, 110, 110), 0.4))
    return c


NOMBRES_MARCA = {"hombre": MARCAS, "mujer": MARCAS}
