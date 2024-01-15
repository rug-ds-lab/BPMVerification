package nl.rug.ds.bpm.verification.event.listener;

import nl.rug.ds.bpm.verification.event.PerformanceEvent;

public interface PerformanceEventListener {
    void performanceEvent(PerformanceEvent event);
}
