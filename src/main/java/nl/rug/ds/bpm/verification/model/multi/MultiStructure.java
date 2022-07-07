package nl.rug.ds.bpm.verification.model.multi;

import nl.rug.ds.bpm.expression.CompositeExpression;
import nl.rug.ds.bpm.specification.jaxb.SpecificationSet;
import nl.rug.ds.bpm.util.exception.ConverterException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.model.generic.AbstractStructure;

import java.util.HashSet;
import java.util.Set;

public class MultiStructure extends AbstractStructure<MultiState> {
    private final Set<Partition> partitions;

    /**
     * Creates a MultiStructure
     */
    public MultiStructure() {
        super();
        partitions = new HashSet<>();
    }

    /**
     * Adds a Partition to this MultiStructure.
     *
     * @param specificationSet   the SpecificationSet applicable to the Partition.
     * @param atomicPropositions the Set of atomic proportions relevant to the Partition.
     */
    public void addPartition(SpecificationSet specificationSet, Set<String> atomicPropositions) {
        partitions.add(new Partition(specificationSet, atomicPropositions));
        Logger.log("Adding partition for " + String.join(", ", atomicPropositions), LogEvent.VERBOSE);
    }

    /**
     * Returns the set of Partitions of this MultiStructure.
     *
     * @return the set of Partitions.
     */
    public Set<Partition> getPartitions() {
        return partitions;
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

        for (Partition partition : partitions) {
            if (partition.contradicts(stateExpression) || partition.contradicts(guardExpression))
                partition.addState(found);
            else
                partition.addInitial(state);
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

        for (Partition partition : partitions) {
            if (partition.contradicts(stateExpression) || partition.contradicts(guardExpression))
                partition.addState(found);
            else
                partition.addNext(previous, found);
        }

        return found;
    }
}
