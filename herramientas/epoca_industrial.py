"""Época INDUSTRIAL (la España de finales del siglo XIX, hacia 1890): 20 ropas, 20 peinados, 20 barbas, 20
complementos y 20 rasgos de cara por sexo. Trajes de tres piezas, blusones de obrero, chulapos y chulapas, polisones,
sirvientas con delantal, bombines, boinas, mantones de Manila y relojes de bolsillo. Mismo formato que epoca_medieval."""
from dibujar_habitantes import (Lienzo, cf, oscuro, mezclar, ruido, mechones, flequillo, flequillo_profundo, volumen,
                                cabeza, espalda, BRAZOS, PIERNAS, LADOS, G_BRILLO, G_LUZ, G, G_SOMBRA,
                                G_OSCURO, G_HONDO, COLETA, MONO, TRENZAS, FLEQUILLO, MELENA, TRENZA_ATRAS,
                                CAPAS_PIERNAS, CUERO, LINO, ORO, TELA, CAPA_X, CAPA_Y)
from epoca_medieval import _barba_base, _bigote, _perilla, _pecho, _gorro, _banda, _hombros, _flor, _collar

ID = "industrial"

NEGRO = (38, 36, 40)
BLANCO = (238, 236, 230)
MAHON = (58, 84, 130)              # azul de obrero
PANA = (120, 90, 60)
PLATA = (196, 200, 206)


def botines(c, color=NEGRO, desde=10):
    for pierna in PIERNAS:
        c.caja(pierna, color, filas=(desde, 12), fuerza=0.04)
        x0, y0, _, _ = c.cara(pierna, "frente")
        c.punto(x0 + 1, y0 + desde, mezclar(color, (255, 255, 255), 0.25))


def alpargatas(c):
    for pierna in PIERNAS:
        c.caja(pierna, (220, 210, 190), filas=(11, 12), fuerza=0.04)
        for lado in LADOS:
            x0, y0, x1, _ = c.cara(pierna, lado)
            for x in range(x0, x1, 2):
                c.punto(x, y0 + 10, NEGRO)


def camisa_cuello(c, corbata=None, pajarita=None):
    """Cuello de camisa blanco y, si se pide, corbata o pajarita."""
    fx, fy, _, _ = c.cara("cuerpo", "frente")
    for col in (2, 3, 4, 5):
        c.punto(fx + col, fy, BLANCO)
    for fila in range(1, 4):
        c.punto(fx + 3, fy + fila, BLANCO)
        c.punto(fx + 4, fy + fila, BLANCO)
    if corbata:
        for fila in range(0, 5):
            c.punto(fx + 3 + (fila % 2 if fila > 1 else 0), fy + fila, corbata)
        c.punto(fx + 4, fy + 1, corbata)
    if pajarita:
        for col in (2, 3, 4, 5):
            c.punto(fx + col, fy + 1, pajarita)
        c.punto(fx + 3, fy, oscuro(pajarita, 0.8))
        c.punto(fx + 4, fy, oscuro(pajarita, 0.8))


def cuadros(c, nombre, color, filas=None, solo=LADOS):
    """Tela de cuadros finos (pantalones de chulapo, faldas)."""
    c.caja(nombre, color, filas=filas, fuerza=0.04, solo=solo)
    _, _, _, h, _ = c.cajas[nombre]
    a, b = filas if filas else (0, h)
    for lado in solo:
        x0, y0, x1, _ = c.cara(nombre, lado)
        for y in range(y0 + a, y0 + b):
            for x in range(x0, x1):
                if (x - x0) % 2 == 0 or (y - y0) % 2 == 0:
                    c.punto(x, y, oscuro(color, 0.82) if (x - x0) % 2 == 0 and (y - y0) % 2 == 0 else
                            oscuro(color, 0.92))


# ================================================================= ROPA: 5 cortes x 4 colores

COLORES = {
    "hombre": [
        [NEGRO, (90, 90, 96), (96, 70, 50), (36, 46, 76)],                 # traje de tres piezas
        [MAHON, (110, 110, 110), (110, 86, 60), (90, 96, 66)],             # obrero
        [(70, 70, 80), (110, 80, 60), (60, 70, 60), (120, 110, 100)],      # chulapo (color del chaleco)
        [PANA, (90, 80, 60), (70, 76, 60), (130, 110, 80)],                # campesino de pana
        [(36, 50, 90), (50, 76, 56), (100, 100, 104), (110, 40, 50)],      # uniforme con botones dorados
    ],
    "mujer": [
        [(120, 40, 60), (50, 70, 110), (70, 100, 80), (170, 140, 110)],    # vestido con polisón
        [MAHON, (120, 100, 80), (110, 110, 120), (140, 90, 80)],           # obrera
        [(30, 30, 36), (120, 30, 40), (60, 60, 100), (80, 60, 50)],        # chulapa (falda)
        [NEGRO, (50, 50, 70), (70, 50, 50), (60, 60, 60)],                 # sirvienta
        [(90, 60, 80), (60, 80, 90), (150, 120, 90), (80, 90, 60)],        # traje de paseo
    ],
}
NOMBRES_ROPA = {
    "hombre": ["traje de tres piezas", "blusón de obrero", "traje de chulapo", "traje de pana", "uniforme"],
    "mujer": ["vestido con polisón", "traje de obrera", "traje de chulapa", "uniforme de sirvienta", "traje de paseo"],
}
NOMBRES_COLOR = {
    "hombre": [["negro", "gris", "marrón", "azul marino"], ["azul mahón", "gris", "pardo", "verde oliva"],
               ["gris", "marrón", "verde", "beis"], ["marrón", "pardo", "verde", "claro"],
               ["azul", "verde", "gris", "granate"]],
    "mujer": [["granate", "azul", "verde", "beis"], ["azul", "parda", "gris", "teja"],
              ["negra", "roja", "azul", "marrón"], ["negro", "azul noche", "granate", "gris"],
              ["malva", "azul", "camel", "verde"]],
}


def ropa(sexo, n):
    mujer = sexo == "mujer"
    corte, color = divmod(n, 4)
    tela = COLORES[sexo][corte][color]
    c = Lienzo(3 if mujer else 4, semilla=4200 + n + (40 if mujer else 0))
    fx, fy, _, _ = c.cara("cuerpo", "frente")

    if not mujer and corte == 0:        # chaqueta, chaleco, camisa con corbata, pantalón largo y botines
        c.caja("cuerpo", tela, fuerza=TELA)
        for brazo in BRAZOS:
            c.caja(brazo, tela, filas=(0, 11), fuerza=TELA)
            c.fila(brazo, 11, BLANCO)
        chaleco = oscuro(tela, 0.75) if tela != NEGRO else (80, 70, 60)
        c.rect((fx + 2, fy + 1, fx + 6, fy + 9), chaleco, 0.04)
        for fila in (3, 5, 7):
            c.punto(fx + 4, fy + fila, ORO)
        camisa_cuello(c, corbata=(110, 30, 40) if color % 2 else (40, 40, 60))
        c.punto(fx + 5, fy + 5, ORO)                   # la cadena del reloj
        c.punto(fx + 6, fy + 6, ORO)
        for pierna in PIERNAS:
            c.caja(pierna, tela, fuerza=TELA)
        botines(c)
    elif not mujer and corte == 1:      # blusón de obrero, pantalón de faena y botas
        c.caja("cuerpo", tela, fuerza=0.09)
        for brazo in BRAZOS:
            c.caja(brazo, tela, filas=(0, 11), fuerza=0.09)
            c.fila(brazo, 10, oscuro(tela, 0.78))
        for fila in range(0, 4):
            c.punto(fx + 4, fy + fila, oscuro(tela, 0.7))
        c.punto(fx + 3, fy, BLANCO)
        c.rect((fx + 1, fy + 3, fx + 3, fy + 5), oscuro(tela, 0.85))           # bolsillo
        for pierna in PIERNAS:
            c.caja(pierna, (70, 66, 60), fuerza=0.09)
        c.caja("cuerpo", (70, 66, 60), filas=(11, 12), fuerza=0.09)
        x0, y0, _, _ = c.cara("pierna_d", "frente")
        c.rect((x0 + 1, y0 + 4, x0 + 3, y0 + 6), mezclar((70, 66, 60), (130, 120, 100), 0.4))   # remiendo
        botines(c, (60, 44, 34), 9)
    elif not mujer and corte == 2:      # chulapo: chaleco ceñido, camisa blanca, pantalón de cuadros y botines
        c.caja("cuerpo", BLANCO, fuerza=0.04)
        for brazo in BRAZOS:
            c.caja(brazo, BLANCO, filas=(0, 11), fuerza=0.04)
            c.fila(brazo, 10, oscuro(BLANCO, 0.88))
        c.caja("cuerpo", tela, filas=(0, 10), solo=("derecha", "izquierda", "detras"), fuerza=0.04)
        c.rect((fx, fy + 1, fx + 3, fy + 10), tela, 0.04)
        c.rect((fx + 5, fy + 1, fx + 8, fy + 10), tela, 0.04)
        for fila in (2, 4, 6, 8):
            c.punto(fx + 2, fy + fila, PLATA)
        camisa_cuello(c, pajarita=None)
        c.punto(fx + 3, fy + 1, (190, 40, 50))          # pañuelo rojo al cuello
        c.punto(fx + 4, fy + 1, (190, 40, 50))
        for pierna in PIERNAS:
            cuadros(c, pierna, (150, 140, 120) if color != 3 else (100, 96, 90), filas=(0, 11))
        c.caja("cuerpo", (40, 40, 44), filas=(10, 12), fuerza=0.04)                  # faja negra
        botines(c, NEGRO, 11)
    elif not mujer and corte == 3:      # campesino: camisa, chaleco de pana, faja, pantalón de pana y alpargatas
        c.caja("cuerpo", LINO, fuerza=0.05)
        for brazo in BRAZOS:
            c.caja(brazo, LINO, filas=(0, 9), fuerza=0.05)
            c.fila(brazo, 8, oscuro(LINO, 0.85))
        c.caja("cuerpo", tela, filas=(0, 9), solo=("derecha", "izquierda", "detras"), fuerza=0.1)
        c.rect((fx, fy, fx + 3, fy + 9), tela, 0.1)
        c.rect((fx + 5, fy, fx + 8, fy + 9), tela, 0.1)
        c.caja("cuerpo", (40, 40, 60), filas=(9, 11), fuerza=0.05)                    # faja
        for pierna in PIERNAS:
            c.caja(pierna, oscuro(tela, 0.8), filas=(0, 11), fuerza=0.1)
            for lado in LADOS:
                x0, y0, x1, _ = c.cara(pierna, lado)
                for x in range(x0, x1, 2):
                    for y in range(y0, y0 + 11):
                        c.punto(x, y, oscuro(tela, 0.72))      # canalé de la pana
        c.caja("cuerpo", oscuro(tela, 0.8), filas=(11, 12), fuerza=0.1)
        alpargatas(c)
    elif not mujer:                     # uniforme (ferroviario, cartero…): guerrera abotonada, ribetes dorados
        c.caja("cuerpo", tela, fuerza=0.05)
        for brazo in BRAZOS:
            c.caja(brazo, tela, filas=(0, 12), fuerza=0.05)
            c.fila(brazo, 10, ORO)
        for fila in range(1, 11, 2):
            c.punto(fx + 2, fy + fila, ORO)
            c.punto(fx + 5, fy + fila, ORO)
        c.fila("cuerpo", 0, oscuro(tela, 0.7))
        c.fila("cuerpo", 9, NEGRO, solo=None)
        c.punto(fx + 3, fy + 9, ORO)
        c.punto(fx + 4, fy + 9, ORO)
        for pierna in PIERNAS:
            c.caja(pierna, oscuro(tela, 0.7), fuerza=0.05)
            x0, y0, _, _ = c.cara(pierna, "derecha")
            for fila in range(10):
                c.punto(x0 + 1, y0 + fila, (170, 40, 40))
        botines(c)
    elif corte == 0:                    # vestido de cuello alto con polisón: cuerpo entallado y falda con volantes
        c.caja("cuerpo", tela, fuerza=0.04)
        for brazo in BRAZOS:
            c.caja(brazo, tela, fuerza=0.04)
            c.fila(brazo, 11, BLANCO)
        for capa in ("brazo_d_capa", "brazo_i_capa"):          # mangas de jamón
            c.caja(capa, mezclar(tela, (255, 255, 255), 0.1), filas=(0, 3), fuerza=0.04)
        c.fila("cuerpo", 0, BLANCO)
        for fila in range(1, 8, 2):
            c.punto(fx + 4, fy + fila, mezclar(tela, (255, 255, 255), 0.4))
        c.fila("cuerpo", 7, oscuro(tela, 0.7), solo=None)
        for pierna in PIERNAS:
            c.caja(pierna, tela, fuerza=0.04)
        for capa in CAPAS_PIERNAS:
            c.caja(capa, mezclar(tela, (255, 255, 255), 0.05), filas=(0, 12), solo=LADOS, fuerza=0.04)
            for fila in (6, 9):
                c.fila(capa, fila, oscuro(tela, 0.75))
        c.caja("cuerpo_capa", mezclar(tela, (255, 255, 255), 0.08), filas=(8, 12), solo=("detras",), fuerza=0.04)
        botines(c)
    elif corte == 1:                    # obrera: blusa de manga larga, falda larga, delantal y mangas remangadas
        blusa = mezclar(tela, (255, 255, 255), 0.55)
        c.caja("cuerpo", blusa, fuerza=0.05)
        for brazo in BRAZOS:
            c.caja(brazo, blusa, filas=(0, 8), fuerza=0.05)
            c.fila(brazo, 7, oscuro(blusa, 0.85))
        c.caja("cuerpo", tela, filas=(7, 12), fuerza=0.07)
        for pierna in PIERNAS:
            c.caja(pierna, tela, fuerza=0.07)
        for capa in CAPAS_PIERNAS:
            c.caja(capa, oscuro(tela, 0.92), filas=(0, 11), solo=LADOS, fuerza=0.07)
        delantal = (200, 196, 186)
        c.rect((fx + 1, fy + 3, fx + 7, fy + 12), delantal, 0.05)
        for capa in CAPAS_PIERNAS:
            px0, py0, px1, _ = c.cara(capa, "frente")
            c.rect((px0, py0, px1, py0 + 8), delantal, 0.05)
        c.fila("cuerpo", 7, oscuro(delantal, 0.8), solo=("derecha", "izquierda", "detras"))
        botines(c, (60, 44, 34))
    elif corte == 2:                    # chulapa: blusa blanca ceñida y falda larga ajustada con volante
        c.caja("cuerpo", BLANCO, fuerza=0.04)
        for brazo in BRAZOS:
            c.caja(brazo, BLANCO, fuerza=0.04)
            c.fila(brazo, 11, oscuro(BLANCO, 0.88))
        for fila in range(1, 7, 2):
            c.punto(fx + 4, fy + fila, PLATA)
        c.caja("cuerpo", tela, filas=(7, 12), fuerza=0.04)
        for pierna in PIERNAS:
            c.caja(pierna, tela, fuerza=0.04)
        for capa in CAPAS_PIERNAS:
            c.caja(capa, tela, filas=(8, 12), solo=LADOS, fuerza=0.04)
            c.fila(capa, 8, oscuro(tela, 0.7))
            c.fila(capa, 11, BLANCO)
        botines(c)
    elif corte == 3:                    # sirvienta: vestido oscuro, puños y cuello blancos, delantal blanco
        c.caja("cuerpo", tela, fuerza=0.04)
        for brazo in BRAZOS:
            c.caja(brazo, tela, fuerza=0.04)
            c.fila(brazo, 10, BLANCO)
            c.fila(brazo, 11, BLANCO)
        c.rect((fx + 2, fy, fx + 6, fy + 1), BLANCO)
        for pierna in PIERNAS:
            c.caja(pierna, tela, fuerza=0.04)
        for capa in CAPAS_PIERNAS:
            c.caja(capa, tela, filas=(0, 11), solo=LADOS, fuerza=0.04)
        c.rect((fx + 1, fy + 2, fx + 7, fy + 12), BLANCO, 0.03)
        for cual in ("derecha", "izquierda"):
            x0, y0, x1, _ = c.cara("cuerpo", cual)
            c.punto(x0 + 1, y0 + 2, BLANCO)
        c.caja("cuerpo", BLANCO, filas=(7, 8), solo=("derecha", "izquierda", "detras"))
        for capa in CAPAS_PIERNAS:
            px0, py0, px1, _ = c.cara(capa, "frente")
            c.rect((px0, py0, px1, py0 + 9), BLANCO, 0.03)
        botines(c)
    else:                               # traje de paseo: chaqueta entallada con solapas y falda a juego
        c.caja("cuerpo", tela, fuerza=0.05)
        for brazo in BRAZOS:
            c.caja(brazo, tela, fuerza=0.05)
            c.fila(brazo, 11, BLANCO)
        for fila in range(0, 4):
            c.punto(fx + 3, fy + fila, BLANCO)
            c.punto(fx + 4, fy + fila, BLANCO)
        c.punto(fx + 2, fy + 1, oscuro(tela, 0.7))
        c.punto(fx + 5, fy + 1, oscuro(tela, 0.7))
        for fila in (4, 6, 8):
            c.punto(fx + 4, fy + fila, oscuro(tela, 0.6))
        for pierna in PIERNAS:
            c.caja(pierna, tela, fuerza=0.05)
        for capa in CAPAS_PIERNAS:
            c.caja(capa, oscuro(tela, 0.95), filas=(0, 12), solo=LADOS, fuerza=0.05)
        botines(c)
    return c


def nombre_ropa(sexo, n):
    corte, color = divmod(n, 4)
    return NOMBRES_ROPA[sexo][corte] + " " + NOMBRES_COLOR[sexo][corte][color]


# ================================================================= PEINADOS
# Especiales: "rizado", "ondas", "engominado" (brillo liso), "rapado", "calvo", "entradas", "rodete" (moño trenzado
# en espiral detrás), "pompadour" (moño alto y ahuecado por delante).
PEINADOS = {
    "mujer": [
        ("moño alto ahuecado", 5, 8, [4, 3, 2, 2, 2, 2, 3, 4], (0, 0), MONO, False, "pompadour"),
        ("recogido con flequillo rizado", 6, 8, [6, 4, 4, 4, 4, 4, 4, 6], (0, 0), MONO | FLEQUILLO, False, "rizado"),
        ("rodete", 6, 8, [6, 3, 2, 1, 1, 2, 3, 6], (0, 0), 0, False, "rodete"),
        ("moño bajo con raya al medio", 6, 8, [6, 3, 2, 1, 1, 2, 3, 6], (0, 0), TRENZA_ATRAS, False, "ondas"),
        ("trenza larga", 6, 8, [6, 3, 4, 2, 3, 4, 3, 6], (0, 0), TRENZA_ATRAS, False, None),
        ("dos trenzas", 6, 8, [7, 3, 4, 3, 3, 4, 3, 7], (0, 0), TRENZAS, False, None),
        ("tirabuzones", 8, 8, [8, 4, 3, 3, 3, 3, 4, 8], (5, 2), MELENA, False, "rizado"),
        ("caracoles en la frente", 6, 8, [6, 3, 2, 2, 2, 2, 3, 6], (0, 0), MONO, False, "caracoles"),
        ("larga suelta", 8, 8, [8, 3, 2, 1, 1, 2, 3, 8], (7, 2), MELENA, False, None),
        ("media melena ondulada", 7, 8, [7, 3, 2, 2, 1, 2, 3, 7], (0, 0), 0, False, "ondas"),
        ("coleta con lazo", 6, 8, [6, 4, 3, 2, 3, 3, 4, 6], (0, 0), COLETA, False, None),
        ("moño de obrera", 5, 8, [5, 3, 3, 2, 2, 3, 3, 5], (0, 0), MONO, False, None),
        ("flequillo recto con moño", 5, 8, [5, 4, 4, 4, 4, 4, 4, 5], (0, 0), MONO | FLEQUILLO, False, None),
        ("ondas al agua", 7, 8, [7, 4, 3, 2, 2, 3, 4, 7], (2, 1), 0, False, "ondas"),
        ("larga con flequillo", 8, 8, [8, 4, 4, 3, 4, 3, 4, 8], (6, 2), FLEQUILLO | MELENA, False, None),
        ("recogido con raya al lado", 6, 8, [7, 4, 3, 2, 1, 1, 2, 5], (0, 0), MONO, False, None),
        ("rizada corta", 5, 7, [6, 4, 3, 4, 3, 4, 3, 6], (0, 0), FLEQUILLO, False, "rizado"),
        ("larguísima", 8, 8, [8, 4, 3, 3, 3, 3, 4, 8], (12, 4), MELENA, False, None),
        ("trenzas recogidas", 6, 8, [6, 3, 2, 2, 2, 2, 3, 6], (0, 0), 0, False, "rodete"),
        ("moño alto con flequillo", 5, 8, [5, 4, 3, 4, 3, 4, 3, 5], (0, 0), MONO | FLEQUILLO, False, "pompadour"),
    ],
    "hombre": [
        ("raya al lado engominado", 3, 7, [3, 3, 2, 1, 0, 0, 0, 1], (0, 0), 0, False, "engominado"),
        ("raya al medio", 3, 7, [2, 2, 1, 0, 0, 1, 2, 2], (0, 0), 0, False, "engominado"),
        ("peinado hacia atrás", 3, 7, [1, 1, 0, 0, 0, 0, 1, 1], (0, 0), 0, False, "engominado"),
        ("corto", 3, 7, [2, 2, 1, 2, 2, 1, 2, 2], (0, 0), 0, False, None),
        ("rapado", 4, 7, [1] * 8, (0, 0), 0, False, "rapado"),
        ("rizado", 4, 7, [2, 3, 2, 3, 2, 3, 2, 3], (0, 0), FLEQUILLO, False, "rizado"),
        ("con entradas", 3, 7, [0, 1, 0, 0, 0, 0, 1, 0], (0, 0), 0, True, "entradas"),
        ("calvo con corona", 4, 6, [0] * 8, (0, 0), 0, True, "calvo"),
        ("caracol de chulapo", 3, 7, [0, 0, 0, 0, 0, 3, 2, 0], (0, 0), 0, False, "engominado"),
        ("flequillo corto", 3, 7, [3, 3, 3, 2, 2, 3, 3, 3], (0, 0), FLEQUILLO, False, None),
        ("melena de artista", 7, 8, [3, 2, 2, 1, 1, 2, 2, 3], (2, 0), MELENA, False, "ondas"),
        ("despeinado", 4, 7, [3, 2, 3, 1, 3, 2, 3, 2], (0, 0), FLEQUILLO, False, None),
        ("ondulado corto", 3, 7, [2, 2, 1, 1, 1, 1, 2, 2], (0, 0), 0, False, "ondas"),
        ("rizado corto", 3, 6, [2, 2, 1, 2, 2, 1, 2, 2], (0, 0), 0, False, "rizado"),
        ("tupé", 3, 7, [1, 2, 3, 3, 3, 2, 1, 1], (0, 0), FLEQUILLO, False, None),
        ("rapado a los lados", 2, 5, [2, 2, 2, 2, 2, 2, 2, 2], (0, 0), 0, False, None),
        ("canoso con entradas", 3, 7, [0, 1, 0, 0, 0, 0, 1, 0], (0, 0), 0, True, "entradas"),
        ("melena con raya", 7, 8, [4, 2, 1, 0, 0, 1, 2, 4], (2, 0), MELENA, False, None),
        ("pelo de cepillo", 3, 6, [1] * 8, (0, 0), 0, False, "cepillo"),
        ("raya al lado ondulado", 3, 7, [3, 3, 2, 2, 1, 1, 1, 2], (0, 0), 0, False, "ondas"),
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


def _engominado(c):
    """Pelo liso y brillante pegado a la cabeza: tonos uniformes con una línea de brillo."""
    for cual in ("arriba", "derecha", "izquierda", "detras"):
        x0, y0, x1, y1 = c.cara("cabeza", cual)
        for y in range(y0, y1):
            for x in range(x0, x1):
                if c.leer(x, y)[3]:
                    c.punto(x, y, G_BRILLO if (cual == "arriba" and x - x0 == 2) or (y - y0 == 1 and cual != "arriba")
                            else G_SOMBRA)


def _rodete(c):
    """Moño trenzado en espiral, plano, en la nuca (capa exterior)."""
    x0, y0, _, _ = c.cara("cabeza_capa", "detras")
    for (x, y) in ((2, 3), (3, 3), (4, 3), (5, 3), (5, 4), (5, 5), (4, 6), (3, 6), (2, 6), (2, 5), (2, 4), (3, 4),
                   (4, 4), (4, 5), (3, 5)):
        c.punto(x0 + x, y0 + y, G_LUZ if (x + y) % 2 else G_SOMBRA)


def _pompadour(c):
    """Pelo ahuecado alto por delante y arriba (capa exterior)."""
    x0, y0, x1, y1 = c.cara("cabeza_capa", "arriba")
    for y in range(y0, y1):
        for x in range(x0, x1):
            c.punto(x, y, G_LUZ if (x + y) % 3 else G)
    for cual in ("frente", "derecha", "izquierda"):
        x0, y0, x1, _ = c.cara("cabeza_capa", cual)
        for x in range(x0, x1):
            c.punto(x, y0, G_LUZ if x % 2 else G)


def _caracoles(c):
    """Caracolillos pegados a la frente y las sienes (moda de las majas y las chulapas)."""
    for col, fila in ((0, 2), (1, 1), (6, 1), (7, 2)):
        c.punto(CAPA_X + col, CAPA_Y + fila, G_SOMBRA)
        c.punto(CAPA_X + col, CAPA_Y + fila + 1, G_LUZ)


def pelo(sexo, n):
    mujer = sexo == "mujer"
    nombre, lados, nuca, fleq, (esp, esp_lados), _, _, especial = PEINADOS[sexo][n]
    c = Lienzo(3 if mujer else 4, semilla=4100 + n * 3 + (50 if mujer else 0))
    if not mujer:
        for col in (1, 2, 5, 6):
            cf(c, col, 3, G_OSCURO)
    if especial in ("rapado", "cepillo"):
        for cual, filas in (("arriba", 8), ("derecha", 4), ("izquierda", 4), ("detras", 7)):
            x0, y0, x1, _ = c.cara("cabeza", cual)
            for y in range(y0, y0 + filas):
                for x in range(x0, x1):
                    c.punto(x, y, G_SOMBRA if ruido(x, y, 3) < 0.7 else G_OSCURO)
        if especial == "cepillo":                   # de punta: la capa de arriba, como un cepillo
            x0, y0, x1, y1 = c.cara("cabeza_capa", "arriba")
            for y in range(y0, y1):
                for x in range(x0, x1):
                    if (x + y) % 2 == 0:
                        c.punto(x, y, G)
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
    if especial not in ("rapado", "calvo", "cepillo"):
        (flequillo_profundo if mujer else flequillo)(c, fleq)
    elif especial in ("rapado", "cepillo"):
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
    elif especial == "engominado":
        _engominado(c)
    if especial not in ("rapado", "calvo", "cepillo", "engominado"):
        volumen(c, 0.3 if mujer else 0.45)
    if especial == "rodete":
        _rodete(c)
    elif especial == "pompadour":
        _pompadour(c)
    elif especial == "caracoles":
        _caracoles(c)
    return c


# ================================================================= BARBAS (1..20; 0 = ninguna)

BARBAS = [
    "bigote de manillar", "patillas de hacha", "patillas con bigote", "barba completa", "bigote de morsa",
    "perilla imperial", "bigote fino", "barba de candado", "barba larga", "de tres días", "bigote y perilla",
    "barba recortada", "patillas", "bigote grueso", "barba de chivo", "barba bifurcada", "barba rala",
    "barba de marinero", "barba de anciano", "bigote caído",
]


def barba(n):
    c = Lienzo(semilla=4400 + n)
    nombre = BARBAS[n - 1]
    if nombre == "bigote de manillar":
        _bigote(c, "manillar")
        cf(c, 0, 3, G_LUZ)
        cf(c, 7, 3, G_LUZ)
    elif nombre in ("patillas de hacha", "patillas con bigote"):
        for col in (0, 1, 6, 7):
            for fila in range(3, 8):
                if not (col in (1, 6) and fila < 5):
                    cf(c, col, fila, G_SOMBRA if col in (0, 7) else G)
        for lado in ("derecha", "izquierda"):
            x0, y0, x1, _ = c.cara("cabeza", lado)
            mechones(c, (x0, y0 + 3, x1, y0 + 8), False)
        if nombre == "patillas con bigote":
            _bigote(c, "grueso")
    elif nombre == "barba completa":
        _barba_base(c)
        _bigote(c, "grueso")
    elif nombre == "bigote de morsa":
        _bigote(c, "grueso")
        for col in range(1, 7):
            cf(c, col, 6, G_SOMBRA if col in (1, 6) else G)
    elif nombre == "perilla imperial":
        _bigote(c, "manillar")
        for col in (3, 4):
            cf(c, col, 7, G)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.punto(x0 + 3, y0, G_SOMBRA)
        c.punto(x0 + 4, y0, G)
    elif nombre == "bigote fino":
        _bigote(c, "fino")
    elif nombre == "barba de candado":
        _bigote(c, "grueso")
        for col in (2, 3, 4, 5):
            cf(c, col, 7, G)
        for col in (1, 6):
            cf(c, col, 6, G_SOMBRA)
    elif nombre == "barba larga":
        _barba_base(c)
        _bigote(c, "grueso")
        _pecho(c, 4)
    elif nombre == "de tres días":
        for col in range(8):
            if col not in (3, 4):
                cf(c, col, 7, G_SOMBRA)
        cf(c, 0, 6, G_SOMBRA)
        cf(c, 7, 6, G_SOMBRA)
    elif nombre == "bigote y perilla":
        _bigote(c, "fino")
        _perilla(c)
    elif nombre == "barba recortada":
        _barba_base(c, 2, G_SOMBRA)
        _bigote(c, "fino")
    elif nombre == "patillas":
        for col in (0, 7):
            for fila in range(3, 7):
                cf(c, col, fila, G_SOMBRA)
        for lado in ("derecha", "izquierda"):
            x0, y0, x1, _ = c.cara("cabeza", lado)
            mechones(c, (x0, y0 + 3, x0 + 3, y0 + 7), False)
    elif nombre == "bigote grueso":
        _bigote(c, "grueso")
    elif nombre == "barba de chivo":
        for col in (2, 3, 4, 5):
            cf(c, col, 7, G)
        _perilla(c, larga=True)
    elif nombre == "barba bifurcada":
        _barba_base(c)
        _bigote(c, "grueso")
        _pecho(c, 4, 6, hueco=True)
    elif nombre == "barba rala":
        for col in range(8):
            for fila in (5, 6, 7):
                if ruido(col, fila, 9) < (0.5 if fila == 7 else 0.25) and not (fila < 7 and 2 <= col <= 5):
                    cf(c, col, fila, G_SOMBRA)
    elif nombre == "barba de marinero":
        for col in range(8):
            cf(c, col, 7, G_SOMBRA)
        for col in (0, 7):
            cf(c, col, 6, G_SOMBRA)
            cf(c, col, 5, G_SOMBRA)
        mechones(c, c.cara("cabeza", "abajo"), False)
    elif nombre == "barba de anciano":
        _barba_base(c, 4)
        _bigote(c, "grueso")
        _pecho(c, 6, 6)
    else:                                                          # bigote caído
        _bigote(c, "caido")
    return c


# ================================================================= COMPLEMENTOS (1..20; 0 = ninguno). No se tiñen.

FIELTRO = (46, 42, 44)
PAJA = (220, 196, 120)


def _copa(c, color, alto=3, cinta=None):
    """Sombrero de copa o bombín: copa en la capa exterior de la cabeza, por encima del pelo."""
    _gorro(c, color, alto, alto - 1 if alto > 2 else 1, fuerza=0.04)
    if cinta:
        _banda(c, alto - 1, cinta)


COMPLEMENTOS = {
    "mujer": ["mantón de Manila", "pañuelo de chulapa", "clavel en el pelo", "sombrero con flores", "sombrerito con velo",
              "peineta", "camafeo", "collar de perlas", "pendientes de oro", "guantes largos", "cofia de sirvienta",
              "chal de lana", "gargantilla de terciopelo", "lazo grande", "broche", "bolso", "gafas redondas",
              "bufanda", "pañuelo a la cabeza", "sombrero de paja"],
    "hombre": ["bombín", "chistera", "boina", "gorra de obrero", "sombrero cordobés", "parpusa", "pajarita",
               "reloj de bolsillo", "monóculo", "gafas redondas", "pañuelo al cuello", "tirantes", "clavel en la solapa",
               "bufanda", "casco de minero", "guantes", "faja roja", "sombrero de paja", "gorra de plato",
               "puro en la boca"],
}


def complemento(sexo, n):
    mujer = sexo == "mujer"
    c = Lienzo(3 if mujer else 4, semilla=4600 + n + (40 if mujer else 0))
    nombre = COMPLEMENTOS[sexo][n - 1]
    fx, fy, _, _ = c.cara("cabeza_capa", "frente")
    if nombre == "mantón de Manila":
        base = (30, 30, 34) if n % 2 else (200, 40, 50)
        _hombros(c, base, 5, 3)
        c.caja("cuerpo_capa", base, filas=(0, 9), solo=("detras",), fuerza=0.04)
        x0, y0, x1, _ = c.cara("cuerpo_capa", "detras")          # flores bordadas y flecos
        for (x, y) in ((1, 2), (5, 3), (3, 5), (6, 6), (2, 7)):
            _flor(c, x0 + x, y0 + y, (230, 120, 140), (250, 220, 120))
        for x in range(x0, x1, 2):
            c.punto(x, y0 + 9, base)
        x0, y0, x1, _ = c.cara("cuerpo_capa", "frente")
        _flor(c, x0 + 1, y0 + 2, (230, 120, 140), (250, 220, 120))
        _flor(c, x0 + 6, y0 + 2, (230, 120, 140), (250, 220, 120))
    elif nombre == "pañuelo de chulapa":
        _gorro(c, BLANCO, 4, 1)
        for cual in ("derecha", "izquierda", "detras"):
            x0, y0, x1, _ = c.cara("cabeza_capa", cual)
            for y in range(y0, y0 + 4):
                for x in range(x0, x1):
                    if (x + y) % 3 == 0:
                        c.punto(x, y, (40, 40, 44))
        x0, y0, _, _ = c.cara("cabeza_capa", "frente")
        c.punto(x0 + 6, y0 + 1, (200, 40, 50))
        c.punto(x0 + 7, y0 + 1, (200, 40, 50))
    elif nombre == "clavel en el pelo":
        _flor(c, fx + 6, fy + 1, (200, 30, 40), (150, 20, 30))
    elif nombre == "clavel en la solapa":
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        _flor(c, x0 + 1, y0 + 2, (200, 30, 40), (150, 20, 30))
    elif nombre == "sombrero con flores":
        _gorro(c, PAJA, 2, 1, detalle=(1, (120, 60, 90)), fuerza=0.1)
        for col, color in ((1, (230, 120, 150)), (3, (250, 230, 120)), (5, (170, 120, 220))):
            c.punto(fx + col, fy, color)
    elif nombre == "sombrerito con velo":
        _gorro(c, NEGRO, 1, 1, fuerza=0.04)
        for col in range(8):
            for fila in range(1, 4):
                if (col + fila) % 2 == 0:
                    c.punto(fx + col, fy + fila, (40, 40, 44))
    elif nombre == "peineta":
        x0, y0, x1, y1 = c.cara("cabeza_capa", "arriba")
        for x in range(x0 + 1, x1 - 1):
            c.punto(x, y1 - 2, (120, 70, 40))
            c.punto(x, y1 - 1, (150, 90, 50))
        x0, y0, _, _ = c.cara("cabeza_capa", "detras")
        for x in range(x0 + 1, x0 + 7):
            c.punto(x, y0, (150, 90, 50))
    elif nombre == "camafeo":
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.rect((x0 + 3, y0 + 1, x0 + 5, y0 + 3), (230, 210, 200))
        c.punto(x0 + 3, y0 + 1, ORO)
        c.punto(x0 + 4, y0 + 2, (180, 120, 100))
    elif nombre == "collar de perlas":
        _collar(c, (240, 236, 228))
    elif nombre == "pendientes de oro":
        for cual in ("derecha", "izquierda"):
            x0, y0, _, _ = c.cara("cabeza_capa", cual)
            c.punto(x0 + 4, y0 + 6, ORO)
            c.punto(x0 + 4, y0 + 7, ORO)
    elif nombre in ("guantes largos", "guantes"):
        color = (236, 230, 220) if mujer else (60, 50, 44)
        for capa in ("brazo_d_capa", "brazo_i_capa"):
            c.caja(capa, color, filas=(5 if mujer else 9, 12), fuerza=0.04)
    elif nombre == "cofia de sirvienta":
        _gorro(c, BLANCO, 1, 1)
        for col in range(8):
            if col % 2 == 0:
                c.punto(fx + col, fy, BLANCO)
    elif nombre == "chal de lana":
        _hombros(c, (130, 110, 100), 4, 3)
    elif nombre == "gargantilla de terciopelo":
        for cual in LADOS:
            x0, y0, x1, _ = c.cara("cuerpo_capa", cual)
            c.rect((x0, y0, x1, y0 + 1), NEGRO)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.punto(x0 + 3, y0, ORO)
    elif nombre == "lazo grande":
        x0, y0, _, _ = c.cara("cabeza_capa", "detras")
        for x, y in ((1, 1), (2, 2), (3, 2), (4, 2), (5, 2), (6, 1), (1, 3), (6, 3), (3, 3), (4, 3)):
            c.punto(x0 + x, y0 + y, (190, 50, 70))
    elif nombre == "broche":
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.punto(x0 + 3, y0, ORO)
        c.punto(x0 + 4, y0, (60, 120, 160))
    elif nombre == "bolso":
        x0, y0, _, _ = c.cara("brazo_i_capa", "frente")
        c.rect((x0, y0 + 9, x0 + 3, y0 + 12), (90, 50, 40))
        c.punto(x0 + 1, y0 + 8, ORO)
    elif nombre == "gafas redondas":
        ojo = 5 if mujer else 4
        for col in (1, 2, 5, 6):
            c.punto(fx + col, fy + ojo, (230, 240, 245))
        for col in (0, 3, 4, 7):
            c.punto(fx + col, fy + ojo, (60, 60, 64))
    elif nombre == "bufanda":
        for cual in LADOS:
            x0, y0, x1, _ = c.cara("cuerpo_capa", cual)
            c.rect((x0, y0, x1, y0 + 2), (150, 40, 40), 0.06)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.rect((x0 + 5, y0 + 2, x0 + 7, y0 + 6), (150, 40, 40), 0.06)
        c.fila("cuerpo_capa", 1, (200, 180, 150), solo=("frente",))
    elif nombre == "pañuelo a la cabeza":
        _gorro(c, (60, 80, 140), 4, 2)
    elif nombre == "sombrero de paja":
        _gorro(c, PAJA, 2, 1, detalle=(1, NEGRO), fuerza=0.1)
    elif nombre == "bombín":
        _copa(c, FIELTRO, 2, (30, 28, 30))
    elif nombre == "chistera":
        _copa(c, (24, 22, 26), 3, (60, 20, 30))
    elif nombre == "boina":
        _gorro(c, (30, 34, 50), 2, 1, fuerza=0.04)
        x0, y0, x1, y1 = c.cara("cabeza_capa", "arriba")
        c.punto(x0 + 4, y0 + 4, (20, 20, 30))
        x0, y0, _, _ = c.cara("cabeza_capa", "izquierda")
        c.rect((x0 + 2, y0 + 2, x0 + 7, y0 + 3), (30, 34, 50))
    elif nombre == "gorra de obrero":
        _gorro(c, (90, 86, 80), 2, 1, fuerza=0.08)
        c.rect((fx + 1, fy + 2, fx + 7, fy + 3), (60, 58, 54))
    elif nombre == "parpusa":
        _gorro(c, (50, 50, 54), 2, 1, fuerza=0.04)
        for col in range(8):
            if col % 2:
                c.punto(fx + col, fy, (230, 230, 230))
        c.rect((fx + 1, fy + 2, fx + 7, fy + 3), (30, 30, 34))
    elif nombre == "sombrero cordobés":
        _gorro(c, (30, 28, 32), 2, 1, detalle=(1, (60, 56, 60)), fuerza=0.04)
        _banda(c, 2, (30, 28, 32))
    elif nombre == "gorra de plato":
        _gorro(c, (36, 50, 90), 2, 1, detalle=(1, ORO), fuerza=0.04)
        c.rect((fx + 1, fy + 2, fx + 7, fy + 3), NEGRO)
        c.punto(fx + 4, fy, ORO)
    elif nombre == "casco de minero":
        _gorro(c, (110, 100, 70), 2, 1, fuerza=0.06)
        c.rect((fx + 3, fy, fx + 5, fy + 2), (250, 230, 140))
        c.rect((fx + 3, fy + 2, fx + 5, fy + 3), (90, 80, 60))
    elif nombre == "pajarita":
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for col in (2, 3, 4, 5):
            c.punto(x0 + col, y0, (30, 30, 34) if col in (2, 5) else (20, 20, 24))
        c.punto(x0 + 2, y0 + 1, (30, 30, 34))
        c.punto(x0 + 5, y0 + 1, (30, 30, 34))
    elif nombre == "reloj de bolsillo":
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for i, col in enumerate((2, 3, 4, 5)):
            c.punto(x0 + col, y0 + 5 + (1 if col in (3, 4) else 0), ORO)
        c.punto(x0 + 5, y0 + 4, PLATA)
    elif nombre == "monóculo":
        c.punto(fx + 5, fy + 4, (230, 240, 245))
        c.punto(fx + 6, fy + 4, (230, 240, 245))
        c.punto(fx + 4, fy + 4, ORO)
        c.punto(fx + 7, fy + 4, ORO)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for fila in range(3):
            c.punto(x0 + 6, y0 + fila, ORO)
    elif nombre == "pañuelo al cuello":
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for cual in LADOS:
            x0, y0, x1, _ = c.cara("cuerpo_capa", cual)
            c.rect((x0, y0, x1, y0 + 1), (180, 40, 50))
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.rect((x0 + 3, y0 + 1, x0 + 5, y0 + 3), (180, 40, 50))
    elif nombre == "tirantes":
        for cual, cols in (("frente", (1, 6)), ("detras", (2, 5))):
            x0, y0, _, _ = c.cara("cuerpo_capa", cual)
            for col in cols:
                for fila in range(0, 11):
                    c.punto(x0 + col, y0 + fila, (120, 40, 40))
    elif nombre == "faja roja":
        c.caja("cuerpo_capa", (170, 40, 40), filas=(9, 11), fuerza=0.05)
    elif nombre == "puro en la boca":
        c.punto(fx + 5, fy + 6, (110, 70, 40))
        c.punto(fx + 6, fy + 6, (110, 70, 40))
        c.punto(fx + 7, fy + 6, (230, 120, 60))
    return c


# ================================================================= RASGOS DE CARA (1..20; 0 = ninguno)

MARCAS = ["hollín", "carbonilla", "polvos de arroz", "colorete", "pecas", "lunar", "cicatriz", "ojeras",
          "nariz roja", "quemado por el sol", "arrugas", "verruga", "ojo morado", "labios pintados",
          "marcas de viruela", "grasa de máquina", "mejillas sonrosadas", "cicatriz en la ceja", "herida reciente",
          "lunar junto a la boca"]

CICATRIZ, LUNAR, OJERA, ROJO_C = (236, 196, 186), (50, 34, 30), (120, 92, 110), (214, 92, 92)


def marca(sexo, n):
    mujer = sexo == "mujer"
    c = Lienzo(3 if mujer else 4, semilla=4700 + n)
    nombre = MARCAS[n - 1]
    ojo = 5 if mujer else 4
    mejilla = 7 if mujer else 5
    if nombre == "hollín":
        for x, y in ((0, mejilla), (1, mejilla), (6, mejilla - 1), (7, 3), (2, 2), (5, 1)):
            cf(c, x, y, (64, 60, 58))
    elif nombre == "carbonilla":
        for x in range(8):
            for y in range(1, 8):
                if ruido(x, y, 43) < 0.2 and not (y in (ojo, ojo + (1 if mujer else 0)) and x in (1, 2, 5, 6)):
                    cf(c, x, y, (50, 48, 48))
    elif nombre == "polvos de arroz":
        for x in range(8):
            for y in range(1, 8):
                if not (y in (ojo, ojo + (1 if mujer else 0)) and x in (1, 2, 5, 6)) and ruido(x, y, 5) < 0.4:
                    cf(c, x, y, (246, 238, 232))
    elif nombre == "colorete":
        for x in ((1, 2, 5, 6) if mujer else (0, 1, 6, 7)):
            cf(c, x, mejilla, ROJO_C)
    elif nombre == "pecas":
        for x, y in ([(1, 7), (2, 7), (5, 7), (6, 7)] if mujer else [(1, 5), (2, 6), (5, 6), (6, 5)]):
            cf(c, x, y, (156, 98, 66))
    elif nombre == "lunar":
        cf(c, 6 if not mujer else 7, mejilla, LUNAR)
    elif nombre == "cicatriz":
        for y in (mejilla - 1, mejilla):
            cf(c, 6, y, CICATRIZ)
        cf(c, 7, mejilla - 2, CICATRIZ)
    elif nombre == "ojeras":
        for x in (1, 2, 5, 6):
            cf(c, x, ojo + (2 if mujer else 1), OJERA)
    elif nombre == "nariz roja":
        cf(c, 3, ojo + (2 if mujer else 1), ROJO_C)
        cf(c, 4, ojo + (2 if mujer else 1), ROJO_C)
    elif nombre == "quemado por el sol":
        for x in range(1, 7):
            cf(c, x, ojo + (2 if mujer else 1), (226, 120, 100) if x in (3, 4) else (220, 140, 120))
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
            cf(c, 3, 7, (180, 40, 60))
            cf(c, 4, 7, (180, 40, 60))
        else:
            cf(c, 3, 6, (170, 60, 70))
            cf(c, 4, 6, (170, 60, 70))
    elif nombre == "marcas de viruela":
        for x, y in ((1, mejilla), (6, mejilla), (2, 2), (5, 3), (7, mejilla - 1), (0, mejilla - 1)):
            cf(c, x, y, (176, 140, 120))
    elif nombre == "grasa de máquina":
        for x, y in ((5, 1), (6, 2), (6, 1), (0, mejilla), (1, mejilla + (0 if mujer else 1))):
            cf(c, x, min(7, y), (40, 36, 30))
    elif nombre == "mejillas sonrosadas":
        for x in (0, 1, 6, 7):
            cf(c, x, mejilla, mezclar((240, 180, 170), (230, 110, 110), 0.4))
    elif nombre == "cicatriz en la ceja":
        for y in range(ojo - 2, ojo):
            cf(c, 5, y, CICATRIZ)
    elif nombre == "herida reciente":
        for x, y in ((5, 2), (6, 3), (7, 4)):
            cf(c, x, y, (180, 40, 40))
    else:                                                         # lunar junto a la boca
        cf(c, 5, 7 if mujer else 6, LUNAR)
    return c


NOMBRES_MARCA = {"hombre": MARCAS, "mujer": MARCAS}
