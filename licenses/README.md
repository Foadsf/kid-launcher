# Third-party material in this app

| What | File | Source | Licence |
|---|---|---|---|
| Fredoka font (static SemiBold instance, Latin subset) | `app/src/main/assets/fonts/fredoka_semibold.ttf` | [google/fonts `ofl/fredoka`](https://github.com/google/fonts/tree/main/ofl/fredoka), Copyright 2016 The Fredoka Project Authors | SIL Open Font License 1.1 - [`Fredoka-OFL.txt`](Fredoka-OFL.txt) |

The font was modified (instanced and subset) by `tools/make_font.sh`. Fredoka declares no
Reserved Font Name, so the modified version keeps its name, as OFL section 3 permits.

Everything else is original to this app and drawn as code: the icons by
`tools/gen_icons.py` (vector drawables), and the sky, sun, clouds and hills by `SkyView.java`.
The app has no library dependencies at all.
