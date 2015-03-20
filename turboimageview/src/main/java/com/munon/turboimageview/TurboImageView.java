package com.munon.turboimageview;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import com.munon.turboimageview.MultiTouchController.MultiTouchObjectCanvas;
import com.munon.turboimageview.MultiTouchController.PointInfo;
import com.munon.turboimageview.MultiTouchController.PositionAndScale;

import java.util.ArrayList;

public class TurboImageView extends View implements
    MultiTouchObjectCanvas<MultiTouchObject> {

    private ArrayList<MultiTouchObject> mImages = new ArrayList<MultiTouchObject>();
    private MultiTouchController<MultiTouchObject> multiTouchController = new MultiTouchController<MultiTouchObject>(
        this);
    private PointInfo currTouchPoint = new PointInfo();
    private static final int UI_MODE_ROTATE = 1, UI_MODE_ANISOTROPIC_SCALE = 2;
    private int mUIMode = UI_MODE_ROTATE;
    private static final float SCREEN_MARGIN = 100;
    private int displayWidth, displayHeight;

    public TurboImageView(Context context) {
        this(context, null);
    }

    public TurboImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TurboImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        Resources res = context.getResources();
        setBackgroundColor(Color.TRANSPARENT);

        DisplayMetrics metrics = res.getDisplayMetrics();
        this.displayWidth = res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? Math
            .max(metrics.widthPixels, metrics.heightPixels) : Math.min(
            metrics.widthPixels, metrics.heightPixels);
        this.displayHeight = res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? Math
            .min(metrics.widthPixels, metrics.heightPixels) : Math.max(
            metrics.widthPixels, metrics.heightPixels);
    }

    public void loadImages(Context context, int resourceId) {
        Resources res = context.getResources();
        mImages.add(new ImageObject(resourceId, res));
        float cx = SCREEN_MARGIN
            + (float) (Math.random() * (displayWidth - 2 * SCREEN_MARGIN));
        float cy = SCREEN_MARGIN
            + (float) (Math.random() * (displayHeight - 2 * SCREEN_MARGIN));
        mImages.get(mImages.size() - 1).load(context, cx, cy);
        invalidate();
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = mImages.size();
        for (int i = 0; i < n; i++)
            mImages.get(i).draw(canvas);
    }

    public void trackballClicked() {
        mUIMode = (mUIMode + 1) % 3;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return multiTouchController.onTouchEvent(event);
    }

    /**
     * Get the image that is under the single-touch point, or return null
     * (canceling the drag op) if none
     */
    public MultiTouchObject getDraggableObjectAtPoint(PointInfo pt) {
        float x = pt.getX(), y = pt.getY();
        int n = mImages.size();
        for (int i = n - 1; i >= 0; i--) {
            ImageObject im = (ImageObject) mImages.get(i);
            if (im.containsPoint(x, y))
                return im;
        }
        return null;
    }

    /**
     * Select an object for dragging. Called whenever an object is found to be
     * under the point (non-null is returned by getDraggableObjectAtPoint()) and
     * a drag operation is starting. Called with null when drag op ends.
     */
    public void selectObject(MultiTouchObject img, PointInfo touchPoint) {
        currTouchPoint.set(touchPoint);
        if (img != null) {
            // Move image to the top of the stack when selected
            mImages.remove(img);
            mImages.add(img);
        } else {
            // Called with img == null when drag stops.
        }
        invalidate();
    }

    public void deleteSelectedObject() {
        if (mImages.size() > 0) {
            mImages.remove(mImages.size() - 1);
            invalidate();
        }
    }

    /**
     * Get the current position and scale of the selected image. Called whenever
     * a drag starts or is reset.
     */
    public void getPositionAndScale(MultiTouchObject img,
                                    PositionAndScale objPosAndScaleOut) {
        objPosAndScaleOut.set(img.getCenterX(), img.getCenterY(),
            (mUIMode & UI_MODE_ANISOTROPIC_SCALE) == 0,
            (img.getScaleX() + img.getScaleY()) / 2,
            (mUIMode & UI_MODE_ANISOTROPIC_SCALE) != 0, img.getScaleX(),
            img.getScaleY(), (mUIMode & UI_MODE_ROTATE) != 0,
            img.getAngle());
    }

    /** Set the position and scale of the dragged/stretched image. */
    public boolean setPositionAndScale(MultiTouchObject img,
                                       PositionAndScale newImgPosAndScale, PointInfo touchPoint) {
        currTouchPoint.set(touchPoint);
        boolean ok = ((ImageObject) img).setPos(newImgPosAndScale);
        if (ok)
            invalidate();
        return ok;
    }

    public boolean pointInObjectGrabArea(PointInfo pt, MultiTouchObject img) {
        return false;
    }
}
