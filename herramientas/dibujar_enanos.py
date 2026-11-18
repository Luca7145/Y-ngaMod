"""Enanos (textura de entidad 64x32 para ModeloEnano): un bloque con cara, bracitos y patitas.
Amarillo brillante (luminoso) y rojo brillante (oscuro). Arriba más claro, abajo más oscuro, grano suave
y bordes marcados en cada cara; la cara (dos ojos de raya y la boca) va en el frente del cuerpo.
También las de la enana madre (enano_madre*.png: diadema y pestañas) y una vista en perspectiva (enanos_vista.png).
Uso: python3 dibujar_enanos.py <carpeta_salida>"""
import sys
from PIL import Image

PALETAS = {
    # nombre: (muy claro, claro, base, oscuro, muy oscuro, rasgos de la cara)
    "enano": ((255, 240, 150), (255, 222, 95), (240, 194, 52), (200, 150, 30), (150, 105, 20), (95, 55, 10)),
    "enano_oscuro": ((255, 130, 110), (235, 80, 66), (196, 38, 36), (145, 20, 24), (95, 10, 15), (35, 0, 4)),
}


def ruido(x, y, semilla):
    h = (x * 73856093) ^ (y * 19349663) ^ (semilla * 83492791)
    h = (h ^ (h >> 13)) * 1274126177
    return ((h ^ (h >> 16)) & 0xFF) / 255.0


def cara(p, x0, y0, ancho, alto, pal, luz, semilla):
    """Rellena una cara de la caja: luz = 2 (arriba), 1 (lados), 0 (abajo)."""
    mc, cl, ba, os, mo, _ = pal
    tonos = {2: (cl, mc, ba), 1: (ba, cl, os), 0: (os, ba, mo)}[luz]
    for y in range(alto):
        for x in range(ancho):
            r = ruido(x0 + x, y0 + y, semilla)
            c = tonos[0] if r < 0.74 else tonos[1] if r < 0.9 else tonos[2]
            borde = x == 0 or y == 0 or x == ancho - 1 or y == alto - 1
            if borde:
                c = os if luz > 0 else mo
            p[x0 + x, y0 + y] = c + (255,)


def caja(p, u, v, dx, dy, dz, pal, semilla):
    """Pinta las 6 caras de una caja de Minecraft (distribución estándar de UV)."""
    cara(p, u + dz, v, dx, dz, pal, 2, semilla)               # arriba
    cara(p, u + dz + dx, v, dx, dz, pal, 0, semilla + 1)      # abajo
    cara(p, u, v + dz, dz, dy, pal, 1, semilla + 2)           # lado
    cara(p, u + dz, v + dz, dx, dy, pal, 1, semilla + 3)      # frente
    cara(p, u + dz + dx, v + dz, dz, dy, pal, 1, semilla + 4) # otro lado
    cara(p, u + 2 * dz + dx, v + dz, dx, dy, pal, 1, semilla + 5)  # detrás


def dibujar(pal):
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    p = im.load()
    caja(p, 0, 0, 12, 8, 12, pal, 1)      # cuerpo
    caja(p, 0, 20, 4, 3, 4, pal, 11)      # patas
    caja(p, 16, 20, 4, 2, 3, pal, 21)     # brazos
    # La cara, en el frente del cuerpo (12..23, 12..19): ojos de raya y la boca un poco más abajo
    rasgo = pal[5] + (255,)
    for x in (14, 15, 16):
        p[x, 14] = rasgo
    for x in (19, 20, 21):
        p[x, 14] = rasgo
    for x in range(15, 20):
        p[x, 17] = rasgo
    # un brillo bajo cada ojo, para que no se vea plano
    for x in (14, 15, 16, 19, 20, 21):
        p[x, 15] = pal[3] + (255,)
    return im


def madre(tex, oscura):
    """La enana madre: la misma textura con una diadema alrededor de lo alto del cuerpo (dorada o de sangre),
    picos por delante, pestañas y una marca en la frente."""
    im = tex.copy()
    p = im.load()
    corona = [(255, 255, 245), (196, 132, 18), (255, 250, 210)] if not oscura else [(150, 14, 20), (110, 8, 14), (200, 40, 40)]
    # techo del cuerpo (12..23, 0..11): borde de la diadema
    for k in range(12):
        for (x, y) in ((12 + k, 0), (12 + k, 11), (12, k), (23, k)):
            p[x, y] = corona[k % 2] + (255,)
    # fila de arriba de las cuatro caras laterales (v = 12) con picos en el frente
    for x in range(0, 48):
        p[x, 12] = corona[x % 2] + (255,)
    for x in (13, 16, 19, 22):
        p[x, 13] = corona[2] + (255,)
    # pestañas sobre los ojos y marca en la frente
    rasgo = (40, 20, 10, 255) if not oscura else (20, 0, 4, 255)
    for x in (14, 16, 19, 21):
        p[x, 13] = rasgo
    p[17, 13] = corona[2] + (255,)
    p[18, 13] = corona[2] + (255,)
    return im


def vista(tex, e=9):
    """Perspectiva (caballera) del modelo: cada píxel de cada cara visible se dibuja como un polígono."""
    from PIL import ImageDraw
    im = Image.new("RGBA", (30 * e, 22 * e), (60, 60, 60, 255))
    d = ImageDraw.Draw(im)
    ox, oy = 5 * e, 19 * e

    def pr(x, y, z):  # x a la derecha, y hacia arriba, z hacia el fondo
        return (ox + (x + z * 0.55) * e, oy - (y + z * 0.45) * e)

    def cara(tu, tv, ancho, alto, origen, eje_u, eje_v):
        for j in range(alto):
            for i in range(ancho):
                c = tex.getpixel((tu + i, tv + j))
                if c[3] == 0:
                    continue
                def punto(a, b):
                    return pr(*(origen[k] + eje_u[k] * a + eje_v[k] * b for k in range(3)))
                d.polygon([punto(i, j), punto(i + 1, j), punto(i + 1, j + 1), punto(i, j + 1)], fill=c)

    def caja(u, v, dx, dy, dz, x0, y0, z0):
        # orden de dibujo: de atrás hacia delante
        cara(u + dz, v, dx, dz, (x0, y0 + dy, z0 + dz), (1, 0, 0), (0, 0, -1))                 # techo
        cara(u + dz + dx, v + dz, dz, dy, (x0 + dx, y0 + dy, z0), (0, 0, 1), (0, -1, 0))       # lado derecho
        cara(u + dz, v + dz, dx, dy, (x0, y0 + dy, z0), (1, 0, 0), (0, -1, 0))                 # frente

    caja(0, 20, 4, 3, 4, 7.5, 0, 5.5)      # patas de atrás
    caja(0, 20, 4, 3, 4, 0.5, 0, 5.5)
    caja(16, 20, 4, 2, 3, 12, 4.5, 4)      # brazo derecho (detrás del cuerpo)
    caja(16, 20, 4, 2, 3, -3.5, 4.5, 3)    # brazo izquierdo (se dibuja antes: queda detrás del frente)
    caja(0, 0, 12, 8, 12, 0, 3, 0)         # cuerpo
    caja(0, 20, 4, 3, 4, 0.5, 0, 0.5)      # patas de delante
    caja(0, 20, 4, 3, 4, 7.5, 0, 0.5)
    return im


salida = sys.argv[1] if len(sys.argv) > 1 else "."
vistas = []
for nombre, pal in PALETAS.items():
    t = dibujar(pal)
    t.save(f"{salida}/{nombre}.png")
    vistas.append(vista(t))
    m = madre(t, nombre == "enano_oscuro")
    m.save(f"{salida}/{nombre.replace('enano', 'enano_madre')}.png")
    vistas.append(vista(m))
fondo = Image.new("RGBA", (vistas[0].width * 4 + 30, vistas[0].height), (60, 60, 60, 255))
for i, v in enumerate(vistas):
    fondo.paste(v, (i * (v.width + 10), 0))
fondo.save(f"{salida}/enanos_vista.png")
tira = Image.new("RGBA", (64 * 4 * 2 + 10, 32 * 4), (60, 60, 60, 255))
for i, nombre in enumerate(PALETAS):
    r = Image.open(f"{salida}/{nombre}.png").resize((256, 128), Image.NEAREST)
    tira.paste(r, (i * 266, 0), r)
tira.save(f"{salida}/enanos_textura.png")
