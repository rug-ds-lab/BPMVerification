package nl.rug.ds.bpm.verification.model.multi;

import nl.rug.ds.bpm.expression.CompositeExpression;
import nl.rug.ds.bpm.specification.jaxb.SpecificationSet;
import nl.rug.ds.bpm.util.exception.ConverterException;
import nl.rug.ds.bpm.verification.model.generic.AbstractStructure;

import java.util.HashSet;
import java.util.Set;

public class MultiStructure extends AbstractStructure<MultiState> {
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

    /**
     * Returns the set of SubStructures of this MultiStructure.
     *
     * @return the set of SubStructures.
     */
    public Set<SubStructure> getSubStructures() {
        return subStructures;
    }

    /**
     * @param state
     * @param stateExpression
     * @param guardExpression
     * @return
     * @throws ConverterException
     */
    public synchronized MultiState addInitial(MultiState state, CompositeExpression stateExpression, CompositeExpression guardExpression) throws ConverterException {
        MultiState found = this.addInitial(state);

        for (SubStructure subStructure : subStructures) {
            if (subStructure.contradicts(stateExpression) || subStructure.contradicts(guardExpression))
                subStructure.addState(found);
            else
                subStructure.addInitial(state);
        }

        return super.addInitial(state);
    }


    /**
     * @param previous
     * @param created
     * @param stateExpression
     * @param guardExpression
     * @return
     * @throws ConverterException
     */
    public synchronized MultiState addNext(MultiState previous, MultiState created, CompositeExpression stateExpression, CompositeExpression guardExpression) throws ConverterException {
        MultiState found = this.addNext(previous, created);

        for (SubStructure subStructure : subStructures) {
            if (subStructure.contradicts(stateExpression) || subStructure.contradicts(guardExpression))
                subStructure.addState(found);
            else
                subStructure.addNext(previous, found);
        }

        return found;
    }
}
