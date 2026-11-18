"""Sintetiza los sonidos del pico-guitarra (guitarra eléctrica con distorsión, cuerda pulsada Karplus-Strong).

Uso: python sintetizar_guitarra.py <carpeta_salida>
Luego convierte a OGG mono:  ffmpeg -i guitarra_nota.wav -ac 1 -c:a libvorbis -q:a 5 guitarra_nota.ogg
y copia los .ogg a forge-1.20.1-47.4.10-mdk/src/main/resources/assets/yonga/sounds/
Necesita: numpy y scipy."""
import sys
import numpy as np
from scipy.signal import butter, lfilter

SR = 44100
rng = np.random.default_rng(11)

def cuerda(freq, dur, brillo=0.7, decaimiento=0.997):
    """Cuerda pulsada: un ruido inicial que da vueltas en un bucle con un filtro que lo va apagando."""
    n = int(SR * dur)
    periodo = int(round(SR / freq))
    b, a = butter(2, 300 + 5000 * brillo, fs=SR)
    buf = lfilter(b, a, rng.uniform(-1, 1, periodo))
    salida = np.zeros(n)
    for i in range(n):
        j = i % periodo
        salida[i] = buf[j]
        buf[j] = decaimiento * 0.5 * (buf[j] + buf[(j + 1) % periodo])
    return salida

def ampli(s, ganancia, corte):
    """Distorsión de ampli: realza medios, satura y quita lo más chillón. Todo de una vez (sin trozos, sin clics)."""
    b, a = butter(2, [150, 4000], btype="band", fs=SR)
    s = lfilter(b, a, s)
    s = np.tanh(ganancia * s / np.max(np.abs(s)))
    b, a = butter(4, corte, fs=SR)
    return lfilter(b, a, s)

def nota(freq, dur):
    s = cuerda(freq, dur, brillo=0.75, decaimiento=0.9992)
    s = s + 0.5 * cuerda(freq * 2.0, dur, brillo=0.5, decaimiento=0.999)   # algo de octava: más "cantarina"
    # Ataque de la púa
    n = int(0.008 * SR)
    b, a = butter(2, 3000, fs=SR)
    s[:n] += 0.3 * lfilter(b, a, rng.uniform(-1, 1, n)) * np.linspace(1, 0, n)
    return ampli(s, 5.0, 5000)

def acorde(raiz, dur):
    # Acorde de potencia (raíz + quinta + octava) bien distorsionado, con un golpe grave
    notas = [raiz, raiz * 1.4983, raiz * 2.0]
    s = sum(cuerda(f, dur, brillo=0.85, decaimiento=0.9993) for f in notas)
    s = ampli(s, 9.0, 4500)
    t = np.arange(len(s)) / SR
    s = s + 0.9 * np.sin(2 * np.pi * 50 * t) * np.exp(-t * 8)   # golpe ("boom") de la excavación
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
    MI3 = 164.81  # nota base: Mi (E3). El juego la sube/baja hasta una octava.
    n = terminar(nota(MI3, 1.4))
    guardar(f"{salida}/guitarra_nota.wav", n)
    a = terminar(acorde(MI3 / 2, 2.2), 0.6)
    guardar(f"{salida}/guitarra_acorde.wav", a)

    # Demo: un riff cambiando el tono como hace Minecraft (cambiando la velocidad) y al final el acorde
    def tono(s, semitonos):
        f = 2 ** (semitonos / 12)
        idx = np.arange(0, len(s) - 1, f)
        return np.interp(idx, np.arange(len(s)), s)
    riff = [0, 3, 5, 0, 3, 6, 5, 0, 3, 5, 3, 0, 7, 5, 3, 0]
    paso = int(0.18 * SR)
    demo = np.zeros(paso * len(riff) + 3 * SR)
    for i, st in enumerate(riff):
        x = tono(n, st)[: int(0.5 * SR)]
        x = x * np.linspace(1, 0.0, len(x)) ** 0.5
        demo[i * paso: i * paso + len(x)] += x
    inicio = paso * len(riff)
    demo[inicio: inicio + len(a)] += a[: len(demo) - inicio]
    guardar(f"{salida}/demo_guitarra.wav", 0.9 * demo / np.max(np.abs(demo)))
    print("ok")
