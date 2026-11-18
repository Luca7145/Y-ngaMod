"""Duende: tres texturas de entidad (64x64, para ModeloDuende), una por máscara, con sus ojos que brillan,
y los tres dardos (formato de flecha de Minecraft, 32x32).
  veneno   máscara verde con cara de calavera y colmillos
  nausea   máscara amarilla con espirales en los ojos y boca ondulada
  lentitud máscara azul con ojos adormilados y una concha de caracol en la frente
Cuerpo morado, túnica oscura con cinturón, capucha morada oscura y cerbatana de caña.
Uso: python3 dibujar_duende.py <carpeta_salida>"""
import sys
from PIL import Image

PIEL = [(124, 64, 168), (106, 50, 148), (140, 82, 186), (92, 40, 128)]
CAPUCHA = [(70, 30, 100), (58, 24, 84), (84, 40, 116)]
TUNICA = [(62, 44, 56), (50, 34, 46), (74, 54, 66)]
CINTURON = (110, 76, 40)
CANA = [(176, 168, 92), (160, 150, 80), (190, 184, 108)]
OSCURO = (20, 10, 24)

MASCARAS = {
    "veneno": {"base": [(70, 150, 60), (56, 128, 48), (86, 168, 72)], "borde": (30, 70, 28), "ojos": (150, 255, 90)},
    "nausea": {"base": [(232, 182, 52), (212, 152, 40), (244, 202, 80)], "borde": (150, 90, 20), "ojos": (255, 240, 140)},
    "lentitud": {"base": [(122, 172, 222), (102, 152, 206), (146, 192, 234)], "borde": (54, 90, 140), "ojos": (170, 230, 255)},
}


def h(x, y, s):
    n = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def tono(paleta, x, y, s):
    r = h(x, y, s)
    k = 0 if r < 0.55 else 1 if r < 0.8 else 2 if r < 0.94 else 3
    return paleta[min(k, len(paleta) - 1)]


def caja(P, u, v, dx, dy, dz, pintar):
    caras = [("arriba", u + dz, v, dx, dz), ("abajo", u + dz + dx, v, dx, dz),
             ("lado_a", u, v + dz, dz, dy), ("frente", u + dz, v + dz, dx, dy),
             ("lado_b", u + dz + dx, v + dz, dz, dy), ("detras", u + 2 * dz + dx, v + dz, dx, dy)]
    for nombre, x0, y0, w, alto in caras:
        for j in range(alto):
            for i in range(w):
                c = pintar(nombre, i, j, w, alto, x0 + i, y0 + j)
                if c is not None:
                    P[x0 + i, y0 + j] = c + (255,)


def cara_mascara(nombre, i, j, base, borde, ojos, pon_ojo):
    """La cara de la máscara (7x6)."""
    if nombre == "veneno":
        if (i, j) in ((1, 1), (5, 1)):
            return borde                                    # cejas de calavera
        if (i, j) in ((1, 2), (2, 2), (4, 2), (5, 2)):
            if i in (2, 4):
                pon_ojo(ojos)
            return OSCURO
        if j == 3 and i == 3:
            return borde                                    # nariz
        if j == 4 and 1 <= i <= 5:
            return (236, 236, 220) if i in (2, 4) else OSCURO   # colmillos
        if j == 5 and i in (2, 4):
            return (236, 236, 220)
    elif nombre == "nausea":
        # ojos que brillan con un remolino morado alrededor (arriba-izquierda y abajo-derecha)
        if (i, j) in ((2, 2), (4, 2)):
            pon_ojo(ojos)
            return ojos
        if (i, j) in ((1, 1), (2, 1), (3, 3), (4, 1), (5, 1), (5, 3), (1, 3)):
            return (110, 50, 140)
        if (i, j) in ((1, 4), (3, 4), (5, 4), (2, 5), (4, 5)):
            return (110, 50, 140)                           # boca ondulada
    elif nombre == "lentitud":
        if j == 0 and i in (2, 3, 4):
            return (200, 180, 120) if i != 3 else (150, 120, 70)   # concha de caracol
        if j == 1 and i in (1, 2, 4, 5):
            return base[2]                                  # párpados
        if j == 2 and i in (1, 2, 4, 5):
            if i in (2, 4):
                pon_ojo(ojos)
            return OSCURO                                   # ojos medio cerrados
        if (i, j) == (3, 4):
            return OSCURO                                   # boquita en "o"
    if i in (0, 6) or j == 0 and nombre != "lentitud":
        return borde
    return None


def dibujar(nombre, datos):
    tex = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    ojos_img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    P, PO = tex.load(), ojos_img.load()
    base, borde, ojos = datos["base"], datos["borde"], datos["ojos"]

    # cuerpo (0,0) 5x4x3: túnica con cinturón
    caja(P, 0, 0, 5, 4, 3, lambda c, i, j, w, a, x, y: CINTURON if j == 2 and c not in ("arriba", "abajo") else tono(TUNICA, x, y, 1))
    # cabeza (0,8) 7x6x7: piel morada; por delante la tapa la máscara
    caja(P, 0, 8, 7, 6, 7, lambda c, i, j, w, a, x, y: tono(CAPUCHA, x, y, 2) if (c == "arriba" or j < 2) else tono(PIEL, x, y, 3))

    # máscara (28,8) 7x6x1
    def mascara(c, i, j, w, a, x, y):
        if c == "frente":
            def pon_ojo(color):
                PO[x, y] = color + (255,)
            r = cara_mascara(nombre, i, j, base, borde, ojos, pon_ojo)
            return r if r is not None else tono(base, x, y, 4)
        return borde
    caja(P, 28, 8, 7, 6, 1, mascara)

    # capucha (0,22) 5x2x5 y pico (20,22) 2x2x2
    caja(P, 0, 22, 5, 2, 5, lambda c, i, j, w, a, x, y: tono(CAPUCHA, x, y, 5))
    caja(P, 20, 22, 2, 2, 2, lambda c, i, j, w, a, x, y: tono(CAPUCHA, x, y, 6))
    # oreja (28,16) 4x2x1: por dentro rosada
    caja(P, 28, 16, 4, 2, 1, lambda c, i, j, w, a, x, y: (196, 120, 190) if c == "frente" and 0 < i < 3 else tono(PIEL, x, y, 7))
    # brazo (28,22) 2x6x2: mano más oscura
    caja(P, 28, 22, 2, 6, 2, lambda c, i, j, w, a, x, y: PIEL[3] if j == a - 1 else tono(PIEL, x, y, 8))
    # pierna (36,22) 2x4x2: pantalón y pies
    caja(P, 36, 22, 2, 4, 2, lambda c, i, j, w, a, x, y: OSCURO if j == a - 1 or c == "abajo" else tono(TUNICA, x, y, 9))
    # cerbatana (0,32) 1x1x10: caña con nudos y una franja del color de la máscara
    caja(P, 0, 32, 1, 1, 10, lambda c, i, j, w, a, x, y:
         (base[0] if (c in ("lado_a", "lado_b") and i in (4, 5)) or (c in ("arriba", "abajo") and j in (4, 5))
          else (CANA[1] if (i % 3 == 2 or j % 3 == 2) else CANA[0])))
    return tex, ojos_img


def dardo(color):
    """Formato de flecha de Minecraft (32x32): lado en (0,0)-(16,5), culo en (0,5)-(5,10)."""
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    p = im.load()
    for x in range(2, 13):
        p[x, 2] = (150, 120, 70, 255)                      # varilla fina
    for x, y in ((0, 1), (1, 1), (2, 1), (0, 3), (1, 3), (2, 3), (1, 0), (1, 4)):
        p[x, y] = color + (255,)                           # plumas
    for x in (13, 14):
        p[x, 2] = (200, 205, 212, 255)
    p[15, 2] = (240, 244, 248, 255)                        # punta
    for x, y in ((2, 7), (1, 6), (3, 8), (3, 6), (1, 8)):
        p[x, y + 0] = color + (255,)
    return im


salida = sys.argv[1] if len(sys.argv) > 1 else "."
for nombre, datos in MASCARAS.items():
    tex, ojos = dibujar(nombre, datos)
    tex.save(f"{salida}/duende_{nombre}.png")
    ojos.save(f"{salida}/duende_{nombre}_ojos.png")
    dardo(datos["base"][0]).save(f"{salida}/dardo_{nombre}.png")
