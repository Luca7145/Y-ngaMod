"""Sonidos del Astado (sintetizados desde cero, sin muestras):
  astado_respiracion  respiración pesada y rasposa (acecho y de pie)
  astado_rama         rama que se parte (acecho)
  astado_paso         pisada pesada sobre hojas
  astado_grunido      gruñido grave
  astado_chillido     el chillido al empezar la caza (agudo, áspero, con vibrato)
  astado_caza         jadeo rápido con gruñidos mientras caza
  astado_mordisco     crujido de mordisco
  astado_golpe        golpe seco (embestida)
  astado_herido       quejido corto
  astado_muerte       aullido largo que se apaga
  astado_susto        el chillido del susto (muy fuerte y sucio)
Uso: python3 sintetizar_astado.py <carpeta_salida>  (deja los .wav y, si hay ffmpeg, los .ogg mono)"""
import os
import subprocess
import sys

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, sosfilt

SR = 44100
rng = np.random.default_rng(7)


def t(seg):
    return np.arange(int(SR * seg)) / SR


def ruido(seg):
    return rng.standard_normal(int(SR * seg))


def banda(x, lo, hi, orden=4):
    return sosfilt(butter(orden, [lo, hi], btype="band", fs=SR, output="sos"), x)


def paso_bajo(x, f, orden=4):
    return sosfilt(butter(orden, f, btype="low", fs=SR, output="sos"), x)


def paso_alto(x, f, orden=2):
    return sosfilt(butter(orden, f, btype="high", fs=SR, output="sos"), x)


def envolvente(n, puntos):
    """puntos: lista de (posición 0..1, valor)."""
    xs = np.linspace(0, 1, n)
    return np.interp(xs, [p for p, _ in puntos], [v for _, v in puntos])


def sierra(frec, tt):
    fase = np.cumsum(frec / SR) if np.ndim(frec) else frec * tt
    return 2 * (fase % 1.0) - 1


def saturar(x, k):
    return np.tanh(k * x) / np.tanh(k)


def jitter(n, cantidad, suavizado=400):
    j = rng.standard_normal(n // suavizado + 2)
    return 1 + cantidad * np.interp(np.arange(n), np.arange(len(j)) * suavizado, j)


def normalizar(x, pico=0.9):
    x = x - np.mean(x)
    return x / (np.max(np.abs(x)) + 1e-9) * pico


def fundido(x, ms=8):
    k = int(SR * ms / 1000)
    x[:k] *= np.linspace(0, 1, k)
    x[-k:] *= np.linspace(1, 0, k)
    return x


# ---------------- SONIDOS ----------------

def respiracion():
    partes = []
    for dur, sube in ((1.1, True), (1.5, False)):
        n = int(SR * dur)
        r = ruido(dur)
        aire = banda(r, 350, 1800) * 0.7 + banda(r, 1800, 4200) * 0.25
        env = envolvente(n, [(0, 0), (0.25 if sube else 0.1, 1), (0.8, 0.7), (1, 0)])
        tt = t(dur)
        ronquera = sierra(55 * jitter(n, 0.08) * (1.1 if sube else 0.9), tt)
        ronquera = paso_bajo(ronquera, 500) * (0.5 if sube else 0.8)
        crujido = (rng.random(n) < 0.0009) * rng.standard_normal(n) * 3   # flemas
        partes.append((aire + ronquera + banda(crujido, 400, 3000)) * env)
        partes.append(np.zeros(int(SR * 0.12)))
    return normalizar(np.concatenate(partes), 0.8)


def rama():
    dur = 0.45
    n = int(SR * dur)
    x = np.zeros(n)
    for inicio, fuerza in ((0.0, 1.0), (0.035, 0.6), (0.06, 0.45), (0.1, 0.3), (0.16, 0.15)):
        i = int(SR * inicio)
        k = int(SR * 0.012)
        x[i:i + k] += rng.standard_normal(k) * np.exp(-np.linspace(0, 6, k)) * fuerza
    crujir = ruido(dur) * envolvente(n, [(0, 0.3), (0.4, 0.1), (1, 0)]) * 0.15
    x = banda(x, 900, 7000) + banda(crujir, 1500, 6000)
    return fundido(normalizar(x, 0.9))


def paso():
    dur = 0.5
    n = int(SR * dur)
    tt = t(dur)
    golpe = np.sin(2 * np.pi * (55 * np.exp(-tt * 6)) * tt) * np.exp(-tt * 14)
    hojas = banda(ruido(dur), 1200, 6000) * envolvente(n, [(0, 0), (0.03, 1), (0.35, 0.3), (1, 0)]) * 0.35
    ramitas = (rng.random(n) < 0.002) * rng.standard_normal(n) * 2
    ramitas = banda(ramitas, 2000, 8000) * envolvente(n, [(0, 1), (0.5, 0.2), (1, 0)])
    return fundido(normalizar(golpe * 1.2 + hojas + ramitas * 0.5, 0.9))


def grunido(dur=1.6, base=62, sucio=4.0):
    n = int(SR * dur)
    tt = t(dur)
    frec = base * jitter(n, 0.12, 300) * envolvente(n, [(0, 0.9), (0.3, 1.05), (1, 0.85)])
    voz = sierra(frec, tt) + 0.6 * sierra(frec * 1.5, tt) + 0.4 * sierra(frec * 0.5, tt)
    am = 0.6 + 0.4 * np.abs(np.sin(2 * np.pi * 9 * tt * jitter(n, 0.2)))   # vibración de garganta
    voz = saturar(paso_bajo(voz * am, 900), sucio)
    aire = banda(ruido(dur), 300, 1500) * 0.35
    env = envolvente(n, [(0, 0), (0.12, 1), (0.75, 0.8), (1, 0)])
    return fundido(normalizar((voz + aire) * env, 0.9))


def chillido(dur=2.6, desde=380, hasta=1250, sucio=6.0):
    n = int(SR * dur)
    tt = t(dur)
    curva = envolvente(n, [(0, desde), (0.18, hasta), (0.7, hasta * 0.92), (1, hasta * 0.6)])
    vibrato = 1 + 0.035 * np.sin(2 * np.pi * 7.5 * tt) + 0.02 * (jitter(n, 1.0, 200) - 1)
    voces = sum(sierra(curva * vibrato * d, tt) for d in (1.0, 1.013, 0.987, 2.01))
    voces = saturar(banda(voces, 250, 6500), sucio)
    garganta = banda(ruido(dur), 1500, 7000) * 0.5 * (0.6 + 0.4 * np.sin(2 * np.pi * 23 * tt))
    sub = saturar(sierra(curva * 0.25, tt), 3) * 0.4
    env = envolvente(n, [(0, 0), (0.05, 1), (0.65, 0.9), (1, 0)])
    return fundido(normalizar((voces + garganta + paso_bajo(sub, 400)) * env, 0.95))


def caza():
    partes = []
    for i in range(6):   # jadeos rápidos y entrecortados
        dur = 0.16 + rng.random() * 0.12
        n = int(SR * dur)
        r = banda(ruido(dur), 500, 3500)
        env = envolvente(n, [(0, 0), (0.2, 1), (1, 0)])
        tt = t(dur)
        voz = paso_bajo(sierra(80 * jitter(n, 0.2, 200) * (1.3 if i % 2 else 1), tt), 700) * 0.6
        partes.append((r + voz) * env * (0.7 + 0.3 * rng.random()))
        partes.append(np.zeros(int(SR * (0.05 + rng.random() * 0.06))))
    jadeo = np.concatenate(partes)
    final = grunido(0.9, 70, 5.0) * 0.8
    return fundido(normalizar(np.concatenate([jadeo, final]), 0.9))


def mordisco():
    dur = 0.4
    n = int(SR * dur)
    x = np.zeros(n)
    for inicio, fuerza in ((0.0, 1.0), (0.03, 0.8), (0.07, 0.6), (0.12, 0.35), (0.2, 0.2)):
        i = int(SR * inicio)
        k = int(SR * 0.02)
        x[i:i + k] += rng.standard_normal(k) * np.exp(-np.linspace(0, 5, k)) * fuerza
    tt = t(dur)
    golpe = np.sin(2 * np.pi * 120 * tt) * np.exp(-tt * 30)
    humedo = banda(ruido(dur), 300, 1500) * envolvente(n, [(0, 0), (0.05, 0.5), (0.5, 0.15), (1, 0)])
    return fundido(normalizar(banda(x, 600, 8000) + golpe * 0.8 + humedo * 0.4, 0.95))


def golpe():
    dur = 0.6
    n = int(SR * dur)
    tt = t(dur)
    cuerpo = np.sin(2 * np.pi * (70 * np.exp(-tt * 4) + 30) * tt) * np.exp(-tt * 9)
    chasquido = paso_alto(ruido(dur), 1500) * np.exp(-tt * 60) * 0.6
    madera = banda(ruido(dur), 200, 900) * np.exp(-tt * 15) * 0.5
    return fundido(normalizar(saturar(cuerpo * 1.4 + chasquido + madera, 2.0), 0.95))


def herido():
    return chillido(0.7, 600, 1000, 5.0) * 0.8 + np.pad(grunido(0.7, 85, 4.0), (0, 0)) * 0.5


def muerte():
    a = chillido(2.2, 900, 1100, 5.0)
    n = len(a)
    a = a * envolvente(n, [(0, 1), (0.5, 0.8), (1, 0.4)])
    tt = t(2.2)
    caida = envolvente(n, [(0, 1), (1, 0.35)])
    estertor = grunido(2.2, 55, 6.0) * caida
    cola = grunido(1.4, 40, 3.0) * envolvente(int(SR * 1.4), [(0, 0.7), (1, 0)])
    return fundido(normalizar(np.concatenate([a * 0.7 + estertor * 0.6, cola]), 0.9))


def susto():
    a = chillido(1.5, 700, 1700, 9.0)
    n = len(a)
    ruido_fuerte = banda(ruido(1.5), 800, 9000) * envolvente(n, [(0, 1), (0.3, 0.6), (1, 0)])
    golpe_inicial = np.zeros(n)
    k = int(SR * 0.08)
    golpe_inicial[:k] = saturar(rng.standard_normal(k), 3) * np.linspace(1, 0, k)
    return fundido(normalizar(saturar(a + ruido_fuerte * 0.6 + golpe_inicial, 3.0), 0.98), 2)


SONIDOS = {
    "astado_respiracion": respiracion, "astado_rama": rama, "astado_paso": paso, "astado_grunido": grunido,
    "astado_chillido": chillido, "astado_caza": caza, "astado_mordisco": mordisco, "astado_golpe": golpe,
    "astado_herido": herido, "astado_muerte": muerte, "astado_susto": susto,
}

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
