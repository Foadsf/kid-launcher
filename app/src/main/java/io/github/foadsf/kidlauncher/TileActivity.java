package io.github.foadsf.kidlauncher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** What the home screen and the Games screen share: tiles, launching, pop-in and wiggles. */
abstract class TileActivity extends Activity {
    /** Ignore further taps for this long after launching something (no double launches). */
    private static final long LAUNCH_GUARD_MS = 1500;

    private static Typeface face;
    final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final List<TileView> tiles = new ArrayList<>();
    private long launchedAt;

    private final Runnable nudge = new Runnable() {
        @Override
        public void run() {
            if (!tiles.isEmpty()) tiles.get(random.nextInt(tiles.size())).wiggle();
            handler.postDelayed(this, 7000 + random.nextInt(5000));
        }
    };

    Typeface face() {
        if (face == null) face = Typeface.createFromAsset(getAssets(), "fonts/fredoka_semibold.ttf");
        return face;
    }

    void addTiles(TileGrid grid, KidApps.Entry[] list) {
        PackageManager pm = getPackageManager();
        Resources res = getResources();
        for (final KidApps.Resolved app : KidApps.resolve(pm, list)) {
            TileView t = new TileView(this, getString(app.entry.label), app.icon(pm, res, face()),
                    app.entry.icon == 0, res.getColor(app.entry.color),
                    res.getColor(app.entry.deep), face());
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    open(app);
                }
            });
            tiles.add(t);
            grid.addView(t);
        }
        popIn();
    }

    private void open(final KidApps.Resolved app) {
        long now = System.currentTimeMillis();
        if (now - launchedAt < LAUNCH_GUARD_MS) return;
        launchedAt = now;
        // Let the release bounce play before the next screen covers it.
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = app.component == null
                        ? new Intent(TileActivity.this, GamesActivity.class)
                        : app.launchIntent();
                try {
                    startActivity(i);
                } catch (ActivityNotFoundException | SecurityException e) {
                    Log.w(KidApps.TAG, "cannot start " + app.component, e);
                    Toast.makeText(TileActivity.this, R.string.app_missing, Toast.LENGTH_SHORT).show();
                }
            }
        }, 160);
    }

    /** Tiles pop up one after another, like bubbles. */
    void popIn() {
        for (int i = 0; i < tiles.size(); i++) {
            TileView t = tiles.get(i);
            t.setScaleX(0.4f);
            t.setScaleY(0.4f);
            t.setAlpha(0f);
            t.animate().scaleX(1f).scaleY(1f).alpha(1f)
                    .setStartDelay(Math.min(60L * i, 360L)).setDuration(420)
                    .setInterpolator(new OvershootInterpolator(2.2f)).start();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        popIn();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(nudge);
        handler.postDelayed(nudge, 4000);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(nudge);
        super.onPause();
    }
}
