"""Genera todo lo de los ESTANTES (los de la 1.21.9, en la 1.20.1): texturas propias (dibujadas aquí, imitando el
aspecto de tronco descortezado), modelos, estados de bloque, modelos de objeto, botín, recetas, la etiqueta del
hacha y los textos.
Uso: python3 generar_estantes.py <carpeta src/main/resources>

Texturas por madera (16x16):
  <madera>_shelf               la cara de delante de la tabla del fondo (donde se apoyan los objetos)
  <madera>_shelf_powered       igual, activado (con una línea encendida sobre el listón de abajo)
  <madera>_shelf_powered_left / _center / _right   activado y unido a otros (sin el remate del lado que se une)
  <madera>_shelf_side          los listones y los cantos (y la parte de atrás)
  <madera>_shelf_side_left / _center / _right      los listones de un estante unido
"""
import json
import os
import sys

from PIL import Image

MADERAS = {  # color base del tronco descortezado (aprox.), nombre en español, receta
    "oak": ((177, 144, 86), "roble", "stripped_oak_log"),
    "spruce": ((115, 89, 52), "abeto", "stripped_spruce_log"),
    "birch": ((196, 176, 118), "abedul", "stripped_birch_log"),
    "jungle": ((171, 132, 84), "jungla", "stripped_jungle_log"),
    "acacia": ((174, 92, 59), "acacia", "stripped_acacia_log"),
    "dark_oak": ((96, 76, 49), "roble oscuro", "stripped_dark_oak_log"),
    "mangrove": ((119, 54, 47), "mangle", "stripped_mangrove_log"),
    "cherry": ((219, 160, 158), "cerezo", "stripped_cherry_log"),
    "bamboo": ((195, 179, 83), "bambú", "stripped_bamboo_block"),
    "crimson": ((137, 57, 90), "carmesí", "stripped_crimson_stem"),
    "warped": ((57, 150, 147), "distorsionado", "stripped_warped_stem"),
}
INGLES = {"oak": "Oak", "spruce": "Spruce", "birch": "Birch", "jungle": "Jungle", "acacia": "Acacia",
          "dark_oak": "Dark Oak", "mangrove": "Mangrove", "cherry": "Cherry", "bamboo": "Bamboo",
          "crimson": "Crimson", "warped": "Warped"}
PARTES = ["unconnected", "left", "center", "right"]


def h(x, y, s):
    n = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def mezcla(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def veta(base, x, y, s):
    """Madera descortezada: vetas horizontales largas."""
    linea = h(0, y, s)
    k = 0.92 + 0.12 * linea + 0.06 * (h(x // 4, y, s + 1) - 0.5)
    if h(x // 3, y, s + 2) > 0.93:
        k -= 0.12                                              # nudo / veta oscura
    return mezcla(base, k)


def remates(parte):
    """Qué bordes (izquierdo, derecho) llevan remate oscuro: los que no se unen a otro estante."""
    return {"unconnected": (True, True), "left": (True, False), "center": (False, False), "right": (False, True)}[parte]


def tabla(base, parte, s):
    img = Image.new("RGBA", (16, 16))
    p = img.load()
    izq, der = remates(parte)
    for y in range(16):
        for x in range(16):
            c = veta(base, x, y, s)
            if y in (3, 12):
                c = mezcla(base, 0.72)                         # el canto de cada listón
            if y in (0, 15):
                c = mezcla(base, 1.1)
            if (x == 0 and izq) or (x == 15 and der):
                c = mezcla(base, 0.68)                         # remate del lado
            p[x, y] = c + (255,)
    return img


def frente(base, parte, activado, s):
    img = Image.new("RGBA", (16, 16))
    p = img.load()
    izq, der = remates(parte)
    fondo = mezcla(base, 0.82)                                  # la tabla del fondo, en sombra
    for y in range(16):
        for x in range(16):
            c = veta(fondo, x, y, s + 7)
            if x in (5, 10):
                c = mezcla(fondo, 0.86)                        # las juntas que separan las tres ranuras
            if y in (4, 11):
                c = mezcla(fondo, 0.7)                         # sombra de los listones
            if (x == 0 and izq) or (x == 15 and der):
                c = mezcla(base, 0.62)
            if activado and y == 10 and not ((x == 0 and izq) or (x == 15 and der)):
                c = (226, 64, 34) if (x + s) % 3 else (255, 120, 60)   # la línea encendida
            p[x, y] = c + (255,)
    return img


def escribir(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(datos, f, ensure_ascii=False, indent=2)
        f.write("\n")


def plantilla():
    """El modelo (mirando al norte): la tabla del fondo y los listones de arriba y abajo."""
    t, fr = "#tabla", "#frente"
    return {
        "parent": "block/block",
        "textures": {"particle": t},
        "elements": [
            {"from": [0, 0, 13], "to": [16, 16, 16], "faces": {
                "north": {"uv": [0, 0, 16, 16], "texture": fr},
                "south": {"uv": [0, 0, 16, 16], "texture": t, "cullface": "south"},
                "east": {"uv": [0, 0, 3, 16], "texture": t, "cullface": "east"},
                "west": {"uv": [13, 0, 16, 16], "texture": t, "cullface": "west"},
                "up": {"uv": [0, 13, 16, 16], "texture": t, "cullface": "up"},
                "down": {"uv": [0, 0, 16, 3], "texture": t, "cullface": "down"}}},
            {"from": [0, 12, 11], "to": [16, 16, 13], "faces": {
                "north": {"uv": [0, 0, 16, 4], "texture": t},
                "east": {"uv": [3, 0, 5, 4], "texture": t, "cullface": "east"},
                "west": {"uv": [11, 0, 13, 4], "texture": t, "cullface": "west"},
                "up": {"uv": [0, 11, 16, 13], "texture": t, "cullface": "up"},
                "down": {"uv": [0, 3, 16, 5], "texture": t}}},
            {"from": [0, 0, 11], "to": [16, 4, 13], "faces": {
                "north": {"uv": [0, 12, 16, 16], "texture": t},
                "east": {"uv": [3, 12, 5, 16], "texture": t, "cullface": "east"},
                "west": {"uv": [11, 12, 13, 16], "texture": t, "cullface": "west"},
                "up": {"uv": [0, 3, 16, 5], "texture": t},
                "down": {"uv": [0, 11, 16, 13], "texture": t, "cullface": "down"}}},
        ],
    }


def main(res):
    a = os.path.join(res, "assets", "yonga")
    d = os.path.join(res, "data")
    escribir(os.path.join(a, "models", "block", "shelf_template.json"), plantilla())
    tex = os.path.join(a, "textures", "block")
    os.makedirs(tex, exist_ok=True)
    lang_es, lang_en, bloques = {}, {}, []
    for semilla, (madera, (base, es, receta)) in enumerate(MADERAS.items(), start=3):
        n = madera + "_shelf"
        bloques.append("yonga:" + n)
        # texturas
        tabla(base, "unconnected", semilla).save(os.path.join(tex, n + "_side.png"))
        frente(base, "unconnected", False, semilla).save(os.path.join(tex, n + ".png"))
        frente(base, "unconnected", True, semilla).save(os.path.join(tex, n + "_powered.png"))
        for parte in ("left", "center", "right"):
            tabla(base, parte, semilla).save(os.path.join(tex, n + "_side_" + parte + ".png"))
            frente(base, parte, True, semilla).save(os.path.join(tex, n + "_powered_" + parte + ".png"))
        # modelos
        modelos = {
            n: (n, n + "_side"),
            n + "_powered": (n + "_powered", n + "_side"),
            n + "_powered_left": (n + "_powered_left", n + "_side_left"),
            n + "_powered_center": (n + "_powered_center", n + "_side_center"),
            n + "_powered_right": (n + "_powered_right", n + "_side_right"),
        }
        for nombre, (fr, tb) in modelos.items():
            escribir(os.path.join(a, "models", "block", nombre + ".json"), {
                "parent": "yonga:block/shelf_template",
                "textures": {"frente": "yonga:block/" + fr, "tabla": "yonga:block/" + tb}})
        escribir(os.path.join(a, "models", "item", n + ".json"), {"parent": "yonga:block/" + n})
        # estados: facing x powered x side_chain
        giros = {"north": 0, "east": 90, "south": 180, "west": 270}
        variantes = {}
        for f, y in giros.items():
            for parte in PARTES:
                variantes[f"facing={f},powered=false,side_chain={parte}"] = dict({"model": "yonga:block/" + n}, **({"y": y} if y else {}))
                modelo = n + "_powered" + ("" if parte == "unconnected" else "_" + parte)
                variantes[f"facing={f},powered=true,side_chain={parte}"] = dict({"model": "yonga:block/" + modelo}, **({"y": y} if y else {}))
        escribir(os.path.join(a, "blockstates", n + ".json"), {"variants": variantes})
        # botín: se suelta a sí mismo (lo de dentro lo suelta el bloque al romperse)
        escribir(os.path.join(d, "yonga", "loot_tables", "blocks", n + ".json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "yonga:" + n}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
        # receta: 6 troncos descortezados (arriba y abajo) -> 6 estantes
        escribir(os.path.join(d, "yonga", "recipes", n + ".json"), {
            "type": "minecraft:crafting_shaped", "category": "building", "group": "shelf",
            "pattern": ["###", "   ", "###"], "key": {"#": {"item": "minecraft:" + receta}},
            "result": {"item": "yonga:" + n, "count": 6}})
        lang_es["block.yonga." + n] = "Estante de " + es
        lang_en["block.yonga." + n] = INGLES[madera] + " Shelf"
    # etiqueta del hacha (se añade a la que ya hubiera)
    ruta_hacha = os.path.join(d, "minecraft", "tags", "blocks", "mineable", "axe.json")
    hacha = {"replace": False, "values": []}
    if os.path.exists(ruta_hacha):
        with open(ruta_hacha, encoding="utf-8") as f:
            hacha = json.load(f)
    for b in bloques:
        if b not in hacha["values"]:
            hacha["values"].append(b)
    escribir(ruta_hacha, hacha)
    # textos
    sonidos_es = {"activate": "Se activa un estante", "deactivate": "Se desactiva un estante",
                  "multi_swap": "Se intercambian objetos del estante", "place_item": "Se pone un objeto en el estante",
                  "single_swap": "Se cambia un objeto del estante", "take_item": "Se coge un objeto del estante"}
    sonidos_en = {"activate": "Shelf activates", "deactivate": "Shelf deactivates", "multi_swap": "Shelf items swapped",
                  "place_item": "Item placed on shelf", "single_swap": "Shelf item swapped", "take_item": "Item taken from shelf"}
    for k in sonidos_es:
        lang_es["subtitles.yonga.block.shelf." + k] = sonidos_es[k]
        lang_en["subtitles.yonga.block.shelf." + k] = sonidos_en[k]
    for archivo, nuevos in (("es_es.json", lang_es), ("en_us.json", lang_en)):
        ruta = os.path.join(a, "lang", archivo)
        with open(ruta, encoding="utf-8") as f:
            datos = json.load(f)
        datos.update(nuevos)
        escribir(ruta, datos)
    # sonidos
    ruta = os.path.join(a, "sounds.json")
    with open(ruta, encoding="utf-8") as f:
        datos = json.load(f)
    for k in sonidos_es:
        datos["block.shelf." + k] = {"subtitle": "subtitles.yonga.block.shelf." + k,
                                     "sounds": [{"name": "yonga:shelf/" + k}]}
    escribir(ruta, datos)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else ".")
