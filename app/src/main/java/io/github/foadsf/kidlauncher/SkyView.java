package io.github.foadsf.kidlauncher;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * The soft background: a pink-to-sky gradient, a faint rainbow, a smiling sun, clouds,
 * twinkles and two rolling hills. Drawn once per size change; nothing animates here, so the
 * home screen costs no battery while it just sits there.
 */
public class SkyView extends View {
    private static final int[] RAINBOW = {
            0xFFFF8FA3, 0xFFFFAD7A, 0xFFFFD166, 0xFF7EE0B5, 0xFF7CC8FF, 0xFFB79CFF};

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sky = new Paint();
    private final Path hillBack = new Path();
    private final Path hillFront = new Path();
    private final Path tmp = new Path();
    private final RectF oval = new RectF();
    private final float dp;
    private boolean sun = true;
    private final int statusBar;

    public SkyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = getResources();
        dp = res.getDisplayMetrics().density;
        int id = res.getIdentifier("status_bar_height", "dimen", "android");
        statusBar = id > 0 ? res.getDimensionPixelSize(id) : Math.round(25 * dp);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
    }

    /** The Games screen hides the sun: its house button sits where the sun would be. */
    void setShowSun(boolean show) {
        sun = show;
        invalidate();
    }

    private int color(int id) {
        return getResources().getColor(id);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        sky.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{color(R.color.sky_top), color(R.color.sky_mid), color(R.color.sky_bottom)},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));

        hillBack.reset();
        hillBack.moveTo(0, h - 46 * dp);
        hillBack.quadTo(w * 0.28f, h - 92 * dp, w * 0.58f, h - 50 * dp);
        hillBack.quadTo(w * 0.82f, h - 22 * dp, w, h - 64 * dp);
        hillBack.lineTo(w, h);
        hillBack.lineTo(0, h);
        hillBack.close();

        hillFront.reset();
        hillFront.moveTo(0, h - 18 * dp);
        hillFront.quadTo(w * 0.35f, h - 52 * dp, w * 0.7f, h - 20 * dp);
        hillFront.quadTo(w * 0.88f, h - 6 * dp, w, h - 26 * dp);
        hillFront.lineTo(w, h);
        hillFront.lineTo(0, h);
        hillFront.close();
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        boolean land = w > h;
        c.drawRect(0, 0, w, h, sky);

        // Rainbow, centred low so its crown sits in the header band.
        float cx = w * 0.5f, cy = land ? h * 0.95f : h * 0.55f;
        float band = 9 * dp, r0 = (land ? h * 0.92f : w * 0.62f);
        stroke.setStrokeCap(Paint.Cap.BUTT);
        stroke.setStrokeWidth(band);
        for (int i = 0; i < RAINBOW.length; i++) {
            float r = r0 - i * band;
            oval.set(cx - r, cy - r, cx + r, cy + r);
            stroke.setColor((RAINBOW[i] & 0x00FFFFFF) | 0x66000000);
            c.drawArc(oval, 180, 180, false, stroke);
        }
        stroke.setStrokeCap(Paint.Cap.ROUND);

        float top = statusBar;
        float headerMid = top + (land ? 26 : 46) * dp;

        cloud(c, w * 0.80f, headerMid + 6 * dp, land ? 0.8f : 1f);
        cloud(c, w * 0.12f, h * 0.58f, 0.8f);
        cloud(c, w * 0.88f, h * 0.80f, 0.9f);

        if (sun) sun(c, (land ? 34 : 38) * dp, headerMid, (land ? 16 : 20) * dp);

        // Twinkles scattered where the tiles leave gaps.
        float[][] twinkles = {
                {0.30f, 0.020f, 7}, {0.62f, 0.035f, 5}, {0.95f, 0.30f, 6}, {0.04f, 0.40f, 5},
                {0.50f, 0.52f, 6}, {0.96f, 0.62f, 5}, {0.06f, 0.86f, 6}, {0.50f, 0.95f, 7}};
        for (int i = 0; i < twinkles.length; i++) {
            float[] t = twinkles[i];
            fill.setColor(i % 2 == 0 ? 0xE6FFFFFF : 0xE6FFD166);
            twinkle(c, w * t[0], top + (h - top) * t[1], t[2] * dp);
        }

        fill.setColor(color(R.color.hill_back));
        c.drawPath(hillBack, fill);
        fill.setColor(color(R.color.hill_front));
        c.drawPath(hillFront, fill);
    }

    private void cloud(Canvas c, float x, float y, float s) {
        fill.setColor(0xF0FFFFFF);
        float u = 10 * dp * s;
        c.drawCircle(x - 1.6f * u, y + 0.3f * u, 1.2f * u, fill);
        c.drawCircle(x - 0.2f * u, y - 0.6f * u, 1.7f * u, fill);
        c.drawCircle(x + 1.5f * u, y + 0.1f * u, 1.3f * u, fill);
        oval.set(x - 2.8f * u, y, x + 2.8f * u, y + 1.5f * u);
        c.drawRoundRect(oval, 0.75f * u, 0.75f * u, fill);
    }

    private void sun(Canvas c, float x, float y, float r) {
        stroke.setColor(color(R.color.sun_ray));
        stroke.setStrokeWidth(r * 0.22f);
        for (int i = 0; i < 10; i++) {
            double a = Math.toRadians(i * 36 + 8);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            c.drawLine(x + cos * r * 1.3f, y + sin * r * 1.3f,
                    x + cos * r * 1.62f, y + sin * r * 1.62f, stroke);
        }
        fill.setColor(color(R.color.sun));
        c.drawCircle(x, y, r, fill);

        // A friendly face: eyes, rosy cheeks, smile.
        fill.setColor(color(R.color.plum));
        c.drawCircle(x - r * 0.34f, y - r * 0.12f, r * 0.11f, fill);
        c.drawCircle(x + r * 0.34f, y - r * 0.12f, r * 0.11f, fill);
        fill.setColor(0x66FF8FA3);
        c.drawCircle(x - r * 0.55f, y + r * 0.22f, r * 0.17f, fill);
        c.drawCircle(x + r * 0.55f, y + r * 0.22f, r * 0.17f, fill);
        stroke.setColor(color(R.color.plum));
        stroke.setStrokeWidth(r * 0.09f);
        oval.set(x - r * 0.32f, y - r * 0.12f, x + r * 0.32f, y + r * 0.42f);
        c.drawArc(oval, 25, 130, false, stroke);
    }

    private void twinkle(Canvas c, float x, float y, float r) {
        tmp.reset();
        float k = r * 0.3f;
        tmp.moveTo(x, y - r);
        tmp.quadTo(x + k * 0.3f, y - k * 0.3f, x + r, y);
        tmp.quadTo(x + k * 0.3f, y + k * 0.3f, x, y + r);
        tmp.quadTo(x - k * 0.3f, y + k * 0.3f, x - r, y);
        tmp.quadTo(x - k * 0.3f, y - k * 0.3f, x, y - r);
        tmp.close();
        c.drawPath(tmp, fill);
    }
}
