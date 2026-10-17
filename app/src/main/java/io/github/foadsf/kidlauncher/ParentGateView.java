package io.github.foadsf.kidlauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * An unmarked corner. Holding a finger on it for {@link #HOLD_MS} opens the parent gate.
 * A thin ring appears after the first second and fills up, so a grown-up knows to keep
 * holding; a tap, a brush past or a finger that slides off does nothing.
 */
public class ParentGateView extends View {
    static final long HOLD_MS = 3000;
    private static final long RING_AFTER_MS = 1000;

    interface Listener {
        void onParentGate();
    }

    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private Listener listener;
    private long downAt = -1;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (downAt < 0) return;
            if (SystemClock.uptimeMillis() - downAt >= HOLD_MS) {
                downAt = -1;
                invalidate();
                if (listener != null) listener.onParentGate();
            } else {
                invalidate();
                postDelayed(this, 50);
            }
        }
    };

    public ParentGateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeCap(Paint.Cap.ROUND);
        ring.setStrokeWidth(4 * getResources().getDisplayMetrics().density);
        ring.setColor(0x995B3E8C);
        // Not a button as far as accessibility is concerned: there is nothing here for her.
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void setListener(Listener l) {
        listener = l;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downAt = SystemClock.uptimeMillis();
                removeCallbacks(tick);
                postDelayed(tick, 50);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (e.getX() < 0 || e.getY() < 0 || e.getX() > getWidth() || e.getY() > getHeight()) {
                    cancel();
                }
                return true;
            case MotionEvent.ACTION_POINTER_DOWN: // a second finger is not a deliberate hold
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                cancel();
                return true;
            default:
                return true;
        }
    }

    private void cancel() {
        downAt = -1;
        removeCallbacks(tick);
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas c) {
        if (downAt < 0) return;
        long held = SystemClock.uptimeMillis() - downAt;
        if (held < RING_AFTER_MS) return;
        float p = Math.min(1f, (held - RING_AFTER_MS) / (float) (HOLD_MS - RING_AFTER_MS));
        float inset = getWidth() * 0.22f;
        oval.set(inset, inset, getWidth() - inset, getHeight() - inset);
        c.drawArc(oval, -90, 360 * p, false, ring);
    }
}
