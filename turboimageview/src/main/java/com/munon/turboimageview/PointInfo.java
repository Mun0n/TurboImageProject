package com.munon.turboimageview;

/**
 * A class that packages up all MotionEvent information with all derived
 * multitouch information (if available)
 */

public class PointInfo {
    // Multitouch information
    private int numPoints;
    private float[] xs = new float[MultiTouchController.MAX_TOUCH_POINTS];
    private float[] ys = new float[MultiTouchController.MAX_TOUCH_POINTS];
    private float[] pressures = new float[MultiTouchController.MAX_TOUCH_POINTS];
    private int[] pointerIds = new int[MultiTouchController.MAX_TOUCH_POINTS];

    // Midpoint of pinch operations
    private float xMid;
    private float yMid;
    private float pressureMid;

    // Width/diameter/angle of pinch operations
    private float dx;
    private float dy;
    private float diameter;
    private float diameterSq;
    private float angle;

    // Whether or not there is at least one finger down (isDown) and/or at
    // least two fingers down (isMultiTouch)
    private boolean isDown;
    private boolean isMultiTouch;

    // Whether or not these fields have already been calculated, for caching purposes
    private boolean diameterSqIsCalculated;
    private boolean diameterIsCalculated;
    private boolean angleIsCalculated;

    // Event action code and event time
    private int action;
    private long eventTime;

    /** Set all point info */
    protected void set(int numPoints, float[] x, float[] y,
                       float[] pressure, int[] pointerIds, int action,
                       boolean isDown, long eventTime) {
        this.eventTime = eventTime;
        this.action = action;
        this.numPoints = numPoints;
        for (int i = 0; i < numPoints; i++) {
            this.xs[i] = x[i];
            this.ys[i] = y[i];
            this.pressures[i] = pressure[i];
            this.pointerIds[i] = pointerIds[i];
        }
        this.isDown = isDown;
        this.isMultiTouch = numPoints >= 2;

        if (isMultiTouch) {
            xMid = (x[0] + x[1]) * .5f;
            yMid = (y[0] + y[1]) * .5f;
            pressureMid = (pressure[0] + pressure[1]) * .5f;
            dx = Math.abs(x[1] - x[0]);
            dy = Math.abs(y[1] - y[0]);

        } else {
            // Single-touch event
            xMid = x[0];
            yMid = y[0];
            pressureMid = pressure[0];
            dx = dy = 0.0f;
        }
        // Need to re-calculate the expensive params if they're needed
        diameterSqIsCalculated = diameterIsCalculated = angleIsCalculated = false;
    }

    /**
     * Copy all fields from one PointInfo class to another. PointInfo objects
     * are volatile so you should use this if you want to keep track of the last
     * touch event in your own code.
     */
    public void set(PointInfo other) {
        this.numPoints = other.numPoints;
        for (int i = 0; i < numPoints; i++) {
            this.xs[i] = other.xs[i];
            this.ys[i] = other.ys[i];
            this.pressures[i] = other.pressures[i];
            this.pointerIds[i] = other.pointerIds[i];
        }
        this.xMid = other.xMid;
        this.yMid = other.yMid;
        this.pressureMid = other.pressureMid;
        this.dx = other.dx;
        this.dy = other.dy;
        this.diameter = other.diameter;
        this.diameterSq = other.diameterSq;
        this.angle = other.angle;
        this.isDown = other.isDown;
        this.action = other.action;
        this.isMultiTouch = other.isMultiTouch;
        this.diameterIsCalculated = other.diameterIsCalculated;
        this.diameterSqIsCalculated = other.diameterSqIsCalculated;
        this.angleIsCalculated = other.angleIsCalculated;
        this.eventTime = other.eventTime;
    }

    /** True if number of touch points >= 2. */
    public boolean isMultiTouch() {
        return isMultiTouch;
    }

    /** Difference between x coords of touchpoint 0 and 1. */
    public float getMultiTouchWidth() {
        return isMultiTouch ? dx : 0.0f;
    }

    /** Difference between y coords of touchpoint 0 and 1. */
    public float getMultiTouchHeight() {
        return isMultiTouch ? dy : 0.0f;
    }

    /**
     * Fast integer sqrt, by Jim Ulery. Much faster than Math.sqrt()
     * for integers.
     */
    private int julery_isqrt(int val) {
        int temp, g = 0, b = 0x8000, bshft = 15;
        do {
            if (val >= (temp = (((g << 1) + b) << bshft--))) {
                g += b;
                val -= temp;
            }
        } while ((b >>= 1) > 0);
        return g;
    }

    /**
     * Calculate the squared diameter of the multitouch event, and cache it.
     * Use this if you don't need to perform the sqrt.
     */
    public float getMultiTouchDiameterSq() {
        if (!diameterSqIsCalculated) {
            diameterSq = (isMultiTouch ? dx * dx + dy * dy : 0.0f);
            diameterSqIsCalculated = true;
        }
        return diameterSq;
    }

    /**
     * Calculate the diameter of the multitouch event, and cache it. Uses fast
     * int sqrt but gives accuracy to 1/16px.
     */
    public float getMultiTouchDiameter() {
        if (!diameterIsCalculated) {
            if (!isMultiTouch) {
                diameter = 0.0f;
            } else {
                // Get 1/16 pixel's worth of subpixel accuracy, works on
                // screens up to 2048x2048
                // before we get overflow (at which point you can reduce or
                // eliminate subpix accuracy, or use longs in julery_isqrt())
                float diamSq = getMultiTouchDiameterSq();
                diameter = (diamSq == 0.0f ? 0.0f
                    : (float) julery_isqrt((int) (256 * diamSq)) / 16.0f);
                // Make sure diameter is never less than dx or dy, for trig purposes
                if (diameter < dx) {
                    diameter = dx;
                }
                if (diameter < dy) {
                    diameter = dy;
                }
            }
            diameterIsCalculated = true;
        }
        return diameter;
    }

    /**
     * Calculate the angle of a multitouch event, and cache it.
     * Actually gives the smaller of the two angles between the x axis and the line
     * between the two touchpoints, so range is [0,Math.PI/2]. Uses Math.atan2().
     */
    public float getMultiTouchAngle() {
        if (!angleIsCalculated) {
            if (!isMultiTouch) {
                angle = 0.0f;
            } else {
                angle = (float) Math.atan2(ys[1] - ys[0], xs[1] - xs[0]);
                angleIsCalculated = true;
            }
        }
        return angle;
    }

    /** Return the total number of touch points */
    public int getNumTouchPoints() {
        return numPoints;
    }

    /**
     * Return the X coord of the first touch point if there's only one,
     * or the midpoint between first and second touch points if two or more.
     */
    public float getX() {
        return xMid;
    }

    /**
     * Return the array of X coords -- only the first getNumTouchPoints()
     * of these is defined.
     */
    public float[] getXs() {
        return xs;
    }

    /**
     * Return the X coord of the first touch point if there's only one,
     * or the midpoint between first and second touch points if two or more.
     */
    public float getY() {
        return yMid;
    }

    /**
     * Return the array of Y coords -- only the first getNumTouchPoints()
     * of these is defined.
     */
    public float[] getYs() {
        return ys;
    }

    /**
     * Return the array of pointer ids -- only the first getNumTouchPoints()
     * of these is defined. These don't have to be all the numbers from 0 to
     * getNumTouchPoints()-1 inclusive, numbers can be skipped if a finger is
     * lifted and the touch sensor is capable of detecting that that
     * particular touch point is no longer down. Note that a lot of sensors do
     * not have this capability: when finger 1 is lifted up finger 2
     * becomes the new finger 1.  However in theory these IDs can correct for
     * that.  Convert back to indices using MotionEvent.findPointerIndex().
     */
    public int[] getPointerIds() {
        return pointerIds;
    }

    /**
     * Return the pressure the first touch point if there's only one,
     * or the average pressure of first and second touch points if two or more.
     */
    public float getPressure() {
        return pressureMid;
    }

    /**
     * Return the array of pressures -- only the first getNumTouchPoints()
     * of these is defined.
     */
    public float[] getPressures() {
        return pressures;
    }

    public boolean isDown() {
        return isDown;
    }

    public int getAction() {
        return action;
    }

    public long getEventTime() {
        return eventTime;
    }
}
