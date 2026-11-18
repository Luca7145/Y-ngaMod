"""Sonidos de la gárgola (con la voz de sintetizar_duende.py y el ruido de sintetizar_astado.py):
  gargola_chillido  chillido áspero, entre murciélago grande y buitre
  gargola_grunido   gruñido grave con roce de piedra
  gargola_muerte    chillido que se rompe en un desmoronarse de piedra
Uso: python3 sintetizar_gargola.py <carpeta_salida>"""
import importlib.util
import os
import subprocess
import sys

import numpy as np
from scipy.io import wavfile

_ruta = os.path.join(os.path.dirname(os.path.abspath(__file__)), "sintetizar_duende.py")
_spec = importlib.util.spec_from_file_location("sintetizar_duende", _ruta)
D = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(D)
A = D.A
SR = D.SR


def chillido():
    d = 0.6
    v = D.voz_aguda(d, [(0, 900), (0.2, 1300), (1, 800)], vocal=0.1, fritura=0.5)
    aspero = A.banda(A.ruido(d), 2000, 8000) * 0.5
    s = A.saturar(v + aspero, 2.5)
    return A.fundido(A.normalizar(s * A.envolvente(len(s), [(0, 0), (0.08, 1), (0.6, 0.7), (1, 0)]), 0.9))


def grunido():
    d = 1.1
    n = int(SR * d)
    v = D.voz_aguda(d, [(0, 160), (0.5, 190), (1, 140)], vocal=0.9, fritura=0.6)
    roce = A.banda(A.ruido(d), 80, 700) * (0.5 + 0.5 * A.jitter(n, 0.8, 1200))
    s = A.saturar(v * 0.8 + roce * 0.9, 2.0)
    return A.fundido(A.normalizar(s * A.envolvente(n, [(0, 0), (0.2, 1), (0.8, 0.7), (1, 0)]), 0.85))


def muerte():
    c = chillido()
    d = 1.2
    n = int(SR * d)
    golpes = (np.random.default_rng(5).random(n) < 0.004) * np.random.default_rng(6).standard_normal(n) * 3
    piedra = A.banda(golpes, 300, 5000) + A.banda(A.ruido(d), 60, 500) * 0.5
    piedra = piedra * A.envolvente(n, [(0, 1), (1, 0)])
    return A.fundido(A.normalizar(np.concatenate([c * 0.9, piedra]), 0.9))


SONIDOS = {"gargola_chillido": chillido, "gargola_grunido": grunido, "gargola_muerte": muerte}

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
