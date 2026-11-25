"""Época CONTEMPORÁNEA (la España de hoy, en un pueblo): 20 ropas, 20 peinados, 20 barbas, 20 complementos y 20
rasgos de cara por sexo. Camisetas y vaqueros, sudaderas con capucha, chándales, camisas, trajes, vestidos de verano,
zapatillas; gorras, gafas de sol, auriculares, mochilas, piercings y tatuajes. Mismo formato que epoca_medieval."""
from dibujar_habitantes import (Lienzo, cf, oscuro, mezclar, ruido, mechones, flequillo, flequillo_profundo, volumen,
                                cabeza, espalda, BRAZOS, PIERNAS, LADOS, G_BRILLO, G_LUZ, G, G_SOMBRA,
                                G_OSCURO, G_HONDO, COLETA, MONO, TRENZAS, FLEQUILLO, MELENA, TRENZA_ATRAS,
                                CAPAS_PIERNAS, CUERO, ORO, CAPA_X, CAPA_Y)
from epoca_medieval import _barba_base, _bigote, _perilla, _pecho, _gorro, _banda, _hombros, _flor, _collar

ID = "contemporanea"

NEGRO = (34, 34, 38)
BLANCO = (240, 240, 238)
VAQUERO = (70, 96, 140)
VAQUERO_CLARO = (120, 148, 186)
GRIS = (140, 140, 146)
PLATA = (200, 204, 210)


def zapatillas(c, color=BLANCO, suela=BLANCO):
    for pierna in PIERNAS:
        c.caja(pierna, color, filas=(10, 12), fuerza=0.03)
        c.fila(pierna, 11, suela)
        x0, y0, _, _ = c.cara(pierna, "frente")
        c.punto(x0 + 1, y0 + 10, oscuro(color, 0.8) if color != BLANCO else (60, 60, 70))
        x0, y0, _, _ = c.cara(pierna, "derecha")
        c.punto(x0 + 2, y0 + 10, (60, 60, 70) if color == BLANCO else BLANCO)


def vaqueros(c, color=VAQUERO, hasta=10, cintura=True):
    for pierna in PIERNAS:
        c.caja(pierna, color, filas=(0, hasta), fuerza=0.08)
        for lado in ("derecha", "izquierda"):
            x0, y0, x1, _ = c.cara(pierna, lado)
            for y in range(y0, y0 + hasta):
                c.punto(x0 + 2, y, oscuro(color, 0.82))          # costura
    if cintura:
        c.caja("cuerpo", color, filas=(11, 12), fuerza=0.08)
        fx, fy, _, _ = c.cara("cuerpo", "frente")
        c.punto(fx + 4, fy + 11, PLATA)


def logo(c, color, estilo=0):
    """Un dibujo sencillo en el pecho de la camiseta (no copia ninguna marca)."""
    fx, fy, _, _ = c.cara("cuerpo", "frente")
    if estilo == 0:                     # raya horizontal
        for col in range(1, 7):
            c.punto(fx + col, fy + 4, color)
    elif estilo == 1:                   # círculo
        for x, y in ((3, 3), (4, 3), (2, 4), (5, 4), (2, 5), (5, 5), (3, 6), (4, 6)):
            c.punto(fx + x, fy + y, color)
    elif estilo == 2:                   # estrella simple
        for x, y in ((4, 3), (3, 4), (4, 4), (5, 4), (4, 5), (3, 6), (5, 6)):
            c.punto(fx + x, fy + y, color)
    else:                               # bandas en las mangas
        for brazo in BRAZOS:
            c.fila(brazo, 2, color)


# ================================================================= ROPA: 5 cortes x 4 colores

COLORES = {
    "hombre": [
        [BLANCO, NEGRO, (180, 50, 50), (60, 110, 80)],                     # camiseta y vaqueros
        [GRIS, NEGRO, (50, 70, 120), (120, 80, 50)],                       # sudadera con capucha
        [(160, 190, 220), BLANCO, (200, 90, 80), (90, 120, 80)],           # camisa y chinos
        [(40, 44, 60), NEGRO, (90, 90, 96), (110, 80, 60)],                # traje
        [(40, 60, 130), NEGRO, (150, 30, 40), (40, 100, 60)],              # chándal
    ],
    "mujer": [
        [BLANCO, NEGRO, (220, 120, 150), (110, 160, 200)],                 # camiseta y vaqueros
        [(240, 200, 120), (200, 90, 110), (130, 180, 150), (150, 160, 220)],  # vestido de verano
        [GRIS, NEGRO, (180, 140, 190), (90, 120, 100)],                    # sudadera y mallas
        [BLANCO, (240, 220, 210), (160, 200, 220), (230, 200, 150)],       # blusa y falda
        [(120, 50, 60), (60, 70, 90), (200, 180, 150), (70, 90, 70)],      # jersey y vaqueros
    ],
}
NOMBRES_ROPA = {
    "hombre": ["camiseta y vaqueros", "sudadera con capucha", "camisa y chinos", "traje", "chándal"],
    "mujer": ["camiseta y vaqueros", "vestido de verano", "sudadera y mallas", "blusa y falda", "jersey y vaqueros"],
}
NOMBRES_COLOR = {
    "hombre": [["blanca", "negra", "roja", "verde"], ["gris", "negra", "azul", "marrón"],
               ["azul cielo", "blanca", "coral", "verde"], ["azul marino", "negro", "gris", "marrón"],
               ["azul", "negro", "rojo", "verde"]],
    "mujer": [["blanca", "negra", "rosa", "celeste"], ["amarillo", "rosa", "verde menta", "lila"],
              ["gris", "negra", "malva", "verde"], ["blanca", "crema", "azul", "beis"],
              ["granate", "azul", "beis", "verde"]],
}


def ropa(sexo, n):
    mujer = sexo == "mujer"
    corte, color = divmod(n, 4)
    tela = COLORES[sexo][corte][color]
    c = Lienzo(3 if mujer else 4, semilla=5200 + n + (40 if mujer else 0))
    fx, fy, _, _ = c.cara("cuerpo", "frente")
    contraste = NEGRO if tela in (BLANCO, (240, 200, 120), (240, 220, 210)) else BLANCO

    if corte == 0:                      # camiseta de manga corta con un dibujo, vaqueros y zapatillas
        c.caja("cuerpo", tela, fuerza=0.04)
        for brazo in BRAZOS:
            c.caja(brazo, tela, filas=(0, 4), fuerza=0.04)
            c.fila(brazo, 3, oscuro(tela, 0.88))
        c.punto(fx + 3, fy, oscuro(tela, 0.75))
        c.punto(fx + 4, fy, oscuro(tela, 0.75))
        logo(c, contraste if color % 2 == 0 else mezclar(tela, contraste, 0.5), n % 4)
        vaqueros(c, VAQUERO if color < 2 else VAQUERO_CLARO)
        zapatillas(c, BLANCO if color != 1 else NEGRO)
    elif not mujer and corte == 1:      # sudadera con capucha y bolsillo canguro, vaqueros, zapatillas
        c.caja("cuerpo", tela, fuerza=0.05)
        for brazo in BRAZOS:
            c.caja(brazo, tela, fuerza=0.05)
            c.fila(brazo, 11, oscuro(tela, 0.8))
        c.rect((fx + 1, fy + 7, fx + 7, fy + 10), oscuro(tela, 0.88))
        c.punto(fx + 3, fy + 1, BLANCO)                 # cordones
        c.punto(fx + 3, fy + 2, BLANCO)
        c.punto(fx + 5, fy + 1, BLANCO)
        c.punto(fx + 5, fy + 2, BLANCO)
        c.caja("cuerpo_capa", oscuro(tela, 0.9), filas=(0, 3), solo=("detras",), fuerza=0.05)   # la capucha
        c.fila("cuerpo", 11, oscuro(tela, 0.8))
        vaqueros(c, VAQUERO, cintura=False)
        zapatillas(c, NEGRO if tela != NEGRO else BLANCO, BLANCO)
    elif not mujer and corte == 2:      # camisa abotonada remangada y pantalón chino
        c.caja("cuerpo", tela, fuerza=0.04)
        for brazo in BRAZOS:
            c.caja(brazo, tela, filas=(0, 8), fuerza=0.04)
            c.fila(brazo, 7, oscuro(tela, 0.85))
        for fila in range(0, 11):
            c.punto(fx + 4, fy + fila, oscuro(tela, 0.85))
            if fila % 3 == 1:
                c.punto(fx + 3, fy + fila, BLANCO)
        c.punto(fx + 2, fy, oscuro(tela, 0.75))
        c.punto(fx + 5, fy, oscuro(tela, 0.75))
        chino = (190, 170, 130) if color != 3 else (70, 70, 80)
        for pierna in PIERNAS:
            c.caja(pierna, chino, filas=(0, 10), fuerza=0.04)
        c.caja("cuerpo", (80, 56, 40), filas=(11, 12), fuerza=0.04)              # cinturón
        zapatillas(c, (110, 80, 60), (200, 190, 170))
    elif not mujer and corte == 3:      # traje con camisa blanca y corbata, zapatos negros
        c.caja("cuerpo", tela, fuerza=0.04)
        for brazo in BRAZOS:
            c.caja(brazo, tela, filas=(0, 11), fuerza=0.04)
            c.fila(brazo, 11, BLANCO)
        for fila in range(0, 6):
            c.punto(fx + 3, fy + fila, BLANCO)
            c.punto(fx + 4, fy + fila, BLANCO)
        corbata = [(150, 40, 50), (60, 70, 130), (120, 120, 130), (60, 90, 60)][color]
        for fila in range(0, 7):
            c.punto(fx + 3 + (1 if fila == 0 else 0), fy + fila, corbata)
            c.punto(fx + 4, fy + fila, corbata)
        c.punto(fx + 2, fy + 1, oscuro(tela, 0.7))
        c.punto(fx + 5, fy + 1, oscuro(tela, 0.7))
        c.punto(fx + 4, fy + 8, (20, 20, 24))
        for pierna in PIERNAS:
            c.caja(pierna, tela, fuerza=0.04)
        for pierna in PIERNAS:
            c.caja(pierna, (24, 22, 24), filas=(11, 12), fuerza=0.03)
    elif not mujer:                     # chándal: chaqueta con cremallera y rayas, pantalón a juego
        c.caja("cuerpo", tela, fuerza=0.04)
        for brazo in BRAZOS:
            c.caja(brazo, tela, fuerza=0.04)
            for lado in ("derecha", "izquierda"):
                x0, y0, x1, _ = c.cara(brazo, lado)
                for y in range(y0, y0 + 12):
                    c.punto(x0 + 1, y, BLANCO)
        for fila in range(12):
            c.punto(fx + 4, fy + fila, PLATA if fila % 2 else oscuro(tela, 0.7))
        for pierna in PIERNAS:
            c.caja(pierna, tela, filas=(0, 10), fuerza=0.04)
            for lado in ("derecha", "izquierda"):
                x0, y0, _, _ = c.cara(pierna, lado)
                for y in range(y0, y0 + 10):
                    c.punto(x0 + 1, y, BLANCO)
        zapatillas(c)
    elif corte == 1:                    # vestido de verano de tirantes, con flores, y sandalias
        c.caja("cuerpo", tela, fuerza=0.03)
        for pierna in PIERNAS:
            c.caja(pierna, tela, filas=(0, 6), fuerza=0.03)
        for capa in CAPAS_PIERNAS:
            c.caja(capa, tela, filas=(0, 7), solo=LADOS, fuerza=0.03)
        for nombre in ("cuerpo", "pierna_d_capa", "pierna_i_capa"):
            for lado in LADOS:
                x0, y0, x1, y1 = c.cara(nombre, lado)
                for y in range(y0 + 2, y1 if nombre == "cuerpo" else y0 + 7):
                    for x in range(x0, x1):
                        if ruido(x, y, 77 + n) < 0.08:
                            c.punto(x, y, BLANCO)
        c.fila("cuerpo", 6, oscuro(tela, 0.8))
        for pierna in PIERNAS:          # sandalias
            c.fila(pierna, 11, (140, 100, 70))
            x0, y0, _, _ = c.cara(pierna, "frente")
            c.punto(x0 + 1, y0 + 10, (140, 100, 70))
    elif corte == 2:                    # sudadera ancha y mallas negras, zapatillas
        c.caja("cuerpo", tela, fuerza=0.05)
        for brazo in BRAZOS:
            c.caja(brazo, tela, fuerza=0.05)
            c.fila(brazo, 11, oscuro(tela, 0.8))
        c.fila("cuerpo", 9, oscuro(tela, 0.8))
        c.rect((fx + 1, fy + 6, fx + 7, fy + 9), oscuro(tela, 0.9))
        c.caja("cuerpo", (30, 30, 34), filas=(10, 12), fuerza=0.03)
        for pierna in PIERNAS:
            c.caja(pierna, (30, 30, 34), filas=(0, 10), fuerza=0.03)
        zapatillas(c, BLANCO, BLANCO)
    elif corte == 3:                    # blusa y falda por la rodilla, bailarinas
        c.caja("cuerpo", tela, fuerza=0.03)
        for brazo in BRAZOS:
            c.caja(brazo, tela, filas=(0, 6), fuerza=0.03)
        for fila in range(1, 7, 2):
            c.punto(fx + 4, fy + fila, oscuro(tela, 0.8))
        falda = [(40, 40, 60), (120, 40, 50), (60, 60, 64), (90, 70, 50)][color]
        c.caja("cuerpo", falda, filas=(8, 12), fuerza=0.03)
        for pierna in PIERNAS:
            c.caja(pierna, falda, filas=(0, 5), fuerza=0.03)
        for capa in CAPAS_PIERNAS:
            c.caja(capa, falda, filas=(0, 6), solo=LADOS, fuerza=0.03)
        for pierna in PIERNAS:
            c.caja(pierna, falda, filas=(11, 12), fuerza=0.03)
    else:                               # jersey de punto y vaqueros, botines
        c.caja("cuerpo", tela, fuerza=0.06)
        for brazo in BRAZOS:
            c.caja(brazo, tela, fuerza=0.06)
            c.fila(brazo, 11, oscuro(tela, 0.82))
        for lado in LADOS:              # punto: líneas verticales
            x0, y0, x1, _ = c.cara("cuerpo", lado)
            for x in range(x0, x1, 2):
                for y in range(y0 + 1, y0 + 10):
                    c.punto(x, y, oscuro(tela, 0.9))
        c.fila("cuerpo", 0, oscuro(tela, 0.8))
        c.fila("cuerpo", 10, oscuro(tela, 0.8))
        vaqueros(c, VAQUERO_CLARO if color % 2 else VAQUERO, hasta=9)
        for pierna in PIERNAS:
            c.caja(pierna, (70, 50, 40), filas=(9, 12), fuerza=0.04)
    return c


def nombre_ropa(sexo, n):
    corte, color = divmod(n, 4)
    return NOMBRES_ROPA[sexo][corte] + " " + NOMBRES_COLOR[sexo][corte][color]


# ================================================================= PEINADOS
# Especiales: "degradado" (rapado abajo a los lados), "rizado", "afro" (volumen de rizos), "rastas", "rapado",
# "calvo", "entradas", "lateral" (un lado rapado), "despeinado".
PEINADOS = {
    "mujer": [
        ("coleta alta", 5, 8, [5, 3, 2, 2, 2, 2, 3, 5], (0, 0), COLETA, False, None),
        ("moño despeinado", 5, 8, [6, 4, 2, 3, 2, 3, 3, 6], (0, 0), MONO, False, "despeinado"),
        ("melena corta (bob)", 6, 7, [7, 3, 2, 2, 2, 2, 3, 7], (0, 0), 0, False, None),
        ("pixie", 3, 5, [4, 4, 3, 3, 2, 2, 1, 2], (0, 0), FLEQUILLO, False, None),
        ("larga lisa", 8, 8, [8, 3, 2, 1, 1, 2, 3, 8], (8, 2), MELENA, False, None),
        ("ondas sueltas", 8, 8, [8, 4, 3, 3, 2, 3, 4, 8], (6, 2), MELENA, False, "rizado"),
        ("trenzas de raíz", 6, 8, [6, 3, 2, 1, 1, 2, 3, 6], (0, 0), TRENZAS, False, None),
        ("media coleta", 8, 8, [8, 3, 3, 2, 2, 3, 3, 8], (5, 1), MELENA, False, None),
        ("flequillo cortina", 8, 8, [8, 5, 3, 1, 1, 3, 5, 8], (6, 2), MELENA, False, None),
        ("rapado lateral", 6, 8, [8, 4, 4, 3, 2, 1, 1, 1], (5, 1), MELENA, False, "lateral"),
        ("afro", 7, 8, [5, 4, 3, 3, 3, 3, 4, 5], (0, 0), 0, False, "afro"),
        ("dos moños", 5, 8, [5, 3, 2, 2, 2, 2, 3, 5], (0, 0), TRENZAS, False, None),
        ("larga con flequillo", 8, 8, [8, 4, 4, 4, 4, 4, 4, 8], (7, 2), FLEQUILLO | MELENA, False, None),
        ("rastas", 8, 8, [8, 4, 2, 4, 2, 4, 2, 8], (8, 2), MELENA, False, "rastas"),
        ("corta despeinada", 4, 6, [6, 4, 2, 4, 3, 2, 4, 6], (0, 0), FLEQUILLO, False, "despeinado"),
        ("coleta baja", 6, 8, [6, 3, 3, 2, 2, 3, 3, 6], (0, 0), TRENZA_ATRAS, False, None),
        ("melena por los hombros", 7, 8, [7, 3, 2, 2, 1, 2, 3, 7], (2, 1), MELENA, False, None),
        ("rizos cortos", 5, 7, [6, 4, 3, 4, 3, 4, 3, 6], (0, 0), FLEQUILLO, False, "rizado"),
        ("trenza de espiga", 6, 8, [6, 3, 4, 2, 3, 4, 3, 6], (0, 0), TRENZA_ATRAS, False, None),
        ("rapada", 4, 7, [1] * 8, (0, 0), 0, False, "rapado"),
    ],
    "hombre": [
        ("degradado", 2, 6, [2, 2, 1, 2, 2, 1, 2, 2], (0, 0), 0, False, "degradado"),
        ("tupé con degradado", 2, 6, [1, 2, 3, 3, 3, 3, 2, 1], (0, 0), FLEQUILLO, False, "degradado"),
        ("raya al lado", 3, 7, [3, 3, 2, 2, 1, 1, 1, 2], (0, 0), 0, False, None),
        ("moño masculino", 4, 8, [1, 1, 0, 0, 0, 0, 1, 1], (0, 0), MONO, False, None),
        ("rapado", 4, 7, [1] * 8, (0, 0), 0, False, "rapado"),
        ("rizos", 4, 7, [3, 3, 2, 3, 2, 3, 2, 3], (0, 0), FLEQUILLO, False, "rizado"),
        ("rastas cortas", 5, 7, [3, 2, 1, 2, 1, 2, 1, 3], (0, 0), 0, False, "rastas"),
        ("flequillo", 4, 7, [3, 3, 3, 3, 3, 3, 3, 3], (0, 0), FLEQUILLO, False, None),
        ("calvo", 0, 0, [0] * 8, (0, 0), 0, True, "calvo_del_todo"),
        ("cresta", 2, 6, [0, 0, 1, 3, 3, 1, 0, 0], (0, 0), 0, False, "cresta"),
        ("pelo largo", 8, 8, [4, 3, 2, 2, 2, 2, 3, 4], (5, 1), MELENA, False, None),
        ("afro", 6, 7, [3, 3, 2, 2, 2, 2, 3, 3], (0, 0), 0, False, "afro"),
        ("con entradas", 3, 7, [0, 1, 0, 0, 0, 0, 1, 0], (0, 0), 0, True, "entradas"),
        ("despeinado", 4, 7, [3, 2, 3, 1, 3, 2, 3, 2], (0, 0), FLEQUILLO, False, "despeinado"),
        ("peinado hacia atrás", 3, 8, [1, 1, 0, 0, 0, 0, 1, 1], (0, 0), 0, False, None),
        ("media melena", 7, 8, [4, 2, 2, 1, 1, 2, 2, 4], (1, 0), MELENA, False, None),
        ("rapado con dibujo", 4, 7, [1] * 8, (0, 0), 0, False, "rapado_dibujo"),
        ("flequillo de lado", 3, 7, [4, 4, 3, 3, 2, 2, 1, 1], (0, 0), FLEQUILLO, False, "degradado"),
        ("coleta", 5, 8, [2, 2, 1, 1, 1, 1, 2, 2], (0, 0), COLETA, False, None),
        ("canoso corto", 3, 6, [1, 1, 0, 0, 0, 0, 1, 1], (0, 0), 0, True, None),
    ],
}


def _rastas(c):
    for cual in ("arriba", "derecha", "izquierda", "detras"):
        x0, y0, x1, y1 = c.cara("cabeza", cual)
        for y in range(y0, y1):
            for x in range(x0, x1):
                if c.leer(x, y)[3] and (x - x0) % 3 == 2:
                    c.punto(x, y, G_OSCURO)
                elif c.leer(x, y)[3] and (y + (x - x0) // 3) % 3 == 0:
                    c.punto(x, y, G_LUZ)


def _degradado(c):
    """Lados rapados que se van oscureciendo hacia abajo (la piel asoma entre el pelo)."""
    for cual in ("derecha", "izquierda", "detras"):
        x0, y0, x1, y1 = c.cara("cabeza", cual)
        for y in range(y0 + 2, y0 + 7):
            for x in range(x0, x1):
                if ruido(x, y, 15) < (y - y0 - 1) / 6:
                    c.borrar(x, y)
                else:
                    c.punto(x, y, G_SOMBRA)


def _afro(c):
    """Volumen de rizos por toda la cabeza (capa exterior entera, con textura de rizo)."""
    for cual in ("arriba", "derecha", "izquierda", "detras"):
        x0, y0, x1, y1 = c.cara("cabeza_capa", cual)
        for y in range(y0, y1 if cual in ("arriba", "detras") else y0 + 6):
            for x in range(x0, x1):
                c.punto(x, y, G_LUZ if (x + y) % 2 == 0 else (G_SOMBRA if (x * 3 + y) % 5 == 0 else G))
    x0, y0, x1, _ = c.cara("cabeza_capa", "frente")
    for x in range(x0, x1):
        c.punto(x, y0, G_LUZ if x % 2 else G)


def pelo(sexo, n):
    mujer = sexo == "mujer"
    nombre, lados, nuca, fleq, (esp, esp_lados), _, _, especial = PEINADOS[sexo][n]
    c = Lienzo(3 if mujer else 4, semilla=5100 + n * 3 + (50 if mujer else 0))
    if not mujer:
        for col in (1, 2, 5, 6):
            cf(c, col, 3, G_OSCURO)
    if especial in ("rapado", "rapado_dibujo"):
        for cual, filas in (("arriba", 8), ("derecha", 4), ("izquierda", 4), ("detras", 7)):
            x0, y0, x1, _ = c.cara("cabeza", cual)
            for y in range(y0, y0 + filas):
                for x in range(x0, x1):
                    c.punto(x, y, G_SOMBRA if ruido(x, y, 3) < 0.7 else G_OSCURO)
        if especial == "rapado_dibujo":                 # una raya afeitada en un lado
            x0, y0, x1, _ = c.cara("cabeza", "izquierda")
            for x in range(x0 + 1, x1 - 1):
                c.borrar(x, y0 + 2 + (x - x0) // 3)
    elif especial == "calvo_del_todo":
        pass
    elif especial == "cresta":
        x0, y0, x1, y1 = c.cara("cabeza", "arriba")
        mechones(c, (x0 + 2, y0, x0 + 6, y1))
        x0, y0, x1, _ = c.cara("cabeza", "detras")
        mechones(c, (x0 + 2, y0, x0 + 6, y0 + nuca), False)
        for cual in ("derecha", "izquierda"):
            x0, y0, x1, _ = c.cara("cabeza", cual)
            for y in range(y0, y0 + 3):
                for x in range(x0, x1):
                    c.punto(x, y, G_SOMBRA if ruido(x, y, 3) < 0.6 else G_OSCURO)
        x0, y0, x1, y1 = c.cara("cabeza_capa", "arriba")
        for y in range(y0, y1):
            for x in (x0 + 3, x0 + 4):
                c.punto(x, y, G_LUZ if (x + y) % 2 else G)
    else:
        cabeza(c, lados, nuca)
        if especial == "entradas":
            x0, y0, x1, y1 = c.cara("cabeza", "arriba")
            for x in (x0, x0 + 1, x1 - 2, x1 - 1):
                for y in range(y1 - 3, y1):
                    c.borrar(x, y)
        if especial == "lateral":
            x0, y0, x1, _ = c.cara("cabeza", "izquierda")
            for y in range(y0, y0 + 8):
                for x in range(x0, x1):
                    if ruido(x, y, 4) < 0.6:
                        c.borrar(x, y)
                    else:
                        c.punto(x, y, G_SOMBRA)
    if especial not in ("rapado", "rapado_dibujo", "calvo_del_todo", "cresta"):
        (flequillo_profundo if mujer else flequillo)(c, fleq)
    elif especial in ("rapado", "rapado_dibujo", "cresta"):
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
    elif especial == "rastas":
        _rastas(c)
    elif especial == "despeinado":
        for cual in ("arriba", "derecha", "izquierda", "detras"):
            x0, y0, x1, y1 = c.cara("cabeza", cual)
            for y in range(y0, y1):
                for x in range(x0, x1):
                    if c.leer(x, y)[3] and ruido(x, y, 51 + c.semilla) < 0.2:
                        c.punto(x, y, G_OSCURO)
    if especial not in ("rapado", "rapado_dibujo", "calvo_del_todo", "cresta", "afro"):
        volumen(c, 0.3 if mujer else 0.45)
    if especial == "degradado":
        _degradado(c)
    elif especial == "afro":
        _afro(c)
    elif especial == "despeinado":
        x0, y0, x1, y1 = c.cara("cabeza_capa", "arriba")
        for y in range(y0, y1):
            for x in range(x0, x1):
                if ruido(x, y, 61 + c.semilla) < 0.25:
                    c.punto(x, y, G_LUZ)
    return c


# ================================================================= BARBAS (1..20; 0 = ninguna)

BARBAS = [
    "de tres días", "barba corta degradada", "barba hípster", "barba completa", "perilla", "bigote", "candado",
    "barba de una semana", "barba larga", "patillas", "bigote y perilla", "barba recortada", "barba rala",
    "barba de leñador", "perilla larga", "bigote fino", "barba de collar", "barba bifurcada", "barba canosa corta",
    "bigote grueso",
]


def barba(n):
    c = Lienzo(semilla=5400 + n)
    nombre = BARBAS[n - 1]
    if nombre == "de tres días":
        for col in range(8):
            if col not in (3, 4):
                cf(c, col, 7, G_SOMBRA)
        cf(c, 0, 6, G_SOMBRA)
        cf(c, 7, 6, G_SOMBRA)
    elif nombre == "barba corta degradada":
        _barba_base(c, 2, G_SOMBRA)
        _bigote(c, "fino")
        for col in (0, 7):
            cf(c, col, 4, G_HONDO)
    elif nombre == "barba hípster":
        _barba_base(c, 4)
        _bigote(c, "manillar")
        _pecho(c, 3, 6)
    elif nombre == "barba completa":
        _barba_base(c)
        _bigote(c, "grueso")
    elif nombre == "perilla":
        _perilla(c)
    elif nombre == "bigote":
        _bigote(c, "grueso")
    elif nombre == "candado":
        _bigote(c, "fino")
        for col in (2, 3, 4, 5):
            cf(c, col, 7, G)
        for col in (1, 6):
            cf(c, col, 6, G_SOMBRA)
    elif nombre == "barba de una semana":
        _barba_base(c, 3, G_SOMBRA)
        for col in range(8):
            for fila in (5, 6, 7):
                if ruido(col, fila, 13) < 0.2 and c.leer(8 + col, 8 + fila)[3]:
                    cf(c, col, fila, G_HONDO)
    elif nombre == "barba larga":
        _barba_base(c)
        _bigote(c, "grueso")
        _pecho(c, 4)
    elif nombre == "patillas":
        for col in (0, 7):
            for fila in range(3, 7):
                cf(c, col, fila, G_SOMBRA)
        for lado in ("derecha", "izquierda"):
            x0, y0, x1, _ = c.cara("cabeza", lado)
            mechones(c, (x0, y0 + 3, x0 + 3, y0 + 7), False)
    elif nombre == "bigote y perilla":
        _bigote(c, "fino")
        _perilla(c)
    elif nombre == "barba recortada":
        _barba_base(c, 2, G_SOMBRA)
        _bigote(c, "fino")
    elif nombre == "barba rala":
        for col in range(8):
            for fila in (5, 6, 7):
                if ruido(col, fila, 9) < (0.5 if fila == 7 else 0.25) and not (fila < 7 and 2 <= col <= 5):
                    cf(c, col, fila, G_SOMBRA)
    elif nombre == "barba de leñador":
        _barba_base(c, 4)
        _bigote(c, "grueso")
        _pecho(c, 2, 6)
    elif nombre == "perilla larga":
        _bigote(c, "fino")
        _perilla(c, larga=True)
    elif nombre == "bigote fino":
        _bigote(c, "fino")
    elif nombre == "barba de collar":
        for col in range(8):
            cf(c, col, 7, G_SOMBRA)
        for col in (0, 7):
            cf(c, col, 6, G_SOMBRA)
            cf(c, col, 5, G_SOMBRA)
        mechones(c, c.cara("cabeza", "abajo"), False)
    elif nombre == "barba bifurcada":
        _barba_base(c)
        _bigote(c, "grueso")
        _pecho(c, 4, 6, hueco=True)
    elif nombre == "barba canosa corta":
        _barba_base(c, 2, G_LUZ)
        _bigote(c, "fino")
    else:                                                          # bigote grueso
        _bigote(c, "grueso")
        cf(c, 1, 6, G)
        cf(c, 6, 6, G)
    return c


# ================================================================= COMPLEMENTOS (1..20; 0 = ninguno). No se tiñen.

def _gorra(c, color, visera=None, atras=False):
    _gorro(c, color, 2, 1, fuerza=0.04)
    visera = visera or oscuro(color, 0.75)
    cual = "detras" if atras else "frente"
    x0, y0, x1, _ = c.cara("cabeza_capa", cual)
    c.rect((x0 + 1, y0 + 2, x1 - 1, y0 + 3), visera)


def _gafas(c, cristal, montura, ojo):
    fx, fy, _, _ = c.cara("cabeza_capa", "frente")
    for col in (1, 2, 5, 6):
        c.punto(fx + col, fy + ojo, cristal)
    for col in (0, 3, 4, 7):
        c.punto(fx + col, fy + ojo, montura)
    for cual in ("derecha", "izquierda"):
        x0, y0, x1, _ = c.cara("cabeza_capa", cual)
        c.rect((x0, y0 + ojo, x1, y0 + ojo + 1), montura)


def _auriculares(c, color, al_cuello=False):
    if al_cuello:
        for cual in ("derecha", "izquierda", "detras"):
            x0, y0, x1, _ = c.cara("cuerpo_capa", cual)
            c.rect((x0, y0, x1, y0 + 1), color)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.rect((x0, y0, x0 + 2, y0 + 2), color)
        c.rect((x0 + 6, y0, x0 + 8, y0 + 2), color)
        return
    x0, y0, x1, y1 = c.cara("cabeza_capa", "arriba")
    for y in range(y0, y1):
        c.punto(x0 + 3, y, color)
        c.punto(x0 + 4, y, color)
    for cual in ("derecha", "izquierda"):
        x0, y0, x1, _ = c.cara("cabeza_capa", cual)
        for y in range(y0, y0 + 4):
            c.punto(x0 + 3, y, color)
            c.punto(x0 + 4, y, color)
        c.rect((x0 + 2, y0 + 4, x0 + 6, y0 + 7), oscuro(color, 0.8))


def _mochila(c, color):
    x0, y0, _, _ = c.cara("cuerpo_capa", "detras")
    c.rect((x0 + 1, y0 + 1, x0 + 7, y0 + 10), color, 0.04)
    c.rect((x0 + 2, y0 + 6, x0 + 6, y0 + 9), oscuro(color, 0.85))
    x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
    for fila in range(0, 8):
        c.punto(x0 + 1, y0 + fila, oscuro(color, 0.7))
        c.punto(x0 + 6, y0 + fila, oscuro(color, 0.7))


def _tatuaje(c, capa, color=(40, 50, 70)):
    x0, y0, _, _ = c.cara(capa, "frente")
    for x, y in ((1, 1), (2, 1), (0, 2), (3, 2), (1, 3), (2, 3), (1, 4), (2, 5), (1, 6)):
        c.punto(x0 + x, y0 + y, color)
    x0, y0, _, _ = c.cara(capa, "derecha" if capa.startswith("brazo_d") else "izquierda")
    for x, y in ((1, 2), (2, 3), (1, 4)):
        c.punto(x0 + x, y0 + y, color)


COMPLEMENTOS = {
    "mujer": ["gafas de sol en la cabeza", "gafas de pasta", "auriculares", "gorro de lana", "gorra",
              "coletero", "diadema", "pendientes de aro", "collar fino", "mochila", "bolso cruzado", "bufanda",
              "reloj", "pulseras", "piercing en la nariz", "tatuaje en el brazo", "lazo", "sombrero de paja",
              "cinta en el pelo", "gafas de sol"],
    "hombre": ["gorra", "gorra hacia atrás", "gorro de lana", "gafas de sol", "gafas de pasta", "auriculares",
               "cascos al cuello", "mochila", "reloj", "pendiente", "corbata", "bufanda", "cadena", "sombrero",
               "riñonera", "pulsera", "pendiente de aro", "tatuaje en el brazo", "gafas redondas", "cinta deportiva"],
}


def complemento(sexo, n):
    mujer = sexo == "mujer"
    c = Lienzo(3 if mujer else 4, semilla=5600 + n + (40 if mujer else 0))
    nombre = COMPLEMENTOS[sexo][n - 1]
    fx, fy, _, _ = c.cara("cabeza_capa", "frente")
    ojo = 5 if mujer else 4
    if nombre == "gafas de sol en la cabeza":
        _gafas(c, (40, 40, 50), (20, 20, 24), 1)
    elif nombre == "gafas de sol":
        _gafas(c, (30, 30, 40), (20, 20, 24), ojo)
    elif nombre == "gafas de pasta":
        _gafas(c, (220, 230, 240), (30, 26, 24), ojo)
    elif nombre == "gafas redondas":
        _gafas(c, (220, 230, 240), (170, 140, 80), ojo)
    elif nombre == "auriculares":
        _auriculares(c, (240, 240, 240) if mujer else (30, 30, 34))
    elif nombre == "cascos al cuello":
        _auriculares(c, (30, 30, 34), al_cuello=True)
    elif nombre == "gorro de lana":
        color = (200, 80, 90) if mujer else (60, 70, 90)
        _gorro(c, color, 3, 2, detalle=(2, oscuro(color, 0.75)))
        x0, y0, x1, y1 = c.cara("cabeza_capa", "arriba")
        c.punto(x0 + 3, y0 + 3, (240, 240, 240))
        c.punto(x0 + 4, y0 + 4, (240, 240, 240))
    elif nombre == "gorra":
        _gorra(c, (230, 230, 230) if mujer else (40, 60, 120))
    elif nombre == "gorra hacia atrás":
        _gorra(c, (180, 40, 40), atras=True)
    elif nombre == "coletero":
        x0, y0, _, _ = c.cara("cabeza_capa", "detras")
        c.rect((x0 + 3, y0 + 2, x0 + 5, y0 + 4), (220, 120, 150))
    elif nombre == "diadema":
        _banda(c, 1, (40, 40, 44), frente=False)
        x0, y0, x1, y1 = c.cara("cabeza_capa", "arriba")
        for x in range(x0, x1):
            c.punto(x, y1 - 3, (40, 40, 44))
    elif nombre in ("pendientes de aro", "pendiente de aro"):
        for cual in (("derecha", "izquierda") if mujer else ("izquierda",)):
            x0, y0, _, _ = c.cara("cabeza_capa", cual)
            c.punto(x0 + 4, y0 + 6, ORO)
            c.punto(x0 + 3, y0 + 7, ORO)
            c.punto(x0 + 5, y0 + 7, ORO)
    elif nombre == "pendiente":
        x0, y0, _, _ = c.cara("cabeza_capa", "izquierda")
        c.punto(x0 + 4, y0 + 6, PLATA)
    elif nombre == "collar fino":
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for col in (1, 2, 5, 6):
            c.punto(x0 + col, y0 + (0 if col in (1, 6) else 1), ORO)
        c.punto(x0 + 3, y0 + 2, ORO)
        c.punto(x0 + 4, y0 + 2, ORO)
    elif nombre == "cadena":
        _collar(c, PLATA, PLATA, filas=2)
    elif nombre == "mochila":
        _mochila(c, (60, 90, 150) if mujer else (40, 40, 44))
    elif nombre == "bolso cruzado":
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for i in range(9):
            c.punto(x0 + 7 - i * 7 // 8, y0 + i, (90, 60, 40))
        c.rect((x0, y0 + 8, x0 + 3, y0 + 11), (150, 90, 60))
    elif nombre == "riñonera":
        c.fila("cuerpo_capa", 9, (30, 30, 34), solo=None)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.rect((x0 + 2, y0 + 9, x0 + 6, y0 + 11), (40, 40, 44))
        c.punto(x0 + 3, y0 + 9, PLATA)
    elif nombre == "bufanda":
        color = (190, 60, 70) if mujer else (80, 80, 90)
        for cual in LADOS:
            x0, y0, x1, _ = c.cara("cuerpo_capa", cual)
            c.rect((x0, y0, x1, y0 + 2), color, 0.06)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.rect((x0 + 1, y0 + 2, x0 + 3, y0 + 7), color, 0.06)
    elif nombre == "reloj":
        x0, y0, _, _ = c.cara("brazo_i_capa", "frente")
        c.rect((x0, y0 + 9, x0 + (2 if mujer else 3), y0 + 10), (30, 30, 34))
        c.punto(x0 + 1, y0 + 9, PLATA)
    elif nombre in ("pulseras", "pulsera"):
        c.fila("brazo_d_capa", 9, (200, 60, 80) if mujer else (40, 40, 44))
        if mujer:
            c.fila("brazo_d_capa", 10, ORO)
    elif nombre == "piercing en la nariz":
        c.punto(fx + 4, fy + 6, PLATA)
    elif nombre == "tatuaje en el brazo":
        _tatuaje(c, "brazo_i_capa")
    elif nombre == "lazo":
        x0, y0, _, _ = c.cara("cabeza_capa", "detras")
        for x, y in ((2, 2), (3, 3), (4, 3), (5, 2), (2, 4), (5, 4)):
            c.punto(x0 + x, y0 + y, (30, 30, 34))
    elif nombre == "sombrero de paja":
        _gorro(c, (220, 196, 130), 2, 1, detalle=(1, (40, 40, 44)), fuerza=0.1)
    elif nombre == "sombrero":
        _gorro(c, (70, 66, 64), 2, 1, detalle=(1, (30, 30, 34)), fuerza=0.04)
    elif nombre in ("cinta en el pelo", "cinta deportiva"):
        _banda(c, 1 if mujer else 2, (230, 230, 230) if nombre == "cinta deportiva" else (60, 120, 200))
    elif nombre == "corbata":
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for fila in range(0, 7):
            c.punto(x0 + 3, y0 + fila, (150, 40, 50))
            c.punto(x0 + 4, y0 + fila, (150, 40, 50))
    return c


# ================================================================= RASGOS DE CARA (1..20; 0 = ninguno)

MARCAS = ["raya en el ojo", "sombra de ojos", "pecas", "lunar", "colorete", "labios pintados", "ojeras",
          "granos", "quemado por el sol", "cicatriz", "cicatriz en la ceja", "arrugas", "purpurina", "pintalabios oscuro",
          "rubor", "tirita en la nariz", "pecas pintadas", "ojo morado", "verruga", "estrellita junto al ojo"]

CICATRIZ, LUNAR, OJERA, ROJO_C = (236, 196, 186), (50, 34, 30), (120, 92, 110), (214, 92, 92)


def marca(sexo, n):
    mujer = sexo == "mujer"
    c = Lienzo(3 if mujer else 4, semilla=5700 + n)
    nombre = MARCAS[n - 1]
    ojo = 5 if mujer else 4
    mejilla = 7 if mujer else 5
    if nombre == "raya en el ojo":
        for x in (0, 7):
            cf(c, x, ojo - 1 if mujer else ojo, (24, 20, 22))
        if not mujer:
            for x in (1, 2, 5, 6):
                cf(c, x, 3, (40, 30, 30))
    elif nombre == "sombra de ojos":
        for x in (1, 2, 5, 6):
            cf(c, x, 3 if mujer else ojo - 1, (150, 110, 170))
    elif nombre in ("pecas", "pecas pintadas"):
        pecas = [(1, 7), (2, 7), (5, 7), (6, 7), (0, 7), (7, 7)] if mujer else [(1, 5), (2, 6), (5, 6), (6, 5)]
        for x, y in pecas:
            cf(c, x, y, (156, 98, 66) if nombre == "pecas" else (120, 70, 50))
    elif nombre == "lunar":
        cf(c, 6 if not mujer else 7, mejilla, LUNAR)
    elif nombre in ("colorete", "rubor"):
        for x in ((1, 2, 5, 6) if mujer else (0, 1, 6, 7)):
            cf(c, x, mejilla, ROJO_C if nombre == "colorete" else (236, 150, 150))
    elif nombre == "labios pintados":
        if mujer:
            cf(c, 3, 7, (200, 40, 60))
            cf(c, 4, 7, (200, 40, 60))
        else:
            cf(c, 3, 6, (170, 60, 70))
            cf(c, 4, 6, (170, 60, 70))
    elif nombre == "pintalabios oscuro":
        y = 7 if mujer else 6
        cf(c, 3, y, (90, 30, 50))
        cf(c, 4, y, (90, 30, 50))
    elif nombre == "ojeras":
        for x in (1, 2, 5, 6):
            cf(c, x, ojo + (2 if mujer else 1), OJERA)
    elif nombre == "granos":
        for x, y in ((1, 2), (6, mejilla), (2, mejilla), (5, 1)):
            cf(c, x, y, (220, 110, 100))
    elif nombre == "quemado por el sol":
        for x in range(1, 7):
            cf(c, x, ojo + (2 if mujer else 1), (226, 120, 100) if x in (3, 4) else (220, 140, 120))
    elif nombre == "cicatriz":
        for y in (mejilla - 1, mejilla):
            cf(c, 6, y, CICATRIZ)
        cf(c, 7, mejilla - 2, CICATRIZ)
    elif nombre == "cicatriz en la ceja":
        for y in range(ojo - 2, ojo):
            cf(c, 5, y, CICATRIZ)
    elif nombre == "arrugas":
        for x in (3, 4):
            cf(c, x, 2, oscuro((200, 160, 130), 0.75))
        cf(c, 0, ojo + 1, oscuro((200, 160, 130), 0.75))
        cf(c, 7, ojo + 1, oscuro((200, 160, 130), 0.75))
    elif nombre == "purpurina":
        for x, y in ((0, ojo), (7, ojo), (0, mejilla - 1), (7, mejilla - 1), (1, 3), (6, 3)):
            cf(c, x, y, (230, 200, 240) if (x + y) % 2 else (250, 230, 150))
    elif nombre == "tirita en la nariz":
        for x in (2, 3, 4, 5):
            cf(c, x, ojo + (2 if mujer else 1), (230, 190, 150))
    elif nombre == "ojo morado":
        for x, y in ((0, ojo), (0, ojo + 1), (1, ojo - 1), (2, ojo - 1), (1, ojo + (2 if mujer else 1))):
            cf(c, x, y, (110, 70, 120))
    elif nombre == "verruga":
        cf(c, 2, ojo + 2, (140, 110, 90))
    else:                                                         # estrellita (tatuaje o pegatina) junto al ojo
        y = ojo + (1 if mujer else 1)
        for x, dy in ((7, 0), (6, 1), (7, 1), (7, 2)):
            cf(c, x, y + dy - 1, (60, 70, 120))
    return c


NOMBRES_MARCA = {"hombre": MARCAS, "mujer": MARCAS}
