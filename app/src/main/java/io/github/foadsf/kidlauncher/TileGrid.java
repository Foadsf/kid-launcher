package io.github.foadsf.kidlauncher;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Lays its children out as equal tiles: 2 columns in portrait, 3 in landscape (4 when the
 * grid scrolls, so more than one row of games fits on the short landscape screen).
 *
 * With a fixed height (the home screen) the tiles grow to fill it: on the S4 mini (360x615 dp
 * usable) that is about 162x160 dp. Inside a ScrollView (the Games screen) the height is
 * unbounded, so tiles are nearly square and the list scrolls. On the home screen a lone tile
 * in the last row stretches across it, which is how Games gets its wide button.
 */
public class TileGrid extends ViewGroup {
    private final int gap;
    private boolean spanLonelyLast;
    private int cols, rows, cellW, cellH, offsetY;

    public TileGrid(Context context, AttributeSet attrs) {
        super(context, attrs);
        gap = getResources().getDimensionPixelSize(R.dimen.gap);
        setClipChildren(false); // let pressed tiles bounce past their cell
    }

    void setSpanLonelyLast(boolean span) {
        spanLonelyLast = span;
        requestLayout();
    }

    /** Show the press immediately: a tap here is never the start of a scroll gesture we own. */
    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    private boolean lastSpans() {
        int n = getChildCount();
        return spanLonelyLast && cols > 1 && n % cols == 1;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec), h = MeasureSpec.getSize(heightSpec);
        boolean scrolling = MeasureSpec.getMode(heightSpec) == MeasureSpec.UNSPECIFIED;
        boolean land = getResources().getConfiguration().orientation
                == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        int n = getChildCount();
        cols = land ? (scrolling ? 4 : 3) : 2;
        if (n > 0 && n < cols) cols = n;
        rows = Math.max(1, (n + cols - 1) / cols);
        cellW = (w - (cols + 1) * gap) / cols;
        if (scrolling) {
            cellH = Math.round(cellW * 0.95f);
            h = rows * cellH + (rows + 1) * gap;
            offsetY = 0;
        } else {
            cellH = (h - (rows + 1) * gap) / rows;
            // Do not let tiles turn into tall strips when there are only a few of them.
            cellH = Math.min(cellH, Math.round(cellW * 1.2f));
            offsetY = (h - (rows * cellH + (rows + 1) * gap)) / 2;
        }
        int ch = MeasureSpec.makeMeasureSpec(cellH, MeasureSpec.EXACTLY);
        for (int i = 0; i < n; i++) {
            boolean wide = i == n - 1 && lastSpans();
            int cw = wide ? cols * cellW + (cols - 1) * gap : cellW;
            getChildAt(i).measure(MeasureSpec.makeMeasureSpec(cw, MeasureSpec.EXACTLY), ch);
        }
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int n = getChildCount();
        int usedW = cols * cellW + (cols + 1) * gap;
        int offsetX = (r - l - usedW) / 2;
        for (int i = 0; i < n; i++) {
            View v = getChildAt(i);
            int row = i / cols, col = i % cols;
            // Centre a short last row (unless its lone tile spans it).
            int inRow = Math.min(cols, n - row * cols);
            int rowShift = lastSpans() && i == n - 1 ? 0 : (cols - inRow) * (cellW + gap) / 2;
            int x = offsetX + rowShift + gap + col * (cellW + gap);
            int y = offsetY + gap + row * (cellH + gap);
            v.layout(x, y, x + v.getMeasuredWidth(), y + cellH);
        }
    }
}
