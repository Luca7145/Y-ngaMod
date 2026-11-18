"""Sintetiza el sonido de la espada-keytar: un "lead" de sintetizador (dos dientes de sierra desafinados + filtro).

Uso: python sintetizar_keytar.py <carpeta_salida>
Luego convierte a OGG mono:  ffmpeg -i keytar_nota.wav -ac 1 -c:a libvorbis -q:a 5 keytar_nota.ogg
y copia el .ogg a forge-1.20.1-47.4.10-mdk/src/main/resources/assets/yonga/sounds/
Necesita: numpy y scipy."""
import sys
import numpy as np
from scipy.io import wavfile

SR = 44100

# Armónicos solo hasta 8 kHz: así, aunque el juego suba la nota una octava (x2), sigue por debajo de 16 kHz
# y no aparecen distorsiones (aliasing)
LIMITE_ARMONICOS = 8000

def sierra(freq, t):
    # Diente de sierra suavizado (suma de armónicos) para que no chirríe
    s = np.zeros_like(t)
    k = 1
    while freq * k <= LIMITE_ARMONICOS:
        s += np.sin(2 * np.pi * freq * k * t) / k
        k += 1
    return s

def paso_bajo_variable(s, cortes):
    """Filtro paso bajo (dos etapas) cuyo corte cambia muestra a muestra, SIN cortes ni chasquidos:
    el filtro recuerda su estado de una muestra a la siguiente."""
    alfa = 1.0 - np.exp(-2 * np.pi * cortes / SR)
    salida = np.zeros_like(s)
    a = b = 0.0
    for i in range(len(s)):
        a += alfa[i] * (s[i] - a)
        b += alfa[i] * (a - b)
        salida[i] = b
    return salida

def nota_lead(freq, dur):
    t = np.arange(int(SR * dur)) / SR
    # Dos osciladores ligeramente desafinados + uno una octava abajo: sonido "gordo" de sintetizador
    s = sierra(freq * 0.997, t) + sierra(freq * 1.003, t) + 0.5 * sierra(freq / 2, t)
    # Filtro que se abre al principio y se va cerrando (el "wah" típico), continuo
    salida = paso_bajo_variable(s, 900 + 4200 * np.exp(-t * 5))
    # Envolvente: ataque rápido, caída a un nivel medio y final suave
    env = np.minimum(1.0, t / 0.008) * (0.55 + 0.45 * np.exp(-t * 6))
    fin = int(0.25 * SR)
    env[-fin:] *= np.linspace(1, 0, fin)
    salida *= env
    return 0.9 * salida / np.max(np.abs(salida))

if __name__ == "__main__":
    carpeta = sys.argv[1]
    DO4 = 261.63   # nota base: do central. El juego la sube/baja una octava.
    nota = nota_lead(DO4, 0.9)
    wavfile.write(f"{carpeta}/keytar_nota.wav", SR, (nota * 32767).astype(np.int16))

    # Demo: una melodía tocada como lo haría el juego (cambiando la velocidad de la misma nota)
    def tono(s, semitonos):
        idx = np.arange(0, len(s) - 1, 2 ** (semitonos / 12))
        return np.interp(idx, np.arange(len(s)), s)
    melodia = [0, 4, 7, 12, 7, 4, 0, -5, -1, 2, 7, 11, 12]
    paso = int(0.18 * SR)
    demo = np.zeros(paso * len(melodia) + SR)
    for i, st in enumerate(melodia):
        x = tono(nota, st)[: int(0.5 * SR)]
        x = x * np.linspace(1, 0, len(x)) ** 0.7
        demo[i * paso: i * paso + len(x)] += x
    wavfile.write(f"{carpeta}/demo_keytar.wav", SR, (0.9 * demo / np.max(np.abs(demo)) * 32767).astype(np.int16))
    print("ok")
