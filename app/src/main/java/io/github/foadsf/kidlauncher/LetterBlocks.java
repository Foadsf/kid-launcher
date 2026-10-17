package io.github.foadsf.kidlauncher;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

/**
 * Three toy blocks, A B C, for the letters game. LetterBox ships the stock Android
 * placeholder as its icon, which tells a five-year-old nothing; a picture of letters does.
 * Drawn in code because a vector drawable cannot carry the font.
 */
final class LetterBlocks extends Drawable {
    private static final int[][] BLOCKS = { // fill, edge
            {0xFFFF8FA3, 0xFFE8607E}, {0xFFFFD166, 0xFFF0A830}, {0xFF7CC8FF, 0xFF3F95E0}};
    private static final String[] LETTERS = {"A", "B", "C"};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    LetterBlocks(Typeface face) {
        text.setTypeface(face);
        text.setColor(0xFFFFFFFF);
        text.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        float s = b.width() * 0.44f, edge = s * 0.1f;
        float cx = b.exactCenterX(), bottom = b.top + b.height() * 0.93f;
        // Two blocks on the floor, one balanced on top.
        float[][] at = {
                {cx - s * 1.03f, bottom - s}, {cx + s * 0.03f, bottom - s}, {cx - s * 0.5f, bottom - 2.05f * s}};
        text.setTextSize(s * 0.72f);
        for (int i = 0; i < 3; i++) {
            float x = at[i][0], y = at[i][1];
            paint.setColor(BLOCKS[i][1]);
            r.set(x, y + edge, x + s, y + s);
            c.drawRoundRect(r, s * 0.2f, s * 0.2f, paint);
            paint.setColor(BLOCKS[i][0]);
            r.set(x, y, x + s, y + s - edge);
            c.drawRoundRect(r, s * 0.2f, s * 0.2f, paint);
            float ty = y + (s - edge) / 2f - (text.ascent() + text.descent()) / 2f;
            c.drawText(LETTERS[i], x + s / 2f, ty, text);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        text.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
        text.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
