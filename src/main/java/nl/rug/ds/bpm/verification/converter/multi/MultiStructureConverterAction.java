package nl.rug.ds.bpm.verification.converter.multi;

import nl.rug.ds.bpm.expression.CompositeExpression;
import nl.rug.ds.bpm.petrinet.interfaces.element.TransitionI;
import nl.rug.ds.bpm.petrinet.interfaces.marking.MarkingI;
import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.util.exception.ConverterException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.converter.generic.AbstractConverterAction;
import nl.rug.ds.bpm.verification.model.multi.MultiState;
import nl.rug.ds.bpm.verification.model.multi.MultiStructure;
import nl.rug.ds.bpm.verification.model.multi.factory.MultiFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;

/**
 * Class that converts a given VerifiableNet into a MultiStructure using RecursiveActions.
 */
public class MultiStructureConverterAction extends AbstractConverterAction<MultiState> {
    private final MultiFactory multiFactory;
    private final MultiStructure multiStructure;
    private MultiState previous;

    /**
     * Creates a MultiStructureConverterAction to compute the initial States and start the RecursiveAction computation.
     *
     * @param net            a VerifiableNet
     * @param marking        the initial Marking of the VerifiableNet.
     * @param factory        the StructureFactory used.
     * @param multiStructure the MultiStructure to populate.
     */
    public MultiStructureConverterAction(VerifiableNet net, MarkingI marking, MultiFactory factory, MultiStructure multiStructure) {
        super(net, marking);
        this.multiFactory = factory;
        this.multiStructure = multiStructure;
    }

    /**
     * Creates a MultiStructureConverterAction to compute subsequent States of the RecursiveAction computation.
     *
     * @param net            a VerifiableNet
     * @param marking        the subsequent Marking of the VerifiableNet obtained after firing the Transition fired.
     * @param fired          the Transition fired to obtain marking.
     * @param factory        the StructureFactory used.
     * @param multiStructure the MultiStructure to populate.
     * @param previous       the State obtained in the previous computation step.
     */
    public MultiStructureConverterAction(ForkJoinPool forkJoinPool, VerifiableNet net, MarkingI marking, TransitionI fired, Set<? extends TransitionI> previousParallelEnabledTransitions, MultiFactory factory, MultiStructure multiStructure, MultiState previous) {
        super(forkJoinPool, net, marking, fired, previousParallelEnabledTransitions);
        this.multiFactory = factory;
        this.multiStructure = multiStructure;
        this.previous = previous;
    }

    /**
     * Computes the initial States and starts the RecursiveAction computation.
     */
    @Override
    public void computeInitial() {
        Set<RecursiveAction> nextActions = new HashSet<>();

        for (Set<? extends TransitionI> enabled : net.getParallelEnabledTransitions(marking)) {
            Set<CompositeExpression> guardExpressions = multiFactory.getGuardExpressions(enabled);
            Set<CompositeExpression> expressions = multiFactory.getEnabledExpressions(enabled);
            expressions.addAll(multiFactory.getDataExpressions(marking));

            TreeSet<String> AP = multiFactory.addAtomicPropositions(expressions);
            expressions.addAll(guardExpressions);
            AP.addAll(multiFactory.addAtomicPropositions(multiFactory.inferExpressions(expressions)));

            MultiState created = multiFactory.createState(marking.toString(), AP);
            CompositeExpression stateExpression = multiFactory.composeExpressions(expressions);
            CompositeExpression guardExpression = multiFactory.composeExpressions(guardExpressions);

            try {
                MultiState found = multiStructure.addInitial(created, stateExpression, guardExpression);

                if (isNew(created, found) && !isSink(enabled))
                    nextActions.addAll(nextActions(found, enabled));
                else {
                    multiStructure.addNext(found, found, stateExpression, guardExpression); //makeSink(found);

                    Logger.log("Encountered empty initial marking, setting sink state.", LogEvent.WARNING);
                }
            } catch (ConverterException e) {
                Logger.log("Encountered issue adding initial marking.", LogEvent.ERROR);
            }
        }

        for (RecursiveAction action : nextActions)
            getForkJoinPool().execute(action);

        getForkJoinPool().shutdown();
        try {
            if (getForkJoinPool().awaitTermination(1, TimeUnit.DAYS))
                getForkJoinPool().shutdownNow();
        } catch (InterruptedException exception) {
            Logger.log("Model too big.", LogEvent.CRITICAL);
        }
    }

    @Override
    public void compute() {
        Set<RecursiveAction> nextActions = new HashSet<>();

        for (Set<? extends TransitionI> enabled : net.getParallelEnabledTransitions(marking)) {
            if (!enabled.containsAll(previousParallelEnabledTransitions)) continue;

            Set<CompositeExpression> expressions = multiFactory.getEnabledExpressions(enabled);
            Set<CompositeExpression> guardExpressions = multiFactory.getGuardExpressions(enabled);
            expressions.addAll(multiFactory.getDataExpressions(marking));

            TreeSet<String> AP = multiFactory.addAtomicPropositions(expressions);
            expressions.addAll(guardExpressions);
            AP.addAll(multiFactory.addAtomicPropositions(multiFactory.inferExpressions(expressions)));

            MultiState created = multiFactory.createState(marking.toString(), AP);
            CompositeExpression stateExpression = multiFactory.composeExpressions(expressions);
            CompositeExpression guardExpression = multiFactory.composeExpressions(guardExpressions);

            try {
                MultiState found = multiStructure.addNext(previous, created, stateExpression, guardExpression);

                if (isNew(created, found)) {
                    if (isSink(enabled)) {
                        multiStructure.addNext(found, found, stateExpression, guardExpression); //makeSink(found);
                    }

                    nextActions.addAll(nextActions(found, enabled));
                }
            } catch (ConverterException e) {
                Logger.log("Encountered issue adding initial marking.", LogEvent.ERROR);
            }
        }

        if (report())
            Logger.log("Pool of " + getForkJoinPool().getQueuedTaskCount() + " jobs with " + getForkJoinPool().getRunningThreadCount() + " active workers", LogEvent.INFO);

        for (RecursiveAction action : nextActions)
            getForkJoinPool().execute(action);
    }

    /**
     * Obtains the set of next ConverterActions from the State created by this ConverterAction and the obtained set of enabled Transitions.
     *
     * @param created the State created by this ConverterAction.
     * @param enabled the obtained set of enabled Transitions.
     * @return a set of ConverterActions.
     */
    @Override
    public Set<? extends RecursiveAction> nextActions(MultiState created, Set<? extends TransitionI> enabled) {
        Set<MultiStructureConverterAction> nextActions = new HashSet<>();
        for (TransitionI transition : enabled)
            for (MarkingI step : net.fireTransition(transition, marking))
                nextActions.add(new MultiStructureConverterAction(getForkJoinPool(), this.net, step, transition, enabled, this.multiFactory, this.multiStructure, created));

        return nextActions;
    }
}
