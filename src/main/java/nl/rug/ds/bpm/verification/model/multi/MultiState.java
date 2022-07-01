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
    private boolean inLoop = false;

    /**
     * Creates a multi state.
     *
     * @param atomicPropositions The set of atomic propositions that hold in this state.
     */
    public MultiState(Set<String> atomicPropositions) {
        super(atomicPropositions);
        parents = new HashMap<>();
        nextSubStates = new HashMap<>();
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
    }

    /**
     * Assigns the given StutterState as the parent of this state within the given substructure.
     *
     * @param subStructure the SubStructure the given parent belongs to.
     * @param block        the parent to assign.
     */
    public void setParent(SubStructure subStructure, Block block) {
        block.addSubState(this);

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
     * Updates the parent of this and all previous states within the given substructure while
     * they belong to current and have no other reachable parents.
     *
     * @param subStructure the SubStructure the given parent belongs to.
     * @param current      the currently assigned parent.
     * @param newparent    the parent to assign.
     */
    public void updatePreviousParents(SubStructure subStructure, Block current, Block newparent) {
        if (parents.get(subStructure) == current && getNextParents(subStructure).stream().allMatch(p -> p == newparent)) {
            setParent(subStructure, newparent);
            current.removeSubState(this);

            for (MultiState previous : getPreviousStates())
                previous.updatePreviousParents(subStructure, current, newparent);
        }
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
     * Returns the set of parents of the next states for the given substructure.
     *
     * @param subStructure the substructure the returned parents should belong to.
     * @return Set of StutterStates.
     */
    public synchronized Set<Block> getNextParents(SubStructure subStructure) {
        return nextSubStates.get(subStructure).stream().filter(s -> s != this).map(s -> s.getParent(subStructure)).collect(Collectors.toSet());
    }

    /**
     * Returns whether this state is a splitter of its parent within the given substructure.
     *
     * @param subStructure the given substructure.
     * @return true iff this an exit state of its parent and its next parents are disjoint from the other exit states.
     */
    public boolean isSplitter(SubStructure subStructure) {
        Block parent = getParent(subStructure);
        Set<Block> otherParents = parent.getExitStates().stream().filter(s -> s != this).flatMap(s -> s.getNextParents(subStructure).stream()).collect(Collectors.toSet());

        return parent.getExitStates().contains(this) && !otherParents.isEmpty() && Collections.disjoint(this.getNextParents(subStructure), otherParents);
    }

    @Override
    public String toString() {
        return id + ": {" + hash + " | " + parents.values().stream().map(Object::toString).collect(Collectors.joining(",")) + " }";
    }
}
