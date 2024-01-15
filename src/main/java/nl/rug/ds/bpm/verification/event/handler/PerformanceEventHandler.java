package nl.rug.ds.bpm.verification.event.handler;

import nl.rug.ds.bpm.verification.event.PerformanceEvent;
import nl.rug.ds.bpm.verification.event.listener.PerformanceEventListener;

public class PerformanceEventHandler extends AbstractEventHandler<PerformanceEventListener, PerformanceEvent> {

    public PerformanceEventHandler() {
        super();
    }

    @Override
    protected void notify(PerformanceEvent event) {
        for (PerformanceEventListener listener : listenerSet)
            listener.performanceEvent(event);
    }
}
