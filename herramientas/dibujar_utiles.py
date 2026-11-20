"""Los útiles de oficio nuevos: los incensarios de suelo del Sacerdote (luminoso y oscuro, apagados y encendidos), la
el garfio del Explorador (entero, sin gancho y el gancho solo). Dibujos, modelos,
estados de bloque, botín, recetas, etiquetas y textos.
Uso: python3 dibujar_utiles.py <carpeta src/main/resources>"""
import json
import math
import os
import sys

from PIL import Image

CONDICION = [{"type": "yonga:modulo_activo", "modulo": "roles"}]
NADA = (0, 0, 0, 0)


def h(x, y, s):
    n = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def c4(c):
    return c if len(c) == 4 else c + (255,)


# ---------------------------------------------------------------- incensarios

PALETAS = {
    "luminoso": {
        "metal": [(228, 184, 62), (210, 166, 50), (236, 194, 74)], "claro": (250, 216, 108), "oscuro": (176, 132, 38),
        "hondo": (112, 80, 24), "adorno": (240, 234, 218), "agujero": (58, 38, 18),
        "brasa": [(255, 244, 196), (255, 214, 110), (250, 170, 60)], "ceniza": [(92, 86, 80), (70, 64, 60)],
    },
    "oscuro": {
        "metal": [(60, 54, 68), (52, 46, 60), (68, 61, 77)], "claro": (98, 88, 110), "oscuro": (38, 34, 46),
        "hondo": (20, 18, 25), "adorno": (126, 72, 168), "agujero": (10, 8, 12),
        "brasa": [(170, 246, 250), (100, 214, 236), (52, 150, 196)], "ceniza": [(44, 42, 48), (30, 28, 34)],
    },
}


def metal(p, x, y, s):
    r = h(x, y, s)
    return p["metal"][0] if r < 0.6 else (p["metal"][1] if r < 0.8 else p["metal"][2])


def incensario_lado(p, encendido, s):
    """Lado. Las franjas van a la altura de cada pieza del modelo (UV automática):
    v 2 remate, v 3-4 tapa, v 5-8 cuenco, v 9-13 columna, v 14-15 pie."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), c4(metal(p, x, y, s)))
    luz = lambda i: c4(p["brasa"][i]) if encendido else c4(p["agujero"])
    # remate
    for x in range(16):
        img.putpixel((x, 2), c4(p["adorno"] if x % 2 == 0 else p["claro"]))
    # tapa: borde de arriba claro y agujeritos abajo
    for x in range(16):
        img.putpixel((x, 3), c4(p["claro"]))
        img.putpixel((x, 4), luz(1) if x % 2 == 1 else c4(p["oscuro"]))
    # cuenco: aro de arriba, cuerpo calado (rombos), aro de abajo
    for x in range(16):
        img.putpixel((x, 5), c4(p["claro"] if x % 4 else p["adorno"]))
        img.putpixel((x, 8), c4(p["oscuro"]))
    for x in range(16):
        k = x % 4
        if k == 1:
            img.putpixel((x, 6), luz(0))
            img.putpixel((x, 7), luz(2))
        elif k == 3:
            img.putpixel((x, 6), c4(p["adorno"]))
            img.putpixel((x, 7), c4(metal(p, x, 7, s)))
    # columna: luz a la izquierda, sombra a la derecha (u 7 y 8)
    for y in range(9, 14):
        for x in range(16):
            img.putpixel((x, y), c4(p["claro"] if x % 2 == 1 else p["oscuro"]))
        img.putpixel((7, y), c4(p["claro"]))
        img.putpixel((8, y), c4(p["oscuro"]))
    for x in range(16):
        img.putpixel((x, 11), c4(p["adorno"]))            # anillo a media columna
    # pie
    for x in range(16):
        img.putpixel((x, 14), c4(p["claro"]))
        img.putpixel((x, 15), c4(p["hondo"]))
    return img


def incensario_arriba(p, encendido, s):
    """Arriba. Borde del cuenco (u 4-12), dentro ceniza o brasas, y la tapa (u 6-10) con agujeros."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), c4(metal(p, x, y, s)))
    for y in range(4, 12):
        for x in range(4, 12):
            borde = x in (4, 11) or y in (4, 11)
            if borde:
                c = p["claro"] if (x + y) % 3 else p["adorno"]
            elif encendido:
                c = p["brasa"][int(h(x, y, s + 7) * 3) % 3]
            else:
                c = p["ceniza"][0 if h(x, y, s + 7) < 0.6 else 1]
            img.putpixel((x, y), c4(c))
    for y in range(6, 10):
        for x in range(6, 10):
            borde = x in (6, 9) or y in (6, 9)
            c = p["claro"] if borde else (p["brasa"][0] if encendido else p["agujero"])
            img.putpixel((x, y), c4(c))
    for x, y in ((7, 7), (8, 8)):
        img.putpixel((x, y), c4(p["adorno"]))
    return img


def modelo_incensario(nombre, encendido):
    sufijo = "_encendido" if encendido else ""
    lado = f"yonga:block/{nombre}_lado{sufijo}"
    arriba = f"yonga:block/{nombre}_arriba{sufijo}"

    def caras(arriba_uv=None, abajo_uv=None, cull_abajo=False):
        f = {d: {"texture": "#lado"} for d in ("north", "south", "east", "west")}
        f["up"] = {"texture": "#arriba"} if arriba_uv is None else {"texture": "#arriba", "uv": arriba_uv}
        f["down"] = {"texture": "#arriba", "uv": abajo_uv or [0, 0, 2, 2]}
        if cull_abajo:
            f["down"]["cullface"] = "down"
        return f

    return {
        "parent": "block/block",
        "render_type": "minecraft:cutout",
        "textures": {"particle": lado, "lado": lado, "arriba": arriba},
        "elements": [
            {"from": [4, 0, 4], "to": [12, 2, 12], "faces": caras(arriba_uv=[0, 0, 2, 2], cull_abajo=True)},
            {"from": [7, 2, 7], "to": [9, 7, 9], "faces": caras(arriba_uv=[0, 0, 2, 2])},
            {"from": [4, 7, 4], "to": [12, 11, 12], "faces": caras()},
            {"from": [6, 11, 6], "to": [10, 13, 10], "faces": caras()},
            {"from": [7.5, 13, 7.5], "to": [8.5, 14, 8.5], "faces": caras(arriba_uv=[7, 7, 8, 8])},
        ],
    }


# ---------------------------------------------------------------- iconos (formas geométricas)

def dist_segmento(px, py, a, b):
    ax, ay = a
    bx, by = b
    dx, dy = bx - ax, by - ay
    l2 = dx * dx + dy * dy
    t = 0 if l2 == 0 else max(0, min(1, ((px - ax) * dx + (py - ay) * dy) / l2))
    return math.hypot(px - (ax + t * dx), py - (ay + t * dy)), t


def pintar_formas(formas):
    """formas: lista de (prueba(px, py) -> material o None). Devuelve una rejilla 16x16 de materiales."""
    rejilla = [[None] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            for prueba in formas:
                m = prueba(x + 0.5, y + 0.5)
                if m is not None:
                    rejilla[y][x] = m
    return rejilla


def segmento(a, b, r, material):
    return lambda px, py: material if dist_segmento(px, py, a, b)[0] <= r else None


def anillo(c, r0, r1, material):
    return lambda px, py: material if r0 <= math.hypot(px - c[0], py - c[1]) <= r1 else None


MATERIALES = {
    # (luz, normal, sombra)
    "acero": ((222, 226, 234), (164, 170, 182), (104, 108, 122)),
    "lucarita": ((247, 149, 57), (209, 130, 56), (143, 69, 0)),
    "madera": ((150, 112, 70), (112, 82, 50), (76, 54, 32)),
    "cuerda": ((214, 188, 140), (176, 148, 102), (128, 104, 70)),
    "oro": ((250, 214, 104), (228, 184, 62), (176, 132, 38)),
    "marfil": ((244, 240, 226), (234, 228, 210), (196, 186, 162)),
    "ebano": ((74, 67, 88), (52, 47, 62), (30, 27, 36)),
    "cuenta": ((170, 116, 70), (130, 84, 48), (92, 58, 32)),
}
CONTORNO = (34, 32, 40, 255)


def a_imagen(rejilla):
    img = Image.new("RGBA", (16, 16), NADA)
    lleno = lambda x, y: 0 <= x < 16 and 0 <= y < 16 and rejilla[y][x] is not None
    for y in range(16):
        for x in range(16):
            m = rejilla[y][x]
            if m is None:
                if any(lleno(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    img.putpixel((x, y), CONTORNO)
                continue
            luz, normal, sombra = MATERIALES[m]
            if not lleno(x - 1, y) or not lleno(x, y - 1):
                c = luz
            elif not lleno(x + 1, y) or not lleno(x, y + 1):
                c = sombra
            else:
                c = normal
            img.putpixel((x, y), c4(c))
    return img


def gancho(origen, giro, escala, largo_mango, material="acero"):
    """El gancho (tres puntas). Local: puntas hacia arriba (y negativa), origen en la base de las puntas."""
    co, se = math.cos(math.radians(giro)), math.sin(math.radians(giro))

    def p(x, y):
        x, y = x * escala, y * escala
        return (origen[0] + x * co - y * se, origen[1] + x * se + y * co)

    r = 0.75
    return [
        segmento(p(0, 0), p(0, -3.6), r, material),
        segmento(p(0, 1.2), p(-2.6, -0.6), r, material),
        segmento(p(-2.6, -0.6), p(-3.0, -2.4), r, material),
        segmento(p(-3.0, -2.4), p(-2.2, -3.2), r * 0.8, material),
        segmento(p(0, 1.2), p(2.6, -0.6), r, material),
        segmento(p(2.6, -0.6), p(3.0, -2.4), r, material),
        segmento(p(3.0, -2.4), p(2.2, -3.2), r * 0.8, material),
        segmento(p(0, 0), p(0, largo_mango), r, material),
    ]


def gancho_solo():
    formas = gancho((8.0, 6.0), 0, 1.0, 6.5) + [anillo((8.0, 13.6), 0.9, 1.9, "acero")]
    return a_imagen(pintar_formas(formas))


def lanzador(con_gancho):
    formas = [
        segmento((2.4, 13.6), (5.6, 10.4), 1.35, "madera"),           # culata
        segmento((5.2, 10.8), (8.0, 8.0), 1.0, "acero"),              # cañón
        segmento((4.4, 12.8), (5.6, 13.8), 0.55, "acero"),            # gatillo
    ]
    if con_gancho:
        # A 45º el gancho se dibuja a mano, píxel a píxel: centro, punta en diagonal y dos puntas a los lados
        pixeles = {(10, 5), (9, 6), (9, 7), (8, 7),
                   (11, 4), (12, 3), (13, 2), (14, 1),
                   (10, 4), (10, 3), (10, 2), (9, 1),
                   (11, 5), (12, 5), (13, 5), (14, 6)}
        formas.append(lambda px, py: "acero" if (int(px), int(py)) in pixeles else None)
    else:
        formas.append(segmento((8.0, 8.0), (9.6, 6.4), 0.55, "cuerda"))
    return a_imagen(pintar_formas(formas))


def baculo():
    """El báculo: vara mitad marfil (abajo) y mitad ébano, anilla de lucarita y el cayado de oro arriba."""
    centro = (11.0, 4.6)

    def cayado(px, py):
        d = math.hypot(px - centro[0], py - centro[1])
        if not (1.3 <= d <= 2.6):
            return None
        ang = math.degrees(math.atan2(-(py - centro[1]), px - centro[0]))   # 0 = derecha, 90 = arriba
        return None if -135 < ang < -25 else "oro"

    def vara(px, py):
        d, t = dist_segmento(px, py, (2.4, 13.6), (9.2, 6.8))
        if d > 0.9:
            return None
        if 0.62 <= t <= 0.74:
            return "lucarita"
        return "marfil" if t < 0.62 else "ebano"

    return a_imagen(pintar_formas([vara, cayado]))


def rosario():
    """El rosario: un collar de cuentas y una cruz de oro colgando."""
    cuentas = set()
    for i in range(16):
        a = 2 * math.pi * i / 16
        cuentas.add((round(8 + 4.6 * math.cos(a) - 0.5), round(5.6 + 3.6 * math.sin(a) - 0.5)))
    cruz = {(7, 10), (7, 11), (7, 12), (7, 13), (7, 14), (6, 11), (8, 11), (7, 9)}

    def forma(px, py):
        p = (int(px), int(py))
        if p in cruz:
            return "oro"
        return "cuenta" if p in cuentas else None

    return a_imagen(pintar_formas([forma]))


# Asa cromada y antena; panel de arriba con botones de lucarita y el dial; franja cromada; dos altavoces con su cono;
# en medio la ventanita del casete con las dos bobinas y debajo las teclas; patas abajo
RADIOCASETE = [
    "...............a",
    "....kkkkkkkk..a.",
    "...kppppppppka..",
    "...kpk....kpka..",
    "kkkkkkkkkkkkkkkk",
    "kTLTLTTTTTTyyTTk",
    "kSSSSSSSSSSSSSSk",
    "kBkkkBwwwwBkkkBk",
    "kkgdgkrwwrkgdgkk",
    "kkdcdkwwwwkdcdkk",
    "kkgdgkBLLBkgdgkk",
    "kBkkkBBBBBBkkkBk",
    "kDDDDDDDDDDDDDDk",
    "kkkkkkkkkkkkkkkk",
    ".kk..........kk.",
    "................"]
COLORES_RADIO = {"k": (20, 18, 24), "p": (204, 208, 218), "a": (170, 174, 186), "S": (150, 156, 168),
                 "T": (96, 96, 112), "B": (62, 62, 76), "D": (42, 42, 52), "g": (30, 30, 36), "d": (78, 78, 90),
                 "c": (160, 160, 172), "w": (150, 196, 214), "r": (70, 52, 40), "L": (232, 140, 60),
                 "y": (240, 210, 90)}


def radiocasete():
    """El radiocasete: asa arriba, dos altavoces, la ventanita del casete y botones de lucarita."""
    img = Image.new("RGBA", (16, 16), NADA)
    for y, fila in enumerate(RADIOCASETE):
        for x, ch in enumerate(fila):
            if ch in COLORES_RADIO:
                img.putpixel((x, y), c4(COLORES_RADIO[ch]))
    return img


def textura_rosario():
    """Para el rosario puesto (CapaRosario): arriba madera de las cuentas, abajo oro de la cruz."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if y < 8:
                c = (130, 84, 48) if h(x, y, 5) < 0.6 else (150, 100, 58)
            else:
                c = (228, 184, 62) if h(x, y, 6) < 0.6 else (250, 214, 104)
            img.putpixel((x, y), c4(c))
    return img


# ---------------------------------------------------------------- escribir

def guardar_json(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(datos, f, indent=2, ensure_ascii=False)
        f.write("\n")


def receta(oficio, rango, categoria, patron, clave, resultado):
    return {"conditions": CONDICION, "type": "yonga:receta_oficio", "oficio": oficio, "rango": rango,
            "category": categoria, "pattern": patron,
            "key": {k: ({"tag": v[1:]} if v.startswith("#") else {"item": v}) for k, v in clave.items()},
            "result": {"item": resultado}}


TEXTOS_ES = {
    "block.yonga.incensario_luminoso": "Incensario",
    "block.yonga.incensario_oscuro": "Incensario oscuro",
    "item.yonga.garfio": "Garfio",
    "entity.yonga.garfio": "Garfio",
    "yonga.garfio.uso": "Donde llega la cuerda, llegas tú.",
    "item.yonga.rosario": "Rosario",
    "item.yonga.baculo": "Báculo",
    "item.yonga.radiocasete": "Radiocasete",
    "yonga.radiocasete.pausa": "Pausa",
    "yonga.radiocasete.continuar": "Continuar",
    "yonga.radiocasete.reiniciar": "Volver a empezar",
}
TEXTOS_EN = {
    "block.yonga.incensario_luminoso": "Censer",
    "block.yonga.incensario_oscuro": "Dark Censer",
    "item.yonga.garfio": "Grappling Hook",
    "entity.yonga.garfio": "Grappling Hook",
    "yonga.garfio.uso": "Where the rope reaches, so do you.",
    "item.yonga.rosario": "Rosary",
    "item.yonga.baculo": "Crosier",
    "item.yonga.radiocasete": "Boombox",
    "yonga.radiocasete.pausa": "Pause",
    "yonga.radiocasete.continuar": "Play",
    "yonga.radiocasete.reiniciar": "Restart",
}


def anadir_textos(ruta, textos):
    with open(ruta, encoding="utf-8") as f:
        datos = json.load(f)
    datos.update(textos)
    guardar_json(ruta, datos)


def anadir_a_etiqueta(ruta, valores):
    with open(ruta, encoding="utf-8") as f:
        datos = json.load(f)
    presentes = {v if isinstance(v, str) else v["id"] for v in datos["values"]}
    for v in valores:
        if v not in presentes:
            datos["values"].append({"id": v, "required": False})
    guardar_json(ruta, datos)


def main(raiz):
    a = os.path.join(raiz, "assets", "yonga")
    d = os.path.join(raiz, "data", "yonga")
    tb, ti = os.path.join(a, "textures", "block"), os.path.join(a, "textures", "item")
    os.makedirs(tb, exist_ok=True)
    os.makedirs(ti, exist_ok=True)

    # Incensarios
    for i, (lado, p) in enumerate(PALETAS.items()):
        nombre = f"incensario_{lado}"
        for encendido in (False, True):
            sufijo = "_encendido" if encendido else ""
            incensario_lado(p, encendido, 30 + i).save(os.path.join(tb, f"{nombre}_lado{sufijo}.png"))
            incensario_arriba(p, encendido, 40 + i).save(os.path.join(tb, f"{nombre}_arriba{sufijo}.png"))
            guardar_json(os.path.join(a, "models", "block", f"{nombre}{sufijo}.json"), modelo_incensario(nombre, encendido))
        guardar_json(os.path.join(a, "blockstates", f"{nombre}.json"), {"variants": {
            "lit=false": {"model": f"yonga:block/{nombre}"},
            "lit=true": {"model": f"yonga:block/{nombre}_encendido"}}})
        guardar_json(os.path.join(a, "models", "item", f"{nombre}.json"), {"parent": f"yonga:block/{nombre}"})
        guardar_json(os.path.join(d, "loot_tables", "blocks", f"{nombre}.json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"yonga:{nombre}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    anadir_a_etiqueta(os.path.join(raiz, "data", "minecraft", "tags", "blocks", "mineable", "pickaxe.json"),
                      ["yonga:incensario_luminoso", "yonga:incensario_oscuro"])

    # Garfio: entero (0), sin gancho porque está lanzado (1) y el gancho solo (2)
    lanzador(True).save(os.path.join(ti, "garfio.png"))
    lanzador(False).save(os.path.join(ti, "garfio_lanzado.png"))
    gancho_solo().save(os.path.join(ti, "garfio_gancho.png"))
    guardar_json(os.path.join(a, "models", "item", "garfio.json"), {
        "parent": "minecraft:item/handheld", "textures": {"layer0": "yonga:item/garfio"},
        "overrides": [{"predicate": {"custom_model_data": 1}, "model": "yonga:item/garfio_lanzado"},
                      {"predicate": {"custom_model_data": 2}, "model": "yonga:item/garfio_gancho"}]})
    guardar_json(os.path.join(a, "models", "item", "garfio_lanzado.json"),
                 {"parent": "minecraft:item/handheld", "textures": {"layer0": "yonga:item/garfio_lanzado"}})
    guardar_json(os.path.join(a, "models", "item", "garfio_gancho.json"),
                 {"parent": "minecraft:item/generated", "textures": {"layer0": "yonga:item/garfio_gancho"}})

    # Rosario y báculo
    rosario().save(os.path.join(ti, "rosario.png"))
    baculo().save(os.path.join(ti, "baculo.png"))
    os.makedirs(os.path.join(a, "textures", "entity"), exist_ok=True)
    textura_rosario().save(os.path.join(a, "textures", "entity", "rosario.png"))
    guardar_json(os.path.join(a, "models", "item", "rosario.json"),
                 {"parent": "minecraft:item/generated", "textures": {"layer0": "yonga:item/rosario"}})
    guardar_json(os.path.join(a, "models", "item", "baculo.json"),
                 {"parent": "minecraft:item/handheld", "textures": {"layer0": "yonga:item/baculo"}})
    cara = os.path.join(d, "tags", "items", "cara.json")
    with open(cara, encoding="utf-8") as f:
        datos = json.load(f)
    if "yonga:rosario" not in datos["values"]:
        datos["values"].append("yonga:rosario")
    guardar_json(cara, datos)

    # Radiocasete
    radiocasete().save(os.path.join(ti, "radiocasete.png"))
    guardar_json(os.path.join(a, "models", "item", "radiocasete.json"),
                 {"parent": "minecraft:item/generated", "textures": {"layer0": "yonga:item/radiocasete"}})

    # Recetas
    r = os.path.join(d, "recipes")
    guardar_json(os.path.join(r, "radiocasete.json"), receta(
        "melomano", "aprendiz", "misc", ["INI", "IJI"],
        {"I": "minecraft:iron_ingot", "N": "minecraft:note_block", "J": "minecraft:jukebox"}, "yonga:radiocasete"))
    guardar_json(os.path.join(r, "rosario.json"), receta(
        "sacerdote", "novato", "misc", ["SSS", "S S", " G "],
        {"S": "#minecraft:wooden_buttons", "G": "minecraft:gold_nugget"}, "yonga:rosario"))
    guardar_json(os.path.join(r, "baculo.json"), receta(
        "sacerdote", "oficial", "equipment", [" GG", " LG", "S  "],
        {"G": "minecraft:gold_ingot", "L": "yonga:lucarita", "S": "minecraft:stick"}, "yonga:baculo"))
    guardar_json(os.path.join(r, "incensario_luminoso.json"), receta(
        "sacerdote", "aprendiz", "misc", [" C ", "GFG", " G "],
        {"C": "minecraft:chain", "G": "minecraft:gold_ingot", "F": "minecraft:campfire"}, "yonga:incensario_luminoso"))
    guardar_json(os.path.join(r, "incensario_oscuro.json"), receta(
        "sacerdote", "aprendiz", "misc", [" C ", "BFB", " B "],
        {"C": "minecraft:chain", "B": "minecraft:polished_blackstone", "F": "minecraft:soul_campfire"}, "yonga:incensario_oscuro"))
    guardar_json(os.path.join(r, "garfio.json"), receta(
        "explorador", "maestro", "equipment", [" DL", " CD", "X  "],
        {"D": "minecraft:diamond", "L": "yonga:lucarita", "C": "minecraft:chain", "X": "minecraft:crossbow"}, "yonga:garfio"))

    # Textos
    anadir_textos(os.path.join(a, "lang", "es_es.json"), TEXTOS_ES)
    anadir_textos(os.path.join(a, "lang", "en_us.json"), TEXTOS_EN)


if __name__ == "__main__":
    main(sys.argv[1])
