package nl.rug.ds.bpm.verification.model.generic.factory;

import nl.rug.ds.bpm.expression.CompositeExpression;
import nl.rug.ds.bpm.expression.ExpressionBuilder;
import nl.rug.ds.bpm.expression.LogicalType;
import nl.rug.ds.bpm.petrinet.interfaces.element.TransitionI;
import nl.rug.ds.bpm.petrinet.interfaces.marking.DataMarkingI;
import nl.rug.ds.bpm.petrinet.interfaces.marking.MarkingI;
import nl.rug.ds.bpm.util.comparator.ComparableComparator;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.map.AtomicPropositionMap;
import nl.rug.ds.bpm.verification.model.StructureFactory;

import java.util.Set;
import java.util.TreeSet;

/**
 * Abstract factory to create Structure, States, and ConverterActions.
 */
public abstract class AbstractStructureFactory implements StructureFactory {
    public final static CompositeExpression tau = ExpressionBuilder.parseExpression("tau");

    protected AtomicPropositionMap<CompositeExpression> apMap;

    /**
     * Creates an abstract Structure factory.
     */
    public AbstractStructureFactory() {
        apMap = new AtomicPropositionMap<>();
        apMap.addID(tau);
    }

    /**
     * Obtains, or creates, the atomic proposition that represents the given expression.
     *
     * @param expression the expression.
     * @return the atomic proposition.
     */
    @Override
    public synchronized String addAtomicProposition(CompositeExpression expression) {
        return apMap.addID(expression);
    }

    /**
     * Obtains, or creates, the atomic proposition that represents the given expression.
     *
     * @param expression the expression.
     * @return the atomic proposition.
     */
    @Override
    public synchronized String addAtomicProposition(String expression) {
        return addAtomicProposition(ExpressionBuilder.parseExpression(expression));
    }

    /**
     * Obtains, or creates, the atomic propositions that represents the given expressions.
     *
     * @param expressions the set of expressions.
     * @return an ordered set of atomic propositions.
     */
    @Override
    public synchronized TreeSet<String> addAtomicPropositions(Set<CompositeExpression> expressions) {
        TreeSet<String> ap = new TreeSet<String>(new ComparableComparator<String>());

        for (CompositeExpression expression : expressions)
            ap.add(addAtomicProposition(expression));

        return ap;
    }

    /**
     * Returns a set of expressions that hold based on the data in the given Marking.
     *
     * @param marking the marking of the Net.
     * @return a set of CompositeExpressions.
     */
    @Override
    public Set<CompositeExpression> getDataExpressions(MarkingI marking) {
        TreeSet<CompositeExpression> expressions = new TreeSet<>(new ComparableComparator<CompositeExpression>());

        if (marking instanceof DataMarkingI)
            for (String b : ((DataMarkingI) marking).getBindings().keySet())
                expressions.add(ExpressionBuilder.parseExpression((b + " == " + ((DataMarkingI) marking).getBindings().get(b))));

        return expressions;
    }


    /**
     * Returns a set of expressions that hold based on the set of enabled transitions.
     *
     * @param enabled the (parallel) enabled transitions.
     * @return a set of CompositeExpressions.
     */
    @Override
    public Set<CompositeExpression> getEnabledExpressions(Set<? extends TransitionI> enabled) {
        TreeSet<CompositeExpression> expressions = new TreeSet<>(new ComparableComparator<CompositeExpression>());

        for (TransitionI transition : enabled) {
            expressions.add(ExpressionBuilder.parseExpression(transition.getId()));

            if (!transition.getName().isEmpty())
                expressions.add(ExpressionBuilder.parseExpression(transition.getName()));

            if (transition.isTau())
                expressions.add(AbstractStructureFactory.tau);
        }
        return expressions;
    }


    /**
     * Returns a set of expressions that hold based on the set of enabled transitions.
     *
     * @param enabled the (parallel) enabled transitions.
     * @return a set of CompositeExpressions.
     */
    @Override
    public Set<CompositeExpression> getGuardExpressions(Set<? extends TransitionI> enabled) {
        TreeSet<CompositeExpression> expressions = new TreeSet<>(new ComparableComparator<CompositeExpression>());

        for (TransitionI transition : enabled) {
            CompositeExpression guard = transition.getGuard();
            if (guard != null)
                expressions.add(guard);
        }

        return expressions;
    }

    /**
     * Returns the set of Expressions that follow from the given Expression.
     *
     * @param expression the Expression of what holds.
     * @return an ordered set of Expressions that follow from stateExpression.
     */
    @Override
    public synchronized Set<CompositeExpression> inferExpressions(CompositeExpression expression) {
        TreeSet<CompositeExpression> expressions = new TreeSet<>(new ComparableComparator<CompositeExpression>());

        for (CompositeExpression e : apMap.getIDKeys()) {
            //TODO: @Nick: The function Expression.isFulfilledBy(OtherExpression) fails to return expected results.
            boolean implied = e.isFulfilledBy(expression);
            if (implied)
                expressions.add(expression);
            Logger.log("Evaluated expression " + expression + " => " + e + " as " + implied, 0);
        }

        return expressions;
    }

    /**
     * Returns the set of Expressions that follow from the given set of Expressions.
     *
     * @param expressions the set of Expressions of what holds.
     * @return an ordered set of Expressions that follow from stateExpression.
     */
    @Override
    public Set<CompositeExpression> inferExpressions(Set<CompositeExpression> expressions) {
        return inferExpressions(composeExpressions(expressions));
    }

    /**
     * Returns a CompositeExpression of the given set of expressions.
     *
     * @param expressions a set of CompositeExpressions.
     * @return a CompositeExpression.
     */
    @Override
    public CompositeExpression composeExpressions(Set<CompositeExpression> expressions) {
        CompositeExpression stateExpression;

        if (expressions.isEmpty())
            stateExpression = ExpressionBuilder.parseExpression("false");
        else if (expressions.size() == 1)
            stateExpression = expressions.iterator().next();
        else {
            stateExpression = new CompositeExpression(LogicalType.AND);
            for (CompositeExpression expression : expressions)
                stateExpression.addArgument(expression);
        }

        return stateExpression;
    }

    /**
     * Obtain the map that converts between user expressions and internally used atomic propositions.
     *
     * @return the map.
     */
    @Override
    public AtomicPropositionMap<CompositeExpression> getAtomicPropositionMap() {
        return apMap;
    }
}
