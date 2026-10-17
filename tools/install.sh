#!/usr/bin/env bash
# Install the launcher onto an old phone whose USB link may keep dropping (some old phones
# fall into a reset loop: up ~5 s, disconnect, re-enumerate).
#
#   tools/install.sh APK SERIAL        (serial from `adb devices`; always name it)
#
#   - split push and install: `pm install` runs ON the phone, so it finishes even if USB drops
#   - bounded retries, never `adb wait-for-device`
#   - success is judged by `pm path` and an on-device SHA-1 of the pushed file, never adb's output
set -u

[ $# -eq 2 ] || { echo "usage: $0 APK SERIAL" >&2; exit 2; }
APK=$1
SERIAL=$2
PKG=io.github.foadsf.kidlauncher
TMP=/data/local/tmp/kid-launcher.apk
ADB=${ADB:-adb}
TRIES=${TRIES:-15}

[ -f "$APK" ] || { echo "no APK at $APK - run ./gradlew assembleDebug first" >&2; exit 2; }

a() { timeout 60 "$ADB" -s "$SERIAL" "$@" 2>&1; }

want=$(sha1sum "$APK" | cut -d' ' -f1)
for i in $(seq 1 "$TRIES"); do
    a push "$APK" "$TMP" >/dev/null
    # Many old Android builds have no sha1sum; fall back to comparing byte counts then.
    got=$(a shell "sha1sum $TMP 2>/dev/null || ls -l $TMP" | tr -d '\r')
    size=$(stat -c %s "$APK")
    if grep -q "$want" <<<"$got" || { ! grep -q '^[0-9a-f]\{40\}' <<<"$got" && grep -qw "$size" <<<"$got"; }; then
        echo "[$i] pushed ($size bytes, on-device: ${got%% *})"
        out=$(a shell "pm install -r $TMP")
        echo "[$i] pm install: $(tr -d '\r' <<<"$out" | tail -1)"
        break
    fi
    echo "[$i] push not confirmed on device, retrying" >&2
    sleep 2
done

for i in $(seq 1 "$TRIES"); do
    # Search, do not take line 1: some builds print linker WARNINGs before the answer.
    p=$(a shell "pm path $PKG" | tr -d '\r' | grep -m1 '^package:')
    [ -n "$p" ] && { echo "installed: $p"; exit 0; }
    sleep 2
done
echo "NOT installed: pm path $PKG returned nothing" >&2
exit 1
