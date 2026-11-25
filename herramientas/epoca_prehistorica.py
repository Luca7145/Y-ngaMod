"""Época PREHISTÓRICA: 20 ropas, 20 peinados, 20 barbas, 20 complementos y 20 rasgos de cara por sexo.
Pieles, fibras trenzadas, cuero sin curtir, collares de dientes y conchas, plumas y pinturas de ocre. Nada de metal ni
de telas tejidas finas. Mismo formato que epoca_medieval (ver dibujar_habitantes)."""
from dibujar_habitantes import (Lienzo, cf, oscuro, mezclar, ruido, mechones, flequillo, flequillo_profundo, volumen,
                                cabeza, espalda, cejas, BRAZOS, PIERNAS, LADOS, G_BRILLO, G_LUZ, G, G_SOMBRA,
                                G_OSCURO, G_HONDO, COLETA, MONO, TRENZAS, FLEQUILLO, MELENA, TRENZA_ATRAS,
                                CAPAS_PIERNAS)
from epoca_medieval import _barba_base, _bigote, _perilla, _pecho, _gorro, _banda, _hombros, _flor

ID = "prehistorica"

# ================================================================= MATERIALES

PIEL_CLARA = (176, 132, 88)       # piel curtida clara (ciervo)
PIEL_OSCURA = (104, 72, 48)       # piel oscura (bisonte)
LOBO = (128, 122, 114)            # piel gris de lobo
LEOPARDO = (196, 150, 76)         # piel con manchas
OSO = (84, 60, 44)
FIBRA = (178, 156, 96)            # hierbas secas, rafia
CUERDA = (150, 120, 76)
HUESO = (232, 224, 200)
DIENTE = (240, 234, 214)
CONCHA = (236, 214, 200)
OCRE = (178, 84, 48)
OCRE_AMARILLO = (204, 150, 60)
CARBON = (44, 40, 38)
ARCILLA = (206, 196, 184)
AMBAR = (222, 140, 40)
PELO_BLANCO = (226, 222, 212)     # borde de pelo de las pieles de invierno

PIEL = 0.13                       # textura de las pieles: más basta que la de las telas


def _pelaje(c, nombre, color, filas=None, solo=None, manchas=None, fuerza=PIEL):
    """Una piel animal: textura basta y, si se pide, manchas (leopardo) o rayas (fibras)."""
    c.caja(nombre, color, filas=filas, fuerza=fuerza, solo=solo)
    if not manchas:
        return
    u, v, w, h, d = c.cajas[nombre]
    from dibujar_habitantes import caras
    a, b = filas if filas else (0, h)
    for cual, (x0, y0, x1, y1) in caras(u, v, w, h, d).items():
        if solo and cual not in solo:
            continue
        if cual in ("arriba", "abajo"):
            continue
        for y in range(y0 + a, y0 + b):
            for x in range(x0, x1):
                r = ruido(x, y, 313 + c.semilla)
                if manchas == "manchas" and r < 0.16:
                    c.punto(x, y, oscuro(color, 0.45))
                elif manchas == "fibras" and (x + (y // 2)) % 3 == 0:
                    c.punto(x, y, oscuro(color, 0.82))


def _flecos(c, nombre, fila, color, cual=LADOS):
    """Fila de flecos en el borde: un píxel sí y otro no (con hueco transparente si es una capa)."""
    u, v, w, h, d = c.cajas[nombre]
    for lado in cual:
        x0, y0, x1, _ = c.cara(nombre, lado)
        for x in range(x0, x1):
            if (x + fila) % 2 == 0:
                c.punto(x, y0 + fila, oscuro(color, 0.85))
            elif nombre.endswith("_capa"):
                c.borrar(x, y0 + fila)


def _atado(c, pierna_capa, color, desde=6, hasta=12):
    """Polainas de piel atadas con cuerda en espiral."""
    c.caja(pierna_capa, color, filas=(desde, hasta), solo=LADOS, fuerza=PIEL)
    for lado in LADOS:
        x0, y0, x1, _ = c.cara(pierna_capa, lado)
        for y in range(y0 + desde, y0 + hasta):
            c.punto(x0 + (y * 2) % (x1 - x0), y, CUERDA)


def _pies(c, color=PIEL_OSCURA):
    """Abarcas de piel atadas al tobillo."""
    for pierna in PIERNAS:
        c.caja(pierna, color, filas=(10, 12), fuerza=PIEL)
        c.fila(pierna, 10, CUERDA)


def _bandolera(c, color, de_izquierda=True):
    x0, y0, _, _ = c.cara("cuerpo", "frente")
    for i in range(10):
        col = i * 7 // 9 if de_izquierda else 7 - i * 7 // 9
        c.punto(x0 + col, y0 + i, color)
    x0, y0, _, _ = c.cara("cuerpo", "detras")
    for i in range(10):
        col = 7 - i * 7 // 9 if de_izquierda else i * 7 // 9
        c.punto(x0 + col, y0 + i, oscuro(color, 0.9))


def _un_hombro(c, nombre, color, desde_fila, izquierda=True, manchas=None):
    """Prenda que deja un hombro al aire: el borde de arriba va en diagonal."""
    _pelaje(c, nombre, color, filas=(0, 12), manchas=manchas)
    x0, y0, x1, _ = c.cara(nombre, "frente")
    bx0, by0, bx1, _ = c.cara(nombre, "detras")
    for col in range(8):
        hueco = (7 - col if izquierda else col) * desde_fila // 7
        for fila in range(hueco):
            c.borrar(x0 + col, y0 + fila)
            c.borrar(bx0 + 7 - col, by0 + fila)
    for lado in (("izquierda",) if izquierda else ("derecha",)):
        lx0, ly0, lx1, _ = c.cara(nombre, lado)
        c.rect((lx0, ly0, lx1, ly0 + desde_fila), (0, 0, 0))
        for y in range(ly0, ly0 + desde_fila):
            for x in range(lx0, lx1):
                c.borrar(x, y)
    arriba = c.cara(nombre, "arriba")
    for y in range(arriba[1], arriba[3]):
        for x in range(arriba[0], arriba[2]):
            if (x - arriba[0] < 4) == izquierda:
                c.borrar(x, y)


# ================================================================= ROPA: 5 cortes x 4 colores

COLORES = {
    "hombre": [
        [PIEL_CLARA, PIEL_OSCURA, LOBO, LEOPARDO],                         # taparrabos
        [PIEL_CLARA, PIEL_OSCURA, LOBO, LEOPARDO],                         # túnica de un hombro
        [OSO, PIEL_OSCURA, LOBO, (150, 110, 70)],                          # manto de oso
        [FIBRA, (150, 150, 90), (186, 140, 84), (130, 120, 80)],           # faldellín de fibras
        [PIEL_CLARA, PIEL_OSCURA, LOBO, (196, 186, 170)],                  # abrigo de invierno
    ],
    "mujer": [
        [PIEL_CLARA, PIEL_OSCURA, LOBO, LEOPARDO],                         # vestido de un hombro
        [PIEL_CLARA, PIEL_OSCURA, LOBO, LEOPARDO],                         # top y falda con flecos
        [FIBRA, (150, 150, 90), (186, 140, 84), (130, 120, 80)],           # falda de fibras
        [PIEL_CLARA, PIEL_OSCURA, (160, 120, 90), (140, 100, 70)],         # vestido largo con cuentas
        [PIEL_CLARA, PIEL_OSCURA, LOBO, (196, 186, 170)],                  # abrigo de invierno
    ],
}
NOMBRES_ROPA = {
    "hombre": ["taparrabos", "túnica de un hombro", "manto de piel de oso", "faldellín de fibras",
               "abrigo de pieles"],
    "mujer": ["vestido de un hombro", "top y falda con flecos", "falda de fibras", "vestido largo con cuentas",
              "abrigo de pieles"],
}
NOMBRES_COLOR = {
    "hombre": [["de ciervo", "de bisonte", "de lobo", "de leopardo"], ["de ciervo", "de bisonte", "de lobo", "de leopardo"],
               ["oscuro", "pardo", "gris", "claro"], ["de paja", "de junco", "de rafia", "de esparto"],
               ["de ciervo", "de bisonte", "de lobo", "blanco"]],
    "mujer": [["de ciervo", "de bisonte", "de lobo", "de leopardo"], ["de ciervo", "de bisonte", "de lobo", "de leopardo"],
              ["de paja", "de junco", "de rafia", "de esparto"], ["claro", "oscuro", "rojizo", "pardo"],
              ["de ciervo", "de bisonte", "de lobo", "blanco"]],
}


def ropa(sexo, n):
    mujer = sexo == "mujer"
    corte, color = divmod(n, 4)
    tela = COLORES[sexo][corte][color]
    manchas = "manchas" if tela == LEOPARDO else None
    c = Lienzo(3 if mujer else 4, semilla=900 + n + (40 if mujer else 0))
    fx, fy, _, _ = c.cara("cuerpo", "frente")

    if not mujer and corte == 0:        # taparrabos: torso al aire, cinturón de cuerda y abarcas
        _pelaje(c, "cuerpo", tela, filas=(9, 12), manchas=manchas)
        c.fila("cuerpo", 9, CUERDA)
        for pierna in PIERNAS:
            _pelaje(c, pierna, tela, filas=(0, 4), manchas=manchas)
        for capa in CAPAS_PIERNAS:       # el faldón cae por delante y por detrás
            _pelaje(c, capa, tela, filas=(0, 5), solo=("frente", "detras"), manchas=manchas)
            _flecos(c, capa, 4, tela, ("frente", "detras"))
        _pies(c)
    elif not mujer and corte == 1:      # túnica de un hombro hasta el muslo, polainas atadas
        _un_hombro(c, "cuerpo", tela, 4, izquierda=True, manchas=manchas)
        c.fila("cuerpo", 9, CUERDA)
        for capa in CAPAS_PIERNAS:
            _pelaje(c, capa, tela, filas=(0, 5), solo=LADOS, manchas=manchas)
            _flecos(c, capa, 4, tela)
            _atado(c, capa, oscuro(tela, 0.8), 7, 11)
        for pierna in PIERNAS:
            _pelaje(c, pierna, tela, filas=(0, 4), manchas=manchas)
        _pies(c)
    elif not mujer and corte == 2:      # manto de oso sobre los hombros, chaleco y calzas de piel
        _pelaje(c, "cuerpo", oscuro(tela, 1.25), fuerza=PIEL)
        c.rect((fx + 3, fy, fx + 5, fy + 9), (0, 0, 0))   # pecho al aire por el centro
        for y in range(fy, fy + 9):
            for x in (fx + 3, fx + 4):
                c.borrar(x, y)
        c.fila("cuerpo", 9, CUERDA, solo=None)
        _pelaje(c, "cuerpo_capa", tela, filas=(0, 4), fuerza=0.2)              # el manto, con pelo largo
        _pelaje(c, "cuerpo_capa", tela, filas=(0, 11), solo=("detras",), fuerza=0.2)
        _flecos(c, "cuerpo_capa", 10, tela, ("detras",))
        for capa in ("brazo_d_capa", "brazo_i_capa"):
            _pelaje(c, capa, tela, filas=(0, 3), fuerza=0.2)
        for pierna in PIERNAS:
            _pelaje(c, pierna, oscuro(tela, 1.2))
        _pies(c, OSO)
    elif not mujer and corte == 3:      # faldellín de fibras colgantes, bandolera de cuero
        _pelaje(c, "cuerpo", tela, filas=(9, 12), manchas="fibras", fuerza=0.1)
        for pierna in PIERNAS:
            _pelaje(c, pierna, tela, filas=(0, 3), manchas="fibras", fuerza=0.1)
        for capa in CAPAS_PIERNAS:
            _pelaje(c, capa, tela, filas=(0, 6), solo=LADOS, manchas="fibras", fuerza=0.1)
            _flecos(c, capa, 5, tela)
        c.fila("cuerpo", 9, CUERDA)
        _bandolera(c, PIEL_OSCURA)
        _pies(c)
    elif not mujer:                     # abrigo de pieles con mangas, borde de pelo blanco y polainas
        _pelaje(c, "cuerpo", tela)
        for brazo in BRAZOS:
            _pelaje(c, brazo, tela, filas=(0, 11))
            c.fila(brazo, 10, PELO_BLANCO)
        c.fila("cuerpo", 0, PELO_BLANCO)
        for y in range(fy + 1, fy + 12):                       # cierre cruzado
            c.punto(fx + 4, y, oscuro(tela, 0.7))
        c.fila("cuerpo", 9, CUERDA, solo=None)
        for capa in CAPAS_PIERNAS:
            _pelaje(c, capa, tela, filas=(0, 4), solo=LADOS)
            c.fila(capa, 3, PELO_BLANCO)
        for pierna in PIERNAS:
            _pelaje(c, pierna, oscuro(tela, 0.85))
        for capa in CAPAS_PIERNAS:
            _atado(c, capa, oscuro(tela, 0.75), 6, 11)
        _pies(c)
    elif corte == 0:                    # vestido de piel cruzado, con una tira de pelo de un hombro a la cadera
        _pelaje(c, "cuerpo", tela, manchas=manchas)
        for i in range(7):              # la tira de pelo en diagonal (por delante y por detrás)
            col = 7 - i
            c.punto(fx + col, fy + i, PELO_BLANCO)
            c.punto(fx + max(0, col - 1), fy + i, oscuro(PELO_BLANCO, 0.85))
            bx, by, _, _ = c.cara("cuerpo", "detras")
            c.punto(bx + i, by + i, PELO_BLANCO)
        c.fila("cuerpo", 6, CUERDA)
        for pierna in PIERNAS:
            _pelaje(c, pierna, tela, filas=(0, 7), manchas=manchas)
        for capa in CAPAS_PIERNAS:
            _pelaje(c, capa, tela, filas=(0, 8), solo=LADOS, manchas=manchas)
            _flecos(c, capa, 7, tela)
        _pies(c)
    elif corte == 1:                    # top de piel atado y falda corta con flecos
        _pelaje(c, "cuerpo", tela, filas=(0, 6), manchas=manchas)
        for y in range(fy, fy + 2):     # escote atado con cuerda
            c.punto(fx + 3, y, oscuro(tela, 0.7))
            c.punto(fx + 4, y, CUERDA)
        c.fila("cuerpo", 5, oscuro(tela, 0.8))
        _pelaje(c, "cuerpo", tela, filas=(9, 12), manchas=manchas)
        c.fila("cuerpo", 9, CUERDA)
        for pierna in PIERNAS:
            _pelaje(c, pierna, tela, filas=(0, 4), manchas=manchas)
        for capa in CAPAS_PIERNAS:
            _pelaje(c, capa, tela, filas=(0, 6), solo=LADOS, manchas=manchas)
            _flecos(c, capa, 5, tela)
        _pies(c, PIEL_CLARA)
    elif corte == 2:                    # top cruzado de piel y falda larga de fibras
        _pelaje(c, "cuerpo", PIEL_OSCURA, filas=(0, 6))
        c.fila("cuerpo", 5, oscuro(PIEL_OSCURA, 0.75))
        for fila in range(6):
            c.punto(fx + 3 + (fila % 2), fy + fila, CUERDA)
        _pelaje(c, "cuerpo", tela, filas=(9, 12), manchas="fibras", fuerza=0.1)
        c.fila("cuerpo", 9, CUERDA)
        for pierna in PIERNAS:
            _pelaje(c, pierna, tela, filas=(0, 9), manchas="fibras", fuerza=0.1)
        for capa in CAPAS_PIERNAS:
            _pelaje(c, capa, tela, filas=(0, 10), solo=LADOS, manchas="fibras", fuerza=0.1)
            _flecos(c, capa, 9, tela)
        _pies(c)
    elif corte == 3:                    # vestido largo de piel con cinturón de cuerda y cuentas de hueso
        _pelaje(c, "cuerpo", tela)
        for brazo in BRAZOS:
            _pelaje(c, brazo, tela, filas=(0, 4))
            _flecos(c, brazo, 3, tela)
        for col in range(1, 7):
            c.punto(fx + col, fy + (0 if col in (1, 6) else 1), HUESO if col % 2 else OCRE)
        c.fila("cuerpo", 7, CUERDA, solo=None)
        c.punto(fx + 2, fy + 8, HUESO)
        c.punto(fx + 2, fy + 9, OCRE)
        for pierna in PIERNAS:
            _pelaje(c, pierna, tela, filas=(0, 10))
        for capa in CAPAS_PIERNAS:
            _pelaje(c, capa, oscuro(tela, 0.95), filas=(0, 11), solo=LADOS)
            _flecos(c, capa, 10, tela)
        _pies(c)
    else:                               # abrigo de pieles largo con cuello de pelo
        _pelaje(c, "cuerpo", tela)
        for brazo in BRAZOS:
            _pelaje(c, brazo, tela, filas=(0, 11))
            c.fila(brazo, 10, PELO_BLANCO)
        _pelaje(c, "cuerpo_capa", PELO_BLANCO, filas=(0, 2), fuerza=0.15)
        for y in range(fy + 2, fy + 12):
            c.punto(fx + 4, y, oscuro(tela, 0.7))
        c.fila("cuerpo", 8, CUERDA, solo=None)
        for pierna in PIERNAS:
            _pelaje(c, pierna, tela, filas=(0, 10))
        for capa in CAPAS_PIERNAS:
            _pelaje(c, capa, tela, filas=(0, 10), solo=LADOS)
            c.fila(capa, 9, PELO_BLANCO)
        _pies(c)
    return c


def nombre_ropa(sexo, n):
    corte, color = divmod(n, 4)
    return NOMBRES_ROPA[sexo][corte] + " " + NOMBRES_COLOR[sexo][corte][color]


# ================================================================= PEINADOS
# (nombre, lados, nuca, flequillo, (filas de espalda, lados de espalda), piezas 3D, solo adultos, especial)
# Especiales nuevos: "rastas" (mechones gruesos separados), "enredado" (pelo hecho una maraña, con huecos),
# "cresta" (solo una franja por el centro), "cuchillo" (cortado a trasquilones).
PEINADOS = {
    "mujer": [
        ("melena salvaje", 8, 8, [8, 5, 3, 4, 2, 4, 5, 8], (7, 2), MELENA, False, "enredado"),
        ("melena enredada", 8, 8, [8, 4, 4, 2, 3, 4, 3, 8], (6, 2), MELENA, False, "enredado"),
        ("rastas largas", 8, 8, [8, 5, 3, 5, 3, 5, 3, 8], (9, 2), MELENA, False, "rastas"),
        ("trenza atada con cordón", 6, 8, [6, 3, 4, 2, 3, 4, 3, 6], (0, 0), TRENZA_ATRAS, False, None),
        ("dos trenzas", 6, 8, [7, 3, 4, 3, 3, 4, 3, 7], (0, 0), TRENZAS, False, None),
        ("moño con hueso", 5, 8, [5, 3, 2, 2, 1, 2, 3, 5], (0, 0), MONO, False, "hueso"),
        ("coleta atada con cuerda", 6, 8, [6, 4, 3, 2, 3, 3, 4, 6], (0, 0), COLETA, False, None),
        ("flequillo desigual", 8, 8, [8, 5, 3, 5, 2, 4, 3, 8], (5, 1), FLEQUILLO | MELENA, False, "cuchillo"),
        ("media melena enredada", 7, 8, [7, 4, 2, 4, 3, 2, 4, 7], (0, 0), FLEQUILLO, False, "enredado"),
        ("rizada salvaje", 8, 8, [8, 4, 3, 4, 3, 4, 3, 8], (6, 2), MELENA, False, "rizado"),
        ("larguísima", 8, 8, [8, 4, 3, 3, 3, 3, 4, 8], (12, 4), MELENA, False, None),
        ("flequillo a cuchillo", 7, 8, [8, 4, 4, 4, 4, 4, 4, 8], (0, 0), FLEQUILLO, False, "cuchillo"),
        ("trenzas finas", 7, 8, [7, 3, 2, 2, 2, 2, 3, 7], (0, 0), TRENZAS, False, "rastas"),
        ("rastas recogidas", 5, 8, [5, 3, 3, 2, 2, 3, 3, 5], (0, 0), MONO, False, "rastas"),
        ("melena con mechones trenzados", 8, 8, [8, 4, 3, 2, 2, 3, 4, 8], (6, 2), MELENA, False, "trenzados"),
        ("corta desgreñada", 4, 6, [6, 4, 2, 4, 3, 2, 4, 6], (0, 0), FLEQUILLO, False, "enredado"),
        ("coleta alta", 5, 8, [5, 3, 2, 2, 2, 2, 3, 5], (0, 0), COLETA, False, None),
        ("larga con raya al medio", 8, 8, [8, 3, 2, 1, 1, 2, 3, 8], (7, 2), MELENA, False, None),
        ("recogido con cinta de cuero", 6, 8, [6, 3, 3, 2, 2, 3, 3, 6], (0, 0), TRENZA_ATRAS, False, None),
        ("larga ondulada", 8, 8, [8, 4, 3, 4, 3, 4, 3, 8], (8, 3), MELENA, False, "rizado"),
    ],
    "hombre": [
        ("desgreñado largo", 8, 8, [4, 3, 2, 3, 2, 3, 2, 4], (3, 1), MELENA, False, "enredado"),
        ("melena enredada", 8, 8, [5, 3, 2, 3, 3, 2, 3, 5], (4, 1), MELENA, False, "enredado"),
        ("rastas", 8, 8, [5, 3, 1, 3, 1, 3, 1, 5], (5, 1), MELENA, False, "rastas"),
        ("rapado", 4, 7, [1] * 8, (0, 0), 0, False, "rapado"),
        ("corto a trasquilones", 4, 7, [3, 1, 2, 1, 3, 1, 2, 2], (0, 0), 0, False, "cuchillo"),
        ("coleta de guerrero", 5, 8, [2, 2, 1, 1, 1, 1, 2, 2], (0, 0), COLETA, False, None),
        ("moño alto", 4, 8, [1, 1, 0, 0, 0, 0, 1, 1], (0, 0), MONO, False, "hueso"),
        ("rizado salvaje", 6, 7, [3, 3, 2, 3, 2, 3, 2, 3], (1, 0), FLEQUILLO, False, "rizado"),
        ("melena con raya", 8, 8, [5, 2, 1, 0, 0, 1, 2, 5], (3, 0), MELENA, False, None),
        ("trenzas de cazador", 6, 8, [2, 2, 1, 1, 1, 1, 2, 2], (0, 0), TRENZAS, False, None),
        ("calvo con melena", 6, 8, [0] * 8, (3, 0), 0, True, "calvo_largo"),
        ("entradas con melena", 7, 8, [0, 1, 0, 0, 0, 0, 1, 0], (2, 0), MELENA, True, "entradas"),
        ("cresta", 0, 7, [0, 0, 0, 2, 2, 0, 0, 0], (0, 0), 0, False, "cresta"),
        ("rizado corto", 3, 6, [2, 2, 1, 2, 2, 1, 2, 2], (0, 0), 0, False, "rizado"),
        ("flequillo a cuchillo", 5, 7, [3, 3, 3, 3, 3, 3, 3, 3], (0, 0), FLEQUILLO, False, "cuchillo"),
        ("rastas cortas", 5, 7, [3, 2, 1, 2, 1, 2, 1, 3], (0, 0), 0, False, "rastas"),
        ("pelo atado atrás", 6, 8, [3, 2, 1, 1, 1, 1, 2, 3], (0, 0), TRENZA_ATRAS, False, None),
        ("melena hasta los hombros", 8, 8, [4, 2, 2, 1, 2, 2, 2, 4], (2, 0), MELENA, False, None),
        ("rapado con moño", 2, 4, [0] * 8, (0, 0), MONO, False, "rapado_mono"),
        ("larguísimo", 8, 8, [5, 3, 2, 2, 2, 2, 3, 5], (8, 2), MELENA, False, "enredado"),
    ],
}


def _rastas(c):
    """Mechones gruesos de dos píxeles con una raya oscura entre uno y otro."""
    for cual in ("arriba", "derecha", "izquierda", "detras"):
        x0, y0, x1, y1 = c.cara("cabeza", cual)
        for y in range(y0, y1):
            for x in range(x0, x1):
                if c.leer(x, y)[3] and (x - x0) % 3 == 2:
                    c.punto(x, y, G_OSCURO)
                elif c.leer(x, y)[3] and (y + (x - x0) // 3) % 3 == 0:
                    c.punto(x, y, G_LUZ)
    x0, y0, x1, y1 = c.cara("cuerpo_capa", "detras")
    for y in range(y0, y1):
        for x in range(x0, x1):
            if c.leer(x, y)[3] and (x - x0) % 3 == 2:
                c.borrar(x, y)                          # entre rasta y rasta se ve la espalda


def _enredado(c):
    """Maraña: tonos revueltos y algún mechón que se sale por fuera (en la capa exterior)."""
    for cual in ("arriba", "derecha", "izquierda", "detras"):
        x0, y0, x1, y1 = c.cara("cabeza", cual)
        for y in range(y0, y1):
            for x in range(x0, x1):
                if not c.leer(x, y)[3]:
                    continue
                r = ruido(x, y, 51 + c.semilla)
                if r < 0.2:
                    c.punto(x, y, G_OSCURO)
                elif r > 0.85:
                    c.punto(x, y, G_BRILLO)


def _cuchillo(c):
    """Cortado a trasquilones: puntas de distinto largo en los lados y la nuca."""
    for cual in ("derecha", "izquierda", "detras"):
        x0, y0, x1, y1 = c.cara("cabeza", cual)
        for x in range(x0, x1):
            ultima = None
            for y in range(y0, y1):
                if c.leer(x, y)[3]:
                    ultima = y
            if ultima is not None and ultima > y0 + 1 and ruido(x, 3, 7 + c.semilla) < 0.5:
                c.borrar(x, ultima)


def _trenzados(c):
    """Mechones trenzados finos sueltos a los lados de la cara (en la capa exterior)."""
    for cual in ("derecha", "izquierda"):
        x0, y0, x1, _ = c.cara("cabeza_capa", cual)
        x = x0 + 5 if cual == "derecha" else x0 + 2
        for y in range(y0 + 2, y0 + 8):
            c.punto(x, y, G_LUZ if y % 2 else G_SOMBRA)


def _hueso(c):
    """Un hueso atravesado en el recogido (no se tiñe: va en blanco roto, el tinte lo deja algo del color del pelo;
    por eso solo se insinúa con brillo)."""
    x0, y0, x1, _ = c.cara("cabeza_capa", "detras")
    for x in range(x0 + 1, x0 + 7):
        c.punto(x, y0 + 1, G_BRILLO)
    c.punto(x0, y0 + 1, G_LUZ)
    c.punto(x0 + 7, y0 + 1, G_LUZ)


def pelo(sexo, n):
    mujer = sexo == "mujer"
    nombre, lados, nuca, fleq, (esp, esp_lados), _, _, especial = PEINADOS[sexo][n]
    c = Lienzo(3 if mujer else 4, semilla=1100 + n * 3 + (50 if mujer else 0))
    if not mujer:
        # Cejas pobladas: en la prehistoria, más gruesas y juntas
        for col in (1, 2, 3, 4, 5, 6):
            cf(c, col, 3, G_OSCURO if col not in (3, 4) else G_SOMBRA)
    if especial in ("rapado", "rapado_mono"):
        for cual, filas in (("arriba", 8), ("derecha", 4 if especial == "rapado" else 2),
                            ("izquierda", 4 if especial == "rapado" else 2), ("detras", 7 if especial == "rapado" else 4)):
            x0, y0, x1, _ = c.cara("cabeza", cual)
            for y in range(y0, y0 + filas):
                for x in range(x0, x1):
                    c.punto(x, y, G_SOMBRA if ruido(x, y, 3) < 0.7 else G_OSCURO)
        if especial == "rapado_mono":
            x0, y0, x1, y1 = c.cara("cabeza", "arriba")
            mechones(c, (x0 + 2, y0 + 2, x1 - 2, y1 - 2))
    elif especial == "calvo_largo":
        for lado in ("derecha", "izquierda"):
            x0, y0, x1, _ = c.cara("cabeza", lado)
            mechones(c, (x0, y0 + 2, x1, y0 + lados), False)
        x0, y0, x1, _ = c.cara("cabeza", "detras")
        mechones(c, (x0, y0 + 2, x1, y0 + nuca), False)
    elif especial == "cresta":
        x0, y0, x1, y1 = c.cara("cabeza", "arriba")
        mechones(c, (x0 + 3, y0, x0 + 5, y1))
        x0, y0, x1, _ = c.cara("cabeza", "detras")
        mechones(c, (x0 + 3, y0, x0 + 5, y0 + nuca), False)
        x0, y0, x1, y1 = c.cara("cabeza_capa", "arriba")          # la cresta, de pie en la capa de fuera
        for y in range(y0, y1):
            c.punto(x0 + 3, y, G_LUZ)
            c.punto(x0 + 4, y, G)
        for cual in ("derecha", "izquierda"):                     # sienes rapadas
            x0, y0, x1, _ = c.cara("cabeza", cual)
            for y in range(y0, y0 + 4):
                for x in range(x0, x1):
                    if ruido(x, y, 9) < 0.5:
                        c.punto(x, y, G_SOMBRA)
    else:
        cabeza(c, lados, nuca)
        if especial == "entradas":
            x0, y0, x1, y1 = c.cara("cabeza", "arriba")
            for x in (x0, x0 + 1, x1 - 2, x1 - 1):
                for y in range(y1 - 3, y1):
                    c.borrar(x, y)
    if especial not in ("rapado", "rapado_mono", "calvo_largo", "cresta"):
        (flequillo_profundo if mujer else flequillo)(c, fleq)
    elif especial == "cresta":
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
    elif especial == "enredado":
        _enredado(c)
    elif especial == "cuchillo":
        _cuchillo(c)
    elif especial == "trenzados":
        _trenzados(c)
    if especial not in ("rapado", "rapado_mono", "cresta"):
        volumen(c, 0.2 if especial == "enredado" else (0.3 if mujer else 0.45))
    if especial == "enredado":
        # Pelos que se salen por arriba y por los lados
        for cual in ("arriba", "derecha", "izquierda"):
            x0, y0, x1, y1 = c.cara("cabeza_capa", cual)
            for y in range(y0, y1):
                for x in range(x0, x1):
                    if ruido(x, y, 61 + c.semilla) < 0.08:
                        c.punto(x, y, G_LUZ)
    if especial == "hueso":
        _hueso(c)
    return c


# ================================================================= BARBAS (1..20; 0 = ninguna)

BARBAS = [
    "salvaje", "enredada", "larga", "muy larga", "de tres días", "corta", "bigote grueso", "con cuentas",
    "trenzada", "bifurcada", "perilla", "patillas pobladas", "rala", "de chivo", "hasta el pecho", "de collar",
    "corta con bigote", "hirsuta", "con anilla de hueso", "rizada",
]


def _maraña(c, filas_pecho=0, ancho=6):
    """Barba de pelo revuelto que llega a la cara y, si se pide, al pecho."""
    _barba_base(c, 4)
    _bigote(c, "grueso")
    for col in range(8):
        for fila in (5, 6, 7):
            if fila >= 6 or col in (0, 1, 6, 7):
                if ruido(col, fila, 17 + c.semilla) < 0.3:
                    cf(c, col, fila, G_OSCURO)
    if filas_pecho:
        _pecho(c, filas_pecho, ancho)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for fila in range(filas_pecho):
            for col in range(8):
                if c.leer(x0 + col, y0 + fila)[3] and ruido(col, fila, 23 + c.semilla) < 0.25:
                    c.punto(x0 + col, y0 + fila, G_OSCURO)


def barba(n):
    c = Lienzo(semilla=1400 + n)
    nombre = BARBAS[n - 1]
    if nombre == "salvaje":
        _maraña(c, 3)
    elif nombre == "enredada":
        _maraña(c, 2, 4)
    elif nombre == "larga":
        _barba_base(c)
        _bigote(c, "grueso")
        _pecho(c, 4)
    elif nombre == "muy larga":
        _barba_base(c, 4)
        _bigote(c, "grueso")
        _pecho(c, 7, 6)
    elif nombre == "de tres días":
        for col in range(8):
            if col not in (3, 4):
                cf(c, col, 7, G_SOMBRA)
        for col in (0, 7):
            cf(c, col, 6, G_SOMBRA)
            cf(c, col, 5, G_SOMBRA)
    elif nombre == "corta":
        _barba_base(c, 2, G_SOMBRA)
    elif nombre == "bigote grueso":
        _bigote(c, "grueso")
        _bigote(c, "caido")
    elif nombre == "con cuentas":
        _barba_base(c)
        _bigote(c, "grueso")
        _pecho(c, 3)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.punto(x0 + 3, y0 + 2, G_BRILLO)
        c.punto(x0 + 4, y0 + 1, G_BRILLO)
    elif nombre == "trenzada":
        _barba_base(c)
        _bigote(c, "caido")
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for fila in range(6):
            c.punto(x0 + 3, y0 + fila, G_SOMBRA if fila % 2 else G_LUZ)
            c.punto(x0 + 4, y0 + fila, G_LUZ if fila % 2 else G_SOMBRA)
    elif nombre == "bifurcada":
        _barba_base(c)
        _bigote(c, "grueso")
        _pecho(c, 4, 6, hueco=True)
    elif nombre == "perilla":
        _bigote(c, "fino")
        _perilla(c, larga=True)
    elif nombre == "patillas pobladas":
        for col in (0, 1, 6, 7):
            for fila in range(3, 8):
                if not (col in (1, 6) and fila < 5):
                    cf(c, col, fila, G_SOMBRA if col in (0, 7) else G)
        for lado in ("derecha", "izquierda"):
            x0, y0, x1, _ = c.cara("cabeza", lado)
            mechones(c, (x0, y0 + 3, x1, y0 + 8), False)
    elif nombre == "rala":
        for col in range(8):
            for fila in (5, 6, 7):
                if ruido(col, fila, 9) < (0.5 if fila == 7 else 0.25) and not (fila < 7 and 2 <= col <= 5):
                    cf(c, col, fila, G_SOMBRA)
    elif nombre == "de chivo":
        for col in (2, 3, 4, 5):
            cf(c, col, 7, G)
        _perilla(c, larga=True)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for fila in range(2, 4):
            c.punto(x0 + 3, y0 + fila, G_SOMBRA)
            c.punto(x0 + 4, y0 + fila, G)
    elif nombre == "hasta el pecho":
        _barba_base(c, 4)
        _bigote(c, "grueso")
        _pecho(c, 5, 6)
    elif nombre == "de collar":
        for col in range(8):
            cf(c, col, 7, G_SOMBRA)
        for col in (0, 7):
            cf(c, col, 6, G_SOMBRA)
            cf(c, col, 5, G_SOMBRA)
        mechones(c, c.cara("cabeza", "abajo"), False)
    elif nombre == "corta con bigote":
        _barba_base(c, 2, G_SOMBRA)
        _bigote(c, "grueso")
    elif nombre == "hirsuta":
        _maraña(c, 1, 6)
        for col in (0, 7):
            cf(c, col, 4, G_SOMBRA)
    elif nombre == "con anilla de hueso":
        _barba_base(c)
        _bigote(c, "grueso")
        _pecho(c, 4, 2)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.punto(x0 + 3, y0 + 2, G_BRILLO)
        c.punto(x0 + 4, y0 + 2, G_BRILLO)
    else:                                                          # rizada
        _barba_base(c)
        _bigote(c, "grueso")
        _pecho(c, 2)
        for col in range(8):
            for fila in (5, 6, 7):
                if (col + fila) % 2 == 0 and c.leer(8 + col, 8 + fila)[3]:
                    cf(c, col, fila, G_LUZ)
    return c


# ================================================================= COMPLEMENTOS (1..20; 0 = ninguno). No se tiñen.

def _collar_de(c, cuentas, cuerda=CUERDA, colgante=None, filas=2):
    """Collar en la capa exterior del pecho: una cuerda que baja en V con cuentas alternas."""
    x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
    puntos = [(0, 0), (1, 0), (2, 1), (5, 1), (6, 0), (7, 0)] if filas == 2 else [(1, 0), (2, 0), (5, 0), (6, 0)]
    for i, (col, fila) in enumerate(puntos):
        c.punto(x0 + col, y0 + fila, cuentas[i % len(cuentas)] if cuentas else cuerda)
    for col in (3, 4):
        c.punto(x0 + col, y0 + filas, cuentas[(col + 1) % len(cuentas)] if cuentas else cuerda)
    if colgante:
        c.punto(x0 + 3, y0 + filas + 1, colgante)
        c.punto(x0 + 4, y0 + filas + 1, colgante)
        c.punto(x0 + 3, y0 + filas + 2, oscuro(colgante, 0.85))


def _pluma(c, x, y, color, largo=4, hacia_arriba=True):
    for i in range(largo):
        c.punto(x, y - i if hacia_arriba else y + i, color if i % 2 else oscuro(color, 0.8))
    c.punto(x, y - largo if hacia_arriba else y + largo, (40, 36, 34))


COMPLEMENTOS = {
    "mujer": ["collar de dientes", "collar de conchas", "collar de cuentas de hueso", "pluma en el pelo",
              "corona de flores", "cinta de cuero en la frente", "hueso en el pelo", "pendientes de hueso",
              "brazaletes de cuero", "manto de piel", "cesta a la espalda", "zurrón de piel", "cinturón de conchas",
              "diadema de plumas", "cinta con cuentas", "capucha de lobo", "collar de piedras", "pulseras de cuentas",
              "colgante de ámbar", "flor en el pelo"],
    "hombre": ["collar de dientes", "colmillo colgante", "pluma en el pelo", "dos plumas", "cinta de cuero",
               "capucha de lobo", "manto de piel de oso", "bandolera de cuero", "zurrón", "brazaletes de cuero",
               "cuchillo de sílex al cinto", "collar de garras", "pendiente de hueso", "hueso en la nariz",
               "carcaj a la espalda", "cuerno colgado", "polainas de piel", "collar de conchas",
               "diadema de plumas", "cinta con cuentas"],
}


def complemento(sexo, n):
    mujer = sexo == "mujer"
    c = Lienzo(3 if mujer else 4, semilla=1600 + n + (40 if mujer else 0))
    nombre = COMPLEMENTOS[sexo][n - 1]
    fx, fy, _, _ = c.cara("cabeza_capa", "frente")
    if nombre == "collar de dientes":
        _collar_de(c, [DIENTE, CUERDA])
    elif nombre == "collar de conchas":
        _collar_de(c, [CONCHA, (210, 170, 150)])
    elif nombre == "collar de cuentas de hueso":
        _collar_de(c, [HUESO, OCRE, HUESO], filas=2)
    elif nombre == "collar de piedras":
        _collar_de(c, [(90, 140, 130), (150, 80, 60), (60, 60, 70)])
    elif nombre == "collar de garras":
        _collar_de(c, [CARBON, CUERDA, (70, 60, 50)], colgante=CARBON)
    elif nombre == "colmillo colgante":
        _collar_de(c, None, colgante=DIENTE)
    elif nombre == "colgante de ámbar":
        _collar_de(c, None, colgante=AMBAR)
    elif nombre in ("pluma en el pelo", "dos plumas"):
        x0, y0, _, _ = c.cara("cabeza_capa", "izquierda")
        _pluma(c, x0 + 6, y0 + 4, (230, 226, 210))
        if nombre == "dos plumas":
            _pluma(c, x0 + 4, y0 + 4, (170, 70, 50), 5)
        c.punto(x0 + 6, y0 + 5, CUERDA)
    elif nombre == "diadema de plumas":
        _banda(c, 1, CUERDA)
        for i, col in enumerate((0, 2, 4, 6)):
            _pluma(c, fx + col + 1, fy + 0, [(230, 226, 210), (170, 70, 50), (60, 70, 90), (230, 226, 210)][i], 1)
        x0, y0, x1, _ = c.cara("cabeza_capa", "arriba")
        for x in range(x0, x1, 2):
            c.punto(x, y0 + 7, (230, 226, 210))
            c.punto(x, y0 + 6, (170, 70, 50))
    elif nombre in ("cinta de cuero en la frente", "cinta de cuero"):
        _banda(c, 1 if mujer else 2, PIEL_OSCURA)
    elif nombre == "cinta con cuentas":
        _banda(c, 1 if mujer else 2, PIEL_OSCURA)
        for col in (1, 4, 6):
            c.punto(fx + col, fy + (1 if mujer else 2), [HUESO, OCRE, CONCHA][col % 3])
    elif nombre == "corona de flores":
        _banda(c, 1, (80, 130, 60))
        for i, col in enumerate((0, 2, 4, 6)):
            c.punto(fx + col + 1, fy + 1, [(230, 210, 90), (250, 250, 250), (220, 110, 160), (250, 230, 120)][i])
    elif nombre == "flor en el pelo":
        _flor(c, fx + 6, fy + 1, (250, 220, 90), (180, 110, 40))
    elif nombre == "hueso en el pelo":
        x0, y0, _, _ = c.cara("cabeza_capa", "detras")
        for x in range(x0, x0 + 8):
            c.punto(x, y0 + 2, HUESO)
        c.punto(x0, y0 + 1, HUESO)
        c.punto(x0 + 7, y0 + 3, HUESO)
    elif nombre in ("pendientes de hueso", "pendiente de hueso"):
        for cual in (("derecha", "izquierda") if mujer else ("izquierda",)):
            x0, y0, _, _ = c.cara("cabeza_capa", cual)
            c.punto(x0 + 4, y0 + 6, HUESO)
            c.punto(x0 + 4, y0 + 7, HUESO)
    elif nombre == "hueso en la nariz":
        fila = 5
        for col in (2, 3, 4, 5):
            c.punto(fx + col, fy + fila, HUESO)
    elif nombre in ("brazaletes de cuero",):
        for capa in ("brazo_d_capa", "brazo_i_capa"):
            c.caja(capa, PIEL_OSCURA, filas=(7, 10), fuerza=0.08)
            c.fila(capa, 8, CUERDA)
    elif nombre == "pulseras de cuentas":
        for capa in ("brazo_d_capa", "brazo_i_capa"):
            for fila, color in ((9, OCRE), (10, HUESO)):
                c.fila(capa, fila, color)
    elif nombre == "manto de piel":
        _hombros(c, LOBO, 3, 2)
        c.caja("cuerpo_capa", LOBO, filas=(0, 7), solo=("detras",), fuerza=0.15)
    elif nombre == "manto de piel de oso":
        _hombros(c, OSO, 3, 3)
        c.caja("cuerpo_capa", OSO, filas=(0, 10), solo=("detras",), fuerza=0.2)
        _gorro(c, OSO, 2, 1, fuerza=0.2)
        x0, y0, _, _ = c.cara("cabeza_capa", "frente")
        c.punto(x0 + 1, y0, oscuro(OSO, 0.6))
        c.punto(x0 + 6, y0, oscuro(OSO, 0.6))
    elif nombre == "capucha de lobo":
        _gorro(c, LOBO, 6 if mujer else 5, 2, fuerza=0.15)
        x0, y0, _, _ = c.cara("cabeza_capa", "frente")
        for col in (1, 6):                                       # ojos y orejas del lobo
            c.punto(x0 + col, y0 + 1, CARBON)
        c.punto(x0, y0, oscuro(LOBO, 0.7))
        c.punto(x0 + 7, y0, oscuro(LOBO, 0.7))
        _hombros(c, LOBO, 2, 1)
        c.caja("cuerpo_capa", LOBO, filas=(0, 6), solo=("detras",), fuerza=0.15)
    elif nombre == "cesta a la espalda":
        x0, y0, _, _ = c.cara("cuerpo_capa", "detras")
        for y in range(y0, y0 + 9):
            for x in range(x0 + 1, x0 + 7):
                c.punto(x, y, FIBRA if (x + y) % 2 else oscuro(FIBRA, 0.75))
        for cual in ("derecha", "izquierda"):
            x0, y0, x1, _ = c.cara("cuerpo_capa", cual)
            c.punto(x0 + 1, y0, CUERDA)
            c.punto(x0 + 2, y0 + 1, CUERDA)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.punto(x0 + 1, y0, CUERDA)
        c.punto(x0 + 6, y0, CUERDA)
    elif nombre in ("zurrón de piel", "zurrón"):
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for i in range(9):
            c.punto(x0 + i * 7 // 8, y0 + i, oscuro(PIEL_OSCURA, 0.8))
        c.rect((x0 + 5, y0 + 8, x0 + 8, y0 + 11), PIEL_CLARA, 0.1)
        c.punto(x0 + 6, y0 + 10, CUERDA)
    elif nombre == "bandolera de cuero":
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for i in range(11):
            c.punto(x0 + i * 7 // 10, y0 + i, PIEL_OSCURA)
            c.punto(x0 + min(7, i * 7 // 10 + 1), y0 + i, oscuro(PIEL_OSCURA, 0.8))
        x0, y0, _, _ = c.cara("cuerpo_capa", "detras")
        for i in range(11):
            c.punto(x0 + 7 - i * 7 // 10, y0 + i, PIEL_OSCURA)
    elif nombre == "cinturón de conchas":
        c.fila("cuerpo_capa", 9, CUERDA, solo=None)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for col in range(0, 8, 2):
            c.punto(x0 + col, y0 + 9, CONCHA)
            c.punto(x0 + col, y0 + 10, oscuro(CONCHA, 0.85))
    elif nombre == "cuchillo de sílex al cinto":
        c.fila("cuerpo_capa", 9, PIEL_OSCURA, solo=None)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        c.punto(x0 + 6, y0 + 9, (120, 100, 70))
        c.punto(x0 + 6, y0 + 10, (90, 90, 96))
        c.punto(x0 + 6, y0 + 11, (70, 70, 78))
    elif nombre == "carcaj a la espalda":
        x0, y0, _, _ = c.cara("cuerpo_capa", "detras")
        for i in range(10):
            c.punto(x0 + 1 + i * 5 // 9, y0 + 10 - i, PIEL_OSCURA)
            c.punto(x0 + 2 + i * 5 // 9, y0 + 10 - i, oscuro(PIEL_OSCURA, 0.8))
        for i, color in enumerate(((230, 226, 210), (170, 70, 50), (230, 226, 210))):
            c.punto(x0 + 6 + (i % 2), y0 + i // 2, color)
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for i in range(10):
            c.punto(x0 + 7 - i * 7 // 9, y0 + i, PIEL_OSCURA)
    elif nombre == "cuerno colgado":
        x0, y0, _, _ = c.cara("cuerpo_capa", "frente")
        for i in range(8):
            c.punto(x0 + 7 - i, y0 + i, CUERDA)
        c.rect((x0 + 1, y0 + 8, x0 + 3, y0 + 10), (230, 220, 190))
        c.punto(x0, y0 + 10, (180, 170, 140))
    elif nombre == "polainas de piel":
        for capa in CAPAS_PIERNAS:
            c.caja(capa, LOBO, filas=(5, 11), solo=LADOS, fuerza=0.15)
            c.fila(capa, 5, CUERDA)
            c.fila(capa, 8, CUERDA)
    return c


# ================================================================= RASGOS DE CARA (1..20; 0 = ninguno)
# Pinturas, tatuajes y marcas de la vida al aire libre.

MARCAS = ["ocre en las mejillas", "rayas de guerra", "puntos blancos", "antifaz negro", "tatuaje en la barbilla",
          "barro en la cara", "pintura azul", "huella de mano", "zarpazo", "ceniza en la frente", "tierra en la cara",
          "pecas", "cicatriz en la mejilla", "ojo morado", "quemado por el sol", "lunar", "ojeras",
          "tatuaje en la frente", "escarificación", "franja de ocre"]

AZUL = (70, 100, 160)
BLANCO_PINTURA = (236, 232, 222)
CICATRIZ, LUNAR, OJERA = (236, 196, 186), (70, 44, 34), (120, 92, 110)


def marca(sexo, n):
    mujer = sexo == "mujer"
    c = Lienzo(3 if mujer else 4, semilla=1700 + n)
    nombre = MARCAS[n - 1]
    ojo = 5 if mujer else 4
    mejilla = 7 if mujer else 5
    if nombre == "ocre en las mejillas":
        for x in ((0, 1, 6, 7) if not mujer else (0, 1, 6, 7)):
            cf(c, x, mejilla, OCRE)
        if not mujer:
            cf(c, 0, mejilla + 1, OCRE)
            cf(c, 7, mejilla + 1, OCRE)
    elif nombre == "rayas de guerra":
        for x in (0, 7):
            for y in range(ojo + 1, ojo + 3):
                cf(c, x, y, OCRE)
        for x in (1, 6):
            for y in range(ojo + 1, ojo + 3):
                cf(c, x, y, CARBON)
    elif nombre == "puntos blancos":
        for x, y in ((0, ojo + 1), (1, ojo + 2), (6, ojo + 2), (7, ojo + 1), (3, 1), (4, 1)):
            cf(c, x, min(7, y), BLANCO_PINTURA)
    elif nombre == "antifaz negro":
        for x in range(8):
            y = ojo - 1 if mujer else ojo - 1
            if x not in (3, 4):
                cf(c, x, y, CARBON)
            if x in (0, 3, 4, 7):
                cf(c, x, ojo if not mujer else ojo, CARBON)
    elif nombre == "tatuaje en la barbilla":
        for x in (2, 3, 4, 5):
            if mujer:
                cf(c, x, 7, AZUL if x in (3, 4) else oscuro(AZUL, 0.8))
            else:
                cf(c, x, 7, AZUL)
    elif nombre == "barro en la cara":
        for x in range(8):
            for y in range(1, 8):
                if ruido(x, y, 41) < 0.22 and not (y in (ojo, ojo + (1 if mujer else 0)) and x in (1, 2, 5, 6)):
                    cf(c, x, y, (132, 104, 74))
    elif nombre == "pintura azul":
        for x in range(8):
            if x not in (1, 2, 5, 6) or mujer:
                cf(c, x, ojo - (2 if mujer else 1), AZUL)
        for x in (0, 7):
            cf(c, x, ojo, AZUL)
            cf(c, x, ojo + 1, AZUL)
    elif nombre == "huella de mano":
        for x, y in ((4, mejilla - 1), (5, mejilla - 1), (6, mejilla - 1), (5, mejilla), (6, mejilla), (7, mejilla - 2),
                     (4, mejilla - 2), (6, mejilla - 3) if not mujer else (6, mejilla - 2)):
            cf(c, x, y, OCRE)
    elif nombre == "zarpazo":
        for i, x in enumerate((5, 6, 7)):
            for y in range(1 + i, 4 + i):
                cf(c, x, y, CICATRIZ)
    elif nombre == "ceniza en la frente":
        for x in range(1, 7):
            cf(c, x, 1, (110, 106, 104) if x % 2 else (80, 76, 74))
        cf(c, 3, 2, (96, 92, 90))
        cf(c, 4, 2, (96, 92, 90))
    elif nombre == "tierra en la cara":
        for x, y in ((0, mejilla), (1, mejilla), (6, mejilla - 1), (7, 3), (2, 2)):
            cf(c, x, y, (122, 94, 64))
    elif nombre == "pecas":
        for x, y in ([(1, 7), (2, 7), (5, 7), (6, 7), (0, 7), (7, 7)] if mujer else [(1, 5), (2, 6), (5, 6), (6, 5), (0, 6), (7, 6)]):
            cf(c, x, y, (156, 98, 66))
    elif nombre == "cicatriz en la mejilla":
        for y in (mejilla - 1, mejilla):
            cf(c, 6, y, CICATRIZ)
        cf(c, 7, mejilla - 2, CICATRIZ)
    elif nombre == "ojo morado":
        for x, y in ((0, ojo), (0, ojo + 1), (1, ojo - 1), (2, ojo - 1), (1, ojo + (2 if mujer else 1))):
            cf(c, x, y, (110, 70, 120))
    elif nombre == "quemado por el sol":
        for x in range(1, 7):
            cf(c, x, ojo + (2 if mujer else 1), (226, 120, 100) if x in (3, 4) else (220, 140, 120))
    elif nombre == "lunar":
        cf(c, 6 if not mujer else 7, mejilla, LUNAR)
    elif nombre == "ojeras":
        for x in (1, 2, 5, 6):
            cf(c, x, ojo + (2 if mujer else 1), OJERA)
    elif nombre == "tatuaje en la frente":
        for x, y in ((3, 0), (4, 0), (2, 1), (5, 1), (3, 2), (4, 2)):
            cf(c, x, y, AZUL)
    elif nombre == "escarificación":
        for x in (0, 1, 6, 7):
            cf(c, x, mejilla - 1, CICATRIZ)
        for x in (0, 7):
            cf(c, x, mejilla, CICATRIZ)
    else:                                                          # franja de ocre de oreja a oreja
        for x in range(8):
            cf(c, x, ojo + (2 if mujer else 1), OCRE_AMARILLO if x not in (3, 4) else OCRE)
    return c


NOMBRES_MARCA = {"hombre": MARCAS, "mujer": MARCAS}
