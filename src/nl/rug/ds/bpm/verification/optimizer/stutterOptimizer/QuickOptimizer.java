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
        int eventCount = 2000;
        int count = 0;
        HashSet<State> removed = new HashSet<>();

        for (State current: kripke.getStates()) {
            Iterator<State> nextStates = current.getNextStates().iterator();
            boolean allStutter = nextStates.hasNext();
            while (nextStates.hasNext() && allStutter)
                allStutter = current.APequals(nextStates.next());

            if (allStutter) {
                Set<State> previous = new HashSet<State>(current.getPreviousStates());
                Set<State> next = new HashSet<State>(current.getNextStates());

                for (State n : current.getNextStates()) {
                    previous.addAll(n.getPreviousStates());
                    next.addAll(n.getNextStates());
                    n.getPreviousStates().clear();
                    n.getNextStates().clear();
                    //System.out.println("Stutter "+s.toString()+" to "+n.toString());
                }
                removed.addAll(current.getNextStates());

                for (State prev : previous) {
                    prev.getNextStates().removeAll(current.getNextStates());
                    prev.addNext(current);
                }

                for (State x : next) {
                    x.getPreviousStates().removeAll(current.getNextStates());
                    x.addPrevious(current);
                }

                previous.removeAll(current.getNextStates());
                previous.remove(current);

                next.removeAll(current.getNextStates());
                next.remove(current);

                current.getPreviousStates().clear();
                current.getNextStates().clear();

                current.setNextStates(next);
                current.setPreviousStates(previous);

                count++;
            }

            if (count >= eventCount) {
                eventHandler.logInfo("Optimizing state space (pass 1, at " + count + " states)");
                eventCount += 2000;
            }
        }

        kripke.getStates().removeAll(removed);
    }
}