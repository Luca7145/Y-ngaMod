"""Sintetiza los sonidos del banjo-pala (cuerda pulsada Karplus-Strong, brillante y corta, con el "tang" del parche).

Uso: python sintetizar_banjo.py <carpeta_salida>
Luego convierte a OGG mono:  ffmpeg -i banjo_nota.wav -ac 1 -c:a libvorbis -q:a 5 banjo_nota.ogg
y copia los .ogg a forge-1.20.1-47.4.10-mdk/src/main/resources/assets/yonga/sounds/
Necesita: numpy y scipy."""
import sys
import numpy as np
from scipy.signal import butter, lfilter

SR = 44100
rng = np.random.default_rng(23)

def cuerda(freq, dur, brillo=0.9, decaimiento=0.994):
    n = int(SR * dur)
    periodo = int(round(SR / freq))
    b, a = butter(2, 300 + 7000 * brillo, fs=SR)
    buf = lfilter(b, a, rng.uniform(-1, 1, periodo))
    salida = np.zeros(n)
    for i in range(n):
        j = i % periodo
        salida[i] = buf[j]
        buf[j] = decaimiento * 0.5 * (buf[j] + buf[(j + 1) % periodo])
    return salida

def parche(s):
    """El parche del banjo: resonancias nasales (~1 y ~2,5 kHz) y nada de graves. Todo de una vez, sin clics."""
    b, a = butter(2, 120, btype="high", fs=SR)
    s = lfilter(b, a, s)
    b1, a1 = butter(2, [800, 1400], btype="band", fs=SR)
    b2, a2 = butter(2, [2000, 3200], btype="band", fs=SR)
    return s + 1.2 * lfilter(b1, a1, s) + 0.8 * lfilter(b2, a2, s)

def nota(freq, dur):
    s = cuerda(freq, dur, brillo=0.95, decaimiento=0.9965)
    t = np.arange(len(s)) / SR
    s = s * np.exp(-t * 2.5)                                     # el banjo se apaga rápido
    n = int(0.004 * SR)
    s[:n] += 0.4 * rng.uniform(-1, 1, n) * np.linspace(1, 0, n)  # uña / púa metálica
    return parche(s)

def punteo(raiz, dur):
    """Un "roll" rápido de banjo (afinación abierta de Sol) que acaba en un rasgueo."""
    acorde = [1.0, 1.2599, 1.4983, 2.0, 2.5198]                 # Sol, Si, Re, Sol, Si
    patron = [0, 2, 4, 1, 3, 4, 2, 4, 0, 3, 4, 1, 2, 4, 3, 4]
    paso = int(0.075 * SR)
    s = np.zeros(int(dur * SR))
    for i, k in enumerate(patron):
        x = nota(raiz * acorde[k], 0.6)
        s[i * paso: i * paso + len(x)] += x[: len(s) - i * paso]
    inicio = len(patron) * paso
    for k, f in enumerate(acorde):                               # rasgueo final
        x = nota(raiz * f, 1.2) * 1.3
        p = inicio + k * int(0.012 * SR)
        s[p: p + len(x)] += x[: len(s) - p]
    return s

def terminar(s, fundido=0.25):
    n = int(fundido * SR)
    s[-n:] *= np.linspace(1, 0, n)
    s[: int(0.003 * SR)] *= np.linspace(0, 1, int(0.003 * SR))
    return 0.9 * s / np.max(np.abs(s))

def guardar(ruta, s):
    from scipy.io import wavfile
    wavfile.write(ruta, SR, (s * 32767).astype(np.int16))

if __name__ == "__main__":
    salida = sys.argv[1]
    SOL3 = 196.00  # nota base: Sol (G3). El juego la sube/baja hasta una octava.
    n = terminar(nota(SOL3, 1.0))
    guardar(f"{salida}/banjo_nota.wav", n)
    p = terminar(punteo(SOL3, 2.6), 0.5)
    guardar(f"{salida}/banjo_punteo.wav", p)

    def tono(s, semitonos):
        f = 2 ** (semitonos / 12)
        idx = np.arange(0, len(s) - 1, f)
        return np.interp(idx, np.arange(len(s)), s)
    melodia = [0, 4, 7, 12, 7, 4, 0, 2, 4, 2, 0, -5, 0, 4, 7, 7]
    paso = int(0.17 * SR)
    demo = np.zeros(paso * len(melodia) + 3 * SR)
    for i, st in enumerate(melodia):
        x = tono(n, st)[: int(0.5 * SR)]
        x = x * np.linspace(1, 0.0, len(x)) ** 0.5
        demo[i * paso: i * paso + len(x)] += x
    inicio = paso * len(melodia)
    demo[inicio: inicio + len(p)] += p[: len(demo) - inicio]
    guardar(f"{salida}/demo_banjo.wav", 0.9 * demo / np.max(np.abs(demo)))
    print("ok")
