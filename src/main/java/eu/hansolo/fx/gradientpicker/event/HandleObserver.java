package eu.hansolo.fx.gradientpicker.event;

import eu.hansolo.fx.gradientpicker.event.HandleEvent;

import java.util.EventListener;


@FunctionalInterface
public interface HandleObserver extends EventListener {
    void onHandleEvent(final HandleEvent EVENT);
}
