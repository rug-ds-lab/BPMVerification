package nl.rug.ds.bpm.verification.optimizer.stutterOptimizer;

import nl.rug.ds.bpm.event.EventHandler;
import nl.rug.ds.bpm.verification.comparator.StateComparator;
import nl.rug.ds.bpm.verification.model.kripke.Kripke;
import nl.rug.ds.bpm.verification.model.kripke.State;

import java.util.*;

/**
 * Created by p256867 on 2-5-2017.
 */
public class QuickOptimizer {
    private EventHandler eventHandler;
    private Kripke kripke;

    public QuickOptimizer(EventHandler eventHandler, Kripke kripke) {
        this.eventHandler = eventHandler;
        this.kripke = kripke;

        stutterOptimize();
    }

    private void stutterOptimize() {
        int eventCount = 16000;
        int count = 0;

        Iterator<State> iterator = kripke.getStates().iterator();
        while (iterator.hasNext()) {
            State s = iterator.next();
            if(count >= eventCount) {
                eventHandler.logInfo("Optimizing state space (pass 1, at " + count + "/" + kripke.getStates().size() + " states");
                eventCount += 16000;
            }
            count++;

            Iterator<State> j = s.getNextStates().iterator();
            boolean allStutter = j.hasNext();
            while (j.hasNext() && allStutter)
                allStutter = s.APequals(j.next());

            if(allStutter) {
                for (State n : s.getNextStates()) {
                    n.getPreviousStates().remove(s);
                    n.getPreviousStates().addAll(s.getPreviousStates());
                }

                for (State p : s.getPreviousStates()) {
                    p.getNextStates().remove(s);
                    p.getNextStates().addAll(s.getNextStates());
                }
                iterator.remove();
            }
        }
    }
}