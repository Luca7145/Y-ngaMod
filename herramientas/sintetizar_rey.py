"""Sonidos del Rey de Arenisca (con las herramientas de sintetizar_astado.py):
  rey_paso      un paso de coloso: retumbar grave y arena que se escurre
  rey_rugido    gemido de piedra: un zumbido grave que cruje y roza, como una montaña que se queja
  rey_golpe     golpe contra el suelo: estallido sordo, grave que tiembla y piedras que caen
  rey_sello     se rompe un sello: cristal que estalla y una campanada dorada que se apaga
  rey_tormenta  viento que aúlla con arena
  rey_herido    crujido de piedra corto con un quejido
  rey_muerte    se derrumba: gemido largo que baja, y piedra que se desmorona
Uso: python3 sintetizar_rey.py <carpeta_salida>"""
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


def crujidos(d, densidad=0.002, semilla=1, lo=200, hi=3000):
    n = int(SR * d)
    r = np.random.default_rng(semilla)
    x = (r.random(n) < densidad) * r.standard_normal(n) * 3
    return A.banda(x, lo, hi)


def arena(d, semilla=2):
    return A.banda(A.ruido(d), 1500, 7000) * 0.5 + crujidos(d, 0.01, semilla, 2000, 8000) * 0.3


def gemido(d, curva, aspero=2.5):
    n = int(SR * d)
    tt = A.t(d)
    frec = A.envolvente(n, curva) * A.jitter(n, 0.04)
    fase = np.cumsum(frec / SR)
    voz = A.sierra(frec, tt) + 0.8 * np.sin(2 * np.pi * fase * 0.5) + 0.4 * np.sin(2 * np.pi * fase * 2.01)
    voz *= 1 + 0.6 * np.sin(2 * np.pi * 13 * tt)          # el roce de la piedra
    voz = A.saturar(A.paso_bajo(voz, 900), aspero)
    return voz + crujidos(d, 0.003, 3) * 0.8


def paso():
    d = 1.6
    tt = A.t(d)
    golpe = np.sin(2 * np.pi * (28 + 30 * np.exp(-tt * 8)) * tt) * np.exp(-tt * 3.0) * 1.6
    sordo = A.paso_bajo(A.ruido(d), 300) * np.exp(-tt * 6.0) * 1.2
    escurre = arena(d, 4) * np.exp(-((tt - 0.5) ** 2) / 0.12) * 0.6
    return A.fundido(A.normalizar(golpe + sordo + escurre, 0.95))


def rugido():
    d = 3.2
    n = int(SR * d)
    x = gemido(d, [(0, 38), (0.3, 62), (0.7, 55), (1, 30)])
    x *= A.envolvente(n, [(0, 0), (0.12, 1), (0.75, 0.85), (1, 0)])
    return A.fundido(A.normalizar(x + arena(d, 5) * 0.25, 0.95))


def golpe():
    d = 2.4
    tt = A.t(d)
    estallido = A.paso_bajo(A.saturar(A.ruido(d), 2.0), 1800) * np.exp(-tt * 5.0) * 1.2
    grave = np.sin(2 * np.pi * (24 + 50 * np.exp(-tt * 7)) * tt) * np.exp(-tt * 1.6) * 1.8
    piedras = A.paso_bajo(crujidos(d, 0.002, 7), 2500) * np.exp(-tt * 0.9) * 1.2
    return A.fundido(A.normalizar(estallido + grave + piedras + arena(d, 6) * np.exp(-tt * 1.2) * 0.5, 0.98))


def sello():
    d = 3.0
    tt = A.t(d)
    cristal = A.banda(A.ruido(d), 3000, 9000) * np.exp(-tt * 9) * 1.2 + crujidos(d, 0.006, 8, 2500, 9000) * np.exp(-tt * 4)
    campana = sum(np.sin(2 * np.pi * f * tt) * a * np.exp(-tt * k) for f, a, k in
                  ((523, 0.6, 1.2), (784, 0.4, 1.6), (1047, 0.3, 2.0), (1318, 0.15, 2.6)))
    grave = np.sin(2 * np.pi * 65 * tt) * np.exp(-tt * 1.5) * 0.6
    return A.fundido(A.normalizar(cristal + campana + grave, 0.95))


def tormenta():
    d = 4.0
    n = int(SR * d)
    tt = A.t(d)
    aullido = A.banda(A.ruido(d), 300, 1400) * (1 + 0.6 * np.sin(2 * np.pi * 0.7 * tt + np.sin(2 * np.pi * 0.23 * tt)))
    silbido = A.banda(A.ruido(d), 2000, 5000) * (0.4 + 0.3 * np.sin(2 * np.pi * 1.1 * tt))
    x = (aullido + silbido + arena(d, 9) * 0.6) * A.envolvente(n, [(0, 0), (0.2, 1), (0.8, 1), (1, 0)])
    return A.fundido(A.normalizar(x, 0.85))


def herido():
    d = 0.9
    n = int(SR * d)
    tt = A.t(d)
    crack = crujidos(d, 0.02, 10, 400, 5000) * np.exp(-tt * 6) * 1.5
    queja = gemido(d, [(0, 70), (0.3, 95), (1, 50)], 3.0) * A.envolvente(n, [(0, 0), (0.1, 1), (1, 0)]) * 0.7
    return A.fundido(A.normalizar(crack + queja, 0.9))


def muerte():
    d = 6.0
    n = int(SR * d)
    tt = A.t(d)
    x = gemido(d, [(0, 60), (0.25, 75), (0.6, 40), (1, 18)])
    x *= A.envolvente(n, [(0, 0), (0.05, 1), (0.6, 0.7), (1, 0)])
    derrumbe = A.paso_bajo(crujidos(d, 0.004, 11), 2000) * np.minimum(1.0, tt / 2.0) * 1.2
    retumbar = A.banda(A.ruido(d), 20, 120) * np.minimum(1.0, tt / 3.0) * 1.5
    return A.fundido(A.normalizar(x + derrumbe + retumbar + arena(d, 12) * 0.4, 0.95))


SONIDOS = {"rey_paso": paso, "rey_rugido": rugido, "rey_golpe": golpe, "rey_sello": sello,
           "rey_tormenta": tormenta, "rey_herido": herido, "rey_muerte": muerte}

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
