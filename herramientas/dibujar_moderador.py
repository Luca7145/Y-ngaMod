"""El guante del moderador: el dibujo del inventario (un guante blanco con puño rojo y ribete dorado) y su modelo,
que lo esconde en la mano (escala 0 en primera y tercera persona), además de los textos.
Uso: python3 dibujar_moderador.py <carpeta src/main/resources>"""
import json
import os
import sys

from PIL import Image

MATERIALES = {
    # (luz, normal, sombra)
    "guante": ((250, 250, 248), (226, 226, 222), (184, 184, 182)),
    "puno": ((214, 60, 60), (176, 34, 38), (120, 20, 26)),
    "oro": ((252, 222, 120), (232, 180, 50), (176, 128, 30)),
}
CONTORNO = (30, 26, 30, 255)


def forma():
    p = {}
    for x in (6, 8, 10):                                     # tres dedos
        for y in range(2, 8):
            p[(x, y)] = "guante"
    for y in range(3, 8):                                    # el meñique, más corto
        p[(12, y)] = "guante"
    for x in range(5, 13):                                   # la palma
        for y in range(7, 12):
            p[(x, y)] = "guante"
    for xy in ((2, 5), (3, 5), (3, 6), (4, 6), (4, 7)):      # el pulgar
        p[xy] = "guante"
    for x in range(5, 13):                                   # el puño
        p[(x, 12)] = "oro"
        p[(x, 13)] = "puno"
        p[(x, 14)] = "puno"
    return p


def dibujar():
    p = forma()
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            if (x, y) not in p:
                if any((x + dx, y + dy) in p for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    img.putpixel((x, y), CONTORNO)
                continue
            luz, normal, sombra = MATERIALES[p[(x, y)]]
            if (x - 1, y) not in p or (x, y - 1) not in p:
                c = luz
            elif (x + 1, y) not in p or (x, y + 1) not in p:
                c = sombra
            else:
                c = normal
            img.putpixel((x, y), c + (255,))
    return img


def guardar(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(datos, f, indent=2, ensure_ascii=False)
        f.write("\n")


def main(raiz):
    a = os.path.join(raiz, "assets", "yonga")
    dibujar().save(os.path.join(a, "textures", "item", "guante_moderador.png"))
    nada = {"scale": [0, 0, 0]}
    guardar(os.path.join(a, "models", "item", "guante_moderador.json"), {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "yonga:item/guante_moderador"},
        "display": {"firstperson_righthand": nada, "firstperson_lefthand": nada,
                    "thirdperson_righthand": nada, "thirdperson_lefthand": nada, "head": nada}})
    for f, textos in (("es_es.json", {"item.yonga.guante_moderador": "Guante del moderador",
                                      "yonga.guante.inventario_de": "Inventario de %s"}),
                      ("en_us.json", {"item.yonga.guante_moderador": "Moderator's Glove",
                                      "yonga.guante.inventario_de": "%s's inventory"})):
        ruta = os.path.join(a, "lang", f)
        with open(ruta, encoding="utf-8") as fh:
            datos = json.load(fh)
        datos.update(textos)
        guardar(ruta, datos)


if __name__ == "__main__":
    main(sys.argv[1])
