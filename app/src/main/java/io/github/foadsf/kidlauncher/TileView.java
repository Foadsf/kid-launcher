package io.github.foadsf.kidlauncher;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * One big candy-button tile: a rounded pastel body sitting on a deeper "lip", a white badge
 * holding the picture, and a short label underneath. Pressing squishes it down onto its lip
 * and shrinks it a little; letting go springs it back with an overshoot.
 */
public class TileView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float dp, radius, edge;
    private final int fill, deep, ink;
    private final String label;
    private final Drawable icon;
    private final boolean foreignIcon;
    private final Path badge = new Path();
    private float iconAngle;
    private ValueAnimator wiggle;

    /**
     * @param foreignIcon true for another app's icon: it is drawn a little smaller and clipped
     *                    to the badge, because some (Vector Pinball) are opaque squares.
     */
    public TileView(Context context, String label, Drawable icon, boolean foreignIcon,
                    int fill, int deep, Typeface face) {
        super(context);
        this.label = label;
        this.icon = icon;
        this.foreignIcon = foreignIcon;
        this.fill = fill;
        this.deep = deep;
        this.ink = shade(deep, 0.62f);
        dp = getResources().getDisplayMetrics().density;
        radius = getResources().getDimension(R.dimen.tile_radius);
        edge = getResources().getDimension(R.dimen.tile_edge);
        text.setTypeface(face);
        text.setColor(ink);
        text.setTextAlign(Paint.Align.CENTER);
        setClickable(true);
        setFocusable(true);
        setContentDescription(label);
    }

    private static int shade(int c, float k) {
        return Color.rgb(Math.round(Color.red(c) * k), Math.round(Color.green(c) * k),
                Math.round(Color.blue(c) * k));
    }

    @Override
    public void setPressed(boolean pressed) {
        boolean was = isPressed();
        super.setPressed(pressed);
        if (was == pressed) return;
        animate().cancel();
        if (pressed) {
            animate().scaleX(0.93f).scaleY(0.93f).setDuration(70)
                    .setInterpolator(new DecelerateInterpolator()).start();
        } else {
            animate().scaleX(1f).scaleY(1f).setDuration(320)
                    .setInterpolator(new OvershootInterpolator(4f)).start();
        }
        invalidate();
    }

    /** A little "pick me!" shake of the picture. */
    public void wiggle() {
        if (wiggle != null && wiggle.isRunning()) return;
        wiggle = ValueAnimator.ofFloat(0f, 1f);
        wiggle.setDuration(900);
        wiggle.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                float t = (Float) a.getAnimatedValue();
                iconAngle = (float) (Math.sin(t * Math.PI * 5) * 11 * (1 - t));
                invalidate();
            }
        });
        wiggle.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (wiggle != null) wiggle.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float r = Math.min(radius, Math.min(w, h) / 4f);
        float sink = isPressed() ? edge * 0.7f : 0f;
        float bodyH = h - edge;

        // Lip, then body (which slides down onto the lip while pressed).
        paint.setColor(deep);
        rect.set(0, edge, w, h);
        c.drawRoundRect(rect, r, r, paint);
        paint.setColor(fill);
        rect.set(0, sink, w, bodyH + sink);
        c.drawRoundRect(rect, r, r, paint);

        // Candy shine in the top-left corner.
        paint.setColor(0x59FFFFFF);
        rect.set(r * 0.55f, sink + 9 * dp, w * 0.46f, sink + 21 * dp);
        c.drawRoundRect(rect, 6 * dp, 6 * dp, paint);
        c.drawCircle(w * 0.46f + 9 * dp, sink + 15 * dp, 4 * dp, paint);

        // A wide tile (Games) puts picture and word side by side, both bigger.
        boolean wide = w > bodyH * 1.6f;
        float sp = getResources().getDisplayMetrics().scaledDensity;
        float size = wide ? Math.min(40 * sp, bodyH * 0.26f)
                : Math.min(27 * sp, bodyH * 0.17f);
        text.setTextSize(size);
        float br = Math.min(w, bodyH) * (wide ? 0.33f : 0.30f);
        float max = wide ? w - 2 * br - 48 * dp : w - 18 * dp;
        float tw = text.measureText(label);
        if (tw > max) {
            text.setTextSize(size * max / tw);
            tw = max;
        }
        float bcx, bcy, tx, ty;
        if (wide) {
            float gapX = 18 * dp, total = 2 * br + gapX + tw;
            bcx = (w - total) / 2f + br;
            bcy = sink + bodyH * 0.5f;
            tx = bcx + br + gapX + tw / 2f;
            ty = bcy - (text.ascent() + text.descent()) / 2f;
        } else {
            bcx = w / 2f;
            bcy = sink + bodyH * 0.41f;
            tx = bcx;
            ty = sink + bodyH * 0.895f;
        }

        // White badge with the picture.
        paint.setColor(0x26000000 | (deep & 0x00FFFFFF));
        c.drawCircle(bcx, bcy + 3 * dp, br, paint);
        paint.setColor(0xF7FFFFFF);
        c.drawCircle(bcx, bcy, br, paint);
        if (icon != null) {
            int half = Math.round(br * (foreignIcon ? 0.72f : 0.80f));
            icon.setBounds(Math.round(bcx) - half, Math.round(bcy) - half,
                    Math.round(bcx) + half, Math.round(bcy) + half);
            int save = c.save();
            if (foreignIcon) {
                badge.reset();
                badge.addCircle(bcx, bcy, br, Path.Direction.CW);
                c.clipPath(badge); // hardware-accelerated since API 18
            }
            c.rotate(iconAngle, bcx, bcy);
            icon.draw(c);
            c.restoreToCount(save);
            if (foreignIcon) {
                // A clean rim over the clip edge, which is not anti-aliased.
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(4 * dp);
                paint.setColor(0xFFFFFFFF);
                c.drawCircle(bcx, bcy, br - 1.5f * dp, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }

        c.drawText(label, tx, ty, text);
    }
}
