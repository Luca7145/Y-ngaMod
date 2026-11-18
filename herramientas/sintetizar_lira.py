"""Sintetiza los sonidos del arco-lira (cuerda pulsada suave tipo arpa, y un glissando con zumbido para la flecha sónica).

Uso: python sintetizar_lira.py <carpeta_salida>
Luego convierte a OGG mono:  ffmpeg -i lira_nota.wav -ac 1 -c:a libvorbis -q:a 5 lira_nota.ogg
y copia los .ogg a forge-1.20.1-47.4.10-mdk/src/main/resources/assets/yonga/sounds/
Necesita: numpy y scipy."""
import sys
import numpy as np
from scipy.signal import butter, lfilter

SR = 44100
rng = np.random.default_rng(41)

def cuerda(freq, dur, brillo=0.5, decaimiento=0.998):
    n = int(SR * dur)
    periodo = int(round(SR / freq))
    b, a = butter(2, 300 + 4000 * brillo, fs=SR)
    buf = lfilter(b, a, rng.uniform(-1, 1, periodo))
    salida = np.zeros(n)
    for i in range(n):
        j = i % periodo
        salida[i] = buf[j]
        buf[j] = decaimiento * 0.5 * (buf[j] + buf[(j + 1) % periodo])
    return salida

def nota(freq, dur):
    """Lira: cuerda suave (pulsada con el dedo) + la fundamental redonda que resuena en la caja."""
    s = cuerda(freq, dur, brillo=0.45, decaimiento=0.9985)
    t = np.arange(len(s)) / SR
    s = s / np.max(np.abs(s)) + 0.5 * np.sin(2 * np.pi * freq * t) * np.exp(-t * 2.0)
    b, a = butter(2, 4500, fs=SR)
    return lfilter(b, a, s)

def sonica(raiz, dur):
    """Glissando de arpa hacia arriba + un zumbido que sube (la flecha saliendo)."""
    escala = [0, 2, 4, 7, 9, 12, 14, 16, 19, 21, 24]           # pentatónica
    s = np.zeros(int(dur * SR))
    for i, st in enumerate(escala):
        x = nota(raiz * 2 ** (st / 12), 1.2)
        p = int(i * 0.035 * SR)
        s[p:p + len(x)] += x[: len(s) - p] * (0.6 + 0.4 * i / len(escala))
    n = int(0.9 * SR); t = np.arange(n) / SR
    f = 200 + 1800 * (t / t[-1]) ** 2                          # zumbido que sube de tono
    zumbido = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.sin(np.pi * t / t[-1]) ** 2
    b, a = butter(2, [1500, 6000], btype="band", fs=SR)
    aire = lfilter(b, a, rng.uniform(-1, 1, n)) * np.sin(np.pi * t / t[-1]) ** 2
    p = int(0.2 * SR)
    s[p:p + n] += 0.5 * zumbido + 1.5 * aire
    return s

def terminar(s, fundido=0.3):
    n = int(fundido * SR)
    s[-n:] *= np.linspace(1, 0, n)
    s[: int(0.003 * SR)] *= np.linspace(0, 1, int(0.003 * SR))
    return 0.9 * s / np.max(np.abs(s))

def guardar(ruta, s):
    from scipy.io import wavfile
    wavfile.write(ruta, SR, (s * 32767).astype(np.int16))

if __name__ == "__main__":
    salida = sys.argv[1]
    MI4 = 329.63  # nota base: Mi (E4). El juego la sube/baja hasta una octava.
    n = terminar(nota(MI4, 1.6), 0.4)
    guardar(f"{salida}/lira_nota.wav", n)
    so = terminar(sonica(MI4, 2.0), 0.5)
    guardar(f"{salida}/lira_sonica.wav", so)

    def tono(s, semitonos):
        f = 2 ** (semitonos / 12)
        return np.interp(np.arange(0, len(s) - 1, f), np.arange(len(s)), s)
    demo = np.zeros(int(7.0 * SR))
    for i, st in enumerate([0, 4, 7, 5]):                       # 4 flechas y la sónica
        x = tono(n, st)
        p = int(i * 1.0 * SR); demo[p:p + len(x)] += x[: len(demo) - p]
    p = int(4.2 * SR); demo[p:p + len(so)] += so[: len(demo) - p]
    guardar(f"{salida}/demo_lira.wav", 0.9 * demo / np.max(np.abs(demo)))
    print("ok")
