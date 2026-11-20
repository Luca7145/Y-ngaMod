"""Genera todo lo del MÓDULO TABACO: los dibujos (objetos y fases de la planta), modelos, estado de bloque, botín de
la planta, recetas (con la condición del módulo), las semillas que suelta la hierba y los textos.
Uso: python3 generar_tabaco.py <carpeta src/main/resources>"""
import json
import math
import os
import sys

from PIL import Image

CONDICION = [{"type": "yonga:modulo_activo", "modulo": "tabaco"}]


def h(x, y, s):
    n = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def lienzo():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def pon(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), c + (255,) if len(c) == 3 else c)


def hoja(colores, vena, s, x0=2, y0=13, x1=13, y1=2, ancho=3.4):
    """Una hoja grande en diagonal, con su nervio."""
    img = lienzo()
    ax, ay = x1 - x0, y1 - y0
    largo = math.hypot(ax, ay)
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5 - x0, y + 0.5 - y0
            t = (px * ax + py * ay) / (largo * largo)
            d = abs(px * ay - py * ax) / largo
            if 0 <= t <= 1 and d <= ancho * math.sin(math.pi * min(1.0, t * 1.05)) ** 0.8:
                if d < 0.6:
                    c = vena
                elif d > ancho * math.sin(math.pi * t) ** 0.8 - 0.9:
                    c = colores[2]
                else:
                    c = colores[0] if h(x, y, s) > 0.3 else colores[1]
                pon(img, x, y, c)
    pon(img, x0 - 1, y0 + 1, vena)          # el rabito
    return img


def palito(largo, grosor, cuerpo, punta_ini=None, s=0):
    """Un cilindro en diagonal desde abajo-izquierda: devuelve la imagen y la lista de (t, x, y) por píxel."""
    img = lienzo()
    x0, y0 = 2.5, 13.5
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5 - x0, y0 - (y + 0.5)
            t = (px + py) / 2.0                     # a lo largo de la diagonal
            d = abs(px - py) / math.sqrt(2)         # de lado
            if -0.2 <= t <= largo and d <= grosor / 2:
                c = cuerpo(t / largo, d / (grosor / 2), x, y)
                if c:
                    pon(img, x, y, c)
    return img


def cigarro(estado):
    largo = {0: 10.5, 1: 9.6, 2: 3.6}[estado]

    def cuerpo(t, lado, x, y):
        filtro = t * largo < 3.2
        if filtro:
            c = (214, 128, 52) if h(x, y, 1) > 0.25 else (232, 160, 82)
            return tuple(int(v * (0.85 if lado > 0.6 else 1.0)) for v in c)
        if estado >= 1 and t > 0.93:
            if estado == 2:
                return (110, 108, 104)                 # ceniza apagada
            return (255, 120, 30) if lado < 0.6 else (200, 40, 20)   # la brasa
        if estado == 1 and t > 0.86:
            return (150, 148, 144)                     # ceniza
        c = (242, 240, 232) if lado < 0.6 else (206, 204, 196)
        return c
    return palito(largo, 2.2, cuerpo)


def puro(estado):
    largo = {0: 11.0, 1: 10.2, 2: 4.2}[estado]

    def cuerpo(t, lado, x, y):
        if 0.12 < t * 11.0 / largo * (largo / 11.0) and 1.4 < t * largo < 2.6:
            return (190, 30, 30) if h(x, y, 3) > 0.3 else (230, 190, 70)   # la vitola
        if estado >= 1 and t > 0.92:
            if estado == 2:
                return (96, 92, 88)
            return (255, 110, 30) if lado < 0.5 else (190, 40, 20)
        if estado == 1 and t > 0.84:
            return (140, 136, 130)
        base = (124, 78, 40) if h(x, y, 4) > 0.3 else (104, 64, 32)
        return tuple(int(v * (0.78 if lado > 0.7 else 1.0)) for v in base)
    return palito(largo, 3.3, cuerpo)


def cajetilla(llena):
    img = lienzo()
    for y in range(4, 15):
        for x in range(3, 13):
            borde = x in (3, 12) or y == 14
            c = (186, 30, 34) if y < 9 else (238, 236, 228)
            if y == 9:
                c = (230, 190, 70)                       # la franja dorada
            if borde:
                c = tuple(int(v * 0.75) for v in c)
            pon(img, x, y, c)
    for x in range(6, 10):
        pon(img, x, 11, (40, 40, 40)) if x in (6, 9) else pon(img, x, 11, (120, 30, 30))
    if llena:
        # tapa abierta hacia atrás y los filtros asomando
        for x in range(3, 13):
            pon(img, x, 2, (150, 22, 26))
            pon(img, x, 3, (120, 18, 22))
        for x in (4, 6, 8, 10):
            pon(img, x, 4, (214, 128, 52))
            pon(img, x + 1, 4, (232, 160, 82))
            pon(img, x, 5, (200, 116, 46))
            pon(img, x + 1, 5, (214, 128, 52))
    else:
        for x in range(3, 13):
            pon(img, x, 4, (150, 22, 26))
        for x in range(4, 12):
            pon(img, x, 5, (210, 46, 50))
    return img


def semillas():
    img = lienzo()
    for (x, y) in ((5, 6), (9, 4), (7, 9), (11, 8), (4, 11), (9, 12)):
        pon(img, x, y, (96, 62, 30))
        pon(img, x + 1, y, (120, 80, 40))
        pon(img, x, y + 1, (70, 44, 22))
    return img


def picado():
    img = lienzo()
    for i in range(70):
        x = int(3 + h(i, 1, 9) * 10)
        y = int(8 + h(i, 2, 9) * 6)
        if (x - 8) ** 2 / 30 + (y - 11.5) ** 2 / 8 > 1:
            continue
        c = (150, 96, 44) if h(i, 3, 9) > 0.4 else (110, 66, 30)
        pon(img, x, y, c)
        if h(i, 4, 9) > 0.5:
            pon(img, x + 1, y - 1, (176, 120, 60))
    return img


VERDES = [(78, 150, 54), (64, 128, 44), (48, 100, 34)]
VERDES_CLAROS = [(100, 170, 70), (86, 152, 58), (60, 118, 42)]


def planta(etapa):
    """Fases de la planta: tallo con hojas grandes alternas; al final, flores rosas arriba."""
    img = lienzo()
    alto = [3, 7, 10, 14, 16][etapa]
    for y in range(16 - alto, 16):
        pon(img, 8, y, (70, 120, 46))
    hojas = [(15 - k * 3, -1 if k % 2 else 1) for k in range(max(1, alto // 3))]
    for (y, lado) in hojas:
        tam = 2 + min(etapa, 3)
        for i in range(tam):
            for j in range(-1, 2):
                if abs(j) + i * 0.25 > 1.6:
                    continue
                x = 8 + lado * (i + 1)
                c = VERDES[(i + j) % 3] if etapa < 4 else VERDES_CLAROS[(i + j) % 3]
                pon(img, x, y + j - i // 2, c)
    if etapa == 4:
        for (x, y) in ((7, 0), (8, 1), (9, 0), (6, 1), (10, 1), (8, 0)):
            pon(img, x, y, (232, 120, 168) if (x + y) % 2 else (250, 170, 200))
    return img


def escribir(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(datos, f, ensure_ascii=False, indent=2)
        f.write("\n")


def main(res):
    a = os.path.join(res, "assets", "yonga")
    d = os.path.join(res, "data")
    ti, tb = os.path.join(a, "textures", "item"), os.path.join(a, "textures", "block")
    os.makedirs(ti, exist_ok=True)
    os.makedirs(tb, exist_ok=True)
    # --- dibujos
    semillas().save(os.path.join(ti, "semillas_tabaco.png"))
    hoja(VERDES, (40, 86, 30), 5).save(os.path.join(ti, "hoja_tabaco.png"))
    hoja([(196, 146, 64), (176, 124, 52), (140, 96, 40)], (120, 80, 34), 6).save(os.path.join(ti, "hoja_tabaco_curada.png"))
    picado().save(os.path.join(ti, "tabaco_picado.png"))
    for e, sufijo in ((0, ""), (1, "_encendido"), (2, "_gastado")):
        cigarro(e).save(os.path.join(ti, "cigarro" + sufijo + ".png"))
        puro(e).save(os.path.join(ti, "puro" + sufijo + ".png"))
    cajetilla(False).save(os.path.join(ti, "cajetilla.png"))
    cajetilla(True).save(os.path.join(ti, "cajetilla_llena.png"))
    for e in range(5):
        planta(e).save(os.path.join(tb, f"tabaco_etapa_{e}.png"))
    # --- modelos de objeto
    simples = ["semillas_tabaco", "hoja_tabaco", "hoja_tabaco_curada", "tabaco_picado",
               "cigarro_encendido", "cigarro_gastado", "puro_encendido", "puro_gastado", "cajetilla_llena"]
    for n in simples:
        escribir(os.path.join(a, "models", "item", n + ".json"),
                 {"parent": "minecraft:item/generated", "textures": {"layer0": "yonga:item/" + n}})
    for n in ("cigarro", "puro"):
        escribir(os.path.join(a, "models", "item", n + ".json"), {
            "parent": "minecraft:item/generated", "textures": {"layer0": "yonga:item/" + n},
            "overrides": [{"predicate": {"yonga:estado": 0.5}, "model": "yonga:item/" + n + "_encendido"},
                          {"predicate": {"yonga:estado": 1.0}, "model": "yonga:item/" + n + "_gastado"}]})
    escribir(os.path.join(a, "models", "item", "cajetilla.json"), {
        "parent": "minecraft:item/generated", "textures": {"layer0": "yonga:item/cajetilla"},
        "overrides": [{"predicate": {"yonga:llena": 1.0}, "model": "yonga:item/cajetilla_llena"}]})
    # --- la planta: 8 edades sobre 5 dibujos
    etapa = [0, 0, 1, 1, 2, 2, 3, 4]
    for e in range(5):
        escribir(os.path.join(a, "models", "block", f"tabaco_etapa_{e}.json"), {
            "parent": "minecraft:block/crop", "render_type": "minecraft:cutout",
            "textures": {"crop": f"yonga:block/tabaco_etapa_{e}"}})
    escribir(os.path.join(a, "blockstates", "planta_tabaco.json"),
             {"variants": {f"age={i}": {"model": f"yonga:block/tabaco_etapa_{etapa[i]}"} for i in range(8)}})
    madura = [{"condition": "minecraft:block_state_property", "block": "yonga:planta_tabaco", "properties": {"age": "7"}}]
    escribir(os.path.join(d, "yonga", "loot_tables", "blocks", "planta_tabaco.json"), {
        "type": "minecraft:block",
        "pools": [
            {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": "yonga:hoja_tabaco", "conditions": madura,
                 "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 3}}]},
                {"type": "minecraft:item", "name": "yonga:semillas_tabaco"}]}]},
            {"rolls": 1, "bonus_rolls": 0, "conditions": madura, "entries": [
                {"type": "minecraft:item", "name": "yonga:semillas_tabaco", "functions": [
                    {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                     "formula": "minecraft:binomial_with_bonus_count", "parameters": {"extra": 3, "probability": 0.5714286}}]}]}],
        "functions": [{"function": "minecraft:explosion_decay"}]})
    # --- recetas
    r = os.path.join(d, "yonga", "recipes", "tabaco")
    for tipo, tiempo, nombre in (("smelting", 200, "horno"), ("smoking", 100, "ahumador"), ("campfire_cooking", 600, "fogata")):
        escribir(os.path.join(r, "hoja_tabaco_curada_" + nombre + ".json"), {
            "conditions": CONDICION, "type": "minecraft:" + tipo, "category": "misc",
            "ingredient": {"item": "yonga:hoja_tabaco"}, "result": "yonga:hoja_tabaco_curada",
            "experience": 0.1, "cookingtime": tiempo})
    escribir(os.path.join(r, "tabaco_picado.json"), {
        "conditions": CONDICION, "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": "yonga:hoja_tabaco_curada"}], "result": {"item": "yonga:tabaco_picado", "count": 2}})
    escribir(os.path.join(r, "cigarro.json"), {
        "conditions": CONDICION, "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": "minecraft:paper"}, {"item": "yonga:tabaco_picado"}], "result": {"item": "yonga:cigarro"}})
    escribir(os.path.join(r, "puro.json"), {
        "conditions": CONDICION, "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["H", "H", "H"], "key": {"H": {"item": "yonga:hoja_tabaco_curada"}}, "result": {"item": "yonga:puro"}})
    escribir(os.path.join(r, "cajetilla.json"), {
        "conditions": CONDICION, "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": "minecraft:paper"}, {"item": "minecraft:paper"}, {"item": "minecraft:paper"},
                        {"item": "minecraft:red_dye"}], "result": {"item": "yonga:cajetilla"}})
    # --- semillas al romper hierba
    ruta = os.path.join(d, "forge", "loot_modifiers", "global_loot_modifiers.json")
    glm = {"replace": False, "entries": []}
    if os.path.exists(ruta):
        with open(ruta, encoding="utf-8") as f:
            glm = json.load(f)
    if "yonga:semillas_tabaco" not in glm["entries"]:
        glm["entries"].append("yonga:semillas_tabaco")
    escribir(ruta, glm)
    escribir(os.path.join(d, "yonga", "loot_modifiers", "semillas_tabaco.json"),
             {"type": "yonga:semillas_tabaco", "conditions": [], "probabilidad": 0.04})
    # --- textos
    es = {"item.yonga.semillas_tabaco": "Semillas de tabaco", "block.yonga.planta_tabaco": "Planta de tabaco",
          "item.yonga.hoja_tabaco": "Hoja de tabaco", "item.yonga.hoja_tabaco_curada": "Hoja de tabaco curada",
          "item.yonga.tabaco_picado": "Tabaco picado", "item.yonga.cigarro": "Cigarro", "item.yonga.puro": "Puro",
          "item.yonga.cajetilla": "Cajetilla", "container.yonga.cajetilla": "Cajetilla",
          "yonga.tabaco.sin_encender": "Sin encender", "yonga.tabaco.encendido": "Encendido (%s)",
          "yonga.tabaco.gastado": "Gastado",
          "yonga.modulo.tabaco": "Tabaco",
          "yonga.modulo.tabaco.desc": "La planta de tabaco, el cigarro, el puro y la cajetilla. Se encienden con el mechero; cada calada da un boost o wither y náuseas (más probable cuanto más fumes)."}
    en = {"item.yonga.semillas_tabaco": "Tobacco Seeds", "block.yonga.planta_tabaco": "Tobacco Plant",
          "item.yonga.hoja_tabaco": "Tobacco Leaf", "item.yonga.hoja_tabaco_curada": "Cured Tobacco Leaf",
          "item.yonga.tabaco_picado": "Shredded Tobacco", "item.yonga.cigarro": "Cigarette", "item.yonga.puro": "Cigar",
          "item.yonga.cajetilla": "Cigarette Pack", "container.yonga.cajetilla": "Cigarette Pack",
          "yonga.tabaco.sin_encender": "Unlit", "yonga.tabaco.encendido": "Lit (%s)", "yonga.tabaco.gastado": "Spent",
          "yonga.modulo.tabaco": "Tobacco",
          "yonga.modulo.tabaco.desc": "Tobacco plant, cigarettes, cigars and the cigarette pack. Light them with flint and steel; each puff gives a boost or wither and nausea (likelier the more you smoke)."}
    for archivo, nuevos in (("es_es.json", es), ("en_us.json", en)):
        ruta = os.path.join(a, "lang", archivo)
        with open(ruta, encoding="utf-8") as f:
            datos = json.load(f)
        datos.update(nuevos)
        escribir(ruta, datos)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else ".")
