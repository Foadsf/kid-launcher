#!/usr/bin/env python3
"""Drive the installed kid launcher over adb and check what it actually does.

    python3 -I tools/verify_on_device.py --serial SERIAL [--adb PATH] [--shots DIR]

Checks the DEFAULT whitelists (edit HOME / GAMES below if you changed KidApps.java).
Starts the launcher explicitly (it never makes it the default home), then:
  1. home screen: exactly the whitelisted tiles, nothing else; each tile starts its app
  2. Games screen: exactly the whitelisted games present on the device; each one starts
  3. parent gate: a tap and a 1 s hold do nothing; a 3.5 s hold asks a sum; a wrong answer
     is refused; the right one opens the parent menu, whose first item opens Settings
Success is judged by `dumpsys window` focus and uiautomator dumps on the device, not by
adb exit codes. Works on API 19: screenshots are taken on-device and pulled, because
KitKat's adbd has no `exec-out`.
"""
import argparse
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PKG = "io.github.foadsf.kidlauncher"
HOME = [("Camera", "com.sec.android.app.camera"), ("Photos", "com.sec.android.gallery3d"),
        ("Music", "com.sec.android.app.music"), ("Videos", "com.samsung.everglades.video"),
        ("Games", PKG)]
GAMES = [("Robot", "io.github.foadsf.robotpath"), ("Patterns", "io.github.foadsf.patternmachine"), ("Hops", "io.github.foadsf.kangaroohops"), ("Balance", "io.github.foadsf.balancescale"), ("Colouring", "eu.quelltext.coloring"), ("Memory", "se.tube42.kidsmem.android"),
         ("Teddy", "ca.rmen.nounours"), ("Doodle", "ardash.doodledudette"),
         ("Paint", "anupam.acrylic"), ("Letters", "ch.cryptobit.letterbox"),
         ("Counting", "eu.quelltext.counting"), ("Pairs", "org.secuso.privacyfriendlymemory"),
         ("Train", "org.liri.liri"), ("Pinball", "com.dozingcatsoftware.bouncy"),
         ("Blocks", "dev.lonami.klooni"), ("Quiz", "com.quaap.primary")]

ap = argparse.ArgumentParser()
ap.add_argument("--serial", required=True)
ap.add_argument("--adb", default=os.environ.get("ADB", "adb"))
ap.add_argument("--shots", help="directory for screenshots")
args = ap.parse_args()
failures = []


def adb(*a, timeout=60, check=False):
    for attempt in range(8):  # bounded retries: the phone's USB may be in a reset loop
        p = subprocess.run([args.adb, "-s", args.serial, *a], capture_output=True, text=True,
                           timeout=timeout)
        out = "\n".join(l for l in (p.stdout + p.stderr).splitlines() if "WARNING: linker" not in l)
        if p.returncode == 0 and not re.search(r"error: (closed|device .* not found)", out):
            return out
        time.sleep(2)
    if check:
        raise SystemExit(f"adb {' '.join(a)} kept failing: {out}")
    return out


def sh(cmd):
    return adb("shell", cmd)


def ok(cond, what):
    print(("PASS " if cond else "FAIL ") + what)
    if not cond:
        failures.append(what)


def focus():
    m = re.search(r"mCurrentFocus=Window\{\S+ \S+ ([^/\s}]+)/?(\S*)\}", sh("dumpsys window windows"))
    return (m.group(1), m.group(2)) if m else ("", "")


def dump():
    sh("uiautomator dump /data/local/tmp/ui.xml")
    xml = sh("cat /data/local/tmp/ui.xml")
    xml = xml[xml.find("<?xml"):]
    return ET.fromstring(xml[: xml.rfind(">") + 1])


def center(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    return (x1 + x2) // 2, (y1 + y2) // 2


def tiles(root):
    """Our TileViews are plain Views with a content description: the label."""
    return {n.get("content-desc"): n for n in root.iter("node")
            if n.get("package") == PKG and n.get("class") == "android.view.View"
            and n.get("content-desc")}


def texts(root):
    return [n.get("text") for n in root.iter("node") if n.get("text")]


def shot(name):
    if not args.shots:
        return
    os.makedirs(args.shots, exist_ok=True)
    sh("screencap -p /data/local/tmp/shot.png")
    adb("pull", "/data/local/tmp/shot.png", os.path.join(args.shots, name + ".png"))


def installed(pkg):
    return sh(f"pm path {pkg}").strip().startswith("package:")


def start(activity):
    sh(f"am start -W -n {PKG}/.{activity}")
    time.sleep(2.5)  # pop-in animation


def tap(x, y):
    sh(f"input tap {x} {y}")


# ---- 1. home screen -------------------------------------------------------------------
ok(installed(PKG), f"pm path {PKG}")
start("HomeActivity")
ok(focus()[0] == PKG, "launcher has focus")
shot("home")
root = dump()
found = tiles(root)
want = [l for l, p in HOME if p == PKG or installed(p)]
ok(sorted(found) == sorted(want), f"home tiles are exactly {want} (got {sorted(found)})")
for label, pkg in HOME:
    if label not in found:
        continue
    start("HomeActivity")
    tap(*center(tiles(dump())[label]))
    time.sleep(3)
    f = focus()
    ok(f[0] == pkg and (pkg != PKG or f[1].endswith("GamesActivity")),
       f"tap {label} -> {pkg} (focus {f[0]}/{f[1]})")
    if pkg != PKG:
        sh(f"am force-stop {pkg}")

# ---- 2. games -------------------------------------------------------------------------
start("GamesActivity")
shot("games")
seen = {}
for _ in range(4):  # scroll through the list collecting labels
    seen.update({k: True for k in tiles(dump())})
    sh("input swipe 270 800 270 250 400")
    time.sleep(1)
shot("games-scrolled")
want = [l for l, p in GAMES if installed(p)]
ok(sorted(seen) == sorted(want), f"games are exactly the installed whitelist ({len(want)}) "
                                 f"(got {len(seen)}: {sorted(set(seen) ^ set(want)) or 'match'})")
for label, pkg in GAMES:
    if label not in seen:
        continue
    start("GamesActivity")
    for _ in range(3):  # an earlier visit may have left the list scrolled down
        sh("input swipe 270 300 270 850 300")
    node = None
    for _ in range(5):
        node = tiles(dump()).get(label)
        if node is not None and center(node)[1] < 880:
            break
        sh("input swipe 270 800 270 400 400")
        time.sleep(1)
    if node is None:
        ok(False, f"game tile {label} not found on the Games screen")
        continue
    tap(*center(node))
    time.sleep(4)
    f = focus()
    ok(f[0] == pkg, f"tap game {label} -> {pkg} (focus {f[0]})")
    sh(f"am force-stop {pkg}")

# ---- 3. parent gate -------------------------------------------------------------------
start("HomeActivity")
greeting = next(n for n in dump().iter("node") if n.get("text", "").startswith(("Hi", "Hoi")))
dens = int(re.search(r"(\d+)", sh("wm density")).group(1)) / 160
w = int(re.search(r"(\d+)x\d+", sh("wm size")).group(1))
gx, gy = int(w - 32 * dens), center(greeting)[1]
gx_, gy_ = gx, gy - int(12 * dens)  # the gate is the 64 dp square in the header's corner

tap(gx_, gy_)
time.sleep(1.5)
ok("Grown-ups only" not in texts(dump()), "a tap on the corner does nothing")
sh(f"input swipe {gx_} {gy_} {gx_} {gy_} 1000")
time.sleep(2.5)
ok("Grown-ups only" not in texts(dump()), "a 1 s hold does nothing")


def open_gate():
    sh(f"input swipe {gx_} {gy_} {gx_} {gy_} 3500")
    time.sleep(1.5)
    root = dump()
    q = next((t for t in texts(root) if re.fullmatch(r"\d+ \+ \d+ = \?", t)), None)
    return root, q


def answer(n):
    """Type n and press OK. The keyboard pans the dialog up as it opens, so the OK button is
    located only after typing, or the tap lands outside the dialog and cancels it."""
    time.sleep(1.5)
    sh(f"input text {n}")
    time.sleep(1)
    tap(*center(next(x for x in dump().iter("node") if x.get("text") == "OK")))


root, q = open_gate()
ok(q is not None, f"a 3.5 s hold asks a sum ({q})")
shot("gate")
a, b = map(int, re.findall(r"\d+", q))
answer(a + b + 1)
time.sleep(1.5)
ok("Parent menu" not in texts(dump()), "a wrong answer does not open the parent menu")

root, q = open_gate()
a, b = map(int, re.findall(r"\d+", q))
answer(a + b)
time.sleep(1.5)
root = dump()
ok("Parent menu" in texts(root), "the right answer opens the parent menu")
shot("parent-menu")
tap(*center(next(n for n in root.iter("node") if n.get("text") == "Open Settings")))
time.sleep(3)
ok(focus()[0] == "com.android.settings", f"Open Settings -> com.android.settings ({focus()[0]})")
sh("input keyevent KEYCODE_BACK")

print(f"\n{'ALL PASSED' if not failures else f'{len(failures)} FAILED'}")
sys.exit(1 if failures else 0)
