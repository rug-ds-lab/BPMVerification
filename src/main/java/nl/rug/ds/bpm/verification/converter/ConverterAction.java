package nl.rug.ds.bpm.verification.converter;

import nl.rug.ds.bpm.petrinet.interfaces.element.TransitionI;
import nl.rug.ds.bpm.verification.model.State;

import java.util.Set;
import java.util.concurrent.RecursiveAction;

public interface ConverterAction {

    /**
     * Computes the initial States and starts the RecursiveAction computation.
     */
    void computeInitial();

    /**
     * Overriden method from RecursiveAction that computes a step to calculate the structure.
     */
    void compute();

    /**
     * Returns true iff the created State was new to the Structure by comparing it with the returned State found.
     *
     * @param created the created State.
     * @param found   the State returned after calling addState(State created).
     * @return created == found.
     */
    boolean isNew(State created, State found);

    /**
     * Returns true iff the enabled set of Transitions produces a sink State.
     *
     * @param enabled the set of (parallel) enabled Transitions.
     * @return True iff enabled is empty.
     */
    boolean isSink(Set<? extends TransitionI> enabled);

    /**
     * Turns the given State into a sink.
     *
     * @param sink the State to turn into a sink.
     */
    void makeSink(State sink);

    /**
     * Obtains the set of next ConverterActions from the State created by the current ConverterAction and the obtained set of enabled Transitions.
     *
     * @param created the State created by the current ConverterAction.
     * @param enabled the obtained set of enabled Transitions.
     * @return a set of ConverterActions.
     */
    Set<? extends RecursiveAction> nextActions(State created, Set<? extends TransitionI> enabled);
}
