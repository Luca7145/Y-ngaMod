"""Los útiles del Ingeniero: imán, foco (bloque que apunta), botas de muelles (icono y armadura puesta), trampa de
pinchos (bloque con los pinchos dentro o fuera) y escáner (tablet). Dibujos, modelos, estados de bloque, botín,
recetas de oficio, etiquetas (también las de Forge para la lucarita, que usa el escáner) y textos.
Uso: python3 dibujar_ingeniero.py <carpeta src/main/resources>"""
import json
import math
import os
import sys

from PIL import Image

CONDICION = [{"type": "yonga:modulo_activo", "modulo": "roles"}]
NADA = (0, 0, 0, 0)


def h(x, y, s):
    n = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    n = (n * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def c4(c):
    return c if len(c) == 4 else tuple(c) + (255,)


def desde_ascii(filas, colores, ancho=16):
    img = Image.new("RGBA", (ancho, len(filas)), NADA)
    for y, fila in enumerate(filas):
        assert len(fila) == ancho, (fila, len(fila))
        for x, ch in enumerate(fila):
            if ch in colores:
                img.putpixel((x, y), c4(colores[ch]))
    return img


# ---------------------------------------------------------------- iconos

IMAN = [
    "................", "................", "....kkkkkkkk....", "...kRRRRRRRRk...", "..krRkkkkkkrRk..",
    "..krRk....krRk..", "..krRk....krRk..", "..krRk....krRk..", "..krRk....krRk..", "..krRk....krRk..",
    "..kSsk....kSsk..", "..kSsk....kSsk..", "..kkkk....kkkk..", "................", "................",
    "................"]
COLORES_IMAN = {"k": (34, 24, 26), "R": (196, 40, 40), "r": (238, 98, 88), "S": (220, 224, 232), "s": (150, 156, 168)}

BOTAS = [
    "................", "................", "................", "...kkkk...kkkk..", "...kllk...kllk..",
    "...kLLk...kLLk..", "...kLLk...kLLk..", ".kkkLLk.kkkLLk..", ".kLLLLk.kLLLLk..", ".kIIIIk.kIIIIk..",
    "..kSSk...kSSk...", "..kssk...kssk...", "..kSSk...kSSk...", "..kssk...kssk...", ".kkkkkk.kkkkkk..",
    "................"]
COLORES_BOTAS = {"k": (30, 26, 24), "l": (156, 106, 64), "L": (120, 78, 44), "I": (164, 170, 180),
                 "S": (216, 220, 228), "s": (100, 106, 118)}


def iman_puesto():
    """Para el imán colgado en el pecho (CapaIman): arriba el cordón, en medio el rojo, abajo la plata."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if y < 4:
                c = (62, 58, 66) if (x + y) % 2 else (78, 74, 82)
            elif y < 12:
                c = (214, 52, 50) if h(x, y, 11) < 0.6 else (186, 36, 38)
            else:
                c = (224, 228, 236) if h(x, y, 12) < 0.6 else (176, 182, 194)
            img.putpixel((x, y), c4(c))
    return img


def escaner():
    """La tablet: marco oscuro, pantalla verde con los aros del sonar, una mancha y el botón de abajo."""
    img = Image.new("RGBA", (16, 16), NADA)
    for y in range(2, 15):
        for x in range(1, 15):
            borde = x in (1, 14) or y in (2, 14)
            img.putpixel((x, y), c4((24, 26, 30) if borde else (58, 62, 72)))
    for y in range(4, 12):
        for x in range(3, 13):
            d = math.hypot(x - 7.5, y - 7.5)
            c = (10, 30, 18)
            if abs(d - 3.6) < 0.55 or abs(d - 1.6) < 0.5:
                c = (40, 120, 70)
            img.putpixel((x, y), c4(c))
    for t in range(1, 4):                                    # la línea del barrido
        img.putpixel((7 + t, 7 - t + 1), c4((110, 255, 140)))
    img.putpixel((10, 5), c4((240, 40, 40)))                 # dos puntos rojos
    img.putpixel((5, 9), c4((240, 40, 40)))
    img.putpixel((7, 7), c4((255, 255, 255)))
    img.putpixel((7, 13), c4((110, 116, 128)))
    img.putpixel((8, 13), c4((110, 116, 128)))
    return img


# ---------------------------------------------------------------- la tablet en la mano (textures/misc)

def tablet():
    """72x72: cubre de -8 a 136 en las medidas del mapa (2 unidades por píxel). Marco gris, pantalla verde oscura con
    los aros del sonar, la cruz y la flecha del jugador en el centro."""
    n = 72
    img = Image.new("RGBA", (n, n))
    c = n / 2
    for y in range(n):
        for x in range(n):
            if x < 4 or y < 4 or x >= n - 4 or y >= n - 4:
                borde = x in (0, n - 1) or y in (0, n - 1)
                interior = x in (3, n - 4) or y in (3, n - 4)
                col = (26, 28, 32) if borde else ((88, 94, 106) if interior else (58, 62, 72))
            else:
                d = math.hypot(x + 0.5 - c, y + 0.5 - c)
                col = (8, 22, 14) if d <= 30.5 else (14, 18, 18)
                if d <= 30.5 and (abs(d - 30) < 0.6 or abs(d - 20) < 0.5 or abs(d - 10) < 0.5):
                    col = (30, 92, 56)
                if d <= 30 and (abs(x + 0.5 - c) < 0.6 or abs(y + 0.5 - c) < 0.6):
                    col = (20, 58, 36)
            img.putpixel((x, y), c4(col))
    for x, y in ((35, 34), (36, 34), (34, 35), (35, 35), (36, 35), (37, 35), (35, 36), (36, 36)):
        img.putpixel((x, y), c4((236, 240, 236)))           # tú, mirando arriba
    return img


def barrido():
    """64x64: la línea del barrido apuntando arriba y su estela hacia atrás (gira en el juego)."""
    n = 64
    img = Image.new("RGBA", (n, n), NADA)
    c = n / 2
    for y in range(n):
        for x in range(n):
            dx, dy = x + 0.5 - c, y + 0.5 - c
            d = math.hypot(dx, dy)
            if d > 30 or d < 0.5:
                continue
            ang = math.atan2(dx, -dy)                        # 0 = arriba, positivo = sentido de las agujas
            if -0.75 <= ang <= 0.03:
                alfa = int(150 * (1 + ang / 0.75)) if ang < -0.05 else 230
                img.putpixel((x, y), (110, 255, 140, max(0, alfa)))
    return img


def punto():
    img = Image.new("RGBA", (8, 8), NADA)
    for y in range(8):
        for x in range(8):
            d = math.hypot(x + 0.5 - 4, y + 0.5 - 4)
            if d < 3.0:
                img.putpixel((x, y), (255, 50, 40, 255) if d < 2.0 else (255, 60, 50, 150))
    return img


# ---------------------------------------------------------------- foco

def foco_lado(s):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = (62, 66, 76) if h(x, y, s) < 0.7 else (70, 74, 84)
            if x % 4 == 1 and 3 <= y <= 12:
                c = (34, 36, 42)                            # rejillas de ventilación
            if y in (0, 15) or x in (0, 15):
                c = (40, 42, 50)
            img.putpixel((x, y), c4(c))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        img.putpixel((x, y), c4((140, 146, 158)))           # remaches
    return img


def foco_frente(encendido):
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 6.6 or x in (0, 15) or y in (0, 15):
                c = (92, 96, 108)                           # el aro
            elif d > 5.8:
                c = (40, 42, 50)
            elif encendido:
                k = d / 5.8
                c = (255, int(252 - 40 * k), int(220 - 120 * k))
            else:
                c = (70, 86, 100) if (x + y) % 5 else (96, 116, 132)
            img.putpixel((x, y), c4(c))
    if not encendido:
        for i in range(3):                                   # un reflejo en el cristal
            img.putpixel((5 + i, 4 + i), c4((150, 170, 186)))
    return img


def foco_detras():
    img = foco_lado(9)
    for y in range(6, 10):
        for x in range(6, 10):
            img.putpixel((x, y), c4((20, 20, 24)))          # la salida del cable
    return img


def modelo_foco(encendido):
    frente = "yonga:block/foco_frente" + ("_encendido" if encendido else "")
    caras = lambda frontal: {
        "north": {"texture": "#frente" if frontal else "#lado"},
        "south": {"texture": "#detras"}, "east": {"texture": "#lado"}, "west": {"texture": "#lado"},
        "up": {"texture": "#lado"}, "down": {"texture": "#lado"}}
    return {
        "parent": "block/block",
        "textures": {"particle": "yonga:block/foco_lado", "lado": "yonga:block/foco_lado",
                     "frente": frente, "detras": "yonga:block/foco_detras"},
        "elements": [
            {"from": [3, 3, 5], "to": [13, 13, 16], "faces": caras(False)},           # la carcasa
            {"from": [1, 1, 1], "to": [15, 15, 5], "faces": caras(True)},             # la boca con el cristal
        ],
        "display": {"gui": {"rotation": [30, 135, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]}},
    }


def estados_foco():
    giros = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
    variantes = {}
    for cara, giro in giros.items():
        for lit in ("false", "true"):
            modelo = "yonga:block/foco" + ("_encendido" if lit == "true" else "")
            variantes[f"facing={cara},lit={lit}"] = dict({"model": modelo}, **giro)
    return {"variants": variantes}


# ---------------------------------------------------------------- trampa de pinchos

PINCHOS = (3, 8, 13)


def trampa_lado():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if y < 3:
                c = (232, 190, 30) if ((x + y) // 2) % 2 == 0 else (30, 28, 30)   # franja de aviso
            else:
                c = (88, 92, 100) if h(x, y, 3) < 0.7 else (78, 82, 90)
                if y in (3, 15) or x in (0, 15):
                    c = (54, 56, 62)
            img.putpixel((x, y), c4(c))
    for x, y in ((2, 6), (13, 6), (2, 12), (13, 12)):
        img.putpixel((x, y), c4((150, 156, 166)))
    return img


def trampa_arriba():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = (96, 100, 110) if h(x, y, 4) < 0.7 else (86, 90, 100)
            if y in (0, 15) or x in (0, 15):
                c = (54, 56, 62)
            img.putpixel((x, y), c4(c))
    for cx in PINCHOS:
        for cz in PINCHOS:
            for dx in (-1, 0):
                for dz in (-1, 0):
                    img.putpixel((cx + dx, cz + dz), c4((22, 22, 26)))
    return img


def trampa_abajo():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), c4((70, 74, 82) if h(x, y, 5) < 0.6 else (62, 66, 74)))
    return img


def pincho():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = y / 15
            base = (int(214 - 90 * k), int(220 - 90 * k), int(230 - 90 * k))
            img.putpixel((x, y), c4(base if x % 2 == 0 else tuple(v - 18 for v in base)))
    return img


def modelo_trampa(fuera):
    caja = {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
        "down": {"texture": "#abajo", "cullface": "down"}, "up": {"texture": "#arriba", "cullface": "up"},
        "north": {"texture": "#lado", "cullface": "north"}, "south": {"texture": "#lado", "cullface": "south"},
        "east": {"texture": "#lado", "cullface": "east"}, "west": {"texture": "#lado", "cullface": "west"}}}
    elementos = [caja]
    if fuera:
        for cx in PINCHOS:
            for cz in PINCHOS:
                lados = {d: {"texture": "#pincho"} for d in ("north", "south", "east", "west")}
                elementos.append({"from": [cx - 1, 16, cz - 1], "to": [cx + 1, 21, cz + 1],
                                  "faces": dict(lados, up={"texture": "#pincho"})})
                elementos.append({"from": [cx - 0.5, 21, cz - 0.5], "to": [cx + 0.5, 24, cz + 0.5],
                                  "faces": dict(lados, up={"texture": "#pincho"})})
    return {"parent": "block/block",
            "textures": {"particle": "yonga:block/trampa_pinchos_lado", "lado": "yonga:block/trampa_pinchos_lado",
                         "arriba": "yonga:block/trampa_pinchos_arriba", "abajo": "yonga:block/trampa_pinchos_abajo",
                         "pincho": "yonga:block/trampa_pinchos_pincho"},
            "elements": elementos}


# ---------------------------------------------------------------- botas puestas (64x32, como las de Minecraft)

def armadura_muelles():
    """Solo la parte de las piernas de abajo (las botas): x 0-16, y 16-32 de la capa 1."""
    img = Image.new("RGBA", (64, 32), NADA)
    cuero, cuero_os, hierro, plata, plata_os = (120, 78, 44), (92, 58, 32), (164, 170, 180), (216, 220, 228), (100, 106, 118)
    for y in range(16, 32):
        for x in range(0, 16):
            if y < 20:                                       # tapas de arriba y abajo de la pierna
                if 8 <= x < 12:
                    img.putpixel((x, y), c4(hierro))         # la suela
                continue
            fila = y - 20                                    # 0 = rodilla, 11 = pie
            if fila < 5:
                continue
            if fila in (5, 6):
                c = cuero_os if fila == 5 else cuero
            elif fila == 7:
                c = hierro
            elif fila < 11:
                c = plata if (fila % 2 == 0) else plata_os  # el muelle
            else:
                c = hierro
            img.putpixel((x, y), c4(c))
    return img


# ---------------------------------------------------------------- escribir

def guardar(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(datos, f, indent=2, ensure_ascii=False)
        f.write("\n")


def leer(ruta):
    with open(ruta, encoding="utf-8") as f:
        return json.load(f)


def receta(rango, categoria, patron, clave, resultado, cantidad=1):
    return {"conditions": CONDICION, "type": "yonga:receta_oficio", "oficio": "ingeniero", "rango": rango,
            "category": categoria, "pattern": patron,
            "key": {k: ({"tag": v[1:]} if v.startswith("#") else {"item": v}) for k, v in clave.items()},
            "result": {"item": resultado, "count": cantidad}}


def botin(nombre):
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": "yonga:" + nombre}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]}


def anadir_a_etiqueta(ruta, valores):
    datos = leer(ruta) if os.path.exists(ruta) else {"replace": False, "values": []}
    presentes = {v if isinstance(v, str) else v["id"] for v in datos["values"]}
    for v in valores:
        if v not in presentes:
            datos["values"].append({"id": v, "required": False})
    guardar(ruta, datos)


TEXTOS = {
    "es_es.json": {
        "item.yonga.iman": "Imán",
        "block.yonga.foco": "Foco",
        "item.yonga.botas_muelles": "Botas de muelles",
        "block.yonga.trampa_pinchos": "Trampa de pinchos",
        "item.yonga.escaner": "Escáner",     },
    "en_us.json": {
        "item.yonga.iman": "Magnet",
        "block.yonga.foco": "Spotlight",
        "item.yonga.botas_muelles": "Spring Boots",
        "block.yonga.trampa_pinchos": "Spike Trap",
        "item.yonga.escaner": "Scanner",     },
}


def main(raiz):
    a = os.path.join(raiz, "assets", "yonga")
    d = os.path.join(raiz, "data", "yonga")
    ti, tb = os.path.join(a, "textures", "item"), os.path.join(a, "textures", "block")

    # Iconos
    desde_ascii(IMAN, COLORES_IMAN).save(os.path.join(ti, "iman.png"))
    desde_ascii(BOTAS, COLORES_BOTAS).save(os.path.join(ti, "botas_muelles.png"))
    escaner().save(os.path.join(ti, "escaner.png"))
    tm = os.path.join(a, "textures", "misc")
    os.makedirs(tm, exist_ok=True)
    tablet().save(os.path.join(tm, "escaner_tablet.png"))
    barrido().save(os.path.join(tm, "escaner_barrido.png"))
    punto().save(os.path.join(tm, "escaner_punto.png"))
    Image.new("RGBA", (4, 4), (255, 255, 255, 255)).save(os.path.join(tm, "escaner_blanco.png"))
    for n in ("iman", "botas_muelles", "escaner"):
        guardar(os.path.join(a, "models", "item", n + ".json"),
                {"parent": "minecraft:item/generated", "textures": {"layer0": "yonga:item/" + n}})
    os.makedirs(os.path.join(a, "textures", "entity"), exist_ok=True)
    iman_puesto().save(os.path.join(a, "textures", "entity", "iman.png"))
    os.makedirs(os.path.join(a, "textures", "models", "armor"), exist_ok=True)
    armadura_muelles().save(os.path.join(a, "textures", "models", "armor", "muelles_layer_1.png"))

    # Foco
    foco_lado(7).save(os.path.join(tb, "foco_lado.png"))
    foco_detras().save(os.path.join(tb, "foco_detras.png"))
    foco_frente(False).save(os.path.join(tb, "foco_frente.png"))
    foco_frente(True).save(os.path.join(tb, "foco_frente_encendido.png"))
    guardar(os.path.join(a, "models", "block", "foco.json"), modelo_foco(False))
    guardar(os.path.join(a, "models", "block", "foco_encendido.json"), modelo_foco(True))
    guardar(os.path.join(a, "blockstates", "foco.json"), estados_foco())
    guardar(os.path.join(a, "models", "item", "foco.json"), {"parent": "yonga:block/foco_encendido"})

    # Trampa de pinchos
    trampa_lado().save(os.path.join(tb, "trampa_pinchos_lado.png"))
    trampa_arriba().save(os.path.join(tb, "trampa_pinchos_arriba.png"))
    trampa_abajo().save(os.path.join(tb, "trampa_pinchos_abajo.png"))
    pincho().save(os.path.join(tb, "trampa_pinchos_pincho.png"))
    guardar(os.path.join(a, "models", "block", "trampa_pinchos.json"), modelo_trampa(False))
    guardar(os.path.join(a, "models", "block", "trampa_pinchos_fuera.json"), modelo_trampa(True))
    guardar(os.path.join(a, "blockstates", "trampa_pinchos.json"), {"variants": {
        "fuera=false": {"model": "yonga:block/trampa_pinchos"}, "fuera=true": {"model": "yonga:block/trampa_pinchos_fuera"}}})
    guardar(os.path.join(a, "models", "item", "trampa_pinchos.json"), {"parent": "yonga:block/trampa_pinchos_fuera"})

    # Botín y herramienta
    for n in ("foco", "trampa_pinchos"):
        guardar(os.path.join(d, "loot_tables", "blocks", n + ".json"), botin(n))
    anadir_a_etiqueta(os.path.join(raiz, "data", "minecraft", "tags", "blocks", "mineable", "pickaxe.json"),
                      ["yonga:foco", "yonga:trampa_pinchos"])

    # Etiquetas de Forge de la lucarita (para el escáner y para otros mods, como la trituradora de Create)
    f = os.path.join(raiz, "data", "forge", "tags")
    guardar(os.path.join(f, "blocks", "ores", "lucarita.json"), {"replace": False, "values": ["yonga:lucarita_ore"]})
    anadir_a_etiqueta(os.path.join(f, "blocks", "ores.json"), ["#forge:ores/lucarita"])
    guardar(os.path.join(f, "items", "ores", "lucarita.json"), {"replace": False, "values": ["yonga:lucarita_ore"]})
    guardar(os.path.join(f, "items", "ingots", "lucarita.json"), {"replace": False, "values": ["yonga:lucarita"]})
    guardar(os.path.join(f, "items", "raw_materials", "lucarita.json"), {"replace": False, "values": ["yonga:raw_lucarita"]})

    # Recetas (de Ingeniero, cada una desde su rango)
    r = os.path.join(d, "recipes")
    guardar(os.path.join(r, "iman.json"), receta("novato", "equipment", ["I I", "R R", "I I"],
            {"I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, "yonga:iman"))
    guardar(os.path.join(r, "foco.json"), receta("aprendiz", "misc", ["IGI", "IOI", "IRI"],
            {"I": "minecraft:iron_ingot", "G": "minecraft:glass", "O": "minecraft:glowstone", "R": "minecraft:redstone"},
            "yonga:foco"))
    guardar(os.path.join(r, "botas_muelles.json"), receta("oficial", "equipment", ["SBS", "P P"],
            {"S": "minecraft:slime_ball", "B": "minecraft:iron_boots", "P": "minecraft:piston"}, "yonga:botas_muelles"))
    guardar(os.path.join(r, "trampa_pinchos.json"), receta("oficial", "misc", ["III", "SPS", "SRS"],
            {"I": "minecraft:iron_ingot", "S": "minecraft:stone", "P": "minecraft:piston", "R": "minecraft:redstone"},
            "yonga:trampa_pinchos", 2))
    guardar(os.path.join(r, "escaner.json"), receta("experto", "equipment", ["IGI", "LCL", "IBI"],
            {"I": "minecraft:iron_ingot", "G": "minecraft:glass_pane", "L": "yonga:lucarita", "C": "minecraft:compass",
             "B": "minecraft:redstone_block"}, "yonga:escaner"))

    # Textos
    for f_, textos in TEXTOS.items():
        ruta = os.path.join(a, "lang", f_)
        datos = leer(ruta)
        datos.update(textos)
        guardar(ruta, datos)


if __name__ == "__main__":
    main(sys.argv[1])
