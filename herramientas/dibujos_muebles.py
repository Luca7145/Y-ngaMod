"""Dibujos (16x16) de los muebles y adornos de las casas. Los usa generar_muebles.py."""
import math
from PIL import Image

def ruido(x, y, s=0):
    n = (x * 374761393 + y * 668265263 + s * 982451653) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 0xFFFF

def T(pal, i):
    return pal[max(0, min(len(pal) - 1, i))] + (255,)

def lienzo(fondo=None):
    return Image.new('RGBA', (16, 16), fondo + (255,) if fondo else (0, 0, 0, 0))

def relleno(pal, s, base=None):
    im = lienzo(); p = im.load()
    b = len(pal) // 2 if base is None else base
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, s)
            p[x, y] = T(pal, b + (n > 0.78) - (n < 0.2))
    return im

PIEL = [(70, 46, 28), (92, 62, 38), (114, 80, 50), (136, 98, 64), (160, 120, 82), (186, 150, 108)]
MIMBRE = [(118, 84, 40), (146, 108, 56), (172, 132, 72), (196, 156, 92), (218, 182, 118)]
OCRE = [(140, 54, 26), (170, 76, 36), (196, 104, 50)]
HIERRO = [(24, 24, 28), (36, 36, 40), (50, 50, 56), (66, 66, 72), (86, 86, 92)]
MADERA_OSC = [(46, 28, 16), (62, 40, 22), (78, 52, 30), (96, 66, 40), (116, 82, 50)]
LATON = [(120, 86, 24), (170, 128, 40), (214, 172, 66), (240, 210, 120)]
BLANCO = [(208, 210, 212), (222, 224, 226), (234, 236, 238), (244, 245, 246), (252, 252, 252)]

def piel():
    """Una piel extendida en el suelo, con el borde irregular (lo de fuera, transparente)."""
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            dx, dy = (x - 7.5) / 7.6, (y - 7.5) / 6.4
            borde = 1 + 0.14 * math.sin(x * 1.7 + y * 0.9) + 0.1 * math.cos(y * 2.3)
            d = dx * dx + dy * dy
            # las patas (cuatro salientes en las esquinas)
            pata = (x in (1, 2, 13, 14)) and (y in (0, 1, 14, 15))
            if d > borde and not pata:
                continue
            n = ruido(x, y, 3)
            t = 3 if d < 0.45 else 2
            if d > 0.8: t = 1
            t += (n > 0.7) - (n < 0.2)
            if (x + y * 3) % 7 == 0: t += 1                   # mechones
            p[x, y] = T(PIEL, t)
    return im

def mimbre():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            fila = y // 2
            sobre = ((x // 2) + fila) % 2 == 0
            t = 3 if sobre else 1
            if y % 2 == 1: t -= 1
            t += ruido(x, y, 4) > 0.8
            p[x, y] = T(MIMBRE, t)
    return im

def rupestre():
    """Pinturas en la pared: un ciervo, manos y rayas (fondo transparente, se ve la pared detrás)."""
    im = lienzo(); p = im.load()
    def pinta(x, y, c=None):
        if 0 <= x < 16 and 0 <= y < 16 and ruido(x, y, 7) > 0.12:
            p[x, y] = (c or OCRE[ruido(x, y, 8) > 0.5]) + (255,)
    # ciervo (cuerpo, patas, cuello, astas)
    for x in range(3, 10):
        for y in range(6, 9): pinta(x, y)
    for (x, y) in ((3, 9), (3, 10), (4, 11), (8, 9), (9, 10), (9, 11), (10, 5), (10, 6), (11, 4), (11, 5), (12, 4),
                   (11, 3), (10, 2), (12, 2), (13, 3), (9, 1), (13, 1), (2, 6), (1, 5)):
        pinta(x, y)
    # manos (en negro)
    NEG = (40, 30, 26)
    for (bx, by) in ((12, 10),):
        for (x, y) in ((0, 2), (1, 2), (2, 2), (0, 3), (1, 3), (2, 3), (1, 4), (-1, 1), (0, 0), (1, 0), (2, 0), (3, 1)):
            pinta(bx + x, by + y, NEG)
    # rayas (cuentas)
    for x in (1, 3, 5):
        for y in range(13, 16): pinta(x, y, NEG)
    return im

def brasas():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 9)
            if n > 0.72: c = (255, 210, 90)
            elif n > 0.45: c = (236, 108, 30)
            elif n > 0.25: c = (150, 40, 16)
            else: c = (44, 30, 26)
            p[x, y] = c + (255,)
    return im

def llama():
    """Llamita de vela (se usa en un cubo pequeño: toda la textura es llama)."""
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) / 8 + (15 - y) / 30
            c = (255, 250, 210) if d < 0.45 else ((255, 214, 90) if d < 0.75 else (240, 140, 40))
            p[x, y] = c + (255,)
    return im

def tapiz():
    """Tapiz de lana: cenefa, rombos y un ciervo en el centro (sin escudos ni símbolos)."""
    ROJO = [(96, 20, 24), (128, 32, 34), (156, 44, 42)]
    AZUL = [(30, 42, 92), (44, 60, 120)]
    ORO = [(196, 150, 50), (230, 190, 80)]
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 10)
            if x in (0, 15) or y in (0, 15):
                c = ORO[n > 0.5]
            elif x in (1, 14) or y in (1, 14):
                c = AZUL[0]
            else:
                c = ROJO[1 + (n > 0.7) - (n < 0.2)]
                if (abs(x - 7.5) + abs(y - 7.5)) in (5.0, 6.0):
                    c = ORO[0]
            p[x, y] = c + (255,)
    for (x, y) in ((6, 7), (7, 7), (8, 7), (9, 7), (6, 8), (9, 8), (6, 9), (9, 9), (10, 6), (10, 5), (11, 4), (9, 4), (5, 7)):
        p[x, y] = ORO[1] + (255,)
    # flecos abajo
    for x in range(1, 15, 2):
        p[x, 15] = (0, 0, 0, 0)
    return im

def rueca():
    """La rueda de la rueca (con radios; lo demás, transparente)."""
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            a = math.atan2(y - 7.5, x - 7.5)
            if 6.3 < d < 7.8:
                p[x, y] = T(MADERA_OSC, 3 + (ruido(x, y, 11) > 0.6))
            elif d < 1.5:
                p[x, y] = T(MADERA_OSC, 1)
            elif d < 6.4 and min(abs(math.sin(a * 3)), 1) < 0.18:
                p[x, y] = T(MADERA_OSC, 2)
    return im

def comoda_frente():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 12)
            if y in (0, 5, 10, 15) or x in (0, 15):
                t = 0
            elif y in (1, 6, 11):
                t = 3
            else:
                t = 2 + (n > 0.75) - (n < 0.2)
            p[x, y] = T(MADERA_OSC, t)
    for y in (3, 8, 13):                                     # tiradores de latón
        for x in (4, 11):
            p[x, y] = T(LATON, 3); p[x + 1, y] = T(LATON, 2)
    return im

def reloj_pared():
    """Frente del reloj (se usa con uv 5..11 x 1..13): esfera arriba y péndulo abajo."""
    im = relleno(MADERA_OSC, 13); p = im.load()
    for y in range(2, 8):
        for x in range(6, 10 + 1):
            d = math.hypot(x - 8, y - 4.5)
            if d < 2.6: p[x, y] = T(BLANCO, 3)
    p[8, 4] = HIERRO[0] + (255,); p[8, 3] = HIERRO[0] + (255,); p[9, 4] = HIERRO[0] + (255,)
    for y in range(8, 12):
        for x in range(6, 11):
            p[x, y] = (40, 34, 30, 255)
    for y in range(8, 11): p[8, y] = T(LATON, 2)
    p[8, 11] = T(LATON, 3); p[7, 11] = T(LATON, 2); p[9, 11] = T(LATON, 2)
    return im

def globo():
    MAR = [(30, 70, 130), (44, 92, 156), (60, 112, 176)]
    TIERRA = [(168, 140, 80), (120, 140, 70)]
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x // 2, y // 2, 14)
            if n > 0.62:
                c = TIERRA[ruido(x, y, 15) > 0.5]
            else:
                c = MAR[1 + (ruido(x, y, 16) > 0.7)]
            if y in (0, 15): c = (230, 230, 220)
            p[x, y] = c + (255,)
    return im

def cuadro():
    """Un paisaje con marco dorado (campo, colina, árbol y cielo)."""
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = LATON[2 + ((x + y) % 3 == 0)]
            elif x in (1, 14) or y in (1, 14):
                c = LATON[1]
            elif y < 7:
                c = (150, 186, 214) if y < 4 else (196, 214, 222)
            elif y < 10:
                c = (92, 128, 70) if (x + 2 * (y - 7)) < 11 else (120, 150, 80)
            else:
                c = (150, 160, 80) if ruido(x, y, 17) > 0.4 else (126, 140, 66)
            p[x, y] = c + (255,)
    for (x, y) in ((10, 5), (10, 6), (10, 7), (10, 8)):          # tronco
        p[x, y] = (80, 56, 34, 255)
    for (x, y) in ((9, 3), (10, 3), (11, 3), (9, 4), (10, 4), (11, 4), (8, 4), (12, 4), (10, 2)):
        p[x, y] = (54, 96, 50, 255)
    p[4, 3] = (250, 240, 200, 255)                                 # el sol
    return im

def papel():
    im = relleno([(214, 204, 176), (228, 220, 194), (240, 234, 212)], 18); p = im.load()
    for y in range(3, 14, 2):
        for x in range(2, 14):
            if ruido(x, y, 19) > 0.3: p[x, y] = (70, 60, 56, 255)
    return im

def hierro_forjado():
    return relleno(HIERRO, 20)

def hierro_fundido():
    im = relleno(HIERRO, 21, 2); p = im.load()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15): p[x, y] = T(HIERRO, 1)
            if (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)): p[x, y] = T(HIERRO, 4)
    return im

def estufa_frente():
    im = hierro_fundido(); p = im.load()
    for y in range(5, 13):
        for x in range(3, 13):
            if x in (3, 12) or y in (5, 12): p[x, y] = T(HIERRO, 4)
            elif x % 2 == 0: p[x, y] = T(HIERRO, 0)
            else:
                n = ruido(x, y, 22)
                p[x, y] = ((255, 190, 70) if n > 0.6 else (226, 96, 28)) + (255,)
    for x in range(5, 11): p[x, 3] = T(LATON, 2)                 # el asa de la puerta
    return im

def farol_cristal():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 9)
            c = (255, 246, 200) if d < 3 else ((255, 224, 140) if d < 6 else (236, 196, 120))
            if x in (0, 15): c = (60, 50, 40)
            p[x, y] = c + (255,)
    return im

def pantalla():
    """Pantalla encendida (tele u ordenador): reflejo y un paisaje azulado."""
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = (20, 20, 24)
            elif y < 9:
                c = (70 + y * 6, 120 + y * 6, 190)
            elif y < 11:
                c = (60, 120, 80)
            else:
                c = (40, 90, 60)
            if x - y in (3, 4) and 1 <= y <= 6: c = tuple(min(255, v + 40) for v in c)
            p[x, y] = c + (255,)
    return im

def pantalla_lampara():
    return relleno([(214, 196, 160), (230, 214, 180), (242, 230, 202)], 23)

def nevera_frente():
    im = relleno(BLANCO, 24, 3); p = im.load()
    for x in range(16): p[x, 5] = T(BLANCO, 0)
    for y in range(1, 4): p[13, y] = (150, 152, 156, 255)
    for y in range(7, 11): p[13, y] = (150, 152, 156, 255)
    for x in range(16): p[x, 0] = T(BLANCO, 1); p[x, 15] = T(BLANCO, 1)
    return im

def teclado():
    im = lienzo((30, 30, 34)); p = im.load()
    for y in range(16):
        for x in range(16):
            if x % 2 == 1 and y % 3 != 2: p[x, y] = (70, 70, 76, 255)
    return im

def hogaza():
    im = relleno([(120, 70, 30), (150, 92, 42), (176, 116, 56), (200, 144, 80)], 25); p = im.load()
    for x in range(3, 13):
        p[x, 5] = (226, 186, 120, 255); p[x, 10] = (226, 186, 120, 255)
    return im

def radiador():
    return relleno([(176, 172, 164), (192, 188, 180), (206, 202, 194), (218, 214, 206)], 26)

def tela_gris():
    return relleno([(84, 86, 92), (96, 98, 104), (108, 110, 116), (120, 122, 128)], 27)

def cojin():
    im = relleno([(150, 152, 158), (164, 166, 172), (178, 180, 186)], 28); p = im.load()
    for x in range(16): p[x, 7] = (132, 134, 140, 255)
    return im

def mecha():
    im = lienzo((40, 34, 30)); p = im.load()
    for y in range(16):
        for x in range(16):
            if ruido(x, y, 30) > 0.7: p[x, y] = (70, 62, 56, 255)
    return im

def brasas_apagadas():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 9)
            c = (120, 116, 110) if n > 0.72 else ((74, 70, 66) if n > 0.4 else (36, 32, 30))
            p[x, y] = c + (255,)
    return im

def farol_apagado():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            c = (150, 146, 130) if math.hypot(x - 7.5, y - 9) < 4 else (120, 118, 108)
            if x in (0, 15): c = (60, 50, 40)
            p[x, y] = c + (255,)
    return im

def estufa_apagada():
    im = hierro_fundido(); p = im.load()
    for y in range(5, 13):
        for x in range(3, 13):
            if x in (3, 12) or y in (5, 12): p[x, y] = T(HIERRO, 4)
            elif x % 2 == 0: p[x, y] = T(HIERRO, 0)
            else: p[x, y] = ((60, 52, 48) if ruido(x, y, 22) > 0.6 else (30, 28, 28)) + (255,)
    for x in range(5, 11): p[x, 3] = T(LATON, 2)
    return im

def pantalla_apagada():
    im = lienzo((18, 18, 22)); p = im.load()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15): p[x, y] = (20, 20, 24, 255)
            elif x - y in (3, 4) and 1 <= y <= 6: p[x, y] = (44, 46, 54, 255)
    return im

DIBUJOS = {
    "mecha": mecha, "brasas_apagadas": brasas_apagadas, "farol_apagado": farol_apagado,
    "estufa_apagada": estufa_apagada, "pantalla_apagada": pantalla_apagada,
    "piel": piel, "mimbre": mimbre, "rupestre": rupestre, "brasas": brasas, "llama": llama, "tapiz": tapiz,
    "rueca": rueca, "comoda_frente": comoda_frente, "reloj_pared": reloj_pared, "globo": globo, "cuadro": cuadro,
    "papel": papel, "hierro_forjado": hierro_forjado, "hierro_fundido": hierro_fundido, "estufa_frente": estufa_frente,
    "farol_cristal": farol_cristal, "pantalla": pantalla, "pantalla_lampara": pantalla_lampara,
    "nevera_frente": nevera_frente, "teclado": teclado, "hogaza": hogaza, "radiador": radiador,
    "tela_gris": tela_gris, "cojin": cojin,
}
