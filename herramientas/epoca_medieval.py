"""Época MEDIEVAL: 20 ropas, 20 peinados, 20 barbas, 20 complementos y 20 rasgos de cara por sexo.
Usa las herramientas de dibujar_habitantes (Lienzo, cf, mechones, flequillo…). Cada lista lleva su nombre (para el
libro del creador) y los peinados sus piezas 3D (ver client/habitantes/CapaPelo3D)."""
from dibujar_habitantes import (Lienzo, cf, oscuro, mezclar, ruido, mechones, flequillo, flequillo_profundo, volumen,
                                cabeza, espalda, cejas, CARA_X, CARA_Y, CAPA_X, CAPA_Y, BRAZOS, PIERNAS,
                                LADOS, G_BRILLO, G_LUZ, G, G_SOMBRA, G_OSCURO, G_HONDO, COLETA, MONO, TRENZAS,
                                FLEQUILLO, MELENA, TRENZA_ATRAS, zapatos, punos, CUERO, LINO, ORO, TELA, CAPAS_PIERNAS)

ID = "medieval"

# ================================================================= ROPA: 5 cortes x 4 colores

COLORES = {
    "hombre": [
        [(120, 86, 56), (98, 112, 60), (78, 96, 128), (138, 60, 52)],      # túnica con cinturón
        [(84, 70, 112), (60, 92, 82), (150, 124, 92), (112, 52, 48)],      # túnica larga bordada
        [(70, 70, 76), (110, 40, 40), (52, 74, 110), (96, 82, 46)],        # jubón con camisa
        [(128, 112, 86), (110, 98, 78), (96, 90, 74), (134, 118, 96)],     # sayo de campesino
        [(72, 96, 60), (110, 74, 44), (60, 74, 92), (92, 64, 52)],         # cazador con chaleco de cuero
    ],
    "mujer": [
        [(120, 86, 56), (98, 112, 60), (78, 96, 128), (138, 60, 52)],      # vestido con delantal
        [(150, 54, 60), (58, 88, 120), (90, 110, 66), (110, 80, 120)],     # corpiño con blusa
        [(66, 96, 118), (132, 70, 92), (150, 130, 96), (74, 100, 74)],     # vestido largo con cinturón
        [(128, 112, 86), (112, 90, 70), (96, 104, 84), (140, 116, 92)],    # saya de campesina con pañoleta
        [(118, 30, 44), (34, 52, 104), (40, 84, 60), (96, 40, 96)],        # vestido noble bicolor
    ],
}
NOMBRES_ROPA = {
    "hombre": ["túnica con cinturón", "túnica larga bordada", "jubón con camisa", "sayo de campesino",
               "cazador con chaleco"],
    "mujer": ["vestido con delantal", "corpiño con blusa", "vestido largo con cinturón", "saya con pañoleta",
              "vestido noble"],
}
NOMBRES_COLOR = {
    "hombre": [["pardo", "verde", "azul", "rojo"], ["morado", "verde mar", "ocre", "granate"],
               ["gris", "rojo", "azul", "mostaza"], ["crudo", "terroso", "gris", "arena"],
               ["verde", "cuero", "azul", "marrón"]],
    "mujer": [["pardo", "verde", "azul", "rojo"], ["rojo", "azul", "verde", "lila"], ["azul", "rosa", "ocre", "verde"],
              ["crudo", "terroso", "verde gris", "arena"], ["carmesí", "azul noche", "verde", "púrpura"]],
}


def ropa(sexo, n):
    mujer = sexo == "mujer"
    corte, color = divmod(n, 4)
    tela = COLORES[sexo][corte][color]
    c = Lienzo(3 if mujer else 4, semilla=200 + n + (40 if mujer else 0))
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
    elif not mujer and corte == 2:      # jubón abotonado, camisa blanca, calzas y botas altas
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
    elif not mujer and corte == 3:      # sayo de campesino: tela basta con remiendos, cuerda de cinturón, mangas cortas
        c.caja("cuerpo", tela, fuerza=0.12)
        for brazo in BRAZOS:
            c.caja(brazo, tela, filas=(0, 5), fuerza=0.12)
            c.fila(brazo, 4, oscuro(tela, 0.8))
        for pierna in PIERNAS:
            c.caja(pierna, oscuro(tela, 0.72), fuerza=0.1)
        c.rect((fx + 1, fy + 3, fx + 3, fy + 5), mezclar(tela, (90, 70, 50), 0.4))       # remiendo
        c.rect((fx + 5, fy + 9, fx + 7, fy + 11), mezclar(tela, (150, 140, 110), 0.4))   # otro
        c.fila("cuerpo", 8, (176, 150, 100), solo=None)                                   # cuerda
        c.punto(fx + 2, fy + 9, (176, 150, 100))
        for fila in range(2):
            c.punto(fx + 3 + fila, fy, oscuro(tela, 0.7))
        zapatos(c)
    elif not mujer:                     # cazador: camisa de color, chaleco de cuero, cinturón y botas altas
        c.caja("cuerpo", tela, fuerza=TELA)
        for brazo in BRAZOS:
            c.caja(brazo, tela, filas=(0, 11), fuerza=TELA)
        punos(c, oscuro(CUERO, 0.9), 10)
        c.caja("cuerpo", CUERO, filas=(0, 10), solo=("derecha", "izquierda", "detras"), fuerza=0.08)
        c.rect((fx, fy, fx + 3, fy + 10), CUERO, 0.08)
        c.rect((fx + 5, fy, fx + 8, fy + 10), CUERO, 0.08)
        c.fila("cuerpo", 10, oscuro(CUERO, 0.7), solo=None)
        c.punto(fx + 4, fy + 10, ORO)
        for pierna in PIERNAS:
            c.caja(pierna, oscuro(tela, 0.55), fuerza=TELA)
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
    elif corte == 2:                    # vestido largo con cinturón y bordado dorado
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
    elif corte == 3:                    # saya de campesina: mangas remangadas, pañoleta cruzada y falda con remiendo
        c.caja("cuerpo", tela, fuerza=0.11)
        for brazo in BRAZOS:
            c.caja(brazo, LINO, filas=(0, 6), fuerza=0.05)
            c.fila(brazo, 5, oscuro(LINO, 0.84))
        panuelo = mezclar(tela, (240, 230, 210), 0.65)
        for fila in range(4):
            for col in range(8):
                if abs(col - 3.5) >= fila + 0.5:
                    c.punto(fx + col, fy + fila, panuelo)
        c.caja("cuerpo", panuelo, filas=(0, 2), solo=("derecha", "izquierda", "detras"))
        for pierna in PIERNAS:
            c.caja(pierna, oscuro(tela, 0.9), fuerza=0.1)
        for capa in CAPAS_PIERNAS:
            c.caja(capa, oscuro(tela, 0.86), filas=(0, 10), solo=LADOS, fuerza=0.1)
        px0, py0, _, _ = c.cara("pierna_d_capa", "frente")
        c.rect((px0 + 1, py0 + 4, px0 + 3, py0 + 6), mezclar(tela, (80, 60, 40), 0.4))
        c.fila("cuerpo", 8, oscuro(tela, 0.7))
        zapatos(c)
    else:                               # vestido noble: dos colores, mangas largas, cinturón dorado bajo
        claro = mezclar(tela, (240, 226, 200), 0.55)
        c.caja("cuerpo", tela, fuerza=0.05)
        c.rect((fx + 2, fy + 1, fx + 6, fy + 8), claro, 0.04)
        c.fila("cuerpo", 0, ORO)
        for brazo in BRAZOS:
            c.caja(brazo, tela, fuerza=0.05)
            c.fila(brazo, 11, ORO)
        c.fila("cuerpo", 9, ORO, solo=None)
        for pierna in PIERNAS:
            c.caja(pierna, tela, fuerza=0.05)
        for capa in CAPAS_PIERNAS:
            c.caja(capa, oscuro(tela, 0.92), filas=(0, 12), solo=LADOS, fuerza=0.05)
            c.fila(capa, 11, ORO)
        zapatos(c)
    return c


def nombre_ropa(sexo, n):
    corte, color = divmod(n, 4)
    return NOMBRES_ROPA[sexo][corte] + " " + NOMBRES_COLOR[sexo][corte][color]


# ================================================================= PEINADOS
# (nombre, lados, nuca, flequillo, (filas de espalda, lados de espalda), piezas 3D, solo adultos, especial)
PEINADOS = {
    "mujer": [
        ("larga con flequillo", 8, 8, [8, 4, 4, 3, 4, 3, 4, 8], (6, 2), FLEQUILLO | MELENA, False, None),
        ("larga con raya al lado", 8, 8, [8, 4, 4, 3, 2, 2, 3, 7], (5, 1), MELENA, False, None),
        ("trenza", 6, 8, [6, 3, 4, 2, 3, 4, 3, 6], (0, 0), TRENZA_ATRAS, False, None),
        ("moño", 5, 8, [5, 3, 2, 2, 1, 2, 3, 5], (0, 0), MONO, False, None),
        ("coleta", 6, 8, [6, 4, 3, 2, 3, 3, 4, 6], (0, 0), COLETA, False, None),
        ("media melena con flequillo", 7, 8, [8, 4, 4, 4, 4, 4, 4, 8], (0, 0), FLEQUILLO, False, None),
        ("dos trenzas", 6, 8, [7, 3, 4, 3, 3, 4, 3, 7], (0, 0), TRENZAS, False, None),
        ("corta con flequillo", 5, 7, [7, 4, 3, 4, 3, 4, 3, 7], (0, 0), FLEQUILLO, False, None),
        ("larga con raya al medio", 8, 8, [8, 3, 2, 1, 1, 2, 3, 8], (7, 2), MELENA, False, None),
        ("moño con flequillo", 5, 8, [6, 4, 3, 4, 4, 3, 4, 6], (0, 0), MONO, False, None),
        ("coleta alta con flequillo", 5, 8, [6, 4, 4, 3, 3, 4, 4, 6], (0, 0), COLETA | FLEQUILLO, False, None),
        ("dos trenzas con flequillo", 6, 8, [7, 4, 4, 4, 4, 4, 4, 7], (0, 0), TRENZAS | FLEQUILLO, False, None),
        ("trenza con raya", 6, 8, [6, 3, 2, 1, 2, 3, 4, 6], (0, 0), TRENZA_ATRAS, False, None),
        ("melena ondulada", 8, 8, [8, 4, 3, 4, 3, 4, 3, 8], (6, 2), MELENA, False, "rizado"),
        ("media melena sin flequillo", 7, 8, [7, 3, 2, 2, 1, 2, 3, 7], (0, 0), 0, False, None),
        ("recogido con mechones", 7, 8, [8, 3, 3, 2, 2, 3, 3, 8], (0, 0), MONO, False, None),
        ("flequillo de lado", 7, 8, [8, 4, 4, 4, 3, 2, 2, 7], (4, 1), MELENA, False, None),
        ("coleta baja", 6, 8, [6, 3, 3, 2, 2, 3, 3, 6], (0, 0), TRENZA_ATRAS, False, None),
        ("larguísima", 8, 8, [8, 4, 3, 3, 3, 3, 4, 8], (12, 4), MELENA, False, None),
        ("corta despeinada", 4, 6, [6, 4, 2, 4, 3, 2, 4, 6], (0, 0), FLEQUILLO, False, "rizado"),
    ],
    "hombre": [
        ("corto con flequillo", 3, 7, [2, 2, 1, 2, 2, 1, 2, 2], (0, 0), 0, False, None),
        ("rapado", 4, 7, [1] * 8, (0, 0), 0, False, "rapado"),
        ("melena", 8, 8, [4, 2, 2, 1, 2, 2, 2, 4], (2, 0), MELENA, False, None),
        ("tonsura", 5, 7, [1] * 8, (0, 0), 0, True, "tonsura"),
        ("raya al lado", 3, 7, [3, 3, 2, 2, 1, 1, 1, 2], (0, 0), 0, False, None),
        ("tazón", 5, 7, [3] * 8, (0, 0), FLEQUILLO, False, None),
        ("rizado", 5, 7, [2, 3, 2, 3, 2, 3, 2, 3], (0, 0), FLEQUILLO, False, "rizado"),
        ("flequillo despeinado", 4, 7, [3, 2, 3, 1, 3, 2, 3, 2], (0, 0), FLEQUILLO, False, None),
        ("calvo con corona", 4, 6, [0] * 8, (0, 0), 0, True, "calvo"),
        ("coleta corta", 4, 8, [2, 2, 1, 1, 1, 1, 2, 2], (0, 0), COLETA, False, None),
        ("melena con raya al medio", 8, 8, [5, 2, 1, 0, 0, 1, 2, 5], (3, 0), MELENA, False, None),
        ("peinado hacia atrás", 3, 8, [1, 1, 0, 0, 0, 0, 1, 1], (0, 0), 0, False, None),
        ("pelo largo atado", 6, 8, [3, 2, 1, 1, 1, 1, 2, 3], (0, 0), TRENZA_ATRAS, False, None),
        ("rizado corto", 3, 6, [2, 2, 1, 2, 2, 1, 2, 2], (0, 0), 0, False, "rizado"),
        ("con entradas", 3, 7, [0, 1, 0, 0, 0, 0, 1, 0], (0, 0), 0, True, "entradas"),
        ("flequillo largo de lado", 4, 7, [4, 4, 3, 3, 2, 2, 1, 2], (0, 0), FLEQUILLO, False, None),
        ("tazón largo", 7, 8, [3] * 8, (1, 0), FLEQUILLO | MELENA, False, None),
        ("trenzas de guerrero", 6, 8, [2, 2, 1, 1, 1, 1, 2, 2], (0, 0), TRENZAS, False, None),
        ("moño de guerrero", 4, 8, [1, 1, 0, 0, 0, 0, 1, 1], (0, 0), MONO, False, None),
        ("desgreñado largo", 8, 8, [4, 3, 2, 3, 2, 3, 2, 4], (3, 1), MELENA, False, "rizado"),
    ],
}


def pelo(sexo, n):
    mujer = sexo == "mujer"
    nombre, lados, nuca, fleq, (esp, esp_lados), _, _, especial = PEINADOS[sexo][n]
    c = Lienzo(3 if mujer else 4, semilla=100 + n * 3 + (50 if mujer else 0))
    if not mujer:
        cejas(c, False, 3)
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
        if especial == "tonsura":
            x0, y0, x1, y1 = c.cara("cabeza", "arriba")
            for y in range(y0 + 2, y1 - 2):
                for x in range(x0 + 2, x1 - 2):
                    c.borrar(x, y)
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
    if especial not in ("rapado", "calvo", "tonsura"):
        volumen(c, 0.3 if mujer else 0.45)
    return c


# ================================================================= BARBAS (1..20; 0 = ninguna)

def _barba_base(c, filas_lados=3, tono=G):
    for col in range(8):
        cf(c, col, 7, tono if col % 3 else G_SOMBRA)
    for col in (0, 1, 6, 7):
        cf(c, col, 6, tono)
        cf(c, col, 5, G_SOMBRA)
    for lado in ("derecha", "izquierda"):
        x0, y0, x1, _ = c.cara("cabeza", lado)
        mechones(c, (x0, y0 + 8 - filas_lados, x1, y0 + 8), False)
    mechones(c, c.cara("cabeza", "abajo"), False)


def _bigote(c, tipo):
    if tipo == "fino":
        for col in (2, 3, 4, 5):
            cf(c, col, 5, G_SOMBRA)
    elif tipo == "grueso":
        for col in range(1, 7):
            cf(c, col, 5, G if col in (1, 6) else G_SOMBRA)
        cf(c, 2, 6, G)
        cf(c, 5, 6, G)
    elif tipo == "caido":
        for col in (2, 3, 4, 5):
            cf(c, col, 5, G_SOMBRA)
        for col in (1, 6):
            cf(c, col, 6, G)
            cf(c, col, 7, G_SOMBRA)
    elif tipo == "manillar":
        for col in (2, 3, 4, 5):
            cf(c, col, 5, G_SOMBRA)
        cf(c, 1, 5, G)
        cf(c, 6, 5, G)
        cf(c, 0, 4, G_LUZ)
        cf(c, 7, 4, G_LUZ)


def _perilla(c, larga=False):
    for col in (3, 4):
        cf(c, col, 7, G)
    cf(c, 2, 7, G_SOMBRA)
    cf(c, 5, 7, G_SOMBRA)
    if larga:
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for fila in range(2):
            c.punto(x0 + 3, y0 + fila, G_SOMBRA)
            c.punto(x0 + 4, y0 + fila, G)


def _pecho(c, filas, ancho=4, hueco=False):
    x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
    izq = 4 - ancho // 2
    for fila in range(filas):
        for col in range(izq, izq + ancho):
            if hueco and col in (3, 4) and fila >= filas - 2:
                continue
            c.punto(x0 + col, y0 + fila, G if (col + fila) % 3 else G_SOMBRA)


BARBAS = [
    "de tres días", "bigote fino", "bigote grueso", "bigote caído", "perilla", "bigote y perilla", "barba corta",
    "barba corta con bigote", "barba completa", "barba larga", "barba muy larga", "patillas", "patillas con bigote",
    "barba de collar", "collar con bigote", "barba bifurcada", "perilla larga", "bigote de manillar",
    "barba trenzada", "barba rala",
]


def barba(n):
    c = Lienzo(semilla=400 + n)
    nombre = BARBAS[n - 1]
    if nombre == "de tres días":
        for col in range(8):
            if col not in (3, 4):
                cf(c, col, 7, G_SOMBRA)
        cf(c, 0, 6, G_SOMBRA)
        cf(c, 7, 6, G_SOMBRA)
    elif nombre == "bigote fino":
        _bigote(c, "fino")
    elif nombre == "bigote grueso":
        _bigote(c, "grueso")
    elif nombre == "bigote caído":
        _bigote(c, "caido")
    elif nombre == "perilla":
        _perilla(c)
    elif nombre == "bigote y perilla":
        _bigote(c, "fino")
        _perilla(c)
    elif nombre == "barba corta":
        _barba_base(c, 2, G_SOMBRA)
    elif nombre == "barba corta con bigote":
        _barba_base(c, 2, G_SOMBRA)
        _bigote(c, "fino")
    elif nombre == "barba completa":
        _barba_base(c)
        _bigote(c, "grueso")
    elif nombre == "barba larga":
        _barba_base(c)
        _bigote(c, "grueso")
        _pecho(c, 3)
    elif nombre == "barba muy larga":
        _barba_base(c, 4)
        _bigote(c, "grueso")
        _pecho(c, 6, 6)
    elif nombre == "patillas":
        for col in (0, 7):
            for fila in range(3, 7):
                cf(c, col, fila, G_SOMBRA)
        for lado in ("derecha", "izquierda"):
            x0, y0, x1, _ = c.cara("cabeza", lado)
            mechones(c, (x0, y0 + 3, x0 + 3, y0 + 7), False)
    elif nombre == "patillas con bigote":
        for col in (0, 7):
            for fila in range(3, 7):
                cf(c, col, fila, G_SOMBRA)
        _bigote(c, "grueso")
    elif nombre == "barba de collar":
        for col in range(8):
            cf(c, col, 7, G_SOMBRA)
        for col in (0, 7):
            cf(c, col, 6, G_SOMBRA)
            cf(c, col, 5, G_SOMBRA)
        mechones(c, c.cara("cabeza", "abajo"), False)
    elif nombre == "collar con bigote":
        for col in range(8):
            cf(c, col, 7, G_SOMBRA)
        for col in (0, 7):
            cf(c, col, 6, G_SOMBRA)
        _bigote(c, "fino")
    elif nombre == "barba bifurcada":
        _barba_base(c)
        _bigote(c, "grueso")
        _pecho(c, 4, 6, hueco=True)
    elif nombre == "perilla larga":
        _bigote(c, "fino")
        _perilla(c, larga=True)
    elif nombre == "bigote de manillar":
        _bigote(c, "manillar")
    elif nombre == "barba trenzada":
        _barba_base(c)
        _bigote(c, "caido")
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for fila in range(5):
            c.punto(x0 + 3, y0 + fila, G_SOMBRA if fila % 2 else G_LUZ)
            c.punto(x0 + 4, y0 + fila, G_LUZ if fila % 2 else G_SOMBRA)
    else:                                                          # rala: pelos sueltos
        for col in range(8):
            for fila in (5, 6, 7):
                if ruido(col, fila, 9) < (0.5 if fila == 7 else 0.25) and not (fila < 7 and 2 <= col <= 5):
                    cf(c, col, fila, G_SOMBRA)
    return c


# ================================================================= COMPLEMENTOS (1..20; 0 = ninguno). No se tiñen.

def _gorro(c, color, filas, frente=None, detalle=None, fuerza=0.06):
    """Gorro en la capa exterior de la cabeza: arriba entero y 'filas' por los lados y detrás; por delante, 'frente'
    filas (por defecto, las mismas)."""
    frente = filas if frente is None else frente
    x0, y0, x1, y1 = c.cara("cabeza_capa", "arriba")
    c.rect((x0, y0, x1, y1), color, fuerza)
    for cual in ("derecha", "izquierda", "detras"):
        x0, y0, x1, _ = c.cara("cabeza_capa", cual)
        c.rect((x0, y0, x1, y0 + filas), color, fuerza)
    x0, y0, x1, _ = c.cara("cabeza_capa", "frente")
    c.rect((x0, y0, x1, y0 + frente), color, fuerza)
    if detalle:
        for cual in ("derecha", "izquierda", "detras", "frente"):
            x0, y0, x1, _ = c.cara("cabeza_capa", cual)
            c.rect((x0, y0 + detalle[0], x1, y0 + detalle[0] + 1), detalle[1])


def _banda(c, fila, color, frente=True):
    for cual in ("derecha", "izquierda", "detras") + (("frente",) if frente else ()):
        x0, y0, x1, _ = c.cara("cabeza_capa", cual)
        c.rect((x0, y0 + fila, x1, y0 + fila + 1), color)


def _hombros(c, color, filas=3, brazos=2):
    c.caja("cuerpo_capa", color, filas=(0, filas), fuerza=0.06)
    for capa in ("brazo_d_capa", "brazo_i_capa"):
        c.caja(capa, color, filas=(0, brazos), fuerza=0.06)


def _collar(c, color, colgante=None, filas=1):
    x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
    for col in range(1, 7):
        if (col in (1, 6) and filas == 1) or col in (2, 5) or filas > 1:
            c.punto(x0 + col, y0 + (0 if col in (1, 6) else min(filas, 1)), color)
    if colgante:
        c.punto(x0 + 3, y0 + 2, colgante)
        c.punto(x0 + 4, y0 + 2, colgante)
        c.punto(x0 + 3, y0 + 3, oscuro(colgante, 0.8))


def _cinturon_bolsa(c, cinto, bolsa):
    c.fila("cuerpo_capa", 8, cinto, solo=None)
    x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
    c.rect((x0 + 5, y0 + 9, x0 + 7, y0 + 11), bolsa)


def _flor(c, x, y, petalo, centro):
    for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1)):
        c.punto(x + dx, y + dy, petalo)
    c.punto(x, y, centro)


BLANCO_LINO = (238, 234, 222)
PAJA = (214, 186, 110)
FIELTRO = (86, 70, 56)
LANA = (150, 54, 48)

COMPLEMENTOS = {
    "mujer": ["cofia blanca", "toca con barbuquejo", "velo", "diadema dorada", "corona de flores", "pañuelo atado",
              "cinta roja", "cinta azul", "gorro de lana", "sombrero de paja", "capucha", "flor en el pelo",
              "collar de cuentas", "colgante", "chal", "cinturón con bolsa", "pendientes de oro", "guantes",
              "cofia con lazo", "redecilla dorada"],
    "hombre": ["capucha", "gorro de fieltro", "sombrero de paja", "sombrero con pluma", "cofia de lino",
               "gorro de lana", "cinta en la frente", "pañuelo", "chaperón", "casquete de cuero", "bolsa al cinto",
               "cuerno de caza", "cruz de madera", "capa corta", "zurrón", "guantes de cuero", "brazales de cuero",
               "cinturón ancho", "aro en la oreja", "gorro frigio"],
}


def complemento(sexo, n):
    mujer = sexo == "mujer"
    c = Lienzo(3 if mujer else 4, semilla=600 + n + (40 if mujer else 0))
    nombre = COMPLEMENTOS[sexo][n - 1]
    fx, fy, _, _ = c.cara("cabeza_capa", "frente")
    if nombre in ("cofia blanca", "cofia de lino"):
        _gorro(c, BLANCO_LINO, 5, 1)
    elif nombre == "toca con barbuquejo":
        _gorro(c, BLANCO_LINO, 8, 1)
        c.rect((fx, fy + 7, fx + 8, fy + 8), BLANCO_LINO)
    elif nombre == "velo":
        _gorro(c, (120, 96, 140), 8, 1)
        c.caja("cuerpo_capa", (120, 96, 140), filas=(0, 4), solo=("detras",), fuerza=0.06)
    elif nombre == "diadema dorada":
        _banda(c, 1, ORO)
        c.punto(fx + 4, fy + 1, (200, 60, 60))
    elif nombre == "corona de flores":
        _banda(c, 1, (80, 130, 60))
        for i, col in enumerate((0, 2, 4, 6)):
            c.punto(fx + col + 1, fy + 1, [(230, 90, 120), (250, 230, 120), (170, 120, 220), (250, 250, 250)][i])
    elif nombre == "pañuelo atado":
        _gorro(c, (180, 60, 50), 4, 2)
        x0, y0, _, _ = c.cara("cabeza_capa", "detras")
        c.rect((x0 + 3, y0 + 4, x0 + 5, y0 + 6), (150, 46, 40))
    elif nombre in ("cinta roja", "cinta azul", "cinta en la frente"):
        color = (196, 50, 56) if nombre == "cinta roja" else (60, 90, 170) if nombre == "cinta azul" else CUERO
        _banda(c, 1 if mujer else 2, color)
        if mujer:
            x0, y0, _, _ = c.cara("cabeza_capa", "izquierda")
            c.rect((x0 + 5, y0, x0 + 7, y0 + 2), color)
    elif nombre == "gorro de lana":
        _gorro(c, LANA, 3, 2, detalle=(2, oscuro(LANA, 0.75)))
    elif nombre in ("sombrero de paja",):
        _gorro(c, PAJA, 2, 1, detalle=(1, (150, 60, 50)), fuerza=0.12)
    elif nombre == "capucha":
        color = (92, 76, 60) if not mujer else (120, 60, 56)
        _gorro(c, color, 8, 1)
        for cual in ("derecha", "izquierda"):
            x0, y0, x1, _ = c.cara("cabeza_capa", cual)
            for y in range(y0 + 1, y0 + 8):
                c.punto(x0 if cual == "derecha" else x1 - 1, y, oscuro(color, 0.8))
        _hombros(c, color, 2, 1)
    elif nombre == "flor en el pelo":
        _flor(c, fx + 6, fy + 1, (240, 120, 150), (250, 230, 120))
    elif nombre == "collar de cuentas":
        _collar(c, (200, 70, 70))
    elif nombre in ("colgante", "cruz de madera"):
        color = ORO if nombre == "colgante" else (120, 84, 50)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for col in (2, 5):
            c.punto(x0 + col, y0, (80, 60, 40))
        c.punto(x0 + 3, y0 + 1, (80, 60, 40))
        c.punto(x0 + 4, y0 + 1, (80, 60, 40))
        c.punto(x0 + 3, y0 + 2, color)
        if nombre == "cruz de madera":
            c.punto(x0 + 2, y0 + 3, color)
            c.punto(x0 + 4, y0 + 3, color)
            c.punto(x0 + 3, y0 + 4, color)
        c.punto(x0 + 3, y0 + 3, color)
    elif nombre == "chal":
        _hombros(c, (170, 140, 100), 4, 3)
    elif nombre in ("cinturón con bolsa", "bolsa al cinto"):
        _cinturon_bolsa(c, CUERO, oscuro(CUERO, 0.8))
    elif nombre == "pendientes de oro":
        for cual in ("derecha", "izquierda"):
            x0, y0, x1, _ = c.cara("cabeza_capa", cual)
            c.punto(x0 + 4, y0 + 7, ORO)
    elif nombre == "aro en la oreja":
        x0, y0, _, _ = c.cara("cabeza_capa", "izquierda")
        c.punto(x0 + 4, y0 + 6, ORO)
    elif nombre in ("guantes", "guantes de cuero"):
        color = (236, 230, 220) if nombre == "guantes" else CUERO
        for capa in ("brazo_d_capa", "brazo_i_capa"):
            c.caja(capa, color, filas=(9, 12), fuerza=0.05)
    elif nombre == "cofia con lazo":
        _gorro(c, BLANCO_LINO, 5, 1)
        x0, y0, _, _ = c.cara("cabeza_capa", "detras")
        c.rect((x0 + 2, y0 + 5, x0 + 6, y0 + 6), (220, 90, 110))
    elif nombre == "redecilla dorada":
        x0, y0, x1, y1 = c.cara("cabeza_capa", "detras")
        for y in range(y0 + 1, y0 + 7):
            for x in range(x0, x1):
                if (x + y) % 2 == 0:
                    c.punto(x, y, ORO)
        _banda(c, 0, ORO, frente=False)
    elif nombre == "gorro de fieltro":
        _gorro(c, FIELTRO, 3, 2, detalle=(2, oscuro(FIELTRO, 0.7)))
    elif nombre == "sombrero con pluma":
        _gorro(c, (60, 80, 60), 2, 1, detalle=(1, (40, 50, 40)))
        x0, y0, _, _ = c.cara("cabeza_capa", "izquierda")
        for i, color in enumerate(((240, 240, 230), (200, 60, 60), (240, 240, 230), (200, 60, 60))):
            c.punto(x0 + 4 + i, y0 + (1 if i < 2 else 0), color)
    elif nombre == "pañuelo":
        _gorro(c, (60, 90, 140), 3, 2)
        x0, y0, _, _ = c.cara("cabeza_capa", "detras")
        c.rect((x0 + 3, y0 + 3, x0 + 5, y0 + 5), (50, 76, 120))
    elif nombre == "chaperón":
        _gorro(c, (130, 40, 50), 3, 2, detalle=(2, (100, 30, 40)))
        x0, y0, _, _ = c.cara("cabeza_capa", "izquierda")
        c.rect((x0 + 5, y0 + 3, x0 + 7, y0 + 8), (130, 40, 50))
    elif nombre == "casquete de cuero":
        _gorro(c, CUERO, 2, 1, detalle=(1, oscuro(CUERO, 0.7)))
    elif nombre == "cuerno de caza":
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for i in range(8):
            c.punto(x0 + 7 - i, y0 + i, CUERO)
        c.rect((x0 + 1, y0 + 8, x0 + 3, y0 + 10), (230, 220, 190))
    elif nombre == "capa corta":
        _hombros(c, (110, 40, 40), 3, 2)
        c.caja("cuerpo_capa", (110, 40, 40), filas=(0, 8), solo=("detras",), fuerza=0.06)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.punto(x0 + 3, y0 + 1, ORO)
    elif nombre == "zurrón":
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for i in range(9):
            c.punto(x0 + i * 7 // 8, y0 + i, oscuro(CUERO, 0.8))
        c.rect((x0 + 6, y0 + 8, x0 + 8, y0 + 11), CUERO)
    elif nombre == "brazales de cuero":
        for capa in ("brazo_d_capa", "brazo_i_capa"):
            c.caja(capa, CUERO, filas=(6, 10), fuerza=0.06)
    elif nombre == "cinturón ancho":
        c.caja("cuerpo_capa", oscuro(CUERO, 0.9), filas=(8, 10), fuerza=0.05)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.rect((x0 + 3, y0 + 8, x0 + 5, y0 + 10), ORO)
    elif nombre == "gorro frigio":
        _gorro(c, (170, 40, 40), 3, 2)
        x0, y0, _, _ = c.cara("cabeza_capa", "frente")
        c.rect((x0 + 2, y0, x0 + 6, y0 + 1), oscuro((170, 40, 40), 0.8))
    return c


# ================================================================= RASGOS DE CARA (1..20; 0 = ninguno)
# Posiciones: mujeres (ojos en las filas 5-6, mejillas y boca en la 7); hombres (ojos en la 4, mejillas en la 5-6)

MARCAS = ["pecas", "muchas pecas", "cicatriz en la mejilla", "cicatriz sobre el ojo", "cicatriz en la nariz",
          "lunar en la mejilla", "lunar junto a la boca", "ojeras", "colorete", "hollín", "tierra en la cara",
          "nariz roja", "verruga", "ojo morado", "quemadura", "arrugas", "quemado por el sol", "marcas de viruela",
          "herida reciente", "parche en el ojo"]

PECA, CICATRIZ, LUNAR, OJERA, ROJO = (156, 98, 66), (236, 196, 186), (70, 44, 34), (120, 92, 110), (214, 92, 92)


def marca(sexo, n):
    mujer = sexo == "mujer"
    c = Lienzo(3 if mujer else 4, semilla=700 + n)
    nombre = MARCAS[n - 1]
    ojo = 5 if mujer else 4                  # fila de los ojos (la de arriba)
    mejilla = 7 if mujer else 5
    if nombre in ("pecas", "muchas pecas"):
        pecas = [(1, 7), (2, 7), (5, 7), (6, 7)] if mujer else [(1, 5), (2, 6), (5, 6), (6, 5)]
        if nombre == "muchas pecas":
            pecas += [(0, 7), (7, 7), (3, 6), (4, 6)] if mujer else [(0, 6), (7, 6), (3, 5), (4, 5)]
        for x, y in pecas:
            cf(c, x, y, PECA)
    elif nombre == "cicatriz en la mejilla":
        for y in (mejilla - 1, mejilla):
            cf(c, 6, y, CICATRIZ)
        cf(c, 7, mejilla - 2, CICATRIZ)
    elif nombre == "cicatriz sobre el ojo":
        for y in range(ojo - 2, ojo + 3):
            if y != ojo and not (mujer and y == ojo + 1):
                cf(c, 5, y, CICATRIZ)
    elif nombre == "cicatriz en la nariz":
        cf(c, 3, ojo + 1, CICATRIZ)
        cf(c, 4, ojo, CICATRIZ)
    elif nombre == "lunar en la mejilla":
        cf(c, 6 if not mujer else 7, mejilla, LUNAR)
    elif nombre == "lunar junto a la boca":
        cf(c, 5, 7 if mujer else 6, LUNAR)
    elif nombre == "ojeras":
        for x in (1, 2, 5, 6):
            cf(c, x, ojo + (2 if mujer else 1), OJERA)
    elif nombre == "colorete":
        for x in ((1, 2, 5, 6) if mujer else (0, 1, 6, 7)):
            cf(c, x, mejilla, ROJO)
    elif nombre in ("hollín", "tierra en la cara"):
        color = (64, 60, 58) if nombre == "hollín" else (122, 94, 64)
        for x, y in ((0, mejilla), (1, mejilla), (6, mejilla - 1), (7, 3), (2, 2)):
            cf(c, x, y, color)
    elif nombre == "nariz roja":
        cf(c, 3, ojo + (2 if mujer else 1), ROJO)
        cf(c, 4, ojo + (2 if mujer else 1), ROJO)
    elif nombre == "verruga":
        cf(c, 2, ojo + (2 if mujer else 2), (140, 110, 90))
    elif nombre == "ojo morado":
        for x, y in ((0, ojo), (0, ojo + 1), (1, ojo - 1), (2, ojo - 1), (1, ojo + (2 if mujer else 1))):
            cf(c, x, y, (110, 70, 120))
    elif nombre == "quemadura":
        for x, y in ((6, mejilla - 1), (7, mejilla - 1), (7, mejilla), (6, mejilla), (7, mejilla - 2)):
            cf(c, x, y, (200, 130, 120))
    elif nombre == "arrugas":
        for x in (3, 4):
            cf(c, x, 2, oscuro((200, 160, 130), 0.75))
        cf(c, 0, ojo + 1, oscuro((200, 160, 130), 0.75))
        cf(c, 7, ojo + 1, oscuro((200, 160, 130), 0.75))
    elif nombre == "quemado por el sol":
        for x in range(1, 7):
            cf(c, x, ojo + (2 if mujer else 1), (226, 120, 100) if x in (3, 4) else (220, 140, 120))
    elif nombre == "marcas de viruela":
        for x, y in ((1, mejilla), (6, mejilla), (2, 2), (5, 3), (7, mejilla - 1), (0, mejilla - 1)):
            cf(c, x, y, (176, 140, 120))
    elif nombre == "herida reciente":
        for x, y in ((5, 2), (6, 3), (7, 4)):
            cf(c, x, y, (180, 40, 40))
    else:                                                         # parche en el ojo
        for y in (ojo, ojo + (1 if mujer else 0)):
            cf(c, 5, y, (30, 26, 24))
            cf(c, 6, y, (30, 26, 24))
        for x in (0, 1, 2, 3, 4, 7):
            cf(c, x, ojo - 2, (40, 34, 30))
        x0, y0, x1, _ = c.cara("cabeza", "derecha")
        c.rect((x0, y0 + ojo - 2, x1, y0 + ojo - 1), (40, 34, 30))
        x0, y0, x1, _ = c.cara("cabeza", "izquierda")
        c.rect((x0, y0 + ojo - 2, x1, y0 + ojo - 1), (40, 34, 30))
        x0, y0, x1, _ = c.cara("cabeza", "detras")
        c.rect((x0, y0 + ojo - 2, x1, y0 + ojo - 1), (40, 34, 30))
    return c


NOMBRES_MARCA = {"hombre": MARCAS, "mujer": MARCAS}
