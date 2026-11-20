"""Los objetos sagrados del Sacerdote: la túnica puesta (tunica_layer_1/2.png, 64x32 como las armaduras de
Minecraft), los iconos (capucha, túnica, faldón, sandalias, halo, cuernos), las texturas del halo y los cuernos
puestos, sus modelos de objeto, recetas, la etiqueta de cara y los textos.

Diseño para los dos lados de la fe: mitad marfil (lado derecho del que la lleva) y mitad negra (izquierdo), con
ribetes de oro y en el pecho un emblema de medio sol y media luna.
Uso: python3 dibujar_sagrado.py <carpeta src/main/resources>"""
import json
import math
import os
import sys

from PIL import Image

MARFIL = [(234, 228, 210), (218, 210, 190), (244, 240, 226)]
NEGRO = [(40, 36, 48), (30, 27, 36), (52, 47, 62)]
ORO = [(228, 184, 62), (184, 138, 40), (250, 214, 104)]
PLATA = (206, 214, 228)
CUERO = [(122, 82, 46), (98, 64, 34)]
NADA = (0, 0, 0, 0)


def h(x, y, s):
    n = (x * 73856093) ^ (y * 19349663) ^ (s * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def tela(paleta, x, y, s):
    r = h(x, y, s)
    c = paleta[0] if r < 0.7 else paleta[1]
    if x % 3 == 0 and h(x, 0, s) > 0.5:
        c = paleta[1]                                     # pliegues verticales
    return c + (255,)


def caja(img, u, v, w, alto, d, pintar):
    """Pinta las 6 caras de una caja del modelo (como las coloca Minecraft)."""
    caras = {"arriba": (u + d, v, w, d), "abajo": (u + d + w, v, w, d), "derecha": (u, v + d, d, alto),
             "frente": (u + d, v + d, w, alto), "izquierda": (u + d + w, v + d, d, alto), "detras": (u + 2 * d + w, v + d, w, alto)}
    for nombre, (x0, y0, cw, ch) in caras.items():
        for j in range(ch):
            for i in range(cw):
                c = pintar(nombre, i, j, cw, ch, x0 + i, y0 + j)
                if c is not None:
                    img.putpixel((x0 + i, y0 + j), c)


def mitad(cara, i, w):
    """¿Marfil? El lado derecho del que la lleva es marfil, el izquierdo negro."""
    if cara == "derecha":
        return True
    if cara == "izquierda":
        return False
    if cara in ("frente", "arriba", "abajo"):
        return i < w / 2                                  # de frente, el derecho del que la lleva queda a la izquierda
    return i >= w / 2                                     # por detrás, al revés


def capa1():
    img = Image.new("RGBA", (64, 32), NADA)

    # CAPUCHA (en la capa del sombrero, más grande): por fuera mitad y mitad, abierta por delante con ribete de oro
    def capucha(cara, i, j, w, a, x, y):
        if cara == "abajo":
            return None
        if cara == "frente":
            if j <= 1 or i == 0 or i == w - 1:
                return ORO[0] + (255,) if j == 1 or (j > 1) else ORO[1] + (255,)
            return None                                   # la cara se ve
        c = tela(MARFIL if mitad(cara, i, w) else NEGRO, x, y, 1)
        if cara in ("derecha", "izquierda", "detras") and j == a - 1:
            return ORO[1] + (255,)
        return c
    caja(img, 32, 0, 8, 8, 8, capucha)

    # TÚNICA: el cuerpo, mitad y mitad, con ribete central de oro, cuello en V y el emblema sol-luna
    def cuerpo(cara, i, j, w, a, x, y):
        marfil = mitad(cara, i, w)
        c = tela(MARFIL if marfil else NEGRO, x, y, 2)
        if cara == "frente":
            cx, cy = 3.5, 4.0
            d = math.hypot(i - cx, j - cy)
            if d <= 2.2:
                if i < 4:
                    return ORO[2] + (255,) if d < 1.2 else ORO[0] + (255,)       # medio sol
                return PLATA + (255,) if d > 0.9 else (150, 160, 182, 255)       # media luna
            if d <= 2.9 and i < 4 and (i + j) % 2 == 0:
                return ORO[1] + (255,)                                            # rayos
            if j <= 1 and abs(i - 3.5) <= 1.5 - j:
                return NEGRO[1] + (255,)                                          # el cuello en V
            if i in (3, 4) and j >= 7:
                return ORO[0] + (255,) if (j % 2) else ORO[1] + (255,)            # el ribete central
            if j == a - 1:
                return ORO[1] + (255,)
        if cara in ("derecha", "izquierda", "detras") and j == a - 1:
            return ORO[1] + (255,)
        return c
    caja(img, 16, 16, 8, 12, 4, cuerpo)

    # MANGAS: anchas, de marfil, con puño negro y ribete de oro (las dos iguales: Minecraft usa la misma textura)
    def manga(cara, i, j, w, a, x, y):
        if cara == "abajo":
            return NEGRO[1] + (255,)
        if j >= a - 3:
            return ORO[0] + (255,) if j == a - 3 else tela(NEGRO, x, y, 3)
        return tela(MARFIL, x, y, 3)
    caja(img, 40, 16, 4, 12, 4, manga)

    # SANDALIAS (en las piernas, solo abajo): suela y tiras de cuero, con una hebilla de oro
    def sandalia(cara, i, j, w, a, x, y):
        if cara == "abajo":
            return CUERO[1] + (255,)
        if cara == "arriba":
            return None
        if j == a - 1:
            return CUERO[0] + (255,)
        if j == a - 3 and cara != "detras":
            return CUERO[0] + (255,) if (i % 2 == 0 or cara == "frente") else None
        if cara == "frente" and j == a - 2 and i in (1, 2):
            return ORO[0] + (255,)
        return None
    caja(img, 0, 16, 4, 12, 4, sandalia)
    return img


def capa2():
    img = Image.new("RGBA", (64, 32), NADA)

    # FALDÓN: la cintura (fajín de oro) ...
    def cintura(cara, i, j, w, a, x, y):
        if cara in ("arriba", "abajo") or j < a - 5:
            return None
        if j == a - 5:
            return ORO[0] + (255,)
        return tela(MARFIL if mitad(cara, i, w) else NEGRO, x, y, 4)
    caja(img, 16, 16, 8, 12, 4, cintura)

    # ... y las piernas, hasta casi los pies, con el borde negro y dorado
    def pierna(cara, i, j, w, a, x, y):
        if cara in ("arriba", "abajo") or j >= a - 2:
            return None
        if j == a - 3:
            return ORO[1] + (255,)
        if j >= a - 5:
            return tela(NEGRO, x, y, 5)
        return tela(MARFIL, x, y, 5)
    caja(img, 0, 16, 4, 12, 4, pierna)
    return img


PALETA_ICONO = {
    "k": (26, 22, 30),                                      # contorno
    "w": MARFIL[2], "m": MARFIL[0], "M": (196, 186, 162),   # marfil: luz, base, sombra
    "g": (74, 67, 88), "n": NEGRO[2], "N": NEGRO[1],        # negro: luz, base, sombra
    "o": ORO[0], "O": ORO[1], "y": ORO[2],                  # oro: base, sombra, brillo (medio sol)
    "p": PLATA,                                             # plata (media luna)
    "s": (18, 14, 20),                                      # sombra dentro de la capucha
    "l": (150, 104, 62), "c": CUERO[0], "C": CUERO[1],      # cuero
    "h": (60, 50, 50), "r": (150, 40, 40), "b": (214, 206, 186),
}
# Al reflejar la mitad marfil para hacer la negra, cada color pasa a su pareja
ESPEJO = {"w": "g", "m": "n", "M": "N", "y": "p"}


def icono(dibujo):
    img = Image.new("RGBA", (16, 16), NADA)
    for y, fila in enumerate(dibujo):
        for x, ch in enumerate(fila):
            c = PALETA_ICONO.get(ch)
            if c:
                img.putpixel((x, y), c + (255,))
    return img


def simetrico(mitad, colores=True):
    """La mitad izquierda (8 columnas, marfil); la derecha es su reflejo en negro (o igual, si colores=False)."""
    filas = []
    for f in mitad:
        assert len(f) == 8, f
        derecha = "".join(ESPEJO.get(ch, ch) if colores else ch for ch in reversed(f))
        filas.append(f + derecha)
    return filas


ICONOS = {
    "capucha_sagrada": simetrico([
        "........", "........", "....kkkk", "..kkwwmo", ".kwwmmmo", ".kwmmmmo", "kwmmokkk", "kwmmosss",
        "kwmmosss", "kwmMosss", "kwmMosss", "kwMMoooo", "kMMmmmMo", "kkkkkkkk", "........", "........"]),
    "tunica_sagrada": simetrico([
        "........", "..kkkkk.", ".kwwwmok", "kwwmmmoy", "kwmmmmoy", "kwmmMmoo", "kok.kmmo", "kkk.kwmo",
        "....kwmo", "....kwmo", "....kwMo", "....kwMo", "....kMMo", "....kooo", "....kkkk", "........"]),
    "faldon_sagrado": simetrico([
        "........", "........", "..kkkkkk", "..kooooo", "..kwwmmy", "..kwmmmm", ".kwwmmmk", ".kwmmmk.",
        ".kwmmMk.", "kwmmMk..", "kwmmMk..", "kwmMMk..", "kooook..", "kkkkkk..", "........", "........"]),
    "sandalias_sagradas": simetrico([
        "........", "........", "........", "........", "........", "........", "..kkk...", ".kolok..",
        ".ko.ok..", "kolllok.", "kcooock.", "kCCCCCCk", ".kkkkkk.", "........", "........", "........"], colores=False),
    "halo": [
        "................", "................", "................", "................", "....oooooooo....",
        "..ooO......Ooo..", ".oO..........Oo.", ".oO..........Oo.", "..ooO......Ooo..", "....oooooooo....",
        "................", "................", "................", "................", "................", "................"],
    "cuernos": [
        "................", "..r..........r..", "..hr........rh..", "..hh........hh..", "...hh......hh...",
        "...hhh....hhh...", "....hhh..hhh....", "....hhh..hhh....", "...bhhh..hhhb...", "...bbhh..hhbb...",
        "....bb....bb....", "................", "................", "................", "................", "................"],
}


def textura_halo():
    img = Image.new("RGBA", (16, 16), NADA)
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), (ORO[2] if (x + y) % 3 else (255, 238, 160)) + (255,))
    return img


def textura_cuernos():
    img = Image.new("RGBA", (16, 16), NADA)
    for y in range(16):
        for x in range(16):
            if y < 8:
                c = (48, 40, 42) if h(x, y, 7) > 0.3 else (64, 54, 54)     # la base, oscura
                if x >= 8:
                    c = (72, 46, 46)                                       # el tramo medio, rojizo
            else:
                c = (150, 40, 40) if y < 10 else (196, 70, 60)             # la punta, roja
            img.putpixel((x, y), c + (255,))
    return img


def escribir(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(datos, f, ensure_ascii=False, indent=2)
        f.write("\n")


def main(res):
    a = os.path.join(res, "assets", "yonga")
    d = os.path.join(res, "data", "yonga")
    os.makedirs(os.path.join(a, "textures", "models", "armor"), exist_ok=True)
    capa1().save(os.path.join(a, "textures", "models", "armor", "tunica_layer_1.png"))
    capa2().save(os.path.join(a, "textures", "models", "armor", "tunica_layer_2.png"))
    textura_halo().save(os.path.join(a, "textures", "entity", "halo.png"))
    textura_cuernos().save(os.path.join(a, "textures", "entity", "cuernos.png"))
    for nombre, dibujo in ICONOS.items():
        icono(dibujo).save(os.path.join(a, "textures", "item", nombre + ".png"))
        escribir(os.path.join(a, "models", "item", nombre + ".json"),
                 {"parent": "minecraft:item/generated", "textures": {"layer0": "yonga:item/" + nombre}})
    # recetas: la túnica desde Oficial; el halo y los cuernos desde Experto
    cond = [{"type": "yonga:modulo_activo", "modulo": "roles"}]

    def receta(nombre, rango, patron, claves):
        escribir(os.path.join(d, "recipes", nombre + ".json"), {
            "conditions": cond, "type": "yonga:receta_oficio", "oficio": "sacerdote", "rango": rango,
            "category": "equipment", "pattern": patron, "key": claves, "result": {"item": "yonga:" + nombre}})
    lana, oro = {"tag": "minecraft:wool"}, {"item": "minecraft:gold_ingot"}
    receta("capucha_sagrada", "oficial", ["WWW", "WGW"], {"W": lana, "G": oro})
    receta("tunica_sagrada", "oficial", ["W W", "WGW", "WWW"], {"W": lana, "G": oro})
    receta("faldon_sagrado", "oficial", ["WGW", "W W", "W W"], {"W": lana, "G": oro})
    receta("sandalias_sagradas", "oficial", ["S S", "L L"], {"S": {"item": "minecraft:string"}, "L": {"item": "minecraft:leather"}})
    receta("halo", "experto", ["NGN", "GLG", "NGN"], {"N": {"item": "minecraft:gold_nugget"}, "G": oro,
                                                       "L": {"item": "minecraft:glowstone_dust"}})
    receta("cuernos", "experto", ["B B", "BOB"], {"B": {"item": "minecraft:bone"}, "O": {"item": "minecraft:crying_obsidian"}})
    # etiqueta de cara
    ruta = os.path.join(d, "tags", "items", "cara.json")
    with open(ruta, encoding="utf-8") as f:
        cara = json.load(f)
    for n in ("yonga:halo", "yonga:cuernos"):
        if n not in cara["values"]:
            cara["values"].append(n)
    escribir(ruta, cara)
    # textos
    es = {"item.yonga.capucha_sagrada": "Capucha sagrada", "item.yonga.tunica_sagrada": "Túnica sagrada",
          "item.yonga.faldon_sagrado": "Faldón sagrado", "item.yonga.sandalias_sagradas": "Sandalias sagradas",
          "item.yonga.halo": "Halo", "item.yonga.cuernos": "Cuernos",
          "yonga.sagrado.tunica": "Ni la luz ni la sombra la atraviesan.",
          "yonga.sagrado.halo": "Brilla donde la oscuridad muerde.",
          "yonga.sagrado.cuernos": "Lo que la luz teme, tú lo llevas en la frente."}
    en = {"item.yonga.capucha_sagrada": "Sacred Hood", "item.yonga.tunica_sagrada": "Sacred Robe",
          "item.yonga.faldon_sagrado": "Sacred Skirt", "item.yonga.sandalias_sagradas": "Sacred Sandals",
          "item.yonga.halo": "Halo", "item.yonga.cuernos": "Horns",
          "yonga.sagrado.tunica": "Neither light nor shadow gets through.",
          "yonga.sagrado.halo": "It shines where darkness bites.",
          "yonga.sagrado.cuernos": "What the light fears, you wear on your brow."}
    for archivo, nuevos in (("es_es.json", es), ("en_us.json", en)):
        ruta = os.path.join(a, "lang", archivo)
        with open(ruta, encoding="utf-8") as f:
            datos = json.load(f)
        datos.update(nuevos)
        escribir(ruta, datos)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else ".")
