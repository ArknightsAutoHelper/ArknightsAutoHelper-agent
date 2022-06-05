package xyz.cirno.aah.agent;

import android.annotation.SuppressLint;
import android.view.InputDevice;
import android.view.MotionEvent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("PrivateApi, BlockedPrivateApi")
public class TouchStateTracker {

    private static final Method MotionEvent_setDisplayId;

    static {
        Method meth;
        try {
            meth = MotionEvent.class.getDeclaredMethod("setDisplayId", int.class);
        } catch (NoSuchMethodException e) {
            meth = null;
        }
        MotionEvent_setDisplayId = meth;
    }

    private static final int DEFAULT_DEVICE_ID = 0;
    private static final float DEFAULT_PRESSURE = 1.0f;
    private static final float NO_PRESSURE = 0.0f;
    private static final float DEFAULT_SIZE = 0.0f;
    private static final int DEFAULT_META_STATE = 0;
    private static final float DEFAULT_PRECISION_X = 1.0f;
    private static final float DEFAULT_PRECISION_Y = 1.0f;
    private static final int DEFAULT_EDGE_FLAGS = 0;
    private static final int DEFAULT_BUTTON_STATE = 0;
    private static final int DEFAULT_FLAGS = 0;


    private final int SLOT_COUNT = 10;
    private final int displayId;

    private boolean anyDown;
    private long downTime;

    private final MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[SLOT_COUNT];
    private final MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[SLOT_COUNT];

    private final Map<Integer, Integer> pointerIdToSlot = new HashMap<>(SLOT_COUNT);

    private final List<MotionEvent> pendingEvents = new ArrayList<>();

    public TouchStateTracker(int displayId) {
        if (MotionEvent_setDisplayId == null && displayId != 0) {
            throw new IllegalArgumentException("displayId != 0 but MotionEvent.setDisplayId() is not available");
        }
        this.displayId = displayId;
    }


    public void updateState(long eventTime, int action, int pointerId, float x, float y, float pressure, boolean mergeMultitouchMove) {
        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_MOVE) {
            throw new IllegalArgumentException("action must be one of ACTION_DOWN, ACTION_UP and ACTION_MOVE");
        }
        if (action == MotionEvent.ACTION_DOWN && pointerIdToSlot.containsKey(pointerId)) {
            throw new IllegalArgumentException("ACTION_DOWN with existing pointerId");
        }
        int slot;
        if (action == MotionEvent.ACTION_DOWN) {
            slot = allocateSlot(pointerId);
        } else {
            Integer slotInteger = pointerIdToSlot.get(pointerId);
            if (slotInteger == null) {
                throw new IllegalArgumentException("ACTION_UP/ACTION_MOVE with unknown pointerId");
            }
            slot = slotInteger;
        }
        int slotCount = pointerIdToSlot.size();

        int realAction = action;

        if (mergeMultitouchMove && action != MotionEvent.ACTION_MOVE) {
            throw new IllegalArgumentException("mergeMultitouchMove is only available for ACTION_MOVE");
        }

        if (slotCount != 1) {
            // secondary pointers must use ACTION_POINTER_* ORed with the pointerIndex
            if (action == MotionEvent.ACTION_UP) {
                realAction = MotionEvent.ACTION_POINTER_UP | (slot << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            } else if (action == MotionEvent.ACTION_DOWN) {
                realAction = MotionEvent.ACTION_POINTER_DOWN | (slot  << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            }
        }

        pointerProperties[slot] = new MotionEvent.PointerProperties();
        pointerProperties[slot].id = slot;
        pointerProperties[slot].toolType = MotionEvent.TOOL_TYPE_FINGER;
        pointerCoords[slot] = new MotionEvent.PointerCoords();
        pointerCoords[slot].x = x;
        pointerCoords[slot].y = y;
        pointerCoords[slot].pressure = pressure;
        pointerCoords[slot].size = DEFAULT_SIZE;

        long now = eventTime;

        if (action == MotionEvent.ACTION_DOWN && !anyDown) {
            anyDown = true;
            downTime = now;
        }

        if (!mergeMultitouchMove) {

            MotionEvent.PointerProperties[] propsToSend = new MotionEvent.PointerProperties[slotCount];
            MotionEvent.PointerCoords[] coordsToSend = new MotionEvent.PointerCoords[slotCount];
            int i = 0;
            for (int slotId : pointerIdToSlot.values()) {
                propsToSend[i] = pointerProperties[slotId];
                coordsToSend[i] = pointerCoords[slotId];
                i++;
            }

            MotionEvent event = MotionEvent.obtain(downTime, now, realAction, slotCount,
                    propsToSend, coordsToSend, DEFAULT_META_STATE, DEFAULT_BUTTON_STATE,
                    DEFAULT_PRECISION_X, DEFAULT_PRECISION_Y, DEFAULT_DEVICE_ID,
                    DEFAULT_EDGE_FLAGS, InputDevice.SOURCE_TOUCHSCREEN, DEFAULT_FLAGS);
            if (MotionEvent_setDisplayId != null) {
                try {
                    MotionEvent_setDisplayId.invoke(event, displayId);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }

            if (action == MotionEvent.ACTION_UP) {
                pointerIdToSlot.remove(pointerId);
                anyDown = !pointerIdToSlot.isEmpty();
            }

            synchronized (pendingEvents) {
                pendingEvents.add(event);
            }
        }

    }

    public MotionEvent[] fetchPendingEvents() {
        synchronized (pendingEvents) {
            MotionEvent[] events = pendingEvents.toArray(new MotionEvent[pendingEvents.size()]);
            pendingEvents.clear();
            return events;
        }
    }

    private int allocateSlot(int pointerId) {
        for (int i = 0; i < SLOT_COUNT; ++i) {
            if (!pointerIdToSlot.containsValue(i)) {
                pointerIdToSlot.put(pointerId, i);
                return i;
            }
        }
        throw new IllegalStateException("No free slots");
    }
}
