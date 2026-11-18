"""Iconos (16x16) de la armadura de lucarita, coherentes con la armadura puesta
(dibujar_lucarita_armadura.py): las mismas placas con rendijas, el mismo grano y los mismos tonos.

Cada icono es un dibujo con letras:
  letras minúsculas (a, b, c...) = una placa de metal (cada letra, una placa distinta)
  g = ribete de oro   x = rendija oscura   A = gema azul   R = gema roja   . = vacío
  Se le pone contorno oscuro alrededor, como a los objetos de Minecraft.

Uso: python dibujar_lucarita_iconos.py <carpeta_salida>"""
import sys
from PIL import Image

T = (0, 0, 0, 0)
CONT = (50, 22, 0, 255)
GRIETA = (66, 30, 2, 255)
C = [(99, 49, 1, 255), (128, 62, 6, 255), (156, 84, 22, 255), (186, 108, 40, 255),
     (209, 130, 56, 255), (232, 146, 62, 255), (247, 168, 84, 255), (252, 214, 170, 255)]
ORO = [(130, 92, 20, 255), (184, 142, 44, 255), (231, 197, 71, 255), (250, 230, 140, 255)]
AZ = [(32, 52, 128, 255), (60, 96, 196, 255), (120, 170, 245, 255)]
RO = [(120, 18, 26, 255), (190, 40, 44, 255), (240, 110, 96, 255)]

def ruido(x, y, semilla=0):
    n = (x * 374761393 + y * 668265263 + semilla * 982451653) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 0xFFFF

def icono(dibujo, tonos=None, vetas=None):
    """tonos: {letra: +1/-1} para aclarar u oscurecer una placa. vetas: {(x, y): color}."""
    tonos = tonos or {}
    assert len(dibujo) == 16 and all(len(f) == 16 for f in dibujo), dibujo
    im = Image.new('RGBA', (16, 16), T); p = im.load()
    celda = {(x, y): c for y, f in enumerate(dibujo) for x, c in enumerate(f) if c != '.'}
    for (x, y), c in celda.items():
        r = ruido(x, y, ord(c))
        if c == 'x':
            col = GRIETA
        elif c == 'A':
            col = AZ[2] if celda.get((x, y - 1)) != 'A' else AZ[0]
        elif c == 'R':
            col = RO[2] if celda.get((x, y - 1)) != 'R' else RO[0]
        elif c == 'g':
            arriba = celda.get((x, y - 1)) != 'g'
            abajo = celda.get((x, y + 1)) != 'g'
            col = ORO[3] if arriba and r > 0.6 else (ORO[2] if arriba or not abajo else ORO[1])
            if abajo and not arriba: col = ORO[1]
        else:
            misma = lambda dx, dy: celda.get((x + dx, y + dy)) == c
            if not misma(0, -1):   i = 6 if r > 0.8 else 5
            elif not misma(-1, 0): i = 4 if r > 0.5 else 3
            elif not misma(0, 1):  i = 0 if r > 0.5 else 1
            elif not misma(1, 0):  i = 1 if r > 0.5 else 2
            else:                  i = 2 + (r > 0.3) + (r > 0.75) + (r > 0.95)
            col = C[max(0, min(7, i + tonos.get(c, 0)))]
        p[x, y] = col
    for (x, y), col in (vetas or {}).items():
        p[x, y] = col
    for y in range(16):
        for x in range(16):
            if (x, y) not in celda and any((x + dx, y + dy) in celda for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                p[x, y] = CONT
    return im

# Casco: frente con ribete de oro y gema, rendija, mejillas a los lados y el NASAL entre los ojos
CASCO = ["................",
         "................",
         "....gggggggg....",
         "...aaaaARaaaa...",
         "..aaaaaaaaaaaa..",
         "..aaaaaaaaaaaa..",
         "..bbbxxxxxxccc..",
         "..bbb..dd..ccc..",
         "..bbb..dd..ccc..",
         "..bbb......ccc..",
         "..ggg......ggg..",
         "................", "................", "................", "................", "................"]
CASCO_VETAS = {(3, 7): AZ[1], (4, 8): AZ[0], (12, 7): RO[1], (11, 8): RO[0]}

# Peto: hombreras con ribete, cuello, pectorales con gema, abdomen, cinturón de oro con hebilla
PETO = ["................",
        "..hhh......hhh..",
        ".hhhhh....hhhhh.",
        ".hhhhhxxxxhhhhh.",
        ".gggaaaabbbbggg.",
        "..gaaaaARbbbbg..",
        "...aaaaabbbbb...",
        "...aaaaabbbbb...",
        "...xxxxxxxxxx...",
        "...cccccccccc...",
        "...cccccccccc...",
        "...ggggARgggg...",
        "....dddddddd....",
        "................", "................", "................"]
PETO_VETAS = {}

# Pantalones: cinturón de oro con hebilla, muslos, rodilleras y espinilleras
PANTALONES = ["................",
              "...ggggARgggg...",
              "...aaaaaaaaaa...",
              "...aaaaxxaaaa...",
              "...bbbb..cccc...",
              "...bbbb..cccc...",
              "...bbbb..cccc...",
              "...dddd..eeee...",
              "...xxxx..xxxx...",
              "...ffff..hhhh...",
              "...ffff..hhhh...",
              "................", "................", "................", "................", "................"]
PANTALONES_VETAS = {(4, 4): AZ[2], (5, 5): AZ[1], (11, 4): RO[2], (10, 5): RO[1], (5, 7): ORO[3], (10, 7): ORO[3]}

# Botas: vuelta de oro, caña, y pie con puntera
BOTAS = ["................", "................", "................", "................", "................",
         "................",
         "...ggg....ggg...",
         "...aaa....bbb...",
         "...aaa....bbb...",
         "...xxx....xxx...",
         "..cccc....dddd..",
         "..cccc....dddd..",
         "................", "................", "................", "................"]
BOTAS_VETAS = {(4, 7): AZ[2], (5, 8): AZ[1], (11, 7): RO[2], (10, 8): RO[1], (2, 10): ORO[3], (13, 10): ORO[3]}

if __name__ == "__main__":
    salida = sys.argv[1]
    icono(CASCO, {'d': 1, 'b': -1, 'c': -1}, CASCO_VETAS).save(f"{salida}/casco_lucarita.png")
    icono(PETO, {'h': 1, 'c': -1, 'd': -1}, PETO_VETAS).save(f"{salida}/peto_lucarita.png")
    icono(PANTALONES, {'d': 1, 'e': 1, 'f': -1, 'h': -1}, PANTALONES_VETAS).save(f"{salida}/pantalones_lucarita.png")
    icono(BOTAS, {'c': -1, 'd': -1}, BOTAS_VETAS).save(f"{salida}/botas_lucarita.png")
    print("ok")
