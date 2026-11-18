"""Genera los bloques de construcción del Ingeniero: texturas (16x16, estilo Minecraft) y todos sus archivos
de datos (estados, modelos, objetos, botines, etiquetas y recetas del banco).

Uso (desde la carpeta del repositorio):  python herramientas/generar_bloques.py
Escribe en forge-1.20.1-47.4.10-mdk/src/main/resources. Si cambias un dibujo, vuelve a ejecutarlo."""
import json, os, sys
from PIL import Image
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dibujos_estilos as E

RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'forge-1.20.1-47.4.10-mdk', 'src', 'main', 'resources')
ASSETS = os.path.join(RAIZ, 'assets', 'yonga')
DATA = os.path.join(RAIZ, 'data', 'yonga')

def ruido(x, y, s=0):
    n = (x * 374761393 + y * 668265263 + s * 982451653) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 0xFFFF

def tono(pal, i):
    return pal[max(0, min(len(pal) - 1, i))]

GRIS = [(52, 52, 56), (74, 74, 78), (96, 96, 100), (116, 116, 120), (134, 134, 138), (152, 152, 156), (176, 176, 180)]
GRANITO = [(60, 54, 52), (84, 76, 72), (108, 98, 92), (128, 118, 110), (146, 136, 126), (164, 154, 144), (186, 178, 168)]
ACERA = [(96, 96, 92), (120, 120, 114), (140, 140, 134), (156, 156, 150), (170, 170, 164), (184, 184, 178), (204, 204, 198)]
ARENISCA = [(120, 98, 66), (148, 124, 88), (176, 150, 108), (196, 172, 128), (212, 190, 146), (226, 208, 166), (240, 228, 196)]
ACERO = [(40, 42, 48), (58, 62, 70), (80, 85, 94), (104, 110, 120), (128, 134, 144), (156, 162, 170), (196, 200, 206)]
AMARILLO = [(150, 110, 10), (196, 150, 20), (232, 186, 30), (250, 210, 60)]
NEGRO = [(22, 22, 24), (34, 34, 38), (48, 48, 52)]
COBRE = [(99, 49, 1), (143, 69, 0), (186, 108, 40), (209, 130, 56), (247, 149, 57), (252, 214, 170)]

def lienzo():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))

def piezas(regiones, pal, junta, semilla=0, redondear=False):
    """regiones: función (x, y) -> id de pieza (o None = junta). Cada pieza con su bisel y grano."""
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            r = regiones(x, y)
            if r is None:
                p[x, y] = junta + (255,); continue
            arriba = regiones(x, (y - 1) % 16) != r
            izq = regiones((x - 1) % 16, y) != r
            abajo = regiones(x, (y + 1) % 16) != r
            der = regiones((x + 1) % 16, y) != r
            if redondear and (arriba or abajo) and (izq or der):
                p[x, y] = junta + (255,); continue
            n = ruido(x, y, semilla + hash(r) % 97)
            base = len(pal) // 2 + (hash(r) % 3 - 1) * 0     # cada pieza, de su tono
            if arriba or izq: i = base + 2 if n > 0.4 else base + 1
            elif abajo or der: i = base - 2 if n > 0.4 else base - 1
            else: i = base + (n > 0.75) - (n < 0.25)
            p[x, y] = tono(pal, i) + (255,)
    return im

# ---------- ADOQUINADO ----------
def adoquin_calle():
    # Adoquines de 7x3 (más junta), en hileras desplazadas medio adoquín, con las esquinas redondeadas
    def r(x, y):
        fila = y // 4
        desp = 4 if fila % 2 else 0
        if y % 4 == 3 or (x + desp) % 8 == 7:
            return None
        return (fila, ((x + desp) % 16) // 8)
    im = piezas(r, GRIS[1:] + [(196, 196, 200)], (84, 84, 88), 1)
    p = im.load()
    for y in range(16):                      # esquinas redondeadas (solo el píxel de la esquina)
        for x in range(16):
            fila = y // 4; desp = 4 if fila % 2 else 0; lx = (x + desp) % 8; ly = y % 4
            if (lx in (0, 6)) and (ly in (0, 2)):
                p[x, y] = (84, 84, 88, 255)
    return im

def adoquin_espiga():
    # espiga: ladrillos de 2x4 que alternan de pie y tumbados en bloques de 4x4
    def r(x, y):
        bx, by = x // 4, y // 4
        lx, ly = x % 4, y % 4
        if (bx + by) % 2 == 0:      # tumbados: dos de 4x2
            if ly == 1 or ly == 3: return None if lx in (0,) and ly == 3 else ('h', bx, by, ly // 2) if ly != 1 else None
            return ('h', bx, by, ly // 2)
        else:                       # de pie: dos de 2x4
            if lx == 1 or lx == 3: return None
            return ('v', bx, by, lx // 2)
    return piezas(r, GRANITO, (44, 38, 36), 2)

def baldosa_acera():
    def r(x, y):
        if x % 8 == 7 or y % 8 == 7: return None
        return (x // 8, y // 8)
    im = piezas(r, ACERA, (88, 88, 84), 3)
    p = im.load()
    for y in range(16):                       # puntitos de la textura antideslizante
        for x in range(16):
            if x % 8 not in (0, 7) and y % 8 not in (0, 7) and (x + 2 * y) % 5 == 0:
                p[x, y] = tono(ACERA, 1) + (255,)
    return im

# ---------- SILLERÍA ----------
def sillares():
    def r(x, y):
        fila = y // 8
        desp = 4 if fila % 2 else 0
        if y % 8 == 7 or (x + desp) % 8 == 7: return None
        return (fila, ((x + desp) % 16) // 8)
    return piezas(r, ARENISCA, (110, 90, 60), 4)

def sillar_moldura():
    im = sillares(); p = im.load()
    perfil = {5: 6, 6: 5, 7: 1, 8: 4, 9: 3, 10: 0}            # la moldura (fila: tono)
    for y, t in perfil.items():
        for x in range(16):
            n = ruido(x, y, 9)
            p[x, y] = tono(ARENISCA, t + (1 if n > 0.8 else 0)) + (255,)
    return im

def columna_lado():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            f = x % 4                                         # acanaladuras
            t = {0: 5, 1: 3, 2: 2, 3: 4}[f]
            t += 1 if ruido(x, y, 5) > 0.85 else 0
            p[x, y] = tono(ARENISCA, t) + (255,)
    return im

def columna_tapa():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            t = 4 if d < 6.5 else 2
            if 6.5 <= d < 7.5: t = 1
            if d < 2: t = 3
            t += 1 if ruido(x, y, 6) > 0.8 else 0
            p[x, y] = tono(ARENISCA, t) + (255,)
    return im

# ---------- INDUSTRIAL ----------
def planchas_remachadas():
    def r(x, y):
        if x % 8 == 7 or y % 8 == 7: return None
        return (x // 8, y // 8)
    im = piezas(r, ACERO, (30, 32, 36), 7); p = im.load()
    for bx in (0, 8):
        for by in (0, 8):
            for dx, dy in ((1, 1), (5, 1), (1, 5), (5, 5)):
                p[bx + dx, by + dy] = ACERO[6] + (255,)
                p[bx + dx + 1, by + dy + 1] = ACERO[1] + (255,)
    return im

def chapa_estriada():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 8)
            p[x, y] = tono(ACERO, 3 + (n > 0.7) - (n < 0.2)) + (255,)
    # rombos alargados en dos direcciones, alternos
    for cy in range(0, 16, 4):
        for cx in range(0, 16, 4):
            if ((cx + cy) // 4) % 2 == 0:
                pts = [(cx, cy + 2), (cx + 1, cy + 1), (cx + 2, cy)]
            else:
                pts = [(cx, cy), (cx + 1, cy + 1), (cx + 2, cy + 2)]
            for i, (x, y) in enumerate(pts):
                p[x % 16, y % 16] = (ACERO[6] if i == 0 else ACERO[5]) + (255,)
                p[(x + 1) % 16, (y + 1) % 16] = ACERO[1] + (255,)
    return im

def rejilla():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            borde = x in (0, 15) or y in (0, 15)
            barra = x % 5 == 0 or y % 5 == 0
            if borde or barra:
                n = ruido(x, y, 10)
                t = 3 if (y % 5 == 0 or y in (0, 15)) else 2
                if borde: t = 4 if (x == 0 or y == 0) else 1
                p[x, y] = tono(ACERO, t + (n > 0.8)) + (255,)
    return im

def aviso():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 11)
            if x in (0, 15) or y in (0, 15):
                p[x, y] = tono(ACERO, 4 if (x == 0 or y == 0) else 1) + (255,)
            elif ((x + y) // 4) % 2 == 0:
                p[x, y] = tono(AMARILLO, 2 + (n > 0.8) - (n < 0.15)) + (255,)
            else:
                p[x, y] = tono(NEGRO, 1 + (n > 0.7)) + (255,)
    return im

# ---------- MOSAICO ----------
CREMA = [(176, 150, 120), (200, 176, 146), (222, 202, 172), (236, 220, 194), (246, 236, 216)]
BARRO = [(96, 40, 24), (128, 56, 34), (156, 74, 44), (180, 94, 58), (200, 116, 76)]
PARDO = [(56, 36, 26), (74, 48, 34), (92, 62, 44), (112, 78, 56)]
AZUL_AZ = [(22, 46, 120), (36, 70, 160), (64, 104, 196), (120, 156, 224)]

def damero():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 20)
            if x % 4 == 3 or y % 4 == 3:
                p[x, y] = (110, 92, 76, 255); continue           # junta fina
            pal = CREMA if ((x // 4) + (y // 4)) % 2 == 0 else BARRO
            t = 3 if (x % 4 == 0 or y % 4 == 0) else 2
            p[x, y] = tono(pal, t + (n > 0.85) - (n < 0.1)) + (255,)
    return im

def mosaico_rombos():
    """Mosaico de rombos (arlequín): rombos alternos crema y barro, con juntas finas en diagonal."""
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            u, v = (x + y) % 16, (x - y) % 16
            n = ruido(x, y, 21)
            if u % 8 == 0 or v % 8 == 0:
                p[x, y] = tono(PARDO, 1) + (255,); continue
            celda = (((x + y) // 8) + ((x - y + 16) // 8)) % 2
            pal = CREMA if celda == 0 else BARRO
            t = 3 if (u % 8 == 1 or v % 8 == 1) else 2
            p[x, y] = tono(pal, t + (n > 0.85) - (n < 0.1)) + (255,)
    return im

def azulejo():
    """Azulejo blanco con dibujo azul: estrella en el centro y cuartos de círculo en las esquinas
    (al juntar cuatro, las esquinas forman flores)."""
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 22)
            c = tono(CREMA, 4 - (n < 0.15))
            cx, cy = x - 7.5, y - 7.5
            # estrella central (rombo + cruz)
            if abs(cx) + abs(cy) <= 4.2 and not (abs(cx) + abs(cy) <= 2.2):
                c = tono(AZUL_AZ, 1 + (abs(cx) + abs(cy) > 3.5))
            if abs(cx) + abs(cy) <= 1.2:
                c = tono(AZUL_AZ, 3)
            if (abs(cx) < 0.6 or abs(cy) < 0.6) and 4.2 < max(abs(cx), abs(cy)) <= 5.6:
                c = tono(AZUL_AZ, 2)
            # cuartos de círculo en las esquinas
            for ex, ey in ((0, 0), (15, 0), (0, 15), (15, 15)):
                d = ((x - ex) ** 2 + (y - ey) ** 2) ** 0.5
                if 2.5 <= d <= 4.0: c = tono(AZUL_AZ, 1)
                if d < 1.5: c = tono(AZUL_AZ, 0)
            if x in (0, 15) or y in (0, 15):
                c = c if c[2] > c[0] else tono(CREMA, 2)        # borde del azulejo
            p[x, y] = c + (255,)
    return im

# ---------- CRISTALERÍA ----------
PLOMO = (36, 36, 42)

def vidriera():
    """Vidriera emplomada en rombos de colores (translúcida)."""
    im = lienzo(); p = im.load()
    colores = [(70, 110, 220), (210, 60, 60), (230, 180, 50), (60, 170, 90)]
    for y in range(16):
        for x in range(16):
            u, v = (x + y) % 8, (x - y) % 8
            if u == 0 or v == 0 or x in (0, 15) or y in (0, 15):
                p[x, y] = PLOMO + (255,); continue
            celda = ((x + y) // 8 + (x - y + 16) // 8) % 4
            r, g, b = colores[celda]
            n = ruido(x, y, 23)
            brillo = 30 if (u == 1 or v == 1) else (-20 if n < 0.2 else 0)
            p[x, y] = (min(255, r + brillo), min(255, g + brillo), min(255, b + brillo), 170)
    return im

def cristal_esmerilado():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 24)
            if x in (0, 15) or y in (0, 15):
                p[x, y] = (226, 234, 238, 220); continue
            v = 210 + int(n * 40)
            p[x, y] = (v, min(255, v + 8), min(255, v + 14), 150 + int(n * 40))
    return im

def claraboya():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 25)
            marco = x in (0, 1, 14, 15) or y in (0, 1, 14, 15) or x in (7, 8) or y in (7, 8)
            if marco:
                t = 4 if (x in (0, 7) or y in (0, 7)) else 2
                p[x, y] = tono(ACERO, t + (n > 0.8)) + (255,)
            else:
                brillo = ((x % 7) - (y % 7)) in (0, 1)
                p[x, y] = (230, 245, 255, 120) if brillo else (190, 215, 230, 55)
    for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14), (7, 7), (8, 8)):
        p[x, y] = ACERO[6] + (255,)
    return im

# ---------- LUCARITA ----------
VAZ = [(41, 67, 150), (80, 120, 215), (140, 180, 245)]
VRO = [(150, 26, 34), (210, 60, 54), (240, 120, 100)]

def lucarita_pulida():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 26)
            if x == 0 or y == 0: t = 5
            elif x == 15 or y == 15: t = 0
            elif x == 1 or y == 1: t = 4
            elif x == 14 or y == 14: t = 1
            else: t = 3 + (n > 0.8) - (n < 0.2)
            p[x, y] = tono(COBRE, t) + (255,)
    # vetas que serpentean (azul y roja), como en el lingote
    for i, (x, y) in enumerate([(3, 4), (4, 5), (5, 5), (6, 6), (7, 6), (8, 7), (9, 8), (10, 8), (11, 9)]):
        p[x, y] = tono(VAZ, 1 + (i % 3 == 0)) + (255,); p[x, y + 1] = VAZ[0] + (255,)
    for i, (x, y) in enumerate([(4, 11), (5, 10), (6, 10), (7, 11), (8, 11), (9, 12), (10, 12), (11, 11), (12, 11)]):
        p[x, y] = tono(VRO, 1 + (i % 3 == 0)) + (255,); p[x, y - 1] = VRO[0] + (255,)
    for (x, y) in ((5, 8), (10, 5), (12, 13)):
        p[x, y] = COBRE[5] + (255,)
    return im

def ladrillos_lucarita():
    def r(x, y):
        fila = y // 4
        desp = 4 if fila % 2 else 0
        if y % 4 == 3 or (x + desp) % 8 == 7: return None
        return (fila, ((x + desp) % 16) // 8)
    im = piezas(r, COBRE, (60, 28, 4), 27); p = im.load()
    for (x, y), c in (((2, 1), VAZ[1]), ((3, 1), VAZ[2]), ((10, 5), VRO[1]), ((11, 5), VRO[2]), ((5, 9), VAZ[1]), ((13, 13), VRO[1])):
        p[x, y] = c + (255,)
    return im

def lampara_lucarita():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 28)
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6.5:                                          # marco de cobre
                p[x, y] = tono(COBRE, (4 if (x == 0 or y == 0) else 1) + (n > 0.8)) + (255,)
            elif d > 5.5:
                p[x, y] = tono(COBRE, 2) + (255,)
            else:                                                # luz: del centro amarillo claro a naranja
                r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
                if r < 2: c = (255, 248, 210)
                elif r < 3.5: c = (255, 222, 140)
                elif r < 5: c = (255, 186, 80)
                else: c = (240, 146, 50)
                if n > 0.9: c = (255, 240, 190)
                p[x, y] = c + (255,)
    for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):           # remaches con las gemas
        p[x, y] = (VAZ[1] if x == y else VRO[1]) + (255,)
    return im

# ---------- EL BANCO ----------
def banco_arriba():
    im = planchas_remachadas(); p = im.load()
    for x in range(3, 13):                                     # ranura de la sierra
        p[x, 7] = NEGRO[0] + (255,); p[x, 8] = NEGRO[1] + (255,)
    for x in range(5, 11):                                     # disco de sierra
        p[x, 6] = ACERO[6] + (255,)
    for (x, y) in ((1, 1), (2, 1), (1, 2), (14, 13), (13, 14), (14, 14)):
        p[x, y] = COBRE[4] + (255,)
    return im

def banco_lado():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            n = ruido(x, y, 12)
            if y < 3:                                          # tablero
                t = 5 if y == 0 else (3 if y == 1 else 1)
                p[x, y] = tono(ACERO, t + (n > 0.85)) + (255,)
            elif x < 2 or x > 13 or y > 13:                   # patas y travesaño
                p[x, y] = tono(ACERO, (4 if x == 0 else 2) + (n > 0.8) - (n < 0.2)) + (255,)
            elif 3 <= x <= 12 and 5 <= y <= 11:                # panel de cobre (lucarita)
                t = 4 if (x == 3 or y == 5) else (1 if (x == 12 or y == 11) else 3)
                p[x, y] = tono(COBRE, t + (n > 0.85)) + (255,)
            else:
                p[x, y] = tono(ACERO, 1 + (n > 0.7)) + (255,)
    for (x, y) in ((5, 7), (6, 7), (9, 9), (10, 9)):            # vetas azul y roja
        p[x, y] = ((80, 120, 215) if y == 7 else (225, 70, 60)) + (255,)
    return im

def banco_abajo():
    im = lienzo(); p = im.load()
    for y in range(16):
        for x in range(16):
            p[x, y] = tono(ACERO, 1 + (ruido(x, y, 13) > 0.7)) + (255,)
    return im

# ---------- DATOS ----------
def escribir(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, 'w', encoding='utf-8') as f:
        json.dump(datos, f, indent=2, ensure_ascii=False)

def modelo(nombre, datos):
    escribir(os.path.join(ASSETS, 'models', 'block', nombre + '.json'), datos)

def estado(nombre, datos):
    escribir(os.path.join(ASSETS, 'blockstates', nombre + '.json'), datos)

def objeto(nombre, padre):
    escribir(os.path.join(ASSETS, 'models', 'item', nombre + '.json'), {"parent": "yonga:block/" + padre})

def botin(nombre, losa=False):
    entrada = {"type": "minecraft:item", "name": "yonga:" + nombre}
    if losa:
        entrada["functions"] = [{"function": "minecraft:set_count", "count": 2, "add": False,
                                 "conditions": [{"condition": "minecraft:block_state_property", "block": "yonga:" + nombre,
                                                 "properties": {"type": "double"}}]}, {"function": "minecraft:explosion_decay"}]
    escribir(os.path.join(DATA, 'loot_tables', 'blocks', nombre + '.json'),
             {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [entrada], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

def cubo(nombre, textura=None, render=None):
    m = {"parent": "minecraft:block/cube_all", "textures": {"all": "yonga:block/" + (textura or nombre)}}
    if render: m["render_type"] = render
    modelo(nombre, m)
    estado(nombre, {"variants": {"": {"model": "yonga:block/" + nombre}}})
    objeto(nombre, nombre); botin(nombre)

def losa(nombre, base):
    t = "yonga:block/" + base
    modelo(nombre, {"parent": "minecraft:block/slab", "textures": {"bottom": t, "top": t, "side": t}})
    modelo(nombre + "_arriba", {"parent": "minecraft:block/slab_top", "textures": {"bottom": t, "top": t, "side": t}})
    estado(nombre, {"variants": {"type=bottom": {"model": "yonga:block/" + nombre}, "type=top": {"model": "yonga:block/" + nombre + "_arriba"},
                                 "type=double": {"model": "yonga:block/" + base}}})
    objeto(nombre, nombre); botin(nombre, losa=True)

def escaleras(nombre, base):
    t = "yonga:block/" + base
    for sufijo, padre in (("", "stairs"), ("_interior", "inner_stairs"), ("_exterior", "outer_stairs")):
        modelo(nombre + sufijo, {"parent": "minecraft:block/" + padre, "textures": {"bottom": t, "top": t, "side": t}})
    variantes = {}
    giro = {"east": 0, "south": 90, "west": 180, "north": 270}
    for facing, y0 in giro.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                m = nombre + ("" if shape == "straight" else ("_interior" if shape.startswith("inner") else "_exterior"))
                y = y0
                if shape in ("inner_left", "outer_left"): y = (y - 90) % 360
                x = 0
                if half == "top":
                    x = 180
                    if shape in ("inner_right", "outer_right"): y = (y + 90) % 360
                    elif shape in ("inner_left", "outer_left"): y = (y + 90) % 360
                v = {"model": "yonga:block/" + m}
                if x: v["x"] = x
                if y: v["y"] = y
                if x or y: v["uvlock"] = True
                variantes[f"facing={facing},half={half},shape={shape}"] = v
    estado(nombre, {"variants": variantes})
    objeto(nombre, nombre); botin(nombre)

def receta(nombre, ingrediente, rango, cantidad=1):
    ing = {"tag": ingrediente[1:]} if ingrediente.startswith('#') else {"item": ingrediente}
    escribir(os.path.join(DATA, 'recipes', 'banco', nombre + '.json'),
             {"conditions": [{"type": "yonga:modulo_activo", "modulo": "roles"}],
              "type": "yonga:banco_ingeniero", "ingredient": ing, "result": "yonga:" + nombre, "count": cantidad, "rango": rango})

def textura(nombre, im):
    ruta = os.path.join(ASSETS, 'textures', 'block', nombre + '.png')
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    im.save(ruta)

PIEDRAS = ["minecraft:stone", "minecraft:cobblestone", "minecraft:stone_bricks", "minecraft:andesite", "minecraft:polished_andesite"]
ARENISCAS = ["minecraft:sandstone", "minecraft:smooth_sandstone", "minecraft:cut_sandstone", "minecraft:smooth_stone"]

# (nombre, dibujo, losa+escaleras, entrada, rango del banco, cantidad)
FAMILIAS = [
    ("adoquin_calle", adoquin_calle, True, PIEDRAS, "todos", 1),
    ("adoquin_espiga", adoquin_espiga, True, PIEDRAS, "todos", 1),
    ("baldosa_acera", baldosa_acera, False, PIEDRAS, "todos", 1),
    ("sillares", sillares, True, ARENISCAS, "novato", 1),
    ("sillar_moldura", sillar_moldura, False, ARENISCAS, "novato", 1),
    ("planchas_remachadas", planchas_remachadas, True, ["minecraft:iron_block"], "aprendiz", 4),
    ("chapa_estriada", chapa_estriada, True, ["minecraft:iron_block"], "aprendiz", 4),
    ("aviso", aviso, False, ["minecraft:iron_block"], "aprendiz", 4),
    ("damero", damero, True, ["minecraft:terracotta", "minecraft:white_terracotta"], "oficial", 1),
    ("mosaico_rombos", mosaico_rombos, False, ["minecraft:terracotta", "minecraft:white_terracotta"], "oficial", 1),
    ("azulejo", azulejo, False, ["minecraft:terracotta", "minecraft:white_terracotta"], "oficial", 1),
    ("lucarita_pulida", lucarita_pulida, False, ["yonga:lucarita"], "maestro", 8),
    ("ladrillos_lucarita", ladrillos_lucarita, True, ["yonga:lucarita"], "maestro", 8),
    ("lampara_lucarita", lampara_lucarita, False, ["yonga:lucarita"], "maestro", 4),
]

# Cristalería (translúcidos, se rompen a mano como el cristal)
CRISTALES = [("vidriera", vidriera), ("cristal_esmerilado", cristal_esmerilado), ("claraboya", claraboya)]

# ================= ESTILOS ARQUITECTÓNICOS =================
# (nombre, dibujo, tipo, losa y escaleras, material, entradas, rango, cantidad, nombre en español, en inglés)
#   tipo: "cubo", "pilar" (como un tronco; su tapa es <nombre>_tapa) o "cristal" (translúcido)
#   material: piedra, calcita, ladrillo, barro, hormigon, ceramica, madera, lana, cristal, piedra_negra, arenisca
PIEDRAS_GOTICAS = ["minecraft:stone_bricks", "minecraft:deepslate_bricks", "minecraft:cobbled_deepslate"]
CALIZAS = ["minecraft:calcite", "minecraft:quartz_block", "minecraft:white_concrete"]
ARENISCAS_TODAS = ["minecraft:sandstone", "minecraft:smooth_sandstone", "minecraft:cut_sandstone"]
ESTILOS = [
    # Neolítico y antiguo
    ("piedra_seca", E.piedra_seca, "cubo", True, "piedra", ["minecraft:cobblestone", "minecraft:stone", "minecraft:mossy_cobblestone"], "todos", 1, "Muro de piedra seca", "Dry Stone Wall"),
    ("adobe", E.adobe, "cubo", True, "barro", ["minecraft:packed_mud", "minecraft:mud_bricks"], "todos", 1, "Adobe", "Adobe"),
    ("megalito", E.megalito, "cubo", False, "piedra", ["minecraft:stone", "minecraft:andesite", "minecraft:cobblestone"], "novato", 1, "Piedra megalítica", "Megalith Stone"),
    # Clásico (templos griegos y romanos)
    ("marmol", E.marmol, "cubo", True, "calcita", ["minecraft:calcite", "minecraft:diorite", "minecraft:quartz_block"], "aprendiz", 1, "Mármol", "Marble"),
    ("columna_marmol", E.columna_marmol, "pilar", False, "calcita", ["minecraft:calcite", "minecraft:diorite", "minecraft:quartz_block"], "aprendiz", 1, "Columna de mármol", "Marble Column"),
    ("friso_griego", E.friso_griego, "cubo", False, "calcita", ["minecraft:calcite", "minecraft:diorite", "minecraft:quartz_block"], "aprendiz", 1, "Friso de greca", "Greek Key Frieze"),
    ("ladrillo_romano", E.ladrillo_romano, "cubo", True, "ladrillo", ["minecraft:bricks", "minecraft:terracotta"], "aprendiz", 1, "Ladrillo romano", "Roman Brick"),
    # Moderno (casas modernas)
    ("hormigon_pulido", E.hormigon_pulido, "cubo", True, "hormigon", ["minecraft:light_gray_concrete", "minecraft:gray_concrete", "minecraft:smooth_stone"], "aprendiz", 1, "Hormigón pulido", "Polished Concrete"),
    ("lamas_madera", E.lamas_madera, "cubo", False, "madera", ["#minecraft:planks"], "aprendiz", 1, "Lamas de madera", "Wood Slats"),
    ("azulejo_metro", E.azulejo_metro, "cubo", False, "ceramica", ["minecraft:white_concrete", "minecraft:white_terracotta", "minecraft:calcite"], "aprendiz", 1, "Azulejo de metro", "Subway Tile"),
    ("revoco_blanco", E.revoco_blanco, "cubo", True, "hormigon", ["minecraft:white_concrete", "minecraft:calcite"], "aprendiz", 1, "Revoco blanco", "White Render"),
    ("ventanal", E.ventanal, "cristal", False, "cristal", ["minecraft:glass"], "aprendiz", 1, "Ventanal", "Picture Window"),
    # Gótico (iglesias y catedrales)
    ("piedra_gotica", E.piedra_gotica, "cubo", True, "piedra", PIEDRAS_GOTICAS, "oficial", 1, "Piedra gótica", "Gothic Stone"),
    ("traceria", E.traceria, "cubo", False, "piedra", PIEDRAS_GOTICAS, "oficial", 1, "Tracería gótica", "Gothic Tracery"),
    ("roseton", E.roseton, "cristal", False, "cristal", ["minecraft:glass"], "oficial", 1, "Rosetón", "Rose Window"),
    ("suelo_ajedrez", E.suelo_ajedrez, "cubo", True, "calcita", ["minecraft:calcite", "minecraft:quartz_block"], "oficial", 1, "Suelo de mármol ajedrezado", "Checkered Marble Floor"),
    # Renacentista
    ("almohadillado", E.almohadillado, "cubo", True, "piedra", ARENISCAS_TODAS + ["minecraft:stone_bricks"], "oficial", 1, "Sillar almohadillado", "Rusticated Stone"),
    ("casetonado", E.casetonado, "cubo", False, "piedra", ARENISCAS_TODAS + ["minecraft:stone_bricks"], "oficial", 1, "Casetón", "Coffered Panel"),
    # Neoclásico
    ("estuco_moldura", E.estuco_moldura, "cubo", False, "calcita", CALIZAS, "oficial", 1, "Estuco con moldura", "Moulded Stucco"),
    ("denticulos", E.denticulos, "cubo", False, "calcita", CALIZAS, "oficial", 1, "Cornisa de dentículos", "Dentil Cornice"),
    ("pilastra", E.pilastra, "pilar", False, "calcita", CALIZAS, "oficial", 1, "Pilastra", "Pilaster"),
    # Barroco
    ("marmol_negro", E.marmol_negro, "cubo", True, "piedra_negra", ["minecraft:polished_blackstone", "minecraft:blackstone"], "experto", 1, "Mármol negro y oro", "Black and Gold Marble"),
    ("talla_dorada", E.talla_dorada, "cubo", False, "madera", ["minecraft:dark_oak_planks"], "experto", 1, "Talla dorada", "Gilded Carving"),
    ("damasco", E.damasco, "cubo", False, "lana", ["minecraft:red_wool"], "experto", 1, "Damasco rojo", "Red Damask"),
    # Rococó
    ("estuco_rosa", E.estuco_rosa, "cubo", False, "hormigon", ["minecraft:pink_concrete", "minecraft:pink_terracotta"], "experto", 1, "Estuco rococó", "Rococo Stucco"),
    ("panel_rococo", E.panel_rococo, "cubo", False, "hormigon", ["minecraft:light_blue_concrete", "minecraft:light_blue_terracotta"], "experto", 1, "Panel rococó", "Rococo Panel"),
    # Egipcio (templos)
    ("jeroglificos", E.jeroglificos, "cubo", False, "arenisca", ARENISCAS_TODAS + ["minecraft:chiseled_sandstone"], "experto", 1, "Arenisca con jeroglíficos", "Hieroglyph Sandstone"),
    # Art déco
    ("abanico_deco", E.abanico_deco, "cubo", False, "piedra_negra", ["minecraft:black_concrete", "minecraft:polished_blackstone"], "maestro", 1, "Abanico art déco", "Art Deco Fan"),
    ("zigzag_deco", E.zigzag_deco, "cubo", False, "piedra_negra", ["minecraft:black_concrete", "minecraft:polished_blackstone"], "maestro", 1, "Zigzag art déco", "Art Deco Zigzag"),
    ("marmol_verde", E.marmol_verde, "cubo", True, "piedra", ["minecraft:green_terracotta", "minecraft:green_concrete"], "maestro", 1, "Mármol verde", "Green Marble"),
]

COPIA_MATERIAL = {"piedra": "STONE_BRICKS", "calcita": "CALCITE", "ladrillo": "BRICKS", "barro": "MUD_BRICKS",
                  "hormigon": "LIGHT_GRAY_CONCRETE", "ceramica": "TERRACOTTA", "madera": "OAK_PLANKS", "lana": "RED_WOOL",
                  "cristal": "GLASS", "piedra_negra": "POLISHED_BLACKSTONE", "arenisca": "SANDSTONE"}
HERRAMIENTA = {"piedra": "pickaxe", "calcita": "pickaxe", "ladrillo": "pickaxe", "barro": "pickaxe", "hormigon": "pickaxe",
               "ceramica": "pickaxe", "piedra_negra": "pickaxe", "arenisca": "pickaxe", "madera": "axe"}

def java_estilos():
    """Escribe ConstruccionEstilos.java con el registro de todos los bloques de ESTILOS."""
    lineas = []
    for nombre, _, tipo, extras, material, *_ in ESTILOS:
        props = f'BlockBehaviour.Properties.copy(Blocks.{COPIA_MATERIAL[material]})'
        clase = {"cubo": "Block", "pilar": "RotatedPillarBlock", "cristal": "GlassBlock"}[tipo]
        lineas.append(f'        RegistryObject<Block> {nombre} = Construccion.bloque("{nombre}", () -> new {clase}({props}));')
        if extras:
            lineas.append(f'        Construccion.bloque("{nombre}_losa", () -> new SlabBlock({props}));')
            lineas.append(f'        Construccion.bloque("{nombre}_escaleras", () -> new StairBlock(() -> {nombre}.get().defaultBlockState(), {props}));')
    codigo = """package com.lucarita.yonga.modulos.roles.construccion;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;

/**
 * Bloques por estilos arquitectónicos (neolítico, clásico, moderno, gótico, renacentista, neoclásico,
 * barroco, rococó, egipcio y art déco).
 *
 * ¡NO LO EDITES A MANO! Lo escribe herramientas/generar_bloques.py a partir de su lista ESTILOS
 * (ahí están también sus dibujos, de qué se hacen en el banco, qué rango piden y sus nombres).
 */
final class ConstruccionEstilos {

    private ConstruccionEstilos() {}

    static void registrar() {
""" + "\n".join(lineas) + """
    }
}
"""
    ruta = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'forge-1.20.1-47.4.10-mdk', 'src', 'main', 'java',
                        'com', 'lucarita', 'yonga', 'modulos', 'roles', 'construccion', 'ConstruccionEstilos.java')
    with open(ruta, 'w', encoding='utf-8') as f:
        f.write(codigo)

def receta_banco(archivo, entrada, resultado, cantidad, rango):
    ing = {"tag": entrada[1:]} if entrada.startswith('#') else {"item": entrada}
    escribir(os.path.join(DATA, 'recipes', 'banco', archivo + '.json'),
             {"conditions": [{"type": "yonga:modulo_activo", "modulo": "roles"}], "type": "yonga:banco_ingeniero",
              "ingredient": ing, "result": "yonga:" + resultado, "count": cantidad, "rango": rango})

def generar_estilos(etiquetas, losas, escaleras_, nombres_es, nombres_en):
    for nombre, dibujo, tipo, extras, material, entradas, rango, cantidad, es, en in ESTILOS:
        textura(nombre, dibujo())
        if tipo == "pilar":
            tapa = getattr(E, nombre + "_tapa", None)
            textura(nombre + "_tapa", tapa() if tapa else dibujo())
            modelo(nombre, {"parent": "minecraft:block/cube_column", "textures": {"end": "yonga:block/" + nombre + "_tapa", "side": "yonga:block/" + nombre}})
            modelo(nombre + "_horizontal", {"parent": "minecraft:block/cube_column_horizontal", "textures": {"end": "yonga:block/" + nombre + "_tapa", "side": "yonga:block/" + nombre}})
            estado(nombre, {"variants": {"axis=y": {"model": "yonga:block/" + nombre}, "axis=z": {"model": "yonga:block/" + nombre + "_horizontal", "x": 90},
                                         "axis=x": {"model": "yonga:block/" + nombre + "_horizontal", "x": 90, "y": 90}}})
            objeto(nombre, nombre); botin(nombre)
        else:
            cubo(nombre, render="minecraft:translucent" if tipo == "cristal" else None)
        herramienta = HERRAMIENTA.get(material)
        nombres_es[nombre] = es; nombres_en[nombre] = en
        todos = [nombre]
        if extras:
            losa(nombre + "_losa", nombre); escaleras(nombre + "_escaleras", nombre)
            losas.append("yonga:" + nombre + "_losa"); escaleras_.append("yonga:" + nombre + "_escaleras")
            todos += [nombre + "_losa", nombre + "_escaleras"]
            nombres_es[nombre + "_losa"] = "Losa de " + es[0].lower() + es[1:]; nombres_en[nombre + "_losa"] = en + " Slab"
            nombres_es[nombre + "_escaleras"] = "Escaleras de " + es[0].lower() + es[1:]; nombres_en[nombre + "_escaleras"] = en + " Stairs"
        if herramienta:
            etiquetas.setdefault(herramienta, []).extend("yonga:" + t for t in todos)
        for entrada in entradas:
            origen = entrada.split(':')[1].replace('/', '_')
            receta_banco(f"{nombre}_desde_{origen}", entrada, nombre, cantidad, rango)
            if extras:
                receta_banco(f"{nombre}_losa_desde_{origen}", entrada, nombre + "_losa", 2 * cantidad, rango)
                receta_banco(f"{nombre}_escaleras_desde_{origen}", entrada, nombre + "_escaleras", cantidad, rango)
    java_estilos()

if __name__ == "__main__":
    etiqueta_pico, losas, escaleras_ = [], [], []
    for nombre, dibujo, con_extras, entradas, rango, cantidad in FAMILIAS:
        textura(nombre, dibujo())
        cubo(nombre)
        etiqueta_pico.append("yonga:" + nombre)
        if con_extras:
            losa(nombre + "_losa", nombre); escaleras(nombre + "_escaleras", nombre)
            etiqueta_pico += ["yonga:" + nombre + "_losa", "yonga:" + nombre + "_escaleras"]
            losas.append("yonga:" + nombre + "_losa"); escaleras_.append("yonga:" + nombre + "_escaleras")
        # una receta por entrada (mismo resultado)
        for entrada in entradas:
            escribir(os.path.join(DATA, 'recipes', 'banco', f"{nombre}_desde_{entrada.split(':')[1]}.json"),
                     {"conditions": [{"type": "yonga:modulo_activo", "modulo": "roles"}],
                      "type": "yonga:banco_ingeniero", "ingredient": {"item": entrada}, "result": "yonga:" + nombre,
                      "count": cantidad, "rango": rango})
        if con_extras:
            for entrada in entradas:
                for extra, n in (("_losa", 2), ("_escaleras", 1)):
                    escribir(os.path.join(DATA, 'recipes', 'banco', f"{nombre}{extra}_desde_{entrada.split(':')[1]}.json"),
                             {"conditions": [{"type": "yonga:modulo_activo", "modulo": "roles"}],
                              "type": "yonga:banco_ingeniero", "ingredient": {"item": entrada}, "result": "yonga:" + nombre + extra,
                              "count": n * cantidad, "rango": rango})
    # Columna (pilar: tapa y lado)
    textura("columna", columna_lado()); textura("columna_tapa", columna_tapa())
    modelo("columna", {"parent": "minecraft:block/cube_column", "textures": {"end": "yonga:block/columna_tapa", "side": "yonga:block/columna"}})
    modelo("columna_horizontal", {"parent": "minecraft:block/cube_column_horizontal", "textures": {"end": "yonga:block/columna_tapa", "side": "yonga:block/columna"}})
    estado("columna", {"variants": {"axis=y": {"model": "yonga:block/columna"}, "axis=z": {"model": "yonga:block/columna_horizontal", "x": 90},
                                    "axis=x": {"model": "yonga:block/columna_horizontal", "x": 90, "y": 90}}})
    objeto("columna", "columna"); botin("columna"); etiqueta_pico.append("yonga:columna")
    for entrada in ARENISCAS:
        escribir(os.path.join(DATA, 'recipes', 'banco', f"columna_desde_{entrada.split(':')[1]}.json"),
                 {"conditions": [{"type": "yonga:modulo_activo", "modulo": "roles"}], "type": "yonga:banco_ingeniero",
                  "ingredient": {"item": entrada}, "result": "yonga:columna", "count": 1, "rango": "novato"})
    # Rejilla (con huecos transparentes)
    textura("rejilla", rejilla()); cubo("rejilla", render="minecraft:cutout"); etiqueta_pico.append("yonga:rejilla")
    escribir(os.path.join(DATA, 'recipes', 'banco', "rejilla_desde_iron_block.json"),
             {"conditions": [{"type": "yonga:modulo_activo", "modulo": "roles"}], "type": "yonga:banco_ingeniero",
              "ingredient": {"item": "minecraft:iron_block"}, "result": "yonga:rejilla", "count": 4, "rango": "aprendiz"})
    # Cristalería
    for nombre, dibujo in CRISTALES:
        textura(nombre, dibujo()); cubo(nombre, render="minecraft:translucent")
        escribir(os.path.join(DATA, 'recipes', 'banco', f"{nombre}_desde_glass.json"),
                 {"conditions": [{"type": "yonga:modulo_activo", "modulo": "roles"}], "type": "yonga:banco_ingeniero",
                  "ingredient": {"item": "minecraft:glass"}, "result": "yonga:" + nombre, "count": 1, "rango": "experto"})
    # El banco
    textura("banco_ingeniero_arriba", banco_arriba()); textura("banco_ingeniero_lado", banco_lado()); textura("banco_ingeniero_abajo", banco_abajo())
    modelo("banco_ingeniero", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "yonga:block/banco_ingeniero_arriba", "side": "yonga:block/banco_ingeniero_lado", "bottom": "yonga:block/banco_ingeniero_abajo"}})
    estado("banco_ingeniero", {"variants": {"": {"model": "yonga:block/banco_ingeniero"}}})
    objeto("banco_ingeniero", "banco_ingeniero"); botin("banco_ingeniero"); etiqueta_pico.append("yonga:banco_ingeniero")
    escribir(os.path.join(DATA, 'recipes', 'banco_ingeniero.json'),
             {"conditions": [{"type": "yonga:modulo_activo", "modulo": "roles"}], "type": "yonga:receta_oficio", "oficio": "ingeniero",
              "rango": "novato", "category": "misc", "pattern": ["IPI", "LSL", "IPI"],
              "key": {"I": {"item": "minecraft:iron_block"}, "P": {"item": "minecraft:piston"}, "L": {"item": "yonga:lucarita"},
                      "S": {"item": "minecraft:stonecutter"}}, "result": {"item": "yonga:banco_ingeniero"}})
    # Estilos arquitectónicos
    etiquetas_estilos, nombres_es, nombres_en = {}, {}, {}
    generar_estilos(etiquetas_estilos, losas, escaleras_, nombres_es, nombres_en)
    etiqueta_pico += etiquetas_estilos.get("pickaxe", [])
    for idioma, nombres in (("es_es", nombres_es), ("en_us", nombres_en)):
        ruta = os.path.join(ASSETS, 'lang', idioma + '.json')
        datos = json.load(open(ruta, encoding='utf-8'))
        for k, v in nombres.items():
            datos["block.yonga." + k] = v
        escribir(ruta, datos)
    # Etiquetas de Minecraft: se pican con pico; losas y escaleras
    MC = os.path.join(RAIZ, 'data', 'minecraft', 'tags', 'blocks')
    def anadir(ruta, valores):
        datos = {"replace": False, "values": []}
        if os.path.exists(ruta):
            datos = json.load(open(ruta, encoding='utf-8'))
        for v in valores:
            if v not in datos["values"]: datos["values"].append(v)
        escribir(ruta, datos)
    anadir(os.path.join(MC, 'mineable', 'pickaxe.json'), etiqueta_pico)
    anadir(os.path.join(MC, 'mineable', 'axe.json'), etiquetas_estilos.get("axe", []))
    anadir(os.path.join(MC, 'slabs.json'), losas)
    anadir(os.path.join(MC, 'stairs.json'), escaleras_)
    MCI = os.path.join(RAIZ, 'data', 'minecraft', 'tags', 'items')
    anadir(os.path.join(MCI, 'slabs.json'), losas)
    anadir(os.path.join(MCI, 'stairs.json'), escaleras_)
    print("ok")
