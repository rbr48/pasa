# Cyberpunk High-Energy Electronic Soundtrack Generator (130 BPM)
# Pure Python math + wave synthesis. 44.1kHz, 16-bit Stereo PCM.

import wave
import struct
import math
import random

SAMPLE_RATE = 44100
BPM = 130
BEAT_DUR = 60.0 / BPM
TOTAL_DUR = 36.0  # 36 seconds punchy ad
NUM_SAMPLES = int(SAMPLE_RATE * TOTAL_DUR)

print(f"Generating {TOTAL_DUR}s Cyberpunk soundtrack at {BPM} BPM...")

# Buffers
left = [0.0] * NUM_SAMPLES
right = [0.0] * NUM_SAMPLES

def add_sample(t_idx, l_val, r_val):
    if 0 <= t_idx < NUM_SAMPLES:
        left[t_idx] += l_val
        right[t_idx] += r_val

# 1. KICK DRUM (Heavy punchy 808-style sine pitch-drop)
def kick(start_time, intensity=1.0):
    dur = 0.35
    start_sample = int(start_time * SAMPLE_RATE)
    num_k_samples = int(dur * SAMPLE_RATE)
    for i in range(num_k_samples):
        t = i / SAMPLE_RATE
        # Pitch drops rapidly from 150 Hz to 45 Hz
        f = 45.0 + 105.0 * math.exp(-t * 28.0)
        phase = 2.0 * math.pi * (45.0 * t - (105.0 / 28.0) * math.exp(-t * 28.0))
        env = math.exp(-t * 12.0) * intensity
        # Click transient at start
        click = (math.exp(-t * 300.0) * 0.4) * (random.random() * 2.0 - 1.0)
        val = (math.sin(phase) * 0.8 + click) * env
        add_sample(start_sample + i, val * 0.85, val * 0.85)

# 2. SNARE / CLAP (Burst noise + 220Hz body)
def clap(start_time, intensity=0.8):
    dur = 0.25
    start_sample = int(start_time * SAMPLE_RATE)
    num_c_samples = int(dur * SAMPLE_RATE)
    for i in range(num_c_samples):
        t = i / SAMPLE_RATE
        noise = (random.random() * 2.0 - 1.0)
        tone = math.sin(2.0 * math.pi * 210.0 * t) * 0.3
        env = math.exp(-t * 22.0) * intensity
        # Early reflections for wide stereo clap
        val_l = (noise * 0.7 + tone) * env
        val_r = (noise * 0.7 - tone) * env
        add_sample(start_sample + i, val_l * 0.65, val_r * 0.65)

# 3. HI-HAT (Short metallic noise)
def hihat(start_time, open_hat=False, intensity=0.4):
    dur = 0.18 if open_hat else 0.05
    decay = 18.0 if open_hat else 80.0
    start_sample = int(start_time * SAMPLE_RATE)
    num_h_samples = int(dur * SAMPLE_RATE)
    for i in range(num_h_samples):
        t = i / SAMPLE_RATE
        # High-pass metallic noise
        noise = (random.random() * 2.0 - 1.0) - (random.random() * 0.5)
        env = math.exp(-t * decay) * intensity
        add_sample(start_sample + i, noise * env * 0.3, noise * env * 0.3)

# 4. ROLLING SAWTOOTH BASSLINE (D-minor: D1=36.7Hz, F1=43.6Hz, G1=49.0Hz, A1=55.0Hz, C2=65.4Hz)
def bass_note(start_time, dur, freq, intensity=0.55):
    start_sample = int(start_time * SAMPLE_RATE)
    num_b_samples = int(dur * SAMPLE_RATE)
    for i in range(num_b_samples):
        t = i / SAMPLE_RATE
        # Sawtooth wave approximation with 4 harmonics
        saw = 0.0
        for h in range(1, 6):
            saw += (math.sin(2.0 * math.pi * freq * h * t) / h)
        saw *= (2.0 / math.pi)
        # Lowpass filter effect via envelope
        filter_env = math.exp(-t * 8.0)
        amp_env = math.exp(-t * 4.0) * intensity
        val = saw * (0.4 + 0.6 * filter_env) * amp_env
        add_sample(start_sample + i, val * 0.6, val * 0.6)

# 5. SUB IMPACT RISER / SWOOSH
def riser(start_time, dur):
    start_sample = int(start_time * SAMPLE_RATE)
    num_r_samples = int(dur * SAMPLE_RATE)
    for i in range(num_r_samples):
        t = i / SAMPLE_RATE
        progress = t / dur
        f = 80.0 + 800.0 * (progress ** 2)
        val = math.sin(2.0 * math.pi * f * t) * (progress ** 1.5) * 0.3
        add_sample(start_sample + i, val * (1 - progress * 0.3), val * (progress * 0.3 + 0.7))

# 6. SUB BASS DROP BOOM
def boom(start_time):
    dur = 2.0
    start_sample = int(start_time * SAMPLE_RATE)
    num_samples = int(dur * SAMPLE_RATE)
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        f = 70.0 * math.exp(-t * 2.0)
        env = math.exp(-t * 2.5) * 0.8
        val = math.sin(2.0 * math.pi * f * t) * env
        add_sample(start_sample + i, val, val)

# ── COMPOSITION ──
print("Arranging tracks...")

# Intro Build-Up (0 to 6s)
boom(0.0)
for beat in range(0, 13):
    t = beat * BEAT_DUR
    if beat >= 4:
        kick(t, 0.9)
    if beat >= 8:
        hihat(t + BEAT_DUR * 0.5, open_hat=False, intensity=0.35)

riser(3.0, 3.0)

# Main Drop (6s to 33s)
bass_patterns = [
    36.7, 36.7, 43.6, 36.7, 49.0, 36.7, 43.6, 55.0, # D, D, F, D, G, D, F, A
    36.7, 36.7, 43.6, 36.7, 65.4, 55.0, 49.0, 43.6  # D, D, F, D, C, A, G, F
]

drop_start_beat = int(6.0 / BEAT_DUR)
drop_end_beat = int(33.0 / BEAT_DUR)

boom(6.0) # Massive drop hit at 6s

for beat in range(drop_start_beat, drop_end_beat):
    t = beat * BEAT_DUR

    # Kick on every beat (4-on-the-floor)
    kick(t, 1.0)

    # Clap on beats 2 & 4
    if beat % 2 == 1:
        clap(t, 0.85)

    # 16th-note Hi-hats
    for sixteenth in range(4):
        ht = t + sixteenth * (BEAT_DUR / 4.0)
        is_open = (sixteenth == 2)
        hihat(ht, open_hat=is_open, intensity=0.35 if not is_open else 0.5)

    # 16th-note Rolling Bassline
    for sixteenth in range(4):
        bt = t + sixteenth * (BEAT_DUR / 4.0)
        pat_idx = (beat * 4 + sixteenth) % len(bass_patterns)
        freq = bass_patterns[pat_idx]
        bass_note(bt, BEAT_DUR / 4.0, freq, intensity=0.6)

    # Transition risers at 15s and 25s
    if abs(t - 15.0) < 0.2 or abs(t - 25.0) < 0.2:
        riser(t, 2.0)
        boom(t + 2.0)

# Outro Hit (33s to 36s)
boom(33.0)
kick(33.0, 1.1)

# Normalization & Master Limiting
print("Mastering audio...")
max_val = 0.001
for i in range(NUM_SAMPLES):
    max_val = max(max_val, abs(left[i]), abs(right[i]))

norm_factor = 0.92 / max_val
print(f"Peak amplitude: {max_val:.3f}, normalizing by {norm_factor:.3f}")

out_wav = r"E:\Projects\PrivateApp\promo_video_builder\output\soundtrack.wav"
with wave.open(out_wav, "wb") as wf:
    wf.setnchannels(2)
    wf.setsampwidth(2)
    wf.setframerate(SAMPLE_RATE)
    frames = bytearray()
    for i in range(NUM_SAMPLES):
        # Soft saturation / clip limit
        l_sample = max(-0.98, min(0.98, left[i] * norm_factor))
        r_sample = max(-0.98, min(0.98, right[i] * norm_factor))
        l_int = int(l_sample * 32767.0)
        r_int = int(r_sample * 32767.0)
        frames.extend(struct.pack("<hh", l_int, r_int))
    wf.writeframes(frames)

print(f"Soundtrack saved to {out_wav}")
