package nl.rug.ds.bpm.verification.event.handler;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractEventHandler<L, E> {
    protected final Set<L> listenerSet;

    public AbstractEventHandler() {
        listenerSet = new HashSet<>();
    }

    public void addEventListener(L listener) {
        listenerSet.add(listener);
    }

    public void removeEventListener(L listener) {
        listenerSet.remove(listener);
    }

    public void fireEvent(E event) {
        notify(event);
    }

    protected abstract void notify(E event);
}
