"""Astado (piel negra): textura de entidad (128x64, para ModeloAstado), textura de los ojos que brillan (astado_ojos.png)
y la imagen del susto (astado_susto.png, la cara que salta al agarrarte).

Materiales: piel negra de cadáver con manchas podridas, musgo en hombros/espalda/muslos, hueso a la
vista (cráneo de ciervo, costillas, columna, antebrazos, espinillas), astas de hueso viejo, garras y pezuñas
negras. Cada caja del modelo se pinta cara por cara según la distribución de UV de Minecraft.
Uso: python3 dibujar_astado.py <carpeta_salida>"""
import math
import sys
from PIL import Image, ImageDraw

PIEL = [(30, 27, 26), (23, 21, 20), (38, 34, 32), (50, 45, 42)]
PODRIDO = [(48, 26, 23), (36, 19, 17), (60, 34, 28), (40, 44, 30)]
MUSGO = [(58, 92, 38), (74, 112, 46), (46, 74, 30), (92, 130, 56)]
HUESO = [(216, 206, 182), (198, 188, 162), (178, 166, 140), (228, 220, 200)]
HUECO = (46, 30, 26)
ASTA = [(112, 94, 74), (94, 78, 60), (130, 112, 90), (150, 136, 112)]
GARRA = [(34, 28, 24), (48, 40, 34), (24, 20, 18)]
NEGRO = (10, 5, 5)


def h(x, y, s):
    n = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def suave(x, y, escala, s):
    """Ruido suave (para manchas grandes)."""
    gx, gy = x / escala, y / escala
    x0, y0 = math.floor(gx), math.floor(gy)
    fx, fy = gx - x0, gy - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a, b = h(x0, y0, s), h(x0 + 1, y0, s)
    c, d = h(x0, y0 + 1, s), h(x0 + 1, y0 + 1, s)
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy


def tono(paleta, x, y, s):
    r = h(x, y, s)
    k = 0 if r < 0.5 else 1 if r < 0.75 else 2 if r < 0.92 else 3
    return paleta[min(k, len(paleta) - 1)]


def piel(x, y, s, musgo=0.0, podrido=0.35):
    """Piel con manchas de podrido y (si se pide) de musgo."""
    if musgo > 0 and suave(x, y, 3.0, s + 7) > 1.0 - musgo:
        return tono(MUSGO, x, y, s)
    if suave(x, y, 2.5, s + 3) > 1.0 - podrido:
        return tono(PODRIDO, x, y, s)
    return tono(PIEL, x, y, s)


TEX = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
OJOS = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
P = TEX.load()
PO = OJOS.load()


def caja(u, v, dx, dy, dz, pintar):
    """pintar(cara, i, j, ancho, alto, x, y) -> color. Caras: arriba, abajo, lado_a, frente, lado_b, detras."""
    caras = [("arriba", u + dz, v, dx, dz), ("abajo", u + dz + dx, v, dx, dz),
             ("lado_a", u, v + dz, dz, dy), ("frente", u + dz, v + dz, dx, dy),
             ("lado_b", u + dz + dx, v + dz, dz, dy), ("detras", u + 2 * dz + dx, v + dz, dx, dy)]
    for nombre, x0, y0, w, alto in caras:
        for j in range(alto):
            for i in range(w):
                c = pintar(nombre, i, j, w, alto, x0 + i, y0 + j)
                if c is not None:
                    P[x0 + i, y0 + j] = c + (255,)


# ---------- TORSO (0,0) 8x13x5: costillas delante, columna detrás, musgo en los hombros ----------
def torso(cara, i, j, w, a, x, y):
    if cara == "frente":
        if 1 <= j <= 8 and 1 <= i <= w - 2:
            if i in (3, 4):
                return tono(HUESO, x, y, 1)                   # esternón
            return tono(HUESO, x, y, 2) if j % 2 == 1 else HUECO   # costillas y huecos
        return piel(x, y, 1, musgo=0.0, podrido=0.5)
    if cara == "detras":
        if i in (3, 4):
            return tono(HUESO, x, y, 3) if j % 3 != 2 else HUECO   # columna
        return piel(x, y, 4, musgo=0.55 if j < 6 else 0.25)
    if cara == "arriba":
        return piel(x, y, 5, musgo=0.7)
    if cara in ("lado_a", "lado_b") and 2 <= j <= 7 and j % 2 == 0 and i in (1, 2, 3):
        return tono(HUESO, x, y, 6)                           # costillas asomando por los lados
    return piel(x, y, 7, musgo=0.2)


caja(0, 0, 8, 13, 5, torso)

# ---------- CRÁNEO (28,0) 6x6x7 y HOCICO (80,0) 4x3x4: hueso de ciervo ----------
def craneo(cara, i, j, w, a, x, y):
    if cara == "frente" and j in (1, 2) and i in (1, 4):
        PO[x, y] = (255, 190, 120, 255) if j == 1 else (255, 40, 25, 255)
        return NEGRO                                           # cuencas de los ojos
    if cara == "frente" and j == 3 and i in (2, 3):
        return HUECO
    if cara == "arriba" and suave(x, y, 2.0, 11) > 0.6:
        return tono(MUSGO, x, y, 11)                           # musgo creciendo en el cráneo
    if h(x, y, 12) > 0.9:
        return HUECO                                           # grietas
    return tono(HUESO, x, y, 13)


caja(28, 0, 6, 6, 7, craneo)


def hocico(cara, i, j, w, a, x, y):
    if cara == "frente" and j == 0 and i in (1, 2):
        return NEGRO                                           # fosas nasales
    if cara in ("frente", "lado_a", "lado_b") and j == a - 1:
        return (240, 234, 214) if (i % 2 == 0) else HUECO      # dientes de arriba
    return tono(HUESO, x, y, 14)


caja(80, 0, 4, 3, 4, hocico)


def mandibula(cara, i, j, w, a, x, y):
    if cara == "arriba" and (j == 0 or i in (0, w - 1)):
        return (240, 234, 214) if (i + j) % 2 == 0 else HUECO  # dientes de abajo
    if cara == "arriba":
        return (90, 30, 28)                                    # dentro de la boca
    if cara == "frente":
        return (240, 234, 214) if i % 2 == 0 else HUECO
    if cara in ("lado_a", "lado_b") and h(x, y, 15) > 0.6:
        return tono(PODRIDO, x, y, 15)                         # restos de carne
    return tono(HUESO, x, y, 16)


caja(98, 0, 4, 1, 5, mandibula)

# ---------- PELVIS (56,0) 7x4x4 y CUELLO (56,8) 3x4x3 ----------
caja(56, 0, 7, 4, 4, lambda c, i, j, w, a, x, y:
     tono(HUESO, x, y, 17) if (c in ("lado_a", "lado_b") and j < 3) else piel(x, y, 18, musgo=0.2))
caja(56, 8, 3, 4, 3, lambda c, i, j, w, a, x, y:
     tono(HUESO, x, y, 19) if (c == "detras" and i == 1) else piel(x, y, 20, podrido=0.5))
# vértebras (70,8) 1x1x2
caja(70, 8, 1, 1, 2, lambda c, i, j, w, a, x, y: tono(HUESO, x, y, 21))

# ---------- ASTAS: rama (78,8) 1x10x1, base (102,8) 2x4x2 y puntas ----------
def asta(largo, s):
    def pintar(c, i, j, w, a, x, y):
        if c == "arriba" or (c != "abajo" and j < 2 and largo < 9):
            return ASTA[3]                                     # puntas más claras
        if j % 4 == 1 and h(x, y, s) > 0.5:
            return HUECO                                       # anillos/grietas
        return tono(ASTA, x, y, s)
    return pintar


caja(78, 8, 1, 10, 1, asta(10, 22))
caja(102, 8, 2, 4, 2, lambda c, i, j, w, a, x, y: tono(ASTA[:2], x, y, 26) if h(x, y, 27) > 0.15 else HUECO)
caja(84, 8, 1, 7, 1, asta(7, 23))
caja(90, 8, 1, 8, 1, asta(8, 24))
caja(96, 8, 1, 5, 1, asta(5, 25))

# ---------- BRAZOS: brazo (0,20) 3x14x3, antebrazo (14,20) 2x12x2, mano (24,20) 3x2x3, garra (38,20) ----------
caja(0, 20, 3, 14, 3, lambda c, i, j, w, a, x, y:
     piel(x, y, 26, musgo=0.75 if j < 4 or c == "arriba" else 0.15, podrido=0.4))


def antebrazo(c, i, j, w, a, x, y):
    # casi hueso: piel solo en tiras (tendones)
    if c in ("frente", "detras", "arriba", "abajo") or h(x, y, 27) > 0.45:
        return tono(HUESO, x, y, 28) if (j % 5 != 4) else HUECO
    return tono(PODRIDO, x, y, 29)


caja(14, 20, 2, 12, 2, antebrazo)
caja(24, 20, 3, 2, 3, lambda c, i, j, w, a, x, y: tono(PODRIDO, x, y, 30))
caja(38, 20, 1, 4, 1, lambda c, i, j, w, a, x, y: GARRA[2] if j == 3 else tono(GARRA[:2], x, y, 31))

# ---------- PIERNAS: muslo (44,20) 3x12x3, espinilla (58,20) 2x11x2, pie (68,20) 3x1x4 ----------
caja(44, 20, 3, 12, 3, lambda c, i, j, w, a, x, y: piel(x, y, 32, musgo=0.45, podrido=0.4))
caja(58, 20, 2, 11, 2, lambda c, i, j, w, a, x, y:
     tono(HUESO, x, y, 33) if (c == "frente" or j > 7) else piel(x, y, 34, podrido=0.6))
caja(68, 20, 3, 1, 4, lambda c, i, j, w, a, x, y: tono(GARRA, x, y, 35))   # pezuñas


# ---------- LA CARA DEL SUSTO ----------
def susto():
    """Primer plano del cráneo: alargado, cuencas rasgadas con una pupila roja, boca abierta de par en par
    con colmillos, jirones de carne, astas gruesas y sombra hacia abajo y hacia los bordes."""
    n = 128
    im = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    HB = (205, 194, 168)       # hueso base (luego se sombrea)
    oscuro = (12, 5, 5)
    carne = (96, 36, 30)
    carne2 = (70, 24, 22)

    # Astas: gruesas, con varias puntas, saliéndose por arriba
    for lado in (-1, 1):
        x0 = 64 + lado * 20
        rama = [(x0, 36), (x0 + lado * 10, 22), (x0 + lado * 22, 12), (x0 + lado * 36, 0)]
        d.line(rama, fill=ASTA[1], width=7, joint="curve")
        d.line([(x0 + lado * 8, 25), (x0 + lado * 4, 6), (x0 + lado * 6, -4)], fill=ASTA[0], width=5)
        d.line([(x0 + lado * 20, 14), (x0 + lado * 34, 14), (x0 + lado * 44, 8)], fill=ASTA[0], width=4)
        d.line([(x0 + lado * 28, 7), (x0 + lado * 26, -6)], fill=ASTA[2], width=4)
        d.line([(x0 + lado * 14, 18), (x0 + lado * 18, 4)], fill=ASTA[2], width=3)

    # Mandíbula descolgada (detrás del cráneo): dos huesos que bajan y el interior de la boca
    d.polygon([(40, 94), (88, 94), (96, 128), (32, 128)], fill=oscuro)
    d.polygon([(36, 88), (45, 91), (40, 128), (28, 128)], fill=HB)
    d.polygon([(92, 88), (83, 91), (88, 128), (100, 128)], fill=HB)
    # dientes de abajo, apuntando hacia arriba, desiguales
    for k, alto in enumerate((10, 6, 14, 7, 12, 5, 13, 8, 11, 6, 12)):
        x = 41 + k * 4.2
        d.polygon([(x, 127), (x + 3, 127), (x + 1.5, 127 - alto)], fill=(222, 212, 188))

    # Cráneo y hocico largo
    d.polygon([(38, 44), (44, 32), (56, 26), (72, 26), (84, 32), (90, 44), (90, 58), (84, 66),
               (80, 100), (70, 106), (58, 106), (48, 100), (44, 66), (38, 58)], fill=HB)
    # colmillos de arriba, largos y torcidos
    for k, alto in enumerate((12, 7, 15, 6, 6, 14, 8, 11)):
        x = 50 + k * 3.6
        d.polygon([(x, 102), (x + 3, 102), (x + 1.5 + (k % 3 - 1), 102 + alto)], fill=(232, 224, 202))

    # Cuencas rasgadas, inclinadas hacia el centro (mirada de odio)
    d.polygon([(40, 48), (60, 55), (58, 66), (44, 62)], fill=oscuro)
    d.polygon([(88, 48), (68, 55), (70, 66), (84, 62)], fill=oscuro)
    # Fosas nasales: dos rajas largas
    d.polygon([(60, 74), (63, 74), (62, 90), (59, 88)], fill=oscuro)
    d.polygon([(68, 74), (65, 74), (66, 90), (69, 88)], fill=oscuro)
    # Grietas
    d.line([(64, 27), (62, 36), (66, 42), (63, 50)], fill=(120, 108, 88), width=1)
    d.line([(84, 40), (80, 48)], fill=(120, 108, 88), width=1)
    d.line([(46, 70), (50, 82), (48, 92)], fill=(120, 108, 88), width=1)

    # Jirones de carne podrida colgando de los lados y entre las mandíbulas
    for (x, y0, largo, ancho) in ((40, 58, 34, 4), (86, 58, 40, 4), (46, 96, 26, 2), (81, 96, 30, 2), (56, 104, 14, 2)):
        d.polygon([(x - ancho / 2, y0), (x + ancho / 2, y0), (x + 1, y0 + largo), (x - 1, y0 + largo - 3)], fill=carne)
    d.line([(52, 104), (56, 120)], fill=carne2, width=1)
    d.line([(76, 104), (73, 122)], fill=carne2, width=1)

    pi = im.load()
    # Sombreado del hueso: más oscuro hacia abajo y hacia los lados, con grano y musgo arriba
    for y in range(n):
        for x in range(n):
            c = pi[x, y]
            if not c[3] or c[:3] != HB:
                continue
            if y < 44 and suave(x, y, 5.0, 40) > 0.6:
                pi[x, y] = tono(MUSGO, x, y, 41) + (255,)
                continue
            k = 1.0 - 0.45 * (y / n) - 0.35 * (abs(x - 64) / 30) ** 2
            k -= 0.10 if h(x, y, 42) > 0.8 else 0
            k = max(0.25, k)
            pi[x, y] = (int(c[0] * k), int(c[1] * k), int(c[2] * k), 255)
    # Borde oscuro alrededor de las cuencas (hundidas)
    for y in range(44, 70):
        for x in range(36, 92):
            c = pi[x, y]
            if c[3] and c[:3] != oscuro and any(pi[x + dx, y + dy][:3] == oscuro
                                                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (2, 0), (-2, 0))):
                pi[x, y] = (int(c[0] * 0.45), int(c[1] * 0.4), int(c[2] * 0.4), 255)
    # Ojos: pupila roja pequeña con núcleo claro y un halo
    for cx, cy in ((52, 59), (76, 59)):
        for y in range(cy - 4, cy + 5):
            for x in range(cx - 4, cx + 5):
                dist = math.hypot(x - cx, y - cy)
                if dist <= 1.0:
                    pi[x, y] = (255, 236, 200, 255)
                elif dist <= 2.2:
                    pi[x, y] = (255, 50, 30, 255)
                elif dist <= 4.2 and pi[x, y][:3] == oscuro:
                    pi[x, y] = (110, 12, 10, 255)
    return im.resize((n * 3, n * 3), Image.NEAREST)


salida = sys.argv[1] if len(sys.argv) > 1 else "."
TEX.save(f"{salida}/astado.png")
OJOS.save(f"{salida}/astado_ojos.png")
susto().save(f"{salida}/astado_susto.png")
TEX.resize((512, 256), Image.NEAREST).save(f"{salida}/astado_textura_grande.png")
