"""Sonidos de Zor el quemado (con las herramientas de sintetizar_astado.py):
  zor_rugido   rugido de dragón: grave, rasposo, con un gruñido que sube y crepita
  zor_fuego    el chorro de fuego: rugido de llamas (ruido grave que sopla) con chasquidos
  zor_excavar  la tierra que se remueve: crujidos y retumbar sordo bajo tierra
  zor_emerger  sale de la tierra: estallido, piedras que caen y un rugido corto
  zor_herido   gruñido corto de dolor
  zor_muerte   rugido largo que se apaga entre crepitar de brasas
Uso: python3 sintetizar_zor.py <carpeta_salida>"""
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


def crepitar(d, densidad=0.003, semilla=3):
    n = int(SR * d)
    r = np.random.default_rng(semilla)
    chasquidos = (r.random(n) < densidad) * r.standard_normal(n) * 3
    return A.banda(chasquidos, 1500, 8000)


def voz_dragon(d, curva, sucio=3.0):
    n = int(SR * d)
    tt = A.t(d)
    frec = A.envolvente(n, curva) * A.jitter(n, 0.05)
    fase = np.cumsum(frec / SR)
    voz = A.sierra(frec, tt) + 0.7 * np.sin(2 * np.pi * fase * 0.5) + 0.3 * np.sin(2 * np.pi * fase * 1.5)
    # aspereza: modulación rápida (como un gruñido de garganta)
    voz *= 1 + 0.5 * np.sin(2 * np.pi * 31 * tt)
    voz = A.saturar(A.paso_bajo(voz, 1400), sucio)
    soplo = A.banda(A.ruido(d), 150, 2500) * 0.5
    return voz + soplo


def rugido():
    d = 2.4
    n = int(SR * d)
    x = voz_dragon(d, [(0, 50), (0.25, 95), (0.6, 85), (1, 40)])
    x = x * A.envolvente(n, [(0, 0), (0.08, 1), (0.7, 0.85), (1, 0)]) + crepitar(d) * 0.4
    return A.fundido(A.normalizar(x, 0.95))


def fuego():
    d = 2.6
    n = int(SR * d)
    tt = A.t(d)
    llamas = A.banda(A.ruido(d), 60, 1800) * (1 + 0.4 * np.sin(2 * np.pi * 6 * tt + np.sin(2 * np.pi * 1.3 * tt)))
    silbido = A.banda(A.ruido(d), 2500, 7000) * 0.25
    x = A.saturar(llamas, 1.6) + silbido + crepitar(d, 0.006, 5) * 0.6
    return A.fundido(A.normalizar(x * A.envolvente(n, [(0, 0), (0.06, 1), (0.85, 0.9), (1, 0)]), 0.9))


def excavar():
    d = 1.8
    n = int(SR * d)
    tt = A.t(d)
    retumbar = A.banda(A.ruido(d), 25, 160) * (1 + 0.6 * np.sin(2 * np.pi * 3 * tt)) * 1.4
    crujidos = A.banda(crepitar(d, 0.004, 7), 300, 2500) * 1.2
    piedras = A.paso_bajo(crepitar(d, 0.0015, 11), 1500)
    x = (retumbar + crujidos + piedras) * A.envolvente(n, [(0, 0), (0.15, 1), (0.8, 1), (1, 0)])
    return A.fundido(A.normalizar(x, 0.85))


def emerger():
    d = 2.6
    n = int(SR * d)
    tt = A.t(d)
    estallido = A.paso_bajo(A.saturar(A.ruido(d), 2.5), 2500) * np.exp(-tt * 4.0)
    grave = np.sin(2 * np.pi * (34 + 40 * np.exp(-tt * 6)) * tt) * np.exp(-tt * 1.4) * 1.5
    piedras = A.paso_bajo(crepitar(d, 0.002, 13), 2500) * np.exp(-tt * 0.8) * 1.2
    corto = np.zeros(n)
    r = voz_dragon(1.3, [(0, 70), (0.3, 110), (1, 55)])
    r *= A.envolvente(len(r), [(0, 0), (0.1, 1), (1, 0)])
    i = int(SR * 0.35)
    corto[i:i + len(r)] = r[:n - i] * 0.7
    return A.fundido(A.normalizar(estallido + grave + piedras + corto, 0.98))


def herido():
    d = 0.7
    n = int(SR * d)
    x = voz_dragon(d, [(0, 110), (0.3, 150), (1, 70)], 4.0)
    return A.fundido(A.normalizar(x * A.envolvente(n, [(0, 0), (0.08, 1), (1, 0)]), 0.9))


def muerte():
    d = 4.5
    n = int(SR * d)
    tt = A.t(d)
    x = voz_dragon(d, [(0, 90), (0.2, 120), (0.6, 60), (1, 25)])
    x *= A.envolvente(n, [(0, 0), (0.05, 1), (0.5, 0.7), (1, 0)])
    brasas = crepitar(d, 0.005, 17) * np.minimum(1.0, tt / 1.5) * 0.7
    return A.fundido(A.normalizar(x + brasas, 0.95))


SONIDOS = {"zor_rugido": rugido, "zor_fuego": fuego, "zor_excavar": excavar, "zor_emerger": emerger,
           "zor_herido": herido, "zor_muerte": muerte}

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
