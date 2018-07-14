package eu.hansolo.fx.gradientpicker.event;

import eu.hansolo.fx.gradientpicker.Handle;
import java.util.EventObject;


public class HandleEvent extends EventObject {
    private final HandleEventType TYPE;


    // ******************** Constructors **************************************
    public HandleEvent(final Object SRC, final HandleEventType TYPE) {
        super(SRC);
        this.TYPE   = TYPE;
    }


    // ******************** Methods *******************************************
    public HandleEventType getType() { return TYPE; }

    public Handle getHandle() { return (Handle) getSource(); }
}
