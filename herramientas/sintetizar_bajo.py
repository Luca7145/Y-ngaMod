"""Sintetiza los sonidos del bajo-hacha (cuerda pulsada Karplus-Strong).

Uso: python sintetizar_bajo.py <carpeta_salida>
Luego convierte a OGG mono:  ffmpeg -i bajo_nota.wav -ac 1 -c:a libvorbis -q:a 5 bajo_nota.ogg
y copia los .ogg a forge-1.20.1-47.4.10-mdk/src/main/resources/assets/yonga/sounds/
Necesita: numpy y scipy."""
import sys
import numpy as np
from scipy.signal import butter, lfilter

SR = 44100
rng = np.random.default_rng(7)

def cuerda(freq, dur, brillo=0.5, decaimiento=0.996):
    """Cuerda pulsada: un ruido inicial que da vueltas en un bucle con un filtro que lo va apagando."""
    n = int(SR * dur)
    periodo = int(round(SR / freq))
    # Ataque: ruido filtrado (la púa/el dedo); menos brillo = más grave y redondo
    b, a = butter(2, 200 + 3000 * brillo, fs=SR)
    buf = lfilter(b, a, rng.uniform(-1, 1, periodo))
    salida = np.zeros(n)
    for i in range(n):
        j = i % periodo
        salida[i] = buf[j]
        buf[j] = decaimiento * 0.5 * (buf[j] + buf[(j + 1) % periodo])
    return salida

def bajo(freq, dur):
    s = cuerda(freq, dur, brillo=0.35, decaimiento=0.9985)
    # Cuerpo grave: senoidal en la fundamental que se apaga
    t = np.arange(len(s)) / SR
    s = s + 0.6 * np.sin(2 * np.pi * freq * t) * np.exp(-t * 2.2)
    # Ataque de la púa
    ataque = rng.uniform(-1, 1, int(0.012 * SR)) * np.linspace(1, 0, int(0.012 * SR))
    b, a = butter(2, 1800, fs=SR)
    s[:len(ataque)] += 0.35 * lfilter(b, a, ataque)
    # Un poco de saturación (ampli de bajo) y paso bajo para que sea cálido
    s = np.tanh(1.8 * s)
    b, a = butter(4, 1400, fs=SR)
    s = lfilter(b, a, s)
    return s

def acorde(raiz, dur):
    # Acorde de potencia: raíz + quinta + octava, distorsionado
    notas = [raiz, raiz * 1.4983, raiz * 2.0]
    s = sum(cuerda(f, dur, brillo=0.8, decaimiento=0.9988) for f in notas)
    t = np.arange(len(s)) / SR
    s = s + 0.8 * np.sin(2 * np.pi * raiz * t) * np.exp(-t * 1.5)            # cuerpo grave
    s = s + 1.2 * np.sin(2 * np.pi * 45 * t) * np.exp(-t * 9)                 # golpe ("boom")
    s = np.tanh(4.0 * s)                                                     # distorsión
    b, a = butter(4, 3500, fs=SR)
    return lfilter(b, a, s)

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
    RE2 = 73.42  # nota base: Re grave (D2). El juego la sube/baja hasta una octava.
    nota = terminar(bajo(RE2, 1.6))
    guardar(f"{salida}/bajo_nota.wav", nota)
    guardar(f"{salida}/bajo_acorde.wav", terminar(acorde(RE2, 2.0), 0.5))

    # Demo de cómo sonaría tocando: un riff cambiando el tono como hace Minecraft (cambiando la velocidad)
    def tono(s, semitonos):
        f = 2 ** (semitonos / 12)
        idx = np.arange(0, len(s) - 1, f)
        return np.interp(idx, np.arange(len(s)), s)
    riff = [0, 0, 12, 0, 10, 0, 7, 5, 0, 0, 12, 0, 10, 12, 15, 12]
    paso = int(0.2 * SR)
    demo = np.zeros(paso * len(riff) + 2 * SR)
    for i, st in enumerate(riff):
        x = tono(nota, st - 12 + 12)[: int(0.6 * SR)]
        x = x * np.linspace(1, 0.0, len(x)) ** 0.5
        demo[i * paso: i * paso + len(x)] += x
    acor = terminar(acorde(RE2, 2.0), 0.5)
    inicio = paso * len(riff)
    demo[inicio: inicio + len(acor)] += acor[: len(demo) - inicio]
    guardar(f"{salida}/demo_riff.wav", 0.9 * demo / np.max(np.abs(demo)))
    print("ok")
