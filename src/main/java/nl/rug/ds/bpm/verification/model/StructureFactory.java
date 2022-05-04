package nl.rug.ds.bpm.verification.model;

import nl.rug.ds.bpm.expression.CompositeExpression;
import nl.rug.ds.bpm.petrinet.interfaces.element.TransitionI;
import nl.rug.ds.bpm.petrinet.interfaces.marking.MarkingI;
import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.verification.converter.generic.AbstractConverterAction;
import nl.rug.ds.bpm.verification.map.AtomicPropositionMap;

import java.util.Set;
import java.util.TreeSet;

public interface StructureFactory {

    /**
     * Creates a new Structure.
     *
     * @return the new Structure.
     */
    Structure createStructure();

    /**
     * Creates a new State for a given set of atomic propositions.
     *
     * @param atomicPropositions the ordered set of atomic propositions that should hold in the created State.
     * @return the created State.
     */
    State createState(Set<String> atomicPropositions);

    /**
     * Creates a new State for a given Marking of a Net and a set of atomic propositions.
     *
     * @param marking            the Marking that should hold in the created State.
     * @param atomicPropositions the ordered set of atomic propositions that should hold in the created State.
     * @return the created State.
     */
    State createState(String marking, Set<String> atomicPropositions);

    /**
     * Obtains, or creates, the atomic proposition that represents the given expression.
     *
     * @param expression the expression.
     * @return the atomic proposition.
     */
    String addAtomicProposition(CompositeExpression expression);

    /**
     * Obtains, or creates, the atomic proposition that represents the given expression.
     *
     * @param expression the expression.
     * @return the atomic proposition.
     */
    String addAtomicProposition(String expression);

    /**
     * Obtains, or creates, the atomic propositions that represents the given expressions.
     *
     * @param expressions the set of expressions.
     * @return an ordered set of atomic propositions.
     */
    TreeSet<String> addAtomicPropositions(Set<CompositeExpression> expressions);

    /**
     * Obtain the map that converts between user expressions and internally used atomic propositions.
     *
     * @return the map.
     */
    AtomicPropositionMap<CompositeExpression> getAtomicPropositionMap();

    /**
     * Returns a set of expressions that hold based on the data in the given Marking.
     *
     * @param marking the marking of the Net.
     * @return a set of CompositeExpressions.
     */
    Set<CompositeExpression> getDataExpressions(MarkingI marking);

    /**
     * Returns a set of expressions that hold based on the set of enabled transitions.
     *
     * @param enabled the (parallel) enabled transitions.
     * @return a set of CompositeExpressions.
     */
    Set<CompositeExpression> getEnabledExpressions(Set<? extends TransitionI> enabled);

    /**
     * Returns a set of expressions that hold based on the set of enabled transitions.
     *
     * @param enabled the (parallel) enabled transitions.
     * @return a set of CompositeExpressions.
     */
    Set<CompositeExpression> getGuardExpressions(Set<? extends TransitionI> enabled);

    /**
     * Returns the set of Expressions that follow from the given Expression.
     *
     * @param expression the Expression of what holds.
     * @return an ordered set of Expressions that follow from stateExpression.
     */
    Set<CompositeExpression> inferExpressions(CompositeExpression expression);

    /**
     * Returns the set of Expressions that follow from the given set of Expressions.
     *
     * @param expressions the set of Expressions of what holds.
     * @return an ordered set of Expressions that follow from stateExpression.
     */
    Set<CompositeExpression> inferExpressions(Set<CompositeExpression> expressions);

    /**
     * Returns a CompositeExpression of the given set of expressions.
     *
     * @param expressions a set of CompositeExpressions.
     * @return a CompositeExpression.
     */
    CompositeExpression composeExpressions(Set<CompositeExpression> expressions);

    /**
     * Creates a new Structure ConverterAction.
     *
     * @param net       a VerifiableNet.
     * @param marking   the initial Marking.
     * @param structure the Structure to populate.
     * @return a new ConverterAction.
     */
    AbstractConverterAction createConverter(VerifiableNet net, MarkingI marking, Structure structure);
}
