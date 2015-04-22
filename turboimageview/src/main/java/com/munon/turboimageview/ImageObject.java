package com.munon.turboimageview;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class ImageObject extends MultiTouchObject {
    private static final double INITIAL_SCALE_FACTOR = 0.15;

    private transient Drawable drawable;

    public ImageObject(int resourceId, Resources res) {
        super(res);
        drawable = res.getDrawable(resourceId);
        initPaint();
    }

    public ImageObject(Drawable drawable, Resources res) {
        super(res);
        this.drawable = drawable;
        initPaint();
    }

    public ImageObject(Bitmap bitmap, Resources res) {
        super(res);
        this.drawable = new BitmapDrawable(res, bitmap);
        initPaint();
    }

    public void initPaint() {
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(borderColor);
        borderPaint.setAntiAlias(true);
        borderPaint.setStrokeWidth(3.0f);
        borderPaint.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));
    }

    public void draw(Canvas canvas) {
        canvas.save();

        float dx = (maxX + minX) / 2;
        float dy = (maxY + minY) / 2;

        drawable.setBounds((int) minX, (int) minY, (int) maxX, (int) maxY);
        if (flippedHorizontally) {
            canvas.scale(-1f, 1f, dx, dy);
        }
        canvas.translate(dx, dy);
        if (flippedHorizontally) {
            canvas.rotate(-angle * 180.0f / (float) Math.PI);
        } else {
            canvas.rotate(angle * 180.0f / (float) Math.PI);
        }
        canvas.translate(-dx, -dy);

        drawable.draw(canvas);

        if (isLatestSelected) {
            canvas.drawRect((int) minX, (int) minY, (int) maxX, (int) maxY, borderPaint);
            /*Ready to show an X button to delete the view but impossible to detect when that X button is touched*/
            //canvas.drawBitmap(cancelBitmap, minX - (cancelBitmap.getWidth() / 2), minY - (cancelBitmap.getHeight() / 2), new Paint());
        }

        canvas.restore();
    }

    /**
     * Called by activity's onPause() method to free memory used for loading the images
     */
    @Override
    public void unload() {
        this.drawable = null;
    }

    /** Called by activity's onResume() method to init the images */
    @SuppressWarnings("deprecation")
    @Override
    public void init(Context context, float startMidX, float startMidY) {
        Resources res = context.getResources();
        init(res);

        this.startMidX = startMidX;
        this.startMidY = startMidY;

        width = drawable.getIntrinsicWidth();
        height = drawable.getIntrinsicHeight();

        float centerX;
        float centerY;
        float scaleX;
        float scaleY;
        float angle;
        if (firstLoad) {
            centerX = startMidX;
            centerY = startMidY;

            float scaleFactor = (float) (Math.max(displayWidth, displayHeight) /
                (float) Math.max(width, height) * INITIAL_SCALE_FACTOR);
            scaleX = scaleY = scaleFactor;
            angle = 0.0f;

            firstLoad = false;
        } else {
            centerX = this.centerX;
            centerY = this.centerY;
            scaleX = this.scaleX;
            scaleY = this.scaleY;
            angle = this.angle;
        }
        setPos(centerX, centerY, scaleX, scaleY, angle);
    }
}
