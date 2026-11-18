"""Sonidos del Pez del viento (con las herramientas de sintetizar_astado.py):
  pez_canto    canto de ballena: gemidos largos que suben y bajan, con eco (intro, rodeando, al dividirse)
  pez_bramido  bramido grave y enorme (empieza un ataque)
  pez_rayo     el rayo de luz: zumbido brillante y sostenido con chisporroteo
  pez_golpe    el golpe contra el suelo: estruendo y retumbar
  pez_ilusion  destello de cristal (aparece o se deshace una ilusión)
  pez_herido   quejido corto
  pez_muerte   canto largo que se apaga, con mucho eco
Uso: python3 sintetizar_pez.py <carpeta_salida>"""
import importlib.util
import os
import subprocess
import sys

import numpy as np
from scipy.io import wavfile

_ruta = os.path.join(os.path.dirname(os.path.abspath(__file__)), "sintetizar_astado.py")
_spec = importlib.util.spec_from_file_location("sintetizar_astado", _ruta)
A = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(A)
SR = A.SR


def eco(x, retrasos=((0.23, 0.45), (0.51, 0.3), (0.83, 0.18), (1.2, 0.1))):
    """Ecos (como en un espacio enorme)."""
    largo = len(x) + int(SR * max(r for r, _ in retrasos)) + 1
    y = np.zeros(largo)
    y[:len(x)] += x
    for r, g in retrasos:
        i = int(SR * r)
        y[i:i + len(x)] += A.paso_bajo(x, 2500) * g
    return y


def gemido(dur, curva, brillo=0.3):
    n = int(SR * dur)
    tt = A.t(dur)
    frec = A.envolvente(n, curva) * (1 + 0.006 * np.sin(2 * np.pi * 5 * tt))
    fase = np.cumsum(frec / SR)
    voz = np.sin(2 * np.pi * fase) + 0.5 * np.sin(2 * np.pi * 2 * fase) + brillo * np.sin(2 * np.pi * 3 * fase)
    voz += 0.15 * A.banda(A.ruido(dur), 200, 1500)
    return voz * A.envolvente(n, [(0, 0), (0.15, 1), (0.8, 0.85), (1, 0)])


def canto():
    partes = [gemido(1.6, [(0, 160), (0.4, 320), (1, 260)]), np.zeros(int(SR * 0.15)),
              gemido(1.2, [(0, 420), (0.5, 380), (1, 210)], 0.5), np.zeros(int(SR * 0.1)),
              gemido(1.4, [(0, 140), (0.7, 120), (1, 90)], 0.2)]
    return A.fundido(A.normalizar(eco(np.concatenate(partes)), 0.9))


def bramido():
    d = 2.0
    n = int(SR * d)
    tt = A.t(d)
    frec = A.envolvente(n, [(0, 55), (0.3, 80), (1, 45)])
    fase = np.cumsum(frec / SR)
    voz = A.sierra(frec, tt) + 0.6 * np.sin(2 * np.pi * fase * 0.5)
    voz = A.saturar(A.paso_bajo(voz, 900), 2.5)
    soplo = A.banda(A.ruido(d), 100, 1200) * 0.6
    x = (voz + soplo) * A.envolvente(n, [(0, 0), (0.1, 1), (0.7, 0.8), (1, 0)])
    return A.fundido(A.normalizar(eco(x, ((0.3, 0.35), (0.7, 0.2))), 0.95))


def rayo():
    d = 2.6
    n = int(SR * d)
    tt = A.t(d)
    zumbido = sum(np.sin(2 * np.pi * f * tt) / k for k, f in enumerate((220, 440, 662, 880, 1330), 1))
    zumbido *= 1 + 0.25 * np.sin(2 * np.pi * 9 * tt)
    chispas = (np.random.default_rng(8).random(n) < 0.004) * np.random.default_rng(9).standard_normal(n) * 2
    chispas = A.banda(chispas, 2000, 9000)
    brillo = A.banda(A.ruido(d), 3000, 10000) * 0.3
    x = (zumbido * 0.6 + chispas + brillo) * A.envolvente(n, [(0, 0), (0.04, 1), (0.9, 0.9), (1, 0)])
    return A.fundido(A.normalizar(x, 0.9))


def golpe():
    d = 2.5
    n = int(SR * d)
    tt = A.t(d)
    estruendo = A.saturar(A.ruido(d), 2.0) * np.exp(-tt * 3.5)
    estruendo = A.paso_bajo(estruendo, 3000)
    grave = np.sin(2 * np.pi * (38 + 30 * np.exp(-tt * 5)) * tt) * np.exp(-tt * 1.6)
    retumbar = A.banda(A.ruido(d), 30, 200) * np.exp(-tt * 1.2) * 1.5
    return A.fundido(A.normalizar(estruendo + grave * 1.4 + retumbar, 0.98))


def ilusion():
    d = 1.4
    tt = A.t(d)
    x = sum(np.sin(2 * np.pi * f * tt) * np.exp(-tt * (2.5 + i)) for i, f in enumerate((880, 1320, 1760, 2640, 3520)))
    x += A.banda(A.ruido(d), 4000, 12000) * np.exp(-tt * 4) * 0.4
    return A.fundido(A.normalizar(eco(x, ((0.12, 0.4), (0.27, 0.25))), 0.8))


def herido():
    return A.fundido(A.normalizar(gemido(0.6, [(0, 300), (0.3, 380), (1, 200)], 0.6), 0.9))


def muerte():
    partes = [gemido(2.0, [(0, 300), (0.3, 420), (1, 200)], 0.4), gemido(2.4, [(0, 220), (1, 70)], 0.2)]
    return A.fundido(A.normalizar(eco(np.concatenate(partes), ((0.35, 0.5), (0.8, 0.35), (1.4, 0.22), (2.1, 0.12))), 0.9))


SONIDOS = {"pez_canto": canto, "pez_bramido": bramido, "pez_rayo": rayo, "pez_golpe": golpe, "pez_ilusion": ilusion,
           "pez_herido": herido, "pez_muerte": muerte}

if __name__ == "__main__":
    salida = sys.argv[1] if len(sys.argv) > 1 else "."
    for nombre, funcion in SONIDOS.items():
        audio = funcion()
        wav = os.path.join(salida, nombre + ".wav")
        wavfile.write(wav, SR, (audio * 32767).astype(np.int16))
        try:
            subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-c:a", "libvorbis",
                            "-q:a", "5", os.path.join(salida, nombre + ".ogg")], check=True)
        except (OSError, subprocess.CalledProcessError):
            print("Sin ffmpeg: convierte los .wav a .ogg mono a mano")
