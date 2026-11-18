"""Sonidos del centinela de piedra (con las herramientas de sintetizar_astado.py):
  centinela_despertar  retumbar de piedra que roza y cruje, con un zumbido grave (también al morir)
  centinela_carga      zumbido que sube mientras apunta el rayo (1,5 s)
  centinela_rayo       el estallido del rayo de luz
Uso: python3 sintetizar_centinela.py <carpeta_salida>"""
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


def despertar():
    d = 2.4
    n = int(SR * d)
    tt = A.t(d)
    roce = A.banda(A.ruido(d), 60, 600) * (0.6 + 0.4 * A.jitter(n, 0.8, 900))
    crujidos = (np.random.default_rng(3).random(n) < 0.0015) * np.random.default_rng(4).standard_normal(n) * 4
    crujidos = A.banda(crujidos, 800, 5000)
    zumbido = (np.sin(2 * np.pi * 55 * tt) + 0.5 * np.sin(2 * np.pi * 110 * tt + 0.3)) * A.envolvente(n, [(0, 0), (0.5, 0.6), (1, 0)])
    env = A.envolvente(n, [(0, 0), (0.1, 1), (0.8, 0.8), (1, 0)])
    return A.fundido(A.normalizar((roce * 1.2 + crujidos * 0.6 + zumbido * 0.5) * env, 0.9))


def carga():
    d = 1.5
    n = int(SR * d)
    frec = A.envolvente(n, [(0, 180), (1, 900)])
    fase = np.cumsum(frec / SR)
    tono = np.sin(2 * np.pi * fase) + 0.4 * np.sin(2 * np.pi * fase * 2.01) + 0.2 * np.sin(2 * np.pi * fase * 3.02)
    vibrato = 1 + 0.3 * np.sin(2 * np.pi * A.envolvente(n, [(0, 6), (1, 22)]) * A.t(d))
    brillo = A.banda(A.ruido(d), 3000, 9000) * A.envolvente(n, [(0, 0), (1, 0.35)])
    env = A.envolvente(n, [(0, 0), (0.2, 0.6), (1, 1)])
    return A.fundido(A.normalizar((tono * vibrato * 0.8 + brillo) * env, 0.85))


def rayo():
    d = 0.9
    n = int(SR * d)
    tt = A.t(d)
    golpe = A.saturar(A.ruido(d), 2.0) * np.exp(-tt * 9)
    golpe = A.banda(golpe, 200, 9000)
    zap = A.sierra(A.envolvente(n, [(0, 1400), (1, 300)]), tt) * np.exp(-tt * 5) * 0.6
    grave = np.sin(2 * np.pi * (60 + 40 * np.exp(-tt * 8)) * tt) * np.exp(-tt * 4) * 0.9
    return A.fundido(A.normalizar(golpe + zap + grave, 0.95))


SONIDOS = {"centinela_despertar": despertar, "centinela_carga": carga, "centinela_rayo": rayo}

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
