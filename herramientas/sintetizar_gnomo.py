"""Sonidos del gnomo (con la voz aguda de sintetizar_duende.py, algo más grave y menos chillona):
  gnomo_grito     "¡ja!" de guerra, corto (al saltar y de vez en cuando)
  gnomo_esfuerzo  "¡nnngh!" al tirar de la espada clavada
  gnomo_herido    quejido corto
  gnomo_muerte    "¡aaay...!" que se apaga
Uso: python3 sintetizar_gnomo.py <carpeta_salida>"""
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


def grito():
    s = D.voz_aguda(0.28, [(0, 420), (0.3, 560), (1, 380)], vocal=0.0, fritura=0.1)
    ataque = A.banda(A.ruido(0.28), 2500, 8000) * A.envolvente(len(s), [(0, 0.6), (0.12, 0), (1, 0)])   # la "j"
    return A.fundido(A.normalizar((s + ataque * 0.4) * A.envolvente(len(s), [(0, 0), (0.1, 1), (0.7, 0.8), (1, 0)]), 0.9))


def esfuerzo():
    s = D.voz_aguda(0.55, [(0, 300), (0.6, 340), (1, 280)], vocal=0.9, fritura=0.35)   # "nnngh": cerrada, rota
    am = 0.7 + 0.3 * np.sin(2 * np.pi * 11 * A.t(0.55))
    return A.fundido(A.normalizar(s * am * A.envolvente(len(s), [(0, 0), (0.15, 1), (0.85, 0.9), (1, 0)]), 0.8))


def herido():
    s = D.voz_aguda(0.25, [(0, 600), (0.3, 720), (1, 450)], vocal=0.3, fritura=0.2)
    return A.fundido(A.normalizar(s * A.envolvente(len(s), [(0, 0), (0.1, 1), (1, 0)]), 0.9))


def muerte():
    s = D.voz_aguda(0.9, [(0, 650), (0.2, 720), (1, 250)], vocal=0.2, fritura=0.3)
    return A.fundido(A.normalizar(s * A.envolvente(len(s), [(0, 0), (0.08, 1), (0.7, 0.6), (1, 0)]), 0.9))


SONIDOS = {"gnomo_grito": grito, "gnomo_esfuerzo": esfuerzo, "gnomo_herido": herido, "gnomo_muerte": muerte}

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
