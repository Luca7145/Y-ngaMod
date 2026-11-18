"""Sintetiza los sonidos de la flauta (tono casi puro + soplido, con vibrato) y el encantamiento al domar.

Uso: python sintetizar_flauta.py <carpeta_salida>
Luego convierte a OGG mono:  ffmpeg -i flauta_nota.wav -ac 1 -c:a libvorbis -q:a 5 flauta_nota.ogg
y copia los .ogg a forge-1.20.1-47.4.10-mdk/src/main/resources/assets/yonga/sounds/
Necesita: numpy y scipy."""
import sys
import numpy as np
from scipy.signal import butter, lfilter

SR = 44100
rng = np.random.default_rng(61)

def flauta(frecuencias, vibrato=0.006):
    n = len(frecuencias); t = np.arange(n) / SR
    retraso = np.clip(t / 0.25, 0, 1)                            # el vibrato entra poco a poco
    f = frecuencias * (1 + vibrato * retraso * np.sin(2 * np.pi * 5.0 * t))
    fase = 2 * np.pi * np.cumsum(f) / SR
    s = np.sin(fase) + 0.25 * np.sin(2 * fase) + 0.08 * np.sin(3 * fase)
    b, a = butter(2, [1500, 7000], btype="band", fs=SR)
    soplo = lfilter(b, a, rng.uniform(-1, 1, n))
    chiff = np.exp(-t * 40)                                      # el "chuf" del ataque
    return s + 0.12 * soplo + 0.5 * soplo * chiff

def envolvente(n, ataque=0.05, caida=0.2):
    e = np.ones(n); a = min(int(ataque * SR), n // 3); c = min(int(caida * SR), n // 2)
    e[:a] = np.linspace(0, 1, a) ** 0.6
    e[-c:] *= np.linspace(1, 0, c) ** 1.5
    return e

def nota(freq, dur):
    n = int(dur * SR)
    return flauta(np.full(n, freq)) * envolvente(n)

def encanto(raiz):
    """Trino rápido que sube (arpegio mayor) y acaba en una nota larga: el mob queda encantado."""
    pasos = [0, 4, 7, 12, 7, 12, 16, 19, 24]
    total = np.zeros(int(2.2 * SR)); p = 0
    for i, st in enumerate(pasos):
        largo = 0.09 if i < len(pasos) - 1 else 1.4
        x = nota(raiz * 2 ** (st / 12), largo + 0.06)
        total[p:p + len(x)] += x[: len(total) - p]
        p += int(largo * SR)
    return total

def terminar(s):
    return 0.85 * s / np.max(np.abs(s))

def guardar(ruta, s):
    from scipy.io import wavfile
    wavfile.write(ruta, SR, (s * 32767).astype(np.int16))

if __name__ == "__main__":
    salida = sys.argv[1]
    MI5 = 659.25  # nota base: Mi (E5). El juego la sube/baja hasta una octava.
    n = terminar(nota(MI5, 1.0)); guardar(f"{salida}/flauta_nota.wav", n)
    e = terminar(encanto(MI5 / 2)); guardar(f"{salida}/flauta_encanto.wav", e)
    def tono(s, semitonos):
        f = 2 ** (semitonos / 12)
        return np.interp(np.arange(0, len(s) - 1, f), np.arange(len(s)), s)
    demo = np.zeros(int(5.5 * SR)); p = 0
    for st in (0, 2, 4, 7, 4, 2, 0):
        y = tono(n, st)[: int(0.4 * SR)]; y[-800:] *= np.linspace(1, 0, 800)
        demo[p:p + len(y)] += y; p += int(0.35 * SR)
    p += int(0.3 * SR); demo[p:p + len(e)] += e[: len(demo) - p]
    guardar(f"{salida}/demo_flauta.wav", 0.9 * demo / np.max(np.abs(demo)))
    print("ok")
