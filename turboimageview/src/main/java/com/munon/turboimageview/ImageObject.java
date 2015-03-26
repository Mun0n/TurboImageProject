package com.munon.turboimageview;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

public class ImageObject extends MultiTouchObject {
    private static final double INITIAL_SCALE_FACTOR = 0.15;

    private transient Drawable mDrawable;
    private Bitmap cancelBitmap;

    private int mResourceId;

    public ImageObject(int resourceId, Resources res) {
        super(res);
        mResourceId = resourceId;
        initPaint();
    }

    public ImageObject(ImageObject imageObject, Resources res) {
        super(res);
        mDrawable = imageObject.mDrawable;
        mResourceId = imageObject.mResourceId;
        mScaleX = imageObject.mScaleX;
        mScaleY = imageObject.mScaleY;
        mCenterX = imageObject.mCenterX;
        mCenterY = imageObject.mCenterY;
        mAngle = imageObject.mAngle;
        initPaint();
    }

    public void initPaint() {
        //cancelBitmap = BitmapFactory.decodeResource(res, R.drawable.cancel);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(borderColor);
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
            /*Ready to show an X button to delete the view but impossible to detect when that X button is touched*/
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
    @SuppressWarnings("deprecation")
    @Override
    public void load(Context context, float startMidX, float startMidY) {
        Resources res = context.getResources();
        init(res);

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
        setPos(centerX, centerY, scaleX, scaleY, angle);
    }
}
