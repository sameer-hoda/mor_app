#!/usr/bin/env bash
# Mor screenshot helper — capture or prepare ONE small preview for inspection.
#
# Why: the provider rejects a request when accumulated history images exceed
# 30MB ("Downloaded image content cannot exceed 30MB"). Keep previews tiny and
# keep full-resolution originals outside tracked source.
#
# Usage:
#   scripts/mor-shot.sh <name>                 # capture emulator screenshot
#   scripts/mor-shot.sh --prepare <input> <name>  # prepare an existing image
#
# Outputs exactly one preview under the byte budget, or exits 32 and tells you
# not to send it. Full-resolution originals are preserved in:
#   ${MOR_EVIDENCE_DIR:-$HOME/.mor-evidence}/originals
#
# Env knobs (all optional):
#   MOR_EVIDENCE_DIR       default: $HOME/.mor-evidence
#   MOR_DEVICE             default: emulator-5554
#   MOR_MAX_PREVIEW_BYTES  default: 180000 (conservative, << 30MB request budget)
#   MOR_PREVIEW_WIDTH      default: 540
#   MOR_PREVIEW_QUALITY    default: 55
#   MOR_MAX_PREVIEW_DIM    default: 2000
#   ADB                    default: /opt/homebrew/share/android-commandlinetools/platform-tools/adb
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  scripts/mor-shot.sh <name>
  scripts/mor-shot.sh --prepare <input-image> <name>

Env: MOR_EVIDENCE_DIR, MOR_DEVICE, MOR_MAX_PREVIEW_BYTES,
     MOR_PREVIEW_WIDTH, MOR_PREVIEW_QUALITY, MOR_MAX_PREVIEW_DIM, ADB
EOF
}

die() { echo "ERROR: $*" >&2; exit "${2:-1}"; }

MODE="capture"
if [[ "${1:-}" == "--prepare" ]]; then
  MODE="prepare"
  shift
  INPUT="${1:-}"
  NAME="${2:-}"
  [[ -n "$INPUT" && -n "$NAME" ]] || { usage; exit 2; }
  [[ -f "$INPUT" ]] || die "input not found: $INPUT" 31
else
  NAME="${1:-}"
  [[ -n "$NAME" ]] || { usage; exit 2; }
fi

EVIDENCE_DIR="${MOR_EVIDENCE_DIR:-$HOME/.mor-evidence}"
ORIG_DIR="$EVIDENCE_DIR/originals"
PREVIEW_DIR="$EVIDENCE_DIR/previews"
mkdir -p "$ORIG_DIR" "$PREVIEW_DIR"

DEVICE="${MOR_DEVICE:-emulator-5554}"
ADB="${ADB:-/opt/homebrew/share/android-commandlinetools/platform-tools/adb}"
MAX_PREVIEW_BYTES="${MOR_MAX_PREVIEW_BYTES:-180000}"
PREFERRED_WIDTH="${MOR_PREVIEW_WIDTH:-540}"
PREFERRED_QUALITY="${MOR_PREVIEW_QUALITY:-55}"
MAX_DIM="${MOR_MAX_PREVIEW_DIM:-2000}"

STAMP="$(date +%Y%m%d-%H%M%S)"
SAFE="$(printf '%s' "$NAME" | tr -cs 'A-Za-z0-9._-' '-')"
SAFE="${SAFE#-}"; SAFE="${SAFE%-}"

if [[ "$MODE" == "capture" ]]; then
  command -v "$ADB" >/dev/null 2>&1 || die "adb not found: $ADB" 31
  ORIG="$ORIG_DIR/${STAMP}-${SAFE}.png"
  "$ADB" -s "$DEVICE" exec-out screencap -p > "$ORIG" || die "adb capture failed" 31
  [[ -s "$ORIG" ]] || die "capture produced an empty file" 31
else
  EXT="${INPUT##*.}"
  [[ "$EXT" == "$INPUT" || -z "$EXT" ]] && EXT="img"
  ORIG="$ORIG_DIR/${STAMP}-${SAFE}.${EXT}"
  cp "$INPUT" "$ORIG" || die "could not preserve original" 31
  [[ -s "$ORIG" ]] || die "input is empty: $INPUT" 31
fi

DIMS="$(sips -g pixelWidth -g pixelHeight "$ORIG" 2>/dev/null \
  | awk '/pixelWidth/{w=$2}/pixelHeight/{h=$2}END{if(w&&h)print w"x"h}')"
[[ -n "$DIMS" ]] || die "input is not a readable image: $ORIG" 31
ORIG_W="${DIMS%x*}"

PREVIEW="$PREVIEW_DIR/${STAMP}-${SAFE}.jpg"
for w in "$PREFERRED_WIDTH" 480 420 360 320 280; do
  CW="$w"
  (( CW > ORIG_W )) && CW="$ORIG_W"
  for q in "$PREFERRED_QUALITY" 45 35 25 15; do
    TMP="$PREVIEW_DIR/.tmp-$$-${CW}-${q}.jpg"
    if ! sips -s format jpeg -s formatOptions "$q" --resampleWidth "$CW" "$ORIG" --out "$TMP" >/dev/null 2>&1; then
      rm -f "$TMP"; continue
    fi
    [[ -s "$TMP" ]] || { rm -f "$TMP"; continue; }
    if ! file -b "$TMP" | grep -qi 'JPEG'; then rm -f "$TMP"; continue; fi
    BYTES="$(stat -f%z "$TMP")"
    PD="$(sips -g pixelWidth -g pixelHeight "$TMP" 2>/dev/null \
      | awk '/pixelWidth/{w=$2}/pixelHeight/{h=$2}END{if(w&&h)print w"x"h}')"
    if [[ -n "$PD" && "$PD" =~ ^([0-9]+)x([0-9]+)$ ]]; then
      PW="${BASH_REMATCH[1]}"; PH="${BASH_REMATCH[2]}"
      if (( BYTES <= MAX_PREVIEW_BYTES && PW <= MAX_DIM && PH <= MAX_DIM )); then
        mv "$TMP" "$PREVIEW"
        echo "PREVIEW=$PREVIEW"
        echo "BYTES=$BYTES"
        echo "DIMS=${PW}x${PH}"
        echo "BUDGET=$MAX_PREVIEW_BYTES"
        echo "ORIGINAL=$ORIG"
        echo "ORIGINAL_DIMS=$DIMS"
        exit 0
      fi
    fi
    rm -f "$TMP"
  done
done

die "REJECTED: no candidate preview <= ${MAX_PREVIEW_BYTES} bytes at <= ${MAX_DIM}px; do NOT send $ORIG" 32
