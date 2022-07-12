package nl.rug.ds.bpm.verification.model.multi;

import nl.rug.ds.bpm.expression.CompositeExpression;
import nl.rug.ds.bpm.specification.jaxb.SpecificationSet;
import nl.rug.ds.bpm.util.exception.ConverterException;
import nl.rug.ds.bpm.util.log.LogEvent;
import nl.rug.ds.bpm.util.log.Logger;
import nl.rug.ds.bpm.verification.model.generic.AbstractStructure;

import java.util.HashSet;
import java.util.Set;

/**
 * Class representing a multistructure that tracks multiple partitions simultaneously.
 */
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
     * Add a state to the transition system, and adds it to the set of initially accessible states.
     * Adds the state to each partition of this multistructure, but only adds it to the set of initially accessible
     * states iff the conditions of the partition does not contradict the given state- and guard-expression.
     *
     * @param state           the given state.
     * @param stateExpression the expression that holds the truths for the given state.
     * @param guardExpression the expression that holds the guards for the given state.
     * @return state if new, otherwise the equaling known state.
     * @throws ConverterException if the maximum number of states was reached.
     */
    public MultiState addInitial(MultiState state, CompositeExpression stateExpression, CompositeExpression guardExpression) throws ConverterException {
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
     * Add a relation to the transition system from the given current state to the given next state, add the given next
     * state to the transition system if it is not known. Adds the next state to each partition of this multistructure,
     * but only adds the relation iff the conditions of the partition does not contradict the given state- and
     * guard-expression.
     *
     * @param current         a state current to this transition system.
     * @param next            the state that must become accessible from the given current state.
     * @param stateExpression the expression that holds the truths for the given state.
     * @param guardExpression the expression that holds the guards for the given state.
     * @return created if new, otherwise the equaling known state.
     * @throws ConverterException if the maximum number of states was reached.
     */
    public synchronized MultiState addNext(MultiState current, MultiState next, CompositeExpression stateExpression, CompositeExpression guardExpression) throws ConverterException {
        MultiState found = this.addNext(current, next);

        for (Partition partition : partitions) {
            if (partition.contradicts(stateExpression) || partition.contradicts(guardExpression))
                partition.addState(found);
            else
                partition.addNext(current, found);
        }

        return found;
    }

    /**
     * Clears the multistructure from any references to multistates, leaving only the partitions and its blocks.
     */
    public void clear() {
        states.clear();

        for (Partition partition : partitions)
            partition.clear();
    }
}
