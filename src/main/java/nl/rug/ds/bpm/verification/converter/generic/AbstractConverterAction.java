package nl.rug.ds.bpm.verification.converter.generic;

import nl.rug.ds.bpm.petrinet.interfaces.element.TransitionI;
import nl.rug.ds.bpm.petrinet.interfaces.marking.MarkingI;
import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.verification.converter.ConverterAction;
import nl.rug.ds.bpm.verification.model.generic.AbstractState;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * An abstract ConverterAction.
 */
public abstract class AbstractConverterAction<S extends AbstractState<S>> extends RecursiveAction implements ConverterAction<S> {
    private ForkJoinPool forkJoinPool;

    protected VerifiableNet net;
    protected MarkingI marking;
    protected TransitionI fired;
    protected Set<? extends TransitionI> previousParallelEnabledTransitions;

    protected static long report = 0;

    /**
     * Creates an abstract ConverterAction.
     *
     * @param net     a VerifiableNet.
     * @param marking the initial Marking of the VerifiableNet.
     */
    public AbstractConverterAction(VerifiableNet net, MarkingI marking) {
        this.net = net;
        this.marking = marking;

        forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors() * 2, ForkJoinPool.defaultForkJoinWorkerThreadFactory, Thread.getDefaultUncaughtExceptionHandler(), true);
    }

    /**
     * Creates an abstract ConverterAction.
     *
     * @param net     a VerifiableNet.
     * @param marking the Marking of the VerifiableNet after firing the Transition fired.
     * @param fired   the Transition that fired to obtain marking.
     */
    public AbstractConverterAction(ForkJoinPool forkJoinPool, VerifiableNet net, MarkingI marking, TransitionI fired, Set<? extends TransitionI> previousParallelEnabledTransitions) {
        this(net, marking);
        this.forkJoinPool = forkJoinPool;
        this.fired = fired;
        this.previousParallelEnabledTransitions = new HashSet<>(previousParallelEnabledTransitions);
        this.previousParallelEnabledTransitions.remove(fired);
    }

    /**
     * Returns the pool this task is assigned to.
     *
     * @return the pool this task is assigned to
     */
    protected ForkJoinPool getForkJoinPool() {
        return forkJoinPool;
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

    public synchronized boolean report() {
        report++;

        if (report >= 10000)
            report = 0;

        return (report == 0);
    }
}
