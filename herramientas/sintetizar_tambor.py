"""Sintetiza los sonidos del escudo-tambor (tom grave y un redoble que acaba en un golpe fuerte).

Uso: python sintetizar_tambor.py <carpeta_salida>
Luego convierte a OGG mono:  ffmpeg -i tambor_golpe.wav -ac 1 -c:a libvorbis -q:a 5 tambor_golpe.ogg
y copia los .ogg a forge-1.20.1-47.4.10-mdk/src/main/resources/assets/yonga/sounds/
Necesita: numpy y scipy."""
import sys
import numpy as np
from scipy.signal import butter, lfilter

SR = 44100
rng = np.random.default_rng(31)

def tom(freq, dur, fuerza=1.0):
    """Parche de tambor: una senoidal que baja de tono al apagarse + el chasquido de la baqueta."""
    n = int(dur * SR); t = np.arange(n) / SR
    f = freq * (1 + 0.6 * np.exp(-t * 30))                  # el tono cae justo después del golpe
    fase = 2 * np.pi * np.cumsum(f) / SR
    s = np.sin(fase) * np.exp(-t * 5) + 0.3 * np.sin(2.3 * fase) * np.exp(-t * 12)
    b, a = butter(2, [200, 3000], btype="band", fs=SR)
    s += 0.5 * lfilter(b, a, rng.uniform(-1, 1, n)) * np.exp(-t * 60)
    return fuerza * np.tanh(1.5 * s)

def caja(dur, fuerza=1.0):
    """Caja (redoble): ruido brillante con bordón + un poco de cuerpo."""
    n = int(dur * SR); t = np.arange(n) / SR
    b, a = butter(2, [1500, 7000], btype="band", fs=SR)
    s = lfilter(b, a, rng.uniform(-1, 1, n)) * np.exp(-t * 25)
    s += 0.5 * np.sin(2 * np.pi * 190 * t) * np.exp(-t * 30)
    return fuerza * s

def redoble():
    total = np.zeros(int(2.4 * SR))
    golpes = 18
    for i in range(golpes):                                  # cada vez más rápido y más fuerte
        p = int(SR * 0.9 * (1 - (1 - i / golpes) ** 1.6))
        x = caja(0.25, 0.25 + 0.6 * i / golpes)
        total[p:p + len(x)] += x[: len(total) - p]
    p = int(0.95 * SR)
    for f, k in ((55, 1.6), (82, 1.0)):                      # golpe final: bombo + tom
        x = tom(f, 1.4, k)
        total[p:p + len(x)] += x
    x = caja(0.6, 1.2)
    total[p:p + len(x)] += x
    return np.tanh(1.2 * total)

def terminar(s, fundido=0.2):
    n = int(fundido * SR)
    s[-n:] *= np.linspace(1, 0, n)
    s[: int(0.002 * SR)] *= np.linspace(0, 1, int(0.002 * SR))
    return 0.9 * s / np.max(np.abs(s))

def guardar(ruta, s):
    from scipy.io import wavfile
    wavfile.write(ruta, SR, (s * 32767).astype(np.int16))

if __name__ == "__main__":
    salida = sys.argv[1]
    golpe = terminar(tom(98, 0.8))
    guardar(f"{salida}/tambor_golpe.wav", golpe)
    r = terminar(redoble(), 0.4)
    guardar(f"{salida}/tambor_redoble.wav", r)

    # Demo: tres golpes parados (cada uno más agudo, como en el juego) y el redoble
    def tono(s, semitonos):
        f = 2 ** (semitonos / 12)
        return np.interp(np.arange(0, len(s) - 1, f), np.arange(len(s)), s)
    demo = np.zeros(int(5.5 * SR))
    for i in range(3):
        x = tono(golpe, i * 2)
        demo[i * int(0.7 * SR): i * int(0.7 * SR) + len(x)] += x
    p = int(2.1 * SR); demo[p:p + len(r)] += r[: len(demo) - p]
    guardar(f"{salida}/demo_tambor.wav", 0.9 * demo / np.max(np.abs(demo)))
    print("ok")
