package nl.rug.ds.bpm.verification.model.multi;

import nl.rug.ds.bpm.specification.jaxb.SpecificationSet;
import nl.rug.ds.bpm.verification.model.generic.AbstractStructure;

import java.util.HashSet;
import java.util.Set;

public class MultiStructure extends AbstractStructure {
    private final Set<SubStructure> subStructures;

    /**
     * Creates a MultiStructure
     */
    public MultiStructure() {
        super();
        subStructures = new HashSet<>();
    }

    /**
     * Adds a SubStructure to this MultiStructure.
     *
     * @param specificationSet   the SpecificationSet applicable to the SubStructure.
     * @param atomicPropositions the Set of atomic proportions relevant to the SubStructure.
     */
    public void addSubStructure(SpecificationSet specificationSet, Set<String> atomicPropositions) {
        subStructures.add(new SubStructure(specificationSet, atomicPropositions));
    }

    public Set<SubStructure> getSubStructures() {
        return subStructures;
    }

    public void finalizeStructure() {
        states.clear();
        initial.clear();

        for (SubStructure subStructure : subStructures)
            subStructure.finalizeStructure();
    }
}
