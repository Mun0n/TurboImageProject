package com.munon.turboimageview;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

public class ImageObject extends MultiTouchObject {

    private static final double INITIAL_SCALE_FACTOR = 0.15;

    private transient Drawable mDrawable;
    private Bitmap cancelBitmap;
    private Paint borderPaint;
    private Resources res;

    private int mResourceId;

    public ImageObject(int resourceId, Resources res) {
        super(res);
        this.res = res;
        mResourceId = resourceId;
        initPaint();
    }

    public ImageObject(ImageObject e, Resources res) {
        super(res);
        this.res = res;
        mDrawable = e.mDrawable;
        mResourceId = e.mResourceId;
        mScaleX = e.mScaleX;
        mScaleY = e.mScaleY;
        mCenterX = e.mCenterX;
        mCenterY = e.mCenterY;
        mAngle = e.mAngle;
        initPaint();
    }

    public void initPaint() {
        //cancelBitmap = BitmapFactory.decodeResource(res, R.drawable.cancel);

        borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.BLACK);
        borderPaint.setAntiAlias(true);
        borderPaint.setStrokeWidth(3.0f);
        borderPaint.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));
    }

    public void draw(Canvas canvas) {
        canvas.save();

        float dx = (mMaxX + mMinX) / 2;
        float dy = (mMaxY + mMinY) / 2;

        mDrawable.setBounds((int) mMinX, (int) mMinY, (int) mMaxX, (int) mMaxY);

        canvas.translate(dx, dy);
        canvas.rotate(mAngle * 180.0f / (float) Math.PI);
        canvas.translate(-dx, -dy);

        mDrawable.draw(canvas);

        if (mIsLatestSelected) {
            canvas.drawRect((int) mMinX, (int) mMinY, (int) mMaxX, (int) mMaxY, borderPaint);
            /*Ready to show an x to delete the view but imposible to detect when the x is touched*/
            //canvas.drawBitmap(cancelBitmap, mMinX - (cancelBitmap.getWidth() / 2), mMinY - (cancelBitmap.getHeight() / 2), new Paint());
        }

        canvas.restore();
    }

    /**
     * Called by activity's onPause() method to free memory used for loading the images
     */
    @Override
    public void unload() {
        this.mDrawable = null;
    }

    /** Called by activity's onResume() method to load the images */
    @Override
    public void load(Context context, float startMidX, float startMidY) {
        Resources res = context.getResources();
        getMetrics(res);

        mStartMidX = startMidX;
        mStartMidY = startMidY;

        mDrawable = res.getDrawable(mResourceId);

        mWidth = mDrawable.getIntrinsicWidth();
        mHeight = mDrawable.getIntrinsicHeight();

        float centerX;
        float centerY;
        float scaleX;
        float scaleY;
        float angle;
        if (mFirstLoad) {
            centerX = startMidX;
            centerY = startMidY;

            float scaleFactor = (float) (Math.max(mDisplayWidth, mDisplayHeight) /
                (float) Math.max(mWidth, mHeight) * INITIAL_SCALE_FACTOR);
            scaleX = scaleY = scaleFactor;
            angle = 0.0f;

            mFirstLoad = false;
        } else {
            centerX = mCenterX;
            centerY = mCenterY;
            scaleX = mScaleX;
            scaleY = mScaleY;
            angle = mAngle;
        }
        setPos(centerX, centerY, scaleX, scaleY, mAngle);
    }

    public void setLastSelected(boolean selected) {
        mIsLatestSelected = selected;
    }

    public boolean isSelected() {
        return mIsLatestSelected;
    }
}
