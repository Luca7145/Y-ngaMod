"""Sonidos del león de guerra (sintetizados, reutilizando las herramientas de sintetizar_astado.py):
  leon_rugido   rugido de león de verdad: gemidos, el rugido ("aaaooo", voz rota) y la serie de gruñidos
                cada vez más separados. La voz es un modelo de garganta: pulsos irregulares de las cuerdas
                vocales con "fritura" pasando por resonancias que se mueven (de "a" a "u").
  leon_grunido  gruñido grave de fondo
  leon_herido   quejido corto
  leon_muerte   rugido que se apaga
  leon_tajo     silbido de un tajo (sable o zarpa)
Uso: python3 sintetizar_leon.py <carpeta_salida>"""
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
SR, t, ruido, banda, paso_bajo, envolvente, sierra, saturar, jitter, normalizar, fundido = (
    A.SR, A.t, A.ruido, A.banda, A.paso_bajo, A.envolvente, A.sierra, A.saturar, A.jitter, A.normalizar, A.fundido)


from scipy.signal import butter, sosfilt, sosfilt_zi


def pulsos_glotis(frec, n, fritura=0.25, jit=0.04):
    """Pulsos de las cuerdas vocales (no una onda perfecta): cada periodo con un poco de desorden y, con
    'fritura', pulsos alternos más débiles (la voz rota y rasposa de un león)."""
    x = np.zeros(n)
    pos = 0.0
    k = 0
    rng = np.random.default_rng(11)
    while pos < n - 1:
        i = int(pos)
        f = max(30.0, frec[i]) * (1 + jit * rng.standard_normal())
        amp = (1.0 - fritura if k % 2 else 1.0) * (1 + 0.15 * rng.standard_normal())
        x[i] += amp
        pos += SR / f
        k += 1
    # forma del pulso: decae rápido (más graves que agudos)
    pulso = np.exp(-np.arange(int(SR * 0.004)) / (SR * 0.0012))
    return np.convolve(x, pulso)[:n]


def formantes(x, f1, f2, f3, anchos=(0.5, 0.35, 0.25), pesos=(1.0, 0.6, 0.25), bloque=1024):
    """Filtra por tres resonancias de la garganta que se mueven con el tiempo (para pasar de "a" a "o")."""
    n = len(x)
    salida = np.zeros(n)
    for fk, ancho, peso in zip((f1, f2, f3), anchos, pesos):
        zi = None
        for a0 in range(0, n, bloque):
            b0 = min(n, a0 + bloque)
            c = float(np.mean(fk[a0:b0]))
            sos = butter(2, [c * (1 - ancho / 2), min(c * (1 + ancho / 2), SR / 2 - 100)], btype="band", fs=SR, output="sos")
            if zi is None:
                zi = sosfilt_zi(sos) * 0
            y, zi = sosfilt(sos, x[a0:b0], zi=zi)
            salida[a0:b0] += y * peso
    return salida


def voz_leon(dur, tono, vocal, fritura=0.3, aire=0.25, sucio=1.8):
    """tono: [(pos, Hz)]; vocal: [(pos, 0..1)] de "a" (0) a "u" (1)."""
    n = int(SR * dur)
    frec = envolvente(n, tono) * jitter(n, 0.03, 800)
    v = envolvente(n, vocal)
    f1 = 750 - 380 * v           # "a" -> "u": la primera resonancia baja
    f2 = 1250 - 500 * v
    f3 = np.full(n, 2600.0)
    glotis = pulsos_glotis(frec, n, fritura)
    soplo = ruido(dur) * aire * 0.08
    x = formantes(glotis + soplo, f1, f2, f3)
    x = x + paso_bajo(glotis, 300) * 0.5          # el cuerpo grave del pecho
    return saturar(x / (np.max(np.abs(x)) + 1e-9), sucio)


def rugido():
    partes = []
    # un par de gemidos graves para coger aire
    for d, f in ((0.45, 95), (0.5, 105)):
        g = voz_leon(d, [(0, f * 0.9), (0.5, f * 1.15), (1, f * 0.85)], [(0, 0.8), (1, 1.0)], 0.35, 0.4)
        partes += [g * envolvente(len(g), [(0, 0), (0.3, 0.55), (1, 0)]), np.zeros(int(SR * 0.25))]
    # el rugido: sube con fuerza, "aaaooo", se rompe y cae
    d = 2.0
    r = voz_leon(d, [(0, 120), (0.22, 235), (0.5, 210), (0.8, 150), (1, 95)], [(0, 0.0), (0.4, 0.15), (1, 0.9)], 0.35, 0.3, 2.4)
    partes += [r * envolvente(len(r), [(0, 0), (0.12, 1), (0.6, 0.95), (1, 0)]), np.zeros(int(SR * 0.3))]
    # la serie de gruñidos: "uh, uh, uh..." cada vez más flojos y separados
    for i, hueco in enumerate((0.38, 0.45, 0.55, 0.68, 0.85)):
        d = 0.24 - i * 0.01
        f = 125 - i * 6
        g = voz_leon(d, [(0, f * 1.1), (1, f * 0.8)], [(0, 0.6), (1, 1.0)], 0.4, 0.6, 2.0)
        partes += [g * envolvente(len(g), [(0, 0), (0.12, 0.9 - i * 0.13), (1, 0)]), np.zeros(int(SR * hueco))]
    return fundido(normalizar(np.concatenate(partes), 0.95))


def grunido():
    d = 1.4
    g = voz_leon(d, [(0, 70), (0.4, 82), (1, 62)], [(0, 0.85), (1, 1.0)], 0.5, 0.5, 2.5)
    am = 0.6 + 0.4 * np.abs(np.sin(2 * np.pi * 6.5 * t(d)))     # ronroneo amenazante
    return fundido(normalizar(g * am * envolvente(len(g), [(0, 0), (0.15, 1), (0.8, 0.8), (1, 0)]), 0.85))


def herido():
    d = 0.55
    g = voz_leon(d, [(0, 220), (0.25, 280), (1, 160)], [(0, 0.1), (1, 0.6)], 0.3, 0.4, 2.2)
    return fundido(normalizar(g * envolvente(len(g), [(0, 0), (0.08, 1), (1, 0)]), 0.9))


def muerte():
    d = 2.8
    g = voz_leon(d, [(0, 170), (0.2, 210), (0.6, 120), (1, 55)], [(0, 0.1), (1, 1.0)], 0.45, 0.5, 2.2)
    estertor = voz_leon(1.2, [(0, 70), (1, 45)], [(0, 1), (1, 1)], 0.6, 0.7, 2.0)
    return fundido(normalizar(np.concatenate([g * envolvente(len(g), [(0, 0), (0.1, 1), (0.7, 0.6), (1, 0.1)]),
                                              estertor * envolvente(len(estertor), [(0, 0.4), (1, 0)])]), 0.9))


def tajo():
    dur = 0.35
    n = int(SR * dur)
    r = ruido(dur)
    # silbido que barre de grave a agudo y vuelve
    trozos = []
    pasos = 12
    for k in range(pasos):
        a, b = k * n // pasos, (k + 1) * n // pasos
        centro = 500 + 3500 * np.sin(np.pi * k / (pasos - 1))
        trozos.append(banda(r, max(150, centro * 0.6), min(15000, centro * 1.6))[a:b])
    x = np.concatenate(trozos) * envolvente(n, [(0, 0), (0.35, 1), (1, 0)])
    return fundido(normalizar(x, 0.8))


SONIDOS = {"leon_rugido": rugido, "leon_grunido": grunido, "leon_herido": herido, "leon_muerte": muerte, "leon_tajo": tajo}

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
