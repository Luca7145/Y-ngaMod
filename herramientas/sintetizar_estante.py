"""Sonidos del estante (block.shelf.*), con las herramientas de sintetizar_astado.py: golpecitos de madera.
  activate      clac de madera y un zumbido corto que sube (se enciende)
  deactivate    lo mismo, bajando
  multi_swap    varios golpes rápidos de madera, con un roce
  place_item    un toque suave de algo que se apoya en la madera
  single_swap   dos toques (sale uno, entra otro)
  take_item     un toque más ligero y agudo
Uso: python3 sintetizar_estante.py <carpeta_salida>   (deja los .ogg en <carpeta>/shelf/)"""
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


def toque(d, frec=420, brillo=2500, decae=38.0, fuerza=1.0):
    """Un golpe de madera: un poco de ruido con resonancias que se apagan enseguida."""
    tt = A.t(d)
    golpe = A.banda(A.ruido(d), 300, brillo) * np.exp(-tt * decae * 2.5)
    cuerpo = (np.sin(2 * np.pi * frec * tt) + 0.5 * np.sin(2 * np.pi * frec * 2.3 * tt)
              + 0.25 * np.sin(2 * np.pi * frec * 3.9 * tt)) * np.exp(-tt * decae)
    return (golpe * 0.8 + cuerpo * 0.6) * fuerza


def pegar(total, trozos):
    n = int(SR * total)
    x = np.zeros(n)
    for inicio, s in trozos:
        i = int(SR * inicio)
        x[i:i + len(s)] += s[:n - i]
    return A.fundido(A.normalizar(x, 0.9))


def zumbido(d, desde, hasta):
    n = int(SR * d)
    frec = np.linspace(desde, hasta, n)
    fase = np.cumsum(frec / SR)
    x = np.sin(2 * np.pi * fase) * A.envolvente(n, [(0, 0), (0.2, 1), (1, 0)])
    return A.paso_bajo(x, 1200) * 0.25


SONIDOS = {
    "activate": lambda: pegar(0.45, [(0, toque(0.3, 380)), (0.03, zumbido(0.35, 180, 360))]),
    "deactivate": lambda: pegar(0.45, [(0, toque(0.3, 340)), (0.03, zumbido(0.35, 360, 160))]),
    "multi_swap": lambda: pegar(0.6, [(0.0, toque(0.25, 400)), (0.07, toque(0.25, 470, fuerza=0.8)),
                                      (0.14, toque(0.25, 430, fuerza=0.9)), (0.21, toque(0.3, 380)),
                                      (0.02, A.banda(A.ruido(0.3), 1500, 6000) * np.exp(-A.t(0.3) * 8) * 0.25)]),
    "place_item": lambda: pegar(0.3, [(0, toque(0.28, 360, 1800, 30.0))]),
    "single_swap": lambda: pegar(0.35, [(0, toque(0.25, 450, fuerza=0.7)), (0.09, toque(0.25, 370))]),
    "take_item": lambda: pegar(0.25, [(0, toque(0.22, 520, 3500, 45.0, 0.8))]),
}

if __name__ == "__main__":
    salida = os.path.join(sys.argv[1] if len(sys.argv) > 1 else ".", "shelf")
    os.makedirs(salida, exist_ok=True)
    for nombre, funcion in SONIDOS.items():
        audio = funcion()
        wav = os.path.join(salida, nombre + ".wav")
        wavfile.write(wav, SR, (audio * 32767).astype(np.int16))
        try:
            subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-c:a", "libvorbis",
                            "-q:a", "5", os.path.join(salida, nombre + ".ogg")], check=True)
            os.remove(wav)
        except (OSError, subprocess.CalledProcessError):
            print("Sin ffmpeg: convierte los .wav a .ogg mono a mano")
