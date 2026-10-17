#!/usr/bin/env bash
# Rebuild app/src/main/assets/fonts/fredoka_semibold.ttf from upstream Fredoka (SIL OFL 1.1).
#
# Upstream ships only a variable font. KitKat's font stack predates variable fonts, so this
# cuts a static SemiBold (wght 600, wdth 100) instance and subsets it to Latin + Latin-1
# (English and Dutch labels), which takes it from 159 KB to ~32 KB. Needs python3 + fontTools.
# Fredoka declares no Reserved Font Name, so the modified font may keep its name.
set -euo pipefail
cd "$(dirname "$0")/.."
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
curl -sfL -o "$work/Fredoka.ttf" \
    "https://raw.githubusercontent.com/google/fonts/main/ofl/fredoka/Fredoka%5Bwdth,wght%5D.ttf"
# The file this app's font was cut from on 2026-10-17:
echo "2ba02e68b152868aef9ba28e24b3648c7d457fe6f25c761f2c2c53fb61a73fc8  $work/Fredoka.ttf" \
    | sha256sum -c - || echo "WARNING: upstream Fredoka changed since 2026-10-17" >&2
python3 -I -m fontTools.varLib.instancer "$work/Fredoka.ttf" wght=600 wdth=100 -o "$work/inst.ttf"
python3 -I -m fontTools.subset "$work/inst.ttf" \
    --unicodes="U+0020-007E,U+00A0-00FF,U+2018-201E,U+2026" --layout-features='kern,liga' \
    --output-file=app/src/main/assets/fonts/fredoka_semibold.ttf
ls -l app/src/main/assets/fonts/fredoka_semibold.ttf
