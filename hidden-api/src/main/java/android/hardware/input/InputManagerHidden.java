package android.hardware.input;


import android.view.InputEvent;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(InputManager.class)
public class InputManagerHidden {
    public static final int INJECT_INPUT_EVENT_MODE_ASYNC = 0;
    public static final int INJECT_INPUT_EVENT_MODE_WAIT_FOR_RESULT = 1;
    public static final int INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH = 2;
    public static InputManagerHidden getInstance() {
        throw new UnsupportedOperationException();
    }

    public boolean injectInputEvent(InputEvent event, int mode) {
        throw new UnsupportedOperationException();
    }
}
