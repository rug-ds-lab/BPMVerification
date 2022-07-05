package nl.rug.ds.bpm.verification.model.multi;


import nl.rug.ds.bpm.util.comparator.ComparableComparator;
import nl.rug.ds.bpm.verification.model.generic.MarkedState;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that implements a Kripke state with multiple parent stutter states.
 */
public class MultiState extends MarkedState<MultiState> {
    private final Map<SubStructure, Block> parents;
    private final Map<SubStructure, Set<MultiState>> nextSubStates;
    private final Map<SubStructure, Boolean> flags;

    /**
     * Creates a multi state.
     *
     * @param atomicPropositions The set of atomic propositions that hold in this state.
     */
    public MultiState(Set<String> atomicPropositions) {
        super(atomicPropositions);
        parents = new HashMap<>();
        nextSubStates = new HashMap<>();
        flags = new HashMap<>();
    }

    /**
     * Creates a multi state.
     *
     * @param marking            a unique atomic proposition representing the state of a Petri net.
     * @param atomicPropositions The set of atomic propositions that hold in this state.
     */
    public MultiState(String marking, Set<String> atomicPropositions) {
        super(marking, atomicPropositions);
        parents = new HashMap<>();
        nextSubStates = new HashMap<>();
        flags = new HashMap<>();
    }

    /**
     * Assigns the given StutterState as the parent of this state within the given substructure.
     *
     * @param subStructure the SubStructure the given parent belongs to.
     * @param block        the parent to assign.
     */
    public void setParent(SubStructure subStructure, Block block) {
        parents.put(subStructure, block);

        if (!nextSubStates.containsKey(subStructure))
            nextSubStates.put(subStructure, new TreeSet<MultiState>(new ComparableComparator<MultiState>()));
    }

    /**
     * Returns the parent of this state within the given substructure.
     *
     * @param subStructure the SubStructure the returned parent belongs to.
     * @return the parent StutterState.
     */
    public Block getParent(SubStructure subStructure) {
        return parents.get(subStructure);
    }

    /**
     * Add a state as a next state that is accessible from this state within the given substructure.
     *
     * @param subStructure the given substructure for which to add the next state.
     * @param s            the next state.
     * @return true if the set of next states did not already contain the given state.
     */
    public boolean addNext(SubStructure subStructure, MultiState s) {
        return nextSubStates.get(subStructure).add(s);
    }

    /**
     * Add a state as a next state that is accessible from this state within the given substructure.
     *
     * @param subStructure the given substructure for which to add the next state.
     * @param s            the set of  next state.
     * @return true if the set of next states did not already contain the given state.
     */
    public boolean addNext(SubStructure subStructure, Set<MultiState> s) {
        return nextSubStates.get(subStructure).addAll(s);
    }

    /**
     * Returns the set of next states that are accessible from this state within the given substructure.
     *
     * @param subStructure the given substructure for which to obtain the next states.
     * @return the set of next states that are accessible from this state.
     */
    public Set<MultiState> getNextStates(SubStructure subStructure) {
        return (nextSubStates.containsKey(subStructure) ? nextSubStates.get(subStructure) : new HashSet<MultiState>());
    }

    /**
     * Returns the set of previous states that are accessible from this state within the given substructure.
     * We're not tracking previous states for each substructure in order to save memory, but we can obtain the set by filtering.
     *
     * @param subStructure the given substructure for which to obtain the next states.
     * @return the set of previous states that are accessible from this state.
     */
    public Set<MultiState> getPreviousStates(SubStructure subStructure) {
        return getPreviousStates().stream().filter(s -> s.getNextStates(subStructure).contains(this)).collect(Collectors.toSet());
    }

    /**
     * Returns whether the flag of the stutter algorithm was raised or not for the given substructure.
     *
     * @param subStructure the given substructure.
     * @return true iff the flag of the stutter algorithm was raised for the given substructure.
     */
    public boolean getFlag(SubStructure subStructure) {
        return flags.containsKey(subStructure) && flags.get(subStructure);
    }

    /**
     * Sets the flag of the stutter algorithm for the given substructure.
     *
     * @param subStructure the given substructure.
     * @param flag         true to raise the flag, false to lower the flag.
     */
    public void setFlag(SubStructure subStructure, boolean flag) {
        flags.put(subStructure, flag);
    }

    @Override
    public String toString() {
        return id + ": {" + hash + " | " + parents.values().stream().map(Object::toString).collect(Collectors.joining(",")) + " }";
    }
}
