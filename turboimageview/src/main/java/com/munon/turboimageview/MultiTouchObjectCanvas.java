package com.munon.turboimageview;

public interface MultiTouchObjectCanvas<T> {

    /**
     * See if there is a draggable object at the current point. Returns the
     * object at the point, or null if nothing to drag. To start a multitouch
     * drag/stretch operation, this routine must return some non-null reference
     * to an object. This object is passed into the other methods in this interface
     * when they are called.
     *
     * @param touchPoint The point being tested (in object coordinates). Return the
     * topmost object under this point, or if dragging/stretching
     * the whole canvas, just return a reference to the canvas.
     * @return a reference to the object under the point being tested, or
     * null to cancel the drag operation. If dragging/stretching the whole
     * canvas (e.g. in a photo viewer), always return non-null, otherwise
     * the stretch operation won't work.
     */
    public T getDraggableObjectAtPoint(PointInfo touchPoint);

    /**
     * TODO: document
     */
    public boolean pointInObjectGrabArea(PointInfo touchPoint, T obj);

    /**
     * Get the screen coords of the dragged object's origin, and scale
     * multiplier to convert screen coords to obj coords. The job of this routine
     * is to call the .set() method on the passed PositionAndScale object to
     * record the initial position and scale of the object (in object coordinates)
     * before any dragging/stretching takes place.
     *
     * @param obj The object being dragged/stretched.
     * @param objPosAndScaleOut Output parameter: You need to call objPosAndScaleOut.set()
     * to record the current position and scale of obj.
     */
    public void getPositionAndScale(T obj, PositionAndScale objPosAndScaleOut);

    /**
     * Callback to update the position and scale (in object coords) of the
     * currently-dragged object.
     *
     * @param obj The object being dragged/stretched.
     * @param newObjPosAndScale The new position and scale of the object, in object
     * coordinates. Use this to move/resize the object before returning.
     * @param touchPoint Info about the current touch point, including multitouch
     * information and utilities to calculate and cache multitouch pinch
     * diameter etc. (Note: touchPoint is volatile, if you want to
     * keep any fields of touchPoint, you must copy them before the method
     * body exits.)
     * @return true
     * if setting the position and scale of the object was successful,
     * or false if the position or scale parameters are out of range
     * for this object.
     */
    public boolean setPositionAndScale(T obj, PositionAndScale newObjPosAndScale,
                                       PointInfo touchPoint);

    /**
     * Select an object at the given point. Can be used to bring the object to
     * top etc. Only called when first touchpoint goes down, not when multitouch
     * is initiated. Also called with null on touch-up.
     *
     * @param obj The object being selected by single-touch, or null on touch-up.
     * @param touchPoint The current touch point.
     */
    public void selectObject(T obj, PointInfo touchPoint);

    public void deselectAll();

    public void canvasTouched();

}
