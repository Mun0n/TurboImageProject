package com.munon.turboimageview;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import java.io.Serializable;

public abstract class MultiTouchObject implements Serializable {

    protected boolean firstLoad = true;

    protected transient Paint paint = new Paint();
    protected final transient Paint borderPaint = new Paint();

    protected static final int DEFAULT_BORDER_COLOR = Color.BLACK;
    protected int borderColor = DEFAULT_BORDER_COLOR;

    protected int width;
    protected int height;

    // width/height of screen
    protected int displayWidth;
    protected int displayHeight;

    protected float centerX;
    protected float centerY;
    protected float scaleX;
    protected float scaleY;
    protected float angle;

    protected float minX;
    protected float maxX;
    protected float minY;
    protected float maxY;

    protected final static int GRAB_AREA_SIZE = 40;
    protected boolean isGrabAreaSelected = false;
    protected boolean isLatestSelected = false;

    protected float grabAreaX1;
    protected float grabAreaY1;
    protected float grabAreaX2;
    protected float grabAreaY2;

    protected float startMidX;
    protected float startMidY;

    protected boolean flippedHorizontally;

    private static final int UI_MODE_ROTATE = 1;
    private static final int UI_MODE_ANISOTROPIC_SCALE = 2;
    protected final int mUIMode = UI_MODE_ROTATE;

    public MultiTouchObject(Resources res) {
        init(res);
    }

    protected void init(Resources res) {
        flippedHorizontally = false;

        DisplayMetrics metrics = res.getDisplayMetrics();
        displayWidth =
            (res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                ? Math.max(metrics.widthPixels, metrics.heightPixels)
                : Math.min(metrics.widthPixels, metrics.heightPixels);
        displayHeight =
            (res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                ? Math.min(metrics.widthPixels, metrics.heightPixels)
                : Math.max(metrics.widthPixels, metrics.heightPixels);
    }

    /**
     * Set the position and scale of an image in screen coordinates
     */
    public boolean setPos(PositionAndScale newImgPosAndScale) {
        float newScaleX;
        float newScaleY;

        if ((mUIMode & UI_MODE_ANISOTROPIC_SCALE) != 0) {
            newScaleX = newImgPosAndScale.getScaleX();
        } else {
            newScaleX = newImgPosAndScale.getScale();
        }

        if ((mUIMode & UI_MODE_ANISOTROPIC_SCALE) != 0) {
            newScaleY = newImgPosAndScale.getScaleY();
        } else {
            newScaleY = newImgPosAndScale.getScale();
        }

        return setPos(newImgPosAndScale.getXOff(),
            newImgPosAndScale.getYOff(),
            newScaleX,
            newScaleY,
            newImgPosAndScale.getAngle());
    }

    /**
     * Set the position and scale of an image in screen coordinates
     */
    protected boolean setPos(float centerX, float centerY,
                             float scaleX, float scaleY, float angle) {
        float ws = (width / 2) * scaleX;
        float hs = (height / 2) * scaleY;

        minX = centerX - ws;
        minY = centerY - hs;
        maxX = centerX + ws;
        maxY = centerY + hs;

        grabAreaX1 = maxX - GRAB_AREA_SIZE;
        grabAreaY1 = maxY - GRAB_AREA_SIZE;
        grabAreaX2 = maxX;
        grabAreaY2 = maxY;

        this.centerX = centerX;
        this.centerY = centerY;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.angle = angle;

        return true;
    }

    /**
     * Return whether or not the given screen coords are inside this image
     */
    public boolean containsPoint(float touchX, float touchY) {
        return (touchX >= minX && touchX <= maxX && touchY >= minY && touchY <= maxY);
    }

    public boolean grabAreaContainsPoint(float touchX, float touchY) {
        return (touchX >= grabAreaX1 && touchX <= grabAreaX2 &&
            touchY >= grabAreaY1 && touchY <= grabAreaY2);
    }

    public void reload(Context context) {
        firstLoad = false; // Let the init know properties have changed so reload those,
        // don't go back and start with defaults
        init(context, centerX, centerY);
    }

    public abstract void draw(Canvas canvas);

    public abstract void init(Context context, float startMidX, float startMidY);

    public abstract void unload();

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getCenterX() {
        return centerX;
    }

    public float getCenterY() {
        return centerY;
    }

    public float getScaleX() {
        return scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    public float getAngle() {
        return angle;
    }

    public float getMinX() {
        return minX;
    }

    public float getMaxX() {
        return maxX;
    }

    public float getMinY() {
        return minY;
    }

    public float getMaxY() {
        return maxY;
    }

    public void setIsGrabAreaSelected(boolean selected) {
        isGrabAreaSelected = selected;
    }

    public boolean isGrabAreaSelected() {
        return isGrabAreaSelected;
    }

    public boolean isSelected() {
        return isLatestSelected;
    }

    public void setSelected(boolean selected) {
        this.isLatestSelected = selected;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
        borderPaint.setColor(borderColor);
    }

    public boolean isFlippedHorizontally() {
        return flippedHorizontally;
    }

    public void setFlippedHorizontally(boolean flipped) {
        this.flippedHorizontally = flipped;
    }
}
