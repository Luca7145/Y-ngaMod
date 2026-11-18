"""Icono de la pestaña creativa (16x16): una Y gruesa con los colores de la lucarita.
Brazo izquierdo azul, brazo derecho rojo y tronco de oro; contorno oscuro, brillo arriba a la izquierda
y sombra abajo a la derecha. Uso: python3 dibujar_icono_y.py <carpeta_salida>"""
import sys
from PIL import Image

AZUL = [(28, 48, 150), (48, 84, 227), (77, 114, 247), (109, 137, 247), (170, 190, 255)]
ROJO = [(120, 18, 28), (170, 30, 40), (209, 36, 47), (217, 67, 75), (245, 140, 140)]
ORO = [(99, 49, 1), (168, 104, 44), (195, 133, 43), (231, 197, 71), (252, 233, 160)]
CONTORNO = (30, 18, 12, 255)

# Forma: los dos brazos en diagonal (3 píxeles de grosor) que se juntan en el centro y bajan como tronco
forma = {}
for y in range(1, 8):
    izq = 1 + round((y - 1) * 4 / 6)              # los brazos bajan hacia el centro sin cruzarse
    for x in range(izq, izq + 4):
        if x <= 7:
            forma[(x, y)] = AZUL                  # brazo izquierdo
        if 15 - x >= 8:
            forma[(15 - x, y)] = ROJO             # brazo derecho
for y in range(7, 15):
    for x in (6, 7, 8, 9):
        forma[(x, y)] = ORO                       # tronco (y la unión de los brazos)

im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
p = im.load()
for (x, y), pal in forma.items():
    luz = (x - 1, y) not in forma or (x, y - 1) not in forma
    sombra = (x + 1, y) not in forma or (x, y + 1) not in forma
    tono = 4 if luz and not sombra else 1 if sombra and not luz else 2 + ((x * 7 + y * 13) % 3 == 0)
    p[x, y] = pal[tono] + (255,)
# contorno: se calcula primero y se pinta después
borde = {(x + dx, y + dy) for (x, y) in forma for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
         if (x + dx, y + dy) not in forma and 0 <= x + dx < 16 and 0 <= y + dy < 16}
for b in borde:
    p[b] = CONTORNO

salida = sys.argv[1] if len(sys.argv) > 1 else "."
im.save(f"{salida}/icono_yonga.png")
im.resize((160, 160), Image.NEAREST).save(f"{salida}/icono_yonga_grande.png")
