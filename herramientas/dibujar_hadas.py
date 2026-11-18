"""Hadas (textura de entidad animada): bolitas de luz. 8 fotogramas de 16x16 en una tira vertical (16x128).
En cada fotograma el halo "respira", el núcleo titila un poco y tres destellos giran alrededor y parpadean
(en cruz cuando brillan más). El juego pasa de fotograma cada 3 ticks (ver HadaRenderer.TICKS_POR_FOTOGRAMA)
y la dibuja siempre mirando a la cámara y a plena luz.
Uso: python3 dibujar_hadas.py <carpeta_salida>"""
import math
import sys
from PIL import Image

FOTOGRAMAS = 8
COLORES = {
    # nombre: (núcleo, cuerpo claro, cuerpo, borde/halo)
    "hada": ((245, 253, 255), (190, 235, 255), (120, 200, 255), (70, 150, 255)),
    "hada_oscura": ((252, 235, 255), (215, 160, 255), (160, 80, 240), (95, 30, 180)),
}


def poner(p, x, y, color, alfa):
    """Pinta sin tapar algo más opaco que ya hubiera."""
    if 0 <= x < 16 and 0 <= y < 16 and p[x, y][3] < alfa:
        p[x, y] = color + (alfa,)


def fotograma(nucleo, claro, cuerpo, borde, f):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p = im.load()
    fase = 2 * math.pi * f / FOTOGRAMAS
    respira = 0.45 * math.sin(fase)               # el halo crece y encoge
    titila = 0.35 * math.sin(2 * fase)            # el núcleo late el doble de rápido
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d <= 1.5 + titila:
                c, a = nucleo, 255
            elif d <= 2.7 + titila * 0.5:
                c, a = claro, 255
            elif d <= 3.8:
                c, a = cuerpo, 255
            elif d <= 4.6 + respira * 0.5:
                c, a = borde, 190
            elif d <= 5.6 + respira:
                c, a = borde, 95
            elif d <= 6.4 + respira:
                c, a = borde, 35
            else:
                continue
            p[x, y] = c + (a,)
    # brillo fijo arriba a la izquierda, para que se vea redonda
    p[6, 5] = nucleo + (255,)
    p[5, 6] = claro + (255,)
    # tres destellos girando alrededor, cada uno con su propio parpadeo
    for i in range(3):
        angulo = fase / 2 + i * 2 * math.pi / 3         # media vuelta por ciclo
        radio = 6.3 + 0.6 * math.sin(fase + i)
        x = round(7.5 + radio * math.cos(angulo))
        y = round(7.5 + radio * math.sin(angulo))
        brillo = (math.sin(fase * 2 + i * 2.1) + 1) / 2  # 0..1
        if brillo > 0.75:     # en cruz
            poner(p, x, y, nucleo, 255)
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                poner(p, x + dx, y + dy, claro, 150)
        elif brillo > 0.3:
            poner(p, x, y, claro, 220)
        else:
            poner(p, x, y, claro, 90)
    return im


salida = sys.argv[1] if len(sys.argv) > 1 else "."
vista = Image.new("RGBA", (FOTOGRAMAS * 90, 2 * 90), (40, 40, 40, 255))
for fila, (nombre, colores) in enumerate(COLORES.items()):
    tira = Image.new("RGBA", (16, 16 * FOTOGRAMAS), (0, 0, 0, 0))
    for f in range(FOTOGRAMAS):
        im = fotograma(*colores, f)
        tira.paste(im, (0, 16 * f))
        r = im.resize((80, 80), Image.NEAREST)
        vista.paste(r, (5 + f * 90, 5 + fila * 90), r)
    tira.save(f"{salida}/{nombre}.png")
vista.save(f"{salida}/hadas_grande.png")
# GIF para ver la animación (3 ticks por fotograma = 150 ms)
for nombre, colores in COLORES.items():
    cuadros = []
    for f in range(FOTOGRAMAS):
        fondo = Image.new("RGBA", (160, 160), (40, 40, 40, 255))
        r = fotograma(*colores, f).resize((160, 160), Image.NEAREST)
        fondo.paste(r, (0, 0), r)
        cuadros.append(fondo.convert("P", palette=Image.ADAPTIVE))
    cuadros[0].save(f"{salida}/{nombre}_animada.gif", save_all=True, append_images=cuadros[1:], duration=150, loop=0)
