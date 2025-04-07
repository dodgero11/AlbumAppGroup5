package com.example.albumappgroup5.utils;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

public class TouchImageView extends androidx.appcompat.widget.AppCompatImageView {

    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();

    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;

    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;

    float[] matrixValues = new float[9];
    float minScale = 1f;
    float maxScale = 5f;

    public TouchImageView(Context context) {
        super(context);
        init();
    }

    public TouchImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TouchImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setScaleType(ScaleType.MATRIX);
        matrix.setTranslate(1f, 1f);
        setImageMatrix(matrix);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                mode = DRAG;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    matrix.set(savedMatrix);
                    float dx = event.getX() - start.x;
                    float dy = event.getY() - start.y;
                    matrix.postTranslate(dx, dy);
                    fixTranslation();
                } else if (mode == ZOOM) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        matrix.set(savedMatrix);
                        float scale = newDist / oldDist;
                        float currentScale = getScale();
                        float desiredScale = currentScale * scale;

                        if (desiredScale > maxScale) scale = maxScale / currentScale;
                        else if (desiredScale < minScale) scale = minScale / currentScale;

                        matrix.postScale(scale, scale, mid.x, mid.y);
                        fixTranslation();
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
        }

        setImageMatrix(matrix);
        return true;
    }

    private void fixTranslation() {
        RectF bounds = getDrawableBounds();
        if (bounds == null) return;

        float deltaX = getFixTrans(bounds.left, bounds.right, getWidth());
        float deltaY = getFixTrans(bounds.top, bounds.bottom, getHeight());

        matrix.postTranslate(deltaX, deltaY);
    }

    private float getFixTrans(float min, float max, float viewSize) {
        float delta = 0;
        if (max - min < viewSize) {
            delta = (viewSize - (max + min)) / 2;
        } else {
            if (min > 0) delta = -min;
            if (max < viewSize) delta = viewSize - max;
        }
        return delta;
    }

    private RectF getDrawableBounds() {
        if (getDrawable() == null) return null;
        matrix.getValues(matrixValues);
        float scaleX = matrixValues[Matrix.MSCALE_X];
        float scaleY = matrixValues[Matrix.MSCALE_Y];
        float transX = matrixValues[Matrix.MTRANS_X];
        float transY = matrixValues[Matrix.MTRANS_Y];

        int intrinsicWidth = getDrawable().getIntrinsicWidth();
        int intrinsicHeight = getDrawable().getIntrinsicHeight();

        float width = intrinsicWidth * scaleX;
        float height = intrinsicHeight * scaleY;

        return new RectF(transX, transY, transX + width, transY + height);
    }

    private float getScale() {
        matrix.getValues(matrixValues);
        return matrixValues[Matrix.MSCALE_X];
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }
}
