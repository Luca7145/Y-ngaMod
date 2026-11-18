"""Lanza, katana y mandoble (25x25), con la paleta de las otras armas del Herrero.
Uso: python dibujar_armas_nuevas.py <carpeta_salida>"""
import sys, math
from PIL import Image
N = 25
CONT = (24, 24, 24, 255)
OSC = (68, 68, 68, 255); MED = (150, 150, 150, 255); CLA = (193, 193, 193, 255); MUYCLA = (216, 216, 216, 255); BLANCO = (255, 255, 255, 255)
MAD = (54, 37, 16, 255); MAD_C = (92, 64, 30, 255)
AZUL = (44, 78, 150, 255); AZUL_C = (72, 112, 190, 255)
NEGRO = (36, 36, 40, 255)
LATON = (150, 118, 50, 255); LATON_C = (196, 160, 80, 255)

def dibujar(pinta, inicio):
    im = Image.new('RGBA', (N, N), (0, 0, 0, 0)); p = im.load(); lleno = set()
    for y in range(N):
        for x in range(N):
            dx, dy = x + 0.5 - inicio[0], y + 0.5 - inicio[1]
            u, v = (dx - dy) / math.sqrt(2), (dx + dy) / math.sqrt(2)
            c = pinta(u, v)
            if c:
                p[x, y] = c; lleno.add((x, y))
    for y in range(N):
        for x in range(N):
            if (x, y) not in lleno and any((x + a, y + b) in lleno for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                p[x, y] = CONT
    return im

def hoja(u, v, ancho, canal=True, hasta_canal=99):
    """Colores de una hoja de doble filo: filo de arriba con luz, de abajo en sombra, canal en medio."""
    a = abs(v)
    if canal and a < 0.45 and u < hasta_canal: return MED
    if v < 0: return BLANCO if a > ancho - 0.6 else MUYCLA
    return OSC if a > ancho - 0.6 else CLA

def lanza(u, v):
    a = abs(v)
    if u < 0 or u > 31.5: return None
    if u <= 1.0 and a <= 0.75: return MED if v < 0 else OSC                      # regatón de acero
    if u <= 21.0 and a <= 0.5:                                                    # asta de madera
        if 15.5 <= u <= 17.5 or 6.5 <= u <= 8.0: return AZUL_C if v < 0 else AZUL  # anillas azules
        return MAD_C if v < 0 else MAD
    if 21.0 < u <= 22.6 and a <= 1.15: return CLA if v < 0 else OSC              # cubo de la punta
    if u > 22.6:                                                                  # punta en hoja de laurel
        t = (u - 22.6) / 8.9
        ancho = 2.1 * math.sin(math.pi * min(1, t * 1.15)) ** 0.75 if t < 0.87 else 2.1 * (1 - t) / 0.13 * 0.35
        if a <= ancho + 0.1:
            return hoja(u, v, max(ancho, 0.7), hasta_canal=29)
    return None

def katana(u, v):
    # La hoja se curva un poco; el filo va por fuera de la curva
    curva = 0.0 if u < 8.5 else 0.0045 * (u - 8.5) ** 2
    w = v - curva
    a = abs(w)
    if u < 0 or u > 32: return None
    if u <= 1.0 and abs(v) <= 1.1: return LATON_C if v < 0 else LATON             # kashira (remate)
    if u <= 7.0 and abs(v) <= 1.0:                                                 # tsuka: empuñadura trenzada
        rombo = (int(u * 1.5) + (1 if v > 0 else 0)) % 2
        return (AZUL_C if v < 0 else AZUL) if rombo else NEGRO
    if 7.0 < u <= 8.5 and abs(v) <= 2.4: return OSC if u > 7.8 else MED          # tsuba (guarda)
    if 8.5 < u <= 9.4 and abs(v) <= 1.0: return LATON_C                            # habaki
    if u > 9.4:
        ancho = 1.15 if u < 29 else 1.15 * (32 - u) / 3
        if a <= ancho + 0.1:
            if w < -0.35: return BLANCO if a > ancho - 0.55 else MUYCLA           # filo (en el lado de fuera de la curva)
            return CLA if w < 0.35 else OSC                                        # lomo
    return None

def mandoble(u, v):
    a = abs(v)
    if u < 0 or u > 32.3: return None
    if math.hypot(u - 0.8, v) <= 1.5: return MED if v < 0 else OSC                 # pomo
    if u <= 7.5 and a <= 1.0:                                                      # empuñadura larga (dos manos)
        return (MAD_C if v < 0 else MAD) if int(u * 1.6) % 2 else (AZUL_C if v < 0 else AZUL)
    if 7.5 < u <= 9.0 and a <= 4.2:                                                # gavilanes largos
        if a > 3.4: return CLA if v < 0 else OSC                                   # remates
        return MUYCLA if u < 8.2 else MED
    if 9.0 < u <= 10.6 and a <= 1.2: return CLA if v < 0 else MED                  # recazo
    if u > 10.6:
        ancho = 1.9 if u < 28.0 else 1.9 * (32.3 - u) / 4.3
        if a <= ancho + 0.12:
            return hoja(u, v, max(ancho, 0.7), hasta_canal=26)
    return None

if __name__ == "__main__":
    salida = sys.argv[1]
    ims = []
    for nombre, f, inicio in (("lanza", lanza, (1.0, 24.0)), ("katana", katana, (1.8, 23.2)), ("mandoble", mandoble, (1.6, 23.4))):
        im = dibujar(f, inicio); im.save(f"{salida}/{nombre}.png"); ims.append(im)
    g = Image.new('RGBA', (3 * 138 + 10, 148), (139, 139, 139, 255))
    for i, im in enumerate(ims):
        r = im.resize((128, 128), Image.NEAREST); g.paste(r, (10 + i * 138, 10), r)
    g.save(f"{salida}/armas_nuevas_grande.png")
