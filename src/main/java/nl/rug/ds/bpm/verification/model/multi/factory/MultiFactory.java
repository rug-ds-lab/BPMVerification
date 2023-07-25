package nl.rug.ds.bpm.verification.model.multi.factory;

import nl.rug.ds.bpm.petrinet.interfaces.marking.MarkingI;
import nl.rug.ds.bpm.petrinet.interfaces.net.VerifiableNet;
import nl.rug.ds.bpm.verification.converter.multi.MultiStructureConverterAction;
import nl.rug.ds.bpm.verification.model.StructureFactory;
import nl.rug.ds.bpm.verification.model.generic.factory.AbstractStructureFactory;
import nl.rug.ds.bpm.verification.model.multi.MultiState;
import nl.rug.ds.bpm.verification.model.multi.MultiStructure;

import java.util.Set;

public class MultiFactory extends AbstractStructureFactory<MultiState, MultiStructure> implements StructureFactory<MultiState, MultiStructure> {

    /**
     * Creates a new MultiStructure.
     *
     * @return the new MultiStructure.
     */
    @Override
    public MultiStructure createStructure() {
        return new MultiStructure();
    }

    /**
     * Creates a new MultiStructure State for a given set of atomic propositions.
     *
     * @param atomicPropositions the ordered set of atomic propositions that should hold in the created State.
     * @return the created State.
     */
    @Override
    public MultiState createState(Set<String> atomicPropositions) {
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
    public MultiState createState(String marking, Set<String> atomicPropositions) {
        return new MultiState(marking, atomicPropositions);
    }

    @Override
    public MultiStructureConverterAction createConverter(VerifiableNet net, MarkingI marking, MultiStructure structure) {
        MultiStructureConverterAction.newForkJoinPool();
        return new MultiStructureConverterAction(net, marking, this, structure);
    }
}
