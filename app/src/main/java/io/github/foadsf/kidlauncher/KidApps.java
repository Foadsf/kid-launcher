package io.github.foadsf.kidlauncher;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * The whitelists. Only these apps get a tile, in this order; anything not listed here is
 * never shown, however it got onto the phone. An entry whose package is missing is skipped,
 * except that Camera, Photos and Music fall back to whichever installed app fills that role.
 *
 * The defaults are what was installed on the Galaxy S4 mini VE (GT-I9195I, stock 4.4.4) this
 * was written for, with each game's name and category checked against F-Droid's index.
 * To change either list: edit it, rebuild, reinstall (see README).
 */
final class KidApps {
    static final String TAG = "KidLauncher";

    /** {@code Entry.icon} value: draw {@link LetterBlocks} instead of the app's own icon. */
    static final int ICON_LETTER_BLOCKS = -1;

    /** Not a package: the Games tile on the home screen, which opens {@link GamesActivity}. */
    static final String GAMES = "#games";

    static final class Entry {
        final String pkg;
        final int label, icon, color, deep;
        final List<Intent> fallbacks = new ArrayList<>();

        /** {@code icon == 0} means "use the app's own icon". */
        Entry(String pkg, int label, int icon, int color, int deep) {
            this.pkg = pkg;
            this.label = label;
            this.icon = icon;
            this.color = color;
            this.deep = deep;
        }

        /** If {@link #pkg} is missing, use an app that answers one of these, in order. */
        Entry orElse(Intent... roles) {
            java.util.Collections.addAll(fallbacks, roles);
            return this;
        }
    }

    private static Intent appCategory(String category) {
        return new Intent(Intent.ACTION_MAIN).addCategory(category);
    }

    static final Entry[] HOME = {
            new Entry("com.sec.android.app.camera", R.string.tile_camera, R.drawable.ic_camera,
                    R.color.pink, R.color.pink_deep)
                    .orElse(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)),
            new Entry("com.sec.android.gallery3d", R.string.tile_photos, R.drawable.ic_photos,
                    R.color.yellow, R.color.yellow_deep)
                    .orElse(appCategory(Intent.CATEGORY_APP_GALLERY), new Intent(Intent.ACTION_VIEW)
                            .setDataAndType(Uri.parse("content://media/external/images/media/1"),
                                    "image/*")),
            new Entry("com.sec.android.app.music", R.string.tile_music, R.drawable.ic_music,
                    R.color.lilac, R.color.lilac_deep)
                    .orElse(appCategory(Intent.CATEGORY_APP_MUSIC)),
            new Entry("com.samsung.everglades.video", R.string.tile_videos, R.drawable.ic_videos,
                    R.color.blue, R.color.blue_deep),
            new Entry(GAMES, R.string.tile_games, R.drawable.ic_games,
                    R.color.mint, R.color.mint_deep),
    };

    // Tile colours cycle through the palette so neighbours never match.
    private static final int[][] CYCLE = {
            {R.color.pink, R.color.pink_deep}, {R.color.yellow, R.color.yellow_deep},
            {R.color.lilac, R.color.lilac_deep}, {R.color.blue, R.color.blue_deep},
            {R.color.mint, R.color.mint_deep}, {R.color.peach, R.color.peach_deep}};

    private static Entry game(int i, String pkg, int label) {
        return game(i, pkg, label, 0);
    }

    private static Entry game(int i, String pkg, int label, int icon) {
        return new Entry(pkg, label, icon, CYCLE[i % CYCLE.length][0], CYCLE[i % CYCLE.length][1]);
    }

    /** F-Droid names in the comments; the readers (Orion, Seeneva) are deliberately absent. */
    static final Entry[] GAME_LIST = {
            game(3, "io.github.foadsf.robotpath", R.string.game_robot),        // Robot Path (ours)
            game(4, "io.github.foadsf.patternmachine", R.string.game_patterns), // Pattern Machine (ours)
            game(1, "io.github.foadsf.kangaroohops", R.string.game_hops),      // Kangaroo Hops (ours)
            game(5, "io.github.foadsf.balancescale", R.string.game_balance),   // Balance Scale (ours)
            game(0, "eu.quelltext.coloring", R.string.game_coloring),          // Coloring Book
            game(1, "se.tube42.kidsmem.android", R.string.game_memory),        // Candy Memory
            game(2, "ca.rmen.nounours", R.string.game_teddy),                  // Nounours and friends
            game(3, "ardash.doodledudette", R.string.game_doodle),             // Doodle Dudette
            game(4, "anupam.acrylic", R.string.game_paint),                    // Acrylic Paint
            game(5, "ch.cryptobit.letterbox", R.string.game_letters,           // LetterBox:
                    ICON_LETTER_BLOCKS),                               // its icon is the stock placeholder
            game(6, "eu.quelltext.counting", R.string.game_counting),          // 12345 - Learn Counting
            game(7, "org.secuso.privacyfriendlymemory", R.string.game_pairs),  // Memo Game
            game(8, "org.liri.liri", R.string.game_train),                     // Li-Ri
            game(9, "com.dozingcatsoftware.bouncy", R.string.game_pinball),    // Vector Pinball
            game(10, "dev.lonami.klooni", R.string.game_blocks),               // 1010! Klooni
            game(11, "com.quaap.primary", R.string.game_quiz),                 // Primary
    };

    static final class Resolved {
        final Entry entry;
        final ComponentName component; // null for the Games tile
        final ActivityInfo info;

        Resolved(Entry entry, ActivityInfo info) {
            this.entry = entry;
            this.info = info;
            this.component = info == null ? null : new ComponentName(info.packageName, info.name);
        }

        Intent launchIntent() {
            return new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(component)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        }

        /** Our drawn picture, or the app's own icon at a sharp density. */
        Drawable icon(PackageManager pm, Resources ours, Typeface face) {
            if (entry.icon == ICON_LETTER_BLOCKS) return new LetterBlocks(face);
            if (entry.icon != 0) return ours.getDrawable(entry.icon);
            try {
                int id = info.getIconResource();
                if (id != 0) {
                    Resources theirs = pm.getResourcesForApplication(info.applicationInfo);
                    return theirs.getDrawableForDensity(id, DisplayMetrics.DENSITY_XXHIGH);
                }
            } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
                Log.w(TAG, "no high-res icon for " + entry.pkg, e);
            }
            return info.loadIcon(pm);
        }
    }

    /** Entries that have a launcher activity on this phone, in list order. */
    static List<Resolved> resolve(PackageManager pm, Entry[] list) {
        List<Resolved> out = new ArrayList<>();
        for (Entry e : list) {
            if (GAMES.equals(e.pkg)) {
                out.add(new Resolved(e, null));
                continue;
            }
            List<ResolveInfo> hits = launchable(pm, e.pkg);
            for (int i = 0; hits.isEmpty() && i < e.fallbacks.size(); i++) {
                String pkg = byRole(pm, e.fallbacks.get(i));
                if (pkg != null) {
                    hits = launchable(pm, pkg);
                    Log.i(TAG, e.pkg + " missing; using " + pkg);
                }
            }
            if (hits.isEmpty()) {
                Log.i(TAG, "skip (not installed / no launcher activity): " + e.pkg);
                continue;
            }
            Resolved r = new Resolved(e, hits.get(0).activityInfo);
            Log.i(TAG, "tile: " + r.component.flattenToShortString());
            out.add(r);
        }
        return out;
    }

    /** A launchable app answering {@code role}; the phone's own (system) apps win. */
    private static String byRole(PackageManager pm, Intent role) {
        String any = null;
        for (ResolveInfo ri : pm.queryIntentActivities(role, 0)) {
            String pkg = ri.activityInfo.packageName;
            if (launchable(pm, pkg).isEmpty()) continue;
            if ((ri.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) return pkg;
            if (any == null) any = pkg;
        }
        return any;
    }

    private static List<ResolveInfo> launchable(PackageManager pm, String pkg) {
        Intent probe = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(pkg);
        return pm.queryIntentActivities(probe, 0);
    }

    private KidApps() {
    }
}
