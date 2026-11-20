"""El martillo de guerra (arma del Herrero): 25x25, con la paleta de las otras armas de lucarita (acero azulado,
mango de madera y empuñadura vendada en azul). Cabeza perpendicular al mango: cara de martillo arriba a la izquierda
(el lado que golpea, como en las demás armas), pico abajo a la derecha y una punta arriba.
Uso: python3 dibujar_martillo.py <carpeta src/main/resources>"""
import math
import os
import sys

from PIL import Image

N = 25
ACERO = {"contorno": (0x17, 0x2d, 0x40), "sombra": (0x2c, 0x3d, 0x65), "medio": (0x42, 0x51, 0x7c),
         "claro": (0x76, 0x83, 0x9f), "brillo": (0x9e, 0xaf, 0xd2), "destello": (0xd4, 0xe0, 0xf4)}
MADERA = {"contorno": (0x36, 0x24, 0x11), "base": (0x61, 0x4e, 0x1a), "claro": (0x80, 0x68, 0x2a)}
VENDA = {"contorno": (0x00, 0x02, 0x2c), "base": (0x42, 0x67, 0xa8), "sombra": (0x1b, 0x32, 0x5e)}

C = (16.0, 8.0)                                   # centro de la cabeza
V = (1 / math.sqrt(2), -1 / math.sqrt(2))         # dirección del mango (hacia arriba a la derecha)
U = (-1 / math.sqrt(2), -1 / math.sqrt(2))        # hacia donde va la cara del martillo (arriba a la izquierda,
                                                  # el lado que golpea, como la hoja de la alabarda); el pico al otro lado


def local(px, py):
    dx, dy = px - C[0], py - C[1]
    return dx * U[0] + dy * U[1], dx * V[0] + dy * V[1]        # (a lo largo de la cabeza, a lo largo del mango)


def es_cabeza(px, py):
    u, v = local(px, py)
    if 1.0 <= u <= 6.0 and abs(v) <= 3.1:                     # el martillo (bloque)
        return "martillo"
    if -1.6 <= u < 1.0 and abs(v) <= 1.7:                     # cuello
        return "cuello"
    if -8.6 <= u < -1.6 and abs(v) <= 1.7 * (u + 8.6) / 7.0:  # el pico, cada vez más fino
        return "pico"
    if abs(u) <= 1.0 and 0 <= v <= 5.6 - abs(u):              # la punta de arriba
        return "punta"
    return None


def dist_mango(px, py):
    a, b = (2.2, 22.8), (C[0], C[1])
    dx, dy = b[0] - a[0], b[1] - a[1]
    t = max(0, min(1, ((px - a[0]) * dx + (py - a[1]) * dy) / (dx * dx + dy * dy)))
    return math.hypot(px - (a[0] + t * dx), py - (a[1] + t * dy)), t


def dibujar():
    tipo = [[None] * N for _ in range(N)]
    for y in range(N):
        for x in range(N):
            px, py = x + 0.5, y + 0.5
            c = es_cabeza(px, py)
            if c:
                tipo[y][x] = ("acero", c)
                continue
            if math.hypot(px - 2.6, py - 22.4) <= 1.3:             # pomo
                tipo[y][x] = ("acero", "pomo")
                continue
            d, t = dist_mango(px, py)
            if d <= 0.75:
                tipo[y][x] = ("venda" if t < 0.38 else "madera", t)
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    lleno = lambda x, y: 0 <= x < N and 0 <= y < N and tipo[y][x] is not None
    for y in range(N):
        for x in range(N):
            if tipo[y][x] is None:
                vecinos = [tipo[y + dy][x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if lleno(x + dx, y + dy)]
                if vecinos:
                    m = vecinos[0][0]
                    c = ACERO["contorno"] if any(v[0] == "acero" for v in vecinos) else (
                        VENDA["contorno"] if m == "venda" else MADERA["contorno"])
                    img.putpixel((x, y), c + (255,))
                continue
            material, dato = tipo[y][x]
            arriba_izq = not lleno(x - 1, y) or not lleno(x, y - 1)
            abajo_der = not lleno(x + 1, y) or not lleno(x, y + 1)
            if material == "acero":
                u, v = local(x + 0.5, y + 0.5)
                if arriba_izq:
                    c = ACERO["destello"] if dato == "martillo" else ACERO["brillo"]
                elif abajo_der:
                    c = ACERO["sombra"]
                elif dato == "martillo":
                    c = ACERO["brillo"] if v > 0.6 else (ACERO["claro"] if v > -0.8 else ACERO["medio"])
                else:
                    c = ACERO["claro"]
            elif material == "venda":
                c = VENDA["sombra"] if (x + y) % 3 == 0 else VENDA["base"]
            else:
                c = MADERA["claro"] if arriba_izq else MADERA["base"]
            img.putpixel((x, y), c + (255,))
    return img


if __name__ == "__main__":
    dibujar().save(os.path.join(sys.argv[1], "assets", "yonga", "textures", "item", "martillo_guerra.png"))
