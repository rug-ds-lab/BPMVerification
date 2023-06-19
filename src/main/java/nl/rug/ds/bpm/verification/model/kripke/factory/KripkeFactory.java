package nl.rug.ds.bpm.verification.model.kripke.factory;

import nl.rug.ds.bpm.expression.CompositeExpression;
import nl.rug.ds.bpm.petrinet.interfaces.element.TransitionI;
import nl.rug.ds.bpm.petrinet.interfaces.marking.MarkingI;
import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.verification.converter.kripke.KripkeStructureConverterAction;
import nl.rug.ds.bpm.verification.model.StructureFactory;
import nl.rug.ds.bpm.verification.model.generic.factory.AbstractStructureFactory;
import nl.rug.ds.bpm.verification.model.kripke.KripkeState;
import nl.rug.ds.bpm.verification.model.kripke.KripkeStructure;

import java.util.Set;
import java.util.TreeSet;

public class KripkeFactory extends AbstractStructureFactory<KripkeState, KripkeStructure> implements StructureFactory<KripkeState, KripkeStructure> {

    /**
     * Creates a new Kripke structure.
     *
     * @return the new Kripke structure.
     */
    @Override
    public KripkeStructure createStructure() {
        return new KripkeStructure();
    }

    /**
     * Creates a new Kripke structure State for a given set of atomic propositions.
     *
     * @param atomicPropositions the ordered set of atomic propositions that should hold in the created State.
     * @return the created State.
     */
    @Override
    public KripkeState createState(Set<String> atomicPropositions) {
        return new KripkeState(atomicPropositions);
    }

    /**
     * Creates a new Kripke structure State for a given Marking of a Net and a set of atomic propositions.
     *
     * @param marking            the Marking that should hold in the created State.
     * @param atomicPropositions the ordered set of atomic propositions that should hold in the created State.
     * @return the created State.
     */
    @Override
    public KripkeState createState(String marking, Set<String> atomicPropositions) {
        return new KripkeState(marking, atomicPropositions);
    }

    /**
     * Creates a new Kripke structure State for a given Marking of a Net and a set of (parallel) enabled Transitions.
     *
     * @param marking     the Marking that should hold in the created State.
     * @param transitions the set of (parallel) enabled Transitions.
     * @return the created State.
     */
    public KripkeState createState(MarkingI marking, Set<? extends TransitionI> transitions) {
        Set<CompositeExpression> expressions = getEnabledExpressions(transitions);
        expressions.addAll(getDataExpressions(marking));

        TreeSet<String> AP = addAtomicPropositions(expressions);
        AP.addAll(addAtomicPropositions(inferExpressions(expressions)));

        return createState(marking.toString(), AP);
    }

    /**
     * Creates a new KripkeStructureConverterAction.
     *
     * @param net       a VerifiableNet.
     * @param marking   the initial Marking.
     * @param structure the Kripke Structure to populate.
     * @return a new KripkeStructureConverterAction.
     */
    @Override
    public KripkeStructureConverterAction createConverter(VerifiableNet net, MarkingI marking, KripkeStructure structure) {
        return new KripkeStructureConverterAction(net, marking, this, structure);
    }
}
