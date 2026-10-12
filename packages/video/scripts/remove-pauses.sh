#!/usr/bin/env bash
# Taglia le pause (silenzi) da un video con ffmpeg: rileva i silenzi con
# silencedetect e ricompone solo i tratti parlati.
# Uso: remove-pauses.sh input.mp4 [output.mp4] [soglia_dB] [durata_min_s] [margine_s]
set -euo pipefail

IN="$1"
OUT="${2:-${IN%.*}-cut.mp4}"
NOISE="${3:--35dB}"   # sotto questa soglia è silenzio
MIN="${4:-0.5}"       # pause più corte di così restano
PAD="${5:-0.1}"       # respiro lasciato prima/dopo ogni frase

DUR=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$IN")

# Elenco "inizio fine" dei silenzi
SIL=$(ffmpeg -hide_banner -nostats -i "$IN" -af "silencedetect=noise=${NOISE}:d=${MIN}" -f null - 2>&1 |
  awk '/silence_start/{s=$NF} /silence_end/{print s, $5}')

# Inverte i silenzi in segmenti da tenere, con margine
SEGS=$(echo "$SIL" | awk -v dur="$DUR" -v pad="$PAD" '
  BEGIN{prev=0}
  NF==2 { s=$1+pad; if (s>prev) printf "%.3f %.3f\n", prev, s; prev=$2-pad; if(prev<0)prev=0 }
  END{ if (dur>prev) printf "%.3f %.3f\n", prev, dur }')

if [ -z "$SEGS" ]; then echo "Nessun segmento trovato"; exit 1; fi

FILTER=""; N=0
while read -r A B; do
  FILTER+="[0:v]trim=${A}:${B},setpts=PTS-STARTPTS[v$N];[0:a]atrim=${A}:${B},asetpts=PTS-STARTPTS[a$N];"
  N=$((N+1))
done <<< "$SEGS"
for i in $(seq 0 $((N-1))); do FILTER+="[v$i][a$i]"; done
FILTER+="concat=n=${N}:v=1:a=1[v][a]"

ffmpeg -hide_banner -y -i "$IN" -filter_complex "$FILTER" -map "[v]" -map "[a]" \
  -c:v libx264 -preset fast -crf 20 -c:a aac -b:a 160k "$OUT"

NEW=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$OUT")
printf "Fatto: %s (%.1fs -> %.1fs, %d segmenti)\n" "$OUT" "$DUR" "$NEW" "$N"
