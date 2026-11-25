"""Estructuras (.nbt) de las aldeas de cada época y las piscinas de plantillas que sustituyen a las casas de Minecraft.

Las aldeas de Minecraft se montan con piezas (calles, casas, centro) unidas por bloques jigsaw. Las calles buscan sus
casas en la piscina "minecraft:village/<bioma>/houses": aquí se sobrescribe esa piscina (y la de las aldeas zombi)
para que, al generarse el mundo, en lugar de las casas de siempre salgan las de la época.

Cada casa lleva:
  · un jigsaw en el borde, delante de la puerta, llamado "minecraft:building_entrance" (por ahí se engancha a la calle);
  · otro jigsaw dentro, mirando arriba, que pide un aldeano a "minecraft:village/plains/villagers" (luego el mod lo
    convierte en habitante de la época).

Uso: python3 generar_aldeas.py <carpeta src/main/resources>"""
import gzip
import json
import math
import os
import struct
import sys

VERSION_DATOS = 3465          # 1.20.1


# ================================================================= NBT mínimo (big endian, comprimido con gzip)

class Int(int):
    pass


class Lista(list):
    def __init__(self, tipo, valores):
        super().__init__(valores)
        self.tipo = tipo


def _cadena(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def _carga(v):
    if isinstance(v, dict):
        out = b""
        for k, x in v.items():
            out += bytes([_tipo(x)]) + _cadena(k) + _carga(x)
        return out + b"\x00"
    if isinstance(v, Lista):
        out = bytes([v.tipo]) + struct.pack(">i", len(v))
        for x in v:
            out += _carga(x)
        return out
    if isinstance(v, str):
        return _cadena(v)
    if isinstance(v, int):
        return struct.pack(">i", v)
    raise TypeError(type(v))


def _tipo(v):
    if isinstance(v, dict):
        return 10
    if isinstance(v, Lista):
        return 9
    if isinstance(v, str):
        return 8
    if isinstance(v, int):
        return 3
    raise TypeError(type(v))


def guardar_nbt(ruta, raiz):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    datos = bytes([10]) + _cadena("") + _carga(raiz)
    with open(ruta, "wb") as bruto, gzip.GzipFile(fileobj=bruto, mode="wb", mtime=0) as f:   # siempre igual
        f.write(datos)


# ================================================================= plantilla

class Plantilla:
    def __init__(self, ancho, alto, fondo):
        self.tam = (ancho, alto, fondo)
        self.bloques = {}            # (x, y, z) -> (nombre, propiedades, nbt)

    def poner(self, x, y, z, nombre, props=None, nbt=None):
        w, h, d = self.tam
        if 0 <= x < w and 0 <= y < h and 0 <= z < d:
            self.bloques[(x, y, z)] = (nombre, tuple(sorted((props or {}).items())), nbt)

    def leer(self, x, y, z):
        b = self.bloques.get((x, y, z))
        return b[0] if b else None

    # Bloques que se unen con sus vecinos (en la plantilla hay que dejarlo ya calculado: al colocarse no se recalcula)
    @staticmethod
    def _tipo_union(nombre):
        n = nombre.split(":")[1]
        if n.endswith("_pane") or n == "iron_bars":
            return "panel"
        if n.endswith("_fence"):
            return "valla"
        if n.endswith("_wall"):
            return "muro"
        return None

    @staticmethod
    def _macizo(nombre):
        n = nombre.split(":")[1]
        if nombre.startswith("yonga:") and n in MUEBLES:
            return False
        nada = ("air", "jigsaw", "water", "torch", "lantern", "bell", "campfire", "composter", "brewing_stand",
                "stonecutter", "grindstone")
        partes = ("stairs", "slab", "door", "bed", "fence", "pane", "bars", "wall", "torch", "lantern", "potted",
                  "pressure_plate", "carpet", "sign", "ladder", "trapdoor", "flower")
        return n not in nada and not any(t in n for t in partes)

    def unir(self):
        lados = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
        nuevos = {}
        for (x, y, z), (nombre, props, nbt) in self.bloques.items():
            tipo = self._tipo_union(nombre)
            if not tipo:
                continue
            pr = dict(props)
            unidos = []
            for lado, (dx, dz) in lados.items():
                vecino = self.leer(x + dx, y, z + dz)
                une = False
                if vecino:
                    tv = self._tipo_union(vecino)
                    if tipo == "valla":
                        une = tv == "valla" or self._macizo(vecino)
                    else:
                        une = tv in ("panel", "muro") or self._macizo(vecino)
                if tipo == "muro":
                    pr[lado] = "low" if une else "none"
                else:
                    pr[lado] = "true" if une else "false"
                if une:
                    unidos.append(lado)
            if tipo == "muro":
                recto = sorted(unidos) in (["north", "south"], ["east", "west"])
                arriba = self.leer(x, y + 1, z)
                pr["up"] = "false" if recto and not arriba else "true"
            pr.setdefault("waterlogged", "false")
            nuevos[(x, y, z)] = (nombre, tuple(sorted(pr.items())), nbt)
        self.bloques.update(nuevos)

    def guardar(self, ruta):
        self.unir()
        paleta, indice, bloques = [], {}, []
        for (x, y, z), (nombre, props, nbt) in sorted(self.bloques.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
            clave = (nombre, props)
            if clave not in indice:
                indice[clave] = len(paleta)
                e = {"Name": nombre}
                if props:
                    e["Properties"] = {k: v for k, v in props}
                paleta.append(e)
            b = {"pos": Lista(3, [x, y, z]), "state": indice[clave]}
            if nbt:
                b["nbt"] = nbt
            bloques.append(b)
        raiz = {
            "DataVersion": VERSION_DATOS,
            "size": Lista(3, list(self.tam)),
            "palette": Lista(10, paleta),
            "blocks": Lista(10, bloques),
            "entities": Lista(10, []),
        }
        guardar_nbt(ruta, raiz)


def jigsaw(p, x, y, z, orientacion, nombre, objetivo, piscina, final="minecraft:air", union="rollable"):
    p.poner(x, y, z, "minecraft:jigsaw", {"orientation": orientacion}, {
        "id": "minecraft:jigsaw", "name": nombre, "target": objetivo, "pool": piscina,
        "final_state": final, "joint": union})


# ================================================================= CHOZA PREHISTÓRICA

def elipse(x, z, cx, cz, rx, rz):
    if rx <= 0 or rz <= 0:
        return 99
    return ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2


import generar_muebles as _GM                        # los muebles del mod (nombres y si se atraviesan)
MUEBLES = set(_GM.M)
ALTOS = {"yonga:" + n for n, d in _GM.M.items() if d["arriba"]}            # de dos bloques de alto
GUARDAN = {"yonga:" + n for n, d in _GM.M.items() if d["huecos"] >= 9}       # se guardan cosas dentro
PASABLES = {"yonga:" + n for n, d in _GM.M.items() if d["pasable"]} | {"yonga:piel_suelo"}

# Qué se pone en las casas de cada época:
#   "suelo": muebles contra la pared (nombre, o (nombre, lo que va encima)); "pared": colgados a la altura de la vista;
#   "alfombra": en medio del cuarto. Con "arriba" (si lo hay) se amuebla el piso de arriba (los dormitorios).
AJUAR = {
    "prehistorica": {"suelo": ["yonga:lena_apilada", "yonga:vasija_barro", "yonga:cesto_mimbre", "yonga:piedra_moler",
                               "yonga:vasija_barro"],
                     "pared": ["yonga:pintura_rupestre", "yonga:astas_pared"], "alfombra": "yonga:piel_suelo"},
    "antigua": {"suelo": ["yonga:lecho_romano", ("yonga:mesa_marmol", "yonga:lucerna"), "yonga:anfora", "yonga:brasero",
                          "yonga:busto_marmol", "minecraft:decorated_pot", "yonga:anfora"],
                "pared": [], "alfombra": None},
    "medieval": {"suelo": ["yonga:rueca", "minecraft:barrel", "yonga:banco_madera", "yonga:candelabro_hierro",
                           "yonga:taburete", "minecraft:chest", "yonga:lena_apilada"],
                 "pared": ["yonga:tapiz", "yonga:tapiz"], "alfombra": None},
    "moderna": {"suelo": [("yonga:comoda", "yonga:candelabro_plata"), "yonga:escritorio", "minecraft:bookshelf",
                          "yonga:silla_tapizada", "yonga:globo_terraqueo", "minecraft:bookshelf"],
                "arriba": [("yonga:comoda", "yonga:candelabro_plata"), "yonga:silla_tapizada", "minecraft:bookshelf",
                           "yonga:globo_terraqueo", "yonga:escritorio"],
                "pared": ["yonga:cuadro_paisaje", "yonga:reloj_pared", "yonga:cuadro_paisaje"], "alfombra": "minecraft:red_carpet"},
    "industrial": {"suelo": ["yonga:estufa_hierro", "yonga:gramofono", "yonga:perchero", "yonga:maquina_coser",
                             "yonga:radiador", "minecraft:bookshelf"],
                   "arriba": ["yonga:radiador", ("yonga:comoda", "yonga:candelabro_plata"), "yonga:perchero",
                              "yonga:silla_tapizada", "yonga:gramofono"],
                   "pared": ["yonga:lampara_gas", "yonga:cuadro_paisaje", "yonga:reloj_pared", "yonga:lampara_gas"],
                   "alfombra": "minecraft:brown_carpet"},
    "contemporanea": {"suelo": ["yonga:sofa", "yonga:television", "yonga:nevera", "yonga:planta_maceta",
                                "yonga:lampara_pie", ("yonga:mesa_moderna", "yonga:ordenador"), "yonga:sofa"],
                      "arriba": [("yonga:mesa_moderna", "yonga:ordenador"), "yonga:planta_maceta", "yonga:lampara_pie",
                                 "yonga:sofa", "yonga:television"],
                      "pared": ["yonga:cuadro_paisaje", "yonga:cuadro_paisaje"], "alfombra": "minecraft:light_gray_carpet"},
}

_ATRAS = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}      # hacia dónde está la pared
_FRENTE = {"north": "south", "south": "north", "west": "east", "east": "west"}       # y hacia dónde mira el mueble


def amueblar(p, region, y, entrada, usadas, epoca, piso="suelo", semilla=0, reservadas=(), maximo=None):
    """Pone los muebles de la época en un cuarto. region: casillas (x, z) del suelo; y: altura a la que van los muebles
    (la del suelo + 1); entrada: la casilla por la que se entra (puerta o rellano de la escalera).
    Solo se ponen contra las paredes y cada uno se prueba antes: si deja algún rincón, cama o puesto de trabajo sin
    poder llegar desde la entrada, se quita. Las casillas ocupadas se añaden a usadas."""
    ajuar = AJUAR[epoca]
    objetos = list(ajuar.get(piso) or ajuar["suelo"])
    region = set(region)
    if maximo is None:
        maximo = max(2, len(region) // 4)                  # que no quede atestado
    objetos = objetos[:maximo]
    reservadas = set(reservadas) | {entrada} | {(entrada[0] + dx, entrada[1] + dz) for dx, dz in _ATRAS.values()}
    puestos = set(PUESTOS.values())

    def aire(b):
        return b is None or b == "minecraft:air" or b in PASABLES or b.endswith("_carpet")

    def pisable(c):
        return c in region and aire(p.leer(c[0], y, c[1])) and aire(p.leer(c[0], y + 1, c[1]))

    def conectado():
        vistos, cola = {entrada}, [entrada]
        while cola:
            x, z = cola.pop()
            for dx, dz in _ATRAS.values():
                v = (x + dx, z + dz)
                if v not in vistos and pisable(v):
                    vistos.add(v); cola.append(v)
        for c in region:
            b = p.leer(c[0], y, c[1]) or "minecraft:air"
            if pisable(c) and c not in vistos:
                return False
            if (b.endswith("_bed") or b in puestos) and not any((c[0] + dx, c[1] + dz) in vistos for dx, dz in _ATRAS.values()):
                return False
        return True

    def paredes(c, altura):
        """Lados de la casilla con pared maciza a esa altura."""
        lados = []
        for lado, (dx, dz) in _ATRAS.items():
            b = p.leer(c[0] + dx, altura, c[1] + dz)
            if b and Plantilla._macizo(b):
                lados.append(lado)
        return lados

    orden = sorted(region, key=lambda c: _hash(c[0], c[1], 71 + semilla))
    for obj in objetos:
        nombre, encima = obj if isinstance(obj, tuple) else (obj, None)
        for c in orden:
            if c in usadas or c in reservadas or not pisable(c) or (c[0], c[1]) == entrada:
                continue
            lados = paredes(c, y)
            if not lados:
                continue
            lado = lados[0]
            props = {"facing": _FRENTE[lado]} if nombre.startswith("yonga:") or nombre.endswith(("chest", "barrel")) else None
            if nombre.endswith("barrel"):
                props = {"facing": "up", "open": "false"}
            elif nombre.endswith("chest"):
                props = {"facing": _FRENTE[lado], "type": "single", "waterlogged": "false"}
            alta = nombre in ALTOS
            if alta and not aire(p.leer(c[0], y + 1, c[1])):
                continue
            if alta:
                props = dict(props, half="lower")
            nbt = None
            if nombre in GUARDAN and _hash(c[0], c[1], 83 + semilla) < 0.6:     # algunos traen cosas dentro
                nbt = {"LootTable": "minecraft:chests/village/village_butcher" if nombre == "yonga:nevera"
                       else "minecraft:chests/village/village_plains_house"}
            antes = p.leer(c[0], y, c[1])
            p.poner(c[0], y, c[1], nombre, props, nbt)
            if conectado():
                usadas.add(c)
                if alta:
                    p.poner(c[0], y + 1, c[1], nombre, dict(props, half="upper"))
                elif encima and aire(p.leer(c[0], y + 1, c[1])):
                    p.poner(c[0], y + 1, c[1], encima, {"facing": _FRENTE[lado]})
                break
            p.poner(c[0], y, c[1], antes or "minecraft:air")
    # Colgados en la pared, a la altura de la vista (no pegados unos a otros)
    colgados = []
    for nombre in ajuar["pared"]:
        for c in orden:
            if c in reservadas and c == entrada:
                continue
            if not aire(p.leer(c[0], y + 1, c[1])) or p.leer(c[0], y + 1, c[1]) in PASABLES:
                continue
            if any(abs(c[0] - o[0]) + abs(c[1] - o[1]) < 3 for o in colgados):
                continue
            lados = paredes(c, y + 1)
            if not lados or not aire(p.leer(c[0], y + 2, c[1])):
                continue
            p.poner(c[0], y + 1, c[1], nombre, {"facing": _FRENTE[lados[0]]})
            colgados.append(c)
            break
    # Una alfombra en medio
    if ajuar["alfombra"]:
        medio = [c for c in region if pisable(c) and not paredes(c, y) and c != entrada]
        if medio:
            cx = sum(c[0] for c in region) / len(region); cz = sum(c[1] for c in region) / len(region)
            c = min(medio, key=lambda c: (c[0] - cx) ** 2 + (c[1] - cz) ** 2)
            props = {"facing": "north"} if ajuar["alfombra"].startswith("yonga:") else None
            for v in [c] + [(c[0] + dx, c[1] + dz) for dx, dz in ((1, 0), (0, 1), (1, 1))]:
                if v in medio and ajuar["alfombra"].endswith("_carpet"):
                    p.poner(v[0], y, v[1], ajuar["alfombra"])
            if not ajuar["alfombra"].endswith("_carpet"):
                p.poner(c[0], y, c[1], ajuar["alfombra"], props)


PUESTOS = {
    "composter": "minecraft:composter", "smoker": "minecraft:smoker", "smithing": "minecraft:smithing_table",
    "furnace": "minecraft:furnace", "barrel": "minecraft:barrel", "fletching": "minecraft:fletching_table",
    "stonecutter": "minecraft:stonecutter", "loom": "minecraft:loom", "brewing": "minecraft:brewing_stand",
}


def choza(ancho, fondo, puesto=None, camas=1):
    """Choza redonda u ovalada de barro y paja. La puerta mira al norte (z = 0), donde está el jigsaw de la calle.
    Se deja un pasillo de 2 delante de la puerta (de ahí sale el camino a la calle)."""
    margen = 2                                       # filas de delante para el caminito
    w, d = ancho, fondo + margen
    rx, rz = max(2.0, ancho / 2.0 - 1.0), max(2.0, fondo / 2.0 - 1.0)
    cx, cz = (ancho - 1) / 2.0, margen + (fondo - 1) / 2.0
    alto = 4 + int(max(rx, rz) + 2) + 2
    p = Plantilla(w, alto, d)
    dentro = {(x, z) for x in range(w) for z in range(d) if elipse(x, z, cx, cz, rx + 0.3, rz + 0.3) <= 1}
    pared = {(x, z) for (x, z) in dentro if any((x + dx, z + dz) not in dentro for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))}
    for (x, z) in dentro:
        p.poner(x, 0, z, "minecraft:packed_mud")
        for y in range(1, 4):
            p.poner(x, y, z, "minecraft:air")
    for (x, z) in pared:
        poste = (x * 3 + z * 5) % 4 == 0
        for y in (1, 2):
            p.poner(x, y, z, "minecraft:stripped_spruce_log" if poste else "minecraft:mud_bricks")
        p.poner(x, 3, z, "minecraft:stripped_spruce_log", {"axis": "x" if rx >= rz else "z"})
    # Tejado cónico de paja (y aire por dentro del cono, por si la choza se mete en una ladera)
    k = 0
    while True:
        ax, az = rx + 1.0 - k * 0.9, rz + 1.0 - k * 0.9
        y = 4 + k
        if ax < 0.8 or az < 0.8:
            p.poner(int(round(cx)), y, int(round(cz)), "minecraft:hay_block")
            break
        for x in range(math.floor(cx - ax), math.ceil(cx + ax) + 1):
            for z in range(math.floor(cz - az), math.ceil(cz + az) + 1):
                e = elipse(x, z, cx, cz, ax, az)
                if e <= 1 and elipse(x, z, cx, cz, ax - 1.6, az - 1.6) > 1:
                    p.poner(x, y, z, "minecraft:hay_block")
                elif e <= 1:
                    p.poner(x, y, z, "minecraft:air")
        k += 1
    # Puerta al norte, en medio
    px = int(round(cx))
    pz = min(z for (x, z) in pared if x == px)
    for y in (1, 2):
        p.poner(px, y, pz, "minecraft:air")
    for z in range(0, pz):                           # el caminito hasta el borde
        p.poner(px, 0, z, "minecraft:dirt_path")
        for y in (1, 2, 3):
            p.poner(px, y, z, "minecraft:air")
    # (Las chozas van en las calles de Minecraft: ahí el jigsaw a la altura de la puerta las deja a ras de suelo)
    jigsaw(p, px, 1, 0, "north_up", "minecraft:building_entrance", "minecraft:empty", "minecraft:empty")

    # Dentro
    libres = [(x, z) for (x, z) in dentro if (x, z) not in pared and (x, z) != (px, pz + 1)]
    junto = [(x, z) for (x, z) in libres if any((x + dx, z + dz) in pared for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))]
    usadas = set()
    centro = (int(round(cx)), int(round(cz)))
    if len(libres) >= 14 and centro in libres:
        p.poner(centro[0], 1, centro[1], "minecraft:campfire", {"lit": "false"})
        usadas.add(centro)
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            usadas.add((centro[0] + dx, centro[1] + dz))
    fondo_orden = sorted(junto, key=lambda c: -((c[0] - px) ** 2 + (c[1] - pz) ** 2))
    if puesto:
        for c in fondo_orden:
            if c not in usadas:
                p.poner(c[0], 1, c[1], PUESTOS[puesto])
                usadas.add(c)
                break
    puestas = 0
    for cab in fondo_orden:
        if puestas >= camas:
            break
        if cab in usadas:
            continue
        mejor, dirc = (cab[0] - cx) ** 2 + (cab[1] - cz) ** 2, None
        for dx, dz, nom in ((1, 0, "east"), (-1, 0, "west"), (0, 1, "south"), (0, -1, "north")):
            pie = (cab[0] + dx, cab[1] + dz)
            dist = (pie[0] - cx) ** 2 + (pie[1] - cz) ** 2
            if pie in libres and pie not in usadas and dist < mejor:
                mejor, dirc = dist, (dx, dz, nom)
        if not dirc:
            continue
        dx, dz, nom = dirc
        pie = (cab[0] + dx, cab[1] + dz)
        opuesto = {"east": "west", "west": "east", "south": "north", "north": "south"}[nom]
        p.poner(pie[0], 1, pie[1], "minecraft:brown_bed", {"part": "foot", "facing": opuesto})
        p.poner(cab[0], 1, cab[1], "minecraft:brown_bed", {"part": "head", "facing": opuesto})
        usadas.update({pie, cab, (cab[0] + 1, cab[1]), (cab[0] - 1, cab[1]), (cab[0], cab[1] + 1), (cab[0], cab[1] - 1)})
        puestas += 1
    # Antorcha cerca de la puerta y el sitio del aldeano (jigsaw mirando arriba)
    cerca = sorted(junto, key=lambda c: (c[0] - px) ** 2 + (c[1] - pz) ** 2)
    for c in cerca:
        if c not in usadas and (c[0] - px) ** 2 + (c[1] - pz) ** 2 >= 2:
            p.poner(c[0], 1, c[1], "minecraft:torch")
            usadas.add(c)
            break
    amueblar(p, libres + [(px, pz + 1)], 1, (px, pz + 1), usadas, "prehistorica", semilla=ancho * 7 + fondo)
    for c in sorted(libres, key=lambda c: (c[0] - cx) ** 2 + (c[1] - cz) ** 2):
        if c not in usadas:
            jigsaw(p, c[0], 1, c[1], "up_north", "minecraft:bottom", "minecraft:bottom",
                   "minecraft:village/plains/villagers")
            break
    return p


# ================================================================= ÉPOCAS

BIOMAS = ["plains", "desert", "savanna", "snowy", "taiga"]


def prehistorica(raiz):
    """Las chozas y su piscina (peso: más casas de dormir que talleres, como en las aldeas de siempre)."""
    base = os.path.join(raiz, "data", "yonga", "structures", "aldea", "prehistorica")
    elementos = []

    def anadir(nombre, plantilla, peso):
        plantilla.guardar(os.path.join(base, nombre + ".nbt"))
        elementos.append({"weight": peso, "element": {
            "element_type": "minecraft:single_pool_element", "location": "yonga:aldea/prehistorica/" + nombre,
            "processors": "minecraft:empty", "projection": "rigid"}})

    anadir("choza_pequena_1", choza(7, 7, camas=1), 4)
    anadir("choza_pequena_2", choza(7, 8, camas=2), 3)
    anadir("choza_mediana", choza(9, 9, camas=2), 3)
    anadir("choza_larga", choza(11, 9, camas=3), 2)
    anadir("choza_grande", choza(11, 11, camas=4), 1)
    for i, puesto in enumerate(PUESTOS):
        anadir("taller_" + puesto, choza(9 if i % 2 else 8, 8, puesto=puesto, camas=1), 2)
    # La aldea prehistórica es la de Minecraft (sus centros y sus calles, de llanura) con las casas cambiadas
    estructura(raiz, "aldea_prehistorica", "minecraft:village/plains/town_centers")
    piscina = {"fallback": "minecraft:empty", "elements": elementos}
    for bioma in BIOMAS:
        for sub in ("houses", "zombie/houses"):
            ruta = os.path.join(raiz, "data", "minecraft", "worldgen", "template_pool", "village", bioma, sub + ".json")
            os.makedirs(os.path.dirname(ruta), exist_ok=True)
            with open(ruta, "w", encoding="utf-8") as f:
                json.dump(piscina, f, indent=2)
    return len(elementos)


# ================================================================= ANTIGÜEDAD (Hispania romana)
# Estructura propia "yonga:aldea_antigua": un FORO en el centro (plaza con la campana sobre un pedestal, columnas,
# bancos y faroles), CALLES empedradas que se ramifican (rectas y cruces) y CASAS de estuco blanco con zócalo ocre,
# columnas de cuarzo en las esquinas, rejas en las ventanas y tejado de teja (escaleras de ladrillo).

def _hash(x, z, s=0):
    n = (x * 73856093) ^ (z * 19349663) ^ (s * 83492791)
    return (n & 0xFFFF) / 0xFFFF


def empedrado(x, z):
    r = _hash(x, z, 5)
    return "minecraft:polished_andesite" if r < 0.5 else "minecraft:stone_bricks" if r < 0.8 else "minecraft:cobblestone"


VILLAGERS = "minecraft:village/plains/villagers"
CALLES = "yonga:aldea/antigua/calles"          # (las de la antigua; las demás épocas pasan las suyas)
CASAS = "yonga:aldea/antigua/casas"


def calle(largo, lados, suelo=empedrado, calles=CALLES, casas=CASAS):
    """Calle recta de 3 de ancho: se engancha por detrás, sigue por delante y tiene sitios para casas a los lados
    (lados = posiciones z donde sale una casa a cada lado). suelo(x, z) da el bloque de cada casilla."""
    p = Plantilla(3, 3, largo)
    for z in range(largo):
        for x in range(3):
            p.poner(x, 0, z, suelo(x, z))
            p.poner(x, 1, z, "minecraft:air")
            p.poner(x, 2, z, "minecraft:air")
    jigsaw(p, 1, 0, 0, "north_up", "yonga:calle", "yonga:calle", "minecraft:empty", suelo(1, 0))
    jigsaw(p, 1, 0, largo - 1, "south_up", "yonga:calle", "yonga:calle", calles, suelo(1, largo - 1))
    for z in lados:
        jigsaw(p, 0, 0, z, "west_up", "yonga:lado", "minecraft:building_entrance", casas, suelo(0, z))
        jigsaw(p, 2, 0, z, "east_up", "yonga:lado", "minecraft:building_entrance", casas, suelo(2, z))
    return p


def cruce(tres=False, suelo=empedrado, calles=CALLES):
    p = Plantilla(3, 3, 3)
    for z in range(3):
        for x in range(3):
            p.poner(x, 0, z, suelo(x, z + 7))
            p.poner(x, 1, z, "minecraft:air")
            p.poner(x, 2, z, "minecraft:air")
    jigsaw(p, 1, 0, 0, "north_up", "yonga:calle", "yonga:calle", "minecraft:empty", suelo(1, 7))
    if not tres:
        jigsaw(p, 1, 0, 2, "south_up", "yonga:calle", "yonga:calle", calles, suelo(1, 9))
    jigsaw(p, 0, 0, 1, "west_up", "yonga:calle", "yonga:calle", calles, suelo(0, 8))
    jigsaw(p, 2, 0, 1, "east_up", "yonga:calle", "yonga:calle", calles, suelo(2, 8))
    return p


def fin_calle(suelo=empedrado):
    p = Plantilla(3, 3, 2)
    for z in range(2):
        for x in range(3):
            p.poner(x, 0, z, suelo(x, z + 3))
            p.poner(x, 1, z, "minecraft:air")
    jigsaw(p, 1, 0, 0, "north_up", "yonga:calle", "yonga:calle", "minecraft:empty", suelo(1, 3))
    return p


def calles_de(raiz, base, pref, suelo):
    """Las piezas de calle de una época y sus piscinas (calles, con fin_calle de reserva)."""
    calles, casas = pref + "calles", pref + "casas"
    calle(16, [3, 8, 13], suelo, calles, casas).guardar(os.path.join(base, "calle_larga.nbt"))
    calle(10, [3, 7], suelo, calles, casas).guardar(os.path.join(base, "calle_corta.nbt"))
    cruce(False, suelo, calles).guardar(os.path.join(base, "cruce.nbt"))
    cruce(True, suelo, calles).guardar(os.path.join(base, "cruce_t.nbt"))
    fin_calle(suelo).guardar(os.path.join(base, "fin_calle.nbt"))
    _piscina(raiz, calles, [_elemento(pref + "calle_larga", 4, "terrain_matching"),
                            _elemento(pref + "calle_corta", 3, "terrain_matching"),
                            _elemento(pref + "cruce", 2, "terrain_matching"),
                            _elemento(pref + "cruce_t", 2, "terrain_matching")],
             fallback=pref + "fin_calle")
    _piscina(raiz, pref + "fin_calle", [_elemento(pref + "fin_calle", 1, "terrain_matching")])


def estructura(raiz, nombre, inicio):
    """Una aldea como estructura propia (jigsaw, como las de Minecraft)."""
    escribir_json(raiz, ["data", "yonga", "worldgen", "structure", nombre + ".json"], {
        "type": "minecraft:jigsaw",
        "biomes": "#yonga:tiene_aldea",
        "step": "surface_structures",
        "spawn_overrides": {},
        "terrain_adaptation": "beard_thin",
        "start_pool": inicio,
        "size": 6,
        "start_height": {"absolute": 0},
        "project_start_to_heightmap": "WORLD_SURFACE_WG",
        "max_distance_from_center": 80,
        "use_expansion_hack": True,
    })


def foro():
    """Plaza de 17 x 17: losas de piedra lisa con cenefa de ladrillo de piedra, la campana sobre un pedestal en
    medio, cuatro columnas, bancos de cuarzo, faroles en postes y las cuatro calles saliendo de los lados."""
    n = 17
    p = Plantilla(n, 8, n)
    c = n // 2
    for x in range(n):
        for z in range(n):
            borde = x in (0, n - 1) or z in (0, n - 1) or x in (2, n - 3) and 2 <= z <= n - 3 or z in (2, n - 3) and 2 <= x <= n - 3
            p.poner(x, 0, z, "minecraft:stone_bricks" if borde else "minecraft:smooth_stone")
            for y in range(1, 8):
                p.poner(x, y, z, "minecraft:air")
    # Pedestal y campana
    p.poner(c, 1, c, "minecraft:chiseled_stone_bricks")
    p.poner(c, 2, c, "minecraft:bell", {"attachment": "floor", "facing": "north"})
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        p.poner(c + dx, 1, c + dz, "minecraft:stone_brick_slab", {"type": "bottom"})
    # Columnas
    for x, z in ((4, 4), (n - 5, 4), (4, n - 5), (n - 5, n - 5)):
        for y in range(1, 5):
            p.poner(x, y, z, "minecraft:quartz_pillar")
        p.poner(x, 5, z, "minecraft:chiseled_quartz_block")
    # Bancos mirando a la campana
    for dx, dz, mira in ((0, -3, "south"), (0, 3, "north"), (-3, 0, "east"), (3, 0, "west")):
        for o in (-1, 0, 1):
            x, z = (c + o, c + dz) if dx == 0 else (c + dx, c + o)
            p.poner(x, 1, z, "minecraft:quartz_stairs", {"facing": {"south": "north", "north": "south", "east": "west", "west": "east"}[mira], "half": "bottom"})
    # Faroles en postes
    for x, z in ((2, 2), (n - 3, 2), (2, n - 3), (n - 3, n - 3)):
        p.poner(x, 1, z, "minecraft:stone_brick_wall")
        p.poner(x, 2, z, "minecraft:stone_brick_wall")
        p.poner(x, 3, z, "minecraft:lantern", {"hanging": "false"})
    # Calles a los cuatro lados y dos aldeanos
    jigsaw(p, c, 0, 0, "north_up", "yonga:calle", "yonga:calle", CALLES, "minecraft:smooth_stone")
    jigsaw(p, c, 0, n - 1, "south_up", "yonga:calle", "yonga:calle", CALLES, "minecraft:smooth_stone")
    jigsaw(p, 0, 0, c, "west_up", "yonga:calle", "yonga:calle", CALLES, "minecraft:smooth_stone")
    jigsaw(p, n - 1, 0, c, "east_up", "yonga:calle", "yonga:calle", CALLES, "minecraft:smooth_stone")
    jigsaw(p, c - 4, 1, c + 2, "up_north", "minecraft:bottom", "minecraft:bottom", VILLAGERS)
    jigsaw(p, c + 4, 1, c - 2, "up_north", "minecraft:bottom", "minecraft:bottom", VILLAGERS)
    return p


def estuco(x, y, z):
    """Pared de estuco con textura: sobre todo cuarzo liso, con manchas de calcita y de diorita pulida."""
    r = _hash(x * 7 + y * 13, z * 3 + y, 11)
    return ("minecraft:smooth_quartz" if r < 0.5 else "minecraft:calcite" if r < 0.72
            else "minecraft:polished_diorite" if r < 0.9 else "minecraft:bone_block")


MACETAS = ["minecraft:potted_red_tulip", "minecraft:potted_poppy", "minecraft:potted_fern", "minecraft:potted_azure_bluet",
           "minecraft:potted_oxeye_daisy"]


def casa_romana(ancho, fondo, camas=1, puesto=None, portico=False):
    """Casa de estuco con azotea (tejado plano). Por fuera: zócalo de arenisca cortada, paredes de estuco con
    textura, columnas de cuarzo en las esquinas, ventanas con reja y dintel, una cornisa de ladrillo de cuarzo y un
    pretil con macetas en la azotea. La puerta, al norte (z = margen), con un paso empedrado hasta el jigsaw del
    borde. Si portico, dos columnas delante con un tejadillo de losas."""
    m = 2
    w, d = ancho, fondo + m
    alto = 8
    p = Plantilla(w, alto, d)
    z0, z1 = m, m + fondo - 1
    px = w // 2
    for x in range(w):
        for z in range(z0, z1 + 1):
            borde = x in (0, w - 1) or z in (z0, z1)
            esquina = x in (0, w - 1) and z in (z0, z1)
            if borde:
                p.poner(x, 0, z, "minecraft:stone_bricks")
                for y in range(1, 5):
                    lado = x if z in (z0, z1) else z - z0
                    pilastra = not esquina and lado % 4 == 0 and abs(x - px) > 1
                    if esquina or (pilastra and 1 < y < 4):
                        p.poner(x, y, z, "minecraft:quartz_pillar")
                    elif y == 1:
                        p.poner(x, y, z, "minecraft:cut_sandstone" if (x + z) % 4 else "minecraft:chiseled_sandstone")
                    elif y == 4:
                        p.poner(x, y, z, "minecraft:quartz_bricks")               # cornisa
                    else:
                        p.poner(x, y, z, estuco(x, y, z))
            else:
                mosaico = (x + z) % 2 == 0 and 2 <= x <= w - 3 and z0 + 2 <= z <= z1 - 2
                p.poner(x, 0, z, "minecraft:polished_granite" if mosaico else "minecraft:polished_andesite")
                for y in range(1, 5):
                    p.poner(x, y, z, "minecraft:air")
            # Azotea: losa lisa, con pretil de losas de cuarzo en el borde y alguna maceta
            p.poner(x, 5, z, "minecraft:smooth_stone" if not borde else "minecraft:quartz_bricks")
            p.poner(x, 6, z, "minecraft:air")
            if borde:
                maceta = not esquina and _hash(x, z, 21) < 0.12
                p.poner(x, 6, z, MACETAS[int(_hash(z, x, 4) * len(MACETAS)) % len(MACETAS)] if maceta
                        else "minecraft:quartz_slab", None if maceta else {"type": "bottom"})
    # Ventanas con reja y dintel de cuarzo, en los lados y al fondo
    def ventana(x, z):
        for y in (2, 3):
            p.poner(x, y, z, "minecraft:iron_bars")
    for z in range(z0 + 2, z1 - 1, 3):
        for x in (0, w - 1):
            ventana(x, z)
    for x in range(2, w - 2, 3):
        if x != px:
            ventana(x, z1)
    for x in range(2, w - 2, 4):
        if abs(x - px) > 1:
            ventana(x, z0)
    # Luz en la azotea (que no salgan monstruos) y una lucerna dentro
    p.poner(px, 6, (z0 + z1) // 2, "minecraft:lantern", {"hanging": "false"})
    # Puerta (hueco de 1 x 2) y paso empedrado hasta la calle
    for y in (1, 2):
        p.poner(px, y, z0, "minecraft:air")
    for z in range(0, z0):
        p.poner(px, 0, z, empedrado(px, z))
        for y in (1, 2, 3):
            p.poner(px, y, z, "minecraft:air")
    if portico:
        for x in (px - 2, px + 2):
            p.poner(x, 0, z0 - 1, "minecraft:stone_bricks")
            for y in range(1, 5):
                p.poner(x, y, z0 - 1, "minecraft:quartz_pillar")
        for x in range(px - 2, px + 3):
            p.poner(x, 5, z0 - 1, "minecraft:smooth_stone_slab", {"type": "bottom"})
    jigsaw(p, px, 0, 0, "north_up", "minecraft:building_entrance", "minecraft:empty", "minecraft:empty", empedrado(px, 0))
    p.poner(px, 1, 0, "minecraft:air")

    # Dentro: puesto de trabajo al fondo, camas en los lados, mesa con farol y un farol colgado
    libres = [(x, z) for x in range(1, w - 1) for z in range(z0 + 1, z1) if not (x == px and z == z0 + 1)]
    usadas = set()
    if puesto:
        p.poner(px, 1, z1 - 1, PUESTOS[puesto])
        usadas.add((px, z1 - 1))
    puestas = 0
    for z in range(z1 - 1, z0 + 1, -2):
        for x, mira in ((1, "west"), (w - 2, "east")):
            if puestas >= camas or (x, z) in usadas or (x, z - 1) in usadas:
                continue
            # cabecera contra la pared de fondo de la fila: cama a lo largo de z (cabeza en z, pies en z-1)
            p.poner(x, 1, z, "minecraft:red_bed", {"part": "head", "facing": "south"})
            p.poner(x, 1, z - 1, "minecraft:red_bed", {"part": "foot", "facing": "south"})
            usadas.update({(x, z), (x, z - 1)})
            puestas += 1
    mesa = (px, z0 + 2 if fondo >= 7 else z1 - 1)
    if mesa not in usadas and fondo >= 7:
        p.poner(mesa[0], 1, mesa[1], "minecraft:oak_fence")
        p.poner(mesa[0], 2, mesa[1], "minecraft:lantern", {"hanging": "false"})
        usadas.add(mesa)
        for dx, mira in ((-1, "east"), (1, "west")):
            if (mesa[0] + dx, mesa[1]) not in usadas:
                p.poner(mesa[0] + dx, 1, mesa[1], "minecraft:oak_stairs", {"facing": mira, "half": "bottom"})
                usadas.add((mesa[0] + dx, mesa[1]))
    else:
        p.poner(px, 4, (z0 + z1) // 2, "minecraft:lantern", {"hanging": "true"})
    amueblar(p, libres + [(px, z0 + 1)], 1, (px, z0 + 1), usadas, "antigua", semilla=ancho * 7 + fondo)
    for c in sorted(libres, key=lambda c: abs(c[0] - px) + abs(c[1] - (z0 + z1) // 2)):
        if c not in usadas:
            jigsaw(p, c[0], 1, c[1], "up_north", "minecraft:bottom", "minecraft:bottom", VILLAGERS)
            break
    return p


def _piscina(raiz, nombre, elementos, fallback="minecraft:empty"):
    ruta = os.path.join(raiz, "data", "yonga", "worldgen", "template_pool", *nombre.split(":")[1].split("/")) + ".json"
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump({"fallback": fallback, "elements": elementos}, f, indent=2)


def _elemento(ubicacion, peso, proyeccion="rigid", procesadores="minecraft:empty"):
    return {"weight": peso, "element": {"element_type": "minecraft:single_pool_element", "location": ubicacion,
                                         "processors": procesadores, "projection": proyeccion}}


def procesadores_create(raiz, epoca):
    """Lista de procesadores que cambia algunos bloques por los de Create si está instalado (ver ConCreateProcessor)."""
    escribir_json(raiz, ["data", "yonga", "worldgen", "processor_list", "con_create_" + epoca + ".json"],
                  {"processors": [{"processor_type": "yonga:con_create", "epoca": epoca}]})
    return "yonga:con_create_" + epoca


def antigua(raiz):
    base = os.path.join(raiz, "data", "yonga", "structures", "aldea", "antigua")
    pref = "yonga:aldea/antigua/"
    foro().guardar(os.path.join(base, "foro.nbt"))
    calles_de(raiz, base, pref, empedrado)
    casas = []

    def casa(nombre, plantilla, peso):
        plantilla.guardar(os.path.join(base, nombre + ".nbt"))
        casas.append(_elemento(pref + nombre, peso))

    casa("casa_pequena", casa_romana(7, 7, camas=1), 4)
    casa("casa", casa_romana(9, 8, camas=2), 4)
    casa("insula", casa_romana(11, 9, camas=4), 2)
    casa("domus", casa_romana(13, 11, camas=4, portico=True), 1)
    for puesto in PUESTOS:
        casa("taller_" + puesto, casa_romana(9, 8, camas=1, puesto=puesto), 2)

    _piscina(raiz, pref + "centros", [_elemento(pref + "foro", 1)])
    _piscina(raiz, pref + "casas", casas)
    estructura(raiz, "aldea_antigua", pref + "centros")
    return len(casas)


# ================================================================= EDAD MEDIA
# Estructura "yonga:aldea_medieval": una PLAZA con pozo, la campana colgada de un pórtico de madera, puestos de
# mercado y faroles; CALLES de tierra, grava y algún adoquín; CASAS de entramado (zócalo de piedra, paredes encaladas
# con vigas de abeto, ventanas de cristal, puerta de madera y tejado a dos aguas de abeto con aleros); algunas con
# chimenea humeante.

def tierra(x, z):
    r = _hash(x, z, 9)
    return ("minecraft:dirt_path" if r < 0.62 else "minecraft:gravel" if r < 0.77
            else "minecraft:coarse_dirt" if r < 0.88 else "minecraft:cobblestone")


def puerta(p, x, y, z, madera="spruce", mira="north"):
    for mitad, dy in (("lower", 0), ("upper", 1)):
        p.poner(x, y + dy, z, f"minecraft:{madera}_door",
                {"facing": mira, "half": mitad, "hinge": "left", "open": "false", "powered": "false"})


def tejado_dos_aguas(p, w, z0, z1, y0, teja, losa, hastial, cumbrera=None, remate=None):
    """Tejado a dos aguas con la cumbrera a lo ancho (x), aleros de un bloque delante (z0 - 1) y detrás (z1 + 1).
    En la primera capa (la de los aleros) se cierra todo el contorno de la casa con remate (o con el hastial), para
    que no quede un hueco entre la pared y el tejado por el que se vea el interior."""
    for x in range(w):
        for z in range(z0, z1 + 1):
            if x in (0, w - 1) or z in (z0, z1):
                if remate:
                    p.poner(x, y0, z, remate[0], remate[1](x, z) if callable(remate[1]) else remate[1])
                else:
                    p.poner(x, y0, z, hastial(x, y0, z))
            elif p.leer(x, y0, z) is None:
                p.poner(x, y0, z, "minecraft:air")
    k = 0
    while True:
        zn, zs, y = z0 - 1 + k, z1 + 1 - k, y0 + k
        if zn > zs:
            break
        for x in range(w):
            if zn == zs:
                p.poner(x, y, zn, cumbrera or losa, None if cumbrera else {"type": "bottom"})
            else:
                p.poner(x, y, zn, teja, {"facing": "south", "half": "bottom"})
                p.poner(x, y, zs, teja, {"facing": "north", "half": "bottom"})
        if k >= 1:
            for z in range(zn + 1, zs):
                for x in (0, w - 1):
                    p.poner(x, y, z, hastial(x, y, z))
                for x in range(1, w - 1):
                    if p.leer(x, y, z) is None:
                        p.poner(x, y, z, "minecraft:air")
        k += 1
    return y0 + k


CAMAS_MEDIEVALES = ["minecraft:red_bed", "minecraft:white_bed", "minecraft:blue_bed", "minecraft:yellow_bed"]


def casa_medieval(ancho, fondo, camas=1, puesto=None, chimenea=False, semilla=0):
    m = 2
    w, d = ancho, fondo + m + 1
    alto = 6 + (fondo + 1) // 2 + 4
    p = Plantilla(w, alto, d)
    z0, z1 = m, m + fondo - 1
    px = w // 2
    cal = "minecraft:white_terracotta"
    for x in range(w):
        for z in range(z0, z1 + 1):
            borde = x in (0, w - 1) or z in (z0, z1)
            esquina = x in (0, w - 1) and z in (z0, z1)
            if borde:
                p.poner(x, 0, z, "minecraft:cobblestone")
                lado = x if z in (z0, z1) else z - z0
                poste = esquina or (lado % 3 == 0 and abs(x - px) > 1)
                for y in range(1, 5):
                    if y == 1:
                        b = "minecraft:mossy_cobblestone" if _hash(x, z, semilla) < 0.25 else "minecraft:cobblestone"
                        p.poner(x, y, z, b)
                    elif poste:
                        p.poner(x, y, z, "minecraft:spruce_log")
                    elif y == 4:
                        p.poner(x, y, z, "minecraft:stripped_spruce_log", {"axis": "x" if z in (z0, z1) else "z"})
                    else:
                        p.poner(x, y, z, cal)
            else:
                p.poner(x, 0, z, "minecraft:spruce_planks" if (x + z) % 5 else "minecraft:oak_planks")
                for y in range(1, 5):
                    p.poner(x, y, z, "minecraft:air")
                p.poner(x, 5, z, "minecraft:spruce_planks")              # techo
    # Ventanas de cristal entre los postes
    for z in range(z0 + 1, z1):
        if (z - z0) % 3 == 2:
            for x in (0, w - 1):
                for y in (2, 3):
                    p.poner(x, y, z, "minecraft:glass_pane")
    for x in range(1, w - 1):
        if x % 3 == 2 and abs(x - px) > 1:
            for y in (2, 3):
                p.poner(x, y, z1, "minecraft:glass_pane")
                p.poner(x, y, z0, "minecraft:glass_pane")

    def hastial(x, y, z):
        return "minecraft:spruce_log" if z == (z0 + z1) // 2 else cal
    tejado_dos_aguas(p, w, z0, z1, 5, "minecraft:spruce_stairs", "minecraft:spruce_slab", hastial,
                     remate=("minecraft:stripped_spruce_log", lambda x, z: {"axis": "x" if z in (z0, z1) else "z"}))
    # Puerta de madera, con farol al lado, y el camino a la calle
    for y in (1, 2):
        p.poner(px, y, z0, "minecraft:air")
    puerta(p, px, 1, z0)
    p.poner(px, 3, z0, "minecraft:stripped_spruce_log", {"axis": "x"})
    for z in range(0, z0):
        p.poner(px, 0, z, tierra(px, z))
        for y in (1, 2, 3):
            p.poner(px, y, z, "minecraft:air")
    jigsaw(p, px, 0, 0, "north_up", "minecraft:building_entrance", "minecraft:empty", "minecraft:empty", tierra(px, 0))
    p.poner(px, 1, 0, "minecraft:air")
    # Chimenea de piedra en una esquina del fondo, con humo
    if chimenea:
        cx, cz = 1, z1 - 1
        for y in range(1, alto - 1):
            p.poner(cx, y, cz, "minecraft:cobblestone")
        p.poner(cx, alto - 1, cz, "minecraft:campfire", {"lit": "true", "signal_fire": "false"})
    # Dentro: puesto al fondo, camas a los lados, mesa con sillas y un farol colgado
    usadas = set()
    if chimenea:
        usadas.add((1, z1 - 1))
    if puesto:
        p.poner(px, 1, z1 - 1, PUESTOS[puesto])
        usadas.add((px, z1 - 1))
    puestas = 0
    color = CAMAS_MEDIEVALES[semilla % len(CAMAS_MEDIEVALES)]
    for z in range(z1 - 1, z0 + 1, -2):
        for x in (w - 2, 1):
            if puestas >= camas or (x, z) in usadas or (x, z - 1) in usadas:
                continue
            p.poner(x, 1, z, color, {"part": "head", "facing": "south"})
            p.poner(x, 1, z - 1, color, {"part": "foot", "facing": "south"})
            usadas.update({(x, z), (x, z - 1)})
            puestas += 1
    if fondo >= 7 and w >= 7:
        mesa = (px, z0 + 2)
        p.poner(mesa[0], 1, mesa[1], "minecraft:oak_fence")
        p.poner(mesa[0], 2, mesa[1], "minecraft:oak_pressure_plate")
        usadas.add(mesa)
        for dx, mira in ((-1, "east"), (1, "west")):
            if (mesa[0] + dx, mesa[1]) not in usadas:
                p.poner(mesa[0] + dx, 1, mesa[1], "minecraft:oak_stairs", {"facing": mira, "half": "bottom"})
                usadas.add((mesa[0] + dx, mesa[1]))
    p.poner(px, 4, (z0 + z1) // 2 + 1, "minecraft:lantern", {"hanging": "true"})
    libres = [(x, z) for x in range(1, w - 1) for z in range(z0 + 1, z1) if not (x == px and z == z0 + 1)]
    amueblar(p, libres + [(px, z0 + 1)], 1, (px, z0 + 1), usadas, "medieval", semilla=semilla + ancho)
    for c in sorted(libres, key=lambda c: abs(c[0] - px) + abs(c[1] - (z0 + z1) // 2)):
        if c not in usadas:
            jigsaw(p, c[0], 1, c[1], "up_north", "minecraft:bottom", "minecraft:bottom", VILLAGERS)
            break
    return p


def plaza_medieval():
    """Plaza de 15 x 15: suelo de adoquín, piedra y grava; pozo con agua en medio (con su tejadillo), la campana
    colgada de un pórtico de madera, dos puestos de mercado con toldo y faroles en las esquinas."""
    n = 15
    p = Plantilla(n, 7, n)
    c = n // 2
    for x in range(n):
        for z in range(n):
            r = _hash(x, z, 31)
            p.poner(x, 0, z, "minecraft:cobblestone" if r < 0.45 else "minecraft:stone_bricks" if r < 0.7
                    else "minecraft:gravel" if r < 0.85 else "minecraft:andesite")
            for y in range(1, 7):
                p.poner(x, y, z, "minecraft:air")
    # Pozo: aro de piedra, agua dentro (contenida por la piedra) y tejadillo sobre postes
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if dx == 0 and dz == 0:
                p.poner(c, 0, c, "minecraft:water", {"level": "0"})
                p.poner(c, 1, c, "minecraft:air")
            else:
                p.poner(c + dx, 1, c + dz, "minecraft:stone_brick_wall" if dx and dz else "minecraft:stone_bricks")
    for dx, dz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        p.poner(c + dx, 2, c + dz, "minecraft:spruce_fence")
        p.poner(c + dx, 3, c + dz, "minecraft:spruce_fence")
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            p.poner(c + dx, 4, c + dz, "minecraft:spruce_slab", {"type": "bottom"})
    # La campana, colgada de un pórtico de madera
    bz = c - 4
    for dx in (-1, 1):
        for y in range(1, 4):
            p.poner(c + dx, y, bz, "minecraft:spruce_log")
    p.poner(c - 1, 4, bz, "minecraft:stripped_spruce_log", {"axis": "x"})
    p.poner(c, 4, bz, "minecraft:stripped_spruce_log", {"axis": "x"})
    p.poner(c + 1, 4, bz, "minecraft:stripped_spruce_log", {"axis": "x"})
    p.poner(c, 3, bz, "minecraft:bell", {"attachment": "ceiling", "facing": "east"})
    # Puestos de mercado con toldo de lana
    for sx, lana in ((c - 5, "minecraft:red_wool"), (c + 4, "minecraft:white_wool")):
        sz = c + 3
        for dx in (0, 1):
            p.poner(sx + dx, 1, sz, "minecraft:barrel", {"facing": "up"})
            p.poner(sx + dx, 1, sz + 1, "minecraft:oak_fence")
            p.poner(sx + dx, 2, sz + 1, "minecraft:oak_fence")
            for dz in (-1, 0, 1):
                p.poner(sx + dx, 3, sz + dz, lana)
    # Faroles en postes en las esquinas
    for x, z in ((1, 1), (n - 2, 1), (1, n - 2), (n - 2, n - 2)):
        p.poner(x, 1, z, "minecraft:spruce_fence")
        p.poner(x, 2, z, "minecraft:spruce_fence")
        p.poner(x, 3, z, "minecraft:lantern", {"hanging": "false"})
    jigsaw(p, c, 0, 0, "north_up", "yonga:calle", "yonga:calle", "yonga:aldea/medieval/calles", "minecraft:cobblestone")
    jigsaw(p, c, 0, n - 1, "south_up", "yonga:calle", "yonga:calle", "yonga:aldea/medieval/calles", "minecraft:cobblestone")
    jigsaw(p, 0, 0, c, "west_up", "yonga:calle", "yonga:calle", "yonga:aldea/medieval/calles", "minecraft:cobblestone")
    jigsaw(p, n - 1, 0, c, "east_up", "yonga:calle", "yonga:calle", "yonga:aldea/medieval/calles", "minecraft:cobblestone")
    jigsaw(p, c - 3, 1, c + 1, "up_north", "minecraft:bottom", "minecraft:bottom", VILLAGERS)
    jigsaw(p, c + 3, 1, c - 2, "up_north", "minecraft:bottom", "minecraft:bottom", VILLAGERS)
    return p


def medieval(raiz):
    base = os.path.join(raiz, "data", "yonga", "structures", "aldea", "medieval")
    pref = "yonga:aldea/medieval/"
    plaza_medieval().guardar(os.path.join(base, "plaza.nbt"))
    calles_de(raiz, base, pref, tierra)
    casas = []

    def casa(nombre, plantilla, peso):
        plantilla.guardar(os.path.join(base, nombre + ".nbt"))
        casas.append(_elemento(pref + nombre, peso))

    casa("casa_pequena", casa_medieval(7, 7, camas=1, semilla=1), 4)
    casa("casa", casa_medieval(9, 8, camas=2, chimenea=True, semilla=2), 4)
    casa("casa_larga", casa_medieval(11, 9, camas=3, chimenea=True, semilla=3), 2)
    casa("casa_grande", casa_medieval(13, 11, camas=4, chimenea=True, semilla=0), 1)
    for i, puesto in enumerate(PUESTOS):
        casa("taller_" + puesto, casa_medieval(9, 8, camas=1, puesto=puesto, chimenea=puesto == "smithing", semilla=i), 2)
    _piscina(raiz, pref + "centros", [_elemento(pref + "plaza", 1)])
    _piscina(raiz, pref + "casas", casas)
    estructura(raiz, "aldea_medieval", pref + "centros")
    return len(casas)


# ================================================================= SIGLO XVII
# Estructura "yonga:aldea_moderna": una PLAZA MAYOR con soportales, un templete con la campana y una fuente; CALLES
# de adoquín; CASAS encaladas de dos plantas: zócalo y esquinas de piedra, rejas en las ventanas de abajo, balcón de
# madera arriba, puerta de roble oscuro y tejado de teja a dos aguas.

def adoquin(x, z):
    r = _hash(x, z, 17)
    return ("minecraft:cobblestone" if r < 0.5 else "minecraft:stone" if r < 0.72
            else "minecraft:andesite" if r < 0.88 else "minecraft:gravel")


def encalado(x, y, z):
    return "minecraft:white_concrete" if _hash(x * 5 + y, z * 7 + y, 19) < 0.8 else "minecraft:calcite"


ESTILO_XVII = {
    "muro": lambda x, y, z: encalado(x, y, z),
    "esquina": lambda x, y, z: "minecraft:stone_bricks" if y % 2 else "minecraft:polished_andesite",
    "zocalo": "minecraft:polished_andesite", "cimiento": "minecraft:stone_bricks",
    "viga": "minecraft:stripped_dark_oak_log", "tablon": "minecraft:dark_oak_planks",
    "suelo": lambda x, z: "minecraft:polished_andesite" if (x + z) % 2 else "minecraft:stone_bricks",
    "teja": "minecraft:brick_stairs", "losa": "minecraft:brick_slab", "cumbrera": "minecraft:bricks",
    "madera": "dark_oak", "dintel": "minecraft:stone_bricks", "ventana_baja": "minecraft:iron_bars",
    "ventana_alta": "minecraft:glass_pane", "camino": lambda x, z: adoquin(x, z), "farola": None,
    "camas": ["minecraft:red_bed", "minecraft:green_bed", "minecraft:blue_bed"], "chimenea": False,
    "epoca": "moderna",
}


def casa_moderna(ancho, fondo, camas=2, puesto=None, dos_plantas=True, semilla=0, est=None):
    """Casa de una o dos plantas con un estilo (ESTILO_XVII, ESTILO_INDUSTRIAL…)."""
    est = est or ESTILO_XVII
    m = 2
    w, d = ancho, fondo + m + 1
    planta = 5                                        # altura de cada planta (suelo + 4)
    techo = planta * (2 if dos_plantas else 1)        # y del último forjado
    alto = techo + (fondo + 1) // 2 + 4
    p = Plantilla(w, alto, d)
    z0, z1 = m, m + fondo - 1
    px = w // 2
    for x in range(w):
        for z in range(z0, z1 + 1):
            borde = x in (0, w - 1) or z in (z0, z1)
            esquina = x in (0, w - 1) and z in (z0, z1)
            if borde:
                p.poner(x, 0, z, est["cimiento"])
                for y in range(1, techo + 1):
                    if y % planta == 0:
                        p.poner(x, y, z, est["viga"], {"axis": "x" if z in (z0, z1) else "z"})
                    elif esquina:
                        p.poner(x, y, z, est["esquina"](x, y, z))
                    elif y == 1:
                        p.poner(x, y, z, est["zocalo"])
                    else:
                        p.poner(x, y, z, est["muro"](x, y, z))
            else:
                p.poner(x, 0, z, est["suelo"](x, z))
                for y in range(1, techo):
                    p.poner(x, y, z, "minecraft:air" if y % planta else est["tablon"])
                p.poner(x, techo, z, est["tablon"])
    # Ventanas: abajo con reja, arriba con cristal
    for piso in range(2 if dos_plantas else 1):
        y0 = piso * planta + 2
        mat = est["ventana_baja"] if piso == 0 else est["ventana_alta"]
        for z in range(z0 + 2, z1 - 1, 3):
            for x in (0, w - 1):
                for y in (y0, y0 + 1):
                    p.poner(x, y, z, mat)
        for x in range(2, w - 2, 3):
            if abs(x - px) > 1 or piso > 0:
                for y in (y0, y0 + 1):
                    if not (piso > 0 and x == px):
                        p.poner(x, y, z0, mat)
                    p.poner(x, y, z1, mat)
    # Tejado de teja a dos aguas sobre una viga de remate
    for x in range(w):
        for z in (z0, z1):
            p.poner(x, techo, z, est["viga"], {"axis": "x"})

    def hastial(x, y, z):
        return est["muro"](x, y, z)
    if est.get("azotea"):
        # Azotea: cubierta lisa, pretil alrededor, placas solares, una antena y la máquina del aire acondicionado
        for x in range(w):
            for z in range(z0, z1 + 1):
                borde = x in (0, w - 1) or z in (z0, z1)
                p.poner(x, techo, z, est["cubierta"] if not borde else est["muro"](x, techo, z))
                p.poner(x, techo + 1, z, est["pretil"] if borde else "minecraft:air")
                p.poner(x, techo + 2, z, "minecraft:air")
        for x in range(2, w - 2, 2):
            for z in range(z0 + 2, min(z1 - 1, z0 + 5)):
                p.poner(x, techo + 1, z, "minecraft:daylight_detector")
        p.poner(w - 2, techo + 1, z1 - 1, "minecraft:lightning_rod")
        p.poner(1, techo + 1, z1 - 1, "minecraft:iron_block" if semilla % 2 else "minecraft:smooth_stone")
    else:
        tejado_dos_aguas(p, w, z0, z1, techo + 1, est["teja"], est["losa"], hastial, est["cumbrera"])
    # Puerta de roble oscuro con dintel de piedra, y el camino
    for y in (1, 2):
        p.poner(px, y, z0, "minecraft:air")
    puerta(p, px, 1, z0, est["madera"])
    p.poner(px, 3, z0, est["dintel"])
    for z in range(0, z0):
        p.poner(px, 0, z, est["camino"](px, z))
        for y in (1, 2, 3):
            p.poner(px, y, z, "minecraft:air")
    jigsaw(p, px, 0, 0, "north_up", "minecraft:building_entrance", "minecraft:empty", "minecraft:empty",
           est["camino"](px, 0))
    p.poner(px, 1, 0, "minecraft:air")
    # Farola de hierro fundido junto a la entrada
    if est["farola"]:
        fx = px + 2 if px + 2 < w else px - 2
        p.poner(fx, 0, 0, est["camino"](fx, 0))
        for y in (1, 2):
            p.poner(fx, y, 0, est["farola"])
        if est.get("luz") == "barra":
            p.poner(fx, 3, 0, est["farola"])
            p.poner(fx, 4, 0, "minecraft:end_rod", {"facing": "up"})
        else:
            p.poner(fx, 3, 0, "minecraft:lantern", {"hanging": "false"})
    # Chimenea de ladrillo con humo, en la esquina de atrás a la izquierda
    if est["chimenea"]:
        for y in range(1, alto - 1):
            p.poner(1, y, z1 - 1, "minecraft:bricks")
        p.poner(1, alto - 1, z1 - 1, "minecraft:campfire", {"lit": "true", "signal_fire": "false"})
    # Arriba, sobre la puerta, una ventana de cristal
    if dos_plantas:
        for y in (planta + 2, planta + 3):
            p.poner(px, y, z0, "minecraft:glass_pane")
        # Escalera de madera (los habitantes no saben subir por escaleras de mano): a lo largo de la pared derecha,
        # subiendo hacia la puerta, con hueco en el forjado para la cabeza
        ex = w - 2
        for k in range(1, planta + 1):
            z = z1 - k
            p.poner(ex, k, z, f"minecraft:{est['madera']}_stairs", {"facing": "north", "half": "bottom"})
            for y in range(1, k):
                p.poner(ex, y, z, est["tablon"])                                 # macizo debajo
            # Hueco de TRES por encima de cada escalón: al pasar de uno al siguiente, un habitante alto (casi dos
            # bloques) aún tiene la cabeza en la casilla de atrás, y con dos se daba contra el suelo de arriba
            for y in range(k + 1, min(k + 4, planta + 1)):
                p.poner(ex, y, z, "minecraft:air")
    # Dentro
    usadas = set()
    if est["chimenea"]:
        usadas.add((1, z1 - 1))
    if dos_plantas:
        usadas.update({(w - 2, z1 - k) for k in range(1, planta + 1)})
    if puesto:
        p.poner(px, 1, z1 - 1, PUESTOS[puesto])
        usadas.add((px, z1 - 1))
    # Camas: arriba si hay dos plantas
    yc = planta + 1 if dos_plantas else 1
    usadas_arriba = ({(w - 2, z1 - k) for k in range(1, planta + 2)} | {(w - 3, z1 - planta), (1, z1 - 1)}
                     if dos_plantas else usadas)
    puestas = 0
    color = est["camas"][semilla % len(est["camas"])]
    for z in range(z1 - 1, z0 + 1, -2):
        for x in (w - 2, 1):
            if puestas >= camas or (x, z) in usadas_arriba or (x, z - 1) in usadas_arriba:
                continue
            p.poner(x, yc, z, color, {"part": "head", "facing": "south"})
            p.poner(x, yc, z - 1, color, {"part": "foot", "facing": "south"})
            usadas_arriba.update({(x, z), (x, z - 1)})
            puestas += 1
    # Abajo: mesa con sillas y farol colgado; arriba otro farol
    if fondo >= 7 and w >= 7:
        mesa = (px, (z0 + z1) // 2)
        if mesa not in usadas:
            p.poner(mesa[0], 1, mesa[1], f"minecraft:{est['madera']}_fence")
            p.poner(mesa[0], 2, mesa[1], f"minecraft:{est['madera']}_pressure_plate")
            usadas.add(mesa)
            for dx, mira in ((-1, "east"), (1, "west")):
                p.poner(mesa[0] + dx, 1, mesa[1], f"minecraft:{est['madera']}_stairs", {"facing": mira, "half": "bottom"})
                usadas.add((mesa[0] + dx, mesa[1]))
    p.poner(px, planta - 1, z0 + 2, "minecraft:lantern", {"hanging": "true"})
    if dos_plantas:
        p.poner(px, techo - 1, (z0 + z1) // 2, "minecraft:lantern", {"hanging": "true"})
    libres = [(x, z) for x in range(1, w - 1) for z in range(z0 + 1, z1) if not (x == px and z == z0 + 1)]
    # Los muebles de la época: abajo el cuarto de estar y, si hay dos plantas, arriba los dormitorios
    epoca = est.get("epoca", "moderna")
    ex = w - 2
    amueblar(p, libres + [(px, z0 + 1)], 1, (px, z0 + 1), usadas, epoca, semilla=semilla + ancho,
             reservadas={(ex - 1, z1 - 1), (ex - 1, z1 - 2)} if dos_plantas else ())
    if dos_plantas:
        hueco = {(ex, z1 - k) for k in range(1, planta)}
        arriba = [(x, z) for x in range(1, w - 1) for z in range(z0 + 1, z1) if (x, z) not in hueco]
        amueblar(p, arriba, planta + 1, (ex, z1 - planta), set(usadas_arriba), epoca, piso="arriba",
                 semilla=semilla + ancho * 3, reservadas={(ex, z1 - planta - 1), (ex - 1, z1 - planta)})
    for c in sorted(libres, key=lambda c: abs(c[0] - px) + abs(c[1] - z1)):
        if c not in usadas:
            jigsaw(p, c[0], 1, c[1], "up_north", "minecraft:bottom", "minecraft:bottom", VILLAGERS)
            break
    return p


def plaza_mayor():
    """Plaza de 19 x 19: losas de piedra en damero, soportales (columnas de piedra con cubierta de teja) en dos lados,
    un templete en medio con la campana colgada, una fuente y bancos de madera."""
    n = 19
    p = Plantilla(n, 8, n)
    c = n // 2
    for x in range(n):
        for z in range(n):
            p.poner(x, 0, z, "minecraft:stone_bricks" if (x + z) % 2 else "minecraft:polished_andesite")
            for y in range(1, 8):
                p.poner(x, y, z, "minecraft:air")
    # Soportales al oeste y al este: columnas cada 3 y cubierta de losas de ladrillo
    for x0 in (0, n - 3):
        for z in range(1, n - 1):
            for x in range(x0, x0 + 3):
                p.poner(x, 4, z, "minecraft:brick_slab", {"type": "bottom"})
            if z % 3 == 1:
                col = x0 + (2 if x0 == 0 else 0)
                for y in range(1, 4):
                    p.poner(col, y, z, "minecraft:stone_brick_wall")
        for z in range(1, n - 1):
            if z % 4 == 2:
                p.poner(x0 + 1, 3, z, "minecraft:lantern", {"hanging": "true"})
    # Templete con la campana: cuatro columnas, cubierta de teja y la campana colgando en medio
    for dx, dz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        for y in range(1, 4):
            p.poner(c + dx, y, c + dz, "minecraft:stone_brick_wall")
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            p.poner(c + dx, 4, c + dz, "minecraft:bricks" if dx == 0 and dz == 0 else "minecraft:brick_slab",
                    None if dx == 0 and dz == 0 else {"type": "bottom"})
    p.poner(c, 3, c, "minecraft:bell", {"attachment": "ceiling", "facing": "north"})
    p.poner(c, 0, c, "minecraft:chiseled_stone_bricks")
    # Fuente: pilón de piedra con agua y un caño en medio
    fx, fz = c, c + 5
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if dx == 0 and dz == 0:
                p.poner(fx, 0, fz, "minecraft:water", {"level": "0"})
                p.poner(fx, 1, fz, "minecraft:air")
            else:
                p.poner(fx + dx, 0, fz + dz, "minecraft:water", {"level": "0"})
                p.poner(fx + dx, 1, fz + dz, "minecraft:air")
    for dx in (-2, -1, 0, 1, 2):
        for dz in (-2, -1, 0, 1, 2):
            if max(abs(dx), abs(dz)) == 2:
                p.poner(fx + dx, 0, fz + dz, "minecraft:stone_bricks")
                p.poner(fx + dx, 1, fz + dz, "minecraft:stone_brick_slab", {"type": "bottom"})
    p.poner(fx, 0, fz, "minecraft:stone_bricks")          # el caño, en medio del pilón
    p.poner(fx, 1, fz, "minecraft:stone_brick_wall")
    p.poner(fx, 2, fz, "minecraft:stone_brick_wall")
    # Bancos de madera mirando al templete
    for dz, mira in ((-4, "north"), (4, "south")):
        for dx in (-2, -1, 1, 2):
            if dz == 4:
                continue
            p.poner(c + dx, 1, c + dz, "minecraft:dark_oak_stairs", {"facing": mira, "half": "bottom"})
    for dx, mira in ((-4, "west"), (4, "east")):
        for dz in (-1, 1):
            p.poner(c + dx, 1, c + dz, "minecraft:dark_oak_stairs", {"facing": mira, "half": "bottom"})
    # Macetas en las esquinas
    for x, z in ((4, 1), (n - 5, 1), (4, n - 2), (n - 5, n - 2)):
        p.poner(x, 1, z, "minecraft:potted_azalea_bush")
    pre = "yonga:aldea/moderna/calles"
    jigsaw(p, c, 0, 0, "north_up", "yonga:calle", "yonga:calle", pre, "minecraft:stone_bricks")
    jigsaw(p, c, 0, n - 1, "south_up", "yonga:calle", "yonga:calle", pre, "minecraft:stone_bricks")
    jigsaw(p, 0, 0, c, "west_up", "yonga:calle", "yonga:calle", pre, "minecraft:stone_bricks")
    jigsaw(p, n - 1, 0, c, "east_up", "yonga:calle", "yonga:calle", pre, "minecraft:stone_bricks")
    jigsaw(p, c - 4, 1, c - 2, "up_north", "minecraft:bottom", "minecraft:bottom", VILLAGERS)
    jigsaw(p, c + 4, 1, c + 2, "up_north", "minecraft:bottom", "minecraft:bottom", VILLAGERS)
    return p


def moderna(raiz):
    base = os.path.join(raiz, "data", "yonga", "structures", "aldea", "moderna")
    pref = "yonga:aldea/moderna/"
    plaza_mayor().guardar(os.path.join(base, "plaza.nbt"))
    calles_de(raiz, base, pref, adoquin)
    casas = []

    def casa(nombre, plantilla, peso):
        plantilla.guardar(os.path.join(base, nombre + ".nbt"))
        casas.append(_elemento(pref + nombre, peso))

    casa("casa_baja", casa_moderna(7, 7, camas=1, dos_plantas=False, semilla=1), 3)
    casa("casa", casa_moderna(9, 8, camas=2, semilla=2), 4)
    casa("casa_larga", casa_moderna(11, 9, camas=3, semilla=0), 3)
    casa("casona", casa_moderna(13, 11, camas=4, semilla=1), 1)
    for i, puesto in enumerate(PUESTOS):
        casa("taller_" + puesto, casa_moderna(9, 8, camas=1, puesto=puesto, dos_plantas=False, semilla=i), 2)
    _piscina(raiz, pref + "centros", [_elemento(pref + "plaza", 1)])
    _piscina(raiz, pref + "casas", casas)
    estructura(raiz, "aldea_moderna", pref + "centros")
    return len(casas)


# ================================================================= ÉPOCA INDUSTRIAL (hacia 1890)
# Estructura "yonga:aldea_industrial": una PLAZA con quiosco de música (la campana cuelga en él), farolas de hierro
# fundido y bancos; CALLES de adoquín y piedra; CASAS de ladrillo visto de una y dos plantas, con dinteles de piedra,
# tejado de pizarra (tejas de pizarra profunda), chimenea humeante y farola a la puerta; y alguna FÁBRICA con su
# chimenea alta.

def adoquin_industrial(x, z):
    r = _hash(x, z, 23)
    return ("minecraft:stone" if r < 0.4 else "minecraft:cobblestone" if r < 0.7
            else "minecraft:polished_andesite" if r < 0.9 else "minecraft:andesite")


def ladrillo(x, y, z):
    r = _hash(x * 3 + y * 11, z * 5 + y, 29)
    return "minecraft:bricks" if r < 0.9 else "minecraft:mud_bricks"


ESTILO_INDUSTRIAL = {
    "muro": ladrillo,
    "esquina": lambda x, y, z: "minecraft:stone_bricks" if y % 2 else "minecraft:bricks",
    "zocalo": "minecraft:stone_bricks", "cimiento": "minecraft:stone_bricks",
    "viga": "minecraft:stripped_spruce_log", "tablon": "minecraft:spruce_planks",
    "suelo": lambda x, z: "minecraft:oak_planks" if (x + z) % 3 else "minecraft:spruce_planks",
    "teja": "minecraft:deepslate_tile_stairs", "losa": "minecraft:deepslate_tile_slab",
    "cumbrera": "minecraft:deepslate_tiles", "madera": "spruce", "dintel": "minecraft:smooth_stone",
    "ventana_baja": "minecraft:glass_pane", "ventana_alta": "minecraft:glass_pane",
    "camino": lambda x, z: adoquin_industrial(x, z), "farola": "minecraft:polished_blackstone_wall",
    "camas": ["minecraft:white_bed", "minecraft:light_gray_bed", "minecraft:brown_bed"], "chimenea": True,
    "epoca": "industrial",
}


def fabrica(puesto="smithing"):
    """Nave de ladrillo de 15 x 11 con ventanales, tejado de pizarra, chimenea alta de ladrillo con humo y, dentro,
    el puesto de trabajo, un yunque, hornos y una cama para el encargado."""
    w, fondo, m = 15, 11, 2
    d = fondo + m + 1
    alto_muro = 6
    alto = alto_muro + (fondo + 1) // 2 + 14
    p = Plantilla(w, alto, d)
    z0, z1 = m, m + fondo - 1
    px = w // 2
    for x in range(w):
        for z in range(z0, z1 + 1):
            borde = x in (0, w - 1) or z in (z0, z1)
            if borde:
                p.poner(x, 0, z, "minecraft:stone_bricks")
                for y in range(1, alto_muro + 1):
                    columna = (x if z in (z0, z1) else z - z0) % 4 == 0
                    p.poner(x, y, z, "minecraft:stone_bricks" if y == 1 or columna or y == alto_muro else ladrillo(x, y, z))
            else:
                p.poner(x, 0, z, "minecraft:stone_bricks" if (x + z) % 2 else "minecraft:polished_andesite")
                for y in range(1, alto_muro + 1):
                    p.poner(x, y, z, "minecraft:air")
    # Ventanales altos entre las columnas
    for x in range(1, w - 1):
        if x % 4 and abs(x - px) > 1:
            for y in range(2, 5):
                p.poner(x, y, z0, "minecraft:glass_pane")
                p.poner(x, y, z1, "minecraft:glass_pane")
    for z in range(z0 + 1, z1):
        if (z - z0) % 4:
            for y in range(2, 5):
                p.poner(0, y, z, "minecraft:glass_pane")
                p.poner(w - 1, y, z, "minecraft:glass_pane")
    tejado_dos_aguas(p, w, z0, z1, alto_muro + 1, "minecraft:deepslate_tile_stairs", "minecraft:deepslate_tile_slab",
                     lambda x, y, z: ladrillo(x, y, z), "minecraft:deepslate_tiles")
    # Portón de dos hojas (px - 1 y px), con dintel de piedra encima, y el camino de dos de ancho
    for x in (px - 1, px):
        for y in (1, 2):
            p.poner(x, y, z0, "minecraft:air")
        p.poner(x, 3, z0, "minecraft:smooth_stone")
    for x, bisagra in ((px - 1, "left"), (px, "right")):
        for mitad, dy in (("lower", 0), ("upper", 1)):
            p.poner(x, 1 + dy, z0, "minecraft:spruce_door",
                    {"facing": "north", "half": mitad, "hinge": bisagra, "open": "false", "powered": "false"})
    for y in range(1, 5):                                    # que las ventanas no lleguen al portón
        for x in (px - 2, px + 1):
            p.poner(x, y, z0, "minecraft:stone_bricks")
    for z in range(0, z0):
        for x in (px - 1, px):
            p.poner(x, 0, z, adoquin_industrial(x, z))
            for y in (1, 2, 3):
                p.poner(x, y, z, "minecraft:air")
    jigsaw(p, px, 0, 0, "north_up", "minecraft:building_entrance", "minecraft:empty", "minecraft:empty",
           adoquin_industrial(px, 0))
    # Chimenea alta (2 x 2) en una esquina de atrás, que sale muy por encima del tejado
    for y in range(1, alto - 1):
        for cx in (w - 3, w - 2):
            for cz in (z1 - 2, z1 - 1):
                p.poner(cx, y, cz, "minecraft:bricks" if y % 6 else "minecraft:stone_bricks")
    p.poner(w - 3, alto - 1, z1 - 2, "minecraft:campfire", {"lit": "true", "signal_fire": "true"})
    p.poner(w - 2, alto - 1, z1 - 1, "minecraft:campfire", {"lit": "true", "signal_fire": "false"})
    # Dentro: el puesto, hornos, yunque, mesa de trabajo, cama del encargado y faroles colgados
    p.poner(px, 1, z1 - 1, PUESTOS[puesto])
    p.poner(px - 2, 1, z1 - 1, "minecraft:blast_furnace", {"facing": "north"})
    p.poner(px + 2, 1, z1 - 1, "minecraft:furnace", {"facing": "north"})
    p.poner(px - 4, 1, z1 - 1, "minecraft:anvil", {"facing": "east"})
    p.poner(2, 1, z0 + 3, "minecraft:crafting_table")
    p.poner(1, 1, z1 - 1, "minecraft:white_bed", {"part": "head", "facing": "south"})
    p.poner(1, 1, z1 - 2, "minecraft:white_bed", {"part": "foot", "facing": "south"})
    # Viga de lado a lado y faroles colgados de ella
    zm = (z0 + z1) // 2
    for x in range(1, w - 1):
        p.poner(x, alto_muro + 1, zm, "minecraft:stripped_spruce_log", {"axis": "x"})
    for x in (3, px, w - 4):
        p.poner(x, alto_muro, zm, "minecraft:lantern", {"hanging": "true"})
    # La transmisión: un eje a lo largo de la nave, cerca del techo, con dos cajas de engranajes en los extremos
    # (basalto pulido y cobre encerado; con Create instalado son su eje y sus carcasas de cobre)
    zt = z1 - 3
    for x in range(1, w - 3):
        caja = x in (1, w - 4)
        p.poner(x, alto_muro - 1, zt, "minecraft:waxed_cut_copper" if caja else "minecraft:polished_basalt",
                None if caja else {"axis": "x"})
    usadas = {(px, (z0 + z1) // 2)}
    interior = [(x, z) for x in range(1, w - 1) for z in range(z0 + 1, z1)]
    amueblar(p, interior, 1, (px, z0 + 1), usadas, "industrial", semilla=len(puesto), maximo=5,
             reservadas={(px - 1, z0 + 1), (px, z1 - 2), (px - 2, z1 - 2), (px + 2, z1 - 2), (px - 4, z1 - 2)})
    jigsaw(p, px, 1, (z0 + z1) // 2, "up_north", "minecraft:bottom", "minecraft:bottom", VILLAGERS)
    return p


def plaza_quiosco():
    """Plaza de 17 x 17 de adoquín con un quiosco de música en medio (tarima de piedra, barandilla de hierro,
    columnas finas y tejadillo de pizarra; la campana cuelga dentro), farolas de hierro fundido y bancos."""
    n = 17
    p = Plantilla(n, 9, n)
    c = n // 2
    for x in range(n):
        for z in range(n):
            borde = x in (0, n - 1) or z in (0, n - 1)
            p.poner(x, 0, z, "minecraft:stone_bricks" if borde else adoquin_industrial(x, z))
            for y in range(1, 9):
                p.poner(x, y, z, "minecraft:air")
    # Quiosco: tarima de 5 x 5, barandilla, columnas y tejado
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            p.poner(c + dx, 1, c + dz, "minecraft:polished_andesite")
            borde = max(abs(dx), abs(dz)) == 2
            if borde and not (dz == 2 and dx == 0):
                esquina = abs(dx) == 2 and abs(dz) == 2
                p.poner(c + dx, 2, c + dz, "minecraft:polished_blackstone_wall" if esquina else "minecraft:iron_bars")
                if esquina:
                    for y in (3, 4):
                        p.poner(c + dx, y, c + dz, "minecraft:polished_blackstone_wall")
    p.poner(c, 1, c + 3, "minecraft:polished_andesite_stairs", {"facing": "north", "half": "bottom"})
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            if max(abs(dx), abs(dz)) == 3:
                p.poner(c + dx, 5, c + dz, "minecraft:deepslate_tile_slab", {"type": "bottom"})
            elif max(abs(dx), abs(dz)) <= 2:
                p.poner(c + dx, 5, c + dz, "minecraft:deepslate_tiles" if max(abs(dx), abs(dz)) == 2 else
                        "minecraft:smooth_stone")
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            p.poner(c + dx, 6, c + dz, "minecraft:deepslate_tile_slab", {"type": "bottom"})
    p.poner(c, 6, c, "minecraft:deepslate_tiles")
    p.poner(c, 7, c, "minecraft:lightning_rod")
    p.poner(c, 4, c, "minecraft:bell", {"attachment": "ceiling", "facing": "north"})
    # Farolas de hierro fundido
    for x, z in ((2, 2), (n - 3, 2), (2, n - 3), (n - 3, n - 3), (c - 5, c), (c + 5, c)):
        for y in (1, 2, 3):
            p.poner(x, y, z, "minecraft:polished_blackstone_wall")
        p.poner(x, 4, z, "minecraft:lantern", {"hanging": "false"})
    # Bancos de madera alrededor
    for dz, mira in ((-5, "north"), (5, "south")):
        for dx in (-2, -1, 1, 2):
            p.poner(c + dx, 1, c + dz, "minecraft:spruce_stairs", {"facing": mira, "half": "bottom"})
    pre = "yonga:aldea/industrial/calles"
    jigsaw(p, c, 0, 0, "north_up", "yonga:calle", "yonga:calle", pre, "minecraft:stone_bricks")
    jigsaw(p, c, 0, n - 1, "south_up", "yonga:calle", "yonga:calle", pre, "minecraft:stone_bricks")
    jigsaw(p, 0, 0, c, "west_up", "yonga:calle", "yonga:calle", pre, "minecraft:stone_bricks")
    jigsaw(p, n - 1, 0, c, "east_up", "yonga:calle", "yonga:calle", pre, "minecraft:stone_bricks")
    jigsaw(p, c - 4, 1, c - 3, "up_north", "minecraft:bottom", "minecraft:bottom", VILLAGERS)
    jigsaw(p, c + 4, 1, c + 3, "up_north", "minecraft:bottom", "minecraft:bottom", VILLAGERS)
    return p


def industrial(raiz):
    base = os.path.join(raiz, "data", "yonga", "structures", "aldea", "industrial")
    pref = "yonga:aldea/industrial/"
    plaza_quiosco().guardar(os.path.join(base, "plaza.nbt"))
    calles_de(raiz, base, pref, adoquin_industrial)
    casas = []
    create = procesadores_create(raiz, "industrial")

    def casa(nombre, plantilla, peso):
        plantilla.guardar(os.path.join(base, nombre + ".nbt"))
        casas.append(_elemento(pref + nombre, peso, procesadores=create))

    e = ESTILO_INDUSTRIAL
    casa("casa_baja", casa_moderna(7, 7, camas=1, dos_plantas=False, semilla=0, est=e), 3)
    casa("casa", casa_moderna(9, 8, camas=2, semilla=1, est=e), 4)
    casa("casa_larga", casa_moderna(11, 9, camas=3, semilla=2, est=e), 3)
    casa("casa_grande", casa_moderna(13, 11, camas=4, semilla=0, est=e), 1)
    casa("fabrica", fabrica("smithing"), 1)
    casa("fabrica_textil", fabrica("loom"), 1)
    for i, puesto in enumerate(PUESTOS):
        casa("taller_" + puesto, casa_moderna(9, 8, camas=1, puesto=puesto, dos_plantas=False, semilla=i, est=e), 2)
    _piscina(raiz, pref + "centros", [_elemento(pref + "plaza", 1)])
    _piscina(raiz, pref + "casas", casas)
    estructura(raiz, "aldea_industrial", pref + "centros")
    return len(casas)


# ================================================================= HOY
# Estructura "yonga:aldea_contemporanea": un pueblo de hoy. PLAZA de baldosa con una fuente moderna, la campana sobre
# un monolito, bancos, jardineras, farolas y una marquesina de autobús; CALLES de asfalto con la raya discontinua;
# CASAS de dos plantas revocadas en colores claros, con ventanas grandes, azotea con placas solares y antena, y farola
# a la puerta; algunas de una planta con tejado de teja.

def asfalto(x, z):
    if x == 1 and z % 4 in (0, 1):
        return "minecraft:white_concrete"                      # la raya discontinua
    return "minecraft:gray_concrete" if _hash(x, z, 37) < 0.85 else "minecraft:light_gray_concrete"


def acera(x, z):
    return "minecraft:smooth_stone" if (x + z) % 2 else "minecraft:polished_andesite"


REVOCOS = ["minecraft:white_concrete", "minecraft:smooth_sandstone", "minecraft:light_gray_concrete",
           "minecraft:white_terracotta"]


def estilo_hoy(color, azotea=True):
    revoco = REVOCOS[color % len(REVOCOS)]
    return {
        "muro": lambda x, y, z: revoco, "esquina": lambda x, y, z: revoco,
        "zocalo": "minecraft:polished_andesite", "cimiento": "minecraft:smooth_stone",
        "viga": "minecraft:quartz_pillar", "tablon": "minecraft:birch_planks",
        "suelo": lambda x, z: "minecraft:smooth_quartz" if (x + z) % 2 else "minecraft:polished_diorite",
        "teja": "minecraft:brick_stairs", "losa": "minecraft:brick_slab", "cumbrera": "minecraft:bricks",
        "madera": "birch", "dintel": "minecraft:polished_andesite",
        "ventana_baja": "minecraft:glass_pane", "ventana_alta": "minecraft:glass_pane",
        "camino": acera, "farola": "minecraft:polished_deepslate_wall", "luz": "barra",
        "camas": ["minecraft:light_blue_bed", "minecraft:gray_bed", "minecraft:pink_bed", "minecraft:lime_bed"],
        "chimenea": False, "azotea": azotea, "cubierta": "minecraft:gray_concrete",
        "pretil": "minecraft:smooth_quartz_slab", "epoca": "contemporanea",
    }


def plaza_hoy():
    """Plaza de 17 x 17 de baldosa: fuente moderna, la campana sobre un monolito de cuarzo, bancos, jardineras con
    arbustos, farolas y una marquesina de autobús con banco."""
    n = 17
    p = Plantilla(n, 7, n)
    c = n // 2
    for x in range(n):
        for z in range(n):
            borde = x in (0, n - 1) or z in (0, n - 1)
            p.poner(x, 0, z, "minecraft:polished_andesite" if borde else
                    "minecraft:smooth_stone" if (x // 2 + z // 2) % 2 else "minecraft:light_gray_concrete")
            for y in range(1, 7):
                p.poner(x, y, z, "minecraft:air")
    # La campana, sobre un monolito de cuarzo
    p.poner(c, 1, c, "minecraft:quartz_pillar")
    p.poner(c, 2, c, "minecraft:bell", {"attachment": "floor", "facing": "north"})
    p.poner(c, 0, c, "minecraft:chiseled_quartz_block")
    # Fuente moderna: lámina de agua baja en un vaso de cuarzo, con chorro (un muro de cuarzo... de piedra lisa)
    fx, fz = c, c + 4
    for dx in range(-2, 3):
        for dz in range(-1, 2):
            if abs(dx) == 2 or abs(dz) == 1:
                p.poner(fx + dx, 0, fz + dz, "minecraft:smooth_quartz")
                p.poner(fx + dx, 1, fz + dz, "minecraft:smooth_quartz_slab", {"type": "bottom"})
            else:
                p.poner(fx + dx, 0, fz + dz, "minecraft:water", {"level": "0"})
    # Jardineras con arbustos y bancos
    for x, z in ((3, 3), (n - 4, 3), (3, n - 4), (n - 4, n - 4)):
        for dx in (0, 1):
            for dz in (0, 1):
                p.poner(x + dx, 1, z + dz, "minecraft:moss_block")
                p.poner(x + dx, 2, z + dz, "minecraft:azalea" if (dx + dz) % 2 else "minecraft:flowering_azalea")
    for dz, mira in ((-3, "north"),):
        for dx in (-2, -1, 1, 2):
            p.poner(c + dx, 1, c + dz, "minecraft:birch_stairs", {"facing": mira, "half": "bottom"})
    # Farolas
    for x, z in ((1, 1), (n - 2, 1), (1, n - 2), (n - 2, n - 2), (c - 4, c), (c + 4, c)):
        for y in (1, 2, 3):
            p.poner(x, y, z, "minecraft:polished_deepslate_wall")
        p.poner(x, 4, z, "minecraft:end_rod", {"facing": "up"})
    # Marquesina de autobús: tejadillo, cristal detrás y banco
    mx, mz = c + 5, c - 5
    for dx in range(-1, 2):
        p.poner(mx + dx, 1, mz - 1, "minecraft:glass_pane")
        p.poner(mx + dx, 2, mz - 1, "minecraft:glass_pane")
        p.poner(mx + dx, 3, mz - 1, "minecraft:smooth_stone_slab", {"type": "bottom"})
        p.poner(mx + dx, 3, mz, "minecraft:smooth_stone_slab", {"type": "bottom"})
        p.poner(mx + dx, 1, mz, "minecraft:birch_stairs", {"facing": "south", "half": "bottom"})
    for dx in (-2, 2):
        for y in (1, 2):
            p.poner(mx + dx, y, mz, "minecraft:iron_bars")
        p.poner(mx + dx, 3, mz, "minecraft:smooth_stone_slab", {"type": "bottom"})
    pre = "yonga:aldea/contemporanea/calles"
    jigsaw(p, c, 0, 0, "north_up", "yonga:calle", "yonga:calle", pre, "minecraft:polished_andesite")
    jigsaw(p, c, 0, n - 1, "south_up", "yonga:calle", "yonga:calle", pre, "minecraft:polished_andesite")
    jigsaw(p, 0, 0, c, "west_up", "yonga:calle", "yonga:calle", pre, "minecraft:polished_andesite")
    jigsaw(p, n - 1, 0, c, "east_up", "yonga:calle", "yonga:calle", pre, "minecraft:polished_andesite")
    jigsaw(p, c - 3, 1, c + 2, "up_north", "minecraft:bottom", "minecraft:bottom", VILLAGERS)
    jigsaw(p, c + 3, 1, c - 2, "up_north", "minecraft:bottom", "minecraft:bottom", VILLAGERS)
    return p


def contemporanea(raiz):
    base = os.path.join(raiz, "data", "yonga", "structures", "aldea", "contemporanea")
    pref = "yonga:aldea/contemporanea/"
    plaza_hoy().guardar(os.path.join(base, "plaza.nbt"))
    calles_de(raiz, base, pref, asfalto)
    casas = []
    create = procesadores_create(raiz, "contemporanea")

    def casa(nombre, plantilla, peso):
        plantilla.guardar(os.path.join(base, nombre + ".nbt"))
        casas.append(_elemento(pref + nombre, peso, procesadores=create))

    for i in range(4):
        casa(f"casa_{i}", casa_moderna(9, 8, camas=2, semilla=i, est=estilo_hoy(i)), 2)
    casa("chalet", casa_moderna(13, 11, camas=4, semilla=1, est=estilo_hoy(1)), 1)
    casa("adosado", casa_moderna(11, 9, camas=3, semilla=2, est=estilo_hoy(2)), 2)
    casa("casa_baja", casa_moderna(7, 7, camas=1, dos_plantas=False, semilla=3, est=estilo_hoy(0, azotea=False)), 2)
    casa("casa_baja_teja", casa_moderna(9, 8, camas=2, dos_plantas=False, semilla=0, est=estilo_hoy(3, azotea=False)), 2)
    for i, puesto in enumerate(PUESTOS):
        casa("taller_" + puesto, casa_moderna(9, 8, camas=1, puesto=puesto, dos_plantas=False, semilla=i,
                                              est=estilo_hoy(i)), 2)
    _piscina(raiz, pref + "centros", [_elemento(pref + "plaza", 1)])
    _piscina(raiz, pref + "casas", casas)
    estructura(raiz, "aldea_contemporanea", pref + "centros")
    return len(casas)


def escribir_json(raiz, partes, datos):
    ruta = os.path.join(raiz, *partes)
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(datos, f, indent=2)


def comunes(raiz, epocas):
    """Biomas con aldea, la etiqueta de aldeas (para que cuenten como aldeas: mapas, /locate…) y el conjunto de
    estructuras de aldeas: solo las nuestras (una por época), todas con el mismo peso. Las de Minecraft ya no salen."""
    escribir_json(raiz, ["data", "yonga", "tags", "worldgen", "biome", "tiene_aldea.json"], {"replace": False, "values": [
        "#minecraft:has_structure/village_" + b for b in BIOMAS]})
    nuestras = ["yonga:aldea_" + e for e in epocas]
    escribir_json(raiz, ["data", "minecraft", "tags", "worldgen", "structure", "village.json"],
                  {"replace": False, "values": nuestras})
    # Para encontrarlas: /locate structure yonga:aldea_<época> (o #yonga:aldea_<época>)
    for e in epocas:
        escribir_json(raiz, ["data", "yonga", "tags", "worldgen", "structure", "aldea_" + e + ".json"],
                      {"replace": False, "values": ["yonga:aldea_" + e]})
    escribir_json(raiz, ["data", "minecraft", "worldgen", "structure_set", "villages.json"], {
        "structures": [{"structure": s, "weight": 1} for s in nuestras],
        "placement": {"type": "minecraft:random_spread", "salt": 10387312, "separation": 8, "spacing": 34}})


def main():
    raiz = sys.argv[1] if len(sys.argv) > 1 else "src/main/resources"
    n = prehistorica(raiz)
    print(f"Prehistoria: {n} chozas; piscinas de casas de Minecraft sustituidas en {len(BIOMAS)} biomas")
    n = antigua(raiz)
    print(f"Antigüedad: foro, calles y {n} casas (estructura yonga:aldea_antigua)")
    n = medieval(raiz)
    print(f"Edad Media: plaza, calles y {n} casas (estructura yonga:aldea_medieval)")
    n = moderna(raiz)
    print(f"Siglo XVII: plaza mayor, calles y {n} casas (estructura yonga:aldea_moderna)")
    n = industrial(raiz)
    print(f"Época industrial: plaza con quiosco, calles y {n} edificios (estructura yonga:aldea_industrial)")
    n = contemporanea(raiz)
    print(f"Hoy: plaza, calles de asfalto y {n} casas (estructura yonga:aldea_contemporanea)")
    comunes(raiz, ["prehistorica", "antigua", "medieval", "moderna", "industrial", "contemporanea"])


if __name__ == "__main__":
    main()
