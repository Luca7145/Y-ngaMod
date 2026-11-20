"""Los discos de música propios. A partir de la lista DISCOS genera, para cada uno: el dibujo (negro con surcos y
reflejos, y la etiqueta de color en el centro, como los de Minecraft), el modelo, su entrada en sounds.json, el
título en los idiomas, las etiquetas de Minecraft (es un disco; lo sueltan los creepers que mata un esqueleto) y la
lista de discos que pueden salir en los cofres (data/yonga/loot_modifiers/discos_*.json).

Además, cada disco necesita:
  - el sonido en assets/yonga/sounds/<nombre>.ogg, en MONO (si no, no baja con la distancia):
        ffmpeg -i cancion.mp3 -map 0:a -ac 1 -ar 44100 -c:a libvorbis -q:a 5 -map_metadata -1 <nombre>.ogg
  - su línea en core/ModItems con la duración en ticks (segundos x 20).

Uso: python3 dibujar_discos.py <carpeta src/main/resources>"""
import glob
import json
import math
import os
import sys

from PIL import Image

CONTORNO = (18, 16, 20, 255)
VINILO = [(38, 36, 42, 255), (30, 28, 34, 255)]          # surcos alternos
BRILLO = (96, 94, 104, 255)
AGUJERO = (0, 0, 0, 0)

# nombre -> (título, colores de la etiqueta: base, sombra, brillo)
DISCOS = {
    "disco_mamada_sobre_mamada_v1": ("Mamada sobre Mamada v1", ((232, 176, 48), (176, 124, 24), (252, 222, 120))),
    "disco_mamada_sobre_mamada": ("Mamada sobre Mamada v2", ((214, 60, 120), (158, 34, 86), (246, 132, 178))),
    "disco_mamada_sobre_mamada_v3": ("Mamada sobre Mamada v3", ((40, 178, 170), (22, 120, 116), (120, 228, 216))),
    "disco_media_vita": ("Media Vita", ((120, 40, 40), (80, 22, 26), (180, 80, 76))),
    "disco_baka_mitai": ("Baka Mitai", ((236, 236, 240), (180, 180, 190), (255, 255, 255))),
    "disco_caballo_homosexual": ("Caballo Homosexual de las Montañas",
                                 ((156, 92, 220), (108, 56, 168), (204, 160, 248))),
    "disco_papa_palpa": ("Papa Palpa is the Senate", ((60, 60, 70), (34, 34, 40), (200, 40, 40))),
    "disco_call_me_maybe": ("Call me Maybe", ((80, 150, 236), (46, 100, 184), (156, 202, 250))),
    "disco_caramelldansen": ("Caramelldansen", ((250, 128, 40), (200, 86, 20), (255, 186, 110))),
    "disco_virtual_insanity": ("Virtual Insanity", ((236, 72, 48), (180, 40, 26), (255, 150, 120))),
    "disco_pipo_pipo": ("Pipo Pipo", ((250, 170, 200), (214, 120, 160), (255, 220, 234))),
    "disco_sexbomb_rayman": ("Sexbomb Rayman", ((140, 60, 170), (96, 34, 124), (230, 120, 40))),
    "disco_its_raining_men": ("It's Raining Men", ((70, 120, 180), (40, 78, 130), (240, 240, 250))),
    "disco_pitito_con_el_cierre": ("Me Agarré el Pitito Con el Cierre", ((150, 160, 170), (100, 108, 118), (214, 220, 226))),
    "disco_somewhere_on_kamino": ("Somewhere on Kamino", ((96, 170, 200), (56, 116, 146), (200, 232, 244))),
    "disco_puton_del_barrio": ("El Putón del Barrio", ((230, 40, 90), (170, 20, 60), (40, 40, 40))),
    "disco_flyday_chinatown": ("Flyday Chinatown", ((60, 200, 220), (240, 110, 180), (250, 240, 170))),
    "disco_stay_with_me": ("Stay With Me", ((44, 52, 120), (24, 28, 76), (236, 170, 200))),
    "disco_drunken_sailor": ("What Shall We Do with the Drunken Sailor", ((130, 90, 50), (90, 58, 30), (40, 80, 150))),
    "disco_chum_bucket_rhumba": ("The Chum Bucket Rhumba", ((90, 170, 70), (56, 120, 42), (170, 220, 140))),
}


def disco(etiqueta):
    base, sombra, brillo = etiqueta
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d > 7.2:
                continue
            if d > 6.4:
                c = CONTORNO
            elif d < 0.8:
                c = AGUJERO
            elif d < 1.4:
                c = CONTORNO
            elif d < 3.0:
                # etiqueta: luz arriba a la izquierda, sombra abajo a la derecha
                c = brillo if (x + y) < 13 and d > 2.0 else (sombra if (x + y) > 16 else base)
                c = c + (255,)
            else:
                c = VINILO[int(d * 2) % 2]
                ang = math.degrees(math.atan2(y - cy, x - cx))
                if -150 < ang < -110 or 30 < ang < 70:      # dos reflejos de luz en diagonal
                    c = BRILLO if int(d * 2) % 2 == 0 else (70, 68, 78, 255)
            img.putpixel((x, y), c)
    return img


def leer(ruta):
    with open(ruta, encoding="utf-8") as f:
        return json.load(f)


def guardar(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(datos, f, indent=2, ensure_ascii=False)
        f.write("\n")


def main(raiz):
    a = os.path.join(raiz, "assets", "yonga")
    ids = ["yonga:" + n for n in DISCOS]
    sonidos = leer(os.path.join(a, "sounds.json"))
    idiomas = {f: leer(os.path.join(a, "lang", f)) for f in ("es_es.json", "en_us.json")}
    for nombre, (titulo, etiqueta) in DISCOS.items():
        if not os.path.exists(os.path.join(a, "sounds", nombre + ".ogg")):
            print("AVISO: falta el sonido", nombre + ".ogg")
        disco(etiqueta).save(os.path.join(a, "textures", "item", nombre + ".png"))
        guardar(os.path.join(a, "models", "item", nombre + ".json"),
                {"parent": "minecraft:item/generated", "textures": {"layer0": "yonga:item/" + nombre}})
        sonidos[nombre] = {"sounds": [{"name": "yonga:" + nombre, "stream": True}]}
        for f, texto in idiomas.items():
            texto["item.yonga." + nombre] = "Disco de música" if f == "es_es.json" else "Music Disc"
            texto["item.yonga." + nombre + ".desc"] = titulo
    guardar(os.path.join(a, "sounds.json"), sonidos)
    for f, texto in idiomas.items():
        guardar(os.path.join(a, "lang", f), texto)
    for etiqueta in ("music_discs", "creeper_drop_music_discs"):
        guardar(os.path.join(raiz, "data", "minecraft", "tags", "items", etiqueta + ".json"),
                {"replace": False, "values": ids})
    for ruta in glob.glob(os.path.join(raiz, "data", "yonga", "loot_modifiers", "discos_*.json")):
        datos = leer(ruta)
        datos["discos"] = ids
        guardar(ruta, datos)


if __name__ == "__main__":
    main(sys.argv[1])
