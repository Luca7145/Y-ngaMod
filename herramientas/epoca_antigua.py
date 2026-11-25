"""Época ANTIGUA (Hispania romana, hacia el siglo I): 20 ropas, 20 peinados, 20 barbas, 20 complementos y 20 rasgos
de cara por sexo. Túnicas, togas, estolas y pallas de lana y lino, sandalias de tiras, fíbulas, coronas de laurel y
oro. Mismo formato que epoca_medieval (ver dibujar_habitantes)."""
from dibujar_habitantes import (Lienzo, cf, oscuro, mezclar, ruido, mechones, flequillo, flequillo_profundo, volumen,
                                cabeza, espalda, BRAZOS, PIERNAS, LADOS, G_BRILLO, G_LUZ, G, G_SOMBRA,
                                G_OSCURO, G_HONDO, COLETA, MONO, TRENZAS, FLEQUILLO, MELENA, TRENZA_ATRAS,
                                CAPAS_PIERNAS, CUERO, ORO, TELA, punos)
from epoca_medieval import _barba_base, _bigote, _perilla, _pecho, _gorro, _banda, _hombros, _flor, _collar

ID = "antigua"

# ================================================================= MATERIALES

LANA = (232, 226, 210)            # lana blanca sin teñir
CRUDO = (214, 200, 170)
PURPURA = (110, 36, 80)           # la púrpura de Tiro
ROJO = (156, 44, 40)
AZUL = (60, 84, 130)
OCRE = (196, 150, 70)
VERDE = (96, 120, 70)
GRIS = (150, 146, 138)
LAUREL = (88, 124, 60)
BRONCE = (176, 120, 60)


def sandalias(c, color=CUERO):
    """Sandalias de tiras: la piel se ve entre las correas (solo se pintan las tiras)."""
    for pierna in PIERNAS:
        c.fila(pierna, 11, color)
        for lado in LADOS:
            x0, y0, x1, _ = c.cara(pierna, lado)
            for x in range(x0, x1):
                if (x - x0) % 2 == 0:
                    c.punto(x, y0 + 10, color)
            c.punto(x0 + 1, y0 + 9, oscuro(color, 0.9))
        c.rect(c.cara(pierna, "abajo"), oscuro(color, 0.8))


def _caer_pliegues(c, nombre, color, filas=None, solo=LADOS):
    """Tela de lana con pliegues verticales (columnas alternas algo más oscuras)."""
    c.caja(nombre, color, filas=filas, fuerza=0.05, solo=solo)
    u, v, w, h, d = c.cajas[nombre]
    a, b = filas if filas else (0, h)
    for lado in solo:
        if lado in ("arriba", "abajo"):
            continue
        x0, y0, x1, _ = c.cara(nombre, lado)
        for x in range(x0, x1):
            if (x - x0) % 3 == 1:
                for y in range(y0 + max(a, 2), y0 + b):
                    c.punto(x, y, oscuro(color, 0.86))


def _diagonal(c, nombre, color, borde=None, desde=0):
    """Pliegue de toga o palla que cruza el pecho del hombro izquierdo a la cadera derecha."""
    x0, y0, _, _ = c.cara(nombre, "frente")
    for i in range(9):
        col = 7 - i * 7 // 8
        for dc in (0, 1, 2):
            if 0 <= col - dc < 8:
                c.punto(x0 + col - dc, y0 + desde + i, color if dc else oscuro(color, 0.85))
        if borde and col - 3 >= 0:
            c.punto(x0 + col - 3, y0 + desde + i, borde)


# ================================================================= ROPA: 5 cortes x 4 colores

COLORES = {
    "hombre": [
        [LANA, ROJO, AZUL, OCRE],                                          # túnica corta con cinturón
        [LANA, LANA, CRUDO, GRIS],                                         # toga (la 2ª con franja púrpura)
        [ROJO, (120, 90, 60), VERDE, GRIS],                                # túnica con capa y fíbula
        [CRUDO, (150, 120, 90), GRIS, (176, 160, 120)],                    # exomis de trabajo
        [LANA, OCRE, AZUL, (130, 70, 60)],                                 # túnica larga con clavos
    ],
    "mujer": [
        [LANA, AZUL, (170, 90, 110), OCRE],                                # estola
        [LANA, VERDE, (150, 100, 140), ROJO],                              # estola con palla
        [LANA, (120, 150, 170), (200, 170, 110), (170, 110, 90)],          # peplo
        [CRUDO, (150, 120, 90), GRIS, (140, 130, 100)],                    # túnica de trabajo
        [LANA, PURPURA, AZUL, (180, 110, 70)],                             # quitón con cintas cruzadas
    ],
}
NOMBRES_ROPA = {
    "hombre": ["túnica corta", "toga", "túnica con capa", "exomis", "túnica larga con franjas"],
    "mujer": ["estola", "estola con palla", "peplo", "túnica de trabajo", "quitón con cintas"],
}
NOMBRES_COLOR = {
    "hombre": [["blanca", "roja", "azul", "ocre"], ["blanca", "con franja púrpura", "cruda", "gris"],
               ["roja", "parda", "verde", "gris"], ["cruda", "parda", "gris", "de lino"],
               ["blanca", "ocre", "azul", "granate"]],
    "mujer": [["blanca", "azul", "rosa", "ocre"], ["blanca", "verde", "malva", "roja"],
              ["blanco", "celeste", "dorado", "teja"], ["cruda", "parda", "gris", "de lino"],
              ["blanco", "púrpura", "azul", "anaranjado"]],
}


def ropa(sexo, n):
    mujer = sexo == "mujer"
    corte, color = divmod(n, 4)
    tela = COLORES[sexo][corte][color]
    c = Lienzo(3 if mujer else 4, semilla=2200 + n + (40 if mujer else 0))
    fx, fy, _, _ = c.cara("cuerpo", "frente")
    claro = tela == LANA or tela == CRUDO

    if not mujer and corte == 0:        # túnica corta hasta la rodilla, mangas cortas, cinturón de cuero
        _caer_pliegues(c, "cuerpo", tela)
        c.caja("cuerpo", tela, solo=("arriba", "abajo"))
        for brazo in BRAZOS:
            c.caja(brazo, tela, filas=(0, 4), fuerza=0.05)
            c.fila(brazo, 3, oscuro(tela, 0.85))
        for pierna in PIERNAS:
            c.caja(pierna, tela, filas=(0, 4), fuerza=0.05)
        for capa in CAPAS_PIERNAS:
            _caer_pliegues(c, capa, tela, filas=(0, 6))
            c.fila(capa, 5, oscuro(tela, 0.8))
        c.fila("cuerpo", 8, CUERO, solo=None)
        c.punto(fx + 4, fy + 8, BRONCE)
        for fila in range(2):           # cuello redondo
            c.punto(fx + 3, fy + fila, oscuro(tela, 0.75))
            c.punto(fx + 4, fy + fila, oscuro(tela, 0.75))
        sandalias(c)
    elif not mujer and corte == 1:      # toga sobre la túnica: tela por el hombro izquierdo y el brazo
        _caer_pliegues(c, "cuerpo", tela)
        c.caja("cuerpo", tela, solo=("arriba", "abajo"))
        for brazo in BRAZOS:
            c.caja(brazo, tela, filas=(0, 4), fuerza=0.05)
        c.caja("brazo_i", tela, filas=(0, 10), fuerza=0.05)          # el brazo que sujeta la toga, tapado
        franja = PURPURA if color == 1 else None
        _diagonal(c, "cuerpo", mezclar(tela, (255, 255, 255), 0.3), franja)
        bx, by, _, _ = c.cara("cuerpo", "detras")
        for i in range(10):
            c.punto(bx + i * 7 // 9, by + i, oscuro(tela, 0.82))
        for pierna in PIERNAS:
            _caer_pliegues(c, pierna, tela, filas=(0, 9))
        for capa in CAPAS_PIERNAS:
            _caer_pliegues(c, capa, tela, filas=(0, 10))
            if franja:
                c.fila(capa, 9, franja)
        sandalias(c)
    elif not mujer and corte == 2:      # túnica de color con capa (sagum) sujeta con fíbula al hombro
        _caer_pliegues(c, "cuerpo", tela)
        c.caja("cuerpo", tela, solo=("arriba", "abajo"))
        for brazo in BRAZOS:
            c.caja(brazo, tela, filas=(0, 4), fuerza=0.05)
        for pierna in PIERNAS:
            c.caja(pierna, tela, filas=(0, 4), fuerza=0.05)
        for capa in CAPAS_PIERNAS:
            _caer_pliegues(c, capa, tela, filas=(0, 6))
        capa_color = oscuro(tela, 0.62) if tela != GRIS else (110, 80, 56)
        c.caja("cuerpo_capa", capa_color, filas=(0, 11), solo=("detras", "derecha"), fuerza=0.06)
        c.caja("cuerpo_capa", capa_color, filas=(0, 2), fuerza=0.06)
        c.caja("brazo_d_capa", capa_color, filas=(0, 3), fuerza=0.06)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.punto(x0 + 1, y0 + 1, ORO)                                  # la fíbula
        c.fila("cuerpo", 8, CUERO, solo=None)
        sandalias(c)
        for pierna in PIERNAS:          # botas de tiras más altas (caligas)
            for lado in LADOS:
                x0, y0, x1, _ = c.cara(pierna, lado)
                c.punto(x0 + 2, y0 + 8, CUERO)
    elif not mujer and corte == 3:      # exomis: túnica de trabajo que deja el hombro derecho al aire
        c.caja("cuerpo", tela, fuerza=0.1)
        x0, y0, x1, _ = c.cara("cuerpo", "frente")
        bx, by, _, _ = c.cara("cuerpo", "detras")
        for col in range(4):            # hombro y pecho derecho (izquierda de la imagen) al aire
            for fila in range(3 - col if col < 3 else 0):
                c.borrar(x0 + col, y0 + fila)
                c.borrar(bx + 7 - col, by + fila)
        dx0, dy0, dx1, _ = c.cara("cuerpo", "derecha")
        for y in range(dy0, dy0 + 3):
            for x in range(dx0, dx1):
                c.borrar(x, y)
        ax0, ay0, _, ay1 = c.cara("cuerpo", "arriba")
        for y in range(ay0, ay1):
            for x in range(ax0, ax0 + 3):
                c.borrar(x, y)
        c.fila("cuerpo", 8, (150, 120, 80), solo=None)               # cuerda
        for pierna in PIERNAS:
            c.caja(pierna, tela, filas=(0, 3), fuerza=0.1)
        for capa in CAPAS_PIERNAS:
            c.caja(capa, tela, filas=(0, 5), solo=LADOS, fuerza=0.1)
        x0, y0, _, _ = c.cara("pierna_d_capa", "frente")
        c.rect((x0 + 1, y0 + 2, x0 + 3, y0 + 4), mezclar(tela, (90, 70, 50), 0.4))   # remiendo
        sandalias(c)
    elif not mujer:                     # túnica larga de mercader, con dos franjas (clavos) de arriba abajo
        _caer_pliegues(c, "cuerpo", tela)
        c.caja("cuerpo", tela, solo=("arriba", "abajo"))
        franja = PURPURA if tela in (LANA, OCRE) else mezclar(tela, (255, 255, 255), 0.5)
        for brazo in BRAZOS:
            c.caja(brazo, tela, filas=(0, 11), fuerza=0.05)
            c.fila(brazo, 10, oscuro(tela, 0.8))
        for fila in range(12):
            if fila != 8:
                c.punto(fx + 2, fy + fila, franja)
                c.punto(fx + 5, fy + fila, franja)
        c.fila("cuerpo", 8, oscuro(tela, 0.6), solo=None)
        for pierna in PIERNAS:
            _caer_pliegues(c, pierna, tela, filas=(0, 10))
        for capa in CAPAS_PIERNAS:
            _caer_pliegues(c, capa, tela, filas=(0, 11))
        x0, y0, _, _ = c.cara("pierna_d_capa", "frente")
        for fila in range(11):
            c.punto(x0 + 2, y0 + fila, franja)
        x0, y0, _, _ = c.cara("pierna_i_capa", "frente")
        for fila in range(11):
            c.punto(x0 + 1, y0 + fila, franja)
        sandalias(c)
    elif corte == 0:                    # estola: vestido largo hasta los pies, ceñido bajo el pecho
        _caer_pliegues(c, "cuerpo", tela)
        c.caja("cuerpo", tela, solo=("arriba", "abajo"))
        for brazo in BRAZOS:
            c.caja(brazo, tela, filas=(0, 3), fuerza=0.05)
        c.fila("cuerpo", 5, ORO if not claro else (170, 150, 110), solo=None)
        for fila in range(2):
            c.punto(fx + 3, fy + fila, oscuro(tela, 0.8))
            c.punto(fx + 4, fy + fila, oscuro(tela, 0.8))
        for pierna in PIERNAS:
            _caer_pliegues(c, pierna, tela, filas=(0, 11))
        for capa in CAPAS_PIERNAS:
            _caer_pliegues(c, capa, mezclar(tela, (255, 255, 255), 0.05), filas=(0, 12))
            c.fila(capa, 11, oscuro(tela, 0.75))
        sandalias(c)
    elif corte == 1:                    # estola con palla: manto echado por un hombro y la espalda
        base = LANA if tela != LANA else (200, 190, 220)
        _caer_pliegues(c, "cuerpo", base)
        c.caja("cuerpo", base, solo=("arriba", "abajo"))
        for brazo in BRAZOS:
            c.caja(brazo, base, filas=(0, 3), fuerza=0.05)
        c.caja("brazo_i", tela, filas=(0, 9), fuerza=0.05)
        _diagonal(c, "cuerpo", tela, oscuro(tela, 0.7))
        c.caja("cuerpo_capa", tela, filas=(0, 11), solo=("detras",), fuerza=0.05)
        for pierna in PIERNAS:
            _caer_pliegues(c, pierna, base, filas=(0, 11))
        for capa in CAPAS_PIERNAS:
            _caer_pliegues(c, capa, base, filas=(0, 12))
        x0, y0, _, _ = c.cara("pierna_d_capa", "frente")
        c.rect((x0, y0, x0 + 4, y0 + 4), tela)                       # la palla cae por delante
        sandalias(c)
    elif corte == 2:                    # peplo: tela doblada arriba (solapa), ceñido a la cintura, sin mangas
        _caer_pliegues(c, "cuerpo", tela)
        c.caja("cuerpo", tela, solo=("arriba", "abajo"))
        c.caja("cuerpo_capa", mezclar(tela, (255, 255, 255), 0.12), filas=(0, 6), solo=LADOS, fuerza=0.05)
        c.fila("cuerpo_capa", 5, oscuro(tela, 0.8))
        c.fila("cuerpo", 7, oscuro(tela, 0.6), solo=None)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.punto(x0 + 1, y0, ORO)
        c.punto(x0 + 6, y0, ORO)
        for pierna in PIERNAS:
            _caer_pliegues(c, pierna, tela, filas=(0, 11))
        for capa in CAPAS_PIERNAS:
            _caer_pliegues(c, capa, tela, filas=(0, 12))
        sandalias(c)
    elif corte == 3:                    # túnica de trabajo a media pierna, mangas cortas y delantal
        c.caja("cuerpo", tela, fuerza=0.1)
        for brazo in BRAZOS:
            c.caja(brazo, tela, filas=(0, 4), fuerza=0.1)
        c.fila("cuerpo", 7, (150, 120, 80), solo=None)
        for pierna in PIERNAS:
            c.caja(pierna, tela, filas=(0, 8), fuerza=0.1)
        for capa in CAPAS_PIERNAS:
            c.caja(capa, tela, filas=(0, 9), solo=LADOS, fuerza=0.1)
        delantal = mezclar(tela, (240, 232, 214), 0.55)
        c.rect((fx + 1, fy + 8, fx + 7, fy + 12), delantal, 0.04)
        for capa in CAPAS_PIERNAS:
            px0, py0, px1, _ = c.cara(capa, "frente")
            c.rect((px0, py0, px1, py0 + 6), delantal, 0.04)
        sandalias(c)
    else:                               # quitón con cintas cruzadas sobre el pecho, mangas abotonadas
        _caer_pliegues(c, "cuerpo", tela)
        c.caja("cuerpo", tela, solo=("arriba", "abajo"))
        for brazo in BRAZOS:
            c.caja(brazo, tela, filas=(0, 5), fuerza=0.05)
            for lado in ("frente", "derecha", "izquierda"):
                x0, y0, _, _ = c.cara(brazo, lado)
                c.punto(x0 + 1, y0 + 1, ORO)
                c.punto(x0 + 1, y0 + 3, ORO)
        cinta = ORO if tela != OCRE else (120, 60, 40)
        for i in range(7):
            c.punto(fx + i + 1 if i < 7 else fx, fy + i, cinta)
            c.punto(fx + 6 - i, fy + i, cinta)
        c.fila("cuerpo", 7, cinta, solo=None)
        for pierna in PIERNAS:
            _caer_pliegues(c, pierna, tela, filas=(0, 11))
        for capa in CAPAS_PIERNAS:
            _caer_pliegues(c, capa, tela, filas=(0, 12))
        sandalias(c)
    return c


def nombre_ropa(sexo, n):
    corte, color = divmod(n, 4)
    return NOMBRES_ROPA[sexo][corte] + " " + NOMBRES_COLOR[sexo][corte][color]


# ================================================================= PEINADOS
# (nombre, lados, nuca, flequillo, (filas de espalda, lados de espalda), piezas 3D, solo adultos, especial)
# Especiales: "rizado", "rizos_frente" (flequillo de rizos), "ondas" (ondas marcadas a los lados), "rapado",
# "calvo", "entradas".
PEINADOS = {
    "mujer": [
        ("moño romano", 5, 8, [5, 3, 2, 2, 1, 2, 3, 5], (0, 0), MONO, False, "ondas"),
        ("moño con rizos en la frente", 5, 8, [5, 3, 3, 2, 2, 3, 3, 5], (0, 0), MONO, False, "rizos_frente"),
        ("trenzas recogidas", 6, 8, [6, 3, 2, 1, 1, 2, 3, 6], (0, 0), TRENZA_ATRAS, False, None),
        ("tutulus de matrona", 5, 8, [4, 2, 2, 1, 1, 2, 2, 4], (0, 0), MONO, True, "ondas"),
        ("nudo griego", 6, 8, [6, 3, 2, 1, 1, 2, 3, 6], (0, 0), MONO, False, "ondas"),
        ("coleta baja", 6, 8, [6, 3, 3, 2, 2, 3, 3, 6], (0, 0), TRENZA_ATRAS, False, None),
        ("trenza en corona", 5, 8, [4, 3, 2, 2, 2, 2, 3, 4], (0, 0), 0, False, "corona"),
        ("melena suelta", 8, 8, [8, 4, 3, 2, 2, 3, 4, 8], (6, 2), MELENA, False, None),
        ("larga con cinta", 8, 8, [8, 3, 2, 1, 1, 2, 3, 8], (6, 2), MELENA, False, "ondas"),
        ("recogido con redecilla", 6, 8, [6, 3, 3, 2, 2, 3, 3, 6], (0, 0), MONO, False, None),
        ("flequillo de rizos", 7, 8, [7, 4, 4, 4, 4, 4, 4, 7], (0, 0), FLEQUILLO, False, "rizos_frente"),
        ("moño alto", 4, 8, [4, 3, 2, 2, 2, 2, 3, 4], (0, 0), MONO, False, None),
        ("rizos sueltos", 8, 8, [8, 4, 3, 4, 3, 4, 3, 8], (5, 2), MELENA, False, "rizado"),
        ("dos trenzas", 6, 8, [7, 3, 4, 3, 3, 4, 3, 7], (0, 0), TRENZAS, False, None),
        ("coleta con rizos", 6, 8, [6, 4, 3, 3, 3, 3, 4, 6], (0, 0), COLETA, False, "rizos_frente"),
        ("media melena ondulada", 7, 8, [7, 3, 2, 2, 1, 2, 3, 7], (0, 0), 0, False, "ondas"),
        ("raya al medio con moño bajo", 6, 8, [6, 3, 2, 1, 1, 2, 3, 6], (0, 0), TRENZA_ATRAS, False, "ondas"),
        ("larguísima", 8, 8, [8, 4, 3, 3, 3, 3, 4, 8], (12, 4), MELENA, False, None),
        ("corta rizada", 5, 7, [6, 4, 3, 4, 3, 4, 3, 6], (0, 0), FLEQUILLO, False, "rizado"),
        ("trenza larga", 6, 8, [6, 3, 4, 2, 3, 4, 3, 6], (0, 0), TRENZA_ATRAS, False, None),
    ],
    "hombre": [
        ("corto romano", 3, 7, [2, 2, 2, 2, 2, 2, 2, 2], (0, 0), 0, False, None),
        ("rizado corto", 3, 6, [2, 2, 1, 2, 2, 1, 2, 2], (0, 0), 0, False, "rizado"),
        ("flequillo de César", 3, 7, [3, 2, 3, 2, 3, 2, 3, 2], (0, 0), FLEQUILLO, False, None),
        ("con entradas", 3, 7, [0, 1, 0, 0, 0, 0, 1, 0], (0, 0), 0, True, "entradas"),
        ("rapado", 4, 7, [1] * 8, (0, 0), 0, False, "rapado"),
        ("rizos griegos", 5, 7, [3, 3, 2, 3, 2, 3, 2, 3], (0, 0), FLEQUILLO, False, "rizado"),
        ("melena de filósofo", 7, 8, [3, 2, 1, 1, 1, 1, 2, 3], (2, 0), MELENA, False, "ondas"),
        ("peinado hacia delante", 3, 7, [3, 3, 3, 2, 2, 3, 3, 3], (0, 0), 0, False, None),
        ("calvo con corona", 4, 6, [0] * 8, (0, 0), 0, True, "calvo"),
        ("raya al lado", 3, 7, [3, 3, 2, 2, 1, 1, 1, 2], (0, 0), 0, False, None),
        ("flequillo de rizos", 4, 7, [3, 3, 3, 3, 3, 3, 3, 3], (0, 0), FLEQUILLO, False, "rizos_frente"),
        ("melena rizada", 7, 8, [4, 3, 2, 3, 2, 3, 2, 4], (3, 1), MELENA, False, "rizado"),
        ("coleta de bárbaro", 5, 8, [2, 2, 1, 1, 1, 1, 2, 2], (0, 0), COLETA, False, None),
        ("corto ondulado", 3, 7, [2, 2, 1, 1, 1, 1, 2, 2], (0, 0), 0, False, "ondas"),
        ("flequillo recto corto", 3, 7, [1] * 8, (0, 0), 0, False, None),
        ("melena con cinta", 7, 8, [3, 2, 1, 0, 0, 1, 2, 3], (2, 0), MELENA, False, None),
        ("despeinado", 4, 7, [3, 2, 3, 1, 3, 2, 3, 2], (0, 0), FLEQUILLO, False, None),
        ("canoso de senador", 3, 7, [1, 1, 0, 0, 0, 0, 1, 1], (0, 0), 0, True, "entradas"),
        ("trenzas de lusitano", 6, 8, [2, 2, 1, 1, 1, 1, 2, 2], (0, 0), TRENZAS, False, None),
        ("rizado largo", 8, 8, [4, 3, 2, 3, 2, 3, 2, 4], (3, 1), MELENA, False, "rizado"),
    ],
}


def _ondas(c):
    """Ondas marcadas: filas alternas de luz y sombra a los lados y detrás (el peinado de las estatuas)."""
    for cual in ("derecha", "izquierda", "detras"):
        x0, y0, x1, y1 = c.cara("cabeza", cual)
        for y in range(y0, y1):
            for x in range(x0, x1):
                if c.leer(x, y)[3]:
                    fase = (y + (x - x0) // 2) % 3
                    c.punto(x, y, G_LUZ if fase == 0 else (G if fase == 1 else G_SOMBRA))


def _rizos_frente(c, mujer):
    """Flequillo de rizos menudos sobre la frente (en la capa exterior, para que tenga relieve)."""
    from dibujar_habitantes import CAPA_X, CAPA_Y
    for col in range(8):
        for fila in range(2):
            if (col + fila) % 2 == 0:
                c.punto(CAPA_X + col, CAPA_Y + fila, G_LUZ if fila == 0 else G)
            else:
                c.punto(CAPA_X + col, CAPA_Y + fila, G_SOMBRA)


def _corona_trenza(c):
    """Trenza que rodea la cabeza como una corona (capa exterior, a media altura)."""
    for cual in ("frente", "derecha", "izquierda", "detras"):
        x0, y0, x1, _ = c.cara("cabeza_capa", cual)
        for x in range(x0, x1):
            c.punto(x, y0, G_LUZ if x % 2 else G_SOMBRA)
            c.punto(x, y0 + 1, G_SOMBRA if x % 2 else G)


def pelo(sexo, n):
    mujer = sexo == "mujer"
    nombre, lados, nuca, fleq, (esp, esp_lados), _, _, especial = PEINADOS[sexo][n]
    c = Lienzo(3 if mujer else 4, semilla=2100 + n * 3 + (50 if mujer else 0))
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
            mechones(c, (x0, y0 + 2, x1, y0 + 5), False)
        x0, y0, x1, _ = c.cara("cabeza", "detras")
        mechones(c, (x0, y0 + 2, x1, y0 + 6), False)
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
    if especial not in ("rapado", "calvo"):
        volumen(c, 0.3 if mujer else 0.45)
    if especial == "rizos_frente":
        _rizos_frente(c, mujer)
    elif especial == "corona":
        _corona_trenza(c)
    return c


# ================================================================= BARBAS (1..20; 0 = ninguna)
# Los romanos van casi siempre afeitados; las barbas son de filósofos, de griegos, de bárbaros o de luto.

BARBAS = [
    "de tres días", "corta recortada", "de filósofo", "griega rizada", "bigote galo", "perilla", "de Adriano",
    "patillas", "de collar", "rizada corta", "completa", "larga", "bifurcada", "de luto", "bigote y barba corta",
    "rala", "puntiaguda", "perilla larga", "muy larga", "bigote fino",
]


def barba(n):
    c = Lienzo(semilla=2400 + n)
    nombre = BARBAS[n - 1]
    if nombre == "de tres días":
        for col in range(8):
            if col not in (3, 4):
                cf(c, col, 7, G_SOMBRA)
        cf(c, 0, 6, G_SOMBRA)
        cf(c, 7, 6, G_SOMBRA)
    elif nombre == "corta recortada":
        _barba_base(c, 2, G_SOMBRA)
    elif nombre == "de filósofo":
        _barba_base(c)
        _bigote(c, "grueso")
        _pecho(c, 4)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for fila in range(4):
            for col in range(2, 6):
                if (col + fila) % 2 == 0 and c.leer(x0 + col, y0 + fila)[3]:
                    c.punto(x0 + col, y0 + fila, G_LUZ)
    elif nombre == "griega rizada":
        _barba_base(c)
        _bigote(c, "grueso")
        _pecho(c, 2)
        for col in range(8):
            for fila in (6, 7):
                if (col + fila) % 2 == 0 and c.leer(8 + col, 8 + fila)[3]:
                    cf(c, col, fila, G_LUZ)
    elif nombre == "bigote galo":
        _bigote(c, "caido")
        for col in (1, 6):
            cf(c, col, 7, G)
    elif nombre == "perilla":
        _perilla(c)
    elif nombre == "de Adriano":
        _barba_base(c, 3, G_SOMBRA)
        _bigote(c, "fino")
        for col in range(1, 7):
            if (col % 2) == 0:
                cf(c, col, 7, G)
    elif nombre == "patillas":
        for col in (0, 7):
            for fila in range(3, 7):
                cf(c, col, fila, G_SOMBRA)
        for lado in ("derecha", "izquierda"):
            x0, y0, x1, _ = c.cara("cabeza", lado)
            mechones(c, (x0, y0 + 3, x0 + 3, y0 + 7), False)
    elif nombre == "de collar":
        for col in range(8):
            cf(c, col, 7, G_SOMBRA)
        for col in (0, 7):
            cf(c, col, 6, G_SOMBRA)
            cf(c, col, 5, G_SOMBRA)
        mechones(c, c.cara("cabeza", "abajo"), False)
    elif nombre == "rizada corta":
        _barba_base(c, 2)
        for col in range(8):
            for fila in (6, 7):
                if (col + fila) % 2 == 0 and c.leer(8 + col, 8 + fila)[3]:
                    cf(c, col, fila, G_LUZ)
    elif nombre == "completa":
        _barba_base(c)
        _bigote(c, "grueso")
    elif nombre == "larga":
        _barba_base(c)
        _bigote(c, "grueso")
        _pecho(c, 3)
    elif nombre == "bifurcada":
        _barba_base(c)
        _bigote(c, "grueso")
        _pecho(c, 4, 6, hueco=True)
    elif nombre == "de luto":
        _barba_base(c, 3, G_SOMBRA)
        for col in range(8):
            for fila in (5, 6, 7):
                if ruido(col, fila, 31) < 0.25:
                    cf(c, col, fila, G_OSCURO)
    elif nombre == "bigote y barba corta":
        _barba_base(c, 2, G_SOMBRA)
        _bigote(c, "grueso")
    elif nombre == "rala":
        for col in range(8):
            for fila in (5, 6, 7):
                if ruido(col, fila, 9) < (0.5 if fila == 7 else 0.25) and not (fila < 7 and 2 <= col <= 5):
                    cf(c, col, fila, G_SOMBRA)
    elif nombre == "puntiaguda":
        _barba_base(c, 2)
        _bigote(c, "fino")
        _pecho(c, 2, 2)
    elif nombre == "perilla larga":
        _bigote(c, "fino")
        _perilla(c, larga=True)
    elif nombre == "muy larga":
        _barba_base(c, 4)
        _bigote(c, "grueso")
        _pecho(c, 6, 6)
    else:                                                          # bigote fino
        _bigote(c, "fino")
    return c


# ================================================================= COMPLEMENTOS (1..20; 0 = ninguno). No se tiñen.

PERLA = (240, 236, 228)
FIELTRO = (150, 120, 80)
PAJA = (214, 186, 110)


def _laurel(c, color=LAUREL, frutos=None):
    """Corona de hojas: una fila de hojas alternas alrededor de la cabeza, abierta por delante."""
    for cual in ("derecha", "izquierda", "detras", "frente"):
        x0, y0, x1, _ = c.cara("cabeza_capa", cual)
        for x in range(x0, x1):
            if cual == "frente" and 2 <= x - x0 <= 5:
                continue
            c.punto(x, y0 + 1, color if x % 2 else oscuro(color, 0.75))
            if x % 2 == 0:
                c.punto(x, y0, mezclar(color, (255, 255, 255), 0.2))
    if frutos:
        x0, y0, _, _ = c.cara("cabeza_capa", "izquierda")
        c.punto(x0 + 3, y0 + 1, frutos)
        x0, y0, _, _ = c.cara("cabeza_capa", "derecha")
        c.punto(x0 + 4, y0 + 1, frutos)


COMPLEMENTOS = {
    "mujer": ["diadema de oro", "corona de laurel", "redecilla dorada", "velo de palla", "pendientes de oro",
              "collar de oro", "collar de perlas", "brazaletes de serpiente", "cinta en el pelo", "fíbula al hombro",
              "corona de flores", "chal", "sombrero de paja", "cinturón dorado", "bolsa al cinto", "colgante de bulla",
              "pendientes de perla", "diadema con gema", "cinta púrpura", "flor en el pelo"],
    "hombre": ["corona de laurel", "corona de roble", "petaso", "gorro frigio", "capa con fíbula", "bulla",
               "cinturón militar", "brazalete de bronce", "zurrón", "cinta en la frente", "manto de lana",
               "bolsa al cinto", "capucha", "palio de filósofo", "gladio al cinto", "torques", "brazales de cuero",
               "tablilla de cera", "anillo en la oreja", "corona de olivo"],
}


def complemento(sexo, n):
    mujer = sexo == "mujer"
    c = Lienzo(3 if mujer else 4, semilla=2600 + n + (40 if mujer else 0))
    nombre = COMPLEMENTOS[sexo][n - 1]
    fx, fy, _, _ = c.cara("cabeza_capa", "frente")
    if nombre == "diadema de oro":
        _banda(c, 1, ORO)
    elif nombre == "diadema con gema":
        _banda(c, 1, ORO)
        c.punto(fx + 3, fy + 1, (60, 140, 120))
        c.punto(fx + 4, fy + 1, (60, 140, 120))
        c.punto(fx + 3, fy, ORO)
        c.punto(fx + 4, fy, ORO)
    elif nombre == "corona de laurel":
        _laurel(c)
    elif nombre == "corona de roble":
        _laurel(c, (110, 120, 50), (150, 110, 50))
    elif nombre == "corona de olivo":
        _laurel(c, (130, 140, 100), (60, 60, 50))
    elif nombre == "corona de flores":
        _banda(c, 1, (80, 130, 60))
        for i, col in enumerate((0, 2, 4, 6)):
            c.punto(fx + col + 1, fy + 1, [(230, 90, 120), (250, 230, 120), (240, 240, 250), (170, 120, 220)][i])
    elif nombre == "redecilla dorada":
        x0, y0, x1, y1 = c.cara("cabeza_capa", "detras")
        for y in range(y0 + 1, y0 + 7):
            for x in range(x0, x1):
                if (x + y) % 2 == 0:
                    c.punto(x, y, ORO)
        _banda(c, 0, ORO, frente=False)
    elif nombre == "velo de palla":
        color = (170, 140, 180)
        _gorro(c, color, 8, 1)
        _hombros(c, color, 2, 2)
        c.caja("cuerpo_capa", color, filas=(0, 6), solo=("detras",), fuerza=0.05)
    elif nombre in ("pendientes de oro", "pendientes de perla"):
        color = ORO if nombre == "pendientes de oro" else PERLA
        for cual in ("derecha", "izquierda"):
            x0, y0, _, _ = c.cara("cabeza_capa", cual)
            c.punto(x0 + 4, y0 + 6, ORO)
            c.punto(x0 + 4, y0 + 7, color)
    elif nombre == "anillo en la oreja":
        x0, y0, _, _ = c.cara("cabeza_capa", "izquierda")
        c.punto(x0 + 4, y0 + 6, ORO)
    elif nombre == "collar de oro":
        _collar(c, ORO, (60, 140, 120))
    elif nombre == "collar de perlas":
        _collar(c, PERLA)
    elif nombre == "brazaletes de serpiente":
        for capa in ("brazo_d_capa", "brazo_i_capa"):
            c.fila(capa, 3, ORO)
            c.fila(capa, 5, ORO)
            x0, y0, _, _ = c.cara(capa, "frente")
            c.punto(x0 + 1, y0 + 4, ORO)
    elif nombre == "brazalete de bronce":
        c.fila("brazo_i_capa", 4, BRONCE)
        c.fila("brazo_i_capa", 5, oscuro(BRONCE, 0.8))
    elif nombre in ("cinta en el pelo", "cinta en la frente", "cinta púrpura"):
        color = PURPURA if nombre == "cinta púrpura" else ((196, 60, 60) if mujer else LANA)
        _banda(c, 1 if mujer else 2, color)
    elif nombre == "fíbula al hombro":
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.punto(x0 + 1, y0, ORO)
        c.punto(x0 + 1, y0 + 1, ORO)
        c.punto(x0 + 2, y0 + 1, oscuro(ORO, 0.8))
    elif nombre == "chal":
        _hombros(c, (180, 160, 200), 4, 3)
    elif nombre == "sombrero de paja":
        _gorro(c, PAJA, 2, 1, detalle=(1, (140, 100, 60)), fuerza=0.12)
    elif nombre == "petaso":
        _gorro(c, FIELTRO, 2, 1, detalle=(1, oscuro(FIELTRO, 0.7)), fuerza=0.1)
        _banda(c, 2, oscuro(FIELTRO, 0.85))
    elif nombre == "gorro frigio":
        _gorro(c, (170, 40, 40), 3, 2)
        x0, y0, _, _ = c.cara("cabeza_capa", "frente")
        c.rect((x0 + 2, y0, x0 + 6, y0 + 1), oscuro((170, 40, 40), 0.8))
    elif nombre == "cinturón dorado":
        c.fila("cuerpo_capa", 6, ORO, solo=None)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.punto(x0 + 3, y0 + 7, ORO)
        c.punto(x0 + 4, y0 + 7, ORO)
    elif nombre in ("bolsa al cinto",):
        c.fila("cuerpo_capa", 8, CUERO, solo=None)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.rect((x0 + 5, y0 + 9, x0 + 7, y0 + 11), oscuro(CUERO, 0.8))
    elif nombre in ("colgante de bulla", "bulla"):
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for col in (2, 5):
            c.punto(x0 + col, y0, CUERO)
        c.punto(x0 + 3, y0 + 1, CUERO)
        c.punto(x0 + 4, y0 + 1, CUERO)
        c.rect((x0 + 3, y0 + 2, x0 + 5, y0 + 4), ORO)
    elif nombre == "capa con fíbula":
        _hombros(c, (140, 40, 40), 3, 2)
        c.caja("cuerpo_capa", (140, 40, 40), filas=(0, 11), solo=("detras",), fuerza=0.06)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.punto(x0 + 1, y0 + 1, ORO)
    elif nombre == "manto de lana":
        _hombros(c, (130, 110, 90), 3, 3)
        c.caja("cuerpo_capa", (130, 110, 90), filas=(0, 9), solo=("detras",), fuerza=0.06)
    elif nombre == "palio de filósofo":
        color = (190, 180, 160)
        c.caja("brazo_i_capa", color, filas=(0, 8), fuerza=0.05)
        _diagonal(c, "cuerpo_capa", color, oscuro(color, 0.75))
        c.caja("cuerpo_capa", color, filas=(0, 10), solo=("detras",), fuerza=0.05)
    elif nombre == "cinturón militar":
        c.fila("cuerpo_capa", 8, CUERO, solo=None)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for col in range(0, 8, 2):
            c.punto(x0 + col, y0 + 8, BRONCE)
        for col in (2, 3, 4, 5):         # el delantal de tiras (pteruges)
            for fila in range(9, 12):
                if col % 2 == 0:
                    c.punto(x0 + col, y0 + fila, CUERO if fila < 11 else BRONCE)
    elif nombre == "zurrón":
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for i in range(9):
            c.punto(x0 + i * 7 // 8, y0 + i, oscuro(CUERO, 0.8))
        c.rect((x0 + 6, y0 + 8, x0 + 8, y0 + 11), CUERO)
    elif nombre == "capucha":
        color = (110, 90, 70)
        _gorro(c, color, 7, 1)
        _hombros(c, color, 2, 1)
    elif nombre == "gladio al cinto":
        c.fila("cuerpo_capa", 8, CUERO, solo=None)
        x0, y0, _, _ = c.cara("cuerpo_capa", "derecha")
        c.punto(x0 + 2, y0 + 8, BRONCE)
        for fila in range(9, 12):
            c.punto(x0 + 2, y0 + fila, (90, 70, 50))
    elif nombre == "torques":
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for col in range(1, 7):
            c.punto(x0 + col, y0 + (0 if col in (1, 6) else 1), ORO)
        c.punto(x0 + 3, y0 + 1, mezclar(ORO, (255, 255, 255), 0.3))
    elif nombre == "brazales de cuero":
        for capa in ("brazo_d_capa", "brazo_i_capa"):
            c.caja(capa, CUERO, filas=(6, 10), fuerza=0.06)
    elif nombre == "tablilla de cera":
        c.fila("cuerpo_capa", 8, CUERO, solo=None)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.rect((x0 + 5, y0 + 9, x0 + 8, y0 + 11), (150, 110, 60))
        c.rect((x0 + 6, y0 + 9, x0 + 7, y0 + 10), (60, 50, 40))
    elif nombre == "flor en el pelo":
        _flor(c, fx + 6, fy + 1, (240, 120, 150), (250, 230, 120))
    return c


# ================================================================= RASGOS DE CARA (1..20; 0 = ninguno)

MARCAS = ["pecas", "kohl en los ojos", "colorete", "polvo blanco", "lunar postizo", "cicatriz de batalla",
          "cicatriz en la ceja", "nariz rota", "quemado por el sol", "ojeras", "arrugas", "verruga", "ojo morado",
          "labios pintados", "sombra azul en los párpados", "polvo de oro", "tierra en la cara", "cejas juntas",
          "marcas de viruela", "herida reciente"]

CICATRIZ, LUNAR, OJERA, ROJO_C = (236, 196, 186), (50, 34, 30), (120, 92, 110), (214, 92, 92)
KOHL = (30, 26, 28)


def marca(sexo, n):
    mujer = sexo == "mujer"
    c = Lienzo(3 if mujer else 4, semilla=2700 + n)
    nombre = MARCAS[n - 1]
    ojo = 5 if mujer else 4
    mejilla = 7 if mujer else 5
    if nombre == "pecas":
        for x, y in ([(1, 7), (2, 7), (5, 7), (6, 7)] if mujer else [(1, 5), (2, 6), (5, 6), (6, 5)]):
            cf(c, x, y, (156, 98, 66))
    elif nombre == "kohl en los ojos":
        for x in (0, 7):
            cf(c, x, ojo, KOHL)
        if mujer:
            cf(c, 0, ojo - 1, KOHL)
            cf(c, 7, ojo - 1, KOHL)
        else:
            for x in (1, 2, 5, 6):
                cf(c, x, ojo + 1, mezclar(KOHL, (200, 160, 130), 0.4))
    elif nombre == "colorete":
        for x in ((1, 2, 5, 6) if mujer else (0, 1, 6, 7)):
            cf(c, x, mejilla, ROJO_C)
    elif nombre == "polvo blanco":
        for x in range(8):
            for y in range(1, 8):
                if not (y in (ojo, ojo + (1 if mujer else 0)) and x in (1, 2, 5, 6)) and ruido(x, y, 5) < 0.5:
                    cf(c, x, y, (246, 238, 232))
    elif nombre == "lunar postizo":
        cf(c, 6, mejilla - 1 if mujer else mejilla, LUNAR)
    elif nombre == "cicatriz de batalla":
        for i in range(4):
            cf(c, 1 + i, 1 + i if i < 2 else mejilla - 2 + i, CICATRIZ)
    elif nombre == "cicatriz en la ceja":
        for y in range(ojo - 2, ojo):
            cf(c, 5, y, CICATRIZ)
    elif nombre == "nariz rota":
        cf(c, 3, ojo, oscuro((200, 160, 130), 0.8))
        cf(c, 4, ojo + 1, oscuro((200, 160, 130), 0.75))
    elif nombre == "quemado por el sol":
        for x in range(1, 7):
            cf(c, x, ojo + (2 if mujer else 1), (226, 120, 100) if x in (3, 4) else (220, 140, 120))
    elif nombre == "ojeras":
        for x in (1, 2, 5, 6):
            cf(c, x, ojo + (2 if mujer else 1), OJERA)
    elif nombre == "arrugas":
        for x in (3, 4):
            cf(c, x, 2, oscuro((200, 160, 130), 0.75))
        cf(c, 0, ojo + 1, oscuro((200, 160, 130), 0.75))
        cf(c, 7, ojo + 1, oscuro((200, 160, 130), 0.75))
    elif nombre == "verruga":
        cf(c, 2, ojo + 2, (140, 110, 90))
    elif nombre == "ojo morado":
        for x, y in ((0, ojo), (0, ojo + 1), (1, ojo - 1), (2, ojo - 1), (1, ojo + (2 if mujer else 1))):
            cf(c, x, y, (110, 70, 120))
    elif nombre == "labios pintados":
        if mujer:
            cf(c, 3, 7, (190, 40, 60))
            cf(c, 4, 7, (190, 40, 60))
        else:
            cf(c, 3, 6, (170, 60, 70))
            cf(c, 4, 6, (170, 60, 70))
    elif nombre == "sombra azul en los párpados":
        for x in ((1, 2, 5, 6) if mujer else (1, 2, 5, 6)):
            cf(c, x, ojo - 1 if not mujer else 3, (80, 120, 170))
    elif nombre == "polvo de oro":
        for x, y in ((0, mejilla), (7, mejilla), (1, 2), (6, 2), (0, 3), (7, 3)):
            cf(c, x, y, (230, 196, 100))
    elif nombre == "tierra en la cara":
        for x, y in ((0, mejilla), (1, mejilla), (6, mejilla - 1), (7, 3), (2, 2)):
            cf(c, x, y, (122, 94, 64))
    elif nombre == "cejas juntas":
        for x in (3, 4):
            cf(c, x, 3 if not mujer else 3, (60, 44, 36))
    elif nombre == "marcas de viruela":
        for x, y in ((1, mejilla), (6, mejilla), (2, 2), (5, 3), (7, mejilla - 1), (0, mejilla - 1)):
            cf(c, x, y, (176, 140, 120))
    else:                                                         # herida reciente
        for x, y in ((5, 2), (6, 3), (7, 4)):
            cf(c, x, y, (180, 40, 40))
    return c


NOMBRES_MARCA = {"hombre": MARCAS, "mujer": MARCAS}
