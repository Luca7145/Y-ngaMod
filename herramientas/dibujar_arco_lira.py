"""Bocetos (32x32) del arco-lira: lira en vertical (cuerpo en U, brazos con volutas, travesaño arriba,
cuerdas verticales). Se dibuja con la flecha apuntando a la derecha y al guardar se da la vuelta (espejo):
en el juego la izquierda de la textura es hacia delante, así que la flecha apunta a la izquierda y tensa
las cuerdas hacia la derecha (hacia el jugador). 4 estados: quieto y 3 de tensado."""
import sys
from PIL import Image
N = 32
CONT = (19, 19, 18, 255)
ORO = (214, 170, 60, 255); ORO_CLA = (250, 222, 120, 255); ORO_OSC = (140, 96, 30, 255); ORO_MUY_OSC = (95, 62, 20, 255)
CUERDA = (240, 232, 200, 255); CIAN = (68, 218, 235, 255)
FLECHA = (110, 80, 50, 255); PUNTA = (170, 175, 182, 255); PLUMA = (240, 240, 240, 255)
CX, CY, RX, RY, GROSOR = 15.5, 14.0, 12.5, 14.5, 2.6
Y_BARRA, Y_FLECHA = 5, 16
CUERDAS = list(range(9, 24, 2))          # 8 cuerdas

def en_elipse(x, y, rx, ry):
    return ((x - CX) / rx) ** 2 + ((y - CY) / ry) ** 2 <= 1

def cuerpo(x, y):
    """Cuerpo en U + brazos + volutas + travesaño. Devuelve el color o None."""
    # U: anillo de la elipse, solo la mitad de abajo (y brazos que suben rectos un poco abiertos)
    if y >= CY and en_elipse(x, y, RX, RY) and not en_elipse(x, y, RX - GROSOR, RY - GROSOR):
        return 'cuerpo'
    # brazos: de y=CY hacia arriba, abriéndose un poco hacia fuera
    for lado in (-1, 1):
        borde_ext = CX + lado * (RX - 0.5 + (CY - y) * 0.12)
        if Y_BARRA - 1 <= y < CY:
            if lado < 0 and borde_ext - 0.5 <= x <= borde_ext + GROSOR - 1:
                return 'cuerpo'
            if lado > 0 and borde_ext - GROSOR + 1 <= x <= borde_ext + 0.5:
                return 'cuerpo'
    # travesaño
    if y in (Y_BARRA, Y_BARRA + 1) and 5 <= x <= 26:
        return 'barra'
    return None

# brazo que sube por encima del travesaño y se enrosca hacia fuera
VOLUTAS_IZQ = [(4, 4), (4, 3), (4, 2), (3, 1), (2, 0), (1, 0), (0, 1), (0, 2), (1, 3), (2, 3), (2, 2)]
VOLUTAS = VOLUTAS_IZQ + [(31 - x, y) for x, y in VOLUTAS_IZQ]

def pintar(salida, tension):
    im = Image.new('RGBA', (N, N), (0, 0, 0, 0)); p = im.load(); hay = set(); cuerdas = set()
    # cuerdas (debajo del cuerpo)
    for cx in CUERDAS:
        fondo = next(y for y in range(Y_FLECHA, N) if cuerpo(cx, y))
        for y in range(Y_BARRA + 2, fondo):
            if tension:
                span = (Y_FLECHA - (Y_BARRA + 2)) if y <= Y_FLECHA else (fondo - Y_FLECHA)
                x = round(cx - tension * (1 - abs(y - Y_FLECHA) / span))
            else:
                x = cx
            p[x, y] = CUERDA; cuerdas.add((x, y))
    # cuerpo
    for y in range(N):
        for x in range(N):
            c = cuerpo(x, y)
            if c:
                hay.add((x, y))
                if c == 'barra':
                    p[x, y] = ORO_CLA if y == Y_BARRA else ORO_OSC
                else:
                    luz = (x - CX) * -0.6 + (CY - y) * 0.4       # luz desde arriba a la izquierda
                    interior = not en_elipse(x, y, RX - 1.2, RY - 1.2) if y >= CY else False
                    p[x, y] = ORO_CLA if luz > 4 else (ORO if luz > -4 else ORO_OSC)
                    if y >= CY and en_elipse(x, y, RX - GROSOR + 1, RY - GROSOR + 1):
                        p[x, y] = ORO_MUY_OSC                     # canto interior (sombra)
    # ranuras decorativas en la U y gema
    for x, y in ((5, 19), (6, 22), (26, 19), (25, 22), (9, 26), (22, 26)):
        p[x, y] = ORO_MUY_OSC
    for x, y in ((15, 28), (16, 28)):
        p[x, y] = CIAN
    for x, y in VOLUTAS:
        p[x, y] = ORO_CLA if y <= 2 else ORO; hay.add((x, y))
    # flecha horizontal
    if tension:
        culata = min(CUERDAS) - tension
        for x in range(culata, 31):
            p[x, Y_FLECHA] = FLECHA; hay.add((x, Y_FLECHA))
        for x, y in ((30, Y_FLECHA), (29, Y_FLECHA - 1), (29, Y_FLECHA + 1), (31, Y_FLECHA)):
            if x < N:
                p[x, y] = PUNTA; hay.add((x, y))
        for k in (0, 1):
            for dy in (-1, 1):
                p[culata + k, Y_FLECHA + dy] = PLUMA; hay.add((culata + k, Y_FLECHA + dy))
    for y in range(N):
        for x in range(N):
            if (x, y) not in hay and (x, y) not in cuerdas and any((x + dx, y + dy) in hay for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                p[x, y] = CONT
    im = im.transpose(Image.FLIP_LEFT_RIGHT)
    im.save(salida)
    return im

carpeta = sys.argv[1]
ims = [pintar(f"{carpeta}/arco_lira.png", 0)]
for i, t in enumerate((2, 4, 6)):
    ims.append(pintar(f"{carpeta}/arco_lira_pulling_{i}.png", t))
g = Image.new('RGBA', (4 * 170, 170), (139, 139, 139, 255))
for i, im in enumerate(ims):
    r = im.resize((160, 160), Image.NEAREST); g.paste(r, (5 + i * 170, 5), r)
g.save(f"{carpeta}/arco_lira_grande.png")
