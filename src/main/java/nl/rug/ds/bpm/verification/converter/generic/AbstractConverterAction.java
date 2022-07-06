package nl.rug.ds.bpm.verification.converter.generic;

import nl.rug.ds.bpm.petrinet.interfaces.element.TransitionI;
import nl.rug.ds.bpm.petrinet.interfaces.marking.MarkingI;
import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.verification.converter.ConverterAction;
import nl.rug.ds.bpm.verification.model.generic.AbstractState;

import java.util.Set;
import java.util.concurrent.RecursiveAction;

/**
 * An abstract ConverterAction.
 */
public abstract class AbstractConverterAction<S extends AbstractState<S>> extends RecursiveAction implements ConverterAction<S> {
    protected VerifiableNet net;
    protected MarkingI marking;
    protected TransitionI fired;

    /**
     * Creates an abstract ConverterAction.
     *
     * @param net     a VerifiableNet.
     * @param marking the initial Marking of the VeriableNet.
     */
    public AbstractConverterAction(VerifiableNet net, MarkingI marking) {
        this.net = net;
        this.marking = marking;
    }

    /**
     * Creates an abstract ConverterAction.
     *
     * @param net     a VerifiableNet.
     * @param marking the Marking of the VeriableNet after firing the Transition fired.
     * @param fired   the Transition that fired to obtain marking.
     */
    public AbstractConverterAction(VerifiableNet net, MarkingI marking, TransitionI fired) {
        this(net, marking);
        this.fired = fired;
    }

    /**
     * Returns true iff the created State was new to the Structure by comparing it with the returned State found.
     *
     * @param created the created State.
     * @param found   the State returned after calling addState(State created).
     * @return created == found.
     */
    @Override
    public boolean isNew(S created, S found) {
        return created == found;
    }

    /**
     * Returns true iff the enabled set of Transitions produces a sink State.
     *
     * @param enabled the set of (parallel) enabled Transitions.
     * @return True iff enabled is empty.
     */
    public boolean isSink(Set<? extends TransitionI> enabled) {
        return enabled.isEmpty();
    }

    /**
     * Turns the given State into a sink.
     *
     * @param sink the State to turn into a sink.
     */
    public void makeSink(S sink) {
        sink.addNext(sink);
        sink.addPrevious(sink);
    }
}