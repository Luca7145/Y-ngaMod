"""Sonidos del duende (reutilizando el modelo de garganta de sintetizar_leon.py, pero con voz aguda):
  duende_risa     risita "jijiji" entrecortada y burlona
  duende_soplido  soplido seco de cerbatana ("ffft")
  duende_puf      "puf" mágico al volverse invisible o reaparecer
  duende_herido   chillido corto
  duende_muerte   chillido que cae y se apaga
Uso: python3 sintetizar_duende.py <carpeta_salida>"""
import importlib.util
import os
import subprocess
import sys

import numpy as np
from scipy.io import wavfile

_ruta = os.path.join(os.path.dirname(os.path.abspath(__file__)), "sintetizar_leon.py")
_spec = importlib.util.spec_from_file_location("sintetizar_leon", _ruta)
L = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(L)
A = L.A
SR = A.SR


def voz_aguda(dur, tono, vocal=0.0, fritura=0.1):
    """Como la del león pero con resonancias de garganta pequeña (más agudas)."""
    n = int(SR * dur)
    frec = A.envolvente(n, tono) * A.jitter(n, 0.04, 300)
    glotis = L.pulsos_glotis(frec, n, fritura, 0.03)
    v = np.full(n, vocal)
    f1 = 900 - 350 * v
    f2 = 2400 - 700 * v
    f3 = np.full(n, 3600.0)
    x = L.formantes(glotis + A.ruido(dur) * 0.01, f1, f2, f3, anchos=(0.6, 0.4, 0.3), pesos=(1.0, 0.8, 0.4))
    return A.saturar(x / (np.max(np.abs(x)) + 1e-9), 1.5)


def risa():
    partes = []
    for k in range(6):
        d = 0.075 + 0.01 * (k % 2)
        f = 620 + 60 * np.sin(k * 1.7) + (k % 2) * 40
        s = voz_aguda(d, [(0, f), (1, f * 1.08)], vocal=0.0)
        aire = A.banda(A.ruido(d), 2000, 7000) * 0.25        # la "j" de jiji
        s = (s + aire) * A.envolvente(len(s), [(0, 0), (0.2, 1), (1, 0)]) * (1.0 - k * 0.08)
        partes += [s, np.zeros(int(SR * (0.045 + 0.01 * k)))]
    return A.fundido(A.normalizar(np.concatenate(partes), 0.85))


def soplido():
    d = 0.22
    n = int(SR * d)
    golpe = A.banda(A.ruido(d), 1500, 9000) * A.envolvente(n, [(0, 0), (0.05, 1), (0.4, 0.3), (1, 0)])
    tubo = np.sin(2 * np.pi * 420 * A.t(d)) * np.exp(-A.t(d) * 25) * 0.4    # resuena la caña
    return A.fundido(A.normalizar(golpe + tubo, 0.85))


def puf():
    d = 0.6
    n = int(SR * d)
    nube = A.banda(A.ruido(d), 300, 3000) * A.envolvente(n, [(0, 0), (0.06, 1), (1, 0)])
    tt = A.t(d)
    brillo = sum(np.sin(2 * np.pi * f * tt) * np.exp(-tt * (6 + i)) for i, f in enumerate((1320, 1760, 2210, 2640)))
    return A.fundido(A.normalizar(nube * 0.7 + brillo * 0.25, 0.8))


def herido():
    d = 0.3
    s = voz_aguda(d, [(0, 900), (0.3, 1150), (1, 700)], vocal=0.2, fritura=0.25)
    return A.fundido(A.normalizar(s * A.envolvente(len(s), [(0, 0), (0.1, 1), (1, 0)]), 0.9))


def muerte():
    d = 1.1
    s = voz_aguda(d, [(0, 1000), (0.2, 1200), (1, 300)], vocal=0.5, fritura=0.35)
    return A.fundido(A.normalizar(s * A.envolvente(len(s), [(0, 0), (0.08, 1), (0.7, 0.6), (1, 0)]), 0.9))


SONIDOS = {"duende_risa": risa, "duende_soplido": soplido, "duende_puf": puf, "duende_herido": herido, "duende_muerte": muerte}

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
