"""Sintetiza los sonidos de la armónica (lengüeta: onda rica en armónicos impares + aire, con vibrato).

Uso: python sintetizar_armonica.py <carpeta_salida>
Luego convierte a OGG mono:  ffmpeg -i armonica_nota.wav -ac 1 -c:a libvorbis -q:a 5 armonica_nota.ogg
y copia los .ogg a forge-1.20.1-47.4.10-mdk/src/main/resources/assets/yonga/sounds/
Necesita: numpy y scipy."""
import sys
import numpy as np
from scipy.signal import butter, lfilter

SR = 44100
rng = np.random.default_rng(53)

def lengueta(frecuencias, vibrato=0.0):
    """Lengüeta que vibra a una frecuencia que puede cambiar en el tiempo (para los 'bends' del blues)."""
    n = len(frecuencias); t = np.arange(n) / SR
    f = frecuencias * (1 + vibrato * np.sin(2 * np.pi * 5.5 * t))
    fase = 2 * np.pi * np.cumsum(f) / SR
    s = np.zeros(n)
    for k, amp in ((1, 1.0), (2, 0.45), (3, 0.6), (4, 0.2), (5, 0.35), (7, 0.18), (9, 0.1)):
        s += amp * np.sin(k * fase)
    s = np.tanh(1.3 * s)                                         # la lengüeta "raspa" un poco
    b, a = butter(2, [1200, 3500], btype="band", fs=SR)
    s += 0.6 * lfilter(b, a, s)                                  # resonancia de las placas (nasal)
    b, a = butter(2, [800, 5000], btype="band", fs=SR)
    s += 0.06 * lfilter(b, a, rng.uniform(-1, 1, n))             # aire
    return s

def envolvente(n, ataque=0.03, caida=0.15):
    e = np.ones(n)
    a = int(ataque * SR); c = int(caida * SR)
    e[:a] = np.linspace(0, 1, a) ** 0.7
    e[-c:] *= np.linspace(1, 0, c)
    return e

def nota(freq, dur):
    n = int(dur * SR)
    return lengueta(np.full(n, freq), vibrato=0.004) * envolvente(n)

def quejido(freq, dur):
    """Nota que entra 'bajada' (un tono por debajo), sube a su sitio y cae al final: el típico quejido del blues."""
    n = int(dur * SR); t = np.arange(n) / dur / SR
    semitonos = -2 * np.exp(-t * 8) - 1.5 * np.clip((t - 0.65) / 0.35, 0, 1) ** 2
    return lengueta(freq * 2 ** (semitonos / 12), vibrato=0.008) * envolvente(n, 0.02, 0.25)

def lamento(freq, dur):
    """Acorde de armónica (tónica, tercera menor, quinta) que se queja hacia abajo, con trémolo lento."""
    n = int(dur * SR); t = np.arange(n) / SR
    caida = -3 * np.clip((t / dur - 0.3) / 0.7, 0, 1) ** 1.5
    s = sum(lengueta(freq * r * 2 ** (caida / 12), vibrato=0.01) for r in (1.0, 1.1892, 1.4983))
    s *= 1 - 0.35 * (0.5 + 0.5 * np.sin(2 * np.pi * 4 * t))   # trémolo (la mano abriendo y cerrando)
    return s * envolvente(n, 0.08, 0.6)

def terminar(s):
    return 0.9 * s / np.max(np.abs(s))

def guardar(ruta, s):
    from scipy.io import wavfile
    wavfile.write(ruta, SR, (s * 32767).astype(np.int16))

if __name__ == "__main__":
    salida = sys.argv[1]
    LA4 = 440.0  # nota base: La (A4). El juego la sube/baja hasta una octava.
    n = terminar(nota(LA4, 0.9)); guardar(f"{salida}/armonica_nota.wav", n)
    q = terminar(quejido(LA4, 0.9)); guardar(f"{salida}/armonica_quejido.wav", q)
    l = terminar(lamento(LA4 / 2, 2.6)); guardar(f"{salida}/armonica_lamento.wav", l)

    def tono(s, semitonos):
        f = 2 ** (semitonos / 12)
        return np.interp(np.arange(0, len(s) - 1, f), np.arange(len(s)), s)
    demo = np.zeros(int(7.5 * SR)); p = 0
    for st, x in ((0, n), (3, n), (5, n), (-5, q), (0, n), (3, q), (-2, n), (0, q)):
        y = tono(x, st)[: int(0.55 * SR)]
        y[-500:] *= np.linspace(1, 0, 500)
        demo[p:p + len(y)] += y; p += int(0.45 * SR)
    p += int(0.4 * SR); demo[p:p + len(l)] += l[: len(demo) - p]
    guardar(f"{salida}/demo_armonica.wav", 0.9 * demo / np.max(np.abs(demo)))
    print("ok")
