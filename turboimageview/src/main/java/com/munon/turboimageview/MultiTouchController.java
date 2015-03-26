package com.munon.turboimageview;

import android.util.Log;
import android.view.MotionEvent;
import java.lang.reflect.Method;

public class MultiTouchController<T> {
    private static final String TAG = "MultiTouchController";

    private static final long EVENT_SETTLE_TIME_INTERVAL = 20;
    private static final float MAX_MULTITOUCH_POS_JUMP_SIZE = 30.0f;
    private static final float MAX_MULTITOUCH_DIM_JUMP_SIZE = 40.0f;
    private static final float MIN_MULTITOUCH_SEPARATION = 30.0f;
    private static final float THRESHOLD = 3.0f;
    public static final int MAX_TOUCH_POINTS = 20;

    private MultiTouchObjectCanvas<T> objectCanvas;

    private PointInfo mCurrentTouchPoint;
    private PointInfo mPrevTouchPoint;

    /** Fields extracted from mCurrentTouchPoint */
    private float mCurrentTouchPointX;
    private float mCurrentTouchPointY;
    private float mCurrentTouchPointDiam;
    private float mCurrentTouchPointWidth;
    private float mCurrentTouchPointHeight;
    private float mCurrentTouchPointAng;

    /**
     * Extract fields from mCurrentTouchPoint, respecting the update* fields of mCurrentTouchPoint.
     * This just avoids code duplication. I hate that Java doesn't support
     * higher-order functions, tuples or multiple return values from functions.
     */
    private void extractCurrentTouchPointInfo() {
        // Get new drag/pinch params. Only read multitouch fields that are needed,
        // to avoid unnecessary computation (diameter and angle are expensive operations).
        mCurrentTouchPointX = mCurrentTouchPoint.getX();
        mCurrentTouchPointY = mCurrentTouchPoint.getY();
        mCurrentTouchPointDiam = Math.max(MIN_MULTITOUCH_SEPARATION * .71f,
            !mCurrentTouchPointPosAndScale.isUpdateScale() ? 0.0f : mCurrentTouchPoint.getMultiTouchDiameter());
        mCurrentTouchPointWidth = Math.max(MIN_MULTITOUCH_SEPARATION,
            !mCurrentTouchPointPosAndScale.isUpdateScaleXY() ? 0.0f : mCurrentTouchPoint.getMultiTouchWidth());
        mCurrentTouchPointHeight = Math.max(MIN_MULTITOUCH_SEPARATION,
            !mCurrentTouchPointPosAndScale.isUpdateScaleXY() ? 0.0f : mCurrentTouchPoint.getMultiTouchHeight());
        mCurrentTouchPointAng = !mCurrentTouchPointPosAndScale.isUpdateAngle() ? 0.0f : mCurrentTouchPoint.getMultiTouchAngle();
    }

    /**
     * Whether to handle single-touch events/drags before multi-touch is
     * initiated or not; if not, they are handled by subclasses
     */
    private boolean handleSingleTouchEvents;

    /** The object being dragged/stretched */
    private T selectedObject = null;

    private final PositionAndScale mCurrentTouchPointPosAndScale = new PositionAndScale();

    /**
     * Drag/pinch start time and time to ignore spurious events until
     * (to smooth over event noise)
     */
    private long mSettleStartTime;
    private long mSettleEndTime;

    /** Conversion from object coords to screen coords */
    private float startPosX;
    private float startPosY;

    /** Conversion between scale and width, and object angle and start pinch angle */
    private float startScaleOverPinchDiam;
    private float startAngleMinusPinchAngle;

    /** Conversion between X scale and width, and Y scale and height */
    private float startScaleXOverPinchWidth;
    private float startScaleYOverPinchHeight;

    /** Whether the current object has moved beyond THRESHOLD */
    private boolean mDragOccurred = false;

    private static final int MODE_NOTHING = 0;
    private static final int MODE_DRAG = 1;
    private static final int MODE_PINCH = 2;
    private static final int MODE_ST_GRAB = 3;

    /** Current drag mode */
    private int mMode = MODE_NOTHING;

    public MultiTouchController(MultiTouchObjectCanvas<T> objectCanvas) {
        this(objectCanvas, true);
    }

    public MultiTouchController(MultiTouchObjectCanvas<T> objectCanvas,
                                boolean handleSingleTouchEvents) {
        this.mCurrentTouchPoint = new PointInfo();
        this.mPrevTouchPoint = new PointInfo();
        this.handleSingleTouchEvents = handleSingleTouchEvents;
        this.objectCanvas = objectCanvas;
    }

    /**
     * Whether to handle single-touch events/drags before multi-touch is
     * initiated or not; if not, they are handled by subclasses. Default: true
     */
    protected void setHandleSingleTouchEvents(boolean handleSingleTouchEvents) {
        this.handleSingleTouchEvents = handleSingleTouchEvents;
    }

    /**
     * Whether to handle single-touch events/drags before multi-touch is
     * initiated or not; if not, they are handled by subclasses. Default: true
     */
    protected boolean getHandleSingleTouchEvents() {
        return handleSingleTouchEvents;
    }

    public boolean dragOccurred() {
        return mDragOccurred;
    }

    private static final boolean multiTouchSupported;
    private static Method m_getPointerCount;
    private static Method m_getPointerId;
    private static Method m_getPressure;
    private static Method m_getHistoricalX;
    private static Method m_getHistoricalY;
    private static Method m_getHistoricalPressure;
    private static Method m_getX;
    private static Method m_getY;
    private static int ACTION_POINTER_UP = 6;
    private static int ACTION_POINTER_INDEX_SHIFT = 8;

    static {
        boolean succeeded = false;
        try {
            // Android 2.0.1 stuff:
            m_getPointerCount = MotionEvent.class.getMethod("getPointerCount");
            m_getPointerId = MotionEvent.class.getMethod("getPointerId", Integer.TYPE);
            m_getPressure = MotionEvent.class.getMethod("getPressure", Integer.TYPE);
            m_getHistoricalX = MotionEvent.class.getMethod("getHistoricalX",
                Integer.TYPE, Integer.TYPE);
            m_getHistoricalY = MotionEvent.class.getMethod("getHistoricalY",
                Integer.TYPE, Integer.TYPE);
            m_getHistoricalPressure = MotionEvent.class.
                getMethod("getHistoricalPressure", Integer.TYPE, Integer.TYPE);
            m_getX = MotionEvent.class.getMethod("getX", Integer.TYPE);
            m_getY = MotionEvent.class.getMethod("getY", Integer.TYPE);
            succeeded = true;
        } catch (Exception e) {
            Log.e(TAG, "static initializer failed", e);
        }

        multiTouchSupported = succeeded;
        if (multiTouchSupported) {
            // Android 2.2+ stuff (the original Android 2.2 consts are declared above,
            // and these actions aren't used previous to Android 2.2):
            try {
                ACTION_POINTER_UP = MotionEvent.class
                    .getField("ACTION_POINTER_UP").getInt(null);
                ACTION_POINTER_INDEX_SHIFT = MotionEvent.class
                    .getField("ACTION_POINTER_INDEX_SHIFT").getInt(null);
            } catch (Exception ignored) {
            }
        }
    }

    private static final float[] xVals = new float[MAX_TOUCH_POINTS];
    private static final float[] yVals = new float[MAX_TOUCH_POINTS];
    private static final float[] pressureVals = new float[MAX_TOUCH_POINTS];
    private static final int[] pointerIds = new int[MAX_TOUCH_POINTS];

    /** Process incoming touch events */
    public boolean onTouchEvent(MotionEvent event) {
        try {
            int pointerCount = multiTouchSupported ?
                (Integer) m_getPointerCount.invoke(event) : 1;
            if (getMode() == MODE_NOTHING && !handleSingleTouchEvents && pointerCount == 1)
                // Not handling initial single touch events, just pass them on
                return false;
            int action = event.getAction();
            int histLen = event.getHistorySize() / pointerCount;
            for (int histIdx = 0; histIdx <= histLen; histIdx++) {
                // Read from history entries until histIdx == histLen,
                // then read from current event
                boolean processingHist = histIdx < histLen;
                if (!multiTouchSupported || pointerCount == 1) {
                    // Use single-pointer methods -- these are needed as a special
                    // case (for some weird reason) even if
                    // multitouch is supported but there's only one touch point down
                    // currently -- event.getX(0) etc. throw an exception if there's
                    // only one point down.
                    xVals[0] = processingHist ? event.getHistoricalX(histIdx)
                        : event.getX();
                    yVals[0] = processingHist ? event.getHistoricalY(histIdx)
                        : event.getY();
                    pressureVals[0] = processingHist
                        ? event.getHistoricalPressure(histIdx) : event.getPressure();
                } else {
                    int numPointers = Math.min(pointerCount, MAX_TOUCH_POINTS);
                    for (int ptrIdx = 0; ptrIdx < numPointers; ptrIdx++) {
                        int ptrId = (Integer) m_getPointerId.invoke(event, ptrIdx);
                        pointerIds[ptrIdx] = ptrId;
                        // N.B. if pointerCount == 1, then the following methods throw
                        // an array index out of range exception, and the code above
                        // is therefore required not just for Android 1.5/1.6 but
                        // also for when there is only one touch point on the screen --
                        // pointlessly inconsistent :(
                        xVals[ptrIdx] = (Float) (processingHist
                            ? m_getHistoricalX.invoke(event, ptrIdx, histIdx)
                            : m_getX.invoke(event, ptrIdx));
                        yVals[ptrIdx] = (Float) (processingHist
                            ? m_getHistoricalY.invoke(event, ptrIdx, histIdx)
                            : m_getY.invoke(event, ptrIdx));
                        pressureVals[ptrIdx] = (Float) (processingHist
                            ? m_getHistoricalPressure.invoke(event, ptrIdx, histIdx)
                            : m_getPressure.invoke(event, ptrIdx));
                    }
                }

                // Decode event
                decodeTouchEvent(pointerCount, xVals, yVals, pressureVals, pointerIds,
                        /* action = */processingHist ? MotionEvent.ACTION_MOVE : action,
                        /* down = */processingHist ? true : action !=
                        MotionEvent.ACTION_UP && (action &
                        ((1 << ACTION_POINTER_INDEX_SHIFT) - 1)) != ACTION_POINTER_UP
                        && action != MotionEvent.ACTION_CANCEL, //
                    processingHist ? event.getHistoricalEventTime(histIdx) :
                        event.getEventTime());
            }

            return true;
        } catch (Exception e) {
            // In case any of the introspection stuff fails (it shouldn't)
            Log.e(TAG, "onTouchEvent() failed", e);
            return false;
        }
    }

    private void decodeTouchEvent(int pointerCount, float[] x, float[] y,
                                  float[] pressure, int[] pointerIds, int action, boolean down,
                                  long eventTime) {
        // Swap curr/prev points
        PointInfo tmp = mPrevTouchPoint;
        mPrevTouchPoint = mCurrentTouchPoint;
        mCurrentTouchPoint = tmp;
        // Overwrite old prev point
        mCurrentTouchPoint.set(pointerCount, x, y, pressure, pointerIds, action, down, eventTime);
        multiTouchController();
    }

    // ---------------------------------------------------------------------------

    /**
     * Start dragging/pinching, or reset drag/pinch to current point if something
     * goes out of range
     */
    private void anchorAtThisPositionAndScale() {
        if (selectedObject == null) {
            return;
        }

        // Get selected object's current position and scale
        objectCanvas.getPositionAndScale(selectedObject, mCurrentTouchPointPosAndScale);

        // Figure out the object coords of the drag start point's screen coords.
        // All stretching should be around this point in object-coord-space.
        // Also figure out out ratio between object scale factor and multitouch
        // diameter at beginning of drag; same for angle and optional anisotropic
        // scale.
        float currScaleInv = 1.0f / (!mCurrentTouchPointPosAndScale.isUpdateScale() ? 1.0f
            : mCurrentTouchPointPosAndScale.getScale() == 0.0f ? 1.0f : mCurrentTouchPointPosAndScale.getScale());
        extractCurrentTouchPointInfo();
        startPosX = (mCurrentTouchPointX - mCurrentTouchPointPosAndScale.getXOff()) * currScaleInv;
        startPosY = (mCurrentTouchPointY - mCurrentTouchPointPosAndScale.getYOff()) * currScaleInv;
        startScaleOverPinchDiam = mCurrentTouchPointPosAndScale.getScale() / mCurrentTouchPointDiam;
        startScaleXOverPinchWidth = mCurrentTouchPointPosAndScale.getScaleX() / mCurrentTouchPointWidth;
        startScaleYOverPinchHeight = mCurrentTouchPointPosAndScale.getScaleY() / mCurrentTouchPointHeight;
        startAngleMinusPinchAngle = mCurrentTouchPointPosAndScale.getAngle() - mCurrentTouchPointAng;
    }

    /**
     * Drag/stretch/rotate the selected object using the current touch
     * position(s) relative to the anchor position(s).
     */
    private void performDragOrPinch() {
        if (selectedObject == null) {
            return;
        }

        // Calc new position of dragged object
        float currScale = !mCurrentTouchPointPosAndScale.isUpdateScale() ? 1.0f
            : mCurrentTouchPointPosAndScale.getScale() == 0.0f ? 1.0f : mCurrentTouchPointPosAndScale.getScale();
        extractCurrentTouchPointInfo();
        float newPosX = mCurrentTouchPointX - startPosX * currScale;
        float newPosY = mCurrentTouchPointY - startPosY * currScale;

        // Calc new angle of object, if any
        //float newAngle = mCurrentTouchPointAng;
        //if (getMode() == MODE_ST_GRAB) {
        //    // grabbed by single touch area
        //    newAngle = (float) Math.atan2(newPosY-startPosY,
        //            newPosX-startPosX) + mCurrentTouchPointAng;
        //} else {
        //    newAngle = startAngleMinusPinchAngle + mCurrentTouchPointAng;
        //}

        float deltaX = mCurrentTouchPoint.getX() - mPrevTouchPoint.getX();
        float deltaY = mCurrentTouchPoint.getY() - mPrevTouchPoint.getY();

        // Calc new scale of object, if any
        float newScale;
        if (getMode() == MODE_ST_GRAB) {
            if (deltaX < 0.0f || deltaY < 0.0f) {
                newScale = mCurrentTouchPointPosAndScale.getScale() - 0.04f;
            } else {
                newScale = mCurrentTouchPointPosAndScale.getScale() + 0.04f;
            }
            if (newScale < 0.35f) return;
        } else {
            newScale = startScaleOverPinchDiam * mCurrentTouchPointDiam;
        }

        if (!mDragOccurred) {
            if (!pastThreshold(Math.abs(deltaX), Math.abs(deltaY), newScale)) {
                return;
            }
        }

        float newScaleX = startScaleXOverPinchWidth * mCurrentTouchPointWidth;
        float newScaleY = startScaleYOverPinchHeight * mCurrentTouchPointHeight;
        float newAngle = startAngleMinusPinchAngle + mCurrentTouchPointAng;

        // Set the new obj coords, scale, and angle as appropriate
        // (notifying the subclass of the change).
        mCurrentTouchPointPosAndScale.set(newPosX, newPosY, newScale, newScaleX, newScaleY, newAngle);

        objectCanvas.setPositionAndScale(selectedObject,
            mCurrentTouchPointPosAndScale, mCurrentTouchPoint);
        mDragOccurred = true;
    }

    /**
     * Returns true if selectedObject has moved passed the movement THRESHOLD,
     * otherwise false.
     * This serves to help avoid small jitters in the object when the user
     * places their finger on the object without intending to move it.
     */
    private boolean pastThreshold(float deltaX, float deltaY, float newScale) {
        if (deltaX < THRESHOLD && deltaY < THRESHOLD) {
            if (newScale == mCurrentTouchPointPosAndScale.getScale()) {
                mDragOccurred = false;
                return false;
            }
        }
        mDragOccurred = true;
        return true;
    }

    /**
     * State-based controller for tracking switches between no-touch,
     * single-touch and multi-touch situations. Includes logic for cleaning up the
     * event stream, as events around touch up/down are noisy at least on
     * early Synaptics sensors.
     */
    private void multiTouchController() {
        switch (mMode) {
            case MODE_NOTHING:
                if (mCurrentTouchPoint.isDown()) {
                    // Start a new single-point drag
                    selectedObject = objectCanvas.getDraggableObjectAtPoint(mCurrentTouchPoint);
                    if (selectedObject != null) {
                        objectCanvas.deselectAll();
                        ((ImageObject) selectedObject).setSelected(true);
                        if (objectCanvas.pointInObjectGrabArea(mCurrentTouchPoint, selectedObject)) {
                            // Started a new single-point scale/rotate
                            setMode(MODE_ST_GRAB);

                            objectCanvas.selectObject(selectedObject, mCurrentTouchPoint);
                            anchorAtThisPositionAndScale();
                            mSettleStartTime = mSettleEndTime = mCurrentTouchPoint.getEventTime();
                        } else {
                            // Started a new single-point drag
                            setMode(MODE_DRAG);

                            objectCanvas.selectObject(selectedObject, mCurrentTouchPoint);
                            anchorAtThisPositionAndScale();
                            // Don't need any settling time if just placing one finger,
                            // there is no noise
                            mSettleStartTime = mSettleEndTime = mCurrentTouchPoint.getEventTime();
                        }
                    }
                } else {
                    objectCanvas.canvasTouched();
                }
                break;

            case MODE_ST_GRAB:
                if (!mCurrentTouchPoint.isDown()) {
                    // First finger was released, stop scale/rotate
                    setMode(MODE_NOTHING);

                    objectCanvas.selectObject((selectedObject = null), mCurrentTouchPoint);
                    mDragOccurred = false;
                } else {
                    // Point 1 is still down, do scale/rotate
                    performDragOrPinch();
                }
                break;

            case MODE_DRAG:
                if (!mCurrentTouchPoint.isDown()) {
                    // First finger was released, stop dragging
                    setMode(MODE_NOTHING);

                    objectCanvas.selectObject((selectedObject = null), mCurrentTouchPoint);
                    mDragOccurred = false;
                } else if (mCurrentTouchPoint.isMultiTouch()) {
                    // Point 1 was already down and point 2 was just placed down
                    setMode(MODE_PINCH);

                    // Restart the drag with the new drag position (that is at the
                    // midpoint between the touchpoints)
                    anchorAtThisPositionAndScale();
                    // Need to let events settle before moving things,
                    // to help with event noise on touchdown
                    mSettleStartTime = mCurrentTouchPoint.getEventTime();
                    mSettleEndTime = mSettleStartTime + EVENT_SETTLE_TIME_INTERVAL;

                } else {
                    // Point 1 is still down and point 2 did not change state,
                    // just do single-point drag to new location
                    if (mCurrentTouchPoint.getEventTime() < mSettleEndTime) {
                        // Ignore the first few events if we just stopped stretching,
                        // because if finger 2 was kept down while
                        // finger 1 is lifted, then point 1 gets mapped to finger 2.
                        // Restart the drag from the new position.
                        anchorAtThisPositionAndScale();
                    } else {
                        // Keep dragging, move to new point
                        performDragOrPinch();
                    }
                }
                break;

            case MODE_PINCH:
                if (!mCurrentTouchPoint.isMultiTouch() || !mCurrentTouchPoint.isDown()) {
                    // Dropped one or both points, stop stretching
                    if (!mCurrentTouchPoint.isDown()) {
                        // Dropped both points, go back to doing nothing
                        setMode(MODE_NOTHING);

                        objectCanvas.selectObject((selectedObject = null), mCurrentTouchPoint);

                    } else {
                        // Just dropped point 2, downgrade to a single-point drag
                        setMode(MODE_DRAG);

                        // Restart the pinch with the single-finger position
                        anchorAtThisPositionAndScale();
                        // Ignore the first few events after the drop, in case we
                        // dropped finger 1 and left finger 2 down
                        mSettleStartTime = mCurrentTouchPoint.getEventTime();
                        mSettleEndTime = mSettleStartTime + EVENT_SETTLE_TIME_INTERVAL;
                    }

                } else {
                    // Still pinching
                    if (Math.abs(mCurrentTouchPoint.getX() - mPrevTouchPoint.getX()) >
                        MAX_MULTITOUCH_POS_JUMP_SIZE
                        || Math.abs(mCurrentTouchPoint.getY() - mPrevTouchPoint.getY()) >
                        MAX_MULTITOUCH_POS_JUMP_SIZE
                        || Math.abs(mCurrentTouchPoint.getMultiTouchWidth() -
                        mPrevTouchPoint.getMultiTouchWidth()) * .5f >
                        MAX_MULTITOUCH_DIM_JUMP_SIZE
                        || Math.abs(mCurrentTouchPoint.getMultiTouchHeight() -
                        mPrevTouchPoint.getMultiTouchHeight()) * .5f >
                        MAX_MULTITOUCH_DIM_JUMP_SIZE) {
                        // Jumped too far, probably event noise, reset and ignore events
                        // for a bit
                        anchorAtThisPositionAndScale();
                        mSettleStartTime = mCurrentTouchPoint.getEventTime();
                        mSettleEndTime = mSettleStartTime + EVENT_SETTLE_TIME_INTERVAL;

                    } else if (mCurrentTouchPoint.getEventTime() < mSettleEndTime) {
                        // Events have not yet settled, reset
                        anchorAtThisPositionAndScale();
                    } else {
                        // Stretch to new position and size
                        performDragOrPinch();
                    }
                }
                break;
        }
    }

    public int getMode() {
        return mMode;
    }

    public void setMode(int newMode) {
        mMode = newMode;
    }
}
