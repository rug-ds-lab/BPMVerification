package nl.rug.ds.bpm.verification.model.multi.factory;

import nl.rug.ds.bpm.petrinet.interfaces.marking.MarkingI;
import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.verification.converter.generic.AbstractConverterAction;
import nl.rug.ds.bpm.verification.converter.multi.MultiStructureConverterAction;
import nl.rug.ds.bpm.verification.model.State;
import nl.rug.ds.bpm.verification.model.Structure;
import nl.rug.ds.bpm.verification.model.StructureFactory;
import nl.rug.ds.bpm.verification.model.generic.factory.AbstractStructureFactory;
import nl.rug.ds.bpm.verification.model.multi.MultiState;
import nl.rug.ds.bpm.verification.model.multi.MultiStructure;

import java.util.Set;

public class MultiFactory extends AbstractStructureFactory implements StructureFactory {

    /**
     * Creates a new MultiStructure.
     *
     * @return the new MultiStructure.
     */
    @Override
    public Structure createStructure() {
        return new MultiStructure();
    }

    /**
     * Creates a new MultiStructure State for a given set of atomic propositions.
     *
     * @param atomicPropositions the ordered set of atomic propositions that should hold in the created State.
     * @return the created State.
     */
    @Override
    public State createState(Set<String> atomicPropositions) {
        return new MultiState(atomicPropositions);
    }

    /**
     * Creates a new MultiStructure State for a given Marking of a Net and a set of atomic propositions.
     *
     * @param marking            the Marking that should hold in the created State.
     * @param atomicPropositions the ordered set of atomic propositions that should hold in the created State.
     * @return the created State.
     */
    @Override
    public State createState(String marking, Set<String> atomicPropositions) {
        return new MultiState(marking, atomicPropositions);
    }

    @Override
    public AbstractConverterAction createConverter(VerifiableNet net, MarkingI marking, Structure structure) {
        return new MultiStructureConverterAction(net, marking, this, (MultiStructure) structure);
    }
}
