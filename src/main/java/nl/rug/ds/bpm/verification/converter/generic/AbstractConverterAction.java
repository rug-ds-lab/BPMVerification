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
    private static ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors() * 4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, Thread.getDefaultUncaughtExceptionHandler(), true);

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
    }

    /**
     * Creates an abstract ConverterAction.
     *
     * @param net     a VerifiableNet.
     * @param marking the Marking of the VerifiableNet after firing the Transition fired.
     * @param fired   the Transition that fired to obtain marking.
     */
    public AbstractConverterAction(VerifiableNet net, MarkingI marking, TransitionI fired, Set<? extends TransitionI> previousParallelEnabledTransitions) {
        this(net, marking);
        this.fired = fired;
        this.previousParallelEnabledTransitions = new HashSet<>(previousParallelEnabledTransitions);
        this.previousParallelEnabledTransitions.remove(fired);
    }

    /**
     * Returns the pool this task is assigned to.
     *
     * @return the pool this task is assigned to
     */
    public static ForkJoinPool getForkJoinPool() {
        return forkJoinPool;
    }


    /**
     * Creates a new pool that this task is assigned to.
     */
    public static void newForkJoinPool() {
        forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors() * 4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, Thread.getDefaultUncaughtExceptionHandler(), true);
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

        if (report >= 100000)
            report = 0;

        return (report == 0);
    }
}
